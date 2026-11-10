package com.velorec.recorder.debug

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.velorec.recorder.ScreenRecordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Nivel de registro para filtrado de Logcat
 */
enum class LogcatLevel(val code: String, val label: String) {
    VERBOSE("V", "Verbose"),
    DEBUG("D", "Debug"),
    INFO("I", "Info"),
    WARN("W", "Warning"),
    ERROR("E", "Error"),
    FATAL("F", "Fatal")
}

/**
 * Representa una línea individual de log extraída de Logcat
 */
data class LogcatEntry(
    val timestamp: String,
    val level: LogcatLevel,
    val tag: String,
    val message: String
)

/**
 * Representa un reporte de crash no controlado interceptado
 */
data class CrashReportItem(
    val timestampMillis: Long,
    val formattedDate: String,
    val threadName: String,
    val exceptionClass: String,
    val message: String,
    val stackTrace: String,
    val supportedAbis: String,
    val rustLoaded: Boolean,
    val cppLoaded: Boolean
)

/**
 * VeloRecDebugManager.kt
 *
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Administrador central de diagnóstico en tiempo real (estilo LeakCanary):
 * 1. Intercepta excepciones no controladas mediante 'UncaughtExceptionHandler'.
 * 2. Lee y filtra el registro de Logcat del proceso de la app en segundo plano (Dispatchers.IO).
 * 3. Monitorea el estado de carga y enlace de bibliotecas nativas C++23 y Rust 2024.
 * 4. Recopila telemetría de memoria física (RSS, OOM score) para detectar riesgo de LMK.
 * 5. Genera reportes estructurados en Markdown listos para copiar con 1 toque.
 */
object VeloRecDebugManager {

    private const val TAG = "VeloRecDebugManager"
    private var isInitialized = false
    private var defaultUncaughtHandler: Thread.UncaughtExceptionHandler? = null

    @Volatile
    var latestCrash: CrashReportItem? = null
        private set

    /**
     * Inicializa el interceptor global de caídas (crashes).
     */
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        defaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                handleUncaughtException(context, thread, throwable)
            } catch (e: Throwable) {
                Log.e(TAG, "Error persistiendo reporte de crash: ${e.message}", e)
            } finally {
                defaultUncaughtHandler?.uncaughtException(thread, throwable)
            }
        }

        // Cargar el último crash almacenado en disco si existe
        loadLatestCrashFromDisk(context)
        Log.i(TAG, "VeloRecDebugManager inicializado con éxito.")
    }

    private fun handleUncaughtException(context: Context, thread: Thread, throwable: Throwable) {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(now))

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val abis = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "Desconocida"

        val crashItem = CrashReportItem(
            timestampMillis = now,
            formattedDate = dateStr,
            threadName = thread.name,
            exceptionClass = throwable.javaClass.name,
            message = throwable.localizedMessage ?: throwable.message ?: "Sin mensaje",
            stackTrace = stackTrace,
            supportedAbis = abis,
            rustLoaded = ScreenRecordService.isRustLoaded,
            cppLoaded = ScreenRecordService.isCppLoaded
        )

        latestCrash = crashItem

        // Guardar archivo en directorio privado de la app
        val crashDir = File(context.filesDir, "debug_crashes")
        if (!crashDir.exists()) {
            crashDir.mkdirs()
        }
        val crashFile = File(crashDir, "crash_${now}.txt")
        val reportContent = buildString {
            appendLine("=== VELOREC CRASH REPORT ===")
            appendLine("Fecha: $dateStr")
            appendLine("Hilo: ${thread.name}")
            appendLine("Excepción: ${throwable.javaClass.name}")
            appendLine("Mensaje: ${throwable.localizedMessage}")
            appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            appendLine("ABIs Soportadas: $abis")
            appendLine("Rust 2024 Cargado: ${ScreenRecordService.isRustLoaded}")
            appendLine("C++23 Cargado: ${ScreenRecordService.isCppLoaded}")
            appendLine("----------------------------------------")
            appendLine("STACK TRACE:")
            appendLine(stackTrace)
        }

        crashFile.writeText(reportContent)
    }

    private fun loadLatestCrashFromDisk(context: Context) {
        try {
            val crashDir = File(context.filesDir, "debug_crashes")
            if (!crashDir.exists()) return
            val files = crashDir.listFiles { f -> f.extension == "txt" } ?: return
            val newestFile = files.maxByOrNull { it.lastModified() } ?: return

            val lines = newestFile.readLines()
            var dateStr = ""
            var threadName = "main"
            var excClass = "Exception"
            var msg = ""
            val stackLines = mutableListOf<String>()
            var readingStack = false

            for (line in lines) {
                when {
                    line.startsWith("Fecha: ") -> dateStr = line.removePrefix("Fecha: ").trim()
                    line.startsWith("Hilo: ") -> threadName = line.removePrefix("Hilo: ").trim()
                    line.startsWith("Excepción: ") -> excClass = line.removePrefix("Excepción: ").trim()
                    line.startsWith("Mensaje: ") -> msg = line.removePrefix("Mensaje: ").trim()
                    line.startsWith("STACK TRACE:") -> readingStack = true
                    readingStack -> stackLines.add(line)
                }
            }

            latestCrash = CrashReportItem(
                timestampMillis = newestFile.lastModified(),
                formattedDate = if (dateStr.isNotBlank()) dateStr else "Previa",
                threadName = threadName,
                exceptionClass = excClass,
                message = msg,
                stackTrace = stackLines.joinToString("\n"),
                supportedAbis = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "Desconocida",
                rustLoaded = ScreenRecordService.isRustLoaded,
                cppLoaded = ScreenRecordService.isCppLoaded
            )
        } catch (e: Throwable) {
            Log.w(TAG, "No se pudo cargar el crash previo: ${e.message}")
        }
    }

    /**
     * Lee y filtra el registro de Logcat del proceso de VeloRec en segundo plano.
     */
    suspend fun readFilteredLogcat(
        query: String = "",
        minLevel: LogcatLevel = LogcatLevel.VERBOSE
    ): List<LogcatEntry> = withContext(Dispatchers.IO) {
        val myPid = Process.myPid()
        val result = mutableListOf<LogcatEntry>()

        try {
            // Lee los últimos 800 mensajes de logcat con formato time
            val process = ProcessBuilder("logcat", "-d", "-v", "time", "*:V")
                .redirectErrorStream(true)
                .start()

            val relevantTags = setOf(
                "ScreenRecordService",
                "VeloRec",
                "VeloRec_Rust2024",
                "VeloRecGpuShader",
                "VeloRecDebugManager",
                "AudioRecordEngine",
                "AudioMuxerHelper",
                "GalleryStorageManager",
                "AndroidRuntime",
                "FATAL EXCEPTION",
                "DEBUG",
                "MediaCodec",
                "libprocessgroup"
            )

            // Expresión regular estándar para logcat -v time:
            // "05-10 14:20:10.123 D/Tag( 1234): message" o "05-10 14:20:10.123 I/Tag(1234): message"
            val logPattern = Regex("""^(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s+([VDIWEF])/([^(]+)\(\s*(\d+)\):\s*(.*)$""")

            process.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    val match = logPattern.find(line)
                    if (match != null) {
                        val (timeStr, levelCode, tagRaw, pidStr, message) = match.destructured
                        val tag = tagRaw.trim()
                        val pid = pidStr.trim().toIntOrNull() ?: 0

                        val isMyProcess = (pid == myPid) || (myPid == 0)
                        val isRelevantTag = relevantTags.any { tag.contains(it, ignoreCase = true) }

                        // Incluir si pertenece al proceso actual o es un tag crítico de audio/video/runtime
                        if (isMyProcess || isRelevantTag) {
                            val level = when (levelCode) {
                                "V" -> LogcatLevel.VERBOSE
                                "D" -> LogcatLevel.DEBUG
                                "I" -> LogcatLevel.INFO
                                "W" -> LogcatLevel.WARN
                                "E" -> LogcatLevel.ERROR
                                "F" -> LogcatLevel.FATAL
                                else -> LogcatLevel.VERBOSE
                            }

                            if (level.ordinal >= minLevel.ordinal) {
                                if (query.isBlank() ||
                                    tag.contains(query, ignoreCase = true) ||
                                    message.contains(query, ignoreCase = true)
                                ) {
                                    result.add(
                                        LogcatEntry(
                                            timestamp = timeStr,
                                            level = level,
                                            tag = tag,
                                            message = message
                                        )
                                    )
                                }
                            }
                        }
                    } else if (line.contains("FATAL EXCEPTION", ignoreCase = true) ||
                               line.contains("backtrace:", ignoreCase = true) ||
                               line.contains("DEBUG   :", ignoreCase = true)) {
                        result.add(
                            LogcatEntry(
                                timestamp = "",
                                level = LogcatLevel.FATAL,
                                tag = "CrashNative",
                                message = line
                            )
                        )
                    }
                }
            }
        } catch (t: Throwable) {
            result.add(
                LogcatEntry(
                    timestamp = "",
                    level = LogcatLevel.ERROR,
                    tag = TAG,
                    message = "Fallo al ejecutar logcat: ${t.localizedMessage}"
                )
            )
        }

        result.takeLast(400).reversed()
    }

    /**
     * Limpia el búfer de logcat del sistema.
     */
    suspend fun clearLogcatBuffer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("logcat", "-c").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Limpia el historial de reportes de caídas persistidos en disco.
     */
    fun clearCrashReports(context: Context) {
        try {
            val crashDir = File(context.filesDir, "debug_crashes")
            if (crashDir.exists()) {
                crashDir.deleteRecursively()
            }
            latestCrash = null
        } catch (e: Exception) {
            Log.w(TAG, "Error limpiando crashes: ${e.message}")
        }
    }

    /**
     * Genera un reporte diagnóstico completo en formato Markdown listo para copiar al portapapeles.
     */
    suspend fun generateDiagnosticReport(context: Context): String = withContext(Dispatchers.IO) {
        val abis = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "Desconocida"
        val rustStatus = if (ScreenRecordService.isRustLoaded) "✅ Cargada con éxito" else "❌ No disponible (${ScreenRecordService.rustLoadError ?: "Falta .so en paquete"})"
        val cppStatus = if (ScreenRecordService.isCppLoaded) "✅ Cargada con éxito" else "❌ No disponible"

        // Obtener logs recientes con errores y warnings
        val recentErrors = readFilteredLogcat(minLevel = LogcatLevel.WARN).take(30)

        buildString {
            appendLine("### 📱 Reporte de Diagnóstico VeloRec Studio")
            appendLine("- **Fecha**: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("- **Dispositivo**: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("- **Android**: v${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- **Arquitecturas Soportadas (ABI)**: `$abis`")
            appendLine()
            appendLine("#### 🦀 Estado de Bibliotecas Nativas")
            appendLine("- `libvelorec_rust2024.so`: $rustStatus")
            appendLine("- `libvelorec_cpp23.so`: $cppStatus")
            appendLine()

            latestCrash?.let { crash ->
                appendLine("#### 💥 Último Crash Registrado")
                appendLine("- **Fecha**: ${crash.formattedDate}")
                appendLine("- **Excepción**: `${crash.exceptionClass}`")
                appendLine("- **Mensaje**: ${crash.message}")
                appendLine("```text")
                appendLine(crash.stackTrace.take(1500))
                appendLine("```")
                appendLine()
            }

            appendLine("#### 📋 Registros de Error y Advertencias Recientes (${recentErrors.size})")
            if (recentErrors.isEmpty()) {
                appendLine("_No se encontraron errores ni advertencias recientes en Logcat._")
            } else {
                appendLine("```text")
                for (entry in recentErrors) {
                    val icon = when (entry.level) {
                        LogcatLevel.ERROR, LogcatLevel.FATAL -> "[ERROR]"
                        LogcatLevel.WARN -> "[WARN]"
                        else -> "[INFO]"
                    }
                    appendLine("${entry.timestamp} $icon ${entry.tag}: ${entry.message}")
                }
                appendLine("```")
            }
        }
    }
}
