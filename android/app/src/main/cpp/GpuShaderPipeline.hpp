#pragma once
/**
 * GpuShaderPipeline.hpp (C++23)
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Define la tubería de aceleración de sombreado en GPU (GLSL y SPIR-V) para VeloRec Studio:
 * 1. Abstracción dual: Soporte para shaders de texto GLSL (OpenGL ES 3.0+) y bytecode SPIR-V (Vulkan).
 * 2. C++23 moderno con 'std::expected' para gestión de errores de coste cero (Zero Overhead) sin excepciones.
 * 3. Gestión de efectos en tiempo real:
 *    - FaceCam circular con máscara antialiased y Chroma Key en GPU.
 *    - Realce de contraste y saturación para juegos móviles (Gaming Color Boost).
 */

#include <string>
#include <string_view>
#include <expected>
#include <span>
#include <cstdint>
#include <GLES3/gl3.h>
#include <EGL/egl.h>

namespace velorec::gpu {

enum class ShaderFormat {
    GLSL_ES32,  // Shaders en texto plano GLSL ES 3.20 (OpenGL ES)
    SPIR_V      // Shaders en bytecode compilado binario SPIR-V (Vulkan / glShaderBinary)
};

enum class ShaderEffectType {
    PASSTHROUGH,
    FACECAM_CIRCULAR,
    GAMING_COLOR_BOOST
};

struct FaceCamConfig {
    float borderWidth = 0.03f;
    float borderColorR = 0.0f;
    float borderColorG = 0.85f;
    float borderColorB = 1.0f;
    float borderColorA = 1.0f;
    bool chromaKeyEnabled = false;
    float keyColorR = 0.0f;
    float keyColorG = 1.0f;
    float keyColorB = 0.0f;
    float similarity = 0.35f;
    float smoothness = 0.10f;
};

struct ColorBoostConfig {
    float saturation = 1.25f; // 1.0 = normal, 1.25 = mejora gaming
    float contrast = 1.12f;
    float brightness = 0.02f;
};

class GpuShaderPipeline {
public:
    GpuShaderPipeline();
    ~GpuShaderPipeline();

    // No copiable para asegurar propiedad única de identificadores OpenGL/Vulkan
    GpuShaderPipeline(const GpuShaderPipeline&) = delete;
    GpuShaderPipeline& operator=(const GpuShaderPipeline&) = delete;

    GpuShaderPipeline(GpuShaderPipeline&& other) noexcept;
    GpuShaderPipeline& operator=(GpuShaderPipeline&& other) noexcept;

    /**
     * Inicializa la tubería de sombreadores con GLSL o SPIR-V
     */
    std::expected<void, std::string> initialize(ShaderFormat format, ShaderEffectType effect);

    /**
     * Configura los parámetros de FaceCam
     */
    void updateFaceCamConfig(const FaceCamConfig& config);

    /**
     * Configura los parámetros de realce de color para juegos
     */
    void updateColorBoostConfig(const ColorBoostConfig& config);

    /**
     * Aplica el programa de sombreado a la superficie gráfica actual
     */
    void bind();
    void unbind();

    [[nodiscard]] bool isReady() const noexcept { return isInitialized_; }
    [[nodiscard]] GLuint getProgramHandle() const noexcept { return programHandle_; }
    [[nodiscard]] ShaderFormat getActiveFormat() const noexcept { return activeFormat_; }

private:
    std::expected<GLuint, std::string> compileGlslShader(GLenum type, std::string_view source);
    std::expected<GLuint, std::string> loadSpirvShader(GLenum type, std::span<const uint32_t> bytecode);

    GLuint programHandle_ = 0;
    GLuint vertexShaderHandle_ = 0;
    GLuint fragmentShaderHandle_ = 0;
    bool isInitialized_ = false;
    ShaderFormat activeFormat_ = ShaderFormat::GLSL_ES32;
    ShaderEffectType activeEffect_ = ShaderEffectType::PASSTHROUGH;

    // Ubicaciones de uniforms en GPU
    GLint uBorderColorLoc_ = -1;
    GLint uBorderWidthLoc_ = -1;
    GLint uChromaKeyEnabledLoc_ = -1;
    GLint uKeyColorLoc_ = -1;
    GLint uSimilarityLoc_ = -1;
    GLint uSmoothnessLoc_ = -1;
    GLint uSaturationLoc_ = -1;
    GLint uContrastLoc_ = -1;
    GLint uBrightnessLoc_ = -1;
};

} // namespace velorec::gpu
