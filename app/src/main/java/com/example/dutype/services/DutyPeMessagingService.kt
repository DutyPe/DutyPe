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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

/**
 * Firebase Cloud Messaging Service - Enterprise Grade
 * 
 * Handles FCM push notifications from Cloud Functions
 * Supports foreground and background notification delivery
 * Routes notifications to appropriate channels based on priority
 * 
 * ARCHITECTURE:
 * Cloud Functions → FCM → This Service → Notification Channels → User
 */
class DutyPeMessagingService : FirebaseMessagingService() {
    
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        
        if (userId == null) {
            Timber.w("🔔 Cannot save FCM token - user not authenticated")
            return
        }
        
        // OPTIMIZED: Save to users.fcmToken field (no separate collection)
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update(
                "fcmToken", token,
                "fcmTokenUpdatedAt", System.currentTimeMillis()
            )
            .addOnSuccessListener {
                Timber.d("🔔 ✅ FCM token saved to users.fcmToken")
            }
            .addOnFailureListener { e ->
                Timber.e(e, "🔔 ❌ Failed to save FCM token")
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
        val notificationId = System.currentTimeMillis().toInt()
        
        // Create pending intent for deep link
        val intent = createDeepLinkIntent(deepLink)
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
    private fun createDeepLinkIntent(deepLink: String?): Intent {
        return if (deepLink != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                setClass(this@DutyPeMessagingService, MainActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }
    
    /**
     * Get notification category for Android system
     */
    private fun getNotificationCategory(type: String): String {
        return when (type) {
            "BIRTHDAY" -> NotificationCompat.CATEGORY_EVENT
            "JOB_EXPIRY", "JOB_ALERT" -> NotificationCompat.CATEGORY_REMINDER
            "APPLICATION_STATUS" -> NotificationCompat.CATEGORY_STATUS
            "PENDING_APPLICATIONS" -> NotificationCompat.CATEGORY_REMINDER
            "RE_ENGAGEMENT" -> NotificationCompat.CATEGORY_RECOMMENDATION
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
                    val renewIntent = createDeepLinkIntent("dutype://job/$jobId/renew")
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
                val viewIntent = createDeepLinkIntent("dutype://applications")
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
            "RE_ENGAGEMENT" -> {
                // Add "Browse Jobs" action
                val browseIntent = createDeepLinkIntent("dutype://jobs")
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
