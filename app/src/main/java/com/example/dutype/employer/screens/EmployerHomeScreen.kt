package com.example.dutype.employer.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.ui.unit.sp
import com.example.dutype.navigation.Routes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.airbnb.lottie.compose.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.components.AnnouncementList
import com.example.dutype.components.AppUpdatePrompt
import com.example.dutype.components.OptimizedImage
import com.example.dutype.components.GuestWelcomeBonusCard
import com.example.dutype.components.WelcomeCelebrationOverlay
import com.example.dutype.components.consumeWelcomeCelebrationFlag
import com.example.dutype.employer.components.EmployerJobCard
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
import com.example.dutype.employer.models.JobStats
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.viewmodels.InstantHelpViewModel
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ApplicationStateManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.JobListing
import com.example.dutype.repositories.ReferralConfig
import com.example.dutype.models.ApplicationStats
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.utils.DeepLinkHandler
import com.example.dutype.components.JobCardShimmer
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import androidx.compose.ui.platform.LocalContext
import com.dutype.app.R
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.components.NotificationPermissionBottomSheet
import com.example.dutype.components.openNotificationSettings
import com.example.dutype.components.BirthdayBanner
import com.example.dutype.services.BirthdayInfo
import com.example.dutype.services.BirthdayService
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.utils.appVersionInfo
import com.example.dutype.viewmodels.AppConfigViewModel
import com.example.dutype.utils.JobEditPolicy
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerHomeScreen(
    navController: NavController,
    rootNavController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null,
    notificationPermissionManager: com.example.dutype.utils.NotificationPermissionManager
) {
    val context = LocalContext.current
    val hiltFirestore = remember(context) { com.example.dutype.di.firestoreFromHilt(context) }
    val hiltAuth = remember(context) { com.example.dutype.di.authFromHilt(context) }
    val viewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val applicationViewModel: EmployerApplicationViewModel = hiltViewModel()
    val instantHelpViewModel: InstantHelpViewModel = hiltViewModel()
    val appConfigViewModel: AppConfigViewModel = hiltViewModel()
    val referralConfig by appConfigViewModel.referralConfig.collectAsStateWithLifecycle()
    val appUpdateConfig by appConfigViewModel.appUpdateConfig.collectAsStateWithLifecycle()
    val dynamicFeatures by appConfigViewModel.dynamicFeaturesConfig.collectAsStateWithLifecycle()
    val subscriptionViewModel: com.example.dutype.viewmodels.SubscriptionViewModel = hiltViewModel()
    val subscription by subscriptionViewModel.activeSubscription.collectAsStateWithLifecycle()
    val appVersionInfo = remember(context) { context.appVersionInfo() }
    
    // NotificationService for unread count (lightweight)
    val notificationService = remember { 
        com.example.dutype.services.NotificationService(
            context,
            hiltFirestore
        )
    }
    val employerJobUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appStats by applicationViewModel.stats.collectAsStateWithLifecycle()
    val instantHelpState by instantHelpViewModel.uiState.collectAsStateWithLifecycle()
    
    // Announcement ViewModel for in-app announcements
    val announcementViewModel: com.example.dutype.viewmodels.AnnouncementViewModel = hiltViewModel()
    val announcements by announcementViewModel.announcements.collectAsStateWithLifecycle()
    
    // Unread notification count for badge (lightweight - only count, not full notifications)
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    
    // Birthday wish state 
    val birthdayService = remember { 
        BirthdayService(
            hiltFirestore,
            hiltAuth,
            com.example.dutype.services.NotificationService(context, hiltFirestore)
        )
    }
    var birthdayInfo by remember { mutableStateOf<BirthdayInfo?>(null) }
    var showBirthdayBanner by remember { mutableStateOf(false) }
    
    // State for sharing job actions
    var jobToShare by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    // Permission handling - Check permissions only once
    var hasNotificationPermission by remember { 
        mutableStateOf(notificationPermissionManager.isNotificationPermissionGranted()) 
    }
    // Location permission removed - not needed for employer side
    
    // Track if permissions have been requested to avoid repeated requests
    var permissionsRequested by remember { mutableStateOf(false) }
    var isFirstTimeUser by remember { mutableStateOf(true) }
    
    // Bottom sheet state
    var showNotificationBottomSheet by remember { mutableStateOf(false) }
    
    // Track if bottom sheets have been shown in this app session
    var bottomSheetsShownInSession by remember { mutableStateOf(false) }
    
    // Location permission launcher removed - not needed for employer side
    
    // CRITICAL FIX: Don't cache employerId - get fresh value to handle role switches
    // Using remember would cache the value and break after role switch
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val employerId = currentUser?.uid
    val isGuestEmployer = currentUser == null || currentUser.isAnonymous
    LaunchedEffect(employerId) {
        if (employerId != null) {
            Timber.d("EMPLOYER_HOME: Loading jobs for employerId=$employerId")
            viewModel.loadMyJobs()
            instantHelpViewModel.loadEmployerUrgentNeeds()
        } else {
            Timber.w("EMPLOYER_HOME: No employerId - user not authenticated")
        }
    }
    
    // Refresh subscription and metadata when returning to this screen
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (user != null && !user.isAnonymous) {
                        profileCompletionViewModel.metadataManager.userMetadata.refresh(com.example.dutype.models.UserRole.EMPLOYER)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
        
    LaunchedEffect(employerId) {
        // Load announcements for employer role
        announcementViewModel.loadAnnouncements("employer")
        
        // Fetch unread notification count for badge (lightweight - only count)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { userId ->
            try {
                val result = notificationService.getUnreadNotificationCount(
                    userId = userId,
                    activeRole = "EMPLOYER"
                )
                result.onSuccess { count ->
                    unreadNotificationCount = count
                }
                
                //  Check if today is user's birthday
                if (!birthdayService.hasWishedToday(context, userId)) {
                    val bday = birthdayService.checkIfBirthday(userId)
                    if (bday != null) {
                        birthdayInfo = bday
                        showBirthdayBanner = true
                        // Send birthday notification
                        birthdayService.sendBirthdayNotification(userId, bday.userName)
                        // Mark as wished today to avoid duplicates
                        birthdayService.markWishedToday(context, userId)
                        Timber.i(" Happy Birthday ${bday.userName}! Banner and notification sent.")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch unread notification count or check birthday")
            }
        }
    }
    
    // Note: We don't track views for employers viewing their own jobs
    
    // Check subscription expiry to trigger on-device push warning
    LaunchedEffect(subscription) {
        val currentUserId = hiltAuth.currentUser?.uid
        val hasActiveSub = subscription.status == "ACTIVE" || subscription.status == "TRIAL"
        if (currentUserId != null && hasActiveSub && subscription.expiryDate > 0L) {
            val msRemaining = subscription.expiryDate - System.currentTimeMillis()
            val fiveDaysMs = 5L * 24L * 60L * 60L * 1000L
            if (msRemaining in 0L..fiveDaysMs) {
                val daysRemaining = (msRemaining / (24L * 60L * 60L * 1000L)).coerceAtLeast(0).toInt()
                
                // Only notify once per day to prevent spam
                val sharedPrefs = context.getSharedPreferences("dutype_employer_prefs", android.content.Context.MODE_PRIVATE)
                val lastShownKey = "sub_expiry_notif_last_shown"
                val lastShown = sharedPrefs.getLong(lastShownKey, 0L)
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                if (lastShown < todayStart) {
                    try {
                        val notification = com.example.dutype.models.NotificationData(
                            id = "sub_expiry_${subscription.expiryDate}",
                            recipientId = currentUserId,
                            title = "Subscription Expiring Soon!",
                            message = "Your plan expires in $daysRemaining days. Renew now to avoid interruption.",
                            type = com.example.dutype.models.NotificationType.JOB_EXPIRY_REMINDER,
                            targetRole = "EMPLOYER",
                            createdAt = System.currentTimeMillis(),
                            isRead = false
                        )
                        notificationService.sendNotification(notification, currentUserId)
                        sharedPrefs.edit().putLong(lastShownKey, System.currentTimeMillis()).apply()
                        Timber.d("Triggered on-device sub expiry notification. Days left: $daysRemaining")
                    } catch (e: java.lang.Exception) {
                        Timber.w(e, "Failed to send local subscription expiry notification")
                    }
                }
            }
        }
    }
    
    // Handle permissions: Permissions are now requested on SelectRoleScreen after onboarding
    // Here we only show bottom sheets for returning users who denied permissions
    LaunchedEffect(Unit) {
        Timber.d("EmployerHomeScreen - Checking permission status for bottom sheets")
        Timber.d("EmployerHomeScreen - hasNotificationPermission: $hasNotificationPermission")
        
        // Only show bottom sheets for denied permissions (permissions are requested on SelectRoleScreen)
        if (!bottomSheetsShownInSession) {
            bottomSheetsShownInSession = true
            if (!hasNotificationPermission) {
                Timber.d("EmployerHomeScreen - Showing notification bottom sheet for denied permission")
                showNotificationBottomSheet = true
            }
        }
    }
    
    // Note: Permission requests moved to SelectRoleScreen after onboarding
    // Bottom sheets will show once per app session when user returns after denying permissions
    
    // Handle job sharing
    LaunchedEffect(jobToShare) {
        jobToShare?.let { (jobId, jobTitle) ->
            shareJob(jobId, jobTitle, context)
            jobToShare = null
        }
    }
    
    // Helper functions to handle job actions
    val handleJobShare = remember { { jobId: String, jobTitle: String -> jobToShare = Pair(jobId, jobTitle) } }
    
    val isDark = com.example.dutype.ui.theme.isAppInDarkTheme()
    val employerStatusBarColor = Color(0xFFEFF6FF)
    LaunchedEffect(employerStatusBarColor) {
        onStatusBarColorChange(employerStatusBarColor)
    }

    val recentJobs: List<JobListing> = employerJobUiState.myJobs
    val urgentRequests = instantHelpState.employerInstantRequests
    val jobStats = JobStats(
        activeJobs = recentJobs.count { it.status == "open" } + urgentRequests.count { it.status == "open" },
        totalApplications = appStats.totalApplications,
        todayJobs = recentJobs.count { DateTimeUtils.isToday(it.createdAt) } + urgentRequests.count { DateTimeUtils.isToday(it.createdAt) },
        totalJobs = recentJobs.size + urgentRequests.size
    )
    val isLoading = employerJobUiState.isLoading
    val isRefreshing = employerJobUiState.isRefreshing
    val error = employerJobUiState.error
    val showEmployerCashBonus = false
    val showEmployerWelcomeCard = false
    val employerWelcomeMessage = when {
        referralConfig.employerUnlimitedJobPostingEnabled && showEmployerCashBonus -> stringResource(R.string.guest_employer_welcome_message_with_bonus)
        referralConfig.employerUnlimitedJobPostingEnabled -> stringResource(R.string.guest_employer_welcome_message_posts)
        showEmployerCashBonus -> stringResource(R.string.guest_employer_welcome_message_bonus)
        else -> stringResource(R.string.guest_employer_welcome_message_posts)
    }
    
    // Company name state - loaded instantly from UserMetadata
    val userStats by applicationViewModel.userMetadata.userStats.collectAsStateWithLifecycle()
    val companyName = userStats.companyName.ifBlank { userStats.fullName }
    var profileSetupStatus by remember { mutableStateOf<com.example.dutype.state.ProfileSetupStatus?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF6FF))
    ) {
        EmployerHomeBackdropDecor(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
        // Offline banner at the very top
        val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
        val isOnline by connectivityViewModel.isOnline.collectAsState()
        com.example.dutype.components.OfflineBanner(isOffline = !isOnline)
        
        //  Birthday Banner - Shows if today is user's birthday
        if (showBirthdayBanner && birthdayInfo != null) {
            BirthdayBanner(
                userName = birthdayInfo!!.userName,
                onDismiss = { showBirthdayBanner = false }
            )
        }
        
        // Profile completion prompt removed - not needed for hyper-local employers

        // Show dashboard content directly with pull-to-refresh
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                // Refresh all data sources
                viewModel.refreshMyJobs()
                instantHelpViewModel.loadEmployerUrgentNeeds()
                announcementViewModel.loadAnnouncements("EMPLOYER") // Refresh announcements for employers
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            DashboardContent(
                recentJobs = recentJobs,
                jobStats = jobStats,
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                navController = navController,
                viewModel = viewModel,
                scrollStateManager = scrollStateManager,
                onShareJob = handleJobShare,
                context = context,
                applicationCountsByJobId = employerJobUiState.applicationCountsByJobId,
                announcements = announcements,
                onDismissAnnouncement = { announcementId ->
                    announcementViewModel.dismissAnnouncement(announcementId)
                },
                employerPromoBannerUrl = dynamicFeatures.employerPromoBannerUrl,
                subscription = subscription,
                urgentRequests = instantHelpState.employerInstantRequests,
                urgentResponsesByRequestId = instantHelpState.employerInstantResponses,
                isLoadingUrgentRequests = instantHelpState.isLoadingEmployerUrgentNeeds,
                referralConfig = referralConfig,
                isGuestEmployer = isGuestEmployer,
                showGuestWelcomeCard = showEmployerWelcomeCard,
                guestWelcomeTitle = stringResource(R.string.guest_employer_welcome_title),
                guestWelcomeMessage = employerWelcomeMessage,
                guestWelcomeButtonText = stringResource(R.string.guest_welcome_login_register),
                onGuestWelcomeClick = {
                    rootNavController.navigate("${Routes.ENHANCED_LOGIN}?role=EMPLOYER")
                },
                companyName = companyName.ifEmpty { "" },
                unreadCount = unreadNotificationCount,
                headerLottieUrl = dynamicFeatures.headerLottieUrl,
                onNotificationClick = {
                    navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_NOTIFICATIONS)
                }
            )
        }

        // Show error if any
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.dismiss))
                        }
                    }
                }
            }
        } // Column
        
        // Notification permission bottom sheet
        NotificationPermissionBottomSheet(
            isVisible = showNotificationBottomSheet,
            onDismiss = { showNotificationBottomSheet = false },
            onEnableNotifications = {
                openNotificationSettings(context)
            },
            userRole = "employer"
        )

        if (!showNotificationBottomSheet) {
            AppUpdatePrompt(
                config = appUpdateConfig,
                currentVersionCode = appVersionInfo.code,
                userRole = "EMPLOYER"
            )
        }

        // Welcome celebration overlay — shown once after new employer completes profile
        // var showCelebration by remember { mutableStateOf(consumeWelcomeCelebrationFlag(context)) }
        // WelcomeCelebrationOverlay(
        //     visible = showCelebration,
        //     bonusAmount = referralConfig.employerSignupBonus.toInt().takeIf { showEmployerCashBonus && it > 0 } ?: 0,
        //     onDismiss = { showCelebration = false }
        // )
    } // Box
}

@Composable
private fun EmployerHomeBackdropDecor(modifier: Modifier = Modifier) {
    // Intentionally empty: the role theme provides a single solid screen
    // background, and the design rule forbids gradient halos. Kept as a
    // no-op so existing call-sites continue to work without restructuring.
    Box(modifier = modifier)
}

@Composable
fun DashboardContent(
    recentJobs: List<JobListing>,
    jobStats: JobStats,
    isLoading: Boolean,
    isRefreshing: Boolean,
    navController: NavController,
    viewModel: FirestoreEmployerJobViewModel,
    scrollStateManager: ScrollStateManager? = null,
    onShareJob: (String, String) -> Unit = { _, _ -> },
    context: android.content.Context,
    applicationCountsByJobId: Map<String, Int> = emptyMap(),
    announcements: List<com.example.dutype.models.Announcement> = emptyList(),
    onDismissAnnouncement: (String) -> Unit = {},
    employerPromoBannerUrl: String = "",
    subscription: com.example.dutype.models.EmployerSubscription = com.example.dutype.models.EmployerSubscription(),
    urgentRequests: List<com.example.dutype.models.InstantRequest> = emptyList(),
    urgentResponsesByRequestId: Map<String, List<com.example.dutype.models.InstantResponse>> = emptyMap(),
    isLoadingUrgentRequests: Boolean = false,
    referralConfig: ReferralConfig = ReferralConfig(),
    isGuestEmployer: Boolean = false,
    showGuestWelcomeCard: Boolean = false,
    guestWelcomeTitle: String = "",
    guestWelcomeMessage: String = "",
    guestWelcomeButtonText: String = "",
    onGuestWelcomeClick: () -> Unit = {},
    companyName: String = "",
    unreadCount: Int = 0,
    headerLottieUrl: String = "",
    onNotificationClick: () -> Unit = {},
    applicationViewModel: EmployerApplicationViewModel = hiltViewModel()
) {
    // Move view model & state collection to composable scope (not inside LazyListScope)
    val appStats by applicationViewModel.stats.collectAsState()
    val updatedStats = remember(jobStats, appStats.totalApplications) {
        jobStats.copy(totalApplications = appStats.totalApplications)
    }
    val hasNormalJobs = recentJobs.isNotEmpty()
    val hasUrgentNeeds = urgentRequests.isNotEmpty()

    if (isLoading && recentJobs.isEmpty()) {
        // Show loading when first coming to the page
        LoadingScreen()
    } else {
        ScrollAwareLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 0.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            scrollStateManager = scrollStateManager
        ) {
            item {
                WelcomeHeader(
                    companyName = companyName.ifEmpty { "" },
                    unreadCount = unreadCount,
                    headerLottieUrl = headerLottieUrl,
                    onNotificationClick = onNotificationClick
                )
            }
            if (showGuestWelcomeCard) {
                item {
                    GuestWelcomeBonusCard(
                        title = guestWelcomeTitle,
                        message = guestWelcomeMessage,
                        buttonText = stringResource(R.string.guest_welcome_claim_gift),
                        onClick = onGuestWelcomeClick,
                        onVariantImpression = { variant ->
                            Timber.d("Welcome gift impression (employer) variant=%s", variant.name)
                        },
                        onVariantClick = { variant ->
                            Timber.d("Welcome gift click (employer) variant=%s", variant.name)
                        }
                    )
                }
            }

            if (employerPromoBannerUrl.isNotBlank() || announcements.isNotEmpty()) {
                item {
                    AnnouncementList(
                        announcements = announcements,
                        promoBannerUrl = employerPromoBannerUrl,
                        onDismiss = { announcementId -> onDismissAnnouncement(announcementId) },
                        onAction = { announcement ->
                            announcement.actionRoute?.let { route ->
                                DeepLinkHandler.handleAnnouncementAction(route, navController, context)
                            }
                        }
                    )
                }
            }

            val totalCredits = subscription.normalCredits
            val hasActiveSub = subscription.status == "ACTIVE" || subscription.status == "TRIAL"
            
            // Check if subscription is expiring within 5 days
            val isExpiringSoon = hasActiveSub && subscription.expiryDate > 0L && 
                (subscription.expiryDate - System.currentTimeMillis()) in 0L..(5L * 24L * 60L * 60L * 1000L)

            val expiringJobs = recentJobs.filter { job ->
                val daysUntilExpiry = if (job.expiresAt > System.currentTimeMillis()) {
                    (job.expiresAt - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L)
                } else {
                    -1L
                }
                job.status.lowercase() == "open" && daysUntilExpiry in 0..2
            }

            if (expiringJobs.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        expiringJobs.forEach { job ->
                            val daysRemaining = ((job.expiresAt - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0)
                            val dayText = if (daysRemaining == 0L) "1 day" else "$daysRemaining days"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_MY_JOBS) }, // Navigate to My Jobs to manage
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)), // Red tint
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFFEE2E2), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Job Expiring: ${job.title}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B)
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = "Expires in $dayText. Click to extend job.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFB91C1C)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                val isErrorState = !hasActiveSub || totalCredits <= 0
                val bgColor = when {
                    isErrorState -> Color(0xFFF9FAFB)
                    isExpiringSoon -> Color(0xFFFEF9C3)
                    else -> Color(0xFFF5F3FF)
                }
                val borderColor = when {
                    isErrorState -> Color(0xFFE5E7EB)
                    isExpiringSoon -> Color(0xFFFEF08A)
                    else -> Color(0xFFDDD6FE)
                }
                val iconBgColor = when {
                    isErrorState -> Color(0xFFFEE2E2)
                    isExpiringSoon -> Color(0xFFFEF3C7)
                    else -> Color(0xFFEDE9FE)
                }
                val iconColor = when {
                    isErrorState -> Color(0xFFEF4444)
                    isExpiringSoon -> Color(0xFFD97706)
                    else -> Color(0xFF8B5CF6)
                }
                val titleColor = when {
                    isErrorState -> Color(0xFF111827)
                    isExpiringSoon -> Color(0xFF92400E)
                    else -> Color(0xFF5B21B6)
                }
                val subtitleColor = when {
                    isErrorState -> Color(0xFF6B7280)
                    isExpiringSoon -> Color(0xFFB45309)
                    else -> Color(0xFF7C3AED)
                }
                val vectorIcon = when {
                    isErrorState -> Icons.Default.Warning
                    isExpiringSoon -> Icons.Default.Warning
                    else -> Icons.Default.CheckCircle
                }

                val titleText = when {
                    isErrorState -> "Subscription Required"
                    isExpiringSoon -> "Subscription Expiring Soon!"
                    else -> "Subscription Active: ${subscription.planId.replace("_", " ").uppercase()}"
                }
                
                val subtitleText = when {
                    isErrorState -> "Get a plan to unlock job posting"
                    isExpiringSoon -> {
                        val daysRemaining = ((subscription.expiryDate - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0)
                        "Your plan expires in $daysRemaining days. Click to renew."
                    }
                    else -> "$totalCredits posts remaining (Normal or Urgent)"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_SUBSCRIPTION) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(width = 1.dp, color = borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(color = iconBgColor, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = vectorIcon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = titleText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = titleColor
                                    )
                                    if (isErrorState) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFEF2F2), RoundedCornerShape(4.dp))
                                                .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(stringResource(R.string.action_due), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = subtitleText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = subtitleColor
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            item {
                EnhancedStatsGrid(updatedStats, onViewAnalytics = { navController.navigate(com.example.dutype.navigation.Routes.ANALYTICS) })
            }


            /*
            if (isGuestEmployer) {
                item {
                    InviteEarnEmployerCard(
                        inviteEarnAmount = referralConfig.rewardPerReferral.toInt().coerceAtLeast(1)
                    )
                }
            }
            */

            if (updatedStats.totalJobs == 0) {
                item {
                    EmployerPostJobSection(
                        onPostUrgentNeed = { navController.navigate(Routes.EMPLOYER_POST_URGENT_NEED) },
                        onPostNormalJob = { navController.navigate(Routes.EMPLOYER_POST_JOB) }
                    )
                }
            }

            if (hasUrgentNeeds || isLoadingUrgentRequests) {
                item {
                    EmployerUrgentNeedSummarySection(
                        requests = urgentRequests,
                        responsesByRequestId = urgentResponsesByRequestId,
                        isLoading = isLoadingUrgentRequests,
                        onViewAll = { navController.navigate(Routes.employerHistoryRoute("urgent")) },
                        onOpenRequest = { request -> navController.navigate(Routes.employerUrgentNeedDetailRoute(request.requestId)) },
                        onPostUrgentNeed = { navController.navigate(Routes.EMPLOYER_POST_URGENT_NEED) }
                    )
                }
            }
            
            // Job Analytics Card removed per task list requirement

            item {
                RecentJobsSection(
                    jobs = recentJobs,
                    navController = navController,
                    onRefresh = { viewModel.refreshMyJobs() },
                    isRefreshing = isRefreshing,
                    onViewAllClick = { 
                        // Navigate to posted jobs screen
                        navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_MY_JOBS)
                    },
                    onTabSwitch = { /* No longer needed */ },
                    onShareJob = onShareJob,
                    context = context,
                    applicationCountsByJobId = applicationCountsByJobId
                )
            }

            item {
                EmployerTrustSignalsCard()
            }

            item {
                com.example.dutype.components.MadeWithLoveFooter()
            }
            
        }
    }
}

@Composable
private fun EmployerTrustSignalsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmployerTrustSignalItem(
                icon = Icons.Default.CheckCircle,
                title = "Verified Workers",
                subtitle = "100% verified professionals",
                iconTint = EmployerColors.Success,
                modifier = Modifier.weight(1f)
            )
            EmployerTrustSignalItem(
                icon = Icons.Default.Schedule,
                title = "Quick Response",
                subtitle = "Get responses in minutes",
                iconTint = EmployerColors.Warning,
                modifier = Modifier.weight(1f)
            )
            EmployerTrustSignalItem(
                icon = Icons.Default.CheckCircle,
                title = "Safe & Secure",
                subtitle = "Your data is always protected",
                iconTint = EmployerColors.Info,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EmployerTrustSignalItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = EmployerColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(color = EmployerColors.TextSecondary),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InviteEarnEmployerCard(inviteEarnAmount: Int = 20) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.SuccessLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(EmployerColors.Success.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = EmployerColors.Success,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Invite & Earn ₹$inviteEarnAmount",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = EmployerColors.Success,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Invite other employers and earn ₹$inviteEarnAmount when they post their first job.",
                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.Success)
                )
            }

            Button(
                onClick = {},
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmployerColors.Success,
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.invite_now))
            }
        }
    }
}

@Composable
private fun EmployerPostJobSection(
    onPostUrgentNeed: () -> Unit,
    onPostNormalJob: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(
                text = "Create a New Posting",
                style = AppTypography.sectionHeader.copy(
                    color = EmployerColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                fontFamily = MeeshoFontFamily
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Choose the posting format that fits your immediate staffing need.",
                style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
            )
        }

        // Card 1: Urgent Need
        val isDark = com.example.dutype.ui.theme.isAppInDarkTheme()
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
            border = BorderStroke(1.dp, EmployerColors.Border),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isDark) Color(0xFF4C0519) else Color(0xFFFEF2F2),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isDark) Color(0xFF9F1239) else Color(0xFFFEE2E2),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFF43F5E) else Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.post_urgent_need_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = EmployerColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isDark) Color(0xFF4C0519) else Color(0xFFFEF2F2),
                                        RoundedCornerShape(99.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0xFF9F1239) else Color(0xFFFEE2E2),
                                        RoundedCornerShape(99.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.instant_match),
                                    color = if (isDark) Color(0xFFF43F5E) else Color(0xFFEF4444),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.urgent_need_desc),
                            style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                        )
                    }
                }
                
                Button(
                    onClick = onPostUrgentNeed,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF43F5E) else Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.post_urgent_need_title), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Card 2: Normal Job
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
            border = BorderStroke(1.dp, EmployerColors.Border),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isDark) Color(0xFF2E1065) else Color(0xFFF5F3FF),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isDark) Color(0xFF5B21B6) else Color(0xFFEDE9FE),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFA78BFA) else Color(0xFF8B5CF6),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.post_normal_job),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = EmployerColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isDark) Color(0xFF2E1065) else Color(0xFFF5F3FF),
                                        RoundedCornerShape(99.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0xFF5B21B6) else Color(0xFFEDE9FE),
                                        RoundedCornerShape(99.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.standard_job_tag),
                                    color = if (isDark) Color(0xFFA78BFA) else Color(0xFF8B5CF6),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.standard_job_desc),
                            style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                        )
                    }
                }
                
                Button(
                    onClick = onPostNormalJob,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFA78BFA) else Color(0xFF8B5CF6),
                        contentColor = if (isDark) Color.Black else Color.White
                    )
                ) {
                    Text(stringResource(R.string.post_normal_job), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun UrgentNeedCtaCard(
    title: String,
    body: String,
    urgentButtonLabel: String,
    showNormalJobAction: Boolean,
    onPostUrgentNeed: () -> Unit,
    onPostNormalJob: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(EmployerColors.Warning.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = EmployerColors.Warning,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = EmployerColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPostUrgentNeed,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
                ) {
                    Text(urgentButtonLabel)
                }
                if (showNormalJobAction) {
                    TextButton(
                        onClick = onPostNormalJob,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.post_normal_job))
                    }
                }
            }
        }
    }
}

//@Composable
//fun HomeTabContent(
//    recentJobs: List<JobPostingModel>,
//    jobStats: JobStats,
//    isLoading: Boolean,
//    isRefreshing: Boolean,
//    navController: NavController,
//    viewModel: EmployerViewModel,
//    onTabSwitch: (Int) -> Unit,
//    scrollStateManager: ScrollStateManager? = null
//) {
//    if (isLoading && recentJobs.isEmpty()) {
//        // Show loading when first coming to the page
//        LoadingScreen()
//    } else {
//        ScrollAwareLazyColumn(
//            modifier = Modifier.fillMaxSize(),
//            contentPadding = PaddingValues(
//                top = 16.dp,
//                start = 16.dp,
//                end = 16.dp,
//                bottom = 0.dp
//            ),
//            scrollStateManager = scrollStateManager
//        ) {
//
//
//            item {
//                EnhancedStatsGrid(jobStats)
//            }
//
//            item {
//                RecentJobsSection(
//                    jobs = recentJobs,
//                    navController = navController,
//                    onRefresh = { viewModel.refreshMyJobs() },
//                    isRefreshing = isRefreshing,
//                    onViewAllClick = { onTabSwitch(2) }, // Switch to My Jobs tab
//                    onTabSwitch = onTabSwitch
//                )
//            }
//        }
//    }
//}

@Composable
fun LoadingScreen() {
    // Shimmer setup - moved to top level so it can be used throughout the function
    val shimmerColors = listOf(
        WorkerColors.ShimmerBase,
        WorkerColors.ShimmerHighlight,
        WorkerColors.ShimmerBase
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(300), // Reduced from 1200ms to 600ms
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Solid role background â€” the shimmer skeleton sits on the same
            // surface as the rest of the employer flow.
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        // Welcome header shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(16.dp)
        ) {
            Column {
                Spacer(
                    modifier = Modifier
                        .height(24.dp)
                        .fillMaxWidth(0.6f)
                        .background(brush, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Spacer(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.4f)
                        .background(brush, RoundedCornerShape(4.dp))
                )
            }
        }
        
        // Stats grid shimmer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(
                            modifier = Modifier
                                .size(40.dp)
                                .background(brush, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Spacer(
                            modifier = Modifier
                                .height(20.dp)
                                .fillMaxWidth(0.8f)
                                .background(brush, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Spacer(
                            modifier = Modifier
                                .height(14.dp)
                                .fillMaxWidth(0.6f)
                                .background(brush, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Job cards shimmer
        repeat(3) {
            JobCardShimmer()
        }
    }
}

@Composable
fun WelcomeHeader(
    companyName: String,
    unreadCount: Int = 0,
    headerLottieUrl: String = "",
    employerPrimaryColorHex: String = "#8B5CF6",
    onNotificationClick: () -> Unit = {}
) {
    val headerColor = remember(employerPrimaryColorHex) {
        try {
            Color(android.graphics.Color.parseColor(employerPrimaryColorHex))
        } catch (_: Exception) {
            Color(0xFF8B5CF6)
        }
    }

    val compositionResult = rememberLottieComposition(
        spec = LottieCompositionSpec.Url(headerLottieUrl.ifBlank { "https://localhost/dummy.json" })
    )
    val composition = compositionResult.value
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val greetingText = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                in 0..11 -> "Good Morning!"
                in 12..16 -> "Good Afternoon!"
                else -> "Good Evening!"
            }

            Text(
                text = if (companyName.isNotEmpty()) companyName else greetingText,
                style = AppTypography.displayTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF0F172A)
                ),
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color.Red,
                                shape = CircleShape
                            )
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedStatsGrid(stats: JobStats, onViewAnalytics: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Dashboard",
                style = AppTypography.sectionHeader.copy(
                    color = EmployerColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (onViewAnalytics != null) {
                TextButton(onClick = onViewAnalytics) {
                    Text(stringResource(R.string.view_analytics), style = AppTypography.buttonMedium.copy(color = EmployerColors.Primary))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassStatCard("Active", stats.activeJobs.toString(), Icons.Default.Work, EmployerColors.Success, Modifier.weight(1f))
            GlassStatCard("Applications", stats.totalApplications.toString(), Icons.Default.People, EmployerColors.Info, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassStatCard("Today", stats.todayJobs.toString(), Icons.Default.CalendarToday, Color(0xFF8B5CF6), Modifier.weight(1f))
            GlassStatCard("Total", stats.totalJobs.toString(), Icons.Default.Analytics, EmployerColors.Warning, Modifier.weight(1f))
        }
    }
}

@Composable
private fun GlassStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground.copy(alpha = 0.85f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmployerColors.TextPrimary,
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EmployerColors.TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

// NOTE: StatCard function moved to AnalyticsScreen.kt to avoid duplication
// Import from there: com.example.dutype.employer.screens.StatCard

@SuppressLint("SuspiciousIndentation")
@Composable
fun RecentJobsSection(
    jobs: List<JobListing>,
    navController: NavController,
    onRefresh: () -> Unit,
    isRefreshing: Boolean = false,
    onViewAllClick: () -> Unit,
    onTabSwitch: (Int) -> Unit,
    onShareJob: (String, String) -> Unit = { _, _ -> },
    context: android.content.Context,
    applicationCountsByJobId: Map<String, Int> = emptyMap()
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
            Text(
                text = "Recent Job Postings",
                style = com.example.dutype.ui.theme.AppTypography.sectionHeader,
                modifier = Modifier.padding(start = 8.dp)
            )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
                // View All button removed
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Show refresh indicator at the fetching place
        if (isRefreshing && jobs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Refreshing recent jobs...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (jobs.isEmpty()) {
            EmptyJobsState(onPostJob = { onTabSwitch(1) })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                jobs
                    .take(5) // Show more recent jobs
                    .map { job ->
                    // Convert JobListing to JobPostingModel for display
                    val shiftDisplay = job.shiftTiming.ifBlank { ShiftTiming.FLEXIBLE.displayName }
                    val shiftEnum = ShiftTiming.values().firstOrNull { shift ->
                        shift.name.equals(shiftDisplay, ignoreCase = true) ||
                            shift.displayName.equals(shiftDisplay, ignoreCase = true)
                    } ?: ShiftTiming.FLEXIBLE
                    val jobPosting = JobPostingModel(
                        jobId = job.id,
                        title = job.title,
                        description = job.description,
                        location = job.addressText.ifBlank { job.location },
                        // Bug #6 fix: salary is now a free-form String â€”
                        // pass it through verbatim so "Negotiable",
                        // ranges, and "+" suffixes survive the round-trip.
                        payAmount = job.salary,
                        payType = when (job.salaryType.uppercase()) {
                            "HOURLY" -> PayType.HOURLY
                            "WEEKLY" -> PayType.WEEKLY
                            "MONTHLY" -> PayType.MONTHLY
                            "TASK" -> PayType.TASK
                            else -> PayType.DAILY
                        },
                        category = try { JobCategory.valueOf(job.getCategory().uppercase()) } catch (e: Exception) { JobCategory.HELPER },
                        shiftTiming = shiftEnum,
                        shiftTimingText = shiftDisplay,
                        // Batch-p #8: surface the actual posted vacancies +
                        // application count instead of hard-coding 0. The
                        // job card was reading 0 positions / 0 applications
                        // for every post regardless of reality.
                        vacancies = job.vacancies,
                        employerId = job.employerId,
                        employerName = job.companyName,
                        postedTime = job.createdAt,
                        contactNumber = job.contactNumber,
                        applicationsReceived = applicationCountsByJobId[job.id] ?: 0,
                        isFilled = job.status == "closed",
                        status = job.status,
                        expiresAt = job.expiresAt,
                        imageUrl = job.jobImageUrl
                    )
                    
                    EmployerJobCard(
                        jobPosting = jobPosting,
                        onEditClick = { jobId ->
                            try {
                                Timber.d("EmployerHomeScreen - Edit clicked for job ID: $jobId")
                                Timber.d("EmployerHomeScreen - Job title: ${job.title}")
                                
                                val currentTime = System.currentTimeMillis()
                                val jobPostedTime = job.createdAt
                                
                                if (!JobEditPolicy.canEdit(jobPostedTime, currentTime)) {
                                    Toast.makeText(
                                        context,
                                        JobEditPolicy.blockedMessage(jobPostedTime, currentTime),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Timber.w("EmployerHomeScreen - Job cannot be edited after ${JobEditPolicy.EDIT_WINDOW_HOURS} hours")
                                } else {
                                    navController.navigate(Routes.editJobRoute(jobId))
                                }
                            } catch (e: Exception) {
                                Timber.e("EmployerHomeScreen - Error in edit click: ${e.message}")
                                e.printStackTrace()
                                Toast.makeText(context, context.getString(R.string.error_open_edit_screen, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onViewApplicationsClick = { jobId ->
                            try {
                                Timber.d("EmployerHomeScreen - View applications clicked for job ID: $jobId")
                                navController.navigate(com.example.dutype.navigation.Routes.employerApplicationsJobRoute(jobId))
                            } catch (e: Exception) {
                                Timber.e("EmployerHomeScreen - Error navigating to applications: ${e.message}")
                                e.printStackTrace()
                                Toast.makeText(context, context.getString(R.string.error_open_applications, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShareClick = { jobId ->
                                // Share job functionality
                                Timber.d(" SHARE: onShareClick called with jobId='$jobId', title='${job.title}'")
                                Timber.d(" SHARE: JobPosting.jobId='${jobPosting.jobId}', Job.id='${job.id}'")
                                onShareJob(jobId, job.title)
                            },
                        showActions = true, // Show actions for better interaction
                    )
                }
                
                // View All button removed - all jobs now shown below
            }
        }
    }
}


@Composable
fun EmptyJobsState(onPostJob: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Work,
                contentDescription = "No jobs",
                modifier = Modifier.size(64.dp),
                tint = Color.Gray.copy(alpha = 0.6f)
            )
            Text(
                text = "No Recent Jobs",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start by posting your first job to find great candidates",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
//            Button(
//                onClick = onPostJob,
//                shape = RoundedCornerShape(12.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.primary
//                )
//            )
////            {
////                Icon(Icons.Default.Add, contentDescription = "Post Job")
////                Spacer(modifier = Modifier.width(8.dp))
////                Text("Post Your First Job", fontWeight = FontWeight.Bold)
////            }
        }
    }
}

@Composable
private fun EmployerProfileCompletionPrompt(
    completionPercentage: Int,
    missingFields: List<String>,
    onCompleteProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.InfoLight),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = "Profile",
                    tint = EmployerColors.Info,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Complete Your Company Profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmployerColors.Primary
                    )
                )
            }
            
            Text(
                text = "Complete your profile to access all features and attract better candidates",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EmployerColors.Primary
                )
            )
            
            // Progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Profile Completion",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = EmployerColors.Primary
                        )
                    )
                    Text(
                        text = "$completionPercentage%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmployerColors.Primary
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(EmployerColors.Primary.copy(alpha = 0.18f), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(completionPercentage / 100f)
                            .background(EmployerColors.Info, RoundedCornerShape(3.dp))
                    )
                }
            }
            
            if (missingFields.isNotEmpty()) {
                Text(
                    text = "Missing: ${missingFields.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EmployerColors.TextSecondary
                    )
                )
            }
            
            androidx.compose.material3.Button(
                onClick = onCompleteProfile,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmployerColors.Info
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Complete Profile",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

// NOTE: isToday() and getTimeAgo() removed - use DateTimeUtils instead
// Import: import com.example.dutype.utils.DateTimeUtils
// Usage: DateTimeUtils.isToday(timestamp), DateTimeUtils.formatRelativeTime(timestamp)

// Share job functionality
// Share job functionality with deep link
private fun shareJob(jobId: String, jobTitle: String, context: android.content.Context) {
    Timber.d(" SHARE: Sharing job - jobId='$jobId', title='$jobTitle'")
    
    if (jobId.isBlank()) {
        Timber.e(" SHARE: ERROR - jobId is blank!")
        Toast.makeText(context, context.getString(R.string.share_job_invalid), Toast.LENGTH_SHORT).show()
        return
    }
    
    val jobDeepLink = com.example.dutype.utils.DeepLinkHandler.generateJobWebLink(jobId)
    Timber.d(" SHARE: Generated deep link: $jobDeepLink")
    
    val shareText = """
Hiring Now: $jobTitle

Apply now: $jobDeepLink

Download DutyPe app for instant job alerts
    """.trimIndent()
    
    Timber.d(" SHARE: Share text prepared, length=${shareText.length}")
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_job_subject, jobTitle))
    }
    
    try {
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_job_title)))
        Timber.d(" SHARE: Share intent launched successfully")
    } catch (e: Exception) {
        Timber.e(e, " SHARE: Error launching share intent")
        // Fallback: Copy to clipboard
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(context.getString(R.string.job_share_clip_label), shareText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.job_copied_clipboard), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ApplicationAnalyticsSection(
    navController: NavController,
    applicationViewModel: EmployerApplicationViewModel,
    modifier: Modifier = Modifier,
    viewModel: FirestoreEmployerJobViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Get application statistics from EmployerApplicationViewModel
    val appStats by applicationViewModel.stats.collectAsState()
    
    // Calculate real analytics from job data and application stats
    val totalApplications = appStats.totalApplications
    val activeJobs = uiState.myJobs.count { it.status == "open" }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.job_analytics),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                    )
                )
                TextButton(
                    onClick = { navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_APPLICATIONS) }
                ) {
                    Text(
                        text = stringResource(R.string.view_applications),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EmployerColors.Info
                        )
                    )
                }
            }
            
            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Jobs
                AnalyticsItem(
                    label = stringResource(R.string.active_jobs),
                    value = activeJobs.toString(),
                    icon = Icons.Default.Work,
                    color = EmployerColors.Success,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsItem(
                    label = stringResource(R.string.total_jobs),
                    value = uiState.myJobs.size.toString(),
                    icon = Icons.Default.Analytics,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Recent Activity - Show actual job activity
            if (uiState.myJobs.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                        text = "Recent Job Activity",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = EmployerColors.TextSecondary
                    )
                )
                
                    // Show recent job activities
                    uiState.myJobs.take(3).forEach { job ->
                com.example.dutype.employer.screens.ActivityItem(
                            title = "${job.title} - applications",
                            time = DateTimeUtils.formatRelativeTime(job.createdAt),
                            icon = Icons.Default.Work
                        )
                    }
                }
            }
        }
    }
}

// NOTE: AnalyticsItem and ActivityItem functions moved to AnalyticsScreen.kt
// Import from there: com.example.dutype.employer.screens.AnalyticsItem
// Import from there: com.example.dutype.employer.screens.ActivityItem

