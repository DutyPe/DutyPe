package com.example.dutype.repositories

import com.example.dutype.cache.JobCacheManager
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.services.FirestoreService
import com.example.dutype.utils.LocationService.Companion.calculateDistance
import com.example.dutype.utils.toJobListing
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class FirestoreJobRepository @Inject constructor(
    private val firestoreService: FirestoreService,
    private val auth: FirebaseAuth,
    private val cacheManager: JobCacheManager
) {
    
    /**
     * Create a new job posting
     */
    fun createJob(jobData: Map<String, Any>): Flow<Result<String>> = flow {
        try {
            val result = firestoreService.createJob(jobData)
            // Invalidate cache on job creation
            result.onSuccess { cacheManager.clearJobsCache() }
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get all active jobs (for workers) with saved status and pagination
     * Uses in-memory cache with TTL for performance
     */
    fun getAllJobs(limit: Long = 50L, lastCreatedAt: Long? = null): Flow<Result<List<JobListing>>> = flow {
        // Check cache first (only for initial load, not pagination)
        if (lastCreatedAt == null) {
            val cachedJobs = cacheManager.getAllJobsCached()
            if (cachedJobs != null) {
                Timber.d("✅ Returning ${cachedJobs.size} jobs from cache")
                emit(Result.success(cachedJobs))
                return@flow
            }
        }
        
        // Fetch from Firestore
        try {
            val result = firestoreService.getAllJobs(limit, lastCreatedAt)
            result.fold(
                onSuccess = { jobsData ->
                    // Use shared extension function for conversion
                    val jobListings = jobsData.map { it.toJobListing() }
                    
                    // Get saved job IDs (use cache if available)
                    val savedJobIds = getSavedJobIdsWithCache()
                    
                    // Update saved status
                    val updatedJobListings = jobListings.map { job ->
                        job.copy(isSaved = savedJobIds.contains(job.id))
                    }
                    
                    // Cache the results (only for initial load)
                    if (lastCreatedAt == null) {
                        cacheManager.cacheAllJobs(updatedJobListings)
                    }
                    
                    emit(Result.success(updatedJobListings))
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get saved job IDs with caching
     */
    private suspend fun getSavedJobIdsWithCache(): Set<String> {
        val currentUser = auth.currentUser ?: return emptySet()
        
        // Check cache first
        val cachedIds = cacheManager.getSavedJobIdsCached()
        if (cachedIds != null) {
            return cachedIds
        }
        
        // Fetch from Firestore
        val savedJobsResult = firestoreService.getSavedJobs(currentUser.uid)
        return savedJobsResult.fold(
            onSuccess = { savedJobsData ->
                val savedJobIds = savedJobsData.mapNotNull { it["jobId"] as? String }.toSet()
                cacheManager.cacheSavedJobIds(savedJobIds)
                savedJobIds
            },
            onFailure = { emptySet() }
        )
    }
    
    /**
     * Get jobs posted by a specific employer with REAL-TIME updates
     * Application count changes are reflected immediately
     */
    fun getJobsByEmployer(employerId: String): Flow<Result<List<JobListing>>> = 
        firestoreService.getJobsByEmployerRealtime(employerId).map { result ->
            result.map { jobsData ->
                // Use shared extension function for conversion
                jobsData.map { it.toJobListing() }
            }
        }.flowOn(Dispatchers.IO)
    
    /**
     * Get a specific job by ID with saved status
     * Uses cache for faster retrieval
     */
    fun getJobById(jobId: String): Flow<Result<JobListing?>> = flow {
        // Check cache first
        val cachedJob = cacheManager.getJobByIdCached(jobId)
        if (cachedJob != null) {
            Timber.d("✅ Returning job $jobId from cache")
            emit(Result.success(cachedJob))
            return@flow
        }
        
        try {
            val result = firestoreService.getJobById(jobId)
            result.fold(
                onSuccess = { jobData ->
                    // Use shared extension function for conversion
                    val jobListing = jobData?.toJobListing()
                    
                    // Check if this job is saved by the current user
                    val currentUser = auth.currentUser
                    if (currentUser != null && jobListing != null) {
                        val savedJobIds = getSavedJobIdsWithCache()
                        val updatedJob = jobListing.copy(isSaved = savedJobIds.contains(jobId))
                        // Cache the job
                        cacheManager.cacheJobById(updatedJob)
                        emit(Result.success(updatedJob))
                    } else {
                        jobListing?.let { cacheManager.cacheJobById(it) }
                        emit(Result.success(jobListing))
                    }
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update a job posting
     */
    fun updateJob(jobId: String, updates: Map<String, Any>): Flow<Result<Unit>> = flow {
        try {
            val result = firestoreService.updateJob(jobId, updates)
            // Invalidate cache on job update
            result.onSuccess { cacheManager.clearJobsCache() }
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Delete a job posting
     */
    fun deleteJob(jobId: String): Flow<Result<Unit>> = flow {
        try {
            val result = firestoreService.deleteJob(jobId)
            // Invalidate cache on job deletion
            result.onSuccess { cacheManager.clearJobsCache() }
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search jobs by query
     * Uses cache for repeated searches
     */
    fun searchJobs(query: String, limit: Long = 20L): Flow<Result<List<JobListing>>> = flow {
        // Check cache first
        val cachedResults = cacheManager.getSearchResultsCached(query)
        if (cachedResults != null) {
            Timber.d("✅ Returning search results for '$query' from cache")
            emit(Result.success(cachedResults))
            return@flow
        }
        
        try {
            val result = firestoreService.searchJobs(query, limit)
            result.fold(
                onSuccess = { jobsData ->
                    // Use shared extension function for conversion
                    val jobListings = jobsData.map { it.toJobListing() }
                    // Cache the results
                    cacheManager.cacheSearchResults(query, jobListings)
                    emit(Result.success(jobListings))
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get jobs by category
     * Uses cache for repeated category queries
     */
    fun getJobsByCategory(category: String, limit: Long = 20L): Flow<Result<List<JobListing>>> = flow {
        // Check cache first
        val cachedResults = cacheManager.getJobsByCategoryCached(category)
        if (cachedResults != null) {
            Timber.d("✅ Returning jobs for category '$category' from cache")
            emit(Result.success(cachedResults))
            return@flow
        }
        
        try {
            val result = firestoreService.getJobsByCategory(category, limit)
            result.fold(
                onSuccess = { jobsData ->
                    // Use shared extension function for conversion
                    val jobListings = jobsData.map { it.toJobListing() }
                    // Cache the results
                    cacheManager.cacheJobsByCategory(category, jobListings)
                    emit(Result.success(jobListings))
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get jobs by location
     * Uses cache for repeated location queries
     */
    fun getJobsByLocation(location: String, limit: Long = 20L): Flow<Result<List<JobListing>>> = flow {
        // Check cache first
        val cachedResults = cacheManager.getJobsByLocationCached(location)
        if (cachedResults != null) {
            Timber.d("✅ Returning jobs for location '$location' from cache")
            emit(Result.success(cachedResults))
            return@flow
        }
        
        try {
            val result = firestoreService.getJobsByLocation(location, limit)
            result.fold(
                onSuccess = { jobsData ->
                    // Use shared extension function for conversion
                    val jobListings = jobsData.map { it.toJobListing() }
                    // Cache the results
                    cacheManager.cacheJobsByLocation(location, jobListings)
                    emit(Result.success(jobListings))
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Force refresh jobs - clears cache and fetches fresh data
     */
    fun refreshJobs(limit: Long = 50L): Flow<Result<List<JobListing>>> = flow {
        // Clear cache first
        cacheManager.clearJobsCache()
        
        // Fetch fresh data
        try {
            val result = firestoreService.getAllJobs(limit, null)
            result.fold(
                onSuccess = { jobsData ->
                    // Use shared extension function for conversion
                    val jobListings = jobsData.map { it.toJobListing() }
                    val savedJobIds = getSavedJobIdsWithCache()
                    val updatedJobListings = jobListings.map { job ->
                        job.copy(isSaved = savedJobIds.contains(job.id))
                    }
                    cacheManager.cacheAllJobs(updatedJobListings)
                    emit(Result.success(updatedJobListings))
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    // ==================== SUMMARY/LAZY LOADING METHODS ====================
    
    /**
     * PERFORMANCE OPTIMIZATION: Get job summaries for list views
     * Fetches only essential fields needed for job cards (~70% less data)
     * Full job details are fetched on-demand via getJobById()
     * 
     * Uses caching for repeated requests within TTL window
     */
    fun getAllJobsSummary(limit: Long = 50L, lastCreatedAt: Long? = null): Flow<Result<List<JobListingSummary>>> = flow {
        // Check cache first (only for initial load, not pagination)
        if (lastCreatedAt == null) {
            val cachedSummaries = cacheManager.getAllJobSummariesCached()
            if (cachedSummaries != null) {
                Timber.d("✅ Returning ${cachedSummaries.size} job summaries from cache")
                emit(Result.success(cachedSummaries))
                return@flow
            }
        }
        
        try {
            val result = firestoreService.getAllJobsSummary(limit, lastCreatedAt)
            result.fold(
                onSuccess = { jobsData ->
                    val summaries = jobsData.map { JobListingSummary.fromMap(it) }
                    
                    // Get saved job IDs (use cache if available)
                    val savedJobIds = getSavedJobIdsWithCache()
                    
                    // Get applied job IDs (use cache if available)
                    val appliedJobIds = getAppliedJobIdsWithCache()
                    
                    // Update saved status
                    val updatedSummaries = summaries.map { summary ->
                        summary.copy(isSaved = savedJobIds.contains(summary.id))
                    }
                    
                    // Cache the results (only for initial load)
                    if (lastCreatedAt == null) {
                        cacheManager.cacheAllJobSummaries(updatedSummaries)
                    }
                    
                    Timber.d("📦 Repository: Loaded ${updatedSummaries.size} job summaries")
                    emit(Result.success(updatedSummaries))
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get applied job IDs with caching
     * Fetches from applications collection where current user has applied
     */
    private suspend fun getAppliedJobIdsWithCache(): Set<String> {
        val currentUser = auth.currentUser ?: return emptySet()
        
        // Check cache first
        val cachedIds = cacheManager.getAppliedJobIdsCached()
        if (cachedIds != null) {
            return cachedIds
        }
        
        // Fetch from Firestore - get applications where workerId matches current user
        return try {
            val applicationsResult = firestoreService.getApplicationsByWorker(currentUser.uid)
            applicationsResult.fold(
                onSuccess = { applications ->
                    val appliedJobIds = applications.mapNotNull { it["jobId"] as? String }.toSet()
                    cacheManager.cacheAppliedJobIds(appliedJobIds)
                    appliedJobIds
                },
                onFailure = { 
                    Timber.e(it, "Failed to fetch applied job IDs")
                    emptySet() 
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error fetching applied job IDs")
            emptySet()
        }
    }
    
    /**
     * Calculate distance for a job summary based on user's location
     */
    fun calculateSummaryDistance(summary: JobListingSummary, userLat: Double, userLon: Double): JobListingSummary {
        if (summary.latitude == 0.0 && summary.longitude == 0.0) return summary
        if (userLat == 0.0 && userLon == 0.0) return summary
        
        val distance = calculateDistance(userLat, userLon, summary.latitude, summary.longitude)
        return summary.copy(distance = distance)
    }
    
    /**
     * Calculate distances for a list of job summaries
     */
    fun calculateSummaryDistances(summaries: List<JobListingSummary>, userLat: Double, userLon: Double): List<JobListingSummary> {
        return summaries.map { summary -> calculateSummaryDistance(summary, userLat, userLon) }
    }
    
    /**
     * Update saved status in summary cache when user saves/unsaves a job
     */
    suspend fun updateSummarySavedStatus(jobId: String, isSaved: Boolean) {
        cacheManager.updateSummarySavedStatus(jobId, isSaved)
        if (isSaved) {
            cacheManager.addSavedJobId(jobId)
        } else {
            cacheManager.removeSavedJobId(jobId)
        }
    }
    
    /**
     * Refresh job summaries - clears cache and fetches fresh data
     */
    fun refreshJobSummaries(limit: Long = 50L): Flow<Result<List<JobListingSummary>>> = flow {
        // Clear cache first
        cacheManager.clearJobsCache()
        
        // Fetch fresh data
        try {
            val result = firestoreService.getAllJobsSummary(limit, null)
            result.fold(
                onSuccess = { jobsData ->
                    val summaries = jobsData.map { JobListingSummary.fromMap(it) }
                    val savedJobIds = getSavedJobIdsWithCache()
                    val updatedSummaries = summaries.map { summary ->
                        summary.copy(isSaved = savedJobIds.contains(summary.id))
                    }
                    cacheManager.cacheAllJobSummaries(updatedSummaries)
                    emit(Result.success(updatedSummaries))
                },
                onFailure = { exception ->
                    emit(Result.failure(exception))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Calculate distance for a job based on user's location
     * Uses Haversine formula for accurate earth-surface distance
     */
    fun calculateJobDistance(job: JobListing, userLat: Double, userLon: Double): JobListing {
        if (job.latitude == 0.0 && job.longitude == 0.0) {
            // Job doesn't have coordinates, return as is
            Timber.d("📍 Repository: Job '${job.title}' has no coordinates (0,0)")
            return job
        }
        if (userLat == 0.0 && userLon == 0.0) {
            // User doesn't have coordinates, return as is
            Timber.d("📍 Repository: User has no coordinates (0,0)")
            return job
        }
        val distance = calculateDistance(userLat, userLon, job.latitude, job.longitude)
        Timber.d("📍 Repository: Distance calculated for '${job.title}': ${String.format("%.2f", distance)}km (user: $userLat,$userLon -> job: ${job.latitude},${job.longitude})")
        return job.copy(distance = distance)
    }
    
    /**
     * Calculate distances for a list of jobs based on user's location
     */
    fun calculateJobsDistances(jobs: List<JobListing>, userLat: Double, userLon: Double): List<JobListing> {
        Timber.d("📍 Repository: Calculating distances for ${jobs.size} jobs from user location ($userLat, $userLon)")
        return jobs.map { job -> calculateJobDistance(job, userLat, userLon) }
    }
}
