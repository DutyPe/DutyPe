package com.example.dutype.worker.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.dutype.app.R
import com.example.dutype.auth.AuthManager
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.components.ProfileShimmer
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.navigation.Routes
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.viewmodels.ProfileViewModel
import com.example.dutype.worker.models.PersonalInfo
import kotlinx.coroutines.CoroutineScope
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
                                    android.widget.Toast.makeText(context, "Profile photo updated!", android.widget.Toast.LENGTH_SHORT).show()
                                    
                                    // Update worker profile data with image URL
                                    val updatedProfileData = mapOf(
                                        "profileImageUrl" to imageUrl,
                                        "updatedAt" to System.currentTimeMillis()
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
                .background(com.example.dutype.ui.theme.WorkerColors.ScreenBackground)
        ) {
        // Header using CommonHeader (no back button for profile)
        com.example.dutype.components.CommonHeader(
            title = "Profile",
            showBackButton = false,
            backgroundColor = com.example.dutype.ui.theme.WorkerColors.CardBackground,
            titleColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 0.dp)
        ) {
        // User Profile Card
        item {
            val isLoggedIn = currentUserId.isNotEmpty()
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
                ),
                shape = RoundedCornerShape(0.dp),
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
                                coil.compose.SubcomposeAsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(profileImageUri)
                                        .crossfade(true)
                                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = "Profile Picture",
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
                                        },
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF3F4F6)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF3F4F6)),
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
                                )
                            }
                            !profileImageUrl.isNullOrBlank() -> {
                                coil.compose.SubcomposeAsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(profileImageUrl)
                                        .crossfade(true)
                                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = "Profile Picture",
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
                                        },
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF3F4F6)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF3F4F6)),
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
                                )
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
                            // Show Sign up button when not logged in - Black for Worker
                            Button(
                                onClick = { rootNavController.navigate(Routes.ENHANCED_LOGIN) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1F2937)  // Dark/Black for Worker
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = "Login",
                                    style = com.example.dutype.ui.theme.AppTypography.buttonMedium.copy(
                                        color = Color.White
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "View and update your profile details",
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        // Quick Actions Row (Help Centre, Change Language) - Meesho style
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
                ),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Help Centre Button - phone_in_talk icon
                    QuickActionButtonDrawable(
                        iconRes = R.drawable.phone_in_talk_24,
                        title = "Help Centre",
                        modifier = Modifier.weight(1f),
                        iconTint = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                        onClick = { localNavController?.navigate(Routes.HELP) ?: rootNavController.navigate(Routes.HELP) }
                    )
                    
                    // Change Language Button - translate_indic icon
                    val currentLanguage = LocaleHelper.getLanguage(context)
                    QuickActionButtonDrawable(
                        iconRes = R.drawable.translate_indic_24,
                        title = if (currentLanguage == LocaleHelper.LANGUAGE_TELUGU) "భాష మార్చు" else "Change Language",
                        modifier = Modifier.weight(1f),
                        iconTint = Color(0xFFE91E63),  // Pink/magenta color like Meesho's language icon
                        onClick = { showLanguageBottomSheet = true }
                    )
                }
            }
        }
        
        // My Activity Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
                ),
                shape = RoundedCornerShape(0.dp),
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
                    
                    MenuDivider()
                    
                    MeeshoMenuItem(
                        icon = Icons.Outlined.Badge,
                        title = "My Visiting Card",
                        onClick = { 
                            if (currentUserId.isEmpty()) {
                                pendingMenuAction = "visiting_card"
                                showLoginBottomSheet = true
                            } else {
                                localNavController?.navigate(Routes.WORKER_VISITING_CARD) ?: rootNavController.navigate(Routes.WORKER_VISITING_CARD) 
                            }
                        }
                    )
                }
            }
        }
        
        // Rewards Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
                ),
                shape = RoundedCornerShape(0.dp),
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
                ),
                shape = RoundedCornerShape(0.dp),
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
                    
                    // Switch to Employer Role - COMMENTED OUT FOR PRODUCTION
                    // MeeshoMenuItem(
                    //     icon = Icons.Outlined.Work,
                    //     title = "Switch to Employer",
                    //     onClick = { 
                    //         rootNavController.navigate(Routes.EMPLOYER_HOME) {
                    //             popUpTo(Routes.WORKER_HOME) { inclusive = true }
                    //         }
                    //     }
                    // )
                    
                    // MenuDivider()
                    
                    // About Us - Available without login
                    MeeshoMenuItem(
                        icon = Icons.Outlined.Info,
                        title = "About Us",
                        onClick = { localNavController?.navigate(Routes.ABOUT_US) ?: rootNavController.navigate(Routes.ABOUT_US) }
                    )
                    
                    MenuDivider()
                    
                    // Typography Showcase (Dev Tool) - COMMENTED OUT FOR PRODUCTION
                    // MeeshoMenuItem(
                    //     icon = Icons.Outlined.TextFields,
                    //     title = "Typography Showcase",
                    //     onClick = { localNavController?.navigate(Routes.TYPOGRAPHY_SHOWCASE) ?: rootNavController.navigate(Routes.TYPOGRAPHY_SHOWCASE) }
                    // )
                    
                    // MenuDivider()
                    
                    MeeshoMenuItem(
                        icon = Icons.Outlined.Security,
                        title = "Security & Legal",
                        onClick = { localNavController?.navigate(Routes.SECURITY_LEGAL) ?: rootNavController.navigate(Routes.SECURITY_LEGAL) }
                    )
                    
                    // Only show logout when logged in
                    if (currentUserId.isNotEmpty()) {
                        MenuDivider()
                        
                        MeeshoMenuItem(
                            icon = Icons.AutoMirrored.Outlined.ExitToApp,
                            title = "Log Out",
                            isDestructive = true,
                            onClick = { showLogoutDialog = true }
                        )
                    }
                }
            }
        }
        
        // Follow Us Section
        item {
            FollowUsSection()
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
private fun InstagramStyleProfileHeader(
    profileImageUri: Uri?,
    profileImageUrl: String?,
    userName: String,
    userEmail: String,
    profileCompletion: Int,
    onImageClick: () -> Unit,
    onEditClick: () -> Unit,
    isVisible: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
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
                    // Profile image with proper loading/error handling
                    when {
                        profileImageUri != null -> {
                            coil.compose.SubcomposeAsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(profileImageUri)
                                    .crossfade(true)
                                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .clickable { onImageClick() },
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFF3F4F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFF3F4F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Default Profile",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            )
                        }
                        !profileImageUrl.isNullOrBlank() -> {
                            coil.compose.SubcomposeAsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(profileImageUrl)
                                    .crossfade(true)
                                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .clickable { onImageClick() },
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFF3F4F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFF3F4F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Default Profile",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            )
                        }
                        else -> {
                            // Default - show Person icon
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3F4F6))
                                    .clickable { onImageClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Profile",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    // Camera icon overlay (small)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(27.dp)
                            .background(Color(0xFF1F2937), CircleShape)
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
    var personalInfo by remember { mutableStateOf(com.example.dutype.worker.models.PersonalInfo()) }
    var experience by remember { mutableStateOf<List<com.example.dutype.models.WorkExperience>>(emptyList()) }
    var skills by remember { mutableStateOf<List<String>>(emptyList()) }
    var coverLetter by remember { mutableStateOf("") }
    var isFormCompleted by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        personalInfo = dataStore.getPersonalInfo()
        experience = dataStore.getExperience()
        skills = dataStore.getSkills()
        coverLetter = dataStore.getCoverLetter()
        isFormCompleted = dataStore.isFormCompleted()
    }
    
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
    // Use profileCompletion passed from parent instead of calling removed getFormCompletionPercentage
    var isApplicationFormCompleted by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isApplicationFormCompleted = dataStore.isFormCompleted()
    }
    
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
                    color = Color(0xFF1F2937)
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
                        Color(0xFF1F2937),
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
                iconColor = Color(0xFF1F2937) // Blue for about
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
                title = "Help & Info",
                subtitle = "Get assistance when needed",
                onClick = { localNavController?.navigate(Routes.HELP) ?: rootNavController.navigate(Routes.HELP) },
                iconColor = Color(0xFF1F2937) // Teal for help
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
    subtitle: String? = null,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) Color(0xFFDC2626) else Color(0xFF374151), // Darker icon color
            modifier = Modifier.size(24.dp)
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
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(16.dp)
        )
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
                else -> com.example.dutype.ui.theme.WorkerColors.IconPrimary
            },
            modifier = Modifier.size(24.dp)
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
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Meesho-style quick action button (Help Centre, Change Language)
 * Clean bordered box with no background fill
 */
@Composable
private fun QuickActionButton(
    icon: ImageVector,
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
                imageVector = icon,
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



@Composable
private fun AnimatedCarouselReferButton(
    onWhatsAppClick: () -> Unit,
    onInstagramClick: () -> Unit
) {
    // State to track which platform is currently shown (0 = WhatsApp, 1 = Instagram)
    var currentPlatform by remember { mutableStateOf(0) }
    
    // Auto-switch between platforms every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentPlatform = (currentPlatform + 1) % 2
        }
    }
    
    // Colors based on current platform
    val whatsAppColor = Color(0xFF25D366)
    val instagramColors = listOf(
        Color(0xFFF58529),
        Color(0xFFDD2A7B),
        Color(0xFF8134AF),
        Color(0xFF515BD4)
    )
    
    val onClick = if (currentPlatform == 0) onWhatsAppClick else onInstagramClick
    
    Box(
        modifier = Modifier.height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner button content
        Row(
            modifier = Modifier
                .background(
                    brush = if (currentPlatform == 0) {
                        Brush.linearGradient(listOf(whatsAppColor, whatsAppColor))
                    } else {
                        Brush.linearGradient(instagramColors)
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Single icon - changes based on platform
            Image(
                painter = painterResource(
                    id = if (currentPlatform == 0) R.drawable.whatsapp else R.drawable.instagram
                ),
                contentDescription = if (currentPlatform == 0) "WhatsApp" else "Instagram",
                modifier = Modifier.size(18.dp),
                colorFilter = if (currentPlatform == 0) {
                    androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
                } else null
            )
            
            Text(
                text = "Refer",
                style = com.example.dutype.ui.theme.AppTypography.labelMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}


/**
 * Digital Visiting Card Banner - Viral Growth Feature
 * "DutyPe gives you an Identity" - Professional visiting card for workers
 */
@Composable
private fun DigitalVisitingCardBanner(
    onClick: () -> Unit
) {
    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "banner_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A), // Dark Blue
                            Color(0xFF3B82F6), // Blue
                            Color(0xFF8B5CF6)  // Purple
                        )
                    )
                )
        ) {
            // Shimmer effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            start = androidx.compose.ui.geometry.Offset(
                                x = shimmerOffset * 800f - 200f,
                                y = 0f
                            ),
                            end = androidx.compose.ui.geometry.Offset(
                                x = shimmerOffset * 800f + 100f,
                                y = 200f
                            )
                        )
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Visiting Card",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Share on WhatsApp & get more jobs! 🚀",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
                
                // Arrow
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun FollowUsSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val whatsAppChannelUrl = "https://whatsapp.com/channel/0029VbBdNOQ1iUxZMmvg8t2G"
    val instagramUrl = "https://www.instagram.com/dutype.in"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                fontWeight = FontWeight.SemiBold,
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
                        tint = Color(0xFFE4405F),
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
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialMediaIcon(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            fontSize = 11.sp,
            color = com.example.dutype.ui.theme.WorkerColors.TextSecondary
        )
    }
}