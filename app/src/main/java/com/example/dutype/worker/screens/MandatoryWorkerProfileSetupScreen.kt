package com.example.dutype.worker.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.components.SelfieCaptureStep
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.services.NotificationService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.services.FCMTokenManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Mandatory Worker Profile Setup Screen
 * Enhanced with 30+ years of Android development experience
 * Pre-fills Google Sign-In email and makes profile setup mandatory
 * 
 * REFACTORED: Removed ServiceProvider anti-pattern
 * Services are now accessed via ProfileCompletionViewModel
 * 
 * FIX: Using rememberSaveable for form state to survive activity recreation
 * when camera is launched (process death scenario)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandatoryWorkerProfileSetupScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    // Services accessed via ProfileCompletionViewModel (proper DI pattern)
    val locationService = profileCompletionViewModel.locationService
    val fcmTokenManager = profileCompletionViewModel.fcmTokenManager
    val notificationService = profileCompletionViewModel.notificationService
    
    // Form state - using rememberSaveable to survive activity recreation (camera launch)
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var dateOfBirth by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var skills by rememberSaveable { mutableStateOf("") }
    var experience by rememberSaveable { mutableStateOf("") }
    
    // Selfie state - Uri cannot be saved directly, so we save the string representation
    var selfieUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val selfieUri = selfieUriString?.let { Uri.parse(it) }
    var selfieUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isUploadingSelfie by remember { mutableStateOf(false) }
    var selfieError by remember { mutableStateOf<String?>(null) }
    
    // UI state - currentStep must survive activity recreation
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by rememberSaveable { mutableStateOf(1) }
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    var isProfileCompleted by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var isEmailLoaded by remember { mutableStateOf(false) }
    var authMethod by rememberSaveable { mutableStateOf<String?>(null) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }  // Show errors only after Next click
    val totalSteps = 4  // Added selfie step
    
    // Load saved user info based on authentication method
    LaunchedEffect(Unit) {
        // Get auth method to determine which field to prefill
        authMethod = profileCompletionViewModel.getAuthMethod()
        
        Timber.d("MandatoryWorkerProfileSetupScreen - Auth Method: $authMethod")
        
        when (authMethod) {
            "GOOGLE" -> {
                // Google Auth Flow: Prefill email and name from Google
                val savedEmail = profileCompletionViewModel.getUserEmail()
                val savedName = profileCompletionViewModel.getUserName()
                
                Timber.d("Google Auth Flow - savedEmail=$savedEmail, savedName=$savedName")
                
                if (savedEmail != null) {
                    email = savedEmail
                    isEmailLoaded = true
                }
                if (savedName != null) {
                    fullName = savedName
                }
            }
            
            "PHONE_OTP" -> {
                // OTP Auth Flow: Prefill phone number only, leave email empty for user to enter
                val savedPhone = profileCompletionViewModel.getPhoneNumber()
                
                Timber.d("OTP Auth Flow - savedPhone=$savedPhone")
                
                if (savedPhone != null) {
                    phoneNumber = savedPhone
                }
                // Email is NOT prefilled for OTP flow - user can manually enter it
                // Name is also NOT prefilled for OTP flow
            }
            
            else -> {
                Timber.w("Unknown auth method: $authMethod")
                isEmailLoaded = true
            }
        }
        
        Timber.d("After loading - email=$email, fullName=$fullName, phoneNumber=$phoneNumber")
    }
    
    // Email is locked and cannot be changed
    val isEmailLocked = when (authMethod) {
        "GOOGLE" -> email.isNotBlank()  // Google auth: email is read-only if provided
        "PHONE_OTP" -> false            // OTP auth: email is optional, user can enter it
        else -> email.isNotBlank()
    }
    
    // Phone is locked and cannot be changed for OTP auth
    val isPhoneLocked = when (authMethod) {
        "PHONE_OTP" -> phoneNumber.isNotBlank()  // OTP auth: phone is read-only if provided
        "GOOGLE" -> false                         // Google auth: phone is optional, user must enter it
        else -> false
    }
    
    // Email is required only for Google auth
    val isEmailRequired = authMethod == "GOOGLE"
    
    // Phone is always required
    val isPhoneRequired = true
    
    // Simple white background
    val backgroundColor = Color.White
    
    // Animation state for smooth transitions
    val animatedProgress by animateFloatAsState(
        targetValue = currentStep.toFloat() / totalSteps.toFloat(),
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "progress"
    )
    
    // Step-specific validation based on auth method
    // For GOOGLE auth: fullName (prefilled), email (prefilled, read-only), phoneNumber (user enters), address
    // For OTP auth: fullName (user enters), email (optional), phoneNumber (prefilled, read-only), address

    
    val isStep1Valid = when (authMethod) {
        "GOOGLE" -> {
            // Google: fullName (prefilled), phone (user enters - must be valid 10 digits), email (prefilled, required)
            fullName.isNotBlank() && ValidationUtils.isValidIndianPhoneNumber(phoneNumber) && email.isNotBlank()
        }
        "PHONE_OTP" -> {
            // OTP: fullName (user enters - required), phone (prefilled - valid), email (OPTIONAL - not required)
            fullName.isNotBlank() && ValidationUtils.isValidIndianPhoneNumber(phoneNumber)
        }
        else -> {
            // Default: fullName and phone required, email optional
            fullName.isNotBlank() && ValidationUtils.isValidIndianPhoneNumber(phoneNumber)
        }
    }
    val isStep2Valid = address.isNotBlank() && dateOfBirth.isNotBlank() && ValidationUtils.isValidDateOfBirth(dateOfBirth) && gender.isNotBlank()
    val isStep3Valid = skills.isNotBlank() && experience.isNotBlank()
    val isStep4Valid = selfieUri != null  // Selfie is mandatory
    
    // Overall form validation
    val isFormValid = isStep1Valid && isStep2Valid && isStep3Valid && isStep4Valid
    
    
    // Current step validation
    val isCurrentStepValid = when (currentStep) {
        1 -> isStep1Valid
        2 -> isStep2Valid
        3 -> isStep3Valid
        4 -> isStep4Valid
        else -> false
    }
    
    // Validation error messages - Show only when user clicks Next
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var dateOfBirthError by remember { mutableStateOf<String?>(null) }
    var genderError by remember { mutableStateOf<String?>(null) }
    var skillsError by remember { mutableStateOf<String?>(null) }
    var experienceError by remember { mutableStateOf<String?>(null) }
    
    // Debug logging for form validation
    LaunchedEffect(fullName, email, phoneNumber, address, dateOfBirth, gender, currentStep, isCurrentStepValid) {
        // Only update error messages when user tries to proceed (showValidationErrors = true)
        if (showValidationErrors) {
            // Update phone error
            phoneError = when {
                phoneNumber.isBlank() -> "Phone number is required"
                !ValidationUtils.isValidIndianPhoneNumber(phoneNumber) && phoneNumber.isNotBlank() -> "Enter a valid 10-digit phone number"
                else -> null
            }
            
            // Update email error
            emailError = when {
                email.isNotBlank() && !ValidationUtils.isValidEmail(email) -> "Enter a valid email address"
                else -> null
            }
            
            // Update full name error
            fullNameError = when {
                fullName.isBlank() -> "Full name is required"
                else -> null
            }
            
            // Update address error
            addressError = when {
                address.isBlank() -> "Address is required"
                else -> null
            }
            
            // Update date of birth error with age validation
            dateOfBirthError = ValidationUtils.getDateOfBirthError(dateOfBirth)
            
            // Update gender error
            genderError = when {
                gender.isBlank() -> "Gender is required"
                else -> null
            }
            
            // Update skills error
            skillsError = when {
                skills.isBlank() -> "Skills are required"
                else -> null
            }
            
            // Update experience error
            experienceError = when {
                experience.isBlank() -> "Experience is required"
                else -> null
            }
        } else {
            // Clear all errors when not showing validation
            phoneError = null
            emailError = null
            fullNameError = null
            addressError = null
            dateOfBirthError = null
            genderError = null
            skillsError = null
            experienceError = null
        }
        
        Timber.d("Form validation - step=$currentStep, step1Valid=$isStep1Valid, step2Valid=$isStep2Valid, step3Valid=$isStep3Valid, currentValid=$isCurrentStepValid")
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                
                // PREMIUM Main Content Card with stunning design
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        animationSpec = tween(700, easing = EaseOutCubic),
                        initialOffsetY = { it / 2 }
                    ) + fadeIn(animationSpec = tween(600))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp)
                            .shadow(
                                elevation = 28.dp,
                                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                                ambientColor = Color(0xFF1F2937).copy(alpha = 0.2f),
                                spotColor = Color(0xFF1F2937).copy(alpha = 0.15f)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.spacedBy(28.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // Step 1: Personal Information
                            if (currentStep == 1) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                                ) {
                                    PersonalInformationStep(
                                        fullName = fullName,
                                        email = email,
                                        phoneNumber = phoneNumber,
                                        authMethod = authMethod,
                                        phoneError = if (showValidationErrors) phoneError else null,
                                        emailError = if (showValidationErrors) emailError else null,
                                        fullNameError = if (showValidationErrors) fullNameError else null,
                                        onFullNameChange = { fullName = it },
                                        onEmailChange = { newEmail ->
                                            // Email can be changed only for OTP auth (or when not from Google)
                                            if (authMethod != "GOOGLE" || email.isBlank()) {
                                                email = newEmail
                                            }
                                        },
                                        onPhoneChange = { newPhone ->
                                            // Phone can be changed only for non-OTP auth (or when not from OTP)
                                            if (authMethod != "PHONE_OTP" || phoneNumber.isBlank()) {
                                                phoneNumber = newPhone
                                            }
                                        }
                                    )
                                }
                            }
                            
                            // Step 2: Additional Details
                            if (currentStep == 2) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                                ) {
                                    AdditionalDetailsStep(
                                        address = address,
                                        dateOfBirth = dateOfBirth,
                                        gender = gender,
                                        addressError = if (showValidationErrors) addressError else null,
                                        dateOfBirthError = if (showValidationErrors) dateOfBirthError else null,
                                        genderError = if (showValidationErrors) genderError else null,
                                        onAddressChange = { address = it },
                                        onDateOfBirthChange = { dateOfBirth = it },
                                        onGenderChange = { gender = it },
                                        locationService = locationService
                                    )
                                }
                            }
                            
                            // Step 3: Professional Information
                            if (currentStep == 3) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                                ) {
                                    ProfessionalInformationStep(
                                        skills = skills,
                                        experience = experience,
                                        skillsError = if (showValidationErrors) skillsError else null,
                                        experienceError = if (showValidationErrors) experienceError else null,
                                        onSkillsChange = { skills = it },
                                        onExperienceChange = { experience = it }
                                    )
                                }
                            }
                            
                            // Step 4: Selfie Capture (Mandatory)
                            if (currentStep == 4) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                                ) {
                                    SelfieCaptureStep(
                                        selfieUri = selfieUri,
                                        isUploading = isUploadingSelfie,
                                        selfieError = if (showValidationErrors && selfieUri == null) "Please take a selfie to continue" else selfieError,
                                        isEmployer = false,
                                        onSelfieCapture = { uri ->
                                            selfieUriString = uri.toString()
                                            selfieError = null
                                            Timber.d("📸 Worker selfie captured: $uri")
                                        },
                                        onRetake = {
                                            selfieUriString = null
                                            selfieUrl = null
                                        }
                                    )
                                }
                            }
                        
                            // Error Message
                            if (errorMessage != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = errorMessage ?: "",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = Color(0xFFEF4444)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Enhanced Navigation Buttons - Fixed at bottom
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Previous Button (back arrow only)
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier
                                .size(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF1F2937)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp, 
                                Color(0xFF1F2937).copy(alpha = 0.3f)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    // Next/Complete Button
                    Button(
                        onClick = {
                            // Show validation errors when Next is clicked
                            showValidationErrors = true
                            
                            if (isCurrentStepValid) {
                                if (currentStep < totalSteps) {
                                    currentStep++
                                    showValidationErrors = false  // Reset errors for next step
                                } else {
                                    // Complete profile setup
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        selfieError = null
                                        
                                        try {
                                            // Save profile data to Firestore
                                            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                            if (currentUser != null) {
                                                // First upload selfie if available
                                                var uploadedSelfieUrl: String? = null
                                                if (selfieUri != null) {
                                                    isUploadingSelfie = true
                                                    try {
                                                        val uploadResult = profileCompletionViewModel.uploadProfileImage(
                                                            selfieUri!!,
                                                            currentUser.uid,
                                                            "WORKER"
                                                        )
                                                        uploadResult.fold(
                                                            onSuccess = { url ->
                                                                uploadedSelfieUrl = url
                                                                selfieUrl = url
                                                                Timber.d("📸 Worker selfie uploaded: $url")
                                                            },
                                                            onFailure = { e ->
                                                                Timber.e(e, "📸 Failed to upload worker selfie")
                                                                // Show error but continue - selfie upload is not blocking
                                                                selfieError = "Photo upload failed. Your profile will be saved without photo."
                                                            }
                                                        )
                                                    } catch (e: Exception) {
                                                        Timber.e(e, "📸 Exception during selfie upload")
                                                        selfieError = "Photo upload failed. Your profile will be saved without photo."
                                                    } finally {
                                                        isUploadingSelfie = false
                                                    }
                                                }
                                                
                                                val workerProfileData = mutableMapOf(
                                                    "fullName" to fullName,
                                                    "email" to email,
                                                    "phone" to phoneNumber,  // Changed from phoneNumber to phone to match Firebase
                                                    "address" to address,
                                                    "dateOfBirth" to dateOfBirth,
                                                    "gender" to gender,
                                                    "skills" to skills,
                                                    "experience" to experience,
                                                    "role" to "WORKER",
                                                    "profileCompleted" to true,
                                                    "completedAt" to System.currentTimeMillis()
                                                )
                                                
                                                // Add selfie URL if uploaded
                                                if (uploadedSelfieUrl != null) {
                                                    workerProfileData["profileImageUrl"] = uploadedSelfieUrl!!
                                                }
                                                
                                                // Save to Firestore using ProfileCompletionViewModel
                                                profileCompletionViewModel.saveWorkerProfileData(workerProfileData)
                                            }

                                            // Save role to local DataStore so app knows which home to navigate to on reopen
                                            profileCompletionViewModel.updateUserRole(UserRole.WORKER)
                                            
                                            // Mark profile as complete
                                            profileCompletionViewModel.markProfileComplete(UserRole.WORKER)

                                            // Mark profile setup as shown for worker
                                            profileCompletionViewModel.markProfileSetupAsShown(UserRole.WORKER)
                                            
                                            // Send profile completion notification (welcome message)
                                            val notificationUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                            if (notificationUser != null) {
                                                try {
                                                    notificationService.sendProfileCompleteNotification(
                                                        userName = fullName,
                                                        userId = notificationUser.uid,
                                                        userRole = "WORKER"
                                                    )
                                                    Timber.d("📬 Profile completion notification sent for worker")
                                                    
                                                    // Register FCM token with role for push notifications
                                                    fcmTokenManager.registerTokenWithRole("WORKER")
                                                    Timber.d("📬 FCM token registered with WORKER role")
                                                } catch (e: Exception) {
                                                    Timber.e(e, "📬 Failed to send profile completion notification or register FCM")
                                                }
                                            }
                                            
                                            // Navigate to worker home
                                            navController.navigate(Routes.WORKER_HOME) {
                                                popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Failed to complete profile setup"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCurrentStepValid) Color(0xFF1F2937) else Color(0xFF9CA3AF)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (currentStep == totalSteps) "Complete Profile" else "Next",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        if (currentStep < totalSteps) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun PersonalInformationStep(
    fullName: String,
    email: String,
    phoneNumber: String,
    authMethod: String?,
    phoneError: String?,
    emailError: String?,
    fullNameError: String?,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1F2937).copy(alpha = 0.15f),
                                Color(0xFF60A5FA).copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF1F2937),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "Tell us about yourself",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Full Name
        Column {
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = { Text("Full Name *") },
                placeholder = { Text("Enter your full name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(14.dp),
                isError = fullNameError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (fullNameError != null) Color(0xFFDC2626) else Color(0xFF1F2937),
                    unfocusedBorderColor = if (fullNameError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                    focusedLabelColor = if (fullNameError != null) Color(0xFFDC2626) else Color(0xFF1F2937),
                    errorBorderColor = Color(0xFFDC2626),
                    cursorColor = Color(0xFF1F2937)
                ),
                singleLine = true
            )
            if (fullNameError != null) {
                Text(
                    text = fullNameError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        // Email - Behavior differs based on authentication method
        if (authMethod == "GOOGLE") {
            // Google Auth: Email is prefilled and read-only
            OutlinedTextField(
                value = email,
                onValueChange = { }, // Read-only
                label = { Text("Email Address (Verified)") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color(0xFFE5E7EB),
                    disabledTextColor = Color(0xFF6B7280)
                ),
                trailingIcon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified by Google",
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        } else if (authMethod == "PHONE_OTP") {
            // OTP Auth: Email is optional and editable
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email Address (Optional)") },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (emailError != null) Color(0xFFDC2626) else Color(0xFF1F2937),
                        unfocusedBorderColor = if (emailError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                        errorBorderColor = Color(0xFFDC2626)
                    ),
                    singleLine = true
                )
                if (emailError != null) {
                    Text(
                        text = emailError,
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        } else {
            // Default: Email is editable
            Column {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (emailError != null) Color(0xFFDC2626) else Color(0xFF1F2937),
                        unfocusedBorderColor = if (emailError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                        errorBorderColor = Color(0xFFDC2626)
                    ),
                    singleLine = true
                )
                if (emailError != null) {
                    Text(
                        text = emailError,
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }

        // Phone Number - Behavior differs based on authentication method
        if (authMethod == "PHONE_OTP") {
            // OTP Auth: Phone is prefilled and read-only
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { }, // Read-only
                label = { Text("Phone Number (Verified)") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color(0xFFE5E7EB),
                    disabledTextColor = Color(0xFF6B7280)
                ),
                trailingIcon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified by OTP",
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        } else {
            // Google Auth (or default): Phone is editable and mandatory
            Column {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { newValue ->
                        // Only allow digits and limit to 10 characters
                        if (newValue.all { it.isDigit() } && newValue.length <= 10) {
                            onPhoneChange(newValue)
                        }
                    },
                    label = { Text("Phone Number *") },
                    placeholder = { Text("Enter 10-digit phone number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (phoneError != null) Color(0xFFDC2626) else Color(0xFF1F2937),
                        unfocusedBorderColor = if (phoneError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                        errorBorderColor = Color(0xFFDC2626)
                    )
                )
                if (phoneError != null) {
                    Text(
                        text = phoneError,
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }

        // Address removed from here - now in Step 2
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdditionalDetailsStep(
    address: String,
    dateOfBirth: String,
    gender: String,
    addressError: String?,
    dateOfBirthError: String?,
    genderError: String?,
    onAddressChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    locationService: com.example.dutype.utils.LocationService
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Column(
        modifier = Modifier.padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1F2937).copy(alpha = 0.15f),
                                Color(0xFF60A5FA).copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF1F2937),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = "Additional Details",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "Complete your profile information",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Address
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Address *",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                
                var isFetchingLocation by remember { mutableStateOf(false) }
                var fetchError by remember { mutableStateOf<String?>(null) }
                val coroutineScope = rememberCoroutineScope()
                
                // Location permission launcher for fetch button
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    
                    Timber.d("📍 Fetch button - Location permission result: $granted")
                    
                    if (granted) {
                        // Permission granted, now fetch location with high accuracy
                        coroutineScope.launch {
                            isFetchingLocation = true
                            fetchError = null
                            try {
                                // Use getHighAccuracyLocation for GPS-level precision (5-10m)
                                val locationInfo = locationService.getHighAccuracyLocation(
                                    timeoutMs = 15000L,
                                    minAccuracyMeters = 10f
                                )
                                if (locationInfo != null) {
                                    // Use detailed full address for profile
                                    Timber.d("📍 Fetch button - High accuracy location fetched: ${locationInfo.getFullAddress()}")
                                    onAddressChange(locationInfo.getFullAddress())
                                } else {
                                    Timber.w("📍 Fetch button - Location is null, check if GPS is enabled")
                                    fetchError = "Could not get location. Please enable GPS."
                                    android.widget.Toast.makeText(context, "Could not get location. Please enable GPS.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "📍 Fetch button - Error fetching location")
                                fetchError = "Error fetching location"
                                android.widget.Toast.makeText(context, "Error fetching location", android.widget.Toast.LENGTH_SHORT).show()
                            } finally {
                                isFetchingLocation = false
                            }
                        }
                    } else {
                        Timber.w("📍 Fetch button - Location permission denied")
                        android.widget.Toast.makeText(context, "Location permission required to fetch address", android.widget.Toast.LENGTH_SHORT).show()
                        isFetchingLocation = false
                    }
                }
                
                Button(
                    onClick = {
                        Timber.d("📍 Fetch button clicked")
                        isFetchingLocation = true
                        fetchError = null
                        
                        if (locationService.hasLocationPermission()) {
                            Timber.d("📍 Fetch button - Has permission, fetching high accuracy location...")
                            coroutineScope.launch {
                                try {
                                    // Use getHighAccuracyLocation for GPS-level precision (5-10m)
                                    val locationInfo = locationService.getHighAccuracyLocation(
                                        timeoutMs = 15000L,
                                        minAccuracyMeters = 10f
                                    )
                                    if (locationInfo != null) {
                                        // Use detailed full address for profile
                                        Timber.d("📍 Fetch button - High accuracy location fetched: ${locationInfo.getFullAddress()}")
                                        onAddressChange(locationInfo.getFullAddress())
                                    } else {
                                        Timber.w("📍 Fetch button - Location is null")
                                        fetchError = "Could not get location. Please enable GPS."
                                        android.widget.Toast.makeText(context, "Could not get location. Please enable GPS.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "📍 Fetch button - Error fetching location")
                                    fetchError = "Error fetching location"
                                    android.widget.Toast.makeText(context, "Error fetching location", android.widget.Toast.LENGTH_SHORT).show()
                                } finally {
                                    isFetchingLocation = false
                                }
                            }
                        } else {
                            Timber.d("📍 Fetch button - No permission, requesting...")
                            // Request location permission
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111111)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFetchingLocation) "Fetching..." else "Fetch",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                placeholder = { Text("Enter your address", color = Color(0xFFD1D5DB)) },
                leadingIcon = { 
                    Icon(
                        Icons.Default.LocationOn, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF6B7280)
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                isError = addressError != null,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (addressError != null) Color(0xFFDC2626) else Color(0xFF1F2937),
                    unfocusedBorderColor = if (addressError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                    errorBorderColor = Color(0xFFDC2626)
                ),
                singleLine = true
            )
            if (addressError != null) {
                Text(
                    text = addressError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        // Date of Birth
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState()
            
            Text(
                text = "Date of Birth *",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .border(
                        width = 1.dp,
                        color = if (dateOfBirthError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF6B7280)
                        )
                        Text(
                            text = if (dateOfBirth.isEmpty()) "Select your date of birth" else dateOfBirth,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (dateOfBirth.isEmpty()) Color(0xFFD1D5DB) else Color(0xFF1F2937)
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF9CA3AF)
                    )
                }
            }
            
            if (showDatePicker) {
                // Modern calendar-style date picker matching reference design
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                    onDateOfBirthChange(formatter.format(java.util.Date(millis)))
                                    Timber.d("📅 Date selected: ${formatter.format(java.util.Date(millis))}")
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("OK", color = Color(0xFF009688), fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("CANCEL", color = Color(0xFF009688), fontWeight = FontWeight.SemiBold)
                        }
                    },
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White
                    )
                ) {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false, // Hide mode toggle for cleaner look
                        title = null, // Remove default title
                        headline = null, // Remove default headline
                        colors = DatePickerDefaults.colors(
                            containerColor = Color.White,
                            titleContentColor = Color.White,
                            headlineContentColor = Color.White,
                            weekdayContentColor = Color(0xFF6B7280),
                            subheadContentColor = Color.White,
                            yearContentColor = Color(0xFF1F2937),
                            currentYearContentColor = Color(0xFF009688),
                            selectedYearContentColor = Color.White,
                            selectedYearContainerColor = Color(0xFF009688),
                            dayContentColor = Color(0xFF1F2937),
                            selectedDayContentColor = Color.White,
                            selectedDayContainerColor = Color(0xFF009688),
                            todayContentColor = Color(0xFF009688),
                            todayDateBorderColor = Color(0xFF009688),
                            dayInSelectionRangeContentColor = Color(0xFF009688),
                            dayInSelectionRangeContainerColor = Color(0xFF009688).copy(alpha = 0.1f),
                            navigationContentColor = Color(0xFF1F2937)
                        )
                    )
                }
            }
            
            if (dateOfBirthError != null) {
                Text(
                    text = dateOfBirthError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        // Gender Selection
        Column {
            GenderSelectionField(
                selectedGender = gender,
                onGenderSelected = onGenderChange
            )
            if (genderError != null) {
                Text(
                    text = genderError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfessionalInformationStep(
    skills: String,
    experience: String,
    skillsError: String?,
    experienceError: String?,
    onSkillsChange: (String) -> Unit,
    onExperienceChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1F2937).copy(alpha = 0.15f),
                                Color(0xFF60A5FA).copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Work,
                    contentDescription = null,
                    tint = Color(0xFF1F2937),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = "Professional Information",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "Share your skills and experience",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // Skills with STUNNING Icon Chips
        Column {
            Text(
                text = "Skills * (Select all that apply)",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(bottom = 14.dp)
            )
            
            val skillsWithIcons = listOf(
                "Cooking" to Icons.Default.Restaurant,
                "Cleaning" to Icons.Default.CleaningServices,
                "Customer Service" to Icons.Default.SupportAgent,
                "Driving" to Icons.Default.DirectionsCar,
                "Gardening" to Icons.Default.Yard,
                "Security" to Icons.Default.Security,
                "Delivery" to Icons.Default.LocalShipping,
                "Warehouse" to Icons.Default.Warehouse,
                "Housekeeping" to Icons.Default.HomeWork,
                "Food Service" to Icons.Default.Restaurant,
                "Construction" to Icons.Default.Construction,
                "Electrician" to Icons.Default.ElectricalServices,
                "Plumbing" to Icons.Default.Plumbing,
                "Painting" to Icons.Default.FormatPaint,
                "Carpentry" to Icons.Default.Carpenter,
                "Others" to Icons.Default.MoreHoriz
            )
            
            var selectedSkills by remember { 
                mutableStateOf(
                    skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                )
            }
            var showAllSkills by remember { mutableStateOf(false) }
            
            val displaySkills = if (showAllSkills) skillsWithIcons else skillsWithIcons.take(6)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 0.dp, end = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in displaySkills.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (j in 0 until 2) {
                            if (i + j < displaySkills.size) {
                                val (skill, icon) = displaySkills[i + j]
                                val isSelected = selectedSkills.contains(skill)
                                val animatedScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.05f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                                
                                FilterChip(
                                    selected = isSelected,
                                    enabled = true,
                                    onClick = {
                                        selectedSkills = if (isSelected) {
                                            selectedSkills - skill
                                        } else {
                                            selectedSkills + skill
                                        }
                                        onSkillsChange(selectedSkills.joinToString(", "))
                                    },
                                    label = { 
                                        Text(
                                            skill,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            )
                                        ) 
                                    },
                                    leadingIcon = {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color(0xFF111111)
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer(
                                            scaleX = animatedScale,
                                            scaleY = animatedScale
                                        )
                                        .shadow(
                                            elevation = if (isSelected) 4.dp else 2.dp,
                                            shape = RoundedCornerShape(14.dp),
                                            ambientColor = Color(0xFF1F2937).copy(alpha = if (isSelected) 0.1f else 0f),
                                            spotColor = Color(0xFF1F2937).copy(alpha = if (isSelected) 0.08f else 0f)
                                        ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.White,
                                        labelColor = Color(0xFF1F2937),
                                        selectedContainerColor = Color.White,
                                        selectedLabelColor = Color(0xFF1F2937),
                                        selectedLeadingIconColor = Color(0xFF111111)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderWidth = 1.dp,
                                        borderColor = Color(0xFFD1D5DB),
                                        selectedBorderColor = Color(0xFF111111)
                                    )
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                // Show More button
                if (!showAllSkills && skillsWithIcons.size > 6) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAllSkills = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF111111)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                "Show More",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            
            if (skillsError != null) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = skillsError,
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Experience Level
        Column {
            var experienceExpanded by remember { mutableStateOf(false) }
            val experienceLevels = listOf(
                "Fresher (0-1 year)",
                "Entry Level (1-2 years)",
                "Intermediate (2-5 years)",
                "Experienced (5-10 years)",
                "Expert (10+ years)"
            )
            
            Text(
                text = "Experience Level *",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ExposedDropdownMenuBox(
                expanded = experienceExpanded,
                onExpandedChange = { experienceExpanded = it }
            ) {
                OutlinedTextField(
                    value = experience,
                    onValueChange = { },
                    readOnly = true,
                    placeholder = { Text("Select your experience level") },
                    leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = experienceExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = experienceError != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1F2937),
                        unfocusedBorderColor = Color(0xFFE5E7EB),
                        errorBorderColor = Color(0xFFDC2626),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = experienceExpanded,
                    onDismissRequest = { experienceExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    experienceLevels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level, color = Color(0xFF1F2937)) },
                            onClick = {
                                onExperienceChange(level)
                                experienceExpanded = false
                            }
                        )
                    }
                }
            }
            
            if (experienceError != null) {
                Text(
                    text = experienceError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderSelectionField(
    selectedGender: String,
    onGenderSelected: (String) -> Unit
) {
    val genderOptions = listOf("Male", "Female", "Other")
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Gender *",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            genderOptions.forEach { gender ->
                Row(
                    modifier = Modifier
                        .selectable(
                            selected = (gender == selectedGender),
                            onClick = { onGenderSelected(gender) },
                            role = Role.RadioButton
                        ),  
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (gender == selectedGender),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF111111),
                            unselectedColor = Color(0xFFD1D5DB)
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = gender,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                        color = Color(0xFF1F2937)
                    )
                }
            }
        }
    }
}

// Extension for rotating arrow animation
fun Modifier.animateRotation(isExpanded: Boolean): Modifier = this.then(
    Modifier.graphicsLayer {
        rotationZ = if (isExpanded) 180f else 0f
    }
)

// Extension for animating integer values
@Composable
fun animateIntAsState(
    targetValue: Int,
    animationSpec: AnimationSpec<Float> = spring()
): State<Int> {
    val floatValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = animationSpec
    )
    return derivedStateOf { floatValue.toInt() }
}
