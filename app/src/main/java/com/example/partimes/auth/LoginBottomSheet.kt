package com.example.partimes.auth

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.partimes.R
import com.example.partimes.viewmodels.LoginViewModel
import com.example.partimes.viewmodels.OtpUiState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoginBottomSheet(
    onLoginSuccess: () -> Unit,
    onDismiss: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    val selectedCountryCode = "+91" // Fixed country code for India

    val context = LocalContext.current
    val activity = context as Activity
    val otpUiState by loginViewModel.otpUiState.observeAsState(OtpUiState.Idle)

    LaunchedEffect(otpUiState) {
        if (otpUiState is OtpUiState.LoggedIn) {
            onLoginSuccess()
            loginViewModel.resetLoginStateToIdle()
        }
    }

    // Background with same gradient as LocationService
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0066FF),
                        Color(0xFF004BD9),
                        Color(0xFFE3F2FD)
                    )
                )
            )
    ) {
        // Skip Button at the top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onDismiss() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.elevation(0.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "Skip",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f) // Increased height to 85%
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp), // Increased padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .width(50.dp) // Increased width
                        .height(5.dp) // Increased height
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(3.dp)
                        )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Logo and Title
                Image(
                    painter = painterResource(id = R.drawable.parttimes),
                    contentDescription = "ParTimes Logo",
                    modifier = Modifier
                        .size(90.dp) // Increased logo size
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedContent(
                    targetState = otpUiState,
                    transitionSpec = {
                        slideInHorizontally { if (targetState is OtpUiState.OtpSent) 300 else -300 } + fadeIn() togetherWith
                                slideOutHorizontally { if (targetState is OtpUiState.OtpSent) -300 else 300 } + fadeOut()
                    },
                    label = "content_animation"
                ) { currentState ->
                    when (currentState) {
                        is OtpUiState.Loading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 40.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF1976D2),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Processing...",
                                    style = MaterialTheme.typography.body1,
                                    color = Color.Black.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        is OtpUiState.Idle, is OtpUiState.Error -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Welcome to ParTimes",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Enter your phone number to continue",
                                    fontSize = 16.sp,
                                    color = Color.Black.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // Phone Number Input
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 10 && newValue.all { it.isDigit() }) {
                                            phoneNumber = newValue
                                        }
                                    },
                                    label = { Text("Phone Number") },
                                    placeholder = { Text("Enter 10-digit number") },
                                    leadingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 12.dp)
                                        ) {
                                            Text(
                                                selectedCountryCode,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.Black
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(24.dp)
                                                    .background(Color.Gray.copy(alpha = 0.3f))
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color(0xFF1976D2),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                        cursorColor = Color(0xFF1976D2)
                                    ),
                                    singleLine = true
                                )

                                if (currentState is OtpUiState.Error) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = currentState.message,
                                        color = Color.Red,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Continue Button
                                val buttonEnabled =
                                    phoneNumber.isNotBlank() && phoneNumber.length == 10 && otpUiState !is OtpUiState.Loading

                                Button(
                                    onClick = {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        loginViewModel.sendOtp(fullPhoneNumber, activity)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    elevation = ButtonDefaults.elevation(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF1976D2),
                                        contentColor = Color.White,
                                        disabledBackgroundColor = Color(0xFF1976D2).copy(alpha = 0.3f),
                                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = buttonEnabled
                                ) {
                                    Text(
                                        text = "Continue",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = buildAnnotatedString {
                                        append("By continuing, you agree to our ")
                                        withStyle(
                                            style = SpanStyle(
                                                color = Color(0xFF1976D2),
                                                textDecoration = TextDecoration.Underline,
                                                fontWeight = FontWeight.Medium
                                            )
                                        ) {
                                            append("Terms & Conditions")
                                        }
                                        append(" and ")
                                        withStyle(
                                            style = SpanStyle(
                                                color = Color(0xFF1976D2),
                                                textDecoration = TextDecoration.Underline,
                                                fontWeight = FontWeight.Medium
                                            )
                                        ) {
                                            append("Privacy Policy")
                                        }
                                    },
                                    fontSize = 12.sp,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        is OtpUiState.OtpSent -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Verify Your Number",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Enter the 6-digit code sent to $selectedCountryCode$phoneNumber",
                                    fontSize = 16.sp,
                                    color = Color.Black.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // OTP Input
                                OutlinedTextField(
                                    value = otpValue,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                            otpValue = newValue
                                        }
                                    },
                                    label = { Text("Enter OTP") },
                                    placeholder = { Text("6-digit code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color(0xFF1976D2),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                        cursorColor = Color(0xFF1976D2)
                                    ),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Verify Button
                                val buttonEnabled = otpValue.length == 6

                                Button(
                                    onClick = {
                                        loginViewModel.verifyOtp(otpValue)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    elevation = ButtonDefaults.elevation(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF1976D2),
                                        contentColor = Color.White,
                                        disabledBackgroundColor = Color(0xFF1976D2).copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = buttonEnabled
                                ) {
                                    Text(
                                        text = "Verify OTP",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Back to phone number
                                TextButton(
                                    onClick = {
                                        loginViewModel.resetLoginStateToIdle()
                                        otpValue = ""
                                    }
                                ) {
                                    Text(
                                        "Change Phone Number",
                                        color = Color(0xFF1976D2),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                // Resend OTP Button
                                TextButton(
                                    onClick = {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        loginViewModel.resendOtp(fullPhoneNumber, activity)
                                    },
                                    enabled = otpUiState !is OtpUiState.Loading
                                ) {
                                    Text(
                                        if (otpUiState is OtpUiState.Loading) "Sending..." else "Resend OTP",
                                        color = if (otpUiState is OtpUiState.Loading)
                                            Color.Gray else Color(0xFF1976D2),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                            }
                        }

                        else -> { /* Handle other states */
                        }
                    }
                }
            }
        }
    }
}
