package com.example.dutype.services.ai

import retrofit2.Response
import retrofit2.http.*

/**
 * AI Backend Service - Retrofit Interface
 * 
 * Connects Android app to Python FastAPI AI backend
 * All fraud detection, job analysis, and chatbot features
 * 
 * Backend URL: Configure in BuildConfig or local.properties
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */
interface AIBackendService {
    
    // ============================================================
    // HEALTH CHECK
    // ============================================================
    
    @GET("api/v1/health")
    suspend fun healthCheck(): Response<HealthResponse>
    
    // ============================================================
    // JOB ANALYSIS - Pre-posting fraud detection
    // ============================================================
    
    @POST("api/v1/jobs/analyze")
    suspend fun analyzeJob(
        @Body request: JobAnalysisRequest
    ): Response<JobAnalysisResponse>
    
    @POST("api/v1/jobs/quick-check")
    suspend fun quickCheckJob(
        @Query("title") title: String,
        @Query("description") description: String
    ): Response<QuickCheckResponse>
    
    // ============================================================
    // FRAUD DETECTION
    // ============================================================
    
    @POST("api/v1/fraud/detect")
    suspend fun detectFraud(
        @Body request: FraudDetectionRequest
    ): Response<FraudDetectionResponse>
    
    @POST("api/v1/fraud/check-keywords")
    suspend fun checkKeywords(
        @Query("text") text: String
    ): Response<KeywordCheckResponse>
    
    // ============================================================
    // EMPLOYER SCORING
    // ============================================================
    
    @POST("api/v1/employers/score")
    suspend fun scoreEmployer(
        @Body request: EmployerScoreRequest
    ): Response<EmployerScoreResponse>
    
    @GET("api/v1/employers/{employerId}/privileges")
    suspend fun getEmployerPrivileges(
        @Path("employerId") employerId: String
    ): Response<EmployerPrivilegesResponse>
    
    // ============================================================
    // RULES ENGINE - Deterministic validation
    // ============================================================
    
    @POST("api/v1/rules/validate-job")
    suspend fun validateJobRules(
        @Body jobData: Map<String, Any>,
        @Query("employer_tier") employerTier: String = "STANDARD"
    ): Response<RuleValidationResponse>
    
    @POST("api/v1/rules/validate-pay")
    suspend fun validatePayRate(
        @Query("category") category: String,
        @Query("pay_amount") payAmount: Double,
        @Query("pay_type") payType: String
    ): Response<PayValidationResponse>
    
    @GET("api/v1/rules/banned-keywords")
    suspend fun getBannedKeywords(): Response<BannedKeywordsResponse>
    
    // ============================================================
    // WORKER CHATBOT
    // ============================================================
    
    @POST("api/v1/chat/worker")
    suspend fun workerChat(
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    @POST("api/v1/chat/worker/job-safety")
    suspend fun checkJobSafety(
        @Body request: JobSafetyRequest
    ): Response<JobSafetyResponse>
    
    @POST("api/v1/chat/worker/application-status")
    suspend fun explainApplicationStatus(
        @Body request: ApplicationStatusRequest
    ): Response<ApplicationStatusResponse>
    
    // ============================================================
    // EMPLOYER CHATBOT
    // ============================================================
    
    @POST("api/v1/chat/employer")
    suspend fun employerChat(
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    @POST("api/v1/chat/employer/explain-score")
    suspend fun explainEmployerScore(
        @Body request: ScoreExplanationRequest
    ): Response<ScoreExplanationResponse>
    
    @POST("api/v1/chat/employer/explain-penalty")
    suspend fun explainPenalty(
        @Body request: PenaltyExplanationRequest
    ): Response<PenaltyExplanationResponse>
    
    @POST("api/v1/chat/employer/improvement-tips")
    suspend fun getImprovementTips(
        @Body request: ScoreExplanationRequest
    ): Response<ImprovementTipsResponse>
    
    // ============================================================
    // PROTECTION SERVICES
    // ============================================================
    
    // Silence Detection
    @POST("api/v1/protection/silence/track-view")
    suspend fun trackApplicationView(
        @Body request: ApplicationViewRequest
    ): Response<SilenceTrackResponse>
    
    @GET("api/v1/protection/silence/check/{applicationId}")
    suspend fun checkSilence(
        @Path("applicationId") applicationId: String
    ): Response<SilenceCheckResponse>
    
    // Duplicate Detection
    @POST("api/v1/protection/duplicate/check")
    suspend fun checkDuplicate(
        @Body request: DuplicateCheckRequest
    ): Response<DuplicateCheckResponse>
    
    // Multi-Account Detection
    @POST("api/v1/protection/multi-account/register")
    suspend fun registerEmployerDevice(
        @Body request: EmployerRegistrationRequest
    ): Response<MultiAccountResponse>
    
    @GET("api/v1/protection/multi-account/check/{employerId}")
    suspend fun checkMultiAccount(
        @Path("employerId") employerId: String
    ): Response<MultiAccountCheckResponse>
    
    // Worker Protection
    @POST("api/v1/protection/worker/assess-risk")
    suspend fun assessWorkerRisk(
        @Body request: WorkerRiskRequest
    ): Response<WorkerRiskResponse>
    
    @POST("api/v1/protection/worker/check-payment")
    suspend fun checkPaymentRequest(
        @Query("worker_id") workerId: String,
        @Query("employer_id") employerId: String,
        @Query("amount") amount: Double,
        @Query("reason") reason: String
    ): Response<PaymentCheckResponse>
    
    // Regional Patterns
    @POST("api/v1/protection/regional/detect")
    suspend fun detectRegionalScams(
        @Query("text") text: String
    ): Response<RegionalScamResponse>
    
    // Learning Service
    @POST("api/v1/protection/learning/report")
    suspend fun submitReport(
        @Body request: ReportRequest
    ): Response<ReportResponse>
    
    @POST("api/v1/protection/learning/confirm-scam")
    suspend fun confirmScam(
        @Query("job_id") jobId: String?,
        @Query("employer_id") employerId: String?,
        @Query("scam_type") scamType: String,
        @Query("details") details: String
    ): Response<ConfirmScamResponse>
}
