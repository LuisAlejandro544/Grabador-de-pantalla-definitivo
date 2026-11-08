/**
 * GpuShaderPipeline.cpp (C++23)
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Implementación de la tubería de aceleración gráfica en GPU para VeloRec Studio:
 * 1. Compila y enlaza shaders GLSL ES 3.20 o carga bytecode SPIR-V.
 * 2. Valida el estado de compilación y enlace con diagnóstico de errores.
 * 3. Provee bindings de uniforms para filtros de juegos y máscara circular de FaceCam.
 */

#include "GpuShaderPipeline.hpp"
#include "shaders/SpirvShaderBytecode.hpp"
#include <android/log.h>
#include <utility>
#include <vector>

#define LOG_TAG "VeloRecGpuShader"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace velorec::gpu {

// Fuentes embebidas de respaldo en GLSL ES 3.20
static constexpr std::string_view GLSL_VERT_SOURCE = R"(#version 320 es
layout(location = 0) in vec4 aPosition;
layout(location = 1) in vec2 aTexCoord;
out vec2 vTexCoord;
void main() {
    gl_Position = aPosition;
    vTexCoord = aTexCoord;
}
)";

static constexpr std::string_view GLSL_PASSTHROUGH_FRAG_SOURCE = R"(#version 320 es
precision mediump float;
in vec2 vTexCoord;
out vec4 fragColor;
uniform sampler2D uScreenTexture;
void main() {
    fragColor = texture(uScreenTexture, vTexCoord);
}
)";

static constexpr std::string_view GLSL_FACECAM_FRAG_SOURCE = R"(#version 320 es
precision mediump float;
in vec2 vTexCoord;
out vec4 fragColor;
uniform sampler2D uCameraTexture;
uniform vec4 uBorderColor;
uniform float uBorderWidth;
uniform bool uChromaKeyEnabled;
uniform vec3 uKeyColor;
uniform float uSimilarity;
uniform float uSmoothness;

void main() {
    vec2 uv = vTexCoord - vec2(0.5, 0.5);
    float dist = length(uv);
    float radius = 0.5;
    float innerRadius = radius - uBorderWidth;

    if (dist > radius) discard;

    vec4 color = texture(uCameraTexture, vTexCoord);
    if (uChromaKeyEnabled) {
        float diff = distance(color.rgb, uKeyColor);
        float alpha = smoothstep(uSimilarity, uSimilarity + uSmoothness, diff);
        color.a *= alpha;
        if (color.a < 0.05) discard;
    }

    if (dist > innerRadius) {
        float borderAlpha = smoothstep(radius, radius - 0.015, dist);
        fragColor = mix(uBorderColor, color, smoothstep(innerRadius, innerRadius + 0.015, dist)) * borderAlpha;
    } else {
        float edgeAlpha = smoothstep(radius, radius - 0.01, dist);
        fragColor = vec4(color.rgb, color.a * edgeAlpha);
    }
}
)";

static constexpr std::string_view GLSL_COLOR_BOOST_FRAG_SOURCE = R"(#version 320 es
precision mediump float;
in vec2 vTexCoord;
out vec4 fragColor;
uniform sampler2D uScreenTexture;
uniform float uSaturation;
uniform float uContrast;
uniform float uBrightness;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

void main() {
    vec4 color = texture(uScreenTexture, vTexCoord);
    vec3 rgb = color.rgb + vec3(uBrightness);
    rgb = (rgb - 0.5) * uContrast + 0.5;
    float luminance = dot(rgb, LUMA);
    rgb = mix(vec3(luminance), rgb, uSaturation);
    fragColor = vec4(clamp(rgb, 0.0, 1.0), color.a);
}
)";

GpuShaderPipeline::GpuShaderPipeline() = default;

GpuShaderPipeline::~GpuShaderPipeline() {
    if (programHandle_ != 0) {
        glDeleteProgram(programHandle_);
        programHandle_ = 0;
    }
    if (vertexShaderHandle_ != 0) {
        glDeleteShader(vertexShaderHandle_);
        vertexShaderHandle_ = 0;
    }
    if (fragmentShaderHandle_ != 0) {
        glDeleteShader(fragmentShaderHandle_);
        fragmentShaderHandle_ = 0;
    }
}

GpuShaderPipeline::GpuShaderPipeline(GpuShaderPipeline&& other) noexcept
    : programHandle_(std::exchange(other.programHandle_, 0)),
      vertexShaderHandle_(std::exchange(other.vertexShaderHandle_, 0)),
      fragmentShaderHandle_(std::exchange(other.fragmentShaderHandle_, 0)),
      isInitialized_(std::exchange(other.isInitialized_, false)),
      activeFormat_(other.activeFormat_),
      activeEffect_(other.activeEffect_),
      uBorderColorLoc_(other.uBorderColorLoc_),
      uBorderWidthLoc_(other.uBorderWidthLoc_),
      uChromaKeyEnabledLoc_(other.uChromaKeyEnabledLoc_),
      uKeyColorLoc_(other.uKeyColorLoc_),
      uSimilarityLoc_(other.uSimilarityLoc_),
      uSmoothnessLoc_(other.uSmoothnessLoc_),
      uSaturationLoc_(other.uSaturationLoc_),
      uContrastLoc_(other.uContrastLoc_),
      uBrightnessLoc_(other.uBrightnessLoc_) {}

GpuShaderPipeline& GpuShaderPipeline::operator=(GpuShaderPipeline&& other) noexcept {
    if (this != &other) {
        if (programHandle_ != 0) glDeleteProgram(programHandle_);
        if (vertexShaderHandle_ != 0) glDeleteShader(vertexShaderHandle_);
        if (fragmentShaderHandle_ != 0) glDeleteShader(fragmentShaderHandle_);

        programHandle_ = std::exchange(other.programHandle_, 0);
        vertexShaderHandle_ = std::exchange(other.vertexShaderHandle_, 0);
        fragmentShaderHandle_ = std::exchange(other.fragmentShaderHandle_, 0);
        isInitialized_ = std::exchange(other.isInitialized_, false);
        activeFormat_ = other.activeFormat_;
        activeEffect_ = other.activeEffect_;
        uBorderColorLoc_ = other.uBorderColorLoc_;
        uBorderWidthLoc_ = other.uBorderWidthLoc_;
        uChromaKeyEnabledLoc_ = other.uChromaKeyEnabledLoc_;
        uKeyColorLoc_ = other.uKeyColorLoc_;
        uSimilarityLoc_ = other.uSimilarityLoc_;
        uSmoothnessLoc_ = other.uSmoothnessLoc_;
        uSaturationLoc_ = other.uSaturationLoc_;
        uContrastLoc_ = other.uContrastLoc_;
        uBrightnessLoc_ = other.uBrightnessLoc_;
    }
    return *this;
}

std::expected<GLuint, std::string> GpuShaderPipeline::compileGlslShader(GLenum type, std::string_view source) {
    GLuint shader = glCreateShader(type);
    if (shader == 0) {
        return std::unexpected("No se pudo crear identificador de sombreador OpenGL ES");
    }

    const char* srcPtr = source.data();
    auto srcLen = static_cast<GLint>(source.size());
    glShaderSource(shader, 1, &srcPtr, &srcLen);
    glCompileShader(shader);

    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
        std::string infoLog;
        if (infoLen > 1) {
            infoLog.resize(static_cast<size_t>(infoLen));
            glGetShaderInfoLog(shader, infoLen, nullptr, infoLog.data());
        }
        glDeleteShader(shader);
        return std::unexpected("Error compilando GLSL: " + infoLog);
    }
    return shader;
}

std::expected<GLuint, std::string> GpuShaderPipeline::loadSpirvShader(GLenum type, std::span<const uint32_t> bytecode) {
    GLuint shader = glCreateShader(type);
    if (shader == 0) {
        return std::unexpected("No se pudo crear shader para SPIR-V");
    }

    // glShaderBinary está disponible en OpenGL ES 3.0+ y extensiones SPIR-V
    #if defined(GL_SPIR_V_BINARY_ANGLE) || defined(GL_SHADER_BINARY_FORMAT_SPIR_V)
    glShaderBinary(1, &shader, GL_SHADER_BINARY_FORMAT_SPIR_V, bytecode.data(), static_cast<GLsizei>(bytecode.size_bytes()));
    glSpecializeShader(shader, "main", 0, nullptr, nullptr);
    #else
    LOGI("Aviso: Plataforma no tiene glShaderBinary nativo directo; preparado para tubería Vulkan.");
    #endif

    return shader;
}

std::expected<void, std::string> GpuShaderPipeline::initialize(ShaderFormat format, ShaderEffectType effect) {
    activeFormat_ = format;
    activeEffect_ = effect;

    // 1. Compilar Vertex Shader
    if (format == ShaderFormat::GLSL_ES32) {
        auto vRes = compileGlslShader(GL_VERTEX_SHADER, GLSL_VERT_SOURCE);
        if (!vRes) return std::unexpected(vRes.error());
        vertexShaderHandle_ = *vRes;

        // Seleccionar Fragment Shader según efecto
        std::string_view fSource = GLSL_PASSTHROUGH_FRAG_SOURCE;
        if (effect == ShaderEffectType::FACECAM_CIRCULAR) {
            fSource = GLSL_FACECAM_FRAG_SOURCE;
        } else if (effect == ShaderEffectType::GAMING_COLOR_BOOST) {
            fSource = GLSL_COLOR_BOOST_FRAG_SOURCE;
        }

        auto fRes = compileGlslShader(GL_FRAGMENT_SHADER, fSource);
        if (!fRes) return std::unexpected(fRes.error());
        fragmentShaderHandle_ = *fRes;
    } else {
        // Formato SPIR-V (Vulkan)
        auto vBytes = getSpirvShaderBytecode("passthrough");
        auto vRes = loadSpirvShader(GL_VERTEX_SHADER, vBytes);
        if (!vRes) return std::unexpected(vRes.error());
        vertexShaderHandle_ = *vRes;

        auto fBytes = (effect == ShaderEffectType::FACECAM_CIRCULAR)
            ? getSpirvShaderBytecode("facecam_circular")
            : (effect == ShaderEffectType::GAMING_COLOR_BOOST)
                ? getSpirvShaderBytecode("color_boost")
                : getSpirvShaderBytecode("passthrough");

        auto fRes = loadSpirvShader(GL_FRAGMENT_SHADER, fBytes);
        if (!fRes) return std::unexpected(fRes.error());
        fragmentShaderHandle_ = *fRes;
    }

    // 2. Crear y enlazar programa
    programHandle_ = glCreateProgram();
    glAttachShader(programHandle_, vertexShaderHandle_);
    glAttachShader(programHandle_, fragmentShaderHandle_);
    glLinkProgram(programHandle_);

    GLint linked = 0;
    glGetProgramiv(programHandle_, GL_LINK_STATUS, &linked);
    if (!linked) {
        GLint infoLen = 0;
        glGetProgramiv(programHandle_, GL_INFO_LOG_LENGTH, &infoLen);
        std::string infoLog;
        if (infoLen > 1) {
            infoLog.resize(static_cast<size_t>(infoLen));
            glGetProgramInfoLog(programHandle_, infoLen, nullptr, infoLog.data());
        }
        return std::unexpected("Error enlazando programa GPU: " + infoLog);
    }

    // 3. Obtener ubicaciones de uniforms
    uBorderColorLoc_ = glGetUniformLocation(programHandle_, "uBorderColor");
    uBorderWidthLoc_ = glGetUniformLocation(programHandle_, "uBorderWidth");
    uChromaKeyEnabledLoc_ = glGetUniformLocation(programHandle_, "uChromaKeyEnabled");
    uKeyColorLoc_ = glGetUniformLocation(programHandle_, "uKeyColor");
    uSimilarityLoc_ = glGetUniformLocation(programHandle_, "uSimilarity");
    uSmoothnessLoc_ = glGetUniformLocation(programHandle_, "uSmoothness");
    uSaturationLoc_ = glGetUniformLocation(programHandle_, "uSaturation");
    uContrastLoc_ = glGetUniformLocation(programHandle_, "uContrast");
    uBrightnessLoc_ = glGetUniformLocation(programHandle_, "uBrightness");

    isInitialized_ = true;
    LOGI("Tubería de sombreado GPU inicializada con éxito (Efecto: %d, Formato: %s)",
         static_cast<int>(effect), (format == ShaderFormat::GLSL_ES32 ? "GLSL" : "SPIR-V"));
    return {};
}

void GpuShaderPipeline::updateFaceCamConfig(const FaceCamConfig& config) {
    if (!isInitialized_ || programHandle_ == 0) return;
    glUseProgram(programHandle_);
    if (uBorderColorLoc_ >= 0) {
        glUniform4f(uBorderColorLoc_, config.borderColorR, config.borderColorG, config.borderColorB, config.borderColorA);
    }
    if (uBorderWidthLoc_ >= 0) {
        glUniform1f(uBorderWidthLoc_, config.borderWidth);
    }
    if (uChromaKeyEnabledLoc_ >= 0) {
        glUniform1i(uChromaKeyEnabledLoc_, config.chromaKeyEnabled ? 1 : 0);
    }
    if (uKeyColorLoc_ >= 0) {
        glUniform3f(uKeyColorLoc_, config.keyColorR, config.keyColorG, config.keyColorB);
    }
    if (uSimilarityLoc_ >= 0) {
        glUniform1f(uSimilarityLoc_, config.similarity);
    }
    if (uSmoothnessLoc_ >= 0) {
        glUniform1f(uSmoothnessLoc_, config.smoothness);
    }
}

void GpuShaderPipeline::updateColorBoostConfig(const ColorBoostConfig& config) {
    if (!isInitialized_ || programHandle_ == 0) return;
    glUseProgram(programHandle_);
    if (uSaturationLoc_ >= 0) {
        glUniform1f(uSaturationLoc_, config.saturation);
    }
    if (uContrastLoc_ >= 0) {
        glUniform1f(uContrastLoc_, config.contrast);
    }
    if (uBrightnessLoc_ >= 0) {
        glUniform1f(uBrightnessLoc_, config.brightness);
    }
}

void GpuShaderPipeline::bind() {
    if (isInitialized_ && programHandle_ != 0) {
        glUseProgram(programHandle_);
    }
}

void GpuShaderPipeline::unbind() {
    glUseProgram(0);
}

} // namespace velorec::gpu
