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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class DutyPeApplication : Application() {
    
    // Application-scoped coroutine scope for background initialization
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        
        // Critical path - Initialize Firebase first (required for auth)
        Firebase.initialize(this)
        
        // Initialize Timber early for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("🔧 Debug logging enabled")
        }
        
        // Defer non-critical initialization to background
        applicationScope.launch {
            initializeNonCriticalComponents()
        }
    }
    
    /**
     * Initialize non-critical components in background to improve startup time
     */
    private fun initializeNonCriticalComponents() {
        // Initialize Crash Reporting for Play Console
        CrashReportingHelper.initialize(this)
        
        // Initialize Firebase Crashlytics
        initializeCrashlytics()
        
        // Initialize Places SDK (lazy - only when needed)
        initializePlacesSdk()
    }
    
    private fun initializePlacesSdk() {
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
     */
    private fun initializeCrashlytics() {
        try {
            // Enable Crashlytics in release builds only
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            Timber.d("✅ Firebase Crashlytics initialized")
        } catch (e: Exception) {
            Timber.e("⚠️ Failed to initialize Crashlytics: ${e.message}")
        }
    }
}
