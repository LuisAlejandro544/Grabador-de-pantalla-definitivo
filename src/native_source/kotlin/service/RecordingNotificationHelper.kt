package com.velorec.recorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * RecordingNotificationHelper.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Encapsula la gestión de notificaciones y canales de notificación para el Foreground Service
 * de VeloRec Studio, cumpliendo con las políticas de Android 8.0 Oreo (API 26) hasta Android 15 (API 35).
 * 
 * LÓGICA Y SEGURIDAD:
 * 1. Crea el canal con importancia baja (IMPORTANCE_LOW) para que no emita sonidos intrusivos
 *    mientras el usuario graba partidas de juegos o contenido en su pantalla.
 * 2. Construye la notificación fija con icono, etiqueta del motor (C++23 o Rust 2024),
 *    resolución activa y tasa de cuadros por segundo.
 */
object RecordingNotificationHelper {

    const val CHANNEL_ID = "velorec_recording_channel"
    const val NOTIFICATION_ID = 8801

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Grabación de Pantalla VeloRec",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el estado activo de grabación con C++23 / Rust"
                setShowBadge(false)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun buildRecordingNotification(
        context: Context,
        engine: String,
        width: Int,
        height: Int,
        fps: Int
    ): Notification {
        val engineLabel = if (engine == "rust2024") "Rust 2024" else "C++23"
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Grabando pantalla")
            .setContentText("Motor: $engineLabel | ${width}x${height} @ ${fps}fps")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
