package com.example.dutype.worker.models

import com.example.dutype.utils.ValidationUtils
// Use canonical models from main models package - SINGLE SOURCE OF TRUTH
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.WorkExperience
import com.example.dutype.models.DocumentType as MainDocumentType

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data models for job application functionality (Worker-specific)
 * 
 * NOTE: The following are imported from com.example.dutype.models to maintain
 * single source of truth across the codebase:
 * - ApplicationStatus
 * - WorkExperience (use canonical version from JobApplicationModels.kt)
 * - DocumentType (use canonical version from JobApplicationModels.kt)
 * 
 * This file contains ONLY worker-specific models that don't exist in main models package.
 */

/**
 * Personal information for job applications
 * Worker-specific - not duplicated elsewhere
 */
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

/**
 * Document model for worker uploads
 * Worker-specific with local path support
 */
data class Document(
    val id: String = "",
    val name: String = "",
    val type: MainDocumentType,
    val url: String = "",
    val uploadedAt: Long = System.currentTimeMillis(),
    val size: Long = 0L,
    val mimeType: String = "",
    val localPath: String? = null
)

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
    else if (!ValidationUtils.isValidFullName(fullName)) errors["fullName"] = "Please enter a valid full name"
    
    if (email.isBlank()) errors["email"] = "Email is required"
    else if (!ValidationUtils.isValidEmail(email)) {
        errors["email"] = "Please enter a valid email"
    }
    if (phone.isBlank()) errors["phone"] = "Phone number is required"
    else if (!ValidationUtils.isValidIndianPhoneNumber(phone)) errors["phone"] = "Please enter a valid phone number"
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
    if (documents.none { it.type == MainDocumentType.RESUME }) {
        errors["resume"] = "Resume is required"
    }
    
    return ValidationResult(errors.isEmpty(), errors)
}

// NOTE: Use extensions from com.example.dutype.models.JobApplicationModels:
// - ApplicationStatus.getDisplayName()
// - ApplicationStatus.getStatusColor()
// - DocumentType.getDisplayName() is available in main models

/**
 * Worker-specific document icon helper
 */
fun MainDocumentType.getIcon(): ImageVector {
    return when (this) {
        MainDocumentType.RESUME -> Icons.Default.Description
        MainDocumentType.CERTIFICATE -> Icons.Default.School
        MainDocumentType.ID_PROOF -> Icons.Default.Badge
        MainDocumentType.PHOTO -> Icons.Default.Photo
        MainDocumentType.OTHER -> Icons.Default.AttachFile
    }
}
