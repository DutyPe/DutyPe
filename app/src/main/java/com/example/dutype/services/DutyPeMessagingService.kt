package com.example.dutype.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dutype.app.R
import com.example.dutype.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service - Enterprise Grade
 * 
 * Handles FCM push notifications from Cloud Functions
 * Supports foreground and background notification delivery
 * Routes notifications to appropriate channels based on priority
 * 
 * ARCHITECTURE:
 * Cloud Functions → FCM → This Service → Notification Channels → User
 *
 * PUSH-PATH DISCIPLINE: onMessageReceived MUST render the notification using
 * only the FCM payload (title, body, type, deepLink, channel). No Firestore
 * reads on this path — those belong in the app's cold-start enrichment only.
 */
@AndroidEntryPoint
class DutyPeMessagingService : FirebaseMessagingService() {

    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var auth: FirebaseAuth
    
    companion object {
        private const val NOTIFICATION_GROUP = "DUTYPE_NOTIFICATIONS"
    }
    
    /**
     * Called when a new FCM message is received
     * Handles both foreground and background messages
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Timber.d("🔔 FCM Message received from: ${remoteMessage.from}")
        
        // Extract notification data
        val notification = remoteMessage.notification
        val data = remoteMessage.data
        
        // Get title and body
        val title = notification?.title ?: data["title"] ?: return
        val body = notification?.body ?: data["body"] ?: return
        
        // Get notification metadata
        val type = data["type"] ?: "GENERAL"
        val deepLink = data["deepLink"]
        val channelId = data["channel"] ?: NotificationChannelManager.getChannelForType(type)
        
        Timber.d("🔔 Notification: type=$type, channel=$channelId")
        
        // Show notification (handles both foreground and background)
        showNotification(
            title = title,
            body = body,
            type = type,
            deepLink = deepLink,
            channelId = channelId,
            data = data
        )
    }
    
    /**
     * Called when FCM token is refreshed
     * Save new token to Firestore for server-side targeting
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        Timber.d("🔔 New FCM token: ${token.take(20)}...")
        
        // Save token to Firestore
        saveFCMToken(token)
    }
    
    /**
     * Save FCM token to Firestore (users.fcmToken field)
     * Allows Cloud Functions to send targeted notifications
     */
    private fun saveFCMToken(token: String) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Timber.w("🔔 Cannot save FCM token - user not authenticated")
            return
        }

        // Save to users.fcmToken field only (schema: fcmToken)
        firestore
            .collection(com.example.dutype.firestore.FirestoreCollections.USERS)
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Timber.d("🔔 ✅ FCM token saved to users.fcmToken")
            }
            .addOnFailureListener { e ->
                val fsError = e as? FirebaseFirestoreException
                if (fsError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    fsError?.code == FirebaseFirestoreException.Code.NOT_FOUND
                ) {
                    Timber.w("🔔 FCM token save skipped: ${fsError.code}")
                } else {
                    Timber.e(e, "🔔 ❌ Failed to save FCM token")
                }
            }
    }
    
    /**
     * Show notification to user
     * Handles foreground and background delivery
     */
    private fun showNotification(
        title: String,
        body: String,
        type: String,
        deepLink: String?,
        channelId: String,
        data: Map<String, String>
    ) {
        val notificationId = data["notificationId"]
            ?.takeIf { it.isNotBlank() }
            ?.hashCode()
            ?: System.currentTimeMillis().toInt()
        
        // Create pending intent for deep link
        val intent = createDeepLinkIntent(deepLink, notificationId)
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Determine notification priority based on channel
        val priority = when (channelId) {
            NotificationChannelManager.CHANNEL_HIGH_PRIORITY -> NotificationCompat.PRIORITY_HIGH
            NotificationChannelManager.CHANNEL_MEDIUM_PRIORITY -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
        
        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this exists
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setGroup(NOTIFICATION_GROUP)
            .setCategory(getNotificationCategory(type))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                // Add action buttons based on type
                addActionsForType(this, type, data, notificationId)
            }
            .build()
        
        // Show notification
        try {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
            Timber.d("🔔 ✅ Notification shown: $title")
        } catch (e: SecurityException) {
            Timber.e(e, "🔔 ❌ Failed to show notification - permission denied")
        }
    }
    
    /**
     * Create intent for deep link
     */
    private fun createDeepLinkIntent(deepLink: String?, notificationId: Int? = null): Intent {
        val uri = deepLink?.let { Uri.parse(it) }
        val uriHost = uri?.host.orEmpty()
        val isExternalLink = uri?.scheme == "market" ||
            (uri?.scheme in listOf("http", "https") && uriHost.contains("play.google.com"))

        if (uri != null && isExternalLink) {
            return Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                notificationId?.let { putExtra("notification_system_id", it) }
            }
        }

        return if (deepLink != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                setClass(this@DutyPeMessagingService, MainActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("from_notification", true)
                notificationId?.let { putExtra("notification_system_id", it) }
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("from_notification", true)
                notificationId?.let { putExtra("notification_system_id", it) }
            }
        }
    }
    
    /**
     * Get notification category for Android system
     */
    private fun getNotificationCategory(type: String): String {
        return when (type) {
            "BIRTHDAY" -> NotificationCompat.CATEGORY_EVENT
            "JOB_EXPIRY", "JOB_ALERT", "JOB_POSTED", "JOB_UPDATE" -> NotificationCompat.CATEGORY_REMINDER
            "APPLICATION_STATUS", "APPLICATION_STATUS_UPDATE", "SHORTLISTED",
            "REJECTED", "NEW_APPLICATION", "WORKER_HIRED" -> NotificationCompat.CATEGORY_STATUS
            "PROFILE_COMPLETE", "WELCOME" -> NotificationCompat.CATEGORY_RECOMMENDATION
            "PENDING_APPLICATIONS" -> NotificationCompat.CATEGORY_REMINDER
            "RE_ENGAGEMENT", "GUEST_ENGAGEMENT" -> NotificationCompat.CATEGORY_RECOMMENDATION
            else -> NotificationCompat.CATEGORY_MESSAGE
        }
    }
    
    /**
     * Add action buttons based on notification type
     */
    private fun addActionsForType(
        builder: NotificationCompat.Builder,
        type: String,
        data: Map<String, String>,
        notificationId: Int
    ) {
        when (type) {
            "JOB_EXPIRY" -> {
                // Add "Renew Job" action
                val jobId = data["jobId"]
                if (jobId != null) {
                    val renewIntent = createDeepLinkIntent("dutype://job/$jobId/renew", notificationId)
                    val renewPendingIntent = PendingIntent.getActivity(
                        this,
                        notificationId + 1,
                        renewIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    builder.addAction(
                        R.drawable.ic_notification,
                        "Renew Job",
                        renewPendingIntent
                    )
                }
            }
            "PENDING_APPLICATIONS" -> {
                // Add "View Applications" action
                val viewIntent = createDeepLinkIntent("dutype://employer/applications", notificationId)
                val viewPendingIntent = PendingIntent.getActivity(
                    this,
                    notificationId + 1,
                    viewIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(
                    R.drawable.ic_notification,
                    "View Applications",
                    viewPendingIntent
                )
            }
            "RE_ENGAGEMENT",
            "GUEST_ENGAGEMENT" -> {
                // Add "Browse Jobs" action for guest & re-engagement nudges
                val browseIntent = createDeepLinkIntent("dutype://jobs", notificationId)
                val browsePendingIntent = PendingIntent.getActivity(
                    this,
                    notificationId + 1,
                    browseIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(
                    R.drawable.ic_notification,
                    "Browse Jobs",
                    browsePendingIntent
                )
            }
            "APPLICATION_STATUS",
            "APPLICATION_STATUS_UPDATE",
            "SHORTLISTED",
            "REJECTED" -> {
                val applicationId = data["applicationId"]
                val dest = if (!applicationId.isNullOrEmpty()) "dutype://worker/applications/$applicationId"
                           else "dutype://worker/applications"
                val actionIntent = createDeepLinkIntent(dest, notificationId)
                val actionPendingIntent = PendingIntent.getActivity(
                    this, notificationId + 1, actionIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(R.drawable.ic_notification, "View Application", actionPendingIntent)
            }
            "NEW_APPLICATION" -> {
                val applicationId = data["applicationId"]
                val jobId = data["jobId"]
                val dest = when {
                    !applicationId.isNullOrEmpty() -> "dutype://employer/applications/$applicationId"
                    !jobId.isNullOrEmpty() -> "dutype://employer/jobs/$jobId"
                    else -> "dutype://employer/applications"
                }
                val actionIntent = createDeepLinkIntent(dest, notificationId)
                val actionPendingIntent = PendingIntent.getActivity(
                    this, notificationId + 1, actionIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(R.drawable.ic_notification, "Review Now", actionPendingIntent)
            }
            "WORKER_HIRED" -> {
                val jobId = data["jobId"]
                val dest = if (!jobId.isNullOrEmpty()) "dutype://employer/jobs/$jobId"
                           else "dutype://employer/applications"
                val actionIntent = createDeepLinkIntent(dest, notificationId)
                val actionPendingIntent = PendingIntent.getActivity(
                    this, notificationId + 1, actionIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(R.drawable.ic_notification, "View Job", actionPendingIntent)
            }
            "JOB_POSTED", "JOB_UPDATE", "JOB_EXPIRY_REMINDER" -> {
                val jobId = data["jobId"]
                val dest = if (!jobId.isNullOrEmpty()) "dutype://employer/jobs/$jobId"
                           else "dutype://employer/jobs"
                val actionIntent = createDeepLinkIntent(dest, notificationId)
                val actionPendingIntent = PendingIntent.getActivity(
                    this, notificationId + 1, actionIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(R.drawable.ic_notification, "Manage Job", actionPendingIntent)
            }
            "PROFILE_COMPLETE", "WELCOME" -> {
                val role = data["userRole"] ?: ""
                val dest = if (role.equals("EMPLOYER", ignoreCase = true))
                    "dutype://employer/dashboard" else "dutype://worker/jobs"
                val actionIntent = createDeepLinkIntent(dest, notificationId)
                val actionPendingIntent = PendingIntent.getActivity(
                    this, notificationId + 1, actionIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(R.drawable.ic_notification, "Get Started", actionPendingIntent)
            }
        }
    }
    
    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
