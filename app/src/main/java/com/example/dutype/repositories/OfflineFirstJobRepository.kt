package com.example.dutype.repositories

import com.example.dutype.database.dao.JobDao
import com.example.dutype.database.entity.JobEntity
import com.example.dutype.models.JobListing
import com.example.dutype.performance.PerformanceTracker
import com.example.dutype.services.FirestoreService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OfflineFirstJobRepository - Room-backed repository with network sync
 * 
 * Implements offline-first architecture:
 * 1. Always return cached data first (instant UI)
 * 2. Fetch from network in background
 * 3. Update cache with fresh data
 * 4. Emit updated data to UI
 * 
 * Benefits:
 * - Instant app startup (no network wait)
 * - Works offline
 * - Reduced API calls
 * - Better battery life
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
@Singleton
class OfflineFirstJobRepository @Inject constructor(
    private val jobDao: JobDao,
    private val firestoreService: FirestoreService,
    private val performanceTracker: PerformanceTracker
) {
    
    companion object {
        // Cache TTL: 5 minutes for jobs
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
        
        // Stale-while-revalidate: Return stale data up to 1 hour old
        private const val STALE_THRESHOLD_MS = 60 * 60 * 1000L
        
        // Default page size
        private const val DEFAULT_PAGE_SIZE = 50
    }
    
    // ==========================================
    // OFFLINE-FIRST JOB LOADING
    // ==========================================
    
    /**
     * Get active jobs with offline-first strategy
     * 
     * Strategy:
     * 1. Emit cached data immediately (if available)
     * 2. Check if cache is stale
     * 3. If stale, fetch from network and update cache
     * 4. Emit fresh data
     */
    fun getActiveJobs(limit: Int = DEFAULT_PAGE_SIZE): Flow<Result<List<JobListing>>> = flow {
        val startTime = System.currentTimeMillis()
        
        // Step 1: Try to get from cache first
        val cachedJobs = jobDao.getActiveJobsFirstPage(limit)
        val cacheAge = jobDao.getCacheAgeMs()
        
        if (cachedJobs.isNotEmpty()) {
            Timber.d("📦 OFFLINE-FIRST: Returning ${cachedJobs.size} jobs from cache (age: ${cacheAge}ms)")
            performanceTracker.trackCacheAccess("jobs", isHit = true, lookupTimeMs = System.currentTimeMillis() - startTime)
            
            // Emit cached data immediately
            emit(Result.success(cachedJobs.map { it.toJobListing() }))
            
            // Check if we need to refresh
            if (cacheAge > CACHE_TTL_MS) {
                Timber.d("📦 OFFLINE-FIRST: Cache stale, refreshing in background...")
                refreshJobsFromNetwork(limit)
            }
        } else {
            Timber.d("📦 OFFLINE-FIRST: Cache empty, fetching from network...")
            performanceTracker.trackCacheAccess("jobs", isHit = false)
            
            // No cache, must fetch from network
            val networkResult = fetchJobsFromNetwork(limit)
            emit(networkResult)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get jobs as Flow (reactive updates from Room)
     */
    fun getActiveJobsFlow(limit: Int = DEFAULT_PAGE_SIZE): Flow<List<JobListing>> {
        return jobDao.getActiveJobsWithLimit(limit).map { entities ->
            entities.map { it.toJobListing() }
        }
    }
    
    /**
     * Get jobs with pagination (cursor-based)
     */
    suspend fun getJobsPaginated(
        limit: Int = DEFAULT_PAGE_SIZE,
        lastCreatedAt: Long? = null
    ): Result<List<JobListing>> = withContext(Dispatchers.IO) {
        try {
            val jobs = if (lastCreatedAt != null) {
                jobDao.getActiveJobsPaginated(limit, lastCreatedAt)
            } else {
                jobDao.getActiveJobsFirstPage(limit)
            }
            
            if (jobs.isNotEmpty()) {
                Timber.d("📦 OFFLINE-FIRST: Returning ${jobs.size} paginated jobs from cache")
                return@withContext Result.success(jobs.map { it.toJobListing() })
            }
            
            fetchJobsFromNetwork(limit, lastCreatedAt)
        } catch (e: Exception) {
            Timber.e(e, "📦 OFFLINE-FIRST: Error getting paginated jobs")
            Result.failure(e)
        }
    }
    
    // ==========================================
    // NETWORK OPERATIONS
    // ==========================================
    
    /**
     * Fetch jobs from Firestore and cache them
     */
    private suspend fun fetchJobsFromNetwork(
        limit: Int,
        lastCreatedAt: Long? = null
    ): Result<List<JobListing>> {
        return try {
            val startTime = System.currentTimeMillis()
            
            val result = firestoreService.getAllJobs(limit.toLong(), lastCreatedAt)
            
            result.fold(
                onSuccess = { jobsData ->
                    val jobs = jobsData.map { data ->
                        mapToJobListing(data)
                    }
                    
                    // Cache the jobs
                    val entities = jobs.map { JobEntity.fromJobListing(it) }
                    jobDao.upsertJobs(entities)
                    
                    val duration = System.currentTimeMillis() - startTime
                    performanceTracker.trackApiCall("get_jobs", duration, success = true)
                    Timber.d("📦 OFFLINE-FIRST: Fetched and cached ${jobs.size} jobs in ${duration}ms")
                    
                    Result.success(jobs)
                },
                onFailure = { error ->
                    val duration = System.currentTimeMillis() - startTime
                    performanceTracker.trackApiCall("get_jobs", duration, success = false)
                    Timber.e(error, "📦 OFFLINE-FIRST: Network fetch failed")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "📦 OFFLINE-FIRST: Exception fetching jobs")
            Result.failure(e)
        }
    }
    
    /**
     * Refresh jobs from network (background operation)
     */
    private suspend fun refreshJobsFromNetwork(limit: Int) {
        try {
            val result = firestoreService.getAllJobs(limit.toLong(), null)
            result.onSuccess { jobsData ->
                val jobs = jobsData.map { mapToJobListing(it) }
                val entities = jobs.map { JobEntity.fromJobListing(it) }
                jobDao.upsertJobs(entities)
                Timber.d("📦 OFFLINE-FIRST: Background refresh complete - ${jobs.size} jobs")
            }
        } catch (e: Exception) {
            Timber.w(e, "📦 OFFLINE-FIRST: Background refresh failed")
        }
    }
    
    // ==========================================
    // SINGLE JOB OPERATIONS
    // ==========================================
    
    /**
     * Get job by ID with offline-first strategy
     */
    suspend fun getJobById(jobId: String): Result<JobListing?> = withContext(Dispatchers.IO) {
        try {
            // Bug #8 fix: Use cache ONLY when it actually contains the private
            // detail fields (description). The Room cache may have been written
            // from a list path that read jobmetadata only (no description /
            // contactNumber / vacancies / benefits). Returning that cache makes
            // JobDescriptionScreen render an empty description forever. When
            // description is blank we treat it as a partial entry and fetch +
            // remerge from network so job_details is included.
            val cached = jobDao.getJobById(jobId)
            if (cached != null && cached.description.isNotBlank()) {
                Timber.d("📦 OFFLINE-FIRST: Job $jobId served from cache (full)")
                return@withContext Result.success(cached.toJobListing())
            }
            if (cached != null) {
                Timber.d("📦 OFFLINE-FIRST: cache for $jobId is partial — refetching merged")
            }
            
            // Fetch from network
            val result = firestoreService.getJobById(jobId)
            result.fold(
                onSuccess = { jobData ->
                    if (jobData != null) {
                        val job = mapToJobListing(jobData)
                        jobDao.insertJob(JobEntity.fromJobListing(job))
                        Result.success(job)
                    } else {
                        Result.success(null)
                    }
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get job by ID as Flow (reactive)
     */
    fun getJobByIdFlow(jobId: String): Flow<JobListing?> {
        return jobDao.getJobByIdFlow(jobId).map { it?.toJobListing() }
    }
    
    // ==========================================
    // LOCATION-BASED QUERIES
    // ==========================================
    
    /**
     * Get jobs near a location (for map view)
     * Uses bounding box for efficient database query
     */
    suspend fun getJobsNearLocation(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 10.0,
        limit: Int = 100
    ): Result<List<JobListing>> = withContext(Dispatchers.IO) {
        try {
            // Calculate bounding box (approximate)
            val latDelta = radiusKm / 111.0 // 1 degree ≈ 111 km
            val lonDelta = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(latitude)))
            
            val minLat = latitude - latDelta
            val maxLat = latitude + latDelta
            val minLon = longitude - lonDelta
            val maxLon = longitude + lonDelta
            
            val jobs = jobDao.getJobsInBoundingBox(minLat, maxLat, minLon, maxLon, limit)
            Timber.d("📦 OFFLINE-FIRST: Found ${jobs.size} jobs within ${radiusKm}km")
            
            Result.success(jobs.map { it.toJobListing() })
        } catch (e: Exception) {
            Timber.e(e, "📦 OFFLINE-FIRST: Error getting nearby jobs")
            Result.failure(e)
        }
    }
    
    /**
     * Get jobs with coordinates (for map view)
     */
    suspend fun getJobsWithCoordinates(limit: Int = 100): Result<List<JobListing>> = withContext(Dispatchers.IO) {
        try {
            val jobs = jobDao.getJobsWithCoordinates(limit)
            Result.success(jobs.map { it.toJobListing() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==========================================
    // CATEGORY & SEARCH
    // ==========================================
    
    /**
     * Get jobs by category
     */
    suspend fun getJobsByCategory(category: String, limit: Int = DEFAULT_PAGE_SIZE): Result<List<JobListing>> = withContext(Dispatchers.IO) {
        try {
            val jobs = jobDao.getJobsByCategoryWithLimit(category, limit)
            if (jobs.isNotEmpty()) {
                return@withContext Result.success(jobs.map { it.toJobListing() })
            }
            
            // Fetch from network if cache empty
            val result = firestoreService.getJobsByCategory(category, limit.toLong())
            result.fold(
                onSuccess = { jobsData ->
                    val jobList = jobsData.map { mapToJobListing(it) }
                    val entities = jobList.map { JobEntity.fromJobListing(it) }
                    jobDao.upsertJobs(entities)
                    Result.success(jobList)
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search jobs
     */
    suspend fun searchJobs(query: String, limit: Int = DEFAULT_PAGE_SIZE): Result<List<JobListing>> = withContext(Dispatchers.IO) {
        try {
            val jobs = jobDao.searchJobsWithLimit(query, limit)
            Result.success(jobs.map { it.toJobListing() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==========================================
    // CACHE MANAGEMENT
    // ==========================================
    
    /**
     * Force refresh from network
     */
    suspend fun forceRefresh(limit: Int = DEFAULT_PAGE_SIZE): Result<List<JobListing>> = withContext(Dispatchers.IO) {
        Timber.d("📦 OFFLINE-FIRST: Force refresh requested")
        fetchJobsFromNetwork(limit)
    }
    
    /**
     * Clear old cache entries
     */
    suspend fun clearOldCache(maxAgeMs: Long = STALE_THRESHOLD_MS) = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - maxAgeMs
        jobDao.deleteOldCache(threshold)
        Timber.d("📦 OFFLINE-FIRST: Cleared cache older than ${maxAgeMs}ms")
    }
    
    /**
     * Clear expired jobs
     * REMOVED: deleteExpiredJobs method - expiry is now calculated on-demand
     * Jobs are filtered by expiry in the UI layer using job.isExpired()
     */
    suspend fun clearExpiredJobs() = withContext(Dispatchers.IO) {
        // No-op: Expiry is now calculated on-demand, not stored in database
        // Jobs are filtered by expiry in the UI layer
        Timber.d("📦 OFFLINE-FIRST: clearExpiredJobs is deprecated (expiry calculated on-demand)")
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        CacheStats(
            totalJobs = jobDao.getTotalJobCount(),
            activeJobs = jobDao.getActiveJobCount(),
            cacheAgeMs = jobDao.getCacheAgeMs(),
            isStale = jobDao.isCacheStale(CACHE_TTL_MS)
        )
    }
    
    // ==========================================
    // HELPERS
    // ==========================================
    
    private fun mapToJobListing(data: Map<String, Any>): JobListing {
        val locationMap = data["location"] as? Map<*, *>
        val lat = (locationMap?.get("lat") as? Number)?.toDouble()
            ?: (data["lat"] as? Number)?.toDouble() ?: 0.0
        val lng = (locationMap?.get("lng") as? Number)?.toDouble()
            ?: (data["lng"] as? Number)?.toDouble() ?: 0.0
        return JobListing(
            id = data["jobId"] as? String ?: data["id"] as? String ?: "",
            employerId = data["employerId"] as? String ?: "",
            title = data["title"] as? String ?: "",
            jobType = data["jobType"] as? String ?: "",
            salary = (data["salary"] as? String)?.trim()
                ?: ((data["salary"] as? Number)?.let {
                    val d = it.toDouble()
                    if (d <= 0.0) "" else if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
                } ?: ""),
            salaryType = data["salaryType"] as? String ?: "",
            lat = lat,
            lng = lng,
            geohash = data["geohash"] as? String ?: "",
            urgency = data["urgency"] as? String ?: "",
            status = data["status"] as? String ?: "open",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L,
            description = data["description"] as? String ?: "",
            contactNumber = data["contactNumber"] as? String ?: "",
            addressText = data["addressText"] as? String ?: ""
        )
    }
    
    /**
     * Cache statistics data class
     */
    data class CacheStats(
        val totalJobs: Int,
        val activeJobs: Int,
        val cacheAgeMs: Long,
        val isStale: Boolean
    )
}
