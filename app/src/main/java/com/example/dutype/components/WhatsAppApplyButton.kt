package com.example.dutype.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.example.dutype.utils.PhoneUtils
import java.net.URLEncoder

/**
 * WhatsApp Apply Button Component
 * 
 * P0 Feature: One-click WhatsApp apply with pre-filled message
 * India runs on WhatsApp - this is critical for user adoption
 */

// WhatsApp brand color
val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppDarkGreen = Color(0xFF128C7E)

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
    
    // Use canonical PhoneUtils for phone formatting
    val cleanPhone = PhoneUtils.formatForWhatsApp(phoneNumber)
    
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

// REMOVED: WhatsAppApplyButton(), WhatsAppApplyIconButton(), WhatsAppApplyOutlinedButton()
// These Composable button variants were never called anywhere in the codebase
// Only openWhatsAppApply() function is used directly in JobDescriptionScreen
// If button variants are needed in future, they can be re-added
