package com.example.dutype

import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.firestore.FirestoreSchema.WorkerProfiles
import com.example.dutype.jobs.Geohash
import com.example.dutype.models.EmployerSubscription
import com.example.dutype.models.formatPaise
import com.example.dutype.models.isValidNormalizedReferralCode
import com.example.dutype.models.normalizeReferralCode
import com.example.dutype.profile.EmployerProfile
import com.example.dutype.profile.ExperienceBucket
import com.example.dutype.profile.WorkerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure logic the new schema depends on: geohash cells, money formatting, codes, profile rules. */
class SchemaLogicTest {

    // Madhapur, Hyderabad
    private val lat = 17.4486
    private val lng = 78.3908

    @Test
    fun geohashEncodeDecodeRoundTrip() {
        val hash = Geohash.encode(lat, lng, 9)
        assertEquals(9, hash.length)
        val box = Geohash.decode(hash)
        assertTrue(lat in box.minLat..box.maxLat)
        assertTrue(lng in box.minLng..box.maxLng)
        assertEquals(0.0, box.distanceKmFrom(lat, lng), 1e-9)
    }

    @Test
    fun geohashPrefixesNest() {
        val full = Geohash.encode(lat, lng, 9)
        assertEquals(full.take(4), Geohash.encode(lat, lng, 4))
        assertEquals(full.take(6), Geohash.encode(lat, lng, 6))
    }

    @Test
    fun coveringCellsIncludeHomeCellAndReachRadius() {
        val cells = Geohash.covering(lat, lng, 10.0, 6)
        assertTrue(Geohash.encode(lat, lng, 6) in cells)
        // A point ~8 km north must fall in one of the covering cells.
        val north = lat + 8.0 / 111.0
        assertTrue(Geohash.encode(north, lng, 6) in cells)
        // Every returned cell touches the circle.
        assertTrue(cells.all { Geohash.decode(it).distanceKmFrom(lat, lng) <= 10.0 })
    }

    @Test
    fun distanceIsHaversine() {
        // Madhapur -> Secunderabad is roughly 18 km as the crow flies.
        val d = Geohash.distanceKm(lat, lng, 17.4399, 78.4983)
        assertTrue("was $d", d in 10.0..13.0)
        assertEquals(0.0, Geohash.distanceKm(lat, lng, lat, lng), 1e-9)
    }

    @Test
    fun paiseFormatting() {
        assertEquals("₹100", formatPaise(10_000))
        assertEquals("₹123.50", formatPaise(12_350))
        assertEquals("₹0", formatPaise(0))
    }

    @Test
    fun referralCodesAreNormalized() {
        assertEquals("AB12CD", normalizeReferralCode(" ab-12 cd "))
        assertTrue(isValidNormalizedReferralCode("ab12cd"))
        assertFalse(isValidNormalizedReferralCode("ab1"))
    }

    @Test
    fun subscriptionParsesCreditsAndActivity() {
        val future = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + 86_400_000))
        val sub = EmployerSubscription.fromMap(
            mapOf(
                EmployerProfiles.Subscription.PLAN_ID to "starter_99",
                EmployerProfiles.Subscription.STATUS to "ACTIVE",
                EmployerProfiles.Subscription.EXPIRES_AT to future,
                EmployerProfiles.Subscription.CREDITS to mapOf(
                    EmployerProfiles.Subscription.CREDITS_NORMAL to 3L,
                    EmployerProfiles.Subscription.CREDITS_INSTANT to 5L
                )
            )
        )
        assertTrue(sub.isActive)
        assertEquals(3, sub.normalCredits)
        assertEquals(5, sub.instantCredits)
        assertFalse(EmployerSubscription.fromMap(null).isActive)
    }

    @Test
    fun workerProfileCompletionNeedsNamePhoneAndSkill() {
        val base = mapOf(WorkerProfiles.NAME to "Ravi", WorkerProfiles.PHONE to "+919876543210")
        assertFalse(WorkerProfile.from("u1", base).isComplete)
        val complete = WorkerProfile.from("u1", base + (WorkerProfiles.SKILLS to listOf("DRIVER")))
        assertTrue(complete.isComplete)
        assertTrue(complete.completionPercent in 1..100)
    }

    @Test
    fun companyEmployerNeedsBusinessDetails() {
        val individual = EmployerProfile.from(
            "e1", mapOf(EmployerProfiles.OWNER_NAME to "Sita", EmployerProfiles.PHONE to "+919876543210")
        )
        assertTrue(individual.isComplete)
        assertEquals("Sita", individual.displayName)

        val company = EmployerProfile.from(
            "e2",
            mapOf(
                EmployerProfiles.EMPLOYER_TYPE to Values.EmployerType.COMPANY,
                EmployerProfiles.OWNER_NAME to "Sita",
                EmployerProfiles.PHONE to "+919876543210"
            )
        )
        assertFalse(company.isComplete)
        val filled = EmployerProfile.from(
            "e2",
            mapOf(
                EmployerProfiles.EMPLOYER_TYPE to Values.EmployerType.COMPANY,
                EmployerProfiles.OWNER_NAME to "Sita",
                EmployerProfiles.BUSINESS_NAME to "Sai Comforts",
                EmployerProfiles.BUSINESS_TYPE to "hospitality",
                EmployerProfiles.PHONE to "+919876543210"
            )
        )
        assertTrue(filled.isComplete)
        assertEquals("Sai Comforts", filled.displayName)
    }

    @Test
    fun experienceBucketsRoundTrip() {
        ExperienceBucket.entries.forEach { assertEquals(it, ExperienceBucket.forYears(it.years)) }
        assertEquals(ExperienceBucket.FIVE_PLUS, ExperienceBucket.forYears(12))
    }
}
