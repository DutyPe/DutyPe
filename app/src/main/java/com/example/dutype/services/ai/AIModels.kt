package com.example.dutype.services.ai

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * AI Backend Data Models
 * 
 * Request/Response models for AI Backend API
 * Matches Python FastAPI backend schemas
 */
// ============================================================
// COMMON MODELS
// ============================================================
@Keep
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String
)
// ============================================================
// JOB ANALYSIS MODELS
// ============================================================
@Keep
data class JobAnalysisRequest(
    @SerializedName("job_id") val jobId: String,
    val title: String,
    val description: String,
    val category: String,
    @SerializedName("pay_amount") val payAmount: Double,
    @SerializedName("pay_type") val payType: String, // DAILY, HOURLY, MONTHLY, FIXED
    val location: String,
    val vacancies: Int = 1,
    @SerializedName("employer_id") val employerId: String,
    @SerializedName("employer_tier") val employerTier: String = "VERIFIED",
    @SerializedName("employer_history") val employerHistory: EmployerHistory? = null
)

@Keep
data class EmployerHistory(
    @SerializedName("total_jobs_posted") val totalJobsPosted: Int = 0,
    @SerializedName("jobs_completed") val jobsCompleted: Int = 0,
    @SerializedName("jobs_cancelled") val jobsCancelled: Int = 0,
    @SerializedName("average_rating") val averageRating: Double = 0.0,
    @SerializedName("total_reports") val totalReports: Int = 0,
    @SerializedName("account_age_days") val accountAgeDays: Int = 0
)

@Keep
data class JobAnalysisResponse(
    @SerializedName("job_id") val jobId: String,
    @SerializedName("risk_score") val riskScore: Int, // 0-100
    @SerializedName("risk_level") val riskLevel: String, // LOW, MEDIUM, HIGH, CRITICAL
    @SerializedName("should_block") val shouldBlock: Boolean,
    @SerializedName("should_review") val shouldReview: Boolean,
    val flags: List<String>,
    val reasoning: String,
    @SerializedName("banned_keywords_found") val bannedKeywordsFound: List<String>,
    @SerializedName("suspicious_keywords_found") val suspiciousKeywordsFound: List<String>,
    @SerializedName("rule_violations") val ruleViolations: List<String>
)

@Keep
data class QuickCheckResponse(
    @SerializedName("has_banned_keywords") val hasBannedKeywords: Boolean,
    @SerializedName("banned_keywords") val bannedKeywords: List<String>,
    @SerializedName("suspicious_keywords") val suspiciousKeywords: List<String>,
    @SerializedName("should_block") val shouldBlock: Boolean,
    @SerializedName("risk_indicators") val riskIndicators: List<String> = emptyList()
)

// ============================================================
// FRAUD DETECTION MODELS
// ============================================================

@Keep
data class FraudDetectionRequest(
    val text: String,
    @SerializedName("context_type") val contextType: String = "JOB_POSTING", // JOB_POSTING, MESSAGE, PROFILE
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("additional_context") val additionalContext: Map<String, Any>? = null
)

@Keep
data class FraudDetectionResponse(
    @SerializedName("is_fraud") val isFraud: Boolean,
    @SerializedName("fraud_probability") val fraudProbability: Int, // 0-100
    @SerializedName("should_block") val shouldBlock: Boolean,
    @SerializedName("scam_types") val scamTypes: List<String>,
    @SerializedName("banned_keywords") val bannedKeywords: List<String>,
    @SerializedName("suspicious_keywords") val suspiciousKeywords: List<String>,
    val reasoning: String
)

@Keep
data class KeywordCheckResponse(
    @SerializedName("has_banned_keywords") val hasBannedKeywords: Boolean,
    @SerializedName("banned_keywords") val bannedKeywords: List<String>,
    @SerializedName("suspicious_keywords") val suspiciousKeywords: List<String>,
    @SerializedName("should_block") val shouldBlock: Boolean
)

// ============================================================
// EMPLOYER SCORING MODELS
// ============================================================

@Keep
data class EmployerScoreRequest(
    @SerializedName("employer_id") val employerId: String,
    val metrics: EmployerMetrics
)

@Keep
data class EmployerMetrics(
    @SerializedName("response_rate") val responseRate: Double = 100.0, // percentage
    @SerializedName("avg_response_time_minutes") val avgResponseTimeMinutes: Double = 30.0,
    @SerializedName("jobs_posted_30d") val jobsPosted30d: Int = 0,
    @SerializedName("jobs_completed_30d") val jobsCompleted30d: Int = 0,
    @SerializedName("jobs_cancelled_30d") val jobsCancelled30d: Int = 0,
    @SerializedName("sla_misses_30d") val slaMisses30d: Int = 0,
    @SerializedName("worker_complaints") val workerComplaints: Int = 0,
    @SerializedName("instant_job_abuse") val instantJobAbuse: Boolean = false,
    @SerializedName("account_age_days") val accountAgeDays: Int = 0,
    @SerializedName("total_workers_hired") val totalWorkersHired: Int = 0,
    @SerializedName("average_rating") val averageRating: Double = 0.0
)

@Keep
data class EmployerScoreResponse(
    @SerializedName("employer_id") val employerId: String,
    @SerializedName("reliability_score") val reliabilityScore: Int, // 0-100
    @SerializedName("risk_level") val riskLevel: String, // LOW, MEDIUM, HIGH
    @SerializedName("current_tier") val currentTier: String, // VERIFIED, TRUSTED, BUSINESS, RESTRICTED
    @SerializedName("can_post_instant_jobs") val canPostInstantJobs: Boolean,
    @SerializedName("requires_deposit") val requiresDeposit: Boolean,
    @SerializedName("posting_limit") val postingLimit: Int,
    @SerializedName("behavior_flags") val behaviorFlags: List<String>,
    val recommendations: List<String>
)

@Keep
data class EmployerPrivilegesResponse(
    @SerializedName("employer_id") val employerId: String,
    @SerializedName("can_post_instant_jobs") val canPostInstantJobs: Boolean,
    @SerializedName("can_post_rush_jobs") val canPostRushJobs: Boolean,
    @SerializedName("posting_limit_per_day") val postingLimitPerDay: Int,
    @SerializedName("requires_deposit") val requiresDeposit: Boolean,
    val message: String? = null
)

// ============================================================
// RULES ENGINE MODELS
// ============================================================

@Keep
data class RuleValidationResponse(
    @SerializedName("is_valid") val isValid: Boolean,
    val violations: List<String>,
    @SerializedName("should_block") val shouldBlock: Boolean,
    @SerializedName("banned_keywords") val bannedKeywords: List<String>,
    @SerializedName("suspicious_keywords") val suspiciousKeywords: List<String>
)

@Keep
data class PayValidationResponse(
    @SerializedName("is_valid") val isValid: Boolean,
    val reason: String,
    val category: String,
    @SerializedName("pay_amount") val payAmount: Double,
    @SerializedName("pay_type") val payType: String
)

@Keep
data class BannedKeywordsResponse(
    @SerializedName("banned_keywords") val bannedKeywords: List<String>,
    @SerializedName("suspicious_keywords") val suspiciousKeywords: List<String>
)
