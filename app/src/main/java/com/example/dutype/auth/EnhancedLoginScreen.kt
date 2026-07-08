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
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
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
import com.example.dutype.ui.theme.LocalDarkMode
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.FirestoreUtils
import com.example.dutype.utils.LocaleHelper
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
        otpViewModel.setRoleContext(selectedRole)
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

    // Handle OTP verification success
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
                            // Bug #9: surface single-role-per-phone block as a
                            // friendly toast instead of a generic error.
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
                            Toast.makeText(
                                context,
                                toastText,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                    otpViewModel.resetState()
                    return@LaunchedEffect
                } else {
                    // No Firebase user - go back to role selection
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 30.dp),
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

                                    // Login-only: check if user exists AND matches the role
                                    // the user selected on the landing page. Single-role-per-phone
                                    // means a Worker cannot log in on the Employer tab and vice versa.
                                    val phoneCheck = FirestoreUtils.checkPhoneForRole(
                                        phoneNumber = fullPhoneNumber,
                                        requestedRole = role.name
                                    )
                                    when (phoneCheck.exists) {
                                        FirestoreUtils.PhoneExistenceResult.NOT_EXISTS -> {
                                        isCheckingPhone = false
                                        Toast.makeText(
                                            context,
                                            if (isTelugu) "ఈ నంబర్‌కు సంబంధించిన ఖాతా కనబడలేదు. దయచేసి ముందుగా నమోదు చేయండి." else "No account found with this number. Please Register first.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Timber.w("📱 Login blocked - User doesn't exist: $fullPhoneNumber")
                                        return@launch
                                        }
                                        FirestoreUtils.PhoneExistenceResult.UNKNOWN -> {
                                            Timber.w("📱 Login phone pre-check unavailable; continuing to OTP and enforcing account state after auth: $fullPhoneNumber")
                                        }
                                        FirestoreUtils.PhoneExistenceResult.EXISTS -> {
                                            if (phoneCheck.roleConflict) {
                                                isCheckingPhone = false
                                                val existingRoleLabel = when (phoneCheck.existingRole?.uppercase()) {
                                                    "WORKER" -> if (isTelugu) "వర్కర్" else "worker"
                                                    "EMPLOYER" -> if (isTelugu) "ఎంప్లాయర్" else "employer"
                                                    else -> if (isTelugu) "వేరే పాత్ర" else "different role"
                                                }
                                                Toast.makeText(
                                                    context,
                                                    if (isTelugu) "ఈ నంబర్ $existingRoleLabel గా నమోదు అయింది. దయచేసి $existingRoleLabel గా లాగిన్ చేయండి."
                                                    else "This number is registered as a $existingRoleLabel. Please log in as a $existingRoleLabel.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                Timber.w(
                                                    "📱 Login blocked - role conflict phone=$fullPhoneNumber " +
                                                        "existingRole=${phoneCheck.existingRole} requested=${role.name}"
                                                )
                                                return@launch
                                            }
                                        }
                                    }

                                    isCheckingPhone = false
                                    profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                    profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                    otpViewModel.sendOtp(fullPhoneNumber, context)
                                } catch (e: Exception) {
                                    isCheckingPhone = false
                                    Timber.e(e, "📱 Error in phone check")
                                    Toast.makeText(
                                        context,
                                        if (isTelugu) "ఖాతా ధృవీకరణ విఫలమైంది. దయచేసి మళ్లీ ప్రయత్నించండి." else "Account verification failed. Please try again.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        onBackClick = {
                            safeAuthBackNavigation(navController)
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
    val context = LocalContext.current
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU
    val policyLinkColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary

    var hasInteracted by remember { mutableStateOf(false) }
    var hasRequestedPhoneHint by remember { mutableStateOf(false) }
    val requestPhoneNumberHint = rememberPhoneNumberHintRequester(
        onPhoneNumberReceived = { selectedPhoneNumber ->
            hasInteracted = true
            onPhoneNumberChange(selectedPhoneNumber)
        },
        onUnavailable = { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    )

    val phoneValidationError = remember(phoneNumber, hasInteracted) {
        if (!hasInteracted || phoneNumber.isEmpty() || phoneNumber.length < 10) null
        else ValidationUtils.getPhoneError(phoneNumber, true)
    }

    LoginPhoneEntrySection(
        phoneNumber = phoneNumber,
        selectedCountryCode = selectedCountryCode,
        phoneValidationError = phoneValidationError,
        otpState = otpState,
        isCheckingPhone = isCheckingPhone,
        isTelugu = isTelugu,
        onPhoneNumberChange = { newValue ->
            hasInteracted = true
            onPhoneNumberChange(newValue)
        },
        onPhoneFocused = {
            if (!hasRequestedPhoneHint && phoneNumber.isBlank()) {
                hasRequestedPhoneHint = true
                requestPhoneNumberHint()
            }
        },
        onContinueClick = {
            Timber.d("📱 Login - Continue button clicked")
            onContinueClick()
        },
        onBackClick = onBackClick,
        onRegisterClick = onRegisterClick
    )
    return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp),
        horizontalAlignment = Alignment.Start
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .offset(x = (-12).dp)
                .padding(bottom = 4.dp)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = if (isTelugu) "వెనక్కి" else "Back",
                tint = WorkerColors.TextPrimary
            )
        }

        Text(
            text = if (isTelugu) "తిరిగి స్వాగతం" else "Welcome back",
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold),
            color = WorkerColors.TextPrimary,
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isTelugu) "లాగిన్ కోసం మీ మొబైల్ నంబర్ నమోదు చేయండి" else "Enter your mobile number to login",
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
                modifier = Modifier
                    .weight(1f)
                    .height(53.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !hasRequestedPhoneHint && phoneNumber.isBlank()) {
                            hasRequestedPhoneHint = true
                            requestPhoneNumberHint()
                        }
                    },
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
                append(if (isTelugu) "కొనసాగించడం ద్వారా, మీరు మా " else "By continuing, you agree to our ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = policyLinkColor)) {
                    append(if (isTelugu) "సేవా నిబంధనలు" else "Terms of Service")
                }
                append(if (isTelugu) " మరియు " else " and ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = policyLinkColor)) {
                    append(if (isTelugu) "గోప్యతా విధానం" else "Privacy Policy")
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
                text = if (isTelugu) "ఖాతా లేదా? " else "Don't have an account? ",
                style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary)
            )
            TextButton(
                onClick = onRegisterClick,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (isTelugu) "ఇప్పుడే నమోదు చేయండి" else "Register Now",
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

@Composable
internal fun LoginPhoneEntrySection(
    phoneNumber: String,
    selectedCountryCode: String,
    phoneValidationError: String?,
    otpState: com.example.dutype.viewmodels.OtpState,
    isCheckingPhone: Boolean,
    isTelugu: Boolean,
    onPhoneNumberChange: (String) -> Unit,
    onPhoneFocused: () -> Unit,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val appContext = LocalContext.current
    val buttonEnabled = ValidationUtils.isValidIndianPhoneNumber(phoneNumber) && !otpState.isLoading && !isCheckingPhone

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-12).dp)
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (isTelugu) "వెనక్కి" else "Back",
                    tint = WorkerColors.TextPrimary
                )
            }

            TextButton(
                onClick = {
                    val whatsappNumber = "919121706236" // DutyPe support number
                    val message = "Hello DutyPe Team! I am facing a login issue. Please help me log in."
                    val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
                    val whatsappUrl = "https://wa.me/$whatsappNumber?text=$encodedMessage"

                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse(whatsappUrl)
                            setPackage("com.whatsapp")
                        }
                        appContext.startActivity(intent)
                    } catch (e: Exception) {
                        val browserIntent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(whatsappUrl)
                        )
                        appContext.startActivity(browserIntent)
                    }
                },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        contentDescription = null,
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTelugu) "సపోర్ట్" else "Contact Support",
                        style = AppTypography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WorkerColors.TextPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (isTelugu) "తిరిగి స్వాగతం" else "Welcome back",
            style = AppTypography.displayTitle.copy(
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = WorkerColors.TextPrimary
            ),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isTelugu) "సమీప ఉద్యోగాలను వెంటనే కనుగొనండి" else "Find nearby jobs instantly",
            style = AppTypography.bodyLarge.copy(
                color = WorkerColors.TextSecondary,
                lineHeight = 24.sp
            ),
            textAlign = TextAlign.Start
        )

        if (phoneValidationError != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = phoneValidationError, color = WorkerColors.Error, style = AppTypography.caption)
        }

        Spacer(modifier = Modifier.height(34.dp))

        Text(
            text = if (isTelugu) "మొబైల్ నంబర్" else "Mobile Number",
            style = AppTypography.bodyLarge.copy(
                color = WorkerColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        AuthPhoneEntryField(
            phoneNumber = phoneNumber,
            onPhoneNumberChange = onPhoneNumberChange,
            selectedCountryCode = selectedCountryCode,
            hasError = phoneValidationError != null,
            onFocused = onPhoneFocused,
            placeholderText = ""
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthPrimaryButton(
            text = stringResource(R.string.continue_text),
            enabled = buttonEnabled,
            isLoading = isCheckingPhone || otpState.isLoading,
            onClick = onContinueClick
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isTelugu) "కొత్త ఖాతా ఉందా? " else "Create a new account? ",
                style = AppTypography.bodyLarge.copy(color = WorkerColors.TextSecondary)
            )
            TextButton(
                onClick = onRegisterClick,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (isTelugu) "ఖాతా సృష్టించండి" else "Create Account",
                    style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
internal fun AuthScreenBackdrop() {
    val isDark = LocalDarkMode.current
    val bloomLarge = if (isDark) Color(0xFF26262B).copy(alpha = 0.45f) else Color(0xFFEDEDEF).copy(alpha = 0.34f)
    val bloomSmall = if (isDark) Color(0xFF26262B).copy(alpha = 0.55f) else Color(0xFFEDEDEF).copy(alpha = 0.58f)
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 44.dp, y = 76.dp)
                .size(196.dp)
                .clip(CircleShape)
                .background(bloomLarge)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-26).dp, y = 260.dp)
                .size(78.dp)
                .clip(CircleShape)
                .background(bloomSmall)
        )
    }
}

@Composable
internal fun AuthMark(size: androidx.compose.ui.unit.Dp = 52.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3A3A40), Color(0xFF101013))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo_foreground),
            contentDescription = null,
            modifier = Modifier.size(size * 0.7f)
        )
    }
}

@Composable
internal fun LoginArtwork(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(150.dp)
            .height(126.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(106.dp)
                .clip(CircleShape)
                .background(WorkerColors.PrimaryLight.copy(alpha = 0.52f))
        )
        listOf(22.dp, 52.dp, 84.dp).forEachIndexed { index, xOffset ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-xOffset), y = (-26 + index * 4).dp)
                    .width(18.dp)
                    .height((36 + index * 14).dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(WorkerColors.Border.copy(alpha = 0.42f))
            )
        }
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = WorkerColors.Primary,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-24).dp, y = 8.dp)
                .size(58.dp)
        )
    }
}

@Composable
internal fun AuthPhoneEntryField(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountryCode: String,
    hasError: Boolean,
    onFocused: () -> Unit,
    placeholderText: String = "98765 43210"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(RoundedCornerShape(18.dp))
            .background(WorkerColors.CardBackground)
            .border(1.dp, if (hasError) WorkerColors.Error else WorkerColors.Border, RoundedCornerShape(18.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .width(96.dp)
                .height(58.dp)
                .clickable { }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "🇮🇳", fontSize = 22.sp, fontFamily = MeeshoFontFamily)
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = WorkerColors.IconPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(58.dp)
                .background(WorkerColors.Border)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedCountryCode,
                style = AppTypography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WorkerColors.TextPrimary
                )
            )
            Spacer(modifier = Modifier.width(22.dp))
            BasicTextField(
                value = phoneNumber,
                onValueChange = { newValue ->
                    onPhoneNumberChange(newValue.filter { it.isDigit() }.take(10))
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) onFocused()
                    },
                singleLine = true,
                textStyle = AppTypography.bodyLarge.copy(
                    fontSize = 18.sp,
                    color = WorkerColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(WorkerColors.Primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                decorationBox = { innerTextField ->
                    if (phoneNumber.isBlank() && placeholderText.isNotBlank()) {
                        Text(
                            text = placeholderText,
                            style = AppTypography.bodyLarge.copy(
                                fontSize = 18.sp,
                                color = WorkerColors.TextTertiary
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
internal fun AuthPrimaryButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) WorkerColors.Primary else WorkerColors.Divider,
            contentColor = Color.White,
            disabledContainerColor = WorkerColors.Divider,
            disabledContentColor = WorkerColors.TextDisabled
        ),
        shape = RoundedCornerShape(18.dp),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.2.dp, modifier = Modifier.size(22.dp))
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = text,
                    style = AppTypography.buttonLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Center)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(30.dp)
                )
            }
        }
    }
}

@Composable
internal fun SecureOtpLine(text: String = "Secure OTP Login") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = WorkerColors.Primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = AppTypography.bodyLarge.copy(color = WorkerColors.TextSecondary)
        )
    }
}

@Composable
internal fun OrDivider(label: String = "or") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(WorkerColors.Border)
        )
        Text(
            text = label,
            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary),
            modifier = Modifier.padding(horizontal = 22.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(WorkerColors.Border)
        )
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
    val context = LocalContext.current
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if (isTelugu) "డ్యూటీపీకి స్వాగతం" else "Welcome to DutyPe",
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
                append(if (isTelugu) "SMS ద్వారా పంపిన 6 అంకెల కోడ్‌ను ఇక్కడ నమోదు చేయండి: " else "Enter the 6-digit code sent via SMS at ")
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = WorkerColors.TextPrimary
                    )
                ) {
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

        AuthOtpBoxes(
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
                            contentDescription = if (isTelugu) "OTP మళ్లీ పంపండి" else "Resend OTP",
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
                    text = if (isTelugu) "OTP మళ్లీ పంపండి" else "Resend OTP",
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
                    Text(if (isTelugu) "ధృవీకరించండి" else "Verify", style = AppTypography.buttonLarge)
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
