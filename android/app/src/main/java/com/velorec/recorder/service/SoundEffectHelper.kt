package com.velorec.recorder.service

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

/**
 * SoundEffectHelper.kt
 *
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Reproductor de efectos de sonido de ultra baja latencia basado en 'SoundPool'
 * para retroalimentación auditiva (cuenta regresiva, inicio y fin de grabación).
 *
 * LÓGICA Y AISLAMIENTO:
 * 1. Usa AudioAttributes.USAGE_ASSISTANCE_SONIFICATION para mantenerse separado
 *    del canal multimedia del videojuego.
 * 2. Resuelve los recursos en 'res/raw/' dinámicamente mediante 'resources.getIdentifier'
 *    para que la app compile sin errores incluso si los audios no están presentes en local.
 */
object SoundEffectHelper {

    private const val TAG = "SoundEffectHelper"

    private var soundPool: SoundPool? = null
    private var startSoundId: Int = 0
    private var stopSoundId: Int = 0
    private var tickSoundId: Int = 0

    @Synchronized
    fun preload(context: Context) {
        if (soundPool != null) return
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build()

            val pkg = context.packageName
            val res = context.resources

            val startRes = res.getIdentifier("rec_start", "raw", pkg)
            val stopRes = res.getIdentifier("rec_stop", "raw", pkg)
            val tickRes = res.getIdentifier("countdown_tick", "raw", pkg)

            if (startRes != 0) startSoundId = pool.load(context, startRes, 1)
            if (stopRes != 0) stopSoundId = pool.load(context, stopRes, 1)
            if (tickRes != 0) tickSoundId = pool.load(context, tickRes, 1)

            soundPool = pool
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron precargar efectos de sonido: ${e.message}")
        }
    }

    fun playStart() {
        try {
            if (startSoundId != 0) {
                soundPool?.play(startSoundId, 0.75f, 0.75f, 1, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    fun playStop() {
        try {
            if (stopSoundId != 0) {
                soundPool?.play(stopSoundId, 0.75f, 0.75f, 1, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    fun playTick() {
        try {
            if (tickSoundId != 0) {
                soundPool?.play(tickSoundId, 0.55f, 0.55f, 1, 0, 1.0f)
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    fun release() {
        try {
            soundPool?.release()
        } catch (_: Exception) {}
        soundPool = null
        startSoundId = 0
        stopSoundId = 0
        tickSoundId = 0
    }
}
