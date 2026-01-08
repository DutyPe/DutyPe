package com.example.dutype.viewmodels

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.ads.AdManager
import com.example.dutype.ads.AdPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * AdViewModel - Manages ad state and operations
 * 
 * Handles:
 * 1. Employer Interstitial: Before Post Job screen
 * 2. Employer Rewarded: Unlock 3 worker contacts
 * 3. Worker Rewarded: View job description
 */
@HiltViewModel
class AdViewModel @Inject constructor(
    private val adManager: AdManager,
    private val adPreferences: AdPreferences
) : ViewModel() {
    
    // Expose ad ready states
    val isInterstitialReady = adManager.isInterstitialReady
    val isEmployerRewardedReady = adManager.isEmployerRewardedReady
    val isWorkerRewardedReady = adManager.isWorkerRewardedReady
    
    // Contact unlocks remaining
    private val _contactUnlocksRemaining = MutableStateFlow(0)
    val contactUnlocksRemaining: StateFlow<Int> = _contactUnlocksRemaining.asStateFlow()
    
    // ==================== INTERSTITIAL AD (Employer Post Job) ====================
    
    /**
     * Load interstitial ad
     */
    fun loadInterstitialAd(context: Context) {
        adManager.loadInterstitialAd(context)
    }
    
    /**
     * Show interstitial ad before Post Job navigation
     */
    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit,
        onAdNotReady: () -> Unit
    ) {
        adManager.showInterstitialAd(activity, onAdDismissed, onAdNotReady)
    }
    
    // ==================== EMPLOYER REWARDED AD (Contact Unlock) ====================
    
    /**
     * Load employer rewarded ad
     */
    fun loadEmployerRewardedAd(context: Context) {
        adManager.loadEmployerRewardedAd(context)
    }
    
    /**
     * Check if contact is already unlocked
     */
    fun isContactUnlocked(context: Context, applicationId: String): Boolean {
        return adPreferences.isContactUnlocked(context, applicationId)
    }
    
    /**
     * Get remaining contact unlocks
     */
    fun getContactUnlocksRemaining(context: Context): Int {
        val remaining = adPreferences.getContactUnlocksRemaining(context)
        _contactUnlocksRemaining.value = remaining
        return remaining
    }
    
    /**
     * Try to unlock contact - uses existing unlocks or shows rewarded ad
     * @param applicationId The application ID to unlock
     * @param activity Current activity for showing ad
     * @param onUnlocked Called when contact is unlocked (either from existing unlocks or after ad)
     * @param onNeedToWatchAd Called when user needs to watch ad (no unlocks remaining)
     */
    fun unlockContact(
        context: Context,
        applicationId: String,
        activity: Activity,
        onUnlocked: () -> Unit,
        onNeedToWatchAd: () -> Unit
    ) {
        // Check if already unlocked
        if (adPreferences.isContactUnlocked(context, applicationId)) {
            Timber.d("📺 Contact already unlocked: $applicationId")
            onUnlocked()
            return
        }
        
        // Try to use existing unlock
        if (adPreferences.useContactUnlock(context, applicationId)) {
            Timber.d("📺 Used existing unlock for: $applicationId")
            _contactUnlocksRemaining.value = adPreferences.getContactUnlocksRemaining(context)
            onUnlocked()
            return
        }
        
        // No unlocks remaining - need to watch ad
        onNeedToWatchAd()
    }
    
    /**
     * Show rewarded ad to earn 3 contact unlocks
     */
    fun showEmployerRewardedAd(
        activity: Activity,
        context: Context,
        onRewarded: () -> Unit,
        onAdNotReady: () -> Unit
    ) {
        adManager.showEmployerRewardedAd(
            activity = activity,
            onRewarded = {
                // Add 3 contact unlocks
                adPreferences.addContactUnlocks(context, AdManager.CONTACTS_PER_AD)
                adPreferences.incrementAdsWatched(context)
                _contactUnlocksRemaining.value = adPreferences.getContactUnlocksRemaining(context)
                Timber.d("📺 Employer earned ${AdManager.CONTACTS_PER_AD} contact unlocks")
                onRewarded()
            },
            onAdNotReady = onAdNotReady
        )
    }
    
    // ==================== WORKER REWARDED AD (Job Description) ====================
    
    /**
     * Load worker rewarded ad
     */
    fun loadWorkerRewardedAd(context: Context) {
        adManager.loadWorkerRewardedAd(context)
    }
    
    /**
     * Check if worker has already viewed this job today (no ad needed)
     */
    fun hasViewedJobToday(context: Context, jobId: String): Boolean {
        return adPreferences.hasViewedJobToday(context, jobId)
    }
    
    /**
     * Show rewarded ad for worker to view job description
     * @param jobId The job ID being viewed
     * @param activity Current activity
     * @param onCanView Called when worker can view job (after ad or if already viewed today)
     * @param onAdNotReady Called if ad not ready (allow viewing anyway)
     */
    fun showWorkerRewardedAdForJob(
        context: Context,
        jobId: String,
        activity: Activity,
        onCanView: () -> Unit,
        onAdNotReady: () -> Unit
    ) {
        // Check if already viewed today
        if (adPreferences.hasViewedJobToday(context, jobId)) {
            Timber.d("📺 Job already viewed today: $jobId")
            onCanView()
            return
        }
        
        // Show rewarded ad
        adManager.showWorkerRewardedAd(
            activity = activity,
            onRewarded = {
                // Mark job as viewed
                adPreferences.markJobViewed(context, jobId)
                adPreferences.incrementAdsWatched(context)
                Timber.d("📺 Worker earned job view: $jobId")
                onCanView()
            },
            onAdNotReady = {
                // Ad not ready - allow viewing anyway (better UX)
                adPreferences.markJobViewed(context, jobId)
                onAdNotReady()
            }
        )
    }
    
    // ==================== PRELOAD ALL ADS ====================
    
    /**
     * Preload all ads - call after user logs in
     */
    fun preloadAllAds(context: Context) {
        viewModelScope.launch {
            adManager.preloadAllAds(context)
        }
    }
}
