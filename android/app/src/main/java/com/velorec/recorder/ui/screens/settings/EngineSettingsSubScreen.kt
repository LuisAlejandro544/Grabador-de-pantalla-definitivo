package com.velorec.recorder.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.velorec.recorder.ui.theme.WarningAmber

/**
 * EngineSettingsSubScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Pantalla dedicada para visualización y configuración de los motores nativos NDK:
 * - Rust 2024 (Motor Principal Exclusivo Zero-Copy & GC-Free, inmune a LMK).
 * - C++23 (Tubería de Shaders GPU GLSL ES 3.20 y SPIR-V Vulkan para FaceCam/Gaming Boost).
 */
@Composable
fun EngineSettingsSubScreen(
    selectedEngine: String,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onEngineChange: (String) -> Unit,
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
            title = "Arquitectura Nativa NDK",
            description = "VeloRec utiliza Rust (Edition 2024) como motor exclusivo de captura por hardware Zero-Copy (sin GC ni cierres por LMK) y C++23 para aceleración de Shaders GPU y Libyuv.",
            accentColor = accentColor,
            onBack = onBack
        )

        // Rust 2024 (Motor Principal de Captura)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(
                containerColor = WarningAmber.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, WarningAmber)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🦀 Motor Rust (Edition 2024) — ACTIVO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WarningAmber
                    )
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Motor principal y exclusivo de grabación de pantalla. Garantía matemática de seguridad en memoria sin recolector de basura (Zero GC). Escribe directamente a AMediaMuxer del NDK con canales multihilo atómicos, inmune a cierres en segundo plano del sistema operativo.",
                    fontSize = 11.5.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // C++23 (Tubería de Shaders GPU y Procesamiento de Color)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Pipeline C++23 Shaders & Rust YUV",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = accentColor
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aceleración gráfica en C++23 con GLSL ES 3.20 y SPIR-V Vulkan (GpuShaderPipeline) para FaceCam circular antialiased, Chroma Key y Gaming Boost en GPU. Conversión YUV/NV12 y escalado 100% en Rust 2024 con yuvutils-rs y aceleración SIMD ARM NEON.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
