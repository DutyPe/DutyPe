package com.example.dutype.utils

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber

/**
 * Utility functions for Firestore database operations.
 */
object FirestoreUtils {

    /**
     * Ensures a canonical users document exists without ever writing placeholder values.
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
        val existingData = existingDoc.data.orEmpty()

        @Suppress("UNCHECKED_CAST")
        val existingRoles = (existingData["roles"] as? List<String>).orEmpty()
        val mergedRoles = (existingRoles + roleUpper)
            .map { it.uppercase() }
            .filter { it == "WORKER" || it == "EMPLOYER" }
            .distinct()
            .ifEmpty { listOf(roleUpper) }

        val resolvedPhone = phoneNumber
            ?.takeIf { it.isNotBlank() }
            ?.let(PhoneNumberUtils::normalize)
            ?: existingData["phone"] as? String
        val resolvedName = fullName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: existingData["fullName"] as? String

        if (resolvedPhone.isNullOrBlank() || resolvedName.isNullOrBlank()) {
            throw IllegalStateException("Refusing to create users/$userId without fullName and phone")
        }

        val strictUserDoc = linkedMapOf<String, Any>(
            "userId" to userId,
            "phone" to resolvedPhone,
            "fullName" to resolvedName,
            "roles" to mergedRoles,
            "activeRole" to roleUpper,
            "isVerified" to ((existingData["isVerified"] as? Boolean) ?: false),
            "isActive" to ((existingData["isActive"] as? Boolean) ?: true),
            "createdAt" to ((existingData["createdAt"] as? Timestamp) ?: Timestamp.now()),
            "lastActiveAt" to Timestamp.now()
        )

        val existingProfileImageUrl = existingData["profileImageUrl"] as? String
        if (!existingProfileImageUrl.isNullOrBlank()) {
            strictUserDoc["profileImageUrl"] = existingProfileImageUrl
        }

        val existingFcmToken = existingData["fcmToken"] as? String
        if (!existingFcmToken.isNullOrBlank()) {
            strictUserDoc["fcmToken"] = existingFcmToken
        }

        val existingLocation = existingData["location"] as? Map<*, *>
        val lat = (existingLocation?.get("lat") as? Number)?.toDouble()
        val lng = (existingLocation?.get("lng") as? Number)?.toDouble()
        if (lat != null && lng != null && GeoUtils.hasValidCoordinates(lat, lng)) {
            strictUserDoc["location"] = mapOf("lat" to lat, "lng" to lng)
            strictUserDoc["geohash"] = GeoUtils.encodeGeohash(lat, lng)
        }

        userRef.set(strictUserDoc, SetOptions.merge()).await()
    }

    /**
     * Check if a user exists by normalized phone number in strict users schema.
     */
    suspend fun checkUserExistsByPhoneNumber(phoneNumber: String): Map<String, Any?>? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val normalized = PhoneNumberUtils.normalize(phoneNumber)

            Timber.d("Phone check: normalized=$normalized")

            val primaryResult = firestore.collection("users")
                .whereEqualTo("phone", normalized)
                .limit(1)
                .get()
                .await()

            if (primaryResult.documents.isNotEmpty()) {
                primaryResult.documents[0].data
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Phone check error for: $phoneNumber")
            null
        }
    }

    suspend fun doesUserExist(phoneNumber: String): Boolean {
        return checkUserExistsByPhoneNumber(phoneNumber) != null
    }

    suspend fun updateUserRole(userId: String, role: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data.orEmpty()

            @Suppress("UNCHECKED_CAST")
            val existingRoles = (userData["roles"] as? List<String>)?.toMutableList() ?: mutableListOf()

            val roleUpper = role.uppercase()
            if (!existingRoles.contains(roleUpper)) {
                existingRoles.add(roleUpper)
            }

            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "roles" to existingRoles,
                        "activeRole" to roleUpper,
                        "lastActiveAt" to Timestamp.now()
                    )
                )
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error updating user role for $userId")
            throw e
        }
    }

    suspend fun getUserByUid(uid: String): Map<String, Any>? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val documentSnapshot = withTimeout(5_000L) {
                firestore.collection("users")
                    .document(uid)
                    .get()
                    .await()
            }

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

    suspend fun saveUserPhoneNumber(userId: String, phoneNumber: String, role: String) {
        try {
            val normalizedPhone = PhoneNumberUtils.normalize(phoneNumber)
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "phone" to normalizedPhone,
                        "lastActiveAt" to Timestamp.now()
                    )
                )
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error saving phone number for $userId")
            throw e
        }
    }

    suspend fun saveUserFullName(userId: String, fullName: String, role: String) {
        try {
            val trimmedName = fullName.trim()
            if (trimmedName.isBlank()) {
                throw IllegalArgumentException("Full name cannot be blank")
            }
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "fullName" to trimmedName,
                        "lastActiveAt" to Timestamp.now()
                    )
                )
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error saving full name for $userId")
            throw e
        }
    }
}
