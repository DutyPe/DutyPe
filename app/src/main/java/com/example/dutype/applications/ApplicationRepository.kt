package com.example.dutype.applications

import com.example.dutype.firestore.FirestoreSchema.Applications
import com.example.dutype.firestore.FirestoreSchema.WorkerCards
import com.example.dutype.jobs.JobRepository
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.WorkerCard
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applications: live lists read straight from Firestore (only changed documents are re-read),
 * every write through the applications Cloud Functions.
 *
 * The worker's list joins the live job cards (one query per 30 jobs) so title, pay, area and
 * the job's current open/filled/expired status are always current.
 */
@Singleton
class ApplicationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val jobRepository: JobRepository
) {
    private val collection get() = firestore.collection(Applications.COLLECTION)

    // ─────────────────────────────── reads ───────────────────────────────

    /** The worker's applications, newest first, each with its live job card. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun workerApplications(workerId: String): Flow<Result<List<JobApplication>>> =
        listen(collection.whereEqualTo(Applications.WORKER_ID, workerId)
            .orderBy(Applications.CREATED_AT, Query.Direction.DESCENDING)
            .limit(WORKER_LIMIT))
            .mapLatest { result -> result.mapCatching { withJobs(it) } }

    /** Applicants for one of the employer's jobs, newest first. */
    fun jobApplicants(employerId: String, jobId: String): Flow<Result<List<JobApplication>>> =
        listen(collection.whereEqualTo(Applications.EMPLOYER_ID, employerId)
            .whereEqualTo(Applications.JOB_ID, jobId)
            .orderBy(Applications.CREATED_AT, Query.Direction.DESCENDING)
            .limit(EMPLOYER_LIMIT))

    /** Every applicant across the employer's jobs, newest first. */
    fun employerApplications(employerId: String): Flow<Result<List<JobApplication>>> =
        listen(collection.whereEqualTo(Applications.EMPLOYER_ID, employerId)
            .orderBy(Applications.CREATED_AT, Query.Direction.DESCENDING)
            .limit(EMPLOYER_LIMIT))

    suspend fun getApplication(applicationId: String): Result<JobApplication?> = runCatching {
        val snap = collection.document(applicationId).get().await()
        snap.data?.let { JobApplication.from(snap.id, it) }
    }

    /**
     * Public worker cards for applicants, fetched one by one in parallel (1 read each, same as a
     * batch query). Worker cards cannot be listed, so nobody can page through every worker.
     */
    suspend fun workerCards(workerIds: Collection<String>): Result<Map<String, WorkerCard>> = runCatching {
        coroutineScope {
            workerIds.distinct().filter(String::isNotBlank).map { id ->
                async {
                    val doc = firestore.collection(WorkerCards.COLLECTION).document(id).get().await()
                    doc.data?.let { WorkerCard.from(doc.id, it) }
                }
            }.awaitAll().filterNotNull().associateBy { it.uid }
        }
    }

    /**
     * The worker tapped Call: records the call (a first call creates the application) and returns
     * the employer's number, which is never readable from Firestore by workers.
     */
    suspend fun callEmployer(jobId: String): Result<String> =
        call("applyToJob", mapOf("jobId" to jobId, "viaCall" to true))
            .map { (it["contactNumber"] as? String).orEmpty() }

    // ─────────────────────────────── writes ───────────────────────────────

    /** Apply to a job. With [viaCall] the call is counted and a first call creates the application. */
    suspend fun apply(jobId: String, viaCall: Boolean = false): Result<String> =
        call("applyToJob", mapOf("jobId" to jobId, "viaCall" to viaCall)).map { it["applicationId"] as String }

    suspend fun withdraw(jobId: String): Result<Unit> =
        call("withdrawApplication", mapOf("jobId" to jobId)).map { }

    suspend fun setStatus(applicationId: String, status: ApplicationStatus): Result<Unit> =
        call("setApplicationStatus", mapOf("applicationId" to applicationId, "status" to status.key)).map { }

    // ─────────────────────────────── internals ───────────────────────────────

    private fun listen(query: Query): Flow<Result<List<JobApplication>>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) trySend(Result.failure(error))
            else if (snapshot != null) {
                trySend(Result.success(snapshot.documents.mapNotNull { doc -> doc.data?.let { JobApplication.from(doc.id, it) } }))
            }
        }
        awaitClose { registration.remove() }
    }

    private suspend fun withJobs(applications: List<JobApplication>): List<JobApplication> {
        if (applications.isEmpty()) return applications
        val cards = jobRepository.getCards(applications.map { it.jobId }).getOrDefault(emptyList()).associateBy { it.id }
        return applications.map { it.copy(job = cards[it.jobId]) }
    }

    private suspend fun call(name: String, payload: Map<String, Any?>): Result<Map<String, Any?>> = runCatching {
        @Suppress("UNCHECKED_CAST")
        functions.getHttpsCallable(name).call(payload).await().data as? Map<String, Any?> ?: emptyMap()
    }

    private companion object {
        const val WORKER_LIMIT = 100L
        const val EMPLOYER_LIMIT = 200L
    }
}
