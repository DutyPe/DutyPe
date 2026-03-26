package com.example.dutype.repositories

import com.example.dutype.services.ai.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Backend Repository
 * 
 * Repository pattern for AI Backend services
 * Handles all communication with Python FastAPI backend
 * 
 * Key Features:
 * - Job analysis and fraud detection
 * - Real-time keyword checking
 * - Employer scoring
 * - 3-strike blocking system
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
@Singleton
class AIBackendRepository @Inject constructor(
    private val aiBackendService: AIBackendService
) {
    
    companion object {
        private const val TAG = "AIBackendRepo"
        const val MAX_BLOCKED_ATTEMPTS = 3 // 3 strikes = suspension
    }

    private fun <T> bodyOrFailure(response: Response<T>, operation: String): Result<T> {
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            Result.success(body)
        } else {
            Result.failure(Exception("$operation failed: ${response.code()}"))
        }
    }
    
    // ============================================================
    // HEALTH CHECK
    // ============================================================
    
    suspend fun checkHealth(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiBackendService.healthCheck()
            bodyOrFailure(response, "Health check")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Health check error")
            Result.failure(e)
        }
    }
    
    // ============================================================
    // JOB ANALYSIS - Pre-posting fraud detection
    // ============================================================
    
    /**
     * Full AI analysis of job posting
     * Use before saving job to Firestore
     * 
     * @return JobAnalysisResponse with risk score and blocking decision
     */
    suspend fun analyzeJob(request: JobAnalysisRequest): Result<JobAnalysisResponse> = 
        withContext(Dispatchers.IO) {
            try {
                Timber.d("$TAG: Analyzing job ${request.jobId}")
                val response = aiBackendService.analyzeJob(request)

                val result = response.body()
                if (response.isSuccessful && result != null) {
                    Timber.d("$TAG: Job analysis complete - Risk: ${result.riskScore}, Block: ${result.shouldBlock}")
                    Result.success(result)
                } else {
                    val error = "Job analysis failed: ${response.code()} - ${response.errorBody()?.string()}"
                    Timber.w("$TAG: $error")
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Job analysis error")
                Result.failure(e)
            }
        }
    
    /**
     * Quick keyword check (deterministic, no AI)
     * Use for real-time feedback while typing
     * 
     * @return QuickCheckResponse with banned/suspicious keywords
     */
    suspend fun quickCheckJob(title: String, description: String): Result<QuickCheckResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = aiBackendService.quickCheckJob(title, description)
                bodyOrFailure(response, "Quick check")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Quick check error")
                Result.failure(e)
            }
        }
    
    /**
     * Check text for banned keywords only
     * Fastest check - use for instant feedback
     */
    suspend fun checkKeywords(text: String): Result<KeywordCheckResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = aiBackendService.checkKeywords(text)
                bodyOrFailure(response, "Keyword check")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Keyword check error")
                Result.failure(e)
            }
        }
    
    // ============================================================
    // FRAUD DETECTION
    // ============================================================
    
    /**
     * Detect fraud patterns in text
     * Use for messages, profiles, or any user-generated content
     */
    suspend fun detectFraud(request: FraudDetectionRequest): Result<FraudDetectionResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = aiBackendService.detectFraud(request)
                bodyOrFailure(response, "Fraud detection")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fraud detection error")
                Result.failure(e)
            }
        }
    
    // ============================================================
    // EMPLOYER SCORING
    // ============================================================
    
    /**
     * Get employer reliability score
     * Determines posting privileges and restrictions
     */
    suspend fun scoreEmployer(request: EmployerScoreRequest): Result<EmployerScoreResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = aiBackendService.scoreEmployer(request)
                bodyOrFailure(response, "Employer scoring")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Employer scoring error")
                Result.failure(e)
            }
        }
    
    /**
     * Get employer privileges (quick lookup)
     */
    suspend fun getEmployerPrivileges(employerId: String): Result<EmployerPrivilegesResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = aiBackendService.getEmployerPrivileges(employerId)
                bodyOrFailure(response, "Get privileges")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Get privileges error")
                Result.failure(e)
            }
        }
    
    // ============================================================
    // RULES ENGINE
    // ============================================================
    
    /**
     * Validate job against deterministic rules
     * No AI - pure rule-based validation
     */
    suspend fun validateJobRules(
        jobData: Map<String, Any>,
        employerTier: String = "VERIFIED"
    ): Result<RuleValidationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiBackendService.validateJobRules(jobData, employerTier)
            bodyOrFailure(response, "Rule validation")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Rule validation error")
            Result.failure(e)
        }
    }
    
    /**
     * Validate pay rate for category
     */
    suspend fun validatePayRate(
        category: String,
        payAmount: Double,
        payType: String
    ): Result<PayValidationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiBackendService.validatePayRate(category, payAmount, payType)
            bodyOrFailure(response, "Pay validation")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Pay validation error")
            Result.failure(e)
        }
    }
    
    // ============================================================
    // PROTECTION SERVICES
    // ============================================================
    
    /**
     * Track when employer views an application (for silence detection)
     */
    suspend fun trackApplicationView(
        applicationId: String,
        employerId: String,
        workerId: String
    ): Result<SilenceTrackResponse> = withContext(Dispatchers.IO) {
        try {
            val request = ApplicationViewRequest(applicationId, employerId, workerId)
            val response = aiBackendService.trackApplicationView(request)
            bodyOrFailure(response, "Track view")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Track view error")
            Result.failure(e)
        }
    }
    
    /**
     * Check for duplicate job posting
     */
    suspend fun checkDuplicate(
        jobId: String,
        title: String,
        description: String,
        employerId: String
    ): Result<DuplicateCheckResponse> = withContext(Dispatchers.IO) {
        try {
            val request = DuplicateCheckRequest(jobId, title, description, employerId)
            val response = aiBackendService.checkDuplicate(request)
            bodyOrFailure(response, "Duplicate check")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Duplicate check error")
            Result.failure(e)
        }
    }
    
    /**
     * Assess risk for worker applying to job
     */
    suspend fun assessWorkerRisk(
        workerId: String,
        job: Map<String, Any>,
        employer: Map<String, Any>,
        worker: Map<String, Any>? = null
    ): Result<WorkerRiskResponse> = withContext(Dispatchers.IO) {
        try {
            val request = WorkerRiskRequest(workerId, job, employer, worker)
            val response = aiBackendService.assessWorkerRisk(request)
            bodyOrFailure(response, "Risk assessment")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Risk assessment error")
            Result.failure(e)
        }
    }
    
    /**
     * Submit report for learning
     */
    suspend fun submitReport(request: ReportRequest): Result<ReportResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = aiBackendService.submitReport(request)

                bodyOrFailure(response, "Submit report")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Submit report error")
                Result.failure(e)
            }
        }
}