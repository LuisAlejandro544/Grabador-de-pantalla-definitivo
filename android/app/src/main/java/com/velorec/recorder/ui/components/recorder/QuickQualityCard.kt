package com.velorec.recorder.ui.components.recorder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
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
import com.velorec.recorder.ui.theme.PrimaryIndigo

/**
 * QuickQualityCard.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Componente modular de Material 3 con diseño táctil ergonómico optimizado para smartphones.
 * 
 * MEJORAS DE ERGONOMÍA Y DISEÑO NATIVO:
 * 1. Perfiles Gamers y Creadores de 1-Toque (Gaming 60 FPS, Pro 120 FPS, Ahorro Batería, Tutorial con Voz).
 * 2. Tarjetas táctiles segmentadas y cómodas (touch targets >= 48dp) sin apiñamiento.
 * 3. Selector de resolución con aspecto nativo proporcional y cálculo de paridad de píxeles.
 * 4. Control de bitrate con estimación de almacenamiento en vivo (MB/min) y evaluación térmica segura.
 * 5. Acceso directo a la pantalla completa de Ajustes Técnicos para configuraciones avanzadas.
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
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0xFF223147)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Cabecera elegante con icono y resumen de perfil
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Ajustes Rápidos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Personaliza tu sesión de captura",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Resumen compacto del perfil
                val audioSummary = when (selectedAudio) {
                    "internal" -> "Interno"
                    "mic" -> "Micro"
                    "mute" -> "Mute"
                    else -> "Mezcla"
                }
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = "${selectedResolution.label} • ${selectedBitrateMbps}M • $audioSummary",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. PERFILES RÁPIDOS 1-TAP (GAMING, PRO, AHORRO, TUTORIAL)
            Text(
                text = "PERFILES RÁPIDOS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Perfil 1: Gamer Óptimo (Nativa FHD / 8 Mbps / Audio Interno / 3s)
                val isGamerPreset = selectedBitrateMbps == 8 && selectedAudio == "internal"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGamerPreset) Color(0xFF065F46) else DarkCard,
                    border = BorderStroke(1.dp, if (isGamerPreset) Color(0xFF34D399) else Color(0xFF334155)),
                    modifier = Modifier.clickable(enabled = !isRecording) {
                        availableResolutions.firstOrNull { it.id == "native_fhd" || it.id == "native_100" }?.let {
                            onResolutionChange(it)
                        }
                        onBitrateChange(8)
                        onAudioChange("internal")
                        onCountdownChange(3)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = if (isGamerPreset) Color.White else Color(0xFF34D399),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gamer 60 FPS",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isGamerPreset) Color.White else Color(0xFFE2E8F0)
                        )
                    }
                }

                // Perfil 2: Máxima Nitidez 120 FPS (Nativa FHD / 16 Mbps / Audio Interno / 3s)
                val isProPreset = selectedBitrateMbps == 16 && selectedAudio == "internal"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isProPreset) primaryColor else DarkCard,
                    border = BorderStroke(1.dp, if (isProPreset) Color(0xFF818CF8) else Color(0xFF334155)),
                    modifier = Modifier.clickable(enabled = !isRecording) {
                        availableResolutions.firstOrNull { it.id == "native_100" || it.id == "native_fhd" }?.let {
                            onResolutionChange(it)
                        }
                        onBitrateChange(16)
                        onAudioChange("internal")
                        onCountdownChange(3)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isProPreset) Color.White else Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ultra 120 FPS",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isProPreset) Color.White else Color(0xFFE2E8F0)
                        )
                    }
                }

                // Perfil 3: Ahorro Gaming (Nativa SD / 4 Mbps / Audio Interno / 3s)
                val isBatteryPreset = selectedBitrateMbps == 4
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isBatteryPreset) Color(0xFF1E3A8A) else DarkCard,
                    border = BorderStroke(1.dp, if (isBatteryPreset) Color(0xFF60A5FA) else Color(0xFF334155)),
                    modifier = Modifier.clickable(enabled = !isRecording) {
                        availableResolutions.firstOrNull { it.id == "native_sd" || it.id == "native_hd" }?.let {
                            onResolutionChange(it)
                        }
                        onBitrateChange(4)
                        onAudioChange("internal")
                        onCountdownChange(3)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = "🔋 ", fontSize = 11.sp)
                        Text(
                            text = "Ahorro Batería",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBatteryPreset) Color.White else Color(0xFFE2E8F0)
                        )
                    }
                }

                // Perfil 4: Tutorial con Micrófono (Nativa HD / 6-8 Mbps / Mezcla / 3s)
                val isTutorialPreset = selectedAudio == "both" || selectedAudio == "mic"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isTutorialPreset) Color(0xFF581C87) else DarkCard,
                    border = BorderStroke(1.dp, if (isTutorialPreset) Color(0xFFC084FC) else Color(0xFF334155)),
                    modifier = Modifier.clickable(enabled = !isRecording) {
                        availableResolutions.firstOrNull { it.id == "native_hd" }?.let {
                            onResolutionChange(it)
                        }
                        onBitrateChange(8)
                        onAudioChange("both")
                        onCountdownChange(3)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isTutorialPreset) Color.White else Color(0xFFC084FC),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tutorial con Voz",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTutorialPreset) Color.White else Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. SELECTOR DE RESOLUCIÓN DE PANTALLA
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
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Resolución de Pantalla",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
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
                        DarkCard
                    }
                    val borderColor = if (isSelected) {
                        if (preset.isNative) Color(0xFF34D399) else primaryColor
                    } else {
                        Color(0xFF334155)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = chipColor,
                        border = BorderStroke(1.dp, borderColor),
                        shadowElevation = if (isSelected) 3.dp else 0.dp,
                        modifier = Modifier.clickable(enabled = !isRecording) {
                            onResolutionChange(preset)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (preset.isNative) {
                                Text(text = "📱 ", fontSize = 11.sp)
                            }
                            Column {
                                Text(
                                    text = preset.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFFF1F5F9)
                                )
                                Text(
                                    text = "${preset.width}x${preset.height}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) Color(0xFFD1FAE5) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. FUENTE DE AUDIO (SONIDO)
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
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Fuente de Audio",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
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
                        DarkCard
                    }
                    val borderColor = if (isSelected) {
                        if (isInternal) Color(0xFF38BDF8) else primaryColor
                    } else {
                        Color(0xFF334155)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = chipColor,
                        border = BorderStroke(1.dp, borderColor),
                        shadowElevation = if (isSelected) 3.dp else 0.dp,
                        modifier = Modifier.clickable(enabled = !isRecording) {
                            onAudioChange(audioPreset.id)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "${audioPreset.iconEmoji} ",
                                fontSize = 12.sp
                            )
                            Text(
                                text = audioPreset.label + if (isInternal) " ★" else "",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. CONTROL DE TASA DE BITS (BITRATE)
            val bitrateAdvice = getBitrateThermalAdvice(selectedBitrateMbps)
            val approxMbPerMin = (selectedBitrateMbps * 60) / 8
            val tierBadgeColor = when {
                selectedBitrateMbps <= 3 -> Color(0xFF10B981)
                selectedBitrateMbps <= 16 -> Color(0xFFF59E0B)
                selectedBitrateMbps <= 30 -> Color(0xFF38BDF8)
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Calidad de Video (Bitrate)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tierBadgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, tierBadgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = bitrateAdvice.tierName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierBadgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .size(36.dp)
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
                                .size(36.dp)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "1 Mbps", fontSize = 9.sp, color = Color(0xFF64748B))
                        Text(
                            text = "$selectedBitrateMbps Mbps (~$approxMbPerMin MB/min)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(text = "60 Mbps", fontSize = 9.sp, color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. CUENTA REGRESIVA DE INICIO
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
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cuenta Regresiva",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COUNTDOWN_PRESETS.forEach { sec ->
                    val isSelected = selectedCountdownSec == sec
                    val label = when (sec) {
                        0 -> "0s"
                        3 -> "3s ★"
                        else -> "${sec}s"
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) primaryColor else DarkCard,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) primaryColor else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !isRecording) {
                                onCountdownChange(sec)
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. ENLACE DIRECTO A AJUSTES AVANZADOS (Desacoplamiento modular limpio)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSettings() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ajustes Avanzados de Códec y Motor NDK",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
