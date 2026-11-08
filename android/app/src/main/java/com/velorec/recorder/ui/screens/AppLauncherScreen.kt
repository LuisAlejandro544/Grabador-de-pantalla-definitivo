package com.velorec.recorder.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.data.model.InstalledAppItem
import com.velorec.recorder.data.model.NativeRecordingConfig
import com.velorec.recorder.data.model.ResolutionPreset
import com.velorec.recorder.ui.theme.DarkBackground
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.RecordRed
import com.velorec.recorder.ui.theme.SuccessGreen
import com.velorec.recorder.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AppLauncherScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Sección táctil que lista todos los juegos y aplicaciones descargadas en el smartphone.
 * Permite con 1 solo toque abrir la aplicación o juego seleccionado e iniciar la grabación
 * de pantalla automáticamente con la configuración preestablecida.
 * 
 * CARACTERÍSTICAS Y OPTIMIZACIONES:
 * 1. Consulta asíncrona en 'Dispatchers.IO' mediante PackageManager para evitar bloqueos del hilo UI.
 * 2. Detección automática de videojuegos mediante ApplicationInfo.CATEGORY_GAME y flags del sistema.
 * 3. Filtrado dinámico por texto (Buscador) y pestañas rápidas (Todos, Juegos, Apps).
 * 4. Conversión segura de iconos nativos (Drawable a ImageBitmap) con renderizado fluido.
 * 5. Orquestación directa con MainActivity para lanzar la app e iniciar ScreenRecordService.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeAppLauncherSection(
    selectedEngine: String,
    selectedResolution: ResolutionPreset,
    selectedBitrateMbps: Int,
    selectedFps: Int,
    selectedCountdownSec: Int,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onLaunchAppAndRecord: (packageName: String, config: NativeRecordingConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Todos") } // "Todos", "Juegos", "Apps"

    // Carga asíncrona de aplicaciones y juegos en segundo plano (Dispatchers.IO)
    LaunchedEffect(Unit) {
        isLoading = true
        val apps = withContext(Dispatchers.IO) {
            queryInstalledApplications(context)
        }
        installedApps = apps
        isLoading = false
    }

    // Filtrar lista de apps según el buscador y categoría
    val filteredApps = remember(installedApps, searchQuery, selectedCategoryFilter) {
        installedApps.filter { app ->
            val matchesQuery = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategoryFilter) {
                "Juegos" -> app.isGame
                "Apps" -> !app.isGame
                else -> true
            }

            matchesQuery && matchesCategory
        }
    }

    val currentConfig = remember(selectedEngine, selectedResolution, selectedBitrateMbps, selectedFps, selectedCountdownSec) {
        NativeRecordingConfig(
            engine = selectedEngine,
            width = selectedResolution.width,
            height = selectedResolution.height,
            fps = selectedFps,
            bitrateBps = selectedBitrateMbps * 1_000_000,
            countdownSec = selectedCountdownSec
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Encabezado del Lanzador Rápido
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(primaryColor, accentColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "Lanzador de Juegos",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Lanzador Rápido",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = primaryColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, primaryColor)
                        ) {
                            Text(
                                text = "1-CLICK REC",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Abre cualquier juego o app y comienza a grabar al instante",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Banner informativo de la configuración activa
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Configuración activa:",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Text(
                    text = "${selectedResolution.label} • ${selectedFps}fps • ${selectedBitrateMbps}M • ${if (selectedEngine == "rust2024") "Rust" else "C++23"}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Campo de búsqueda de aplicaciones
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar juego o app...", fontSize = 13.sp, color = Color(0xFF64748B)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = DarkCard,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Chips de categoría: Todos, Juegos, Apps
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Todos", "Juegos", "Apps").forEach { category ->
                val isSelected = selectedCategoryFilter == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryFilter = category },
                    label = {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryColor.copy(alpha = 0.25f),
                        selectedLabelColor = accentColor,
                        containerColor = DarkCard,
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) accentColor else Color(0xFF334155)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lista de Aplicaciones Instaladas
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Detectando juegos y apps instaladas...",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No se encontraron aplicaciones",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Prueba con otro término de búsqueda o categoría",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppLauncherCard(
                        app = app,
                        isRecording = isRecording,
                        accentColor = accentColor,
                        primaryColor = primaryColor,
                        onLaunch = {
                            onLaunchAppAndRecord(app.packageName, currentConfig)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual para cada juego o aplicación instalada.
 */
@Composable
private fun AppLauncherCard(
    app: InstalledAppItem,
    isRecording: Boolean,
    accentColor: Color,
    primaryColor: Color,
    onLaunch: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (app.isGame) WarningAmber.copy(alpha = 0.35f) else Color(0xFF334155)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de la App / Juego
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .border(
                        1.dp,
                        if (app.isGame) WarningAmber.copy(alpha = 0.5f) else Color(0xFF475569),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = remember(app.icon) {
                    app.icon?.let { drawableToBitmapSafe(it) }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        imageVector = if (app.isGame) Icons.Default.SportsEsports else Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = if (app.isGame) WarningAmber else accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nombre y detalles del paquete
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (app.isGame) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF451A03),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "JUEGO",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = app.packageName,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón de Acción Táctil: Abrir y Grabar
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) DarkCard else RecordRed
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (isRecording) Color(0xFF64748B) else RecordRed.copy(alpha = 0.8f)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.PlayArrow else Icons.Default.Videocam,
                        contentDescription = "Abrir y Grabar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRecording) "Abrir" else "REC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Consulta las aplicaciones instaladas con intención de lanzamiento (LAUNCHER).
 * Clasifica si pertenecen a la categoría de videojuegos mediante las APIs de Android.
 */
private fun queryInstalledApplications(context: Context): List<InstalledAppItem> {
    val pm = context.packageManager
    val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolveInfos = pm.queryIntentActivities(launchIntent, 0)
    val appList = mutableListOf<InstalledAppItem>()
    val ourPackage = context.packageName

    for (resolve in resolveInfos) {
        val pkg = resolve.activityInfo.packageName
        if (pkg == ourPackage) continue // Excluir a la propia VeloRec de la lista

        val appName = resolve.loadLabel(pm)?.toString() ?: pkg
        val icon = resolve.loadIcon(pm)

        val isGame = try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            val isCategoryGame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appInfo.category == ApplicationInfo.CATEGORY_GAME
            } else {
                false
            }
            isCategoryGame || (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0 ||
                    pkg.contains("game", ignoreCase = true) ||
                    appName.contains("game", ignoreCase = true)
        } catch (e: Exception) {
            false
        }

        appList.add(
            InstalledAppItem(
                packageName = pkg,
                appName = appName,
                isGame = isGame,
                category = if (isGame) "Juego" else "App",
                icon = icon
            )
        )
    }

    // Si la lista está vacía (como en ciertos emuladores o entornos aislados), incluir accesos directos estándar
    if (appList.isEmpty()) {
        val fallbackPresets = listOf(
            InstalledAppItem("com.dts.freefireth", "Free Fire MAX", isGame = true, category = "Juego"),
            InstalledAppItem("com.tencent.ig", "PUBG Mobile", isGame = true, category = "Juego"),
            InstalledAppItem("com.activision.callofduty.shooter", "Call of Duty: Mobile", isGame = true, category = "Juego"),
            InstalledAppItem("com.supercell.brawlstars", "Brawl Stars", isGame = true, category = "Juego"),
            InstalledAppItem("com.roblox.client", "Roblox", isGame = true, category = "Juego"),
            InstalledAppItem("com.miHoYo.GenshinImpact", "Genshin Impact", isGame = true, category = "Juego"),
            InstalledAppItem("com.google.android.youtube", "YouTube", isGame = false, category = "App"),
            InstalledAppItem("org.videolan.vlc", "VLC for Android", isGame = false, category = "App")
        )
        return fallbackPresets
    }

    // Ordenar priorizando juegos primero y luego alfabéticamente
    return appList.sortedWith(
        compareByDescending<InstalledAppItem> { it.isGame }
            .thenBy { it.appName.lowercase() }
    )
}

/**
 * Convierte de forma segura cualquier Drawable a Bitmap sin fugas de memoria.
 */
private fun drawableToBitmapSafe(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    return try {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}
