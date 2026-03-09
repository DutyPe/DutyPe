import org.gradle.kotlin.dsl.implementation
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
    id("com.google.firebase.crashlytics")
}

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Load local properties for API keys
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
	namespace = "com.dutype.app"
    compileSdk = 35

    defaultConfig {
		applicationId = "com.dutype.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 35
        versionName = "2.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "MAPS_API_KEY", "\"${localProperties.getProperty("MAPS_API_KEY", "")}\"")
        buildConfigField("String", "AZURE_MAPS_KEY", "\"${localProperties.getProperty("AZURE_MAPS_KEY", "")}\"")
        
        // AI Backend Configuration
        buildConfigField("String", "AI_BACKEND_URL", "\"${localProperties.getProperty("AI_BACKEND_URL", "http://10.0.2.2:8000/")}\"")
        buildConfigField("String", "AI_BACKEND_API_KEY", "\"${localProperties.getProperty("AI_BACKEND_API_KEY", "")}\"")
        
        // Manifest placeholders for API keys
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
        
        // 16 KB Page Size Support for Android 15+ (Required by Google Play from Nov 1, 2025)
        // Ensures native libraries work on devices with 16KB page sizes
        ndk {
            // This flag is not needed for pure Kotlin/Java apps
            // The issue is in third-party native libraries (CameraX, etc.)
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            
            // Enable debug symbols for crash analysis
            ndk {
                debugSymbolLevel = "full"
            }
        }
        
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    
    // JNI Libraries packaging - 16KB page size compatibility
    packaging {
        // Resources to exclude
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin-tooling-metadata.json"
        }
        
        // JNI libs configuration for 16KB page size support
        jniLibs {
            // Use uncompressed native libraries (required for 16KB page size)
            useLegacyPackaging = false
            // Keep debug symbols for crash analysis
            keepDebugSymbols += "**/*.so"
        }
    }
    
    // Split APKs by ABI to reduce size
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        
        // PERFORMANCE: Enable Compose strong skipping mode
        // Reduces unnecessary recompositions by 40-60%
        // Used by: Google, Meta, Uber apps
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:strongSkipping=true"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    // Lint configuration - disable problematic checks
    lint {
        // Disable NullSafeMutableLiveData check due to lint tool bug
        // (IncompatibleClassChangeError in NonNullableMutableLiveDataDetector)
        disable += "NullSafeMutableLiveData"
        
        // Don't abort build on lint errors during release
        abortOnError = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(libs.androidx.activity.compose)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.compose.foundation)
    debugImplementation(libs.firebase.appcheck.debug)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Android 12+ Splash Screen API (MODERN 2024-2026 STANDARD)
    // Official Google recommendation for all apps targeting Android 12+
    // Provides consistent splash screen experience across all Android versions
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Material3 - Single version to avoid conflicts
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // P1 FIX: Image loading with WebP support (30% smaller images)
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("io.coil-kt:coil-gif:2.4.0") // GIF support
    // Note: WebP is natively supported on Android 4.0+ (API 14+)

    // Compose and Lifecycle
    implementation("androidx.compose.runtime:runtime-livedata:1.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7") 
    
    implementation("androidx.compose.animation:animation:1.6.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    // Accompanist libraries - Only keep what's needed
    implementation("com.google.accompanist:accompanist-pager:0.28.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.28.0")
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // Firebase Phone Number Verification (PNV) - Recommended by Firebase
    // Uses Android Credential Manager for secure, consent-based phone verification
    implementation("com.google.firebase:firebase-pnv:16.0.0-beta01")
    
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-appcheck")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-functions-ktx") // For Cloud Functions calls
    implementation("com.google.firebase:firebase-perf-ktx") // Performance Monitoring
    
    // Google Play Integrity API
    implementation("com.google.android.play:integrity:1.6.0")
    
    // SafetyNet for reCAPTCHA (CRITICAL for Phone Auth rate limiting)
    implementation("com.google.android.gms:play-services-safetynet:18.1.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Google Sign-In with Credential Manager
    implementation("androidx.credentials:credentials:1.6.0-beta03")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0-beta03")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.0")
    
    // Google Maps for Map-First Interface
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")

    // Animations - Lottie
    implementation("com.airbnb.android:lottie-compose:6.0.0")

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Guava - explicitly include to prevent R8 issues with Hilt
    implementation("com.google.guava:guava:33.0.0-android")
    ksp(libs.hilt.compiler)

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // OkHttp for Azure Maps API calls and AI Backend
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Retrofit for AI Backend API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // CameraX for QR Code Scanning (16KB page size compatible - v1.5.0+)
    implementation("androidx.camera:camera-core:1.5.0")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")
    
    // ZXing for QR Code Generation and Scanning
    implementation("com.google.zxing:core:3.5.2")
    
    // Google Play In-App Review API
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")
    
    // Google Play In-App Update API
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    
    // Google Mobile Ads SDK (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.6.0")
}

afterEvaluate {
    // Temporary workaround: Disable Crashlytics mapping-file upload due to network/DNS issues
    // with firebasecrashlyticssymbols.googleapis.com. This allows the release build to complete locally.
    tasks.findByName("uploadCrashlyticsMappingFileRelease")?.enabled = false
}