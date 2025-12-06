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
}
