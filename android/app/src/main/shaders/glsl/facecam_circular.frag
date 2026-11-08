#version 320 es
/**
 * facecam_circular.frag (GLSL ES 3.20 / SPIR-V compatible)
 * 
 * ¿QUÉ HACE ESTE SHADER?
 * Fragment Shader acelerado por hardware en la GPU para la cámara frontal (FaceCam):
 * 1. Convierte el rectángulo de la cámara en un círculo perfecto con antialiasing suave (smoothstep).
 * 2. Dibuja un borde de color dinámico ajustable (uBorderColor y uBorderWidth).
 * 3. Incorpora Chroma Key en tiempo real para eliminar fondos verdes sin consumir CPU.
 */

precision mediump float;

layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 fragColor;

// Uniforms configurables por la tubería C++23 o Vulkan (en UBO para compatibilidad SPIR-V)
layout(binding = 0) uniform sampler2D uCameraTexture;

layout(binding = 1) uniform FaceCamUBO {
    vec4 uBorderColor;       // Color del borde exterior
    vec4 uKeyColor;          // Color clave a suprimir (RGB en xyz)
    float uBorderWidth;      // Grosor del borde normalizado (0.0 a 0.1)
    float uSimilarity;       // Tolerancia de similitud de color
    float uSmoothness;       // Suavizado del borde del chroma
    int uChromaKeyEnabled;   // Activar eliminación de pantalla verde (1 = sí, 0 = no)
};

void main() {
    // Centrar coordenadas en el rango [-0.5, 0.5]
    vec2 center = vec2(0.5, 0.5);
    vec2 uv = vTexCoord - center;
    float dist = length(uv);

    // Radio del círculo: 0.5 (ocupa todo el cuadro)
    float radius = 0.5;
    float innerRadius = radius - uBorderWidth;

    // Si está fuera del círculo exterior, descartar píxel
    if (dist > radius) {
        discard;
    }

    vec4 texColor = texture(uCameraTexture, vTexCoord);

    // 1. Procesamiento de Chroma Key (eliminación de pantalla verde en GPU)
    if (uChromaKeyEnabled != 0) {
        float diff = distance(texColor.rgb, uKeyColor.rgb);
        float alpha = smoothstep(uSimilarity, uSimilarity + uSmoothness, diff);
        texColor.a *= alpha;
        if (texColor.a < 0.05) {
            discard;
        }
    }

    // 2. Dibujado de borde antialiased
    if (dist > innerRadius) {
        float borderAlpha = smoothstep(radius, radius - 0.015, dist);
        fragColor = mix(uBorderColor, texColor, smoothstep(innerRadius, innerRadius + 0.015, dist)) * borderAlpha;
    } else {
        float edgeAlpha = smoothstep(radius, radius - 0.01, dist);
        fragColor = vec4(texColor.rgb, texColor.a * edgeAlpha);
    }
}
