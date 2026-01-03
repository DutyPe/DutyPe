package com.example.dutype.employer.screens.profilescreen

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
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.dutype.auth.AuthManager
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.components.ProfileRatingSection
import com.example.dutype.navigation.Routes
import com.example.dutype.components.ProfileShimmer
import com.example.dutype.services.RatingService
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.components.TrustBadge
import com.example.dutype.components.TrustBadgeSize
import com.example.dutype.models.parseTrustTier
import com.example.dutype.utils.LocaleHelper
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerProfileScreen(
    rootNavController: NavController,
    localNavController: NavController? = null,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    val screenBackgroundColor = Color(0xFFF8FAFC)
    LaunchedEffect(Unit) {
        onStatusBarColorChange?.invoke(screenBackgroundColor)
    }
    
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val context = LocalContext.current
    // Services accessed via ProfileCompletionViewModel (proper DI pattern)
    val authManager = profileCompletionViewModel.authManager
    val ratingService = profileCompletionViewModel.ratingService
    
    var companyName by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var employerTrustTier by remember { mutableStateOf("VERIFIED") }
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
                        companyPhone = data["contactPhone"] as? String ?: ""
                        profileImageUrl = data["profileImageUrl"] as? String
                        employerTrustTier = data["trustTier"] as? String ?: "VERIFIED"
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
            uri?.let { selectedUri ->
                profileImageUri = selectedUri
                isUploadingImage = true
                scope.launch {
                    try {
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val uploadResult = profileCompletionViewModel.uploadProfileImage(selectedUri, currentUser.uid, "employer")
                            uploadResult.fold(
                                onSuccess = { imageUrl -> profileImageUrl = imageUrl },
                                onFailure = { profileImageUri = null }
                            )
                        }
                    } catch (e: Exception) {
                        profileImageUri = null
                    } finally {
                        isUploadingImage = false
                    }
                }
            }
        }

    if (isLoadingProfile) {
        ProfileShimmer()
    } else {
        // Main Profile Screen
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundColor)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Profile Title
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Profile",
                    style = AppTypography.pageTitle.copy(color = Color.Black)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // ═══════════════════════════════════════════════════════════════
            // BUSINESS PROFILE SECTION (Flat Menu Item)
            // ═══════════════════════════════════════════════════════════════
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Profile as Flat Menu Item: Photo | Name + Phone | Arrow
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    localNavController?.navigate(Routes.EMPLOYER_COMPANY_DETAILS)
                                        ?: rootNavController.navigate(Routes.EMPLOYER_COMPANY_DETAILS)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Company Logo
                            Box(modifier = Modifier.size(56.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE5E7EB))
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
                                        !profileImageUrl.isNullOrBlank() -> {
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
                                                modifier = Modifier.size(28.dp)
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
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Company Name + Phone
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = companyName.ifEmpty { "Your Company" },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        ),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    // Trust Badge - clickable to see explanation
                                    TrustBadge(
                                        tier = parseTrustTier(employerTrustTier),
                                        size = TrustBadgeSize.SMALL,
                                        showLabel = true,
                                        modifier = Modifier.clickable {
                                            localNavController?.navigate(Routes.EMPLOYER_TRUST_BADGES)
                                                ?: rootNavController.navigate(Routes.EMPLOYER_TRUST_BADGES)
                                        }
                                    )
                                }
                                if (companyPhone.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = companyPhone,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFF6B7280)
                                        )
                                    )
                                }
                            }
                            
                            // Arrow
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Rating Section below profile
                        if (currentUserId.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFE5E7EB))
                            Spacer(modifier = Modifier.height(12.dp))
                            ProfileRatingSection(
                                userId = currentUserId,
                                isWorker = false,
                                ratingService = ratingService,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // ═══════════════════════════════════════════════════════════════
            // BUSINESS & JOBS SECTION
            // ═══════════════════════════════════════════════════════════════
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SectionHeader(title = "Business & Jobs")
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        ProfileMenuItem(
                            icon = Icons.Default.Verified,
                            title = "Trust Badges",
                            subtitle = "View your verification status",
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_TRUST_BADGES) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_TRUST_BADGES) 
                            }
                        )
                        
                        ProfileMenuItem(
                            icon = Icons.Default.CardMembership,
                            title = "Subscription",
                            subtitle = "Manage your plan",
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_SUBSCRIPTION) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_SUBSCRIPTION) 
                            }
                        )
                        
                        ProfileMenuItem(
                            icon = Icons.Default.Work,
                            title = "My Job Posts",
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_HISTORY) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_HISTORY) 
                            }
                        )

                        ProfileMenuItem(
                            icon = Icons.Default.LocationOn,
                            title = "Work Locations",
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) 
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // ═══════════════════════════════════════════════════════════════
            // PREFERENCES & SUPPORT SECTION
            // ═══════════════════════════════════════════════════════════════
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SectionHeader(title = "Preferences & Support")
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        ProfileMenuItem(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_NOTIFICATION_SETTINGS) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_NOTIFICATION_SETTINGS) 
                            }
                        )
                        
                        ProfileMenuItem(
                            icon = Icons.Default.Language,
                            title = if (LocaleHelper.isTelugu(context)) "భాష" else "Language",
                            onClick = { 
                                localNavController?.navigate(Routes.LANGUAGE_SELECTION) 
                                    ?: rootNavController.navigate(Routes.LANGUAGE_SELECTION) 
                            }
                        )
                        
                        ProfileMenuItem(
                            icon = Icons.Default.Chat,
                            title = "Messages",
                            onClick = { 
                                localNavController?.navigate(Routes.CHAT_CONVERSATIONS) 
                                    ?: rootNavController.navigate(Routes.CHAT_CONVERSATIONS) 
                            }
                        )
                        
                        ProfileMenuItem(
                            icon = Icons.Default.SmartToy,
                            title = "AI Assistant",
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_AI_CHAT) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_AI_CHAT) 
                            }
                        )
                        
                        ProfileMenuItem(
                            icon = Icons.Default.Help,
                            title = "Help & Support",
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_HELP) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_HELP) 
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // ═══════════════════════════════════════════════════════════════
            // MORE SETTINGS BUTTON
            // ═══════════════════════════════════════════════════════════════
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            localNavController?.navigate(Routes.EMPLOYER_MORE_SETTINGS)
                                ?: rootNavController.navigate(Routes.EMPLOYER_MORE_SETTINGS)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "More Settings",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1F2937)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        ProfessionalLogoutDialog(
            isVisible = showLogoutDialog,
            onDismiss = { showLogoutDialog = false },
            navController = rootNavController,
            userRole = "Employer",
            authManager = authManager,
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

// ═══════════════════════════════════════════════════════════════
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTypography.sectionHeader.copy(color = Color.Black)
    )
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) Color(0xFFDC2626) else Color(0xFF374151),
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
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(20.dp)
        )
    }
}
