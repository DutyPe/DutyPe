package com.example.dutype.services.ai

import com.example.dutype.repositories.AIBackendRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Job Posting Screening Service
 * 
 * Integrates AI fraud detection into job posting flow
 * 
 * Key Features:
 * - Real-time field validation (title, description, salary)
 * - 3-strike blocking system (3 blocked attempts = suspension)
 * - Pre-posting AI analysis
 * - Shadow banning for suspicious employers
 * 
 * Flow:
 * 1. Pre-posting check (employer eligibility)
 * 2. Real-time title analysis
 * 3. Deep description analysis
 * 4. Salary validation against market rates
 * 5. Vacancy validation by employer tier
 * 6. Final comprehensive AI check
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class JobPostingScreeningService @Inject constructor(
    private val aiRepository: AIBackendRepository,
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val TAG = "JobScreening"
        const val MAX_BLOCKED_ATTEMPTS = 3
        private const val BLOCKED_ATTEMPTS_COLLECTION = "employer_blocked_attempts"
    }
    
    // Current validation state
    private val _validationState = MutableStateFlow(JobPostingValidationState())
    val validationState: StateFlow<JobPostingValidationState> = _validationState.asStateFlow()
    
    // ============================================================
    // PRE-POSTING CHECK - Step 1
    // ============================================================
    
    /**
     * Check if employer can post jobs
     * Call BEFORE showing job posting form
     * 
     * @return Pair<canPost, reason>
     */
    suspend fun canEmployerPost(employerId: String): Pair<Boolean, String?> {
        return try {
            // Check blocked attempts
            val blockedAttempts = getBlockedAttempts(employerId)
            
            if (blockedAttempts.isSuspended) {
                Timber.w("$TAG: Employer $employerId is SUSPENDED")
                return Pair(false, "Your account is suspended due to multiple policy violations. Contact support.")
            }
            
            if (blockedAttempts.blockedCount >= MAX_BLOCKED_ATTEMPTS) {
                // Auto-suspend
                suspendEmployer(employerId, "Exceeded maximum blocked attempts")
                return Pair(false, "Your account has been suspended. Contact support.")
            }
            
            // Check employer privileges from AI backend
            val privilegesResult = aiRepository.getEmployerPrivileges(employerId)
            privilegesResult.fold(
                onSuccess = { privileges ->
                    if (privileges.postingLimitPerDay <= 0) {
                        Pair(false, "You have reached your daily posting limit")
                    } else {
                        Pair(true, null)
                    }
                },
                onFailure = {
                    // Allow posting if backend is unavailable (fail open for UX)
                    Timber.w("$TAG: AI backend unavailable, allowing post")
                    Pair(true, null)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error checking employer eligibility")
            Pair(true, null) // Fail open
        }
    }
    
    // ============================================================
    // REAL-TIME TITLE CHECK - Step 2
    // ============================================================
    
    /**
     * Check job title in real-time
     * Call on title field change (debounced)
     */
    suspend fun checkTitle(title: String): FieldValidation {
        if (title.isBlank() || title.length < 3) {
            return FieldValidation(isValid = true)
        }
        
        return try {
            val result = aiRepository.checkKeywords(title)
            result.fold(
                onSuccess = { response ->
                    val validation = FieldValidation(
                        isValid = !response.shouldBlock,
                        warnings = response.suspiciousKeywords.map { "Suspicious: '$it'" },
                        errors = response.bannedKeywords.map { "Banned keyword: '$it'" },
                        riskScore = if (response.hasBannedKeywords) 100 else 
                            response.suspiciousKeywords.size * 20
                    )
                    
                    _validationState.value = _validationState.value.copy(
                        titleCheck = validation,
                        isValid = validation.isValid && _validationState.value.descriptionCheck.isValid
                    )
                    
                    validation
                },
                onFailure = {
                    FieldValidation(isValid = true) // Fail open
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Title check error")
            FieldValidation(isValid = true)
        }
    }
    
    // ============================================================
    // DESCRIPTION ANALYSIS - Step 3
    // ============================================================
    
    /**
     * Deep analysis of job description
     * Call on description field blur or after typing stops
     */
    suspend fun checkDescription(title: String, description: String): FieldValidation {
        if (description.isBlank() || description.length < 10) {
            return FieldValidation(isValid = true)
        }
        
        return try {
            val result = aiRepository.quickCheckJob(title, description)
            result.fold(
                onSuccess = { response ->
                    val warnings = mutableListOf<String>()
                    val errors = mutableListOf<String>()
                    
                    response.bannedKeywords.forEach { errors.add("Banned: '$it'") }
                    response.suspiciousKeywords.forEach { warnings.add("Suspicious: '$it'") }
                    response.riskIndicators.forEach { warnings.add(it) }
                    
                    val validation = FieldValidation(
                        isValid = !response.shouldBlock,
                        warnings = warnings,
                        errors = errors,
                        riskScore = if (response.hasBannedKeywords) 100 else 
                            (response.suspiciousKeywords.size * 15 + response.riskIndicators.size * 10)
                                .coerceAtMost(80)
                    )
                    
                    _validationState.value = _validationState.value.copy(
                        descriptionCheck = validation,
                        isValid = validation.isValid && _validationState.value.titleCheck.isValid
                    )
                    
                    validation
                },
                onFailure = {
                    FieldValidation(isValid = true)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Description check error")
            FieldValidation(isValid = true)
        }
    }
    
    // ============================================================
    // SALARY VALIDATION - Step 4
    // ============================================================
    
    /**
     * Validate salary against market rates
     */
    suspend fun checkSalary(
        category: String,
        payAmount: Double,
        payType: String
    ): FieldValidation {
        return try {
            val result = aiRepository.validatePayRate(category, payAmount, payType)
            result.fold(
                onSuccess = { response ->
                    val validation = FieldValidation(
                        isValid = response.isValid,
                        warnings = if (!response.isValid) listOf(response.reason) else emptyList(),
                        errors = emptyList(),
                        riskScore = if (response.isValid) 0 else 50
                    )
                    
                    _validationState.value = _validationState.value.copy(
                        salaryCheck = validation
                    )
                    
                    validation
                },
                onFailure = {
                    FieldValidation(isValid = true)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Salary check error")
            FieldValidation(isValid = true)
        }
    }
    
    // ============================================================
    // FINAL AI CHECK - Step 6
    // ============================================================
    
    /**
     * Final comprehensive AI analysis before posting
     * Call when user clicks "Post Job"
     * 
     * @return JobAnalysisResponse with final decision
     */
    suspend fun finalCheck(
        jobId: String,
        title: String,
        description: String,
        category: String,
        payAmount: Double,
        payType: String,
        location: String,
        vacancies: Int,
        employerId: String,
        employerTier: String,
        employerHistory: EmployerHistory?
    ): Result<JobAnalysisResponse> {
        return try {
            val request = JobAnalysisRequest(
                jobId = jobId,
                title = title,
                description = description,
                category = category,
                payAmount = payAmount,
                payType = payType,
                location = location,
                vacancies = vacancies,
                employerId = employerId,
                employerTier = employerTier,
                employerHistory = employerHistory
            )
            
            val result = aiRepository.analyzeJob(request)
            
            result.fold(
                onSuccess = { response ->
                    // Update validation state
                    _validationState.value = _validationState.value.copy(
                        overallRiskScore = response.riskScore,
                        shouldBlock = response.shouldBlock,
                        blockReason = if (response.shouldBlock) {
                            response.flags.firstOrNull() ?: response.reasoning
                        } else null
                    )
                    
                    // If blocked, record the attempt
                    if (response.shouldBlock) {
                        recordBlockedAttempt(
                            employerId = employerId,
                            reason = response.flags.firstOrNull() ?: "Policy violation"
                        )
                    }
                    
                    Timber.d("$TAG: Final check - Risk: ${response.riskScore}, Block: ${response.shouldBlock}")
                },
                onFailure = { error ->
                    Timber.e("$TAG: Final check failed: ${error.message}")
                }
            )
            
            result
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Final check error")
            Result.failure(e)
        }
    }
    
    // ============================================================
    // 3-STRIKE BLOCKING SYSTEM
    // ============================================================
    
    /**
     * Get employer's blocked attempts count
     */
    suspend fun getBlockedAttempts(employerId: String): EmployerBlockedAttempts {
        return try {
            val doc = firestore.collection(BLOCKED_ATTEMPTS_COLLECTION)
                .document(employerId)
                .get()
                .await()
            
            if (doc.exists()) {
                EmployerBlockedAttempts(
                    employerId = employerId,
                    blockedCount = doc.getLong("blocked_count")?.toInt() ?: 0,
                    lastBlockedAt = doc.getLong("last_blocked_at"),
                    blockReasons = (doc.get("block_reasons") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    isSuspended = doc.getBoolean("is_suspended") ?: false,
                    suspendedAt = doc.getLong("suspended_at")
                )
            } else {
                EmployerBlockedAttempts(employerId = employerId)
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error getting blocked attempts")
            EmployerBlockedAttempts(employerId = employerId)
        }
    }
    
    /**
     * Record a blocked attempt
     * After 3 attempts, employer is auto-suspended
     */
    suspend fun recordBlockedAttempt(employerId: String, reason: String) {
        try {
            val current = getBlockedAttempts(employerId)
            val newCount = current.blockedCount + 1
            val newReasons = current.blockReasons + reason
            
            val data = hashMapOf(
                "employer_id" to employerId,
                "blocked_count" to newCount,
                "last_blocked_at" to System.currentTimeMillis(),
                "block_reasons" to newReasons.takeLast(10), // Keep last 10 reasons
                "is_suspended" to (newCount >= MAX_BLOCKED_ATTEMPTS),
                "suspended_at" to if (newCount >= MAX_BLOCKED_ATTEMPTS) System.currentTimeMillis() else null
            )
            
            firestore.collection(BLOCKED_ATTEMPTS_COLLECTION)
                .document(employerId)
                .set(data)
                .await()
            
            Timber.w("$TAG: Recorded blocked attempt for $employerId (count: $newCount)")
            
            if (newCount >= MAX_BLOCKED_ATTEMPTS) {
                Timber.e("$TAG: ⛔ EMPLOYER $employerId AUTO-SUSPENDED after $newCount blocked attempts")
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error recording blocked attempt")
        }
    }
    
    /**
     * Suspend employer account
     */
    private suspend fun suspendEmployer(employerId: String, reason: String) {
        try {
            firestore.collection(BLOCKED_ATTEMPTS_COLLECTION)
                .document(employerId)
                .update(
                    mapOf(
                        "is_suspended" to true,
                        "suspended_at" to System.currentTimeMillis(),
                        "suspension_reason" to reason
                    )
                )
                .await()
            
            // Also update user's suspension status
            firestore.collection("users")
                .document(employerId)
                .update(
                    mapOf(
                        "is_suspended" to true,
                        "suspended_at" to System.currentTimeMillis(),
                        "suspension_reason" to reason
                    )
                )
                .await()
            
            Timber.e("$TAG: ⛔ Employer $employerId SUSPENDED: $reason")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Error suspending employer")
        }
    }
    
    // ============================================================
    // UTILITY
    // ============================================================
    
    /**
     * Reset validation state
     * Call when starting new job posting
     */
    fun resetValidation() {
        _validationState.value = JobPostingValidationState()
    }
    
    /**
     * Check if current validation allows posting
     */
    fun canPost(): Boolean {
        val state = _validationState.value
        return state.isValid && 
               !state.shouldBlock && 
               state.titleCheck.isValid && 
               state.descriptionCheck.isValid
    }
    
}