package com.example.dutype.services

import android.content.Context
import androidx.work.*
import com.example.dutype.models.NotificationData
import com.example.dutype.models.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Scheduler
 * 
 * Handles smart timing for notifications following industry best practices:
 * - Quiet hours (22:00 - 08:00)
 * - Optimal send times based on user activity
 * - Batch notifications to avoid spam
 * - Frequency control
 * 
 * Industry pattern: PhonePe/Paytm respect quiet hours and send at optimal times
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val notificationService: NotificationService
) {
    
    companion object {
        private const val QUIET_HOURS_START = 22 // 10 PM
        private const val QUIET_HOURS_END = 8 // 8 AM
        
        // Optimal send times (based on user activity patterns)
        private const val WORKER_OPTIMAL_MORNING = 8 // 8 AM
        private const val WORKER_OPTIMAL_EVENING = 18 // 6 PM
        private const val EMPLOYER_OPTIMAL_MORNING = 10 // 10 AM
        private const val EMPLOYER_OPTIMAL_AFTERNOON = 15 // 3 PM
        
        // Frequency limits
        private const val MAX_NOTIFICATIONS_PER_HOUR = 3
        private const val MAX_NOTIFICATIONS_PER_DAY = 10
    }
    
    /**
     * Schedule notification with smart timing
     * Respects quiet hours and optimal send times
     */
    suspend fun scheduleNotification(notification: NotificationData, userId: String) {
        try {
            // Check user preferences
            val userPrefs = getUserNotificationPreferences(userId)
            
            // Check if user has disabled this notification type
            if (!userPrefs.isNotificationTypeEnabled(notification.type)) {
                Timber.d("NotificationScheduler: Notification type ${notification.type} disabled for user $userId")
                return
            }
            
            // Check frequency limits
            if (!checkFrequencyLimits(userId)) {
                Timber.d("NotificationScheduler: Frequency limit reached for user $userId")
                return
            }
            
            // Check quiet hours
            if (isQuietHours(userPrefs)) {
                // Schedule for next optimal time
                val delayMinutes = calculateDelayUntilOptimalTime(userPrefs)
                scheduleDelayedNotification(notification, userId, delayMinutes)
                Timber.d("NotificationScheduler: Scheduled notification for $delayMinutes minutes later (quiet hours)")
            } else {
                // Send immediately
                notificationService.sendNotification(notification, userId)
                recordNotificationSent(userId)
                Timber.d("NotificationScheduler: Sent notification immediately")
            }
        } catch (e: Exception) {
            Timber.e(e, "NotificationScheduler: Error scheduling notification")
        }
    }
    
    /**
     * Check if current time is within quiet hours
     */
    private fun isQuietHours(userPrefs: UserNotificationPreferences): Boolean {
        if (!userPrefs.quietHoursEnabled) return false
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val quietStart = userPrefs.quietHoursStart ?: QUIET_HOURS_START
        val quietEnd = userPrefs.quietHoursEnd ?: QUIET_HOURS_END
        
        return if (quietStart < quietEnd) {
            currentHour >= quietStart || currentHour < quietEnd
        } else {
            currentHour >= quietStart && currentHour < quietEnd
        }
    }
    
    /**
     * Calculate delay until next optimal send time
     */
    private fun calculateDelayUntilOptimalTime(userPrefs: UserNotificationPreferences): Long {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val optimalHour = when (userPrefs.userRole) {
            "WORKER" -> if (currentHour < 12) WORKER_OPTIMAL_MORNING else WORKER_OPTIMAL_EVENING
            "EMPLOYER" -> if (currentHour < 12) EMPLOYER_OPTIMAL_MORNING else EMPLOYER_OPTIMAL_AFTERNOON
            else -> 9 // Default to 9 AM
        }
        
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, optimalHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            
            // If optimal time has passed today, schedule for tomorrow
            if (timeInMillis <= calendar.timeInMillis) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val delayMillis = targetCalendar.timeInMillis - calendar.timeInMillis
        return TimeUnit.MILLISECONDS.toMinutes(delayMillis)
    }
    
    /**
     * Schedule delayed notification using WorkManager
     */
    private fun scheduleDelayedNotification(
        notification: NotificationData,
        userId: String,
        delayMinutes: Long
    ) {
        val workRequest = OneTimeWorkRequestBuilder<SendNotificationWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    "notificationId" to notification.id,
                    "userId" to userId,
                    "title" to notification.title,
                    "message" to notification.message,
                    "type" to notification.type.name,
                    "data" to notification.data.toString()
                )
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
    }
    
    /**
     * Check if user has exceeded frequency limits
     */
    private suspend fun checkFrequencyLimits(userId: String): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val oneHourAgo = now - TimeUnit.HOURS.toMillis(1)
            val oneDayAgo = now - TimeUnit.DAYS.toMillis(1)
            
            // Check hourly limit
            val hourlyCount = firestore.collection("notificationLog")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("sentAt", oneHourAgo)
                .get()
                .await()
                .size()
            
            if (hourlyCount >= MAX_NOTIFICATIONS_PER_HOUR) {
                Timber.w("NotificationScheduler: Hourly limit exceeded for user $userId")
                return false
            }
            
            // Check daily limit
            val dailyCount = firestore.collection("notificationLog")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("sentAt", oneDayAgo)
                .get()
                .await()
                .size()
            
            if (dailyCount >= MAX_NOTIFICATIONS_PER_DAY) {
                Timber.w("NotificationScheduler: Daily limit exceeded for user $userId")
                return false
            }
            
            true
        } catch (e: Exception) {
            Timber.e(e, "Error checking frequency limits")
            true // Allow notification on error
        }
    }
    
    /**
     * Record notification sent for frequency tracking
     */
    private suspend fun recordNotificationSent(userId: String) {
        try {
            firestore.collection("notificationLog")
                .add(
                    mapOf(
                        "userId" to userId,
                        "sentAt" to System.currentTimeMillis()
                    )
                )
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error recording notification sent")
        }
    }
    
    /**
     * Get user notification preferences
     */
    private suspend fun getUserNotificationPreferences(userId: String): UserNotificationPreferences {
        return try {
            val doc = firestore.collection("userPreferences")
                .document(userId)
                .get()
                .await()
            
            if (doc.exists()) {
                UserNotificationPreferences(
                    quietHoursEnabled = doc.getBoolean("quietHoursEnabled") ?: true,
                    quietHoursStart = doc.getLong("quietHoursStart")?.toInt() ?: QUIET_HOURS_START,
                    quietHoursEnd = doc.getLong("quietHoursEnd")?.toInt() ?: QUIET_HOURS_END,
                    userRole = doc.getString("userRole") ?: "WORKER",
                    enabledNotificationTypes = doc.get("enabledNotificationTypes") as? List<String> ?: emptyList()
                )
            } else {
                // Default preferences
                UserNotificationPreferences()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user preferences")
            UserNotificationPreferences()
        }
    }
}

/**
 * User notification preferences
 */
data class UserNotificationPreferences(
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 8,
    val userRole: String = "WORKER",
    val enabledNotificationTypes: List<String> = emptyList()
) {
    fun isNotificationTypeEnabled(type: com.example.dutype.models.NotificationType): Boolean {
        // If no preferences set, enable all
        if (enabledNotificationTypes.isEmpty()) return true
        return enabledNotificationTypes.contains(type.name)
    }
}

/**
 * WorkManager worker for sending delayed notifications
 */
class SendNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val notificationId = inputData.getString("notificationId") ?: return Result.failure()
            val userId = inputData.getString("userId") ?: return Result.failure()
            val title = inputData.getString("title") ?: return Result.failure()
            val message = inputData.getString("message") ?: return Result.failure()
            val type = inputData.getString("type") ?: return Result.failure()
            
            // Send notification
            Timber.d("SendNotificationWorker: Sending delayed notification $notificationId")
            
            // P2 FIX: Send notification via NotificationService
            try {
                val notificationService = NotificationService(
                    context = applicationContext,
                    firestore = FirebaseFirestore.getInstance()
                )
                
                // Parse type string to NotificationType enum
                val notificationType = try {
                    NotificationType.valueOf(type)
                } catch (e: Exception) {
                    NotificationType.GENERAL
                }
                
                // Create notification data model
                val notificationData = NotificationData(
                    id = notificationId,
                    recipientId = userId,
                    title = title,
                    message = message,
                    type = notificationType,
                    data = emptyMap()
                )
                
                // Send the notification
                notificationService.sendNotification(
                    notification = notificationData,
                    recipientId = userId
                )
                
                Timber.i("✅ SendNotificationWorker: Sent notification $notificationId to $userId")
                Result.success()
            } catch (e: Exception) {
                Timber.e(e, "❌ SendNotificationWorker: Failed to send notification")
                Result.failure()
            }
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SendNotificationWorker: Error sending notification")
            Result.failure()
        }
    }
}
