package com.velorec.recorder.ui.components.recorder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import com.velorec.recorder.data.model.AUDIO_SOURCE_PRESETS
import com.velorec.recorder.data.model.BITRATE_PRESETS
import com.velorec.recorder.data.model.COUNTDOWN_PRESETS
import com.velorec.recorder.data.model.MIN_BITRATE_MBPS
import com.velorec.recorder.data.model.MAX_SAFE_BITRATE_MBPS
import com.velorec.recorder.data.model.ResolutionPreset
import com.velorec.recorder.data.model.getBitrateThermalAdvice
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface

/**
 * QuickQualityCard.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Componente modular de Material 3 que agrupa los controles táctiles de ajuste rápido
 * para la sesión de grabación:
 * 1. Selector de resolución con soporte de escalas nativas proporcionales.
 * 2. Control deslizante de bitrate con botones de paso fino (+ / -) y evaluación térmica.
 * 3. Selector rápido de fuente de audio (Interno, Micrófono, Mezcla, Silencio).
 * 4. Selector de temporizador de cuenta regresiva (0s, 3s, 5s, 10s).
 */
@Composable
fun QuickQualityCard(
    selectedResolution: ResolutionPreset,
    selectedBitrateMbps: Int,
    selectedAudio: String,
    selectedCountdownSec: Int,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    availableResolutions: List<ResolutionPreset>,
    onResolutionChange: (ResolutionPreset) -> Unit,
    onBitrateChange: (Int) -> Unit,
    onAudioChange: (String) -> Unit,
    onCountdownChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0xFF223147)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Cabecera de la Tarjeta con Resumen Activo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ajustes Rápidos de Calidad",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Badge del Perfil Activo
                val audioBadge = when (selectedAudio) {
                    "internal" -> "INTERNO"
                    "mic" -> "MIC"
                    "mute" -> "MUTE"
                    else -> "MEZCLA"
                }
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${selectedResolution.id.uppercase()} • ${selectedBitrateMbps}M • $audioBadge" + (if (selectedCountdownSec > 0) " • ${selectedCountdownSec}s" else " • 0s"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila 1: Selector de Resolución con Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Resolución de Pantalla",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableResolutions.forEach { preset ->
                    val isSelected = selectedResolution.id == preset.id
                    val chipColor = if (isSelected) {
                        if (preset.isNative) Color(0xFF059669) else primaryColor
                    } else {
                        if (preset.isNative) Color(0xFF064E3B).copy(alpha = 0.35f) else DarkCard
                    }
                    val borderColor = if (isSelected) {
                        if (preset.isNative) Color(0xFF34D399) else primaryColor
                    } else {
                        if (preset.isNative) Color(0xFF059669).copy(alpha = 0.5f) else Color(0xFF334155)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = chipColor,
                        border = BorderStroke(1.dp, borderColor),
                        shadowElevation = if (isSelected) 3.dp else 0.dp,
                        modifier = Modifier.clickable(enabled = !isRecording) {
                            onResolutionChange(preset)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            if (preset.isNative) {
                                Text(
                                    text = "📱 ",
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = preset.name.split(" ")[0] + " (${preset.label})",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else if (preset.isNative) Color(0xFF6EE7B7) else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila 2: Selector de Tasa de Bits (Bitrate) Personalizado con Protección Térmica
            val bitrateAdvice = getBitrateThermalAdvice(selectedBitrateMbps)
            val approxMbPerMin = (selectedBitrateMbps * 60) / 8
            val tierBadgeColor = when {
                selectedBitrateMbps <= 3 -> Color(0xFF10B981)
                selectedBitrateMbps <= 16 -> Color(0xFFF59E0B)
                selectedBitrateMbps <= 30 -> Color(0xFF38BDF8)
                selectedBitrateMbps <= 50 -> Color(0xFFA855F7)
                else -> Color(0xFFEF4444)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Tasa de Bits Personalizada",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tierBadgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, tierBadgeColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(tierBadgeColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = bitrateAdvice.tierName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierBadgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Contenedor táctil del Deslizador y Steppers
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón de reducción precisa [-]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(enabled = !isRecording && selectedBitrateMbps > MIN_BITRATE_MBPS) {
                                    onBitrateChange((selectedBitrateMbps - 1).coerceAtLeast(MIN_BITRATE_MBPS))
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Disminuir Bitrate",
                                    tint = if (selectedBitrateMbps > MIN_BITRATE_MBPS) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Deslizador continuo
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
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

                        // Botón de aumento preciso [+]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(enabled = !isRecording && selectedBitrateMbps < MAX_SAFE_BITRATE_MBPS) {
                                    onBitrateChange((selectedBitrateMbps + 1).coerceAtMost(MAX_SAFE_BITRATE_MBPS))
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Aumentar Bitrate",
                                    tint = if (selectedBitrateMbps < MAX_SAFE_BITRATE_MBPS) Color.White else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Indicadores clave del rango
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "1 Mbps (Mínimo)", fontSize = 9.sp, color = Color(0xFF64748B))
                        Text(
                            text = "$selectedBitrateMbps Mbps (~$approxMbPerMin MB/min)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(text = "60 Mbps (Máx Seguro)", fontSize = 9.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Fila de accesos directos rápidos (1-tap presets)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BITRATE_PRESETS.forEach { mbps ->
                    val isSelected = selectedBitrateMbps == mbps
                    val isIdeal = mbps == 12
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) accentColor else DarkCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) accentColor else Color(0xFF334155)
                        ),
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier.clickable(enabled = !isRecording) {
                            onBitrateChange(mbps)
                        }
                    ) {
                        Text(
                            text = if (isIdeal) "${mbps}M ★" else "${mbps}M",
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila 3: Fuente de Audio
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = when (selectedAudio) {
                        "internal" -> Icons.Default.VolumeUp
                        "mic" -> Icons.Default.Mic
                        "mute" -> Icons.Default.VolumeOff
                        else -> Icons.Default.Headphones
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Fuente de Audio (Sonido)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AUDIO_SOURCE_PRESETS.forEach { audioPreset ->
                    val isSelected = selectedAudio == audioPreset.id
                    val isInternal = audioPreset.id == "internal"
                    val chipColor = if (isSelected) {
                        if (isInternal) Color(0xFF0284C7) else primaryColor
                    } else {
                        if (isInternal) Color(0xFF0369A1).copy(alpha = 0.25f) else DarkCard
                    }
                    val borderColor = if (isSelected) {
                        if (isInternal) Color(0xFF38BDF8) else primaryColor
                    } else {
                        if (isInternal) Color(0xFF0284C7).copy(alpha = 0.45f) else Color(0xFF334155)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = chipColor,
                        border = BorderStroke(1.dp, borderColor),
                        shadowElevation = if (isSelected) 3.dp else 0.dp,
                        modifier = Modifier.clickable(enabled = !isRecording) {
                            onAudioChange(audioPreset.id)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${audioPreset.iconEmoji} ",
                                fontSize = 11.sp
                            )
                            Text(
                                text = audioPreset.label + if (isInternal) " ★" else "",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else if (isInternal) Color(0xFF7DD3FC) else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila 4: Temporizador de Inicio (Cuenta Regresiva)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Cuenta Regresiva de Inicio",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COUNTDOWN_PRESETS.forEach { sec ->
                    val isSelected = selectedCountdownSec == sec
                    val label = when (sec) {
                        0 -> "0s (Inmediata)"
                        3 -> "3s (Ideal ★)"
                        else -> "${sec}s"
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) primaryColor else DarkCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) primaryColor else Color(0xFF334155)
                        ),
                        shadowElevation = if (isSelected) 3.dp else 0.dp,
                        modifier = Modifier.clickable(enabled = !isRecording) {
                            onCountdownChange(sec)
                        }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}
