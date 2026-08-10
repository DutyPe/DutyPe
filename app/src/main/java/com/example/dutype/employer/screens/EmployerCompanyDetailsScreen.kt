package com.example.dutype.employer.screens

import com.dutype.app.R
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import androidx.compose.ui.res.stringResource

/**
 * Employer Company Details screen.
 *
 * Refactored Apr 2026 to mirror the structure and polish of
 * WorkerProfileDetailsScreen Ã¢â‚¬â€ same Surface-based custom top bar,
 * staggered AnimatedVisibility entrance, LazyColumn body, view/edit
 * mode separation, sticky bottom Save/Cancel bar, and EmployerColors
 * theming throughout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerCompanyDetailsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()

    // Identity
    var currentUserId by remember { mutableStateOf("") }

    // Form state Ã¢â‚¬â€ basic info
    var companyName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }

    // Form state Ã¢â‚¬â€ company details
    var industry by remember { mutableStateOf("") }

    // Profile image
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }

    // Edit / save state
    var isEditMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Ratings
    var employerRating by remember { mutableStateOf(0f) }
    var employerTotalRatings by remember { mutableStateOf(0) }
    var employerReviews by remember {
        mutableStateOf<List<com.example.dutype.services.Rating>>(emptyList())
    }
    var employerGivenReviews by remember {
        mutableStateOf<List<com.example.dutype.services.Rating>>(emptyList())
    }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var isReviewsLoading by remember { mutableStateOf(false) }
    val ratingService = remember {
        com.example.dutype.services.RatingService(
            com.example.dutype.di.firestoreFromHilt(context),
            com.example.dutype.di.authFromHilt(context)
        )
    }

    // Image picker
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
                                profileCompletionViewModel.saveEmployerProfileData(
                                    mapOf("profileImageUrl" to imageUrl)
                                )
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
            currentUserId = currentUser.uid
            try {
                val employerProfileData =
                    profileCompletionViewModel.getEmployerProfileData(currentUser.uid)
                employerProfileData.fold(
                    onSuccess = { data ->
                        companyName = data["companyName"] as? String ?: ""
                        contactPhone = data["contactPhone"] as? String
                            ?: data["phone"] as? String ?: ""
                        businessAddress = data["businessAddress"] as? String ?: ""
                        industry = data["industry"] as? String ?: ""
                        profileImageUrl = data["profileImageUrl"] as? String
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Error loading employer profile data")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading employer profile data")
            }
        }
    }

    // Load rating summary
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            try {
                val employerDoc = com.example.dutype.di.firestoreFromHilt(context)
                    .collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES)
                    .document(currentUserId)
                    .get()
                    .await()
                employerRating = (employerDoc.getDouble("rating") ?: 0.0).toFloat()
                employerTotalRatings = (employerDoc.getLong("totalRatings") ?: 0L).toInt()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.e(e, "Error loading employer rating summary")
            }
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EmployerColors.CardBackground,
                shadowElevation = 2.dp
            ) {
                Column {
                    Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = EmployerColors.TextPrimary
                            )
                        }
                        Text(
                            text = stringResource(R.string.company_details_section),
                            style = AppTypography.screenTitle,
                            color = EmployerColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (!isEditMode) {
                            IconButton(onClick = { isEditMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Company Details",
                                    tint = EmployerColors.TextPrimary
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = EmployerColors.Divider, thickness = 1.dp)
                }
            }
        },
        containerColor = EmployerColors.ScreenBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Logo + headline
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(600)) + slideInVertically(tween(600))
                    ) {
                        CompanyLogoSection(
                            profileImageUrl = profileImageUrl,
                            profileImageUri = profileImageUri,
                            isUploadingImage = isUploadingImage,
                            companyName = companyName,
                            onImageClick = { imagePickerLauncher.launch("image/*") }
                        )
                    }
                }

                // Ratings & Reviews
                if (currentUserId.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(700, 50)) + slideInVertically(tween(700, 50))
                        ) {
                            RatingsCard(
                                rating = employerRating,
                                totalRatings = employerTotalRatings,
                                onClick = {
                                    scope.launch {
                                        isReviewsLoading = true
                                        employerReviews = ratingService.getUserRatings(currentUserId)
                                        employerGivenReviews = ratingService.getRatingsGivenByUser(currentUserId)
                                        isReviewsLoading = false
                                        showReviewsSheet = true
                                    }
                                }
                            )
                        }
                    }
                }

                // Basic Information
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(800, 100)) + slideInVertically(tween(800, 100))
                    ) {
                        if (isEditMode) {
                            EditableBasicInfoCard(
                                companyName = companyName,
                                onCompanyNameChange = { companyName = it },
                                contactPhone = contactPhone,
                                onContactPhoneChange = { contactPhone = it },
                                businessAddress = businessAddress,
                                onBusinessAddressChange = { businessAddress = it }
                            )
                        } else {
                            CompanyInfoCard(
                                title = stringResource(R.string.basic_information),
                                items = listOf(
                                    stringResource(R.string.company_name) to companyName,
                                    stringResource(R.string.contact_phone) to contactPhone,
                                    stringResource(R.string.business_address) to businessAddress
                                )
                            )
                        }
                    }
                }

                // Company details
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        if (isEditMode) {
                            EditableCompanyDetailsCard(
                                industry = industry,
                                onIndustryChange = { industry = it }
                            )
                        } else {
                            CompanyInfoCard(
                                title = stringResource(R.string.company_details_section),
                                items = listOf(
                                    stringResource(R.string.industry) to industry
                                )
                            )
                        }
                    }
                }
            }

            if (isEditMode) {
                EditModeButtons(
                    isSaving = isSaving,
                    onCancel = { isEditMode = false },
                    onSave = {
                        isSaving = true
                        scope.launch {
                            try {
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                if (currentUser != null) {
                                    val data = mutableMapOf<String, Any>(
                                        "companyName" to companyName,
                                        "businessAddress" to businessAddress,
                                        "industry" to industry
                                    )
                                    profileImageUrl?.takeIf { it.isNotBlank() }?.let {
                                        data["profileImageUrl"] = it
                                    }
                                    profileCompletionViewModel.saveEmployerProfileData(data)
                                    Toast.makeText(
                                        context,
                                        "Company details saved successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                isEditMode = false
                            } catch (e: Exception) {
                                Timber.e(e, "Error saving company details")
                                Toast.makeText(
                                    context,
                                    "Failed to save company details",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                )
            }
        }
    }

    com.example.dutype.components.UserReviewsBottomSheet(
        isVisible = showReviewsSheet,
        title = stringResource(R.string.employer_ratings_reviews),
        averageRating = employerRating,
        totalRatings = employerTotalRatings,
        reviews = employerReviews,
        givenReviews = employerGivenReviews,
        isLoading = isReviewsLoading,
        isGivenLoading = isReviewsLoading,
        receivedTabTitle = stringResource(R.string.workers_rated_you),
        givenTabTitle = stringResource(R.string.you_rated_workers),
        onDismiss = { showReviewsSheet = false }
    )
}

// ============================================
// HELPER COMPOSABLES
// ============================================

@Composable
private fun CompanyLogoSection(
    profileImageUrl: String?,
    profileImageUri: Uri?,
    isUploadingImage: Boolean,
    companyName: String,
    onImageClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isUploadingImage -> {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(EmployerColors.ChipBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = EmployerColors.Primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
                profileImageUri != null -> {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onImageClick)
                    ) {
                        com.example.dutype.components.OptimizedProfileImage(
                            imageUrl = profileImageUri.toString(),
                            contentDescription = "Company Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                !profileImageUrl.isNullOrBlank() -> {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onImageClick)
                    ) {
                        com.example.dutype.components.OptimizedProfileImage(
                            imageUrl = profileImageUrl,
                            contentDescription = "Company Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                else -> {
                    DefaultCompanyLogo(companyName = companyName, onClick = onImageClick)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EmployerColors.Primary)
                    .clickable(onClick = onImageClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change Logo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (companyName.isNotBlank()) companyName else "Add Your Company",
            style = AppTypography.pageTitle,
            color = if (companyName.isNotBlank()) EmployerColors.TextPrimary else EmployerColors.TextSecondary
        )

        Text(
            text = stringResource(R.string.auto_tap_logo_to_change),
            style = AppTypography.caption,
            color = EmployerColors.TextSecondary
        )
    }
}

@Composable
private fun DefaultCompanyLogo(
    companyName: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(EmployerColors.PrimaryLight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (companyName.isNotBlank()) {
            Text(
                text = companyName.take(2).uppercase(),
                style = AppTypography.displayTitle,
                color = EmployerColors.Primary
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default Logo",
                tint = EmployerColors.IconSecondary,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun RatingsCard(
    rating: Float,
    totalRatings: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.auto_ratings_reviews),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = EmployerColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (totalRatings > 0) {
                        "Ã¢Ëœâ€¦ ${"%.1f".format(rating)}  Ã¢â‚¬Â¢  $totalRatings review${if (totalRatings != 1) "s" else ""}"
                    } else {
                        "No ratings yet"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "View reviews",
                tint = EmployerColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CompanyInfoCard(
    title: String,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmployerColors.Divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = AppTypography.sectionHeader,
                color = EmployerColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            items.forEachIndexed { index, (label, value) ->
                CompanyFieldDisplay(label = label, value = value)
                if (index < items.size - 1) {
                    HorizontalDivider(
                        color = EmployerColors.Divider,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanyFieldDisplay(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = AppTypography.caption,
            color = EmployerColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value.ifBlank { "Not provided" },
            style = AppTypography.bodyMedium,
            color = if (value.isNotBlank()) EmployerColors.TextPrimary else EmployerColors.TextSecondary
        )
    }
}

@Composable
private fun EditableBasicInfoCard(
    companyName: String,
    onCompanyNameChange: (String) -> Unit,
    contactPhone: String,
    onContactPhoneChange: (String) -> Unit,
    businessAddress: String,
    onBusinessAddressChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmployerColors.Divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.basic_information),
                style = AppTypography.sectionHeader,
                color = EmployerColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            CompanyTextField(
                label = stringResource(R.string.company_name),
                value = companyName,
                onValueChange = onCompanyNameChange,
                placeholder = stringResource(R.string.enter_company_name)
            )
            CompanyTextField(
                label = stringResource(R.string.contact_phone),
                value = contactPhone,
                onValueChange = onContactPhoneChange,
                placeholder = stringResource(R.string.login_phone_number),
                keyboardType = KeyboardType.Phone,
                enabled = false
            )
            CompanyTextField(
                label = stringResource(R.string.business_address),
                value = businessAddress,
                onValueChange = onBusinessAddressChange,
                placeholder = stringResource(R.string.enter_business_address),
                singleLine = false,
                minLines = 2
            )
        }
    }
}

@Composable
private fun EditableCompanyDetailsCard(
    industry: String,
    onIndustryChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmployerColors.Divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.company_details_section),
                style = AppTypography.sectionHeader,
                color = EmployerColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            CompanyTextField(
                label = stringResource(R.string.industry),
                value = industry,
                onValueChange = onIndustryChange,
                placeholder = "e.g. Hospitality, Retail, IT services"
            )
        }
    }
}

@Composable
private fun CompanyTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isFetchingLocation by remember { mutableStateOf(false) }

    // Local helper so both the direct tap (permission already granted) and the
    // post-grant callback share the exact same fetch path.
    suspend fun runLocationFetch() {
        try {
            val locationService = com.example.dutype.utils.LocationService(context)
            val locationInfo = locationService.getCurrentLocation()
            if (locationInfo != null) {
                onValueChange(locationInfo.getFullAddress())
            } else {
                Toast.makeText(
                    context,
                    "Unable to fetch location. Please check permissions.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error fetching location: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            isFetchingLocation = false
        }
    }

    // Re-prompt the OS location permission on every tap if it hasn't been granted
    // yet â€” including recoveries from a previous denial.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch { runLocationFetch() }
        } else {
            isFetchingLocation = false
            Toast.makeText(
                context,
                "Location permission required to use this button.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val isAddressField = label == "Business Address"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = AppTypography.caption,
            color = EmployerColors.TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    style = AppTypography.bodyMedium,
                    color = EmployerColors.TextSecondary.copy(alpha = 0.6f)
                )
            },
            textStyle = AppTypography.bodyMedium.copy(color = EmployerColors.TextPrimary),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmployerColors.Primary,
                unfocusedBorderColor = EmployerColors.Border,
                disabledBorderColor = EmployerColors.Border.copy(alpha = 0.5f),
                disabledTextColor = EmployerColors.TextSecondary
            ),
            shape = RoundedCornerShape(8.dp),
            trailingIcon = if (isAddressField && enabled && !isFetchingLocation) {
                {
                    IconButton(
                        onClick = {
                            isFetchingLocation = true
                            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                scope.launch { runLocationFetch() }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Fetch current location",
                            tint = EmployerColors.Primary
                        )
                    }
                }
            } else if (isAddressField && isFetchingLocation) {
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = EmployerColors.Primary
                    )
                }
            } else null
        )

        if (!enabled && label == stringResource(R.string.contact_phone)) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.auto_phone_number_cannot_be_changed_from_login),
                style = AppTypography.caption.copy(
                    color = EmployerColors.TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun EditModeButtons(
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EmployerColors.CardBackground,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = EmployerColors.TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.auto_cancel),
                    style = AppTypography.buttonMedium
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmployerColors.Primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = stringResource(R.string.auto_save_changes),
                        style = AppTypography.buttonMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
