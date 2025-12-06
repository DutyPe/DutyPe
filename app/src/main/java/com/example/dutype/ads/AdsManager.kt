package com.example.dutype.ads

import android.content.Context

/**
 * Manages Google Mobile Ads SDK initialization
 */
object AdsManager {
    fun initializeMobileAds(context: Context) {
        // MobileAds initialization disabled for testing
        // MobileAds.initialize(context)
        timber.log.Timber.i("Google Mobile Ads SDK initialization disabled (testing)")
    }
}
