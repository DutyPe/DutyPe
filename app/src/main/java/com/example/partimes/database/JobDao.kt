package com.example.partimes.database

import androidx.room.*
import com.example.partimes.models.JobListing
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM joblisting")
    fun getAllJobs(): Flow<List<JobListing>>

    @Query("SELECT * FROM joblisting WHERE jobId = :jobId")
    suspend fun getJobById(jobId: String): JobListing?

    @Query("SELECT * FROM joblisting WHERE isActive = 1")
    fun getActiveJobs(): Flow<List<JobListing>>

    @Query("SELECT * FROM joblisting WHERE isTrending = 1")
    fun getTrendingJobs(): Flow<List<JobListing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobListing)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobListing>)

    @Update
    suspend fun updateJob(job: JobListing)

    @Delete
    suspend fun deleteJob(job: JobListing)

    @Query("DELETE FROM joblisting")
    suspend fun deleteAllJobs()
}
