plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.8.0"
    id("com.google.gms.google-services") // Requerido para Firebase
}

android {
    namespace = "com.example.ar_muebles"  // <-- Añadir esta línea

    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ar_muebles"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true // Habilitar ViewBinding para generar las clases de binding automáticamente
    }

    compileOptions {
        // Cambia la versión de la JVM a 17 para estar alineada con Kotlin
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        // Asegúrate de que la versión de la JVM sea 17, igual que compileOptions
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {

    // ARCore para habilitar la realidad aumentada en el dispositivo
    implementation("com.google.ar:core:1.37.0")

    // SceneView con ARCore integrado
    implementation("io.github.sceneview:arsceneview:0.10.0") // Nueva dependencia combinada

    // Firebase y otras dependencias necesarias
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Material Components y otras dependencias estándar de AndroidX
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.3")

    // Play Services para soporte de autenticación de Google
    implementation("com.google.android.gms:play-services-auth:20.5.0")
    implementation("androidx.core:core-ktx:1.10.1")

    // Dependencias para pruebas
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
