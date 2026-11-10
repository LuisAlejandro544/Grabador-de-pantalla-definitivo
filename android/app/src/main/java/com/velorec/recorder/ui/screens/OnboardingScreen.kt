package com.velorec.recorder.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.velorec.recorder.ui.theme.DarkBackground
import com.velorec.recorder.ui.theme.DarkCard
import com.velorec.recorder.ui.theme.DarkSurface
import com.velorec.recorder.ui.theme.RecordRed
import com.velorec.recorder.ui.theme.SuccessGreen
import com.velorec.recorder.ui.theme.WarningAmber

/**
 * OnboardingScreen.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Pantalla de bienvenida interactiva de múltiples páginas para la primera apertura de VeloRec Studio.
 * 
 * PROPÓSITO Y EXPERIENCIA DE USUARIO:
 * 1. Explica al usuario cómo funciona la app y sus ventajas reales (sin anuncios, NDK Rust Zero-Copy, audio interno y 1-Click REC).
 * 2. Permite conceder de manera clara y guiada los permisos estándar de Android (Audio y Notificaciones).
 * 3. Proporciona navegación táctil fluida con indicadores animados y opción de omitir.
 */
@Composable
fun OnboardingScreen(
    accentColor: Color,
    primaryColor: Color,
    onRequestPermissions: () -> Unit,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 4

    fun checkAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun checkNotificationsPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    var audioGranted by remember { mutableStateOf(checkAudioPermission()) }
    var notificationsGranted by remember { mutableStateOf(checkNotificationsPermission()) }

    // Reevaluar permisos automáticamente al volver del diálogo del sistema (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                audioGranted = checkAudioPermission()
                notificationsGranted = checkNotificationsPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra superior con botón Omitir
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo miniatura
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(primaryColor, accentColor))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VeloRec Studio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (currentPage < totalPages - 1) {
                    TextButton(onClick = onFinishOnboarding) {
                        Text(
                            text = "Omitir",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contenedor principal de la página actual con scroll vertical seguro
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                when (currentPage) {
                    0 -> OnboardingPagePerformance(accentColor, primaryColor)
                    1 -> OnboardingPageAudio(accentColor, primaryColor)
                    2 -> OnboardingPageLauncher(accentColor, primaryColor)
                    3 -> OnboardingPagePermissions(
                        accentColor = accentColor,
                        primaryColor = primaryColor,
                        audioGranted = audioGranted,
                        notificationsGranted = notificationsGranted,
                        onRequestPermissions = onRequestPermissions
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indicador de Páginas (Dots animados)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                for (i in 0 until totalPages) {
                    val isSelected = i == currentPage
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accentColor else Color(0xFF334155)
                            )
                            .clickable { currentPage = i }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de acción inferiores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkCard,
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .height(54.dp)
                            .clickable { currentPage -= 1 }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = "Atrás",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (currentPage < totalPages - 1) {
                            currentPage += 1
                        } else {
                            onFinishOnboarding()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentPage == totalPages - 1) RecordRed else primaryColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (currentPage == totalPages - 1) "¡COMENZAR AHORA!" else "Siguiente",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentPage == totalPages - 1) Icons.Default.Check else Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Página 1: Rendimiento Móvil y Rust 2024
 */
@Composable
private fun OnboardingPagePerformance(accentColor: Color, primaryColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.15f))
                .border(2.dp, accentColor.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Grabación Sin Lag Ni Anuncios",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "100% nativa para Android. Sin anuncios molestos, sin marcas de agua forzadas y sin suscripciones.",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureBenefitCard(
            icon = Icons.Default.Bolt,
            iconTint = WarningAmber,
            title = "Motor NDK Rust 2024 (Zero-Copy)",
            subtitle = "Memoria plana (<15 MB) inmune al cierre de Android durante partidas intensivas."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureBenefitCard(
            icon = Icons.Default.SportsEsports,
            iconTint = accentColor,
            title = "60 y 120 FPS Fluidos",
            subtitle = "Reserva los núcleos de alto rendimiento de tu CPU 100% libres para tu videojuego."
        )
    }
}

/**
 * Página 2: Audio Interno y Sincronización Maestra
 */
@Composable
private fun OnboardingPageAudio(accentColor: Color, primaryColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0xFF0369A1).copy(alpha = 0.2f))
                .border(2.dp, Color(0xFF38BDF8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sonido de Juegos Cristalino",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Se acabaron los videos con sonido desfasado o con ruidos molestos del ambiente.",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureBenefitCard(
            icon = Icons.Default.Videocam,
            iconTint = SuccessGreen,
            title = "Audio Interno Puro (Android 10+)",
            subtitle = "Captura directamente el sonido interno del juego sin activar el micrófono ni ruidos."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureBenefitCard(
            icon = Icons.Default.Mic,
            iconTint = Color(0xFFC084FC),
            title = "Mezcla Estéreo y Cero Esperas",
            subtitle = "Graba tu voz y el juego a la vez. Al tocar detener, se finaliza en menos de 15 ms."
        )
    }
}

/**
 * Página 3: Lanzador 1-Click REC y Galería
 */
@Composable
private fun OnboardingPageLauncher(accentColor: Color, primaryColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0xFF431407).copy(alpha = 0.3f))
                .border(2.dp, WarningAmber, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Lanzador Rápido 1-Click REC",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Accede a tus juegos favoritos y comienza a grabar sin pasos manuales ni esperas.",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureBenefitCard(
            icon = Icons.Default.SportsEsports,
            iconTint = WarningAmber,
            title = "Abre y Graba al Instante",
            subtitle = "Toca tu juego (Free Fire, PUBG, Roblox, Brawl Stars) y la app comenzará a capturar automáticamente."
        )

        Spacer(modifier = Modifier.height(10.dp))

        FeatureBenefitCard(
            icon = Icons.Default.VideoLibrary,
            iconTint = primaryColor,
            title = "Galería Pública Directa (Movies/VeloRec)",
            subtitle = "Tus grabaciones aparecen al momento en Google Fotos, Galería de tu móvil o WhatsApp."
        )
    }
}

/**
 * Página 4: Permisos Necesarios
 */
@Composable
private fun OnboardingPagePermissions(
    accentColor: Color,
    primaryColor: Color,
    audioGranted: Boolean,
    notificationsGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.2f))
                .border(2.dp, primaryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Permisos de Android",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "VeloRec solo solicita los permisos esenciales para capturar audio y controlar la grabación:",
            fontSize = 13.5.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tarjeta de Permiso de Audio
        PermissionItemCard(
            title = "Grabación de Audio / Micrófono",
            description = "Para capturar tu voz o mezclarla con el sonido del juego.",
            icon = Icons.Default.Mic,
            isGranted = audioGranted,
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tarjeta de Permiso de Notificaciones (Android 13+)
        PermissionItemCard(
            title = "Controles en Notificación",
            description = "Para pausar, reanudar o detener la grabación desde la barra de estado.",
            icon = Icons.Default.Notifications,
            isGranted = notificationsGranted,
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!audioGranted || !notificationsGranted) {
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Conceder Permisos de Android",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        } else {
            Surface(
                color = SuccessGreen.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "¡Todos los permisos concedidos!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }
        }
    }
}

/**
 * Componente visual para mostrar una ventaja o característica destacada.
 */
@Composable
private fun FeatureBenefitCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF223147)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * Componente visual para cada permiso con indicador de concedido o pendiente.
 */
@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    accentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (isGranted) SuccessGreen.copy(alpha = 0.4f) else Color(0xFF334155)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isGranted) SuccessGreen.copy(alpha = 0.15f) else DarkCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) SuccessGreen else accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = if (isGranted) SuccessGreen.copy(alpha = 0.2f) else DarkCard,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (isGranted) SuccessGreen else Color(0xFF475569))
            ) {
                Text(
                    text = if (isGranted) "LISTO" else "PENDIENTE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isGranted) SuccessGreen else Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
