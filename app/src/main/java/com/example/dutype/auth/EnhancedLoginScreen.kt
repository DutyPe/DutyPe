package com.example.dutype.auth

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

// Brand Palette matching modern design
private val BrandBlue = Color(0xFF0077F0)
private val BrandGreen = Color(0xFF007A3D)
private val WhatsAppGreen = Color(0xFF25D366)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val BorderNormal = Color(0xFFCBD5E1)
private val BorderFaint = Color(0xFFE2E8F0)
private val DisabledGray = Color(0xFFBDBDBD)

private data class OnboardingSlideData(
    val imageRes: Int,
    val titleRes: Int,
    val descRes: Int,
    val highlightEn: String,
    val highlightTe: String,
    val highlightHi: String,
    val gradientColors: List<Color>
)

private val loginOnboardingDeck = listOf(
    OnboardingSlideData(
        imageRes = R.drawable.onboarding1,
        titleRes = R.string.onboarding_title_1,
        descRes = R.string.onboarding_desc_1,
        highlightEn = "Work & Jobs Near You",
        highlightTe = "మీ దరిదాపుల్లోనే పనులు",
        highlightHi = "अपने आस-पास काम",
        gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
    ),
    OnboardingSlideData(
        imageRes = R.drawable.onboarding2,
        titleRes = R.string.onboarding_title_2,
        descRes = R.string.onboarding_desc_2,
        highlightEn = "Hire in 5 Minutes",
        highlightTe = "వర్కర్లను వేగంగా పొందండి",
        highlightHi = "तेजी से वर्कर प्राप्त करें",
        gradientColors = listOf(Color(0xFF9F1239), Color(0xFFE11D48))
    ),
    OnboardingSlideData(
        imageRes = R.drawable.onboarding3,
        titleRes = R.string.onboarding_title_3,
        descRes = R.string.onboarding_desc_3,
        highlightEn = "Instant Urgent Hire",
        highlightTe = "ఇన్‌స్టంట్ హైరింగ్ సదుపాయం",
        highlightHi = "तत्काल भर्ती (Instant Hire)",
        gradientColors = listOf(Color(0xFF4C1D95), Color(0xFF7C3AED))
    ),
    OnboardingSlideData(
        imageRes = R.drawable.onboarding4,
        titleRes = R.string.onboarding_title_4,
        descRes = R.string.onboarding_desc_4,
        highlightEn = "100% Verified Community",
        highlightTe = "నమ్మకమైన కమ్యూనిటీ",
        highlightHi = "भरोसेमंद समुदाय",
        gradientColors = listOf(Color(0xFF065F46), Color(0xFF059669))
    )
)

@Composable
fun EnhancedLoginScreen(
    navController: NavController,
    skipRoleSelection: Boolean = true,
    initialRole: String = "WORKER",
    returnRoute: String? = null,
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
        Timber.d("EnhancedLoginScreen - Role: $initialRole -> $selectedRole, returnRoute: $returnRoute")
        otpViewModel.setRoleContext(selectedRole)
    }

    OtpLoginScreen(
        role = selectedRole,
        otpViewModel = otpViewModel,
        profileCompletionViewModel = profileCompletionViewModel,
        navController = navController,
        returnRoute = returnRoute
    )
}

@Composable
private fun OtpLoginScreen(
    role: UserRole,
    otpViewModel: OtpViewModel,
    profileCompletionViewModel: ProfileCompletionViewModel,
    navController: NavController,
    returnRoute: String? = null
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

    // Status bar with white icons on rich top gradient, dark icons on bottom white navigation bar
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val originalLightStatusBars = insetsController?.isAppearanceLightStatusBars ?: true
        val originalLightNavBars = insetsController?.isAppearanceLightNavigationBars ?: true

        insetsController?.isAppearanceLightStatusBars = false
        insetsController?.isAppearanceLightNavigationBars = true
        window?.statusBarColor = android.graphics.Color.TRANSPARENT
        window?.navigationBarColor = android.graphics.Color.WHITE

        onDispose {
            insetsController?.isAppearanceLightStatusBars = originalLightStatusBars
            insetsController?.isAppearanceLightNavigationBars = originalLightNavBars
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

                            // Authoritative users/{uid} sync
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users").document(currentUser.uid)
                                .set(
                                    mapOf(
                                        "uid" to currentUser.uid,
                                        "phoneNumber" to (currentUser.phoneNumber ?: outcome.user?.phone.orEmpty()),
                                        "role" to outcome.role.name,
                                        "name" to (resolvedName ?: outcome.user?.name ?: ""),
                                        "updatedAt" to com.google.firebase.Timestamp.now()
                                    ),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                            otpViewModel.lastTruecallerResult?.photoUrl?.trim()?.takeIf { it.isNotBlank() }?.let { pic ->
                                profileCompletionViewModel.saveUserPhotoUrl(pic)
                            }

                            // Mark onboarding completed at login
                            scope.launch {
                                runCatching { profileCompletionViewModel.markOnboardingCompleted() }
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

                                // Redirect to requested destination or back to caller
                                handlePostLoginNavigation(outcome.role, navController, returnRoute)
                            } else {
                                navigateToProfileSetup(outcome.role, navController, returnRoute)
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

    // 4 Onboarding Slides Pager State with Auto-Scrolling every 3.5 seconds
    val pagerState = rememberPagerState(pageCount = { loginOnboardingDeck.size })
    LaunchedEffect(pagerState) {
        while (true) {
            delay(3500L)
            val nextPage = (pagerState.currentPage + 1) % loginOnboardingDeck.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    // Dynamic animated background gradient matching the active slide
    val currentSlide = loginOnboardingDeck[pagerState.currentPage]
    val animatedColorStart by animateColorAsState(
        targetValue = currentSlide.gradientColors[0],
        animationSpec = tween(600),
        label = "gradStart"
    )
    val animatedColorEnd by animateColorAsState(
        targetValue = currentSlide.gradientColors[1],
        animationSpec = tween(600),
        label = "gradEnd"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedColorStart, animatedColorEnd)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // =========================================================================
            // TOP SECTION: Full Onboarding Graphics & Auto Carousel (~55% height)
            // =========================================================================
            val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isKeyboardOpen) 0.5f else 1.15f)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // HorizontalPager showing 4 Onboarding Screens
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { pageIndex ->
                    val slide = loginOnboardingDeck[pageIndex]
                    val highlightWord = when {
                        isTelugu -> slide.highlightTe
                        LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_HINDI -> slide.highlightHi
                        else -> slide.highlightEn
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp, end = 24.dp, top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Punchy headline in pure white text without "DutyPe" prefix (26sp, bold)
                        Text(
                            text = highlightWord,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(id = slide.descRes),
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // High quality transparent PNG Onboarding Illustration (slightly enlarged)
                        Image(
                            painter = painterResource(id = slide.imageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .heightIn(min = 175.dp, max = 235.dp)
                                .weight(1f, fill = false),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Animated 4-dot indicators right above the curved bottom sheet (refined size)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    repeat(loginOnboardingDeck.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 16.dp else 4.5.dp,
                            animationSpec = tween(300),
                            label = "dot_w"
                        )
                        val dotColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.5.dp)
                                .height(4.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }

            // =========================================================================
            // DOWNSIDE: Curved White Bottom Sheet Card for Login
            // =========================================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isKeyboardOpen) 1.5f else 1f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White,
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!otpState.otpSent) {
                        // Title "Log in or sign up" (Clean typography)
                        Text(
                            text = if (isTelugu) "లాగిన్ లేదా సైన్ అప్" else "Log in or sign up",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Single Clean Rounded Phone Number Input Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.dp,
                                    color = if (phoneHasFocus) Color(0xFF0F172A) else Color(0xFFD1D5DB),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    phoneFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "+91  ",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
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
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color(0xFF0F172A)
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF0F172A)),
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
                                                text = if (isTelugu) "మొబైల్ నంబర్" else "Phone number",
                                                fontSize = 16.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Solid Black Continue Button (Always Active, validates on tap)
                        Button(
                            onClick = {
                                val trimmed = phoneNumber.trim()
                                if (trimmed.isEmpty()) {
                                    val msg = if (isTelugu) "దయచేసి ఫోన్ నంబర్ నమోదు చేయండి" else "Please enter phone number"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else if (trimmed.length < 10 || !ValidationUtils.isValidIndianPhoneNumber(trimmed)) {
                                    val msg = if (isTelugu) "దయచేసి సరైన 10 అంకెల మొబైల్ నంబర్ నమోదు చేయండి" else "Please enter a valid 10-digit mobile number"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    val fullPhoneNumber = selectedCountryCode + trimmed
                                    smsRetrieverHelper.startListening()
                                    otpViewModel.onSmsRetrieverStarted()
                                    scope.launch {
                                        profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                        profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                        runCatching { profileCompletionViewModel.markOnboardingCompleted() }
                                    }
                                    otpViewModel.sendOtp(fullPhoneNumber, context, mode = "unified")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF111111),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            if (otpState.isLoading) {
                                com.example.dutype.components.ButtonThreeDotsLoader(dotColor = Color.White)
                            } else {
                                Text(
                                    text = if (isTelugu) "కొనసాగించండి" else "Continue",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // "or" Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                            Text(
                                text = if (isTelugu) "లేదా" else "or",
                                modifier = Modifier.padding(horizontal = 14.dp),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Truecaller 1-Tap Login Secondary Button
                        TruecallerLoginButton(
                            busy = isTruecallerBusy || otpState.isLoading,
                            onAuthorized = { code, verifier ->
                                isTruecallerBusy = true
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                scope.launch {
                                    profileCompletionViewModel.saveAuthMethod("TRUECALLER")
                                    runCatching { profileCompletionViewModel.markOnboardingCompleted() }
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



                        Spacer(modifier = Modifier.height(10.dp))

                        // Terms of Service & Privacy Policy fine print
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isTelugu) "కొనసాగడం ద్వారా మీరు మా " else "By continuing, you agree to our ",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = if (isTelugu) "నిబంధనలు" else "Terms of Service",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                modifier = Modifier.clickable { navController.navigate(Routes.TERMS_OF_SERVICE) }
                            )
                            Text(text = " • ", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                            Text(
                                text = if (isTelugu) "గోప్యతా విధానం" else "Privacy Policy",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                modifier = Modifier.clickable { navController.navigate(Routes.PRIVACY_POLICY) }
                            )
                        }
                    } else {
                        // OTP Verification Screen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    otpViewModel.resetState()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTelugu) "OTP ధృవీకరించండి" else "Verify OTP",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isTelugu) "+91 $phoneNumber కు పంపిన 6 అంకెల కోడ్‌ను నమోదు చేయండి" else "Enter the 6-digit code sent to +91 $phoneNumber",
                            fontSize = 13.5.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // 6-digit OTP Input Boxes
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Resend options
                        if (resendCooldown > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(color = Color(0xFF2563EB), modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isTelugu) "OTP ఆటో రీడింగ్ $resendCooldown సెకన్లు" else "Auto reading OTP $resendCooldown secs",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isTelugu) "మళ్లీ OTP పంపండి" else "Resend OTP",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.clickable {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        otpViewModel.sendOtp(fullPhoneNumber, context, mode = "unified")
                                    }
                                )
                                Text(
                                    text = "WhatsApp OTP",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WhatsAppGreen,
                                    modifier = Modifier.clickable {
                                        val fullPhoneNumber = selectedCountryCode + phoneNumber
                                        otpViewModel.resendViaWhatsapp(fullPhoneNumber, context, mode = "unified")
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        val verifyButtonEnabled = otpValue.length == 6 && !otpState.isLoading && !isNavigatingAway && !otpState.otpVerified

                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                otpViewModel.verifyOtp(otpValue, context)
                            },
                            enabled = verifyButtonEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF111111),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFCBD5E1),
                                disabledContentColor = Color(0xFF94A3B8)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            if (otpState.isLoading) {
                                com.example.dutype.components.ButtonThreeDotsLoader(dotColor = Color.White)
                            } else {
                                Text(
                                    text = if (isTelugu) "ధృవీకరించండి" else "Verify & Continue",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
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
 * Replicated 6-digit OTP row with animated blinking caret inside the active box
 */
@Composable
private fun OtpSixDigitInputRow(
    otpValue: String,
    onOtpChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isInputFocused by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

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
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            repeat(6) { index ->
                val digit = otpValue.getOrNull(index)?.toString() ?: ""
                val isCurrentBox = index == otpValue.length && isInputFocused
                val isActive = isCurrentBox || (index == 5 && otpValue.length == 6)

                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (digit.isNotEmpty()) {
                        Text(
                            text = digit,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    } else if (isCurrentBox) {
                        // Animated blinking cursor positioned properly inside the active OTP box
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(24.dp)
                                .background(Color(0xFF0F172A).copy(alpha = cursorAlpha))
                        )
                    }
                }
            }
        }

        // Invisible BasicTextField capturing input with transparent cursor
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
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = TextStyle(color = Color.Transparent)
        )
    }
}

private fun handlePostLoginNavigation(
    role: UserRole,
    navController: NavController,
    returnRoute: String?
) {
    if (returnRoute.isNullOrBlank()) {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            navigateToHome(role, navController)
        }
        return
    }

    val trimmedRoute = returnRoute.trim()
    val isWorkerInnerRoute = trimmedRoute.startsWith("worker_") ||
        trimmedRoute.startsWith("job_application") ||
        trimmedRoute == Routes.WORKER_REFER_EARN ||
        trimmedRoute == Routes.WORKER_EARNINGS ||
        trimmedRoute == Routes.WORKER_HISTORY ||
        trimmedRoute == Routes.WORKER_PROFILE_DETAILS ||
        trimmedRoute == Routes.WORKER_ALL_JOBS ||
        trimmedRoute == Routes.WORKER_JOB_MAP ||
        trimmedRoute == Routes.WORKER_NOTIFICATIONS ||
        trimmedRoute == Routes.WORKER_CATEGORIES

    val isEmployerInnerRoute = trimmedRoute.startsWith("employer_") ||
        trimmedRoute == Routes.EMPLOYER_POST_JOB ||
        trimmedRoute == Routes.EMPLOYER_POST_URGENT_NEED ||
        trimmedRoute == Routes.EMPLOYER_REFER_EARN ||
        trimmedRoute == Routes.EMPLOYER_COMPANY_DETAILS ||
        trimmedRoute == Routes.EMPLOYER_MY_JOBS ||
        trimmedRoute == Routes.EMPLOYER_APPLICATIONS ||
        trimmedRoute == Routes.EMPLOYER_SUBSCRIPTION ||
        trimmedRoute == Routes.EMPLOYER_NOTIFICATIONS ||
        trimmedRoute == Routes.ANALYTICS ||
        trimmedRoute == Routes.EMPLOYER_MANAGE_ADDRESSES

    when {
        role == UserRole.WORKER && isWorkerInnerRoute -> {
            com.example.dutype.navigation.WorkerInnerNavQueue.setPending(trimmedRoute)
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else {
                navController.navigate(Routes.WORKER_HOME) {
                    popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        role == UserRole.EMPLOYER && isEmployerInnerRoute -> {
            com.example.dutype.navigation.EmployerInnerNavQueue.setPending(trimmedRoute)
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else {
                navController.navigate(Routes.EMPLOYER_HOME) {
                    popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        else -> {
            try {
                navController.navigate(trimmedRoute) {
                    popUpTo(Routes.ENHANCED_LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                Timber.e(e, "Error navigating to returnRoute: $trimmedRoute")
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    navigateToHome(role, navController)
                }
            }
        }
    }
}
