package com.velorec.recorder.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * MetricBadge.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Componente modular reutilizable para mostrar métricas clave de hardware y rendimiento
 * en tiempo real (CPU, Latencia en milisegundos, Consumo de RAM y Tasa Zero-Copy).
 * 
 * LÓGICA Y DISEÑO:
 * - Diseñado para pantallas táctiles móviles, adaptándose dinámicamente al ancho disponible con Modifier.weight().
 * - Utiliza tipografía monoespaciada para valores numéricos para evitar saltos o vibraciones visuales.
 */
@Composable
fun MetricBadge(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Surface(
        modifier = modifier,
        color = DarkSurface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                fontSize = 12.sp,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
