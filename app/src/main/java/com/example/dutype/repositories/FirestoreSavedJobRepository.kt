package com.example.dutype.repositories

import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.performance.MainThreadChecker
import com.example.dutype.services.FirestoreService
import com.example.dutype.utils.toJobListing
import com.example.dutype.utils.toJobListingSummary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * FirestoreSavedJobRepository - Handles saved jobs operations
 * 
 * SIMPLE IMPLEMENTATION (February 2026):
 * - NO CACHE - Always fetch fresh from Firestore
 * - NO STATE MANAGER - Direct Firestore updates
 * - Like Naukri/Lokal Jobs - Simple and reliable
 * 
 * @author DutyPe Engineering Team
 * @since 2.5.0
 */
@Singleton
class FirestoreSavedJobRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val auth: FirebaseAuth // Inject FirebaseAuth instead of capturing currentUser
) {
    
    /**
     * Get current user ID dynamically to avoid stale reference
     * This is called in each method to ensure we always have the current user
     */
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    fun getSavedJobs(): Flow<Result<List<JobListing>>> = flow {
        MainThreadChecker.assertBackgroundThread("FirestoreSavedJobRepository.getSavedJobs")
        
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
    }.flowOn(Dispatchers.IO)
    
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
    }.flowOn(Dispatchers.IO)
    
    suspend fun saveJob(jobId: String): Result<Unit> {
        MainThreadChecker.assertBackgroundThread("FirestoreSavedJobRepository.saveJob")
        
        val workerId = getCurrentUserId()
        if (workerId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        
        // SIMPLE: Just update Firestore, no cache
        return firestoreService.saveJob(workerId, jobId)
    }
    
    suspend fun unsaveJob(jobId: String): Result<Unit> {
        MainThreadChecker.assertBackgroundThread("FirestoreSavedJobRepository.unsaveJob")
        
        val workerId = getCurrentUserId()
        if (workerId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        
        // SIMPLE: Just update Firestore, no cache
        return firestoreService.unsaveJob(workerId, jobId)
    }
    
    suspend fun isJobSaved(jobId: String): Result<Boolean> {
        MainThreadChecker.assertBackgroundThread("FirestoreSavedJobRepository.isJobSaved")
        
        val workerId = getCurrentUserId()
        if (workerId == null) {
            return Result.failure(Exception("User not authenticated"))
        }
        
        return firestoreService.isJobSaved(workerId, jobId)
    }
}
