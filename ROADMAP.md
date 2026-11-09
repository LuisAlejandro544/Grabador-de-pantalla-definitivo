# 🗺️ Hoja de Ruta (ROADMAP) — VeloRec Studio

Plan de evolución técnica y funcional para el grabador de pantalla móvil de alto rendimiento.

---

## 📌 Fase 1: Arquitectura Base y Motores Nativos (Completada ✅)

- [x] **Diseño de Interfaz 100% Móvil Nativo con Jetpack Compose (Material 3)**: Interfaz táctil ergonómica sin marcos web simulados, con navegación inferior fija, HUD de métricas de hardware y controles optimizados para uso con una sola mano en smartphones.
- [x] **Arquitectura 100% Nativa Pura (Sin React Native)**: Eliminación de capas intermedias y overhead de JS; interfaz directa en Jetpack Compose comunicándose de forma inmediata con el servicio en primer plano y NDK.
- [x] **Arquitectura Modular Kotlin y MVVM**: Separación del código en paquetes desacoplados (`data/model`, `data/repository` con `Dispatchers.IO`, `ui/screens`, `ui/components`, `ui/theme`, `ui/MainViewModel` con StateFlow, y `service/RecordingNotificationHelper`), eliminando archivos monolíticos y garantizando persistencia del cronómetro ante rotaciones del smartphone.
- [x] **Capa de Orquestación Kotlin con Corrutinas**:
  - `ScreenRecordService` con soporte para Android 14+ (`FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`).
  - `minSdkVersion = 26` y `targetSdkVersion = 35`.
  - Tubería Zero-Copy delegando `Surface` a C++ y Rust.
- [x] **Motor Central de Grabación Nativo Rust (Edition 2024) [MOTOR PRINCIPAL]**:
  - Implementación con `edition = "2024"`, canales `crossbeam-channel` y bindings JNI seguros.
  - Cero condiciones de carrera, sin recolector de basura (GC-Free) y huella de memoria plana (<15 MB RSS) totalmente inmune a muertes por Low Memory Killer (LMK) o Phantom Process Killer de Android mientras se juegan títulos pesados.
- [x] **Tubería Gráfica C++23 Especializada para Shaders de GPU y libyuv**:
  - Compilado con `-std=c++23` con `GpuShaderPipeline` para FaceCam antialiased, Chroma Key en GPU, Gaming Color Boost y Google libyuv (SIMD NEON) para conversiones de color y escalado bilineal a 60/120 FPS sin consumo de CPU.
- [x] **Sincronización Maestra de Audio/Video y Buffer Expandido Anti-Lag**:
  - Búfer circular de audio de 262,144 muestras (~2.73s de audio estéreo 48 kHz) que previene caídas de paquetes y chasquidos bajo alta carga de CPU.
  - Reloj monotónico de tiempo real (`System.nanoTime()`) y normalización base cero en `AudioMuxerHelper` para sincronización milimétrica instantánea entre disparos, pasos y video.
- [x] **Publicación Directa en la Galería del Sistema (`Movies/VeloRec`)**:
  - Guardado automático mediante MediaStore API (Scoped Storage) e indexación instantánea con `MediaScannerConnection` para visibilidad inmediata en Google Fotos, Galería Samsung, Xiaomi, etc.
- [x] **Lanzador Rápido de Juegos y Apps con 1-Click REC (`NativeAppLauncherSection.kt`)**:
  - Detección asíncrona (`Dispatchers.IO`) de todos los juegos y aplicaciones descargadas en el smartphone mediante `PackageManager`.
  - Tarjetas interactivas con icono nativo y botón *"ABRIR Y GRABAR"* que abre el juego e inicia automáticamente la grabación sin pasos manuales.
  - Buscador en vivo y filtros por categoría (*Todos, Juegos, Apps*).
- [x] **Integración de Aceleración Gráfica con Shaders de GPU (GLSL ES 3.20 y SPIR-V Vulkan)**:
  - Tubería gráfica dual en C++23 (`GpuShaderPipeline.hpp/cpp`) vinculada a `GLESv3` y `EGL`.
  - Shaders para FaceCam circular con borde antialiased, Chroma Key en GPU y Gaming Color Boost (realce de saturación y contraste a 60/120 FPS).
- [x] **Motor de Streaming de Ultra Baja Latencia Unificado en Rust (Edition 2024 / `streamer.rs`)**:
  - Módulo nativo `rust/src/streamer.rs` con canales atómicos `crossbeam-channel` y *Ring Buffer* de 4 MB para absorber fluctuaciones de red móvil 4G/5G.
  - Reemplazo y descarte definitivo de Go en la app móvil: Cero pausas de Garbage Collector, huella de RAM mínima (<10 MB) y sin bibliotecas secundarias extras en el APK.
- **Buffer Pacing Asíncrono para Almacenamiento eMMC (Detección Automática)**:
  - [x] Detección automática de memorias eMMC mediante `/sys/block/mmcblk0` o `/proc/partitions`.
  - [x] Desacopla la escritura de muestras en `AMediaMuxer` mediante un hilo y cola amortiguadora (*coalescing* hasta 16 muestras) para evitar micro-tirones (*stutters*) por picos de latencia de I/O en memorias eMMC 5.1 durante escenas con partículas o monedas masivas en juegos (ej: Subway Surfers). Si se detecta almacenamiento UFS/NVMe, se mantiene la escritura directa ultrarrápida.
- **Afinidad de Hilos (Thread Affinity) a Núcleos de Eficiencia (LITTLE)**:
  - [x] Aislamiento de núcleos grandes de alto rendimiento (Big cluster, Cortex-A75) para el videojuego en ejecución mediante `sched_setaffinity` en Linux.
  - [x] Los hilos de codificación y compresión (`VeloRec_CppEncoder`, `VeloRec_Pacing`, `velorec_rust_worker`) quedan anclados a los núcleos de bajo consumo (LITTLE cluster, Cortex-A55), reduciendo la temperatura y garantizando que el juego no sufra caídas de FPS.
- **Captura de Audio Interno Puro sin Mezcla Indeseada**:
  - [x] Corrección de la tubería de audio interno en `ScreenRecordService` y `AudioRecordEngine`. En modo "Audio Interno", el Foreground Service solicita exclusivamente `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` (sin solicitar micrófono), evitando el icono de privacidad de Android 14+ y eliminando el enrutamiento parásito del micro.
  - [x] Soporte para selección directa de fuente de audio (Interno, Micrófono, Mezcla, Silencio) desde la pantalla de grabación y lanzador de juegos.
- **Multiplexación de Audio en Tiempo Real en C++23 (Eliminación del Paso 2)**:
  - [x] Transmisión en tiempo real de paquetes AAC (`nativeWriteAudioSample`) directamente al `AMediaMuxer` nativo en C++23. El archivo MP4 ya queda listo y sincronizado al terminar, sin requerir procesamiento posterior ni fusión secundaria de audio.
- **Grabación Directa sobre Descriptor de la Galería MediaStore (Eliminación del Paso 3)**:
  - [x] Creación de la entrada en MediaStore con `IS_PENDING = 1` previo al inicio de la grabación y pase directo de `rawFd` a `AMediaMuxer_new(fd, ...)`. Al detener, se finaliza con `IS_PENDING = 0` en <15 ms, eliminando por completo copias pesadas de cientos de megabytes en discos eMMC lentos.
- **Guardado en Segundo Plano con Notificación de Progreso Escalonado**:
  - [x] Desacople total del hilo de UI al presionar "Detener", con retroalimentación progresiva en la barra de estado (25% -> 75% -> 100%), eliminando cualquier sensación de lentitud o congelamiento.
- [x] **Integración de Ecosistema Avanzado de Rust (oboe, yuvutils-rs, glow)**:
  - Crate `oboe` (`audio_oboe.rs`) para audio nativo de latencia de estudio (<10 ms).
  - Crate `yuvutils-rs` (`color_yuv.rs`) con aceleración vectorial SIMD ARM NEON para 32 y 64 bits.
  - Crate `glow` (`gpu_glow.rs`) para sombreadores GPU OpenGL ES 3.20 multiplataforma en Rust puro.
- [x] **Suite de Pruebas Unitarias Deterministas**:
  - Validación con `node:test` de cálculo de bitrate, validación de paridad de resolución, temporizador de cuenta regresiva, validación de paquetes Android, detección de eMMC, cálculo de afinidad de hilos, especificaciones de shaders GPU y motor Go/Rust.

---

## 🚀 Fase 2: Transmisión en Vivo y Protocolos de Red (Q1 2027)

- [ ] **Streaming Nativo de Ultra Baja Latencia en Rust (Tokio / srt-tokio)**: Transmisión directa de pantalla hacia YouTube, Twitch o servidores RTMP/SRT utilizando Rust con cero pausas de Garbage Collector y eliminando la necesidad de empaquetar el runtime de Go.
- [ ] **Streaming RTMP / RTMPS**: Transmisión de pantalla en directo hacia plataformas con reconexión automática.
- [ ] **Protocolo SRT (Secure Reliable Transport)**: Protocolo de ultra baja latencia con corrección de paquetes (ARQ) sobre conexiones móviles inestables (4G/5G).
- [ ] **Soporte para Vulkan Video Extensions en C++23 / Ash**: Reemplazo directo de EGL por Vulkan Video Encode para teléfonos con Snapdragon 8 Gen 2/3/4 y Dimensity 9300+.
- [ ] **Adaptación Dinámica de Bitrate (ABR)**: Ajuste en tiempo real de la tasa de bits ante caídas de señal o picos de congestión de red.

---

## ✂️ Fase 3: Editor de Video en el Teléfono (Q2 2027)

- [ ] **Recorte Rápido sin Recodificar (Stream Copy)**: Cortar inicio y fin del video en menos de 1 segundo sin pérdida de calidad.
- [ ] **Superposición de Marca de Agua y Texto**: Renderizado acelerado por hardware para logotipos de canal o marcas de agua.
- [ ] **Generador de GIFs animados**: Exportación de fragmentos destacados de 5 a 15 segundos en formato GIF de alta velocidad.
- [ ] **Extracción y Mezcla de Pistas de Audio**: Separación de la pista de micrófono y la pista de audio interno del juego en archivos AAC independientes.

---

## 🎮 Fase 4: Modo Gaming y Telemetría de Alto Nivel (Q3 2027)

- [ ] **Monitor de Tasa de Refresco de Pantalla (`Choreographer`)**: Medición precisa de los FPS reales entregados por el juego con overlay flotante no intrusivo.
- [ ] **Protección Térmica Inteligente**:
  - Lectura de sensores térmicos del SoC mediante `ThermalManager` de Android.
  - Reducción dinámica de resolución o FPS si el procesador alcanza los 45°C para evitar caídas de rendimiento (*thermal throttling*).
- [ ] **Modo No Molestar Automático**: Bloqueo temporal de notificaciones emergentes y llamadas telefónicas durante la sesión de grabación.
- [ ] **Pausa Automática por Pantalla Apagada**: Pausa inteligente del codificador si la pantalla entra en reposo.

---

## 📦 Fase 5: Distribución Móvil y Tiendas de Terceros (Q4 2027)

- [ ] **Publicación Automática en Uptodown**: Integración de scripts para actualizar versiones APK directamente en el catálogo de Uptodown.
- [ ] **Soporte para F-Droid**: Empaquetado reproducible compatible con los estándares de compilación de F-Droid.
- [x] **Configuración Remota Dinámica (Server-Driven UI sin reinstalar APK)**: La app consulta asíncronamente `app_config.json` en los releases de GitHub (`https://github.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases`) con resiliencia offline para cambiar acentos, banners, anuncios y opciones en caliente.
- [x] **Flujo de Publicación Manual en GitHub Actions**: Action `publish-config-release.yml` con activación estricta manual (`workflow_dispatch`) para publicar el JSON a GitHub Releases.
- [ ] **Sistema de Actualizaciones In-App de APK Sin Google Play**: Comprobación de nuevas versiones con descargas de paquetes `.apk` con verificación de hash SHA-256.
