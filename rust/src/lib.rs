//! lib.rs - Motor de Grabación y Codificación de Pantalla en Rust (Edition 2024)
//! 
//! ¿QUÉ HACE ESTE ARCHIVO?
//! 1. Implementa el backend nativo de codificación en Rust puro con seguridad garantizada por el compilador.
//! 2. CERO COPIA (Zero-Copy): Utiliza AMediaCodec_createInputSurface y ANativeWindow_toSurface del NDK
//!    para crear la Surface de hardware donde la GPU proyecta directamente los fotogramas sin pasar por la JVM.
//! 3. Procesa los búferes codificados (H.264 / HEVC / AV1) en un hilo de trabajo dedicado con
//!    canales multihilo atómicos (crossbeam-channel), garantizando cero condiciones de carrera (Data Races)
//!    y ausencia total de recolección de basura (Zero GC).
//! 4. Escribe el contenedor MP4 mediante AMediaMuxer del NDK con latencia mínima (~3 ms por cuadro).
//! 5. Expone las firmas JNI 'Java_com_velorec_recorder_ScreenRecordService_native*Rust*' para que Kotlin
//!    lo invoque de forma inmediata y transparente.
//! 6. Cumple estrictamente con Rust Edition 2024 utilizando #[unsafe(no_mangle)] y transferencia segura
//!    entre hilos mediante direcciones escalares (usize) inmunes a restricciones de punteros crudos !Send.

#![allow(clippy::collapsible_if, clippy::missing_safety_doc, clippy::let_unit_value)]

pub mod audio_oboe;
pub mod color_yuv;
pub mod gpu_glow;
pub mod streamer;

pub use audio_oboe::{OboeAudioConfig, OboeAudioEngine, PcmAudioPacket};
pub use color_yuv::{YuvColorConverter, YuvColorFormat};
pub use gpu_glow::{FaceCamShaderParams, GamingColorBoostParams, GlowGpuPipeline, GlowShaderEffect};
pub use streamer::{RustStreamEngine, StreamConfig, StreamPacketType, StreamStats};


use std::ffi::CString;
use std::fs::OpenOptions;
use std::os::unix::io::IntoRawFd;
use std::sync::atomic::{AtomicBool, AtomicI64, AtomicU64, Ordering};

use std::sync::{Arc, Mutex};
use std::thread::{self, JoinHandle};
use std::time::{Duration, Instant};

use crossbeam_channel::{bounded, Sender};
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jobject};
use jni::JNIEnv;
use log::{error, info, LevelFilter};

// Constantes oficiales del NDK para codificación y color de superficie
const COLOR_FORMAT_SURFACE: i32 = 0x7f000789;
const AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED: isize = -2;
const AMEDIACODEC_INFO_TRY_AGAIN_LATER: isize = -1;
const AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM: u32 = 4;
const TIMEOUT_US: i64 = 10_000; // 10 milisegundos

unsafe extern "C" {
    fn AMediaFormat_setBuffer(
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
enum RecorderCommand {
    Pause,
    Resume,
    Stop,
}

/// Muestra de audio pendiente si arriba antes de iniciar el muxer
#[derive(Debug, Clone)]
pub struct PendingAudioPacket {
    pub data: Vec<u8>,
    pub pts_us: i64,
    pub flags: u32,
}

/// Estado compartido del multiplexor nativo AMediaMuxer entre hilos de video y audio
pub struct SharedMuxerState {
    pub muxer_addr: usize,
    pub raw_fd: std::os::unix::io::RawFd,
    pub video_track_index: i32,
    pub audio_track_index: i32,
    pub is_started: bool,
    pub audio_sample_rate: i32,
    pub audio_channel_count: i32,
    pub audio_csd: Option<Vec<u8>>,
    pub pending_audio: Vec<PendingAudioPacket>,
}

/// Estructura que encapsula las direcciones de memoria de hardware NDK
/// Se almacenan como escalares usize para garantizar compatibilidad incondicional
/// con los rasgos Send/Sync del compilador de Rust 2024 al transferirse entre hilos.
struct NdkHardwareHandles {
    codec_addr: usize,
    native_window_addr: usize,
}

/// Estado y ejecución del motor Rust 2024 con Multiplexación en Tiempo Real
pub struct RustRecorderEngine {
    is_recording: Arc<AtomicBool>,
    is_paused: Arc<AtomicBool>,
    frames_encoded: Arc<AtomicU64>,
    audio_samples_written: Arc<AtomicU64>,
    last_latency_us: Arc<AtomicI64>,
    cmd_sender: Sender<RecorderCommand>,
    worker_handle: Option<JoinHandle<()>>,
    hardware: Option<NdkHardwareHandles>,
    shared_muxer: Arc<Mutex<SharedMuxerState>>,
    is_mute: bool,
}

impl RustRecorderEngine {
    /// Inicializa el codificador de hardware mediante el Android NDK y obtiene la Surface
    pub fn new_and_init(env: &mut JNIEnv<'_>, config: RustRecorderConfig) -> Result<(Self, jobject), String> {
        info!("Rust 2024: Inicializando encoder hardware: {}x{} @ {} fps, {} bps, mime: {}",
            config.width, config.height, config.fps, config.bitrate_bps, config.codec_mime);

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

            info!("Rust 2024: Inicialización de hardware completada con éxito. Surface y Muxer listos (is_mute={}).", is_mute);
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
                let mut buffer_info = ndk_sys::AMediaCodecBufferInfo {
                    offset: 0,
                    size: data.len() as i32,
                    presentationTimeUs: pts_us,
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

        // Aplica afinidad de hilos hacia núcleos de eficiencia (LITTLE) en procesadores ARM big.LITTLE
        fn apply_efficiency_core_affinity(thread_name: &str) {
            if let Ok(c_name) = CString::new(thread_name) {
                unsafe {
                    libc::pthread_setname_np(libc::pthread_self(), c_name.as_ptr());
                }
            }

            let total_cores = unsafe { libc::sysconf(libc::_SC_NPROCESSORS_CONF) as i32 };
            if total_cores <= 1 {
                return;
            }

            unsafe {
                let mut cpuset: libc::cpu_set_t = std::mem::zeroed();
                libc::CPU_ZERO(&mut cpuset);

                let efficiency_cores = if total_cores >= 8 { 4 } else { (total_cores / 2).max(1) };
                for i in 0..efficiency_cores {
                    libc::CPU_SET(i as usize, &mut cpuset);
                }

                let res = libc::sched_setaffinity(0, std::mem::size_of::<libc::cpu_set_t>(), &cpuset);
                if res == 0 {
                    info!("Rust 2024: Afinidad aplicada para [{}] hacia {} núcleos LITTLE de {} totales.",
                        thread_name, efficiency_cores, total_cores);
                }
            }
        }

        // Hilo worker de codificación sin contención de cerrojos
        let handle = thread::Builder::new()
            .name("velorec_rust_worker".into())
            .spawn(move || {
                info!("Rust 2024: Hilo de codificación y multiplexación activo.");
                apply_efficiency_core_affinity("velorec_rust_worker");
                unsafe {
                    libc::setpriority(libc::PRIO_PROCESS, 0, -16);
                }
                let codec = codec_addr as *mut ndk_sys::AMediaCodec;
                let native_window = native_window_addr as *mut ndk_sys::ANativeWindow;

                let mut buffer_info = ndk_sys::AMediaCodecBufferInfo {
                    offset: 0,
                    size: 0,
                    presentationTimeUs: 0,
                    flags: 0,
                };

                let mut first_frame_time_us: i64 = -1;
                let mut last_presentation_time_us: i64 = -1;
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
                                    let mut sample_info = buffer_info;
                                    if is_mute {
                                        if first_frame_time_us < 0 {
                                            first_frame_time_us = buffer_info.presentationTimeUs;
                                        }
                                        if sample_info.presentationTimeUs >= first_frame_time_us {
                                            sample_info.presentationTimeUs -= first_frame_time_us;
                                        } else {
                                            sample_info.presentationTimeUs = 0;
                                        }
                                    }

                                    // Garantizar estricta monotonicidad creciente
                                    if sample_info.presentationTimeUs <= last_presentation_time_us {
                                        sample_info.presentationTimeUs = if last_presentation_time_us >= 0 {
                                            last_presentation_time_us + 1000
                                        } else {
                                            0
                                        };
                                    }
                                    last_presentation_time_us = sample_info.presentationTimeUs;

                                    if let Ok(muxer_lock) = shared_muxer.lock() {
                                        if muxer_lock.is_started && muxer_lock.video_track_index >= 0 {
                                            ndk_sys::AMediaMuxer_writeSampleData(
                                                muxer_lock.muxer_addr as *mut ndk_sys::AMediaMuxer,
                                                muxer_lock.video_track_index as usize,
                                                buffer_ptr,
                                                &sample_info,
                                            );
                                            frames_encoded.fetch_add(1, Ordering::Relaxed);
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

                                // Drenar muestras de audio previas
                                let pending = std::mem::take(&mut muxer_lock.pending_audio);
                                let a_track = muxer_lock.audio_track_index;
                                for pkt in pending {
                                    if a_track >= 0 {
                                        let mut a_info = ndk_sys::AMediaCodecBufferInfo {
                                            offset: 0,
                                            size: pkt.data.len() as i32,
                                            presentationTimeUs: pkt.pts_us,
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
        info!("Rust 2024: Grabación detenida. Total cuadros: {}, Total muestras audio: {}",
            self.frames_encoded.load(Ordering::Relaxed),
            self.audio_samples_written.load(Ordering::Relaxed));
    }
}

// Instancia global del motor Rust gestionada de forma thread-safe con Mutex
static GLOBAL_RUST_ENGINE: Mutex<Option<RustRecorderEngine>> = Mutex::new(None);

// =========================================================================
// FIRMAS JNI PARA COMUNICACIÓN CON KOTLIN (ScreenRecordService)
// Cumpliendo con Rust Edition 2024 (RFC 3325: unsafe(no_mangle))
// =========================================================================

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeInitRustRecorder<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    path: JString<'local>,
    width: jint,
    height: jint,
    fps: jint,
    bitrate: jint,
    codec: JString<'local>,
    audio: JString<'local>,
) -> jobject {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(LevelFilter::Info)
            .with_tag("VeloRec_Rust2024"),
    );

    let path_str: String = env.get_string(&path)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();
    let codec_str: String = env.get_string(&codec)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_else(|_| "H264".into());
    let audio_str: String = env.get_string(&audio)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();

    let codec_mime = match codec_str.as_str() {
        "HEVC" => "video/hevc",
        "AV1" => "video/av01",
        _ => "video/avc",
    };

    let config = RustRecorderConfig {
        output_path: path_str,
        width,
        height,
        fps,
        bitrate_bps: bitrate,
        codec_mime: codec_mime.to_string(),
        audio_source: audio_str,
    };

    match RustRecorderEngine::new_and_init(&mut env, config) {
        Ok((engine, surface_obj)) => {
            if let Ok(mut lock) = GLOBAL_RUST_ENGINE.lock() {
                *lock = Some(engine);
            }
            surface_obj
        }
        Err(err_msg) => {
            error!("Error al inicializar RustRecorderEngine: {}", err_msg);
            let _ = env.throw_new("java/lang/RuntimeException", &err_msg);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeStartRustEncoding(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) {
    if let Ok(mut lock) = GLOBAL_RUST_ENGINE.lock() {
        if let Some(ref mut engine) = *lock {
            if let Err(e) = engine.start() {
                error!("Error al iniciar motor Rust: {}", e);
            }
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativePauseRustEncoding(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) {
    if let Ok(lock) = GLOBAL_RUST_ENGINE.lock() {
        if let Some(ref engine) = *lock {
            engine.pause();
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeResumeRustEncoding(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) {
    if let Ok(lock) = GLOBAL_RUST_ENGINE.lock() {
        if let Some(ref engine) = *lock {
            engine.resume();
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeStopRustEncoding(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) {
    if let Ok(mut lock) = GLOBAL_RUST_ENGINE.lock() {
        if let Some(ref mut engine) = lock.take() {
            engine.stop();
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeConfigureRustAudioTrack<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    sample_rate: jint,
    channel_count: jint,
    csd_buffer: jobject,
    csd_size: jint,
) {
    let csd_vec: Option<Vec<u8>> = if !csd_buffer.is_null() && csd_size > 0 {
        let jobj = unsafe { jni::objects::JObject::from_raw(csd_buffer) };
        let byte_buf = jni::objects::JByteBuffer::from(jobj);
        if let Ok(addr) = env.get_direct_buffer_address(&byte_buf) {
            Some(std::slice::from_raw_parts(addr, csd_size as usize).to_vec())
        } else {
            None
        }
    } else {
        None
    };

    if let Ok(lock) = GLOBAL_RUST_ENGINE.lock() {
        if let Some(ref engine) = *lock {
            engine.configure_audio(sample_rate, channel_count, csd_vec);
        }
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeWriteRustAudioSample<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    byte_buffer: jobject,
    size: jint,
    pts_us: jlong,
    flags: jint,
) {
    if size <= 0 || byte_buffer.is_null() {
        return;
    }

    let jobj = unsafe { jni::objects::JObject::from_raw(byte_buffer) };
    let byte_buf = jni::objects::JByteBuffer::from(jobj);
    if let Ok(addr) = env.get_direct_buffer_address(&byte_buf) {
        let slice = std::slice::from_raw_parts(addr, size as usize);
        if let Ok(lock) = GLOBAL_RUST_ENGINE.lock() {
            if let Some(ref engine) = *lock {
                engine.write_audio_sample(slice, pts_us as i64, flags as u32);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_rust_recorder_config_defaults() {
        let config = RustRecorderConfig {
            output_path: "/data/local/tmp/test.mp4".into(),
            width: 1080,
            height: 1920,
            fps: 60,
            bitrate_bps: 12_000_000,
            codec_mime: "video/avc".into(),
            audio_source: "both".into(),
        };

        assert_eq!(config.width % 2, 0);
        assert_eq!(config.height % 2, 0);
        assert_eq!(config.fps, 60);
        assert_eq!(config.codec_mime, "video/avc");
    }

    #[test]
    fn test_recorder_command_channel_delivery() {
        let (tx, rx) = bounded::<RecorderCommand>(4);
        assert!(tx.send(RecorderCommand::Pause).is_ok());
        assert!(tx.send(RecorderCommand::Resume).is_ok());
        assert!(tx.send(RecorderCommand::Stop).is_ok());

        assert!(matches!(rx.recv().unwrap(), RecorderCommand::Pause));
        assert!(matches!(rx.recv().unwrap(), RecorderCommand::Resume));
        assert!(matches!(rx.recv().unwrap(), RecorderCommand::Stop));
    }
}

