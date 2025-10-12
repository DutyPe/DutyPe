package com.example.partimes.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.partimes.auth.AuthManager
import com.example.partimes.models.User
import com.example.partimes.models.UserRole
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
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(context as android.app.Activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            // Auto-verification completed
                            signInWithPhoneAuthCredential(credential, context)
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                error = e.message ?: "Verification failed"
                            )
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            storedVerificationId = verificationId
                            resendToken = token
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                otpSent = true,
                                message = "OTP sent to $phoneNumber"
                            )
                        }
                    })
                    .build()
                
                PhoneAuthProvider.verifyPhoneNumber(options)
            } catch (e: Exception) {
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send OTP"
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
                    error = e.message ?: "Authentication failed"
                )
            }
        }
    }

    fun resendOtp(phoneNumber: String, context: Context) {
        viewModelScope.launch {
            _otpState.value = _otpState.value.copy(isLoading = true, error = null)
            
            try {
                val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(context as android.app.Activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            signInWithPhoneAuthCredential(credential, context)
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                error = e.message ?: "Verification failed"
                            )
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            storedVerificationId = verificationId
                            resendToken = token
                            _otpState.value = _otpState.value.copy(
                                isLoading = false,
                                otpSent = true,
                                message = "OTP resent to $phoneNumber"
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
                _otpState.value = _otpState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to resend OTP"
                )
            }
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
