package com.example.dutype.database.dao

import androidx.room.*
import com.example.dutype.database.entity.SavedJobEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Saved Jobs
 */
@Dao
interface SavedJobDao {

    // ==================== INSERT ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedJob(savedJob: SavedJobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedJobs(savedJobs: List<SavedJobEntity>)

    // ==================== QUERY ====================

    @Query("SELECT * FROM saved_jobs WHERE workerId = :workerId ORDER BY savedAt DESC")
    fun getSavedJobsByWorker(workerId: String): Flow<List<SavedJobEntity>>

    @Query("SELECT jobId FROM saved_jobs WHERE workerId = :workerId")
    suspend fun getSavedJobIds(workerId: String): List<String>

    @Query("SELECT jobId FROM saved_jobs WHERE workerId = :workerId")
    fun getSavedJobIdsFlow(workerId: String): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_jobs WHERE workerId = :workerId AND jobId = :jobId)")
    suspend fun isJobSaved(workerId: String, jobId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM saved_jobs WHERE workerId = :workerId AND jobId = :jobId)")
    fun isJobSavedFlow(workerId: String, jobId: String): Flow<Boolean>

    @Query("SELECT * FROM saved_jobs WHERE isSynced = 0")
    suspend fun getUnsyncedSavedJobs(): List<SavedJobEntity>

    @Query("SELECT * FROM saved_jobs WHERE pendingAction IS NOT NULL")
    suspend fun getPendingActions(): List<SavedJobEntity>

    // ==================== UPDATE ====================

    @Update
    suspend fun updateSavedJob(savedJob: SavedJobEntity)

    @Query("UPDATE saved_jobs SET isSynced = :synced, pendingAction = NULL WHERE id = :id")
    suspend fun updateSyncStatus(id: String, synced: Boolean)

    // ==================== DELETE ====================

    @Delete
    suspend fun deleteSavedJob(savedJob: SavedJobEntity)

    @Query("DELETE FROM saved_jobs WHERE workerId = :workerId AND jobId = :jobId")
    suspend fun deleteSavedJobByIds(workerId: String, jobId: String)

    @Query("DELETE FROM saved_jobs WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)

    @Query("DELETE FROM saved_jobs WHERE workerId = :workerId")
    suspend fun deleteSavedJobsByWorker(workerId: String)

    @Query("DELETE FROM saved_jobs")
    suspend fun deleteAllSavedJobs()

    // ==================== STATS ====================

    @Query("SELECT COUNT(*) FROM saved_jobs WHERE workerId = :workerId")
    suspend fun getSavedJobCount(workerId: String): Int
}
