/**
 * @file NativeRecorderEngine.cpp
 * @brief Implementación del motor de captura y codificación en C++23 con NDK AMediaCodec.
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * 1. Configura el codificador por hardware H.264 / HEVC / AV1 en el Android NDK mediante AMediaCodec.
 * 2. Invoca AMediaCodec_createInputSurface() para generar una ANativeWindow de hardware:
 *    Esta superficie recibe directamente los frames del GPU/Display sin intermediación de Java.
 * 3. Ejecuta un hilo de trabajo con std::thread que lee los buffers de salida (AMediaCodec_dequeueOutputBuffer)
 *    y los escribe en el archivo contenedor MP4 con AMediaMuxer_writeSampleData.
 * 4. Exporta las funciones JNI 'Java_com_velorec_recorder_ScreenRecordService_native*'
 *    para que Kotlin pueda controlarlo instantáneamente.
 */

#include "NativeRecorderEngine.hpp"
#include <chrono>
#include <fcntl.h>
#include <unistd.h>
#include <sys/resource.h>
#include <pthread.h>
#include <sched.h>
#include <vector>

#define LOG_TAG "VeloRec_CPP23"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace velorec::cpp23 {

NativeRecorderEngine::NativeRecorderEngine(const RecorderConfig& config)
    : m_config(config) {
    LOGI("Instanciando NativeRecorderEngine C++23: %dx%d @ %d fps", 
         config.width, config.height, config.fps);
}

NativeRecorderEngine::~NativeRecorderEngine() {
    stop();
    if (m_format) {
        AMediaFormat_delete(m_format);
        m_format = nullptr;
    }
}

bool NativeRecorderEngine::checkIsEmmcStorage() noexcept {
    // 1. Verificación directa de nodos de bloque eMMC estándar en el kernel Linux de Android
    if (access("/sys/block/mmcblk0", F_OK) == 0 || access("/sys/class/block/mmcblk0", F_OK) == 0) {
        LOGI("Almacenamiento eMMC detectado mediante sysfs (/sys/block/mmcblk0).");
        return true;
    }

    // 2. Inspección de particiones activas en /proc/partitions
    int fd = open("/proc/partitions", O_RDONLY);
    if (fd >= 0) {
        char buf[2048];
        ssize_t bytesRead = read(fd, buf, sizeof(buf) - 1);
        close(fd);
        if (bytesRead > 0) {
            buf[bytesRead] = '\0';
            if (std::string_view(buf).find("mmcblk0") != std::string_view::npos) {
                LOGI("Almacenamiento eMMC detectado en /proc/partitions.");
                return true;
            }
        }
    }

    LOGI("Almacenamiento de alta velocidad UFS / NVMe detectado (sin mmcblk0).");
    return false;
}

void NativeRecorderEngine::applyCpuAffinityForEfficiencyCores(const char* threadName) noexcept {
    if (threadName) {
        pthread_setname_np(pthread_self(), threadName);
    }

    int totalCores = static_cast<int>(sysconf(_SC_NPROCESSORS_CONF));
    if (totalCores <= 1) {
        return; // Dispositivo mononúcleo, no hay afinidad aplicable
    }

    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);

    // Intentar leer las frecuencias máximas de los núcleos en sysfs para distinguir LITTLE vs big
    int efficiencyCoresFound = 0;
    int maxFreqGlobal = 0;
    std::vector<int> coreFreqs(totalCores, 0);

    for (int i = 0; i < totalCores; ++i) {
        char path[128];
        snprintf(path, sizeof(path), "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", i);
        int fd = open(path, O_RDONLY);
        if (fd >= 0) {
            char buf[32];
            ssize_t bytes = read(fd, buf, sizeof(buf) - 1);
            close(fd);
            if (bytes > 0) {
                buf[bytes] = '\0';
                coreFreqs[i] = atoi(buf);
                if (coreFreqs[i] > maxFreqGlobal) {
                    maxFreqGlobal = coreFreqs[i];
                }
            }
        }
    }

    if (maxFreqGlobal > 0) {
        // Asignar afinidad exclusivamente a los núcleos con menor frecuencia (LITTLE cluster)
        for (int i = 0; i < totalCores; ++i) {
            if (coreFreqs[i] > 0 && coreFreqs[i] < maxFreqGlobal) {
                CPU_SET(i, &cpuset);
                efficiencyCoresFound++;
            }
        }
    }

    // Estrategia de respaldo determinista para SoCs big.LITTLE (ej: Helio G81 / G85 / G88, Snapdragon):
    if (efficiencyCoresFound == 0) {
        int efficiencyLimit = (totalCores >= 8) ? 4 : (totalCores / 2);
        for (int i = 0; i < efficiencyLimit; ++i) {
            CPU_SET(i, &cpuset);
        }
        efficiencyCoresFound = efficiencyLimit;
    }

    int result = sched_setaffinity(0, sizeof(cpu_set_t), &cpuset);
    if (result == 0) {
        LOGI("Afinidad de hilos aplicada exitosamente para [%s]: %d nucleos LITTLE asignados de %d totales.",
             threadName ? threadName : "Hilo", efficiencyCoresFound, totalCores);
    } else {
        LOGI("Nota de afinidad: sched_setaffinity no restringido por politicas del kernel (status %d).", result);
    }
}

void NativeRecorderEngine::checkAndStartMuxer() {
    std::lock_guard<std::mutex> lock(m_muxerInitMutex);
    if (m_isMuxerStarted.load()) return;

    bool canStart = m_videoFormatReady.load() && (!m_hasAudio.load() || m_audioFormatReady.load());
    if (canStart) {
        media_status_t status = AMediaMuxer_start(m_muxer);
        if (status == AMEDIA_OK) {
            m_isMuxerStarted.store(true);
            LOGI("AMediaMuxer iniciado exitosamente con pistas: video=%zd, audio=%zd",
                 m_videoTrackIndex, m_audioTrackIndex);

            // Vaciar cola pre-inicio de video si había muestras esperando al inicio del audio
            std::lock_guard<std::mutex> writeLock(m_muxerWriteMutex);
            for (const auto& sample : m_preStartVideoQueue) {
                if (m_isEmmcActive.load()) {
                    std::lock_guard<std::mutex> pLock(m_pacingMutex);
                    m_pacingQueue.push_back(sample);
                } else {
                    AMediaMuxer_writeSampleData(m_muxer, sample.trackIndex, sample.data.data(), &sample.info);
                }
            }
            if (m_isEmmcActive.load() && !m_preStartVideoQueue.empty()) {
                m_pacingCv.notify_one();
            }
            m_preStartVideoQueue.clear();
        } else {
            LOGE("Fallo al iniciar AMediaMuxer (status=%d)", static_cast<int>(status));
        }
    }
}

bool NativeRecorderEngine::addAudioTrack(const uint8_t* csd0Data, size_t csd0Size, int32_t sampleRate, int32_t channelCount) {
    std::lock_guard<std::mutex> lock(m_muxerInitMutex);
    if (m_isMuxerStarted.load() || m_audioTrackIndex >= 0 || !m_muxer) {
        return false;
    }

    AMediaFormat* audioFormat = AMediaFormat_new();
    AMediaFormat_setString(audioFormat, AMEDIAFORMAT_KEY_MIME, "audio/mp4a-latm");
    AMediaFormat_setInt32(audioFormat, AMEDIAFORMAT_KEY_SAMPLE_RATE, sampleRate > 0 ? sampleRate : 44100);
    AMediaFormat_setInt32(audioFormat, AMEDIAFORMAT_KEY_CHANNEL_COUNT, channelCount > 0 ? channelCount : 2);
    AMediaFormat_setInt32(audioFormat, AMEDIAFORMAT_KEY_BIT_RATE, 128000);

    if (csd0Data && csd0Size > 0) {
        AMediaFormat_setBuffer(audioFormat, "csd-0", const_cast<uint8_t*>(csd0Data), csd0Size);
    }

    m_audioTrackIndex = AMediaMuxer_addTrack(m_muxer, audioFormat);
    AMediaFormat_delete(audioFormat);

    if (m_audioTrackIndex >= 0) {
        m_audioFormatReady.store(true);
        LOGI("Pista de audio AAC registrada en AMediaMuxer: índice=%zd, %d Hz, %d canales",
             m_audioTrackIndex, sampleRate, channelCount);
        checkAndStartMuxer();
        return true;
    } else {
        LOGE("Fallo al añadir pista de audio al AMediaMuxer");
        return false;
    }
}

void NativeRecorderEngine::writeAudioSample(const uint8_t* data, size_t size, int64_t ptsUs, int32_t flags) {
    if (!data || size == 0 || !m_muxer || m_audioTrackIndex < 0) {
        return;
    }
    if (m_isPaused.load()) return;

    AMediaCodecBufferInfo info;
    info.offset = 0;
    info.size = static_cast<int32_t>(size);
    info.presentationTimeUs = ptsUs;
    info.flags = flags;

    // Normalizar PTS de audio con el tiempo base de video para estricta sincronización
    int64_t firstTime = m_firstFrameTimeUs.load();
    if (firstTime > 0) {
        if (info.presentationTimeUs >= firstTime) {
            info.presentationTimeUs -= firstTime;
        } else {
            info.presentationTimeUs = 0;
        }
    }

    // Monotonicidad estricta para audio en el contenedor MP4
    int64_t lastPts = m_lastAudioPtsUs.load();
    if (info.presentationTimeUs <= lastPts) {
        info.presentationTimeUs = (lastPts >= 0) ? (lastPts + 1000) : 0;
    }
    m_lastAudioPtsUs.store(info.presentationTimeUs);

    if (m_isEmmcActive.load()) {
        PacedSample sample;
        sample.trackIndex = static_cast<int>(m_audioTrackIndex);
        sample.data.assign(data, data + size);
        sample.info = info;
        {
            std::lock_guard<std::mutex> pLock(m_pacingMutex);
            m_pacingQueue.push_back(std::move(sample));
        }
        m_pacingCv.notify_one();
    } else {
        std::lock_guard<std::mutex> writeLock(m_muxerWriteMutex);
        if (m_isMuxerStarted.load()) {
            AMediaMuxer_writeSampleData(m_muxer, m_audioTrackIndex, data, &info);
        }
    }
}

std::expected<jobject, std::string> NativeRecorderEngine::initialize(JNIEnv* env) {
    // 0. Detección de tecnología de almacenamiento y soporte de audio
    bool isEmmc = m_config.isEmmcStorage || checkIsEmmcStorage();
    m_isEmmcActive.store(isEmmc);
    m_hasAudio.store(m_config.audioSource != "mute");
    m_videoFormatReady.store(false);
    m_audioFormatReady.store(false);
    m_isMuxerStarted.store(false);

    LOGI("Detección de almacenamiento: eMMC=%s | Audio habilitado=%s", 
         isEmmc ? "SI (Buffer Pacing)" : "NO (Directo)",
         m_hasAudio.load() ? "SI" : "NO");

    // 1. Crear formato de video para el encoder
    m_format = AMediaFormat_new();
    AMediaFormat_setString(m_format, AMEDIAFORMAT_KEY_MIME, m_config.codecMime.c_str());
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_WIDTH, m_config.width);
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_HEIGHT, m_config.height);
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_BIT_RATE, m_config.bitrateBps);
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_FRAME_RATE, m_config.fps);
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_I_FRAME_INTERVAL, 1); // 1 segundo entre Keyframes
    // Modo de color Surface (COLOR_FormatSurface = 0x7f000789)
    AMediaFormat_setInt32(m_format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 0x7f000789);
    // Repetición automática de cuadro si la pantalla está estática (evita videos congelados)
    int64_t repeatIntervalUs = (m_config.fps > 0) ? (1'000'000LL / m_config.fps) : 16'666LL;
    AMediaFormat_setInt64(m_format, "repeat-previous-frame-after", repeatIntervalUs);

    // 2. Crear codificador por hardware según el MIME especificado
    m_codec = AMediaCodec_createEncoderByType(m_config.codecMime.c_str());
    if (!m_codec) {
        return std::unexpected(std::string("No se pudo crear AMediaCodec para el codec ") + m_config.codecMime);
    }

    media_status_t status = AMediaCodec_configure(
        m_codec,
        m_format,
        nullptr,
        nullptr,
        AMEDIACODEC_CONFIGURE_FLAG_ENCODE
    );

    if (status != AMEDIA_OK) {
        return std::unexpected(std::string("Error al configurar AMediaCodec: ") + std::to_string(static_cast<int>(status)));
    }

    // 3. Crear Input Surface de hardware
    status = AMediaCodec_createInputSurface(m_codec, &m_nativeWindow);
    if (status != AMEDIA_OK || !m_nativeWindow) {
        return std::unexpected("Fallo al crear la ANativeWindow Input Surface de hardware.");
    }

    // 4. Crear Muxer MP4 de salida: Usar targetFd de MediaStore si se proveyó, o abrir archivo
    int fd = -1;
    if (m_config.targetFd >= 0) {
        fd = dup(m_config.targetFd);
        LOGI("AMediaMuxer enlazado a descriptor directo de MediaStore / Galería: targetFd=%d -> fd=%d",
             m_config.targetFd, fd);
    } else {
        fd = open(m_config.outputPath.c_str(), O_CREAT | O_RDWR | O_TRUNC, 0644);
        LOGI("AMediaMuxer enlazado a ruta de archivo: %s", m_config.outputPath.c_str());
    }

    if (fd < 0) {
        return std::unexpected(std::string("No se pudo abrir el descriptor/archivo de salida: ") + m_config.outputPath);
    }

    m_muxer = AMediaMuxer_new(fd, AMEDIAMUXER_OUTPUT_FORMAT_MPEG_4);
    close(fd); // AMediaMuxer duplica el descriptor internamente

    if (!m_muxer) {
        return std::unexpected("No se pudo instanciar AMediaMuxer.");
    }

    // 5. Convertir ANativeWindow a jobject android.view.Surface para Java/Kotlin
    m_surfaceObject = ANativeWindow_toSurface(env, m_nativeWindow);
    if (!m_surfaceObject) {
        return std::unexpected("No se pudo convertir ANativeWindow a android.view.Surface JNI.");
    }

    LOGI("Inicialización de C++23 completada con éxito. Surface lista.");
    return m_surfaceObject;
}

std::expected<void, std::string> NativeRecorderEngine::start() {
    if (!m_codec) return std::unexpected("Codec no inicializado.");
    
    media_status_t status = AMediaCodec_start(m_codec);
    if (status != AMEDIA_OK) {
        return std::unexpected(std::string("Fallo al iniciar AMediaCodec: ") + std::to_string(static_cast<int>(status)));
    }

    m_isRecording.store(true);
    m_isPaused.store(false);
    m_stopRequested.store(false);

    // Si eMMC está activo, iniciar el hilo desacoplado de Buffer Pacing
    if (m_isEmmcActive.load()) {
        m_pacingStopRequested.store(false);
        m_pacingThread = std::thread([this]() {
            this->pacingFlusherLoop();
        });
        LOGI("C++23: Hilo de Buffer Pacing Asíncrono para eMMC lanzado.");
    }

    // Iniciar hilo worker con std::thread compatible con NDK libc++
    m_workerThread = std::thread([this]() {
        this->encodingWorkerLoop();
    });

    LOGI("Grabación C++23 iniciada en segundo plano.");
    return {};
}

void NativeRecorderEngine::pause() {
    m_isPaused.store(true);
    LOGI("Codificación C++23 pausada.");
}

void NativeRecorderEngine::resume() {
    m_isPaused.store(false);
    LOGI("Codificación C++23 reanudada.");
}

std::expected<void, std::string> NativeRecorderEngine::stop() {
    if (!m_isRecording.load()) return {};

    m_isRecording.store(false);

    // 1. Señalizar el fin de la transmisión de entrada al encoder de hardware
    // Esto hace que el hardware termine de codificar los fotogramas pendientes y emita el flag EOS
    if (m_codec) {
        AMediaCodec_signalEndOfInputStream(m_codec);
    }

    // 2. Notificar al hilo de trabajo para que termine una vez drenados los fotogramas
    m_stopRequested.store(true);
    if (m_workerThread.joinable()) {
        m_workerThread.join();
    }

    // 3. Finalizar y vaciar el hilo de Buffer Pacing de eMMC
    if (m_isEmmcActive.load()) {
        m_pacingStopRequested.store(true);
        m_pacingCv.notify_all();
        if (m_pacingThread.joinable()) {
            m_pacingThread.join();
        }
    }

    if (m_codec) {
        AMediaCodec_stop(m_codec);
        AMediaCodec_delete(m_codec);
        m_codec = nullptr;
    }

    if (m_nativeWindow) {
        ANativeWindow_release(m_nativeWindow);
        m_nativeWindow = nullptr;
    }

    if (m_muxer) {
        if (m_isMuxerStarted.load()) {
            AMediaMuxer_stop(m_muxer);
        }
        AMediaMuxer_delete(m_muxer);
        m_muxer = nullptr;
    }

    LOGI("Grabación C++23 finalizada y recursos liberados limpiamente.");
    return {};
}

EngineStats NativeRecorderEngine::getStats() const noexcept {
    return EngineStats{
        .totalFramesEncoded = m_frameCount.load(),
        .droppedFrames = m_droppedCount.load(),
        .averageLatencyMs = m_lastLatencyMs.load(),
        .currentFps = static_cast<float>(m_config.fps),
        .isRecording = m_isRecording.load(),
        .isPaused = m_isPaused.load(),
        .isEmmcPacingActive = m_isEmmcActive.load()
    };
}

void NativeRecorderEngine::pacingFlusherLoop() {
    // Asignar afinidad hacia núcleos de eficiencia LITTLE
    applyCpuAffinityForEfficiencyCores("VeloRec_Pacing");
    setpriority(PRIO_PROCESS, 0, 5); // Prioridad de fondo para coalescing de I/O

    std::vector<PacedSample> localBatch;
    localBatch.reserve(16);

    while (true) {
        {
            std::unique_lock<std::mutex> lock(m_pacingMutex);
            m_pacingCv.wait_for(lock, std::chrono::milliseconds(10), [this]() {
                return !m_pacingQueue.empty() || m_pacingStopRequested.load();
            });

            if (m_pacingQueue.empty() && m_pacingStopRequested.load()) {
                break;
            }

            // Drenar lote de hasta 16 muestras amortiguadas
            size_t batchCount = std::min(m_pacingQueue.size(), static_cast<size_t>(16));
            if (batchCount > 0) {
                localBatch.assign(
                    std::make_move_iterator(m_pacingQueue.begin()),
                    std::make_move_iterator(m_pacingQueue.begin() + batchCount)
                );
                m_pacingQueue.erase(m_pacingQueue.begin(), m_pacingQueue.begin() + batchCount);
            }
        }

        // Escritura asíncrona por lotes (coalescing) al muxer
        for (auto& sample : localBatch) {
            if (m_muxer && m_isMuxerStarted.load()) {
                std::lock_guard<std::mutex> lock(m_muxerWriteMutex);
                AMediaMuxer_writeSampleData(m_muxer, sample.trackIndex, sample.data.data(), &sample.info);
            }
        }
        localBatch.clear();
    }

    // Drenar remanentes al finalizar
    {
        std::lock_guard<std::mutex> lock(m_pacingMutex);
        for (auto& sample : m_pacingQueue) {
            if (m_muxer && m_isMuxerStarted.load()) {
                std::lock_guard<std::mutex> wLock(m_muxerWriteMutex);
                AMediaMuxer_writeSampleData(m_muxer, sample.trackIndex, sample.data.data(), &sample.info);
            }
        }
        m_pacingQueue.clear();
    }

    LOGI("C++23: Hilo de Buffer Pacing finalizado con éxito.");
}

void NativeRecorderEngine::encodingWorkerLoop() {
    // 1. Aplicar afinidad de hilos a núcleos LITTLE para dejar libres los núcleos grandes para el juego
    applyCpuAffinityForEfficiencyCores("VeloRec_CppEncoder");

    // 2. Configurar prioridad de tiempo real para el hilo de codificación NDK en Linux/Android:
    setpriority(PRIO_PROCESS, 0, -16);

    // 3. Intentar asignar política de tiempo real Round-Robin (SCHED_RR) si las capacidades del sistema lo permiten
    struct sched_param schedParam{};
    schedParam.sched_priority = sched_get_priority_max(SCHED_RR);
    pthread_setschedparam(pthread_self(), SCHED_RR, &schedParam);

    AMediaCodecBufferInfo info;
    constexpr int64_t TIMEOUT_US = 10'000; // 10ms
    int drainRetries = 0;
    constexpr int MAX_DRAIN_RETRIES = 50; // Hasta 500ms tras stopRequested para vaciar el encoder

    while (true) {
        if (m_isPaused.load() && !m_stopRequested.load()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(20));
            continue;
        }

        auto startTick = std::chrono::steady_clock::now();

        ssize_t outIndex = AMediaCodec_dequeueOutputBuffer(m_codec, &info, TIMEOUT_US);

        if (outIndex >= 0) {
            drainRetries = 0; // Datos recibidos, reiniciar contador de espera
            size_t bufferSize = 0;
            uint8_t* buffer = AMediaCodec_getOutputBuffer(m_codec, outIndex, &bufferSize);

            if (buffer && info.size > 0) {
                // Descartar buffers de configuración (SPS/PPS) en writeSampleData
                // AMediaMuxer ya los tiene en el MediaFormat de addTrack
                if ((info.flags & 2) != 0) { // 2 = AMEDIACODEC_BUFFER_FLAG_CODEC_CONFIG
                    LOGI("C++23: Buffer CODEC_CONFIG omitido en muxer");
                } else {
                    // Sincronización y normalización de PTS a tiempo base cero
                    if (m_firstFrameTimeUs.load() < 0) {
                        m_firstFrameTimeUs.store(info.presentationTimeUs);
                    }
                    int64_t firstTime = m_firstFrameTimeUs.load();
                    if (info.presentationTimeUs >= firstTime) {
                        info.presentationTimeUs -= firstTime;
                    } else {
                        info.presentationTimeUs = 0;
                    }

                    // Garantizar estricta monotonicidad creciente en las marcas de tiempo para AMediaMuxer
                    int64_t lastPts = m_lastPresentationTimeUs.load();
                    if (info.presentationTimeUs <= lastPts) {
                        info.presentationTimeUs = (lastPts >= 0) ? (lastPts + 1000) : 0;
                    }
                    m_lastPresentationTimeUs.store(info.presentationTimeUs);

                    // Si el muxer aún no ha arrancado (esperando pista de audio)
                    if (!m_isMuxerStarted.load()) {
                        PacedSample preSample;
                        preSample.trackIndex = static_cast<int>(m_videoTrackIndex);
                        preSample.data.assign(buffer, buffer + info.size);
                        preSample.info = info;
                        {
                            std::lock_guard<std::mutex> lock(m_muxerInitMutex);
                            m_preStartVideoQueue.push_back(std::move(preSample));
                            if (m_preStartVideoQueue.size() >= 30) {
                                LOGI("Audio track no recibido tras 30 cuadros; forzando arranque de muxer solo-video.");
                                m_hasAudio.store(false);
                            }
                        }
                        checkAndStartMuxer();
                    } else if (m_isEmmcActive.load()) {
                        PacedSample sample;
                        sample.trackIndex = static_cast<int>(m_videoTrackIndex);
                        sample.data.assign(buffer, buffer + info.size);
                        sample.info = info;
                        {
                            std::lock_guard<std::mutex> lock(m_pacingMutex);
                            m_pacingQueue.push_back(std::move(sample));
                        }
                        m_pacingCv.notify_one();
                    } else {
                        std::lock_guard<std::mutex> lock(m_muxerWriteMutex);
                        AMediaMuxer_writeSampleData(m_muxer, m_videoTrackIndex, buffer, &info);
                    }
                    m_frameCount.fetch_add(1, std::memory_order_relaxed);
                }
            }

            AMediaCodec_releaseOutputBuffer(m_codec, outIndex, false);

            if ((info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) != 0) {
                LOGI("C++23: Señal EOS recibida y procesada en el Muxer.");
                break;
            }

            auto endTick = std::chrono::steady_clock::now();
            double latency = std::chrono::duration<double, std::milli>(endTick - startTick).count();
            m_lastLatencyMs.store(latency, std::memory_order_relaxed);

        } else if (outIndex == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
            // El formato de salida ha sido determinado por el encoder de hardware
            AMediaFormat* newFormat = AMediaCodec_getOutputFormat(m_codec);
            m_videoTrackIndex = AMediaMuxer_addTrack(m_muxer, newFormat);
            m_videoFormatReady.store(true);
            AMediaFormat_delete(newFormat);
            LOGI("Pista de video registrada en C++23: índice=%zd", m_videoTrackIndex);
            checkAndStartMuxer();
        } else if (outIndex == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
            if (m_stopRequested.load()) {
                drainRetries++;
                if (drainRetries >= MAX_DRAIN_RETRIES) {
                    LOGI("C++23: Drenado completado tras stopRequested (%d reintentos).", drainRetries);
                    break;
                }
            }
        }
    }
}

} // namespace velorec::cpp23

// =========================================================================
// ENLACES JNI PARA COMUNICACIÓN CON KOTLIN (ScreenRecordService)
// =========================================================================

static std::unique_ptr<velorec::cpp23::NativeRecorderEngine> g_cppEngine = nullptr;

extern "C" {

JNIEXPORT jobject JNICALL
Java_com_velorec_recorder_ScreenRecordService_nativeInitCppRecorder(
    JNIEnv* env,
    jobject /* thiz */,
    jstring jPath,
    jint width,
    jint height,
    jint fps,
    jint bitrate,
    jstring jCodec,
    jstring jAudio,
    jint targetFd
) {
    const char* pathStr = env->GetStringUTFChars(jPath, nullptr);
    const char* codecStr = env->GetStringUTFChars(jCodec, nullptr);
    const char* audioStr = env->GetStringUTFChars(jAudio, nullptr);

    std::string codecMime = "video/avc";
    if (std::string_view(codecStr) == "HEVC") {
        codecMime = "video/hevc";
    } else if (std::string_view(codecStr) == "AV1") {
        codecMime = "video/av01";
    }

    velorec::cpp23::RecorderConfig config{
        .outputPath = pathStr,
        .width = width,
        .height = height,
        .fps = fps,
        .bitrateBps = bitrate,
        .codecMime = codecMime,
        .audioSource = audioStr,
        .targetFd = targetFd
    };

    env->ReleaseStringUTFChars(jPath, pathStr);
    env->ReleaseStringUTFChars(jCodec, codecStr);
    env->ReleaseStringUTFChars(jAudio, audioStr);

    g_cppEngine = std::make_unique<velorec::cpp23::NativeRecorderEngine>(config);
    auto initResult = g_cppEngine->initialize(env);

    if (!initResult.has_value()) {
        jclass excClass = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(excClass, initResult.error().c_str());
        return nullptr;
    }

    return initResult.value();
}

JNIEXPORT jboolean JNICALL
Java_com_velorec_recorder_ScreenRecordService_nativeAddAudioTrack(
    JNIEnv* env,
    jobject /* thiz */,
    jobject jCsdBuffer,
    jint csdSize,
    jint sampleRate,
    jint channelCount
) {
    if (!g_cppEngine) return JNI_FALSE;
    const uint8_t* csdData = nullptr;
    if (jCsdBuffer && csdSize > 0) {
        csdData = static_cast<const uint8_t*>(env->GetDirectBufferAddress(jCsdBuffer));
    }
    bool success = g_cppEngine->addAudioTrack(csdData, static_cast<size_t>(csdSize), sampleRate, channelCount);
    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_velorec_recorder_ScreenRecordService_nativeWriteAudioSample(
    JNIEnv* env,
    jobject /* thiz */,
    jobject jBuffer,
    jint offset,
    jint size,
    jlong ptsUs,
    jint flags
) {
    if (!g_cppEngine || !jBuffer || size <= 0) return;
    uint8_t* basePtr = static_cast<uint8_t*>(env->GetDirectBufferAddress(jBuffer));
    if (basePtr) {
        g_cppEngine->writeAudioSample(basePtr + offset, static_cast<size_t>(size), ptsUs, flags);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_velorec_recorder_ScreenRecordService_nativeIsAudioTrackAdded(
    JNIEnv* /* env */,
    jobject /* thiz */
) {
    return (g_cppEngine && g_cppEngine->isAudioTrackAdded()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_velorec_recorder_ScreenRecordService_nativeStartCppEncoding(JNIEnv* env, jobject /* thiz */) {
    if (g_cppEngine) {
        auto res = g_cppEngine->start();
        if (!res.has_value()) {
            jclass excClass = env->FindClass("java/lang/RuntimeException");
            env->ThrowNew(excClass, res.error().c_str());
        }
    }
}

JNIEXPORT void JNICALL
Java_com_velorec_recorder_ScreenRecordService_nativePauseCppEncoding(JNIEnv* /* env */, jobject /* thiz */) {
    if (g_cppEngine) {
        g_cppEngine->pause();
    }
}

JNIEXPORT void JNICALL
Java_com_velorec_recorder_ScreenRecordService_nativeResumeCppEncoding(JNIEnv* /* env */, jobject /* thiz */) {
    if (g_cppEngine) {
        g_cppEngine->resume();
    }
}

JNIEXPORT void JNICALL
Java_com_velorec_recorder_ScreenRecordService_nativeStopCppEncoding(JNIEnv* /* env */, jobject /* thiz */) {
    if (g_cppEngine) {
        g_cppEngine->stop();
        g_cppEngine.reset();
    }
}

} // extern "C"
