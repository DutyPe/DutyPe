package com.example.dutype.database.dao

import androidx.room.*
import com.example.dutype.database.entity.ApplicationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Job Applications - Offline-First Architecture
 * 
 * Enables workers to:
 * - View their applications offline
 * - Queue new applications when offline
 * - Track application status changes
 * 
 * @author DutyPe Engineering Team
 * @since 2.2.0
 */
@Dao
interface ApplicationDao {
    
    // ==================== INSERT ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: ApplicationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(applications: List<ApplicationEntity>)
    
    /**
     * Queue an application for offline submission
     */
    @Transaction
    suspend fun queueApplication(application: ApplicationEntity) {
        insertApplication(application.copy(
            isSynced = false,
            isPendingSubmission = true,
            cachedAt = System.currentTimeMillis()
        ))
    }
    
    // ==================== QUERY - WORKER ====================
    
    /**
     * Get all applications for a worker (for My Jobs screen)
     */
    @Query("SELECT * FROM applications WHERE workerId = :workerId ORDER BY appliedAt DESC")
    fun getApplicationsByWorker(workerId: String): Flow<List<ApplicationEntity>>
    
    /**
     * Get applications by worker with limit
     */
    @Query("SELECT * FROM applications WHERE workerId = :workerId ORDER BY appliedAt DESC LIMIT :limit")
    suspend fun getApplicationsByWorkerWithLimit(workerId: String, limit: Int): List<ApplicationEntity>
    
    /**
     * Get active applications (not withdrawn/rejected)
     */
    @Query("""
        SELECT * FROM applications 
        WHERE workerId = :workerId 
        AND status NOT IN ('WITHDRAWN', 'REJECTED')
        ORDER BY appliedAt DESC
    """)
    fun getActiveApplicationsByWorker(workerId: String): Flow<List<ApplicationEntity>>
    
    /**
     * Check if worker has applied to a job
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM applications 
        WHERE workerId = :workerId 
        AND jobId = :jobId 
        AND status NOT IN ('WITHDRAWN', 'REJECTED')
    """)
    suspend fun hasWorkerApplied(workerId: String, jobId: String): Boolean
    
    /**
     * Get all job IDs that worker has applied to
     */
    @Query("""
        SELECT jobId FROM applications 
        WHERE workerId = :workerId 
        AND status NOT IN ('WITHDRAWN', 'REJECTED')
    """)
    suspend fun getAppliedJobIds(workerId: String): List<String>
    
    /**
     * Get applied job IDs as Flow for reactive updates
     */
    @Query("""
        SELECT jobId FROM applications 
        WHERE workerId = :workerId 
        AND status NOT IN ('WITHDRAWN', 'REJECTED')
    """)
    fun getAppliedJobIdsFlow(workerId: String): Flow<List<String>>
    
    // ==================== QUERY - EMPLOYER ====================
    
    /**
     * Get all applications for an employer's jobs
     */
    @Query("SELECT * FROM applications WHERE employerId = :employerId ORDER BY appliedAt DESC")
    fun getApplicationsByEmployer(employerId: String): Flow<List<ApplicationEntity>>
    
    /**
     * Get applications for a specific job
     */
    @Query("SELECT * FROM applications WHERE jobId = :jobId ORDER BY appliedAt DESC")
    fun getApplicationsByJob(jobId: String): Flow<List<ApplicationEntity>>
    
    // ==================== QUERY - SINGLE ====================
    
    @Query("SELECT * FROM applications WHERE applicationId = :applicationId")
    suspend fun getApplicationById(applicationId: String): ApplicationEntity?
    
    // ==================== QUERY - STATUS ====================
    
    /**
     * Get applications by status
     */
    @Query("SELECT * FROM applications WHERE workerId = :workerId AND status = :status ORDER BY appliedAt DESC")
    fun getApplicationsByStatus(workerId: String, status: String): Flow<List<ApplicationEntity>>
    
    // ==================== QUERY - SYNC ====================
    
    /**
     * Get applications pending submission (queued offline)
     */
    @Query("SELECT * FROM applications WHERE isPendingSubmission = 1")
    suspend fun getPendingSubmissions(): List<ApplicationEntity>
    
    /**
     * Get unsynced applications
     */
    @Query("SELECT * FROM applications WHERE isSynced = 0")
    suspend fun getUnsyncedApplications(): List<ApplicationEntity>
    
    @Query("SELECT COUNT(*) FROM applications WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
    
    // ==================== UPDATE ====================
    
    @Update
    suspend fun updateApplication(application: ApplicationEntity)
    
    @Query("UPDATE applications SET status = :status, updatedAt = :updatedAt WHERE applicationId = :applicationId")
    suspend fun updateStatus(applicationId: String, status: String, updatedAt: Long)
    
    @Query("UPDATE applications SET isSynced = :synced WHERE applicationId = :applicationId")
    suspend fun updateSyncStatus(applicationId: String, synced: Boolean)
    
    @Query("UPDATE applications SET isSynced = 1, isPendingSubmission = 0 WHERE applicationId = :applicationId")
    suspend fun markAsSynced(applicationId: String)
    
    @Query("UPDATE applications SET isSynced = 1 WHERE applicationId IN (:applicationIds)")
    suspend fun markApplicationsAsSynced(applicationIds: List<String>)
    
    // ==================== DELETE ====================
    
    @Delete
    suspend fun deleteApplication(application: ApplicationEntity)
    
    @Query("DELETE FROM applications WHERE applicationId = :applicationId")
    suspend fun deleteApplicationById(applicationId: String)
    
    @Query("DELETE FROM applications WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
    
    @Query("DELETE FROM applications")
    suspend fun deleteAllApplications()
    
    // ==================== CACHE MANAGEMENT ====================
    
    @Query("SELECT MAX(cachedAt) FROM applications")
    suspend fun getLastCacheTime(): Long?
    
    @Query("SELECT COUNT(*) FROM applications")
    suspend fun getTotalCount(): Int
}
