package com.example.dutype.auth

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

private val BrandBluePrimary = Color(0xFF2563EB)
private val BrandBlueLight = Color(0xFFEFF6FF)
private val BrandBlueBorder = Color(0xFFBFDBFE)
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
                                val existingRoleLabel = if (existingRole == "employer") "employer" else "worker"
                                if (isTelugu)
                                    "ఈ నంబర్ ${existingRoleLabel}గా నమోదైంది. దయచేసి ${existingRoleLabel}గా లాగిన్ అవ్వండి."
                                else
                                    "This number is already registered as a $existingRoleLabel. Please log in as a $existingRoleLabel."
                            } else if (msg == "account-not-found") {
                                if (isTelugu) "ఈ నంబర్‌కు సంబంధించిన ఖాతా కనబడలేదు. దయచేసి ముందుగా నమోదు చేయండి." else "No account found with this number. Please Register first."
                            } else {
                                error.message ?: if (isTelugu) "మీ ఖాతాను లోడ్ చేయలేకపోయాం. దయచేసి మళ్లీ ప్రయత్నించండి." else "Could not load your account. Please try again."
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
                .background(SurfaceBg)
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
        // Clean, Seamless Single-Page Design (M3 Container, vertically centered content)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Action Header Bar: Back Arrow | WhatsApp Help & Language Chip (Anchored Top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { safeAuthBackNavigation(navController) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp Help Button
                    Surface(
                        onClick = {
                            val whatsappUrl = "https://wa.me/919121706236?text=Hello%20DutyPe%20Team!%20I%20need%20help%20logging%20in."
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
                        color = MaterialTheme.colorScheme.surface,
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
                                text = stringResource(R.string.auth_help),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }
            }

            // Main Vertically Centered Content Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 48.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Title & Subtitle Hero Block
                Text(
                    text = stringResource(R.string.auth_welcome_back),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.auth_sign_in_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Inline Input Section (Part of the screen, no elevated card box)
                Text(
                    text = stringResource(R.string.auth_mobile_number),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Outlined Phone Number Box with Blue Accent Border
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, if (phoneNumber.isNotEmpty()) BrandBluePrimary else BrandBlueBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedCountryCode,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Ink900
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Ink600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

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
                                            color = Ink400
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
                                if (phoneCheck.exists == FirestoreUtils.PhoneExistenceResult.NOT_EXISTS) {
                                    isCheckingPhone = false
                                    Toast.makeText(context, if (isTelugu) "ఈ నంబర్‌కు ఖాతా లేదు. దయచేసి నమోదు చేయండి." else "No account found with this number. Please Register first.", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                isCheckingPhone = false
                                profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                otpViewModel.sendOtp(fullPhoneNumber, context)
                            } catch (e: Exception) {
                                isCheckingPhone = false
                                Toast.makeText(context, "Verification failed. Please try again.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = buttonEnabled,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBluePrimary,
                        contentColor = Color.White,
                        disabledContainerColor = BrandBlueBorder,
                        disabledContentColor = Color.White
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

                Spacer(modifier = Modifier.height(28.dp))

                // Footer Registration Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.auth_dont_have_account),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                color = BrandBluePrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.auth_enter_otp),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        val enterSmsCodePrefix = stringResource(R.string.auth_enter_sms_code)
        val changeNumberText = stringResource(R.string.auth_change_number)

        val annotatedText = buildAnnotatedString {
            append(enterSmsCodePrefix)
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)) {
                append("+91 $phoneNumber")
            }
            append("  ")
            pushStringAnnotation(tag = "CHANGE", annotation = "change")
            withStyle(SpanStyle(color = BrandBluePrimary, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                append(changeNumberText)
            }
            pop()
        }

        androidx.compose.foundation.text.ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                annotatedText.getStringAnnotations(tag = "CHANGE", start = offset, end = offset)
                    .firstOrNull()?.let {
                        onBackClick()
                    }
            }
        )

        Spacer(modifier = Modifier.height(28.dp))


        // ────────────────────────────────────────────────────────────────────

        // Clean 6-Digit OTP Box Layout
        AuthOtpBoxes(
            otpValue = otpValue,
            onOtpChange = onOtpChange,
            digitCount = 6
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Verify OTP Button
        Button(
            onClick = onVerifyClick,
            enabled = otpValue.length == 6 && !otpState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandBluePrimary,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFCBD5E1),
                disabledContentColor = Color.White
            )
        ) {
            if (otpState.isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = stringResource(R.string.auth_verify_otp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Resend OTP Row with Timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.auth_didnt_receive_code),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            if (resendCooldownSeconds > 0) {
                Text(
                    text = stringResource(R.string.auth_resend_in, resendCooldownSeconds),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                            color = BrandBluePrimary
                        )
                    )
                }
            }
        }
    }
}
