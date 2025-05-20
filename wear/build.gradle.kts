plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.randomadjective.prototipodatalayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.randomadjective.prototipodatalayer"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.wear)
    implementation(libs.play.services.wearable)
    implementation("androidx.wear:wear:1.2.0")
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.wear.tiles:tiles-material:1.2.0")
    implementation("androidx.wear:wear:1.3.0")
    implementation(libs.androidx.appcompat)
}