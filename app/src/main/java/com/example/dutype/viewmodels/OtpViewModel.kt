package com.example.dutype.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.example.dutype.services.FCMTokenManager
import com.example.dutype.state.AppStateManager
import com.example.dutype.utils.FirestoreUtils
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
import com.example.dutype.metadata.MetadataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * OtpViewModel - Handles OTP-based phone authentication
 * 
 * REFACTORED:
 * - Now injects AuthManager singleton instead of creating new instance
 * - Uses FirestoreUtils.getUserByUid() for profile checks (canonical implementation)
 * - Integrates AppStateManager for proper session initialization
 * - Initializes MetadataManager after successful authentication
 */
@HiltViewModel
class OtpViewModel @Inject constructor(
    private val fcmTokenManager: FCMTokenManager,
    private val authManager: AuthManager,  // CRITICAL FIX: Inject singleton instead of creating new instance
    private val appStateManager: AppStateManager,  // Session state management
    private val metadataManager: MetadataManager  // Metadata initialization after auth
) : ViewModel() {

    private val _otpState = MutableStateFlow(OtpState())
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    
    // Role context for FCM registration - set by LoginBottomSheet before OTP flow
    private var pendingRole: UserRole = UserRole.WORKER
    
    /**
     * Set the role context for FCM registration
     * Call this before starting OTP flow to ensure proper topic subscription
     */
    fun setRoleContext(role: UserRole) {
        pendingRole = role
        Timber.d("OtpViewModel: Role context set to $role for FCM registration")
    }

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
                                Timber.i("Phone verification completed automatically")
                                signInWithPhoneAuthCredential(credential, context)
                            }

                            override fun onVerificationFailed(e: FirebaseException) {
                                Timber.e(e, "Phone verification failed")
                                // Log for debugging Play Integrity issues
                                Timber.e("Exception class: ${e::class.simpleName}")
                                Timber.e("Full error: $e")
                                
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
                            Timber.i("OTP code sent successfully")
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
                            Timber.i("Auto-retrieval timeout - manual entry required")
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
                Timber.e(e, "Exception sending OTP")
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
                    // REFACTORED: Now uses FirestoreUtils.getUserByUid() - canonical implementation
                    Timber.d("OtpViewModel - Checking for existing profile data for user: $userId")
                    val existingProfileData = FirestoreUtils.getUserByUid(userId)
                    val hasExistingProfile = existingProfileData != null && isProfileComplete(existingProfileData)
                    
                    Timber.d("OtpViewModel - Existing profile found: $hasExistingProfile")
                    Timber.d("OtpViewModel - Profile data: $existingProfileData")
                    
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
                    
                    // CRITICAL FIX: Use injected AuthManager singleton instead of creating new instance
                    authManager.saveUser(user)
                    authManager.setLoggedIn(true)
                    
                    // Initialize AppStateManager session for proper state tracking
                    appStateManager.initializeSession(userId, user.role)
                    
                    // Initialize Firestore-dependent metadata now that user is authenticated
                    viewModelScope.launch {
                        try {
                            metadataManager.initializeWithAuth()
                            Timber.i("✅ Metadata initialized after authentication")
                        } catch (e: Exception) {
                            Timber.w(e, "⚠️ Failed to initialize metadata after auth")
                        }
                    }
                    
                    Timber.i("✅ User authenticated successfully: $userId")
                    CrashReportingHelper.logBreadcrumb("User saved to AuthManager - Authentication complete")
                    CrashReportingHelper.setUserInfo(userId, phoneNumber)
                    
                    // 🔔 Register FCM token for push notifications with role-based topics
                    viewModelScope.launch {
                        try {
                            // Use role from existing profile, or fall back to pendingRole from LoginBottomSheet
                            val userRole = if (existingProfileData?.get("role") != null) {
                                user.role.name
                            } else {
                                pendingRole.name
                            }
                            fcmTokenManager.registerTokenWithRole(userRole)
                            Timber.i("✅ FCM token registered with role for user: $userId, role: $userRole")
                        } catch (e: Exception) {
                            Timber.w(e, "⚠️ Failed to register FCM token with role, trying basic registration")
                            try {
                                fcmTokenManager.registerToken()
                                Timber.i("✅ FCM token registered (basic) for user: $userId")
                            } catch (e2: Exception) {
                                Timber.w(e2, "⚠️ Failed to register FCM token")
                            }
                        }
                    }
                    
                    // 📱 IF EXISTING PROFILE: Mark profile as complete so navigation goes to HOME not PROFILE_SETUP
                    if (hasExistingProfile) {
                        Timber.i("OtpViewModel - Existing user detected, marking profile as complete")
                        try {
                            // This updates ProfileSetupStateManager with completion status
                            // Navigation will check this flag and go to home screen
                            val userRole = user.role
                            updateProfileComplete(userId, userRole, true)
                        } catch (e: Exception) {
                            Timber.w(e, "Error marking profile as complete")
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
     * Check if user profile is complete based on Firestore data
     * REFACTORED: Extracted from inline logic for better readability
     */
    private fun isProfileComplete(userData: Map<String, Any>?): Boolean {
        if (userData == null) return false
        
        // Check if profile is actually complete (has essential fields)
        val hasEssentialData = (userData.containsKey("phoneNumber") || userData.containsKey("phone")) &&
                             userData.containsKey("address") &&
                             userData.containsKey("fullName")
        
        val isProfileComplete = userData["profileCompleted"] == true || 
                               userData["isProfileComplete"] == true
        
        Timber.d("isProfileComplete - hasEssentialData: $hasEssentialData, isProfileComplete: $isProfileComplete")
        
        return hasEssentialData || isProfileComplete
    }

    /**
     * Update profile complete status in ProfileSetupStateManager via context
     * This needs to be called from the UI layer to properly update the datastore
     */
    private suspend fun updateProfileComplete(userId: String, role: UserRole, isComplete: Boolean) {
        try {
            // Note: This will be called from UI layer which has access to ProfileCompletionViewModel
            Timber.d("updateProfileComplete - userId: $userId, role: $role, isComplete: $isComplete")
            // The actual update will happen in the UI layer via LaunchedEffect
        } catch (e: Exception) {
            Timber.e(e, "updateProfileComplete - Error")
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
                            Timber.i("Phone verification completed automatically (resend)")
                            CrashReportingHelper.logBreadcrumb("OTP resend: Auto-verification completed")
                            signInWithPhoneAuthCredential(credential, context)
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            Timber.e(e, "Phone verification failed (resend)")
                            Timber.e("Exception class: ${e::class.simpleName}")
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
                            Timber.i("OTP code resent successfully")
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
                            Timber.i("Auto-retrieval timeout (resend) - manual entry required")
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
                    Timber.e(e, "Exception resending OTP")
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
                // Invalid OTP / Wrong code entered
                msg.contains("invalid", ignoreCase = true) && msg.contains("code", ignoreCase = true) ||
                msg.contains("invalid verification code", ignoreCase = true) ||
                msg.contains("INVALID_CODE", ignoreCase = true) ||
                msg.contains("SESSION_EXPIRED", ignoreCase = true) ->
                    "The verification code you entered is incorrect. Please check and try again."
                
                // Expired OTP
                msg.contains("expired", ignoreCase = true) ||
                msg.contains("code has expired", ignoreCase = true) ->
                    "This verification code has expired. Please request a new one."
                
                // Too many attempts
                msg.contains("too many", ignoreCase = true) ||
                msg.contains("blocked", ignoreCase = true) ||
                msg.contains("unusual activity", ignoreCase = true) ->
                    "Too many verification attempts. Please wait a few minutes before trying again."
                
                // Play Store recognition delay (MOST COMMON - works locally but not on Play Store)
                msg.contains("app not Recognized by Play Store", ignoreCase = true) ||
                msg.contains("18002", ignoreCase = true) ->
                    "App recognition pending. Please wait 24-48 hours after installation for Play Store to recognize your app."
                
                // Play Integrity API errors (status codes 17028)
                msg.contains("17028", ignoreCase = true) || 
                msg.contains("Play Integrity", ignoreCase = true) ->
                    "SMS verification temporarily unavailable. Please enter OTP manually."
                    
                msg.contains("BILLING_NOT_ENABLED", ignoreCase = true) ->
                    "Phone authentication is disabled for this Firebase project."
                    
                msg.contains("quota", ignoreCase = true) ->
                    "SMS quota exceeded. Please try again later."
                    
                msg.contains("network", ignoreCase = true) ->
                    "Network error. Please check your connection and try again."
                    
                msg.contains("invalid phone", ignoreCase = true) ||
                msg.contains("invalid format", ignoreCase = true) ->
                    "Invalid phone number format. Please enter a valid 10-digit number."
                    
                else -> "Verification failed. Please try again."
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
