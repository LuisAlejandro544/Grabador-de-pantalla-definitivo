package com.velorec.recorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.velorec.recorder.MainActivity
import com.velorec.recorder.ScreenRecordService

/**
 * RecordingNotificationHelper.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Encapsula la gestión de notificaciones y canales de notificación para el Foreground Service
 * de VeloRec Studio, cumpliendo con las políticas de Android 8.0 Oreo (API 26) hasta Android 15 (API 35).
 * 
 * LÓGICA Y SEGURIDAD:
 * 1. Crea el canal con importancia baja (IMPORTANCE_LOW) para no emitir sonidos intrusivos
 *    mientras el usuario graba partidas de juegos o videos en pantalla.
 * 2. Construye la notificación persistente fija (ongoing) con icono del sistema y metadatos del motor (C++23 / Rust 2024).
 * 3. Provee acciones rápidas de 'Detener' y 'Pausar/Reanudar' directamente en la barra de estado de Android
 *    para permitir controlar la grabación sin necesidad de volver a abrir la pantalla principal.
 * 4. Configura 'PendingIntent' seguro con 'FLAG_IMMUTABLE' para abrir 'MainActivity' al tocar la notificación.
 */
object RecordingNotificationHelper {

    const val CHANNEL_ID = "velorec_recording_channel"
    const val NOTIFICATION_ID = 8801

    fun createNotificationChannel(context: Context) {
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

    fun buildRecordingNotification(
        context: Context,
        engine: String,
        width: Int,
        height: Int,
        fps: Int,
        isPaused: Boolean = false,
        countdownRemaining: Int = 0
    ): Notification {
        val engineLabel = if (engine == "rust2024") "Rust 2024" else "C++23"
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Intent para abrir MainActivity al presionar la notificación
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, flags)

        // Intent para detener o cancelar la grabación desde la barra de notificaciones
        val stopIntent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(context, 1, stopIntent, flags)

        // Intent para pausar o reanudar
        val pauseResumeIntent = Intent(context, ScreenRecordService::class.java).apply {
            action = if (isPaused) ScreenRecordService.ACTION_RESUME else ScreenRecordService.ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(context, 2, pauseResumeIntent, flags)

        val title = when {
            countdownRemaining > 0 -> "Iniciando grabación en ${countdownRemaining}s..."
            isPaused -> "Grabación en pausa"
            else -> "Grabando pantalla"
        }

        val contentText = when {
            countdownRemaining > 0 -> "Prepárate: motor $engineLabel | ${width}x${height} @ ${fps}fps"
            else -> "Motor: $engineLabel | ${width}x${height} @ ${fps}fps"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Si la cuenta regresiva está activa, solo muestra acción de cancelar; al grabar, añade pausa/detener
        if (countdownRemaining == 0) {
            builder.addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Reanudar" else "Pausar",
                pauseResumePendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Detener",
                stopPendingIntent
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancelar",
                stopPendingIntent
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return builder.build()
    }
}
