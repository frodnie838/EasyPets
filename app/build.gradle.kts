plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.easypets"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.easypets"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.material:material:1.11.0")  //Dependencia de Material Components
    // Dependencias Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.8.0")) //Firebase BoM -> Herramientas Generales de Firebase
    implementation("com.google.firebase:firebase-analytics") // Herramientas de Análisis de Uso
    implementation("com.google.firebase:firebase-database") // Módulo Firebase Real Time
}