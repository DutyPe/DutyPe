package com.example.dutype.database.dao

import androidx.room.*
import com.example.dutype.database.entity.ApplicationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Applications
 */
@Dao
interface ApplicationDao {
    
    // ==================== INSERT ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: ApplicationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(applications: List<ApplicationEntity>)
    
    // ==================== QUERY ====================
    
    @Query("SELECT * FROM applications WHERE workerId = :workerId AND isActive = 1 ORDER BY appliedAt DESC")
    fun getApplicationsByWorker(workerId: String): Flow<List<ApplicationEntity>>
    
    @Query("SELECT * FROM applications WHERE employerId = :employerId AND isActive = 1 ORDER BY appliedAt DESC")
    fun getApplicationsByEmployer(employerId: String): Flow<List<ApplicationEntity>>
    
    @Query("SELECT * FROM applications WHERE jobId = :jobId AND isActive = 1 ORDER BY appliedAt DESC")
    fun getApplicationsByJob(jobId: String): Flow<List<ApplicationEntity>>
    
    @Query("SELECT * FROM applications WHERE applicationId = :applicationId")
    suspend fun getApplicationById(applicationId: String): ApplicationEntity?
    
    @Query("SELECT * FROM applications WHERE workerId = :workerId AND status = :status AND isActive = 1 ORDER BY appliedAt DESC")
    fun getApplicationsByStatus(workerId: String, status: String): Flow<List<ApplicationEntity>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM applications WHERE workerId = :workerId AND jobId = :jobId AND isActive = 1 AND status != 'WITHDRAWN' AND status != 'REJECTED')")
    suspend fun hasAppliedToJob(workerId: String, jobId: String): Boolean
    
    @Query("SELECT jobId FROM applications WHERE workerId = :workerId AND isActive = 1 AND status != 'WITHDRAWN' AND status != 'REJECTED'")
    suspend fun getAppliedJobIds(workerId: String): List<String>
    
    @Query("SELECT * FROM applications WHERE isSynced = 0")
    suspend fun getUnsyncedApplications(): List<ApplicationEntity>
    
    @Query("SELECT * FROM applications WHERE pendingAction IS NOT NULL")
    suspend fun getPendingActions(): List<ApplicationEntity>
    
    // ==================== UPDATE ====================
    
    @Update
    suspend fun updateApplication(application: ApplicationEntity)
    
    @Query("UPDATE applications SET status = :status, updatedAt = :updatedAt, isSynced = 0 WHERE applicationId = :applicationId")
    suspend fun updateStatus(applicationId: String, status: String, updatedAt: Long)
    
    @Query("UPDATE applications SET isSynced = :synced, pendingAction = NULL WHERE applicationId = :applicationId")
    suspend fun updateSyncStatus(applicationId: String, synced: Boolean)
    
    @Query("UPDATE applications SET pendingAction = :action WHERE applicationId = :applicationId")
    suspend fun setPendingAction(applicationId: String, action: String?)
    
    // ==================== DELETE ====================
    
    @Delete
    suspend fun deleteApplication(application: ApplicationEntity)
    
    @Query("DELETE FROM applications WHERE applicationId = :applicationId")
    suspend fun deleteApplicationById(applicationId: String)
    
    @Query("DELETE FROM applications WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
    
    @Query("DELETE FROM applications WHERE workerId = :workerId")
    suspend fun deleteApplicationsByWorker(workerId: String)
    
    @Query("DELETE FROM applications")
    suspend fun deleteAllApplications()
    
    // ==================== STATS ====================
    
    @Query("SELECT COUNT(*) FROM applications WHERE workerId = :workerId AND isActive = 1")
    suspend fun getTotalApplicationCount(workerId: String): Int
    
    @Query("SELECT COUNT(*) FROM applications WHERE workerId = :workerId AND status = :status AND isActive = 1")
    suspend fun getApplicationCountByStatus(workerId: String, status: String): Int
}
