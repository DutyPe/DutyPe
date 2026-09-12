package com.example.dutype.services

import com.dutype.app.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.NotificationData
import com.example.dutype.models.NotificationType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
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
    data class NotificationPage(
        val notifications: List<NotificationData>,
        val lastVisible: DocumentSnapshot?,
        val hasMore: Boolean
    )

    companion object {
        // Cloud cleanup should delete notification docs once this 30-day retention window passes.
        private const val NOTIFICATION_RETENTION_MS = 30L * 24 * 60 * 60 * 1000
        // Firestore rules enforce CF-only creates on /notifications.
        private const val CLIENT_NOTIFICATION_WRITES_ENABLED = false
        private const val SELF_NOTIFICATION_PERSIST_TIMEOUT_MS = 2_500L
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationsCollection = "notifications"

    // Worker-specific notification types
    private val workerTypes = setOf(
        NotificationType.APPLICATION_STATUS,
        NotificationType.APPLICATION_STATUS_UPDATE,
        NotificationType.APPLICATION_STATUS_UPDATE,
        NotificationType.REJECTED,
        NotificationType.WORKER_HIRED,
        NotificationType.NEW_JOB_ALERT,
        NotificationType.JOB_RECOMMENDATION,
        NotificationType.APPLICATION_REMINDER
    )

    // Employer-specific notification types
    private val employerTypes = setOf(
        NotificationType.NEW_APPLICATION,
        NotificationType.JOB_POSTED,
        NotificationType.JOB_EXPIRY_REMINDER
    )
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for different types of notifications (API 26+)
     */
    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannelManager.createNotificationChannels(context)
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
     * Send notification for job expiring soon (3-day reminder)
     */
    suspend fun sendJobExpiryReminderNotification(
        jobId: String,
        jobTitle: String,
        employerId: String,
        daysLeft: Int = 3
    ): Result<Unit> {
        return try {
            val notification = createJobExpiryReminderNotification(jobId, jobTitle, daysLeft)
            sendNotification(notification, employerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send job expiry reminder notification")
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
                title = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_application_withdrawn_title),
                message = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_application_withdrawn_msg),
                type = NotificationType.APPLICATION_STATUS,
                data = mapOf(
                    "applicationId" to application.id,
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
     * Cursor-based paginated notification fetch for high-volume users.
     * Uses server-side ordering and a document cursor to avoid large client reads.
     */
    fun getUserNotificationsPage(
        userId: String,
        activeRole: String? = null,
        pageSize: Int = 50,
        lastVisible: DocumentSnapshot? = null
    ): Flow<Result<NotificationPage>> = flow {
        try {
            val boundedPageSize = pageSize.coerceIn(10, 100)
            val now = System.currentTimeMillis()

            var query: Query = firestore.collection(notificationsCollection)
                .whereEqualTo("recipientId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(boundedPageSize.toLong())

            if (lastVisible != null) {
                query = query.startAfter(lastVisible)
            }

            val snapshot = query.get().await()
            val notifications = snapshot.documents.mapNotNull { doc ->
                parseNotificationDocument(doc, activeRole, now)
            }

            emit(
                Result.success(
                    NotificationPage(
                        notifications = notifications,
                        lastVisible = snapshot.documents.lastOrNull(),
                        hasMore = snapshot.documents.size >= boundedPageSize
                    )
                )
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    
    /**
     * Get notifications for a user, optionally filtered by active role
     */
    fun getUserNotifications(userId: String, activeRole: String? = null): Flow<Result<List<NotificationData>>> = flow {
        try {
            Timber.i("NotificationService.getUserNotifications - Loading notifications for userId: $userId, role: $activeRole")
            val snapshot = firestore.collection(notificationsCollection)
                .whereEqualTo("recipientId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            Timber.d("NotificationService.getUserNotifications - Found ${snapshot.documents.size} documents")
            val now = System.currentTimeMillis()
            
            val notifications = snapshot.documents.mapNotNull { doc ->
                parseNotificationDocument(doc, activeRole, now)
            }
            // Query already ordered by createdAt DESC; keep order stable.
            
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
        return markAllNotificationsAsRead(userId, activeRole = null)
    }

    suspend fun markAllNotificationsAsRead(userId: String, activeRole: String?): Result<Unit> {
        return try {
            val normalizedRole = activeRole?.uppercase()?.takeIf { it.isNotBlank() }

            // Firestore write batch hard limit is 500 docs; drain unread notifications in chunks.
            while (true) {
                val snapshot = firestore.collection(notificationsCollection)
                    .whereEqualTo("recipientId", userId)
                    .whereEqualTo("isRead", false)
                    .limit(500)
                    .get()
                    .await()

                if (snapshot.isEmpty) break

                val now = System.currentTimeMillis()
                val docsToUpdate = snapshot.documents.filter { doc ->
                    normalizedRole == null || parseNotificationDocument(doc, normalizedRole, now) != null
                }

                if (docsToUpdate.isEmpty()) {
                    break
                }

                val batch = firestore.batch()
                docsToUpdate.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get unread notification count
     */
    suspend fun getUnreadNotificationCount(userId: String): Result<Int> {
        return getUnreadNotificationCount(userId, activeRole = null)
    }

    suspend fun getUnreadNotificationCount(userId: String, activeRole: String?): Result<Int> {
        return try {
            val normalizedRole = activeRole?.uppercase()?.takeIf { it.isNotBlank() }

            if (normalizedRole == null) {
                // Use count() aggregation to avoid downloading documents
                val countQuery = firestore.collection(notificationsCollection)
                    .whereEqualTo("recipientId", userId)
                    .whereEqualTo("isRead", false)
                    .count()

                val snapshot = countQuery.get(com.google.firebase.firestore.AggregateSource.SERVER).await()
                Result.success(snapshot.count.toInt())
            } else {
                val snapshot = firestore.collection(notificationsCollection)
                    .whereEqualTo("recipientId", userId)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()

                val now = System.currentTimeMillis()
                val count = snapshot.documents.count { doc ->
                    parseNotificationDocument(doc, normalizedRole, now) != null
                }
                Result.success(count)
            }
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
        val titleRes = when (newStatus) {
            ApplicationStatus.APPLIED -> R.string.notif_app_status_applied_title
            ApplicationStatus.HIRED -> R.string.notif_app_status_hired_title
            ApplicationStatus.COMPLETED -> R.string.notif_app_status_hired_title
            ApplicationStatus.REJECTED -> R.string.notif_app_status_rejected_title
            ApplicationStatus.WITHDRAWN -> R.string.notif_application_withdrawn_title
            else -> R.string.notif_app_status_rejected_title
        }
        val msgRes = when (newStatus) {
            ApplicationStatus.APPLIED -> R.string.notif_app_status_applied_msg
            ApplicationStatus.HIRED -> R.string.notif_app_status_hired_msg
            ApplicationStatus.COMPLETED -> R.string.notif_app_status_hired_msg
            ApplicationStatus.REJECTED -> R.string.notif_app_status_rejected_msg
            ApplicationStatus.WITHDRAWN -> R.string.notif_application_withdrawn_msg
            else -> R.string.notif_app_status_rejected_msg
        }
        val title = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, titleRes)
        val message = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, msgRes)
        
        return NotificationData(
            id = UUID.randomUUID().toString(),
            recipientId = application.workerId,
            title = title,
            message = message,
            type = NotificationType.APPLICATION_STATUS,
            targetRole = "WORKER",
            data = mapOf(
                "applicationId" to application.id,
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
            title = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_new_application_title),
            message = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_new_application_msg),
            type = NotificationType.NEW_APPLICATION,
            targetRole = "EMPLOYER",
            data = mapOf(
                "applicationId" to application.id,
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
            title = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_job_posted_title),
            message = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_job_posted_msg, jobTitle),
            type = NotificationType.JOB_POSTED,
            targetRole = "EMPLOYER",
            data = mapOf(
                "jobTitle" to jobTitle,
                "action" to "view_job"
            ),
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
    }

    /**
     * Create job expiry reminder notification
     */
    private fun createJobExpiryReminderNotification(jobId: String, jobTitle: String, daysLeft: Int): NotificationData {
        return NotificationData(
            id = UUID.randomUUID().toString(),
            recipientId = "",
            title = "Job Post Expiring Soon",
            message = "Your job post for $jobTitle expires in $daysLeft days. Tap here to extend or repost.",
            type = NotificationType.JOB_EXPIRY_REMINDER,
            targetRole = "EMPLOYER",
            data = mapOf(
                "jobId" to jobId,
                "jobTitle" to jobTitle,
                "daysLeft" to daysLeft.toString(),
                "action" to "view_my_jobs"
            ),
            createdAt = System.currentTimeMillis(),
            isRead = false
        )
    }
    
    /**
     * Create profile complete notification (welcome message)
     */
    private fun createProfileCompleteNotification(userName: String, userRole: String): NotificationData {
        val title = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_profile_complete_title)
        val msgRes = if (userRole.equals("EMPLOYER", ignoreCase = true)) {
            R.string.notif_profile_complete_employer_msg
        } else {
            R.string.notif_profile_complete_worker_msg
        }
        val message = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, msgRes, userName)
        
        val action = if (userRole.equals("EMPLOYER", ignoreCase = true)) "view_employer_home" else "view_worker_home"
        
        return NotificationData(
            id = UUID.randomUUID().toString(),
            recipientId = "", // Will be set when sending
            title = title,
            message = message,
            type = NotificationType.PROFILE_COMPLETE,
            targetRole = userRole.uppercase(),
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
            title = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_worker_hired_title),
            message = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_worker_hired_msg, jobTitle),
            type = NotificationType.WORKER_HIRED,
            targetRole = "WORKER",
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
            title = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_employer_hired_title),
            message = com.example.dutype.utils.LocaleHelper.getLocalizedString(context, R.string.notif_employer_hired_msg, workerName, jobTitle),
            type = NotificationType.WORKER_HIRED,
            targetRole = "EMPLOYER",
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
            if (notification.targetRole.isNotBlank()) {
                put("targetRole", notification.targetRole.uppercase())
            }
        }
        
        // Save to Firestore - Cloud Function will handle FCM push notification
        val notificationWithRecipient = notification.copy(
            recipientId = recipientId,
            data = dataWithDeepLink,
            expiresAt = notification.createdAt + NOTIFICATION_RETENTION_MS
        )
        Timber.d("Saving notification to Firestore...")
        Timber.d("Notification to save: $notificationWithRecipient")

        // Match firestore.rules for /notifications create:
        // allowed keys: recipientId, title, message, type, data, isRead, createdAt (timestamp)
        val firestorePayload = hashMapOf<String, Any>(
            "recipientId" to notificationWithRecipient.recipientId,
            "title" to notificationWithRecipient.title,
            "message" to notificationWithRecipient.message,
            "type" to notificationWithRecipient.type.name,
            "data" to notificationWithRecipient.data,
            "isRead" to notificationWithRecipient.isRead,
            "createdAt" to com.google.firebase.Timestamp(java.util.Date(notificationWithRecipient.createdAt))
        )
        
        // Show local notification immediately for the current user (self-notifications such as
        // profile complete, job posted, worker hired confirmation, etc.)
        // Cross-user notifications (employer ← new application, worker ← status update) are
        // delivered to the OTHER device via Cloud Function → FCM.
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null && recipientId == currentUserId) {
            showLocalNotification(notificationWithRecipient)
        }

        if (!CLIENT_NOTIFICATION_WRITES_ENABLED) {
            if (currentUserId != null && recipientId == currentUserId) {
                persistSelfNotificationInBackground(notificationWithRecipient)
            } else {
                Timber.d("Skipping client notification write (CF-only rules): recipient=%s type=%s", recipientId, notification.type)
            }
            return
        }

        firestore.collection(notificationsCollection)
            .document(notificationWithRecipient.id)
            .set(firestorePayload)
            .await()

        Timber.i("Notification saved to Firestore successfully with ID: ${notificationWithRecipient.id}")
        Timber.d("Deep link included in notification data: $deepLink")
    }

    private fun persistSelfNotificationInBackground(notification: NotificationData) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val callablePayload = hashMapOf<String, Any>(
                    "notificationId" to notification.id,
                    "title" to notification.title,
                    "message" to notification.message,
                    "type" to notification.type.name,
                    "data" to notification.data,
                    "targetRole" to notification.targetRole,
                    "expiresAt" to notification.expiresAt
                )

                val callableResult = withTimeoutOrNull(SELF_NOTIFICATION_PERSIST_TIMEOUT_MS) {
                    com.google.firebase.functions.FirebaseFunctions
                        .getInstance()
                        .getHttpsCallable("persistSelfNotification")
                        .call(callablePayload)
                        .await()
                }

                if (callableResult == null) {
                    Timber.w("NotificationService: persistSelfNotification timed out for %s", notification.id)
                    return@launch
                }

                Timber.d("NotificationService: self-notification persisted via callable: %s", notification.id)
            } catch (e: Exception) {
                Timber.w(e, "NotificationService: failed to persist self-notification via callable")
            }
        }
    }
    
    /**
     * Show a local on-device notification immediately.
     * Called for self-notifications (recipient == current user) to guarantee display
     * without depending on Cloud Function → FCM round-trip.
     * Uses NotificationChannelManager channels to stay consistent with FCM payloads.
     */
    private fun showLocalNotification(notification: NotificationData) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("NotificationService: POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val deepLink = notification.data["deepLink"]
        val deepLinkUri = deepLink?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        val deepLinkHost = deepLinkUri?.host.orEmpty()
        val isExternalLink = deepLinkUri?.scheme == "market" ||
            (deepLinkUri?.scheme in listOf("http", "https") && deepLinkHost.contains("play.google.com"))

        val intent = if (deepLinkUri != null) {
            if (isExternalLink) {
                Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                    setClass(context, com.example.dutype.MainActivity::class.java)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
        } else {
            Intent(context, com.example.dutype.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("notificationId", notification.id)
            }
        }

        val notificationId = notification.id.hashCode()
        intent.putExtra("from_notification", true)
        intent.putExtra("notification_system_id", notificationId)
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when (notification.type) {
            NotificationType.APPLICATION_STATUS,
            NotificationType.APPLICATION_STATUS_UPDATE,
            NotificationType.NEW_APPLICATION,
            NotificationType.WORKER_HIRED,
            NotificationType.BIRTHDAY,
            NotificationType.APPLICATION_STATUS_UPDATE,
            NotificationType.REJECTED -> NotificationChannelManager.CHANNEL_HIGH_PRIORITY

            NotificationType.JOB_RECOMMENDATION,
            NotificationType.APPLICATION_REMINDER,
            NotificationType.INTERVIEW_SCHEDULED -> NotificationChannelManager.CHANNEL_MEDIUM_PRIORITY

            else -> NotificationChannelManager.CHANNEL_HIGH_PRIORITY
        }

        val priority = when (channelId) {
            NotificationChannelManager.CHANNEL_HIGH_PRIORITY -> NotificationCompat.PRIORITY_HIGH
            NotificationChannelManager.CHANNEL_MEDIUM_PRIORITY -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NotificationIcons.tintColor(context))
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                NotificationIcons.largeIcon(context)?.let { setLargeIcon(it) }
            }

        try {
            notificationManager.notify(notificationId, notificationBuilder.build())
            Timber.d("NotificationService: ✅ Local notification shown: ${notification.title}")
        } catch (e: SecurityException) {
            Timber.e(e, "NotificationService: ❌ Failed to show local notification")
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

    private fun inferRoleFromData(type: NotificationType, data: Map<String, Any>): String {
        @Suppress("UNCHECKED_CAST")
        val nestedData = data["data"] as? Map<String, Any>

        val action = (nestedData?.get("action") ?: data["action"])?.toString()?.uppercase() ?: ""
        val targetRole = (nestedData?.get("targetRole") ?: data["targetRole"])?.toString()?.uppercase() ?: ""
        val userRole = (nestedData?.get("userRole") ?: data["userRole"])?.toString()?.uppercase() ?: ""

        return when {
            targetRole in setOf("WORKER", "EMPLOYER") -> targetRole
            type == NotificationType.WORKER_HIRED && action == "VIEW_APPLICATIONS" -> "EMPLOYER"
            type == NotificationType.WORKER_HIRED -> "WORKER"
            type == NotificationType.PROFILE_COMPLETE && userRole.isNotBlank() -> userRole
            userRole in setOf("WORKER", "EMPLOYER") -> userRole
            else -> ""
        }
    }

    private fun parseNotificationDocument(
        doc: DocumentSnapshot,
        activeRole: String?,
        now: Long
    ): NotificationData? {
        return try {
            val data = doc.data ?: return null

            val createdAt = when (val createdAtValue = data["createdAt"]) {
                is Long -> createdAtValue
                is Timestamp -> createdAtValue.toDate().time
                else -> now
            }

            val expiresAt = when (val expiresAtValue = data["expiresAt"]) {
                is Long -> expiresAtValue
                is Timestamp -> expiresAtValue.toDate().time
                else -> createdAt + NOTIFICATION_RETENTION_MS
            }

            if (expiresAt <= now) {
                return null
            }

            val typeString = data["type"]?.toString() ?: "GENERAL"
            val type = try {
                NotificationType.valueOf(typeString.uppercase())
            } catch (e: Exception) {
                NotificationType.GENERAL
            }

            if (activeRole != null) {
                val storedRole = data["targetRole"]?.toString() ?: ""
                val inferredRole = inferRoleFromData(type, data)
                val effectiveRole = if (storedRole.isNotEmpty()) storedRole else inferredRole

                if (effectiveRole.isNotEmpty() && !effectiveRole.equals(activeRole, ignoreCase = true)) {
                    return null
                }

                if (effectiveRole.isEmpty()) {
                    when {
                        activeRole.equals("WORKER", ignoreCase = true) && type in employerTypes -> return null
                        activeRole.equals("EMPLOYER", ignoreCase = true) && type in workerTypes -> return null
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            val notificationDataMap = (data["data"] as? Map<String, Any>)?.mapValues { it.value.toString() } ?: emptyMap()
            val effectiveRole = (data["targetRole"]?.toString())
                ?.takeIf { it.isNotBlank() }
                ?: inferRoleFromData(type, data)

            NotificationData(
                id = doc.id,
                recipientId = data["recipientId"]?.toString() ?: "",
                title = data["title"]?.toString() ?: "",
                message = (data["message"] ?: data["body"])?.toString() ?: "",
                type = type,
                targetRole = effectiveRole,
                data = notificationDataMap,
                createdAt = createdAt,
                expiresAt = expiresAt,
                isRead = data["isRead"] as? Boolean ?: false
            )
        } catch (e: Exception) {
            Timber.e(e, "NotificationService - Error parsing document ${doc.id}")
            null
        }
    }
}

