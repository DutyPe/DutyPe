package com.example.dutype.employer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.repositories.AIBackendRepository
import com.example.dutype.services.ai.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * AI-Enhanced Job Posting ViewModel
 * 
 * Integrates AI fraud detection into job posting flow:
 * - Real-time field validation
 * - Pre-posting eligibility check
 * - Final AI analysis before save
 * - 3-strike blocking system
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */

data class AIJobPostingUiState(
    // Form fields
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val payAmount: String = "",
    val payType: String = "DAILY",
    val location: String = "",
    val vacancies: String = "1",
    
    // Validation state
    val titleValidation: FieldValidation = FieldValidation(),
    val descriptionValidation: FieldValidation = FieldValidation(),
    val salaryValidation: FieldValidation = FieldValidation(),
    val vacancyValidation: FieldValidation = FieldValidation(),
    
    // Overall state
    val overallRiskScore: Int = 0,
    val canPost: Boolean = true,
    val blockReason: String? = null,
    val blockedAttempts: Int = 0,
    
    // UI state
    val isCheckingEligibility: Boolean = false,
    val isValidatingTitle: Boolean = false,
    val isValidatingDescription: Boolean = false,
    val isValidatingSalary: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitError: String? = null,
    
    // Employer state
    val employerTier: String = "VERIFIED",
    val isSuspended: Boolean = false
)

@HiltViewModel
class AIJobPostingViewModel @Inject constructor(
    private val aiRepository: AIBackendRepository,
    val screeningService: JobPostingScreeningService,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    companion object {
        private const val TAG = "AIJobPosting"
        private const val DEBOUNCE_MS = 500L
    }
    
    private val _uiState = MutableStateFlow(AIJobPostingUiState())
    val uiState: StateFlow<AIJobPostingUiState> = _uiState.asStateFlow()
    
    private var titleCheckJob: Job? = null
    private var descriptionCheckJob: Job? = null
    
    private val employerId: String
        get() = auth.currentUser?.uid ?: ""
    
    init {
        checkEmployerEligibility()
    }
    
    // ============================================================
    // STEP 1: PRE-POSTING ELIGIBILITY CHECK
    // ============================================================
    
    /**
     * Check if employer can post jobs
     * Called on screen load
     */
    fun checkEmployerEligibility() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingEligibility = true)
            
            try {
                val (canPost, reason) = screeningService.canEmployerPost(employerId)
                val blockedAttempts = screeningService.getBlockedAttempts(employerId)
                
                _uiState.value = _uiState.value.copy(
                    isCheckingEligibility = false,
                    canPost = canPost,
                    blockReason = reason,
                    blockedAttempts = blockedAttempts.blockedCount,
                    isSuspended = blockedAttempts.isSuspended
                )
                
                if (!canPost) {
                    Timber.w("$TAG: Employer $employerId cannot post: $reason")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Eligibility check error")
                _uiState.value = _uiState.value.copy(
                    isCheckingEligibility = false,
                    canPost = true // Fail open
                )
            }
        }
    }
    
    // ============================================================
    // STEP 2: REAL-TIME TITLE VALIDATION
    // ============================================================
    
    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
        
        // Cancel previous check
        titleCheckJob?.cancel()
        
        if (title.length < 3) {
            _uiState.value = _uiState.value.copy(
                titleValidation = FieldValidation(),
                isValidatingTitle = false
            )
            return
        }
        
        // Debounced check
        titleCheckJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            _uiState.value = _uiState.value.copy(isValidatingTitle = true)
            
            val validation = screeningService.checkTitle(title)
            
            _uiState.value = _uiState.value.copy(
                titleValidation = validation,
                isValidatingTitle = false
            )
            
            updateOverallRisk()
        }
    }
    
    // ============================================================
    // STEP 3: DESCRIPTION VALIDATION
    // ============================================================
    
    fun onDescriptionChanged(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
        
        descriptionCheckJob?.cancel()
        
        if (description.length < 10) {
            _uiState.value = _uiState.value.copy(
                descriptionValidation = FieldValidation(),
                isValidatingDescription = false
            )
            return
        }
        
        descriptionCheckJob = viewModelScope.launch {
            delay(DEBOUNCE_MS * 2) // Longer debounce for description
            _uiState.value = _uiState.value.copy(isValidatingDescription = true)
            
            val validation = screeningService.checkDescription(
                _uiState.value.title,
                description
            )
            
            _uiState.value = _uiState.value.copy(
                descriptionValidation = validation,
                isValidatingDescription = false
            )
            
            updateOverallRisk()
        }
    }
    
    // ============================================================
    // STEP 4: SALARY VALIDATION
    // ============================================================
    
    fun onSalaryChanged(amount: String, payType: String) {
        _uiState.value = _uiState.value.copy(
            payAmount = amount,
            payType = payType
        )
        
        val amountDouble = amount.toDoubleOrNull() ?: return
        // REMOVED: Category auto-detection - will be done from title/description
        // For now, use a generic category for salary validation
        
        if (amountDouble <= 0) return
        
        // Fetch market rates using auto-detected category from title
        val detectedCategory = com.example.dutype.utils.CategoryDetector.detectCategory(
            _uiState.value.title,
            _uiState.value.description
        )
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isValidatingSalary = true)
            
            val validation = screeningService.checkSalary(detectedCategory, amountDouble, payType)
            
            _uiState.value = _uiState.value.copy(
                salaryValidation = validation,
                isValidatingSalary = false
            )
            
            updateOverallRisk()
        }
    }
    
    // ============================================================
    // OTHER FIELD UPDATES
    // ============================================================
    
    fun onCategoryChanged(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
        
        // Re-validate salary if already entered
        val amount = _uiState.value.payAmount.toDoubleOrNull()
        if (amount != null) {
            onSalaryChanged(_uiState.value.payAmount, _uiState.value.payType)
        }
    }
    
    fun onLocationChanged(location: String) {
        _uiState.value = _uiState.value.copy(location = location)
    }
    
    fun onVacanciesChanged(vacancies: String) {
        _uiState.value = _uiState.value.copy(vacancies = vacancies)
    }
    
    // ============================================================
    // STEP 6: FINAL CHECK & SUBMIT
    // ============================================================
    
    /**
     * Submit job posting with final AI check
     */
    fun submitJob() {
        val state = _uiState.value
        
        // Basic validation
        if (state.title.isBlank() || state.description.isBlank() || 
            state.payAmount.isBlank()) {
            _uiState.value = state.copy(submitError = "Please fill all required fields")
            return
        }
        
        // Check if already blocked
        if (!state.canPost || state.isSuspended) {
            _uiState.value = state.copy(submitError = state.blockReason ?: "Cannot post jobs")
            return
        }
        
        // Check field validations
        if (!state.titleValidation.isValid || !state.descriptionValidation.isValid) {
            _uiState.value = state.copy(submitError = "Please fix the highlighted issues")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
            
            try {
                val jobId = UUID.randomUUID().toString()
                
                // Final AI check
                val result = screeningService.finalCheck(
                    jobId = jobId,
                    title = state.title,
                    description = state.description,
                    category = "", // Auto-detected
                    payAmount = state.payAmount.toDoubleOrNull() ?: 0.0,
                    payType = state.payType,
                    location = state.location,
                    vacancies = state.vacancies.toIntOrNull() ?: 1,
                    employerId = employerId,
                    employerTier = state.employerTier,
                    employerHistory = null // TODO: Fetch from Firestore
                )
                
                result.fold(
                    onSuccess = { analysis ->
                        if (analysis.shouldBlock) {
                            // Job blocked
                            val newBlockedCount = state.blockedAttempts + 1
                            _uiState.value = _uiState.value.copy(
                                isSubmitting = false,
                                submitError = "Job blocked: ${analysis.flags.firstOrNull() ?: analysis.reasoning}",
                                blockedAttempts = newBlockedCount,
                                canPost = newBlockedCount < JobPostingScreeningService.MAX_BLOCKED_ATTEMPTS
                            )
                            
                            if (newBlockedCount >= JobPostingScreeningService.MAX_BLOCKED_ATTEMPTS) {
                                _uiState.value = _uiState.value.copy(
                                    isSuspended = true,
                                    blockReason = "Account suspended due to multiple policy violations"
                                )
                            }
                        } else {
                            // Job approved - save to Firestore
                            saveJobToFirestore(jobId, analysis)
                        }
                    },
                    onFailure = { error ->
                        Timber.e("$TAG: Final check failed: ${error.message}")
                        // Allow posting if AI backend is down (fail open)
                        saveJobToFirestore(jobId, null)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Submit error")
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    submitError = "Failed to post job: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Save job to Firestore after AI approval
     */
    private suspend fun saveJobToFirestore(jobId: String, analysis: JobAnalysisResponse?) {
        try {
            val state = _uiState.value
            
            val jobData = hashMapOf(
                "id" to jobId,
                "title" to state.title,
                "description" to state.description,
                "category" to "", // Auto-detected
                "payAmount" to (state.payAmount.toDoubleOrNull() ?: 0.0),
                "payType" to state.payType,
                "location" to state.location,
                "vacancies" to (state.vacancies.toIntOrNull() ?: 1),
                "employerId" to employerId,
                "postedAt" to System.currentTimeMillis(),
                "status" to "ACTIVE",
                "isHidden" to false,
                // AI metadata
                "aiRiskScore" to (analysis?.riskScore ?: 0),
                "aiRiskLevel" to (analysis?.riskLevel ?: "UNKNOWN"),
                "aiFlags" to (analysis?.flags ?: emptyList<String>()),
                "aiReviewed" to (analysis != null)
            )
            
            // Apply shadow ban if high risk but not blocked
            if (analysis != null && analysis.riskScore >= 50 && !analysis.shouldBlock) {
                jobData["visibility"] = "LIMITED" // Shadow ban
                jobData["shadowBanReason"] = "High risk score: ${analysis.riskScore}"
                Timber.w("$TAG: Job $jobId shadow banned (risk: ${analysis.riskScore})")
            }
            
            firestore.collection("jobs")
                .document(jobId)
                .set(jobData)
                .await()
            
            // Register for duplicate detection
            aiRepository.checkDuplicate(jobId, state.title, state.description, employerId)
            
            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                submitSuccess = true
            )
            
            Timber.d("$TAG: Job $jobId posted successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Firestore save error")
            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                submitError = "Failed to save job: ${e.message}"
            )
        }
    }
    
    // ============================================================
    // UTILITY
    // ============================================================
    
    private fun updateOverallRisk() {
        val state = _uiState.value
        val totalRisk = listOf(
            state.titleValidation.riskScore,
            state.descriptionValidation.riskScore,
            state.salaryValidation.riskScore,
            state.vacancyValidation.riskScore
        ).maxOrNull() ?: 0
        
        _uiState.value = state.copy(overallRiskScore = totalRisk)
    }
    
    fun resetForm() {
        screeningService.resetValidation()
        _uiState.value = AIJobPostingUiState(
            employerTier = _uiState.value.employerTier,
            blockedAttempts = _uiState.value.blockedAttempts,
            isSuspended = _uiState.value.isSuspended
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(submitError = null)
    }
}
