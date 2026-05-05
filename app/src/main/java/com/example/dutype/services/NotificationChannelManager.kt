package com.example.dutype.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Notification Channel Manager - Enterprise Grade
 * 
 * Manages Android notification channels for different priority levels
 * Follows Android best practices and enterprise patterns (Swiggy/Zomato/LinkedIn)
 * 
 * CHANNELS:
 * - HIGH: Job alerts, application updates, birthday wishes (sound + vibration)
 * - MEDIUM: Reminders, recommendations (sound only)
 * - LOW: Re-engagement, tips (silent)
 */
object NotificationChannelManager {
    
    // Channel IDs (must match Cloud Functions)
    const val CHANNEL_HIGH_PRIORITY = "high_priority"
    const val CHANNEL_MEDIUM_PRIORITY = "medium_priority"
    const val CHANNEL_LOW_PRIORITY = "low_priority"
    
    // Channel Names (user-visible)
    private const val CHANNEL_HIGH_NAME = "Important Alerts"
    private const val CHANNEL_MEDIUM_NAME = "Reminders"
    private const val CHANNEL_LOW_NAME = "Updates & Tips"
    
    // Channel Descriptions (user-visible)
    private const val CHANNEL_HIGH_DESC = "Job alerts, application updates, and special occasions"
    private const val CHANNEL_MEDIUM_DESC = "Job reminders and recommendations"
    private const val CHANNEL_LOW_DESC = "App updates, tips, and re-engagement messages"
    
    /**
     * Create all notification channels
     * Call this once during app initialization
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create all channels
            val channels = listOf(
                createHighPriorityChannel(),
                createMediumPriorityChannel(),
                createLowPriorityChannel()
            )
            
            notificationManager.createNotificationChannels(channels)
            
            android.util.Log.d("NotificationChannels", "✅ Created ${channels.size} notification channels")
        }
    }
    
    /**
     * HIGH PRIORITY CHANNEL
     * For: Job alerts, application updates, birthday wishes
     * Behavior: Sound + Vibration + LED + Badge
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createHighPriorityChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_HIGH_PRIORITY,
            CHANNEL_HIGH_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_HIGH_DESC
            
            // Visual indicators
            enableLights(true)
            lightColor = Color.RED
            
            // Sound & Vibration
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500) // Vibrate pattern
            setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            
            // Badge
            setShowBadge(true)
            
            // Lock screen visibility
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
    }
    
    /**
     * MEDIUM PRIORITY CHANNEL
     * For: Reminders, recommendations
     * Behavior: Sound + Badge (no vibration)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createMediumPriorityChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_MEDIUM_PRIORITY,
            CHANNEL_MEDIUM_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_MEDIUM_DESC
            
            // Visual indicators
            enableLights(true)
            lightColor = Color.YELLOW
            
            // Sound only (no vibration)
            enableVibration(false)
            setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            
            // Badge
            setShowBadge(true)
            
            // Lock screen visibility
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
    }
    
    /**
     * LOW PRIORITY CHANNEL
     * For: Re-engagement, tips, updates
     * Behavior: Silent (no sound, no vibration)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLowPriorityChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_LOW_PRIORITY,
            CHANNEL_LOW_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_LOW_DESC
            
            // No visual indicators
            enableLights(false)
            
            // Silent (no sound, no vibration)
            enableVibration(false)
            setSound(null, null)
            
            // No badge
            setShowBadge(false)
            
            // Lock screen visibility
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
    }
    
    /**
     * Get channel ID for notification type
     * Maps notification types to appropriate channels
     */
    fun getChannelForType(notificationType: String): String {
        return when (notificationType) {
            // HIGH PRIORITY
            "BIRTHDAY",
            "JOB_EXPIRY",
            "APPLICATION_STATUS",
            "JOB_ALERT",
            "NEW_JOB_ALERT",
            "EMPLOYER_MESSAGE" -> CHANNEL_HIGH_PRIORITY
            
            // MEDIUM PRIORITY
            "PENDING_APPLICATIONS",
            "JOB_RECOMMENDATION",
            "REMINDER" -> CHANNEL_MEDIUM_PRIORITY
            
            // LOW PRIORITY
            "RE_ENGAGEMENT",
            "GUEST_ENGAGEMENT",
            "TIP",
            "UPDATE" -> CHANNEL_LOW_PRIORITY
            
            // Default to medium
            else -> CHANNEL_MEDIUM_PRIORITY
        }
    }
    
    /**
     * Check if notification channels are enabled
     * Returns true if at least one channel is enabled
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Check if notifications are enabled globally
            if (!notificationManager.areNotificationsEnabled()) {
                return false
            }
            
            // Check if at least one channel is enabled
            val channels = notificationManager.notificationChannels
            return channels.any { it.importance != NotificationManager.IMPORTANCE_NONE }
        }
        
        // Pre-Oreo: Check global notification setting
        return androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    
    /**
     * Open notification settings for the app
     * Allows users to manage notification preferences
     */
    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        }
        
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
