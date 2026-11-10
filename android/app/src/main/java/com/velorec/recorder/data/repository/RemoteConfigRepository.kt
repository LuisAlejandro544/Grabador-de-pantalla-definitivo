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

    const val CURRENT_APP_VERSION = "1.0.0-E"
    private const val REPO_RELEASES_URL =
        "https://api.github.com/repos/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases/latest"
    private const val RAW_FALLBACK_URL =
        "https://raw.githubusercontent.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/main/app_config.json"

    /**
     * Compara jerárquicamente dos cadenas de versión semántica (ej: 'v1.0.1-E' vs '1.0.0-E').
     * Retorna true si la versión remota es mayor a la instalada.
     */
    fun isNewerVersion(remoteTag: String, currentVersion: String = CURRENT_APP_VERSION): Boolean {
        fun parseParts(v: String): Triple<Int, Int, Int> {
            val clean = v.trim().removePrefix("v").removePrefix("V").substringBefore("-")
            val parts = clean.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            return Triple(major, minor, patch)
        }
        val (rMaj, rMin, rPat) = parseParts(remoteTag)
        val (cMaj, cMin, cPat) = parseParts(currentVersion)
        if (rMaj > cMaj) return true
        if (rMaj < cMaj) return false
        if (rMin > cMin) return true
        if (rMin < cMin) return false
        return rPat > cPat
    }

    /**
     * Consulta directamente la API de GitHub Releases para comprobar si existe una nueva versión.
     * Retorna el modelo completo AppUpdateInfo con notas del release y URL directa del APK.
     */
    suspend fun checkForAppUpdate(currentVersion: String = CURRENT_APP_VERSION): com.velorec.recorder.data.model.AppUpdateInfo {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(REPO_RELEASES_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "VeloRecStudio-Android")
                if (conn.responseCode == 200) {
                    val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val releaseObj = JSONObject(body)
                    val tagName = releaseObj.optString("tag_name", "")
                    val releaseName = releaseObj.optString("name", "VeloRec $tagName")
                    val releaseNotes = releaseObj.optString("body", "Actualización con mejoras de rendimiento y estabilidad.")
                    val publishedAt = releaseObj.optString("published_at", "")
                    val htmlUrl = releaseObj.optString("html_url", "https://github.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases/latest")

                    var directApkUrl: String? = null
                    val assets = releaseObj.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk", ignoreCase = true)) {
                                directApkUrl = asset.optString("browser_download_url")
                                // Preferir APK universal si hay múltiples
                                if (assetName.contains("Universal", ignoreCase = true)) {
                                    break
                                }
                            }
                        }
                    }

                    val finalDownloadUrl = directApkUrl ?: htmlUrl
                    val hasUpdate = tagName.isNotBlank() && isNewerVersion(tagName, currentVersion)
                    val isStable = tagName.endsWith("-E", ignoreCase = true)

                    com.velorec.recorder.data.model.AppUpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersionTag = tagName.ifBlank { currentVersion },
                        currentVersionTag = currentVersion,
                        releaseTitle = releaseName,
                        releaseNotes = releaseNotes,
                        apkDownloadUrl = finalDownloadUrl,
                        isStable = isStable,
                        publishedAt = publishedAt,
                        checkedSuccessfully = true
                    )
                } else {
                    com.velorec.recorder.data.model.AppUpdateInfo(checkedSuccessfully = false)
                }
            } catch (e: Exception) {
                com.velorec.recorder.data.model.AppUpdateInfo(checkedSuccessfully = false)
            }
        }
    }

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
                // 1. Intentar descargar directamente el asset del release de GitHub y consultar actualización
                var jsonString: String? = null
                var detectedUpdateInfo = com.velorec.recorder.data.model.AppUpdateInfo()

                val releaseResponse = fetchString(REPO_RELEASES_URL)
                if (releaseResponse != null) {
                    val releaseObj = JSONObject(releaseResponse)
                    val tagName = releaseObj.optString("tag_name", "")
                    val releaseTitle = releaseObj.optString("name", "VeloRec $tagName")
                    val releaseNotes = releaseObj.optString("body", "")
                    val publishedAt = releaseObj.optString("published_at", "")
                    val htmlUrl = releaseObj.optString("html_url", "https://github.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases/latest")

                    var directApkUrl: String? = null
                    val assets = releaseObj.optJSONArray("assets")
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val aName = asset.optString("name", "")
                            if (aName == "app_config.json") {
                                val downloadUrl = asset.optString("browser_download_url")
                                jsonString = fetchString(downloadUrl)
                            }
                            if (aName.endsWith(".apk", ignoreCase = true) && directApkUrl == null) {
                                directApkUrl = asset.optString("browser_download_url")
                            }
                        }
                    }

                    if (tagName.isNotBlank()) {
                        val hasUpdate = isNewerVersion(tagName, CURRENT_APP_VERSION)
                        detectedUpdateInfo = com.velorec.recorder.data.model.AppUpdateInfo(
                            hasUpdate = hasUpdate,
                            latestVersionTag = tagName,
                            currentVersionTag = CURRENT_APP_VERSION,
                            releaseTitle = releaseTitle,
                            releaseNotes = releaseNotes,
                            apkDownloadUrl = directApkUrl ?: htmlUrl,
                            isStable = tagName.endsWith("-E", ignoreCase = true),
                            publishedAt = publishedAt,
                            checkedSuccessfully = true
                        )
                    }
                }

                // 2. Fallback a raw de GitHub si el release aún no tiene el asset adjunto
                if (jsonString == null) {
                    jsonString = fetchString(RAW_FALLBACK_URL)
                }

                if (jsonString != null) {
                    val root = JSONObject(jsonString)
                    val version = root.optString("version", CURRENT_APP_VERSION)
                    val apkDownloadUrl = root.optString(
                        "apk_download_url",
                        "https://github.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases/latest"
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
                        isFetchedFromNetwork = true,
                        updateInfo = detectedUpdateInfo
                    )
                } else {
                    RemoteUiConfig(updateInfo = detectedUpdateInfo) // Fallback seguro offline con info de update si se detectó
                }
            } catch (e: Exception) {
                RemoteUiConfig() // Fallback seguro ante cualquier fallo de parseo
            }
        }
    }
}

