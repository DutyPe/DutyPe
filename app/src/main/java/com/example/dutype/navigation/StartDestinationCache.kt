package com.example.dutype.navigation

import android.content.Context
import android.content.SharedPreferences

/**
 * Synchronous on-disk cache of the last-resolved app start destination.
 *
 * The canonical `(authState, role, profileComplete)` lives in DataStore via
 * `ProfileSetupStateManager`, but DataStore reads are suspending. To draw the
 * right screen on the very first frame after a cold start we need a value
 * that can be read synchronously from `MainNavGraph`'s composition.
 *
 * Strategy:
 * - Whenever the async resolver in `MainNavGraph` (or auth/role-switch flows)
 *   determines the correct start destination, it calls [save].
 * - On the next cold start, [read] returns that value immediately, so the
 *   `NavHost` is built with the correct `startDestination` without waiting
 *   for DataStore + Firestore round-trips.
 * - The async resolver still runs and reconciles asynchronously after first
 *   frame — if reality differs (e.g. user logged out from another device), it
 *   navigates to the corrected destination.
 *
 * Backed by `SharedPreferences` (commit-mode) because the API is fully
 * synchronous and the payload is a single string.
 */
object StartDestinationCache {
    private const val PREFS_NAME = "start_destination_cache"
    private const val KEY_ROUTE = "route"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the cached route, or `null` if no cache yet. */
    fun read(context: Context): String? = prefs(context).getString(KEY_ROUTE, null)

    /** Persists the resolved start destination route synchronously. */
    fun save(context: Context, route: String) {
        prefs(context).edit().putString(KEY_ROUTE, route).apply()
    }

    /** Wipes the cache (e.g. on logout, account switch). */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ROUTE).apply()
    }
}
