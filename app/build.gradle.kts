plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    dependencies {
        implementation("com.android.tools.build:apksig:8.7.3")
    }
}
android {
    namespace = "com.pink.easytool"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    defaultConfig {
        applicationId = "com.pink.easytool"
        minSdk = 26
        targetSdk = 32
        versionCode = 1
        versionName = "0.1.0"
    }
}
