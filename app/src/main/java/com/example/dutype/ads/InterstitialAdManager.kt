package com.example.dutype.ads

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Manages Interstitial Ad loading and display
 * Interstitials are full-screen ads that appear between app content
 */
class InterstitialAdManager(private val context: Context) {
    
    // Ads disabled: no InterstitialAd instance is kept
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
        // Ads disabled for testing: immediately report failure so callers proceed
        Timber.i("ℹ️ loadInterstitialAd skipped (ads disabled for testing)")
        _isAdReady.value = false
        _isAdLoading.value = false
        onAdFailed("Ads disabled for testing")
    }
    
    /**
     * Show the loaded interstitial ad
     * Must call from an Activity context
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit = {}) {
        // Ads disabled for testing: immediately continue flow
        Timber.i("ℹ️ showInterstitialAd skipped (ads disabled for testing)")
        onAdDismissed()
    }
    
    /**
     * Check if ad is ready to show
     */
    fun isReady(): Boolean = _isAdReady.value
    
    /**
     * Preload ad for faster display later
     */
    fun preloadAd() {
        if (!_isAdReady.value && !_isAdLoading.value) {
            loadInterstitialAd()
        }
    }
}
