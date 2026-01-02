package com.example.dutype.database.dao

import androidx.room.*
import com.example.dutype.database.entity.JobEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Jobs
 */
@Dao
interface JobDao {
    
    // ==================== INSERT ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobEntity>)
    
    // ==================== QUERY ====================
    
    @Query("SELECT * FROM jobs WHERE isActive = 1 ORDER BY postedAt DESC")
    fun getAllActiveJobs(): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE isActive = 1 ORDER BY postedAt DESC LIMIT :limit")
    fun getActiveJobsWithLimit(limit: Int): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE jobId = :jobId")
    suspend fun getJobById(jobId: String): JobEntity?
    
    @Query("SELECT * FROM jobs WHERE jobId = :jobId")
    fun getJobByIdFlow(jobId: String): Flow<JobEntity?>
    
    @Query("SELECT * FROM jobs WHERE employerId = :employerId ORDER BY postedAt DESC")
    fun getJobsByEmployer(employerId: String): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE category = :category AND isActive = 1 ORDER BY postedAt DESC")
    fun getJobsByCategory(category: String): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE location LIKE '%' || :location || '%' AND isActive = 1 ORDER BY postedAt DESC")
    fun getJobsByLocation(location: String): Flow<List<JobEntity>>
    
    @Query("SELECT * FROM jobs WHERE title LIKE '%' || :query || '%' OR companyName LIKE '%' || :query || '%' AND isActive = 1 ORDER BY postedAt DESC")
    fun searchJobs(query: String): Flow<List<JobEntity>>
    
    @Query("SELECT COUNT(*) FROM jobs WHERE isActive = 1")
    suspend fun getActiveJobCount(): Int
    
    @Query("SELECT * FROM jobs WHERE isSynced = 0")
    suspend fun getUnsyncedJobs(): List<JobEntity>
    
    // ==================== UPDATE ====================
    
    @Update
    suspend fun updateJob(job: JobEntity)
    
    @Query("UPDATE jobs SET isSynced = :synced WHERE jobId = :jobId")
    suspend fun updateSyncStatus(jobId: String, synced: Boolean)
    
    @Query("UPDATE jobs SET applicationCount = :count WHERE jobId = :jobId")
    suspend fun updateApplicationCount(jobId: String, count: Int)
    
    // ==================== DELETE ====================
    
    @Delete
    suspend fun deleteJob(job: JobEntity)
    
    @Query("DELETE FROM jobs WHERE jobId = :jobId")
    suspend fun deleteJobById(jobId: String)
    
    @Query("DELETE FROM jobs WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
    
    @Query("DELETE FROM jobs")
    suspend fun deleteAllJobs()
    
    // ==================== CACHE MANAGEMENT ====================
    
    @Query("SELECT MAX(cachedAt) FROM jobs")
    suspend fun getLastCacheTime(): Long?
    
    @Query("SELECT COUNT(*) FROM jobs")
    suspend fun getTotalJobCount(): Int
}
