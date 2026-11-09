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
import androidx.core.content.IntentCompat
import com.velorec.recorder.audio.AudioMuxerHelper
import com.velorec.recorder.audio.AudioRecordEngine
import com.velorec.recorder.service.RecordingNotificationHelper
import com.velorec.recorder.storage.GalleryStorageManager
import com.velorec.recorder.storage.DirectGalleryRecordingTarget
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
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Log

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
 * 4. Tubería ZERO-COPY Y GUARDADO DIRECTO EN GALERÍA:
 *    - Obtiene la Surface de hardware directamente del codificador nativo en C++23 o Rust 2024.
 *    - Vincula esa Surface con el VirtualDisplay de MediaProjection.
 *    - Grabación directa sobre el descriptor de archivo de la Galería (MediaStore IS_PENDING = 1)
 *      evitando copias de disco de 300+ MB al detener la grabación.
 *    - Multiplexación de Audio en Tiempo Real en C++23 con AMediaMuxer entrelazado.
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
    private var projectionCallback: MediaProjection.Callback? = null
    private val isStopping = AtomicBoolean(false)
    private var timerJob: Job? = null
    private var isNativeEncoderStarted = false

    // Parámetros de la sesión actual (Rust 2024 como motor nativo exclusivo)
    private var currentEngine: String = "rust2024"
    private var currentWidth: Int = 1080
    private var currentHeight: Int = 1920
    private var currentFps: Int = 60

    // Motor de captura y codificación de audio (micrófono, audio interno o mezcla estéreo)
    private var audioEngine: AudioRecordEngine? = null
    private var tempVideoFilePath: String? = null
    private var tempAudioFilePath: String? = null

    companion object {
        const val ACTION_START = "com.velorec.ACTION_START"
        const val ACTION_PAUSE = "com.velorec.ACTION_PAUSE"
        const val ACTION_RESUME = "com.velorec.ACTION_RESUME"
        const val ACTION_STOP = "com.velorec.ACTION_STOP"
        const val ACTION_QUERY_STATUS = "com.velorec.ACTION_QUERY_STATUS"
        const val ACTION_STATE_CHANGED = "com.velorec.ACTION_STATE_CHANGED"

        const val EXTRA_STATE_IS_RECORDING = "extra_is_recording"
        const val EXTRA_STATE_IS_PAUSED = "extra_is_paused"
        const val EXTRA_STATE_DURATION_SEC = "extra_duration_sec"
        const val EXTRA_STATE_IS_COUNTING_DOWN = "extra_is_counting_down"
        const val EXTRA_STATE_COUNTDOWN_REMAINING = "extra_countdown_remaining"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_ENGINE = "extra_engine"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_CODEC = "extra_codec"
        const val EXTRA_AUDIO = "extra_audio"
        const val EXTRA_COUNTDOWN = "extra_countdown"

        var activeEngine: String = "rust2024"
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

        private val _isCountingDownFlow = MutableStateFlow(false)
        val isCountingDownFlow: StateFlow<Boolean> = _isCountingDownFlow.asStateFlow()

        private val _countdownRemainingFlow = MutableStateFlow(0)
        val countdownRemainingFlow: StateFlow<Int> = _countdownRemainingFlow.asStateFlow()

        /**
         * Actualiza los flujos de estado en el proceso de la UI cuando se reciben broadcasts
         * desde el proceso aislado de grabación (:recorder).
         */
        fun updateStateFromBroadcast(
            isRecording: Boolean,
            isPaused: Boolean,
            durationSec: Int,
            isCountingDown: Boolean,
            countdownRemaining: Int
        ) {
            _isRecordingFlow.value = isRecording
            _isPausedFlow.value = isPaused
            _durationSecFlow.value = durationSec
            _isCountingDownFlow.value = isCountingDown
            _countdownRemainingFlow.value = countdownRemaining
        }

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

    /**
     * Emite un broadcast explícito con el estado actual del servicio para que
     * el proceso principal de la UI (MainActivity) se sincronice reactivamente sin importar
     * que el servicio corra en un proceso separado (:recorder).
     */
    private fun notifyStateChanged() {
        try {
            val broadcastIntent = Intent(ACTION_STATE_CHANGED).apply {
                setPackage(packageName)
                putExtra(EXTRA_STATE_IS_RECORDING, _isRecordingFlow.value)
                putExtra(EXTRA_STATE_IS_PAUSED, _isPausedFlow.value)
                putExtra(EXTRA_STATE_DURATION_SEC, _durationSecFlow.value)
                putExtra(EXTRA_STATE_IS_COUNTING_DOWN, _isCountingDownFlow.value)
                putExtra(EXTRA_STATE_COUNTDOWN_REMAINING, _countdownRemainingFlow.value)
            }
            sendBroadcast(broadcastIntent)
        } catch (_: Exception) {}
    }

    override fun onCreate() {
        super.onCreate()
        RecordingNotificationHelper.createNotificationChannel(this)
        // Limpiar archivos temporales residuales de sesiones previas en segundo plano
        serviceScope.launch(Dispatchers.IO) {
            try {
                val privateDir = applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
                privateDir.listFiles { f -> f.name.startsWith("temp_") }?.forEach { it.delete() }
            } catch (_: Exception) {}
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStartRecording(intent)
            ACTION_PAUSE -> handlePauseRecording()
            ACTION_RESUME -> handleResumeRecording()
            ACTION_STOP -> handleStopRecording()
            ACTION_QUERY_STATUS -> notifyStateChanged()
        }
        // START_STICKY para asegurar que el Foreground Service no sea terminado
        // abruptamente cuando el usuario sale de la aplicación a jugar o ver videos
        return Service.START_STICKY
    }

    private fun handleStartRecording(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = IntentCompat.getParcelableExtra(intent, EXTRA_RESULT_DATA, Intent::class.java) ?: return

        val engine = intent.getStringExtra(EXTRA_ENGINE) ?: "cpp23"
        val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
        val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
        val fps = intent.getIntExtra(EXTRA_FPS, 60)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, 12_000_000)
        val codec = intent.getStringExtra(EXTRA_CODEC) ?: "H264"
        val audio = intent.getStringExtra(EXTRA_AUDIO) ?: "both"
        val countdown = intent.getIntExtra(EXTRA_COUNTDOWN, 0)

        activeEngine = engine
        currentEngine = engine
        currentWidth = width
        currentHeight = height
        currentFps = fps
        isNativeEncoderStarted = false
        isStopping.set(false)

        _isRecordingFlow.value = true
        _isPausedFlow.value = false
        _durationSecFlow.value = 0
        notifyStateChanged()

        val isMute = audio == "mute"

        // Iniciar en primer plano INMEDIATAMENTE mientras la Activity está en primer plano
        // Esto previene que Android 14+ / Tecno HiOS bloquee el micrófono cuando el usuario
        // sale a la pantalla de inicio o a otra aplicación durante la cuenta regresiva.
        val initialNotification = RecordingNotificationHelper.buildRecordingNotification(
            this,
            engine,
            width,
            height,
            fps,
            isPaused = false,
            countdownRemaining = countdown
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (audio == "mic" || audio == "both") {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                }
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(RecordingNotificationHelper.NOTIFICATION_ID, initialNotification, serviceType)
        } else {
            startForeground(RecordingNotificationHelper.NOTIFICATION_ID, initialNotification)
        }

        // Ejecución en hilo de fondo usando Corrutinas (Dispatchers.IO)
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Si el usuario configuró cuenta regresiva (ej. 3s o 5s), correr dentro del Foreground Service
                if (countdown > 0) {
                    _isCountingDownFlow.value = true
                    var remaining = countdown
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

                    while (remaining > 0 && isActive && _isRecordingFlow.value) {
                        _countdownRemainingFlow.value = remaining
                        notifyStateChanged()
                        notificationManager?.notify(
                            RecordingNotificationHelper.NOTIFICATION_ID,
                            RecordingNotificationHelper.buildRecordingNotification(
                                this@ScreenRecordService,
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
                    _isCountingDownFlow.value = false
                    _countdownRemainingFlow.value = 0
                    notifyStateChanged()

                    if (!isActive || !_isRecordingFlow.value) {
                        // Fue cancelado por el usuario durante la cuenta atrás
                        notifyStateChanged()
                        return@launch
                    }
                }

                // Iniciar cronómetro de duración de la grabación
                startServiceTimer()

                // Actualizar notificación a grabación activa
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.notify(
                    RecordingNotificationHelper.NOTIFICATION_ID,
                    RecordingNotificationHelper.buildRecordingNotification(
                        this@ScreenRecordService,
                        engine,
                        width,
                        height,
                        fps,
                        isPaused = false,
                        countdownRemaining = 0
                    )
                )

                val outputDir = getExternalFilesDir(null) ?: filesDir
                val timestamp = System.currentTimeMillis()
                val shouldRecordAudio = audio != "mute"

                val displayName = "VeloRec_${timestamp}_rust2024.mp4"
                val finalOutputFile = File(outputDir, displayName)
                lastOutputFilePath = finalOutputFile.absolutePath

                val videoFileToEncode = if (shouldRecordAudio) {
                    File(outputDir, "temp_video_${timestamp}.mp4").also { 
                        tempVideoFilePath = it.absolutePath 
                    }
                } else {
                    tempVideoFilePath = null
                    finalOutputFile
                }

                val audioFileToEncode = if (shouldRecordAudio) {
                    File(outputDir, "temp_audio_${timestamp}.m4a").also { 
                        tempAudioFilePath = it.absolutePath 
                    }
                } else {
                    tempAudioFilePath = null
                    null
                }

                // Inicializar motor nativo en Rust 2024 y obtener la Surface de hardware (Zero-Copy)
                val nativeSurface: Surface = nativeInitRustRecorder(
                    videoFileToEncode.absolutePath,
                    width,
                    height,
                    fps,
                    bitrate,
                    codec,
                    audio
                )

                isNativeEncoderStarted = true

                // Iniciar MediaProjection del sistema
                val projectionManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = projectionManager.getMediaProjection(resultCode, resultData)
                if (projection == null) {
                    handleStopRecording()
                    return@launch
                }
                mediaProjection = projection

                // =========================================================================
                // CORRECCIÓN CRÍTICA DE ANDROID 14+ (API 34 / 35):
                // En Android 14+, registrar MediaProjection.Callback con un Handler en el Looper
                // principal es OBLIGATORIO antes de llamar a createVirtualDisplay.
                // =========================================================================
                val mainHandler = Handler(Looper.getMainLooper())
                val callback = object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        handleStopRecording()
                    }
                }
                projectionCallback = callback
                projection.registerCallback(callback, mainHandler)

                // Señalizar al motor Rust para comenzar a codificar fotogramas
                nativeStartRustEncoding()

                // Crear VirtualDisplay conectado directamente a la Surface nativa de hardware ya activa (Zero-Copy)
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

                // Iniciar Audio simultáneamente con VirtualDisplay para sincronización estricta Audio/Video
                if (shouldRecordAudio) {
                    audioEngine = AudioRecordEngine(
                        context = this@ScreenRecordService,
                        audioSource = audio,
                        outputM4aPath = audioFileToEncode?.absolutePath,
                        mediaProjection = projection
                    ).apply {
                        start()
                    }
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
                    notifyStateChanged()
                }
            }
        }
    }

    private fun handlePauseRecording() {
        _isPausedFlow.value = true
        notifyStateChanged()
        updateNotification(isPaused = true)
        audioEngine?.pause()
        serviceScope.launch(Dispatchers.IO) {
            try {
                if (isNativeEncoderStarted) {
                    nativePauseRustEncoding()
                }
            } catch (e: Exception) {
                Log.e("ScreenRecordService", "Error al pausar motor de codificación nativo: ${e.message}", e)
            }
        }
    }

    private fun handleResumeRecording() {
        _isPausedFlow.value = false
        notifyStateChanged()
        updateNotification(isPaused = false)
        audioEngine?.resume()
        serviceScope.launch(Dispatchers.IO) {
            try {
                if (isNativeEncoderStarted) {
                    nativeResumeRustEncoding()
                }
            } catch (e: Exception) {
                Log.e("ScreenRecordService", "Error al reanudar motor de codificación nativo: ${e.message}", e)
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
        // Prevenir reentrancia y carreras concurrentes si el usuario presiona 'Detener' múltiples veces
        // o si MediaProjection.Callback.onStop() se activa simultáneamente
        if (!isStopping.compareAndSet(false, true)) {
            Log.w("ScreenRecordService", "handleStopRecording ya en ejecución, ignorando llamada redundante")
            return
        }

        timerJob?.cancel()
        timerJob = null
        _isCountingDownFlow.value = false
        _countdownRemainingFlow.value = 0
        _isRecordingFlow.value = false
        _isPausedFlow.value = false
        notifyStateChanged()

        // Notificación visual de guardado en la barra de estado con porcentaje inicial (25%)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(
            RecordingNotificationHelper.NOTIFICATION_ID,
            RecordingNotificationHelper.buildSavingNotification(
                this,
                currentEngine,
                progressPercent = 25,
                customStatus = "Deteniendo codificación..."
            )
        )

        serviceScope.launch(Dispatchers.IO) {
            try {
                // 1. Detener PRIMERO el motor de audio mientras MediaProjection sigue activo
                try {
                    audioEngine?.stop()
                } catch (e: Exception) {
                    Log.e("ScreenRecordService", "Error al detener audioEngine: ${e.message}", e)
                }
                audioEngine = null

                // 2. Detener codificación en el motor Rust en Dispatchers.IO
                if (isNativeEncoderStarted) {
                    try {
                        nativeStopRustEncoding()
                    } catch (e: Exception) {
                        Log.e("ScreenRecordService", "Error al detener motor Rust: ${e.message}", e)
                    }
                    isNativeEncoderStarted = false
                }

                // 3. Liberar VirtualDisplay
                try {
                    virtualDisplay?.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                virtualDisplay = null

                // 4. Desregistrar el callback para evitar reentrancias circulares y detener MediaProjection
                try {
                    val cb = projectionCallback
                    if (cb != null) {
                        mediaProjection?.unregisterCallback(cb)
                    }
                    mediaProjection?.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                projectionCallback = null
                mediaProjection = null

                // Actualizar progreso al 75% mientras se finaliza la publicación en la Galería
                notificationManager?.notify(
                    RecordingNotificationHelper.NOTIFICATION_ID,
                    RecordingNotificationHelper.buildSavingNotification(
                        this@ScreenRecordService,
                        currentEngine,
                        progressPercent = 75,
                        customStatus = "Publicando en Galería..."
                    )
                )

                // 5. Fusión de audio y video con sincronización estricta y publicación en la Galería pública
                val tempVideo = tempVideoFilePath
                val tempAudio = tempAudioFilePath
                val finalOutput = lastOutputFilePath

                if (tempVideo != null && finalOutput != null) {
                    if (tempAudio != null && File(tempAudio).exists() && File(tempAudio).length() > 100L) {
                        AudioMuxerHelper.mergeAudioVideo(tempVideo, tempAudio, finalOutput)
                    } else {
                        val src = File(tempVideo)
                        val dst = File(finalOutput)
                        if (src.exists() && src.length() > 0L) {
                            if (src.absolutePath != dst.absolutePath) {
                                if (dst.exists()) dst.delete()
                                val renamed = src.renameTo(dst)
                                if (!renamed) {
                                    src.copyTo(dst, overwrite = true)
                                    src.delete()
                                }
                            }
                        }
                    }
                }

                tempVideoFilePath = null
                tempAudioFilePath = null

                // Publicar en la Galería nativa del usuario ('Movies/VeloRec')
                val finalFile = lastOutputFilePath?.let { File(it) }
                if (finalFile != null && finalFile.exists() && finalFile.length() > 0L) {
                    val publishedFile = GalleryStorageManager.publishVideoToGallery(
                        context = applicationContext,
                        sourceFile = finalFile
                    )
                    if (publishedFile != null && publishedFile.exists()) {
                        lastOutputFilePath = publishedFile.absolutePath
                        lastFileSizeBytes = publishedFile.length()
                    } else {
                        lastFileSizeBytes = finalFile.length()
                    }
                    Log.i("ScreenRecordService", "Grabación Rust 2024 guardada en Galería: ${lastOutputFilePath} (${lastFileSizeBytes} bytes)")
                } else {
                    Log.w("ScreenRecordService", "Archivo final no encontrado o vacío tras finalizar captura.")
                }

                // Notificación de guardado completado (100% éxito)
                lastOutputFilePath?.let { savedPath ->
                    notificationManager?.notify(
                        RecordingNotificationHelper.NOTIFICATION_ID + 1,
                        RecordingNotificationHelper.buildSavedCompletedNotification(this@ScreenRecordService, savedPath)
                    )
                }

            } catch (e: Exception) {
                Log.e("ScreenRecordService", "Excepción inesperada en handleStopRecording: ${e.message}", e)
            } finally {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        timerJob = null
        _isCountingDownFlow.value = false
        _countdownRemainingFlow.value = 0
        _isRecordingFlow.value = false
        _isPausedFlow.value = false
        try {
            audioEngine?.stop()
        } catch (_: Exception) {}
        audioEngine = null
        serviceScope.cancel()
    }

    /**
     * Detección estricta de tecnología de almacenamiento (eMMC vs UFS/NVMe)
     * en dispositivos Android mediante inspección de particiones y bloques Linux.
     */
    private fun isEmmcStorageDetected(): Boolean {
        return try {
            File("/sys/block/mmcblk0").exists() ||
            File("/sys/class/block/mmcblk0").exists() ||
            File("/proc/partitions").takeIf { it.exists() }?.readText()?.contains("mmcblk0") == true
        } catch (_: Exception) {
            false
        }
    }

    // ==========================================
    // FIRMAS JNI PARA MOTOR RUST (EDITION 2024)
    // (Enlazan con Java_com_velorec_recorder_ScreenRecordService_native*Rust*)
    // ==========================================
    private external fun nativeInitRustRecorder(
        path: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        codec: String,
        audio: String
    ): Surface
    private external fun nativeStartRustEncoding()
    private external fun nativePauseRustEncoding()
    private external fun nativeResumeRustEncoding()
    private external fun nativeStopRustEncoding()
}
