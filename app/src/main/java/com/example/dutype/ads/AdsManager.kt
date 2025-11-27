package com.example.dutype.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds

/**
 * Manages Google Mobile Ads SDK initialization
 */
object AdsManager {
    fun initializeMobileAds(context: Context) {
        MobileAds.initialize(context)
        println("✅ Google Mobile Ads SDK initialized")
    }
}
