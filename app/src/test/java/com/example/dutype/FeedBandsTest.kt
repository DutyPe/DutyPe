package com.example.dutype

import com.example.dutype.jobs.Geohash
import com.example.dutype.models.FeedSection
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The feed's km bands query the ~5 km cells returned by [Geohash.covering]; none may be missed. */
class FeedBandsTest {

    // Khammam town
    private val lat = 17.2477
    private val lng = 80.1437

    @Test
    fun everyPointInsideEachBandFallsInAQueriedCell() {
        FeedSection.BANDS.forEach { band ->
            val radius = band.maxKm!!
            val cells = Geohash.covering(lat, lng, radius, 5)
            assertTrue("$radius km -> ${cells.size} cells (max 3 queries of 30)", cells.size <= 90)
            var bearing = 0.0
            while (bearing < 360.0) {
                for (fraction in listOf(0.25, 0.5, 0.75, 0.999)) {
                    val r = radius * fraction
                    val pLat = lat + (r / 111.0) * cos(bearing * PI / 180)
                    val pLng = lng + (r / (111.0 * cos(lat * PI / 180))) * sin(bearing * PI / 180)
                    if (Geohash.distanceKm(lat, lng, pLat, pLng) > radius) continue
                    assertTrue("$radius km band missed a job at bearing $bearing", Geohash.encode(pLat, pLng, 5) in cells)
                }
                bearing += 5.0
            }
        }
    }

    @Test
    fun widerBandsOnlyAddCells() {
        var previous = emptySet<String>()
        FeedSection.BANDS.forEach { band ->
            val cells = Geohash.covering(lat, lng, band.maxKm!!, 5)
            assertTrue(cells.containsAll(previous))
            previous = cells
        }
    }
}
