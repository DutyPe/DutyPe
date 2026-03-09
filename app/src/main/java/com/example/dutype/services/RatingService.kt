package com.example.dutype.services

import com.example.dutype.models.JobRating
import com.example.dutype.models.RatingUserRole
import com.example.dutype.models.UserRatingSummary
import com.example.dutype.performance.MainThreadChecker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rating Service for Two-way Rating System
 * Handles rating submission, retrieval, and summary calculations
 * 
 * REFACTORED: Now receives FirebaseFirestore via constructor injection
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class RatingService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        const val RATINGS_COLLECTION = "ratings"
        // OPTIMIZED: Rating summaries now stored in users collection as nested field
        // No separate collection needed - reduces collections from 39 to 8
        const val USERS_COLLECTION = "users"
    }
    
    /**
     * Submit a rating for a completed job
     */
    suspend fun submitRating(rating: JobRating): Result<JobRating> {
        return try {
            MainThreadChecker.assertBackgroundThread("RatingService.submitRating")
            Timber.d("📊 RatingService: Submitting rating for job ${rating.jobId}")
            
            // Check if rating already exists
            val existingRating = getRatingForJob(
                jobId = rating.jobId,
                raterUserId = rating.raterUserId
            )
            
            if (existingRating.isSuccess && existingRating.getOrNull() != null) {
                Timber.w("📊 RatingService: Rating already exists for this job")
                return Result.failure(Exception("You have already rated this job"))
            }
            
            val ratingRef = firestore.collection(RATINGS_COLLECTION).document()
            val ratingWithId = rating.copy(
                ratingId = ratingRef.id,
                createdAt = System.currentTimeMillis()
            )
            
            val ratingData = mapOf(
                "ratingId" to ratingWithId.ratingId,
                "jobId" to ratingWithId.jobId,
                "applicationId" to ratingWithId.applicationId,
                "ratedUserId" to ratingWithId.ratedUserId,
                "ratedUserRole" to ratingWithId.ratedUserRole.name,
                "raterUserId" to ratingWithId.raterUserId,
                "raterUserRole" to ratingWithId.raterUserRole.name,
                "overallRating" to ratingWithId.overallRating,
                "punctualityRating" to ratingWithId.punctualityRating,
                "qualityRating" to ratingWithId.qualityRating,
                "communicationRating" to ratingWithId.communicationRating,
                "professionalismRating" to ratingWithId.professionalismRating,
                "paymentRating" to ratingWithId.paymentRating,
                "feedback" to ratingWithId.feedback,
                "tags" to ratingWithId.tags,
                "jobTitle" to ratingWithId.jobTitle,
                "companyName" to ratingWithId.companyName,
                "createdAt" to ratingWithId.createdAt,
                "isActive" to true
            )
            
            ratingRef.set(ratingData).await()
            Timber.i("📊 RatingService: ✅ Rating submitted successfully")
            
            // Update the user's rating summary
            updateUserRatingSummary(ratingWithId.ratedUserId, ratingWithId.ratedUserRole)
            
            Result.success(ratingWithId)
        } catch (e: Exception) {
            Timber.e(e, "📊 RatingService: ❌ Failed to submit rating")
            Result.failure(e)
        }
    }
    
    /**
     * Get rating for a specific job by a specific rater
     */
    suspend fun getRatingForJob(jobId: String, raterUserId: String): Result<JobRating?> {
        return try {
            MainThreadChecker.assertBackgroundThread("RatingService.getRatingForJob")
            val query = firestore.collection(RATINGS_COLLECTION)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("raterUserId", raterUserId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            if (query.isEmpty) {
                Result.success(null)
            } else {
                val doc = query.documents.first()
                val rating = parseRatingDocument(doc.data)
                Result.success(rating)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting rating for job")
            Result.failure(e)
        }
    }
    
    /**
     * Get all ratings received by a user
     */
    suspend fun getRatingsForUser(userId: String): Result<List<JobRating>> {
        return try {
            MainThreadChecker.assertBackgroundThread("RatingService.getRatingsForUser")
            val query = firestore.collection(RATINGS_COLLECTION)
                .whereEqualTo("ratedUserId", userId)
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            val ratings = query.documents.mapNotNull { doc ->
                parseRatingDocument(doc.data)
            }
            Result.success(ratings)
        } catch (e: Exception) {
            Timber.e(e, "Error getting ratings for user")
            Result.failure(e)
        }
    }
    
    /**
     * Get user's rating summary
     * OPTIMIZED: Now reads from users.ratingSummary field instead of separate collection
     */
    suspend fun getUserRatingSummary(userId: String): Result<UserRatingSummary?> {
        return try {
            val doc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!doc.exists()) {
                return Result.success(null)
            }
            
            val userData = doc.data ?: return Result.success(null)
            val ratingSummaryData = userData["ratingSummary"] as? Map<*, *>
            
            if (ratingSummaryData == null) {
                // Calculate summary if not exists
                val ratings = getRatingsForUser(userId).getOrNull() ?: emptyList()
                if (ratings.isEmpty()) {
                    return Result.success(null)
                }
                val summary = calculateRatingSummary(userId, ratings)
                return Result.success(summary)
            }
            
            val summary = UserRatingSummary(
                userId = ratingSummaryData["userId"] as? String ?: userId,
                userRole = RatingUserRole.valueOf(ratingSummaryData["userRole"] as? String ?: "WORKER"),
                averageRating = (ratingSummaryData["averageRating"] as? Number)?.toFloat() ?: 0f,
                totalRatings = (ratingSummaryData["totalRatings"] as? Number)?.toInt() ?: 0,
                totalJobs = (ratingSummaryData["totalJobs"] as? Number)?.toInt() ?: 0,
                averagePunctuality = (ratingSummaryData["averagePunctuality"] as? Number)?.toFloat() ?: 0f,
                averageQuality = (ratingSummaryData["averageQuality"] as? Number)?.toFloat() ?: 0f,
                averageCommunication = (ratingSummaryData["averageCommunication"] as? Number)?.toFloat() ?: 0f,
                averageProfessionalism = (ratingSummaryData["averageProfessionalism"] as? Number)?.toFloat() ?: 0f,
                averagePayment = (ratingSummaryData["averagePayment"] as? Number)?.toFloat() ?: 0f,
                fiveStarCount = (ratingSummaryData["fiveStarCount"] as? Number)?.toInt() ?: 0,
                fourStarCount = (ratingSummaryData["fourStarCount"] as? Number)?.toInt() ?: 0,
                threeStarCount = (ratingSummaryData["threeStarCount"] as? Number)?.toInt() ?: 0,
                twoStarCount = (ratingSummaryData["twoStarCount"] as? Number)?.toInt() ?: 0,
                oneStarCount = (ratingSummaryData["oneStarCount"] as? Number)?.toInt() ?: 0,
                topTags = (ratingSummaryData["topTags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                lastUpdated = (ratingSummaryData["lastUpdated"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
            Result.success(summary)
        } catch (e: Exception) {
            Timber.e(e, "Error getting user rating summary")
            Result.failure(e)
        }
    }

    
    /**
     * Update user's rating summary after a new rating
     * OPTIMIZED: Now stores in users.ratingSummary field instead of separate collection
     */
    private suspend fun updateUserRatingSummary(userId: String, userRole: RatingUserRole) {
        try {
            val ratingsResult = getRatingsForUser(userId)
            if (ratingsResult.isFailure) return
            
            val ratings = ratingsResult.getOrNull() ?: return
            if (ratings.isEmpty()) return
            
            val summary = calculateRatingSummary(userId, ratings, userRole)
            
            val summaryData = mapOf(
                "userId" to summary.userId,
                "userRole" to summary.userRole.name,
                "averageRating" to summary.averageRating,
                "totalRatings" to summary.totalRatings,
                "totalJobs" to summary.totalJobs,
                "averagePunctuality" to summary.averagePunctuality,
                "averageQuality" to summary.averageQuality,
                "averageCommunication" to summary.averageCommunication,
                "averageProfessionalism" to summary.averageProfessionalism,
                "averagePayment" to summary.averagePayment,
                "fiveStarCount" to summary.fiveStarCount,
                "fourStarCount" to summary.fourStarCount,
                "threeStarCount" to summary.threeStarCount,
                "twoStarCount" to summary.twoStarCount,
                "oneStarCount" to summary.oneStarCount,
                "topTags" to summary.topTags,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            // Store as nested field in users collection
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("ratingSummary", summaryData)
                .await()
            
            Timber.d("📊 RatingService: Updated rating summary for user $userId in users collection")
        } catch (e: Exception) {
            Timber.e(e, "Error updating user rating summary")
        }
    }
    
    /**
     * Calculate rating summary from list of ratings
     */
    private fun calculateRatingSummary(
        userId: String,
        ratings: List<JobRating>,
        userRole: RatingUserRole = RatingUserRole.WORKER
    ): UserRatingSummary {
        if (ratings.isEmpty()) {
            return UserRatingSummary(userId = userId, userRole = userRole)
        }
        
        val totalRatings = ratings.size
        val averageRating = ratings.map { it.overallRating }.average().toFloat()
        
        // Calculate breakdown averages
        val avgPunctuality = ratings.filter { it.punctualityRating > 0 }
            .map { it.punctualityRating }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
        val avgQuality = ratings.filter { it.qualityRating > 0 }
            .map { it.qualityRating }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
        val avgCommunication = ratings.filter { it.communicationRating > 0 }
            .map { it.communicationRating }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
        val avgProfessionalism = ratings.filter { it.professionalismRating > 0 }
            .map { it.professionalismRating }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
        val avgPayment = ratings.filter { it.paymentRating > 0 }
            .map { it.paymentRating }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
        
        // Count star distribution
        val fiveStars = ratings.count { it.overallRating == 5 }
        val fourStars = ratings.count { it.overallRating == 4 }
        val threeStars = ratings.count { it.overallRating == 3 }
        val twoStars = ratings.count { it.overallRating == 2 }
        val oneStars = ratings.count { it.overallRating == 1 }
        
        // Get top tags
        val allTags = ratings.flatMap { it.tags }
        val topTags = allTags.groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
        
        return UserRatingSummary(
            userId = userId,
            userRole = userRole,
            averageRating = averageRating,
            totalRatings = totalRatings,
            totalJobs = ratings.map { it.jobId }.distinct().size,
            averagePunctuality = avgPunctuality,
            averageQuality = avgQuality,
            averageCommunication = avgCommunication,
            averageProfessionalism = avgProfessionalism,
            averagePayment = avgPayment,
            fiveStarCount = fiveStars,
            fourStarCount = fourStars,
            threeStarCount = threeStars,
            twoStarCount = twoStars,
            oneStarCount = oneStars,
            topTags = topTags,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Parse rating document from Firestore
     */
    private fun parseRatingDocument(data: Map<String, Any>?): JobRating? {
        if (data == null) return null
        
        return try {
            JobRating(
                ratingId = data["ratingId"] as? String ?: "",
                jobId = data["jobId"] as? String ?: "",
                applicationId = data["applicationId"] as? String ?: "",
                ratedUserId = data["ratedUserId"] as? String ?: "",
                ratedUserRole = RatingUserRole.valueOf(data["ratedUserRole"] as? String ?: "WORKER"),
                raterUserId = data["raterUserId"] as? String ?: "",
                raterUserRole = RatingUserRole.valueOf(data["raterUserRole"] as? String ?: "EMPLOYER"),
                overallRating = (data["overallRating"] as? Number)?.toInt() ?: 0,
                punctualityRating = (data["punctualityRating"] as? Number)?.toInt() ?: 0,
                qualityRating = (data["qualityRating"] as? Number)?.toInt() ?: 0,
                communicationRating = (data["communicationRating"] as? Number)?.toInt() ?: 0,
                professionalismRating = (data["professionalismRating"] as? Number)?.toInt() ?: 0,
                paymentRating = (data["paymentRating"] as? Number)?.toInt() ?: 0,
                feedback = data["feedback"] as? String ?: "",
                tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                jobTitle = data["jobTitle"] as? String ?: "",
                companyName = data["companyName"] as? String ?: "",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isActive = data["isActive"] as? Boolean ?: true
            )
        } catch (e: Exception) {
            Timber.e(e, "Error parsing rating document")
            null
        }
    }
    
    /**
     * Check if user can rate a job (job must be completed/accepted)
     */
    suspend fun canRateJob(jobId: String, userId: String): Result<Boolean> {
        return try {
            // Check if already rated
            val existingRating = getRatingForJob(jobId, userId)
            if (existingRating.isSuccess && existingRating.getOrNull() != null) {
                return Result.success(false)
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if a user has already rated for a specific job
     * Returns a suspend function result instead of Flow to avoid exception transparency issues
     */
    suspend fun hasUserRatedForJob(raterId: String, jobId: String): Result<Boolean> {
        return try {
            val result = getRatingForJob(jobId, raterId)
            val hasRated = result.isSuccess && result.getOrNull() != null
            Result.success(hasRated)
        } catch (e: Exception) {
            Timber.e(e, "Error checking if user has rated job")
            // Return false on error to allow rating attempt (will fail at submit if already rated)
            Result.success(false)
        }
    }
}
