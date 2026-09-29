package com.example.dutype.worker.screens.map

import com.example.dutype.models.JobListingSummary
import kotlin.math.floor
import kotlin.math.pow

/** Something drawn on the map: a single job pill or a cluster bubble. */
internal sealed interface MapItem {
    val key: String
    val lat: Double
    val lng: Double
}

internal data class MapSingle(val job: JobListingSummary) : MapItem {
    override val key: String get() = "j:${job.id}"
    override val lat: Double get() = job.lat
    override val lng: Double get() = job.lng
}

internal data class MapCluster(
    override val key: String,
    override val lat: Double,
    override val lng: Double,
    val jobs: List<JobListingSummary>
) : MapItem {
    val count: Int get() = jobs.size
}

/** Simple grid clustering in lat/lng space; cell size shrinks with zoom (about 88dp on screen). */
internal object MapClusterer {

    private const val CELL_DEGREES_AT_ZOOM0 = 90.0
    private const val SAME_SPOT_ZOOM = 16.5f
    private const val SAME_SPOT_CELL = 0.00012

    fun cluster(jobs: List<JobListingSummary>, zoom: Float, pinnedId: String?): List<MapItem> {
        val cell = if (zoom >= SAME_SPOT_ZOOM) SAME_SPOT_CELL else CELL_DEGREES_AT_ZOOM0 / 2.0.pow(zoom.toDouble())
        val groups = LinkedHashMap<String, MutableList<JobListingSummary>>()
        val result = ArrayList<MapItem>()
        for (job in jobs) {
            if (job.id == pinnedId) {
                result.add(MapSingle(job))
                continue
            }
            val key = "${floor(job.lat / cell).toLong()}:${floor(job.lng / cell).toLong()}"
            groups.getOrPut(key) { ArrayList() }.add(job)
        }
        for ((key, group) in groups) {
            if (group.size == 1) {
                result.add(MapSingle(group[0]))
            } else {
                val lat = group.sumOf { it.lat } / group.size
                val lng = group.sumOf { it.lng } / group.size
                result.add(MapCluster("c:$key:${group.size}", lat, lng, group))
            }
        }
        return result
    }
}
