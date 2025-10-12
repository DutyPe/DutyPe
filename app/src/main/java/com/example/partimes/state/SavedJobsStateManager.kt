package com.example.partimes.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized state manager for saved jobs
 * This follows the industry pattern of having a single source of truth
 * for saved jobs state across the entire application
 */
@Singleton
class SavedJobsStateManager @Inject constructor() {
    
    // Single source of truth for saved job IDs
    private val _savedJobIds = MutableStateFlow<Set<String>>(emptySet())
    val savedJobIds: StateFlow<Set<String>> = _savedJobIds.asStateFlow()
    
    // Trigger for refreshing job data
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()
    
    /**
     * Add a job to saved jobs
     */
    fun addSavedJob(jobId: String) {
        val currentIds = _savedJobIds.value
        _savedJobIds.value = currentIds + jobId
        triggerRefresh()
    }
    
    /**
     * Remove a job from saved jobs
     */
    fun removeSavedJob(jobId: String) {
        val currentIds = _savedJobIds.value
        _savedJobIds.value = currentIds - jobId
        triggerRefresh()
    }
    
    /**
     * Set the complete list of saved job IDs
     */
    fun setSavedJobIds(jobIds: Set<String>) {
        _savedJobIds.value = jobIds
        triggerRefresh()
    }
    
    /**
     * Check if a job is saved
     */
    fun isJobSaved(jobId: String): Boolean {
        return _savedJobIds.value.contains(jobId)
    }
    
    /**
     * Trigger a refresh of job data across the app
     */
    private fun triggerRefresh() {
        _refreshTrigger.value = System.currentTimeMillis()
    }
    
    /**
     * Clear all saved jobs (for logout, etc.)
     */
    fun clearSavedJobs() {
        _savedJobIds.value = emptySet()
        triggerRefresh()
    }
}

