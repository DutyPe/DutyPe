package com.example.dutype.repositories

import com.example.dutype.services.ai.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    
    // ============================================================
    // HEALTH CHECK
    // ============================================================
    
    suspend fun checkHealth(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = aiBackendService.healthCheck()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Health check failed: ${response.code()}"))
            }
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
                
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
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
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Quick check failed: ${response.code()}"))
                }
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
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Keyword check failed: ${response.code()}"))
                }
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
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Fraud detection failed: ${response.code()}"))
                }
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
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Employer scoring failed: ${response.code()}"))
                }
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
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Get privileges failed: ${response.code()}"))
                }
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
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Rule validation failed: ${response.code()}"))
            }
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
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Pay validation failed: ${response.code()}"))
            }
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
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Track view failed: ${response.code()}"))
            }
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
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Duplicate check failed: ${response.code()}"))
            }
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
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Risk assessment failed: ${response.code()}"))
            }
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
                
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Submit report failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Submit report error")
                Result.failure(e)
            }
        }
}