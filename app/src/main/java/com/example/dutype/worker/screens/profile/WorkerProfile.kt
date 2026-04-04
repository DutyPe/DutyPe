package com.example.dutype.worker.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dutype.app.R
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.components.ProfileShimmer
import com.example.dutype.components.RoleSwitchDialog
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.viewmodels.ProfileViewModel
import com.example.dutype.worker.models.PersonalInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profileUiState by profileViewModel.uiState.collectAsState()
    // Services accessed via ProfileCompletionViewModel (proper DI pattern)
    val authManager = profileCompletionViewModel.authManager
    val profileCompletionService = profileCompletionViewModel.profileCompletionService
    var currentUserId by remember { mutableStateOf("") }
    
    // Auth validation - ensure unauthenticated users cannot access profile actions
    LaunchedEffect(Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Timber.w("Worker Profile - User not authenticated, staying in guest profile mode")
        } else {
            Timber.i("Worker Profile - User authenticated: ${currentUser.uid}")
        }
    }
    
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
    var showLanguageBottomSheet by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Guest mode - Login bottom sheet state
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    var pendingMenuAction by remember { mutableStateOf<String?>(null) }

    // LIGHTWEIGHT PROFILE: Use metadata for basic profile info (name, phone, image)
    // Full profile data loads only in profile details screen
    val userStats by profileCompletionViewModel.metadataManager.userMetadata.userStats.collectAsState()
    
    // Get profile data from dataStore - using state with LaunchedEffect for suspend functions
    var personalInfo by remember { mutableStateOf(com.example.dutype.worker.models.PersonalInfo()) }
    var experience by remember { mutableStateOf<List<com.example.dutype.models.WorkExperience>>(emptyList()) }
    var skills by remember { mutableStateOf<List<String>>(emptyList()) }
    var coverLetter by remember { mutableStateOf("") }
    var isFormCompleted by remember { mutableStateOf(false) }
    
    // Load dataStore data in coroutine (lightweight - local storage only)
    LaunchedEffect(Unit) {
        personalInfo = dataStore.getPersonalInfo()
        isFormCompleted = dataStore.isFormCompleted()
        // NOTE: experience, skills, coverLetter removed from main profile screen
        // These load only in profile details screen for performance
    }
    
    // DON'T load full profile from Firebase on main profile screen
    // Use metadata instead for lightweight display
    // LaunchedEffect(Unit) { profileViewModel.loadProfile() } // REMOVED for performance
    
    // Use backend profile data if available, otherwise fallback to dataStore
    val backendUser = profileUiState.user
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var profileSetupStatus by remember { mutableStateOf<com.example.dutype.state.ProfileSetupStatus?>(null) }
    var profileCompletion by remember { mutableStateOf(0) }
    
    // Firebase profile data state for reactive updates
    var firebaseProfileData by remember { mutableStateOf<Map<String, Any?>?>(null) }
    
    // REMOVED: Heavy profile completion calculation from main screen
    // This now happens only in profile details screen
    
    // LIGHTWEIGHT: Load only basic profile info using metadata
    LaunchedEffect(Unit) {
        isLoadingProfile = true
        try {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                currentUserId = currentUser.uid
                
                // LIGHTWEIGHT: Only load basic profile (name, phone, image) - no heavy stats
                profileCompletionViewModel.metadataManager.userMetadata.loadBasicProfile()
                
                // Use metadata for profile image URL
                profileImageUrl = userStats.profileImageUrl.ifEmpty { null }
                
                Timber.i("Worker profile (lightweight) - Name: ${userStats.fullName}, Phone: ${userStats.phone}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading lightweight profile")
        } finally {
            isLoadingProfile = false
        }
    }
    
    // Update userName from metadata (lightweight)
    LaunchedEffect(userStats, personalInfo) {
        userName = when {
            userStats.fullName.isNotBlank() -> userStats.fullName
            personalInfo.fullName.isNotBlank() -> personalInfo.fullName
            else -> "User"
        }
        // Update profile image from metadata
        if (userStats.profileImageUrl.isNotBlank() && profileImageUrl == null) {
            profileImageUrl = userStats.profileImageUrl
        }
    }

    // Status bar color - White for profile screen
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
                                    android.widget.Toast.makeText(context, "Profile photo updated!", android.widget.Toast.LENGTH_SHORT).show()
                                    
                                    // Update worker profile data with image URL
                                    val updatedProfileData = mapOf(
                                        "profileImageUrl" to imageUrl
                                    )
                                    profileCompletionViewModel.saveWorkerProfileData(updatedProfileData)
                                },
                                onFailure = { exception ->
                                    Timber.e(exception, "Failed to upload profile image")
                                    profileImageUri = null // Reset the local preview
                                    // Show user-friendly error message
                                    val errorMessage = when {
                                        exception.message?.contains("quota", ignoreCase = true) == true ||
                                        exception.message?.contains("billing", ignoreCase = true) == true ||
                                        exception.message?.contains("storage", ignoreCase = true) == true ->
                                            "Photo upload temporarily unavailable. Please try again later."
                                        exception.message?.contains("network", ignoreCase = true) == true ->
                                            "Network error. Please check your connection."
                                        else -> "Failed to upload photo. Please try again."
                                    }
                                    android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error uploading profile image")
                        profileImageUri = null // Reset the local preview
                        android.widget.Toast.makeText(context, "Failed to upload photo. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                    } finally {
                        isUploadingImage = false
                    }
                }
            }
        }

    // Play Store URL constant
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
    
    // WhatsApp sharing function
    val shareToWhatsApp = {
        val packageManager = context.packageManager
        
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
                        "Download link: $playStoreUrl"
                    )
                    setPackage("com.whatsapp")
                }
                context.startActivity(shareIntent)
            } else {
                // WhatsApp not installed, open in browser
                val browserIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20$playStoreUrl")
                )
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Fallback to browser
            val browserIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20$playStoreUrl")
            )
            context.startActivity(browserIntent)
        }
    }
    
    // Instagram sharing function
    val shareToInstagram = {
        val packageManager = context.packageManager
        
        try {
            // Try to open Instagram Stories or Feed
            val instagramIntent = packageManager.getLaunchIntentForPackage("com.instagram.android")
            if (instagramIntent != null) {
                // Create sharing intent for Instagram
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, 
                        "🚀 Found an amazing job app! DutyPe helps you find your dream job easily.\n\n" +
                        "📲 Download now: $playStoreUrl\n\n" +
                        "#DutyPe #Jobs #Career #Hiring"
                    )
                    setPackage("com.instagram.android")
                }
                context.startActivity(shareIntent)
            } else {
                // Instagram not installed, open in browser
                val browserIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.instagram.com/")
                )
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Fallback - open Play Store link
            val browserIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(playStoreUrl)
            )
            context.startActivity(browserIntent)
        }
    }

    // Settings-style layout with Meesho-style background
    // Show shimmer while loading, then show actual content
    if (isLoadingProfile || profileUiState.isLoading) {
        ProfileShimmer()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .background(Color.White)  // White background
        ) {
            // Offline banner at the very top
            val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
            val isOnline by connectivityViewModel.isOnline.collectAsState()
            com.example.dutype.components.OfflineBanner(isOffline = !isOnline)
            
            // Header using CommonHeader (no back button for profile)
            com.example.dutype.components.CommonHeader( 
                title = "Profile",
                showBackButton = false,
                backgroundColor = com.example.dutype.ui.theme.WorkerColors.CardBackground,
                titleColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        // Language Change Icon
                        val currentLanguage = LocaleHelper.getLanguage(context)
                        IconButton(onClick = { showLanguageBottomSheet = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.translate_indic_24),
                                contentDescription = if (currentLanguage == LocaleHelper.LANGUAGE_TELUGU) "భాష మార్చు" else "Change Language",
                                tint = Color(0xFFE91E63), // Pink/magenta color
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // WhatsApp Support Icon
                        IconButton(onClick = {
                            val whatsappNumber = "919121706236" // DutyPe support number
                            val message = "Hello DutyPe Team! I need help with DutyPe app."
                            val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
                            val whatsappUrl = "https://wa.me/$whatsappNumber?text=$encodedMessage"
                            
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse(whatsappUrl)
                                    setPackage("com.whatsapp")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // If WhatsApp is not installed, open in browser
                                val browserIntent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(whatsappUrl)
                                )
                                context.startActivity(browserIntent)
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = com.dutype.app.R.drawable.ic_whatsapp),
                                contentDescription = "WhatsApp Support",
                                tint = Color(0xFF25D366), // WhatsApp green color
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 0.dp)
        ) {
        // User Profile Card
        item {
            Spacer(modifier = Modifier.height(8.dp))
            val isLoggedIn = currentUserId.isNotEmpty()
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            if (isLoggedIn) {
                                rootNavController.navigate(Routes.WORKER_PROFILE_DETAILS)
                            } else {
                                pendingMenuAction = "profile"
                                showLoginBottomSheet = true
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Picture
                    Box(
                        modifier = Modifier.size(56.dp)
                    ) {
                        // Use SubcomposeAsyncImage for better loading/error handling
                        when {
                            isUploadingImage -> {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF3F4F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            profileImageUri != null -> {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .clickable { 
                                            if (isLoggedIn) {
                                                imagePickerLauncher.launch("image/*")
                                            } else {
                                                pendingMenuAction = "profile"
                                                showLoginBottomSheet = true
                                            }
                                        }
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
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .clickable { 
                                            if (isLoggedIn) {
                                                imagePickerLauncher.launch("image/*")
                                            } else {
                                                pendingMenuAction = "profile"
                                                showLoginBottomSheet = true
                                            }
                                        }
                                ) {
                                    com.example.dutype.components.OptimizedProfileImage(
                                        imageUrl = profileImageUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            else -> {
                                // Default - show Person icon
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF3F4F6))
                                        .clickable { 
                                            if (isLoggedIn) {
                                                imagePickerLauncher.launch("image/*")
                                            } else {
                                                pendingMenuAction = "profile"
                                                showLoginBottomSheet = true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Default Profile",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // User Info or Sign up button
                    Column(modifier = Modifier.weight(1f)) {
                        if (isLoggedIn) {
                            // LIGHTWEIGHT: Get phone number from metadata, then Firebase Auth as fallback
                            val authPhone = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
                            val userPhone = userStats.phone.ifBlank { personalInfo.phone }.ifBlank { authPhone }
                            val hasName = userName.isNotBlank() && userName != "User"
                            
                            if (hasName) {
                                // Show name (primary) and phone number (secondary)
                                Text(
                                    text = userName,
                                    style = com.example.dutype.ui.theme.AppTypography.pageTitle.copy(
                                        color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                                    ),
                                    maxLines = 1
                                )
                                if (userPhone.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = userPhone,
                                        style = com.example.dutype.ui.theme.AppTypography.bodySmall.copy(
                                            color = com.example.dutype.ui.theme.WorkerColors.TextSecondary
                                        )
                                    )
                                }
                            } else {
                                // No name set - show phone number as primary (bigger text)
                                if (userPhone.isNotBlank()) {
                                    Text(
                                        text = userPhone,
                                        style = com.example.dutype.ui.theme.AppTypography.cardTitle.copy(
                                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tap to add your name",
                                        style = com.example.dutype.ui.theme.AppTypography.bodySmall.copy(
                                            color = com.example.dutype.ui.theme.WorkerColors.TextSecondary
                                        )
                                    )
                                } else {
                                    // Fallback if no phone available (shouldn't happen for logged in users)
                                    Text(
                                        text = "Set up your profile",
                                        style = com.example.dutype.ui.theme.AppTypography.cardTitle.copy(
                                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tap to add your details",
                                        style = com.example.dutype.ui.theme.AppTypography.bodySmall.copy(
                                            color = com.example.dutype.ui.theme.WorkerColors.TextSecondary
                                        )
                                    )
                                }
                            }
                        } else {
                            // Show guest CTA when not logged in
                            Button(
                                onClick = { 
                                    // CRITICAL FIX: Pass role=WORKER to maintain role context after login
                                    rootNavController.navigate("${Routes.ENHANCED_LOGIN}?role=WORKER")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1F2937)  // Dark/Black for Worker
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text(
                                    text = "Log in / Sign up",
                                    style = com.example.dutype.ui.theme.AppTypography.buttonMedium.copy(
                                        color = Color.White
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "View and update your profile data",
                                style = com.example.dutype.ui.theme.AppTypography.bodySmall.copy(
                                    color = com.example.dutype.ui.theme.WorkerColors.TextSecondary
                                )
                            )
                        }
                    }
                    
                    if (isLoggedIn) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = com.example.dutype.ui.theme.WorkerColors.IconSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        // My Activity Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    // Section Header
                    Text(
                        text = "My Activity",
                        style = com.example.dutype.ui.theme.AppTypography.sectionHeader.copy(
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        ),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                    
                    MeeshoMenuItem(
                        icon = Icons.Outlined.Description,
                        title = "My Applications",
                        onClick = { 
                            if (currentUserId.isEmpty()) {
                                pendingMenuAction = "applications"
                                showLoginBottomSheet = true
                            } else {
                                localNavController?.navigate(Routes.WORKER_HISTORY) ?: rootNavController.navigate(Routes.WORKER_HISTORY) 
                            }
                        }
                    )
                    
                    MenuDivider()

                    MeeshoMenuItem(
                        icon = Icons.Outlined.Star,
                        title = "My Earnings",
                        onClick = { 
                            if (currentUserId.isEmpty()) {
                                pendingMenuAction = "earnings"
                                showLoginBottomSheet = true
                            } else {
                                localNavController?.navigate(Routes.WORKER_EARNINGS) ?: rootNavController.navigate(Routes.WORKER_EARNINGS) 
                            }
                        }
                    )
                    
                    // My Visiting Card menu item commented as requested
                    // MeeshoMenuItem(
                    //     icon = Icons.Outlined.Badge,
                    //     title = "My Visiting Card",
                    //     onClick = {
                    //         if (currentUserId.isEmpty()) {
                    //             pendingMenuAction = "visiting_card"
                    //             showLoginBottomSheet = true
                    //         } else {
                    //             localNavController?.navigate(Routes.WORKER_VISITING_CARD) ?: rootNavController.navigate(Routes.WORKER_VISITING_CARD)
                    //         }
                    //     }
                    // )
                }
            }
        }
        
        // Rewards Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    Text(
                        text = "Rewards",
                        style = com.example.dutype.ui.theme.AppTypography.sectionHeader.copy(
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        ),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                    
                    MeeshoMenuItem(
                        icon = Icons.Outlined.CardGiftcard,
                        title = "Refer & Earn",
                        badgeText = "New",
                        onClick = { 
                            // Refer & Earn requires login
                            if (currentUserId.isNotEmpty()) {
                                localNavController?.navigate(Routes.WORKER_REFER_EARN) ?: rootNavController.navigate(Routes.WORKER_REFER_EARN)
                            } else {
                                pendingMenuAction = "refer_earn"
                                showLoginBottomSheet = true
                            }
                        }
                    )
                }
            }
        }
        
        // Others Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    Text(
                        text = "Others",
                        style = com.example.dutype.ui.theme.AppTypography.sectionHeader.copy(
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        ),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                    
                    // Help & FAQs - First item
                    MeeshoMenuItem(
                        icon = Icons.Outlined.Phone,
                        title = "Help & FAQs",
                        onClick = { localNavController?.navigate(Routes.HELP) ?: rootNavController.navigate(Routes.HELP) }
                    )
                    
                    MenuDivider()
                    
                    // Role Management - Switch Role Support
                    if (currentUserId.isNotEmpty()) {
                        val roleManagementViewModel: com.example.dutype.viewmodels.RoleManagementViewModel = hiltViewModel()
                        val currentUser by roleManagementViewModel.currentUser.collectAsState()
                        var showRoleSwitchDialog by remember { mutableStateOf(false) }
                        var isRoleSwitching by remember { mutableStateOf(false) }
                        
                        // Show switch role for all logged-in users
                        MeeshoMenuItem(
                            icon = Icons.Outlined.SwapHoriz,
                            title = "Switch to Employer",
                            onClick = {
                                if (currentUser != null && currentUser!!.isDualRole()) {
                                    // Already has employer role enabled — switch directly
                                    showRoleSwitchDialog = true
                                } else {
                                    // Single role — navigate to employer home (will trigger profile setup if needed)
                                    showRoleSwitchDialog = true
                                }
                            }
                        )
                        
                        MenuDivider()

                        RoleSwitchDialog(
                            showDialog = showRoleSwitchDialog,
                            currentRole = currentUser?.activeRole ?: com.example.dutype.models.UserRole.WORKER,
                            enabledRoles = currentUser?.getEnabledRoles() ?: listOf(com.example.dutype.models.UserRole.WORKER),
                            isLoading = isRoleSwitching,
                            onDismiss = { showRoleSwitchDialog = false },
                            onSwitchRole = { targetRole ->
                                isRoleSwitching = true
                                val appContext = context.applicationContext as android.app.Application
                                val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                    appContext,
                                    com.example.dutype.managers.RoleSwitchManagerEntryPoint::class.java
                                )
                                val roleSwitchManager = entryPoint.roleSwitchManager()
                                scope.launch {
                                    try {
                                        roleSwitchManager.switchRole(
                                            context = context,
                                            navController = rootNavController,
                                            roleViewModel = roleManagementViewModel,
                                            oldRole = currentUser?.activeRole ?: com.example.dutype.models.UserRole.WORKER,
                                            newRole = targetRole,
                                            onSuccess = {
                                                isRoleSwitching = false
                                                showRoleSwitchDialog = false
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Switched to ${targetRole.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onError = { error ->
                                                isRoleSwitching = false
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Failed to switch: $error",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        )
                                    } catch (e: Exception) {
                                        isRoleSwitching = false
                                        android.widget.Toast.makeText(
                                            context,
                                            "Error: ${e.message}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                    
                    // About Us - Available without login
                    MeeshoMenuItem(
                        icon = Icons.Outlined.Info,
                        title = "About Us",
                        onClick = { localNavController?.navigate(Routes.ABOUT_US) ?: rootNavController.navigate(Routes.ABOUT_US) }
                    )
                    
                    MenuDivider()
                    
                }
            }
        }
        
        // Follow Us Section - COMMENTED OUT
        /*
        item {
            FollowUsSection()
        }
        */
        
        // Logout - Simple menu item below Follow Us
        item {
            if (currentUserId.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    MeeshoMenuItem(
                        icon = Icons.AutoMirrored.Outlined.ExitToApp,
                        title = "Log Out",
                        isDestructive = true,
                        onClick = { showLogoutDialog = true }
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
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
                                "phone" to updatedPersonalInfo.phone,
                                "skills" to skills  // mapped to jobTypes[] in worker_profiles by service
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
                                        phone = data["phone"] as? String ?: personalInfo.phone
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
    
    // Language Selection Bottom Sheet
    if (showLanguageBottomSheet) {
        com.example.dutype.components.LanguageSelectionBottomSheet(
            onDismiss = { showLanguageBottomSheet = false }
        )
    }
    
    // Guest Mode - Login Bottom Sheet
    com.example.dutype.components.LoginBottomSheet(
        isVisible = showLoginBottomSheet,
        onDismiss = { 
            showLoginBottomSheet = false
            pendingMenuAction = null
        },
        onLoginSuccess = {
            showLoginBottomSheet = false
            // Execute the pending action after successful login
            when (pendingMenuAction) {
                "profile" -> rootNavController.navigate(Routes.WORKER_PROFILE_DETAILS)
                "applications" -> localNavController?.navigate(Routes.WORKER_HISTORY) ?: rootNavController.navigate(Routes.WORKER_HISTORY)
                "earnings" -> localNavController?.navigate(Routes.WORKER_EARNINGS) ?: rootNavController.navigate(Routes.WORKER_EARNINGS)
                "visiting_card" -> localNavController?.navigate(Routes.WORKER_VISITING_CARD) ?: rootNavController.navigate(Routes.WORKER_VISITING_CARD)
                "refer_earn" -> localNavController?.navigate(Routes.WORKER_REFER_EARN) ?: rootNavController.navigate(Routes.WORKER_REFER_EARN)
            }
            pendingMenuAction = null
        },
        role = com.example.dutype.models.UserRole.WORKER,
        title = "Login Required",
        subtitle = when (pendingMenuAction) {
            "profile" -> "Login to view and edit your profile"
            "applications" -> "Login to view your job applications"
            "earnings" -> "Login to view your earnings"
            "visiting_card" -> "Login to create your digital visiting card"
            "refer_earn" -> "Login to refer friends and earn rewards"
            else -> "Please login to access this feature"
        }
    )
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
                                focusedBorderColor = Color(0xFF1F2937),
                                focusedLabelColor = Color(0xFF1F2937)
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
                                focusedBorderColor = Color(0xFF1F2937),
                                focusedLabelColor = Color(0xFF1F2937)
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
                                focusedBorderColor = Color(0xFF1F2937),
                                focusedLabelColor = Color(0xFF1F2937)
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
                                focusedBorderColor = Color(0xFF1F2937),
                                focusedLabelColor = Color(0xFF1F2937)
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
                            containerColor = Color(0xFF1F2937)
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

// ============================================
// MEESHO-STYLE COMPONENTS
// ============================================

/**
 * Meesho-style menu item with clean design - supports custom icon colors
 */
@Composable
private fun MeeshoMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    badgeText: String? = null,
    isDestructive: Boolean = false,
    iconColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with custom color support
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                isDestructive -> com.example.dutype.ui.theme.WorkerColors.Error
                iconColor != null -> iconColor
                else -> Color(0xFF374151) // Professional dark gray (gray-700) for profile icons
            },
            modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = com.example.dutype.ui.theme.AppTypography.menuItemTitle.copy(
                color = if (isDestructive) 
                    com.example.dutype.ui.theme.WorkerColors.Error 
                else 
                    com.example.dutype.ui.theme.WorkerColors.TextPrimary
            ),
            modifier = Modifier.weight(1f)
        )
        
        // Badge if present
        if (badgeText != null) {
            Text(
                text = badgeText,
                style = com.example.dutype.ui.theme.AppTypography.newBadge.copy(
                    color = com.example.dutype.ui.theme.WorkerColors.Primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = com.example.dutype.ui.theme.WorkerColors.IconSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Meesho-style quick action button using drawable resource
 * Clean bordered box with no background fill
 */
@Composable
private fun QuickActionButtonDrawable(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    iconTint: Color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent  // No background - Meesho style
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            com.example.dutype.ui.theme.WorkerColors.Border
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = com.example.dutype.ui.theme.AppTypography.quickActionLabel.copy(
                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                    fontFamily = com.example.dutype.ui.theme.MeeshoFontFamily
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Thin divider for menu items
 */
@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp) // Align with text after icon (16dp padding + 24dp icon + 16dp spacing)
            .height(1.dp)
            .background(com.example.dutype.ui.theme.WorkerColors.Divider)
    )
}


// COMMENTED OUT - Follow Us Section
/*
@Composable
private fun FollowUsSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val whatsAppChannelUrl = "https://whatsapp.com/channel/0029VbBdNOQ1iUxZMmvg8t2G"
    val instagramUrl = "https://www.instagram.com/dutype.in"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Follow Us On",
                fontSize = 16.sp,
                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Instagram
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                        .clickable {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(instagramUrl)
                            )
                            context.startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_instagram),
                        contentDescription = "Instagram",
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // WhatsApp
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                        .clickable {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(whatsAppChannelUrl)
                            )
                            context.startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
*/

// Role Management Menu Item
/**
 * Role Management Menu Item - Flat design matching other menu items
 * Shows current role with chevron icon for switching
 */
@Composable
private fun RoleManagementMenuItem(
    currentRole: com.example.dutype.models.UserRole,
    onSwitchClick: () -> Unit
) {
    val roleIcon = when (currentRole) {
        com.example.dutype.models.UserRole.WORKER -> Icons.Outlined.Person
        com.example.dutype.models.UserRole.EMPLOYER -> Icons.Default.Business
        else -> Icons.Outlined.Person
    }
    
    val roleColor = when (currentRole) {
        com.example.dutype.models.UserRole.WORKER -> Color(0xFF10B981) // Green
        com.example.dutype.models.UserRole.EMPLOYER -> Color(0xFF3B82F6) // Blue
        else -> Color(0xFF6B7280)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSwitchClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = roleIcon,
                contentDescription = "Role",
                tint = Color(0xFF4B5563), // Match other menu icons
                modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Switch Role",
                    style = com.example.dutype.ui.theme.AppTypography.menuItemTitle.copy(
                        color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Active: ",
                        style = com.example.dutype.ui.theme.AppTypography.menuItemSubtitle.copy(
                            color = com.example.dutype.ui.theme.WorkerColors.TextSecondary
                        )
                    )
                    Text(
                        text = currentRole.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = com.example.dutype.ui.theme.AppTypography.menuItemSubtitle.copy(
                            color = roleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
        
        // Chevron icon (matching other menu items)
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Switch",
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(24.dp)
        )
    }
}
