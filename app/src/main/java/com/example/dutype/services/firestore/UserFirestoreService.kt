package com.example.dutype.services.firestore

import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.example.dutype.utils.PhoneNumberUtils
import com.example.dutype.utils.RetryUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val PHONE_ROLES_COLLECTION = FirestoreCollections.PHONE_ROLES
        private const val WORKER_PROFILES_COLLECTION = FirestoreCollections.WORKER_PROFILES
        private const val EMPLOYER_PROFILES_COLLECTION = FirestoreCollections.EMPLOYER_PROFILES
        private const val USER_TOKENS_COLLECTION = FirestoreCollections.USER_TOKENS
    }

    suspend fun createOrUpdateUser(user: User): Result<Unit> {
        return try {
            RetryUtils.retryWithBackoffResult {
                val roleName = user.role.name
                val normalizedPhone = PhoneNumberUtils.normalize(user.phone)
                val now = Timestamp.now()

                if (normalizedPhone.isNotBlank()) {
                    firestore.collection(PHONE_ROLES_COLLECTION)
                        .document(normalizedPhone)
                        .set(
                            mapOf(
                                "phoneNumber" to normalizedPhone,
                                "roles" to listOf(roleName),
                                "name" to user.fullName,
                                "uid" to user.id,
                                "createdAt" to Timestamp(Date(user.createdAt)),
                                "updatedAt" to now
                            ),
                            SetOptions.merge()
                        )
                        .await()
                }

                val profileData = mutableMapOf<String, Any?>(
                    "userId" to user.id,
                    "phone" to normalizedPhone,
                    "fullName" to user.fullName,
                    "profileImageUrl" to user.profileImageUrl,
                    "role" to roleName,
                    "createdAt" to Timestamp(Date(user.createdAt)),
                    "updatedAt" to now
                )
                if (com.example.dutype.utils.GeoUtils.hasValidCoordinates(user.lat, user.lng)) {
                    profileData["location"] = mapOf("lat" to user.lat, "lng" to user.lng)
                    profileData["geohash"] = com.example.dutype.utils.GeoUtils.encodeGeohash(user.lat, user.lng)
                }

                firestore.collection(profileCollectionForRole(roleName))
                    .document(user.id)
                    .set(profileData.filterValues { it != null }, SetOptions.merge())
                    .await()

                if (!user.fcmToken.isNullOrBlank()) {
                    firestore.collection(USER_TOKENS_COLLECTION)
                        .document(user.id)
                        .set(
                            mapOf(
                                "fcmToken" to user.fcmToken.trim(),
                                "platform" to "android",
                                "updatedAt" to now
                            ),
                            SetOptions.merge()
                        )
                        .await()
                }

                Timber.i("Firestore: Profile-backed user saved successfully for ${user.id}")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Firestore Error")
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val profile = loadProfileData(userId) ?: return Result.success(null)
            Result.success(User.fromFirestoreMap(userId, profile))
        } catch (e: Exception) {
            Timber.e(e, "UserFirestoreService.getUserById failed")
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val role = updates["role"]?.toString()?.uppercase()
                ?.takeIf { it == UserRole.WORKER.name || it == UserRole.EMPLOYER.name }
                ?: existingRoleForUser(userId)
                ?: UserRole.WORKER.name
            val sanitizedUpdates = sanitizeProfileUpdates(userId, updates, role)

            if (sanitizedUpdates.isNotEmpty()) {
                firestore.collection(profileCollectionForRole(role))
                    .document(userId)
                    .set(sanitizedUpdates, SetOptions.merge())
                    .await()
            }

            val fcmToken = updates["fcmToken"] as? String
            if (!fcmToken.isNullOrBlank()) {
                firestore.collection(USER_TOKENS_COLLECTION)
                    .document(userId)
                    .set(
                        mapOf(
                            "fcmToken" to fcmToken.trim(),
                            "platform" to "android",
                            "updatedAt" to Timestamp.now()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }

            val phone = sanitizedUpdates["phone"] as? String
            val fullName = sanitizedUpdates["fullName"] as? String
            if (!phone.isNullOrBlank() || !fullName.isNullOrBlank()) {
                val normalizedPhone = phone ?: loadProfileData(userId)?.get("phone") as? String
                if (!normalizedPhone.isNullOrBlank()) {
                    val phoneRoleUpdates = mutableMapOf<String, Any>(
                        "phoneNumber" to normalizedPhone,
                        "roles" to listOf(role),
                        "uid" to userId,
                        "updatedAt" to Timestamp.now()
                    )
                    if (!fullName.isNullOrBlank()) phoneRoleUpdates["name"] = fullName
                    firestore.collection(PHONE_ROLES_COLLECTION)
                        .document(normalizedPhone)
                        .set(phoneRoleUpdates, SetOptions.merge())
                        .await()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitizeProfileUpdates(userId: String, updates: Map<String, Any>, role: String): Map<String, Any> {
        val sanitized = mutableMapOf<String, Any>()

        val fullName = updates["fullName"] as? String
        if (!fullName.isNullOrBlank()) sanitized["fullName"] = fullName.trim()

        val phone = updates["phone"] as? String
        if (!phone.isNullOrBlank()) sanitized["phone"] = PhoneNumberUtils.normalize(phone)

        val profileImageUrl = updates["profileImageUrl"] as? String
        if (!profileImageUrl.isNullOrBlank()) sanitized["profileImageUrl"] = profileImageUrl.trim()

        val locationFromMap = updates["location"] as? Map<*, *>
        val mapLat = (locationFromMap?.get("lat") as? Number)?.toDouble()
        val mapLng = (locationFromMap?.get("lng") as? Number)?.toDouble()
        if (mapLat != null && mapLng != null && com.example.dutype.utils.GeoUtils.hasValidCoordinates(mapLat, mapLng)) {
            sanitized["location"] = mapOf("lat" to mapLat, "lng" to mapLng)
            sanitized["geohash"] = com.example.dutype.utils.GeoUtils.encodeGeohash(mapLat, mapLng)
        }

        if (sanitized.isEmpty()) {
            return emptyMap()
        }

        sanitized["userId"] = userId
        sanitized["role"] = role
        sanitized["updatedAt"] = Timestamp.now()
        return sanitized
    }

    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            val role = existingRoleForUser(userId)
            if (role != null) {
                firestore.collection(profileCollectionForRole(role)).document(userId).delete().await()
            }
            firestore.collection(USER_TOKENS_COLLECTION).document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSummary(userId: String): Result<Map<String, Any?>?> {
        return try {
            val data = loadProfileData(userId) ?: return Result.success(null)
            Result.success(summaryFromProfile(userId, data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSummaries(userIds: List<String>): Result<List<Map<String, Any?>>> {
        return try {
            if (userIds.isEmpty()) return Result.success(emptyList())
            val summariesById = mutableMapOf<String, Map<String, Any?>>()

            for (chunk in userIds.chunked(30)) {
                for (collection in listOf(WORKER_PROFILES_COLLECTION, EMPLOYER_PROFILES_COLLECTION)) {
                    val query = firestore.collection(collection)
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    query.documents.forEach { doc ->
                        if (!summariesById.containsKey(doc.id)) {
                            summariesById[doc.id] = summaryFromProfile(doc.id, doc.data.orEmpty())
                        }
                    }
                }
            }

            Result.success(userIds.mapNotNull { summariesById[it] })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun loadProfileData(userId: String): Map<String, Any?>? {
        for (role in listOf(UserRole.WORKER.name, UserRole.EMPLOYER.name)) {
            val snapshot = firestore.collection(profileCollectionForRole(role)).document(userId).get().await()
            if (snapshot.exists()) {
                val data = snapshot.data.orEmpty().toMutableMap()
                data["role"] = data["role"] ?: role
                return data
            }
        }
        return null
    }

    private suspend fun existingRoleForUser(userId: String): String? {
        if (firestore.collection(WORKER_PROFILES_COLLECTION).document(userId).get().await().exists()) {
            return UserRole.WORKER.name
        }
        if (firestore.collection(EMPLOYER_PROFILES_COLLECTION).document(userId).get().await().exists()) {
            return UserRole.EMPLOYER.name
        }
        return null
    }

    private fun profileCollectionForRole(role: String): String {
        return if (role.uppercase() == UserRole.EMPLOYER.name) EMPLOYER_PROFILES_COLLECTION else WORKER_PROFILES_COLLECTION
    }

    private fun summaryFromProfile(userId: String, data: Map<String, Any?>): Map<String, Any?> {
        val role = data["role"] ?: UserRole.WORKER.name
        return mapOf(
            "id" to userId,
            "fullName" to (data["fullName"] ?: data["name"]),
            "phone" to data["phone"],
            "profileImageUrl" to data["profileImageUrl"],
            "role" to role
        )
    }
}