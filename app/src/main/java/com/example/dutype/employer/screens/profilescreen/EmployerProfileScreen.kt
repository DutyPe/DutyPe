package com.example.dutype.common.employer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.dutype.auth.AuthManager
import com.example.dutype.auth.GoogleSignInManager
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.components.ProfileRatingSection
import com.example.dutype.navigation.Routes
import com.example.dutype.components.ProfileShimmer
import com.example.dutype.services.RatingService
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Composable
fun EmployerProfileScreen(
    rootNavController: NavController,
    localNavController: NavController? = null,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    // Set status bar to white for this screen
    LaunchedEffect(Unit) {
        onStatusBarColorChange?.invoke(Color.White)
    }
    
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val context = LocalContext.current
    val authManager: AuthManager = remember { AuthManager(context) }
    val googleSignInManager: GoogleSignInManager = remember { GoogleSignInManager(context) }
    val ratingService: RatingService = remember { RatingService() }
    
    var companyName by remember { mutableStateOf("") }
    var companyEmail by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") } 
    var isLoadingProfile by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Load profile data
    LaunchedEffect(Unit) {
        isLoadingProfile = true
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            try {
                val employerProfileData = profileCompletionViewModel.getEmployerProfileData(currentUser.uid)
                employerProfileData.fold(
                    onSuccess = { data ->
                        companyName = data["companyName"] as? String ?: ""
                        companyEmail = data["contactEmail"] as? String ?: ""
                        companyPhone = data["contactPhone"] as? String ?: ""
                        companyAddress = data["businessAddress"] as? String ?: ""
                        profileImageUrl = data["profileImageUrl"] as? String
                        Timber.d("📸 EMPLOYER PROFILE: Loaded profile image URL: $profileImageUrl")
                    },
                    onFailure = { e ->
                        Timber.e("Error loading employer profile data: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Timber.e("Error loading profile: ${e.message}")
            }
        }
        isLoadingProfile = false
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            Timber.d("📸 EMPLOYER PROFILE: Image picker result - uri: $uri")
            uri?.let { selectedUri ->
                profileImageUri = selectedUri
                isUploadingImage = true
                
                scope.launch {
                    try {
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val uploadResult = profileCompletionViewModel.uploadProfileImage(selectedUri, currentUser.uid, "employer")
                            uploadResult.fold(
                                onSuccess = { imageUrl ->
                                    profileImageUrl = imageUrl
                                    Timber.i("📸 EMPLOYER PROFILE: ✅ Profile image uploaded: $imageUrl")
                                },
                                onFailure = { exception ->
                                    Timber.e(exception, "📸 EMPLOYER PROFILE: ❌ Failed to upload profile image")
                                    profileImageUri = null
                                }
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "📸 EMPLOYER PROFILE: ❌ Error uploading profile image")
                        profileImageUri = null
                    } finally {
                        isUploadingImage = false
                    }
                }
            }
        }

    // Show shimmer while loading, then show actual content
    if (isLoadingProfile) {
        ProfileShimmer()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(13.dp))
            
            // Profile Title
            Text(
                text = "Profile",
                style = com.example.dutype.ui.theme.AppTypography.pageTitle.copy(
                    color = Color.Black
                )
            )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Company Info Section (Clickable) - Navigate to Company Details screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { 
                    localNavController?.navigate(Routes.EMPLOYER_COMPANY_DETAILS) 
                        ?: rootNavController.navigate(Routes.EMPLOYER_COMPANY_DETAILS) 
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Company Logo - Clickable to upload image
            Box(
                modifier = Modifier.size(60.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isUploadingImage -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF3B82F6),
                                strokeWidth = 2.dp
                            )
                        }
                        profileImageUri != null -> {
                            AsyncImage(
                                model = profileImageUri,
                                contentDescription = "Company Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                        profileImageUrl != null && profileImageUrl!!.isNotBlank() -> {
                            AsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Company Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
                
                // Camera overlay
                if (!isUploadingImage) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .background(Color(0xFF3B82F6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Company Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = companyName.ifEmpty { "Your Company" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.dp))
                if (companyPhone.isNotEmpty()) {
                    Text(
                        text = companyPhone,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        )
                    )
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
            style = com.example.dutype.ui.theme.AppTypography.sectionHeader.copy(
                color = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Settings Menu Items
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Rating Section - Only shows if employer has ratings (as first menu item)
            if (currentUserId.isNotEmpty()) {
                item {
                    ProfileRatingSection(
                        userId = currentUserId,
                        isWorker = false,
                        ratingService = ratingService,
                        modifier = Modifier
                    )
                }
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.LocationOn,
                    title = "Manage Addresses",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) ?: rootNavController.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_NOTIFICATION_SETTINGS) ?: rootNavController.navigate(Routes.EMPLOYER_NOTIFICATION_SETTINGS) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.History,
                    title = "Job Posting History",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_HISTORY) ?: rootNavController.navigate(Routes.EMPLOYER_HISTORY) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Help,
                    title = "Help & Support",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_HELP) ?: rootNavController.navigate(Routes.EMPLOYER_HELP) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Info,
                    title = "About Us",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_ABOUT) ?: rootNavController.navigate(Routes.EMPLOYER_ABOUT) }
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

    // Logout Dialog
    if (showLogoutDialog) {
        ProfessionalLogoutDialog(
            isVisible = showLogoutDialog,
            onDismiss = { showLogoutDialog = false },
            navController = rootNavController,
            userRole = "Employer",
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
        userRole = "employer"
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
            tint = if (isDestructive) Color(0xFFDC2626) else Color(0xFF6B7280), // Gray for employer side - same as worker
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