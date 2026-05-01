package com.example.dutype.ads

import android.app.Activity
import android.content.Context
import com.example.dutype.analytics.Analytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor() {

    companion object {
        const val CONTACTS_PER_AD = 3
    }

    private val _isInterstitialLoading = MutableStateFlow(false)
    val isInterstitialLoading: StateFlow<Boolean> = _isInterstitialLoading

    private val _isEmployerRewardedLoading = MutableStateFlow(false)
    val isEmployerRewardedLoading: StateFlow<Boolean> = _isEmployerRewardedLoading

    private val _isWorkerRewardedLoading = MutableStateFlow(false)
    val isWorkerRewardedLoading: StateFlow<Boolean> = _isWorkerRewardedLoading

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady

    private val _isEmployerRewardedReady = MutableStateFlow(true)
    val isEmployerRewardedReady: StateFlow<Boolean> = _isEmployerRewardedReady

    private val _isWorkerRewardedReady = MutableStateFlow(true)
    val isWorkerRewardedReady: StateFlow<Boolean> = _isWorkerRewardedReady

    fun initialize(context: Context) {
        Timber.i("AdMob SDK disabled to keep release dex and Play update size low")
    }

    fun loadInterstitialAd(context: Context) {
        _isInterstitialLoading.value = false
        _isInterstitialReady.value = false
        Timber.d("Interstitial ads disabled")
    }

    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit,
        onAdNotReady: () -> Unit
    ) {
        Timber.d("Interstitial ads disabled; continuing flow")
        onAdNotReady()
    }

    fun loadEmployerRewardedAd(context: Context) {
        _isEmployerRewardedLoading.value = false
        _isEmployerRewardedReady.value = true
        Timber.d("Employer rewarded ads disabled")
    }

    fun showEmployerRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdNotReady: () -> Unit
    ) {
        Timber.d("Employer rewarded ads disabled; granting unlock flow")
        Analytics.rewardedAdCompleted(placement = "employer_contact_unlock_no_ad")
        onRewarded()
    }

    fun loadWorkerRewardedAd(context: Context) {
        _isWorkerRewardedLoading.value = false
        _isWorkerRewardedReady.value = true
        Timber.d("Worker rewarded ads disabled")
    }

    fun showWorkerRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onAdNotReady: () -> Unit
    ) {
        Timber.d("Worker rewarded ads disabled; granting job view flow")
        Analytics.rewardedAdCompleted(placement = "worker_job_description_unlock_no_ad")
        onRewarded()
    }

    fun preloadAllAds(context: Context) {
        _isInterstitialReady.value = false
        _isEmployerRewardedReady.value = true
        _isWorkerRewardedReady.value = true
        Timber.d("Ad preloading skipped because AdMob SDK is disabled")
    }
}
