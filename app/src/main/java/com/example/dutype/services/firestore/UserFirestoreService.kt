package com.example.dutype.services.firestore

import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import com.example.dutype.utils.RetryUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserFirestoreService - Handles all user-related Firestore operations
 * 
 * Extracted from FirestoreService as part of architecture refactoring.
 * Single responsibility: User CRUD operations and profile management.
 * 
 * @author DutyPe Engineering Team
 * @since 2.1.0
 */
@Singleton
class UserFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        const val USERS_COLLECTION = "users"
    }
    
    /**
     * Create or update user in Firestore (Core authentication data only)
     */
    suspend fun createOrUpdateUser(user: User): Result<Unit> {
        return try {
            RetryUtils.retryWithBackoffResult {
                val userRef = firestore.collection(USERS_COLLECTION).document(user.id)
                Timber.d("Firestore: Saving user to path: ${USERS_COLLECTION}/${user.id}")
                
                val coreUserData = mapOf(
                    "userId" to user.id,
                    "phone" to user.phone,
                    "fullName" to user.fullName,
                    "profileImageUrl" to user.profileImageUrl,
                    "roles" to user.roles,
                    "activeRole" to user.activeRole.name,
                    "location" to mapOf("lat" to user.lat, "lng" to user.lng),
                    "geohash" to com.example.dutype.utils.GeoUtils.encodeGeohash(user.lat, user.lng),
                    "isVerified" to false,
                    "isActive" to user.isActive,
                    "fcmToken" to user.fcmToken,
                    "createdAt" to Timestamp(Date(user.createdAt)),
                    "lastActiveAt" to Timestamp.now()
                )
                
                userRef.set(coreUserData).await()
                Timber.i("Firestore: User saved successfully to ${USERS_COLLECTION}/${user.id}")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Firestore Error")
            Result.failure(e)
        }
    }
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            Timber.d("🔍 UserFirestoreService.getUserById - Fetching user: $userId")
            val document = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            if (document.exists()) {
                // Log the raw data to debug field names
                Timber.d("🔍 UserFirestoreService.getUserById - Raw data: ${document.data}")
                Timber.d("🔍 UserFirestoreService.getUserById - fullName field: ${document.getString("fullName")}")
                
                val user = document.toObject(User::class.java)
                Timber.d("🔍 UserFirestoreService.getUserById - Deserialized user fullName: ${user?.fullName}")
                
                val userWithId = user?.copy(id = document.id) ?: User(id = document.id)
                Result.success(userWithId)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Timber.e("🔍 UserFirestoreService.getUserById - Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get user by email
     */
    suspend fun getUserByEmail(email: String): Result<User?> {
        return Result.failure(UnsupportedOperationException("Email lookup is not supported in strict users schema"))
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val sanitizedUpdates = sanitizeUserUpdates(updates)
            if (sanitizedUpdates.isEmpty()) {
                Timber.w("UserFirestoreService.updateUserProfile: Ignoring empty/unsupported updates for $userId")
                return Result.success(Unit)
            }

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(sanitizedUpdates, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitizeUserUpdates(updates: Map<String, Any>): Map<String, Any> {
        val sanitized = mutableMapOf<String, Any>()

        val fullName = updates["fullName"] as? String
        if (!fullName.isNullOrBlank()) {
            sanitized["fullName"] = fullName.trim()
        }

        val phone = updates["phone"] as? String
        if (!phone.isNullOrBlank()) {
            sanitized["phone"] = com.example.dutype.utils.PhoneNumberUtils.normalize(phone)
        }

        val profileImageUrl = updates["profileImageUrl"] as? String
        if (!profileImageUrl.isNullOrBlank()) {
            sanitized["profileImageUrl"] = profileImageUrl.trim()
        }

        val roles = updates["roles"] as? List<*>
        if (roles != null) {
            val validRoles = roles.mapNotNull { it?.toString()?.trim()?.uppercase() }
                .filter { it == "WORKER" || it == "EMPLOYER" }
                .distinct()
            if (validRoles.isNotEmpty()) {
                sanitized["roles"] = validRoles
            }
        }

        val activeRole = updates["activeRole"]?.toString()?.trim()?.uppercase()
        if (activeRole == "WORKER" || activeRole == "EMPLOYER") {
            sanitized["activeRole"] = activeRole
        }

        val fcmToken = updates["fcmToken"] as? String
        if (!fcmToken.isNullOrBlank()) {
            sanitized["fcmToken"] = fcmToken.trim()
        }

        val isActive = updates["isActive"] as? Boolean
        if (isActive != null) {
            sanitized["isActive"] = isActive
        }

        val isVerified = updates["isVerified"] as? Boolean
        if (isVerified != null) {
            sanitized["isVerified"] = isVerified
        }

        val locationFromMap = updates["location"] as? Map<*, *>
        val mapLat = (locationFromMap?.get("lat") as? Number)?.toDouble()
        val mapLng = (locationFromMap?.get("lng") as? Number)?.toDouble()
        if (mapLat != null && mapLng != null && com.example.dutype.utils.GeoUtils.hasValidCoordinates(mapLat, mapLng)) {
            sanitized["location"] = mapOf("lat" to mapLat, "lng" to mapLng)
            sanitized["geohash"] = com.example.dutype.utils.GeoUtils.encodeGeohash(mapLat, mapLng)
        }

        sanitized["lastActiveAt"] = Timestamp.now()
        return sanitized
    }
    
    /**
     * Delete user
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    /**
     * Get all users (for admin purposes) — PAGINATED for 5L+ scale
     */
    suspend fun getAllUsers(limit: Long = 50): Result<List<User>> {
        return try {
            val query = firestore.collection(USERS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            
            val users = query.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get users by role — PAGINATED for 5L+ scale
     */
    suspend fun getUsersByRole(role: UserRole, limit: Long = 50): Result<List<User>> {
        return try {
            val query = firestore.collection(USERS_COLLECTION)
                .whereArrayContains("roles", role.name)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            
            val users = query.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user exists by email
     */
    suspend fun userExistsByEmail(email: String): Result<Boolean> {
        return Result.failure(UnsupportedOperationException("Email lookup is not supported in strict users schema"))
    }
    
    /**
     * Update user's last login time
     */
    suspend fun updateLastLogin(userId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "lastActiveAt" to Timestamp.now()
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Switch user role
     */
    suspend fun switchUserRole(userId: String, newRole: UserRole): Result<User> {
        return try {
            val updates = mapOf(
                "activeRole" to newRole.name,
                "lastActiveAt" to Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            
            val document = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            val user = document.toObject(User::class.java)
                ?: throw Exception("Failed to retrieve updated user")
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user count — P0 FIX: Use Firestore count() aggregation instead of downloading all docs
     */
    suspend fun getUserCount(): Result<Long> {
        return try {
            val countQuery = firestore.collection(USERS_COLLECTION)
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()
            Result.success(countQuery.count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search users by name or email
     */
    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val usersQuery = firestore.collection(USERS_COLLECTION)
                .orderBy("fullName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .await()
            
            val users = usersQuery.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user summary for list views — LIGHTWEIGHT (6 fields only)
     * Use this instead of getUserById() when showing user cards/lists
     */
    suspend fun getUserSummary(userId: String): Result<Map<String, Any?>?> {
        return try {
            val document = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            if (document.exists()) {
                val data = document.data ?: return Result.success(null)
                
                val summary = mapOf<String, Any?>(
                    "id" to document.id,
                    "fullName" to data["fullName"],
                    "phone" to data["phone"],
                    "profileImageUrl" to data["profileImageUrl"],
                    "roles" to data["roles"],
                    "activeRole" to data["activeRole"]
                )
                Result.success(summary)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Batch get user summaries — LIGHTWEIGHT
     */
    suspend fun getUserSummaries(userIds: List<String>): Result<List<Map<String, Any?>>> {
        return try {
            if (userIds.isEmpty()) return Result.success(emptyList())
            
            val chunks = userIds.chunked(30)
            val allSummaries = mutableListOf<Map<String, Any?>>()
            
            for (chunk in chunks) {
                val query = firestore.collection(USERS_COLLECTION)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                
                val summariesById = query.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    doc.id to mapOf<String, Any?>(
                        "id" to doc.id,
                        "fullName" to data["fullName"],
                        "phone" to data["phone"],
                        "profileImageUrl" to data["profileImageUrl"],
                        "roles" to data["roles"],
                        "activeRole" to data["activeRole"]
                    )
                }.toMap()
                allSummaries.addAll(chunk.mapNotNull { summariesById[it] })
            }
            
            Result.success(allSummaries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

