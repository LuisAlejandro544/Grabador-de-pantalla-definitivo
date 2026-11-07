package com.velorec.recorder.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * GalleryStorageManager.kt
 *
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Gestiona el almacenamiento público de las grabaciones de pantalla directamente en la
 * GALERÍA NATIVA del smartphone del usuario (álbum 'Movies/VeloRec' o 'DCIM/VeloRec').
 *
 * LÓGICA Y COMPATIBILIDAD ANDROID (API 26 a API 35):
 * 1. Scoped Storage / MediaStore API (Android 10+ / API 29-35):
 *    - Inserta los metadatos en 'MediaStore.Video.Media.EXTERNAL_CONTENT_URI' con 'RELATIVE_PATH = Movies/VeloRec'.
 *    - Escribe el flujo de bytes directamente en el 'OutputStream' del Uri asignado por el sistema.
 *    - Establece 'IS_PENDING = 0' al finalizar la escritura para que la galería del sistema (Google Fotos,
 *      Galería Samsung, Xiaomi, Motorola, etc.) indexe y muestre la miniatura al instante.
 * 2. Almacenamiento Público Tradicional (Android 8.0 y 9.0 / API 26-28):
 *    - Crea la carpeta '/storage/emulated/0/Movies/VeloRec/'.
 *    - Copia el archivo directamente y lanza 'MediaScannerConnection.scanFile' para indexar el MP4/MKV.
 * 3. Doble Respaldo:
 *    - Si por restricciones de permisos el guardado público fallara, preserva el archivo en el
 *      almacenamiento privado de la aplicación para garantizar que el usuario nunca pierda su grabación.
 */
object GalleryStorageManager {

    private const val TAG = "VeloRec_Gallery"
    private const val ALBUM_DIRECTORY_NAME = "Movies/VeloRec"
    private const val BUFFER_SIZE = 64 * 1024 // 64 KB para transferencia fluida de archivos grandes

    /**
     * Publica un archivo de video grabado (MP4 o MKV) directamente en la Galería del usuario.
     *
     * @param context Contexto de la aplicación o servicio.
     * @param sourceFile Archivo temporal generado por el motor de codificación.
     * @param displayName Nombre visible para el archivo (ej: 'VeloRec_1715000000_cpp23.mp4').
     * @return Archivo final guardado o null si ocurrió un error irrecuperable.
     */
    fun publishVideoToGallery(
        context: Context,
        sourceFile: File,
        displayName: String? = null
    ): File? {
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            Log.e(TAG, "El archivo fuente no existe o está vacío: ${sourceFile.absolutePath}")
            return null
        }

        val targetName = displayName ?: sourceFile.name
        val isMkv = targetName.endsWith(".mkv", ignoreCase = true)
        val mimeType = if (isMkv) "video/x-matroska" else "video/mp4"

        try {
            // Android 10+ (API 29+): Usar MediaStore Scoped Storage
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, targetName)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Video.Media.RELATIVE_PATH, ALBUM_DIRECTORY_NAME)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                }

                val resolver = context.contentResolver
                val collectionUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collectionUri, values)

                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                outputStream.write(buffer, 0, read)
                            }
                            outputStream.flush()
                        }
                    }

                    // Quitar IS_PENDING para que aparezca en la galería del sistema inmediatamente
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)

                    Log.i(TAG, "Video publicado exitosamente en Galería del sistema vía MediaStore: $itemUri")
                }
            }

            // Guardar también copia directa en la carpeta pública Movies/VeloRec
            val publicMoviesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "VeloRec"
            )
            if (!publicMoviesDir.exists()) {
                publicMoviesDir.mkdirs()
            }

            val publicFile = File(publicMoviesDir, targetName)
            if (publicFile.absolutePath != sourceFile.absolutePath) {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(publicFile).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                        output.flush()
                    }
                }
            }

            // Escanear con MediaScannerConnection para forzar indexación inmediata en todas las versiones de Android
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(publicFile.absolutePath),
                arrayOf(mimeType)
            ) { path, uri ->
                Log.i(TAG, "MediaScanner indexó el video en Galería: $path -> $uri")
            }

            return publicFile

        } catch (e: Exception) {
            Log.e(TAG, "Error al publicar video en galería pública: ${e.message}", e)
            // Fallback seguro: devolver el archivo original para que no se pierda la grabación
            return sourceFile
        }
    }

    /**
     * Obtiene la lista unificada de todas las grabaciones de pantalla disponibles en el smartphone:
     * 1. Directorio público de galería ('Movies/VeloRec').
     * 2. Directorio privado de la app ('Android/data/com.velorec.recorder/files/').
     */
    fun getAllRecordings(context: Context): List<File> {
        val uniqueFilesMap = mutableMapOf<String, File>()

        // 1. Archivos en la carpeta pública Movies/VeloRec
        try {
            val publicMoviesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "VeloRec"
            )
            if (publicMoviesDir.exists() && publicMoviesDir.isDirectory) {
                publicMoviesDir.listFiles { file ->
                    val ext = file.extension.lowercase()
                    (ext == "mp4" || ext == "mkv") && file.length() > 0L
                }?.forEach { file ->
                    uniqueFilesMap[file.name] = file
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer directorio público Movies/VeloRec: ${e.message}")
        }

        // 2. Archivos en el directorio privado de la aplicación
        try {
            val privateDir = context.getExternalFilesDir(null) ?: context.filesDir
            privateDir.listFiles { file ->
                val ext = file.extension.lowercase()
                (ext == "mp4" || ext == "mkv") && file.length() > 0L && !file.name.startsWith("temp_")
            }?.forEach { file ->
                if (!uniqueFilesMap.containsKey(file.name)) {
                    uniqueFilesMap[file.name] = file
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer directorio privado: ${e.message}")
        }

        return uniqueFilesMap.values.sortedByDescending { it.lastModified() }
    }

    /**
     * Elimina un archivo de video tanto del disco como de MediaStore para mantener la galería sincronizada.
     */
    fun deleteRecording(context: Context, file: File): Boolean {
        return try {
            val path = file.absolutePath
            val deletedFromDisk = file.delete()

            // Eliminar de MediaStore si existe
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                resolver.delete(
                    uri,
                    "${MediaStore.Video.Media.DISPLAY_NAME} = ?",
                    arrayOf(file.name)
                )
            } else {
                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    arrayOf(path),
                    null
                ) { _, _ -> }
            }

            deletedFromDisk
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar video: ${e.message}", e)
            file.delete()
        }
    }
}
