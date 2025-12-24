package com.example.dutype.worker.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.dutype.app.R
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.worker.models.PersonalInfo
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.ProfileViewModel
import com.example.dutype.auth.AuthManager
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.components.ProfileCompletionProgress
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.auth.GoogleSignInManager
import com.example.dutype.models.UserRole
import com.example.dutype.components.ProfileShimmer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileScreen(
    rootNavController: NavController,
    localNavController: NavController? = null,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null,
    dataStore: ApplicationFormDataStore
) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val authManager = remember { AuthManager(context) }
    val googleSignInManager = remember { GoogleSignInManager(context) }
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profileUiState by profileViewModel.uiState.collectAsState()
    val profileCompletionService: ProfileCompletionService = remember { ProfileCompletionService() }
    
    // Auth validation - Check if user is still authenticated
    // COMMENTED OUT: Allow users to view profile with dummy data without login
    /*
    LaunchedEffect(Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Timber.w("Worker Profile - User not authenticated, redirecting to login")
            rootNavController.navigate(com.example.dutype.navigation.Routes.ENHANCED_LOGIN) {
                popUpTo(com.example.dutype.navigation.Routes.WORKER_HOME) { inclusive = false }
            }
        } else {
            Timber.i("Worker Profile - User authenticated: ${currentUser.uid}")
        }
    }
    */
    
    // Profile completion state
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    var isProfileCompleted by remember { mutableStateOf(false) }
    
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Get profile data from dataStore
    var personalInfo by remember { mutableStateOf(dataStore.getPersonalInfo()) }
    val experience = remember { dataStore.getExperience() }
    val skills = remember { dataStore.getSkills() }
    val coverLetter = remember { dataStore.getCoverLetter() }
    val isFormCompleted = remember { dataStore.isFormCompleted() }
    
    // Initialize ProfileViewModel and load profile data from Firebase
    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }
    
    // Use backend profile data if available, otherwise fallback to dataStore
    val backendUser = profileUiState.user
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var profileSetupStatus by remember { mutableStateOf<com.example.dutype.state.ProfileSetupStatus?>(null) }
    var profileCompletion by remember { mutableStateOf(0) }
    
    // Firebase profile data state for reactive updates
    var firebaseProfileData by remember { mutableStateOf<Map<String, Any?>?>(null) }
    
    // Calculate profile completion percentage
    LaunchedEffect(personalInfo, experience, skills) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val completion = profileCompletionService.calculateWorkerProfileCompletion(
                    fullName = personalInfo.fullName,
                    email = personalInfo.email,
                    phoneNumber = personalInfo.phone,
                    address = personalInfo.address,
                    dateOfBirth = personalInfo.dateOfBirth,
                    gender = personalInfo.gender,
                    skills = skills.joinToString(", "),
                    experience = experience.joinToString(", "),
                    profileImageUrl = null
                )
                profileCompletionPercentage = completion
                isProfileCompleted = completion >= 100
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Load profile completion status from our new system
    LaunchedEffect(Unit) {
        isLoadingProfile = true
        try {
            val status = profileCompletionViewModel.getProfileSetupStatus(com.example.dutype.models.UserRole.WORKER)
            profileSetupStatus = status
            profileCompletion = status.completionPercentage
            
            // Load user info from ProfileSetupStateManager
            val savedEmail = profileCompletionViewModel.getUserEmail()
            val savedName = profileCompletionViewModel.getUserName()
            
            if (savedEmail != null) {
                userEmail = savedEmail
            }
            if (savedName != null) {
                userName = savedName
            }
            
            // Load additional profile data from Firestore
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                try {
                    val workerProfileData = profileCompletionViewModel.getWorkerProfileData(currentUser.uid)
                    workerProfileData.fold(
                        onSuccess = { data ->
                            // Store Firebase data in reactive state
                            firebaseProfileData = data
                            
                            // Load profile image URL from Firebase
                            val imageUrl = data["profileImageUrl"] as? String
                            if (imageUrl != null) {
                                profileImageUrl = imageUrl
                            }
                            
                            // Update personal info with Firestore data
                            val updatedPersonalInfo = personalInfo.copy(
                                fullName = data["fullName"] as? String ?: personalInfo.fullName,
                                email = data["email"] as? String ?: personalInfo.email,
                                phone = data["phone"] as? String ?: personalInfo.phone,
                                address = data["address"] as? String ?: personalInfo.address,
                                dateOfBirth = data["dateOfBirth"] as? String ?: personalInfo.dateOfBirth,
                                gender = data["gender"] as? String ?: personalInfo.gender
                            )
                            
                            // Update the personalInfo state to trigger recomposition
                            personalInfo = updatedPersonalInfo
                            
                            // Save the updated personal info back to DataStore
                            dataStore.savePersonalInfo(updatedPersonalInfo)
                            
                            Timber.i("Worker profile loaded - Name: ${data["fullName"]}, Email: ${data["email"]}, Phone: ${data["phone"]}")
                        },
                        onFailure = { exception ->
                            Timber.e(exception, "Error loading worker profile data")
                        }
                    )
                } catch (e: Exception) {
                    // Handle error loading additional profile data
                    Timber.e(e, "Error loading worker profile data")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            // Fallback to dataStore
            profileCompletion = if (isFormCompleted) 100 else dataStore.getFormCompletionPercentage()
        } finally {
            isLoadingProfile = false
        }
    }
    
    // Update userName and userEmail when data changes - prioritize Firebase data
    LaunchedEffect(backendUser, personalInfo, firebaseProfileData) {
        val firebaseData = firebaseProfileData
        val firebaseFullName = firebaseData?.get("fullName") as? String
        val firebaseEmail = firebaseData?.get("email") as? String
        
        userName = when {
            firebaseFullName?.isNotBlank() == true -> firebaseFullName
            backendUser?.fullName?.isNotBlank() == true -> backendUser.fullName
            personalInfo.fullName.isNotBlank() -> personalInfo.fullName
            else -> "User"
        }
        userEmail = when {
            firebaseEmail?.isNotBlank() == true -> firebaseEmail
            backendUser?.email?.isNotBlank() == true -> backendUser.email
            personalInfo.email.isNotBlank() -> personalInfo.email
            else -> "dutypein@gmail.com"
        }
    }

    // Status bar color
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        delay(200)
        isVisible = true
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            Timber.d("📸 WORKER PROFILE: Image picker result - uri: $uri")
            uri?.let { selectedUri ->
                Timber.d("📸 WORKER PROFILE: Selected image URI: $selectedUri")
                profileImageUri = selectedUri
                isUploadingImage = true
                
                // Upload image to Firebase Storage and update profile
                scope.launch {
                    try {
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        Timber.d("📸 WORKER PROFILE: Current user: ${currentUser?.uid}")
                        if (currentUser != null) {
                            // Upload to Firebase Storage
                            Timber.d("📸 WORKER PROFILE: Starting upload...")
                            val uploadResult = profileCompletionViewModel.uploadProfileImage(selectedUri, currentUser.uid, "worker")
                            uploadResult.fold(
                                onSuccess = { imageUrl ->
                                    profileImageUrl = imageUrl
                                    Timber.i("📸 WORKER PROFILE: ✅ Profile image uploaded: $imageUrl")
                                    
                                    // Update worker profile data with image URL
                                    val updatedProfileData = mapOf(
                                        "profileImageUrl" to imageUrl,
                                        "updatedAt" to System.currentTimeMillis()
                                    )
                                    profileCompletionViewModel.saveWorkerProfileData(updatedProfileData)
                                },
                                onFailure = { exception ->
                                    Timber.e(exception, "Failed to upload profile image")
                                }
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error uploading profile image")
                    } finally {
                        isUploadingImage = false
                    }
                }
            }
        }

    // WhatsApp sharing function
    val shareToWhatsApp = {
        val packageManager = context.packageManager
        val appPackageName = context.packageName
        
        try {
            // Try to open WhatsApp directly
            val whatsappIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (whatsappIntent != null) {
                // Create sharing intent for WhatsApp
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, 
                        "Check out this amazing job app! Download DutyPe and find your dream job.\n\n" +
                        "Download link: https://play.google.com/store/apps/details?id=$appPackageName"
                    )
                    setPackage("com.whatsapp")
                }
                context.startActivity(shareIntent)
            } else {
                // WhatsApp not installed, open in browser
                val browserIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20https://play.google.com/store/apps/details?id=$appPackageName")
                )
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Fallback to browser
            val browserIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20https://play.google.com/store/apps/details?id=$appPackageName")
            )
            context.startActivity(browserIntent)
        }
    }

    // Settings-style layout with white background
    // Show shimmer while loading, then show actual content
    if (isLoadingProfile || profileUiState.isLoading) {
        ProfileShimmer()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Settings Title with Refer button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
            )
            
            // Refer button with WhatsApp icon (green background)
            Button(
                onClick = { shareToWhatsApp() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.whatsapp),
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(21.dp),
                        tint = Color.White
                    )
                    Text(
                        text = "Refer",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // User Profile Section (like in the image)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Profile info (clickable)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { rootNavController.navigate(Routes.WORKER_PROFILE_DETAILS) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier.size(60.dp)
                ) {
                    Image(
                        painter = when {
                            isUploadingImage -> painterResource(id = R.drawable.user) // Show default while uploading
                            profileImageUri != null -> rememberAsyncImagePainter(profileImageUri)
                            profileImageUrl != null -> rememberAsyncImagePainter(profileImageUrl)
                            else -> painterResource(id = R.drawable.user)
                        },
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                            .clickable { imagePickerLauncher.launch("image/*") }
                    )
                    
                    // Show loading indicator when uploading
                    if (isUploadingImage) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // User Info
                Column {
                    Text(
                        text = userName.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    val firebasePhone = firebaseProfileData?.get("phone") as? String
                    val phoneNumber = when {
                        firebasePhone?.isNotBlank() == true -> firebasePhone
                        backendUser?.getPhoneDisplay()?.isNotBlank() == true -> backendUser.getPhoneDisplay()
                        personalInfo.phone.isNotBlank() -> personalInfo.phone
                        else -> ""
                    }
                    if (!phoneNumber.isNullOrEmpty()) {
                        Text(
                            text = phoneNumber,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray
                            )
                        )
                    }
                }
            }
            
            // Right side - Arrow only
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // App Settings Section
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Settings Menu Items with black icons
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    onClick = { localNavController?.navigate(Routes.WORKER_NOTIFICATION_SETTINGS) ?: rootNavController.navigate(Routes.WORKER_NOTIFICATION_SETTINGS) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.History,
                    title = "Application History",
                    onClick = { localNavController?.navigate(Routes.WORKER_HISTORY) ?: rootNavController.navigate(Routes.WORKER_HISTORY) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Support,
                    title = "Help & Support",
                    onClick = { localNavController?.navigate(Routes.HELP) ?: rootNavController.navigate(Routes.HELP) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Info,
                    title = "About Us",
                    onClick = { localNavController?.navigate(Routes.ABOUT_US) ?: rootNavController.navigate(Routes.ABOUT_US) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    onClick = { localNavController?.navigate(Routes.PRIVACY) ?: rootNavController.navigate(Routes.PRIVACY) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Gavel,
                    title = "Terms & Conditions",
                    onClick = { localNavController?.navigate(Routes.TERMS) ?: rootNavController.navigate(Routes.TERMS) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Security,
                    title = "Security",
                    onClick = { localNavController?.navigate(Routes.SECURITY) ?: rootNavController.navigate(Routes.SECURITY) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Feedback,
                    title = "Send Feedback",
                    onClick = { showFeedbackSheet = true }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsMenuItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Log Out",
                    onClick = { showLogoutDialog = true },
                    isDestructive = true
                )
            }
        }
    }
    } // End of else block for loading check

    // Dialogs
    if (showEditDialog) {
        ModernEditDialog(
            userName = userName,
            userEmail = userEmail,
            personalInfo = personalInfo,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail, updatedPersonalInfo ->
                scope.launch {
                    try {
                        // Update local variables
                        userName = newName
                        userEmail = newEmail
                        personalInfo = updatedPersonalInfo
                        
                        // Save to DataStore
                        dataStore.savePersonalInfo(updatedPersonalInfo)
                        
                        // Save to Firebase
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val workerProfileData = mapOf(
                                "fullName" to newName,
                                "email" to newEmail,
                                "phone" to updatedPersonalInfo.phone,
                                "address" to updatedPersonalInfo.address,
                                "dateOfBirth" to updatedPersonalInfo.dateOfBirth,
                                "gender" to updatedPersonalInfo.gender,
                                "skills" to skills,
                                "experience" to experience,
                                "updatedAt" to System.currentTimeMillis()
                            )
                            
                            profileCompletionViewModel.saveWorkerProfileData(workerProfileData)
                            Timber.i("Worker profile updated successfully in Firebase")
                            
                            // Refresh the profile data from Firebase to show updated values
                            try {
                                val refreshedData = profileCompletionViewModel.getWorkerProfileData(currentUser.uid)
                                refreshedData.fold(
                                    onSuccess = { data ->
                                    val refreshedPersonalInfo = personalInfo.copy(
                                        fullName = data["fullName"] as? String ?: personalInfo.fullName,
                                        email = data["email"] as? String ?: personalInfo.email,
                                        phone = data["phone"] as? String ?: personalInfo.phone,
                                        address = data["address"] as? String ?: personalInfo.address,
                                        dateOfBirth = data["dateOfBirth"] as? String ?: personalInfo.dateOfBirth,
                                        gender = data["gender"] as? String ?: personalInfo.gender
                                    )
                                    
                                    // Update local state with refreshed data
                                    personalInfo = refreshedPersonalInfo
                                    userName = refreshedPersonalInfo.fullName
                                    userEmail = refreshedPersonalInfo.email
                                    
                                    Timber.i("Worker profile refreshed with updated data")
                                    },
                                    onFailure = { exception ->
                                        Timber.e(exception, "Error refreshing worker profile data")
                                    }
                                )
                            } catch (refreshError: Exception) {
                                Timber.w(refreshError, "Could not refresh profile data")
                            }
                        }
                        
                        showEditDialog = false
                    } catch (e: Exception) {
                        Timber.e(e, "Error updating worker profile")
                        // Still close dialog even if Firebase save fails
                        showEditDialog = false
                    }
                }
            }
        )
    }

    if (showLogoutDialog) {
        ProfessionalLogoutDialog(
            isVisible = showLogoutDialog,
            onDismiss = { showLogoutDialog = false },
            navController = rootNavController,
            userRole = "Worker",
            authManager = authManager,
            googleSignInManager = googleSignInManager,
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }
    
    // Feedback Bottom Sheet
    com.example.dutype.components.FeedbackBottomSheet(
        isVisible = showFeedbackSheet,
        onDismiss = { showFeedbackSheet = false },
        userRole = "worker"
    )
}

@Composable
private fun InstagramStyleProfileHeader(
    profileImageUri: Uri?,
    userName: String,
    userEmail: String,
    profileCompletion: Int,
    onImageClick: () -> Unit,
    onEditClick: () -> Unit,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Top row: Profile picture + Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture (Instagram style)
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Profile image
                    Image(
                        painter = if (profileImageUri != null)
                            rememberAsyncImagePainter(profileImageUri)
                        else
                            painterResource(id = R.drawable.user),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .clickable { onImageClick() }
                    )

                    // Camera icon overlay (small)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(27.dp)
                            .background(Color(0xFF3B82F6), CircleShape)
                            .clickable { onImageClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Stats Row (Instagram style)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        number = "24",
                        label = "Applications"
                    )
                    StatItem(
                        number = "8",
                        label = "Interviews"
                    )
                    StatItem(
                        number = "3",
                        label = "Offers"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Info Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Username
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Email
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
                
                // Edit Icon
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationFormDataSection(
    dataStore: ApplicationFormDataStore,
    backendUser: com.example.dutype.models.User? = null
) {
    val personalInfo = remember { dataStore.getPersonalInfo() }
    val experience = remember { dataStore.getExperience() }
    val skills = remember { dataStore.getSkills() }
    val coverLetter = remember { dataStore.getCoverLetter() }
    val isFormCompleted = remember { dataStore.isFormCompleted() }
    
    if (isFormCompleted || personalInfo.fullName.isNotBlank() || backendUser != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Section Header
            Text(
                text = "Profile Information",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Personal Information Card - Use backend data if available, otherwise dataStore
            if (backendUser?.fullName?.isNotBlank() == true || personalInfo.fullName.isNotBlank()) {
                ApplicationDataCard(
                    title = "Personal Information",
                    icon = Icons.Default.Person,
                    items = listOf(
                        "Name" to (backendUser?.fullName ?: personalInfo.fullName),
                        "Email" to (backendUser?.email ?: personalInfo.email),
                        "Phone" to (backendUser?.getPhoneDisplay() ?: personalInfo.phone),
                        "Address" to (backendUser?.getAddressDisplay() ?: personalInfo.address),
                        "Date of Birth" to (backendUser?.dateOfBirth ?: personalInfo.dateOfBirth),
                        "Gender" to (backendUser?.gender ?: personalInfo.gender)
                    ).filter { it.second.isNotBlank() }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Experience Card
            if (experience.isNotEmpty()) {
                ApplicationDataCard(
                    title = "Work Experience",
                    icon = Icons.Default.Work,
                    items = experience.mapIndexed { index, exp ->
                        "Experience ${index + 1}" to "${exp.position} at ${exp.company}"
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Skills Card - Use backend data if available, otherwise dataStore
            val displaySkills = backendUser?.skills ?: skills.joinToString(", ")
            if (displaySkills.isNotBlank()) {
                ApplicationDataCard(
                    title = "Skills",
                    icon = Icons.Default.Star,
                    items = listOf("Skills" to displaySkills)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Cover Letter Card - Use backend data if available, otherwise dataStore
            val displayCoverLetter = backendUser?.coverLetter ?: coverLetter
            if (displayCoverLetter.isNotBlank()) {
                ApplicationDataCard(
                    title = "Cover Letter",
                    icon = Icons.Default.Description,
                    items = listOf("Cover Letter" to displayCoverLetter.take(100) + if (displayCoverLetter.length > 100) "..." else "")
                )
            }
        }
    }
}

@Composable
private fun ApplicationDataCard(
    title: String,
    icon: ImageVector,
    items: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Card Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Card Items
        items.forEach { (label, value) ->
            if (value.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$label:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280)
                        ),
                        modifier = Modifier.width(80.dp)
                    )
                    
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF374151)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (items.indexOf(label to value) < items.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    number: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF6B7280)
            )
        )
    }
}

@Composable
private fun ProfileCompletionProgress(
    profileCompletion: Int,
    dataStore: ApplicationFormDataStore
) {
    val applicationFormCompletion = remember { dataStore.getFormCompletionPercentage() }
    val isApplicationFormCompleted = remember { dataStore.isFormCompleted() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Progress bar header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile Completion",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            Text(
                text = "${profileCompletion}%",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6)
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    Color(0xFFE5E7EB),
                    RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(profileCompletion / 100f)
                    .background(
                        Color(0xFF3B82F6),
                        RoundedCornerShape(4.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress description
        Text(
            text = when {
                profileCompletion < 30 -> "Complete your basic information to get started"
                profileCompletion < 60 -> "Add more details to improve your profile visibility"
                profileCompletion < 90 -> "Almost there! Complete a few more sections"
                else -> "Excellent! Your profile is well-completed"
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF6B7280)
            )
        )
    }
}


@Composable
private fun FlatSettingsMenu(
    rootNavController: NavController,
    localNavController: NavController? = null,
    onLogoutClick: () -> Unit,
    profileCompletionViewModel: ProfileCompletionViewModel,
    scope: CoroutineScope,
    isVisible: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Essential Menu Items Only
        Text(
            text = "Account Management",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            FlatMenuItem(
                icon = Icons.Outlined.Person,
                title = "Complete Profile",
                subtitle = "Add professional details",
                onClick = { localNavController?.navigate(Routes.WORKER_PROFILE_DETAILS) ?: rootNavController.navigate(Routes.WORKER_PROFILE_DETAILS) },
                iconColor = Color(0xFF8B5CF6) // Purple for profile
            )

            FlatMenuItem(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                subtitle = "Manage your alerts",
                onClick = { localNavController?.navigate(Routes.WORKER_NOTIFICATIONS) ?: rootNavController.navigate(Routes.WORKER_NOTIFICATIONS) },
                iconColor = Color(0xFFEC4899) // Pink for notifications
            )


            FlatMenuItem(
                icon = Icons.Outlined.Info,
                title = "About Us",
                subtitle = "Learn more about our app",
                onClick = { localNavController?.navigate(Routes.ABOUT_US) ?: rootNavController.navigate(Routes.ABOUT_US) },
                iconColor = Color(0xFF3B82F6) // Blue for about
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Support Section
        Text(
            text = "Support",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            FlatMenuItem(
                icon = Icons.AutoMirrored.Outlined.Help,
                title = "Help & Support",
                subtitle = "Get assistance when needed",
                onClick = { localNavController?.navigate(Routes.HELP) ?: rootNavController.navigate(Routes.HELP) },
                iconColor = Color(0xFF10B981) // Teal for help
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Section
        FlatMenuItem(
            icon = Icons.AutoMirrored.Outlined.ExitToApp,
            title = "Log Out",
            subtitle = "Sign out of your account",
            onClick = onLogoutClick,
            isDestructive = true
        )
    }
}

@Composable
private fun FlatMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    iconColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor ?: if (isDestructive) Color(0xFFDC2626) else Color(0xFF059669),
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) Color(0xFFDC2626) else Color(0xFF1F2937)
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF059669),
            modifier = Modifier.size(28.dp)
        )
    }
}


@Composable
private fun ModernEditDialog(
    userName: String,
    userEmail: String,
    personalInfo: PersonalInfo,
    onDismiss: () -> Unit,
    onSave: (String, String, PersonalInfo) -> Unit
) {
    var newName by remember { mutableStateOf(userName) }
    var newEmail by remember { mutableStateOf(userEmail) }
    var newPhone by remember { mutableStateOf(personalInfo.phone) }
    var newAddress by remember { mutableStateOf(personalInfo.address) }
    var newDateOfBirth by remember { mutableStateOf(personalInfo.dateOfBirth) }
    var newGender by remember { mutableStateOf(personalInfo.gender) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)    
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF6B7280)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Scrollable content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                focusedLabelColor = Color(0xFF3B82F6)
                            )
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { /* Email cannot be changed */ },
                            label = { Text("Email Address") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF666666),
                                disabledBorderColor = Color(0xFFE0E0E0),
                                disabledLabelColor = Color(0xFF999999)
                            )
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { /* Phone cannot be changed */ },
                            label = { Text("Phone Number") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF666666),
                                disabledBorderColor = Color(0xFFE0E0E0),
                                disabledLabelColor = Color(0xFF999999)
                            ),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Phone number locked",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = newAddress,
                            onValueChange = { newAddress = it },
                            label = { Text("Address") },
                            leadingIcon = {
                                Icon(Icons.Default.Home, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                focusedLabelColor = Color(0xFF3B82F6)
                            )
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = newDateOfBirth,
                            onValueChange = { newDateOfBirth = it },
                            label = { Text("Date of Birth") },
                            placeholder = { Text("DD/MM/YYYY") },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                focusedLabelColor = Color(0xFF3B82F6)
                            )
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = newGender,
                            onValueChange = { newGender = it },
                            label = { Text("Gender") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                focusedLabelColor = Color(0xFF3B82F6)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF6B7280))
                    }
                    
                    Button(
                        onClick = { 
                            val updatedPersonalInfo = personalInfo.copy(
                                fullName = newName,
                                email = newEmail,
                                phone = newPhone,
                                address = newAddress,
                                dateOfBirth = newDateOfBirth,
                                gender = newGender
                            )
                            onSave(newName, newEmail, updatedPersonalInfo)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernLogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to log out? You'll need to sign in again to access your account.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280),
                    lineHeight = 22.sp
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Log Out")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) Color(0xFFDC2626) else Color(0xFF6B7280), // Gray for worker side - softer look
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) Color(0xFFDC2626) else Color(0xFF1F2937) // Dark gray text
            ),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF), // Light gray arrow
            modifier = Modifier.size(16.dp)
        )
    }
}


