package com.example.dutype.services.firestore

import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
        // DEPRECATED: worker_profiles and employer_profiles merged into users collection
        // Keeping constants for backward compatibility during migration
        @Deprecated("Use USERS_COLLECTION instead")
        const val WORKER_PROFILES_COLLECTION = "worker_profiles"
        @Deprecated("Use USERS_COLLECTION instead")
        const val EMPLOYER_PROFILES_COLLECTION = "employer_profiles"
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
                    "email" to user.email,
                    "fullName" to user.fullName,
                    "profileImageUrl" to user.profileImageUrl,
                    "roles" to user.roles,
                    "activeRole" to user.activeRole.name,
                    "profileCompleted" to user.profileCompleted,
                    "isActive" to user.isActive,
                    "createdAt" to user.createdAt
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
     * Get all users (for admin purposes)
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val query = firestore.collection(USERS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val users = query.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get users by role
     */
    suspend fun getUsersByRole(role: UserRole): Result<List<User>> {
        return try {
            val query = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("role", role.name)
                .orderBy("createdAt", Query.Direction.DESCENDING)
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
                "role" to newRole.name,
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
     * Get user count
     */
    suspend fun getUserCount(): Result<Long> {
        return try {
            val query = firestore.collection(USERS_COLLECTION).get().await()
            Result.success(query.size().toLong())
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
     * Get user summary for list views (lightweight)
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
     * Batch get user summaries
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
    
    /**
     * Create or update worker profile
     * OPTIMIZED: Now stores in users collection
     */
    suspend fun createOrUpdateWorkerProfile(userId: String, workerData: Map<String, Any>): Result<Unit> {
        return try {
            RetryUtils.retryWithBackoffResult {
                val profileRef = firestore.collection(WORKER_PROFILES_COLLECTION).document(userId)
                val data = workerData.toMutableMap()
                data["userId"] = userId
                data["updatedAt"] = System.currentTimeMillis()
                profileRef.set(data).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating/updating worker profile")
            Result.failure(e)
        }
    }
    
    /**
     * Create or update employer profile in separate collection
     */
    suspend fun createOrUpdateEmployerProfile(userId: String, employerData: Map<String, Any>): Result<Unit> {
        return try {
            RetryUtils.retryWithBackoffResult {
                val profileRef = firestore.collection(EMPLOYER_PROFILES_COLLECTION).document(userId)
                val data = employerData.toMutableMap()
                data["userId"] = userId
                data["updatedAt"] = System.currentTimeMillis()
                profileRef.set(data).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error creating/updating employer profile")
            Result.failure(e)
        }
    }
    
    /**
     * Get worker profile by user ID
     */
    suspend fun getWorkerProfile(userId: String): Result<Map<String, Any>?> {
        return try {
            val document = firestore.collection(WORKER_PROFILES_COLLECTION).document(userId).get().await()
            if (document.exists()) {
                Result.success(document.data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get employer profile by user ID
     */
    suspend fun getEmployerProfile(userId: String): Result<Map<String, Any>?> {
        return try {
            val document = firestore.collection(EMPLOYER_PROFILES_COLLECTION).document(userId).get().await()
            if (document.exists()) {
                Result.success(document.data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get worker profiles by email
     */
    suspend fun getWorkerProfilesByEmail(email: String): Result<List<Map<String, Any>>> {
        return try {
            val query = firestore.collection(WORKER_PROFILES_COLLECTION)
                .whereEqualTo("email", email)
                .get()
                .await()
            
            val profiles = query.documents.mapNotNull { it.data }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get employer profiles by email
     */
    suspend fun getEmployerProfilesByEmail(email: String): Result<List<Map<String, Any>>> {
        return try {
            val query = firestore.collection(EMPLOYER_PROFILES_COLLECTION)
                .whereEqualTo("contactEmail", email)
                .get()
                .await()
            
            val profiles = query.documents.mapNotNull { it.data }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

