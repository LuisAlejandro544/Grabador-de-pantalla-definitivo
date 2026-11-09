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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.data.model.ResolutionPreset
import com.velorec.recorder.data.model.STANDARD_POPULAR_PRESETS
import com.velorec.recorder.data.model.createCustomResolutionPreset
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.SuccessGreen
import com.velorec.recorder.ui.theme.WarningAmber

/**
 * ResolutionSettingsSubScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Pantalla dedicada e independiente para configuración de resoluciones de pantalla:
 * 1. Pestaña 'Nativas': Resoluciones proporcionales adaptadas a la pantalla física del móvil (100% borde a borde).
 * 2. Pestaña 'Estándar': Formatos populares universales (20:9, 19.5:9, 19:9, 21:9, 9:16).
 * 3. Pestaña 'Personalizada': Editor interactivo con validación obligatoria de paridad YUV420p (ancho % 2 == 0 && alto % 2 == 0).
 */
enum class ResolutionTab {
    NATIVE, STANDARD, CUSTOM
}

@Composable
fun ResolutionSettingsSubScreen(
    resolutions: List<ResolutionPreset>,
    selectedResolution: ResolutionPreset,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onResolutionChange: (ResolutionPreset) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nativePresets = remember(resolutions) { resolutions.filter { it.isNative } }
    val initialTab = remember(selectedResolution) {
        when {
            selectedResolution.isCustom -> ResolutionTab.CUSTOM
            selectedResolution.isNative -> ResolutionTab.NATIVE
            else -> ResolutionTab.STANDARD
        }
    }
    var currentTab by remember { mutableStateOf(initialTab) }
    var standardFilter by remember { mutableStateOf("TODAS") }

    // Estados para la pestaña de resolución personalizada
    var customWidthText by remember(selectedResolution) {
        mutableStateOf(if (selectedResolution.isCustom) selectedResolution.width.toString() else "1080")
    }
    var customHeightText by remember(selectedResolution) {
        mutableStateOf(if (selectedResolution.isCustom) selectedResolution.height.toString() else "2400")
    }
    var customSavedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubScreenHeader(
            title = "Resolución de Grabación",
            description = "Elige entre resoluciones nativas que llenan toda tu pantalla, formatos estándar universales o crea tu propia resolución personalizada.",
            accentColor = accentColor,
            onBack = onBack
        )

        // BARRA DE NAVEGACIÓN EN PESTAÑAS
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab Nativas
                val isNativeTab = currentTab == ResolutionTab.NATIVE
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { currentTab = ResolutionTab.NATIVE },
                    color = if (isNativeTab) primaryColor.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isNativeTab) BorderStroke(1.dp, primaryColor) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = if (isNativeTab) accentColor else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nativas",
                            fontWeight = if (isNativeTab) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isNativeTab) accentColor else Color(0xFF94A3B8)
                        )
                    }
                }

                // Tab Estándar
                val isStandardTab = currentTab == ResolutionTab.STANDARD
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { currentTab = ResolutionTab.STANDARD },
                    color = if (isStandardTab) primaryColor.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isStandardTab) BorderStroke(1.dp, primaryColor) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = if (isStandardTab) accentColor else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Estándar",
                            fontWeight = if (isStandardTab) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isStandardTab) accentColor else Color(0xFF94A3B8)
                        )
                    }
                }

                // Tab Personalizada
                val isCustomTab = currentTab == ResolutionTab.CUSTOM
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { currentTab = ResolutionTab.CUSTOM },
                    color = if (isCustomTab) primaryColor.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isCustomTab) BorderStroke(1.dp, primaryColor) else null
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isCustomTab) accentColor else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Personalizada",
                            fontWeight = if (isCustomTab) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isCustomTab) accentColor else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        when (currentTab) {
            // PESTAÑA 1: RESOLUCIONES NATIVAS DE LA PANTALLA
            ResolutionTab.NATIVE -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100% de tu pantalla (Borde a Borde)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Estas resoluciones se calculan a la medida exacta de tu celular. Cubren toda la pantalla sin bandas negras ni deformar la imagen.",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.5.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💡 ¿Cómo influye la opción que elijas?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Alta Calidad (100% Nativa o FHD): Máxima nitidez pixel a pixel. Los textos finos y pequeños se leen perfectos.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "• Ahorro y Gaming (HD o 540p): Sigue llenando el 100% de tu pantalla, pero los bordes se verán más suaves o pixelados. A cambio, el celular NO se calienta, la batería dura más y los juegos van fluidos.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "OPCIONES ADAPTADAS A TU DISPOSITIVO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )

                nativePresets.forEach { preset ->
                    val isSelected = selectedResolution.id == preset.id
                    val qualityDescription = when (preset.id) {
                        "native_100" -> "Máxima nitidez absoluta (Pixel-Perfect) • Ideal para tutoriales"
                        "native_fhd" -> "Alta definición balanceada • Recomendada ★"
                        "native_hd" -> "Buen equilibrio • Archivos más livianos y menor consumo"
                        "native_sd" -> "Modo Gaming y Ahorro • Máxima fluidez sin calentar el móvil"
                        "native_2k" -> "Ultra definición / Supermuestreo de alta gama"
                        else -> "100% Pantalla completa sin bordes negros"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRecording) { onResolutionChange(preset) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) primaryColor.copy(alpha = 0.2f) else DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, primaryColor) else BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) accentColor else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = primaryColor.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${preset.scalePercent}%",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "${preset.width} x ${preset.height} px • Bitrate sugerido: ${preset.recommendedBitrateMbps} Mbps",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = qualityDescription,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                    lineHeight = 14.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Seleccionada",
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // PESTAÑA 2: RESOLUCIONES ESTÁNDAR (POPULARES)
            ResolutionTab.STANDARD -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Formatos Estándar de la Industria",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Medidas universales usadas por teléfonos (20:9, 19.5:9, 19:9, 21:9) y plataformas de video vertical (9:16).",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "💡 Si tu pantalla no tiene esta proporción exacta, el video tendrá bordes de encuadre limpios, ideal para publicar en YouTube Shorts, TikTok o Reels sin recortes indeseados.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )
                    }
                }

                Text(
                    text = "FILTRAR POR PROPORCIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )

                val filterOptions = listOf("TODAS", "20:9", "19.5:9", "19:9", "21:9", "9:16")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.forEach { filter ->
                        val isFilterActive = standardFilter == filter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { standardFilter = filter },
                            color = if (isFilterActive) primaryColor.copy(alpha = 0.3f) else DarkCard,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isFilterActive) primaryColor else Color(0xFF334155))
                        ) {
                            Text(
                                text = filter,
                                fontSize = 10.sp,
                                fontWeight = if (isFilterActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isFilterActive) accentColor else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                val filteredPresets = remember(standardFilter) {
                    if (standardFilter == "TODAS") {
                        STANDARD_POPULAR_PRESETS
                    } else {
                        STANDARD_POPULAR_PRESETS.filter { it.category == standardFilter || it.aspectRatioLabel == standardFilter }
                    }
                }

                filteredPresets.forEach { preset ->
                    val isSelected = selectedResolution.id == preset.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isRecording) { onResolutionChange(preset) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) primaryColor.copy(alpha = 0.2f) else DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, primaryColor) else BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = preset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = if (isSelected) accentColor else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = DarkCard,
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, Color(0xFF475569))
                                    ) {
                                        Text(
                                            text = preset.aspectRatioLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "${preset.width} x ${preset.height} px • Bitrate: ${preset.recommendedBitrateMbps} Mbps • ${preset.label}",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Seleccionada",
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // PESTAÑA 3: RESOLUCIÓN PERSONALIZADA
            ResolutionTab.CUSTOM -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Configura tus Medidas a Mano",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Introduce el ancho y alto que desees. VeloRec Studio corregirá automáticamente números impares asegurando la paridad obligatoria de codificación por hardware AMediaCodec.",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.5.sp
                        )
                    }
                }

                val rawW = customWidthText.toIntOrNull() ?: 1080
                val rawH = customHeightText.toIntOrNull() ?: 2400
                val evenW = maxOf(240, if (rawW % 2 == 0) rawW else rawW - 1)
                val evenH = maxOf(240, if (rawH % 2 == 0) rawH else rawH - 1)
                val aspectDecimal = evenH.toDouble() / evenW.toDouble().coerceAtLeast(1.0)
                val calculatedRatio = when {
                    Math.abs(aspectDecimal - (20.0 / 9.0)) < 0.025 -> "20:9"
                    Math.abs(aspectDecimal - (19.5 / 9.0)) < 0.025 -> "19.5:9"
                    Math.abs(aspectDecimal - (19.0 / 9.0)) < 0.025 -> "19:9"
                    Math.abs(aspectDecimal - (21.0 / 9.0)) < 0.025 -> "21:9"
                    Math.abs(aspectDecimal - (16.0 / 9.0)) < 0.025 -> "9:16"
                    else -> String.format(java.util.Locale.US, "%.2f:1", aspectDecimal)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ancho (px)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = customWidthText,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 5) {
                                            customWidthText = input
                                            customSavedMessage = null
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = Color(0xFF475569),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Alto (px)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = customHeightText,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 5) {
                                            customHeightText = input
                                            customSavedMessage = null
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = Color(0xFF475569),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val temp = customWidthText
                                        customWidthText = customHeightText
                                        customHeightText = temp
                                        customSavedMessage = null
                                    },
                                color = DarkCard,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Invertir",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1.4f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val native100 = nativePresets.firstOrNull()
                                        if (native100 != null) {
                                            customWidthText = native100.width.toString()
                                            customHeightText = native100.height.toString()
                                            customSavedMessage = null
                                        }
                                    },
                                color = DarkCard,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Copiar Pantalla",
                                        fontSize = 11.sp,
                                        color = accentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Medida Final Par:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "$evenW x $evenH px",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Proporción Calculada:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = calculatedRatio,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }

                                if (rawW % 2 != 0 || rawH % 2 != 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "⚠️ Se redondearon dimensiones impares a múltiplos de 2 para compatibilidad con codificadores de hardware.",
                                        fontSize = 10.sp,
                                        color = WarningAmber
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val customPreset = createCustomResolutionPreset(evenW, evenH)
                                onResolutionChange(customPreset)
                                customSavedMessage = "¡Resolución de ${evenW}x${evenH} aplicada exitosamente!"
                            },
                            enabled = !isRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guardar y Usar Esta Resolución",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (customSavedMessage != null) {
                            Text(
                                text = customSavedMessage ?: "",
                                color = SuccessGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (selectedResolution.isCustom) {
                    Text(
                        text = "RESOLUCIÓN PERSONALIZADA ACTIVA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, primaryColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedResolution.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${selectedResolution.width} x ${selectedResolution.height} px • ${selectedResolution.aspectRatioLabel}",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Activa",
                                tint = primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
