package com.example.dutype.services.ai

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * AI Protection & Fraud Detection Models
 */

// ============================================================
// PROTECTION MODELS
// ============================================================

@Keep
data class ApplicationViewRequest(
    @SerializedName("application_id") val applicationId: String,
    @SerializedName("employer_id") val employerId: String,
    @SerializedName("worker_id") val workerId: String
)

@Keep
data class SilenceTrackResponse(
    val tracked: Boolean,
    @SerializedName("application_id") val applicationId: String,
    @SerializedName("viewed_at") val viewedAt: Long
)

@Keep
data class SilenceCheckResponse(
    @SerializedName("is_silent") val isSilent: Boolean,
    @SerializedName("hours_since_view") val hoursSinceView: Double?,
    @SerializedName("penalty_applied") val penaltyApplied: Boolean
)

@Keep
data class DuplicateCheckRequest(
    @SerializedName("job_id") val jobId: String,
    val title: String,
    val description: String,
    @SerializedName("employer_id") val employerId: String
)

@Keep
data class DuplicateCheckResponse(
    @SerializedName("is_duplicate") val isDuplicate: Boolean,
    @SerializedName("similarity_score") val similarityScore: Double,
    @SerializedName("similar_job_id") val similarJobId: String?,
    @SerializedName("should_block") val shouldBlock: Boolean
)

@Keep
data class EmployerRegistrationRequest(
    @SerializedName("employer_id") val employerId: String,
    val phone: String? = null,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("ip_address") val ipAddress: String? = null,
    val email: String? = null
)

@Keep
data class MultiAccountResponse(
    val registered: Boolean,
    @SerializedName("is_suspicious") val isSuspicious: Boolean,
    @SerializedName("linked_accounts") val linkedAccounts: List<String>
)

@Keep
data class MultiAccountCheckResponse(
    @SerializedName("has_multi_accounts") val hasMultiAccounts: Boolean,
    @SerializedName("linked_count") val linkedCount: Int,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("should_restrict") val shouldRestrict: Boolean
)

@Keep
data class WorkerRiskRequest(
    @SerializedName("worker_id") val workerId: String,
    val job: Map<String, Any>,
    val employer: Map<String, Any>,
    val worker: Map<String, Any>? = null
)

@Keep
data class WorkerRiskResponse(
    @SerializedName("risk_level") val riskLevel: String, // LOW, MEDIUM, HIGH, CRITICAL
    @SerializedName("risk_score") val riskScore: Int,
    val warnings: List<String>,
    @SerializedName("should_warn") val shouldWarn: Boolean,
    @SerializedName("should_block") val shouldBlock: Boolean,
    val explanation: String
)

@Keep
data class PaymentCheckResponse(
    @SerializedName("is_suspicious") val isSuspicious: Boolean,
    @SerializedName("should_block") val shouldBlock: Boolean,
    val warning: String,
    val reason: String
)

@Keep
data class RegionalScamResponse(
    @SerializedName("has_regional_patterns") val hasRegionalPatterns: Boolean,
    val patterns: List<RegionalPattern>,
    @SerializedName("risk_score") val riskScore: Int
)

@Keep
data class RegionalPattern(
    val language: String,
    val pattern: String,
    @SerializedName("scam_type") val scamType: String
)

@Keep
data class ReportRequest(
    @SerializedName("reporter_id") val reporterId: String,
    @SerializedName("reporter_type") val reporterType: String, // WORKER, EMPLOYER
    @SerializedName("target_type") val targetType: String, // JOB, EMPLOYER, WORKER
    @SerializedName("target_id") val targetId: String,
    val reason: String,
    val description: String,
    val evidence: Map<String, Any>? = null
)

@Keep
data class ReportResponse(
    val success: Boolean,
    @SerializedName("report_id") val reportId: String,
    val message: String
)

@Keep
data class ConfirmScamResponse(
    val confirmed: Boolean,
    @SerializedName("patterns_learned") val patternsLearned: Int
)

// ============================================================
// JOB POSTING SCREENING STATE
// ============================================================

/**
 * Tracks employer's blocked attempts for 3-strike system
 */
@Keep
data class EmployerBlockedAttempts(
    @SerializedName("employer_id") val employerId: String,
    @SerializedName("blocked_count") val blockedCount: Int = 0,
    @SerializedName("last_blocked_at") val lastBlockedAt: Long? = null,
    @SerializedName("block_reasons") val blockReasons: List<String> = emptyList(),
    @SerializedName("is_suspended") val isSuspended: Boolean = false,
    @SerializedName("suspended_at") val suspendedAt: Long? = null
)

/**
 * Real-time job posting validation state
 */
@Keep
data class JobPostingValidationState(
    @SerializedName("is_valid") val isValid: Boolean = true,
    @SerializedName("title_check") val titleCheck: FieldValidation = FieldValidation(),
    @SerializedName("description_check") val descriptionCheck: FieldValidation = FieldValidation(),
    @SerializedName("salary_check") val salaryCheck: FieldValidation = FieldValidation(),
    @SerializedName("vacancy_check") val vacancyCheck: FieldValidation = FieldValidation(),
    @SerializedName("overall_risk_score") val overallRiskScore: Int = 0,
    @SerializedName("should_block") val shouldBlock: Boolean = false,
    @SerializedName("block_reason") val blockReason: String? = null
)

@Keep
data class FieldValidation(
    @SerializedName("is_valid") val isValid: Boolean = true,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    @SerializedName("risk_score") val riskScore: Int = 0
)
