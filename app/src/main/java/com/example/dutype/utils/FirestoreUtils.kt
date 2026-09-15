package com.example.dutype.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Utility functions for Firestore database operations
 */
object FirestoreUtils {

    suspend fun readProfileDocument(userId: String): com.google.firebase.firestore.DocumentSnapshot {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val owner = currentUserId == userId
        val reference = FirebaseFirestore.getInstance()
            .collection(if (owner) "users" else "public_profiles").document(userId)
        val document = reference.get().await()
        if (owner && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid != currentUserId) {
            throw IllegalStateException("Account changed while reading a private profile")
        }
        if (!owner && !document.exists() && currentUserId != null) {
            com.google.firebase.functions.FirebaseFunctions.getInstance().getHttpsCallable("getPublicProfile")
                .call(mapOf("userId" to userId)).await()
            return reference.get(com.google.firebase.firestore.Source.SERVER).await()
        }
        return document
    }

    suspend fun applicationContact(applicationId: String): Map<String, Any> {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")
        val response = com.google.firebase.functions.FirebaseFunctions.getInstance()
            .getHttpsCallable("getApplicationContact").call(mapOf("applicationId" to applicationId)).await()
        check(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == userId) {
            "Account changed while reading application contacts"
        }
        val profile = (response.data as? Map<*, *>)?.get("profile") as? Map<*, *>
            ?: throw IllegalStateException("Application contact is unavailable")
        return profile.entries.mapNotNull { (key, value) ->
            if (key is String && value != null) key to value else null
        }.toMap()
    }
    
    /**
     * Check if a user exists by phone number in Firestore
     * 
     * PERFORMANCE OPTIMIZED: Single query with normalized phone number
     * Instead of trying 6+ queries with different variants, normalize first
     * and search with a single query. Falls back to phoneNumber field only if needed.
     * 
     * @param phoneNumber The phone number to search for (any format)
     * @return User data map if found, null otherwise
     */
    suspend fun checkUserExistsByPhoneNumber(phoneNumber: String): Map<String, Any?>? {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return null
        if (PhoneNumberUtils.normalize(user.phoneNumber.orEmpty()) != PhoneNumberUtils.normalize(phoneNumber)) return null
        return FirebaseFirestore.getInstance().collection("users").document(user.uid).get().await().data
    }
    
    /**
     * Check if user exists by phone number (simple boolean check)
     * 
     * @param phoneNumber The phone number to check (with country code)
     * @return true if user exists, false otherwise
     */
    suspend fun doesUserExist(phoneNumber: String): Boolean {
        return checkUserExistsByPhoneNumber(phoneNumber) != null
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
            
            // Update roles array and activeRole (no legacy 'role' field)
            val updates = mapOf(
                "roles" to existingRoles,
                "activeRole" to roleUpper
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
            val documentSnapshot = readProfileDocument(uid)
            
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
     * CRITICAL FIX: Now normalizes phone number to consistent format
     * Always stores as: +{countryCode}{number}
     * Example: +919876543210
     * 
     * @param userId The user's Firebase UID
     * @param phoneNumber The phone number to save (any format)
     * @param role The user's role (WORKER or EMPLOYER)
     */
    suspend fun saveUserPhoneNumber(userId: String, phoneNumber: String, role: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userRef = firestore.collection("users").document(userId)
            
            // Normalize phone number to consistent format
            val normalizedPhone = PhoneNumberUtils.normalize(phoneNumber)
            
            val updates = hashMapOf<String, Any>(
                "phone" to normalizedPhone,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            // Use set with merge to create or update
            userRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            Timber.d("✅ Saved normalized phone number $normalizedPhone for user $userId")
        } catch (e: Exception) {
            Timber.e(e, "❌ Error saving phone number for $userId")
            throw e
        }
    }

    /**
     * Save user's full name to Firestore (captured during registration).
     */
    suspend fun saveUserFullName(userId: String, fullName: String, role: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userRef = firestore.collection("users").document(userId)

            val updates = hashMapOf<String, Any>(
                "fullName" to fullName,
                "role" to role,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            userRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            Timber.d("✅ Saved full name '$fullName' for user $userId")
        } catch (e: Exception) {
            Timber.e(e, "❌ Error saving full name for $userId")
            throw e
        }
    }
}
