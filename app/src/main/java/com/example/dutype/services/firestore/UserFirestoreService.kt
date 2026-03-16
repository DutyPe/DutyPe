package com.example.dutype.services.firestore

import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import com.example.dutype.utils.RetryUtils
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
                    "id" to user.id,
                    "phone" to user.phone,
                    "email" to user.email,
                    "fullName" to user.fullName,
                    "name" to user.fullName,
                    "profileImageUrl" to user.profileImageUrl,
                    "role" to user.activeRole.name,
                    "roles" to user.roles,
                    "activeRole" to user.activeRole.name,
                    "profileCompleted" to user.profileCompleted,
                    "isActive" to user.isActive,
                    "createdAt" to user.createdAt,
                    "updatedAt" to System.currentTimeMillis(),
                    "referralCode" to user.referralCode
                )
                
                // Merge prevents accidental field loss when this method runs with partial user data.
                userRef.set(coreUserData, SetOptions.merge()).await()
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
        return try {
            val query = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            if (!query.isEmpty) {
                val document = query.documents.first()
                val user = document.toObject(User::class.java)
                val userWithId = user?.copy(id = document.id) ?: User(id = document.id)
                Result.success(userWithId)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
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
        return try {
            val query = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            Result.success(!query.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user's last login time
     */
    suspend fun updateLastLogin(userId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "lastLoginAt" to System.currentTimeMillis(),
                "isActive" to true
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
                "lastLoginAt" to System.currentTimeMillis()
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
            // Fallback: Read from metadata doc if count() fails
            try {
                val metaDoc = firestore.collection("metadata").document("platform_stats").get().await()
                val count = metaDoc.getLong("totalUsers") ?: 0L
                Result.success(count)
            } catch (fallbackError: Exception) {
                Result.failure(e)
            }
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
                    "id" to data["id"],
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
                    .whereIn("id", chunk)
                    .get()
                    .await()
                
                val summaries = query.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    mapOf<String, Any?>(
                        "id" to data["id"],
                        "fullName" to data["fullName"],
                        "phone" to data["phone"],
                        "profileImageUrl" to data["profileImageUrl"],
                        "roles" to data["roles"],
                        "activeRole" to data["activeRole"]
                    )
                }
                allSummaries.addAll(summaries)
            }
            
            Result.success(allSummaries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

