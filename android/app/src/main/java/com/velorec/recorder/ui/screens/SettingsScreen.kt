package com.velorec.recorder.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.velorec.recorder.data.model.RemoteUiConfig
import com.velorec.recorder.data.model.ResolutionPreset
import com.velorec.recorder.data.model.getAdaptivePresetsForDevice
import com.velorec.recorder.ui.screens.settings.AudioSettingsSubScreen
import com.velorec.recorder.ui.screens.settings.BitrateSettingsSubScreen
import com.velorec.recorder.ui.screens.settings.CodecSettingsSubScreen
import com.velorec.recorder.ui.screens.settings.CountdownSettingsSubScreen
import com.velorec.recorder.ui.screens.settings.EngineSettingsSubScreen
import com.velorec.recorder.ui.screens.settings.FpsSettingsSubScreen
import com.velorec.recorder.ui.screens.settings.ResolutionSettingsSubScreen
import com.velorec.recorder.ui.screens.settings.SettingsMainMenu
import com.velorec.recorder.ui.screens.settings.SettingsSubScreen

// Re-exportamos para retrocompatibilidad total
typealias SettingsSubScreen = com.velorec.recorder.ui.screens.settings.SettingsSubScreen

/**
 * SettingsScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Orquestador principal de la sección de Ajustes Técnicos de VeloRec Studio.
 * 
 * ARQUITECTURA MODULAR DE NAVEGACIÓN PROFESIONAL:
 * En lugar de amontonar miles de líneas de código en un solo archivo monolítico,
 * este módulo delega cada categoría técnica a su propia subpantalla dedicada e independiente:
 * 1. 'SettingsMainMenu': Menú principal con tarjetas y botones de acceso rápido.
 * 2. 'ResolutionSettingsSubScreen': Resoluciones proporcionales nativas, estándar y personalizada con paridad.
 * 3. 'BitrateSettingsSubScreen': Control continuo de 1 a 60 Mbps, nivel térmico y calculadora de almacenamiento.
 * 4. 'FpsSettingsSubScreen': Tasas de cuadro (24, 30, 60, 90 y 120 FPS).
 * 5. 'CodecSettingsSubScreen': Códecs por hardware (H.264, HEVC, AV1).
 * 6. 'AudioSettingsSubScreen': Fuentes de audio (Mezcla estéreo, Micrófono, Audio interno, Silencio).
 * 7. 'CountdownSettingsSubScreen': Temporizador de inicio previo (0s, 3s, 5s, 10s).
 * 8. 'EngineSettingsSubScreen': Motor nativo (Rust 2024 Zero-Copy vs C++23 NDK).
 * 
 * Soporta 'BackHandler' nativo de Android (físico o por gestos) para regresar suavemente al menú principal.
 */
@Composable
fun NativeSettingsSection(
    selectedEngine: String,
    selectedResolution: ResolutionPreset,
    selectedBitrateMbps: Int,
    selectedFps: Int,
    selectedCodec: String,
    selectedAudio: String,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    remoteConfig: RemoteUiConfig,
    availableResolutions: List<ResolutionPreset>? = null,
    selectedCountdownSec: Int = 3,
    onEngineChange: (String) -> Unit,
    onResolutionChange: (ResolutionPreset) -> Unit,
    onBitrateChange: (Int) -> Unit,
    onFpsChange: (Int) -> Unit,
    onCodecChange: (String) -> Unit,
    onAudioChange: (String) -> Unit,
    onCountdownChange: (Int) -> Unit = {},
    onCheckUpdates: () -> Unit = {}
) {
    val context = LocalContext.current
    val resolutions = availableResolutions ?: remember(context) { getAdaptivePresetsForDevice(context) }
    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // Soporte para botón atrás del sistema Android (físico o por gestos)
    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

    when (currentSubScreen) {
        SettingsSubScreen.MAIN -> {
            SettingsMainMenu(
                selectedEngine = selectedEngine,
                selectedResolution = selectedResolution,
                selectedBitrateMbps = selectedBitrateMbps,
                selectedFps = selectedFps,
                selectedCodec = selectedCodec,
                selectedAudio = selectedAudio,
                selectedCountdownSec = selectedCountdownSec,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                remoteConfig = remoteConfig,
                onNavigateTo = { currentSubScreen = it },
                onCheckUpdates = onCheckUpdates
            )
        }
        SettingsSubScreen.RESOLUTION -> {
            ResolutionSettingsSubScreen(
                resolutions = resolutions,
                selectedResolution = selectedResolution,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                onResolutionChange = onResolutionChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.BITRATE -> {
            BitrateSettingsSubScreen(
                selectedBitrateMbps = selectedBitrateMbps,
                isRecording = isRecording,
                accentColor = accentColor,
                onBitrateChange = onBitrateChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.FPS -> {
            FpsSettingsSubScreen(
                selectedFps = selectedFps,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                onFpsChange = onFpsChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.CODEC -> {
            CodecSettingsSubScreen(
                selectedCodec = selectedCodec,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                onCodecChange = onCodecChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.AUDIO -> {
            AudioSettingsSubScreen(
                selectedAudio = selectedAudio,
                isRecording = isRecording,
                accentColor = accentColor,
                onAudioChange = onAudioChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.COUNTDOWN -> {
            CountdownSettingsSubScreen(
                selectedCountdownSec = selectedCountdownSec,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                onCountdownChange = onCountdownChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.ENGINE -> {
            EngineSettingsSubScreen(
                selectedEngine = selectedEngine,
                isRecording = isRecording,
                accentColor = accentColor,
                primaryColor = primaryColor,
                onEngineChange = onEngineChange,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
    }
}
