package com.example.dutype.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Community Reporting Service
 * 
 * P1 Feature: 3 reports = auto-hide job
 * Crowd-sourced moderation for clean platform
 * 
 * REFACTORED: Now receives Firebase dependencies via constructor injection
 * 
 * Report Types:
 * - SCAM: Fraudulent job posting
 * - FAKE: Non-existent job/company
 * - INAPPROPRIATE: Offensive content
 * - DUPLICATE: Same job posted multiple times
 * - MISLEADING: Incorrect salary/location/details
 * - HARASSMENT: Employer harassment
 * - OTHER: Other issues
 * 
 * @author DutyPe Engineering Team
 * @since 2.0.0
 */

enum class ReportType(val displayName: String, val description: String) {
    SCAM("Scam/Fraud", "This job is asking for money or seems fraudulent"),
    FAKE("Fake Job", "This job or company doesn't exist"),
    INAPPROPRIATE("Inappropriate", "Contains offensive or inappropriate content"),
    DUPLICATE("Duplicate", "Same job posted multiple times"),
    MISLEADING("Misleading Info", "Salary, location, or details are incorrect"),
    HARASSMENT("Harassment", "Employer is harassing applicants"),
    SPAM("Spam", "Promotional or spam content"),
    OTHER("Other", "Other issues not listed above")
}

data class JobReport(
    val reportId: String = "",
    val jobId: String = "",
    val reporterId: String = "",
    val reporterPhone: String = "",
    val reportType: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, REVIEWED, RESOLVED, DISMISSED
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val actionTaken: String? = null
)

data class ReportResult(
    val success: Boolean,
    val message: String,
    val totalReports: Int = 0,
    val jobHidden: Boolean = false
)

@Singleton
class ReportingService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val functions = FirebaseFunctions.getInstance()
    
    companion object {
        const val REPORTS_COLLECTION = "job_reports"
        const val JOBS_COLLECTION = "jobs"
        const val AUTO_HIDE_THRESHOLD = 3 // 3 reports = auto-hide
        const val REPORT_COOLDOWN_HOURS = 24 // Can't report same job twice in 24 hours
    }
    
    /**
     * Report a job posting
     * Returns result with total reports and whether job was auto-hidden
     */
    suspend fun reportJob(
        jobId: String,
        reportType: ReportType,
        description: String = ""
    ): Result<ReportResult> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            
            val userId = currentUser.uid
            val userPhone = currentUser.phoneNumber ?: ""
            
            // Check if user already reported this job recently
            val existingReport = checkExistingReport(jobId, userId)
            if (existingReport) {
                return Result.success(ReportResult(
                    success = false,
                    message = "You have already reported this job"
                ))
            }
            
            // Create report document
            val reportRef = firestore.collection(REPORTS_COLLECTION).document()
            val report = JobReport(
                reportId = reportRef.id,
                jobId = jobId,
                reporterId = userId,
                reporterPhone = userPhone,
                reportType = reportType.name,
                description = description,
                timestamp = System.currentTimeMillis()
            )
            
            // Save report
            reportRef.set(report).await()
            
            // Update job's report count
            val jobRef = firestore.collection(JOBS_COLLECTION).document(jobId)
            jobRef.update(
                mapOf(
                    "reportCount" to FieldValue.increment(1),
                    "lastReportedAt" to System.currentTimeMillis(),
                    "reportTypes" to FieldValue.arrayUnion(reportType.name)
                )
            ).await()
            
            // Get updated report count
            val jobDoc = jobRef.get().await()
            val reportCount = jobDoc.getLong("reportCount")?.toInt() ?: 1
            
            // Auto-hide if threshold reached
            var jobHidden = false
            if (reportCount >= AUTO_HIDE_THRESHOLD) {
                jobRef.update(
                    mapOf(
                        "isHidden" to true,
                        "hiddenReason" to "AUTO_HIDDEN_COMMUNITY_REPORTS",
                        "hiddenAt" to System.currentTimeMillis()
                    )
                ).await()
                jobHidden = true
                Timber.w("🚨 Job $jobId auto-hidden after $reportCount reports")
            }
            
            // Log to fraud signals
            logFraudSignal(jobId, reportType, reportCount)
            
            Timber.d("📝 Job $jobId reported: ${reportType.name} (total: $reportCount)")
            
            Result.success(ReportResult(
                success = true,
                message = if (jobHidden) 
                    "Thank you! This job has been hidden for review." 
                else 
                    "Thank you for reporting. We'll review this job.",
                totalReports = reportCount,
                jobHidden = jobHidden
            ))
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to report job")
            Result.failure(e)
        }
    }
    
    /**
     * Check if user already reported this job
     */
    private suspend fun checkExistingReport(jobId: String, userId: String): Boolean {
        return try {
            val cutoffTime = System.currentTimeMillis() - (REPORT_COOLDOWN_HOURS * 60 * 60 * 1000)
            
            val existingReports = firestore.collection(REPORTS_COLLECTION)
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("reporterId", userId)
                .whereGreaterThan("timestamp", cutoffTime)
                .get()
                .await()
            
            existingReports.documents.isNotEmpty()
        } catch (e: Exception) {
            Timber.e(e, "Error checking existing report")
            false
        }
    }
    
    /**
     * Log fraud signal for analytics
     */
    private suspend fun logFraudSignal(jobId: String, reportType: ReportType, totalReports: Int) {
        try {
            val signalRef = firestore.collection("fraud_signals").document()
            signalRef.set(mapOf(
                "signalId" to signalRef.id,
                "jobId" to jobId,
                "signalType" to "COMMUNITY_REPORT",
                "severity" to when {
                    totalReports >= AUTO_HIDE_THRESHOLD -> "HIGH"
                    totalReports >= 2 -> "MEDIUM"
                    else -> "LOW"
                },
                "details" to mapOf(
                    "reportType" to reportType.name,
                    "totalReports" to totalReports,
                    "autoHidden" to (totalReports >= AUTO_HIDE_THRESHOLD)
                ),
                "timestamp" to System.currentTimeMillis(),
                "resolved" to false
            )).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to log fraud signal")
        }
    }
    
    /**
     * Get report count for a job
     */
    suspend fun getJobReportCount(jobId: String): Int {
        return try {
            val jobDoc = firestore.collection(JOBS_COLLECTION).document(jobId).get().await()
            jobDoc.getLong("reportCount")?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Check if job is hidden due to reports
     */
    suspend fun isJobHidden(jobId: String): Boolean {
        return try {
            val jobDoc = firestore.collection(JOBS_COLLECTION).document(jobId).get().await()
            jobDoc.getBoolean("isHidden") ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get user's reports
     */
    suspend fun getUserReports(userId: String): List<JobReport> {
        return try {
            val reports = firestore.collection(REPORTS_COLLECTION)
                .whereEqualTo("reporterId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            reports.documents.mapNotNull { doc ->
                doc.toObject(JobReport::class.java)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user reports")
            emptyList()
        }
    }
}
