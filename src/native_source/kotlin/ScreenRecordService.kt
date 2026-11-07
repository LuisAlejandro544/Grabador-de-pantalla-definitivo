package com.velorec.recorder

/**
 * ScreenRecordService.kt
 * Servicio en primer plano (Foreground Service) de Android para captura de pantalla.
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * 1. Mantiene el proceso activo en segundo plano mientras el usuario usa otras aplicaciones o juegos.
 * 2. Cumple estrictamente con las políticas de Android 14 (API 34) declarando
 *    FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION en el AndroidManifest.
 * 3. Delega la codificación pesada a C++23 o Rust 2024:
 *    - Obtiene una 'Surface' de hardware directamente del motor nativo (AMediaCodec / EGL / Rust).
 *    - Pasa esa Surface al VirtualDisplay de MediaProjection.
 *    - ¡CERO COPIA DE MEMORIA EN KOTLIN! No se instancian Bitmaps ni buffers en la máquina virtual JVM/ART.
 * 4. Gestiona las llamadas JNI cargando 'velorec_cpp23' o 'velorec_rust2024'.
 */

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.Surface
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File

class ScreenRecordService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var nativeEnginePointer: Long = 0L

    companion object {
        const val CHANNEL_ID = "velorec_recording_channel"
        const val NOTIFICATION_ID = 8801

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

        // Carga dinámica de las bibliotecas nativas compiladas
        init {
            try {
                System.loadLibrary("velorec_cpp23")
                System.loadLibrary("velorec_rust2024")
            } catch (e: UnsatisfiedLinkError) {
                // Las librerías se cargarán bajo demanda en tiempo de ejecución
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStartRecording(intent)
            ACTION_PAUSE -> handlePauseRecording()
            ACTION_RESUME -> handleResumeRecording()
            ACTION_STOP -> handleStopRecording()
        }
        return START_NOT_STICKY
    }

    private fun handleStartRecording(intent: Intent) {
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA) ?: return
        val engine = intent.getStringExtra(EXTRA_ENGINE) ?: "cpp23"
        val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
        val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
        val fps = intent.getIntExtra(EXTRA_FPS, 60)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, 12_000_000)
        val codec = intent.getStringExtra(EXTRA_CODEC) ?: "H264"
        val audio = intent.getStringExtra(EXTRA_AUDIO) ?: "both"

        activeEngine = engine

        // Configuración del Foreground Service según versión de Android
        val notification = buildRecordingNotification(engine, width, height, fps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ (API 34) exige FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Ejecutamos la inicialización en hilo de fondo usando Corrutinas
        serviceScope.launch(Dispatchers.IO) {
            try {
                val outputDir = getExternalFilesDir(null) ?: filesDir
                val timestamp = System.currentTimeMillis()
                val outputFile = File(outputDir, "VeloRec_${timestamp}_${engine}.mp4")
                lastOutputFilePath = outputFile.absolutePath

                // Inicializar motor nativo elegido (C++23 o Rust 2024) y obtener la Surface de entrada de hardware
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
                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

                // Crear VirtualDisplay conectado directamente a la Surface nativa
                val densityDpi = resources.displayMetrics.densityDpi
                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "VeloRec-Capture",
                    width,
                    height,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    nativeSurface,
                    null,
                    null
                )

                // Señalizar al motor nativo para comenzar a recibir frames
                if (engine == "rust2024") {
                    nativeStartRustEncoding()
                } else {
                    nativeStartCppEncoding()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    private fun handlePauseRecording() {
        serviceScope.launch(Dispatchers.IO) {
            if (activeEngine == "rust2024") {
                nativePauseRustEncoding()
            } else {
                nativePauseCppEncoding()
            }
        }
    }

    private fun handleResumeRecording() {
        serviceScope.launch(Dispatchers.IO) {
            if (activeEngine == "rust2024") {
                nativeResumeRustEncoding()
            } else {
                nativeResumeCppEncoding()
            }
        }
    }

    private fun handleStopRecording() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Detener codificación en el motor nativo
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Grabación de Pantalla VeloRec",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Muestra el estado activo de grabación con C++23 / Rust"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildRecordingNotification(engine: String, width: Int, height: Int, fps: Int): Notification {
        val engineLabel = if (engine == "rust2024") "Rust 2024" else "C++23"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Grabando pantalla")
            .setContentText("Motor: $engineLabel | ${width}x${height} @ ${fps}fps")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // ==========================================
    // FIRMAS JNI PARA MOTOR C++23
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
    // ==========================================
    private external fun nativeInitRustRecorder(
        path: String, width: Int, height: Int, fps: Int, bitrate: Int, codec: String, audio: String
    ): Surface
    private external fun nativeStartRustEncoding()
    private external fun nativePauseRustEncoding()
    private external fun nativeResumeRustEncoding()
    private external fun nativeStopRustEncoding()
}
