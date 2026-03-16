package com.example.dutype.state

import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enterprise-level Application State Manager
 * Manages application state across the entire app
 */
@Singleton
class ApplicationStateManager @Inject constructor() {
    
    // Applied jobs tracking
    private val _appliedJobIds = MutableStateFlow<Set<String>>(emptySet())
    val appliedJobIds: StateFlow<Set<String>> = _appliedJobIds.asStateFlow()
    
    // Application details tracking
    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    val applications: StateFlow<List<JobApplication>> = _applications.asStateFlow()
    
    // Application status tracking
    private val _applicationStatuses = MutableStateFlow<Map<String, ApplicationStatus>>(emptyMap())
    val applicationStatuses: StateFlow<Map<String, ApplicationStatus>> = _applicationStatuses.asStateFlow()
    
    // Refresh triggers
    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger: StateFlow<Int> = _refreshTrigger.asStateFlow()
    
    /**
     * Add applied job
     */
    fun addAppliedJob(jobId: String) {
        _appliedJobIds.update { it + jobId }
        triggerRefresh()
    }
    
    /**
     * Remove applied job
     */
    fun removeAppliedJob(jobId: String) {
        _appliedJobIds.update { it - jobId }
        triggerRefresh()
    }
    
    /**
     * Set applied jobs
     */
    fun setAppliedJobs(jobIds: Set<String>) {
        _appliedJobIds.value = jobIds
        triggerRefresh()
    }
    
    /**
     * Check if job is applied
     */
    fun isJobApplied(jobId: String): Boolean {
        return _appliedJobIds.value.contains(jobId)
    }
    
    /**
     * Update applications list
     */
    fun updateApplications(applications: List<JobApplication>) {
        _applications.value = applications
        
        // Update applied job IDs using jobId (exclude rejected and withdrawn)
        val appliedIds = applications
            .filter { it.status != ApplicationStatus.REJECTED && it.status != ApplicationStatus.WITHDRAWN }
            .map { it.jobId }
            .toSet()
        _appliedJobIds.value = appliedIds
        
        // Update application statuses keyed by jobId
        val statusMap = applications.associate { it.jobId to it.status }
        _applicationStatuses.value = statusMap
        
        triggerRefresh()
    }
    
    /**
     * Get application by job ID
     */
    fun getApplicationByJobId(jobId: String): JobApplication? {
        return _applications.value.find { it.jobId == jobId }
    }
    
    /**
     * Get application status by job ID
     */
    fun getApplicationStatus(jobId: String): ApplicationStatus? {
        return _applicationStatuses.value[jobId]
    }
    
    /**
     * Update application status
     */
    fun updateApplicationStatus(jobId: String, status: ApplicationStatus) {
        _applicationStatuses.update { it + (jobId to status) }
        
        // Update the application in the list
        _applications.update { applications ->
            applications.map { app ->
                if (app.jobId == jobId) {
                    app.copy(status = status)
                } else {
                    app
                }
            }
        }
        
        triggerRefresh()
    }
    
    /**
     * Clear all data
     */
    fun clearAll() {
        _appliedJobIds.value = emptySet()
        _applications.value = emptyList()
        _applicationStatuses.value = emptyMap()
        triggerRefresh()
    }
    
    /**
     * Trigger refresh
     */
    private fun triggerRefresh() {
        _refreshTrigger.update { it + 1 }
    }
}
