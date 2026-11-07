/**
 * Generador de archivos ZIP para exportar el proyecto nativo completo
 * React Native + Kotlin + C++23 + Rust (Edition 2024).
 * Permite al usuario móvil descargar todo el código listo para compilar en un solo toque.
 */

import JSZip from 'jszip';
import { NATIVE_FILES_CATALOG } from './nativeCodeCatalog';

export async function exportNativeProjectZip(): Promise<Blob> {
  const zip = new JSZip();

  // 1. Agregar archivo README explicativo en español
  const readmeContent = `# VeloRec Studio - Grabador de Pantalla Móvil de Alto Rendimiento

Proyecto híbrido modular: React Native + Kotlin + C++23 + Rust (Edition 2024).

## 🚀 Arquitectura
1. **Frontend (React Native)**: Interfaz de usuario, configuración de resolución, bitrate y selección de motor.
2. **Puente y Orquestación (Kotlin)**:
   - Manejo del diálogo de permiso de captura \`MediaProjection\`.
   - Servicio en primer plano (\`ScreenRecordService\`) compatible con Android 14+ (\`FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION\`).
   - Gestión de concurrencia mediante Corrutinas (\`Dispatchers.IO\`).
   - CERO copia de memoria: Kotlin entrega la Surface de hardware directamente al encoder nativo.
3. **Motor C++23 (AMediaCodec)**:
   - Uso de \`std::expected\`, \`std::span\`, \`std::jthread\` y \`std::format\`.
   - Creación de \`ANativeWindow\` de entrada con \`AMediaCodec_createInputSurface\`.
   - Multiplexación MP4 directa en bajo nivel.
4. **Motor Rust (Edition 2024)**:
   - Compilación con Rust 1.85+ (Edition 2024).
   - Canales multihilo sin contención con \`crossbeam-channel\`.
   - Seguridad de memoria estricta y zero-cost abstractions sin Garbage Collector.

## 📱 Compatibilidad de Arquitecturas
- **arm64-v8a** (64-bit ARM, estándar en dispositivos modernos)
- **armeabi-v7a** (32-bit ARM, compatibilidad para dispositivos antiguos)
- **x86_64** (Emuladores y Chromebooks)

## 🛠️ Cómo Compilar

### Opción 1: Compilación Automática en la Nube con GitHub Actions (Recomendado sin PC)
Este proyecto incluye el flujo \`.github/workflows/build-apk.yml\` con ejecución 100% MANUAL:
1. Sube este código a tu repositorio de GitHub.
2. Abre GitHub en el navegador de tu teléfono y ve a la pestaña **Actions**.
3. Toca **Compilar APK Debug (VeloRec Mobile)** y pulsa el botón **Run workflow**.
4. GitHub Actions descargará Node.js, JDK 17, Android SDK 35, NDK r26c, Rust 2024 y compilará el APK Debug.
5. Al terminar (3-4 min), descarga el artefacto **VeloRec-Debug-APK** (.apk) directo a tu teléfono.

### Opción 2: Compilación Local con Gradle
1. Instala dependencias: \`npm install\`
2. Compila el APK Debug: \`./gradlew assembleDebug\`
3. El APK se generará en: \`android/app/build/outputs/apk/debug/app-debug.apk\`
`;

  zip.file('README.md', readmeContent);

  // 2. Agregar todos los archivos del catálogo en sus respectivas carpetas
  for (const item of NATIVE_FILES_CATALOG) {
    zip.file(item.path, item.code);
  }

  // 3. Generar el blob ZIP comprimido
  const blob = await zip.generateAsync({ type: 'blob' });
  return blob;
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
