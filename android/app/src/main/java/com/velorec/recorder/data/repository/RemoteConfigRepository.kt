package com.velorec.recorder.data.repository

import com.velorec.recorder.data.model.RemoteAnnouncementConfig
import com.velorec.recorder.data.model.RemoteThemeConfig
import com.velorec.recorder.data.model.RemoteUiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * RemoteConfigRepository.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Repositorio de datos que gestiona la sincronización asíncrona no bloqueante
 * de la configuración remota (Server-Driven UI) desde GitHub Releases.
 * 
 * LÓGICA Y CORRUTINAS:
 * 1. Ejecución 100% en 'Dispatchers.IO': Ninguna llamada de red o parseo de JSON
 *    toca el hilo principal (Main Thread) de la interfaz de usuario.
 * 2. Estrategia de doble intento:
 *    a) Intenta consultar el último release en la API de GitHub para descargar el asset 'app_config.json'.
 *    b) Si no hay release aún o la API responde con límite de tasa (rate limit), recurre
 *       al archivo raw en la rama principal.
 * 3. Resiliencia Total Offline:
 *    Si el dispositivo no tiene conexión a internet, está en modo avión o la petición
 *    excede el timeout de 4000ms, captura la excepción silenciosamente y retorna un
 *    RemoteUiConfig por defecto con bandera 'isFetchedFromNetwork = false'.
 */
object RemoteConfigRepository {

    private const val REPO_RELEASES_URL =
        "https://api.github.com/repos/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases/latest"
    private const val RAW_FALLBACK_URL =
        "https://raw.githubusercontent.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/main/app_config.json"

    suspend fun fetchRemoteConfigSafe(): RemoteUiConfig {
        return withContext(Dispatchers.IO) {
            fun fetchString(urlStr: String): String? {
                return try {
                    val url = URL(urlStr)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.setRequestProperty("User-Agent", "VeloRecStudio-Android")
                    if (conn.responseCode == 200) {
                        BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            try {
                // 1. Intentar descargar directamente el asset del release de GitHub
                var jsonString: String? = null
                val releaseResponse = fetchString(REPO_RELEASES_URL)
                if (releaseResponse != null) {
                    val releaseObj = JSONObject(releaseResponse)
                    val assets = releaseObj.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.optString("name") == "app_config.json") {
                                val downloadUrl = asset.optString("browser_download_url")
                                jsonString = fetchString(downloadUrl)
                                break
                            }
                        }
                    }
                }

                // 2. Fallback a raw de GitHub si el release aún no tiene el asset adjunto
                if (jsonString == null) {
                    jsonString = fetchString(RAW_FALLBACK_URL)
                }

                if (jsonString != null) {
                    val root = JSONObject(jsonString)
                    val version = root.optString("version", "1.0.0")
                    val apkDownloadUrl = root.optString(
                        "apk_download_url",
                        "https://github.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases"
                    )

                    val themeObj = root.optJSONObject("theme")
                    val accentHex = themeObj?.optString("accent_color_hex", "#06B6D4") ?: "#06B6D4"
                    val primaryHex = themeObj?.optString("primary_color_hex", "#6366F1") ?: "#6366F1"
                    val bannerActive = themeObj?.optBoolean("banner_active", true) ?: true
                    val bannerText = themeObj?.optString(
                        "banner_text",
                        "⚡ VeloRec Studio: Grabación nativa NDK a 60/120 FPS sin lag."
                    ) ?: ""
                    val bannerType = themeObj?.optString("banner_type", "info") ?: "info"

                    val announceObj = root.optJSONObject("announcement")
                    val showDialog = announceObj?.optBoolean("show_dialog", false) ?: false
                    val announceTitle = announceObj?.optString("title", "") ?: ""
                    val announceMessage = announceObj?.optString("message", "") ?: ""

                    RemoteUiConfig(
                        version = version,
                        apkDownloadUrl = apkDownloadUrl,
                        theme = RemoteThemeConfig(
                            accentColorHex = accentHex,
                            primaryColorHex = primaryHex,
                            bannerActive = bannerActive,
                            bannerText = bannerText,
                            bannerType = bannerType
                        ),
                        announcement = RemoteAnnouncementConfig(
                            showDialog = showDialog,
                            title = announceTitle,
                            message = announceMessage
                        ),
                        isFetchedFromNetwork = true
                    )
                } else {
                    RemoteUiConfig() // Fallback seguro offline
                }
            } catch (e: Exception) {
                RemoteUiConfig() // Fallback seguro ante cualquier fallo de parseo
            }
        }
    }
}
