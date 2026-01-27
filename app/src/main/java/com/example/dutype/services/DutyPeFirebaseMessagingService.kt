package com.example.dutype.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dutype.app.R
import com.example.dutype.MainActivity
import com.example.dutype.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service
 * Handles incoming push notifications and FCM token updates
 * 
 * NOTE: Token updates are delegated to FCMTokenManager (canonical implementation)
 * to avoid duplicate token handling logic
 */
@AndroidEntryPoint
class DutyPeFirebaseMessagingService : FirebaseMessagingService() {
    
    @Inject
    lateinit var fcmTokenManager: FCMTokenManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val CHANNEL_ID_HIGH = "dutype_high_priority"
        private const val CHANNEL_ID_DEFAULT = "dutype_default"
        private const val CHANNEL_ID_LOW = "dutype_low_priority"
        
        // Notification types
        const val TYPE_NEW_APPLICATION = "new_application"
        const val TYPE_APPLICATION_STATUS = "application_status"
        const val TYPE_JOB_UPDATE = "job_update"
        const val TYPE_MESSAGE = "message"
        const val TYPE_GENERAL = "general"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        Timber.d("FCM Service created")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM Message received from: ${remoteMessage.from}")
        
        // Handle data payload (preferred for background handling)
        if (remoteMessage.data.isNotEmpty()) {
            Timber.d("FCM Data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
        
        // Handle notification payload (only when app is in foreground)
        remoteMessage.notification?.let { notification ->
            Timber.d("FCM Notification: ${notification.title} - ${notification.body}")
            showNotification(
                title = notification.title ?: "DutyPe",
                message = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    /**
     * Handle data-only messages (works in background)
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "DutyPe"
        val message = data["message"] ?: data["body"] ?: ""
        val type = data["type"] ?: TYPE_GENERAL
        val notificationId = data["notificationId"] ?: System.currentTimeMillis().toString()
        
        // DUPLICATE FIX: Don't store in Firestore here - Cloud Function already stored it
        // This was causing duplicate notifications (one from Cloud Function, one from here)
        // storeNotificationInFirestore(data) // REMOVED
        
        // Show local notification only
        showNotification(
            title = title,
            message = message,
            data = data,
            notificationId = notificationId.hashCode()
        )
    }
    
    /**
     * Store notification in Firestore for in-app notification center
     * 
     * DEPRECATED: This method is no longer used to prevent duplicate notifications.
     * Cloud Function `sendPushNotification` already stores notifications in Firestore.
     * Keeping this method for reference but it should NOT be called.
     */
    @Deprecated("Use Cloud Function to store notifications instead", level = DeprecationLevel.ERROR)
    private fun storeNotificationInFirestore(data: Map<String, String>) {
        // REMOVED: This was causing duplicate notifications
        // Cloud Function already stores notifications in Firestore
        Timber.w("FCM: storeNotificationInFirestore() called but is deprecated - Cloud Function handles this")
    }
    
    /**
     * Show local notification
     */
    private fun showNotification(
        title: String,
        message: String,
        data: Map<String, String>,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val intent = createNotificationIntent(data)
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val channelId = getChannelForType(data["type"])
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        // Create expandable BigTextStyle for long messages
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(message)
            .setBigContentTitle(title)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(bigTextStyle)
            .setPriority(getPriorityForType(data["type"]))
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF3B82F6.toInt()) // Blue color
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // Categorize as message
        
        // Add action buttons based on type
        addActionsForType(notificationBuilder, data)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        Timber.d("FCM: Local notification shown with ID: $notificationId")
    }
    
    /**
     * Create intent for notification tap with deep link
     * Uses NotificationDeepLinkBuilder to generate proper deep links
     */
    private fun createNotificationIntent(data: Map<String, String>): Intent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        // Parse notification type
        val typeString = data["type"] ?: TYPE_GENERAL
        val notificationType = try {
            com.example.dutype.models.NotificationType.valueOf(typeString.uppercase())
        } catch (e: Exception) {
            Timber.w("Unknown notification type: $typeString, using GENERAL")
            com.example.dutype.models.NotificationType.GENERAL
        }
        
        // Build deep link using NotificationDeepLinkBuilder
        val deepLink = com.example.dutype.utils.NotificationDeepLinkBuilder.buildDeepLink(
            notificationType,
            data
        )
        
        Timber.d("📱 FCM: Built deep link for notification: $deepLink")
        
        // Set deep link as data URI
        intent.data = android.net.Uri.parse(deepLink)
        
        // Also pass notification metadata for tracking
        intent.putExtra("notification_id", data["notificationId"] ?: data["id"] ?: "")
        intent.putExtra("notification_type", typeString)
        intent.putExtra("from_notification", true)
        
        // Pass all data for further processing
        data.forEach { (key, value) ->
            intent.putExtra(key, value)
        }
        
        return intent
    }

    
    /**
     * Get notification channel based on type
     */
    private fun getChannelForType(type: String?): String {
        return when (type) {
            TYPE_NEW_APPLICATION, TYPE_APPLICATION_STATUS -> CHANNEL_ID_HIGH
            TYPE_JOB_UPDATE -> CHANNEL_ID_DEFAULT
            else -> CHANNEL_ID_DEFAULT
        }
    }
    
    /**
     * Get notification priority based on type
     */
    private fun getPriorityForType(type: String?): Int {
        return when (type) {
            TYPE_NEW_APPLICATION, TYPE_APPLICATION_STATUS -> NotificationCompat.PRIORITY_HIGH
            TYPE_JOB_UPDATE -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }
    
    /**
     * Add action buttons to notification based on type with deep links
     */
    private fun addActionsForType(builder: NotificationCompat.Builder, data: Map<String, String>) {
        val type = data["type"]
        
        // Parse notification type
        val notificationType = try {
            com.example.dutype.models.NotificationType.valueOf(type?.uppercase() ?: "GENERAL")
        } catch (e: Exception) {
            com.example.dutype.models.NotificationType.GENERAL
        }
        
        when (notificationType) {
            com.example.dutype.models.NotificationType.NEW_APPLICATION -> {
                // Add "View Application" action with deep link
                val applicationId = data["applicationId"] ?: data["application_id"]
                if (applicationId != null) {
                    val deepLink = "dutype://employer/applications/$applicationId"
                    val viewIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        this.data = android.net.Uri.parse(deepLink)
                        putExtra("from_notification", true)
                    }
                    val viewPendingIntent = PendingIntent.getActivity(
                        this,
                        System.currentTimeMillis().toInt(),
                        viewIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(0, "View Application", viewPendingIntent)
                }
            }
            
            com.example.dutype.models.NotificationType.APPLICATION_STATUS,
            com.example.dutype.models.NotificationType.APPLICATION_STATUS_UPDATE,
            com.example.dutype.models.NotificationType.SHORTLISTED,
            com.example.dutype.models.NotificationType.REJECTED -> {
                // Add "View Details" action with deep link
                val applicationId = data["applicationId"] ?: data["application_id"]
                if (applicationId != null) {
                    val deepLink = "dutype://worker/applications/$applicationId"
                    val viewIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        this.data = android.net.Uri.parse(deepLink)
                        putExtra("from_notification", true)
                    }
                    val viewPendingIntent = PendingIntent.getActivity(
                        this,
                        System.currentTimeMillis().toInt(),
                        viewIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(0, "View Details", viewPendingIntent)
                }
            }
            
            com.example.dutype.models.NotificationType.NEW_JOB_ALERT,
            com.example.dutype.models.NotificationType.JOB_RECOMMENDATION -> {
                // Add "View Job" action with deep link
                val jobId = data["jobId"] ?: data["job_id"]
                if (jobId != null) {
                    val deepLink = "dutype://job/$jobId"
                    val viewIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        this.data = android.net.Uri.parse(deepLink)
                        putExtra("from_notification", true)
                    }
                    val viewPendingIntent = PendingIntent.getActivity(
                        this,
                        System.currentTimeMillis().toInt(),
                        viewIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(0, "View Job", viewPendingIntent)
                }
            }
            
            else -> {
                // No action buttons for other types
            }
        }
    }
    
    /**
     * Called when FCM token is refreshed
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM: New token received: ${token.take(20)}...")
        
        // Delegate to FCMTokenManager (canonical implementation)
        // This avoids duplicate token update logic
        serviceScope.launch {
            try {
                fcmTokenManager.updateToken(token)
                Timber.i("FCM: Token updated via FCMTokenManager")
            } catch (e: Exception) {
                Timber.e(e, "FCM: Error updating token via FCMTokenManager")
            }
        }
    }
    
    // NOTE: updateTokenInFirestore() REMOVED - Use FCMTokenManager.updateToken() instead
    // This eliminates duplicate FCM token handling logic
    
    /**
     * Create notification channels
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // High priority channel for applications
            val highChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                "Applications & Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important notifications about job applications"
                enableVibration(true)
                enableLights(true)
            }
            
            // Default channel for general updates
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "Job Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about job postings and updates"
            }
            
            // Low priority channel
            val lowChannel = NotificationChannel(
                CHANNEL_ID_LOW,
                "General",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General app notifications"
            }
            
            notificationManager.createNotificationChannels(listOf(highChannel, defaultChannel, lowChannel))
            Timber.d("FCM: Notification channels created")
        }
    }
}
