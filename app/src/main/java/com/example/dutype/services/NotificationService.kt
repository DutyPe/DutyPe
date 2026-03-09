package com.example.dutype.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.NotificationData
import com.example.dutype.models.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.example.dutype.utils.SecureLogger
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Notification Service
 * Handles in-app notifications and Firestore storage
 * Works with FCM for push notifications
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
                enableVibration(true)
            },
            NotificationChannel(
                "new_applications",
                "New Applications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about new job applications"
                enableVibration(true)
            },
            NotificationChannel(
                "job_updates",
                "Job Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about job posting updates"
            },
            NotificationChannel(
                "birthday",
                "Birthday Wishes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Birthday wishes from DutyPe 🎂"
                enableVibration(true)
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
        Timber.i("NotificationService.sendJobPostedNotification called")
        Timber.d("jobTitle: $jobTitle")
        Timber.d("employerId: $employerId")
        
        return try {
            val notification = createJobPostedNotification(jobTitle)
            Timber.d("Created notification: ${notification.title}")
            sendNotification(notification, employerId)
            Timber.i("Notification sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send job posted notification")
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
        Timber.i("NotificationService.sendJobPausedNotification called")
        Timber.d("jobTitle: $jobTitle")
        Timber.d("isPaused: $isPaused")
        Timber.d("employerId: $employerId")
        
        return try {
            val notification = createJobPausedNotification(jobTitle, isPaused)
            Timber.d("Created notification: ${notification.title}")
            sendNotification(notification, employerId)
            Timber.i("Notification sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send job paused notification")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Send notification for profile completion (welcome message)
     */
    suspend fun sendProfileCompleteNotification(
        userName: String,
        userId: String,
        userRole: String
    ): Result<Unit> {
        Timber.i("NotificationService.sendProfileCompleteNotification called")
        Timber.d("userName: $userName, userId: $userId, userRole: $userRole")
        
        return try {
            val notification = createProfileCompleteNotification(userName, userRole)
            Timber.d("Created notification: ${notification.title}")
            sendNotification(notification, userId)
            Timber.i("Profile complete notification sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send profile complete notification")
            Result.failure(e)
        }
    }
    
    /**
     * Send notification when worker is hired
     */
    suspend fun sendWorkerHiredNotification(
        workerName: String,
        jobTitle: String,
        workerId: String,
        employerId: String,
        jobId: String
    ): Result<Unit> {
        Timber.i("NotificationService.sendWorkerHiredNotification called")
        Timber.d("workerName: $workerName, jobTitle: $jobTitle")
        
        return try {
            // Send notification to worker
            val workerNotification = createWorkerHiredNotification(jobTitle, jobId)
            sendNotification(workerNotification, workerId)
            
            // Send notification to employer
            val employerNotification = createEmployerHiredNotification(workerName, jobTitle, jobId)
            sendNotification(employerNotification, employerId)
            
            Timber.i("Worker hired notifications sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send worker hired notification")
            Result.failure(e)
        }
    }
    
    /**
     * Send notification when worker withdraws application
     */
    suspend fun sendApplicationWithdrawnNotification(
        application: JobApplication,
        employerId: String
    ): Result<Unit> {
        return try {
            val notification = NotificationData(
                id = UUID.randomUUID().toString(),
                recipientId = employerId,
                title = "Application Withdrawn",
                message = "${application.workerName} has withdrawn their application for ${application.jobTitle}",
                type = NotificationType.APPLICATION_STATUS,
                data = mapOf(
                    "applicationId" to application.applicationId,
                    "jobId" to application.jobId,
                    "workerId" to application.workerId,
                    "status" to "WITHDRAWN"
                ),
                createdAt = System.currentTimeMillis(),
                isRead = false
            )
            sendNotification(notification, employerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send application withdrawn notification")
            Result.failure(e)
        }
    }
    
    
    /**
     * Get notifications for a user
     */
    fun getUserNotifications(userId: String): Flow<Result<List<NotificationData>>> = flow {
        try {
            Timber.i("NotificationService.getUserNotifications - Loading notifications for userId: $userId")
            val snapshot = firestore.collection(notificationsCollection)
                .whereEqualTo("recipientId", userId)
                .limit(50)
                .get()
                .await()
            
            Timber.d("NotificationService.getUserNotifications - Found ${snapshot.documents.size} documents")
            
            val notifications = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    
                    // Handle createdAt - can be Long or Timestamp
                    val createdAt = when (val createdAtValue = data["createdAt"]) {
                        is Long -> createdAtValue
                        is com.google.firebase.Timestamp -> createdAtValue.toDate().time
                        else -> System.currentTimeMillis()
                    }
                    
                    // Parse type safely
                    val typeString = data["type"]?.toString() ?: "GENERAL"
                    val type = try {
                        NotificationType.valueOf(typeString.uppercase())
                    } catch (e: Exception) {
                        NotificationType.GENERAL
                    }
                    
                    // Parse data map safely
                    @Suppress("UNCHECKED_CAST")
                    val notificationDataMap = (data["data"] as? Map<String, Any>)?.mapValues { it.value.toString() } ?: emptyMap()
                    
                    NotificationData(
                        id = doc.id,
                        recipientId = data["recipientId"]?.toString() ?: "",
                        title = data["title"]?.toString() ?: "",
                        message = data["message"]?.toString() ?: "",
                        type = type,
                        data = notificationDataMap,
                        createdAt = createdAt,
                        isRead = data["isRead"] as? Boolean ?: false
                    )
                } catch (e: Exception) {
                    Timber.e(e, "NotificationService.getUserNotifications - Error parsing document ${doc.id}")
                    null
                }
            }
            // Sort locally by createdAt in descending order
            .sortedByDescending { it.createdAt }
            
            Timber.i("NotificationService.getUserNotifications - Successfully parsed ${notifications.size} notifications")
            emit(Result.success(notifications))
        } catch (e: Exception) {
            Timber.e(e, "NotificationService.getUserNotifications - Error")
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
            ApplicationStatus.PENDING -> "Application Submitted"
            ApplicationStatus.UNDER_REVIEW -> "Application Under Review"
            ApplicationStatus.ACCEPTED -> "Congratulations! You're Accepted"
            ApplicationStatus.COMPLETED -> "Job Completed"
            ApplicationStatus.REJECTED -> "Application Update"
            ApplicationStatus.WITHDRAWN -> "Application Withdrawn"
        }
        
        val message = when (newStatus) {
            ApplicationStatus.PENDING -> "Your application for ${application.jobTitle} has been submitted successfully"
            ApplicationStatus.UNDER_REVIEW -> "Your application for ${application.jobTitle} is now under review"
            ApplicationStatus.ACCEPTED -> "Congratulations! You've been accepted for ${application.jobTitle}"
            ApplicationStatus.COMPLETED -> "Great job! Your work for ${application.jobTitle} has been marked as completed"
            ApplicationStatus.REJECTED -> "Update on your application for ${application.jobTitle}"
            ApplicationStatus.WITHDRAWN -> "You have withdrawn your application for ${application.jobTitle}"
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
     * Create profile complete notification (welcome message)
     */
    private fun createProfileCompleteNotification(userName: String, userRole: String): NotificationData {
        val title = "Welcome to DutyPe! 🎉"
        val message = if (userRole.equals("EMPLOYER", ignoreCase = true)) {
            "Hi $userName! Your profile is complete. Start posting jobs and find the best workers for your business."
        } else {
            "Hi $userName! Your profile is complete. Start exploring jobs and apply to opportunities that match your skills."
        }
        
        val action = if (userRole.equals("EMPLOYER", ignoreCase = true)) "view_employer_home" else "view_worker_home"
        
        return NotificationData(
            id = UUID.randomUUID().toString(),
            recipientId = "", // Will be set when sending
            title = title,
            message = message,
            type = NotificationType.PROFILE_COMPLETE,
            data = mapOf(
                "userName" to userName,
                "userRole" to userRole,
                "action" to action
            ),
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
    }
    
    /**
     * Create worker hired notification (for worker)
     */
    private fun createWorkerHiredNotification(jobTitle: String, jobId: String): NotificationData {
        return NotificationData(
            id = UUID.randomUUID().toString(),
            recipientId = "", // Will be set when sending
            title = "Congratulations! You're Hired! 🎉",
            message = "Great news! You've been hired for '$jobTitle'. Contact the employer to discuss next steps.",
            type = NotificationType.WORKER_HIRED,
            data = mapOf(
                "jobTitle" to jobTitle,
                "jobId" to jobId,
                "action" to "view_job"
            ),
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
    }
    
    /**
     * Create employer hired notification (for employer)
     */
    private fun createEmployerHiredNotification(workerName: String, jobTitle: String, jobId: String): NotificationData {
        return NotificationData(
            id = UUID.randomUUID().toString(),
            recipientId = "", // Will be set when sending
            title = "Worker Hired Successfully! ✅",
            message = "You've successfully hired $workerName for '$jobTitle'. Contact them to coordinate the start date.",
            type = NotificationType.WORKER_HIRED,
            data = mapOf(
                "workerName" to workerName,
                "jobTitle" to jobTitle,
                "jobId" to jobId,
                "action" to "view_applications"
            ),
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
    }

    /**
     * Send notification to user with deep link
     * 
     * NOTE: We only save to Firestore here. The Cloud Function `sendPushNotification`
     * will automatically trigger and send the FCM push notification.
     * DO NOT call sendPushNotification() locally to avoid duplicate notifications!
     */
    suspend fun sendNotification(notification: NotificationData, recipientId: String) {
        Timber.i("NotificationService.sendNotification called")
        Timber.d("notification.id: ${notification.id}")
        Timber.d("notification.title: ${notification.title}")
        Timber.d("notification.type: ${notification.type}")
        Timber.d("recipientId: $recipientId")
        
        // Build deep link for this notification
        val deepLink = com.example.dutype.utils.NotificationDeepLinkBuilder.buildDeepLink(
            notification.type,
            notification.data
        )
        Timber.d("📱 Generated deep link: $deepLink")
        
        // Add deep link to notification data
        val dataWithDeepLink = notification.data.toMutableMap().apply {
            put("deepLink", deepLink)
            put("notificationId", notification.id)
        }
        
        // Save to Firestore - Cloud Function will handle FCM push notification
        val notificationWithRecipient = notification.copy(
            recipientId = recipientId,
            data = dataWithDeepLink
        )
        Timber.d("Saving notification to Firestore...")
        Timber.d("Notification to save: $notificationWithRecipient")
        
        firestore.collection(notificationsCollection)
            .document(notificationWithRecipient.id)
            .set(notificationWithRecipient)
            .await()
        
        Timber.i("Notification saved to Firestore successfully with ID: ${notificationWithRecipient.id}")
        Timber.d("Deep link included in notification data: $deepLink")
        // NOTE: FCM push notification is sent by Cloud Function (sendPushNotification in index.ts)
        // to avoid duplicate notifications
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
                Timber.w("Notification permission not granted, cannot show notification")
                return
            }
        }
        val intent = Intent(context, com.example.dutype.MainActivity::class.java).apply {
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
            NotificationType.PROFILE_COMPLETE -> "general"
            NotificationType.WORKER_HIRED -> "application_updates"
            NotificationType.WELCOME -> "general"
            NotificationType.BIRTHDAY -> "birthday"
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
    
    /**
     * Archive a notification
     */
    suspend fun archiveNotification(notificationId: String): Result<Unit> {
        return try {
            // In a real implementation, this would update the database
            // For now, we'll just return success
            Timber.i("NotificationService - Archiving notification: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "NotificationService - Error archiving notification")
            Result.failure(e)
        }
    }
    
    /**
     * Delete a notification from Firebase
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            Timber.i("NotificationService - Deleting notification from Firebase: $notificationId")
            
            firestore.collection(notificationsCollection)
                .document(notificationId)
                .delete()
                .await()
            
            Timber.i("NotificationService - Notification deleted successfully: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "NotificationService - Error deleting notification")
            Result.failure(e)
        }
    }
}
