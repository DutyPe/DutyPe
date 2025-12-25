package com.example.dutype.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

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
    // Ads are disabled for testing. Immediately call the dismissal callback
    LaunchedEffect(Unit) {
        onAdShown()
        onAdFailed("Ads disabled for testing")
        onAdDismissed()
    }
}
