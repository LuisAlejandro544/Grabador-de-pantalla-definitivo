# 📱 VeloRec Studio — Grabador de Pantalla Móvil Nativo de Alto Rendimiento

> **Arquitectura móvil 100% nativa**: Kotlin (Jetpack Compose Material 3 & Corrutinas) + C++23 (AMediaCodec NDK) + Rust (Edition 2024 Zero-Copy).

VeloRec Studio es una aplicación **100% móvil nativa** de captura y codificación de pantalla para Android. Diseñada con interfaz táctil ergonómica y moderna basada en **Jetpack Compose (Material 3)** puro, elimina por completo los cuellos de botella de la máquina virtual (JVM/ART) mediante una tubería **Zero-Copy** que transfiere directamente las superficies de hardware de la GPU a los codificadores nativos en C++23 y Rust.

---

## ⚡ Características Principales

- **Diseño 100% Móvil Nativo (Sin Emuladores ni Marcos Web)**:
  - Interfaz táctil ergonómica adaptada para usarse con una sola mano en smartphones.
  - Vistas nativas construidas con **Kotlin Jetpack Compose (Material 3)** con arquitectura modular desacoplada en paquetes (`ui/screens`, `ui/components`, `ui/theme`, `data/repository`, `data/model` y `service`).
  - Retención de estado persistente ante rotaciones mediante **`MainViewModel` (StateFlow)**.
  - Paleta oscura de alto contraste, tipografía nítida y botones de gran área de toque.
- **Doble Motor de Codificación Nativo por Hardware**:
  - **⚡ C++23 Core Engine**: Acceso directo al Android NDK con `AMediaCodec_createInputSurface`, `std::expected` (manejo de errores de coste cero), `std::span`, `std::jthread` cooperativo y multiplexor MP4 de ultra baja latencia (~3.2 ms por cuadro).
  - **🦀 Rust Core Engine (Edition 2024 / Rust 1.85+)**: Garantía matemática de seguridad en memoria sin recolector de basura (Zero GC), canales atómicos lock-free con `crossbeam-channel` y ausencia total de condiciones de carrera (*Data Races*).
  - **Alternancia Dinámica**: El usuario puede alternar entre ambos motores nativos con un toque en tiempo real.
- **Tubería Zero-Copy en Kotlin**:
  - Kotlin solo orquesta permisos y ciclo de vida; no procesa bitmaps ni aloca buffers pesados en memoria Java.
  - Implementación con **Kotlin Coroutines (`Dispatchers.IO`)** para prevenir cualquier congelamiento del hilo principal (*ANR*).
- **Cumplimiento Estricto con Android 14+ (API 34/35)**:
  - Servicio en primer plano configurado con `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`.
  - `minSdkVersion = 26` (Android 8.0 Oreo) y `targetSdkVersion = 35` (Android 15).
- **Soporte Multiarquitectura (32 y 64 bits) y Compilación Selectiva en CI**:
  - `arm64-v8a`: Estándar de 64 bits para teléfonos modernos.
  - `armeabi-v7a`: Soporte de 32 bits para dispositivos antiguos.
  - `x86_64`: Emuladores de desarrollo y Chromebooks.
  - **Selector de Arquitectura en GitHub Actions**: Permite compilar el APK **Universal** (todas las arquitecturas) o un **APK individual** específico (`arm64-v8a`, `armeabi-v7a` o `x86_64`) para reducir drásticamente el peso de descarga del archivo.
- **Temporizador de Cuenta Regresiva de Inicio**:
  - Configurable en 0s (inmediata), 3s (ideal/recomendada), 5s y 10s.
  - Overlay visual táctil de cuenta atrás con botón de cancelación instantánea para dar tiempo al usuario de cambiar a la app o juego antes de grabar.
- **Resoluciones Proporcionales Adaptadas al Smartphone con Selector de Escala Nativa**:
  - Detección en vivo de la relación de aspecto nativa del dispositivo (ej. 20:9, 19.5:9, 18:9) con cálculo automático de paridad de píxeles (`width % 2 == 0 && height % 2 == 0`) eliminando desajustes de pantalla y barras verdes en codificadores de hardware.
  - **Múltiples Resoluciones en Proporción Nativa**: Permite al usuario elegir diferentes calidades ocupando el 100% de la pantalla sin deformación:
    - **Nativa 100% (Pantalla Completa 1:1)**: Resolución nativa total del panel táctil (ej. 1080x2400 px).
    - **Nativa FHD Adaptada (1080p)**: Nitidez excelente con menor temperatura.
    - **Nativa HD Adaptada (720p)**: Equilibrada para partidas largas y tutoriales.
    - **Nativa Ahorro / Gaming (540p)**: Máxima fluidez sin calentamiento en celulares de gama media y baja.
    - **Nativa 2K Ultra (1440p)**: Máxima nitidez para pantallas Quad HD+.
- **Integración Nativa con Google libyuv (C++23 & SIMD ARM NEON)**:
  - Infraestructura lista con la biblioteca oficial **`libyuv` de Google (Licencia BSD)** acelerada por hardware SIMD (ARM NEON para 32 bits `armeabi-v7a` y 64 bits `arm64-v8a`) para conversiones de color ultrarrápidas (RGBA a YUV420 Planar / NV12) y escalado bilineal continuo.
- **Control Total de Video y Audio**:
  - Resoluciones predefinidas (Modo Nativo de Pantalla con escalas y Estándar 9:16: 1080p FHD, 720p HD, 480p SD, 1440p 2K, 4K UHD) y resolución personalizada con comprobación de paridad YUV420p.
  - Tasas de cuadro: 24, 30, 60, 90 y 120 FPS.
  - Bitrate ajustable de 2 a 50 Mbps con cálculo de almacenamiento en vivo.
  - Códecs por hardware: H.264 (AVC), H.265 (HEVC) y AV1.
- **Buffer Pacing Asíncrono para Almacenamiento eMMC (Detección Automática)**:
  - Detección automática en hardware de memorias eMMC mediante `/sys/block/mmcblk0` y `/proc/partitions`.
  - Desacopla la escritura en `AMediaMuxer` utilizando un hilo de fondo y una cola de amortiguación por lotes (*coalescing* de hasta 16 muestras).
  - Absorbe la latencia de escritura de memorias eMMC 5.1 durante escenas con partículas o monedas masivas en videojuegos (ej: Subway Surfers), erradicando micro-congelamientos (*stutters*).
  - En dispositivos con memorias UFS o NVMe, el sistema omite la cola y realiza escritura directa de ultra-alta velocidad.
- **Afinidad de Hilos de CPU (Thread Affinity) hacia Núcleos de Eficiencia (LITTLE)**:
  - En procesadores ARM big.LITTLE (ej: MediaTek Helio G81 / G85 / G88, Qualcomm Snapdragon), ancla mediante `sched_setaffinity` los hilos de codificación y vaciado (`VeloRec_CppEncoder`, `VeloRec_Pacing`, `velorec_rust_worker`) exclusivamente a los núcleos de bajo consumo (Cortex-A55).
  - Reserva los núcleos grandes de alto rendimiento (Big, Cortex-A75) **100% libres y disponibles para el videojuego**, evitando pérdidas de FPS o caídas térmicas.
- **Captura de Audio Interno Puro y Mezcla Dual Inteligente**:
  - Corrección de la tubería de audio en `ScreenRecordService` y `AudioRecordEngine`: en modo "Audio Interno", el servicio en primer plano solicita exclusivamente `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` (sin solicitar micrófono), suprimiendo el indicador de privacidad de Android 14+ y eliminando el enrutamiento parásito del micro.
  - Generación de silencio de confort mono/estéreo durante pausas del juego para mantener sincronización estricta Audio/Video.
  - Selector táctil de acceso rápido (1 toque) en la pantalla principal para cambiar entre Audio Interno, Micrófono, Mezcla y Silencio.
- **Grabación Directa NDK C++23 / Rust 2024 (Zero-Copy & GC-Free)**:
  - **Sin Impacto del Recolector de Basura (GC-Free)**: Los fotogramas de la GPU se canalizan directamente a `ANativeWindow` de hardware y se empaquetan en C++23 / Rust 2024 con `AMediaMuxer` en hilos de fondo dedicados. Cero pausas de GC durante la captura.
  - **Multiplexación de Audio en Tiempo Real en C++23 (Eliminación del Paso 2)**: Transmisión directa de paquetes AAC desde Kotlin a `AMediaMuxer` entrelazados en tiempo real. Al parar la grabación, el archivo MP4 ya contiene video y audio sincronizados sin necesidad de procesos de fusión posteriores.
  - **Alineación con Android 15 (API 35+)**: Compilación nativa optimizada para arquitecturas de 64 y 32 bits (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
- **Grabación Directa sobre Descriptor de la Galería MediaStore (Eliminación del Paso 3)**:
  - La entrada en la galería se crea al inicio (`IS_PENDING = 1`) y su descriptor nativo (`rawFd`) se pasa a C++23 (`AMediaMuxer_new(fd, ...)`). Al detener, el archivo ya está en `Movies/VeloRec`; solo se remueve la marca pendiente (<15 ms), erradicando las copias de disco de 300+ MB en memorias eMMC lentas.
- **Guardado en Segundo Plano con Notificación de Progreso Escalonado**:
  - Al presionar "Detener", la interfaz se libera de inmediato (`isRecording = false`) y una notificación dinámica muestra el progreso en segundo plano (25% deteniendo codificador -> 75% finalizando contenedor -> 100% video disponible en galería), garantizando cero cuelgues o congelamientos ANR.
- **Publicación Directa en la Galería del Sistema (`Movies/VeloRec`)**:
  - Las grabaciones se indexan al instante con `MediaScannerConnection` para aparecer de inmediato en Google Fotos, Galería Samsung, Xiaomi, Motorola, VLC o WhatsApp.
- **🎮 Lanzador Rápido de Juegos y Aplicaciones (1-Click REC)**:
  - Nueva sección táctil dedicada (`NativeAppLauncherSection.kt`) en la barra de navegación inferior.
  - Lista de forma asíncrona (`Dispatchers.IO`) todos los juegos y aplicaciones descargadas en el teléfono con su icono oficial y badge de categoría.
  - **Apertura y Grabación Instantánea**: Al tocar un juego (ej. Free Fire, PUBG, COD Mobile, Brawl Stars), la app inicia la captura con la resolución/FPS seleccionados y abre el juego inmediatamente para comenzar a jugar y grabar sin pasos intermedios.
  - Buscador en tiempo real y filtros rápidos (*Todos, Juegos, Apps*).
- **🐹 Motor de Streaming en Go (Golang 1.22+)**:
  - Infraestructura lista en `/go/streamer.go` para transmisión de ultra baja latencia (RTMP, RTMPS, SRT y WebRTC).
  - Despachador de paquetes NAL con *goroutines*, colas concurrentes y *Ring Buffer* de 4 MB para absorber fluctuaciones de red móvil 4G/5G.
  - Tarea Gradle `goStreamerCheck` y compilación en GitHub Actions con `setup-go@v5`.
- **🎨 Aceleración Gráfica con Shaders de GPU (GLSL ES 3.20 y SPIR-V Vulkan)**:
  - Tubería gráfica dual en C++23 (`GpuShaderPipeline.hpp` & `.cpp`) enlazada con `GLESv3` y `EGL`.
  - Shaders GLSL ES 3.20 y bytecode binario SPIR-V (`SpirvShaderBytecode.hpp`) para renderizado a 60/120 FPS sin consumo de CPU:
    - `facecam_circular.frag`: Cámara flotante con máscara circular antialiased, borde dinámico y Chroma Key (pantalla verde) en GPU.
    - `gaming_color_boost.frag`: Realce de contraste dinámico, brillo y saturación cromática para videojuegos.
    - `passthrough.vert`: Vertex shader passthrough de alta velocidad.
- **Herramientas de Superposición**:
  - Cámara flotante FaceCam ajustable.
  - Efectos visuales de toques en pantalla.
- **Galería Integrada**:
  - Lista unificada de grabaciones públicas y privadas, reproductor nativo in-app sin bloqueos, control de velocidad y eliminación sincronizada con MediaStore.

---

## 🏗️ Flujo de Arquitectura

```
[Kotlin Jetpack Compose Material 3 UI] 
       │ (Material 3 Dark Theme + State Hoisting)
       ▼
[MainActivity & ScreenRecordService] 
       │ (Corrutinas Dispatchers.IO + MediaProjection Android 14/15)
       ▼
[Android ScreenRecordService] 
       │ (Foreground Service tipo mediaProjection en Android 14+)
       ▼
[JNI Native Interface]
 ┌─────┴─────────────────────────────────┐
 ▼                                       ▼
[C++23 Engine: ANativeWindow]          [Rust 2024 Engine: Surface]
 └─────┬─────────────────────────────────┘
       ▼
[Hardware Encoder (AMediaCodec H.264/HEVC/AV1)]
       ▼
[Archivo MP4 / Galería de Dispositivo]
```

---

## 🚀 Compilación del APK Debug (Sin Necesidad de PC)

Este proyecto incluye un flujo de **GitHub Actions** configurado con activación **100% MANUAL (`workflow_dispatch`)**, lo que te permite compilar el APK directamente desde tu teléfono móvil sin instalar nada en tu dispositivo:

1. **Sube el código a tu repositorio de GitHub**:
   - Descarga el ZIP desde la pestaña **Código** de la aplicación y súbelo a un repositorio (público o privado).
2. **Abre la pestaña Actions**:
   - Desde el navegador de tu celular en GitHub, toca la pestaña **Actions**.
3. **Ejecuta el Flujo Manual**:
   - Selecciona **"Compilar APK Debug (VeloRec Mobile)"**.
   - Pulsa el botón **"Run workflow"** y selecciona:
     - **Motor nativo**: C++23 o Rust 2024.
     - **Arquitectura de destino**:
       - `all` (Universal: compatible con cualquier teléfono y emulador).
       - `arm64-v8a` (Solo 64-bit ARM: APK ligero optimizado para smartphones actuales).
       - `armeabi-v7a` (Solo 32-bit ARM: para celulares antiguos).
       - `x86_64` (Solo emuladores de Android Studio / PC).
4. **Descarga el APK**:
   - En 3-4 minutos, GitHub Actions terminará la compilación.
   - En la sección **Artifacts** descarga el archivo resultante (`VeloRec-Debug-APK-Universal`, `VeloRec-Debug-APK-arm64-v8a`, etc.) e instálalo de inmediato en tu celular o súbelo a Uptodown.

---

## 🛡️ Auditoría y Análisis de Código para Producción (Lint & Seguridad)

Para garantizar la estabilidad del APK en tiendas de terceros como Uptodown y prevenir crasheos inesperados en celulares de usuarios, dispones del flujo de GitHub Actions **"Auditoría y Análisis de Código para Producción"** (`.github/workflows/code-analysis-audit.yml`), ejecutable manualmente (`workflow_dispatch`) desde tu teléfono:

1. **Análisis Estático Multilenguaje**:
   - **Kotlin & Android Lint**: Analiza fugas de memoria, llamadas bloqueantes en el hilo principal de la UI, cumplimiento estricto de permisos en Android 14/15 (`FOREGROUND_SERVICE_MEDIA_PROJECTION`) y compatibilidad con `minSdk 26`.
   - **C++23 & libyuv (`cppcheck`)**: Inspección de punteros crudos, desbordamientos de buffer (*buffer overflow*) y fugas de memoria en la tubería `ANativeWindow`.
   - **Rust Edition 2024 (`cargo clippy` & `cargo check`)**: Detección de anti-patrones, posibles pánicos (`unwrap` inseguros) y validación de seguridad de memoria concurrente para ARM64.
   - **TypeScript & Lógica Pura**: Comprobación de tipos estrictos (`tsc --noEmit`) y suite de tests unitarios matemáticos (`recorderCore.test.ts`).
2. **Reporte Ejecutivo para Descarga Móvil**:
   - Genera el artefacto `Reporte-Auditoria-Produccion-VeloRec` con un informe legible en formato Markdown (`AUDIT_SUMMARY.md`) y los reportes detallados de Android Lint en HTML para inspeccionarlos directamente desde el smartphone.

---

## 🎨 Actualizaciones Dinámicas de Diseño (Sin Reinstalar APK)

Para evitar la molestia de tener que reinstalar un nuevo APK cada vez que cambias un color, un texto, un aviso o una opción de grabación, VeloRec Studio implementa un sistema **Server-Driven UI (Configuración Remota)**:

1. **Lectura Inteligente desde GitHub Releases**:
   - Al abrirse, la aplicación consulta asíncronamente en segundo plano los últimos releases de tu repositorio:  
     [`https://github.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases`](https://github.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases)
   - Si detecta un archivo `app_config.json` actualizado, aplica dinámicamente el color de acento, banners informativos, anuncios emergentes y parámetros de grabación sin tocar el APK instalado.
2. **Resiliencia Total (La app NO está atada al JSON)**:
   - Si el dispositivo está sin conexión a internet, en modo avión o GitHub tiene problemas de red, la app **funciona al 100% de inmediato con sus valores por defecto nativos**, sin esperas ni bloqueos.
3. **Flujo Manual de Publicación con GitHub Actions**:
   - Dispones del flujo manual **"Publicar Configuración y Diseño a GitHub Releases"** (`.github/workflows/publish-config-release.yml`).
   - **Disparo 100% manual (`workflow_dispatch`)**: No se activa con commits, push ni pull requests para no consumir cuotas de GitHub.
   - Valida el JSON y lo adjunta automáticamente al último release de GitHub para que todos los usuarios de la app reciban los cambios de diseño.

---

## 💻 Compilación Local (Para Desarrolladores)

Si cuentas con Android Studio y las herramientas NDK en tu entorno local:

```bash
# 1. Instalar dependencias del proyecto
npm install

# 2. Compilar APK Debug para Android
cd android
./gradlew assembleDebug

# 3. El archivo APK resultante se ubicará en:
# android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Licencia

Este proyecto está bajo licencia permisiva **MIT / Apache-2.0**, permitiendo libre uso, distribución e instalación sin imponer requisitos de código abierto de tipo copyleft.
