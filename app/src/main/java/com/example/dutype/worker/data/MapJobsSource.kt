package com.example.dutype.worker.data

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
 * Data source used ONLY by the worker Map tab.
 *
 *  - [nearbyStream]: nearest ~200 open jobs around the user using widening rings
 *    (3 -> 8 -> 20 -> 50 -> 100 km). Emits after every ring so markers appear progressively.
 *  - [fetchBounds]: every open job inside a map viewport, using geohash range queries that
 *    cover the visible bounds. Returns null when every query failed (caller keeps old data).
 *
 * Only single-field indexes (geohash range) are used, so no composite index is required.
 */
@Singleton
class MapJobsSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private val RING_STEPS_KM = listOf(3.0, 8.0, 20.0, 50.0, 100.0)
        private const val CELL_LIMIT = 100L
        private const val QUERY_TIMEOUT_MS = 6_000L
        private const val MAX_VIEWPORT_RADIUS_KM = 60.0
        private const val MIN_VIEWPORT_RADIUS_KM = 0.5
        private const val MAX_VIEWPORT_RESULTS = 500
    }

    /** Progressive nearest-first jobs around a point; each emission is the full list so far. */
    fun nearbyStream(latitude: Double, longitude: Double, target: Int): Flow<List<JobListingSummary>> = flow {
        val now = System.currentTimeMillis()
        for (radius in RING_STEPS_KM) {
            val page = queryCircle(latitude, longitude, radius, now, target)
            emit(page)
            if (page.size >= target) break
        }
    }.flowOn(Dispatchers.IO)

    /** Open jobs inside the given viewport, or null when all queries failed. */
    suspend fun fetchBounds(
        south: Double,
        west: Double,
        north: Double,
        east: Double
    ): List<JobListingSummary>? = withContext(Dispatchers.IO) {
        val centerLat = (south + north) / 2.0
        val centerLng = (west + east) / 2.0
        val radiusKm = GeoUtils.calculateHaversineDistance(centerLat, centerLng, north, east)
            .coerceIn(MIN_VIEWPORT_RADIUS_KM, MAX_VIEWPORT_RADIUS_KM)
        val now = System.currentTimeMillis()
        val cells = fetchCells(centerLat, centerLng, radiusKm) ?: return@withContext null
        val latPad = (north - south) * 0.05
        val lngPad = (east - west) * 0.05
        cells
            .distinctBy { it.first }
            .mapNotNull { (id, data) -> parseOpen(id, data, now) }
            .filter {
                it.lat in (south - latPad)..(north + latPad) &&
                    it.lng in (west - lngPad)..(east + lngPad)
            }
            .sortedBy { GeoUtils.calculateHaversineDistance(centerLat, centerLng, it.lat, it.lng) }
            .take(MAX_VIEWPORT_RESULTS)
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private suspend fun queryCircle(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        now: Long,
        limit: Int
    ): List<JobListingSummary> {
        val cells = fetchCells(latitude, longitude, radiusKm)
            ?: throw IllegalStateException("Unable to load nearby jobs")
        return cells
            .distinctBy { it.first }
            .mapNotNull { (id, data) -> parseOpen(id, data, now) }
            .mapNotNull { summary ->
                val km = GeoUtils.calculateHaversineDistance(latitude, longitude, summary.lat, summary.lng)
                if (km <= radiusKm) summary.copy(distance = km) else null
            }
            .sortedBy { it.distance ?: Double.MAX_VALUE }
            .take(limit)
    }

    /** Runs every geohash cell in parallel. Null when there were cells and all of them failed. */
    private suspend fun fetchCells(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Pair<String, Map<String, Any>>>? {
        val bounds = GeoUtils.getGeohashQueryBounds(latitude, longitude, radiusKm)
        val results = coroutineScope {
            bounds.map { bound -> async { runCell(bound.startHash, bound.endHash) } }.awaitAll()
        }
        if (results.isNotEmpty() && results.all { it == null }) return null
        return results.filterNotNull().flatten()
    }

    private suspend fun runCell(startHash: String, endHash: String): List<Pair<String, Map<String, Any>>>? {
        val query = firestore.collection(FirestoreCollections.JOBS)
            .orderBy("geohash")
            .startAt(startHash)
            .endAt(endHash)
            .limit(CELL_LIMIT)
        return fetch(query)
    }

    /** Server read with timeout; falls back to the Firestore local cache. Null on failure. */
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
            Timber.w(e, "MapJobsSource: query failed")
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
        if (!GeoUtils.hasValidCoordinates(summary.lat, summary.lng)) return null
        return summary
    }
}
