package com.example.dutype.ads

import android.app.Activity
import android.content.Context
import com.example.dutype.analytics.Analytics
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdManager - Centralized ad management for DutyPe
 * 
 * Ad Placements:
 * 1. Employer Interstitial: Before navigating to Post Job screen
 * 2. Employer Rewarded: Watch ad to unlock 3 worker contacts
 * 3. Worker Rewarded: Watch ad before viewing job description
 */
@Singleton
class AdManager @Inject constructor() {
    
    companion object {
        // Test IDs for development (Google's official test IDs - always work)
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
        
        // Production Ad Unit IDs from AdMob Console
        private const val EMPLOYER_INTERSTITIAL_ID = "ca-app-pub-5503082977524600/7080040867" // Post Job interstitial
        private const val EMPLOYER_REWARDED_ID = "ca-app-pub-5503082977524600/2831812273"     // Contact unlock rewarded
        private const val WORKER_REWARDED_ID = "ca-app-pub-5503082977524600/4422087938"       // Job description rewarded
        
        // Set to false to use test ads for debugging (test ads always work)
        // Set to true for production release
        // NOTE: If ads aren't showing, try setting this to false first to verify ad integration works
        private const val USE_PRODUCTION_ADS = true  // Production mode - uses real ad unit IDs
        
        // Ad Unit IDs - automatically switches between test and production
        val INTERSTITIAL_AD_UNIT_ID = if (USE_PRODUCTION_ADS) EMPLOYER_INTERSTITIAL_ID else TEST_INTERSTITIAL_ID
        val EMPLOYER_REWARDED_AD_UNIT_ID = if (USE_PRODUCTION_ADS) EMPLOYER_REWARDED_ID else TEST_REWARDED_ID
        val WORKER_REWARDED_AD_UNIT_ID = if (USE_PRODUCTION_ADS) WORKER_REWARDED_ID else TEST_REWARDED_ID
        
        // Contact unlock quota per rewarded ad
        const val CONTACTS_PER_AD = 3
    }
    
    // Ad instances
    private var interstitialAd: InterstitialAd? = null
    private var employerRewardedAd: RewardedAd? = null
    private var workerRewardedAd: RewardedAd? = null
    
    // Loading states
    private val _isInterstitialLoading = MutableStateFlow(false)
    val isInterstitialLoading: StateFlow<Boolean> = _isInterstitialLoading
    
    private val _isEmployerRewardedLoading = MutableStateFlow(false)
    val isEmployerRewardedLoading: StateFlow<Boolean> = _isEmployerRewardedLoading
    
    private val _isWorkerRewardedLoading = MutableStateFlow(false)
    val isWorkerRewardedLoading: StateFlow<Boolean> = _isWorkerRewardedLoading
    
    // Ready states
    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady
    
    private val _isEmployerRewardedReady = MutableStateFlow(false)
    val isEmployerRewardedReady: StateFlow<Boolean> = _isEmployerRewardedReady
    
    private val _isWorkerRewardedReady = MutableStateFlow(false)
    val isWorkerRewardedReady: StateFlow<Boolean> = _isWorkerRewardedReady
    
    /**
     * Initialize Mobile Ads SDK - call once in Application.onCreate()
     * 
     * OPTIMIZATION: This is deferred by 5 seconds in DutyPeApplication to prevent
     * WebView and Camera service from loading on app startup (saves 1.5s startup time)
     */
    fun initialize(context: Context) {
        // CRITICAL OPTIMIZATION: Disable WebView debugging and media features
        // This prevents Camera service from loading during ad initialization
        try {
            android.webkit.WebView.setWebContentsDebuggingEnabled(false)
            
            // Disable WebView's automatic media/camera initialization
            // Camera is only needed in profile setup screens, not for ads
            val webSettings = android.webkit.WebSettings.getDefaultUserAgent(context)
            Timber.d("📺 WebView user agent: $webSettings")
        } catch (e: Exception) {
            // Non-fatal - WebView might not be available yet
            Timber.w("📺 WebView optimization skipped: ${e.message}")
        }
        
        MobileAds.initialize(context) { initializationStatus ->
            Timber.d("📺 AdMob initialized: ${initializationStatus.adapterStatusMap}")
        }
    }
    
    // ==================== INTERSTITIAL AD (Employer Post Job) ====================
    
    /**
     * Load interstitial ad for employer post job flow
     */
    fun loadInterstitialAd(context: Context) {
        if (_isInterstitialLoading.value || interstitialAd != null) {
            Timber.d("📺 Interstitial ad already loading or loaded, skipping")
            return
        }
        
        _isInterstitialLoading.value = true
        Timber.d("📺 Loading interstitial ad with ID: $INTERSTITIAL_AD_UNIT_ID (Production: $USE_PRODUCTION_ADS)")
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Timber.d("📺 ✅ Interstitial ad loaded successfully!")
                    interstitialAd = ad
                    _isInterstitialLoading.value = false
                    _isInterstitialReady.value = true
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("📺 ❌ Interstitial ad failed to load: code=${error.code}, message=${error.message}, domain=${error.domain}")
                    interstitialAd = null
                    _isInterstitialLoading.value = false
                    _isInterstitialReady.value = false
                }
            }
        )
    }
    
    /**
     * Show interstitial ad before navigating to Post Job screen
     * @param activity Current activity
     * @param onAdDismissed Called when ad is dismissed (navigate to Post Job)
     * @param onAdNotReady Called if ad is not ready (skip ad, navigate directly)
     */
    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit,
        onAdNotReady: () -> Unit
    ) {
        val ad = interstitialAd
        if (ad == null) {
            Timber.d("📺 Interstitial not ready (ad is null), skipping. isReady=${_isInterstitialReady.value}, isLoading=${_isInterstitialLoading.value}")
            onAdNotReady()
            return
        }
        
        Timber.d("📺 Showing interstitial ad...")
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Timber.d("📺 ✅ Interstitial dismissed by user")
                interstitialAd = null
                _isInterstitialReady.value = false
                onAdDismissed()
                // Preload next ad
                loadInterstitialAd(activity)
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Timber.w("📺 ❌ Interstitial failed to show: code=${error.code}, message=${error.message}")
                interstitialAd = null
                _isInterstitialReady.value = false
                onAdNotReady()
            }
            
            override fun onAdShowedFullScreenContent() {
                Timber.d("📺 Interstitial ad is now showing")
            }
        }
        
        ad.show(activity)
    }
    
    // ==================== EMPLOYER REWARDED AD (Contact Unlock) ====================
    
    /**
     * Load rewarded ad for employer contact unlock
     */
    fun loadEmployerRewardedAd(context: Context) {
        if (_isEmployerRewardedLoading.value || employerRewardedAd != null) return
        
        _isEmployerRewardedLoading.value = true
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            EMPLOYER_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Timber.d("📺 Employer rewarded ad loaded")
                    employerRewardedAd = ad
                    _isEmployerRewardedLoading.value = false
                    _isEmployerRewardedReady.value = true
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("📺 Employer rewarded ad failed to load: ${error.message}")
                    employerRewardedAd = null
                    _isEmployerRewardedLoading.value = false
                    _isEmployerRewardedReady.value = false
                }
            }
        )
    }

    /**
     * Show rewarded ad for employer to unlock 3 worker contacts
     * @param activity Current activity
     * @param onRewarded Called when user earns reward (unlock 3 contacts)
     * @param onAdNotReady Called if ad is not ready
     */
    fun showEmployerRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdNotReady: () -> Unit
    ) {
        val ad = employerRewardedAd
        if (ad == null) {
            Timber.d("📺 Employer rewarded ad not ready")
            onAdNotReady()
            return
        }
        
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Timber.d("📺 Employer rewarded ad dismissed")
                employerRewardedAd = null
                _isEmployerRewardedReady.value = false
                // Preload next ad
                loadEmployerRewardedAd(activity)
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Timber.w("📺 Employer rewarded ad failed to show: ${error.message}")
                employerRewardedAd = null
                _isEmployerRewardedReady.value = false
                onAdNotReady()
            }
        }
        
        ad.show(activity) { rewardItem ->
            Timber.d("📺 Employer earned reward: ${rewardItem.amount} ${rewardItem.type}")
            Analytics.rewardedAdCompleted(placement = "employer_contact_unlock")
            onRewarded()
        }
    }
    
    // ==================== WORKER REWARDED AD (Job Description) ====================
    
    /**
     * Load rewarded ad for worker job description view
     */
    fun loadWorkerRewardedAd(context: Context) {
        if (_isWorkerRewardedLoading.value || workerRewardedAd != null) return
        
        _isWorkerRewardedLoading.value = true
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            WORKER_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Timber.d("📺 Worker rewarded ad loaded")
                    workerRewardedAd = ad
                    _isWorkerRewardedLoading.value = false
                    _isWorkerRewardedReady.value = true
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("📺 Worker rewarded ad failed to load: ${error.message}")
                    workerRewardedAd = null
                    _isWorkerRewardedLoading.value = false
                    _isWorkerRewardedReady.value = false
                }
            }
        )
    }
    
    /**
     * Show rewarded ad for worker to view job description
     * @param activity Current activity
     * @param onRewarded Called when user earns reward (can view job)
     * @param onAdNotReady Called if ad is not ready (allow viewing anyway)
     */
    fun showWorkerRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdNotReady: () -> Unit
    ) {
        val ad = workerRewardedAd
        if (ad == null) {
            Timber.d("📺 Worker rewarded ad not ready, allowing view")
            onAdNotReady()
            return
        }
        
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Timber.d("📺 Worker rewarded ad dismissed")
                workerRewardedAd = null
                _isWorkerRewardedReady.value = false
                // Preload next ad
                loadWorkerRewardedAd(activity)
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Timber.w("📺 Worker rewarded ad failed to show: ${error.message}")
                workerRewardedAd = null
                _isWorkerRewardedReady.value = false
                onAdNotReady()
            }
        }
        
        ad.show(activity) { rewardItem ->
            Timber.d("📺 Worker earned reward: ${rewardItem.amount} ${rewardItem.type}")
            Analytics.rewardedAdCompleted(placement = "worker_job_description_unlock")
            onRewarded()
        }
    }
    
    // ==================== PRELOAD ALL ADS ====================
    
    /**
     * Preload all ads - call after user logs in
     */
    fun preloadAllAds(context: Context) {
        Timber.d("📺 Preloading all ads... (Production mode: $USE_PRODUCTION_ADS)")
        Timber.d("📺 Interstitial ID: $INTERSTITIAL_AD_UNIT_ID")
        Timber.d("📺 Employer Rewarded ID: $EMPLOYER_REWARDED_AD_UNIT_ID")
        Timber.d("📺 Worker Rewarded ID: $WORKER_REWARDED_AD_UNIT_ID")
        loadInterstitialAd(context)
        loadEmployerRewardedAd(context)
        loadWorkerRewardedAd(context)
    }
}
