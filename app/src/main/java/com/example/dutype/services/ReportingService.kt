package com.example.dutype.services

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.firebase.Timestamp
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Job reporting via WhatsApp support.
 *
 * Reports are no longer stored in Firestore. The worker picks a reason in the report sheet and the
 * app opens a chat with the DutyPe WhatsApp support number, pre-filled with the job details and
 * the reason, so the support admin can review and act on it.
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
    val message: String
)

/** Builds the report message and opens it in a WhatsApp chat with DutyPe support. */
@Singleton
class ReportingService @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    companion object {
        const val MAX_CLAIM_AMOUNT = 1_000_000.0
        const val SUPPORT_WHATSAPP_NUMBER = "918500717800"
    }

    fun reportJob(
        jobId: String,
        jobTitle: String,
        companyName: String,
        reportType: ReportType,
        description: String = ""
    ): Result<ReportResult> {
        val message = buildString {
            appendLine("Job report - DutyPe")
            appendLine()
            appendLine("Reason: ${reportType.displayName}")
            appendLine("Job: $jobTitle")
            if (companyName.isNotBlank()) appendLine("Company: $companyName")
            appendLine("Job ID: $jobId")
            description.trim().takeIf { it.isNotEmpty() }?.let { appendLine("Details: ${it.take(500)}") }
        }
        return openSupportChat(message.trim(), "Send the message in WhatsApp so our support team can review this job.")
    }

    /** Non-payment after completed work; the amount and date go into the message for support. */
    fun reportNonPayment(
        jobId: String,
        jobTitle: String,
        companyName: String,
        amountOwed: Double,
        workedOn: Timestamp,
        description: String = ""
    ): Result<ReportResult> {
        if (amountOwed <= 0.0 || amountOwed > MAX_CLAIM_AMOUNT) return Result.failure(Exception("Enter the amount you are owed"))
        if (workedOn.toDate().time > System.currentTimeMillis()) return Result.failure(Exception("Work date cannot be in the future"))
        val day = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).format(workedOn.toDate())
        val message = buildString {
            appendLine("Payment not received - DutyPe")
            appendLine()
            appendLine("Job: $jobTitle")
            if (companyName.isNotBlank()) appendLine("Company: $companyName")
            appendLine("Job ID: $jobId")
            appendLine("Amount owed: ₹${amountOwed.toLong()}")
            appendLine("Worked on: $day")
            description.trim().takeIf { it.isNotEmpty() }?.let { appendLine("Details: ${it.take(500)}") }
        }
        return openSupportChat(message.trim(), "Send the message in WhatsApp so our team can look into this payment.")
    }

    private fun openSupportChat(message: String, thanks: String): Result<ReportResult> {
        val uri = Uri.parse("https://wa.me/$SUPPORT_WHATSAPP_NUMBER?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            try {
                appContext.startActivity(Intent(intent).setPackage("com.whatsapp"))
            } catch (_: ActivityNotFoundException) {
                // WhatsApp Business or no WhatsApp: let the system pick (browser opens wa.me).
                appContext.startActivity(intent)
            }
            Result.success(ReportResult(true, thanks))
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "no app to open WhatsApp support")
            Result.failure(Exception("WhatsApp is not available on this phone."))
        }
    }
}
