plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.carplayer.btplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.carplayer.btplayer"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters.add("armeabi-v7a") }
    }
    buildTypes {
        debug { isMinifyEnabled = false }
        release { isMinifyEnabled = false }
    }
    buildFeatures { viewBinding = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
