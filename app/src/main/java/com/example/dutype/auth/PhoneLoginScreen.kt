package com.example.dutype.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.dutype.R
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.OtpViewModel

@OptIn(ExperimentalAnimationApi::class)
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

    // Professional color scheme
    val primaryBlue = Color(0xFF2563EB)
    val darkBlue = Color(0xFF1E40AF)
    val lightBlue = Color(0xFFF0F7FF)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8FAFC),
            Color(0xFFE2E8F0)
        )
    )
    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White,
            Color(0xFFFBFCFE)
        )
    )

    // Animation states
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(72.dp))

                // Enhanced Logo Section with Professional Styling
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            ambientColor = primaryBlue.copy(alpha = 0.3f),
                            spotColor = primaryBlue.copy(alpha = 0.3f)
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    lightBlue
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = primaryBlue.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dutype),
                        contentDescription = "dutype Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .graphicsLayer {
                                scaleX = logoScale
                                scaleY = logoScale
                            },
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Enhanced Brand Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DutyPe",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(primaryBlue, darkBlue)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Work • Earn • Grow",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))

                // Professional Main Content Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color.Black.copy(alpha = 0.08f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardGradient)
                    ) {
                        AnimatedContent(
                            targetState = !otpState.otpSent,
                            transitionSpec = {
                                slideInHorizontally(
                                    initialOffsetX = { if (targetState) -300 else 300 },
                                    animationSpec = tween(400, easing = EaseOutCubic)
                                ) + fadeIn(animationSpec = tween(400)) with
                                slideOutHorizontally(
                                    targetOffsetX = { if (targetState) 300 else -300 },
                                    animationSpec = tween(400, easing = EaseInCubic)
                                ) + fadeOut(animationSpec = tween(400))
                            }
                        ) { isPhoneNumberScreen ->
                            if (isPhoneNumberScreen) {
                                // Enhanced Phone Number Input Screen
                                PhoneNumberInputSection(
                                    phoneNumber = phoneNumber,
                                    onPhoneNumberChange = { newValue ->
                                        if (newValue.all { it.isDigit() } && newValue.length <= 10) {
                                            phoneNumber = newValue
                                        }
                                    },
                                    selectedCountryCode = selectedCountryCode,
                                    otpState = otpState,
                                    onContinueClick = {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        otpViewModel.sendOtp(fullPhoneNumber, context)
                                    },
                                    primaryBlue = primaryBlue,
                                    darkBlue = darkBlue
                                )
                            } else {
                                // Enhanced OTP Verification Screen
                                OtpVerificationSection(
                                    otpValue = otpValue,
                                    onOtpChange = { newValue ->
                                        if (newValue.all { it.isDigit() } && newValue.length <= 6) {
                                            otpValue = newValue
                                        }
                                    },
                                    selectedCountryCode = selectedCountryCode,
                                    phoneNumber = phoneNumber,
                                    otpState = otpState,
                                    onVerifyClick = {
                                        otpViewModel.verifyOtp(otpValue, context)
                                    },
                                    onResendClick = {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        otpViewModel.resendOtp(fullPhoneNumber, context)
                                    },
                                    primaryBlue = primaryBlue,
                                    darkBlue = darkBlue
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Professional Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Secure • Fast • Reliable",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneNumberInputSection(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountryCode: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    onContinueClick: () -> Unit,
    primaryBlue: Color,
    darkBlue: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Professional Header Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = primaryBlue,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your mobile number to continue your journey",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Enhanced Phone Input Field
        Column {
            Text(
                text = "Mobile Number",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = Color(0xFF374151),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                placeholder = {
                    Text(
                        "Enter your 10-digit number",
                        color = Color(0xFF9CA3AF),
                        fontSize = 16.sp
                    )
                },
                leadingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "+91",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = primaryBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(Color(0xFFCBD5E1))
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    cursorColor = primaryBlue,
                    focusedContainerColor = primaryBlue.copy(alpha = 0.02f),
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                ),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Premium Continue Button
        val buttonEnabled = phoneNumber.length == 10 && !otpState.isLoading

        Button(
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (buttonEnabled) 8.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = primaryBlue.copy(alpha = 0.3f),
                    spotColor = primaryBlue.copy(alpha = 0.3f)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryBlue,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE5E7EB),
                disabledContentColor = Color(0xFF9CA3AF)
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = buttonEnabled
        ) {
            if (otpState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Sending OTP...",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Continue",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Enhanced Error Message
        AnimatedVisibility(
            visible = otpState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEF2F2)
                ),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = SolidColor(Color(0xFFFECACA))
                )
            ) {
                Text(
                    text = otpState.error ?: "",
                    color = Color(0xFFDC2626),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OtpVerificationSection(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    selectedCountryCode: String,
    phoneNumber: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    primaryBlue: Color,
    darkBlue: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp), // Reduced from 36.dp
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Professional Header Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = primaryBlue,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp)) // Reduced from 16.dp

            Text(
                text = "Verify OTP",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp)) // Reduced from 8.dp

            Text(
                text = "We've sent a 6-digit verification code to",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .background(
                        color = primaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "$selectedCountryCode $phoneNumber",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    ),
                    color = primaryBlue,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp)) // Reduced from 40.dp

        // Enhanced OTP Input Field
        Column {
            Text(
                text = "Verification Code",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = Color(0xFF374151),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = otpValue,
                onValueChange = onOtpChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp), // Reduced from 72.dp
                placeholder = {
                    Text(
                        "Enter 6-digit code",
                        color = Color(0xFF9CA3AF),
                        fontSize = 16.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    cursorColor = primaryBlue,
                    focusedContainerColor = primaryBlue.copy(alpha = 0.02f),
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                ),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp, // Reduced from 12.sp
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp // Reduced from 24.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp)) // Reduced from 40.dp

        // Premium Verify Button - Always Visible and Properly Sized
        val otpButtonEnabled = otpValue.length == 6 && !otpState.isLoading

        Button(
            onClick = onVerifyClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp) // Reduced from 64.dp to prevent compression
                .shadow(
                    elevation = if (otpButtonEnabled) 8.dp else 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = primaryBlue.copy(alpha = 0.3f),
                    spotColor = primaryBlue.copy(alpha = 0.3f)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (otpButtonEnabled) primaryBlue else Color(0xFF9CA3AF),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF9CA3AF),
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = otpButtonEnabled
        ) {
            if (otpState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp) // Reduced from 22.dp
                    )
                    Text(
                        text = "Verifying...",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Verify OTP",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp // Reduced from 18.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp) // Reduced from 20.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp)) // Reduced from 24.dp

        // Enhanced Resend Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8FAFC)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp), // Reduced from 16.dp
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Didn't receive the code? ",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                TextButton(
                    onClick = onResendClick,
                    enabled = !otpState.isLoading,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp) // Added to control button size
                ) {
                    Text(
                        text = "Resend OTP",
                        color = primaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Enhanced Error Message
        AnimatedVisibility(
            visible = otpState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEF2F2)
                ),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.dp,
                    brush = SolidColor(Color(0xFFEFCACA))
                )
            ) {
                Text(
                    text = otpState.error ?: "",
                    color = Color(0xFFDC2626),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}
