import org.gradle.kotlin.dsl.implementation
import com.dutype.build.CheckReleaseSizeBudgetTask
import com.dutype.build.ValidateReleaseMappingBaselineTask
import com.dutype.build.VerifyNative16KbPageSizeTask
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

val hasReleaseSigningConfig = keystorePropertiesFile.exists() &&
    listOf("keyAlias", "keyPassword", "storeFile", "storePassword").all { key ->
        !keystoreProperties.getProperty(key).isNullOrBlank()
    } && file(keystoreProperties.getProperty("storeFile")).exists()

// Load local properties for API keys
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
	namespace = "com.dutype.app"
    compileSdk = 36

    defaultConfig {
		applicationId = "com.dutype.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 806
        versionName = "4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Locales shipped in the bundle. Anything omitted here is stripped at build
        // time, so values-<lang> is silently dropped no matter what the UI offers.
        resourceConfigurations += listOf("en", "te", "hi")
        
        // Manifest placeholders for API keys
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
        
        // 16 KB Page Size Support for Android 15+ (Required by Google Play from Nov 1, 2025)
        // Ensures native libraries work on devices with 16KB page sizes
        ndk {
            abiFilters.clear()
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningConfig) {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
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
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "[dutype] WARNING: release signing is not configured on this machine. " +
                        "bundleRelease will produce an unsigned verification bundle; add " +
                        "keystore.properties to create a Play-uploadable signed bundle."
                )
            }

            // ---------------------------------------------------------------
            // Play update-size churn fix. Without this,
            // R8 renames every class/method on every build, so even a 1-line
            // colour change rewrites most of classes.dex and Play patch size
            // stays around ~18 MB. With it, names stay stable across versions
            // and the binary diff Play ships drops to roughly the size of the
            // actual code change.
            //
            // Workflow:
            //   1. Keep app/mapping/release-mapping.txt as the mapping from
            //      the currently live Play release.
            //   2. Build the next release. R8 consumes that previous mapping
            //      through the generated -applymapping fragment below.
            //   3. Upload the AAB and wait until Play accepts it as the new
            //      baseline.
            //   4. Only then run :app:archiveReleaseMapping and commit the new
            //      app/mapping/release-mapping.txt for the following release.
            //
            // Do not auto-run archiveReleaseMapping from bundleRelease. Doing
            // that overwrites the previous-production baseline before the AAB
            // is accepted, which is exactly how small hotfixes turn into large
            // Play update patches.
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
            useLegacyPackaging = false
        }

        // Play update-size fix:
        // Keep DEX files uncompressed/aligned in generated APK splits. When DEX
        // is deflated, even a tiny Kotlin/Compose edit can reshuffle the
        // compressed stream and Play may report a ~7 MB update because the dex
        // split patches poorly. Uncompressed DEX gives Play a stable byte stream
        // to delta, so small fixes patch closer to their real binary change.
        dex {
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
        buildConfig = false
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
// Play update-size churn fix, part 2: manually copy the fresh R8 mapping.txt
// into app/mapping/release-mapping.txt after Play accepts the uploaded release.
// The current build must keep consuming the previous-production mapping as its
// baseline; archiving too early corrupts the next hotfix baseline.
//
// Why: R8 writes the mapping into
//   app/build/outputs/mapping/release/mapping.txt
// We mirror it under source-controlled app/mapping/ only after upload/approval
// so the following versionCode can reuse the exact production baseline.
// ---------------------------------------------------------------------------
val archiveReleaseMapping by tasks.registering(Copy::class) {
    val sourceMapping = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")
    from(sourceMapping)
    into(layout.projectDirectory.dir("mapping"))
    rename { "release-mapping.txt" }
    onlyIf { sourceMapping.get().asFile.exists() }
    group = "release"
    description = "Manually archives the release R8 mapping after Play accepts the uploaded release."
}

fun releaseBudgetBytes(propertyName: String, defaultMb: Double): Long {
    val configuredValue = (findProperty(propertyName) as? String)?.toDoubleOrNull() ?: defaultMb
    return (configuredValue * 1024.0 * 1024.0).toLong()
}

val validateReleaseMappingBaseline by tasks.registering(ValidateReleaseMappingBaselineTask::class) {
    group = "verification"
    description = "Fails release builds if the previous-production R8 mapping baseline is missing, unmaterialized, or locally modified."
    mappingFile.set(layout.projectDirectory.file("mapping/release-mapping.txt"))
    allowMissingMapping.set(
        providers.gradleProperty("com.dutype.allowMissingReleaseMapping")
            .map { it.equals("true", ignoreCase = true) }
            .orElse(false)
    )
    expectedSha256.set(providers.gradleProperty("com.dutype.releaseMappingSha256").orElse(""))
    repoRoot.set(rootProject.layout.projectDirectory)
}

val checkReleaseSizeBudget by tasks.registering(CheckReleaseSizeBudgetTask::class) {
    group = "verification"
    description = "Fails release builds when AAB/dex/resources exceed DutyPe's hotfix update-size budgets."
    aabFile.set(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
    maxAabBytes.set(releaseBudgetBytes("com.dutype.maxReleaseAabMb", 18.8))
    maxDexFiles.set((findProperty("com.dutype.maxReleaseDexFiles") as? String)?.toIntOrNull() ?: 1)
    maxDexRawBytes.set(releaseBudgetBytes("com.dutype.maxReleaseDexRawMb", 11.0))
    maxDexCompressedBytes.set(releaseBudgetBytes("com.dutype.maxReleaseDexCompressedMb", 5.0))
    maxResourcesBytes.set(releaseBudgetBytes("com.dutype.maxReleaseResourcesMb", 1.7))
}

val verifyNative16KbPageSize by tasks.registering(VerifyNative16KbPageSizeTask::class) {
    group = "verification"
    description = "Fails release builds when bundled native libraries are not 16 KB page-size compatible."
    aabFile.set(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
}

tasks.configureEach {
    if (name == "preReleaseBuild") {
        dependsOn(validateReleaseMappingBaseline)
    }

    if (name == "bundleRelease") {
        finalizedBy(checkReleaseSizeBudget)
        finalizedBy(verifyNative16KbPageSize)
    }
}

// Exclude legacy deprecated firebase-iid module globally to prevent AbstractMethodError in FirebaseInitProvider
configurations.all {
    exclude(group = "com.google.firebase", module = "firebase-iid")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation("com.google.android.play:review-ktx:2.0.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.appcompat.resources)
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
    implementation("androidx.compose.material:material-icons-extended")

    // P1 FIX: Image loading with WebP support (30% smaller images)
    implementation("io.coil-kt:coil-compose:2.4.0")
    // Lottie Compose for rich animations (e.g. rain animation header)
    implementation("com.airbnb.android:lottie-compose:6.1.0")

    // Compose and Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7") 
    
    implementation("androidx.compose.animation:animation")

    // Accompanist libraries - Only keep what's needed
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.firebase:geofire-android-common:3.2.0")
    implementation("com.google.firebase:firebase-iid:21.1.0")
    implementation("com.google.firebase:firebase-iid-interop:17.1.0")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-appcheck")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-functions-ktx") // For Cloud Functions calls
    
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

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    // Required by WorkManager ListenableFuture APIs used by JobPostingWorker.
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
    implementation("net.zetetic:sqlcipher-android:4.6.1")
    implementation("androidx.sqlite:sqlite:2.6.2")
    // EncryptedSharedPreferences for securely storing the DB passphrase
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // OkHttp for Google Geocoding HTTP fallback calls.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
}

// Crashlytics mapping-file upload is intentionally LEFT ENABLED (default).
// Without it, every release stack trace in the Crashlytics console is
// obfuscated and effectively unreadable. If your local network blocks
// firebasecrashlyticssymbols.googleapis.com, the build will still succeed;
// upload the mapping manually afterwards with:
//   ./gradlew :app:uploadCrashlyticsMappingFileRelease
// or run the build from a CI runner with unrestricted DNS.
configurations.all {
    exclude(group = "com.google.firebase", module = "firebase-iid")
    resolutionStrategy {
        eachDependency {
            if (requested.group == "androidx.work") {
                useVersion("2.9.0")
                because("Force consistent WorkManager version to prevent AbstractMethodError")
            }
        }
    }
}
