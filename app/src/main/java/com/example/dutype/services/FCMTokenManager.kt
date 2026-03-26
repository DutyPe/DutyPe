package com.example.dutype.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FCM Token Manager
 * Handles FCM token registration, storage, and updates for push notifications
 * Supports topic-based messaging for role-based broadcast notifications
 * 
 * REFACTORED: Now receives Firebase dependencies via constructor injection
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class FCMTokenManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    companion object {
        // Topic names for role-based notifications
        const val TOPIC_ALL_USERS = "all_users"
        const val TOPIC_GUEST_USERS = "guest_users"
        const val TOPIC_WORKERS = "workers"
        const val TOPIC_EMPLOYERS = "employers"
        const val TOPIC_APP_UPDATES = "app_updates"
        const val TOPIC_PROMOTIONS = "promotions"
    }
    
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
            
            // Subscribe to all_users topic by default
            subscribeToTopic(TOPIC_ALL_USERS)
            unsubscribeFromTopic(TOPIC_GUEST_USERS)
            
            Result.success(token)
        } catch (e: Exception) {
            Timber.e(e, "FCMTokenManager: Error registering FCM token")
            Result.failure(e)
        }
    }
    
    /**
     * Register FCM token with role-based topic subscription
     * Call this after profile setup when role is known
     */
    suspend fun registerTokenWithRole(role: String): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Timber.w("FCMTokenManager: No authenticated user, cannot register token")
                return Result.failure(Exception("User not authenticated"))
            }
            
            val token = FirebaseMessaging.getInstance().token.await()
            Timber.d("FCMTokenManager: Got FCM token: ${token.take(20)}...")
            
            // Save token with role info
            saveTokenToFirestoreWithRole(userId, token, role)
            
            // Subscribe to role-based topics
            subscribeToRoleTopics(role)
            unsubscribeFromTopic(TOPIC_GUEST_USERS)
            
            Result.success(token)
        } catch (e: Exception) {
            Timber.e(e, "FCMTokenManager: Error registering FCM token with role")
            Result.failure(e)
        }
    }
    
    /**
     * Subscribe to topics based on user role
     */
    fun subscribeToRoleTopics(role: String) {
        // Subscribe to all users topic
        subscribeToTopic(TOPIC_ALL_USERS)
        subscribeToTopic(TOPIC_APP_UPDATES)
        
        // Subscribe to role-specific topic
        when (role.uppercase()) {
            "WORKER" -> {
                subscribeToTopic(TOPIC_WORKERS)
                unsubscribeFromTopic(TOPIC_EMPLOYERS) // Ensure not subscribed to wrong topic
                Timber.i("FCMTokenManager: Subscribed to WORKER topics")
            }
            "EMPLOYER" -> {
                subscribeToTopic(TOPIC_EMPLOYERS)
                unsubscribeFromTopic(TOPIC_WORKERS) // Ensure not subscribed to wrong topic
                Timber.i("FCMTokenManager: Subscribed to EMPLOYER topics")
            }
        }
    }
    
    /**
     * Save FCM token to Firestore for the user
     */
    suspend fun saveTokenToFirestore(userId: String, token: String) {
        try {
            val tokenData = mapOf(
                "fcmToken" to token,
                "lastActiveAt" to Timestamp.now()
            )
            firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                .document(userId)
                .update(tokenData)
                .await()
            Timber.i("FCMTokenManager: Token saved for user: $userId")
        } catch (e: Exception) {
            Timber.e(e, "FCMTokenManager: Error saving token to Firestore")
            throw e
        }
    }
    
    /**
     * Save FCM token with role information
     */
    suspend fun saveTokenToFirestoreWithRole(userId: String, token: String, role: String) {
        try {
            val tokenData = mapOf(
                "fcmToken" to token,
                "lastActiveAt" to Timestamp.now()
            )
            firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                .document(userId)
                .update(tokenData)
                .await()
            Timber.i("FCMTokenManager: Token with role saved for user: $userId, role: $role")
        } catch (e: Exception) {
            Timber.e(e, "FCMTokenManager: Error saving token with role to Firestore")
            throw e
        }
    }

    
    /**
     * Remove FCM token when user logs out
     */
    suspend fun removeToken() {
        try {
            val userId = auth.currentUser?.uid ?: return
            
            firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS)
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
            // OPTIMIZED: Read from users.fcmToken field (no separate fcm_tokens collection)
            val doc = firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                .document(userId)
                .get()
                .await()
            
            if (doc.exists()) {
                doc.getString("fcmToken")
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
