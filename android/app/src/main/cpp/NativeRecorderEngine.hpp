/**
 * @file NativeRecorderEngine.hpp
 * @brief Motor de captura y codificación de pantalla de ultra bajo retardo en C++23.
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Declara la clase del motor C++23 que interactúa directamente con el NDK de Android.
 * Utiliza características modernas de C++23:
 * - std::expected para manejo de errores sin excepciones (zero-cost error handling).
 * - std::span para vistas contiguas de memoria sin copia.
 * - std::jthread con std::stop_token para gestión cooperativa de hilos.
 * - std::format para formateo seguro de cadenas y telemetría.
 * - Interfaz directa con AMediaCodec y AMediaMuxer del Android NDK.
 */

#pragma once

#include <string>
#include <string_view>
#include <expected>
#include <span>
#include <memory>
#include <atomic>
#include <thread>
#include <jni.h>

#include <media/NdkMediaCodec.h>
#include <media/NdkMediaMuxer.h>
#include <media/NdkMediaFormat.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>

namespace velorec::cpp23 {

struct RecorderConfig {
    std::string outputPath;
    int32_t width{1080};
    int32_t height{1920};
    int32_t fps{60};
    int32_t bitrateBps{12'000'000};
    std::string codecMime{"video/avc"}; // "video/avc" (H.264), "video/hevc", "video/av01"
    std::string audioSource{"both"};
};

struct EngineStats {
    uint64_t totalFramesEncoded{0};
    uint64_t droppedFrames{0};
    double averageLatencyMs{0.0};
    float currentFps{0.0f};
    bool isRecording{false};
    bool isPaused{false};
};

class NativeRecorderEngine {
public:
    explicit NativeRecorderEngine(const RecorderConfig& config);
    ~NativeRecorderEngine();

    // Eliminar copia para seguridad de memoria RAII
    NativeRecorderEngine(const NativeRecorderEngine&) = delete;
    NativeRecorderEngine& operator=(const NativeRecorderEngine&) = delete;

    // Inicializa AMediaCodec y retorna la Surface de entrada de hardware para MediaProjection
    std::expected<jobject, std::string> initialize(JNIEnv* env);

    // Control de flujo de codificación
    std::expected<void, std::string> start();
    void pause();
    void resume();
    std::expected<void, std::string> stop();

    [[nodiscard]] EngineStats getStats() const noexcept;

private:
    void encodingWorkerLoop(std::stop_token stopToken);

    RecorderConfig m_config;
    std::atomic<bool> m_isRecording{false};
    std::atomic<bool> m_isPaused{false};

    AMediaCodec* m_codec{nullptr};
    AMediaMuxer* m_muxer{nullptr};
    AMediaFormat* m_format{nullptr};
    ANativeWindow* m_nativeWindow{nullptr};
    jobject m_surfaceObject{nullptr};

    ssize_t m_videoTrackIndex{-1};
    std::atomic<bool> m_isMuxerStarted{false};

    std::jthread m_workerThread;
    std::atomic<uint64_t> m_frameCount{0};
    std::atomic<uint64_t> m_droppedCount{0};
    std::atomic<double> m_lastLatencyMs{0.0};
};

} // namespace velorec::cpp23
