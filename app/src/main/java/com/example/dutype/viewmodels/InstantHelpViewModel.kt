package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.InstantHelpDefaults
import com.example.dutype.models.InstantRequest
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
    val isLoadingAvailability: Boolean = false,
    val isSavingAvailability: Boolean = false,
    val isLoadingRequests: Boolean = false,
    val updatingRequestId: String? = null,
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
                    _uiState.update {
                        it.copy(
                            instantRequests = requests,
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

    fun setWorkerRadius(radiusKm: Double, currentLocation: LocationData?) {
        val current = _uiState.value.workerAvailability
        val updated = current.copy(radiusKm = radiusKm)
        _uiState.update { it.copy(workerAvailability = updated) }
        if (updated.isAvailable) {
            saveWorkerAvailability(updated, currentLocation, reloadRequests = true)
        }
    }

    fun toggleWorkerCategory(category: String, currentLocation: LocationData?) {
        val current = _uiState.value.workerAvailability
        val updatedCategories = if (category in current.categories) {
            current.categories.filterNot { it == category }.ifEmpty { InstantHelpDefaults.defaultWorkerCategories }
        } else {
            (current.categories + category).distinct()
        }
        val updated = current.copy(categories = updatedCategories)
        _uiState.update { it.copy(workerAvailability = updated) }
        if (updated.isAvailable) {
            saveWorkerAvailability(updated, currentLocation, reloadRequests = true)
        }
    }

    fun respondToInstantRequest(request: InstantRequest, action: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingRequestId = request.requestId, error = null) }
            instantHelpService.respondToInstantRequest(request, action).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            updatingRequestId = null,
                            instantRequests = state.instantRequests.filterNot { it.requestId == request.requestId },
                            message = when (action.lowercase()) {
                                "called" -> "Call recorded"
                                "busy" -> "Marked busy"
                                else -> "Interest sent"
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

    fun clearInstantHelpMessage() {
        _uiState.update { it.copy(error = null, message = null) }
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
                categories = availability.categories,
                radiusKm = availability.radiusKm,
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