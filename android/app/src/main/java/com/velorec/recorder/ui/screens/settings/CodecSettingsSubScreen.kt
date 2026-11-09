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
import com.velorec.recorder.ui.theme.DarkSurface

/**
 * CodecSettingsSubScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Pantalla dedicada para selección del códec de video por hardware (AMediaCodec):
 * - H.264 / AVC (Compatibilidad universal)
 * - H.265 / HEVC (Alta compresión eficiente)
 * - AV1 (Próxima generación abierta)
 */
@Composable
fun CodecSettingsSubScreen(
    selectedCodec: String,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onCodecChange: (String) -> Unit,
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
