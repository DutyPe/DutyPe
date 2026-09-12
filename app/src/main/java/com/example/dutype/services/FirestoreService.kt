package com.example.dutype.services

import com.example.dutype.models.User
import com.example.dutype.services.firestore.UserFirestoreService
import com.example.dutype.services.firestore.JobFirestoreService
import com.example.dutype.services.firestore.ApplicationFirestoreService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirestoreService - Facade for all Firestore operations
 * 
 * Delegates to domain-specific services:
 * - UserFirestoreService: User CRUD and profile operations
 * - JobFirestoreService: Job CRUD and queries
 * - ApplicationFirestoreService: Saved jobs and application queries
 */
@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userService: UserFirestoreService,
    private val jobService: JobFirestoreService,
    private val applicationService: ApplicationFirestoreService
) {
    
    // ==================== USER METHODS ====================
    
    suspend fun getUserById(userId: String): Result<User?> = userService.getUserById(userId)
    
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> =
        userService.updateUserProfile(userId, updates)
    
    suspend fun getUserSummary(userId: String): Result<Map<String, Any?>?> = userService.getUserSummary(userId)
    
    suspend fun getUserSummaries(userIds: List<String>): Result<List<Map<String, Any?>>> =
        userService.getUserSummaries(userIds)

    
    // ==================== JOB METHODS ====================
    
    suspend fun createJob(jobData: Map<String, Any>): Result<String> = jobService.createJob(jobData)
    
    suspend fun getAllJobs(limit: Long = 50L, lastCreatedAt: Long? = null): Result<List<Map<String, Any>>> =
        jobService.getAllJobs(limit, lastCreatedAt)
    
    suspend fun getAllJobsSummary(
        limit: Long = 50L,
        lastDocumentId: String? = null,
        category: String? = null,
        userLatitude: Double? = null,
        userLongitude: Double? = null,
        radiusKm: Double = 10.0
    ): Result<List<Map<String, Any>>> =
        jobService.getAllJobsSummary(limit, lastDocumentId, category, userLatitude, userLongitude, radiusKm)

    suspend fun getNearbyJobsSummary(
        userLatitude: Double,
        userLongitude: Double,
        radiusKm: Double = 10.0,
        category: String? = null,
        limitPerCell: Long = 50L
    ): Result<List<Map<String, Any>>> =
        jobService.getNearbyJobsSummary(userLatitude, userLongitude, radiusKm, category, limitPerCell)
    
    fun getJobsByEmployerRealtime(employerId: String): Flow<Result<List<Map<String, Any>>>> =
        jobService.getJobsByEmployerRealtime(employerId)
    
    suspend fun getJobById(jobId: String): Result<Map<String, Any>?> = jobService.getJobById(jobId)
    
    suspend fun updateJob(jobId: String, updates: Map<String, Any>): Result<Unit> =
        jobService.updateJob(jobId, updates)
    
    suspend fun deleteJob(jobId: String): Result<Unit> = jobService.deleteJob(jobId)
    
    suspend fun pauseJob(jobId: String): Result<Unit> = jobService.pauseJob(jobId)

    suspend fun closeJob(jobId: String): Result<Unit> = jobService.closeJob(jobId)
    
    suspend fun resumeJob(jobId: String): Result<Unit> = jobService.resumeJob(jobId)
    
    suspend fun renewJob(jobId: String, employerId: String): Result<Unit> = jobService.renewJob(jobId, employerId)
    
    suspend fun searchJobs(query: String, limit: Long = 20L): Result<List<Map<String, Any>>> =
        jobService.searchJobs(query, limit)
    
    suspend fun getJobsByCategory(category: String, limit: Long = 20L): Result<List<Map<String, Any>>> =
        jobService.getJobsByCategory(category, limit)
    
    suspend fun getJobsByLocation(location: String, limit: Long = 20L): Result<List<Map<String, Any>>> =
        jobService.getJobsByLocation(location, limit)
    
    suspend fun getTotalJobCount(): Result<Int> = jobService.getTotalJobCount()
    
    suspend fun getJobsFiltered(
        category: String? = null,
        minSalary: Int? = null,
        maxSalary: Int? = null,
        payType: String? = null,
        gender: String? = null,
        jobType: String? = null,
        limit: Long = 50L,
        lastDocumentId: String? = null
    ): Result<List<Map<String, Any>>> = jobService.getJobsFiltered(
        category, minSalary, maxSalary, payType, gender, jobType, limit, lastDocumentId
    )
    
    // ==================== SAVED JOBS & APPLICATIONS ====================
    
    suspend fun saveJob(workerId: String, jobId: String): Result<Unit> =
        applicationService.saveJob(workerId, jobId)
    
    suspend fun unsaveJob(workerId: String, jobId: String): Result<Unit> =
        applicationService.unsaveJob(workerId, jobId)
    
    suspend fun isJobSaved(workerId: String, jobId: String): Result<Boolean> =
        applicationService.isJobSaved(workerId, jobId)
    
    suspend fun getSavedJobs(workerId: String): Result<List<Map<String, Any>>> =
        applicationService.getSavedJobs(workerId)
    
    suspend fun getApplicationsByWorker(workerId: String): Result<List<Map<String, Any>>> =
        applicationService.getApplicationsByWorker(workerId)
}
