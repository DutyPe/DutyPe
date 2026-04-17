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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import com.example.dutype.components.NotificationPermissionBottomSheet
import com.example.dutype.components.OfflineBanner
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.components.WorkerHomeShimmer
import com.example.dutype.components.openNotificationSettings
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.navigation.Routes
import com.example.dutype.location.TopCityChips
import com.example.dutype.services.BirthdayInfo
import com.example.dutype.services.BirthdayService
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.DeepLinkHandler
import com.example.dutype.utils.GeoUtils
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

private val WorkerHomeHeaderTopColor = Color(0xFFFFFFFF)
private val WorkerHomeHeaderMidColor = Color(0xFFF8FAFC)
private val WorkerHomeHeaderBottomColor = Color(0xFFEEF6FF)

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
    // LocationPreferences accessed via FirestoreJobViewModel (proper DI pattern)
    val locationPreferences = jobViewModel.locationPreferences
    val currentLocation by locationPreferences.currentLocation.collectAsStateWithLifecycle()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val jobApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val announcementViewModel: com.example.dutype.viewmodels.AnnouncementViewModel = hiltViewModel()
    val announcements by announcementViewModel.announcements.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val jobApplicationService = jobApplicationViewModel.jobApplicationService
    val locationService = jobViewModel.locationService
    val jobUiState by jobViewModel.uiState.collectAsStateWithLifecycle()
    
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    
    val birthdayService: BirthdayService = hiltViewModel<com.example.dutype.viewmodels.BirthdayServiceHolder>().service
    var birthdayInfo by remember { mutableStateOf<BirthdayInfo?>(null) }
    var showBirthdayBanner by remember { mutableStateOf(false) }
    
    // PERFORMANCE FIX P0: Use ViewModel's filtered jobs instead of computing in Composable
    val filteredJobs by jobViewModel.filteredJobs.collectAsStateWithLifecycle()
    
    // PERFORMANCE FIX P2: Use ViewModel's vacancy statuses (cleared on refresh)
    val jobVacancyStatuses by jobViewModel.jobVacancyStatuses.collectAsStateWithLifecycle()

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
    val locationLoadingState by locationPreferences.isLocationLoading.collectAsStateWithLifecycle()

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
                val freshCachedLocation = locationPreferences.getSavedLocationIfFresh(5 * 60 * 1000L)
                if (freshCachedLocation != null) {
                    Timber.d("📍 Using fresh cached location, skipping new GPS fetch")
                    jobViewModel.setUserLocation(
                        freshCachedLocation.latitude,
                        freshCachedLocation.longitude,
                        immediate = true
                    )
                    isLocationLoading = false
                    return@LaunchedEffect
                }

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
                                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.USERS).document(userId).update(
                                        mapOf(
                                            "location" to mapOf(
                                                "lat" to data.latitude,
                                                "lng" to data.longitude
                                            ),
                                            "geohash" to GeoUtils.encodeGeohash(data.latitude, data.longitude),
                                            "lastActiveAt" to com.google.firebase.Timestamp.now()
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
        
        // CRITICAL: Refresh StateFlow from SharedPreferences in case location was saved
        // while this screen wasn't composed (e.g., saved from SelectRoleScreen async GPS)
        locationPreferences.refreshLocation()
        
        // CRITICAL FIX: Check if location already exists FIRST
        val savedLocation = locationPreferences.getSavedLocationIfFresh()
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

            if (hasLocationPermission && !jobViewModel.locationFetchedInSession && !locationPreferences.isManualLocationLocked()) {
                Timber.d("📍 Refreshing location in background for WorkerHomeScreen")
                jobViewModel.locationFetchedInSession = true
                isLocationLoading = true
            }
        } else if (hasLocationPermission && !jobViewModel.locationFetchedInSession && !locationPreferences.isManualLocationLocked()) {
            // Permission granted but no saved location - fetch it
            Timber.d("📍 Permission granted but no saved location - fetching now")
            jobViewModel.locationFetchedInSession = true
            isLocationLoading = true
        }
        
        // PERFORMANCE FIX: Load ONLY 3 jobs for instant home screen load
        // This is the Instagram/TikTok pattern - show something immediately
        Timber.d("🏠 Loading 3 jobs for instant display...")
        jobViewModel.loadJobsSummaryForHome()
        
        Timber.d("🏠 WorkerHomeScreen - INIT: Complete (instant - <100ms)")
    }
    
    // CRITICAL: React to location changes from async GPS callbacks
    // This ensures the UI updates immediately when location is fetched
    // (e.g., from SelectRoleScreen's async GPS or WorkerHomeScreen's own fetch)
    LaunchedEffect(currentLocation) {
        val loc = currentLocation
        if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
            Timber.d("📍 Location StateFlow updated: ${loc.getShortAddress()} - updating ViewModel")
            jobViewModel.setUserLocation(loc.latitude, loc.longitude, immediate = true)
        }
    }
    
    // LAZY LOAD: Announcements - only when user scrolls to announcement section
    LaunchedEffect(Unit) {
        delay(2500)
        announcementViewModel.loadAnnouncements("worker")
    }

    // Fetch unread notification count after initial load
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { userId ->
            delay(3000)
            try {
                unreadNotificationCount = jobApplicationService.getUnreadNotificationCount(
                    userId = userId,
                    activeRole = "WORKER"
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch unread notification count")
            }
        }
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

    // Update status bar color when tab changes
    LaunchedEffect(pagerState.currentPage) {
        onStatusBarColorChange(WorkerHomeHeaderTopColor)
    }

    // Set initial status bar color
    LaunchedEffect(Unit) {
        onStatusBarColorChange(WorkerHomeHeaderTopColor)
    }


    // REMOVED: Vacancy status loading on home screen
    // Home screen shows preview only - full details load on job detail screen
    // This saves 500ms per page load


    // Location text - show FULL address like professional apps (Swiggy, Zomato, Flipkart)
    val locationText = remember(currentLocation) {
        when {
            currentLocation != null -> {
                val loc = currentLocation!!

                formatWorkerHomeLocation(loc)
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

    val topLocationChips = remember(currentLocation) {
        TopCityChips.buildTopLocationChips(currentLocation)
    }

    val onLocationChipSelected: (TopCityChips.CityLocationChip) -> Unit = { chip ->
        val selectedLocation = TopCityChips.toLocationData(chip)
        locationPreferences.savePreferredLocation(selectedLocation)
        jobViewModel.setUserLocation(selectedLocation.latitude, selectedLocation.longitude, immediate = true)
        jobViewModel.loadJobsSummaryForHome()
    }

    // Layered gradient background to give the home screen a richer visual identity.
    Box(modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF0FDFA),
                    Color(0xFFEFF6FF),
                    Color(0xFFFFFBEB)
                )
            )
        )
    ) {
        WorkerHomeBackdropDecor(modifier = Modifier.fillMaxSize())

        // Track location bar alpha from scroll
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
                            if (hasLocationPermission) {
                                isLocationLoading = true
                            }
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
                                        jobViewModel.loadJobsSummaryForHome()
                                        if (hasLocationPermission) {
                                            isLocationLoading = true
                                        }
                                    }
                                )
                            }

                            else -> {
                                val showEmptyJobsState = !jobUiState.isLoading && (jobUiState.jobs.isEmpty() || filteredJobs.isEmpty())
                                val isAppliedAllVariant = !jobUiState.jobs.isEmpty() && filteredJobs.isEmpty()

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
                                    currentLocation = currentLocation,
                                    onLocationChipSelected = onLocationChipSelected,
                                    userName = currentUser?.displayName ?: "",
                                    userEmail = currentUser?.email ?: "",
                                    userSkills = emptyList(),
                                    onScrollOffsetChange = { offset ->
                                        onStatusBarColorChange(WorkerHomeHeaderTopColor)
                                    },
                                    onLocationBarAlphaChange = { alpha ->
                                        locationBarAlpha = alpha
                                    },
                                    showEmptyJobsState = showEmptyJobsState,
                                    emptyJobsIsAppliedAllVariant = isAppliedAllVariant,
                                    emptyJobsCurrentLocationName = currentLocation?.getShortAddress(),
                                    emptyJobsSuggestedCities = topLocationChips,
                                    onEmptyJobsCitySelected = onLocationChipSelected
                                )
                            }
                        }
                    }
                    
                    // Voice Search FAB - DISABLED
                    /*
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
                            .padding(bottom = 30.dp, end = 16.dp)
                            .size(56.dp),
                        shape = CircleShape,
                        containerColor = Color(0xFF1F2937),
                        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    */
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
                    locationBarAlpha = locationBarAlpha,
                    isLocationLoading = isLocationLoading || locationLoadingState,
                    unreadNotificationCount = unreadNotificationCount,
                    onMapClick = { navController.navigate(Routes.WORKER_JOB_MAP) },
                    onNotificationClick = {
                        currentUser?.uid?.let { userId ->
                            scope.launch {
                                try {
                                    unreadNotificationCount = jobApplicationService.getUnreadNotificationCount(
                                        userId = userId,
                                        activeRole = "WORKER"
                                    )
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

private fun formatWorkerHomeLocation(location: com.example.dutype.models.LocationData): String {
    val rawAddress = when {
        location.address.isNotBlank() -> location.address
        else -> listOfNotNull(
            location.area?.takeIf { it.isNotBlank() },
            location.city?.takeIf { it.isNotBlank() },
            location.state?.takeIf { it.isNotBlank() },
            location.country?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
    }

    val normalizedParts = rawAddress
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .fold(mutableListOf<String>()) { acc, part ->
            if (acc.none { it.equals(part, ignoreCase = true) }) {
                acc.add(part)
            }
            acc
        }

    return normalizedParts.joinToString(", ").ifBlank { "Select Your Location" }
}


@Composable
private fun LoadingContent() {
    WorkerHomeShimmer()
}

@Composable
private fun WorkerHomeBackdropDecor(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(310.dp)
                .offset(x = 200.dp, y = (-130).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF99F6E4).copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-90).dp, y = 450.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFBFDBFE).copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun EmptyJobsState(
    navController: NavController? = null,
    currentLocationName: String? = null,
    isAppliedAllVariant: Boolean = false,
    suggestedCities: List<TopCityChips.CityLocationChip> = emptyList(),
    onCitySelected: (TopCityChips.CityLocationChip) -> Unit = {}
) {
    // Rotate through humorous messages so repeat visits feel fresh
    val humorMessages = remember {
        listOf(
            "Looks like the jobs took a chai break ☕\nTry a different area — they're hiding nearby!",
            "Even Google Maps can't find jobs here 😅\nLet's search somewhere else!",
            "The jobs are playing hide & seek 🙈\nChange your location and catch them!",
            "Your area is on a job vacation 🏖️\nPick another spot and get back to work!",
            "No jobs found... but your potential is unlimited 💪\nTry a different location!"
        )
    }
    val messageIndex = remember { (0 until humorMessages.size).random() }
    val humorMessage = humorMessages[messageIndex]

    val locationLabel = currentLocationName?.let { "near \"$it\"" } ?: "in your area"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Illustration circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAppliedAllVariant) "🎉" else "📍",
                    fontSize = 44.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isAppliedAllVariant) "You're on fire!" else "No jobs $locationLabel",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isAppliedAllVariant)
                    "You've applied to everything here 🚀\nChange your location to find more opportunities!"
                else
                    humorMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280),
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Primary CTA — navigate to location picker
            Button(
                onClick = { navController?.navigate(Routes.MANUAL_LOCATION_ROUTE) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Change Location",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (suggestedCities.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestedCities) { chip ->
                        FilterChip(
                            selected = false,
                            onClick = { onCitySelected(chip) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(chip.label())
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = Color(0xFF1F2937)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Secondary hint
            Text(
                text = "Jobs are available in 500+ cities across India",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF)
                ),
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
    currentLocation: com.example.dutype.models.LocationData? = null,
    onLocationChipSelected: (TopCityChips.CityLocationChip) -> Unit = {},
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
    onLocationBarAlphaChange: (Float) -> Unit = {},
    showEmptyJobsState: Boolean = false,
    emptyJobsIsAppliedAllVariant: Boolean = false,
    emptyJobsCurrentLocationName: String? = null,
    emptyJobsSuggestedCities: List<TopCityChips.CityLocationChip> = emptyList(),
    onEmptyJobsCitySelected: (TopCityChips.CityLocationChip) -> Unit = {}
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
        // Jobs are already distance-enriched and sorted by ViewModel/engine.
        // Keep home preview concise; full list is available in All Jobs.
        availableJobs.take(5)
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
    
    // P1 FIX: Use snapshotFlow to debounce scroll offset changes (was firing 60x/sec)
    LaunchedEffect(Unit) {
        snapshotFlow { scrollOffset.value }
            .collect { offset -> onScrollOffsetChange(offset) }
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
                                        .background(Color.White)
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
        // Location chips removed per design requirement
        /*
        item {
            TopLocationChipsSection(
                currentLocation = currentLocation,
                onLocationChipSelected = onLocationChipSelected
            )
        }
        */

        if (showEmptyJobsState) {
            item {
                EmptyJobsState(
                    navController = rootNavController,
                    currentLocationName = emptyJobsCurrentLocationName,
                    isAppliedAllVariant = emptyJobsIsAppliedAllVariant,
                    suggestedCities = emptyJobsSuggestedCities,
                    onCitySelected = onEmptyJobsCitySelected
                )
            }
        }

        if (!showEmptyJobsState) {
            // Section 2: Browse Categories (at the top) - transparent to show gradient
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

            // Section 3: Jobs For You (skill-matched) - transparent to show gradient
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
                        sectionTitle = when {
                            userSkills.isNotEmpty() -> stringResource(R.string.jobs_for_you)
                            skillMatchedJobs.any { it.distance != null } -> stringResource(R.string.jobs_near_you)
                            else -> null
                        }
                    )
                }
            }
        }
        
        // Section 4: DutyPe Promise Carousel (at the bottom after jobs)
        item {
            DutyPePromiseCarousel()
        }
        
    }
    }
}

@Composable
private fun TopLocationChipsSection(
    currentLocation: com.example.dutype.models.LocationData?,
    onLocationChipSelected: (TopCityChips.CityLocationChip) -> Unit
) {
    val chips = remember(currentLocation) {
        TopCityChips.buildTopLocationChips(currentLocation)
    }

    if (chips.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "Top locations",
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.Black,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chips) { chip ->
                FilterChip(
                    selected = false,
                    onClick = { onLocationChipSelected(chip) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(chip.label())
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        labelColor = Color(0xFF1F2937)
                    )
                )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
    locationBarAlpha: Float,
    isLocationLoading: Boolean = false,
    unreadNotificationCount: Int = 0,
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
                brush = Brush.verticalGradient(
                    colors = listOf(
                        WorkerHomeHeaderTopColor,
                        WorkerHomeHeaderMidColor,
                        WorkerHomeHeaderBottomColor
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DutyPe",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        color = Color(0xFF1A1A1A)
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = Color(0xFF1A1A1A).copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF1A1A1A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-3).dp, y = 7.dp)
                                .background(
                                    color = Color(0xFFF97316),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((60.dp * locationBarAlpha.coerceIn(0f, 1f)).coerceAtLeast(0.dp))
                .clipToBounds()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 2.dp + (6.dp * locationBarAlpha)
                )
                .graphicsLayer {
                    alpha = locationBarAlpha.coerceIn(0f, 1f)
                    translationY = -10f * (1f - locationBarAlpha)
                }
        ) {
            androidx.compose.material3.Surface(
                onClick = onLocationClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = 0.985f + (0.015f * locationBarAlpha)
                        scaleY = 0.96f + (0.04f * locationBarAlpha)
                    },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A1A).copy(alpha = 0.05f + (0.05f * locationBarAlpha)),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF1A1A1A).copy(alpha = 0.08f + (0.12f * locationBarAlpha))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF92400E).copy(alpha = 0.82f + (0.18f * locationBarAlpha)),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Work zone",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF78716C).copy(alpha = 0.64f + (0.36f * locationBarAlpha)),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = locationText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1A1A1A).copy(alpha = 0.8f + (0.2f * locationBarAlpha)),
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(20.dp)
                    ) {
                        if (isLocationLoading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF92400E).copy(alpha = 0.82f + (0.18f * locationBarAlpha))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF1A1A1A).copy(alpha = 0.52f + (0.28f * locationBarAlpha)),
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
