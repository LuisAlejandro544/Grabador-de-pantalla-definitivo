package com.velorec.recorder.ui.components.recorder

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.data.model.ResolutionPreset
import com.velorec.recorder.ui.theme.DarkBackground
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.RecordRed
import com.velorec.recorder.ui.theme.SuccessGreen
import com.velorec.recorder.ui.theme.WarningAmber

/**
 * RecorderHudCard.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Componente modular que renderiza el visor interactivo (HUD Viewfinder) de captura.
 * 
 * LÓGICA Y ARQUITECTURA MODULAR:
 * 1. Muestra el estado activo de captura (Listo, Grabando en Vivo, Pausado) con animaciones de pulso.
 * 2. Formatea el cronómetro milimétrico en tiempo real (%02d:%02d).
 * 3. Expone el badge del motor nativo activo (Rust 2024 Zero-Copy vs C++23 NDK).
 * 4. Presenta la ficha técnica resumida del perfil seleccionado.
 */
@Composable
fun RecorderHudCard(
    selectedEngine: String,
    selectedResolution: ResolutionPreset,
    selectedFps: Int,
    selectedBitrateMbps: Int,
    isRecording: Boolean,
    isPaused: Boolean,
    durationSec: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0xFF223147)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF131D30), DarkBackground)
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Etiqueta superior de estado y motor
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de Estado
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isRecording && isPaused -> WarningAmber
                                    isRecording -> RecordRed.copy(alpha = pulseAlpha)
                                    else -> SuccessGreen
                                }
                            )
                    )
                    Text(
                        text = when {
                            isRecording && isPaused -> "PAUSADO"
                            isRecording -> "GRABANDO EN VIVO"
                            else -> "LISTO PARA CAPTURAR"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isRecording && isPaused -> WarningAmber
                            isRecording -> RecordRed
                            else -> SuccessGreen
                        },
                        letterSpacing = 0.5.sp
                    )
                }

                // Badge del Motor Activo
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = if (selectedEngine == "rust2024") "🦀 RUST ZERO-COPY" else "⚡ C++23 NDK",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedEngine == "rust2024") WarningAmber else accentColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            // Contenido Central del HUD
            if (isRecording) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(RecordRed.copy(alpha = if (isPaused) 0.5f else pulseAlpha))
                        )
                        Text(
                            text = "%02d:%02d".format(durationSec / 60, durationSec % 60),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp,
                            color = if (isPaused) WarningAmber else Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${selectedResolution.name} • ${selectedFps} FPS • ${selectedBitrateMbps} Mbps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Listo para Grabar",
                            tint = accentColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Listo para Grabar Pantalla y Audio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${selectedResolution.name} • ${selectedFps} FPS • ${selectedBitrateMbps} Mbps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Pie del HUD: especificación de audio activo
            Text(
                text = "Tubería Zero-Copy de Hardware NDK • Captura en Segundo Plano",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
