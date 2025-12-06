package com.example.dutype.services

import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import com.example.dutype.utils.RetryUtils

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
     * Only stores essential fields, profile data goes to separate collections
     */
    suspend fun createOrUpdateUser(user: User): Result<Unit> {
        return try {
            RetryUtils.retryWithBackoffResult {
                val userRef = firestore.collection(USERS_COLLECTION).document(user.id)
                Timber.d("Firestore: Saving user to path: ${USERS_COLLECTION}/${user.id}")
                
                // Only store essential authentication and core user data
                val coreUserData = mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "fullName" to user.fullName,
                    "profileImageUrl" to user.profileImageUrl,
                    "role" to user.role.name,
                    "isProfileComplete" to user.isProfileComplete,
                    "isVerified" to user.isVerified,
                    "isActive" to user.isActive,
                    "createdAt" to user.createdAt,
                    "lastLoginAt" to user.lastLoginAt
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
     * Create or update worker profile in separate collection
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
     * Get user by ID
     */
    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val document = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                // CRITICAL FIX: Set ID from document ID since Firestore doesn't store it in the document
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
                // CRITICAL FIX: Set ID from document ID since Firestore doesn't store it in the document
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
    
    // ==================== JOB MANAGEMENT METHODS ====================
    
    /**
     * Create a new job posting in Firestore
     */
    suspend fun createJob(jobData: Map<String, Any>): Result<String> {
        return try {
            val jobRef = firestore.collection(JOBS_COLLECTION).document()
            val data = jobData.toMutableMap()
            data["jobId"] = jobRef.id
            data["createdAt"] = System.currentTimeMillis()
            data["updatedAt"] = System.currentTimeMillis()
            data["isActive"] = true
            data["viewCount"] = 0L
            data["applicationCount"] = 0L
            
            jobRef.set(data).await()
            Result.success(jobRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all active jobs (for workers) with pagination support
     */
    suspend fun getAllJobs(limit: Long = 50L, lastCreatedAt: Long? = null): Result<List<Map<String, Any>>> {
        return try {
            var query = firestore.collection(JOBS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
            
            if (lastCreatedAt != null) {
                query = query.startAfter(lastCreatedAt)
            }
                
            val snapshot = query.get().await()
            
            // Filter active jobs in memory to avoid index requirement
            val jobs = snapshot.documents.mapNotNull { it.data }
                .filter { (it["isActive"] as? Boolean) == true }
            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get jobs posted by a specific employer
     */
    suspend fun getJobsByEmployer(employerId: String): Result<List<Map<String, Any>>> {
        return try {
            val query = firestore.collection(JOBS_COLLECTION)
                .whereEqualTo("employerId", employerId)
                .get()
                .await()
            
            // Sort in memory to avoid index requirement
            val jobs = query.documents.mapNotNull { it.data }
                .sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a specific job by ID
     */
    suspend fun getJobById(jobId: String): Result<Map<String, Any>?> {
        return try {
            Timber.d("🔍 FirestoreService.getJobById - Looking for jobId: $jobId")
            
            // First try to get by document ID (most efficient)
            val document = firestore.collection(JOBS_COLLECTION).document(jobId).get().await()
            if (document.exists()) {
                Timber.d("🔍 FirestoreService.getJobById - Document found by ID: ${document.data}")
                Result.success(document.data)
            } else {
                // If not found by document ID, try to query by jobId field
                Timber.d("🔍 FirestoreService.getJobById - Not found by document ID, trying query by jobId field")
                val query = firestore.collection(JOBS_COLLECTION)
                    .whereEqualTo("jobId", jobId)
                    .limit(1)
                    .get()
                    .await()
                
                if (!query.isEmpty) {
                    val doc = query.documents.first()
                    Timber.d("🔍 FirestoreService.getJobById - Document found by query: ${doc.data}")
                    Result.success(doc.data)
                } else {
                    Timber.d("🔍 FirestoreService.getJobById - Document not found by query either")
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Timber.e("🔍 FirestoreService.getJobById - Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update a job posting
     */
    suspend fun updateJob(jobId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val data = updates.toMutableMap()
            data["updatedAt"] = System.currentTimeMillis()
            
            firestore.collection(JOBS_COLLECTION)
                .document(jobId)
                .update(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a job posting
     */
    suspend fun deleteJob(jobId: String): Result<Unit> {
        return try {
            firestore.collection(JOBS_COLLECTION)
                .document(jobId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Increment job view count
     */
    suspend fun incrementJobViewCount(jobId: String): Result<Unit> {
        return try {
            firestore.collection(JOBS_COLLECTION)
                .document(jobId)
                .update("viewCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search jobs by title or description
     */
    suspend fun searchJobs(query: String, limit: Long = 20L): Result<List<Map<String, Any>>> {
        return try {
            // Simple text search - for production, consider using Algolia or similar
            val jobsQuery = firestore.collection(JOBS_COLLECTION)
                .whereEqualTo("isActive", true)
                .orderBy("title")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(limit)
                .get()
                .await()
            
            val jobs = jobsQuery.documents.mapNotNull { it.data }
            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get jobs by category
     */
    suspend fun getJobsByCategory(category: String, limit: Long = 20L): Result<List<Map<String, Any>>> {
        return try {
            val query = firestore.collection(JOBS_COLLECTION)
                .whereEqualTo("category", category)
                .limit(limit * 2) // Get more to account for filtering
                .get()
                .await()
            
            // Filter active jobs and sort in memory to avoid index requirement
            val jobs = query.documents.mapNotNull { it.data }
                .filter { (it["isActive"] as? Boolean) == true }
                .sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
                .take(limit.toInt())
            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get jobs by location
     */
    suspend fun getJobsByLocation(location: String, limit: Long = 20L): Result<List<Map<String, Any>>> {
        return try {
            val query = firestore.collection(JOBS_COLLECTION)
                .whereEqualTo("location", location)
                .limit(limit * 2) // Get more to account for filtering
                .get()
                .await()
            
            // Filter active jobs and sort in memory to avoid index requirement
            val jobs = query.documents.mapNotNull { it.data }
                .filter { (it["isActive"] as? Boolean) == true }
                .sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
                .take(limit.toInt())
            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== SAVED JOBS METHODS ====================
    
    /**
     * Save a job for a worker (prevents duplicates)
     */
    suspend fun saveJob(workerId: String, jobId: String): Result<Unit> {
        return try {
            // First check if job is already saved
            val isAlreadySaved = isJobSaved(workerId, jobId)
            if (isAlreadySaved.isSuccess && isAlreadySaved.getOrNull() == true) {
                return Result.success(Unit) // Already saved, return success
            }
            
            val savedJobRef = firestore.collection(SAVED_JOBS_COLLECTION).document()
            val savedJobData = mapOf(
                "id" to savedJobRef.id,
                "workerId" to workerId,
                "jobId" to jobId,
                "savedAt" to System.currentTimeMillis(),
                "isActive" to true
            )
            savedJobRef.set(savedJobData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove a saved job for a worker
     */
    suspend fun unsaveJob(workerId: String, jobId: String): Result<Unit> {
        return try {
            val query = firestore.collection(SAVED_JOBS_COLLECTION)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            for (document in query.documents) {
                document.reference.update("isActive", false).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if a job is saved by a worker
     */
    suspend fun isJobSaved(workerId: String, jobId: String): Result<Boolean> {
        return try {
            val query = firestore.collection(SAVED_JOBS_COLLECTION)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            Result.success(!query.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all saved jobs for a worker
     */
    suspend fun getSavedJobs(workerId: String): Result<List<Map<String, Any>>> {
        return try {
            Timber.d("🔍 DEBUG FirestoreService: Getting saved jobs for worker: $workerId")
            val query = firestore.collection(SAVED_JOBS_COLLECTION)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            Timber.d("🔍 DEBUG FirestoreService: Found ${query.documents.size} saved job documents")
            val savedJobIds = query.documents.mapNotNull { it.data?.get("jobId") as? String }
            Timber.d("🔍 DEBUG FirestoreService: Saved job IDs: $savedJobIds")
            
            if (savedJobIds.isEmpty()) {
                Timber.d("🔍 DEBUG FirestoreService: No saved jobs found")
                return Result.success(emptyList())
            }
            
            // Get the actual job data for saved job IDs
            val jobsQuery = firestore.collection(JOBS_COLLECTION)
                .whereIn("jobId", savedJobIds.take(10)) // Firestore limit is 10 for 'in' queries
                .get()
                .await()
            
            Timber.d("🔍 DEBUG FirestoreService: Found ${jobsQuery.documents.size} job documents")
            
            val jobs = jobsQuery.documents.mapNotNull { it.data }
                .filter { (it["isActive"] as? Boolean) == true }
                .sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
            
            Timber.d("🔍 DEBUG FirestoreService: Returning ${jobs.size} active jobs")
            Result.success(jobs)
        } catch (e: Exception) {
            Timber.e("❌ DEBUG FirestoreService: Error getting saved jobs: ${e.message}")
            Result.failure(e)
        }
    }

}
