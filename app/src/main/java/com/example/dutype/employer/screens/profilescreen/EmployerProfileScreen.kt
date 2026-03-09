package com.example.dutype.employer.screens.profilescreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
import com.example.dutype.navigation.Routes
import com.example.dutype.components.ProfileShimmer
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.components.TrustBadge
import com.example.dutype.components.TrustBadgeSize
import com.example.dutype.models.parseTrustTier
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.utils.LocaleHelper
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.dutype.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerProfileScreen(
    rootNavController: NavController,
    localNavController: NavController? = null,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    val screenBackgroundColor = WorkerColors.ScreenBackground
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
    
    // LIGHTWEIGHT PROFILE: Use metadata for basic profile info (name, phone, image)
    val userStats by profileCompletionViewModel.metadataManager.userMetadata.userStats.collectAsState()
    
    var companyName by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var employerTrustTier by remember { mutableStateOf("VERIFIED") }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showLanguageBottomSheet by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // Guest mode - Login bottom sheet state
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    var pendingMenuAction by remember { mutableStateOf<String?>(null) }

    // LIGHTWEIGHT: Load only basic profile info using metadata
    LaunchedEffect(Unit) {
        isLoadingProfile = true
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            try {
                // LIGHTWEIGHT: Only load basic profile (name, phone, image) - no heavy stats
                profileCompletionViewModel.metadataManager.userMetadata.loadBasicProfile()
                
                // Use metadata for profile image URL
                profileImageUrl = userStats.profileImageUrl.ifEmpty { null }
                
                Timber.i("Employer profile (lightweight) - Company: ${userStats.companyName}, Phone: ${userStats.phone}")
            } catch (e: Exception) {
                Timber.e("Error loading lightweight profile: ${e.message}")
            }
        }
        isLoadingProfile = false
    }
    
    // Update company name and phone from metadata (lightweight)
    // Also get phone from Firebase Auth as fallback for new users
    LaunchedEffect(userStats) {
        companyName = userStats.companyName.ifEmpty { userStats.fullName }
        // Get phone from metadata, fallback to Firebase Auth
        val authPhone = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
        companyPhone = userStats.phone.ifBlank { authPhone }
        if (userStats.profileImageUrl.isNotBlank() && profileImageUrl == null) {
            profileImageUrl = userStats.profileImageUrl
        }
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
                                onSuccess = { imageUrl -> 
                                    profileImageUrl = imageUrl
                                    android.widget.Toast.makeText(context, "Profile photo updated!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { exception ->
                                    profileImageUri = null
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
                        profileImageUri = null
                        android.widget.Toast.makeText(context, "Failed to upload photo. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .background(screenBackgroundColor)
        ) {
            // Offline banner at the very top
            val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
            val isOnline by connectivityViewModel.isOnline.collectAsState()
            com.example.dutype.components.OfflineBanner(isOffline = !isOnline)
            
            // Header using CommonHeader (no back button for profile)
            com.example.dutype.components.CommonHeader(
                title = stringResource(R.string.profile),
                showBackButton = false,
                backgroundColor = WorkerColors.CardBackground,
                titleColor = WorkerColors.TextPrimary
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
            // ═══════════════════════════════════════════════════════════════
            // BUSINESS PROFILE SECTION (Flat Menu Item)
            // ═══════════════════════════════════════════════════════════════
            item {
                val isLoggedIn = currentUserId.isNotEmpty()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Profile as Flat Menu Item: Photo | Name + Phone | Arrow
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isLoggedIn) {
                                        localNavController?.navigate(Routes.EMPLOYER_COMPANY_DETAILS)
                                            ?: rootNavController.navigate(Routes.EMPLOYER_COMPANY_DETAILS)
                                    } else {
                                        pendingMenuAction = "profile"
                                        showLoginBottomSheet = true
                                    }
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
                                        .background(WorkerColors.ChipBackground)
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
                                    when {
                                        isUploadingImage -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = WorkerColors.Info,
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
                                                tint = WorkerColors.IconSecondary,
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
                                            .background(WorkerColors.Info, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Change Photo",
                                            tint = WorkerColors.CardBackground,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Company Name + Phone or Sign up button
                            Column(modifier = Modifier.weight(1f)) {
                                if (isLoggedIn) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = companyName.ifEmpty { stringResource(R.string.your_company) },
                                            style = AppTypography.pageTitle.copy(
                                                color = WorkerColors.TextPrimary
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
                                            style = AppTypography.bodyMedium.copy(
                                                color = WorkerColors.TextSecondary
                                            )
                                        )
                                    }
                                } else {
                                    // Show Sign up button when not logged in - Blue for Employer
                                    Button(
                                        onClick = { rootNavController.navigate(Routes.ENHANCED_LOGIN) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmployerColors.Primary  // Blue for Employer
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(
                                            text = "Sign up",
                                            style = AppTypography.buttonMedium.copy(
                                                color = Color.White
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "View and update your profile details",
                                        style = AppTypography.bodySmall.copy(
                                            color = EmployerColors.TextSecondary
                                        )
                                    )
                                }
                            }
                            
                            // Arrow - only show when logged in
                            if (isLoggedIn) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = WorkerColors.IconSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // ═══════════════════════════════════════════════════════════════
            // QUICK ACTIONS (Help Centre, Change Language) - Meesho style
            // ═══════════════════════════════════════════════════════════════
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Help Centre Button
                        EmployerQuickActionButton(
                            iconRes = R.drawable.phone_in_talk_24,
                            title = stringResource(R.string.help_and_support),
                            modifier = Modifier.weight(1f),
                            iconTint = WorkerColors.TextPrimary,
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_HELP) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_HELP) 
                            }
                        )
                        
                        // Change Language Button
                        val currentLanguage = LocaleHelper.getLanguage(context)
                        EmployerQuickActionButton(
                            iconRes = R.drawable.translate_indic_24,
                            title = if (currentLanguage == LocaleHelper.LANGUAGE_TELUGU) "భాష మార్చు" else stringResource(R.string.language),
                            modifier = Modifier.weight(1f),
                            iconTint = Color(0xFFE91E63),  // Pink/magenta
                            onClick = { showLanguageBottomSheet = true }
                        )
                    }
                }
            }
            
            // ═══════════════════════════════════════════════════════════════
            // MY ACTIVITY SECTION
            // ═══════════════════════════════════════════════════════════════
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SectionHeader(title = "My Activity")
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        ProfileMenuItem(
                            icon = Icons.Outlined.Verified,
                            title = stringResource(R.string.trust_badges),
                            onClick = { 
                                if (currentUserId.isEmpty()) {
                                    pendingMenuAction = "trust_badges"
                                    showLoginBottomSheet = true
                                } else {
                                    localNavController?.navigate(Routes.EMPLOYER_TRUST_BADGES) 
                                        ?: rootNavController.navigate(Routes.EMPLOYER_TRUST_BADGES) 
                                }
                            }
                        )
                        
                        EmployerMenuDivider()
                        
                        ProfileMenuItem(
                            icon = Icons.Outlined.Work,
                            title = stringResource(R.string.my_job_posts),
                            onClick = { 
                                if (currentUserId.isEmpty()) {
                                    pendingMenuAction = "job_posts"
                                    showLoginBottomSheet = true
                                } else {
                                    localNavController?.navigate(Routes.EMPLOYER_HISTORY) 
                                        ?: rootNavController.navigate(Routes.EMPLOYER_HISTORY) 
                                }
                            }
                        )
                        
                        EmployerMenuDivider()

                        ProfileMenuItem(
                            icon = Icons.Outlined.LocationOn,
                            title = stringResource(R.string.work_locations),
                            onClick = { 
                                if (currentUserId.isEmpty()) {
                                    pendingMenuAction = "locations"
                                    showLoginBottomSheet = true
                                } else {
                                    localNavController?.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) 
                                        ?: rootNavController.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) 
                                }
                            }
                        )
                    }
                }
            }
            
            // ═══════════════════════════════════════════════════════════════
            // OTHERS SECTION
            // ═══════════════════════════════════════════════════════════════
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SectionHeader(title = "Others")
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Refer & Earn
                        ProfileMenuItem(
                            icon = Icons.Outlined.CardGiftcard,
                            title = "Refer & Earn",
                            onClick = { 
                                if (currentUserId.isEmpty()) {
                                    pendingMenuAction = "refer_earn"
                                    showLoginBottomSheet = true
                                } else {
                                    localNavController?.navigate(Routes.EMPLOYER_REFER_EARN) 
                                        ?: rootNavController.navigate(Routes.EMPLOYER_REFER_EARN)
                                }
                            }
                        )
                        
                        EmployerMenuDivider()
                        
                        // Switch to Worker Role - COMMENTED OUT
                        /*
                        ProfileMenuItem(
                            icon = Icons.Outlined.Person,
                            title = "Switch to Worker",
                            onClick = { 
                                rootNavController.navigate(Routes.WORKER_HOME) {
                                    popUpTo(Routes.EMPLOYER_HOME) { inclusive = true }
                                }
                            }
                        )
                        
                        EmployerMenuDivider()
                        */
                        
                        // About - Available without login
                        ProfileMenuItem(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.about),
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_ABOUT) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_ABOUT) 
                            }
                        )
                        
                        EmployerMenuDivider()
                        
                        // Security & Legal menu item removed - screen was deleted
                        
                        // Only show logout when logged in
                        if (currentUserId.isNotEmpty()) {
                            // Logout moved below Follow Us section
                        }
                    }
                }
            }
            
            // Follow Us Section
            item {
                EmployerFollowUsSection()
            }
            
            // Logout - Simple menu item below Follow Us
            item {
                if (currentUserId.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 13.dp)
                            .padding(top = 3.dp), // Minimal gap
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // No elevation
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Outlined.ExitToApp,
                            title = stringResource(R.string.log_out),
                            onClick = { showLogoutDialog = true },
                            isDestructive = true
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
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
                "profile" -> localNavController?.navigate(Routes.EMPLOYER_COMPANY_DETAILS) ?: rootNavController.navigate(Routes.EMPLOYER_COMPANY_DETAILS)
                "trust_badges" -> localNavController?.navigate(Routes.EMPLOYER_TRUST_BADGES) ?: rootNavController.navigate(Routes.EMPLOYER_TRUST_BADGES)
                "job_posts" -> localNavController?.navigate(Routes.EMPLOYER_HISTORY) ?: rootNavController.navigate(Routes.EMPLOYER_HISTORY)
                "locations" -> localNavController?.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) ?: rootNavController.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES)
                "refer_earn" -> localNavController?.navigate(Routes.EMPLOYER_REFER_EARN) ?: rootNavController.navigate(Routes.EMPLOYER_REFER_EARN)
            }
            pendingMenuAction = null
        },
        role = com.example.dutype.models.UserRole.EMPLOYER,
        title = "Login Required",
        subtitle = when (pendingMenuAction) {
            "profile" -> "Login to view and edit your company profile"
            "trust_badges" -> "Login to view your trust badges"
            "job_posts" -> "Login to view your job posts"
            "locations" -> "Login to manage work locations"
            "refer_earn" -> "Login to refer friends and earn rewards"
            else -> "Please login to access this feature"
        }
    )
}

// ═══════════════════════════════════════════════════════════════
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTypography.sectionHeader.copy(color = WorkerColors.TextPrimary)
    )
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    iconColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with custom color support
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                isDestructive -> WorkerColors.Error
                iconColor != null -> iconColor
                else -> Color(0xFF4B5563) // text-gray-600 for profile icons
            },
            modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.menuItemTitle.copy(
                    color = if (isDestructive) WorkerColors.Error else WorkerColors.TextPrimary
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTypography.menuItemSubtitle.copy(
                        color = WorkerColors.TextSecondary
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = WorkerColors.IconSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Meesho-style quick action button using drawable resource
 * Clean bordered box with no background fill
 */
@Composable
private fun EmployerQuickActionButton(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    iconTint: Color = WorkerColors.TextPrimary,
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
        border = BorderStroke(1.dp, WorkerColors.Border)
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
                style = AppTypography.quickActionLabel.copy(
                    color = WorkerColors.TextPrimary,
                    fontFamily = MeeshoFontFamily
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
private fun EmployerMenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp) // Align with text after icon
            .height(1.dp)
            .background(WorkerColors.Divider)
    )
}

@Composable
private fun EmployerFollowUsSection() {
    val context = LocalContext.current
    val whatsAppChannelUrl = "https://whatsapp.com/channel/0029VbBdNOQ1iUxZMmvg8t2G"
    val instagramUrl = "https://www.instagram.com/dutype.in"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = WorkerColors.CardBackground
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

@Composable
private fun EmployerSocialMediaIcon(
    @androidx.annotation.DrawableRes iconRes: Int,
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
            tint = WorkerColors.TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = AppTypography.bodySmall.copy(
                color = WorkerColors.TextSecondary
            )
        )
    }
}
