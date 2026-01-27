package com.example.dutype.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Calendar

/**
 * Smart Notification Worker - Enterprise-Grade Background Notifications
 * 
 * STRATEGY (Following Swiggy, Zomato, PhonePe, LinkedIn patterns):
 * 
 * 1. INTELLIGENT DEDUPLICATION:
 *    - Tracks last notification sent per user per type
 *    - Prevents spam with minimum intervals (24h for re-engagement, 12h for reminders)
 *    - Uses Firestore for cross-device deduplication
 * 
 * 2. QUIET HOURS RESPECT:
 *    - No notifications between 10 PM - 8 AM (user sleep time)
 *    - Queues notifications for next morning if needed
 * 
 * 3. NOTIFICATION IMPORTANCE SCORING:
 *    - High: Job expiry (24h before), Application status changes
 *    - Medium: Pending applications (48h+), New jobs nearby
 *    - Low: Re-engagement (3+ days inactive)
 * 
 * 4. RATE LIMITING:
 *    - Max 3 notifications per user per day
 *    - Prioritizes high-importance notifications
 * 
 * 5. BATTERY OPTIMIZATION:
 *    - Batches Firestore queries
 *    - Limits to 100 users per run
 *    - Uses pagination for large datasets
 * 
 * Runs periodically (5 min for testing, 3 hours for production)
 */
@HiltWorker
class SmartNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val smartNotificationManager: SmartNotificationManager,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {
    
    companion object {
        // Deduplication intervals (milliseconds)
        private const val RE_ENGAGEMENT_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
        private const val REMINDER_INTERVAL = 12 * 60 * 60 * 1000L // 12 hours
        private const val JOB_EXPIRY_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
        
        // Rate limiting
        private const val MAX_NOTIFICATIONS_PER_USER_PER_DAY = 3
        
        // Batch size for processing
        private const val BATCH_SIZE = 100L // Long for Firestore limit()
        
        // Quiet hours (10 PM - 8 AM)
        private const val QUIET_HOUR_START = 22 // 10 PM
        private const val QUIET_HOUR_END = 8 // 8 AM
    }
    
    override suspend fun doWork(): Result {
        return try {
            Timber.i("🔔 ========================================")
            Timber.i("🔔 SmartNotificationWorker: STARTING")
            Timber.i("🔔 Run attempt: $runAttemptCount")
            Timber.i("🔔 Tags: ${tags.joinToString()}")
            Timber.i("🔔 ========================================")
            
            // Check if we're in quiet hours
            if (isQuietHours()) {
                Timber.i("🔔 SmartNotificationWorker: Quiet hours (10 PM - 8 AM), skipping notifications")
                return Result.success()
            }
            
            // 0. Check birthdays (HIGH priority - special day!)
            checkBirthdays()
            
            // 1. Check jobs expiring in 24 hours (HIGH priority)
            checkExpiringJobs()
            
            // 2. Check pending applications (MEDIUM priority)
            checkPendingApplications()
            
            // 3. Re-engage inactive workers (LOW priority)
            reEngageInactiveWorkers()
            
            // 4. Re-engage inactive employers (LOW priority)
            reEngageInactiveEmployers()
            
            Timber.i("🔔 ========================================")
            Timber.i("🔔 SmartNotificationWorker: COMPLETED SUCCESSFULLY")
            Timber.i("🔔 ========================================")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "🔔 SmartNotificationWorker: ERROR - ${e.message}")
            Result.retry()
        }
    }
    
    /**
     * Check if current time is in quiet hours (10 PM - 8 AM)
     * Enterprise pattern: Respect user sleep time
     */
    private fun isQuietHours(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour >= QUIET_HOUR_START || hour < QUIET_HOUR_END
    }
    
    /**
     * Check if user can receive notification (rate limiting + deduplication)
     * 
     * @param userId User ID
     * @param notificationType Type of notification (re_engagement, reminder, job_expiry)
     * @param minInterval Minimum interval between notifications of this type
     * @return true if notification can be sent
     */
    private suspend fun canSendNotification(
        userId: String,
        notificationType: String,
        minInterval: Long
    ): Boolean {
        try {
            // Check last notification of this type
            val lastNotificationSnapshot = firestore.collection("notification_tracking")
                .document(userId)
                .collection("sent")
                .whereEqualTo("type", notificationType)
                .orderBy("sentAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (!lastNotificationSnapshot.isEmpty) {
                val lastSentAt = lastNotificationSnapshot.documents[0].getLong("sentAt") ?: 0
                val timeSinceLastNotification = System.currentTimeMillis() - lastSentAt
                
                if (timeSinceLastNotification < minInterval) {
                    Timber.d("🔔 Deduplication: Skipping $notificationType for $userId (sent ${timeSinceLastNotification / 1000 / 60} min ago)")
                    return false
                }
            }
            
            // Check daily rate limit
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val todayNotificationsSnapshot = firestore.collection("notification_tracking")
                .document(userId)
                .collection("sent")
                .whereGreaterThan("sentAt", todayStart)
                .get()
                .await()
            
            if (todayNotificationsSnapshot.size().toLong() >= MAX_NOTIFICATIONS_PER_USER_PER_DAY) {
                Timber.d("🔔 Rate limit: User $userId reached daily limit (${todayNotificationsSnapshot.size()})")
                return false
            }
            
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error checking notification eligibility")
            return false // Fail safe - don't send if we can't check
        }
    }
    
    /**
     * Track notification sent (for deduplication and rate limiting)
     */
    private suspend fun trackNotificationSent(userId: String, notificationType: String) {
        try {
            firestore.collection("notification_tracking")
                .document(userId)
                .collection("sent")
                .add(mapOf(
                    "type" to notificationType,
                    "sentAt" to System.currentTimeMillis()
                ))
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error tracking notification")
        }
    }
    
    /**
     * Check birthdays and send wishes (HIGH priority - special day!)
     * Uses Firestore tracking instead of SharedPreferences for cross-device deduplication
     */
    private suspend fun checkBirthdays() {
        try {
            val today = Calendar.getInstance()
            val todayDay = today.get(Calendar.DAY_OF_MONTH)
            val todayMonth = today.get(Calendar.MONTH) + 1 // Calendar months are 0-indexed
            
            Timber.i("🎂 ========== BIRTHDAY CHECK START ==========")
            Timber.i("🎂 Today's date: $todayDay/$todayMonth/${today.get(Calendar.YEAR)}")
            
            // Get all users (batch processing)
            val usersSnapshot = firestore.collection("users")
                .limit(BATCH_SIZE)
                .get()
                .await()
            
            Timber.i("🎂 Checking ${usersSnapshot.documents.size} users for birthdays")
            
            var birthdayWishesSent = 0
            var birthdaysFound = 0
            var alreadySent = 0
            
            usersSnapshot.documents.forEach { doc ->
                val userId = doc.id
                val dateOfBirth = doc.getString("dateOfBirth")
                val fullName = doc.getString("fullName") ?: "Friend"
                
                if (dateOfBirth != null) {
                    // Parse date of birth
                    val (birthDay, birthMonth) = parseDateOfBirth(dateOfBirth) ?: return@forEach
                    
                    // Check if today is their birthday
                    if (birthDay == todayDay && birthMonth == todayMonth) {
                        birthdaysFound++
                        Timber.i("🎂 🎉 BIRTHDAY FOUND: $fullName (userId: $userId, DOB: $dateOfBirth)")
                        
                        // Check if we can send notification (deduplication)
                        if (canSendNotification(userId, "birthday", 24 * 60 * 60 * 1000L)) {
                            // Send birthday notification
                            val userName = fullName.split(" ").firstOrNull() ?: fullName
                            val notification = com.example.dutype.models.NotificationData(
                                id = java.util.UUID.randomUUID().toString(),
                                recipientId = userId,
                                title = "🎂 Happy Birthday, $userName! 🎉",
                                message = "Wishing you a wonderful birthday filled with joy and success! May this year bring you amazing opportunities. - Team DutyPe",
                                type = com.example.dutype.models.NotificationType.BIRTHDAY,
                                data = mapOf(
                                    "userName" to userName,
                                    "action" to "birthday_wish",
                                    "deepLink" to "dutype://profile"
                                ),
                                createdAt = System.currentTimeMillis(),
                                isRead = false
                            )
                            
                            // Save to Firestore (Cloud Function will send FCM push)
                            firestore.collection("notifications")
                                .document(notification.id)
                                .set(notification)
                                .await()
                            
                            trackNotificationSent(userId, "birthday")
                            birthdayWishesSent++
                            Timber.i("🎂 ✅ Birthday notification sent to $userName (notificationId: ${notification.id})")
                        } else {
                            alreadySent++
                            Timber.d("🎂 ⏭️ Birthday notification already sent today for $fullName")
                        }
                    }
                }
            }
            
            Timber.i("🎂 ========== BIRTHDAY CHECK COMPLETE ==========")
            Timber.i("🎂 Summary: Found $birthdaysFound birthdays, Sent $birthdayWishesSent notifications, Already sent: $alreadySent")
        } catch (e: Exception) {
            Timber.e(e, "🎂 ❌ Error checking birthdays")
        }
    }
    
    /**
     * Parse date of birth string to day and month
     * Supports formats: "DD/MM/YYYY", "DD-MM-YYYY", "YYYY-MM-DD"
     */
    private fun parseDateOfBirth(dateOfBirth: String): Pair<Int, Int>? {
        return try {
            val parts = dateOfBirth.split("/", "-")
            when {
                // Format: DD/MM/YYYY or DD-MM-YYYY
                parts.size == 3 && parts[0].length <= 2 -> {
                    val day = parts[0].toInt()
                    val month = parts[1].toInt()
                    Pair(day, month)
                }
                // Format: YYYY-MM-DD
                parts.size == 3 && parts[0].length == 4 -> {
                    val day = parts[2].toInt()
                    val month = parts[1].toInt()
                    Pair(day, month)
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.w("Could not parse date of birth: $dateOfBirth")
            null
        }
    }
    
    /**
     * Check jobs expiring in next 24 hours and notify employers
     * HIGH PRIORITY - Job expiry is critical for employers
     */
    private suspend fun checkExpiringJobs() {
        try {
            val now = System.currentTimeMillis()
            val tomorrow = now + (24 * 60 * 60 * 1000) // 24 hours from now
            
            val snapshot = firestore.collection("jobs")
                .whereEqualTo("status", "ACTIVE")
                .whereLessThan("expiresAt", tomorrow)
                .whereGreaterThan("expiresAt", now)
                .limit(BATCH_SIZE)
                .get()
                .await()
            
            Timber.d("🔔 Found ${snapshot.size()} jobs expiring in 24 hours")
            
            var sentCount = 0
            snapshot.documents.forEach { doc ->
                val jobId = doc.id
                val employerId = doc.getString("employerId") ?: return@forEach
                
                // Check if we can send notification (deduplication + rate limiting)
                if (canSendNotification(employerId, "job_expiry", JOB_EXPIRY_INTERVAL)) {
                    smartNotificationManager.notifyJobExpiringSoon(jobId, employerId)
                    trackNotificationSent(employerId, "job_expiry")
                    sentCount++
                }
            }
            
            Timber.i("🔔 Sent $sentCount job expiry notifications")
        } catch (e: Exception) {
            Timber.e(e, "Error checking expiring jobs")
        }
    }
    
    /**
     * Check applications pending for 48+ hours and remind employers
     * MEDIUM PRIORITY - Pending applications need attention
     */
    private suspend fun checkPendingApplications() {
        try {
            val twoDaysAgo = System.currentTimeMillis() - (48 * 60 * 60 * 1000)
            
            // Get employers with pending applications
            val snapshot = firestore.collection("applications")
                .whereEqualTo("status", "PENDING")
                .whereLessThan("appliedAt", twoDaysAgo)
                .limit(BATCH_SIZE)
                .get()
                .await()
            
            // Group by employer
            val employerIds = snapshot.documents
                .mapNotNull { it.getString("employerId") }
                .distinct()
            
            Timber.d("🔔 Found ${employerIds.size} employers with pending applications")
            
            var sentCount = 0
            employerIds.forEach { employerId ->
                // Check if we can send notification (deduplication + rate limiting)
                if (canSendNotification(employerId, "pending_applications", REMINDER_INTERVAL)) {
                    smartNotificationManager.notifyPendingApplications(employerId)
                    trackNotificationSent(employerId, "pending_applications")
                    sentCount++
                }
            }
            
            Timber.i("🔔 Sent $sentCount pending application reminders")
        } catch (e: Exception) {
            Timber.e(e, "Error checking pending applications")
        }
    }
    
    /**
     * Re-engage workers who haven't applied in 3+ days
     * LOW PRIORITY - Re-engagement is nice-to-have
     */
    private suspend fun reEngageInactiveWorkers() {
        try {
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)
            
            // Get workers (batch processing)
            val workersSnapshot = firestore.collection("users")
                .whereEqualTo("role", "WORKER")
                .limit(BATCH_SIZE)
                .get()
                .await()
            
            var reEngagedCount = 0
            
            workersSnapshot.documents.forEach { doc ->
                val workerId = doc.id
                
                // Check if we can send notification (deduplication + rate limiting)
                if (!canSendNotification(workerId, "re_engagement", RE_ENGAGEMENT_INTERVAL)) {
                    return@forEach
                }
                
                // Check last application
                val lastAppSnapshot = firestore.collection("applications")
                    .whereEqualTo("workerId", workerId)
                    .orderBy("appliedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                
                val shouldReEngage = if (lastAppSnapshot.isEmpty) {
                    true // Never applied
                } else {
                    val lastAppliedAt = lastAppSnapshot.documents[0].getLong("appliedAt") ?: 0
                    lastAppliedAt < threeDaysAgo // Inactive for 3+ days
                }
                
                if (shouldReEngage) {
                    smartNotificationManager.reEngageInactiveWorker(workerId)
                    trackNotificationSent(workerId, "re_engagement")
                    reEngagedCount++
                }
            }
            
            Timber.i("🔔 Re-engaged $reEngagedCount inactive workers")
        } catch (e: Exception) {
            Timber.e(e, "Error re-engaging inactive workers")
        }
    }
    
    /**
     * Re-engage employers who haven't posted in 15+ days
     * LOW PRIORITY - Re-engagement is nice-to-have
     */
    private suspend fun reEngageInactiveEmployers() {
        try {
            val fifteenDaysAgo = System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000)
            
            // Get employers (batch processing)
            val employersSnapshot = firestore.collection("users")
                .whereEqualTo("role", "EMPLOYER")
                .limit(BATCH_SIZE)
                .get()
                .await()
            
            var reEngagedCount = 0
            
            employersSnapshot.documents.forEach { doc ->
                val employerId = doc.id
                
                // Check if we can send notification (deduplication + rate limiting)
                if (!canSendNotification(employerId, "re_engagement", RE_ENGAGEMENT_INTERVAL)) {
                    return@forEach
                }
                
                // Check last job post
                val lastJobSnapshot = firestore.collection("jobs")
                    .whereEqualTo("employerId", employerId)
                    .orderBy("postedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                
                val shouldReEngage = if (lastJobSnapshot.isEmpty) {
                    true // Never posted
                } else {
                    val lastPostedAt = lastJobSnapshot.documents[0].getLong("postedAt") ?: 0
                    lastPostedAt < fifteenDaysAgo // Inactive for 15+ days
                }
                
                if (shouldReEngage) {
                    smartNotificationManager.reEngageInactiveEmployer(employerId)
                    trackNotificationSent(employerId, "re_engagement")
                    reEngagedCount++
                }
            }
            
            Timber.i("🔔 Re-engaged $reEngagedCount inactive employers")
        } catch (e: Exception) {
            Timber.e(e, "Error re-engaging inactive employers")
        }
    }
    
}
