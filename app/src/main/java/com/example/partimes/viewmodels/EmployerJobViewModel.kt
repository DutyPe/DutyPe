package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.auth.AuthManager
import com.example.partimes.models.JobListing
import com.example.partimes.network.ApiClient
import com.example.partimes.repositories.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EmployerJobUiState(
    val myJobs: List<JobListing> = emptyList(),
    val allJobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val isCreatingJob: Boolean = false,
    val isUpdatingJob: Boolean = false,
    val isDeletingJob: Boolean = false
)

class EmployerJobViewModel : ViewModel() {
    
    private lateinit var jobRepository: JobRepository
    private lateinit var authManager: AuthManager
    
    private val _uiState = MutableStateFlow(EmployerJobUiState())
    val uiState: StateFlow<EmployerJobUiState> = _uiState.asStateFlow()
    
    fun initialize(authManager: AuthManager) {
        this.authManager = authManager
        this.jobRepository = JobRepository(ApiClient.getApiService(), authManager)
    }
    
    fun loadMyJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val employerId = authManager.getUserEmail() ?: ""
                val result = jobRepository.getJobsByEmployer(employerId)
                result.fold(
                    onSuccess = { data ->
                        val jobs = data["jobs"] as? List<Map<String, Any>> ?: emptyList()
                        val jobListings = jobs.map { jobData ->
                            convertMapToJobListing(jobData)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            myJobs = jobListings,
                            isLoading = false
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            hasError = true,
                            error = exception.message ?: "Failed to load your jobs"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load your jobs"
                )
            }
        }
    }
    
    fun loadAllJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val result = jobRepository.getAllJobs(0, 20)
                result.fold(
                    onSuccess = { data ->
                        val jobs = data["jobs"] as? List<Map<String, Any>> ?: emptyList()
                        val jobListings = jobs.map { jobData ->
                            convertMapToJobListing(jobData)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            allJobs = jobListings,
                            isLoading = false
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
    
    fun refreshMyJobs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null, hasError = false)
            
            try {
                val employerId = authManager.getUserEmail() ?: ""
                val result = jobRepository.getJobsByEmployer(employerId)
                result.fold(
                    onSuccess = { data ->
                        val jobs = data["jobs"] as? List<Map<String, Any>> ?: emptyList()
                        val jobListings = jobs.map { jobData ->
                            convertMapToJobListing(jobData)
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            myJobs = jobListings,
                            isRefreshing = false
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            hasError = true,
                            error = exception.message ?: "Failed to refresh your jobs"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    hasError = true,
                    error = e.message ?: "Failed to refresh your jobs"
                )
            }
        }
    }
    
    fun createJob(job: JobListing, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingJob = true, error = null, hasError = false)
            
            try {
                val result = jobRepository.createJob(job)
                result.fold(
                    onSuccess = { createdJob ->
                        _uiState.value = _uiState.value.copy(
                            isCreatingJob = false,
                            myJobs = _uiState.value.myJobs + createdJob
                        )
                        callback(true, null)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isCreatingJob = false,
                            hasError = true,
                            error = exception.message ?: "Failed to create job"
                        )
                        callback(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreatingJob = false,
                    hasError = true,
                    error = e.message ?: "Failed to create job"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun updateJob(jobId: String, job: JobListing, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingJob = true, error = null, hasError = false)
            
            try {
                val result = jobRepository.updateJob(jobId, job)
                result.fold(
                    onSuccess = { updatedJob ->
                        _uiState.value = _uiState.value.copy(
                            isUpdatingJob = false,
                            myJobs = _uiState.value.myJobs.map { 
                                if (it.id == jobId) updatedJob else it 
                            }
                        )
                        callback(true, null)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isUpdatingJob = false,
                            hasError = true,
                            error = exception.message ?: "Failed to update job"
                        )
                        callback(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdatingJob = false,
                    hasError = true,
                    error = e.message ?: "Failed to update job"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun deleteJob(jobId: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingJob = true, error = null, hasError = false)
            
            try {
                val result = jobRepository.deleteJob(jobId)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isDeletingJob = false,
                            myJobs = _uiState.value.myJobs.filter { it.id != jobId }
                        )
                        callback(true, null)
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isDeletingJob = false,
                            hasError = true,
                            error = exception.message ?: "Failed to delete job"
                        )
                        callback(false, exception.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeletingJob = false,
                    hasError = true,
                    error = e.message ?: "Failed to delete job"
                )
                callback(false, e.message)
            }
        }
    }
    
    fun getJobById(jobId: String, callback: (JobListing?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = jobRepository.getJobById(jobId)
                result.fold(
                    onSuccess = { job ->
                        callback(job)
                    },
                    onFailure = { exception ->
                        callback(null)
                    }
                )
            } catch (e: Exception) {
                callback(null)
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
            skills = (jobData["perks"] as? List<String>) ?: emptyList(),
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
            // Note: isBookmarked and isApplied are jobseeker-specific and handled separately
        )
    }
}
