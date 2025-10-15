package com.example.dutype.models

import java.time.LocalDateTime

/**
 * Professional Job Application Models
 * Designed for scalability and enterprise-level applications
 */

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
    val skills: List<String> = emptyList(),
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
    val referralSource: String? = null
)

data class StatusUpdate(
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "", // workerId or employerId
    val notes: String? = null,
    val systemUpdate: Boolean = false
)

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

data class ApplicationAnalytics(
    val totalApplications: Int = 0,
    val applicationsThisWeek: Int = 0,
    val applicationsThisMonth: Int = 0,
    val averageResponseTime: Long = 0L, // in milliseconds
    val topJobTitles: List<String> = emptyList(),
    val applicationTrends: Map<String, Int> = emptyMap()
)

data class DocumentAttachment(
    val documentId: String = "",
    val fileName: String,
    val fileUrl: String,
    val fileType: DocumentType,
    val fileSize: Long = 0L,
    val uploadedAt: Long = System.currentTimeMillis(),
    val isRequired: Boolean = false
)

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
    UNDER_REVIEW,      // Under employer review
    REVIEWED,          // Employer viewed application
    SHORTLISTED,       // Selected for next round
    INTERVIEW_SCHEDULED, // Interview scheduled
    INTERVIEWED,       // Interview completed
    SELECTED,          // Job offered
    REJECTED,          // Not selected
    WITHDRAWN,         // Worker withdrew
    EXPIRED,           // Job expired
    HIRED              // Successfully hired
}

/**
 * Extension function to get display name for ApplicationStatus
 */
fun ApplicationStatus.getDisplayName(): String {
    return when (this) {
        ApplicationStatus.PENDING -> "Pending"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.REVIEWED -> "Reviewed"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.INTERVIEW_SCHEDULED -> "Interview Scheduled"
        ApplicationStatus.INTERVIEWED -> "Interviewed"
        ApplicationStatus.SELECTED -> "Selected"
        ApplicationStatus.REJECTED -> "Rejected"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
        ApplicationStatus.EXPIRED -> "Expired"
        ApplicationStatus.HIRED -> "Hired"
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
 * Helper functions for status display
 */

fun ApplicationStatus.getStatusColor(): androidx.compose.ui.graphics.Color {
    return when (this) {
        ApplicationStatus.PENDING -> androidx.compose.ui.graphics.Color(0xFFF59E0B) // Amber
        ApplicationStatus.UNDER_REVIEW -> androidx.compose.ui.graphics.Color(0xFF3B82F6) // Blue
        ApplicationStatus.REVIEWED -> androidx.compose.ui.graphics.Color(0xFF3B82F6) // Blue
        ApplicationStatus.SHORTLISTED -> androidx.compose.ui.graphics.Color(0xFF10B981) // Green
        ApplicationStatus.INTERVIEW_SCHEDULED -> androidx.compose.ui.graphics.Color(0xFF8B5CF6) // Purple
        ApplicationStatus.INTERVIEWED -> androidx.compose.ui.graphics.Color(0xFF06B6D4) // Cyan
        ApplicationStatus.SELECTED -> androidx.compose.ui.graphics.Color(0xFF059669) // Emerald
        ApplicationStatus.REJECTED -> androidx.compose.ui.graphics.Color(0xFFEF4444) // Red
        ApplicationStatus.WITHDRAWN -> androidx.compose.ui.graphics.Color(0xFF6B7280) // Gray
        ApplicationStatus.EXPIRED -> androidx.compose.ui.graphics.Color(0xFF9CA3AF) // Light Gray
        ApplicationStatus.HIRED -> androidx.compose.ui.graphics.Color(0xFF10B981) // Green
    }
}

fun ApplicationStatus.getStatusIcon(): String {
    return when (this) {
        ApplicationStatus.PENDING -> "⏳"
        ApplicationStatus.UNDER_REVIEW -> "👀"
        ApplicationStatus.REVIEWED -> "👀"
        ApplicationStatus.SHORTLISTED -> "⭐"
        ApplicationStatus.INTERVIEW_SCHEDULED -> "📅"
        ApplicationStatus.INTERVIEWED -> "💼"
        ApplicationStatus.SELECTED -> "🎉"
        ApplicationStatus.REJECTED -> "❌"
        ApplicationStatus.WITHDRAWN -> "↩️"
        ApplicationStatus.EXPIRED -> "⏰"
        ApplicationStatus.HIRED -> "🏆"
    }
}
