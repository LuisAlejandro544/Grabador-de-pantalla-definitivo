package com.velorec.recorder.data.repository

/**
 * UserPreferencesRepository.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Gestiona la persistencia local de todas las preferencias y configuraciones técnicas
 * que el usuario selecciona en VeloRec Studio (motor C++23/Rust, resolución nativa o personalizada,
 * bitrate, tasa de cuadros FPS, códec de hardware, fuente de audio y cuenta regresiva).
 * 
 * LÓGICA Y RESPONSABILIDADES:
 * 1. Utiliza SharedPreferences de Android en modo privado (cifrado a nivel de sandbox del sistema).
 * 2. Guarda los cambios de manera no bloqueante y asíncrona (apply()) sin congelar el hilo UI de Compose.
 * 3. Restaura fielmente resoluciones Nativas de pantalla, Estándar populares y Personalizadas
 *    manteniendo la paridad de hardware obligatoria para el codificador YUV420p.
 * 4. Proporciona valores por defecto seguros para la primera apertura de la app.
 */

import android.content.Context
import android.content.SharedPreferences
import com.velorec.recorder.data.model.ResolutionPreset
import com.velorec.recorder.data.model.STANDARD_POPULAR_PRESETS
import com.velorec.recorder.data.model.createCustomResolutionPreset

object UserPreferencesRepository {

    private const val PREFS_NAME = "velorec_user_preferences"

    // Claves de configuración
    private const val KEY_ENGINE = "pref_engine"
    private const val KEY_BITRATE_MBPS = "pref_bitrate_mbps"
    private const val KEY_FPS = "pref_fps"
    private const val KEY_CODEC = "pref_codec"
    private const val KEY_AUDIO_SOURCE = "pref_audio_source"
    private const val KEY_COUNTDOWN_SEC = "pref_countdown_sec"

    // Claves para persistir la resolución seleccionada
    private const val KEY_RES_ID = "pref_res_id"
    private const val KEY_RES_NAME = "pref_res_name"
    private const val KEY_RES_WIDTH = "pref_res_width"
    private const val KEY_RES_HEIGHT = "pref_res_height"
    private const val KEY_RES_LABEL = "pref_res_label"
    private const val KEY_RES_BITRATE = "pref_res_bitrate"
    private const val KEY_RES_IS_NATIVE = "pref_res_is_native"
    private const val KEY_RES_SCALE_PERCENT = "pref_res_scale_percent"
    private const val KEY_RES_ASPECT_RATIO = "pref_res_aspect_ratio"
    private const val KEY_RES_CATEGORY = "pref_res_category"
    private const val KEY_RES_IS_CUSTOM = "pref_res_is_custom"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // =========================================================================
    // MOTOR NATIVO (C++23 vs Rust 2024)
    // =========================================================================
    fun saveEngine(context: Context, engine: String) {
        getPrefs(context).edit().putString(KEY_ENGINE, engine).apply()
    }

    fun loadEngine(context: Context): String {
        return getPrefs(context).getString(KEY_ENGINE, "cpp23") ?: "cpp23"
    }

    // =========================================================================
    // TASA DE BITS (BITRATE EN MBPS)
    // =========================================================================
    fun saveBitrate(context: Context, bitrateMbps: Int) {
        getPrefs(context).edit().putInt(KEY_BITRATE_MBPS, bitrateMbps).apply()
    }

    fun loadBitrate(context: Context): Int {
        return getPrefs(context).getInt(KEY_BITRATE_MBPS, 12)
    }

    // =========================================================================
    // TASA DE CUADROS POR SEGUNDO (FPS)
    // =========================================================================
    fun saveFps(context: Context, fps: Int) {
        getPrefs(context).edit().putInt(KEY_FPS, fps).apply()
    }

    fun loadFps(context: Context): Int {
        return getPrefs(context).getInt(KEY_FPS, 60)
    }

    // =========================================================================
    // CÓDEC DE HARDWARE (H264 / HEVC / AV1)
    // =========================================================================
    fun saveCodec(context: Context, codec: String) {
        getPrefs(context).edit().putString(KEY_CODEC, codec).apply()
    }

    fun loadCodec(context: Context): String {
        return getPrefs(context).getString(KEY_CODEC, "H264") ?: "H264"
    }

    // =========================================================================
    // FUENTE DE CAPTURA DE AUDIO
    // =========================================================================
    fun saveAudioSource(context: Context, audioSource: String) {
        getPrefs(context).edit().putString(KEY_AUDIO_SOURCE, audioSource).apply()
    }

    fun loadAudioSource(context: Context): String {
        return getPrefs(context).getString(KEY_AUDIO_SOURCE, "both") ?: "both"
    }

    // =========================================================================
    // CUENTA REGRESIVA DE INICIO
    // =========================================================================
    fun saveCountdownSec(context: Context, countdownSec: Int) {
        getPrefs(context).edit().putInt(KEY_COUNTDOWN_SEC, countdownSec).apply()
    }

    fun loadCountdownSec(context: Context): Int {
        return getPrefs(context).getInt(KEY_COUNTDOWN_SEC, 3)
    }

    // =========================================================================
    // RESOLUCIÓN DE GRABACIÓN (NATIVAS, ESTÁNDAR O PERSONALIZADAS)
    // =========================================================================
    fun saveResolution(context: Context, resolution: ResolutionPreset) {
        getPrefs(context).edit()
            .putString(KEY_RES_ID, resolution.id)
            .putString(KEY_RES_NAME, resolution.name)
            .putInt(KEY_RES_WIDTH, resolution.width)
            .putInt(KEY_RES_HEIGHT, resolution.height)
            .putString(KEY_RES_LABEL, resolution.label)
            .putInt(KEY_RES_BITRATE, resolution.recommendedBitrateMbps)
            .putBoolean(KEY_RES_IS_NATIVE, resolution.isNative)
            .putInt(KEY_RES_SCALE_PERCENT, resolution.scalePercent)
            .putString(KEY_RES_ASPECT_RATIO, resolution.aspectRatioLabel)
            .putString(KEY_RES_CATEGORY, resolution.category)
            .putBoolean(KEY_RES_IS_CUSTOM, resolution.isCustom)
            .apply()
    }

    fun loadResolution(context: Context, adaptivePresets: List<ResolutionPreset>): ResolutionPreset {
        val prefs = getPrefs(context)
        val savedId = prefs.getString(KEY_RES_ID, null)

        // Si no hay configuración guardada previa, usar la resolución nativa 100% de la pantalla
        if (savedId.isNullOrBlank()) {
            return adaptivePresets.firstOrNull { it.isNative } ?: adaptivePresets.first()
        }

        // 1. Intentar coincidencia con las resoluciones nativas del dispositivo actual
        val matchingNative = adaptivePresets.find { it.id == savedId }
        if (matchingNative != null) {
            return matchingNative
        }

        // 2. Intentar coincidencia con la lista de resoluciones estándar de la industria
        val matchingStandard = STANDARD_POPULAR_PRESETS.find { it.id == savedId }
        if (matchingStandard != null) {
            return matchingStandard
        }

        // 3. Si era una resolución personalizada o no coincide, reconstruir a partir de los valores guardados
        val isCustom = prefs.getBoolean(KEY_RES_IS_CUSTOM, false) || savedId.startsWith("custom_")
        val savedWidth = prefs.getInt(KEY_RES_WIDTH, 1080)
        val savedHeight = prefs.getInt(KEY_RES_HEIGHT, 2400)
        val savedName = prefs.getString(KEY_RES_NAME, null)

        if (isCustom) {
            return createCustomResolutionPreset(savedWidth, savedHeight, savedName)
        }

        // 4. Reconstrucción genérica con valores almacenados
        return ResolutionPreset(
            id = savedId,
            name = savedName ?: "Resolución ($savedWidth x $savedHeight)",
            width = if (savedWidth % 2 == 0) savedWidth else savedWidth - 1,
            height = if (savedHeight % 2 == 0) savedHeight else savedHeight - 1,
            label = prefs.getString(KEY_RES_LABEL, "STD") ?: "STD",
            recommendedBitrateMbps = prefs.getInt(KEY_RES_BITRATE, 12),
            isNative = prefs.getBoolean(KEY_RES_IS_NATIVE, false),
            scalePercent = prefs.getInt(KEY_RES_SCALE_PERCENT, 100),
            aspectRatioLabel = prefs.getString(KEY_RES_ASPECT_RATIO, "") ?: "",
            category = prefs.getString(KEY_RES_CATEGORY, "Estándar") ?: "Estándar",
            isCustom = isCustom
        )
    }
}
