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
        val existingIds = currentJobs.mapTo(HashSet(currentJobs.size)) { it.jobId }
        val unique = newJobs.filter { it.jobId !in existingIds }

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
