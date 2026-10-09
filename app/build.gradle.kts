plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.example.monitordecuidados"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.monitordecuidados"
        minSdk = 24
        targetSdk = 35
        versionCode = 10000  // v1.0.0-beta
        versionName = "v1.0.0-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // SPEC SEC-04: Secrets Management
        buildConfigField("String", "DEFAULT_MONITOR_IP", "\"192.168.1.100\"")
        buildConfigField("String", "DEFAULT_TERMINAL_IP", "\"192.168.1.101\"")
        buildConfigField("int", "HTTP_PORT", "8080")
        buildConfigField("int", "AUDIO_PORT", "9000")
        buildConfigField("int", "VIDEO_PORT", "9001")
        buildConfigField("boolean", "LOG_TO_FILE", "true")
        buildConfigField("boolean", "DEBUG_LOGGING", "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "DEFAULT_MONITOR_IP", "\"0.0.0.0\"")
            buildConfigField("String", "DEFAULT_TERMINAL_IP", "\"0.0.0.0\"")
            buildConfigField("boolean", "DEBUG_LOGGING", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.localbroadcastmanager)
    
    // Security & Crypto
    implementation(libs.androidx.security.crypto)
    implementation(libs.sqlcipher)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.play.services.auth)

    // QR & ML Kit
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation("com.google.guava:guava:33.0.0-android")
    implementation(libs.zxing.core)

    // Local Communication & Utils
    implementation(libs.nanohttpd)
    implementation(libs.jmdns)
    implementation(libs.okhttp)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Image Loading
    implementation(libs.glide)

    // Testing (Task 7 + Phase 6)
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
}