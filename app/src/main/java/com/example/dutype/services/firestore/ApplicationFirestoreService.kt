package com.example.dutype.services.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ApplicationFirestoreService - Handles saved jobs and application-related Firestore operations
 * 
 * Extracted from FirestoreService as part of architecture refactoring.
 * Single responsibility: Saved jobs and application queries.
 * 
 * Note: Core application logic (submit, withdraw, status updates) remains in JobApplicationService.
 * This service handles the basic Firestore operations for saved jobs and application queries.
 * 
 * @author DutyPe Engineering Team
 * @since 2.1.0
 */
@Singleton
class ApplicationFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        const val JOBS_COLLECTION = "jobs"
        const val SAVED_JOBS_COLLECTION = "saved_jobs"
        const val APPLICATIONS_COLLECTION = "applications"
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
                return Result.success(Unit)
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
     * PERFORMANCE FIX: Uses chunked "in" queries to handle Firestore's 10-item limit
     * Processes chunks in parallel for better performance
     */
    suspend fun getSavedJobs(workerId: String): Result<List<Map<String, Any>>> {
        return try {
            Timber.d("🔍 DEBUG ApplicationFirestoreService: Getting saved jobs for worker: $workerId")
            val query = firestore.collection(SAVED_JOBS_COLLECTION)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            Timber.d("🔍 DEBUG ApplicationFirestoreService: Found ${query.documents.size} saved job documents")
            val savedJobIds = query.documents.mapNotNull { it.data?.get("jobId") as? String }
            Timber.d("🔍 DEBUG ApplicationFirestoreService: Saved job IDs: $savedJobIds")
            
            if (savedJobIds.isEmpty()) {
                Timber.d("🔍 DEBUG ApplicationFirestoreService: No saved jobs found")
                return Result.success(emptyList())
            }
            
            // PERFORMANCE FIX: Chunk job IDs to handle Firestore's 10-item "in" query limit
            // Process chunks in parallel for better performance
            val chunks = savedJobIds.chunked(10)
            Timber.d("📦 BATCH: Fetching ${savedJobIds.size} saved jobs in ${chunks.size} chunks")
            
            val allJobs = mutableListOf<Map<String, Any>>()
            
            coroutineScope {
                val deferredResults = chunks.map { chunk ->
                    async(Dispatchers.IO) {
                        try {
                            val jobsQuery = firestore.collection(JOBS_COLLECTION)
                                .whereIn("jobId", chunk)
                                .get()
                                .await()
                            
                            jobsQuery.documents.mapNotNull { it.data }
                                .filter { (it["isActive"] as? Boolean) == true }
                        } catch (e: Exception) {
                            Timber.w(e, "📦 BATCH: Error fetching chunk of saved jobs")
                            emptyList()
                        }
                    }
                }
                
                // Collect all results
                deferredResults.forEach { deferred ->
                    allJobs.addAll(deferred.await())
                }
            }
            
            // Sort by createdAt descending
            val sortedJobs = allJobs.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
            
            Timber.d("📦 BATCH: Successfully fetched ${sortedJobs.size} saved jobs")
            Result.success(sortedJobs)
        } catch (e: Exception) {
            Timber.e("❌ DEBUG ApplicationFirestoreService: Error getting saved jobs: ${e.message}")
            Result.failure(e)
        }
    }
    
    // ==================== APPLICATION QUERY METHODS ====================
    
    /**
     * Get all applications by a worker
     */
    suspend fun getApplicationsByWorker(workerId: String): Result<List<Map<String, Any>>> {
        return try {
            Timber.d("🔍 ApplicationFirestoreService: Getting applications for worker: $workerId")
            val query = firestore.collection(APPLICATIONS_COLLECTION)
                .whereEqualTo("workerId", workerId)
                .get()
                .await()
            
            val applications = query.documents.mapNotNull { doc ->
                doc.data?.toMutableMap()?.apply {
                    put("applicationId", doc.id)
                }
            }
            
            Timber.d("🔍 ApplicationFirestoreService: Found ${applications.size} applications for worker")
            Result.success(applications)
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFirestoreService: Error getting applications for worker")
            Result.failure(e)
        }
    }
}
