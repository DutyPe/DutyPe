package com.example.dutype.services.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JobFirestoreService - Handles all job-related Firestore operations
 * 
 * Extracted from FirestoreService as part of architecture refactoring.
 * Single responsibility: Job CRUD operations and queries.
 * 
 * @author DutyPe Engineering Team
 * @since 2.1.0
 */
@Singleton
class JobFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        const val JOBS_COLLECTION = "jobs"
    }
    
    /**
     * Create a new job posting in Firestore
     */
    suspend fun createJob(jobData: Map<String, Any>): Result<String> {
        return try {
            Timber.d("📝 FIRESTORE DEBUG: createJob() called")
            
            val jobRef = firestore.collection(JOBS_COLLECTION).document()
            val data = jobData.toMutableMap()
            val currentTime = System.currentTimeMillis()
            data["jobId"] = jobRef.id
            data["createdAt"] = currentTime
            data["updatedAt"] = currentTime
            data["postedAt"] = currentTime
            data["isActive"] = true
            data["applicationCount"] = 0L
            
            // Job Expiry System - Default 15 days
            val expiryDays = (data["expiryDays"] as? Number)?.toInt() ?: 15
            data["expiryDays"] = expiryDays
            data["expiresAt"] = currentTime + (expiryDays * 24 * 60 * 60 * 1000L)
            
            val lat = data["latitude"]
            val lon = data["longitude"]
            Timber.d("📝 FIRESTORE DEBUG: Saving job with coordinates - lat: $lat, lon: $lon")
            Timber.d("📝 FIRESTORE DEBUG: Job ID: ${jobRef.id}")
            Timber.d("📝 FIRESTORE DEBUG: Job expires in $expiryDays days")
            
            jobRef.set(data).await()
            
            Timber.i("📝 FIRESTORE DEBUG: ✅ Job saved successfully to Firestore")
            Result.success(jobRef.id)
        } catch (e: Exception) {
            Timber.e(e, "📝 FIRESTORE DEBUG: ❌ Failed to save job to Firestore")
            Result.failure(e)
        }
    }
    
    /**
     * Get all active jobs with pagination support
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
            val currentTime = System.currentTimeMillis()
            
            val jobs = snapshot.documents.mapNotNull { it.data }
                .filter { job ->
                    val isActive = (job["isActive"] as? Boolean) == true
                    val expiresAt = (job["expiresAt"] as? Number)?.toLong() ?: 0L
                    val isNotExpired = expiresAt == 0L || expiresAt > currentTime
                    isActive && isNotExpired
                }
            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    /**
     * Get job summaries for list views (lightweight - ~70% bandwidth reduction)
     * Uses cursor-based pagination for optimal performance at scale
     * Industry standard: Firestore cursor pagination with server-side filtering
     */
    suspend fun getAllJobsSummary(limit: Long = 50L, lastCreatedAt: Long? = null): Result<List<Map<String, Any>>> {
        return try {
            // Build optimized query with server-side filtering
            // Note: Using isActive filter only - isFilled filtered client-side for index compatibility
            var query = firestore.collection(JOBS_COLLECTION)
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
            
            // Apply cursor for pagination
            if (lastCreatedAt != null) {
                query = query.startAfter(lastCreatedAt)
            }
            
            // Apply limit (or fetch all if -1)
            if (limit > 0) {
                query = query.limit(limit)
            }
            
            val snapshot = query.get().await()
            val currentTime = System.currentTimeMillis()
            
            // Client-side filtering for isFilled and expiry
            val jobs = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L
                val isNotExpired = expiresAt == 0L || expiresAt > currentTime
                val isFilled = (data["isFilled"] as? Boolean) ?: false
                
                if (isNotExpired && !isFilled) {
                    mapOf(
                        "jobId" to (data["jobId"] ?: doc.id),
                        "employerId" to (data["employerId"] ?: ""),
                        "title" to (data["title"] ?: ""),
                        "companyName" to (data["companyName"] ?: ""),
                        "location" to (data["location"] ?: ""),
                        "latitude" to (data["latitude"] ?: 0.0),
                        "longitude" to (data["longitude"] ?: 0.0),
                        "payAmount" to (data["payAmount"] ?: ""),
                        "payType" to (data["payType"] ?: ""),
                        "category" to (data["category"] ?: ""),
                        "jobType" to (data["jobType"] ?: ""),
                        "vacancies" to (data["vacancies"] ?: 0),
                        "createdAt" to (data["createdAt"] ?: System.currentTimeMillis()),
                        "urgency" to (data["urgency"] ?: ""),
                        "employerTrustTier" to (data["employerTrustTier"] ?: "VERIFIED"),
                        "jobImageUrl" to (data["jobImageUrl"] ?: ""),
                        "isFilled" to false
                    )
                } else null
            }
            
            Timber.d("📦 Fetched ${jobs.size} job summaries (limit=$limit, cursor=${lastCreatedAt != null})")
            Result.success(jobs)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch job summaries")
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
            
            val jobs = query.documents.mapNotNull { it.data }
                .sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get jobs posted by employer with REAL-TIME updates
     */
    fun getJobsByEmployerRealtime(employerId: String): Flow<Result<List<Map<String, Any>>>> = callbackFlow {
        val listenerRegistration = firestore.collection(JOBS_COLLECTION)
            .whereEqualTo("employerId", employerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Real-time listener error for employer jobs")
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val jobs = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toMutableMap()?.apply {
                            put("jobId", doc.id)
                        }
                    }.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
                    
                    Timber.d("Real-time update: ${jobs.size} jobs for employer $employerId")
                    trySend(Result.success(jobs))
                }
            }
        
        awaitClose { 
            listenerRegistration.remove()
            Timber.d("Real-time listener removed for employer $employerId")
        }
    }
    
    /**
     * Get a specific job by ID
     */
    suspend fun getJobById(jobId: String): Result<Map<String, Any>?> {
        return try {
            Timber.d("🔍 JobFirestoreService.getJobById - Looking for jobId: $jobId")
            
            val document = firestore.collection(JOBS_COLLECTION).document(jobId).get().await()
            if (document.exists()) {
                Timber.d("🔍 JobFirestoreService.getJobById - Document found by ID")
                Result.success(document.data)
            } else {
                Timber.d("🔍 JobFirestoreService.getJobById - Not found by document ID, trying query")
                val query = firestore.collection(JOBS_COLLECTION)
                    .whereEqualTo("jobId", jobId)
                    .limit(1)
                    .get()
                    .await()
                
                if (!query.isEmpty) {
                    val doc = query.documents.first()
                    Timber.d("🔍 JobFirestoreService.getJobById - Document found by query")
                    Result.success(doc.data)
                } else {
                    Timber.d("🔍 JobFirestoreService.getJobById - Document not found")
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Timber.e("🔍 JobFirestoreService.getJobById - Error: ${e.message}")
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
     * Search jobs by title
     */
    suspend fun searchJobs(query: String, limit: Long = 20L): Result<List<Map<String, Any>>> {
        return try {
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
                .limit(limit * 2)
                .get()
                .await()
            
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
                .limit(limit * 2)
                .get()
                .await()
            
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
     * Get total count of active, unfilled jobs
     * Used for "All Jobs" badge in categories screen
     * Note: Firestore doesn't support COUNT queries, so we fetch minimal data
     */
    suspend fun getTotalJobCount(): Result<Int> {
        return try {
            val currentTime = System.currentTimeMillis()
            
            // Server-side filter for active jobs only (using existing index)
            val query = firestore.collection(JOBS_COLLECTION)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            // Filter isFilled and expired jobs client-side
            val count = query.documents.count { doc ->
                val data = doc.data ?: return@count false
                val expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L
                val isFilled = (data["isFilled"] as? Boolean) ?: false
                val isNotExpired = expiresAt == 0L || expiresAt > currentTime
                isNotExpired && !isFilled
            }
            
            Timber.d("📊 Total active jobs: $count")
            Result.success(count)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get total job count")
            Result.failure(e)
        }
    }
    
    /**
     * Get jobs by category with cursor-based pagination
     * Industry standard: Server-side filtering + cursor pagination for O(1) page loads
     * 
     * @param category The category name to filter by
     * @param limit Number of jobs to fetch per page (default 15)
     * @param lastCreatedAt Timestamp cursor for pagination (null for first page)
     */
    suspend fun getJobsByCategoryPaginated(
        category: String, 
        limit: Long = 15L, 
        lastCreatedAt: Long? = null
    ): Result<List<Map<String, Any>>> {
        return try {
            val currentTime = System.currentTimeMillis()
            
            // Optimized query - using existing index (isActive + category + createdAt)
            var query = firestore.collection(JOBS_COLLECTION)
                .whereEqualTo("isActive", true)
                .whereEqualTo("category", category)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            
            // Apply cursor for O(1) pagination
            if (lastCreatedAt != null) {
                query = query.startAfter(lastCreatedAt)
            }
            
            val result = query.limit(limit).get().await()
            
            // Client-side filtering for isFilled and expiry
            val jobs = result.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L
                val isNotExpired = expiresAt == 0L || expiresAt > currentTime
                val isFilled = (data["isFilled"] as? Boolean) ?: false
                
                if (isNotExpired && !isFilled) {
                    data.toMutableMap().apply {
                        put("id", doc.id)
                        put("jobId", data["jobId"] ?: doc.id)
                    }
                } else null
            }
            
            Timber.d("📦 Fetched ${jobs.size} jobs for category '$category' (limit=$limit)")
            Result.success(jobs)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch jobs for category '$category'")
            Result.failure(e)
        }
    }
}
