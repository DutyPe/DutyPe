package com.example.dutype.jobs

import com.example.dutype.firestore.FirestoreSchema.Jobs
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.models.FeedSection
import com.example.dutype.models.JobListingSummary
import com.example.dutype.utils.toJobListingSummary
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * What a job list asks for. [category] and [urgentOnly] run on the server; [maxRadiusKm] limits
 * the feed to the km bands (no district / state fallback); [keyword] searches one word on the
 * server (`keywords array-contains`; category and urgent then run on the cards, to keep the index
 * count small); [matches] (pay, job type, shift, the other search words) runs on the loaded cards.
 */
data class JobQuery(
    val category: String? = null,
    val urgentOnly: Boolean = false,
    val maxRadiusKm: Double? = null,
    val keyword: String? = null,
    val matches: (JobListingSummary) -> Boolean = { true }
)

/** Splits search text the way the server builds `keywords` (lib/keywords.ts): lower-case words of 2+ letters. */
fun searchWords(text: String): List<String> =
    java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC).lowercase()
        .split(Regex("[^\\p{L}\\p{M}\\p{N}]+"))
        .filter { it.length >= 2 }
        .distinct()

/**
 * The worker job feed, read-once and page by page:
 *
 * 1. **0–5, 5–10, 10–15, 15–20 km.** Each band queries `cell in [...]` for the ~5 km cells
 *    (geohash5) touching its circle that no earlier band queried — at most 30 cells per query, so
 *    the whole 20 km is about 5 queries. Every job's exact distance is computed on the phone; a job
 *    in a corner of a cell but outside the band is held and shown in the band it belongs to, so
 *    nothing read is wasted and no job appears before a nearer band is finished. Within a band,
 *    jobs arrive newest first and each page is sorted by distance.
 * 2. **The rest of the worker's district**, then **the rest of their state** (newest first). Jobs
 *    in other states are never shown past 20 km.
 * Without a location it pages the newest open jobs anywhere.
 *
 * One [nextPage] at a time (the ViewModel serialises calls). A failed call keeps the position.
 */
class JobFeedPager internal constructor(
    private val firestore: FirebaseFirestore,
    private val places: PlaceRepository,
    private val lat: Double?,
    private val lng: Double?,
    private val query: JobQuery
) {
    data class Page(val jobs: List<JobListingSummary>, val hasMore: Boolean)

    /** One query (optionally over a set of cells) paged newest first. */
    private inner class Source(private val cells: List<String>?, private val field: String?, private val value: Any?) {
        private var after: DocumentSnapshot? = null
        var done = false
            private set

        suspend fun fetch(): List<DocumentSnapshot> {
            var q = baseQuery()
            if (cells != null) q = q.whereIn(Jobs.CELL, cells)
            if (field != null) q = q.whereEqualTo(field, value)
            q = q.orderBy(Jobs.CREATED_AT, Query.Direction.DESCENDING).limit(FETCH_SIZE)
            after?.let { q = q.startAfter(it) }
            val docs = q.get().await().documents
            if (docs.size < FETCH_SIZE) done = true else after = docs.last()
            return docs
        }
    }

    private val hasLocation = lat != null && lng != null
    private val approximate = hasLocation && places.isApproximate()
    private val bands: List<FeedSection> = if (!hasLocation) emptyList() else {
        val cap = (query.maxRadiusKm ?: MAX_BAND_KM).coerceAtMost(MAX_BAND_KM)
        FeedSection.BANDS.filter { it.maxKm!! <= cap }.ifEmpty { listOf(FeedSection.KM_5) }
    }
    private val stages: List<FeedSection> = when {
        !hasLocation -> listOf(FeedSection.ANYWHERE)
        query.maxRadiusKm != null -> bands
        else -> bands + listOf(FeedSection.DISTRICT, FeedSection.STATE)
    }
    private var stageIndex = -1
    private var section: FeedSection? = null
    private val sources = ArrayDeque<Source>()
    private val queriedCells = HashSet<String>()
    private val seen = HashSet<String>()
    private val ready = ArrayDeque<JobListingSummary>()
    /** Read in a band but farther than it: shown in the band (or place section) it belongs to. */
    private val held = ArrayList<JobListingSummary>()
    private var place: PlaceRepository.Place? = null

    suspend fun nextPage(pageSize: Int): Result<Page> = runCatching {
        val out = ArrayList<JobListingSummary>(pageSize)
        var reads = 0
        while (out.size < pageSize) {
            val next = ready.removeFirstOrNull()
            if (next != null) {
                out.add(next)
                continue
            }
            if (reads >= READ_BUDGET) break
            val source = sources.firstOrNull { !it.done }
            if (source == null) {
                if (!openNextStage()) break
                continue
            }
            val docs = source.fetch()
            reads += docs.size.coerceAtLeast(1)
            accept(docs)
        }
        Page(out, hasMore = ready.isNotEmpty() || sources.any { !it.done } || stageIndex < stages.lastIndex)
    }

    /** Moves to the next section. False when the feed is finished. */
    private suspend fun openNextStage(): Boolean {
        if (stageIndex >= stages.lastIndex) return false
        val next = stages[stageIndex + 1]
        // Resolve the worker's district / state before committing, safely handling network or function errors.
        if ((next == FeedSection.DISTRICT || next == FeedSection.STATE) && place == null) {
            place = runCatching { places.placeFor(lat!!, lng!!) }.getOrNull()
        }
        stageIndex++
        section = next
        sources.clear()
        when (next) {
            FeedSection.KM_5, FeedSection.KM_10, FeedSection.KM_15, FeedSection.KM_20 -> {
                val radius = next.maxKm!!
                val cells = Geohash.covering(lat!!, lng!!, radius, PlaceRepository.CELL_PRECISION)
                    .filter { queriedCells.add(it) }
                    .sortedBy { Geohash.decode(it).distanceKmFrom(lat, lng) }
                cells.chunked(MAX_IN_VALUES).forEach { sources.add(Source(it, null, null)) }
                release { (it.distance ?: Double.MAX_VALUE) <= radius }
            }
            FeedSection.DISTRICT -> {
                val id = place?.districtId
                release { id != null && it.district.isNotBlank() && it.district == place?.district }
                if (id != null) sources.add(Source(null, Jobs.DISTRICT_ID, id))
            }
            FeedSection.STATE -> {
                val id = place?.stateId
                release { id != null && it.state.isNotBlank() && it.state == place?.state }
                held.clear()   // anything left is past 20 km in another state
                if (id != null) sources.add(Source(null, Jobs.STATE_ID, id))
            }
            FeedSection.ANYWHERE -> sources.add(Source(null, null, null))
        }
        return true
    }

    /** Moves held jobs that belong to the current section into [ready], nearest first. */
    private fun release(belongs: (JobListingSummary) -> Boolean) {
        val now = held.filter(belongs).sortedBy { it.distance ?: Double.MAX_VALUE }
        held.removeAll(now.toSet())
        now.forEach { it.feedSection = section }
        ready.addAll(now)
    }

    private fun accept(docs: List<DocumentSnapshot>) {
        val now = System.currentTimeMillis()
        val current = section ?: return
        val radius = current.maxKm
        val batch = ArrayList<JobListingSummary>(docs.size)
        docs.forEach { doc ->
            if (!seen.add(doc.id)) return@forEach
            val job = doc.data?.toJobListingSummary(doc.id) ?: return@forEach
            if (job.expiresAt in 1 until now || !query.matches(job)) return@forEach
            if (query.keyword != null && (
                    (query.category != null && job.category != query.category) ||
                        (query.urgentOnly && !job.isUrgent))
            ) return@forEach
            if (hasLocation) {
                job.distance = Geohash.distanceKm(lat!!, lng!!, job.lat, job.lng)
                job.distanceApprox = approximate
            }
            if (radius != null && (job.distance ?: Double.MAX_VALUE) > radius) held.add(job) else batch.add(job)
        }
        batch.sortBy { it.distance ?: Double.MAX_VALUE }
        batch.forEach { it.feedSection = current }
        ready.addAll(batch)
    }

    private fun baseQuery(): Query {
        var q: Query = firestore.collection(Jobs.COLLECTION).whereEqualTo(Jobs.STATUS, Values.JobStatus.OPEN)
        if (query.keyword != null) return q.whereArrayContains(Jobs.KEYWORDS, query.keyword)
        query.category?.let { q = q.whereEqualTo(Jobs.CATEGORY, it) }
        if (query.urgentOnly) q = q.whereEqualTo(Jobs.URGENCY, Values.Urgency.HIGH)
        return q
    }

    private companion object {
        const val MAX_BAND_KM = 20.0
        /** Firestore's limit for one `in` filter. */
        const val MAX_IN_VALUES = 30
        const val FETCH_SIZE = 20L
        /** Max job documents one page may read (district / state pages skip jobs already shown). */
        const val READ_BUDGET = 120
    }
}
