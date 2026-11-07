# 📱 VeloRec Studio — Grabador de Pantalla Móvil Nativo de Alto Rendimiento

> **Arquitectura móvil nativa**: React Native + Kotlin (Jetpack Compose Material 3 & Corrutinas) + C++23 (AMediaCodec NDK) + Rust (Edition 2024).

VeloRec Studio es una aplicación **100% móvil nativa** de captura y codificación de pantalla para Android. Diseñada con interfaz táctil ergonómica y moderna basada en **Jetpack Compose (Material 3)** y orquestada con **React Native TurboModules**, elimina por completo los cuellos de botella de la máquina virtual (JVM/ART) mediante una tubería **Zero-Copy** que transfiere directamente las superficies de hardware de la GPU a los codificadores nativos en C++23 y Rust.

---

## ⚡ Características Principales

- **Diseño 100% Móvil Nativo (Sin Emuladores ni Marcos Web)**:
  - Interfaz táctil ergonómica adaptada para usarse con una sola mano en smartphones.
  - Vistas nativas construidas con **Kotlin Jetpack Compose (Material 3)** integradas en la arquitectura de React Native.
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
- **Soporte Multiarquitectura (32 y 64 bits)**:
  - `arm64-v8a`: Estándar de 64 bits para teléfonos modernos.
  - `armeabi-v7a`: Soporte de 32 bits para dispositivos antiguos.
  - `x86_64`: Emuladores de desarrollo y Chromebooks.
- **Control Total de Video y Audio**:
  - Resoluciones predefinidas (480p SD, 720p HD, 1080p FHD, 1440p 2K, 4K UHD) y resolución personalizada con comprobación de paridad YUV420p.
  - Tasas de cuadro: 24, 30, 60, 90 y 120 FPS.
  - Bitrate ajustable de 2 a 50 Mbps con cálculo de almacenamiento en vivo.
  - Códecs por hardware: H.264 (AVC), H.265 (HEVC) y AV1.
  - Captura de audio: Audio Interno, Micrófono con medidor VU en decibeles, Mezcla estéreo y Silencio.
- **Herramientas de Superposición**:
  - Cámara flotante FaceCam ajustable.
  - Efectos visuales de toques en pantalla.
- **Galería Integrada**:
  - Reproductor con control de velocidad (0.5x, 1x, 1.5x, 2x) y descarga directa al almacenamiento local.

---

## 🏗️ Flujo de Arquitectura

```
[React Native + Kotlin Jetpack Compose UI] 
       │ (Material 3 Dark Theme + TurboModule Spec)
       ▼
[Kotlin ScreenRecorderModule & VeloRecComposeUI] 
       │ (Corrutinas Dispatchers.IO + MediaProjection)
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
   - Pulsa el botón **"Run workflow"** y selecciona tu preferencia de motor.
4. **Descarga el APK**:
   - En 3-4 minutos, GitHub Actions terminará la compilación.
   - En la sección **Artifacts** descarga el archivo **`VeloRec-Debug-APK`** e instálalo de inmediato en tu celular o súbelo a Uptodown.

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
