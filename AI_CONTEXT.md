# 🧠 AI Context — VeloRec Studio

Este documento proporciona contexto técnico estructurado y reglas invariantes para modelos de lenguaje (LLMs) y sistemas de asistencia que interactúen con el repositorio de **VeloRec Studio**.

---

## 🎯 Propósito del Proyecto

VeloRec Studio es una aplicación móvil de grabación de pantalla de alto rendimiento diseñada para Android. Utiliza una arquitectura en cuatro capas para maximizar la tasa de cuadros por segundo (FPS) y minimizar el consumo térmico y de batería:

1. **Frontend / Presentación**: Aplicación móvil 100% nativa con **Kotlin Jetpack Compose (Material 3)** organizada modularmente (`ui/screens`, `ui/components`, `ui/theme`, `data/repository`, `data/model`) con Server-Driven UI dinámico desde GitHub Releases y **Lanzador Rápido de Juegos/Apps (1-Click REC)**.
2. **Orquestación en Android**: Kotlin con Arquitectura MVVM (`MainViewModel` con StateFlow), Corrutinas (`Dispatchers.IO`), gestión modular de permisos `MediaProjection`, y `ScreenRecordService` con `RecordingNotificationHelper`.
3. **Motor de Grabación Nativo Exclusivo (Hardware NDK)**:
   - **Rust (Edition 2024)**: Motor central de grabación de pantalla con `crossbeam-channel`, seguridad de memoria estricta y huella de memoria plana (<15 MB RSS) inmune a Low Memory Killer (LMK). Cero recolección de basura (GC-Free).
4. **Aceleración Gráfica y Protocolos de Red (C++23 Shaders & Rust Streamer)**:
   - **Shaders GPU (C++23 & SPIR-V Vulkan)**: Tubería C++23 (`GpuShaderPipeline`) dedicada exclusivamente para FaceCam circular antialiased, Chroma Key (pantalla verde) y Gaming Color Boost mediante shaders GPU (GLSL ES 3.20 y SPIR-V Vulkan). `libyuv` fue migrado al 100% a Rust 2024 (`rust/src/color_yuv.rs` con `yuvutils-rs` y aceleración SIMD ARM NEON).
   - **Rust Streamer (`rust/src/streamer.rs`)**: Infraestructura de streaming de red de ultra baja latencia (RTMP, RTMPS, SRT y WebRTC) unificada en Rust puro con canales `crossbeam-channel` y *Ring Buffer* de 4 MB, reemplazando completamente el runtime de Go con cero pausas de Garbage Collector y menor uso de RAM (<10 MB).
5. **Grabación Directa NDK y Multiplexación en Tiempo Real 100% en Rust**:
   - Codificación directa de hardware a MP4 en Rust sin pasar por JVM/ART.
   - **Multiplexación Simultánea de Video y Audio en Rust**: `AMediaMuxer` registra las pistas de video y audio AAC simultáneamente. Las muestras se escriben de forma entrelazada en tiempo real (`nativeWriteRustAudioSample`), sin archivos temporales en disco y permitiendo finalización instantánea (<15 ms) al presionar "Detener".
   - **Sincronización de Audio Maestra y Erradicación Total de Desync (Zero-Drift & Anti-Lag)**: Reloj continuo de muestras matemáticas respaldado por la base temporal T0 unificada en Rust `SharedMuxerState`, con purga de backlog en el búfer circular (`discardOldestIfExceeds`) ante picos de CPU, erradicando retrasos acumulados en partidas de Subway Surfers o Free Fire.
   - **Google Oboe 100% en Rust**: Captura de audio de ultra baja latencia (<10 ms con AAudio/OpenSL ES) administrada exclusivamente en Rust (crate `oboe`), eliminando la dependencia prefab AAR de Oboe en C++23.
   - **Conversión de Color y Escalado YUV 100% en Rust (`yuvutils-rs`)**: Conversiones de color NV12, I420 y RGBA junto a escalado bilineal en `rust/src/color_yuv.rs` con aceleración vectorial SIMD ARM NEON (32/64 bits), erradicando la dependencia de Google libyuv en C++23 y agilizando la compilación CMake.
   - Guardado directo en Galería (`Movies/VeloRec`) con indexación inmediata.
6. **CI/CD Móvil**: Flujo de GitHub Actions con disparo **estrictamente manual (`workflow_dispatch`)** para compilar el APK Debug sin necesidad de computadora.

---

## 🔒 Reglas Técnicas Invariantes (Reglas de Oro)

1. **Tubería Zero-Copy en Kotlin**:
   - Bajo ninguna circunstancia se debe procesar el flujo de cuadros en la capa de Java/Kotlin (no usar `ImageReader` con `Bitmap` o buffers en memoria ART).
   - Kotlin únicamente solicita el permiso del sistema y pasa la `Surface` obtenida del motor C++ o Rust a `MediaProjection.createVirtualDisplay`.
2. **Cumplimiento con Android 14+ (API 34/35)**:
   - Toda llamada a `startForeground()` debe incluir `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`.
   - En `AndroidManifest.xml` debe existir el permiso `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />`.
   - `minSdkVersion` se mantiene en **26** (Android 8.0 Oreo) y `targetSdkVersion` en **35**.
3. **Estándares de Lenguaje Fijos**:
   - C++ **debe** compilarse con `-std=c++23`.
   - Rust **debe** utilizar `edition = "2024"` (Rust 1.85+).
4. **Soporte de Arquitecturas (32 y 64 bits) y Compilación Selectiva en CI**:
   - Se generan las bibliotecas nativas para `arm64-v8a` (64-bit ARM), `armeabi-v7a` (32-bit ARM) y `x86_64` (emulador).
   - El workflow `build-apk.yml` permite compilar en GitHub Actions el APK Universal o APKs monofiltrados por arquitectura individual (`-PtargetAbi`) para optimizar el peso del instalador.
5. **Entorno del Usuario y Diseño 100% Móvil Nativo**:
   - La app es una aplicación para smartphone, **NO un sitio web**.
   - No utilizar marcos o barras simuladas de teléfono (`09:41 5G 100%`) dentro de la pantalla, ya que el usuario opera directamente desde un dispositivo físico real.
   - Las interfaces deben ser 100% touch-friendly, con botones grandes, navegación inferior fija Material 3 y sin desbordamientos horizontales.
   - No forzar herramientas que requieran línea de comandos local; el APK se compila en la nube mediante GitHub Actions.
6. **Cuenta Regresiva y Presets Adaptativos a la Pantalla**:
   - Toda inicialización de grabación debe respetar la cuenta regresiva seleccionada (0s / desactivada, 3s, 5s o 10s) con interfaz táctil de cancelación inmediata.
   - Las resoluciones ofrecidas deben adaptarse al aspect ratio físico del teléfono (ej. 20:9 o 19.5:9) y garantizar dimensiones pares para evitar desbordamientos y barras verdes en codificadores de hardware.
   - **Múltiples Escalas Nativas**: Permite seleccionar variantes en la misma relación de aspecto: 100% Nativa (1:1), FHD Adaptada, HD Adaptada, Ahorro Gaming (540p) y 2K Ultra.
7. **Políticas de Propiedad Intelectual y Licenciamiento**:
   - Nunca incluir nombres de marcas comerciales protegidas en nombres de archivos.
   - Solo usar dependencias con licencias permisivas (MIT, Apache-2.0, BSD). Prohibido introducir librerías GPL o copyleft que obliguen a abrir el código.
8. **Configuración Remota Dinámica y Releases de GitHub**:
   - La app consulta de forma asíncrona y no bloqueante los releases en `https://github.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases`.
   - **Resiliencia Offline Obligatoria**: La app nunca debe estar atada al 100% a la conexión ni fallar si no hay internet; siempre debe inicializarse con valores por defecto locales seguros.
   - El workflow `.github/workflows/publish-config-release.yml` debe ejecutarse **exclusivamente con `workflow_dispatch`** (nunca en `push` ni `pull_request`).
9. **Grabación NDK Pura y Almacenamiento Público en Galería**:
   - Toda grabación completada se publica directamente en la Galería del usuario (`Movies/VeloRec`) mediante Scoped Storage y `MediaScannerConnection`.
   - Limpieza automática de archivos temporales al iniciar el servicio, garantizando que el almacenamiento del dispositivo se mantenga optimizado.
10. **Auditoría de Calidad y Seguridad para Producción en CI (`code-analysis-audit.yml`)**:
   - Workflow activado **estrictamente por `workflow_dispatch`**.
   - Ejecuta análisis estático multinivel: Android Lint (Kotlin API 35 / fugas de memoria), Cppcheck (C++23 y libyuv), Clippy (Rust 2024 / memory safety sin GC) y comprobación TypeScript/node:test.
11. **Biblioteca Google libyuv (C++23 & SIMD NEON)**:
   - Integrada en CMake con `libyuv_bridge.hpp` y `libyuv_bridge.cpp`. Proporciona la base para conversiones YUV420 y escalado bilineal acelerado por hardware para 32 y 64 bits.
12. **Motor Rust 2024 Optimizado contra Low Memory Killer (LMK)**:
   - El motor en Rust no acumula memoria en cola ni asigna buffers dinámicos durante la captura; escribe los datos directamente al multiplexor nativo con huella menor a 15 MB de RAM, previniendo que el asesino de procesos en segundo plano de Android (LMK/Phantom Process Killer) elimine la app mientras se ejecutan juegos pesados.
13. **Afinidad de Hilos (Thread Affinity) hacia Núcleos de Eficiencia (LITTLE)**:
   - En procesadores móviles big.LITTLE (ej: Helio G81/G85/G88, Snapdragon), los hilos de fondo (`velorec_rust_worker`) se fijan a los núcleos de bajo consumo (Cortex-A55) mediante `sched_setaffinity`, dejando los núcleos Big (Cortex-A75) 100% disponibles para el videojuego (Subway Surfers, Free Fire).
14. **Sincronización Maestra de Audio y Video con Reloj Monotónico**:
   - `AudioRecordEngine.kt` sincroniza los timestamps de audio mediante `System.nanoTime()` y cuenta con un búfer circular de 262,144 muestras (~2.73s). `AudioMuxerHelper.kt` normaliza los timestamps base de video y audio a t=0, eliminando cualquier desfase entre disparos, pasos y video.
15. **Publicación Directa en la Galería MediaStore Scoped Storage**:
   - Al finalizar la captura, el audio y video se combinan de forma ultrarrápida y se publican en `Movies/VeloRec` mediante MediaStore y `MediaScannerConnection`, garantizando visibilidad instantánea en Google Fotos y reproductores del sistema.
16. **Guardado en Segundo Plano con Notificación de Progreso Escalonado**:
   - Al tocar "Detener", la interfaz responde de inmediato cambiando su estado a inactivo (`isRecording = false`), mientras una corrutina en `Dispatchers.IO` procesa el cierre y emite notificaciones de progreso (25% -> 75% -> 100%) para evitar cuelgues o bloqueos del hilo principal.
17. **Integración de Google Oboe en Rust (`audio_oboe.rs`)**:
   - Enlace oficial del crate `oboe` para captura nativa de audio mediante `AAudio` (Android 8.0+) y `OpenSL ES`. Ofrece latencias de audio inferiores a 10 milisegundos sin pausas de Garbage Collector, facilitando audio nítido para grabaciones gaming.
18. **Conversión de Color Acelerada con SIMD ARM NEON en Rust Puro (`color_yuv.rs`)**:
   - Implementación con `yuvutils-rs` para transformar RGBA a NV12/YUV420 y viceversa a más de 200 FPS en procesadores móviles de 32 y 64 bits, garantizando seguridad de memoria y erradicando cuelgues por punteros corruptos.
19. **Tubería de Sombreadores GPU en Rust Puro con Glow (`gpu_glow.rs`)**:
   - Renderizado OpenGL ES 3.20 con `glow` para FaceCam circular antialiased, Chroma Key en GPU y Gaming Color Boost (realce de saturación y contraste a 60/120 FPS sin consumo de CPU).
20. **Evaluación de Streaming Móvil (Rust vs. Go)**:
   - Para el cliente móvil dentro del smartphone, **Rust** es superior a Go al compartir el mismo espacio de memoria con el codificador de hardware NDK, tener huella menor (<10 MB vs 30 MB), no sufrir pausas de Garbage Collector durante partidas intensivas y eliminar la necesidad de empaquetar un segundo runtime (`libgo.so`).
21. **Guardián de Resiliencia y Blindaje Anti-LMK en Rust 2024 (`resilience.rs`)**:
   - Compactación periódica de heap vía `malloc_trim(0)` para forzar a Bionic libc a devolver páginas al kernel de Linux, manteniendo una huella plana de RSS (<15 MB).
   - Prioridad de proceso `nice -16` y prioridad de E/S de tiempo real (`SYS_ioprio_set` Best-Effort Clase 2) para blindar la escritura de paquetes de video contra saturación del disco.
   - Fijación de páginas de memoria en RAM física (`mlock`) para impedir que el compresor zRAM o kswapd del sistema operativo las mueva a swap.
   - Manejador de señales POSIX de emergencia (`SIGTERM`, `SIGINT`, `SIGHUP`) que intercepta intentos de matar el proceso y finaliza atómicamente el MP4 (cerrando el átomo `moov`), eliminando cualquier riesgo de video corrupto.
   - Lectura directa de `/proc/self/status` (VmRSS), `/proc/self/oom_score` y `/proc/meminfo` (MemAvailable) con sincronización en `ScreenRecordService.onTrimMemory(level)`.

---

## 📊 Estado de los Datos y Almacenamiento

- Las grabaciones se guardan en el álbum público `Movies/VeloRec` (accesible desde Google Fotos, Galería Samsung, Xiaomi, etc.) y se respaldan en el almacenamiento local.
- Las opciones de configuración persisten en `localStorage` / `SharedPreferences` con fallback automático a valores seguros por defecto (1080p @ 60 FPS, 12 Mbps, Rust 2024).
- El validador de resoluciones exige que ancho y alto sean números pares para compatibilidad con codificadores por hardware YUV420p (`width % 2 === 0 && height % 2 === 0`).
