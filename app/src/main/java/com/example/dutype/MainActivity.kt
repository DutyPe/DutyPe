package com.example.dutype

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.dutype.app.BuildConfig
import com.example.dutype.components.DeveloperModeChecker
import com.example.dutype.components.DeveloperModeWarningSheet
import com.example.dutype.navigation.MainNavGraph
import com.example.dutype.services.FCMTokenManager
import com.example.dutype.services.JobApplicationService
import com.example.dutype.ui.theme.dutypeTheme
import com.example.dutype.ui.theme.ResponsiveTheme
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.rememberWindowSizeClass
import com.example.dutype.viewmodels.SubscriptionViewModel
import com.razorpay.PaymentResultListener
// Ads temporarily disabled for testing
// import com.example.dutype.ads.AdsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultListener {
    
    // Create NotificationPermissionManager at the activity level
    private lateinit var notificationPermissionManager: NotificationPermissionManager
    
    @Inject
    lateinit var jobApplicationService: JobApplicationService
    
    @Inject
    lateinit var fcmTokenManager: FCMTokenManager
    
    // Razorpay payment callbacks
    private var onPaymentSuccess: ((String, String?, String?) -> Unit)? = null
    private var onPaymentError: ((Int, String) -> Unit)? = null
    
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
            val lifecycleOwner = LocalLifecycleOwner.current
            
            // Check developer mode on app start - PRODUCTION ONLY (release builds)
            LaunchedEffect(Unit) {
                if (!BuildConfig.DEBUG) {
                    showDeveloperModeWarning = DeveloperModeChecker.isDeveloperModeEnabled(this@MainActivity)
                    if (showDeveloperModeWarning) {
                        Timber.w("⚠️ Developer Mode detected - showing security warning")
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
    
    // PaymentResultListener implementation for Razorpay
    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        Timber.d("💳 Razorpay Payment Success: $razorpayPaymentID")
        razorpayPaymentID?.let { paymentId ->
            // Broadcast payment success to ViewModel via event bus or shared state
            PaymentResultHolder.setSuccess(paymentId, null, null)
        }
    }
    
    override fun onPaymentError(code: Int, response: String?) {
        Timber.e("💳 Razorpay Payment Error: $code - $response")
        PaymentResultHolder.setError(code, response ?: "Payment failed")
    }
}

/**
 * Singleton to hold payment results for ViewModel to observe
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
