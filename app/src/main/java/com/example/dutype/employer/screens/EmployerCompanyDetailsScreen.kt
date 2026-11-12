package com.example.dutype.employer.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.example.dutype.firestore.FirestoreSchema.Values
import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.dutype.app.R
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.models.QuickLocations
import com.example.dutype.utils.GeoUtils
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import timber.log.Timber

// ---------------------------------------------------------------------------
// Screen 38 design tokens (Employer "Company Details")
// ---------------------------------------------------------------------------
private val CdNavy = Color(0xFF0F172A)
private val CdCobalt = Color(0xFF2563EB)
private val CdBorder = Color(0xFFE2E8F0)
private val CdMuted = Color(0xFF64748B)
private val CdHint = Color(0xFF94A3B8)
private val CdChipText = Color(0xFF475569)
private val CdGreen = Color(0xFF16A34A)
private val CdGreenBg = Color(0xFFF0FDF4)
private val CdGstGreen = Color(0xFF10B981)



/**
 * Employer Company Details screen (business profile editor).
 * Layout follows Screen38-CompanyDetails mockup.
 */
@Composable
fun EmployerCompanyDetailsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()

    val locationService = profileCompletionViewModel.locationService

    // Form state
    var employerType by remember { mutableStateOf(Values.EmployerType.COMPANY) }
    var companyName by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var businessLatitude by remember { mutableDoubleStateOf(0.0) }
    var businessLongitude by remember { mutableDoubleStateOf(0.0) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    // Profile image
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }

    val fetchLocation: () -> Unit = {
        scope.launch {
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
                    profileCompletionViewModel.locationPreferences.saveLocation(locationService.toLocationData(locationInfo))
                } else {
                    Toast.makeText(context, "Could not detect GPS location. Please enter address manually.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching current location in EmployerCompanyDetailsScreen")
                Toast.makeText(context, "Location detection failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Location permission required to detect GPS location", Toast.LENGTH_SHORT).show()
        }
    }

    // Image picker + upload (unchanged behavior)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            profileImageUri = selectedUri
            isUploadingImage = true
            scope.launch {
                try {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        val uploadResult = profileCompletionViewModel.uploadProfileImage(
                            selectedUri, currentUser.uid, "employer"
                        )
                        uploadResult.fold(
                            onSuccess = { imageUrl ->
                                profileImageUrl = imageUrl
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.company_logo_updated),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { exception ->
                                Timber.e(exception, "Failed to upload company logo")
                                profileImageUri = null
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.failed_upload_logo),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    } else {
                        profileImageUri = null
                        Toast.makeText(context, context.getString(R.string.please_login_upload_logo), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error uploading company logo")
                    profileImageUri = null
                    Toast.makeText(context, context.getString(R.string.error_uploading_logo), Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingImage = false
                }
            }
        }
    }

    // Load profile
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                if (email.isBlank()) {
                    email = currentUser.email.orEmpty().ifBlank { profileCompletionViewModel.getUserEmail().orEmpty() }
                }
                profileCompletionViewModel.getEmployer(currentUser.uid)
                    .onSuccess { saved ->
                        if (saved != null) {
                            employerType = saved.employerType.ifBlank { Values.EmployerType.COMPANY }
                            companyName = if (saved.isCompany) saved.businessName else saved.ownerName
                            contactPerson = saved.ownerName
                            contactPhone = saved.phone.ifBlank { currentUser.phoneNumber.orEmpty() }
                            businessAddress = saved.address
                            if (GeoUtils.hasValidCoordinates(saved.lat, saved.lng)) {
                                businessLatitude = saved.lat
                                businessLongitude = saved.lng
                            }
                            profileImageUrl = saved.photoUrl.ifBlank { null }
                        }
                    }
                    .onFailure { Timber.e(it, "Error loading employer profile data") }

                if (contactPhone.isBlank()) {
                    contactPhone = currentUser.phoneNumber?.removePrefix("+91")?.trim() ?: ""
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading employer profile data")
            }
        }
    }

    val doSave: () -> Unit = {
        if (!isSaving) {
            isSaving = true
            scope.launch {
                try {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        val isCompany = employerType == Values.EmployerType.COMPANY

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
                                    }
                                } catch (e: Exception) {
                                    Timber.w(e, "Could not geocode employer address: $businessAddress")
                                }
                            }
                        }
                        if (!GeoUtils.hasValidCoordinates(finalLat, finalLng)) {
                            finalLat = 17.2473
                            finalLng = 80.1514
                        }
                        val geohashStr = GeoFireUtils.getGeoHashForLocation(GeoLocation(finalLat, finalLng))
                        val userPhone = currentUser.phoneNumber?.takeIf { it.isNotBlank() }
                            ?: (if (contactPhone.isNotBlank()) "+91 ${contactPhone.removePrefix("+91").trim()}" else "")

                        val employerProfileData = mutableMapOf<String, Any?>(
                            EmployerProfiles.EMPLOYER_TYPE to employerType,
                            EmployerProfiles.OWNER_NAME to (if (contactPerson.isNotBlank()) contactPerson.trim() else companyName.trim()),
                            EmployerProfiles.BUSINESS_NAME to (if (companyName.isNotBlank()) companyName.trim() else contactPerson.trim()),
                            EmployerProfiles.PHONE to userPhone,
                            EmployerProfiles.ADDRESS to businessAddress.trim(),
                            EmployerProfiles.AREA to businessAddress.take(40),
                            EmployerProfiles.LAT to finalLat,
                            EmployerProfiles.LNG to finalLng,
                            "geohash" to geohashStr
                        )
                        if (email.isNotBlank()) {
                            employerProfileData[EmployerProfiles.EMAIL] = email.trim()
                        }
                        if (!profileImageUrl.isNullOrBlank()) {
                            employerProfileData[EmployerProfiles.PHOTO_URL] = profileImageUrl
                        }

                        // 1. Save employer document in Firestore
                        profileCompletionViewModel.saveEmployer(employerProfileData).getOrThrow()

                        // 2. Authoritative sync to central users collection
                        val userLocationUpdate = mapOf<String, Any?>(
                            "name" to (if (contactPerson.isNotBlank()) contactPerson.trim() else companyName.trim()),
                            "fullName" to (if (contactPerson.isNotBlank()) contactPerson.trim() else companyName.trim()),
                            "businessName" to (if (companyName.isNotBlank()) companyName.trim() else contactPerson.trim()),
                            "city" to businessAddress.take(40),
                            "email" to email.trim(),
                            "updatedAt" to com.google.firebase.Timestamp.now(),
                            "lat" to finalLat,
                            "lng" to finalLng,
                            "geohash" to geohashStr,
                            "address" to businessAddress.trim(),
                            "area" to businessAddress.take(40),
                            "hasLocation" to true,
                            "locationUpdatedAt" to com.google.firebase.Timestamp.now()
                        )
                        runCatching {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users").document(currentUser.uid)
                                .set(userLocationUpdate, com.google.firebase.firestore.SetOptions.merge())
                                .await()
                        }.onFailure { Timber.w(it, "Could not sync employer location to users collection") }

                        Toast.makeText(
                            context,
                            context.getString(R.string.profile_saved_success),
                            Toast.LENGTH_SHORT
                        ).show()

                        // Save and go back
                        navController.popBackStack()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error saving company details")
                    Toast.makeText(
                        context,
                        context.getString(R.string.profile_saved_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    isSaving = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.bg())
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            CdTopBar(
                onBack = { navController.popBackStack() }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CdPhotoCard(
                    profileImageUrl = profileImageUrl,
                    profileImageUri = profileImageUri,
                    isUploading = isUploadingImage,
                    onChoose = { imagePickerLauncher.launch("image/*") }
                )
                CdEmployerTypeCard(
                    selectedType = employerType,
                    onSelectType = { employerType = it }
                )
                CdBasicsCard(
                    companyName = companyName,
                    onCompanyName = { companyName = it },
                    contactPerson = contactPerson,
                    onContactPerson = { contactPerson = it },
                    contactPhone = contactPhone,
                    email = email,
                    onEmailChange = { email = it }
                )
                CdAddressCard(
                    address = businessAddress,
                    onAddressChange = { businessAddress = it },
                    latitude = businessLatitude,
                    longitude = businessLongitude,
                    isDetecting = isDetectingLocation,
                    onDetectLocation = {
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
                    onSelectPreset = { presetAddress, lat, lng ->
                        businessAddress = presetAddress
                        businessLatitude = lat
                        businessLongitude = lng
                    }
                )
            }
        }

        CdBottomBar(
            isSaving = isSaving,
            onSave = doSave,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ============================================
// SECTIONS
// ============================================

@Composable
private fun CdTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = CdNavy.fg(),
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.company_details_section),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CdNavy.fg()
        )
    }
}

@Composable
private fun CdPhotoCard(
    profileImageUrl: String?,
    profileImageUri: Uri?,
    isUploading: Boolean,
    onChoose: () -> Unit
) {
    val dashColor = CdHint.fg()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val sw = 1.5.dp.toPx()
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(size.width - sw, size.height - sw),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = sw,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f
                        )
                    )
                )
            }
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val hasImage = profileImageUri != null || !profileImageUrl.isNullOrBlank()
        when {
            isUploading -> {
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = CdCobalt.fg(),
                        strokeWidth = 3.dp
                    )
                }
            }
            hasImage -> {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    com.example.dutype.components.OptimizedProfileImage(
                        imageUrl = profileImageUri?.toString() ?: profileImageUrl.orEmpty(),
                        contentDescription = stringResource(R.string.company_photo),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = CdMuted.fg(),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.upload_workplace_photo_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = CdNavy.fg(),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = stringResource(R.string.upload_workplace_photo_desc),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = CdMuted.fg(),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, CdNavy.bd()), RoundedCornerShape(18.dp))
                .clickable(enabled = !isUploading, onClick = onChoose)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.choose_photo),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CdNavy.fg()
            )
        }
    }
}

@Composable
private fun CdCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CdBorder.bd()), RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun CdEmployerTypeCard(
    selectedType: String,
    onSelectType: (String) -> Unit
) {
    CdCard {
        Text(
            text = "EMPLOYER / BUSINESS TYPE",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = CdHint.fg(),
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf(
                Values.EmployerType.INDIVIDUAL to "Individual",
                "SHOP" to "Shop / Business",
                Values.EmployerType.COMPANY to "Company"
            )
            types.forEach { (typeValue, label) ->
                val isSelected = (selectedType == typeValue) || (typeValue == "SHOP" && selectedType == "SHOP")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) CdNavy.bg() else Color(0xFFF1F5F9).bg())
                        .border(
                            1.dp,
                            if (isSelected) CdNavy.bd() else CdBorder.bd(),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectType(typeValue) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else CdNavy.fg(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CdBasicsCard(
    companyName: String,
    onCompanyName: (String) -> Unit,
    contactPerson: String,
    onContactPerson: (String) -> Unit,
    contactPhone: String,
    email: String,
    onEmailChange: (String) -> Unit
) {
    CdCard {
        Text(
            text = stringResource(R.string.business_basics),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = CdHint.fg(),
            letterSpacing = 0.6.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        CdField(
            label = stringResource(R.string.business_name),
            value = companyName,
            onValueChange = onCompanyName,
            placeholder = "e.g. Sri Venkateswara Kirana / Shop"
        )
        Spacer(modifier = Modifier.height(10.dp))
        CdField(
            label = stringResource(R.string.owner_contact_name),
            value = contactPerson,
            onValueChange = onContactPerson,
            placeholder = "e.g. Kiran Kumar"
        )
        Spacer(modifier = Modifier.height(10.dp))
        CdField(
            label = stringResource(R.string.contact_phone_number_label),
            value = contactPhone,
            onValueChange = {},
            enabled = false,
            keyboardType = KeyboardType.Phone,
            trailing = if (contactPhone.isNotBlank()) {
                { CdVerifiedBadge() }
            } else null
        )
        Spacer(modifier = Modifier.height(10.dp))
        CdField(
            label = stringResource(R.string.email_address),
            value = email,
            onValueChange = onEmailChange,
            placeholder = stringResource(R.string.enter_email),
            keyboardType = KeyboardType.Email
        )
    }
}

@Composable
private fun CdAddressCard(
    address: String,
    onAddressChange: (String) -> Unit,
    latitude: Double,
    longitude: Double,
    isDetecting: Boolean,
    onDetectLocation: () -> Unit,
    onSelectPreset: (String, Double, Double) -> Unit
) {
    CdCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BUSINESS LOCATION & ADDRESS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CdHint.fg(),
                letterSpacing = 0.6.sp
            )
            if (GeoUtils.hasValidCoordinates(latitude, longitude)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = CdGreen.fg(),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "GPS Saved",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CdGreen.fg()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        CdField(
            label = stringResource(R.string.business_address),
            value = address,
            onValueChange = onAddressChange,
            placeholder = "e.g. Near Bustand, Wyra Road, Khammam"
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEFF6FF).bg())
                .border(1.dp, Color(0xFFBFDBFE).bd(), RoundedCornerShape(10.dp))
                .clickable(enabled = !isDetecting, onClick = onDetectLocation),
            contentAlignment = Alignment.Center
        ) {
            if (isDetecting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CdCobalt.fg()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detecting current location...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CdCobalt.fg()
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📍", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Use Current GPS Location",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CdCobalt.fg()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Quick Select Area:",
            fontSize = 11.sp,
            color = CdMuted.fg(),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickLocations.PRESETS.forEach { preset ->
                val isSelected = address.contains(preset.shortName, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) CdCobalt.bg() else Color(0xFFF1F5F9).bg())
                        .clickable {
                            onSelectPreset(preset.address, preset.lat, preset.lng)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset.shortName,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else CdNavy.fg()
                    )
                }
            }
        }
    }
}



@Composable
private fun CdBottomBar(
    isSaving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.bg())
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 1.dp, color = CdBorder.bd())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(79.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(CdNavy.bg())
                    .clickable(enabled = !isSaving, onClick = onSave),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = stringResource(R.string.save_company_details_action),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================
// SMALL BUILDING BLOCKS
// ============================================



@Composable
private fun CdVerifiedBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CdGreenBg.bg())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = CdGreen.fg(),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.verified_label),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = CdGreen.fg(),
            maxLines = 1
        )
    }
}

/**
 * 56dp outlined field with a stacked label (11sp) above the value (14sp).
 */
@Composable
private fun CdField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(BorderStroke(1.dp, CdBorder.bd()), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = CdMuted.fg(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = CdNavy.fg()
                ),
                cursorBrush = SolidColor(CdCobalt.fg()),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    capitalization = capitalization
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                color = CdHint.fg(),
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}
