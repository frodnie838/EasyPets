import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.easypets"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.easypets"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Leer la clave de local.properties de forma segura
        val properties = Properties()
        val propertiesFile = rootProject.file("local.properties")
        if (propertiesFile.exists()) {
            properties.load(FileInputStream(propertiesFile))
        }
        val apiKey = properties.getProperty("MAPS_API_KEY") ?: ""

        // Creamos la variable que usaremos en Java
        resValue("string", "MAPS_API_KEY", apiKey)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging:25.0.1")

    // Google Sign In
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.material:material:1.11.0")

    // Pruebas
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Calendario
    implementation("com.applandeo:material-calendar-view:1.9.0")

    // Conectar la API
    implementation("com.android.volley:volley:1.2.1")
    // Cargar fotos de Internet en ImageView
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // Librería para programar tareas en segundo plano (Recordatorios)
    implementation("androidx.work:work-runtime:2.9.0")
}