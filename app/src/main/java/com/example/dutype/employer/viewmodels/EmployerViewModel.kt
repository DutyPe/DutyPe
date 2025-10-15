package com.example.dutype.employer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.api.employer.JobPostingApiClient
import com.example.dutype.employer.models.JobPostingModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class EmployerViewModel : ViewModel() {

    private val _postedJobs = MutableStateFlow<List<JobPostingModel>>(emptyList())
    val postedJobs: StateFlow<List<JobPostingModel>> = _postedJobs.asStateFlow()

    private val _recentJobs = MutableStateFlow<List<JobPostingModel>>(emptyList())
    val recentJobs: StateFlow<List<JobPostingModel>> = _recentJobs.asStateFlow()

    private val _jobStats = MutableStateFlow(JobStats())
    val jobStats: StateFlow<JobStats> = _jobStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Current job being created/edited
    private val _currentJob = MutableStateFlow<JobPostingModel?>(null)
    val currentJob: StateFlow<JobPostingModel?> = _currentJob.asStateFlow()

    init {
        loadAllData()
        startAutoRefresh()
    }
    
    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30000) // Refresh every 30 seconds
                if (!_isLoading.value && !_isRefreshing.value) {
                    refreshJobs()
                }
            }
        }
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadJobsFromApi()
                calculateJobStats()
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadJobsFromApi() {
        try {
            val allJobs = JobPostingApiClient.api.getJobs()
            _postedJobs.value = allJobs

            // Get recent jobs (last 14 days, max 10 items for better dashboard display)
            val fourteenDaysAgo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000)
            _recentJobs.value = allJobs
                .filter { it.postedTime >= fourteenDaysAgo }
                .sortedByDescending { it.postedTime }
                .take(10)
        } catch (e: Exception) {
            // Fallback to empty lists on API failure
            _postedJobs.value = emptyList()
            _recentJobs.value = emptyList()
            throw e
        }
    }

    private fun calculateJobStats() {
        val jobs = _postedJobs.value
        val activeJobs = jobs.count { it.isActive }
        val totalApplications = jobs.sumOf { it.applicationsReceived }
        val todayJobs = jobs.count { isToday(it.postedTime) }

        _jobStats.value = JobStats(
            activeJobs = activeJobs,
            totalApplications = totalApplications,
            todayJobs = todayJobs,
            totalJobs = jobs.size
        )
    }

    private fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val jobDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == jobDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == jobDate.get(Calendar.DAY_OF_YEAR)
    }

    fun refreshJobs() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            try {
                loadJobsFromApi()
                calculateJobStats()
            } catch (e: Exception) {
                _error.value = "Failed to refresh: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun updateEmployer(
        name: String,
        company: String,
        email: String,
        professionalSkills: List<String>,
        yearsOfExperience: Int,
        position: String,
        companySize: String,
        industry: String,
        bio: String,
        linkedInProfile: String,
        phoneNumber: String
    ) {
        // Implement your update logic here
    }

    fun postJob(jobPosting: JobPostingModel) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val postedJob = JobPostingApiClient.api.postJob(jobPosting)
                // Refresh the jobs list after successful posting
                loadJobsFromApi()
                calculateJobStats()
            } catch (e: Exception) {
                _error.value = "Failed to post job: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateJob(jobPosting: JobPostingModel) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: Implement API call for updating job
                val updatedJobs = _postedJobs.value.map { job ->
                    if (job.jobId == jobPosting.jobId) {
                        jobPosting
                    } else {
                        job
                    }
                }
                _postedJobs.value = updatedJobs
                calculateJobStats()
            } catch (e: Exception) {
                _error.value = "Failed to update job: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteJob(jobPosting: JobPostingModel) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: Implement API call for deleting job
                val updatedJobs = _postedJobs.value.filter { it.jobId != jobPosting.jobId }
                _postedJobs.value = updatedJobs
                _recentJobs.value = _recentJobs.value.filter { it.jobId != jobPosting.jobId }
                calculateJobStats()
            } catch (e: Exception) {
                _error.value = "Failed to delete job: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setCurrentJob(job: JobPostingModel) {
        _currentJob.value = job
    }

    fun clearCurrentJob() {
        _currentJob.value = null
    }

    fun getJobById(jobId: String): JobPostingModel? {
        return _postedJobs.value.find { it.jobId == jobId }
    }
}

data class JobStats(
    val activeJobs: Int = 0,
    val totalApplications: Int = 0,
    val todayJobs: Int = 0,
    val totalJobs: Int = 0
)
