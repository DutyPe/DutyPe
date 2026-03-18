package com.example.dutype.services.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
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
        const val APPLICATIONS_COLLECTION = "applications"
        const val SAVED_JOBS_COLLECTION = "saved_jobs"
    }
    
    // ==================== SAVED JOBS METHODS (OPTIMIZED) ====================
    
    /**
     * Save a job for a worker
     * OPTIMIZED: Single atomic write with FieldValue.arrayUnion (no read needed)
     */
    suspend fun saveJob(workerId: String, jobId: String): Result<Unit> {
        return try {
            val saveId = "${workerId}_${jobId}"
            val payload = mapOf(
                "userId" to workerId,
                "jobId" to jobId,
                "createdAt" to Timestamp.now()
            )
            firestore.collection(SAVED_JOBS_COLLECTION)
                .document(saveId)
                .set(payload)
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
            val saveId = "${workerId}_${jobId}"
            firestore.collection(SAVED_JOBS_COLLECTION)
                .document(saveId)
                .delete()
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
            val saveId = "${workerId}_${jobId}"
            val savedDoc = firestore.collection(SAVED_JOBS_COLLECTION)
                .document(saveId)
                .get()
                .await()
            Result.success(savedDoc.exists())
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
            val savedJobsSnapshot = firestore.collection(SAVED_JOBS_COLLECTION)
                .whereEqualTo("userId", workerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .get()
                .await()

            val savedJobIds = savedJobsSnapshot.documents
                .mapNotNull { it.getString("jobId") }
            
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
                    if (data != null && (data["status"] as? String) == "open") {
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
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
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
