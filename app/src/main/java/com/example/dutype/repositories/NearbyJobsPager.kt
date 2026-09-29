package com.example.dutype.repositories

import com.example.dutype.models.JobListingSummary
import com.example.dutype.utils.GeoUtils
import timber.log.Timber

/**
 * Distance-ordered ("nearest first, then outward") pager for the worker Jobs tab.
 *
 * WHY THIS EXISTS
 * Firestore cannot order by distance. The old implementation paged by createdAt and then
 * sorted only the page it had just fetched, so page 2 contained the newest jobs from
 * anywhere (often 100+ km away) even while nearer jobs were still unseen.
 *
 * HOW IT WORKS (concentric rings, GeoFire geohash range queries)
 *   ring 0:   0 -   5 km
 *   ring 1:   5 -  15 km
 *   ring 2:  15 -  30 km
 *   ring 3:  30 -  50 km
 *   ring 4:  50 - 100 km
 *   ring 5: 100 - 250 km
 *   rest  : everything else / jobs without a geohash, chronological cursor scan
 *
 * A ring is fetched completely (all geohash cells covering the ring's outer circle),
 * trimmed to inner <= haversine distance < outer, sorted by exact distance and buffered.
 * Pages are then cut from the buffer 10 at a time. The pager only moves on to the next
 * ring when the buffer is empty, so the global order stays nearest -> far and page N+1
 * can never contain a job that is nearer than something on page N.
 *
 * Not thread-safe: the caller must issue one [nextPage] at a time (the ViewModel does).
 *
 * @param maxRadiusKm when set (user picked a distance filter) rings stop at this radius
 *                    and the chronological "rest" scan is skipped entirely.
 */
class NearbyJobsPager(
    private val repository: FirestoreJobRepository,
    userLatitude: Double?,
    userLongitude: Double?,
    private val category: String?,
    maxRadiusKm: Double? = null
) {

    data class Page(val jobs: List<JobListingSummary>, val hasMore: Boolean)

    private val lat: Double = userLatitude ?: 0.0
    private val lng: Double = userLongitude ?: 0.0
    private val hasLocation: Boolean =
        userLatitude != null && userLongitude != null && GeoUtils.hasValidCoordinates(lat, lng)

    private val rings: List<Pair<Double, Double>> = if (!hasLocation) {
        emptyList()
    } else {
        val bounded = maxRadiusKm?.takeIf { it > 0.0 }
        BASE_RINGS
            .filter { (inner, _) -> bounded == null || inner < bounded }
            .map { (inner, outer) -> inner to (if (bounded != null) minOf(outer, bounded) else outer) }
    }

    private var ringIndex = 0
    private val buffer = ArrayDeque<JobListingSummary>()
    private val seenIds = HashSet<String>()
    private var restCursor: String? = null
    private var restDone: Boolean = hasLocation && maxRadiusKm != null && maxRadiusKm > 0.0

    /**
     * Returns the next [pageSize] jobs in nearest-first order. An empty page means the
     * pager is exhausted. On a network error the pager keeps its position, so calling
     * [nextPage] again retries the same ring/cursor.
     */
    suspend fun nextPage(pageSize: Int): Result<Page> {
        val out = ArrayList<JobListingSummary>(pageSize)
        while (out.size < pageSize) {
            if (buffer.isNotEmpty()) {
                out.add(buffer.removeFirst())
                continue
            }

            if (ringIndex < rings.size) {
                val (inner, outer) = rings[ringIndex]
                val ring = repository.fetchRingSummaries(lat, lng, inner, outer, category)
                val list = ring.getOrElse { error ->
                    return if (out.isEmpty()) Result.failure(error) else Result.success(Page(out, true))
                }
                ringIndex++
                Timber.d("NearbyJobsPager: ring ${inner.toInt()}-${outer.toInt()}km -> ${list.size} jobs")
                for (job in list) {
                    if (seenIds.add(job.id)) buffer.addLast(job)
                }
                continue
            }

            if (!restDone) {
                val chunk = repository.fetchChronologicalSummaries(
                    limit = REST_CHUNK,
                    lastDocumentId = restCursor,
                    category = category,
                    userLatitude = if (hasLocation) lat else null,
                    userLongitude = if (hasLocation) lng else null
                )
                val list = chunk.getOrElse { error ->
                    return if (out.isEmpty()) Result.failure(error) else Result.success(Page(out, true))
                }
                if (list.isEmpty()) {
                    restDone = true
                    continue
                }
                restCursor = list.last().id
                list.filter { seenIds.add(it.id) }
                    .sortedBy { it.distance ?: Double.MAX_VALUE }
                    .forEach { buffer.addLast(it) }
                continue
            }

            break
        }

        val hasMore = buffer.isNotEmpty() || ringIndex < rings.size || !restDone
        return Result.success(Page(out, hasMore))
    }

    private companion object {
        const val REST_CHUNK = 30L
        val BASE_RINGS: List<Pair<Double, Double>> = listOf(
            0.0 to 5.0,
            5.0 to 15.0,
            15.0 to 30.0,
            30.0 to 50.0,
            50.0 to 100.0,
            100.0 to 250.0
        )
    }
}
