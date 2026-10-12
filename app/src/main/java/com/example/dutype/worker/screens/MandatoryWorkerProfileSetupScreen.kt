package com.example.dutype.worker.screens

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.LocationAutocompleteField
import com.example.dutype.components.markWelcomeCelebrationPending
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.firestore.FirestoreSchema.WorkerProfiles
import com.example.dutype.models.UserRole
import com.example.dutype.models.normalizeReferralCode
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.LocalRoleColors
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.LocationService
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.launch
import timber.log.Timber

// =============================================================================
// STITCH DESIGN SPEC PALETTE
// =============================================================================
private val StitchNavy = Color(0xFF0F172A)
private val StitchTrack = Color(0xFFF1F5F9)
private val StitchBorder = Color(0xFFE2E8F0)
private val StitchBlue = Color(0xFF2563EB)
private val StitchLabel = Color(0xFF64748B)
private val StitchDisabledBg = Color(0xFFF8FAFC)
private val StitchFieldValue = Color(0xFF0F172A)

/**
 * Generic Stitch-style outlined text field matching Employer Setup.
 */
@Composable
private fun StitchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    helperText: String? = null,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    backgroundColor: Color = Color.White,
    valueColor: Color = StitchFieldValue,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(backgroundColor, RoundedCornerShape(14.dp))
                .border(
                    width = 1.dp,
                    color = if (errorMessage != null) Color(0xFFEF4444) else StitchBorder.bd(),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(12.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    readOnly = readOnly,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = valueColor
                    ),
                    keyboardOptions = keyboardOptions,
                    cursorBrush = SolidColor(Color(0xFF00A859)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingContent()
            }
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFEF4444),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else if (helperText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                color = StitchLabel,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

private val AVAILABLE_TRADES = listOf(
    "Electrician",
    "Plumber",
    "Painter",
    "Carpenter",
    "Welder",
    "Driver",
    "Delivery Partner",
    "Cook / Chef",
    "Cleaner / Maid",
    "Security Guard",
    "Tailor",
    "Construction Worker",
    "Factory Worker",
    "Helper / Peon",
    "AC Technician",
    "Beautician"
)

private val EXPERIENCE_OPTIONS = listOf(
    "Fresher (<1 yr)" to 0,
    "1 - 3 Years" to 2,
    "3 - 5 Years" to 4,
    "5+ Years" to 5
)

private val GENDER_OPTIONS = listOf("Male", "Female", "Other")

/**
 * Clean 2-Step Mandatory Worker Profile Setup Screen.
 * Designed symmetrically with MandatoryEmployerProfileSetupScreen.
 *
 * Step 1: Full Name, Read-only Verified Phone, Gender, Trade Skills chips, Experience,
 *         Optional 12-digit Aadhaar Number (No OTP verification), and Interactive Location Map.
 * Step 2: Referral Code Entry with img_referral_hands, Continue & Skip options.
 */
@Composable
fun MandatoryWorkerProfileSetupScreen(
    navController: NavController,
    returnRoute: String? = null,
    profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reviewTriggerService = rememberInAppReviewTriggerService()
    val locationService = profileCompletionViewModel.locationService
    val notificationService = profileCompletionViewModel.notificationService
    val fcmTokenManager = profileCompletionViewModel.fcmTokenManager

    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    var isLoadingExistingData by rememberSaveable { mutableStateOf(true) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

    // Step 1 State
    var fullNameInput by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var selectedGender by rememberSaveable { mutableStateOf("Male") }
    var selectedSkills by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedExperienceIndex by rememberSaveable { mutableIntStateOf(1) }
    var aadhaarNumberInput by rememberSaveable { mutableStateOf("") }
    var workerAddress by rememberSaveable { mutableStateOf("") }
    var workerLatitude by rememberSaveable { mutableDoubleStateOf(0.0) }
    var workerLongitude by rememberSaveable { mutableDoubleStateOf(0.0) }

    // Step 2 State
    var referralCodeInput by rememberSaveable { mutableStateOf("") }
    var referralErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Pre-fill existing data
    LaunchedEffect(Unit) {
        isLoadingExistingData = true
        try {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                profileCompletionViewModel.getWorker(currentUser.uid).getOrNull()?.let { existing ->
                    if (existing.name.isNotBlank() && !existing.name.equals("User", ignoreCase = true)) {
                        fullNameInput = existing.name.trim()
                    }
                    if (existing.address.isNotBlank()) workerAddress = existing.address
                    if (existing.skills.isNotEmpty()) selectedSkills = existing.skills.toSet()
                    if (existing.gender.isNotBlank()) selectedGender = existing.gender
                    if (contactPhone.isBlank() && existing.phone.isNotBlank()) {
                        contactPhone = existing.phone.removePrefix("+91").trim()
                    }
                    if (GeoUtils.hasValidCoordinates(existing.lat, existing.lng)) {
                        workerLatitude = existing.lat
                        workerLongitude = existing.lng
                    }
                }
            }

            if (fullNameInput.isBlank()) {
                val savedName = profileCompletionViewModel.getUserName()
                if (savedName != null && !savedName.equals("User", ignoreCase = true)) {
                    fullNameInput = savedName.trim()
                }
            }

            if (contactPhone.isBlank()) {
                val savedPhone = profileCompletionViewModel.getPhoneNumber()
                if (savedPhone != null) {
                    contactPhone = savedPhone.replace("+91", "").trim()
                }
            }

            val savedRef = profileCompletionViewModel.getReferralCode()
            if (!savedRef.isNullOrBlank()) {
                referralCodeInput = savedRef.trim()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error prefilling worker profile setup")
        } finally {
            isLoadingExistingData = false
        }
    }

    // Validation
    val isNameValid = fullNameInput.trim().length >= 2
    val isSkillsValid = selectedSkills.isNotEmpty()
    val isLocationValid = workerAddress.isNotBlank() || GeoUtils.hasValidCoordinates(workerLatitude, workerLongitude)
    val isStep1Valid = isNameValid && isSkillsValid && isLocationValid

    var isCompletionInProgress by remember { mutableStateOf(false) }

    fun handleCompletion() {
        if (isCompletionInProgress) return
        isCompletionInProgress = true

        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    ?: throw IllegalStateException("User not authenticated")

                var finalLat = workerLatitude
                var finalLng = workerLongitude
                if (!GeoUtils.hasValidCoordinates(finalLat, finalLng) && workerAddress.isNotBlank()) {
                    try {
                        val geocoded = locationService.getCoordinatesFromAddress(workerAddress.trim())
                        if (geocoded != null && GeoUtils.hasValidCoordinates(geocoded.latitude, geocoded.longitude)) {
                            finalLat = geocoded.latitude
                            finalLng = geocoded.longitude
                            workerLatitude = finalLat
                            workerLongitude = finalLng
                            profileCompletionViewModel.locationPreferences.saveLocation(locationService.toLocationData(geocoded))
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Could not geocode worker address: $workerAddress")
                    }
                }

                val userPhone = currentUser.phoneNumber?.takeIf { it.isNotBlank() }
                    ?: (if (contactPhone.isNotBlank()) "+91 $contactPhone" else "")

                val experienceYears = EXPERIENCE_OPTIONS.getOrNull(selectedExperienceIndex)?.second ?: 1

                val workerProfileData = mutableMapOf<String, Any?>(
                    WorkerProfiles.NAME to fullNameInput.trim(),
                    WorkerProfiles.ADDRESS to workerAddress.trim(),
                    WorkerProfiles.SKILLS to selectedSkills.toList(),
                    WorkerProfiles.GENDER to selectedGender,
                    WorkerProfiles.EXPERIENCE_YEARS to experienceYears,
                    WorkerProfiles.AVAILABLE to true,
                    WorkerProfiles.PHONE to userPhone
                )

                if (aadhaarNumberInput.trim().length >= 12) {
                    workerProfileData["aadhaarNumber"] = aadhaarNumberInput.trim()
                }

                if (GeoUtils.hasValidCoordinates(finalLat, finalLng)) {
                    workerProfileData[WorkerProfiles.LAT] = finalLat
                    workerProfileData[WorkerProfiles.LNG] = finalLng
                    workerProfileData["geohash"] = com.firebase.geofire.GeoFireUtils.getGeoHashForLocation(
                        com.firebase.geofire.GeoLocation(finalLat, finalLng)
                    )
                }

                // Save to Firestore
                profileCompletionViewModel.saveWorker(workerProfileData).getOrThrow()

                // Save to local storage
                profileCompletionViewModel.saveUserInfoToLocalStorage(
                    name = fullNameInput.trim(),
                    role = UserRole.WORKER
                )

                val wasAlreadyComplete = profileCompletionViewModel.isProfileComplete(UserRole.WORKER)
                profileCompletionViewModel.markProfileComplete(UserRole.WORKER)
                profileCompletionViewModel.markProfileSetupAsShown(UserRole.WORKER)

                if (!wasAlreadyComplete) {
                    markWelcomeCelebrationPending(context)
                    scope.launch {
                        runCatching {
                            notificationService.sendProfileCompleteNotification(
                                userName = fullNameInput.trim(),
                                userId = currentUser.uid,
                                userRole = "WORKER"
                            )
                            fcmTokenManager.registerTokenWithRole("WORKER")
                        }.onFailure { Timber.e(it, "Notification/FCM background register failed") }
                    }
                    val activity = context as? Activity
                    if (activity != null) {
                        reviewTriggerService.onWorkerProfileCompleted(activity)
                    }
                }

                // Navigate to next screen
                if (returnRoute != null) {
                    Timber.d("📍 Navigating to return route '$returnRoute'")
                    navController.navigate(returnRoute) {
                        popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    Timber.d("📍 Navigating to WORKER_HOME")
                    navController.navigate(Routes.WORKER_HOME) {
                        popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to complete worker profile")
                errorMessage = e.message ?: context.getString(R.string.failed_to_complete_profile_setup)
            } finally {
                isLoading = false
                isCompletionInProgress = false
            }
        }
    }

    if (isLoadingExistingData) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalRoleColors.current.screenBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Color(0xFF00B159))
                Text(stringResource(R.string.loading_your_profile), color = Color.Gray)
            }
        }
        return
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            if (currentStep == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (navController.previousBackStackEntry != null) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = Color(0xFF0F172A)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(40.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Step 1 of 2",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }

                    // 2-segment progress bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF00B159))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFE2E8F0))
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color(0xFF0F172A)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentStep == 1) {
                        Button(
                            onClick = {
                                showValidationErrors = true
                                if (isStep1Valid) {
                                    currentStep = 2
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStep1Valid) Color(0xFF00B159) else Color(0xFFCBD5E1),
                                disabledContainerColor = Color(0xFFCBD5E1)
                            )
                        ) {
                            Text(
                                text = "Continue to Next Step →",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val rawCode = referralCodeInput.trim()
                                if (rawCode.isNotBlank()) {
                                    val normalized = normalizeReferralCode(rawCode)
                                    scope.launch {
                                        referralErrorMessage = null
                                        val res = profileCompletionViewModel.validateReferralCode(normalized)
                                        if (res.isValid) {
                                            profileCompletionViewModel.saveReferralCode(normalized)
                                            runCatching { profileCompletionViewModel.applyReferralCode(normalized) }
                                            handleCompletion()
                                        } else {
                                            referralErrorMessage = res.errorMessage ?: "Invalid referral code"
                                        }
                                    }
                                } else {
                                    handleCompletion()
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00B159),
                                disabledContainerColor = Color(0xFFCBD5E1)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "Continue",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        TextButton(
                            onClick = { handleCompletion() },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Skip for now",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF00B159)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentStep == 1) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    val annotatedTitle = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF111827), fontWeight = FontWeight.Bold, fontSize = 26.sp)) {
                            append("Help us ")
                        }
                        withStyle(SpanStyle(color = Color(0xFF00A859), fontWeight = FontWeight.Bold, fontSize = 26.sp)) {
                            append("know you")
                        }
                        withStyle(SpanStyle(color = Color(0xFF111827), fontWeight = FontWeight.Bold, fontSize = 26.sp)) {
                            append(" better")
                        }
                    }

                    Text(text = annotatedTitle, lineHeight = 34.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Basic profile details & your skills",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // 1. Full Name (Required)
                StitchTextField(
                    value = fullNameInput,
                    onValueChange = { fullNameInput = it },
                    placeholder = "Full name *",
                    errorMessage = if (showValidationErrors && !isNameValid) "Please enter your full name" else null
                )

                // 2. Mobile Number (Verified badge)
                if (contactPhone.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "+91 $contactPhone",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFFECFDF5), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Verified",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Verified",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                // 3. Gender Pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "GENDER *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchLabel,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GENDER_OPTIONS.forEach { gender ->
                            val isSelected = selectedGender.equals(gender, ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { selectedGender = gender },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = gender,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Primary Skills Selectable Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SELECT YOUR SKILLS *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchLabel,
                        letterSpacing = 0.5.sp
                    )
                    if (showValidationErrors && !isSkillsValid) {
                        Text(
                            text = "Please select at least 1 skill",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }

                    // Multi-row skill chips
                    OptInTradeChips(
                        trades = AVAILABLE_TRADES,
                        selected = selectedSkills,
                        onToggle = { trade ->
                            selectedSkills = if (trade in selectedSkills) {
                                selectedSkills - trade
                            } else {
                                selectedSkills + trade
                            }
                        }
                    )
                }

                // 5. Experience Selector Pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "WORK EXPERIENCE *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchLabel,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EXPERIENCE_OPTIONS.forEachIndexed { index, (label, _) ->
                            val isSelected = selectedExperienceIndex == index
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { selectedExperienceIndex = index },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF00A859) else Color.White,
                                border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Aadhaar Number (Optional - NO OTP verification)
                StitchTextField(
                    value = aadhaarNumberInput,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isDigit() }.take(12)
                        aadhaarNumberInput = filtered
                    },
                    placeholder = "12-digit Aadhaar Number (optional)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    helperText = "For worker badge & identity with employers (no OTP required)"
                )

                // 7. Work Location Section (Required)
                LocationSectionCard(
                    address = workerAddress,
                    latitude = workerLatitude,
                    longitude = workerLongitude,
                    showError = showValidationErrors && !isLocationValid,
                    locationService = locationService,
                    onAddressChange = { workerAddress = it },
                    onLocationChange = { lat, lng ->
                        workerLatitude = lat
                        workerLongitude = lng
                    }
                )
            } else {
                // Step 2: Referral Code Entry (Matches Image 3)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_handshake_partnership),
                        contentDescription = "Referral Partnership",
                        modifier = Modifier
                            .size(170.dp)
                            .padding(bottom = 12.dp),
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        text = "Enter referral code",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "It's optional, you can skip this",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    OutlinedTextField(
                        value = referralCodeInput,
                        onValueChange = { input ->
                            referralCodeInput = normalizeReferralCode(input).take(12)
                            referralErrorMessage = null
                        },
                        placeholder = {
                            Text(
                                text = "Enter referral code",
                                fontSize = 15.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        singleLine = true,
                        isError = referralErrorMessage != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B159),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorBorderColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (referralErrorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = referralErrorMessage ?: "",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptInTradeChips(
    trades: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    val chunked = remember(trades) { trades.chunked(3) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunked.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { trade ->
                    val isSelected = trade in selected
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable { onToggle(trade) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF00B159) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = trade,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF065F46) else Color(0xFF334155),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
                // Fill missing cells if row has fewer than 3
                for (i in 0 until (3 - row.size)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LocationSectionCard(
    address: String,
    latitude: Double,
    longitude: Double,
    showError: Boolean,
    locationService: LocationService,
    onAddressChange: (String) -> Unit,
    onLocationChange: (Double, Double) -> Unit
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
            isFetchingLocation = true
            try {
                val locationInfo = locationService.getHighAccuracyLocation(
                    timeoutMs = 8000L,
                    minAccuracyMeters = 35f
                ) ?: locationService.getCurrentLocation()
                if (locationInfo != null) {
                    onAddressChange(locationInfo.getFullAddress())
                    onLocationChange(locationInfo.latitude, locationInfo.longitude)
                    detectedAreaText = locationInfo.getShortAddress()
                } else {
                    locationErrorText = context.getString(R.string.could_not_get_current_location)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching current location")
                locationErrorText = e.message ?: "Failed to get location"
            } finally {
                isFetchingLocation = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            if (manualLocationRequest || address.isBlank()) {
                runFetch()
            }
        } else if (manualLocationRequest) {
            locationErrorText = context.getString(R.string.location_permission_fetch_address)
        }
        manualLocationRequest = false
    }

    LaunchedEffect(Unit) {
        if (!autoLocationAsked) {
            autoLocationAsked = true
            if (locationService.hasLocationPermission()) {
                if (address.isBlank()) runFetch()
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, StitchBorder.bd())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Work Area / Location *",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchNavy.fg()
                )

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
                        modifier = Modifier.size(15.dp),
                        tint = StitchBlue.fg()
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.use_current_location),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchBlue.fg()
                    )
                }
            }

            LocationAutocompleteField(
                value = address,
                onValueChange = onAddressChange,
                onLocationSelected = { selectedAddress, lat, lng ->
                    onAddressChange(selectedAddress)
                    onLocationChange(lat, lng)
                },
                locationService = locationService,
                label = null,
                placeholder = "City, Area or Landmark *",
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = Icons.Default.LocationOn,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.bg(),
                    unfocusedContainerColor = Color.White.bg(),
                    focusedBorderColor = if (showError) Color(0xFFEF4444) else Color(0xFF00A859),
                    unfocusedBorderColor = if (showError) Color(0xFFEF4444) else StitchBorder.bd(),
                    errorBorderColor = Color(0xFFEF4444)
                )
            )

            if (showError) {
                Text(
                    text = "Please select or enter your location",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (isFetchingLocation) {
                Text(
                    text = stringResource(R.string.fetching_your_location),
                    color = StitchLabel.fg(),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else if (locationErrorText != null) {
                Text(
                    text = locationErrorText ?: "",
                    color = Color(0xFFDC2626).fg(),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else if (detectedAreaText != null) {
                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF16A34A).fg(),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.worker_setup_location_detected),
                        color = Color(0xFF16A34A).fg(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " · ${detectedAreaText ?: ""}",
                        color = StitchLabel.fg(),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
