# 🤖 README_AI — VeloRec Studio

> **Ficha Técnica Ejecutiva para Agentes y Asistentes de Inteligencia Artificial.**

---

## 📋 Resumen del Repositorio

| Atributo | Detalle |
|---|---|
| **Nombre** | VeloRec Studio (Grabador de Pantalla 100% Móvil Nativo) |
| **Plataformas de Destino** | Android Nativo (APK instalable, Uptodown, F-Droid) |
| **Frontend Móvil** | **React Native (TurboModule)** + **Kotlin Jetpack Compose (Material 3)** |
| **Orquestación Nativa** | Kotlin (Jetpack Compose UI + Coroutines `Dispatchers.IO` + Foreground Service) |
| **Motores de Video** | **C++23** (AMediaCodec NDK) y **Rust (Edition 2024 / 1.85+)** |
| **Versiones de Android** | `minSdkVersion = 26` (Android 8.0) \| `targetSdkVersion = 35` (Android 15) |
| **Arquitecturas CPU** | `arm64-v8a` (64-bit), `armeabi-v7a` (32-bit), `x86_64` |
| **CI / Compilación** | GitHub Actions (`workflow_dispatch`, 100% manual) |
| **Licenciamiento** | MIT / Apache-2.0 Permisivo (Sin restricciones de copyleft) |

---

## ⚡ Mapa Rápido de Componentes

- **`.github/workflows/build-apk.yml`**: Flujo de GitHub Actions manual para compilar `app-debug.apk` con Node 20, Java 17, Android SDK 35, NDK r26c, Rust 2024 y `cargo-ndk`.
- **`android/app/src/main/java/com/velorec/recorder/VeloRecComposeUI.kt`**: Interfaz de usuario táctil nativa en Kotlin con Jetpack Compose Material 3.
- **`src/native_source/react_native/NativeScreenRecorder.ts`**: Especificación TurboModule tipada para React Native.
- **`src/native_source/kotlin/ScreenRecorderModule.kt`**: Módulo puente de React Native con Corrutinas para delegación asíncrona.
- **`src/native_source/kotlin/ScreenRecordService.kt`**: Servicio en primer plano con `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` que vincula la `Surface` nativa con `MediaProjection`.
- **`src/native_source/cpp/NativeRecorderEngine.hpp` & `.cpp`**: Motor C++23 con `AMediaCodec_createInputSurface` para codificación directa por hardware con cero copias.
- **`src/native_source/cpp/CMakeLists.txt`**: Configuración CMake con `-std=c++23` y enlace a `mediandk`, `android` y `log`.
- **`src/native_source/rust/Cargo.toml` & `src/lib.rs`**: Motor Rust Edition 2024 con canales `crossbeam-channel` y enlaces JNI.
- **`src/native_source/android/build.gradle.kts`**: Script Gradle que compila tanto CMake (C++23), Cargo NDK (Rust) como Jetpack Compose para arquitecturas de 32 y 64 bits con firma de depuración automática.
- **`src/utils/recorderCore.ts`**: Lógica desacoplada de cálculo de bitrate, validación de paridad de resolución y estimaciones de peso.
- **`src/utils/recorderCore.test.ts`**: Suite de tests unitarios ejecutables con `npx --yes tsx --test`.

---

## 🛠️ Cómo Compilar el APK Debug (Procedimiento para el Usuario Móvil)

El usuario no dispone de PC; todo el ciclo de compilación se realiza en la nube:
1. El usuario descarga el archivo `.ZIP` desde la pestaña **Código** de la aplicación.
2. Sube el contenido a un repositorio en **GitHub**.
3. En la pestaña **Actions**, selecciona **"Compilar APK Debug (VeloRec Mobile)"** y toca **"Run workflow"**.
4. GitHub Actions ejecuta la compilación en un runner Linux y genera el artefacto descargable **`VeloRec-Debug-APK`**.
5. El usuario descarga el APK directamente a su teléfono móvil.
