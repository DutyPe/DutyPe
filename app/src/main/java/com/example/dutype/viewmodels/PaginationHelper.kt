package com.example.dutype.viewmodels

import com.example.dutype.models.JobListing
import timber.log.Timber

/**
 * Shared pagination utilities for job ViewModels.
 * Extracts common append/deduplicate/sliding-window logic used by
 * FirestoreJobViewModel, AllJobsViewModel, and CategoriesViewModel.
 */
object PaginationHelper {

    private const val DEFAULT_MAX_IN_MEMORY = 500

    private fun stableJobKey(job: JobListing): String {
        val primaryId = job.jobId.ifBlank { job.id }
        return if (primaryId.isNotBlank()) {
            primaryId
        } else {
            "${job.employerId}:${job.title.trim().lowercase()}:${job.createdAt}"
        }
    }

    /**
     * Append new jobs to existing list with deduplication and optional sliding window.
     *
     * @param currentJobs existing jobs in state
     * @param newJobs freshly loaded jobs
     * @param maxInMemory sliding-window cap (0 = unlimited)
     * @return combined, deduplicated list
     */
    fun appendJobs(
        currentJobs: List<JobListing>,
        newJobs: List<JobListing>,
        maxInMemory: Int = DEFAULT_MAX_IN_MEMORY
    ): List<JobListing> {
        val existingIds = currentJobs.mapTo(HashSet(currentJobs.size)) { stableJobKey(it) }
        val unique = newJobs.filter { stableJobKey(it) !in existingIds }

        Timber.d("📦 PaginationHelper: ${newJobs.size} loaded, ${unique.size} unique, ${currentJobs.size} existing")

        val combined = currentJobs + unique
        return if (maxInMemory > 0 && combined.size > maxInMemory) {
            Timber.d("📦 Sliding window: keeping last $maxInMemory of ${combined.size}")
            combined.takeLast(maxInMemory)
        } else {
            combined
        }
    }

    /**
     * Determine whether more pages are available.
     * Returns false only when Firestore returned zero documents.
     */
    fun hasMorePages(loadedCount: Int): Boolean = loadedCount > 0
}
