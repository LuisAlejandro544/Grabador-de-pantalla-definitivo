package com.velorec.recorder.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.velorec.recorder.data.model.BITRATE_PRESETS
import com.velorec.recorder.data.model.COUNTDOWN_PRESETS
import com.velorec.recorder.data.model.FPS_PRESETS
import com.velorec.recorder.data.model.RESOLUTION_PRESETS
import com.velorec.recorder.data.model.getAdaptivePresetsForDevice
import com.velorec.recorder.data.model.RemoteUiConfig
import com.velorec.recorder.data.model.ResolutionPreset
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.SuccessGreen
import com.velorec.recorder.ui.theme.WarningAmber

/**
 * SettingsScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Implementa la pantalla de configuración técnica avanzada y parámetros de hardware.
 * 
 * LÓGICA Y OPCIONES:
 * 1. Tarjeta de estado de Server-Driven UI (GitHub Releases) con indicador ONLINE/LOCAL.
 * 2. Conmutador de Motor Nativo NDK: C++23 vs Rust 2024.
 * 3. Selector de Resolución con verificación de paridad YUV420p (480p hasta 4K).
 * 4. Tasa de Bits (Bitrate) con estimación de tamaño de archivo por minuto en tiempo real.
 * 5. Tasa de Cuadros (24, 30, 60, 90, 120 FPS).
 * 6. Códec de hardware: H.264 (AVC), H.265 (HEVC) y AV1.
 * 7. Fuente de audio: Mezcla estéreo, Micrófono, Audio interno y Silencio.
 * 8. Temporizador de cuenta regresiva de inicio (0s, 3s, 5s, 10s).
 */
@Composable
fun NativeSettingsSection(
    selectedEngine: String,
    selectedResolution: ResolutionPreset,
    selectedBitrateMbps: Int,
    selectedFps: Int,
    selectedCodec: String,
    selectedAudio: String,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    remoteConfig: RemoteUiConfig,
    availableResolutions: List<ResolutionPreset>? = null,
    selectedCountdownSec: Int = 3,
    onEngineChange: (String) -> Unit,
    onResolutionChange: (ResolutionPreset) -> Unit,
    onBitrateChange: (Int) -> Unit,
    onFpsChange: (Int) -> Unit,
    onCodecChange: (String) -> Unit,
    onAudioChange: (String) -> Unit,
    onCountdownChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val resolutions = availableResolutions ?: remember(context) { getAdaptivePresetsForDevice(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título de la Sección
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = primaryColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ajustes de Grabación y Hardware",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
        }

        // Tarjeta de Estado de Actualizaciones Remotas de GitHub Releases
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Configuración Remota (GitHub)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    Surface(
                        color = if (remoteConfig.isFetchedFromNetwork) SuccessGreen.copy(alpha = 0.2f) else DarkCard,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (remoteConfig.isFetchedFromNetwork) "ONLINE" else "LOCAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remoteConfig.isFetchedFromNetwork) SuccessGreen else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "La app lee automáticamente las actualizaciones de 'app_config.json' desde GitHub Releases. Si no hay conexión, opera con sus valores nativos.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(remoteConfig.apkDownloadUrl))
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver Releases en GitHub", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // 1. Selector de Motor Nativo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Motor Nativo por Hardware",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ambos motores graban directamente desde la GPU con Zero-Copy.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onEngineChange("cpp23") },
                        enabled = !isRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedEngine == "cpp23") primaryColor else DarkCard
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚡ C++23 NDK", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onEngineChange("rust2024") },
                        enabled = !isRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedEngine == "rust2024") WarningAmber else DarkCard
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🦀 Rust 2024", fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Selector de Resolución
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resolución de Grabación",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Paridad obligatoria YUV420p asegurada en todas las opciones.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(12.dp))

                resolutions.forEach { preset ->
                    val isSelected = selectedResolution.id == preset.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isRecording) { onResolutionChange(preset) },
                        color = if (isSelected) primaryColor.copy(alpha = 0.2f) else DarkCard,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, primaryColor) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = preset.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) accentColor else Color.White
                                )
                                Text(
                                    text = "${preset.width} x ${preset.height} px • ${if (preset.isNative) "📱 Nativa (${preset.scalePercent}%)" else "📺 Estándar 9:16"}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            if (isSelected) {
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
            }
        }

        // 3. Selector de Tasa de Bits (Bitrate)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tasa de Bits (Bitrate)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "$selectedBitrateMbps Mbps",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A mayor bitrate, mayor nitidez y mayor tamaño de archivo.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Chips de Bitrate Rápido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BITRATE_PRESETS.forEach { mbps ->
                        val isSelected = selectedBitrateMbps == mbps
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isRecording) { onBitrateChange(mbps) },
                            color = if (isSelected) accentColor else DarkCard
                        ) {
                            Text(
                                text = "${mbps}M",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Estimación de Tamaño de Archivo
                val estimatedMbPerMin = (selectedBitrateMbps * 60) / 8
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Peso estimado: ~$estimatedMbPerMin MB / min (~${estimatedMbPerMin * 10} MB por 10 min)",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }

        // 4. Selector de Tasa de Cuadros (FPS)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SlowMotionVideo, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tasa de Cuadros (FPS)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FPS_PRESETS.forEach { fps ->
                        val isSelected = selectedFps == fps
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isRecording) { onFpsChange(fps) },
                            color = if (isSelected) primaryColor else DarkCard
                        ) {
                            Text(
                                text = "$fps",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 5. Selector de Códec de Hardware
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Códec de Hardware (AMediaCodec)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "H264" to "H.264 (AVC)",
                        "HEVC" to "H.265 (HEVC)",
                        "AV1" to "AV1"
                    ).forEach { (codecKey, label) ->
                        val isSelected = selectedCodec == codecKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isRecording) { onCodecChange(codecKey) },
                            color = if (isSelected) primaryColor else DarkCard
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 6. Fuente de Captura de Audio
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fuente de Captura de Audio",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple("both", "Mezcla", Icons.Default.Headphones),
                        Triple("mic", "Micrófono", Icons.Default.Mic),
                        Triple("internal", "Interno", Icons.Default.VolumeUp),
                        Triple("mute", "Silencio", Icons.Default.VolumeOff)
                    ).forEach { (audioKey, label, audioIcon) ->
                        val isSelected = selectedAudio == audioKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isRecording) { onAudioChange(audioKey) },
                            color = if (isSelected) accentColor else DarkCard
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = audioIcon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Cuenta Regresiva de Inicio (Temporizador de Preparación)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cuenta Regresiva de Inicio",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Retardo antes de comenzar a capturar para preparar tu pantalla o cambiar de aplicación.",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    COUNTDOWN_PRESETS.forEach { sec ->
                        val isSelected = selectedCountdownSec == sec
                        val title = when (sec) {
                            0 -> "0s"
                            3 -> "3s ★"
                            5 -> "5s"
                            10 -> "10s"
                            else -> "${sec}s"
                        }
                        val subtitle = when (sec) {
                            0 -> "Inmediata"
                            3 -> "Recomendada"
                            5 -> "Estándar"
                            10 -> "Larga"
                            else -> "Segundos"
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isRecording) { onCountdownChange(sec) },
                            color = if (isSelected) primaryColor else DarkCard,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else null
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    fontSize = 9.sp,
                                    color = if (isSelected) accentColor else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

