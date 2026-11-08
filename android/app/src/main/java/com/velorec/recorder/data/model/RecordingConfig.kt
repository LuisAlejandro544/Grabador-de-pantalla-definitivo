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

data class AudioSourcePreset(
    val id: String,
    val name: String,
    val label: String,
    val description: String,
    val iconEmoji: String = "🔊"
)

val AUDIO_SOURCE_PRESETS = listOf(
    AudioSourcePreset("internal", "Audio Interno (Juegos/Apps)", "Interno", "Captura directa del sonido del juego o app sin ruidos del ambiente", "🔊"),
    AudioSourcePreset("both", "Mezcla Estéreo (Mic + Interno)", "Mezcla", "Graba tu voz y el sonido del juego simultáneamente", "🎧"),
    AudioSourcePreset("mic", "Solo Micrófono", "Micrófono", "Captura únicamente tu voz mediante el micrófono del teléfono", "🎙️"),
    AudioSourcePreset("mute", "Silencio (Mute)", "Silencio", "Genera el video sin pista de audio para máximo ahorro de espacio", "🔇")
)

data class NativeRecordingConfig(
    val engine: String = "cpp23",
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 60,
    val bitrateBps: Int = 12_000_000,
    val codec: String = "H264",
    val audioSource: String = "internal",
    val countdownSec: Int = 3
)

data class ResolutionPreset(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val label: String,
    val recommendedBitrateMbps: Int,
    val isNative: Boolean = false,
    val scalePercent: Int = 100,
    val aspectRatioLabel: String = "",
    val category: String = "Estándar",
    val isCustom: Boolean = false
)

val RESOLUTION_PRESETS = listOf(
    ResolutionPreset("native_100", "Nativa 100% (Pantalla)", 1080, 2400, "Real", 14, isNative = true, scalePercent = 100, aspectRatioLabel = "Nativa", category = "Nativa"),
    ResolutionPreset("native_fhd", "Nativa FHD Adaptada", 1080, 2400, "FHD", 12, isNative = true, scalePercent = 100, aspectRatioLabel = "Nativa", category = "Nativa"),
    ResolutionPreset("native_hd", "Nativa HD Adaptada", 720, 1600, "HD", 8, isNative = true, scalePercent = 67, aspectRatioLabel = "Nativa", category = "Nativa"),
    ResolutionPreset("native_sd", "Nativa Ahorro (Gaming)", 540, 1200, "SD", 4, isNative = true, scalePercent = 50, aspectRatioLabel = "Nativa", category = "Nativa"),
    ResolutionPreset("1080p", "1080p FHD (9:16)", 1080, 1920, "FHD", 12, aspectRatioLabel = "9:16", category = "9:16"),
    ResolutionPreset("720p", "720p HD (9:16)", 720, 1280, "HD", 8, aspectRatioLabel = "9:16", category = "9:16"),
    ResolutionPreset("480p", "480p SD (9:16)", 480, 854, "SD", 4, aspectRatioLabel = "9:16", category = "9:16"),
    ResolutionPreset("1440p", "1440p 2K (9:16)", 1440, 2560, "2K", 24, aspectRatioLabel = "9:16", category = "9:16"),
    ResolutionPreset("4k", "4K UHD (9:16)", 2160, 3840, "4K", 35, aspectRatioLabel = "9:16", category = "9:16")
)

/**
 * Preajustes estándar más utilizados en dispositivos Android modernos y redes sociales.
 * Incluye formatos 20:9, 19.5:9 (13:6), 19:9, 21:9 (7:3) y 9:16 vertical universal.
 */
val STANDARD_POPULAR_PRESETS: List<ResolutionPreset> = listOf(
    // 20:9 (Xiaomi, Redmi, Realme, Poco, Tecno, Samsung A)
    ResolutionPreset("std_2400x1080", "2400 x 1080 (FHD+ 20:9)", 1080, 2400, "FHD+", 12, aspectRatioLabel = "20:9", category = "20:9"),
    ResolutionPreset("std_1600x720", "1600 x 720 (HD+ 20:9)", 720, 1600, "HD+", 8, aspectRatioLabel = "20:9", category = "20:9"),
    ResolutionPreset("std_1200x540", "1200 x 540 (Ahorro 20:9)", 540, 1200, "SD", 4, aspectRatioLabel = "20:9", category = "20:9"),

    // 19.5:9 / 13:6 (iPhone, Galaxy S20-S24, Redmi Note)
    ResolutionPreset("std_2340x1080", "2340 x 1080 (FHD+ 19.5:9)", 1080, 2340, "FHD+", 12, aspectRatioLabel = "19.5:9", category = "19.5:9"),
    ResolutionPreset("std_1560x720", "1560 x 720 (HD+ 19.5:9)", 720, 1560, "HD+", 8, aspectRatioLabel = "19.5:9", category = "19.5:9"),
    ResolutionPreset("std_1040x480", "1040 x 480 (SD+ 19.5:9)", 480, 1040, "SD", 4, aspectRatioLabel = "19.5:9", category = "19.5:9"),
    ResolutionPreset("std_780x360", "780 x 360 (Ultra Ahorro 19.5:9)", 360, 780, "MIN", 3, aspectRatioLabel = "19.5:9", category = "19.5:9"),

    // 19:9 (Galaxy S10/Note 10)
    ResolutionPreset("std_3040x1440", "3040 x 1440 (QHD+ 19:9)", 1440, 3040, "2K+", 24, aspectRatioLabel = "19:9", category = "19:9"),

    // 21:9 / 7:3 (Cinemático y Ultra Panorámico)
    ResolutionPreset("std_2520x1080", "2520 x 1080 (Cinemático 21:9)", 1080, 2520, "21:9", 14, aspectRatioLabel = "21:9", category = "21:9"),

    // 9:16 (Universal para Shorts, Reels, TikTok y YouTube)
    ResolutionPreset("std_4k_916", "2160 x 3840 (4K UHD 9:16)", 2160, 3840, "4K", 35, aspectRatioLabel = "9:16", category = "9:16"),
    ResolutionPreset("std_2k_916", "1440 x 2560 (2K QHD 9:16)", 1440, 2560, "2K", 24, aspectRatioLabel = "9:16", category = "9:16"),
    ResolutionPreset("1080p", "1080 x 1920 (FHD 9:16)", 1080, 1920, "FHD", 12, aspectRatioLabel = "9:16", category = "9:16"),
    ResolutionPreset("720p", "720 x 1280 (HD 9:16)", 720, 1280, "HD", 8, aspectRatioLabel = "9:16", category = "9:16"),
    ResolutionPreset("480p", "480 x 854 (SD 9:16)", 480, 854, "SD", 4, aspectRatioLabel = "9:16", category = "9:16")
)

/**
 * Crea una resolución personalizada garantizando dimensiones pares (% 2 == 0)
 * para evitar fallos catastróficos en codificadores de hardware AMediaCodec.
 */
fun createCustomResolutionPreset(
    width: Int,
    height: Int,
    customName: String? = null
): ResolutionPreset {
    val cleanW = maxOf(240, if (width % 2 == 0) width else width - 1)
    val cleanH = maxOf(240, if (height % 2 == 0) height else height - 1)
    val aspectDecimal = cleanH.toDouble() / cleanW.toDouble().coerceAtLeast(1.0)

    val ratioLabel = when {
        Math.abs(aspectDecimal - (20.0 / 9.0)) < 0.025 -> "20:9"
        Math.abs(aspectDecimal - (19.5 / 9.0)) < 0.025 -> "19.5:9"
        Math.abs(aspectDecimal - (19.0 / 9.0)) < 0.025 -> "19:9"
        Math.abs(aspectDecimal - (21.0 / 9.0)) < 0.025 -> "21:9"
        Math.abs(aspectDecimal - (16.0 / 9.0)) < 0.025 -> "9:16"
        else -> String.format(java.util.Locale.US, "%.2f:1", aspectDecimal)
    }

    val recommendedBitrate = when {
        cleanW >= 2160 -> 35
        cleanW >= 1440 -> 24
        cleanW >= 1080 -> 12
        cleanW >= 720 -> 8
        else -> 4
    }

    val name = customName ?: "Personalizada (${cleanW}x${cleanH})"

    return ResolutionPreset(
        id = "custom_${cleanW}x${cleanH}",
        name = name,
        width = cleanW,
        height = cleanH,
        label = "Custom",
        recommendedBitrateMbps = recommendedBitrate,
        isNative = false,
        scalePercent = 100,
        aspectRatioLabel = ratioLabel,
        category = "Personalizada",
        isCustom = true
    )
}

/**
 * Genera resoluciones adaptadas a la relación de aspecto real de la pantalla del smartphone del usuario.
 * Garantiza que ancho y alto sean números pares (múltiplos de 2) y previene barras verdes
 * por desajuste entre VirtualDisplay y la superficie física del dispositivo.
 * Permite seleccionar diferentes resoluciones y calidades conservando el aspect ratio nativo de la pantalla.
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
        Pair(1080, 2400)
    }

    val aspect = screenHeight.toDouble() / screenWidth.toDouble().coerceAtLeast(1.0)

    fun calculateHeight(targetWidth: Int): Int {
        val calculated = (targetWidth * aspect).toInt()
        return if (calculated % 2 == 0) calculated else calculated + 1
    }

    val screenScaleBase = screenWidth.coerceAtLeast(1)

    return listOf(
        ResolutionPreset(
            id = "native_100",
            name = "Nativa 100% (${screenWidth}x${screenHeight})",
            width = screenWidth,
            height = screenHeight,
            label = "Pantalla",
            recommendedBitrateMbps = if (screenWidth >= 1080) 14 else 8,
            isNative = true,
            scalePercent = 100,
            aspectRatioLabel = "Nativa",
            category = "Nativa"
        ),
        ResolutionPreset(
            id = "native_fhd",
            name = "Nativa FHD Adaptada",
            width = 1080,
            height = calculateHeight(1080),
            label = "FHD",
            recommendedBitrateMbps = 12,
            isNative = true,
            scalePercent = minOf(100, (1080 * 100 / screenScaleBase)),
            aspectRatioLabel = "Nativa",
            category = "Nativa"
        ),
        ResolutionPreset(
            id = "native_hd",
            name = "Nativa HD Adaptada",
            width = 720,
            height = calculateHeight(720),
            label = "HD",
            recommendedBitrateMbps = 8,
            isNative = true,
            scalePercent = minOf(100, (720 * 100 / screenScaleBase)),
            aspectRatioLabel = "Nativa",
            category = "Nativa"
        ),
        ResolutionPreset(
            id = "native_sd",
            name = "Nativa Ahorro (Gaming)",
            width = 540,
            height = calculateHeight(540),
            label = "SD",
            recommendedBitrateMbps = 4,
            isNative = true,
            scalePercent = minOf(100, (540 * 100 / screenScaleBase)),
            aspectRatioLabel = "Nativa",
            category = "Nativa"
        ),
        ResolutionPreset(
            id = "native_2k",
            name = "Nativa 2K Ultra",
            width = 1440,
            height = calculateHeight(1440),
            label = "2K",
            recommendedBitrateMbps = 24,
            isNative = true,
            scalePercent = (1440 * 100 / screenScaleBase),
            aspectRatioLabel = "Nativa",
            category = "Nativa"
        )
    )
}

val BITRATE_PRESETS = listOf(4, 8, 12, 16, 24, 35, 50)
val FPS_PRESETS = listOf(24, 30, 60, 90, 120)
val COUNTDOWN_PRESETS = listOf(0, 3, 5, 10)

enum class NativeTab {
    RECORDER, LAUNCHER, SETTINGS, GALLERY, BENCHMARK
}

/**
 * Representa una aplicación o juego instalado en el dispositivo disponible en el Lanzador Rápido.
 */
data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isGame: Boolean = false,
    val category: String = "App",
    val versionName: String = "",
    val icon: android.graphics.drawable.Drawable? = null
)
