package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.auth.AuthManager
import com.example.partimes.models.JobListing
import com.example.partimes.repositories.SavedJobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedJobUiState(
    val savedJobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val savedJobCount: Int = 0,
    val isSaving: Boolean = false,
    val isUnsaving: Boolean = false
)

@HiltViewModel
class SavedJobViewModel @Inject constructor(
    private val savedJobRepository: SavedJobRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedJobUiState())
    val uiState: StateFlow<SavedJobUiState> = _uiState.asStateFlow()

    fun initialize(authManager: AuthManager) {
        // No specific initialization needed here as SavedJobRepository already uses AuthManager
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
                    error = e.message
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
                    error = e.message
                )
            }
        }
    }

    fun saveJob(jobId: String, notes: String? = null, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, hasError = false, error = null)
            
            val result = savedJobRepository.saveJob(jobId, notes)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false)
                onComplete(true, null)
                loadSavedJobs() // Refresh the list
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    hasError = true,
                    error = e.message
                )
                onComplete(false, e.message)
            }
        }
    }

    fun unsaveJob(jobId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUnsaving = true, hasError = false, error = null)
            
            val result = savedJobRepository.unsaveJob(jobId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isUnsaving = false)
                onComplete(true, null)
                loadSavedJobs() // Refresh the list
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isUnsaving = false,
                    hasError = true,
                    error = e.message
                )
                onComplete(false, e.message)
            }
        }
    }

    fun isJobSaved(jobId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = savedJobRepository.isJobSaved(jobId)
            result.onSuccess { isSaved ->
                onComplete(isSaved)
            }.onFailure {
                onComplete(false)
            }
        }
    }

    fun updateSavedJobNotes(jobId: String, notes: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = savedJobRepository.updateSavedJobNotes(jobId, notes)
            result.onSuccess {
                onComplete(true, null)
            }.onFailure { e ->
                onComplete(false, e.message)
            }
        }
    }

    fun loadSavedJobCount() {
        viewModelScope.launch {
            val result = savedJobRepository.getSavedJobCount()
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(savedJobCount = count)
            }.onFailure {
                // Ignore count loading errors
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, hasError = false)
    }
}
