package com.example.partimes.repository

import com.example.partimes.apis.RetrofitClient
import com.example.partimes.models.JobListing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository class for handling job-related data operations
 * Acts as a single source of truth for the app's data
 */
class JobRepository {

    // In-memory cache for jobs
    private var cachedJobs: List<JobListing> = emptyList()

    /**
     * Get all jobs, either from cache or from the API
     * @param callback Callback that will be called with the jobs
     */
    suspend fun getJobs(callback: (List<JobListing>) -> Unit) {
        // If we have cached jobs, return them immediately
        if (cachedJobs.isNotEmpty()) {
            callback(cachedJobs)

            // Then fetch fresh data in the background
            refreshJobs(callback)
        } else {
            // If no cached data, fetch from the API
            refreshJobs(callback)
        }
    }

    /**
     * Force refresh jobs from the API
     * @param callback Callback that will be called with the fresh jobs
     */
    private suspend fun refreshJobs(callback: (List<JobListing>) -> Unit) {
        try {
            withContext(Dispatchers.IO) {
                val jobs = RetrofitClient.apiService.getAllJobs()
                cachedJobs = jobs
                withContext(Dispatchers.Main) {
                    callback(jobs)
                }
            }
        } catch (e: Exception) {
            // If API call fails, use existing cache
            if (cachedJobs.isNotEmpty()) {
                callback(cachedJobs)
            } else {
                // If no cached data, propagate the error
                throw e
            }
        }
    }

    /**
     * Get a specific job by ID
     * @param jobId ID of the job to retrieve
     * @return The job or null if not found
     */
    fun getJobById(jobId: String): JobListing? {
        return cachedJobs.find { it.jobId == jobId }
    }

    /**
     * Filter jobs by timing type
     * @param timingType Timing type to filter by (e.g., "Hourly", "Daily", "Full-time")
     * @return List of jobs with the specified timing type
     */
    fun getJobsByTimingType(timingType: String): List<JobListing> {
        return cachedJobs.filter {
            it.timing.contains(timingType, ignoreCase = true)
        }
    }

    /**
     * Filter jobs by location
     * @param location Location to filter by
     * @return List of jobs in the specified location
     */
    fun getJobsByLocation(location: String): List<JobListing> {
        return cachedJobs.filter {
            it.locationNearby.contains(location, ignoreCase = true) ||
                    it.specificLocation.contains(location, ignoreCase = true)
        }
    }

    /**
     * Clear the job cache
     */
    fun clearCache() {
        cachedJobs = emptyList()
    }
}