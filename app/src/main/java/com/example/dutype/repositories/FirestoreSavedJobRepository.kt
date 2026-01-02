package com.example.dutype.repositories

import com.example.dutype.cache.JobCacheManager
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.services.FirestoreService
import com.example.dutype.utils.toJobListing
import com.example.dutype.utils.toJobListingSummary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * FirestoreSavedJobRepository - Handles saved jobs operations
 * 
 * REFACTORED (January 2026):
 * - Fixed stale currentUser reference bug (P0 CRITICAL)
 * - Now fetches currentUser dynamically in each method
 * - Uses shared toJobListing() extension function
 * - Removed duplicate convertMapToJobListing()
 * 
 * @author DutyPe Engineering Team
 * @since 2.1.0
 */
@Singleton
class FirestoreSavedJobRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val cacheManager: JobCacheManager,
    private val auth: FirebaseAuth // Inject FirebaseAuth instead of capturing currentUser
) {
    
    /**
     * Get current user ID dynamically to avoid stale reference
     * This is called in each method to ensure we always have the current user
     */
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    fun getSavedJobs(): Flow<Result<List<JobListing>>> = flow {
        val workerId = getCurrentUserId()
        if (workerId == null) {
            emit(Result.failure(Exception("User not authenticated")))
            return@flow
        }
        
        val result = firestoreService.getSavedJobs(workerId)
        result.fold(
            onSuccess = { jobMaps ->
                // Use shared extension function for conversion
                val jobListings = jobMaps.mapNotNull { jobMap ->
                    try {
                        jobMap.toJobListing(isSaved = true)
                    } catch (e: Exception) {
                        Timber.e(e, "Error converting job map to JobListing")
                        null
                    }
                }
                emit(Result.success(jobListings))
            },
            onFailure = { exception ->
                emit(Result.failure(exception))
            }
        )
    }
    
    /**
     * PERFORMANCE OPTIMIZATION: Get saved jobs as summaries
     * Returns lightweight JobListingSummary for list views
     */
    fun getSavedJobSummaries(): Flow<Result<List<JobListingSummary>>> = flow {
        val workerId = getCurrentUserId()
        if (workerId == null) {
            emit(Result.failure(Exception("User not authenticated")))
            return@flow
        }
        
        val result = firestoreService.getSavedJobs(workerId)
        result.fold(
            onSuccess = { jobMaps ->
                // Use shared extension function for conversion
                val summaries = jobMaps.mapNotNull { jobMap ->
                    try {
                        jobMap.toJobListingSummary(isSaved = true)
                    } catch (e: Exception) {
                        Timber.e(e, "Error converting job map to JobListingSummary")
                        null
                    }
                }
                emit(Result.success(summaries))
            },
            onFailure = { exception ->
                emit(Result.failure(exception))
            }
        )
    }
    
    suspend fun saveJob(jobId: String): Result<Unit> {
        val workerId = getCurrentUserId()
        if (workerId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        
        val result = firestoreService.saveJob(workerId, jobId)
        // Update cache on success
        result.onSuccess {
            cacheManager.addSavedJobId(jobId)
            cacheManager.updateSummarySavedStatus(jobId, true)
        }
        return result
    }
    
    suspend fun unsaveJob(jobId: String): Result<Unit> {
        val workerId = getCurrentUserId()
        if (workerId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        
        val result = firestoreService.unsaveJob(workerId, jobId)
        // Update cache on success
        result.onSuccess {
            cacheManager.removeSavedJobId(jobId)
            cacheManager.updateSummarySavedStatus(jobId, false)
        }
        return result
    }
    
    suspend fun isJobSaved(jobId: String): Result<Boolean> {
        val workerId = getCurrentUserId()
        if (workerId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        
        return firestoreService.isJobSaved(workerId, jobId)
    }
}
