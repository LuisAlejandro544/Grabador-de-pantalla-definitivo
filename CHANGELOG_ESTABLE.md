# 🚀 VeloRec Studio — Notas de la Versión v1.0.0-E (Estable)

Bienvenido a la primera **Versión Oficial Estable (`v1.0.0-E`)** de **VeloRec Studio**.

> 💡 **Significado del identificador `-E`**:  
> La **E** significa **ESTABLE**. Representa una versión completamente verificada, optimizada y blindada contra fallos, caídas del sistema o desincronización de audio para su uso diario intensivo en smartphones Android.

---

## 🌟 Novedades Principales de la Versión v1.0.0-E

### 1. 🦀 Núcleo Nativo NDK en Rust 2024 (Zero-Copy & GC-Free)
- **Codificación directa por hardware**: La tubería de captura de pantalla y multiplexación corre 100% en hilos dedicados de Rust (Edition 2024), interactuando directamente con `ANativeWindow` y `AMediaCodec`.
- **Cero Recolección de Basura (GC-Free)**: Al no depender del Garbage Collector de la JVM ni de Go, el consumo de memoria se mantiene plano (<15 MB RSS) y sin micro-pausas que causen caídas de fotogramas (stuttering).
- **Inmunidad al Low Memory Killer (LMK)**: Probado y blindado contra el cierre intempestivo de procesos de Android mientras juegas títulos de alta demanda como *Free Fire*, *Subway Surfers* o *COD Mobile*.

### 2. ⚡ 60 FPS y 120 FPS Fluidos con Paridad de Resolución
- **Soporte de alta tasa de cuadros**: Graba a 60 FPS estables o activa el modo **120 FPS Ultra** en pantallas de alta tasa de refresco.
- **Paridad estricta de resolución**: Asegura dimensiones pares (`width % 2 == 0` y `height % 2 == 0`) para evitar fallos del codificador YUV420p de hardware en cualquier resolución nativa.

### 3. 🎙️ Audio Interno Puro y Sincronización Maestra (Zero-Drift & Anti-Lag)
- **Captura de sonido del juego sin root**: Mediante `AudioPlaybackCaptureConfiguration` (Android 10+ / API 29+) con calidad cristalina a 48 kHz.
- **Mezcla estéreo dual en tiempo real**: Graba el audio interno del juego y tu micrófono de forma simultánea con algoritmos matemáticos de clamping contra distorsión digital.
- **Reloj de Muestras Continuo y Sincronización T0**: Las pistas de audio y video inician en un tiempo base unificado `T0 = 0`, evitando el desfase acumulativo de audio en partidas largas.
- **Finalización Instantánea al Detener (<15 ms)**: Rust entrelaza el audio AAC y el video directamente en el archivo MP4 final en vivo. Al pulsar "Detener", la grabación se cierra en milisegundos sin procesos de copiado pesado ni fusiones secundarias en disco.

### 4. 🧠 Afinidad de Hilos a Núcleos de Eficiencia (big.LITTLE)
- Los hilos de captura y multiplexación en segundo plano se anclan exclusivamente a los núcleos LITTLE (Cortex-A55), dejando los núcleos de alto rendimiento Big (Cortex-A75) **100% libres y sin interrupciones para el videojuego**.

### 5. 🛡️ Guardián de Resiliencia Anti-LMK (`resilience.rs`)
- **Compactación periódica con `malloc_trim(0)`**: Libera páginas huérfanas de memoria Bionic libc de vuelta al kernel de Linux.
- **Prioridad de proceso y E/S en tiempo real**: Configura prioridad CPU `nice -16` y prioridad de E/S `SYS_ioprio_set` (Best-Effort Clase 2).
- **Manejador de señales de emergencia**: Si el sistema operativo envía una señal de terminación (`SIGTERM`, `SIGINT`), el MP4 se finaliza y cierra atómicamente para garantizar **Cero Pérdida de Datos**.

### 6. 📱 Nueva Interfaz Móvil Ergonómica en Jetpack Compose (Material 3)
- **Diseño táctil nativo**: Pensado para uso con una sola mano en pantallas físicas de smartphones (sin simular barras de estado o marcos artificiales).
- **Acceso rápido**: El visor de captura (*Viewfinder*) y el botón principal **"INICIAR GRABACIÓN"** están ubicados al alcance del pulgar sin necesidad de desplazamientos.
- **Perfiles Rápidos de Calidad en 1 Toque**:
  - `🎮 Gamer 60 FPS`: 1080p, 8 Mbps, Audio Interno, 3s cuenta regresiva.
  - `⚡ Ultra 120 FPS`: Resolución nativa a 120 FPS con alta fluidez.
  - `🔋 Ahorro Batería`: 540p / 4 Mbps para largas jornadas de juego.
  - `🎙️ Tutorial con Voz`: 1080p a 60 FPS con micrófono activado.
- **Pantalla de Bienvenida e Inducción (Onboarding)**:
  - Recorrido interactivo de 4 pasos explicando las ventajas clave.
  - Concesión guiada de permisos de Grabación de Audio y Notificaciones de Android.
- **Lanzador de Juegos 1-Click REC con Caché Inteligente**:
  - Detecta tus juegos y aplicaciones instaladas sin ralentizaciones.
  - **Caché por huella de paquetes**: Escanea las aplicaciones una sola vez y no vuelve a escanear a menos que instales o desinstales un juego, cargando la lista en 0 ms.
- **VeloRec Inspector (Debug)**:
  - Consola de diagnóstico integrada estilo LeakCanary para visualizar el Logcat en tiempo real, verificar la carga de bibliotecas nativas y el estado de la memoria RAM.

---

## 🔒 Garantía Absoluta de Tus Grabaciones (Blindaje de Datos)

> 🛡️ **IMPORTANTE: Tus videos grabados NUNCA se eliminan al actualizar la app**  
> 
> VeloRec Studio guarda todas tus grabaciones directamente en la **Galería Pública del Dispositivo** (`Movies/VeloRec`) utilizando el almacenamiento compartido de Android (`MediaStore API`).  
> 
> En el sistema operativo Android:
> 1. El almacenamiento compartido (`Movies/VeloRec`) es **independiente del ciclo de vida del APK**.
> 2. Al instalar una actualización (por ejemplo, de `v1.0.0-E` a `v1.0.1-E`), el instalador de Android únicamente reemplaza los archivos binarios de la app.
> 3. Tus videos en Google Fotos, la Galería nativa del teléfono y el disco del celular permanecen **100% seguros, intactos y accesibles en todo momento**.

---

## 🔄 Sistema de Actualizaciones Integrado

A partir de esta versión `v1.0.0-E`:
1. VeloRec Studio verifica de forma asíncrona la API de GitHub Releases (`LuisAlejandro544/Grabador-de-pantalla-definitivo`).
2. Si detecta una nueva versión con el identificador `-E`, muestra una ventana emergente detallando las notas del release y las mejoras incluidas.
3. Incluye un botón para **Descargar e Instalar Directamente el APK** sin intermediarios ni complicaciones.

---

## 📦 Arquitecturas Móviles Soportadas

- **arm64-v8a**: Celulares modernos de 64 bits (máxima optimización SIMD ARM NEON).
- **armeabi-v7a**: Celulares de 32 bits (retrocompatibilidad total).
- **x86_64**: Emuladores y dispositivos Android de pruebas.
