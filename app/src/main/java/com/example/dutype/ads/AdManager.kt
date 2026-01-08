package com.example.dutype.ads

import android.app.Activity
import android.content.Context
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
        // TODO: Replace with your actual Ad Unit IDs from AdMob console
        // Test IDs for development (replace with real IDs before release)
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
        
        // Production Ad Unit IDs (replace these with your actual IDs)
        private const val EMPLOYER_INTERSTITIAL_ID = "ca-app-pub-5503082977524600/XXXXXXXXXX" // Post Job interstitial
        private const val EMPLOYER_REWARDED_ID = "ca-app-pub-5503082977524600/XXXXXXXXXX"     // Contact unlock rewarded
        private const val WORKER_REWARDED_ID = "ca-app-pub-5503082977524600/XXXXXXXXXX"       // Job description rewarded
        
        // Use test IDs in debug, production IDs in release
        val INTERSTITIAL_AD_UNIT_ID = TEST_INTERSTITIAL_ID // Change to EMPLOYER_INTERSTITIAL_ID for production
        val EMPLOYER_REWARDED_AD_UNIT_ID = TEST_REWARDED_ID // Change to EMPLOYER_REWARDED_ID for production
        val WORKER_REWARDED_AD_UNIT_ID = TEST_REWARDED_ID   // Change to WORKER_REWARDED_ID for production
        
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
     */
    fun initialize(context: Context) {
        MobileAds.initialize(context) { initializationStatus ->
            Timber.d("📺 AdMob initialized: ${initializationStatus.adapterStatusMap}")
        }
    }
    
    // ==================== INTERSTITIAL AD (Employer Post Job) ====================
    
    /**
     * Load interstitial ad for employer post job flow
     */
    fun loadInterstitialAd(context: Context) {
        if (_isInterstitialLoading.value || interstitialAd != null) return
        
        _isInterstitialLoading.value = true
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Timber.d("📺 Interstitial ad loaded")
                    interstitialAd = ad
                    _isInterstitialLoading.value = false
                    _isInterstitialReady.value = true
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("📺 Interstitial ad failed to load: ${error.message}")
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
            Timber.d("📺 Interstitial not ready, skipping")
            onAdNotReady()
            return
        }
        
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Timber.d("📺 Interstitial dismissed")
                interstitialAd = null
                _isInterstitialReady.value = false
                onAdDismissed()
                // Preload next ad
                loadInterstitialAd(activity)
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Timber.w("📺 Interstitial failed to show: ${error.message}")
                interstitialAd = null
                _isInterstitialReady.value = false
                onAdNotReady()
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
            onRewarded()
        }
    }
    
    // ==================== PRELOAD ALL ADS ====================
    
    /**
     * Preload all ads - call after user logs in
     */
    fun preloadAllAds(context: Context) {
        loadInterstitialAd(context)
        loadEmployerRewardedAd(context)
        loadWorkerRewardedAd(context)
    }
}
