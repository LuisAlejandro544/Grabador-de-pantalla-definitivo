# 🧠 AI Context — VeloRec Studio

Este documento proporciona contexto técnico estructurado y reglas invariantes para modelos de lenguaje (LLMs) y sistemas de asistencia que interactúen con el repositorio de **VeloRec Studio**.

---

## 🎯 Propósito del Proyecto

VeloRec Studio es una aplicación móvil de grabación de pantalla de alto rendimiento diseñada para Android. Utiliza una arquitectura en cuatro capas para maximizar la tasa de cuadros por segundo (FPS) y minimizar el consumo térmico y de batería:

1. **Frontend / Presentación**: Aplicación móvil 100% nativa con **Kotlin Jetpack Compose (Material 3)** organizada modularmente (`ui/screens`, `ui/components`, `ui/theme`, `data/repository`, `data/model`) con Server-Driven UI dinámico desde GitHub Releases y **Lanzador Rápido de Juegos/Apps (1-Click REC)**.
2. **Orquestación en Android**: Kotlin con Arquitectura MVVM (`MainViewModel` con StateFlow), Corrutinas (`Dispatchers.IO`), gestión modular de permisos `MediaProjection`, y `ScreenRecordService` con `RecordingNotificationHelper`.
3. **Motores de Codificación Nativa (Hardware NDK)**:
   - **C++23**: `AMediaCodec_createInputSurface`, `std::expected`, `std::span`, `std::jthread`, `ANativeWindow`.
   - **Rust (Edition 2024)**: Crate con `crossbeam-channel`, seguridad de memoria garantizada en tiempo de compilación y enlaces JNI.
4. **Aceleración Gráfica y Protocolos de Red (Go & Shaders GPU)**:
   - **Shaders GPU (GLSL ES 3.20 & SPIR-V Vulkan)**: Tubería C++23 (`GpuShaderPipeline`) para FaceCam circular antialiased, Chroma Key (pantalla verde) y Gaming Color Boost en la GPU a 60/120 FPS sin consumo de CPU.
   - **Go (Golang 1.22+)**: Infraestructura de streaming (`/go/streamer.go`) con *goroutines*, colas de paquetes NAL y *Ring Buffer* de 4 MB para protocolos RTMP/SRT.
5. **Grabación Directa NDK y Multiplexación Nativa de Android**:
   - Codificación directa de hardware a MP4 sin intermediación de la JVM/ART ni recolección de basura (GC-Free).
   - Multiplexación sin pérdida de audio y video mediante `AudioMuxerHelper` (`MediaMuxer` y `MediaExtractor` oficiales de Android) con guardado directo en Galería (`Movies/VeloRec`).
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
12. **Buffer Pacing Asíncrono para eMMC (Detección Automática)**:
   - Auto-detección mediante `/sys/block/mmcblk0` o `/proc/partitions`. Si es eMMC 5.1, desacopla la escritura en `AMediaMuxer` mediante un hilo y cola de amortiguación por lotes (hasta 16 muestras) para evitar micro-congelamientos ante ráfagas de I/O en juegos. En memorias UFS/NVMe realiza escritura directa.
13. **Afinidad de Hilos (Thread Affinity) hacia Núcleos de Eficiencia (LITTLE)**:
   - En procesadores móviles big.LITTLE (ej: Helio G81/G85/G88, Snapdragon), los hilos de fondo (`VeloRec_CppEncoder`, `VeloRec_Pacing`, `velorec_rust_worker`) se fijan a los núcleos de bajo consumo (Cortex-A55) mediante `sched_setaffinity`, dejando los núcleos Big (Cortex-A75) 100% disponibles para el videojuego (Subway Surfers, Free Fire).
14. **Multiplexación de Audio en Tiempo Real en C++23 (Eliminación del Paso 2)**:
   - El motor de audio en Kotlin pasa los paquetes AAC en tiempo real a `AMediaMuxer` vía JNI (`nativeWriteAudioSample`). Al detener la captura, el contenedor MP4 ya está completamente sincronizado, eliminando la necesidad de fusionar archivos temporales secundarios en diferido.
15. **Grabación Directa sobre Descriptor de la Galería MediaStore (Eliminación del Paso 3)**:
   - Se crea el destino en MediaStore con `IS_PENDING = 1` previo a la captura y se suministra el descriptor nativo (`targetFd`) al multiplexor C++23. Al terminar, solo se actualiza `IS_PENDING = 0` en <15 ms, erradicando por completo copias byte a byte de 300+ MB en almacenamientos eMMC lentos.
16. **Guardado en Segundo Plano con Notificación de Progreso Escalonado**:
   - Al tocar "Detener", la interfaz responde de inmediato cambiando su estado a inactivo (`isRecording = false`), mientras una corrutina en `Dispatchers.IO` procesa el cierre y emite notificaciones de progreso (25% -> 75% -> 100%) para evitar cuelgues o bloqueos del hilo principal.

---

## 📊 Estado de los Datos y Almacenamiento

- Las grabaciones se guardan en el álbum público `Movies/VeloRec` (accesible desde Google Fotos, Galería Samsung, Xiaomi, etc.) y se respaldan en el almacenamiento local.
- Las opciones de configuración persisten en `localStorage` / `SharedPreferences` con fallback automático a valores seguros por defecto (1080p @ 60 FPS, 12 Mbps, C++23).
- El validador de resoluciones exige que ancho y alto sean números pares para compatibilidad con codificadores por hardware YUV420p (`width % 2 === 0 && height % 2 === 0`).
