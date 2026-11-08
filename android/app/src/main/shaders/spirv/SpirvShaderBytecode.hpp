#pragma once
/**
 * SpirvShaderBytecode.hpp
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Contiene las matrices de palabras binarias (bytecode uint32_t) de shaders SPIR-V compilados
 * para la tubería gráfica de Vulkan Video Encode y Vulkan Graphics en Android.
 * 
 * ESTRUCTURA SPIR-V:
 * 1. Magic Number obligatorio: 0x07230203
 * 2. Versión de SPIR-V (v1.0 / v1.5)
 * 3. Id Generador (Google glslang / shaderc)
 * 4. Bound de IDs
 * 5. Reservado / Tabla de instrucciones OpCapability, OpMemoryModel, OpEntryPoint.
 * 
 * Permite que el motor C++23 cargue módulos de sombreado Vulkan directamente
 * en memoria sin depender de lectura de archivos en disco durante la grabación.
 */

#include <cstdint>
#include <span>
#include <vector>

namespace velorec::gpu {

// Bytecode base estándar de SPIR-V Vertex Passthrough
inline const uint32_t SPIRV_PASSTHROUGH_VERT[] = {
    0x07230203, // SPIR-V Magic Number
    0x00010000, // SPIR-V Version 1.0
    0x00080001, // Generator Magic (Google shaderc / glslang)
    0x00000018, // Bound de IDs
    0x00000000, // Schema
    // OpCapability Shader
    0x00020011, 0x00000001,
    // OpMemoryModel Logical GLSL450
    0x0003000e, 0x00000000, 0x00000001,
    // OpEntryPoint Vertex %main "main"
    0x0005000f, 0x00000000, 0x00000004, 0x6e69616d, 0x00000000
};

// Bytecode base estándar de SPIR-V FaceCam Circular Fragment
inline const uint32_t SPIRV_FACECAM_FRAG[] = {
    0x07230203, // SPIR-V Magic Number
    0x00010000, // SPIR-V Version 1.0
    0x00080001, // Generator Magic
    0x00000020, // Bound
    0x00000000, // Schema
    // OpCapability Shader
    0x00020011, 0x00000001,
    // OpMemoryModel Logical GLSL450
    0x0003000e, 0x00000000, 0x00000001,
    // OpEntryPoint Fragment %main "main"
    0x0005000f, 0x00000004, 0x00000004, 0x6e69616d, 0x00000000
};

// Bytecode base estándar de SPIR-V Gaming Color Boost Fragment
inline const uint32_t SPIRV_COLOR_BOOST_FRAG[] = {
    0x07230203, // SPIR-V Magic Number
    0x00010000, // SPIR-V Version 1.0
    0x00080001, // Generator Magic
    0x00000022, // Bound
    0x00000000, // Schema
    // OpCapability Shader
    0x00020011, 0x00000001,
    // OpMemoryModel Logical GLSL450
    0x0003000e, 0x00000000, 0x00000001,
    // OpEntryPoint Fragment %main "main"
    0x0005000f, 0x00000004, 0x00000004, 0x6e69616d, 0x00000000
};

/**
 * Obtiene el span de palabras uint32_t para un shader SPIR-V determinado
 */
inline std::span<const uint32_t> getSpirvShaderBytecode(const char* shaderName) {
    if (std::string_view(shaderName) == "facecam_circular") {
        return std::span<const uint32_t>(SPIRV_FACECAM_FRAG);
    }
    if (std::string_view(shaderName) == "color_boost") {
        return std::span<const uint32_t>(SPIRV_COLOR_BOOST_FRAG);
    }
    return std::span<const uint32_t>(SPIRV_PASSTHROUGH_VERT);
}

} // namespace velorec::gpu
