#pragma once

/**
 * libyuv_bridge.hpp
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Proporciona la interfaz C++23 de alto rendimiento para la integración con la biblioteca
 * libyuv de Google (Licencia BSD permisiva), acelerada por hardware mediante instrucciones
 * vectoriales SIMD (ARM NEON en arquitecturas de 64 bits 'arm64-v8a' y 32 bits 'armeabi-v7a').
 * 
 * LÓGICA Y PROPÓSITO TÉCNICO:
 * 1. Conversión de formatos de color de coste cero entre YUV420 Planar (I420), Bi-Planar (NV12/NV21)
 *    y empaquetado (RGBA/ARGB) requerido por los codificadores de hardware AMediaCodec de Android.
 * 2. Escalado bilineal por hardware (SIMD) para resoluciones nativas adaptadas y FaceCam flotante
 *    sin sobrecargar la CPU del smartphone.
 * 3. Rotación de fotogramas sin copia adicional de memoria ante giros del dispositivo.
 * 4. Manejo moderno de errores mediante C++23 std::expected y vistas de memoria no propietarias std::span.
 */

#include <cstdint>
#include <span>
#include <expected>
#include <string_view>

namespace velorec::yuv {

enum class YuvError {
    InvalidDimensions,
    NullBuffer,
    OddDimensions,
    ConversionFailed,
    UnsupportedFormat
};

struct ResolutionDimensions {
    int32_t width{0};
    int32_t height{0};

    [[nodiscard]] constexpr bool isValid() const noexcept {
        return width >= 240 && height >= 240 && (width % 2 == 0) && (height % 2 == 0);
    }
};

/**
 * Puente de aceleración libyuv para VeloRec Studio.
 * Actualmente encapsula las funciones de preparación de memoria y conversión rápida.
 */
class LibYuvBridge {
public:
    LibYuvBridge() = default;
    ~LibYuvBridge() = default;

    // No copiable para garantizar seguridad de recursos
    LibYuvBridge(const LibYuvBridge&) = delete;
    LibYuvBridge& operator=(const LibYuvBridge&) = delete;

    // Movible
    LibYuvBridge(LibYuvBridge&&) noexcept = default;
    LibYuvBridge& operator=(LibYuvBridge&&) noexcept = default;

    /**
     * Verifica si la aceleración vectorial NEON está disponible en el hardware del teléfono.
     */
    [[nodiscard]] static bool isSimdAccelerationSupported() noexcept;

    /**
     * Convierte fotogramas RGBA (ej. procedentes de la cámara frontal FaceCam o captura de pantalla)
     * a formato YUV420p (I420) compatible con AMediaCodec.
     */
    [[nodiscard]] static std::expected<void, YuvError> convertRgbaToI420(
        std::span<const uint8_t> srcRgba,
        int32_t srcStride,
        std::span<uint8_t> dstY,
        int32_t dstStrideY,
        std::span<uint8_t> dstU,
        int32_t dstStrideU,
        std::span<uint8_t> dstV,
        int32_t dstStrideV,
        ResolutionDimensions dimensions
    ) noexcept;

    /**
     * Escala un plano de video manteniendo la relación de aspecto nativa del smartphone
     * mediante interpolación bilineal acelerada por hardware.
     */
    [[nodiscard]] static std::expected<void, YuvError> scalePlaneBilinear(
        std::span<const uint8_t> srcPlane,
        int32_t srcStride,
        ResolutionDimensions srcDims,
        std::span<uint8_t> dstPlane,
        int32_t dstStride,
        ResolutionDimensions dstDims
    ) noexcept;
};

} // namespace velorec::yuv
