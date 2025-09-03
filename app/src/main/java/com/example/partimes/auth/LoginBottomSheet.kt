package com.example.partimes.auth

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.partimes.R
import com.example.partimes.viewmodels.LoginViewModel
import com.example.partimes.viewmodels.OtpUiState
import com.example.partimes.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoginBottomSheetScreen(navController: NavHostController) {
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded,
        skipHalfExpanded = true,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    // Enhanced fade animation for background
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (bottomSheetState.targetValue == ModalBottomSheetValue.Expanded) 0.6f else 0.3f,
        animationSpec = tween(300),
        label = "background_alpha"
    )

    // Handle back press and sheet state changes
    LaunchedEffect(bottomSheetState.targetValue) {
        if (bottomSheetState.targetValue == ModalBottomSheetValue.Hidden) {
            navController.navigate(Routes.LOCATION_SERVICE_SCREEN_ROUTE) {
                popUpTo("login_bottom_sheet") { inclusive = true }
            }
        }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            LoginBottomSheet(
                onLoginSuccess = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        bottomSheetState.hide()
                    }
                    navController.navigate(Routes.LOCATION_SERVICE_SCREEN_ROUTE) {
                        popUpTo("login_bottom_sheet") { inclusive = true }
                    }
                },
                onDismiss = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch {
                        bottomSheetState.hide()
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        // Enhanced background with animated overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = backgroundAlpha)
                )
        ) {
            // Enhanced skip button with better styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate(Routes.LOCATION_SERVICE_SCREEN_ROUTE) {
                            popUpTo("login_bottom_sheet") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White.copy(alpha = 0.95f),
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.elevation(6.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Skip",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            // Enhanced branding section with animations
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Animated title
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
                        animationSpec = tween(800),
                        initialOffsetY = { -100 }
                    )
                ) {
                    Text(
                        text = "ParTimes",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Animated subtitle
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(1000, delayMillis = 200)) + slideInVertically(
                        animationSpec = tween(1000, delayMillis = 200),
                        initialOffsetY = { 50 }
                    )
                ) {
                    Text(
                        text = "Your Gateway to Flexible Employment",
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoginBottomSheet(
    onLoginSuccess: () -> Unit,
    onDismiss: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    val selectedCountryCode = "+91"

    val context = LocalContext.current
    val activity = context as Activity
    val otpUiState by loginViewModel.otpUiState.observeAsState(OtpUiState.Idle)
    val focusManager = LocalFocusManager.current
    val hapticFeedback = LocalHapticFeedback.current

    // Enhanced animations
    val logoScale by animateFloatAsState(
        targetValue = when (otpUiState) {
            is OtpUiState.Loading -> 0.9f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "logo_scale"
    )

    LaunchedEffect(otpUiState) {
        if (otpUiState is OtpUiState.LoggedIn) {
            onLoginSuccess()
            loginViewModel.resetLoginStateToIdle()
        }
    }

    // Enhanced background gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0066FF),
                        Color(0xFF004BD9),
                        Color(0xFFE3F2FD)
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Enhanced skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.2f),
                elevation = 0.dp,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
            ) {
                Text(
                    "Skip",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        // Enhanced main content surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .align(Alignment.BottomCenter)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Enhanced handle bar
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(6.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Enhanced logo with animation
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(logoScale)
                        .shadow(8.dp, CircleShape),
                    shape = CircleShape,
                    color = Color.White,
                    elevation = 4.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.parttimes),
                        contentDescription = "ParTimes Logo",
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Enhanced content with better animations
                AnimatedContent(
                    targetState = otpUiState,
                    transitionSpec = {
                        val slideDirection = if (targetState is OtpUiState.OtpSent) 1 else -1
                        slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { slideDirection * 400 } + fadeIn(
                            animationSpec = tween(400)
                        ) togetherWith slideOutHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { -slideDirection * 400 } + fadeOut(
                            animationSpec = tween(200)
                        )
                    },
                    label = "content_animation"
                ) { currentState ->
                    when (currentState) {
                        is OtpUiState.Loading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 48.dp)
                            ) {
                                // Enhanced loading indicator
                                CircularProgressIndicator(
                                    color = Color(0xFF1976D2),
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    "Processing your request...",
                                    style = MaterialTheme.typography.h6,
                                    color = Color.Black.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Please wait a moment",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        is OtpUiState.Idle, is OtpUiState.Error -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Welcome to ParTimes",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Enter your phone number to get started",
                                    fontSize = 17.sp,
                                    color = Color.Black.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )

                                Spacer(modifier = Modifier.height(36.dp))

                                // Enhanced phone number input
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 10 && newValue.all { it.isDigit() }) {
                                            phoneNumber = newValue
                                            if (newValue.length <= 9) {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    },
                                    label = { Text("Phone Number", fontWeight = FontWeight.Medium) },
                                    placeholder = { Text("Enter 10-digit number") },
                                    leadingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 16.dp)
                                        ) {
                                            Surface(
                                                color = Color(0xFF1976D2).copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    selectedCountryCode,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1976D2),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(28.dp)
                                                    .background(
                                                        Color.Gray.copy(alpha = 0.3f),
                                                        RoundedCornerShape(1.dp)
                                                    )
                                            )
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color(0xFF1976D2),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                                        cursorColor = Color(0xFF1976D2),
                                        focusedLabelColor = Color(0xFF1976D2)
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp)
                                )

                                // Enhanced error display
                                AnimatedVisibility(
                                    visible = currentState is OtpUiState.Error,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                                ) {
                                    if (currentState is OtpUiState.Error) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp),
                                            backgroundColor = Color.Red.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = 0.dp,
                                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = currentState.message,
                                                color = Color.Red.copy(alpha = 0.8f),
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // Enhanced continue button
                                val buttonEnabled = phoneNumber.isNotBlank() &&
                                                  phoneNumber.length == 10 &&
                                                  otpUiState !is OtpUiState.Loading

                                Button(
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        loginViewModel.sendOtp(fullPhoneNumber, activity)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    elevation = ButtonDefaults.elevation(
                                        defaultElevation = if (buttonEnabled) 4.dp else 0.dp
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (buttonEnabled) Color(0xFF1976D2) else Color(0xFF1976D2).copy(alpha = 0.4f),
                                        contentColor = Color.White,
                                        disabledBackgroundColor = Color(0xFF1976D2).copy(alpha = 0.4f),
                                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = buttonEnabled
                                ) {
                                    Text(
                                        text = "Continue",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Enhanced terms text
                                Card(
                                    backgroundColor = Color.Gray.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 0.dp
                                ) {
                                    Text(
                                        text = buildAnnotatedString {
                                            append("By continuing, you agree to our ")
                                            withStyle(
                                                style = SpanStyle(
                                                    color = Color(0xFF1976D2),
                                                    textDecoration = TextDecoration.Underline,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            ) {
                                                append("Terms & Conditions")
                                            }
                                            append(" and ")
                                            withStyle(
                                                style = SpanStyle(
                                                    color = Color(0xFF1976D2),
                                                    textDecoration = TextDecoration.Underline,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            ) {
                                                append("Privacy Policy")
                                            }
                                        },
                                        fontSize = 13.sp,
                                        color = Color.Black.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        is OtpUiState.OtpSent -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Verify Your Number",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "We've sent a 6-digit code to\n$selectedCountryCode $phoneNumber",
                                    fontSize = 17.sp,
                                    color = Color.Black.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp
                                )

                                Spacer(modifier = Modifier.height(36.dp))

                                // Enhanced OTP input
                                OutlinedTextField(
                                    value = otpValue,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                            otpValue = newValue
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                    label = { Text("Enter OTP", fontWeight = FontWeight.Medium) },
                                    placeholder = { Text("6-digit verification code") },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        focusedBorderColor = Color(0xFF1976D2),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                                        cursorColor = Color(0xFF1976D2),
                                        focusedLabelColor = Color(0xFF1976D2)
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp)
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                // Enhanced verify button
                                val buttonEnabled = otpValue.length == 6

                                Button(
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        loginViewModel.verifyOtp(otpValue)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    elevation = ButtonDefaults.elevation(
                                        defaultElevation = if (buttonEnabled) 4.dp else 0.dp
                                    ),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = if (buttonEnabled) Color(0xFF1976D2) else Color(0xFF1976D2).copy(alpha = 0.4f),
                                        contentColor = Color.White,
                                        disabledBackgroundColor = Color(0xFF1976D2).copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = buttonEnabled
                                ) {
                                    Text(
                                        text = "Verify OTP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Enhanced action buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TextButton(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            loginViewModel.resetLoginStateToIdle()
                                            otpValue = ""
                                        }
                                    ) {
                                        Text(
                                            "Change Number",
                                            color = Color(0xFF1976D2),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                                            loginViewModel.resendOtp(fullPhoneNumber, activity)
                                        },
                                        enabled = otpUiState !is OtpUiState.Loading
                                    ) {
                                        Text(
                                            if (otpUiState is OtpUiState.Loading) "Sending..." else "Resend OTP",
                                            color = if (otpUiState is OtpUiState.Loading)
                                                Color.Gray else Color(0xFF1976D2),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        else -> { /* Handle other states */ }
                    }
                }
            }
        }
    }
}
