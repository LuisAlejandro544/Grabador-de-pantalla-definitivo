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
  - Toda llamada al NDK (`nativeInitRustRecorder`, `nativeStartRustEncoding`) debe ejecutarse en `Dispatchers.IO`, nunca en el hilo principal de la UI.
- **Enlace de Firmas JNI en Rust 2024**:
  - Las firmas nativas exportadas en `rust/src/lib.rs` corresponden a `Java_com_velorec_recorder_ScreenRecordService_native*Rust*`. Por ello, las declaraciones `external fun` residen en `ScreenRecordService` para evitar excepciones `UnsatisfiedLinkError` en tiempo de ejecución.
- **Arquitectura Modular en Kotlin**:
  - La capa nativa se estructura en paquetes (`data/model`, `data/repository`, `ui/screens`, `ui/components`, `ui/theme`, `service`, `audio`, `storage`), manteniendo `VeloRecComposeUI.kt` como fachada modular retrocompatible.
- **Grabación Directa NDK Rust 2024 sin Recolector de Basura (GC-Free & LMK-Safe)**:
  - La codificación de video se realiza al 100% en hilos dedicados de Rust 2024 (`std::thread`), enlazados a `ANativeWindow` de hardware y `AMediaMuxer`.
  - Mantiene un consumo plano de memoria (<15 MB RSS) sin colas dinámicas ilimitadas, haciéndolo 100% inmune a muertes provocadas por el Low Memory Killer (LMK) o Phantom Process Killer de Android durante partidas de juegos intensivos (Free Fire, Subway Surfers).
- **Tubería Gráfica C++23 Dedicada a Shaders GPU y libyuv**:
  - C++23 se reserva exclusivamente para la aceleración visual: `GpuShaderPipeline` (GLSL ES 3.20 y SPIR-V Vulkan) para FaceCam antialiased, Chroma Key y Gaming Color Boost, y `libyuv_bridge` para conversiones de color ultrarrápidas con aceleración SIMD ARM NEON.
- **Sincronización Maestra de Audio/Video y Búfer de 256K Muestras (Anti-Lag y Cero Desync)**:
  - `AudioRecordEngine.kt` cuenta con un búfer circular expandido a 262,144 muestras (~2.73s de audio estéreo 48 kHz) para absorber picos de procesamiento de CPU sin perder muestras ni producir ruidos de chasquido o audio entrecortado.
  - El codificador calcula los timestamps con reloj monotónico de tiempo real (`System.nanoTime()`) y `AudioMuxerHelper` normaliza el tiempo base de video y audio a t=0. Al disparar o correr en un juego, el audio y los cuadros coinciden con precisión milimétrica sin retraso acumulativo.
- **Multiplexación Nativa de Audio y Video 100% en Rust (Cero Espera al Detener)**:
  - Rust 2024 registra ambas pistas (video y audio AAC) en `AMediaMuxer` antes de iniciar la captura.
  - Las muestras de audio se transmiten en tiempo real vía `nativeWriteRustAudioSample`, entrelazándose al vuelo con el video en el mismo contenedor MP4.
  - Al presionar "Detener", la finalización es instantánea (<15 ms) sin crear archivos temporales (`temp_video.mp4` o `temp_audio.m4a`) ni requerir fases de merge secundarias.
- **Publicación en Galería Pública del Dispositivo**:
  - Toda grabación finalizada se publica en `Movies/VeloRec` mediante `GalleryStorageManager` (MediaStore Scoped Storage + `MediaScannerConnection.scanFile`) para garantizar que aparezca de inmediato en Google Fotos y en la galería nativa del usuario.
- **Auditoría de Código y Calidad para Producción (`code-analysis-audit.yml`)**:
  - Flujo manual para evaluar seguridad y estabilidad antes de publicar en tiendas como Uptodown. Corre Android Lint (`./gradlew lintDebug`), `cppcheck` para C++23 y libyuv, y `cargo clippy` para Rust 2024.
- **Guardado en Segundo Plano con Notificación de Progreso Escalonado**:
  - Al presionar "Detener", el servicio libera inmediatamente el hilo y el estado de la UI (`isRecording = false`), mientras una corrutina en `Dispatchers.IO` actualiza la notificación en la barra de estado con el progreso de finalización (25% deteniendo codificador, 75% publicando en galería, 100% completado). La interfaz permanece reactiva y ergonómica en teléfonos modestos.
- **Afinidad de Hilos (Thread Affinity) a Núcleos de Eficiencia (LITTLE)**:
  - `apply_efficiency_core_affinity()` en Rust 2024 y `applyCpuAffinityForEfficiencyCores()` en C++23 usan `sched_setaffinity` para anclar los hilos de fondo a los núcleos LITTLE (Cortex-A55, núcleos 0..3), reservando los núcleos Big (Cortex-A75) **100% libres y sin interrupciones para el videojuego en ejecución**.
- **Integración de Ecosistema Avanzado en Rust (`oboe`, `yuvutils-rs`, `glow`)**:
  - `oboe` (`rust/src/audio_oboe.rs`): Captura de audio de ultra baja latencia (<10 ms) con AAudio/OpenSL ES.
  - `yuvutils-rs` (`rust/src/color_yuv.rs`): Conversiones de color YUV/NV12 y RGBA con aceleración SIMD ARM NEON en 32 y 64 bits.
  - `glow` (`rust/src/gpu_glow.rs`): Shaders OpenGL ES 3.20 en Rust puro (FaceCam, Chroma Key, Gaming Boost).
- **Streaming en el Dispositivo (Rust vs. Go)**:
  - Para transmisión en vivo desde el smartphone, se prioriza **Rust** sobre Go: menor consumo de memoria (<10 MB vs 30 MB), cero pausas de Garbage Collector que afecten al juego, y eliminación del runtime secundario `libgo.so` en el APK.
