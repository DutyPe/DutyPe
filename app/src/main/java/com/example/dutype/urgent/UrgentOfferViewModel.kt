package com.example.dutype.urgent

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.InstantRequest
import com.example.dutype.services.InstantHelpService
import com.example.dutype.utils.GeoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State of the full-screen urgent offer page. */
sealed interface UrgentOfferState {
    data object Loading : UrgentOfferState
    /** The offer is open: Accept / Skip. */
    data class Offer(val request: InstantRequest, val distanceKm: Double?, val accepting: Boolean = false) : UrgentOfferState
    /** The job is this worker's: address, call and directions. */
    data class Accepted(val request: InstantRequest?, val result: UrgentOffers.Result.Accepted) : UrgentOfferState
    data class Unavailable(val reason: UrgentOffers.Result) : UrgentOfferState
    data class Failed(val message: String) : UrgentOfferState
}

@HiltViewModel
class UrgentOfferViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val service: InstantHelpService,
    private val locationPreferences: LocationPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<UrgentOfferState>(UrgentOfferState.Loading)
    val state: StateFlow<UrgentOfferState> = _state.asStateFlow()
    private var requestId: String = ""

    fun load(id: String) {
        if (id == requestId && _state.value !is UrgentOfferState.Failed) return
        requestId = id
        // The page replaces the ringing notification.
        NotificationManagerCompat.from(context).cancel(UrgentOffers.notificationId(id))
        _state.value = UrgentOfferState.Loading
        viewModelScope.launch {
            val request = service.getRequestForOffer(id).getOrElse {
                _state.value = UrgentOfferState.Failed(it.message ?: "Could not load this job")
                return@launch
            }
            if (request == null) {
                _state.value = UrgentOfferState.Unavailable(UrgentOffers.Result.Closed)
                return@launch
            }
            // Already accepted (e.g. from the notification): show the job, not the offer.
            val mine = service.myResponseStatus(id)
            if (mine == "accepted" || mine == "completed") {
                accept(request)
                return@launch
            }
            if (mine == "rejected" || mine == "no_show") {
                _state.value = UrgentOfferState.Unavailable(UrgentOffers.Result.Removed)
                return@launch
            }
            val expired = request.expiresAt in 1 until System.currentTimeMillis()
            _state.value = when {
                request.status == "filled" -> UrgentOfferState.Unavailable(UrgentOffers.Result.Filled)
                request.status != "open" || expired -> UrgentOfferState.Unavailable(UrgentOffers.Result.Closed)
                else -> UrgentOfferState.Offer(request, distanceTo(request))
            }
        }
    }

    fun accept() {
        val offer = _state.value as? UrgentOfferState.Offer ?: return
        if (offer.accepting) return
        _state.value = offer.copy(accepting = true)
        viewModelScope.launch { accept(offer.request) }
    }

    private suspend fun accept(request: InstantRequest) {
        service.acceptOffer(requestId).fold(
            onSuccess = { result ->
                _state.value = when (result) {
                    is UrgentOffers.Result.Accepted -> UrgentOfferState.Accepted(request, result)
                    else -> UrgentOfferState.Unavailable(result)
                }
            },
            onFailure = {
                _state.value = UrgentOfferState.Offer(request, distanceTo(request))
                _state.value = UrgentOfferState.Failed(it.message ?: "Could not accept. Check your internet and try again.")
            }
        )
    }

    private fun distanceTo(request: InstantRequest): Double? {
        val here = locationPreferences.getSavedLocation()?.takeIf { it.hasValidCoordinates() } ?: return null
        if (!GeoUtils.hasValidCoordinates(request.lat, request.lng)) return null
        return GeoUtils.calculateDistance(here.latitude, here.longitude, request.lat, request.lng)
    }
}
