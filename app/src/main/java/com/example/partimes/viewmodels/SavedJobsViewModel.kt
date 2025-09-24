package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.models.JobListing
import com.example.partimes.repositories.SavedJobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedJobsUiState(
    val savedJobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val savedJobCount: Int = 0,
    val isSaving: Boolean = false,
    val isUnsaving: Boolean = false,
    val searchQuery: String = "",
    val showMessage: String? = null
)

@HiltViewModel
class SavedJobsViewModel @Inject constructor(
    private val savedJobRepository: SavedJobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedJobsUiState())
    val uiState: StateFlow<SavedJobsUiState> = _uiState.asStateFlow()

    init {
        // Load saved jobs safely - errors are handled in loadSavedJobs()
        try {
            loadSavedJobs()
        } catch (e: Exception) {
            // Handle any initialization errors gracefully
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                hasError = true,
                error = "Failed to initialize saved jobs: ${e.message}"
            )
        }
    }

    fun loadSavedJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false, error = null)
            
            val result = savedJobRepository.getSavedJobs()
            result.onSuccess { jobs ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    savedJobs = jobs,
                    savedJobCount = jobs.size
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load saved jobs"
                )
            }
        }
    }

    fun refreshSavedJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, hasError = false, error = null)
            
            val result = savedJobRepository.getSavedJobs()
            result.onSuccess { jobs ->
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    savedJobs = jobs,
                    savedJobCount = jobs.size
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    hasError = true,
                    error = e.message ?: "Failed to refresh saved jobs"
                )
            }
        }
    }

    fun saveJob(jobId: String, notes: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, hasError = false, error = null)
            
            val result = savedJobRepository.saveJob(jobId, notes)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showMessage = "Job saved successfully"
                )
                // Refresh the list
                loadSavedJobs()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    hasError = true,
                    error = e.message ?: "Failed to save job"
                )
            }
        }
    }

    fun unsaveJob(jobId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUnsaving = true, hasError = false, error = null)
            
            val result = savedJobRepository.unsaveJob(jobId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isUnsaving = false,
                    showMessage = "Job removed from saved list"
                )
                // Refresh the list
                loadSavedJobs()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isUnsaving = false,
                    hasError = true,
                    error = e.message ?: "Failed to unsave job"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(showMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(hasError = false, error = null)
    }
}
