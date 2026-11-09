package com.velorec.recorder.service

import java.io.File

/**
 * HardwareStorageDetector.kt
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Detección estricta de tecnología de almacenamiento físico (eMMC vs UFS / NVMe)
 * en teléfonos Android mediante inspección de nodos de dispositivos de bloques en Linux.
 * 
 * LÓGICA Y ARQUITECTURA MODULAR:
 * Desacopla la lógica de verificación de hardware del servicio principal 'ScreenRecordService'.
 * Permite tomar decisiones de buffer pacing y descarte asíncrono en memorias lentas eMMC 5.1
 * para evitar micro-tirones (stutter) en juegos pesados.
 */
object HardwareStorageDetector {

    /**
     * Comprueba si el dispositivo opera sobre almacenamiento eMMC lento ('mmcblk0').
     */
    fun isEmmcStorageDetected(): Boolean {
        return try {
            File("/sys/block/mmcblk0").exists() ||
            File("/sys/class/block/mmcblk0").exists() ||
            File("/proc/partitions").takeIf { it.exists() }?.readText()?.contains("mmcblk0") == true
        } catch (_: Exception) {
            false
        }
    }
}
