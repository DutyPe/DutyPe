package com.example.dutype.employer.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.repositories.AIBackendRepository
import com.example.dutype.services.ai.*
import com.example.dutype.utils.GeoUtils
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
        private const val COLLECTION_USERS = FirestoreCollections.USERS
        private const val COLLECTION_EMPLOYER_PROFILES = FirestoreCollections.EMPLOYER_PROFILES
        private const val COLLECTION_JOBS = FirestoreCollections.JOBS
        private const val COLLECTION_JOB_DETAILS = FirestoreCollections.JOB_DETAILS
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
            val salary = state.payAmount.toDoubleOrNull()
            if (salary == null || salary <= 0.0) {
                _uiState.value = state.copy(
                    isSubmitting = false,
                    submitError = "Please enter a valid salary before posting"
                )
                return
            }

            if (state.title.isBlank() || state.description.isBlank() || state.location.isBlank()) {
                _uiState.value = state.copy(
                    isSubmitting = false,
                    submitError = "Title, description, and address are required"
                )
                return
            }

            val userDoc = firestore.collection(COLLECTION_USERS).document(employerId).get().await()
            val employerDoc = firestore.collection(COLLECTION_EMPLOYER_PROFILES).document(employerId).get().await()
            val companyName = employerDoc.getString("companyName")?.trim().orEmpty()
            val contactNumber = userDoc.getString("phone")?.trim().orEmpty()
            val coordinates = parseCoordinates(state.location)
                ?: parseLocationMap(userDoc.get("location") as? Map<*, *>)

            if (coordinates == null || !GeoUtils.hasValidCoordinates(coordinates.first, coordinates.second)) {
                _uiState.value = state.copy(
                    isSubmitting = false,
                    submitError = "Valid GPS location is required before posting"
                )
                return
            }

            if (companyName.isBlank()) {
                _uiState.value = state.copy(
                    isSubmitting = false,
                    submitError = "Employer company name is required"
                )
                return
            }
            if (contactNumber.isBlank()) {
                _uiState.value = state.copy(
                    isSubmitting = false,
                    submitError = "Employer phone number is required"
                )
                return
            }

            val createdAtMillis = System.currentTimeMillis()
            val createdAt = com.google.firebase.Timestamp.now()
            val expiresAt = com.google.firebase.Timestamp(
                java.util.Date(createdAtMillis + (15L * 24 * 60 * 60 * 1000L))
            )
            val geohash = GeoUtils.encodeGeohash(coordinates.first, coordinates.second)
            val resolvedJobType = state.category.ifBlank {
                com.example.dutype.utils.CategoryDetector.detectCategory(state.title, state.description)
            }.ifBlank { "OTHER" }

            val cityFromAddress = state.location.trim().split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .let { parts ->
                    when {
                        parts.isEmpty() -> ""
                        parts.size >= 3 -> parts[parts.size - 2]
                        parts.size == 2 -> parts[1]
                        else -> parts[0]
                    }
                }

            val jobData = hashMapOf<String, Any>(
                "employerId" to employerId,
                "companyName" to companyName,
                "title" to state.title.trim(),
                "jobType" to resolvedJobType,
                "salary" to salary,
                "salaryType" to state.payType.trim().uppercase().ifBlank { "DAILY" },
                "addressText" to state.location.trim(),
                "urgency" to "MEDIUM",
                "location" to mapOf("lat" to coordinates.first, "lng" to coordinates.second),
                "geohash" to geohash,
                "companyCity" to cityFromAddress,
                "status" to "open",
                "createdAt" to createdAt,
                "expiresAt" to expiresAt
            )

            val detailsData = hashMapOf<String, Any>(
                "description" to state.description.trim(),
                "contactNumber" to contactNumber,
                "whatsappNumber" to "",
                "addressText" to state.location.trim(),
                "jobType" to resolvedJobType,
                "vacancies" to (state.vacancies.toIntOrNull() ?: 1),
                "workingHours" to "",
                "educationRequired" to "",
                "benefits" to emptyList<String>()
            )

            val batch = firestore.batch()
            val jobRef = firestore.collection(COLLECTION_JOBS).document(jobId)
            val detailsRef = firestore.collection(COLLECTION_JOB_DETAILS).document(jobId)
            batch.set(jobRef, jobData)
            batch.set(detailsRef, detailsData)
            batch.commit().await()
            
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

    private fun parseCoordinates(locationText: String): Pair<Double, Double>? {
        val parts = locationText.split(',').map { it.trim() }
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        return lat to lng
    }

    private fun parseLocationMap(locationMap: Map<*, *>?): Pair<Double, Double>? {
        val lat = (locationMap?.get("lat") as? Number)?.toDouble() ?: return null
        val lng = (locationMap["lng"] as? Number)?.toDouble() ?: return null
        return lat to lng
    }
}
