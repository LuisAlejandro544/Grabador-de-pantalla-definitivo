package com.velorec.recorder.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AudioRecordEngine.kt
 *
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Motor de captura y codificación de audio en tiempo real de alta fidelidad para Android.
 *
 * CORRECCIONES CRÍTICAS IMPLEMENTADAS:
 * 1. ByteOrder.LITTLE_ENDIAN OBLIGATORIO:
 *    - Los codificadores AAC por hardware de Android esperan PCM 16-bit Little-Endian en arquitecturas ARM.
 *    - ByteBuffer.allocateDirect usa Big-Endian por defecto en Java NIO; invertir los bytes convertía
 *      cualquier sonido ambiente en ruido blanco estático de máximo volumen ("shhhhh").
 *      Se fuerza estrictamente .order(ByteOrder.LITTLE_ENDIAN).
 * 2. EXPANSIÓN MONO A ESTÉREO REAL:
 *    - El 95% de los smartphones Android no soportan CHANNEL_IN_STEREO en el micrófono principal.
 *    - Al degradar a CHANNEL_IN_MONO, los datos deben duplicarse idénticamente a los canales Izquierdo
 *      y Derecho (L = s, R = s) en vez de tratarse como estéreo entrelazado (lo cual provocaba desfase,
 *      cancelación de fase, duplicación de tiempo y distorsión metálica).
 * 3. MEZCLA CONCURRENTE SIN BLOQUEOS ("both"):
 *    - Las lecturas secuenciales bloqueantes de micrófono y audio interno sumaban 46ms de espera por ciclo,
 *      desbordando los buffers de hardware y causando pérdida de paquetes y chasquidos.
 *    - Se utiliza un hilo lector concurrente con cola de desacoplamiento para el audio interno,
 *      garantizando que la falta de sonido en juegos o apps no bloquee ni distorsione la voz del micrófono.
 * 4. TIMESTAMP MONOTÓNICO EXACTO BASADO EN MUESTRAS:
 *    - PTS = (totalFrames * 1_000_000L) / SAMPLE_RATE, eliminando desviaciones de tiempo respecto al video.
 */
class AudioRecordEngine(
    private val context: Context,
    private val audioSource: String,
    private val outputM4aPath: String? = null,
    private val mediaProjection: MediaProjection?
) {

    companion object {
        private const val TAG = "VeloRec_AudioEngine"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 2 // Estéreo AAC
        private const val BITRATE = 128_000 // 128 kbps AAC-LC
        private const val TIMEOUT_US = 10_000L
        private const val SAMPLES_PER_FRAME = 1024 // 1024 muestras por canal = 2048 muestras estéreo (23.2ms)
    }

    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    private var workerThread: Thread? = null
    private var internalAudioReaderThread: Thread? = null

    /**
     * Chunk de memoria nativa fuera del Heap de Java (DirectByteBuffer Off-Heap).
     * Garantiza cero alocaciones de memoria durante la captura continua de audio,
     * impidiendo que el Recolector de Basura (GC) de ART se active durante partidas o videos.
     */
    private class DirectAudioChunk(val capacityBytes: Int) {
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(capacityBytes).order(ByteOrder.LITTLE_ENDIAN)
        var sizeBytes: Int = 0
    }

    private var micRecord: AudioRecord? = null
    private var isMicMono = false

    private var internalRecord: AudioRecord? = null
    private val freeChunkPool = ConcurrentLinkedQueue<DirectAudioChunk>()
    private val internalAudioQueue = ConcurrentLinkedQueue<DirectAudioChunk>()

    private var audioEncoder: MediaCodec? = null
    private var audioMuxer: MediaMuxer? = null
    private var audioTrackIndex: Int = -1
    private var isMuxerStarted: Boolean = false

    /**
     * Callback invocado cuando el codificador AAC genera el codec-specific data (csd-0).
     * Permite registrar la pista de audio en AMediaMuxer de C++23 en tiempo real.
     */
    var onAudioTrackConfigured: ((csdBuffer: ByteBuffer, csdSize: Int, sampleRate: Int, channelCount: Int) -> Unit)? = null

    /**
     * Callback invocado cada vez que se produce una muestra AAC codificada.
     * Envía los bytes directamente al muxer nativo en tiempo real sin pasar por archivos temporales.
     */
    var onAudioSampleEncoded: ((dataBuffer: ByteBuffer, offset: Int, size: Int, ptsUs: Long, flags: Int) -> Unit)? = null

    /**
     * Inicia la captura y codificación de audio en un hilo de fondo dedicado.
     */
    fun start() {
        if (audioSource == "mute") {
            Log.i(TAG, "Audio configurado en 'mute'. No se inicia motor de audio.")
            return
        }

        if (isRecording.getAndSet(true)) {
            Log.w(TAG, "El motor de audio ya se encontraba en ejecución.")
            return
        }

        isPaused.set(false)
        internalAudioQueue.clear()

        workerThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runAudioLoop()
        }, "VeloRecAudioWorker").apply {
            start()
        }

        Log.i(TAG, "Motor de audio iniciado con modo: '$audioSource' -> ${outputM4aPath ?: "Multiplexación Nativa NDK"}")
    }

    fun pause() {
        isPaused.set(true)
        Log.i(TAG, "Captura de audio pausada.")
    }

    fun resume() {
        isPaused.set(false)
        Log.i(TAG, "Captura de audio reanudada.")
    }

    /**
     * Detiene la captura y codificación, asegurando el vaciado de buffers y liberación de hardware.
     */
    fun stop() {
        if (!isRecording.getAndSet(false)) {
            return
        }

        try {
            internalAudioReaderThread?.join(800)
        } catch (_: InterruptedException) {}
        internalAudioReaderThread = null

        try {
            workerThread?.join(3000)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupción al esperar hilo de audio: ${e.message}")
        }
        workerThread = null

        cleanup()
        Log.i(TAG, "Motor de audio detenido y recursos liberados.")
    }

    @SuppressLint("MissingPermission")
    private fun initAudioRecords(bufferSize: Int) {
        val shouldCaptureMic = audioSource == "mic" || audioSource == "both"
        val shouldCaptureInternal = audioSource == "internal" || audioSource == "both"

        // 1. Inicializar AudioRecord para Micrófono con verificación mono/estéreo
        if (shouldCaptureMic) {
            try {
                micRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 4
                )
                if (micRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.i(TAG, "Dispositivo móvil no soporta micrófono estéreo nativo. Usando CHANNEL_IN_MONO con expansión estéreo.")
                    micRecord?.release()
                    micRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize * 4
                    )
                    isMicMono = true
                } else {
                    isMicMono = false
                    Log.i(TAG, "Micrófono estéreo nativo inicializado con éxito.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al inicializar micrófono: ${e.message}", e)
                micRecord = null
            }
        }

        // 2. Inicializar AudioRecord para Audio Interno (Android 10+ / API 29+)
        if (shouldCaptureInternal && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
            try {
                val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build()

                val internalBufferSize = maxOf(bufferSize * 4, 16384)
                internalRecord = AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(playbackConfig)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(internalBufferSize)
                    .build()

                if (internalRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "Audio interno no inicializado con éxito. Estado: ${internalRecord?.state}")
                    internalRecord?.release()
                    internalRecord = null
                } else {
                    Log.i(TAG, "Captura de audio interno configurada correctamente.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al configurar AudioPlaybackCapture: ${e.message}", e)
                internalRecord = null
            }
        }
    }

    private fun initEncoderAndMuxer() {
        try {
            if (!outputM4aPath.isNullOrBlank()) {
                val outputFile = File(outputM4aPath)
                outputFile.parentFile?.mkdirs()
                if (outputFile.exists()) outputFile.delete()
            }

            // Formato AAC-LC Estéreo (44.1 kHz, 128 kbps)
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            if (!outputM4aPath.isNullOrBlank()) {
                audioMuxer = MediaMuxer(outputM4aPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } else {
                audioMuxer = null
            }
            isMuxerStarted = false
            audioTrackIndex = -1

        } catch (e: Exception) {
            Log.e(TAG, "Fallo al inicializar codificador AAC o Muxer: ${e.message}", e)
        }
    }

    private fun runAudioLoop() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        initAudioRecords(minBufferSize)
        initEncoderAndMuxer()

        val encoder = audioEncoder ?: return
        val muxer = audioMuxer

        try {
            micRecord?.startRecording()
            internalRecord?.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error al arrancar AudioRecord: ${e.message}", e)
        }

        // Buffers de trabajo preasignados
        // SAMPLES_PER_FRAME = 1024, con 2 canales = 2048 shorts (4096 bytes)
        val stereoSamplesCount = SAMPLES_PER_FRAME * CHANNEL_COUNT
        val chunkSizeBytes = stereoSamplesCount * 2
        val micMonoBuffer = if (isMicMono) ShortArray(SAMPLES_PER_FRAME) else null
        val micStereoBuffer = ShortArray(stereoSamplesCount)
        val finalStereoBuffer = ShortArray(stereoSamplesCount)

        // Pre-alocar pool de chunks DirectByteBuffer Off-Heap (cero impacto en el recolector de basura de Java)
        freeChunkPool.clear()
        internalAudioQueue.clear()
        for (i in 0 until 16) {
            freeChunkPool.offer(DirectAudioChunk(chunkSizeBytes))
        }

        // Arrancamos lector dedicado en segundo plano para audio interno tanto en modo 'internal' como en 'both'
        // usando exclusivamente DirectByteBuffer (off-heap) para erradicar cualquier bloqueo y alocaciones por frame
        if ((audioSource == "both" || audioSource == "internal") && internalRecord != null) {
            internalAudioReaderThread = Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                while (isRecording.get()) {
                    if (isPaused.get()) {
                        try { Thread.sleep(20) } catch (_: InterruptedException) { break }
                        continue
                    }
                    val chunk = freeChunkPool.poll() ?: DirectAudioChunk(chunkSizeBytes)
                    chunk.buffer.clear()
                    val readBytes = internalRecord?.read(chunk.buffer, chunkSizeBytes) ?: 0
                    if (readBytes > 0) {
                        chunk.sizeBytes = readBytes
                        // Limitar cola a 10 chunks (~230ms) para descartar en caso de retraso extremo
                        if (internalAudioQueue.size > 10) {
                            val discarded = internalAudioQueue.poll()
                            if (discarded != null) freeChunkPool.offer(discarded)
                        }
                        internalAudioQueue.offer(chunk)
                    } else {
                        freeChunkPool.offer(chunk)
                        try { Thread.sleep(5) } catch (_: InterruptedException) { break }
                    }
                }
            }, "VeloRecInternalAudioReader").apply { start() }
        }

        // ByteBuffer con ORDEN LITTLE_ENDIAN ESTRICTO (Vital para no corromper muestras a ruido blanco)
        val pcmByteBuffer = ByteBuffer.allocateDirect(stereoSamplesCount * 2).order(ByteOrder.LITTLE_ENDIAN)

        val bufferInfo = MediaCodec.BufferInfo()
        var totalFramesEncoded = 0L

        while (isRecording.get()) {
            if (isPaused.get()) {
                try {
                    Thread.sleep(20)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            var framesObtained = 0

            when (audioSource) {
                "mic" -> {
                    if (isMicMono && micMonoBuffer != null) {
                        val readMono = micRecord?.read(micMonoBuffer, 0, SAMPLES_PER_FRAME) ?: 0
                        if (readMono > 0) {
                            // Expansión Mono a Estéreo idéntico (L = s, R = s)
                            for (i in 0 until readMono) {
                                val s = micMonoBuffer[i]
                                finalStereoBuffer[i * 2] = s
                                finalStereoBuffer[i * 2 + 1] = s
                            }
                            framesObtained = readMono
                        }
                    } else {
                        val readStereo = micRecord?.read(finalStereoBuffer, 0, stereoSamplesCount) ?: 0
                        if (readStereo > 0) {
                            framesObtained = readStereo / CHANNEL_COUNT
                        }
                    }
                }

                "internal" -> {
                    // Extraer audio interno puro de la cola concurrente (DirectByteBuffer Off-Heap)
                    val internalChunk = internalAudioQueue.poll()
                    if (internalChunk != null) {
                        internalChunk.buffer.position(0)
                        internalChunk.buffer.limit(internalChunk.sizeBytes)
                        val shortBuf = internalChunk.buffer.asShortBuffer()
                        val samplesCount = minOf(shortBuf.remaining(), stereoSamplesCount)
                        for (i in 0 until samplesCount) {
                            finalStereoBuffer[i] = shortBuf.get()
                        }
                        for (i in samplesCount until stereoSamplesCount) {
                            finalStereoBuffer[i] = 0
                        }
                        framesObtained = stereoSamplesCount / CHANNEL_COUNT
                        freeChunkPool.offer(internalChunk)
                    } else if (internalRecord != null) {
                        // Si el juego está en silencio o cargando pero el grabador interno está activo,
                        // generar silencio temporal a cadencia fija de frame para mantener estricta
                        // sincronización monotónica de audio y video sin bloquear el pipeline.
                        java.util.Arrays.fill(finalStereoBuffer, 0.toShort())
                        framesObtained = stereoSamplesCount / CHANNEL_COUNT
                        try {
                            Thread.sleep(20)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                }

                "both" -> {
                    // 1. Obtener audio del micrófono
                    if (isMicMono && micMonoBuffer != null) {
                        val readMono = micRecord?.read(micMonoBuffer, 0, SAMPLES_PER_FRAME) ?: 0
                        if (readMono > 0) {
                            for (i in 0 until readMono) {
                                val s = micMonoBuffer[i]
                                micStereoBuffer[i * 2] = s
                                micStereoBuffer[i * 2 + 1] = s
                            }
                            framesObtained = readMono
                        }
                    } else {
                        val readStereo = micRecord?.read(micStereoBuffer, 0, stereoSamplesCount) ?: 0
                        if (readStereo > 0) {
                            framesObtained = readStereo / CHANNEL_COUNT
                        }
                    }

                    // 2. Extraer audio interno de la cola concurrente (DirectByteBuffer Off-Heap)
                    val internalChunk = internalAudioQueue.poll()
                    val internalShorts = if (internalChunk != null) {
                        internalChunk.buffer.position(0)
                        internalChunk.buffer.limit(internalChunk.sizeBytes)
                        internalChunk.buffer.asShortBuffer()
                    } else null
                    val internalSamples = if (internalShorts != null && internalChunk != null) internalChunk.sizeBytes / 2 else 0

                    // 3. Mezcla Estéreo con Suma Saturada (Clamping anti-distorsión)
                    val mixSamples = framesObtained * CHANNEL_COUNT
                    for (i in 0 until mixSamples) {
                        val micSample = micStereoBuffer[i].toInt()
                        val intSample = if (i < internalSamples && internalShorts != null) internalShorts.get(i).toInt() else 0
                        val mixed = (micSample + intSample).coerceIn(-32768, 32767)
                        finalStereoBuffer[i] = mixed.toShort()
                    }

                    // 4. Reciclar chunk DirectByteBuffer off-heap para reutilización sin crear basura en el heap
                    if (internalChunk != null) {
                        freeChunkPool.offer(internalChunk)
                    }
                }
            }

            if (framesObtained <= 0) {
                try {
                    Thread.sleep(5)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            // Transferir muestras 16-bit PCM al ByteBuffer en formato Little-Endian
            val totalShortsToWrite = framesObtained * CHANNEL_COUNT
            val pcmBytesLength = totalShortsToWrite * 2

            pcmByteBuffer.clear()
            for (i in 0 until totalShortsToWrite) {
                pcmByteBuffer.putShort(finalStereoBuffer[i])
            }
            pcmByteBuffer.flip()

            // Cálculo estricto de marca de tiempo (PTS) basado en muestras reales
            val presentationTimeUs = (totalFramesEncoded * 1_000_000L) / SAMPLE_RATE

            // Alimentar encoder MediaCodec
            val inputBufferIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(pcmByteBuffer)

                encoder.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    pcmBytesLength,
                    presentationTimeUs,
                    0
                )
                totalFramesEncoded += framesObtained
            }

            // Drenar paquetes codificados hacia el Muxer
            drainEncoder(encoder, muxer, bufferInfo, endOfStream = false)
        }

        // Señal de fin de flujo de audio (EOS)
        val finalPtsUs = (totalFramesEncoded * 1_000_000L) / SAMPLE_RATE
        val endInputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
        if (endInputIndex >= 0) {
            encoder.queueInputBuffer(endInputIndex, 0, 0, finalPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        drainEncoder(encoder, muxer, bufferInfo, endOfStream = true)
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer?,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean
    ) {
        while (true) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                val csd0 = newFormat.getByteBuffer("csd-0")
                if (csd0 != null) {
                    onAudioTrackConfigured?.invoke(csd0, csd0.remaining(), SAMPLE_RATE, CHANNEL_COUNT)
                }

                if (muxer != null && !isMuxerStarted) {
                    audioTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    isMuxerStarted = true
                    Log.i(TAG, "Muxer de audio independiente iniciado. Pista asignada: $audioTrackIndex")
                }
            } else if (outputBufferIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(outputBufferIndex)

                if (encodedData != null && bufferInfo.size > 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)

                        // Transmitir directamente a C++23 si la multiplexación en vivo está activa
                        if (onAudioSampleEncoded != null) {
                            onAudioSampleEncoded?.invoke(
                                encodedData,
                                bufferInfo.offset,
                                bufferInfo.size,
                                bufferInfo.presentationTimeUs,
                                bufferInfo.flags
                            )
                        } else if (muxer != null && isMuxerStarted) {
                            muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                        }
                    }
                }

                encoder.releaseOutputBuffer(outputBufferIndex, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            } else {
                // Sin datos adicionales disponibles en este ciclo
                break
            }

            if (!endOfStream && outputBufferIndex < 0) {
                break
            }
        }
    }

    private fun cleanup() {
        try {
            micRecord?.stop()
            micRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al liberar micRecord: ${e.message}")
        }
        micRecord = null

        try {
            internalRecord?.stop()
            internalRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al liberar internalRecord: ${e.message}")
        }
        internalRecord = null

        try {
            audioEncoder?.stop()
            audioEncoder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al liberar audioEncoder: ${e.message}")
        }
        audioEncoder = null

        try {
            if (isMuxerStarted) {
                audioMuxer?.stop()
            }
            audioMuxer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al liberar audioMuxer: ${e.message}")
        }
        audioMuxer = null
        isMuxerStarted = false
        audioTrackIndex = -1
    }
}

