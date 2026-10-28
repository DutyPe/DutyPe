package com.example.dutype.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import timber.log.Timber

/**
 * Enterprise OEM & Vivo Power Management Helper
 *
 * Vivo devices running Funtouch OS / OriginOS apply aggressive background restrictions:
 * 1. Suspending background threads and blocking file I/O when locked or in low-power mode.
 * 2. Restricting socket/IPC and waking services without allowing credential-protected storage access.
 * 3. Enforcing "High Background Power Consumption" whitelists for instant notification delivery.
 */
object VivoPowerManagerHelper {

    fun isVivoDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        return manufacturer.contains("vivo", ignoreCase = true) ||
            brand.contains("vivo", ignoreCase = true) ||
            brand.contains("iqoo", ignoreCase = true)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } catch (e: Exception) {
            Timber.w(e, "VivoPowerManagerHelper: Failed to check battery optimizations")
            true
        }
    }

    /**
     * Attempts to open Vivo's proprietary background power consumption / auto-start settings,
     * falling back to standard Android battery optimization settings.
     */
    fun openVivoBackgroundPowerSettings(context: Context): Boolean {
        val intents = listOf(
            // Vivo / iQOO High Background Power Consumption intent
            Intent().setComponent(
                ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            ),
            Intent().setComponent(
                ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")
            ),
            Intent().setComponent(
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")
            ),
            Intent().setComponent(
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity")
            ),
            // Generic Android Battery Optimization settings
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                Timber.w(e, "VivoPowerManagerHelper: Failed intent resolution for ${intent.component}")
            }
        }
        return false
    }
}
