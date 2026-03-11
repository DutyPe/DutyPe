package com.example.dutype.services.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
     * OPTIMIZED: Single atomic write with FieldValue.arrayUnion (no read needed)
     */
    suspend fun saveJob(workerId: String, jobId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(workerId)
                .update("savedJobs", FieldValue.arrayUnion(jobId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error saving job")
            Result.failure(e)
        }
    }
    
    /**
     * Remove a saved job for a worker
     * OPTIMIZED: Single atomic write with FieldValue.arrayRemove (no read needed)
     */
    suspend fun unsaveJob(workerId: String, jobId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(workerId)
                .update("savedJobs", FieldValue.arrayRemove(jobId))
                .await()
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
     * OPTIMIZED: Batched whereIn queries (10 per batch) instead of N individual reads
     */
    suspend fun getSavedJobs(workerId: String): Result<List<Map<String, Any>>> {
        return try {
            // Get saved job IDs from users collection
            val userDoc = firestore.collection(USERS_COLLECTION).document(workerId).get().await()
            val savedJobIds = (userDoc.get("savedJobs") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            
            if (savedJobIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Batch fetch using whereIn (Firestore limit: 10 per query)
            val allJobs = mutableListOf<Map<String, Any>>()
            
            for (chunk in savedJobIds.chunked(10)) {
                val snapshot = firestore.collection(JOBS_COLLECTION)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                
                snapshot.documents.forEach { doc ->
                    val data = doc.data
                    if (data != null && (data["isActive"] as? Boolean) == true) {
                        allJobs.add(data)
                    }
                }
            }
            
            val sortedJobs = allJobs.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
            Result.success(sortedJobs)
        } catch (e: Exception) {
            Timber.e(e, "Error getting saved jobs")
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
                .limit(200) // P0 FIX: Prevent unbounded reads at 5L+ scale
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
