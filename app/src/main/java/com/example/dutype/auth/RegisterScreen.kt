package com.example.dutype.auth

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
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
import androidx.compose.material.icons.filled.Business
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
    /** Employers only: the name typed is a person (INDIVIDUAL) or a business (COMPANY). */
    var employerType by remember { mutableStateOf(com.example.dutype.firestore.FirestoreSchema.Values.EmployerType.INDIVIDUAL) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    var isCheckingPhone by remember { mutableStateOf(false) }
    var showLanguageBottomSheet by remember { mutableStateOf(false) }
    var truecallerBusy by remember { mutableStateOf(false) }
    /** Email shared through Truecaller sign-up (kept locally; the server saves it on the profile). */
    var truecallerEmail by remember { mutableStateOf("") }

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

    // Handle OTP verification success — unified registration / login flow
    LaunchedEffect(otpState.otpVerified) {
        if (otpState.otpVerified) {
            Timber.d("📱 REGISTER - OTP verified, resolving user profile")
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val pendingReferralCode = profileCompletionViewModel.getReferralCode()
                    val resolvedName = fullName.trim().ifBlank { otpViewModel.lastTruecallerResult?.name.orEmpty() }.ifBlank { "User" }
                    val authResult = otpViewModel.resolveUnifiedAuth(
                        role = role,
                        fullName = resolvedName,
                        referralCode = pendingReferralCode,
                        employerType = employerType.takeIf { role == UserRole.EMPLOYER }
                    )

                    authResult.fold(
                        onSuccess = { outcome ->
                            profileCompletionViewModel.saveUserInfoToLocalStorage(
                                email = truecallerEmail.ifBlank { otpViewModel.lastTruecallerResult?.email.orEmpty() },
                                name = resolvedName,
                                role = role
                            )

                            // completeRegistration already applied the referral code server-side.
                            if (!pendingReferralCode.isNullOrBlank()) profileCompletionViewModel.clearReferralCode()

                            otpViewModel.resetState()
                            if (outcome.destination == OtpViewModel.PostOtpDestination.HOME) {
                                profileCompletionViewModel.markProfileComplete(outcome.role)
                                profileCompletionViewModel.markProfileSetupAsShown(outcome.role)
                                val homeRoute = when (role) {
                                    UserRole.WORKER -> Routes.WORKER_HOME
                                    UserRole.EMPLOYER -> Routes.EMPLOYER_HOME
                                    else -> Routes.SELECT_ROLE
                                }
                                navController.navigate(homeRoute) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            } else {
                                navigateToProfileSetup(role, navController)
                            }
                        },
                        onFailure = { error ->
                            val msg = error.message.orEmpty()
                            val toastText = if (msg.startsWith("phone-already-registered-as:")) {
                                val existingRole = msg.substringAfter(":").lowercase()
                                val existingRoleLabel = if (existingRole == "employer") context.getString(R.string.employer) else context.getString(R.string.worker)
                                context.getString(R.string.auth_phone_registered_as_role, existingRoleLabel, existingRoleLabel)
                            } else {
                                error.message ?: context.getString(R.string.auth_verification_failed)
                            }
                            Timber.e(error, "REGISTER - Auth resolution failed")
                            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                            otpViewModel.resetState()
                        }
                    )
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

    LaunchedEffect(otpState.otpSent) {
        if (otpState.otpSent) {
            val toastMessage = if (otpState.channel == com.example.dutype.viewmodels.OtpChannel.WHATSAPP) {
                context.getString(R.string.auth_code_sent_whatsapp)
            } else {
                context.getString(R.string.auth_code_sent_sms)
            }
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
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
                .background(Color.White.bg())
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.bg())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Top Action Header Bar: WhatsApp Help & Language Chip (End aligned, no back arrow)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
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
                        color = Color.White.bg(),
                        border = BorderStroke(1.dp, CardBorder.bd())
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = null,
                                tint = Color(0xFF25D366).fg(),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.auth_help),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = Ink900.fg())
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            Text(
                text = stringResource(R.string.auth_create_your_account),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink900.fg()
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
            RegisterInputSection(
                fullName = fullName,
                onFullNameChange = { fullName = it },
                employerType = employerType,
                onEmployerTypeChange = { employerType = it },
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
                                        "WORKER" -> context.getString(R.string.worker)
                                        "EMPLOYER" -> context.getString(R.string.employer)
                                        else -> null
                                    }
                                    val message = if (existingRoleLabel != null) {
                                        context.getString(R.string.auth_phone_registered_as_role, existingRoleLabel, existingRoleLabel)
                                    } else {
                                        context.getString(R.string.auth_already_registered)
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
                                    Timber.w("Phone check returned UNKNOWN; proceeding with OTP registration for $fullPhoneNumber")
                                    isCheckingPhone = false
                                    profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                    profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                    profileCompletionViewModel.saveUserInfoToLocalStorage(email = "", name = fullName.trim(), role = role)
                                    otpViewModel.sendOtp(fullPhoneNumber, context)
                                }
                            }
                        } catch (e: Exception) {
                            isCheckingPhone = false
                            Toast.makeText(context, context.getString(R.string.auth_verification_failed), Toast.LENGTH_LONG).show()
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

            // Truecaller one-tap sign-up / login: verified number + name (+ email) without an SMS.
            TruecallerLoginButton(
                busy = truecallerBusy || otpState.isLoading || isCheckingPhone,
                onAuthorized = { code, verifier ->
                    truecallerBusy = true
                    scope.launch {
                        otpViewModel.truecallerExchange(code, verifier, mode = "unified").fold(
                            onSuccess = { tc ->
                                truecallerBusy = false
                                phoneNumber = tc.phone.removePrefix("+91")
                                if (fullName.isBlank()) fullName = tc.name
                                when {
                                    tc.roleConflict || (tc.existingRole != null && tc.existingRole != role.name) -> {
                                        val label = context.getString(
                                            if (tc.existingRole == "EMPLOYER") R.string.employer else R.string.worker
                                        )
                                        Toast.makeText(
                                            context, context.getString(R.string.auth_phone_registered_as_role, label, label), Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    tc.allowed -> {
                                        truecallerEmail = tc.email
                                        profileCompletionViewModel.saveAuthMethod("TRUECALLER")
                                        profileCompletionViewModel.savePhoneNumber(tc.phone)
                                        profileCompletionViewModel.saveUserInfoToLocalStorage(
                                            email = tc.email,
                                            name = fullName.trim().ifBlank { tc.name }.ifBlank { "User" },
                                            role = role
                                        )
                                        otpViewModel.signInWithTruecaller(tc)
                                    }
                                    else -> Toast.makeText(context, context.getString(R.string.auth_truecaller_failed), Toast.LENGTH_LONG).show()
                                }
                            },
                            onFailure = {
                                truecallerBusy = false
                                Toast.makeText(context, context.getString(R.string.auth_truecaller_failed), Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                onFailed = {
                    Toast.makeText(context, context.getString(R.string.auth_truecaller_failed), Toast.LENGTH_LONG).show()
                }
            )

            Spacer(modifier = Modifier.height(36.dp))

            val termsPrefix = stringResource(R.string.auth_by_continuing_agree)
            val termsTitle = stringResource(R.string.auth_terms)
            val privacyTitle = stringResource(R.string.auth_privacy_policy_short)
            val termsAnnotated = buildAnnotatedString {
                append(termsPrefix)
                pushStringAnnotation(tag = "TERMS", annotation = "terms")
                withStyle(SpanStyle(color = Ink900.fg(), fontWeight = FontWeight.SemiBold)) {
                    append(termsTitle)
                }
                pop()
                append(" & ")
                pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                withStyle(SpanStyle(color = Ink900.fg(), fontWeight = FontWeight.SemiBold)) {
                    append(privacyTitle)
                }
                pop()
            }
            androidx.compose.foundation.text.ClickableText(
                text = termsAnnotated,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Ink600.fg(),
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auth_already_have_account),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Ink600.fg())
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
                        text = stringResource(R.string.auth_login),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandBluePrimary.fg()
                        )
                    )
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
    employerType: String,
    onEmployerTypeChange: (String) -> Unit,
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
        employerType = employerType,
        onEmployerTypeChange = onEmployerTypeChange,
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
    employerType: String,
    onEmployerTypeChange: (String) -> Unit,
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

        val isCompany = role == UserRole.EMPLOYER &&
            employerType == com.example.dutype.firestore.FirestoreSchema.Values.EmployerType.COMPANY
        if (role == UserRole.EMPLOYER) {
            // Same choice as the employer profile setup: hiring as a person, or as a business.
            com.example.dutype.employer.screens.StitchSegmentedToggle(
                isIndividual = !isCompany,
                onIndividualSelected = { onEmployerTypeChange(com.example.dutype.firestore.FirestoreSchema.Values.EmployerType.INDIVIDUAL) },
                onCompanySelected = { onEmployerTypeChange(com.example.dutype.firestore.FirestoreSchema.Values.EmployerType.COMPANY) },
                individualLabel = stringResource(R.string.auth_personal_individual),
                companyLabel = stringResource(R.string.auth_company_business)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Name label: person or business
        Text(
            text = when {
                isCompany -> stringResource(R.string.auth_company_shop_name)
                role == UserRole.EMPLOYER -> stringResource(R.string.auth_your_full_name)
                else -> stringResource(R.string.auth_full_name)
            },
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Ink600.fg()
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        RegisterNameField(
            value = fullName,
            isTelugu = isTelugu,
            textColor = Ink900.fg(),
            role = role,
            isCompany = isCompany,
            onValueChange = onFullNameChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Number label
        Text(
            text = stringResource(R.string.auth_mobile_number),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Ink600.fg()
            )
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Outlined Phone Number Box: flag | +91 | divider | number
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.bg(),
            border = BorderStroke(
                if (phoneNumber.isNotEmpty()) 2.dp else 1.dp,
                if (phoneValidationError != null) WorkerColors.Error
                else if (phoneNumber.isNotEmpty()) BrandBluePrimary.bd()
                else BrandBlueBorder.bd()
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
                        color = Ink900.fg()
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(CardBorder.bg())
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
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = Ink900.fg(),
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(BrandBluePrimary.fg()),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    decorationBox = { innerTextField ->
                        if (phoneNumber.isBlank()) {
                            Text(
                                text = stringResource(R.string.auto_phone_number),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.sp,
                                    color = Ink600.fg()
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
                textColor = Ink900.fg(),
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
                containerColor = BrandBluePrimary.bg(),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE2E8F0).bg(),
                disabledContentColor = Color(0xFF94A3B8).fg()
            )
        ) {
            if (isCheckingPhone || otpState.isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                val ctaColor = if (buttonEnabled) Color.White else Color(0xFF94A3B8).fg()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(18.dp)) // Equal weight balance box
                    Text(
                        text = stringResource(R.string.auth_send_otp),
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
    isCompany: Boolean,
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
                text = if (isCompany) stringResource(R.string.auth_company_name_hint) else stringResource(R.string.auth_enter_full_name_hint),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, color = Ink600.fg())
            )
        },
        leadingIcon = {
            Icon(if (isCompany) Icons.Filled.Business else Icons.Filled.Person, contentDescription = null, tint = Ink400.fg(), modifier = Modifier.size(20.dp))
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandBluePrimary.bd(),
            unfocusedBorderColor = BrandBlueBorder.bd(),
            cursorColor = BrandBluePrimary.fg(),
            focusedContainerColor = Color.White.bg(),
            unfocusedContainerColor = Color.White.bg()
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
                                appContext.getString(R.string.auth_valid_code_from, validation.referrerName),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            codeValidationError = validation.errorMessage
                            validatedReferrerName = null
                            onValidatedCodeChanged(null)
                            Toast.makeText(
                                appContext,
                                validation.errorMessage ?: appContext.getString(R.string.auth_invalid_code),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        isValidatingCode = false
                        codeValidationError = appContext.getString(R.string.auth_verification_failed)
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
                text = stringResource(R.string.auth_have_referral_code),
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
                    text = if (showReferralInput) stringResource(R.string.auth_hide) else stringResource(R.string.auth_enter_code),
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
                                stringResource(R.string.auth_referral_hint),
                                style = AppTypography.bodyMedium.copy(color = Ink400.fg())
                            )
                        },
                        trailingIcon = {
                            when {
                                isValidatingCode -> CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = BrandBluePrimary.fg()
                                )
                                validatedReferrerName != null -> Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = stringResource(R.string.valid),
                                    tint = WorkerColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )

                                referralCode.isNotEmpty() -> IconButton(onClick = onClear) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                        contentDescription = stringResource(R.string.clear),
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
                            cursorColor = BrandBluePrimary.fg(),
                            focusedContainerColor = Color.White.bg(),
                            unfocusedContainerColor = Color.White.bg()
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
                            containerColor = BrandBluePrimary.bg(),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE2E8F0).bg(),
                            disabledContentColor = Ink400.fg()
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isValidatingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(
                                text = if (validatedReferrerName != null) "✓" else stringResource(R.string.verify),
                                style = AppTypography.buttonMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                when {
                    validatedReferrerName != null -> Text(
                        stringResource(R.string.auth_valid_code_from, validatedReferrerName),
                        style = AppTypography.caption.copy(color = WorkerColors.Success)
                    )
                    codeValidationError != null && referralCode.length >= 7 -> Text(
                        codeValidationError ?: stringResource(R.string.auth_invalid_code),
                        style = AppTypography.caption.copy(color = WorkerColors.Error)
                    )
                    else -> Text(
                        stringResource(R.string.auth_referral_explanation),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Back arrow, top-left
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Ink900.fg()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.auth_verify_your_number),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900.fg()
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // "+91 98765 43210 · Change number"
        val changeNumberLabel = stringResource(R.string.auth_change_number_action)
        val annotatedText = buildAnnotatedString {
            append("+91 $phoneNumber")
            append("  ·  ")
            pushStringAnnotation(tag = "CHANGE", annotation = "change")
            withStyle(SpanStyle(color = BrandBluePrimary.fg(), fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                append(changeNumberLabel)
            }
            pop()
        }

        androidx.compose.foundation.text.ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium.copy(color = Ink600.fg(), fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                annotatedText.getStringAnnotations(tag = "CHANGE", start = offset, end = offset)
                    .firstOrNull()?.let {
                        onBackClick()
                    }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (otpState.channel == com.example.dutype.viewmodels.OtpChannel.WHATSAPP) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_whatsapp),
                    contentDescription = null,
                    tint = Color(0xFF25D366).fg(),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.auth_code_sent_whatsapp),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Ink600.fg(), fontSize = 14.sp)
                )
            }
        }

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
                    text = stringResource(
                        if (otpState.channel == com.example.dutype.viewmodels.OtpChannel.WHATSAPP) R.string.auth_get_sms_in else R.string.auth_resend_otp_in,
                        minutes, seconds
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Ink600.fg(),
                        fontSize = 14.sp
                    )
                )
            } else {
                TextButton(
                    onClick = onResendClick,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (otpState.channel == com.example.dutype.viewmodels.OtpChannel.WHATSAPP) R.string.auth_get_sms_instead else R.string.auth_resend_otp
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandEmeraldAccent.fg()
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        val otpButtonEnabled = otpValue.length == 6 && !otpState.isLoading
        val otpCtaColor = if (otpButtonEnabled) Color.White else Color(0xFF94A3B8).fg()

        Button(
            onClick = onVerifyClick,
            enabled = otpButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandBluePrimary.bg(),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE2E8F0).bg(),
                disabledContentColor = Color(0xFF94A3B8).fg()
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
                        text = stringResource(R.string.auth_verify_and_continue),
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
