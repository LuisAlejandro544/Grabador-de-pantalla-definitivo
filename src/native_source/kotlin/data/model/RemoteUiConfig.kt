package com.velorec.recorder.data.model

/**
 * RemoteUiConfig.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Modela la estructura de datos para Server-Driven UI (Configuración Remota)
 * descargada de forma asíncrona desde GitHub Releases (app_config.json).
 * 
 * LÓGICA Y RESPONSABILIDADES:
 * 1. RemoteThemeConfig: Almacena colores de acento y primario en formato HEX,
 *    junto con la configuración del banner superior dinámico.
 * 2. RemoteAnnouncementConfig: Gestiona alertas modulares y avisos emergentes sin reinstalar el APK.
 * 3. RemoteUiConfig: Contenedor global con URL de descarga de versiones y bandera
 *    'isFetchedFromNetwork' para saber si los valores provienen de internet o del fallback local.
 */

data class RemoteThemeConfig(
    val accentColorHex: String = "#06B6D4",
    val primaryColorHex: String = "#6366F1",
    val bannerActive: Boolean = true,
    val bannerText: String = "⚡ VeloRec Studio: Grabación nativa NDK a 60/120 FPS sin lag.",
    val bannerType: String = "info"
)

data class RemoteAnnouncementConfig(
    val showDialog: Boolean = false,
    val title: String = "",
    val message: String = ""
)

data class RemoteUiConfig(
    val version: String = "1.0.0",
    val apkDownloadUrl: String = "https://github.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases",
    val theme: RemoteThemeConfig = RemoteThemeConfig(),
    val announcement: RemoteAnnouncementConfig = RemoteAnnouncementConfig(),
    val isFetchedFromNetwork: Boolean = false
)
