# 📁 Estructura del Proyecto — VeloRec Studio

Árbol de directorios detallado y explicación técnica de cada módulo del grabador de pantalla móvil híbrido.

---

## 🌳 Árbol de Archivos General

```
/
├── .github/
│   └── workflows/
│       ├── build-apk.yml               # GitHub Actions para compilar APK Debug manualmente con selector de ABI (universal o individual)
│       ├── release-apk-stable.yml      # GitHub Actions activado por Release con sufijo '-E' para publicar APKs automáticamente
│       ├── code-analysis-audit.yml     # GitHub Actions para auditoría de producción y análisis estático (Kotlin Lint, Cppcheck, Clippy, TS)
│       └── publish-config-release.yml  # GitHub Actions para publicar app_config.json en GitHub Releases (workflow_dispatch)
│
├── CHANGELOG_ESTABLE.md                # Registro oficial de cambios y notas de la versión estable v1.0.0-E
├── android/                             # Estructura del proyecto Android nativo completa
│   ├── gradle/
│   │   └── wrapper/
│   │       └── gradle-wrapper.properties # Configuración de Gradle Wrapper v8.7.0
│   ├── gradlew                         # Script ejecutable POSIX para compilar en Linux/macOS
│   ├── gradlew.bat                     # Script batch para compilar en Windows
│   ├── settings.gradle.kts             # Repositorios Maven y vinculación del módulo :app
│   ├── build.gradle.kts                # Configuración raíz de Gradle Kotlin DSL
│   └── app/
│       ├── build.gradle.kts            # Configuración de :app con Jetpack Compose, CMake C++23 y Cargo NDK
│       └── src/main/
│           ├── AndroidManifest.xml     # Permisos MediaProjection y ForegroundService Android 14+
│           ├── res/                    # Recursos de la aplicación (iconos adaptativos, drawables, strings, styles)
│           │   ├── drawable/           # Vectores nativos ic_launcher_background y ic_launcher_foreground
│           │   ├── mipmap/             # Iconos de inicio para la aplicación móvil
│           │   ├── mipmap-anydpi-v26/  # Iconos adaptativos para Android 8.0+
│           │   └── values/             # strings.xml, styles.xml y colors.xml
│           ├── java/com/velorec/recorder/
│           │   ├── MainActivity.kt          # Punto de entrada de ComponentActivity con StateFlow/ViewModel
│           │   ├── VeloRecComposeUI.kt      # Fachada modular retrocompatible de Jetpack Compose
│           │   ├── ScreenRecordService.kt   # Foreground Service con Surface nativa Zero-Copy y JNI
│           │   ├── data/
│           │   │   ├── model/
│           │   │   │   ├── RecordingConfig.kt   # Modelos de configuración, presets adaptativos, cuenta regresiva y bitrate
│           │   │   │   └── RemoteUiConfig.kt    # Modelos Server-Driven UI (app_config.json)
│           │   │   └── repository/
│           │   │       └── RemoteConfigRepository.kt # Carga asíncrona segura de GitHub Releases (Dispatchers.IO)
│           │   ├── audio/
│           │   │   ├── AudioRecordEngine.kt     # Captura de micrófono, audio interno (API 29+) y mezcla PCM 16-bit
│           │   │   └── AudioMuxerHelper.kt      # Multiplexación nativa oficial de Android (MediaMuxer) en <200ms
│           │   ├── storage/
│           │   │   └── GalleryStorageManager.kt # Publicación en Galería nativa (Movies/VeloRec) vía MediaStore
│           │   ├── service/
│           │   │   ├── RecordingNotificationHelper.kt # Gestión de canal y notificaciones de Foreground Service
│           │   │   ├── HardwareStorageDetector.kt     # Detección de tecnología de almacenamiento (eMMC vs UFS/NVMe)
│           │   │   ├── RecordingCountdownHelper.kt    # Temporizador de cuenta atrás previo en primer plano
│           │   │   └── RecordingSessionTimer.kt       # Cronómetro de sesión asíncrono y desacoplado
│           │   └── ui/
│           │       ├── MainViewModel.kt     # ViewModel de retención de estado de grabación y cronómetro
│           │       ├── VeloRecMainScreen.kt # Scaffold principal, TopAppBar y navegación inferior
│           │       ├── theme/
│           │       │   └── Color.kt         # Paleta oscura Material 3 y parseColorSafe
│           │       ├── components/
│           │       │   ├── MetricBadge.kt   # Componente de telemetría de hardware
│           │       │   ├── RemoteAnnouncementDialog.kt # Diálogo modal para anuncios remotos
│           │       │   ├── AppUpdateDialog.kt # Diálogo modal táctil para actualización de APK con notas y blindaje de grabaciones
│           │       │   └── recorder/        # Módulos desacoplados del visor de grabación
│           │       │       ├── QuickQualityCard.kt     # Tarjeta de ajustes rápidos de resolución, bitrate y audio
│           │       │       ├── RecorderHudCard.kt      # Visor tipo HUD con cronómetro y animaciones de pulso
│           │       │       ├── HardwareTelemetryRow.kt # Fila de métricas de CPU, latencia, RAM y Zero-Copy
│           │       │       └── RecorderActionButtons.kt# Botones ergonómicos de gran área táctil (REC / Pausa)
│           │       └── screens/
│           │           ├── OnboardingScreen.kt  # Pantalla de inducción táctil de 4 pasos con solicitud guiada de permisos
│           │           ├── RecorderScreen.kt    # Orquestador modular del visor principal de captura
│           │           ├── AppLauncherScreen.kt # Lanzador rápido de juegos y apps descargadas con 1-Click REC
│           │           ├── SettingsScreen.kt    # Orquestador de navegación modular con BackHandler
│           │           ├── settings/            # Subpantallas técnicas independientes y desacopladas
│           │           │   ├── SettingsCommon.kt             # Tipos comunes, enum SettingsSubScreen y SubScreenHeader
│           │           │   ├── SettingsMainMenu.kt           # Menú principal con accesos y estado Server-Driven UI
│           │           │   ├── ResolutionSettingsSubScreen.kt# Pestañas Nativas, Estándar y Personalizada (Paridad)
│           │           │   ├── BitrateSettingsSubScreen.kt   # Deslizador continuo 1-60M, cálculo de espacio y antiahogamiento
│           │           │   ├── FpsSettingsSubScreen.kt       # Presets 24, 30, 60, 90 y 120 FPS
│           │           │   ├── CodecSettingsSubScreen.kt     # Códecs por hardware H.264, HEVC y AV1
│           │           │   ├── AudioSettingsSubScreen.kt     # Fuentes de audio: interno, mic, mezcla y silencio
│           │           │   ├── CountdownSettingsSubScreen.kt # Temporizadores 0s, 3s, 5s y 10s
│           │           │   └── EngineSettingsSubScreen.kt    # Motor NDK Rust 2024 Zero-Copy vs C++23
│           │           ├── GalleryScreen.kt     # Galería de videos grabados con FileProvider
│           │           └── BenchmarkScreen.kt   # Telemetría comparativa C++23 vs Rust 2024
│           ├── shaders/                         # Aceleración gráfica por hardware en GPU
│           │   ├── glsl/                        # Shaders de texto GLSL ES 3.20 (FaceCam circular, Gaming Color Boost, Passthrough)
│           │   └── spirv/                       # Shaders en bytecode compilado SPIR-V (SpirvShaderBytecode.hpp) para Vulkan
│           └── cpp/
│               ├── CMakeLists.txt          # Configuración CMake para C++23 (-std=c++23, -O3), libyuv y GLESv3/EGL
│               ├── GpuShaderPipeline.hpp   # Tubería C++23 para sombreadores GPU (GLSL ES 3.20 y SPIR-V)
│               ├── GpuShaderPipeline.cpp   # Implementación de compilación, enlace y uniforms de FaceCam/Gaming
│               ├── libyuv_bridge.hpp       # Interfaz C++23 para aceleración SIMD ARM NEON de Google libyuv (32 y 64 bits)
│               └── libyuv_bridge.cpp       # Implementación de conversiones de color YUV420 y escalado bilineal con libyuv
│
├── rust/                               # Motor nativo unificado en Rust (Edition 2024)
│   ├── Cargo.toml                      # Manifiesto de Rust con edition = "2024", oboe, yuvutils-rs y glow
│   └── src/
│       ├── lib.rs                      # Fachada raíz del crate cdylib con reexportaciones y pruebas unitarias
│       ├── encoder.rs                  # Hardware encoder NDK AMediaCodec, ANativeWindow y bucle worker
│       ├── muxer.rs                    # AMediaMuxer, SharedMuxerState, T0 PTS normalizado y búfer preventivo
│       ├── cpu_affinity.rs             # Afinidad a núcleos de bajo consumo LITTLE (sched_setaffinity)
│       ├── jni_bindings.rs             # Firmas JNI exportadas para Java_com_velorec_recorder_ScreenRecordService_*
│       ├── audio_oboe.rs               # Integración de Google Oboe para captura de audio nativo (<10ms)
│       ├── color_yuv.rs                # Conversión de color YUV420/NV12 y RGBA con SIMD ARM NEON (yuvutils-rs)
│       ├── gpu_glow.rs                 # Tubería de sombreadores GPU OpenGL ES 3.20 en Rust puro (glow)
│       ├── resilience.rs               # Guardián anti-LMK, malloc_trim, prioridad nice -16, ioprio y señales de emergencia
│       └── streamer.rs                 # Motor de streaming en vivo (RTMP/SRT) en Rust con Ring Buffer (Zero-GC)
│
├── src/                                # Código fuente de la página web oficial (Landing Page)
│   ├── components/                     # Componentes de la landing page de descarga
│   │   └── landing/                    # Secciones de presentación orientadas a usuarios comunes
│   │       ├── Navbar.tsx              # Barra de navegación con botón de descarga del APK
│   │       ├── HeroSection.tsx         # Titular, ventajas sin anuncios y mockup del teléfono
│   │       ├── FeaturesSection.tsx     # Ventajas clave para gamers y creadores (sin lag, 120 FPS)
│   │       ├── ComparisonSection.tsx   # Comparativa directa y calculadora de ahorro de espacio
│   │       ├── RecordingModesSection.tsx # Modos de grabación (Gaming, Tutoriales, Redes)
│   │       ├── InstallationGuideSection.tsx # Guía de instalación en 3 pasos sencillos
│   │       ├── FAQSection.tsx          # Preguntas frecuentes sobre el APK
│   │       ├── Footer.tsx              # Pie de página y enlaces a releases de GitHub
│   │       └── DownloadModal.tsx       # Modal de descarga del instalador APK
│   │
│   ├── types/
│   │   └── recorder.ts                 # Tipos TypeScript e interfaces de resoluciones
│   │
│   ├── utils/                          # Lógica pura y pruebas en CI
│   │   ├── recorderCore.ts             # Funciones matemáticas de bitrate y validación
│   │   └── recorderCore.test.ts        # Tests unitarios deterministas con node:test
│   │
│   ├── App.tsx                         # Página principal de bienvenida y descarga
│   ├── index.css                       # Estilos globales de Tailwind CSS v4 y fuentes
│   └── main.tsx                        # Punto de entrada de React 19
│
├── index.html                          # HTML con viewport optimizado para móviles
├── app_config.json                     # Configuración remota dinámica Server-Driven UI (GitHub Releases)
├── metadata.json                       # Metadatos del applet en Google AI Studio
├── package.json                        # Dependencias de React, Vite, Motion, JSZip y Tailwind
├── tsconfig.json                       # Configuración de TypeScript ES2022
├── README.md                           # Documentación general del proyecto
├── ROADMAP.md                          # Plan de desarrollo y futuras fases
├── STRUCTURE.md                        # Explicación de la arquitectura de archivos (este archivo)
├── AI_CONTEXT.md                       # Contexto técnico para modelos de lenguaje
├── AGENTS.md                           # Guía operativa para agentes de desarrollo
└── README_AI.md                        # Resumen ejecutivo para asistentes de IA
```

---

## 🔍 Explicación de Carpetas y Módulos Clave

### 1. `/android` (Capa Android Nativa Completa)
Contiene la configuración de Gradle Kotlin DSL (`build.gradle.kts`) lista para compilar de forma autónoma:
- **Gradle Wrapper**: Incluye los ejecutables `gradlew` (Linux/macOS), `gradlew.bat` (Windows) y `gradle-wrapper.properties` configurado con Gradle 8.7.
- **Configuración Raíz y Módulos**: `settings.gradle.kts` y `build.gradle.kts` vinculando el módulo `:app`.
- **CMake**: Compila la tubería gráfica `GpuShaderPipeline.cpp` y Google `libyuv_bridge.cpp` con el estándar C++23 (`-std=c++23`).
- **Cargo NDK**: Compila el módulo Rust generando las bibliotecas `.so` para arquitecturas de 64 bits (`arm64-v8a`), 32 bits (`armeabi-v7a`) y `x86_64`.
- **Servicio y Módulos de Soporte**:
  - `ScreenRecordService.kt`: Servicio en primer plano `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` con Corrutinas Kotlin (`Dispatchers.IO`) y enlaces nativos NDK.
  - `service/HardwareStorageDetector.kt`: Detección de tecnología de almacenamiento físico (eMMC vs UFS).
  - `service/RecordingCountdownHelper.kt`: Cuenta atrás previa no bloqueante en el Foreground Service.
  - `service/RecordingSessionTimer.kt`: Cronómetro de duración desacoplado.

### 2. `/rust` (Motor Rust Edition 2024 con Hardware Encoder y Módulos de Alto Rendimiento)
Crate autónomo con `crate-type = ["cdylib", "rlib"]` estructurado de forma modular:
- **`lib.rs`**: Fachada raíz y orquestador del crate que reexporta todos los tipos y preserva pruebas unitarias.
- **`encoder.rs`**: Inicialización de `AMediaCodec` de hardware, `AMediaFormat`, creación de la `ANativeWindow Surface` (Zero-Copy) y bucle worker con `crossbeam-channel`.
- **`muxer.rs`**: Estado compartido `SharedMuxerState` para `AMediaMuxer`, entrelazado de video y audio AAC en tiempo real y sincronización PTS T0 estricta.
- **`cpu_affinity.rs`**: Fijación de hilos a núcleos LITTLE (Cortex-A55) mediante `sched_setaffinity`.
- **`jni_bindings.rs`**: Implementación de firmas exportadas JNI (`Java_com_velorec_recorder_ScreenRecordService_*`) con `#[unsafe(no_mangle)]` cumpliendo con Rust Edition 2024.
- **`audio_oboe.rs`**: Captura de audio de ultra baja latencia (<10 ms) con AAudio/OpenSL ES.
- **`color_yuv.rs`**: Conversión de color acelerada con SIMD ARM NEON (yuvutils-rs).
- **`gpu_glow.rs`**: Sombreadores GPU OpenGL ES 3.20 en Rust puro (glow).
- **`resilience.rs`**: Guardián de resiliencia anti-LMK, purga periódica `malloc_trim(0)`, prioridad `nice -16`, E/S de tiempo real `SYS_ioprio_set`, fijación de memoria física `mlock` y manejador de señales de emergencia (`SIGTERM`/`SIGINT`).
- **`streamer.rs`**: Motor de streaming en vivo RTMP/SRT de ultra baja latencia con Ring Buffer (Zero-GC).

### 3. `/android/.../ui` (Componentes y Pantallas Jetpack Compose Material 3)
- **`screens/RecorderScreen.kt`**: Orquestador visual del visor principal de captura.
- **`components/recorder/`**:
  - `QuickQualityCard.kt`: Ajustes rápidos de resolución, bitrate continuo y audio.
  - `RecorderHudCard.kt`: HUD de cámara con animaciones de pulso y cronómetro en vivo.
  - `HardwareTelemetryRow.kt`: Métricas en vivo de CPU, latencia NDK, RAM y Zero-Copy.
  - `RecorderActionButtons.kt`: Botones táctiles ergonómicos de gran área de toque.
- **`screens/SettingsScreen.kt`**: Orquestador de ajustes con soporte para el botón atrás del sistema (`BackHandler`).
- **`screens/settings/`**: Subpantallas técnicas independientes:
  - `SettingsCommon.kt`: Tipos compartidos, enums y encabezado `SubScreenHeader`.
  - `SettingsMainMenu.kt`: Menú principal con accesos rápidos.
  - `ResolutionSettingsSubScreen.kt`: Pestañas de resoluciones nativas, estándar y personalizada con paridad YUV420p.
  - `BitrateSettingsSubScreen.kt`: Control fino de 1 a 60 Mbps, nivel térmico y calculadora de almacenamiento.
  - `FpsSettingsSubScreen.kt`: Tasas de 24 a 120 FPS.
  - `CodecSettingsSubScreen.kt`: Códecs por hardware H.264, HEVC y AV1.
  - `AudioSettingsSubScreen.kt`: Fuentes de audio (interno, mic, mezcla y silencio).
  - `CountdownSettingsSubScreen.kt`: Temporizador de 0s a 10s.
  - `EngineSettingsSubScreen.kt`: Comparativa y especificaciones de Rust 2024 vs C++23.

### 4. `/src/utils` (Lógica Pura y Pruebas)
- `recorderCore.ts`: Funciones puras de cálculo de bitrate, almacenamiento, paridad de resolución y latencia.
- `recorderCore.test.ts`: Pruebas unitarias deterministas ejecutadas con `npx --yes tsx --test`.
