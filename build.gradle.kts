// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
    id("com.google.firebase.crashlytics") version "3.0.3" apply false
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false

    // P1-1: Baseline Profile plugin. Wires the :baselineprofile Macrobenchmark
    // module into :app so `./gradlew :app:generateBaselineProfile` produces
    // `app/src/<variant>/generated/baselineProfiles/baseline-prof.txt` which
    // is bundled into the release AAB. Generation requires a connected
    // device or a Gradle Managed Device (GMD) with Play Store image.
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.android.test) apply false

    // P3-6: Dependency Analysis plugin. Run `./gradlew buildHealth` to surface
    // unused, misused, and transitive-leaked dependencies. Off the critical
    // path of the regular build; only runs when its tasks are invoked.
    id("com.autonomousapps.dependency-analysis") version "2.5.0"
}
