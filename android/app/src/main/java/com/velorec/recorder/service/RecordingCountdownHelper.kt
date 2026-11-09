package com.velorec.recorder.service

import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * RecordingCountdownHelper.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Gestiona el ciclo de vida de la cuenta regresiva de inicio (3s, 5s, 10s) dentro
 * del Foreground Service antes de inyectar el VirtualDisplay del codificador nativo.
 * 
 * LÓGICA Y ARQUITECTURA MODULAR:
 * 1. Desacopla el bucle asíncrono de retraso temporal de 'ScreenRecordService'.
 * 2. Actualiza la notificación de la barra de estado en cada segundo transcurrido.
 * 3. Notifica los cambios de estado a la UI de Compose y detecta cancelaciones anticipadas.
 */
object RecordingCountdownHelper {

    /**
     * Ejecuta el ciclo de cuenta atrás en el hilo de trabajo.
     * Retorna 'true' si la cuenta finalizó exitosamente, o 'false' si fue cancelada por el usuario.
     */
    suspend fun runCountdown(
        context: Context,
        countdownSec: Int,
        engine: String,
        width: Int,
        height: Int,
        fps: Int,
        onTick: (remaining: Int) -> Unit,
        isStillActive: () -> Boolean
    ): Boolean {
        if (countdownSec <= 0) return true

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        var remaining = countdownSec

        while (remaining > 0 && coroutineContext.isActive && isStillActive()) {
            onTick(remaining)
            notificationManager?.notify(
                RecordingNotificationHelper.NOTIFICATION_ID,
                RecordingNotificationHelper.buildRecordingNotification(
                    context,
                    engine,
                    width,
                    height,
                    fps,
                    isPaused = false,
                    countdownRemaining = remaining
                )
            )
            delay(1000L)
            remaining -= 1
        }

        onTick(0)
        return coroutineContext.isActive && isStillActive()
    }
}
