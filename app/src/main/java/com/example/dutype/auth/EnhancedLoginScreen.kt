package com.example.dutype.auth

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.example.dutype.utils.FirestoreUtils
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.utils.isDebuggableBuild
import com.example.dutype.viewmodels.OtpViewModel
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import com.example.dutype.ui.theme.WorkerColors

private val BrandBluePrimary = Color(0xFF0F0F0F)
private val BrandBlueLight = Color(0xFFF8FAFC)
private val BrandBlueBorder = Color(0xFFE2E8F0)
private val BrandEmeraldAccent = Color(0xFF10B981)
private val Ink900 = Color(0xFF0F172A)
private val Ink600 = Color(0xFF475569)
private val Ink400 = Color(0xFF94A3B8)
private val CardBorder = Color(0xFFE2E8F0)
private val SurfaceBg = Color(0xFFF8FAFC)

@Composable
fun EnhancedLoginScreen(
    navController: NavController,
    skipRoleSelection: Boolean = true,
    initialRole: String = "WORKER",
    isRegisterMode: Boolean = false,
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
        otpViewModel.setRoleContext(selectedRole)
    }

    OtpLoginScreen(
        role = selectedRole,
        otpViewModel = otpViewModel,
        profileCompletionViewModel = profileCompletionViewModel,
        navController = navController
    )
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class)
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

    LaunchedEffect(otpState.otpVerified) {
        if (otpState.otpVerified) {
            Timber.d("📱 OTP VERIFICATION SUCCESS - Starting user check flow")

            try {
                val currentUser = FirebaseAuth.getInstance().currentUser

                if (currentUser != null) {
                    val loginResult = otpViewModel.completeLogin(role)
                    loginResult.fold(
                        onSuccess = { outcome ->
                            profileCompletionViewModel.saveUserInfoToLocalStorage(
                                email = "",
                                name = "",
                                role = outcome.role
                            )
                            if (outcome.destination == OtpViewModel.PostOtpDestination.HOME) {
                                profileCompletionViewModel.markProfileComplete(outcome.role)
                                profileCompletionViewModel.markProfileSetupAsShown(outcome.role)
                                navigateToHome(outcome.role, navController)
                            } else {
                                navigateToProfileSetup(outcome.role, navController)
                            }
                        },
                        onFailure = { error ->
                            Timber.e(error, "OTP VERIFICATION SUCCESS - Login resolution failed")
                            val msg = error.message.orEmpty()
                            val toastText = if (msg.startsWith("phone-already-registered-as:")) {
                                val existingRole = msg.substringAfter(":").lowercase()
                                val existingRoleLabel = if (existingRole == "employer") context.getString(R.string.employer) else context.getString(R.string.worker)
                                context.getString(R.string.auth_phone_registered_as_role, existingRoleLabel, existingRoleLabel)
                            } else if (msg == "account-not-found") {
                                context.getString(R.string.auth_no_account_found)
                            } else {
                                error.message ?: context.getString(R.string.auth_verification_failed)
                            }
                            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                        }
                    )
                    otpViewModel.resetState()
                    return@LaunchedEffect
                } else {
                    navController.navigate(Routes.SELECT_ROLE) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "📱 Error checking profile")
                navigateToProfileSetup(role, navController)
            }

            otpViewModel.resetState()
        }
    }

    if (otpState.otpSent) {
        // ── Enterprise SMS Auto-Retrieval ────────────────────────────────────
        // Start listening for incoming SMS the moment the OTP screen shows.
        // DisposableEffect guarantees cleanup on back-press / recomposition.
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
                otpValue = autoCode   // fill the UI boxes
                // verifyOtp is already called by onSmsAutoRetrieved; no double-call needed
            }
        }
        // ────────────────────────────────────────────────────────────────────

        // Full screen OTP entry
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBg.bg())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            val resendCooldown by otpViewModel.resendCooldownSeconds.collectAsState()
            OtpInputSection(
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
        // Phone-entry screen — matches the Stitch design spec pixel-for-pixel:
        // title, subtitle, flag+code input, Send OTP pill, "or" divider,
        // Continue with Google, and the Terms/Privacy footer pinned to the bottom.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.bg())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                // Top Action Header Bar: WhatsApp Help (End aligned, matching RegisterScreen)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            val whatsappUrl = "https://wa.me/918500717800?text=Hello%20DutyPe%20Team!%20I%20need%20help%20logging%20in."
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

                // Pushes the form comfortably lower down toward the middle of the screen.
                Spacer(modifier = Modifier.height(authTopGap(0.18f) + 20.dp))

                // Title & Subtitle Hero Block
                Text(
                    text = stringResource(R.string.auth_welcome_back),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink900.fg()
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.auth_enter_mobile_number),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Ink600.fg(),
                        fontSize = 15.sp
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Outlined Phone Number Box: 🇮🇳 flag | +91 | divider | number
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.bg(),
                    border = BorderStroke(if (phoneNumber.isNotEmpty()) 2.dp else 1.dp, if (phoneNumber.isNotEmpty()) BrandBluePrimary.bd() else BrandBlueBorder.bd())
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
                                    phoneNumber = digits
                                }
                            )
                        }
                        LaunchedEffect(autofillNode) {
                            autofillTree += autofillNode
                        }

                        BasicTextField(
                            value = phoneNumber,
                            onValueChange = { newValue ->
                                phoneNumber = newValue.filter { it.isDigit() }.take(10)
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

                Spacer(modifier = Modifier.height(24.dp))

                val buttonEnabled = ValidationUtils.isValidIndianPhoneNumber(phoneNumber) && !otpState.isLoading && !isCheckingPhone

                // Full-width Send OTP Pill Button with Arrow at the end
                Button(
                    onClick = {
                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                        isCheckingPhone = true
                        scope.launch {
                            try {
                                val phoneCheck = FirestoreUtils.checkPhoneForRole(
                                    phoneNumber = fullPhoneNumber,
                                    requestedRole = role.name
                                )
                                when (phoneCheck.exists) {
                                    FirestoreUtils.PhoneExistenceResult.NOT_EXISTS -> {
                                        isCheckingPhone = false
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.auth_no_account_found),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch
                                    }
                                    FirestoreUtils.PhoneExistenceResult.EXISTS -> {
                                        if (phoneCheck.roleConflict) {
                                            isCheckingPhone = false
                                            val existingRoleLabel = when (phoneCheck.existingRole?.uppercase()) {
                                                "WORKER" -> context.getString(R.string.worker)
                                                "EMPLOYER" -> context.getString(R.string.employer)
                                                else -> context.getString(R.string.select_role)
                                            }
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.auth_phone_registered_as_role, existingRoleLabel, existingRoleLabel),
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }
                                    }
                                    FirestoreUtils.PhoneExistenceResult.UNKNOWN -> {
                                        isCheckingPhone = false
                                        Toast.makeText(
                                            context,
                                            FirestoreUtils.unknownMessage(context, phoneCheck),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch
                                    }
                                }
                                isCheckingPhone = false
                                profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                otpViewModel.sendOtp(fullPhoneNumber, context)
                            } catch (e: Exception) {
                                isCheckingPhone = false
                                Toast.makeText(context, context.getString(R.string.auth_verification_failed), Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = buttonEnabled,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBluePrimary.bg(),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE2E8F0).bg(),
                        disabledContentColor = Color(0xFF475569).fg()
                    )
                ) {
                    if (isCheckingPhone || otpState.isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(modifier = Modifier.size(18.dp)) // Empty space balancer
                            Text(
                                text = stringResource(R.string.auth_send_otp),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // DEV-ONLY: Skip OTP and jump straight to the role's home screen.
                // Only rendered in debuggable builds (never shows in a release/Play build).
                // This bypasses real Firebase Auth, so anything reading FirebaseAuth's
                // currentUser downstream may behave as "not logged in" - it's a UI/navigation
                // shortcut for testing screens, not a real sign-in.
                if (LocalContext.current.isDebuggableBuild()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { navigateToHome(role, navController) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WorkerColors.TextSecondary
                        ),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1).bd())
                    ) {
                        Text(
                            text = "Skip (Dev) → ${role.name} home",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Terms & Privacy footer + Register link placed close below the login section
                Spacer(modifier = Modifier.height(28.dp))

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

                // Footer Registration Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.auth_dont_have_account),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Ink600.fg())
                    )
                    TextButton(
                        onClick = {
                            navController.navigate("${Routes.REGISTER}?role=${role.name}") {
                                popUpTo("${Routes.ENHANCED_LOGIN}?role=${role.name}") { inclusive = true }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auth_create_account),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandBluePrimary.fg()
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

@Composable
private fun OtpInputSection(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    phoneNumber: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit,
    resendCooldownSeconds: Int = 0
) {
    val context = LocalContext.current
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU

    // Pulsing alpha for the "Reading SMS" indicator
    val infiniteTransition = rememberInfiniteTransition(label = "sms_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Back arrow, top-left — matches the Stitch design spec exactly
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
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
        val changeNumberText = stringResource(R.string.auth_change_number_action)

        val annotatedText = buildAnnotatedString {
            append("+91 $phoneNumber")
            append("  ·  ")
            pushStringAnnotation(tag = "CHANGE", annotation = "change")
            withStyle(SpanStyle(color = BrandBluePrimary.fg(), fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                append(changeNumberText)
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

        // Clean 6-Digit OTP Box Layout
        AuthOtpBoxes(
            otpValue = otpValue,
            onOtpChange = onOtpChange,
            digitCount = 6
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Resend OTP line — single centered line, exactly as in the design spec:
        // "Resend OTP in 0:28" while cooling down, tappable "Resend OTP" after.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (resendCooldownSeconds > 0) {
                val minutes = resendCooldownSeconds / 60
                val seconds = resendCooldownSeconds % 60
                Text(
                    text = stringResource(R.string.auth_resend_otp_in, minutes, seconds),
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
                        text = stringResource(R.string.auth_resend_otp),
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

        // Verify & Continue button — grayed out until all 6 digits are entered
        Button(
            onClick = onVerifyClick,
            enabled = otpValue.length == 6 && !otpState.isLoading,
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
                            color = if (otpValue.length == 6 && !otpState.isLoading) Color.White else Color(0xFF94A3B8).fg()
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (otpValue.length == 6 && !otpState.isLoading) Color.White else Color(0xFF94A3B8).fg(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
