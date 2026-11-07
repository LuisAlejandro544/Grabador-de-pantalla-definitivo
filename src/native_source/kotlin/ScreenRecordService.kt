package com.velorec.recorder

import android.app.Activity
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Surface
import com.velorec.recorder.service.RecordingNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * ScreenRecordService.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Servicio en primer plano (Foreground Service) de Android para captura de pantalla de alto rendimiento.
 * 
 * LÓGICA Y ARQUITECTURA MODULAR:
 * 1. Mantiene el proceso activo en segundo plano mientras el usuario navega o juega en su smartphone.
 * 2. CUMPLIMIENTO ESTRICTO CON ANDROID 14+ (API 34/35):
 *    - Registra obligatoriamente 'MediaProjection.Callback' con Handler en el Looper principal.
 *    - Devuelve 'START_STICKY' en 'onStartCommand' para evitar que el sistema cierre el servicio
 *      al cambiar a otra aplicación o videojuego.
 *    - Declara 'FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION'.
 * 3. Delega la creación de canales, notificaciones fijas y controles en la barra de estado
 *    a 'RecordingNotificationHelper'.
 * 4. Tubería ZERO-COPY:
 *    - Obtiene la Surface de hardware directamente del codificador nativo en C++23 o Rust 2024.
 *    - Vincula esa Surface con el VirtualDisplay de MediaProjection.
 *    - CERO alocación de memoria ni bitmaps en la máquina virtual Java/ART.
 * 5. Corrutinas ('Dispatchers.IO'): Todas las llamadas al NDK se ejecutan fuera del hilo de UI
 *    para prevenir congelamientos ANR.
 * 6. Sincronización Reactiva de Estado:
 *    - Expone 'isRecordingFlow', 'isPausedFlow' y 'durationSecFlow' para que la UI de Jetpack Compose
 *      refleje el estado real aunque la Activity haya sido pausada o recreada.
 */
class ScreenRecordService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var timerJob: Job? = null

    // Parámetros de la sesión actual
    private var currentEngine: String = "cpp23"
    private var currentWidth: Int = 1080
    private var currentHeight: Int = 1920
    private var currentFps: Int = 60

    companion object {
        const val ACTION_START = "com.velorec.ACTION_START"
        const val ACTION_PAUSE = "com.velorec.ACTION_PAUSE"
        const val ACTION_RESUME = "com.velorec.ACTION_RESUME"
        const val ACTION_STOP = "com.velorec.ACTION_STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_ENGINE = "extra_engine"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_CODEC = "extra_codec"
        const val EXTRA_AUDIO = "extra_audio"

        var activeEngine: String = "cpp23"
        var lastOutputFilePath: String? = null
        var lastDurationMs: Long = 0L
        var lastFileSizeBytes: Long = 0L

        // Flujos de estado globales observables por MainActivity y MainViewModel
        private val _isRecordingFlow = MutableStateFlow(false)
        val isRecordingFlow: StateFlow<Boolean> = _isRecordingFlow.asStateFlow()

        private val _isPausedFlow = MutableStateFlow(false)
        val isPausedFlow: StateFlow<Boolean> = _isPausedFlow.asStateFlow()

        private val _durationSecFlow = MutableStateFlow(0)
        val durationSecFlow: StateFlow<Int> = _durationSecFlow.asStateFlow()

        // Carga dinámica de las bibliotecas nativas C++23 y Rust 2024
        init {
            try {
                System.loadLibrary("velorec_cpp23")
                System.loadLibrary("velorec_rust2024")
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        RecordingNotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStartRecording(intent)
            ACTION_PAUSE -> handlePauseRecording()
            ACTION_RESUME -> handleResumeRecording()
            ACTION_STOP -> handleStopRecording()
        }
        // START_STICKY para asegurar que el Foreground Service no sea terminado
        // abruptamente cuando el usuario sale de la aplicación a jugar o ver videos
        return START_STICKY
    }

    private fun handleStartRecording(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA) as? Intent
        } ?: return

        val engine = intent.getStringExtra(EXTRA_ENGINE) ?: "cpp23"
        val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
        val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
        val fps = intent.getIntExtra(EXTRA_FPS, 60)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, 12_000_000)
        val codec = intent.getStringExtra(EXTRA_CODEC) ?: "H264"
        val audio = intent.getStringExtra(EXTRA_AUDIO) ?: "both"

        activeEngine = engine
        currentEngine = engine
        currentWidth = width
        currentHeight = height
        currentFps = fps

        _isRecordingFlow.value = true
        _isPausedFlow.value = false
        _durationSecFlow.value = 0

        // Configuración de la notificación persistente
        val notification = RecordingNotificationHelper.buildRecordingNotification(
            this,
            engine,
            width,
            height,
            fps,
            isPaused = false
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(RecordingNotificationHelper.NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(RecordingNotificationHelper.NOTIFICATION_ID, notification)
        }

        // Iniciar temporizador del servicio
        startServiceTimer()

        // Ejecución en hilo de fondo usando Corrutinas (Dispatchers.IO)
        serviceScope.launch(Dispatchers.IO) {
            try {
                val outputDir = getExternalFilesDir(null) ?: filesDir
                val timestamp = System.currentTimeMillis()
                val outputFile = File(outputDir, "VeloRec_${timestamp}_${engine}.mp4")
                lastOutputFilePath = outputFile.absolutePath

                // Inicializar motor nativo elegido (C++23 o Rust 2024) y obtener la Surface de hardware
                val nativeSurface: Surface = if (engine == "rust2024") {
                    nativeInitRustRecorder(
                        outputFile.absolutePath,
                        width,
                        height,
                        fps,
                        bitrate,
                        codec,
                        audio
                    )
                } else {
                    nativeInitCppRecorder(
                        outputFile.absolutePath,
                        width,
                        height,
                        fps,
                        bitrate,
                        codec,
                        audio
                    )
                }

                // Iniciar MediaProjection del sistema
                val projectionManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = projectionManager.getMediaProjection(resultCode, resultData)
                mediaProjection = projection

                // =========================================================================
                // CORRECCIÓN CRÍTICA DE ANDROID 14+ (API 34 / 35):
                // En Android 14+, registrar MediaProjection.Callback con un Handler en el Looper
                // principal es OBLIGATORIO antes de llamar a createVirtualDisplay.
                // Sin este registro, el sistema aborta la proyección de pantalla tan pronto
                // la Activity sale a segundo plano.
                // =========================================================================
                val mainHandler = Handler(Looper.getMainLooper())
                projection.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        handleStopRecording()
                    }
                }, mainHandler)

                // Crear VirtualDisplay conectado directamente a la Surface nativa de hardware (Zero-Copy)
                val densityDpi = resources.displayMetrics.densityDpi
                virtualDisplay = projection.createVirtualDisplay(
                    "VeloRec-Capture",
                    width,
                    height,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    nativeSurface,
                    null,
                    mainHandler
                )

                // Señalizar al motor nativo para comenzar a codificar fotogramas
                if (engine == "rust2024") {
                    nativeStartRustEncoding()
                } else {
                    nativeStartCppEncoding()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                handleStopRecording()
            }
        }
    }

    private fun startServiceTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000L)
                if (_isRecordingFlow.value && !_isPausedFlow.value) {
                    _durationSecFlow.value += 1
                }
            }
        }
    }

    private fun handlePauseRecording() {
        _isPausedFlow.value = true
        updateNotification(isPaused = true)
        serviceScope.launch(Dispatchers.IO) {
            if (activeEngine == "rust2024") {
                nativePauseRustEncoding()
            } else {
                nativePauseCppEncoding()
            }
        }
    }

    private fun handleResumeRecording() {
        _isPausedFlow.value = false
        updateNotification(isPaused = false)
        serviceScope.launch(Dispatchers.IO) {
            if (activeEngine == "rust2024") {
                nativeResumeRustEncoding()
            } else {
                nativeResumeCppEncoding()
            }
        }
    }

    private fun updateNotification(isPaused: Boolean) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = RecordingNotificationHelper.buildRecordingNotification(
            this,
            currentEngine,
            currentWidth,
            currentHeight,
            currentFps,
            isPaused = isPaused
        )
        notificationManager?.notify(RecordingNotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun handleStopRecording() {
        timerJob?.cancel()
        timerJob = null
        _isRecordingFlow.value = false
        _isPausedFlow.value = false

        serviceScope.launch(Dispatchers.IO) {
            try {
                // Detener codificación en el motor nativo en Dispatchers.IO
                if (activeEngine == "rust2024") {
                    nativeStopRustEncoding()
                } else {
                    nativeStopCppEncoding()
                }

                // Liberar VirtualDisplay y MediaProjection
                virtualDisplay?.release()
                virtualDisplay = null
                mediaProjection?.stop()
                mediaProjection = null

                val outFile = lastOutputFilePath?.let { File(it) }
                if (outFile != null && outFile.exists()) {
                    lastFileSizeBytes = outFile.length()
                }

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        timerJob = null
        _isRecordingFlow.value = false
        _isPausedFlow.value = false
        serviceScope.cancel()
    }

    // ==========================================
    // FIRMAS JNI PARA MOTOR C++23
    // (Enlazan con Java_com_velorec_recorder_ScreenRecordService_native*)
    // ==========================================
    private external fun nativeInitCppRecorder(
        path: String, width: Int, height: Int, fps: Int, bitrate: Int, codec: String, audio: String
    ): Surface
    private external fun nativeStartCppEncoding()
    private external fun nativePauseCppEncoding()
    private external fun nativeResumeCppEncoding()
    private external fun nativeStopCppEncoding()

    // ==========================================
    // FIRMAS JNI PARA MOTOR RUST (EDITION 2024)
    // (Enlazan con Java_com_velorec_recorder_ScreenRecordService_native*Rust*)
    // ==========================================
    private external fun nativeInitRustRecorder(
        path: String, width: Int, height: Int, fps: Int, bitrate: Int, codec: String, audio: String
    ): Surface
    private external fun nativeStartRustEncoding()
    private external fun nativePauseRustEncoding()
    private external fun nativeResumeRustEncoding()
    private external fun nativeStopRustEncoding()
}
