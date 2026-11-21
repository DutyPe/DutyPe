package com.example.dutype.ads

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val BANNER_AD_UNIT_ID: String
        get() = if (com.dutype.app.BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5503082977524600/2289947878"

    /** Live AdMob Unit ID for Success & Post-Action screens */
    val SUCCESS_AD_UNIT_ID: String
        get() = if (com.dutype.app.BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5503082977524600/1475559655"

    /** Live AdMob Unit ID for Worker Earnings & Referral screens / Job Description Video ads */
    val EARNINGS_AD_UNIT_ID: String
        get() = if (com.dutype.app.BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-5503082977524600/7322782701"

    /** Live AdMob Rewarded Video Unit ID for Spin & Win */
    val REWARDED_AD_UNIT_ID: String
        get() = if (com.dutype.app.BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917" else "ca-app-pub-5503082977524600/2148771632"
}

/**
 * Computes the optimal Google AdMob Anchored Adaptive Banner size
 * for the current device width, maximizing fill rate and preventing letterboxing.
 */
fun getAdaptiveBannerAdSize(context: Context, horizontalPaddingDp: Int = 32): AdSize {
    val displayMetrics = context.resources.displayMetrics
    val density = displayMetrics.density
    val screenWidthDp = (displayMetrics.widthPixels / density).toInt()
    val targetWidthDp = (screenWidthDp - horizontalPaddingDp).coerceAtLeast(300)
    return runCatching {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, targetWidthDp)
    }.getOrDefault(AdSize.BANNER)
}

/**
 * Eagerly pre-warms and loads an AdMob banner view in memory at screen initialization.
 * When the user scrolls to the ad placement, it appears with 0ms latency.
 */
@Composable
fun rememberDutyPePrewarmedAd(
    adUnitId: String = AdMobConfig.BANNER_AD_UNIT_ID,
    adSize: AdSize? = null
): AdView {
    val context = LocalContext.current
    val adView = remember(adUnitId) {
        AdView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            this.adUnitId = adUnitId
            val size = adSize ?: getAdaptiveBannerAdSize(context, 32)
            setAdSize(size)
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) {
        onDispose {
            runCatching { adView.destroy() }
        }
    }
    return adView
}

/**
 * Eagerly pre-warms and loads an AdMob Video Ad (Medium Rectangle 300x250) in memory.
 * Used by JobDescriptionScreen to render rich-media/video ads with 0ms instant display.
 */
@Composable
fun rememberDutyPeVideoAd(
    adUnitId: String = AdMobConfig.EARNINGS_AD_UNIT_ID
): AdView {
    val context = LocalContext.current
    val adView = remember(adUnitId) {
        AdView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            this.adUnitId = adUnitId
            setAdSize(AdSize.MEDIUM_RECTANGLE)
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) {
        onDispose {
            runCatching { adView.destroy() }
        }
    }
    return adView
}

/**
 * 1. Post-Action Real AdMob Card.
 * Displayed on Success screens (Job Application Sent, Service Booking Confirmed).
 * Prioritizes live Google AdMob (Medium Rectangle, fallback to Adaptive Banner).
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
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        this.adUnitId = adUnitId
                        setAdSize(AdSize.MEDIUM_RECTANGLE)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isLoaded = true
                                isFailed = false
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Timber.d("AdMob post-action ad failed with MEDIUM_RECTANGLE: ${error.message}, falling back to adaptive banner")
                                val adaptiveSize = getAdaptiveBannerAdSize(ctx, 32)
                                setAdSize(adaptiveSize)
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        isLoaded = true
                                        isFailed = false
                                    }
                                    override fun onAdFailedToLoad(err: LoadAdError) {
                                        Timber.d("AdMob post-action fallback banner failed: ${err.message}")
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
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        this.adUnitId = adUnitId
                        val adaptiveSize = getAdaptiveBannerAdSize(ctx, 32)
                        setAdSize(adaptiveSize)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isLoaded = true
                                isFailed = false
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Timber.d("AdMob pre-map adaptive banner failed: ${error.message}, falling back to standard BANNER")
                                setAdSize(AdSize.BANNER)
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        isLoaded = true
                                        isFailed = false
                                    }
                                    override fun onAdFailedToLoad(err: LoadAdError) {
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
 * 3. In-Feed Native Job Ad Card.
 * Fits seamlessly inside the worker job list (All Jobs & Worker Home).
 * Clean, native layout without intrusive labels; fast-loading with adaptive sizing and pre-warming.
 */
@Composable
fun DutyPeInFeedAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.BANNER_AD_UNIT_ID,
    prewarmedAdView: AdView? = null
) {
    var isLoaded by remember { mutableStateOf(prewarmedAdView != null) }
    var isFailed by remember { mutableStateOf(false) }

    if (!isFailed) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isLoaded) Modifier.padding(vertical = 4.dp) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (prewarmedAdView != null) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { prewarmedAdView }
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            this.adUnitId = adUnitId
                            val adaptiveSize = getAdaptiveBannerAdSize(ctx, 32)
                            setAdSize(adaptiveSize)
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    isLoaded = true
                                    isFailed = false
                                }
                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    Timber.d("InFeed adaptive ad failed: ${error.message}, falling back to standard BANNER")
                                    setAdSize(AdSize.BANNER)
                                    adListener = object : AdListener() {
                                        override fun onAdLoaded() {
                                            isLoaded = true
                                            isFailed = false
                                        }
                                        override fun onAdFailedToLoad(err: LoadAdError) {
                                            Timber.d("InFeed fallback banner failed: ${err.message}")
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
}

/**
 * 4. Earnings & Referral Ad Banner.
 * Placed in Earnings Dashboard and Refer & Earn screens using AdMob Banner ID.
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
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        this.adUnitId = adUnitId
                        val adaptiveSize = getAdaptiveBannerAdSize(ctx, 32)
                        setAdSize(adaptiveSize)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isLoaded = true
                                isFailed = false
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Timber.d("AdMob earnings banner adaptive failed: ${error.message}, falling back to standard BANNER")
                                setAdSize(AdSize.BANNER)
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        isLoaded = true
                                        isFailed = false
                                    }
                                    override fun onAdFailedToLoad(err: LoadAdError) {
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
 * 5. Video-capable AdMob Card (for Job Description Screen).
 * Loads AdSize.MEDIUM_RECTANGLE (300x250) which plays Google AdMob Video and rich-media ads.
 * Seamless, clean design without sponsor/verified headers; instant rendering via prewarmedAdView.
 */
@Composable
fun DutyPeVideoAdCard(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.EARNINGS_AD_UNIT_ID,
    prewarmedAdView: AdView? = null
) {
    var isLoaded by remember { mutableStateOf(prewarmedAdView != null) }
    var isFailed by remember { mutableStateOf(false) }

    if (!isFailed) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isLoaded) Modifier.padding(vertical = 6.dp) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (prewarmedAdView != null) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    factory = { prewarmedAdView }
                )
            } else {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            this.adUnitId = adUnitId
                            setAdSize(AdSize.MEDIUM_RECTANGLE)
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    isLoaded = true
                                    isFailed = false
                                }
                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    Timber.d("AdMob video medium rectangle failed: ${error.message}, falling back to adaptive banner")
                                    val adaptiveSize = getAdaptiveBannerAdSize(ctx, 32)
                                    setAdSize(adaptiveSize)
                                    adListener = object : AdListener() {
                                        override fun onAdLoaded() {
                                            isLoaded = true
                                            isFailed = false
                                        }
                                        override fun onAdFailedToLoad(err: LoadAdError) {
                                            Timber.d("AdMob video fallback banner failed: ${err.message}")
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
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                this.adUnitId = adUnitId
                setAdSize(adSize)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
