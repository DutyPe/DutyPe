package com.example.partimes.auth

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.partimes.R
import com.example.partimes.navigation.Routes
import com.example.partimes.viewmodels.LoginViewModel
import com.example.partimes.viewmodels.OtpUiState

@Composable
fun LoginScreen(
    navController: NavController,
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
            navController.navigate(Routes.LOCATION_SERVICE_SCREEN_ROUTE) {
                popUpTo(Routes.LOGIN_SIGNUP_ROUTE) { inclusive = true }
            }
            loginViewModel.resetLoginStateToIdle()
        }
    }

    // Professional gradient background (changed to black)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black, // Black
                        Color(0xFF1A1A1A), // Dark gray
                        Color(0xFF333333), // Medium gray
                        Color(0xFF1A1A1A)  // Dark gray
                    )
                )
            )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            backgroundColor = Color.Transparent,
            topBar = {
                if (otpUiState is OtpUiState.Idle || otpUiState is OtpUiState.Error) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInHorizontally() + fadeIn(),
                            exit = slideOutHorizontally() + fadeOut()
                        ) {
                            Button(
                                onClick = {
                                    navController.navigate(Routes.LOCATION_SERVICE_SCREEN_ROUTE) {
                                        popUpTo(Routes.LOGIN_SIGNUP_ROUTE) { inclusive = true }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White // Blue text
                                ),
                                elevation = ButtonDefaults.elevation(0.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "Skip",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                AnimatedContent(
                    targetState = otpUiState,
                    transitionSpec = {
                        slideInVertically { it } + fadeIn() togetherWith
                                slideOutVertically { -it } + fadeOut()
                    },
                    label = "bottom_bar_animation"
                ) { currentState ->
                    when (currentState) {
                        is OtpUiState.Idle, is OtpUiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0xFF1A1A1A).copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        append("By continuing, you agree to our ")
                                        withStyle(style = SpanStyle(
                                            color = Color(0xFF1976D2), // Blue links
                                            textDecoration = TextDecoration.Underline,
                                            fontWeight = FontWeight.Medium
                                        )) {
                                            append("Terms & Conditions")
                                        }
                                        append(" and ")
                                        withStyle(style = SpanStyle(
                                            color = Color(0xFF1976D2), // Blue links
                                            textDecoration = TextDecoration.Underline,
                                            fontWeight = FontWeight.Medium
                                        )) {
                                            append("Privacy Policy")
                                        }
                                    },
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                val buttonEnabled = phoneNumber.isNotBlank() && phoneNumber.length == 10 && otpUiState !is OtpUiState.Loading
                                val buttonScale by animateFloatAsState(
                                    targetValue = if (buttonEnabled) 1f else 0.95f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "button_scale"
                                )

                                Button(
                                    onClick = {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        loginViewModel.sendOtp(fullPhoneNumber, activity)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .graphicsLayer {
                                            scaleX = buttonScale
                                            scaleY = buttonScale
                                        },
                                    elevation = ButtonDefaults.elevation(0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF1976D2), // Blue button
                                        contentColor = Color.White,
                                        disabledBackgroundColor = Color(0xFF1976D2).copy(alpha = 0.3f),
                                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = buttonEnabled
                                ) {
                                    if (currentState is OtpUiState.Loading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "Continue",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                        is OtpUiState.OtpSent -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color(0xFF1A1A1A).copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
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
                                        backgroundColor = Color(0xFF1976D2), // Blue button
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
                            }
                        }
                        else -> { /* No bottom bar for other states */ }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(32.dp))

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
                                modifier = Modifier.padding(top = 60.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF1976D2), // Blue loading indicator
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Processing...",
                                    style = MaterialTheme.typography.body1,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is OtpUiState.Error -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFEB3B).copy(alpha = 0.1f))
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "⚠️",
                                    fontSize = 32.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Text(
                                    text = currentState.message,
                                    color = Color(0xFF1976D2), // Blue error text
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                Button(
                                    onClick = { loginViewModel.resetLoginStateToIdle() },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF1976D2) // Blue button
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = ButtonDefaults.elevation(0.dp)
                                ) {
                                    Text("Try Again", color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        is OtpUiState.Idle -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Company Logo and Branding Section
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(bottom = 48.dp)
                                ) {
                                    // Logo
                                    Image(
                                        painter = painterResource(id = R.drawable.parttimes),
                                        contentDescription = "ParTimes Logo",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(20.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Company Name
                                    Text(
                                        text = "ParTimes",
                                        style = MaterialTheme.typography.h4.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 32.sp
                                        ),
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Tagline
                                    Text(
                                        text = "Work • Earn • Grow",
                                        style = MaterialTheme.typography.subtitle1.copy(
                                            fontSize = 16.sp,
                                            letterSpacing = 2.sp
                                        ),
//                                        color = Color(0xFF1976D2), // Blue tagline
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = "Your Gateway to Flexible Employment",
                                        style = MaterialTheme.typography.body2.copy(
                                            fontSize = 14.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Login Section
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "Welcome Back!",
                                        style = MaterialTheme.typography.h5.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "Enter your mobile number to continue",
                                        style = MaterialTheme.typography.body1.copy(
                                            fontSize = 16.sp,
                                            lineHeight = 24.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(bottom = 32.dp)
                                    )

                                    // Enhanced phone input field
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        elevation = 0.dp,
                                        backgroundColor = Color.White.copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "+91",
                                                style = MaterialTheme.typography.body1.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                ),
                                                color = Color.White,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(24.dp)
                                                    .background(Color.White.copy(alpha = 0.3f))
                                            )
                                            
                                            OutlinedTextField(
                                                value = phoneNumber,
                                                onValueChange = { newValue ->
                                                    if (newValue.all { it.isDigit() } && newValue.length <= 10) {
                                                        phoneNumber = newValue
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 12.dp),
                                                label = null,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                singleLine = true,
                                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                                    focusedBorderColor = Color.Transparent,
                                                    unfocusedBorderColor = Color.Transparent,
                                                    backgroundColor = Color.Transparent,
                                                    cursorColor = Color(0xFF1976D2), // Blue cursor
                                                    textColor = Color.White
                                                ),
                                                textStyle = MaterialTheme.typography.body1.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 18.sp,
                                                    letterSpacing = 1.sp
                                                ),
                                                placeholder = {
                                                    Text(
                                                        "Enter mobile number",
                                                        color = Color.White.copy(alpha = 0.6f),
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Phone number format hint
                                    if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
                                        Text(
                                            text = "Please enter a 10-digit mobile number",
                                            color = Color(0xFFFFB74D),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    } else if (phoneNumber.length == 10) {
                                        Text(
                                            text = "✓ Valid mobile number",
                                            color = Color(0xFF4CAF50), // Green validation text
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        is OtpUiState.OtpSent -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Enhanced OTP Header with logo
                                Image(
                                    painter = painterResource(id = R.drawable.parttimes),
                                    contentDescription = "ParTimes Logo",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Fit
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Text(
                                    text = "Verification Code",
                                    style = MaterialTheme.typography.h4.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Text(
                                    text = "We've sent a 6-digit code to",
                                    style = MaterialTheme.typography.body1.copy(fontSize = 16.sp),
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${selectedCountryCode} ${phoneNumber}",
                                    style = MaterialTheme.typography.body1.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = Color(0xFF1976D2), // Blue phone number
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 40.dp)
                                )

                                // Enhanced OTP Input Field
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(64.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = 0.dp,
                                    backgroundColor = Color.White.copy(alpha = 0.1f)
                                ) {
                                    OutlinedTextField(
                                        value = otpValue,
                                        onValueChange = { newValue ->
                                            if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                                otpValue = newValue
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxSize(),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            backgroundColor = Color.Transparent,
                                            cursorColor = Color(0xFF1976D2), // Blue cursor
                                            textColor = Color.White
                                        ),
                                        textStyle = MaterialTheme.typography.h5.copy(
                                            textAlign = TextAlign.Center,
                                            letterSpacing = 6.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        ),
                                        placeholder = {
                                            Text(
                                                "• • • • • •",
                                                color = Color.White.copy(alpha = 0.5f),
                                                style = MaterialTheme.typography.h5,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center,
                                                letterSpacing = 6.sp,
                                                fontSize = 24.sp
                                            )
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                TextButton(
                                    onClick = {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        loginViewModel.resendOtp(fullPhoneNumber, activity)
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color(0xFF1976D2) // Blue text button
                                    )
                                ) {
                                    Text(
                                        "Didn't receive the code? Resend",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        is OtpUiState.LoggedIn -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 60.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.parttimes),
                                    contentDescription = "ParTimes Logo",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Welcome to ParTimes!",
                                    style = MaterialTheme.typography.h5.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Setting up your workspace...",
                                    style = MaterialTheme.typography.body1,
                                    color = Color(0xFF1976D2), // Blue text
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    MaterialTheme {
        LoginScreen(navController = rememberNavController())
    }
}