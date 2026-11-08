/**
 * @file NativeRecorderEngine.hpp
 * @brief Motor de captura y codificación de pantalla de ultra bajo retardo en C++23.
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Declara la clase del motor C++23 que interactúa directamente con el NDK de Android.
 * Utiliza características modernas de C++23:
 * - std::expected para manejo de errores sin excepciones (zero-cost error handling).
 * - std::span para vistas contiguas de memoria sin copia.
 * - std::thread con std::atomic<bool> para gestión concurrente compatible con NDK libc++.
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
    bool isEmmcStorage{false};
    int targetFd{-1}; // Descriptor de archivo directo de MediaStore / Galería
};

struct PacedSample {
    int trackIndex{0}; // 0 = video, 1 = audio
    std::vector<uint8_t> data;
    AMediaCodecBufferInfo info;
};

struct EngineStats {
    uint64_t totalFramesEncoded{0};
    uint64_t droppedFrames{0};
    double averageLatencyMs{0.0};
    float currentFps{0.0f};
    bool isRecording{false};
    bool isPaused{false};
    bool isEmmcPacingActive{false};
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

    // Multiplexación de Audio en Tiempo Real en el mismo AMediaMuxer
    bool addAudioTrack(const uint8_t* csd0Data, size_t csd0Size, int32_t sampleRate, int32_t channelCount);
    void writeAudioSample(const uint8_t* data, size_t size, int64_t ptsUs, int32_t flags);
    [[nodiscard]] bool isAudioTrackAdded() const noexcept { return m_audioTrackIndex >= 0; }

    [[nodiscard]] EngineStats getStats() const noexcept;

    // Detección de almacenamiento eMMC para activación automática de Buffer Pacing
    static bool checkIsEmmcStorage() noexcept;

    // Afinidad de hilos hacia núcleos de eficiencia (LITTLE) para liberar núcleos Big
    static void applyCpuAffinityForEfficiencyCores(const char* threadName) noexcept;

private:
    void encodingWorkerLoop();
    void pacingFlusherLoop();
    void checkAndStartMuxer();

    RecorderConfig m_config;
    std::atomic<bool> m_isRecording{false};
    std::atomic<bool> m_isPaused{false};
    std::atomic<bool> m_stopRequested{false};
    std::atomic<bool> m_isEmmcActive{false};

    AMediaCodec* m_codec{nullptr};
    AMediaMuxer* m_muxer{nullptr};
    AMediaFormat* m_format{nullptr};
    ANativeWindow* m_nativeWindow{nullptr};
    jobject m_surfaceObject{nullptr};

    ssize_t m_videoTrackIndex{-1};
    ssize_t m_audioTrackIndex{-1};
    std::atomic<bool> m_isMuxerStarted{false};
    std::atomic<bool> m_videoFormatReady{false};
    std::atomic<bool> m_audioFormatReady{false};
    std::atomic<bool> m_hasAudio{false};
    std::mutex m_muxerInitMutex;
    std::mutex m_muxerWriteMutex;
    std::vector<PacedSample> m_preStartVideoQueue;

    std::thread m_workerThread;
    std::thread m_pacingThread;
    std::mutex m_pacingMutex;
    std::condition_variable m_pacingCv;
    std::vector<PacedSample> m_pacingQueue;
    std::atomic<bool> m_pacingStopRequested{false};

    std::atomic<int64_t> m_firstFrameTimeUs{-1};
    std::atomic<int64_t> m_lastPresentationTimeUs{-1};
    std::atomic<int64_t> m_lastAudioPtsUs{-1};
    std::atomic<uint64_t> m_frameCount{0};
    std::atomic<uint64_t> m_droppedCount{0};
    std::atomic<double> m_lastLatencyMs{0.0};
};

} // namespace velorec::cpp23
