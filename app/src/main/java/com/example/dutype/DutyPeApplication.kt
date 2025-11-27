package com.example.dutype

import android.app.Application
import com.dutype.app.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.example.dutype.utils.CrashReportingHelper
import com.example.dutype.utils.FirebaseAppCheckManager

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
        
        // Initialize Firebase App Check with duplicate key crash prevention
        // Uses FirebaseAppCheckManager to ensure single-threaded initialization
        FirebaseAppCheckManager.initialize(this, isDebug = BuildConfig.DEBUG)
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("🔧 Debug logging enabled")
        }

        // Initialize Places SDK
        if (!Places.isInitialized()) {
            val apiKey = BuildConfig.MAPS_API_KEY
            if (apiKey.isNotBlank()) {
                Places.initialize(applicationContext, apiKey)
                Timber.d("✅ Places SDK initialized")
            } else {
                Timber.e("❌ Places SDK initialization skipped: MAPS_API_KEY is missing or empty.")
            }
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
}
