// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false // Soporte para Kotlin en Android
    id("com.android.application") version "8.5.2" apply false // Plugin para módulos de aplicaciones Android
    id("com.android.library") version "8.5.2" apply false // Plugin para módulos de librerías Android
    id("com.google.gms.google-services") version "4.3.14" apply false // Plugin de servicios de Google (Firebase)
    // Se eliminó la duplicación de alias para evitar errores
}

buildscript {
    dependencies {
        // Plugin de Android Gradle necesario para Groovy (aunque aquí estamos usando Kotlin DSL)
        classpath("com.android.tools.build:gradle:8.5.2")
    }
}

// Se eliminó la sección `allprojects` y `repositories` ya que esta información debería estar en `settings.gradle.kts`.

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
