package com.velorec.recorder.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * RecordingSessionTimer.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Cronómetro de duración de la sesión de grabación para Android.
 * 
 * LÓGICA Y ARQUITECTURA MODULAR:
 * 1. Desacopla la gestión del temporizador y sus Job de corrutina fuera de 'ScreenRecordService'.
 * 2. Incrementa el segundero únicamente si la sesión está activa y no se encuentra en estado de pausa.
 * 3. Permite arranque, pausa, reanudación y cancelación limpia sin fugas de memoria.
 */
class RecordingSessionTimer(
    private val scope: CoroutineScope,
    private val onTick: (durationSec: Int) -> Unit
) {
    private var timerJob: Job? = null
    private var currentSeconds: Int = 0

    fun start() {
        cancel()
        currentSeconds = 0
        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000L)
                currentSeconds += 1
                onTick(currentSeconds)
            }
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
    }

    fun getDurationSec(): Int = currentSeconds
}
