package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.models.JobListing
import com.example.partimes.repositories.FirestoreSavedJobRepository
import com.example.partimes.state.SavedJobsStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val savedJobRepository: FirestoreSavedJobRepository,
    private val savedJobsStateManager: SavedJobsStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedJobsUiState())
    val uiState: StateFlow<SavedJobsUiState> = _uiState.asStateFlow()

    init {
        // Listen to refresh triggers and reload saved jobs
        viewModelScope.launch {
            combine(
                savedJobsStateManager.refreshTrigger,
                savedJobsStateManager.savedJobIds
            ) { _, _ ->
                loadSavedJobs()
            }.collect { }
        }
    }

    fun loadSavedJobs() {
        viewModelScope.launch {
            println("🔍 DEBUG SavedJobsViewModel: Loading saved jobs...")
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false, error = null)
            
            savedJobRepository.getSavedJobs().collect { result ->
                result.onSuccess { jobs ->
                    println("🔍 DEBUG SavedJobsViewModel: Loaded ${jobs.size} saved jobs")
                    jobs.forEach { job ->
                        println("🔍 DEBUG SavedJobsViewModel: Saved job - ${job.id} (${job.title})")
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        savedJobs = jobs,
                        savedJobCount = jobs.size
                    )
                }.onFailure { e ->
                    println("❌ DEBUG SavedJobsViewModel: Failed to load saved jobs: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasError = true,
                        error = e.message ?: "Failed to load saved jobs"
                    )
                }
            }
        }
    }

    fun refreshSavedJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, hasError = false, error = null)
            
            savedJobRepository.getSavedJobs().collect { result ->
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
    }

    fun saveJob(jobId: String, notes: String? = null) {
        viewModelScope.launch {
            println("🔍 DEBUG SavedJobsViewModel: Saving job $jobId")
            _uiState.value = _uiState.value.copy(isSaving = true, hasError = false, error = null)
            
            val result = savedJobRepository.saveJob(jobId)
            result.onSuccess {
                println("✅ DEBUG SavedJobsViewModel: Successfully saved job $jobId")
                // Update centralized state
                savedJobsStateManager.addSavedJob(jobId)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showMessage = "Job saved successfully"
                )
            }.onFailure { e ->
                println("❌ DEBUG SavedJobsViewModel: Failed to save job $jobId: ${e.message}")
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
            println("🔍 DEBUG SavedJobsViewModel: Unsaving job $jobId")
            _uiState.value = _uiState.value.copy(isUnsaving = true, hasError = false, error = null)
            
            val result = savedJobRepository.unsaveJob(jobId)
            result.onSuccess {
                println("✅ DEBUG SavedJobsViewModel: Successfully unsaved job $jobId")
                // Update centralized state
                savedJobsStateManager.removeSavedJob(jobId)
                _uiState.value = _uiState.value.copy(
                    isUnsaving = false,
                    showMessage = "Job removed from saved list"
                )
            }.onFailure { e ->
                println("❌ DEBUG SavedJobsViewModel: Failed to unsave job $jobId: ${e.message}")
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

    // Check if a specific job is saved and deliver result to caller
    fun isJobSaved(jobId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = savedJobRepository.isJobSaved(jobId)
            val saved = result.getOrElse { false }
            onResult(saved)
        }
    }
}
