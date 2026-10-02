package com.example.dutype.homeservices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.repositories.LocationRepository
import com.google.firebase.functions.FirebaseFunctionsException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Shared by the customer and partner screens of DutyPe Services. */
@HiltViewModel
class HomeServicesViewModel @Inject constructor(
    private val repo: HomeServicesRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _catalog = MutableStateFlow<ServicesCatalog?>(null)
    val catalog: StateFlow<ServicesCatalog?> = _catalog.asStateFlow()

    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()

    val myBookings: StateFlow<List<ServiceBooking>> =
        repo.observeMyBookings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** [loaded] is false until the first answer, so the screen does not flash the apply form. */
    data class PartnerUi(val loaded: Boolean, val profile: PartnerProfile?)

    val partner: StateFlow<PartnerUi> =
        repo.observePartner().map { PartnerUi(true, it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PartnerUi(false, null))

    val partnerJobs: StateFlow<List<ServiceBooking>> =
        repo.observePartnerJobs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topups: StateFlow<List<PartnerTopup>> =
        repo.observeTopups().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadCatalog(force: Boolean = false) {
        viewModelScope.launch {
            runCatching { repo.catalog(force) }
                .onSuccess {
                    _catalog.value = it
                    _catalogError.value = null
                }
                .onFailure {
                    Timber.w(it, "catalog failed")
                    _catalogError.value = message(it)
                }
        }
    }

    fun booking(bookingId: String): Flow<ServiceBooking?> = repo.observeBooking(bookingId)

    /** Current phone location: lat, lng, area, address (null without permission / GPS). */
    suspend fun currentPlace(): Place? = runCatching {
        locationRepository.getHighAccuracy(timeoutMs = 8_000L, minAccuracyMeters = 100f)?.let {
            Place(it.latitude, it.longitude, it.area, it.address)
        } ?: locationRepository.lastKnownLocation()?.let { Place(it.latitude, it.longitude, it.area.orEmpty(), it.address) }
    }.getOrNull()

    data class Place(val lat: Double, val lng: Double, val area: String, val address: String)

    suspend fun book(
        service: ServiceItem, addressText: String, area: String, lat: Double, lng: Double, note: String, scheduledAt: Long?
    ): Result<String> = runCatching {
        repo.createBooking(service.id, addressText, area, lat, lng, note, scheduledAt)
    }.mapError()

    suspend fun startCode(bookingId: String): String = runCatching { repo.startCode(bookingId) }.getOrDefault("")

    suspend fun cancel(bookingId: String): Result<Unit> = runCatching { repo.cancel(bookingId) }.mapError()

    suspend fun rate(bookingId: String, stars: Int, review: String): Result<Unit> =
        runCatching { repo.rate(bookingId, stars, review) }.mapError()

    // ─────────────────────────── partner ───────────────────────────

    suspend fun apply(categories: List<String>, experienceYears: Int, area: String, note: String): Result<Unit> =
        runCatching { repo.apply(categories, experienceYears, area, note) }.mapError()

    /** Going online sends the current location (offers come for jobs near it). */
    suspend fun setOnline(online: Boolean): Result<Unit> = runCatching {
        val place = if (online) currentPlace() ?: error("Turn on location to go Online") else null
        repo.setOnline(online, place?.lat, place?.lng)
    }.mapError()

    suspend fun offer(bookingId: String): Result<ServiceOffer> = runCatching { repo.offer(bookingId) }.mapError()

    suspend fun accept(bookingId: String): Result<AcceptResult> = runCatching { repo.accept(bookingId) }.mapError()

    suspend fun update(bookingId: String, action: String, otp: String = "", extras: Int = 0, extrasNote: String = ""): Result<Unit> =
        runCatching { repo.update(bookingId, action, otp, extras, extrasNote) }.mapError()

    suspend fun requestTopup(amount: Int, utr: String): Result<Unit> =
        runCatching { repo.requestTopup(amount, utr) }.mapError()

    private fun <T> Result<T>.mapError(): Result<T> =
        fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(Exception(message(it), it)) })

    private fun message(e: Throwable): String = when (e) {
        is FirebaseFunctionsException -> e.message ?: "Something went wrong. Please try again."
        else -> e.message?.takeIf { it.length < 120 } ?: "Please check your internet and try again."
    }
}
