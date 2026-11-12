package com.example.dutype.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import timber.log.Timber

/**
 * Production-safe navigation extensions for Jetpack Navigation.
 *
 * Protects against:
 * 1. java.lang.IllegalArgumentException: Destination/route or popUpTo target not found in graph
 * 2. Lifecycle races where navigation is invoked when lifecycle is DESTROYED or not STARTED
 * 3. Crashes in background / coroutine invokeSuspend (e.g., cold start deep links / FCM)
 */
fun NavController.safeNavigate(
    route: String,
    builder: (NavOptionsBuilder.() -> Unit)? = null
): Boolean {
    return try {
        val currentState = currentBackStackEntry?.lifecycle?.currentState
        if (currentState != null && !currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Timber.w("safeNavigate: navigation to '$route' deferred or skipped; lifecycle is $currentState")
            return false
        }
        if (builder != null) {
            navigate(route, builder)
        } else {
            navigate(route)
        }
        true
    } catch (e: IllegalArgumentException) {
        Timber.e(e, "safeNavigate: IllegalArgumentException navigating to '$route'")
        false
    } catch (e: Exception) {
        Timber.e(e, "safeNavigate: unexpected error navigating to '$route'")
        false
    }
}
