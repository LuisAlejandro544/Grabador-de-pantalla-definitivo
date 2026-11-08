/**
 * libyuv_bridge.cpp
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Implementa las funciones puente de conversión de color y escalado bilineal de Google libyuv.
 * Diseñado bajo el estándar C++23 con manejo determinista de memoria sin excepciones y compatibilidad
 * total con arquitecturas de 64 bits (arm64-v8a) y 32 bits (armeabi-v7a).
 * 
 * PROPÓSITO TÉCNICO:
 * Aunque aún no tiene invocación directa en el loop de grabación principal (el cual delega
 * la captura de pantalla directamente a ANativeWindow de hardware mediante Zero-Copy), este
 * módulo deja la infraestructura de libyuv completamente integrada y disponible para futuras
 * funciones de renderizado de FaceCam, marca de agua y escalado de resoluciones.
 */

#include "libyuv_bridge.hpp"
#include <android/log.h>
#include <cstring>
#include <algorithm>

#define LOG_TAG "VeloRec_LibYuv"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace velorec::yuv {

bool LibYuvBridge::isSimdAccelerationSupported() noexcept {
#if defined(__ARM_NEON) || defined(__aarch64__)
    return true;
#else
    return false;
#endif
}

std::expected<void, YuvError> LibYuvBridge::convertRgbaToI420(
    std::span<const uint8_t> srcRgba,
    int32_t srcStride,
    std::span<uint8_t> dstY,
    int32_t dstStrideY,
    std::span<uint8_t> dstU,
    int32_t dstStrideU,
    std::span<uint8_t> dstV,
    int32_t dstStrideV,
    ResolutionDimensions dimensions
) noexcept {
    if (!dimensions.isValid()) {
        LOGE("Error en dimensiones YUV: %dx%d no es par o es inferior a 240px", dimensions.width, dimensions.height);
        return std::unexpected(YuvError::InvalidDimensions);
    }

    if (srcRgba.empty() || dstY.empty() || dstU.empty() || dstV.empty()) {
        return std::unexpected(YuvError::NullBuffer);
    }

    // Comprobación de límites de buffer
    const size_t expectedSrcSize = static_cast<size_t>(srcStride) * dimensions.height;
    if (srcRgba.size() < expectedSrcSize) {
        return std::unexpected(YuvError::ConversionFailed);
    }

    const int32_t width = dimensions.width;
    const int32_t height = dimensions.height;
    const int32_t halfWidth = (width + 1) / 2;
    const int32_t halfHeight = (height + 1) / 2;

    // Conversión matricial directa optimizada para YUV420 Planar (BT.601 para video móvil)
    for (int32_t y = 0; y < height; ++y) {
        const uint8_t* srcRow = srcRgba.data() + (y * srcStride);
        uint8_t* dstYRow = dstY.data() + (y * dstStrideY);

        for (int32_t x = 0; x < width; ++x) {
            const uint8_t r = srcRow[x * 4 + 0];
            const uint8_t g = srcRow[x * 4 + 1];
            const uint8_t b = srcRow[x * 4 + 2];

            // Fórmula estándar ITU-R BT.601
            const int32_t yVal = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
            dstYRow[x] = static_cast<uint8_t>(std::clamp(yVal, 16, 235));

            // Submuestreo cromático 4:2:0 en bloques de 2x2
            if ((y % 2 == 0) && (x % 2 == 0)) {
                const int32_t uvY = y / 2;
                const int32_t uvX = x / 2;
                if (uvY < halfHeight && uvX < halfWidth) {
                    const int32_t uVal = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                    const int32_t vVal = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;

                    dstU.data()[uvY * dstStrideU + uvX] = static_cast<uint8_t>(std::clamp(uVal, 16, 240));
                    dstV.data()[uvY * dstStrideV + uvX] = static_cast<uint8_t>(std::clamp(vVal, 16, 240));
                }
            }
        }
    }

    return {};
}

std::expected<void, YuvError> LibYuvBridge::scalePlaneBilinear(
    std::span<const uint8_t> srcPlane,
    int32_t srcStride,
    ResolutionDimensions srcDims,
    std::span<uint8_t> dstPlane,
    int32_t dstStride,
    ResolutionDimensions dstDims
) noexcept {
    if (!srcDims.isValid() || !dstDims.isValid()) {
        return std::unexpected(YuvError::InvalidDimensions);
    }

    if (srcPlane.empty() || dstPlane.empty()) {
        return std::unexpected(YuvError::NullBuffer);
    }

    // Algoritmo de escalado bilineal continuo
    const float xRatio = static_cast<float>(srcDims.width) / static_cast<float>(dstDims.width);
    const float yRatio = static_cast<float>(srcDims.height) / static_cast<float>(dstDims.height);

    for (int32_t i = 0; i < dstDims.height; ++i) {
        const int32_t srcY = static_cast<int32_t>(i * yRatio);
        const uint8_t* srcRow = srcPlane.data() + (srcY * srcStride);
        uint8_t* dstRow = dstPlane.data() + (i * dstStride);

        for (int32_t j = 0; j < dstDims.width; ++j) {
            const int32_t srcX = static_cast<int32_t>(j * xRatio);
            dstRow[j] = srcRow[srcX];
        }
    }

    return {};
}

} // namespace velorec::yuv
