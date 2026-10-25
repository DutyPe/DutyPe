package com.example.dutype.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import timber.log.Timber

/**
 * DutyPe Official Production Google AdMob Unit IDs.
 * Real ads exclusively loaded for live AdMob monetization.
 */
object AdMobConfig {
    /** Live AdMob Banner ID for Job Description pre-map and In-feed list */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-5503082977524600/2289947878"

    /** Live AdMob Unit ID for Success & Post-Action screens */
    const val SUCCESS_AD_UNIT_ID = "ca-app-pub-5503082977524600/1475559655"

    /** Live AdMob Unit ID for Worker Earnings & Referral screens */
    const val EARNINGS_AD_UNIT_ID = "ca-app-pub-5503082977524600/7322782701"
}

/**
 * 1. Post-Action Real AdMob Card.
 * Displayed on Success screens (Job Application Sent, Service Booking Confirmed).
 * Prioritizes live Google AdMob (Medium Rectangle, fallback to Banner).
 * No hardcoded or mock ads are rendered.
 */
@Composable
fun DutyPePostActionAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.SUCCESS_AD_UNIT_ID
) {
    var isLoaded by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }

    if (!isFailed) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isLoaded) Modifier.padding(vertical = 8.dp) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        this.adUnitId = adUnitId
                        setAdSize(AdSize.MEDIUM_RECTANGLE)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isLoaded = true
                                isFailed = false
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Timber.d("AdMob post-action ad failed with MEDIUM_RECTANGLE: ${error.message}, trying standard BANNER")
                                setAdSize(AdSize.BANNER)
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        isLoaded = true
                                        isFailed = false
                                    }
                                    override fun onAdFailedToLoad(err: LoadAdError) {
                                        Timber.d("AdMob post-action ad failed with BANNER: ${err.message}")
                                        isLoaded = false
                                        isFailed = true
                                    }
                                }
                                loadAd(AdRequest.Builder().build())
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}

/**
 * 2. Pre-Map Ad Banner (Job Description Screen).
 * Placed right before Location Map in JobDescriptionScreen.
 * No hardcoded or mock ads are rendered.
 */
@Composable
fun DutyPePreMapAdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.BANNER_AD_UNIT_ID
) {
    var isLoaded by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }

    if (!isFailed) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isLoaded) Modifier.padding(vertical = 6.dp) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        this.adUnitId = adUnitId
                        setAdSize(AdSize.BANNER)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isLoaded = true
                                isFailed = false
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Timber.d("AdMob pre-map banner failed: ${error.message}")
                                isLoaded = false
                                isFailed = true
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}

/**
 * 3. In-Feed Native Job Ad Card.
 * Fits cleanly inside the worker job list.
 * No hardcoded or mock ads are rendered.
 */
@Composable
fun DutyPeInFeedAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.BANNER_AD_UNIT_ID
) {
    var isLoaded by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }

    if (!isFailed) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isLoaded) Modifier.padding(vertical = 4.dp) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        this.adUnitId = adUnitId
                        setAdSize(AdSize.BANNER)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isLoaded = true
                                isFailed = false
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Timber.d("AdMob in-feed banner failed: ${error.message}")
                                isLoaded = false
                                isFailed = true
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}

/**
 * 4. Earnings & Referral Ad Banner.
 * Placed in Earnings Dashboard and Refer & Earn screens using AdMob Banner ID.
 * No hardcoded or mock ads are rendered.
 */
@Composable
fun DutyPeEarningsBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.EARNINGS_AD_UNIT_ID
) {
    var isLoaded by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }

    if (!isFailed) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isLoaded) Modifier.padding(vertical = 6.dp) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        this.adUnitId = adUnitId
                        setAdSize(AdSize.BANNER)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isLoaded = true
                                isFailed = false
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Timber.d("AdMob earnings banner failed: ${error.message}")
                                isLoaded = false
                                isFailed = true
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}

/**
 * Standard AdMob Banner view wrapper.
 */
@Composable
fun AdMobBanner(
    adUnitId: String,
    modifier: Modifier = Modifier,
    adSize: AdSize = AdSize.BANNER
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                this.adUnitId = adUnitId
                setAdSize(adSize)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
