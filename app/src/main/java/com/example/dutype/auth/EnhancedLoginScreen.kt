package com.example.dutype.auth

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.LanguageSelectionBottomSheet
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.viewmodels.OtpViewModel
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

// Brand Palette matching PhonePe Business design
private val BrandBlue = Color(0xFF0077F0)
private val BrandGreen = Color(0xFF007A3D)
private val BrandGreenLight = Color(0xFFE8F5E9)
private val WhatsAppGreen = Color(0xFF25D366)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val BorderNormal = Color(0xFFCBD5E1)
private val BorderFaint = Color(0xFFE2E8F0)
private val DisabledGray = Color(0xFFBDBDBD)

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

@Composable
private fun OtpLoginScreen(
    role: UserRole,
    otpViewModel: OtpViewModel,
    profileCompletionViewModel: ProfileCompletionViewModel,
    navController: NavController
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    var phoneHasFocus by remember { mutableStateOf(false) }
    var isNavigatingAway by remember { mutableStateOf(false) }
    var isTruecallerBusy by remember { mutableStateOf(false) }

    val selectedCountryCode = "+91"
    val context = LocalContext.current
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU
    val scope = rememberCoroutineScope()
    val otpState by otpViewModel.otpState.collectAsState()
    val resendCooldown by otpViewModel.resendCooldownSeconds.collectAsState()

    // Eagerly prime Truecaller SDK so isOAuthFlowUsable resolves without waiting for button click
    LaunchedEffect(Unit) {
        TruecallerAuth.init(context)
    }

    val phoneFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            otpViewModel.resetState()
        }
    }

    val smsRetrieverHelper = remember {
        SmsAutoRetrieverHelper(
            context = context,
            onOtpRetrieved = { code ->
                otpViewModel.onSmsAutoRetrieved(code, context)
            },
            onError = { _ ->
                otpViewModel.onSmsRetrieverStopped()
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            smsRetrieverHelper.unregisterReceiver()
            otpViewModel.onSmsRetrieverStopped()
        }
    }

    // Light Status Bar with dark icons
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val originalLightStatusBars = insetsController?.isAppearanceLightStatusBars ?: true
        val originalLightNavBars = insetsController?.isAppearanceLightNavigationBars ?: true

        insetsController?.isAppearanceLightStatusBars = true
        insetsController?.isAppearanceLightNavigationBars = true
        window?.statusBarColor = android.graphics.Color.WHITE
        window?.navigationBarColor = android.graphics.Color.WHITE

        onDispose {
            insetsController?.isAppearanceLightStatusBars = originalLightStatusBars
            insetsController?.isAppearanceLightNavigationBars = originalLightNavBars
        }
    }

    // Auto-focus phone number field on initial composition
    LaunchedEffect(otpState.otpSent) {
        if (!otpState.otpSent && !isNavigatingAway && !otpState.otpVerified) {
            delay(150)
            runCatching {
                phoneFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }

    // Auto-redirect if user is already signed in and not in the middle of entering OTP
    val currentAuthUser = FirebaseAuth.getInstance().currentUser
    LaunchedEffect(currentAuthUser, isNavigatingAway, otpState.otpVerified) {
        if (currentAuthUser != null && !otpState.otpSent && !isNavigatingAway && !otpState.otpVerified) {
            navigateToHome(role, navController)
        }
    }

    BackHandler {
        if (otpState.otpSent) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            otpViewModel.resetState()
        } else {
            safeAuthBackNavigation(navController)
        }
    }

    // OTP Verification Success -> Unified Auth Resolution
    LaunchedEffect(otpState.otpVerified) {
        if (otpState.otpVerified) {
            isNavigatingAway = true
            Timber.d("📱 OTP VERIFICATION SUCCESS - Starting unified auth resolution")
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val authResult = otpViewModel.resolveUnifiedAuth(role = role)
                    authResult.fold(
                        onSuccess = { outcome ->
                            val resolvedName = outcome.user?.name?.trim()?.takeIf { it.isNotBlank() && !it.equals("User", ignoreCase = true) }
                                ?: otpViewModel.lastTruecallerResult?.name?.trim()?.takeIf { it.isNotBlank() }
                            profileCompletionViewModel.saveUserInfoToLocalStorage(
                                email = otpViewModel.lastTruecallerResult?.email ?: "",
                                name = resolvedName ?: "",
                                role = outcome.role
                            )
                            otpViewModel.lastTruecallerResult?.photoUrl?.trim()?.takeIf { it.isNotBlank() }?.let { pic ->
                                profileCompletionViewModel.saveUserPhotoUrl(pic)
                            }
                            if (outcome.destination == OtpViewModel.PostOtpDestination.HOME) {
                                profileCompletionViewModel.markProfileComplete(outcome.role)
                                profileCompletionViewModel.markProfileSetupAsShown(outcome.role)
                                val welcomeMessage = if (!resolvedName.isNullOrBlank()) {
                                    context.getString(R.string.auth_welcome_back_user, resolvedName)
                                } else {
                                    context.getString(R.string.auth_welcome_back)
                                }
                                Toast.makeText(context, welcomeMessage, Toast.LENGTH_SHORT).show()
                                navigateToHome(outcome.role, navController)
                            } else {
                                navigateToProfileSetup(outcome.role, navController)
                            }
                        },
                        onFailure = { error ->
                            isNavigatingAway = false
                            isTruecallerBusy = false
                            Timber.e(error, "OTP VERIFICATION SUCCESS - Auth resolution failed")
                            val msg = error.message.orEmpty()
                            val toastText = if (msg.contains("phone-already-registered-as:")) {
                                val existingRole = msg.substringAfter("phone-already-registered-as:").substringBefore(" ").substringBefore("\n").trim().lowercase()
                                val existingRoleLabel = if (existingRole.contains("employer")) context.getString(R.string.employer) else context.getString(R.string.worker)
                                context.getString(R.string.auth_phone_registered_as_role, existingRoleLabel, existingRoleLabel)
                            } else {
                                error.message ?: context.getString(R.string.auth_verification_failed)
                            }
                            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                            otpViewModel.resetState()
                        }
                    )
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
        }
    }

    LaunchedEffect(otpState.otpSent) {
        if (otpState.otpSent) {
            val toastMessage = context.getString(R.string.auth_code_sent_whatsapp)
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(otpState.error) {
        val err = otpState.error
        if (!err.isNullOrBlank()) isTruecallerBusy = false
        if (!err.isNullOrBlank()) {
            val toastText = if (err.contains("phone-already-registered-as:")) {
                val existingRole = err.substringAfter("phone-already-registered-as:").substringBefore(" ").substringBefore("\n").trim().lowercase()
                val existingRoleLabel = if (existingRole.contains("employer")) {
                    context.getString(R.string.employer)
                } else {
                    context.getString(R.string.worker)
                }
                context.getString(R.string.auth_phone_registered_as_role, existingRoleLabel, existingRoleLabel)
            } else if (err == "account-not-found") {
                context.getString(R.string.auth_no_account_found)
            } else {
                err
            }
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
        }
    }

    // Auto-fill OTP field when Play Services delivers the SMS code
    LaunchedEffect(otpState.autoRetrievedOtp) {
        val autoCode = otpState.autoRetrievedOtp
        if (!autoCode.isNullOrBlank() && autoCode.length == 6) {
            otpValue = autoCode
        }
    }



    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            if (!otpState.otpSent) {
                // =========================================================================
                // SCREEN 1: Enter Your Mobile Number (PhonePe Business replicated)
                // =========================================================================
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top App Header: Logo + Help & Support Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DutyPeBrandBadge()

                        // Help & Support pill button
                        Surface(
                            onClick = {
                                navController.navigate(Routes.CONTACT_US)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderNormal)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "Help & Support",
                                    tint = TextDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTelugu) "సహాయం" else "Help",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDark
                                )
                            }
                        }
                    }

                    // Main Content (Enter Mobile Number) - 22.dp padding for slightly narrower input width
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 22.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = if (isTelugu) "మీ మొబైల్ నంబర్‌ను నమోదు చేయండి" else "Enter Your Mobile Number",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isTelugu) "లాగిన్ లేదా సైన్ అప్ చేయడానికి దయచేసి మొబైల్ నంబర్‌ను నమోదు చేయండి" else "Please enter the mobile number for login or signup",
                            fontSize = 14.5.sp,
                            color = TextMuted,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        // Large rounded input box with black focused border
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.8.dp,
                                    color = if (phoneHasFocus) Color(0xFF0F0F0F) else BorderNormal,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .background(Color.White)
                                .clickable {
                                    phoneFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "+91  ",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )

                                BasicTextField(
                                    value = phoneNumber,
                                    onValueChange = { newValue ->
                                        phoneNumber = newValue.filter { it.isDigit() }.take(10)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(phoneFocusRequester)
                                        .onFocusChanged { phoneHasFocus = it.isFocused },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        letterSpacing = 1.2.sp
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF0F0F0F)),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }),
                                    decorationBox = { innerTextField ->
                                        if (phoneNumber.isEmpty()) {
                                            Text(
                                                text = "987654321",
                                                style = TextStyle(
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BorderNormal,
                                                    letterSpacing = 1.2.sp
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Divider OR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = BorderFaint,
                                thickness = 1.dp
                            )
                            Text(
                                text = if (isTelugu) "లేదా" else "OR",
                                modifier = Modifier.padding(horizontal = 14.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = BorderFaint,
                                thickness = 1.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Solid Blue Truecaller 1-Tap Login Button
                        TruecallerLoginButton(
                            busy = isTruecallerBusy || otpState.isLoading,
                            onAuthorized = { code, verifier ->
                                isTruecallerBusy = true
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                scope.launch {
                                    profileCompletionViewModel.saveAuthMethod("TRUECALLER")
                                    val exchangeResult = otpViewModel.truecallerExchange(
                                        authorizationCode = code,
                                        codeVerifier = verifier,
                                        role = role.name,
                                        mode = "unified"
                                    )
                                    exchangeResult.fold(
                                        onSuccess = { tc ->
                                            if (tc.allowed && !tc.token.isNullOrBlank()) {
                                                profileCompletionViewModel.savePhoneNumber(tc.phone)
                                                otpViewModel.signInWithTruecaller(tc)
                                            } else {
                                                isTruecallerBusy = false
                                                if (tc.roleConflict) {
                                                    val existingRoleLabel = if (tc.existingRole?.contains("EMPLOYER", ignoreCase = true) == true) {
                                                        context.getString(R.string.employer)
                                                    } else {
                                                        context.getString(R.string.worker)
                                                    }
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.auth_phone_registered_as_role, existingRoleLabel, existingRoleLabel),
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.auth_truecaller_failed),
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        },
                                        onFailure = { err ->
                                            isTruecallerBusy = false
                                            Timber.e(err, "Truecaller exchange failed")
                                            Toast.makeText(
                                                context,
                                                err.message ?: context.getString(R.string.auth_truecaller_failed),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                }
                            },
                            onFailed = {
                                isTruecallerBusy = false
                                Toast.makeText(context, context.getString(R.string.auth_truecaller_failed), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Bottom Area (18+ Terms & Docked CONTINUE Button)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        HorizontalDivider(color = BorderFaint, thickness = 1.dp)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Terms and 18+ requirement in 2 lines with light black links
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isTelugu) "కొనసాగడం ద్వారా, మీరు 18+ సంవత్సరాల వయస్సు కలిగి ఉన్నారని మరియు మా" else "By continuing, you agree that you are 18+ years old and accept our",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isTelugu) "నిబంధనలు & షరతులు" else "Terms & Conditions",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.clickable { navController.navigate(Routes.TERMS_OF_SERVICE) }
                                )
                                Text(
                                    text = if (isTelugu) " మరియు " else " & ",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = if (isTelugu) "గోప్యతా విధానం" else "Privacy Policy",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.clickable { navController.navigate(Routes.PRIVACY_POLICY) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val buttonEnabled = ValidationUtils.isValidIndianPhoneNumber(phoneNumber) && !otpState.isLoading && !isTruecallerBusy

                        // Docked Full-Width CONTINUE Button (Solid Black with 3-dot animated loader)
                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                val fullPhoneNumber = selectedCountryCode + phoneNumber
                                smsRetrieverHelper.startListening()
                                otpViewModel.onSmsRetrieverStarted()
                                scope.launch {
                                    profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                    profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                }
                                otpViewModel.sendOtp(fullPhoneNumber, context, mode = "unified")
                            },
                            enabled = buttonEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White,
                                disabledContainerColor = DisabledGray,
                                disabledContentColor = Color(0xFF757575)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            if (otpState.isLoading) {
                                com.example.dutype.components.ButtonThreeDotsLoader(dotColor = Color.White)
                            } else {
                                Text(
                                    text = "CONTINUE",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (buttonEnabled) Color.White else Color(0xFF757575)
                                )
                            }
                        }
                    }
                }
            } else {
                // =========================================================================
                // SCREEN 2: Enter the 6 Digit OTP sent (PhonePe Business replicated)
                // =========================================================================
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top App Header with Back Arrow + DutyPe + Help Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                otpViewModel.resetState()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        DutyPeBrandBadge()

                        Spacer(modifier = Modifier.weight(1f))

                        // Help & Support button
                        Surface(
                            onClick = {
                                navController.navigate(Routes.CONTACT_US)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderNormal)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "Help & Support",
                                    tint = TextDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTelugu) "సహాయం" else "Help",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDark
                                )
                            }
                        }
                    }

                    // Main Content (OTP Input)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp)
                    ) {
                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = if (isTelugu) "పంపిన 6 అంకెల OTP ని నమోదు చేయండి" else "Enter the 6 Digit OTP sent",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "to ",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = phoneNumber,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // 6 Distinct OTP Rectangular Input Boxes
                        OtpSixDigitInputRow(
                            otpValue = otpValue,
                            onOtpChange = { newCode ->
                                otpValue = newCode
                                if (newCode.length == 6) {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    otpViewModel.verifyOtp(newCode, context)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Auto-reading indicator / Resend options
                        if (resendCooldown > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = BrandGreen,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isTelugu) "OTP ఆటో రీడింగ్ $resendCooldown సెకన్లు" else "Auto reading OTP $resendCooldown secs",
                                    fontSize = 14.5.sp,
                                    color = TextMuted
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = if (isTelugu) "మళ్లీ OTP పంపండి" else "Resend OTP",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandGreen,
                                    modifier = Modifier.clickable {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        otpViewModel.sendOtp(fullPhoneNumber, context, mode = "unified")
                                    }
                                )

                                Text(
                                    text = "Resend via WhatsApp",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WhatsAppGreen,
                                    modifier = Modifier.clickable {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        otpViewModel.resendViaWhatsapp(fullPhoneNumber, context, mode = "unified")
                                    }
                                )
                            }
                        }
                    }

                    val verifyButtonEnabled = otpValue.length == 6 && !otpState.isLoading && !isNavigatingAway && !otpState.otpVerified

                    // Docked VERIFY Button (Solid Black)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                otpViewModel.verifyOtp(otpValue, context)
                            },
                            enabled = verifyButtonEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White,
                                disabledContainerColor = DisabledGray,
                                disabledContentColor = Color(0xFF757575)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            if (otpState.isLoading || isNavigatingAway || otpState.otpVerified) {
                                com.example.dutype.components.ButtonThreeDotsLoader(dotColor = Color.White)
                            } else {
                                Text(
                                    text = "VERIFY",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (verifyButtonEnabled) Color.White else Color(0xFF757575)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

/**
 * DutyPe Brand wordmark - Clean black text matching WorkerHomeScreen
 */
@Composable
private fun DutyPeBrandBadge(modifier: Modifier = Modifier) {
    Text(
        text = "DutyPe",
        color = Color.Black,
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.sp,
        modifier = modifier
    )
}

/**
 * Replicated 6-digit OTP row with thin rectangular borders exactly like Image 3
 */
@Composable
private fun OtpSixDigitInputRow(
    otpValue: String,
    onOtpChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isInputFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
        ) {
            repeat(6) { index ->
                val digit = otpValue.getOrNull(index)?.toString() ?: ""
                val isActive = (index == otpValue.length && isInputFocused) || (index == 5 && otpValue.length == 6)

                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) Color(0xFF0F0F0F) else BorderNormal,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }
        }

        // Invisible BasicTextField capturing input
        BasicTextField(
            value = otpValue,
            onValueChange = { newValue ->
                val clean = newValue.filter { it.isDigit() }.take(6)
                if (clean != otpValue) {
                    onOtpChange(clean)
                }
            },
            modifier = Modifier
                .matchParentSize()
                .focusRequester(focusRequester)
                .onFocusChanged { isInputFocused = it.isFocused },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            textStyle = TextStyle(color = Color.Transparent)
        )
    }
}
