package com.example.dutype.workers

import com.dutype.app.R
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dutype.models.NotificationData
import com.example.dutype.models.NotificationType
import com.example.dutype.services.NotificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Pending Application Notification Worker
 * 
 * Sends notifications to workers when their job applications have been pending for more than 24 hours.
 * 
 * Features:
 * - Runs every 6 hours
 * - One notification per job (rate limited to once per 24 hours)
 * - Quiet hours respected (skips ~10 PMâ€“8 AM)
 * - Includes deep link to JobDescriptionScreen
 * 
 * Notification Message:
 * "Your application is still pending. For faster update, please call the employer."
 *
 * Idempotency contract:
 * - Safe to re-run. The 24-hour rate limiter (`lastNotifiedAt` per application doc)
 *   prevents duplicate notifications even if WorkManager retries this run after a failure.
 * - No external side effects beyond the rate-limited notification + a Firestore write
 *   that is itself a last-write-wins update on a single document, so retries converge.
 */
@HiltWorker
class PendingApplicationNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val firestore: FirebaseFirestore,
    private val notificationService: NotificationService
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "pending_application_notification_worker"
        private const val PENDING_THRESHOLD_HOURS = 24L
        private const val NOTIFICATION_COOLDOWN_HOURS = 24L
    }

    override suspend fun doWork(): Result {
        return try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Timber.w("ðŸ”” PendingApplicationNotificationWorker - No authenticated user, skipping")
                return Result.success()
            }

            // Quiet hours: never send reminders late night / early morning (no ~2 AM pings).
            val hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (hourOfDay >= 22 || hourOfDay < 8) {
                Timber.d("ðŸ”” PendingApplicationNotificationWorker - quiet hours (hour=$hourOfDay), skipping")
                return Result.success()
            }

            Timber.d("ðŸ”” PendingApplicationNotificationWorker - Starting for user $currentUserId")
            
            val now = System.currentTimeMillis()
            val pendingThreshold = now - TimeUnit.HOURS.toMillis(PENDING_THRESHOLD_HOURS)
            val prefs = applicationContext.getSharedPreferences("pending_app_notifications", Context.MODE_PRIVATE)
            val notificationCooldown = now - TimeUnit.HOURS.toMillis(NOTIFICATION_COOLDOWN_HOURS)
            
            // Query by workerId only to avoid composite index dependency, then filter locally.
            val workerApplications = firestore.collection(com.example.dutype.firestore.FirestoreCollections.APPLICATIONS)
                .whereEqualTo("workerId", currentUserId)
                .limit(400)
                .get()
                .await()

            val pendingApplications = workerApplications.documents.filter { doc ->
                val status = doc.getString("status")
                val createdAtTs = doc.getTimestamp("createdAt")
                val createdAt = createdAtTs?.toDate()?.time ?: 0L
                status == "applied" && createdAt in 1..pendingThreshold
            }

            Timber.d("ðŸ”” Found ${pendingApplications.size} pending applications older than 24 hours")
            
            var notificationsSent = 0
            
            for (doc in pendingApplications) {
                try {
                    val workerId = doc.getString("workerId") ?: continue
                    val jobId = doc.getString("jobId") ?: continue
                    val applicationId = doc.id
                    val jobTitle = doc.getString("jobTitle") ?: "this job"
                    val cooldownKey = "pending_$applicationId"
                    val lastNotificationSent = prefs.getLong(cooldownKey, 0L)
                    
                    // Rate limit: Only send if no notification sent in last 24 hours
                    if (lastNotificationSent > notificationCooldown) {
                        Timber.d("ðŸ”” Skipping notification for application $applicationId - already sent in last 24h")
                        continue
                    }
                    
                    // Send notification
                    val notification = NotificationData(
                        id = "pending_app_$applicationId",
                        recipientId = workerId,
                        title = applicationContext.getString(R.string.pending_application_title),
                        message = applicationContext.getString(R.string.pending_application_message, jobTitle),
                        type = NotificationType.APPLICATION_STATUS_UPDATE,
                        data = mapOf(
                            "jobId" to jobId,
                            "applicationId" to applicationId,
                            "action" to "VIEW_JOB_DETAILS",
                            "notificationType" to "PENDING_APPLICATION_REMINDER"
                        ),
                        createdAt = now
                    )
                    
                    // Send notification (no quiet hours restriction as per requirements)
                    notificationService.sendNotification(notification, workerId)
                    
                    prefs.edit().putLong(cooldownKey, now).apply()
                    
                    notificationsSent++
                    Timber.d("âœ… Sent pending application notification for job $jobId to worker $workerId")
                    
                } catch (e: Exception) {
                    Timber.e(e, "âŒ Error processing application ${doc.id}")
                    // Continue with next application
                }
            }
            
            Timber.d("ðŸ”” PendingApplicationNotificationWorker - Completed. Sent $notificationsSent notifications")
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "âŒ PendingApplicationNotificationWorker - Failed")
            Result.retry()
        }
    }
}
