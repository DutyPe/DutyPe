package com.example.dutype.employer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
import com.example.dutype.repositories.FirestoreJobRepository
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.utils.CategoryDetector
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VoiceJobPostingViewModel @Inject constructor(
    private val jobRepository: FirestoreJobRepository,
    private val profileService: ProfileCompletionService,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(VoiceJobPostingState())
    val uiState: StateFlow<VoiceJobPostingState> = _uiState.asStateFlow()
    
    private val _jobData = MutableStateFlow(VoiceJobData())
    val jobData: StateFlow<VoiceJobData> = _jobData.asStateFlow()
    
    init {
        loadEmployerProfile()
    }
    
    private fun loadEmployerProfile() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val result = profileService.getEmployerProfileData(userId)
                
                result.onSuccess { profile ->
                    _jobData.value = _jobData.value.copy(
                        contactPhone = profile["contactPhone"] as? String ?: "",
                        companyName = profile["companyName"] as? String ?: "",
                        description = _jobData.value.description
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load employer profile")
            }
        }
    }

    fun startVoicePosting() {
        _uiState.value = VoiceJobPostingState(
            currentStep = VoiceStep.JOB_TITLE,
            isListening = false,
            isSpeaking = true
        )
    }
    
    fun processVoiceInput(text: String) {
        Timber.d("🎤 Processing voice input: $text for step ${_uiState.value.currentStep}")
        
        when (_uiState.value.currentStep) {
            VoiceStep.JOB_TITLE -> handleJobTitle(text)
            VoiceStep.LOCATION -> handleLocation(text)
            VoiceStep.SALARY -> handleSalary(text)
            VoiceStep.DESCRIPTION -> handleDescription(text)
            VoiceStep.CONTACT -> handleContact(text)
            VoiceStep.CONFIRMATION -> handleConfirmation(text)
            VoiceStep.COMPLETED -> {}
        }
    }
    
    private fun handleJobTitle(text: String) {
        val categoryString = CategoryDetector.detectCategory(text, "")
        val category = try {
            JobCategory.valueOf(categoryString.uppercase())
        } catch (e: Exception) {
            JobCategory.OTHER
        }
        _jobData.value = _jobData.value.copy(
            jobTitle = text,
            category = category,
            description = _jobData.value.description
        )
        moveToNextStep()
    }
    
    private fun handleLocation(text: String) {
        _jobData.value = _jobData.value.copy(
            location = text,
            description = _jobData.value.description
        )
        moveToNextStep()
    }
    
    private fun handleSalary(text: String) {
        val numbers = text.replace(Regex("[^0-9]"), "")
        val payType = when {
            text.contains("month", ignoreCase = true) -> PayType.MONTHLY
            text.contains("day", ignoreCase = true) || text.contains("daily", ignoreCase = true) -> PayType.DAILY
            text.contains("hour", ignoreCase = true) -> PayType.HOURLY
            text.contains("task", ignoreCase = true) || text.contains("project", ignoreCase = true) -> PayType.TASK
            else -> PayType.MONTHLY
        }
        _jobData.value = _jobData.value.copy(
            payAmount = numbers,
            payType = payType,
            description = _jobData.value.description
        )
        moveToNextStep()
    }
    
    private fun handleDescription(text: String) {
        _jobData.value = _jobData.value.copy(description = text)
        moveToNextStep()
    }
    
    private fun handleContact(text: String) {
        val numbers = text.replace(Regex("[^0-9]"), "")
        val phone = if (numbers.length >= 10) numbers.takeLast(10) else _jobData.value.contactPhone
        _jobData.value = _jobData.value.copy(
            contactPhone = phone,
            description = _jobData.value.description
        )
        moveToNextStep()
    }
    
    private fun handleConfirmation(text: String) {
        val confirmed = text.contains("yes", ignoreCase = true) || 
                       text.contains("post", ignoreCase = true) ||
                       text.contains("confirm", ignoreCase = true)
        if (confirmed) {
            postJob()
        } else {
            _uiState.value = _uiState.value.copy(
                currentStep = VoiceStep.JOB_TITLE,
                errorMessage = "Job posting cancelled"
            )
        }
    }

    private fun moveToNextStep() {
        val nextStep = when (_uiState.value.currentStep) {
            VoiceStep.JOB_TITLE -> VoiceStep.LOCATION
            VoiceStep.LOCATION -> VoiceStep.SALARY
            VoiceStep.SALARY -> VoiceStep.DESCRIPTION
            VoiceStep.DESCRIPTION -> VoiceStep.CONTACT
            VoiceStep.CONTACT -> VoiceStep.CONFIRMATION
            VoiceStep.CONFIRMATION -> VoiceStep.COMPLETED
            VoiceStep.COMPLETED -> VoiceStep.COMPLETED
        }
        _uiState.value = _uiState.value.copy(
            currentStep = nextStep,
            isListening = false,
            isSpeaking = true
        )
    }
    
    fun goToPreviousStep() {
        val previousStep = when (_uiState.value.currentStep) {
            VoiceStep.JOB_TITLE -> VoiceStep.JOB_TITLE
            VoiceStep.LOCATION -> VoiceStep.JOB_TITLE
            VoiceStep.SALARY -> VoiceStep.LOCATION
            VoiceStep.DESCRIPTION -> VoiceStep.SALARY
            VoiceStep.CONTACT -> VoiceStep.DESCRIPTION
            VoiceStep.CONFIRMATION -> VoiceStep.CONTACT
            VoiceStep.COMPLETED -> VoiceStep.CONFIRMATION
        }
        _uiState.value = _uiState.value.copy(currentStep = previousStep, isSpeaking = true)
    }
    
    fun startListening() {
        _uiState.value = _uiState.value.copy(isListening = true, isSpeaking = false)
    }
    
    fun stopListening() {
        _uiState.value = _uiState.value.copy(isListening = false)
    }
    
    fun finishSpeaking() {
        _uiState.value = _uiState.value.copy(isSpeaking = false)
    }

    private fun postJob() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isPosting = true)
                val userId = auth.currentUser?.uid ?: throw Exception("Not logged in")
                val data = _jobData.value
                
                val jobData = mapOf(
                    "title" to data.jobTitle,
                    "payAmount" to data.payAmount,
                    "payType" to data.payType.name,
                    "location" to data.location,
                    "description" to data.description,
                    "contactNumber" to data.contactPhone,
                    "category" to data.category.name,
                    "shiftTiming" to data.shiftTiming.name,
                    "urgency" to "FLEXIBLE",
                    "vacancies" to data.vacancies,
                    "employerId" to userId,
                    "employerName" to data.companyName,
                    "isActive" to true,
                    "isFilled" to false,
                    "postedTime" to System.currentTimeMillis()
                )
                
                jobRepository.createJob(jobData).collect { result ->
                    result.onSuccess { jobId ->
                        _uiState.value = _uiState.value.copy(
                            currentStep = VoiceStep.COMPLETED,
                            isPosting = false,
                            successMessage = "Job posted successfully!"
                        )
                        Timber.d("✅ Job posted via voice: $jobId")
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isPosting = false,
                            errorMessage = error.message ?: "Failed to post job"
                        )
                        Timber.e(error, "❌ Failed to post job")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPosting = false,
                    errorMessage = e.message ?: "Failed to post job"
                )
                Timber.e(e, "❌ Error posting job")
            }
        }
    }
    
    fun reset() {
        _uiState.value = VoiceJobPostingState()
        _jobData.value = VoiceJobData()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

enum class VoiceStep {
    JOB_TITLE, LOCATION, SALARY, DESCRIPTION, CONTACT, CONFIRMATION, COMPLETED
}

data class VoiceJobPostingState(
    val currentStep: VoiceStep = VoiceStep.JOB_TITLE,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isPosting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class VoiceJobData(
    val jobTitle: String = "",
    val category: JobCategory = JobCategory.OTHER,
    val location: String = "",
    val payAmount: String = "",
    val payType: PayType = PayType.MONTHLY,
    val shiftTiming: ShiftTiming = ShiftTiming.FLEXIBLE,
    val description: String = "",
    val vacancies: Int = 1,
    val contactPhone: String = "",
    val companyName: String = ""
)
