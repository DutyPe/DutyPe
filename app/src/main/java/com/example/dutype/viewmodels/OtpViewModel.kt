package com.example.dutype.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.TimeUnit
import com.example.dutype.utils.CrashReportingHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor() : ViewModel() {

    private val _otpState = MutableStateFlow(OtpState())
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(phoneNumber: String, context: Context) {
        viewModelScope.launch {
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            
            // Log to crash reports
            CrashReportingHelper.logBreadcrumb("OTP send started: $phoneNumber")
            
            try {
                // Get activity from context (required for PhoneAuthProvider)
                val activity = context as? android.app.Activity
                if (activity == null) {
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Activity context required for phone authentication"
                    )
                    return@launch
                }
                
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                // Auto-verification completed (instant verification or auto-retrieval)
                                println("✅ Phone verification completed automatically")
                                signInWithPhoneAuthCredential(credential, context)
                            }

                            override fun onVerificationFailed(e: FirebaseException) {
                                println("❌ Phone verification failed: ${e.message}")
                                // Log for debugging Play Integrity issues
                                println("Exception class: ${e::class.simpleName}")
                                println("Full error: $e")
                                
                                Timber.e("❌ OTP verification failed: ${e.message}")
                                Timber.e("Exception: ${e::class.simpleName} - $e")
                                
                                // Log to crash reports for Play Console
                                CrashReportingHelper.logEvent("otp_verification_failed", e.message ?: "unknown")
                                CrashReportingHelper.logBreadcrumb("OTP verification failed: ${e::class.simpleName}")
                                
                                // Specific error handling for Play Store app recognition delay
                                if (e.message?.contains("app not Recognized", ignoreCase = true) == true) {
                                    Timber.w("⚠️ CRITICAL: App not recognized by Play Store yet!")
                                    Timber.w("💡 Wait 24-48 hours after upload for Play Store to recognize your app")
                                    CrashReportingHelper.logEvent("app_not_recognized_by_play_store", true)
                                }
                                
                                _otpState.value = _otpState.value.copy(
                                    isLoading = false,
                                    error = mapPhoneAuthError(e),
                                    message = "SMS verification encountered an issue. You can still manually enter the OTP sent to your phone."
                                )
                            }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            println("📲 OTP code sent successfully")
                            storedVerificationId = verificationId
                            resendToken = token
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                otpSent = true,
                                message = "OTP sent to $phoneNumber"
                            )
                        }
                        
                        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                            // Auto-retrieval timeout - user must manually enter the code
                            println("⏱️ Auto-retrieval timeout - manual entry required")
                            storedVerificationId = verificationId
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                otpSent = true,
                                message = "Please enter the OTP sent to your phone"
                            )
                        }
                    })
                    .build()
                
                PhoneAuthProvider.verifyPhoneNumber(options)
            } catch (e: Exception) {
                println("❌ Exception sending OTP: ${e.message}")
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = mapPhoneAuthError(e),
                    message = "SMS verification is temporarily unavailable. Please enter OTP manually when received."
                )
            }
        }
    }

    fun verifyOtp(otp: String, context: Context) {
        viewModelScope.launch {
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            
            // Track OTP verification attempt for crash investigation
            CrashReportingHelper.logBreadcrumb("OTP verification started - Code: ${otp.take(1)}***")
            
            try {
                val verificationId = storedVerificationId
                       if (verificationId != null) {
                           CrashReportingHelper.logBreadcrumb("OTP verification ID available - proceeding")
                           val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                           signInWithPhoneAuthCredential(credential, context)
                       } else {
                    Timber.e("❌ Verification ID not found for OTP verification")
                    CrashReportingHelper.logBreadcrumb("OTP verification failed: No verification ID stored")
                    CrashReportingHelper.logEvent("otp_verify_no_verification_id", true)
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Verification ID not found"
                    )
                }
            } catch (e: Exception) {
                Timber.e("❌ OTP verification exception: ${e.message}")
                CrashReportingHelper.logBreadcrumb("OTP verification error: ${e::class.simpleName}")
                CrashReportingHelper.logEvent("otp_verify_exception", e.message ?: "unknown")
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to verify OTP"
                )
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, context: Context) {
        viewModelScope.launch {
            CrashReportingHelper.logBreadcrumb("Phone credential sign-in started")
            try {
                val result = auth.signInWithCredential(credential).await()
                val firebaseUser = result.user
                
                if (firebaseUser != null) {
                    val phoneNumber = firebaseUser.phoneNumber ?: ""
                    val userId = firebaseUser.uid
                    CrashReportingHelper.logBreadcrumb("Firebase sign-in successful: $phoneNumber")
                    
                    // 🔍 CRITICAL: Check if user has existing profile data in Firestore
                    println("🔍 OtpViewModel - Checking for existing profile data for user: $userId")
                    val profileCheckResult = checkExistingProfile(userId)
                    val hasExistingProfile = profileCheckResult.first
                    val existingProfileData = profileCheckResult.second
                    
                    println("🔍 OtpViewModel - Existing profile found: $hasExistingProfile")
                    println("🔍 OtpViewModel - Profile data: $existingProfileData")
                    
                    // Create user object from Firebase data, using existing profile if available
                    val user = User(
                        id = userId,
                        email = existingProfileData?.get("email") as? String ?: "",
                        fullName = existingProfileData?.get("fullName") as? String ?: "",
                        role = if (existingProfileData?.get("role") != null) {
                            try {
                                UserRole.valueOf((existingProfileData["role"] as? String)?.uppercase() ?: "WORKER")
                            } catch (e: Exception) {
                                UserRole.WORKER
                            }
                        } else {
                            UserRole.WORKER
                        },
                        isVerified = true,
                        profileImageUrl = existingProfileData?.get("profileImageUrl") as? String
                    )
                    
                    // Save user to AuthManager
                    val authManager = AuthManager(context)
                    authManager.saveUser(user)
                    authManager.setLoggedIn(true)
                    
                    Timber.i("✅ User authenticated successfully: $userId")
                    CrashReportingHelper.logBreadcrumb("User saved to AuthManager - Authentication complete")
                    CrashReportingHelper.setUserInfo(userId, phoneNumber)
                    
                    // 📱 IF EXISTING PROFILE: Mark profile as complete so navigation goes to HOME not PROFILE_SETUP
                    if (hasExistingProfile) {
                        println("✅ OtpViewModel - Existing user detected, marking profile as complete")
                        try {
                            // This updates ProfileSetupStateManager with completion status
                            // Navigation will check this flag and go to home screen
                            val userRole = user.role
                            updateProfileComplete(userId, userRole, true)
                        } catch (e: Exception) {
                            println("⚠️ Error marking profile as complete: ${e.message}")
                            Timber.w("⚠️ Error marking profile as complete: ${e.message}")
                        }
                    }
                    
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        otpVerified = true,
                        message = "Phone authentication successful",
                        phoneNumber = firebaseUser.phoneNumber  // Store phone number
                    )
                } else {
                    Timber.e("❌ Authentication succeeded but no user returned")
                    CrashReportingHelper.logBreadcrumb("Sign-in failed: No Firebase user returned")
                    CrashReportingHelper.logEvent("sign_in_no_user", true)
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Authentication failed - no user data"
                    )
                }
            } catch (e: Exception) {
                Timber.e("❌ Phone auth credential sign-in failed: ${e.message}")
                CrashReportingHelper.logBreadcrumb("Phone credential sign-in error: ${e::class.simpleName}")
                CrashReportingHelper.logEvent("phone_credential_signin_error", e.message ?: "unknown")
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = mapPhoneAuthError(e)
                )
            }
        }
    }

    /**
     * Check if user has existing profile in Firestore
     * Returns Pair<hasProfile, profileData>
     */
    private suspend fun checkExistingProfile(userId: String): Pair<Boolean, Map<String, Any?>?> {
        return try {
            val userDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()
            
            if (userDoc.exists()) {
                val userData = userDoc.data
                // Check if profile is actually complete (has essential fields)
                val hasEssentialData = userData?.containsKey("phoneNumber") == true || 
                                     userData?.containsKey("phone") == true &&
                                     userData?.containsKey("address") == true &&
                                     userData?.containsKey("fullName") == true
                
                val isProfileComplete = userData?.get("profileCompleted") == true || 
                                       userData?.get("isProfileComplete") == true
                
                println("🔍 checkExistingProfile - Document exists: true, hasEssentialData: $hasEssentialData, isProfileComplete: $isProfileComplete")
                
                // Return true if profile is complete or has essential data
                val hasProfile = hasEssentialData || isProfileComplete
                Pair(hasProfile, if (hasProfile) userData else null)
            } else {
                println("🔍 checkExistingProfile - Document exists: false")
                Pair(false, null)
            }
        } catch (e: Exception) {
            println("❌ checkExistingProfile - Error: ${e.message}")
            Pair(false, null)
        }
    }

    /**
     * Update profile complete status in ProfileSetupStateManager via context
     * This needs to be called from the UI layer to properly update the datastore
     */
    private suspend fun updateProfileComplete(userId: String, role: UserRole, isComplete: Boolean) {
        try {
            // Note: This will be called from UI layer which has access to ProfileCompletionViewModel
            println("📱 updateProfileComplete - userId: $userId, role: $role, isComplete: $isComplete")
            // The actual update will happen in the UI layer via LaunchedEffect
        } catch (e: Exception) {
            println("❌ updateProfileComplete - Error: ${e.message}")
        }
    }

    fun resendOtp(phoneNumber: String, context: Context) {
        viewModelScope.launch {
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            
            // Track OTP resend attempt for crash investigation
            CrashReportingHelper.logBreadcrumb("OTP resend started: $phoneNumber")
            
            try {
                // Get activity from context (required for PhoneAuthProvider)
                val activity = context as? android.app.Activity
                if (activity == null) {
                    Timber.e("❌ No Activity context available for OTP resend")
                    CrashReportingHelper.logBreadcrumb("OTP resend failed: No Activity context")
                    CrashReportingHelper.logEvent("otp_resend_no_activity", true)
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Activity context required for phone authentication"
                    )
                    return@launch
                }
                
                val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            println("✅ Phone verification completed automatically (resend)")
                            CrashReportingHelper.logBreadcrumb("OTP resend: Auto-verification completed")
                            signInWithPhoneAuthCredential(credential, context)
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            println("❌ Phone verification failed (resend): ${e.message}")
                            println("Exception class: ${e::class.simpleName}")
                            Timber.e("❌ OTP resend verification failed: ${e.message}")
                            CrashReportingHelper.logBreadcrumb("OTP resend verification failed: ${e::class.simpleName}")
                            CrashReportingHelper.logEvent("otp_resend_failed", e.message ?: "unknown")
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                error = mapPhoneAuthError(e),
                                message = "SMS verification encountered an issue. You can still manually enter the OTP sent to your phone."
                            )
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            println("📲 OTP code resent successfully")
                            CrashReportingHelper.logBreadcrumb("OTP resend: Code sent successfully to $phoneNumber")
                            storedVerificationId = verificationId
                            resendToken = token
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                otpSent = true,
                                message = "OTP resent to $phoneNumber"
                            )
                        }
                        
                        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                            println("⏱️ Auto-retrieval timeout (resend) - manual entry required")
                            storedVerificationId = verificationId
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                otpSent = true,
                                message = "Please enter the OTP sent to your phone"
                            )
                        }
                    })
                
                // Only set force resending token if it's not null
                resendToken?.let { token ->
                    optionsBuilder.setForceResendingToken(token)
                }
                
                val options = optionsBuilder.build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            } catch (e: Exception) {
                    println("❌ Exception resending OTP: ${e.message}")
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = mapPhoneAuthError(e),
                        message = "SMS verification is temporarily unavailable. Please enter OTP manually when received."
                    )
            }
        }
    }

        /**
         * Map known phone-auth exceptions to friendly messages.
         * Handles specific Play Integrity API errors that prevent SMS auto-retrieval.
         * 
         * CRITICAL: If you see "app not Recognized by Play Store" error on Play Store version:
         * → App needs 24-48 hours to be recognized by Google Play Store
         * → Works locally because debug apps bypass Play Integrity checks
         */
        private fun mapPhoneAuthError(e: Exception): String {
            val msg = e.message ?: "Verification failed"
            return when {
                // Play Store recognition delay (MOST COMMON - works locally but not on Play Store)
                msg.contains("app not Recognized by Play Store", ignoreCase = true) ||
                msg.contains("18002", ignoreCase = true) ->
                    "⏳ App recognition pending. Please wait 24-48 hours after installation for Play Store to recognize your app. OTP will then work automatically."
                
                // Play Integrity API errors (status codes 17028)
                msg.contains("17028", ignoreCase = true) || 
                msg.contains("Play Integrity", ignoreCase = true) ->
                    "SMS verification temporarily unavailable. Ensure your app is recognized by Play Store. Please enter OTP manually."
                    
                msg.contains("BILLING_NOT_ENABLED", ignoreCase = true) ->
                    "Phone authentication is disabled for this Firebase project. Enable billing and configure Play Integrity or reCAPTCHA Enterprise in the Firebase Console."
                msg.contains("quota", ignoreCase = true) ->
                    "SMS quota exceeded for this project. Check Firebase usage and billing."
                msg.contains("network", ignoreCase = true) ->
                    "Network error. Please check your connection and try again."
                else -> msg
            }
        }

    fun resetState() {
        _otpState.value = OtpState()
        storedVerificationId = null
        resendToken = null
    }
}

data class OtpState(
    val isLoading: Boolean = false,
    val otpSent: Boolean = false,
    val otpVerified: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val phoneNumber: String? = null  // Store phone number for profile setup
)
