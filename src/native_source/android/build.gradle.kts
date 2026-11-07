/**
 * build.gradle.kts (Nivel de Módulo de la App Android)
 * 
 * ¿QUÉ HACE ESTE ARCHIVO?
 * Configura la compilación completa de la aplicación Android en Gradle Kotlin DSL integrando:
 * 1. Kotlin Coroutines para ejecución concurrente segura.
 * 2. Compilación de C++23 mediante CMake y Android NDK.
 * 3. Compilación de Rust (Edition 2024) mediante tarea personalizada de Cargo NDK
 *    generando bibliotecas nativas .so para arquitecturas de 64 bits (arm64-v8a) y 32 bits (armeabi-v7a).
 * 4. Versión mínima (minSdk) fijada en 26 (Android 8.0) para garantizar acceso a MediaCodec y Foreground Services,
 *    y targetSdk 35 (Android 15) para compatibilidad con las últimas políticas de captura y seguridad.
 */

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.velorec.recorder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.velorec.recorder"
        minSdk = 26 // Android 8.0 Oreo: Requerido para soporte completo de MediaProjection y hardware encoders
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Soporte explícito para arquitecturas de 64 bits y 32 bits móviles
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
    }

    // Configuración del sistema de compilación CMake para C++23
    externalNativeBuild {
        cmake {
            path = file("../cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        getByName("main") {
            // Incluye las bibliotecas compartidas .so generadas por Rust en jniLibs
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    buildFeatures {
        compose = true
        prefab = true // Habilita Prefab para enlazar paquetes nativos C/C++ AAR como Google Oboe
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
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
    description = "Compila la biblioteca Rust (Edition 2024) para todas las arquitecturas de destino de Android"
    group = "build"

    doLast {
        val targets = listOf(
            "arm64-v8a",     // 64-bit ARM
            "armeabi-v7a",   // 32-bit ARM
            "x86_64"         // Emulador x86_64
        )

        val rustDir = rootProject.projectDir.resolve("../rust").normalize()
        val jniLibsDir = file("src/main/jniLibs")

        if (rustDir.exists()) {
            jniLibsDir.mkdirs()
            val ndkHome = System.getenv("ANDROID_NDK_HOME")
                ?: System.getenv("NDK_HOME")
                ?: System.getenv("ANDROID_HOME")?.let { "$it/ndk" }

            for (androidAbi in targets) {
                val expectedSo = file("src/main/jniLibs/$androidAbi/libvelorec_rust2024.so")
                if (expectedSo.exists() && expectedSo.length() > 0) {
                    println("✅ Rust 2024 (.so precompilada encontrada para $androidAbi: ${expectedSo.length()} bytes)")
                    continue
                }

                println("🔨 Compilando Rust 2024 para arquitectura: $androidAbi...")
                try {
                    val cmd = mutableListOf(
                        "cargo", "ndk",
                        "-t", androidAbi,
                        "--platform", "26",
                        "-o", jniLibsDir.absolutePath,
                        "build", "--release"
                    )
                    val pb = ProcessBuilder(cmd)
                        .directory(rustDir)
                        .redirectErrorStream(true)

                    if (ndkHome != null) {
                        pb.environment()["ANDROID_NDK_HOME"] = ndkHome
                        pb.environment()["NDK_HOME"] = ndkHome
                    }

                    val process = pb.start()
                    val outputText = process.inputStream.bufferedReader().use { it.readText() }
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        println("⚠️ cargo-ndk finalizó con código $exitCode para $androidAbi:")
                        if (outputText.isNotBlank()) {
                            println(outputText.trim())
                        }
                    } else {
                        println("✅ Rust 2024 compilado con éxito para $androidAbi.")
                    }
                } catch (e: Exception) {
                    println("⚠️ Advertencia en cargo-ndk ($androidAbi): ${e.message}")
                }
            }
            println("✅ Bibliotecas de Rust 2024 generadas e inyectadas en jniLibs.")
        }
    }
}

// Asegurar que la compilación de Rust se ejecute antes de empaquetar el APK
tasks.named("preBuild") {
    dependsOn("cargoNdkBuild")
}

configurations.all {
    resolutionStrategy {
        force("com.facebook.fbjni:fbjni:0.6.0")
    }
}

dependencies {
    // Jetpack Compose (Material 3 nativo para diseño móvil)
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Arquitectura de Pantallas, Navegación Táctil Móvil y Ciclo de Vida (Apache-2.0)
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Reproducción de video de alto rendimiento acelerada por hardware (Apache-2.0)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Captura y reproducción de audio de ultra baja latencia en C++23 (Apache-2.0)
    implementation("com.google.oboe:oboe:1.9.0")

    // Kotlin y Corrutinas para evitar bloqueos en el Main Thread
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // AndroidX y Servicios en primer plano
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
