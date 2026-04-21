package com.example.dutype.utils

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber

/**
 * Utility functions for Firestore database operations.
 */
object FirestoreUtils {

    enum class PhoneExistenceResult {
        EXISTS,
        NOT_EXISTS,
        UNKNOWN
    }

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
        val userRef = firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS).document(userId)
        val roleUpper = role.uppercase()
        val existingDoc = userRef.get().await()
        val existingData = existingDoc.data.orEmpty()

        // Single-role architecture: ignore any legacy roles[] in the existing
        // document and overwrite with this role only.
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
            "phone" to resolvedPhone,
            "fullName" to resolvedName,
            "roles" to listOf(roleUpper),
            "activeRole" to roleUpper,
            "createdAt" to ((existingData["createdAt"] as? Timestamp) ?: Timestamp.now())
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

        // Replace with canonical schema to remove legacy keys that violate strict Firestore rules.
        userRef.set(strictUserDoc).await()
    }

    /**
     * Check if a user exists by normalized phone number in strict users schema.
     */
    suspend fun checkUserExistsByPhoneNumber(phoneNumber: String): Map<String, Any?>? {
        val firestore = FirebaseFirestore.getInstance()
        val variants = PhoneNumberUtils.getVariants(phoneNumber)

        Timber.d("Phone check: variants=$variants")

        for (variant in variants) {
            val result = firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                .whereEqualTo("phone", variant)
                .limit(1)
                .get()
                .await()

            if (result.documents.isNotEmpty()) {
                return result.documents[0].data
            }
        }

        return null
    }

    suspend fun doesUserExist(phoneNumber: String): Boolean {
        return checkUserExistsByPhoneNumber(phoneNumber) != null
    }

    suspend fun checkPhoneExistence(phoneNumber: String): PhoneExistenceResult {
        val callableResult = checkPhoneExistenceViaCallable(phoneNumber)
        if (callableResult != PhoneExistenceResult.UNKNOWN) {
            return callableResult
        }

        if (FirebaseAuth.getInstance().currentUser == null) {
            Timber.d("Phone existence fallback skipped for guest user (users query requires auth)")
            return PhoneExistenceResult.UNKNOWN
        }

        return try {
            val result = checkUserExistsByPhoneNumber(phoneNumber)
            if (result != null) PhoneExistenceResult.EXISTS else PhoneExistenceResult.NOT_EXISTS
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Timber.w("Phone existence check blocked by rules. Continuing with UNKNOWN.")
                PhoneExistenceResult.UNKNOWN
            } else {
                Timber.e(e, "Phone existence check failed")
                PhoneExistenceResult.UNKNOWN
            }
        } catch (e: Exception) {
            Timber.e(e, "Phone existence check failed")
            PhoneExistenceResult.UNKNOWN
        }
    }

    private suspend fun checkPhoneExistenceViaCallable(phoneNumber: String): PhoneExistenceResult {
        val normalized = PhoneNumberUtils.normalize(phoneNumber)
        val variants = PhoneNumberUtils.getVariants(phoneNumber)
        val callableNames = listOf("checkPhoneExists")

        for (callableName in callableNames) {
            try {
                val response = FirebaseFunctions.getInstance()
                    .getHttpsCallable(callableName)
                    .call(
                        mapOf(
                            "phone" to normalized,
                            "variants" to variants
                        )
                    )
                    .await()

                @Suppress("UNCHECKED_CAST")
                val payload = response.data as? Map<String, Any>
                val exists = payload?.get("exists") as? Boolean

                when (exists) {
                    true -> return PhoneExistenceResult.EXISTS
                    false -> return PhoneExistenceResult.NOT_EXISTS
                    null -> Timber.w("Callable $callableName returned invalid payload: $payload")
                }
            } catch (e: Exception) {
                Timber.w(e, "Callable $callableName unavailable")
            }
        }

        return PhoneExistenceResult.UNKNOWN
    }

    suspend fun updateUserRole(userId: String, role: String) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val roleUpper = role.uppercase()
            val updates = com.example.dutype.models.User.roleFieldsFor(
                runCatching { com.example.dutype.models.UserRole.valueOf(roleUpper) }
                    .getOrDefault(com.example.dutype.models.UserRole.WORKER)
            )

            firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                .document(userId)
                .update(updates)
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
                firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS)
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
                .collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                .document(userId)
                .update(
                    mapOf(
                        "phone" to normalizedPhone
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
                .collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                .document(userId)
                .update(
                    mapOf(
                        "fullName" to trimmedName
                    )
                )
                .await()
        } catch (e: Exception) {
            Timber.e(e, "Error saving full name for $userId")
            throw e
        }
    }
}
