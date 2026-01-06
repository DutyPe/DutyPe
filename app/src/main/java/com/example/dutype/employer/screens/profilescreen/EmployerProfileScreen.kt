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
                                onSuccess = { imageUrl -> 
                                    profileImageUrl = imageUrl
                                    android.widget.Toast.makeText(context, "Profile photo updated!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { 
                                    profileImageUri = null
                                    android.widget.Toast.makeText(context, "Failed to upload photo", android.widget.Toast.LENGTH_SHORT).show()
                                }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundColor)
        ) {
            // Header using CommonHeader (no back button for profile)
            com.example.dutype.components.CommonHeader(
                title = stringResource(R.string.profile),
                showBackButton = false,
                backgroundColor = WorkerColors.CardBackground,
                titleColor = WorkerColors.TextPrimary
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                                            style = AppTypography.cardTitle.copy(
                                                fontWeight = FontWeight.Bold,
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
                            icon = Icons.Default.Verified,
                            title = stringResource(R.string.trust_badges),
                            iconColor = Color(0xFF10B981), // Green
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
                            icon = Icons.Default.CardMembership,
                            title = stringResource(R.string.subscription),
                            iconColor = Color(0xFFF59E0B), // Amber/Gold
                            onClick = { 
                                if (currentUserId.isEmpty()) {
                                    pendingMenuAction = "subscription"
                                    showLoginBottomSheet = true
                                } else {
                                    localNavController?.navigate(Routes.EMPLOYER_SUBSCRIPTION) 
                                        ?: rootNavController.navigate(Routes.EMPLOYER_SUBSCRIPTION) 
                                }
                            }
                        )
                        
                        EmployerMenuDivider()
                        
                        ProfileMenuItem(
                            icon = Icons.Default.Work,
                            title = stringResource(R.string.my_job_posts),
                            iconColor = Color(0xFF3B82F6), // Blue
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
                            icon = Icons.Default.LocationOn,
                            title = stringResource(R.string.work_locations),
                            iconColor = Color(0xFFEF4444), // Red
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
                        
                        // Switch to Worker Role
                        ProfileMenuItem(
                            icon = Icons.Default.Person,
                            title = "Switch to Worker",
                            iconColor = Color(0xFF8B5CF6), // Purple
                            onClick = { 
                                rootNavController.navigate(Routes.WORKER_HOME) {
                                    popUpTo(Routes.EMPLOYER_HOME) { inclusive = true }
                                }
                            }
                        )
                        
                        EmployerMenuDivider()
                        
                        // About - Available without login
                        ProfileMenuItem(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.about),
                            iconColor = Color(0xFF06B6D4), // Cyan
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_ABOUT) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_ABOUT) 
                            }
                        )
                        
                        EmployerMenuDivider()
                        
                        ProfileMenuItem(
                            icon = Icons.Default.Security,
                            title = stringResource(R.string.security_legal),
                            iconColor = Color(0xFF6366F1), // Indigo
                            onClick = { 
                                localNavController?.navigate(Routes.SECURITY_LEGAL) 
                                    ?: rootNavController.navigate(Routes.SECURITY_LEGAL) 
                            }
                        )
                        
                        // Only show logout when logged in
                        if (currentUserId.isNotEmpty()) {
                            EmployerMenuDivider()
                            
                            ProfileMenuItem(
                                icon = Icons.Default.ExitToApp,
                                title = stringResource(R.string.log_out),
                                onClick = { showLogoutDialog = true },
                                isDestructive = true
                            )
                        }
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
                "subscription" -> localNavController?.navigate(Routes.EMPLOYER_SUBSCRIPTION) ?: rootNavController.navigate(Routes.EMPLOYER_SUBSCRIPTION)
                "job_posts" -> localNavController?.navigate(Routes.EMPLOYER_HISTORY) ?: rootNavController.navigate(Routes.EMPLOYER_HISTORY)
                "locations" -> localNavController?.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) ?: rootNavController.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES)
            }
            pendingMenuAction = null
        },
        role = com.example.dutype.models.UserRole.EMPLOYER,
        title = "Login Required",
        subtitle = when (pendingMenuAction) {
            "profile" -> "Login to view and edit your company profile"
            "trust_badges" -> "Login to view your trust badges"
            "subscription" -> "Login to manage your subscription"
            "job_posts" -> "Login to view your job posts"
            "locations" -> "Login to manage work locations"
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
                else -> WorkerColors.IconPrimary
            },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.menuItemTitle.copy(
                    fontWeight = FontWeight.Medium,
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
