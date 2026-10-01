package com.example.dutype

import com.example.dutype.jobs.Geohash
import com.example.dutype.jobs.PlaceRepository
import com.example.dutype.urgent.UrgentOffers
import org.junit.Assert.assertEquals
import org.junit.Test

/** The phone must subscribe to the exact topics functions/src/urgent.ts sends offers to. */
class UrgentTopicTest {
    @Test
    fun topicMatchesServer() {
        // Server: urgentTopic(cell, locale) = `urgent_${cell}_${locale}`, cell = geohash precision 5.
        val cell = Geohash.encode(17.2477, 80.1437, PlaceRepository.CELL_PRECISION)
        assertEquals(5, cell.length)
        assertEquals("urgent_${cell}_te", PlaceRepository.urgentTopic(cell, "te"))
    }

    @Test
    fun offerLinksOpenTheOfferPage() {
        assertEquals("dutype://urgent/abc123", UrgentOffers.deepLink("abc123"))
        assertEquals(UrgentOffers.notificationId("abc123"), UrgentOffers.notificationId("abc123"))
    }
}
