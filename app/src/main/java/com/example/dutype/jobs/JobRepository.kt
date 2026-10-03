package com.example.dutype.jobs

import android.util.LruCache
import com.example.dutype.firestore.FirestoreSchema.JobContacts
import com.example.dutype.firestore.FirestoreSchema.JobDetails
import com.example.dutype.firestore.FirestoreSchema.Jobs
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.utils.toJobListing
import com.example.dutype.utils.toJobListingSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything the app does with jobs. Reads go straight to Firestore (cards for lists, card +
 * details when a job is opened); every write goes through the jobs Cloud Functions, which own
 * validation, billing and the server-only fields.
 */
@Singleton
class JobRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    val places: PlaceRepository
) {
    private val openedJobs = LruCache<String, JobListing>(64)

    /** The worker feed around a point: 5/10/15/20 km bands, then district, then state (newest-first without one). */
    fun pager(lat: Double?, lng: Double?, query: JobQuery = JobQuery()): JobFeedPager =
        JobFeedPager(firestore, places, lat, lng, query)

    /**
     * Card + details (+ the contact number when the caller owns the job). Cached for the session
     * so reopening a job costs nothing.
     */
    suspend fun getJob(jobId: String, refresh: Boolean = false): Result<JobListing?> = runCatching {
        if (!refresh) openedJobs.get(jobId)?.let { return@runCatching it }
        coroutineScope {
            val card = async { firestore.collection(Jobs.COLLECTION).document(jobId).get().await() }
            val details = async { firestore.collection(JobDetails.COLLECTION).document(jobId).get().await() }
            val cardSnap = card.await()
            val data = cardSnap.data ?: return@coroutineScope null
            val job = data.toJobListing(jobId, details.await().data)
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val owned = if (uid != null && uid == job.employerId) {
                val contact = firestore.collection(JobContacts.COLLECTION).document(jobId).get().await()
                job.copy(contactNumber = contact.getString(JobContacts.CONTACT_NUMBER).orEmpty())
            } else job
            owned.also { openedJobs.put(jobId, it) }
        }
    }

    /**
     * Cards for a known set of ids (saved jobs, applications), 30 per query. Cards read in the
     * last [CARD_TTL_MS] come from memory, so a live list that re-emits re-reads nothing.
     */
    suspend fun getCards(jobIds: Collection<String>): Result<List<JobListingSummary>> = runCatching {
        val now = System.currentTimeMillis()
        val ids = jobIds.distinct()
        val missing = ids.filter { id -> cardCache.get(id)?.let { now - it.second > CARD_TTL_MS } ?: true }
        coroutineScope {
            missing.chunked(30).map { chunk ->
                async {
                    firestore.collection(Jobs.COLLECTION)
                        .whereIn(FieldPath.documentId(), chunk)
                        .get().await().documents
                        .mapNotNull { doc -> doc.data?.toJobListingSummary(doc.id) }
                }
            }.awaitAll().flatten()
        }.forEach { cardCache.put(it.id, it to now) }
        ids.mapNotNull { cardCache.get(it)?.first }
    }

    private val cardCache = LruCache<String, Pair<JobListingSummary, Long>>(500)

    /** The employer's own job cards, newest first, live. */
    fun employerJobs(employerId: String): Flow<Result<List<JobListingSummary>>> = callbackFlow {
        val registration = firestore.collection(Jobs.COLLECTION)
            .whereEqualTo(Jobs.EMPLOYER_ID, employerId)
            .orderBy(Jobs.CREATED_AT, Query.Direction.DESCENDING)
            .limit(EMPLOYER_JOBS_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                } else if (snapshot != null) {
                    trySend(Result.success(snapshot.documents.mapNotNull { it.data?.toJobListingSummary(it.id) }))
                }
            }
        awaitClose { registration.remove() }
    }

    // ───────────────────────────── writes (Cloud Functions) ─────────────────────────────

    /** Posts a job. [requestId] makes retries safe: the same id never creates a second job. */
    suspend fun postJob(form: JobForm, requestId: String = UUID.randomUUID().toString()): Result<String> =
        call("postJob", form.toPayload() + ("requestId" to requestId)).map { it["jobId"] as String }

    suspend fun updateJob(jobId: String, form: JobForm): Result<Unit> =
        call("updateJob", form.toPayload() + ("jobId" to jobId)).map { openedJobs.remove(jobId); Unit }

    suspend fun setStatus(jobId: String, status: String): Result<Unit> =
        call("setJobStatus", mapOf("jobId" to jobId, "status" to status)).map { openedJobs.remove(jobId); Unit }

    suspend fun renewJob(jobId: String): Result<Unit> =
        call("renewJob", mapOf("jobId" to jobId)).map { openedJobs.remove(jobId); Unit }

    suspend fun deleteJob(jobId: String): Result<Unit> =
        call("deleteJob", mapOf("jobId" to jobId)).map { openedJobs.remove(jobId); Unit }

    private suspend fun call(name: String, payload: Map<String, Any?>): Result<Map<String, Any?>> = runCatching {
        @Suppress("UNCHECKED_CAST")
        functions.getHttpsCallable(name).call(payload).await().data as? Map<String, Any?> ?: emptyMap()
    }

    private companion object {
        const val EMPLOYER_JOBS_LIMIT = 100L
        const val CARD_TTL_MS = 5 * 60 * 1000L
    }
}

/** The current job as a form — the base for an edit that changes only some fields. */
fun JobListing.toForm(): JobForm = JobForm(
    title = title,
    category = category,
    employmentType = employmentType,
    payAmount = payAmount,
    payType = payType,
    vacancies = vacancies,
    urgency = urgency,
    shift = shift,
    area = area,
    lat = lat,
    lng = lng,
    photoUrl = photoUrl,
    description = description,
    addressText = addressText,
    contactNumber = contactNumber,
    gender = gender.ifBlank { "ANY" },
    experienceRequired = experienceRequired,
    educationRequired = educationRequired,
    benefits = benefits
)

/** The employer-editable fields of a job, as the postJob / updateJob callables take them. */
data class JobForm(
    val title: String,
    val category: String,
    val employmentType: String,
    val payAmount: Long,
    val payType: String,
    val vacancies: Int,
    val urgency: String,
    val shift: String,
    val area: String,
    val lat: Double,
    val lng: Double,
    val photoUrl: String?,
    val description: String,
    val addressText: String,
    val contactNumber: String,
    val gender: String,
    val experienceRequired: String,
    val educationRequired: String,
    val benefits: List<String>,
    /** Shop / business name typed while posting; saved to the employer profile if it has none. */
    val businessName: String = ""
) {
    fun toPayload(): Map<String, Any?> = mapOf(
        Jobs.TITLE to title,
        Jobs.CATEGORY to category,
        Jobs.EMPLOYMENT_TYPE to employmentType,
        Jobs.PAY_AMOUNT to payAmount,
        Jobs.PAY_TYPE to payType,
        Jobs.VACANCIES to vacancies,
        Jobs.URGENCY to urgency,
        Jobs.SHIFT to shift,
        Jobs.AREA to area,
        Jobs.LAT to lat,
        Jobs.LNG to lng,
        Jobs.PHOTO_URL to photoUrl,
        JobDetails.DESCRIPTION to description,
        JobDetails.ADDRESS_TEXT to addressText,
        "contactNumber" to contactNumber,
        JobDetails.GENDER to gender,
        JobDetails.EXPERIENCE_REQUIRED to experienceRequired,
        JobDetails.EDUCATION_REQUIRED to educationRequired,
        JobDetails.BENEFITS to benefits,
        "businessName" to businessName
    )
}
