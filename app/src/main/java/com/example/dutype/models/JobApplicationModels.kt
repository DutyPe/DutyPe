package com.example.dutype.models

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color

/**
 * JobApplication - MINIMAL MODEL (12 fields)
 * Based on Urban Company/TaskRabbit/LinkedIn patterns
 * 
 * SCALABILITY: Worker profile fields are nullable and populated dynamically by ViewModel
 * to avoid storing redundant data in Firestore. These fields are enriched at runtime.
 */
@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class JobApplication(
    // IDs (4 fields)
    val id: String = "",
    val jobId: String = "",
    val workerId: String = "",
    val employerId: String = "",
    
    // Status (3 fields)
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val appliedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Active flag (for soft delete / filtering)
    val active: Boolean = true,  // CRITICAL: Must be a field, not computed property, for Firestore deserialization
    
    // Denormalized display data (4 fields)
    val jobTitle: String = "",
    val jobLocation: String = "",
    val companyName: String = "",
    val workerName: String = "",
    
    // Optional
    val coverLetter: String = "",
    
    // Notification tracking (NEW)
    val lastPendingNotificationSent: Long? = null,  // Track when pending notification was sent
    
    // RUNTIME ENRICHMENT: Worker profile data (not stored in Firestore, populated by ViewModel)
    // These fields are fetched dynamically from User profile to avoid data duplication
    val workerEmail: String = "",
    val workerPhone: String? = null,
    val workerLocation: String? = null,
    val workerGender: String? = null,
    val workerDateOfBirth: String? = null,
    val workerProfileImageUrl: String? = null,
    val workExperience: List<WorkExperience> = emptyList(),
    val workExperienceText: String? = null,
    val skills: List<String> = emptyList(),
    val skillsText: String? = null,
    val education: List<Education> = emptyList(),
    val certifications: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val availability: String? = null,
    val expectedSalary: String? = null,
    val resumeUrl: String? = null,
    
    // Verification status (enriched from User profile)
    val workerAadhaarVerified: Boolean? = null,
    val workerPhoneVerified: Boolean? = null,
    val workerJobsInArea: Int? = null,
    val workerLocalRating: Float? = null,
    val workerTotalReviews: Int? = null,
    val workerBackgroundCheckPassed: Boolean? = null,
    val workerIdentityVerified: Boolean? = null,
    
    // Additional fields for home entry jobs
    val homeEntryJob: Boolean = false,
    val additionalDocuments: List<DocumentAttachment> = emptyList(),
    val statusHistory: List<StatusHistoryEntry> = emptyList()
) {
    // Computed properties for backward compatibility
    val applicationId: String get() = id
}

/**
 * Document attachment for applications
 */
@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class DocumentAttachment(
    val id: String = "",
    val name: String = "",
    val fileName: String = name, // Alias for backward compatibility
    val url: String = "",
    val fileUrl: String = url, // Alias for backward compatibility
    val type: String = "", // "resume", "certificate", "id_proof", etc.
    val fileType: DocumentType = DocumentType.OTHER,
    val fileSize: Long = 0L, // File size in bytes
    val uploadedAt: Long = System.currentTimeMillis()
)

/**
 * Document type enum for better type safety
 */
enum class DocumentType {
    RESUME,
    CERTIFICATE,
    ID_PROOF,
    PHOTO,
    OTHER;
    
    val displayName: String
        get() = when (this) {
            RESUME -> "Resume"
            CERTIFICATE -> "Certificate"
            ID_PROOF -> "ID Proof"
            PHOTO -> "Photo"
            OTHER -> "Document"
        }
    
    fun getIcon(): String = when (this) {
        RESUME -> "📄"
        CERTIFICATE -> "🎓"
        ID_PROOF -> "🪪"
        PHOTO -> "📷"
        OTHER -> "📎"
    }
}

/**
 * Status history entry for tracking application status changes
 */
@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class StatusHistoryEntry(
    val status: ApplicationStatus = ApplicationStatus.PENDING,  // Added default value
    val timestamp: Long = System.currentTimeMillis(),  // Added default value
    val updatedAt: Long = timestamp, // Alias for backward compatibility
    val notes: String? = null,
    val updatedBy: String? = null,
    val systemUpdate: Boolean = false // True if updated by system, false if by user
)

// Type alias for backward compatibility
typealias StatusUpdate = StatusHistoryEntry

enum class ApplicationStatus {
    PENDING,
    UNDER_REVIEW,
    REJECTED,
    ACCEPTED,
    COMPLETED,
    WITHDRAWN
}

fun ApplicationStatus.getDisplayName(): String = when (this) {
    ApplicationStatus.PENDING -> "Pending"
    ApplicationStatus.UNDER_REVIEW -> "Under Review"
    ApplicationStatus.REJECTED -> "Rejected"
    ApplicationStatus.ACCEPTED -> "Accepted"
    ApplicationStatus.COMPLETED -> "Completed"
    ApplicationStatus.WITHDRAWN -> "Withdrawn"
}


// Supporting classes for application forms
@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class WorkExperience(
    val id: String = "",
    val company: String = "",
    val position: String = "",
    val location: String? = null,
    val startDate: String = "",
    val endDate: String? = null,
    val isCurrent: Boolean = false,
    val description: String = "",
    val achievements: List<String> = emptyList()
)

@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class Education(
    val id: String = "",
    val institution: String = "",
    val degree: String = "",
    val fieldOfStudy: String? = null,
    val startDate: String = "",
    val endDate: String? = null,
    val gpa: String? = null
)

enum class JobVacancyStatus {
    OPEN,
    FILLED,
    CLOSED,
    EXPIRED
}

fun JobVacancyStatus.getDisplayName(): String = when (this) {
    JobVacancyStatus.OPEN -> "Open"
    JobVacancyStatus.FILLED -> "Filled"
    JobVacancyStatus.CLOSED -> "Closed"
    JobVacancyStatus.EXPIRED -> "Expired"
}

/**
 * Extension function to get status color for ApplicationStatus
 * Used in UI components to display status with appropriate colors
 */
fun ApplicationStatus.getStatusColor(): Color {
    return when (this) {
        ApplicationStatus.PENDING -> Color(0xFFFFA500) // Orange
        ApplicationStatus.UNDER_REVIEW -> Color(0xFF2196F3) // Blue
        ApplicationStatus.ACCEPTED -> Color(0xFF4CAF50) // Green
        ApplicationStatus.REJECTED -> Color(0xFFF44336) // Red
        ApplicationStatus.COMPLETED -> Color(0xFF9C27B0) // Purple
        ApplicationStatus.WITHDRAWN -> Color(0xFF757575) // Gray
    }
}


/**
 * UI State for Job Application screens
 */
data class JobApplicationUiState(
    val applications: List<JobApplication> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val hasError: Boolean = false,
    val error: String = "",
    val submissionSuccess: Boolean = false
)

/**
 * Application statistics for worker
 */
@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class ApplicationStats(
    val totalApplications: Int = 0,
    val pendingApplications: Int = 0,
    val reviewedApplications: Int = 0,
    val shortlistedApplications: Int = 0,
    val rejectedApplications: Int = 0,
    val hiredApplications: Int = 0,
    val acceptedApplications: Int = 0,
    val completedApplications: Int = 0,
    val withdrawnApplications: Int = 0,
    val recentApplications: List<JobApplication> = emptyList()
)

/**
 * Application analytics for employer
 */
@Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class ApplicationAnalytics(
    val totalApplications: Int = 0,
    val applicationsThisWeek: Int = 0,
    val applicationsThisMonth: Int = 0,
    val newApplications: Int = 0,
    val underReviewApplications: Int = 0,
    val acceptedApplications: Int = 0,
    val rejectedApplications: Int = 0,
    val averageResponseTime: Long = 0L,
    val topJobTitles: List<String> = emptyList(),
    val applicationTrends: Map<String, Int> = emptyMap()
)

