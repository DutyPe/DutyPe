package com.example.dutype.employer.screens

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.ProfileSetupReferralCard
import com.example.dutype.components.markWelcomeCelebrationPending
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.ui.theme.LocalRoleColors
import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.viewmodels.ProfileCompletionViewModel

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.dutype.models.normalizeReferralCode
import androidx.compose.ui.text.SpanStyle
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
 * Generic Stitch-style outlined field: white/gray rounded box with a small
 * gray label pinned at the top, a bold value line below it, and optional helper/error notes.
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
                    color = if (errorMessage != null) EmployerColors.Error else StitchBorder.bd(),
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
                color = EmployerColors.Error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else if (helperText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                color = StitchLabel.fg(),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Mandatory Employer Profile Setup Screen (Single Screen)
 *
 * Streamlined single-screen setup capturing:
 * - Full Name (required)
 * - Email (optional)
 * - Shop / Business Name (optional: "give or ignore", can be provided during normal job posting)
 * - Verified locked phone number
 * - Location (GPS auto-fetch + autocomplete + compact map preview)
 * - Referral Code card
 *
 * @param navController Navigation controller for screen navigation
 * @param returnRoute Optional route to navigate to after profile completion
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
    val locationService = profileCompletionViewModel.locationService
    val notificationService = profileCompletionViewModel.notificationService
    val fcmTokenManager = profileCompletionViewModel.fcmTokenManager

    // Form state
    var fullNameInput by rememberSaveable { mutableStateOf("") }
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    var email by rememberSaveable { mutableStateOf("") }
    var shopOrBusinessName by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var businessAddress by rememberSaveable { mutableStateOf("") }
    var businessLatitude by rememberSaveable { mutableStateOf(0.0) }
    var businessLongitude by rememberSaveable { mutableStateOf(0.0) }

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
                    businessAddress = locationInfo.getFullAddress()
                    businessLatitude = locationInfo.latitude
                    businessLongitude = locationInfo.longitude
                    detectedAreaText = locationInfo.getShortAddress()
                    profileCompletionViewModel.locationPreferences.saveLocation(locationService.toLocationData(locationInfo))
                } else {
                    locationErrorText = "Could not detect GPS location. Please turn on device location or select your town below."
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
            locationErrorText = "Location permission is required to post jobs and hire workers near you. Please enable GPS or select your town."
        }
    }

    val fullName = fullNameInput.trim()

    // Validation & loading states
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingExistingData by remember { mutableStateOf(true) }
    var referralCodeInput by rememberSaveable { mutableStateOf("") }
    var referralErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var verifiedReferrerName by rememberSaveable { mutableStateOf<String?>(null) }
    var isCheckingReferral by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

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

    // Pre-fill existing data from Firebase & local cache
    LaunchedEffect(Unit) {
        isLoadingExistingData = true
        try {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                profileCompletionViewModel.getEmployer(currentUser.uid).getOrNull()?.let { saved ->
                    var loadedName = ""
                    if (saved.ownerName.isNotBlank() && !saved.ownerName.equals("User", ignoreCase = true)) {
                        loadedName = saved.ownerName
                    } else if (saved.businessName.isNotBlank() && !saved.businessName.equals("User", ignoreCase = true)) {
                        loadedName = saved.businessName
                    }
                    if (loadedName.isNotBlank()) {
                        fullNameInput = loadedName.trim()
                    }
                    if (saved.businessName.isNotBlank() &&
                        !saved.businessName.equals("User", ignoreCase = true) &&
                        saved.businessName != saved.ownerName
                    ) {
                        shopOrBusinessName = saved.businessName
                    }
                    if (contactPhone.isBlank()) contactPhone = saved.phone.removePrefix("+91").trim()
                    if (businessAddress.isBlank()) businessAddress = saved.address
                    if (businessLatitude == 0.0 && businessLongitude == 0.0 &&
                        GeoUtils.hasValidCoordinates(saved.lat, saved.lng)
                    ) {
                        businessLatitude = saved.lat
                        businessLongitude = saved.lng
                    }
                }

                // Fallback: load name from local state if still empty
                if (fullNameInput.isBlank()) {
                    val savedName = profileCompletionViewModel.getUserName()
                    if (savedName != null && !savedName.equals("User", ignoreCase = true)) {
                        fullNameInput = savedName.trim()
                    }
                }
                // Pre-fill email if available
                if (email.isBlank()) {
                    val savedEmail = profileCompletionViewModel.getUserEmail()
                    if (!savedEmail.isNullOrBlank()) {
                        email = savedEmail
                    }
                }
                // Pre-fill phone from auth if available
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
            }
        } catch (e: Exception) {
            Timber.e(e, "Error prefilling employer profile setup")
        } finally {
            isLoadingExistingData = false
        }
    }

    // Auto-prompt location permission on screen open if coordinates are not set
    LaunchedEffect(businessLatitude, businessLongitude) {
        if (!GeoUtils.hasValidCoordinates(businessLatitude, businessLongitude)) {
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

    // Validation
    val isNameValid = fullNameInput.trim().length >= 2
    val isEmailValid = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val isLocationValid = (GeoUtils.hasValidCoordinates(businessLatitude, businessLongitude) || QuickLocations.matchCoordinates(businessAddress) != null) && businessAddress.isNotBlank()
    val isFormValid = isNameValid && isEmailValid && isLocationValid

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

                // Resolve coordinates if missing but address text is present
                var finalLat = businessLatitude
                var finalLng = businessLongitude
                if (!GeoUtils.hasValidCoordinates(finalLat, finalLng)) {
                    val matched = QuickLocations.matchCoordinates(businessAddress)
                    if (matched != null) {
                        finalLat = matched.first
                        finalLng = matched.second
                    } else if (businessAddress.isNotBlank()) {
                        try {
                            val geocoded = locationService.getCoordinatesFromAddress(businessAddress.trim())
                            if (geocoded != null && GeoUtils.hasValidCoordinates(geocoded.latitude, geocoded.longitude)) {
                                finalLat = geocoded.latitude
                                finalLng = geocoded.longitude
                                profileCompletionViewModel.locationPreferences.saveLocation(locationService.toLocationData(geocoded))
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "Could not geocode employer address: $businessAddress")
                        }
                    }
                }

                // If still missing coordinates, default to Khammam center so matching never fails
                if (!GeoUtils.hasValidCoordinates(finalLat, finalLng)) {
                    finalLat = 17.2473
                    finalLng = 80.1514
                }
                businessLatitude = finalLat
                businessLongitude = finalLng

                val hasShop = shopOrBusinessName.isNotBlank()
                val userPhone = currentUser.phoneNumber?.takeIf { it.isNotBlank() }
                    ?: (if (contactPhone.isNotBlank()) "+91 $contactPhone" else "")

                val geohashStr = com.firebase.geofire.GeoFireUtils.getGeoHashForLocation(
                    com.firebase.geofire.GeoLocation(finalLat, finalLng)
                )

                val employerProfileData = mutableMapOf<String, Any?>(
                    EmployerProfiles.EMPLOYER_TYPE to (if (hasShop) Values.EmployerType.COMPANY else Values.EmployerType.INDIVIDUAL),
                    EmployerProfiles.OWNER_NAME to fullName.trim(),
                    EmployerProfiles.BUSINESS_NAME to if (hasShop) shopOrBusinessName.trim() else "",
                    EmployerProfiles.PHONE to userPhone,
                    EmployerProfiles.ADDRESS to businessAddress.trim(),
                    EmployerProfiles.LAT to finalLat,
                    EmployerProfiles.LNG to finalLng,
                    "geohash" to geohashStr
                )
                if (email.isNotBlank()) {
                    employerProfileData[EmployerProfiles.EMAIL] = email.trim()
                }
                val savedPhoto = profileCompletionViewModel.getUserPhotoUrl()
                if (!savedPhoto.isNullOrBlank()) {
                    employerProfileData[EmployerProfiles.PHOTO_URL] = savedPhoto
                }

                // 1. Save employer document in Firestore
                profileCompletionViewModel.saveEmployer(employerProfileData).getOrThrow()

                // 2. Authoritative sync to central users collection
                val userLocationUpdate = mapOf<String, Any?>(
                    "lat" to finalLat,
                    "lng" to finalLng,
                    "geohash" to geohashStr,
                    "address" to businessAddress.trim(),
                    "area" to (detectedAreaText ?: businessAddress.take(40)),
                    "hasLocation" to true,
                    "locationUpdatedAt" to com.google.firebase.Timestamp.now()
                )
                runCatching {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(currentUser.uid)
                        .set(userLocationUpdate, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }.onFailure { Timber.w(it, "Could not sync employer location to users collection") }

                // 3. Atomically apply referral code AFTER profile rows exist
                if (!referralCodeToApply.isNullOrBlank()) {
                    val applyResult = profileCompletionViewModel.applyReferralCode(
                        code = referralCodeToApply,
                        name = fullName.trim(),
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

                // 4. Generate/refresh employer's own referral code using their verified real name / business name
                runCatching {
                    val realName = if (hasShop) shopOrBusinessName.trim() else fullName.trim()
                    profileCompletionViewModel.refreshReferralCode(realName)
                }.onFailure { Timber.w(it, "Could not refresh employer referral code with real name") }

                // Save to employer work_locations collection and saved addresses
                if (businessAddress.isNotBlank()) {
                    runCatching {
                        val workLocRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection(EmployerProfiles.COLLECTION)
                            .document(currentUser.uid)
                            .collection(EmployerProfiles.WorkLocations.COLLECTION)

                        val locLabel = if (hasShop) shopOrBusinessName.trim() else "Main Office"
                        workLocRef.document("loc_primary").set(
                            mapOf(
                                EmployerProfiles.WorkLocations.LABEL to locLabel,
                                EmployerProfiles.WorkLocations.ADDRESS to businessAddress.trim(),
                                EmployerProfiles.WorkLocations.LAT to finalLat,
                                EmployerProfiles.WorkLocations.LNG to finalLng
                            )
                        ).await()

                        val savedPrefs = context.getSharedPreferences("dutype_saved_addresses", Context.MODE_PRIVATE)
                        val tag = if (hasShop) "shop" else "office"
                        savedPrefs.edit()
                            .putString("addr_$tag", businessAddress.trim())
                            .putString("area_$tag", locLabel)
                            .putString("lat_$tag", finalLat.toString())
                            .putString("lng_$tag", finalLng.toString())
                            .apply()
                    }.onFailure { Timber.w(it, "Could not save primary work location") }
                }

                // Save to local storage for instant cache
                profileCompletionViewModel.saveUserInfoToLocalStorage(
                    email = email.trim(),
                    name = fullName.trim(),
                    role = UserRole.EMPLOYER
                )

                val wasAlreadyComplete = profileCompletionViewModel.isProfileComplete(UserRole.EMPLOYER)
                profileCompletionViewModel.markProfileComplete(UserRole.EMPLOYER)
                profileCompletionViewModel.markProfileSetupAsShown(UserRole.EMPLOYER)

                if (!wasAlreadyComplete) {
                    markWelcomeCelebrationPending(context)
                    scope.launch {
                        runCatching {
                            notificationService.sendProfileCompleteNotification(
                                userName = fullName.trim(),
                                userId = currentUser.uid,
                                userRole = "EMPLOYER"
                            )
                            fcmTokenManager.registerTokenWithRole("EMPLOYER")
                        }.onFailure { Timber.e(it, "Notification/FCM background register failed") }
                    }
                    val activity = context as? Activity
                    if (activity != null) {
                        reviewTriggerService.onEmployerProfileCompleted(activity)
                    }
                }

                // Navigate to next screen
                if (returnRoute != null) {
                    Timber.d("📍 Queuing return route '$returnRoute' and navigating to EMPLOYER_HOME")
                    com.example.dutype.navigation.EmployerInnerNavQueue.setPending(returnRoute)
                    navController.navigate(Routes.EMPLOYER_HOME) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    Timber.d("📍 Navigating to EMPLOYER_HOME")
                    navController.navigate(Routes.EMPLOYER_HOME) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to complete employer profile")
                errorMessage = e.message ?: context.getString(R.string.failed_to_complete_profile_setup)
            } finally {
                isLoading = false
                isCompletionInProgress = false
            }
        }
    }

    // Loading existing profile
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
                CircularProgressIndicator(color = EmployerColors.Primary.fg())
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
                                if (isFormValid) {
                                    currentStep = 2
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFormValid) Color(0xFF00B159) else Color(0xFFCBD5E1),
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
                // Header: "Help us know you better" (Clean Title, no mascot)
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

                    Text(
                        text = annotatedTitle,
                        lineHeight = 34.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Basic profile details & official hiring work location",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 1. Full Name (Required) - inside placeholder only
                StitchTextField(
                    value = fullNameInput,
                    onValueChange = { fullNameInput = it },
                    placeholder = "Full name *",
                    errorMessage = if (showValidationErrors && !isNameValid) "Please enter your full name" else null
                )

                // 2. Shop / Business Name (Optional) - inside placeholder only
                StitchTextField(
                    value = shopOrBusinessName,
                    onValueChange = { shopOrBusinessName = it },
                    placeholder = "Shop / Business name (optional)"
                )

                // 3. Email (Optional) - inside placeholder only
                StitchTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email (optional)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    errorMessage = if (showValidationErrors && !isEmailValid) "Please enter a valid email address" else null
                )

                // 4. Mobile Number (Verified badge)
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

                // 5. Work Location Section (Required)
                LocationSectionCard(
                    businessAddress = businessAddress,
                    businessLatitude = businessLatitude,
                    businessLongitude = businessLongitude,
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
                    onAddressChange = { businessAddress = it },
                    onLocationChange = { lat, lng ->
                        businessLatitude = lat
                        businessLongitude = lng
                    }
                )
            } else {
                // Step 2: Referral Code Entry (Matches Image 3 exactly)
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
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            cursorColor = Color(0xFF00B159),
                            errorBorderColor = Color(0xFFEF4444)
                        ),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters
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
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp)
                        )
                    }
                }
            }

            // Error display if any
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
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.Error)
                        )
                    }
                }
            }

        }
    }
}

/**
 * Dedicated location picker card with GPS auto-fetch, quick Telangana areas, and autocomplete.
 */
@Composable
private fun LocationSectionCard(
    businessAddress: String,
    businessLatitude: Double,
    businessLongitude: Double,
    showError: Boolean,
    isDetecting: Boolean,
    locationErrorText: String?,
    locationService: com.example.dutype.utils.LocationService,
    onDetectClick: () -> Unit,
    onAddressChange: (String) -> Unit,
    onLocationChange: (Double, Double) -> Unit
) {
    val isCoordsValid = GeoUtils.hasValidCoordinates(businessLatitude, businessLongitude)

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
                            text = "DutyPe connects you to local workers and candidates nearby. Allow GPS or tap a quick area below.",
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

                // Location Autocomplete Field (only shown when location is not yet verified)
                com.example.dutype.components.LocationAutocompleteField(
                    value = businessAddress,
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = businessAddress.ifBlank { "GPS Coordinates Verified" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "GPS: %.4f° N, %.4f° E • Hyperlocal matching within 15 km".format(businessLatitude, businessLongitude),
                                fontSize = 11.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(
                            onClick = {
                                onLocationChange(0.0, 0.0)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Change",
                                color = StitchNavy.fg(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (showError) {
                Text(
                    text = "Please detect your location or select your area so nearby workers can find your jobs.",
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
