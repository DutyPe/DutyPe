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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

internal val WorkerHomeHeaderTopColor = Color(0xFF000000)
internal val WorkerHomeHeaderMidColor = Color(0xFF000000)
internal val WorkerHomeHeaderBottomColor = Color(0xFF000000)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class
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
    val locationRepository = remember { com.example.dutype.di.locationRepositoryFromHilt(context) }
    val jobUiState by jobViewModel.uiState.collectAsStateWithLifecycle()
    
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    
    // P1-2: BirthdayService kept (passed to HomeSectionsContent); the local birthdayInfo/showBirthdayBanner
    // mutableState pair previously declared here was dead (never assigned, never read) and was deleted.
    val birthdayService: BirthdayService = com.example.dutype.di.rememberBirthdayService()

    // PERFORMANCE FIX P0: Use ViewModel's filtered jobs instead of computing in Composable
    val filteredJobs by jobViewModel.filteredJobs.collectAsStateWithLifecycle()

    // PERFORMANCE FIX P2: Use ViewModel's vacancy statuses (cleared on refresh)
    val jobVacancyStatuses by jobViewModel.jobVacancyStatuses.collectAsStateWithLifecycle()

    // Permission handling - Check permissions only once.
    // P1-2: Removed unused `hasNotificationPermission` (never read after assignment).
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

    // P1-2: Removed dead `permissionsRequested`, `isFirstTimeUser`, `bottomSheetsShownInSession`,
    // and `hasNotificationPermission` flags — each was assigned an initial value but never read.

    // Bottom sheet state - declare before permission launchers
    var showNotificationBottomSheet by remember { mutableStateOf(false) }

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
    
    // P1-2: Removed `voiceSearchLauncher` — only consumer was the commented-out Voice Search FAB block
    // (also removed below). Restore both together if voice search is reintroduced.

    // Track if location fetch is in progress to prevent duplicate calls
    var locationFetchInProgress by remember { mutableStateOf(false) }

    // Fetch location when permission is granted and loading is true
    LaunchedEffect(isLocationLoading) {
        if (isLocationLoading && hasLocationPermission && !locationFetchInProgress) {
            locationFetchInProgress = true
            try {
                val freshCachedLocation = locationPreferences.getSavedLocationIfFresh(5 * 60 * 1000L)
                if (freshCachedLocation != null) {
                    Timber.d("ðŸ“ Using fresh cached location, skipping new GPS fetch")
                    jobViewModel.setUserLocation(
                        freshCachedLocation.latitude,
                        freshCachedLocation.longitude,
                        immediate = true
                    )
                    isLocationLoading = false
                    return@LaunchedEffect
                }

                // ðŸš€ UBER/SWIGGY STRATEGY: Get location instantly, upgrade in background
                // This provides immediate results while improving accuracy.
                // Uses LocationRepository so concurrent screens share a single GPS request.
                Timber.d("ðŸ“ Starting FAST location fetch (Uber/Swiggy strategy)...")
                
                locationRepository.refresh { locationData ->
                    if (locationData != null) {
                        Timber.d("ðŸ“ âš¡ Location update received: ${locationData.getShortAddress()} (${locationData.accuracy}m)")
                        
                        // Location already saved by getLocationFast()
                        locationPreferences.setPermissionGranted(true)
                        
                        // Convert LocationInfo to LocationData for Firestore sync
                        val data = locationService.toLocationData(locationData)
                        
                        // Save to Firestore for cross-device sync
                        currentUser?.uid?.let { userId ->
                            launch(Dispatchers.IO) {
                                try {
                                    val firestore = com.example.dutype.di.firestoreFromHilt(context)
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
                                    Timber.d("ðŸ“ Location synced to Firestore")
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
                            Timber.d("ðŸ“ FORCING UI REFRESH after location update")
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
        Timber.d("ðŸ  WorkerHomeScreen - INIT: Starting ULTRA-FAST initialization")
        
        // CRITICAL: Refresh StateFlow from SharedPreferences in case location was saved
        // while this screen wasn't composed (e.g., saved from SelectRoleScreen async GPS)
        locationPreferences.refreshLocation()
        
        // CRITICAL FIX: Check if location already exists FIRST
        val savedLocation = locationPreferences.getSavedLocationIfFresh()
        val hasValidLocation = savedLocation != null && 
                              savedLocation.latitude != 0.0 && 
                              savedLocation.longitude != 0.0
        
        if (hasValidLocation) {
            Timber.d("ðŸ“ Using cached location: ${savedLocation?.getShortAddress()}")
            // Set location immediately for instant distance calculations
            jobViewModel.setUserLocation(
                savedLocation!!.latitude, 
                savedLocation.longitude, 
                immediate = true
            )

            if (hasLocationPermission && !jobViewModel.locationFetchedInSession && !locationPreferences.isManualLocationLocked()) {
                Timber.d("ðŸ“ Refreshing location in background for WorkerHomeScreen")
                jobViewModel.locationFetchedInSession = true
                isLocationLoading = true
            }
        } else if (hasLocationPermission && !jobViewModel.locationFetchedInSession && !locationPreferences.isManualLocationLocked()) {
            // Permission granted but no saved location - fetch it
            Timber.d("ðŸ“ Permission granted but no saved location - fetching now")
            jobViewModel.locationFetchedInSession = true
            isLocationLoading = true
        }
        
        // PERFORMANCE FIX: Load ONLY 3 jobs for instant home screen load
        // This is the Instagram/TikTok pattern - show something immediately
        Timber.d("ðŸ  Loading 3 jobs for instant display...")
        jobViewModel.loadJobsSummaryForHome()
        
        Timber.d("ðŸ  WorkerHomeScreen - INIT: Complete (instant - <100ms)")
    }
    
    // CRITICAL: React to location changes from async GPS callbacks
    // This ensures the UI updates immediately when location is fetched
    // (e.g., from SelectRoleScreen's async GPS or WorkerHomeScreen's own fetch)
    LaunchedEffect(currentLocation) {
        val loc = currentLocation
        if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
            Timber.d("ðŸ“ Location StateFlow updated: ${loc.getShortAddress()} - updating ViewModel")
            jobViewModel.setUserLocation(loc.latitude, loc.longitude, immediate = true)
        }
    }
    
    // Load announcements immediately so guests and logged-in users both see them.
    LaunchedEffect(Unit) {
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

    // P1-2: Removed `playStoreUrl` constant + `shareToWhatsApp` lambda. Both were duplicated
    // in WorkerProfile / refer-earn screens (where they are actually
    // invoked) and were never called from WorkerHomeScreen.

    // P1-2: Removed unused `tabTitles`, `tabIcons`, the duplicate `coroutineScope`, and `pagerState`.
    // The screen no longer renders an Accompanist HorizontalPager — these declarations were leftover
    // from a previous tab-based layout. Status-bar color is now driven by the single LaunchedEffect below.
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Debug: Log when announcements change
    LaunchedEffect(announcements) {
        Timber.d("ðŸ“¢ WorkerHomeScreen: Announcements updated - count: ${announcements.size}")
        announcements.forEach { announcement ->
            Timber.d("ðŸ“¢   - ${announcement.title} (targetRole: ${announcement.targetRole})")
        }
    }

    // Update status bar color (was previously also keyed on pagerState.currentPage; pager removed in P1-2).
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

    // Solid role background — every worker screen shares the same clean
    // white surface so the role identity stays consistent across the app.
    Box(modifier = Modifier
        .fillMaxSize()
        .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
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
                                    onJobClick = { /* P1-2: was assigning to dead `clickedJobId` state; no-op now */ },
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
                                    onEmptyJobsCitySelected = onLocationChipSelected,
                                    announcements = announcements,
                                    onDismissAnnouncement = { id -> announcementViewModel.dismissAnnouncement(id) },
                                    birthdayService = birthdayService
                                )
                            }
                        }
                    }
                    
                    // P1-2: Removed commented-out Voice Search FAB block (DISABLED for >6 months).
                    // Voice search lives behind WORKER_ALL_JOBS?voiceQuery= now; reintroduce here only
                    // alongside the matching `voiceSearchLauncher` if voice-from-home is brought back.
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
