# 📁 Estructura del Proyecto — VeloRec Studio

Árbol de directorios detallado y explicación técnica de cada módulo del grabador de pantalla móvil híbrido.

---

## 🌳 Árbol de Archivos General

```
/
├── .github/
│   └── workflows/
│       └── build-apk.yml               # GitHub Actions para compilar APK Debug manualmente
│
├── android/                             # Estructura del proyecto Android nativo
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml     # Permisos MediaProjection y ForegroundService
│   │   │   ├── java/com/velorec/recorder/
│   │   │   │   ├── ScreenRecorderModule.kt  # Módulo puente React Native con Corrutinas
│   │   │   │   └── ScreenRecordService.kt   # Foreground Service con Surface nativa
│   │   │   └── cpp/
│   │   │       ├── CMakeLists.txt      # Configuración CMake para C++23 (-std=c++23)
│   │   │       ├── NativeRecorderEngine.hpp # Cabecera C++23 con std::expected y span
│   │   │       └── NativeRecorderEngine.cpp # AMediaCodec, AMediaMuxer y enlaces JNI
│   │   └── build.gradle.kts            # Gradle Kotlin DSL con CMake y Cargo NDK
│   └── build.gradle.kts                # Configuración raíz de Gradle
│
├── rust/                               # Motor nativo en Rust (Edition 2024)
│   ├── Cargo.toml                      # Manifiesto de Rust con edition = "2024" y cdylib
│   └── src/
│       └── lib.rs                      # Implementación segura de codificación y JNI
│
├── src/                                # Código fuente de la interfaz y utilidades
│   ├── components/                     # Componentes modulares de pantalla
│   │   ├── Navigation.tsx              # Barra de cabecera y barra de navegación inferior
│   │   ├── RecorderScreen.tsx          # Pantalla principal con visor, VU meter y controles
│   │   ├── SettingsScreen.tsx          # Configuración de motor, resolución, FPS, códec y ABIs
│   │   ├── GalleryScreen.tsx           # Galería de videos grabados con reproductor modal
│   │   ├── BenchmarkScreen.tsx         # Prueba de estrés comparativa C++23 vs Rust 2024
│   │   └── NativeCodeScreen.tsx        # Explorador de código y exportador a ZIP
│   │
│   ├── native_source/                  # Archivos nativos comentados para el catálogo
│   │   ├── android/
│   │   │   ├── AndroidManifest.xml
│   │   │   └── build.gradle.kts
│   │   ├── cpp/
│   │   │   ├── CMakeLists.txt
│   │   │   ├── NativeRecorderEngine.hpp
│   │   │   └── NativeRecorderEngine.cpp
│   │   ├── kotlin/
│   │   │   ├── ScreenRecorderModule.kt
│   │   │   └── ScreenRecordService.kt
│   │   ├── react_native/
│   │   │   └── NativeScreenRecorder.ts
│   │   └── rust/
│   │       ├── Cargo.toml
│   │       └── src/lib.rs
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

### 1. `/android` (Capa Android Nativa)
Contiene la configuración de Gradle Kotlin DSL (`build.gradle.kts`) configurada para soportar:
- **CMake**: Compila `NativeRecorderEngine.cpp` con el estándar C++23 activado explícitamente (`-std=c++23`).
- **Cargo NDK**: Compila el módulo Rust generando las bibliotecas `.so` para arquitecturas de 64 bits (`arm64-v8a`), 32 bits (`armeabi-v7a`) y `x86_64`.
- **Servicios**: `ScreenRecordService.kt` hereda de `android.app.Service` y maneja `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` con Corrutinas Kotlin.

### 2. `/rust` (Motor Rust Edition 2024)
Crate autónomo con `crate-type = ["cdylib"]`. Utiliza las garantías del compilador de Rust para asegurar que ningún hilo de codificación pueda provocar condiciones de carrera (*Data Races*) o corrupción de memoria.

### 3. `/src/components` (Diseño Modular UI)
Cumpliendo con la regla de diseño modular:
- Cada pantalla principal reside en su propio archivo independiente.
- La navegación está desacoplada del renderizado de video.
- La galería y el reproductor de video cuentan con su propio ciclo de vida.

### 4. `/src/utils` (Lógica Pura y Pruebas)
- `recorderCore.ts` no depende del DOM ni de React, permitiendo ejecutar pruebas instantáneas con `npx --yes tsx --test`.
- `zipExport.ts` compila dinámicamente todo el proyecto nativo en un archivo descargable con un solo toque desde el navegador del celular.
