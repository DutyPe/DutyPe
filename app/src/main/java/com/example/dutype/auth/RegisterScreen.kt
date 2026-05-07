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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.auth.rememberPhoneNumberHintRequester
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.FirestoreUtils
import com.example.dutype.utils.LocaleHelper
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
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU
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
                        val resolvedName = fullName.trim()
                        profileCompletionViewModel.saveUserInfoToLocalStorage(
                            email = "",
                            name = resolvedName,
                            role = role
                        )

                        var referralAppliedInstantly = false
                        if (!pendingReferralCode.isNullOrBlank()) {
                            val resolvedPhone = currentUser.phoneNumber ?: otpState.phoneNumber ?: ""
                            val applyResult = profileCompletionViewModel.applyReferralCode(
                                referralCode = pendingReferralCode,
                                newUserId = currentUser.uid,
                                newUserRole = role.name,
                                newUserName = resolvedName.ifBlank { resolvedPhone.ifBlank { "DutyPe User" } },
                                newUserPhone = resolvedPhone
                            )

                            applyResult.fold(
                                onSuccess = { referralResult ->
                                    referralAppliedInstantly = true
                                    Timber.d("REGISTER - Referral applied immediately for user ${currentUser.uid}")
                                    val successMessage = context.getString(R.string.referral_code_applied_success)
                                    Toast.makeText(
                                        context,
                                        successMessage,
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onFailure = { error ->
                                    // BUG #11 FIX: Previously this was a silent log.warn — users
                                    // never knew their referral failed and saw no money credited.
                                    // Surface the CF error so they can retry / report; the
                                    // profile-setup fallback will still attempt again.
                                    Timber.w(error, "REGISTER - Immediate referral apply failed; fallback will run after profile completion")
                                    val msg = error.message?.takeIf { it.isNotBlank() }
                                        ?: if (isTelugu) "రిఫరల్ కోడ్ వర్తించలేదు. ప్రొఫైల్ పూర్తయిన తర్వాత మళ్లీ ప్రయత్నిస్తాం." else "Couldn't apply referral now — we'll retry after profile setup."
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }

                        if (!pendingReferralCode.isNullOrBlank() && !referralAppliedInstantly) {
                            Timber.d("REGISTER - Referral preserved for fallback apply after profile completion")
                        }

                        otpViewModel.resetState()
                        navigateToProfileSetup(role, navController)
                    } else {
                        val error = registrationResult.exceptionOrNull()
                        Timber.e(error, "REGISTER - Registration finalization failed")
                        Toast.makeText(
                            context,
                            error?.message ?: if (isTelugu) "నమోదును పూర్తి చేయలేకపోయాం. దయచేసి మళ్లీ ప్రయత్నించండి." else "Could not finish registration. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        otpViewModel.resetState()
                    }
                    return@LaunchedEffect
                } else {
                    navController.navigate(Routes.SELECT_ROLE) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
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
            .background(com.example.dutype.ui.theme.WorkerColors.CardBackground)
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

                                    // Check if user already exists. Single-role-per-phone:
                                    // if a user exists under a DIFFERENT role, surface the
                                    // role-conflict message so they know where to log in.
                                    val phoneCheck = FirestoreUtils.checkPhoneForRole(
                                        phoneNumber = fullPhoneNumber,
                                        requestedRole = role.name
                                    )
                                    when (phoneCheck.exists) {
                                        FirestoreUtils.PhoneExistenceResult.EXISTS -> {
                                        isCheckingPhone = false
                                        val existingRoleLabel = when (phoneCheck.existingRole?.uppercase()) {
                                            "WORKER" -> if (isTelugu) "వర్కర్" else "worker"
                                            "EMPLOYER" -> if (isTelugu) "ఎంప్లాయర్" else "employer"
                                            else -> null
                                        }
                                        val message = when {
                                            phoneCheck.roleConflict && existingRoleLabel != null ->
                                                if (isTelugu) "ఈ నంబర్ ఇప్పటికే $existingRoleLabel గా నమోదు అయింది. దయచేసి $existingRoleLabel గా లాగిన్ చేయండి."
                                                else "This number is already registered as a $existingRoleLabel. Please log in as a $existingRoleLabel."
                                            else ->
                                                if (isTelugu) "ఈ నంబర్ ఇప్పటికే నమోదు అయింది. దయచేసి లాగిన్ చేయండి."
                                                else "This number is already registered. Please Login instead."
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        Timber.w(
                                            "📱 REGISTER blocked - phone=$fullPhoneNumber " +
                                                "existingRole=${phoneCheck.existingRole} " +
                                                "requestedRole=${role.name} conflict=${phoneCheck.roleConflict}"
                                        )
                                        return@launch
                                        }
                                        FirestoreUtils.PhoneExistenceResult.UNKNOWN -> {
                                            Timber.w("📱 REGISTER phone pre-check unavailable; continuing to OTP and enforcing uniqueness in registration transaction: $fullPhoneNumber")
                                        }
                                        FirestoreUtils.PhoneExistenceResult.NOT_EXISTS -> Unit
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
                                    // Bug #11 fix: Do NOT fall through to sendOtp on error — that
                                    // would burn an SMS even when the role-conflict check failed.
                                    Toast.makeText(
                                        context,
                                        if (isTelugu) "ఖాతా ధృవీకరణ విఫలమైంది. దయచేసి మళ్లీ ప్రయత్నించండి."
                                        else "Account verification failed. Please try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
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
    var hasRequestedPhoneHint by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU
    val policyLinkColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary
    val requestPhoneNumberHint = rememberPhoneNumberHintRequester(
        onPhoneNumberReceived = { selectedPhoneNumber ->
            hasPhoneInteracted = true
            onPhoneNumberChange(selectedPhoneNumber)
        },
        onUnavailable = { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    )

    val phoneValidationError = remember(phoneNumber, hasPhoneInteracted) {
        if (!hasPhoneInteracted || phoneNumber.isEmpty() || phoneNumber.length < 10) null
        else ValidationUtils.getPhoneError(phoneNumber, true)
    }

    var validatedReferralCode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Back button
        IconButton(
            onClick = onBackClick,
              modifier = Modifier.offset(x = (-12).dp).padding(bottom = 4.dp).size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (isTelugu) "వెనక్కి" else "Back",
                tint = WorkerColors.TextPrimary
            )
        }

        Text(
            text = if (isTelugu) "మీ ఖాతా సృష్టించండి" else "Create your account",
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold),
            color = WorkerColors.TextPrimary,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isTelugu) "డ్యూటీపీలో వేలాది కార్మికులు మరియు యజమానులతో చేరండి" else "Join thousands of workers & employers on DutyPe",
            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Full Name Input ──
        Text(
            text = if (isTelugu) "పూర్తి పేరు" else "Full Name",
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
                    if (isTelugu) "మీ పూర్తి పేరు నమోదు చేయండి" else "Enter your full name",
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
            text = if (isTelugu) "మొబైల్ నంబర్" else "Mobile Number",
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
                    .height(53.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !hasRequestedPhoneHint && phoneNumber.isBlank()) {
                            hasRequestedPhoneHint = true
                            requestPhoneNumberHint()
                        }
                    },
                // Batch-o #1: removed auto-launch of the Google Phone
                // Number Hint bottom sheet on focus. The user prefers
                // the keyboard's native suggestion bar (Phone keyboard
                // type already surfaces stored numbers) over an
                // unsolicited modal dialog every time the field is
                // focused.
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

        RegisterReferralSection(
            isTelugu = isTelugu,
            onValidatedCodeChanged = { validatedReferralCode = it }
        )

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
                    append(if (isTelugu) "నేను ఈ వాటికి అంగీకరిస్తున్నాను: " else "I agree to the ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = policyLinkColor)) {
                        append(if (isTelugu) "సేవా నిబంధనలు" else "Terms of Service")
                    }
                    append(if (isTelugu) " మరియు " else " and ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = policyLinkColor)) {
                        append(if (isTelugu) "గోప్యతా విధానం" else "Privacy Policy")
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
                if (!validatedReferralCode.isNullOrBlank()) {
                    scope.launch {
                        profileCompletionViewModel.saveReferralCode(validatedReferralCode!!)
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
                Text(if (isTelugu) "ఖాతా సృష్టించండి" else "Create Account", style = AppTypography.buttonLarge)
            }
        }

        // Validation hint if not all fields filled
        if (!buttonEnabled && !otpState.isLoading && !isCheckingPhone) {
            Spacer(modifier = Modifier.height(6.dp))
            val hint = when {
                fullName.trim().length < 2 -> if (isTelugu) "మీ పూర్తి పేరు నమోదు చేయండి (కనీసం 2 అక్షరాలు)" else "Enter your full name (at least 2 characters)"
                !phoneValid -> if (isTelugu) "చెల్లుబాటు అయ్యే 10 అంకెల మొబైల్ నంబర్ నమోదు చేయండి" else "Enter a valid 10-digit mobile number"
                !termsAccepted -> if (isTelugu) "దయచేసి నిబంధనలు మరియు షరతులను అంగీకరించండి" else "Please accept the Terms & Conditions"
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

        // ── Already have an account? Login ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isTelugu) "ఇప్పటికే ఖాతా ఉందా? " else "Already have an account? ",
                style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary)
            )
            TextButton(
                onClick = onLoginClick,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (isTelugu) "లాగిన్" else "Login",
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

@Composable
private fun RegisterReferralSection(
    isTelugu: Boolean,
    onValidatedCodeChanged: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showReferralInput by remember { mutableStateOf(false) }
    var referralCode by remember { mutableStateOf("") }
    var isValidatingCode by remember { mutableStateOf(false) }
    var codeValidationError by remember { mutableStateOf<String?>(null) }
    var validatedReferrerName by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isTelugu) "రిఫరల్ కోడ్ ఉందా?" else "Have a referral code?",
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
                text = if (showReferralInput) {
                    if (isTelugu) "దాచు" else "Hide"
                } else {
                    if (isTelugu) "కోడ్ నమోదు చేయండి" else "Enter Code"
                },
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
                        onValidatedCodeChanged(null)
                    },
                    placeholder = {
                        Text(
                            if (isTelugu) "ఉదా: DUTY4F9A" else "e.g. DUTY4F9A",
                            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextTertiary)
                        )
                    },
                    trailingIcon = {
                        when {
                            isValidatingCode -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = WorkerColors.TextPrimary
                            )

                            validatedReferrerName != null -> Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = if (isTelugu) "చెల్లుబాటు అయ్యింది" else "Valid",
                                tint = WorkerColors.Success,
                                modifier = Modifier.size(20.dp)
                            )

                            referralCode.isNotEmpty() -> IconButton(onClick = {
                                referralCode = ""
                                codeValidationError = null
                                validatedReferrerName = null
                                onValidatedCodeChanged(null)
                            }) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                    contentDescription = if (isTelugu) "తీసివేయండి" else "Clear",
                                    tint = WorkerColors.IconSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
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
                                    val referralService = com.example.dutype.di.referralServiceFromHilt(context)
                                    val validation = referralService.validateReferralCode(referralCode)
                                    isValidatingCode = false
                                    if (validation.isValid) {
                                        val normalizedCode = com.example.dutype.models.normalizeReferralCode(referralCode)
                                        validatedReferrerName = validation.referrerName
                                        codeValidationError = null
                                        onValidatedCodeChanged(normalizedCode)
                                        Toast.makeText(
                                            context,
                                            if (isTelugu) "✓ ${validation.referrerName} నుండి చెల్లుబాటు అయ్యే కోడ్" else "✓ Valid code from ${validation.referrerName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        codeValidationError = validation.errorMessage
                                        validatedReferrerName = null
                                        onValidatedCodeChanged(null)
                                        Toast.makeText(
                                            context,
                                            validation.errorMessage ?: if (isTelugu) "చెల్లని కోడ్" else "Invalid code",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    isValidatingCode = false
                                    codeValidationError = if (isTelugu) "కోడ్ ధృవీకరణ విఫలమైంది" else "Failed to validate code"
                                    validatedReferrerName = null
                                    onValidatedCodeChanged(null)
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
                        Text(
                            text = if (validatedReferrerName != null) "✓" else if (isTelugu) "ధృవీకరించండి" else "Verify",
                            style = AppTypography.buttonMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            when {
                validatedReferrerName != null -> Text(
                    if (isTelugu) "✓ $validatedReferrerName నుండి చెల్లుబాటు అయ్యే కోడ్" else "✓ Valid code from $validatedReferrerName",
                    style = AppTypography.caption.copy(color = WorkerColors.Success)
                )

                codeValidationError != null && referralCode.length >= 7 -> Text(
                    codeValidationError ?: if (isTelugu) "చెల్లని కోడ్" else "Invalid code",
                    style = AppTypography.caption.copy(color = WorkerColors.Error)
                )

                else -> Text(
                    if (isTelugu) "మీ స్నేహితుడి రిఫరల్ కోడ్ ఉంటే ఇక్కడ నమోదు చేయండి" else "Enter your friend's referral code if you have one",
                    style = AppTypography.caption.copy(color = WorkerColors.TextSecondary)
                )
            }
        }
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
    val context = LocalContext.current
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if (isTelugu) "మీ నంబర్‌ను ధృవీకరించండి" else "Verify your number",
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
            color = WorkerColors.TextPrimary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = buildAnnotatedString {
                append(if (isTelugu) "SMS ద్వారా పంపిన 6 అంకెల కోడ్‌ను ఇక్కడ నమోదు చేయండి: " else "Enter the 6-digit code sent via SMS to ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)) {
                    append("+91 $phoneNumber")
                }
                if (!isTelugu) append(".")
            },
            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(
                text = if (isTelugu) "మొబైల్ నంబర్ మార్చాలా?" else "Change your mobile number?",
                style = AppTypography.bodyMedium.copy(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                color = WorkerColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(23.dp))

        // OTP Input Boxes
        AuthOtpBoxes(otpValue = otpValue, onOtpChange = onOtpChange, digitCount = 6)

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
                            contentDescription = if (isTelugu) "OTP మళ్లీ పంపండి" else "Resend OTP",
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
                    text = if (isTelugu) "OTP మళ్లీ పంపండి" else "Resend OTP",
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
                    Text(if (isTelugu) "ధృవీకరించి ఖాతా సృష్టించండి" else "Verify & Create Account", style = AppTypography.buttonLarge)
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

// ─── Navigation helpers ──────────────────────────────────────────────────────
