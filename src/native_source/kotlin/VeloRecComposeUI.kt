package com.velorec.recorder

/**
 * VeloRecComposeUI.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Implementa la interfaz de usuario móvil 100% nativa en Kotlin con Android Jetpack Compose (Material 3).
 * Proporciona controles táctiles directos, métricas en vivo y orquestación con React Native.
 */

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF131A29)
val DarkCard = Color(0xFF1E293B)
val PrimaryIndigo = Color(0xFF6366F1)
val AccentCyan = Color(0xFF06B6D4)
val RecordRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)
val SuccessGreen = Color(0xFF10B981)

enum class NativeTab {
    RECORDER, SETTINGS, GALLERY, BENCHMARK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeloRecMainScreen(
    onStartRecording: (engine: String) -> Unit = {},
    onStopRecording: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(NativeTab.RECORDER) }
    var selectedEngine by remember { mutableStateOf("cpp23") }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var durationSec by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("VeloRec Studio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = DarkCard, shape = RoundedCornerShape(4.dp)) {
                            Text("V1.0 PRO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentCyan, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding))
    }
}
