package com.example.dutype

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dutype.app.BuildConfig
import com.example.dutype.metadata.MetadataManager
import com.example.dutype.worker.sync.JobSyncWorker
import com.example.dutype.ads.AdManager
import com.example.dutype.performance.ANRWatchdog
import com.example.dutype.performance.StrictModeManager
import com.example.dutype.services.NotificationChannelManager
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
    
    @Inject
    lateinit var anrWatchdog: ANRWatchdog
    
    @Inject
    lateinit var anrHandler: com.example.dutype.performance.ANRHandler
    
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
        
        // PERFORMANCE: Defer notification channels to background
        applicationScope.launch(Dispatchers.IO) {
            NotificationChannelManager.createNotificationChannels(this@DutyPeApplication)
        }
        
        // DISABLED: StrictMode (causes excessive noise from Google Play Services)
        // Google's own libraries (Firebase, GMS, OkHttp, Conscrypt) trigger violations
        // These are not actionable for app developers
        // if (BuildConfig.DEBUG) {
        //     StrictModeManager.enableForDevelopment()
        // }
        
        // PERFORMANCE: Initialize Firebase asynchronously
        applicationScope.launch(Dispatchers.IO) {
            Firebase.initialize(this@DutyPeApplication)
            
            // Initialize Firebase App Check after Firebase (handles errors gracefully)
            initializeAppCheck()
        }
        
        // Initialize MainThreadChecker with ANRHandler for production-safe error handling
        com.example.dutype.performance.MainThreadChecker.init(this, anrHandler)
        
        // DISABLED: ANR Watchdog (causing debug log noise)
        // startANRMonitoring()
        
        // Defer ALL heavy initialization to background for instant app launch
        applicationScope.launch {
            // DISABLED: AdMob initialization temporarily disabled
            /*
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
            */
            Timber.d("📺 AdMob initialization DISABLED")
        }
        
        // Schedule background job sync immediately (don't wait for AdMob)
        applicationScope.launch {
            try {
                scheduleBackgroundSync()
                schedulePendingApplicationNotifications()
                // REMOVED: SmartNotificationWorker (replaced with Cloud Functions)
                // Smart notifications now run server-side via Firebase Cloud Functions
                // This eliminates permission errors and battery drain
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
     * Schedule pending application notifications worker
     * Runs every 6 hours to check for applications pending > 24 hours
     */
    private fun schedulePendingApplicationNotifications() {
        try {
            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                com.example.dutype.workers.PendingApplicationNotificationWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                androidx.work.PeriodicWorkRequestBuilder<com.example.dutype.workers.PendingApplicationNotificationWorker>(
                    6, java.util.concurrent.TimeUnit.HOURS
                ).build()
            )
            Timber.d("🔔 Pending application notifications scheduled (every 6 hours)")
        } catch (e: Exception) {
            Timber.w(e, "🔔 Failed to schedule pending application notifications")
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
                    // AGGRESSIVE FILTERING: Skip all system/library noise
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
                        "StrictMode", // Filter StrictMode warnings from GMS libraries
                        "Choreographer", // Filter frame skip warnings
                        "FA", // Firebase Analytics
                        "ViewRootImpl", // View system internals
                        "VRI[MainActivity]", // View root impl
                        "TRuntime.CTransportBackend", // Firebase logging transport
                        // Firebase internal
                        "FirebearSt",
                        "FirebearStorage",
                        "Firestore", // Filter Firestore CustomClassMapper warnings
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
                        // Video codec system noise
                        "PipelineWatcher",
                        "CCodecBufferChannel",
                        "BufferPoolAccessor",
                        "BufferPoolAccessor2.0",
                        "C2BqBufferQueueBlockPool",
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
                    
                    // Skip Firestore warnings about missing fields (these are non-critical)
                    if (tag?.contains("Firestore") == true && 
                        (message.contains("CustomClassMapper") || 
                         message.contains("No setter/field") ||
                         message.contains("isDismissible") ||
                         message.contains("isActive"))) {
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
                        message.contains("Loading com.google.android.webview") || // WebView loading
                        message.contains("StrictMode policy violation") || // StrictMode violations from GMS
                        message.contains("NonSdkApiUsedViolation") || // Non-SDK API usage from GMS
                        message.contains("UntaggedSocketViolation") || // Untagged sockets from Firebase/OkHttp
                        message.contains("android.os.strictmode") || // All StrictMode violations
                        message.contains("Skipped") && message.contains("frames") || // Frame skip warnings
                        message.contains("CustomClassMapper") || // Firestore mapping warnings
                        message.contains("No setter/field") || // Firestore field warnings
                        message.contains("Read error: ssl=") || // SSL read errors from Firebase
                        message.contains("SSL shutdown failed") || // SSL shutdown errors
                        message.contains("onWorkDone: frameIndex not found") || // Video codec noise
                        message.contains("receive c2 sleep hint") || // Video codec noise
                        message.contains("ignore dup c2 sleep hint") || // Video codec noise
                        message.contains("bufferpool2") || // Buffer pool noise
                        message.contains("evictor expired") || // Buffer pool eviction
                        message.contains("destructor()") || // Buffer queue cleanup
                        message.contains("disconnect") && message.contains("BLAST Consumer") || // Buffer queue disconnect
                        message.contains("visibilityChanged") || // View visibility changes
                        message.contains("AppSizeAfterRelayout") || // View size changes
                        message.contains("Application backgrounded") || // Firebase Analytics
                        message.contains("Making request to: https://firebaselogging") || // Firebase logging requests
                        message.contains("firebaselogging-pa.googleapis.com")) { // Firebase logging
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
     * Start ANR monitoring
     */
    private fun startANRMonitoring() {
        try {
            // Configure thresholds
            anrWatchdog.configure(
                warningThresholdMs = 5000L,  // 5 seconds warning
                criticalThresholdMs = 10000L, // 10 seconds critical
                checkIntervalMs = 2000L       // Check every 2 seconds
            )
            
            // Start monitoring
            anrWatchdog.start(object : com.example.dutype.performance.ANRListener {
                override fun onANRWarning(blockTimeMs: Long, stackTrace: Array<StackTraceElement>) {
                    Timber.w("⚠️ ANR WARNING: Main thread blocked for ${blockTimeMs}ms")
                    
                    // Log to Crashlytics
                    Firebase.crashlytics.log("ANR_WARNING: ${blockTimeMs}ms")
                    Firebase.crashlytics.setCustomKey("last_anr_warning_ms", blockTimeMs)
                }
                
                override fun onANRDetected(
                    blockTimeMs: Long,
                    mainThreadStackTrace: Array<StackTraceElement>,
                    allThreadStackTraces: Map<String, Array<StackTraceElement>>
                ) {
                    Timber.e("🔴 CRITICAL ANR: Main thread blocked for ${blockTimeMs}ms")
                    
                    // Log to Crashlytics with full context
                    Firebase.crashlytics.log("CRITICAL_ANR: ${blockTimeMs}ms")
                    Firebase.crashlytics.setCustomKey("anr_duration_ms", blockTimeMs)
                    Firebase.crashlytics.setCustomKey("anr_thread_count", allThreadStackTraces.size)
                }
            })
            
            Timber.i("🔴 ANR Watchdog started successfully")
        } catch (e: Exception) {
            Timber.e(e, "🔴 Failed to start ANR Watchdog")
        }
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
