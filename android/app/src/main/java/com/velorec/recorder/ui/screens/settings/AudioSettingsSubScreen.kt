package com.velorec.recorder.ui.screens.settings

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface

/**
 * AudioSettingsSubScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Pantalla dedicada para configuración de la fuente de captura de audio:
 * - Mezcla Estéreo (Micrófono + Audio Interno)
 * - Solo Micrófono (Captura de voz)
 * - Solo Audio Interno (Sonido puro de juegos/apps sin ruidos externos)
 * - Silencio (Mute para ahorro de espacio)
 */
@Composable
fun AudioSettingsSubScreen(
    selectedAudio: String,
    isRecording: Boolean,
    accentColor: Color,
    onAudioChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
