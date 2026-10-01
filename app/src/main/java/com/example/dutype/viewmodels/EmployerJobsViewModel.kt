package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.cache.EmployerProfileCache
import com.example.dutype.data.JobDraftDataStore
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.jobs.JobForm
import com.example.dutype.jobs.JobRepository
import com.example.dutype.models.JobListing
import com.example.dutype.utils.toJobListing
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class EmployerJobsUiState(
    val myJobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val isCreatingJob: Boolean = false,
    val isUpdatingJob: Boolean = false
)

/**
 * The employer's own jobs (live list of cards) and every job write, through [JobRepository].
 * Server errors (quota, edit window, duplicates) come back as readable messages.
 */
@HiltViewModel
class EmployerJobsViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val auth: FirebaseAuth,
    val employerProfileCache: EmployerProfileCache,
    val jobDraftDataStore: JobDraftDataStore,
    val locationService: com.example.dutype.utils.LocationService,
    val profileCompletionService: com.example.dutype.services.ProfileCompletionService,
    private val workLocations: com.example.dutype.services.SavedWorkLocationsStore
) : ViewModel() {

    /** Saved hiring addresses; the listener starts on first use (employer screens only). */
    val savedWorkLocationsStore: com.example.dutype.services.SavedWorkLocationsStore
        get() = workLocations.also { it.start() }

    private val _uiState = MutableStateFlow(EmployerJobsUiState())
    val uiState: StateFlow<EmployerJobsUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null
    /** One request id per post attempt, so a retried tap never creates a second job. */
    private var pendingPostRequestId: String? = null

    /** Starts the live list once; later calls are no-ops while it is listening. */
    fun loadMyJobs() {
        val uid = auth.currentUser?.uid ?: run {
            _uiState.update { it.copy(hasError = true, error = "User not authenticated") }
            return
        }
        if (listenJob?.isActive == true) return
        _uiState.update { it.copy(isLoading = it.myJobs.isEmpty(), hasError = false, error = null) }
        listenJob = viewModelScope.launch {
            jobRepository.employerJobs(uid).collect { result ->
                result.onSuccess { jobs ->
                    _uiState.update { it.copy(myJobs = jobs.map { job -> job.toJobListing() }, isLoading = false) }
                }.onFailure { error ->
                    Timber.w(error, "EmployerJobsVM: live jobs failed")
                    _uiState.update { it.copy(isLoading = false, hasError = true, error = error.message) }
                }
            }
        }
    }

    fun postJob(form: JobForm, callback: (Boolean, String?, String?) -> Unit) {
        val requestId = pendingPostRequestId ?: UUID.randomUUID().toString().also { pendingPostRequestId = it }
        _uiState.update { it.copy(isCreatingJob = true, hasError = false, error = null) }
        viewModelScope.launch {
            jobRepository.postJob(form, requestId)
                .onSuccess { jobId ->
                    pendingPostRequestId = null
                    clearDraft()
                    _uiState.update { it.copy(isCreatingJob = false) }
                    callback(true, jobId, null)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isCreatingJob = false) }
                    callback(false, null, error.message)
                }
        }
    }

    fun updateJob(jobId: String, form: JobForm, callback: (Boolean, String?) -> Unit) = write(callback) {
        jobRepository.updateJob(jobId, form)
    }

    fun markFilled(jobId: String, callback: (Boolean, String?) -> Unit) = write(callback) {
        jobRepository.setStatus(jobId, Values.JobStatus.FILLED)
    }

    fun closeJob(jobId: String, callback: (Boolean, String?) -> Unit) = write(callback) {
        jobRepository.setStatus(jobId, Values.JobStatus.CLOSED)
    }

    fun reopenJob(jobId: String, callback: (Boolean, String?) -> Unit) = write(callback) {
        jobRepository.setStatus(jobId, Values.JobStatus.OPEN)
    }

    fun renewJob(jobId: String, callback: (Boolean, String?) -> Unit) = write(callback) {
        jobRepository.renewJob(jobId)
    }

    fun deleteJob(jobId: String, callback: (Boolean, String?) -> Unit) = write(callback) {
        jobRepository.deleteJob(jobId)
    }

    fun getJob(jobId: String, callback: (JobListing?) -> Unit) {
        viewModelScope.launch { callback(jobRepository.getJob(jobId, refresh = true).getOrNull()) }
    }

    suspend fun getCachedProfile(): EmployerProfileCache.CachedProfile? =
        auth.currentUser?.uid?.let { employerProfileCache.getProfile(it) }

    /** Drafts are device-scoped; guests keep theirs under "GUEST" through sign-in. */
    suspend fun getSavedDraft(): JobDraftDataStore.JobDraft? = jobDraftDataStore.getDraft(auth.currentUser?.uid.orEmpty())

    fun saveDraft(draft: JobDraftDataStore.JobDraft) {
        viewModelScope.launch { jobDraftDataStore.saveDraft(draft.copy(employerId = auth.currentUser?.uid ?: "GUEST")) }
    }

    fun clearDraft() {
        viewModelScope.launch { jobDraftDataStore.clearDraft() }
    }

    fun clearError() = _uiState.update { it.copy(error = null, hasError = false) }

    private fun write(callback: (Boolean, String?) -> Unit, action: suspend () -> Result<Unit>) {
        _uiState.update { it.copy(isUpdatingJob = true) }
        viewModelScope.launch {
            val result = action()
            _uiState.update { it.copy(isUpdatingJob = false) }
            callback(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }
}
