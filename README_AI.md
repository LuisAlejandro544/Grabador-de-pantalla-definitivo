# 🤖 README_AI — VeloRec Studio

> **Ficha Técnica Ejecutiva para Agentes y Asistentes de Inteligencia Artificial.**

---

## 📋 Resumen del Repositorio

| Atributo | Detalle |
|---|---|
| **Nombre** | VeloRec Studio (Grabador de Pantalla 100% Móvil Nativo) |
| **Plataformas de Destino** | Android Nativo (APK instalable, Uptodown, F-Droid) |
| **Frontend Móvil** | **Kotlin Jetpack Compose (Material 3)** (100% Nativo) |
| **Orquestación Nativa** | Kotlin (Jetpack Compose UI + Coroutines `Dispatchers.IO` + Foreground Service) |
| **Motores de Video** | **C++23** (AMediaCodec NDK) y **Rust (Edition 2024 / 1.85+)** |
| **Versiones de Android** | `minSdkVersion = 26` (Android 8.0) \| `targetSdkVersion = 35` (Android 15) |
| **Arquitecturas CPU** | `arm64-v8a` (64-bit), `armeabi-v7a` (32-bit), `x86_64` (Selector universal/individual en CI) |
| **CI / Compilación** | GitHub Actions (`workflow_dispatch`, 100% manual con selector de ABI) |
| **Licenciamiento** | MIT / Apache-2.0 Permisivo (Sin restricciones de copyleft) |

---

## ⚡ Mapa Rápido de Componentes

- **`.github/workflows/build-apk.yml`**: Flujo de GitHub Actions manual para compilar APK Debug con selector dinámico de arquitectura (`all`, `arm64-v8a`, `armeabi-v7a`, `x86_64`), Java 17, Android SDK 35, NDK r26c, Rust 2024 y `cargo-ndk`.
- **`.github/workflows/publish-config-release.yml`**: Flujo manual para publicar `app_config.json` a GitHub Releases.
- **`android/app/src/main/java/com/velorec/recorder/`**: Módulos nativos desacoplados:
  - `MainActivity.kt`: Entrada de ComponentActivity conectada a `MainViewModel`.
  - `ui/MainViewModel.kt`: Manejador de estado reactivo y temporizador persistente a rotaciones.
  - `ui/VeloRecMainScreen.kt`: Scaffold principal, barra de navegación táctil, conmutador NDK y diálogo de cuenta regresiva previa a la captura.
  - `ui/screens/`: Pantallas desacopladas (`RecorderScreen.kt`, `SettingsScreen.kt`, `GalleryScreen.kt`, `BenchmarkScreen.kt`).
  - `ui/components/`: Componentes modulares (`MetricBadge.kt`, `RemoteAnnouncementDialog.kt`).
  - `ui/theme/Color.kt`: Paleta Material 3 Dark Theme y parseo seguro de colores remotos.
  - `data/model/`: `RecordingConfig.kt` (presets adaptativos de aspect ratio, temporizador de cuenta regresiva y bitrate) y `RemoteUiConfig.kt`.
  - `data/repository/RemoteConfigRepository.kt`: Sincronización asíncrona de `app_config.json` en `Dispatchers.IO`.
  - `service/RecordingNotificationHelper.kt`: Canales y construcción de notificaciones del servicio.
  - `ffmpeg/FFmpegRecoveryEngine.kt`: Motor de recuperación ante apagones o muerte del proceso con FFmpeg Kit LTS 16KB, timestamps PTS base 0 y multiplexación `+faststart`.
  - `storage/GalleryStorageManager.kt`: Publicación e indexación en el álbum público `Movies/VeloRec` de la galería nativa mediante MediaStore API y MediaScannerConnection.
  - `ScreenRecordService.kt`: Servicio en primer plano con `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`, corrutinas IO y firmas JNI C++23 y Rust 2024.
  - `VeloRecComposeUI.kt`: Fachada modular que mantiene retrocompatibilidad total.
- **`android/app/src/main/cpp/NativeRecorderEngine.hpp` & `.cpp`**: Motor C++23 con `AMediaCodec_createInputSurface` para codificación directa por hardware con cero copias y normalización PTS base 0.
- **`android/app/src/main/cpp/CMakeLists.txt`**: Configuración CMake con `-std=c++23` y enlace a `mediandk`, `android` y `log`.
- **`rust/Cargo.toml` & `rust/src/lib.rs`**: Motor Rust Edition 2024 con canales `crossbeam-channel`, sincronización PTS base 0 y enlaces JNI.
- **`android/app/build.gradle.kts`**: Script Gradle que compila CMake (C++23), Cargo NDK (Rust) con soporte para `-PtargetAbi`, y Jetpack Compose con firma de depuración automática.
- **`src/utils/recorderCore.ts`**: Lógica desacoplada de cálculo de bitrate, validación de paridad de resolución, validación de cuenta regresiva y estimaciones de peso.
- **`src/utils/recorderCore.test.ts`**: Suite de tests unitarios ejecutables con `npx --yes tsx --test`.

---

## 🛠️ Cómo Compilar el APK Debug (Procedimiento para el Usuario Móvil)

El usuario no dispone de PC; todo el ciclo de compilación se realiza en la nube:
1. El usuario descarga el archivo `.ZIP` desde la pestaña **Código** de la aplicación.
2. Sube el contenido a un repositorio en **GitHub**.
3. En la pestaña **Actions**, selecciona **"Compilar APK Debug (VeloRec Mobile)"** y toca **"Run workflow"**.
4. Selecciona el motor deseado (C++23 o Rust 2024) y la arquitectura de destino (`all` Universal o individual `arm64-v8a`, `armeabi-v7a`, `x86_64`).
5. GitHub Actions ejecuta la compilación en un runner Linux y genera el artefacto descargable (`VeloRec-Debug-APK-Universal`, etc.).
6. El usuario descarga el APK directamente a su teléfono móvil.
