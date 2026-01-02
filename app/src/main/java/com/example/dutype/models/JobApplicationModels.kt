package com.example.dutype.models

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName
import java.time.LocalDateTime

/**
 * Professional Job Application Models
 * Designed for scalability and enterprise-level applications
 */

@Keep
data class JobApplication(
    val applicationId: String = "",
    val jobId: String = "",
    val workerId: String = "",
    val employerId: String = "",
    
    // Application Status
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val statusHistory: List<StatusUpdate> = emptyList(),
    
    // Worker Information (from profile)
    val workerName: String = "",
    val workerEmail: String = "",
    val workerPhone: String? = null,
    val workerProfileImageUrl: String? = null,
    val workerLocation: String? = null,
    val workerDateOfBirth: String? = null,
    val workerGender: String? = null,
    
    // Professional Information
    val workExperience: List<WorkExperience> = emptyList(),
    val workExperienceText: String? = null, // For simple text-based experience
    val skills: List<String> = emptyList(),
    val skillsText: String? = null, // For simple text-based skills
    val education: List<Education> = emptyList(),
    val certifications: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val availability: String? = null,
    val expectedSalary: String? = null,
    
    // Application Content
    val coverLetter: String = "",
    val resumeUrl: String? = null,
    val additionalDocuments: List<DocumentAttachment> = emptyList(),
    val customAnswers: Map<String, String> = emptyMap(), // For custom questions
    
    // Portfolio & Links (Removed - dutype doesn't need external profiles)
    
    // Job Information (snapshot at time of application)
    val jobTitle: String = "",
    val companyName: String = "",
    val jobLocation: String = "",
    val jobType: String = "",
    val payInfo: String = "",
    
    // Timestamps
    val appliedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastViewedByEmployer: Long? = null,
    
    // Communication
    val employerNotes: String? = null,
    val workerNotes: String? = null,
    val interviewScheduledAt: Long? = null,
    val interviewLocation: String? = null,
    val interviewNotes: String? = null,
    
    // Metadata
    val active: Boolean = true, // Using 'active' to match Firestore field name
    val applicationSource: ApplicationSource = ApplicationSource.MOBILE_APP,
    val referralSource: String? = null,
    
    // Job vacancy status - Use @PropertyName to avoid conflicting getters for boolean "is" properties
    @get:PropertyName("isFilled") @set:PropertyName("isFilled")
    var isFilled: Boolean = false,
    val filled: Boolean = false, // Firestore field compatibility
    
    // Work verification fields (to avoid "No setter/field" warnings)
    val verificationStatus: String? = null,
    val verificationCode: String? = null,
    val verificationId: String? = null
)

@Keep
data class StatusUpdate(
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "", // workerId or employerId
    val notes: String? = null,
    val systemUpdate: Boolean = false
)

@Keep
data class ApplicationStats(
    val totalApplications: Int = 0,
    val pendingApplications: Int = 0,
    val reviewedApplications: Int = 0,
    val shortlistedApplications: Int = 0,
    val interviewedApplications: Int = 0,
    val selectedApplications: Int = 0,
    val rejectedApplications: Int = 0,
    val hiredApplications: Int = 0,
    val thisMonthApplications: Int = 0,
    val responseRate: Float = 0f,
    val recentApplications: List<JobApplication> = emptyList()
)

@Keep
data class DocumentAttachment(
    val documentId: String = "",
    val fileName: String,
    val fileUrl: String,
    val fileType: DocumentType,
    val fileSize: Long = 0L,
    val uploadedAt: Long = System.currentTimeMillis(),
    val isRequired: Boolean = false
)

@Keep
data class WorkExperience(
    val id: String = "",
    val company: String,
    val position: String,
    val startDate: String,
    val endDate: String? = null,
    val description: String,
    val isCurrent: Boolean = false,
    val location: String? = null,
    val salary: String? = null,
    val achievements: List<String> = emptyList()
)

@Keep
data class Education(
    val id: String = "",
    val institution: String,
    val degree: String,
    val fieldOfStudy: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val gpa: String? = null,
    val description: String? = null,
    val isCurrent: Boolean = false
)

enum class ApplicationStatus {
    PENDING,           // Just applied
    UNDER_REVIEW,      // Under employer review (when employer opens/clicks application)
    REJECTED,          // Not selected
    ACCEPTED,          // Selected by employer (worker hired)
    COMPLETED,         // Job completed - employer marked as done
    WITHDRAWN          // Worker withdrew application
}

/**
 * Extension function to get display name for ApplicationStatus
 */
fun ApplicationStatus.getDisplayName(): String {
    return when (this) {
        ApplicationStatus.PENDING -> "Pending"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.REJECTED -> "Rejected"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.COMPLETED -> "Completed"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
    }
}

enum class DocumentType {
    RESUME,
    COVER_LETTER,
    PORTFOLIO,
    CERTIFICATE,
    ID_PROOF,
    EXPERIENCE_LETTER,
    OTHER
}

enum class ApplicationSource {
    MOBILE_APP,
    WEB_PORTAL,
    REFERRAL,
    JOB_BOARD
}

/**
 * Job Vacancy Status
 */
enum class JobVacancyStatus {
    OPEN,           // Accepting applications
    FILLED,         // All positions filled
    CLOSED,         // No longer accepting applications
    EXPIRED         // Job expired
}

/**
 * Job Vacancy Status Helper
 */
fun JobVacancyStatus.getDisplayName(): String {
    return when (this) {
        JobVacancyStatus.OPEN -> "Open"
        JobVacancyStatus.FILLED -> "Filled"
        JobVacancyStatus.CLOSED -> "Closed"
        JobVacancyStatus.EXPIRED -> "Expired"
    }
}

fun JobVacancyStatus.getStatusColor(): androidx.compose.ui.graphics.Color {
    return when (this) {
        JobVacancyStatus.OPEN -> androidx.compose.ui.graphics.Color(0xFF10B981) // Green
        JobVacancyStatus.FILLED -> androidx.compose.ui.graphics.Color(0xFF6B7280) // Gray
        JobVacancyStatus.CLOSED -> androidx.compose.ui.graphics.Color(0xFFEF4444) // Red
        JobVacancyStatus.EXPIRED -> androidx.compose.ui.graphics.Color(0xFF9CA3AF) // Light Gray
    }
}

/**
 * UI State for Job Applications
 */
data class JobApplicationUiState(
    val applications: List<JobApplication> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val submissionSuccess: Boolean = false
)

/**
 * Application Analytics for employer dashboard
 */
@Keep
data class ApplicationAnalytics(
    val totalApplications: Int = 0,
    val applicationsThisWeek: Int = 0,
    val applicationsThisMonth: Int = 0,
    val averageResponseTime: Long = 0L,
    val topJobTitles: List<String> = emptyList(),
    val applicationTrends: Map<String, Int> = emptyMap()
)

/**
 * Helper functions for status display
 */

fun ApplicationStatus.getStatusColor(): androidx.compose.ui.graphics.Color {
    return when (this) {
        ApplicationStatus.PENDING -> androidx.compose.ui.graphics.Color(0xFFF59E0B) // Amber
        ApplicationStatus.UNDER_REVIEW -> androidx.compose.ui.graphics.Color(0xFF3B82F6) // Blue
        ApplicationStatus.REJECTED -> androidx.compose.ui.graphics.Color(0xFFEF4444) // Red
        ApplicationStatus.ACCEPTED -> androidx.compose.ui.graphics.Color(0xFF10B981) // Green
        ApplicationStatus.COMPLETED -> androidx.compose.ui.graphics.Color(0xFF8B5CF6) // Purple
        ApplicationStatus.WITHDRAWN -> androidx.compose.ui.graphics.Color(0xFF6B7280) // Gray
    }
}

fun ApplicationStatus.getStatusIcon(): String {
    return when (this) {
        ApplicationStatus.PENDING -> "⏳"
        ApplicationStatus.UNDER_REVIEW -> "👀"
        ApplicationStatus.REJECTED -> "❌"
        ApplicationStatus.ACCEPTED -> "✅"
        ApplicationStatus.COMPLETED -> "🎉"
        ApplicationStatus.WITHDRAWN -> "↩️"
    }
}
