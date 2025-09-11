package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.models.JobListing
import com.example.partimes.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

class JobListingsViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    // StateFlows for UI states
    private val _allJobs = MutableStateFlow<List<JobListing>>(emptyList())
    val allJobs: StateFlow<List<JobListing>> = _allJobs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    // Cache timestamp to know when the data was last fetched
    private var lastFetchTime: Long = 0

    // Cache TTL in milliseconds (e.g., 5 minutes)
    private val CACHE_TTL = 5 * 60 * 1000

    /**
     * Fetch jobs from the repository with caching strategy
     * @param forceRefresh If true, ignores the cache and fetches fresh data
     */
    fun fetchJobs(forceRefresh: Boolean = false) {
        val currentTime = System.currentTimeMillis()

        // Check if we have cached data and it's still valid
        if (!forceRefresh &&
            _allJobs.value.isNotEmpty() &&
            (currentTime - lastFetchTime) < CACHE_TTL) {
            // Use cached data
            return
        }

        // Show loading or refreshing state
        if (_allJobs.value.isEmpty()) {
            _isLoading.value = true
        } else {
            _isRefreshing.value = true
        }

        // Reset error state
        _hasError.value = false
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                // Fetch jobs from repository
                repository.getAllJobs().collect { jobs ->
                    // Update state flows with fetched data
                    _allJobs.value = jobs
                    _isLoading.value = false
                    _isRefreshing.value = false
                    lastFetchTime = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                // Handle error state
                _hasError.value = true
                _errorMessage.value = when (e) {
                    is IOException -> "Network error. Please check your connection."
                    else -> e.message ?: "Failed to load jobs"
                }
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Search jobs by query
     * @param query The search query
     */
    fun searchJobs(query: String) {
        viewModelScope.launch {
            if (query.isEmpty()) {
                // If query is empty, just use the cached data or fetch new data
                if (_allJobs.value.isEmpty()) {
                    fetchJobs()
                }
                return@launch
            }

            _isLoading.value = true

            try {
                // This is a simple in-memory filtering
                val filteredJobs = repository.getJobById("")?.let { listOf(it) } ?: emptyList()

                // Simple in-memory search
                val searchResults = _allJobs.value.filter { job ->
                    job.title.contains(query, ignoreCase = true) ||
                            job.company.contains(query, ignoreCase = true) ||
                            job.description.contains(query, ignoreCase = true) ||
                            job.locationNearby.contains(query, ignoreCase = true) ||
                            job.specificLocation.contains(query, ignoreCase = true)
                }

                _allJobs.value = searchResults
            } catch (e: Exception) {
                _hasError.value = true
                _errorMessage.value = "Error searching jobs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Filter jobs by timing type (hourly, daily, part-time, etc.)
     * @param timingType The timing type to filter by
     */
    fun filterByTimingType(timingType: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                repository.getJobsByTimingType(timingType).collect { filteredJobs ->
                    _allJobs.value = filteredJobs
                }
            } catch (e: Exception) {
                _hasError.value = true
                _errorMessage.value = "Error filtering jobs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Filter jobs by location
     * @param location The location to filter by
     */
    fun filterByLocation(location: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                repository.getJobsByLocation(location).collect { filteredJobs ->
                    _allJobs.value = filteredJobs
                }
            } catch (e: Exception) {
                _hasError.value = true
                _errorMessage.value = "Error filtering jobs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear all filters and show all jobs
     */
    fun clearFilters() {
        fetchJobs(false)
    }
}