package com.example.dutype

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.dutype.app.BuildConfig
import com.example.dutype.components.DeveloperModeChecker
import com.example.dutype.components.DeveloperModeWarningSheet
import com.example.dutype.components.DeviceBlacklistedSheet
import com.example.dutype.navigation.MainNavGraph
import com.example.dutype.services.FCMTokenManager
import com.example.dutype.services.JobApplicationService
import com.example.dutype.services.BlacklistService
import com.example.dutype.services.DeviceFingerprintService
import com.example.dutype.ui.theme.dutypeTheme
import com.example.dutype.ui.theme.ResponsiveTheme
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.rememberWindowSizeClass
// NOTE: Razorpay/Subscription disabled - replaced with AdMob ads
// import com.example.dutype.viewmodels.SubscriptionViewModel
// import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // NOTE: PaymentResultListener removed - Razorpay disabled, using AdMob ads instead
    
    // Create NotificationPermissionManager at the activity level
    private lateinit var notificationPermissionManager: NotificationPermissionManager
    
    @Inject
    lateinit var jobApplicationService: JobApplicationService
    
    @Inject
    lateinit var fcmTokenManager: FCMTokenManager
    
    @Inject
    lateinit var blacklistService: BlacklistService
    
    @Inject
    lateinit var deviceFingerprintService: DeviceFingerprintService
    
    @Inject
    lateinit var metadataManager: com.example.dutype.metadata.MetadataManager
    
    // NOTE: Razorpay payment callbacks removed - using AdMob ads instead
    
    override fun attachBaseContext(newBase: Context) {
        // Apply saved language preference
        super.attachBaseContext(LocaleHelper.setLocale(newBase))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
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

        // Create NotificationPermissionManager before setContent
        notificationPermissionManager = NotificationPermissionManager(this)
        Timber.d("✅ NotificationPermissionManager initialized")

        // Enable edge-to-edge for Android 15+ compatibility
        // This is the recommended way for SDK 35+
        enableEdgeToEdge()
        Timber.d("✅ Edge-to-edge enabled (Android 15+ compatible)")

        setContent {
            val windowSizeClass = rememberWindowSizeClass()
            
            // Developer mode detection state - ENABLED FOR PRODUCTION
            // This warns users if Developer Options are enabled on their device
            var showDeveloperModeWarning by remember { mutableStateOf(false) }
            var showDeviceBlacklistedWarning by remember { mutableStateOf(false) }
            var blacklistReason by remember { mutableStateOf("") }
            var showMaintenanceMode by remember { mutableStateOf(false) }
            var showForceUpdate by remember { mutableStateOf(false) }
            val lifecycleOwner = LocalLifecycleOwner.current
            
            // Check maintenance mode and force update on app start
            LaunchedEffect(Unit) {
                // Wait for metadata to initialize
                kotlinx.coroutines.delay(1000)
                
                // If user is already authenticated, initialize Firestore metadata
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    try {
                        metadataManager.initializeWithAuth()
                        Timber.d("📊 Metadata initialized for returning user")
                    } catch (e: Exception) {
                        Timber.w(e, "📊 Failed to initialize metadata for returning user")
                    }
                }
                
                // Check maintenance mode
                if (metadataManager.isMaintenanceMode()) {
                    Timber.w("🔧 App is in MAINTENANCE MODE")
                    showMaintenanceMode = true
                }
                
                // Check force update
                if (metadataManager.needsForceUpdate()) {
                    Timber.w("⬆️ Force update required")
                    showForceUpdate = true
                }
            }
            
            // Check developer mode on app start - PRODUCTION ONLY (release builds)
            LaunchedEffect(Unit) {
                if (!BuildConfig.DEBUG) {
                    showDeveloperModeWarning = DeveloperModeChecker.isDeveloperModeEnabled(this@MainActivity)
                    if (showDeveloperModeWarning) {
                        Timber.w("⚠️ Developer Mode detected - showing security warning")
                    }
                }
            }
            
            // P0 FIX #5: Check device blacklist on app launch
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        // Use canonical DeviceFingerprintService for device ID
                        val deviceId = deviceFingerprintService.getAndroidId(this@MainActivity)
                        if (deviceId.isNotBlank()) {
                            val result = blacklistService.isDeviceBlacklisted(deviceId)
                            if (result.isBlacklisted) {
                                Timber.w("🛡️ BLACKLIST: ⛔ Device is BLACKLISTED - ${result.reason}")
                                withContext(Dispatchers.Main) {
                                    showDeviceBlacklistedWarning = true
                                    blacklistReason = result.reason ?: "Violation of terms of service"
                                }
                            } else {
                                Timber.d("🛡️ BLACKLIST: ✅ Device is NOT blacklisted")
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "🛡️ BLACKLIST: Error checking device blacklist")
                    }
                }
            }
            
            // Re-check on resume (in case user disabled it in settings)
            LaunchedEffect(lifecycleOwner) {
                if (!BuildConfig.DEBUG) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val isDeveloperMode = DeveloperModeChecker.isDeveloperModeEnabled(this@MainActivity)
                            showDeveloperModeWarning = isDeveloperMode
                            if (isDeveloperMode) {
                                Timber.w("⚠️ Developer Mode still enabled on resume")
                            } else {
                                Timber.d("✅ Developer Mode is disabled")
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                }
            }
            
            // Refresh FCM token on app start to ensure push notifications work
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        // Check if user is authenticated before refreshing FCM token
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            try {
                                fcmTokenManager.registerToken()
                                Timber.d("✅ FCM token refreshed on app start")
                            } catch (e: Exception) {
                                Timber.e(e, "❌ Failed to refresh FCM token")
                            }
                        } else {
                            Timber.d("⏭️ Skipping FCM token refresh - user not authenticated")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "❌ Error in FCM token refresh")
                    }
                }
            }
            
            dutypeTheme {
                ResponsiveTheme(windowSizeClass = windowSizeClass) {
                    val navController = rememberNavController()

                    // System bar color state
                    var statusBarColor by remember { mutableStateOf(Color.White) } // White

                    // Apply system bar colors at the top level
                    LaunchedEffect(statusBarColor) {
                        window.statusBarColor = statusBarColor.toArgb()
                        window.navigationBarColor = Color.Black.toArgb()

                        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                        insetsController.isAppearanceLightStatusBars = true // Dark icons on white
                        insetsController.isAppearanceLightNavigationBars = false // Light icons on black
                        
                        Timber.d("Status bar color changed to: ${statusBarColor}")
                    }
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        Timber.d("🚀 Initializing MainNavGraph")
                        MainNavGraph(
                            navController = navController,
                            onStatusBarColorChange = { color ->
                                statusBarColor = color
                            },
                            notificationData = intent.extras?.getString("notificationId"),
                            notificationPermissionManager = notificationPermissionManager,
                            notificationIntent = intent
                        )
                        
                        // Developer Mode Warning Sheet - PRODUCTION ONLY (release builds)
                        if (!BuildConfig.DEBUG) {
                            DeveloperModeWarningSheet(
                                isVisible = showDeveloperModeWarning,
                                onDismissRequest = { /* Not dismissible - user must disable developer mode */ }
                            )
                        }
                        
                        // P0 FIX #5: Device Blacklisted Warning Sheet
                        DeviceBlacklistedSheet(
                            isVisible = showDeviceBlacklistedWarning,
                            reason = blacklistReason,
                            onAppealClick = {
                                // Open email intent for appeal
                                try {
                                    // Use canonical DeviceFingerprintService for device ID
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:support@dutype.com")
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Appeal: Device Blocked")
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Device ID: ${deviceFingerprintService.getAndroidId(this@MainActivity)}\n\nReason for appeal:\n")
                                    }
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to open email app")
                                }
                            }
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
    
    override fun onStart() {
        super.onStart()
        Timber.d("📱 MainActivity.onStart()")
    }
    
    override fun onResume() {
        super.onResume()
        Timber.d("📱 MainActivity.onResume()")
    }
    
    override fun onPause() {
        super.onPause()
        Timber.d("📱 MainActivity.onPause()")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Timber.d("📱 MainActivity.onDestroy()")
    }
    
    // NOTE: Razorpay PaymentResultListener removed - using AdMob ads instead
    // Payment callbacks commented out - no longer needed
    /*
    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        Timber.d("💳 Razorpay Payment Success: $razorpayPaymentID")
        razorpayPaymentID?.let { paymentId ->
            PaymentResultHolder.setSuccess(paymentId, null, null)
        }
    }
    
    override fun onPaymentError(code: Int, response: String?) {
        Timber.e("💳 Razorpay Payment Error: $code - $response")
        PaymentResultHolder.setError(code, response ?: "Payment failed")
    }
    */
}

/**
 * NOTE: PaymentResultHolder kept for backward compatibility but no longer used
 * Razorpay payments replaced with AdMob rewarded ads
 */
object PaymentResultHolder {
    private var successCallback: ((String, String?, String?) -> Unit)? = null
    private var errorCallback: ((Int, String) -> Unit)? = null
    
    fun setCallbacks(
        onSuccess: (String, String?, String?) -> Unit,
        onError: (Int, String) -> Unit
    ) {
        successCallback = onSuccess
        errorCallback = onError
    }
    
    fun setSuccess(paymentId: String, orderId: String?, signature: String?) {
        successCallback?.invoke(paymentId, orderId, signature)
    }
    
    fun setError(code: Int, message: String) {
        errorCallback?.invoke(code, message)
    }
    
    fun clearCallbacks() {
        successCallback = null
        errorCallback = null
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
