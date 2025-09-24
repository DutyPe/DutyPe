package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.auth.AuthManager
import com.example.partimes.models.JobListing
import com.example.partimes.repositories.JobRecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobRecommendationUiState(
    val personalizedRecommendations: List<JobListing> = emptyList(),
    val similarJobs: List<JobListing> = emptyList(),
    val popularJobs: List<JobListing> = emptyList(),
    val trendingJobs: List<JobListing> = emptyList(),
    val locationBasedRecommendations: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class JobRecommendationViewModel @Inject constructor(
    private val jobRecommendationRepository: JobRecommendationRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobRecommendationUiState())
    val uiState: StateFlow<JobRecommendationUiState> = _uiState.asStateFlow()

    fun initialize(authManager: AuthManager) {
        // No specific initialization needed here as JobRecommendationRepository already uses AuthManager
    }

    fun loadPersonalizedRecommendations(limit: Int = 10) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false, error = null)
            
            val result = jobRecommendationRepository.getPersonalizedRecommendations(limit)
            result.onSuccess { recommendations ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    personalizedRecommendations = recommendations
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

    fun loadSimilarJobs(jobId: String, limit: Int = 5) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false, error = null)
            
            val result = jobRecommendationRepository.getSimilarJobs(jobId, limit)
            result.onSuccess { similarJobs ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    similarJobs = similarJobs
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

    fun loadPopularJobs(limit: Int = 10) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false, error = null)
            
            val result = jobRecommendationRepository.getPopularJobs(limit)
            result.onSuccess { popularJobs ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    popularJobs = popularJobs
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

    fun loadTrendingJobs(limit: Int = 10) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false, error = null)
            
            val result = jobRecommendationRepository.getTrendingJobs(limit)
            result.onSuccess { trendingJobs ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    trendingJobs = trendingJobs
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

    fun loadLocationBasedRecommendations(location: String, limit: Int = 10) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false, error = null)
            
            val result = jobRecommendationRepository.getLocationBasedRecommendations(location, limit)
            result.onSuccess { recommendations ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    locationBasedRecommendations = recommendations
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

    fun refreshAllRecommendations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, hasError = false, error = null)
            
            // Load all types of recommendations
            val personalizedResult = jobRecommendationRepository.getPersonalizedRecommendations(10)
            val popularResult = jobRecommendationRepository.getPopularJobs(10)
            val trendingResult = jobRecommendationRepository.getTrendingJobs(10)
            
            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                personalizedRecommendations = personalizedResult.getOrNull() ?: emptyList(),
                popularJobs = popularResult.getOrNull() ?: emptyList(),
                trendingJobs = trendingResult.getOrNull() ?: emptyList(),
                hasError = personalizedResult.isFailure || popularResult.isFailure || trendingResult.isFailure,
                error = when {
                    personalizedResult.isFailure -> personalizedResult.exceptionOrNull()?.message
                    popularResult.isFailure -> popularResult.exceptionOrNull()?.message
                    trendingResult.isFailure -> trendingResult.exceptionOrNull()?.message
                    else -> null
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, hasError = false)
    }
}
