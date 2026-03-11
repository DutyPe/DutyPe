package com.example.dutype.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.FirestoreUtils
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.viewmodels.OtpViewModel
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * EnhancedLoginScreen - Login-only OTP authentication
 *
 * This screen handles phone number OTP authentication for existing users.
 * Registration is handled by the separate RegisterScreen.
 * 
 * Flow:
 * 1. SelectRoleScreen (user picks Worker/Employer)
 * 2. EnhancedLoginScreen (this screen - OTP login)
 * 3. Profile Setup or Home Screen
 */
@Composable
fun EnhancedLoginScreen(
    navController: NavController,
    skipRoleSelection: Boolean = true,
    initialRole: String = "WORKER",
    isRegisterMode: Boolean = false, // kept for nav compatibility, ignored
    otpViewModel: OtpViewModel = hiltViewModel()
) {
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    
    val selectedRole = remember(initialRole) {
        when (initialRole.uppercase()) {
            "WORKER" -> UserRole.WORKER
            "EMPLOYER" -> UserRole.EMPLOYER
            else -> UserRole.WORKER
        }
    }

    LaunchedEffect(initialRole, selectedRole) {
        Timber.d("EnhancedLoginScreen - Role: $initialRole -> $selectedRole")
    }

    OtpLoginScreen(
        role = selectedRole,
        otpViewModel = otpViewModel,
        profileCompletionViewModel = profileCompletionViewModel,
        navController = navController
    )
}


/**
 * OTP Login Screen - Login-only flow (no registration logic)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun OtpLoginScreen(
    role: UserRole,
    otpViewModel: OtpViewModel,
    profileCompletionViewModel: ProfileCompletionViewModel,
    navController: NavController
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    var isCheckingPhone by remember { mutableStateOf(false) }
    val selectedCountryCode = "+91"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val otpState by otpViewModel.otpState.collectAsState()

    // Handle OTP verification success
    LaunchedEffect(otpState.otpVerified) {
        if (otpState.otpVerified) {
            Timber.d("📱 OTP VERIFICATION SUCCESS - Starting user check flow")

            try {
                val currentUser = FirebaseAuth.getInstance().currentUser

                if (currentUser != null) {
                    val userId = currentUser.uid

                    // Fetch user data from Firestore
                    var existingUserData: Map<String, Any>? = null
                    try {
                        existingUserData = FirestoreUtils.getUserByUid(userId)
                    } catch (e: Exception) {
                        Timber.w(e, "📱 Failed to fetch user data from Firestore")
                    }

                    if (existingUserData != null) {
                        val userRole = existingUserData["role"] as? String
                        val profileComplete = existingUserData["profileCompleted"] as? Boolean ?: false
                        val fullName = existingUserData["fullName"] as? String

                        // CRITICAL FIX: DUAL ROLE SUPPORT
                        // Users can have multiple roles (Worker + Employer)
                        // The 'role' parameter from navigation indicates which role they want to use for this session
                        // ALWAYS use the 'role' parameter, NOT the database role
                        // This ensures users navigate to the correct home screen based on where they clicked login

                        if (userRole != null) {
                            val parsedRole = try { UserRole.valueOf(userRole.uppercase()) } catch (e: Exception) { null }
                            val hasRequiredFields = !fullName.isNullOrBlank() && profileComplete

                            if (parsedRole != null && hasRequiredFields) {
                                // User has complete profile - navigate to home
                                // CRITICAL: Use 'role' parameter (from navigation), NOT parsedRole (from DB)
                                profileCompletionViewModel.updateUserRole(role)
                                profileCompletionViewModel.markProfileComplete(role)
                                profileCompletionViewModel.markProfileSetupAsShown(role)
                                profileCompletionViewModel.saveUserInfoToLocalStorage(
                                    email = existingUserData["email"] as? String ?: "",
                                    name = fullName ?: "",
                                    role = role  // Use navigation role, not DB role
                                )

                                // CRITICAL: Navigate based on 'role' parameter, not DB role
                                navigateToHome(role, navController)
                            } else {
                                // Profile incomplete - go to setup
                                navigateToProfileSetup(role, navController)
                            }
                        } else {
                            // No role in DB - use selected role and go to setup
                            profileCompletionViewModel.updateUserRole(role)
                            navigateToProfileSetup(role, navController)
                        }
                    } else {
                        // New user - save role and phone number to Firebase
                        profileCompletionViewModel.updateUserRole(role)
                        
                        // Save phone number to Firebase for new users
                        val phoneToSave = currentUser.phoneNumber ?: otpState.phoneNumber
                        if (!phoneToSave.isNullOrBlank()) {
                            try {
                                FirestoreUtils.saveUserPhoneNumber(userId, phoneToSave, role.name)
                                Timber.d("📱 EnhancedLoginScreen - Saved phone number to Firebase: $phoneToSave")
                            } catch (e: Exception) {
                                Timber.w(e, "📱 Failed to save phone number to Firebase")
                            }
                        }
                        
                        // 🎁 CRITICAL FIX: Apply referral code if provided during registration
                        val savedReferralCode = profileCompletionViewModel.getReferralCode()
                        if (!savedReferralCode.isNullOrBlank()) {
                            try {
                                Timber.d("🎁 REFERRAL: Applying referral code after registration: $savedReferralCode")
                                
                                val result = profileCompletionViewModel.applyReferralCode(
                                    referralCode = savedReferralCode,
                                    newUserId = userId,
                                    newUserRole = role.name,
                                    newUserName = currentUser.displayName ?: phoneToSave ?: "User",
                                    newUserPhone = phoneToSave ?: ""
                                )
                                
                                if (result.isSuccess) {
                                    Timber.d("🎁 REFERRAL: ✅ Code applied successfully!")
                                    Toast.makeText(
                                        context,
                                        "✓ Referral code applied! You earned ₹25 bonus",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Timber.w("🎁 REFERRAL: ❌ Failed to apply code: ${result.exceptionOrNull()?.message}")
                                    // Don't block registration if referral fails
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "🎁 REFERRAL: Error applying code")
                                // Don't block registration if referral fails
                            }
                        }
                        
                        navigateToProfileSetup(role, navController)
                    }
                } else {
                    // No Firebase user - go back to role selection
                    navController.navigate(Routes.SELECT_ROLE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "📱 Error checking profile")
                navigateToProfileSetup(role, navController)
            }

            otpViewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)  // White background for login screen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            AnimatedContent(
                targetState = !otpState.otpSent,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { if (targetState) -400 else 400 },
                        animationSpec = tween(450, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(450)) with
                    slideOutHorizontally(
                        targetOffsetX = { if (targetState) 400 else -400 },
                        animationSpec = tween(450, easing = EaseInCubic)
                    ) + fadeOut(animationSpec = tween(450))
                },
                label = "login_animation"
            ) { isPhoneNumberScreen ->
                if (isPhoneNumberScreen) {
                    PhoneInputSection(
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 10) {
                                phoneNumber = newValue
                            }
                        },
                        selectedCountryCode = selectedCountryCode,
                        otpState = otpState,
                        isCheckingPhone = isCheckingPhone,
                        onContinueClick = {
                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                            scope.launch {
                                try {
                                    isCheckingPhone = true

                                    // Login-only: check if user exists
                                    val userExists = FirestoreUtils.doesUserExist(fullPhoneNumber)
                                    if (!userExists) {
                                        isCheckingPhone = false
                                        Toast.makeText(
                                            context,
                                            "No account found with this number. Please Register first.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Timber.w("📱 Login blocked - User doesn't exist: $fullPhoneNumber")
                                        return@launch
                                    }

                                    isCheckingPhone = false
                                    profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                    profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                    otpViewModel.sendOtp(fullPhoneNumber, context)
                                } catch (e: Exception) {
                                    isCheckingPhone = false
                                    Timber.e(e, "📱 Error in phone check")
                                    otpViewModel.sendOtp(fullPhoneNumber, context)
                                }
                            }
                        },
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onRegisterClick = {
                            // Navigate to separate register screen
                            navController.navigate("${Routes.REGISTER}?role=${role.name}") {
                                popUpTo("${Routes.ENHANCED_LOGIN}?role=${role.name}") { inclusive = true }
                            }
                        }
                    )
                } else {
                    val resendCooldown by otpViewModel.resendCooldownSeconds.collectAsState()
                    OtpInputSection(
                        otpValue = otpValue,
                        onOtpChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 6) {
                                otpValue = newValue
                            }
                        },
                        phoneNumber = phoneNumber,
                        otpState = otpState,
                        onVerifyClick = {
                            otpViewModel.verifyOtp(otpValue, context)
                        },
                        onResendClick = {
                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                            otpViewModel.resendOtp(fullPhoneNumber, context)
                        },
                        onBackClick = {
                            otpViewModel.resetState()
                        },
                        resendCooldownSeconds = resendCooldown
                    )
                }
            }
        }
    }
}

private fun navigateToHome(role: UserRole, navController: NavController) {
    when (role) {
        UserRole.WORKER -> navController.navigate(Routes.WORKER_HOME) {
            popUpTo(0) { inclusive = true }
        }
        UserRole.EMPLOYER -> navController.navigate(Routes.EMPLOYER_HOME) {
            popUpTo(0) { inclusive = true }
        }
        else -> navController.navigate(Routes.WORKER_HOME) {
            popUpTo(0) { inclusive = true }
        }
    }
}

private fun navigateToProfileSetup(role: UserRole, navController: NavController) {
    when (role) {
        UserRole.WORKER -> navController.navigate(Routes.PROFILE_SETUP) {
            popUpTo(0) { inclusive = true }
        }
        UserRole.EMPLOYER -> navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) {
            popUpTo(0) { inclusive = true }
        }
        else -> navController.navigate(Routes.PROFILE_SETUP) {
            popUpTo(0) { inclusive = true }
        }
    }
}


/**
 * Phone Input Section - Login-only phone number entry
 */
@Composable
private fun PhoneInputSection(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountryCode: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    isCheckingPhone: Boolean,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var hasInteracted by remember { mutableStateOf(false) }

    val phoneValidationError = remember(phoneNumber, hasInteracted) {
        if (!hasInteracted || phoneNumber.isEmpty() || phoneNumber.length < 10) null
        else ValidationUtils.getPhoneError(phoneNumber, true)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalAlignment = Alignment.Start
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = WorkerColors.TextPrimary
            )
        }

        Text(
            text = "Welcome back",
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold),
            color = WorkerColors.TextPrimary,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Enter your mobile number to login",
            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary),
            textAlign = TextAlign.Start
        )

        if (phoneValidationError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = phoneValidationError, color = WorkerColors.Error, style = AppTypography.caption)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { /* Country picker - future */ },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.width(66.dp).height(53.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.CardBackground, contentColor = WorkerColors.TextPrimary),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, WorkerColors.Border),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(text = "🇮🇳", fontSize = 18.sp, fontFamily = MeeshoFontFamily)
            }

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }.take(10)
                    hasInteracted = true
                    onPhoneNumberChange(filtered)
                },
                placeholder = { Text(text = "9876543210", style = AppTypography.bodyLarge.copy(color = WorkerColors.TextTertiary)) },
                leadingIcon = { Text(text = selectedCountryCode, style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)) },
                trailingIcon = { Icon(Icons.Filled.Person, contentDescription = "Profile", tint = WorkerColors.IconSecondary) },
                modifier = Modifier.weight(1f).height(53.dp),
                singleLine = true,
                isError = phoneValidationError != null,
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (phoneValidationError != null) WorkerColors.Error else WorkerColors.TextPrimary,
                    unfocusedBorderColor = if (phoneValidationError != null) WorkerColors.Error else WorkerColors.Border,
                    errorBorderColor = WorkerColors.Error,
                    cursorColor = WorkerColors.TextPrimary,
                    focusedContainerColor = WorkerColors.CardBackground,
                    unfocusedContainerColor = WorkerColors.CardBackground
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val buttonEnabled = ValidationUtils.isValidIndianPhoneNumber(phoneNumber) && !otpState.isLoading && !isCheckingPhone

        Button(
            onClick = {
                Timber.d("📱 Login - Continue button clicked")
                onContinueClick()
            },
            modifier = Modifier.fillMaxWidth().height(53.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (buttonEnabled) WorkerColors.TextPrimary else WorkerColors.CardBackground,
                contentColor = if (buttonEnabled) WorkerColors.CardBackground else WorkerColors.TextPrimary,
                disabledContainerColor = WorkerColors.ChipBackground,
                disabledContentColor = WorkerColors.TextSecondary
            ),
            shape = RoundedCornerShape(6.dp),
            enabled = buttonEnabled,
            border = BorderStroke(1.dp, WorkerColors.Border)
        ) {
            if (isCheckingPhone || otpState.isLoading) {
                CircularProgressIndicator(color = WorkerColors.TextSecondary, strokeWidth = 2.2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text(stringResource(R.string.continue_text), style = AppTypography.buttonLarge)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = buildAnnotatedString {
                append("By continuing, you agree to our ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WorkerColors.Info, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                    append("Terms of Service")
                }
                append(" and ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WorkerColors.Info, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                    append("Privacy Policy")
                }
            },
            style = AppTypography.caption.copy(color = WorkerColors.TextSecondary, lineHeight = 18.sp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Navigate to Register screen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary)
            )
            TextButton(
                onClick = onRegisterClick,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Register Now",
                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = WorkerColors.Info)
                )
            }
        }

        AnimatedVisibility(
            visible = otpState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            com.example.dutype.components.ErrorCard(message = otpState.error)
        }
    }
}


/**
 * OTP Input Section - Enter the 6-digit OTP code
 */
@Composable
private fun OtpInputSection(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    phoneNumber: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit,
    resendCooldownSeconds: Int = 0  // Add parameter for ViewModel cooldown
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Welcome to DutyPe",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = WorkerColors.TextPrimary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = buildAnnotatedString {
                append("Enter the 6-digit code sent via SMS at ")
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = WorkerColors.TextPrimary
                    )
                ) {
                    append("+91 $phoneNumber")
                }
                append(".")
            },
            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(3.dp))

        TextButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(
                text = "Change your mobile number?",
                style = AppTypography.bodyMedium.copy(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                color = WorkerColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(23.dp))

        OtpInputBoxes(
            otpValue = otpValue,
            onOtpChange = onOtpChange,
            digitCount = 6
        )

        Spacer(modifier = Modifier.height(18.dp))

        val otpButtonEnabled = otpValue.length == 6 && !otpState.isLoading

        // Use ViewModel cooldown instead of local timer
        val timerActive = resendCooldownSeconds > 0
        val remainingSeconds = resendCooldownSeconds

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Resend button with timer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(70.dp)
            ) {
                Box(
                    modifier = Modifier.size(53.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (timerActive && remainingSeconds > 0) return@IconButton
                            onResendClick()
                        },
                        modifier = Modifier.size(53.dp),
                        enabled = remainingSeconds == 0 || !timerActive
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_revert),
                            contentDescription = "Resend OTP",
                            tint = if (timerActive && remainingSeconds > 0) WorkerColors.TextDisabled else WorkerColors.TextPrimary
                        )
                    }

                    // Timer display
                    if (timerActive && remainingSeconds > 0) {
                        Text(
                            text = remainingSeconds.toString(),
                            style = AppTypography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = WorkerColors.TextSecondary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 2.dp, bottom = 1.dp)
                        )
                    }
                }

                // Resend hint text
                Text(
                    text = "Resend OTP",
                    style = AppTypography.labelSmall.copy(
                        color = if (timerActive && remainingSeconds > 0) WorkerColors.TextDisabled else WorkerColors.TextSecondary
                    )
                )
            }

            Button(
                onClick = onVerifyClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                    .height(53.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorkerColors.TextPrimary,
                    contentColor = WorkerColors.CardBackground,
                    disabledContainerColor = WorkerColors.ChipBackground,
                    disabledContentColor = WorkerColors.TextTertiary
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = otpButtonEnabled
            ) {
                if (otpState.isLoading) {
                    CircularProgressIndicator(
                        color = WorkerColors.CardBackground,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("Verify", style = AppTypography.buttonLarge)
                }
            }
        }

        AnimatedVisibility(
            visible = otpState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            com.example.dutype.components.ErrorCard(
                message = otpState.error
            )
        }
    }
}

/**
 * OTP Input Boxes - Visual representation of 6-digit OTP
 */
@Composable
private fun OtpInputBoxes(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    digitCount: Int = 6
) {
    var isFocused by remember { mutableStateOf(false) }

    // When this composable is first shown, mark as focused
    LaunchedEffect(Unit) {
        isFocused = true
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isFocused = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            repeat(digitCount) { index ->
                val isFocusedIndex = index == otpValue.length && isFocused
                val isFilledIndex = index < otpValue.length
                val digit = otpValue.getOrNull(index)?.toString() ?: ""

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isFilledIndex) WorkerColors.SuccessLight else WorkerColors.CardBackground,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                isFocusedIndex -> WorkerColors.TextPrimary
                                isFilledIndex -> WorkerColors.Success
                                else -> WorkerColors.Border
                            },
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        style = AppTypography.pageTitle.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = WorkerColors.TextPrimary
                    )
                }
            }
        }

        // Invisible text field for input with autofill support
        BasicTextField(
            value = otpValue,
            onValueChange = { newValue ->
                if (newValue.length <= digitCount && newValue.all { it.isDigit() }) {
                    onOtpChange(newValue)
                    isFocused = true
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { isFocused = true }
                .alpha(0f),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = { innerTextField ->
                innerTextField()
            }
        )
    }
}
