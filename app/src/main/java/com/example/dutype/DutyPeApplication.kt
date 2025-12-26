package com.example.dutype

import android.app.Application
import com.dutype.app.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
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
        
        // Initialize Firebase App Check for phone auth (CRITICAL for OTP on Play Store)
        initializeAppCheck()
        
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
     * Initialize Firebase App Check for phone authentication
     * This is REQUIRED for OTP/SMS verification to work on Play Store builds
     */
    private fun initializeAppCheck() {
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            
            if (BuildConfig.DEBUG) {
                // Use debug provider for development/testing
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Timber.d("✅ Firebase App Check initialized (DEBUG mode)")
            } else {
                // Use Play Integrity for production builds
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Timber.d("✅ Firebase App Check initialized (Play Integrity)")
            }
        } catch (e: Exception) {
            Timber.e(e, "⚠️ Failed to initialize Firebase App Check: ${e.message}")
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
        
        // Log Azure Maps status
        logAzureMapsStatus()
    }
    
    /**
     * Log Azure Maps configuration status
     */
    private fun logAzureMapsStatus() {
        val azureKey = try {
            BuildConfig::class.java.getField("AZURE_MAPS_KEY").get(null) as? String ?: ""
        } catch (e: Exception) { "" }
        
        if (azureKey.isNotBlank()) {
            Timber.d("✅ Azure Maps configured")
        } else {
            Timber.w("⚠️ Azure Maps key not configured - using fallback geocoder")
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
