package com.velorec.recorder.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Headphones
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.data.model.RemoteUiConfig
import com.velorec.recorder.data.model.ResolutionPreset
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.SuccessGreen
import com.velorec.recorder.ui.theme.WarningAmber

/**
 * SettingsMainMenu.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Menú principal de la sección de Ajustes con arquitectura de navegación jerárquica:
 * - Tarjeta informativa de Server-Driven UI (GitHub Releases).
 * - Categoría 1: Video y Calidad Visual (Resolución, Bitrate, FPS, Códec).
 * - Categoría 2: Audio y Tiempo (Fuente de audio y Cuenta regresiva).
 * - Categoría 3: Motor Nativo NDK (C++23 vs Rust 2024).
 */
@Composable
fun SettingsMainMenu(
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
    onNavigateTo: (SettingsSubScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
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

        // Título del Grupo 2: Audio y Tiempo
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

        // Acceso directo a VeloRec Inspector (Crashes, Logcat y Diagnóstico)
        val context = LocalContext.current
        SettingsNavigationRow(
            icon = Icons.Default.BugReport,
            title = "VeloRec Inspector (Diagnóstico)",
            subtitle = "Logcat en tiempo real, caídas (crashes) y telemetría de Rust",
            badgeText = "🔍 Debug",
            accentColor = Color(0xFFC084FC),
            onClick = {
                context.startActivity(Intent(context, com.velorec.recorder.debug.DebugConsoleActivity::class.java))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
