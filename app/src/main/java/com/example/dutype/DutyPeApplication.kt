package com.example.dutype

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dutype.app.BuildConfig
import com.example.dutype.metadata.MetadataManager
import com.example.dutype.worker.sync.JobSyncWorker
import com.example.dutype.ads.AdManager
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.example.dutype.utils.CrashReportingHelper
import com.example.dutype.utils.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DutyPeApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var metadataManager: MetadataManager
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var adManager: AdManager
    
    // Note: FeatureFlags is a data class in AppMetadata, not an injectable class
    // Access via: appMetadata.featureFlags.value
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun attachBaseContext(base: Context) {
        // Apply saved language preference before super.attachBaseContext
        super.attachBaseContext(LocaleHelper.setLocale(base))
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber first for logging
        initializeTimber()
        
        // Initialize Firebase (required for auth/firestore)
        Firebase.initialize(this)
        
        // Initialize Firebase App Check (handles errors gracefully)
        initializeAppCheck()
        
        // Defer ALL heavy initialization to background for instant app launch
        applicationScope.launch {
            // CRITICAL OPTIMIZATION: Defer AdMob initialization by 5 seconds
            // This prevents WebView and Camera service from loading on startup
            // AdMob will be ready by the time user navigates to screens with ads
            kotlinx.coroutines.delay(5000) // 5 second delay for instant startup
            
            Timber.d("🚀 Starting deferred initialization (AdMob + WebView)")
            
            // AdMob initialization - must use Main dispatcher for ad loading
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    adManager.initialize(this@DutyPeApplication)
                    Timber.d("📺 AdMob SDK initialized (deferred 5s, WebView camera disabled)")
                    
                    // Preload ads after initialization (requires main thread)
                    adManager.preloadAllAds(this@DutyPeApplication)
                    Timber.d("📺 AdMob ads preloading started (background)")
                }
            } catch (e: Exception) {
                Timber.w(e, "📺 AdMob initialization failed (non-fatal)")
            }
        }
        
        // Schedule background job sync immediately (don't wait for AdMob)
        applicationScope.launch {
            try {
                scheduleBackgroundSync()
                scheduleSmartNotifications()
            } catch (e: Exception) {
                Timber.w(e, "🔄 Background sync scheduling failed (non-fatal)")
            }
        }
        
        // Other non-critical components
        applicationScope.launch {
            initializeNonCriticalComponents()
        }
    }
    
    /**
     * WorkManager configuration with Hilt support
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()
    
    /**
     * Schedule background job sync for offline-first architecture
     */
    private fun scheduleBackgroundSync() {
        try {
            JobSyncWorker.schedule(this)
            Timber.d("🔄 Background sync scheduled")
        } catch (e: Exception) {
            Timber.w(e, "🔄 Failed to schedule background sync")
        }
    }
    
    /**
     * Schedule smart notifications for background delivery
     * Runs even when app is closed - persists across device reboots
     * 
     * ENTERPRISE PATTERN: Following Swiggy, Zomato, PhonePe approach
     * - Birthday wishes (daily check)
     * - Job expiry reminders (24h before)
     * - Pending application reminders (48h+)
     * - Inactive user re-engagement (3+ days)
     */
    private fun scheduleSmartNotifications() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false) // Run even on low battery
                .setRequiresCharging(false) // Run even when not charging
                .setRequiresDeviceIdle(false) // Run even when device is active
                .build()
            
            // PRODUCTION: 3 hours interval (optimal for engagement + battery)
            val periodicWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.dutype.services.SmartNotificationWorker>(
                3, java.util.concurrent.TimeUnit.HOURS // PRODUCTION: 3 hours
            )
                .setConstraints(constraints)
                .addTag("smart_notifications")
                .setInitialDelay(5, java.util.concurrent.TimeUnit.MINUTES) // Start after 5 minutes
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    15, java.util.concurrent.TimeUnit.MINUTES
                )
                .build()
            
            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "smart_notifications_periodic",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP, // KEEP existing schedule
                periodicWorkRequest
            )
            
            Timber.i("🔔 ========================================")
            Timber.i("🔔 SMART NOTIFICATIONS: Scheduled successfully")
            Timber.i("🔔 Interval: 3 hours")
            Timber.i("🔔 Initial delay: 5 minutes")
            Timber.i("🔔 Runs in background even when app is closed")
            Timber.i("🔔 Survives device reboot")
            Timber.i("🔔 ========================================")
            
            // DEBUG: Trigger immediate test run
            if (BuildConfig.DEBUG) {
                triggerImmediateNotificationTest()
            }
        } catch (e: Exception) {
            Timber.e(e, "🔔 SMART NOTIFICATION: Failed to schedule")
        }
    }
    
    /**
     * Trigger immediate notification check for testing (DEBUG only)
     */
    private fun triggerImmediateNotificationTest() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .build()
            
            val immediateWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.dutype.services.SmartNotificationWorker>()
                .setConstraints(constraints)
                .addTag("smart_notifications_test")
                .setInitialDelay(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            androidx.work.WorkManager.getInstance(this).enqueue(immediateWorkRequest)
            
            Timber.i("🔔 SMART NOTIFICATION: ⚡ Immediate test triggered (DEBUG mode, runs in 10s)")
        } catch (e: Exception) {
            Timber.e(e, "🔔 SMART NOTIFICATION: Failed to trigger immediate test")
        }
    }
    
    /**
     * Initialize Timber logging with filtered tree to reduce noise
     */
    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            // Custom tree that filters out noisy Firebase/GMS logs
            Timber.plant(object : Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Filter out noisy Google Play Services and Firebase internal logs
                    val noisyTags = listOf(
                        "GoogleApiManager",
                        "FlagRegistrar", 
                        "ProviderInstaller",
                        "DynamiteModule",
                        "nativeloader",
                        "ApplicationLoaders",
                        "FilePhenotypeFlags",
                        "LocalRequestInterceptor",
                        "NativeCrypto",
                        "InsetsController",
                        // Firebase internal
                        "FirebearSt",
                        "FirebearStorage",
                        // Camera/CameraX noise (triggered by WebView)
                        "CameraManagerGlobal",
                        "StreamUseCaseUtil",
                        "UseCaseAttachState",
                        "SyncCaptureSessionBase",
                        "CaptureSession",
                        "Camera2Cap",
                        "SyncCaptureSessionImpl",
                        "DeferrableSurface",
                        "VivoJavaJsonManager",
                        "VivoCameraUtils",
                        "Camera2CameraImpl",
                        "CameraStateRegistry",
                        "CameraStateMachine",
                        "Camera2CameraControlImp",
                        "VideoUsageControl",
                        "StreamStateObserver",
                        "Camera2PresenceSrc",
                        "CameraManagerGlobal",
                        "BufferQueueProducer",
                        "BufferQueueConsumer",
                        "BLASTBufferQueue",
                        "SurfaceViewImpl",
                        "ImageReader_JNI",
                        "DMABUFHEAPS",
                        // System noise
                        "CompatChangeReporter",
                        "GraphicsEnvironment",
                        "VivoJsonResourceManager",
                        "PowerHalWrapper"
                    )
                    
                    // Skip noisy tags completely (including errors - they're GMS internal)
                    if (tag in noisyTags) {
                        return
                    }
                    
                    // Skip DEVELOPER_ERROR spam (it's a GMS config issue, not your app)
                    if (message.contains("DEVELOPER_ERROR") || 
                        message.contains("statusCode=DEVELOPER_ERROR") ||
                        message.contains("Unknown calling package name 'com.google.android.gms'") ||
                        message.contains("Phenotype.API is not available") ||
                        message.contains("Failed to get service from broker") ||
                        message.contains("Failed to register com.google.android.gms") ||
                        message.contains("cannot use FILE backing without declarative registration") ||
                        message.contains("hiddenapi:") ||
                        message.contains("ClassLoaderContext") ||
                        message.contains("BBinder_init") ||
                        message.contains("Connecting to camera service") || // Camera triggered by WebView
                        message.contains("Loading com.google.android.webview")) { // WebView loading
                        return
                    }
                    
                    // Skip throwables that are GMS internal errors
                    if (t != null && t.message?.contains("com.google.android.gms") == true) {
                        return
                    }
                    
                    super.log(priority, tag, message, t)
                }
            })
            Timber.d("🔧 Debug logging enabled (filtered)")
        }
    }
    
    /**
     * Initialize Firebase App Check for phone authentication
     * Handles errors gracefully - OTP still works without App Check in most cases
     */
    private fun initializeAppCheck() {
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            
            if (BuildConfig.DEBUG) {
                // Use debug provider for development/testing
                try {
                    val debugProviderClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    val getInstance = debugProviderClass.getMethod("getInstance")
                    val debugProvider = getInstance.invoke(null)
                    firebaseAppCheck.installAppCheckProviderFactory(debugProvider as com.google.firebase.appcheck.AppCheckProviderFactory)
                    Timber.d("✅ Firebase App Check initialized (DEBUG mode)")
                } catch (e: Exception) {
                    // Debug provider not available - this is fine for local testing
                    // OTP will still work, just without App Check protection
                    Timber.d("ℹ️ App Check debug provider not available - OTP will work without it")
                }
            } else {
                // Use Play Integrity for production builds
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Timber.d("✅ Firebase App Check initialized (Play Integrity)")
            }
        } catch (e: Exception) {
            // App Check initialization failed - this is non-fatal
            // OTP authentication will still work in most cases
            Timber.w("ℹ️ App Check init skipped: ${e.message}")
        }
    }
    
    /**
     * Initialize non-critical components in background
     */
    private fun initializeNonCriticalComponents() {
        CrashReportingHelper.initialize(this)
        initializeCrashlytics()
        logMapsApiStatus()
        initializeMetadata()
    }
    
    /**
     * Initialize metadata system for app-wide stats and feature flags
     */
    private fun initializeMetadata() {
        applicationScope.launch {
            try {
                metadataManager.initialize(this@DutyPeApplication)
                Timber.d("📊 Metadata system initialized")
            } catch (e: Exception) {
                Timber.e(e, "📊 Failed to initialize metadata system")
            }
        }
    }
    
    private fun logMapsApiStatus() {
        val mapsKey = try {
            BuildConfig::class.java.getField("MAPS_API_KEY").get(null) as? String ?: ""
        } catch (e: Exception) { "" }
        
        if (mapsKey.isNotBlank() && mapsKey != "YOUR_GOOGLE_MAPS_API_KEY_HERE") {
            Timber.d("✅ Google Maps API configured")
        }
    }
    
    private fun initializeCrashlytics() {
        try {
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        } catch (e: Exception) {
            // Non-fatal - app works without Crashlytics
        }
    }
}
