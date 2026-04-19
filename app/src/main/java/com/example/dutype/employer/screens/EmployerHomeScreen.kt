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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.dutype.employer.components.EmployerJobCard
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
import com.example.dutype.employer.models.JobUrgency
import com.example.dutype.employer.models.JobPerk
import com.example.dutype.employer.models.JobStats
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ApplicationStateManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.JobListing
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
    
    // NotificationService for unread count (lightweight)
    val notificationService = remember { 
        com.example.dutype.services.NotificationService(
            context,
            hiltFirestore
        )
    }
    val employerJobUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appStats by applicationViewModel.stats.collectAsStateWithLifecycle()
    
    // Announcement ViewModel for in-app announcements
    val announcementViewModel: com.example.dutype.viewmodels.AnnouncementViewModel = hiltViewModel()
    val announcements by announcementViewModel.announcements.collectAsStateWithLifecycle()
    
    // Unread notification count for badge (lightweight - only count, not full notifications)
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    
    // Birthday wish state 🎂
    val birthdayService = remember { 
        BirthdayService(
            hiltFirestore,
            hiltAuth,
            com.example.dutype.services.NotificationService(context, hiltFirestore)
        )
    }
    var birthdayInfo by remember { mutableStateOf<BirthdayInfo?>(null) }
    var showBirthdayBanner by remember { mutableStateOf(false) }
    
    // State for sharing and job actions
    var jobToShare by remember { mutableStateOf<Pair<String, String>?>(null) }
    var jobToToggle by remember { mutableStateOf<String?>(null) }
    
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
    val employerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(employerId) {
        if (employerId != null) {
            Timber.d("🏢 EMPLOYER_HOME: Loading jobs for employerId=$employerId")
            viewModel.loadMyJobs()
        } else {
            Timber.w("🏢 EMPLOYER_HOME: No employerId - user not authenticated")
        }
        
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
                
                // 🎂 Check if today is user's birthday
                if (!birthdayService.hasWishedToday(context, userId)) {
                    val bday = birthdayService.checkIfBirthday(userId)
                    if (bday != null) {
                        birthdayInfo = bday
                        showBirthdayBanner = true
                        // Send birthday notification
                        birthdayService.sendBirthdayNotification(userId, bday.userName)
                        // Mark as wished today to avoid duplicates
                        birthdayService.markWishedToday(context, userId)
                        Timber.i("🎂 Happy Birthday ${bday.userName}! Banner and notification sent.")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch unread notification count or check birthday")
            }
        }
    }
    
    // Note: We don't track views for employers viewing their own jobs
    
    // Handle permissions: Permissions are now requested on SelectRoleScreen after onboarding
    // Here we only show bottom sheets for returning users who denied permissions
    LaunchedEffect(Unit) {
        Timber.d("🏠 EmployerHomeScreen - Checking permission status for bottom sheets")
        Timber.d("🏠 EmployerHomeScreen - hasNotificationPermission: $hasNotificationPermission")
        
        // Only show bottom sheets for denied permissions (permissions are requested on SelectRoleScreen)
        if (!bottomSheetsShownInSession) {
            bottomSheetsShownInSession = true
            if (!hasNotificationPermission) {
                Timber.d("🏠 EmployerHomeScreen - Showing notification bottom sheet for denied permission")
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
    
    // Handle job toggle
    LaunchedEffect(jobToToggle) {
        jobToToggle?.let { jobId ->
            viewModel.toggleJobStatus(jobId)
            jobToToggle = null
        }
    }
    
    // Helper functions to handle job actions
    val handleJobToggle = remember { { jobId: String -> jobToToggle = jobId } }
    val handleJobShare = remember { { jobId: String, jobTitle: String -> jobToShare = Pair(jobId, jobTitle) } }
    
    // Set status bar color once on entering this screen.
    LaunchedEffect(Unit) {
        onStatusBarColorChange(EmployerColors.StatusBarColor)
    }

    val recentJobs: List<JobListing> = employerJobUiState.myJobs
    val jobStats = JobStats(
        activeJobs = recentJobs.count { it.status == "open" },
        totalApplications = appStats.totalApplications,
        todayJobs = recentJobs.count { DateTimeUtils.isToday(it.createdAt) },
        totalJobs = recentJobs.size
    )
    val isLoading = employerJobUiState.isLoading
    val isRefreshing = employerJobUiState.isRefreshing
    val error = employerJobUiState.error
    
    // Company name state - starts empty, will be populated from profile
    var companyName by remember { mutableStateOf("") }
    var profileSetupStatus by remember { mutableStateOf<com.example.dutype.state.ProfileSetupStatus?>(null) }
    
    // Load company name from profile data
    LaunchedEffect(Unit) {
        try {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Load company name from Firestore employer profile data
                val employerProfileResult = profileCompletionViewModel.getEmployerProfileData(currentUser.uid)
                employerProfileResult.fold(
                    onSuccess = { data ->
                        val savedCompanyName = data["companyName"] as? String
                        if (!savedCompanyName.isNullOrBlank()) {
                            companyName = savedCompanyName
                            Timber.d("🏠 EmployerHomeScreen - Loaded company name: $companyName")
                        }
                    },
                    onFailure = { e ->
                        Timber.e("🏠 EmployerHomeScreen - Error loading company name: ${e.message}")
                    }
                )
            }
        } catch (e: Exception) {
            // Handle error - keep empty company name
            Timber.e("🏠 EmployerHomeScreen - Exception loading profile: ${e.message}")
            companyName = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Solid role background — every employer screen shares the same
            // calm light-blue surface so the role identity stays consistent.
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        EmployerHomeBackdropDecor(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
        // Offline banner at the very top
        val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
        val isOnline by connectivityViewModel.isOnline.collectAsState()
        com.example.dutype.components.OfflineBanner(isOffline = !isOnline)
        
        WelcomeHeader(
            companyName = companyName.ifEmpty { "" },
            unreadCount = unreadNotificationCount,
            onNotificationClick = {
                navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_NOTIFICATIONS)
            }
        )
        
        // 🎂 Birthday Banner - Shows if today is user's birthday
        if (showBirthdayBanner && birthdayInfo != null) {
            BirthdayBanner(
                userName = birthdayInfo!!.userName,
                onDismiss = { showBirthdayBanner = false }
            )
        }
        
        // 📢 In-App Announcements - Feature updates, banners
        if (announcements.isNotEmpty()) {
            AnnouncementList(
                announcements = announcements,
                onDismiss = { announcementId ->
                    announcementViewModel.dismissAnnouncement(announcementId)
                },
                onAction = { announcement ->
                    announcement.actionRoute?.let { route: String ->
                        DeepLinkHandler.handleDeepLink(route, navController)
                    }
                }
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
                onToggleJob = handleJobToggle,
                onShareJob = handleJobShare,
                context = context
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
        
        // Voice Job Posting FAB - Post jobs using voice commands
        // TODO: Uncomment for future release - AI/Voice features will be released in next version
        /*
        FloatingActionButton(
            onClick = {
                navController.navigate(Routes.EMPLOYER_VOICE_POST_JOB)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 30.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Job Posting",
                modifier = Modifier.size(28.dp)
            )
        }
        */
        
        // Notification permission bottom sheet
        NotificationPermissionBottomSheet(
            isVisible = showNotificationBottomSheet,
            onDismiss = { showNotificationBottomSheet = false },
            onEnableNotifications = {
                openNotificationSettings(context)
            },
            userRole = "employer"
        )
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
    onToggleJob: (String) -> Unit = {},
    onShareJob: (String, String) -> Unit = { _, _ -> },
    context: android.content.Context,
    applicationViewModel: EmployerApplicationViewModel = hiltViewModel()
) {
    // Move view model & state collection to composable scope (not inside LazyListScope)
    val appStats by applicationViewModel.stats.collectAsState()
    val updatedStats = remember(jobStats, appStats.totalApplications) {
        jobStats.copy(totalApplications = appStats.totalApplications)
    }

    if (isLoading && recentJobs.isEmpty()) {
        // Show loading when first coming to the page
        LoadingScreen()
    } else {
        ScrollAwareLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 80.dp
            ),
            scrollStateManager = scrollStateManager
        ) {
            item {
                EnhancedStatsGrid(updatedStats, onViewAnalytics = { navController.navigate(com.example.dutype.navigation.Routes.ANALYTICS) })
            }
            
            // Job Analytics Card removed per task list requirement

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                RecentJobsSection(
                    jobs = recentJobs,
                    navController = navController,
                    onRefresh = { viewModel.refreshMyJobs() },
                    isRefreshing = isRefreshing,
                    onViewAllClick = { 
                        // Navigate to posted jobs screen
                        navController.navigate("employer_my_jobs")
                    },
                    onTabSwitch = { /* No longer needed */ },
                    onToggleJob = onToggleJob,
                    onShareJob = onShareJob,
                    context = context
                )
            }

            item {
                com.example.dutype.components.MadeWithLoveFooter()
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
        }
    }
}

@Composable
fun WelcomeHeader(
    companyName: String,
    unreadCount: Int = 0,
    onNotificationClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)  // Transparent to show gradient background
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show welcoming text instead of company name when not logged in
        val greetingText = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning!"
            in 12..16 -> "Good Afternoon!"
            else -> "Good Evening!"
        }
        
        Text(
            text = if (companyName.isNotEmpty()) companyName else greetingText,
            style = AppTypography.displayTitle.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = WorkerColors.TextPrimary
            ),
            modifier = Modifier.weight(1f)
        )
        
        // Notification icon with badge
        Box {
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,  // Changed to outlined like worker screen
                    contentDescription = "Notifications",
                    tint = WorkerColors.TextPrimary,
                    modifier = Modifier.size(24.dp)  // Consistent size with worker screen
                )
            }
            
            // Notification badge - simple dot without count (matching worker side)
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            WorkerColors.Error,
                            shape = CircleShape
                        )
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                )
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
                    color = Color(0xFF0F172A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (onViewAnalytics != null) {
                TextButton(onClick = onViewAnalytics) {
                    Text(stringResource(R.string.view_analytics), style = AppTypography.buttonMedium.copy(color = Color(0xFF2563EB)))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassStatCard("Active", stats.activeJobs.toString(), Icons.Default.Work, Color(0xFF10B981), Modifier.weight(1f))
            GlassStatCard("Applications", stats.totalApplications.toString(), Icons.Default.People, Color(0xFF3B82F6), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassStatCard("Today", stats.todayJobs.toString(), Icons.Default.CalendarToday, Color(0xFF8B5CF6), Modifier.weight(1f))
            GlassStatCard("Total", stats.totalJobs.toString(), Icons.Default.Analytics, Color(0xFFF59E0B), Modifier.weight(1f))
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
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
                        color = Color(0xFF0F172A),
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
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
    onToggleJob: (String) -> Unit = {},
    onShareJob: (String, String) -> Unit = { _, _ -> },
    context: android.content.Context
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
                    val jobPosting = JobPostingModel(
                        jobId = job.id,
                        title = job.title,
                        description = job.description,
                        location = job.addressText.ifBlank { job.location },
                        payAmount = job.salary.toInt().toString(),
                        payType = when (job.salaryType.uppercase()) {
                            "HOURLY" -> PayType.HOURLY
                            "MONTHLY" -> PayType.MONTHLY
                            "TASK" -> PayType.TASK
                            else -> PayType.DAILY
                        },
                        category = try { JobCategory.valueOf(job.getCategory().uppercase()) } catch (e: Exception) { JobCategory.HELPER },
                        shiftTiming = ShiftTiming.FLEXIBLE,
                        urgency = JobUrgency.FLEXIBLE,
                        vacancies = 0,
                        employerId = job.employerId,
                        employerName = job.companyName,
                        postedTime = job.createdAt,
                        contactNumber = job.contactNumber,
                        isActive = job.status == "open",
                        applicationsReceived = 0,
                        isFilled = job.status == "closed"
                    )
                    
                    EmployerJobCard(
                        jobPosting = jobPosting,
                        onEditClick = { jobId ->
                            try {
                                Timber.d("🔍 EmployerHomeScreen - Edit clicked for job ID: $jobId")
                                Timber.d("🔍 EmployerHomeScreen - Job title: ${job.title}")
                                
                                // Industry standard: Allow editing within 7 days of posting
                                val currentTime = System.currentTimeMillis()
                                val jobPostedTime = job.createdAt
                                val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L // 7 days
                                
                                if (currentTime - jobPostedTime > sevenDaysInMillis) {
                                    val daysSincePosted = (currentTime - jobPostedTime) / (24 * 60 * 60 * 1000)
                                    Toast.makeText(
                                        context, 
                                        "Jobs can only be edited within 7 days of posting. This job was posted $daysSincePosted days ago.", 
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Timber.w("🔍 EmployerHomeScreen - Job cannot be edited, posted $daysSincePosted days ago")
                                } else {
                                    navController.navigate(Routes.editJobRoute(jobId))
                                }
                            } catch (e: Exception) {
                                Timber.e("🔍 EmployerHomeScreen - Error in edit click: ${e.message}")
                                e.printStackTrace()
                                Toast.makeText(context, "Error opening edit screen: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onViewApplicationsClick = { jobId ->
                            try {
                                Timber.d("🔍 EmployerHomeScreen - View applications clicked for job ID: $jobId")
                                navController.navigate("employer_applications_job/$jobId")
                            } catch (e: Exception) {
                                Timber.e("🔍 EmployerHomeScreen - Error navigating to applications: ${e.message}")
                                e.printStackTrace()
                                Toast.makeText(context, "Error opening applications: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                            onToggleActiveClick = { jobId ->
                                // Toggle job active status
                                onToggleJob(jobId)
                            },
                            onShareClick = { jobId ->
                                // Share job functionality
                                Timber.d("📤 SHARE: onShareClick called with jobId='$jobId', title='${job.title}'")
                                Timber.d("📤 SHARE: JobPosting.jobId='${jobPosting.jobId}', Job.id='${job.id}'")
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
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
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Complete Your Company Profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                )
            }
            
            Text(
                text = "Complete your profile to access all features and attract better candidates",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF1E40AF)
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
                            color = Color(0xFF1E40AF)
                        )
                    )
                    Text(
                        text = "$completionPercentage%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color(0xFFE0E7FF), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(completionPercentage / 100f)
                            .background(Color(0xFF3B82F6), RoundedCornerShape(3.dp))
                    )
                }
            }
            
            if (missingFields.isNotEmpty()) {
                Text(
                    text = "Missing: ${missingFields.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
            
            androidx.compose.material3.Button(
                onClick = onCompleteProfile,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
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
    Timber.d("📤 SHARE: Sharing job - jobId='$jobId', title='$jobTitle'")
    
    if (jobId.isBlank()) {
        Timber.e("📤 SHARE: ERROR - jobId is blank!")
        Toast.makeText(context, "Error: Cannot share job (invalid job ID)", Toast.LENGTH_SHORT).show()
        return
    }
    
    val jobDeepLink = com.example.dutype.utils.DeepLinkHandler.generateJobWebLink(jobId)
    Timber.d("📤 SHARE: Generated deep link: $jobDeepLink")
    
    val shareText = """
🎯 Hiring Now: $jobTitle

👉 Apply now: $jobDeepLink

📲 Download DutyPe app for instant job alerts
    """.trimIndent()
    
    Timber.d("📤 SHARE: Share text prepared, length=${shareText.length}")
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Job: $jobTitle")
    }
    
    try {
        context.startActivity(Intent.createChooser(shareIntent, "Share Job"))
        Timber.d("📤 SHARE: Share intent launched successfully")
    } catch (e: Exception) {
        Timber.e(e, "📤 SHARE: Error launching share intent")
        // Fallback: Copy to clipboard
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Job Share", shareText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Job details copied to clipboard", Toast.LENGTH_SHORT).show()
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
    val pausedJobs = uiState.myJobs.count { it.status != "open" }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    text = "Job Analytics",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                TextButton(
                    onClick = { navController.navigate("employer_applications") }
                ) {
                    Text(
                        text = "View Applications",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF3B82F6)
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
                    label = "Active Jobs",
                    value = activeJobs.toString(),
                    icon = Icons.Default.Work,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                // Paused Jobs
                AnalyticsItem(
                    label = "Paused Jobs",
                    value = pausedJobs.toString(),
                    icon = Icons.Default.Pause,
                    color = Color(0xFFF59E0B),
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
                        color = Color(0xFF374151)
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
