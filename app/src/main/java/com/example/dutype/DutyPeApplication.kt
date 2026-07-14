package com.example.dutype

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.dutype.metadata.MetadataManager
import com.example.dutype.worker.sync.JobSyncWorker
import com.example.dutype.services.NotificationChannelManager
import com.example.dutype.utils.googleMapsApiKey
import com.example.dutype.utils.isDebuggableBuild
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
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

    companion object {
        // Keep StrictMode opt-in to avoid vendor ROM noise drowning real app errors in Logcat.
        private const val ENABLE_STRICT_MODE_IN_DEBUG = false

        // PERF: Hoisted into a static Set so the Timber filter doesn't allocate a
        // ~70-element ArrayList + perform O(n) lookup on every single log call.
        // During startup Firebase/GMS log heavily; this filter is a hot path.
        private val NOISY_LOG_TAGS: Set<String> = setOf(
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
            "StrictMode",
            "Choreographer",
            "FA",
            "ViewRootImpl",
            "VRI[MainActivity]",
            "TRuntime.CTransportBackend",
            "FirebearSt",
            "FirebearStorage",
            "Firestore",
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
            "BufferQueueProducer",
            "BufferQueueConsumer",
            "BLASTBufferQueue",
            "SurfaceViewImpl",
            "ImageReader_JNI",
            "DMABUFHEAPS",
            "PipelineWatcher",
            "CCodecBufferChannel",
            "BufferPoolAccessor",
            "BufferPoolAccessor2.0",
            "C2BqBufferQueueBlockPool",
            "CompatChangeReporter",
            "GraphicsEnvironment",
            "VivoJsonResourceManager",
            "PowerHalWrapper"
        )
    }
    
    @Inject
    lateinit var metadataManager: MetadataManager
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var anrHandler: com.example.dutype.performance.ANRHandler
    // Note: FeatureFlags is a data class in AppMetadata, not an injectable class
    // Access via: appMetadata.featureFlags.value
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var appCheckDebugHintLogged = false
    
    override fun attachBaseContext(base: Context) {
        // Apply saved language preference before super.attachBaseContext
        super.attachBaseContext(LocaleHelper.setLocale(base))
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber first for logging
        initializeTimber()

        // StrictMode is opt-in in debug builds because some vendor ROM hooks generate
        // high-volume violations unrelated to app logic, which can hide actionable errors.
        if (isDebuggableBuild() && ENABLE_STRICT_MODE_IN_DEBUG) {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build()
            )
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
        }
        
        // CRITICAL P0 FIX: Firebase MUST be initialized synchronously BEFORE Hilt injects
        // Firebase-dependent singletons (FirebaseFirestore, FirebaseAuth, etc.)
        // Previously this was async causing race conditions with Hilt DI
        Firebase.initialize(this@DutyPeApplication)

        // CRITICAL: App Check must be installed BEFORE any Firestore/Auth/Functions calls.
        // Async initialization can race with early app reads and cause PERMISSION_DENIED.
        initializeAppCheck()

        // Diagnostic log: deferred off the startup critical path.
        applicationScope.launch { logFirebaseBinding() }
        
        // PERFORMANCE: Defer notification channels to background
        applicationScope.launch(Dispatchers.IO) {
            NotificationChannelManager.createNotificationChannels(this@DutyPeApplication)
        }
        
        // Initialize MainThreadChecker with ANRHandler for production-safe error handling
        com.example.dutype.performance.MainThreadChecker.init(this, anrHandler)
        
        // Schedule background job sync immediately
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
            .setMinimumLoggingLevel(if (isDebuggableBuild()) android.util.Log.DEBUG else android.util.Log.INFO)
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
        if (isDebuggableBuild()) {
            // Custom tree that filters out noisy Firebase/GMS logs
            Timber.plant(object : Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Skip noisy tags completely (including errors - they're GMS internal)
                    if (tag in NOISY_LOG_TAGS) {
                        return
                    }
                    
                    // Skip Firestore warnings about missing fields (these are non-critical)
                    if (tag?.contains("Firestore") == true && 
                        (message.contains("CustomClassMapper") || 
                         message.contains("No setter/field") ||
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
            
            if (isDebuggableBuild()) {
                // Use debug provider for development/testing
                try {
                    val debugProviderClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    val getInstance = debugProviderClass.getMethod("getInstance")
                    val debugProvider = getInstance.invoke(null)
                    firebaseAppCheck.installAppCheckProviderFactory(debugProvider as com.google.firebase.appcheck.AppCheckProviderFactory)
                    Timber.i("✅ Firebase App Check initialized (DEBUG provider)")
                    logAppCheckDebugSecretIfPresent()
                    firebaseAppCheck.getAppCheckToken(false)
                        .addOnSuccessListener {
                            Timber.i("✅ App Check token acquired (debug) - expiresAt=%d", it.expireTimeMillis)
                            logAppCheckDebugSecretIfPresent()
                        }
                        .addOnFailureListener { tokenError ->
                            val errorMessage = tokenError.message.orEmpty()
                            val isDebugRegistrationIssue =
                                errorMessage.contains("App attestation failed", ignoreCase = true) ||
                                errorMessage.contains("code: 403", ignoreCase = true)

                            if (isDebugRegistrationIssue) {
                                if (!appCheckDebugHintLogged) {
                                    appCheckDebugHintLogged = true
                                    val projectId = runCatching {
                                        FirebaseApp.getInstance().options.projectId
                                    }.getOrDefault("unknown-project")
                                    Timber.w(
                                        "⚠️ DEBUG App Check rejected (project=%s). Add the debug secret from logcat to Firebase Console > Build > App Check > Android app > Manage debug tokens.",
                                        projectId
                                    )
                                }
                                logAppCheckDebugSecretIfPresent()
                            } else {
                                Timber.e(tokenError, "❌ App Check token fetch failed in DEBUG")
                                logAppCheckDebugSecretIfPresent()
                            }
                        }
                } catch (e: Exception) {
                    // Fallback to Play Integrity if debug provider class is unavailable.
                    // This may fail on sideloaded/debug builds, but gives a deterministic state.
                    firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                    Timber.e(e, "❌ Debug App Check provider unavailable; fell back to Play Integrity")
                }
            } else {
                // Use Play Integrity for production builds
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Timber.i("✅ Firebase App Check initialized (Play Integrity)")
                firebaseAppCheck.getAppCheckToken(false)
                    .addOnSuccessListener {
                        Timber.i("✅ App Check token acquired (release) - expiresAt=%d", it.expireTimeMillis)
                    }
                    .addOnFailureListener { tokenError ->
                        Timber.e(tokenError, "❌ App Check token fetch failed in release")
                    }
            }
        } catch (e: Exception) {
            // App Check initialization failed - this is non-fatal
            // OTP authentication will still work in most cases
            Timber.w("ℹ️ App Check init skipped: ${e.message}")
        }
    }

    private fun logAppCheckDebugSecretIfPresent() {
        if (!isDebuggableBuild()) return

        runCatching {
            val storeName = "com.google.firebase.appcheck.debug.store"
            val keyName = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
            val prefs = getSharedPreferences(storeName, MODE_PRIVATE)
            val uuidPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            val secret = prefs.getString(keyName, null)
                ?: prefs.all.values
                    .filterIsInstance<String>()
                    .firstOrNull { candidate -> uuidPattern.matches(candidate) }

            if (!secret.isNullOrBlank()) {
                Timber.w("🔐 Firebase App Check debug secret (add in console): %s", secret)
            } else {
                Timber.i("ℹ️ App Check debug secret not generated yet. Check Logcat tag DebugAppCheckProvider after token request.")
            }
        }.onFailure { e ->
            Timber.w(e, "⚠️ Unable to read App Check debug secret from local store")
        }
    }

    private fun logFirebaseBinding() {
        try {
            val app = FirebaseApp.getInstance()
            val options = app.options
            Timber.i(
                "🔥 Firebase binding: projectId=%s appId=%s senderId=%s apiKey=%s",
                options.projectId,
                options.applicationId,
                options.gcmSenderId,
                options.apiKey.take(10) + "..."
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ Unable to log Firebase runtime binding")
        }
    }
    
    /**
     * Initialize non-critical components in background
     */
    private fun initializeNonCriticalComponents() {
        CrashReportingHelper.initialize(this)
        initializeCrashlytics()
        initializeGoogleMapsServices()
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
    
    private fun initializeGoogleMapsServices() {
        val mapsKey = googleMapsApiKey()

        if (!mapsKey.isNullOrBlank()) {
            Timber.d("Google Maps web APIs configured")
        }
    }
    
    private fun initializeCrashlytics() {
        try {
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(!isDebuggableBuild())
        } catch (e: Exception) {
            // Non-fatal - app works without Crashlytics
        }
    }
}
