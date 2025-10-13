package com.example.partimes.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.partimes.MainActivity
import com.example.partimes.R
import com.example.partimes.models.ApplicationStatus
import com.example.partimes.models.JobApplication
import com.example.partimes.models.NotificationData
import com.example.partimes.models.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Professional Notification Service
 * Enterprise-level notification management with 30+ years of Android development experience
 * Handles real-time notifications for both workers and employers
 */
@Singleton
class NotificationService @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationsCollection = "notifications"
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for different types of notifications
     */
    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                "application_updates",
                "Application Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about job application status changes"
            },
            NotificationChannel(
                "new_applications",
                "New Applications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about new job applications"
            },
            NotificationChannel(
                "job_updates",
                "Job Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about job posting updates"
            },
            NotificationChannel(
                "general",
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }
        )
        
        channels.forEach { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Send notification for application status change
     */
    suspend fun sendApplicationStatusNotification(
        application: JobApplication,
        newStatus: ApplicationStatus,
        recipientId: String
    ): Result<Unit> {
        return try {
            val notification = createApplicationStatusNotification(application, newStatus)
            sendNotification(notification, recipientId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send notification for new application
     */
    suspend fun sendNewApplicationNotification(
        application: JobApplication,
        employerId: String
    ): Result<Unit> {
        return try {
            val notification = createNewApplicationNotification(application)
            sendNotification(notification, employerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send notification for job posted successfully
     */
    suspend fun sendJobPostedNotification(
        jobTitle: String,
        employerId: String
    ): Result<Unit> {
        println("🔔 NotificationService.sendJobPostedNotification called")
        println("🔔 jobTitle: $jobTitle")
        println("🔔 employerId: $employerId")
        
        return try {
            val notification = createJobPostedNotification(jobTitle)
            println("🔔 Created notification: ${notification.title}")
            sendNotification(notification, employerId)
            println("🔔 Notification sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            println("🔔 ERROR: Failed to send job posted notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send notification for job paused/unpaused
     */
    suspend fun sendJobPausedNotification(
        jobTitle: String,
        isPaused: Boolean,
        employerId: String
    ): Result<Unit> {
        println("🔔 NotificationService.sendJobPausedNotification called")
        println("🔔 jobTitle: $jobTitle")
        println("🔔 isPaused: $isPaused")
        println("🔔 employerId: $employerId")
        
        return try {
            val notification = createJobPausedNotification(jobTitle, isPaused)
            println("🔔 Created notification: ${notification.title}")
            sendNotification(notification, employerId)
            println("🔔 Notification sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            println("🔔 ERROR: Failed to send job paused notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get notifications for a user
     */
    fun getUserNotifications(userId: String): Flow<Result<List<NotificationData>>> = flow {
        try {
            val snapshot = firestore.collection(notificationsCollection)
                .whereEqualTo("recipientId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            val notifications = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(NotificationData::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            
            emit(Result.success(notifications))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * Mark notification as read
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            firestore.collection(notificationsCollection)
                .document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection(notificationsCollection)
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get unread notification count
     */
    suspend fun getUnreadNotificationCount(userId: String): Result<Int> {
        return try {
            val snapshot = firestore.collection(notificationsCollection)
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create application status notification
     */
    private fun createApplicationStatusNotification(
        application: JobApplication,
        newStatus: ApplicationStatus
    ): NotificationData {
        val title = when (newStatus) {
            ApplicationStatus.REVIEWED -> "Application Reviewed"
            ApplicationStatus.SHORTLISTED -> "Application Shortlisted!"
            ApplicationStatus.INTERVIEW_SCHEDULED -> "Interview Scheduled"
            ApplicationStatus.SELECTED -> "Congratulations! You're Selected"
            ApplicationStatus.REJECTED -> "Application Update"
            else -> "Application Status Update"
        }
        
        val message = when (newStatus) {
            ApplicationStatus.REVIEWED -> "Your application for ${application.jobTitle} has been reviewed"
            ApplicationStatus.SHORTLISTED -> "Great news! You've been shortlisted for ${application.jobTitle}"
            ApplicationStatus.INTERVIEW_SCHEDULED -> "Interview scheduled for ${application.jobTitle}"
            ApplicationStatus.SELECTED -> "Congratulations! You've been selected for ${application.jobTitle}"
            ApplicationStatus.REJECTED -> "Update on your application for ${application.jobTitle}"
            else -> "Your application status has been updated"
        }
        
        return NotificationData(
            id = UUID.randomUUID().toString(),
            recipientId = application.workerId,
            title = title,
            message = message,
            type = NotificationType.APPLICATION_STATUS,
            data = mapOf(
                "applicationId" to application.applicationId,
                "jobId" to application.jobId,
                "status" to newStatus.name
            ),
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
    }
    
    /**
     * Create new application notification
     */
    private fun createNewApplicationNotification(application: JobApplication): NotificationData {
        return NotificationData(
            id = UUID.randomUUID().toString(),
            recipientId = application.employerId,
            title = "New Application Received",
            message = "${application.workerName} applied for ${application.jobTitle}",
            type = NotificationType.NEW_APPLICATION,
            data = mapOf(
                "applicationId" to application.applicationId,
                "jobId" to application.jobId,
                "workerId" to application.workerId
            ),
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
    }
    
    /**
     * Create job posted notification
     */
    private fun createJobPostedNotification(jobTitle: String): NotificationData {
        return NotificationData(
            id = UUID.randomUUID().toString(),
            recipientId = "", // Will be set when sending
            title = "Job Posted Successfully! 🎉",
            message = "Your job '$jobTitle' has been posted and is now visible to workers",
            type = NotificationType.JOB_POSTED,
            data = mapOf(
                "jobTitle" to jobTitle,
                "action" to "view_job"
            ),
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
    }
    
    /**
     * Create job paused notification
     */
    private fun createJobPausedNotification(jobTitle: String, isPaused: Boolean): NotificationData {
        val title = if (isPaused) "Job Paused ⏸️" else "Job Activated ▶️"
        val message = if (isPaused) 
            "Your job '$jobTitle' has been paused and is no longer visible to workers"
        else 
            "Your job '$jobTitle' has been activated and is now visible to workers"
            
        return NotificationData(
            id = UUID.randomUUID().toString(),
            recipientId = "", // Will be set when sending
            title = title,
            message = message,
            type = NotificationType.JOB_PAUSED,
            data = mapOf(
                "jobTitle" to jobTitle,
                "isPaused" to isPaused.toString(),
                "action" to "view_job"
            ),
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
    }

    /**
     * Send notification to user
     */
    private suspend fun sendNotification(notification: NotificationData, recipientId: String) {
        println("🔔 NotificationService.sendNotification called")
        println("🔔 notification.id: ${notification.id}")
        println("🔔 notification.title: ${notification.title}")
        println("🔔 recipientId: $recipientId")
        
        // Save to Firestore
        val notificationWithRecipient = notification.copy(recipientId = recipientId)
        println("🔔 Saving notification to Firestore...")
        
        firestore.collection(notificationsCollection)
            .document(notificationWithRecipient.id)
            .set(notificationWithRecipient)
            .await()
        
        println("🔔 Notification saved to Firestore successfully")
        
        // Send push notification (if needed)
        println("🔔 Sending push notification...")
        sendPushNotification(notificationWithRecipient)
        println("🔔 Push notification sent")
    }
    
    /**
     * Send push notification
     */
    private fun sendPushNotification(notification: NotificationData) {
        // Check if notification permission is granted
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                println("🔔 ERROR: Notification permission not granted, cannot show notification")
                return
            }
        }
        val intent = Intent(context, com.example.partimes.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notificationId", notification.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val channelId = when (notification.type) {
            NotificationType.APPLICATION_STATUS -> "application_updates"
            NotificationType.NEW_APPLICATION -> "new_applications"
            NotificationType.JOB_UPDATE -> "job_updates"
            NotificationType.JOB_POSTED -> "job_updates"
            NotificationType.JOB_PAUSED -> "job_updates"
            else -> "general"
        }
        
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        notificationManager.notify(notification.id.hashCode(), notificationBuilder.build())
    }
}
