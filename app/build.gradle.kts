plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.pink.easytool"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.pink.easytool"
        minSdk = 26
        targetSdk = 32
        versionCode = 1
        versionName = "0.1.0"
    }
}
