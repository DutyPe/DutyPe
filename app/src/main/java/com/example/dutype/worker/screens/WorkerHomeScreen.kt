package com.example.dutype.worker.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.R
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.location.LocationPreferences
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.worker.viewmodels.WorkerNotificationViewModel
import com.example.dutype.state.SavedJobsStateManager
import com.example.dutype.ui.theme.WorkerGradientBackground
import com.example.dutype.utils.JobCardShimmer
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.ProfileViewModel
import com.example.dutype.viewmodels.JobApplicationViewModel
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.services.JobApplicationService
import com.example.dutype.services.NotificationService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ApplicationStateManager
import com.google.firebase.auth.FirebaseAuth
import com.example.dutype.worker.components.ProfileCompletionPrompt
import com.example.dutype.worker.components.CompactProfileCompletionBanner
import com.example.dutype.worker.components.JobCard
import com.example.dutype.worker.models.JobCardModel
import com.example.dutype.worker.models.JobTag
import com.example.dutype.worker.models.LocationInfo
import com.example.dutype.worker.models.PayInfo
import com.example.dutype.worker.models.PayType
import com.example.dutype.worker.models.TagType
import com.example.dutype.worker.models.TimeInfo
import com.example.dutype.worker.models.UrgencyLevel
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.state.ProfileSetupStateManager
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.mutableIntStateOf
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.components.NotificationPermissionBottomSheet
import com.example.dutype.components.openNotificationSettings
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch

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
    notificationPermissionManager: com.example.dutype.utils.NotificationPermissionManager
) {
    val context = LocalContext.current
    val locationPreferences = remember { LocationPreferences(context) }
    val currentLocation by locationPreferences.currentLocation.collectAsState()
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val jobApplicationViewModel: JobApplicationViewModel = hiltViewModel()
    val smartApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    val dataStore: ApplicationFormDataStore = remember { ApplicationFormDataStore(context) }
    val notificationViewModel: WorkerNotificationViewModel = hiltViewModel()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val jobApplicationService: JobApplicationService = remember { 
        JobApplicationService(
            notificationService = com.example.dutype.services.NotificationService(
                context = context,
                firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            ),
            profileCompletionService = ProfileCompletionService(),
            applicationStateManager = ApplicationStateManager()
        )
    }
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    val jobUiState by jobViewModel.uiState.collectAsState()
    val profileUiState by profileViewModel.uiState.collectAsState()
    val jobApplicationUiState by jobApplicationViewModel.uiState.collectAsStateWithLifecycle()
    val applications = jobApplicationUiState.applications
    
    // Extract applied job IDs from the applications in the ViewModel
    val appliedJobIds = remember(applications) {
        applications.map { it.jobId }.toSet()
    }
    
    // View tracking state
    var jobViewCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
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
                val currentLocationAddress = com.example.dutype.location.fetchUserLocation(context)
                // Save the fetched location
                val locationData = com.example.dutype.models.LocationData(
                    address = currentLocationAddress,
                    latitude = 0.0, // You can get actual coordinates if needed
                    longitude = 0.0,
                    city = currentLocationAddress.split(",")[0].trim(),
                    state = currentLocationAddress.split(",").getOrNull(1)?.trim(),
                    country = "India",
                    postalCode = null
                )
                locationPreferences.saveLocation(locationData)
            } catch (e: Exception) {
                // Handle error - maybe show a toast
                Log.e("Location", "Failed to fetch location", e)
            } finally {
                isLocationLoading = false
            }
        }
    }

    // Profile completion variables
    var profileSetupStatus by remember { mutableStateOf<com.example.dutype.state.ProfileSetupStatus?>(null) }
    var profileCompletionPercentage by remember { mutableIntStateOf(0) }
    var missingFields by remember { mutableStateOf<List<String>>(emptyList()) }
    var canApplyDirectly by remember { mutableStateOf(false) }
    
    // Load profile completion status
    LaunchedEffect(Unit) {
        try {
            val status = profileCompletionViewModel.getProfileSetupStatus(com.example.dutype.models.UserRole.WORKER)
            profileSetupStatus = status
            profileCompletionPercentage = status.completionPercentage
            missingFields = status.missingFields
            canApplyDirectly = status.isComplete
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Function to apply for job directly
    val applyForJobDirectly: (String) -> Unit = { jobId ->
        if (canApplyDirectly) {
            // Show applying message
            android.widget.Toast.makeText(context, "Applying for job...", android.widget.Toast.LENGTH_SHORT).show()

            // Use SmartJobApplicationViewModel for direct application
            smartApplicationViewModel.applyForJob(jobId)

            // Navigate to applied jobs to show the new application
            navController.navigate(Routes.WORKER_MY_JOBS)
        } else {
            // Profile not complete, show completion prompt
            android.widget.Toast.makeText(context, "Please complete your profile first", android.widget.Toast.LENGTH_LONG).show()
            navController.navigate(Routes.PROFILE_SETUP)
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
    
    // Function to check if user has applied for a job
    fun hasAppliedToJob(jobId: String, applications: List<com.example.dutype.models.JobApplication>): Boolean {
        return applications.any { it.jobId == jobId }
    }
    
    
    // Load data and handle permissions
    LaunchedEffect(Unit) {
        jobViewModel.loadJobs()
        profileViewModel.loadProfile()
        jobApplicationViewModel.loadMyApplications()
        notificationViewModel.loadNotifications() // Load notifications to update badge
    }
    
    // Handle permissions: Ask first time, show bottom sheets if denied on reopen
    LaunchedEffect(Unit) {
        // Check if this is first time user or returning user
        val sharedPrefs = context.getSharedPreferences("permission_prefs", android.content.Context.MODE_PRIVATE)
        isFirstTimeUser = !sharedPrefs.getBoolean("permissions_asked_before", false)
        
        if (isFirstTimeUser) {
            // First time user - ask permissions normally
            permissionsRequested = true
            sharedPrefs.edit().putBoolean("permissions_asked_before", true).apply()
            
            // Request notification permission first
            if (!hasNotificationPermission) {
                notificationPermissionManager.requestNotificationPermission(
                    onResult = { isGranted ->
                        hasNotificationPermission = isGranted
                        if (isGranted) {
                            Toast.makeText(context, "Notifications enabled for job alerts", Toast.LENGTH_SHORT).show()
                        }
                        // After notification permission is handled, request location permission
                        if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onDenied = {
                        // Notification denied - request location permission next
                        if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                        // Don't show bottom sheet yet - wait for both permissions to be handled
                    }
                )
            } else if (!hasLocationPermission) {
                // Notification already granted, request location permission
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        } else {
            // Returning user - show bottom sheets only for denied permissions AND only once per session
            if (!bottomSheetsShownInSession) {
                bottomSheetsShownInSession = true
                if (!hasNotificationPermission) {
                    showNotificationBottomSheet = true
                }
                // No location bottom sheet - will show empty state instead
            }
        }
    }
    
    // Note: Using simple session tracking without lifecycle observer
    // Bottom sheets will show once per app session when user returns after denying permissions
    
    
    // Refresh jobs when screen becomes visible (for proper saved state)
    DisposableEffect(Unit) {
        // Refresh jobs when component is created
        jobViewModel.loadJobs()
        
        onDispose {
            // Cleanup if needed
        }
    }
    
    // WhatsApp sharing function
    val shareToWhatsApp = {
        val packageManager = context.packageManager
        val appPackageName = context.packageName
        
        try {
            // Try to open WhatsApp directly
            val whatsappIntent = packageManager.getLaunchIntentForPackage ("com.whatsapp")
            if (whatsappIntent != null) {
                // Create sharing intent for WhatsApp
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, 
                        "Check out this amazing job app! Download DutyPe and find your dream job.\n\n" +
                        "Download link: https://play.google.com/store/apps/details?id=$appPackageName"
                    )
                    setPackage("com.whatsapp")
                }
                context.startActivity(shareIntent)
            } else {
                // WhatsApp not installed, open in browser
                val browserIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20https://play.google.com/store/apps/details?id=$appPackageName")
                )
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Fallback to browser
            val browserIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20https://play.google.com/store/apps/details?id=$appPackageName")
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

    
    // Load view counts and vacancy statuses for jobs
    LaunchedEffect(jobUiState.jobs) {
        jobUiState.jobs.forEach { job ->
            // Track view count
            jobApplicationService.getJobViewCount(job.jobId).onSuccess { viewCount ->
                println("🔍 WorkerHomeScreen - Job ${job.jobId} view count: $viewCount")
                jobViewCounts = jobViewCounts + (job.jobId to viewCount)
            }.onFailure { error ->
                println("🔍 WorkerHomeScreen - Error loading view count for job ${job.jobId}: ${error.message}")
            }
            
            // Track vacancy status
            jobApplicationService.getJobVacancyStatus(job.jobId).onSuccess { status ->
                println("🔍 WorkerHomeScreen - Job ${job.jobId} vacancy status: $status")
                jobVacancyStatuses = jobVacancyStatuses + (job.jobId to status)
            }.onFailure { error ->
                println("🔍 WorkerHomeScreen - Error loading vacancy status for job ${job.jobId}: ${error.message}")
            }
        }
    }
    

    // Enhanced location text - showing only city name for cleaner display
    val locationText = remember(currentLocation) {
        when {
            currentLocation != null -> {
                val city = currentLocation!!.city
                val state = currentLocation!!.state

                when {
                    !city.isNullOrEmpty() -> {
                        // Show only city name, clean formatting
                        cleanLocationHeaderText(city)
                    }
                    currentLocation!!.address.isNotEmpty() -> {
                        // Extract city from address if available
                        val addressParts = currentLocation!!.address.split(",")
                        val extractedCity = if (addressParts.isNotEmpty()) {
                            addressParts[0].trim()
                        } else {
                            currentLocation!!.address
                        }
                        cleanLocationHeaderText(extractedCity)
                    }
                    else -> "Select Your Location"
                }
            }
            else -> if (hasLocationPermission) {
                // Show actual location when permission is granted
                when {
                    isLocationLoading -> "Getting your location..."
                    else -> {
                        val location = currentLocation
                        when {
                            location != null -> {
                                val addressParts = location.address.split(",")
                                val extractedCity = if (addressParts.isNotEmpty()) {
                                    addressParts[0].trim()
                                } else {
                                    location.address
                                }
                                cleanLocationHeaderText(extractedCity)
                            }
                            else -> "Getting your location..."
                        }
                    }
                }
            } else {
                "Please enable location"
            }
        }
    }

    WorkerGradientBackground {
        // Filter chips state and logic
        val filterChips = listOf(
            "Trending Gigs" to Icons.Default.Star,
            "Daily Jobs" to Icons.Default.CalendarToday,
            "Hourly Jobs" to Icons.Default.AccessTime,
            "Nearby" to Icons.Default.LocationOn,
            "Part Times" to Icons.Default.Work,
            "Full Times" to Icons.Default.CheckCircle
        )
        var selectedChip by remember { mutableStateOf("Trending Gigs") }
        
        // Filter jobs based on selected chip
        val filteredJobs = remember(selectedChip, jobUiState.jobs, appliedJobIds, jobVacancyStatuses) {
            val availableJobs = jobUiState.jobs.filter { job -> 
                !appliedJobIds.contains(job.jobId) &&
                jobVacancyStatuses[job.jobId] != JobVacancyStatus.FILLED
            }
            
            when (selectedChip) {
                "Trending Gigs" -> availableJobs // Show all for trending
                "Daily Jobs" -> availableJobs.filter { 
                    it.payType.equals("DAILY", true) || 
                    it.payType.contains("day", true) ||
                    it.jobType.equals("Daily", true)
                }
                "Hourly Jobs" -> availableJobs.filter { 
                    it.payType.equals("HOURLY", true) || 
                    it.payType.contains("hour", true) ||
                    it.jobType.equals("Hourly", true)
                }
                "Nearby" -> availableJobs.filter { job ->
                    // Filter by nearby jobs - could filter by distance if available
                    // For now, prioritize jobs with specific location information
                    job.location.isNotEmpty() && !job.location.equals("Location", true)
                }
                "Part Times" -> availableJobs.filter { 
                    it.jobType.equals("Part-time", true) || 
                    it.jobType.contains("part", true)
                }
                "Full Times" -> availableJobs.filter { 
                    it.jobType.equals("Full-time", true) || 
                    it.jobType.contains("full", true)
                }
                else -> availableJobs
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Enhanced header section with modern design and subtle animation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
//                // DutyPe title
//                Text(
//                    text = "DutyPe",
//                    style = MaterialTheme.typography.headlineMedium.copy(
//                        fontWeight = FontWeight.Bold,
//                        color = Color.Black,
//                        fontSize = 24.sp
//                    ),
//                    modifier = Modifier.padding(bottom = 8.dp)
//                )

                // Single row header - Location on left, Icons on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Location section (clickable)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // Navigate to manual location screen when location is clicked
                                rootNavController.navigate(Routes.MANUAL_LOCATION_ROUTE)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Location icon
                        Icon(
                            painter = painterResource(id = R.drawable.location_icon),
                            contentDescription = "Location",
                            tint = Color.Black,
                            modifier = Modifier.size(25.dp)
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        // Location text (clickable) with dropdown arrow (not clickable)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = locationText,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // Show loading indicator when fetching location
                            if (isLocationLoading) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown",
                                tint = Color.Black,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    // Right side - Action icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search icon
                        IconButton(
                            onClick = { 
                                // Handle search functionality - could navigate to search screen or show search dialog
                                jobViewModel.loadJobs() // For now, just reload jobs
                            },
                            modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Black,
                                modifier = Modifier.size(25.dp)
                            )
                        }


                        // Notification icon with badge
                        Box {
                            IconButton(
                                onClick = {
                                    navController.navigate(Routes.WORKER_NOTIFICATIONS)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.Black,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            
                            // Notification badge
                            if (notificationUiState.unreadCount > 0) {
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

                Spacer(modifier = Modifier.height(16.dp))

                // Enhanced filter chips with better styling and functionality
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(filterChips) { (chip, icon) ->
                        FilterChip(
                            onClick = { 
                                selectedChip = chip
                            },
                            label = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedChip == chip) Color.White else Color(0xFF374151)
                                    )
                                    Text(
                                        text = chip,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = if (selectedChip == chip) Color.White else Color(0xFF374151)
                                        )
                                    )
                                }
                            },
                            selected = selectedChip == chip,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.Black,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF8FAFC),
                                labelColor = Color(0xFF374151),
                                iconColor = if (selectedChip == chip) Color.White else Color(0xFF374151)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedChip == chip,
                                borderColor = if (selectedChip == chip) Color.Black else Color(0xFFE5E7EB),
                                selectedBorderColor = Color.Black,
                                borderWidth = 1.dp
                            )
                        )
                    }
                }
            }

            // Content section with pager
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
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
                            jobUiState.jobs.isEmpty() -> {
                                // Use the existing empty state with suitcase icon
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Work,
                                            contentDescription = "No jobs",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Text(
                                            text = "No Jobs Available",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF374151)
                                        )
                                        Text(
                                            text = "There are no job opportunities available right now. Check back later for new postings!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                            else -> {
                                VerticalJobsContent(
                                    jobListings = filteredJobs,
                                    navController = navController,
                                    savedJobsViewModel = savedJobsViewModel,
                                    applications = applications,
                                    onApplyClick = applyForJobDirectly,
                                    hasLocationPermission = hasLocationPermission,
                                    context = context,
                                    jobViewCounts = jobViewCounts,
                                    jobVacancyStatuses = jobVacancyStatuses,
                                    selectedChip = selectedChip,
                                    onJobClick = { jobId -> 
                                        clickedJobId = jobId
                                        // Track job view
                                        if (currentUser != null) {
                                            scope.launch {
                                                try {
                                                    jobApplicationService.trackJobView(
                                                        jobId = jobId,
                                                        viewerId = currentUser.uid,
                                                        viewerType = "worker"
                                                    )
                                                    println("🔍 WorkerHomeScreen - Tracked view for job: $jobId")
                                                } catch (e: Exception) {
                                                    println("❌ WorkerHomeScreen - Error tracking view: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                )
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
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(6) { // Show 6 shimmer cards
            JobCardShimmer()
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
                        containerColor = Color(0xFF6366F1)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Jobs Made with",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "💙",
                        fontSize = 23.sp
                    )
                    Text(
                        text = "in Bharat",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalJobsContent(
    jobListings: List<JobListing>,
    navController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    applications: List<JobApplication>,
    onApplyClick: (String) -> Unit,
    hasLocationPermission: Boolean = false,
    context: android.content.Context,
    jobViewCounts: Map<String, Int> = emptyMap(),
    jobVacancyStatuses: Map<String, JobVacancyStatus> = emptyMap(),
    onJobClick: (String) -> Unit
) {
    // Convert JobListing to JobCardModel
    val jobCards = remember(jobListings, jobViewCounts, jobVacancyStatuses) {
        jobListings.map { job ->
            val viewCount = jobViewCounts[job.jobId] ?: 0
            val vacancyStatus = jobVacancyStatuses[job.jobId] ?: JobVacancyStatus.OPEN
            val isFilled = vacancyStatus == JobVacancyStatus.FILLED
            
            JobCardModel(
                jobId = job.id,
                title = job.title,
                employerName = job.companyName,
                payInfo = PayInfo(
                    amount = cleanPaymentAmount(
                        (job.payAmount.ifEmpty { job.salary }).ifEmpty {
                            if (job.payRate > 0.0) job.payRate.toInt().toString() else ""
                        }
                    ),
                    type = when {
                        job.payType.equals("HOURLY", true) || job.payType.contains("hour", true) -> PayType.HOURLY
                        job.payType.equals("DAILY", true) || job.payType.contains("day", true) -> PayType.DAILY
                        job.payType.equals("MONTHLY", true) || job.payType.contains("month", true) -> PayType.MONTHLY
                        else -> PayType.DAILY
                    },
                    period = "" // computed in PayInfo.getDisplayText
                ),
                location = LocationInfo(
                    area = truncateLocationText(job.area ?: job.location),
                    city = truncateLocationText(job.city ?: job.location),
                    distance = "2.5"
                ),
                tags = listOf(
                    JobTag(
                        text = job.jobType,
                        emoji = "💼",
                        type = TagType.BENEFIT
                    ),
                    JobTag(
                        text = job.category,
                        emoji = "🏷️",
                        type = TagType.BENEFIT
                    )
                ),
                timeInfo = TimeInfo(
                    postedTime = job.postedDate,
                    urgency = if (job.isUrgent()) UrgencyLevel.URGENT else UrgencyLevel.NORMAL
                ),
                phoneNumber = job.contactNumber,
                description = job.description,
                jobType = job.jobType,
                vacancies = job.vacancies, // Set actual vacancy count from job listing
                isBookmarked = false, // TODO: Get from WorkerJobInteraction
                isSaved = job.isSaved, // Get from JobListing
                isApplied = false, // TODO: Get from WorkerJobInteraction
                viewCount = viewCount,
                isFilled = isFilled
            )
        }
    }
    
    if (jobCards.isEmpty()) {
        // Empty state - show different messages based on location permission
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                if (!hasLocationPermission) {
                    // Location permission denied - show location permission message
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location permission needed",
                        tint = Color.Black,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Location Permission Required",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                    Text(
                        text = "Please give location permission to see jobs near you!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { openLocationSettings(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Open App Settings",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Location permission granted but no jobs available
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = "No jobs",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No Jobs Available",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                    Text(
                        text = "There are no job opportunities available right now. Check back later for new postings!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    } else {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Trending Gigs Section - matching image style
        item {
            JobSection(
                title = "Trending Gigs",
                jobs = jobCards,
                navController = navController,
                savedJobsViewModel = savedJobsViewModel,
                applications = applications,
                onApplyClick = onApplyClick,
                onJobClick = onJobClick
            )
        }
        
        // Hourly Jobs Section
        item {
            JobSection(
                title = "Hourly Jobs",
                jobs = jobCards.filter { it.payInfo.type == PayType.HOURLY },
                navController = navController,
                savedJobsViewModel = savedJobsViewModel,
                applications = applications,
                onApplyClick = onApplyClick,
                onJobClick = onJobClick
            )
        }
        
        // Daily Jobs Section
        item {
            JobSection(
                title = "Daily Jobs",
                jobs = jobCards.filter { it.payInfo.type == PayType.DAILY },
                navController = navController,
                savedJobsViewModel = savedJobsViewModel,
                applications = applications,
                onApplyClick = onApplyClick,
                onJobClick = onJobClick
            )
        }
        
        // Part-time/Full-time Jobs Section
        item {
            JobSection(
                title = "Part-time & Full-time",
                jobs = jobCards.filter { 
                    it.jobType == "Part-time" || it.jobType == "Full-time" 
                },
                navController = navController,
                savedJobsViewModel = savedJobsViewModel,
                applications = applications,
                onApplyClick = onApplyClick,
                onJobClick = onJobClick
            )
            }
        }
    }
}

@Composable
private fun VerticalJobsContent(
    jobListings: List<JobListing>,
    navController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    applications: List<JobApplication>,
    onApplyClick: (String) -> Unit,
    hasLocationPermission: Boolean = false,
    context: android.content.Context,
    jobViewCounts: Map<String, Int> = emptyMap(),
    jobVacancyStatuses: Map<String, JobVacancyStatus> = emptyMap(),
    selectedChip: String,
    onJobClick: (String) -> Unit
) {
    // Convert JobListing to JobCardModel
    val jobCards = remember(jobListings, jobViewCounts, jobVacancyStatuses) {
        jobListings.map { job ->
            val viewCount = jobViewCounts[job.jobId] ?: 0
            val vacancyStatus = jobVacancyStatuses[job.jobId] ?: JobVacancyStatus.OPEN
            val isFilled = vacancyStatus == JobVacancyStatus.FILLED
            
            JobCardModel(
                jobId = job.id,
                title = job.title,
                employerName = job.companyName,
                payInfo = PayInfo(
                    amount = cleanPaymentAmount(
                        (job.payAmount.ifEmpty { job.salary }).ifEmpty {
                            if (job.payRate > 0.0) job.payRate.toInt().toString() else ""
                        }
                    ),
                    type = when {
                        job.payType.equals("HOURLY", true) || job.payType.contains("hour", true) -> PayType.HOURLY
                        job.payType.equals("DAILY", true) || job.payType.contains("day", true) -> PayType.DAILY
                        job.payType.equals("MONTHLY", true) || job.payType.contains("month", true) -> PayType.MONTHLY
                        else -> PayType.DAILY
                    },
                    period = "" // computed in PayInfo.getDisplayText
                ),
                location = LocationInfo(
                    area = truncateLocationText(job.area ?: job.location),
                    city = truncateLocationText(job.city ?: job.location),
                    distance = "2.5"
                ),
                tags = listOf(
                    JobTag(
                        text = job.jobType,
                        emoji = "💼",
                        type = TagType.BENEFIT
                    ),
                    JobTag(
                        text = job.category,
                        emoji = "🏷️",
                        type = TagType.BENEFIT
                    )
                ),
                timeInfo = TimeInfo(
                    postedTime = job.postedDate,
                    urgency = if (job.isUrgent()) UrgencyLevel.URGENT else UrgencyLevel.NORMAL
                ),
                phoneNumber = job.contactNumber,
                description = job.description,
                jobType = job.jobType,
                vacancies = job.vacancies,
                isBookmarked = false,
                isSaved = job.isSaved,
                isApplied = false,
                viewCount = viewCount,
                isFilled = isFilled
            )
        }
    }
    
    if (jobCards.isEmpty()) {
        // Empty state for filtered results
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = "No jobs",
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No Jobs Available",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                Text(
                    text = "No $selectedChip available right now. Try selecting a different filter or check back later!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Vertical scrolling job cards
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(jobCards) { job ->
                JobCard(
                    jobCard = job,
                    isSaved = job.isSaved,
                    hasApplied = hasAppliedToJob(job.jobId, applications),
                    onApplyClick = { jobId ->
                        onApplyClick(jobId)
                    },
                    onSaveClick = { jobId ->
                        if (job.isSaved) {
                            savedJobsViewModel.unsaveJob(jobId)
                        } else {
                            savedJobsViewModel.saveJob(jobId)
                        }
                    },
                    onCardClick = { jobId ->
                        println("🔍 VerticalJobsContent - Job card clicked: $jobId")
                        onJobClick(jobId)
                        navController.navigate(Routes.jobDetailRoute(jobId))
                    },
                    onViewTrack = { jobId ->
                        onJobClick(jobId)
                    }
            )
            }
        }
    }
}

@Composable
private fun JobSection(
    title: String,
    jobs: List<JobCardModel>,
    navController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    applications: List<JobApplication>,
    onApplyClick: (String) -> Unit,
    onJobClick: (String) -> Unit
) {
    // Only show section if there are jobs
    if (jobs.isNotEmpty()) {
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
                text = title,
                    style = MaterialTheme.typography.titleMedium.copy( // Reduced from titleLarge
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            )
            Text(
                text = "See all",
                    style = MaterialTheme.typography.bodySmall.copy( // Reduced from bodyMedium
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.clickable { /* Handle see all */ }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Horizontal scrolling job cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(jobs) { job ->
                JobCard(
                    jobCard = job,
                    isSaved = job.isSaved,
                    hasApplied = hasAppliedToJob(job.jobId, applications),
                            onApplyClick = { jobId ->
                                onApplyClick(jobId)
                            },
                    onSaveClick = { jobId ->
                        if (job.isSaved) {
                            savedJobsViewModel.unsaveJob(jobId)
                        } else {
                            savedJobsViewModel.saveJob(jobId)
                        }
                    },
                    onCardClick = { jobId ->
                        println("🔍 JobSection - Job card clicked: $jobId")
                        onJobClick(jobId) // Call the view tracking first
                        navController.navigate(Routes.jobDetailRoute(jobId))
                    },
                )
                }
            }
        }
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
 * Helper function to truncate location text for better display in job cards
 * Only used in WorkerHomeScreen to keep location names concise
 */
private fun truncateLocationText(locationText: String?): String {
    if (locationText.isNullOrEmpty()) {
        return "Location"
    }
    
    // If the text is already short enough, return as is
    if (locationText.length <= 20) {
        return locationText
    }
    
    // Truncate long location names and add ellipsis
    return "${locationText.take(17)}..."
}

/**
 * Helper function to clean payment amount from duplicates
 * Removes any existing payment type text (hourly, daily, monthly) from the amount
 */
private fun cleanPaymentAmount(amount: String): String {
    return amount
        .replace("/hourly", "", ignoreCase = true)
        .replace("/daily", "", ignoreCase = true)
        .replace("/monthly", "", ignoreCase = true)
        .replace("/per task", "", ignoreCase = true)
        .replace("hourly", "", ignoreCase = true)
        .replace("daily", "", ignoreCase = true)
        .replace("monthly", "", ignoreCase = true)
        .replace("per task", "", ignoreCase = true)
        .replace("per hour", "", ignoreCase = true)
        .replace("per day", "", ignoreCase = true)
        .replace("per month", "", ignoreCase = true)
        .replace("Rs.", "", ignoreCase = true)
        .replace("rs", "", ignoreCase = true)
        .replace("/", "")
        .trim()
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
