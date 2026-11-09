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
 * Pantalla principal de captura de pantalla y monitor de hardware en vivo.
 * 
 * ARQUITECTURA MODULAR DESACOPLADA:
 * Orquesta los submódulos especializados de la UI de grabación:
 * 1. Banner Dinámico Remoto (Server-Driven UI desde GitHub Releases).
 * 2. 'QuickQualityCard': Selector de resolución nativa, control fino de bitrate, audio y cuenta regresiva.
 * 3. 'RecorderHudCard': Visor tipo HUD de cámara con animaciones de pulso y cronómetro en vivo.
 * 4. 'HardwareTelemetryRow': Badges de telemetría de CPU, latencia NDK, memoria y Zero-Copy.
 * 5. 'RecorderActionButtons': Botones táctiles ergonómicos de gran tamaño (Pausar/Reanudar y Iniciar/Detener).
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Banner Dinámico Remoto (Server-Driven UI de GitHub Releases)
        if (!bannerText.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = bannerText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF1F5F9),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 2. Tarjeta Nativa Material 3: Configuración Rápida de Grabación
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
            onCountdownChange = onCountdownChange
        )

        // 3. Monitor de Captura Nativo (HUD Viewfinder)
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

        // 4. Métricas de Hardware en Vivo (Material 3 Badges)
        HardwareTelemetryRow(
            selectedEngine = selectedEngine,
            isRecording = isRecording
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Botones de Acción Táctiles Ergonómicos
        RecorderActionButtons(
            isRecording = isRecording,
            isPaused = isPaused,
            primaryColor = primaryColor,
            onToggleRecord = onToggleRecord,
            onTogglePause = onTogglePause
        )
    }
}
