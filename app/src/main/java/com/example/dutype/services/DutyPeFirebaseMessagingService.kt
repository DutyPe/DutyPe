package com.example.dutype.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.dutype.MainActivity
import com.example.dutype.R
import com.example.dutype.navigation.Routes
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
class DutyPeFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload
        val data = remoteMessage.data
        val title = data["title"] ?: "DutyPe Notification"
        val message = data["message"] ?: ""
        val notificationId = data["notificationId"] ?: System.currentTimeMillis().toString()
        val action = data["action"] ?: ""
        val jobId = data["jobId"] ?: ""
        val applicationId = data["applicationId"] ?: ""
        val userId = data["userId"] ?: ""

        // Show local notification
        showNotification(
            notificationId = notificationId,
            title = title,
            message = message,
            action = action,
            jobId = jobId,
            applicationId = applicationId,
            userId = userId
        )
    }

    private fun showNotification(
        notificationId: String,
        title: String,
        message: String,
        action: String,
        jobId: String,
        applicationId: String,
        userId: String
    ) {
        val intent = createNotificationIntent(action, jobId, applicationId, userId)
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId.hashCode(), notification)
    }

    private fun createNotificationIntent(
        action: String,
        jobId: String,
        applicationId: String,
        userId: String
    ): Intent {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        // Add navigation data based on action
        when (action) {
            "view_application" -> {
                if (applicationId.isNotEmpty()) {
                    intent.putExtra("navigate_to", Routes.EMPLOYER_APPLICATION_DETAIL.replace("{applicationId}", applicationId))
                } else {
                    intent.putExtra("navigate_to", Routes.EMPLOYER_APPLICATIONS)
                }
            }
            "view_job" -> {
                if (jobId.isNotEmpty()) {
                    intent.putExtra("navigate_to", Routes.jobDetailRoute(jobId))
                }
            }
            "view_chat" -> {
                // Navigate to chat or messages
                intent.putExtra("navigate_to", Routes.EMPLOYER_HOME)
            }
            "extend_job" -> {
                if (jobId.isNotEmpty()) {
                    intent.putExtra("navigate_to", Routes.editJobRoute(jobId))
                }
            }
            "complete_profile" -> {
                // Navigate to profile setup
                intent.putExtra("navigate_to", Routes.EMPLOYER_PROFILE_SETUP)
            }
            "view_summary" -> {
                intent.putExtra("navigate_to", Routes.ANALYTICS)
            }
            else -> {
                // Default navigation to home
                intent.putExtra("navigate_to", Routes.EMPLOYER_HOME)
            }
        }
        
        // Store notification data for processing
        intent.putExtra("notification_action", action)
        intent.putExtra("job_id", jobId)
        intent.putExtra("application_id", applicationId)
        intent.putExtra("user_id", userId)
        
        return intent
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DutyPe Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for job applications, updates, and messages"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server if needed
        // You can implement token registration here
    }

    companion object {
        private const val CHANNEL_ID = "dutype_notifications"
    }
}
