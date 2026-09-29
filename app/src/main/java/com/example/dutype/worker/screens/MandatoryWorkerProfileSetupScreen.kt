package com.example.dutype.worker.screens

import com.dutype.app.R
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CleaningServices as OutlinedCleaningServices
import androidx.compose.material.icons.outlined.ChildCare as OutlinedChildCare
import androidx.compose.material.icons.outlined.DeliveryDining as OutlinedDeliveryDining
import androidx.compose.material.icons.outlined.DirectionsCar as OutlinedDirectionsCar
import androidx.compose.material.icons.outlined.ElectricalServices as OutlinedElectricalServices
import androidx.compose.material.icons.outlined.Favorite as OutlinedFavorite
import androidx.compose.material.icons.outlined.Female as OutlinedFemale
import androidx.compose.material.icons.outlined.FormatPaint as OutlinedFormatPaint
import androidx.compose.material.icons.outlined.Handyman as OutlinedHandyman
import androidx.compose.material.icons.outlined.Checkroom as OutlinedCheckroom
import androidx.compose.material.icons.outlined.Build as OutlinedBuild
import androidx.compose.material.icons.outlined.CarRepair as OutlinedCarRepair
import androidx.compose.material.icons.outlined.Carpenter as OutlinedCarpenter
import androidx.compose.material.icons.outlined.Construction as OutlinedConstruction
import androidx.compose.material.icons.outlined.LocalShipping as OutlinedLocalShipping
import androidx.compose.material.icons.outlined.Male as OutlinedMale
import androidx.compose.material.icons.outlined.MoreHoriz as OutlinedMoreHoriz
import androidx.compose.material.icons.outlined.Person as OutlinedPerson
import androidx.compose.material.icons.outlined.Plumbing as OutlinedPlumbing
import androidx.compose.material.icons.outlined.Restaurant as OutlinedRestaurant
import androidx.compose.material.icons.outlined.Security as OutlinedSecurity
import androidx.compose.material.icons.outlined.Work as OutlinedWork
import androidx.compose.material.icons.outlined.Yard as OutlinedYard
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.components.ReferralValidationResult
import com.example.dutype.components.isValidReferralCode
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.dutype.components.markWelcomeCelebrationPending
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val MIN_WORKER_BIO_LENGTH = 20
private const val MAX_WORKER_BIO_LENGTH = 300
private val DOB_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// ============================================================================
// STITCH DESIGN SPEC — exact palette/typography for the 3-step wizard.
// Sourced from the provided "Basic Information / What's your trade? / Where
// are you based?" screenshots. Kept local to this file so the visual redesign
// doesn't leak into shared WorkerColors used elsewhere in the app.
// ============================================================================
private val StitchInk = Color(0xFF0F0F0F)          // selected pill / CTA / completed progress segment
private val StitchTitle = Color(0xFF0F172A)        // screen title text
private val StitchSubtitle = Color(0xFF64748B)     // subtitle under title
private val StitchLabel = Color(0xFF94A3B8)        // uppercase section labels (GENDER, EXPERIENCE, ...)
private val StitchBorder = Color(0xFFE2E8F0)       // unselected pill / field border
private val StitchTrack = Color(0xFFDBEAFE)        // upcoming/current progress segment track
private val StitchAccent = Color(0xFF10B981)       // "Detect" location accent (teal/green)
private val StitchWhite = Color(0xFFFFFFFF)
private val StitchSuccess = Color(0xFF16A34A)      // "Location detected" confirmation
private val StitchFieldLabel = Color(0xFF64748B)   // small label above filled text field value

/**
 * 3-segment thin rounded progress bar used at the top of every onboarding step.
 * Completed steps render solid ink-black, the current/upcoming steps render a
 * light blue-gray track — matches the Stitch design spec exactly.
 */
@Composable
private fun StitchStepProgressBar(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (segment in 1..totalSteps) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (segment <= currentStep) StitchInk else StitchTrack)
            )
        }
    }
}

/**
 * Selectable pill chip used for Gender / Experience / Trade / About-you rows.
 * Selected = solid black background + white text. Unselected = white
 * background + light gray border + dark text. Matches the Stitch design spec.
 */
@Composable
private fun StitchPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingCheck: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) StitchInk else StitchWhite)
            .border(
                width = 1.dp,
                color = if (selected) StitchInk else StitchBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (selected) StitchWhite else StitchTitle,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (selected && leadingCheck) {
                Text(
                    text = "✓ ",
                    color = StitchWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = text,
                color = if (selected) StitchWhite else StitchTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/** Picks a friendly icon for a skill name (lowercase contains-matching). */
private fun skillIcon(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        n.contains("electric") -> Icons.Outlined.OutlinedElectricalServices
        n.contains("plumb") -> Icons.Outlined.OutlinedPlumbing
        n.contains("carpent") -> Icons.Outlined.OutlinedCarpenter
        n.contains("paint") -> Icons.Outlined.OutlinedFormatPaint
        n.contains("deliver") -> Icons.Outlined.OutlinedDeliveryDining
        n.contains("driv") || n.contains("driver") -> Icons.Outlined.OutlinedDirectionsCar
        n.contains("warehouse") || n.contains("shipping") -> Icons.Outlined.OutlinedLocalShipping
        n.contains("cook") || n.contains("food") || n.contains("kitchen") || n.contains("chef") -> Icons.Outlined.OutlinedRestaurant
        n.contains("clean") || n.contains("maid") || n.contains("housekeep") -> Icons.Outlined.OutlinedCleaningServices
        n.contains("garden") -> Icons.Outlined.OutlinedYard
        n.contains("tailor") || n.contains("stitch") -> Icons.Outlined.OutlinedCheckroom
        n.contains("mechanic") -> Icons.Outlined.OutlinedCarRepair
        n.contains("weld") -> Icons.Outlined.OutlinedBuild
        n.contains("mason") || n.contains("tile") || n.contains("construct") -> Icons.Outlined.OutlinedConstruction
        n.contains("technician") || n.startsWith("ac ") -> Icons.Outlined.OutlinedHandyman
        n.contains("secur") || n.contains("guard") -> Icons.Outlined.OutlinedSecurity
        n.contains("child") || n.contains("baby") -> Icons.Outlined.OutlinedChildCare
        n.contains("elder") || n.contains("care") -> Icons.Outlined.OutlinedFavorite
        n.contains("helper") -> Icons.Outlined.OutlinedHandyman
        n.contains("other") -> Icons.Outlined.OutlinedMoreHoriz
        else -> Icons.Outlined.OutlinedWork
    }
}

/** Icon for a gender option. */
private fun genderIcon(option: String): ImageVector {
    return when (option.lowercase()) {
        "male" -> Icons.Outlined.OutlinedMale
        "female" -> Icons.Outlined.OutlinedFemale
        else -> Icons.Outlined.OutlinedPerson
    }
}

/** Small uppercase, letter-spaced section label (GENDER, EXPERIENCE, ...). */
@Composable
private fun StitchSectionLabel(text: String) {
    Text(
        text = text,
        color = StitchLabel,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

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
    navController: NavController,
    returnRoute: String? = null // Optional return route for job application flow
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val reviewTriggerService = rememberInAppReviewTriggerService()
    val crashlytics = remember { FirebaseCrashlytics.getInstance() }
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
    var educationQualification by rememberSaveable { mutableStateOf("") }
    var workerBio by rememberSaveable { mutableStateOf("") }

    // Selfie state - Uri cannot be saved directly, so we save the string representation
    var selfieUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val selfieUri = selfieUriString?.let { Uri.parse(it) }
    var selfieUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isUploadingSelfie by remember { mutableStateOf(false) }
    var selfieError by remember { mutableStateOf<String?>(null) }
    
    // Aadhaar Identity state
    var aadhaarNumber by rememberSaveable { mutableStateOf("") }
    var aadhaarNumberError by remember { mutableStateOf<String?>(null) }
    var aadhaarPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val aadhaarPhotoUri = aadhaarPhotoUriString?.let { Uri.parse(it) }
    var aadhaarPhotoUrl by rememberSaveable { mutableStateOf<String?>(null) }
    
    val aadhaarPhotoPicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            aadhaarPhotoUriString = uri.toString()
            aadhaarPhotoUrl = null
        }
    }
    
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
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    var isProfileCompleted by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var isEmailLoaded by remember { mutableStateOf(false) }
    var authMethod by rememberSaveable { mutableStateOf<String?>(null) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }  // Show errors only after Next click
    var isCompletionInProgress by remember { mutableStateOf(false) }  // Prevent double-execution
    val totalSteps = 3  // Identity Verification step removed

    fun logFunnelEvent(event: String, extras: Map<String, String> = emptyMap()) {
        runCatching {
            crashlytics.log("profile_funnel_worker:$event")
            crashlytics.setCustomKey("profile_funnel_role", "WORKER")
            crashlytics.setCustomKey("profile_funnel_event", event)
            crashlytics.setCustomKey("profile_funnel_step", currentStep)
            extras.forEach { (k, v) -> crashlytics.setCustomKey("profile_funnel_$k", v) }
        }.onFailure { Timber.w(it, "Failed to log worker funnel telemetry") }
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
            // Get auth method to determine which field to prefill
            authMethod = profileCompletionViewModel.getAuthMethod()
            Timber.d("MandatoryWorkerProfileSetupScreen - Auth Method: $authMethod")
            
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
                    Timber.d("📦 PREFILL: Loading existing worker profile data (lightweight)")
                    
                    // Prefill form fields with existing data (schema-compliant fields only)
                    val savedFullName = existingData["fullName"] as? String
                    val savedPhone = existingData["phone"] as? String
                    val savedEmail = existingData["email"] as? String
                    val savedDateOfBirth = existingData["dateOfBirth"] as? String
                    val savedGender = existingData["gender"] as? String
                    val savedExperience = existingData["experience"] as? String
                    val savedEducationQualification = existingData["educationQualification"] as? String
                    val savedBio = existingData["bio"] as? String
                    // skills from worker_profiles (List<String>) — joined for display in skills field
                    val savedSkills = when (val rawSkills = existingData["skills"]) {
                        is List<*> -> rawSkills.filterIsInstance<String>()
                        is String -> rawSkills.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        else -> emptyList()
                    }
                    val savedProfileImageUrl = existingData["profileImageUrl"] as? String
                    val savedAadhaarNumber = existingData["aadhaarNumber"] as? String
                    val savedAadhaarPhotoUrl = existingData["aadhaarPhotoUrl"] as? String
                    
                    // Apply prefilled values (only if current field is empty)
                    if (fullName.isBlank() && !savedFullName.isNullOrBlank()) {
                        fullName = savedFullName
                        Timber.d("📦 PREFILL: fullName = $fullName")
                    }
                    if (phoneNumber.isBlank() && !savedPhone.isNullOrBlank()) {
                        // Clean phone number (remove country code if present)
                        phoneNumber = savedPhone.replace("+91", "").trim()
                        Timber.d("📦 PREFILL: phoneNumber = $phoneNumber")
                    }
                    if (email.isBlank() && !savedEmail.isNullOrBlank()) {
                        email = savedEmail
                        isEmailLoaded = true
                        Timber.d("PREFILL: email restored")
                    }
                    if (skills.isBlank() && savedSkills.isNotEmpty()) {
                        skills = savedSkills.joinToString(", ")
                        Timber.d("📦 PREFILL: skills from jobTypes = $skills")
                    }
                    if (dateOfBirth.isBlank() && !savedDateOfBirth.isNullOrBlank()) {
                        dateOfBirth = savedDateOfBirth
                        Timber.d("📦 PREFILL: dateOfBirth = $dateOfBirth")
                    }
                    if (gender.isBlank() && (savedGender == "Male" || savedGender == "Female")) {
                        gender = savedGender
                        Timber.d("📦 PREFILL: gender = $gender")
                    }
                    if (experience.isBlank() && !savedExperience.isNullOrBlank()) {
                        experience = savedExperience
                        Timber.d("📦 PREFILL: experience restored")
                    }
                    if (educationQualification.isBlank() && !savedEducationQualification.isNullOrBlank()) {
                        educationQualification = savedEducationQualification
                        Timber.d("PREFILL: education qualification restored")
                    }
                    if (workerBio.isBlank() && !savedBio.isNullOrBlank()) {
                        workerBio = savedBio
                        Timber.d("PREFILL: worker bio restored")
                    }
                    if (!savedProfileImageUrl.isNullOrBlank()) {
                        selfieUrl = savedProfileImageUrl
                        Timber.d("📦 PREFILL: profileImageUrl exists")
                    }
                    if (aadhaarNumber.isBlank() && !savedAadhaarNumber.isNullOrBlank()) {
                        aadhaarNumber = savedAadhaarNumber
                        Timber.d("📦 PREFILL: aadhaarNumber = ****${savedAadhaarNumber.takeLast(4)}")
                    }
                    if (aadhaarPhotoUrl.isNullOrBlank() && !savedAadhaarPhotoUrl.isNullOrBlank()) {
                        aadhaarPhotoUrl = savedAadhaarPhotoUrl
                    }
                }
            }
            
            // Fallback: Load from auth methods if fields still empty
            when (authMethod) {
                "GOOGLE" -> {
                    // Google Auth Flow: Prefill email and name from Google
                    if (email.isBlank()) {
                        val googleEmail = profileCompletionViewModel.getUserEmail()
                        if (googleEmail != null) {
                            email = googleEmail
                            isEmailLoaded = true
                            Timber.d("📦 PREFILL: email from Google = $email")
                        }
                    }
                    if (fullName.isBlank()) {
                        val googleName = profileCompletionViewModel.getUserName()
                        if (googleName != null) {
                            fullName = googleName
                            Timber.d("📦 PREFILL: fullName from Google = $fullName")
                        }
                    }
                }
                
                "PHONE_OTP" -> {
                    // OTP Auth Flow: Prefill phone number only
                    if (phoneNumber.isBlank()) {
                        val otpPhone = profileCompletionViewModel.getPhoneNumber()
                        if (otpPhone != null) {
                            phoneNumber = otpPhone.replace("+91", "").trim()
                            Timber.d("📦 PREFILL: phoneNumber from OTP = $phoneNumber")
                        }
                    }
                    // Reuse the name captured in the registration bottom-sheet so users
                    // don't have to type their name a second time right after OTP.
                    if (fullName.isBlank()) {
                        val cachedName = profileCompletionViewModel.getUserName()
                        if (!cachedName.isNullOrBlank()) {
                            fullName = cachedName
                            Timber.d("📦 PREFILL: fullName from registration cache = $fullName")
                        }
                    }
                }
                
                else -> {
                    Timber.w("Unknown auth method: $authMethod")
                    isEmailLoaded = true
                }
            }
            
            Timber.d("📦 PREFILL: Final values - email=$email, fullName=$fullName, phoneNumber=$phoneNumber")
        } catch (e: Exception) {
            Timber.e(e, "📦 PREFILL: Error loading existing profile data")
        } finally {
            isLoadingExistingData = false
        }
    }

    LaunchedEffect(currentStep) {
        logFunnelEvent("step_viewed", mapOf("step" to currentStep.toString()))
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
    val backgroundColor = WorkerColors.CardBackground
    
    // Animation state for smooth transitions
    val animatedProgress by animateFloatAsState(
        targetValue = currentStep.toFloat() / totalSteps.toFloat(),
        animationSpec = tween(600, easing = EaseInOutCubic),
        label = "progress"
    )
    
    // Step-specific validation based on auth method
    // For GOOGLE auth: fullName (prefilled), email (prefilled, read-only), phoneNumber (user enters), address
    // For OTP auth: fullName (user enters), email (optional), phoneNumber (prefilled, read-only), address

    
    // Step 1 (Basic information): name, valid date of birth, gender (+ phone/email per auth method).
    val isStep1Valid = fullName.isNotBlank() &&
        ValidationUtils.isValidIndianPhoneNumber(phoneNumber) &&
        (authMethod != "GOOGLE" || email.isNotBlank()) &&
        (gender == "Male" || gender == "Female") &&
        dateOfBirth.isNotBlank() && ValidationUtils.isValidDateOfBirth(dateOfBirth)
    // Step 2 (Skills & experience): at least one skill and an experience level.
    // Education is optional (short bio lives in step 1 and is optional too).
    val isStep2Valid = skills.isNotBlank() && experience.isNotBlank()
    // Step 3 (Where are you based?): address only.
    val isStep3Valid = address.isNotBlank()
    val isStep4Valid = true // Identity Verification step removed
    
    // Overall form validation
    val isFormValid = isStep1Valid && isStep2Valid && isStep3Valid
    
    
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
    var bioError by remember { mutableStateOf<String?>(null) }
    
    // Debug logging for form validation
    LaunchedEffect(fullName, email, phoneNumber, address, dateOfBirth, gender, skills, experience, workerBio, currentStep, isCurrentStepValid, showValidationErrors) {
        // Show DOB age errors immediately when a DOB is selected/typed,
        // while keeping "required" gating behind Next click.
        val liveDateOfBirthError = if (dateOfBirth.length < 10) null else ValidationUtils.getDateOfBirthError(dateOfBirth)

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
            
            // Update Aadhaar error
            aadhaarNumberError = when {
                aadhaarNumber.length < 4 -> "Must be exactly 4 digits"
                else -> null
            }
            
            // Update date of birth error with age validation
            dateOfBirthError = ValidationUtils.getDateOfBirthError(dateOfBirth)
            
            // Update gender error
            genderError = when {
                gender != "Male" && gender != "Female" -> "Gender is required"
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

            bioError = when {
                workerBio.isNotBlank() && workerBio.trim().length < MIN_WORKER_BIO_LENGTH -> "Write at least $MIN_WORKER_BIO_LENGTH characters, or leave bio empty"
                else -> null
            }
        } else {
            // Clear all errors when not showing validation
            phoneError = null
            emailError = null
            fullNameError = null
            addressError = null
            dateOfBirthError = liveDateOfBirthError
            genderError = null
            skillsError = null
            experienceError = null
            bioError = null
        }
        
        Timber.d("Form validation - step=$currentStep, step1Valid=$isStep1Valid, step2Valid=$isStep2Valid, step3Valid=$isStep3Valid, currentValid=$isCurrentStepValid")
    }
    
    // Show loading while fetching existing profile data
    if (isLoadingExistingData) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.example.dutype.ui.theme.WorkerColors.CardBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = WorkerColors.Primary
                )
                Text(
                    "Loading your profile...",
                    color = Color.Gray
                )
            }
        }
        return
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.WorkerColors.CardBackground)
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
                            containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
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
                            // Stitch design: 3-segment progress bar, shown above every step
                            StitchStepProgressBar(currentStep = currentStep, totalSteps = totalSteps)

                            // Step 1: Basic information (photo, name, date of birth, gender, short bio)
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
                                        gender = gender,
                                        workerBio = workerBio,
                                        dateOfBirth = dateOfBirth,
                                        selfieUri = selfieUri,
                                        phoneError = if (showValidationErrors) phoneError else null,
                                        emailError = if (showValidationErrors) emailError else null,
                                        fullNameError = if (showValidationErrors) fullNameError else null,
                                        genderError = if (showValidationErrors) genderError else null,
                                        bioError = if (showValidationErrors) bioError else null,
                                        dateOfBirthError = dateOfBirthError,
                                        onWorkerBioChange = { workerBio = it },
                                        onGenderChange = { gender = it },
                                        onDateOfBirthChange = { dateOfBirth = it },
                                        onSelfieSelected = { uri ->
                                            selfieUriString = uri.toString()
                                            selfieUrl = null
                                        },
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

                            // Step 2: Skills & experience (skills, experience, education)
                            if (currentStep == 2) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                                ) {
                                    SkillsExperienceStep(
                                        skills = skills,
                                        experience = experience,
                                        educationQualification = educationQualification,
                                        skillsError = if (showValidationErrors) skillsError else null,
                                        experienceError = if (showValidationErrors) experienceError else null,
                                        onSkillsChange = { skills = it },
                                        onExperienceChange = { experience = it },
                                        onEducationChange = { educationQualification = it }
                                    )
                                }
                            }

                            // Step 3: Where are you based? (location)
                            if (currentStep == 3) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                                ) {
                                    LocationStep(
                                        address = address,
                                        addressError = if (showValidationErrors) addressError else null,
                                        onAddressChange = { address = it },
                                        locationService = locationService,
                                        locationPreferences = profileCompletionViewModel.locationPreferences
                                    )
                                }
                            }

                            // Error Message
                            if (errorMessage != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = WorkerColors.ErrorLight),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = WorkerColors.Error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = errorMessage ?: "",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = WorkerColors.Error
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
                color = WorkerColors.CardBackground,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Previous Button (back arrow only)
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = {
                                logFunnelEvent("step_back", mapOf("from_step" to currentStep.toString()))
                                currentStep--
                            },
                            modifier = Modifier
                                .size(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WorkerColors.Primary
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
                    }
                    
                    // Next/Complete Button
                    Button(
                        onClick = {
                            // Show validation errors when Next is clicked
                            showValidationErrors = true
                            
                            if (isCurrentStepValid) {
                                if (currentStep < totalSteps) {
                                    val previousStep = currentStep
                                    currentStep++
                                    logFunnelEvent(
                                        "step_advanced",
                                        mapOf(
                                            "from_step" to previousStep.toString(),
                                            "to_step" to currentStep.toString()
                                        )
                                    )
                                    showValidationErrors = false  // Reset errors for next step
                                } else {
                                    // Prevent double-execution
                                    if (isCompletionInProgress) {
                                        Timber.w("📍 Profile completion already in progress, ignoring duplicate call")
                                        return@Button
                                    }
                                    
                                    isCompletionInProgress = true
                                    
                                    // Complete profile setup
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        selfieError = null
                                        
                                        try {
                                            // Save profile data to Firestore
                                            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                            if (currentUser == null) {
                                                logFunnelEvent("completion_failed", mapOf("reason" to "missing_auth_user"))
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
                                                
                                                // Upload Aadhaar photo if available
                                                var uploadedAadhaarPhotoUrl: String? = null
                                                if (false && aadhaarPhotoUri != null) { // Identity Verification step removed
                                                    try {
                                                        val uploadResult = profileCompletionViewModel.uploadProfileImage(
                                                            aadhaarPhotoUri!!,
                                                            currentUser.uid,
                                                            "WORKER_AADHAAR"
                                                        )
                                                        uploadResult.fold(
                                                            onSuccess = { url ->
                                                                uploadedAadhaarPhotoUrl = url
                                                                aadhaarPhotoUrl = url
                                                                Timber.d("📸 Worker Aadhaar photo uploaded: $url")
                                                            },
                                                            onFailure = { e ->
                                                                Timber.e(e, "📸 Failed to upload worker Aadhaar photo")
                                                                errorMessage = "Aadhaar photo upload failed. Please try again."
                                                                throw Exception("Aadhaar photo upload failed")
                                                            }
                                                        )
                                                    } catch (e: Exception) {
                                                        Timber.e(e, "📸 Exception during Aadhaar photo upload")
                                                        errorMessage = "Aadhaar photo upload failed. Please try again."
                                                        throw e
                                                    }
                                                }
                                                
                                                // skills are normalized in ProfileCompletionService.saveWorkerProfileData
                                                val workerProfileData = mutableMapOf<String, Any>(
                                                    "fullName" to fullName,
                                                    "phone" to phoneNumber,
                                                    "address" to address.trim(),
                                                    "skills" to skills,
                                                    "dateOfBirth" to dateOfBirth,
                                                    "gender" to gender,
                                                    "experience" to experience,
                                                    "bio" to workerBio.trim()
                                                    // "aadhaarNumber" to aadhaarNumber
                                                )

                                                // Store email if provided
                                                if (email.isNotBlank()) {
                                                    workerProfileData["email"] = email.trim()
                                                }
                                                if (educationQualification.isNotBlank()) {
                                                    workerProfileData["educationQualification"] = educationQualification.trim()
                                                }

                                                profileCompletionViewModel.locationPreferences
                                                    .getSavedLocationIfFresh()
                                                    ?.takeIf { it.hasValidCoordinates() }
                                                    ?.let { savedLocation ->
                                                        workerProfileData["location"] = mapOf(
                                                            "lat" to savedLocation.latitude,
                                                            "lng" to savedLocation.longitude,
                                                            "address" to address.trim()
                                                        )
                                                    }
                                                
                                                // Add selfie URL if uploaded
                                                if (uploadedSelfieUrl != null) {
                                                    workerProfileData["profileImageUrl"] = uploadedSelfieUrl!!
                                                }
                                                
                                                // Add Aadhaar Photo URL if uploaded
                                                // Identity Verification step removed
                                                profileCompletionViewModel
                                                    .saveWorkerProfileData(workerProfileData)
                                                    .getOrThrow()

                                                val savedReferralCode = profileCompletionViewModel.getReferralCode()
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?: (workerProfileData["referredByCode"] as? String)?.takeIf { it.isNotBlank() }
                                                if (!savedReferralCode.isNullOrBlank()) {
                                                    scope.launch {
                                                        runCatching {
                                                            val referralApplyResult = profileCompletionViewModel.applyReferralCode(
                                                                referralCode = savedReferralCode,
                                                                newUserId = currentUser.uid,
                                                                newUserRole = UserRole.WORKER.name,
                                                                newUserName = fullName.ifBlank { phoneNumber },
                                                                newUserPhone = phoneNumber
                                                            )

                                                            if (referralApplyResult.isSuccess) {
                                                                Toast.makeText(
                                                                    context,
                                                                    context.getString(R.string.referral_code_applied_success),
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            } else {
                                                                Timber.w(
                                                                    "🎁 REFERRAL: Worker fallback apply failed: ${referralApplyResult.exceptionOrNull()?.message}"
                                                                )
                                                            }
                                                        }.onFailure { Timber.w(it, "🎁 REFERRAL: Worker background apply error") }
                                                    }
                                                }
                                            }

                                            // Save role to local DataStore so app knows which home to navigate to on reopen
                                            profileCompletionViewModel.saveUserInfoToLocalStorage(fullName, UserRole.WORKER)
                                            
                                            // Check if this is the FIRST time completing profile (not an update)
                                            val wasAlreadyComplete = profileCompletionViewModel.isProfileComplete(UserRole.WORKER)
                                            
                                            // Mark profile as complete
                                            profileCompletionViewModel.markProfileComplete(UserRole.WORKER)

                                            // Mark profile setup as shown for worker
                                            profileCompletionViewModel.markProfileSetupAsShown(UserRole.WORKER)
                                            
                                            // Send profile completion notification ONLY on first completion (not on updates)
                                            if (!wasAlreadyComplete) {
                                                // Flag for welcome celebration overlay on home screen
                                                markWelcomeCelebrationPending(context)

                                                val notificationUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                                if (notificationUser != null) {
                                                    try {
                                                        notificationService.sendProfileCompleteNotification(
                                                            userName = fullName,
                                                            userId = notificationUser.uid,
                                                            userRole = "WORKER"
                                                        ).onSuccess {
                                                            Timber.d("📬 Profile completion notification sent for worker (first time)")
                                                        }.onFailure { error ->
                                                            Timber.e(error, "📬 Worker profile completion notification failed")
                                                        }
                                                        
                                                        // Register FCM token with role for push notifications
                                                        fcmTokenManager.registerTokenWithRole("WORKER")
                                                        Timber.d("📬 FCM token registered with WORKER role")
                                                    } catch (e: Exception) {
                                                        Timber.e(e, "📬 Failed to send profile completion notification or register FCM")
                                                    }
                                                }

                                                val activity = context as? Activity
                                                if (activity != null) {
                                                    reviewTriggerService.onWorkerProfileCompleted(activity)
                                                }
                                            } else {
                                                Timber.d("📬 Profile already complete - skipping notification (this is a profile update)")
                                            }
                                            
                                            // Navigate to return route (job application) or location fetching screen
                                            if (returnRoute != null) {
                                                navController.navigate(returnRoute) {
                                                    popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                                                }
                                                logFunnelEvent("completed", mapOf("destination" to returnRoute))
                                            } else {
                                                navController.navigate(Routes.WORKER_HOME) {
                                                    popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                                                }
                                                logFunnelEvent("completed", mapOf("destination" to Routes.WORKER_HOME))
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
                            } else {
                                logFunnelEvent("step_blocked", mapOf("step" to currentStep.toString()))
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCurrentStepValid) StitchInk else WorkerColors.TextDisabled,
                            disabledContainerColor = WorkerColors.TextDisabled
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = StitchWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when (currentStep) {
                                    1 -> "Next: Skills →"
                                    2 -> "Next: Location →"
                                    else -> "Complete Profile ✓"
                                },
                                color = StitchWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
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
    gender: String,
    workerBio: String,
    dateOfBirth: String,
    selfieUri: Uri?,
    phoneError: String?,
    emailError: String?,
    fullNameError: String?,
    genderError: String?,
    bioError: String?,
    dateOfBirthError: String?,
    onWorkerBioChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onSelfieSelected: (Uri) -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Step header
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            Text(
                text = "Basic information",
                color = StitchTitle,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your photo, name, birth date, gender and short bio",
                color = StitchSubtitle,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        ProfilePhotoPicker(selfieUri = selfieUri, onSelfieSelected = onSelfieSelected)

        FullNameField(fullName = fullName, error = fullNameError, onChange = onFullNameChange)

        DateOfBirthField(
            dateOfBirth = dateOfBirth,
            error = dateOfBirthError,
            onDateOfBirthChange = onDateOfBirthChange
        )

        // GENDER
        Column {
            StitchSectionLabel("GENDER")
            GenderChipsRow(gender = gender, onGenderChange = onGenderChange)
            if (genderError != null) {
                Text(
                    text = genderError,
                    color = WorkerColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }
        }

        WorkerBioSection(
            workerBio = workerBio,
            bioError = bioError,
            onWorkerBioChange = onWorkerBioChange
        )

        ContactDetailsSection(
            email = email,
            phoneNumber = phoneNumber,
            authMethod = authMethod,
            emailError = emailError,
            phoneError = phoneError,
            onEmailChange = onEmailChange,
            onPhoneChange = onPhoneChange
        )
    }
}

@Composable
private fun FullNameField(fullName: String, error: String?, onChange: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = fullName,
            onValueChange = onChange,
            label = {
                Text(
                    "Full Name",
                    color = StitchFieldLabel,
                    fontSize = 12.sp
                )
            },
            placeholder = { Text(stringResource(R.string.enter_full_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = StitchTitle
            ),
            shape = RoundedCornerShape(14.dp),
            isError = error != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) WorkerColors.Error else StitchInk,
                unfocusedBorderColor = if (error != null) WorkerColors.Error else StitchBorder,
                focusedLabelColor = if (error != null) WorkerColors.Error else StitchFieldLabel,
                unfocusedLabelColor = StitchFieldLabel,
                errorBorderColor = WorkerColors.Error,
                cursorColor = StitchInk
            ),
            singleLine = true
        )
        if (error != null) {
            Text(
                text = error,
                color = WorkerColors.Error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/** Optional profile photo: dashed circle with camera icon, wired to the selfie state. */
@Composable
private fun ProfilePhotoPicker(selfieUri: Uri?, onSelfieSelected: (Uri) -> Unit) {
    val selfiePicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onSelfieSelected(uri)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val dashColor = StitchBorder
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .clickable { selfiePicker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = dashColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    ),
                    cornerRadius = CornerRadius(size.minDimension / 2f, size.minDimension / 2f)
                )
            }
            if (selfieUri != null) {
                coil.compose.AsyncImage(
                    model = selfieUri,
                    contentDescription = "Selected profile photo",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Add profile photo",
                    tint = StitchSubtitle,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = "Add profile photo (optional)",
            color = StitchSubtitle,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

/** Email + phone (needed for account/auth; behavior differs per auth method). */
@Composable
private fun ContactDetailsSection(
    email: String,
    phoneNumber: String,
    authMethod: String?,
    emailError: String?,
    phoneError: String?,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Contact details — required for account/auth (phone always, email for Google
        // auth) but not part of the Stitch screenshot, so styled as a secondary,
        // lower-emphasis block underneath the primary design elements above.
        StitchSectionLabel("CONTACT DETAILS")

        // Email - Behavior differs based on authentication method
        if (authMethod == "GOOGLE") {
            // Google Auth: Email is prefilled and read-only
            OutlinedTextField(
                value = email,
                onValueChange = { }, // Read-only
                label = { Text(stringResource(R.string.email_address_verified)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = WorkerColors.Border,
                    disabledTextColor = WorkerColors.TextSecondary
                ),
                trailingIcon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified by Google",
                        tint = WorkerColors.TextPrimary,
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
                    label = { Text(stringResource(R.string.email_address_optional)) },
                    placeholder = { Text(stringResource(R.string.enter_email)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (emailError != null) WorkerColors.Error else WorkerColors.Primary,
                        unfocusedBorderColor = if (emailError != null) WorkerColors.Error else WorkerColors.Border,
                        errorBorderColor = WorkerColors.Error
                    ),
                    singleLine = true
                )
                if (emailError != null) {
                    Text(
                        text = emailError,
                        color = WorkerColors.Error,
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
                    label = { Text(stringResource(R.string.email_address)) },
                    placeholder = { Text(stringResource(R.string.enter_email)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (emailError != null) WorkerColors.Error else WorkerColors.Primary,
                        unfocusedBorderColor = if (emailError != null) WorkerColors.Error else WorkerColors.Border,
                        errorBorderColor = WorkerColors.Error
                    ),
                    singleLine = true
                )
                if (emailError != null) {
                    Text(
                        text = emailError,
                        color = WorkerColors.Error,
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
                label = { Text(stringResource(R.string.phone_number_verified)) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = WorkerColors.Border,
                    disabledTextColor = WorkerColors.TextSecondary
                ),
                trailingIcon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified by OTP",
                        tint = WorkerColors.TextPrimary,
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
                    label = { Text(stringResource(R.string.phone_number_required)) },
                    placeholder = { Text(stringResource(R.string.enter_10_digit_phone)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (phoneError != null) WorkerColors.Error else WorkerColors.Primary,
                        unfocusedBorderColor = if (phoneError != null) WorkerColors.Error else WorkerColors.Border,
                        errorBorderColor = WorkerColors.Error
                    )
                )
                if (phoneError != null) {
                    Text(
                        text = phoneError,
                        color = WorkerColors.Error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationStep(
    address: String,
    addressError: String?,
    onAddressChange: (String) -> Unit,
    locationService: com.example.dutype.utils.LocationService,
    locationPreferences: com.example.dutype.location.LocationPreferences
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isFetchingLocation by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var detectedAddress by remember { mutableStateOf<String?>(null) }
    var detectedArea by remember { mutableStateOf<String?>(null) }

    suspend fun fetchAddressFast(silent: Boolean) {
        val cachedLocation = locationPreferences.getSavedLocationIfFresh(24 * 60 * 60 * 1000L)
        if (cachedLocation != null) {
            Timber.d("📍 Fetch - Using recent cached location immediately")
            onAddressChange(cachedLocation.getFullAddress())
            detectedAddress = cachedLocation.getFullAddress()
            detectedArea = cachedLocation.city?.takeIf { it.isNotBlank() }
            locationPreferences.setPermissionGranted(true)
        }

        val refinedLocation = locationService.getHighAccuracyLocationData(
            timeoutMs = if (cachedLocation != null) 5000L else 10000L,
            minAccuracyMeters = 40f
        ) ?: locationService.getCurrentLocation()?.let { locationService.toLocationData(it) }

        val finalLocation = refinedLocation ?: cachedLocation
        if (finalLocation != null) {
            Timber.d("📍 Fetch - Location resolved: ${finalLocation.getFullAddress()}")
            onAddressChange(finalLocation.getFullAddress())
            detectedAddress = finalLocation.getFullAddress()
            detectedArea = finalLocation.city?.takeIf { it.isNotBlank() }
            locationPreferences.saveLocation(finalLocation)
            locationPreferences.setPermissionGranted(true)
        } else {
            Timber.w("📍 Fetch - Could not resolve location")
            fetchError = "Could not get location. Please enter address manually or try again."
            if (!silent) {
                Toast.makeText(
                    context,
                    "Could not get location. Please enter address manually or try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun startFetch(silent: Boolean) {
        coroutineScope.launch {
            isFetchingLocation = true
            fetchError = null
            try {
                fetchAddressFast(silent)
            } catch (e: Exception) {
                Timber.e(e, "📍 Fetch - Error fetching location")
                fetchError = "Error fetching location"
                if (!silent) {
                    Toast.makeText(context, context.getString(R.string.error_fetching_location), Toast.LENGTH_SHORT).show()
                }
            } finally {
                isFetchingLocation = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        Timber.d("📍 Fetch - Location permission result: $granted")
        if (granted) {
            startFetch(false)
        } else {
            Toast.makeText(context, context.getString(R.string.location_permission_fetch_address), Toast.LENGTH_SHORT).show()
            isFetchingLocation = false
        }
    }

    // Ask for location permission once when this step opens; if already granted,
    // fetch immediately. Denial silently falls back to manual entry.
    var autoLocationAsked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoLocationAsked) {
            autoLocationAsked = true
            if (locationService.hasLocationPermission()) {
                if (address.isBlank()) startFetch(true)
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 2.dp)) {
            Text(
                text = "Where are you based?",
                color = StitchTitle,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "We use this to show jobs near you",
                color = StitchSubtitle,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            com.example.dutype.components.LocationAutocompleteField(
                value = address,
                onValueChange = onAddressChange,
                onLocationSelected = { selectedAddress, lat, lng ->
                    onAddressChange(selectedAddress)
                    if (com.example.dutype.utils.GeoUtils.hasValidCoordinates(lat, lng)) {
                        locationPreferences.savePreferredLocation(
                            com.example.dutype.models.LocationData(
                                latitude = lat,
                                longitude = lng,
                                city = null,
                                address = selectedAddress
                            )
                        )
                        locationPreferences.setPermissionGranted(true)
                    }
                },
                locationService = locationService,
                label = stringResource(R.string.address_label),
                placeholder = stringResource(R.string.search_or_enter_address_w),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (addressError != null) WorkerColors.Error else WorkerColors.Primary,
                    unfocusedBorderColor = if (addressError != null) WorkerColors.Error else WorkerColors.Border,
                    errorBorderColor = WorkerColors.Error
                )
            )
            if (addressError != null) {
                Text(
                    text = addressError,
                    color = WorkerColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            // Fetched-location info lives UNDER the address field (never above it).
            LocationStatusRow(
                isFetching = isFetchingLocation,
                fetchError = fetchError,
                showDetected = detectedAddress != null && detectedAddress == address,
                detectedArea = detectedArea,
                onDetect = {
                    fetchError = null
                    if (locationService.hasLocationPermission()) {
                        startFetch(false)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            )
        }
    }
}

/** Line under the address field: detection status on the left, "Detect" action on the right. */
@Composable
private fun LocationStatusRow(
    isFetching: Boolean,
    fetchError: String?,
    showDetected: Boolean,
    detectedArea: String?,
    onDetect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isFetching) {
                Text(
                    text = "Detecting your location…",
                    color = StitchSubtitle,
                    fontSize = 12.sp
                )
            } else if (fetchError != null) {
                Text(
                    text = fetchError,
                    color = WorkerColors.Error,
                    fontSize = 12.sp
                )
            } else if (showDetected) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = StitchSuccess,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Location detected",
                    color = StitchSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (detectedArea != null) {
                    Text(
                        text = "  ·  $detectedArea",
                        color = StitchSubtitle,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDetect)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = StitchAccent
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isFetching) "Detecting…" else "Detect",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = StitchAccent
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenderChipsRow(gender: String, onGenderChange: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf("Male", "Female").forEach { option ->
            StitchPill(
                text = option,
                selected = gender == option,
                leadingIcon = genderIcon(option),
                onClick = { onGenderChange(option) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillChipsFlow(
    skills: List<String>,
    selected: Set<String>,
    onSkillClick: (String, Boolean) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        skills.forEach { skill ->
            val isSelected = selected.contains(skill)
            StitchPill(
                text = skill,
                selected = isSelected,
                leadingIcon = skillIcon(skill),
                onClick = { onSkillClick(skill, isSelected) }
            )
        }
    }
}

/** Formats up to 8 raw digits as DD/MM/YYYY (slashes only once the next digit exists). */
private fun formatDobDigits(digits: String): String {
    val d = digits.filter { it.isDigit() }.take(8)
    val out = StringBuilder()
    d.forEachIndexed { index, c ->
        if (index == 2 || index == 4) out.append('/')
        out.append(c)
    }
    return out.toString()
}

/** Shows raw digits ("12032000") as "12/03/2000" while keeping the cursor mapped correctly. */
private object DobVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = formatDobDigits(digits)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val mapped = when {
                    offset <= 2 -> offset
                    offset <= 4 -> offset + 1
                    else -> offset + 2
                }
                return mapped.coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val mapped = when {
                    offset <= 2 -> offset
                    offset <= 5 -> offset - 1
                    else -> offset - 2
                }
                return mapped.coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

/**
 * Date of birth: typed as DD/MM/YYYY (auto-slashes) OR picked from the calendar
 * dialog. Both use the same stored value ("dd/MM/yyyy"), so they stay in sync.
 */
@Composable
private fun DateOfBirthField(
    dateOfBirth: String,
    error: String?,
    onDateOfBirthChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val digits = dateOfBirth.filter { it.isDigit() }.take(8)
    val borderColor = when {
        error != null -> WorkerColors.Error
        focused -> StitchInk
        else -> StitchBorder
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Date of birth",
            color = StitchFieldLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Enter as DD/MM/YYYY or pick from calendar",
            color = StitchLabel,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
        DobInputBox(
            digits = digits,
            borderColor = borderColor,
            onDigitsChange = { newDigits -> onDateOfBirthChange(formatDobDigits(newDigits)) },
            onFocusChange = { focused = it },
            onCalendarClick = { showPicker = true }
        )
        if (error != null) {
            Text(
                text = error,
                color = WorkerColors.Error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
    if (showPicker) {
        DobPickerDialog(
            currentValue = dateOfBirth,
            onDismiss = { showPicker = false },
            onPicked = { formatted ->
                onDateOfBirthChange(formatted)
                showPicker = false
            }
        )
    }
}

@Composable
private fun DobInputBox(
    digits: String,
    borderColor: Color,
    onDigitsChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onCalendarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StitchWhite)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = digits,
            onValueChange = { raw -> onDigitsChange(raw.filter { it.isDigit() }.take(8)) },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { onFocusChange(it.isFocused) },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = StitchTitle
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            visualTransformation = DobVisualTransformation,
            cursorBrush = SolidColor(StitchInk),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (digits.isEmpty()) {
                        Text(
                            text = "DD/MM/YYYY",
                            color = StitchLabel,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        IconButton(onClick = onCalendarClick) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Pick date of birth from calendar",
                tint = StitchSubtitle,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobPickerDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onPicked: (String) -> Unit
) {
    val today = LocalDate.now()
    val minDobDate = today.minusYears(70)
    val maxDobDate = today.minusYears(18)
    // A typed date is only used as the picker's start when it is inside the allowed
    // range (an out-of-range initial date would crash the picker state).
    val typedMillis = parseDobToUtcMillis(currentValue)?.takeIf { millis ->
        val date = utcMillisToLocalDate(millis)
        !date.isBefore(minDobDate) && !date.isAfter(maxDobDate)
    }
    val initialMillis = typedMillis ?: today.minusYears(25).toUtcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = (today.year - 70)..(today.year - 18),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = utcMillisToLocalDate(utcTimeMillis)
                return !date.isBefore(minDobDate) && !date.isAfter(maxDobDate)
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year in minDobDate.year..maxDobDate.year
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        colors = DatePickerDefaults.colors(containerColor = Color.White),
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0F0F0F)),
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) onPicked(formatDobFromUtcMillis(millis)) else onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0F0F0F))
            ) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = state,
            showModeToggle = true,
            colors = whiteDatePickerColors()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun whiteDatePickerColors(): DatePickerColors {
    val ink = Color(0xFF0F0F0F)
    val muted = Color(0xFF64748B)
    return DatePickerDefaults.colors(
        containerColor = Color.White,
        titleContentColor = muted,
        headlineContentColor = ink,
        weekdayContentColor = muted,
        subheadContentColor = ink,
        navigationContentColor = ink,
        yearContentColor = ink,
        currentYearContentColor = ink,
        selectedYearContentColor = Color.White,
        selectedYearContainerColor = ink,
        dayContentColor = ink,
        selectedDayContentColor = Color.White,
        selectedDayContainerColor = ink,
        todayContentColor = ink,
        todayDateBorderColor = ink,
        dividerColor = Color(0xFFE2E8F0),
        dateTextFieldColors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = ink,
            cursorColor = ink
        )
    )
}

// ---------------------------------------------------------------------------
// Step 2 — Skills & experience
// ---------------------------------------------------------------------------

/** Chips shown by default (first 9 visible, the rest behind "N more skills"). */
private val DEFAULT_SKILLS = listOf(
    "Cooking", "Cleaning", "Driving", "Delivery", "Childcare", "Elderly care",
    "Tailoring", "Plumbing", "Electrical", "Painting", "Gardening", "Security", "Others"
)

/** Extra skills that are NOT shown as chips by default; found through the search bar. */
private val EXTRA_SKILLS = listOf("Tile Fitter", "Welder", "Mason", "AC Technician", "Mechanic")

private const val OTHERS_SKILL = "Others"
private const val DEFAULT_VISIBLE_SKILLS = 9
private const val MAX_CUSTOM_SKILL_LENGTH = 30

private val EXPERIENCE_OPTIONS = listOf("No experience", "< 1 yr", "1-3 yrs", "3-5 yrs", "5+ yrs")

private val EDUCATION_OPTIONS = listOf(
    "No formal education", "Below 10th", "10th pass", "12th pass",
    "ITI / Diploma", "Graduate", "Any qualification"
)

private fun parseSkills(raw: String): List<String> =
    raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

/** Trims, strips commas, caps length, capitalizes words; maps known names to their canonical form. */
private fun normalizeSkillInput(raw: String): String {
    val cleaned = raw.replace(",", " ").trim().replace(Regex("\\s+"), " ")
        .take(MAX_CUSTOM_SKILL_LENGTH).trim()
    if (cleaned.isEmpty() || cleaned.equals(OTHERS_SKILL, ignoreCase = true)) return ""
    val known = (DEFAULT_SKILLS + EXTRA_SKILLS).firstOrNull { it.equals(cleaned, ignoreCase = true) }
    if (known != null) return known
    return cleaned.split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}

/** 13sp SemiBold gray label used for the sub-sections of a step. */
@Composable
private fun SubSectionHeader(title: String) {
    Text(
        text = title,
        color = StitchSubtitle,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun SkillsExperienceStep(
    skills: String,
    experience: String,
    educationQualification: String,
    skillsError: String?,
    experienceError: String?,
    onSkillsChange: (String) -> Unit,
    onExperienceChange: (String) -> Unit,
    onEducationChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            Text(
                text = "Skills & experience",
                color = StitchTitle,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "What you do, how long, and your background",
                color = StitchSubtitle,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column {
            SubSectionHeader("What are your skills?")
            SkillsPickerSection(
                skills = skills,
                skillsError = skillsError,
                onSkillsChange = onSkillsChange
            )
        }

        Column {
            SubSectionHeader("How much experience do you have?")
            ChipOptionsFlow(
                options = EXPERIENCE_OPTIONS,
                selected = experience,
                onSelect = onExperienceChange
            )
            if (experienceError != null) {
                Text(
                    text = experienceError,
                    color = WorkerColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(StitchBorder)
        )

        Column {
            SubSectionHeader("Your education (optional)")
            ChipOptionsFlow(
                options = EDUCATION_OPTIONS,
                selected = educationQualification,
                onSelect = onEducationChange
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipOptionsFlow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            StitchPill(
                text = option,
                selected = selected == option,
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun WorkerBioSection(
    workerBio: String,
    bioError: String?,
    onWorkerBioChange: (String) -> Unit
) {
    Column {
        SubSectionHeader("Short bio (optional)")
        OutlinedTextField(
            value = workerBio,
            onValueChange = { onWorkerBioChange(it.take(MAX_WORKER_BIO_LENGTH)) },
            placeholder = { Text(stringResource(R.string.worker_bio_hint)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            isError = bioError != null,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StitchInk,
                unfocusedBorderColor = StitchBorder,
                errorBorderColor = WorkerColors.Error,
                cursorColor = StitchInk
            ),
            supportingText = {
                Text(
                    text = bioError ?: "${workerBio.trim().length}/$MAX_WORKER_BIO_LENGTH characters",
                    color = if (bioError != null) WorkerColors.Error else StitchSubtitle
                )
            }
        )
    }
}

/**
 * Skill chips + search. Default chips come from DEFAULT_SKILLS; EXTRA_SKILLS only
 * surface via search; anything typed that doesn't match can be added as a custom
 * skill (chip or keyboard Done). Selection is stored in the same comma-joined string.
 */
@Composable
private fun SkillsPickerSection(
    skills: String,
    skillsError: String?,
    onSkillsChange: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAllSkills by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    val selected = parseSkills(skills)
    val selectedSet = selected.toSet()
    val query = searchQuery.trim()

    fun toggleSkill(name: String) {
        val updated = if (selected.any { it.equals(name, ignoreCase = true) }) {
            selected.filter { !it.equals(name, ignoreCase = true) }
        } else {
            selected + name
        }
        onSkillsChange(updated.joinToString(", "))
    }

    fun addSkill(name: String) {
        if (name.isNotBlank() && selected.none { it.equals(name, ignoreCase = true) }) {
            onSkillsChange((selected + name).joinToString(", "))
        }
        searchQuery = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkillSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            focusRequester = searchFocus,
            onDone = { addSkill(normalizeSkillInput(searchQuery)) }
        )

        val addedSkills = selected.filter { it !in DEFAULT_SKILLS }
        if (addedSkills.isNotEmpty()) {
            Text(
                text = "Added skills (tap to remove)",
                color = StitchLabel,
                fontSize = 12.sp
            )
            SkillChipsFlow(
                skills = addedSkills,
                selected = selectedSet,
                onSkillClick = { skill, _ -> toggleSkill(skill) }
            )
        }

        if (query.isNotEmpty()) {
            SkillSearchResults(
                query = query,
                selected = selectedSet,
                onPick = { addSkill(it) }
            )
        } else {
            DefaultSkillsGrid(
                showAll = showAllSkills,
                selected = selectedSet,
                onToggle = { toggleSkill(it) },
                onOthers = { searchFocus.requestFocus() },
                onShowAll = { showAllSkills = true }
            )
        }

        if (skillsError != null) {
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = WorkerColors.Error,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = skillsError,
                    color = WorkerColors.Error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SkillSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                "Search or type your own skill...",
                color = StitchSubtitle,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = StitchSubtitle)
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = StitchInk,
            unfocusedBorderColor = StitchBorder,
            cursorColor = StitchInk
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillSearchResults(
    query: String,
    selected: Set<String>,
    onPick: (String) -> Unit
) {
    val allSkills = (DEFAULT_SKILLS + EXTRA_SKILLS).filter { it != OTHERS_SKILL }
    val matches = allSkills.filter { it.contains(query, ignoreCase = true) }
    val typed = normalizeSkillInput(query)
    val alreadyKnown = typed.isEmpty() ||
        allSkills.any { it.equals(typed, ignoreCase = true) } ||
        selected.any { it.equals(typed, ignoreCase = true) }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        matches.forEach { skill ->
            StitchPill(
                text = skill,
                selected = selected.contains(skill),
                leadingIcon = skillIcon(skill),
                onClick = { onPick(skill) }
            )
        }
        if (!alreadyKnown) {
            StitchPill(
                text = "Add \"$typed\"",
                selected = false,
                leadingIcon = Icons.Default.Add,
                onClick = { onPick(typed) }
            )
        }
    }
    if (matches.isEmpty() && alreadyKnown) {
        Text(
            text = "\"$query\" is already added",
            color = StitchSubtitle,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun DefaultSkillsGrid(
    showAll: Boolean,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onOthers: () -> Unit,
    onShowAll: () -> Unit
) {
    val visible = if (showAll) DEFAULT_SKILLS else DEFAULT_SKILLS.take(DEFAULT_VISIBLE_SKILLS)
    val remainingCount = DEFAULT_SKILLS.size - visible.size
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SkillChipsFlow(
            skills = visible,
            selected = selected,
            onSkillClick = { skill, _ ->
                if (skill == OTHERS_SKILL) onOthers() else onToggle(skill)
            }
        )
        if (remainingCount > 0) {
            Text(
                text = "$remainingCount more skills →",
                color = StitchSubtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowAll)
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderSelectionField(
    selectedGender: String,
    onGenderSelected: (String) -> Unit
) {
    val genderOptions = listOf("Male", "Female")
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.auto_gender),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
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
                        color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
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

private fun parseDobToUtcMillis(value: String): Long? {
    return runCatching {
        LocalDate.parse(value, DOB_FORMATTER).toUtcMillis()
    }.getOrNull()
}

private fun formatDobFromUtcMillis(value: Long): String {
    return utcMillisToLocalDate(value).format(DOB_FORMATTER)
}

private fun utcMillisToLocalDate(value: Long): LocalDate {
    return Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate()
}

private fun LocalDate.toUtcMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}


