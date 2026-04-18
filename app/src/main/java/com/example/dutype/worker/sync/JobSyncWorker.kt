package com.example.dutype.worker.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.dutype.database.dao.ApplicationDao
import com.example.dutype.performance.PerformanceTracker
import com.example.dutype.repositories.OfflineFirstJobRepository
import com.example.dutype.services.JobApplicationService
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.utils.NetworkUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * JobSyncWorker - Background sync for offline-first architecture
 * 
 * Responsibilities:
 * - Sync jobs from Firestore to Room database
 * - Submit queued applications when online
 * - Clean up expired cache entries
 * - Track sync performance metrics
 * 
 * Scheduling:
 * - Runs every 30 minutes when connected to network
 * - Requires battery not low
 * - Uses expedited work for critical syncs
 *
 * Idempotency contract:
 * - Safe to re-run. All Firestore -> Room writes use `OnConflictStrategy.REPLACE`,
 *   so re-syncing the same documents converges to the same state.
 * - Queued application submissions are guarded by per-application status flags in Room;
 *   a successful submit transitions the row out of the queued state, so retries skip it.
 * - Cache cleanup is a delete-by-timestamp pass that is naturally idempotent.
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
@HiltWorker
class JobSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val offlineFirstJobRepository: OfflineFirstJobRepository,
    private val applicationDao: ApplicationDao,
    private val jobApplicationService: JobApplicationService,
    private val applicationStateManager: ApplicationStateManager,
    private val performanceTracker: PerformanceTracker
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "job_sync_worker"
        private const val SYNC_INTERVAL_MINUTES = 30L
        private const val CACHE_MAX_AGE_HOURS = 24L
        
        /**
         * Schedule periodic job sync
         * Note: Initial delay of 30 seconds to avoid blocking app startup
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<JobSyncWorker>(
                SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(30, TimeUnit.SECONDS) // Delay first run to not block startup
                .addTag("job_sync")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            
            Timber.d("🔄 SYNC: Scheduled periodic job sync every ${SYNC_INTERVAL_MINUTES} minutes")
        }
        
        /**
         * Cancel scheduled sync
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("🔄 SYNC: Cancelled periodic job sync")
        }
    }
    
    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Timber.d("🔄 SYNC: Starting background sync...")
        
        return try {
            var itemsSynced = 0
            
            // Step 1: Sync jobs from Firestore
            val jobsResult = syncJobs()
            if (jobsResult.isSuccess) {
                itemsSynced += jobsResult.getOrDefault(0)
            }
            
            // Step 2: Submit pending applications
            val applicationsResult = syncPendingApplications()
            if (applicationsResult.isSuccess) {
                itemsSynced += applicationsResult.getOrDefault(0)
            }
            
            // Step 3: Clean up old cache
            cleanupCache()
            
            val duration = System.currentTimeMillis() - startTime
            performanceTracker.trackBackgroundSync(
                syncType = "jobs",
                durationMs = duration,
                itemsSynced = itemsSynced,
                success = true
            )
            
            Timber.d("🔄 SYNC: Background sync completed in ${duration}ms, synced $itemsSynced items")
            Result.success()
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            performanceTracker.trackBackgroundSync(
                syncType = "jobs",
                durationMs = duration,
                itemsSynced = 0,
                success = false
            )
            
            Timber.e(e, "🔄 SYNC: Background sync failed")
            
            // Retry on failure (up to 3 times)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    /**
     * Sync jobs from Firestore to Room
     */
    private suspend fun syncJobs(): kotlin.Result<Int> {
        return try {
            val result = offlineFirstJobRepository.forceRefresh(limit = 100)
            result.fold(
                onSuccess = { jobs ->
                    Timber.d("🔄 SYNC: Synced ${jobs.size} jobs from Firestore")
                    kotlin.Result.success(jobs.size)
                },
                onFailure = { error ->
                    Timber.w(error, "🔄 SYNC: Failed to sync jobs")
                    kotlin.Result.failure(error)
                }
            )
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
    
    /**
     * Submit applications that were queued while offline
     */
    private suspend fun syncPendingApplications(): kotlin.Result<Int> {
        return try {
            val pendingApplications = applicationDao.getPendingSubmissions()
            
            if (pendingApplications.isEmpty()) {
                Timber.d("🔄 SYNC: No pending applications to submit")
                return kotlin.Result.success(0)
            }
            
            Timber.d("🔄 SYNC: Found ${pendingApplications.size} pending applications")
            
            var successCount = 0
            for (application in pendingApplications) {
                val result = jobApplicationService.submitApplication(
                    application = application.toJobApplication(),
                    allowOfflineQueue = false
                )

                when {
                    result.isSuccess -> {
                        applicationDao.markAsSynced(application.applicationId)
                        successCount++
                    }

                    result.exceptionOrNull()?.message?.contains("already applied", ignoreCase = true) == true -> {
                        applicationDao.markAsSynced(application.applicationId)
                        successCount++
                    }

                    result.exceptionOrNull()?.let(NetworkUtils::isRetryableError) == true -> {
                        Timber.w(
                            result.exceptionOrNull(),
                            "🔄 SYNC: Keeping pending application ${application.applicationId} for retry"
                        )
                    }

                    else -> {
                        Timber.w(
                            result.exceptionOrNull(),
                            "🔄 SYNC: Dropping terminally invalid application ${application.applicationId}"
                        )
                        applicationDao.deleteApplicationById(application.applicationId)
                        applicationStateManager.removeAppliedJob(application.jobId)
                    }
                }
            }

            Timber.d("🔄 SYNC: Submitted $successCount pending applications")
            kotlin.Result.success(successCount)
            
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
    
    /**
     * Clean up old cache entries
     */
    private suspend fun cleanupCache() {
        try {
            // Delete jobs cached more than 24 hours ago
            val maxAgeMs = CACHE_MAX_AGE_HOURS * 60 * 60 * 1000L
            offlineFirstJobRepository.clearOldCache(maxAgeMs)
            
            // Delete expired jobs
            offlineFirstJobRepository.clearExpiredJobs()
            
            // Delete old application cache
            val applicationMaxAge = System.currentTimeMillis() - maxAgeMs
            applicationDao.deleteOldCache(applicationMaxAge)
            
            Timber.d("🔄 SYNC: Cache cleanup completed")
        } catch (e: Exception) {
            Timber.w(e, "🔄 SYNC: Cache cleanup failed")
        }
    }
}
