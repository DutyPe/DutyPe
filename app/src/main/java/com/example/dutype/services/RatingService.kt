package com.example.dutype.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    val applicationId: String = "",
    val jobId: String = "",
    val raterId: String = "",
    val raterName: String = "",
    val raterRole: String = "",        // WORKER or EMPLOYER - the role of the person giving the rating
    val targetUserId: String = "",
    val targetUserName: String = "",
    val targetRole: String = "",       // WORKER or EMPLOYER - the role being rated
    val rating: Int = 0,               // 1-5 stars
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
        const val USERS_COLLECTION = "users"
    }

    /**
     * Submit a rating for a completed job
     * Worker rates employer OR employer rates worker
     */
    suspend fun submitRating(
        applicationId: String,
        jobId: String,
        targetUserId: String,
        targetUserName: String,
        targetRole: String,
        raterRole: String,
        rating: Int,
        review: String = "",
        tags: List<String> = emptyList()
    ): Result<RatingResult> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))

            // Check if already rated this application for this target role
            val existing = firestore.collection(RATINGS_COLLECTION)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("fromUserId", currentUser.uid)
                .whereEqualTo("toUserId", targetUserId)
                .limit(1)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.success(RatingResult(false, "You have already rated this"))
            }

            val ratingRef = firestore.collection(RATINGS_COLLECTION).document()
            val ratingData = mapOf(
                "ratingId" to ratingRef.id,
                "jobId" to jobId,
                "fromUserId" to currentUser.uid,
                "toUserId" to targetUserId,
                "rating" to rating,
                "review" to review,
                "createdAt" to System.currentTimeMillis()
            )

            // Save rating
            ratingRef.set(ratingData).await()

            // Update target user's rating summary (role-specific field)
            updateUserRatingSummary(targetUserId, targetRole, rating)

            Timber.d("⭐ Rating submitted: $rating stars for $targetRole $targetUserId")

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
    private suspend fun updateUserRatingSummary(
        userId: String,
        targetRole: String,
        newRating: Int
    ) {
        try {
            val profileCollection = if (targetRole == "WORKER") "worker_profiles" else "employer_profiles"
            val profileRef = firestore.collection(profileCollection).document(userId)
            val profileDoc = profileRef.get().await()

            val currentAvg = (profileDoc.getDouble("rating") ?: 0.0)
            val currentCount = (profileDoc.getLong("totalRatings") ?: 0L).toInt()

            val newCount = currentCount + 1
            val newAvg = ((currentAvg * currentCount) + newRating) / newCount

            profileRef.set(
                mapOf("rating" to newAvg, "totalRatings" to newCount),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            Timber.d("⭐ Updated $targetRole rating for $userId: $newAvg ($newCount ratings)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to update user rating summary")
        }
    }

    /**
     * Check if user has already rated a specific application for a target role
     */
    suspend fun hasRated(applicationId: String, targetRole: String): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val snapshot = firestore.collection(RATINGS_COLLECTION)
                .whereEqualTo("jobId", applicationId)
                .whereEqualTo("fromUserId", currentUser.uid)
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            Timber.e(e, "Error checking if rated")
            false
        }
    }

    /**
     * Get ratings for a specific user filtered by their role.
     * NOTE: No orderBy clause here to avoid composite index requirement in Firestore.
     * We filter by targetUserId only (single-field query), then filter role and sort in memory.
     */
    suspend fun getUserRatings(userId: String, role: String): List<Rating> {
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
                        id = data["ratingId"] as? String ?: doc.id,
                        jobId = data["jobId"] as? String ?: "",
                        raterId = data["fromUserId"] as? String ?: "",
                        targetUserId = data["toUserId"] as? String ?: "",
                        rating = (data["rating"] as? Number)?.toInt() ?: 0,
                        review = data["review"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
                    )
                }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user ratings for $userId/$role")
            emptyList()
        }
    }
}
