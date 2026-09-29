package com.example.dutype.employer.screens

import com.dutype.app.R
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.components.ReferralValidationResult
import com.example.dutype.components.SelectableLocationMap
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

// =============================================================================
// STITCH DESIGN SPEC PALETTE — exact colors from the employer profile setup
// design (fixed, non-theme-adaptive: this screen matches the design 1:1)
// =============================================================================
private val StitchNavy = Color(0xFF0F172A)
private val StitchTrack = Color(0xFFF1F5F9)
private val StitchBorder = Color(0xFFE2E8F0)
private val StitchBlue = Color(0xFF2563EB)
private val StitchLabel = Color(0xFF64748B)
private val StitchDisabledBg = Color(0xFFF1F5F9)
private val StitchFieldValue = Color(0xFF0F172A)

private val StitchIndustryOptions = listOf(
    "Construction",
    "Retail / Shop",
    "Hospitality",
    "Manufacturing",
    "Logistics & Transport",
    "Services",
    "Other"
)

/**
 * Generic Stitch-style outlined field: white/gray rounded box with a small
 * gray label pinned at the top and a bold value line below it.
 */
@Composable
private fun StitchTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    readOnly: Boolean = false,
    backgroundColor: Color = Color.White,
    valueColor: Color = StitchFieldValue,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .border(1.dp, StitchBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 12.sp, color = StitchLabel)
            Spacer(modifier = Modifier.height(2.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = valueColor),
                keyboardOptions = keyboardOptions,
                cursorBrush = SolidColor(valueColor),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder != null) {
                        Text(text = placeholder, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = StitchLabel.copy(alpha = 0.6f))
                    }
                    inner()
                }
            )
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

/** Stitch-style picker field showing a value + chevron, opening a dropdown menu. */
@Composable
private fun StitchDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(1.dp, StitchBorder, RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 12.sp, color = StitchLabel)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value.ifBlank { "Select industry" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value.isBlank()) StitchLabel.copy(alpha = 0.6f) else StitchBlue
                )
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = StitchLabel)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Pill-shaped two-option segmented toggle matching the Stitch design spec. */
@Composable
private fun StitchSegmentedToggle(
    isIndividual: Boolean,
    onIndividualSelected: () -> Unit,
    onCompanySelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(StitchTrack, RoundedCornerShape(24.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isIndividual) StitchNavy else Color.Transparent)
                .clickable(onClick = onIndividualSelected),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Individual",
                color = if (isIndividual) Color.White else StitchLabel,
                fontSize = 14.sp,
                fontWeight = if (isIndividual) FontWeight.Bold else FontWeight.Medium
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(if (!isIndividual) StitchNavy else Color.Transparent)
                .clickable(onClick = onCompanySelected),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Company / Business",
                color = if (!isIndividual) Color.White else StitchLabel,
                fontSize = 14.sp,
                fontWeight = if (!isIndividual) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

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
    var employerType by rememberSaveable { mutableStateOf("INDIVIDUAL") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var businessAddress by rememberSaveable { mutableStateOf("") }
    var businessLatitude by rememberSaveable { mutableStateOf(0.0) }
    var businessLongitude by rememberSaveable { mutableStateOf(0.0) }
    var industry by rememberSaveable { mutableStateOf("") }
    var gstin by rememberSaveable { mutableStateOf("") }
    var contactName by rememberSaveable { mutableStateOf("") }
    var isVerifyingGstin by remember { mutableStateOf(false) }
    var gstinVerifiedMessage by remember { mutableStateOf<String?>(null) }
    
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
                    val savedEmployerType = existingData["employerType"] as? String
                    val savedGstin = existingData["gstin"] as? String
                    val savedFullName = existingData["fullName"] as? String
                    
                    if (!savedEmployerType.isNullOrBlank()) {
                        employerType = savedEmployerType
                        Timber.d("📦 PREFILL: employerType = $employerType")
                    } else if (!savedCompanyName.isNullOrBlank() && savedCompanyName != currentUser.displayName) {
                        employerType = "COMPANY"
                    }
                    
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
                    if (gstin.isBlank() && !savedGstin.isNullOrBlank()) {
                        gstin = savedGstin
                        Timber.d("📦 PREFILL: gstin loaded")
                    }
                    if (contactName.isBlank() && !savedFullName.isNullOrBlank() && savedFullName != savedCompanyName) {
                        contactName = savedFullName
                        Timber.d("📦 PREFILL: contactName = $contactName")
                    }
                    if (businessAddress.isBlank() && !savedBusinessAddress.isNullOrBlank()) {
                        businessAddress = savedBusinessAddress
                        Timber.d("PREFILL: businessAddress loaded")
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

    // Update errors only when showValidationErrors is true
    val isIndividual = employerType == "INDIVIDUAL"
    val phoneNumberRequiredError = stringResource(R.string.phone_number_required_error)
    val validPhoneError = stringResource(R.string.valid_10_digit_phone_error)
    val companyNameRequiredError = if (isIndividual) "Your full name is required" else stringResource(R.string.company_name_required_error)
    val workLocationRequiredError = if (isIndividual) "Home or task location is required" else stringResource(R.string.work_location_required_error)

    LaunchedEffect(contactPhone, companyName, industry, businessAddress, employerType, showValidationErrors) {
        if (showValidationErrors) {
            phoneError = when {
                contactPhone.isBlank() -> phoneNumberRequiredError
                !ValidationUtils.isValidIndianPhoneNumber(contactPhone) -> validPhoneError
                else -> null
            }
            companyNameError = if (companyName.isBlank()) companyNameRequiredError else null
            industryError = null
            addressError = if (businessAddress.isBlank()) workLocationRequiredError else null
        } else {
            phoneError = null
            companyNameError = null
            industryError = null
            addressError = null
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
                        "fullName" to contactName.ifBlank { companyName },
                        "phone" to contactPhone,
                        "employerType" to employerType
                    )

                    if (industry.isNotBlank()) {
                        employerProfileData["industry"] = industry.trim()
                    }
                    if (gstin.isNotBlank()) {
                        employerProfileData["gstin"] = gstin.trim().uppercase()
                    }
                    if (businessAddress.isNotBlank()) {
                        employerProfileData["businessAddress"] = businessAddress.trim()
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
                        ?.takeIf { it.isNotBlank() }
                        ?: (employerProfileData["referredByCode"] as? String)?.takeIf { it.isNotBlank() }
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
        employerType = employerType,
        companyName = companyName,
        contactPhone = contactPhone,
        businessAddress = businessAddress,
        businessLatitude = businessLatitude,
        businessLongitude = businessLongitude,
        industry = industry,
        gstin = gstin,
        contactName = contactName,
        isVerifyingGstin = isVerifyingGstin,
        gstinVerifiedMessage = gstinVerifiedMessage,
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
        referralCode = referralCode,
        isValidatingReferral = isValidatingReferral,
        referralValidationResult = referralValidationResult,
        showReferralSection = showReferralSection,
        hasAlreadyUsedReferral = hasAlreadyUsedReferral,
        locationService = locationService,
        onEmployerTypeChange = { employerType = it },
        onCompanyNameChange = { companyName = it },
        onContactPhoneChange = { contactPhone = it },
        onBusinessAddressChange = { businessAddress = it },
        onBusinessLocationChange = { lat, lng ->
            businessLatitude = lat
            businessLongitude = lng
        },
        onIndustryChange = { industry = it },
        onGstinChange = { gstin = it.take(15).uppercase() },
        onContactNameChange = { contactName = it },
        onVerifyGstin = {
            val gstinPattern = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$")
            scope.launch {
                isVerifyingGstin = true
                gstinVerifiedMessage = null
                kotlinx.coroutines.delay(600)
                gstinVerifiedMessage = if (gstinPattern.matches(gstin.trim().uppercase())) {
                    "GSTIN format looks valid"
                } else {
                    "Please check the GSTIN format"
                }
                isVerifyingGstin = false
            }
        },
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
    employerType: String,
    companyName: String,
    contactPhone: String,
    businessAddress: String,
    businessLatitude: Double,
    businessLongitude: Double,
    industry: String,
    gstin: String,
    contactName: String,
    isVerifyingGstin: Boolean,
    gstinVerifiedMessage: String?,
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
    referralCode: String,
    isValidatingReferral: Boolean,
    referralValidationResult: ReferralValidationResult?,
    showReferralSection: Boolean,
    hasAlreadyUsedReferral: Boolean,
    locationService: com.example.dutype.utils.LocationService,
    onEmployerTypeChange: (String) -> Unit,
    onCompanyNameChange: (String) -> Unit,
    onContactPhoneChange: (String) -> Unit,
    onBusinessAddressChange: (String) -> Unit,
    onBusinessLocationChange: (Double, Double) -> Unit,
    onIndustryChange: (String) -> Unit,
    onGstinChange: (String) -> Unit,
    onContactNameChange: (String) -> Unit,
    onVerifyGstin: () -> Unit,
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
                            employerType = employerType,
                            companyName = companyName,
                            industry = industry,
                            gstin = gstin,
                            contactName = contactName,
                            isVerifyingGstin = isVerifyingGstin,
                            gstinVerifiedMessage = gstinVerifiedMessage,
                            companyNameError = companyNameError,
                            industryError = industryError,
                            referralCode = referralCode,
                            isValidatingReferral = isValidatingReferral,
                            referralValidationResult = referralValidationResult,
                            showReferralSection = showReferralSection,
                            hasAlreadyUsedReferral = hasAlreadyUsedReferral,
                            onEmployerTypeChange = onEmployerTypeChange,
                            onCompanyNameChange = onCompanyNameChange,
                            onIndustryChange = onIndustryChange,
                            onGstinChange = onGstinChange,
                            onContactNameChange = onContactNameChange,
                            onVerifyGstin = onVerifyGstin,
                            onReferralCodeChange = onReferralCodeChange,
                            onValidateReferral = onValidateReferral
                        )

                        ContactDetailsStep(
                            isIndividual = employerType == "INDIVIDUAL",
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
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = onPreviousClick,
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = StitchNavy
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StitchBorder),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps) {
                                onNextClick()
                            } else {
                                onCompleteClick()
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchNavy,
                            disabledContainerColor = StitchNavy.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (currentStep == totalSteps) "Start Hiring" else "Next",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
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
    employerType: String,
    companyName: String,
    industry: String,
    gstin: String,
    contactName: String,
    isVerifyingGstin: Boolean,
    gstinVerifiedMessage: String?,
    companyNameError: String?,
    industryError: String?,
    referralCode: String,
    isValidatingReferral: Boolean,
    referralValidationResult: ReferralValidationResult?,
    showReferralSection: Boolean,
    hasAlreadyUsedReferral: Boolean,
    onEmployerTypeChange: (String) -> Unit,
    onCompanyNameChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onGstinChange: (String) -> Unit,
    onContactNameChange: (String) -> Unit,
    onVerifyGstin: () -> Unit,
    onReferralCodeChange: (String) -> Unit,
    onValidateReferral: (String) -> Unit
) {
    val isIndividual = employerType == "INDIVIDUAL"

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Segmented toggle: Individual | Company / Business
        StitchSegmentedToggle(
            isIndividual = isIndividual,
            onIndividualSelected = { onEmployerTypeChange("INDIVIDUAL") },
            onCompanySelected = { onEmployerTypeChange("COMPANY") }
        )

        // Business / Shop Name — only relevant for Company / Business accounts
        if (!isIndividual) {
            Column {
                StitchTextField(
                    label = "Business / Shop Name",
                    value = companyName,
                    onValueChange = onCompanyNameChange,
                    placeholder = "e.g. Ravi Constructions"
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
        }

        // Industry — dropdown picker
        StitchDropdownField(
            label = if (isIndividual) "Help Needed (Optional)" else "Industry",
            value = industry,
            options = StitchIndustryOptions,
            onValueChange = { onIndustryChange(it) }
        )

        // GSTIN — optional, with inline "Verify →" action (Company only)
        if (!isIndividual) {
            Column {
                StitchTextField(
                    label = "GSTIN (optional)",
                    value = gstin,
                    onValueChange = { onGstinChange(it) },
                    placeholder = "e.g. 36AABCR1234M1Z5",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = gstin.isNotBlank() && !isVerifyingGstin) { onVerifyGstin() }
                        ) {
                            if (isVerifyingGstin) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = StitchBlue, strokeWidth = 2.dp)
                            } else {
                                Text(text = "Verify", color = StitchBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Verify GSTIN", tint = StitchBlue, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )
                if (gstinVerifiedMessage != null) {
                    Text(
                        text = gstinVerifiedMessage,
                        color = StitchLabel,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }

        // Your Full Name
        Column {
            StitchTextField(
                label = "Your Full Name",
                value = if (isIndividual) companyName else contactName,
                onValueChange = if (isIndividual) onCompanyNameChange else onContactNameChange,
                placeholder = "e.g. Ravi Teja"
            )
            if (isIndividual && companyNameError != null) {
                Text(
                    text = companyNameError,
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

/** Result of a current-location fetch; shown under the address field. */
private class LocationFetchOutcome(val areaText: String?, val errorText: String?)

/** Fetches the current location, reverse-geocodes it and pushes it into the form. */
private suspend fun fetchCurrentLocationInto(
    locationService: com.example.dutype.utils.LocationService,
    onAddress: (String) -> Unit,
    onLocation: (Double, Double) -> Unit,
    setFetching: (Boolean) -> Unit
): LocationFetchOutcome {
    setFetching(true)
    return try {
        val locationInfo = locationService.getHighAccuracyLocation(
            timeoutMs = 8000L,
            minAccuracyMeters = 35f
        ) ?: locationService.getCurrentLocation()
        if (locationInfo != null) {
            onAddress(locationInfo.getFullAddress())
            onLocation(locationInfo.latitude, locationInfo.longitude)
            LocationFetchOutcome(locationInfo.getShortAddress(), null)
        } else {
            LocationFetchOutcome(null, "Could not get current location. Please enter manually or try again.")
        }
    } catch (e: Exception) {
        Timber.e(e, "Error fetching location")
        LocationFetchOutcome(null, "Error fetching location: ${e.message}")
    } finally {
        setFetching(false)
    }
}

/** Small status line under the address field: detected area (green) or error (red). */
@Composable
private fun LocationFetchStatus(areaText: String?, errorText: String?) {
    if (errorText != null) {
        Text(
            text = errorText,
            color = Color(0xFFDC2626),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
    } else if (areaText != null) {
        Row(
            modifier = Modifier.padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Location detected",
                color = Color(0xFF16A34A),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (areaText.isNotBlank()) {
                Text(
                    text = " \u00B7 $areaText",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Asks for location permission directly (once) when the location step becomes visible. */
@Composable
private fun AutoRequestLocationOnEntry(
    alreadyAsked: Boolean,
    hasPermission: () -> Boolean,
    addressIsBlank: Boolean,
    onAsked: () -> Unit,
    onGrantedFetch: () -> Unit,
    onRequestPermission: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (alreadyAsked) return@LaunchedEffect
        onAsked()
        if (hasPermission()) {
            if (addressIsBlank) onGrantedFetch()
        } else {
            onRequestPermission()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailsStep(
    isIndividual: Boolean,
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
    val context = LocalContext.current

    var manualLocationRequest by remember { mutableStateOf(false) }
    var autoLocationAsked by rememberSaveable { mutableStateOf(false) }
    var detectedAreaText by remember { mutableStateOf<String?>(null) }
    var locationErrorText by remember { mutableStateOf<String?>(null) }

    val runFetch: () -> Unit = {
        coroutineScope.launch {
            locationErrorText = null
            val outcome = fetchCurrentLocationInto(
                locationService,
                onBusinessAddressChange, onBusinessLocationChange
            ) { isFetchingLocation = it }
            detectedAreaText = outcome.areaText
            locationErrorText = outcome.errorText
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            if (manualLocationRequest || businessAddress.isBlank()) {
                runFetch()
            }
        } else if (manualLocationRequest) {
            locationErrorText = context.getString(R.string.location_permission_fetch_address)
        }
        manualLocationRequest = false
    }

    AutoRequestLocationOnEntry(
        alreadyAsked = autoLocationAsked,
        hasPermission = { locationService.hasLocationPermission() },
        addressIsBlank = businessAddress.isBlank(),
        onAsked = { autoLocationAsked = true },
        onGrantedFetch = { runFetch() },
        onRequestPermission = {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Phone — verified via OTP during login, shown locked/read-only
        StitchTextField(
            label = "Phone",
            value = if (contactPhone.isBlank()) "" else "+91 $contactPhone",
            onValueChange = {},
            readOnly = true,
            backgroundColor = StitchDisabledBg,
            valueColor = StitchLabel,
            placeholder = stringResource(R.string.enter_10_digit_phone),
            trailingContent = {
                Icon(Icons.Default.Lock, contentDescription = "Verified phone number", tint = StitchLabel, modifier = Modifier.size(18.dp))
            }
        )
        if (phoneError != null) {
            Text(
                text = phoneError,
                color = EmployerColors.Error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }

        // City / Area — real autocomplete search preserved, restyled to match design
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        if (locationService.hasLocationPermission()) {
                            runFetch()
                        } else {
                            manualLocationRequest = true
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = StitchBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Use current location",
                        style = MaterialTheme.typography.labelSmall,
                        color = StitchBlue
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
                label = "City / Area",
                placeholder = if (isIndividual) "Search or enter your area / landmark" else stringResource(R.string.search_or_enter_work_location),
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = Icons.Default.LocationOn,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = if (addressError != null) EmployerColors.Error else StitchBlue,
                    unfocusedBorderColor = if (addressError != null) EmployerColors.Error else StitchBorder,
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
            if (isFetchingLocation) {
                Text(
                    text = "Fetching location...",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else {
                LocationFetchStatus(areaText = detectedAreaText, errorText = locationErrorText)
            }
        }

        // Map preview — reuses the real SelectableLocationMap (Google Maps) composable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StitchTrack)
                .border(1.dp, StitchBorder, RoundedCornerShape(16.dp))
        ) {
            if (com.example.dutype.utils.GeoUtils.hasValidCoordinates(businessLatitude, businessLongitude)) {
                SelectableLocationMap(
                    latitude = businessLatitude,
                    longitude = businessLongitude,
                    modifier = Modifier.fillMaxSize(),
                    markerTitle = if (isIndividual) "Your location" else "Business location",
                    onLocationPicked = { lat, lng -> onBusinessLocationChange(lat, lng) }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = StitchLabel, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pick a location above to preview the map",
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchLabel,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

