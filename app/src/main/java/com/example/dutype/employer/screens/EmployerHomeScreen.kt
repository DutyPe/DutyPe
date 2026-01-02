package com.example.dutype.employer.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.employer.components.EmployerJobCard
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
import com.example.dutype.employer.models.JobUrgency
import com.example.dutype.employer.models.JobPerk
import com.example.dutype.employer.viewmodels.EmployerViewModel
import com.example.dutype.employer.viewmodels.JobStats
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.services.JobApplicationService
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ApplicationStateManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dutype.auth.AuthManager
import com.example.dutype.models.JobListing
import com.example.dutype.utils.ScrollStateManager
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
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.components.NotificationPermissionBottomSheet
import com.example.dutype.components.openNotificationSettings
import com.example.dutype.employer.viewmodels.EmployerNotificationViewModel
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
    val viewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val notificationViewModel: EmployerNotificationViewModel = hiltViewModel()
    // JobApplicationService accessed via JobApplicationViewModel (proper DI pattern)
    val jobApplicationViewModel: com.example.dutype.viewmodels.JobApplicationViewModel = hiltViewModel()
    val jobApplicationService = jobApplicationViewModel.jobApplicationService
    val employerJobUiState by viewModel.uiState.collectAsState()
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    
    // Job vacancy status tracking
    var jobVacancyStatuses by remember { mutableStateOf<Map<String, JobVacancyStatus>>(emptyMap()) }
    
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
    
    // Load employer jobs
    LaunchedEffect(Unit) {
        viewModel.loadMyJobs()
        notificationViewModel.loadNotifications() // Load notifications to update badge
    }
    
    // Load job vacancy statuses
    LaunchedEffect(employerJobUiState.myJobs) {
        employerJobUiState.myJobs.forEach { job ->
            jobApplicationService.getJobVacancyStatus(job.jobId).onSuccess { status ->
                jobVacancyStatuses = jobVacancyStatuses + (job.jobId to status)
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
    
    // White background for employer side
    val statusBarColor = Color.White

    // Update status bar color to white
    LaunchedEffect(Unit) {
        onStatusBarColorChange(statusBarColor)
    }

    val recentJobs: List<JobListing> = employerJobUiState.myJobs
    val jobStats = JobStats(
        activeJobs = recentJobs.count { it.isActive },
        totalApplications = recentJobs.sumOf { it.applicationCount.toInt() },
        todayJobs = recentJobs.count { DateTimeUtils.isToday(it.postedAt) },
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        WelcomeHeader(
            companyName = companyName.ifEmpty { "" },
            unreadCount = notificationUiState.unreadCount,
            onNotificationClick = {
                navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_NOTIFICATIONS)
            }
        )
        
        // Profile completion prompt removed - not needed for hyper-local employers

        // Show dashboard content directly
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
                context = context,
                jobVacancyStatuses = jobVacancyStatuses
            )

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
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
        
        // Notification permission bottom sheet
        NotificationPermissionBottomSheet(
            isVisible = showNotificationBottomSheet,
            onDismiss = { showNotificationBottomSheet = false },
            onEnableNotifications = {
                openNotificationSettings(context)
            },
            userRole = "employer"
        )
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
    jobVacancyStatuses: Map<String, JobVacancyStatus> = emptyMap(),
    applicationViewModel: EmployerApplicationViewModel = hiltViewModel()
) {
    // Move view model & state collection to composable scope (not inside LazyListScope)
    val appStats by applicationViewModel.stats.collectAsStateWithLifecycle()
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
                    context = context,
                    jobVacancyStatuses = jobVacancyStatuses
                )
            }
            
            // Footer
            item {
                EmployerFooterContent()
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
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
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
        modifier = Modifier.fillMaxSize()
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show only company name - bold and smaller text
        Text(
            text = companyName.ifEmpty { "Company" },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color(0xFF1F2937)
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
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            // Notification badge - simple dot without count (matching worker side)
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

@Composable
fun EnhancedStatsGrid(stats: JobStats, onViewAnalytics: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    text = "Your Dashboard",
                    style = com.example.dutype.ui.theme.AppTypography.sectionHeader.copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                if (onViewAnalytics != null) {
                    TextButton(onClick = onViewAnalytics) {
                        Text(
                            text = "View Analytics",
                            style = com.example.dutype.ui.theme.AppTypography.buttonMedium
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Paused Jobs",
                    value = stats.pausedJobs.toString(),
                    icon = Icons.Default.Pause,
                    color = Color(0xFFF59E0B), // Amber - matching worker screens
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Applications",
                    value = stats.totalApplications.toString(),
                    icon = Icons.Default.PersonAdd,
                    color = Color(0xFF10B981), // Green - matching worker screens
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Today's Posts",
                    value = stats.todayJobs.toString(),
                    icon = Icons.Default.CalendarToday,
                    color = Color(0xFF3B82F6), // Blue - matching worker screens
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Jobs",
                    value = stats.totalJobs.toString(),
                    icon = Icons.Default.Analytics,
                    color = Color(0xFF8B5CF6), // Purple - matching worker screens
                    modifier = Modifier.weight(1f)
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
    context: android.content.Context,
    jobVacancyStatuses: Map<String, JobVacancyStatus> = emptyMap()
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
                        jobId = job.jobId,
                        title = job.title,
                        description = job.description,
                        location = job.location,
                        payAmount = job.payAmount?.split("/")?.get(0) ?: "0",
                        payType = when {
                            job.payAmount?.contains("hour", ignoreCase = true) == true -> PayType.HOURLY
                            job.payAmount?.contains("day", ignoreCase = true) == true -> PayType.DAILY
                            job.payAmount?.contains("month", ignoreCase = true) == true -> PayType.MONTHLY
                            else -> PayType.DAILY
                        },
                        category = try { JobCategory.valueOf(job.category.uppercase()) } catch (e: Exception) { JobCategory.HELPER },
                        shiftTiming = try { ShiftTiming.valueOf(job.shiftTiming.uppercase()) } catch (e: Exception) { ShiftTiming.FLEXIBLE },
                        urgency = if (job.isUrgent()) JobUrgency.URGENT else JobUrgency.FLEXIBLE,
                        vacancies = job.vacancies,
                        employerId = job.employerId,
                        employerName = job.company ?: job.employerId,
                        postedTime = job.postedAt,
                        contactNumber = job.contactNumber,
                        isActive = job.isActive,
                        applicationsReceived = job.applicationCount.toInt(),
                        isFilled = jobVacancyStatuses[job.jobId] == JobVacancyStatus.FILLED
                    )
                    
                    EmployerJobCard(
                        jobPosting = jobPosting,
                        onEditClick = { jobId ->
                            try {
                                Timber.d("🔍 EmployerHomeScreen - Edit clicked for job ID: $jobId")
                                Timber.d("🔍 EmployerHomeScreen - Job title: ${job.title}")
                                Timber.d("🔍 EmployerHomeScreen - Job posted at: ${job.postedAt}")
                                
                        // Check if job can be edited (within 48 hours)
                        val currentTime = System.currentTimeMillis()
                        val jobPostedTime = job.postedAt
                        val fortyEightHoursInMillis = 48 * 60 * 60 * 1000L // 48 hours in milliseconds
                                
                                Timber.d("🔍 EmployerHomeScreen - Current time: $currentTime")
                                Timber.d("🔍 EmployerHomeScreen - Job posted time: $jobPostedTime")
                                Timber.d("🔍 EmployerHomeScreen - Time difference: ${currentTime - jobPostedTime}")
                                
                        if (currentTime - jobPostedTime > fortyEightHoursInMillis) {
                            val hoursSincePosted = (currentTime - jobPostedTime) / (60 * 60 * 1000)
                            Toast.makeText(
                                context, 
                                "Job cannot be edited after 48 hours. Posted $hoursSincePosted hours ago.", 
                                Toast.LENGTH_LONG
                            ).show()
                                    Timber.w("🔍 EmployerHomeScreen - Job cannot be edited, posted $hoursSincePosted hours ago")
                                } else {
                                    Timber.d("🔍 EmployerHomeScreen - Navigating to edit job screen")
                                    navController.navigate(Routes.editJobRoute(jobId))
                                }
                            } catch (e: Exception) {
                                Timber.e("🔍 EmployerHomeScreen - Error in edit click: ${e.message}")
                                e.printStackTrace()
                                Toast.makeText(context, "Error opening edit screen: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onViewApplicationsClick = { jobId ->
                            navController.navigate("employer_applications_job/$jobId")
                        },
                            onToggleActiveClick = { jobId ->
                                // Toggle job active status
                                onToggleJob(jobId)
                            },
                            onShareClick = { jobId ->
                                // Share job functionality
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
private fun shareJob(jobId: String, jobTitle: String, context: android.content.Context) {
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
    val shareText = """
🎯 *Job Opportunity: $jobTitle*

📱 Apply now on DutyPe App!
📲 Download: $playStoreUrl

Job ID: $jobId

#DutyPe #JobOpportunity #Hiring #LocalJobs
    """.trimIndent()
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Job Opportunity: $jobTitle")
    }
    
    try {
        context.startActivity(Intent.createChooser(shareIntent, "Share Job"))
    } catch (e: Exception) {
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
    val appStats by applicationViewModel.stats.collectAsStateWithLifecycle()
    
    // Calculate real analytics from job data and application stats
    val totalApplications = appStats.totalApplications
    val activeJobs = uiState.myJobs.count { it.isActive }
    val pausedJobs = uiState.myJobs.count { !it.isActive }
    
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
                            title = "${job.title} - ${job.applicationCount} applications",
                            time = DateTimeUtils.formatRelativeTime(job.postedAt),
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

@Composable
private fun EmployerFooterContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "Made with",
            style = com.example.dutype.ui.theme.AppTypography.footerText,
            color = Color(0xFF6B7280)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "💙",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "in Bharat",
            style = com.example.dutype.ui.theme.AppTypography.footerText,
            color = Color(0xFF6B7280)
        )
    }
}
