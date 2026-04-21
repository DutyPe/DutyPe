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
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.dutype.components.RoleSwitchDialog
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.utils.LocaleHelper
import dagger.hilt.android.EntryPointAccessors
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
    val screenBackgroundColor = EmployerColors.ScreenBackground
    LaunchedEffect(Unit) {
        onStatusBarColorChange?.invoke(Color.White)
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
                
                Timber.i("Employer profile (lightweight) - Name: ${userStats.fullName}, Phone: ${userStats.phone}")
            } catch (e: Exception) {
                Timber.e("Error loading lightweight profile: ${e.message}")
            }
        }
        isLoadingProfile = false
    }
    
    // Update company name and phone from metadata (lightweight)
    // Also get phone from Firebase Auth as fallback for new users
    LaunchedEffect(userStats) {
        companyName = userStats.fullName
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
                // Solid role background — no gradient.
                .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
        ) {
            // Offline banner at the very top
            val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
            val isOnline by connectivityViewModel.isOnline.collectAsState()
            com.example.dutype.components.OfflineBanner(isOffline = !isOnline)
            
            // Header using CommonHeader (no back button for profile)
            com.example.dutype.components.CommonHeader(
                title = stringResource(R.string.profile),
                showBackButton = false,
                backgroundColor = Color.Transparent,
                titleColor = WorkerColors.TextPrimary,
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
                                contentDescription = if (currentLanguage == LocaleHelper.LANGUAGE_TELUGU) "భాష మార్చు" else stringResource(R.string.language),
                                tint = Color(0xFFE91E63), // Pink/magenta color
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // WhatsApp Support Button - Icon only with WhatsApp green color
                        IconButton(onClick = {
                            val whatsappNumber = "919121706236" // DutyPe support number
                            val message = "Hello DutyPe Team! I am an employer on DutyPe and I need help with the app."
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
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = "WhatsApp Support",
                                tint = Color(0xFF25D366), // WhatsApp green color
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
            // ═══════════════════════════════════════════════════════════════
            // BUSINESS PROFILE SECTION (Flat Menu Item)
            // ═══════════════════════════════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val isLoggedIn = currentUserId.isNotEmpty()
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    // TODO: Rounded corners commented out for UI testing
                    // shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Profile as Flat Menu Item: Photo | Name + Phone | Arrow
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Solid card surface (no gradient) per the
                                // app-wide rule.
                                .background(
                                    color = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (isLoggedIn) {
                                        localNavController?.navigate(Routes.EMPLOYER_COMPANY_DETAILS)
                                            ?: rootNavController.navigate(Routes.EMPLOYER_COMPANY_DETAILS)
                                    } else {
                                        pendingMenuAction = "profile"
                                        showLoginBottomSheet = true
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Company Logo
                            Box(modifier = Modifier.size(56.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0E7FF))
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
                                            com.example.dutype.components.OptimizedProfileImage(
                                                imageUrl = profileImageUri.toString(),
                                                contentDescription = "Company Logo",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                            )
                                        }
                                        !profileImageUrl.isNullOrBlank() -> {
                                            com.example.dutype.components.OptimizedProfileImage(
                                                imageUrl = profileImageUrl,
                                                contentDescription = "Company Logo",
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
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
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
                                    // Show guest CTA when not logged in
                                    Button(
                                        onClick = { // CRITICAL FIX: Pass role=EMPLOYER to maintain role context after login
                                            rootNavController.navigate("${Routes.ENHANCED_LOGIN}?role=EMPLOYER")
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1F2937)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Text(
                                            text = "Log in / Sign up",
                                            style = AppTypography.buttonMedium.copy(
                                                color = Color.White
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "View and update your profile data",
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
            // MY ACTIVITY SECTION
            // ═══════════════════════════════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    // TODO: Rounded corners commented out for UI testing
                    // shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SectionHeader(title = stringResource(R.string.my_activity))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // ProfileMenuItem(
                        //     icon = Icons.Outlined.Verified,
                        //     title = stringResource(R.string.trust_badges),
                        //     onClick = {
                        //         if (currentUserId.isEmpty()) {
                        //             pendingMenuAction = "trust_badges"
                        //             showLoginBottomSheet = true
                        //         } else {
                        //             localNavController?.navigate(Routes.EMPLOYER_TRUST_BADGES)
                        //                 ?: rootNavController.navigate(Routes.EMPLOYER_TRUST_BADGES)
                        //         }
                        //     }
                        // )
                        // EmployerMenuDivider()
                        
                        ProfileMenuItem(
                            icon = Icons.Outlined.Work,
                            title = stringResource(R.string.my_job_posts),
                            onClick = { 
                                if (currentUserId.isEmpty()) {
                                    pendingMenuAction = "job_posts"
                                    showLoginBottomSheet = true
                                } else {
                                    try {
                                        // Use localNavController if available, otherwise rootNavController
                                        val navControllerToUse = localNavController ?: rootNavController
                                        navControllerToUse.navigate(Routes.EMPLOYER_HISTORY)
                                    } catch (e: Exception) {
                                        timber.log.Timber.e(e, "Error navigating to EMPLOYER_HISTORY")
                                        android.widget.Toast.makeText(
                                            context,
                                            "Unable to open My Jobs. Please try again.",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                        
                        EmployerMenuDivider()
                        
                        // REMOVED: Duplicate "My Business Card" menu item
                        // Kept "Digital Visiting Card" in Others section below

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
            // REWARDS SECTION
            // ═══════════════════════════════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    // TODO: Rounded corners commented out for UI testing
                    // shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SectionHeader(title = stringResource(R.string.rewards))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Refer & Earn
                        ProfileMenuItem(
                            icon = Icons.Outlined.CardGiftcard,
                            title = stringResource(R.string.refer_earn),
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
                    }
                }
            }
            
            // ═══════════════════════════════════════════════════════════════
            // OTHERS SECTION
            // ═══════════════════════════════════════════════════════════════
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    // TODO: Rounded corners commented out for UI testing
                    // shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SectionHeader(title = stringResource(R.string.others))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Help & FAQs - First item
                        ProfileMenuItem(
                            icon = Icons.Outlined.Phone,
                            title = stringResource(R.string.help_faqs),
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_HELP) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_HELP) 
                            }
                        )
                        
                        EmployerMenuDivider()
                        
                        // Digital Visiting Card menu item commented as requested
                        // ProfileMenuItem(
                        //     icon = Icons.Outlined.Person,
                        //     title = "Digital Visiting Card",
                        //     onClick = {
                        //         if (currentUserId.isEmpty()) {
                        //             pendingMenuAction = "visiting_card"
                        //             showLoginBottomSheet = true
                        //         } else {
                        //             localNavController?.navigate(Routes.EMPLOYER_VISITING_CARD)
                        //                 ?: rootNavController.navigate(Routes.EMPLOYER_VISITING_CARD)
                        //         }
                        //     }
                        // )
                        // EmployerMenuDivider()
                        
                        // ── ROLE-SWITCH FLAT MENU ── (commented out per product decision Apr 2026)
                        // The dual-role switch entry has been hidden from the employer profile menu.
                        // Switching is still available through the role-switch dialog from other entry
                        // points. To re-enable, uncomment the block below.
                        /*
                        if (currentUserId.isNotEmpty()) {
                            val roleManagementViewModel: com.example.dutype.viewmodels.RoleManagementViewModel = hiltViewModel()
                            val currentUser by roleManagementViewModel.currentUser.collectAsState()
                            
                            if (currentUser != null && currentUser!!.isDualRole()) {
                                var showRoleSwitchDialog by remember { mutableStateOf(false) }
                                var isRoleSwitching by remember { mutableStateOf(false) }
                                
                                EmployerRoleManagementMenuItem(
                                    currentRole = currentUser!!.activeRole,
                                    onSwitchClick = {
                                        showRoleSwitchDialog = true
                                    }   
                                )
                                
                                EmployerMenuDivider()

                                RoleSwitchDialog(
                                    showDialog = showRoleSwitchDialog,
                                    currentRole = currentUser!!.activeRole,
                                    enabledRoles = currentUser!!.getEnabledRoles(),
                                    isLoading = isRoleSwitching,
                                    onDismiss = { showRoleSwitchDialog = false },
                                    onSwitchRole = { selectedRole ->
                                        isRoleSwitching = true
                                        val appContext = context.applicationContext as android.app.Application
                                        val entryPoint = EntryPointAccessors.fromApplication(
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
                                                    oldRole = currentUser!!.activeRole,
                                                    newRole = selectedRole,
                                                    onSuccess = {
                                                        isRoleSwitching = false
                                                        showRoleSwitchDialog = false
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "Switched to ${selectedRole.name.lowercase().replaceFirstChar { it.uppercase() }} role",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    onError = { error ->
                                                        isRoleSwitching = false
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "Failed to switch role: $error",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                )
                                            } catch (e: Exception) {
                                                isRoleSwitching = false
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Error switching role: ${e.message}",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        */
                        // ── END role-switch flat menu (commented) ──
                        
                        // About - Available without login
                        ProfileMenuItem(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.about),
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_ABOUT) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_ABOUT) 
                            }
                        )
                        
                        // Only show logout when logged in
                        if (currentUserId.isNotEmpty()) {
                            // Logout moved below Follow Us section
                        }
                    }
                }
            }
            
            // Follow Us Section - COMMENTED OUT
            /*
            item {
                EmployerFollowUsSection()
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
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            ProfileMenuItem(
                                icon = Icons.AutoMirrored.Outlined.ExitToApp,
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
                // "trust_badges" -> localNavController?.navigate(Routes.EMPLOYER_TRUST_BADGES) ?: rootNavController.navigate(Routes.EMPLOYER_TRUST_BADGES)
                "job_posts" -> localNavController?.navigate(Routes.EMPLOYER_HISTORY) ?: rootNavController.navigate(Routes.EMPLOYER_HISTORY)
                "locations" -> localNavController?.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) ?: rootNavController.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES)
                "refer_earn" -> localNavController?.navigate(Routes.EMPLOYER_REFER_EARN) ?: rootNavController.navigate(Routes.EMPLOYER_REFER_EARN)
                "visiting_card" -> localNavController?.navigate(Routes.EMPLOYER_VISITING_CARD) ?: rootNavController.navigate(Routes.EMPLOYER_VISITING_CARD)
            }
            pendingMenuAction = null
        },
        role = com.example.dutype.models.UserRole.EMPLOYER,
        navController = rootNavController,
        title = stringResource(R.string.login_required),
        subtitle = when (pendingMenuAction) {
            "profile" -> "Login to view and edit your company profile"
            // "trust_badges" -> "Login to view your trust badges"
            "job_posts" -> "Login to view your job posts"
            "locations" -> "Login to manage work locations"
            "refer_earn" -> "Login to refer friends and earn rewards"
            "visiting_card" -> "Login to view your digital visiting card"
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
    val resolvedIconColor = when {
        isDestructive -> WorkerColors.Error
        iconColor != null -> iconColor
        else -> Color(0xFF3B82F6)
    }

    val iconBgColor = when {
        isDestructive -> Color(0xFFFEE2E2)
        iconColor != null -> iconColor.copy(alpha = 0.1f)
        else -> Color(0xFFEFF6FF)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with tinted background
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedIconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.menuItemTitle.copy(
                    color = if (isDestructive) WorkerColors.Error else Color(0xFF1E293B),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppTypography.menuItemSubtitle.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
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
            .background(Color(0xFFE5E7EB))
    )
}

// COMMENTED OUT - Follow Us Section
/*
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
                        .background(com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground)
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
                        .background(com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground)
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

// Role Management Menu Item for Employer
/**
 * Role Management Menu Item for Employer - Flat design matching other menu items
 * Shows current role with chevron icon for switching
 */
@Composable
private fun EmployerRoleManagementMenuItem(
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
            .padding(vertical = 14.dp),
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
                    style = AppTypography.menuItemTitle.copy(
                        color = WorkerColors.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Active: ",
                        style = AppTypography.menuItemSubtitle.copy(
                            color = WorkerColors.TextSecondary
                        )
                    )
                    Text(
                        text = currentRole.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = AppTypography.menuItemSubtitle.copy(
                            color = roleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
        
        // Chevron icon (matching other menu items)
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Switch",
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(24.dp)
        )
    }
}
