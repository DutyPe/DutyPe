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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.AddCircleOutline
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay

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
    var truecallerBusy by remember { mutableStateOf(false) }

    val selectedCountryCode = "+91"
    val context = LocalContext.current
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU
    val scope = rememberCoroutineScope()
    val otpState by otpViewModel.otpState.collectAsState()

    val phoneFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

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

    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
        val originalLightStatusBars = insetsController?.isAppearanceLightStatusBars ?: true
        val originalLightNavBars = insetsController?.isAppearanceLightNavigationBars ?: true
        val originalNavBarColor = window?.navigationBarColor

        // Status bar: Dark hero background -> White icons (signal, wifi, battery, clock)
        insetsController?.isAppearanceLightStatusBars = false
        insetsController?.isAppearanceLightNavigationBars = true
        window?.navigationBarColor = android.graphics.Color.WHITE

        onDispose {
            insetsController?.isAppearanceLightStatusBars = originalLightStatusBars
            insetsController?.isAppearanceLightNavigationBars = originalLightNavBars
            originalNavBarColor?.let { window?.navigationBarColor = it }
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching {
            phoneFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    BackHandler {
        if (otpState.otpSent) {
            otpViewModel.resetState()
        } else {
            safeAuthBackNavigation(navController)
        }
    }

    LaunchedEffect(otpState.otpVerified) {
        if (otpState.otpVerified) {
            Timber.d("📱 OTP VERIFICATION SUCCESS - Starting unified auth resolution")

            try {
                val currentUser = FirebaseAuth.getInstance().currentUser

                if (currentUser != null) {
                    val tcInfo = otpViewModel.lastTruecallerResult
                    val tcName = tcInfo?.name?.takeIf { it.isNotBlank() }
                    val tcEmail = tcInfo?.email?.orEmpty() ?: ""

                    val authResult = otpViewModel.resolveUnifiedAuth(
                        role = role,
                        fullName = tcName
                    )
                    authResult.fold(
                        onSuccess = { outcome ->
                            val resolvedName = outcome.user?.name?.trim()?.takeIf { it.isNotBlank() && !it.equals("User", ignoreCase = true) }
                                ?: tcName
                            profileCompletionViewModel.saveUserInfoToLocalStorage(
                                email = tcEmail,
                                name = resolvedName ?: "",
                                role = outcome.role
                            )
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
                            Timber.e(error, "OTP VERIFICATION SUCCESS - Auth resolution failed")
                            val msg = error.message.orEmpty()
                            val toastText = if (msg.startsWith("phone-already-registered-as:")) {
                                val existingRole = msg.substringAfter(":").lowercase()
                                val existingRoleLabel = if (existingRole == "employer") context.getString(R.string.employer) else context.getString(R.string.worker)
                                context.getString(R.string.auth_phone_registered_as_role, existingRoleLabel, existingRoleLabel)
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
        // Ensure SMS Retriever is active on OTP screen
        LaunchedEffect(Unit) {
            smsRetrieverHelper.startListening()
            otpViewModel.onSmsRetrieverStarted()
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

        val resendCooldown by otpViewModel.resendCooldownSeconds.collectAsState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Top Hero Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()
            ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color(0xFF141416),
                            radius = size.width * 0.48f,
                            center = Offset(x = size.width * 0.90f, y = size.height * 0.15f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 24.dp)
                    ) {
                        // Top Navigation / Help Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { otpViewModel.resetState(keepActiveSession = true) },
                                shape = CircleShape,
                                color = Color(0x20FFFFFF),
                                border = BorderStroke(1.dp, Color(0x40FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier.size(38.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    val whatsappUrl = "https://wa.me/918019151847?text=Hello%20DutyPe%20Team!%20I%20need%20help%20with%20OTP."
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
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, Color(0x60FFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_whatsapp),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.auth_help),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "DutyPe",
                            modifier = Modifier.padding(horizontal = 24.dp),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-1.5).sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (otpState.channel == com.example.dutype.viewmodels.OtpChannel.WHATSAPP) {
                                stringResource(R.string.auth_code_sent_whatsapp)
                            } else {
                                stringResource(R.string.auth_code_sent_sms)
                            },
                            modifier = Modifier.padding(horizontal = 24.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFCBD5E1),
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                // Bottom White Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = Color.White
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                    ) {
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
                            onResendWhatsapp = {
                                val fullPhoneNumber = selectedCountryCode + phoneNumber
                                otpViewModel.resendViaWhatsapp(fullPhoneNumber, context, mode = "unified")
                            },
                            onResendSms = {
                                val fullPhoneNumber = selectedCountryCode + phoneNumber
                                otpViewModel.resendViaSms(fullPhoneNumber, context, mode = "unified")
                            },
                            onBackClick = { otpViewModel.resetState(keepActiveSession = true) },
                            resendCooldownSeconds = resendCooldown
                        )
                    }
                }
            }
        } else {
        // Phone-entry screen — matches media_1791261378906.png pixel-for-pixel:
        // Top black hero (Help pill, DutyPe 46sp, tagline, 3 badges) +
        // Bottom white card (Enter your number, +91 input, Send OTP arrow button, or divider, Continue with Truecaller, 18+ T&C footer)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Top Hero Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()
            ) {
                    // Dark ambient background circles
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color(0xFF141416),
                            radius = size.width * 0.48f,
                            center = Offset(x = size.width * 0.90f, y = size.height * 0.15f)
                        )
                        drawCircle(
                            color = Color(0xFF101012),
                            radius = size.width * 0.42f,
                            center = Offset(x = size.width * 0.05f, y = size.height * 0.90f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 28.dp)
                    ) {
                        // WhatsApp Help Button (Top-Right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    val whatsappUrl = "https://wa.me/918019151847?text=Hello%20DutyPe%20Team!%20I%20need%20help%20logging%20in."
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
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, Color(0x60FFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_whatsapp),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.auth_help),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // DutyPe Wordmark
                        Text(
                            text = "DutyPe",
                            modifier = Modifier.padding(horizontal = 24.dp),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 46.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = (-1.5).sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tagline: "Jobs & home services, made simple."
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = Color(0xFFCBD5E1), fontWeight = FontWeight.Normal, fontSize = 18.sp)) {
                                    append("Jobs & home services,\n")
                                }
                                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                                    append("made simple.")
                                }
                            },
                            lineHeight = 25.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // 3 Badges Row - crisp single row matching mockup (no clipping or scrolling)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LoginFeatureBadge(
                                icon = Icons.Outlined.LocationOn,
                                text = "Jobs near you"
                            )
                            LoginFeatureBadge(
                                icon = Icons.Outlined.Home,
                                text = "Home services"
                            )
                            LoginFeatureBadge(
                                icon = Icons.Outlined.AddCircleOutline,
                                text = "Hire in minutes"
                            )
                        }
                    }
                }

                // White Bottom Card (top rounded 32.dp, draws all the way to bottom edge)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                        // Title: "Enter your number"
                        Text(
                            text = stringResource(R.string.auth_enter_mobile_number),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink900.fg(),
                                letterSpacing = (-0.4).sp
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Phone Input Box: Flag | +91 | Divider | Number
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(
                                if (phoneNumber.isNotEmpty()) 1.5.dp else 1.dp,
                                if (phoneNumber.isNotEmpty()) Color.Black else BrandBlueBorder.bd()
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🇮🇳",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedCountryCode,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Ink900.fg()
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(24.dp)
                                        .background(CardBorder.bg())
                                )

                                Spacer(modifier = Modifier.width(14.dp))

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
                                        .focusRequester(phoneFocusRequester),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp,
                                        color = Ink900.fg(),
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    cursorBrush = SolidColor(Color.Black),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                                    decorationBox = { innerTextField ->
                                        if (phoneNumber.isBlank()) {
                                            Text(
                                                text = "9876543210",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = 18.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val buttonEnabled = ValidationUtils.isValidIndianPhoneNumber(phoneNumber) && !otpState.isLoading

                        // Send OTP Button
                        Button(
                            onClick = {
                                val fullPhoneNumber = selectedCountryCode + phoneNumber
                                smsRetrieverHelper.startListening()
                                otpViewModel.onSmsRetrieverStarted()

                                scope.launch {
                                    profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                    profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                }
                                otpViewModel.sendOtp(fullPhoneNumber, context, mode = "unified")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = buttonEnabled,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFCBD5E1).bg(),
                                disabledContentColor = Color(0xFF64748B).fg()
                            )
                        ) {
                            if (otpState.isLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.auth_send_otp),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontSize = 16.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        /*
                        // Truecaller temporarily disabled for 1-2 days pending Play Store verification
                        Spacer(modifier = Modifier.height(20.dp))

                        // "or" Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0),
                                thickness = 1.dp
                            )
                            Text(
                                text = "or",
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFE2E8F0),
                                thickness = 1.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Continue with Truecaller Button
                        TruecallerLoginButton(
                            busy = truecallerBusy,
                            enabled = !truecallerBusy && !otpState.isLoading && !isCheckingPhone,
                            onAuthorized = { code, verifier ->
                                truecallerBusy = true
                                scope.launch {
                                    otpViewModel.truecallerExchange(code, verifier, mode = "unified").fold(
                                        onSuccess = { tc ->
                                            truecallerBusy = false
                                            when {
                                                tc.allowed -> {
                                                    profileCompletionViewModel.saveAuthMethod("TRUECALLER")
                                                    profileCompletionViewModel.savePhoneNumber(tc.phone)
                                                    if (tc.name.isNotBlank()) {
                                                        profileCompletionViewModel.saveUserInfoToLocalStorage(
                                                            email = tc.email,
                                                            name = tc.name,
                                                            role = role
                                                        )
                                                    }
                                                    otpViewModel.signInWithTruecaller(tc)
                                                }
                                                tc.roleConflict || (tc.existingRole != null && tc.existingRole != role.name) -> {
                                                    val label = context.getString(
                                                        if (tc.existingRole == "EMPLOYER") R.string.employer else R.string.worker
                                                    )
                                                    Toast.makeText(
                                                        context, context.getString(R.string.auth_phone_registered_as_role, label, label), Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                else -> {
                                                    Toast.makeText(context, context.getString(R.string.auth_truecaller_failed), Toast.LENGTH_LONG).show()
                                                }
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
                        */

                        // DEV-ONLY: Skip OTP
                        if (LocalContext.current.isDebuggableBuild()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { navigateToHome(role, navController) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
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

                        }

                        // Terms & Privacy footer - cleanly anchored at bottom of card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val termsPrefix = stringResource(R.string.auth_by_continuing_agree)
                            val termsTitle = stringResource(R.string.auth_terms)
                            val privacyTitle = stringResource(R.string.auth_privacy_policy_short)
                            val termsAnnotated = buildAnnotatedString {
                                append(termsPrefix)
                                pushStringAnnotation(tag = "TERMS", annotation = "terms")
                                withStyle(SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                                    append(termsTitle)
                                }
                                pop()
                                append(" & ")
                                pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                                withStyle(SpanStyle(color = Color.Black, fontWeight = FontWeight.Bold)) {
                                    append(privacyTitle)
                                }
                                pop()
                            }
                            androidx.compose.foundation.text.ClickableText(
                                text = termsAnnotated,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B),
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
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
private fun LoginFeatureBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OtpInputSection(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    phoneNumber: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    onVerifyClick: () -> Unit,
    onResendWhatsapp: () -> Unit,
    onResendSms: () -> Unit,
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
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Top Header Bar: Back arrow and "Verify OTP" aligned on the exact header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Ink900.fg(),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.auth_verify_otp),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink900.fg(),
                    letterSpacing = (-0.2).sp
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

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

        AuthOtpBoxes(
            otpValue = otpValue,
            onOtpChange = onOtpChange,
            digitCount = 6
        )

        // Wrong / expired code etc. (this screen showed no OTP errors before WhatsApp codes).
        otpState.error?.takeIf { !it.startsWith("phone-already-registered-as:") }?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626).fg(), fontSize = 13.sp),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dual Resend Actions: WhatsApp chip + SMS chip in a natural, centered layout (un-stretched)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chip 1: Resend via WhatsApp (with timer during cooldown)
            Surface(
                onClick = onResendWhatsapp,
                enabled = resendCooldownSeconds <= 0 && !otpState.isLoading,
                shape = RoundedCornerShape(20.dp),
                color = Color.White.bg(),
                border = BorderStroke(
                    1.dp,
                    if (resendCooldownSeconds <= 0) Color(0xFF25D366).bd() else CardBorder.bd()
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        contentDescription = null,
                        tint = Color(0xFF25D366).fg(),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    val waMinutes = resendCooldownSeconds / 60
                    val waSeconds = resendCooldownSeconds % 60
                    Text(
                        text = if (resendCooldownSeconds > 0) {
                            stringResource(R.string.auth_resend_via_whatsapp_in, waMinutes, waSeconds)
                        } else {
                            stringResource(R.string.auth_resend_via_whatsapp)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (resendCooldownSeconds <= 0) Color(0xFF1B5E20).fg() else Ink600.fg()
                        ),
                        maxLines = 1
                    )
                }
            }

            // Chip 2: Send via SMS (STRICTLY LOCKED until WhatsApp cooldown reaches 0)
            val smsEnabled = !otpState.isLoading && resendCooldownSeconds <= 0
            val smsMinutes = resendCooldownSeconds / 60
            val smsSeconds = resendCooldownSeconds % 60
            Surface(
                onClick = onResendSms,
                enabled = smsEnabled,
                shape = RoundedCornerShape(20.dp),
                color = if (smsEnabled) Color.White.bg() else Color(0xFFF8FAFC).bg(),
                border = BorderStroke(
                    1.dp,
                    if (smsEnabled) BrandBlueBorder.bd() else Color(0xFFE2E8F0).bd()
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_profile_message),
                        contentDescription = null,
                        tint = if (smsEnabled) BrandBluePrimary.fg() else Color(0xFF94A3B8).fg(),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (resendCooldownSeconds > 0) {
                            stringResource(R.string.auth_send_via_sms_in, smsMinutes, smsSeconds)
                        } else {
                            stringResource(R.string.auth_send_via_sms)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (smsEnabled) BrandBluePrimary.fg() else Color(0xFF94A3B8).fg()
                        ),
                        maxLines = 1
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
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFCBD5E1).bg(),
                disabledContentColor = Color(0xFF64748B).fg()
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
