package com.example.dutype.services

import com.dutype.app.R
import android.content.Context
import com.example.dutype.models.JobListing
import com.example.dutype.models.NotificationData
import com.example.dutype.models.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart Notification Manager
 * 
 * Sends milestone and location-based notifications.
 * Re-engagement and time-based reminders are handled server-side by Cloud Functions.
 */
@Singleton
class SmartNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val notificationService: NotificationService
) {
    
    /**
     * Notify user about profile completion milestone
     * Only triggers at 75% (reminder) and 100% (celebration)
     */
    suspend fun notifyProfileMilestone(userId: String, completionPercentage: Int): Result<Unit> {
        return try {
            val title = when (completionPercentage) {
                75 -> "Almost Done! ðŸš€"
                100 -> "Profile Complete! ðŸŽ‰"
                else -> return Result.success(Unit) // Only 75% and 100%
            }
            
            val message = when (completionPercentage) {
                75 -> "Just a few more steps! Complete your profile to get more job opportunities."
                100 -> "Congratulations! Your profile is complete. Start applying to jobs!"
                else -> ""
            }
            
            val notification = NotificationData(
                id = UUID.randomUUID().toString(),
                recipientId = userId,
                title = title,
                message = message,
                type = NotificationType.PROFILE_MILESTONE,
                data = mapOf(
                    "completionPercentage" to completionPercentage.toString(),
                    "action" to "view_profile"
                ),
                createdAt = System.currentTimeMillis(),
                isRead = false
            )
            
            notificationService.sendNotification(notification, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error sending profile milestone notification")
            Result.failure(e)
        }
    }
    
    /**
     * Notify user about referral milestone
     * Industry pattern: Paytm/PhonePe notify about cashback earned
     */
    suspend fun notifyReferralMilestone(userId: String, referralCount: Int, rewardAmount: Int): Result<Unit> {
        return try {
            val notification = NotificationData(
                id = UUID.randomUUID().toString(),
                recipientId = userId,
                title = context.getString(R.string.referral_reward_earned_title),
                message = context.getString(R.string.referral_reward_earned_message, rewardAmount, referralCount),
                type = NotificationType.REFERRAL_MILESTONE,
                data = mapOf(
                    "referralCount" to referralCount.toString(),
                    "rewardAmount" to rewardAmount.toString(),
                    "action" to "view_referrals"
                ),
                createdAt = System.currentTimeMillis(),
                isRead = false
            )
            
            notificationService.sendNotification(notification, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error sending referral milestone notification")
            Result.failure(e)
        }
    }
}
