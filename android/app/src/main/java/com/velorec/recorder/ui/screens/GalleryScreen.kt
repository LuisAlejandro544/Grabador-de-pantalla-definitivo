package com.velorec.recorder.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.velorec.recorder.storage.GalleryStorageManager
import com.velorec.recorder.ui.theme.DarkBackground
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.PrimaryIndigo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GalleryScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Gestiona la visualización, tamaño, previsualización nativa in-app y reproducción
 * de los archivos MP4 generados por los codificadores nativos C++23 y Rust 2024
 * en el almacenamiento local del smartphone.
 * 
 * LÓGICA Y SEGURIDAD ANTICRASH:
 * 1. Reproductor In-App Integrado: Permite visualizar las grabaciones directamente dentro
 *    de la app mediante 'AndroidView(VideoView)' sin depender obligatoriamente de reproductores externos.
 * 2. FileProvider Seguro: Utiliza 'FileProvider.getUriForFile' con 'FLAG_GRANT_READ_URI_PERMISSION'
 *    y manejo de excepciones completo ('try-catch' seguro), eliminando por completo cualquier
 *    'FileUriExposedException' o 'ActivityNotFoundException'.
 * 3. Gestión de Archivos: Permite eliminar videos para liberar espacio en el smartphone.
 */
@Composable
fun NativeGallerySection(accentColor: Color) {
    val context = LocalContext.current
    var videoFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedVideoForPreview by remember { mutableStateOf<File?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    fun refreshVideos() {
        videoFiles = GalleryStorageManager.getAllRecordings(context)
    }

    LaunchedEffect(Unit) {
        refreshVideos()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = PrimaryIndigo)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Galería del Teléfono",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Álbum: Movies/VeloRec • Motor NDK Directo",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (videoFiles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No hay grabaciones locales aún.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Inicia una grabación desde la pestaña 'Grabar' para generar tu primer video MP4 con C++23 o Rust.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            videoFiles.forEach { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedVideoForPreview = file },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val sizeMb = file.length().toDouble() / (1024 * 1024)
                            Text(
                                text = "${"%.1f".format(sizeMb)} MB • ${dateFormat.format(Date(file.lastModified()))}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Botón de Reproducir In-App
                            IconButton(onClick = { selectedVideoForPreview = file }) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Ver video",
                                    tint = accentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Botón de Compartir / Abrir externo
                            IconButton(onClick = { launchExternalVideoPlayer(context, file) }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartir video",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Botón de Eliminar
                            IconButton(onClick = { fileToDelete = file }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar video",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Previsualización Nativa con VideoView (Cero Crashes)
    selectedVideoForPreview?.let { file ->
        Dialog(onDismissRequest = { selectedVideoForPreview = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = file.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { selectedVideoForPreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    var previewAspectRatio by remember { mutableStateOf(9f / 16f) }
                    var playbackFailed by remember { mutableStateOf(false) }

                    // Reproductor VideoView nativo de Android
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                // setZOrderMediaOverlay(true) es INDISPENSABLE dentro de un Dialog en Jetpack Compose.
                                // Sin esto, la superficie de hardware del SurfaceView queda oculta detrás de la
                                // ventana del Dialog, mostrando un recuadro completamente negro.
                                setZOrderMediaOverlay(true)
                                val mediaController = MediaController(ctx)
                                mediaController.setAnchorView(this)
                                setMediaController(mediaController)
                                setOnPreparedListener { mp ->
                                    val w = mp.videoWidth
                                    val h = mp.videoHeight
                                    if (w > 0 && h > 0) {
                                        previewAspectRatio = (w.toFloat() / h.toFloat()).coerceIn(0.4f, 1.8f)
                                    }
                                    mp.isLooping = true
                                    seekTo(0)
                                    start()
                                }
                                setOnCompletionListener { mp ->
                                    mp.seekTo(0)
                                    mp.start()
                                }
                                setOnErrorListener { _, what, extra ->
                                    android.util.Log.w("VeloRec_Gallery", "Error en VideoView: what=$what, extra=$extra")
                                    playbackFailed = true
                                    true
                                }
                                setVideoPath(file.absolutePath)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(previewAspectRatio)
                    )

                    if (playbackFailed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aviso: Si el reproductor integrado no muestra la previsualización, usa 'Abrir Externo' para reproducirlo con la app nativa de tu teléfono (Google Fotos, Galería o VLC).",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { launchExternalVideoPlayer(context, file) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir Externo", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Confirmación para Eliminar Video
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("¿Eliminar grabación?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Se eliminará permanentemente ${file.name} del almacenamiento.", color = Color(0xFFCBD5E1)) },
            containerColor = DarkSurface,
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            GalleryStorageManager.deleteRecording(context, file)
                            refreshVideos()
                            Toast.makeText(context, "Video eliminado", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { fileToDelete = null }) {
                    Text("Cancelar", color = Color.White)
                }
            }
        )
    }
}

/**
 * Lanza un intent seguro con FileProvider y FLAG_GRANT_READ_URI_PERMISSION.
 * Protegido con try-catch para prevenir cualquier FileUriExposedException o ActivityNotFoundException.
 */
private fun launchExternalVideoPlayer(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val isMkv = file.extension.lowercase() == "mkv"
        val mimeType = if (isMkv) "video/x-matroska" else "video/mp4"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Reproducir con...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "No se encontró un reproductor externo compatible.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
