package com.example.dutype.services

import com.example.dutype.firestore.FirestoreSchema.EmployerCards
import com.example.dutype.firestore.FirestoreSchema.Ratings
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.firestore.FirestoreSchema.WorkerCards
import com.example.dutype.utils.epochMillis
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** A rating as shown in review lists; names are joined from the public profiles/cards. */
data class Rating(
    val id: String = "",
    val jobId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val raterName: String = "",
    val raterCompanyName: String = "",
    val targetName: String = "",
    val targetCompanyName: String = "",
    val targetRole: String = "",
    val rating: Int = 0,
    val review: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class RatingResult(
    val success: Boolean,
    val message: String
)

/**
 * Workers and employers rate each other after work is done. Ratings are written only by the
 * `submitRating` Cloud Function (ratings/{jobOrRequestId}_{workerId}_{raterRole}), which also updates
 * the target's average (worker_cards / employer_profiles).
 */
@Singleton
class RatingService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) {
    /**
     * [jobId] is the job id, or the urgent need id when [source] is "instant".
     * [targetRole] is the role of the person being rated.
     */
    suspend fun submitRating(
        jobId: String,
        targetUserId: String,
        rating: Int,
        review: String = "",
        tags: List<String> = emptyList(),
        targetRole: String? = null,
        source: String = SOURCE_JOB
    ): Result<RatingResult> = runCatching {
        val data = functions.getHttpsCallable("submitRating").call(
            mapOf(
                "source" to source,
                "id" to jobId,
                "targetId" to targetUserId,
                "stars" to rating,
                "review" to review.trim(),
                "tags" to tags.take(10)
            )
        ).await().data as? Map<*, *>
        if (data?.get("duplicate") == true) RatingResult(false, "You have already rated this")
        else RatingResult(true, "Rating submitted successfully!")
    }.onFailure { Timber.e(it, "submitRating failed") }

    /** One document read (the rating id is deterministic). */
    suspend fun hasRated(jobId: String, targetUserId: String): Boolean = runCatching {
        val me = auth.currentUser ?: return false
        val myRole = me.getIdToken(false).await().claims["role"] as? String
        val ratingId = if (myRole == Values.Role.EMPLOYER) {
            "${jobId}_${targetUserId}_${Values.Role.EMPLOYER}"
        } else {
            "${jobId}_${me.uid}_${Values.Role.WORKER}"
        }
        firestore.collection(Ratings.COLLECTION).document(ratingId).get().await().exists()
    }.getOrDefault(false)

    /** Reviews received by [userId], newest first. */
    suspend fun getUserRatings(userId: String): List<Rating> = load(Ratings.TARGET_ID, userId, joinRaters = true)

    /** Reviews written by [userId], newest first. */
    suspend fun getRatingsGivenByUser(userId: String): List<Rating> = load(Ratings.RATER_ID, userId, joinRaters = false)

    private suspend fun load(field: String, userId: String, joinRaters: Boolean): List<Rating> = runCatching {
        val docs = firestore.collection(Ratings.COLLECTION)
            .whereEqualTo(field, userId)
            .orderBy(Ratings.CREATED_AT, Query.Direction.DESCENDING)
            .limit(50)
            .get().await().documents
        val ratings = docs.mapNotNull { it.toRating() }
        val otherIds = ratings.map { if (joinRaters) it.fromUserId else it.toUserId }
        val names = names(otherIds)
        ratings.map { r ->
            val other = names[if (joinRaters) r.fromUserId else r.toUserId]
            if (joinRaters) r.copy(raterName = other?.first.orEmpty(), raterCompanyName = other?.second.orEmpty())
            else r.copy(targetName = other?.first.orEmpty(), targetCompanyName = other?.second.orEmpty())
        }
    }.getOrElse {
        Timber.e(it, "Failed to load ratings for $userId")
        emptyList()
    }

    /**
     * uid → (person name, business name). Employers come from employer_cards (30 ids per query);
     * the rest are worker cards, fetched one by one because worker cards cannot be listed.
     */
    private suspend fun names(ids: List<String>): Map<String, Pair<String, String>> = coroutineScope {
        val unique = ids.distinct().filter(String::isNotBlank)
        val employers = unique.chunked(30).map { chunk ->
            async {
                firestore.collection(EmployerCards.COLLECTION).whereIn(FieldPath.documentId(), chunk).limit(30).get().await()
                    .documents.mapNotNull { doc ->
                        doc.getString(EmployerCards.NAME)?.let { doc.id to (it to it) }
                    }.toMap()
            }
        }.awaitAll().fold(emptyMap<String, Pair<String, String>>()) { acc, m -> acc + m }
        val workers = unique.filter { it !in employers }.map { id ->
            async {
                firestore.collection(WorkerCards.COLLECTION).document(id).get().await()
                    .getString(WorkerCards.NAME)?.let { id to (it to "") }
            }
        }.awaitAll().filterNotNull().toMap()
        employers + workers
    }

    private fun DocumentSnapshot.toRating(): Rating? {
        val d = data ?: return null
        // id = {jobId}_{workerId}_{raterRole}; job ids and uids contain no "_"-free guarantee, so split from the end.
        val raterRole = id.substringAfterLast('_')
        val withoutRole = id.substringBeforeLast('_')
        return Rating(
            id = id,
            jobId = withoutRole.substringBeforeLast('_'),
            fromUserId = d[Ratings.RATER_ID] as? String ?: "",
            toUserId = d[Ratings.TARGET_ID] as? String ?: "",
            targetRole = if (raterRole == Values.Role.WORKER) Values.Role.EMPLOYER else Values.Role.WORKER,
            rating = (d[Ratings.STARS] as? Number)?.toInt() ?: 0,
            review = d[Ratings.REVIEW] as? String ?: "",
            tags = (d[Ratings.TAGS] as? List<*>)?.filterIsInstance<String>().orEmpty(),
            createdAt = d[Ratings.CREATED_AT].epochMillis()
        )
    }

    companion object {
        const val SOURCE_JOB = "job"
        const val SOURCE_INSTANT = "instant"
    }
}
