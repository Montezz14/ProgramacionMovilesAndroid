plugins {
    id 'com.android.application'
}

android {
    namespace 'com.practica.flappybird'
    compileSdk 34

    defaultConfig {
        applicationId "com.practica.flappybird"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    // El juego no necesita librerías externas: solo SDK de Android nativo.
}
