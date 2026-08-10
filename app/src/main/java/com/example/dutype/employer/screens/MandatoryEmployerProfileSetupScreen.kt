package com.example.dutype.employer.screens

import com.dutype.app.R
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
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.ui.theme.LocalRoleColors
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.dutype.components.markWelcomeCelebrationPending
import timber.log.Timber

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
    val crashlytics = remember { FirebaseCrashlytics.getInstance() }
    // Services accessed via ProfileCompletionViewModel (proper DI pattern)
    val locationService = profileCompletionViewModel.locationService
    val notificationService = profileCompletionViewModel.notificationService
    val fcmTokenManager = profileCompletionViewModel.fcmTokenManager

    // Form state - using rememberSaveable to survive activity recreation (camera launch)
    var companyName by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var businessAddress by rememberSaveable { mutableStateOf("") }
    var businessLatitude by rememberSaveable { mutableStateOf(0.0) }
    var businessLongitude by rememberSaveable { mutableStateOf(0.0) }
    var industry by rememberSaveable { mutableStateOf("") }
    var gstin by rememberSaveable { mutableStateOf("") }
    
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
    val totalSteps = 1

    fun logFunnelEvent(event: String, extras: Map<String, String> = emptyMap()) {
        runCatching {
            crashlytics.log("profile_funnel_employer:$event")
            crashlytics.setCustomKey("profile_funnel_role", "EMPLOYER")
            crashlytics.setCustomKey("profile_funnel_event", event)
            crashlytics.setCustomKey("profile_funnel_step", currentStep)
            extras.forEach { (k, v) -> crashlytics.setCustomKey("profile_funnel_$k", v) }
        }.onFailure { Timber.w(it, "Failed to log employer funnel telemetry") }
    }

    // INDUSTRY BEST PRACTICE: Load existing profile data from Firebase (Single Source of Truth)
    // This handles both new users and existing users with partial data
    // Pattern used by: Google, Uber, Airbnb, LinkedIn
    // Full profile data is loaded for form prefilling - all fields are needed
    LaunchedEffect(Unit) {
        logFunnelEvent(
            event = "started",
            extras = mapOf("return_route" to (returnRoute ?: "none"))
        )
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
                    val savedContactPhone = existingData["phone"] as? String
                    val savedProfileImageUrl = existingData["profileImageUrl"] as? String
                    // employer_profiles fields
                    val savedIndustry = existingData["industry"] as? String
                    val savedBusinessAddress = existingData["businessAddress"] as? String
                    val savedBusinessLocation = existingData["businessLocation"] as? Map<*, *>
                    
                    // Apply prefilled values (only if current field is empty)
                    if (companyName.isBlank() && !savedCompanyName.isNullOrBlank()) {
                        companyName = savedCompanyName
                        Timber.d("📦 PREFILL: companyName = $companyName")
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
                    if (businessAddress.isBlank() && !savedBusinessAddress.isNullOrBlank()) {
                        businessAddress = savedBusinessAddress
                        Timber.d("PREFILL: businessAddress loaded")
                    }
                    val savedGstin = existingData["gstin"] as? String
                    if (gstin.isBlank() && !savedGstin.isNullOrBlank()) {
                        gstin = savedGstin
                        Timber.d("PREFILL: gstin = $gstin")
                    }
                    if (businessLatitude == 0.0 && businessLongitude == 0.0 && savedBusinessLocation != null) {
                        val savedLat = (savedBusinessLocation["lat"] as? Number)?.toDouble()
                        val savedLng = (savedBusinessLocation["lng"] as? Number)?.toDouble()
                        if (savedLat != null && savedLng != null &&
                            com.example.dutype.utils.GeoUtils.hasValidCoordinates(savedLat, savedLng)
                        ) {
                            businessLatitude = savedLat
                            businessLongitude = savedLng
                            Timber.d("PREFILL: businessLocation loaded")
                        }
                    }
                    if (!savedProfileImageUrl.isNullOrBlank()) {
                        selfieUrl = savedProfileImageUrl
                        Timber.d("📦 PREFILL: profileImageUrl exists")
                    }
                }
                
                // Fallback: Load display name from Google Sign-In if still empty
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

    LaunchedEffect(currentStep) {
        logFunnelEvent("step_viewed", mapOf("step" to currentStep.toString()))
    }

    // Validation logic
    val isStep1Valid = companyName.isNotBlank()
    val isStep2Valid = ValidationUtils.isValidIndianPhoneNumber(contactPhone) && businessAddress.isNotBlank()
    val isStep3Valid = true  // Selfie is optional - always valid

    var phoneError by remember { mutableStateOf<String?>(null) }
    var companyNameError by remember { mutableStateOf<String?>(null) }
    var industryError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var gstinError by remember { mutableStateOf<String?>(null) }

    // Update errors only when showValidationErrors is true
    val phoneNumberRequiredError = stringResource(R.string.phone_number_required_error)
    val validPhoneError = stringResource(R.string.valid_10_digit_phone_error)
    val companyNameRequiredError = stringResource(R.string.company_name_required_error)
    val workLocationRequiredError = stringResource(R.string.work_location_required_error)

    LaunchedEffect(contactPhone, companyName, industry, businessAddress, showValidationErrors) {
        if (showValidationErrors) {
            phoneError = when {
                contactPhone.isBlank() -> phoneNumberRequiredError
                !ValidationUtils.isValidIndianPhoneNumber(contactPhone) -> validPhoneError
                else -> null
            }
            companyNameError = if (companyName.isBlank()) companyNameRequiredError else null
            industryError = null
            addressError = if (businessAddress.isBlank()) workLocationRequiredError else null
            gstinError = if (gstin.isNotBlank() && gstin.length != 15) "GSTIN must be 15 characters" else null
        } else {
            phoneError = null
            companyNameError = null
            industryError = null
            addressError = null
            gstinError = null
        }
    }

    val isCurrentStepValid = isStep1Valid && isStep2Valid && isStep3Valid

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
                    
                    val employerProfileData = mutableMapOf<String, Any>(
                        "companyName" to companyName,
                        "phone" to contactPhone
                    )

                    if (industry.isNotBlank()) {
                        employerProfileData["industry"] = industry.trim()
                    }
                    if (businessAddress.isNotBlank()) {
                        employerProfileData["businessAddress"] = businessAddress.trim()
                    }
                    if (gstin.isNotBlank()) {
                        employerProfileData["gstin"] = gstin.trim()
                    }
                    if (com.example.dutype.utils.GeoUtils.hasValidCoordinates(businessLatitude, businessLongitude)) {
                        employerProfileData["businessLocation"] = mapOf(
                            "lat" to businessLatitude,
                            "lng" to businessLongitude
                        )
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
                                        context.getString(R.string.referral_code_applied_success),
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
                    if (!wasAlreadyComplete) {
                        // Flag for welcome celebration overlay on employer home screen
                        markWelcomeCelebrationPending(context)
                    }
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
                    logFunnelEvent("completed", mapOf("destination" to returnRoute))
                } else {
                    Timber.d("📍 Profile complete - navigating to EMPLOYER_HOME")
                    navController.navigate(Routes.EMPLOYER_HOME) {
                        popUpTo(Routes.EMPLOYER_PROFILE_SETUP) { inclusive = true }
                        launchSingleTop = true
                    }
                    logFunnelEvent("completed", mapOf("destination" to Routes.EMPLOYER_HOME))
                }
            } catch (e: Exception) {
                logFunnelEvent("completion_failed", mapOf("reason" to "exception"))
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
                .background(LocalRoleColors.current.screenBackground),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = EmployerColors.Primary
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
        contactPhone = contactPhone,
        businessAddress = businessAddress,
        businessLatitude = businessLatitude,
        businessLongitude = businessLongitude,
        industry = industry,
        gstin = gstin,
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
        companyNameError = if (showValidationErrors) companyNameError else null,
        industryError = if (showValidationErrors) industryError else null,
        addressError = if (showValidationErrors) addressError else null,
        gstinError = if (showValidationErrors) gstinError else null,
        referralCode = referralCode,
        isValidatingReferral = isValidatingReferral,
        referralValidationResult = referralValidationResult,
        showReferralSection = showReferralSection,
        hasAlreadyUsedReferral = hasAlreadyUsedReferral,
        locationService = locationService,
        onCompanyNameChange = { companyName = it },
        onContactPhoneChange = { contactPhone = it },
        onBusinessAddressChange = { businessAddress = it },
        onBusinessLocationChange = { lat, lng ->
            businessLatitude = lat
            businessLongitude = lng
        },
        onIndustryChange = { industry = it },
        onGstinChange = { gstin = it },
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
        onPreviousClick = {
            logFunnelEvent("step_back", mapOf("from_step" to currentStep.toString()))
            currentStep--
        },
        onNextClick = {
            showValidationErrors = true
            if (isCurrentStepValid) {
                val previousStep = currentStep
                currentStep++
                logFunnelEvent(
                    "step_advanced",
                    mapOf(
                        "from_step" to previousStep.toString(),
                        "to_step" to currentStep.toString()
                    )
                )
                showValidationErrors = false
            } else {
                logFunnelEvent("step_blocked", mapOf("step" to currentStep.toString()))
            }
        },
        onCompleteClick = {
            showValidationErrors = true
            if (isCurrentStepValid) {
                handleCompletion()
            } else {
                logFunnelEvent("step_blocked", mapOf("step" to currentStep.toString()))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandatoryEmployerProfileSetupContent(
    companyName: String,
    contactPhone: String,
    businessAddress: String,
    businessLatitude: Double,
    businessLongitude: Double,
    industry: String,
    gstin: String,
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
    companyNameError: String?,
    industryError: String?,
    addressError: String?,
    gstinError: String?,
    referralCode: String,
    isValidatingReferral: Boolean,
    referralValidationResult: ReferralValidationResult?,
    showReferralSection: Boolean,
    hasAlreadyUsedReferral: Boolean,
    locationService: com.example.dutype.utils.LocationService,
    onCompanyNameChange: (String) -> Unit,
    onContactPhoneChange: (String) -> Unit,
    onBusinessAddressChange: (String) -> Unit,
    onBusinessLocationChange: (Double, Double) -> Unit,
    onIndustryChange: (String) -> Unit,
    onGstinChange: (String) -> Unit,
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
            .background(LocalRoleColors.current.screenBackground)
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
                            ambientColor = EmployerColors.TextPrimary.copy(alpha = 0.12f),
                            spotColor = EmployerColors.TextPrimary.copy(alpha = 0.08f)
                        ),
                    colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        CompanyInformationStep(
                            companyName = companyName,
                            industry = industry,
                            gstin = gstin,
                            companyNameError = companyNameError,
                            industryError = industryError,
                            gstinError = gstinError,
                            referralCode = referralCode,
                            isValidatingReferral = isValidatingReferral,
                            referralValidationResult = referralValidationResult,
                            showReferralSection = showReferralSection,
                            hasAlreadyUsedReferral = hasAlreadyUsedReferral,
                            onCompanyNameChange = onCompanyNameChange,
                            onIndustryChange = onIndustryChange,
                            onGstinChange = onGstinChange,
                            onReferralCodeChange = onReferralCodeChange,
                            onValidateReferral = onValidateReferral
                        )

                        ContactDetailsStep(
                            contactPhone = contactPhone,
                            businessAddress = businessAddress,
                            businessLatitude = businessLatitude,
                            businessLongitude = businessLongitude,
                            phoneError = phoneError,
                            addressError = addressError,
                            onContactPhoneChange = onContactPhoneChange,
                            onBusinessAddressChange = onBusinessAddressChange,
                            onBusinessLocationChange = onBusinessLocationChange,
                            locationService = locationService
                        )

                        if (errorMessage != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = EmployerColors.ErrorLight),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = EmployerColors.Error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = errorMessage,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = EmployerColors.Error
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
                color = EmployerColors.CardBackground,
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
                                contentColor = EmployerColors.Primary
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
                        colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = EmployerColors.CardBackground, strokeWidth = 2.dp)
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
    gstin: String,
    companyNameError: String?,
    industryError: String?,
    gstinError: String?,
    referralCode: String,
    isValidatingReferral: Boolean,
    referralValidationResult: ReferralValidationResult?,
    showReferralSection: Boolean,
    hasAlreadyUsedReferral: Boolean,
    onCompanyNameChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onGstinChange: (String) -> Unit,
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
                                EmployerColors.Primary.copy(alpha = 0.15f),
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
                    tint = EmployerColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = stringResource(R.string.auto_company_information),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = com.example.dutype.ui.theme.EmployerColors.TextPrimary,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = stringResource(R.string.auto_tell_us_about_your_company),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EmployerColors.TextSecondary,
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
                    focusedBorderColor = if (companyNameError != null) EmployerColors.Error else EmployerColors.Primary,
                    unfocusedBorderColor = if (companyNameError != null) EmployerColors.Error else EmployerColors.Border,
                    errorBorderColor = EmployerColors.Error
                )
            )
            if (companyNameError != null) {
                Text(
                    text = companyNameError,
                    color = EmployerColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        Column {
            OutlinedTextField(
                value = industry,
                onValueChange = { onIndustryChange(it.take(120)) },
                label = { Text(stringResource(R.string.hiring_categories_optional)) },
                placeholder = { Text(stringResource(R.string.hiring_categories_hint)) },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmployerColors.Primary,
                    unfocusedBorderColor = EmployerColors.Border,
                    cursorColor = EmployerColors.Primary
                )
            )
        }

        Column {
            OutlinedTextField(
                value = gstin,
                onValueChange = { onGstinChange(it.uppercase().take(15)) },
                label = { Text("GSTIN (Optional)") },
                placeholder = { Text("Enter 15-character GSTIN") },
                leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                isError = gstinError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (gstinError != null) EmployerColors.Error else EmployerColors.Primary,
                    unfocusedBorderColor = if (gstinError != null) EmployerColors.Error else EmployerColors.Border,
                    cursorColor = EmployerColors.Primary,
                    errorBorderColor = EmployerColors.Error
                )
            )
            if (gstinError != null) {
                Text(
                    text = gstinError,
                    color = EmployerColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
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
    businessLatitude: Double,
    businessLongitude: Double,
    phoneError: String?,
    addressError: String?,
    onContactPhoneChange: (String) -> Unit,
    onBusinessAddressChange: (String) -> Unit,
    onBusinessLocationChange: (Double, Double) -> Unit,
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
                                EmployerColors.Primary.copy(alpha = 0.15f),
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
                    tint = EmployerColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = stringResource(R.string.auto_contact_details),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = com.example.dutype.ui.theme.EmployerColors.TextPrimary,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = stringResource(R.string.auto_how_can_we_reach_you),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EmployerColors.TextSecondary,
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
                    focusedBorderColor = if (phoneError != null) EmployerColors.Error else EmployerColors.Primary,
                    unfocusedBorderColor = if (phoneError != null) EmployerColors.Error else EmployerColors.Border,
                    errorBorderColor = EmployerColors.Error
                )
            )
            if (phoneError != null) {
                Text(
                    text = phoneError,
                    color = EmployerColors.Error,
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
                    text = stringResource(R.string.work_location_required_label),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
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
                                    onBusinessLocationChange(locationInfo.latitude, locationInfo.longitude)
                                }
                                isFetchingLocation = false
                            }
                        } else {
                            isFetchingLocation = false
                        }
                    },
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmployerColors.Primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmployerColors.CardBackground
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFetchingLocation) "Fetching..." else "Fetch",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmployerColors.CardBackground
                    )
                }
            }
            
            com.example.dutype.components.LocationAutocompleteField(
                value = businessAddress,
                onValueChange = onBusinessAddressChange,
                onLocationSelected = { selectedAddress, latitude, longitude ->
                    onBusinessAddressChange(selectedAddress)
                    onBusinessLocationChange(latitude, longitude)
                },
                locationService = locationService,
                label = stringResource(R.string.business_address),
                placeholder = stringResource(R.string.search_or_enter_work_location),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (addressError != null) EmployerColors.Error else EmployerColors.Primary,
                    unfocusedBorderColor = if (addressError != null) EmployerColors.Error else EmployerColors.Border,
                    errorBorderColor = EmployerColors.Error
                )
            )
            if (addressError != null) {
                Text(
                    text = addressError,
                    color = EmployerColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }
    }
}

