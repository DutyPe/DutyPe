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
import java.util.concurrent.TimeUnit

class OtpViewModel : ViewModel() {

    private val _otpState = MutableStateFlow(OtpState())
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(phoneNumber: String, context: Context) {
        viewModelScope.launch {
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            
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
                                _otpState.value = _otpState.value.copy(
                                    isLoading = false,
                                    error = mapPhoneAuthError(e)
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
                                message = "Please enter the OTP manually"
                            )
                        }
                    })
                    .build()
                
                PhoneAuthProvider.verifyPhoneNumber(options)
            } catch (e: Exception) {
                println("❌ Exception sending OTP: ${e.message}")
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = mapPhoneAuthError(e)
                )
            }
        }
    }

    fun verifyOtp(otp: String, context: Context) {
        viewModelScope.launch {
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            
            try {
                val verificationId = storedVerificationId
                       if (verificationId != null) {
                           val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                           signInWithPhoneAuthCredential(credential, context)
                       } else {
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Verification ID not found"
                    )
                }
            } catch (e: Exception) {
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to verify OTP"
                )
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, context: Context) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val firebaseUser = result.user
                
                if (firebaseUser != null) {
                    val phoneNumber = firebaseUser.phoneNumber ?: ""
                    
                    // Create user object from Firebase data only
                    val user = User(
                        id = firebaseUser.uid, // Use Firebase UID as user ID
                        email = "", // No email for phone-only auth
                        fullName = "", // No default name - user must provide
                        role = UserRole.WORKER, // Default role
                        isVerified = true // Phone number is verified by Firebase
                    )
                    
                    // Save user to AuthManager (no backend call needed for basic auth)
                    val authManager = AuthManager(context)
                    authManager.saveUser(user)
                    authManager.setLoggedIn(true)
                    
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        otpVerified = true,
                        message = "Phone authentication successful"
                    )
                } else {
                    _otpState.value = _otpState.value.copy(
                        isLoading = false,
                        error = "Authentication failed - no user data"
                    )
                }
            } catch (e: Exception) {
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = mapPhoneAuthError(e)
                )
            }
        }
    }

    fun resendOtp(phoneNumber: String, context: Context) {
        viewModelScope.launch {
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            
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
                
                val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            println("✅ Phone verification completed automatically (resend)")
                            signInWithPhoneAuthCredential(credential, context)
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            println("❌ Phone verification failed (resend): ${e.message}")
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                error = mapPhoneAuthError(e)
                            )
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            println("📲 OTP code resent successfully")
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
                                message = "Please enter the OTP manually"
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
                        error = mapPhoneAuthError(e)
                    )
            }
        }
    }

        /**
         * Map known phone-auth exceptions to friendly messages.
         */
        private fun mapPhoneAuthError(e: Exception): String {
            val msg = e.message ?: "Verification failed"
            return when {
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
    val message: String? = null
)
