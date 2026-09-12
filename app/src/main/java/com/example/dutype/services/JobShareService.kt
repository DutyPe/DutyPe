package com.example.dutype.services

import com.dutype.app.R
import android.content.Context
import android.content.Intent
import com.example.dutype.models.JobListing
import com.example.dutype.utils.DeepLinkHandler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Job Share Service — uses only schema fields.
 * salary (Double) + salaryType (String) from jobs collection.
 * addressText (String) from job_details (runtime only).
 */
@Singleton
class JobShareService @Inject constructor() {

    private fun formatPay(job: JobListing): String =
        "₹" + com.example.dutype.utils.SalaryFormatter.display(job.salary, job.salaryType)

    fun shareJob(context: Context, job: JobListing) {
        try {
            val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
            val shareText = buildString {
                append("🚨 *URGENT HIRING NEARBY!* 🚨\n\n")
                append("💼 *Role:* ${job.title}\n")
                if (job.companyName.isNotBlank()) append("🏢 *Company:* ${job.companyName}\n")
                append("💰 *Salary:* ${formatPay(job)}\n")
                if (job.addressText.isNotEmpty()) append("📍 *Location:* ${job.addressText}\n")
                append("✅ *No Resume needed* (Direct Call / 1-Tap Apply)\n\n")
                append("👉 *Apply / Call Employer Now:* $jobLink\n\n")
                append("📲 _Download DutyPe for 100% free local jobs & instant daily work_")
            }
            context.startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }, "Share Job"
            ))
            Timber.d("📤 Job shared: ${job.id}")
        } catch (e: Exception) {
            Timber.e(e, "📤 Error sharing job")
        }
    }

    fun shareJobToWhatsApp(context: Context, job: JobListing) {
        try {
            val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
            val shareText = buildString {
                append("🚨 *URGENT HIRING NEARBY!* 🚨\n\n")
                append("💼 *Role:* ${job.title}\n")
                if (job.companyName.isNotBlank()) append("🏢 *Company:* ${job.companyName}\n")
                append("💰 *Salary:* ${formatPay(job)}\n")
                if (job.addressText.isNotEmpty()) append("📍 *Location:* ${job.addressText}\n")
                append("✅ *No Resume needed* (Direct Call / 1-Tap Apply)\n\n")
                append("👉 *Apply / Call Employer Now:* $jobLink\n\n")
                append("📲 _Download DutyPe for 100% free local jobs & instant daily work_")
            }
            try {
                context.startActivity(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                })
                Timber.d("📤 Job shared to WhatsApp: ${job.id}")
            } catch (e: Exception) {
                shareJob(context, job)
            }
        } catch (e: Exception) {
            Timber.e(e, "📤 Error sharing job to WhatsApp")
        }
    }

    fun copyJobLink(context: Context, job: JobListing): String {
        val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(context.getString(R.string.job_link_clip_label), jobLink))
        Timber.d("📋 Job link copied: $jobLink")
        return jobLink
    }

    fun generateShareMessage(job: JobListing): String {
        val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
        return buildString {
            append("🚨 *URGENT HIRING NEARBY!* 🚨\n\n")
            append("💼 *Role:* ${job.title}\n")
            if (job.companyName.isNotBlank()) append("🏢 *Company:* ${job.companyName}\n")
            append("💰 *Salary:* ${formatPay(job)}\n")
            if (job.addressText.isNotEmpty()) append("📍 *Location:* ${job.addressText}\n")
            append("✅ *No Resume needed* (Direct Call / 1-Tap Apply)\n\n")
            append("👉 *Apply / Call Employer Now:* $jobLink")
        }
    }
}
