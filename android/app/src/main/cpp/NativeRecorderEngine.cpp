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

std::expected<jobject, std::string> NativeRecorderEngine::initialize(JNIEnv* env) {
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

    // 4. Crear Muxer MP4 de salida
    int fd = open(m_config.outputPath.c_str(), O_CREAT | O_RDWR | O_TRUNC, 0644);
    if (fd < 0) {
        return std::unexpected(std::string("No se pudo abrir el archivo de salida: ") + m_config.outputPath);
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
    m_stopRequested.store(true);
    if (m_workerThread.joinable()) {
        m_workerThread.join();
    }

    if (m_codec) {
        AMediaCodec_signalEndOfInputStream(m_codec);
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
        .isPaused = m_isPaused.load()
    };
}

void NativeRecorderEngine::encodingWorkerLoop() {
    AMediaCodecBufferInfo info;
    constexpr int64_t TIMEOUT_US = 10'000; // 10ms

    while (!m_stopRequested.load() && m_isRecording.load()) {
        if (m_isPaused.load()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(20));
            continue;
        }

        auto startTick = std::chrono::steady_clock::now();

        ssize_t outIndex = AMediaCodec_dequeueOutputBuffer(m_codec, &info, TIMEOUT_US);

        if (outIndex >= 0) {
            // Buffer de salida listo
            size_t bufferSize = 0;
            uint8_t* buffer = AMediaCodec_getOutputBuffer(m_codec, outIndex, &bufferSize);

            if (buffer && info.size > 0 && m_isMuxerStarted.load()) {
                // Escribir datos al archivo contenedor
                AMediaMuxer_writeSampleData(m_muxer, m_videoTrackIndex, buffer, &info);
                m_frameCount.fetch_add(1, std::memory_order_relaxed);
            }

            AMediaCodec_releaseOutputBuffer(m_codec, outIndex, false);

            if ((info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) != 0) {
                break;
            }

            auto endTick = std::chrono::steady_clock::now();
            double latency = std::chrono::duration<double, std::milli>(endTick - startTick).count();
            m_lastLatencyMs.store(latency, std::memory_order_relaxed);

        } else if (outIndex == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
            // El formato de salida ha sido determinado por el encoder de hardware
            AMediaFormat* newFormat = AMediaCodec_getOutputFormat(m_codec);
            m_videoTrackIndex = AMediaMuxer_addTrack(m_muxer, newFormat);
            AMediaMuxer_start(m_muxer);
            m_isMuxerStarted.store(true);
            AMediaFormat_delete(newFormat);
            LOGI("Muxer iniciado con nuevo formato. Pista: %zd", m_videoTrackIndex);
        } else if (outIndex == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
            // No hay datos aún, continuar ciclo
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
    jstring jAudio
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
        .audioSource = audioStr
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
