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
import com.velorec.recorder.data.model.COUNTDOWN_PRESETS
import com.velorec.recorder.ui.theme.DarkSurface

/**
 * CountdownSettingsSubScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Pantalla dedicada para configuración del temporizador de inicio previo (Cuenta Regresiva):
 * - 0s (Inmediata / Desactivada)
 * - 3s (Ideal recomendada ★)
 * - 5s (Estándar para juegos pesados)
 * - 10s (Larga para tutoriales)
 */
@Composable
fun CountdownSettingsSubScreen(
    selectedCountdownSec: Int,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onCountdownChange: (Int) -> Unit,
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
