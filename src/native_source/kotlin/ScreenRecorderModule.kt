package com.velorec.recorder

/**
 * ScreenRecorderModule.kt
 * Módulo puente entre React Native y el subsistema nativo de Android.
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * 1. Expone los métodos de grabación a JavaScript mediante anotaciones @ReactMethod.
 * 2. Gestiona el ciclo de vida de la Activity para solicitar permiso de captura de pantalla (MediaProjection).
 * 3. Utiliza Kotlin Coroutines (Dispatchers.IO) para realizar tareas de entrada/salida y llamadas JNI
 *    sin congelar el hilo principal de la interfaz de usuario (Main Thread / UI Thread).
 * 4. Comunica con ScreenRecordService, el Foreground Service que mantiene viva la grabación en segundo plano.
 */

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.*
import java.io.File

class ScreenRecorderModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), ActivityEventListener {

    // Ámbito de corrutinas para operaciones asíncronas seguras
    private val moduleScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var currentConfig: ReadableMap? = null
    private var pendingPromise: Promise? = null
    private var isRecording = false
    private var isPaused = false

    companion object {
        const val MODULE_NAME = "ScreenRecorderModule"
        const val SCREEN_CAPTURE_REQUEST_CODE = 4210
    }

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String = MODULE_NAME

    /**
     * Prepara el grabador solicitando el Intent de MediaProjection al sistema.
     * En Android 10+ (API 29) y Android 14+ (API 34) el sistema operativo exige
     * un consentimiento explícito del usuario a través de un diálogo del sistema.
     */
    @ReactMethod
    fun prepareRecorder(config: ReadableMap, promise: Promise) {
        val currentActivity = reactApplicationContext.currentActivity
        if (currentActivity == null) {
            promise.reject("E_ACTIVITY_NULL", "No se encontró la Activity principal de Android.")
            return
        }

        this.currentConfig = config
        this.pendingPromise = promise

        // Obtenemos el MediaProjectionManager del sistema
        val projectionManager = currentActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = projectionManager.createScreenCaptureIntent()

        // Lanzamos el intent del sistema para obtener resultCode y data
        currentActivity.startActivityForResult(captureIntent, SCREEN_CAPTURE_REQUEST_CODE)
    }

    /**
     * Captura el resultado del diálogo de permisos del sistema operativo.
     */
    override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            val promise = pendingPromise ?: return
            pendingPromise = null

            if (resultCode == Activity.RESULT_OK && data != null) {
                // El usuario aprobó la captura de pantalla: lanzamos el Foreground Service
                val config = currentConfig
                val engineType = config?.getString("engine") ?: "cpp23"
                val width = config?.getInt("width") ?: 1080
                val height = config?.getInt("height") ?: 1920
                val fps = config?.getInt("fps") ?: 60
                val bitrate = config?.getInt("bitrateBps") ?: 12_000_000
                val codec = config?.getString("codec") ?: "H264"
                val audioSource = config?.getString("audioSource") ?: "both"

                moduleScope.launch(Dispatchers.IO) {
                    try {
                        val serviceIntent = Intent(reactContext, ScreenRecordService::class.java).apply {
                            action = ScreenRecordService.ACTION_START
                            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
                            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, data)
                            putExtra(ScreenRecordService.EXTRA_ENGINE, engineType)
                            putExtra(ScreenRecordService.EXTRA_WIDTH, width)
                            putExtra(ScreenRecordService.EXTRA_HEIGHT, height)
                            putExtra(ScreenRecordService.EXTRA_FPS, fps)
                            putExtra(ScreenRecordService.EXTRA_BITRATE, bitrate)
                            putExtra(ScreenRecordService.EXTRA_CODEC, codec)
                            putExtra(ScreenRecordService.EXTRA_AUDIO, audioSource)
                        }

                        // Android 8.0+ (API 26+) exige startForegroundService
                        reactContext.startForegroundService(serviceIntent)
                        isRecording = true
                        isPaused = false

                        withContext(Dispatchers.Main) {
                            promise.resolve(true)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            promise.reject("E_SERVICE_START_FAILED", "Error al iniciar el servicio: ${e.message}", e)
                        }
                    }
                }
            } else {
                promise.reject("E_PERMISSION_DENIED", "El usuario denegó el permiso de captura de pantalla.")
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {}

    @ReactMethod
    fun pauseRecording(promise: Promise) {
        if (!isRecording) {
            promise.reject("E_NOT_RECORDING", "No hay una grabación activa.")
            return
        }
        val intent = Intent(reactContext, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_PAUSE
        }
        reactContext.startService(intent)
        isPaused = true
        promise.resolve(true)
    }

    @ReactMethod
    fun resumeRecording(promise: Promise) {
        if (!isRecording || !isPaused) {
            promise.reject("E_INVALID_STATE", "La grabación no está pausada.")
            return
        }
        val intent = Intent(reactContext, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_RESUME
        }
        reactContext.startService(intent)
        isPaused = false
        promise.resolve(true)
    }

    @ReactMethod
    fun stopRecording(promise: Promise) {
        if (!isRecording) {
            promise.reject("E_NOT_RECORDING", "No hay una grabación activa.")
            return
        }

        moduleScope.launch(Dispatchers.IO) {
            try {
                val intent = Intent(reactContext, ScreenRecordService::class.java).apply {
                    action = ScreenRecordService.ACTION_STOP
                }
                reactContext.startService(intent)
                isRecording = false
                isPaused = false

                val responseMap = Arguments.createMap().apply {
                    putString("filePath", ScreenRecordService.lastOutputFilePath ?: "")
                    putDouble("durationMs", ScreenRecordService.lastDurationMs.toDouble())
                    putDouble("fileSizeBytes", ScreenRecordService.lastFileSizeBytes.toDouble())
                }

                withContext(Dispatchers.Main) {
                    promise.resolve(responseMap)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    promise.reject("E_STOP_FAILED", "Fallo al detener la grabación: ${e.message}", e)
                }
            }
        }
    }

    @ReactMethod
    fun getEngineStatus(promise: Promise) {
        val map = Arguments.createMap().apply {
            putString("engine", ScreenRecordService.activeEngine)
            putBoolean("isRecording", isRecording)
            putBoolean("isPaused", isPaused)
        }
        promise.resolve(map)
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        moduleScope.cancel()
    }
}
