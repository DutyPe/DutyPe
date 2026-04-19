package com.example.dutype.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rating & Review Service
 * 
 * Dual-role support: Workers rate employers, Employers rate workers
 * Ratings are stored per-role so a dual-role user gets separate ratings
 * for their worker and employer roles.
 * 
 * Collection: ratings/{ratingId}
 * User fields updated: averageRating, totalRatings (role-specific via ratingsAsWorker/ratingsAsEmployer)
 */

data class Rating(
    val id: String = "",
    val jobId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val raterName: String = "",
    val rating: Int = 0,            // 1-5 stars
    val review: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class RatingResult(
    val success: Boolean,
    val message: String
)

@Singleton
class RatingService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        const val RATINGS_COLLECTION = "ratings"
        const val APPLICATIONS_COLLECTION = "applications"
        const val USERS_COLLECTION = "users"
    }

    private suspend fun resolveTargetUserId(
        jobId: String,
        providedTargetUserId: String,
        currentUserId: String
    ): String {
        val normalizedProvided = providedTargetUserId.trim()
        if (normalizedProvided.isNotBlank() && normalizedProvided != currentUserId) {
            return normalizedProvided
        }

        // Worker-side fallback: deterministic application id = {jobId}_{workerId}.
        val applicationDoc = firestore.collection(APPLICATIONS_COLLECTION)
            .document("${jobId}_${currentUserId}")
            .get()
            .await()

        if (applicationDoc.exists()) {
            val employerId = applicationDoc.getString("employerId").orEmpty().trim()
            if (employerId.isNotBlank() && employerId != currentUserId) {
                return employerId
            }
        }

        return normalizedProvided
    }

    /**
     * Submit a rating for a completed job.
     * Worker rates employer OR employer rates worker. `raterName` is denormalized
     * from the users collection at write time so reviews render without an extra read.
     */
    suspend fun submitRating(
        jobId: String,
        targetUserId: String,
        rating: Int,
        review: String = "",
        tags: List<String> = emptyList()
    ): Result<RatingResult> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))

            val resolvedTargetUserId = resolveTargetUserId(
                jobId = jobId,
                providedTargetUserId = targetUserId,
                currentUserId = currentUser.uid
            )

            if (resolvedTargetUserId.isBlank()) {
                return Result.failure(Exception("Unable to identify employer for this job. Please refresh and try again."))
            }

            if (currentUser.uid == resolvedTargetUserId) {
                return Result.success(RatingResult(false, "You cannot rate yourself"))
            }

            val ratingId = "${jobId}_${currentUser.uid}"

            // Block duplicate using deterministic doc id required by Firestore rules.
            val existing = firestore.collection(RATINGS_COLLECTION)
                .document(ratingId)
                .get()
                .await()

            if (existing.exists()) {
                return Result.success(RatingResult(false, "You have already rated this"))
            }

            val ratingData = mapOf(
                "jobId" to jobId,
                "fromUserId" to currentUser.uid,
                "toUserId" to resolvedTargetUserId,
                "rating" to rating,
                "review" to review,
                "createdAt" to Timestamp.now()
            )

            firestore.collection(RATINGS_COLLECTION)
                .document(ratingId)
                .set(ratingData)
                .await()

            Timber.d("⭐ Rating submitted: $rating stars for $resolvedTargetUserId")

            Result.success(RatingResult(true, "Rating submitted successfully!"))
        } catch (e: Exception) {
            Timber.e(e, "Failed to submit rating")
            Result.failure(e)
        }
    }

    /**
     * Update the target user's average rating and total ratings in the correct profile collection.
     * Workers → worker_profiles, Employers → employer_profiles (target schema)
     */
    /**
     * Check if the current user has already rated a target user for a specific job.
     */
    suspend fun hasRated(jobId: String, targetUserId: String): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val ratingId = "${jobId}_${currentUser.uid}"
            val snapshot = firestore.collection(RATINGS_COLLECTION)
                .document(ratingId)
                .get()
                .await()
            if (!snapshot.exists()) return false

            val expectedTarget = targetUserId.trim()
            expectedTarget.isBlank() || snapshot.getString("toUserId") == expectedTarget
        } catch (e: Exception) {
            Timber.e(e, "Error checking if rated")
            false
        }
    }

    /**
     * Get ratings received by a user. Single-field query (no composite index);
     * sort happens in memory.
     */
    suspend fun getUserRatings(userId: String): List<Rating> {
        return try {
            val snapshot = firestore.collection(RATINGS_COLLECTION)
                .whereEqualTo("toUserId", userId)
                .limit(100)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Rating(
                        id = doc.id,
                        jobId = data["jobId"] as? String ?: "",
                        fromUserId = data["fromUserId"] as? String ?: "",
                        toUserId = data["toUserId"] as? String ?: "",
                        raterName = data["raterName"] as? String ?: "",
                        rating = (data["rating"] as? Number)?.toInt() ?: 0,
                        review = data["review"] as? String ?: "",
                        tags = (data["tags"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
                        createdAt = when (val created = data["createdAt"]) {
                            is Number -> created.toLong()
                            is Timestamp -> created.toDate().time
                            else -> 0L
                        }
                    )
                }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user ratings for $userId")
            emptyList()
        }
    }
}
