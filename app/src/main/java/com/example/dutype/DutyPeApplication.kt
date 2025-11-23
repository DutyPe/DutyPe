package com.example.dutype

import android.app.Application
import com.dutype.app.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.example.dutype.utils.CrashReportingHelper

@HiltAndroidApp
class DutyPeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        Firebase.initialize(this)
        
        // Initialize Crash Reporting for Play Console (must be before Crashlytics)
        CrashReportingHelper.initialize(this)
        
        // Initialize Firebase Crashlytics for production crash reporting
        initializeCrashlytics()
        
        // Initialize Firebase App Check with proper provider selection
        initializeFirebaseAppCheck()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("🔧 Debug logging enabled")
        }

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
            Timber.d("✅ Places SDK initialized")
        }
    }
    
    /**
     * Initialize Firebase Crashlytics for production crash and ANR reporting
     * 
     * ✅ Features:
     * - Captures uncaught exceptions automatically
     * - Captures ANR (Application Not Responding) issues
     * - Reports to Play Console Crashes & ANRs section
     * - No performance impact (event-driven, not continuous)
     * - No APK size increase (part of Firebase BOM)
     * - Automatic stack trace symbolication from Play Console debug symbols
     * 
     * 📊 In Play Console: Analytics → Crashes & ANRs
     */
    private fun initializeCrashlytics() {
        try {
            // Enable Crashlytics in release builds only (debug builds don't report)
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            
            // Set custom user ID for tracking who reported the crash (optional)
            // Firebase.crashlytics.setUserId(FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous")
            
            Timber.d("✅ Firebase Crashlytics initialized (Production crash reporting active)")
        } catch (e: Exception) {
            Timber.e("⚠️ Failed to initialize Crashlytics: ${e.message}")
        }
    }
    
    /**
     * Initialize Firebase App Check with proper fallback handling
     * 
     * ⚠️ CRITICAL FOR OTP: App must be recognized by Play Store for Play Integrity to work
     * If OTP fails on Play Store but works locally:
     * 1. Wait 24-48 hours for Play Store recognition
     * 2. Or temporarily enable Debug AppCheck in Firebase Console for testing
     * 3. Or use reCAPTCHA Enterprise as alternative verification method
     */
    private fun initializeFirebaseAppCheck() {
        try {
            if (BuildConfig.DEBUG) {
                // Debug builds: Use DebugAppCheckProviderFactory (no Play Store check needed)
                Firebase.appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Timber.d("🔍 Firebase AppCheck initialized with DEBUG provider (testing mode)")
            } else {
                // Release builds: Try Play Integrity, falls back gracefully if app not recognized yet
                try {
                    Firebase.appCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                    Timber.d("✅ Firebase AppCheck initialized with Play Integrity provider")
                } catch (e: Exception) {
                    Timber.w("⚠️ Play Integrity failed (app may not be recognized by Play Store yet): ${e.message}")
                    Timber.i("💡 OTP will work once Play Store recognizes your app (24-48 hours after upload)")
                    // Continue anyway - Firebase will retry Play Integrity automatically
                }
            }
        } catch (e: Exception) {
            Timber.e("❌ Failed to initialize Firebase AppCheck: ${e.message}")
            // App continues to work, Firebase handles missing AppCheck gracefully
        }
    }
}
