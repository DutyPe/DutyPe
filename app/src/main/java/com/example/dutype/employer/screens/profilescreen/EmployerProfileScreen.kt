package com.example.dutype.employer.screens.profilescreen

import com.dutype.app.R
import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.DeleteForever
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
import com.example.dutype.utils.findActivity
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.ui.theme.LocalRoleColors
import com.example.dutype.utils.LocaleHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Star

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerProfileScreen(
    rootNavController: NavController,
    localNavController: NavController? = null,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    val screenBg = com.example.dutype.ui.theme.EmployerColors.ScreenBackground
    LaunchedEffect(screenBg) {
        onStatusBarColorChange?.invoke(screenBg)
    }
    
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val subscriptionViewModel: com.example.dutype.viewmodels.SubscriptionViewModel = hiltViewModel()
    val subState by subscriptionViewModel.activeSubscription.collectAsState()
    val formattedExpiryDate = remember(subState.expiryDate) {
        if (subState.expiryDate > 0) {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(subState.expiryDate))
        } else {
            ""
        }
    }
    val context = LocalContext.current
    // Services accessed via ProfileCompletionViewModel (proper DI pattern)
    val authManager = profileCompletionViewModel.authManager
    
    // LIGHTWEIGHT PROFILE: Use metadata for basic profile info (name, phone, image)
    val userStats by profileCompletionViewModel.metadataManager.userMetadata.userStats.collectAsState()
    
    var companyName by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAccountDeletionDialog by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showLanguageBottomSheet by remember { mutableStateOf(false) }
    // var showThemeBottomSheet by remember { mutableStateOf(false) }
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
                                    android.widget.Toast.makeText(context, context.getString(R.string.profile_photo_updated), android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { exception ->
                                    profileImageUri = null
                                    // Show user-friendly error message
                                    val errorMessage = when {
                                        exception.message?.contains("quota", ignoreCase = true) == true ||
                                        exception.message?.contains("billing", ignoreCase = true) == true ||
                                        exception.message?.contains("storage", ignoreCase = true) == true ->
                                            context.getString(R.string.photo_upload_unavailable)
                                        exception.message?.contains("network", ignoreCase = true) == true ->
                                            context.getString(R.string.network_error_check_connection)
                                        else -> context.getString(R.string.photo_upload_failed)
                                    }
                                    android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    } catch (e: Exception) {
                        profileImageUri = null
                        android.widget.Toast.makeText(context, context.getString(R.string.photo_upload_failed), android.widget.Toast.LENGTH_SHORT).show()
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
                .background(LocalRoleColors.current.screenBackground)
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

                        // }

                        // WhatsApp Support Button - Icon only with WhatsApp green color
                        IconButton(onClick = {
                            val whatsappNumber = "918500717800" // DutyPe support number
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
                    // shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(0.dp)
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
                                    shape = RoundedCornerShape(0.dp)
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
                                        .background(EmployerColors.PrimaryLight)
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
                                            containerColor = EmployerColors.Primary
                                        ),
                                        shape = RoundedCornerShape(0.dp),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.auto_log_in_sign_up),
                                            style = AppTypography.buttonMedium.copy(
                                                color = Color.White
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.auto_view_and_update_your_profile_data),
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.my_activity),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(0.6.dp, Color(0xFFE2E8F0))
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Outlined.Work,
                            title = stringResource(R.string.my_job_posts),
                            iconColor = Color(0xFF2563EB),
                            onClick = { 
                                if (currentUserId.isEmpty()) {
                                    pendingMenuAction = "job_posts"
                                    showLoginBottomSheet = true
                                } else {
                                    try {
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
                        
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        
                        ProfileMenuItem(
                            icon = Icons.Outlined.LocationOn,
                            title = stringResource(R.string.work_locations),
                            iconColor = Color(0xFFEC4899),
                            onClick = { 
                                if (currentUserId.isEmpty()) {
                                    pendingMenuAction = "locations"
                                    showLoginBottomSheet = true
                                } else {
                                    try {
                                        val navControllerToUse = localNavController ?: rootNavController
                                        navControllerToUse.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES)
                                    } catch (e: Exception) {
                                        timber.log.Timber.e(e, "Error navigating to EMPLOYER_MANAGE_ADDRESSES")
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.unable_open_work_locations),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.others),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(0.6.dp, Color(0xFFE2E8F0))
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Outlined.Phone,
                            title = stringResource(R.string.help_faqs),
                            iconColor = Color(0xFF0EA5E9),
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_HELP) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_HELP) 
                            }
                        )
                        
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        
                        ProfileMenuItem(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.about),
                            iconColor = Color(0xFF6366F1),
                            onClick = { 
                                localNavController?.navigate(Routes.EMPLOYER_ABOUT) 
                                    ?: rootNavController.navigate(Routes.EMPLOYER_ABOUT) 
                            }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                        // Settings - Privacy Policy, Terms of Service, Logout, Delete Account
                        ProfileMenuItem(
                            icon = Icons.Outlined.Settings,
                            title = if (com.example.dutype.utils.LocaleHelper.getLanguage(context) == com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU) "సెట్టింగ్‌లు" else "Settings",
                            iconColor = Color(0xFF64748B),
                            onClick = { 
                                rootNavController.navigate(Routes.SETTINGS) 
                            }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                        ProfileMenuItem(
                            icon = Icons.Default.Star,
                            title = if (com.example.dutype.utils.LocaleHelper.getLanguage(context) == com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU) "ప్లే స్టోర్‌లో రేటింగ్ ఇవ్వండి (5★)" else "Rate DutyPe on Play Store (5★)",
                            iconColor = Color(0xFFF59E0B),
                            onClick = {
                                val inAppReviewManager = com.example.dutype.utils.InAppReviewManager(context)
                                inAppReviewManager.openPlayStore(context)
                            }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                        // Join DutyPe WhatsApp Group as flat menu item right after Rate DutyPe
                        val isTelugu = com.example.dutype.utils.LocaleHelper.getLanguage(context) == com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU
                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            title = if (isTelugu) "డ్యూటీపే జాబ్స్ వాట్సాప్ గ్రూప్" else "Join DutyPe Jobs Group",
                            subtitle = if (isTelugu) "తాజా వర్కర్ అప్‌డేట్‌లు & హైరింగ్ కమ్యూనిటీ" else "Daily worker updates & hiring community on WhatsApp",
                            iconColor = Color(0xFF25D366),
                            onClick = {
                                val whatsAppGroupUrl = "https://chat.whatsapp.com/ITnhw0jk2G0I9TNlDCaNQI?s=cl&p=a&ilr=4"
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsAppGroupUrl)).apply {
                                        setPackage("com.whatsapp")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsAppGroupUrl))
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {
                                        android.widget.Toast.makeText(context, "Unable to open WhatsApp link", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
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

    if (showAccountDeletionDialog) {
        com.example.dutype.components.AccountDeletionDialog(
            isVisible = showAccountDeletionDialog,
            onDismiss = { showAccountDeletionDialog = false },
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

    // if (showThemeBottomSheet) {
    //     val themeSheetState = androidx.compose.material3.rememberModalBottomSheetState(
    //         skipPartiallyExpanded = true
    //     )
    //     com.example.dutype.components.ThemeModeBottomSheet(
    //         sheetState = themeSheetState,
    //         onDismiss = { showThemeBottomSheet = false },
    //     )
    // }
    
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
                "job_posts" -> localNavController?.navigate(Routes.EMPLOYER_HISTORY) ?: rootNavController.navigate(Routes.EMPLOYER_HISTORY)
                "locations" -> runCatching {
                    localNavController?.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) ?: rootNavController.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES)
                }.onFailure {
                    timber.log.Timber.e(it, "Error navigating to pending locations action")
                }
                // "refer_earn" -> localNavController?.navigate(Routes.EMPLOYER_REFER_EARN) ?: rootNavController.navigate(Routes.EMPLOYER_REFER_EARN)
            }
            pendingMenuAction = null
        },
        role = com.example.dutype.models.UserRole.EMPLOYER,
        navController = rootNavController,
        title = stringResource(R.string.login_required),
        subtitle = when (pendingMenuAction) {
            "profile" -> stringResource(R.string.login_company_profile)
            "job_posts" -> stringResource(R.string.login_job_posts)
            "locations" -> stringResource(R.string.login_manage_work_locations)
            // "refer_earn" -> stringResource(R.string.login_refer_earn)
            else -> stringResource(R.string.login_access_feature)
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
    badgeText: String? = null,
    isDestructive: Boolean = false,
    iconColor: Color = Color(0xFF2563EB),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isDestructive) Color(0xFFFEE2E2) else iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color(0xFFDC2626) else iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDestructive) Color(0xFFDC2626) else Color(0xFF0F172A),
                        fontSize = 15.sp
                    )
                )
                if (!badgeText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEEF2FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF4F46E5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
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
        shape = RoundedCornerShape(0.dp),
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
            .padding(start = 40.dp)
            .height(1.dp)
            .background(WorkerColors.Divider)
    )
}

// COMMENTED OUT - Follow Us Section
/*
@Composable
private fun EmployerFollowUsSection() {
    val context = LocalContext.current
    val whatsAppChannelUrl = "https://chat.whatsapp.com/ITnhw0jk2G0I9TNlDCaNQI?s=cl&p=a&ilr=4"
    val instagramUrl = "https://www.instagram.com/dutype.in"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = WorkerColors.CardBackground
        ),
        shape = RoundedCornerShape(0.dp),
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
                text = stringResource(R.string.auto_follow_us_on),
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
                        
                        .border(1.dp, EmployerColors.Border, CircleShape)
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
                        tint = EmployerColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // WhatsApp
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        
                        .border(1.dp, EmployerColors.Border, CircleShape)
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
                        tint = EmployerColors.TextPrimary,
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

