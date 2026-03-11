package com.example.dutype

import android.content.Context
import android.content.Intent
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dutype.app.BuildConfig
import com.example.dutype.navigation.MainNavGraph
import com.example.dutype.services.FCMTokenManager
import com.example.dutype.services.JobApplicationService
import com.example.dutype.ui.theme.dutypeTheme
import com.example.dutype.ui.theme.ResponsiveTheme
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.rememberWindowSizeClass
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // Create NotificationPermissionManager at the activity level
    private lateinit var notificationPermissionManager: NotificationPermissionManager
    
    @Inject
    lateinit var jobApplicationService: JobApplicationService
    
    @Inject
    lateinit var fcmTokenManager: FCMTokenManager
    
    @Inject
    lateinit var reviewManager: com.example.dutype.utils.InAppReviewManager
    
    @Inject
    lateinit var updateManager: com.example.dutype.utils.InAppUpdateManager
    
    @Inject
    lateinit var metadataManager: com.example.dutype.metadata.MetadataManager
    
    // Activity result launcher for in-app updates
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // MODERN SPLASH SCREEN API (Android 12+)
        // CRITICAL: Must be called BEFORE super.onCreate()
        // This is the official Google-recommended approach (2024-2026)
        // Used by: LinkedIn, Instagram, Uber, Google apps
        val splashScreen = installSplashScreen()
        
        // Keep splash screen visible while loading (Google recommended approach)
        // This ensures smooth transition and prevents flickering
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        
        super.onCreate(savedInstanceState)
        
        // Initialize Timber for logging (if not already initialized in Application class)
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize Google Mobile Ads SDK
        // AdsManager.initializeMobileAds(this) // DISABLED FOR TESTING
        
        Timber.d("✅ MainActivity.onCreate() - Activity created")
        Timber.d("Package: ${packageName}")
        Timber.d("App version: ${BuildConfig.VERSION_NAME}")
        Timber.d("Build variant: ${BuildConfig.BUILD_TYPE}")
        
        // Log notification intent if present
        if (intent?.getBooleanExtra("from_notification", false) == true) {
            Timber.i("📱 App opened from notification")
            Timber.d("Notification type: ${intent?.getStringExtra("notification_type")}")
            Timber.d("Deep link: ${intent?.data}")
        }

        // Create NotificationPermissionManager before setContent
        notificationPermissionManager = NotificationPermissionManager(this)
        Timber.d("✅ NotificationPermissionManager initialized")

        // Enable edge-to-edge for Android 15+ compatibility with WHITE status bar
        // This is the recommended way for SDK 35+
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.WHITE,
                darkScrim = android.graphics.Color.WHITE
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.WHITE,
                darkScrim = android.graphics.Color.WHITE
            )
        )
        Timber.d("✅ Edge-to-edge enabled with white status bar (Android 15+ compatible)")
        
        // Set navigation bar to white immediately (for older Android versions)
        window.navigationBarColor = android.graphics.Color.WHITE
        window.statusBarColor = android.graphics.Color.WHITE
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightNavigationBars = true // Dark icons on white background
            isAppearanceLightStatusBars = true // Dark icons on white status bar
        }
        Timber.d("✅ Status bar and navigation bar set to white with dark icons")

        setContent {
            // Dismiss splash screen once Compose content is ready
            // This ensures smooth transition from splash to app
            LaunchedEffect(Unit) {
                keepSplashOnScreen = false
            }
            
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

                    // System bar color state
                    var statusBarColor by remember { mutableStateOf(Color.White) } // White

                    // Apply system bar colors using enableEdgeToEdge (Android 15+ compatible)
                    // This replaces deprecated window.statusBarColor and window.navigationBarColor
                    LaunchedEffect(statusBarColor) {
                        // Use enableEdgeToEdge with SystemBarStyle for Android 15+ compatibility
                        // This is the recommended approach instead of deprecated window.statusBarColor
                        
                        // Calculate luminance properly for color detection
                        val luminance = (0.299 * statusBarColor.red + 0.587 * statusBarColor.green + 0.114 * statusBarColor.blue)
                        val isLightStatusBar = luminance > 0.5f
                        
                        val statusBarStyle = if (isLightStatusBar) {
                            // Light status bar - dark icons
                            SystemBarStyle.light(
                                scrim = statusBarColor.toArgb(),
                                darkScrim = statusBarColor.toArgb()
                            )
                        } else {
                            // Dark status bar - light icons
                            SystemBarStyle.dark(scrim = statusBarColor.toArgb())
                        }
                        
                        enableEdgeToEdge(
                            statusBarStyle = statusBarStyle,
                            navigationBarStyle = SystemBarStyle.light(
                                scrim = android.graphics.Color.WHITE,
                                darkScrim = android.graphics.Color.WHITE
                            )
                        )
                        
                        // Keep navigation bar white with dark icons
                        window.navigationBarColor = android.graphics.Color.WHITE
                        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
                        
                        Timber.d("Status bar color changed to: ${statusBarColor} (edge-to-edge)")
                    }
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        Timber.d("🚀 Initializing MainNavGraph")
                        
                        // MODERN 2024-2026 APPROACH: Use navController.handleDeepLink()
                        // This is the OFFICIAL Jetpack Compose Navigation method
                        // Used by Google, LinkedIn, Instagram, and all modern apps
                        LaunchedEffect(navController) {
                            // Small delay to ensure NavController and navigation graph are fully initialized
                            kotlinx.coroutines.delay(50)
                            
                            val deepLinkIntent = intent
                            if (deepLinkIntent?.data != null) {
                                Timber.i("🔗 DEEP LINK: Handling deep link from onCreate")
                                Timber.d("🔗 DEEP LINK: URI = ${deepLinkIntent.data}")
                                Timber.d("🔗 DEEP LINK: Action = ${deepLinkIntent.action}")
                                
                                try {
                                    // OFFICIAL METHOD: navController.handleDeepLink()
                                    // This is the recommended way in Jetpack Compose Navigation 2024+
                                    val handled = navController.handleDeepLink(deepLinkIntent)
                                    if (handled) {
                                        Timber.i("🔗 DEEP LINK: ✅ Successfully handled by NavController")
                                    } else {
                                        Timber.w("🔗 DEEP LINK: ⚠️ NavController couldn't handle, trying manual")
                                        // Fallback to manual handling
                                        com.example.dutype.utils.DeepLinkHandler.handleDeepLink(
                                            deepLinkIntent,
                                            navController
                                        )
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "🔗 DEEP LINK: ❌ Error, trying manual handling")
                                    // Fallback to manual handling
                                    com.example.dutype.utils.DeepLinkHandler.handleDeepLink(
                                        deepLinkIntent,
                                        navController
                                    )
                                }
                            } else {
                                Timber.d("🔗 DEEP LINK: No deep link in intent")
                            }
                        }
                        
                        MainNavGraph(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                statusBarColor = color
                            },
                            notificationData = intent.extras?.getString("notificationId"),
                            notificationPermissionManager = notificationPermissionManager,
                            notificationIntent = intent
                        )
                        
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
        super.onNewIntent(newIntent)
        setIntent(newIntent) // CRITICAL: Update the activity's intent
        
        Timber.i("🔗 DEEP LINK: MainActivity.onNewIntent() - New intent received")
        Timber.d("🔗 DEEP LINK: Intent data = ${newIntent.data}")
        Timber.d("🔗 DEEP LINK: Intent action = ${newIntent.action}")
        
        // Log notification intent if present
        if (newIntent.getBooleanExtra("from_notification", false)) {
            Timber.i("🔗 DEEP LINK: New intent from notification")
            Timber.d("🔗 DEEP LINK: Notification type: ${newIntent.getStringExtra("notification_type")}")
        }
        
        // CRITICAL FIX: Recreate the activity to trigger LaunchedEffect with new intent
        // This is the INDUSTRY STANDARD approach (LinkedIn, Instagram, Uber)
        // It ensures the deep link is handled properly even when app is already running
        val deepLinkUri = newIntent.data
        if (deepLinkUri != null) {
            Timber.i("🔗 DEEP LINK: ✅ Deep link detected in onNewIntent: $deepLinkUri")
            Timber.i("🔗 DEEP LINK: Broadcasting to MainNavGraph deep link receiver...")
            
            // P1 FIX: Use LocalBroadcast instead of recreate() to avoid full activity rebuild
            // MainNavGraph already has a broadcast receiver registered for this
            val broadcastIntent = android.content.Intent("com.example.dutype.DEEP_LINK").apply {
                putExtra("deep_link_uri", deepLinkUri.toString())
            }
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                .sendBroadcast(broadcastIntent)
        } else {
            Timber.w("🔗 DEEP LINK: ⚠️ No deep link URI found in intent")
        }
    }
    
    override fun onStart() {
        super.onStart()
        Timber.d("📱 MainActivity.onStart()")
    }
    
    override fun onResume() {
        super.onResume()
        Timber.d("📱 MainActivity.onResume()")
        
        // Check for in-app updates (automatically skipped in debug builds)
        lifecycleScope.launch {
            updateManager.checkForUpdate(
                activity = this@MainActivity,
                activityResultLauncher = updateResultLauncher,
                onUpdateAvailable = { appUpdateInfo, updateType ->
                    Timber.i("🔄 Update available - type: ${if (updateType == com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE) "IMMEDIATE" else "FLEXIBLE"}")
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
            .background(Color.White),
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
                text = "Under Maintenance",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Text(
                text = "We're making DutyPe even better for you. Please check back in a few minutes.",
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
            .background(Color.White),
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
                text = "Update Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Text(
                text = "A new version of DutyPe is available with important updates. Please update to continue.",
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
                    text = "Update Now",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
