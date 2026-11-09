package com.velorec.recorder.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.velorec.recorder.data.model.BITRATE_PRESETS
import com.velorec.recorder.data.model.COUNTDOWN_PRESETS
import com.velorec.recorder.data.model.FPS_PRESETS
import com.velorec.recorder.data.model.MIN_BITRATE_MBPS
import com.velorec.recorder.data.model.MAX_SAFE_BITRATE_MBPS
import com.velorec.recorder.data.model.getBitrateThermalAdvice
import com.velorec.recorder.data.model.RemoteUiConfig
import com.velorec.recorder.data.model.ResolutionPreset
import com.velorec.recorder.data.model.STANDARD_POPULAR_PRESETS
import com.velorec.recorder.data.model.createCustomResolutionPreset
import com.velorec.recorder.data.model.getAdaptivePresetsForDevice
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.SuccessGreen
import com.velorec.recorder.ui.theme.WarningAmber

/**
 * SettingsScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Implementa la pantalla de configuración técnica profesional y modular de VeloRec Studio.
 * 
 * ARQUITECTURA MODULAR DE NAVEGACIÓN PROFESIONAL:
 * En lugar de amontonar todas las listas de opciones en una sola columna vertical interminable,
 * este módulo organiza los ajustes en un menú maestro con botones de navegación hacia pantallas
 * dedicadas e independientes para cada parámetro técnico:
 * 1. Menú Principal: Botones de acceso rápido a cada sección con vista previa del valor activo.
 * 2. Pantalla de Resolución: Lista completa de resoluciones proporcionales nativas con paridad YUV420p.
 * 3. Pantalla de Bitrate: Presets de velocidad con calculadora de almacenamiento en tiempo real.
 * 4. Pantalla de Tasa de Cuadros (FPS): 24, 30, 60, 90 y 120 FPS con guía de uso.
 * 5. Pantalla de Códec de Hardware: H.264, HEVC y AV1 con detalles de compresión.
 * 6. Pantalla de Fuente de Audio: Mezcla estéreo, Micrófono, Audio Interno y Silencio.
 * 7. Pantalla de Cuenta Regresiva: Temporizador de inicio en segundo plano (0s, 3s, 5s, 10s).
 * 8. Pantalla de Motor Nativo: C++23 NDK vs Rust 2024 Zero-Copy.
 * 
 * Integra 'BackHandler' de Jetpack Compose para que el botón atrás físico o por gestos de Android
 * regrese suavemente al menú principal de ajustes sin cerrar la aplicación.
 */

/**
 * Subpantallas dedicadas dentro de la sección de Ajustes.
 */
enum class SettingsSubScreen {
    MAIN,           // Menú principal con tarjetas y botones hacia cada apartado
    RESOLUTION,     // Pantalla dedicada: Resolución de Grabación
    BITRATE,        // Pantalla dedicada: Tasa de Bits
    FPS,            // Pantalla dedicada: Tasa de Cuadros (FPS)
    CODEC,          // Pantalla dedicada: Códec de Hardware
    AUDIO,          // Pantalla dedicada: Fuente de Captura de Audio
    COUNTDOWN,      // Pantalla dedicada: Cuenta Regresiva de Inicio
    ENGINE          // Pantalla dedicada: Motor Nativo (C++23 vs Rust 2024)
}

@Composable
fun NativeSettingsSection(
    selectedEngine: String,
    selectedResolution: ResolutionPreset,
    selectedBitrateMbps: Int,
    selectedFps: Int,
    selectedCodec: String,
    selectedAudio: String,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    remoteConfig: RemoteUiConfig,
    availableResolutions: List<ResolutionPreset>? = null,
    selectedCountdownSec: Int = 3,
    onEngineChange: (String) -> Unit,
    onResolutionChange: (ResolutionPreset) -> Unit,
    onBitrateChange: (Int) -> Unit,
    onFpsChange: (Int) -> Unit,
    onCodecChange: (String) -> Unit,
    onAudioChange: (String) -> Unit,
    onCountdownChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val resolutions = availableResolutions ?: remember(context) { getAdaptivePresetsForDevice(context) }
    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // Soporte para botón atrás del sistema Android (físico o por gestos)
    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

    when (currentSubScreen) {
        SettingsSubScreen.MAIN -> {
            SettingsMainMenu(
                selectedEngine = selectedEngine,
                selectedResolution = selectedResolution,
                selectedBitrateMbps = selectedBitrateMbps,
                selectedFps = selectedFps,
                selectedCodec = selectedCodec,
                selectedAudio = selectedAudio,
                selectedCountdownSec = selectedCountdownSec,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                remoteConfig = remoteConfig,
                onNavigateTo = { currentSubScreen = it }
            )
        }
        SettingsSubScreen.RESOLUTION -> {
            ResolutionSettingsSubScreen(
                resolutions = resolutions,
                selectedResolution = selectedResolution,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                onResolutionChange = onResolutionChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.BITRATE -> {
            BitrateSettingsSubScreen(
                selectedBitrateMbps = selectedBitrateMbps,
                isRecording = isRecording,
                accentColor = accentColor,
                onBitrateChange = onBitrateChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.FPS -> {
            FpsSettingsSubScreen(
                selectedFps = selectedFps,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                onFpsChange = onFpsChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.CODEC -> {
            CodecSettingsSubScreen(
                selectedCodec = selectedCodec,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                onCodecChange = onCodecChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.AUDIO -> {
            AudioSettingsSubScreen(
                selectedAudio = selectedAudio,
                isRecording = isRecording,
                accentColor = accentColor,
                onAudioChange = onAudioChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.COUNTDOWN -> {
            CountdownSettingsSubScreen(
                selectedCountdownSec = selectedCountdownSec,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                onCountdownChange = onCountdownChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.ENGINE -> {
            EngineSettingsSubScreen(
                selectedEngine = selectedEngine,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                onEngineChange = onEngineChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
    }
}

/**
 * Menú Principal de Ajustes: Presenta botones elegantes hacia cada pantalla de configuración.
 */
@Composable
private fun SettingsMainMenu(
    selectedEngine: String,
    selectedResolution: ResolutionPreset,
    selectedBitrateMbps: Int,
    selectedFps: Int,
    selectedCodec: String,
    selectedAudio: String,
    selectedCountdownSec: Int,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    remoteConfig: RemoteUiConfig,
    onNavigateTo: (SettingsSubScreen) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Encabezado de la Sección de Ajustes
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ajustes de Grabación y Hardware",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 17.sp
            )
        }

        // Tarjeta de Estado de Configuración Remota (GitHub Releases)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Configuración Remota (GitHub)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    Surface(
                        color = if (remoteConfig.isFetchedFromNetwork) SuccessGreen.copy(alpha = 0.2f) else DarkCard,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (remoteConfig.isFetchedFromNetwork) "ONLINE" else "LOCAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remoteConfig.isFetchedFromNetwork) SuccessGreen else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "La app lee automáticamente 'app_config.json' desde GitHub Releases. Si no hay internet, opera con sus valores locales seguros.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(remoteConfig.apkDownloadUrl))
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver Releases en GitHub", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Título del Grupo 1: Parámetros de Video
        Text(
            text = "VIDEO Y CALIDAD VISUAL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )

        // Botón a Pantalla de Resolución
        val resolutionBadge = when {
            selectedResolution.isCustom -> "Custom"
            selectedResolution.isNative -> "Nativa"
            selectedResolution.aspectRatioLabel.isNotBlank() -> selectedResolution.aspectRatioLabel
            else -> "Estándar"
        }
        SettingsNavigationRow(
            icon = Icons.Default.AspectRatio,
            title = "Resolución de Grabación",
            subtitle = "${selectedResolution.name} (${selectedResolution.width}x${selectedResolution.height} px)",
            badgeText = resolutionBadge,
            accentColor = accentColor,
            onClick = { onNavigateTo(SettingsSubScreen.RESOLUTION) }
        )

        // Botón a Pantalla de Bitrate
        SettingsNavigationRow(
            icon = Icons.Default.Speed,
            title = "Tasa de Bits (Bitrate)",
            subtitle = "$selectedBitrateMbps Mbps • ~${(selectedBitrateMbps * 60) / 8} MB por min",
            badgeText = "${selectedBitrateMbps}M",
            accentColor = accentColor,
            onClick = { onNavigateTo(SettingsSubScreen.BITRATE) }
        )

        // Botón a Pantalla de FPS
        SettingsNavigationRow(
            icon = Icons.Default.SlowMotionVideo,
            title = "Tasa de Cuadros (FPS)",
            subtitle = "$selectedFps FPS" + if (selectedFps >= 60) " • Máxima fluidez" else " • Balanceada",
            badgeText = "${selectedFps} FPS",
            accentColor = accentColor,
            onClick = { onNavigateTo(SettingsSubScreen.FPS) }
        )

        // Botón a Pantalla de Códec
        val codecLabel = when (selectedCodec) {
            "H264" -> "H.264 (AVC) • Universal"
            "HEVC" -> "H.265 (HEVC) • Alta compresión"
            "AV1" -> "AV1 • Nueva generación"
            else -> selectedCodec
        }
        SettingsNavigationRow(
            icon = Icons.Default.Movie,
            title = "Códec de Hardware",
            subtitle = codecLabel,
            badgeText = selectedCodec,
            accentColor = accentColor,
            onClick = { onNavigateTo(SettingsSubScreen.CODEC) }
        )

        // Título del Grupo 2: Audio y Preparación
        Text(
            text = "AUDIO Y TIEMPO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
        )

        // Botón a Pantalla de Audio
        val audioLabel = when (selectedAudio) {
            "both" -> "Mezcla (Micrófono + Audio Interno)"
            "mic" -> "Solo Micrófono"
            "internal" -> "Solo Audio Interno (Juegos/Apps)"
            "mute" -> "Silencio (Sin audio)"
            else -> selectedAudio
        }
        val audioIcon = when (selectedAudio) {
            "mic" -> Icons.Default.Mic
            "mute" -> Icons.Default.VolumeOff
            "both" -> Icons.Default.Headphones
            else -> Icons.Default.VolumeUp
        }
        SettingsNavigationRow(
            icon = audioIcon,
            title = "Fuente de Captura de Audio",
            subtitle = audioLabel,
            badgeText = when (selectedAudio) { "both" -> "Mezcla"; "mic" -> "Mic"; "internal" -> "Interno"; else -> "Mute" },
            accentColor = accentColor,
            onClick = { onNavigateTo(SettingsSubScreen.AUDIO) }
        )

        // Botón a Pantalla de Cuenta Regresiva
        val countdownLabel = when (selectedCountdownSec) {
            0 -> "0s (Inmediata / Desactivada)"
            3 -> "3 segundos (Recomendada ★)"
            5 -> "5 segundos (Estándar)"
            10 -> "10 segundos (Larga)"
            else -> "${selectedCountdownSec}s"
        }
        SettingsNavigationRow(
            icon = Icons.Default.Timer,
            title = "Cuenta Regresiva de Inicio",
            subtitle = countdownLabel,
            badgeText = if (selectedCountdownSec == 0) "0s" else "${selectedCountdownSec}s",
            accentColor = accentColor,
            onClick = { onNavigateTo(SettingsSubScreen.COUNTDOWN) }
        )

        // Título del Grupo 3: Arquitectura NDK
        Text(
            text = "MOTOR NATIVO NDK",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
        )

        // Botón a Pantalla de Motor
        val engineLabel = if (selectedEngine == "rust2024") "Rust 2024 (Zero-Copy & GC-Free)" else "C++23 NDK (Hardware Directo AMediaCodec)"
        SettingsNavigationRow(
            icon = Icons.Default.Bolt,
            title = "Motor Nativo por Hardware",
            subtitle = engineLabel,
            badgeText = if (selectedEngine == "rust2024") "🦀 Rust" else "⚡ C++23",
            accentColor = if (selectedEngine == "rust2024") WarningAmber else accentColor,
            onClick = { onNavigateTo(SettingsSubScreen.ENGINE) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Fila / Botón de Navegación elegante hacia cada pantalla de ajuste técnico.
 */
@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFF475569).copy(alpha = 0.4f))
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Encabezado común para todas las subpantallas con botón "← Volver a Ajustes".
 */
@Composable
private fun SubScreenHeader(
    title: String,
    description: String,
    accentColor: Color,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBack() },
            color = DarkCard,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF475569).copy(alpha = 0.4f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Volver a Ajustes",
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = description,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

// =========================================================================
// PANTALLAS DEDICADAS INDEPENDIENTES PARA CADA APARTADO TÉCNICO
// =========================================================================

/**
 * 1. Pantalla Dedicada: Resolución de Grabación
 *
 * Sistema de navegación superior en 3 pestañas:
 * - Nativas (Tu Móvil): Calculadas para el display real de este smartphone (100% pantalla completa).
 * - Estándar (Populares): Formatos 20:9, 19.5:9, 19:9, 21:9 y 9:16 con filtros táctiles.
 * - Personalizada: Editor asistido con validación de paridad YUV420p y cálculo en tiempo real.
 */
enum class ResolutionTab {
    NATIVE, STANDARD, CUSTOM
}

@Composable
private fun ResolutionSettingsSubScreen(
    resolutions: List<ResolutionPreset>,
    selectedResolution: ResolutionPreset,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onResolutionChange: (ResolutionPreset) -> Unit,
    onBack: () -> Unit
) {
    val nativePresets = remember(resolutions) { resolutions.filter { it.isNative } }
    val initialTab = remember(selectedResolution) {
        when {
            selectedResolution.isCustom -> ResolutionTab.CUSTOM
            selectedResolution.isNative -> ResolutionTab.NATIVE
            else -> ResolutionTab.STANDARD
        }
    }
    var currentTab by remember { mutableStateOf(initialTab) }
    var standardFilter by remember { mutableStateOf("TODAS") }

    // Estados para la pestaña de resolución personalizada
    var customWidthText by remember(selectedResolution) {
        mutableStateOf(if (selectedResolution.isCustom) selectedResolution.width.toString() else "1080")
    }
    var customHeightText by remember(selectedResolution) {
        mutableStateOf(if (selectedResolution.isCustom) selectedResolution.height.toString() else "2400")
    }
    var customSavedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader(
            title = "Resolución de Grabación",
            description = "Elige entre resoluciones nativas que llenan toda tu pantalla, formatos estándar universales o crea tu propia resolución personalizada.",
            accentColor = accentColor,
            onBack = onBack
        )

        // BARRA DE NAVEGACIÓN EN PESTAÑAS (SEGMENTED TABS ERGONÓMICO)
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab Nativas
                val isNativeTab = currentTab == ResolutionTab.NATIVE
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { currentTab = ResolutionTab.NATIVE },
                    color = if (isNativeTab) primaryColor.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isNativeTab) BorderStroke(1.dp, primaryColor) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = if (isNativeTab) accentColor else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nativas",
                            fontWeight = if (isNativeTab) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isNativeTab) accentColor else Color(0xFF94A3B8)
                        )
                    }
                }

                // Tab Estándar
                val isStandardTab = currentTab == ResolutionTab.STANDARD
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { currentTab = ResolutionTab.STANDARD },
                    color = if (isStandardTab) primaryColor.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isStandardTab) BorderStroke(1.dp, primaryColor) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = if (isStandardTab) accentColor else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Estándar",
                            fontWeight = if (isStandardTab) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isStandardTab) accentColor else Color(0xFF94A3B8)
                        )
                    }
                }

                // Tab Personalizada
                val isCustomTab = currentTab == ResolutionTab.CUSTOM
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { currentTab = ResolutionTab.CUSTOM },
                    color = if (isCustomTab) primaryColor.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isCustomTab) BorderStroke(1.dp, primaryColor) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isCustomTab) accentColor else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Personalizada",
                            fontWeight = if (isCustomTab) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isCustomTab) accentColor else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        when (currentTab) {
            // =================================================================
            // PESTAÑA 1: RESOLUCIONES NATIVAS DE LA PANTALLA
            // =================================================================
            ResolutionTab.NATIVE -> {
                // Tarjeta explicativa pedagógica sobre Nativas y calidad
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100% de tu pantalla (Borde a Borde)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Estas resoluciones se calculan a la medida exacta de tu celular. Cubren toda la pantalla sin bandas negras ni deformar la imagen.",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.5.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💡 ¿Cómo influye la opción que elijas?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Alta Calidad (100% Nativa o FHD): Máxima nitidez pixel a pixel. Los textos finos y pequeños se leen perfectos.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "• Ahorro y Gaming (HD o 540p): Sigue llenando el 100% de tu pantalla, pero los bordes se verán más suaves o pixelados. A cambio, el celular NO se calienta, la batería dura más y los juegos van fluidos.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "OPCIONES ADAPTADAS A TU DISPOSITIVO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )

                nativePresets.forEach { preset ->
                    val isSelected = selectedResolution.id == preset.id
                    val qualityDescription = when (preset.id) {
                        "native_100" -> "Máxima nitidez absoluta (Pixel-Perfect) • Ideal para tutoriales"
                        "native_fhd" -> "Alta definición balanceada • Recomendada ★"
                        "native_hd" -> "Buen equilibrio • Archivos más livianos y menor consumo"
                        "native_sd" -> "Modo Gaming y Ahorro • Máxima fluidez sin calentar el móvil"
                        "native_2k" -> "Ultra definición / Supermuestreo de alta gama"
                        else -> "100% Pantalla completa sin bordes negros"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRecording) { onResolutionChange(preset) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) primaryColor.copy(alpha = 0.2f) else DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, primaryColor) else BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) accentColor else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = primaryColor.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${preset.scalePercent}%",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "${preset.width} x ${preset.height} px • Bitrate sugerido: ${preset.recommendedBitrateMbps} Mbps",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = qualityDescription,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                    lineHeight = 14.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Seleccionada",
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // PESTAÑA 2: RESOLUCIONES ESTÁNDAR Y NO NATIVAS (POPULARES)
            // =================================================================
            ResolutionTab.STANDARD -> {
                // Tarjeta explicativa de resoluciones universales
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Formatos Estándar de la Industria",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Medidas universales usadas por teléfonos (20:9, 19.5:9, 19:9, 21:9) y plataformas de video vertical (9:16).",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "💡 Si tu pantalla no tiene esta proporción exacta, el video tendrá bordes de encuadre limpios, ideal para publicar en YouTube Shorts, TikTok o Reels sin recortes indeseados.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )
                    }
                }

                // FILTROS RÁPIDOS POR RELACIÓN DE ASPECTO
                Text(
                    text = "FILTRAR POR PROPORCIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )

                val filterOptions = listOf("TODAS", "20:9", "19.5:9", "19:9", "21:9", "9:16")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.forEach { filter ->
                        val isFilterActive = standardFilter == filter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { standardFilter = filter },
                            color = if (isFilterActive) primaryColor.copy(alpha = 0.3f) else DarkCard,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isFilterActive) primaryColor else Color(0xFF334155))
                        ) {
                            Text(
                                text = filter,
                                fontSize = 10.sp,
                                fontWeight = if (isFilterActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isFilterActive) accentColor else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                val filteredPresets = remember(standardFilter) {
                    if (standardFilter == "TODAS") {
                        STANDARD_POPULAR_PRESETS
                    } else {
                        STANDARD_POPULAR_PRESETS.filter { it.category == standardFilter || it.aspectRatioLabel == standardFilter }
                    }
                }

                filteredPresets.forEach { preset ->
                    val isSelected = selectedResolution.id == preset.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRecording) { onResolutionChange(preset) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) primaryColor.copy(alpha = 0.2f) else DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, primaryColor) else BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (isSelected) accentColor else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = DarkCard,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, Color(0xFF475569))
                                    ) {
                                        Text(
                                            text = preset.aspectRatioLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "${preset.width} x ${preset.height} px • Bitrate: ${preset.recommendedBitrateMbps} Mbps • ${preset.label}",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Seleccionada",
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // PESTAÑA 3: RESOLUCIÓN PERSONALIZADA
            // =================================================================
            ResolutionTab.CUSTOM -> {
                // Tarjeta explicativa de paridad y hardware
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Configura tus Medidas a Mano",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Introduce el ancho y alto que desees. VeloRec Studio corregirá automáticamente números impares asegurando la paridad obligatoria de codificación por hardware AMediaCodec.",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.5.sp
                        )
                    }
                }

                // Parseo y cálculo en tiempo real
                val rawW = customWidthText.toIntOrNull() ?: 1080
                val rawH = customHeightText.toIntOrNull() ?: 2400
                val evenW = maxOf(240, if (rawW % 2 == 0) rawW else rawW - 1)
                val evenH = maxOf(240, if (rawH % 2 == 0) rawH else rawH - 1)
                val aspectDecimal = evenH.toDouble() / evenW.toDouble().coerceAtLeast(1.0)
                val calculatedRatio = when {
                    Math.abs(aspectDecimal - (20.0 / 9.0)) < 0.025 -> "20:9"
                    Math.abs(aspectDecimal - (19.5 / 9.0)) < 0.025 -> "19.5:9"
                    Math.abs(aspectDecimal - (19.0 / 9.0)) < 0.025 -> "19:9"
                    Math.abs(aspectDecimal - (21.0 / 9.0)) < 0.025 -> "21:9"
                    Math.abs(aspectDecimal - (16.0 / 9.0)) < 0.025 -> "9:16"
                    else -> String.format(java.util.Locale.US, "%.2f:1", aspectDecimal)
                }

                // Campos de entrada táctil
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Ancho
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ancho (px)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = customWidthText,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 5) {
                                            customWidthText = input
                                            customSavedMessage = null
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = Color(0xFF475569),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Alto
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Alto (px)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = customHeightText,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 5) {
                                            customHeightText = input
                                            customSavedMessage = null
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = Color(0xFF475569),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Botones de ajuste rápido
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val temp = customWidthText
                                        customWidthText = customHeightText
                                        customHeightText = temp
                                        customSavedMessage = null
                                    },
                                color = DarkCard,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Invertir",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1.4f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val native100 = nativePresets.firstOrNull()
                                        if (native100 != null) {
                                            customWidthText = native100.width.toString()
                                            customHeightText = native100.height.toString()
                                            customSavedMessage = null
                                        }
                                    },
                                color = DarkCard,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Copiar Pantalla",
                                        fontSize = 11.sp,
                                        color = accentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Tarjeta de previsualización de parámetros
                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Medida Final Par:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "$evenW x $evenH px",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Proporción Calculada:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = calculatedRatio,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }

                                if (rawW % 2 != 0 || rawH % 2 != 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚠️ Se redondearon dimensiones impares a múltiplos de 2 para compatibilidad con codificadores de hardware.",
                                        fontSize = 10.sp,
                                        color = WarningAmber
                                    )
                                }
                            }
                        }

                        // Botón de aplicación
                        Button(
                            onClick = {
                                val customPreset = createCustomResolutionPreset(evenW, evenH)
                                onResolutionChange(customPreset)
                                customSavedMessage = "¡Resolución de ${evenW}x${evenH} aplicada exitosamente!"
                            },
                            enabled = !isRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guardar y Usar Esta Resolución",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (customSavedMessage != null) {
                            Text(
                                text = customSavedMessage ?: "",
                                color = SuccessGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Si actualmente hay una resolución personalizada activa, mostrar su tarjeta
                if (selectedResolution.isCustom) {
                    Text(
                        text = "RESOLUCIÓN PERSONALIZADA ACTIVA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, primaryColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedResolution.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${selectedResolution.width} x ${selectedResolution.height} px • ${selectedResolution.aspectRatioLabel}",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Activa",
                                tint = primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 2. Pantalla Dedicada: Tasa de Bits (Bitrate)
 */
@Composable
private fun BitrateSettingsSubScreen(
    selectedBitrateMbps: Int,
    isRecording: Boolean,
    accentColor: Color,
    onBitrateChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    val estimatedMbPerMin = (selectedBitrateMbps * 60) / 8
    val estimatedMbPerHour = estimatedMbPerMin * 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader(
            title = "Tasa de Bits (Bitrate)",
            description = "Controla la densidad de información grabada por segundo. Un mayor bitrate ofrece mayor nitidez en escenas rápidas, pero genera archivos de mayor peso.",
            accentColor = accentColor,
            onBack = onBack
        )

        val bitrateAdvice = getBitrateThermalAdvice(selectedBitrateMbps)
        val tierBadgeColor = when {
            selectedBitrateMbps <= 3 -> Color(0xFF10B981)
            selectedBitrateMbps <= 16 -> Color(0xFFF59E0B)
            selectedBitrateMbps <= 30 -> Color(0xFF38BDF8)
            selectedBitrateMbps <= 50 -> Color(0xFFA855F7)
            else -> Color(0xFFEF4444)
        }

        // Tarjeta de Cálculo de Espacio Activo y Nivel Térmico
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Tasa Seleccionada", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tierBadgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, tierBadgeColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(tierBadgeColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = bitrateAdvice.tierName,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = tierBadgeColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$selectedBitrateMbps Mbps",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${bitrateAdvice.safetyLevel}: ${bitrateAdvice.advice}",
                    fontSize = 11.5.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "1 minuto de video", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text(text = "~$estimatedMbPerMin MB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text(text = "10 minutos de video", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text(text = "~${estimatedMbPerMin * 10} MB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text(text = "1 hora de video", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text(text = "~${estimatedMbPerHour / 1024}.${(estimatedMbPerHour % 1024) / 100} GB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Sección de Ajuste Continuo Táctil
        Text(
            text = "AJUSTE PERSONALIZADO CONTINUO (1 A 60 MBPS)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(enabled = !isRecording && selectedBitrateMbps > MIN_BITRATE_MBPS) {
                                onBitrateChange((selectedBitrateMbps - 1).coerceAtLeast(MIN_BITRATE_MBPS))
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Reducir",
                                tint = if (selectedBitrateMbps > MIN_BITRATE_MBPS) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    ) {
                        Slider(
                            value = selectedBitrateMbps.toFloat(),
                            onValueChange = { newValue ->
                                onBitrateChange(newValue.toInt().coerceIn(MIN_BITRATE_MBPS, MAX_SAFE_BITRATE_MBPS))
                            },
                            valueRange = MIN_BITRATE_MBPS.toFloat()..MAX_SAFE_BITRATE_MBPS.toFloat(),
                            steps = (MAX_SAFE_BITRATE_MBPS - MIN_BITRATE_MBPS - 1),
                            enabled = !isRecording,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(enabled = !isRecording && selectedBitrateMbps < MAX_SAFE_BITRATE_MBPS) {
                                onBitrateChange((selectedBitrateMbps + 1).coerceAtMost(MAX_SAFE_BITRATE_MBPS))
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Aumentar",
                                tint = if (selectedBitrateMbps < MAX_SAFE_BITRATE_MBPS) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "1 Mbps (Mínimo)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(text = "12 Mbps (Gamer ★)", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                    Text(text = "60 Mbps (Máx Seguro)", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        // Tarjeta Informativa de Estabilidad Térmica ("Para no ahogar el teléfono")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Protección Antiahogamiento de Hardware",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Para evitar congelamientos o throttling térmico en juegos demandantes, el deslizador ajusta hasta 60 Mbps máximos estables. Además, cambiar de resolución NO alterará ni reseteará tu tasa de bits.",
                        fontSize = 10.5.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Text(
            text = "ACCESOS RÁPIDOS PREDEFINIDOS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )

        BITRATE_PRESETS.forEach { mbps ->
            val isSelected = selectedBitrateMbps == mbps
            val (label, tip) = when (mbps) {
                1 -> "1 Mbps • Modo Ultra Ahorro Mínimo" to "Cero calentamiento (~7.5 MB/min). Ideal para clases, tutoriales largos o llamadas."
                4 -> "4 Mbps • Modo Ahorro Liviano" to "Bajo consumo de almacenamiento y batería para videos de pantalla estáticos."
                8 -> "8 Mbps • Modo Balanceado" to "Ideal para videos de redes sociales y uso general con buena compresión."
                12 -> "12 Mbps • Recomendado Gaming ★" to "Equilibrio óptimo entre nitidez en juegos de acción y temperatura térmica del smartphone."
                16 -> "16 Mbps • Alta Calidad Gaming" to "Nitidez superior en juegos 60 FPS sin artefactos de compresión visual."
                24 -> "24 Mbps • Calidad Pro Creadores" to "Para pantallas 2K y creadores que editan posteriormente en Premiere o DaVinci."
                35 -> "35 Mbps • Ultra Nitidez 2K/4K" to "Tasa excelente para grabaciones dinámicas con movimientos de cámara continuos."
                50 -> "50 Mbps • Máxima Tasa Hardware" to "Máximo flujo sin compresión perceptible para codificadores NDK."
                60 -> "60 Mbps • Límite Seguro NDK" to "Tasa máxima soportada antes de saturar buses de almacenamiento eMMC."
                else -> "$mbps Mbps" to "Tasa personalizada de grabación."
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isRecording) { onBitrateChange(mbps) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) else DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) BorderStroke(1.5.dp, accentColor) else BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (isSelected) accentColor else Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tip,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Seleccionado",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 3. Pantalla Dedicada: Tasa de Cuadros (FPS)
 */
@Composable
private fun FpsSettingsSubScreen(
    selectedFps: Int,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onFpsChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader(
            title = "Tasa de Cuadros por Segundo (FPS)",
            description = "Establece la fluidez temporal de la captura. Tasas de 60 FPS o superiores proporcionan máxima suavidad visual en juegos rápidos.",
            accentColor = accentColor,
            onBack = onBack
        )

        FPS_PRESETS.forEach { fps ->
            val isSelected = selectedFps == fps
            val (title, description) = when (fps) {
                24 -> "24 FPS • Formato Cinematográfico" to "Movimiento clásico de cine. Menor carga térmica en el procesador."
                30 -> "30 FPS • Estándar para Tutoriales" to "Fluidez estándar suficiente para presentaciones, apps de texto y tutoriales."
                60 -> "60 FPS • Estándar Gaming Recomendado ★" to "Fluidez óptima requerida para juegos competitivos (Free Fire, COD Mobile, PUBG)."
                90 -> "90 FPS • Alta Tasa de Refresco" to "Aprovecha pantallas modernas de 90 Hz con suavidad extrema."
                120 -> "120 FPS • Ultra Fluidez Competitiva" to "Máximo rendimiento para dispositivos insignia con paneles de 120 Hz."
                else -> "$fps FPS" to "Tasa personalizada"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isRecording) { onFpsChange(fps) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) primaryColor.copy(alpha = 0.2f) else DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) BorderStroke(1.5.dp, primaryColor) else BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSelected) accentColor else Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = description,
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Seleccionado",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 4. Pantalla Dedicada: Códec de Hardware
 */
@Composable
private fun CodecSettingsSubScreen(
    selectedCodec: String,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onCodecChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader(
            title = "Códec de Hardware (AMediaCodec)",
            description = "Selecciona el algoritmo de compresión acelerado por el hardware de tu procesador móvil.",
            accentColor = accentColor,
            onBack = onBack
        )

        listOf(
            Triple("H264", "H.264 / AVC (Recomendado Universal)", "El estándar más compatible del mundo. Se reproduce en cualquier teléfono, WhatsApp, YouTube, televisión o editor de video."),
            Triple("HEVC", "H.265 / HEVC (Alta Eficiencia)", "Reduce el tamaño del video en un ~40% manteniendo la misma calidad. Ideal para ahorrar memoria en grabaciones largas."),
            Triple("AV1", "AV1 (Próxima Generación)", "Compresión de última generación libre de regalías. Requiere un procesador reciente con soporte de codificación AV1.")
        ).forEach { (codecKey, label, desc) ->
            val isSelected = selectedCodec == codecKey
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isRecording) { onCodecChange(codecKey) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) primaryColor.copy(alpha = 0.2f) else DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) BorderStroke(1.5.dp, primaryColor) else BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (isSelected) accentColor else Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Seleccionado",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 5. Pantalla Dedicada: Fuente de Audio
 */
@Composable
private fun AudioSettingsSubScreen(
    selectedAudio: String,
    isRecording: Boolean,
    accentColor: Color,
    onAudioChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader(
            title = "Fuente de Captura de Audio",
            description = "Configura de dónde se grabará el sonido en tu sesión. VeloRec graba en formato AAC estéreo de alta fidelidad.",
            accentColor = accentColor,
            onBack = onBack
        )

        listOf(
            Triple("both", "Mezcla Estéreo (Micrófono + Audio Interno)", "Graba tu voz y el sonido del juego o app al mismo tiempo sin desfases. Ideal para Gameplays y tutoriales."),
            Triple("mic", "Solo Micrófono", "Captura únicamente tu voz mediante el micrófono del teléfono. Ignora el audio interno."),
            Triple("internal", "Solo Audio Interno (Android 10+)", "Captura directamente el sonido interno de juegos y aplicaciones sin capturar ruidos del ambiente."),
            Triple("mute", "Silencio (Mute)", "Desactiva la grabación de audio. Genera un archivo MP4 sin pista de audio para máximo ahorro de espacio.")
        ).forEach { (audioKey, label, desc) ->
            val isSelected = selectedAudio == audioKey
            val icon = when (audioKey) {
                "mic" -> Icons.Default.Mic
                "mute" -> Icons.Default.VolumeOff
                "both" -> Icons.Default.Headphones
                else -> Icons.Default.VolumeUp
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isRecording) { onAudioChange(audioKey) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) else DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) BorderStroke(1.5.dp, accentColor) else BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) accentColor else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (isSelected) accentColor else Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Seleccionado",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 6. Pantalla Dedicada: Cuenta Regresiva de Inicio
 */
@Composable
private fun CountdownSettingsSubScreen(
    selectedCountdownSec: Int,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onCountdownChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader(
            title = "Cuenta Regresiva de Inicio",
            description = "Configura el tiempo de preparación antes de comenzar a capturar. Te permite cambiar de aplicación o juego tranquilamente mientras el temporizador corre en la barra de notificaciones.",
            accentColor = accentColor,
            onBack = onBack
        )

        COUNTDOWN_PRESETS.forEach { sec ->
            val isSelected = selectedCountdownSec == sec
            val (title, desc) = when (sec) {
                0 -> "0 segundos (Inmediata)" to "Comienza a grabar en el instante en que aceptas el permiso de pantalla sin ningún retardo."
                3 -> "3 segundos (Recomendada ★)" to "El tiempo ideal para minimizar VeloRec e iniciar en tu juego o aplicación."
                5 -> "5 segundos (Estándar)" to "Tiempo extendido para juegos pesados que tardan unos instantes en desplegarse."
                10 -> "10 segundos (Larga)" to "Para preparaciones complejas de tutoriales o configuraciones previas."
                else -> "${sec} segundos" to "Retardo personalizado"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isRecording) { onCountdownChange(sec) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) primaryColor.copy(alpha = 0.2f) else DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) BorderStroke(1.5.dp, primaryColor) else BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSelected) accentColor else Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Seleccionado",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 7. Pantalla Dedicada: Motor Nativo NDK
 */
@Composable
private fun EngineSettingsSubScreen(
    selectedEngine: String,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onEngineChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader(
            title = "Arquitectura Nativa NDK",
            description = "VeloRec utiliza Rust (Edition 2024) como motor exclusivo de captura por hardware Zero-Copy (sin GC ni cierres por LMK) y C++23 para aceleración de Shaders GPU y Libyuv.",
            accentColor = accentColor,
            onBack = onBack
        )

        // Rust 2024 (Motor Principal de Captura)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(
                containerColor = WarningAmber.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, WarningAmber)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🦀 Motor Rust (Edition 2024) — ACTIVO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WarningAmber
                    )
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Motor principal y exclusivo de grabación de pantalla. Garantía matemática de seguridad en memoria sin recolector de basura (Zero GC). Escribe directamente a AMediaMuxer del NDK con canales multihilo atómicos, inmune a cierres en segundo plano del sistema operativo.",
                    fontSize = 11.5.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // C++23 (Tubería de Shaders GPU y Procesamiento de Color)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Pipeline C++23 (Shaders GPU & Libyuv)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aceleración gráfica en C++23 con GLSL ES 3.20 y SPIR-V Vulkan (GpuShaderPipeline) para FaceCam circular antialiased, Chroma Key en GPU, Gaming Color Boost y conversión ultrarrápida de color con Google libyuv.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
