package com.example.dutype.employer.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.components.ReferralValidationResult
import com.example.dutype.components.isValidReferralCode
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.dutype.app.R

/**
 * Mandatory Employer Profile Setup Screen
 * 
 * FIX: Using rememberSaveable for form state to survive activity recreation
 * when camera is launched (process death scenario)
 * 
 * @param navController Navigation controller for screen navigation
 * @param returnRoute Optional route to navigate to after profile completion (e.g., post_job)
 */
@Composable
fun MandatoryEmployerProfileSetupScreen(
    navController: NavController,
    returnRoute: String? = null,
    profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val reviewTriggerService = rememberInAppReviewTriggerService()
    // Services accessed via ProfileCompletionViewModel (proper DI pattern)
    val locationService = profileCompletionViewModel.locationService
    val notificationService = profileCompletionViewModel.notificationService
    val fcmTokenManager = profileCompletionViewModel.fcmTokenManager

    // Form state - using rememberSaveable to survive activity recreation (camera launch)
    var companyName by rememberSaveable { mutableStateOf("") }
    var contactEmail by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var businessAddress by rememberSaveable { mutableStateOf("") }
    var industry by rememberSaveable { mutableStateOf("") }
    var companySize by rememberSaveable { mutableStateOf("") }
    var dateOfBirth by rememberSaveable { mutableStateOf("") }
    var gstNumber by rememberSaveable { mutableStateOf("") } // Optional GST for business verification
    
    // Selfie state - Uri cannot be saved directly, so we save the string representation
    var selfieUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val selfieUri = selfieUriString?.let { Uri.parse(it) }
    var selfieUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isUploadingSelfie by remember { mutableStateOf(false) }
    var selfieError by remember { mutableStateOf<String?>(null) }
    
    // Referral code state - REMOVED: Now handled in login flow before profile setup
    // Referral codes must be entered during registration, not profile setup
    var referralCode by rememberSaveable { mutableStateOf("") }
    var isValidatingReferral by remember { mutableStateOf(false) }
    var referralValidationResult by remember { mutableStateOf<ReferralValidationResult?>(null) }
    var hasAlreadyUsedReferral by remember { mutableStateOf(true) } // Always true to hide referral section
    var showReferralSection by remember { mutableStateOf(false) } // Always false - referral handled in login

    // UI state - currentStep must survive activity recreation
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingExistingData by remember { mutableStateOf(true) } // Loading existing profile data
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by rememberSaveable { mutableStateOf(1) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }
    var gender by rememberSaveable { mutableStateOf("") }
    val totalSteps = 2  // Selfie capture step removed

    // INDUSTRY BEST PRACTICE: Load existing profile data from Firebase (Single Source of Truth)
    // This handles both new users and existing users with partial data
    // Pattern used by: Google, Uber, Airbnb, LinkedIn
    // Full profile data is loaded for form prefilling - all fields are needed
    LaunchedEffect(Unit) {
        isLoadingExistingData = true
        try {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // REMOVED: Referral code retrieval - now handled in login screen
                // Referral is applied immediately after OTP, not during profile setup
                
                // Check if user has already used a referral code
                hasAlreadyUsedReferral = profileCompletionViewModel.hasUserUsedReferralCode(currentUser.uid)
                showReferralSection = !hasAlreadyUsedReferral
                Timber.d("🎁 REFERRAL: hasAlreadyUsedReferral=$hasAlreadyUsedReferral, showReferralSection=$showReferralSection")
                
                // Load full profile data for prefilling (all fields needed for form)
                val existingDataResult = profileCompletionViewModel.loadExistingProfileData()
                existingDataResult.onSuccess { existingData ->
                    Timber.d("📦 PREFILL: Loading existing employer profile data (lightweight)")
                    
                    // Prefill form fields with existing data (schema-compliant fields only)
                    val savedCompanyName = existingData["companyName"] as? String
                    val savedContactEmail = existingData["email"] as? String
                    val savedContactPhone = existingData["phone"] as? String
                    val savedProfileImageUrl = existingData["profileImageUrl"] as? String
                    // employer_profiles fields
                    val savedIndustry = existingData["industry"] as? String
                    val savedCompanySize = existingData["companySize"] as? String
                    val savedGstNumber = existingData["gstNumber"] as? String
                    
                    // Apply prefilled values (only if current field is empty)
                    if (companyName.isBlank() && !savedCompanyName.isNullOrBlank()) {
                        companyName = savedCompanyName
                        Timber.d("📦 PREFILL: companyName = $companyName")
                    }
                    if (contactEmail.isBlank() && !savedContactEmail.isNullOrBlank()) {
                        contactEmail = savedContactEmail
                        Timber.d("📦 PREFILL: contactEmail = $contactEmail")
                    }
                    if (contactPhone.isBlank() && !savedContactPhone.isNullOrBlank()) {
                        // Clean phone number (remove country code if present)
                        contactPhone = savedContactPhone.replace("+91", "").trim()
                        Timber.d("📦 PREFILL: contactPhone = $contactPhone")
                    }
                    if (industry.isBlank() && !savedIndustry.isNullOrBlank()) {
                        industry = savedIndustry
                        Timber.d("📦 PREFILL: industry = $industry")
                    }
                    if (companySize.isBlank() && !savedCompanySize.isNullOrBlank()) {
                        companySize = savedCompanySize
                        Timber.d("📦 PREFILL: companySize = $companySize")
                    }
                    if (gstNumber.isBlank() && !savedGstNumber.isNullOrBlank()) {
                        gstNumber = savedGstNumber
                        Timber.d("📦 PREFILL: gstNumber = $gstNumber")
                    }
                    if (!savedProfileImageUrl.isNullOrBlank()) {
                        selfieUrl = savedProfileImageUrl
                        Timber.d("📦 PREFILL: profileImageUrl exists")
                    }
                }
                
                // Fallback: Load from Google Sign-In if fields still empty
                if (contactEmail.isBlank()) {
                    val savedEmail = profileCompletionViewModel.getUserEmail()
                    if (savedEmail != null) {
                        contactEmail = savedEmail
                        Timber.d("📦 PREFILL: contactEmail from Google = $contactEmail")
                    }
                }
                if (companyName.isBlank()) {
                    val savedName = profileCompletionViewModel.getUserName()
                    if (savedName != null) {
                        companyName = savedName
                        Timber.d("📦 PREFILL: companyName from Google = $companyName")
                    }
                }
                
                // Load phone from OTP auth if available
                if (contactPhone.isBlank()) {
                    val savedPhone = profileCompletionViewModel.getPhoneNumber()
                    if (savedPhone != null) {
                        contactPhone = savedPhone.replace("+91", "").trim()
                        Timber.d("📦 PREFILL: contactPhone from OTP = $contactPhone")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "📦 PREFILL: Error loading existing profile data")
        } finally {
            isLoadingExistingData = false
        }
    }

    // Validation logic
    val isStep1Valid = companyName.isNotBlank() && industry.isNotBlank()
    val isStep2Valid = ValidationUtils.isValidIndianPhoneNumber(contactPhone) && businessAddress.isNotBlank() &&
            (contactEmail.isBlank() || ValidationUtils.isValidEmail(contactEmail)) && gender.isNotBlank() && 
            (dateOfBirth.isBlank() || ValidationUtils.isValidDateOfBirth(dateOfBirth))  // Date of birth is optional
    val isStep3Valid = true  // Selfie is optional - always valid

    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var companyNameError by remember { mutableStateOf<String?>(null) }
    var industryError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var genderError by remember { mutableStateOf<String?>(null) }
    var dateOfBirthError by remember { mutableStateOf<String?>(null) }

    // Update errors only when showValidationErrors is true
    LaunchedEffect(contactPhone, contactEmail, companyName, industry, businessAddress, gender, dateOfBirth, showValidationErrors) {
        if (showValidationErrors) {
            phoneError = when {
                contactPhone.isBlank() -> "Phone number is required"
                !ValidationUtils.isValidIndianPhoneNumber(contactPhone) -> "Enter a valid 10-digit phone number"
                else -> null
            }
            emailError = when {
                contactEmail.isNotBlank() && !ValidationUtils.isValidEmail(contactEmail) -> "Enter a valid email address"
                else -> null
            }
            companyNameError = if (companyName.isBlank()) "Company name is required" else null
            industryError = if (industry.isBlank()) "Please select at least one industry" else null
            addressError = if (businessAddress.isBlank()) "Work location is required" else null
            genderError = if (gender.isBlank()) "Please select your gender" else null
            dateOfBirthError = if (dateOfBirth.isNotBlank()) ValidationUtils.getDateOfBirthError(dateOfBirth) else null  // Optional field
        } else {
            phoneError = null
            emailError = null
            companyNameError = null
            industryError = null
            addressError = null
            genderError = null
            dateOfBirthError = null
        }
    }

    val isCurrentStepValid = when (currentStep) {
        1 -> isStep1Valid
        2 -> isStep2Valid
        3 -> isStep3Valid  // Selfie step (was step 4)
        else -> false
    }

    // Guard to prevent double-execution of handleCompletion
    var isCompletionInProgress by remember { mutableStateOf(false) }

    fun handleCompletion() {
        // Prevent double-execution
        if (isCompletionInProgress) {
            Timber.w("📍 Profile completion already in progress, ignoring duplicate call")
            return
        }
        
        isCompletionInProgress = true
        scope.launch {
            isLoading = true
            errorMessage = null
            selfieError = null
            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    throw IllegalStateException("User not authenticated")
                } else {
                    // First upload selfie if available
                    var uploadedSelfieUrl: String? = null
                    if (selfieUri != null) {
                        isUploadingSelfie = true
                        try {
                            val uploadResult = profileCompletionViewModel.uploadProfileImage(
                                selfieUri!!,
                                currentUser.uid,
                                "EMPLOYER"
                            )
                            uploadResult.fold(
                                onSuccess = { url ->
                                    uploadedSelfieUrl = url
                                    selfieUrl = url
                                    Timber.d("📸 Employer selfie uploaded: $url")
                                },
                                onFailure = { e ->
                                    Timber.e(e, "📸 Failed to upload employer selfie")
                                    // Show error but continue - selfie upload is not blocking
                                    selfieError = "Photo upload failed. Your profile will be saved without photo."
                                }
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "📸 Exception during employer selfie upload")
                            selfieError = "Photo upload failed. Your profile will be saved without photo."
                        } finally {
                            isUploadingSelfie = false
                        }
                    }
                    
                    val employerProfileData = mutableMapOf(
                        "companyName" to companyName,
                        "phone" to contactPhone
                    )

                    // Store email if provided
                    if (contactEmail.isNotBlank()) {
                        employerProfileData["email"] = contactEmail.trim()
                    }

                    // Persist all collected business fields. The Firestore rule
                    // and ProfileCompletionService both whitelist these keys —
                    // omitting any of them previously caused silent data loss.
                    if (industry.isNotBlank()) {
                        employerProfileData["industry"] = industry.trim()
                    }
                    if (companySize.isNotBlank()) {
                        employerProfileData["companySize"] = companySize.trim()
                    }
                    if (businessAddress.isNotBlank()) {
                        employerProfileData["businessAddress"] = businessAddress.trim()
                    }
                    if (gstNumber.isNotBlank()) {
                        employerProfileData["gstNumber"] = gstNumber.trim()
                    }

                    // Add selfie URL if uploaded
                    if (uploadedSelfieUrl != null) {
                        employerProfileData["profileImageUrl"] = uploadedSelfieUrl!!
                    }

                    profileCompletionViewModel
                        .saveEmployerProfileData(employerProfileData)
                        .getOrThrow()

                    // Referral apply is non-critical for the navigation gate —
                    // run it AFTER the user has been routed to home so the
                    // "saving" sheet dismisses promptly. Failure here only
                    // affects bonus crediting; the profile itself is saved.
                    val savedReferralCode = profileCompletionViewModel.getReferralCode()
                    if (!savedReferralCode.isNullOrBlank()) {
                        scope.launch {
                            runCatching {
                                val referralApplyResult = profileCompletionViewModel.applyReferralCode(
                                    referralCode = savedReferralCode,
                                    newUserId = currentUser.uid,
                                    newUserRole = UserRole.EMPLOYER.name,
                                    newUserName = companyName.ifBlank { contactPhone },
                                    newUserPhone = contactPhone
                                )
                                if (referralApplyResult.isSuccess) {
                                    Toast.makeText(
                                        context,
                                        "Referral bonus credited successfully",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Timber.w(
                                        "🎁 REFERRAL: Employer fallback apply failed: ${referralApplyResult.exceptionOrNull()?.message}"
                                    )
                                }
                            }.onFailure { Timber.w(it, "🎁 REFERRAL: background apply error") }
                        }
                    }
                }

                // Save role to local DataStore so app knows which home to navigate to on reopen
                profileCompletionViewModel.saveUserInfoToLocalStorage(companyName, UserRole.EMPLOYER)
                
                // Check if this is the FIRST time completing profile (not an update)
                val wasAlreadyComplete = profileCompletionViewModel.isProfileComplete(UserRole.EMPLOYER)
                
                profileCompletionViewModel.markProfileComplete(UserRole.EMPLOYER)
                profileCompletionViewModel.markProfileSetupAsShown(UserRole.EMPLOYER)

                // Notification + FCM register + review trigger are all
                // non-critical for navigation. Move to fire-and-forget so the
                // user isn't blocked by Cloud Function round-trips.
                if (!wasAlreadyComplete) {
                    val notificationUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (notificationUser != null) {
                        scope.launch {
                            runCatching {
                                notificationService.sendProfileCompleteNotification(
                                    userName = companyName,
                                    userId = notificationUser.uid,
                                    userRole = "EMPLOYER"
                                ).onSuccess {
                                    Timber.d("📬 Profile completion notification sent for employer (first time)")
                                }.onFailure { error ->
                                    Timber.e(error, "📬 Employer profile completion notification failed")
                                }
                                fcmTokenManager.registerTokenWithRole("EMPLOYER")
                                Timber.d("📬 FCM token registered with EMPLOYER role")
                            }.onFailure { Timber.e(it, "📬 Background notification/FCM failure") }
                        }
                    }

                    val activity = context as? Activity
                    if (activity != null) {
                        reviewTriggerService.onEmployerProfileCompleted(activity)
                    }
                } else {
                    Timber.d("📬 Profile already complete - skipping notification (this is a profile update)")
                }

                // Navigate to the returnRoute if provided (e.g. post_job after
                // a guest filled the form and completed profile setup), otherwise
                // fall back to the employer home screen.
                //
                // BUG #1 FIX: `EMPLOYER_POST_JOB` (and other employer-shell
                // routes) only exist inside the nested `EmployerMainScreen`
                // NavHost — not in this outer graph. Navigating to them
                // directly from here crashes with
                // `Navigation destination ... cannot be found in the navigation
                // graph`. We instead navigate the outer controller to
                // `EMPLOYER_HOME` (which mounts `EmployerMainScreen`) and queue
                // the desired inner route via `EmployerInnerNavQueue`; the
                // employer shell drains it on first composition.
                if (returnRoute != null) {
                    Timber.d("📍 Profile complete - queuing inner route '$returnRoute' and navigating to EMPLOYER_HOME")
                    com.example.dutype.navigation.EmployerInnerNavQueue.setPending(returnRoute)
                    navController.navigate(Routes.EMPLOYER_HOME) {
                        popUpTo(Routes.EMPLOYER_PROFILE_SETUP) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    Timber.d("📍 Profile complete - navigating to EMPLOYER_HOME")
                    navController.navigate(Routes.EMPLOYER_HOME) {
                        popUpTo(Routes.EMPLOYER_PROFILE_SETUP) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to complete profile setup"
            } finally {
                isLoading = false
                isCompletionInProgress = false
            }
        }
    }
    
    // Show loading while fetching existing profile data
    if (isLoadingExistingData) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFF3B82F6)
                )
                androidx.compose.material3.Text(
                    "Loading your profile...",
                    color = Color.Gray
                )
            }
        }
        return
    }

    MandatoryEmployerProfileSetupContent(
        companyName = companyName,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
        businessAddress = businessAddress,
        industry = industry,
        companySize = companySize,
        gstNumber = gstNumber,
        gender = gender,
        dateOfBirth = dateOfBirth,
        selfieUri = selfieUri,
        isUploadingSelfie = isUploadingSelfie,
        selfieError = selfieError,
        isLoading = isLoading,
        errorMessage = errorMessage,
        currentStep = currentStep,
        totalSteps = totalSteps,
        isCurrentStepValid = isCurrentStepValid,
        showValidationErrors = showValidationErrors,
        phoneError = if (showValidationErrors) phoneError else null,
        emailError = if (showValidationErrors) emailError else null,
        companyNameError = if (showValidationErrors) companyNameError else null,
        industryError = if (showValidationErrors) industryError else null,
        addressError = if (showValidationErrors) addressError else null,
        genderError = if (showValidationErrors) genderError else null,
        dateOfBirthError = if (showValidationErrors) dateOfBirthError else null,
        referralCode = referralCode,
        isValidatingReferral = isValidatingReferral,
        referralValidationResult = referralValidationResult,
        showReferralSection = showReferralSection,
        hasAlreadyUsedReferral = hasAlreadyUsedReferral,
        locationService = locationService,
        onCompanyNameChange = { companyName = it },
        onContactEmailChange = { contactEmail = it },
        onContactPhoneChange = { contactPhone = it },
        onBusinessAddressChange = { businessAddress = it },
        onIndustryChange = { industry = it },
        onCompanySizeChange = { companySize = it },
        onGstNumberChange = { gstNumber = it },
        onGenderChange = { gender = it },
        onDateOfBirthChange = { dateOfBirth = it },
        onReferralCodeChange = { newCode ->
            referralCode = newCode
            if (referralValidationResult != null) {
                referralValidationResult = null
            }
        },
        onValidateReferral = { code ->
            if (code.isNotBlank() && isValidReferralCode(code)) {
                scope.launch {
                    isValidatingReferral = true
                    try {
                        val result = profileCompletionViewModel.validateReferralCode(code)
                        result.fold(
                            onSuccess = { referrerInfo ->
                                if (referrerInfo != null) {
                                    val roleDisplay = when (referrerInfo.second.uppercase()) {
                                        "EMPLOYER" -> "an Employer"
                                        "WORKER" -> "a Worker"
                                        else -> "a user"
                                    }
                                    referralValidationResult = ReferralValidationResult(
                                        isValid = true,
                                        message = "Valid code from $roleDisplay! You'll both earn ₹25.",
                                        referrerName = referrerInfo.first
                                    )
                                } else {
                                    referralValidationResult = ReferralValidationResult(
                                        isValid = false,
                                        message = "Referral code not found"
                                    )
                                }
                            },
                            onFailure = { e ->
                                referralValidationResult = ReferralValidationResult(
                                    isValid = false,
                                    message = e.message ?: "Invalid referral code"
                                )
                            }
                        )
                    } finally {
                        isValidatingReferral = false
                    }
                }
            }
        },
        onSelfieCapture = { uri ->
            selfieUriString = uri.toString()
            selfieError = null
            Timber.d("📸 Employer selfie captured: $uri")
        },
        onSelfieRetake = {
            selfieUriString = null
            selfieUrl = null
        },
        onPreviousClick = { currentStep-- },
        onNextClick = {
            showValidationErrors = true
            if (isCurrentStepValid) {
                currentStep++
                showValidationErrors = false
            }
        },
        onCompleteClick = {
            showValidationErrors = true
            if (isCurrentStepValid) {
                handleCompletion()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandatoryEmployerProfileSetupContent(
    companyName: String,
    contactEmail: String,
    contactPhone: String,
    businessAddress: String,
    industry: String,
    companySize: String,
    gstNumber: String,
    gender: String,
    dateOfBirth: String,
    selfieUri: Uri?,
    isUploadingSelfie: Boolean,
    selfieError: String?,
    isLoading: Boolean,
    errorMessage: String?,
    currentStep: Int,
    totalSteps: Int,
    isCurrentStepValid: Boolean,
    showValidationErrors: Boolean,
    phoneError: String?,
    emailError: String?,
    companyNameError: String?,
    industryError: String?,
    addressError: String?,
    genderError: String?,
    dateOfBirthError: String?,
    referralCode: String,
    isValidatingReferral: Boolean,
    referralValidationResult: ReferralValidationResult?,
    showReferralSection: Boolean,
    hasAlreadyUsedReferral: Boolean,
    locationService: com.example.dutype.utils.LocationService,
    onCompanyNameChange: (String) -> Unit,
    onContactEmailChange: (String) -> Unit,
    onContactPhoneChange: (String) -> Unit,
    onBusinessAddressChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onCompanySizeChange: (String) -> Unit,
    onGstNumberChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onReferralCodeChange: (String) -> Unit,
    onValidateReferral: (String) -> Unit,
    onSelfieCapture: (Uri) -> Unit,
    onSelfieRetake: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 28.dp,
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            ambientColor = Color.Black.copy(alpha = 0.12f),
                            spotColor = Color.Black.copy(alpha = 0.08f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (currentStep == 1) {
                            CompanyInformationStep(
                                companyName = companyName,
                                industry = industry,
                                companySize = companySize,
                                gstNumber = gstNumber,
                                companyNameError = companyNameError,
                                industryError = industryError,
                                referralCode = referralCode,
                                isValidatingReferral = isValidatingReferral,
                                referralValidationResult = referralValidationResult,
                                showReferralSection = showReferralSection,
                                hasAlreadyUsedReferral = hasAlreadyUsedReferral,
                                onCompanyNameChange = onCompanyNameChange,
                                onIndustryChange = onIndustryChange,
                                onCompanySizeChange = onCompanySizeChange,
                                onGstNumberChange = onGstNumberChange,
                                onReferralCodeChange = onReferralCodeChange,
                                onValidateReferral = onValidateReferral
                            )
                        }

                        if (currentStep == 2) {
                            ContactDetailsStep(
                                contactPhone = contactPhone,
                                businessAddress = businessAddress,
                                contactEmail = contactEmail,
                                gender = gender,
                                dateOfBirth = dateOfBirth,
                                phoneError = phoneError,
                                emailError = emailError,
                                addressError = addressError,
                                genderError = genderError,
                                dateOfBirthError = dateOfBirthError,
                                onContactPhoneChange = onContactPhoneChange,
                                onBusinessAddressChange = onBusinessAddressChange,
                                onContactEmailChange = onContactEmailChange,
                                onGenderChange = onGenderChange,
                                onDateOfBirthChange = onDateOfBirthChange,
                                locationService = locationService
                            )
                        }

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
                                        text = errorMessage,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFFEF4444)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = onPreviousClick,
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF3B82F6)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                onNextClick()
                            } else {
                                onCompleteClick()
                            }
                        },
                        enabled = isCurrentStepValid && !isLoading,
                        modifier = Modifier
                            .height(52.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (currentStep == totalSteps) "Finish" else "Next",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        if (currentStep < totalSteps) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyInformationStep(
    companyName: String,
    industry: String,
    companySize: String,
    gstNumber: String,
    companyNameError: String?,
    industryError: String?,
    referralCode: String,
    isValidatingReferral: Boolean,
    referralValidationResult: ReferralValidationResult?,
    showReferralSection: Boolean,
    hasAlreadyUsedReferral: Boolean,
    onCompanyNameChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onCompanySizeChange: (String) -> Unit,
    onGstNumberChange: (String) -> Unit,
    onReferralCodeChange: (String) -> Unit,
    onValidateReferral: (String) -> Unit
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
                                Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                Color(0xFFD8B4FE).copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = "Company Information",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "Tell us about your company",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Column {
            OutlinedTextField(
                value = companyName,
                onValueChange = onCompanyNameChange,
                label = { Text(stringResource(R.string.company_name_required)) },
                placeholder = { Text(stringResource(R.string.enter_company_name)) },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isError = companyNameError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (companyNameError != null) Color(0xFFEF4444) else Color(0xFF3B82F6),
                    unfocusedBorderColor = if (companyNameError != null) Color(0xFFEF4444) else Color(0xFFE5E7EB),
                    errorBorderColor = Color(0xFFEF4444)
                )
            )
            if (companyNameError != null) {
                Text(
                    text = companyNameError,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        Column {
            Text(
                text = "Industry * (Select all that apply)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val industriesWithIcons = listOf(
                "Food Service" to Icons.Default.Restaurant,
                "Housekeeping" to Icons.Default.HomeWork,
                "Delivery" to Icons.Default.LocalShipping,
                "Warehouse" to Icons.Default.Warehouse,
                "Construction" to Icons.Default.Construction,
                "Healthcare" to Icons.Default.LocalHospital,
                "Retail" to Icons.Default.Store,
                "Manufacturing" to Icons.Default.PrecisionManufacturing,
                "Security" to Icons.Default.Security,
                "Hospitality" to Icons.Default.Hotel,
                "Transportation" to Icons.Default.DirectionsBus,
                "Agriculture" to Icons.Default.Agriculture,
                "IT Services" to Icons.Default.Computer,
                "Education" to Icons.Default.School,
                "Real Estate" to Icons.Default.Home,
                "Others" to Icons.Default.MoreHoriz
            )

            var selectedIndustries by remember {
                mutableStateOf(
                    industry.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                industriesWithIcons.chunked(2).forEach { rowIndustries ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowIndustries.forEach { (ind, icon) ->
                            FilterChip(
                                selected = selectedIndustries.contains(ind),
                                onClick = {
                                    selectedIndustries = if (selectedIndustries.contains(ind)) {
                                        selectedIndustries - ind
                                    } else {
                                        selectedIndustries + ind
                                    }
                                    onIndustryChange(selectedIndustries.joinToString(", "))
                                },
                                label = {
                                    Text(
                                        ind,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (selectedIndustries.contains(ind))
                                                FontWeight.SemiBold
                                            else
                                                FontWeight.Normal
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                leadingIcon = {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedIndustries.contains(ind),
                                    borderWidth = if (selectedIndustries.contains(ind)) 1.5.dp else 1.dp,
                                    borderColor = if (selectedIndustries.contains(ind))
                                        Color(0xFF8B5CF6)
                                    else
                                        Color(0xFFE5E7EB)
                                ),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    selectedContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                                    labelColor = Color(0xFF6B7280),
                                    selectedLabelColor = Color(0xFF8B5CF6),
                                    iconColor = Color(0xFF9CA3AF),
                                    selectedLeadingIconColor = Color(0xFF8B5CF6)
                                )
                            )
                        }
                        if (rowIndustries.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (selectedIndustries.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF8B5CF6).copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "${selectedIndustries.size} industr${if (selectedIndustries.size > 1) "ies" else "y"} selected",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF8B5CF6),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        Column {
            // Apr 2026: company size is now a free-form text field. The
            // dropdown was overkill for an optional field — employers
            // typed weird ranges anyway, and the input keyboard makes
            // small-team entries (e.g. "5") faster than picking from a
            // 5-bucket list.
            OutlinedTextField(
                value = companySize,
                onValueChange = { onCompanySizeChange(it) },
                label = { Text(stringResource(R.string.company_size_label)) },
                placeholder = { Text("e.g. 25") },
                leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF8B5CF6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
        }
        
        // GST Number (Optional) - For Business Verification
        Column {
            val isValidGst = gstNumber.isNotBlank() && com.example.dutype.models.isValidGstNumber(gstNumber)
            
            OutlinedTextField(
                value = gstNumber,
                onValueChange = { newValue ->
                    // Only allow alphanumeric and limit to 15 characters
                    if (newValue.length <= 15 && newValue.all { it.isLetterOrDigit() }) {
                        onGstNumberChange(newValue.uppercase())
                    }
                },
                label = { Text(stringResource(R.string.gst_number_optional)) },
                placeholder = { Text("e.g., 22AAAAA0000A1Z5") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Receipt, 
                        contentDescription = null,
                        tint = if (isValidGst) Color(0xFF10B981) else Color(0xFF6B7280)
                    ) 
                },
                trailingIcon = {
                    if (gstNumber.isNotBlank()) {
                        if (isValidGst) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Valid GST",
                                tint = Color(0xFF10B981)
                            )
                        } else {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Invalid GST format",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isError = gstNumber.isNotBlank() && !isValidGst,
                supportingText = {
                    if (gstNumber.isBlank()) {
                        Text(
                            "Add GST to get 🏢 Business badge",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    } else if (!isValidGst) {
                        Text(
                            "Invalid GST format (15 characters: 22AAAAA0000A1Z5)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFEF4444)
                            )
                        )
                    } else {
                        Text(
                            "✅ Valid GST - You'll get Business badge!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isValidGst) Color(0xFF10B981) else Color(0xFF8B5CF6),
                    unfocusedBorderColor = if (isValidGst) Color(0xFF10B981) else Color(0xFFE5E7EB)
                )
            )
        }
        
        // Referral Code Input - REMOVED: Now handled in login/signup flow
        // Referral codes must be entered DURING registration (EnhancedLoginScreen), not in profile setup
        // This follows best practices from Uber, Airbnb, PayPal - code entry happens BEFORE account creation
        /*
        if (showReferralSection && !hasAlreadyUsedReferral) {
            Spacer(modifier = Modifier.height(8.dp))
            ReferralCodeInput(
                referralCode = referralCode,
                onReferralCodeChange = onReferralCodeChange,
                isValidating = isValidatingReferral,
                validationResult = referralValidationResult,
                onValidate = onValidateReferral
            )
        }
        */
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailsStep(
    contactPhone: String,
    businessAddress: String,
    contactEmail: String,
    gender: String,
    dateOfBirth: String,
    phoneError: String?,
    emailError: String?,
    addressError: String?,
    genderError: String?,
    dateOfBirthError: String?,
    onContactPhoneChange: (String) -> Unit,
    onBusinessAddressChange: (String) -> Unit,
    onContactEmailChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    locationService: com.example.dutype.utils.LocationService
) {
    var isFetchingLocation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
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
                                Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                Color(0xFFD8B4FE).copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ContactPhone,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = "Contact Details",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "How can we reach you?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Column {
            OutlinedTextField(
                value = contactPhone,
                onValueChange = { newValue ->
                    // Only allow digits and limit to 10 characters
                    if (newValue.all { it.isDigit() } && newValue.length <= 10) {
                        onContactPhoneChange(newValue)
                    }
                },
                label = { Text(stringResource(R.string.contact_phone_required)) },
                placeholder = { Text(stringResource(R.string.enter_10_digit_phone)) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phoneError != null,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (phoneError != null) Color(0xFFDC2626) else Color(0xFF3B82F6),
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

        Column {
            OutlinedTextField(
                value = contactEmail,
                onValueChange = onContactEmailChange,
                label = { Text(stringResource(R.string.contact_email_optional)) },
                placeholder = { Text(stringResource(R.string.enter_email_address)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = emailError != null,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (emailError != null) Color(0xFFDC2626) else Color(0xFF3B82F6),
                    unfocusedBorderColor = if (emailError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                    errorBorderColor = Color(0xFFDC2626)
                )
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

        // Work Location with Fetch button
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Work Location *",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                
                Button(
                    onClick = {
                        isFetchingLocation = true
                        if (locationService.hasLocationPermission()) {
                            coroutineScope.launch {
                                // Use getHighAccuracyLocation for GPS-level precision (5-10m)
                                val locationInfo = locationService.getHighAccuracyLocation(
                                    timeoutMs = 15000L,
                                    minAccuracyMeters = 10f
                                )
                                if (locationInfo != null) {
                                    // Use detailed full address for business profile
                                    onBusinessAddressChange(locationInfo.getFullAddress())
                                }
                                isFetchingLocation = false
                            }
                        } else {
                            isFetchingLocation = false
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
            
            com.example.dutype.components.LocationAutocompleteField(
                value = businessAddress,
                onValueChange = onBusinessAddressChange,
                onLocationSelected = { selectedAddress, _, _ ->
                    onBusinessAddressChange(selectedAddress)
                },
                locationService = locationService,
                label = stringResource(R.string.business_address),
                placeholder = stringResource(R.string.search_or_enter_work_location),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (addressError != null) Color(0xFFDC2626) else Color(0xFF3B82F6),
                    unfocusedBorderColor = if (addressError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                    errorBorderColor = Color(0xFFDC2626)
                )
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
        
        // Gender Selection
        Column {
            Text(
                text = "Gender *",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val genderOptions = listOf("Male", "Female", "Other")
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                genderOptions.forEach { genderOption ->
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = (genderOption == gender),
                                onClick = { onGenderChange(genderOption) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (genderOption == gender),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF8B5CF6),
                                unselectedColor = Color(0xFFD1D5DB)
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = genderOption,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color(0xFF1F2937)
                        )
                    }
                }
            }
            
            if (genderError != null) {
                Text(
                    text = genderError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
        
        // Date of Birth
        Column {
            var showDatePicker by remember { mutableStateOf(false) }
            val datePickerState = rememberDatePickerState()
            
            Text(
                text = "Date of Birth *",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { },
                readOnly = true,
                placeholder = { Text(stringResource(R.string.select_date_of_birth)) },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                isError = dateOfBirthError != null,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (dateOfBirthError != null) Color(0xFFDC2626) else Color(0xFF3B82F6),
                    unfocusedBorderColor = if (dateOfBirthError != null) Color(0xFFDC2626) else Color(0xFFE5E7EB),
                    errorBorderColor = Color(0xFFDC2626)
                )
            )
            
            if (dateOfBirthError != null) {
                Text(
                    text = dateOfBirthError,
                    color = Color(0xFFDC2626),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    onDateOfBirthChange(dateFormat.format(Date(millis)))
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}

