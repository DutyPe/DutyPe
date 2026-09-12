package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.InstantRequest
import com.example.dutype.models.InstantResponse
import com.example.dutype.models.LocationData
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.models.WorkerAvailability
import com.example.dutype.services.InstantHelpService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstantHelpUiState(
    val workerAvailability: WorkerAvailability = WorkerAvailability(),
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

@HiltViewModel
class InstantHelpViewModel @Inject constructor(
    private val instantHelpService: InstantHelpService
) : ViewModel() {
    private val _uiState = MutableStateFlow(InstantHelpUiState())
    val uiState: StateFlow<InstantHelpUiState> = _uiState.asStateFlow()

    fun loadWorkerInstantHelp(currentLocation: LocationData?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAvailability = true, error = null) }
            instantHelpService.getWorkerAvailability().fold(
                onSuccess = { savedAvailability ->
                    val availability = savedAvailability ?: WorkerAvailability()
                    _uiState.update {
                        it.copy(
                            workerAvailability = availability,
                            isLoadingAvailability = false
                        )
                    }
                    if (availability.isAvailable) {
                        refreshWorkerInstantRequests(currentLocation)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingAvailability = false,
                            error = error.message ?: "Failed to load availability"
                        )
                    }
                }
            )
        }
    }

    fun refreshWorkerInstantRequests(currentLocation: LocationData?) {
        val availability = _uiState.value.workerAvailability
        if (!availability.isAvailable) {
            _uiState.update { it.copy(instantRequests = emptyList(), isLoadingRequests = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRequests = true, error = null) }
            instantHelpService.getOpenInstantRequestsForWorker(availability, currentLocation).fold(
                onSuccess = { requests ->
                    val responseByRequestId = instantHelpService
                        .getWorkerInstantResponsesForRequests(requests.map { request -> request.requestId })
                        .getOrDefault(emptyMap())
                    val requestsWithWorkerState = requests.map { request ->
                        responseByRequestId[request.requestId]?.let { response ->
                            request.copy(
                                workerResponseId = response.responseId,
                                workerResponseStatus = response.status,
                                workerRespondedAt = response.respondedAt
                            )
                        } ?: request
                    }
                    _uiState.update {
                        it.copy(
                            instantRequests = requestsWithWorkerState,
                            workerInstantResponses = mergeWorkerInstantResponses(
                                it.workerInstantResponses,
                                responseByRequestId.values
                            ),
                            isLoadingRequests = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingRequests = false,
                            error = error.message ?: "Failed to load urgent requests"
                        )
                    }
                }
            )
        }
    }

    fun setWorkerAvailability(isAvailable: Boolean, currentLocation: LocationData?) {
        val current = _uiState.value.workerAvailability
        saveWorkerAvailability(
            availability = current.copy(
                isAvailable = isAvailable,
                status = if (isAvailable) "available" else "offline"
            ),
            currentLocation = currentLocation,
            reloadRequests = isAvailable
        )
    }

    fun respondToInstantRequest(request: InstantRequest, action: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingRequestId = request.requestId, error = null) }
            instantHelpService.respondToInstantRequest(request, action).fold(
                onSuccess = {
                    val normalizedStatus = normalizeWorkerInstantResponseStatus(action)
                    val now = System.currentTimeMillis()
                    _uiState.update { state ->
                        state.copy(
                            updatingRequestId = null,
                            instantRequests = state.instantRequests.map { item ->
                                if (item.requestId == request.requestId) {
                                    item.copy(
                                        workerResponseId = request.workerResponseId,
                                        workerResponseStatus = normalizedStatus,
                                        workerRespondedAt = now
                                    )
                                } else {
                                    item
                                }
                            },
                            message = when (action.lowercase()) {
                                "called" -> "Contact recorded"
                                else -> "Applied for urgent work"
                            }
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            updatingRequestId = null,
                            error = error.message ?: "Failed to update request"
                        )
                    }
                }
            )
        }
    }

    fun createUrgentNeed(input: QuickUrgentNeedInput, onPosted: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingUrgentNeed = true, postedRequestId = null, error = null) }
            instantHelpService.createUrgentNeed(input).fold(
                onSuccess = { requestId ->
                    _uiState.update {
                        it.copy(
                            isPostingUrgentNeed = false,
                            postedRequestId = requestId,
                            message = "Urgent need posted"
                        )
                    }
                    onPosted(requestId)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isPostingUrgentNeed = false,
                            error = error.message ?: "Failed to post urgent need"
                        )
                    }
                }
            )
        }
    }

    fun loadEmployerUrgentNeeds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEmployerUrgentNeeds = true, error = null) }

            val requestsResult = instantHelpService.getEmployerInstantRequests()
            val responsesResult = instantHelpService.getEmployerInstantResponses()

            val requests = requestsResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoadingEmployerUrgentNeeds = false,
                        error = error.message ?: "Failed to load urgent needs"
                    )
                }
                return@launch
            }
            val responses = responsesResult.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoadingEmployerUrgentNeeds = false,
                        error = error.message ?: "Failed to load urgent responses"
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    employerInstantRequests = requests,
                    employerInstantResponses = responses.groupBy { response -> response.requestId },
                    isLoadingEmployerUrgentNeeds = false,
                    error = null
                )
            }
        }
    }

    fun acceptEmployerInstantResponse(response: InstantResponse) {
        updateEmployerInstantResponse(response, "accepted")
    }

    fun completeEmployerInstantResponse(response: InstantResponse, completionProof: String = "") {
        updateEmployerInstantResponse(response, "completed", completionProof)
    }

    fun markEmployerInstantResponseNoShow(response: InstantResponse, reason: String = "") {
        updateEmployerInstantResponse(response, "no_show", reason)
    }

    fun markEmployerInstantRequestFilled(request: InstantRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingEmployerRequestId = request.requestId, error = null) }
            instantHelpService.markEmployerInstantRequestFilled(request).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            updatingEmployerRequestId = null,
                            employerInstantRequests = state.employerInstantRequests.map { item ->
                                if (item.requestId == request.requestId) {
                                    item.copy(status = "filled", failureReason = "")
                                } else {
                                    item
                                }
                            },
                            message = "Urgent job marked filled"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            updatingEmployerRequestId = null,
                            error = error.message ?: "Failed to mark urgent job filled"
                        )
                    }
                }
            )
        }
    }

    fun cancelEmployerInstantRequest(request: InstantRequest, reason: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingEmployerRequestId = request.requestId, error = null) }
            instantHelpService.cancelEmployerInstantRequest(request, reason).fold(
                onSuccess = {
                    val now = System.currentTimeMillis()
                    val safeReason = reason.trim().ifBlank { "Cancelled by employer" }
                    _uiState.update { state ->
                        state.copy(
                            updatingEmployerRequestId = null,
                            employerInstantRequests = state.employerInstantRequests.map { item ->
                                if (item.requestId == request.requestId) {
                                    item.copy(
                                        status = "cancelled",
                                        cancelledAt = now,
                                        cancellationReason = safeReason,
                                        failureReason = safeReason
                                    )
                                } else {
                                    item
                                }
                            },
                            employerInstantResponses = state.employerInstantResponses.mapValues { entry ->
                                if (entry.key == request.requestId) {
                                    entry.value.map { response ->
                                        response.copy(status = "cancelled", failureReason = safeReason, updatedAt = now)
                                    }
                                } else {
                                    entry.value
                                }
                            },
                            message = "Urgent request cancelled"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            updatingEmployerRequestId = null,
                            error = error.message ?: "Failed to cancel urgent request"
                        )
                    }
                }
            )
        }
    }

    fun deleteEmployerInstantRequest(requestId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingEmployerRequestId = requestId, error = null) }
            instantHelpService.deleteEmployerInstantRequest(requestId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            updatingEmployerRequestId = null,
                            employerInstantRequests = state.employerInstantRequests.filter { it.requestId != requestId },
                            message = "Urgent request deleted"
                        )
                    }
                    onComplete(true)
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            updatingEmployerRequestId = null,
                            error = error.message ?: "Failed to delete urgent request"
                        )
                    }
                    onComplete(false)
                }
            )
        }
    }

    fun loadWorkerUrgentHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWorkerUrgentHistory = true, error = null) }
            instantHelpService.getWorkerInstantResponses().fold(
                onSuccess = { responses ->
                    _uiState.update {
                        it.copy(
                            workerInstantResponses = responses,
                            isLoadingWorkerUrgentHistory = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingWorkerUrgentHistory = false,
                            error = error.message ?: "Failed to load urgent work history"
                        )
                    }
                }
            )
        }
    }

    fun clearInstantHelpMessage() {
        _uiState.update { it.copy(error = null, message = null) }
    }

    private fun normalizeWorkerInstantResponseStatus(action: String): String = when (action.lowercase()) {
        "called" -> "called"
        "applied" -> "applied"
        else -> "applied"
    }

    private fun mergeWorkerInstantResponses(
        current: List<InstantResponse>,
        updates: Collection<InstantResponse>
    ): List<InstantResponse> {
        if (updates.isEmpty()) return current
        val byId = current.associateBy { it.responseId }.toMutableMap()
        updates.forEach { response -> byId[response.responseId] = response }
        return byId.values.sortedByDescending { response ->
            response.updatedAt.takeIf { it > 0L } ?: response.createdAt
        }
    }

    private fun updateEmployerInstantResponse(response: InstantResponse, status: String, note: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingEmployerResponseId = response.responseId, error = null) }
            instantHelpService.updateEmployerInstantResponseStatus(response, status, note).fold(
                onSuccess = {
                    val now = System.currentTimeMillis()
                    val normalizedStatus = status.lowercase()
                    _uiState.update { state ->
                        val updatedResponses = state.employerInstantResponses.mapValues { entry ->
                            entry.value.map { item ->
                                if (item.responseId == response.responseId) {
                                    item.copy(
                                        status = normalizedStatus,
                                        updatedAt = now,
                                        acceptedAt = if (normalizedStatus == "accepted") now else item.acceptedAt,
                                        completedAt = if (normalizedStatus == "completed") now else item.completedAt,
                                        completionProof = if (normalizedStatus == "completed" && note.isNotBlank()) note else item.completionProof,
                                        failureReason = if (normalizedStatus in setOf("no_show", "rejected", "cancelled") && note.isNotBlank()) note else item.failureReason
                                    )
                                } else {
                                    item
                                }
                            }
                        }
                        state.copy(
                            updatingEmployerResponseId = null,
                            employerInstantResponses = updatedResponses,
                            employerInstantRequests = state.employerInstantRequests.map { request ->
                                if (request.requestId == response.requestId) {
                                    val requiredWorkers = request.workersNeeded.coerceAtLeast(1)
                                    val selectedAfter = (request.selectedWorkerIds + response.workerId).distinct()
                                    when (normalizedStatus) {
                                        "completed" -> {
                                            val completedAfter = (request.completedWorkerIds + response.workerId).distinct()
                                            request.copy(
                                                status = if (completedAfter.size >= requiredWorkers) "completed" else if (selectedAfter.size >= requiredWorkers || request.status.equals("filled", ignoreCase = true)) "filled" else "open",
                                                selectedWorkerId = response.workerId,
                                                selectedWorkerIds = selectedAfter,
                                                completedWorkerIds = completedAfter,
                                                completedAt = if (completedAfter.size >= requiredWorkers) now else request.completedAt,
                                                completionProof = if (note.isNotBlank()) note else request.completionProof
                                            )
                                        }
                                        "no_show" -> {
                                            val selectedAfterNoShow = request.selectedWorkerIds.filterNot { it == response.workerId }
                                            request.copy(
                                                status = if (selectedAfterNoShow.size >= requiredWorkers) "filled" else "open",
                                                selectedWorkerId = selectedAfterNoShow.lastOrNull().orEmpty(),
                                                selectedWorkerIds = selectedAfterNoShow,
                                                completedWorkerIds = request.completedWorkerIds.filterNot { it == response.workerId },
                                                failureReason = ""
                                            )
                                        }
                                        "accepted" -> request.copy(
                                            status = if (selectedAfter.size >= requiredWorkers || request.status.equals("filled", ignoreCase = true)) "filled" else "open",
                                            selectedWorkerId = response.workerId,
                                            selectedWorkerIds = selectedAfter
                                        )
                                        else -> request
                                    }
                                } else {
                                    request
                                }
                            },
                            message = when (normalizedStatus) {
                                "completed" -> "Urgent work marked done"
                                "no_show" -> "Worker did not come"
                                "accepted" -> "Worker selected. The urgent need closes when the required workers are selected."
                                else -> "Urgent response updated"
                            }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            updatingEmployerResponseId = null,
                            error = error.message ?: "Failed to update urgent response"
                        )
                    }
                }
            )
        }
    }

    private fun saveWorkerAvailability(
        availability: WorkerAvailability,
        currentLocation: LocationData?,
        reloadRequests: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAvailability = true, error = null) }
            instantHelpService.saveWorkerAvailability(
                isAvailable = availability.isAvailable,
                currentLocation = currentLocation
            ).fold(
                onSuccess = { saved ->
                    _uiState.update {
                        it.copy(
                            workerAvailability = saved,
                            isSavingAvailability = false,
                            message = if (saved.isAvailable) "You are available now" else "Availability turned off"
                        )
                    }
                    if (reloadRequests) {
                        refreshWorkerInstantRequests(currentLocation)
                    } else {
                        _uiState.update { it.copy(instantRequests = emptyList()) }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingAvailability = false,
                            error = error.message ?: "Failed to save availability"
                        )
                    }
                }
            )
        }
    }
}
