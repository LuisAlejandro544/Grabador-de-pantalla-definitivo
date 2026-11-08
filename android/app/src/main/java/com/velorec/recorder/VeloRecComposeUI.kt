package com.velorec.recorder

/**
 * VeloRecComposeUI.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Punto de entrada y fachada pública modular para la interfaz móvil nativa de Jetpack Compose (Material 3).
 * 
 * ARQUITECTURA MODULAR:
 * La lógica antes monolítica ha sido descompuesta en módulos de responsabilidad única:
 * 1. Modelos de datos:
 *    - com.velorec.recorder.data.model.NativeRecordingConfig
 *    - com.velorec.recorder.data.model.ResolutionPreset (y listas de presets)
 *    - com.velorec.recorder.data.model.RemoteUiConfig (Server-Driven UI)
 * 2. Repositorio de Red Asíncrono (Dispatchers.IO):
 *    - com.velorec.recorder.data.repository.RemoteConfigRepository
 * 3. Tema y Colores Material 3:
 *    - com.velorec.recorder.ui.theme.DarkBackground, PrimaryIndigo, etc.
 * 4. Componentes Reutilizables:
 *    - com.velorec.recorder.ui.components.MetricBadge
 *    - com.velorec.recorder.ui.components.RemoteAnnouncementDialog
 * 5. Pantallas Desacopladas:
 *    - com.velorec.recorder.ui.screens.RecorderScreen (NativeRecorderViewport)
 *    - com.velorec.recorder.ui.screens.SettingsScreen (NativeSettingsSection)
 *    - com.velorec.recorder.ui.screens.GalleryScreen (NativeGallerySection)
 *    - com.velorec.recorder.ui.screens.BenchmarkScreen (NativeBenchmarkSection)
 * 6. Orquestador Visual:
 *    - com.velorec.recorder.ui.VeloRecMainScreen
 */

// Re-exportamos los tipos y funciones públicas para preservar retrocompatibilidad
typealias NativeRecordingConfig = com.velorec.recorder.data.model.NativeRecordingConfig
typealias ResolutionPreset = com.velorec.recorder.data.model.ResolutionPreset
typealias RemoteUiConfig = com.velorec.recorder.data.model.RemoteUiConfig
typealias RemoteThemeConfig = com.velorec.recorder.data.model.RemoteThemeConfig
typealias RemoteAnnouncementConfig = com.velorec.recorder.data.model.RemoteAnnouncementConfig
typealias NativeTab = com.velorec.recorder.data.model.NativeTab

typealias AudioSourcePreset = com.velorec.recorder.data.model.AudioSourcePreset

val RESOLUTION_PRESETS = com.velorec.recorder.data.model.RESOLUTION_PRESETS
val BITRATE_PRESETS = com.velorec.recorder.data.model.BITRATE_PRESETS
val FPS_PRESETS = com.velorec.recorder.data.model.FPS_PRESETS
val AUDIO_SOURCE_PRESETS = com.velorec.recorder.data.model.AUDIO_SOURCE_PRESETS

val DarkBackground = com.velorec.recorder.ui.theme.DarkBackground
val DarkSurface = com.velorec.recorder.ui.theme.DarkSurface
val DarkCard = com.velorec.recorder.ui.theme.DarkCard
val PrimaryIndigo = com.velorec.recorder.ui.theme.PrimaryIndigo
val DefaultAccentCyan = com.velorec.recorder.ui.theme.DefaultAccentCyan
val RecordRed = com.velorec.recorder.ui.theme.RecordRed
val WarningAmber = com.velorec.recorder.ui.theme.WarningAmber
val SuccessGreen = com.velorec.recorder.ui.theme.SuccessGreen

fun parseColorSafe(hexStr: String, defaultColor: androidx.compose.ui.graphics.Color) =
    com.velorec.recorder.ui.theme.parseColorSafe(hexStr, defaultColor)

suspend fun fetchRemoteConfigSafe() =
    com.velorec.recorder.data.repository.RemoteConfigRepository.fetchRemoteConfigSafe()
