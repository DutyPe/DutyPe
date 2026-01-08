package com.example.dutype.services

import com.example.dutype.models.NotificationData
import com.example.dutype.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Birthday Service - Checks if today is user's birthday and sends wishes
 * Simple and delightful feature to make users happy! 🎂
 */
@Singleton
class BirthdayService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val notificationService: NotificationService
) {
    
    companion object {
        private const val BIRTHDAY_PREFS = "birthday_prefs"
        private const val LAST_WISH_DATE_KEY = "last_wish_date"
    }
    
    /**
     * Check if today is user's birthday
     * Returns user's name if it's their birthday, null otherwise
     */
    suspend fun checkIfBirthday(userId: String): BirthdayInfo? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            
            if (!userDoc.exists()) return null
            
            val dateOfBirth = userDoc.getString("dateOfBirth") ?: return null
            val fullName = userDoc.getString("fullName") ?: "Friend"
            
            // Parse date of birth (format: "DD/MM/YYYY" or "YYYY-MM-DD")
            val today = Calendar.getInstance()
            val todayDay = today.get(Calendar.DAY_OF_MONTH)
            val todayMonth = today.get(Calendar.MONTH) + 1 // Calendar months are 0-indexed
            
            val (birthDay, birthMonth) = parseDateOfBirth(dateOfBirth) ?: return null
            
            if (birthDay == todayDay && birthMonth == todayMonth) {
                Timber.d("🎂 Today is $fullName's birthday!")
                BirthdayInfo(
                    userName = fullName.split(" ").firstOrNull() ?: fullName,
                    fullName = fullName
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking birthday")
            null
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
     * Send birthday notification to user
     */
    suspend fun sendBirthdayNotification(userId: String, userName: String): Result<Unit> {
        return try {
            val notification = NotificationData(
                id = UUID.randomUUID().toString(),
                recipientId = userId,
                title = "🎂 Happy Birthday, $userName! 🎉",
                message = "Wishing you a wonderful birthday filled with joy and success! May this year bring you amazing opportunities. - Team DutyPe",
                type = NotificationType.BIRTHDAY,
                data = mapOf(
                    "userName" to userName,
                    "action" to "birthday_wish"
                ),
                createdAt = System.currentTimeMillis(),
                isRead = false
            )
            
            notificationService.sendNotification(notification, userId)
            Timber.i("🎂 Birthday notification sent to $userName")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send birthday notification")
            Result.failure(e)
        }
    }
    
    /**
     * Check if we already sent birthday wish today (to avoid duplicates)
     */
    fun hasWishedToday(context: android.content.Context, userId: String): Boolean {
        val prefs = context.getSharedPreferences(BIRTHDAY_PREFS, android.content.Context.MODE_PRIVATE)
        val lastWishDate = prefs.getString("${LAST_WISH_DATE_KEY}_$userId", null)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return lastWishDate == today
    }
    
    /**
     * Mark that we've sent birthday wish today
     */
    fun markWishedToday(context: android.content.Context, userId: String) {
        val prefs = context.getSharedPreferences(BIRTHDAY_PREFS, android.content.Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit().putString("${LAST_WISH_DATE_KEY}_$userId", today).apply()
    }
}

/**
 * Birthday info data class
 */
data class BirthdayInfo(
    val userName: String,  // First name for greeting
    val fullName: String   // Full name
)
