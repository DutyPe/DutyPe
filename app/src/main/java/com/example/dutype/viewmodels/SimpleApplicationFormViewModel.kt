package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.worker.models.*
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import com.example.dutype.utils.ValidationUtils

/**
 * Simplified ViewModel for managing job application form state and operations
 * Uses ApplicationFormDataStore for persistent storage
 * Includes auto-save functionality to preserve user data
 */
@HiltViewModel
class SimpleApplicationFormViewModel @Inject constructor(
    private val dataStore: ApplicationFormDataStore,
    private val authManager: AuthManager,
    private val firestoreService: com.example.dutype.services.FirestoreService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ApplicationFormUiState())
    val uiState: StateFlow<ApplicationFormUiState> = _uiState.asStateFlow()
    
    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()
    
    // Auto-save state
    private var autoSaveJob: Job? = null
    private val _lastAutoSaveTime = MutableStateFlow<Long?>(null)
    val lastAutoSaveTime: StateFlow<Long?> = _lastAutoSaveTime.asStateFlow()
    
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
            
            Timber.d("Loading saved application data")
            Timber.d("PersonalInfo: $updatedPersonalInfo")
            Timber.d("Google Email: $googleEmail")
            Timber.d("Google DisplayName: $googleDisplayName")
            Timber.d("Experience: ${experience.size} items")
            Timber.d("Skills: ${skills.size} items")
            Timber.d("CoverLetter: ${coverLetter.take(50)}...")
            Timber.d("Documents: ${documents.size} items")
            
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
        Timber.d("updatePersonalInfo called")
        _uiState.value = _uiState.value.copy(personalInfo = personalInfo)
        dataStore.savePersonalInfo(personalInfo)
        triggerAutoSave()
        Timber.d("PersonalInfo updated in state")
    }
    
    /**
     * Add work experience
     */
    fun addWorkExperience(experience: WorkExperience) {
        val currentExperience = _uiState.value.experience.toMutableList()
        currentExperience.add(experience)
        _uiState.value = _uiState.value.copy(experience = currentExperience)
        dataStore.saveExperience(currentExperience)
        triggerAutoSave()
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
            triggerAutoSave()
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
            triggerAutoSave()
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
            triggerAutoSave()
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
        triggerAutoSave()
    }
    
    /**
     * Update cover letter
     */
    fun updateCoverLetter(coverLetter: String) {
        _uiState.value = _uiState.value.copy(coverLetter = coverLetter)
        dataStore.saveCoverLetter(coverLetter)
        triggerAutoSave()
    }
    
    /**
     * Upload document (simplified - just add to list)
     */
    fun uploadDocument(document: Document) {
        val currentDocuments = _uiState.value.documents.toMutableList()
        currentDocuments.add(document)
        _uiState.value = _uiState.value.copy(documents = currentDocuments)
        dataStore.saveDocuments(currentDocuments)
        triggerAutoSave()
    }
    
    /**
     * Remove document
     */
    fun removeDocument(documentId: String) {
        val currentDocuments = _uiState.value.documents.toMutableList()
        currentDocuments.removeAll { it.id == documentId }
        _uiState.value = _uiState.value.copy(documents = currentDocuments)
        dataStore.saveDocuments(currentDocuments)
        triggerAutoSave()
    }
    
    /**
     * Auto-save draft functionality
     * Triggers a debounced save operation after 2 seconds of inactivity
     */
    private fun triggerAutoSave() {
        // Cancel previous auto-save job if still running
        autoSaveJob?.cancel()
        
        // Start new auto-save job with debounce
        autoSaveJob = viewModelScope.launch {
            delay(2000) // 2 second debounce
            saveDraft()
        }
    }
    
    /**
     * Save current form state as draft
     */
    fun saveDraft() {
        viewModelScope.launch {
            try {
                // All data is already saved to dataStore in individual update methods
                // Just update the timestamp
                _lastAutoSaveTime.value = System.currentTimeMillis()
                Timber.i("Application draft auto-saved successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error auto-saving draft")
            }
        }
    }
    
    /**
     * Submit application - Store complete worker profile in Firestore
     */
    fun submitApplication(jobId: String, jobApplicationViewModel: com.example.dutype.viewmodels.JobApplicationViewModel? = null) {
        viewModelScope.launch {
            Timber.i("Starting worker profile submission to Firestore")
            Timber.d("PersonalInfo: ${_uiState.value.personalInfo}")
            Timber.d("Skills: ${_uiState.value.skills}")
            Timber.d("Experience: ${_uiState.value.experience}")
            Timber.d("CoverLetter length: ${_uiState.value.coverLetter.length}")
            
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            
            try {
                // Get current user ID from Firebase Auth
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    throw Exception("User not authenticated")
                }
                
                val userId = currentUser.uid
                Timber.d("User ID: $userId")
                
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
                        Timber.i("Worker profile successfully saved to Firestore!")
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
                Timber.e(e, "Error during worker profile submission")
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
        val fullNameValid = ValidationUtils.isValidFullName(state.personalInfo.fullName)
        val emailValid = ValidationUtils.isValidEmail(state.personalInfo.email)
        val phoneValid = ValidationUtils.isValidIndianPhoneNumber(state.personalInfo.phone)
        
        Timber.d("Form validation - fullName: $fullNameValid, email: $emailValid, phone: $phoneValid")
        
        return fullNameValid && emailValid && phoneValid
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
