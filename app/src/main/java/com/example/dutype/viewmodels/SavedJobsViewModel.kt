package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.repositories.FirestoreSavedJobRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val auth: FirebaseAuth,
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedJobsUiState())
    val uiState: StateFlow<SavedJobsUiState> = _uiState.asStateFlow()
    
    /**
     * Check if user is authenticated
     */
    private fun isAuthenticated(): Boolean = auth.currentUser != null

    init {
        // Load saved jobs on initialization
        loadSavedJobs()
    }

    fun loadSavedJobs() {
        // Skip if not authenticated (Guest Mode)
        if (!isAuthenticated()) {
            Timber.d("SavedJobsViewModel: Skipping load - user not authenticated (Guest Mode)")
            _uiState.value = _uiState.value.copy(isLoading = false, savedJobs = emptyList(), savedJobCount = 0)
            return
        }
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            com.example.dutype.performance.MainThreadChecker.assertMainThread("SavedJobsViewModel.loadSavedJobs")
            
            Timber.d("SavedJobsViewModel: Loading saved jobs...")
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false, error = null)
            
            savedJobRepository.getSavedJobs().collect { result ->
                result.onSuccess { jobs ->
                    val duration = System.currentTimeMillis() - startTime
                    performanceTracker.trackApiCall("load_saved_jobs", duration, success = true)
                    
                    Timber.d("SavedJobsViewModel: Loaded ${jobs.size} saved jobs in ${duration}ms")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        savedJobs = jobs,
                        savedJobCount = jobs.size
                    )
                }.onFailure { e ->
                    val duration = System.currentTimeMillis() - startTime
                    performanceTracker.trackApiCall("load_saved_jobs", duration, success = false)
                    
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
            _uiState.value = _uiState.value.copy(isSaving = true)
            
            try {
                // SIMPLE: Direct Firestore update (like Naukri/Lokal Jobs)
                withContext(Dispatchers.IO) {
                    savedJobRepository.saveJob(jobId)
                }
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showMessage = "Job saved successfully"
                )
                // P1 FIX: Only refresh in background — UI already updated optimistically by caller
                viewModelScope.launch { loadSavedJobs() }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    showMessage = "Failed to save job: ${e.message}"
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
            _uiState.value = _uiState.value.copy(isUnsaving = true)
            
            try {
                // SIMPLE: Direct Firestore update (like Naukri/Lokal Jobs)
                withContext(Dispatchers.IO) {
                    savedJobRepository.unsaveJob(jobId)
                }
                // P1 FIX: Optimistic local removal — remove from current list immediately
                val updatedJobs = _uiState.value.savedJobs.filter { it.id != jobId }
                _uiState.value = _uiState.value.copy(
                    isUnsaving = false,
                    savedJobs = updatedJobs,
                    savedJobCount = updatedJobs.size,
                    showMessage = "Job removed from saved list"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUnsaving = false,
                    showMessage = "Failed to remove job: ${e.message}"
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
        viewModelScope.launch(Dispatchers.IO) {
            val result = savedJobRepository.isJobSaved(jobId)
            val saved = result.getOrElse { false }
            withContext(Dispatchers.Main) {
                onResult(saved)
            }
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
