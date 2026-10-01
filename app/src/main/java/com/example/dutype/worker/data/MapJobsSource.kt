package com.example.dutype.worker.data

import com.example.dutype.firestore.FirestoreSchema.Jobs
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.jobs.Geohash
import com.example.dutype.jobs.JobQuery
import com.example.dutype.jobs.JobRepository
import com.example.dutype.jobs.PlaceRepository
import com.example.dutype.models.JobListingSummary
import com.example.dutype.utils.toJobListingSummary
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Jobs for the worker Map tab.
 *
 * [nearby] is the first screenful (the feed's km bands). [fetchBounds] loads the viewport by the
 * same ~5 km cells jobs are stored under (`cell in [...]`, 30 cells per query), nearest to the
 * map centre first; a cell loaded in the last [CELL_TTL_MS] is never queried again, so panning
 * back and forth costs nothing. Zoomed out past ~[MAX_VIEW_RADIUS_KM] it loads nothing new.
 */
@Singleton
class MapJobsSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val jobRepository: JobRepository
) {
    private val loadedCells = HashMap<String, Long>()

    suspend fun nearby(latitude: Double, longitude: Double, target: Int): List<JobListingSummary> =
        jobRepository.pager(latitude, longitude, JobQuery(maxRadiusKm = NEARBY_RADIUS_KM))
            .nextPage(target).getOrThrow().jobs

    /** Open jobs in the viewport's cells not loaded in the last [CELL_TTL_MS]. */
    suspend fun fetchBounds(south: Double, west: Double, north: Double, east: Double): List<JobListingSummary> {
        val box = Geohash.Box(south, north, west, east)
        val radiusKm = Geohash.distanceKm(box.centerLat, box.centerLng, box.maxLat, box.maxLng)
        if (radiusKm > MAX_VIEW_RADIUS_KM) return emptyList()
        val now = System.currentTimeMillis()
        val cells = Geohash.covering(box.centerLat, box.centerLng, radiusKm, PlaceRepository.CELL_PRECISION)
            .filter { cell ->
                val b = Geohash.decode(cell)
                b.maxLat >= box.minLat && b.minLat <= box.maxLat && b.maxLng >= box.minLng && b.minLng <= box.maxLng
            }
            .filter { now - (loadedCells[it] ?: 0L) > CELL_TTL_MS }
            .sortedBy { Geohash.decode(it).distanceKmFrom(box.centerLat, box.centerLng) }
            .take(MAX_CELLS_PER_VIEWPORT)
        return coroutineScope {
            cells.chunked(30).map { chunk ->
                async {
                    val docs = firestore.collection(Jobs.COLLECTION)
                        .whereEqualTo(Jobs.STATUS, Values.JobStatus.OPEN)
                        .whereIn(Jobs.CELL, chunk)
                        .orderBy(Jobs.CREATED_AT, Query.Direction.DESCENDING)
                        .limit(PER_QUERY_LIMIT)
                        .get().await().documents
                    chunk.forEach { loadedCells[it] = now }
                    docs.mapNotNull { it.data?.toJobListingSummary(it.id) }
                }
            }.awaitAll().flatten().filter { it.expiresAt == 0L || it.expiresAt > now }
        }
    }

    private companion object {
        const val NEARBY_RADIUS_KM = 20.0
        const val MAX_VIEW_RADIUS_KM = 30.0
        const val MAX_CELLS_PER_VIEWPORT = 90
        const val PER_QUERY_LIMIT = 100L
        const val CELL_TTL_MS = 10 * 60 * 1000L
    }
}
