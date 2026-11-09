//! lib.rs - Fachada y Raíz del Motor Nativo en Rust (Edition 2024)
//! 
//! ¿QUÉ HACE ESTE ARCHIVO?
//! Punto de entrada del crate cdylib 'velorec_rust2024'.
//! Orquesta los submódulos especializados de alto rendimiento y reexporta sus tipos
//! principales preservando compatibilidad total con el compilador y con Kotlin.
//!
//! ARQUITECTURA MODULAR:
//! - 'encoder': Codificador de hardware NDK (AMediaCodec, ANativeWindow, bucle worker).
//! - 'muxer': Multiplexor en tiempo real (AMediaMuxer, SharedMuxerState, sincronización PTS T0).
//! - 'cpu_affinity': Anclaje de hilos a núcleos de eficiencia LITTLE (sched_setaffinity).
//! - 'jni_bindings': Firmas exportadas de interoperabilidad JNI para ScreenRecordService.
//! - 'audio_oboe': Captura nativa de audio de ultra baja latencia con Google Oboe.
//! - 'color_yuv': Conversión de color acelerada por SIMD ARM NEON (yuvutils-rs).
//! - 'gpu_glow': Shaders GPU OpenGL ES 3.20 en Rust puro (glow).
//! - 'streamer': Transmisión en vivo RTMP/SRT de ultra baja latencia.

#![allow(clippy::collapsible_if, clippy::missing_safety_doc, clippy::let_unit_value)]

pub mod audio_oboe;
pub mod color_yuv;
pub mod cpu_affinity;
pub mod encoder;
pub mod gpu_glow;
pub mod jni_bindings;
pub mod muxer;
pub mod resilience;
pub mod streamer;

pub use audio_oboe::{OboeAudioConfig, OboeAudioEngine, PcmAudioPacket};
pub use color_yuv::{YuvColorConverter, YuvColorFormat};
pub use cpu_affinity::apply_efficiency_core_affinity;
pub use encoder::{
    NdkHardwareHandles, RecorderCommand, RustRecorderConfig, RustRecorderEngine,
    AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM, AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED,
    AMEDIACODEC_INFO_TRY_AGAIN_LATER, COLOR_FORMAT_SURFACE, TIMEOUT_US,
};
pub use gpu_glow::{FaceCamShaderParams, GamingColorBoostParams, GlowGpuPipeline, GlowShaderEffect};
pub use jni_bindings::GLOBAL_RUST_ENGINE;
pub use muxer::{PendingAudioPacket, SharedMuxerState};
pub use resilience::{MemoryHealthReport, ResilienceGuardian};
pub use streamer::{RustStreamEngine, StreamConfig, StreamPacketType, StreamStats};

#[cfg(test)]
mod tests {
    use super::*;
    use crossbeam_channel::bounded;

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
