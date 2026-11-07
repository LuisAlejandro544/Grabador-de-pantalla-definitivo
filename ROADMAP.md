# 🗺️ Hoja de Ruta (ROADMAP) — VeloRec Studio

Plan de evolución técnica y funcional para el grabador de pantalla móvil de alto rendimiento.

---

## 📌 Fase 1: Arquitectura Base y Motores Nativos (Completada ✅)

- [x] **Diseño de Interfaz Móvil Touch-First**: Navegación por pestañas inferiores accesible y optimizada para uso exclusivo en teléfonos.
- [x] **Especificación TurboModule en TypeScript**: Definición estricta de interfaces entre React Native y Kotlin.
- [x] **Capa de Orquestación Kotlin con Corrutinas**:
  - `ScreenRecordService` con soporte para Android 14+ (`FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION`).
  - `minSdkVersion = 26` y `targetSdkVersion = 35`.
  - Tubería Zero-Copy delegando `Surface` a C++ y Rust.
- [x] **Motor C++23 NDK**:
  - Implementación con `-std=c++23` utilizando `std::expected`, `std::span`, `std::jthread` y `std::format`.
  - Integración directa con `AMediaCodec` y `AMediaMuxer`.
- [x] **Motor Rust (Edition 2024)**:
  - Crate con `edition = "2024"`, canales `crossbeam-channel` y bindings JNI seguros.
  - Cero condiciones de carrera y memoria segura garantizada por el compilador.
- [x] **Soporte Multiarquitectura (32 y 64 bits)**:
  - Generación de bibliotecas `.so` para `arm64-v8a`, `armeabi-v7a` y `x86_64`.
- [x] **Automatización en la Nube con GitHub Actions**:
  - Flujo manual (`workflow_dispatch`) que compila el APK Debug, descarga dependencias, genera la keystore de prueba y produce el archivo `.apk` descargable.
- [x] **Suite de Pruebas Unitarias Deterministas**:
  - Validación con `node:test` de cálculo de bitrate, validación de paridad de resolución y métricas de motor.

---

## 🚀 Fase 2: Transmisión en Vivo y Protocolos de Red (Q1 2027)

- [ ] **Streaming RTMP / RTMPS**: Transmisión de pantalla en directo hacia plataformas como YouTube, Twitch o servidores propios.
- [ ] **Protocolo SRT (Secure Reliable Transport)**: Protocolo de ultra baja latencia con corrección de paquetes (ARQ) sobre conexiones móviles inestables (4G/5G).
- [ ] **Soporte para Vulkan Video Extensions en C++23**: Reemplazo directo de EGL por Vulkan Video Encode para teléfonos con Snapdragon 8 Gen 2/3/4 y Dimensity 9300+.
- [ ] **Adaptación Dinámica de Bitrate (ABR)**: Ajuste en tiempo real de la tasa de bits ante caídas de señal o picos de congestión de red.

---

## ✂️ Fase 3: Editor de Video en el Teléfono (Q2 2027)

- [ ] **Recorte Rápido sin Recodificar (Stream Copy)**: Cortar inicio y fin del video en menos de 1 segundo sin pérdida de calidad.
- [ ] **Superposición de Marca de Agua y Texto**: Renderizado acelerado por hardware para logotipos de canal o marcas de agua.
- [ ] **Generador de GIFs animados**: Exportación de fragmentos destacados de 5 a 15 segundos en formato GIF de alta velocidad.
- [ ] **Extracción y Mezcla de Pistas de Audio**: Separación de la pista de micrófono y la pista de audio interno del juego en archivos AAC independientes.

---

## 🎮 Fase 4: Modo Gaming y Telemetría de Alto Nivel (Q3 2027)

- [ ] **Monitor de Tasa de Refresco de Pantalla (`Choreographer`)**: Medición precisa de los FPS reales entregados por el juego con overlay flotante no intrusivo.
- [ ] **Protección Térmica Inteligente**:
  - Lectura de sensores térmicos del SoC mediante `ThermalManager` de Android.
  - Reducción dinámica de resolución o FPS si el procesador alcanza los 45°C para evitar caídas de rendimiento (*thermal throttling*).
- [ ] **Modo No Molestar Automático**: Bloqueo temporal de notificaciones emergentes y llamadas telefónicas durante la sesión de grabación.
- [ ] **Pausa Automática por Pantalla Apagada**: Pausa inteligente del codificador si la pantalla entra en reposo.

---

## 📦 Fase 5: Distribución Móvil y Tiendas de Terceros (Q4 2027)

- [ ] **Publicación Automática en Uptodown**: Integración de scripts para actualizar versiones APK directamente en el catálogo de Uptodown.
- [ ] **Soporte para F-Droid**: Empaquetado reproducible compatible con los estándares de compilación de F-Droid.
- [ ] **Sistema de Actualizaciones In-App Sin Google Play**: Comprobación de nuevas versiones con descargas de paquetes `.apk` con verificación de hash SHA-256.
