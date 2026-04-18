package com.example.dutype.utils

import android.content.Context
import com.google.android.gms.auth.api.phone.SmsRetriever
import timber.log.Timber

/**
 * SMS Retriever Helper - Auto-read OTP without SMS permission
 * 
 * This uses Google Play Services SMS Retriever API to automatically
 * detect and read OTP messages without requiring SMS permissions.
 * 
 * Benefits:
 * - No SMS permission needed (no user prompt)
 * - Works with Firebase Phone Auth
 * - Reduces reCAPTCHA triggers
 * - Better user experience
 * 
 * Note: SMS must contain app hash code (Firebase handles this automatically)
 */
object SmsRetrieverHelper {

    private val otpRegex = Regex("\\b(\\d{6})\\b")
    
    /**
     * Start SMS Retriever to auto-read OTP
     * Call this before sending OTP
     */
    fun startSmsRetriever(context: Context) {
        try {
            val client = SmsRetriever.getClient(context)
            
            val task = client.startSmsRetriever()
            
            task.addOnSuccessListener {
                Timber.d("✅ SMS Retriever started - OTP will be auto-read")
            }
            
            task.addOnFailureListener { e ->
                Timber.w(e, "⚠️ SMS Retriever failed to start - user will enter OTP manually")
            }
            
        } catch (e: Exception) {
            Timber.w(e, "⚠️ SMS Retriever not available - user will enter OTP manually")
        }
    }
    
    /**
     * Get app signature hash for SMS format
     * Only needed if you're sending SMS from your own backend
     * Firebase Phone Auth handles this automatically
     */
    fun extractOtpCode(message: String): String? {
        return otpRegex.find(message)?.groupValues?.getOrNull(1)
    }
}
