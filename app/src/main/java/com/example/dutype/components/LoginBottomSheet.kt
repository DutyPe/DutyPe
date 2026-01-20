package com.example.dutype.components

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
import androidx.compose.animation.with
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.FirestoreUtils
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
    otpViewModel: OtpViewModel = hiltViewModel(),
    profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
) {
    if (!isVisible) return
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val otpState by otpViewModel.otpState.collectAsState()  
    
    var phoneNumber by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    var isCheckingPhone by remember { mutableStateOf(false) }
    var isCheckingProfile by remember { mutableStateOf(false) }
    val selectedCountryCode = "+91"
    
    // Referral code state - moved to parent scope so it's accessible in onContinueClick
    var referralCode by remember { mutableStateOf("") }
    var showReferralInput by remember { mutableStateOf(false) }
    var isValidatingCode by remember { mutableStateOf(false) }
    var codeValidationError by remember { mutableStateOf<String?>(null) }
    var validatedReferrerName by remember { mutableStateOf<String?>(null) }
    var hasAlreadyUsedReferral by remember { mutableStateOf(false) }
    
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
                    val userId = currentUser.uid
                    isCheckingProfile = true
                    
                    // Fetch user data from Firestore
                    var existingUserData: Map<String, Any>? = null
                    try {
                        existingUserData = FirestoreUtils.getUserByUid(userId)
                    } catch (e: Exception) {
                        Timber.w(e, "📱 Failed to fetch user data")
                    }
                    
                    if (existingUserData != null) {
                        val userRole = existingUserData["role"] as? String
                        val profileComplete = existingUserData["profileCompleted"] as? Boolean ?: false
                        val fullName = existingUserData["fullName"] as? String
                        val gender = existingUserData["gender"] as? String
                        val address = existingUserData["address"] as? String ?: existingUserData["location"] as? String
                        
                        // Check for role mismatch
                        if (userRole != null) {
                            val existingRoleEnum = try { UserRole.valueOf(userRole.uppercase()) } catch (e: Exception) { null }
                            if (existingRoleEnum != null && existingRoleEnum != role) {
                                val roleDisplayName = userRole.lowercase().replaceFirstChar { it.uppercase() }
                                Toast.makeText(
                                    context,
                                    "This phone is registered as $roleDisplayName. Please login as $roleDisplayName.",
                                    Toast.LENGTH_LONG
                                ).show()
                                otpViewModel.resetState()
                                isCheckingProfile = false
                                return@LaunchedEffect
                            }
                        }
                        
                        // Save user info to local storage
                        if (userRole != null) {
                            val parsedRole = try { UserRole.valueOf(userRole.uppercase()) } catch (e: Exception) { null }
                            if (parsedRole != null) {
                                profileCompletionViewModel.updateUserRole(parsedRole)
                                if (profileComplete) {
                                    profileCompletionViewModel.markProfileComplete(parsedRole)
                                    profileCompletionViewModel.markProfileSetupAsShown(parsedRole)
                                }
                                profileCompletionViewModel.saveUserInfoToLocalStorage(
                                    email = existingUserData["email"] as? String ?: "",
                                    name = fullName ?: "",
                                    role = parsedRole
                                )
                            }
                        }
                        
                        // Check if profile check is required (job application flow)
                        if (requiresProfileCheck) {
                            // Required fields for job application: name, gender, location
                            val hasRequiredFields = !fullName.isNullOrBlank() && 
                                                   !gender.isNullOrBlank() && 
                                                   !address.isNullOrBlank()
                            
                            Timber.d("📱 LoginBottomSheet - Profile check: name=$fullName, gender=$gender, address=$address, hasRequired=$hasRequiredFields")
                            
                            if (!hasRequiredFields && onProfileSetupRequired != null) {
                                // Profile incomplete - navigate to profile setup
                                Timber.d("📱 LoginBottomSheet - Profile incomplete, navigating to setup")
                                otpViewModel.resetState()
                                isCheckingProfile = false
                                onProfileSetupRequired()
                                return@LaunchedEffect
                            }
                        }
                        
                        // Profile complete or no check required - proceed with success
                        otpViewModel.resetState()
                        isCheckingProfile = false
                        onLoginSuccess()
                    } else {
                        // New user - save role and phone number to Firebase
                        profileCompletionViewModel.updateUserRole(role)
                        
                        // Save phone number to Firebase for new users
                        val phoneToSave = currentUser.phoneNumber ?: otpState.phoneNumber
                        if (!phoneToSave.isNullOrBlank()) {
                            try {
                                FirestoreUtils.saveUserPhoneNumber(userId, phoneToSave, role.name)
                                Timber.d("📱 LoginBottomSheet - Saved phone number to Firebase: $phoneToSave")
                            } catch (e: Exception) {
                                Timber.w(e, "📱 Failed to save phone number to Firebase")
                            }
                        }
                        
                        // For job application flow, new users need profile setup
                        if (requiresProfileCheck && onProfileSetupRequired != null) {
                            Timber.d("📱 LoginBottomSheet - New user, navigating to profile setup")
                            otpViewModel.resetState()
                            isCheckingProfile = false
                            onProfileSetupRequired()
                            return@LaunchedEffect
                        }
                        
                        otpViewModel.resetState()
                        isCheckingProfile = false
                        onLoginSuccess()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "📱 Error in login flow")
                otpViewModel.resetState()
                isCheckingProfile = false
                onLoginSuccess()
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = {
            otpViewModel.resetState()
            onDismiss()
        },
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetMaxWidth = Dp.Unspecified
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle indicator
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE5E7EB))
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            AnimatedContent(
                targetState = !otpState.otpSent,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { if (targetState) -300 else 300 },
                        animationSpec = tween(350, easing = EaseOutCubic)
                    ) + fadeIn(animationSpec = tween(350)) with
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
                        title = title,
                        subtitle = subtitle,
                        phoneNumber = phoneNumber,
                        onPhoneNumberChange = { phoneNumber = it },
                        selectedCountryCode = selectedCountryCode,
                        otpState = otpState,
                        isCheckingPhone = isCheckingPhone,
                        referralCode = referralCode,
                        onReferralCodeChange = { referralCode = it },
                        showReferralInput = showReferralInput,
                        onShowReferralInputChange = { showReferralInput = it },
                        isValidatingCode = isValidatingCode,
                        onIsValidatingCodeChange = { isValidatingCode = it },
                        codeValidationError = codeValidationError,
                        onCodeValidationErrorChange = { codeValidationError = it },
                        validatedReferrerName = validatedReferrerName,
                        onValidatedReferrerNameChange = { validatedReferrerName = it },
                        hasAlreadyUsedReferral = hasAlreadyUsedReferral,
                        onHasAlreadyUsedReferralChange = { hasAlreadyUsedReferral = it },
                        onContinueClick = {
                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                            scope.launch {
                                try {
                                    isCheckingPhone = true
                                    
                                    val existingRole = profileCompletionViewModel.checkPhoneExistsWithDifferentRole(
                                        fullPhoneNumber, role
                                    )
                                    
                                    if (existingRole != null) {
                                        isCheckingPhone = false
                                        val roleDisplayName = existingRole.lowercase().replaceFirstChar { it.uppercase() }
                                        Toast.makeText(
                                            context,
                                            "This phone is registered as $roleDisplayName.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@launch
                                    }
                                    
                                    isCheckingPhone = false
                                    profileCompletionViewModel.saveAuthMethod("PHONE_OTP")
                                    profileCompletionViewModel.savePhoneNumber(fullPhoneNumber)
                                    
                                    // Save referral code if provided and user hasn't used one before
                                    if (referralCode.isNotBlank() && !hasAlreadyUsedReferral && validatedReferrerName != null) {
                                        profileCompletionViewModel.saveReferralCode(referralCode.trim().uppercase())
                                        Timber.d("🎁 REFERRAL: Saved referral code for signup: $referralCode")
                                    }
                                    
                                    otpViewModel.sendOtp(fullPhoneNumber, context)
                                } catch (e: Exception) {
                                    isCheckingPhone = false
                                    
                                    // Save referral code if provided and user hasn't used one before
                                    if (referralCode.isNotBlank() && !hasAlreadyUsedReferral && validatedReferrerName != null) {
                                        profileCompletionViewModel.saveReferralCode(referralCode.trim().uppercase())
                                        Timber.d("🎁 REFERRAL: Saved referral code for signup: $referralCode")
                                    }
                                    
                                    otpViewModel.sendOtp(fullPhoneNumber, context)
                                }
                            }
                        }
                    )
                } else {
                    // OTP Input Screen
                    OtpInputContent(
                        otpValue = otpValue,
                        onOtpChange = { otpValue = it },
                        phoneNumber = phoneNumber,
                        otpState = otpState,
                        onVerifyClick = { otpViewModel.verifyOtp(otpValue, context) },
                        onResendClick = {
                            val fullPhoneNumber = selectedCountryCode + phoneNumber
                            otpViewModel.resendOtp(fullPhoneNumber, context)
                        },
                        onBackClick = { otpViewModel.resetState() }
                    )
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
    selectedCountryCode: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    isCheckingPhone: Boolean,
    referralCode: String,
    onReferralCodeChange: (String) -> Unit,
    showReferralInput: Boolean,
    onShowReferralInputChange: (Boolean) -> Unit,
    isValidatingCode: Boolean,
    onIsValidatingCodeChange: (Boolean) -> Unit,
    codeValidationError: String?,
    onCodeValidationErrorChange: (String?) -> Unit,
    validatedReferrerName: String?,
    onValidatedReferrerNameChange: (String?) -> Unit,
    hasAlreadyUsedReferral: Boolean,
    onHasAlreadyUsedReferralChange: (Boolean) -> Unit,
    onContinueClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var hasInteracted by remember { mutableStateOf(false) }
    
    val phoneValidationError = remember(phoneNumber, hasInteracted) {
        if (!hasInteracted || phoneNumber.isEmpty() || phoneNumber.length < 10) null
        else ValidationUtils.getPhoneError(phoneNumber, true)
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Title
        Text(
            text = title,
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold),
            color = WorkerColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Subtitle
        Text(
            text = subtitle,
            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Phone input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.width(66.dp).height(53.dp),
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
                    hasInteracted = true
                    onPhoneNumberChange(filtered)
                },
                placeholder = { Text("9876543210", style = AppTypography.bodyLarge.copy(color = WorkerColors.TextTertiary)) },
                leadingIcon = {
                    Text(selectedCountryCode, style = AppTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                },
                trailingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = WorkerColors.IconSecondary)
                },
                modifier = Modifier.weight(1f).height(53.dp),
                singleLine = true,
                isError = phoneValidationError != null,
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (phoneValidationError != null) WorkerColors.Error else WorkerColors.TextPrimary,
                    unfocusedBorderColor = if (phoneValidationError != null) WorkerColors.Error else WorkerColors.Border,
                    cursorColor = WorkerColors.TextPrimary,
                    focusedContainerColor = WorkerColors.CardBackground,
                    unfocusedContainerColor = WorkerColors.CardBackground
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }
        
        if (phoneValidationError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(phoneValidationError, color = WorkerColors.Error, style = AppTypography.caption)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Referral Code Section (Optional) - Hide if user already used referral
        if (!hasAlreadyUsedReferral) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Have a referral code?",
                    style = AppTypography.bodyMedium.copy(
                        color = WorkerColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
                TextButton(
                    onClick = { onShowReferralInputChange(!showReferralInput) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (showReferralInput) "Hide" else "Enter Code",
                        style = AppTypography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WorkerColors.TextPrimary
                        )
                    )
                }
            }
        }
        
        // Referral Code Input (Expandable)
        if (!hasAlreadyUsedReferral) {
            AnimatedVisibility(
            visible = showReferralInput,
            enter = slideInVertically(
                initialOffsetY = { -20 },
                animationSpec = tween(300)
            ) + fadeIn(tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -20 },
                animationSpec = tween(300)
            ) + fadeOut(tween(300))
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = referralCode,
                    onValueChange = { newValue ->
                        // Only allow alphanumeric, uppercase, max 10 chars
                        val filtered = newValue.filter { it.isLetterOrDigit() }
                            .uppercase()
                            .take(10)
                        onReferralCodeChange(filtered)
                        
                        // Reset validation state when user types
                        onCodeValidationErrorChange(null)
                        onValidatedReferrerNameChange(null)
                        
                        // Validate if code is complete (9 chars: WRK123ABC or EMP456DEF)
                        if (filtered.length >= 9) {
                            onIsValidatingCodeChange(true)
                            scope.launch {
                                try {
                                    val referralService = com.example.dutype.services.ReferralService(
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                                        com.google.firebase.auth.FirebaseAuth.getInstance(),
                                        com.google.firebase.functions.FirebaseFunctions.getInstance(),
                                        com.example.dutype.services.DeviceFingerprintService(com.google.firebase.firestore.FirebaseFirestore.getInstance()),
                                        context
                                    )
                                    
                                    val validation = referralService.validateReferralCode(filtered)
                                    
                                    onIsValidatingCodeChange(false)
                                    
                                    if (validation.isValid) {
                                        onValidatedReferrerNameChange(validation.referrerName)
                                        onCodeValidationErrorChange(null)
                                        Timber.d("🎁 REFERRAL: Valid code - ${validation.referrerName}")
                                    } else {
                                        onCodeValidationErrorChange(validation.errorMessage)
                                        onValidatedReferrerNameChange(null)
                                        Timber.w("🎁 REFERRAL: Invalid code - ${validation.errorMessage}")
                                    }
                                } catch (e: Exception) {
                                    onIsValidatingCodeChange(false)
                                    onCodeValidationErrorChange("Failed to validate code")
                                    Timber.e(e, "🎁 REFERRAL: Validation error")
                                }
                            }
                        }
                    },
                    placeholder = { 
                        Text(
                            "WRK123ABC or EMP456DEF",
                            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextTertiary)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_share),
                            contentDescription = null,
                            tint = WorkerColors.IconSecondary
                        )
                    },
                    trailingIcon = {
                        when {
                            isValidatingCode -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = WorkerColors.TextPrimary
                                )
                            }
                            validatedReferrerName != null -> {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_info_details),
                                    contentDescription = "Valid",
                                    tint = WorkerColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            codeValidationError != null && referralCode.length >= 9 -> {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_delete),
                                    contentDescription = "Invalid",
                                    tint = WorkerColors.Error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            referralCode.isNotEmpty() -> {
                                androidx.compose.material3.IconButton(
                                    onClick = { 
                                        onReferralCodeChange("")
                                        onCodeValidationErrorChange(null)
                                        onValidatedReferrerNameChange(null)
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                        contentDescription = "Clear",
                                        tint = WorkerColors.IconSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            else -> null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(53.dp),
                    singleLine = true,
                    isError = codeValidationError != null && referralCode.length >= 9,
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Validation feedback
                when {
                    validatedReferrerName != null -> {
                        Text(
                            text = "✓ Valid code from ${validatedReferrerName}",
                            style = AppTypography.caption.copy(color = WorkerColors.Success)
                        )
                    }
                    codeValidationError != null && referralCode.length >= 9 -> {
                        Text(
                            text = codeValidationError ?: "Invalid code",
                            style = AppTypography.caption.copy(color = WorkerColors.Error)
                        )
                    }
                    else -> {
                        Text(
                            text = "Optional: Enter referral code to earn ₹10 bonus",
                            style = AppTypography.caption.copy(color = WorkerColors.TextSecondary)
                        )
                    }
                }
            }
        }
        } // Close if (!hasAlreadyUsedReferral)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val buttonEnabled = ValidationUtils.isValidIndianPhoneNumber(phoneNumber) && !otpState.isLoading && !isCheckingPhone
        
        // Check if user already used referral code
        LaunchedEffect(phoneNumber) {
            if (ValidationUtils.isValidIndianPhoneNumber(phoneNumber)) {
                try {
                    val fullPhone = selectedCountryCode + phoneNumber
                    val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    
                    if (userId != null) {
                        // Check if user already has a referral record
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val referralSnapshot = db.collection("referrals")
                            .whereEqualTo("referredUserId", userId)
                            .limit(1)
                            .get()
                            .await()
                        
                        if (!referralSnapshot.isEmpty) {
                            onHasAlreadyUsedReferralChange(true)
                            onShowReferralInputChange(false)
                            Timber.d("🎁 REFERRAL: User already used referral code")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "🎁 REFERRAL: Error checking existing referral")
                }
            }
        }
        
        Button(
            onClick = {
                // Validate referral code before continuing
                if (referralCode.isNotBlank() && codeValidationError != null) {
                    Toast.makeText(context, "Please enter a valid referral code or clear it", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
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
                Text("Continue", style = AppTypography.buttonLarge)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Terms and Privacy Policy with clickable links
        val termsUrl = com.example.dutype.utils.AppConstants.TERMS_URL
        val privacyUrl = com.example.dutype.utils.AppConstants.PRIVACY_URL
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "By continuing, you agree to our ",
                style = AppTypography.caption.copy(color = WorkerColors.TextSecondary)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Terms",
                style = AppTypography.caption.copy(
                    fontWeight = FontWeight.Bold,
                    color = WorkerColors.Info,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                modifier = Modifier.clickable {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(termsUrl))
                    context.startActivity(intent)
                }
            )
            Text(
                text = " and ",
                style = AppTypography.caption.copy(color = WorkerColors.TextSecondary)
            )
            Text(
                text = "Privacy Policy",
                style = AppTypography.caption.copy(
                    fontWeight = FontWeight.Bold,
                    color = WorkerColors.Info,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                modifier = Modifier.clickable {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(privacyUrl))
                    context.startActivity(intent)
                }
            )
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

@Composable
private fun OtpInputContent(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    phoneNumber: String,
    otpState: com.example.dutype.viewmodels.OtpState,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Verify OTP",
            style = AppTypography.pageTitle.copy(fontWeight = FontWeight.Bold),
            color = WorkerColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = buildAnnotatedString {
                append("Enter the 6-digit code sent to ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)) {
                    append("+91 $phoneNumber")
                }
            },
            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary)
        )
        
        TextButton(onClick = onBackClick, modifier = Modifier.padding(top = 4.dp)) {
            Text(
                "Change number?",
                style = AppTypography.bodyMedium.copy(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                color = WorkerColors.TextPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // OTP Input boxes
        OtpInputBoxes(
            otpValue = otpValue,
            onOtpChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) onOtpChange(it) },
            digitCount = 6
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Timer and resend
        var remainingSeconds by remember { mutableIntStateOf(60) }
        var timerActive by remember { mutableStateOf(true) }
        
        LaunchedEffect(timerActive) {
            while (timerActive && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                if (remainingSeconds == 0) timerActive = false
            }
        }
        
        // Reset timer when OTP is successfully sent (for resend)
        LaunchedEffect(otpState.otpSent) {
            if (otpState.otpSent && !timerActive && remainingSeconds == 0) {
                remainingSeconds = 60
                timerActive = true
            }
        }
        
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
                            remainingSeconds = 60
                            timerActive = true
                            onResendClick()
                        },
                        modifier = Modifier.size(53.dp),
                        enabled = remainingSeconds == 0 || !timerActive
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_revert),
                            contentDescription = "Resend OTP",
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

                // Resend hint text
                Text(
                    text = "Resend OTP",
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
                    CircularProgressIndicator(
                        color = WorkerColors.CardBackground,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("Verify", style = AppTypography.buttonLarge)
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
    var isFocused by remember { mutableStateOf(false) }

    // When this composable is first shown, mark as focused
    LaunchedEffect(Unit) {
        isFocused = true
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isFocused = true }
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
                            color = if (isFilledIndex) WorkerColors.SuccessLight else WorkerColors.CardBackground,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                isFocusedIndex -> WorkerColors.TextPrimary
                                isFilledIndex -> WorkerColors.Success
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
                if (newValue.length <= digitCount && newValue.all { it.isDigit() }) {
                    onOtpChange(newValue)
                    isFocused = true
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { isFocused = true }
                .alpha(0f),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
            decorationBox = { innerTextField ->
                innerTextField()
            }
        )
    }
}
