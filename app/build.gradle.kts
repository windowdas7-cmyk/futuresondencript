plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// GitHub हर बिल्ड पर run number बढ़ाता है, वही versionCode बनेगा (अपने-आप अपग्रेड)
val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.futuristic.sounds"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.futuristic.sounds"
        minSdk = 24
        targetSdk = 34
        versionCode = runNumber
        versionName = "1.$runNumber"
    }

    // हर बार एक ही कुंजी (key) से साइन होगा, तभी नया APK पुराने के ऊपर install होगा
    signingConfigs {
        create("fixed") {
            storeFile = file("futuristic.keystore")
            storePassword = "futuristic123"
            keyAlias = "futuristic"
            keyPassword = "futuristic123"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("fixed")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
