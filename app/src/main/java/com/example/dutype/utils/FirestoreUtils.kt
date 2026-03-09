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
     * Update user role in Firestore - DUAL ROLE SUPPORT
     * Adds the role to the roles array if not already present
     * Updates activeRole to the new role
     * 
     * @param userId The user's Firebase UID
     * @param role The role to set (WORKER or EMPLOYER)
     */
    suspend fun updateUserRole(userId: String, role: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            
            // Fetch current user data to get existing roles
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data
            
            // Get existing roles array
            @Suppress("UNCHECKED_CAST")
            val existingRoles = (userData?.get("roles") as? List<String>)?.toMutableList() ?: mutableListOf()
            
            // Add new role if not already present
            val roleUpper = role.uppercase()
            if (!existingRoles.contains(roleUpper)) {
                existingRoles.add(roleUpper)
                Timber.d("✅ FirestoreUtils - Adding role $roleUpper to roles array")
            } else {
                Timber.d("✅ FirestoreUtils - Role $roleUpper already exists in roles array")
            }
            
            // Update both roles array and activeRole
            val updates = mapOf(
                "roles" to existingRoles,
                "activeRole" to roleUpper,
                "role" to roleUpper  // Keep legacy field for backward compatibility
            )
            
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()
            Timber.d("✅ FirestoreUtils - Updated roles array: $existingRoles, activeRole: $roleUpper for user $userId")
        } catch (e: Exception) {
            Timber.e(e, "❌ FirestoreUtils - Error updating user role for $userId")
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
    
    /**
     * Save or update user phone number in Firestore
     * Creates the user document if it doesn't exist
     * 
     * @param userId The user's Firebase UID
     * @param phoneNumber The phone number to save
     * @param role The user's role (WORKER or EMPLOYER)
     */
    suspend fun saveUserPhoneNumber(userId: String, phoneNumber: String, role: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userRef = firestore.collection("users").document(userId)
            
            val updates = hashMapOf<String, Any>(
                "phone" to phoneNumber,
                "role" to role,
                "platform" to "android",
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            // Use set with merge to create or update
            userRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            Timber.d("Saved phone number $phoneNumber for user $userId")
        } catch (e: Exception) {
            Timber.e(e, "Error saving phone number for $userId")
            throw e
        }
    }
}
