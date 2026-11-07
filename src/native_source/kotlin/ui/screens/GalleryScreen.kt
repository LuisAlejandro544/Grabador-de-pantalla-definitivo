package com.velorec.recorder.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.core.content.FileProvider
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
 * Gestiona la visualización, tamaño y reproducción de los archivos MP4 generados
 * por los codificadores nativos C++23 y Rust 2024 en el almacenamiento local del dispositivo.
 * 
 * LÓGICA Y SEGURIDAD:
 * 1. Lee asíncronamente el directorio de archivos externos 'context.getExternalFilesDir(null)'.
 * 2. Ordena los videos en orden cronológico inverso (el más reciente primero).
 * 3. Lanza 'FileProvider' con 'FLAG_GRANT_READ_URI_PERMISSION' para una reproducción segura
 *    en reproductores de video externos (VLC, Google Fotos, etc.) cumpliendo con Android 14/15.
 */
@Composable
fun NativeGallerySection(accentColor: Color) {
    val context = LocalContext.current
    var videoFiles by remember {
        mutableStateOf<List<File>>(emptyList())
    }

    LaunchedEffect(Unit) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val files = dir.listFiles { file -> file.extension.lowercase() == "mp4" }
            ?.sortedByDescending { it.lastModified() }
        videoFiles = files ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Menu, contentDescription = null, tint = PrimaryIndigo)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Grabaciones en Almacenamiento Local",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )
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
                    modifier = Modifier.fillMaxWidth(),
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

                        // Botón de Reproducir / Compartir
                        IconButton(
                            onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "video/mp4")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Reproducir Video"))
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.fromFile(file), "video/mp4")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Reproducir Video"))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Reproducir",
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
