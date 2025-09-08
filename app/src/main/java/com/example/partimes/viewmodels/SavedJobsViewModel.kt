package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.jobseeker.models.JobCardModel
import com.example.partimes.repository.SavedJobsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for saved jobs screen
 */
data class SavedJobsUiState(
    val savedJobs: List<JobCardModel> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val showMessage: String? = null
)

/**
 * ViewModel for managing saved jobs functionality
 */
class SavedJobsViewModel @Inject constructor(
    private val savedJobsRepository: SavedJobsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedJobsUiState())
    val uiState: StateFlow<SavedJobsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Observe saved jobs and update UI state
        viewModelScope.launch {
            combine(
                savedJobsRepository.savedJobs,
                _searchQuery
            ) { savedJobs, query ->
                val filteredJobs = if (query.isBlank()) {
                    savedJobs
                } else {
                    savedJobsRepository.searchSavedJobs(query)
                }
                _uiState.value = SavedJobsUiState(
                    savedJobs = filteredJobs,
                    isLoading = false,
                    isEmpty = filteredJobs.isEmpty()
                )
            }.collect()
        }
    }

    /**
     * Save a job
     */
    fun saveJob(job: JobCardModel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = savedJobsRepository.saveJob(job)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showMessage = if (success) "Job saved successfully!" else "Job already saved"
            )
        }
    }

    /**
     * Unsave a job
     */
    fun unsaveJob(jobId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = savedJobsRepository.unsaveJob(jobId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showMessage = if (success) "Job removed from saved" else "Failed to remove job"
            )
        }
    }

    /**
     * Check if a job is saved
     */
    fun isJobSaved(jobId: String): Boolean {
        return savedJobsRepository.isJobSaved(jobId)
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Get saved jobs by category
     */
    fun getSavedJobsByCategory(category: String) {
        viewModelScope.launch {
            val filteredJobs = savedJobsRepository.getSavedJobsByCategory(category)
            _uiState.value = _uiState.value.copy(
                savedJobs = filteredJobs,
                isEmpty = filteredJobs.isEmpty()
            )
        }
    }

    /**
     * Clear message
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(showMessage = null)
    }

    /**
     * Clear all saved jobs
     */
    fun clearAllSavedJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            savedJobsRepository.clearAllSavedJobs()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showMessage = "All saved jobs cleared"
            )
        }
    }
}
