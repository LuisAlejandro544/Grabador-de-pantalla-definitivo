/**
 * settings.gradle.kts - Configuración del proyecto raíz de Android
 * Define los repositorios de plugins y declara los módulos del proyecto.
 */

pluginManagement {
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
include(":app")
