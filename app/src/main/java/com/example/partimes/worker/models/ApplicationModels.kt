package com.example.partimes.worker.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data models for job application functionality
 */

data class JobApplication(
    val id: String = "",
    val jobId: String,
    val userId: String,
    val personalInfo: PersonalInfo,
    val experience: List<WorkExperience> = emptyList(),
    val skills: List<String> = emptyList(),
    val resumeUrl: String? = null,
    val coverLetter: String = "",
    val documents: List<Document> = emptyList(),
    val status: ApplicationStatus = ApplicationStatus.DRAFT,
    val appliedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val employerFeedback: String? = null,
    val interviewScheduledAt: Long? = null,
    val notes: String? = null
)

data class PersonalInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val emergencyContact: String = "",
    val emergencyPhone: String = ""
)

data class WorkExperience(
    val id: String = "",
    val company: String = "",
    val position: String = "",
    val startDate: String = "",
    val endDate: String? = null,
    val description: String = "",
    val isCurrent: Boolean = false,
    val location: String = "",
    val salary: String? = null
)

data class Document(
    val id: String = "",
    val name: String = "",
    val type: DocumentType,
    val url: String = "",
    val uploadedAt: Long = System.currentTimeMillis(),
    val size: Long = 0L,
    val mimeType: String = "",
    val localPath: String? = null
)

enum class DocumentType {
    RESUME,
    CERTIFICATE,
    ID_PROOF,
    PORTFOLIO,
    OTHER
}

enum class ApplicationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    SHORTLISTED,
    INTERVIEW_SCHEDULED,
    INTERVIEWED,
    SELECTED,
    REJECTED,
    WITHDRAWN,
    EXPIRED
}

/**
 * UI State for Application Form
 */
data class ApplicationFormUiState(
    val personalInfo: PersonalInfo = PersonalInfo(),
    val experience: List<WorkExperience> = emptyList(),
    val skills: List<String> = emptyList(),
    val coverLetter: String = "",
    val documents: List<Document> = emptyList(),
    val isSubmitting: Boolean = false,
    val isUploading: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)

/**
 * Validation result for form fields
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String> = emptyMap()
)

/**
 * File upload progress
 */
data class UploadProgress(
    val fileName: String,
    val progress: Float,
    val isCompleted: Boolean = false,
    val error: String? = null
)

/**
 * Application submission result
 */
sealed class ApplicationResult {
    object Success : ApplicationResult()
    data class Error(val message: String) : ApplicationResult()
    object ValidationError : ApplicationResult()
}

/**
 * Extension functions for validation
 */
fun PersonalInfo.validate(): ValidationResult {
    val errors = mutableMapOf<String, String>()
    
    if (fullName.isBlank()) errors["fullName"] = "Full name is required"
    if (email.isBlank()) errors["email"] = "Email is required"
    else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        errors["email"] = "Please enter a valid email"
    }
    if (phone.isBlank()) errors["phone"] = "Phone number is required"
    else if (phone.length < 10) errors["phone"] = "Please enter a valid phone number"
    if (address.isBlank()) errors["address"] = "Address is required"
    if (dateOfBirth.isBlank()) errors["dateOfBirth"] = "Date of birth is required"
    if (gender.isBlank()) errors["gender"] = "Gender is required"
    
    return ValidationResult(errors.isEmpty(), errors)
}

fun WorkExperience.validate(): ValidationResult {
    val errors = mutableMapOf<String, String>()
    
    if (company.isBlank()) errors["company"] = "Company name is required"
    if (position.isBlank()) errors["position"] = "Position is required"
    if (startDate.isBlank()) errors["startDate"] = "Start date is required"
    if (!isCurrent && endDate.isNullOrBlank()) {
        errors["endDate"] = "End date is required for past positions"
    }
    if (description.isBlank()) errors["description"] = "Job description is required"
    
    return ValidationResult(errors.isEmpty(), errors)
}

fun ApplicationFormUiState.validate(): ValidationResult {
    val errors = mutableMapOf<String, String>()
    
    // Validate personal info
    val personalInfoValidation = personalInfo.validate()
    errors.putAll(personalInfoValidation.errors)
    
    // Validate experience
    experience.forEachIndexed { index, exp ->
        val expValidation = exp.validate()
        expValidation.errors.forEach { (field, error) ->
            errors["experience_${index}_$field"] = error
        }
    }
    
    // Validate skills
    if (skills.isEmpty()) {
        errors["skills"] = "At least one skill is required"
    }
    
    // Validate cover letter
    if (coverLetter.isBlank()) {
        errors["coverLetter"] = "Cover letter is required"
    } else if (coverLetter.length < 50) {
        errors["coverLetter"] = "Cover letter must be at least 50 characters"
    }
    
    // Validate documents
    if (documents.none { it.type == DocumentType.RESUME }) {
        errors["resume"] = "Resume is required"
    }
    
    return ValidationResult(errors.isEmpty(), errors)
}

/**
 * Helper functions for status display
 */
fun ApplicationStatus.getDisplayName(): String {
    return when (this) {
        ApplicationStatus.DRAFT -> "Draft"
        ApplicationStatus.SUBMITTED -> "Submitted"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.INTERVIEW_SCHEDULED -> "Interview Scheduled"
        ApplicationStatus.INTERVIEWED -> "Interviewed"
        ApplicationStatus.SELECTED -> "Selected"
        ApplicationStatus.REJECTED -> "Rejected"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
        ApplicationStatus.EXPIRED -> "Expired"
    }
}

fun ApplicationStatus.getColor(): androidx.compose.ui.graphics.Color {
    return when (this) {
        ApplicationStatus.DRAFT -> androidx.compose.ui.graphics.Color(0xFF9E9E9E)
        ApplicationStatus.SUBMITTED -> androidx.compose.ui.graphics.Color(0xFF2196F3)
        ApplicationStatus.UNDER_REVIEW -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        ApplicationStatus.SHORTLISTED -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
        ApplicationStatus.INTERVIEW_SCHEDULED -> androidx.compose.ui.graphics.Color(0xFF00BCD4)
        ApplicationStatus.INTERVIEWED -> androidx.compose.ui.graphics.Color(0xFF3F51B5)
        ApplicationStatus.SELECTED -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
        ApplicationStatus.REJECTED -> androidx.compose.ui.graphics.Color(0xFFF44336)
        ApplicationStatus.WITHDRAWN -> androidx.compose.ui.graphics.Color(0xFF607D8B)
        ApplicationStatus.EXPIRED -> androidx.compose.ui.graphics.Color(0xFF795548)
    }
}

fun DocumentType.getDisplayName(): String {
    return when (this) {
        DocumentType.RESUME -> "Resume"
        DocumentType.CERTIFICATE -> "Certificate"
        DocumentType.ID_PROOF -> "ID Proof"
        DocumentType.PORTFOLIO -> "Portfolio"
        DocumentType.OTHER -> "Other"
    }
}

fun DocumentType.getIcon(): ImageVector {
    return when (this) {
        DocumentType.RESUME -> Icons.Default.Description
        DocumentType.CERTIFICATE -> Icons.Default.School
        DocumentType.ID_PROOF -> Icons.Default.Badge
        DocumentType.PORTFOLIO -> Icons.Default.Folder
        DocumentType.OTHER -> Icons.Default.AttachFile
    }
}
