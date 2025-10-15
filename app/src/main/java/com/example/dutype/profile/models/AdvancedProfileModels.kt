package com.example.dutype.profile.models

import java.util.Date

/**
 * Advanced profile models for comprehensive user profile management
 */

/**
 * Skill categories for better organization
 */
enum class SkillCategory {
    TECHNICAL,
    SOFT_SKILLS,
    LANGUAGES,
    CERTIFICATIONS,
    TOOLS,
    FRAMEWORKS,
    DATABASES,
    OTHER
}

/**
 * Skill proficiency levels
 */
enum class SkillLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

/**
 * Detailed skill model with category and proficiency
 */
data class Skill(
    val id: String = "",
    val name: String,
    val category: SkillCategory,
    val level: SkillLevel,
    val yearsOfExperience: Int = 0,
    val isVerified: Boolean = false,
    val verifiedBy: String? = null,
    val verifiedAt: Long? = null,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Education level enum
 */
enum class EducationLevel {
    HIGH_SCHOOL,
    DIPLOMA,
    BACHELORS,
    MASTERS,
    PHD,
    CERTIFICATION,
    OTHER
}

/**
 * Education model
 */
data class Education(
    val id: String = "",
    val institution: String,
    val degree: String,
    val fieldOfStudy: String,
    val level: EducationLevel,
    val startDate: String,
    val endDate: String? = null,
    val isCurrent: Boolean = false,
    val gpa: String? = null,
    val description: String = "",
    val location: String = "",
    val isVerified: Boolean = false,
    val verifiedAt: Long? = null
)

/**
 * Work preference types
 */
enum class WorkPreference {
    REMOTE,
    ON_SITE,
    HYBRID,
    FLEXIBLE
}

/**
 * Employment type
 */
enum class EmploymentType {
    FULL_TIME,
    PART_TIME,
    CONTRACT,
    FREELANCE,
    INTERNSHIP,
    TEMPORARY
}

/**
 * Work preferences model
 */
data class WorkPreferences(
    val preferredWorkType: WorkPreference = WorkPreference.ON_SITE,
    val preferredEmploymentType: EmploymentType = EmploymentType.FULL_TIME,
    val preferredSalary: SalaryRange? = null,
    val preferredLocations: List<String> = emptyList(),
    val availability: String = "",
    val noticePeriod: String = "",
    val willingToRelocate: Boolean = false,
    val travelWillingness: Boolean = false
)

/**
 * Salary range model
 */
data class SalaryRange(
    val min: Double,
    val max: Double,
    val currency: String = "USD",
    val period: String = "annually" // annually, monthly, hourly
)

/**
 * Verification status
 */
enum class VerificationStatus {
    NOT_VERIFIED,
    PENDING,
    VERIFIED,
    REJECTED,
    EXPIRED
}

/**
 * Verification type
 */
enum class VerificationType {
    EMAIL,
    PHONE,
    ID_PROOF,
    ADDRESS,
    EDUCATION,
    EMPLOYMENT,
    SKILLS
}

/**
 * Verification model
 */
data class Verification(
    val id: String = "",
    val type: VerificationType,
    val status: VerificationStatus = VerificationStatus.NOT_VERIFIED,
    val documentUrl: String? = null,
    val verifiedBy: String? = null,
    val verifiedAt: Long? = null,
    val expiresAt: Long? = null,
    val rejectionReason: String? = null,
    val submittedAt: Long = System.currentTimeMillis()
)

/**
 * Portfolio item model
 */
data class PortfolioItem(
    val id: String = "",
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val projectUrl: String? = null,
    val technologies: List<String> = emptyList(),
    val startDate: String,
    val endDate: String? = null,
    val isPublic: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Social media links
 */
data class SocialLinks(
    val linkedin: String? = null,
    val github: String? = null,
    val portfolio: String? = null,
    val twitter: String? = null,
    val website: String? = null
)

/**
 * Advanced user profile model
 */
data class AdvancedProfile(
    val userId: String,
    val personalInfo: PersonalInfo,
    val skills: List<Skill> = emptyList(),
    val education: List<Education> = emptyList(),
    val workExperience: List<WorkExperience> = emptyList(),
    val workPreferences: WorkPreferences = WorkPreferences(),
    val verifications: List<Verification> = emptyList(),
    val portfolio: List<PortfolioItem> = emptyList(),
    val socialLinks: SocialLinks = SocialLinks(),
    val resumeUrl: String? = null,
    val profilePictureUrl: String? = null,
    val coverLetter: String = "",
    val bio: String = "",
    val profileCompletionPercentage: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isPublic: Boolean = true,
    val isSearchable: Boolean = true
)

/**
 * Personal information model
 */
data class PersonalInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val nationality: String = "",
    val address: Address = Address(),
    val emergencyContact: EmergencyContact = EmergencyContact()
)

/**
 * Address model
 */
data class Address(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val postalCode: String = "",
    val isVerified: Boolean = false
)

/**
 * Emergency contact model
 */
data class EmergencyContact(
    val name: String = "",
    val relationship: String = "",
    val phone: String = "",
    val email: String = ""
)

/**
 * Work experience model (enhanced from ApplicationModels)
 */
data class WorkExperience(
    val id: String = "",
    val company: String = "",
    val position: String = "",
    val startDate: String = "",
    val endDate: String? = null,
    val isCurrent: Boolean = false,
    val location: String = "",
    val description: String = "",
    val achievements: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val salary: SalaryRange? = null,
    val isVerified: Boolean = false,
    val verifiedAt: Long? = null
)

/**
 * Profile completion tracking
 */
data class ProfileCompletion(
    val personalInfo: Boolean = false,
    val skills: Boolean = false,
    val education: Boolean = false,
    val workExperience: Boolean = false,
    val workPreferences: Boolean = false,
    val resume: Boolean = false,
    val profilePicture: Boolean = false,
    val verifications: Boolean = false
) {
    fun getCompletionPercentage(): Int {
        val totalFields = 8
        val completedFields = listOf(
            personalInfo, skills, education, workExperience,
            workPreferences, resume, profilePicture, verifications
        ).count { it }
        
        return (completedFields * 100) / totalFields
    }
}

/**
 * Extension functions for SkillCategory
 */
fun SkillCategory.getDisplayName(): String {
    return when (this) {
        SkillCategory.TECHNICAL -> "Technical Skills"
        SkillCategory.SOFT_SKILLS -> "Soft Skills"
        SkillCategory.LANGUAGES -> "Languages"
        SkillCategory.CERTIFICATIONS -> "Certifications"
        SkillCategory.TOOLS -> "Tools"
        SkillCategory.FRAMEWORKS -> "Frameworks"
        SkillCategory.DATABASES -> "Databases"
        SkillCategory.OTHER -> "Other"
    }
}

fun SkillCategory.getIcon(): String {
    return when (this) {
        SkillCategory.TECHNICAL -> "💻"
        SkillCategory.SOFT_SKILLS -> "🤝"
        SkillCategory.LANGUAGES -> "🌐"
        SkillCategory.CERTIFICATIONS -> "🏆"
        SkillCategory.TOOLS -> "🔧"
        SkillCategory.FRAMEWORKS -> "⚙️"
        SkillCategory.DATABASES -> "🗄️"
        SkillCategory.OTHER -> "📋"
    }
}

/**
 * Extension functions for SkillLevel
 */
fun SkillLevel.getDisplayName(): String {
    return when (this) {
        SkillLevel.BEGINNER -> "Beginner"
        SkillLevel.INTERMEDIATE -> "Intermediate"
        SkillLevel.ADVANCED -> "Advanced"
        SkillLevel.EXPERT -> "Expert"
    }
}

fun SkillLevel.getColor(): Long {
    return when (this) {
        SkillLevel.BEGINNER -> 0xFF4CAF50
        SkillLevel.INTERMEDIATE -> 0xFF2196F3
        SkillLevel.ADVANCED -> 0xFFFF9800
        SkillLevel.EXPERT -> 0xFF9C27B0
    }
}

/**
 * Extension functions for VerificationStatus
 */
fun VerificationStatus.getDisplayName(): String {
    return when (this) {
        VerificationStatus.NOT_VERIFIED -> "Not Verified"
        VerificationStatus.PENDING -> "Pending"
        VerificationStatus.VERIFIED -> "Verified"
        VerificationStatus.REJECTED -> "Rejected"
        VerificationStatus.EXPIRED -> "Expired"
    }
}

fun VerificationStatus.getColor(): Long {
    return when (this) {
        VerificationStatus.NOT_VERIFIED -> 0xFF9E9E9E
        VerificationStatus.PENDING -> 0xFFFF9800
        VerificationStatus.VERIFIED -> 0xFF4CAF50
        VerificationStatus.REJECTED -> 0xFFF44336
        VerificationStatus.EXPIRED -> 0xFF795548
    }
}

fun VerificationStatus.getIcon(): String {
    return when (this) {
        VerificationStatus.NOT_VERIFIED -> "❓"
        VerificationStatus.PENDING -> "⏳"
        VerificationStatus.VERIFIED -> "✅"
        VerificationStatus.REJECTED -> "❌"
        VerificationStatus.EXPIRED -> "⏰"
    }
}

/**
 * Document model
 */
data class Document(
    val id: String,
    val name: String,
    val type: DocumentType,
    val url: String,
    val uploadedAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false
)

/**
 * Document type enum
 */
enum class DocumentType {
    RESUME,
    COVER_LETTER,
    PORTFOLIO,
    CERTIFICATE,
    TRANSCRIPT,
    ID_PROOF,
    ADDRESS_PROOF,
    OTHER
}




/**
 * Resume upload state
 */
sealed class ResumeUploadState {
    object NoFile : ResumeUploadState()
    data class Uploading(val progress: Int) : ResumeUploadState()
    data class Uploaded(val fileName: String) : ResumeUploadState()
    data class Error(val error: String) : ResumeUploadState()
}

/**
 * Parsed resume data
 */
data class ParsedResumeData(
    val personalInfo: PersonalInfo? = null,
    val skills: List<Skill> = emptyList(),
    val workExperience: List<WorkExperience> = emptyList(),
    val education: List<Education> = emptyList(),
    val summary: String = "",
    val languages: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val projects: List<String> = emptyList(),
    val parsedAt: Long = System.currentTimeMillis()
)

