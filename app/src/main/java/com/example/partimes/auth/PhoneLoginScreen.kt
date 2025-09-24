package com.example.partimes.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.partimes.R
import com.example.partimes.navigation.Routes
import com.example.partimes.viewmodels.OtpViewModel

@Composable
fun PhoneLoginScreen(
    navController: NavController,
    otpViewModel: OtpViewModel = viewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    val selectedCountryCode = "+91" // Fixed country code for India

    val context = LocalContext.current
    val otpState by otpViewModel.otpState.collectAsState()

    LaunchedEffect(otpState) {
        if (otpState.otpVerified) {
            // Navigate to role selection after OTP verification
            navController.navigate(Routes.SELECT_ROLE) {
                popUpTo(Routes.LOGIN_BOTTOM_SHEET) { 
                    inclusive = true 
                }
            }
            otpViewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFF8FAFC)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))
                
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.parttimes),
                    contentDescription = "ParTimes Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // App Name
                Text(
                    text = "ParTimes",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = Color(0xFF1F2937)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Work • Earn • Grow",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = Color(0xFF6B7280)
                    )
                )
                
                Spacer(modifier = Modifier.height(60.dp))
                
                // Main Content Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    when {
                        !otpState.otpSent -> {
                            // Phone number input screen
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Welcome Back",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    color = Color(0xFF1F2937),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Enter your mobile number to continue",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp
                                    ),
                                    color = Color(0xFF6B7280),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(32.dp))

                                // Phone input field
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() } && newValue.length <= 10) {
                                            phoneNumber = newValue
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            "Enter mobile number",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 16.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Text(
                                            text = "+91",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            ),
                                            color = Color(0xFF3B82F6)
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF3B82F6),
                                        unfocusedBorderColor = Color(0xFFE5E7EB),
                                        cursorColor = Color(0xFF3B82F6),
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone
                                    ),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                // Continue button
                                val buttonEnabled = phoneNumber.length == 10 && !otpState.isLoading
                                
                                Button(
                                    onClick = {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        otpViewModel.sendOtp(fullPhoneNumber, context)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3B82F6),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFF9CA3AF),
                                        disabledContentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = buttonEnabled
                                ) {
                                    if (otpState.isLoading) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Sending OTP...",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Continue",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                
                                // Error message
                                if (otpState.error != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = otpState.error ?: "",
                                        color = Color(0xFFEF4444),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        otpState.otpSent -> {
                            // OTP verification screen
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Verify OTP",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    color = Color(0xFF1F2937),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "We've sent a 6-digit code to",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    color = Color(0xFF6B7280),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "$selectedCountryCode $phoneNumber",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color(0xFF1F2937),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(32.dp))

                                // OTP Input Field
                                OutlinedTextField(
                                    value = otpValue,
                                    onValueChange = { newValue: String ->
                                        if (newValue.all { it.isDigit() } && newValue.length <= 6) {
                                            otpValue = newValue
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    placeholder = {
                                        Text(
                                            "Enter 6-digit OTP",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 16.sp
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF3B82F6),
                                        unfocusedBorderColor = Color(0xFFE5E7EB),
                                        cursorColor = Color(0xFF3B82F6),
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                                        textAlign = TextAlign.Center,
                                        letterSpacing = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                // Verify button
                                val otpButtonEnabled = otpValue.length == 6 && !otpState.isLoading
                                
                                Button(
                                    onClick = {
                                        otpViewModel.verifyOtp(otpValue, context)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3B82F6),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFF9CA3AF),
                                        disabledContentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = otpButtonEnabled
                                ) {
                                    if (otpState.isLoading) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Verifying...",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Verify OTP",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Resend OTP
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Didn't receive OTP? ",
                                        color = Color(0xFF6B7280),
                                        fontSize = 14.sp
                                    )
                                    TextButton(
                                        onClick = {
                                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                                            otpViewModel.resendOtp(fullPhoneNumber, context)
                                        },
                                        enabled = !otpState.isLoading
                                    ) {
                                        Text(
                                            text = "Resend",
                                            color = Color(0xFF3B82F6),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                
                                // Error message
                                if (otpState.error != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = otpState.error ?: "",
                                        color = Color(0xFFEF4444),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
