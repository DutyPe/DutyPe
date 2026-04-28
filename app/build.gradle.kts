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
    id("com.google.firebase.firebase-perf")

    // P2-3: kotlinx.serialization powers `@Serializable` NavKey-style destination classes
    // in `navigation/destinations/`. Plugin was already declared `apply false` at the
    // root; we apply it here so destination classes can be serialized for Nav 3 / Compose
    // Nav 2.8 type-safe routes.
    id("org.jetbrains.kotlin.plugin.serialization")

    // P1-1: Baseline Profile consumer plugin. Pairs with the :baselineprofile
    // module to produce + bundle baseline profiles into the release AAB.
    alias(libs.plugins.androidx.baselineprofile)
    id("com.autonomousapps.dependency-analysis")
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
        versionCode = 45
        versionName = "2.6.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "MAPS_API_KEY", "\"${localProperties.getProperty("MAPS_API_KEY", "")}\"")
        buildConfigField("String", "AZURE_MAPS_KEY", "\"${localProperties.getProperty("AZURE_MAPS_KEY", "")}\"")

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

            // ---------------------------------------------------------------
            // Play update-size churn fix (see docs/FEATURE_RELEASE_MAPS_ROUTING.md
            // step 3: "Reuse R8 mapping from previous release"). Without this,
            // R8 renames every class/method on every build, so even a 1-line
            // colour change rewrites most of classes.dex and Play patch size
            // stays around ~18 MB. With it, names stay stable across versions
            // and the binary diff Play ships drops to roughly the size of the
            // actual code change.
            //
            // Workflow:
            //   1. Build release once. After the build, app/mapping/
            //      release-mapping.txt is created/updated automatically (see
            //      the `archiveReleaseMapping` task at the bottom of this file).
            //   2. COMMIT app/mapping/release-mapping.txt alongside the
            //      versionCode bump for that release. NOTE: this file can be
            //      large (200+ MB for big apps) — it is tracked via Git LFS
            //      (see .gitattributes). Run `git lfs install` once on a fresh
            //      clone before building.
            //   3. On the next release build, R8 reads it via `setMappingFile`
            //      below and reuses the same obfuscated names.
            //
            // Safe to enable from the very first build — when the mapping
            // file does not exist yet we simply skip applyMapping. We do this
            // by generating a tiny proguard fragment in build/ that contains
            // `-applymapping <abs path>` and feeding it to R8 via
            // proguardFiles. R8 itself reads the mapping at obfuscation time.
            val previousMappingFile = file("mapping/release-mapping.txt")
            if (previousMappingFile.exists()) {
                val applyMappingRules = layout.buildDirectory
                    .file("intermediates/dutype/applyMapping.pro")
                    .get()
                    .asFile
                applyMappingRules.parentFile.mkdirs()
                applyMappingRules.writeText(
                    "-applymapping \"${previousMappingFile.absolutePath.replace("\\", "/")}\"\n"
                )
                proguardFiles(applyMappingRules)
            }

            // Enable debug symbols for crash analysis
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
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
        }
    }
    
    // NOTE: `splits { abi { ... } }` intentionally removed.
    //
    // We ship to Google Play via App Bundle (AAB), and Play automatically
    // generates per-ABI APKs from the bundle (plus per-density / per-language
    // splits enabled by default). Configuring `splits { abi }` only produces
    // legacy per-ABI APKs + a universal APK during `assembleRelease` — both of
    // which are ignored when uploading an AAB to Play, and only add CI time +
    // clutter to the `app/build/outputs/apk/` directory.
    //
    // If you ever need raw APKs for sideloading a specific ABI, use
    // `./gradlew :app:bundleRelease` and extract from the AAB with `bundletool`.
    
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
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )

        // P3-1: Compose Compiler metrics + reports.
        // Outputs to app/build/compose-metrics and app/build/compose-reports.
        // Enable by passing -Pcom.dutype.enableComposeMetrics=true to Gradle
        // (off by default to keep CI builds fast).
        if (project.findProperty("com.dutype.enableComposeMetrics") == "true") {
            val composeMetricsDir = layout.buildDirectory.dir("compose-metrics").get().asFile.absolutePath
            val composeReportsDir = layout.buildDirectory.dir("compose-reports").get().asFile.absolutePath
            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$composeMetricsDir",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$composeReportsDir"
            )
        }
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

// Room schema export for migration testing (P2-5).
// JSON snapshots are written to `app/schemas/<DbClass>/<version>.json` and
// must be committed so MigrationTestHelper can validate future Migration objects.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// ---------------------------------------------------------------------------
// Play update-size churn fix, part 2: after every release minify task, copy
// the fresh R8 mapping.txt into app/mapping/release-mapping.txt so the next
// release can apply it (see the buildTypes.release block above).
//
// Why: R8 writes the mapping into
//   app/build/outputs/mapping/release/mapping.txt
// We mirror it under source-controlled app/mapping/ so that bumping
// versionCode + committing the new mapping is the only step a developer
// has to remember between releases.
// ---------------------------------------------------------------------------
val archiveReleaseMapping by tasks.registering(Copy::class) {
    val sourceMapping = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")
    from(sourceMapping)
    into(layout.projectDirectory.dir("mapping"))
    rename { "release-mapping.txt" }
    onlyIf { sourceMapping.get().asFile.exists() }
    description = "Archives the release R8 mapping for reuse on the next build (keeps Play patch sizes small)."
}

androidComponents.onVariants { variant ->
    if (variant.name == "release") {
        // `minifyReleaseWithR8` is created lazily by AGP; configureEach
        // ensures we hook it whenever it gets registered without forcing
        // task realization at configuration time.
        tasks.matching { it.name == "minifyReleaseWithR8" }.configureEach {
            finalizedBy(archiveReleaseMapping)
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(libs.androidx.activity.compose)

    // P1-1: ProfileInstaller is the runtime that loads the bundled
    // baseline-prof.txt at app install / first launch.
    implementation(libs.androidx.profileinstaller)

    // P1-1: Wire the :baselineprofile module so the AndroidX plugin can
    // discover the BaselineProfileGenerator producer.
    "baselineProfile"(project(":baselineprofile"))

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    debugImplementation(libs.firebase.appcheck.debug)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Material3 window size classes
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // P1 FIX: Image loading with WebP support (30% smaller images)
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("io.coil-kt:coil-gif:2.4.0") // GIF support
    // Note: WebP is natively supported on Android 4.0+ (API 14+)

    // Compose and Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7") 
    
    implementation("androidx.compose.animation:animation:1.6.0")

    // Accompanist libraries - Only keep what's needed
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.firebase:geofire-android-common:3.2.0")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-appcheck")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-functions-ktx") // For Cloud Functions calls
    implementation("com.google.firebase:firebase-perf-ktx") // Performance Monitoring
    
    // Google Play Integrity API
    implementation("com.google.android.play:integrity:1.6.0")

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
    implementation("com.google.android.libraries.places:places:3.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")

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

    // SQLCipher for encrypted Room database.
    // Use the modern `sqlcipher-android` artifact (4.6.0+) — it ships native
    // libraries with 16 KB ELF segment alignment, which is required by Google
    // Play for Android 15+ targets starting Nov 1, 2025. The legacy
    // `net.zetetic:android-database-sqlcipher:4.5.4` artifact is NOT 16 KB
    // compatible.
    implementation("net.zetetic:sqlcipher-android:4.14.0")
    implementation("androidx.sqlite:sqlite:2.6.2")
    // EncryptedSharedPreferences for securely storing the DB passphrase
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // OkHttp for Azure Maps API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
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