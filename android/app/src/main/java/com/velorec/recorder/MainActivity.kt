package com.velorec.recorder

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.velorec.recorder.data.model.NativeRecordingConfig
import com.velorec.recorder.ui.MainViewModel
import com.velorec.recorder.ui.VeloRecMainScreen
import com.velorec.recorder.ui.theme.DarkBackground
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.PrimaryIndigo

/**
 * MainActivity.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Punto de entrada de la aplicación en Android.
 * Integra la interfaz táctil en Jetpack Compose (Material 3) y la orquestación
 * del servicio en primer plano para MediaProjection (Android 14+ API 34/35).
 * 
 * LÓGICA Y ARQUITECTURA MODULAR:
 * 1. Sincronización Directa con 'ScreenRecordService':
 *    - Observa los flujos 'isRecordingFlow', 'isPausedFlow' y 'durationSecFlow' del Foreground Service.
 *    - Si el usuario sale de la aplicación a jugar o ver videos y luego regresa, la interfaz
 *      mantiene exactamente el estado de grabación en curso con el cronómetro sincronizado.
 * 2. Gestión de permisos con ActivityResultContracts:
 *    - Captura de audio (Manifest.permission.RECORD_AUDIO)
 *    - Notificaciones en Android 13+ (Manifest.permission.POST_NOTIFICATIONS)
 *    - Token de MediaProjection para captura de pantalla del sistema.
 * 3. Tubería CERO COPIA: Kotlin solo orquesta permisos y arranca 'ScreenRecordService';
 *    la codificación pesada en H.264/HEVC/AV1 corre 100% nativa en el NDK con C++23 y Rust 2024.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Receptor de broadcasts para sincronizar estado con el proceso :recorder
    private val serviceStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ScreenRecordService.ACTION_STATE_CHANGED) {
                val isRecording = intent.getBooleanExtra(ScreenRecordService.EXTRA_STATE_IS_RECORDING, false)
                val isPaused = intent.getBooleanExtra(ScreenRecordService.EXTRA_STATE_IS_PAUSED, false)
                val durationSec = intent.getIntExtra(ScreenRecordService.EXTRA_STATE_DURATION_SEC, 0)
                val isCountingDown = intent.getBooleanExtra(ScreenRecordService.EXTRA_STATE_IS_COUNTING_DOWN, false)
                val countdownRemaining = intent.getIntExtra(ScreenRecordService.EXTRA_STATE_COUNTDOWN_REMAINING, 0)

                ScreenRecordService.updateStateFromBroadcast(
                    isRecording = isRecording,
                    isPaused = isPaused,
                    durationSec = durationSec,
                    isCountingDown = isCountingDown,
                    countdownRemaining = countdownRemaining
                )
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        val filter = android.content.IntentFilter(ScreenRecordService.ACTION_STATE_CHANGED)
        ContextCompat.registerReceiver(
            this,
            serviceStateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Consultar estado actual del servicio en el proceso aislado :recorder
        try {
            val queryIntent = Intent(this, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_QUERY_STATUS
            }
            startService(queryIntent)
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(serviceStateReceiver)
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = PrimaryIndigo,
                    background = DarkBackground,
                    surface = DarkSurface,
                    onPrimary = Color.White,
                    onBackground = Color(0xFFF1F5F9),
                    onSurface = Color(0xFFF1F5F9)
                )
            ) {
                // Sincronización reactiva con el servicio en primer plano y ViewModel
                val isRecording by ScreenRecordService.isRecordingFlow.collectAsState()
                val isPaused by ScreenRecordService.isPausedFlow.collectAsState()
                val durationSec by ScreenRecordService.durationSecFlow.collectAsState()
                val pendingConfig by viewModel.pendingConfig.collectAsState()
                val isCountingDown by ScreenRecordService.isCountingDownFlow.collectAsState()
                val countdownRemaining by ScreenRecordService.countdownRemainingFlow.collectAsState()

                // Launcher para MediaProjection (Diálogo nativo de captura del sistema Android)
                // Se solicita PRIMERO al tocar "Iniciar Grabación"; tras la confirmación del usuario,
                // inicia el Foreground Service INMEDIATAMENTE mientras la Activity está en primer plano.
                // Esto garantiza cumplimiento con Android 14+ (API 34/35) y evita el bloqueo de micrófono
                // o solicitud de permisos en segundo plano cuando el usuario sale a YouTube o a un juego.
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
                            putExtra(ScreenRecordService.EXTRA_COUNTDOWN, config.countdownSec)
                        }

                        // Iniciar Foreground Service de forma segura (minSdk 26+ garantizado)
                        ContextCompat.startForegroundService(this, serviceIntent)

                        // Si el usuario seleccionó un juego/app para abrir y grabar automáticamente, lanzarlo de inmediato
                        val targetPackage = viewModel.pendingLaunchPackage.value
                        if (!targetPackage.isNullOrBlank()) {
                            try {
                                val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(launchIntent)
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this, "No se pudo abrir $targetPackage: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.clearPendingLaunchPackage()
                        }

                        val toastMsg = if (config.countdownSec > 0) {
                            "Iniciando en ${config.countdownSec}s (${config.width}x${config.height} @ ${config.fps}fps)"
                        } else {
                            "Grabando pantalla (${config.width}x${config.height} @ ${config.fps}fps - ${config.bitrateBps / 1_000_000}Mbps)"
                        }
                        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.clearPendingLaunchPackage()
                        Toast.makeText(this, "Permiso de captura cancelado", Toast.LENGTH_SHORT).show()
                    }
                }

                // Launcher para permisos de micrófono y notificaciones
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: true
                    if (audioGranted) {
                        val projectionManager =
                            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                    } else {
                        viewModel.clearPendingLaunchPackage()
                        Toast.makeText(this, "Se requiere permiso de audio para capturar", Toast.LENGTH_SHORT).show()
                    }
                }

                // Función compartida para solicitar permisos y lanzar captura
                val startRecordingFlow: (NativeRecordingConfig) -> Unit = { config ->
                    viewModel.setPendingConfig(config)
                    val permissionsToRequest = mutableListOf<String>()

                    // Solo solicitar micrófono si la fuente de audio no es 'mute'
                    if (config.audioSource != "mute" && ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    } else {
                        val projectionManager =
                            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                    }
                }

                VeloRecMainScreen(
                    isRecording = isRecording,
                    isPaused = isPaused,
                    durationSec = durationSec,
                    isCountingDown = isCountingDown,
                    countdownRemaining = countdownRemaining,
                    onCancelCountdown = {
                        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
                            action = ScreenRecordService.ACTION_STOP
                        }
                        startService(stopIntent)
                    },
                    onStartRecording = startRecordingFlow,
                    onLaunchAppAndRecord = { packageName, config ->
                        if (isRecording) {
                            try {
                                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(launchIntent)
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this, "No se pudo abrir la app: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.setPendingLaunchPackage(packageName)
                            startRecordingFlow(config)
                        }
                    },
                    onTogglePause = {
                        val pauseIntent = Intent(this, ScreenRecordService::class.java).apply {
                            action = if (isPaused) {
                                ScreenRecordService.ACTION_RESUME
                            } else {
                                ScreenRecordService.ACTION_PAUSE
                            }
                        }
                        startService(pauseIntent)
                    },
                    onStopRecording = {
                        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
                            action = ScreenRecordService.ACTION_STOP
                        }
                        startService(stopIntent)
                        Toast.makeText(this, "Grabación finalizada y guardada en MP4", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}
