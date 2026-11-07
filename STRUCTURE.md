# 📁 Estructura del Proyecto — VeloRec Studio

Árbol de directorios detallado y explicación técnica de cada módulo del grabador de pantalla móvil híbrido.

---

## 🌳 Árbol de Archivos General

```
/
├── .github/
│   └── workflows/
│       ├── build-apk.yml               # GitHub Actions para compilar APK Debug manualmente (workflow_dispatch)
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
│           │   │   │   ├── RecordingConfig.kt   # Modelos de configuración, presets de resolución y bitrate
│           │   │   │   └── RemoteUiConfig.kt    # Modelos Server-Driven UI (app_config.json)
│           │   │   └── repository/
│           │   │       └── RemoteConfigRepository.kt # Carga asíncrona segura de GitHub Releases (Dispatchers.IO)
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
│           │           ├── RecorderScreen.kt  # Pantalla de visor de captura, chips y botones REC
│           │           ├── SettingsScreen.kt  # Ajustes de hardware, códec, audio y GitHub
│           │           ├── GalleryScreen.kt   # Galería de videos grabados con FileProvider
│           │           └── BenchmarkScreen.kt # Telemetría comparativa C++23 vs Rust 2024
│           └── cpp/
│               ├── CMakeLists.txt      # Configuración CMake para C++23 (-std=c++23, -O3)
│               ├── NativeRecorderEngine.hpp # Cabecera C++23 con std::expected, span y jthread
│               └── NativeRecorderEngine.cpp # AMediaCodec, AMediaMuxer, ANativeWindow y JNI
│
├── rust/                               # Motor nativo en Rust (Edition 2024)
│   ├── Cargo.toml                      # Manifiesto de Rust con edition = "2024", cdylib y ndk-sys
│   └── src/
│       └── lib.rs                      # Hardware encoder NDK AMediaCodec, ANativeWindow y crossbeam
│
├── src/                                # Código fuente de la interfaz y utilidades
│   ├── components/                     # Componentes modulares de pantalla táctil móvil
│   │   ├── Navigation.tsx              # Barra de cabecera y barra de navegación inferior táctil
│   │   ├── RecorderScreen.tsx          # Pantalla principal con monitor nativo, VU meter y controles
│   │   ├── SettingsScreen.tsx          # Configuración de motor, resolución, FPS, códec y ABIs
│   │   ├── GalleryScreen.tsx           # Galería de videos grabados con reproductor modal
│   │   └── BenchmarkScreen.tsx         # Prueba de estrés comparativa C++23 vs Rust 2024
│   │
│   ├── types/
│   │   └── recorder.ts                 # Tipos TypeScript, interfaces de motor y resoluciones
│   │
│   ├── utils/                          # Lógica pura, pruebas y exportación
│   │   ├── nativeCodeCatalog.ts        # Catálogo de archivos nativos y descripciones
│   │   ├── recorderCore.ts             # Funciones matemáticas desacopladas de la UI
│   │   ├── recorderCore.test.ts        # Tests unitarios deterministas con node:test
│   │   └── zipExport.ts                # Generador de archivo .ZIP con JSZip para móvil
│   │
│   ├── App.tsx                         # Componente raíz con estado global y MediaRecorder
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
├── ESTRUCTURE.md                       # Referencia de estructura del proyecto
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
- **CMake**: Compila `NativeRecorderEngine.cpp` con el estándar C++23 activado explícitamente (`-std=c++23`).
- **Cargo NDK**: Compila el módulo Rust generando las bibliotecas `.so` para arquitecturas de 64 bits (`arm64-v8a`), 32 bits (`armeabi-v7a`) y `x86_64`.
- **Servicios**: `ScreenRecordService.kt` hereda de `android.app.Service` y maneja `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` con Corrutinas Kotlin (`Dispatchers.IO`).

### 2. `/rust` (Motor Rust Edition 2024 con Hardware Encoder)
Crate autónomo con `crate-type = ["cdylib"]`.
- Configurado con `edition = "2024"` y dependencias NDK (`ndk = "0.9.0"`, `ndk-sys = "0.6.0"`, `crossbeam-channel = "0.5.14"`, `libc = "0.2"`).
- Implementa la tubería **Zero-Copy**: crea el codificador de hardware mediante `AMediaCodec_createEncoderByType`, obtiene la superficie nativa con `AMediaCodec_createInputSurface` y la entrega a Android vía `ANativeWindow_toSurface`.
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
