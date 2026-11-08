package com.velorec.recorder.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.velorec.recorder.data.model.NativeRecordingConfig
import com.velorec.recorder.data.model.NativeTab
import com.velorec.recorder.data.model.RESOLUTION_PRESETS
import com.velorec.recorder.data.model.getAdaptivePresetsForDevice
import com.velorec.recorder.data.model.RemoteUiConfig
import com.velorec.recorder.data.repository.RemoteConfigRepository
import com.velorec.recorder.data.repository.UserPreferencesRepository
import com.velorec.recorder.ui.components.RemoteAnnouncementDialog
import com.velorec.recorder.ui.screens.NativeBenchmarkSection
import com.velorec.recorder.ui.screens.NativeGallerySection
import com.velorec.recorder.ui.screens.NativeRecorderViewport
import com.velorec.recorder.ui.screens.NativeSettingsSection
import com.velorec.recorder.ui.theme.DarkBackground
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.DefaultAccentCyan
import com.velorec.recorder.ui.theme.PrimaryIndigo
import com.velorec.recorder.ui.theme.WarningAmber
import com.velorec.recorder.ui.theme.parseColorSafe

/**
 * VeloRecMainScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Contenedor visual principal y orquestador de navegación de VeloRec Studio en Jetpack Compose.
 * 
 * LÓGICA Y RESPONSABILIDADES:
 * 1. Mantiene el Scaffold con TopAppBar fija y NavigationBar inferior táctil.
 * 2. Carga asíncronamente en 'LaunchedEffect' la configuración remota Server-Driven UI
 *    mediante 'RemoteConfigRepository.fetchRemoteConfigSafe()'.
 * 3. Permite alternar dinámicamente entre las 4 pantallas desacopladas:
 *    - RECORDER (Captura y telemetría)
 *    - SETTINGS (Configuraciones de hardware NDK)
 *    - GALLERY (Archivos grabados locales MP4)
 *    - BENCHMARK (Comparativa de rendimiento C++23 vs Rust 2024)
 * 4. Gestiona el selector rápido de motor (badge superior conmutador C++23 / Rust 2024).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeloRecMainScreen(
    isRecording: Boolean = false,
    isPaused: Boolean = false,
    durationSec: Int = 0,
    isCountingDown: Boolean = false,
    countdownRemaining: Int = 3,
    onCancelCountdown: () -> Unit = {},
    onStartRecording: (config: NativeRecordingConfig) -> Unit = {},
    onTogglePause: () -> Unit = {},
    onStopRecording: () -> Unit = {}
) {
    val context = LocalContext.current
    val adaptivePresets = remember(context) { getAdaptivePresetsForDevice(context) }

    var activeTab by remember { mutableStateOf(NativeTab.RECORDER) }
    var selectedEngine by remember {
        mutableStateOf(UserPreferencesRepository.loadEngine(context))
    }
    var selectedResolution by remember(adaptivePresets) {
        mutableStateOf(UserPreferencesRepository.loadResolution(context, adaptivePresets))
    }
    var selectedBitrateMbps by remember {
        mutableIntStateOf(UserPreferencesRepository.loadBitrate(context))
    }
    var selectedFps by remember {
        mutableIntStateOf(UserPreferencesRepository.loadFps(context))
    }
    var selectedCodec by remember {
        mutableStateOf(UserPreferencesRepository.loadCodec(context))
    }
    var selectedAudio by remember {
        mutableStateOf(UserPreferencesRepository.loadAudioSource(context))
    }
    var selectedCountdownSec by remember {
        mutableIntStateOf(UserPreferencesRepository.loadCountdownSec(context))
    }

    // Estado de configuración remota dinámica (Server-Driven UI)
    var remoteConfig by remember { mutableStateOf(RemoteUiConfig()) }
    var showAnnouncementModal by remember { mutableStateOf(false) }

    // Descarga asíncrona no bloqueante de app_config.json desde GitHub Releases
    LaunchedEffect(Unit) {
        val fetched = RemoteConfigRepository.fetchRemoteConfigSafe()
        remoteConfig = fetched
        if (fetched.announcement.showDialog && fetched.announcement.message.isNotBlank()) {
            showAnnouncementModal = true
        }
    }

    val dynamicAccentColor = parseColorSafe(remoteConfig.theme.accentColorHex, DefaultAccentCyan)
    val dynamicPrimaryColor = parseColorSafe(remoteConfig.theme.primaryColorHex, PrimaryIndigo)

    // Modal de Anuncio Remoto (si se activa en app_config.json)
    if (showAnnouncementModal) {
        RemoteAnnouncementDialog(
            announcement = remoteConfig.announcement,
            accentColor = dynamicAccentColor,
            onDismiss = { showAnnouncementModal = false }
        )
    }

    // Modal de Cuenta Regresiva (se visualiza tras conceder el permiso de Android)
    if (isCountingDown) {
        Dialog(onDismissRequest = onCancelCountdown) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = DarkSurface,
                border = BorderStroke(2.dp, dynamicAccentColor),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(dynamicPrimaryColor.copy(alpha = 0.2f))
                            .border(3.dp, dynamicAccentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (countdownRemaining > 0) "$countdownRemaining" else "¡YA!",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = dynamicAccentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Iniciando Grabación...",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Prepárate o ve a la app que deseas capturar",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    androidx.compose.material3.Button(
                        onClick = onCancelCountdown,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = DarkCard
                        ),
                        border = BorderStroke(1.dp, Color(0xFF64748B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancelar",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(dynamicPrimaryColor, dynamicAccentColor))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "VeloRec Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "VeloRec Studio",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = DarkCard,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "V${remoteConfig.version} PRO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = dynamicAccentColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Captura HD • 60/120 FPS sin lag",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    // Badge nativo del motor con selector rápido interactivo
                    Surface(
                        color = if (selectedEngine == "rust2024") Color(0xFF451A03) else Color(0xFF083344),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable {
                                if (!isRecording) {
                                    selectedEngine = if (selectedEngine == "rust2024") "cpp23" else "rust2024"
                                }
                            }
                            .border(
                                width = 1.dp,
                                color = if (selectedEngine == "rust2024") WarningAmber else dynamicAccentColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (selectedEngine == "rust2024") WarningAmber else dynamicAccentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedEngine == "rust2024") "Rust 2024" else "C++23 NDK",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedEngine == "rust2024") WarningAmber else dynamicAccentColor
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == NativeTab.RECORDER,
                    onClick = { activeTab = NativeTab.RECORDER },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Grabar") },
                    label = { Text("Grabar", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = dynamicPrimaryColor,
                        selectedTextColor = dynamicPrimaryColor,
                        indicatorColor = dynamicPrimaryColor.copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == NativeTab.SETTINGS,
                    onClick = { activeTab = NativeTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = dynamicPrimaryColor,
                        selectedTextColor = dynamicPrimaryColor,
                        indicatorColor = dynamicPrimaryColor.copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == NativeTab.GALLERY,
                    onClick = { activeTab = NativeTab.GALLERY },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Galería") },
                    label = { Text("Galería", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = dynamicPrimaryColor,
                        selectedTextColor = dynamicPrimaryColor,
                        indicatorColor = dynamicPrimaryColor.copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == NativeTab.BENCHMARK,
                    onClick = { activeTab = NativeTab.BENCHMARK },
                    icon = { Icon(Icons.Default.Speed, contentDescription = "Motor") },
                    label = { Text("Motor", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = dynamicPrimaryColor,
                        selectedTextColor = dynamicPrimaryColor,
                        indicatorColor = dynamicPrimaryColor.copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                NativeTab.RECORDER -> {
                    NativeRecorderViewport(
                        selectedEngine = selectedEngine,
                        selectedResolution = selectedResolution,
                        selectedBitrateMbps = selectedBitrateMbps,
                        selectedFps = selectedFps,
                        isRecording = isRecording,
                        isPaused = isPaused,
                        durationSec = durationSec,
                        accentColor = dynamicAccentColor,
                        primaryColor = dynamicPrimaryColor,
                        bannerText = if (remoteConfig.theme.bannerActive) remoteConfig.theme.bannerText else null,
                        availableResolutions = adaptivePresets,
                        selectedCountdownSec = selectedCountdownSec,
                        onResolutionChange = {
                            selectedResolution = it
                            UserPreferencesRepository.saveResolution(context, it)
                        },
                        onBitrateChange = {
                            selectedBitrateMbps = it
                            UserPreferencesRepository.saveBitrate(context, it)
                        },
                        onCountdownChange = {
                            selectedCountdownSec = it
                            UserPreferencesRepository.saveCountdownSec(context, it)
                        },
                        onToggleRecord = {
                            if (!isRecording) {
                                val config = NativeRecordingConfig(
                                    engine = selectedEngine,
                                    width = selectedResolution.width,
                                    height = selectedResolution.height,
                                    fps = selectedFps,
                                    bitrateBps = selectedBitrateMbps * 1_000_000,
                                    codec = selectedCodec,
                                    audioSource = selectedAudio,
                                    countdownSec = selectedCountdownSec
                                )
                                onStartRecording(config)
                            } else {
                                onStopRecording()
                            }
                        },
                        onTogglePause = onTogglePause,
                        onOpenSettings = { activeTab = NativeTab.SETTINGS }
                    )
                }
                NativeTab.SETTINGS -> {
                    NativeSettingsSection(
                        selectedEngine = selectedEngine,
                        selectedResolution = selectedResolution,
                        selectedBitrateMbps = selectedBitrateMbps,
                        selectedFps = selectedFps,
                        selectedCodec = selectedCodec,
                        selectedAudio = selectedAudio,
                        isRecording = isRecording,
                        accentColor = dynamicAccentColor,
                        primaryColor = dynamicPrimaryColor,
                        remoteConfig = remoteConfig,
                        availableResolutions = adaptivePresets,
                        selectedCountdownSec = selectedCountdownSec,
                        onEngineChange = {
                            selectedEngine = it
                            UserPreferencesRepository.saveEngine(context, it)
                        },
                        onResolutionChange = {
                            selectedResolution = it
                            UserPreferencesRepository.saveResolution(context, it)
                        },
                        onBitrateChange = {
                            selectedBitrateMbps = it
                            UserPreferencesRepository.saveBitrate(context, it)
                        },
                        onFpsChange = {
                            selectedFps = it
                            UserPreferencesRepository.saveFps(context, it)
                        },
                        onCodecChange = {
                            selectedCodec = it
                            UserPreferencesRepository.saveCodec(context, it)
                        },
                        onAudioChange = {
                            selectedAudio = it
                            UserPreferencesRepository.saveAudioSource(context, it)
                        },
                        onCountdownChange = {
                            selectedCountdownSec = it
                            UserPreferencesRepository.saveCountdownSec(context, it)
                        }
                    )
                }
                NativeTab.GALLERY -> {
                    NativeGallerySection(accentColor = dynamicAccentColor)
                }
                NativeTab.BENCHMARK -> {
                    NativeBenchmarkSection(
                        selectedEngine = selectedEngine,
                        accentColor = dynamicAccentColor
                    )
                }
            }
        }
    }
}
