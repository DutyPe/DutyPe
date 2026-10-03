package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.InstantRequest
import com.example.dutype.models.InstantResponse
import com.example.dutype.models.LocationData
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.models.WorkerAvailability
import com.example.dutype.profile.CurrentProfileStore
import com.example.dutype.services.InstantHelpService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstantHelpUiState(
    val workerAvailability: WorkerAvailability = WorkerAvailability(isAvailable = true),
    val instantRequests: List<InstantRequest> = emptyList(),
    val workerInstantResponses: List<InstantResponse> = emptyList(),
    val employerInstantRequests: List<InstantRequest> = emptyList(),
    val employerInstantResponses: Map<String, List<InstantResponse>> = emptyMap(),
    val isLoadingAvailability: Boolean = false,
    val isSavingAvailability: Boolean = false,
    val isLoadingRequests: Boolean = false,
    val isLoadingWorkerUrgentHistory: Boolean = false,
    val isLoadingEmployerUrgentNeeds: Boolean = false,
    val updatingRequestId: String? = null,
    val updatingEmployerRequestId: String? = null,
    val updatingEmployerResponseId: String? = null,
    val isPostingUrgentNeed: Boolean = false,
    val postedRequestId: String? = null,
    val error: String? = null,
    val message: String? = null
)

/** Instant help for both roles; all writes are Cloud Functions, state is refreshed from the server after each. */
@HiltViewModel
class InstantHelpViewModel @Inject constructor(
    private val instantHelpService: InstantHelpService,
    private val profileStore: CurrentProfileStore,
    private val profileCompletionService: com.example.dutype.services.ProfileCompletionService,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow(InstantHelpUiState())
    val uiState: StateFlow<InstantHelpUiState> = _uiState.asStateFlow()

    private var workerRefreshJob: Job? = null

    // ─────────────────────────────── worker ───────────────────────────────

    fun loadWorkerInstantHelp(currentLocation: LocationData?) = refreshWorkerInstantRequests(currentLocation)

    fun refreshWorkerInstantRequests(currentLocation: LocationData?) {
        workerRefreshJob?.cancel()
        workerRefreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRequests = true, error = null) }
            instantHelpService.getOpenInstantRequestsForWorker(currentLocation).fold(
                onSuccess = { list -> _uiState.update { it.copy(instantRequests = list, isLoadingRequests = false) } },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoadingRequests = false, error = e.message ?: "Failed to load urgent requests") }
                }
            )
        }
    }

    fun respondToInstantRequest(request: InstantRequest, action: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingRequestId = request.requestId, error = null) }
            instantHelpService.respondToInstantRequest(request, action).fold(
                onSuccess = {
                    val status = if (action == "called") "called" else "applied"
                    _uiState.update { state ->
                        state.copy(
                            updatingRequestId = null,
                            instantRequests = state.instantRequests.map { item ->
                                if (item.requestId != request.requestId) item
                                else item.copy(workerResponseStatus = status, workerRespondedAt = System.currentTimeMillis())
                            },
                            message = if (action == "called") "Contact recorded" else "Applied for urgent work"
                        )
                    }
                    onSuccess()
                },
                onFailure = { e -> _uiState.update { it.copy(updatingRequestId = null, error = e.message ?: "Failed to update request") } }
            )
        }
    }

    fun loadWorkerUrgentHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWorkerUrgentHistory = true, error = null) }
            instantHelpService.getWorkerInstantResponses().fold(
                onSuccess = { list -> _uiState.update { it.copy(workerInstantResponses = list, isLoadingWorkerUrgentHistory = false) } },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoadingWorkerUrgentHistory = false, error = e.message ?: "Failed to load urgent work history") }
                }
            )
        }
    }

    // ─────────────────────────────── employer ───────────────────────────────

    fun createUrgentNeed(input: QuickUrgentNeedInput, onPosted: (String) -> Unit = {}) {
        if (_uiState.value.isPostingUrgentNeed) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingUrgentNeed = true, postedRequestId = null, error = null) }
            val employer = profileStore.employer.value
                ?: auth.currentUser?.uid?.let { profileCompletionService.getEmployer(it).getOrNull() }
            instantHelpService.createUrgentNeed(input, employer).fold(
                onSuccess = { id ->
                    _uiState.update { it.copy(isPostingUrgentNeed = false, postedRequestId = id, message = "Urgent need posted") }
                    onPosted(id)
                    loadEmployerUrgentNeeds()
                },
                onFailure = { e -> _uiState.update { it.copy(isPostingUrgentNeed = false, error = e.message ?: "Failed to post urgent need") } }
            )
        }
    }

    fun loadEmployerUrgentNeeds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEmployerUrgentNeeds = true, error = null) }
            val requests = instantHelpService.getEmployerInstantRequests().getOrElse { e ->
                _uiState.update { it.copy(isLoadingEmployerUrgentNeeds = false, error = e.message ?: "Failed to load urgent needs") }
                return@launch
            }
            val responses = instantHelpService.getEmployerInstantResponses(requests).getOrDefault(emptyList())
                .groupBy { it.requestId }
            val withCompletion = requests.map { request ->
                request.copy(
                    completedWorkerIds = responses[request.requestId].orEmpty()
                        .filter { it.status == "completed" }.map { it.workerId }
                )
            }
            _uiState.update {
                it.copy(
                    employerInstantRequests = withCompletion,
                    employerInstantResponses = responses,
                    isLoadingEmployerUrgentNeeds = false
                )
            }
        }
    }

    fun acceptEmployerInstantResponse(response: InstantResponse) = updateResponse(response, "accepted", "Worker selected")

    @Suppress("UNUSED_PARAMETER")
    fun completeEmployerInstantResponse(response: InstantResponse, completionProof: String = "") =
        updateResponse(response, "completed", "Urgent work marked done")

    @Suppress("UNUSED_PARAMETER")
    fun markEmployerInstantResponseNoShow(response: InstantResponse, reason: String = "") =
        updateResponse(response, "no_show", "Worker did not come")

    fun markEmployerInstantRequestFilled(request: InstantRequest) = updateRequest(request.requestId, "Urgent job marked filled") {
        instantHelpService.markEmployerInstantRequestFilled(request)
    }

    @Suppress("UNUSED_PARAMETER")
    fun cancelEmployerInstantRequest(request: InstantRequest, reason: String = "") =
        updateRequest(request.requestId, "Urgent request cancelled") { instantHelpService.cancelEmployerInstantRequest(request) }

    fun deleteEmployerInstantRequest(requestId: String, onComplete: (Boolean) -> Unit) {
        updateRequest(requestId, "Urgent request deleted", onComplete) { instantHelpService.deleteEmployerInstantRequest(requestId) }
    }

    /** Looks up the phone of a worker who responded, then hands it to [onPhone] (e.g. the dialer). */
    fun callWorker(response: InstantResponse, onPhone: (String) -> Unit) {
        viewModelScope.launch {
            instantHelpService.workerPhone(response).fold(
                onSuccess = onPhone,
                onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Phone number unavailable") } }
            )
        }
    }

    fun clearInstantHelpMessage() {
        _uiState.update { it.copy(error = null, message = null) }
    }

    /** Take an accepted worker off the job (e.g. a friend of the other worker is coming instead). */
    fun removeEmployerUrgentWorker(response: InstantResponse, reason: String) =
        updateResponse(response, "rejected", "Worker removed. The place is open again.", reason.trim())

    private fun updateResponse(response: InstantResponse, status: String, doneMessage: String, reason: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingEmployerResponseId = response.responseId, error = null) }
            instantHelpService.updateEmployerInstantResponseStatus(response, status, reason).fold(
                onSuccess = {
                    _uiState.update { it.copy(updatingEmployerResponseId = null, message = doneMessage) }
                    loadEmployerUrgentNeeds()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(updatingEmployerResponseId = null, error = e.message ?: "Failed to update urgent response") }
                }
            )
        }
    }

    private fun updateRequest(
        requestId: String,
        doneMessage: String,
        onComplete: (Boolean) -> Unit = {},
        action: suspend () -> Result<Unit>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingEmployerRequestId = requestId, error = null) }
            action().fold(
                onSuccess = {
                    _uiState.update { it.copy(updatingEmployerRequestId = null, message = doneMessage) }
                    onComplete(true)
                    loadEmployerUrgentNeeds()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(updatingEmployerRequestId = null, error = e.message ?: "Failed to update urgent request") }
                    onComplete(false)
                }
            )
        }
    }
}
