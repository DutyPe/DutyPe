package com.example.dutype.services

import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.example.dutype.services.firestore.UserFirestoreService
import com.example.dutype.services.firestore.JobFirestoreService
import com.example.dutype.services.firestore.ApplicationFirestoreService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirestoreService - Facade for all Firestore operations
 * 
 * REFACTORED (January 2026): Now acts as a facade delegating to domain-specific services:
 * - UserFirestoreService: User CRUD and profile operations
 * - JobFirestoreService: Job CRUD and queries
 * - ApplicationFirestoreService: Saved jobs and application queries
 * 
 * This facade maintains backward compatibility while allowing gradual migration
 * to the domain-specific services.
 * 
 * @author DutyPe Engineering Team
 * @since 2.1.0
 */
@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userService: UserFirestoreService,
    private val jobService: JobFirestoreService,
    private val applicationService: ApplicationFirestoreService
) {
    
    companion object {
        // Core Collections - Optimized Schema (8 collections)
        const val USERS_COLLECTION = "users"
        const val JOBS_COLLECTION = "jobs"
        const val APPLICATIONS_COLLECTION = "job_applications"
        const val NOTIFICATIONS_COLLECTION = "notifications"
    }
    
    // ==================== USER METHODS (delegated to UserFirestoreService) ====================
    
    suspend fun createOrUpdateUser(user: User): Result<Unit> = userService.createOrUpdateUser(user)
    
    suspend fun createOrUpdateWorkerProfile(userId: String, workerData: Map<String, Any>): Result<Unit> =
        userService.createOrUpdateWorkerProfile(userId, workerData)
    
    suspend fun createOrUpdateEmployerProfile(userId: String, employerData: Map<String, Any>): Result<Unit> =
        userService.createOrUpdateEmployerProfile(userId, employerData)
    
    suspend fun getWorkerProfile(userId: String): Result<Map<String, Any>?> =
        userService.getWorkerProfile(userId)
    
    suspend fun getEmployerProfile(userId: String): Result<Map<String, Any>?> =
        userService.getEmployerProfile(userId)
    
    suspend fun getUserById(userId: String): Result<User?> = userService.getUserById(userId)
    
    suspend fun getUserByEmail(email: String): Result<User?> = userService.getUserByEmail(email)
    
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> =
        userService.updateUserProfile(userId, updates)
    
    suspend fun deleteUser(userId: String): Result<Unit> = userService.deleteUser(userId)
    
    suspend fun getWorkerProfilesByEmail(email: String): Result<List<Map<String, Any>>> =
        userService.getWorkerProfilesByEmail(email)
    
    suspend fun getEmployerProfilesByEmail(email: String): Result<List<Map<String, Any>>> =
        userService.getEmployerProfilesByEmail(email)
    
    suspend fun getAllUsers(): Result<List<User>> = userService.getAllUsers()
    
    suspend fun getUsersByRole(role: UserRole): Result<List<User>> = userService.getUsersByRole(role)
    
    suspend fun userExistsByEmail(email: String): Result<Boolean> = userService.userExistsByEmail(email)
    
    suspend fun updateLastLogin(userId: String): Result<Unit> = userService.updateLastLogin(userId)
    
    suspend fun switchUserRole(userId: String, newRole: UserRole): Result<User> =
        userService.switchUserRole(userId, newRole)
    
    suspend fun getUserCount(): Result<Long> = userService.getUserCount()
    
    suspend fun searchUsers(query: String): Result<List<User>> = userService.searchUsers(query)
    
    suspend fun getUserSummary(userId: String): Result<Map<String, Any?>?> = userService.getUserSummary(userId)
    
    suspend fun getUserSummaries(userIds: List<String>): Result<List<Map<String, Any?>>> =
        userService.getUserSummaries(userIds)

    
    // ==================== JOB METHODS (delegated to JobFirestoreService) ====================
    
    suspend fun createJob(jobData: Map<String, Any>): Result<String> = jobService.createJob(jobData)
    
    suspend fun getAllJobs(limit: Long = 50L, lastCreatedAt: Long? = null): Result<List<Map<String, Any>>> =
        jobService.getAllJobs(limit, lastCreatedAt)
    
    suspend fun getAllJobsSummary(limit: Long = 50L, lastDocumentId: String? = null, category: String? = null): Result<List<Map<String, Any>>> =
        jobService.getAllJobsSummary(limit, lastDocumentId, category)
    
    suspend fun getJobsByEmployer(employerId: String): Result<List<Map<String, Any>>> =
        jobService.getJobsByEmployer(employerId)
    
    fun getJobsByEmployerRealtime(employerId: String): Flow<Result<List<Map<String, Any>>>> =
        jobService.getJobsByEmployerRealtime(employerId)
    
    suspend fun getJobById(jobId: String): Result<Map<String, Any>?> = jobService.getJobById(jobId)
    
    suspend fun updateJob(jobId: String, updates: Map<String, Any>): Result<Unit> =
        jobService.updateJob(jobId, updates)
    
    suspend fun deleteJob(jobId: String): Result<Unit> = jobService.deleteJob(jobId)
    
    suspend fun searchJobs(query: String, limit: Long = 20L): Result<List<Map<String, Any>>> =
        jobService.searchJobs(query, limit)
    
    suspend fun getJobsByCategory(category: String, limit: Long = 20L): Result<List<Map<String, Any>>> =
        jobService.getJobsByCategory(category, limit)
    
    suspend fun getJobsByLocation(location: String, limit: Long = 20L): Result<List<Map<String, Any>>> =
        jobService.getJobsByLocation(location, limit)
    
    suspend fun getTotalJobCount(): Result<Int> = jobService.getTotalJobCount()
    
    /**
     * P0 FIX: Server-side filtering for jobs
     * Reduces data transfer by 80-90% compared to client-side filtering
     */
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
    
    // ==================== SAVED JOBS METHODS (delegated to ApplicationFirestoreService) ====================
    
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
