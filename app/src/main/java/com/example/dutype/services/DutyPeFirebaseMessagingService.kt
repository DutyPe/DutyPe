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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Firebase Cloud Messaging Service
 * Handles incoming push notifications and FCM token updates
 */
class DutyPeFirebaseMessagingService : FirebaseMessagingService() {
    
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
        
        // Store notification in Firestore for in-app display
        storeNotificationInFirestore(data)
        
        // Show local notification
        showNotification(
            title = title,
            message = message,
            data = data,
            notificationId = notificationId.hashCode()
        )
    }
    
    /**
     * Store notification in Firestore for in-app notification center
     */
    private fun storeNotificationInFirestore(data: Map<String, String>) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        serviceScope.launch {
            try {
                val notificationData = mapOf(
                    "id" to (data["notificationId"] ?: System.currentTimeMillis().toString()),
                    "recipientId" to userId,
                    "title" to (data["title"] ?: "DutyPe"),
                    "message" to (data["message"] ?: data["body"] ?: ""),
                    "type" to (data["type"] ?: TYPE_GENERAL),
                    "data" to data,
                    "createdAt" to System.currentTimeMillis(),
                    "isRead" to false
                )
                
                firestore.collection("notifications")
                    .document(notificationData["id"] as String)
                    .set(notificationData)
                    .await()
                
                Timber.d("FCM: Notification stored in Firestore")
            } catch (e: Exception) {
                Timber.e(e, "FCM: Error storing notification in Firestore")
            }
        }
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
     * Create intent for notification tap
     */
    private fun createNotificationIntent(data: Map<String, String>): Intent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val action = data["action"] ?: ""
        val jobId = data["jobId"] ?: ""
        val applicationId = data["applicationId"] ?: ""
        val type = data["type"] ?: ""
        
        // Determine navigation route based on action and type
        val navigateTo = when (action) {
            "view_application" -> {
                if (applicationId.isNotEmpty()) {
                    Routes.EMPLOYER_APPLICATION_DETAIL.replace("{applicationId}", applicationId)
                } else {
                    Routes.EMPLOYER_APPLICATIONS
                }
            }
            "view_job" -> {
                if (jobId.isNotEmpty()) Routes.jobDetailRoute(jobId) else Routes.WORKER_HOME
            }
            "view_applications" -> Routes.WORKER_MY_JOBS
            "complete_profile" -> Routes.PROFILE_SETUP
            "view_employer_profile" -> Routes.EMPLOYER_PROFILE_SETUP
            "view_employer_home" -> Routes.EMPLOYER_HOME
            "view_worker_home" -> Routes.WORKER_HOME
            else -> {
                // Fallback based on notification type
                when (type) {
                    TYPE_NEW_APPLICATION -> Routes.EMPLOYER_APPLICATIONS
                    TYPE_APPLICATION_STATUS -> Routes.WORKER_MY_JOBS
                    TYPE_JOB_UPDATE -> if (jobId.isNotEmpty()) Routes.jobDetailRoute(jobId) else null
                    "profile_complete", "welcome" -> null // Navigate to home based on role
                    "worker_hired" -> Routes.WORKER_MY_JOBS
                    else -> null
                }
            }
        }
        
        navigateTo?.let { intent.putExtra("navigate_to", it) }
        
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
     * Add action buttons to notification based on type
     */
    private fun addActionsForType(builder: NotificationCompat.Builder, data: Map<String, String>) {
        val type = data["type"]
        
        when (type) {
            TYPE_NEW_APPLICATION -> {
                // Add "View" action for new applications
                val viewIntent = createNotificationIntent(data.toMutableMap().apply {
                    put("action", "view_application")
                })
                val viewPendingIntent = PendingIntent.getActivity(
                    this,
                    System.currentTimeMillis().toInt(),
                    viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, "View Application", viewPendingIntent)
            }
            TYPE_APPLICATION_STATUS -> {
                // Add "View Details" action
                val viewIntent = createNotificationIntent(data.toMutableMap().apply {
                    put("action", "view_applications")
                })
                val viewPendingIntent = PendingIntent.getActivity(
                    this,
                    System.currentTimeMillis().toInt(),
                    viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, "View Details", viewPendingIntent)
            }
        }
    }
    
    /**
     * Called when FCM token is refreshed
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM: New token received: ${token.take(20)}...")
        
        // Update token in Firestore
        serviceScope.launch {
            updateTokenInFirestore(token)
        }
    }
    
    /**
     * Update FCM token in Firestore
     */
    private suspend fun updateTokenInFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Timber.w("FCM: No user logged in, token update skipped")
            return
        }
        
        try {
            val tokenData = mapOf(
                "fcmToken" to token,
                "fcmTokenUpdatedAt" to System.currentTimeMillis(),
                "platform" to "android"
            )
            
            // Update in users collection
            firestore.collection("users")
                .document(userId)
                .set(tokenData, SetOptions.merge())
                .await()
            
            // Update in fcm_tokens collection
            firestore.collection("fcm_tokens")
                .document(userId)
                .set(mapOf(
                    "token" to token,
                    "userId" to userId,
                    "updatedAt" to System.currentTimeMillis(),
                    "platform" to "android",
                    "isActive" to true
                ))
                .await()
            
            Timber.i("FCM: Token updated in Firestore for user: $userId")
        } catch (e: Exception) {
            Timber.e(e, "FCM: Error updating token in Firestore")
        }
    }
    
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
