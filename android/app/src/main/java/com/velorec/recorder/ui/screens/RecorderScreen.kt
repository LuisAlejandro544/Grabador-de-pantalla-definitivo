package com.velorec.recorder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.data.model.RESOLUTION_PRESETS
import com.velorec.recorder.data.model.ResolutionPreset
import com.velorec.recorder.ui.components.recorder.HardwareTelemetryRow
import com.velorec.recorder.ui.components.recorder.QuickQualityCard
import com.velorec.recorder.ui.components.recorder.RecorderActionButtons
import com.velorec.recorder.ui.components.recorder.RecorderHudCard

/**
 * RecorderScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Pantalla principal de captura de pantalla y monitor de hardware en vivo en Jetpack Compose.
 * 
 * ARQUITECTURA MODULAR DE EXPERIENCIA NATIVA ERGONÓMICA:
 * 1. Banner Dinámico Remoto (Server-Driven UI desde GitHub Releases).
 * 2. 'RecorderHudCard' (Consola Viewfinder Hero en la parte superior): Muestra el estado activo
 *    y cronómetro inmediatamente sin tener que scrollear.
 * 3. 'RecorderActionButtons' (Botón Principal Iniciar/Detener): Ubicado justo debajo del visor
 *    para acceso instantáneo de 1 solo toque con el pulgar.
 * 4. 'HardwareTelemetryRow': Métricas en tiempo real de CPU, latencia NDK, memoria y Zero-Copy.
 * 5. 'QuickQualityCard': Perfiles de 1-toque (Gamer, Ultra 120, Ahorro, Tutorial) y ajustes
 *    táctiles ergonómicos, con enlace directo a la pestaña de Ajustes Avanzados.
 */
@Composable
fun NativeRecorderViewport(
    selectedEngine: String,
    selectedResolution: ResolutionPreset,
    selectedBitrateMbps: Int,
    selectedFps: Int,
    isRecording: Boolean,
    isPaused: Boolean,
    durationSec: Int,
    accentColor: Color,
    primaryColor: Color,
    bannerText: String?,
    availableResolutions: List<ResolutionPreset> = RESOLUTION_PRESETS,
    selectedCountdownSec: Int = 3,
    selectedAudio: String = "internal",
    onResolutionChange: (ResolutionPreset) -> Unit,
    onBitrateChange: (Int) -> Unit,
    onAudioChange: (String) -> Unit = {},
    onCountdownChange: (Int) -> Unit = {},
    onToggleRecord: () -> Unit,
    onTogglePause: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Banner Dinámico Remoto (Server-Driven UI de GitHub Releases)
        if (!bannerText.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = bannerText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF1F5F9),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // 2. HERO VIEWFINDER DE CÁMARA (HUD Viewfinder)
        // Posicionado en la parte superior para visibilidad inmediata al abrir la app
        RecorderHudCard(
            selectedEngine = selectedEngine,
            selectedResolution = selectedResolution,
            selectedFps = selectedFps,
            selectedBitrateMbps = selectedBitrateMbps,
            isRecording = isRecording,
            isPaused = isPaused,
            durationSec = durationSec,
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. BOTÓN PRINCIPAL DE ACCIÓN (INICIAR / DETENER GRABACIÓN)
        // Inmediatamente accesible con una sola mano sin necesidad de hacer scroll
        RecorderActionButtons(
            isRecording = isRecording,
            isPaused = isPaused,
            primaryColor = primaryColor,
            onToggleRecord = onToggleRecord,
            onTogglePause = onTogglePause
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4. TELEMETRÍA DE HARDWARE EN VIVO (CPU, Latencia NDK, RAM, Zero-Copy)
        HardwareTelemetryRow(
            selectedEngine = selectedEngine,
            isRecording = isRecording
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 5. AJUSTES RÁPIDOS Y PERFILES GAMERS DE 1-TOQUE
        QuickQualityCard(
            selectedResolution = selectedResolution,
            selectedBitrateMbps = selectedBitrateMbps,
            selectedAudio = selectedAudio,
            selectedCountdownSec = selectedCountdownSec,
            isRecording = isRecording,
            accentColor = accentColor,
            primaryColor = primaryColor,
            availableResolutions = availableResolutions,
            onResolutionChange = onResolutionChange,
            onBitrateChange = onBitrateChange,
            onAudioChange = onAudioChange,
            onCountdownChange = onCountdownChange,
            onOpenSettings = onOpenSettings
        )
    }
}
