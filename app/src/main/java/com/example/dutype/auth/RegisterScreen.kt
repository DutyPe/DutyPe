package com.example.dutype.auth

import com.dutype.app.R
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.components.LanguageSelectionBottomSheet
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.LocalDarkMode
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.FirestoreUtils
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.viewmodels.OtpState
import com.example.dutype.viewmodels.OtpViewModel
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import timber.log.Timber

private val BrandBluePrimary = Color(0xFF0F0F0F)
private val BrandBlueLight = Color(0xFFF8FAFC)
private val BrandBlueBorder = Color(0xFFE2E8F0)
private val BrandEmeraldAccent = Color(0xFF10B981)
private val Ink900 = Color(0xFF0F172A)
private val Ink600 = Color(0xFF475569)
private val Ink400 = Color(0xFF94A3B8)
private val CardBorder = Color(0xFFE2E8F0)

/**
 * RegisterScreen - Dedicated registration screen
 *
 * Redesigned to match EnhancedLoginScreen look & feel:
 * - Brand Blue background banner with app launcher icon badge
 * - Top bar language selection chip & WhatsApp help button
 * - Clean full name & phone number input boxes (without right contact icon)
 * - Brand Blue pill CTA button
 * - Integrated LanguageSelectionBottomSheet
 */
@OptIn(ExperimentalComposeUiApi::class)
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
    var showLanguageBottomSheet by remember { mutableStateOf(false) }

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

                        // PERF: applying a referral code calls a Cloud Function (cold start + RTT).
                        // It must not hold the user on the OTP screen: navigate immediately and
                        // apply in a process-level scope (the profile-setup fallback still retries
                        // if this fails, because the code is only cleared on success).
                        if (!pendingReferralCode.isNullOrBlank()) {
                            val resolvedPhone = currentUser.phoneNumber ?: otpState.phoneNumber ?: ""
                            val referralUserId = currentUser.uid
                            val referralName = resolvedName.ifBlank { resolvedPhone.ifBlank { "DutyPe User" } }
                            val referralRoleName = role.name
                            val appContext = context.applicationContext
                            val successMessage = context.getString(R.string.referral_code_applied_success)
                            val retryMessage = if (isTelugu) "రిఫరల్ కోడ్ వర్తించలేదు. ప్రొఫైల్ పూర్తయిన తర్వాత మళ్లీ ప్రయత్నిస్తాం." else "Couldn't apply referral now — we'll retry after profile setup."
                            kotlinx.coroutines.CoroutineScope(
                                kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main
                            ).launch {
                                val applyResult = profileCompletionViewModel.applyReferralCode(
                                    referralCode = pendingReferralCode,
                                    newUserId = referralUserId,
                                    newUserRole = referralRoleName,
                                    newUserName = referralName,
                                    newUserPhone = resolvedPhone
                                )
                                applyResult.fold(
                                    onSuccess = {
                                        Timber.d("REGISTER - Referral applied for user $referralUserId")
                                        Toast.makeText(appContext, successMessage, Toast.LENGTH_LONG).show()
                                    },
                                    onFailure = { error ->
                                        Timber.w(error, "REGISTER - Immediate referral apply failed; fallback will run after profile completion")
                                        val msg = error.message?.takeIf { it.isNotBlank() } ?: retryMessage
                                        Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
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
                } else {
                    navController.navigate(Routes.SELECT_ROLE) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                otpViewModel.resetState()
            }
        }
    }

    if (otpState.otpSent) {
        // ── Enterprise SMS Auto-Retrieval ────────────────────────────────────
        DisposableEffect(Unit) {
            val helper = SmsAutoRetrieverHelper(
                context = context,
                onOtpRetrieved = { code ->
                    otpViewModel.onSmsAutoRetrieved(code, context)
                },
                onError = { _ ->
                    otpViewModel.onSmsRetrieverStopped()
                }
            )
            helper.startListening()
            otpViewModel.onSmsRetrieverStarted()
            onDispose {
                helper.unregisterReceiver()
                otpViewModel.onSmsRetrieverStopped()
            }
        }

        // Auto-fill OTP field when Play Services delivers the code
        LaunchedEffect(otpState.autoRetrievedOtp) {
            val autoCode = otpState.autoRetrievedOtp
            if (!autoCode.isNullOrBlank() && autoCode.length == 6) {
                otpValue = autoCode
            }
        }
        // ────────────────────────────────────────────────────────────────────

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            val resendCooldown by otpViewModel.resendCooldownSeconds.collectAsState()
            RegisterOtpSection(
                otpValue = otpValue,
                onOtpChange = { newValue ->
                    if (newValue.all { it.isDigit() } && newValue.length <= 6) {
                        otpValue = newValue
                        if (newValue.length == 6 && !otpState.isLoading && !otpState.otpVerified) {
                            otpViewModel.verifyOtp(newValue, context)
                        }
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

    } else {
        // Register entry screen - same Stitch design language as EnhancedLoginScreen:
        // white background, left-aligned 24sp title, flat outlined fields, black pill CTA,
        // "or" divider + Google, and the Terms/Privacy + Log in footer pinned to the bottom.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 132.dp)
            ) {
                // Top Action Header Bar: WhatsApp Help & Language Chip (End aligned, no back arrow)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        // WhatsApp Help Button
                        Surface(
                            onClick = {
                                val whatsappUrl = "https://wa.me/918500717800?text=Hello%20DutyPe%20Team!%20I%20need%20help%20creating%20an%20account."
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse(whatsappUrl)
                                        setPackage("com.whatsapp")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(whatsappUrl))
                                    context.startActivity(browserIntent)
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_whatsapp),
                                    contentDescription = null,
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTelugu) "సహాయం" else "Help",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = Ink900)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isTelugu) "మీ ఖాతాను సృష్టించండి" else "Create your account",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink900
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))
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
                    profileCompletionViewModel = profileCompletionViewModel,
                    onContinueClick = {
                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                        scope.launch {
                            try {
                                isCheckingPhone = true
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
                                        val message = if (existingRoleLabel != null) {
                                            if (isTelugu)
                                                "ఈ ఫోన్ నంబర్ ఇప్పటికే $existingRoleLabel గా ఉంది. దయచేసి $existingRoleLabel గా లాగిన్ అవ్వండి."
                                            else
                                                "This phone number is already registered as a $existingRoleLabel. Please log in as a $existingRoleLabel."
                                        } else {
                                            if (isTelugu)
                                                "ఈ ఫోన్ నంబర్‌తో ఖాతా ఉంది. దయచేసి లాగిన్ అవ్వండి."
                                            else
                                                "This phone number is already registered. Please log in."
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                    FirestoreUtils.PhoneExistenceResult.NOT_EXISTS -> {
                                        isCheckingPhone = false
                                        profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                        profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                        profileCompletionViewModel.saveUserInfoToLocalStorage(email = "", name = fullName.trim(), role = role)
                                        otpViewModel.sendOtp(fullPhoneNumber, context)
                                    }
                                    FirestoreUtils.PhoneExistenceResult.UNKNOWN -> {
                                        isCheckingPhone = false
                                        Toast.makeText(
                                            context,
                                            if (isTelugu) "ఖాతా ధృవీకరణ విఫలమైంది. దయచేసి ఇంటర్నెట్ కనెక్షన్ తనిఖీ చేసి మళ్లీ ప్రయత్నించండి."
                                            else "Could not verify account. Please check your internet connection and try again.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                isCheckingPhone = false
                                Toast.makeText(context, "Verification failed. Please try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onBackClick = { safeAuthBackNavigation(navController) },
                    onLoginClick = {
                        navController.navigate("${Routes.ENHANCED_LOGIN}?role=${role.name}") {
                            popUpTo("${Routes.REGISTER}?role=${role.name}") { inclusive = true }
                        }
                    },
                    role = role
                )

            }

            // Terms & Privacy footer + Log in link, pinned to the bottom (sibling of the
            // scrollable column, since a scrollable Column cannot use weight()).
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(bottom = 8.dp)
            ) {
                val termsAnnotated = buildAnnotatedString {
                    if (isTelugu) {
                        append("కొనసాగడం ద్వారా, మీరు మా ")
                    } else {
                        append("By continuing, you agree to our ")
                    }
                    pushStringAnnotation(tag = "TERMS", annotation = "terms")
                    withStyle(SpanStyle(color = Ink900, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                        append(if (isTelugu) "సేవా నిబంధనలు" else "Terms")
                    }
                    pop()
                    append(" & ")
                    pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                    withStyle(SpanStyle(color = Ink900, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                        append(if (isTelugu) "గోప్యతా విధానం" else "Privacy Policy")
                    }
                    pop()
                    if (isTelugu) append(" లకు అంగీకరిస్తున్నారు")
                }
                androidx.compose.foundation.text.ClickableText(
                    text = termsAnnotated,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Ink600,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { offset ->
                        termsAnnotated.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                            .firstOrNull()?.let {
                                navController.navigate(Routes.TERMS_OF_SERVICE)
                            }
                        termsAnnotated.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                            .firstOrNull()?.let {
                                navController.navigate(Routes.PRIVACY_POLICY)
                            }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTelugu) "ఇప్పటికే ఖాతా ఉందా?" else "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Ink600)
                    )
                    TextButton(
                        onClick = {
                            navController.navigate("${Routes.ENHANCED_LOGIN}?role=${role.name}") {
                                popUpTo("${Routes.REGISTER}?role=${role.name}") { inclusive = true }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isTelugu) "లాగిన్" else "Log in",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandBluePrimary
                            )
                        )
                    }
                }
            }
        }
    }

    if (showLanguageBottomSheet) {
        LanguageSelectionBottomSheet(
            onDismiss = { showLanguageBottomSheet = false }
        )
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
    profileCompletionViewModel: ProfileCompletionViewModel,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    role: UserRole
) {
    var hasPhoneInteracted by remember { mutableStateOf(false) }
    var hasRequestedPhoneHint by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU
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

    RegisterEntrySection(
        fullName = fullName,
        onFullNameChange = onFullNameChange,
        phoneNumber = phoneNumber,
        onPhoneNumberChange = onPhoneNumberChange,
        selectedCountryCode = selectedCountryCode,
        phoneValidationError = phoneValidationError,
        otpState = otpState,
        isCheckingPhone = isCheckingPhone,
        isTelugu = isTelugu,
        onPhoneFocused = {
            if (!hasRequestedPhoneHint && phoneNumber.isBlank()) {
                hasRequestedPhoneHint = true
                requestPhoneNumberHint()
            }
        },
        onValidatedCodeChanged = { validatedReferralCode = it },
        onCreateClick = {
            Timber.d("📱 Register - Continue clicked, name=$fullName")
            if (!validatedReferralCode.isNullOrBlank()) {
                scope.launch {
                    profileCompletionViewModel.saveReferralCode(validatedReferralCode!!)
                }
            }
            onContinueClick()
        },
        onBackClick = onBackClick,
        onLoginClick = onLoginClick,
        role = role
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RegisterEntrySection(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountryCode: String,
    phoneValidationError: String?,
    otpState: OtpState,
    isCheckingPhone: Boolean,
    isTelugu: Boolean,
    onPhoneFocused: () -> Unit,
    onValidatedCodeChanged: (String?) -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    role: UserRole
) {
    val appContext = LocalContext.current
    val nameValid = fullName.trim().length >= 2
    val phoneValid = ValidationUtils.isValidIndianPhoneNumber(phoneNumber)
    val buttonEnabled = nameValid && phoneValid && !otpState.isLoading && !isCheckingPhone

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {

        // Full Name / Company Name label
        Text(
            text = if (role == UserRole.EMPLOYER) {
                if (isTelugu) "కంపెనీ పేరు" else "Company name"
            } else {
                if (isTelugu) "పూర్తి పేరు" else "Full name"
            },
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Ink600
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        RegisterNameField(
            value = fullName,
            isTelugu = isTelugu,
            textColor = Ink900,
            role = role,
            onValueChange = onFullNameChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Number label
        Text(
            text = if (isTelugu) "మొబైల్ నంబర్" else "Mobile number",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Ink600
            )
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Outlined Phone Number Box: flag | +91 | divider | number
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(
                if (phoneNumber.isNotEmpty()) 2.dp else 1.dp,
                if (phoneValidationError != null) WorkerColors.Error
                else if (phoneNumber.isNotEmpty()) BrandBluePrimary
                else BrandBlueBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🇮🇳",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedCountryCode,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Ink900
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(CardBorder)
                )

                Spacer(modifier = Modifier.width(12.dp))

                val autofill = LocalAutofill.current
                val autofillTree = LocalAutofillTree.current
                val autofillNode = remember {
                    AutofillNode(
                        autofillTypes = listOf(AutofillType.PhoneNumberNational, AutofillType.PhoneNumber),
                        onFill = { filled ->
                            val digits = filled.filter { it.isDigit() }.takeLast(10)
                            onPhoneNumberChange(digits)
                        }
                    )
                }
                LaunchedEffect(autofillNode) {
                    autofillTree += autofillNode
                }

                BasicTextField(
                    value = phoneNumber,
                    onValueChange = { newValue ->
                        onPhoneNumberChange(newValue.filter { it.isDigit() }.take(10))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                runCatching { autofill?.requestAutofillForNode(autofillNode) }
                            } else {
                                runCatching { autofill?.cancelAutofillForNode(autofillNode) }
                            }
                        },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = Ink900,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(BrandBluePrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    decorationBox = { innerTextField ->
                        if (phoneNumber.isBlank()) {
                            Text(
                                text = stringResource(R.string.auto_phone_number),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.sp,
                                    color = Ink600
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        if (phoneValidationError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = phoneValidationError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (role != UserRole.EMPLOYER) {
            RegisterReferralSection(
                isTelugu = isTelugu,
                textColor = Ink900,
                onValidatedCodeChanged = onValidatedCodeChanged
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Full-width Send OTP pill with arrow
        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = buttonEnabled,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandBluePrimary,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE2E8F0),
                disabledContentColor = Color(0xFF94A3B8)
            )
        ) {
            if (isCheckingPhone || otpState.isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                val ctaColor = if (buttonEnabled) Color.White else Color(0xFF94A3B8)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(18.dp)) // Equal weight balance box
                    Text(
                        text = if (isTelugu) "OTP పంపండి" else "Send OTP",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ctaColor
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ctaColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // "or" divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(CardBorder)
            )
            Text(
                text = if (isTelugu) "లేదా" else "or",
                style = MaterialTheme.typography.bodyMedium.copy(color = Ink600),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(CardBorder)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue with Google (placeholder)
        OutlinedButton(
            onClick = {
                Toast.makeText(
                    appContext,
                    "Google sign-in is coming soon",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, CardBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink900)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isTelugu) "Google తో కొనసాగండి" else "Continue with Google",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink900
                )
            )
        }

        AnimatedVisibility(
            visible = otpState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                com.example.dutype.components.ErrorCard(message = otpState.error)
            }
        }
    }
}

@Composable
private fun RegisterNameField(
    value: String,
    isTelugu: Boolean,
    textColor: Color,
    role: UserRole,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val filtered = if (role == UserRole.EMPLOYER) {
                newValue.filter { it.isLetter() || it.isDigit() || it == ' ' || it == '.' || it == '&' || it == '-' }.take(50)
            } else {
                newValue.filter { it.isLetter() || it == ' ' || it == '.' }.take(50)
            }
            onValueChange(filtered)
        },
        placeholder = {
            Text(
                if (role == UserRole.EMPLOYER) {
                    if (isTelugu) "కంపెనీ పేరు నమోదు చేయండి" else "Enter company name"
                } else {
                    if (isTelugu) "మీ పూర్తి పేరు నమోదు చేయండి" else "Enter full name"
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, color = Ink600)
            )
        },
        leadingIcon = {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Ink400, modifier = Modifier.size(20.dp))
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandBluePrimary,
            unfocusedBorderColor = BrandBlueBorder,
            cursorColor = BrandBluePrimary,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Words
        )
    )
}

@Composable
private fun RegisterReferralSection(
    isTelugu: Boolean,
    textColor: Color,
    onValidatedCodeChanged: (String?) -> Unit
) {
    val appContext = LocalContext.current
    val scope = rememberCoroutineScope()

    var showReferralInput by remember { mutableStateOf(false) }
    var referralCode by remember { mutableStateOf("") }
    var isValidatingCode by remember { mutableStateOf(false) }
    var codeValidationError by remember { mutableStateOf<String?>(null) }
    var validatedReferrerName by remember { mutableStateOf<String?>(null) }

    RegisterReferralCard(
        isTelugu = isTelugu,
        textColor = textColor,
        showReferralInput = showReferralInput,
        referralCode = referralCode,
        isValidatingCode = isValidatingCode,
        codeValidationError = codeValidationError,
        validatedReferrerName = validatedReferrerName,
        onToggle = { showReferralInput = !showReferralInput },
        onCodeChange = { newValue ->
            val filtered = com.example.dutype.models.normalizeReferralCode(newValue)
            referralCode = filtered
            codeValidationError = null
            validatedReferrerName = null
            if (filtered.length in 7..10) {
                onValidatedCodeChanged(filtered)
                scope.launch {
                    try {
                        val referralService = com.example.dutype.di.referralServiceFromHilt(appContext)
                        val validation = referralService.validateReferralCode(filtered)
                        if (referralCode == filtered) {
                            if (validation.isValid) {
                                validatedReferrerName = validation.referrerName
                                codeValidationError = null
                                onValidatedCodeChanged(filtered)
                            } else {
                                codeValidationError = validation.errorMessage
                                validatedReferrerName = null
                            }
                        }
                    } catch (_: Exception) {
                        // Keep candidate code, let server handle during registration
                    }
                }
            } else {
                onValidatedCodeChanged(null)
            }
        },
        onClear = {
            referralCode = ""
            codeValidationError = null
            validatedReferrerName = null
            onValidatedCodeChanged(null)
        },
        onVerify = {
            if (referralCode.length >= 7) {
                isValidatingCode = true
                scope.launch {
                    try {
                        val referralService = com.example.dutype.di.referralServiceFromHilt(appContext)
                        val validation = referralService.validateReferralCode(referralCode)
                        isValidatingCode = false
                        if (validation.isValid) {
                            val normalizedCode = com.example.dutype.models.normalizeReferralCode(referralCode)
                            validatedReferrerName = validation.referrerName
                            codeValidationError = null
                            onValidatedCodeChanged(normalizedCode)
                            Toast.makeText(
                                appContext,
                                if (isTelugu) "✓ ${validation.referrerName} నుండి చెల్లుబాటు అయ్యే కోడ్" else "✓ Valid code from ${validation.referrerName}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            codeValidationError = validation.errorMessage
                            validatedReferrerName = null
                            onValidatedCodeChanged(null)
                            Toast.makeText(
                                appContext,
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
        }
    )
}

@Composable
private fun RegisterReferralCard(
    isTelugu: Boolean,
    textColor: Color,
    showReferralInput: Boolean,
    referralCode: String,
    isValidatingCode: Boolean,
    codeValidationError: String?,
    validatedReferrerName: String?,
    onToggle: () -> Unit,
    onCodeChange: (String) -> Unit,
    onClear: () -> Unit,
    onVerify: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isTelugu) "రిఫరల్ కోడ్ ఉందా?" else "Have a referral code?",
                style = AppTypography.bodyMedium.copy(
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
            )

            TextButton(
                onClick = onToggle,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (showReferralInput) {
                        if (isTelugu) "దాచు" else "Hide"
                    } else {
                        if (isTelugu) "కోడ్ నమోదు" else "Enter Code"
                    },
                    style = AppTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
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
                        onValueChange = onCodeChange,
                        placeholder = {
                            Text(
                                if (isTelugu) "ఉదా: DUTY4F9A" else "e.g. DUTY4F9A",
                                style = AppTypography.bodyMedium.copy(color = Ink400)
                            )
                        },
                        trailingIcon = {
                            when {
                                isValidatingCode -> CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = BrandBluePrimary
                                )
                                validatedReferrerName != null -> Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = if (isTelugu) "చెల్లుబాటు అయ్యింది" else "Valid",
                                    tint = WorkerColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )

                                referralCode.isNotEmpty() -> IconButton(onClick = onClear) {
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
                            .height(56.dp),
                        singleLine = true,
                        isError = codeValidationError != null && referralCode.length >= 7,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = when {
                                validatedReferrerName != null -> WorkerColors.Success
                                codeValidationError != null -> WorkerColors.Error
                                else -> BrandBluePrimary
                            },
                            unfocusedBorderColor = when {
                                validatedReferrerName != null -> WorkerColors.Success
                                codeValidationError != null -> WorkerColors.Error
                                else -> BrandBlueBorder
                            },
                            cursorColor = BrandBluePrimary,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None
                        )
                    )

                    Button(
                        onClick = onVerify,
                        modifier = Modifier.height(56.dp),
                        enabled = referralCode.length >= 7 && !isValidatingCode && validatedReferrerName == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBluePrimary,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Ink400
                        ),
                        shape = RoundedCornerShape(12.dp),
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

                Spacer(modifier = Modifier.height(8.dp))
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
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Back arrow, top-left
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Ink900
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isTelugu) "మీ నంబర్‌ను ధృవీకరించండి" else "Verify your number",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // "+91 98765 43210 · Change number"
        val annotatedText = buildAnnotatedString {
            append("+91 $phoneNumber")
            append("  ·  ")
            pushStringAnnotation(tag = "CHANGE", annotation = "change")
            withStyle(SpanStyle(color = BrandBluePrimary, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                append(if (isTelugu) "నంబర్ మార్చండి" else "Change number")
            }
            pop()
        }

        androidx.compose.foundation.text.ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium.copy(color = Ink600, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                annotatedText.getStringAnnotations(tag = "CHANGE", start = offset, end = offset)
                    .firstOrNull()?.let {
                        onBackClick()
                    }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        AuthOtpBoxes(otpValue = otpValue, onOtpChange = onOtpChange, digitCount = 6)

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (resendCooldownSeconds > 0) {
                val minutes = resendCooldownSeconds / 60
                val seconds = resendCooldownSeconds % 60
                Text(
                    text = if (isTelugu) "OTP మళ్లీ పంపడానికి %d:%02d".format(minutes, seconds)
                    else "Resend OTP in %d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Ink600,
                        fontSize = 14.sp
                    )
                )
            } else {
                TextButton(
                    onClick = onResendClick,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isTelugu) "OTP మళ్లీ పంపండి" else "Resend OTP",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandEmeraldAccent
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val otpButtonEnabled = otpValue.length == 6 && !otpState.isLoading
        val otpCtaColor = if (otpButtonEnabled) Color.White else Color(0xFF94A3B8)

        Button(
            onClick = onVerifyClick,
            enabled = otpButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandBluePrimary,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE2E8F0),
                disabledContentColor = Color(0xFF94A3B8)
            )
        ) {
            if (otpState.isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isTelugu) "ధృవీకరించి కొనసాగండి" else "Verify & Continue",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = otpCtaColor
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = otpCtaColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = otpState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                com.example.dutype.components.ErrorCard(message = otpState.error)
            }
        }
    }
}

// ─── OTP Input Boxes ─────────────────────────────────────────────────────────

// ─── Navigation helpers ──────────────────────────────────────────────────────
