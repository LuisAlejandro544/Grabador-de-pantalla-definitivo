package com.velorec.recorder.data.model

/**
 * RecordingConfig.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Define los modelos de datos inmutables y preajustes (presets) para la grabación de pantalla
 * de alto rendimiento en VeloRec Studio.
 * 
 * LÓGICA Y RESPONSABILIDADES:
 * 1. NativeRecordingConfig: Objeto de transporte que encapsula los parámetros de codificación
 *    (motor C++23 o Rust 2024, resolución par YUV420p, tasa de cuadros FPS, bitrate en bps,
 *    códec de hardware y fuente de audio).
 * 2. ResolutionPreset: Estructura que define resoluciones estándar asegurando paridad de píxeles
 *    (ancho y alto pares) para prevenir excepciones en AMediaCodec del NDK.
 * 3. Presets constantes para Bitrate (4 a 50 Mbps) y Tasas de Cuadro (24 a 120 FPS).
 * 4. NativeTab: Enumeración de las cuatro pantallas principales de la aplicación táctil.
 */

data class NativeRecordingConfig(
    val engine: String = "cpp23",
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 60,
    val bitrateBps: Int = 12_000_000,
    val codec: String = "H264",
    val audioSource: String = "both"
)

data class ResolutionPreset(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val label: String,
    val recommendedBitrateMbps: Int
)

val RESOLUTION_PRESETS = listOf(
    ResolutionPreset("480p", "480p SD", 480, 854, "SD", 4),
    ResolutionPreset("720p", "720p HD", 720, 1280, "HD", 8),
    ResolutionPreset("1080p", "1080p FHD", 1080, 1920, "FHD", 12),
    ResolutionPreset("1440p", "1440p 2K", 1440, 2560, "2K", 24),
    ResolutionPreset("4k", "4K UHD", 2160, 3840, "4K", 35)
)

val BITRATE_PRESETS = listOf(4, 8, 12, 16, 24, 35, 50)
val FPS_PRESETS = listOf(24, 30, 60, 90, 120)

enum class NativeTab {
    RECORDER, SETTINGS, GALLERY, BENCHMARK
}
