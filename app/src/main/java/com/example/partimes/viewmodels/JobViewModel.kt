package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.auth.AuthManager
import com.example.partimes.models.JobListing
import com.example.partimes.network.ApiClient
import com.example.partimes.repositories.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobUiState(
    val jobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = true,
    val totalJobs: Int = 0
)

@HiltViewModel
class JobViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(JobUiState())
    val uiState: StateFlow<JobUiState> = _uiState.asStateFlow()
    
    
    fun loadJobs(page: Int = 0, size: Int = 20) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val result = jobRepository.getAllJobs(page, size)
                result.fold(
                    onSuccess = { data ->
                        val jobs = data["jobs"] as? List<Map<String, Any>> ?: emptyList()
                        val jobListings = jobs.map { jobData ->
                            convertMapToJobListing(jobData)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            jobs = if (page == 0) jobListings else _uiState.value.jobs + jobListings,
                            isLoading = false,
                            currentPage = page,
                            hasMore = (data["hasNext"] as? Boolean) ?: false,
                            totalJobs = (data["totalItems"] as? Int) ?: 0
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasError = true,
                            error = exception.message ?: "Failed to load jobs"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load jobs"
                )
            }
        }
    }
    
    fun refreshJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null, hasError = false)
            
            try {
                val result = jobRepository.getAllJobs(0, 20)
                result.fold(
                    onSuccess = { data ->
                        val jobs = data["jobs"] as? List<Map<String, Any>> ?: emptyList()
                        val jobListings = jobs.map { jobData ->
                            convertMapToJobListing(jobData)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            jobs = jobListings,
                            isRefreshing = false,
                            currentPage = 0,
                            hasMore = (data["hasNext"] as? Boolean) ?: false,
                            totalJobs = (data["totalItems"] as? Int) ?: 0
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            hasError = true,
                            error = exception.message ?: "Failed to refresh jobs"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    hasError = true,
                    error = e.message ?: "Failed to refresh jobs"
                )
            }
        }
    }
    
    fun loadMoreJobs() {
        if (!_uiState.value.hasMore || _uiState.value.isLoading) return
        
        val nextPage = _uiState.value.currentPage + 1
        loadJobs(nextPage, 20)
    }
    
    fun searchJobs(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val result = jobRepository.searchJobs(query, 0, 20)
                result.fold(
                    onSuccess = { data ->
                        val jobs = data["jobs"] as? List<Map<String, Any>> ?: emptyList()
                        val jobListings = jobs.map { jobData ->
                            convertMapToJobListing(jobData)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            jobs = jobListings,
                            isLoading = false,
                            currentPage = 0,
                            hasMore = false,
                            totalJobs = jobListings.size
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasError = true,
                            error = exception.message ?: "Failed to search jobs"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to search jobs"
                )
            }
        }
    }
    
    fun filterJobs(
        city: String? = null,
        payType: String? = null,
        jobType: String? = null,
        category: String? = null,
        urgency: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val result = jobRepository.filterJobs(city, payType, jobType, category, urgency, 0, 20)
                result.fold(
                    onSuccess = { data ->
                        val jobs = data["jobs"] as? List<Map<String, Any>> ?: emptyList()
                        val jobListings = jobs.map { jobData ->
                            convertMapToJobListing(jobData)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            jobs = jobListings,
                            isLoading = false,
                            currentPage = 0,
                            hasMore = false,
                            totalJobs = jobListings.size
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasError = true,
                            error = exception.message ?: "Failed to filter jobs"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to filter jobs"
                )
            }
        }
    }
    
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, hasError = false)
    }
    
    private fun convertMapToJobListing(jobData: Map<String, Any>): JobListing {
        return JobListing(
            id = jobData["jobId"] as? String ?: "",
            title = jobData["title"] as? String ?: "",
            company = jobData["employerName"] as? String ?: "",
            location = jobData["location"] as? String ?: "",
            salary = jobData["payAmount"] as? String ?: "",
            description = jobData["description"] as? String ?: "",
            requirements = (jobData["requirements"] as? List<String>) ?: emptyList(),
            benefits = (jobData["benefits"] as? List<String>) ?: emptyList(),
            postedDate = jobData["postedTime"] as? String ?: "",
            jobType = jobData["jobType"] as? String ?: "Part-time",
            experienceLevel = jobData["experienceRequired"] as? String ?: "Entry Level",
            isRemote = false,
            urgency = (jobData["urgency"] as? String) ?: "NORMAL",
            category = jobData["category"] as? String ?: "General",
            skills = (jobData["skills"] as? List<String>) ?: emptyList(),
            contactInfo = jobData["contactNumber"] as? String ?: "",
            applicationDeadline = jobData["applicationDeadline"] as? String ?: "",
            workingHours = jobData["workingHours"] as? String ?: "",
            ageRange = jobData["ageRange"] as? String ?: "",
            gender = jobData["gender"] as? String ?: "",
            companySize = jobData["companySize"] as? String ?: "",
            industry = jobData["industry"] as? String ?: "",
            isVerified = (jobData["isVerified"] as? Boolean) ?: false,
            viewCount = (jobData["viewCount"] as? Number)?.toLong() ?: 0L,
            applicationCount = (jobData["applicationsReceived"] as? Number)?.toLong() ?: 0L,
            // isBookmarked and isApplied are jobseeker-specific and handled separately
        )
    }
    
    /**
     * Get a specific job by ID
     */
    suspend fun getJobById(jobId: String): Result<JobListing> {
        return try {
            val result = jobRepository.getJobById(jobId)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
