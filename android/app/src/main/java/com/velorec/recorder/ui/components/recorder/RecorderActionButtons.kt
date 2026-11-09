package com.velorec.recorder.ui.components.recorder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.RecordRed
import com.velorec.recorder.ui.theme.WarningAmber

/**
 * RecorderActionButtons.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Componente modular con los botones de acción ergonómicos de gran área táctil
 * para interactuar con una sola mano en smartphones:
 * 1. Botón de Pausa / Reanudación (solo visible cuando la grabación está en curso).
 * 2. Botón Principal: INICIAR GRABACIÓN / DETENER GRABACIÓN con colores de alto contraste.
 */
@Composable
fun RecorderActionButtons(
    isRecording: Boolean,
    isPaused: Boolean,
    primaryColor: Color,
    onToggleRecord: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        if (isRecording) {
            // Botón de Pausa / Reanudar
            Button(
                onClick = onTogglePause,
                colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(58.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pausar o Reanudar",
                    tint = if (isPaused) WarningAmber else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Botón Principal: INICIAR / DETENER GRABACIÓN
        Button(
            onClick = onToggleRecord,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) RecordRed else primaryColor
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .height(58.dp)
                .weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isRecording) "DETENER GRABACIÓN" else "INICIAR GRABACIÓN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
