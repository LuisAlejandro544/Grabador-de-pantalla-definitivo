package com.velorec.recorder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat

/**
 * MainActivity.kt
 * 
 * Punto de entrada principal de VeloRec Studio en Android.
 * Integra la interfaz táctil en Jetpack Compose (Material 3) y la orquestación
 * del servicio en primer plano para MediaProjection (Android 14+ API 34/35).
 * 
 * CERO COPIA EN KOTLIN:
 * Kotlin solo solicita los permisos del sistema (MediaProjection, Audio, Notificaciones)
 * y pasa la Surface obtenida directamente del codificador por hardware en C++23 o Rust 2024.
 * La grabación pesada, codificación H.264/HEVC/AV1 y multiplexación MP4 es 100% nativa.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6366F1),
                    background = Color(0xFF090D16),
                    surface = Color(0xFF131A29),
                    onPrimary = Color.White,
                    onBackground = Color(0xFFF1F5F9),
                    onSurface = Color(0xFFF1F5F9)
                )
            ) {
                var isRecording by remember { mutableStateOf(false) }
                var isPaused by remember { mutableStateOf(false) }
                var durationSec by remember { mutableIntStateOf(0) }

                // Timer en tiempo real para el contador REC mm:ss
                LaunchedEffect(isRecording, isPaused) {
                    if (isRecording && !isPaused) {
                        while (true) {
                            kotlinx.coroutines.delay(1000L)
                            durationSec++
                        }
                    }
                }

                // Parámetros pendientes de grabación mientras el usuario acepta el diálogo del sistema
                var pendingConfig by remember {
                    mutableStateOf<NativeRecordingConfig?>(null)
                }

                // Launcher para MediaProjection (Captura de Pantalla del Sistema)
                val mediaProjectionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        val config = pendingConfig ?: NativeRecordingConfig()
                        val serviceIntent = Intent(this, ScreenRecordService::class.java).apply {
                            action = ScreenRecordService.ACTION_START
                            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, result.resultCode)
                            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, result.data)
                            putExtra(ScreenRecordService.EXTRA_ENGINE, config.engine)
                            putExtra(ScreenRecordService.EXTRA_WIDTH, config.width)
                            putExtra(ScreenRecordService.EXTRA_HEIGHT, config.height)
                            putExtra(ScreenRecordService.EXTRA_FPS, config.fps)
                            putExtra(ScreenRecordService.EXTRA_BITRATE, config.bitrateBps)
                            putExtra(ScreenRecordService.EXTRA_CODEC, config.codec)
                            putExtra(ScreenRecordService.EXTRA_AUDIO, config.audioSource)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(this, serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }

                        isRecording = true
                        isPaused = false
                        durationSec = 0
                        Toast.makeText(
                            this,
                            "Grabando pantalla (${config.width}x${config.height} @ ${config.fps}fps - ${config.bitrateBps / 1_000_000}Mbps)",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this, "Permiso de captura de pantalla cancelado", Toast.LENGTH_SHORT).show()
                        isRecording = false
                    }
                }

                // Launcher para permisos de micrófono y notificaciones
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: true
                    if (audioGranted) {
                        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                    } else {
                        Toast.makeText(this, "Se requiere permiso de audio para capturar", Toast.LENGTH_SHORT).show()
                    }
                }

                VeloRecMainScreen(
                    isRecording = isRecording,
                    isPaused = isPaused,
                    durationSec = durationSec,
                    onStartRecording = { config ->
                        pendingConfig = config
                        val permissionsToRequest = mutableListOf<String>()

                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }

                        if (permissionsToRequest.isNotEmpty()) {
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        } else {
                            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                        }
                    },
                    onTogglePause = {
                        val newPausedState = !isPaused
                        isPaused = newPausedState
                        val pauseIntent = Intent(this, ScreenRecordService::class.java).apply {
                            action = if (newPausedState) ScreenRecordService.ACTION_PAUSE else ScreenRecordService.ACTION_RESUME
                        }
                        startService(pauseIntent)
                    },
                    onStopRecording = {
                        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
                            action = ScreenRecordService.ACTION_STOP
                        }
                        startService(stopIntent)
                        isRecording = false
                        isPaused = false
                        durationSec = 0
                        Toast.makeText(this, "Grabación finalizada y guardada en MP4", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}
