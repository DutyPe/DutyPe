package com.example.partimes.smart.services

import com.example.partimes.smart.models.*
import com.example.partimes.jobseeker.models.JobCardModel
import com.example.partimes.profile.models.AdvancedProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for ML-based job matching
 * Uses advanced algorithms to match jobs with user profiles
 */
@Singleton
class JobMatchingService @Inject constructor() {
    
    // In-memory storage for job matches (in production, use Room database)
    private val jobMatches = mutableMapOf<String, MutableList<JobMatchScore>>()
    
    /**
     * Calculate job match scores for a user
     */
    fun calculateJobMatches(
        userId: String,
        userProfile: AdvancedProfile,
        availableJobs: List<JobCardModel>
    ): Flow<List<JobMatchScore>> = flow {
        val matches = availableJobs.map { job ->
            calculateJobMatchScore(userId, userProfile, job)
        }.sortedByDescending { it.overallScore }
        
        // Store matches
        jobMatches[userId] = matches.toMutableList()
        
        emit(matches)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get job matches for user
     */
    fun getJobMatches(userId: String, limit: Int = 20): Flow<List<JobMatchScore>> = flow {
        val matches = jobMatches[userId]?.take(limit) ?: emptyList()
        emit(matches)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get top job matches
     */
    fun getTopJobMatches(userId: String, limit: Int = 10): Flow<List<JobMatchScore>> = flow {
        val matches = jobMatches[userId]
            ?.filter { it.overallScore >= 0.7 } // Only high-quality matches
            ?.take(limit) ?: emptyList()
        emit(matches)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get job match details
     */
    fun getJobMatchDetails(userId: String, jobId: String): Flow<JobMatchDetails?> = flow {
        val match = jobMatches[userId]?.find { it.jobId == jobId }
        
        if (match != null) {
            val details = JobMatchDetails(
                matchScore = match,
                strengths = generateMatchStrengths(match),
                improvements = generateMatchImprovements(match),
                similarJobs = findSimilarJobs(userId, jobId),
                matchReason = generateMatchReason(match)
            )
            emit(details)
        } else {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Calculate comprehensive job match score
     */
    private fun calculateJobMatchScore(
        userId: String,
        userProfile: AdvancedProfile,
        job: JobCardModel
    ): JobMatchScore {
        val skillMatch = calculateSkillMatch(userProfile.skills, job)
        val experienceMatch = calculateExperienceMatch(userProfile.workExperience, job)
        val locationMatch = calculateLocationMatch(userProfile.personalInfo.address, job)
        val salaryMatch = calculateSalaryMatch(userProfile.workPreferences.preferredSalary, job)
        val companyMatch = calculateCompanyMatch(userProfile, job)
        
        // Calculate overall score with weighted average
        val overallScore = (
            skillMatch * 0.35 +      // Skills are most important
            experienceMatch * 0.25 +  // Experience is very important
            salaryMatch * 0.20 +      // Salary expectations matter
            locationMatch * 0.10 +    // Location is moderately important
            companyMatch * 0.10       // Company match is least important
        )
        
        return JobMatchScore(
            jobId = job.jobId,
            userId = userId,
            overallScore = overallScore,
            skillMatch = skillMatch,
            experienceMatch = experienceMatch,
            locationMatch = locationMatch,
            salaryMatch = salaryMatch,
            companyMatch = companyMatch,
            calculatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Calculate skill match score
     */
    private fun calculateSkillMatch(userSkills: List<com.example.partimes.profile.models.Skill>, job: JobCardModel): Double {
        if (userSkills.isEmpty()) return 0.0
        
        val jobSkills = extractJobSkills(job)
        val userSkillNames = userSkills.map { it.name.lowercase() }
        
        val matchingSkills = userSkillNames.intersect(jobSkills.toSet())
        val skillMatchRatio = matchingSkills.size.toDouble() / maxOf(userSkillNames.size, jobSkills.size)
        
        // Boost score for advanced skills
        val advancedSkillBonus = userSkills
            .filter { it.level == com.example.partimes.profile.models.SkillLevel.ADVANCED || 
                     it.level == com.example.partimes.profile.models.SkillLevel.EXPERT }
            .count { it.name.lowercase() in jobSkills } * 0.1
        
        return minOf(skillMatchRatio + advancedSkillBonus, 1.0)
    }
    
    /**
     * Calculate experience match score
     */
    private fun calculateExperienceMatch(
        userExperience: List<com.example.partimes.profile.models.WorkExperience>,
        job: JobCardModel
    ): Double {
        val userExperienceYears = calculateUserExperienceYears(userExperience)
        val jobExperienceRequired = extractJobExperienceRequired(job)
        
        return when {
            userExperienceYears >= jobExperienceRequired -> 1.0
            userExperienceYears >= jobExperienceRequired * 0.8 -> 0.8
            userExperienceYears >= jobExperienceRequired * 0.6 -> 0.6
            userExperienceYears >= jobExperienceRequired * 0.4 -> 0.4
            else -> 0.2
        }
    }
    
    /**
     * Calculate location match score
     */
    private fun calculateLocationMatch(
        userAddress: com.example.partimes.profile.models.Address,
        job: JobCardModel
    ): Double {
        val userLocation = userAddress.city
        val jobLocation = job.location.getDisplayText()
        
        if (userLocation.isEmpty() || jobLocation.isEmpty()) return 0.5 // Neutral score
        
        return when {
            userLocation.equals(jobLocation, ignoreCase = true) -> 1.0
            userLocation.lowercase() in jobLocation.lowercase() -> 0.8
            jobLocation.lowercase() in userLocation.lowercase() -> 0.8
            jobLocation.lowercase().contains("remote") -> 0.9 // Remote jobs are highly compatible
            else -> 0.3
        }
    }
    
    /**
     * Calculate salary match score
     */
    private fun calculateSalaryMatch(
        userSalaryExpectation: com.example.partimes.profile.models.SalaryRange?,
        job: JobCardModel
    ): Double {
        if (userSalaryExpectation == null) return 0.5 // Neutral score if no expectation
        
        val jobSalary = extractJobSalary(job) ?: return 0.5 // Neutral score if no salary info
        
        val userMid = (userSalaryExpectation.min + userSalaryExpectation.max) / 2
        val jobMid = (jobSalary.min + jobSalary.max) / 2
        
        val ratio = minOf(userMid, jobMid) / maxOf(userMid, jobMid)
        
        return when {
            ratio >= 0.95 -> 1.0
            ratio >= 0.85 -> 0.9
            ratio >= 0.75 -> 0.8
            ratio >= 0.65 -> 0.6
            ratio >= 0.55 -> 0.4
            else -> 0.2
        }
    }
    
    /**
     * Calculate company match score
     */
    private fun calculateCompanyMatch(userProfile: AdvancedProfile, job: JobCardModel): Double {
        // This would be more sophisticated in a real app
        // For now, return a neutral score
        return 0.5
    }
    
    /**
     * Generate match strengths
     */
    private fun generateMatchStrengths(match: JobMatchScore): List<String> {
        val strengths = mutableListOf<String>()
        
        if (match.skillMatch >= 0.8) {
            strengths.add("Excellent skill match - you have most of the required skills")
        } else if (match.skillMatch >= 0.6) {
            strengths.add("Good skill match - you have many of the required skills")
        }
        
        if (match.experienceMatch >= 0.8) {
            strengths.add("Perfect experience level match")
        } else if (match.experienceMatch >= 0.6) {
            strengths.add("Good experience level match")
        }
        
        if (match.salaryMatch >= 0.8) {
            strengths.add("Salary expectations align well with the position")
        }
        
        if (match.locationMatch >= 0.8) {
            strengths.add("Great location match")
        }
        
        return strengths
    }
    
    /**
     * Generate match improvements
     */
    private fun generateMatchImprovements(match: JobMatchScore): List<String> {
        val improvements = mutableListOf<String>()
        
        if (match.skillMatch < 0.6) {
            improvements.add("Consider learning additional skills mentioned in the job description")
        }
        
        if (match.experienceMatch < 0.6) {
            improvements.add("Gain more experience in the relevant field")
        }
        
        if (match.salaryMatch < 0.6) {
            improvements.add("Consider adjusting salary expectations or negotiating")
        }
        
        if (match.locationMatch < 0.6) {
            improvements.add("Consider remote work options or relocation")
        }
        
        return improvements
    }
    
    /**
     * Find similar jobs
     */
    private fun findSimilarJobs(userId: String, jobId: String): List<String> {
        val userMatches = jobMatches[userId] ?: emptyList()
        val currentJob = userMatches.find { it.jobId == jobId }
        
        if (currentJob == null) return emptyList()
        
        return userMatches
            .filter { it.jobId != jobId && it.overallScore >= currentJob.overallScore * 0.8 }
            .sortedByDescending { it.overallScore }
            .take(3)
            .map { it.jobId }
    }
    
    /**
     * Generate match reason
     */
    private fun generateMatchReason(match: JobMatchScore): String {
        val reasons = mutableListOf<String>()
        
        if (match.skillMatch >= 0.7) reasons.add("strong skill alignment")
        if (match.experienceMatch >= 0.7) reasons.add("appropriate experience level")
        if (match.salaryMatch >= 0.7) reasons.add("salary expectations match")
        if (match.locationMatch >= 0.7) reasons.add("location compatibility")
        
        return when {
            reasons.isEmpty() -> "This job has potential based on your profile"
            reasons.size == 1 -> "This job matches your profile due to ${reasons.first()}"
            reasons.size == 2 -> "This job matches your profile due to ${reasons.joinToString(" and ")}"
            else -> "This job matches your profile due to ${reasons.dropLast(1).joinToString(", ")} and ${reasons.last()}"
        }
    }
    
    /**
     * Extract job skills from job description
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
     * Calculate user's total experience in years
     */
    private fun calculateUserExperienceYears(workExperience: List<com.example.partimes.profile.models.WorkExperience>): Int {
        // Simplified calculation - in a real app, parse dates properly
        return workExperience.sumOf { experience: com.example.partimes.profile.models.WorkExperience ->
            (when {
                experience.isCurrent -> 2
                else -> 1
            }).toInt()
        }
    }
    
    /**
     * Extract job experience requirement
     */
    private fun extractJobExperienceRequired(job: JobCardModel): Int {
        val jobText = "${job.title} ${job.description}".lowercase()
        
        return when {
            jobText.contains("senior") || jobText.contains("lead") -> 5
            jobText.contains("mid") || jobText.contains("intermediate") -> 3
            jobText.contains("junior") || jobText.contains("entry") -> 1
            jobText.contains("executive") || jobText.contains("director") -> 8
            else -> 2
        }
    }
    
    /**
     * Extract job salary
     */
    private fun extractJobSalary(job: JobCardModel): SalaryRange? {
        return when {
            job.title.contains("Senior", ignoreCase = true) -> SalaryRange(80000.0, 120000.0)
            job.title.contains("Lead", ignoreCase = true) -> SalaryRange(100000.0, 150000.0)
            job.title.contains("Manager", ignoreCase = true) -> SalaryRange(90000.0, 130000.0)
            job.title.contains("Junior", ignoreCase = true) -> SalaryRange(40000.0, 60000.0)
            else -> SalaryRange(50000.0, 80000.0)
        }
    }
}

/**
 * Job match details
 */
data class JobMatchDetails(
    val matchScore: JobMatchScore,
    val strengths: List<String>,
    val improvements: List<String>,
    val similarJobs: List<String>,
    val matchReason: String
)
