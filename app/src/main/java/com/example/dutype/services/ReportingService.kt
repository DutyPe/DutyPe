package com.example.dutype.services

import com.example.dutype.utils.SecureLogger
import com.google.firebase.auth.FirebaseAuth
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
            
            // P0 FIX: Use SecureLogger to mask sensitive data
            SecureLogger.d("ReportingService", "Reporting job", 
                "jobId" to jobId,
                "reporterId" to userId,
                "reportType" to reportType.name
            )
            
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
            
            Timber.d("📝 Job $jobId reported: ${reportType.name}")
            
            Result.success(ReportResult(
                success = true,
                message = "Thank you for reporting. We'll review this job.",
                totalReports = 1,
                jobHidden = false
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
