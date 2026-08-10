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
 * Job Share Service â€” uses only schema fields.
 * salary (Double) + salaryType (String) from jobs collection.
 * addressText (String) from job_details (runtime only).
 */
@Singleton
class JobShareService @Inject constructor() {

    private fun formatPay(job: JobListing): String =
        "â‚¹" + com.example.dutype.utils.SalaryFormatter.display(job.salary, job.salaryType)

    fun shareJob(context: Context, job: JobListing) {
        try {
            val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
            val shareText = buildString {
                append("ðŸ’¼ ${job.title}\n\n")
                append("ðŸ’° ${formatPay(job)}\n")
                if (job.addressText.isNotEmpty()) append("ðŸ“ ${job.addressText}\n")
                append("\nðŸ‘‰ Apply now: $jobLink\n\n")
                append("ðŸ“² Download DutyPe app for instant job alerts")
            }
            context.startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }, "Share Job"
            ))
            Timber.d("ðŸ“¤ Job shared: ${job.id}")
        } catch (e: Exception) {
            Timber.e(e, "ðŸ“¤ Error sharing job")
        }
    }

    fun shareJobToWhatsApp(context: Context, job: JobListing) {
        try {
            val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
            val shareText = buildString {
                append("ðŸ’¼ *${job.title}*\n\n")
                append("ðŸ’° ${formatPay(job)}\n")
                if (job.addressText.isNotEmpty()) append("ðŸ“ ${job.addressText}\n")
                append("\nðŸ‘‰ *Apply now:* $jobLink\n\n")
                append("ðŸ“² _Download DutyPe app for instant job alerts_")
            }
            try {
                context.startActivity(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                })
                Timber.d("ðŸ“¤ Job shared to WhatsApp: ${job.id}")
            } catch (e: Exception) {
                shareJob(context, job)
            }
        } catch (e: Exception) {
            Timber.e(e, "ðŸ“¤ Error sharing job to WhatsApp")
        }
    }

    fun copyJobLink(context: Context, job: JobListing): String {
        val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(context.getString(R.string.job_link_clip_label), jobLink))
        Timber.d("ðŸ“‹ Job link copied: $jobLink")
        return jobLink
    }

    fun generateShareMessage(job: JobListing): String {
        val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
        return buildString {
            append("ðŸ’¼ ${job.title}\n\n")
            append("ðŸ’° ${formatPay(job)}\n")
            if (job.addressText.isNotEmpty()) append("ðŸ“ ${job.addressText}\n")
            append("\nðŸ‘‰ Apply: $jobLink")
        }
    }
}
