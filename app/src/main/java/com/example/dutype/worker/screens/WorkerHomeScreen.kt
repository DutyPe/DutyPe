package com.example.dutype.worker.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
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
import androidx.compose.ui.res.colorResource
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
import com.example.dutype.components.AppUpdatePrompt
import com.example.dutype.components.BirthdayBanner
import com.example.dutype.components.WelcomeCelebrationOverlay
import com.example.dutype.components.consumeWelcomeCelebrationFlag
import com.example.dutype.components.LocationAutocompleteField
import com.example.dutype.components.NotificationPermissionBottomSheet
import com.example.dutype.components.OfflineBanner
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.components.WorkerHomeShimmer
import com.example.dutype.components.openNotificationSettings
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.models.LocationData
import com.example.dutype.navigation.Routes
import com.example.dutype.location.TopCityChips
import com.example.dutype.services.BirthdayService
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.DeepLinkHandler
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.utils.appVersionInfo
import com.example.dutype.viewmodels.AppConfigViewModel
import com.example.dutype.viewmodels.ConnectivityViewModel
import com.example.dutype.viewmodels.EarningsViewModel
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.InstantHelpViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.viewmodels.WorkerJobRequestViewModel
import com.example.dutype.worker.components.JobCard
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

// Status bar matches the top of the worker home header. These are Android
// resources so color-only release tweaks do not rewrite classes.dex.
internal val WorkerHomeHeaderTopColor: Color
    @Composable @ReadOnlyComposable get() = colorResource(R.color.worker_home_header_top)
internal val WorkerHomeHeaderMidColor: Color
    @Composable @ReadOnlyComposable get() = colorResource(R.color.worker_home_header_mid)
internal val WorkerHomeHeaderBottomColor: Color
    @Composable @ReadOnlyComposable get() = colorResource(R.color.worker_home_header_bottom)

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
    val gettingLocationText = stringResource(R.string.getting_location)
    val tapToGetLocationText = stringResource(R.string.tap_to_get_location)
    val enableLocationText = stringResource(R.string.enable_location)
    // LAZY LOADING: Only instantiate ViewModels needed for HomeScreen
    // Other ViewModels are instantiated on their respective screens
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    // LocationPreferences accessed via FirestoreJobViewModel (proper DI pattern)
    val locationPreferences = jobViewModel.locationPreferences
    val currentLocation by locationPreferences.currentLocation.collectAsStateWithLifecycle()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val isGuestUser = currentUser == null || currentUser.isAnonymous
    val jobApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val announcementViewModel: com.example.dutype.viewmodels.AnnouncementViewModel = hiltViewModel()
    val workerJobRequestViewModel: WorkerJobRequestViewModel = hiltViewModel()
    val instantHelpViewModel: InstantHelpViewModel = hiltViewModel()
    val appConfigViewModel: AppConfigViewModel = hiltViewModel()
    val announcements by announcementViewModel.announcements.collectAsStateWithLifecycle()
    val workerRequestState by workerJobRequestViewModel.uiState.collectAsStateWithLifecycle()
    val instantHelpState by instantHelpViewModel.uiState.collectAsStateWithLifecycle()
    val referralConfig by appConfigViewModel.referralConfig.collectAsStateWithLifecycle()
    val appUpdateConfig by appConfigViewModel.appUpdateConfig.collectAsStateWithLifecycle()
    val appVersionInfo = remember(context) { context.appVersionInfo() }
    val scope = rememberCoroutineScope()
    val jobApplicationService = jobApplicationViewModel.jobApplicationService
    val earningsViewModel: EarningsViewModel = hiltViewModel()
    val earningsUiState by earningsViewModel.uiState.collectAsStateWithLifecycle()
    val locationService = jobViewModel.locationService
    val locationRepository = remember { com.example.dutype.di.locationRepositoryFromHilt(context) }
    val jobUiState by jobViewModel.uiState.collectAsStateWithLifecycle()
    
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    var workerRating by remember { mutableStateOf(0f) }
    var workerReviewCount by remember { mutableIntStateOf(0) }
    
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
    
    // P1-2: Removed dead `permissionsRequested`, `isFirstTimeUser`, `bottomSheetsShownInSession`,
    // and `hasNotificationPermission` flags — each was assigned an initial value but never read.

    // Bottom sheet state - declare before permission launchers
    var showNotificationBottomSheet by remember { mutableStateOf(false) }
    var showLocationPickerSheet by remember { mutableStateOf(false) }
    var locationPickerText by remember { mutableStateOf("") }
    var locationPickerError by remember { mutableStateOf<String?>(null) }
    var isFetchingSheetLocation by remember { mutableStateOf(false) }
    var shouldFetchCurrentLocationAfterPermission by remember { mutableStateOf(false) }
    var showNoUrgentJobsToastAfterSwitchOn by remember { mutableStateOf(false) }
    val locationPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    suspend fun saveWorkerHomeLocation(locationData: LocationData, manual: Boolean) {
        if (manual) {
            locationPreferences.savePreferredLocation(locationData)
        } else {
            locationPreferences.setLocationModeAuto()
            locationPreferences.saveLocation(locationData, forceManualOverride = true)
            locationPreferences.setPermissionGranted(true)
        }

        if (GeoUtils.hasValidCoordinates(locationData.latitude, locationData.longitude)) {
            jobViewModel.setUserLocation(locationData.latitude, locationData.longitude, immediate = true)
        }

        currentUser?.uid?.let { userId ->
            runCatching {
                val firestore = com.example.dutype.di.firestoreFromHilt(context)
                val updateData = mutableMapOf<String, Any>(
                    "address" to locationData.getFullAddress(),
                    "location" to mapOf(
                        "lat" to locationData.latitude,
                        "lng" to locationData.longitude
                    ),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                if (GeoUtils.hasValidCoordinates(locationData.latitude, locationData.longitude)) {
                    updateData["geohash"] = GeoUtils.encodeGeohash(locationData.latitude, locationData.longitude)
                }
                firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES)
                    .document(userId)
                    .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                    .await()
            }.onFailure { error ->
                Timber.e(error, "Failed to sync selected worker location")
            }
        }
    }

    suspend fun fetchCurrentLocationFromSheet() {
        isFetchingSheetLocation = true
        locationPickerError = null
        try {
            val locationInfo = locationService.getHighAccuracyLocation(
                timeoutMs = 15000L,
                minAccuracyMeters = 10f
            )
            if (locationInfo == null || !GeoUtils.hasValidCoordinates(locationInfo.latitude, locationInfo.longitude)) {
                locationPickerError = context.getString(R.string.worker_location_fetch_failed)
                return
            }
            val locationData = locationService.toLocationData(locationInfo)
            saveWorkerHomeLocation(locationData, manual = false)
            locationPickerText = locationData.getFullAddress()
            showLocationPickerSheet = false
            android.widget.Toast.makeText(context, context.getString(R.string.location_updated), android.widget.Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Timber.e(error, "Failed to fetch current location from worker home")
            locationPickerError = context.getString(
                R.string.worker_location_update_failed,
                error.message ?: context.getString(R.string.try_again)
            )
        } finally {
            isFetchingSheetLocation = false
        }
    }

    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val wasGranted = hasLocationPermission
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        // If permission was just granted, set loading state
        if (!wasGranted && hasLocationPermission) {
            if (shouldFetchCurrentLocationAfterPermission) {
                shouldFetchCurrentLocationAfterPermission = false
                scope.launch { fetchCurrentLocationFromSheet() }
            } else {
                isLocationLoading = true
            }
        } else if (!hasLocationPermission && shouldFetchCurrentLocationAfterPermission) {
            shouldFetchCurrentLocationAfterPermission = false
            locationPickerError = context.getString(R.string.location_permission_required_current)
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
                    Timber.d(" Using fresh cached location, skipping new GPS fetch")
                    jobViewModel.setUserLocation(
                        freshCachedLocation.latitude,
                        freshCachedLocation.longitude,
                        immediate = true
                    )
                    isLocationLoading = false
                    return@LaunchedEffect
                }

                //  UBER/SWIGGY STRATEGY: Get location instantly, upgrade in background
                // This provides immediate results while improving accuracy.
                // Uses LocationRepository so concurrent screens share a single GPS request.
                Timber.d(" Starting FAST location fetch (Uber/Swiggy strategy)...")
                
                locationRepository.refresh { locationData ->
                    if (locationData != null) {
                        Timber.d(" âš¡ Location update received: ${locationData.getShortAddress()} (${locationData.accuracy}m)")
                        
                        // Location already saved by getLocationFast()
                        locationPreferences.setPermissionGranted(true)
                        
                        // Convert LocationInfo to LocationData for Firestore sync
                        val data = locationService.toLocationData(locationData)
                        
                        // Save to Firestore for cross-device sync
                        currentUser?.uid?.let { userId ->
                            launch(Dispatchers.IO) {
                                try {
                                    val firestore = com.example.dutype.di.firestoreFromHilt(context)
                                    firestore.collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES).document(userId).set(
                                        mapOf(
                                            "address" to data.getFullAddress(),
                                            "location" to mapOf(
                                                "lat" to data.latitude,
                                                "lng" to data.longitude
                                            ),
                                            "geohash" to GeoUtils.encodeGeohash(data.latitude, data.longitude),
                                            "updatedAt" to com.google.firebase.Timestamp.now()
                                        ),
                                        com.google.firebase.firestore.SetOptions.merge()
                                    ).await()
                                    Timber.d(" Location synced to Firestore")
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
                            Timber.d(" FORCING UI REFRESH after location update")
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
        Timber.d("WorkerHomeScreen - INIT: Starting ULTRA-FAST initialization")
        
        // CRITICAL: Refresh StateFlow from SharedPreferences in case location was saved
        // while this screen wasn't composed (e.g., saved from SelectRoleScreen async GPS)
        locationPreferences.refreshLocation()
        
        // CRITICAL FIX: Check if location already exists FIRST
        val savedLocation = locationPreferences.getSavedLocationIfFresh()
        val hasValidLocation = savedLocation != null && 
                              savedLocation.latitude != 0.0 && 
                              savedLocation.longitude != 0.0
        
        if (hasValidLocation) {
            Timber.d("Using cached location: ${savedLocation?.getShortAddress()}")
            // Set location immediately for instant distance calculations
            jobViewModel.setUserLocation(
                savedLocation!!.latitude, 
                savedLocation.longitude, 
                immediate = true
            )

            if (hasLocationPermission && !jobViewModel.locationFetchedInSession && !locationPreferences.isManualLocationLocked()) {
                Timber.d("Refreshing location in background for WorkerHomeScreen")
                jobViewModel.locationFetchedInSession = true
                isLocationLoading = true
            }
        } else if (hasLocationPermission && !jobViewModel.locationFetchedInSession && !locationPreferences.isManualLocationLocked()) {
            // Permission granted but no saved location - fetch it
            Timber.d("Permission granted but no saved location - fetching now")
            jobViewModel.locationFetchedInSession = true
            isLocationLoading = true
        }
        
        // PERFORMANCE FIX: Load ONLY 3 jobs for instant home screen load
        // This is the Instagram/TikTok pattern - show something immediately
        Timber.d("Loading 3 jobs for instant display...")
        jobViewModel.loadJobsSummaryForHome()
        
        Timber.d("WorkerHomeScreen - INIT: Complete (instant - <100ms)")
    }
    
    // CRITICAL: React to location changes from async GPS callbacks
    // This ensures the UI updates immediately when location is fetched
    // (e.g., from SelectRoleScreen's async GPS or WorkerHomeScreen's own fetch)
    LaunchedEffect(currentLocation) {
        val loc = currentLocation
        if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
            Timber.d(" Location StateFlow updated: ${loc.getShortAddress()} - updating ViewModel")
            jobViewModel.setUserLocation(loc.latitude, loc.longitude, immediate = true)
        }
    }
    
    // Load announcements immediately so guests and logged-in users both see them.
    LaunchedEffect(Unit) {
        announcementViewModel.loadAnnouncements("worker")
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            workerJobRequestViewModel.loadPendingRequests()
            instantHelpViewModel.loadWorkerInstantHelp(currentLocation)
            earningsViewModel.loadEarnings()

            runCatching {
                val workerDoc = com.example.dutype.di.firestoreFromHilt(context)
                    .collection(com.example.dutype.firestore.FirestoreCollections.WORKER_PROFILES)
                    .document(currentUser.uid)
                    .get()
                    .await()
                if (workerDoc.exists()) {
                    workerRating = (workerDoc.getDouble("rating") ?: 0.0).toFloat()
                    workerReviewCount = workerDoc.getLong("totalRatings")?.toInt()
                        ?: workerDoc.getLong("reviewCount")?.toInt()
                        ?: 0
                }
            }.onFailure {
                Timber.w(it, "Failed to load worker rating summary for home header")
            }
        }
    }

    val paidTransactions = remember(earningsUiState.transactions) {
        earningsUiState.transactions.filter { it.status == PaymentStatus.PAID }
    }

    val todayEarningsAmount = remember(paidTransactions) {
        val now = java.util.Calendar.getInstance()
        paidTransactions
            .filter {
                val txCal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                txCal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
                    txCal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
            }
            .sumOf { it.amount }
    }

    val thisWeekEarningsAmount = remember(paidTransactions) {
        val now = java.util.Calendar.getInstance()
        paidTransactions
            .filter {
                val txCal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                txCal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
                    txCal.get(java.util.Calendar.WEEK_OF_YEAR) == now.get(java.util.Calendar.WEEK_OF_YEAR)
            }
            .sumOf { it.amount }
    }

    val todayJobsDone = remember(paidTransactions) {
        val now = java.util.Calendar.getInstance()
        paidTransactions.count {
            val txCal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
            txCal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
                txCal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
        }
    }

    val thisWeekJobsDone = remember(paidTransactions) {
        val now = java.util.Calendar.getInstance()
        paidTransactions.count {
            val txCal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
            txCal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
                txCal.get(java.util.Calendar.WEEK_OF_YEAR) == now.get(java.util.Calendar.WEEK_OF_YEAR)
        }
    }

    LaunchedEffect(currentUser?.uid, currentLocation?.latitude, currentLocation?.longitude) {
        if (currentUser?.uid != null && currentLocation != null) {
            instantHelpViewModel.loadWorkerInstantHelp(currentLocation)
        }
    }

    LaunchedEffect(
        showNoUrgentJobsToastAfterSwitchOn,
        instantHelpState.workerAvailability.isAvailable,
        instantHelpState.isLoadingRequests,
        instantHelpState.instantRequests,
        instantHelpState.error
    ) {
        if (
            showNoUrgentJobsToastAfterSwitchOn &&
            instantHelpState.workerAvailability.isAvailable &&
            !instantHelpState.isLoadingRequests &&
            instantHelpState.instantRequests.isEmpty() &&
            instantHelpState.error.isNullOrBlank()
        ) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.no_urgent_jobs_currently),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            showNoUrgentJobsToastAfterSwitchOn = false
        } else if (instantHelpState.instantRequests.isNotEmpty() || !instantHelpState.error.isNullOrBlank()) {
            showNoUrgentJobsToastAfterSwitchOn = false
        }
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
        Timber.d(" WorkerHomeScreen: Announcements updated - count: ${announcements.size}")
        announcements.forEach { announcement ->
            Timber.d("   - ${announcement.title} (targetRole: ${announcement.targetRole})")
        }
    }

    val workerHomeHeaderTopColor = WorkerHomeHeaderTopColor

    // Update status bar color (was previously also keyed on pagerState.currentPage; pager removed in P1-2).
    LaunchedEffect(workerHomeHeaderTopColor) {
        onStatusBarColorChange(Color.White)
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
                    isLocationLoading -> gettingLocationText
                    else -> tapToGetLocationText
                }
            } else {
                enableLocationText
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
                            workerJobRequestViewModel.loadPendingRequests()
                            instantHelpViewModel.refreshWorkerInstantRequests(currentLocation)
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
                                        onStatusBarColorChange(Color.White)
                                    },
                                    onLocationBarAlphaChange = { alpha ->
                                        locationBarAlpha = alpha
                                    },
                                    showEmptyJobsState = showEmptyJobsState,
                                    emptyJobsIsAppliedAllVariant = isAppliedAllVariant,
                                    emptyJobsCurrentLocationName = currentLocation?.getShortAddress(),
                                    emptyJobsSuggestedCities = topLocationChips,
                                    onEmptyJobsCitySelected = onLocationChipSelected,
                                    showGuestWelcomeCard = isGuestUser,
                                    guestWelcomeTitle = stringResource(R.string.guest_worker_welcome_title),
                                    guestWelcomeMessage = stringResource(R.string.guest_worker_welcome_message),
                                    guestWelcomeButtonText = stringResource(R.string.guest_welcome_login_register),
                                    onGuestWelcomeClick = {
                                        rootNavController.navigate("${Routes.ENHANCED_LOGIN}?role=WORKER")
                                    },
                                    referralRewardAmount = referralConfig.rewardPerReferral.toInt(),
                                    onReferEarnClick = {
                                        if (isGuestUser) {
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.login_to_refer_earn),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            navController.navigate(Routes.WORKER_REFER_EARN)
                                        }
                                    },
                                    announcements = announcements,
                                    onDismissAnnouncement = { id -> announcementViewModel.dismissAnnouncement(id) },
                                    birthdayService = birthdayService,
                                    workerJobRequests = workerRequestState.requests,
                                    updatingWorkerJobRequestId = workerRequestState.updatingRequestId,
                                    workerAvailability = instantHelpState.workerAvailability,
                                    instantRequests = instantHelpState.instantRequests,
                                    updatingInstantRequestId = instantHelpState.updatingRequestId,
                                    isLoadingInstantRequests = instantHelpState.isLoadingRequests,
                                    instantHelpError = instantHelpState.error,
                                    onTurnOnAvailability = {
                                        val selectedLocation = currentLocation
                                        if (isGuestUser) {
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.please_login_instant_works),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else if (
                                            selectedLocation == null ||
                                            !GeoUtils.hasValidCoordinates(selectedLocation.latitude, selectedLocation.longitude)
                                        ) {
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.set_location_before_instant),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            showNoUrgentJobsToastAfterSwitchOn = true
                                            instantHelpViewModel.setWorkerAvailability(true, selectedLocation)
                                        }
                                    },
                                    onApplyInstantRequest = { request ->
                                        instantHelpViewModel.respondToInstantRequest(request, "applied")
                                    },
                                    onCallInstantRequest = { request ->
                                        openWorkerUrgentDialer(
                                            context,
                                            request.contactNumber.ifBlank { request.employerPhone }
                                        )
                                        instantHelpViewModel.respondToInstantRequest(request, "called") {
                                            // Contact UI is opened immediately; this callback only confirms the response record.
                                        }
                                    },
                                    onAcceptWorkerJobRequest = { request ->
                                        workerJobRequestViewModel.acceptRequest(request.requestId) { acceptedJobId ->
                                            if (acceptedJobId.isNotBlank()) {
                                                navController.navigate(Routes.jobDetailRoute(acceptedJobId))
                                            }
                                        }
                                    },
                                    onRejectWorkerJobRequest = { request ->
                                        workerJobRequestViewModel.rejectRequest(request.requestId)
                                    },
                                    onOpenWorkerJobRequest = { request ->
                                        if (request.jobId.isNotBlank()) {
                                            navController.navigate(Routes.jobDetailRoute(request.jobId))
                                        }
                                    },
                                    todayEarningsAmount = todayEarningsAmount,
                                    todayJobsDone = todayJobsDone,
                                    thisWeekEarningsAmount = thisWeekEarningsAmount,
                                    weekJobsDone = thisWeekJobsDone,
                                    ratingValue = workerRating,
                                    reviewCount = workerReviewCount
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
                    isLocationLoading = isLocationLoading,
                    unreadNotificationCount = unreadNotificationCount,
                    isInstantAvailable = instantHelpState.workerAvailability.isAvailable,
                    isInstantAvailabilitySaving = instantHelpState.isSavingAvailability,
                    todayEarningsAmount = todayEarningsAmount,
                    todayJobsDone = todayJobsDone,
                    thisWeekEarningsAmount = thisWeekEarningsAmount,
                    weekJobsDone = thisWeekJobsDone,
                    ratingValue = workerRating,
                    reviewCount = workerReviewCount,
                    onInstantAvailabilityChange = { isAvailable ->
                        val selectedLocation = currentLocation
                        if (isGuestUser) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.please_login_instant_works),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else if (
                            isAvailable &&
                            (selectedLocation == null || !GeoUtils.hasValidCoordinates(selectedLocation.latitude, selectedLocation.longitude))
                        ) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.set_location_before_instant),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            showNoUrgentJobsToastAfterSwitchOn = isAvailable
                            instantHelpViewModel.setWorkerAvailability(isAvailable, selectedLocation)
                        }
                    },
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
                    onLocationClick = {
                        locationPickerText = currentLocation?.getDisplayAddress().orEmpty()
                        locationPickerError = null
                        showLocationPickerSheet = true
                    }
                )
            }
        }

        if (showLocationPickerSheet) {
            WorkerHomeLocationPickerSheet(
                sheetState = locationPickerSheetState,
                value = locationPickerText,
                onValueChange = {
                    locationPickerText = it
                    locationPickerError = null
                },
                locationService = locationService,
                isFetchingCurrentLocation = isFetchingSheetLocation,
                errorMessage = locationPickerError,
                onDismiss = { showLocationPickerSheet = false },
                onUseCurrentLocation = {
                    if (locationService.hasLocationPermission()) {
                        scope.launch { fetchCurrentLocationFromSheet() }
                    } else {
                        shouldFetchCurrentLocationAfterPermission = true
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onLocationSelected = { selectedAddress, latitude, longitude ->
                    val selectedLocation = buildWorkerHomeLocationData(
                        address = selectedAddress,
                        latitude = latitude,
                        longitude = longitude
                    )
                    scope.launch {
                        saveWorkerHomeLocation(selectedLocation, manual = true)
                        locationPickerText = selectedAddress
                        showLocationPickerSheet = false
                        android.widget.Toast.makeText(context, context.getString(R.string.location_updated), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
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

        if (!showNotificationBottomSheet && !showLocationPickerSheet) {
            AppUpdatePrompt(
                config = appUpdateConfig,
                currentVersionCode = appVersionInfo.code,
                userRole = "WORKER"
            )
        }

        // Welcome celebration overlay — shown once after new user completes profile
        var showCelebration by remember { mutableStateOf(consumeWelcomeCelebrationFlag(context)) }
        WelcomeCelebrationOverlay(
            visible = showCelebration,
            bonusAmount = referralConfig.signupBonus.toInt().takeIf { it > 0 } ?: 0,
            onDismiss = { showCelebration = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerHomeLocationPickerSheet(
    sheetState: androidx.compose.material3.SheetState,
    value: String,
    onValueChange: (String) -> Unit,
    locationService: com.example.dutype.utils.LocationService,
    isFetchingCurrentLocation: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onLocationSelected: (String, Double, Double) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.choose_work_location),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            )
            Text(
                text = stringResource(R.string.choose_work_location_body),
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B))
            )

            Button(
                onClick = onUseCurrentLocation,
                enabled = !isFetchingCurrentLocation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                if (isFetchingCurrentLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isFetchingCurrentLocation) {
                        stringResource(R.string.fetching_location)
                    } else {
                        stringResource(R.string.use_current_location)
                    }
                )
            }

            LocationAutocompleteField(
                value = value,
                onValueChange = onValueChange,
                onLocationSelected = onLocationSelected,
                locationService = locationService,
                label = stringResource(R.string.search_location),
                placeholder = stringResource(R.string.area_street_city),
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun buildWorkerHomeLocationData(
    address: String,
    latitude: Double,
    longitude: Double
): LocationData {
    val parts = address.split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val area = parts.firstOrNull().orEmpty()
    val city = parts.drop(1)
        .firstOrNull { !it.equals(area, ignoreCase = true) && !it.equals("India", ignoreCase = true) }

    return LocationData(
        latitude = latitude,
        longitude = longitude,
        city = city,
        address = address,
        area = area.ifBlank { null },
        country = "India"
    )
}

private fun openWorkerUrgentDialer(context: android.content.Context, phone: String) {
    val normalized = phone.filter { it.isDigit() || it == '+' }
    if (normalized.isBlank()) {
        android.widget.Toast.makeText(context, context.getString(R.string.contact_number_unavailable), android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalized")))
    }.onFailure {
        android.widget.Toast.makeText(context, context.getString(R.string.unable_to_open_dialer), android.widget.Toast.LENGTH_SHORT).show()
    }
}
