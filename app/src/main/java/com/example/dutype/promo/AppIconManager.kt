package com.example.dutype.promo

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.dutype.utils.appVersionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Launcher-icon switching via `<activity-alias>` + PackageManager.setComponentEnabledSetting
 * (the approach recommended for Android).
 *
 * Rules:
 *  - Exactly ONE launcher alias is enabled at any time (target is enabled before the others are
 *    disabled, so there is never a moment without a launcher entry).
 *  - Idempotent: nothing is touched when the desired icon is already the only enabled one.
 *  - Runs ONLY when the app is in the background (MainActivity.onStop / the WorkManager job when
 *    the app is not foregrounded). Changing components while the user is in the app can close the
 *    app or flicker on some launchers/OEMs, even with DONT_KILL_APP.
 *  - Never on the main thread.
 *  - Switching TO a festival icon is throttled to once per 24h; reverting to the default icon is
 *    never throttled (expiry / kill switch must always win).
 *  - Fail-safe: any error leaves the current icon untouched.
 *
 * To add a variant: add an `<activity-alias>` in AndroidManifest.xml (enabled="false", MAIN/LAUNCHER
 * filter, its own icon/roundIcon), add ONE line to [ALIASES], ship the app, then set
 * `appIcon.activeIconId` in the config.
 */
object AppIconManager {
    private const val TAG = "PromoConfig"
    private const val PREFS = "app_icon_state_v1"
    private const val KEY_LAST_APPLY_AT = "last_apply_at"
    const val DEFAULT_ID = "default"
    private val THROTTLE_MS = TimeUnit.HOURS.toMillis(24)

    /** iconId -> alias class (must match the manifest). Keep "default" first. */
    private val ALIASES: LinkedHashMap<String, String> = linkedMapOf(
        DEFAULT_ID to "com.example.dutype.DefaultAlias",
        "birthday" to "com.example.dutype.BirthdayAlias",
        "independence" to "com.example.dutype.IndependenceAlias"
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    /** Fire-and-forget; safe to call from onStop (does the work on an IO thread). */
    fun reconcileAsync(context: Context) {
        val app = context.applicationContext
        scope.launch { reconcile(app) }
    }

    /** Computes the desired icon from the cached config and applies it. Call off the main thread. */
    fun reconcile(context: Context): Boolean {
        return try {
            val ctx = context.applicationContext
            val desired = desiredIconId(LaunchConfigStore.current(ctx), System.currentTimeMillis(), ctx.appVersionInfo().code)
            apply(ctx, desired)
        } catch (t: Throwable) {
            Timber.tag(TAG).d("Icon reconcile failed (%s); keeping current icon", t.javaClass.simpleName)
            false
        }
    }

    /**
     * Pure decision: which icon should be showing now. Disabled / absent / expired / out-of-version
     * config all resolve to the default icon.
     */
    fun desiredIconId(config: LaunchConfig?, now: Long, appVersion: Long): String {
        val icon = config?.appIcon
        if (config == null || !config.enabled || icon == null || !icon.enabled) return DEFAULT_ID
        if (icon.startAt > 0L && now < icon.startAt) return DEFAULT_ID
        if (icon.endAt > 0L && now >= icon.endAt) return DEFAULT_ID
        if (icon.minAppVersion > 0L && appVersion < icon.minAppVersion) return DEFAULT_ID
        if (icon.maxAppVersion > 0L && appVersion > icon.maxAppVersion) return DEFAULT_ID
        return icon.activeIconId.ifBlank { DEFAULT_ID }
    }

    /** Enables [iconId]'s alias and disables the rest, only when needed. Returns true if in the desired state. */
    fun apply(context: Context, iconId: String): Boolean {
        val ctx = context.applicationContext
        val targetAlias = ALIASES[iconId.trim().lowercase()]
        if (targetAlias == null) {
            // Unknown id (e.g. config newer than this build): keep whatever is showing.
            Timber.tag(TAG).d("Unknown icon id '%s'; keeping current icon", iconId)
            return false
        }
        return try {
            synchronized(lock) {
                val pm = ctx.packageManager
                val enabled = ALIASES.filter { (id, cls) -> isEnabled(pm, ctx, id, cls) }.values.toList()
                if (enabled.size == 1 && enabled[0] == targetAlias) return@synchronized true

                val toDefault = targetAlias == ALIASES[DEFAULT_ID]
                val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val now = System.currentTimeMillis()
                if (!toDefault && now - prefs.getLong(KEY_LAST_APPLY_AT, 0L) < THROTTLE_MS) {
                    Timber.tag(TAG).d("Icon switch to '%s' throttled (24h)", iconId)
                    return@synchronized false
                }

                Timber.tag(TAG).d("Switching launcher icon %s -> %s", enabled, iconId)
                // Enable first, then disable: there is always at least one launcher entry.
                pm.setComponentEnabledSetting(
                    ComponentName(ctx, targetAlias),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                ALIASES.values.filter { it != targetAlias }.forEach { cls ->
                    pm.setComponentEnabledSetting(
                        ComponentName(ctx, cls),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
                prefs.edit().putLong(KEY_LAST_APPLY_AT, now).apply()
                true
            }
        } catch (t: Throwable) {
            Timber.tag(TAG).d("Icon apply failed (%s); keeping current icon", t.javaClass.simpleName)
            false
        }
    }

    private fun isEnabled(pm: PackageManager, ctx: Context, id: String, cls: String): Boolean {
        val state = pm.getComponentEnabledSetting(ComponentName(ctx, cls))
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            // DEFAULT = the manifest value, and only the default alias is enabled in the manifest.
            (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && id == DEFAULT_ID)
    }
}
