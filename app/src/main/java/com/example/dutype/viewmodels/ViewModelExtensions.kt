package com.example.dutype.viewmodels

import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.repositories.FirestoreJobRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * P2 FIX: ViewModel Extension Functions
 * 
 * Eliminates duplicate distance calculation code across ViewModels.
 * Provides reusable helper methods for common ViewModel operations.
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.2
 */

/**
 * Calculate distances for jobs if user location is available
 * 
 * P2 FIX: Extracted from duplicate code in:
 * - AllJobsViewModel
 * - FirestoreJobViewModel
 * - WorkerHomeViewModel
 * - CategoriesViewModel
 * 
 * @param jobs List of jobs to calculate distances for
 * @param userLatitude User's latitude (0.0 if not available)
 * @param userLongitude User's longitude (0.0 if not available)
 * @param repository Repository to use for calculation
 * @return Jobs with calculated distances
 */
suspend fun calculateDistancesIfAvailable(
    jobs: List<JobListing>,
    userLatitude: Double,
    userLongitude: Double,
    repository: FirestoreJobRepository
): List<JobListing> {
    return if (userLatitude != 0.0 || userLongitude != 0.0) {
        withContext(Dispatchers.Default) {
            repository.calculateJobsDistances(jobs, userLatitude, userLongitude)
        }
    } else {
        jobs
    }
}

/**
 * Calculate distances for job summaries if user location is available
 * 
 * @param summaries List of job summaries to calculate distances for
 * @param userLatitude User's latitude (0.0 if not available)
 * @param userLongitude User's longitude (0.0 if not available)
 * @param repository Repository to use for calculation
 * @return Summaries with calculated distances
 */
suspend fun calculateSummaryDistancesIfAvailable(
    summaries: List<JobListingSummary>,
    userLatitude: Double,
    userLongitude: Double,
    repository: FirestoreJobRepository
): List<JobListingSummary> {
    return if (userLatitude != 0.0 || userLongitude != 0.0) {
        withContext(Dispatchers.Default) {
            repository.calculateSummaryDistances(summaries, userLatitude, userLongitude)
        }
    } else {
        summaries
    }
}

/**
 * Check if user location is available
 * 
 * @param latitude User's latitude
 * @param longitude User's longitude
 * @return true if location is available (not 0,0)
 */
fun hasUserLocation(latitude: Double, longitude: Double): Boolean {
    return latitude != 0.0 || longitude != 0.0
}
