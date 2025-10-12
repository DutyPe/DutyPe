package com.example.partimes.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.worker.models.*
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
    private val authManager: AuthManager,
    private val firestoreService: com.example.partimes.services.FirestoreService
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
     * Load saved data from data store and pre-populate with Google Sign-In data
     */
    private fun loadSavedData() {
        viewModelScope.launch {
            val personalInfo = dataStore.getPersonalInfo()
            val experience = dataStore.getExperience()
            val skills = dataStore.getSkills()
            val coverLetter = dataStore.getCoverLetter()
            val documents = dataStore.getDocuments()
            
            // Get Google Sign-In user data
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val googleEmail = currentUser?.email ?: ""
            val googleDisplayName = currentUser?.displayName ?: ""
            
            // Pre-populate with Google Sign-In data if not already set
            val updatedPersonalInfo = personalInfo.copy(
                email = if (personalInfo.email.isBlank() && googleEmail.isNotBlank()) googleEmail else personalInfo.email,
                fullName = if (personalInfo.fullName.isBlank() && googleDisplayName.isNotBlank()) googleDisplayName else personalInfo.fullName
            )
            
            println("DEBUG: Loading saved data:")
            println("DEBUG: PersonalInfo: $updatedPersonalInfo")
            println("DEBUG: Google Email: $googleEmail")
            println("DEBUG: Google DisplayName: $googleDisplayName")
            println("DEBUG: Experience: $experience")
            println("DEBUG: Skills: $skills")
            println("DEBUG: CoverLetter: $coverLetter")
            println("DEBUG: Documents: $documents")
            
            _uiState.value = ApplicationFormUiState(
                personalInfo = updatedPersonalInfo,
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
     * Submit application - Store complete worker profile in Firestore
     */
    fun submitApplication(jobId: String, jobApplicationViewModel: com.example.partimes.viewmodels.JobApplicationViewModel? = null) {
        viewModelScope.launch {
            println("🔥 Starting worker profile submission to Firestore...")
            println("📊 PersonalInfo: ${_uiState.value.personalInfo}")
            println("📊 Skills: ${_uiState.value.skills}")
            println("📊 Experience: ${_uiState.value.experience}")
            println("📊 CoverLetter: ${_uiState.value.coverLetter}")
            
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            
            try {
                // Get current user ID from Firebase Auth
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    throw Exception("User not authenticated")
                }
                
                val userId = currentUser.uid
                println("👤 User ID: $userId")
                
                // Prepare worker profile data for Firestore
                val workerProfileData = mapOf(
                    // Personal Information
                    "fullName" to _uiState.value.personalInfo.fullName,
                    "email" to _uiState.value.personalInfo.email,
                    "phone" to _uiState.value.personalInfo.phone,
                    "address" to _uiState.value.personalInfo.address,
                    "dateOfBirth" to _uiState.value.personalInfo.dateOfBirth,
                    "gender" to _uiState.value.personalInfo.gender,
                    
                    // Professional Information
                    "skills" to _uiState.value.skills,
                    "experience" to _uiState.value.experience.map { exp ->
                        mapOf(
                            "position" to exp.position,
                            "company" to exp.company,
                            "startDate" to exp.startDate,
                            "endDate" to exp.endDate,
                            "description" to exp.description,
                            "isCurrent" to exp.isCurrent
                        )
                    },
                    "coverLetter" to _uiState.value.coverLetter,
                    "resumeUrl" to (_uiState.value.documents.find { it.type == DocumentType.RESUME }?.url ?: ""),
                    "education" to "", // Can be added later if needed
                    
                    // Documents
                    "documents" to _uiState.value.documents.map { doc ->
                        mapOf(
                            "type" to doc.type.name,
                            "url" to (doc.url ?: ""),
                            "name" to doc.name
                        )
                    },
                    
                    // Metadata
                    "profileCompleted" to true,
                    "completedAt" to System.currentTimeMillis()
                )
                
                // Store in Firestore
                val result = firestoreService.createOrUpdateWorkerProfile(userId, workerProfileData)
                
                if (result.isSuccess) {
                    // Update main user record to mark profile as complete
                    val userResult = firestoreService.createOrUpdateUser(
                        User(
                            id = userId,
                            email = currentUser.email ?: _uiState.value.personalInfo.email,
                            fullName = currentUser.displayName ?: _uiState.value.personalInfo.fullName,
                            role = UserRole.WORKER,
                            isProfileComplete = true,
                            isVerified = true,
                            isActive = true,
                            createdAt = System.currentTimeMillis(),
                            lastLoginAt = System.currentTimeMillis()
                        )
                    )
                    
                    if (userResult.isSuccess) {
                        println("✅ Worker profile successfully saved to Firestore!")
                        dataStore.setFormCompleted(true)
                        
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            isSubmitted = true
                        )
                    } else {
                        throw Exception("Failed to update user record: ${userResult.exceptionOrNull()?.message}")
                    }
                } else {
                    throw Exception("Failed to save worker profile: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                println("❌ Error during worker profile submission: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Failed to save profile: ${e.message}"
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
