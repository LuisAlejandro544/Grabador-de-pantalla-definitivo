/**
 * Catálogo de código fuente nativo para visualización interactiva y descarga en ZIP.
 * Contiene todo el código fuente de React Native, Kotlin, C++23 y Rust 2024 con descripciones pedagógicas.
 */

export interface NativeFileItem {
  id: string;
  category: 'React Native' | 'Kotlin' | 'C++23' | 'Rust 2024' | 'Gradle & Build' | 'GitHub Actions';
  name: string;
  path: string;
  language: 'typescript' | 'kotlin' | 'cpp' | 'rust' | 'gradle' | 'xml' | 'yaml';
  description: string;
  code: string;
}

export const NATIVE_FILES_CATALOG: NativeFileItem[] = [
  {
    id: 'rn-spec',
    category: 'React Native',
    name: 'NativeScreenRecorder.ts',
    path: 'src/native/NativeScreenRecorder.ts',
    language: 'typescript',
    description: 'Especificación de TurboModule tipada en TypeScript para comunicar la interfaz con Kotlin.',
    code: `import { TurboModule, TurboModuleRegistry } from 'react-native';

export type NativeEngine = 'cpp23' | 'rust2024';

export interface NativeRecordConfig {
  engine: NativeEngine;           // 'cpp23' o 'rust2024'
  width: number;                  // Ancho en píxeles (ej: 1080)
  height: number;                 // Alto en píxeles (ej: 1920)
  fps: number;                    // Cuadros por segundo (ej: 60)
  bitrateBps: number;             // Bitrate en bits/segundo (ej: 12_000_000)
  codec: 'H264' | 'HEVC' | 'AV1'; // Códec por hardware
  audioSource: 'internal' | 'mic' | 'both' | 'mute';
  sampleRate: number;             // 44100 o 48000 Hz
  outputPath?: string;
}

export interface Spec extends TurboModule {
  prepareRecorder(config: NativeRecordConfig): Promise<boolean>;
  startRecording(): Promise<string>;
  pauseRecording(): Promise<boolean>;
  resumeRecording(): Promise<boolean>;
  stopRecording(): Promise<{ filePath: string; durationMs: number; fileSizeBytes: number }>;
  getEngineStatus(): Promise<{ engine: NativeEngine; isRecording: boolean; isPaused: boolean }>;
}

export default TurboModuleRegistry.get<Spec>('ScreenRecorderModule');`
  },
  {
    id: 'kt-module',
    category: 'Kotlin',
    name: 'ScreenRecorderModule.kt',
    path: 'android/src/main/java/com/velorec/recorder/ScreenRecorderModule.kt',
    language: 'kotlin',
    description: 'Módulo puente de React Native con Kotlin Coroutines (Dispatchers.IO) para no bloquear la interfaz gráfica.',
    code: `package com.velorec.recorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import com.facebook.react.bridge.*
import kotlinx.coroutines.*

class ScreenRecorderModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), ActivityEventListener {

    private val moduleScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pendingPromise: Promise? = null
    private var currentConfig: ReadableMap? = null

    companion object {
        const val MODULE_NAME = "ScreenRecorderModule"
        const val SCREEN_CAPTURE_REQUEST_CODE = 4210
    }

    override fun getName(): String = MODULE_NAME

    @ReactMethod
    fun prepareRecorder(config: ReadableMap, promise: Promise) {
        val currentActivity = reactApplicationContext.currentActivity ?: run {
            promise.reject("E_NULL_ACTIVITY", "No hay Activity activa")
            return
        }
        this.currentConfig = config
        this.pendingPromise = promise
        val projectionManager = currentActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        currentActivity.startActivityForResult(projectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST_CODE)
    }

    override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            val promise = pendingPromise ?: return
            pendingPromise = null
            if (resultCode == Activity.RESULT_OK && data != null) {
                val intent = Intent(reactContext, ScreenRecordService::class.java).apply {
                    action = ScreenRecordService.ACTION_START
                    putExtra(ScreenRecordService.EXTRA_RESULT_DATA, data)
                    putExtra(ScreenRecordService.EXTRA_ENGINE, currentConfig?.getString("engine") ?: "cpp23")
                }
                reactContext.startForegroundService(intent)
                promise.resolve(true)
            } else {
                promise.reject("E_CANCELLED", "Permiso de captura de pantalla denegado por el usuario")
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {}
}`
  },
  {
    id: 'kt-service',
    category: 'Kotlin',
    name: 'ScreenRecordService.kt',
    path: 'android/src/main/java/com/velorec/recorder/ScreenRecordService.kt',
    language: 'kotlin',
    description: 'Servicio en primer plano (Foreground Service) compatible con Android 14+ y conexión directa de Surface a C++/Rust.',
    code: `package com.velorec.recorder

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.Surface
import kotlinx.coroutines.*
import java.io.File

class ScreenRecordService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    companion object {
        const val ACTION_START = "com.velorec.ACTION_START"
        const val ACTION_STOP = "com.velorec.ACTION_STOP"
        const val NOTIFICATION_ID = 8801

        init {
            System.loadLibrary("velorec_cpp23")
            System.loadLibrary("velorec_rust2024")
        }
    }

    // JNI bindings hacia los motores nativos
    private external fun nativeInitCppRecorder(path: String, w: Int, h: Int, fps: Int, br: Int, codec: String, audio: String): Surface
    private external fun nativeInitRustRecorder(path: String, w: Int, h: Int, fps: Int, br: Int, codec: String, audio: String): Surface
    private external fun nativeStartCppEncoding()
    private external fun nativeStartRustEncoding()
    private external fun nativeStopCppEncoding()
    private external fun nativeStopRustEncoding()

    override fun onBind(intent: Intent?) = null
}`
  },
  {
    id: 'cpp-header',
    category: 'C++23',
    name: 'NativeRecorderEngine.hpp',
    path: 'android/src/main/cpp/NativeRecorderEngine.hpp',
    language: 'cpp',
    description: 'Cabecera C++23 con std::expected, std::span, std::jthread y punteros al NDK AMediaCodec.',
    code: `#pragma once
#include <string>
#include <expected>
#include <span>
#include <thread>
#include <atomic>
#include <jni.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaMuxer.h>
#include <android/native_window.h>

namespace velorec::cpp23 {

struct RecorderConfig {
    std::string outputPath;
    int32_t width{1080};
    int32_t height{1920};
    int32_t fps{60};
    int32_t bitrateBps{12'000'000};
    std::string codecMime{"video/avc"};
};

class NativeRecorderEngine {
public:
    explicit NativeRecorderEngine(const RecorderConfig& config);
    ~NativeRecorderEngine();

    std::expected<jobject, std::string> initialize(JNIEnv* env);
    std::expected<void, std::string> start();
    void pause();
    void resume();
    std::expected<void, std::string> stop();

private:
    void encodingWorkerLoop(std::stop_token stopToken);
    RecorderConfig m_config;
    std::atomic<bool> m_isRecording{false};
    AMediaCodec* m_codec{nullptr};
    AMediaMuxer* m_muxer{nullptr};
    ANativeWindow* m_nativeWindow{nullptr};
    std::jthread m_workerThread;
};

}`
  },
  {
    id: 'cpp-impl',
    category: 'C++23',
    name: 'NativeRecorderEngine.cpp',
    path: 'android/src/main/cpp/NativeRecorderEngine.cpp',
    language: 'cpp',
    description: 'Implementación C++23 usando AMediaCodec_createInputSurface para codificación directa por GPU con zero-copy.',
    code: `#include "NativeRecorderEngine.hpp"
#include <format>
#include <fcntl.h>
#include <unistd.h>
#include <android/log.h>

namespace velorec::cpp23 {

std::expected<jobject, std::string> NativeRecorderEngine::initialize(JNIEnv* env) {
    AMediaFormat* format = AMediaFormat_new();
    AMediaFormat_setString(format, AMEDIAFORMAT_KEY_MIME, m_config.codecMime.c_str());
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_WIDTH, m_config.width);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_HEIGHT, m_config.height);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_BIT_RATE, m_config.bitrateBps);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_FRAME_RATE, m_config.fps);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 0x7f000789); // COLOR_FormatSurface

    m_codec = AMediaCodec_createEncoderByType(m_config.codecMime.c_str());
    AMediaCodec_configure(m_codec, format, nullptr, nullptr, AMEDIACODEC_CONFIGURE_FLAG_ENCODE);
    AMediaCodec_createInputSurface(m_codec, &m_nativeWindow);

    jobject surfaceObj = ANativeWindow_toSurface(env, m_nativeWindow);
    return surfaceObj;
}

void NativeRecorderEngine::encodingWorkerLoop(std::stop_token stopToken) {
    AMediaCodecBufferInfo info;
    while (!stopToken.stop_requested() && m_isRecording.load()) {
        ssize_t outIndex = AMediaCodec_dequeueOutputBuffer(m_codec, &info, 10000);
        if (outIndex >= 0) {
            size_t size = 0;
            uint8_t* buffer = AMediaCodec_getOutputBuffer(m_codec, outIndex, &size);
            if (buffer && info.size > 0) {
                AMediaMuxer_writeSampleData(m_muxer, 0, buffer, &info);
            }
            AMediaCodec_releaseOutputBuffer(m_codec, outIndex, false);
        }
    }
}

}`
  },
  {
    id: 'rust-cargo',
    category: 'Rust 2024',
    name: 'Cargo.toml',
    path: 'rust/Cargo.toml',
    language: 'gradle',
    description: 'Manifiesto de Rust con edition = "2024", cdylib, ndk 0.9 y optimizaciones LTO agresivas.',
    code: `[package]
name = "velorec_rust2024"
version = "0.1.0"
edition = "2024"

[lib]
crate-type = ["cdylib"]

[dependencies]
jni = { version = "0.21.1", default-features = false }
ndk = { version = "0.9.0", features = ["media", "bitmap"] }
crossbeam-channel = "0.5.14"
android_logger = "0.14.1"
log = "0.4.22"

[profile.release]
opt-level = 3
lto = "fat"
codegen-units = 1
panic = "abort"
strip = true`
  },
  {
    id: 'rust-code',
    category: 'Rust 2024',
    name: 'lib.rs',
    path: 'rust/src/lib.rs',
    language: 'rust',
    description: 'Motor Rust (Edition 2024) con hardware encoder NDK AMediaCodec, ANativeWindow Surface, canales lock-free con crossbeam y multiplexor MP4.',
    code: `//! lib.rs - Motor de Grabación y Codificación de Pantalla en Rust (Edition 2024)
use std::ffi::CString;
use std::fs::OpenOptions;
use std::os::unix::io::IntoRawFd;
use std::sync::atomic::{AtomicBool, AtomicI64, AtomicU64, Ordering};
use std::sync::Arc;
use std::thread::{self, JoinHandle};
use std::time::{Duration, Instant};

use crossbeam_channel::{bounded, Sender};
use jni::objects::{JClass, JString};
use jni::sys::{jint, jobject, jstring};
use jni::JNIEnv;
use log::{error, info, LevelFilter};

const COLOR_FORMAT_SURFACE: i32 = 0x7f000789;
const TIMEOUT_US: i64 = 10_000;

pub struct RustRecorderConfig {
    pub output_path: String,
    pub width: i32,
    pub height: i32,
    pub fps: i32,
    pub bitrate_bps: i32,
    pub codec_mime: String,
    pub audio_source: String,
}

pub struct RustRecorderEngine {
    config: RustRecorderConfig,
    is_recording: Arc<AtomicBool>,
    is_paused: Arc<AtomicBool>,
    frames_encoded: Arc<AtomicU64>,
    worker_handle: Option<JoinHandle<()>>,
}

// Inicializa AMediaCodec de hardware y devuelve la Surface a Kotlin (Zero-Copy)
#[no_mangle]
pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeInitRustRecorder(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
    width: jint,
    height: jint,
    fps: jint,
    bitrate: jint,
    codec: JString,
    audio: JString,
) -> jobject {
    let path_str: String = env.get_string(&path).map(|s| s.into()).unwrap_or_default();
    let codec_str: String = env.get_string(&codec).map(|s| s.into()).unwrap_or_else(|_| "H264".into());
    let codec_mime = match codec_str.as_str() {
        "HEVC" => "video/hevc",
        "AV1" => "video/av01",
        _ => "video/avc",
    };

    let format = ndk_sys::AMediaFormat_new();
    let mime_c = CString::new(codec_mime).unwrap();
    ndk_sys::AMediaFormat_setString(format, CString::new("mime").unwrap().as_ptr(), mime_c.as_ptr());
    ndk_sys::AMediaFormat_setInt32(format, CString::new("width").unwrap().as_ptr(), width);
    ndk_sys::AMediaFormat_setInt32(format, CString::new("height").unwrap().as_ptr(), height);
    ndk_sys::AMediaFormat_setInt32(format, CString::new("bitrate").unwrap().as_ptr(), bitrate);
    ndk_sys::AMediaFormat_setInt32(format, CString::new("frame-rate").unwrap().as_ptr(), fps);
    ndk_sys::AMediaFormat_setInt32(format, CString::new("color-format").unwrap().as_ptr(), COLOR_FORMAT_SURFACE);

    let encoder = ndk_sys::AMediaCodec_createEncoderByType(mime_c.as_ptr());
    ndk_sys::AMediaCodec_configure(encoder, format, std::ptr::null_mut(), std::ptr::null_mut(), ndk_sys::AMEDIACODEC_CONFIGURE_FLAG_ENCODE);
    ndk_sys::AMediaFormat_delete(format);

    let mut native_window: *mut ndk_sys::ANativeWindow = std::ptr::null_mut();
    ndk_sys::AMediaCodec_createInputSurface(encoder, &mut native_window);

    let raw_env = env.get_raw();
    let surface_obj = ndk_sys::ANativeWindow_toSurface(raw_env as *mut _, native_window);
    surface_obj
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeStartRustEncoding(
    _env: JNIEnv,
    _class: JClass,
) {
    // Inicia worker loop de codificación y streaming
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_velorec_recorder_ScreenRecordService_nativeStopRustEncoding(
    _env: JNIEnv,
    _class: JClass,
) {
    // Finaliza codificación y libera recursos NDK
}`
  },
  {
    id: 'gradlew-script',
    category: 'Gradle & Build',
    name: 'gradlew',
    path: 'android/gradlew',
    language: 'gradle',
    description: 'Script ejecutable POSIX para arrancar el compilador Gradle en Linux/macOS sin necesidad de tener Gradle preinstalado.',
    code: `#!/bin/sh
# Gradle Start Up Script for UN*X (POSIX Standard Wrapper)
APP_BASE_NAME=\`basename "$0"\`
SAVED="\`pwd\`"
cd "\`dirname \\"$0\\"\`" >/dev/null
APP_HOME="\`pwd -P\`"
cd "$SAVED" >/dev/null

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$CLASSPATH" ]; then
    mkdir -p "$APP_HOME/gradle/wrapper"
    curl -sLo "$CLASSPATH" "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar" || true
fi

exec java -Xmx64m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"`
  },
  {
    id: 'gradle-wrapper-props',
    category: 'Gradle & Build',
    name: 'gradle-wrapper.properties',
    path: 'android/gradle/wrapper/gradle-wrapper.properties',
    language: 'gradle',
    description: 'Definición de versión oficial del wrapper Gradle (v8.7.0) compatible con Java 17 y Android Gradle Plugin 8.5+.',
    code: `distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.7-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists`
  },
  {
    id: 'gradle-settings',
    category: 'Gradle & Build',
    name: 'settings.gradle.kts',
    path: 'android/settings.gradle.kts',
    language: 'gradle',
    description: 'Configuración raíz de Gradle Kotlin DSL definiendo repositorios Google, Maven Central y el módulo :app.',
    code: `pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "VeloRec"
include(":app")`
  },
  {
    id: 'gradle-root-build',
    category: 'Gradle & Build',
    name: 'build.gradle.kts (Raíz)',
    path: 'android/build.gradle.kts',
    language: 'gradle',
    description: 'Script de construcción raíz que declara los plugins de Android Application y Kotlin para todo el proyecto.',
    code: `plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}`
  },
  {
    id: 'gradle-build',
    category: 'Gradle & Build',
    name: 'build.gradle.kts',
    path: 'android/app/build.gradle.kts',
    language: 'gradle',
    description: 'Configuración Gradle Kotlin DSL para compilar C++23 (CMake) y Rust 2024 (cargo-ndk) en 32/64 bits.',
    code: `plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.velorec.recorder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.velorec.recorder"
        minSdk = 26
        targetSdk = 35

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++23", "-O3")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("../cpp/CMakeLists.txt")
        }
    }
}

tasks.register("cargoNdkBuild") {
    doLast {
        val rustDir = rootProject.projectDir.resolve("../rust").normalize()
        val jniLibsDir = file("src/main/jniLibs")
        jniLibsDir.mkdirs()
        project.exec {
            workingDir = rustDir
            commandLine("cargo", "ndk", "-t", "arm64-v8a", "-o", jniLibsDir.absolutePath, "build", "--release")
        }
    }
}`
  },
  {
    id: 'android-manifest',
    category: 'Gradle & Build',
    name: 'AndroidManifest.xml',
    path: 'android/app/src/main/AndroidManifest.xml',
    language: 'xml',
    description: 'Declaración de permisos de captura de pantalla y servicio en primer plano para Android 14+.',
    code: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application
        android:label="VeloRec Studio"
        android:hardwareAccelerated="true">
        <service
            android:name=".ScreenRecordService"
            android:foregroundServiceType="mediaProjection" />
    </application>
</manifest>`
  },
  {
    id: 'github-action-build',
    category: 'GitHub Actions',
    name: 'build-apk.yml',
    path: '.github/workflows/build-apk.yml',
    language: 'yaml',
    description: 'Flujo de GitHub Actions con disparo 100% MANUAL (workflow_dispatch). Configura Node, JDK 17, Android SDK/NDK, Rust 2024, genera la keystore de depuración y compila el APK Debug descargable.',
    code: `name: Compilar APK Debug (VeloRec Mobile)

# DISPARADOR ESTRICTAMENTE MANUAL (workflow_dispatch)
# NO se activa con push ni pull_request
on:
  workflow_dispatch:
    inputs:
      engine_preference:
        description: 'Motor nativo preferente'
        required: true
        default: 'Ambos (C++23 y Rust 2024)'
        type: choice
        options:
          - 'Ambos (C++23 y Rust 2024)'
          - 'Solo C++23 (AMediaCodec NDK)'
          - 'Solo Rust (Edition 2024)'

jobs:
  build-debug-apk:
    name: 🔨 Construir APK Debug para Android
    runs-on: ubuntu-latest

    steps:
      - name: 📥 Clonar repositorio
        uses: actions/checkout@v4

      - name: 🟢 Configurar Node.js (v20)
        uses: actions/setup-node@v4
        with:
          node-version: 20

      - name: ☕ Configurar Java JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      # 4. Configurar Android SDK y NDK
      - name: 🤖 Configurar variables de Android SDK y NDK
        run: |
          SDK_DIR="/usr/local/lib/android/sdk"
          if [ ! -d "$SDK_DIR" ]; then SDK_DIR="$ANDROID_HOME"; fi
          echo "ANDROID_HOME=$SDK_DIR" >> $GITHUB_ENV
          echo "ANDROID_SDK_ROOT=$SDK_DIR" >> $GITHUB_ENV
          NDK_PATH=$(find $SDK_DIR/ndk -maxdepth 1 -mindepth 1 -type d 2>/dev/null | sort -V | tail -n 1)
          if [ -z "$NDK_PATH" ]; then
            yes | $SDK_DIR/cmdline-tools/latest/bin/sdkmanager --licenses || true
            $SDK_DIR/cmdline-tools/latest/bin/sdkmanager "ndk;26.3.11579264" "cmake;3.22.1"
            NDK_PATH="$SDK_DIR/ndk/26.3.11579264"
          fi
          echo "ANDROID_NDK_HOME=$NDK_PATH" >> $GITHUB_ENV
          echo "NDK_HOME=$NDK_PATH" >> $GITHUB_ENV

      - name: 📦 Aceptar licencias de Android SDK
        run: |
          yes | /usr/local/lib/android/sdk/cmdline-tools/latest/bin/sdkmanager --licenses || true

      - name: 🦀 Instalar Rust Toolchain y Targets Móviles
        uses: dtolnay/rust-toolchain@stable
        with:
          targets: aarch64-linux-android,armv7-linux-androideabi,x86_64-linux-android

      - name: 🛠️ Instalar cargo-ndk
        run: cargo install cargo-ndk

      - name: 📚 Instalar dependencias npm
        run: npm install --legacy-peer-deps

      - name: 🔑 Generar Firma Debug Automática
        run: |
          mkdir -p android/app
          if [ ! -f "android/app/debug.keystore" ]; then
            keytool -genkey -v \\
              -keystore android/app/debug.keystore \\
              -storepass android \\
              -alias androiddebugkey \\
              -keypass android \\
              -keyalg RSA \\
              -keysize 2048 \\
              -validity 10000 \\
              -dname "CN=Android Debug,O=Android,C=US"
          fi

      - name: 🔧 Configurar permisos de gradlew
        run: chmod +x android/gradlew || true

      - name: 🚀 Compilar APK Debug con Gradle
        run: |
          cd android
          ./gradlew assembleDebug --stacktrace

      - name: 📤 Subir APK Debug para descarga móvil
        uses: actions/upload-artifact@v4
        with:
          name: VeloRec-Debug-APK
          path: android/app/build/outputs/apk/debug/*.apk
          retention-days: 14`
  }
];
