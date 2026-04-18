package com.example.dutype.repositories

import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.LocationData
import com.example.dutype.utils.LocationInfo
import com.example.dutype.utils.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the user's location.
 *
 * This thin repository wraps the existing [LocationService] and [LocationPreferences]
 * so that:
 *  - The whole app observes ONE [StateFlow] (`userLocation`) instead of each screen
 *    fetching GPS independently.
 *  - Concurrent `refresh()` calls from multiple screens collapse into a single
 *    in-flight GPS request (the mutex below). This is the main "fast" win: without
 *    it, WorkerHome + Categories + AllJobs + Map can each trigger their own
 *    `getLocationFast` at the same moment and stall each other.
 *  - Callers get the cached last-known location **instantly** via
 *    [lastKnownLocation] for first-paint, while [refresh] updates the flow in
 *    the background.
 *
 * Screens should generally observe through their ViewModel:
 *   `val location by locationRepository.userLocation.collectAsState()`
 * and call `locationRepository.refresh()` from a `LaunchedEffect(Unit)` /
 * `viewModelScope`.
 */
@Singleton
class LocationRepository @Inject constructor(
    private val locationService: LocationService,
    private val locationPreferences: LocationPreferences,
) {

    /** Observed by screens. Seeded from preferences; updated by [refresh]. */
    val userLocation: StateFlow<LocationData?> = locationPreferences.currentLocation

    /** Loading state proxy, already maintained inside [LocationPreferences]. */
    val isLoading: StateFlow<Boolean> = locationPreferences.isLocationLoading

    /** Error state proxy, already maintained inside [LocationPreferences]. */
    val error: StateFlow<String?> = locationPreferences.locationError

    private val refreshMutex = Mutex()

    // Tracks the last time we returned a fresh GPS fix so repeat calls within this
    // window skip the expensive GPS round-trip and just reuse the cached value.
    private val _lastRefreshAt = MutableStateFlow(0L)
    val lastRefreshAt: StateFlow<Long> = _lastRefreshAt.asStateFlow()

    /**
     * Synchronous last-known location. Safe to call from Compose / main thread.
     * Returns immediately from SharedPreferences cache (no GPS I/O).
     */
    fun lastKnownLocation(): LocationData? = locationPreferences.getSavedLocation()

    /**
     * Cached location if it's younger than [maxAgeMs]. Useful for screens that
     * only want to show a location if it's reasonably current.
     */
    fun lastKnownLocationIfFresh(maxAgeMs: Long = FRESH_WINDOW_MS): LocationData? =
        locationPreferences.getSavedLocationIfFresh(maxAgeMs)

    /**
     * Request a fresh location. De-duplicated: if another call is already in
     * flight, this call waits for its result instead of starting a second GPS
     * request. If a fresh fix was obtained within [minIntervalMs] we skip the
     * fetch entirely and return the cached value — this is what prevents the
     * 4+ screens on app-startup from each kicking off their own GPS call.
     *
     * The optional [onLocationUpdate] callback is forwarded to
     * [LocationService.getLocationFast] so callers keep the exact
     * cached-then-GPS update semantics (callback fires twice: once for cache,
     * once for fresh GPS) while still benefiting from cross-screen dedup.
     *
     * @param force if true, bypasses [minIntervalMs] throttling.
     */
    suspend fun refresh(
        minIntervalMs: Long = MIN_REFRESH_INTERVAL_MS,
        force: Boolean = false,
        onLocationUpdate: (LocationInfo?) -> Unit = {},
    ): LocationData? = refreshMutex.withLock {
        val now = System.currentTimeMillis()
        val sinceLast = now - _lastRefreshAt.value
        if (!force && sinceLast < minIntervalMs) {
            Timber.d(
                "📍 LocationRepository: Skipping refresh (last was ${sinceLast}ms ago, " +
                    "min interval = ${minIntervalMs}ms) — reusing cached fix"
            )
            // Still fire the callback once with the cached value so callers that
            // rely on the callback for UI updates keep working.
            userLocation.value?.let { cached ->
                onLocationUpdate(locationDataToInfo(cached))
            }
            return@withLock userLocation.value
        }

        Timber.d("📍 LocationRepository: Starting refresh…")
        // LocationService.getLocationFast() persists to LocationPreferences itself,
        // which updates `_currentLocation` → our `userLocation` StateFlow.
        val info = locationService.getLocationFast(locationPreferences, onLocationUpdate)
        if (info != null) {
            _lastRefreshAt.value = System.currentTimeMillis()
        }
        userLocation.value
    }

    /**
     * Request a high-accuracy one-shot fix (GPS-precision, typically 5–50m).
     *
     * Used by screens that need to anchor precise coordinates (job posting,
     * map pinning) rather than the fast cached-then-GPS flow of [refresh].
     *
     * Shares the same [refreshMutex] as [refresh] so a burst of screens opening
     * simultaneously can't stack parallel GPS requests. On success the fresh
     * fix is persisted to [locationPreferences] so observers of [userLocation]
     * also see it.
     *
     * @param timeoutMs how long to wait for a fix that meets [minAccuracyMeters].
     * @param minAccuracyMeters desired accuracy threshold (lower = stricter).
     */
    suspend fun getHighAccuracy(
        timeoutMs: Long = 10_000L,
        minAccuracyMeters: Float = 50f,
    ): LocationInfo? = refreshMutex.withLock {
        Timber.d("📍 LocationRepository: high-accuracy fetch (timeout=${timeoutMs}ms, minAcc=${minAccuracyMeters}m)")
        val info = locationService.getHighAccuracyLocation(
            timeoutMs = timeoutMs,
            minAccuracyMeters = minAccuracyMeters,
        )
        if (info != null) {
            _lastRefreshAt.value = System.currentTimeMillis()
            // Persist so other screens observing userLocation pick up the fresh fix.
            runCatching { locationPreferences.saveLocation(locationService.toLocationData(info)) }
        }
        info
    }

    // Convert a cached LocationData back into a LocationInfo so that throttled
    // refresh() calls can still invoke the callback with a usable payload.
    private fun locationDataToInfo(data: LocationData): LocationInfo = LocationInfo(
        latitude = data.latitude,
        longitude = data.longitude,
        address = data.address,
        city = data.city.orEmpty(),
        area = data.area.orEmpty(),
        state = data.state.orEmpty(),
        country = data.country.orEmpty(),
        accuracy = data.accuracy,
        timestamp = data.timestamp,
    )

    companion object {
        /** Cached-location freshness window used by [lastKnownLocationIfFresh]. */
        const val FRESH_WINDOW_MS: Long = 15 * 60 * 1000L // 15 minutes

        /**
         * Minimum gap between two real GPS refreshes. Sub-sequent [refresh]
         * calls within this window return the cached value.
         */
        const val MIN_REFRESH_INTERVAL_MS: Long = 30 * 1000L // 30 seconds
    }
}
