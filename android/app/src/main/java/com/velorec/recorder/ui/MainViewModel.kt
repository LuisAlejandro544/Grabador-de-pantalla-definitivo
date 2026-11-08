package com.velorec.recorder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velorec.recorder.data.model.NativeRecordingConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * MainViewModel.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * ViewModel de arquitectura Jetpack que preserva el estado de grabación de pantalla
 * (temporizador REC, estado activo, pausa y parámetros pendientes) ante rotaciones
 * de pantalla o cambios de configuración en smartphones Android.
 * 
 * LÓGICA Y CORRUTINAS:
 * 1. Utiliza 'StateFlow' reactivo para emitir cambios de estado atómicos a la UI de Compose.
 * 2. Gestiona el ciclo de vida del cronómetro REC mm:ss mediante un Job cancelable en 'viewModelScope',
 *    evitando fugas de memoria (memory leaks) si la Activity se recrea.
 */
class MainViewModel : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    private val _pendingConfig = MutableStateFlow<NativeRecordingConfig?>(null)
    val pendingConfig: StateFlow<NativeRecordingConfig?> = _pendingConfig.asStateFlow()

    // Estado de cuenta regresiva posterior a la aprobación de la notificación nativa de Android
    private val _isCountingDown = MutableStateFlow(false)
    val isCountingDown: StateFlow<Boolean> = _isCountingDown.asStateFlow()

    private val _countdownRemaining = MutableStateFlow(3)
    val countdownRemaining: StateFlow<Int> = _countdownRemaining.asStateFlow()

    private var countdownJob: Job? = null
    private var timerJob: Job? = null

    fun setPendingConfig(config: NativeRecordingConfig?) {
        _pendingConfig.value = config
    }

    /**
     * Inicia el temporizador de cuenta regresiva en pantalla UNA VEZ aprobado el diálogo nativo de MediaProjection.
     */
    fun startCountdown(seconds: Int, onComplete: () -> Unit) {
        countdownJob?.cancel()
        if (seconds <= 0) {
            _isCountingDown.value = false
            onComplete()
            return
        }

        _isCountingDown.value = true
        _countdownRemaining.value = seconds
        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1
                _countdownRemaining.value = remaining
            }
            _isCountingDown.value = false
            onComplete()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _isCountingDown.value = false
    }

    fun onRecordingStarted() {
        _isRecording.value = true
        _isPaused.value = false
        _durationSec.value = 0
        startTimerJob()
    }

    fun onPauseToggled(): Boolean {
        val newPaused = !_isPaused.value
        _isPaused.value = newPaused
        return newPaused
    }

    fun onRecordingStopped() {
        _isRecording.value = false
        _isPaused.value = false
        _durationSec.value = 0
        timerJob?.cancel()
        timerJob = null
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (_isRecording.value && !_isPaused.value) {
                    _durationSec.value += 1
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        timerJob?.cancel()
    }
}
