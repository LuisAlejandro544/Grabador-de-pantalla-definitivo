//! jni_bindings.rs - Firmas JNI de Interoperabilidad con Android Kotlin (ScreenRecordService)
//!
//! ¿QUÉ HACE ESTE ARCHIVO?
//! 1. Expone los símbolos nativos C ('extern "system"') requeridos por la JVM de Android.
//! 2. Cumple estrictamente con Rust Edition 2024 (RFC 3325) usando '#[unsafe(no_mangle)]'.
//! 3. Gestiona la instancia global 'GLOBAL_RUST_ENGINE' protegida por 'Mutex' para acceso thread-safe.

use std::sync::Mutex;

use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jobject};
use jni::JNIEnv;
use log::{error, LevelFilter};

use crate::encoder::{RustRecorderConfig, RustRecorderEngine};

// Instancia global del motor Rust gestionada de forma thread-safe con Mutex
pub static GLOBAL_RUST_ENGINE: Mutex<Option<RustRecorderEngine>> = Mutex::new(None);

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
