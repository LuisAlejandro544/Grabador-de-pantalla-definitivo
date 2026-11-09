package com.velorec.recorder.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
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
import com.velorec.recorder.data.model.FPS_PRESETS
import com.velorec.recorder.ui.theme.DarkSurface

/**
 * FpsSettingsSubScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Pantalla dedicada para configuración de la tasa de cuadros por segundo (FPS):
 * - 24 FPS (Cinematográfico)
 * - 30 FPS (Tutoriales estándar)
 * - 60 FPS (Recomendado Gaming ★)
 * - 90 FPS (Pantallas 90 Hz)
 * - 120 FPS (Competitivo ultra fluido)
 */
@Composable
fun FpsSettingsSubScreen(
    selectedFps: Int,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onFpsChange: (Int) -> Unit,
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
