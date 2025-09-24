package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.jobseeker.models.*
import com.example.partimes.models.ApplicationStatus
import com.example.partimes.models.User
import com.example.partimes.models.UserRole
import com.example.partimes.data.ApplicationFormDataStore
import com.example.partimes.auth.AuthManager
import com.example.partimes.network.ApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Simplified ViewModel for managing job application form state and operations
 * Uses ApplicationFormDataStore for persistent storage
 */
@HiltViewModel
class SimpleApplicationFormViewModel @Inject constructor(
    private val dataStore: ApplicationFormDataStore,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ApplicationFormUiState())
    val uiState: StateFlow<ApplicationFormUiState> = _uiState.asStateFlow()
    
    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()
    
    init {
        // Load saved data
        loadSavedData()
        
        // Validate form whenever state changes
        viewModelScope.launch {
            _uiState.collect { state ->
                val validation = validateForm(state)
                _isFormValid.value = validation
            }
        }
    }
    
    /**
     * Load saved data from data store
     */
    private fun loadSavedData() {
        viewModelScope.launch {
            val personalInfo = dataStore.getPersonalInfo()
            val experience = dataStore.getExperience()
            val skills = dataStore.getSkills()
            val coverLetter = dataStore.getCoverLetter()
            val documents = dataStore.getDocuments()
            
            println("DEBUG: Loading saved data:")
            println("DEBUG: PersonalInfo: $personalInfo")
            println("DEBUG: Experience: $experience")
            println("DEBUG: Skills: $skills")
            println("DEBUG: CoverLetter: $coverLetter")
            println("DEBUG: Documents: $documents")
            
            _uiState.value = ApplicationFormUiState(
                personalInfo = personalInfo,
                experience = experience,
                skills = skills,
                coverLetter = coverLetter,
                documents = documents
            )
        }
    }
    
    /**
     * Update personal information
     */
    fun updatePersonalInfo(personalInfo: PersonalInfo) {
        println("DEBUG: updatePersonalInfo called with: $personalInfo")
        _uiState.value = _uiState.value.copy(personalInfo = personalInfo)
        dataStore.savePersonalInfo(personalInfo)
        println("DEBUG: PersonalInfo updated in state: ${_uiState.value.personalInfo}")
        println("DEBUG: Form validation result: ${validateForm(_uiState.value)}")
    }
    
    /**
     * Add work experience
     */
    fun addWorkExperience(experience: WorkExperience) {
        val currentExperience = _uiState.value.experience.toMutableList()
        currentExperience.add(experience)
        _uiState.value = _uiState.value.copy(experience = currentExperience)
        dataStore.saveExperience(currentExperience)
    }
    
    /**
     * Update work experience
     */
    fun updateWorkExperience(index: Int, experience: WorkExperience) {
        val currentExperience = _uiState.value.experience.toMutableList()
        if (index in currentExperience.indices) {
            currentExperience[index] = experience
            _uiState.value = _uiState.value.copy(experience = currentExperience)
            dataStore.saveExperience(currentExperience)
        }
    }
    
    /**
     * Remove work experience
     */
    fun removeWorkExperience(index: Int) {
        val currentExperience = _uiState.value.experience.toMutableList()
        if (index in currentExperience.indices) {
            currentExperience.removeAt(index)
            _uiState.value = _uiState.value.copy(experience = currentExperience)
            dataStore.saveExperience(currentExperience)
        }
    }
    
    /**
     * Add skill
     */
    fun addSkill(skill: String) {
        val currentSkills = _uiState.value.skills.toMutableList()
        if (skill.isNotBlank() && !currentSkills.contains(skill)) {
            currentSkills.add(skill)
            _uiState.value = _uiState.value.copy(skills = currentSkills)
            dataStore.saveSkills(currentSkills)
        }
    }
    
    /**
     * Remove skill
     */
    fun removeSkill(skill: String) {
        val currentSkills = _uiState.value.skills.toMutableList()
        currentSkills.remove(skill)
        _uiState.value = _uiState.value.copy(skills = currentSkills)
        dataStore.saveSkills(currentSkills)
    }
    
    /**
     * Update cover letter
     */
    fun updateCoverLetter(coverLetter: String) {
        _uiState.value = _uiState.value.copy(coverLetter = coverLetter)
        dataStore.saveCoverLetter(coverLetter)
    }
    
    /**
     * Upload document (simplified - just add to list)
     */
    fun uploadDocument(document: Document) {
        val currentDocuments = _uiState.value.documents.toMutableList()
        currentDocuments.add(document)
        _uiState.value = _uiState.value.copy(documents = currentDocuments)
        dataStore.saveDocuments(currentDocuments)
    }
    
    /**
     * Remove document
     */
    fun removeDocument(documentId: String) {
        val currentDocuments = _uiState.value.documents.toMutableList()
        currentDocuments.removeAll { it.id == documentId }
        _uiState.value = _uiState.value.copy(documents = currentDocuments)
        dataStore.saveDocuments(currentDocuments)
    }
    
    /**
     * Submit application
     */
    fun submitApplication(jobId: String, applicationViewModel: com.example.partimes.viewmodels.ApplicationViewModel? = null) {
        viewModelScope.launch {
            println("DEBUG: Submit button clicked, starting submission...")
            println("DEBUG: PersonalInfo: ${_uiState.value.personalInfo}")
            println("DEBUG: Skills: ${_uiState.value.skills}")
            println("DEBUG: Experience: ${_uiState.value.experience}")
            println("DEBUG: CoverLetter: ${_uiState.value.coverLetter}")
            println("DEBUG: Form valid: ${validateForm(_uiState.value)}")
            
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            
            try {
                // Submit profile data to backend
                val user = User(
                    id = null, // Let MongoDB generate the ID
                    email = _uiState.value.personalInfo.email,
                    fullName = _uiState.value.personalInfo.fullName,
                    phoneNumber = _uiState.value.personalInfo.phone,
                    location = _uiState.value.personalInfo.address,
                    dateOfBirth = _uiState.value.personalInfo.dateOfBirth,
                    gender = _uiState.value.personalInfo.gender,
                    bio = _uiState.value.coverLetter,
                    skills = _uiState.value.skills,
                    experience = _uiState.value.experience.joinToString(", ") { "${it.position} at ${it.company}" },
                    education = "", // PersonalInfo doesn't have education field
                    resumeUrl = _uiState.value.documents.find { it.type == DocumentType.RESUME }?.url,
                    coverLetter = _uiState.value.coverLetter,
                    role = UserRole.JOBSEEKER
                )
                
                println("DEBUG: Created User object: $user")
                println("DEBUG: Making API call to updateProfile...")
                
                val response = ApiClient.getApiService().updateProfile(user)
                println("DEBUG: API response received: ${response.code()}")
                println("DEBUG: API response body: ${response.body()}")
                if (response.isSuccessful && response.body()?.get("success") == true) {
                    // Mark form as completed
                    dataStore.setFormCompleted(true)
                    println("DEBUG: Profile updated successfully")
                    
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        isSubmitted = true
                    )
                    println("DEBUG: Profile setup successful, isSubmitted = true")
                } else {
                    val errorMessage = response.body()?.get("message") as? String ?: "Failed to update profile"
                    println("DEBUG: Profile update failed: $errorMessage")
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                println("DEBUG: Profile update exception: ${e.message}")
                println("DEBUG: Exception stack trace: ${e.stackTrace.joinToString("\n")}")
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Failed to update profile"
                )
            }
        }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Validate form - more lenient validation for testing
     */
    private fun validateForm(state: ApplicationFormUiState): Boolean {
        val fullNameValid = state.personalInfo.fullName.isNotBlank()
        val emailValid = state.personalInfo.email.isNotBlank()
        val phoneValid = state.personalInfo.phone.isNotBlank()
        
        println("DEBUG: Form validation - fullName: '$fullNameValid' (${state.personalInfo.fullName}), email: '$emailValid' (${state.personalInfo.email}), phone: '$phoneValid' (${state.personalInfo.phone})")
        
        return fullNameValid && emailValid && phoneValid
               // Removed strict validation for address, dateOfBirth, gender, experience, skills, coverLetter
               // This allows form submission with just basic info for testing
    }
    
    /**
     * Get form completion percentage
     */
    fun getFormCompletionPercentage(): Int {
        return dataStore.getFormCompletionPercentage()
    }
    
    /**
     * Check if form is completed
     */
    fun isFormCompleted(): Boolean {
        return dataStore.isFormCompleted()
    }
}

/**
 * UI State for Application Form
 */
data class ApplicationFormUiState(
    val personalInfo: PersonalInfo = PersonalInfo("", "", "", "", "", ""),
    val experience: List<WorkExperience> = emptyList(),
    val skills: List<String> = emptyList(),
    val coverLetter: String = "",
    val documents: List<Document> = emptyList(),
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)
