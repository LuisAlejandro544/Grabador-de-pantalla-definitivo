#version 320 es
/**
 * passthrough.vert (GLSL ES 3.20 / SPIR-V compatible)
 * 
 * ¿QUÉ HACE ESTE SHADER?
 * Vertex Shader estándar de alta eficiencia para renderizar un cuadrante completo (Fullscreen Quad).
 * Mapea las coordenadas normalizadas de pantalla (-1.0 a 1.0) a coordenadas UV de textura (0.0 a 1.0).
 * Compatible con OpenGL ES 3.0+ y compilable directamente a SPIR-V para Vulkan.
 */

layout(location = 0) in vec4 aPosition;
layout(location = 1) in vec2 aTexCoord;

layout(location = 0) out vec2 vTexCoord;

void main() {
    gl_Position = aPosition;
    vTexCoord = aTexCoord;
}
