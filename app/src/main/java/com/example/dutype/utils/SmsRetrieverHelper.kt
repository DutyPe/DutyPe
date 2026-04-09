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
    fun getAppSignature(context: Context): String? {
        return try {
            val appSignatureHelper = AppSignatureHelper(context)
            val signatures = appSignatureHelper.getAppSignatures()
            signatures.firstOrNull()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get app signature")
            null
        }
    }

    fun extractOtpCode(message: String): String? {
        return otpRegex.find(message)?.groupValues?.getOrNull(1)
    }
}

/**
 * Helper class to get app signature for SMS Retriever API
 * Only needed if sending SMS from custom backend
 */
private class AppSignatureHelper(private val context: Context) {
    
    fun getAppSignatures(): List<String> {
        val signatures = mutableListOf<String>()
        
        try {
            val packageName = context.packageName
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
            
            packageInfo.signatures?.forEach { signature ->
                val hash = hash(packageName, signature.toCharsString())
                if (hash != null) {
                    signatures.add(hash)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting app signatures")
        }
        
        return signatures
    }
    
    private fun hash(packageName: String, signature: String): String? {
        val appInfo = "$packageName $signature"
        try {
            val messageDigest = java.security.MessageDigest.getInstance("SHA-256")
            messageDigest.update(appInfo.toByteArray(Charsets.UTF_8))
            var hashSignature = messageDigest.digest()
            
            hashSignature = hashSignature.copyOfRange(0, 9)
            var base64Hash = android.util.Base64.encodeToString(
                hashSignature,
                android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
            )
            base64Hash = base64Hash.substring(0, 11)
            
            return base64Hash
        } catch (e: Exception) {
            return null
        }
    }
}
