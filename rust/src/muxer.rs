//! muxer.rs - Estado y Multiplexación en Tiempo Real en Rust (Edition 2024)
//!
//! ¿QUÉ HACE ESTE ARCHIVO?
//! 1. Encapsula el estado compartido de AMediaMuxer del NDK entre los hilos de video y audio.
//! 2. Sincronización Maestra (Zero-Drift & Anti-Lag): Normaliza las marcas de tiempo (PTS)
//!    con respecto a un punto maestro T0 unificado para evitar cualquier desfase acumulado en juegos.
//! 3. Provee un búfer preventivo temporal ('pending_audio') para absorber muestras de audio
//!    mientras el muxer espera el primer paquete de video (CODEC_CONFIG / SPS / PPS).

use log::info;

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
    pub master_timebase_origin_us: i64,
    pub last_written_video_pts_us: i64,
    pub last_written_audio_pts_us: i64,
}

impl SharedMuxerState {
    /// Normaliza marcas de tiempo de video y audio anclándolas al mismo punto T0 de la sesión.
    /// Garantiza sincronización milimétrica (cero desfase acumulativo) y monotonicidad estricta.
    pub fn normalize_pts(&mut self, raw_pts_us: i64, is_video: bool) -> i64 {
        if self.master_timebase_origin_us < 0 {
            self.master_timebase_origin_us = raw_pts_us;
            info!(
                "Rust 2024: Base de tiempo maestra T0 anclada a {} us (primer paquete: {})",
                raw_pts_us,
                if is_video { "Video" } else { "Audio" }
            );
        }

        let mut rel_pts = raw_pts_us - self.master_timebase_origin_us;
        if rel_pts < 0 {
            rel_pts = 0;
        }

        if is_video {
            if rel_pts <= self.last_written_video_pts_us {
                rel_pts = if self.last_written_video_pts_us >= 0 {
                    self.last_written_video_pts_us + 1000
                } else {
                    0
                };
            }
            self.last_written_video_pts_us = rel_pts;
        } else {
            if rel_pts <= self.last_written_audio_pts_us {
                rel_pts = if self.last_written_audio_pts_us >= 0 {
                    self.last_written_audio_pts_us + 500
                } else {
                    0
                };
            }
            self.last_written_audio_pts_us = rel_pts;
        }

        rel_pts
    }
}
