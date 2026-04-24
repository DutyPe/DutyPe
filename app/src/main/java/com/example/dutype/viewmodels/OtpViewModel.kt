package com.example.dutype.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutype.app.BuildConfig
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.example.dutype.services.AuthFlowService
import com.example.dutype.services.FCMTokenManager
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
import com.example.dutype.metadata.MetadataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * OtpViewModel - Handles phone authentication via SMS OTP.
 *
 * - Injects AuthManager singleton instead of creating a new instance.
 * - Uses FirestoreUtils.getUserByUid() for profile checks (canonical implementation).
 * - Initializes MetadataManager after successful authentication.
 */
@HiltViewModel
class OtpViewModel @Inject constructor(
    private val fcmTokenManager: FCMTokenManager,
    private val authManager: AuthManager,
    private val metadataManager: MetadataManager,
    private val authFlowService: AuthFlowService,
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker,
    private val errorHandler: com.example.dutype.core.error.ErrorHandler
) : ViewModel() {

    private val _otpState = MutableStateFlow(OtpState())
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    // Resend cooldown timer - prevents spam and reduces rate limiting
    private val _resendCooldownSeconds = MutableStateFlow(0)
    val resendCooldownSeconds: StateFlow<Int> = _resendCooldownSeconds.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    
    // Role context for FCM registration - set by LoginBottomSheet before OTP flow
    private var pendingRole: UserRole = UserRole.WORKER

    enum class PostOtpDestination {
        HOME,
        PROFILE_SETUP
    }

    data class PostOtpNavigation(
        val destination: PostOtpDestination,
        val role: UserRole,
        val message: String? = null
    )

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
            val startTime = System.currentTimeMillis()
            com.example.dutype.performance.MainThreadChecker.assertMainThread("OtpViewModel.sendOtp")
            
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            
            // Log to crash reports
            errorHandler.logBreadcrumb("OTP send started: $phoneNumber")

            // Bug #4 (batch-i) defense-in-depth: re-run the phone/role
            // pre-check here so a UI bypass or stale callable response
            // cannot cause us to burn an SMS for a phone that is already
            // registered under a different role. Previously the conflict
            // was only surfaced AFTER `completeLogin()` ran — which is
            // AFTER the user typed the 6-digit code.
            //
            // Only bail on a DEFINITIVE conflict (EXISTS + roleConflict).
            // We do NOT block on UNKNOWN here because the UI layer is
            // now fail-closed for that case; a second UNKNOWN-block
            // inside the ViewModel would just produce a duplicate toast.
            runCatching {
                FirestoreUtils.checkPhoneForRole(
                    phoneNumber = phoneNumber,
                    requestedRole = pendingRole.name
                )
            }.onSuccess { phoneCheck ->
                if (phoneCheck.exists == FirestoreUtils.PhoneExistenceResult.EXISTS &&
                    phoneCheck.roleConflict
                ) {
                    val existingRole = phoneCheck.existingRole?.lowercase() ?: "different role"
                    Timber.w(
                        "🔒 OTP blocked before send — phone=%s already registered as %s, requested=%s",
                        phoneNumber, phoneCheck.existingRole, pendingRole.name
                    )
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        otpSent = false,
                        error = "phone-already-registered-as:$existingRole",
                        message = "This number is already registered as a $existingRole. Please log in as a $existingRole."
                    )
                    errorHandler.logEvent("otp_send_blocked_role_conflict", true)
                    return@launch
                }
            }.onFailure { err ->
                // Apr 2026 hardening: fail-CLOSED on pre-check errors. Previously
                // the ViewModel logged and continued, which meant a transient
                // Firestore/network blip would burn an SMS for a phone that may
                // have been registered under a different role. Now we refuse
                // to send OTP unless the role check has a definitive answer.
                Timber.w(err, "🔒 OTP blocked — pre-send role check failed for $phoneNumber")
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    otpSent = false,
                    error = "phone-precheck-failed",
                    message = "Could not verify this number right now. Please try again in a moment."
                )
                errorHandler.logEvent("otp_send_blocked_precheck_failed", true)
                return@launch
            }
            
            // Start 60-second cooldown timer for initial OTP send
            startResendCooldown()
            

            try {
                // Get activity from context (required for PhoneAuthProvider)
                val activity = context as? android.app.Activity
                if (activity == null) {
                    val duration = System.currentTimeMillis() - startTime
                    performanceTracker.trackApiCall("send_otp", duration, success = false)
                    
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Activity context required for phone authentication"
                    )
                    return@launch
                }

                configureDebugRecaptchaFallback()
                
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    // Enable reCAPTCHA verification to prevent rate limiting
                    // Firebase automatically uses invisible reCAPTCHA or reCAPTCHA Enterprise
                    // This significantly increases SMS quota and prevents "too many requests" errors
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                // Auto-verification completed (instant verification or auto-retrieval)
                                val duration = System.currentTimeMillis() - startTime
                                performanceTracker.trackApiCall("send_otp", duration, success = true)
                                
                                Timber.i("Phone verification completed automatically")
                                signInWithPhoneAuthCredential(credential, context)
                            }

                            override fun onVerificationFailed(e: FirebaseException) {
                                val duration = System.currentTimeMillis() - startTime
                                performanceTracker.trackApiCall("send_otp", duration, success = false)
                                
                                Timber.e(e, "Phone verification failed")
                                // Log for debugging Play Integrity issues
                                Timber.e("Exception class: ${e::class.simpleName}")
                                Timber.e("Full error: $e")
                                
                                Timber.e("âŒ OTP verification failed: ${e.message}")
                                Timber.e("Exception: ${e::class.simpleName} - $e")
                                
                                // Log to crash reports for Play Console
                                errorHandler.logEvent("otp_verification_failed", e.message ?: "unknown")
                                errorHandler.logBreadcrumb("OTP verification failed: ${e::class.simpleName}")
                                
                                // Specific error handling for Play Store app recognition delay
                                if (e.message?.contains("app not Recognized", ignoreCase = true) == true) {
                                    Timber.w("âš ï¸ CRITICAL: App not recognized by Play Store yet!")
                                    Timber.w(" Wait 24-48 hours after upload for Play Store to recognize your app")
                                    errorHandler.logEvent("app_not_recognized_by_play_store", true)
                                }

                                if (isBillingNotEnabledError(e.message)) {
                                    errorHandler.logEvent("otp_billing_not_enabled", true)
                                    errorHandler.logBreadcrumb("OTP blocked: Firebase phone auth billing not enabled")
                                }
                                
                                _otpState.value = _otpState.value.copy(
                                    isLoading = false,
                                    error = mapPhoneAuthError(e),
                                    message = if (isBillingNotEnabledError(e.message)) {
                                        "Admin action required: enable billing on Firebase project 'dutype-860ac' to send OTP."
                                    } else {
                                        "SMS verification encountered an issue. Please retry in a moment."
                                    }
                                )
                            }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            val duration = System.currentTimeMillis() - startTime
                            performanceTracker.trackApiCall("send_otp", duration, success = true)
                            
                            Timber.i("OTP code sent successfully in ${duration}ms")
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
                val duration = System.currentTimeMillis() - startTime
                performanceTracker.trackApiCall("send_otp", duration, success = false)
                
                Timber.e(e, "Exception sending OTP")
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = mapPhoneAuthError(e),
                    message = if (isBillingNotEnabledError(e.message)) {
                        "Admin action required: enable billing on Firebase project 'dutype-860ac' to send OTP."
                    } else {
                        "SMS verification is temporarily unavailable. Please retry."
                    }
                )
            }
        }
    }

    fun verifyOtp(otp: String, context: Context) {
        viewModelScope.launch {
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            
            // Track OTP verification attempt for crash investigation
            errorHandler.logBreadcrumb("OTP verification started - Code: ${otp.take(1)}***")
            
            try {
                val verificationId = storedVerificationId
                       if (verificationId != null) {
                           errorHandler.logBreadcrumb("OTP verification ID available - proceeding")
                           val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                           signInWithPhoneAuthCredential(credential, context)
                       } else {
                    Timber.e("âŒ Verification ID not found for OTP verification")
                    errorHandler.logBreadcrumb("OTP verification failed: No verification ID stored")
                    errorHandler.logEvent("otp_verify_no_verification_id", true)
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Verification ID not found"
                    )
                }
            } catch (e: Exception) {
                Timber.e("âŒ OTP verification exception: ${e.message}")
                errorHandler.logBreadcrumb("OTP verification error: ${e::class.simpleName}")
                errorHandler.logEvent("otp_verify_exception", e.message ?: "unknown")
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to verify OTP"
                )
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, context: Context) {
        viewModelScope.launch {
            errorHandler.logBreadcrumb("Phone credential sign-in started")
            try {
                val result = auth.signInWithCredential(credential).await()
                val firebaseUser = result.user
                
                if (firebaseUser != null) {
                    val phoneNumber = firebaseUser.phoneNumber ?: ""
                    val userId = firebaseUser.uid
                    errorHandler.logBreadcrumb("Firebase sign-in successful: $phoneNumber")
                    
                    //  CRITICAL: Check if user has existing profile data in Firestore
                    // REFACTORED: Now uses FirestoreUtils.getUserByUid() - canonical implementation
                    Timber.d("OtpViewModel - Checking for existing profile data for user: $userId")
                    val existingProfileData = FirestoreUtils.getUserByUid(userId)
                    val hasExistingProfile = existingProfileData != null && isProfileComplete(existingProfileData)
                    
                    Timber.d("OtpViewModel - Existing profile found: $hasExistingProfile")
                    Timber.d("OtpViewModel - Profile data: $existingProfileData")
                    
                    val resolvedRole = run {
                        val raw = (existingProfileData?.get("role") as? String)
                            ?: (existingProfileData?.get("activeRole") as? String)
                            ?: (existingProfileData?.get("roles") as? List<*>)?.firstOrNull()?.toString()
                            ?: pendingRole.name
                        runCatching { UserRole.valueOf(raw.uppercase()) }.getOrDefault(UserRole.WORKER)
                    }
                    val user = User(
                        id = userId,
                        fullName = existingProfileData?.get("fullName") as? String ?: "",
                        phone = existingProfileData?.get("phone") as? String ?: phoneNumber,
                        role = resolvedRole,
                        profileImageUrl = existingProfileData?.get("profileImageUrl") as? String
                    )
                    
                    // CRITICAL FIX: Use injected AuthManager singleton instead of creating new instance
                    authManager.saveUser(user)
                    authManager.setLoggedIn(true)

                    // Initialize Firestore-dependent metadata now that user is authenticated
                    viewModelScope.launch {
                        try {
                            metadataManager.initializeWithAuth()
                            Timber.i("âœ… Metadata initialized after authentication")
                        } catch (e: Exception) {
                            Timber.w(e, "âš ï¸ Failed to initialize metadata after auth")
                        }
                    }
                    
                    Timber.i("âœ… User authenticated successfully: $userId")
                    errorHandler.logBreadcrumb("User saved to AuthManager - Authentication complete")
                    errorHandler.setUserInfo(userId, phoneNumber)

                    if (hasExistingProfile) {
                        // Existing account: users/{uid} already exists, safe to register token now.
                        viewModelScope.launch {
                            try {
                                val userRole = if (existingProfileData?.get("role") != null || existingProfileData?.get("activeRole") != null) {
                                    user.role.name
                                } else {
                                    pendingRole.name
                                }
                                fcmTokenManager.registerTokenWithRole(userRole)
                                Timber.i("âœ… FCM token registered with role for user: $userId, role: $userRole")
                            } catch (e: Exception) {
                                Timber.w(e, "âš ï¸ Failed to register FCM token with role, trying basic registration")
                                try {
                                    fcmTokenManager.registerToken()
                                    Timber.i("âœ… FCM token registered (basic) for user: $userId")
                                } catch (e2: Exception) {
                                    Timber.w(e2, "âš ï¸ Failed to register FCM token")
                                }
                            }
                        }
                    } else {
                        Timber.i("OtpViewModel - New user detected, deferring FCM registration until completeRegistration")
                    }
                    
                    //  IF EXISTING PROFILE: Mark profile as complete so navigation goes to HOME not PROFILE_SETUP
                    if (hasExistingProfile) {
                        Timber.i("OtpViewModel - Existing user detected, marking profile as complete")
                        try {
                            // This updates ProfileSetupStateManager with completion status
                            // Navigation will check this flag and go to home screen
                            val userRole = user.role
                            updateProfileComplete(userId, userRole, true)
                        } catch (e: Exception) {
                            Timber.w(e, "Error marking profile as complete")
                            Timber.w("âš ï¸ Error marking profile as complete: ${e.message}")
                        }
                    }
                    
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        otpVerified = true,
                        message = "Phone authentication successful",
                        phoneNumber = firebaseUser.phoneNumber  // Store phone number
                    )
                } else {
                    Timber.e("âŒ Authentication succeeded but no user returned")
                    errorHandler.logBreadcrumb("Sign-in failed: No Firebase user returned")
                    errorHandler.logEvent("sign_in_no_user", true)
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Authentication failed - no user data"
                    )
                }
            } catch (e: Exception) {
                Timber.e("âŒ Phone auth credential sign-in failed: ${e.message}")
                errorHandler.logBreadcrumb("Phone credential sign-in error: ${e::class.simpleName}")
                errorHandler.logEvent("phone_credential_signin_error", e.message ?: "unknown")
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

        val hasEssentialData = !((userData["phone"] as? String).isNullOrBlank()) &&
            !((userData["fullName"] as? String).isNullOrBlank())

        val hasRoleData = when {
            userData["activeRole"] is String -> true
            (userData["roles"] as? List<*>)?.isNotEmpty() == true -> true
            else -> false
        }

        Timber.d("isProfileComplete - hasEssentialData: $hasEssentialData, hasRoleData: $hasRoleData")

        return hasEssentialData && hasRoleData
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

    private fun cacheResolvedUser(userData: Map<String, Any>, fallbackRole: UserRole) {
        try {
            val resolvedRole = run {
                val raw = (userData["role"] as? String)
                    ?: (userData["activeRole"] as? String)
                    ?: (userData["roles"] as? List<*>)?.firstOrNull()?.toString()
                    ?: fallbackRole.name
                runCatching { UserRole.valueOf(raw.uppercase()) }.getOrDefault(fallbackRole)
            }

            val cachedUser = User(
                id = userData["userId"] as? String ?: auth.currentUser?.uid.orEmpty(),
                fullName = userData["fullName"] as? String ?: "",
                phone = userData["phone"] as? String ?: auth.currentUser?.phoneNumber.orEmpty(),
                role = resolvedRole,
                profileImageUrl = userData["profileImageUrl"] as? String
            )

            authManager.saveUser(cachedUser)
            authManager.setLoggedIn(true)
        } catch (e: Exception) {
            Timber.w(e, "Failed to cache resolved user")
        }
    }

    suspend fun completeRegistration(
        role: UserRole,
        fullName: String,
        referralCode: String?
    ): Result<PostOtpNavigation> {
        return try {
            authFlowService.completeRegistration(
                requestedRole = role.name,
                fullName = fullName,
                referralCode = referralCode
            ).fold(
                onSuccess = { resolution ->
                    cacheResolvedUser(resolution.userData, role)
                    runCatching { fcmTokenManager.registerTokenWithRole(role.name) }
                    Result.success(PostOtpNavigation(PostOtpDestination.PROFILE_SETUP, role))
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeLogin(role: UserRole): Result<PostOtpNavigation> {
        return try {
            authFlowService.resolveLogin(role.name).fold(
                onSuccess = { resolution ->
                    // Bug #9 fix: single-role-per-phone enforcement at LOGIN time.
                    // If the user has an existing account under a different role,
                    // refuse the login and surface a precise error so the screen
                    // can show "this number is registered as <role>" toast and
                    // sign the user back out.
                    val existingRole = resolution.roleForFcm.uppercase()
                    if (resolution.userData != null &&
                        existingRole.isNotBlank() &&
                        existingRole != role.name.uppercase()
                    ) {
                        runCatching { auth.signOut() }
                        return@fold Result.failure(
                            IllegalStateException("phone-already-registered-as:$existingRole")
                        )
                    }

                    resolution.userData?.let { userData ->
                        cacheResolvedUser(userData, role)
                        runCatching { fcmTokenManager.registerTokenWithRole(resolution.roleForFcm) }
                    }

                    Result.success(
                        PostOtpNavigation(
                            destination = if (resolution.shouldRouteToProfileSetup) {
                                PostOtpDestination.PROFILE_SETUP
                            } else {
                                PostOtpDestination.HOME
                            },
                            role = role
                        )
                    )
                },
                onFailure = { Result.failure(it) }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resendOtp(phoneNumber: String, context: Context) {
        // CRITICAL: Enforce 60-second cooldown to prevent rate limiting
        if (_resendCooldownSeconds.value > 0) {
            Timber.w("âš ï¸ Resend blocked - cooldown active: ${_resendCooldownSeconds.value}s remaining")
            _otpState.value = _otpState.value.copy(
                error = "Please wait ${_resendCooldownSeconds.value} seconds before resending"
            )
            return
        }
        
        viewModelScope.launch {
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            
            // Track OTP resend attempt for crash investigation
            errorHandler.logBreadcrumb("OTP resend started: $phoneNumber")

            // Apr 2026: same fail-closed role pre-check as sendOtp. Without
            // this a user could re-trigger OTP for a phone registered under
            // the other role just by tapping "Resend" — wasting an SMS and
            // making the conflict surface only after the user types the code.
            runCatching {
                FirestoreUtils.checkPhoneForRole(
                    phoneNumber = phoneNumber,
                    requestedRole = pendingRole.name
                )
            }.onSuccess { phoneCheck ->
                if (phoneCheck.exists == FirestoreUtils.PhoneExistenceResult.EXISTS &&
                    phoneCheck.roleConflict
                ) {
                    val existingRole = phoneCheck.existingRole?.lowercase() ?: "different role"
                    Timber.w("🔒 Resend blocked — phone $phoneNumber already a $existingRole")
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        otpSent = false,
                        error = "phone-already-registered-as:$existingRole",
                        message = "This number is already registered as a $existingRole. Please log in as a $existingRole."
                    )
                    errorHandler.logEvent("otp_resend_blocked_role_conflict", true)
                    return@launch
                }
            }.onFailure { err ->
                Timber.w(err, "🔒 Resend blocked — role pre-check failed for $phoneNumber")
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    otpSent = false,
                    error = "phone-precheck-failed",
                    message = "Could not verify this number right now. Please try again in a moment."
                )
                errorHandler.logEvent("otp_resend_blocked_precheck_failed", true)
                return@launch
            }
            
            // Start 60-second cooldown timer
            startResendCooldown()
            

            try {
                // Get activity from context (required for PhoneAuthProvider)
                val activity = context as? android.app.Activity
                if (activity == null) {
                    Timber.e("âŒ No Activity context available for OTP resend")
                    errorHandler.logBreadcrumb("OTP resend failed: No Activity context")
                    errorHandler.logEvent("otp_resend_no_activity", true)
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Activity context required for phone authentication"
                    )
                    return@launch
                }

                configureDebugRecaptchaFallback()
                
                val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            Timber.i("Phone verification completed automatically (resend)")
                            errorHandler.logBreadcrumb("OTP resend: Auto-verification completed")
                            signInWithPhoneAuthCredential(credential, context)
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            Timber.e(e, "Phone verification failed (resend)")
                            Timber.e("Exception class: ${e::class.simpleName}")
                            Timber.e("âŒ OTP resend verification failed: ${e.message}")
                            errorHandler.logBreadcrumb("OTP resend verification failed: ${e::class.simpleName}")
                            errorHandler.logEvent("otp_resend_failed", e.message ?: "unknown")

                            if (isBillingNotEnabledError(e.message)) {
                                errorHandler.logEvent("otp_billing_not_enabled", true)
                                errorHandler.logBreadcrumb("OTP resend blocked: Firebase phone auth billing not enabled")
                            }

                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                error = mapPhoneAuthError(e),
                                message = if (isBillingNotEnabledError(e.message)) {
                                    "Admin action required: enable billing on Firebase project 'dutype-860ac' to resend OTP."
                                } else {
                                    "SMS verification encountered an issue. Please retry in a moment."
                                }
                            )
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            Timber.i("OTP code resent successfully")
                            errorHandler.logBreadcrumb("OTP resend: Code sent successfully to $phoneNumber")
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
                        message = if (isBillingNotEnabledError(e.message)) {
                            "Admin action required: enable billing on Firebase project 'dutype-860ac' to resend OTP."
                        } else {
                            "SMS verification is temporarily unavailable. Please retry."
                        }
                    )
            }
        }
    }

    /**
     * Force classic reCAPTCHA v2 flow in debug builds to avoid Play Integrity-only issues
     * on local testing devices. Reflection keeps compatibility across Auth SDK versions.
     */
    private fun configureDebugRecaptchaFallback() {
        if (!BuildConfig.DEBUG) return

        runCatching {
            val settings = auth.firebaseAuthSettings
            val method = settings.javaClass.getMethod("forceRecaptchaFlowForTesting", Boolean::class.javaPrimitiveType)
            method.invoke(settings, true)
            Timber.d("OTP DEBUG: forceRecaptchaFlowForTesting enabled")
        }.onFailure { e ->
            Timber.d("OTP DEBUG: forceRecaptchaFlowForTesting not available: ${e.message}")
        }
    }

    private fun isBillingNotEnabledError(message: String?): Boolean {
        val safeMessage = message ?: return false
        return safeMessage.contains("BILLING_NOT_ENABLED", ignoreCase = true) ||
            safeMessage.contains("17499", ignoreCase = true)
    }
    
    /**
     * Start 60-second cooldown timer for resend button
     * Prevents spam and reduces Firebase rate limiting
     */
    private fun startResendCooldown() {
        viewModelScope.launch {
            _resendCooldownSeconds.value = 60
            while (_resendCooldownSeconds.value > 0) {
                kotlinx.coroutines.delay(1000)
                _resendCooldownSeconds.value -= 1
            }
        }
    }

        /**
         * Map known phone-auth exceptions to friendly messages.
         * Handles specific Play Integrity API errors that prevent SMS auto-retrieval.
         * 
         * CRITICAL: If you see "app not Recognized by Play Store" error on Play Store version:
         * â†’ App needs 24-48 hours to be recognized by Google Play Store
         * â†’ Works locally because debug apps bypass Play Integrity checks
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
                
                // Too many attempts - ENHANCED MESSAGE with device-specific guidance
                msg.contains("too many", ignoreCase = true) ||
                msg.contains("TOO_MANY_REQUESTS", ignoreCase = true) ||
                msg.contains("blocked", ignoreCase = true) ||
                msg.contains("unusual activity", ignoreCase = true) ||
                msg.contains("rate limit", ignoreCase = true) ->
                    "This phone number has been temporarily blocked on this device due to multiple verification attempts. Please try: 1) Wait 1-2 hours, 2) Try from a different device, or 3) Use a different phone number. This is a security measure by our verification provider."
                
                // Play Store recognition delay (MOST COMMON - works locally but not on Play Store)
                msg.contains("app not Recognized by Play Store", ignoreCase = true) ||
                msg.contains("18002", ignoreCase = true) ->
                    "App recognition pending. Please wait 24-48 hours after installation for Play Store to recognize your app."
                
                // Play Integrity API errors (status codes 17028)
                msg.contains("17028", ignoreCase = true) || 
                msg.contains("Play Integrity", ignoreCase = true) ->
                    "SMS verification temporarily unavailable. Please enter OTP manually."
                    
                msg.contains("BILLING_NOT_ENABLED", ignoreCase = true) ||
                msg.contains("17499", ignoreCase = true) ->
                    "Phone verification failed due to Firebase billing configuration. Enable billing on project 'dutype-860ac' and try again."
                    
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
