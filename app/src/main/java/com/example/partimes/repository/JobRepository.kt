package com.example.partimes.repository

import com.example.partimes.apis.ApiService
import com.example.partimes.database.JobDao
import com.example.partimes.models.JobListing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for handling job-related data operations
 * Acts as a single source of truth for the app's data
 */
@Singleton
class JobRepository @Inject constructor(
    private val jobDao: JobDao,
    private val apiService: ApiService
) {

    /**
     * Get all jobs from local database
     */
    fun getAllJobs(): Flow<List<JobListing>> = jobDao.getAllJobs()

    /**
     * Get active jobs from local database
     */
    fun getActiveJobs(): Flow<List<JobListing>> = jobDao.getActiveJobs()

    /**
     * Get trending jobs from local database
     */
    fun getTrendingJobs(): Flow<List<JobListing>> = jobDao.getTrendingJobs()

    /**
     * Get a specific job by ID
     * @param jobId ID of the job to retrieve
     * @return The job or null if not found
     */
    suspend fun getJobById(jobId: String): JobListing? {
        return jobDao.getJobById(jobId)
    }

    /**
     * Refresh jobs from API and store in local database
     */
    suspend fun refreshJobs(): Flow<List<JobListing>> = flow {
        try {
            val jobs = withContext(Dispatchers.IO) {
                apiService.getAllJobs()
            }
            jobDao.insertJobs(jobs)
            emit(jobs)
        } catch (e: Exception) {
            // If API call fails, emit cached data
            jobDao.getAllJobs().collect { cachedJobs ->
                emit(cachedJobs)
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Filter jobs by timing type
     * @param timingType Timing type to filter by (e.g., "Hourly", "Daily", "Full-time")
     * @return List of jobs with the specified timing type
     */
    fun getJobsByTimingType(timingType: String): Flow<List<JobListing>> = flow {
        val allJobs = jobDao.getAllJobs()
        allJobs.collect { jobs ->
            val filteredJobs = jobs.filter {
                it.timing.contains(timingType, ignoreCase = true)
            }
            emit(filteredJobs)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Filter jobs by location
     * @param location Location to filter by
     * @return List of jobs in the specified location
     */
    fun getJobsByLocation(location: String): Flow<List<JobListing>> = flow {
        val allJobs = jobDao.getAllJobs()
        allJobs.collect { jobs ->
            val filteredJobs = jobs.filter {
                it.locationNearby.contains(location, ignoreCase = true) ||
                        it.specificLocation.contains(location, ignoreCase = true)
            }
            emit(filteredJobs)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Clear all jobs from local database
     */
    suspend fun clearCache() {
        jobDao.deleteAllJobs()
    }
}