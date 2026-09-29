package com.example.dutype.utils

import android.content.Context
import com.example.dutype.promo.AppIconManager

/**
 * Deprecated shim kept for source compatibility. Icon switching is now driven by
 * [com.example.dutype.promo.AppIconManager] from the cached, versioned launch config
 * (schedule window, version targeting, kill switch and auto-revert included).
 */
@Deprecated("Use com.example.dutype.promo.AppIconManager")
object DynamicIconManager {
    /** No-op: the desired icon is computed from the cached launch config, not queued from the UI. */
    @Suppress("UNUSED_PARAMETER")
    fun queueIconSwitch(context: Context, iconName: String) = Unit

    fun applyPendingIconSwitch(context: Context) = AppIconManager.reconcileAsync(context)
}
