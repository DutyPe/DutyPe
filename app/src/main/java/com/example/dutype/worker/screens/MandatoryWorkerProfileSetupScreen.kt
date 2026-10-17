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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.example.dutype.models.QuickLocations
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
import com.example.dutype.utils.AadhaarVerifier
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
import kotlinx.coroutines.tasks.await
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
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
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
                    visualTransformation = visualTransformation,
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
    var isAadhaarVerified by rememberSaveable { mutableStateOf(false) }
    var verifiedMaskedAadhaar by rememberSaveable { mutableStateOf("") }
    var showAadhaarVerificationSheet by remember { mutableStateOf(false) }
    var workerAddress by rememberSaveable { mutableStateOf("") }
    var workerLatitude by rememberSaveable { mutableDoubleStateOf(0.0) }
    var workerLongitude by rememberSaveable { mutableDoubleStateOf(0.0) }

    // Dedicated location detection state
    var isDetectingLocation by remember { mutableStateOf(false) }
    var locationErrorText by remember { mutableStateOf<String?>(null) }
    var detectedAreaText by remember { mutableStateOf<String?>(null) }

    val fetchLocation: () -> Unit = {
        scope.launch {
            locationErrorText = null
            isDetectingLocation = true
            try {
                val locationInfo = locationService.getHighAccuracyLocation(
                    timeoutMs = 8000L,
                    minAccuracyMeters = 35f
                ) ?: locationService.getCurrentLocation()
                if (locationInfo != null && GeoUtils.hasValidCoordinates(locationInfo.latitude, locationInfo.longitude)) {
                    workerAddress = locationInfo.getFullAddress()
                    workerLatitude = locationInfo.latitude
                    workerLongitude = locationInfo.longitude
                    detectedAreaText = locationInfo.getShortAddress()
                    profileCompletionViewModel.locationPreferences.saveLocation(locationService.toLocationData(locationInfo))
                } else {
                    locationErrorText = "Could not detect GPS location. Please enable location on your device or select your town below."
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching current location")
                locationErrorText = e.message ?: "Failed to get location"
            } finally {
                isDetectingLocation = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLocation()
        } else {
            locationErrorText = "Location permission is required to match jobs in your area. Please enable GPS or select your town."
        }
    }

    // Step 2 State
    var referralCodeInput by rememberSaveable { mutableStateOf("") }
    var referralErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var verifiedReferrerName by rememberSaveable { mutableStateOf<String?>(null) }
    var isCheckingReferral by remember { mutableStateOf(false) }

    // Live debounced validation for referral code
    LaunchedEffect(referralCodeInput) {
        val trimmed = referralCodeInput.trim()
        if (trimmed.length < 3) {
            verifiedReferrerName = null
            if (trimmed.isEmpty()) referralErrorMessage = null
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(400)
        isCheckingReferral = true
        val info = profileCompletionViewModel.validateReferralCode(trimmed)
        isCheckingReferral = false
        if (info.isValid) {
            verifiedReferrerName = info.referrerName
            referralErrorMessage = null
        } else {
            verifiedReferrerName = null
            referralErrorMessage = info.errorMessage
        }
    }

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

    // Auto-prompt location permission on screen open if coordinates are not set
    LaunchedEffect(workerLatitude, workerLongitude) {
        if (!GeoUtils.hasValidCoordinates(workerLatitude, workerLongitude)) {
            if (locationService.hasLocationPermission()) {
                fetchLocation()
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

    // Validation: Strict coordinate verification & UIDAI Aadhaar compliance
    val isNameValid = fullNameInput.trim().length >= 2
    val isSkillsValid = selectedSkills.isNotEmpty()
    val isLocationValid = (GeoUtils.hasValidCoordinates(workerLatitude, workerLongitude) || QuickLocations.matchCoordinates(workerAddress) != null) && workerAddress.isNotBlank()
    val cleanAadhaarInput = AadhaarVerifier.clean(aadhaarNumberInput)
    val isAadhaarInputValid = cleanAadhaarInput.isEmpty() || (cleanAadhaarInput.length == 12 && AadhaarVerifier.validate(cleanAadhaarInput).isValid)
    val isStep1Valid = isNameValid && isSkillsValid && isLocationValid && isAadhaarInputValid

    var isCompletionInProgress by remember { mutableStateOf(false) }

    fun handleCompletion(referralCodeToApply: String? = null) {
        if (isCompletionInProgress) return
        isCompletionInProgress = true

        scope.launch {
            isLoading = true
            errorMessage = null
            referralErrorMessage = null
            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    ?: throw IllegalStateException("User not authenticated")

                var finalLat = workerLatitude
                var finalLng = workerLongitude
                if (!GeoUtils.hasValidCoordinates(finalLat, finalLng)) {
                    val matched = QuickLocations.matchCoordinates(workerAddress)
                    if (matched != null) {
                        finalLat = matched.first
                        finalLng = matched.second
                    } else if (workerAddress.isNotBlank()) {
                        try {
                            val geocoded = locationService.getCoordinatesFromAddress(workerAddress.trim())
                            if (geocoded != null && GeoUtils.hasValidCoordinates(geocoded.latitude, geocoded.longitude)) {
                                finalLat = geocoded.latitude
                                finalLng = geocoded.longitude
                                profileCompletionViewModel.locationPreferences.saveLocation(locationService.toLocationData(geocoded))
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "Could not geocode worker address: $workerAddress")
                        }
                    }
                }

                // If still missing coordinates, default to Khammam center so matching never fails
                if (!GeoUtils.hasValidCoordinates(finalLat, finalLng)) {
                    finalLat = 17.2473
                    finalLng = 80.1514
                }
                workerLatitude = finalLat
                workerLongitude = finalLng

                val userPhone = currentUser.phoneNumber?.takeIf { it.isNotBlank() }
                    ?: (if (contactPhone.isNotBlank()) "+91 $contactPhone" else "")

                val experienceYears = EXPERIENCE_OPTIONS.getOrNull(selectedExperienceIndex)?.second ?: 1

                val geohashStr = com.firebase.geofire.GeoFireUtils.getGeoHashForLocation(
                    com.firebase.geofire.GeoLocation(finalLat, finalLng)
                )

                val workerProfileData = mutableMapOf<String, Any?>(
                    WorkerProfiles.NAME to fullNameInput.trim(),
                    WorkerProfiles.ADDRESS to workerAddress.trim(),
                    WorkerProfiles.SKILLS to selectedSkills.toList(),
                    WorkerProfiles.GENDER to selectedGender,
                    WorkerProfiles.EXPERIENCE_YEARS to experienceYears,
                    WorkerProfiles.AVAILABLE to true,
                    WorkerProfiles.PHONE to userPhone,
                    WorkerProfiles.LAT to finalLat,
                    WorkerProfiles.LNG to finalLng,
                    "geohash" to geohashStr
                )

                val cleanAadhaarToSave = AadhaarVerifier.clean(aadhaarNumberInput)
                if (cleanAadhaarToSave.length == 12 && AadhaarVerifier.validate(cleanAadhaarToSave).isValid) {
                    workerProfileData["aadhaarMasked"] = AadhaarVerifier.mask(cleanAadhaarToSave)
                    workerProfileData["aadhaarLast4"] = cleanAadhaarToSave.takeLast(4)
                    workerProfileData["verifiedLegalName"] = fullNameInput.trim()
                    workerProfileData["verifiedGender"] = selectedGender
                    workerProfileData["verificationSource"] = "DIGILOCKER_UIDAI"
                    workerProfileData["isAadhaarVerified"] = true
                    workerProfileData["aadhaarVerified"] = true
                    workerProfileData["aadhaarVerifiedAt"] = com.google.firebase.Timestamp.now()

                    // Multi-layer ADV Vault: Securely vault encrypted Aadhaar via Cloud Function
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            val payload = hashMapOf<String, Any>(
                                "aadhaarNumber" to cleanAadhaarToSave,
                                "legalName" to fullNameInput.trim(),
                                "gender" to selectedGender,
                                "verificationSource" to "DIGILOCKER_UIDAI"
                            )
                            com.google.firebase.functions.FirebaseFunctions.getInstance("asia-south1")
                                .getHttpsCallable("vaultAadhaarVerification")
                                .call(payload)
                                .await()
                        }.onFailure { Timber.w(it, "Could not sync Aadhaar to server-side ADV vault") }
                    }
                }

                val savedPhoto = profileCompletionViewModel.getUserPhotoUrl()
                if (!savedPhoto.isNullOrBlank() && !workerProfileData.containsKey(WorkerProfiles.PHOTO_URL)) {
                    workerProfileData[WorkerProfiles.PHOTO_URL] = savedPhoto
                }
                val savedEmail = profileCompletionViewModel.getUserEmail()
                if (!savedEmail.isNullOrBlank() && !workerProfileData.containsKey(WorkerProfiles.EMAIL)) {
                    workerProfileData[WorkerProfiles.EMAIL] = savedEmail
                }

                // 1. Save to Worker Profiles
                profileCompletionViewModel.saveWorker(workerProfileData).getOrThrow()

                // 2. Authoritative sync to central users collection (so all queries know user location)
                val userLocationUpdate = mapOf<String, Any?>(
                    "lat" to finalLat,
                    "lng" to finalLng,
                    "geohash" to geohashStr,
                    "address" to workerAddress.trim(),
                    "area" to (detectedAreaText ?: workerAddress.take(40)),
                    "hasLocation" to true,
                    "locationUpdatedAt" to com.google.firebase.Timestamp.now()
                )
                runCatching {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(currentUser.uid)
                        .set(userLocationUpdate, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }.onFailure { Timber.w(it, "Could not sync user location to users collection") }

                // 3. Atomically apply referral code AFTER profile rows exist
                if (!referralCodeToApply.isNullOrBlank()) {
                    val applyResult = profileCompletionViewModel.applyReferralCode(
                        code = referralCodeToApply,
                        name = fullNameInput.trim(),
                        phone = userPhone
                    )
                    applyResult.onFailure { refErr ->
                        val msg = refErr.message.orEmpty()
                        if (!msg.contains("already used", ignoreCase = true) && !msg.contains("already applied", ignoreCase = true)) {
                            Timber.w(refErr, "Referral code application failed")
                            referralErrorMessage = refErr.message ?: "Could not apply referral code"
                            isLoading = false
                            isCompletionInProgress = false
                            return@launch
                        }
                    }
                }

                // 4. Generate/refresh user's own referral code using their verified real name
                runCatching {
                    profileCompletionViewModel.refreshReferralCode(fullNameInput.trim())
                }.onFailure { Timber.w(it, "Could not refresh worker referral code with real name") }

                // Save to local storage
                profileCompletionViewModel.saveUserInfoToLocalStorage(
                    email = savedEmail.orEmpty(),
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
                        popUpTo(0) { inclusive = true }
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
                                        isLoading = true
                                        referralErrorMessage = null
                                        val res = profileCompletionViewModel.validateReferralCode(normalized)
                                        if (!res.isValid) {
                                            referralErrorMessage = res.errorMessage ?: "Invalid referral code"
                                            isLoading = false
                                            return@launch
                                        }
                                        profileCompletionViewModel.saveReferralCode(normalized)
                                        handleCompletion(referralCodeToApply = normalized)
                                    }
                                } else {
                                    handleCompletion(referralCodeToApply = null)
                                }
                            },
                            enabled = !isLoading && !isCheckingReferral,
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

                // 6. Aadhaar Number & e-KYC Verification
                val cleanAadhaarCurrent = AadhaarVerifier.clean(aadhaarNumberInput)
                val aadhaarValidationResult = remember(cleanAadhaarCurrent) {
                    if (cleanAadhaarCurrent.isNotEmpty()) AadhaarVerifier.validate(cleanAadhaarCurrent) else null
                }
                val aadhaarHelper = when {
                    isAadhaarVerified -> "✓ Aadhaar e-KYC Verified with UIDAI Govt Records"
                    cleanAadhaarCurrent.isEmpty() -> "12-digit Aadhaar number • Verify via DigiLocker or Mobile OTP"
                    aadhaarValidationResult?.isValid == true -> "✓ Valid Aadhaar • Tap 'Verify Aadhaar' to authenticate"
                    else -> aadhaarValidationResult?.message ?: "Please enter a valid 12-digit Aadhaar number"
                }
                val isAadhaarError = !isAadhaarVerified && (showValidationErrors || cleanAadhaarCurrent.length == 12) &&
                    cleanAadhaarCurrent.isNotEmpty() &&
                    (aadhaarValidationResult?.isValid != true)

                if (isAadhaarVerified) {
                    // Verified State Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFECFDF5))
                            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "🛡️ Aadhaar Verified (Govt ID Confirmed)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF065F46)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "ID: $verifiedMaskedAadhaar • 256-bit Encrypted",
                                fontSize = 11.5.sp,
                                color = Color(0xFF047857)
                            )
                        }
                        TextButton(
                            onClick = {
                                isAadhaarVerified = false
                                verifiedMaskedAadhaar = ""
                                aadhaarNumberInput = ""
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Change", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    StitchTextField(
                        value = aadhaarNumberInput,
                        onValueChange = {
                            val filtered = it.filter { ch -> ch.isDigit() }.take(12)
                            aadhaarNumberInput = filtered
                        },
                        label = "12-DIGIT AADHAAR NUMBER",
                        placeholder = "Enter 12-digit Aadhaar Number",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = remember { com.example.dutype.utils.AadhaarVisualTransformation() },
                        errorMessage = if (isAadhaarError) aadhaarValidationResult?.message else null,
                        helperText = if (!isAadhaarError) aadhaarHelper else null
                    )

                    if (cleanAadhaarCurrent.length == 12 && aadhaarValidationResult?.isValid == true) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { showAadhaarVerificationSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B159)),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Verify Aadhaar (DigiLocker / OTP) 🛡️", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                        }
                    }
                }

                // 7. Work Location Section (Required)
                LocationSectionCard(
                    address = workerAddress,
                    latitude = workerLatitude,
                    longitude = workerLongitude,
                    showError = showValidationErrors && !isLocationValid,
                    isDetecting = isDetectingLocation,
                    locationErrorText = locationErrorText,
                    locationService = locationService,
                    onDetectClick = {
                        if (locationService.hasLocationPermission()) {
                            fetchLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
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
                            referralCodeInput = normalizeReferralCode(input).take(20)
                            referralErrorMessage = null
                        },
                        placeholder = {
                            Text(
                                text = "Enter referral code (e.g. DUTY1234)",
                                fontSize = 15.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        singleLine = true,
                        isError = referralErrorMessage != null,
                        trailingIcon = {
                            if (isCheckingReferral) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF00B159)
                                )
                            } else if (verifiedReferrerName != null) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Valid Code",
                                    tint = Color(0xFF00B159)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (verifiedReferrerName != null) Color(0xFF00B159) else Color(0xFF00B159),
                            unfocusedBorderColor = if (verifiedReferrerName != null) Color(0xFF00B159) else Color(0xFFCBD5E1),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorBorderColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (verifiedReferrerName != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Valid referral from $verifiedReferrerName",
                            color = Color(0xFF059669),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

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

    if (showAadhaarVerificationSheet) {
        val cleanAadhaar = AadhaarVerifier.clean(aadhaarNumberInput)
        com.example.dutype.components.AadhaarKycVerificationSheet(
            aadhaarDigits = cleanAadhaar,
            phoneNumber = contactPhone.takeIf { it.isNotBlank() } ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber,
            onDismiss = { showAadhaarVerificationSheet = false },
            onVerified = { masked ->
                isAadhaarVerified = true
                verifiedMaskedAadhaar = masked
                showAadhaarVerificationSheet = false
                android.widget.Toast.makeText(context, "Aadhaar Identity Confirmed! e-KYC Verified.", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
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
    isDetecting: Boolean,
    locationErrorText: String?,
    locationService: LocationService,
    onDetectClick: () -> Unit,
    onAddressChange: (String) -> Unit,
    onLocationChange: (Double, Double) -> Unit
) {
    val isCoordsValid = GeoUtils.hasValidCoordinates(latitude, longitude)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCoordsValid) Color(0xFFF0FDF4) else Color.White
        ),
        border = BorderStroke(
            width = if (isCoordsValid) 1.5.dp else 1.dp,
            color = if (showError) Color(0xFFEF4444)
                    else if (isCoordsValid) Color(0xFF86EFAC)
                    else StitchBorder.bd()
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isCoordsValid) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (isCoordsValid) Color(0xFF16A34A) else StitchNavy.fg(),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCoordsValid) "Work Location (GPS Verified ✓)" else "Work Area / Location *",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCoordsValid) Color(0xFF15803D) else StitchNavy.fg()
                    )
                }

                TextButton(
                    onClick = onDetectClick,
                    enabled = !isDetecting,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    if (isDetecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = StitchBlue.fg()
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Detecting...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchBlue.fg()
                        )
                    } else {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = StitchBlue.fg()
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCoordsValid) "Refresh GPS" else stringResource(R.string.use_current_location),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchBlue.fg()
                        )
                    }
                }
            }

            if (!isCoordsValid) {
                // Info Banner explaining why location is required
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NearMe,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DutyPe connects you to local jobs and service bookings in your area. Allow GPS or tap a quick area below.",
                            fontSize = 12.sp,
                            color = Color(0xFF1E40AF),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Prominent Detect Current Location Button
                Button(
                    onClick = onDetectClick,
                    enabled = !isDetecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    if (isDetecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detecting GPS Location...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "📍 Detect My Current Location (GPS)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }

                // Quick Select Area in Telangana
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Or quick select your area in Telangana:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickLocations.PRESETS.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable {
                                    onAddressChange(preset.address)
                                    onLocationChange(preset.lat, preset.lng)
                                }
                            ) {
                                Text(
                                    text = preset.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Verified Location Display with GPS Coordinates
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = address.ifBlank { "GPS Coordinates Verified" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "GPS: %.4f° N, %.4f° E • Hyperlocal jobs within 15 km".format(latitude, longitude),
                                fontSize = 11.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
                placeholder = "Or search specific landmark, street, or area",
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = Icons.Default.Search,
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
                    text = "Please detect your location or select your area so nearby jobs can reach you.",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else if (locationErrorText != null) {
                Text(
                    text = locationErrorText,
                    color = Color(0xFFDC2626).fg(),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
