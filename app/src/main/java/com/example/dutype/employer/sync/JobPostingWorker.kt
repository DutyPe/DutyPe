package com.example.dutype.employer.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.services.NotificationService
import com.example.dutype.utils.RetryUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * JobPostingWorker - Background worker for offline job posting support
 * 
 * P0 FIX: Enables job posting even when offline by queuing submissions
 * and automatically retrying when network becomes available.
 * 
 * Features:
 * - Queues job postings when offline
 * - Automatic retry with exponential backoff
 * - Idempotency key prevents duplicate submissions
 * - Sends notification on successful post
 * 
 * @author DutyPe Engineering Team
 * @since 2.3.0
 */
@HiltWorker
class JobPostingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val firestoreJobRepository: FirestoreJobRepository,
    private val notificationService: NotificationService
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME_PREFIX = "job_posting_"
        const val KEY_JOB_DATA = "job_data"
        const val KEY_EMPLOYER_ID = "employer_id"
        const val KEY_IDEMPOTENCY_KEY = "idempotency_key"
        
        private const val MAX_RETRIES = 3
        private val gson = Gson()
        
        /**
         * Queue a job posting for background submission
         * 
         * @param context Application context
         * @param jobData Map of job data to submit
         * @param employerId Employer's user ID
         * @param idempotencyKey Unique key to prevent duplicate submissions
         */
        fun enqueue(
            context: Context,
            jobData: Map<String, Any>,
            employerId: String,
            idempotencyKey: String
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            // Serialize job data to JSON
            val jobDataJson = gson.toJson(jobData)
            
            val inputData = Data.Builder()
                .putString(KEY_JOB_DATA, jobDataJson)
                .putString(KEY_EMPLOYER_ID, employerId)
                .putString(KEY_IDEMPOTENCY_KEY, idempotencyKey)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<JobPostingWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .addTag("job_posting")
                .addTag(idempotencyKey)
                .build()
            
            // Use idempotency key as unique work name to prevent duplicates
            val workName = "$WORK_NAME_PREFIX$idempotencyKey"
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.KEEP, // Keep existing work, don't replace
                workRequest
            )
            
            Timber.d("📤 JOB_POSTING_WORKER: Queued job posting with idempotency key: $idempotencyKey")
        }
        
        /**
         * Cancel a pending job posting
         */
        fun cancel(context: Context, idempotencyKey: String) {
            val workName = "$WORK_NAME_PREFIX$idempotencyKey"
            WorkManager.getInstance(context).cancelUniqueWork(workName)
            Timber.d("📤 JOB_POSTING_WORKER: Cancelled job posting: $idempotencyKey")
        }
        
        /**
         * Check if a job posting is pending
         */
        fun isPending(context: Context, idempotencyKey: String): Boolean {
            val workName = "$WORK_NAME_PREFIX$idempotencyKey"
            val workInfo = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(workName)
                .get()
            return workInfo.any { !it.state.isFinished }
        }
    }
    
    override suspend fun doWork(): Result {
        val jobDataJson = inputData.getString(KEY_JOB_DATA)
        val employerId = inputData.getString(KEY_EMPLOYER_ID)
        val idempotencyKey = inputData.getString(KEY_IDEMPOTENCY_KEY)
        
        if (jobDataJson.isNullOrBlank() || employerId.isNullOrBlank() || idempotencyKey.isNullOrBlank()) {
            Timber.e("📤 JOB_POSTING_WORKER: Missing required input data")
            return Result.failure()
        }
        
        Timber.d("📤 JOB_POSTING_WORKER: Starting job submission (attempt ${runAttemptCount + 1})")
        Timber.d("📤 JOB_POSTING_WORKER: Idempotency key: $idempotencyKey")
        
        return try {
            // Deserialize job data
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val jobData: Map<String, Any> = gson.fromJson(jobDataJson, type)
            
            // Add employer ID to job data
            val jobDataWithEmployer = jobData.toMutableMap()
            jobDataWithEmployer["employerId"] = employerId
            
            // Submit job with retry logic
            val result = RetryUtils.retryWithBackoffResult(
                maxRetries = MAX_RETRIES,
                initialDelay = 1000L,
                maxDelay = 10000L
            ) {
                submitJob(jobDataWithEmployer)
            }
            
            result.fold(
                onSuccess = { jobId ->
                    Timber.i("📤 JOB_POSTING_WORKER: ✅ Job posted successfully! ID: $jobId")
                    
                    // REMOVED: Notification sending moved to FirestoreJobRepository.createJob()
                    // to prevent duplicate notifications from multiple code paths
                    
                    Result.success(
                        Data.Builder()
                            .putString("job_id", jobId)
                            .build()
                    )
                },
                onFailure = { error ->
                    Timber.e(error, "📤 JOB_POSTING_WORKER: ❌ Job posting failed")
                    
                    // Retry if we haven't exceeded max attempts
                    if (runAttemptCount < MAX_RETRIES) {
                        Timber.d("📤 JOB_POSTING_WORKER: Will retry (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
                        Result.retry()
                    } else {
                        Timber.e("📤 JOB_POSTING_WORKER: Max retries exceeded, giving up")
                        Result.failure(
                            Data.Builder()
                                .putString("error", error.message)
                                .build()
                        )
                    }
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "📤 JOB_POSTING_WORKER: Exception during job submission")
            
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure(
                    Data.Builder()
                        .putString("error", e.message)
                        .build()
                )
            }
        }
    }
    
    /**
     * Submit job to Firestore
     */
    private suspend fun submitJob(jobData: Map<String, Any>): kotlin.Result<String> {
        return try {
            var jobId: String? = null
            var error: Throwable? = null
            
            firestoreJobRepository.createJob(jobData).collect { result ->
                result.fold(
                    onSuccess = { id -> jobId = id },
                    onFailure = { e -> error = e }
                )
            }
            
            if (jobId != null) {
                kotlin.Result.success(jobId!!)
            } else {
                kotlin.Result.failure(error ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}
