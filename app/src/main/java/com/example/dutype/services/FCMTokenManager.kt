package com.example.dutype.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM Token Manager
 * Handles FCM token registration, storage, and updates for push notifications
 */
@Singleton
class FCMTokenManager @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Register FCM token for the current user
     * Call this after successful login/signup
     */
    suspend fun registerToken(): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Timber.w("FCMTokenManager: No authenticated user, cannot register token")
                return Result.failure(Exception("User not authenticated"))
            }
            
            val token = FirebaseMessaging.getInstance().token.await()
            Timber.d("FCMTokenManager: Got FCM token: ${token.take(20)}...")
            
            saveTokenToFirestore(userId, token)
            Result.success(token)
        } catch (e: Exception) {
            Timber.e(e, "FCMTokenManager: Error registering FCM token")
            Result.failure(e)
        }
    }
    
    /**
     * Save FCM token to Firestore for the user
     */
    suspend fun saveTokenToFirestore(userId: String, token: String) {
        try {
            val tokenData = mapOf(
                "fcmToken" to token,
                "fcmTokenUpdatedAt" to System.currentTimeMillis(),
                "platform" to "android"
            )
            
            // Save to users collection
            firestore.collection("users")
                .document(userId)
                .set(tokenData, SetOptions.merge())
                .await()
            
            // Also save to fcm_tokens collection for easier querying
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
            
            Timber.i("FCMTokenManager: Token saved successfully for user: $userId")
        } catch (e: Exception) {
            Timber.e(e, "FCMTokenManager: Error saving token to Firestore")
            throw e
        }
    }

    
    /**
     * Remove FCM token when user logs out
     */
    suspend fun removeToken() {
        try {
            val userId = auth.currentUser?.uid ?: return
            
            // Mark token as inactive
            firestore.collection("fcm_tokens")
                .document(userId)
                .update("isActive", false)
                .await()
            
            // Remove from users collection
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", null)
                .await()
            
            Timber.i("FCMTokenManager: Token removed for user: $userId")
        } catch (e: Exception) {
            Timber.e(e, "FCMTokenManager: Error removing token")
        }
    }
    
    /**
     * Update token when it changes (called from FirebaseMessagingService.onNewToken)
     */
    suspend fun updateToken(newToken: String) {
        try {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                saveTokenToFirestore(userId, newToken)
                Timber.i("FCMTokenManager: Token updated for user: $userId")
            } else {
                Timber.w("FCMTokenManager: No user logged in, token update skipped")
            }
        } catch (e: Exception) {
            Timber.e(e, "FCMTokenManager: Error updating token")
        }
    }
    
    /**
     * Get FCM token for a specific user (for sending notifications)
     */
    suspend fun getTokenForUser(userId: String): String? {
        return try {
            val doc = firestore.collection("fcm_tokens")
                .document(userId)
                .get()
                .await()
            
            if (doc.exists() && doc.getBoolean("isActive") == true) {
                doc.getString("token")
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "FCMTokenManager: Error getting token for user: $userId")
            null
        }
    }
    
    /**
     * Subscribe to topic for broadcast notifications
     */
    fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnSuccessListener {
                Timber.i("FCMTokenManager: Subscribed to topic: $topic")
            }
            .addOnFailureListener { e ->
                Timber.e(e, "FCMTokenManager: Failed to subscribe to topic: $topic")
            }
    }
    
    /**
     * Unsubscribe from topic
     */
    fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnSuccessListener {
                Timber.i("FCMTokenManager: Unsubscribed from topic: $topic")
            }
            .addOnFailureListener { e ->
                Timber.e(e, "FCMTokenManager: Failed to unsubscribe from topic: $topic")
            }
    }
}
