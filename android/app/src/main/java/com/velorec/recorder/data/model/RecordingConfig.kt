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

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

data class NativeRecordingConfig(
    val engine: String = "cpp23",
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 60,
    val bitrateBps: Int = 12_000_000,
    val codec: String = "H264",
    val audioSource: String = "both",
    val countdownSec: Int = 3
)

data class ResolutionPreset(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val label: String,
    val recommendedBitrateMbps: Int,
    val isNative: Boolean = false
)

val RESOLUTION_PRESETS = listOf(
    ResolutionPreset("native", "Nativa (Pantalla)", 1080, 2400, "Real", 12, isNative = true),
    ResolutionPreset("1080p", "1080p FHD", 1080, 1920, "FHD", 12),
    ResolutionPreset("720p", "720p HD", 720, 1280, "HD", 8),
    ResolutionPreset("480p", "480p SD", 480, 854, "SD", 4),
    ResolutionPreset("1440p", "1440p 2K", 1440, 2560, "2K", 24),
    ResolutionPreset("4k", "4K UHD", 2160, 3840, "4K", 35)
)

/**
 * Genera resoluciones adaptadas a la relación de aspecto real de la pantalla del smartphone del usuario.
 * Garantiza que ancho y alto sean números pares (múltiplos de 2) y previene barras verdes
 * por desajuste entre VirtualDisplay y la superficie física del dispositivo.
 */
fun getAdaptivePresetsForDevice(context: Context): List<ResolutionPreset> {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    val (screenWidth, screenHeight) = if (windowManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.currentWindowMetrics.bounds
        val w = minOf(bounds.width(), bounds.height())
        val h = maxOf(bounds.width(), bounds.height())
        Pair(if (w % 2 == 0) w else w - 1, if (h % 2 == 0) h else h - 1)
    } else if (windowManager != null) {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val w = minOf(metrics.widthPixels, metrics.heightPixels)
        val h = maxOf(metrics.widthPixels, metrics.heightPixels)
        Pair(if (w % 2 == 0) w else w - 1, if (h % 2 == 0) h else h - 1)
    } else {
        Pair(1080, 1920)
    }

    val aspect = screenHeight.toDouble() / screenWidth.toDouble().coerceAtLeast(1.0)

    fun calculateHeight(targetWidth: Int): Int {
        val calculated = (targetWidth * aspect).toInt()
        return if (calculated % 2 == 0) calculated else calculated + 1
    }

    return listOf(
        ResolutionPreset(
            id = "native",
            name = "Nativa (${screenWidth}p)",
            width = screenWidth,
            height = screenHeight,
            label = "Pantalla",
            recommendedBitrateMbps = if (screenWidth >= 1080) 12 else 8,
            isNative = true
        ),
        ResolutionPreset(
            id = "1080p",
            name = "1080p FHD",
            width = 1080,
            height = calculateHeight(1080),
            label = "FHD",
            recommendedBitrateMbps = 12
        ),
        ResolutionPreset(
            id = "720p",
            name = "720p HD",
            width = 720,
            height = calculateHeight(720),
            label = "HD",
            recommendedBitrateMbps = 8
        ),
        ResolutionPreset(
            id = "480p",
            name = "480p SD",
            width = 480,
            height = calculateHeight(480),
            label = "SD",
            recommendedBitrateMbps = 4
        ),
        ResolutionPreset(
            id = "1440p",
            name = "1440p 2K",
            width = 1440,
            height = calculateHeight(1440),
            label = "2K",
            recommendedBitrateMbps = 24
        )
    )
}

val BITRATE_PRESETS = listOf(4, 8, 12, 16, 24, 35, 50)
val FPS_PRESETS = listOf(24, 30, 60, 90, 120)
val COUNTDOWN_PRESETS = listOf(0, 3, 5, 10)

enum class NativeTab {
    RECORDER, SETTINGS, GALLERY, BENCHMARK
}
