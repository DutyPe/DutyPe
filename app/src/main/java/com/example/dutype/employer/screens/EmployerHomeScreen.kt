package com.example.dutype.employer.screens

import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import androidx.compose.material.icons.filled.Add
import com.dutype.app.R
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
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
import android.net.Uri
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
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
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.JobShift
import com.example.dutype.employer.models.JobStats
import com.example.dutype.viewmodels.EmployerJobsViewModel
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
import com.example.dutype.utils.findActivity
import com.example.dutype.di.rememberInAppReviewTriggerService
import kotlinx.coroutines.delay
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
    val viewModel: EmployerJobsViewModel = hiltViewModel()
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
    val reviewTriggerService = rememberInAppReviewTriggerService()

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
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    var showVoiceJobSheet by remember { mutableStateOf(false) }
    var showVoiceLanguageSheet by remember { mutableStateOf(false) }
    var selectedVoiceLanguage by remember { mutableStateOf(com.example.dutype.employer.voice.VoiceLanguage.TELUGU) }
    LaunchedEffect(employerId) {
        if (employerId != null) {
            Timber.d("EMPLOYER_HOME: Loading jobs for employerId=$employerId")
            viewModel.loadMyJobs()
            instantHelpViewModel.loadEmployerUrgentNeeds()
            applicationViewModel.loadEmployerApplications()
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
                        profileCompletionViewModel.profileStore.start(com.example.dutype.firestore.FirestoreSchema.Values.Role.EMPLOYER)
                        viewModel.loadMyJobs()
                        instantHelpViewModel.loadEmployerUrgentNeeds()
                        applicationViewModel.loadEmployerApplications()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Review prompt: Prompt employer to rate on Google Play after spending some active time (35 seconds)
    LaunchedEffect(Unit) {
        delay(35_000)
        context.findActivity()?.let { activity ->
            reviewTriggerService.onAppUsedForSomeTime(activity)
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

    // Check subscription expiry to trigger on-device push warning
    LaunchedEffect(subscription) {
        val currentUserId = hiltAuth.currentUser?.uid
        val hasActiveSub = subscription.isActive
        if (currentUserId != null && hasActiveSub && subscription.expiresAt > 0L) {
            val msRemaining = subscription.expiresAt - System.currentTimeMillis()
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
                            id = "sub_expiry_${subscription.expiresAt}",
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

        // Employers are not asked on the home screen: notifications are asked for right after their
        // first job is posted (PostJobScreen), when "new applicant" alerts start to matter, and on the
        // notifications screen.
        bottomSheetsShownInSession = true
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
    val employerStatusBarColor = Color.White
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
    val isRefreshing = false
    val error = employerJobUiState.error
    val showEmployerCashBonus = false
    val showEmployerWelcomeCard = false
    val employerWelcomeMessage = when {
        referralConfig.employerUnlimitedJobPostingEnabled && showEmployerCashBonus -> stringResource(R.string.guest_employer_welcome_message_with_bonus)
        referralConfig.employerUnlimitedJobPostingEnabled -> stringResource(R.string.guest_employer_welcome_message_posts)
        showEmployerCashBonus -> stringResource(R.string.guest_employer_welcome_message_bonus)
        else -> stringResource(R.string.guest_employer_welcome_message_posts)
    }

    // Company name from the live own profile (one shared listener)
    val employerProfile by applicationViewModel.profileStore.employer.collectAsStateWithLifecycle()
    val companyName = employerProfile?.displayName.orEmpty()
    var profileSetupStatus by remember { mutableStateOf<com.example.dutype.state.ProfileSetupStatus?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC).bg())
    ) {
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
        var isPullRefreshing by remember { mutableStateOf(false) }
        val isRefreshingActive = isPullRefreshing || isRefreshing
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshingActive,
            onRefresh = {
                coroutineScope.launch {
                    isPullRefreshing = true
                    instantHelpViewModel.loadEmployerUrgentNeeds()
                    announcementViewModel.loadAnnouncements("EMPLOYER")
                    kotlinx.coroutines.delay(1000)
                    isPullRefreshing = false
                }
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshingActive,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Color.White.bg(),
                    color = com.example.dutype.ui.theme.EmployerColors.Primary.fg()
                )
            }
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
                    showLoginBottomSheet = true
                },
                companyName = companyName.ifEmpty { "" },
                unreadCount = unreadNotificationCount,
                headerLottieUrl = dynamicFeatures.headerLottieUrl,
                onNotificationClick = {
                    // Open for guests too: the screen shows its friendly empty state, no login wall.
                    navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_NOTIFICATIONS)
                },
                onVoiceJobClick = {
                    // DutyPe AI: post jobs, hear applicants and hires, all by voice.
                    navController.navigate(com.example.dutype.navigation.Routes.dutypeAiRoute(listen = true))
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

        // Guest Mode Login Bottom Sheet
        com.example.dutype.components.LoginBottomSheet(
            isVisible = showLoginBottomSheet,
            onDismiss = { showLoginBottomSheet = false },
            onLoginSuccess = { showLoginBottomSheet = false },
            role = com.example.dutype.models.UserRole.EMPLOYER,
            title = stringResource(R.string.guest_post_job_title),
            subtitle = stringResource(R.string.guest_post_job_desc),
            navController = navController
        )

        // Pulsing Voice FAB Button (positioned above bottom navigation bar)
        VoicePulsingFab(
            onClick = {
                navController.navigate(com.example.dutype.navigation.Routes.dutypeAiRoute(listen = true))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 105.dp)
        )

        // Language Picker Bottom Sheet (Prompt: Telugu, English, Hindi)
        if (showVoiceLanguageSheet) {
            com.example.dutype.employer.components.VoiceLanguagePickerBottomSheet(
                onDismiss = { showVoiceLanguageSheet = false },
                onLanguageSelected = { lang ->
                    selectedVoiceLanguage = lang
                    showVoiceLanguageSheet = false
                    showVoiceJobSheet = true
                }
            )
        }

        // Voice Job Posting Bottom Sheet
        if (showVoiceJobSheet) {
            com.example.dutype.employer.components.VoiceJobPostingBottomSheet(
                onDismiss = { showVoiceJobSheet = false },
                initialLanguage = selectedVoiceLanguage,
                onPostUrgentJob = { input ->
                    coroutineScope.launch {
                        instantHelpViewModel.createUrgentNeed(input) { requestId ->
                            android.widget.Toast.makeText(context, context.getString(R.string.urgent_need_posted_success), android.widget.Toast.LENGTH_SHORT).show()
                            navController.navigate(
                                com.example.dutype.navigation.Routes.employerUrgentNeedDetailRoute(requestId)
                            )
                        }
                    }
                },
                onEditManually = { input ->
                    navController.navigate(
                        com.example.dutype.navigation.Routes.employerPostUrgentNeedRoute(input.category)
                    )
                },
                employerPhone = employerProfile?.phone.orEmpty(),
                defaultAddress = employerProfile?.address.orEmpty()
            )
        }
    } // Box
}

@Composable
private fun VoicePulsingFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_fab_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_pulse_alpha"
    )

    // Fixed 80.dp container keeps the button and mic icon completely static
    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing wave expands outward around the fixed button
        Box(
            modifier = Modifier
                .size(56.dp + (22.dp * pulseScale))
                .clip(CircleShape)
                .background(Color(0xFF10B981).bg().copy(alpha = pulseAlpha))
        )

        // Floating Action Button - FIXED 56.dp, strictly non-moving
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = Color(0xFF0F172A).bg(),
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Job Posting",
                tint = Color(0xFF10B981).fg(),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun DashboardContent(
    recentJobs: List<JobListing>,
    jobStats: JobStats,
    isLoading: Boolean,
    isRefreshing: Boolean,
    navController: NavController,
    viewModel: EmployerJobsViewModel,
    scrollStateManager: ScrollStateManager? = null,
    onShareJob: (String, String) -> Unit = { _, _ -> },
    context: android.content.Context,
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
    onVoiceJobClick: () -> Unit = {},
    applicationViewModel: EmployerApplicationViewModel = hiltViewModel()
) {
    // Move view model & state collection to composable scope (not inside LazyListScope)
    val appStats by applicationViewModel.stats.collectAsState()
    val updatedStats = remember(jobStats, appStats.totalApplications) {
        jobStats.copy(totalApplications = appStats.totalApplications)
    }
    val hasNormalJobs = recentJobs.isNotEmpty()
    val hasUrgentNeeds = urgentRequests.isNotEmpty()

    val applicationUiState by applicationViewModel.uiState.collectAsStateWithLifecycle()
    var dismissedNudgeApplicationId by remember { mutableStateOf<String?>(null) }
    var showNudgeStopCallsDialog by remember { mutableStateOf(false) }
    var pendingNudgeHiredApp by remember { mutableStateOf<com.example.dutype.models.JobApplication?>(null) }

    val applicationsByJobId = remember(applicationUiState.applications) {
        applicationUiState.applications.groupBy { it.jobId }
    }
    val uncontactedApplications = remember(applicationUiState.applications) {
        applicationUiState.applications.filter { it.status == com.example.dutype.models.ApplicationStatus.APPLIED }
    }
    val totalUrgentResponses = remember(urgentResponsesByRequestId) {
        urgentResponsesByRequestId.values.flatten()
    }
    val unacceptedUrgentResponses = remember(totalUrgentResponses) {
        totalUrgentResponses.filter {
            it.status.equals("pending", ignoreCase = true) ||
            it.status.equals("applied", ignoreCase = true) ||
            it.status.equals("interested", ignoreCase = true)
        }
    }
    val totalPendingWorkers = uncontactedApplications.size + unacceptedUrgentResponses.size

    fun openPhoneDialer(phone: String?) {
        if (phone.isNullOrBlank()) {
            android.widget.Toast.makeText(context, context.getString(R.string.employer_phone_not_available), android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        }.onFailure {
            android.widget.Toast.makeText(context, context.getString(R.string.employer_unable_open_dialer), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /** Reveals the applicant's phone (recorded as an unlock) and hands it to [then]. */
    fun callWorker(jobId: String, workerId: String, then: (String) -> Unit) {
        applicationViewModel.fetchPhoneNumberForWorker(jobId, workerId, onSuccess = then, onFailure = {
            android.widget.Toast.makeText(context, context.getString(R.string.employer_phone_not_available), android.widget.Toast.LENGTH_SHORT).show()
        })
    }

    fun handleHireWorker(application: com.example.dutype.models.JobApplication) {
        applicationViewModel.updateApplicationStatus(
            applicationId = application.id,
            newStatus = com.example.dutype.models.ApplicationStatus.HIRED,
            notes = "Hired directly from home screen"
        )
        android.widget.Toast.makeText(context, context.getString(R.string.employer_home_worker_marked_hired, application.workerName.ifBlank { "Worker" }), android.widget.Toast.LENGTH_SHORT).show()
    }

    val recentNudgeCandidate = remember(applicationUiState.applications, dismissedNudgeApplicationId) {
        applicationUiState.applications
            .filter { it.status == com.example.dutype.models.ApplicationStatus.APPLIED && it.id != dismissedNudgeApplicationId }
            .maxByOrNull { it.createdAt }
    }

    if (isLoading && recentJobs.isEmpty()) {
        // Show loading when first coming to the page
        LoadingScreen()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC).bg())
        ) {
            EmployerHomeHeader(
                companyName = companyName,
                unreadCount = unreadCount,
                onNotificationClick = onNotificationClick,
                modifier = Modifier.fillMaxWidth()
            )

            ScrollAwareLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                scrollStateManager = scrollStateManager
            ) {
                // A new employer (no jobs, no urgent requests yet) gets one welcome section: how to
                // start, in a clean layout, instead of zero counters and three unrelated cards.
                val isFirstTime = recentJobs.isEmpty() && urgentRequests.isEmpty() && !isLoadingUrgentRequests
                if (isFirstTime) item {
                    EmployerWelcomeSection(
                        ownerName = companyName,
                        onPostJob = { navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_POST_JOB) },
                        onPostUrgent = {
                            navController.navigate(com.example.dutype.navigation.Routes.employerPostUrgentNeedRoute(null))
                        },
                        onVoice = onVoiceJobClick
                    )
                }

                if (!isFirstTime) item {
                    EmployerHeroActionCards(
                        onPostRegularClick = {
                            navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_POST_JOB)
                        },
                        onPostUrgentClick = {
                            navController.navigate(
                                com.example.dutype.navigation.Routes.employerPostUrgentNeedRoute(null)
                            )
                        }
                    )
                }

                if (!isFirstTime) item {
                    com.example.dutype.employer.components.VoiceJobTriggerCard(
                        onClick = onVoiceJobClick,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                if (!isFirstTime) item {
                    EmployerStatsRow(
                        activeJobs = updatedStats.activeJobs,
                        applicants = updatedStats.totalApplications,
                        creditsLeft = subscription.normalCredits + subscription.instantCredits,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                if (!isFirstTime) item {
                    EmployerSectionLabel(
                        text = stringResource(R.string.employer_home_active_openings),
                        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
                    )
                }

                val activeOpenJobs = recentJobs.filter { it.status.equals("open", ignoreCase = true) }
                if (isFirstTime) {
                    // The welcome card above already leads to posting.
                } else if (activeOpenJobs.isEmpty()) {
                    item {
                        EmployerNoActiveJobsCard(
                            onPostJobClick = {
                                navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_POST_JOB)
                            }
                        )
                    }
                } else {
                    items(activeOpenJobs, key = { it.id }) { job ->
                        val jobApps = applicationsByJobId[job.id].orEmpty()
                        val count = maxOf(job.applicationCount, jobApps.size)
                        EmployerJobManagementCard(
                            title = job.title,
                            applicantCount = count,
                            isUrgent = job.urgency.equals("HIGH", ignoreCase = true),
                            facepile = jobApps.take(3).map {
                                EmployerFacepileItem(it.workerName, it.workerPhoto)
                            },
                            onReviewClick = {
                                navController.navigate(
                                    com.example.dutype.navigation.Routes.employerApplicationsJobRoute(job.id)
                                )
                            },
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (showNudgeStopCallsDialog && pendingNudgeHiredApp != null) {
            val app = pendingNudgeHiredApp!!
            AlertDialog(
                onDismissRequest = {
                    showNudgeStopCallsDialog = false
                    pendingNudgeHiredApp = null
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🎉", fontSize = 20.sp)
                        Text(stringResource(R.string.employer_nudge_confirm_hiring), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.employer_nudge_hiring_for, app.workerName.ifBlank { "this worker" }, app.jobTitle.ifBlank { "this job" }))
                        Text(
                            stringResource(R.string.employer_nudge_stop_calls_question),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFDC2626).fg()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetJobId = app.jobId
                            applicationViewModel.updateApplicationStatus(app.id, com.example.dutype.models.ApplicationStatus.HIRED, "Hired from home nudge (calls stopped)")
                            if (targetJobId.isNotBlank()) {
                                viewModel.markFilled(targetJobId) { _, _ -> }
                            }
                            dismissedNudgeApplicationId = app.id
                            showNudgeStopCallsDialog = false
                            pendingNudgeHiredApp = null
                            Toast.makeText(context, context.getString(R.string.employer_nudge_calls_stopped_toast), Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626).bg()),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.employer_nudge_stop_calls_btn), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            applicationViewModel.updateApplicationStatus(app.id, com.example.dutype.models.ApplicationStatus.HIRED, "Hired from home nudge (keep open)")
                            dismissedNudgeApplicationId = app.id
                            showNudgeStopCallsDialog = false
                            pendingNudgeHiredApp = null
                            Toast.makeText(context, context.getString(R.string.employer_nudge_keep_open_toast), Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.employer_nudge_keep_open_btn))
                    }
                }
            )
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
            // Solid role background — the shimmer skeleton sits on the same
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
            Spacer(modifier = Modifier.height(12.dp))
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
                text = stringResource(R.string.auto_your_dashboard),
                style = AppTypography.sectionHeader.copy(
                    color = EmployerColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (onViewAnalytics != null) {
                TextButton(onClick = onViewAnalytics) {
                    Text(stringResource(R.string.view_analytics), style = AppTypography.buttonMedium.copy(color = EmployerColors.Primary.fg()))
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
    applicationsByJobId: Map<String, List<com.example.dutype.models.JobApplication>> = emptyMap(),
    onCallWorker: (com.example.dutype.models.JobApplication) -> Unit = {},
    onHireWorker: (com.example.dutype.models.JobApplication) -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
            Text(
                text = stringResource(R.string.auto_recent_job_postings),
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
                        text = stringResource(R.string.auto_refreshing_recent_jobs),
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
                    EmployerJobCard(
                        jobPosting = job,
                        applications = applicationsByJobId[job.id].orEmpty(),
                        onCallWorker = onCallWorker,
                        onHireWorker = onHireWorker,
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
    com.example.dutype.components.DutyPeEmptyState(
        icon = Icons.Default.Work,
        badge = Icons.Filled.Add,
        tone = com.example.dutype.components.EmptyTone.BLUE,
        title = stringResource(R.string.empty_employer_jobs_title),
        message = stringResource(R.string.empty_employer_jobs_body),
        tips = listOf(
            stringResource(R.string.empty_employer_tip_pay),
            stringResource(R.string.empty_employer_tip_nearby)
        ),
        primary = com.example.dutype.components.EmptyStateAction(
            label = stringResource(R.string.post_a_job),
            icon = Icons.Filled.Add,
            onClick = onPostJob
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(EmployerColors.CardBackground, RoundedCornerShape(20.dp))
            .border(1.dp, EmployerColors.Border, RoundedCornerShape(20.dp))
    )
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

// NOTE: AnalyticsItem and ActivityItem functions moved to AnalyticsScreen.kt
// Import from there: com.example.dutype.employer.screens.AnalyticsItem
// Import from there: com.example.dutype.employer.screens.ActivityItem

