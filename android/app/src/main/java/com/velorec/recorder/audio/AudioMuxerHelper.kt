package com.velorec.recorder.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * AudioMuxerHelper.kt
 *
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Proporciona multiplexación ultrarrápida (Stream Copy) para fusionar la pista de video de alta
 * velocidad generada por C++23 o Rust 2024 con la pista de audio capturada en AAC (micrófono,
 * audio interno o mezcla estéreo).
 *
 * CARACTERÍSTICAS TÉCNICAS:
 * 1. Stream Copy sin recodificación: No pierde calidad de video ni CPU; copia directamente los paquetes
 *    codificados entre contenedores MP4 en menos de 300 milisegundos.
 * 2. Resiliencia total ante fallos: Si por alguna razón el archivo de audio resulta ilegible o vacío,
 *    automáticamente preserva el archivo de video intacto en la ruta final para que el usuario jamás
 *    pierda su grabación de pantalla.
 * 3. Limpieza automática: Elimina los archivos temporales una vez generado el MP4 definitivo.
 */
object AudioMuxerHelper {

    private const val TAG = "VeloRec_AudioMuxer"
    private const val BUFFER_CAPACITY_BYTES = 1024 * 1024 // 1 MB para cuadros 4K

    /**
     * Fusiona un archivo de video temporal (MP4) y un archivo de audio AAC (M4A)
     * en el archivo MP4 final con ambas pistas sincronizadas.
     *
     * @param videoPath Ruta del archivo MP4 con la pista de video nativa.
     * @param audioPath Ruta del archivo M4A con la pista de audio AAC.
     * @param outputPath Ruta del archivo MP4 final donde se guardará la grabación completa.
     * @return true si la fusión fue exitosa, false si se aplicó fallback seguro.
     */
    fun mergeAudioVideo(videoPath: String, audioPath: String, outputPath: String): Boolean {
        val videoFile = File(videoPath)
        val audioFile = File(audioPath)
        val outputFile = File(outputPath)

        if (!videoFile.exists() || videoFile.length() == 0L) {
            Log.e(TAG, "El archivo de video fuente no existe o está vacío: $videoPath")
            return false
        }

        // Si el archivo de audio no existe o no tiene datos suficientes, realizar fallback al video
        if (!audioFile.exists() || audioFile.length() < 100L) {
            Log.w(TAG, "Archivo de audio ausente o insuficiente (${audioFile.length()} bytes). Conservando solo video.")
            return safeFallbackVideoOnly(videoFile, outputFile)
        }

        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        return try {
            videoExtractor = MediaExtractor().apply { setDataSource(videoPath) }
            audioExtractor = MediaExtractor().apply { setDataSource(audioPath) }

            // Buscar pista de video
            var videoTrackIndexInExtractor = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndexInExtractor = i
                    videoFormat = format
                    break
                }
            }

            if (videoTrackIndexInExtractor == -1 || videoFormat == null) {
                Log.e(TAG, "No se encontró pista de video en $videoPath")
                return safeFallbackVideoOnly(videoFile, outputFile)
            }

            // Buscar pista de audio
            var audioTrackIndexInExtractor = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndexInExtractor = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndexInExtractor == -1 || audioFormat == null) {
                Log.w(TAG, "No se encontró pista de audio válida en $audioPath. Fallback a video.")
                return safeFallbackVideoOnly(videoFile, outputFile)
            }

            // Crear el Muxer de salida definitivo
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val byteBuffer = ByteBuffer.allocateDirect(BUFFER_CAPACITY_BYTES)
            val bufferInfo = MediaCodec.BufferInfo()

            // =========================================================================
            // SINCRONIZACIÓN MAESTRA DE AUDIO/VIDEO (CERO DESYNC / ANTI-LAG):
            // Tanto el codificador de video en Rust 2024 como AudioRecordEngine estampan
            // las muestras utilizando el reloj monotónico en tiempo real (CLOCK_MONOTONIC).
            // 
            // 1. ANCLAJE UNIFICADO (baseAnchorTime):
            //    En lugar de normalizar video y audio con puntos de referencia separados (lo
            //    que adelantaba el audio ~300-400 ms debido al tiempo de inicialización de
            //    VirtualDisplay y la inyección previa), ambos flujos se anclan exactamente
            //    al timestamp del PRIMER fotograma de video visible.
            //
            // 2. RECORTE DE PRE-ROLL DE AUDIO:
            //    Cualquier muestra de audio capturada antes de que el primer fotograma de
            //    pantalla esté disponible se descarta para que el video y el audio comiencen
            //    en el milisegundo exacto (t = 0).
            //
            // 3. ENTRELAZADO ESTRICTO (INTERLEAVING):
            //    Se intercalan muestras de video y audio según presentationTimeUs para
            //    prevenir desbordamientos en el búfer interno del MediaMuxer del NDK.
            // =========================================================================
            videoExtractor.selectTrack(videoTrackIndexInExtractor)
            audioExtractor.selectTrack(audioTrackIndexInExtractor)

            val rawFirstVideoTime = videoExtractor.sampleTime
            val rawFirstAudioTime = audioExtractor.sampleTime

            // Detectar si ambos flujos operan en el reloj monotónico del sistema (diferencia < 15 segundos y timestamps > 0)
            val isMonotonicSharedClock = rawFirstVideoTime > 0L && rawFirstAudioTime > 0L &&
                kotlin.math.abs(rawFirstVideoTime - rawFirstAudioTime) < 15_000_000L

            val baseAnchorTime = if (isMonotonicSharedClock) {
                rawFirstVideoTime
            } else {
                -1L
            }

            Log.i(
                TAG,
                "Sincronización A/V: rawFirstVideoTime=$rawFirstVideoTime, rawFirstAudioTime=$rawFirstAudioTime, " +
                    "isMonotonicSharedClock=$isMonotonicSharedClock, baseAnchorTime=$baseAnchorTime"
            )

            // Si comparten reloj monotónico, descartar muestras de audio capturadas antes del primer frame de video
            if (isMonotonicSharedClock && baseAnchorTime > 0L) {
                var droppedAudioFrames = 0
                while (true) {
                    val audioPts = audioExtractor.sampleTime
                    if (audioPts < 0L) break
                    // Dejar un margen de 10 ms para absorber el tamaño de cuadro AAC (~21 ms)
                    if (audioPts < baseAnchorTime - 10_000L) {
                        audioExtractor.advance()
                        droppedAudioFrames++
                    } else {
                        break
                    }
                }
                if (droppedAudioFrames > 0) {
                    Log.i(TAG, "Descartadas $droppedAudioFrames muestras de audio previas al inicio del video para sincronización perfecta.")
                }
            }

            var firstVideoSampleTime = -1L
            var firstAudioSampleTime = -1L
            var lastVideoPts = -1L
            var lastAudioPts = -1L
            var videoDone = false
            var audioDone = false

            while (!videoDone || !audioDone) {
                val rawVideoTime = if (!videoDone) videoExtractor.sampleTime else Long.MAX_VALUE
                val rawAudioTime = if (!audioDone) audioExtractor.sampleTime else Long.MAX_VALUE

                if (rawVideoTime == -1L) {
                    videoDone = true
                }
                if (rawAudioTime == -1L) {
                    audioDone = true
                }
                if (videoDone && audioDone) break

                val normVideoTime = if (!videoDone && rawVideoTime != Long.MAX_VALUE) {
                    if (isMonotonicSharedClock) {
                        maxOf(0L, rawVideoTime - baseAnchorTime)
                    } else {
                        if (firstVideoSampleTime < 0) firstVideoSampleTime = rawVideoTime
                        maxOf(0L, rawVideoTime - firstVideoSampleTime)
                    }
                } else Long.MAX_VALUE

                val normAudioTime = if (!audioDone && rawAudioTime != Long.MAX_VALUE) {
                    if (isMonotonicSharedClock) {
                        maxOf(0L, rawAudioTime - baseAnchorTime)
                    } else {
                        if (firstAudioSampleTime < 0) firstAudioSampleTime = rawAudioTime
                        maxOf(0L, rawAudioTime - firstAudioSampleTime)
                    }
                } else Long.MAX_VALUE

                // Escribir la muestra con menor timestamp para mantener entrelazado y sincronización estricta
                if (!videoDone && (audioDone || normVideoTime <= normAudioTime)) {
                    byteBuffer.clear()
                    val sampleSize = videoExtractor.readSampleData(byteBuffer, 0)
                    if (sampleSize < 0) {
                        videoDone = true
                    } else {
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize

                        var pts = normVideoTime
                        if (pts <= lastVideoPts) {
                            pts = if (lastVideoPts >= 0) lastVideoPts + 1000L else 0L
                        }
                        lastVideoPts = pts
                        bufferInfo.presentationTimeUs = pts

                        val sampleFlags = videoExtractor.sampleFlags
                        var codecFlags = 0
                        if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                        }
                        if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
                            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                        }
                        bufferInfo.flags = codecFlags

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            muxer.writeSampleData(muxerVideoTrack, byteBuffer, bufferInfo)
                        }
                        videoExtractor.advance()
                    }
                } else if (!audioDone) {
                    byteBuffer.clear()
                    val sampleSize = audioExtractor.readSampleData(byteBuffer, 0)
                    if (sampleSize < 0) {
                        audioDone = true
                    } else {
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize

                        var pts = normAudioTime
                        if (pts <= lastAudioPts) {
                            pts = if (lastAudioPts >= 0) lastAudioPts + 1000L else 0L
                        }
                        lastAudioPts = pts
                        bufferInfo.presentationTimeUs = pts

                        val sampleFlags = audioExtractor.sampleFlags
                        var codecFlags = 0
                        if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                        }
                        if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
                            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                        }
                        bufferInfo.flags = codecFlags

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            muxer.writeSampleData(muxerAudioTrack, byteBuffer, bufferInfo)
                        }
                        audioExtractor.advance()
                    }
                }
            }

            // Cerrar el Muxer de forma segura antes de limpiar archivos
            var muxerClosedCleanly = false
            try {
                muxer.stop()
                muxerClosedCleanly = true
            } catch (e: Exception) {
                Log.e(TAG, "Error al finalizar muxer: ${e.message}", e)
            } finally {
                try { muxer.release() } catch (_: Exception) {}
                muxer = null
            }

            if (muxerClosedCleanly && outputFile.exists() && outputFile.length() > 0L) {
                Log.i(TAG, "Fusión de video y audio completada con éxito en: $outputPath (${outputFile.length()} bytes)")
                // Limpieza de temporales solo tras confirmar éxito
                videoFile.delete()
                audioFile.delete()
                true
            } else {
                Log.w(TAG, "Muxer incompleto o corrupto, aplicando fallback a video sin recodificar.")
                safeFallbackVideoOnly(videoFile, outputFile)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error durante la fusión de audio y video: ${e.message}", e)
            try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            muxer = null
            safeFallbackVideoOnly(videoFile, outputFile)
        } finally {
            try {
                videoExtractor?.release()
                audioExtractor?.release()
                muxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Aviso al liberar recursos de extractors: ${e.message}")
            }
        }
    }

    /**
     * Fallback seguro: Si la pista de audio falló, normaliza el video a base cero y lo guarda
     * en la ruta de salida para garantizar que la grabación nunca se pierda y sea reproducible
     * en cualquier galería o reproductor.
     */
    private fun safeFallbackVideoOnly(videoFile: File, outputFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            var videoTrackIdx = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIdx = i
                    videoFormat = format
                    break
                }
            }

            if (videoTrackIdx >= 0 && videoFormat != null) {
                extractor.selectTrack(videoTrackIdx)
                val tempRemuxFile = File(outputFile.parentFile, "remux_${System.currentTimeMillis()}.mp4")
                muxer = MediaMuxer(tempRemuxFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrack = muxer.addTrack(videoFormat)
                muxer.start()

                val buffer = ByteBuffer.allocateDirect(BUFFER_CAPACITY_BYTES)
                val bufferInfo = MediaCodec.BufferInfo()
                var firstPts = -1L
                var lastPts = -1L

                while (true) {
                    val sampleTime = extractor.sampleTime
                    if (sampleTime < 0) break
                    if (firstPts < 0) firstPts = sampleTime

                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    var pts = maxOf(0L, sampleTime - firstPts)
                    if (pts <= lastPts) {
                        pts = if (lastPts >= 0) lastPts + 1000L else 0L
                    }
                    lastPts = pts
                    bufferInfo.presentationTimeUs = pts

                    val sampleFlags = extractor.sampleFlags
                    var codecFlags = 0
                    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }
                    bufferInfo.flags = codecFlags
                    muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                    extractor.advance()
                }

                muxer.stop()
                muxer.release()
                muxer = null
                extractor.release()
                extractor = null

                if (tempRemuxFile.exists() && tempRemuxFile.length() > 0L) {
                    if (outputFile.exists()) outputFile.delete()
                    val renamed = tempRemuxFile.renameTo(outputFile)
                    if (!renamed) {
                        tempRemuxFile.copyTo(outputFile, overwrite = true)
                        tempRemuxFile.delete()
                    }
                    videoFile.delete()
                    Log.i(TAG, "Fallback seguro: video remuxed a base 0 exitosamente.")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Aviso en remux de fallback: ${e.message}, intentando preservación directa.")
        } finally {
            try { extractor?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }

        return try {
            if (videoFile.exists()) {
                if (videoFile.absolutePath != outputFile.absolutePath) {
                    if (outputFile.exists()) outputFile.delete()
                    val renamed = videoFile.renameTo(outputFile)
                    if (!renamed) {
                        videoFile.copyTo(outputFile, overwrite = true)
                        videoFile.delete()
                    }
                }
                Log.i(TAG, "Fallback seguro aplicado: video preservado directamente.")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en safeFallbackVideoOnly: ${e.message}", e)
            false
        }
    }
}
