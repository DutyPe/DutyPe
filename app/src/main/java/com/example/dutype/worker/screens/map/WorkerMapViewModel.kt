package com.example.dutype.worker.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.JobListingSummary
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.utils.GeoUtils
import com.example.dutype.worker.data.MapJobsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Visible map region (only the visible area, i.e. excluding map content padding). */
data class MapViewport(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
    val zoom: Float
) {
    val centerLat: Double get() = (south + north) / 2.0
    val centerLng: Double get() = (west + east) / 2.0

    /** True when the point is inside this viewport grown by [margin] x span on every side. */
    fun contains(lat: Double, lng: Double, margin: Double = 0.0): Boolean {
        val latPad = (north - south) * margin
        val lngPad = (east - west) * margin
        return lat >= south - latPad && lat <= north + latPad &&
            lng >= west - lngPad && lng <= east + lngPad
    }

    fun diagonalKm(): Double = GeoUtils.calculateHaversineDistance(south, west, north, east)
}

data class WorkerMapUiState(
    val jobs: List<JobListingSummary> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false
)

/** Process-wide cache so returning to the Map tab paints markers instantly. */
private object MapJobsMemoryCache {
    @Volatile var jobs: List<JobListingSummary> = emptyList()
    @Volatile var lat: Double? = null
    @Volatile var lng: Double? = null
}

/**
 * Map tab state.
 *
 * Load strategy:
 *  1. Last-known location + cached markers are used instantly.
 *  2. [loadInitial]: nearest ~200 jobs via widening rings around the user.
 *  3. [onCameraIdle]: 600 ms debounce, then jobs inside the visible viewport are fetched with
 *     geohash range queries and merged by id (cap [MAX_MARKERS], far-away ones dropped).
 *  4. Every request is guarded by a generation counter, stale ones are cancelled and ignored.
 *  Applied / expired / closed jobs are hidden.
 */
@HiltViewModel
class WorkerMapViewModel @Inject constructor(
    private val source: MapJobsSource,
    applicationStateManager: ApplicationStateManager,
    private val locationPreferences: LocationPreferences
) : ViewModel() {

    companion object {
        private const val MAX_MARKERS = 500
        private const val PRUNE_THRESHOLD = 300
        private const val PRUNE_MARGIN = 2.0
        private const val INITIAL_TARGET = 200
        private const val DEBOUNCE_MS = 600L
        private const val RELOAD_KM = 1.0
    }

    private val store = LinkedHashMap<String, JobListingSummary>()
    private val raw = MutableStateFlow(WorkerMapUiState())

    /** Visible jobs: merged store minus jobs the worker already applied to. */
    val uiState: StateFlow<WorkerMapUiState> = combine(raw, applicationStateManager.appliedJobIds) { state, applied ->
        if (applied.isEmpty()) state else state.copy(jobs = state.jobs.filter { it.id !in applied })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, WorkerMapUiState())

    private var userLat: Double? = null
    private var userLng: Double? = null
    private var refLat: Double? = null
    private var refLng: Double? = null

    private var initGeneration = 0
    private var viewGeneration = 0
    private var initJob: Job? = null
    private var viewJob: Job? = null
    private var debounceJob: Job? = null
    private var activeRequests = 0
    private var initialFor: Pair<Double, Double>? = null
    private var lastFetchViewport: MapViewport? = null

    init {
        val saved = locationPreferences.getSavedLocation()
        if (saved != null && saved.hasValidCoordinates()) {
            userLat = saved.latitude
            userLng = saved.longitude
        }
        val cached = MapJobsMemoryCache.jobs
        if (cached.isNotEmpty()) {
            cached.forEach { store[it.id] = it }
            publish(hasLoaded = true)
        }
    }

    /** Best known centre for the very first camera position (instant map). */
    fun lastKnownLatLng(): Pair<Double, Double>? {
        val lat = userLat ?: MapJobsMemoryCache.lat
        val lng = userLng ?: MapJobsMemoryCache.lng
        return if (lat != null && lng != null) lat to lng else null
    }

    /** Nearest ~200 jobs via widening rings. Safe to call again with a refined location. */
    fun loadInitial(lat: Double, lng: Double) {
        userLat = lat
        userLng = lng
        if (refLat == null) {
            refLat = lat
            refLng = lng
        }
        val previous = initialFor
        if (previous != null && raw.value.hasLoaded &&
            GeoUtils.calculateHaversineDistance(previous.first, previous.second, lat, lng) < RELOAD_KM
        ) {
            publish()
            return
        }
        initialFor = lat to lng
        val generation = ++initGeneration
        initJob?.cancel()
        initJob = viewModelScope.launch {
            setActive(1)
            try {
                source.nearbyStream(lat, lng, INITIAL_TARGET).collect { batch ->
                    if (generation == initGeneration) merge(batch, null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "WorkerMapViewModel: initial load failed")
                if (generation == initGeneration) initialFor = null
            } finally {
                setActive(-1)
            }
        }
    }

    /** Called whenever the camera becomes idle. Debounced; skips insignificant moves. */
    fun onCameraIdle(viewport: MapViewport) {
        refLat = viewport.centerLat
        refLng = viewport.centerLng
        if (!isMeaningfulMove(lastFetchViewport, viewport)) return
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            fetchViewport(viewport)
        }
    }

    /** "Search this area": fetch immediately for the given viewport. */
    fun searchThisArea(viewport: MapViewport) {
        debounceJob?.cancel()
        fetchViewport(viewport)
    }

    private fun isMeaningfulMove(last: MapViewport?, now: MapViewport): Boolean {
        if (last == null) return true
        if (now.zoom < last.zoom - 0.5f) return true
        val shiftKm = GeoUtils.calculateHaversineDistance(
            last.centerLat, last.centerLng, now.centerLat, now.centerLng
        )
        return shiftKm > last.diagonalKm() * 0.2
    }

    private fun fetchViewport(viewport: MapViewport) {
        lastFetchViewport = viewport
        val generation = ++viewGeneration
        viewJob?.cancel()
        viewJob = viewModelScope.launch {
            setActive(1)
            try {
                val result = source.fetchBounds(viewport.south, viewport.west, viewport.north, viewport.east)
                if (generation == viewGeneration) {
                    if (result != null) merge(result, viewport) else lastFetchViewport = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "WorkerMapViewModel: viewport load failed")
                if (generation == viewGeneration) lastFetchViewport = null
            } finally {
                setActive(-1)
            }
        }
    }

    private fun merge(batch: List<JobListingSummary>, viewport: MapViewport?) {
        for (job in batch) {
            if (GeoUtils.hasValidCoordinates(job.lat, job.lng)) store[job.id] = job
        }
        if (viewport != null && store.size > PRUNE_THRESHOLD) {
            val iterator = store.values.iterator()
            while (iterator.hasNext()) {
                val job = iterator.next()
                if (!viewport.contains(job.lat, job.lng, PRUNE_MARGIN)) iterator.remove()
            }
        }
        if (store.size > MAX_MARKERS) trimToCap()
        publish()
    }

    private fun trimToCap() {
        val lat = refLat ?: userLat ?: return
        val lng = refLng ?: userLng ?: return
        val keep = store.values
            .sortedBy { GeoUtils.calculateHaversineDistance(lat, lng, it.lat, it.lng) }
            .take(MAX_MARKERS)
        store.clear()
        keep.forEach { store[it.id] = it }
    }

    private fun publish(hasLoaded: Boolean = false) {
        val now = System.currentTimeMillis()
        val lat = userLat
        val lng = userLng
        val jobs = store.values
            .filter { !(it.expiresAt > 0L && it.expiresAt < now) }
            .map { job ->
                if (lat != null && lng != null) {
                    job.copy(distance = GeoUtils.calculateHaversineDistance(lat, lng, job.lat, job.lng))
                } else job
            }
            .sortedBy { it.distance ?: Double.MAX_VALUE }
        MapJobsMemoryCache.jobs = jobs
        MapJobsMemoryCache.lat = lat ?: MapJobsMemoryCache.lat
        MapJobsMemoryCache.lng = lng ?: MapJobsMemoryCache.lng
        raw.update { it.copy(jobs = jobs, hasLoaded = it.hasLoaded || hasLoaded) }
    }

    private fun setActive(delta: Int) {
        activeRequests = (activeRequests + delta).coerceAtLeast(0)
        raw.update {
            it.copy(
                isLoading = activeRequests > 0,
                hasLoaded = it.hasLoaded || activeRequests == 0
            )
        }
    }
}
