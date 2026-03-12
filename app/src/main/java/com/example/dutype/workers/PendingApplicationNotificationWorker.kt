package com.example.dutype.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dutype.models.ApplicationStatus
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
 * - No quiet hours restriction (as per requirements)
 * - Includes deep link to JobDescriptionScreen
 * 
 * Notification Message:
 * "Your application is still pending. For faster update, please call the employer."
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
                Timber.w("🔔 PendingApplicationNotificationWorker - No authenticated user, skipping")
                return Result.success()
            }
            
            Timber.d("🔔 PendingApplicationNotificationWorker - Starting for user $currentUserId")
            
            val now = System.currentTimeMillis()
            val pendingThreshold = now - TimeUnit.HOURS.toMillis(PENDING_THRESHOLD_HOURS)
            val notificationCooldown = now - TimeUnit.HOURS.toMillis(NOTIFICATION_COOLDOWN_HOURS)
            
            // Query only the CURRENT user's pending applications (not all users)
            val pendingApplications = firestore.collection("job_applications")
                .whereEqualTo("workerId", currentUserId)
                .whereEqualTo("status", ApplicationStatus.PENDING.name)
                .whereEqualTo("active", true)
                .whereLessThan("appliedAt", pendingThreshold)
                .limit(200)
                .get()
                .await()
            
            Timber.d("🔔 Found ${pendingApplications.size()} pending applications older than 24 hours")
            
            var notificationsSent = 0
            
            for (doc in pendingApplications.documents) {
                try {
                    val workerId = doc.getString("workerId") ?: continue
                    val jobId = doc.getString("jobId") ?: continue
                    val applicationId = doc.id
                    val jobTitle = doc.getString("jobTitle") ?: "this job"
                    val companyName = doc.getString("companyName") ?: "the employer"
                    val lastNotificationSent = doc.getLong("lastPendingNotificationSent") ?: 0L
                    
                    // Rate limit: Only send if no notification sent in last 24 hours
                    if (lastNotificationSent > notificationCooldown) {
                        Timber.d("🔔 Skipping notification for application $applicationId - already sent in last 24h")
                        continue
                    }
                    
                    // Send notification
                    val notification = NotificationData(
                        id = "pending_app_$applicationId",
                        recipientId = workerId,
                        title = "Application Still Pending",
                        message = "Your application for $jobTitle at $companyName is still pending. For faster update, please call the employer.",
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
                    
                    // Update lastPendingNotificationSent timestamp
                    firestore.collection("job_applications")
                        .document(applicationId)
                        .update("lastPendingNotificationSent", now)
                        .await()
                    
                    notificationsSent++
                    Timber.d("✅ Sent pending application notification for job $jobId to worker $workerId")
                    
                } catch (e: Exception) {
                    Timber.e(e, "❌ Error processing application ${doc.id}")
                    // Continue with next application
                }
            }
            
            Timber.d("🔔 PendingApplicationNotificationWorker - Completed. Sent $notificationsSent notifications")
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "❌ PendingApplicationNotificationWorker - Failed")
            Result.retry()
        }
    }
}
