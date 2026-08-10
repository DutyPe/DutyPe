package com.example.dutype

import com.dutype.app.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.dutype.navigation.MainNavGraph
import com.example.dutype.services.FCMTokenManager
import com.example.dutype.ui.theme.LocalDarkMode
import com.example.dutype.ui.theme.dutypeTheme
import com.example.dutype.ui.theme.ResponsiveTheme
import com.example.dutype.utils.InAppUpdateManager
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.buildVariantName
import com.example.dutype.utils.appVersionName
import com.example.dutype.utils.rememberWindowSizeClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private companion object {
        private const val ACTION_FCM_OPEN_ACTIVITY = "OPEN_ACTIVITY"
        private const val ACTION_FCM_FLUTTER_CLICK = "FLUTTER_NOTIFICATION_CLICK"
    }

    // Create NotificationPermissionManager at the activity level
    private lateinit var notificationPermissionManager: NotificationPermissionManager

    /**
     * P2-7: Cold-start trace. Started as the very first work in [onCreate]; stopped when
     * Compose reports the main nav graph fully drawn. The Firebase plugin auto-instruments
     * `_app_start` already, but that includes process bring-up before our code runs — this
     * trace captures everything from `super.onCreate` through the first usable frame, which
     * is the metric we actually optimize.
     */
    
    // PERF: Removed unused eager @Inject fields (jobApplicationService, reviewManager,
    // metadataManager). They were declared but never referenced in this Activity, yet
    // Hilt was forced to construct their entire transitive graph (Firestore, Auth,
    // DataStore, etc.) on every cold start. Inject them where they're actually used.

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager
    
    @Inject
    lateinit var updateManager: com.example.dutype.utils.InAppUpdateManager

    /**
     * P2-4: Deep-link bus replaces the prior `LocalBroadcastManager` relay
     * between this activity and [com.example.dutype.navigation.MainNavGraph].
     */
    @Inject
    lateinit var deepLinkBus: com.example.dutype.navigation.DeepLinkBus

    @Inject
    lateinit var appConfigRepository: com.example.dutype.repositories.AppConfigRepository
    
    // Activity result launcher kept for the update manager API.
    private val updateResultLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                Timber.i("✅ Update accepted by user")
            }
            RESULT_CANCELED -> {
                Timber.w("⚠️ Update canceled by user")
                // P0 FIX: Use lifecycleScope instead of leaked CoroutineScope
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    updateManager.trackUpdateDismissal()
                }
            }
            else -> {
                Timber.e("❌ Update failed with result code: ${result.resultCode}")
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Apply saved language preference
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }
    
    private fun sanitizeLaunchIntent(launchIntent: Intent?): Boolean {
        if (launchIntent == null) return false
        // Detect duplicate Activity instance launched directly from Google Play Store installer button
        if (!isTaskRoot && launchIntent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == launchIntent.action) {
            Timber.w("MainActivity - Duplicate activity instance launched from Play Store installer; terminating duplicate task.")
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (sanitizeLaunchIntent(intent)) {
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        // P2-7: start cold-start trace before any other work in onCreate.
        // MODERN SPLASH SCREEN API (Android 12+)
        // CRITICAL: Must be called BEFORE super.onCreate()
        //
        // Best-practice pattern (Google developer docs, 2024-2026):
        // 1. Install the splash screen (theme-driven, so the system draws
        //    it on process bring-up — no Compose work involved).
        // 2. Gate dismissal on a cheap main-thread boolean read.
        // 3. Flip the boolean from a `ViewTreeObserver.OnPreDrawListener`
        //    attached to the content view — this guarantees the splash
        //    drops on the exact frame the NavHost is ready to draw,
        //    preventing the "blank flash" that happens when you flip the
        //    flag from a `LaunchedEffect` (which runs one frame late).
        // 4. A hard-cap `postDelayed` is kept as a safety net so a
        //    catastrophic Compose failure still releases the splash.
        val splashScreen = installSplashScreen()

        var keepSplashOnScreen = true
        var startupOverlayCommitted = false
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen && !startupOverlayCommitted }

        // Fast-path: Attach OnPreDrawListener to content view to dismiss splash
        // on the EXACT millisecond the first frame of Compose is ready to draw (~50-100ms).
        val contentView = findViewById<android.view.View>(android.R.id.content)
        contentView?.viewTreeObserver?.addOnPreDrawListener(
            object : android.view.ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    contentView.viewTreeObserver.removeOnPreDrawListener(this)
                    keepSplashOnScreen = false
                    startupOverlayCommitted = true
                    return true
                }
            }
        )

        // Ultra-short safety fallback (150ms cap instead of 500ms)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            keepSplashOnScreen = false
            startupOverlayCommitted = true
        }, 150L)
        
        super.onCreate(savedInstanceState)
        val launchIntent = normalizeNotificationLaunchIntent(intent)
        launchIntent?.let(::setIntent)
        cancelTappedSystemNotification(launchIntent)
        
        // Initialize Timber for logging (if not already initialized in Application class)
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("✅ MainActivity.onCreate() - Activity created")
        Timber.d("Package: ${packageName}")
        Timber.d("App version: ${appVersionName()}")
        Timber.d("Build variant: ${buildVariantName()}")
        logNotificationTapTelemetry(source = "on_create", sourceIntent = launchIntent)
        
        // Log notification intent if present
        if (launchIntent?.getBooleanExtra("from_notification", false) == true) {
            Timber.i("📱 App opened from notification")
            Timber.d("Notification type: ${launchIntent.getStringExtra("notification_type")}")
            Timber.d("Deep link: ${launchIntent.data}")
        }

        // Create NotificationPermissionManager before setContent
        notificationPermissionManager = NotificationPermissionManager(this)
        Timber.d("✅ NotificationPermissionManager initialized")

        // Dynamic Launcher Icon Updater (2026 Enterprise Feature)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appConfigRepository.dynamicFeaturesConfig.collect { config ->
                    com.example.dutype.utils.DynamicIconManager.queueIconSwitch(this@MainActivity, config.activeLauncherIcon)
                }
            }
        }

        // Guest engagement notifications.
        // PERF: Run off the main thread — FirebaseAuth.currentUser triggers a token
        // store disk read, FirebaseMessaging.getInstance() does first-call I/O, and
        // subscribeToTopic queues to disk-backed Pending Topic Operations prefs.
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                if (FirebaseAuth.getInstance().currentUser == null) {
                    fcmTokenManager.subscribeToTopic(FCMTokenManager.TOPIC_GUEST_USERS)
                    fcmTokenManager.subscribeToLanguageTopicPublic(FCMTokenManager.TOPIC_GUEST_USERS)
                    com.example.dutype.workers.GuestEngagementWorker.cancelAll(this@MainActivity)
                } else {
                    fcmTokenManager.unsubscribeFromTopic(FCMTokenManager.TOPIC_GUEST_USERS)
                    fcmTokenManager.unsubscribeFromLanguageTopicPublic(FCMTokenManager.TOPIC_GUEST_USERS)
                    com.example.dutype.workers.GuestEngagementWorker.scheduleRecurring(this@MainActivity)
                }
            }.onFailure { Timber.w(it, "Guest engagement topic setup failed (non-fatal)") }
        }

        // Enable edge-to-edge for Android 15+ compatibility with WHITE status bar
        // This is the recommended way for SDK 35+
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
        Timber.d("✅ Edge-to-edge enabled with white status bar (Android 15+ compatible)")

        // Keep contrast icons in sync with a light system bar style.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightNavigationBars = true // Dark icons on white background
            isAppearanceLightStatusBars = true // Dark icons on white status bar
        }
        Timber.d("✅ System bar icon appearance configured")

        setContent {
            // SYSTEM SPLASH (Android 12+): Keep the platform splash visible until
            // MainNavGraph has determined the correct start destination. This
            // removes the "blank loading" flash between splash dismissal and the
            // first real screen and matches the pattern used by Google apps,
            // LinkedIn, Instagram, Uber. No Compose splash is drawn on top.
            
            val windowSizeClass = rememberWindowSizeClass()
            

            var showMaintenanceMode by remember { mutableStateOf(false) }
            var showForceUpdate by remember { mutableStateOf(false) }
            val lifecycleOwner = LocalLifecycleOwner.current
            
            // REMOVED: Metadata checks - only load when actually needed
            // Metadata is now loaded lazily when user navigates to screens that need it
            
            // REMOVED: Blacklist check - only check when user performs sensitive actions
            // This prevents unnecessary Firestore calls on every app start
            
            // REMOVED: FCM token refresh - only refresh when user is actively using the app
            // Token refresh moved to when user performs their first action
            
            // REMOVED: In-app review - only show after user completes meaningful actions
            // Review prompt moved to after job application or job posting
            
            // REMOVED: Developer mode check - only check when user tries sensitive actions
            // This prevents unnecessary checks on every app start
            
            // REMOVED: Blacklist check on startup - only check when user performs actions
            // This saves a Firestore call on every app start
            
            // REMOVED: Re-check on resume - unnecessary overhead
            
            // REMOVED: FCM token refresh on startup - only refresh when user is active
            // Token refresh moved to when user performs their first meaningful action
            
            dutypeTheme {
                ResponsiveTheme(windowSizeClass = windowSizeClass) {
                    val navController = rememberNavController()
                    val appConfigViewModel: com.example.dutype.viewmodels.AppConfigViewModel = hiltViewModel()
                    val dynamicFeatures by appConfigViewModel.dynamicFeaturesConfig.collectAsStateWithLifecycle()

                    // P3-7: Crashlytics breadcrumb on every nav route change so crash
                    // reports include the user's recent navigation path.
                    LaunchedEffect(navController) {
                        navController.currentBackStackEntryFlow.collect { entry ->
                            val route = entry.destination.route ?: "unknown"
                            runCatching {
                                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                                    .log("nav: $route")
                            }
                        }
                    }

                    val darkTheme = LocalDarkMode.current
                    var statusBarColor by remember { mutableStateOf(Color.White) }
                    val effectiveStatusBarColor =
                        if (darkTheme && statusBarColor == Color.White) MaterialTheme.colorScheme.background else statusBarColor

                    var mainNavReady by remember { mutableStateOf(false) }
                    var launchExperienceResolved by remember { mutableStateOf(false) }
                    var showLaunchPromo by remember { mutableStateOf(false) }
                    var showStartupOverlay by remember { mutableStateOf(false) }
                    var committedStartupUsesPromo by remember { mutableStateOf<Boolean?>(null) }

                    LaunchedEffect(dynamicFeatures) {
                        if (dynamicFeatures.launchPromoEnabled && dynamicFeatures.launchPromoBannerUrl.isNotBlank()) {
                            showLaunchPromo = true
                        }
                        launchExperienceResolved = true
                    }

                    LaunchedEffect(mainNavReady) {
                        if (mainNavReady) {
                            launchExperienceResolved = true
                        }
                    }

                    LaunchedEffect(mainNavReady, launchExperienceResolved) {
                        if (mainNavReady && launchExperienceResolved && committedStartupUsesPromo == null) {
                            committedStartupUsesPromo = showLaunchPromo
                            startupOverlayCommitted = true
                            keepSplashOnScreen = false
                            showStartupOverlay = showLaunchPromo
                        }
                    }

                    // PERF (Android best practice): do NOT call
                    // `enableEdgeToEdge` on every status-bar color change.
                    // That API is meant to be invoked once per Activity
                    // (it rebinds window insets and runs decor-layout),
                    // and recalling it from a LaunchedEffect keyed on
                    // color triggers a full insets pass + relayout on
                    // every screen navigation — visible jank on older
                    // devices.
                    //
                    // Instead, push the color to the window directly and
                    // update the light/dark icon hint. This is what
                    // `SystemBarStyle.auto` does internally, minus the
                    // insets rebind.
                    val isColorDark = remember(effectiveStatusBarColor) {
                        val red = effectiveStatusBarColor.red
                        val green = effectiveStatusBarColor.green
                        val blue = effectiveStatusBarColor.blue
                        val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
                        luminance < 0.5
                    }

                    LaunchedEffect(effectiveStatusBarColor, darkTheme, isColorDark) {
                        @Suppress("DEPRECATION")
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        WindowCompat.getInsetsController(window, window.decorView).apply {
                            // If dark theme is enabled, or the custom status bar color is dark,
                            // use light-colored system status bar icons (white). Otherwise, use dark icons.
                            isAppearanceLightStatusBars = !darkTheme && !isColorDark
                            isAppearanceLightNavigationBars = !darkTheme
                        }
                    }
                    
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        // PERF: Log only once when MainNavGraph first mounts. Without this
                        // gate, every parent recomposition (e.g. statusBarColor change from
                        // navigation events) re-fires both Timber lines.
                        LaunchedEffect(Unit) {
                            Timber.d("🚀 Initializing MainNavGraph")
                            Timber.d("🔗 DEEP LINK: Startup handling delegated to MainNavGraph when NavHost is ready")
                        }

                        MainNavGraph(
                            navController = navController,
                            onStatusBarColorChange = {
                                statusBarColor = it
                            },
                            onReady = {
                                // Dismiss the system splash once MainNavGraph has resolved
                                // the start destination and is ready to render content.
                                mainNavReady = true
                            },
                            notificationData = launchIntent?.extras?.getString("notificationId"),
                            notificationPermissionManager = notificationPermissionManager,
                            notificationIntent = launchIntent
                        )

                        // Startup experience: a backend-controlled full-screen promo banner
                        // can replace the branded splash during special events/deals.
                        if (showStartupOverlay && committedStartupUsesPromo == true) {
                            com.example.dutype.components.LaunchPromoScreen(
                                mediaType = dynamicFeatures.launchPromoMediaType,
                                bannerUrl = dynamicFeatures.launchPromoBannerUrl,
                                animationUrl = dynamicFeatures.launchPromoAnimationUrl,
                                onAnimationEnd = { showStartupOverlay = false }
                            )
                        }

                        // Maintenance Mode Sheet
                        if (showMaintenanceMode) {
                            MaintenanceModeSheet()
                        }
                        
                        // Force Update Sheet
                        if (showForceUpdate) {
                            ForceUpdateSheet(
                                onUpdateClick = {
                                    // Open Play Store
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("market://details?id=${packageName}")
                                        }
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback to browser
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")
                                        }
                                        startActivity(intent)
                                    }
                                }
                            )
                        }
                    }

                    // Report fully drawn when the main navigation graph is composed.
                    // This is a good signal that your app's main UI is ready.
                    LaunchedEffect(Unit) {
                        reportFullyDrawn()
                        // P2-7: stop the cold-start Perf trace at first usable frame.
                        Timber.d("✅ MainActivity - Report fully drawn")
                    }
                }
            }
        }
    }
    
    /**
     * Handle new intents when app is already running (e.g., notification clicks, shared links)
     * This is CRITICAL for deep links to work when app is in background or already open
     * 
     * INDUSTRY STANDARD: This is how LinkedIn, Instagram, Uber handle deep links
     */
    override fun onNewIntent(newIntent: Intent) {
        val normalizedIntent = normalizeNotificationLaunchIntent(newIntent) ?: newIntent
        super.onNewIntent(normalizedIntent)
        setIntent(normalizedIntent) // CRITICAL: Update the activity's intent
        cancelTappedSystemNotification(normalizedIntent)
        logNotificationTapTelemetry(source = "on_new_intent", sourceIntent = normalizedIntent)

        Timber.i("🔗 DEEP LINK: MainActivity.onNewIntent() - New intent received")
        Timber.d("🔗 DEEP LINK: Intent data = ${newIntent.data}")
        Timber.d("🔗 DEEP LINK: Intent action = ${newIntent.action}")

        // Log notification intent if present
        if (normalizedIntent.getBooleanExtra("from_notification", false)) {
            Timber.i("🔗 DEEP LINK: New intent from notification")
            Timber.d("🔗 DEEP LINK: Notification type: ${newIntent.getStringExtra("notification_type")}")
        }

        // Dispatch deep links to the active navigation graph without recreating the activity.
        // This avoids a full UI rebuild while still handling notification taps reliably.
        val deepLinkUri = normalizedIntent.data
        if (deepLinkUri != null) {
            Timber.i("🔗 DEEP LINK: ✅ Deep link detected in onNewIntent: $deepLinkUri")
            // P2-4: Emit through DeepLinkBus instead of LocalBroadcastManager.
            // MainNavGraph collects this flow inside a LaunchedEffect.
            // FIXED: Use a non-blocking emit so the UI isn't delayed
            lifecycleScope.launch {
                deepLinkBus.emit(deepLinkUri)
            }
            logNotificationTapTelemetry(source = "deeplink_dispatched", sourceIntent = normalizedIntent)
        } else {
            Timber.w("🔗 DEEP LINK: ⚠️ No deep link URI found in intent")
        }
    }

    private fun normalizeNotificationLaunchIntent(sourceIntent: Intent?): Intent? {
        if (sourceIntent == null) return null

        val deepLink = sourceIntent.data?.toString().orEmpty()
            .ifBlank { sourceIntent.getStringExtra("notification_deep_link").orEmpty() }
            .ifBlank { sourceIntent.getStringExtra("deepLink").orEmpty() }
            .ifBlank { sourceIntent.getStringExtra("link").orEmpty() }

        val isNotificationLaunch =
            sourceIntent.getBooleanExtra("from_notification", false) ||
                sourceIntent.action == ACTION_FCM_OPEN_ACTIVITY ||
                sourceIntent.action == ACTION_FCM_FLUTTER_CLICK ||
                sourceIntent.extras?.containsKey("google.message_id") == true ||
                sourceIntent.extras?.containsKey("gcm.n.e") == true ||
                deepLink.isNotBlank()

        if (isNotificationLaunch) {
            sourceIntent.putExtra("from_notification", true)
        }

        if (sourceIntent.data == null && deepLink.isNotBlank()) {
            runCatching {
                sourceIntent.data = Uri.parse(deepLink)
                sourceIntent.putExtra("notification_deep_link", deepLink)
            }.onFailure { error ->
                Timber.w(error, "Failed to normalize notification deep link: $deepLink")
            }
        }

        return sourceIntent
    }

    private fun cancelTappedSystemNotification(sourceIntent: Intent?) {
        val notificationId = sourceIntent?.getIntExtra("notification_system_id", Int.MIN_VALUE) ?: Int.MIN_VALUE
        if (notificationId == Int.MIN_VALUE) return
        runCatching {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
        }.onFailure { error ->
            Timber.w(error, "Failed to cancel tapped notification")
        }
    }

    /**
     * FIXED: Now covers all notification-related intents including:
     * - from_notification extra
     * - notification_deep_link extra
     * - deepLink extra
     * - link extra
     * - Any intent with a data URI or notification action
     */
    private fun logNotificationTapTelemetry(source: String, sourceIntent: Intent?) {
        val intent = sourceIntent ?: return

        val isNotificationLaunch = intent.getBooleanExtra("from_notification", false) ||
                !intent.getStringExtra("notification_deep_link").isNullOrBlank() ||
                intent.data != null ||
                !intent.getStringExtra("deepLink").isNullOrBlank() ||
                !intent.getStringExtra("link").isNullOrBlank()

        if (!isNotificationLaunch) return

        val deepLink = intent.data?.toString().orEmpty()
            .ifBlank { intent.getStringExtra("notification_deep_link").orEmpty() }
            .ifBlank { intent.getStringExtra("deepLink").orEmpty() }
            .ifBlank { intent.getStringExtra("link").orEmpty() }

        val notificationType = intent.getStringExtra("notification_type").orEmpty()
        val systemId = intent.getIntExtra("notification_system_id", Int.MIN_VALUE)

        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log("notification_tap:$source")
            crashlytics.setCustomKey("notification_tap_source", source)
            crashlytics.setCustomKey("notification_tap_type", notificationType.ifBlank { "unknown" })
            crashlytics.setCustomKey("notification_tap_deeplink", deepLink.ifBlank { "none" })
            crashlytics.setCustomKey("notification_tap_system_id", if (systemId == Int.MIN_VALUE) -1 else systemId)
        }.onFailure { error ->
            Timber.w(error, "Failed to log notification tap telemetry")
        }
    }
    
    override fun onStart() {
        super.onStart()
        Timber.d("📱 MainActivity.onStart()")
    }
    
    override fun onResume() {
        super.onResume()
        Timber.d("📱 MainActivity.onResume()")

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                if (FirebaseAuth.getInstance().currentUser != null) {
                    fcmTokenManager.registerToken()
                }
            }.onFailure { error ->
                Timber.w(error, "FCM token refresh on app open failed")
            }
        }
        
        // Check for in-app updates. Native Play Core prompts are disabled in
        // release builds so small hotfixes do not carry the Play update SDK dex.
        lifecycleScope.launch {
            updateManager.checkForUpdate(
                activity = this@MainActivity,
                activityResultLauncher = updateResultLauncher,
                onUpdateAvailable = { appUpdateInfo, updateType ->
                    Timber.i("🔄 Update available - type: ${if (updateType == InAppUpdateManager.UPDATE_TYPE_IMMEDIATE) "IMMEDIATE" else "FLEXIBLE"}")
                    
                    // Register listener for flexible updates to auto-complete when downloaded
                    if (updateType == InAppUpdateManager.UPDATE_TYPE_FLEXIBLE) {
                        updateManager.registerFlexibleUpdateListener(
                            onDownloaded = {
                                Timber.i("✅ Flexible update downloaded - completing update")
                                updateManager.completeFlexibleUpdate()
                            },
                            onFailed = { errorCode ->
                                Timber.e("❌ Flexible update failed: $errorCode")
                            }
                        )
                    }
                },
                onNoUpdate = {
                    Timber.d("✅ App is up to date")
                },
                onError = { exception ->
                    Timber.w("⚠️ Update check failed (non-fatal): ${exception.message}")
                }
            )
            
            // Also check for pending flexible updates
            updateManager.checkForPendingUpdate {
                Timber.i("⏳ Pending update found - prompting user to install")
                // Show snackbar or dialog to complete update
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        Timber.d("📱 MainActivity.onPause()")
        // Schedule background re-engagement check (worker itself skips guests).
        com.example.dutype.workers.GuestEngagementWorker.scheduleBackground(this)
    }

    override fun onStop() {
        super.onStop()
        Timber.d("📱 MainActivity.onStop() - App in background check")
        if (!com.example.dutype.utils.AppLifecycleTracker.isAppInForeground()) {
            val isUserLoggedIn = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
            if (isUserLoggedIn) {
                Timber.d("📱 MainActivity.onStop() - App is ACTUALLY in background and user is logged in, applying launcher icon switch")
                com.example.dutype.utils.DynamicIconManager.applyPendingIconSwitch(this)
            } else {
                Timber.d("📱 MainActivity.onStop() - App is in background but user is NOT logged in (auth flow safety), skipping icon switch")
            }
        } else {
            Timber.d("📱 MainActivity.onStop() - App still in foreground (active activity present), skipping icon switch")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Timber.d("📱 MainActivity.onDestroy()")
    }
}

/**
 * Maintenance Mode Sheet - Shows when app is under maintenance
 */
@Composable
private fun MaintenanceModeSheet() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.WorkerColors.CardBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🔧",
                fontSize = 64.sp
            )
            Text(
                text = stringResource(R.string.auto_under_maintenance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.auto_we_re_making_dutype_even_better_for_you_pl),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Force Update Sheet - Shows when app needs mandatory update
 */
@Composable
private fun ForceUpdateSheet(onUpdateClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.WorkerColors.CardBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⬆️",
                fontSize = 64.sp
            )
            Text(
                text = stringResource(R.string.auto_update_required),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.auto_a_new_version_of_dutype_is_available_with),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUpdateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F2937)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(R.string.auto_update_now),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
