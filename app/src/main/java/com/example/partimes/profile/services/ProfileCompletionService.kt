package com.example.partimes.profile.services

import com.example.partimes.profile.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for tracking and managing profile completion
 * Provides insights and recommendations for profile improvement
 */
@Singleton
class ProfileCompletionService @Inject constructor() {
    
    /**
     * Calculate profile completion percentage
     */
    fun calculateProfileCompletion(profile: AdvancedProfile): ProfileCompletion {
        val personalInfo = isPersonalInfoComplete(profile.personalInfo)
        val skills = profile.skills.isNotEmpty()
        val education = profile.education.isNotEmpty()
        val workExperience = profile.workExperience.isNotEmpty()
        val workPreferences = isWorkPreferencesComplete(profile.workPreferences)
        val resume = profile.resumeUrl != null
        val profilePicture = profile.profilePictureUrl != null
        val verifications = profile.verifications.any { it.status == VerificationStatus.VERIFIED }
        
        return ProfileCompletion(
            personalInfo = personalInfo,
            skills = skills,
            education = education,
            workExperience = workExperience,
            workPreferences = workPreferences,
            resume = resume,
            profilePicture = profilePicture,
            verifications = verifications
        )
    }
    
    /**
     * Get profile completion recommendations
     */
    fun getProfileRecommendations(profile: AdvancedProfile): Flow<List<ProfileRecommendation>> = flow {
        val recommendations = mutableListOf<ProfileRecommendation>()
        val completion = calculateProfileCompletion(profile)
        
        // Personal Info recommendations
        if (!completion.personalInfo) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.PERSONAL_INFO,
                    title = "Complete Personal Information",
                    description = "Add your full name, email, phone number, and address to improve your profile visibility.",
                    priority = RecommendationPriority.HIGH,
                    actionText = "Complete Profile",
                    estimatedTime = "2 minutes"
                )
            )
        }
        
        // Skills recommendations
        if (!completion.skills) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.SKILLS,
                    title = "Add Your Skills",
                    description = "Add at least 5 relevant skills to help employers find you and match you with suitable jobs.",
                    priority = RecommendationPriority.HIGH,
                    actionText = "Add Skills",
                    estimatedTime = "5 minutes"
                )
            )
        } else if (profile.skills.size < 5) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.SKILLS,
                    title = "Add More Skills",
                    description = "Add more skills to improve your job matching. Consider adding both technical and soft skills.",
                    priority = RecommendationPriority.MEDIUM,
                    actionText = "Add More Skills",
                    estimatedTime = "3 minutes"
                )
            )
        }
        
        // Education recommendations
        if (!completion.education) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.EDUCATION,
                    title = "Add Education Details",
                    description = "Include your educational background to showcase your qualifications to employers.",
                    priority = RecommendationPriority.MEDIUM,
                    actionText = "Add Education",
                    estimatedTime = "3 minutes"
                )
            )
        }
        
        // Work Experience recommendations
        if (!completion.workExperience) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.WORK_EXPERIENCE,
                    title = "Add Work Experience",
                    description = "Add your work experience to demonstrate your professional background and achievements.",
                    priority = RecommendationPriority.HIGH,
                    actionText = "Add Experience",
                    estimatedTime = "10 minutes"
                )
            )
        }
        
        // Resume recommendations
        if (!completion.resume) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.RESUME,
                    title = "Upload Your Resume",
                    description = "Upload your resume to provide employers with detailed information about your background.",
                    priority = RecommendationPriority.HIGH,
                    actionText = "Upload Resume",
                    estimatedTime = "2 minutes"
                )
            )
        }
        
        // Profile Picture recommendations
        if (!completion.profilePicture) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.PROFILE_PICTURE,
                    title = "Add Profile Picture",
                    description = "Add a professional profile picture to make your profile more personal and trustworthy.",
                    priority = RecommendationPriority.LOW,
                    actionText = "Add Photo",
                    estimatedTime = "1 minute"
                )
            )
        }
        
        // Verification recommendations
        if (!completion.verifications) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.VERIFICATION,
                    title = "Verify Your Identity",
                    description = "Verify your email and phone number to increase trust and improve your profile visibility.",
                    priority = RecommendationPriority.MEDIUM,
                    actionText = "Verify Now",
                    estimatedTime = "5 minutes"
                )
            )
        }
        
        // Work Preferences recommendations
        if (!completion.workPreferences) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.WORK_PREFERENCES,
                    title = "Set Work Preferences",
                    description = "Specify your work preferences to help us recommend the most suitable jobs for you.",
                    priority = RecommendationPriority.MEDIUM,
                    actionText = "Set Preferences",
                    estimatedTime = "3 minutes"
                )
            )
        }
        
        // Bio recommendations
        if (profile.bio.isEmpty()) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.BIO,
                    title = "Write a Professional Bio",
                    description = "Add a compelling bio that highlights your strengths and career goals.",
                    priority = RecommendationPriority.MEDIUM,
                    actionText = "Write Bio",
                    estimatedTime = "5 minutes"
                )
            )
        }
        
        // Portfolio recommendations
        if (profile.portfolio.isEmpty()) {
            recommendations.add(
                ProfileRecommendation(
                    type = RecommendationType.PORTFOLIO,
                    title = "Add Portfolio Projects",
                    description = "Showcase your work by adding portfolio projects to demonstrate your skills.",
                    priority = RecommendationPriority.LOW,
                    actionText = "Add Portfolio",
                    estimatedTime = "10 minutes"
                )
            )
        }
        
        emit(recommendations)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get profile strength score (0-100)
     */
    fun getProfileStrengthScore(profile: AdvancedProfile): Int {
        val completion = calculateProfileCompletion(profile)
        var score = 0.0
        
        // Base completion score (70 points)
        score += completion.getCompletionPercentage() * 0.7
        
        // Additional quality factors (30 points)
        if (profile.skills.size >= 10) score += 5.0
        if (profile.workExperience.size >= 2) score += 5.0
        if (profile.education.size >= 1) score += 5.0
        if (profile.verifications.count { it.status == VerificationStatus.VERIFIED } >= 2) score += 5.0
        if (profile.bio.isNotEmpty()) score += 5.0
        if (profile.portfolio.isNotEmpty()) score += 5.0
        
        return minOf(score.toInt(), 100)
    }
    
    /**
     * Get profile insights
     */
    fun getProfileInsights(profile: AdvancedProfile): ProfileInsights {
        val completion = calculateProfileCompletion(profile)
        val strengthScore = getProfileStrengthScore(profile)
        
        val insights = mutableListOf<String>()
        
        // Completion insights
        val completionPercentage = completion.getCompletionPercentage()
        when {
            completionPercentage >= 90 -> insights.add("Excellent! Your profile is almost complete.")
            completionPercentage >= 70 -> insights.add("Good progress! Just a few more details to complete your profile.")
            completionPercentage >= 50 -> insights.add("You're halfway there! Complete a few more sections to improve your profile.")
            else -> insights.add("Your profile needs more information to be effective.")
        }
        
        // Skills insights
        when {
            profile.skills.size >= 15 -> insights.add("Great! You have a comprehensive skill set.")
            profile.skills.size >= 10 -> insights.add("Good skill diversity. Consider adding more specialized skills.")
            profile.skills.size >= 5 -> insights.add("Nice start! Add more skills to improve job matching.")
            else -> insights.add("Add more skills to showcase your capabilities.")
        }
        
        // Experience insights
        if (profile.workExperience.isNotEmpty()) {
            val totalExperience = profile.workExperience.sumOf { 
                // Calculate years of experience (simplified)
                if (it.isCurrent) 1.0 else 0.5
            }
            when {
                totalExperience >= 5 -> insights.add("Impressive experience! Your background will attract many employers.")
                totalExperience >= 2 -> insights.add("Good experience level. Highlight your achievements.")
                else -> insights.add("Consider adding more details about your experience and achievements.")
            }
        }
        
        // Verification insights
        val verifiedCount = profile.verifications.count { it.status == VerificationStatus.VERIFIED }
        when {
            verifiedCount >= 3 -> insights.add("Well verified! Your profile has high credibility.")
            verifiedCount >= 2 -> insights.add("Good verification status. Consider verifying more details.")
            verifiedCount >= 1 -> insights.add("Start with verification. It builds trust with employers.")
            else -> insights.add("Verification increases your profile credibility significantly.")
        }
        
        return ProfileInsights(
            strengthScore = strengthScore,
            completionPercentage = completionPercentage,
            insights = insights,
            nextSteps = getNextSteps(completion),
            estimatedCompletionTime = calculateEstimatedCompletionTime(completion)
        )
    }
    
    /**
     * Check if personal info is complete
     */
    private fun isPersonalInfoComplete(personalInfo: PersonalInfo): Boolean {
        return personalInfo.fullName.isNotEmpty() &&
                personalInfo.email.isNotEmpty() &&
                personalInfo.phone.isNotEmpty() &&
                personalInfo.address.city.isNotEmpty() &&
                personalInfo.address.country.isNotEmpty()
    }
    
    /**
     * Check if work preferences are complete
     */
    private fun isWorkPreferencesComplete(workPreferences: WorkPreferences): Boolean {
        return workPreferences.preferredWorkType != null &&
                workPreferences.preferredEmploymentType != null &&
                workPreferences.preferredLocations.isNotEmpty()
    }
    
    /**
     * Get next steps for profile completion
     */
    private fun getNextSteps(completion: ProfileCompletion): List<String> {
        val steps = mutableListOf<String>()
        
        if (!completion.personalInfo) steps.add("Complete personal information")
        if (!completion.skills) steps.add("Add your skills")
        if (!completion.workExperience) steps.add("Add work experience")
        if (!completion.resume) steps.add("Upload your resume")
        if (!completion.verifications) steps.add("Verify your identity")
        if (!completion.education) steps.add("Add education details")
        if (!completion.workPreferences) steps.add("Set work preferences")
        if (!completion.profilePicture) steps.add("Add profile picture")
        
        return steps.take(3) // Show top 3 next steps
    }
    
    /**
     * Calculate estimated time to complete profile
     */
    private fun calculateEstimatedCompletionTime(completion: ProfileCompletion): String {
        var totalMinutes = 0
        
        if (!completion.personalInfo) totalMinutes += 2
        if (!completion.skills) totalMinutes += 5
        if (!completion.workExperience) totalMinutes += 10
        if (!completion.resume) totalMinutes += 2
        if (!completion.verifications) totalMinutes += 5
        if (!completion.education) totalMinutes += 3
        if (!completion.workPreferences) totalMinutes += 3
        if (!completion.profilePicture) totalMinutes += 1
        
        return when {
            totalMinutes < 5 -> "Less than 5 minutes"
            totalMinutes < 15 -> "About $totalMinutes minutes"
            totalMinutes < 30 -> "About ${totalMinutes / 5 * 5} minutes"
            else -> "About ${totalMinutes / 10 * 10} minutes"
        }
    }
}

/**
 * Profile recommendation model
 */
data class ProfileRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val actionText: String,
    val estimatedTime: String
)

/**
 * Recommendation types
 */
enum class RecommendationType {
    PERSONAL_INFO,
    SKILLS,
    EDUCATION,
    WORK_EXPERIENCE,
    WORK_PREFERENCES,
    RESUME,
    PROFILE_PICTURE,
    VERIFICATION,
    BIO,
    PORTFOLIO
}

/**
 * Recommendation priority levels
 */
enum class RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Profile insights model
 */
data class ProfileInsights(
    val strengthScore: Int,
    val completionPercentage: Int,
    val insights: List<String>,
    val nextSteps: List<String>,
    val estimatedCompletionTime: String
)
