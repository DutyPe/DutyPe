package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.applications.ApplicationRepository
import com.example.dutype.models.ApplicationStats
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobApplicationUiState
import com.example.dutype.state.ApplicationStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartJobApplicationUiState(
    val isApplying: Boolean = false,
    val isSubmitting: Boolean = false,
    val isWithdrawing: Boolean = false,
    val applications: List<JobApplication> = emptyList(),
    val applicationSuccess: Boolean = false,
    val withdrawalSuccess: Boolean = false,
    val error: String? = null
)

/**
 * The worker's applications: a live list (with each job's current card) plus apply,
 * call-to-apply and withdraw through [ApplicationRepository].
 */
@HiltViewModel
class SmartJobApplicationViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    private val store: com.example.dutype.applications.WorkerApplicationsStore,
    private val applicationStateManager: ApplicationStateManager,
    val reportingService: com.example.dutype.services.ReportingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartJobApplicationUiState())
    val uiState: StateFlow<SmartJobApplicationUiState> = _uiState.asStateFlow()

    private val _legacyUiState = MutableStateFlow(JobApplicationUiState())
    /** List + loading flag for My Jobs. */
    val legacyUiState: StateFlow<JobApplicationUiState> = _legacyUiState.asStateFlow()

    private val _stats = MutableStateFlow(ApplicationStats())
    val stats: StateFlow<ApplicationStats> = _stats.asStateFlow()
    val appliedJobIds: StateFlow<Set<String>> = applicationStateManager.appliedJobIds
    val applicationStatuses: StateFlow<Map<String, ApplicationStatus>> = applicationStateManager.applicationStatuses

    init {
        viewModelScope.launch { store.applications.collect { publish(it) } }
        viewModelScope.launch { store.isLoading.collect { loading -> _legacyUiState.update { it.copy(isLoading = loading) } } }
    }

    /** Starts the shared live list (no-op when it is already listening). */
    fun loadMyApplications() = store.start()

    fun refreshApplications() = store.start()

    fun applyForJob(jobId: String) = submit(jobId, viaCall = false)


    fun hasUserApplied(jobId: String, onResult: (Boolean) -> Unit) {
        onResult(applicationStateManager.isJobApplied(jobId))
    }

    fun withdrawApplication(application: JobApplication, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        _uiState.update { it.copy(isWithdrawing = true, error = null) }
        viewModelScope.launch {
            val result = repository.withdraw(application.jobId)
            _uiState.update {
                it.copy(isWithdrawing = false, withdrawalSuccess = result.isSuccess, error = result.exceptionOrNull()?.message)
            }
            onResult(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    /**
     * The worker tapped Call: counts the call (a first call also creates the application) and
     * returns the employer's number, or null when it is not available.
     */
    suspend fun callEmployer(jobId: String): String? {
        val result = repository.callEmployer(jobId)
        if (result.isSuccess) applicationStateManager.addAppliedJob(jobId)
        return result.getOrNull()?.ifBlank { null }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun clearSuccessStates() = _uiState.update { it.copy(applicationSuccess = false, withdrawalSuccess = false) }

    private fun submit(jobId: String, viaCall: Boolean) {
        _uiState.update { it.copy(isApplying = true, isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = repository.apply(jobId, viaCall)
            if (result.isSuccess) applicationStateManager.addAppliedJob(jobId)
            _uiState.update {
                it.copy(
                    isApplying = false,
                    isSubmitting = false,
                    applicationSuccess = result.isSuccess,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun publish(list: List<JobApplication>) {
        _legacyUiState.update { it.copy(applications = list) }
        _uiState.update { it.copy(applications = list) }
        _stats.value = ApplicationStats(
            totalApplications = list.size,
            appliedApplications = list.count { it.status == ApplicationStatus.APPLIED },
            rejectedApplications = list.count { it.status == ApplicationStatus.REJECTED },
            hiredApplications = list.count { it.status == ApplicationStatus.HIRED },
            recentApplications = list.take(5)
        )
    }
}
