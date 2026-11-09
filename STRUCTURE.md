# 📁 Estructura del Proyecto — VeloRec Studio

Árbol de directorios detallado y explicación técnica de cada módulo del grabador de pantalla móvil híbrido.

---

## 🌳 Árbol de Archivos General

```
/
├── .github/
│   └── workflows/
│       ├── build-apk.yml               # GitHub Actions para compilar APK Debug manualmente con selector de ABI (universal o individual)
│       ├── code-analysis-audit.yml     # GitHub Actions para auditoría de producción y análisis estático (Kotlin Lint, Cppcheck, Clippy, TS)
│       └── publish-config-release.yml  # GitHub Actions para publicar app_config.json en GitHub Releases (workflow_dispatch)
│
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
│           │   │   └── RecordingNotificationHelper.kt # Gestión de canal y notificaciones de Foreground Service
│           │   └── ui/
│           │       ├── MainViewModel.kt     # ViewModel de retención de estado de grabación y cronómetro
│           │       ├── VeloRecMainScreen.kt # Scaffold principal, TopAppBar y navegación inferior
│           │       ├── theme/
│           │       │   └── Color.kt         # Paleta oscura Material 3 y parseColorSafe
│           │       ├── components/
│           │       │   ├── MetricBadge.kt   # Componente de telemetría de hardware
│           │       │   └── RemoteAnnouncementDialog.kt # Diálogo modal para anuncios remotos
│           │       └── screens/
│           │           ├── RecorderScreen.kt    # Pantalla de visor de captura, chips y botones REC
│           │           ├── AppLauncherScreen.kt # Lanzador rápido de juegos y apps descargadas con 1-Click REC
│           │           ├── SettingsScreen.kt    # Ajustes de hardware, códec, audio y GitHub
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
│       ├── lib.rs                      # Hardware encoder NDK AMediaCodec, ANativeWindow y crossbeam
│       ├── audio_oboe.rs               # Integración de Google Oboe para captura de audio nativo (<10ms)
│       ├── color_yuv.rs                # Conversión de color YUV420/NV12 y RGBA con SIMD ARM NEON (yuvutils-rs)
│       ├── gpu_glow.rs                 # Tubería de sombreadores GPU OpenGL ES 3.20 en Rust puro (glow)
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

## 🔍 Explicación de Carpetas Clave

### 1. `/android` (Capa Android Nativa Completa)
Contiene la configuración de Gradle Kotlin DSL (`build.gradle.kts`) lista para compilar de forma autónoma:
- **Gradle Wrapper**: Incluye los ejecutables `gradlew` (Linux/macOS), `gradlew.bat` (Windows) y `gradle-wrapper.properties` configurado con Gradle 8.7.
- **Configuración Raíz y Módulos**: `settings.gradle.kts` y `build.gradle.kts` vinculando el módulo `:app`.
- **CMake**: Compila la tubería gráfica `GpuShaderPipeline.cpp` y Google `libyuv_bridge.cpp` con el estándar C++23 (`-std=c++23`).
- **Cargo NDK**: Compila el módulo Rust generando las bibliotecas `.so` para arquitecturas de 64 bits (`arm64-v8a`), 32 bits (`armeabi-v7a`) y `x86_64`.
- **Servicios**: `ScreenRecordService.kt` hereda de `android.app.Service` y maneja `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` con Corrutinas Kotlin (`Dispatchers.IO`).

### 2. `/rust` (Motor Rust Edition 2024 con Hardware Encoder y Módulos de Alto Rendimiento)
Crate autónomo con `crate-type = ["cdylib", "rlib"]`.
- Configurado con `edition = "2024"` y dependencias NDK (`ndk = "0.9.0"`, `ndk-sys = "0.6.0"`, `crossbeam-channel = "0.5.14"`, `libc = "0.2"`).
- Implementa la tubería **Zero-Copy**: crea el codificador de hardware mediante `AMediaCodec_createEncoderByType`, obtiene la superficie nativa con `AMediaCodec_createInputSurface` y la entrega a Android vía `ANativeWindow_toSurface`.
- **Integración de Google Oboe (`audio_oboe.rs`)**: Captura de audio de ultra baja latencia (<10 ms) con soporte dual AAudio/OpenSL ES.
- **Conversión de Color Acelerada con SIMD ARM NEON (`color_yuv.rs`)**: Conversiones YUV420/NV12 y RGBA a más de 200 FPS en Rust puro con `yuvutils-rs`.
- **Tubería de Sombreadores GPU (`gpu_glow.rs`)**: Shaders OpenGL ES 3.20 en Rust mediante `glow` para FaceCam circular antialiased, Chroma Key y Gaming Color Boost.
- **Motor de Streaming en Vivo (`streamer.rs`)**: Despacho de paquetes NAL de ultra baja latencia (RTMP/SRT) con *Ring Buffer* de 4 MB y descarte inteligente anti-congestión (Zero-GC).
- **Multiplexación de Audio y Video en Tiempo Real**: Registra simultáneamente las pistas de video (`AMediaCodec`) y audio AAC en `AMediaMuxer`, entrelazando las muestras en vivo en memoria nativa. Al detener la captura, el archivo MP4 se finaliza de inmediato (<15 ms) sin archivos temporales ni fusiones secundarias.
- Procesa búferes codificados en un hilo worker seguro y escribe el archivo MP4 mediante `AMediaMuxer`.
- Garantiza ausencia total de condiciones de carrera (*Data Races*) y cero recolección de basura (*Zero GC*).


### 3. `/src/components` (Diseño Modular UI)
Cumpliendo con la regla de diseño modular:
- Cada pantalla principal reside en su propio archivo independiente.
- La navegación está desacoplada del renderizado de video.
- La galería y el reproductor de video cuentan con su propio ciclo de vida.
- El visor de código nativo permite auditar tanto C++23 como Rust 2024 y los scripts Gradle.

### 4. `/src/utils` (Lógica Pura y Pruebas)
- `recorderCore.ts`: Funciones puras de cálculo de bitrate, almacenamiento, paridad de resolución y latencia.
- `recorderCore.test.ts`: Pruebas unitarias deterministas ejecutadas con `npx --yes tsx --test`.
- `zipExport.ts`: Compila dinámicamente todo el proyecto nativo en un archivo ZIP descargable con un solo toque desde el celular.
