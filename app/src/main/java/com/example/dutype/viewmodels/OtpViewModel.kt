package com.example.dutype.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.User
import com.example.dutype.models.UserRole
import com.example.dutype.services.AuthFlowService
import com.example.dutype.services.FCMTokenManager
import com.example.dutype.utils.FirestoreUtils
import com.example.dutype.utils.findActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.TimeUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.content.Context

/** Callable results are JSON objects: read them as String-keyed maps. */
@Suppress("UNCHECKED_CAST")
private fun Any?.asStringMap(): Map<String, Any?>? = this as? Map<String, Any?>

/** How the current login code was sent. */
enum class OtpChannel { SMS, WHATSAPP }

/** What Truecaller one-tap returned for this number (the server checked the role). */
data class TruecallerSignInResult(
    val allowed: Boolean,
    val phone: String,
    val name: String,
    val email: String,
    val existingRole: String?,
    val token: String?,
    val isNewUser: Boolean = false,
    val roleConflict: Boolean = false
)

/**
 * OtpViewModel - Handles phone authentication.
 *
 * Codes go on WhatsApp first (sendWhatsappOtp, ~₹0.12 each); Firebase SMS (~₹6.7 each) is used
 * when WhatsApp is not available and as the "Get code by SMS" backup. WhatsApp codes and Truecaller
 * sign in with a Firebase custom token for the same phone account, so everything after login is
 * the same as SMS login.
 *
 * - Injects AuthManager singleton instead of creating a new instance.
 * - Uses FirestoreUtils.getUserByUid() for profile checks (canonical implementation).
 */
@HiltViewModel
class OtpViewModel @Inject constructor(
    private val fcmTokenManager: FCMTokenManager,
    private val authManager: AuthManager,
    private val authFlowService: AuthFlowService,
    private val performanceTracker: com.example.dutype.performance.PerformanceTracker,
    private val errorHandler: com.example.dutype.core.error.ErrorHandler,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private companion object {
        /** Bound for the pre-OTP phone/role check so it can never stall the SMS request. */
        const val PRECHECK_TIMEOUT_MS = 3_000L

        /** Bound for the WhatsApp send; past it the SMS is sent instead. */
        const val WHATSAPP_SEND_TIMEOUT_MS = 8_000L

        /** How long the user waits on WhatsApp before "Get code by SMS" is offered. */
        const val SMS_FALLBACK_AFTER_SECONDS = 30

        /**
         * Process-level scope for post-login housekeeping (FCM token registration etc.).
         * It must outlive this ViewModel, which is cleared as soon as navigation happens.
         */
        val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    /**
     * OTP speed-up: while the user is reading/typing the OTP, open the Firestore
     * connection with a tiny public read (app_config allows `get` without auth).
     * The first post-login reads (phoneRoles + role profiles) then skip the
     * cold-channel setup, which was most of the wait after "Verify".
     */
    private fun warmUpFirestore() {
        backgroundScope.launch {
            runCatching {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection(com.example.dutype.firestore.FirestoreSchema.AppConfig.COLLECTION)
                    .document("app_update")
                    .get(com.google.firebase.firestore.Source.SERVER)
                    .await()
            }.onFailure { Timber.d(it, "Firestore warm-up skipped") }
        }
    }

    /** Fire-and-forget FCM registration: never blocks navigation after login/registration. */
    private fun registerFcmInBackground(role: String) {
        backgroundScope.launch {
            try {
                fcmTokenManager.registerTokenWithRole(role)
            } catch (e: Exception) {
                Timber.w(e, "Background FCM registration failed")
            }
        }
    }

    private val _otpState = MutableStateFlow(OtpState())
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    // Resend cooldown timer - prevents spam and reduces rate limiting
    private val _resendCooldownSeconds = MutableStateFlow(0)
    val resendCooldownSeconds: StateFlow<Int> = _resendCooldownSeconds.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    /** The number a WhatsApp code was sent to (verifyWhatsappOtp needs it). */
    private var whatsappPhone: String? = null
    private var cooldownJob: Job? = null

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
            // UNKNOWN or transient pre-check failures must not block Firebase
            // PhoneAuth; the canonical account checks run after OTP sign-in.
            // Normally a free cache hit (the screen just ran the same check). Bounded so a
            // slow network can never delay the SMS request itself.
            runCatching {
                withTimeoutOrNull(PRECHECK_TIMEOUT_MS) {
                    FirestoreUtils.checkPhoneForRole(
                        phoneNumber = phoneNumber,
                        requestedRole = pendingRole.name
                    )
                }
            }.onSuccess { phoneCheck ->
                if (phoneCheck != null && phoneCheck.exists == FirestoreUtils.PhoneExistenceResult.EXISTS &&
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
                Timber.w(err, "📱 OTP pre-send role check failed; continuing to PhoneAuth for $phoneNumber")
                errorHandler.logEvent("otp_send_precheck_failed_continuing", true)
            }

            // WhatsApp first; SMS only when WhatsApp is not available.
            if (trySendWhatsapp(phoneNumber)) return@launch

            // Start 60-second cooldown timer for initial OTP send
            startResendCooldown()


            try {
                // Get activity from context (required for PhoneAuthProvider). Compose can
                // provide a themed ContextWrapper in release, so unwrap it safely.
                val activity = context.findActivity()
                if (activity == null) {
                    val duration = System.currentTimeMillis() - startTime
                    performanceTracker.trackApiCall("send_otp", duration, success = false)

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
                    // Device attestation is handled by Firebase Auth via Play Integrity
                    // (backed by the App Check Play Integrity provider installed in DutyPeApplication).
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

                                Timber.e("❌ OTP verification failed: ${e.message}")
                                Timber.e("Exception: ${e::class.simpleName} - $e")

                                // Log to crash reports for Play Console
                                errorHandler.logEvent("otp_verification_failed", e.message ?: "unknown")
                                errorHandler.logBreadcrumb("OTP verification failed: ${e::class.simpleName}")

                                // Specific error handling for Play Store app recognition delay
                                if (e.message?.contains("app not Recognized", ignoreCase = true) == true) {
                                    Timber.w("⚠️ CRITICAL: App not recognized by Play Store yet!")
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
                            warmUpFirestore()
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                otpSent = true,
                                message = "OTP sent to $phoneNumber"
                            )
                        }

                        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                            // Auto-retrieval timeout - always keep otpSent=true so UI never goes blank/crashes
                            Timber.i("Auto-retrieval timeout - manual entry mode")
                            storedVerificationId = verificationId
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                otpSent = true,       // CRITICAL: must stay true to keep OTP screen visible
                                smsRetrieverActive = false,
                                isAutoVerifying = false,
                                message = "Didn't receive OTP? Enter it manually below."
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
                if (_otpState.value.channel == OtpChannel.WHATSAPP) {
                    verifyWhatsappCode(otp)
                    return@launch
                }
                val verificationId = storedVerificationId
                       if (verificationId != null) {
                           errorHandler.logBreadcrumb("OTP verification ID available - proceeding")
                           val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                           signInWithPhoneAuthCredential(credential, context)
                       } else {
                    Timber.e("❌ Verification ID not found for OTP verification")
                    errorHandler.logBreadcrumb("OTP verification failed: No verification ID stored")
                    errorHandler.logEvent("otp_verify_no_verification_id", true)
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Verification ID not found"
                    )
                }
            } catch (e: Exception) {
                Timber.e("❌ OTP verification exception: ${e.message}")
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
                onFirebaseSignedIn(result.user)
            } catch (e: Exception) {
                Timber.e("❌ Phone auth credential sign-in failed: ${e.message}")
                errorHandler.logBreadcrumb("Phone credential sign-in error: ${e::class.simpleName}")
                errorHandler.logEvent("phone_credential_signin_error", e.message ?: "unknown")
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = mapPhoneAuthError(e)
                )
            }
        }
    }

    /** After any successful Firebase sign-in (SMS credential, WhatsApp code or Truecaller). */
    private fun onFirebaseSignedIn(firebaseUser: com.google.firebase.auth.FirebaseUser?) {
        if (firebaseUser != null) {
            val phoneNumber = firebaseUser.phoneNumber ?: whatsappPhone.orEmpty()
            val userId = firebaseUser.uid
            errorHandler.logBreadcrumb("Firebase sign-in successful: $phoneNumber")

            // PERF: do NOT read the profile docs here. completeLogin()/completeRegistration()
            // (called immediately after otpVerified) resolve role + profile with one
            // concurrent read and re-cache the user, so an extra round trip here only
            // delayed the UI. Cache a minimal user now; it is refined right after.
            val user = User(
                id = userId,
                fullName = "",
                phone = phoneNumber,
                role = pendingRole,
                profileImageUrl = null
            )

            authManager.saveUser(user)
            authManager.setLoggedIn(true)

            Timber.i("User authenticated successfully: $userId")
            errorHandler.logBreadcrumb("User saved to AuthManager - Authentication complete")
            errorHandler.setUserInfo(userId, phoneNumber)
            // FCM registration happens (in the background) from completeLogin /
            // completeRegistration once the role is resolved.

            // Keep the spinner on: the screen still resolves the account
            // (completeLogin / completeRegistration) before navigating, and a
            // spinner-less gap made verification look stuck. resetState() clears it.
            _otpState.value = _otpState.value.copy(
                isLoading = true,
                otpVerified = true,
                message = "Phone authentication successful",
                phoneNumber = firebaseUser.phoneNumber ?: whatsappPhone  // Store phone number
            )
        } else {
            Timber.e("❌ Authentication succeeded but no user returned")
            errorHandler.logBreadcrumb("Sign-in failed: No Firebase user returned")
            errorHandler.logEvent("sign_in_no_user", true)
            _otpState.value = _otpState.value.copy(
                isLoading = false,
                error = "Authentication failed - no user data"
            )
        }
    }

    /**
     * Sends the code on WhatsApp. True when handled (sent, or the user must wait for a code already
     * sent / is refused); false means "send an SMS instead".
     */
    private suspend fun trySendWhatsapp(phoneNumber: String): Boolean {
        val response = try {
            withTimeoutOrNull(WHATSAPP_SEND_TIMEOUT_MS) {
                functions.getHttpsCallable("sendWhatsappOtp")
                    .call(mapOf("phone" to phoneNumber, "role" to pendingRole.name))
                    .await().data.asStringMap()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            val code = (e as? FirebaseFunctionsException)?.code
            when {
                msg.startsWith("phone-already-registered-as:") -> {
                    val existingRole = msg.substringAfter(":").lowercase()
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        otpSent = false,
                        error = "phone-already-registered-as:$existingRole",
                        message = "This number is already registered as a $existingRole. Please log in as a $existingRole."
                    )
                    return true
                }
                code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> {
                    _otpState.value = _otpState.value.copy(isLoading = false, otpSent = false, error = msg)
                    return true
                }
                else -> {
                    // Not deployed yet, App Check, network: SMS still works.
                    Timber.w(e, "WhatsApp OTP unavailable; using SMS")
                    errorHandler.logEvent("whatsapp_otp_unavailable", true)
                    return false
                }
            }
        }
        if (response == null) return false

        val sent = response["sent"] == true
        val waitSeconds = (response["retryAfterSec"] as? Number)?.toInt()
        if (!sent && (response["channel"] != "whatsapp" || waitSeconds == null)) return false

        whatsappPhone = phoneNumber
        storedVerificationId = null
        errorHandler.logEvent(if (sent) "whatsapp_otp_sent" else "whatsapp_otp_wait", true)
        warmUpFirestore()
        _otpState.value = _otpState.value.copy(
            isLoading = false,
            otpSent = true,
            channel = OtpChannel.WHATSAPP,
            message = "Code sent on WhatsApp to $phoneNumber"
        )
        startResendCooldown(if (sent) SMS_FALLBACK_AFTER_SECONDS else maxOf(1, waitSeconds ?: SMS_FALLBACK_AFTER_SECONDS))
        return true
    }

    private suspend fun verifyWhatsappCode(otp: String) {
        val phone = whatsappPhone
        if (phone == null) {
            _otpState.value = _otpState.value.copy(isLoading = false, error = "Please request a new code.")
            return
        }
        try {
            val data = functions.getHttpsCallable("verifyWhatsappOtp")
                .call(mapOf("phone" to phone, "code" to otp))
                .await().data.asStringMap()
            val token = data?.get("token") as? String ?: error("No sign-in token")
            val result = auth.signInWithCustomToken(token).await()
            errorHandler.logEvent("whatsapp_otp_verified", true)
            onFirebaseSignedIn(result.user)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "WhatsApp code verification failed")
            errorHandler.logEvent("whatsapp_otp_verify_failed", e.message ?: "unknown")
            val message = (e as? FirebaseFunctionsException)?.message
                ?: "Verification failed. Please try again."
            _otpState.value = _otpState.value.copy(isLoading = false, error = message)
        }
    }

    var lastTruecallerResult: TruecallerSignInResult? = null
        private set

    /**
     * Truecaller one-tap: the server exchanges the authorization code for the verified number,
     * name and email, and returns a sign-in token only when [mode] ("unified", "login", "register")
     * is allowed for this number and the current role.
     */
    suspend fun truecallerExchange(authorizationCode: String, codeVerifier: String, mode: String = "unified"): Result<TruecallerSignInResult> =
        runCatching {
            val data = functions.getHttpsCallable("truecallerSignIn")
                .call(
                    mapOf(
                        "authorizationCode" to authorizationCode,
                        "codeVerifier" to codeVerifier,
                        "role" to pendingRole.name,
                        "mode" to mode
                    )
                ).await().data.asStringMap() ?: error("Empty response")
            TruecallerSignInResult(
                allowed = data["allowed"] == true,
                phone = data["phone"] as? String ?: "",
                name = data["name"] as? String ?: "",
                email = data["email"] as? String ?: "",
                existingRole = (data["existingRole"] as? String)?.uppercase(),
                token = data["token"] as? String,
                isNewUser = data["isNewUser"] == true,
                roleConflict = data["roleConflict"] == true
            )
        }.onFailure {
            Timber.w(it, "Truecaller sign-in failed")
            errorHandler.logEvent("truecaller_signin_failed", it.message ?: "unknown")
        }

    /** Signs in with the token from [truecallerExchange]; the screen then resolves login / sign-up as after OTP. */
    fun signInWithTruecaller(result: TruecallerSignInResult) {
        val token = result.token ?: return
        lastTruecallerResult = result
        viewModelScope.launch {
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            try {
                whatsappPhone = result.phone
                val signedIn = auth.signInWithCustomToken(token).await()
                errorHandler.logEvent("truecaller_signin_ok", true)
                onFirebaseSignedIn(signedIn.user)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Truecaller custom-token sign-in failed")
                _otpState.value = _otpState.value.copy(isLoading = false, error = "Truecaller sign-in failed. Please use OTP.")
            }
        }
    }

    private fun cacheResolvedUser(user: AuthFlowService.SessionUser, fallbackRole: UserRole) {
        try {
            val role = runCatching { UserRole.valueOf(user.role) }.getOrDefault(fallbackRole)
            authManager.saveUser(
                User(
                    id = user.uid,
                    fullName = user.name,
                    phone = user.phone,
                    role = role,
                    profileImageUrl = user.photoUrl
                )
            )
            authManager.setLoggedIn(true)
        } catch (e: Exception) {
            Timber.w(e, "Failed to cache resolved user")
        }
    }

    suspend fun completeRegistration(
        role: UserRole,
        fullName: String,
        referralCode: String?,
        employerType: String? = null
    ): Result<PostOtpNavigation> {
        return try {
            authFlowService.completeRegistration(
                requestedRole = role.name,
                name = fullName,
                referralCode = referralCode,
                employerType = employerType
            ).fold(
                onSuccess = { resolution ->
                    cacheResolvedUser(resolution.user, role)
                    registerFcmInBackground(role.name)
                    Result.success(PostOtpNavigation(PostOtpDestination.PROFILE_SETUP, role))
                },
                onFailure = {
                    // Registration was refused (e.g. number already registered with the other
                    // role): don't leave the user signed in to a half-created account.
                    runCatching { auth.signOut() }
                    Result.failure(it)
                }
            )
        } catch (e: Exception) {
            runCatching { auth.signOut() }
            Result.failure(e)
        }
    }

    /**
     * Unified auth resolver (modern 1-tap login + registration):
     * - If account exists for [role]: logs in and routes to HOME (or PROFILE_SETUP if profile is incomplete).
     * - If new user (account does not exist): auto-creates account under [role] using [fullName] and routes to PROFILE_SETUP.
     * - If registered under a different role: signs out and surfaces role conflict.
     */
    suspend fun resolveUnifiedAuth(
        role: UserRole,
        fullName: String? = null,
        referralCode: String? = null,
        employerType: String? = null
    ): Result<PostOtpNavigation> {
        return try {
            authFlowService.resolveLogin(role.name).fold(
                onSuccess = { resolution ->
                    cacheResolvedUser(resolution.user, role)
                    registerFcmInBackground(resolution.user.role)
                    Result.success(
                        PostOtpNavigation(
                            destination = if (resolution.shouldRouteToProfileSetup) {
                                PostOtpDestination.PROFILE_SETUP
                            } else {
                                PostOtpDestination.HOME
                            },
                            role = runCatching {
                                UserRole.valueOf(resolution.user.role)
                            }.getOrDefault(role)
                        )
                    )
                },
                onFailure = { error ->
                    val msg = error.message.orEmpty()
                    if (msg == "account-not-found") {
                        // Brand-new user: auto-register them seamlessly so user is never rejected
                        val nameToUse = fullName?.trim()?.takeIf { it.isNotBlank() } ?: "User"
                        completeRegistration(
                            role = role,
                            fullName = nameToUse,
                            referralCode = referralCode,
                            employerType = employerType
                        )
                    } else {
                        if (msg.startsWith("phone-already-registered-as:")) {
                            runCatching { authManager.logout() }
                        }
                        Result.failure(error)
                    }
                }
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
                    cacheResolvedUser(resolution.user, role)
                    registerFcmInBackground(resolution.user.role)

                    Result.success(
                        PostOtpNavigation(
                            destination = if (resolution.shouldRouteToProfileSetup) {
                                PostOtpDestination.PROFILE_SETUP
                            } else {
                                PostOtpDestination.HOME
                            },
                            role = runCatching {
                                UserRole.valueOf(resolution.user.role)
                            }.getOrDefault(role)
                        )
                    )
                },
                onFailure = { error ->
                    val msg = error.message.orEmpty()
                    if (msg.startsWith("phone-already-registered-as:")) {
                        runCatching { authManager.logout() }
                    }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resendOtp(phoneNumber: String, context: Context) {
        // CRITICAL: Enforce 60-second cooldown to prevent rate limiting
        if (_resendCooldownSeconds.value > 0) {
            Timber.w("⚠️ Resend blocked - cooldown active: ${_resendCooldownSeconds.value}s remaining")
            _otpState.value = _otpState.value.copy(
                error = "Please wait ${_resendCooldownSeconds.value} seconds before resending"
            )
            return
        }

        viewModelScope.launch {
            // From WhatsApp, "resend" is the SMS backup ("Didn't get it? Get code by SMS").
            if (_otpState.value.channel == OtpChannel.WHATSAPP) {
                _otpState.value = _otpState.value.copy(channel = OtpChannel.SMS)
                errorHandler.logEvent("whatsapp_otp_sms_fallback", true)
            }
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)

            // Track OTP resend attempt for crash investigation
            errorHandler.logBreadcrumb("OTP resend started: $phoneNumber")

            // Same defense-in-depth role pre-check as sendOtp. Definitive role
            // conflicts still block; UNKNOWN or transient failures continue to
            // PhoneAuth and are resolved after OTP sign-in.
            // Normally a free cache hit (the screen just ran the same check). Bounded so a
            // slow network can never delay the SMS request itself.
            runCatching {
                withTimeoutOrNull(PRECHECK_TIMEOUT_MS) {
                    FirestoreUtils.checkPhoneForRole(
                        phoneNumber = phoneNumber,
                        requestedRole = pendingRole.name
                    )
                }
            }.onSuccess { phoneCheck ->
                if (phoneCheck != null && phoneCheck.exists == FirestoreUtils.PhoneExistenceResult.EXISTS &&
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
                Timber.w(err, "📱 OTP resend role check failed; continuing to PhoneAuth for $phoneNumber")
                errorHandler.logEvent("otp_resend_precheck_failed_continuing", true)
            }

            // Start 60-second cooldown timer
            startResendCooldown()


            try {
                // Get activity from context (required for PhoneAuthProvider). Compose can
                // provide a themed ContextWrapper in release, so unwrap it safely.
                val activity = context.findActivity()
                if (activity == null) {
                    Timber.e("❌ No Activity context available for OTP resend")
                    errorHandler.logBreadcrumb("OTP resend failed: No Activity context")
                    errorHandler.logEvent("otp_resend_no_activity", true)
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
                            errorHandler.logBreadcrumb("OTP resend: Auto-verification completed")
                            signInWithPhoneAuthCredential(credential, context)
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            Timber.e(e, "Phone verification failed (resend)")
                            Timber.e("Exception class: ${e::class.simpleName}")
                            Timber.e("❌ OTP resend verification failed: ${e.message}")
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

    private fun isBillingNotEnabledError(message: String?): Boolean {
        val safeMessage = message ?: return false
        return safeMessage.contains("BILLING_NOT_ENABLED", ignoreCase = true) ||
            safeMessage.contains("17499", ignoreCase = true)
    }

    /**
     * Start 60-second cooldown timer for resend button
     * Prevents spam and reduces Firebase rate limiting
     */
    private fun startResendCooldown(seconds: Int = 60) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _resendCooldownSeconds.value = seconds
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
         * → App needs 24-48 hours to be recognized by Google Play Store
         * → Works locally because debug apps bypass Play Integrity checks
         */
        private fun mapPhoneAuthError(e: Exception): String {
            val msg = e.message ?: "Verification failed"
            return when {
                // Device could not be attested (Play Integrity unavailable / no Activity for the fallback flow)
                e::class.java.simpleName == "FirebaseAuthMissingActivityForRecaptchaException" ||
                msg.contains("MissingActivityForRecaptcha", ignoreCase = true) ->
                    "Couldn't verify this device. Update Google Play services / Play Store and try again."

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
                msg.contains("Play Integrity", ignoreCase = true) ||
                msg.contains("integrity", ignoreCase = true) ->
                    "Couldn't verify this device. Update Google Play services / Play Store and try again."

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
        whatsappPhone = null
        lastTruecallerResult = null
    }

    // ─────────────────────────────────────────────────────────────────────
    // SMS Auto-Retriever integration
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Called by the UI when SmsAutoRetrieverHelper successfully received and
     * extracted the 6-digit OTP from the incoming SMS.  We immediately:
     *   1. Show the code in the UI field (autoRetrievedOtp).
     *   2. Start auto-verification so the user never has to tap anything.
     */
    fun onSmsAutoRetrieved(otp: String, context: Context) {
        Timber.i("📱 SMS Auto-Retrieved OTP successfully – auto-submitting")
        errorHandler.logBreadcrumb("SMS auto-retrieval succeeded")
        errorHandler.logEvent("sms_auto_retrieved", true)

        _otpState.value = _otpState.value.copy(
            autoRetrievedOtp = otp,
            isAutoVerifying = true,
            smsRetrieverActive = false,
            error = null
        )
        // Immediately verify – same path as manual entry
        verifyOtp(otp, context)
    }

    /**
     * Marks that the SMS Retriever listener is now active.  Called by the
     * UI after SmsAutoRetrieverHelper.startListening() succeeds.
     */
    fun onSmsRetrieverStarted() {
        _otpState.value = _otpState.value.copy(smsRetrieverActive = true)
    }

    /**
     * Marks the SMS Retriever as no longer active (timeout / error / success).
     */
    fun onSmsRetrieverStopped() {
        _otpState.value = _otpState.value.copy(
            smsRetrieverActive = false,
            isAutoVerifying = false
        )
    }
}

data class OtpState(
    val isLoading: Boolean = false,
    val otpSent: Boolean = false,
    val otpVerified: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val phoneNumber: String? = null,  // Store phone number for profile setup
    // SMS Auto-Retriever state
    val smsRetrieverActive: Boolean = false,      // true while listening for SMS
    val autoRetrievedOtp: String? = null,          // non-null once auto-filled
    val isAutoVerifying: Boolean = false,          // true while auto-sign-in is in progress
    val channel: OtpChannel = OtpChannel.SMS       // where the current code was sent
)
