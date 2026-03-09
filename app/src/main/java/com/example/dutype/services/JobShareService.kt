package com.example.dutype.services

import android.content.Context
import android.content.Intent
import com.example.dutype.models.JobListing
import com.example.dutype.utils.DeepLinkHandler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Job Share Service - Clean Link Sharing Only
 * 
 * Shares only the job link without any extra text, tags, or promotional content.
 * WhatsApp will automatically generate a preview from the web page's Open Graph tags.
 */
@Singleton
class JobShareService @Inject constructor() {
    
    /**
     * Share job with job details and link
     * 
     * Format:
     * ```
     * 💼 Biology Teacher - Sunflower School (EM)
     * 💰 ₹Negotiable/MONTHLY
     * 📍 Payakarao Peta, Vijayawada
     * 
     * 👉 Apply now: https://dutypeapp.web.app/jobs/123
     * 
     * 📲 Download DutyPe app for instant job alerts
     * ```
     */
    fun shareJob(context: Context, job: JobListing) {
        try {
            val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
            
            val shareText = buildString {
                // Job title with emoji
                append("💼 ${job.title}")
                if (job.companyName.isNotEmpty()) {
                    append(" - ${job.companyName}")
                }
                append("\n\n")
                
                // Key details
                append("💰 ₹${job.payAmount}/${job.payType}")
                append("\n")
                append("📍 ${job.location}")
                if (job.vacancies > 1) {
                    append("\n")
                    append("👥 ${job.vacancies} openings")
                }
                append("\n\n")
                
                // Call to action with link
                append("👉 Apply now: $jobLink")
                append("\n\n")
                
                // App download message
                append("📲 Download DutyPe app for instant job alerts")
            }
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Job"))
            Timber.d("📤 Job shared: ${job.id}")
            
        } catch (e: Exception) {
            Timber.e(e, "📤 Error sharing job")
        }
    }
    
    /**
     * Share job directly to WhatsApp with job details
     */
    fun shareJobToWhatsApp(context: Context, job: JobListing) {
        try {
            val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
            
            val shareText = buildString {
                append("💼 *${job.title}*")
                if (job.companyName.isNotEmpty()) {
                    append(" - ${job.companyName}")
                }
                append("\n\n")
                
                append("💰 ₹${job.payAmount}/${job.payType}")
                append("\n")
                append("📍 ${job.location}")
                if (job.vacancies > 1) {
                    append("\n")
                    append("👥 ${job.vacancies} openings")
                }
                append("\n\n")
                
                append("👉 *Apply now:* $jobLink")
                append("\n\n")
                
                append("📲 _Download DutyPe app for instant job alerts_")
            }
            
            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            
            try {
                context.startActivity(whatsappIntent)
                Timber.d("📤 Job shared to WhatsApp: ${job.id}")
            } catch (e: Exception) {
                // WhatsApp not installed, fallback to general share
                Timber.w("WhatsApp not installed, using general share")
                shareJob(context, job)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "📤 Error sharing job to WhatsApp")
        }
    }
    
    /**
     * Copy job link to clipboard (quick share option)
     */
    fun copyJobLink(context: Context, job: JobListing): String {
        val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
        
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Job Link", jobLink)
        clipboard.setPrimaryClip(clip)
        
        Timber.d("📋 Job link copied: $jobLink")
        return jobLink
    }
    
    /**
     * Generate shareable message for job with details
     */
    fun generateShareMessage(job: JobListing): String {
        val jobLink = DeepLinkHandler.generateJobWebLink(job.id)
        
        return buildString {
            append("💼 ${job.title}")
            if (job.companyName.isNotEmpty()) {
                append(" - ${job.companyName}")
            }
            append("\n\n")
            
            append("💰 ₹${job.payAmount}/${job.payType}")
            append("\n")
            append("📍 ${job.location}")
            if (job.vacancies > 1) {
                append("\n")
                append("👥 ${job.vacancies} openings")
            }
            append("\n\n")
            
            append("👉 Apply: $jobLink")
        }
    }
}
