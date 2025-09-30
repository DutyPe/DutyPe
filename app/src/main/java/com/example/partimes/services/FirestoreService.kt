package com.example.partimes.services

import com.example.partimes.models.User
import com.example.partimes.models.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class FirestoreService {
    
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    companion object {
        // Core Collections - Scalable Design
        const val USERS_COLLECTION = "users"                    // Main user authentication data
        const val WORKER_PROFILES_COLLECTION = "worker_profiles" // Worker-specific profile data
        const val EMPLOYER_PROFILES_COLLECTION = "employer_profiles" // Employer-specific profile data
        
        // Business Collections
        const val JOBS_COLLECTION = "jobs"
        const val APPLICATIONS_COLLECTION = "applications"
        const val NOTIFICATIONS_COLLECTION = "notifications"
        const val SAVED_JOBS_COLLECTION = "saved_jobs"
        
        // Analytics & Performance Collections
        const val USER_ACTIVITY_COLLECTION = "user_activity"
        const val APP_ANALYTICS_COLLECTION = "app_analytics"
    }
    
    /**
     * Create or update user in Firestore (Core authentication data only)
     */
    suspend fun createOrUpdateUser(user: User): Result<Unit> {
        return try {
            val userRef = firestore.collection(USERS_COLLECTION).document(user.id)
            userRef.set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create or update worker profile in separate collection
     */
    suspend fun createOrUpdateWorkerProfile(userId: String, workerData: Map<String, Any>): Result<Unit> {
        return try {
            val profileRef = firestore.collection(WORKER_PROFILES_COLLECTION).document(userId)
            val data = workerData.toMutableMap()
            data["userId"] = userId
            data["updatedAt"] = System.currentTimeMillis()
            profileRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create or update employer profile in separate collection
     */
    suspend fun createOrUpdateEmployerProfile(userId: String, employerData: Map<String, Any>): Result<Unit> {
        return try {
            val profileRef = firestore.collection(EMPLOYER_PROFILES_COLLECTION).document(userId)
            val data = employerData.toMutableMap()
            data["userId"] = userId
            data["updatedAt"] = System.currentTimeMillis()
            profileRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
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
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val document = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
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
                val user = query.documents.first().toObject(User::class.java)
                Result.success(user)
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
            
            // Get updated user
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
            // Note: Firestore doesn't support full-text search natively
            // This is a simple implementation - for production, consider using Algolia or similar
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
}
