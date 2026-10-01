package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.applications.ApplicationRepository
import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.example.dutype.profile.CurrentProfileStore
import com.example.dutype.models.ApplicationStats
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.MatchedWorker
import com.example.dutype.services.WorkerMatchingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

data class EmployerApplicationUiState(
    val applications: List<JobApplication> = emptyList(),
    /** Unfiltered list; [applications] is this after the status filter / search. */
    val allApplications: List<JobApplication> = emptyList(),
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedStatusFilter: ApplicationStatus? = null,
    /** Worker ids whose phone this employer has revealed (employer_profiles/{uid}/unlocks). */
    val unlockedWorkerIds: Set<String> = emptySet(),
    /** Phones revealed in this session, by worker id. */
    val phones: Map<String, String> = emptyMap()
)

data class MatchedWorkersUiState(
    val workers: List<MatchedWorker> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val loadedJobId: String? = null,
    val actionError: String? = null
)

/**
 * The employer's applicants: live list (worker name/photo/skill come with each application;
 * rating, experience and distance from the workers' public cards, 30 per query),
 * status changes and phone reveals through Cloud Functions.
 */
@HiltViewModel
class EmployerApplicationViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    private val jobRepository: com.example.dutype.jobs.JobRepository,
    private val workerMatchingService: WorkerMatchingService,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
    val profileStore: CurrentProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployerApplicationUiState())
    val uiState: StateFlow<EmployerApplicationUiState> = _uiState.asStateFlow()

    private val _stats = MutableStateFlow(ApplicationStats())
    val stats: StateFlow<ApplicationStats> = _stats.asStateFlow()

    private val _matchedWorkersState = MutableStateFlow(MatchedWorkersUiState())
    val matchedWorkersState: StateFlow<MatchedWorkersUiState> = _matchedWorkersState.asStateFlow()

    private var listenJob: Job? = null
    private var listening: String? = null

    init {
        loadUnlocks()
    }

    /** Every applicant across the employer's jobs (live). */
    fun loadEmployerApplications() {
        val uid = auth.currentUser?.uid ?: return fail("Employer not authenticated")
        listenTo("all", repository.employerApplications(uid))
    }

    /** Applicants for one job (live). */
    fun loadJobApplications(jobId: String) {
        val uid = auth.currentUser?.uid ?: return fail("Employer not authenticated")
        listenTo("job:$jobId", repository.jobApplicants(uid, jobId))
    }

    fun refresh() {
        listening = null
        loadEmployerApplications()
    }

    fun loadMatchedWorkers(jobId: String, force: Boolean = false) {
        val current = _matchedWorkersState.value
        if (!force && current.loadedJobId == jobId && current.workers.isNotEmpty()) return
        _matchedWorkersState.value = current.copy(isLoading = true, hasError = false, error = null, loadedJobId = jobId)
        viewModelScope.launch {
            workerMatchingService.getMatchedWorkersForJob(jobId).collect { result ->
                _matchedWorkersState.value = result.fold(
                    onSuccess = { MatchedWorkersUiState(workers = it, loadedJobId = jobId) },
                    onFailure = { MatchedWorkersUiState(hasError = true, error = it.message, loadedJobId = jobId) }
                )
            }
        }
    }

    fun updateApplicationStatus(applicationId: String, newStatus: ApplicationStatus, notes: String? = null) {
        viewModelScope.launch { updateApplicationStatusForResult(applicationId, newStatus) }
    }

    suspend fun updateApplicationStatusForResult(applicationId: String, newStatus: ApplicationStatus): Result<Unit> {
        _uiState.update { it.copy(isUpdating = true) }
        val result = repository.setStatus(applicationId, newStatus)
        _uiState.update {
            it.copy(isUpdating = false, hasError = result.isFailure, error = result.exceptionOrNull()?.message)
        }
        return result
    }

    fun filterApplicationsByStatus(status: ApplicationStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
        applyFilters()
    }

    fun searchApplications(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    suspend fun getApplication(applicationId: String): JobApplication? = repository.getApplication(applicationId).getOrNull()

    fun clearError() = _uiState.update { it.copy(hasError = false, error = null) }

    /** Vacancies left for a job = its vacancies minus hired/completed applicants. */
    fun canHireMoreApplicants(vacancies: Int, onResult: (Boolean, Int) -> Unit) {
        val hired = _uiState.value.allApplications.count {
            it.status == ApplicationStatus.HIRED || it.status == ApplicationStatus.COMPLETED
        }
        val remaining = (vacancies - hired).coerceAtLeast(0)
        onResult(remaining > 0, remaining)
    }

    fun isContactUnlocked(workerId: String): Boolean = workerId in _uiState.value.unlockedWorkerIds

    /** Reveals the worker's phone (recorded server-side as an unlock). */
    fun fetchPhoneNumberForWorker(jobId: String, workerId: String, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                @Suppress("UNCHECKED_CAST")
                val data = functions.getHttpsCallable("getWorkerContact")
                    .call(mapOf("jobId" to jobId, "workerId" to workerId)).await().data as? Map<String, Any?>
                data?.get("phone") as? String ?: ""
            }.onSuccess { phone ->
                if (phone.isBlank()) return@onSuccess onFailure()
                _uiState.update { it.copy(unlockedWorkerIds = it.unlockedWorkerIds + workerId, phones = it.phones + (workerId to phone)) }
                applyFilters()
                _matchedWorkersState.update { state ->
                    state.copy(workers = state.workers.map { if (it.workerId == workerId) it.copy(phone = phone) else it })
                }
                onSuccess(phone)
            }.onFailure {
                Timber.w(it, "getWorkerContact failed")
                onFailure()
            }
        }
    }

    fun unlockContact(application: JobApplication, onSuccess: (String) -> Unit, onFailure: () -> Unit) =
        fetchPhoneNumberForWorker(application.jobId, application.workerId, onSuccess, onFailure)

    // ─────────────────────────────── internals ───────────────────────────────

    private fun listenTo(key: String, source: Flow<Result<List<JobApplication>>>) {
        if (listening == key && listenJob?.isActive == true) return
        listening = key
        listenJob?.cancel()
        _uiState.update { it.copy(isLoading = it.allApplications.isEmpty(), hasError = false) }
        listenJob = viewModelScope.launch {
            source.collect { result ->
                result.onSuccess { list -> publish(withWorkerCards(list)) }
                    .onFailure { error -> fail(error.message ?: "Failed to load applications") }
            }
        }
    }

    /** Adds each applicant's public card and distance from the job (both cached). */
    private suspend fun withWorkerCards(list: List<JobApplication>): List<JobApplication> {
        val cards = repository.workerCards(list.map { it.workerId }).getOrDefault(emptyMap())
        val jobs = jobRepository.getCards(list.map { it.jobId }).getOrDefault(emptyList()).associateBy { it.id }
        return list.map { app ->
            val job = jobs[app.jobId]
            val worker = cards[app.workerId]?.let { card ->
                if (job != null && card.lat != 0.0 && card.lng != 0.0) {
                    card.copy(distanceKm = com.example.dutype.jobs.Geohash.distanceKm(job.lat, job.lng, card.lat, card.lng))
                } else card
            }
            app.copy(worker = worker ?: app.worker, job = job)
        }
    }

    private fun publish(list: List<JobApplication>) {
        _uiState.update { it.copy(allApplications = list, isLoading = false, hasError = false, error = null) }
        applyFilters()
        _stats.value = ApplicationStats(
            totalApplications = list.size,
            appliedApplications = list.count { it.status == ApplicationStatus.APPLIED },
            rejectedApplications = list.count { it.status == ApplicationStatus.REJECTED },
            hiredApplications = list.count { it.status == ApplicationStatus.HIRED },
            recentApplications = list.take(5)
        )
    }

    private fun applyFilters() = _uiState.update { state ->
        val query = state.searchQuery.trim()
        state.copy(applications = state.allApplications
            .map { app -> state.phones[app.workerId]?.let { app.copy(workerPhone = it) } ?: app }
            .filter { app ->
                (state.selectedStatusFilter == null || app.status == state.selectedStatusFilter) &&
                    (query.isEmpty() || app.workerName.contains(query, true) || app.workerSkill.contains(query, true))
            })
    }

    private fun loadUnlocks() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                firestore.collection(EmployerProfiles.COLLECTION).document(uid)
                    .collection(EmployerProfiles.Unlocks.COLLECTION).limit(UNLOCKS_LIMIT).get().await()
                    .documents.map { it.id }.toSet()
            }.onSuccess { ids -> _uiState.update { it.copy(unlockedWorkerIds = it.unlockedWorkerIds + ids) } }
        }
    }

    private fun fail(message: String) = _uiState.update { it.copy(isLoading = false, hasError = true, error = message) }

    private companion object {
        const val UNLOCKS_LIMIT = 500L
    }
}
