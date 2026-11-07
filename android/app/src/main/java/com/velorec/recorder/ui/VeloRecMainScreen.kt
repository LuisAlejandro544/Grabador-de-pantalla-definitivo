package com.velorec.recorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import com.velorec.recorder.data.model.NativeRecordingConfig
import com.velorec.recorder.data.model.NativeTab
import com.velorec.recorder.data.model.RESOLUTION_PRESETS
import com.velorec.recorder.data.model.RemoteUiConfig
import com.velorec.recorder.data.repository.RemoteConfigRepository
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
    onStartRecording: (config: NativeRecordingConfig) -> Unit = {},
    onTogglePause: () -> Unit = {},
    onStopRecording: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(NativeTab.RECORDER) }
    var selectedEngine by remember { mutableStateOf("cpp23") } // "cpp23" o "rust2024"
    var selectedResolution by remember { mutableStateOf(RESOLUTION_PRESETS[2]) } // 1080p por defecto
    var selectedBitrateMbps by remember { mutableIntStateOf(12) }
    var selectedFps by remember { mutableIntStateOf(60) }
    var selectedCodec by remember { mutableStateOf("H264") } // "H264", "HEVC", "AV1"
    var selectedAudio by remember { mutableStateOf("both") } // "both", "mic", "internal", "mute"

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
                                text = "Jetpack Compose + C++23 & Rust NDK",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    // Badge del motor nativo con selector rápido interactivo
                    Surface(
                        color = if (selectedEngine == "rust2024") Color(0xFF451A03) else Color(0xFF083344),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable {
                                if (!isRecording) {
                                    selectedEngine = if (selectedEngine == "rust2024") "cpp23" else "rust2024"
                                }
                            }
                            .border(
                                width = 1.dp,
                                color = if (selectedEngine == "rust2024") WarningAmber else dynamicAccentColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Text(
                            text = if (selectedEngine == "rust2024") "🦀 Rust 2024" else "⚡ C++23",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedEngine == "rust2024") WarningAmber else dynamicAccentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
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
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Grabar") },
                    label = { Text("Grabar", fontSize = 11.sp) },
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
                    label = { Text("Ajustes", fontSize = 11.sp) },
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
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Galería") },
                    label = { Text("Galería", fontSize = 11.sp) },
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
                    icon = { Icon(Icons.Default.Info, contentDescription = "Motor") },
                    label = { Text("Motor", fontSize = 11.sp) },
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
                        onResolutionChange = { selectedResolution = it },
                        onBitrateChange = { selectedBitrateMbps = it },
                        onToggleRecord = {
                            if (!isRecording) {
                                val config = NativeRecordingConfig(
                                    engine = selectedEngine,
                                    width = selectedResolution.width,
                                    height = selectedResolution.height,
                                    fps = selectedFps,
                                    bitrateBps = selectedBitrateMbps * 1_000_000,
                                    codec = selectedCodec,
                                    audioSource = selectedAudio
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
                        onEngineChange = { selectedEngine = it },
                        onResolutionChange = { selectedResolution = it },
                        onBitrateChange = { selectedBitrateMbps = it },
                        onFpsChange = { selectedFps = it },
                        onCodecChange = { selectedCodec = it },
                        onAudioChange = { selectedAudio = it }
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
