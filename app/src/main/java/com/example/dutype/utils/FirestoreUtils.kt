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
     * 
     * PERFORMANCE OPTIMIZED: Single query with normalized phone number
     * Instead of trying 6+ queries with different variants, normalize first
     * and search with a single query. Falls back to phoneNumber field only if needed.
     * 
     * @param phoneNumber The phone number to search for (any format)
     * @return User data map if found, null otherwise
     */
    suspend fun checkUserExistsByPhoneNumber(phoneNumber: String): Map<String, Any?>? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val normalized = PhoneNumberUtils.normalize(phoneNumber)
            
            Timber.d("🔍 Phone check: input=$phoneNumber, normalized=$normalized")
            
            // Primary query: search by normalized phone (single query)
            val primaryResult = firestore.collection("users")
                .whereEqualTo("phone", normalized)
                .limit(1)
                .get()
                .await()
            
            if (primaryResult.documents.isNotEmpty()) {
                val doc = primaryResult.documents[0]
                Timber.d("✅ Found user by phone=$normalized, docId=${doc.id}")
                return doc.data
            }
            
            // Fallback: try without country code (legacy data)
            val withoutCountryCode = normalized.removePrefix("+91").removePrefix("91")
            if (withoutCountryCode != normalized) {
                val fallbackResult = firestore.collection("users")
                    .whereEqualTo("phone", withoutCountryCode)
                    .limit(1)
                    .get()
                    .await()
                
                if (fallbackResult.documents.isNotEmpty()) {
                    val doc = fallbackResult.documents[0]
                    Timber.d("✅ Found user by phone=$withoutCountryCode (legacy), docId=${doc.id}")
                    return doc.data
                }
            }
            
            Timber.d("❌ User not found for phone: $normalized")
            null
        } catch (e: Exception) {
            Timber.e(e, "❌ Phone check error for: $phoneNumber")
            null
        }
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
