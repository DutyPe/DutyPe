package com.example.dutype.repositories

import com.example.dutype.firestore.FirestoreSchema.SavedJobs
import com.example.dutype.jobs.JobRepository
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.utils.toJobListing
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saved jobs: `saved_jobs/{uid_jobId}` holds only the link; the cards are read in batches of 30
 * through [JobRepository.getCards], so a list of N saved jobs costs about N + N/30 reads.
 * Saved jobs that were closed or expired stay in the list with their status.
 */
@Singleton
class FirestoreSavedJobRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val jobRepository: JobRepository,
    private val auth: FirebaseAuth
) {
    private fun uid(): String? = auth.currentUser?.uid

    private fun doc(uid: String, jobId: String) =
        firestore.collection(SavedJobs.COLLECTION).document("${uid}_$jobId")

    fun getSavedJobSummaries(): Flow<Result<List<JobListingSummary>>> = flow {
        val uid = uid() ?: return@flow emit(Result.failure(IllegalStateException("User not authenticated")))
        emit(runCatching {
            val jobIds = firestore.collection(SavedJobs.COLLECTION)
                .whereEqualTo(SavedJobs.USER_ID, uid)
                .orderBy(SavedJobs.CREATED_AT, Query.Direction.DESCENDING)
                .limit(MAX_SAVED)
                .get().await().documents
                .mapNotNull { it.getString(SavedJobs.JOB_ID) }
            val cards = jobRepository.getCards(jobIds).getOrThrow().associateBy { it.id }
            jobIds.mapNotNull { cards[it]?.copy(isSaved = true) }
        })
    }.flowOn(Dispatchers.IO)

    fun getSavedJobs(): Flow<Result<List<JobListing>>> = flow {
        getSavedJobSummaries().collect { result -> emit(result.map { list -> list.map { it.toJobListing() } }) }
    }

    suspend fun saveJob(jobId: String): Result<Unit> = runCatching {
        val uid = uid() ?: throw IllegalStateException("User not authenticated")
        doc(uid, jobId).set(
            mapOf(SavedJobs.USER_ID to uid, SavedJobs.JOB_ID to jobId, SavedJobs.CREATED_AT to Timestamp.now())
        ).await()
    }

    suspend fun unsaveJob(jobId: String): Result<Unit> = runCatching {
        val uid = uid() ?: throw IllegalStateException("User not authenticated")
        doc(uid, jobId).delete().await()
    }

    suspend fun isJobSaved(jobId: String): Result<Boolean> = runCatching {
        val uid = uid() ?: throw IllegalStateException("User not authenticated")
        doc(uid, jobId).get().await().exists()
    }

    private companion object {
        const val MAX_SAVED = 200L
    }
}
