//! encoder.rs - Motor de Codificación de Hardware NDK en Rust (Edition 2024)
//!
//! ¿QUÉ HACE ESTE ARCHIVO?
//! 1. Inicializa AMediaCodec por hardware y AMediaFormat para H.264 / HEVC / AV1.
//! 2. Genera la ANativeWindow Surface de hardware (Zero-Copy) para que la GPU proyecte
//!    los fotogramas directamente sin copias en memoria JVM.
//! 3. Ejecuta el bucle de codificación y multiplexación simultánea en un hilo worker seguro
//!    con canales atómicos lock-free ('crossbeam-channel') y sin pausas de Garbage Collector.
//! 4. Procesa y escribe muestras de video y audio AAC entrelazados en tiempo real.

use std::ffi::CString;
use std::fs::OpenOptions;
use std::os::unix::io::IntoRawFd;
use std::sync::atomic::{AtomicBool, AtomicI64, AtomicU64, Ordering};
use std::sync::{Arc, Mutex};
use std::thread::{self, JoinHandle};
use std::time::{Duration, Instant};

use crossbeam_channel::{bounded, Sender};
use jni::sys::jobject;
use jni::JNIEnv;
use log::info;

use crate::cpu_affinity::apply_efficiency_core_affinity;
use crate::muxer::{PendingAudioPacket, SharedMuxerState};

// Constantes oficiales del NDK para codificación y color de superficie
pub const COLOR_FORMAT_SURFACE: i32 = 0x7f000789;
pub const AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED: isize = -2;
pub const AMEDIACODEC_INFO_TRY_AGAIN_LATER: isize = -1;
pub const AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM: u32 = 4;
pub const TIMEOUT_US: i64 = 10_000; // 10 milisegundos

unsafe extern "C" {
    pub fn AMediaFormat_setBuffer(
        format: *mut ndk_sys::AMediaFormat,
        name: *const libc::c_char,
        data: *const libc::c_void,
        size: libc::size_t,
    );
}

/// Configuración de captura y codificación provista desde Kotlin
#[derive(Debug, Clone)]
pub struct RustRecorderConfig {
    pub output_path: String,
    pub width: i32,
    pub height: i32,
    pub fps: i32,
    pub bitrate_bps: i32,
    pub codec_mime: String,
    pub audio_source: String,
}

/// Mensajes de control interno canalizados con crossbeam-channel
#[derive(Debug)]
pub enum RecorderCommand {
    Pause,
    Resume,
    Stop,
}

/// Estructura que encapsula las direcciones de memoria de hardware NDK
/// Se almacenan como escalares usize para garantizar compatibilidad incondicional
/// con los rasgos Send/Sync del compilador de Rust 2024 al transferirse entre hilos.
pub struct NdkHardwareHandles {
    pub codec_addr: usize,
    pub native_window_addr: usize,
}

/// Estado y ejecución del motor Rust 2024 con Multiplexación en Tiempo Real
pub struct RustRecorderEngine {
    pub is_recording: Arc<AtomicBool>,
    pub is_paused: Arc<AtomicBool>,
    pub frames_encoded: Arc<AtomicU64>,
    pub audio_samples_written: Arc<AtomicU64>,
    pub last_latency_us: Arc<AtomicI64>,
    pub cmd_sender: Sender<RecorderCommand>,
    pub worker_handle: Option<JoinHandle<()>>,
    pub hardware: Option<NdkHardwareHandles>,
    pub shared_muxer: Arc<Mutex<SharedMuxerState>>,
    pub is_mute: bool,
}

impl RustRecorderEngine {
    /// Inicializa el codificador de hardware mediante el Android NDK y obtiene la Surface
    pub fn new_and_init(env: &mut JNIEnv<'_>, config: RustRecorderConfig) -> Result<(Self, jobject), String> {
        info!(
            "Rust 2024: Inicializando encoder hardware: {}x{} @ {} fps, {} bps, mime: {}",
            config.width, config.height, config.fps, config.bitrate_bps, config.codec_mime
        );

        unsafe {
            // 1. Crear y configurar formato de video con NDK
            let format = ndk_sys::AMediaFormat_new();
            if format.is_null() {
                return Err("No se pudo asignar memoria para AMediaFormat".into());
            }

            let mime_c = CString::new(config.codec_mime.as_str()).map_err(|e| e.to_string())?;
            let key_mime = CString::new("mime").unwrap();
            let key_width = CString::new("width").unwrap();
            let key_height = CString::new("height").unwrap();
            let key_bitrate = CString::new("bitrate").unwrap();
            let key_frame_rate = CString::new("frame-rate").unwrap();
            let key_i_frame_interval = CString::new("i-frame-interval").unwrap();
            let key_color_format = CString::new("color-format").unwrap();

            ndk_sys::AMediaFormat_setString(format, key_mime.as_ptr(), mime_c.as_ptr());
            ndk_sys::AMediaFormat_setInt32(format, key_width.as_ptr(), config.width);
            ndk_sys::AMediaFormat_setInt32(format, key_height.as_ptr(), config.height);
            ndk_sys::AMediaFormat_setInt32(format, key_bitrate.as_ptr(), config.bitrate_bps);
            ndk_sys::AMediaFormat_setInt32(format, key_frame_rate.as_ptr(), config.fps);
            ndk_sys::AMediaFormat_setInt32(format, key_i_frame_interval.as_ptr(), 1); // 1 segundo entre Keyframes
            ndk_sys::AMediaFormat_setInt32(format, key_color_format.as_ptr(), COLOR_FORMAT_SURFACE);
            let key_repeat_prev = CString::new("repeat-previous-frame-after").unwrap();
            let repeat_interval_us = if config.fps > 0 { 1_000_000 / config.fps as i64 } else { 16_666 };
            ndk_sys::AMediaFormat_setInt64(format, key_repeat_prev.as_ptr(), repeat_interval_us);

            // 2. Crear codificador por hardware en NDK
            let codec = ndk_sys::AMediaCodec_createEncoderByType(mime_c.as_ptr());
            if codec.is_null() {
                ndk_sys::AMediaFormat_delete(format);
                return Err(format!("No se encontró codificador por hardware para {}", config.codec_mime));
            }

            let status = ndk_sys::AMediaCodec_configure(
                codec,
                format,
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                ndk_sys::AMEDIACODEC_CONFIGURE_FLAG_ENCODE as u32,
            );
            ndk_sys::AMediaFormat_delete(format);

            if status != ndk_sys::media_status_t(0) {
                ndk_sys::AMediaCodec_delete(codec);
                return Err(format!("Fallo al configurar AMediaCodec en Rust (status: {:?})", status));
            }

            // 3. Crear Input Surface de hardware (ANativeWindow)
            let mut native_window: *mut ndk_sys::ANativeWindow = std::ptr::null_mut();
            let surface_status = ndk_sys::AMediaCodec_createInputSurface(codec, &mut native_window);
            if surface_status != ndk_sys::media_status_t(0) || native_window.is_null() {
                ndk_sys::AMediaCodec_delete(codec);
                return Err("Fallo al crear la ANativeWindow Surface de hardware en Rust".into());
            }

            // 4. Crear Muxer MP4 para salida
            let file = OpenOptions::new()
                .read(true)
                .write(true)
                .create(true)
                .truncate(true)
                .open(&config.output_path)
                .map_err(|e| format!("No se pudo crear archivo MP4 {}: {}", config.output_path, e))?;

            let raw_fd = file.into_raw_fd();
            let muxer = ndk_sys::AMediaMuxer_new(raw_fd, ndk_sys::OutputFormat::AMEDIAMUXER_OUTPUT_FORMAT_MPEG_4);

            if muxer.is_null() {
                libc::close(raw_fd);
                ndk_sys::ANativeWindow_release(native_window);
                ndk_sys::AMediaCodec_delete(codec);
                return Err("Fallo al instanciar AMediaMuxer en Rust".into());
            }

            // 5. Convertir ANativeWindow a jobject android.view.Surface para Kotlin
            let jni_raw_env = env.get_raw();
            let surface_jobject = ndk_sys::ANativeWindow_toSurface(jni_raw_env as *mut _, native_window);
            if surface_jobject.is_null() {
                ndk_sys::AMediaMuxer_delete(muxer);
                libc::close(raw_fd);
                ndk_sys::ANativeWindow_release(native_window);
                ndk_sys::AMediaCodec_delete(codec);
                return Err("Fallo al convertir ANativeWindow a android.view.Surface JNI".into());
            }

            let (cmd_tx, _cmd_rx) = bounded::<RecorderCommand>(16);

            let is_mute = config.audio_source == "mute";

            let shared_muxer = Arc::new(Mutex::new(SharedMuxerState {
                muxer_addr: muxer as usize,
                raw_fd,
                video_track_index: -1,
                audio_track_index: -1,
                is_started: false,
                audio_sample_rate: 48000,
                audio_channel_count: 2,
                audio_csd: None,
                pending_audio: Vec::with_capacity(64),
                master_timebase_origin_us: -1,
                last_written_video_pts_us: -1,
                last_written_audio_pts_us: -1,
            }));

            let engine = Self {
                is_recording: Arc::new(AtomicBool::new(false)),
                is_paused: Arc::new(AtomicBool::new(false)),
                frames_encoded: Arc::new(AtomicU64::new(0)),
                audio_samples_written: Arc::new(AtomicU64::new(0)),
                last_latency_us: Arc::new(AtomicI64::new(0)),
                cmd_sender: cmd_tx,
                worker_handle: None,
                hardware: Some(NdkHardwareHandles {
                    codec_addr: codec as usize,
                    native_window_addr: native_window as usize,
                }),
                shared_muxer,
                is_mute,
            };

            info!(
                "Rust 2024: Inicialización de hardware completada con éxito. Surface y Muxer listos (is_mute={}).",
                is_mute
            );
            Ok((engine, surface_jobject))
        }
    }

    /// Configura dinámicamente los parámetros de la pista de audio antes de iniciar el muxer
    pub fn configure_audio(&self, sample_rate: i32, channel_count: i32, csd: Option<Vec<u8>>) {
        if let Ok(mut muxer) = self.shared_muxer.lock() {
            if sample_rate > 0 {
                muxer.audio_sample_rate = sample_rate;
            }
            if channel_count > 0 {
                muxer.audio_channel_count = channel_count;
            }
            if let Some(csd_data) = csd {
                info!("Rust 2024: Configuración CSD-0 de audio recibida ({} bytes).", csd_data.len());
                muxer.audio_csd = Some(csd_data);
            }
        }
    }

    /// Escribe una muestra de audio codificada (AAC) en tiempo real directamente en AMediaMuxer
    pub fn write_audio_sample(&self, data: &[u8], pts_us: i64, flags: u32) {
        if self.is_mute || data.is_empty() {
            return;
        }

        if let Ok(mut muxer) = self.shared_muxer.lock() {
            if muxer.is_started && muxer.audio_track_index >= 0 {
                let normalized_pts = muxer.normalize_pts(pts_us, false);
                let mut buffer_info = ndk_sys::AMediaCodecBufferInfo {
                    offset: 0,
                    size: data.len() as i32,
                    presentationTimeUs: normalized_pts,
                    flags,
                };
                unsafe {
                    ndk_sys::AMediaMuxer_writeSampleData(
                        muxer.muxer_addr as *mut ndk_sys::AMediaMuxer,
                        muxer.audio_track_index as usize,
                        data.as_ptr(),
                        &mut buffer_info,
                    );
                }
                self.audio_samples_written.fetch_add(1, Ordering::Relaxed);
            } else {
                // Buffer preventivo mientras el muxer espera el primer cuadro de video
                if muxer.pending_audio.len() < 64 {
                    muxer.pending_audio.push(PendingAudioPacket {
                        data: data.to_vec(),
                        pts_us,
                        flags,
                    });
                }
            }
        }
    }

    /// Comienza la codificación arrancando AMediaCodec y el hilo de multiplexación simultánea
    pub fn start(&mut self) -> Result<(), String> {
        let hardware = self.hardware.take().ok_or("Hardware ya consumido o no inicializado")?;
        let is_recording = Arc::clone(&self.is_recording);
        let is_paused = Arc::clone(&self.is_paused);
        let frames_encoded = Arc::clone(&self.frames_encoded);
        let audio_samples_written = Arc::clone(&self.audio_samples_written);
        let last_latency_us = Arc::clone(&self.last_latency_us);
        let shared_muxer = Arc::clone(&self.shared_muxer);
        let is_mute = self.is_mute;
        let (cmd_tx, rx_worker) = bounded::<RecorderCommand>(16);
        self.cmd_sender = cmd_tx;

        let codec_addr = hardware.codec_addr;
        let native_window_addr = hardware.native_window_addr;

        unsafe {
            let start_status = ndk_sys::AMediaCodec_start(codec_addr as *mut ndk_sys::AMediaCodec);
            if start_status != ndk_sys::media_status_t(0) {
                return Err(format!("AMediaCodec_start falló con código: {:?}", start_status));
            }
        }

        is_recording.store(true, Ordering::SeqCst);
        is_paused.store(false, Ordering::SeqCst);

        // Hilo worker de codificación sin contención de cerrojos
        let handle = thread::Builder::new()
            .name("velorec_rust_worker".into())
            .spawn(move || {
                info!("Rust 2024: Hilo de codificación y multiplexación activo.");
                apply_efficiency_core_affinity("velorec_rust_worker");
                crate::resilience::ResilienceGuardian::boost_thread_priority("velorec_rust_worker");
                let codec = codec_addr as *mut ndk_sys::AMediaCodec;
                let native_window = native_window_addr as *mut ndk_sys::ANativeWindow;

                let mut buffer_info = ndk_sys::AMediaCodecBufferInfo {
                    offset: 0,
                    size: 0,
                    presentationTimeUs: 0,
                    flags: 0,
                };

                let mut stop_requested = false;
                let mut drain_retries = 0;
                const MAX_DRAIN_RETRIES: i32 = 50; // Hasta 500ms tras stop

                while is_recording.load(Ordering::Relaxed) || stop_requested {
                    // Verificar mensajes en el canal sin bloquear
                    if let Ok(cmd) = rx_worker.try_recv() {
                        match cmd {
                            RecorderCommand::Pause => is_paused.store(true, Ordering::Relaxed),
                            RecorderCommand::Resume => is_paused.store(false, Ordering::Relaxed),
                            RecorderCommand::Stop => {
                                unsafe {
                                    ndk_sys::AMediaCodec_signalEndOfInputStream(codec);
                                }
                                stop_requested = true;
                            }
                        }
                    }

                    if is_paused.load(Ordering::Relaxed) && !stop_requested {
                        thread::sleep(Duration::from_millis(20));
                        continue;
                    }

                    let tick_start = Instant::now();

                    unsafe {
                        let out_idx = ndk_sys::AMediaCodec_dequeueOutputBuffer(
                            codec,
                            &mut buffer_info,
                            TIMEOUT_US,
                        );

                        if out_idx >= 0 {
                            drain_retries = 0; // Datos recibidos
                            let mut buffer_size: usize = 0;
                            let buffer_ptr = ndk_sys::AMediaCodec_getOutputBuffer(
                                codec,
                                out_idx as usize,
                                &mut buffer_size,
                            );

                            if !buffer_ptr.is_null() && buffer_info.size > 0 {
                                // Omitir buffers CODEC_CONFIG (SPS/PPS) en el muxer
                                if (buffer_info.flags & 2) != 0 {
                                    info!("Rust 2024: Buffer CODEC_CONFIG omitido en muxer");
                                } else {
                                    if let Ok(mut muxer_lock) = shared_muxer.lock() {
                                        let normalized_pts = muxer_lock.normalize_pts(buffer_info.presentationTimeUs, true);
                                        let mut sample_info = buffer_info;
                                        sample_info.presentationTimeUs = normalized_pts;

                                        if muxer_lock.is_started && muxer_lock.video_track_index >= 0 {
                                            ndk_sys::AMediaMuxer_writeSampleData(
                                                muxer_lock.muxer_addr as *mut ndk_sys::AMediaMuxer,
                                                muxer_lock.video_track_index as usize,
                                                buffer_ptr,
                                                &sample_info,
                                            );
                                            let current_frames = frames_encoded.fetch_add(1, Ordering::Relaxed);
                                            if current_frames > 0 && current_frames % 300 == 0 {
                                                if let Ok(lock) = crate::jni_bindings::GLOBAL_RESILIENCE.lock() {
                                                    if let Some(ref guardian) = *lock {
                                                        guardian.force_memory_trim();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            ndk_sys::AMediaCodec_releaseOutputBuffer(codec, out_idx as usize, false);

                            let latency = tick_start.elapsed().as_micros() as i64;
                            last_latency_us.store(latency, Ordering::Relaxed);

                            if (buffer_info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) != 0 {
                                info!("Rust 2024: Señal EOS recibida.");
                                break;
                            }
                        } else if out_idx == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED {
                            let new_format = ndk_sys::AMediaCodec_getOutputFormat(codec);
                            if let Ok(mut muxer_lock) = shared_muxer.lock() {
                                let muxer_ptr = muxer_lock.muxer_addr as *mut ndk_sys::AMediaMuxer;
                                let v_idx = ndk_sys::AMediaMuxer_addTrack(muxer_ptr, new_format);
                                muxer_lock.video_track_index = v_idx as i32;
                                ndk_sys::AMediaFormat_delete(new_format);

                                // Si hay audio, agregar la pista de audio AAC de forma estricta ANTES de start
                                if !is_mute {
                                    let audio_format = ndk_sys::AMediaFormat_new();
                                    let mime_audio = CString::new("audio/mp4a-latm").unwrap();
                                    let key_mime = CString::new("mime").unwrap();
                                    let key_sample_rate = CString::new("sample-rate").unwrap();
                                    let key_channel_count = CString::new("channel-count").unwrap();
                                    let key_bitrate = CString::new("bitrate").unwrap();
                                    let key_csd0 = CString::new("csd-0").unwrap();

                                    ndk_sys::AMediaFormat_setString(audio_format, key_mime.as_ptr(), mime_audio.as_ptr());
                                    ndk_sys::AMediaFormat_setInt32(audio_format, key_sample_rate.as_ptr(), muxer_lock.audio_sample_rate);
                                    ndk_sys::AMediaFormat_setInt32(audio_format, key_channel_count.as_ptr(), muxer_lock.audio_channel_count);
                                    ndk_sys::AMediaFormat_setInt32(audio_format, key_bitrate.as_ptr(), 128_000);

                                    let csd_bytes = muxer_lock.audio_csd.clone().unwrap_or_else(|| {
                                        let freq_idx: u8 = if muxer_lock.audio_sample_rate == 44100 { 4 } else { 3 };
                                        let b0 = 0x10 | ((freq_idx >> 1) & 0x07);
                                        let b1 = ((freq_idx & 0x01) << 7) | ((muxer_lock.audio_channel_count as u8 & 0x0F) << 3);
                                        vec![b0, b1]
                                    });

                                    AMediaFormat_setBuffer(
                                        audio_format,
                                        key_csd0.as_ptr(),
                                        csd_bytes.as_ptr() as *const libc::c_void,
                                        csd_bytes.len(),
                                    );

                                    let a_idx = ndk_sys::AMediaMuxer_addTrack(muxer_ptr, audio_format);
                                    muxer_lock.audio_track_index = a_idx as i32;
                                    ndk_sys::AMediaFormat_delete(audio_format);
                                    info!("Rust 2024: Pistas agregadas a AMediaMuxer: Video={}, Audio={}", v_idx, a_idx);
                                } else {
                                    info!("Rust 2024: Pista única de Video={} agregada a AMediaMuxer (Audio silenciado).", v_idx);
                                }

                                ndk_sys::AMediaMuxer_start(muxer_ptr);
                                muxer_lock.is_started = true;
                                info!("Rust 2024: AMediaMuxer iniciado en tiempo real. Multiplexación activa.");

                                // Drenar muestras de audio previas alineándolas con la base de tiempo T0 maestra
                                let pending = std::mem::take(&mut muxer_lock.pending_audio);
                                let a_track = muxer_lock.audio_track_index;
                                for pkt in pending {
                                    if a_track >= 0 {
                                        let normalized_pts = muxer_lock.normalize_pts(pkt.pts_us, false);
                                        let mut a_info = ndk_sys::AMediaCodecBufferInfo {
                                            offset: 0,
                                            size: pkt.data.len() as i32,
                                            presentationTimeUs: normalized_pts,
                                            flags: pkt.flags,
                                        };
                                        ndk_sys::AMediaMuxer_writeSampleData(
                                            muxer_ptr,
                                            a_track as usize,
                                            pkt.data.as_ptr(),
                                            &mut a_info,
                                        );
                                        audio_samples_written.fetch_add(1, Ordering::Relaxed);
                                    }
                                }
                            }
                        } else if out_idx == AMEDIACODEC_INFO_TRY_AGAIN_LATER && stop_requested {
                            drain_retries += 1;
                            if drain_retries >= MAX_DRAIN_RETRIES {
                                info!("Rust 2024: Drenado completado tras stop ({} reintentos)", drain_retries);
                                break;
                            }
                        }
                    }
                }

                // Liberación de recursos NDK de hardware
                unsafe {
                    info!("Rust 2024: Limpiando recursos de hardware NDK...");
                    ndk_sys::AMediaCodec_stop(codec);
                    ndk_sys::AMediaCodec_delete(codec);

                    if !native_window.is_null() {
                        ndk_sys::ANativeWindow_release(native_window);
                    }

                    if let Ok(mut muxer_lock) = shared_muxer.lock() {
                        let muxer_ptr = muxer_lock.muxer_addr as *mut ndk_sys::AMediaMuxer;
                        if muxer_lock.is_started {
                            ndk_sys::AMediaMuxer_stop(muxer_ptr);
                            muxer_lock.is_started = false;
                        }
                        ndk_sys::AMediaMuxer_delete(muxer_ptr);
                        libc::close(muxer_lock.raw_fd);
                    }
                }
                info!("Rust 2024: Codificación y multiplexación finalizada limpiamente (Cero Espera al Detener).");
            })
            .map_err(|e| format!("Fallo al lanzar hilo Rust: {}", e))?;

        self.worker_handle = Some(handle);
        Ok(())
    }

    pub fn pause(&self) {
        let _ = self.cmd_sender.send(RecorderCommand::Pause);
        self.is_paused.store(true, Ordering::SeqCst);
        info!("Rust 2024: Codificación en pausa.");
    }

    pub fn resume(&self) {
        let _ = self.cmd_sender.send(RecorderCommand::Resume);
        self.is_paused.store(false, Ordering::SeqCst);
        info!("Rust 2024: Codificación reanudada.");
    }

    pub fn stop(&mut self) {
        let _ = self.cmd_sender.send(RecorderCommand::Stop);
        self.is_recording.store(false, Ordering::SeqCst);
        if let Some(handle) = self.worker_handle.take() {
            let _ = handle.join();
        }
        info!(
            "Rust 2024: Grabación detenida. Total cuadros: {}, Total muestras audio: {}",
            self.frames_encoded.load(Ordering::Relaxed),
            self.audio_samples_written.load(Ordering::Relaxed)
        );
    }
}
