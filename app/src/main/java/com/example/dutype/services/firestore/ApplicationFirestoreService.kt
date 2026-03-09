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
        const val USERS_COLLECTION = "users"
        const val JOBS_COLLECTION = "jobs"
        const val APPLICATIONS_COLLECTION = "job_applications"
    }
    
    // ==================== SAVED JOBS METHODS (OPTIMIZED) ====================
    
    /**
     * Save a job for a worker
     * OPTIMIZED: Stores in users.savedJobs array instead of separate collection
     */
    suspend fun saveJob(workerId: String, jobId: String): Result<Unit> {
        return try {
            // Get current saved jobs
            val userDoc = firestore.collection(USERS_COLLECTION).document(workerId).get().await()
            val currentSavedJobs = (userDoc.get("savedJobs") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
            
            // Add if not already saved
            if (!currentSavedJobs.contains(jobId)) {
                currentSavedJobs.add(jobId)
                firestore.collection(USERS_COLLECTION)
                    .document(workerId)
                    .update("savedJobs", currentSavedJobs)
                    .await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving job")
            Result.failure(e)
        }
    }
    
    /**
     * Remove a saved job for a worker
     * OPTIMIZED: Removes from users.savedJobs array
     */
    suspend fun unsaveJob(workerId: String, jobId: String): Result<Unit> {
        return try {
            // Get current saved jobs
            val userDoc = firestore.collection(USERS_COLLECTION).document(workerId).get().await()
            val currentSavedJobs = (userDoc.get("savedJobs") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
            
            // Remove if exists
            if (currentSavedJobs.remove(jobId)) {
                firestore.collection(USERS_COLLECTION)
                    .document(workerId)
                    .update("savedJobs", currentSavedJobs)
                    .await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error unsaving job")
            Result.failure(e)
        }
    }
    
    /**
     * Check if a job is saved by a worker
     * OPTIMIZED: Reads from users.savedJobs array
     */
    suspend fun isJobSaved(workerId: String, jobId: String): Result<Boolean> {
        return try {
            val userDoc = firestore.collection(USERS_COLLECTION).document(workerId).get().await()
            val savedJobs = (userDoc.get("savedJobs") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            Result.success(savedJobs.contains(jobId))
        } catch (e: Exception) {
            Timber.e(e, "Error checking if job is saved")
            Result.failure(e)
        }
    }
    
    /**
     * Get all saved jobs for a worker
     * OPTIMIZED: Reads from users.savedJobs array, then fetches job details
     * PERFORMANCE FIX: Uses chunked "in" queries to handle Firestore's 10-item limit
     * Processes chunks in parallel for better performance
     */
    suspend fun getSavedJobs(workerId: String): Result<List<Map<String, Any>>> {
        return try {
            Timber.d("🔍 ApplicationFirestoreService: Getting saved jobs for worker: $workerId")
            
            // Get saved job IDs from users collection
            val userDoc = firestore.collection(USERS_COLLECTION).document(workerId).get().await()
            val savedJobIds = (userDoc.get("savedJobs") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            
            Timber.d("🔍 ApplicationFirestoreService: Found ${savedJobIds.size} saved job IDs")
            
            if (savedJobIds.isEmpty()) {
                Timber.d("🔍 ApplicationFirestoreService: No saved jobs found")
                return Result.success(emptyList())
            }
            
            // PERFORMANCE FIX: Fetch jobs by document ID (not by field query)
            // Firestore document IDs are the job IDs
            Timber.d("📦 BATCH: Fetching ${savedJobIds.size} saved jobs by document ID")
            
            val allJobs = mutableListOf<Map<String, Any>>()
            
            coroutineScope {
                val deferredResults = savedJobIds.map { jobId ->
                    async(Dispatchers.IO) {
                        try {
                            val jobDoc = firestore.collection(JOBS_COLLECTION)
                                .document(jobId)
                                .get()
                                .await()
                            
                            if (jobDoc.exists()) {
                                val jobData = jobDoc.data
                                if (jobData != null && (jobData["isActive"] as? Boolean) == true) {
                                    jobData
                                } else {
                                    null
                                }
                            } else {
                                Timber.w("📦 Job document not found: $jobId")
                                null
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "📦 Error fetching saved job: $jobId")
                            null
                        }
                    }
                }
                
                // Collect all results
                deferredResults.forEach { deferred ->
                    deferred.await()?.let { allJobs.add(it) }
                }
            }
            
            // Deduplicate by job ID (document ID) and sort by createdAt descending
            val uniqueJobs = allJobs.distinctBy { it["id"] as? String ?: "" }
            val sortedJobs = uniqueJobs.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
            
            Timber.d("📦 BATCH: Successfully fetched ${sortedJobs.size} saved jobs (${allJobs.size - sortedJobs.size} duplicates removed)")
            Result.success(sortedJobs)
        } catch (e: Exception) {
            Timber.e(e, "❌ ApplicationFirestoreService: Error getting saved jobs")
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
