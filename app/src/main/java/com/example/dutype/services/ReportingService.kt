package com.example.dutype.services

import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.utils.SecureLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
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
    companion object {
        const val REPORTS_COLLECTION = "job_reports"
        const val JOBS_COLLECTION = FirestoreCollections.JOBS
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

            val reportRef = firestore.collection(REPORTS_COLLECTION)
                .document(buildReportId(userId, jobId))

            if (reportRef.get().await().exists()) {
                return Result.success(
                    ReportResult(
                        success = false,
                        message = "You have already reported this job.",
                        totalReports = 0,
                        jobHidden = false
                    )
                )
            }

            val reportData = mapOf(
                "reportId" to reportRef.id,
                "jobId" to jobId,
                "reporterId" to userId,
                "reporterPhone" to userPhone,
                "reportType" to reportType.name,
                "description" to description.trim(),
                "timestamp" to System.currentTimeMillis(),
                "status" to "PENDING"
            )

            reportRef.set(reportData).await()

            val totalReports = firestore.collection(REPORTS_COLLECTION)
                .whereEqualTo("jobId", jobId)
                .get()
                .await()
                .size()

            val thresholdReached = totalReports >= AUTO_HIDE_THRESHOLD
            
            // AUTO-HIDE: When threshold reached, mark job as closed in both collections
            if (thresholdReached) {
                try {
                    val batch = firestore.batch()
                    batch.update(
                        firestore.collection(JOBS_COLLECTION).document(jobId),
                        mapOf("status" to "closed")
                    )
                    batch.update(
                        firestore.collection(com.example.dutype.firestore.FirestoreCollections.JOB_DETAILS).document(jobId),
                        mapOf("status" to "closed")
                    )
                    batch.commit().await()
                    Timber.w("🚫 Job $jobId auto-hidden: reached $totalReports reports (threshold=$AUTO_HIDE_THRESHOLD)")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to auto-hide reported job $jobId")
                }
            }
            
            val responseMessage = if (thresholdReached) {
                "Report submitted. This job has been hidden due to multiple reports."
            } else {
                "Thank you for reporting. We'll review this job."
            }

            Result.success(
                ReportResult(
                    success = true,
                    message = responseMessage,
                    totalReports = totalReports,
                    jobHidden = thresholdReached
                )
            )
            
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Timber.e(e, "Failed to report job: permission denied")
                return Result.failure(Exception("Unable to submit report right now. Please update app/rules and try again."))
            }
            Timber.e(e, "Failed to report job")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to report job")
            Result.failure(e)
        }
    }
    
    /**
     * Check if user already reported this job
     */
    private suspend fun checkExistingReport(jobId: String, userId: String): Boolean {
        val reportId = buildReportId(userId, jobId)
        return firestore.collection(REPORTS_COLLECTION)
            .document(reportId)
            .get()
            .await()
            .exists()
    }
    
    /**
     * Get user's reports
     */
    suspend fun getUserReports(userId: String): List<JobReport> {
        return try {
            firestore.collection(REPORTS_COLLECTION)
                .whereEqualTo("reporterId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    JobReport(
                        reportId = data["reportId"] as? String ?: doc.id,
                        jobId = data["jobId"] as? String ?: "",
                        reporterId = data["reporterId"] as? String ?: "",
                        reporterPhone = data["reporterPhone"] as? String ?: "",
                        reportType = data["reportType"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                        status = data["status"] as? String ?: "PENDING",
                        reviewedBy = data["reviewedBy"] as? String,
                        reviewedAt = (data["reviewedAt"] as? Number)?.toLong(),
                        actionTaken = data["actionTaken"] as? String
                    )
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get user reports")
            emptyList()
        }
    }

    private fun buildReportId(userId: String, jobId: String): String = "${userId}_${jobId}"
}
