package com.example.dutype.homeservices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.models.LocationData
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
    private val locationRepository: LocationRepository,
    val savedWorkLocationsStore: com.example.dutype.services.SavedWorkLocationsStore
) : ViewModel() {

    val userLocation: StateFlow<LocationData?> = locationRepository.userLocation

    private val _catalog = MutableStateFlow<ServicesCatalog?>(repo.getCachedCatalog())
    val catalog: StateFlow<ServicesCatalog?> = _catalog.asStateFlow()

    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()

    init {
        // Real-time synchronization: when promotional banners or offers are saved in admin web portal,
        // update the mobile app catalog StateFlow instantly without requiring app restart or waiting for TTL.
        viewModelScope.launch {
            repo.observeFirestoreServicesConfig().collect { snap ->
                if (snap != null && snap.exists()) {
                    val current = _catalog.value ?: repo.getCachedCatalog()
                    val newBanners = repo.bannersFromSnapshot(snap)
                    val newOffers = repo.offersFromSnapshot(snap)
                    val newMostBooked = repo.mostBookedServiceIdsFromSnapshot(snap)
                    if (current != null) {
                        _catalog.value = current.copy(
                            promoBanners = newBanners,
                            offers = newOffers,
                            mostBookedServiceIds = if (newMostBooked.isNotEmpty()) newMostBooked else current.mostBookedServiceIds
                        )
                    } else {
                        loadCatalog(force = false)
                    }
                }
            }
        }
    }

    val myBookings: StateFlow<List<ServiceBooking>> =
        repo.observeMyBookings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** My bookings with load / error state (history screen). */
    val bookingsLoad: StateFlow<BookingsLoad> =
        repo.observeMyBookingsLoad().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookingsLoad(false, emptyList(), null))

    fun bookingLoad(bookingId: String): Flow<BookingLoad> = repo.observeBookingLoad(bookingId)

    suspend fun quote(serviceId: String, couponCode: String): Result<ServiceQuote> =
        runCatching { repo.previewQuote(serviceId, couponCode) }.mapError()

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
        val cached = locationRepository.userLocation.value ?: locationRepository.lastKnownLocation()
        if (locationRepository.isManualLocationLocked() && cached != null && cached.hasValidCoordinates()) {
            return@runCatching Place(cached.latitude, cached.longitude, cached.area.orEmpty(), cached.address)
        }
        locationRepository.getHighAccuracy(timeoutMs = 8_000L, minAccuracyMeters = 100f)?.let {
            Place(it.latitude, it.longitude, it.area, it.address)
        } ?: cached?.let { Place(it.latitude, it.longitude, it.area.orEmpty(), it.address) }
    }.getOrNull()

    data class Place(val lat: Double, val lng: Double, val area: String, val address: String)

    fun setManualLocation(area: String, city: String = "Khammam", lat: Double = 17.2473, lng: Double = 80.1514) {
        locationRepository.setManualLocation(city, area, "$area, $city", lat, lng)
    }

    fun resetToGps() {
        locationRepository.setAutoLocation()
        viewModelScope.launch {
            locationRepository.refresh(force = true)
            currentPlace()
        }
    }

    suspend fun searchPlaces(query: String): List<com.example.dutype.models.PlaceSuggestion> =
        locationRepository.searchPlaces(query)

    suspend fun book(
        service: ServiceItem, addressText: String, area: String, lat: Double, lng: Double, note: String, scheduledAt: Long?,
        couponCode: String = ""
    ): Result<String> = runCatching {
        repo.createBooking(service.id, addressText, area, lat, lng, note, scheduledAt, couponCode)
    }.mapError()

    suspend fun startCode(bookingId: String): String = runCatching { repo.startCode(bookingId) }.getOrDefault("")

    suspend fun cancel(bookingId: String, reason: String = ""): Result<Unit> = runCatching { repo.cancel(bookingId, reason) }.mapError()

    suspend fun rate(bookingId: String, stars: Int, review: String): Result<Unit> =
        runCatching { repo.rate(bookingId, stars, review) }.mapError()

    // ─────────────────────────── partner ───────────────────────────

    /** Applying sends where the worker is: partners must be inside the service district. */
    suspend fun apply(
        categories: List<String>, experienceYears: Int, area: String, note: String,
        skillProof: String = "", acceptGuidelines: Boolean = false
    ): Result<Unit> = runCatching {
        val place = currentPlace() ?: error("Turn on location to apply")
        repo.apply(categories, experienceYears, area, note, place.lat, place.lng, skillProof, acceptGuidelines)
    }.mapError()

    suspend fun inServiceArea(lat: Double, lng: Double): Boolean =
        runCatching { repo.isInServiceArea(lat, lng) }.getOrDefault(false)

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
