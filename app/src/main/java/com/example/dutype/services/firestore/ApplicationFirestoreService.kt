package com.example.dutype.services.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldPath
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

    private fun toEpochMillis(value: Any?): Long {
        return when (value) {
            is Timestamp -> value.toDate().time
            is Number -> value.toLong()
            is java.util.Date -> value.time
            else -> 0L
        }
    }

    private fun normalizeReadStatus(data: Map<String, Any>): String {
        val explicit = (data["status"] as? String)?.trim()?.lowercase()
        if (explicit == "open" || explicit == "closed" || explicit == "expired") {
            return explicit
        }
        val isActive = data["isActive"] as? Boolean
        val isFilled = data["isFilled"] as? Boolean
        if (isActive == true && isFilled != true) return "open"
        if (isActive == true && isFilled == true) return "closed"
        return "closed"
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
                "id" to saveId,
                "userId" to workerId,
                "workerId" to workerId,
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
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Timber.w("saved_jobs read denied by rules for workerId=%s, jobId=%s; treating as not saved", workerId, jobId)
                return Result.success(false)
            }
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
            val userIdSnapshot = firestore.collection(SAVED_JOBS_COLLECTION)
                .whereEqualTo("userId", workerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .get()
                .await()

            // Backward compatibility for older docs that stored owner as workerId.
            val legacyWorkerSnapshot = firestore.collection(SAVED_JOBS_COLLECTION)
                .whereEqualTo("workerId", workerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .get()
                .await()

            val mergedSavedDocs = (userIdSnapshot.documents + legacyWorkerSnapshot.documents)
                .associateBy { it.id }
                .values
                .toList()

            val savedJobIds = mergedSavedDocs
                .mapNotNull { it.getString("jobId") }
                .distinct()
            
            if (savedJobIds.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // Batch fetch using whereIn (Firestore limit: 10 per query)
            val allJobs = mutableListOf<Map<String, Any>>()
            
            for (chunk in savedJobIds.chunked(10)) {
                val snapshot = firestore.collection(JOBS_COLLECTION)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                
                snapshot.documents.forEach { doc ->
                    val data = doc.data
                    if (data != null) {
                        val expiresAt = toEpochMillis(data["expiresAt"])
                        val currentTime = System.currentTimeMillis()
                        val normalizedStatus = normalizeReadStatus(data)
                        val resolvedStatus = if (expiresAt > 0L && expiresAt <= currentTime) {
                            "expired"
                        } else {
                            normalizedStatus
                        }

                        // Saved jobs screen should show what the user saved, even if closed/expired.
                        allJobs.add(data.toMutableMap().apply {
                            put("jobId", doc.id)
                            put("status", resolvedStatus)
                            put("expiresAt", expiresAt)
                        })
                    }
                }
            }
            
            val sortedJobs = allJobs.sortedByDescending { toEpochMillis(it["createdAt"]) }
            Result.success(sortedJobs)
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Timber.w("saved_jobs read denied by rules for workerId=%s; returning empty saved list", workerId)
                return Result.success(emptyList())
            }
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
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Timber.w("applications read denied by rules for workerId=%s; returning empty applications", workerId)
                return Result.success(emptyList())
            }
            Timber.e(e, "❌ ApplicationFirestoreService: Error getting applications for worker")
            Result.failure(e)
        }
    }
}
