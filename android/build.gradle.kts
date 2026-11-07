/**
 * build.gradle.kts (Raíz de Android)
 * Configura los plugins de Android Application y Kotlin para todo el proyecto.
 */

plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
