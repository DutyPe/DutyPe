package com.example.dutype.components

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.dutype.ads.InterstitialAdManager

/**
 * Composable that loads and displays an Interstitial Ad
 * Call this when you want to show an ad to the user
 * 
 * Usage:
 * if (showAd) {
 *     AdInterstitial(
 *         onAdDismissed = {
 *             showAd = false
 *             // Do navigation or other actions here
 *         }
 *     )
 * }
 */
@Composable
fun AdInterstitial(
    onAdDismissed: () -> Unit,
    onAdShown: () -> Unit = {},
    onAdFailed: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Create ad manager instance
    val adManager = remember { InterstitialAdManager(context) }
    
    // Load and show ad
    LaunchedEffect(Unit) {
        if (activity != null) {
            // Load ad first
            adManager.loadInterstitialAd(
                onAdLoaded = {
                    println("🎬 Ad loaded, now showing to user")
                    onAdShown()
                    // Show the ad once loaded
                    adManager.showInterstitialAd(
                        activity = activity,
                        onAdDismissed = {
                            println("🔄 Ad dismissed, calling callback")
                            onAdDismissed()
                        }
                    )
                },
                onAdFailed = { errorMsg ->
                    println("❌ Ad failed to load: $errorMsg")
                    onAdFailed(errorMsg)
                    // If ad fails, still allow user to proceed
                    onAdDismissed()
                }
            )
        } else {
            println("❌ Activity context not available")
            onAdDismissed()
        }
    }
}
