package com.example.partimes.auth

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

private const val TAG = "OtpVerification"

@Composable
fun OtpVerificationScreen(navController: NavHostController, phoneNumber: String) {
    val otpCode = remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val verificationId = remember { mutableStateOf("") }
    val resendToken = remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as ComponentActivity) // Safe cast
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "onVerificationCompleted:$credential")
                    signInWithPhoneAuthCredential(auth, credential, navController, context)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.w(TAG, "onVerificationFailed", e)
                    val message = when (e) {
                        is FirebaseAuthInvalidCredentialsException -> "Invalid phone number."
                        is FirebaseTooManyRequestsException -> "Quota exceeded. Try again later."
                        is FirebaseAuthMissingActivityForRecaptchaException -> "Missing Activity for reCAPTCHA."
                        else -> "Verification failed: ${e.localizedMessage}"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verificationIdStr: String, token: PhoneAuthProvider.ForceResendingToken) {
                    Log.d(TAG, "onCodeSent:$verificationIdStr")
                    verificationId.value = verificationIdStr
                    resendToken.value = token
                    Toast.makeText(context, "OTP Sent!", Toast.LENGTH_SHORT).show()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Enter OTP", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = otpCode.value,
            onValueChange = { otpCode.value = it },
            label = { Text("OTP") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (verificationId.value.isNotEmpty() && otpCode.value.isNotEmpty()) {
                    val credential = PhoneAuthProvider.getCredential(verificationId.value, otpCode.value)
                    signInWithPhoneAuthCredential(auth, credential, navController, context)
                } else {
                    Toast.makeText(context, "Please enter OTP", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify OTP")
        }
    }
}

private fun signInWithPhoneAuthCredential(
    auth: FirebaseAuth,
    credential: PhoneAuthCredential,
    navController: NavHostController,
    context: android.content.Context
) {
    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "signInWithCredential:success")
                navController.navigate("select_role") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                Log.w(TAG, "signInWithCredential:failure", task.exception)
                val message = task.exception?.localizedMessage ?: "Authentication failed."
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
}
