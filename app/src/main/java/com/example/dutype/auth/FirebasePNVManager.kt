package com.example.dutype.auth

import android.app.Activity
import com.google.firebase.pnv.FirebasePhoneNumberVerification
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Phone Number Verification (PNV) Manager
 * 
 * Implements Firebase's recommended phone verification using Android Credential Manager.
 * This is more secure and user-friendly than SMS OTP.
 * 
 * Benefits:
 * - No SMS permission required
 * - Instant verification (no SMS delay)
 * - User consent-based (transparent)
 * - More secure than SMS
 * 
 * @see https://firebase.google.com/docs/phone-number-verification/android/get-started
 */
@Singleton
class FirebasePNVManager @Inject constructor() {
    
    /**
     * Check if device supports Firebase PNV
     * Call this on app launch to determine which verification method to use
     * 
     * @return true if at least one SIM supports PNV, false otherwise
     */
    suspend fun checkPNVSupport(): Boolean {
        return try {
            val fpnv = FirebasePhoneNumberVerification.getInstance()
            val results = fpnv.getVerificationSupportInfo().await()
            
            val isSupported = results.any { it.isSupported() }
            
            if (isSupported) {
                Timber.d("📱 Firebase PNV: Device supports PNV - will use Credential Manager")
            } else {
                Timber.d("📱 Firebase PNV: Device does NOT support PNV - will fallback to SMS OTP")
            }
            
            isSupported
        } catch (e: Exception) {
            Timber.e(e, "📱 Firebase PNV: Error checking support - will fallback to SMS OTP")
            false
        }
    }
    
    /**
     * Verify phone number using Firebase PNV
     * 
     * This method:
     * 1. Shows user consent dialog (Android Credential Manager)
     * 2. Verifies phone number with Firebase backend
     * 3. Returns verified phone number and signed token
     * 
     * @param activity Activity context (required for consent dialog)
     * @return PNVResult with phone number and token, or null if failed
     */
    suspend fun verifyPhoneNumber(activity: Activity): PNVResult? {
        return try {
            Timber.d("📱 Firebase PNV: Starting verification flow...")
            
            // Get instance with Activity context (required for consent dialog)
            val fpnv = FirebasePhoneNumberVerification.getInstance(activity)
            
            // Call getVerifiedPhoneNumber - handles entire flow
            val result = fpnv.getVerifiedPhoneNumber().await()
            
            // Extract phone number and token from result
            val verifiedPhoneNumber = result.getPhoneNumber()
            val verifiedToken = result.getToken()
            
            Timber.d("📱 Firebase PNV: ✅ Verification successful!")
            Timber.d("📱 Firebase PNV: Phone: $verifiedPhoneNumber")
            Timber.d("📱 Firebase PNV: Token received (length: ${verifiedToken.length})")
            
            PNVResult(
                phoneNumber = verifiedPhoneNumber,
                token = verifiedToken,
                success = true
            )
        } catch (e: Exception) {
            Timber.e(e, "📱 Firebase PNV: ❌ Verification failed")
            
            // Common failure reasons:
            // - User declined consent
            // - Network error
            // - SIM not supported
            // - Firebase PNV not enabled in console
            
            PNVResult(
                phoneNumber = null,
                token = null,
                success = false,
                error = e.message
            )
        }
    }
}

/**
 * Result of Firebase PNV verification
 */
data class PNVResult(
    val phoneNumber: String?,
    val token: String?,
    val success: Boolean,
    val error: String? = null
)
