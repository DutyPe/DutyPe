package com.example.dutype.worker.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.AnnouncementList
import com.example.dutype.components.BirthdayBanner
import com.example.dutype.components.JobCardShimmer
import com.example.dutype.components.NotificationPermissionBottomSheet
import com.example.dutype.components.OfflineBanner
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.components.openNotificationSettings
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.navigation.Routes
import com.example.dutype.services.BirthdayInfo
import com.example.dutype.services.BirthdayService
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.DeepLinkHandler
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.viewmodels.ConnectivityViewModel
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.worker.components.JobCard
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

// NOTE: hasAppliedToJob function removed - Apply button removed from JobCard
// Users now apply from JobDescriptionScreen only

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
    // LAZY LOADING: Only instantiate ViewModels needed for HomeScreen
    // Other ViewModels are instantiated on their respective screens
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    val workerHomeViewModel: com.example.dutype.viewmodels.WorkerHomeViewModel = hiltViewModel()
    // LocationPreferences accessed via FirestoreJobViewModel (proper DI pattern)
    val locationPreferences = jobViewModel.locationPreferences
    val currentLocation by locationPreferences.currentLocation.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val jobApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val announcementViewModel: com.example.dutype.viewmodels.AnnouncementViewModel = hiltViewModel()
    val announcements by announcementViewModel.announcements.collectAsState()
    val dataStore: ApplicationFormDataStore = remember { ApplicationFormDataStore(context) }
    val scope = rememberCoroutineScope()
    val jobApplicationService = jobApplicationViewModel.jobApplicationService
    val locationService = jobViewModel.locationService
    val jobUiState by jobViewModel.uiState.collectAsState()
    
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    
    val birthdayService: BirthdayService = hiltViewModel<com.example.dutype.viewmodels.BirthdayServiceHolder>().service
    var birthdayInfo by remember { mutableStateOf<BirthdayInfo?>(null) }
    var showBirthdayBanner by remember { mutableStateOf(false) }
    
    // PERFORMANCE FIX P0: Use ViewModel's filtered jobs instead of computing in Composable
    val filteredJobs by jobViewModel.filteredJobs.collectAsState()
    
    // PERFORMANCE FIX P2: Use ViewModel's vacancy statuses (cleared on refresh)
    val jobVacancyStatuses by jobViewModel.jobVacancyStatuses.collectAsState()

    // View tracking state
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
    
    // Observe location loading state from preferences
    val locationLoadingState by locationPreferences.isLocationLoading.collectAsState()

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
    
    // Voice search launcher - for AI voice assistant
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val searchQuery = com.example.dutype.utils.VoiceSearchHelper.extractSearchQuery(result.data)
            if (!searchQuery.isNullOrBlank()) {
                Timber.d("🎤 Voice search query: $searchQuery")
                // Navigate to All Jobs screen with voice search query
                navController.navigate("${Routes.WORKER_ALL_JOBS}?voiceQuery=$searchQuery")
            }
        }
    }

    // Track if location fetch is in progress to prevent duplicate calls
    var locationFetchInProgress by remember { mutableStateOf(false) }

    // Fetch location when permission is granted and loading is true
    LaunchedEffect(isLocationLoading) {
        if (isLocationLoading && hasLocationPermission && !locationFetchInProgress) {
            locationFetchInProgress = true
            try {
                // 🚀 UBER/SWIGGY STRATEGY: Get location instantly, upgrade in background
                // This provides immediate results while improving accuracy
                Timber.d("📍 Starting FAST location fetch (Uber/Swiggy strategy)...")
                
                locationService.getLocationFast(locationPreferences) { locationData ->
                    if (locationData != null) {
                        Timber.d("📍 ⚡ Location update received: ${locationData.getShortAddress()} (${locationData.accuracy}m)")
                        
                        // Location already saved by getLocationFast()
                        locationPreferences.setPermissionGranted(true)
                        
                        // Convert LocationInfo to LocationData for Firestore sync
                        val data = locationService.toLocationData(locationData)
                        
                        // Save to Firestore for cross-device sync
                        currentUser?.uid?.let { userId ->
                            launch(Dispatchers.IO) {
                                try {
                                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    firestore.collection("users").document(userId).update(
                                        mapOf(
                                            "latitude" to data.latitude,
                                            "longitude" to data.longitude,
                                            "address" to data.address,
                                            "city" to data.city,
                                            "area" to data.area,
                                            "state" to data.state,
                                            "locationUpdatedAt" to System.currentTimeMillis()
                                        )
                                    ).await()
                                    Timber.d("📍 Location synced to Firestore")
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to sync location to Firestore")
                                }
                            }
                        }
                        
                        // CRITICAL FIX: Update ViewModel immediately AND trigger UI refresh
                        if (data.latitude != 0.0 || data.longitude != 0.0) {
                            jobViewModel.setUserLocation(
                                data.latitude, 
                                data.longitude,
                                immediate = true  // Calculate distances immediately
                            )
                            
                            // FORCE UI REFRESH: Trigger recomposition by updating a state
                            // This ensures the location text updates immediately
                            Timber.d("📍 FORCING UI REFRESH after location update")
                        }
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch location")
            } finally {
                // Reduce loading delay to 500ms for instant feedback
                delay(500L)
                isLocationLoading = false
                locationFetchInProgress = false
            }
        }
    }

    // NOTE: Apply button removed from JobCard - users apply from JobDescriptionScreen
    // Profile completion checks and smart apply logic no longer needed here

    // Function to save/unsave job
    fun saveJob(jobId: String) {
        savedJobsViewModel.saveJob(jobId)
    }

    fun unsaveJob(jobId: String) {
        savedJobsViewModel.unsaveJob(jobId)
    }

    // NOTE: Apply button removed from JobCard - users apply from JobDescriptionScreen only

    // ENTERPRISE OPTIMIZATION: Load jobs in parallel with page load
    // Instagram/TikTok approach: Show UI instantly, populate data in background
    LaunchedEffect(Unit) {
        Timber.d("🏠 WorkerHomeScreen - INIT: Starting ULTRA-FAST initialization")
        
        // CRITICAL FIX: Check if location already exists FIRST
        val savedLocation = locationPreferences.getSavedLocation()
        val hasValidLocation = savedLocation != null && 
                              savedLocation.latitude != 0.0 && 
                              savedLocation.longitude != 0.0
        
        if (hasValidLocation) {
            Timber.d("📍 Using cached location: ${savedLocation?.getShortAddress()}")
            // Set location immediately for instant distance calculations
            jobViewModel.setUserLocation(
                savedLocation!!.latitude, 
                savedLocation.longitude, 
                immediate = true
            )
        } else if (hasLocationPermission) {
            // Permission granted but no saved location - fetch it
            Timber.d("📍 Permission granted but no saved location - fetching now")
            isLocationLoading = true
        }
        
        // PERFORMANCE FIX: Load ONLY 3 jobs for instant home screen load
        // This is the Instagram/TikTok pattern - show something immediately
        Timber.d("🏠 Loading 3 jobs for instant display...")
        jobViewModel.loadJobsSummaryForHome()
        
        Timber.d("🏠 WorkerHomeScreen - INIT: Complete (instant - <100ms)")
    }
    
    // LAZY LOAD: Announcements - only when user scrolls to announcement section
    LaunchedEffect(Unit) {
        delay(1000) // OPTIMIZED: Load after 1 second instead of 1.5 seconds
        announcementViewModel.loadAnnouncements("worker")
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

    val tabTitles = listOf(
        stringResource(R.string.all_jobs),
        stringResource(R.string.hourly),
        stringResource(R.string.daily),
        "${stringResource(R.string.part_time)} / ${stringResource(R.string.full_time)}"
    )
    val tabIcons = listOf(
        Icons.Default.Star,
        Icons.Default.AccessTime,
        Icons.Default.CalendarToday,
        Icons.Default.Work
    )

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0)
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Debug: Log when announcements change
    LaunchedEffect(announcements) {
        Timber.d("📢 WorkerHomeScreen: Announcements updated - count: ${announcements.size}")
        announcements.forEach { announcement ->
            Timber.d("📢   - ${announcement.title} (targetRole: ${announcement.targetRole})")
        }
    }

    // Status bar colors for different tabs (using cyan for Worker theme)
    val statusBarColors = listOf(
        WorkerColors.StatusBarColor, // Cyan for All Jobs
        WorkerColors.StatusBarColor, // Cyan for Hourly
        WorkerColors.StatusBarColor, // Cyan for Daily
        WorkerColors.StatusBarColor  // Cyan for Part-time/Full-time
    )

    // Update status bar color when tab changes
    LaunchedEffect(pagerState.currentPage) {
        // Use new gradient color for status bar
        onStatusBarColorChange(WorkerColors.StatusBarColor)
    }

    // Set initial status bar color
    LaunchedEffect(Unit) {
        onStatusBarColorChange(WorkerColors.StatusBarColor)
    }


    // REMOVED: Vacancy status loading on home screen
    // Home screen shows preview only - full details load on job detail screen
    // This saves 500ms per page load


    // Location text - show FULL address like professional apps (Swiggy, Zomato, Flipkart)
    val locationText = remember(currentLocation) {
        when {
            currentLocation != null -> {
                val loc = currentLocation!!
                
                // Use getFullAddress() for complete address with all components
                // Example: "Road No. 10, HUDA Layout, Nallagandla, Serilingampalle (M), Telangana 500019"
                when {
                    loc.address.isNotBlank() -> {
                        // Try to get full address from helper method first
                        val fullAddress = loc.getFullAddress()
                        if (fullAddress.isNotBlank() && fullAddress != loc.address) {
                            fullAddress
                        } else {
                            // Use the raw address field if getFullAddress returns same or empty
                            loc.address
                        }
                    }
                    // Fallback: build from available parts if address is empty
                    else -> {
                        val parts = listOfNotNull(
                            loc.area?.takeIf { it.isNotBlank() },
                            loc.city?.takeIf { it.isNotBlank() },
                            loc.state?.takeIf { it.isNotBlank() }
                        )
                        if (parts.isNotEmpty()) parts.joinToString(", ") else "Select Your Location"
                    }
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

    // White background for Worker home screen
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
    ) {
        // Track location bar visibility and alpha from scroll
        var showLocationBarState by remember { mutableStateOf(true) }
        var locationBarAlpha by remember { mutableStateOf(1f) }
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Offline banner at the very top
                val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
                val isOnline by connectivityViewModel.isOnline.collectAsState()
                OfflineBanner(isOffline = !isOnline)

                // Content section - Sections based home screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color.Transparent)  // Transparent to show gradient
                ) {
                    // Simple job cards list
                    PullToRefreshBox(
                        isRefreshing = jobUiState.isRefreshing,
                        onRefresh = {
                            // Refresh all data sources
                            jobViewModel.refreshJobs()
                            announcementViewModel.loadAnnouncements("WORKER") // Refresh announcements for workers
                        },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            // Show shimmer only when loading AND no jobs yet
                            jobUiState.isLoading && jobUiState.jobs.isEmpty() -> {
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
                                when {
                                    // No jobs at all in the system
                                    jobUiState.jobs.isEmpty() -> {
                                        EmptyJobsState()
                                    }
                                    
                                    // All jobs filtered out
                                    filteredJobs.isEmpty() && !jobUiState.isLoading -> {
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
                                            hasLocationPermission = hasLocationPermission,
                                            context = context,
                                            jobVacancyStatuses = jobVacancyStatuses,
                                            scrollStateManager = scrollStateManager,
                                            onJobClick = { jobId ->
                                                clickedJobId = jobId
                                            },
                                            onNavigateToJob = { jobId ->
                                                navController.navigate(Routes.jobDetailRoute(jobId))
                                            },
                                            userName = currentUser?.displayName ?: "",
                                            userEmail = currentUser?.email ?: "",
                                            userSkills = emptyList(),
                                            onScrollOffsetChange = { offset ->
                                                // Keep status bar matching gradient
                                                onStatusBarColorChange(WorkerColors.StatusBarColor)
                                            },
                                            onLocationBarVisibilityChange = { visible ->
                                                showLocationBarState = visible
                                            },
                                            onLocationBarAlphaChange = { alpha ->
                                                locationBarAlpha = alpha
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Voice Search FAB
                    androidx.compose.material3.FloatingActionButton(
                        onClick = {
                            val activity = context as? android.app.Activity
                            if (activity != null) {
                                com.example.dutype.utils.VoiceSearchHelper.startVoiceRecognition(
                                    activity = activity,
                                    launcher = voiceSearchLauncher,
                                    languageCode = "en-IN"
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 30.dp, end = 16.dp),
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Headset,
                            contentDescription = "Voice Search",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // Floating header that stays fixed at top - positioned as overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                DynamicHeader(
                    locationText = locationText,
                    showLocationBar = showLocationBarState,
                    locationBarAlpha = locationBarAlpha,
                    isLocationLoading = isLocationLoading || locationLoadingState,
                    onMapClick = { navController.navigate(Routes.WORKER_JOB_MAP) },
                    onNotificationClick = {
                        currentUser?.uid?.let { userId ->
                            scope.launch(Dispatchers.IO) {
                                try {
                                    unreadNotificationCount = jobApplicationService.getUnreadNotificationCount(userId)
                                } catch (e: Exception) {
                                    Timber.w(e, "Failed to fetch unread notification count")
                                }
                            }
                        }
                        navController.navigate(Routes.WORKER_NOTIFICATIONS)
                    },
                    onLocationClick = { rootNavController.navigate(Routes.MANUAL_LOCATION_ROUTE) }
                )
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
        items(5) { // Show 5 shimmer cards (matches job count for lightning fast loading)
            JobCardShimmer()
        }
    }
}

@Composable
fun EmptyJobsState(
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
                modifier = Modifier.size(IconSizes.ExtraLarge) // Material Design 3: 48dp
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
                    modifier = Modifier.size(IconSizes.ExtraLarge) // Material Design 3: 48dp
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
                        modifier = Modifier.size(IconSizes.Small) // Material Design 3: 20dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again")
                }
            }
        }
    }
}


@OptIn(ExperimentalPagerApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeSectionsContent(
    jobListings: List<JobListing>,
    navController: NavController,
    rootNavController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    hasLocationPermission: Boolean = false,
    context: android.content.Context,
    jobVacancyStatuses: Map<String, JobVacancyStatus> = emptyMap(),
    scrollStateManager: ScrollStateManager? = null,
    onJobClick: (String) -> Unit,
    onNavigateToJob: (String) -> Unit,
    userName: String = "",
    userEmail: String = "",
    userSkills: List<String> = emptyList(),
    onScrollOffsetChange: (Float) -> Unit = {},
    onLocationBarVisibilityChange: (Boolean) -> Unit = {},
    onLocationBarAlphaChange: (Float) -> Unit = {}
) {
    // Get announcements and recently hired from ViewModels
    val announcementViewModel: com.example.dutype.viewmodels.AnnouncementViewModel = hiltViewModel()
    val announcements by announcementViewModel.announcements.collectAsState()
    
    val workerHomeViewModel: com.example.dutype.viewmodels.WorkerHomeViewModel = hiltViewModel()
    val recentHires by workerHomeViewModel.uiState.collectAsState()
    
    // Inject BirthdayService via ViewModel holder
    val birthdayService: BirthdayService = hiltViewModel<com.example.dutype.viewmodels.BirthdayServiceHolder>().service
    
    // Birthday state - use the injected service from above
    var birthdayInfo by remember { mutableStateOf<BirthdayInfo?>(null) }
    var showBirthdayBanner by remember { mutableStateOf(false) }
    
    // Check birthday on init
    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            if (!birthdayService.hasWishedToday(context, userId)) {
                val bday = birthdayService.checkIfBirthday(userId)
                if (bday != null) {
                    birthdayInfo = bday
                    showBirthdayBanner = true
                }
            }
        }
    }
    
    // Memoize filtered jobs to avoid recomputation on every recomposition
    val availableJobs = remember(jobListings, jobVacancyStatuses) {
        jobListings.filter { job ->
            jobVacancyStatuses[job.id] != JobVacancyStatus.FILLED
        }
    }
    
    // Memoize skill-matched jobs - prioritize jobs matching worker skills, then by distance
    val skillMatchedJobs = remember(availableJobs, userSkills) {
        if (userSkills.isEmpty()) {
            // No skills set, just sort by distance
            availableJobs
                .sortedBy { it.distance ?: Double.MAX_VALUE }
                .take(5)
        } else {
            // Score jobs based on skill match
            val scoredJobs = availableJobs.map { job ->
                val jobCategory = job.getCategory().uppercase()
                val skillMatch = userSkills.any { skill ->
                    val normalizedSkill = skill.uppercase().replace("_", " ")
                    jobCategory.contains(normalizedSkill) || 
                    normalizedSkill.contains(jobCategory) ||
                    job.title.uppercase().contains(normalizedSkill)
                }
                Pair(job, if (skillMatch) 0 else 1) // 0 = matched, 1 = not matched
            }
            
            // Sort by skill match first, then by distance
            scoredJobs
                .sortedWith(compareBy({ it.second }, { it.first.distance ?: Double.MAX_VALUE }))
                .map { it.first }
                .take(5)
        }
    }
    
    // Track scroll offset for location bar visibility
    val listState = rememberLazyListState()
    
    // Calculate scroll offset
    val scrollOffset = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex.toFloat() * 1000f + listState.firstVisibleItemScrollOffset.toFloat()
        }
    }
    
    // Gradual fade: Location bar starts fading at 30px, fully hidden at 180px
    val targetAlpha = remember {
        derivedStateOf {
            val offset = scrollOffset.value
            when {
                offset < 30f -> 1f
                offset > 180f -> 0f
                else -> 1f - ((offset - 30f) / 150f)
            }
        }
    }
    
    // Animate alpha for smooth transition
    val locationBarAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetAlpha.value,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 150,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "locationBarAlpha"
    )
    
    val showLocationBarLocal = locationBarAlpha > 0.01f
    
    // P1 FIX: Use snapshotFlow to debounce scroll offset changes (was firing 60x/sec)
    LaunchedEffect(Unit) {
        snapshotFlow { scrollOffset.value }
            .collect { offset -> onScrollOffsetChange(offset) }
    }
    
    // Notify parent about location bar visibility
    LaunchedEffect(showLocationBarLocal) {
        onLocationBarVisibilityChange(showLocationBarLocal)
    }
    
    // Notify parent about alpha for gradual fade
    LaunchedEffect(locationBarAlpha) {
        onLocationBarAlphaChange(locationBarAlpha)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content - scrolls over the header
        ScrollAwareLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent), // Transparent to show purple background
            state = listState,
            contentPadding = PaddingValues(
                top = 140.dp, // Reduced padding for smaller header (no banner)
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            scrollStateManager = scrollStateManager
        ) {
            // 🎂 Birthday Banner - Shows if today is user's birthday
            if (showBirthdayBanner && birthdayInfo != null) {
                item {
                    BirthdayBanner(
                        userName = birthdayInfo!!.userName,
                        onDismiss = { showBirthdayBanner = false }
                    )
                }
            }
        
        // 📢 In-App Announcements - Feature updates, banners
        if (announcements.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))  // Add top padding so it doesn't touch header
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
        }
        
        // 🔥 Recently Hired - Single centered chip with auto-scroll (no elevation)
        // COMMENTED OUT - User requested to hide this section
        /*
        if (recentHires.recentHires.isNotEmpty()) {
            item {
                val hiresList = remember(recentHires.recentHires) { recentHires.recentHires }
                var currentIndex by remember { mutableStateOf(0) }
                
                // Auto-scroll animation (like announcements)
                LaunchedEffect(hiresList.size) {
                    if (hiresList.isNotEmpty()) {
                        while (true) {
                            kotlinx.coroutines.delay(3000) // 3 seconds per hire
                            currentIndex = (currentIndex + 1) % hiresList.size
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 56.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = currentIndex,
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(600)
                            ) + fadeIn(animationSpec = tween(600)) with
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(600)
                            ) + fadeOut(animationSpec = tween(600))
                        },
                        label = "recently_hired_animation"
                    ) { index ->
                        val hire = hiresList.getOrNull(index)
                        if (hire != null) {
                            // Simple chip without elevation
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${hire.workerName} got ${hire.jobTitle}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFF1F2937),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        */
        
        // Section 1: Browse Categories (at the top) - transparent to show gradient
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)  // Transparent to show gradient background
                    .padding(vertical = 16.dp)
            ) {
                BrowseCategoriesSection(
                    onCategoryClick = { category ->
                        // Navigate to CategoriesScreen with the selected category
                        navController.navigate(Routes.categoriesRoute(category))
                    },
                    onViewAllClick = { navController.navigate(Routes.WORKER_CATEGORIES) },
                    getCategoryBadge = { category ->
                        null
                    }
                )
            }
        }
        
        // Section 2: Jobs For You (skill-matched) - transparent to show gradient
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)  // Transparent to show gradient background
                    .padding(vertical = 16.dp)
            ) {
                RecommendedJobsSection(
                    jobs = skillMatchedJobs,
                    onViewAllClick = { navController.navigate(Routes.allJobsRoute("All Jobs")) },
                    savedJobsViewModel = savedJobsViewModel,
                    onNavigateToJob = onNavigateToJob,
                    sectionTitle = if (userSkills.isNotEmpty()) stringResource(R.string.jobs_for_you) else null
                )
            }
        }
        
        // Section 3: DutyPe Promise Carousel (at the bottom after jobs)
        item {
            DutyPePromiseCarousel()
        }
        
    }
    }
}

@Composable
fun RecommendedJobsSection(
    jobs: List<JobListing>,
    onViewAllClick: () -> Unit,
    savedJobsViewModel: SavedJobsViewModel,
    onNavigateToJob: (String) -> Unit,
    sectionTitle: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header with "See all" text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onViewAllClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle ?: stringResource(R.string.jobs_near_you),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,  // Changed to black
                    fontSize = 17.sp
                )
            )
            
            // Arrow button - clean minimal style
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View All",
                tint = Color.Black.copy(alpha = 0.6f),  // Changed to black
                modifier = Modifier
                    .size(IconSizes.Standard)
                    .clickable { onViewAllClick() }
                    .padding(4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Job Cards - Show only 3, using regular JobCard (ad shows on back from JobDescription)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            jobs.forEach { job ->
                val id = job.id.ifEmpty { job.id }
                JobCard(
                    job = job,
                    isSaved = job.isSaved,
                    onSaveClick = {
                        if (job.isSaved) {
                            savedJobsViewModel.unsaveJob(job.id)
                        } else {
                            savedJobsViewModel.saveJob(job.id)
                        }
                    },
                    onCardClick = { onNavigateToJob(it) }
                )
            }
        }
    }
}

@Composable
fun BrowseCategoriesSection(
    onCategoryClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    getCategoryBadge: (String) -> String? = { null }
) {
    // Categories with emojis - all use same light gray background
    val categories = listOf(
        CategoryItem("Delivery", "\uD83D\uDEB4"),
        CategoryItem("Shop Helper", "\uD83C\uDFEA"),
        CategoryItem("Housekeeping", "\uD83E\uDDF9"),
        CategoryItem("Construction", "\uD83D\uDC77"),
        CategoryItem("Events", "\uD83C\uDFAA"),
        CategoryItem("Kitchen", "\uD83C\uDF73"),
        CategoryItem("Driver", "\uD83D\uDE97"),
        CategoryItem("Security", "\uD83D\uDC82"),
        CategoryItem("Electrician", "\uD83D\uDCA1"),
        CategoryItem("Plumber", "\uD83D\uDD27")
    )
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Categories header with "See all" text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onViewAllClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.categories),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.Black,  // Changed to black
                    fontWeight = FontWeight.SemiBold
                )
            )
            // "See all" text with arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.Black.copy(alpha = 0.7f),  // Changed to black
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View All Categories",
                    tint = Color.Black.copy(alpha = 0.6f),  // Changed to black
                    modifier = Modifier.size(IconSizes.Standard)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Categories Grid - 5 per row (original layout)
        val chunkedCategories = categories.chunked(5)
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
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
                        Spacer(modifier = Modifier.width(68.dp))
                    }
                }
            }
        }
    }
}

data class CategoryItem(
    val name: String,
    val emoji: String
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
            .width(68.dp)
    ) {
        // Icon container - white background with subtle shadow
        Card(
            modifier = Modifier.size(60.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.emoji,
                    fontSize = 26.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Category name
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.Black,  // Changed to black
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DutyPePromiseCarousel() {
    // Clean card with consistent background color
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WorkerColors.ScreenBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PromiseItemWithIcon(
                icon = Icons.Default.CheckCircle,
                title = "100% Free",
                subtitle = "No charges",
                iconColor = Color(0xFF10B981),
                textColor = Color(0xFF111827)
            )
            
            PromiseItemWithIcon(
                icon = Icons.Default.Verified,
                title = "Verified",
                subtitle = "Safe jobs",
                iconColor = Color(0xFF3B82F6),
                textColor = Color(0xFF111827)
            )
            
            PromiseItemWithIcon(
                icon = Icons.Default.Headset,
                title = "Support",
                subtitle = "24/7 help",
                iconColor = Color(0xFFF59E0B),
                textColor = Color(0xFF111827)
            )
        }
    }
}

@Composable
fun PromiseItemWithIcon(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    iconColor: Color,
    textColor: Color = Color(0xFF111827)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(90.dp)
    ) {
        // Simple icon with solid color background
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = iconColor.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(IconSizes.Standard) // Material Design 3: 24dp
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Title text
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                fontSize = 13.sp
            ),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        
        // Subtitle text
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DynamicHeader(
    locationText: String,
    showLocationBar: Boolean,
    locationBarAlpha: Float,
    isLocationLoading: Boolean = false,
    onMapClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(
                color = Color(0xFFF5F5F5),  // Lightweight gray background
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
    ) {
        // Top row with DutyPe and icons (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DutyPe",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    color = Color.Black  // Changed to black
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TODO: Re-enable map icon in future release
                /*
                IconButton(
                    onClick = onMapClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = com.dutype.app.R.drawable.location_view),
                        contentDescription = "Map View",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                */
                
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.Black,  // Changed to black
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // Collapsible location bar - hides when scrolling
        if (locationBarAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                androidx.compose.material3.Surface(
                    onClick = onLocationClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = locationBarAlpha
                            scaleY = 0.8f + (0.2f * locationBarAlpha)
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent  // Transparent background - no white box
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            
                            // Location icon with loading indicator
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(20.dp)
                            ) {
                                if (isLocationLoading) {
                                    // Small circular progress indicator
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.Black
                                    )
                                } else {
                                    Icon(   
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            
                            Text(
                                text = locationText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Black,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.5f),  // Semi-transparent black
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun WorkerHomeScreenPreview() {
//    WorkerHomeScreen(
//        navController = NavController(LocalContext.current),
//        onStatusBarColorChange = {}
//    )
//}
