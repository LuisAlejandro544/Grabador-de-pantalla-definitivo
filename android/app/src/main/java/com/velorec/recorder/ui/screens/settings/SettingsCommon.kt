package com.velorec.recorder.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface

/**
 * SettingsCommon.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Define los componentes y tipos comunes compartidos entre todas las subpantallas
 * de la sección de Ajustes:
 * 1. 'SettingsSubScreen': Enum de las pantallas de configuración.
 * 2. 'SubScreenHeader': Encabezado con botón ergonómico de retorno y títulos.
 * 3. 'SettingsNavigationRow': Tarjeta interactiva para navegar desde el menú principal.
 */
enum class SettingsSubScreen {
    MAIN,           // Menú principal con tarjetas y botones hacia cada apartado
    RESOLUTION,     // Pantalla dedicada: Resolución de Grabación
    BITRATE,        // Pantalla dedicada: Tasa de Bits
    FPS,            // Pantalla dedicada: Tasa de Cuadros (FPS)
    CODEC,          // Pantalla dedicada: Códec de Hardware
    AUDIO,          // Pantalla dedicada: Fuente de Captura de Audio
    COUNTDOWN,      // Pantalla dedicada: Cuenta Regresiva de Inicio
    ENGINE          // Pantalla dedicada: Motor Nativo (C++23 vs Rust 2024)
}

/**
 * Encabezado común para todas las subpantallas con botón "← Volver a Ajustes".
 */
@Composable
fun SubScreenHeader(
    title: String,
    description: String,
    accentColor: Color,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBack() },
            color = DarkCard,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFF475569).copy(alpha = 0.4f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Volver a Ajustes",
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = description,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

/**
 * Fila / Botón de Navegación elegante hacia cada pantalla de ajuste técnico.
 */
@Composable
fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0xFF475569).copy(alpha = 0.4f))
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
