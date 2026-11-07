package com.velorec.recorder.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Define la paleta de colores nativa de VeloRec Studio optimizada para pantallas OLED/AMOLED
 * en teléfonos móviles bajo el estándar Material 3 (Dark Theme de alto contraste).
 * 
 * LÓGICA Y RESPONSABILIDADES:
 * 1. DarkBackground / DarkSurface / DarkCard: Tonos oscuros profundos que minimizan el consumo
 *    de batería y la fatiga visual durante sesiones prolongadas de grabación.
 * 2. PrimaryIndigo y DefaultAccentCyan: Colores de acento principales que pueden ser sobreescritos
 *    dinámicamente mediante el Server-Driven UI (app_config.json).
 * 3. RecordRed, WarningAmber, SuccessGreen: Colores semánticos para el estado REC pulsante,
 *    advertencias del motor Rust y telemetría Zero-Copy 100%.
 * 4. parseColorSafe(): Función utilitaria a prueba de fallos para parsear strings hexadecimales
 *    remotos sin producir excepciones 'IllegalArgumentException'.
 */

val DarkBackground = Color(0xFF090D16)
val DarkSurface = Color(0xFF131A29)
val DarkCard = Color(0xFF1E293B)
val PrimaryIndigo = Color(0xFF6366F1)
val DefaultAccentCyan = Color(0xFF06B6D4)
val RecordRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)
val SuccessGreen = Color(0xFF10B981)

fun parseColorSafe(hexStr: String, defaultColor: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexStr))
    } catch (e: Exception) {
        defaultColor
    }
}
