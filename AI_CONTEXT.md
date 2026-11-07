# 🧠 AI Context — VeloRec Studio

Este documento proporciona contexto técnico estructurado y reglas invariantes para modelos de lenguaje (LLMs) y sistemas de asistencia que interactúen con el repositorio de **VeloRec Studio**.

---

## 🎯 Propósito del Proyecto

VeloRec Studio es una aplicación móvil de grabación de pantalla de alto rendimiento diseñada para Android. Utiliza una arquitectura en cuatro capas para maximizar la tasa de cuadros por segundo (FPS) y minimizar el consumo térmico y de batería:

1. **Frontend / Presentación**: Aplicación móvil 100% nativa con **Kotlin Jetpack Compose (Material 3)** organizada modularmente (`ui/screens`, `ui/components`, `ui/theme`, `data/repository`, `data/model`) con Server-Driven UI dinámico desde GitHub Releases.
2. **Orquestación en Android**: Kotlin con Arquitectura MVVM (`MainViewModel` con StateFlow), Corrutinas (`Dispatchers.IO`), gestión modular de permisos `MediaProjection`, y `ScreenRecordService` con `RecordingNotificationHelper`.
3. **Motores de Codificación Nativa (Hardware NDK)**:
   - **C++23**: `AMediaCodec_createInputSurface`, `std::expected`, `std::span`, `std::jthread`, `ANativeWindow`.
   - **Rust (Edition 2024)**: Crate con `crossbeam-channel`, seguridad de memoria garantizada en tiempo de compilación y enlaces JNI.
4. **CI/CD Móvil**: Flujo de GitHub Actions con disparo **estrictamente manual (`workflow_dispatch`)** para compilar el APK Debug sin necesidad de computadora.

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
4. **Soporte de Arquitecturas (32 y 64 bits)**:
   - Se deben generar siempre las bibliotecas para `arm64-v8a` (64-bit ARM), `armeabi-v7a` (32-bit ARM) y `x86_64` (emulador).
5. **Entorno del Usuario y Diseño 100% Móvil Nativo**:
   - La app es una aplicación para smartphone, **NO un sitio web**.
   - No utilizar marcos o barras simuladas de teléfono (`09:41 5G 100%`) dentro de la pantalla, ya que el usuario opera directamente desde un dispositivo físico real.
   - Las interfaces deben ser 100% touch-friendly, con botones grandes, navegación inferior fija Material 3 y sin desbordamientos horizontales.
   - No forzar herramientas que requieran línea de comandos local; el APK se compila en la nube mediante GitHub Actions.
6. **Políticas de Propiedad Intelectual y Licenciamiento**:
   - Nunca incluir nombres de marcas comerciales protegidas en nombres de archivos.
   - Solo usar dependencias con licencias permisivas (MIT, Apache-2.0, BSD). Prohibido introducir librerías GPL o copyleft que obliguen a abrir el código.
7. **Configuración Remota Dinámica y Releases de GitHub**:
   - La app consulta de forma asíncrona y no bloqueante los releases en `https://github.com/LuisAlejandro544/Grabador-de-pantalla-definitivo/releases`.
   - **Resiliencia Offline Obligatoria**: La app nunca debe estar atada al 100% a la conexión ni fallar si no hay internet; siempre debe inicializarse con valores por defecto locales seguros.
   - El workflow `.github/workflows/publish-config-release.yml` debe ejecutarse **exclusivamente con `workflow_dispatch`** (nunca en `push` ni `pull_request`).

---

## 📊 Estado de los Datos y Almacenamiento

- Las grabaciones se almacenan localmente y en el contenedor MP4/MKV estándar.
- Las opciones de configuración persisten en `localStorage` / `SharedPreferences` con fallback automático a valores seguros por defecto (1080p @ 60 FPS, 12 Mbps, C++23).
- El validador de resoluciones exige que ancho y alto sean números pares para compatibilidad con codificadores por hardware YUV420p (`width % 2 === 0 && height % 2 === 0`).
