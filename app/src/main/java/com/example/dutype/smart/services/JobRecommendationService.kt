package com.example.dutype.smart.services

import com.example.dutype.smart.models.*
import com.example.dutype.worker.models.JobCardModel
import com.example.dutype.worker.models.PayType
import com.example.dutype.profile.models.AdvancedProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for generating intelligent job recommendations
 * Uses ML-like algorithms to match jobs with user profiles
 */
@Singleton
class JobRecommendationService @Inject constructor() {
    
    // In-memory storage for recommendations (in production, use Room database)
    private val userRecommendations = mutableMapOf<String, MutableList<JobRecommendation>>()
    private val jobAnalytics = mutableMapOf<String, JobAnalytics>()
    
    /**
     * Get job recommendations for a user
     */
    fun getJobRecommendations(
        userId: String,
        userProfile: AdvancedProfile,
        availableJobs: List<JobCardModel>,
        limit: Int = 20
    ): Flow<List<JobRecommendation>> = flow {
        val recommendations = generateRecommendations(userId, userProfile, availableJobs, limit)
        emit(recommendations)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get recommendations by type
     */
    fun getRecommendationsByType(
        userId: String,
        type: RecommendationType,
        limit: Int = 10
    ): Flow<List<JobRecommendation>> = flow {
        val userRecs = userRecommendations[userId] ?: emptyList()
        val filtered = userRecs.filter { it.type == type }.take(limit)
        emit(filtered)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Generate personalized recommendations
     */
    private fun generateRecommendations(
        userId: String,
        userProfile: AdvancedProfile,
        availableJobs: List<JobCardModel>,
        limit: Int
    ): List<JobRecommendation> {
        val recommendations = mutableListOf<JobRecommendation>()
        
        // Generate different types of recommendations
        recommendations.addAll(generateSkillBasedRecommendations(userId, userProfile, availableJobs))
        recommendations.addAll(generateLocationBasedRecommendations(userId, userProfile, availableJobs))
        recommendations.addAll(generateExperienceBasedRecommendations(userId, userProfile, availableJobs))
        recommendations.addAll(generateSalaryBasedRecommendations(userId, userProfile, availableJobs))
        recommendations.addAll(generateTrendingRecommendations(userId, availableJobs))
        
        // Sort by score and remove duplicates
        val uniqueRecommendations = recommendations
            .distinctBy { it.jobId }
            .sortedByDescending { it.score }
            .take(limit)
        
        // Store recommendations
        userRecommendations[userId] = uniqueRecommendations.toMutableList()
        
        return uniqueRecommendations
    }
    
    /**
     * Generate skill-based recommendations
     */
    private fun generateSkillBasedRecommendations(
        userId: String,
        userProfile: AdvancedProfile,
        availableJobs: List<JobCardModel>
    ): List<JobRecommendation> {
        val userSkills = userProfile.skills.map { it.name.lowercase() }
        val recommendations = mutableListOf<JobRecommendation>()
        
        availableJobs.forEach { job ->
            val jobSkills = extractJobSkills(job)
            val skillMatchScore = calculateSkillMatchScore(userSkills, jobSkills)
            
            if (skillMatchScore > 0.3) { // Minimum 30% skill match
                recommendations.add(
                    JobRecommendation(
                        id = UUID.randomUUID().toString(),
                        jobId = job.jobId,
                        userId = userId,
                        type = RecommendationType.SKILL_MATCH,
                        score = skillMatchScore,
                        reason = "Matches ${(skillMatchScore * 100).toInt()}% of your skills"
                    )
                )
            }
        }
        
        return recommendations.take(5) // Top 5 skill matches
    }
    
    /**
     * Generate location-based recommendations
     */
    private fun generateLocationBasedRecommendations(
        userId: String,
        userProfile: AdvancedProfile,
        availableJobs: List<JobCardModel>
    ): List<JobRecommendation> {
        val userLocation = userProfile.personalInfo.address.city
        val recommendations = mutableListOf<JobRecommendation>()
        
        availableJobs.forEach { job ->
            val jobLocation = job.location.getDisplayText()
            val locationScore = calculateLocationMatchScore(userLocation, jobLocation)
            
            if (locationScore > 0.5) {
                recommendations.add(
                    JobRecommendation(
                        id = UUID.randomUUID().toString(),
                        jobId = job.jobId,
                        userId = userId,
                        type = RecommendationType.LOCATION_BASED,
                        score = locationScore,
                        reason = "Located in ${jobLocation}"
                    )
                )
            }
        }
        
        return recommendations.take(3) // Top 3 location matches
    }
    
    /**
     * Generate experience-based recommendations
     */
    private fun generateExperienceBasedRecommendations(
        userId: String,
        userProfile: AdvancedProfile,
        availableJobs: List<JobCardModel>
    ): List<JobRecommendation> {
        val userExperience = calculateUserExperience(userProfile.workExperience)
        val recommendations = mutableListOf<JobRecommendation>()
        
        availableJobs.forEach { job ->
            val jobExperience = extractJobExperience(job)
            val experienceScore = calculateExperienceMatchScore(userExperience, jobExperience)
            
            if (experienceScore > 0.4) {
                recommendations.add(
                    JobRecommendation(
                        id = UUID.randomUUID().toString(),
                        jobId = job.jobId,
                        userId = userId,
                        type = RecommendationType.EXPERIENCE_MATCH,
                        score = experienceScore,
                        reason = "Matches your experience level"
                    )
                )
            }
        }
        
        return recommendations.take(3) // Top 3 experience matches
    }
    
    /**
     * Generate salary-based recommendations
     */
    private fun generateSalaryBasedRecommendations(
        userId: String,
        userProfile: AdvancedProfile,
        availableJobs: List<JobCardModel>
    ): List<JobRecommendation> {
        val userSalaryExpectation = userProfile.workPreferences.preferredSalary
        val recommendations = mutableListOf<JobRecommendation>()
        
        if (userSalaryExpectation == null) return recommendations
        
        // Convert profile SalaryRange to smart SalaryRange
        val smartSalaryRange = com.example.dutype.smart.models.SalaryRange(
            userSalaryExpectation.min,
            userSalaryExpectation.max
        )
        
        availableJobs.forEach { job ->
            val jobSalary = extractJobSalary(job)
            if (jobSalary != null) {
                val salaryScore = calculateSalaryMatchScore(smartSalaryRange, jobSalary)
                
                if (salaryScore > 0.6) {
                    recommendations.add(
                        JobRecommendation(
                            id = UUID.randomUUID().toString(),
                            jobId = job.jobId,
                            userId = userId,
                            type = RecommendationType.SALARY_MATCH,
                            score = salaryScore,
                            reason = "Salary matches your expectations"
                        )
                    )
                }
            }
        }
        
        return recommendations.take(3) // Top 3 salary matches
    }
    
    /**
     * Generate trending recommendations
     */
    private fun generateTrendingRecommendations(
        userId: String,
        availableJobs: List<JobCardModel>
    ): List<JobRecommendation> {
        val recommendations = mutableListOf<JobRecommendation>()
        
        // Sort jobs by trending score (simplified - in real app, use analytics)
        val trendingJobs = availableJobs.sortedByDescending { 
            // Simulate trending score based on job properties
            when {
                it.title.contains("Senior", ignoreCase = true) -> 0.9
                it.title.contains("Lead", ignoreCase = true) -> 0.8
                it.title.contains("Manager", ignoreCase = true) -> 0.7
                else -> 0.5
            }
        }
        
        trendingJobs.take(3).forEach { job ->
            recommendations.add(
                JobRecommendation(
                    id = UUID.randomUUID().toString(),
                    jobId = job.jobId,
                    userId = userId,
                    type = RecommendationType.TRENDING,
                    score = 0.8, // High score for trending
                    reason = "Trending in your area"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Calculate skill match score
     */
    private fun calculateSkillMatchScore(userSkills: List<String>, jobSkills: List<String>): Double {
        if (userSkills.isEmpty() || jobSkills.isEmpty()) return 0.0
        
        val matchingSkills = userSkills.intersect(jobSkills.toSet()).size
        return matchingSkills.toDouble() / maxOf(userSkills.size, jobSkills.size)
    }
    
    /**
     * Calculate location match score
     */
    private fun calculateLocationMatchScore(userLocation: String, jobLocation: String): Double {
        if (userLocation.isEmpty() || jobLocation.isEmpty()) return 0.0
        
        return when {
            userLocation.equals(jobLocation, ignoreCase = true) -> 1.0
            userLocation.lowercase() in jobLocation.lowercase() -> 0.8
            jobLocation.lowercase() in userLocation.lowercase() -> 0.8
            else -> 0.3 // Different location but still relevant
        }
    }
    
    /**
     * Calculate experience match score
     */
    private fun calculateExperienceMatchScore(userExperience: Int, jobExperience: Int): Double {
        val diff = kotlin.math.abs(userExperience - jobExperience)
        return when {
            diff <= 1 -> 1.0
            diff <= 2 -> 0.8
            diff <= 3 -> 0.6
            diff <= 5 -> 0.4
            else -> 0.2
        }
    }
    
    
    /**
     * Extract skills from job description
     */
    private fun extractJobSkills(job: JobCardModel): List<String> {
        val commonSkills = listOf(
            "java", "python", "javascript", "react", "angular", "vue", "node.js",
            "spring", "django", "flask", "mysql", "postgresql", "mongodb",
            "docker", "kubernetes", "aws", "azure", "git", "jenkins",
            "communication", "leadership", "teamwork", "problem solving"
        )
        
        val jobText = "${job.title} ${job.description}".lowercase()
        return commonSkills.filter { skill -> jobText.contains(skill) }
    }
    
    /**
     * Extract experience requirement from job
     */
    private fun extractJobExperience(job: JobCardModel): Int {
        val jobText = "${job.title} ${job.description}".lowercase()
        
        return when {
            jobText.contains("senior") || jobText.contains("lead") -> 5
            jobText.contains("mid") || jobText.contains("intermediate") -> 3
            jobText.contains("junior") || jobText.contains("entry") -> 1
            jobText.contains("intern") -> 0
            else -> 2 // Default
        }
    }
    
    /**
     * Extract salary from job
     */
    private fun extractJobSalary(job: JobCardModel): SalaryRange? {
        // Try to parse amount from string
        val amountStr = job.payInfo.amount.replace(Regex("[^0-9.]"), "")
        val payRate = amountStr.toDoubleOrNull()
        
        if (payRate != null && payRate > 0) {
             val multiplier = when (job.payInfo.type) {
                PayType.HOURLY -> 2000.0
                PayType.DAILY -> 250.0
                PayType.MONTHLY -> 12.0
                else -> 1.0
            }
            val annualSalary = payRate * multiplier
            return SalaryRange(annualSalary * 0.8, annualSalary * 1.2)
        }

        // Fallback to title-based estimation if no pay rate
        return when {
            job.title.contains("Senior", ignoreCase = true) -> SalaryRange(80000.0, 120000.0)
            job.title.contains("Lead", ignoreCase = true) -> SalaryRange(100000.0, 150000.0)
            job.title.contains("Manager", ignoreCase = true) -> SalaryRange(90000.0, 130000.0)
            job.title.contains("Junior", ignoreCase = true) -> SalaryRange(40000.0, 60000.0)
            else -> SalaryRange(50000.0, 80000.0)
        }
    }
    
    /**
     * Calculate salary match score
     */
    private fun calculateSalaryMatchScore(userSalary: SalaryRange, jobSalary: SalaryRange): Double {
        val userMin = userSalary.min
        val userMax = userSalary.max
        val jobMin = jobSalary.min
        val jobMax = jobSalary.max
        
        return when {
            jobMin >= userMin && jobMax <= userMax -> 1.0 // Perfect match
            jobMin >= userMin && jobMin <= userMax -> 0.8 // Good match
            jobMax >= userMin && jobMax <= userMax -> 0.6 // Partial match
            jobMin <= userMax && jobMax >= userMin -> 0.4 // Some overlap
            else -> 0.0 // No match
        }
    }
    
    /**
     * Calculate user's total experience
     */
    private fun calculateUserExperience(workExperience: List<com.example.dutype.profile.models.WorkExperience>): Int {
        return workExperience.sumOf { experience: com.example.dutype.profile.models.WorkExperience ->
            // Simplified calculation - in real app, parse dates properly
            (when {
                experience.isCurrent -> 2
                else -> 1
            }).toInt()
        }
    }
    
    /**
     * Mark recommendation as viewed
     */
    suspend fun markRecommendationAsViewed(userId: String, recommendationId: String): Result<Unit> {
        return try {
            val recommendations = userRecommendations[userId] ?: return Result.failure(Exception("No recommendations found"))
            val index = recommendations.indexOfFirst { it.id == recommendationId }
            
            if (index != -1) {
                recommendations[index] = recommendations[index].copy(isViewed = true)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Recommendation not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get recommendation statistics
     */
    fun getRecommendationStats(userId: String): RecommendationStats {
        val recommendations = userRecommendations[userId] ?: emptyList()
        
        val byType = recommendations.groupingBy { it.type }.eachCount()
        val viewedCount = recommendations.count { it.isViewed }
        val appliedCount = recommendations.count { it.isApplied }
        val savedCount = recommendations.count { it.isSaved }
        
        return RecommendationStats(
            totalRecommendations = recommendations.size,
            viewedCount = viewedCount,
            appliedCount = appliedCount,
            savedCount = savedCount,
            byType = byType,
            averageScore = if (recommendations.isNotEmpty()) recommendations.map { it.score }.average() else 0.0
        )
    }
}

/**
 * Recommendation statistics
 */
data class RecommendationStats(
    val totalRecommendations: Int = 0,
    val viewedCount: Int = 0,
    val appliedCount: Int = 0,
    val savedCount: Int = 0,
    val byType: Map<RecommendationType, Int> = emptyMap(),
    val averageScore: Double = 0.0
)
