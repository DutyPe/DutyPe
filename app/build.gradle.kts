import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.partimes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.partimes"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material3:material3:1.2.0-beta01")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0-alpha12")
    implementation("androidx.navigation:navigation-compose:2.7.7") // or latest version
    implementation("androidx.compose.material:material-icons-extended:1.6.0") // or latest version
    implementation(libs.coil.compose)

    implementation ("androidx.compose.runtime:runtime-livedata:1.6.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation (libs.androidx.compose.material3.material33)  // Use the correct version of Material3


    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2") // Check latest version
    implementation ("com.google.accompanist:accompanist-flowlayout:0.31.5-beta")
    implementation ("com.google.android.material:material:1.11.0") // Check latest version

    implementation ("com.google.accompanist:accompanist-swiperefresh:0.28.0")
    implementation ("com.google.accompanist:accompanist-pager:0.28.0")
    implementation ("com.google.accompanist:accompanist-pager-indicators:0.28.0")

    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.airbnb.android:lottie-compose:6.0.0")

        implementation ("androidx.compose.material3:material3:1.1.0")
        implementation ("com.google.accompanist:accompanist-permissions:0.37.3")
        implementation ("com.google.android.gms:play-services-location:21.0.1")
        implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.0")
        implementation ("com.google.accompanist:accompanist-systemuicontroller:0.31.5-beta") // For system UI control
}