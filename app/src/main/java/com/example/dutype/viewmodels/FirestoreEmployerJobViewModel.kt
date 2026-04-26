package com.example.dutype.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.cache.EmployerProfileCache
import com.example.dutype.data.JobDraftDataStore
import com.example.dutype.employer.sync.JobPostingWorker
import com.example.dutype.models.JobListing
import com.example.dutype.repositories.FirestoreJobRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

data class FirestoreEmployerJobUiState(
    val myJobs: List<JobListing> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false,
    val isCreatingJob: Boolean = false,
    val isUpdatingJob: Boolean = false,
    val isDeletingJob: Boolean = false,
    // P0 FIX: Offline posting state
    val isQueuedOffline: Boolean = false,
    val pendingJobsCount: Int = 0,
    /**
     * Bug #4 fix: per-job application counts loaded from the dedicated
     * `employer_job_cards` denormalized collection (maintained by Cloud
     * Functions). Keyed by jobId. Empty when not loaded yet — the card
     * falls back to 0 in that case.
     */
    val applicationCountsByJobId: Map<String, Int> = emptyMap()
)

@HiltViewModel
class FirestoreEmployerJobViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestoreJobRepository: FirestoreJobRepository,
    val employerProfileCache: EmployerProfileCache,
    val jobDraftDataStore: JobDraftDataStore,
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FirestoreEmployerJobUiState())
    val uiState: StateFlow<FirestoreEmployerJobUiState> = _uiState.asStateFlow()
    
    // CRITICAL FIX: Always get fresh currentUser to handle role switches
    // Don't cache at initialization - role switches can invalidate cached user
    private val currentUser get() = FirebaseAuth.getInstance().currentUser
    
    /**
     * P1 FIX: Get cached employer profile
     * Returns cached profile data with 5-minute TTL
     */
    suspend fun getCachedProfile(): EmployerProfileCache.CachedProfile? {
        val employerId = currentUser?.uid ?: return null
        return employerProfileCache.getProfile(employerId)
    }
    
    /**
     * P2 FIX: Get saved job draft.
     *
     * Bug #4 fix: A guest can fill the form and then sign in; previously
     * `currentUser?.uid ?: return null` blocked restore on the very first
     * authenticated composition. We now return the device-scoped draft and
     * let the screen decide whether to apply it.
     */
    suspend fun getSavedDraft(): JobDraftDataStore.JobDraft? {
        val employerId = currentUser?.uid.orEmpty()
        return jobDraftDataStore.getDraft(employerId)
    }
    
    /**
     * P2 FIX: Save job draft.
     *
     * Bug #4 fix: Persist guest drafts under a stable "GUEST" bucket so the
     * data survives the register → OTP round-trip.
     */
    fun saveDraft(draft: JobDraftDataStore.JobDraft) {
        viewModelScope.launch {
            val employerId = currentUser?.uid ?: "GUEST"
            jobDraftDataStore.saveDraft(draft.copy(employerId = employerId))
        }
    }
    
    /**
     * P2 FIX: Clear job draft after successful post
     */
    fun clearDraft() {
        viewModelScope.launch {
            jobDraftDataStore.clearDraft()
        }
    }

    /**
     * Bug #4 fix: pull `applicationCount` from the dedicated
     * `employer_job_cards/{jobId}` collection (CF-maintained denormalized
     * snapshot per posted job) so the employer's job-list cards can
     * display the live count without joining against `applications`.
     *
     * Failures are silently logged — the cards fall back to 0 which is
     * preferable to crashing the dashboard if rules / network drop.
     */
    private fun loadApplicationCounts(jobIds: List<String>) {
        if (jobIds.isEmpty()) return
        viewModelScope.launch {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val counts = mutableMapOf<String, Int>()
                jobIds.chunked(10).forEach { chunk ->
                    val snap = firestore.collection("employer_job_cards")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    snap.documents.forEach { doc ->
                        val n = (doc.getLong("applicationCount") ?: 0L).toInt()
                        counts[doc.id] = n
                    }
                }
                _uiState.value = _uiState.value.copy(applicationCountsByJobId = counts)
            } catch (e: Exception) {
                Timber.w(e, "loadApplicationCounts failed; cards will show 0")
            }
        }
    }

    fun loadMyJobs() {
        viewModelScope.launch {
            com.example.dutype.performance.MainThreadChecker.assertMainThread()
            performanceTracker.trackOperation("loadMyJobs")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasError = false)
            
            try {
                val employerId = currentUser?.uid
                if (employerId == null) {
                    Timber.e("Employer ID is null - user not authenticated")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasError = true,
                        error = "User not authenticated"
                    )
                    return@launch
                }
                
                Timber.d("Loading jobs for employer: %s", employerId)
                firestoreJobRepository.getJobsByEmployer(employerId).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            Timber.i("Successfully loaded %d jobs for employer", jobs.size)
                            _uiState.value = _uiState.value.copy(
                                myJobs = jobs,
                                isLoading = false
                            )
                            // Bug #4 fix: hydrate per-job application counts
                            // from the dedicated employer_job_cards collection.
                            loadApplicationCounts(jobs.map { it.id })
                        },
                        onFailure = { exception ->
                            Timber.e(exception, "Failed to load employer jobs")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                hasError = true,
                                error = exception.message ?: "Failed to load your jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception loading employer jobs")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasError = true,
                    error = e.message ?: "Failed to load your jobs"
                )
            }
        }
    }
    
    fun refreshMyJobs() {
        viewModelScope.launch {
            com.example.dutype.performance.MainThreadChecker.assertMainThread()
            performanceTracker.trackOperation("refreshMyJobs")
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null, hasError = false)
            
            try {
                val employerId = currentUser?.uid
                if (employerId == null) {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        hasError = true,
                        error = "User not authenticated"
                    )
                    return@launch
                }
                
                firestoreJobRepository.getJobsByEmployer(employerId).collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            _uiState.value = _uiState.value.copy(
                                myJobs = jobs,
                                isRefreshing = false
                            )
                            loadApplicationCounts(jobs.map { it.id })
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                isRefreshing = false,
                                hasError = true,
                                error = exception.message ?: "Failed to refresh your jobs"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    hasError = true,
                    error = e.message ?: "Failed to refresh your jobs"
                )
            }
        }
    }
    
    fun createJob(jobData: Map<String, Any>, callback: (Boolean, String?, String?) -> Unit) {
        viewModelScope.launch {
            com.example.dutype.performance.MainThreadChecker.assertMainThread()
            performanceTracker.trackOperation("createJob")
            _uiState.value = _uiState.value.copy(isCreatingJob = true, error = null, hasError = false)
            
            Timber.d("📝 VIEWMODEL DEBUG: createJob() called")
            
            try {
                val employerId = currentUser?.uid
                if (employerId == null) {
                    Timber.e("📝 VIEWMODEL DEBUG: ❌ Employer ID is null - user not authenticated")
                    _uiState.value = _uiState.value.copy(
                        isCreatingJob = false,
                        hasError = true,
                        error = "User not authenticated"
                    )
                    callback(false, null, "User not authenticated")
                    return@launch
                }
                
                Timber.d("📝 VIEWMODEL DEBUG: Employer ID: $employerId")
                
                // Add employer ID to job data
                val jobDataWithEmployer = jobData.toMutableMap()
                jobDataWithEmployer["employerId"] = employerId
                
                firestoreJobRepository.createJob(jobDataWithEmployer).collect { result ->
                    result.fold(
                        onSuccess = { jobId ->
                            Timber.i("📝 VIEWMODEL DEBUG: ✅ Job created successfully with ID: $jobId")
                            _uiState.value = _uiState.value.copy(
                                isCreatingJob = false
                            )
                            
                            // P2 FIX: Clear draft on successful post
                            clearDraft()
                            
                            // REMOVED: Notification sending moved to FirestoreJobRepository.createJob()
                            // to prevent duplicate notifications from multiple code paths
                            
                            // Real-time listener will automatically update the jobs list
                            // No need to call loadMyJobs() - it causes duplicate loads
                            callback(true, jobId, null)
                        },
                        onFailure = { exception ->
                            Timber.e(exception, "📝 VIEWMODEL DEBUG: ❌ Failed to create job")
                            _uiState.value = _uiState.value.copy(
                                isCreatingJob = false,
                                hasError = true,
                                error = exception.message ?: "Failed to create job"
                            )
                            callback(false, null, exception.message)
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "📝 VIEWMODEL DEBUG: ❌ Exception creating job")
                _uiState.value = _uiState.value.copy(
                    isCreatingJob = false,
                    hasError = true,
                    error = e.message ?: "Failed to create job"
                )
                callback(false, null, e.message)
            }
        }
    }
    
    /**
     * P0 FIX: Create job with offline support
     * Queues job for background submission if offline
     * 
     * @param jobData Job data to submit
     * @param idempotencyKey Unique key to prevent duplicate submissions
     * @param callback Callback with success status and message
     */
    fun createJobWithOfflineSupport(
        jobData: Map<String, Any>,
        idempotencyKey: String,
        callback: (Boolean, String?, Boolean) -> Unit // success, message, isQueued
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingJob = true, error = null, hasError = false)
            
            Timber.d("📝 OFFLINE_POSTING: createJobWithOfflineSupport() called")
            
            try {
                val employerId = currentUser?.uid
                if (employerId == null) {
                    _uiState.value = _uiState.value.copy(
                        isCreatingJob = false,
                        hasError = true,
                        error = "User not authenticated"
                    )
                    callback(false, "User not authenticated", false)
                    return@launch
                }
                
                // Add employer ID and idempotency key to job data
                val jobDataWithEmployer = jobData.toMutableMap()
                jobDataWithEmployer["employerId"] = employerId
                jobDataWithEmployer["idempotencyKey"] = idempotencyKey
                
                // Try to submit directly first
                var submitted = false
                var submitError: String? = null
                
                try {
                    firestoreJobRepository.createJob(jobDataWithEmployer).collect { result ->
                        result.fold(
                            onSuccess = { jobId ->
                                Timber.i("📝 OFFLINE_POSTING: ✅ Job created directly with ID: $jobId")
                                submitted = true
                                
                                // Clear draft on success
                                clearDraft()
                                
                                // REMOVED: Notification sending moved to FirestoreJobRepository.createJob()
                                // to prevent duplicate notifications from multiple code paths
                                
                            },
                            onFailure = { exception ->
                                submitError = exception.message
                                Timber.w(exception, "📝 OFFLINE_POSTING: Direct submission failed")
                            }
                        )
                    }
                } catch (e: Exception) {
                    submitError = e.message
                    Timber.w(e, "📝 OFFLINE_POSTING: Exception during direct submission")
                }
                
                if (submitted) {
                    _uiState.value = _uiState.value.copy(isCreatingJob = false)
                    callback(true, null, false)
                } else {
                    // Queue for background submission
                    Timber.d("📝 OFFLINE_POSTING: Queuing job for background submission")
                    JobPostingWorker.enqueue(
                        context = context,
                        jobData = jobDataWithEmployer,
                        employerId = employerId,
                        idempotencyKey = idempotencyKey
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isCreatingJob = false,
                        isQueuedOffline = true,
                        pendingJobsCount = _uiState.value.pendingJobsCount + 1
                    )
                    
                    callback(true, "Job queued for posting when online", true)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "📝 OFFLINE_POSTING: ❌ Exception in createJobWithOfflineSupport")
                _uiState.value = _uiState.value.copy(
                    isCreatingJob = false,
                    hasError = true,
                    error = e.message ?: "Failed to create job"
                )
                callback(false, e.message, false)
            }
        }
    }
    
    fun updateJob(jobId: String, updates: Map<String, Any>, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingJob = true, error = null, hasError = false)
            
            try {
                firestoreJobRepository.updateJob(jobId, updates).collect { result ->
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                isUpdatingJob = false
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
                }
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
                firestoreJobRepository.deleteJob(jobId).collect { result ->
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
                }
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
                firestoreJobRepository.getJobById(jobId).collect { result ->
                    result.fold(
                        onSuccess = { job ->
                            callback(job)
                        },
                        onFailure = { exception ->
                            callback(null)
                        }
                    )
                }
            } catch (e: Exception) {
                callback(null)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, hasError = false)
    }
    
    /**
     * Update employer profile information
     * Migrated from EmployerViewModel for consolidation
     */
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
        phoneNumber: String,
        callback: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val employerId = currentUser?.uid
                if (employerId == null) {
                    Timber.e("Cannot update employer - user not authenticated")
                    callback(false, "User not authenticated")
                    return@launch
                }
                
                val updates = mapOf(
                    "fullName" to name,
                    "phone" to phoneNumber
                )
                
                // Update employer profile in Firestore
                firestoreJobRepository.updateEmployerProfile(employerId, updates).collect { result ->
                    result.fold(
                        onSuccess = {
                            Timber.i("Successfully updated employer profile")
                            callback(true, null)
                        },
                        onFailure = { exception ->
                            Timber.e(exception, "Failed to update employer profile")
                            callback(false, exception.message)
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception updating employer profile")
                callback(false, e.message)
            }
        }
    }
}
