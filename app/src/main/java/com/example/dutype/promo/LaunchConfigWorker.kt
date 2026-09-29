package com.example.dutype.promo

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.dutype.utils.AppLifecycleTracker
import com.example.dutype.utils.isDebuggableBuild
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
        return try {
            LaunchConfigFetcher.refresh(applicationContext)
            // Expiry/kill-switch revert must not depend on the user opening the app: if the app
            // is not in the foreground, reconcile the launcher icon now (no-op when unchanged).
            if (!AppLifecycleTracker.isAppInForeground()) {
                AppIconManager.reconcile(applicationContext)
            }
            Result.success()
        } catch (t: Throwable) {
            Timber.tag("PromoConfig").d("Worker failed: %s", t.javaClass.simpleName)
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }
}

/** Schedules the config refresh. Cheap and idempotent; never call before the first frame. */
object LaunchConfigScheduler {
    private const val PERIODIC_NAME = "launch_config_periodic"
    private const val ONCE_NAME = "launch_config_refresh_once"
    private const val REFRESH_INTERVAL_HOURS = 12L

    @Volatile private var periodicScheduled = false

    /** Call from a background thread shortly after the first frame, when the app is foregrounded. */
    fun onAppForeground(context: Context) {
        val ctx = context.applicationContext
        try {
            val wm = WorkManager.getInstance(ctx)
            if (!periodicScheduled) {
                val request = PeriodicWorkRequestBuilder<LaunchConfigWorker>(
                    REFRESH_INTERVAL_HOURS, TimeUnit.HOURS
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                    .build()
                wm.enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
                periodicScheduled = true
            }
            // Opportunistic refresh: at most once per 12h (debug builds: every foreground).
            val staleMs = if (ctx.isDebuggableBuild()) 0L else TimeUnit.HOURS.toMillis(REFRESH_INTERVAL_HOURS)
            val age = System.currentTimeMillis() - LaunchConfigStore.lastFetchAt(ctx)
            if (age >= staleMs) {
                val once = OneTimeWorkRequestBuilder<LaunchConfigWorker>()
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                    )
                    .build()
                wm.enqueueUniqueWork(ONCE_NAME, ExistingWorkPolicy.KEEP, once)
                Timber.tag("PromoConfig").d("Enqueued opportunistic refresh (age=%d ms)", age)
            }
        } catch (t: Throwable) {
            Timber.tag("PromoConfig").d("Scheduling failed: %s", t.javaClass.simpleName)
        }
    }
}
