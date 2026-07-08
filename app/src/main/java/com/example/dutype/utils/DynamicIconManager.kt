package com.example.dutype.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber

object DynamicIconManager {
    private const val PREFS_NAME = "dynamic_icon_prefs"
    private const val KEY_PENDING_ICON = "pending_icon"
    private const val KEY_ACTIVE_ICON = "active_icon"

    private val aliases = listOf(
        "com.example.dutype.DefaultAlias",
        "com.example.dutype.BirthdayAlias",
        "com.example.dutype.IndependenceAlias"
    )

    /**
     * Queues the active icon name change in SharedPreferences.
     * We do not swap immediately in the foreground to prevent the Android OS from
     * abruptly killing the active app process and interrupting user interaction.
     */
    fun queueIconSwitch(context: Context, iconName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeIcon = prefs.getString(KEY_ACTIVE_ICON, "default") ?: "default"
        
        if (activeIcon.equals(iconName, ignoreCase = true)) {
            // Already active, clear any pending updates
            prefs.edit().remove(KEY_PENDING_ICON).apply()
            return
        }
        
        Timber.d("DynamicIconManager: Queueing launcher icon switch to %s", iconName)
        prefs.edit().putString(KEY_PENDING_ICON, iconName.lowercase().trim()).apply()
    }

    /**
     * Executes the pending launcher icon switch when the app goes to the background.
     * This avoids process interruption during active usage.
     */
    fun applyPendingIconSwitch(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pendingIcon = prefs.getString(KEY_PENDING_ICON, null) ?: return
        
        val targetAlias = when (pendingIcon) {
            "birthday" -> "com.example.dutype.BirthdayAlias"
            "independence" -> "com.example.dutype.IndependenceAlias"
            else -> "com.example.dutype.DefaultAlias"
        }

        val packageManager = context.packageManager
        
        // Safety: check which alias is currently enabled
        val currentEnabled = aliases.firstOrNull { aliasName ->
            val comp = ComponentName(context, aliasName)
            val state = packageManager.getComponentEnabledSetting(comp)
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } ?: "com.example.dutype.DefaultAlias"

        if (currentEnabled == targetAlias) {
            Timber.d("DynamicIconManager: Active launcher icon is already %s. Clearing queue.", pendingIcon)
            prefs.edit().putString(KEY_ACTIVE_ICON, pendingIcon).remove(KEY_PENDING_ICON).apply()
            return
        }

        Timber.i("DynamicIconManager: Executing background icon switch from %s to %s", currentEnabled, targetAlias)

        // Enable target launcher first
        packageManager.setComponentEnabledSetting(
            ComponentName(context, targetAlias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Disable all other launchers to ensure only a single active launcher icon is present
        aliases.filter { it != targetAlias }.forEach { aliasName ->
            packageManager.setComponentEnabledSetting(
                ComponentName(context, aliasName),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Commit status
        prefs.edit()
            .putString(KEY_ACTIVE_ICON, pendingIcon)
            .remove(KEY_PENDING_ICON)
            .apply()
    }
}
