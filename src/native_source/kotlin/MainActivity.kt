package com.velorec.recorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * MainActivity.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Punto de entrada principal de VeloRec Studio en Android.
 * Integra la interfaz nativa en Jetpack Compose (Material 3) y la orquestación con React Native.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6366F1),
                    background = Color(0xFF090D16),
                    surface = Color(0xFF131A29),
                    onPrimary = Color.White,
                    onBackground = Color(0xFFF1F5F9),
                    onSurface = Color(0xFFF1F5F9)
                )
            ) {
                VeloRecMainScreen(
                    onStartRecording = { /* Inicia ScreenRecordService */ },
                    onStopRecording = { /* Detiene ScreenRecordService */ }
                )
            }
        }
    }
}
