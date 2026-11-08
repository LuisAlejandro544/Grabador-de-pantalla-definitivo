# 🤖 Guía Operativa para Agentes de IA — VeloRec Studio

Instrucciones de operación, comandos de prueba y protocolos de modificación para agentes y desarrolladores automatizados que trabajen en este repositorio.

---

## 🎯 Protocolo de Actuación

Antes de realizar cualquier cambio en el código:
1. **Razonar antes de ejecutar**: Analizar el impacto en la arquitectura móvil nativa (Kotlin Jetpack Compose -> Kotlin Corrutinas -> JNI -> C++23/Rust 2024).
2. **Respetar la restricción móvil y diseño nativo**: La aplicación es 100% móvil nativa (no un sitio web). La interfaz debe ser ergonómica para interacción táctil (Jetpack Compose Material 3), con controles de gran tamaño y sin simular marcos o barras de estado (`09:41 5G 100%`) dentro de la pantalla, ya que el usuario opera desde un dispositivo físico.
3. **No alterar el diseño gráfico sin solicitud explícita**: Mantener paleta de colores, iconos, tipografía y navegación existentes.
4. **No modificar `commit_message.txt`**: Si existe dicho archivo, no actualizarlo a menos que el usuario lo solicite expresamente.

---

## 🛠️ Roles de Desarrollo Aplicables

Este proyecto implementa el ciclo estructurado de 7 fases:

| Rol | Función en el Proyecto |
|---|---|
| **01. El Arquitecto** | Planifica la estructura entre Kotlin Compose, Coroutines y NDK C++/Rust. |
| **02. El Constructor** | Escribe código modular, tipado y listo para producción sin placeholders. |
| **03. El Detective** | Diagnostica bugs en el ciclo de vida de `MediaProjection`, JNI o Web APIs. |
| **04. El Crítico** | Revisa la seguridad de memoria (zero-copy), permisos y eficiencia de hilos. |
| **05. El Optimizador** | Refactoriza cálculos matemáticos y reduce la latencia por cuadro. |
| **06. El Escudo** | Mantiene la suite de tests con `node:test` y `node:assert/strict`. |
| **07. El Narrador** | Documenta APIs, manifiestos y flujos paso a paso para el usuario. |

---

## 🧪 Comandos de Verificación Obligatorios

Cualquier cambio de código debe validarse ejecutando en orden:

```bash
# 1. Pruebas Unitarias de Lógica Pura (desacoplada del DOM)
npx --yes tsx --test src/utils/recorderCore.test.ts

# 2. Verificación de Tipos TypeScript
npm run lint

# 3. Compilación de Producción del Applet Web
npm run build
```

---

## ⚠️ Puntos Críticos y Trampas Comunes

- **`actions/setup-node@v4` en GitHub Actions**:
  - **NUNCA** añadir `cache: 'npm'` ni `cache: 'gradle'` a menos que un lockfile (`package-lock.json`) esté garantizado en la raíz del repositorio. De lo contrario, la acción fallará inmediatamente.
- **Android SDK y NDK en GitHub Actions (`ubuntu-latest`)**:
  - **NUNCA** usar la acción de terceros `android-actions/setup-android@v3` porque intenta descargar el paquete obsoleto `tools` que Google eliminó de los repositorios del SDK, provocando el fallo `Failed to find package 'tools'`. Los entornos de GitHub Actions ya traen el Android SDK y NDK r26/r27 preinstalados en `/usr/local/lib/android/sdk`; se deben usar directamente las variables de entorno `ANDROID_HOME` y `ANDROID_NDK_HOME`.
- **Disparo del GitHub Action**:
  - Debe mantenerse configurado **únicamente** con `on: workflow_dispatch`. No añadir `push` ni `pull_request` para evitar ejecuciones innecesarias que consuman cuotas de GitHub.
- **Paridad en Resoluciones**:
  - Los codificadores de hardware NDK (`AMediaCodec`) en Android fallarán con error si el ancho o alto son números impares (ej: 1081x1920). Cualquier función que modifique resoluciones debe aplicar `width % 2 === 0 && height % 2 === 0`.
- **Corrutinas en Kotlin**:
  - Toda llamada al NDK (`nativeInitCppRecorder`, `nativeInitRustRecorder`, `nativeStartCppEncoding`) debe ejecutarse en `Dispatchers.IO`, nunca en el hilo principal de la UI.
- **Enlace de Firmas JNI en C++23 y Rust 2024**:
  - Las firmas nativas exportadas en `NativeRecorderEngine.cpp` y `rust/src/lib.rs` corresponden a `Java_com_velorec_recorder_ScreenRecordService_native*`. Por ello, las declaraciones `external fun` residen en `ScreenRecordService` para evitar excepciones `UnsatisfiedLinkError` en tiempo de ejecución.
- **Arquitectura Modular en Kotlin**:
  - La capa nativa se estructura en paquetes (`data/model`, `data/repository`, `ui/screens`, `ui/components`, `ui/theme`, `service`, `audio`, `storage`), manteniendo `VeloRecComposeUI.kt` como fachada modular retrocompatible.
- **Grabación Directa NDK C++23 / Rust 2024 sin Recolector de Basura (GC-Free)**:
  - La codificación de video se realiza al 100% en hilos dedicados de C++23 (`std::thread`) y Rust 2024 (`std::thread`), enlazados a `ANativeWindow` de hardware y `AMediaMuxer`. Ningún byte de video pasa por la JVM/ART ni activa el Garbage Collector de Android durante la captura de pantalla.
- **Multiplexación Nativa de Audio y Video sin FFmpeg**:
  - Cuando se graba con audio, la fusión se realiza de forma instantánea (<200 ms) mediante `AudioMuxerHelper` utilizando el subsistema oficial de Android (`MediaExtractor` y `MediaMuxer`), prescindiendo de binarios externos pesados o incompatibles como FFmpeg.
- **Publicación en Galería Pública del Dispositivo**:
  - Toda grabación finalizada se publica en `Movies/VeloRec` mediante `GalleryStorageManager` (MediaStore Scoped Storage + `MediaScannerConnection.scanFile`) para garantizar que aparezca de inmediato en Google Fotos y en la galería nativa del usuario.
- **Auditoría de Código y Calidad para Producción (`code-analysis-audit.yml`)**:
  - Flujo manual para evaluar seguridad y estabilidad antes de publicar en tiendas como Uptodown. Corre Android Lint (`./gradlew lintDebug`), `cppcheck` para C++23 y libyuv, y `cargo clippy` para Rust 2024.
- **Integración con Google libyuv (C++23 & SIMD NEON)**:
  - Incluye `libyuv_bridge.hpp` y `libyuv_bridge.cpp` en `android/app/src/main/cpp`. Proporciona transformaciones de color seguras sin excepciones (`std::expected`, `std::span`) y escalado bilineal para arquitecturas de 32 y 64 bits.
- **Multiplexación de Audio en Tiempo Real en C++23 (Eliminación del Paso 2)**:
  - En C++23, las pistas de video y audio se multiplexan de forma entrelazada directamente en `AMediaMuxer`. El motor de audio en Kotlin pasa los paquetes codificados AAC mediante JNI (`nativeWriteAudioSample`) en tiempo real. Al presionar "Detener", el contenedor MP4 ya posee el audio y video perfectamente sincronizados, eliminando la necesidad de fusionar archivos temporales por separado.
- **Grabación Directa sobre Descriptor de la Galería MediaStore (Eliminación del Paso 3)**:
  - Antes de iniciar la captura, `GalleryStorageManager.createDirectPendingGalleryTarget()` inserta el registro en MediaStore con `IS_PENDING = 1` y abre un `ParcelFileDescriptor` con modo `"rwt"`. Dicho descriptor nativo (`rawFd`) se pasa a C++23 para que `AMediaMuxer_new(fd, ...)` escriba los bytes directamente en la ubicación final (`Movies/VeloRec`). Al parar la grabación, solo se actualiza `IS_PENDING = 0` y se notifica al escáner de medios (<15 ms), erradicando las copias redundantes de cientos de megabytes en discos eMMC lentos.
- **Guardado en Segundo Plano con Notificación de Progreso Escalonado**:
  - Al presionar "Detener", el servicio libera inmediatamente el hilo y el estado de la UI (`isRecording = false`), mientras una corrutina en `Dispatchers.IO` actualiza la notificación en la barra de estado con el progreso de finalización (25% deteniendo codificador, 75% publicando en galería, 100% completado). La interfaz permanece reactiva y ergonómica en teléfonos modestos.
- **Buffer Pacing Asíncrono para Almacenamiento eMMC (Detección Automática)**:
  - En `NativeRecorderEngine.cpp`, la función `checkIsEmmcStorage()` inspecciona `/sys/block/mmcblk0` y `/proc/partitions`. Si se detecta almacenamiento eMMC 5.1, la escritura en `AMediaMuxer` se encola asíncronamente en lotes de hasta 16 muestras amortiguadas para absorber los picos de latencia de escritura en juegos pesados (ej. Subway Surfers con partículas/monedas masivas). En UFS/NVMe se realiza escritura directa sin sobrecarga.
- **Afinidad de Hilos (Thread Affinity) a Núcleos de Eficiencia (LITTLE)**:
  - `applyCpuAffinityForEfficiencyCores()` en C++23 y `apply_efficiency_core_affinity()` en Rust 2024 usan `sched_setaffinity` para anclar los hilos de fondo de codificación y vaciado a los núcleos LITTLE (Cortex-A55, núcleos 0..3 o menores frecuencias), reservando los núcleos Big (Cortex-A75) **100% libres y sin interrupciones para el videojuego en ejecución**.



