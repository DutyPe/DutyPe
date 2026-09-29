package com.example.dutype.worker.data

import com.example.dutype.cache.JobCacheManager
import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.JobListingSummary
import com.example.dutype.utils.GeoUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One progressive result of the home "nearest jobs" query.
 * [isFinal] is true for the last batch of a stream (enough results, or widest radius reached).
 */
data class HomeJobsBatch(
    val jobs: List<JobListingSummary>,
    val isFinal: Boolean
)

/**
 * Fast, small-page data source used ONLY by the worker Home screen.
 *
 * Why it exists (the shared repository path is slow for a home preview):
 *  - it awaits the saved-jobs list before running any job query,
 *  - it reads up to 9 geohash cells x 30 docs even when 12 cards are needed,
 *  - it runs a second fallback query one after another and emits only ONE fresh result.
 *
 * This source instead:
 *  1. serves the last cached page instantly (memory / Room) via [cachedNearby],
 *  2. streams results with a small page and a stepped radius 5 -> 10 -> 25 -> 50 km, emitting
 *     as soon as the first step has enough jobs ([nearestStream]),
 *  3. fetches instant / urgent (urgency == HIGH) jobs with an independent query so the caller
 *     can run it in parallel ([fetchUrgent]).
 *
 * All Firestore queries use only single-field indexes (geohash range, createdAt, urgency IN),
 * so no composite index is required.
 */
@Singleton
class HomeJobsSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val jobCacheManager: JobCacheManager
) {

    companion object {
        private val RADIUS_STEPS_KM = listOf(5.0, 10.0, 25.0, 50.0, 100.0, 250.0)
        private const val MIN_RESULTS_TO_STOP = 12
        private const val MIN_GUARANTEED_NEAREST = 5
        private const val CELL_LIMIT = 20L
        private const val QUERY_TIMEOUT_MS = 6_000L
        private const val FALLBACK_FETCH_LIMIT = 30L
        private const val URGENT_FETCH_LIMIT = 40L
        private const val MAX_URGENT_RADIUS_KM = 50.0
        private const val MAX_CACHE_RADIUS_KM = 50.0
        private const val CACHE_READ_LIMIT = 30
        private val URGENT_VALUES = listOf("HIGH", "High", "high")
    }

    // ------------------------------------------------------------------
    // Cache-first
    // ------------------------------------------------------------------

    /**
     * Last cached jobs (memory, then Room) ranked nearest-first. Returns instantly (no network).
     * With a location, only jobs within 50 km are returned so a stale list from another city
     * is never shown.
     */
    suspend fun cachedNearby(latitude: Double?, longitude: Double?, limit: Int): List<JobListingSummary> =
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val open = jobCacheManager.getCachedJobSummaries(CACHE_READ_LIMIT)
                    .filter { it.status.equals("open", ignoreCase = true) }
                    .filter { !(it.expiresAt > 0L && it.expiresAt < now) }
                if (latitude != null && longitude != null) {
                    open.mapNotNull { summary ->
                        val km = distanceKm(latitude, longitude, summary)
                        if (km != null && km <= MAX_CACHE_RADIUS_KM) summary.copy(distance = km) else null
                    }.sortedBy { it.distance ?: Double.MAX_VALUE }.take(limit)
                } else {
                    open.sortedByDescending { it.createdAt }.take(limit)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "HomeJobsSource: cache read failed")
                emptyList()
            }
        }

    /** Persist the latest fresh page so the next app start can paint instantly. */
    suspend fun persist(jobs: List<JobListingSummary>) {
        if (jobs.isEmpty()) return
        try {
            jobCacheManager.cacheJobSummaries(jobs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "HomeJobsSource: cache write failed")
        }
    }

    // ------------------------------------------------------------------
    // Nearest jobs
    // ------------------------------------------------------------------

    /**
     * Nearest-first jobs around the given point. Emits after each radius step; the first step
     * that yields [MIN_RESULTS_TO_STOP] jobs (or the widest step) is the final batch. If even the
     * widest radius is empty, falls back to the newest jobs ranked by distance so home is never blank.
     * Throws only when every query of the first step failed (e.g. offline with an empty cache).
     *
     * @param excludeIds jobs the worker can't see on home (already applied). They are dropped
     *   before counting, so the "5 nearest" guarantee holds for what is actually shown.
     */
    fun nearestStream(
        latitude: Double,
        longitude: Double,
        pageSize: Int,
        excludeIds: Set<String> = emptySet()
    ): Flow<HomeJobsBatch> = flow {
        val now = System.currentTimeMillis()
        val lastIndex = RADIUS_STEPS_KM.lastIndex
        for ((index, radius) in RADIUS_STEPS_KM.withIndex()) {
            val page = queryRadius(latitude, longitude, radius, pageSize, now, excludeIds)
            val isLast = index == lastIndex
            if (page.size >= MIN_RESULTS_TO_STOP || isLast) {
                if (page.size < MIN_GUARANTEED_NEAREST) {
                    // Guarantee: always try to reach the 5 nearest jobs anywhere (newest + distance).
                    val fallback = latestPage(now) ?: emptyList()
                    val ranked = (page + fallback.mapNotNull { summary ->
                        val km = distanceKm(latitude, longitude, summary)
                        if (km != null && summary.id !in excludeIds) summary.copy(distance = km) else null
                    }).distinctBy { it.id }
                        .sortedBy { it.distance ?: Double.MAX_VALUE }.take(pageSize)
                    emit(HomeJobsBatch(ranked, isFinal = true))
                } else {
                    emit(HomeJobsBatch(page, isFinal = true))
                }
                return@flow
            }
            emit(HomeJobsBatch(page, isFinal = false))
        }
    }.flowOn(Dispatchers.IO)

    /** No location yet: newest jobs first (single small query). */
    fun latestStream(pageSize: Int): Flow<HomeJobsBatch> = flow {
        val now = System.currentTimeMillis()
        val page = latestPage(now)
            ?: throw IllegalStateException("Unable to load jobs")
        emit(HomeJobsBatch(page.sortedByDescending { it.createdAt }.take(pageSize), isFinal = true))
    }.flowOn(Dispatchers.IO)

    // ------------------------------------------------------------------
    // Instant / urgent jobs
    // ------------------------------------------------------------------

    /**
     * Open jobs with urgency HIGH (the same field the card uses for the URGENT tag).
     * With a location: within 50 km, nearest-first. Without: newest first.
     * Never throws; returns an empty list on failure so it cannot block the nearest list.
     */
    suspend fun fetchUrgent(latitude: Double?, longitude: Double?, limit: Int): List<JobListingSummary> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val query = firestore.collection(FirestoreCollections.JOBS)
                .whereIn("urgency", URGENT_VALUES)
                .limit(URGENT_FETCH_LIMIT)
            val docs = fetch(query) ?: return@withContext emptyList()
            val urgent = docs
                .mapNotNull { (id, data) -> parseOpen(id, data, now) }
                .filter { it.urgency.equals("HIGH", ignoreCase = true) }
            if (latitude != null && longitude != null) {
                urgent.mapNotNull { summary ->
                    val km = distanceKm(latitude, longitude, summary)
                    if (km != null && km <= MAX_URGENT_RADIUS_KM) summary.copy(distance = km) else null
                }.sortedBy { it.distance ?: Double.MAX_VALUE }.take(limit)
            } else {
                urgent.sortedByDescending { it.createdAt }.take(limit)
            }
        }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private suspend fun queryRadius(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        pageSize: Int,
        now: Long,
        excludeIds: Set<String>
    ): List<JobListingSummary> {
        val bounds = GeoUtils.getGeohashQueryBounds(latitude, longitude, radiusKm)
        val cellResults = coroutineScope {
            bounds.map { bound ->
                async { runCell(bound.startHash, bound.endHash) }
            }.awaitAll()
        }
        if (cellResults.isNotEmpty() && cellResults.all { it == null }) {
            throw IllegalStateException("Unable to load nearby jobs")
        }
        return cellResults
            .filterNotNull()
            .flatten()
            .distinctBy { it.first }
            .mapNotNull { (id, data) -> parseOpen(id, data, now) }
            .filter { it.id !in excludeIds }
            .mapNotNull { summary ->
                val km = distanceKm(latitude, longitude, summary)
                if (km != null && km <= radiusKm) summary.copy(distance = km) else null
            }
            .sortedBy { it.distance ?: Double.MAX_VALUE }
            .take(pageSize)
    }

    private suspend fun runCell(startHash: String, endHash: String): List<Pair<String, Map<String, Any>>>? {
        val query = firestore.collection(FirestoreCollections.JOBS)
            .orderBy("geohash")
            .startAt(startHash)
            .endAt(endHash)
            .limit(CELL_LIMIT)
        return fetch(query)
    }

    private suspend fun latestPage(now: Long): List<JobListingSummary>? {
        val query = firestore.collection(FirestoreCollections.JOBS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(FALLBACK_FETCH_LIMIT)
        val docs = fetch(query) ?: return null
        return docs.mapNotNull { (id, data) -> parseOpen(id, data, now) }
    }

    /**
     * Server read with a timeout; on timeout falls back to the Firestore local cache.
     * Returns null when the query failed (so callers can tell "no docs" from "failed").
     */
    private suspend fun fetch(query: Query): List<Pair<String, Map<String, Any>>>? {
        return try {
            val snapshot = withTimeoutOrNull(QUERY_TIMEOUT_MS) { query.get().await() }
                ?: query.get(Source.CACHE).await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data == null) null else Pair(doc.id, data)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "HomeJobsSource: query failed")
            null
        }
    }

    private fun parseOpen(id: String, data: Map<String, Any>, now: Long): JobListingSummary? {
        val explicit = (data["status"] as? String)?.trim()?.lowercase().orEmpty()
        if (explicit.isNotEmpty() && explicit != "open") return null
        val summary = JobListingSummary.fromMap(data, id)
        if (summary.id.isBlank()) return null
        if (!summary.status.equals("open", ignoreCase = true)) return null
        if (summary.expiresAt > 0L && summary.expiresAt < now) return null
        return summary
    }

    private fun distanceKm(latitude: Double, longitude: Double, summary: JobListingSummary): Double? {
        return if (GeoUtils.hasValidCoordinates(summary.lat, summary.lng)) {
            GeoUtils.calculateHaversineDistance(latitude, longitude, summary.lat, summary.lng)
        } else {
            null
        }
    }
}
