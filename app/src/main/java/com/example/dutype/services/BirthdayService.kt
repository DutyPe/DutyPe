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
     * Check if today is the user's birthday.
    * Reads `dateOfBirth` from `worker_profiles/{userId}` and matches
    * day+month against today.
     *
     * Accepted DOB formats: "DD/MM/YYYY", "DD-MM-YYYY", "YYYY-MM-DD".
     */
    suspend fun checkIfBirthday(userId: String): BirthdayInfo? {
        return try {
            val (dob, displayName) = readDobAndName(userId) ?: return null
            val (day, month) = parseDayMonth(dob) ?: return null

            val cal = Calendar.getInstance()
            val todayDay = cal.get(Calendar.DAY_OF_MONTH)
            val todayMonth = cal.get(Calendar.MONTH) + 1 // Calendar months are 0-based

            if (day == todayDay && month == todayMonth) {
                val firstName = displayName.trim().split(" ").firstOrNull().orEmpty()
                BirthdayInfo(
                    userName = firstName.ifBlank { displayName },
                    fullName = displayName
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "Birthday check failed for $userId")
            null
        }
    }

    private suspend fun readDobAndName(userId: String): Pair<String, String>? {
        // Try worker profile first.
        runCatching {
            val workerSnap = firestore.collection("worker_profiles").document(userId).get().await()
            if (workerSnap.exists()) {
                val dob = (workerSnap.getString("dateOfBirth") ?: "").trim()
                val name = (workerSnap.getString("fullName")
                    ?: workerSnap.getString("name")
                    ?: "").trim()
                if (dob.isNotBlank()) return Pair(dob, name)
            }
        }
        return null
    }

    private fun parseDayMonth(dob: String): Pair<Int, Int>? {
        val patterns = listOf("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "dd/MM/yy", "d/M/yyyy")
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }
                val date = sdf.parse(dob) ?: continue
                val cal = Calendar.getInstance().apply { time = date }
                return Pair(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
            } catch (_: Exception) { /* try next pattern */ }
        }
        return null
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
