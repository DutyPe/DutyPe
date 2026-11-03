package com.example.dutype.components

import com.dutype.app.R
import com.example.dutype.ui.theme.fg
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.auth.TruecallerLoginButton
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
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Login Result - Contains login outcome and profile status
 */
data class LoginResult(
    val isSuccess: Boolean,
    val userId: String? = null,
    val isProfileComplete: Boolean = false,
    val hasRequiredFields: Boolean = false, // name, gender, location
    val missingFields: List<String> = emptyList()
)

/**
 * Login Bottom Sheet - Reusable component for guest mode login prompts
 *
 * Shows a bottom sheet with OTP login when users try to access restricted features
 * without being logged in.
 *
 * Two flows supported:
 * 1. Job Application flow (requiresProfileCheck = true):
 *    - After login, checks if profile has required fields (name, gender, location)
 *    - If incomplete, calls onProfileSetupRequired() to navigate to ProfileSetup
 *    - If complete, calls onLoginSuccess()
 *
 * 2. Profile menu items flow (requiresProfileCheck = false):
 *    - After login, directly calls onLoginSuccess()
 *    - No profile check needed
 *
 * @param isVisible Whether the bottom sheet is visible
 * @param onDismiss Callback when the sheet is dismissed
 * @param onLoginSuccess Callback when login is successful (and profile is complete if required)
 * @param onProfileSetupRequired Callback when profile setup is needed (only for job application flow)
 * @param requiresProfileCheck Whether to check profile completion after login (true for job applications)
 * @param role The role to login as (WORKER or EMPLOYER)
 * @param title Optional custom title for the login prompt
 * @param subtitle Optional custom subtitle explaining why login is needed
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun LoginBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit,
    onProfileSetupRequired: (() -> Unit)? = null, // Called when profile needs setup (job application flow)
    requiresProfileCheck: Boolean = false, // true for job applications, false for profile menu items
    role: UserRole = UserRole.WORKER,
    title: String = "Login Required",
    subtitle: String = "Please login to continue with this action",
    navController: NavController? = null, // Used as fallback to auto-route new registrations to profile setup
    otpViewModel: OtpViewModel = hiltViewModel(),
    profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU
    val scope = rememberCoroutineScope()
    val otpState by otpViewModel.otpState.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    var isCheckingPhone by remember { mutableStateOf(false) }
    var isCheckingProfile by remember { mutableStateOf(false) }
    var isTruecallerBusy by remember { mutableStateOf(false) }
    val selectedCountryCode = "+91"

    val handleTruecallerAuthorized: (String, String) -> Unit = { code, codeVerifier ->
        isTruecallerBusy = true
        scope.launch {
            try {
                val result = otpViewModel.truecallerExchange(
                    authorizationCode = code,
                    codeVerifier = codeVerifier,
                    role = role.name,
                    mode = "unified"
                )
                result.fold(
                    onSuccess = { tcResult ->
                        if (tcResult.allowed && !tcResult.token.isNullOrBlank()) {
                            profileCompletionViewModel.savePhoneNumber(tcResult.phone)
                            val resolvedName = tcResult.name.trim().takeIf { it.isNotBlank() && !it.equals("User", ignoreCase = true) } ?: ""
                            if (resolvedName.isNotBlank()) {
                                profileCompletionViewModel.saveUserInfoToLocalStorage(
                                    email = tcResult.email,
                                    name = resolvedName,
                                    role = role
                                )
                            }
                            otpViewModel.signInWithTruecaller(tcResult)
                            isTruecallerBusy = false
                        } else {
                            isTruecallerBusy = false
                            if (tcResult.roleConflict) {
                                val existingRoleLabel = if (tcResult.existingRole?.contains("EMPLOYER", ignoreCase = true) == true) {
                                    if (isTelugu) "ఎంప్లాయర్" else "employer"
                                } else {
                                    if (isTelugu) "వర్కర్" else "worker"
                                }
                                Toast.makeText(
                                    context,
                                    if (isTelugu)
                                        "ఈ నంబర్ ${existingRoleLabel}గా నమోదైంది. దయచేసి ${existingRoleLabel}గా లాగిన్ అవ్వండి."
                                    else
                                        "This number is already registered as a $existingRoleLabel. Please log in as a $existingRoleLabel.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    if (isTelugu) "ట్రూకాలర్ లాగిన్ విఫలమైంది. దయచేసి OTP తో లాగిన్ అవ్వండి." else "Truecaller login failed. Please use OTP.",
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
                            err.message ?: (if (isTelugu) "ట్రూకాలర్ లాగిన్ విఫలమైంది." else "Truecaller login failed."),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                isTruecallerBusy = false
                Timber.e(e, "Truecaller login bottom sheet exception")
                Toast.makeText(context, e.message ?: "Truecaller login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val defaultAirbnbTitle = if (isTelugu) "లాగిన్ లేదా సైన్ అప్" else "Log in or sign up"
    val effectiveTitle = if (title.isBlank() || title == "Login Required" || title == "Sign In / Register") {
        defaultAirbnbTitle
    } else {
        title
    }
    val effectiveSubtitle = if (subtitle == "Please login to continue with this action") {
        if (isTelugu) "ఈ చర్య కొనసాగించడానికి దయచేసి లాగిన్ చేయండి" else subtitle
    } else {
        subtitle
    }

    // Set role context for FCM registration when bottom sheet is shown
    LaunchedEffect(isVisible, role) {
        if (isVisible) {
            otpViewModel.setRoleContext(role)
            Timber.d("📱 LoginBottomSheet - Role context set to $role for FCM")
        }
    }

    // Handle OTP verification success
    LaunchedEffect(otpState.otpVerified) {
        if (otpState.otpVerified) {
            Timber.d("📱 LoginBottomSheet - OTP verified successfully")

            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val loginResult = otpViewModel.resolveUnifiedAuth(role, fullName = null)
                    loginResult.fold(
                        onSuccess = { outcome ->
                            // Persist the name captured in the bottom-sheet registration form
                            // so the profile setup screen can pre-fill it for new users.
                            profileCompletionViewModel.saveUserInfoToLocalStorage(
                                email = "",
                                name = outcome.user?.name ?: "",
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
                                        "name" to (outcome.user?.name ?: ""),
                                        "updatedAt" to com.google.firebase.Timestamp.now()
                                    ),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )

                            val shouldGoToProfileSetup =
                                outcome.destination == OtpViewModel.PostOtpDestination.PROFILE_SETUP &&
                                    onProfileSetupRequired != null

                            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            otpViewModel.resetState()
                            isCheckingProfile = false

                            if (shouldGoToProfileSetup) {
                                onProfileSetupRequired.invoke()
                            } else if (
                                outcome.destination == OtpViewModel.PostOtpDestination.PROFILE_SETUP &&
                                navController != null
                            ) {
                                // New registrations must always finish profile setup before reaching the home screen.
                                val target = when (outcome.role) {
                                    UserRole.EMPLOYER -> Routes.EMPLOYER_PROFILE_SETUP
                                    else -> Routes.PROFILE_SETUP
                                }
                                navController.navigate(target) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                    launchSingleTop = true
                                }
                                onDismiss()
                            } else {
                                onLoginSuccess()
                            }
                        },
                        onFailure = { error ->
                            Timber.e(error, "LoginBottomSheet - Login resolution failed")
                            // Bug #9: surface single-role-per-phone block as a
                            // friendly toast instead of a generic error.
                            val msg = error.message.orEmpty()
                            val toastText = if (msg.contains("phone-already-registered-as:")) {
                                val existingRole = msg.substringAfter("phone-already-registered-as:").substringBefore(" ").substringBefore("\n").trim().lowercase()
                                val existingRoleLabel = if (existingRole.contains("employer")) "employer" else "worker"
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
                            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            otpViewModel.resetState()
                            isCheckingProfile = false
                        }
                    )
                    return@LaunchedEffect
                }
            } catch (e: Exception) {
                Timber.e(e, "📱 Error in login flow")
                focusManager.clearFocus(force = true)
            keyboardController?.hide()
            otpViewModel.resetState()
                isCheckingProfile = false
            }
        }
    }

    LaunchedEffect(otpState.error) {
        val err = otpState.error
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

    ModalBottomSheet(
        onDismissRequest = {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            otpViewModel.resetState()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        containerColor = if (com.example.dutype.ui.theme.isAppInDarkTheme()) Color(0xFF1A2233) else Color(0xFFFAFFFF),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetMaxWidth = Dp.Unspecified
    ) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val sheetHeight = configuration.screenHeightDp.dp * 0.50f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        focusManager.clearFocus(force = true)
            keyboardController?.hide()
            otpViewModel.resetState()
                        onDismiss()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isTelugu) "మూసివేయండి" else "Close",
                        tint = WorkerColors.TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent<Boolean>(
                targetState = !otpState.otpSent,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { if (targetState) -300 else 300 },
                        animationSpec = tween(350, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(350)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { if (targetState) 300 else -300 },
                        animationSpec = tween(350, easing = EaseInCubic)
                    ) + fadeOut(animationSpec = tween(350))
                },
                label = "login_animation"
            ) { isPhoneScreen ->
                if (isPhoneScreen) {
                    // Phone Input Screen
                    PhoneInputContent(
                        title = effectiveTitle,
                        subtitle = effectiveSubtitle,
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { phoneNumber = it },
                        role = role,
                        selectedCountryCode = selectedCountryCode,
                        otpState = otpState,
                        isCheckingPhone = isCheckingPhone,
                        isTruecallerBusy = isTruecallerBusy,
                        onTruecallerAuthorized = handleTruecallerAuthorized,
                        onTruecallerFailed = {
                            isTruecallerBusy = false
                        },
                        onContinueClick = {
                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                            scope.launch {
                                try {
                                    isCheckingPhone = true

                                    // PRE-OTP USER CHECK: Verify role match if user exists
                                    val phoneCheck = com.example.dutype.utils.FirestoreUtils.checkPhoneForRole(
                                        phoneNumber = fullPhoneNumber,
                                        requestedRole = role.name
                                    )
                                    val existingRoleLabel = when (phoneCheck.existingRole?.uppercase()) {
                                        "WORKER" -> if (isTelugu) "వర్కర్" else "worker"
                                        "EMPLOYER" -> if (isTelugu) "ఎంప్లాయర్" else "employer"
                                        else -> null
                                    }
                                    if (phoneCheck.exists == com.example.dutype.utils.FirestoreUtils.PhoneExistenceResult.EXISTS && phoneCheck.roleConflict && existingRoleLabel != null) {
                                        isCheckingPhone = false
                                        Toast.makeText(
                                            context,
                                            if (isTelugu) "ఈ నంబర్ $existingRoleLabel గా నమోదు అయింది. దయచేసి $existingRoleLabel గా లాగిన్ చేయండి."
                                            else "This number is registered as a $existingRoleLabel. Please log in as a $existingRoleLabel.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch
                                    }

                                    isCheckingPhone = false
                                    profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                    profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)

                                    otpViewModel.sendOtp(fullPhoneNumber, context, mode = "unified")
                                } catch (e: Exception) {
                                    isCheckingPhone = false
                                    Timber.e(e, "📱 Error in phone check, proceeding with OTP")
                                    otpViewModel.sendOtp(fullPhoneNumber, context, mode = "unified")
                                }
                            }
                        }
                    )
                } else {
                    // OTP Input Screen
                    val resendCooldown by otpViewModel.resendCooldownSeconds.collectAsState()
                    OtpInputContent(
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
                            otpViewModel.resendOtp(fullPhoneNumber, context, mode = "unified")
                        },
                        onBackClick = { otpViewModel.resetState(keepActiveSession = true) },
                        resendCooldownSeconds = resendCooldown
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun PhoneInputContent(
    title: String,
    subtitle: String,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    role: UserRole,
    selectedCountryCode: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    isCheckingPhone: Boolean,
    isTruecallerBusy: Boolean,
    onTruecallerAuthorized: (String, String) -> Unit,
    onTruecallerFailed: () -> Unit,
    onContinueClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU
    val scope = rememberCoroutineScope()
    var hasInteracted by remember { mutableStateOf(false) }
    var isInputFocused by remember { mutableStateOf(false) }
    val isDark = com.example.dutype.ui.theme.isAppInDarkTheme()

    val phoneValidationError = remember(phoneNumber, hasInteracted) {
        if (!hasInteracted || phoneNumber.isEmpty() || phoneNumber.length < 10) null
        else ValidationUtils.getPhoneError(phoneNumber, true)
    }

    val isButtonBusy = isCheckingPhone || otpState.isLoading

    val borderColor = when {
        phoneValidationError != null -> WorkerColors.Error
        isInputFocused -> if (isDark) Color(0xFF94A3B8) else Color(0xFF222222)
        else -> if (isDark) Color(0xFF334155) else Color(0xFFD1D5DB)
    }
    val inputBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimaryColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF222222)
    val placeholderColor = if (isDark) Color(0xFF64748B) else Color(0xFF9CA3AF)

    // Button always active with solid black styling (white in dark mode)
    val continueBtnContainer = if (isDark) Color.White else Color(0xFF222222)
    val continueBtnContent = if (isDark) Color(0xFF0F172A) else Color.White

    val handleContinueClick: () -> Unit = {
        val trimmed = phoneNumber.trim()
        if (trimmed.isEmpty()) {
            val msg = if (isTelugu) "దయచేసి ఫోన్ నంబర్ నమోదు చేయండి" else "Please enter phone number"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else if (trimmed.length < 10 || !ValidationUtils.isValidIndianPhoneNumber(trimmed)) {
            val msg = if (isTelugu) "దయచేసి సరైన 10 అంకెల మొబైల్ నంబర్ నమోదు చేయండి" else "Please enter a valid 10-digit mobile number"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else {
            onContinueClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered Airbnb Title
        Text(
            text = if (title.isNotBlank()) title else (if (isTelugu) "లాగిన్ లేదా సైన్ అప్" else "Log in or sign up"),
            style = AppTypography.displayTitle.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = (-0.3).sp
            ),
            color = WorkerColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Clean Phone Input Field with refined 1.dp border (not thick/shocking)
        // No popup bottom sheet - keyboard displays phone number suggestions natively
        BasicTextField(
            value = phoneNumber,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }.take(10)
                hasInteracted = true
                onPhoneNumberChange(filtered)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .onFocusChanged { focusState ->
                    isInputFocused = focusState.isFocused
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    handleContinueClick()
                }
            ),
            textStyle = AppTypography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textPrimaryColor
            ),
            cursorBrush = SolidColor(if (isDark) Color.White else Color(0xFF222222)),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(inputBg, RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (phoneNumber.isEmpty()) {
                        Text(
                            text = if (isTelugu) "ఫోన్ నంబర్" else "Phone number",
                            style = AppTypography.bodyLarge.copy(
                                color = placeholderColor,
                                fontSize = 16.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )

        if (phoneValidationError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = phoneValidationError,
                color = WorkerColors.Error,
                style = AppTypography.caption,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // DutyPe Consistent Black Continue Button - Always active, informs user if tapped empty
        Button(
            onClick = handleContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = continueBtnContainer,
                contentColor = continueBtnContent,
                disabledContainerColor = continueBtnContainer,
                disabledContentColor = continueBtnContent
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isButtonBusy,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            if (isButtonBusy) {
                ButtonThreeDotsLoader(dotColor = continueBtnContent)
            } else {
                Text(
                    text = if (isTelugu) "కొనసాగించండి" else "Continue",
                    style = AppTypography.buttonLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = continueBtnContent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Highlighted Bottom Section: 'or' Divider + Truecaller 1-Tap Login
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(0.8.dp)
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFE5E7EB))
            )
            Text(
                text = if (isTelugu) "లేదా" else "or",
                style = AppTypography.caption.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF717171)
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(0.8.dp)
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFE5E7EB))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Truecaller 1-Tap Login Secondary Button (White button with blue border)
        TruecallerLoginButton(
            busy = isTruecallerBusy,
            onAuthorized = onTruecallerAuthorized,
            onFailed = onTruecallerFailed,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = otpState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            ErrorCard(message = otpState.error)
        }
    }
}

@Composable
private fun OtpInputContent(
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if (isTelugu) "OTP ధృవీకరించండి" else "Verify OTP",
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold),
            color = WorkerColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isTelugu) "పంపిన 6 అంకెల కోడ్‌ను నమోదు చేయండి:" else "Enter the 6-digit code sent to",
            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary)
        )
        Text(
            text = "+91 $phoneNumber",
            style = AppTypography.bodyMedium.copy(
                color = WorkerColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        )

        TextButton(onClick = onBackClick, modifier = Modifier.padding(top = 2.dp)) {
            Text(
                if (isTelugu) "నంబర్ మార్చాలా?" else "Change number?",
                style = AppTypography.bodyMedium.copy(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                color = WorkerColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // OTP Input boxes
        if (otpState.channel == com.example.dutype.viewmodels.OtpChannel.WHATSAPP) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = painterResource(id = com.dutype.app.R.drawable.ic_whatsapp),
                    contentDescription = null,
                    tint = Color(0xFF25D366),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.dutype.app.R.string.auth_code_sent_whatsapp),
                    style = AppTypography.bodySmall.copy(
                        color = Color(0xFF1B5E20),
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        OtpInputBoxes(
            otpValue = otpValue,
            onOtpChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) onOtpChange(it) },
            digitCount = 6
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Timer and resend - Use ViewModel cooldown instead of local timer
        val timerActive = resendCooldownSeconds > 0
        val remainingSeconds = resendCooldownSeconds

        val otpButtonEnabled = otpValue.length == 6 && !otpState.isLoading

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Resend button with icon and timer overlay (matching EnhancedLoginScreen)
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

                    // Timer display overlay
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

                // Resend button label
                Text(
                    text = if (isTelugu) "OTP మళ్లీ పంపండి" else "Resend OTP",
                    style = AppTypography.labelSmall.copy(
                        color = if (timerActive && remainingSeconds > 0) WorkerColors.TextDisabled else WorkerColors.TextSecondary
                    )
                )
            }

            Button(
                onClick = onVerifyClick,
                enabled = otpButtonEnabled,
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
                shape = RoundedCornerShape(12.dp)
            ) {
                if (otpState.isLoading) {
                    ButtonThreeDotsLoader(dotColor = WorkerColors.CardBackground)
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
            ErrorCard(message = otpState.error)
        }
    }
}

/**
 * OTP Input Boxes - 6 digit OTP input with individual boxes
 * Uses a single invisible BasicTextField for proper input handling
 */
@Composable
fun OtpInputBoxes(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    digitCount: Int = 6
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    // When this composable is first shown, mark as focused
    LaunchedEffect(Unit) {
        isFocused = true
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isFocused = true
                focusRequester.requestFocus()
                keyboardController?.show()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            repeat(digitCount) { index ->
                val isFocusedIndex = index == otpValue.length && isFocused
                val isFilledIndex = index < otpValue.length
                val digit = otpValue.getOrNull(index)?.toString() ?: ""

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(
                            color = WorkerColors.CardBackground,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                isFocusedIndex -> WorkerColors.TextPrimary
                                isFilledIndex -> WorkerColors.TextPrimary
                                else -> WorkerColors.Border
                            },
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        style = AppTypography.pageTitle.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = WorkerColors.TextPrimary
                    )
                }
            }
        }

        // Invisible text field for input - captures all digits properly
        androidx.compose.foundation.text.BasicTextField(
            value = otpValue,
            onValueChange = { newValue ->
                val normalizedOtp = newValue.filter { it.isDigit() }.take(digitCount)
                if (normalizedOtp != otpValue) {
                    onOtpChange(normalizedOtp)
                }
                isFocused = true
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused || focusState.hasFocus
                }
                .alpha(0f),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
            decorationBox = { innerTextField ->
                innerTextField()
            }
        )
    }
}
