package com.example.dutype.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages Interstitial Ad loading and display
 * Interstitials are full-screen ads that appear between app content
 */
class InterstitialAdManager(private val context: Context) {
    
    private var interstitialAd: InterstitialAd? = null
    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady
    
    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading
    
    // Production Ad Unit ID for Interstitial Ads
    // Test Ad Unit ID: "ca-app-pub-3940256099942544/1033173712"
    private val AD_UNIT_ID = "ca-app-pub-5503082977524600/6394260795"
    
    /**
     * Load interstitial ad with callback
     */
    fun loadInterstitialAd(onAdLoaded: () -> Unit = {}, onAdFailed: (String) -> Unit = {}) {
        if (_isAdLoading.value) {
            println("⚠️ Ad already loading, skipping duplicate request")
            return
        }
        
        _isAdLoading.value = true
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    super.onAdLoaded(ad)
                    interstitialAd = ad
                    _isAdReady.value = true
                    _isAdLoading.value = false
                    println("✅ Interstitial ad loaded successfully")
                    onAdLoaded()
                }
                
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    println("❌ Interstitial ad failed to load: ${adError.message}")
                    _isAdReady.value = false
                    _isAdLoading.value = false
                    onAdFailed(adError.message)
                }
            }
        )
    }
    
    /**
     * Show the loaded interstitial ad
     * Must call from an Activity context
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (interstitialAd == null) {
            println("⚠️ Interstitial ad is not ready yet")
            onAdDismissed()
            return
        }
        
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                println("✅ Ad dismissed by user")
                interstitialAd = null
                _isAdReady.value = false
                onAdDismissed()
                
                // Load next ad for future use
                loadInterstitialAd()
            }
            
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                println("❌ Ad failed to show: ${adError.message}")
                interstitialAd = null
                _isAdReady.value = false
                onAdDismissed()
            }
            
            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                println("✅ Ad showed successfully on screen")
            }
            
            override fun onAdClicked() {
                super.onAdClicked()
                println("👆 User clicked ad")
            }
            
            override fun onAdImpression() {
                super.onAdImpression()
                println("👁️ Ad impression recorded")
            }
        }
        
        try {
            interstitialAd?.show(activity)
            println("📺 Ad show command sent to activity")
        } catch (e: Exception) {
            println("❌ Error showing ad: ${e.message}")
            onAdDismissed()
        }
    }
    
    /**
     * Check if ad is ready to show
     */
    fun isReady(): Boolean = interstitialAd != null && _isAdReady.value
    
    /**
     * Preload ad for faster display later
     */
    fun preloadAd() {
        if (!_isAdReady.value && !_isAdLoading.value) {
            loadInterstitialAd()
        }
    }
}
