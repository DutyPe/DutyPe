package com.example.dutype.promo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.example.dutype.components.LaunchPromoBanner
import com.example.dutype.components.LaunchPromoScreen
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.appVersionInfo
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Delay after the home screen is on screen before a promo may appear. */
private const val PROMO_DELAY_MS = 800L

/**
 * Hosts the launch promo overlay. Everything is served from the local cache:
 *  - it does nothing until the user is on a home screen (never onboarding / login / OTP / setup),
 *  - it waits [PROMO_DELAY_MS] after that, so the first screen is never delayed,
 *  - selection runs on an IO thread against the cached config + local frequency history,
 *  - a promo whose image is not yet in the disk cache is skipped for this launch,
 *  - at most one promo per app process.
 */
@Composable
fun PromoOverlayHost(navController: NavHostController) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val entry by navController.currentBackStackEntryFlow.collectAsState(initial = null)
    val role = roleForRoute(entry?.destination?.route)
    var promo by remember { mutableStateOf<ResolvedPromo?>(null) }

    LaunchedEffect(role) {
        if (role == null || promo != null || LaunchConfigStore.promoShownThisSession) return@LaunchedEffect
        delay(PROMO_DELAY_MS)
        val picked = withContext(Dispatchers.IO) { pickPromo(appContext, role) }
        if (picked != null && !LaunchConfigStore.promoShownThisSession) promo = picked
    }

    val current = promo ?: return

    val onShown = {
        LaunchConfigStore.promoShownThisSession = true
        LaunchConfigStore.recordShown(appContext, current.id)
        logEvent(appContext, "promo_impression", current.id)
    }
    val onClose: (Boolean) -> Unit = { userAction ->
        if (userAction) {
            LaunchConfigStore.recordDismissed(appContext, current.id)
            logEvent(appContext, "promo_dismiss", current.id)
        }
        promo = null
    }
    val onCta = {
        logEvent(appContext, "promo_click", current.id)
        promo = null
        openTarget(context, navController, current.deepLink)
    }

    if (current.isFullscreen) {
        LaunchPromoScreen(promo = current, onShown = onShown, onClose = onClose, onCta = onCta)
    } else {
        LaunchPromoBanner(promo = current, onShown = onShown, onClose = onClose, onCta = onCta)
    }
}

/** Only fully set-up home screens qualify; every auth/onboarding/setup route returns null. */
private fun roleForRoute(route: String?): String? = when (route) {
    Routes.WORKER_HOME -> "worker"
    Routes.EMPLOYER_HOME -> "employer"
    else -> null
}

private fun pickPromo(ctx: Context, role: String): ResolvedPromo? {
    return try {
        val config = LaunchConfigStore.current(ctx)
        val result = PromoSelector.select(
            config = config,
            now = System.currentTimeMillis(),
            role = role,
            language = LocaleHelper.getLanguage(ctx),
            appVersion = ctx.appVersionInfo().code,
            history = LaunchConfigStore.history(ctx),
            isImageReady = { url -> LaunchConfigFetcher.isImageCached(ctx, url) }
        )
        Timber.tag("PromoConfig").d(
            "select role=%s config=%s -> %s",
            role, config?.let { "v${it.version}/enabled=${it.enabled}/${it.promos.size}" }, result?.id
        )
        result
    } catch (t: Throwable) {
        null
    }
}

private fun openTarget(context: Context, navController: NavHostController, target: String) {
    if (target.isBlank()) return
    try {
        if (target.contains("://")) {
            // dutype:// and https://dutype.in links are handled by MainActivity's existing deep-link path.
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).setPackage(context.packageName)
            try {
                context.startActivity(intent)
            } catch (_: Throwable) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
            }
        } else {
            navController.navigate(target)
        }
    } catch (t: Throwable) {
        Timber.tag("PromoConfig").d("CTA navigation failed: %s", t.javaClass.simpleName)
    }
}

private fun logEvent(ctx: Context, name: String, promoId: String) {
    Timber.tag("PromoConfig").d("%s id=%s", name, promoId)
    try {
        FirebaseAnalytics.getInstance(ctx).logEvent(name, Bundle().apply { putString("promo_id", promoId) })
    } catch (_: Throwable) {
    }
}
