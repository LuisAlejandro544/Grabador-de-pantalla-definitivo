/**
 * build.gradle.kts (Módulo :app)
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Configura la compilación completa de la aplicación Android en Gradle Kotlin DSL integrando:
 * 1. Kotlin Coroutines para ejecución concurrente segura en segundo plano (Dispatchers.IO).
 * 2. Compilación de C++23 mediante CMake y Android NDK con flags -std=c++23 y -O3.
 * 3. Compilación de Rust (Edition 2024) mediante tarea personalizada de Cargo NDK
 *    generando bibliotecas nativas .so para arquitecturas de 64 bits (arm64-v8a) y 32 bits (armeabi-v7a).
 * 4. Versión mínima (minSdk) fijada en 26 (Android 8.0 Oreo) para compatibilidad con Foreground Services
 *    y MediaCodec, y targetSdk 35 (Android 15) para cumplimiento estricto con FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.velorec.recorder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.velorec.recorder"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Soporte completo para arquitecturas móviles de 64 y 32 bits
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }

        // Parámetros para el compilador de C++23
        externalNativeBuild {
            cmake {
                arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DCMAKE_CXX_STANDARD=23",
                    "-DANDROID_TOOLCHAIN=clang"
                )
                cppFlags("-std=c++23", "-O3")
            }
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Configuración del sistema de compilación CMake para C++23
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        getByName("main") {
            // Incluye las bibliotecas compartidas .so generadas por Rust en jniLibs
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// =========================================================================
// TAREA GRADLE PARA COMPILAR RUST 2024 CON CARGO-NDK PARA 32 Y 64 BITS
// =========================================================================
tasks.register("cargoNdkBuild") {
    description = "Compila la biblioteca Rust (Edition 2024) para todas las arquitecturas móviles con Cargo-NDK"
    group = "build"

    doLast {
        val targets = listOf(
            "aarch64-linux-android" to "arm64-v8a",     // 64-bit ARM
            "armv7-linux-androideabi" to "armeabi-v7a", // 32-bit ARM
            "x86_64-linux-android" to "x86_64"         // Emulador x86_64
        )

        val rustDir = rootProject.projectDir.resolve("../rust").normalize()
        val jniLibsDir = file("src/main/jniLibs")

        if (rustDir.exists()) {
            jniLibsDir.mkdirs()
            for ((rustTarget, androidAbi) in targets) {
                println("🔨 Compilando Rust 2024 para arquitectura: $androidAbi ($rustTarget)...")
                try {
                    // Se utiliza ProcessBuilder de la JVM estándar para garantizar compatibilidad total
                    // en Gradle Kotlin DSL (evitando fallos de resolución de ámbito con 'exec' en Gradle 8+)
                    val process = ProcessBuilder(
                        "cargo", "ndk",
                        "-t", rustTarget,
                        "-o", jniLibsDir.absolutePath,
                        "build", "--release"
                    )
                        .directory(rustDir)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start()

                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        println("⚠️ Advertencia: cargo-ndk finalizó con código de error $exitCode para $androidAbi.")
                    } else {
                        println("✅ Rust 2024 compilado con éxito para $androidAbi.")
                    }
                } catch (e: Exception) {
                    println("⚠️ Advertencia en cargo-ndk ($androidAbi): ${e.message}. Asegúrate de tener instalado cargo-ndk en el entorno.")
                }
            }
            println("✅ Tarea de Rust 2024 finalizada.")
        } else {
            println("ℹ️ Directorio Rust no encontrado en ${rustDir.absolutePath}, omitiendo cargoNdkBuild.")
        }
    }
}

// Asegurar que la compilación de Rust se ejecute antes de empaquetar el APK
tasks.named("preBuild") {
    dependsOn("cargoNdkBuild")
}

dependencies {
    // React Native
    implementation("com.facebook.react:react-android:+")

    // Kotlin Coroutines para evitar bloqueos en el hilo de la interfaz de usuario
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // AndroidX y Servicios en primer plano
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
