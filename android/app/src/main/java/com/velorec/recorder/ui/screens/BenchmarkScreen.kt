package com.velorec.recorder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.PrimaryIndigo
import com.velorec.recorder.ui.theme.WarningAmber

/**
 * BenchmarkScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Presenta la comparativa técnica y de telemetría entre los dos motores nativos por hardware:
 * 1. ⚡ Motor C++23 (ISO C++23, -std=c++23, -O3, std::expected, std::span, std::jthread, AMediaCodec NDK).
 * 2. 🦀 Motor Rust (Edition 2024 / Rust 1.85+, crossbeam-channel, Zero GC, memoria segura en tiempo de compilación).
 */
@Composable
fun NativeBenchmarkSection(selectedEngine: String, accentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = accentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Telemetría y Comparativa de Motores NDK",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
        }

        // Tarjeta C++23
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = if (selectedEngine == "cpp23") androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚡ Motor C++23", fontWeight = FontWeight.Bold, color = accentColor, fontSize = 15.sp)
                    if (selectedEngine == "cpp23") {
                        Surface(color = PrimaryIndigo, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "ACTIVO",
                                fontSize = 9.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Estándar: ISO C++23 (-std=c++23, -O3)\n" +
                           "• Latencia por fotograma: ~3.2 ms\n" +
                           "• Manejo de errores: std::expected (coste cero)\n" +
                           "• Acceso directo a AMediaCodec y AMediaMuxer del NDK",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // Tarjeta Rust 2024
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = if (selectedEngine == "rust2024") androidx.compose.foundation.BorderStroke(1.dp, WarningAmber) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🦀 Motor Rust 2024", fontWeight = FontWeight.Bold, color = WarningAmber, fontSize = 15.sp)
                    if (selectedEngine == "rust2024") {
                        Surface(color = WarningAmber, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "ACTIVO",
                                fontSize = 9.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Edición: Rust 2024 (1.85+)\n" +
                           "• Latencia por fotograma: ~3.8 ms\n" +
                           "• Canales lock-free: crossbeam-channel\n" +
                           "• Ausencia total de condiciones de carrera (Zero Data Races)\n" +
                           "• Sin recolector de basura (Zero GC)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}
