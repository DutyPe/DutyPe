package com.example.dutype.promo

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.dutype.utils.AppLifecycleTracker
import com.example.dutype.utils.isDebuggableBuild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background refresh of the launch promo / app-icon config.
 *
 * Plain CoroutineWorker on purpose: it needs no injected dependencies, so it does not force
 * Hilt to build any graph (HiltWorkerFactory falls back to the default factory for it).
 */
class LaunchConfigWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Legacy stub retained solely to avoid ClassNotFoundException for any lingering system jobs.
        // Exits immediately with success so Android never retries or holds SystemJobService locks.
        return Result.success()
    }
}

/**
 * Handles promo and holiday app-icon config refresh.
 * Executed purely in foreground coroutines without scheduling background WorkManager jobs,
 * permanently eliminating SystemJobService wakeups and acknowledgeStartMessage ANRs.
 */
object LaunchConfigScheduler {
    private const val REFRESH_INTERVAL_HOURS = 12L

    /** Call from a background thread shortly after the first frame, when the app is foregrounded. */
    fun onAppForeground(context: Context) {
        val ctx = context.applicationContext
        try {
            // Cancel legacy WorkManager jobs from older versions to purge them from system JobScheduler
            runCatching {
                val wm = WorkManager.getInstance(ctx)
                wm.cancelUniqueWork("launch_config_periodic")
                wm.cancelUniqueWork("launch_config_refresh_once")
            }

            // Opportunistic refresh: at most once per 12h (debug builds: every foreground).
            val staleMs = if (ctx.isDebuggableBuild()) 0L else TimeUnit.HOURS.toMillis(REFRESH_INTERVAL_HOURS)
            val age = System.currentTimeMillis() - LaunchConfigStore.lastFetchAt(ctx)
            if (age >= staleMs) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val refreshed = LaunchConfigFetcher.refresh(ctx)
                    if (refreshed && !AppLifecycleTracker.isAppInForeground()) {
                        AppIconManager.reconcile(ctx)
                    }
                    Timber.tag("PromoConfig").d("Foreground promo refresh completed (age=%d ms)", age)
                }
            }
        } catch (t: Throwable) {
            Timber.tag("PromoConfig").d("Scheduling failed: %s", t.javaClass.simpleName)
        }
    }
}
