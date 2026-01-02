package com.example.dutype.worker.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.NotificationPermissionBottomSheet
import com.example.dutype.components.openNotificationSettings
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.navigation.Routes
import com.example.dutype.services.JobApplicationService
import com.example.dutype.services.NotificationService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.components.ReusableSearchBar
import com.example.dutype.ui.theme.WorkerGradientBackground
import com.example.dutype.components.JobCardShimmer
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.JobApplicationViewModel
import com.example.dutype.viewmodels.ProfileViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.worker.components.JobCard
import com.example.dutype.worker.viewmodels.WorkerNotificationViewModel
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.utils.LocationService
import com.example.dutype.metadata.MetadataManager
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState  
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

// Helper function to open DutyPe app settings
fun openLocationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

// Helper function to check if a job has been applied to
fun hasAppliedToJob(jobId: String, applications: List<JobApplication>): Boolean {
    return applications.any { application ->
        application.jobId == jobId &&
                application.status != ApplicationStatus.REJECTED
    }
}

data class HomeUiState( 
    val jobListings: List<JobListing> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val hasError: Boolean = false
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalPagerApi::class
)
@Composable
fun WorkerHomeScreen(
    navController: NavController,
    rootNavController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null,
    notificationPermissionManager: NotificationPermissionManager
) {
    val context = LocalContext.current
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    // LocationPreferences accessed via FirestoreJobViewModel (proper DI pattern)
    val locationPreferences = jobViewModel.locationPreferences
    val currentLocation by locationPreferences.currentLocation.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val jobApplicationViewModel: JobApplicationViewModel = hiltViewModel()
    val smartApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    val dataStore: ApplicationFormDataStore = remember { ApplicationFormDataStore(context) }
    val notificationViewModel: WorkerNotificationViewModel = hiltViewModel()
    // NOTE: ProfileCompletionViewModel removed - was unused (P2 Task 8 refactoring)
    val scope = rememberCoroutineScope()
    // Services accessed via ViewModels (proper DI pattern - no ServiceProviders)
    val jobApplicationService = jobApplicationViewModel.jobApplicationService
    val locationService = jobViewModel.locationService
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    val jobUiState by jobViewModel.uiState.collectAsState()
    val profileUiState by profileViewModel.uiState.collectAsState()
    val jobApplicationUiState by jobApplicationViewModel.uiState.collectAsStateWithLifecycle()
    val applications = jobApplicationUiState.applications

    // View tracking state
    var jobVacancyStatuses by remember { mutableStateOf<Map<String, JobVacancyStatus>>(emptyMap()) }
    var clickedJobId by remember { mutableStateOf<String?>(null) }

    // Permission handling - Check permissions only once
    var hasNotificationPermission by remember {
        mutableStateOf(notificationPermissionManager.isNotificationPermissionGranted())
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Location loading state
    var isLocationLoading by remember { mutableStateOf(false) }

    // Track if permissions have been requested to avoid repeated requests
    var permissionsRequested by remember { mutableStateOf(false) }
    var isFirstTimeUser by remember { mutableStateOf(true) }

    // Bottom sheet state - declare before permission launchers
    var showNotificationBottomSheet by remember { mutableStateOf(false) }

    // Track if bottom sheets have been shown in this app session
    var bottomSheetsShownInSession by remember { mutableStateOf(false) }

    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val wasGranted = hasLocationPermission
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        // If permission was just granted, set loading state
        if (!wasGranted && hasLocationPermission) {
            isLocationLoading = true
        }

        // For first-time users, don't show bottom sheets immediately after denying
        // Bottom sheets will only show when they reopen the app
        // (No bottom sheet logic here for first-time users)
    }

    // Fetch location when permission is granted and loading is true
    LaunchedEffect(isLocationLoading) {
        if (isLocationLoading && hasLocationPermission) {
            try {
                // Use injected LocationService with VERY HIGH ACCURACY like Swiggy/Zomato
                // Use getHighAccuracyLocationData with GPS-level precision (5-10m target)
                val locationData = locationService.getHighAccuracyLocationData(
                    timeoutMs = 15000L,  // Wait up to 15 seconds for accurate location
                    minAccuracyMeters = 10f  // Target 10m GPS precision
                )
                
                if (locationData != null) {
                    // Save the fetched location with all detailed fields
                    locationPreferences.saveLocation(locationData)
                    locationPreferences.setPermissionGranted(true)
                    Timber.d("📍 High accuracy location saved: ${locationData.getShortAddress()}")
                    Timber.d("📍   Full: ${locationData.getFullAddress()}")
                    Timber.d("📍   Coords: lat=${locationData.latitude}, lon=${locationData.longitude}")
                    Timber.d("📍   Accuracy: ${locationData.accuracy}m")
                    
                    // Immediately update ViewModel with new location for distance calculation
                    if (locationData.latitude != 0.0 || locationData.longitude != 0.0) {
                        jobViewModel.setUserLocation(locationData.latitude, locationData.longitude)
                    }
                } else {
                    // Fallback - try getCurrentLocation
                    val locationInfo = locationService.getCurrentLocation()
                    if (locationInfo != null) {
                        val fallbackData = locationService.toLocationData(locationInfo)
                        locationPreferences.saveLocation(fallbackData)
                        Timber.d("📍 Fallback location saved: lat=${locationInfo.latitude}, lon=${locationInfo.longitude}")
                        
                        // Update ViewModel with fallback location
                        if (locationInfo.latitude != 0.0 || locationInfo.longitude != 0.0) {
                            jobViewModel.setUserLocation(locationInfo.latitude, locationInfo.longitude)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error - maybe show a toast
                Timber.e(e, "Failed to fetch location")
            } finally {
                isLocationLoading = false
            }
        }
    }

    // Profile completion variables - use ViewModel state instead of duplicate checks
    val vmCanApplyDirectly by smartApplicationViewModel.canApplyDirectly.collectAsStateWithLifecycle()
    val vmProfileCompletionPercentage by smartApplicationViewModel.profileCompletionPercentage.collectAsStateWithLifecycle()
    val vmMissingFields by smartApplicationViewModel.missingFields.collectAsStateWithLifecycle()

    // Function to apply for job directly
    val applyForJobDirectly: (String) -> Unit = { jobId ->
        // Use ViewModel's canApplyDirectly state
        Timber.d("🎯 applyForJobDirectly - jobId: $jobId, canApply: $vmCanApplyDirectly")
        if (vmCanApplyDirectly) {
            // Apply directly without navigating to application screen
            Toast.makeText(context, "Applying for job...", Toast.LENGTH_SHORT).show()
            smartApplicationViewModel.applyForJob(jobId)
        } else {
            // Profile not complete, show completion prompt
            Toast.makeText(context, "Please complete your profile first (80% required)", Toast.LENGTH_LONG).show()
            navController.navigate(Routes.PROFILE_SETUP)
        }
    }
    
    // Handle application submission result
    val applicationUiState by smartApplicationViewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(applicationUiState.applicationSuccess) {
        if (applicationUiState.applicationSuccess) {
            Toast.makeText(context, "Application submitted successfully!", Toast.LENGTH_SHORT).show()
            // Refresh applications list
            jobApplicationViewModel.loadMyApplications()
            // Reset the submission state
            smartApplicationViewModel.clearSuccessStates()
        }
    }
    
    LaunchedEffect(applicationUiState.error) {
        applicationUiState.error?.let { error ->
            // Show user-friendly error message
            val userMessage = when {
                error.contains("already applied", ignoreCase = true) -> "You have already applied to this job"
                error.contains("PROFILE_INCOMPLETE", ignoreCase = true) -> "Please complete your profile to apply"
                error.contains("Profile check failed", ignoreCase = true) -> "Unable to verify profile. Please try again."
                error.contains("not authenticated", ignoreCase = true) -> "Please sign in to apply"
                error.contains("Job not found", ignoreCase = true) -> "This job is no longer available"
                else -> error
            }
            Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
            // Clear the error after showing
            smartApplicationViewModel.clearError()
        }
    }

    // Function to save/unsave job
    fun saveJob(jobId: String) {
        savedJobsViewModel.saveJob(jobId)
    }

    fun unsaveJob(jobId: String) {
        savedJobsViewModel.unsaveJob(jobId)
    }

    // Smart application features
    // NOTE: Use top-level hasAppliedToJob(jobId, applications) function instead of local duplicate

    // PERFORMANCE FIX: Consolidated all initialization logic into single LaunchedEffect
    // This reduces recomposition triggers and prevents race conditions
    LaunchedEffect(Unit) {
        Timber.d("🏠 WorkerHomeScreen - CONSOLIDATED INIT: Starting all initialization")
        
        // 1. Load profile and notifications (ViewModels handle deduplication)
        profileViewModel.loadProfile()
        notificationViewModel.loadNotifications()
        
        // 2. Handle location persistence
        if (hasLocationPermission) {
            val savedLocation = locationPreferences.getSavedLocation()
            val hasValidLocation = savedLocation != null && 
                savedLocation.latitude != 0.0 && 
                savedLocation.longitude != 0.0 &&
                savedLocation.accuracy > 0f
            
            val isLocationRecent = locationPreferences.isLocationRecent()
            
            when {
                hasValidLocation && isLocationRecent -> {
                    Timber.d("📍 LOCATION: Using cached location (accuracy: ${savedLocation?.accuracy}m)")
                    savedLocation?.let { location ->
                        jobViewModel.setUserLocation(location.latitude, location.longitude)
                    }
                }
                hasValidLocation && !isLocationRecent -> {
                    Timber.d("📍 LOCATION: Using old cached location, refreshing in background")
                    savedLocation?.let { location ->
                        jobViewModel.setUserLocation(location.latitude, location.longitude)
                    }
                    isLocationLoading = true
                }
                else -> {
                    Timber.d("📍 LOCATION: No valid cached location, fetching fresh...")
                    isLocationLoading = true
                }
            }
        }
        
        // 3. Handle permission bottom sheets (only once per session)
        if (!bottomSheetsShownInSession) {
            bottomSheetsShownInSession = true
            if (!hasNotificationPermission) {
                Timber.d("🏠 WorkerHomeScreen - Showing notification bottom sheet")
                showNotificationBottomSheet = true
            }
        }
        
        Timber.d("🏠 WorkerHomeScreen - CONSOLIDATED INIT: Complete")
    }

    // Play Store URL constant
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
    
    // WhatsApp sharing function
    val shareToWhatsApp = {
        val packageManager = context.packageManager

        try {
            // Try to open WhatsApp directly
            val whatsappIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (whatsappIntent != null) {
                // Create sharing intent for WhatsApp
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        "Check out this amazing job app! Download DutyPe and find your dream job.\n\n" +
                                "Download link: $playStoreUrl"
                    )
                    setPackage("com.whatsapp")
                }
                context.startActivity(shareIntent)
            } else {
                // WhatsApp not installed, open in browser
                val browserIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20$playStoreUrl")
                )
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Fallback to browser
            val browserIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20$playStoreUrl")
            )
            context.startActivity(browserIntent)
        }
    }

    val tabTitles = listOf("All Jobs", "Hourly", "Daily", "Part-time / Full-time")
    val tabIcons = listOf(
        Icons.Default.Star,
        Icons.Default.AccessTime,
        Icons.Default.CalendarToday,
        Icons.Default.Work
    )

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0)
    val pullToRefreshState = rememberPullToRefreshState()

    // Status bar colors for different tabs (all using white for consistency)
    val statusBarColors = listOf(
        Color.White, // White for All Jobs
        Color.White, // White for Hourly
        Color.White, // White for Daily
        Color.White  // White for Part-time/Full-time
    )

    // Update status bar color when tab changes
    LaunchedEffect(pagerState.currentPage) {
        val color = statusBarColors.getOrNull(pagerState.currentPage) ?: Color.White
        onStatusBarColorChange(color)
    }

    // Set initial status bar color
    LaunchedEffect(Unit) {
        onStatusBarColorChange(statusBarColors[0])
    }


    // PERFORMANCE FIX: Load vacancy statuses in BATCH instead of N+1 pattern
    // Before: 50 jobs = 50 API calls (N+1 pattern)
    // After: 50 jobs = 1 batched API call (chunks of 10)
    val loadedVacancyJobIds = remember { mutableSetOf<String>() }
    
    LaunchedEffect(jobUiState.jobs) {
        // Only load vacancy status for jobs we haven't loaded yet
        val newJobs = jobUiState.jobs.filter { it.jobId !in loadedVacancyJobIds }
        if (newJobs.isNotEmpty()) {
            val newJobIds = newJobs.map { it.jobId }
            newJobIds.forEach { loadedVacancyJobIds.add(it) }
            
            try {
                // BATCH CALL: Single API call for all jobs instead of N calls
                Timber.d("WorkerHomeScreen - Loading vacancy status for ${newJobIds.size} jobs in BATCH")
                jobApplicationService.getJobVacancyStatusBatch(newJobIds).onSuccess { statusMap ->
                    Timber.d("WorkerHomeScreen - Batch loaded ${statusMap.size} vacancy statuses")
                    jobVacancyStatuses = jobVacancyStatuses + statusMap
                }.onFailure { e ->
                    Timber.w("WorkerHomeScreen - Batch vacancy status failed: ${e.message}")
                }
            } catch (e: Exception) {
                // Silently handle cancellation - don't log as error
                if (e !is kotlinx.coroutines.CancellationException) {
                    Timber.d("WorkerHomeScreen - Error loading batch vacancy status: ${e.message}")
                }
            }
        }
    }


    // Location text - show short address (area, city only)
    val locationText = remember(currentLocation) {
        when {
            currentLocation != null -> {
                // Show short address: area, city (no state, no pincode)
                val loc = currentLocation!!
                val area: String? = loc.area?.takeIf { s: String -> s.isNotBlank() }
                val city: String? = loc.city?.takeIf { s: String -> s.isNotBlank() }
                
                when {
                    area != null && city != null -> "$area, $city"
                    city != null -> city
                    area != null -> area
                    loc.address.isNotEmpty() -> {
                        // Truncate long addresses
                        val addr = loc.address
                        if (addr.length > 30) "${addr.take(27)}..." else addr
                    }
                    else -> "Select Your Location"
                }
            }

            else -> if (hasLocationPermission) {
                when {
                    isLocationLoading -> "Getting your location..."
                    else -> "Tap to get location"
                }
            } else {
                "Enable location"
            }
        }
    }

    WorkerGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .background(Color.White)
        ) {
            // Header section - Compact
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Top row with DutyPe and icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side - DutyPe text and location (no gap)
                    Column {
                        Text(
                            text = "DutyPe",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color.Black
                            )
                        )
                        // Location directly below DutyPe - no gap
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { rootNavController.navigate(Routes.MANUAL_LOCATION_ROUTE) }
                        ) {
                            Text(
                                text = locationText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF6B7280),
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isLocationLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = Color.Black
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF374151),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Right side - Chat and Map buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chat button - Messages
                        androidx.compose.material3.Surface(
                            onClick = { navController.navigate(Routes.CHAT_CONVERSATIONS) },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1F2937),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Chat",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                        
                        // Map View chip button - Jobs on Map (Accessibility Feature)
                        androidx.compose.material3.Surface(
                            onClick = { navController.navigate(Routes.WORKER_JOB_MAP) },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = com.dutype.app.R.drawable.location_view),
                                    contentDescription = null,
                                    tint = Color(0xFF1F2937),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Map",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1F2937)
                                    )
                                )
                            }
                        }
                        
                        Box {
                            IconButton(
                                onClick = { navController.navigate(Routes.WORKER_NOTIFICATIONS) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            if (notificationUiState.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Red, shape = CircleShape)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                )
                            }
                        }
                    }
                }
            }

            // Content section - Sections based home screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color.White)
            ) {
                // Simple job cards list
                PullToRefreshBox(
                    isRefreshing = jobUiState.isRefreshing,
                    onRefresh = {
                        jobViewModel.refreshJobs()
                    },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        // Always show shimmer first while loading
                        jobUiState.isLoading -> {
                            LoadingContent()
                        }

                        jobUiState.hasError -> {
                            ErrorContent(
                                error = jobUiState.error ?: "Unknown error occurred",
                                onRetry = {
                                    jobViewModel.loadJobs()
                                }
                            )
                        }

                        else -> {
                            // Memoize filtered jobs to avoid recomputation on every recomposition
                            val filteredJobs = remember(jobUiState.jobs, applications) {
                                jobUiState.jobs
                                    .filter { job ->
                                        // Exclude filled jobs (all vacancies taken)
                                        !job.isFilled
                                    }
                                    .filter { job ->
                                        // Exclude expired jobs
                                        !job.isExpired()
                                    }
                                    .filter { job ->
                                        // Exclude jobs that worker has already applied to
                                        !applications.any { app -> app.jobId == job.jobId }
                                    }
                            }
                            
                            when {
                                // No jobs at all in the system
                                jobUiState.jobs.isEmpty() -> {
                                    EmptyJobsState()
                                }
                                
                                // All jobs filtered out (user applied to all available jobs)
                                filteredJobs.isEmpty() -> {
                                    EmptyJobsState(
                                        title = "You've Applied to All Jobs!",
                                        message = "Great job! Check back soon for new opportunities."
                                    )
                                }
                                
                                // Show jobs
                                else -> {
                                    HomeSectionsContent(
                                        jobListings = filteredJobs,
                                        navController = navController,
                                        rootNavController = rootNavController,
                                        savedJobsViewModel = savedJobsViewModel,
                                        applications = applications,
                                        onApplyClick = applyForJobDirectly,
                                        hasLocationPermission = hasLocationPermission,
                                        context = context,
                                        jobVacancyStatuses = jobVacancyStatuses,
                                        scrollStateManager = scrollStateManager,
                                        onJobClick = { jobId ->
                                            clickedJobId = jobId
                                        },
                                        userName = profileUiState.user?.fullName ?: currentUser?.displayName ?: "",
                                        userEmail = profileUiState.user?.email ?: currentUser?.email ?: ""
                                    )
                                }
                            }
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
            userRole = "worker"
        )

        // Location selection removed - using empty state with settings button instead
    }
}


@Composable
private fun LoadingContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        contentPadding = PaddingValues(bottom = 100.dp), // Add extra padding for bottom bar
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(5) { // Show 5 shimmer cards (reduced to fit better)
            JobCardShimmer()
        }
    }
}

@Composable
private fun EmptyJobsState(
    title: String = "Jobs Coming Soon!",
    message: String = "We're working to bring you the best opportunities. Check back soon!"
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp), // Add bottom padding for bottom bar
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Work,
                contentDescription = "No jobs",
                tint = Color(0xFF1F2937),
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
//            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "Error",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Oops! Something went wrong",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                )

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2937)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again")
                }
            }
        }
    }
}


@Composable
private fun FooterContent() {
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

@Composable
private fun SafetyTipCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "🛡️", fontSize = 18.sp)
            Text(
                text = "DutyPe Safety Tip: Never pay money to get a job. All verified jobs are free.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF374151),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun WelcomeCarousel(
    userName: String = "",
    userEmail: String = "",
    profileImageUrl: String? = null
) {
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour < 12 -> "Good Morning"
        currentHour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    
    val greetingEmoji = when {
        currentHour < 12 -> "🌅"
        currentHour < 17 -> "☀️"
        else -> "🌙"
    }
    
    // Get display name - first name only for cleaner look
    val displayName = when {
        userName.isNotBlank() -> userName.split(" ").firstOrNull()?.let { 
            com.example.dutype.utils.ValidationUtils.capitalizeWords(it) 
        } ?: "there"
        else -> "there"
    }
    
    val carouselCards = listOf(
        // Greeting Card - Subtle gradient design
        EnhancedCarouselCard(
            type = CardType.GREETING,
            emoji = greetingEmoji,
            title = "$greeting, $displayName!",
            subtitle = "Find your perfect job today",
            gradientColors = listOf(Color(0xFF4F6AF0), Color(0xFF6B7FE8)), // Softer blue-purple
            icon = null
        ),
        // Safety Tip Card
        EnhancedCarouselCard(
            type = CardType.INFO,
            emoji = "🛡️",
            title = "DutyPe Safety Tip",
            subtitle = "Never pay money to get a job. All verified jobs are free.",
            gradientColors = listOf(Color(0xFF2563EB), Color(0xFF3B82F6)), // Softer blue
            icon = Icons.Default.CheckCircle
        ),
        // Verified Jobs Card
        EnhancedCarouselCard(
            type = CardType.INFO,
            emoji = "✅",
            title = "100% Verified Jobs",
            subtitle = "All employers are verified. Your safety is our priority.",
            gradientColors = listOf(Color(0xFF059669), Color(0xFF34D399)), // Softer green
            icon = Icons.Default.CheckCircle
        ),
        // Secure Payments Card
        EnhancedCarouselCard(
            type = CardType.INFO,
            emoji = "💰",
            title = "Secure Payments",
            subtitle = "Get paid on time. Payment protected by DutyPe.",
            gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF9F7AEA)), // Softer purple
            icon = Icons.Default.CheckCircle
        )
    )
    
    val pagerState = rememberPagerState(initialPage = 0)
    
    // Auto-scroll effect
    LaunchedEffect(pagerState) {
        while (true) {
            kotlinx.coroutines.delay(4000) // 4 seconds delay
            val nextPage = (pagerState.currentPage + 1) % carouselCards.size
            pagerState.animateScrollToPage(nextPage)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        com.google.accompanist.pager.HorizontalPager(
            count = carouselCards.size,
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            itemSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val card = carouselCards[page]
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = card.gradientColors
                            )
                        )
                ) {
                    // Decorative circles in background
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .offset(x = 280.dp, y = (-30).dp)
                            .background(
                                Color.White.copy(alpha = 0.1f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .offset(x = 300.dp, y = 60.dp)
                            .background(
                                Color.White.copy(alpha = 0.08f),
                                CircleShape
                            )
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Emoji with background circle
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = card.emoji,
                                fontSize = 26.sp
                            )
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = card.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = card.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                maxLines = 2
                            )
                        }
                        
                        // Arrow icon for non-greeting cards
                        if (card.type != CardType.GREETING) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Page indicators - Modern pill style
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(carouselCards.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(if (pagerState.currentPage == index) 20.dp else 6.dp)
                        .height(6.dp)
                        .background(
                            if (pagerState.currentPage == index) Color(0xFF1F2937) else Color(0xFFD1D5DB),
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}

// Data class for enhanced carousel cards
private data class EnhancedCarouselCard(
    val type: CardType,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
    val icon: ImageVector?
)

private enum class CardType {
    GREETING, INFO
}

@Composable
private fun GreetingCard(userName: String = "there") {
    // This is now replaced by WelcomeCarousel
}

@Composable
private fun HomeSectionsContent(
    jobListings: List<JobListing>,
    navController: NavController,
    rootNavController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    applications: List<JobApplication>,
    onApplyClick: (String) -> Unit,
    hasLocationPermission: Boolean = false,
    context: android.content.Context,
    jobVacancyStatuses: Map<String, JobVacancyStatus> = emptyMap(),
    scrollStateManager: ScrollStateManager? = null,
    onJobClick: (String) -> Unit,
    userName: String = "",
    userEmail: String = ""
) {
    // Memoize filtered jobs to avoid recomputation on every recomposition
    val availableJobs = remember(jobListings, jobVacancyStatuses) {
        jobListings.filter { job ->
            jobVacancyStatuses[job.jobId] != JobVacancyStatus.FILLED
        }
    }
    
    // Memoize nearby jobs (sorted by distance) - show only 3
    val nearbyJobs = remember(availableJobs) {
        availableJobs
            .sortedBy { it.distance ?: Double.MAX_VALUE }
            .take(3)
    }
    
    ScrollAwareLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        scrollStateManager = scrollStateManager
    ) {
        // Section 1: Recommended Jobs Near You - Pass JobListing directly (no conversion needed)
        item {
            RecommendedJobsSection(
                jobs = nearbyJobs,
                onViewAllClick = { navController.navigate(Routes.allJobsRoute("All Jobs")) },
                navController = navController,
                savedJobsViewModel = savedJobsViewModel,
                applications = applications,
                onApplyClick = onApplyClick,
                onJobClick = onJobClick
            )
        }
        
        // Section 2: Browse Categories
        item {
            BrowseCategoriesSection(
                onCategoryClick = { category ->
                    navController.navigate(Routes.allJobsRoute(category))
                },
                onViewAllClick = { navController.navigate(Routes.allJobsRoute("All Jobs")) },
                getCategoryBadge = { category ->
                    // Map display name to category enum for badge lookup
                    val categoryKey = when (category) {
                        "Delivery" -> "DELIVERY"
                        "Shop Helper" -> "SHOP_HELPER"
                        "Housekeeping" -> "HOUSEKEEPING"
                        "Construction" -> "CONSTRUCTION"
                        "Events" -> "EVENTS"
                        "Kitchen" -> "COOK"
                        "Driver" -> "DRIVER"
                        "Security" -> "SECURITY"
                        "Electrician" -> "ELECTRICIAN"
                        "Plumber" -> "PLUMBER"
                        else -> category.uppercase()
                    }
                    // Return badge from metadata (e.g., "🔥 25 jobs")
                    null // Will be populated from MetadataManager in the composable
                }
            )
        }
        
        // Section 3: DutyPe Promise Card
        item {
            DutyPePromiseCard()
        }
        
        // Footer
        item {
            FooterContent()
        }
    }
}

@Composable
private fun RecommendedJobsSection(
    jobs: List<JobListing>,
    onViewAllClick: () -> Unit,
    navController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    applications: List<JobApplication>,
    onApplyClick: (String) -> Unit,
    onJobClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recommended Jobs Near You",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    fontSize = 18.sp
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Job Cards - Show only 3, using JobListing directly
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            jobs.forEach { job ->
                val jobId = job.jobId.ifEmpty { job.id }
                JobCard(
                    job = job,
                    isSaved = job.isSaved,
                    hasApplied = hasAppliedToJob(jobId, applications),
                    onApplyClick = { onApplyClick(jobId) },
                    onSaveClick = {
                        if (job.isSaved) {
                            savedJobsViewModel.unsaveJob(jobId)
                        } else {
                            savedJobsViewModel.saveJob(jobId)
                        }
                    },
                    onCardClick = {
                        onJobClick(jobId)
                        navController.navigate(Routes.jobDetailRoute(jobId))
                    },
                    onViewTrack = { onJobClick(jobId) }
                )
            }
        }
        
        // View All Button
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onViewAllClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1F2937)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "View All Jobs",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BrowseCategoriesSection(
    onCategoryClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    getCategoryBadge: (String) -> String? = { null }
) {
    // Categories with best fit emojis
    val categories = listOf(
        CategoryItem("Delivery", "\uD83D\uDEB4", Color(0xFFFFE4E6)),
        CategoryItem("Shop Helper", "\uD83C\uDFEA", Color(0xFFDCFCE7)),
        CategoryItem("Housekeeping", "\uD83E\uDDF9", Color(0xFFFEF3C7)),
        CategoryItem("Construction", "\uD83D\uDC77", Color(0xFFFFE4E6)),
        CategoryItem("Events", "\uD83C\uDFAA", Color(0xFFE0E7FF)),
        CategoryItem("Kitchen", "\uD83C\uDF73", Color(0xFFF3E8FF)),
        CategoryItem("Driver", "\uD83D\uDE97", Color(0xFFCFFAFE)),
        CategoryItem("Security", "\uD83D\uDC82", Color(0xFFFEE2E2)),
        CategoryItem("Electrician", "\uD83D\uDCA1", Color(0xFFFEF9C3)),
        CategoryItem("Plumber", "\uD83D\uDD27", Color(0xFFDBEAFE))
    )
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Browse Categories",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    fontSize = 18.sp
                )
            )
            
            // View all button - navigates to all jobs screen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.clickable { onViewAllClick() }
            ) {
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF1F2937),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF1F2937),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Categories Grid - 5 per row
        val chunkedCategories = categories.chunked(5)
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            chunkedCategories.forEach { rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowCategories.forEach { category ->
                        CategoryChip(
                            category = category,
                            onClick = { onCategoryClick(category.name) }
                        )
                    }
                    // Fill empty spaces if row has less than 5 items
                    repeat(5 - rowCategories.size) {
                        Spacer(modifier = Modifier.width(60.dp))
                    }
                }
            }
        }
    }
}

private data class CategoryItem(
    val name: String,
    val emoji: String,
    val backgroundColor: Color
)

@Composable
private fun CategoryChip(
    category: CategoryItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(64.dp)
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(category.backgroundColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.emoji,
                fontSize = 24.sp
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Category name
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF374151),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DutyPePromiseCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6FA)), // Very light sky blue
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with shield icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Shield icon - sky blue background
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFB1DAEE), RoundedCornerShape(8.dp)), // Sky blue
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🛡️",
                        fontSize = 18.sp
                    )
                }
                
                Text(
                    text = "DutyPe Promise",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bullet points with dot
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PromiseBulletPoint(text = "100% Free - No fees to find jobs")
                PromiseBulletPoint(text = "Verified Jobs from trusted employers")
                PromiseBulletPoint(text = "Secure Payments - Get paid on time")
                PromiseBulletPoint(text = "24/7 Support for workers")
            }
        }
    }
}

@Composable
private fun PromiseBulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dot bullet
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF1F2021), CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF2B323B),
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun EmptyTabContent(
    title: String,
    message: String,
    icon: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 48.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

/**
 * Helper function to clean location text for header display
 * Removes pincode and long addresses from location display
 */
private fun cleanLocationHeaderText(locationText: String): String {
    // Remove pincode (6 digits) from the location text
    val withoutPincode = locationText.replace(Regex("\\b\\d{6}\\b"), "").trim()

    // Remove any trailing comma or hyphen
    val cleaned = withoutPincode.replace(Regex("[,-]\\s*$"), "").trim()

    // Return the cleaned location text without truncation
    // The Text component will handle overflow with ellipsis if needed
    return cleaned
}


//@Preview(showBackground = true)
//@Composable
//fun WorkerHomeScreenPreview() {
//    WorkerHomeScreen(
//        navController = NavController(LocalContext.current),
//        onStatusBarColorChange = {}
//    )
//}
