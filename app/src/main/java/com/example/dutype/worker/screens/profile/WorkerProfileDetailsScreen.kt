package com.example.dutype.worker.screens.profile

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import timber.log.Timber
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileDetailsScreen(
    navController: NavController,
    dataStore: ApplicationFormDataStore
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    
    // Current user ID
    var currentUserId by remember { mutableStateOf("") }
    
    // Form state
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    
    // Edit mode state
    var isEditMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Rating state
    var workerRating by remember { mutableStateOf(0f) }
    var workerTotalRatings by remember { mutableStateOf(0) }
    var workerReviews by remember { mutableStateOf<List<com.example.dutype.services.Rating>>(emptyList()) }
    var workerGivenReviews by remember { mutableStateOf<List<com.example.dutype.services.Rating>>(emptyList()) }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var isReviewsLoading by remember { mutableStateOf(false) }
    val ratingService = remember {
        com.example.dutype.services.RatingService(
            com.example.dutype.di.firestoreFromHilt(context),
            com.example.dutype.di.authFromHilt(context)
        )
    }
    
    // Image picker launcher with toast notification
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Timber.d("📸 WORKER PROFILE DETAILS: Image picker result - uri: $uri")
        uri?.let { selectedUri ->
            profileImageUri = selectedUri
            isUploadingImage = true
            
            scope.launch {
                try {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    Timber.d("📸 WORKER PROFILE DETAILS: Current user: ${currentUser?.uid}")
                    if (currentUser != null) {
                        Timber.d("📸 WORKER PROFILE DETAILS: Starting upload...")
                        val uploadResult = profileCompletionViewModel.uploadProfileImage(selectedUri, currentUser.uid, "worker")
                        uploadResult.fold(
                            onSuccess = { imageUrl ->
                                profileImageUrl = imageUrl
                                Timber.i("📸 WORKER PROFILE DETAILS: ✅ Profile image uploaded: $imageUrl")
                                
                                // Update profile data with new image URL
                                val updatedProfileData = mapOf(
                                    "profileImageUrl" to imageUrl
                                )
                                profileCompletionViewModel.saveWorkerProfileData(updatedProfileData)
                                
                                // Show success toast
                                Toast.makeText(context, context.getString(R.string.profile_photo_updated), Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { exception ->
                                Timber.e(exception, "📸 WORKER PROFILE DETAILS: ❌ Failed to upload profile image")
                                profileImageUri = null
                                Toast.makeText(context, context.getString(R.string.profile_photo_upload_failed), Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        Timber.w("📸 WORKER PROFILE DETAILS: No current user - cannot upload")
                        profileImageUri = null
                        Toast.makeText(context, context.getString(R.string.profile_login_to_upload), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "📸 WORKER PROFILE DETAILS: ❌ Error uploading profile image")
                    profileImageUri = null
                    Toast.makeText(context, context.getString(R.string.profile_photo_upload_error), Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingImage = false
                }
            }
        }
    }
    
    // Load existing profile data
    LaunchedEffect(Unit) {
        val personalInfo = dataStore.getPersonalInfo()
        
        fullName = personalInfo.fullName
        phoneNumber = personalInfo.phone
        dateOfBirth = personalInfo.dateOfBirth
        gender = personalInfo.gender
        
        // Load profile data from Firebase (including profile image)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            try {
                val workerProfileData = profileCompletionViewModel.getWorkerProfileData(currentUser.uid)
                workerProfileData.fold(
                    onSuccess = { data ->
                        fullName = data["fullName"] as? String ?: fullName
                        phoneNumber = data["phone"] as? String ?: phoneNumber
                        dateOfBirth = data["dateOfBirth"] as? String ?: dateOfBirth
                        gender = data["gender"] as? String ?: gender
                        experience = data["experience"] as? String ?: experience
                        profileImageUrl = data["profileImageUrl"] as? String

                        // skills from worker_profiles — displayed as comma-separated skills
                        val rawSkills = data["skills"]
                        skills = when (rawSkills) {
                            is List<*> -> rawSkills.filterIsInstance<String>().joinToString(", ")
                            is String -> rawSkills
                            else -> skills
                        }

                        Timber.d("📸 WORKER PROFILE DETAILS: Loaded profile image URL: $profileImageUrl")
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Error loading worker profile data")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading worker profile data")
            }
        }
    }
    
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Load worker ratings from worker_profiles (target schema)
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            try {
                val workerDoc = com.example.dutype.di.firestoreFromHilt(context)
                    .collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES).document(currentUserId).get().await()
                workerRating = (workerDoc.getDouble("rating") ?: 0.0).toFloat()
                workerTotalRatings = (workerDoc.getLong("totalRatings") ?: 0L).toInt()
            } catch (e: Exception) {
                Timber.e(e, "Error loading worker rating summary")
            }
        }
    }

    Scaffold(
        topBar = {
            // Custom TopAppBar with status bar padding
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WorkerColors.CardBackground,
                shadowElevation = 2.dp
            ) {
                Column {
                    // Status bar spacer
                    Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                    
                    // Header content
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
                                tint = WorkerColors.TextPrimary
                            )
                        }
                        
                        Text(
                            text = stringResource(R.string.profile_details_title),
                            style = AppTypography.screenTitle,
                            color = WorkerColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Edit button in header
                        if (!isEditMode) {
                            IconButton(onClick = { isEditMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = WorkerColors.TextPrimary
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = WorkerColors.Divider, thickness = 1.dp)
                }
            }
        },
        containerColor = WorkerColors.ScreenBackground
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
                // Profile Picture Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(600)) + slideInVertically(tween(600))
                    ) {
                        ProfileImageSection(
                            profileImageUrl = profileImageUrl,
                            profileImageUri = profileImageUri,
                            isUploadingImage = isUploadingImage,
                            fullName = fullName,
                            onImageClick = { imagePickerLauncher.launch("image/*") }
                        )
                    }
                }
                
                // Ratings & Reviews Section - Only show if user is logged in
                if (currentUserId.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(tween(700, 50)) + slideInVertically(tween(700, 50))
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable {
                                        scope.launch {
                                            isReviewsLoading = true
                                            workerReviews = ratingService.getUserRatings(currentUserId)
                                            workerGivenReviews = ratingService.getRatingsGivenByUser(currentUserId)
                                            isReviewsLoading = false
                                            showReviewsSheet = true
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    androidx.compose.foundation.layout.Column {
                                        Text(
                                            text = stringResource(R.string.my_ratings_reviews),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = WorkerColors.TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (workerTotalRatings > 0) "★ ${"%.1f".format(workerRating)}  •  $workerTotalRatings review${if (workerTotalRatings != 1) "s" else ""}"
                                            else stringResource(R.string.profile_no_ratings),
                                            style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = "View reviews",
                                        tint = WorkerColors.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Personal Information Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(800, 100)) + slideInVertically(tween(800, 100))
                    ) {
                        if (isEditMode) {
                            EditablePersonalInfoCard(
                                fullName = fullName,
                                onFullNameChange = { fullName = it },
                                phoneNumber = phoneNumber,
                                onPhoneNumberChange = { phoneNumber = it },
                                dateOfBirth = dateOfBirth,
                                onDateOfBirthChange = { dateOfBirth = it },
                                gender = gender,
                                onGenderChange = { gender = it }
                            )
                        } else {
                            ProfileInfoCard(
                                title = stringResource(R.string.personal_information),
                                items = listOf(
                                    stringResource(R.string.full_name) to fullName,
                                    stringResource(R.string.phone_number) to phoneNumber,
                                    stringResource(R.string.date_of_birth) to dateOfBirth,
                                    stringResource(R.string.gender) to gender
                                )
                            )
                        }
                    }
                }
                
                // Professional Information Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        if (isEditMode) {
                            EditableProfessionalInfoCard(
                                skills = skills,
                                onSkillsChange = { skills = it },
                                experience = experience,
                                onExperienceChange = { experience = it }
                            )
                        } else {
                            ProfileInfoCard(
                                title = stringResource(R.string.professional_information),
                                items = listOf(
                                    stringResource(R.string.skills) to skills,
                                    stringResource(R.string.experience) to experience
                                )
                            )
                        }
                    }
                }
            }
            
            // Save/Cancel buttons when in edit mode
            if (isEditMode) {
                EditModeButtons(
                    isSaving = isSaving,
                    onCancel = { isEditMode = false },
                    onSave = {
                        // Bug #12 fix: Validate DOB age (>= 18) before saving;
                        // previously the screen accepted any string and silently
                        // wrote it back to Firestore.
                        val dobError = com.example.dutype.utils.ValidationUtils.getDateOfBirthError(dateOfBirth)
                        if (dobError != null) {
                            Toast.makeText(context, dobError, Toast.LENGTH_LONG).show()
                            return@EditModeButtons
                        }
                        isSaving = true
                        scope.launch {
                            try {
                                val personalInfo = com.example.dutype.worker.models.PersonalInfo(
                                    fullName = fullName,
                                    phone = phoneNumber,
                                    dateOfBirth = dateOfBirth,
                                    gender = gender
                                )
                                dataStore.savePersonalInfo(personalInfo)
                                
                                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                if (currentUser != null) {
                                    val workerProfileData = mapOf(
                                        "fullName" to fullName,
                                        "phone" to phoneNumber,
                                        "dateOfBirth" to dateOfBirth,
                                        "gender" to gender,
                                        "skills" to skills,
                                        "experience" to experience
                                    )
                                    profileCompletionViewModel.saveWorkerProfileData(workerProfileData)
                                    Toast.makeText(context, context.getString(R.string.profile_saved), Toast.LENGTH_SHORT).show()
                                }
                                isEditMode = false
                            } catch (e: Exception) {
                                Timber.e(e, "Error saving profile")
                                Toast.makeText(context, context.getString(R.string.profile_save_failed), Toast.LENGTH_SHORT).show()
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
        title = stringResource(R.string.my_ratings_reviews),
        averageRating = workerRating,
        totalRatings = workerTotalRatings,
        reviews = workerReviews,
        givenReviews = workerGivenReviews,
        isLoading = isReviewsLoading,
        isGivenLoading = isReviewsLoading,
        receivedTabTitle = "Employers rated you",
        givenTabTitle = "You rated employers",
        onDismiss = { showReviewsSheet = false }
    )
}

// ============================================
// HELPER COMPOSABLES
// ============================================

@Composable
private fun ProfileImageSection(
    profileImageUrl: String?,
    profileImageUri: Uri?,
    isUploadingImage: Boolean,
    fullName: String,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current
    
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
                            .background(Color(0xFFF3F4F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = WorkerColors.Primary,
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
                            contentDescription = "Profile Picture",
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
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                else -> {
                    DefaultProfileIcon(fullName = fullName, onClick = onImageClick)
                }
            }
            
            // Camera icon overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(WorkerColors.Primary)
                    .clickable(onClick = onImageClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change Photo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = if (fullName.isNotBlank()) fullName else stringResource(R.string.profile_add_your_name),
            style = AppTypography.pageTitle,
            color = if (fullName.isNotBlank()) WorkerColors.TextPrimary else WorkerColors.TextSecondary
        )
        
        Text(
            text = stringResource(R.string.profile_tap_photo_change),
            style = AppTypography.caption,
            color = WorkerColors.TextSecondary
        )
    }
}

@Composable
private fun DefaultProfileIcon(
    fullName: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color(0xFFF3F4F6))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (fullName.isNotBlank()) {
            Text(
                text = fullName.take(2).uppercase(),
                style = AppTypography.displayTitle,
                color = WorkerColors.TextSecondary
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default Profile",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun ProfileInfoCard(
    title: String,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = AppTypography.sectionHeader,
                color = WorkerColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            items.forEachIndexed { index, (label, value) ->
                ProfileFieldDisplay(label = label, value = value)
                if (index < items.size - 1) {
                    HorizontalDivider(
                        color = WorkerColors.Divider,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldDisplay(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = AppTypography.caption,
            color = WorkerColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value.ifBlank { stringResource(R.string.profile_not_provided) },
            style = AppTypography.bodyMedium,
            color = if (value.isNotBlank()) WorkerColors.TextPrimary else WorkerColors.TextSecondary
        )
    }
}

@Composable
private fun EditablePersonalInfoCard(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    dateOfBirth: String,
    onDateOfBirthChange: (String) -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit
) {
    fun formatDob(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8)
        return buildString {
            digits.forEachIndexed { idx, c ->
                if (idx == 2 || idx == 4) append('/')
                append(c)
            }
        }
    }
    var dobInput by remember {
        mutableStateOf(TextFieldValue(dateOfBirth, selection = TextRange(dateOfBirth.length)))
    }
    LaunchedEffect(dateOfBirth) {
        if (dateOfBirth != dobInput.text) {
            dobInput = TextFieldValue(dateOfBirth, selection = TextRange(dateOfBirth.length))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.personal_information),
                style = AppTypography.sectionHeader,
                color = WorkerColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            ProfileTextField(
                label = stringResource(R.string.full_name),
                value = fullName,
                onValueChange = onFullNameChange,
                placeholder = stringResource(R.string.enter_full_name)
            )
            
            ProfileTextField(
                label = stringResource(R.string.phone_number),
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                placeholder = stringResource(R.string.enter_phone_number),
                keyboardType = KeyboardType.Phone,
                enabled = false
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.date_of_birth),
                    style = AppTypography.caption,
                    color = WorkerColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = dobInput,
                    onValueChange = {
                        val formatted = formatDob(it.text)
                        dobInput = TextFieldValue(formatted, selection = TextRange(formatted.length))
                        onDateOfBirthChange(formatted)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.dd_mm_yyyy),
                            style = AppTypography.bodyMedium,
                            color = WorkerColors.TextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = AppTypography.bodyMedium.copy(color = WorkerColors.TextPrimary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WorkerColors.Primary,
                        unfocusedBorderColor = WorkerColors.Divider
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            ProfileTextField(
                label = stringResource(R.string.gender),
                value = gender,
                onValueChange = onGenderChange,
                placeholder = stringResource(R.string.enter_gender)
            )
        }
    }
}

@Composable
private fun EditableProfessionalInfoCard(
    skills: String,
    onSkillsChange: (String) -> Unit,
    experience: String,
    onExperienceChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.professional_information),
                style = AppTypography.sectionHeader,
                color = WorkerColors.TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            ProfileTextField(
                label = stringResource(R.string.skills),
                value = skills,
                onValueChange = onSkillsChange,
                placeholder = stringResource(R.string.enter_skills_comma),
                singleLine = false,
                minLines = 2
            )
            
            ProfileTextField(
                label = stringResource(R.string.experience),
                value = experience,
                onValueChange = onExperienceChange,
                placeholder = stringResource(R.string.describe_experience),
                singleLine = false,
                minLines = 3
            )
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
                android.widget.Toast.makeText(
                    context,
                    "Unable to fetch location. Please check permissions.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Error fetching location: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } finally {
            isFetchingLocation = false
        }
    }

    // Re-prompt the OS location permission on every tap if it hasn't been granted
    // yet — including recoveries from a previous denial.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch { runLocationFetch() }
        } else {
            isFetchingLocation = false
            android.widget.Toast.makeText(
                context,
                "Location permission required to use this button.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Check if this is the address field
    val isAddressField = label == "Address"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = AppTypography.caption,
            color = WorkerColors.TextSecondary,
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
                    color = WorkerColors.TextSecondary.copy(alpha = 0.6f)
                )
            },
            textStyle = AppTypography.bodyMedium.copy(color = WorkerColors.TextPrimary),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WorkerColors.Primary,
                unfocusedBorderColor = WorkerColors.Divider,
                disabledBorderColor = WorkerColors.Divider.copy(alpha = 0.5f),
                disabledTextColor = WorkerColors.TextSecondary
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
                            tint = WorkerColors.Primary
                        )
                    }
                }
            } else if (isAddressField && isFetchingLocation) {
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = WorkerColors.Primary
                    )
                }
            } else null
        )
        
        // Helper text for disabled phone field
        if (!enabled && label == stringResource(R.string.phone_number)) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Phone number cannot be changed (from login)",
                style = AppTypography.caption.copy(
                    color = WorkerColors.TextSecondary.copy(alpha = 0.7f),
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
        color = WorkerColors.CardBackground,
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
                    contentColor = WorkerColors.TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = AppTypography.buttonMedium
                )
            }
            
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WorkerColors.Primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.save),
                        style = AppTypography.buttonMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}
