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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.data.model.BITRATE_PRESETS
import com.velorec.recorder.data.model.MIN_BITRATE_MBPS
import com.velorec.recorder.data.model.MAX_SAFE_BITRATE_MBPS
import com.velorec.recorder.data.model.getBitrateThermalAdvice
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.SuccessGreen

/**
 * BitrateSettingsSubScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Pantalla dedicada e independiente para configuración de la tasa de bits (Bitrate):
 * 1. Calculadora de almacenamiento en tiempo real (1 min, 10 min, 1 hora).
 * 2. Indicador dinámico de nivel térmico ("Antiahogamiento de Hardware").
 * 3. Deslizador táctil continuo de 1 a 60 Mbps con botones de paso fino (+ / -).
 * 4. Presets rápidos de 1-tap con explicaciones orientadas a gaming y creación de contenido.
 */
@Composable
fun BitrateSettingsSubScreen(
    selectedBitrateMbps: Int,
    isRecording: Boolean,
    accentColor: Color,
    onBitrateChange: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estimatedMbPerMin = (selectedBitrateMbps * 60) / 8
    val estimatedMbPerHour = estimatedMbPerMin * 60

    Column(
        modifier = modifier
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

        // Tarjeta Informativa de Estabilidad Térmica
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
