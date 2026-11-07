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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AudioRecordEngine.kt
 *
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Motor de captura y codificación de audio en tiempo real para Android.
 *
 * MODOS DE CAPTURA SOPORTADOS:
 * 1. "mic": Captura de audio de voz a través del micrófono del dispositivo móvil.
 * 2. "internal": Captura de audio interno del sistema / videojuegos / YouTube mediante
 *    'AudioPlaybackCaptureConfiguration' en Android 10+ (API 29+) utilizando el token de MediaProjection.
 * 3. "both": Mezcla estéreo simultánea en tiempo real (Micrófono + Audio Interno) con cálculo de suma
 *    saturada de 16 bits para evitar distorsión o saturación acústica (ideal para gaming y streams).
 * 4. "mute": Sin captura de audio.
 *
 * CODIFICACIÓN:
 * - Codifica los datos PCM 16-bit a formato AAC-LC estéreo (44.1 kHz, 128 kbps) mediante 'MediaCodec' por hardware.
 * - Escribe el flujo AAC en un archivo contenedor M4A mediante 'MediaMuxer'.
 * - Ejecución multihilo en prioridad URGENT_AUDIO fuera del hilo de la interfaz de usuario.
 */
class AudioRecordEngine(
    private val context: Context,
    private val audioSource: String,
    private val outputM4aPath: String,
    private val mediaProjection: MediaProjection?
) {

    companion object {
        private const val TAG = "VeloRec_AudioEngine"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 2
        private const val BITRATE = 128_000 // 128 kbps AAC
        private const val TIMEOUT_US = 10_000L
    }

    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private var workerThread: Thread? = null

    private var micRecord: AudioRecord? = null
    private var internalRecord: AudioRecord? = null

    private var audioEncoder: MediaCodec? = null
    private var audioMuxer: MediaMuxer? = null
    private var audioTrackIndex: Int = -1
    private var isMuxerStarted: Boolean = false

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

        workerThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runAudioLoop()
        }, "VeloRecAudioWorker").apply {
            start()
        }

        Log.i(TAG, "Motor de audio iniciado con modo: '$audioSource' -> $outputM4aPath")
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
            workerThread?.join(1500)
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

        // 1. Inicializar AudioRecord para Micrófono
        if (shouldCaptureMic) {
            try {
                micRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 2
                )
                if (micRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "No se pudo iniciar micrófono en estéreo, reintentando en mono...")
                    micRecord?.release()
                    micRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize * 2
                    )
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

                internalRecord = AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(playbackConfig)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize * 2)
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
            // Asegurar directorio contenedor
            val outputFile = File(outputM4aPath)
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) outputFile.delete()

            // Formato AAC-LC
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            audioMuxer = MediaMuxer(outputM4aPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
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
        val muxer = audioMuxer ?: return

        try {
            micRecord?.startRecording()
            internalRecord?.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error al arrancar AudioRecord: ${e.message}", e)
        }

        val samplesPerRead = 1024 * CHANNEL_COUNT
        val micBuffer = ShortArray(samplesPerRead)
        val internalBuffer = ShortArray(samplesPerRead)
        val mixedBuffer = ShortArray(samplesPerRead)
        val pcmByteBuffer = ByteBuffer.allocateDirect(samplesPerRead * 2)

        val bufferInfo = MediaCodec.BufferInfo()
        var presentationTimeUs = 0L
        val timeIncrementUs = (1024L * 1_000_000L) / SAMPLE_RATE

        while (isRecording.get()) {
            if (isPaused.get()) {
                try {
                    Thread.sleep(20)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            var micReadCount = 0
            var intReadCount = 0

            if (micRecord != null) {
                micReadCount = micRecord?.read(micBuffer, 0, samplesPerRead) ?: 0
            }

            if (internalRecord != null) {
                intReadCount = internalRecord?.read(internalBuffer, 0, samplesPerRead) ?: 0
            }

            val readCount = maxOf(micReadCount, intReadCount).coerceAtLeast(0)
            if (readCount <= 0) {
                try {
                    Thread.sleep(5)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            // Mezcla de Audio en PCM 16-Bit con suma saturada (Clamping anti-distorsión)
            for (i in 0 until readCount) {
                val micSample = if (i < micReadCount) micBuffer[i].toInt() else 0
                val intSample = if (i < intReadCount) internalBuffer[i].toInt() else 0
                val mixed = (micSample + intSample).coerceIn(-32768, 32767)
                mixedBuffer[i] = mixed.toShort()
            }

            // Transferir a ByteBuffer PCM
            pcmByteBuffer.clear()
            for (i in 0 until readCount) {
                pcmByteBuffer.putShort(mixedBuffer[i])
            }
            pcmByteBuffer.flip()
            val pcmBytesLength = readCount * 2

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
                presentationTimeUs += timeIncrementUs
            }

            // Drenar paquetes codificados hacia el Muxer
            drainEncoder(encoder, muxer, bufferInfo, endOfStream = false)
        }

        // Señal de fin de flujo de audio (EOS)
        val endInputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
        if (endInputIndex >= 0) {
            encoder.queueInputBuffer(endInputIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        drainEncoder(encoder, muxer, bufferInfo, endOfStream = true)
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean
    ) {
        while (true) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted) {
                    Log.w(TAG, "Formato cambiado posterior al inicio del Muxer.")
                } else {
                    val newFormat = encoder.outputFormat
                    audioTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    isMuxerStarted = true
                    Log.i(TAG, "Muxer de audio iniciado. Pista asignada: $audioTrackIndex")
                }
            } else if (outputBufferIndex >= 0) {
                val encodedData = encoder.getOutputBuffer(outputBufferIndex)

                if (encodedData != null && bufferInfo.size > 0 && isMuxerStarted) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
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
