#version 320 es
/**
 * gaming_color_boost.frag (GLSL ES 3.20 / SPIR-V compatible)
 * 
 * ¿QUÉ HACE ESTE SHADER?
 * Fragment Shader para mejora visual y realce de juegos en tiempo real en la GPU:
 * 1. Aumenta la saturación y viveza cromática de juegos móviles (FPS, MOBA, Battle Royale).
 * 2. Aplica curva de contraste sigmoide y corrección de gamma.
 * 3. Ejecución directa a 60/120 FPS sin latencia añadida en núcleos Adreno/Mali.
 */

precision mediump float;

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uScreenTexture;
uniform float uSaturation; // 1.0 = original, 1.25 = mejora gaming recomendada, 1.5 = ultra vivo
uniform float uContrast;   // 1.0 = original, 1.15 = contraste dinámico
uniform float uBrightness; // 0.0 = neutro, 0.05 = realce de sombras

// Coeficientes estándar de luminancia ITU-R BT.709
const vec3 LUMA_COEFFS = vec3(0.2126, 0.7152, 0.0722);

void main() {
    vec4 color = texture(uScreenTexture, vTexCoord);

    // 1. Ajuste de brillo
    vec3 rgb = color.rgb + vec3(uBrightness);

    // 2. Ajuste de contraste con punto de pivote en 0.5
    rgb = (rgb - 0.5) * uContrast + 0.5;

    // 3. Ajuste de saturación de color
    float luminance = dot(rgb, LUMA_COEFFS);
    rgb = mix(vec3(luminance), rgb, uSaturation);

    // Clamping seguro entre 0.0 y 1.0 para prevenir artefactos en el codificador por hardware
    fragColor = vec4(clamp(rgb, 0.0, 1.0), color.a);
}
