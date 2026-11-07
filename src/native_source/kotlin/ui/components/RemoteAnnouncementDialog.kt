package com.velorec.recorder.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velorec.recorder.data.model.RemoteAnnouncementConfig
import com.velorec.recorder.ui.theme.DarkSurface

/**
 * RemoteAnnouncementDialog.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Diálogo modal emergente que se activa únicamente si la configuración remota
 * descargada de GitHub Releases incluye un anuncio importante o notificación de versión.
 */
@Composable
fun RemoteAnnouncementDialog(
    announcement: RemoteAnnouncementConfig,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    if (announcement.showDialog && announcement.message.isNotBlank()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Entendido", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = announcement.title.ifBlank { "Novedades de VeloRec" },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = announcement.message,
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
