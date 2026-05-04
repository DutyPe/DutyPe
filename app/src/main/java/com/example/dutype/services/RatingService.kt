package com.example.dutype.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rating & Review Service
 * 
 * Workers rate employers, and employers rate workers.
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
    val raterCompanyName: String = "",
    val targetName: String = "",
    val targetCompanyName: String = "",
    val targetRole: String = "",
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
        const val INSTANT_RESPONSES_COLLECTION = "instant_responses"
        const val EMPLOYER_PROFILES_COLLECTION = "employer_profiles"
        const val WORKER_PROFILES_COLLECTION = "worker_profiles"
    }

    private suspend fun resolveRaterIdentity(userId: String): Pair<String, String> {
        var raterName = auth.currentUser?.displayName.orEmpty().trim()
        var companyName = ""

        runCatching {
            val workerDoc = firestore.collection(WORKER_PROFILES_COLLECTION).document(userId).get().await()
            raterName = workerDoc.getString("fullName")?.trim()?.takeIf { it.isNotBlank() }
                ?: workerDoc.getString("name")?.trim()?.takeIf { it.isNotBlank() }
                ?: raterName
        }

        runCatching {
            val employerDoc = firestore.collection(EMPLOYER_PROFILES_COLLECTION).document(userId).get().await()
            companyName = employerDoc.getString("companyName")?.trim().orEmpty()
            raterName = companyName.takeIf { it.isNotBlank() }
                ?: employerDoc.getString("fullName")?.trim()?.takeIf { it.isNotBlank() }
                ?: employerDoc.getString("name")?.trim()?.takeIf { it.isNotBlank() }
                ?: raterName
        }

        return raterName.ifBlank { "DutyPe user" } to companyName
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

    private suspend fun resolveTargetIdentity(
        jobId: String,
        currentUserId: String,
        targetUserId: String
    ): Pair<String, String> {
        val candidateApplicationIds = listOf(
            "${jobId}_${currentUserId}",
            "${jobId}_${targetUserId}"
        ).distinct()

        for (applicationId in candidateApplicationIds) {
            val applicationDoc = runCatching {
                firestore.collection(APPLICATIONS_COLLECTION).document(applicationId).get().await()
            }.getOrNull() ?: continue

            if (!applicationDoc.exists()) continue

            val workerId = applicationDoc.getString("workerId").orEmpty()
            val employerId = applicationDoc.getString("employerId").orEmpty()
            val workerName = applicationDoc.getString("workerName").orEmpty().trim()
            val companyName = applicationDoc.getString("companyName").orEmpty().trim()

            if (targetUserId == workerId) {
                return workerName.ifBlank { "Worker" } to ""
            }
            if (targetUserId == employerId) {
                return companyName.ifBlank { "Employer" } to companyName
            }
        }

        val candidateInstantResponseIds = listOf(
            "${jobId}_${currentUserId}",
            "${jobId}_${targetUserId}"
        ).distinct()

        for (responseId in candidateInstantResponseIds) {
            val responseDoc = runCatching {
                firestore.collection(INSTANT_RESPONSES_COLLECTION).document(responseId).get().await()
            }.getOrNull() ?: continue

            if (!responseDoc.exists()) continue

            val workerId = responseDoc.getString("workerId").orEmpty()
            val employerId = responseDoc.getString("employerId").orEmpty()
            val workerName = responseDoc.getString("workerName").orEmpty().trim()

            if (targetUserId == workerId) {
                return workerName.ifBlank { "Worker" } to ""
            }
            if (targetUserId == employerId) {
                return "Employer" to ""
            }
        }

        return "DutyPe user" to ""
    }

    private fun normalizeTargetRole(targetRole: String?): String {
        return when (targetRole?.trim()?.uppercase(Locale.US)) {
            "WORKER" -> "WORKER"
            "EMPLOYER" -> "EMPLOYER"
            else -> ""
        }
    }

    /**
     * Submit a rating for a completed job.
     * Worker rates employer OR employer rates worker. `raterName` is denormalized
    * from role profile collections at write time so reviews render without an extra read.
     */
    suspend fun submitRating(
        jobId: String,
        targetUserId: String,
        rating: Int,
        review: String = "",
        tags: List<String> = emptyList(),
        targetRole: String? = null
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

            val (raterName, raterCompanyName) = resolveRaterIdentity(currentUser.uid)
            val (targetName, targetCompanyName) = resolveTargetIdentity(
                jobId = jobId,
                currentUserId = currentUser.uid,
                targetUserId = resolvedTargetUserId
            )
            val normalizedTargetRole = normalizeTargetRole(targetRole)

            val ratingData = mutableMapOf<String, Any>(
                "jobId" to jobId,
                "fromUserId" to currentUser.uid,
                "toUserId" to resolvedTargetUserId,
                "rating" to rating,
                "review" to review,
                "raterName" to raterName,
                "targetName" to targetName,
                "createdAt" to Timestamp.now()
            )
            if (normalizedTargetRole.isNotBlank()) {
                ratingData["targetRole"] = normalizedTargetRole
            }
            if (raterCompanyName.isNotBlank()) {
                ratingData["raterCompanyName"] = raterCompanyName
            }
            if (targetCompanyName.isNotBlank()) {
                ratingData["targetCompanyName"] = targetCompanyName
            }
            if (tags.isNotEmpty()) {
                ratingData["tags"] = tags.take(10)
            }

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
                        raterCompanyName = data["raterCompanyName"] as? String ?: "",
                        targetName = data["targetName"] as? String ?: "",
                        targetCompanyName = data["targetCompanyName"] as? String ?: "",
                        targetRole = data["targetRole"] as? String ?: "",
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

    suspend fun getRatingsGivenByUser(userId: String): List<Rating> {
        return try {
            val snapshot = firestore.collection(RATINGS_COLLECTION)
                .whereEqualTo("fromUserId", userId)
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
                        raterCompanyName = data["raterCompanyName"] as? String ?: "",
                        targetName = data["targetName"] as? String ?: "",
                        targetCompanyName = data["targetCompanyName"] as? String ?: "",
                        targetRole = data["targetRole"] as? String ?: "",
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
            Timber.e(e, "Failed to get ratings given by $userId")
            emptyList()
        }
    }
}
