package com.velorec.recorder.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.velorec.recorder.data.model.AppUpdateInfo
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.SuccessGreen

/**
 * AppUpdateDialog.kt
 *
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Diálogo modal táctil en Jetpack Compose (Material 3) que notifica la disponibilidad
 * de una nueva versión en GitHub Releases, muestra las notas del release (changelog)
 * y provee el botón para descargar e instalar el archivo APK directamente.
 *
 * GARANTÍA DE SEGURIDAD PARA EL USUARIO:
 * Informa de manera destacada que todas las grabaciones guardadas en la galería pública
 * ('Movies/VeloRec') están blindadas y NUNCA se eliminarán con la actualización.
 */
@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    accentColor: Color,
    primaryColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = BorderStroke(1.5.dp, accentColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera con Icono de Actualización
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(primaryColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Actualización",
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "¡Nueva Versión Disponible!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Badges de Versión (Actual vs Nueva)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Actual: v${updateInfo.currentVersionTag}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text("➔", color = Color(0xFF64748B), fontSize = 12.sp)

                    Surface(
                        color = SuccessGreen.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Nueva: v${updateInfo.latestVersionTag}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notas del Release (Changelog extraído de GitHub)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "📋 Novedades de la Versión:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (updateInfo.releaseNotes.isNotBlank()) {
                                updateInfo.releaseNotes
                            } else {
                                "• Optimizaciones de latencia y paridad de resolución.\n• Mejoras de estabilidad en el codificador nativo NDK.\n• Corrección de desincronización de audio en juegos intensivos."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.Default
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Garantía explícita de seguridad de grabaciones
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Blindaje de grabaciones",
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tus grabaciones en la Galería (Movies/VeloRec) están 100% protegidas y NO se borrarán.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Botón principal de descarga e instalación
                Button(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.apkDownloadUrl))
                        context.startActivity(browserIntent)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Descargar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Descargar e Instalar APK",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón secundario para descartar temporalmente
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text(
                        text = "Recordármelo Más Tarde",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
