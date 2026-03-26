package com.example.dutype.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.example.dutype.viewmodels.OtpState
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * RegisterScreen - Dedicated registration screen
 *
 * Separate from login with registration-specific features:
 * - Full name capture (early, before profile setup)
 * - Phone number with OTP verification
 * - Referral code with inline validation
 * - Terms & conditions acceptance
 * - "Already have an account? Login" cross-navigation
 */
@Composable
fun RegisterScreen(
    navController: NavController,
    initialRole: String = "WORKER",
    otpViewModel: OtpViewModel = hiltViewModel()
) {
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()

    val selectedRole = remember(initialRole) {
        when (initialRole.uppercase()) {
            "EMPLOYER" -> UserRole.EMPLOYER
            else -> UserRole.WORKER
        }
    }

    LaunchedEffect(initialRole, selectedRole) {
        Timber.d("RegisterScreen - Role: $initialRole -> $selectedRole")
        otpViewModel.setRoleContext(selectedRole)
    }

    RegisterContent(
        role = selectedRole,
        otpViewModel = otpViewModel,
        profileCompletionViewModel = profileCompletionViewModel,
        navController = navController
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun RegisterContent(
    role: UserRole,
    otpViewModel: OtpViewModel,
    profileCompletionViewModel: ProfileCompletionViewModel,
    navController: NavController
) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    var isCheckingPhone by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    val selectedCountryCode = "+91"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val otpState by otpViewModel.otpState.collectAsState()

    BackHandler {
        if (otpState.otpSent) {
            otpViewModel.resetState()
        } else {
            safeAuthBackNavigation(navController)
        }
    }

    // Handle OTP verification success — new user registration flow
    LaunchedEffect(otpState.otpVerified) {
        if (otpState.otpVerified) {
            Timber.d("📱 REGISTER - OTP verified, creating new user profile")
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val pendingReferralCode = profileCompletionViewModel.getReferralCode()
                    val registrationResult = otpViewModel.completeRegistration(
                        role = role,
                        fullName = fullName.trim(),
                        referralCode = pendingReferralCode
                    )

                    if (registrationResult.isSuccess) {
                        profileCompletionViewModel.saveUserInfoToLocalStorage(
                            email = "",
                            name = fullName.trim(),
                            role = role
                        )

                        val userId = currentUser.uid
                        val phoneToSave = currentUser.phoneNumber ?: otpState.phoneNumber
                        val trimmedName = fullName.trim()

                        if (!pendingReferralCode.isNullOrBlank()) {
                            val referralApplyResult = profileCompletionViewModel.applyReferralCode(
                                referralCode = pendingReferralCode,
                                newUserId = userId,
                                newUserRole = role.name,
                                newUserName = trimmedName.ifBlank { currentUser.displayName ?: phoneToSave ?: "User" },
                                newUserPhone = phoneToSave ?: ""
                            )

                            if (referralApplyResult.isSuccess) {
                                Toast.makeText(context, "Referral bonus credited successfully", Toast.LENGTH_LONG).show()
                            } else {
                                Timber.w("REGISTER - Referral apply failed after registration: ${referralApplyResult.exceptionOrNull()?.message}")
                            }
                        }

                        otpViewModel.resetState()
                        navigateToProfileSetup(role, navController)
                    } else {
                        val error = registrationResult.exceptionOrNull()
                        Timber.e(error, "REGISTER - Registration finalization failed")
                        Toast.makeText(
                            context,
                            error?.message ?: "Could not finish registration. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        otpViewModel.resetState()
                    }
                    return@LaunchedEffect
                } else {
                    navController.navigate(Routes.SELECT_ROLE) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "📱 REGISTER - Error in post-OTP flow")
                navigateToProfileSetup(role, navController)
            }
            otpViewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                    ) + fadeIn(animationSpec = tween(450)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { if (targetState) 400 else -400 },
                        animationSpec = tween(450, easing = EaseInCubic)
                    ) + fadeOut(animationSpec = tween(450))
                },
                label = "register_animation"
            ) { isInputScreen ->
                if (isInputScreen) {
                    RegisterInputSection(
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 10) {
                                phoneNumber = newValue
                            }
                        },
                        selectedCountryCode = selectedCountryCode,
                        otpState = otpState,
                        isCheckingPhone = isCheckingPhone,
                        termsAccepted = termsAccepted,
                        onTermsToggle = { termsAccepted = it },
                        profileCompletionViewModel = profileCompletionViewModel,
                        onContinueClick = {
                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                            scope.launch {
                                try {
                                    isCheckingPhone = true

                                    // Check if user already exists
                                    val userExists = FirestoreUtils.doesUserExist(fullPhoneNumber)
                                    if (userExists) {
                                        isCheckingPhone = false
                                        Toast.makeText(
                                            context,
                                            "This number is already registered. Please Login instead.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Timber.w("📱 REGISTER blocked - User already exists: $fullPhoneNumber")
                                        return@launch
                                    }

                                    isCheckingPhone = false
                                    profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                    profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)

                                    // Save name early so profile setup can pre-fill
                                    if (fullName.trim().isNotBlank()) {
                                        profileCompletionViewModel.saveUserInfoToLocalStorage(
                                            email = "",
                                            name = fullName.trim(),
                                            role = role
                                        )
                                    }

                                    otpViewModel.sendOtp(fullPhoneNumber, context)
                                } catch (e: Exception) {
                                    isCheckingPhone = false
                                    Timber.e(e, "📱 REGISTER - Error in phone check")
                                    otpViewModel.sendOtp(fullPhoneNumber, context)
                                }
                            }
                        },
                        onBackClick = { safeAuthBackNavigation(navController) },
                        onLoginClick = {
                            // Navigate to login screen
                            navController.navigate("${Routes.ENHANCED_LOGIN}?role=${role.name}") {
                                popUpTo("${Routes.REGISTER}?role=${role.name}") { inclusive = true }
                            }
                        }
                    )
                } else {
                    val resendCooldown by otpViewModel.resendCooldownSeconds.collectAsState()
                    RegisterOtpSection(
                        otpValue = otpValue,
                        onOtpChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 6) {
                                otpValue = newValue
                            }
                        },
                        phoneNumber = phoneNumber,
                        otpState = otpState,
                        onVerifyClick = { otpViewModel.verifyOtp(otpValue, context) },
                        onResendClick = {
                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                            otpViewModel.resendOtp(fullPhoneNumber, context)
                        },
                        onBackClick = { otpViewModel.resetState() },
                        resendCooldownSeconds = resendCooldown
                    )
                }
            }
        }
    }
}

private fun safeAuthBackNavigation(navController: NavController) {
    val popped = navController.popBackStack()
    if (!popped) {
        navController.navigate(Routes.SELECT_ROLE) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
}

// ─── Registration Input Section ──────────────────────────────────────────────

@Composable
private fun RegisterInputSection(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountryCode: String,
    otpState: OtpState,
    isCheckingPhone: Boolean,
    termsAccepted: Boolean,
    onTermsToggle: (Boolean) -> Unit,
    profileCompletionViewModel: ProfileCompletionViewModel,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    var hasPhoneInteracted by remember { mutableStateOf(false) }
    val phoneValidationError = remember(phoneNumber, hasPhoneInteracted) {
        if (!hasPhoneInteracted || phoneNumber.isEmpty() || phoneNumber.length < 10) null
        else ValidationUtils.getPhoneError(phoneNumber, true)
    }

    // Referral code state
    var showReferralInput by remember { mutableStateOf(false) }
    var referralCode by remember { mutableStateOf("") }
    var isValidatingCode by remember { mutableStateOf(false) }
    var codeValidationError by remember { mutableStateOf<String?>(null) }
    var validatedReferrerName by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Back button
        IconButton(
            onClick = onBackClick,
              modifier = Modifier.offset(x = (-12).dp).padding(bottom = 8.dp).size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = WorkerColors.TextPrimary
            )
        }

        Text(
            text = "Create your account",
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold),
            color = WorkerColors.TextPrimary,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Join thousands of workers & employers on DutyPe",
            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Full Name Input ──
        Text(
            text = "Full Name",
            style = AppTypography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = WorkerColors.TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = { newValue ->
                // Allow letters, spaces, and dots only; max 50 chars
                val filtered = newValue.filter { it.isLetter() || it == ' ' || it == '.' }.take(50)
                onFullNameChange(filtered)
            },
            placeholder = {
                Text(
                    "Enter your full name",
                    style = AppTypography.bodyLarge.copy(color = WorkerColors.TextTertiary)
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.Person, contentDescription = null, tint = WorkerColors.IconSecondary)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WorkerColors.TextPrimary,
                unfocusedBorderColor = WorkerColors.Border,
                cursorColor = WorkerColors.TextPrimary,
                focusedContainerColor = WorkerColors.CardBackground,
                unfocusedContainerColor = WorkerColors.CardBackground
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Phone Number Input ──
        Text(
            text = "Mobile Number",
            style = AppTypography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = WorkerColors.TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { /* Country picker - future */ },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier
                    .width(66.dp)
                    .height(53.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorkerColors.CardBackground,
                    contentColor = WorkerColors.TextPrimary
                ),
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
                    hasPhoneInteracted = true
                    onPhoneNumberChange(filtered)
                },
                placeholder = {
                    Text("9876543210", style = AppTypography.bodyLarge.copy(color = WorkerColors.TextTertiary))
                },
                leadingIcon = {
                    Text(
                        text = selectedCountryCode,
                        style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(53.dp),
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

        if (phoneValidationError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = phoneValidationError,
                color = WorkerColors.Error,
                style = AppTypography.caption
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Referral Code (Expandable) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Have a referral code?",
                style = AppTypography.bodyMedium.copy(
                    color = WorkerColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
            TextButton(
                onClick = { showReferralInput = !showReferralInput },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (showReferralInput) "Hide" else "Enter Code",
                    style = AppTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = WorkerColors.Info
                    )
                )
            }
        }

        AnimatedVisibility(
            visible = showReferralInput,
            enter = slideInVertically(initialOffsetY = { -20 }, animationSpec = tween(300)) + fadeIn(tween(300)),
            exit = slideOutVertically(targetOffsetY = { -20 }, animationSpec = tween(300)) + fadeOut(tween(300))
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = { newValue ->
                            val filtered = com.example.dutype.models.normalizeReferralCode(newValue)
                            referralCode = filtered
                            codeValidationError = null
                            validatedReferrerName = null
                        },
                        placeholder = {
                            Text("e.g. DUTY4F9A", style = AppTypography.bodyMedium.copy(color = WorkerColors.TextTertiary))
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = WorkerColors.IconSecondary, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            when {
                                isValidatingCode -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = WorkerColors.TextPrimary)
                                validatedReferrerName != null -> Icon(Icons.Filled.CheckCircle, contentDescription = "Valid", tint = WorkerColors.Success, modifier = Modifier.size(20.dp))
                                referralCode.isNotEmpty() -> IconButton(onClick = { referralCode = ""; codeValidationError = null; validatedReferrerName = null }) {
                                    Icon(painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Clear", tint = WorkerColors.IconSecondary, modifier = Modifier.size(18.dp))
                                }
                                else -> null
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(53.dp),
                        singleLine = true,
                        isError = codeValidationError != null && referralCode.length >= 7,
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = when {
                                validatedReferrerName != null -> WorkerColors.Success
                                codeValidationError != null -> WorkerColors.Error
                                else -> WorkerColors.TextPrimary
                            },
                            unfocusedBorderColor = when {
                                validatedReferrerName != null -> WorkerColors.Success
                                codeValidationError != null -> WorkerColors.Error
                                else -> WorkerColors.Border
                            },
                            cursorColor = WorkerColors.TextPrimary,
                            focusedContainerColor = WorkerColors.CardBackground,
                            unfocusedContainerColor = WorkerColors.CardBackground
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None
                        )
                    )

                    Button(
                        onClick = {
                            if (referralCode.length >= 7) {
                                isValidatingCode = true
                                scope.launch {
                                    try {
                                        val referralService = com.example.dutype.services.ReferralService(
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                                            com.google.firebase.auth.FirebaseAuth.getInstance(),
                                            com.google.firebase.functions.FirebaseFunctions.getInstance(),
                                            com.example.dutype.services.SmartNotificationManager(
                                                context,
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                                                com.example.dutype.services.NotificationService(context, com.google.firebase.firestore.FirebaseFirestore.getInstance())
                                            ),
                                            context
                                        )
                                        val validation = referralService.validateReferralCode(referralCode)
                                        isValidatingCode = false
                                        if (validation.isValid) {
                                            validatedReferrerName = validation.referrerName
                                            codeValidationError = null
                                            Toast.makeText(context, "✓ Valid code from ${validation.referrerName}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            codeValidationError = validation.errorMessage
                                            validatedReferrerName = null
                                            Toast.makeText(context, validation.errorMessage ?: "Invalid code", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        isValidatingCode = false
                                        codeValidationError = "Failed to validate code"
                                        Timber.e(e, "🎁 REFERRAL: Validation error")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.height(53.dp),
                        enabled = referralCode.length >= 7 && !isValidatingCode && validatedReferrerName == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WorkerColors.Info,
                            contentColor = Color.White,
                            disabledContainerColor = WorkerColors.ChipBackground,
                            disabledContentColor = WorkerColors.TextSecondary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isValidatingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(text = if (validatedReferrerName != null) "✓" else "Verify", style = AppTypography.buttonMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                when {
                    validatedReferrerName != null -> Text("✓ Valid code from $validatedReferrerName", style = AppTypography.caption.copy(color = WorkerColors.Success))
                    codeValidationError != null && referralCode.length >= 7 -> Text(codeValidationError ?: "Invalid code", style = AppTypography.caption.copy(color = WorkerColors.Error))
                    else -> Text("Enter referral code to earn ₹25 bonus", style = AppTypography.caption.copy(color = WorkerColors.TextSecondary))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Terms & Conditions Checkbox ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = onTermsToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = WorkerColors.TextPrimary,
                    uncheckedColor = WorkerColors.Border,
                    checkmarkColor = Color.White
                )
            )
            Text(
                text = buildAnnotatedString {
                    append("I agree to the ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WorkerColors.Info, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                        append("Terms of Service")
                    }
                    append(" and ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WorkerColors.Info, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                        append("Privacy Policy")
                    }
                },
                style = AppTypography.caption.copy(color = WorkerColors.TextSecondary, lineHeight = 18.sp),
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Register Button ──
        val nameValid = fullName.trim().length >= 2
        val phoneValid = ValidationUtils.isValidIndianPhoneNumber(phoneNumber)
        val buttonEnabled = nameValid && phoneValid && termsAccepted && !otpState.isLoading && !isCheckingPhone

        Button(
            onClick = {
                Timber.d("📱 Register - Continue clicked, name=$fullName")
                // Save referral code if validated
                if (validatedReferrerName != null && referralCode.isNotBlank()) {
                    scope.launch {
                        profileCompletionViewModel.saveReferralCode(com.example.dutype.models.normalizeReferralCode(referralCode))
                    }
                }
                onContinueClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp),
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
                Text("Create Account", style = AppTypography.buttonLarge)
            }
        }

        // Validation hint if not all fields filled
        if (!buttonEnabled && !otpState.isLoading && !isCheckingPhone) {
            Spacer(modifier = Modifier.height(6.dp))
            val hint = when {
                fullName.trim().length < 2 -> "Enter your full name (at least 2 characters)"
                !phoneValid -> "Enter a valid 10-digit mobile number"
                !termsAccepted -> "Please accept the Terms & Conditions"
                else -> null
            }
            if (hint != null) {
                Text(
                    text = hint,
                    style = AppTypography.caption.copy(color = WorkerColors.TextTertiary),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Why Join DutyPe ──
        WhyJoinSection()

        Spacer(modifier = Modifier.height(16.dp))

        // ── Already have an account? Login ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary)
            )
            TextButton(
                onClick = onLoginClick,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Login",
                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = WorkerColors.Info)
                )
            }
        }

        // Error card
        AnimatedVisibility(
            visible = otpState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            com.example.dutype.components.ErrorCard(message = otpState.error)
        }
    }
}

// ─── Why Join Section ────────────────────────────────────────────────────────

@Composable
private fun WhyJoinSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = WorkerColors.CardBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Why join DutyPe?",
            style = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)
        )
        Spacer(modifier = Modifier.height(10.dp))
        BenefitRow(icon = Icons.Filled.Work, text = "Find local jobs near you instantly")
        Spacer(modifier = Modifier.height(8.dp))
        BenefitRow(icon = Icons.Filled.Badge, text = "Build your professional profile")
        Spacer(modifier = Modifier.height(8.dp))
        BenefitRow(icon = Icons.Filled.CardGiftcard, text = "Earn ₹25 bonus with referral code")
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = WorkerColors.Success, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = AppTypography.bodySmall.copy(color = WorkerColors.TextSecondary))
    }
}

// ─── OTP Verification Section (Register) ─────────────────────────────────────

@Composable
private fun RegisterOtpSection(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    phoneNumber: String,
    otpState: OtpState,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit,
    resendCooldownSeconds: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Verify your number",
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
            color = WorkerColors.TextPrimary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = buildAnnotatedString {
                append("Enter the 6-digit code sent via SMS to ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)) {
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

        // OTP Input Boxes
        RegisterOtpInputBoxes(otpValue = otpValue, onOtpChange = onOtpChange, digitCount = 6)

        Spacer(modifier = Modifier.height(18.dp))

        val otpButtonEnabled = otpValue.length == 6 && !otpState.isLoading
        val timerActive = resendCooldownSeconds > 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(70.dp)
            ) {
                Box(
                    modifier = Modifier.size(53.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (timerActive) return@IconButton
                            onResendClick()
                        },
                        modifier = Modifier.size(53.dp),
                        enabled = !timerActive
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_revert),
                            contentDescription = "Resend OTP",
                            tint = if (timerActive) WorkerColors.TextDisabled else WorkerColors.TextPrimary
                        )
                    }
                    if (timerActive) {
                        Text(
                            text = resendCooldownSeconds.toString(),
                            style = AppTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = WorkerColors.TextSecondary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 2.dp, bottom = 1.dp)
                        )
                    }
                }
                Text(
                    text = "Resend OTP",
                    style = AppTypography.labelSmall.copy(
                        color = if (timerActive) WorkerColors.TextDisabled else WorkerColors.TextSecondary
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
                    Text("Verify & Create Account", style = AppTypography.buttonLarge)
                }
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

// ─── OTP Input Boxes ─────────────────────────────────────────────────────────

@Composable
private fun RegisterOtpInputBoxes(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    digitCount: Int = 6
) {
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isFocused = true }

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
                        style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold),
                        color = WorkerColors.TextPrimary
                    )
                }
            }
        }

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
            decorationBox = { innerTextField -> innerTextField() }
        )
    }
}

// ─── Navigation helpers ──────────────────────────────────────────────────────

private fun navigateToProfileSetup(role: UserRole, navController: NavController) {
    when (role) {
        UserRole.WORKER -> navController.navigate(Routes.PROFILE_SETUP) { popUpTo(0) { inclusive = true } }
        UserRole.EMPLOYER -> navController.navigate(Routes.EMPLOYER_PROFILE_SETUP) { popUpTo(0) { inclusive = true } }
        else -> navController.navigate(Routes.PROFILE_SETUP) { popUpTo(0) { inclusive = true } }
    }
}
