package com.example.dutype.services

import com.example.dutype.firestore.FirestoreSchema.JobReports
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.Timestamp
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
    NON_PAYMENT("Not Paid", "I did the work but the employer has not paid me"),
    INAPPROPRIATE("Inappropriate", "Contains offensive or inappropriate content"),
    DUPLICATE("Duplicate", "Same job posted multiple times"),
    MISLEADING("Misleading Info", "Salary, location, or details are incorrect"),
    HARASSMENT("Harassment", "Employer is harassing applicants"),
    SPAM("Spam", "Promotional or spam content"),
    OTHER("Other", "Other issues not listed above")
}

data class ReportResult(
    val success: Boolean,
    val message: String,
    val totalReports: Int = 0,
    val jobHidden: Boolean = false
)

/**
 * Job reports: job_reports/{jobId}_{reporterId} (one per person per job). The server counts them and
 * closes a job at 3 reports; non-payment claims also notify the employer.
 */
@Singleton
class ReportingService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        const val MAX_CLAIM_AMOUNT = 1_000_000.0
    }

    suspend fun reportJob(jobId: String, reportType: ReportType, description: String = ""): Result<ReportResult> =
        submit(jobId, reportType, description.trim().ifBlank { reportType.description }, "Thank you for reporting. We'll review this job.")

    /** Non-payment after completed work; the amount and date go into the note for support. */
    suspend fun reportNonPayment(
        jobId: String,
        employerId: String,
        amountOwed: Double,
        workedOn: Timestamp,
        description: String = ""
    ): Result<ReportResult> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        if (employerId.isBlank() || employerId == uid) return Result.failure(Exception("Invalid employer for this report"))
        if (amountOwed <= 0.0 || amountOwed > MAX_CLAIM_AMOUNT) return Result.failure(Exception("Enter the amount you are owed"))
        if (workedOn.toDate().time > System.currentTimeMillis()) return Result.failure(Exception("Work date cannot be in the future"))
        val day = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).format(workedOn.toDate())
        val note = "Owed ₹${amountOwed.toLong()} for work on $day. ${description.trim()}".trim()
        return submit(jobId, ReportType.NON_PAYMENT, note, "Thank you. Our team will look into this payment.")
    }

    private suspend fun submit(jobId: String, type: ReportType, note: String, thanks: String): Result<ReportResult> = try {
        val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val ref = firestore.collection(JobReports.COLLECTION).document("${jobId}_$uid")
        if (ref.get().await().exists()) {
            Result.success(ReportResult(false, "You have already reported this job."))
        } else {
            ref.set(
                mapOf(
                    JobReports.JOB_ID to jobId,
                    JobReports.REPORTER_ID to uid,
                    JobReports.REASON to type.name,
                    JobReports.NOTE to note.take(500),
                    JobReports.STATUS to "open",
                    JobReports.CREATED_AT to Timestamp.now()
                )
            ).await()
            Result.success(ReportResult(true, thanks))
        }
    } catch (e: FirebaseFirestoreException) {
        Timber.e(e, "report failed")
        Result.failure(if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) Exception("Unable to submit the report right now.") else e)
    } catch (e: Exception) {
        Timber.e(e, "report failed")
        Result.failure(e)
    }
}
