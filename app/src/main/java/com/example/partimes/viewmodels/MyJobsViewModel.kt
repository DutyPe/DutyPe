package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.jobseeker.models.JobApplication
import com.example.partimes.jobseeker.models.ApplicationStatus
import com.example.partimes.repository.ApplicationRepository
import com.example.partimes.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for My Jobs screen
 */
data class MyJobsUiState(
    val applications: List<JobApplication> = emptyList(),
    val savedJobs: List<com.example.partimes.jobseeker.models.JobCardModel> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedTabIndex: Int = 0,
    val searchQuery: String = "",
    val selectedStatusFilter: ApplicationStatus? = null
)

/**
 * ViewModel for managing My Jobs screen state and operations
 */
@HiltViewModel
class MyJobsViewModel @Inject constructor(
    private val applicationRepository: ApplicationRepository,
    private val userRepository: UserRepository,
    private val savedJobsRepository: com.example.partimes.repository.SavedJobsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MyJobsUiState())
    val uiState: StateFlow<MyJobsUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    /**
     * Load applications and saved jobs
     */
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load applications
                applicationRepository.getApplications().collect { applications ->
                    _uiState.value = _uiState.value.copy(
                        applications = applications,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load applications: ${e.message}"
                )
            }
        }
        
        viewModelScope.launch {
            // Load saved jobs
            savedJobsRepository.savedJobs.collect { savedJobs ->
                _uiState.value = _uiState.value.copy(savedJobs = savedJobs)
            }
        }
    }
    
    /**
     * Refresh data
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            try {
                // Refresh applications
                applicationRepository.getApplications().collect { applications ->
                    _uiState.value = _uiState.value.copy(
                        applications = applications,
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Failed to refresh: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update selected tab
     */
    fun updateSelectedTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    /**
     * Update status filter
     */
    fun updateStatusFilter(status: ApplicationStatus?) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
    }
    
    /**
     * Withdraw application
     */
    fun withdrawApplication(applicationId: String) {
        viewModelScope.launch {
            try {
                applicationRepository.withdrawApplication(applicationId)
                    .fold(
                        onSuccess = {
                            // Refresh data to show updated status
                            loadData()
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to withdraw application: ${error.message}"
                            )
                        }
                    )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to withdraw application: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get filtered applications based on search and status filter
     */
    fun getFilteredApplications(): List<JobApplication> {
        val state = _uiState.value
        var filtered = state.applications
        
        // Filter by search query
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter { application ->
                application.jobId.contains(state.searchQuery, ignoreCase = true) ||
                application.personalInfo.fullName.contains(state.searchQuery, ignoreCase = true) ||
                application.skills.any { skill -> 
                    skill.contains(state.searchQuery, ignoreCase = true) 
                }
            }
        }
        
        // Filter by status
        if (state.selectedStatusFilter != null) {
            filtered = filtered.filter { application ->
                application.status == state.selectedStatusFilter
            }
        }
        
        return filtered
    }
    
    /**
     * Get filtered saved jobs based on search query
     */
    fun getFilteredSavedJobs(): List<com.example.partimes.jobseeker.models.JobCardModel> {
        val state = _uiState.value
        var filtered = state.savedJobs
        
        // Filter by search query
        if (state.searchQuery.isNotBlank()) {
            filtered = savedJobsRepository.searchSavedJobs(state.searchQuery)
        }
        
        return filtered
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Get application statistics
     */
    fun getApplicationStats(): Map<ApplicationStatus, Int> {
        return _uiState.value.applications.groupingBy { it.status }.eachCount()
    }
}
