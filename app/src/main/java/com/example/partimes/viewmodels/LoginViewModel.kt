package com.example.partimes.viewmodels

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

sealed class OtpUiState {
    object Idle : OtpUiState()
    object Loading : OtpUiState()
    data class OtpSent(val message: String) : OtpUiState() // Message for user, e.g., "OTP Sent"
    data class Error(val message: String) : OtpUiState()
    object LoggedIn : OtpUiState()
}

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _otpUiState = MutableLiveData<OtpUiState>(OtpUiState.Idle)
    val otpUiState: LiveData<OtpUiState> = _otpUiState

    private var currentVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _otpUiState.value = OtpUiState.Loading

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _otpUiState.value = OtpUiState.Error("Verification failed: ${e.message}")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number,
                // we now need to ask the user to enter the code and then construct a
                // credential by combining the code with a verification ID.
                currentVerificationId = verificationId
                resendToken = token
                _otpUiState.value = OtpUiState.OtpSent("OTP has been sent to $phoneNumber")
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(activity) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(otpCode: String) {
        val storedVerificationId = currentVerificationId
        if (storedVerificationId.isNullOrEmpty()) {
            _otpUiState.value = OtpUiState.Error("Verification ID is missing. Please try sending OTP again.")
            return
        }
        _otpUiState.value = OtpUiState.Loading
        val credential = PhoneAuthProvider.getCredential(storedVerificationId, otpCode)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    _otpUiState.value = OtpUiState.LoggedIn
                } else {
                    // If sign in fails, display a message to the user.
                    _otpUiState.value = OtpUiState.Error("OTP Verification Failed: ${task.exception?.message}")
                }
            }
    }

    fun resendOtp(phoneNumber: String, activity: Activity) {
        val currentToken = resendToken
        if (currentToken == null || phoneNumber.isEmpty()) {
            _otpUiState.value = OtpUiState.Error("Cannot resend OTP at this moment.")
            // Optionally, you could try sending a new OTP from scratch if resendToken is null
            // sendOtp(phoneNumber, activity)
            return
        }
        _otpUiState.value = OtpUiState.Loading // Indicate loading for resend

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                _otpUiState.value = OtpUiState.Error("Resend OTP failed: ${e.message}")
            }
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                currentVerificationId = verificationId
                resendToken = token // Update the resend token
                _otpUiState.value = OtpUiState.OtpSent("New OTP has been sent to $phoneNumber")
            }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(activity) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .setForceResendingToken(currentToken) // Pass the resend token
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun resetLoginStateToIdle() {
        _otpUiState.value = OtpUiState.Idle
    }
}
