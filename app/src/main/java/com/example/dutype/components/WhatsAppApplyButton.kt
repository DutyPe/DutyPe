package com.example.dutype.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.dutype.utils.PhoneNumberUtils
import java.net.URLEncoder

/**
 * WhatsApp Apply Button Component
 * 
 * P0 Feature: One-click WhatsApp apply with pre-filled message
 * India runs on WhatsApp - this is critical for user adoption
 */

/**
 * Opens WhatsApp with pre-filled job application message
 */
fun openWhatsAppApply(
    context: Context,
    phoneNumber: String,
    jobTitle: String,
    companyName: String,
    salary: String = "",
    location: String = ""
) {
    if (phoneNumber.isBlank()) {
        Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
        return
    }
    
    // Use canonical PhoneNumberUtils for phone formatting
    val cleanPhone = PhoneNumberUtils.formatForWhatsApp(phoneNumber)
    
    // Build pre-filled message
    val message = buildString {
        append("🙏 Namaste!\n\n")
        append("I found your job posting on *DutyPe* and I'm interested in applying.\n\n")
        append("📋 *Job:* $jobTitle\n")
        if (companyName.isNotBlank()) append("🏢 *Company:* $companyName\n")
        if (salary.isNotBlank()) append("💰 *Salary:* $salary\n")
        if (location.isNotBlank()) append("📍 *Location:* $location\n")
        append("\nPlease let me know the next steps.\n\n")
        append("Thank you! 🙏")
    }
    
    val encodedMessage = URLEncoder.encode(message, "UTF-8")
    val whatsappUrl = "https://wa.me/$cleanPhone?text=$encodedMessage"
    
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
    }
}
