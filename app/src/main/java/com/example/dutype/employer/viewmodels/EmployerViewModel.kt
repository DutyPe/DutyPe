package com.example.dutype.employer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.services.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class EmployerViewModel @Inject constructor(
    private val firestoreService: FirestoreService
) : ViewModel() {

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
    }
    
    // Auto-refresh removed - use manual refresh or pull-to-refresh instead
    // This prevents battery drain from continuous polling

    private fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadJobsFromFirebase()
                calculateJobStats()
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadJobsFromFirebase() {
        try {
            // Get current employer ID
            val currentEmployerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (currentEmployerId == null) {
                Timber.w("❌ EmployerViewModel: No authenticated user")
                _postedJobs.value = emptyList()
                _recentJobs.value = emptyList()
                return
            }
            
            Timber.d("📋 EmployerViewModel: Loading jobs for employer: $currentEmployerId")
            
            val result = firestoreService.getAllJobs(limit = 100L)
            result.onSuccess { jobsData ->
                // Convert Map<String, Any> to JobPostingModel and filter by employer ID
                val allJobs = jobsData.mapNotNull { jobMap ->
                    try {
                        // Map data to proper JobPostingModel fields
                        JobPostingModel(
                            jobId = jobMap["jobId"] as? String ?: "",
                            title = jobMap["title"] as? String ?: "",
                            payAmount = jobMap["payAmount"] as? String ?: "0",
                            payType = com.example.dutype.employer.models.enums.PayType.DAILY, // Default
                            location = jobMap["location"] as? String ?: "",
                            description = jobMap["description"] as? String ?: "",
                            contactNumber = jobMap["contactNumber"] as? String ?: "",
                            category = com.example.dutype.employer.models.enums.JobCategory.HELPER, // Default
                            postedTime = jobMap["postedTime"] as? Long ?: (jobMap["postedAt"] as? Long ?: 0L),
                            isActive = jobMap["isActive"] as? Boolean ?: true,
                            applicationsReceived = (jobMap["applicationsReceived"] as? Number)?.toInt() 
                                ?: (jobMap["applicationCount"] as? Number)?.toInt() ?: 0,
                            employerId = jobMap["employerId"] as? String
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing job")
                        null
                    }
                }.filter { it.employerId == currentEmployerId } // Filter by current employer
                
                Timber.d("📋 EmployerViewModel: Found ${allJobs.size} jobs for employer")
                _postedJobs.value = allJobs

                // Get recent jobs (last 14 days, max 10 items for better dashboard display)
                val fourteenDaysAgo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000)
                _recentJobs.value = allJobs
                    .filter { it.postedTime >= fourteenDaysAgo }
                    .sortedByDescending { it.postedTime }
                    .take(10)
            }
            result.onFailure { exception ->
                // Fallback to empty lists on Firebase failure
                Timber.e(exception, "❌ EmployerViewModel: Failed to load jobs")
                _postedJobs.value = emptyList()
                _recentJobs.value = emptyList()
                throw exception
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ EmployerViewModel: Exception loading jobs")
            _postedJobs.value = emptyList()
            _recentJobs.value = emptyList()
            throw e
        }
    }

    private fun calculateJobStats() {
        val jobs = _postedJobs.value
        val activeJobs = jobs.count { it.isActive }
        val pausedJobs = jobs.count { !it.isActive } // Count inactive (paused) jobs
        val totalApplications = jobs.sumOf { it.applicationsReceived }
        val todayJobs = jobs.count { isToday(it.postedTime) }

        _jobStats.value = JobStats(
            activeJobs = activeJobs,
            pausedJobs = pausedJobs,
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
                loadJobsFromFirebase()
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

    fun toggleJobActive(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val job = _postedJobs.value.find { it.jobId == jobId }
                if (job != null) {
                    val newActiveStatus = !job.isActive
                    val result = firestoreService.updateJob(jobId, mapOf(
                        "isActive" to newActiveStatus,
                        "updatedAt" to System.currentTimeMillis()
                    ))
                    result.onSuccess {
                        Timber.d("✅ Job ${jobId} toggled to active=$newActiveStatus")
                        // Refresh jobs to show updated status
                        loadJobsFromFirebase()
                        calculateJobStats()
                    }
                    result.onFailure { exception ->
                        _error.value = "Failed to toggle job status: ${exception.message}"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to toggle job status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
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
                // Convert JobPostingModel to Map for Firestore
                val jobData = mapOf(
                    "jobId" to jobPosting.jobId,
                    "title" to jobPosting.title,
                    "description" to jobPosting.description,
                    "payAmount" to jobPosting.payAmount,
                    "payType" to jobPosting.payType.name,
                    "location" to jobPosting.location,
                    "contactNumber" to jobPosting.contactNumber,
                    "category" to jobPosting.category.name,
                    "postedTime" to System.currentTimeMillis(),
                    "isActive" to true,
                    "applicationsReceived" to 0,
                    "employerId" to (jobPosting.employerId ?: "current_user_id")
                )
                val result = firestoreService.createJob(jobData)
                result.onSuccess {
                    // Refresh the jobs list after successful posting
                    loadJobsFromFirebase()
                    calculateJobStats()
                }
                result.onFailure { exception ->
                    _error.value = "Failed to post job: ${exception.message}"
                }
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
                // Convert JobPostingModel to Map for Firestore update
                val updates = mapOf(
                    "title" to jobPosting.title,
                    "description" to jobPosting.description,
                    "payAmount" to jobPosting.payAmount,
                    "payType" to jobPosting.payType.name,
                    "location" to jobPosting.location,
                    "contactNumber" to jobPosting.contactNumber,
                    "category" to jobPosting.category.name,
                    "isActive" to jobPosting.isActive,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                val result = firestoreService.updateJob(jobPosting.jobId, updates)
                result.onSuccess {
                    Timber.d("✅ Job ${jobPosting.jobId} updated successfully")
                    // Refresh jobs to show updated data
                    loadJobsFromFirebase()
                    calculateJobStats()
                }
                result.onFailure { exception ->
                    _error.value = "Failed to update job: ${exception.message}"
                    Timber.e(exception, "❌ Failed to update job")
                }
            } catch (e: Exception) {
                _error.value = "Failed to update job: ${e.message}"
                Timber.e(e, "❌ Exception updating job")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteJob(jobPosting: JobPostingModel) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = firestoreService.deleteJob(jobPosting.jobId)
                result.onSuccess {
                    Timber.d("✅ Job ${jobPosting.jobId} deleted successfully")
                    // Update local state immediately for better UX
                    val updatedJobs = _postedJobs.value.filter { it.jobId != jobPosting.jobId }
                    _postedJobs.value = updatedJobs
                    _recentJobs.value = _recentJobs.value.filter { it.jobId != jobPosting.jobId }
                    calculateJobStats()
                }
                result.onFailure { exception ->
                    _error.value = "Failed to delete job: ${exception.message}"
                    Timber.e(exception, "❌ Failed to delete job")
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete job: ${e.message}"
                Timber.e(e, "❌ Exception deleting job")
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
        // First check in posted jobs
        val jobFromPosted = _postedJobs.value.find { it.jobId == jobId }
        if (jobFromPosted != null) return jobFromPosted
        
        // If not found, check in recent jobs
        val jobFromRecent = _recentJobs.value.find { it.jobId == jobId }
        if (jobFromRecent != null) return jobFromRecent
        
        return null
    }
    
    fun loadJobById(jobId: String) {
        viewModelScope.launch {
            try {
                Timber.d("🔍 EmployerViewModel - Loading job by ID: $jobId")
                
                // First check in existing data
                val existingJob = getJobById(jobId)
                if (existingJob != null) {
                    Timber.d("🔍 EmployerViewModel - Job found in existing data")
                    _currentJob.value = existingJob
                    return@launch
                }
                
                // If not found, load from Firebase
                Timber.d("🔍 EmployerViewModel - Loading job from Firebase")
                val result = firestoreService.getAllJobs(limit = 100L)
                result.onSuccess { jobsData ->
                    val allJobs = jobsData.mapNotNull { jobMap ->
                        try {
                            JobPostingModel(
                                jobId = jobMap["jobId"] as? String ?: "",
                                title = jobMap["title"] as? String ?: "",
                                payAmount = jobMap["payAmount"] as? String ?: "0",
                                payType = com.example.dutype.employer.models.enums.PayType.DAILY,
                                location = jobMap["location"] as? String ?: "",
                                description = jobMap["description"] as? String ?: "",
                                contactNumber = jobMap["contactNumber"] as? String ?: "",
                                category = com.example.dutype.employer.models.enums.JobCategory.HELPER,
                                postedTime = jobMap["postedTime"] as? Long ?: 0L,
                                isActive = jobMap["isActive"] as? Boolean ?: true,
                                applicationsReceived = (jobMap["applicationsReceived"] as? Number)?.toInt() ?: 0,
                                employerId = jobMap["employerId"] as? String
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val job = allJobs.find { it.jobId == jobId }
                    if (job != null) {
                        Timber.d("🔍 EmployerViewModel - Job found in Firebase")
                        _currentJob.value = job
                    } else {
                        Timber.w("🔍 EmployerViewModel - Job not found in Firebase")
                    }
                }
                result.onFailure { exception ->
                    Timber.e(exception, "🔍 EmployerViewModel - Error loading job by ID")
                }
            } catch (e: Exception) {
                Timber.e(e, "🔍 EmployerViewModel - Error loading job by ID")
            }
        }
    }
}

data class JobStats(
    val activeJobs: Int = 0,
    val pausedJobs: Int = 0,
    val totalApplications: Int = 0,
    val todayJobs: Int = 0,
    val totalJobs: Int = 0
)
