package com.velorec.recorder.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.MainActivity
import com.velorec.recorder.ScreenRecordService
import com.velorec.recorder.ui.theme.DarkBackground
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.DefaultAccentCyan
import com.velorec.recorder.ui.theme.PrimaryIndigo
import com.velorec.recorder.ui.theme.WarningAmber
import kotlinx.coroutines.launch

/**
 * DebugConsoleActivity.kt
 *
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Actividad independiente de diagnóstico, logs e inspección de errores (estilo LeakCanary):
 * 1. Acceso autónomo desde el cajón de aplicaciones de Android mediante el icono "VeloRec Inspector".
 * 2. Visualizador y buscador en tiempo real de Logcat filtrado por niveles (Errores, Warnings, Info).
 * 3. Monitor de salud de arquitectura y carga de bibliotecas nativas ('libvelorec_rust2024.so' y 'libvelorec_cpp23.so').
 * 4. Historial de excepciones y caídas (crashes) con stack trace detallado.
 * 5. Botón ergonómico de 1 clic para copiar todo el diagnóstico formateado en Markdown al portapapeles.
 */
class DebugConsoleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Asegurar que el gestor de depuración esté activo
        VeloRecDebugManager.init(applicationContext)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = PrimaryIndigo,
                    background = DarkBackground,
                    surface = DarkSurface
                )
            ) {
                DebugConsoleScreen(
                    onOpenMainActivity = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                        startActivity(intent)
                    },
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugConsoleScreen(
    onOpenMainActivity: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var logs by remember { mutableStateOf<List<LogcatEntry>>(emptyList()) }
    var isLoadingLogs by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf(LogcatLevel.VERBOSE) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    fun refreshLogs() {
        isLoadingLogs = true
        coroutineScope.launch {
            logs = VeloRecDebugManager.readFilteredLogcat(query = searchQuery, minLevel = selectedLevel)
            isLoadingLogs = false
        }
    }

    LaunchedEffect(selectedLevel, searchQuery) {
        refreshLogs()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF3B0764)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VeloRec Inspector",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Consola de Diagnóstico y Logcat",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    // Botón para ir al grabador principal
                    IconButton(onClick = onOpenMainActivity) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Abrir VeloRec",
                            tint = DefaultAccentCyan
                        )
                    }
                    // Botón refrescar (recarga registros y reintenta enlace de librerías nativas)
                    IconButton(onClick = {
                        ScreenRecordService.loadNativeLibraries()
                        refreshLogs()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar Logs",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de acción rápida destacada: Botón "Copiar Diagnóstico Completo"
            Surface(
                color = DarkCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Copiar reporte para el asistente",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Incluye dispositivo, estado de Rust, crashes y logs de error",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val report = VeloRecDebugManager.generateDiagnosticReport(context)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("VeloRec Diagnostic", report)
                                clipboard.setPrimaryClip(clip)
                                copiedToClipboard = true
                                Toast.makeText(context, "✅ Reporte copiado. ¡Pégalo directamente en el chat!", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copiedToClipboard) Color(0xFF10B981) else PrimaryIndigo
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (copiedToClipboard) "¡Copiado!" else "Copiar Todo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Pestañas de navegación
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = DefaultAccentCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Logcat en Vivo (${logs.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Salud & NDK", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        val crashCount = if (VeloRecDebugManager.latestCrash != null) " (1)" else " (0)"
                        Text("Crashes$crashCount", fontSize = 12.sp, color = if (VeloRecDebugManager.latestCrash != null) Color(0xFFEF4444) else Color.White)
                    }
                )
            }

            when (selectedTabIndex) {
                0 -> LogcatTab(
                    logs = logs,
                    isLoading = isLoadingLogs,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedLevel = selectedLevel,
                    onLevelSelected = { selectedLevel = it },
                    onClearLogs = {
                        coroutineScope.launch {
                            VeloRecDebugManager.clearLogcatBuffer()
                            refreshLogs()
                            Toast.makeText(context, "Logcat limpiado", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                1 -> SystemHealthTab()
                2 -> CrashesTab(
                    onClearCrashes = {
                        VeloRecDebugManager.clearCrashReports(context)
                        Toast.makeText(context, "Historial de crashes eliminado", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun LogcatTab(
    logs: List<LogcatEntry>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedLevel: LogcatLevel,
    onLevelSelected: (LogcatLevel) -> Unit,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Barra de búsqueda y filtros rápidos
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Buscar en logs (ej: Rust, Codec, Error)...", fontSize = 12.sp, color = Color(0xFF64748B)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.White)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = DefaultAccentCyan,
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Chips de filtro por nivel de severidad
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogLevelChip("Todos", selectedLevel == LogcatLevel.VERBOSE) { onLevelSelected(LogcatLevel.VERBOSE) }
            LogLevelChip("Info ℹ️", selectedLevel == LogcatLevel.INFO) { onLevelSelected(LogcatLevel.INFO) }
            LogLevelChip("Warnings ⚠️", selectedLevel == LogcatLevel.WARN, Color(0xFFF59E0B)) { onLevelSelected(LogcatLevel.WARN) }
            LogLevelChip("Errores 🔴", selectedLevel == LogcatLevel.ERROR, Color(0xFFEF4444)) { onLevelSelected(LogcatLevel.ERROR) }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onClearLogs, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar búfer", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Contenido de la lista de logs
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DefaultAccentCyan)
            }
        } else if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No hay logs que coincidan con el filtro", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { entry ->
                    LogcatEntryCard(entry)
                }
            }
        }
    }
}

@Composable
fun LogLevelChip(
    label: String,
    isSelected: Boolean,
    accentColor: Color = DefaultAccentCyan,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else DarkSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSelected) accentColor else Color(0xFF334155)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun LogcatEntryCard(entry: LogcatEntry) {
    val levelColor = when (entry.level) {
        LogcatLevel.VERBOSE -> Color(0xFF94A3B8)
        LogcatLevel.DEBUG -> Color(0xFF38BDF8)
        LogcatLevel.INFO -> Color(0xFF34D399)
        LogcatLevel.WARN -> Color(0xFFFBBF24)
        LogcatLevel.ERROR, LogcatLevel.FATAL -> Color(0xFFF87171)
    }

    val levelBg = when (entry.level) {
        LogcatLevel.ERROR, LogcatLevel.FATAL -> Color(0xFF450A0A)
        LogcatLevel.WARN -> Color(0xFF451A03)
        else -> DarkSurface
    }

    Surface(
        color = levelBg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, levelColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(levelColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = entry.tag,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                if (entry.timestamp.isNotBlank()) {
                    Text(
                        text = entry.timestamp,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.message,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SystemHealthTab() {
    val abis = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "Desconocida"
    val isRust = ScreenRecordService.isRustLoaded
    val isCpp = ScreenRecordService.isCppLoaded
    val rustError = ScreenRecordService.rustLoadError

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Diagnóstico de Hardware y Bibliotecas Nativas",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Estado en tiempo real de compilación y enlace dinámico NDK",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        item {
            HealthMetricCard(
                title = "Dispositivo Físico",
                status = "${Build.MANUFACTURER} ${Build.MODEL}",
                subtitle = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                isOk = true
            )
        }

        item {
            HealthMetricCard(
                title = "Arquitectura CPU (ABI)",
                status = abis,
                subtitle = if (abis.contains("arm64")) "Procesador de 64 bits moderno" else "Procesador móvil compatible",
                isOk = true
            )
        }

        item {
            HealthMetricCard(
                title = "Motor Central Rust (Edition 2024)",
                status = if (isRust) "libvelorec_rust2024.so ENLAZADA" else "NO DISPONIBLE",
                subtitle = if (isRust) "Zero-Copy, multiplexación en tiempo real y anti-LMK activo" else (rustError ?: "Falta la librería .so para $abis"),
                isOk = isRust
            )
        }

        item {
            HealthMetricCard(
                title = "Tubería Shaders GPU (C++23)",
                status = if (isCpp) "libvelorec_cpp23.so ENLAZADA" else "NO DISPONIBLE",
                subtitle = if (isCpp) "GLSL ES 3.20 y SPIR-V Vulkan listos" else "Aceleración por shaders no disponible",
                isOk = isCpp
            )
        }

        item {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "💡 Recomendación para Subway Surfers / Gaming",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Si vas a grabar partidas largas o juegos pesados, mantén el motor en Rust 2024. Su huella plana de RAM (<15 MB) evita que Android cierre el servicio a mitad de partida.",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

@Composable
fun HealthMetricCard(
    title: String,
    status: String,
    subtitle: String,
    isOk: Boolean
) {
    Surface(
        color = DarkCard,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isOk) Color(0xFF334155) else Color(0xFFEF4444).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isOk) Color(0xFF10B981) else Color(0xFFEF4444))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 11.sp, color = Color(0xFF94A3B8))
                Text(
                    text = status,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOk) Color.White else Color(0xFFF87171)
                )
                Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun CrashesTab(onClearCrashes: () -> Unit) {
    val crash = VeloRecDebugManager.latestCrash

    if (crash == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "¡Cero Crashes Registrados!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No se han detectado caídas no controladas en las sesiones recientes.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Último Crash Detectado",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171)
                    )
                    IconButton(onClick = onClearCrashes) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar crash", tint = Color(0xFF94A3B8))
                    }
                }
            }

            item {
                Surface(
                    color = Color(0xFF450A0A),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Fecha: ${crash.formattedDate}", fontSize = 11.sp, color = Color(0xFFFCA5A5))
                        Text(text = "Hilo: ${crash.threadName}", fontSize = 11.sp, color = Color(0xFFFCA5A5))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = crash.exceptionClass,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = crash.message,
                            fontSize = 12.sp,
                            color = Color(0xFFFECDD3)
                        )
                    }
                }
            }

            item {
                Text(text = "Stack Trace Completo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = crash.stackTrace,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
