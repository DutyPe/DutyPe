package com.example.dutype.worker.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.example.dutype.profile.ExperienceBucket
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.firestore.FirestoreSchema.WorkerProfiles
import com.dutype.app.R
import com.example.dutype.components.ProfileSetupReferralCard
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
                    .background(if (segment <= currentStep) StitchInk.bg() else StitchTrack.bg())
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
            .background(if (selected) StitchInk.bg() else StitchWhite.bg())
            .border(
                width = 1.dp,
                color = if (selected) StitchInk.fg() else StitchBorder.fg(),
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
                    tint = if (selected) StitchWhite.fg() else StitchTitle.fg(),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (selected && leadingCheck) {
                Text(
                    text = "✓ ",
                    color = StitchWhite.fg(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = text,
                color = if (selected) StitchWhite.fg() else StitchTitle.fg(),
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
        color = StitchLabel.fg(),
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
    val totalSteps = 2  // Streamlined 2-step setup: Basic Info + Skills & Experience

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
                profileCompletionViewModel.getWorker(currentUser.uid).getOrNull()?.let { saved ->
                    if (fullName.isBlank() && !saved.name.equals("User", ignoreCase = true)) fullName = saved.name
                    if (phoneNumber.isBlank()) phoneNumber = saved.phone.removePrefix("+91").trim()
                    if (skills.isBlank() && saved.skills.isNotEmpty()) {
                        skills = saved.skills.joinToString(", ") { JobCategory.fromKey(it).displayName }
                        // Experience is only meaningful once the profile was filled in before.
                        if (experience.isBlank()) experience = ExperienceBucket.forYears(saved.experienceYears).label
                    }
                    if (dateOfBirth.isBlank()) dateOfBirth = saved.dateOfBirth
                    if (gender.isBlank() && (saved.gender == "Male" || saved.gender == "Female")) gender = saved.gender
                    if (educationQualification.isBlank()) educationQualification = saved.education
                    if (workerBio.isBlank()) workerBio = saved.bio
                    if (address.isBlank()) address = saved.address
                    if (saved.photoUrl.isNotBlank()) selfieUrl = saved.photoUrl
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
                        if (googleName != null && !googleName.equals("User", ignoreCase = true)) {
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
                        if (!cachedName.isNullOrBlank() && !cachedName.equals("User", ignoreCase = true)) {
                            fullName = cachedName
                            Timber.d("📦 PREFILL: fullName from registration cache = $fullName")
                        }
                    }
                }

                "TRUECALLER" -> {
                    // Truecaller Flow: Prefill verified phone, optional email, and name (editable)
                    if (phoneNumber.isBlank()) {
                        val tcPhone = profileCompletionViewModel.getPhoneNumber()
                        if (tcPhone != null) {
                            phoneNumber = tcPhone.replace("+91", "").trim()
                            Timber.d("📦 PREFILL: phoneNumber from Truecaller = $phoneNumber")
                        }
                    }
                    if (email.isBlank()) {
                        val tcEmail = profileCompletionViewModel.getUserEmail()
                        if (!tcEmail.isNullOrBlank()) {
                            email = tcEmail
                            Timber.d("📦 PREFILL: email from Truecaller = $email")
                        }
                    }
                    if (fullName.isBlank()) {
                        val tcName = profileCompletionViewModel.getUserName()
                        if (!tcName.isNullOrBlank() && !tcName.equals("User", ignoreCase = true)) {
                            fullName = tcName
                            Timber.d("📦 PREFILL: fullName from Truecaller = $fullName")
                        }
                    }
                }

                else -> {
                    Timber.w("Unknown auth method: $authMethod")
                    isEmailLoaded = true
                }
            }

            if (fullName.trim().equals("User", ignoreCase = true)) {
                fullName = ""
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


    // Step 1 (Basic information): name (*), gender (*), optional email, optional bio, optional referral code
    val isStep1Valid = fullName.trim().isNotBlank() &&
        (gender == "Male" || gender == "Female" || gender.isNotBlank()) &&
        (email.isBlank() || ValidationUtils.isValidEmail(email))
    // Step 2 (Skills & experience): at least one skill and an experience level.
    val isStep2Valid = skills.isNotBlank() && experience.isNotBlank()
    val isStep3Valid = true

    // Overall form validation
    val isFormValid = isStep1Valid && isStep2Valid

    // Current step validation
    val isCurrentStepValid = when (currentStep) {
        1 -> isStep1Valid
        2 -> isStep2Valid
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
    LaunchedEffect(fullName, email, gender, skills, experience, workerBio, currentStep, isCurrentStepValid, showValidationErrors) {
        // Only update error messages when user tries to proceed (showValidationErrors = true)
        if (showValidationErrors) {
            // Update email error
            emailError = when {
                email.isNotBlank() && !ValidationUtils.isValidEmail(email) -> "Enter a valid email address"
                else -> null
            }

            // Update full name error
            fullNameError = when {
                fullName.trim().isBlank() -> "Full name is required"
                else -> null
            }

            // Update gender error
            genderError = when {
                gender != "Male" && gender != "Female" && gender.isBlank() -> "Gender is required"
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
            dateOfBirthError = null
            genderError = null
            skillsError = null
            experienceError = null
            bioError = null
        }

        Timber.d("Form validation - step=$currentStep, step1Valid=$isStep1Valid, step2Valid=$isStep2Valid, currentValid=$isCurrentStepValid")
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
                    color = WorkerColors.Primary.fg()
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

                            // Step 1: Basic information (name, gender, email, short bio, referral)
                            if (currentStep == 1) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                                ) {
                                    PersonalInformationStep(
                                        profileCompletionViewModel = profileCompletionViewModel,
                                        fullName = fullName,
                                        email = email,
                                        gender = gender,
                                        workerBio = workerBio,
                                        fullNameError = if (showValidationErrors) fullNameError else null,
                                        genderError = if (showValidationErrors) genderError else null,
                                        bioError = if (showValidationErrors) bioError else null,
                                        emailError = if (showValidationErrors) emailError else null,
                                        onFullNameChange = { fullName = it },
                                        onGenderChange = { gender = it },
                                        onEmailChange = { email = it },
                                        onWorkerBioChange = { workerBio = it }
                                    )
                                }
                            }

                            // Step 2: Skills & experience (skills, experience)
                            if (currentStep == 2) {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically() + fadeIn(),
                                    exit = slideOutVertically() + fadeOut()
                                ) {
                                    SkillsExperienceStep(
                                        skills = skills,
                                        experience = experience,
                                        skillsError = if (showValidationErrors) skillsError else null,
                                        experienceError = if (showValidationErrors) experienceError else null,
                                        onSkillsChange = { skills = it },
                                        onExperienceChange = { experience = it }
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
                                contentColor = WorkerColors.Primary.fg()
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                Color(0xFF1F2937).copy(alpha = 0.3f)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
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
                                                                selfieError = context.getString(R.string.worker_setup_photo_failed_toast)
                                                            }
                                                        )
                                                    } catch (e: Exception) {
                                                        Timber.e(e, "📸 Exception during selfie upload")
                                                        selfieError = context.getString(R.string.worker_setup_photo_failed_toast)
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

                                                val workerProfileData = mutableMapOf<String, Any?>(
                                                    WorkerProfiles.NAME to fullName.trim(),
                                                    WorkerProfiles.ADDRESS to address.trim(),
                                                    WorkerProfiles.SKILLS to skillKeys(skills),
                                                    WorkerProfiles.DATE_OF_BIRTH to dateOfBirth,
                                                    WorkerProfiles.GENDER to gender,
                                                    WorkerProfiles.EXPERIENCE_YEARS to
                                                        (ExperienceBucket.entries.firstOrNull { it.label == experience }?.years ?: 0),
                                                    WorkerProfiles.EDUCATION to educationQualification.trim(),
                                                    WorkerProfiles.BIO to workerBio.trim(),
                                                    WorkerProfiles.AVAILABLE to true
                                                )
                                                val savedLocation = profileCompletionViewModel.locationPreferences.getSavedLocation()
                                                    ?: profileCompletionViewModel.locationPreferences.currentLocation.value
                                                var finalLat = savedLocation?.latitude ?: 0.0
                                                var finalLng = savedLocation?.longitude ?: 0.0

                                                if (!com.example.dutype.utils.GeoUtils.hasValidCoordinates(finalLat, finalLng) && address.isNotBlank()) {
                                                    try {
                                                        val geocoded = locationService.getCoordinatesFromAddress(address.trim())
                                                        if (geocoded != null && com.example.dutype.utils.GeoUtils.hasValidCoordinates(geocoded.latitude, geocoded.longitude)) {
                                                            finalLat = geocoded.latitude
                                                            finalLng = geocoded.longitude
                                                            profileCompletionViewModel.locationPreferences.saveLocation(locationService.toLocationData(geocoded))
                                                        }
                                                    } catch (e: Exception) {
                                                        Timber.w(e, "Could not geocode worker address: $address")
                                                    }
                                                }

                                                if (com.example.dutype.utils.GeoUtils.hasValidCoordinates(finalLat, finalLng)) {
                                                    workerProfileData[WorkerProfiles.LAT] = finalLat
                                                    workerProfileData[WorkerProfiles.LNG] = finalLng
                                                }
                                                // The photo URL was saved by uploadProfileImage itself.
                                                if (uploadedSelfieUrl != null) Timber.d("Worker photo saved")
                                                profileCompletionViewModel.saveWorker(workerProfileData).getOrThrow()
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
                                            errorMessage = e.message ?: context.getString(R.string.worker_setup_failed_generic)
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
                            containerColor = if (isCurrentStepValid) StitchInk.bg() else WorkerColors.TextDisabled,
                            disabledContainerColor = WorkerColors.TextDisabled
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = StitchWhite.fg(),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when (currentStep) {
                                    1 -> stringResource(R.string.worker_setup_next_skills)
                                    else -> stringResource(R.string.worker_setup_complete_profile)
                                },
                                color = StitchWhite.fg(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
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
    profileCompletionViewModel: ProfileCompletionViewModel,
    fullName: String,
    email: String,
    gender: String,
    workerBio: String,
    fullNameError: String?,
    genderError: String?,
    bioError: String?,
    emailError: String?,
    onFullNameChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onWorkerBioChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Step header
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.worker_setup_basic_info_title),
                color = StitchTitle.fg(),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.worker_setup_basic_info_sub),
                color = StitchSubtitle.fg(),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        FullNameField(fullName = fullName, error = fullNameError, onChange = onFullNameChange)

        // GENDER
        Column {
            StitchSectionLabel(stringResource(R.string.worker_setup_section_gender) + " *")
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

        EmailField(email = email, error = emailError, onChange = onEmailChange)

        WorkerBioSection(
            workerBio = workerBio,
            bioError = bioError,
            onWorkerBioChange = onWorkerBioChange
        )

        ProfileSetupReferralCard(
            role = UserRole.WORKER,
            profileCompletionViewModel = profileCompletionViewModel
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
                    stringResource(R.string.full_name) + " *",
                    color = StitchFieldLabel.fg(),
                    fontSize = 12.sp
                )
            },
            placeholder = { Text(stringResource(R.string.enter_full_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = StitchTitle.fg()
            ),
            shape = RoundedCornerShape(14.dp),
            isError = error != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) WorkerColors.Error else StitchInk.bd(),
                unfocusedBorderColor = if (error != null) WorkerColors.Error else StitchBorder.bd(),
                focusedLabelColor = if (error != null) WorkerColors.Error else StitchFieldLabel.fg(),
                unfocusedLabelColor = StitchFieldLabel.fg(),
                errorBorderColor = WorkerColors.Error,
                cursorColor = StitchInk.fg()
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

@Composable
private fun EmailField(email: String, error: String?, onChange: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = email,
            onValueChange = onChange,
            label = {
                Text(
                    stringResource(R.string.email_address),
                    color = StitchFieldLabel.fg(),
                    fontSize = 12.sp
                )
            },
            placeholder = { Text(stringResource(R.string.enter_email)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = StitchTitle.fg()
            ),
            shape = RoundedCornerShape(14.dp),
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) WorkerColors.Error else StitchInk.bd(),
                unfocusedBorderColor = if (error != null) WorkerColors.Error else StitchBorder.bd(),
                focusedLabelColor = if (error != null) WorkerColors.Error else StitchFieldLabel.fg(),
                unfocusedLabelColor = StitchFieldLabel.fg(),
                errorBorderColor = WorkerColors.Error,
                cursorColor = StitchInk.fg()
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
        val dashColor = StitchBorder.fg()
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
                    tint = StitchSubtitle.fg(),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.worker_setup_add_photo_optional),
            color = StitchSubtitle.fg(),
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
    var isContactExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StitchBorder.bd(), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isContactExpanded = !isContactExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.worker_setup_section_contact_details),
                    color = StitchTitle.fg(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!isContactExpanded && phoneNumber.isNotBlank()) {
                    Text(
                        text = "+91 $phoneNumber",
                        color = StitchSubtitle.fg(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Icon(
                imageVector = if (isContactExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = StitchSubtitle.fg(),
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = isContactExpanded) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                } else if (authMethod == "PHONE_OTP" || authMethod == "TRUECALLER") {
                    // OTP / Truecaller Auth: Email is optional and editable
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
                                focusedBorderColor = if (emailError != null) WorkerColors.Error else WorkerColors.Primary.bd(),
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
                                focusedBorderColor = if (emailError != null) WorkerColors.Error else WorkerColors.Primary.bd(),
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
                if (authMethod == "PHONE_OTP" || authMethod == "TRUECALLER") {
                    // Phone is prefilled and verified
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
                                contentDescription = "Verified",
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
                                focusedBorderColor = if (phoneError != null) WorkerColors.Error else WorkerColors.Primary.bd(),
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
            fetchError = context.getString(R.string.worker_setup_could_not_get_location)
            if (!silent) {
                Toast.makeText(
                    context,
                    R.string.worker_setup_could_not_get_location,
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
                text = stringResource(R.string.worker_setup_where_based_title),
                color = StitchTitle.fg(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.worker_setup_where_based_sub),
                color = StitchSubtitle.fg(),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            onClick = {
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
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = StitchAccent.fg()
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFetchingLocation) stringResource(R.string.worker_setup_detecting) else stringResource(R.string.worker_setup_detect),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchAccent.fg()
                    )
                }
            }

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
                    focusedBorderColor = if (addressError != null) WorkerColors.Error else WorkerColors.Primary.bd(),
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
            LocationStatusRow(
                isFetching = isFetchingLocation,
                fetchError = fetchError,
                showDetected = detectedAddress != null && detectedAddress == address,
                detectedArea = detectedArea
            )
        }
    }
}

/** Status line under the address field showing detection status. */
@Composable
private fun LocationStatusRow(
    isFetching: Boolean,
    fetchError: String?,
    showDetected: Boolean,
    detectedArea: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isFetching) {
            Text(
                text = stringResource(R.string.worker_setup_detecting_location),
                color = StitchSubtitle.fg(),
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
                tint = StitchSuccess.fg(),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.worker_setup_location_detected),
                color = StitchSuccess.fg(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (detectedArea != null) {
                Text(
                    text = "  ·  $detectedArea",
                    color = StitchSubtitle.fg(),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
            val label = when (option.lowercase()) {
                "male" -> stringResource(R.string.gender_male)
                "female" -> stringResource(R.string.gender_female)
                else -> option
            }
            StitchPill(
                text = label,
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
        skills.filter { !it.equals(OTHERS_SKILL, ignoreCase = true) && !it.equals("other", ignoreCase = true) && !it.equals("others", ignoreCase = true) }
            .forEach { skill ->
                val isSelected = selected.any { it.equals(skill, ignoreCase = true) }
                StitchPill(
                    text = skill,
                    selected = isSelected,
                    leadingCheck = isSelected,
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
        focused -> StitchInk.fg()
        else -> StitchBorder.bd()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.date_of_birth) + " *",
            color = StitchFieldLabel.fg(),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.worker_setup_dob_subtitle),
            color = StitchLabel.fg(),
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
            .background(StitchWhite.bg())
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
                color = StitchTitle.fg()
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
                            color = StitchLabel.fg(),
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
                contentDescription = stringResource(R.string.worker_setup_pick_dob_calendar),
                tint = StitchSubtitle.fg(),
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
        colors = DatePickerDefaults.colors(containerColor = Color.White.bg()),
        confirmButton = {
            TextButton(
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0F0F0F).fg()),
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
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0F0F0F).fg())
            ) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = state,
            showModeToggle = false,
            colors = whiteDatePickerColors()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun whiteDatePickerColors(): DatePickerColors {
    val ink = Color(0xFF0F0F0F).fg()
    val muted = Color(0xFF64748B).fg()
    return DatePickerDefaults.colors(
        containerColor = Color.White.bg(),
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
        dividerColor = Color(0xFFE2E8F0).bd(),
        dateTextFieldColors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.bg(),
            unfocusedContainerColor = Color.White.bg(),
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
    JobCategory.COOK, JobCategory.MAID, JobCategory.DRIVER, JobCategory.DELIVERY, JobCategory.HELPER,
    JobCategory.CARETAKER, JobCategory.TAILOR, JobCategory.PLUMBER, JobCategory.ELECTRICIAN,
    JobCategory.PAINTER, JobCategory.GARDENER, JobCategory.SECURITY
).map { it.displayName }.filter { !it.equals("Others", ignoreCase = true) && !it.equals("Other", ignoreCase = true) }

/** Extra skills that are NOT shown as chips by default; found through the search bar. */
private val EXTRA_SKILLS = JobCategory.entries
    .filter { it != JobCategory.OTHER && !it.displayName.equals("Others", ignoreCase = true) && !it.displayName.equals("Other", ignoreCase = true) }
    .map { it.displayName }
    .filter { it !in DEFAULT_SKILLS }

private const val OTHERS_SKILL = "Others"
private const val DEFAULT_VISIBLE_SKILLS = 9
private const val MAX_CUSTOM_SKILL_LENGTH = 30

private val EXPERIENCE_OPTIONS = ExperienceBucket.entries.map { it.label }

private fun parseSkills(raw: String): List<String> =
    raw.split(",").map { it.trim() }.filter { it.isNotEmpty() && !it.equals("Others", ignoreCase = true) && !it.equals("Other", ignoreCase = true) }.distinct()

/** Trims, strips commas, caps length, capitalizes words; maps known names or title-cases custom input. */
private fun normalizeSkillInput(raw: String): String {
    val cleaned = raw.replace(",", " ").trim().replace(Regex("\\s+"), " ")
        .take(MAX_CUSTOM_SKILL_LENGTH).trim()
    if (cleaned.isEmpty() || cleaned.equals(OTHERS_SKILL, ignoreCase = true) || cleaned.equals("other", ignoreCase = true) || cleaned.equals("others", ignoreCase = true)) return ""
    val known = (DEFAULT_SKILLS + EXTRA_SKILLS).firstOrNull { it.equals(cleaned, ignoreCase = true) }
    if (known != null) return known
    return cleaned.split(" ").filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}

/** "Cook, Driver, Welder" → ["COOK", "DRIVER", "WELDER"]. Custom skills fall back to normalized uppercase token. */
private fun skillKeys(raw: String): List<String> = parseSkills(raw).map { name ->
    JobCategory.entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }?.name
        ?: name.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_').ifBlank { "OTHER" }
}

/** 13sp SemiBold gray label used for the sub-sections of a step. */
@Composable
private fun SubSectionHeader(title: String) {
    Text(
        text = title,
        color = StitchSubtitle.fg(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun SkillsExperienceStep(
    skills: String,
    experience: String,
    skillsError: String?,
    experienceError: String?,
    onSkillsChange: (String) -> Unit,
    onExperienceChange: (String) -> Unit
) {
    var isExperienceExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.worker_setup_skills_exp_title),
                color = StitchTitle.fg(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.worker_setup_skills_exp_sub),
                color = StitchSubtitle.fg(),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column {
            SubSectionHeader(stringResource(R.string.worker_setup_what_are_skills) + " *")
            SkillsPickerSection(
                skills = skills,
                skillsError = skillsError,
                onSkillsChange = onSkillsChange
            )
        }

        // Experience Accordion
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StitchBorder.bd(), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExperienceExpanded = !isExperienceExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.worker_setup_how_much_exp) + " *",
                        color = StitchTitle.fg(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!isExperienceExpanded && experience.isNotBlank()) {
                        Text(
                            text = experience,
                            color = StitchAccent.fg(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExperienceExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = StitchSubtitle.fg(),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExperienceExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
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
            }
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
            val label = when (option) {
                "Fresher" -> stringResource(R.string.exp_fresher)
                "1-2 years" -> stringResource(R.string.exp_1_to_2_years)
                "3-5 years" -> stringResource(R.string.exp_3_to_5_years)
                "5+ years" -> stringResource(R.string.exp_5_plus_years)
                else -> option
            }
            StitchPill(
                text = label,
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
        SubSectionHeader(stringResource(R.string.worker_setup_bio_optional))
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
                focusedBorderColor = StitchInk.bd(),
                unfocusedBorderColor = StitchBorder.bd(),
                errorBorderColor = WorkerColors.Error,
                cursorColor = StitchInk.fg()
            ),
            supportingText = {
                Text(
                    text = bioError ?: stringResource(
                        R.string.worker_setup_bio_char_count,
                        workerBio.trim().length,
                        MAX_WORKER_BIO_LENGTH
                    ),
                    color = if (bioError != null) WorkerColors.Error else StitchSubtitle.fg()
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
@OptIn(ExperimentalLayoutApi::class)
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
    val query = searchQuery.trim()

    fun isSelected(skill: String): Boolean =
        selected.any { it.equals(skill, ignoreCase = true) }

    fun toggleSkill(name: String) {
        val cleanName = normalizeSkillInput(name).ifBlank { name.trim() }
        if (cleanName.isBlank()) return
        val updated = if (isSelected(cleanName)) {
            selected.filter { !it.equals(cleanName, ignoreCase = true) }
        } else {
            selected + cleanName
        }
        onSkillsChange(updated.joinToString(", "))
    }

    fun addSkill(name: String) {
        val cleanName = normalizeSkillInput(name).ifBlank { name.trim() }
        if (cleanName.isNotBlank() && !isSelected(cleanName)) {
            onSkillsChange((selected + cleanName).joinToString(", "))
        }
        searchQuery = ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SkillSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            focusRequester = searchFocus,
            onDone = { addSkill(searchQuery) }
        )

        // 1. Prominently display all selected skills as selected chips
        if (selected.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Selected Skills (${selected.size})",
                    color = StitchTitle.fg(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selected.forEach { skill ->
                        StitchPill(
                            text = skill,
                            selected = true,
                            leadingCheck = true,
                            leadingIcon = skillIcon(skill),
                            onClick = { toggleSkill(skill) }
                        )
                    }
                }
            }
        }

        // 2. Search Results or Default Skills Grid
        if (query.isNotEmpty()) {
            SkillSearchResults(
                query = query,
                isSelected = { isSelected(it) },
                onPick = { addSkill(it) }
            )
        } else {
            DefaultSkillsGrid(
                showAll = showAllSkills,
                isSelected = { isSelected(it) },
                onToggle = { toggleSkill(it) },
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
                stringResource(R.string.worker_setup_search_skill_placeholder),
                color = StitchSubtitle.fg(),
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = StitchSubtitle.fg())
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = onDone) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add skill",
                        tint = StitchAccent.fg()
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = StitchInk.bd(),
            unfocusedBorderColor = StitchBorder.bd(),
            cursorColor = StitchInk.fg()
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillSearchResults(
    query: String,
    isSelected: (String) -> Boolean,
    onPick: (String) -> Unit
) {
    val allSkills = (DEFAULT_SKILLS + EXTRA_SKILLS).filter {
        !it.equals(OTHERS_SKILL, ignoreCase = true) && !it.equals("other", ignoreCase = true)
    }
    val matches = allSkills.filter { it.contains(query, ignoreCase = true) }
    val typed = normalizeSkillInput(query).ifBlank { query.trim() }
    val alreadyKnown = typed.isEmpty() ||
        allSkills.any { it.equals(typed, ignoreCase = true) } ||
        isSelected(typed)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Search Results",
            color = StitchSubtitle.fg(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            matches.forEach { skill ->
                val selected = isSelected(skill)
                StitchPill(
                    text = skill,
                    selected = selected,
                    leadingCheck = selected,
                    leadingIcon = skillIcon(skill),
                    onClick = { onPick(skill) }
                )
            }
            if (!alreadyKnown && typed.isNotBlank()) {
                StitchPill(
                    text = "+ Add \"$typed\"",
                    selected = false,
                    leadingIcon = Icons.Default.Add,
                    onClick = { onPick(typed) }
                )
            }
        }
        if (matches.isEmpty() && alreadyKnown && isSelected(query)) {
            Text(
                text = stringResource(R.string.worker_setup_skill_already_added, query),
                color = StitchSubtitle.fg(),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DefaultSkillsGrid(
    showAll: Boolean,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit,
    onShowAll: () -> Unit
) {
    val visible = if (showAll) DEFAULT_SKILLS else DEFAULT_SKILLS.take(DEFAULT_VISIBLE_SKILLS)
    val remainingCount = DEFAULT_SKILLS.size - visible.size
    val filtered = visible.filter { !it.equals(OTHERS_SKILL, ignoreCase = true) && !it.equals("other", ignoreCase = true) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SkillChipsFlow(
            skills = filtered,
            selected = filtered.filter { isSelected(it) }.toSet(),
            onSkillClick = { skill, _ -> onToggle(skill) }
        )
        if (remainingCount > 0) {
            Text(
                text = stringResource(R.string.worker_setup_more_skills, remainingCount),
                color = StitchSubtitle.fg(),
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
                            selectedColor = Color(0xFF111111).fg(),
                            unselectedColor = Color(0xFFD1D5DB).fg()
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when (gender.lowercase()) {
                            "male" -> stringResource(R.string.gender_male)
                            "female" -> stringResource(R.string.gender_female)
                            else -> gender
                        },
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


