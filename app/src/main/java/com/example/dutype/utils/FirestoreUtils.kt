package com.example.dutype.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Utility functions for Firestore database operations
 */
object FirestoreUtils {
    
    /**
     * Check if a user exists by phone number in Firestore
     * Searches both "phoneNumber" and "phone" fields for compatibility
     * 
     * @param phoneNumber The phone number to search for
     * @return User data map if found, null otherwise
     */
    suspend fun checkUserExistsByPhoneNumber(phoneNumber: String): Map<String, Any?>? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            
            // Try searching by phoneNumber field first
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .await()
            
            if (querySnapshot.documents.isNotEmpty()) {
                return querySnapshot.documents[0].data
            }
            
            // Try searching by phone field as fallback
            val querySnapshot2 = firestore.collection("users")
                .whereEqualTo("phone", phoneNumber)
                .get()
                .await()
            
            if (querySnapshot2.documents.isNotEmpty()) {
                return querySnapshot2.documents[0].data
            }
            
            null
        } catch (e: Exception) {
            Timber.e(e, "Error checking user by phone: $phoneNumber")
            null
        }
    }
    
    /**
     * Update user role in Firestore
     * 
     * @param userId The user's Firebase UID
     * @param role The role to set (WORKER or EMPLOYER)
     */
    suspend fun updateUserRole(userId: String, role: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users")
                .document(userId)
                .update("role", role)
                .await()
            Timber.d("Updated user role to $role for user $userId")
        } catch (e: Exception) {
            Timber.e(e, "Error updating user role for $userId")
            throw e
        }
    }
    
    /**
     * Get user data by Firebase UID
     * 
     * @param uid The user's Firebase UID
     * @return User data map if found, null otherwise
     */
    suspend fun getUserByUid(uid: String): Map<String, Any>? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val documentSnapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                @Suppress("UNCHECKED_CAST")
                documentSnapshot.data as? Map<String, Any>
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user by UID: $uid")
            null
        }
    }
}
