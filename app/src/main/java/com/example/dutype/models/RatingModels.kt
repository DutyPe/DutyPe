package com.example.dutype.models

/**
 * Rating Models for Two-way Rating System
 * Workers rate employers after job completion
 * Employers rate workers after job completion
 */

data class JobRating(
    val ratingId: String = "",
    val jobId: String = "",
    val applicationId: String = "",
    
    // Who is being rated
    val ratedUserId: String = "",
    val ratedUserRole: RatingUserRole = RatingUserRole.WORKER,
    
    // Who is giving the rating
    val raterUserId: String = "",
    val raterUserRole: RatingUserRole = RatingUserRole.EMPLOYER,
    
    // Rating details
    val overallRating: Int = 0, // 1-5 stars
    val punctualityRating: Int = 0, // 1-5 (for workers)
    val qualityRating: Int = 0, // 1-5 (for workers)
    val communicationRating: Int = 0, // 1-5 (for both)
    val professionalismRating: Int = 0, // 1-5 (for both)
    val paymentRating: Int = 0, // 1-5 (for employers - did they pay on time?)
    
    // Feedback
    val feedback: String = "",
    val tags: List<String> = emptyList(), // Quick tags like "On time", "Professional", etc.
    
    // Job context
    val jobTitle: String = "",
    val companyName: String = "",
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class RatingUserRole {
    WORKER,
    EMPLOYER
}

/**
 * User rating summary - aggregated from all ratings
 */
data class UserRatingSummary(
    val userId: String = "",
    val userRole: RatingUserRole = RatingUserRole.WORKER,
    
    // Aggregate scores
    val averageRating: Float = 0f,
    val totalRatings: Int = 0,
    val totalJobs: Int = 0,
    
    // Breakdown (for workers)
    val averagePunctuality: Float = 0f,
    val averageQuality: Float = 0f,
    val averageCommunication: Float = 0f,
    val averageProfessionalism: Float = 0f,
    
    // Breakdown (for employers)
    val averagePayment: Float = 0f,
    
    // Rating distribution
    val fiveStarCount: Int = 0,
    val fourStarCount: Int = 0,
    val threeStarCount: Int = 0,
    val twoStarCount: Int = 0,
    val oneStarCount: Int = 0,
    
    // Recent feedback tags
    val topTags: List<String> = emptyList(),
    
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Quick rating tags for workers
 */
object WorkerRatingTags {
    val POSITIVE = listOf(
        "On Time",
        "Hard Working",
        "Professional",
        "Good Communication",
        "Skilled",
        "Reliable",
        "Friendly",
        "Quick Learner"
    )
    
    val NEGATIVE = listOf(
        "Late",
        "Unprofessional",
        "Poor Communication",
        "Needs Improvement"
    )
}

/**
 * Quick rating tags for employers
 */
object EmployerRatingTags {
    val POSITIVE = listOf(
        "Paid on Time",
        "Clear Instructions",
        "Professional",
        "Good Communication",
        "Safe Workplace",
        "Respectful",
        "Fair Pay"
    )
    
    val NEGATIVE = listOf(
        "Late Payment",
        "Unclear Instructions",
        "Poor Communication",
        "Unsafe Conditions"
    )
}

/**
 * UI State for rating
 */
data class RatingUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val submitSuccess: Boolean = false,
    val existingRating: JobRating? = null,
    val userSummary: UserRatingSummary? = null
)
