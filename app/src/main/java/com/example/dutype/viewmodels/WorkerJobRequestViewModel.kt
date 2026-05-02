package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.WorkerJobRequest
import com.example.dutype.services.WorkerMatchingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkerJobRequestUiState(
    val requests: List<WorkerJobRequest> = emptyList(),
    val isLoading: Boolean = false,
    val updatingRequestId: String? = null,
    val error: String? = null
)

@HiltViewModel
class WorkerJobRequestViewModel @Inject constructor(
    private val workerMatchingService: WorkerMatchingService
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkerJobRequestUiState())
    val uiState: StateFlow<WorkerJobRequestUiState> = _uiState.asStateFlow()

    fun loadPendingRequests() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            workerMatchingService.getPendingRequestsForCurrentWorker().collect { result ->
                result.fold(
                    onSuccess = { requests ->
                        _uiState.value = WorkerJobRequestUiState(requests = requests, isLoading = false)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(isLoading = false, error = error.message ?: "Failed to load job requests")
                        }
                    }
                )
            }
        }
    }

    fun acceptRequest(requestId: String, onAccepted: (String) -> Unit = {}) {
        respond(requestId, accept = true, onAccepted = onAccepted)
    }

    fun rejectRequest(requestId: String) {
        respond(requestId, accept = false)
    }

    private fun respond(requestId: String, accept: Boolean, onAccepted: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingRequestId = requestId, error = null) }
            val result = workerMatchingService.respondToWorkerJobRequest(requestId, accept)
            result.fold(
                onSuccess = { jobId ->
                    _uiState.update { state ->
                        state.copy(
                            updatingRequestId = null,
                            requests = state.requests.filterNot { it.requestId == requestId }
                        )
                    }
                    if (accept) onAccepted(jobId)
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
}