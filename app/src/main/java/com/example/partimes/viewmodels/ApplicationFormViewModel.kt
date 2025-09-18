package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.jobseeker.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing job application form state and operations
 */
@HiltViewModel
class ApplicationFormViewModel @Inject constructor(
    private val applicationRepository: com.example.partimes.repository.ApplicationRepository,
    private val userRepository: com.example.partimes.repository.UserRepository,
    private val fileUploadService: com.example.partimes.services.FileUploadService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ApplicationFormUiState())
    val uiState: StateFlow<ApplicationFormUiState> = _uiState.asStateFlow()
    
    private val _uploadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val uploadProgress: StateFlow<Map<String, Float>> = _uploadProgress.asStateFlow()
    
    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()
    
    init {
        // Load user profile data
        loadUserProfile()
        
        // Validate form whenever state changes
        viewModelScope.launch {
            _uiState.collect { state ->
                val validation = state.validate()
                _isFormValid.value = validation.isValid
                _uiState.value = state.copy(validationErrors = validation.errors)
            }
        }
    }
    
    /**
     * Load user profile data and pre-fill form
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = userRepository.currentUser.value
                _uiState.value = _uiState.value.copy(
                    personalInfo = PersonalInfo(
                        fullName = user.name,
                        email = user.email,
                        phone = user.phoneNumber,
                        address = user.address ?: "",
                        dateOfBirth = user.dateOfBirth ?: "",
                        gender = user.gender ?: ""
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load profile: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Update personal information
     */
    fun updatePersonalInfo(personalInfo: PersonalInfo) {
        _uiState.value = _uiState.value.copy(
            personalInfo = personalInfo,
            error = null
        )
    }
    
    /**
     * Add work experience
     */
    fun addWorkExperience(experience: WorkExperience) {
        val currentExperiences = _uiState.value.experience.toMutableList()
        currentExperiences.add(experience)
        _uiState.value = _uiState.value.copy(
            experience = currentExperiences,
            error = null
        )
    }
    
    /**
     * Update work experience at specific index
     */
    fun updateWorkExperience(index: Int, experience: WorkExperience) {
        val currentExperiences = _uiState.value.experience.toMutableList()
        if (index in currentExperiences.indices) {
            currentExperiences[index] = experience
            _uiState.value = _uiState.value.copy(
                experience = currentExperiences,
                error = null
            )
        }
    }
    
    /**
     * Remove work experience at specific index
     */
    fun removeWorkExperience(index: Int) {
        val currentExperiences = _uiState.value.experience.toMutableList()
        if (index in currentExperiences.indices) {
            currentExperiences.removeAt(index)
            _uiState.value = _uiState.value.copy(
                experience = currentExperiences,
                error = null
            )
        }
    }
    
    /**
     * Add skill
     */
    fun addSkill(skill: String) {
        val trimmedSkill = skill.trim()
        if (trimmedSkill.isNotBlank()) {
            val currentSkills = _uiState.value.skills.toMutableList()
            if (!currentSkills.contains(trimmedSkill)) {
                currentSkills.add(trimmedSkill)
                _uiState.value = _uiState.value.copy(
                    skills = currentSkills,
                    error = null
                )
            }
        }
    }
    
    /**
     * Remove skill
     */
    fun removeSkill(skill: String) {
        val currentSkills = _uiState.value.skills.toMutableList()
        currentSkills.remove(skill)
        _uiState.value = _uiState.value.copy(
            skills = currentSkills,
            error = null
        )
    }
    
    /**
     * Update cover letter
     */
    fun updateCoverLetter(coverLetter: String) {
        _uiState.value = _uiState.value.copy(
            coverLetter = coverLetter,
            error = null
        )
    }
    
    /**
     * Upload document
     */
    fun uploadDocument(document: Document) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isUploading = true)
                
                // For now, simulate file upload since we don't have actual file bytes
                // In a real implementation, you would get file bytes from the file picker
                val simulatedFileBytes = ByteArray(1024) // 1KB simulation
                
                fileUploadService.uploadDocument(
                    document = document,
                    fileBytes = simulatedFileBytes,
                    onProgress = { progress ->
                        _uploadProgress.value = _uploadProgress.value + (document.id to progress)
                    }
                ).collect { result ->
                    result.fold(
                        onSuccess = { uploadedDoc ->
                            val currentDocs = _uiState.value.documents.toMutableList()
                            currentDocs.add(uploadedDoc)
                            _uiState.value = _uiState.value.copy(
                                documents = currentDocs,
                                isUploading = false
                            )
                            _uploadProgress.value = _uploadProgress.value - document.id
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isUploading = false,
                                error = error.message
                            )
                            _uploadProgress.value = _uploadProgress.value - document.id
                        }
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = "Upload failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Remove document
     */
    fun removeDocument(documentId: String) {
        val currentDocs = _uiState.value.documents.toMutableList()
        currentDocs.removeAll { it.id == documentId }
        _uiState.value = _uiState.value.copy(
            documents = currentDocs,
            error = null
        )
    }
    
    /**
     * Submit application
     */
    fun submitApplication(jobId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            // Validate form
            val validation = currentState.validate()
            if (!validation.isValid) {
                _uiState.value = currentState.copy(
                    validationErrors = validation.errors,
                    error = "Please fix the validation errors"
                )
                return@launch
            }
            
            _uiState.value = currentState.copy(isSubmitting = true, error = null)
            
            try {
                val application = JobApplication(
                    jobId = jobId,
                    userId = userRepository.currentUser.value.id,
                    personalInfo = currentState.personalInfo,
                    experience = currentState.experience,
                    skills = currentState.skills,
                    resumeUrl = currentState.documents.find { it.type == DocumentType.RESUME }?.url,
                    coverLetter = currentState.coverLetter,
                    documents = currentState.documents,
                    status = ApplicationStatus.SUBMITTED,
                    appliedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                applicationRepository.submitApplication(jobId, application)
                    .fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                isSubmitting = false,
                                isSubmitted = true
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isSubmitting = false,
                                error = error.message
                            )
                        }
                    )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Submission failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Reset form
     */
    fun resetForm() {
        _uiState.value = ApplicationFormUiState()
        _uploadProgress.value = emptyMap()
        loadUserProfile()
    }
    
    /**
     * Get validation error for specific field
     */
    fun getValidationError(field: String): String? {
        return _uiState.value.validationErrors[field]
    }
    
    /**
     * Check if field has validation error
     */
    fun hasValidationError(field: String): Boolean {
        return _uiState.value.validationErrors.containsKey(field)
    }
}
