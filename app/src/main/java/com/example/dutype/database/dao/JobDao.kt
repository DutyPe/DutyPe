package com.example.dutype.database.dao

import androidx.room.*
import com.example.dutype.database.entity.JobEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Jobs - Enhanced for Offline-First Architecture
 * 
 * PERFORMANCE OPTIMIZATION (January 2026):
 * - Added pagination support for large datasets
 * - Added distance-based queries for location features
 * - Added sync status tracking for offline queue
 * - Added cache invalidation helpers
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
@Dao
interface JobDao {
    
    // ==================== INSERT ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobEntity>)
    
    /**
     * Upsert job - insert or update if exists
     */
    @Transaction
    suspend fun upsertJob(job: JobEntity) {
        val existing = getJobById(job.jobId)
        if (existing != null) {
            updateJob(job.copy(cachedAt = System.currentTimeMillis()))
        } else {
            insertJob(job)
        }
    }
    
    /**
     * Upsert multiple jobs efficiently
     */
    @Transaction
    suspend fun upsertJobs(jobs: List<JobEntity>) {
        insertJobs(jobs.map { it.copy(cachedAt = System.currentTimeMillis()) })
    }
    
    // ==================== QUERY - BASIC ====================
    
    @Query("SELECT * FROM jobs WHERE isActive = 1 ORDER BY postedAt DESC")
    fun getAllActiveJobs(): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE isActive = 1 ORDER BY postedAt DESC LIMIT :limit")
    fun getActiveJobsWithLimit(limit: Int): Flow<List<JobEntity>>
    
    /**
     * OFFLINE-FIRST: Get active jobs with pagination
     * Uses cursor-based pagination for better performance
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE isActive = 1 AND postedAt < :lastPostedAt 
        ORDER BY postedAt DESC 
        LIMIT :limit
    """)
    suspend fun getActiveJobsPaginated(limit: Int, lastPostedAt: Long): List<JobEntity>
    
    /**
     * Get first page of active jobs
     */
    @Query("SELECT * FROM jobs WHERE isActive = 1 ORDER BY postedAt DESC LIMIT :limit")
    suspend fun getActiveJobsFirstPage(limit: Int): List<JobEntity>
    
    @Query("SELECT * FROM jobs WHERE jobId = :jobId")
    suspend fun getJobById(jobId: String): JobEntity?
    
    @Query("SELECT * FROM jobs WHERE jobId = :jobId")
    fun getJobByIdFlow(jobId: String): Flow<JobEntity?>
    
    @Query("SELECT * FROM jobs WHERE employerId = :employerId ORDER BY postedAt DESC")
    fun getJobsByEmployer(employerId: String): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE category = :category AND isActive = 1 ORDER BY postedAt DESC")
    fun getJobsByCategory(category: String): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE category = :category AND isActive = 1 ORDER BY postedAt DESC LIMIT :limit")
    suspend fun getJobsByCategoryWithLimit(category: String, limit: Int): List<JobEntity>
    
    @Query("SELECT * FROM jobs WHERE location LIKE '%' || :location || '%' AND isActive = 1 ORDER BY postedAt DESC")
    fun getJobsByLocation(location: String): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE title LIKE '%' || :query || '%' OR companyName LIKE '%' || :query || '%' AND isActive = 1 ORDER BY postedAt DESC")
    fun searchJobs(query: String): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE (title LIKE '%' || :query || '%' OR companyName LIKE '%' || :query || '%') AND isActive = 1 ORDER BY postedAt DESC LIMIT :limit")
    suspend fun searchJobsWithLimit(query: String, limit: Int): List<JobEntity>
    
    // ==================== QUERY - LOCATION BASED ====================
    
    /**
     * OFFLINE-FIRST: Get jobs within approximate distance
     * Uses bounding box for efficient filtering (actual distance calculated in memory)
     * 
     * @param minLat Minimum latitude of bounding box
     * @param maxLat Maximum latitude of bounding box
     * @param minLon Minimum longitude of bounding box
     * @param maxLon Maximum longitude of bounding box
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE isActive = 1 
        AND latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLon AND :maxLon
        ORDER BY postedAt DESC 
        LIMIT :limit
    """)
    suspend fun getJobsInBoundingBox(
        minLat: Double, 
        maxLat: Double, 
        minLon: Double, 
        maxLon: Double,
        limit: Int
    ): List<JobEntity>
    
    /**
     * Get jobs near a location (for offline map view)
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE isActive = 1 
        AND latitude != 0.0 
        AND longitude != 0.0
        ORDER BY postedAt DESC 
        LIMIT :limit
    """)
    suspend fun getJobsWithCoordinates(limit: Int): List<JobEntity>
    
    // ==================== QUERY - COUNTS ====================
    
    @Query("SELECT COUNT(*) FROM jobs WHERE isActive = 1")
    suspend fun getActiveJobCount(): Int
    
    @Query("SELECT COUNT(*) FROM jobs WHERE category = :category AND isActive = 1")
    suspend fun getJobCountByCategory(category: String): Int
    
    @Query("SELECT COUNT(*) FROM jobs")
    suspend fun getTotalJobCount(): Int
    
    // ==================== QUERY - SYNC STATUS ====================
    
    @Query("SELECT * FROM jobs WHERE isSynced = 0")
    suspend fun getUnsyncedJobs(): List<JobEntity>
    
    @Query("SELECT COUNT(*) FROM jobs WHERE isSynced = 0")
    suspend fun getUnsyncedJobCount(): Int
    
    @Query("SELECT * FROM jobs WHERE isSynced = 0 LIMIT :limit")
    suspend fun getUnsyncedJobsWithLimit(limit: Int): List<JobEntity>
    
    // ==================== UPDATE ====================
    
    @Update
    suspend fun updateJob(job: JobEntity)
    
    @Query("UPDATE jobs SET isSynced = :synced WHERE jobId = :jobId")
    suspend fun updateSyncStatus(jobId: String, synced: Boolean)
    
    @Query("UPDATE jobs SET isSynced = 1 WHERE jobId IN (:jobIds)")
    suspend fun markJobsAsSynced(jobIds: List<String>)
    
    @Query("UPDATE jobs SET applicationCount = :count WHERE jobId = :jobId")
    suspend fun updateApplicationCount(jobId: String, count: Int)
    
    @Query("UPDATE jobs SET isFilled = :filled WHERE jobId = :jobId")
    suspend fun updateFilledStatus(jobId: String, filled: Boolean)
    
    @Query("UPDATE jobs SET isActive = :active WHERE jobId = :jobId")
    suspend fun updateActiveStatus(jobId: String, active: Boolean)
    
    // ==================== DELETE ====================
    
    @Delete
    suspend fun deleteJob(job: JobEntity)
    
    @Query("DELETE FROM jobs WHERE jobId = :jobId")
    suspend fun deleteJobById(jobId: String)
    
    /**
     * Delete jobs older than specified timestamp (cache cleanup)
     */
    @Query("DELETE FROM jobs WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
    
    /**
     * Delete expired jobs
     * REMOVED: expiresAt column - expiry is now calculated from postedAt + expiryDays
     * This query is no longer needed as expiry is calculated on-demand
     */
    // @Query("DELETE FROM jobs WHERE expiresAt > 0 AND expiresAt < :currentTime")
    // suspend fun deleteExpiredJobs(currentTime: Long)
    
    @Query("DELETE FROM jobs")
    suspend fun deleteAllJobs()
    
    /**
     * Delete jobs not in the provided list (for sync)
     */
    @Query("DELETE FROM jobs WHERE jobId NOT IN (:jobIds)")
    suspend fun deleteJobsNotIn(jobIds: List<String>)
    
    // ==================== CACHE MANAGEMENT ====================
    
    @Query("SELECT MAX(cachedAt) FROM jobs")
    suspend fun getLastCacheTime(): Long?
    
    @Query("SELECT MIN(cachedAt) FROM jobs")
    suspend fun getOldestCacheTime(): Long?
    
    /**
     * Check if cache is stale (older than TTL)
     */
    suspend fun isCacheStale(ttlMs: Long): Boolean {
        val lastCache = getLastCacheTime() ?: return true
        return System.currentTimeMillis() - lastCache > ttlMs
    }
    
    /**
     * Get cache age in milliseconds
     */
    suspend fun getCacheAgeMs(): Long {
        val lastCache = getLastCacheTime() ?: return Long.MAX_VALUE
        return System.currentTimeMillis() - lastCache
    }
}
