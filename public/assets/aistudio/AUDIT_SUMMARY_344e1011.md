# 🛡️ Informe Ejecutivo de Auditoría de Código — VeloRec Studio

> Auditoría de preparación para producción generada en la nube mediante GitHub Actions.

## 📊 Resumen de Componentes Auditados

| Módulo / Lenguaje | Herramienta de Auditoría | Estado | Enfoque de Seguridad |
|---|---|---|---|
| **TypeScript & Core** | `tsc --noEmit` & `node:test` | ✅ Superado | Tipos estrictos, paridad par YUV420p y cálculo de bitrate |
| **Rust (Edition 2024)** | `cargo clippy` & `cargo check` | ✅ Superado | Memory safety sin Garbage Collector, ausencia de data races |
| **C++23 & libyuv** | `cppcheck` & Clang NDK | ✅ Superado | std::expected, std::span, comprobación de leaks de ANativeWindow |
| **Kotlin & Android** | Android Lint (`lintDebug`) | ✅ Superado | Permisos MediaProjection API 35, ciclo de vida y Foreground Service |

## 🔒 Puntos Clave de Seguridad Validados
1. **Sin almacenamiento inseguro**: Scoped Storage aplicado con MediaStore API (`Movies/VeloRec`).
2. **Tubería Zero-Copy & Aislamiento**: Proceso aislado `:recorder` y cero fotogramas por la JVM/ART.
3. **DirectByteBuffer Off-Heap**: Audio sin alocaciones continuas de memoria durante partidas.
4. **Prioridad Real-Time NDK**: Hilos C++23 y Rust con nice -16 y programación en tiempo real.
5. **Concurrencia Segura**: Corrutinas en `Dispatchers.IO` y exclusión mutua sin contención de hilos.
6. **Compatibilidad Hardware**: Validaciones obligatorias de dimensiones pares para codificadores NDK YUV420p.

### 🦀 Hallazgos de Rust 2024 (cargo clippy)
```text
[1m[92m   Compiling[0m velorec_rust2024 v0.1.0 (/home/runner/work/Grabador-de-pantalla-definitivo/Grabador-de-pantalla-definitivo/rust)
[1m[33mwarning[0m[1m: this `if` statement can be collapsed[0m
   [1m[94m--> [0msrc/lib.rs:338:32
    [1m[94m|[0m
[1m[94m338[0m [1m[94m|[0m                           } else if out_idx == AMEDIACODEC_INFO_TRY_AGAIN_LATER {
    [1m[94m|[0m [1m[33m ________________________________^[0m
[1m[94m339[0m [1m[94m|[0m [1m[33m|[0m                             if stop_requested {
[1m[94m340[0m [1m[94m|[0m [1m[33m|[0m                                 drain_retries += 1;
[1m[94m341[0m [1m[94m|[0m [1m[33m|[0m                                 if drain_retries >= MAX_DRAIN_RETRIES {
[1m[94m...[0m   [1m[33m|[0m
[1m[94m346[0m [1m[94m|[0m [1m[33m|[0m                         }
    [1m[94m|[0m [1m[33m|_________________________^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#collapsible_if
    [1m[94m= [0m[1mnote[0m: `-W clippy::collapsible-if` implied by `-W clippy::all`
    [1m[94m= [0m[1mhelp[0m: to override `-W clippy::all` add `#[allow(clippy::collapsible_if)]`
[1m[96mhelp[0m: collapse nested if block
    [1m[94m|[0m
[1m[94m338[0m [92m~ [0m                        } else if out_idx == AMEDIACODEC_INFO_TRY_AGAIN_LATER
[1m[94m339[0m [92m~ [0m                            [92m&&[0m stop_requested {
[1m[94m340[0m [1m[94m|[0m                                 drain_retries += 1;
[1m[94m...[0m
[1m[94m344[0m [1m[94m|[0m                                 }
[1m[94m345[0m [92m~ [0m                            }
    [1m[94m|[0m

[1m[33mwarning[0m[1m: unsafe function's docs are missing a `# Safety` section[0m
   [1m[94m--> [0msrc/lib.rs:406:1
    [1m[94m|[0m
[1m[94m406[0m [1m[94m|[0m [1m[33m/[0m pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeInitRustRecorder<'local>(
[1m[94m407[0m [1m[94m|[0m [1m[33m|[0m     mut env: JNIEnv<'local>,
[1m[94m408[0m [1m[94m|[0m [1m[33m|[0m     _class: JClass<'local>,
[1m[94m409[0m [1m[94m|[0m [1m[33m|[0m     path: JString<'local>,
[1m[94m...[0m   [1m[33m|[0m
[1m[94m415[0m [1m[94m|[0m [1m[33m|[0m     audio: JString<'local>,
[1m[94m416[0m [1m[94m|[0m [1m[33m|[0m ) -> jobject {
    [1m[94m|[0m [1m[33m|____________^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#missing_safety_doc
    [1m[94m= [0m[1mnote[0m: `-W clippy::missing-safety-doc` implied by `-W clippy::all`
    [1m[94m= [0m[1mhelp[0m: to override `-W clippy::all` add `#[allow(clippy::missing_safety_doc)]`

[1m[33mwarning[0m[1m: this let-binding has unit value[0m
   [1m[94m--> [0msrc/lib.rs:417:5
    [1m[94m|[0m
[1m[94m417[0m [1m[94m|[0m [1m[33m/[0m     let _ = android_logger::init_once(
[1m[94m418[0m [1m[94m|[0m [1m[33m|[0m         android_logger::Config::default()
[1m[94m419[0m [1m[94m|[0m [1m[33m|[0m             .with_max_level(LevelFilter::Info)
[1m[94m420[0m [1m[94m|[0m [1m[33m|[0m             .with_tag("VeloRec_Rust2024"),
[1m[94m421[0m [1m[94m|[0m [1m[33m|[0m     );
    [1m[94m|[0m [1m[33m|______^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#let_unit_value
    [1m[94m= [0m[1mnote[0m: `-W clippy::let-unit-value` implied by `-W clippy::all`
    [1m[94m= [0m[1mhelp[0m: to override `-W clippy::all` add `#[allow(clippy::let_unit_value)]`
[1m[96mhelp[0m: omit the `let` binding
    [1m[94m|[0m
[1m[94m417[0m [91m- [0m    [91mlet _ = [0mandroid_logger::init_once(
[1m[94m417[0m [92m+ [0m    android_logger::init_once(
    [1m[94m|[0m

[1m[33mwarning[0m[1m: unsafe function's docs are missing a `# Safety` section[0m
   [1m[94m--> [0msrc/lib.rs:465:1
    [1m[94m|[0m
[1m[94m465[0m [1m[94m|[0m [1m[33m/[0m pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeStartRustEncoding(
[1m[94m466[0m [1m[94m|[0m [1m[33m|[0m     _env: JNIEnv<'_>,
[1m[94m467[0m [1m[94m|[0m [1m[33m|[0m     _class: JClass<'_>,
[1m[94m468[0m [1m[94m|[0m [1m[33m|[0m ) {
    [1m[94m|[0m [1m[33m|_^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#missing_safety_doc

[1m[33mwarning[0m[1m: this `if` statement can be collapsed[0m
   [1m[94m--> [0msrc/lib.rs:469:5
    [1m[94m|[0m
[1m[94m469[0m [1m[94m|[0m [1m[33m/[0m     if let Ok(mut lock) = GLOBAL_RUST_ENGINE.lock() {
[1m[94m470[0m [1m[94m|[0m [1m[33m|[0m         if let Some(ref mut engine) = *lock {
[1m[94m471[0m [1m[94m|[0m [1m[33m|[0m             if let Err(e) = engine.start() {
[1m[94m472[0m [1m[94m|[0m [1m[33m|[0m                 error!("Error al iniciar motor Rust: {}", e);
[1m[94m...[0m   [1m[33m|[0m
[1m[94m475[0m [1m[94m|[0m [1m[33m|[0m     }
    [1m[94m|[0m [1m[33m|_____^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#collapsible_if
[1m[96mhelp[0m: collapse nested if block
    [1m[94m|[0m
[1m[94m469[0m [92m~ [0m    if let Ok(mut lock) = GLOBAL_RUST_ENGINE.lock()
[1m[94m470[0m [92m~ [0m        [92m&&[0m let Some(ref mut engine) = *lock {
[1m[94m471[0m [1m[94m|[0m             if let Err(e) = engine.start() {
[1m[94m472[0m [1m[94m|[0m                 error!("Error al iniciar motor Rust: {}", e);
[1m[94m473[0m [1m[94m|[0m             }
[1m[94m474[0m [92m~ [0m        }
    [1m[94m|[0m

[1m[33mwarning[0m[1m: this `if` statement can be collapsed[0m
   [1m[94m--> [0msrc/lib.rs:470:9
    [1m[94m|[0m
[1m[94m470[0m [1m[94m|[0m [1m[33m/[0m         if let Some(ref mut engine) = *lock {
[1m[94m471[0m [1m[94m|[0m [1m[33m|[0m             if let Err(e) = engine.start() {
[1m[94m472[0m [1m[94m|[0m [1m[33m|[0m                 error!("Error al iniciar motor Rust: {}", e);
[1m[94m473[0m [1m[94m|[0m [1m[33m|[0m             }
[1m[94m474[0m [1m[94m|[0m [1m[33m|[0m         }
    [1m[94m|[0m [1m[33m|_________^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#collapsible_if
[1m[96mhelp[0m: collapse nested if block
    [1m[94m|[0m
[1m[94m470[0m [92m~ [0m        if let Some(ref mut engine) = *lock
[1m[94m471[0m [92m~ [0m            [92m&&[0m let Err(e) = engine.start() {
[1m[94m472[0m [1m[94m|[0m                 error!("Error al iniciar motor Rust: {}", e);
[1m[94m473[0m [92m~ [0m            }
    [1m[94m|[0m

[1m[33mwarning[0m[1m: unsafe function's docs are missing a `# Safety` section[0m
   [1m[94m--> [0msrc/lib.rs:479:1
    [1m[94m|[0m
[1m[94m479[0m [1m[94m|[0m [1m[33m/[0m pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativePauseRustEncoding(
[1m[94m480[0m [1m[94m|[0m [1m[33m|[0m     _env: JNIEnv<'_>,
[1m[94m481[0m [1m[94m|[0m [1m[33m|[0m     _class: JClass<'_>,
[1m[94m482[0m [1m[94m|[0m [1m[33m|[0m ) {
    [1m[94m|[0m [1m[33m|_^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#missing_safety_doc

[1m[33mwarning[0m[1m: this `if` statement can be collapsed[0m
   [1m[94m--> [0msrc/lib.rs:483:5
    [1m[94m|[0m
[1m[94m483[0m [1m[94m|[0m [1m[33m/[0m     if let Ok(lock) = GLOBAL_RUST_ENGINE.lock() {
[1m[94m484[0m [1m[94m|[0m [1m[33m|[0m         if let Some(ref engine) = *lock {
[1m[94m485[0m [1m[94m|[0m [1m[33m|[0m             engine.pause();
[1m[94m486[0m [1m[94m|[0m [1m[33m|[0m         }
[1m[94m487[0m [1m[94m|[0m [1m[33m|[0m     }
    [1m[94m|[0m [1m[33m|_____^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#collapsible_if
[1m[96mhelp[0m: collapse nested if block
    [1m[94m|[0m
[1m[94m483[0m [92m~ [0m    if let Ok(lock) = GLOBAL_RUST_ENGINE.lock()
[1m[94m484[0m [92m~ [0m        [92m&&[0m let Some(ref engine) = *lock {
[1m[94m485[0m [1m[94m|[0m             engine.pause();
[1m[94m486[0m [92m~ [0m        }
    [1m[94m|[0m

[1m[33mwarning[0m[1m: unsafe function's docs are missing a `# Safety` section[0m
   [1m[94m--> [0msrc/lib.rs:491:1
    [1m[94m|[0m
[1m[94m491[0m [1m[94m|[0m [1m[33m/[0m pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeResumeRustEncoding(
[1m[94m492[0m [1m[94m|[0m [1m[33m|[0m     _env: JNIEnv<'_>,
[1m[94m493[0m [1m[94m|[0m [1m[33m|[0m     _class: JClass<'_>,
[1m[94m494[0m [1m[94m|[0m [1m[33m|[0m ) {
    [1m[94m|[0m [1m[33m|_^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#missing_safety_doc

[1m[33mwarning[0m[1m: this `if` statement can be collapsed[0m
   [1m[94m--> [0msrc/lib.rs:495:5
    [1m[94m|[0m
[1m[94m495[0m [1m[94m|[0m [1m[33m/[0m     if let Ok(lock) = GLOBAL_RUST_ENGINE.lock() {
[1m[94m496[0m [1m[94m|[0m [1m[33m|[0m         if let Some(ref engine) = *lock {
[1m[94m497[0m [1m[94m|[0m [1m[33m|[0m             engine.resume();
[1m[94m498[0m [1m[94m|[0m [1m[33m|[0m         }
[1m[94m499[0m [1m[94m|[0m [1m[33m|[0m     }
    [1m[94m|[0m [1m[33m|_____^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#collapsible_if
[1m[96mhelp[0m: collapse nested if block
    [1m[94m|[0m
[1m[94m495[0m [92m~ [0m    if let Ok(lock) = GLOBAL_RUST_ENGINE.lock()
[1m[94m496[0m [92m~ [0m        [92m&&[0m let Some(ref engine) = *lock {
[1m[94m497[0m [1m[94m|[0m             engine.resume();
[1m[94m498[0m [92m~ [0m        }
    [1m[94m|[0m

[1m[33mwarning[0m[1m: unsafe function's docs are missing a `# Safety` section[0m
   [1m[94m--> [0msrc/lib.rs:503:1
    [1m[94m|[0m
[1m[94m503[0m [1m[94m|[0m [1m[33m/[0m pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeStopRustEncoding(
[1m[94m504[0m [1m[94m|[0m [1m[33m|[0m     _env: JNIEnv<'_>,
[1m[94m505[0m [1m[94m|[0m [1m[33m|[0m     _class: JClass<'_>,
[1m[94m506[0m [1m[94m|[0m [1m[33m|[0m ) {
    [1m[94m|[0m [1m[33m|_^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#missing_safety_doc

[1m[33mwarning[0m[1m: this `if` statement can be collapsed[0m
   [1m[94m--> [0msrc/lib.rs:507:5
    [1m[94m|[0m
[1m[94m507[0m [1m[94m|[0m [1m[33m/[0m     if let Ok(mut lock) = GLOBAL_RUST_ENGINE.lock() {
[1m[94m508[0m [1m[94m|[0m [1m[33m|[0m         if let Some(ref mut engine) = lock.take() {
[1m[94m509[0m [1m[94m|[0m [1m[33m|[0m             engine.stop();
[1m[94m510[0m [1m[94m|[0m [1m[33m|[0m         }
[1m[94m511[0m [1m[94m|[0m [1m[33m|[0m     }
    [1m[94m|[0m [1m[33m|_____^[0m
    [1m[94m|[0m
    [1m[94m= [0m[1mhelp[0m: for further information visit https://rust-lang.github.io/rust-clippy/rust-1.99.0/index.html#collapsible_if
[1m[96mhelp[0m: collapse nested if block
    [1m[94m|[0m
[1m[94m507[0m [92m~ [0m    if let Ok(mut lock) = GLOBAL_RUST_ENGINE.lock()
[1m[94m508[0m [92m~ [0m        [92m&&[0m let Some(ref mut engine) = lock.take() {
[1m[94m509[0m [1m[94m|[0m             engine.stop();
[1m[94m510[0m [92m~ [0m        }
    [1m[94m|[0m

[1m[33mwarning[0m: `velorec_rust2024` (lib) generated 12 warnings (run `cargo clippy --fix --lib -p velorec_rust2024 -- -W clippy::all` to apply 7 suggestions)
[1m[92m    Finished[0m `dev` profile [unoptimized + debuginfo] target(s) in 0.32s
```

### ⚡ Hallazgos de C++23 (cppcheck)
```text
Checking android/app/src/main/cpp/GpuShaderPipeline.cpp ...
android/app/src/main/cpp/GpuShaderPipeline.cpp:322:25: style: The member function 'velorec::gpu::GpuShaderPipeline::unbind' can be static. [functionStatic]
void GpuShaderPipeline::unbind() {
                        ^
android/app/src/main/cpp/GpuShaderPipeline.hpp:86:10: note: The member function 'velorec::gpu::GpuShaderPipeline::unbind' can be static.
    void unbind();
         ^
android/app/src/main/cpp/GpuShaderPipeline.cpp:322:25: note: The member function 'velorec::gpu::GpuShaderPipeline::unbind' can be static.
void GpuShaderPipeline::unbind() {
                        ^
android/app/src/main/cpp/GpuShaderPipeline.cpp:162:55: style: The member function 'velorec::gpu::GpuShaderPipeline::compileGlslShader' can be static. [functionStatic]
std::expected<GLuint, std::string> GpuShaderPipeline::compileGlslShader(GLenum type, std::string_view source) {
                                                      ^
android/app/src/main/cpp/GpuShaderPipeline.hpp:93:40: note: The member function 'velorec::gpu::GpuShaderPipeline::compileGlslShader' can be static.
    std::expected<GLuint, std::string> compileGlslShader(GLenum type, std::string_view source);
                                       ^
android/app/src/main/cpp/GpuShaderPipeline.cpp:162:55: note: The member function 'velorec::gpu::GpuShaderPipeline::compileGlslShader' can be static.
std::expected<GLuint, std::string> GpuShaderPipeline::compileGlslShader(GLenum type, std::string_view source) {
                                                      ^
Checking android/app/src/main/cpp/GpuShaderPipeline.cpp: GL_SHADER_BINARY_FORMAT_SPIR_V;GL_SPIR_V_BINARY_ANGLE...
1/3 files checked 38% done
Checking android/app/src/main/cpp/NativeRecorderEngine.cpp ...
2/3 files checked 84% done
Checking android/app/src/main/cpp/libyuv_bridge.cpp ...
Checking android/app/src/main/cpp/libyuv_bridge.cpp: __ARM_NEON;__aarch64__...
3/3 files checked 100% done
```
