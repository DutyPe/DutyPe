package com.example.dutype.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import com.google.firebase.Timestamp

/**
 * Utility functions for Firestore database operations
 */
object FirestoreUtils {

    /**
     * Ensure a minimal user document exists with dual-role fields.
     * This prevents role-switch/referral failures that occur when users/{uid} is missing
     * right after OTP registration.
     */
    suspend fun ensureMinimalUserDocument(
        userId: String,
        role: String,
        phoneNumber: String? = null,
        fullName: String? = null
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("users").document(userId)
        val roleUpper = role.uppercase()
        val existingDoc = userRef.get().await()

        val now = Timestamp.now()
        val existingData = existingDoc.data.orEmpty()
        @Suppress("UNCHECKED_CAST")
        val existingRoles = (existingData["roles"] as? List<String>).orEmpty()
        val mergedRoles = (existingRoles + roleUpper)
            .map { it.uppercase() }
            .filter { it == "WORKER" || it == "EMPLOYER" }
            .distinct()
            .ifEmpty { listOf(roleUpper) }

        val resolvedPhone = when {
            !phoneNumber.isNullOrBlank() -> PhoneNumberUtils.normalize(phoneNumber)
            (existingData["phone"] as? String).isNullOrBlank().not() -> existingData["phone"] as String
            else -> ""
        }

        val resolvedName = when {
            !fullName.isNullOrBlank() -> fullName.trim()
            (existingData["fullName"] as? String).isNullOrBlank().not() -> existingData["fullName"] as String
            else -> "User"
        }

        @Suppress("UNCHECKED_CAST")
        val existingLocation = existingData["location"] as? Map<String, Any>
        val lat = (existingLocation?.get("lat") as? Number)?.toDouble() ?: 0.0
        val lng = (existingLocation?.get("lng") as? Number)?.toDouble() ?: 0.0

        val hasValidLocation = GeoUtils.hasValidCoordinates(lat, lng)

        val strictUserDoc = hashMapOf<String, Any>(
            "phone" to resolvedPhone,
            "fullName" to resolvedName,
            "roles" to mergedRoles,
            "activeRole" to roleUpper,
            "location" to mapOf("lat" to lat, "lng" to lng),
            "isVerified" to ((existingData["isVerified"] as? Boolean) ?: false),
            "isActive" to ((existingData["isActive"] as? Boolean) ?: true),
            "createdAt" to ((existingData["createdAt"] as? Timestamp) ?: now),
            "lastActiveAt" to now
        )

        if (hasValidLocation) {
            strictUserDoc["geohash"] = existingData["geohash"] as? String ?: GeoUtils.encodeGeohash(lat, lng)
        }

        val existingProfileImageUrl = existingData["profileImageUrl"] as? String
        if (!existingProfileImageUrl.isNullOrBlank()) {
            strictUserDoc["profileImageUrl"] = existingProfileImageUrl
        }

        val existingFcmToken = existingData["fcmToken"] as? String
        if (!existingFcmToken.isNullOrBlank()) {
            strictUserDoc["fcmToken"] = existingFcmToken
        }

        userRef.set(strictUserDoc).await()
    }
    
    /**
     * Check if a user exists by normalized phone number in strict users schema.
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
            // Ensure core dual-role fields and normalized phone are present.
            ensureMinimalUserDocument(userId, role, phoneNumber = phoneNumber)
            val normalizedPhone = PhoneNumberUtils.normalize(phoneNumber)
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
            // Ensure core dual-role fields and store full name without writing legacy role field.
            ensureMinimalUserDocument(userId, role, fullName = fullName)
            Timber.d("✅ Saved full name '$fullName' for user $userId")
        } catch (e: Exception) {
            Timber.e(e, "❌ Error saving full name for $userId")
            throw e
        }
    }
}
