package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.repositories.FirestoreSavedJobRepository
import com.example.dutype.state.SavedJobsStateManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class SavedJobsUiState(
    val savedJobs: List<JobListing> = emptyList(),
    val savedJobSummaries: List<JobListingSummary> = emptyList(),
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
    private val savedJobsStateManager: SavedJobsStateManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedJobsUiState())
    val uiState: StateFlow<SavedJobsUiState> = _uiState.asStateFlow()
    
    /**
     * Check if user is authenticated
     */
    private fun isAuthenticated(): Boolean = auth.currentUser != null

    init {
        // Only listen to refresh triggers if user is authenticated
        // This prevents unnecessary Firestore calls in Guest Mode
        viewModelScope.launch {
            combine(
                savedJobsStateManager.refreshTrigger,
                savedJobsStateManager.savedJobIds
            ) { _, _ ->
                if (isAuthenticated()) {
                    loadSavedJobs()
                }
            }.collect { }
        }
    }

    fun loadSavedJobs() {
        // Skip if not authenticated (Guest Mode)
        if (!isAuthenticated()) {
            Timber.d("SavedJobsViewModel: Skipping load - user not authenticated (Guest Mode)")
            _uiState.value = _uiState.value.copy(isLoading = false, savedJobs = emptyList(), savedJobCount = 0)
            return
        }
        
        viewModelScope.launch {
            Timber.d("SavedJobsViewModel: Loading saved jobs...")
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false, error = null)
            
            savedJobRepository.getSavedJobs().collect { result ->
                result.onSuccess { jobs ->
                    Timber.d("SavedJobsViewModel: Loaded ${jobs.size} saved jobs")
                    jobs.forEach { job ->
                        Timber.d("SavedJobsViewModel: Saved job - ${job.id} (${job.title})")
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        savedJobs = jobs,
                        savedJobCount = jobs.size
                    )
                }.onFailure { e ->
                    Timber.e(e, "SavedJobsViewModel: Failed to load saved jobs")
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
        // Skip if not authenticated (Guest Mode)
        if (!isAuthenticated()) {
            Timber.d("SavedJobsViewModel: Skipping refresh - user not authenticated (Guest Mode)")
            _uiState.value = _uiState.value.copy(isRefreshing = false)
            return
        }
        
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
        // Skip if not authenticated (Guest Mode)
        if (!isAuthenticated()) {
            Timber.d("SavedJobsViewModel: Cannot save job - user not authenticated (Guest Mode)")
            _uiState.value = _uiState.value.copy(showMessage = "Please sign in to save jobs")
            return
        }
        
        viewModelScope.launch {
            Timber.i("SavedJobsViewModel: Saving job $jobId")
            _uiState.value = _uiState.value.copy(isSaving = true, hasError = false, error = null)
            
            val result = savedJobRepository.saveJob(jobId)
            result.onSuccess {
                Timber.i("SavedJobsViewModel: Successfully saved job $jobId")
                // Update centralized state
                savedJobsStateManager.addSavedJob(jobId)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showMessage = "Job saved successfully"
                )
            }.onFailure { e ->
                Timber.e(e, "SavedJobsViewModel: Failed to save job $jobId")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    hasError = true,
                    error = e.message ?: "Failed to save job"
                )
            }
        }
    }

    fun unsaveJob(jobId: String) {
        // Skip if not authenticated (Guest Mode)
        if (!isAuthenticated()) {
            Timber.d("SavedJobsViewModel: Cannot unsave job - user not authenticated (Guest Mode)")
            return
        }
        
        viewModelScope.launch {
            Timber.i("SavedJobsViewModel: Unsaving job $jobId")
            _uiState.value = _uiState.value.copy(isUnsaving = true, hasError = false, error = null)
            
            val result = savedJobRepository.unsaveJob(jobId)
            result.onSuccess {
                Timber.i("SavedJobsViewModel: Successfully unsaved job $jobId")
                // Update centralized state
                savedJobsStateManager.removeSavedJob(jobId)
                _uiState.value = _uiState.value.copy(
                    isUnsaving = false,
                    showMessage = "Job removed from saved list"
                )
            }.onFailure { e ->
                Timber.e(e, "SavedJobsViewModel: Failed to unsave job $jobId")
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
    
    /**
     * PERFORMANCE OPTIMIZATION: Load saved jobs as summaries
     * Use this for list views to reduce memory and network usage
     */
    fun loadSavedJobSummaries() {
        // Skip if not authenticated (Guest Mode)
        if (!isAuthenticated()) {
            Timber.d("SavedJobsViewModel: Skipping load summaries - user not authenticated (Guest Mode)")
            _uiState.value = _uiState.value.copy(isLoading = false, savedJobSummaries = emptyList(), savedJobCount = 0)
            return
        }
        
        viewModelScope.launch {
            Timber.d("SavedJobsViewModel: Loading saved job summaries...")
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false, error = null)
            
            savedJobRepository.getSavedJobSummaries().collect { result ->
                result.onSuccess { summaries ->
                    Timber.d("SavedJobsViewModel: Loaded ${summaries.size} saved job summaries")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        savedJobSummaries = summaries,
                        savedJobCount = summaries.size
                    )
                }.onFailure { e ->
                    Timber.e(e, "SavedJobsViewModel: Failed to load saved job summaries")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasError = true,
                        error = e.message ?: "Failed to load saved jobs"
                    )
                }
            }
        }
    }
    
    /**
     * Refresh saved job summaries
     */
    fun refreshSavedJobSummaries() {
        // Skip if not authenticated (Guest Mode)
        if (!isAuthenticated()) {
            Timber.d("SavedJobsViewModel: Skipping refresh summaries - user not authenticated (Guest Mode)")
            _uiState.value = _uiState.value.copy(isRefreshing = false)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, hasError = false, error = null)
            
            savedJobRepository.getSavedJobSummaries().collect { result ->
                result.onSuccess { summaries ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        savedJobSummaries = summaries,
                        savedJobCount = summaries.size
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
}
