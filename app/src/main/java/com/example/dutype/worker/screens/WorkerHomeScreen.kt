package com.example.dutype.worker.screens

import com.example.dutype.ui.theme.fg
import com.example.dutype.ui.theme.bg
import com.dutype.app.R
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
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.dutype.utils.findActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.dutype.components.AnnouncementList
import com.example.dutype.components.AppUpdatePrompt
import com.example.dutype.di.rememberInAppReviewTriggerService
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
import com.example.dutype.models.LocationData
import com.example.dutype.navigation.Routes
import com.example.dutype.location.TopCityChips
import com.example.dutype.services.BirthdayService
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.DeepLinkHandler
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.utils.appVersionInfo
import com.example.dutype.ui.theme.PrimaryBlue
import com.example.dutype.viewmodels.AppConfigViewModel
import com.example.dutype.viewmodels.ConnectivityViewModel
import com.example.dutype.viewmodels.EarningsViewModel
import com.example.dutype.viewmodels.WorkerHomeViewModel
import com.example.dutype.viewmodels.InstantHelpViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.worker.components.JobCard
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
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
    // FAST HOME FEED: WorkerHomeViewModel loads cached jobs instantly, then nearest jobs and
    // instant/urgent jobs in parallel (see HomeJobsSource). Same instance as HomeSectionsContent uses.
    val jobViewModel: WorkerHomeViewModel = hiltViewModel()
    // LocationPreferences accessed via the ViewModel (proper DI pattern)
    val locationPreferences = jobViewModel.locationPreferences
    val currentLocation by locationPreferences.currentLocation.collectAsStateWithLifecycle()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val isGuestUser = currentUser == null || currentUser.isAnonymous
    val workerOnline by jobViewModel.isOnline.collectAsStateWithLifecycle()
    val savingOnline by jobViewModel.isSavingOnline.collectAsStateWithLifecycle()
    val jobApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val announcementViewModel: com.example.dutype.viewmodels.AnnouncementViewModel = hiltViewModel()
    val instantHelpViewModel: InstantHelpViewModel = hiltViewModel()
    val appConfigViewModel: AppConfigViewModel = hiltViewModel()
    val announcements by announcementViewModel.announcements.collectAsStateWithLifecycle()
    val instantHelpState by instantHelpViewModel.uiState.collectAsStateWithLifecycle()
    val referralConfig by appConfigViewModel.referralConfig.collectAsStateWithLifecycle()
    val appUpdateConfig by appConfigViewModel.appUpdateConfig.collectAsStateWithLifecycle()
    val dynamicFeaturesConfig by appConfigViewModel.dynamicFeaturesConfig.collectAsStateWithLifecycle()
    val applicationStats by jobApplicationViewModel.stats.collectAsStateWithLifecycle()
    val appVersionInfo = remember(context) { context.appVersionInfo() }
    val scope = rememberCoroutineScope()
    val notificationRepository = jobViewModel.notificationRepository
    val earningsViewModel: EarningsViewModel = hiltViewModel()
    val profileCompletionService = hiltViewModel<com.example.dutype.viewmodels.ProfileCompletionViewModel>().profileCompletionService
    val earningsUiState by earningsViewModel.uiState.collectAsStateWithLifecycle()
    val locationService = jobViewModel.locationService
    val locationRepository = remember { com.example.dutype.di.locationRepositoryFromHilt(context) }
    val jobUiState by jobViewModel.uiState.collectAsStateWithLifecycle()

    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    var workerRating by remember { mutableStateOf(0f) }
    var workerReviewCount by remember { mutableIntStateOf(0) }
    var showOutOfAreaSheet by remember { mutableStateOf(false) }
    var outOfAreaDistanceKm by remember { mutableStateOf(0.0) }

    // P1-2: BirthdayService kept (passed to HomeSectionsContent); the local birthdayInfo/showBirthdayBanner
    // mutableState pair previously declared here was dead (never assigned, never read) and was deleted.
    val birthdayService: BirthdayService = com.example.dutype.di.rememberBirthdayService()
    val inAppReviewTriggerService = rememberInAppReviewTriggerService()

    // PERFORMANCE FIX P0: Use ViewModel's filtered jobs instead of computing in Composable
    val filteredJobs by jobViewModel.filteredJobs.collectAsStateWithLifecycle()
    val urgentJobs by jobViewModel.filteredUrgentJobs.collectAsStateWithLifecycle()


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
    var hasAttemptedLocationRequest by remember { mutableStateOf(false) }

    // Re-check permission automatically when user returns to the app (e.g. from System Settings)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val isGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (isGranted && !hasLocationPermission) {
                    hasLocationPermission = true
                    isLocationLoading = true
                }
                jobViewModel.loadJobsSummaryForHome()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Bottom sheet state - declare before permission launchers
    var showNotificationBottomSheet by remember { mutableStateOf(false) }
    var showLocationPickerSheet by remember { mutableStateOf(false) }
    var locationPickerText by remember { mutableStateOf("") }
    var locationPickerError by remember { mutableStateOf<String?>(null) }
    var isFetchingSheetLocation by remember { mutableStateOf(false) }
    var shouldFetchCurrentLocationAfterPermission by remember { mutableStateOf(false) }
    var showNoUrgentJobsToastAfterSwitchOn by remember { mutableStateOf(false) }
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    var loginSheetTitle by remember { mutableStateOf("") }
    var loginSheetSubtitle by remember { mutableStateOf("") }
    // P1 PLAY STORE COMPLIANCE & SMOOTH PERMISSION UX: Show friendly bottom sheet
    // explaining location benefits before triggering Android system permission dialog.
    var showLocationPermissionBottomSheet by remember { mutableStateOf(false) }
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
            jobViewModel.onLocationChanged(locationData.latitude, locationData.longitude)
            instantHelpViewModel.refreshWorkerInstantRequests(locationData)
        }

        currentUser?.uid?.let { userId ->
            runCatching {
                profileCompletionService.saveWorkerLocation(
                    locationData.latitude, locationData.longitude, locationData.getFullAddress()
                ).getOrThrow()
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
            jobViewModel.onGpsFix(locationData.latitude, locationData.longitude)
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
    }

    fun requestOrOpenLocationSettings() {
        val activity = context.findActivity()
        val isPermanentlyDenied = activity != null && hasAttemptedLocationRequest &&
            !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) &&
            !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (isPermanentlyDenied) {
            val message = context.getString(R.string.worker_enable_location_in_settings)
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            openNotificationSettings(context)
        } else {
            hasAttemptedLocationRequest = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Direct location permission request on screen entry
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            hasAttemptedLocationRequest = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            isLocationLoading = true
        }
    }

    // Track if location fetch is in progress to prevent duplicate calls
    var locationFetchInProgress by remember { mutableStateOf(false) }

    // Fetch location when permission is granted and loading is true
    LaunchedEffect(isLocationLoading, hasLocationPermission) {
        if (isLocationLoading) {
            if (!hasLocationPermission) {
                isLocationLoading = false
                locationFetchInProgress = false
                return@LaunchedEffect
            }
            if (locationFetchInProgress) return@LaunchedEffect

            locationFetchInProgress = true
            try {
                withTimeoutOrNull(3500L) {
                    val freshCachedLocation = locationPreferences.getSavedLocationIfFresh(5 * 60 * 1000L)
                    if (freshCachedLocation != null) {
                        Timber.d("  Using fresh cached location, skipping new GPS fetch")
                        jobViewModel.setUserLocation(
                            freshCachedLocation.latitude,
                            freshCachedLocation.longitude,
                            immediate = true
                        )
                        return@withTimeoutOrNull
                    }

                    // UBER/SWIGGY STRATEGY: Get location instantly, upgrade in background
                    Timber.d("  Starting FAST location fetch (Uber/Swiggy strategy)...")
                    locationRepository.refresh { locationData ->
                        if (locationData != null) {
                            Timber.d("  ⚡ Location update received: ${locationData.getShortAddress()} (${locationData.accuracy}m)")
                            locationPreferences.setPermissionGranted(true)
                            val data = locationService.toLocationData(locationData)
                            // Current area only (the profile's home location is set in the profile).
                            jobViewModel.onGpsFix(data.latitude, data.longitude)

                            // A place the worker picked (e.g. Delhi) wins over GPS: the feed stays there
                            // until they switch back to "use my current location".
                            if ((data.latitude != 0.0 || data.longitude != 0.0) &&
                                !locationPreferences.isManualLocationLocked()
                            ) {
                                jobViewModel.setUserLocation(
                                    data.latitude,
                                    data.longitude,
                                    immediate = true
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch location")
            } finally {
                delay(200L)
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
        // Last known location (any age) so the nearest-jobs query never waits for a fresh GPS fix;
        // a background refresh below refines it.
        val savedLocation = locationPreferences.getSavedLocation()
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

    // Review prompt: Prompt worker to rate on Google Play after spending some active time (35 seconds)
    LaunchedEffect(Unit) {
        delay(35_000)
        context.findActivity()?.let { activity ->
            inAppReviewTriggerService.onAppUsedForSomeTime(activity)
        }
    }

    // Location permission will be requested via the bottom sheet when user taps the empty state.
    // Notification permission is requested in WorkerNotificationScreen (contextual — user already
    // navigated to notifications, so they clearly want them).

    LaunchedEffect(currentUser?.uid) {
        if (currentUser?.uid != null) {
            // With a known location the effect below loads instant help once; avoid a duplicate fetch.
            if (currentLocation == null) {
                instantHelpViewModel.loadWorkerInstantHelp(null)
            }
            earningsViewModel.loadEarnings()

            runCatching {
                profileCompletionService.workerCard(currentUser.uid)?.let { card ->
                    workerRating = card.rating.toFloat()
                    workerReviewCount = card.ratingCount
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

    // Real-time home service bookings (cleaning, etc.) while worker is online
    var searchingServiceRequests by remember { mutableStateOf<List<com.example.dutype.models.InstantRequest>>(emptyList()) }
    DisposableEffect(workerOnline) {
        if (!workerOnline) {
            searchingServiceRequests = emptyList()
            return@DisposableEffect onDispose {}
        }
        val reg = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("service_bookings")
            .whereEqualTo("status", "SEARCHING")
            .limit(10)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val now = System.currentTimeMillis()
                searchingServiceRequests = snap.documents.mapNotNull { doc ->
                    runCatching {
                        val exp = doc.getTimestamp("expiresAt")?.toDate()?.time ?: (now + 30 * 60 * 1000L)
                        if (exp <= now) return@mapNotNull null
                        val cat = doc.getString("category").orEmpty()
                        val svcName = doc.getString("serviceName").orEmpty().ifBlank { cat }
                        val price = (doc.getLong("price") ?: 0L).toDouble()
                        val addr = doc.getString("addressText").orEmpty()
                        val area = doc.getString("area").orEmpty()
                        com.example.dutype.models.InstantRequest(
                            requestId = "svc_${doc.id}",
                            employerId = doc.getString("customerId").orEmpty(),
                            employerName = doc.getString("customerName").orEmpty().ifBlank { "DutyPe Customer" },
                            title = "Home Service: $svcName",
                            description = doc.getString("note").orEmpty().ifBlank { "Customer requested $svcName in ${area.ifBlank { addr }}" },
                            category = cat.ifBlank { "Services" },
                            perPersonPayment = price,
                            workersNeeded = 1,
                            status = "open",
                            addressText = addr.ifBlank { area },
                            lat = doc.getDouble("lat") ?: 0.0,
                            lng = doc.getDouble("lng") ?: 0.0,
                            expiresAt = exp,
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: now
                        )
                    }.getOrNull()
                }
            }
        onDispose { reg.remove() }
    }

    // Urgent job audio & vibration alert: rings when urgent jobs or home services arrive in worker's location.
    // If worker ignores/dismisses, it will NOT play sound again for those jobs.
    var ignoredInstantRequestIds by remember { mutableStateOf(setOf<String>()) }

    val combinedWorkerRequests: List<com.example.dutype.models.InstantRequest> = remember(instantHelpState.instantRequests, searchingServiceRequests) {
        val existingIds = instantHelpState.instantRequests.map { it.requestId }.toSet()
        instantHelpState.instantRequests + searchingServiceRequests.filter { it.requestId !in existingIds }
    }

    val activeWorkerRequests: List<com.example.dutype.models.InstantRequest> = remember(combinedWorkerRequests, ignoredInstantRequestIds) {
        combinedWorkerRequests.filter {
            it.requestId !in ignoredInstantRequestIds && !com.example.dutype.urgent.UrgentSoundAlertManager.isIgnored(context, it.requestId)
        }
    }

    val skippedWorkerRequests: List<com.example.dutype.models.InstantRequest> = remember(combinedWorkerRequests, ignoredInstantRequestIds) {
        combinedWorkerRequests.filter {
            it.requestId in ignoredInstantRequestIds || com.example.dutype.urgent.UrgentSoundAlertManager.isIgnored(context, it.requestId)
        }
    }

    LaunchedEffect(
        workerOnline,
        activeWorkerRequests,
        urgentJobs
    ) {
        if (workerOnline) {
            val urgentIds = activeWorkerRequests.map { it.requestId } + urgentJobs.map { it.id }
            if (urgentIds.isNotEmpty()) {
                com.example.dutype.urgent.UrgentSoundAlertManager.playAlertForAnyNew(context, urgentIds)
            }
        } else {
            com.example.dutype.urgent.UrgentSoundAlertManager.stopSound()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            com.example.dutype.urgent.UrgentSoundAlertManager.stopSound()
        }
    }

    // Fetch unread notification count after initial load
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { userId ->
            delay(3000)
            try {
                unreadNotificationCount = notificationRepository.unreadCount()
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
    var isPullRefreshing by remember { mutableStateOf(false) }
    val isRefreshingActive = isPullRefreshing || jobUiState.isRefreshing

    // Debug: Log when announcements change
    LaunchedEffect(announcements) {
        Timber.d(" WorkerHomeScreen: Announcements updated - count: ${announcements.size}")
        announcements.forEach { announcement ->
            Timber.d("   - ${announcement.title} (targetRole: ${announcement.targetRole})")
        }
    }

    // Status bar color driven by the dynamic primary color from remote config.
    // Falls back to the worker_home_header_top resource color if not set.
    val defaultHeaderColor = WorkerHomeHeaderTopColor
    val dynamicStatusBarColor = remember(dynamicFeaturesConfig.primaryColor, defaultHeaderColor) {
        try {
            Color(android.graphics.Color.parseColor(dynamicFeaturesConfig.primaryColor))
        } catch (_: Exception) {
            defaultHeaderColor
        }
    }
    LaunchedEffect(dynamicStatusBarColor) {
        onStatusBarColorChange(dynamicStatusBarColor)
    }


    // REMOVED: Vacancy status loading on home screen
    // Home screen shows preview only - full details load on job detail screen
    // This saves 500ms per page load


    // Location text - show FULL address like professional apps (Swiggy, Zomato, Flipkart)
    val locationText = remember(currentLocation, hasLocationPermission, isLocationLoading) {
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

    // Short area name for the header location chip (falls back to the full location text).
    val homeAreaName = remember(currentLocation, locationText) {
        currentLocation?.area?.takeIf { it.isNotBlank() }
            ?: currentLocation?.city?.takeIf { it.isNotBlank() }
            ?: locationText
    }

    val topLocationChips = remember(currentLocation) {
        TopCityChips.buildTopLocationChips(currentLocation)
    }

    val onLocationChipSelected: (TopCityChips.CityLocationChip) -> Unit = { chip ->
        val selectedLocation = TopCityChips.toLocationData(chip)
        locationPreferences.savePreferredLocation(selectedLocation)
        jobViewModel.onLocationChanged(selectedLocation.latitude, selectedLocation.longitude)
        instantHelpViewModel.refreshWorkerInstantRequests(selectedLocation)
    }

    // Solid role background — every worker screen shares the same clean
    // white surface so the role identity stays consistent across the app.
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFF8FAFC).bg())
    ) {
        WorkerHomeBackdropDecor(modifier = Modifier.fillMaxSize())

        // Track location bar alpha from scroll
        var locationBarAlpha by remember { mutableStateOf(1f) }
        var headerHeightDp by remember { mutableStateOf(180.dp) }
        val density = LocalDensity.current

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
                        isRefreshing = isRefreshingActive,
                        onRefresh = {
                            scope.launch {
                                isPullRefreshing = true
                                jobViewModel.refreshJobs()
                                if (hasLocationPermission) {
                                    isLocationLoading = true
                                }
                                announcementViewModel.loadAnnouncements("WORKER") // Refresh announcements for workers
                                instantHelpViewModel.refreshWorkerInstantRequests(currentLocation)
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
                                color = com.example.dutype.ui.theme.WorkerColors.Primary.fg()
                            )
                        }
                    ) {
                        when {
                            else -> {
                                val jobsFailed = jobUiState.hasError && jobUiState.jobs.isEmpty()
                                val showEmptyJobsState = !jobUiState.isLoading && !jobsFailed &&
                                    (jobUiState.jobs.isEmpty() || filteredJobs.isEmpty())
                                val isAppliedAllVariant = !jobUiState.jobs.isEmpty() && filteredJobs.isEmpty()
                                val showCategoryRail = hasLocationPermission && !showEmptyJobsState

                                HomeSectionsContent(
                                    jobListings = filteredJobs,
                                    urgentJobs = urgentJobs,
                                    isLoadingJobs = jobUiState.isLoading,
                                    navController = navController,
                                    rootNavController = rootNavController,
                                    savedJobsViewModel = savedJobsViewModel,
                                    hasLocationPermission = hasLocationPermission,
                                    context = context,
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
                                        onStatusBarColorChange(dynamicStatusBarColor)
                                    },
                                    onLocationBarAlphaChange = { alpha ->
                                        locationBarAlpha = alpha
                                    },
                                    headerHeightDp = headerHeightDp,
                                    showEmptyJobsState = showEmptyJobsState,
                                    jobsFailed = jobsFailed,
                                    onRetryJobs = {
                                        jobViewModel.loadJobsSummaryForHome(forceRefresh = true)
                                        if (hasLocationPermission) isLocationLoading = true
                                    },
                                    emptyJobsIsAppliedAllVariant = isAppliedAllVariant,
                                    emptyJobsCurrentLocationName = currentLocation?.getShortAddress(),
                                    emptyJobsSuggestedCities = topLocationChips,
                                    onEmptyJobsCitySelected = onLocationChipSelected,
                                    showGuestWelcomeCard = isGuestUser,
                                    guestWelcomeTitle = stringResource(R.string.guest_worker_welcome_title),
                                    guestWelcomeMessage = stringResource(R.string.guest_worker_welcome_message),
                                    guestWelcomeButtonText = stringResource(R.string.guest_welcome_login_register),
                                    onGuestWelcomeClick = {
                                        loginSheetTitle = context.getString(R.string.guest_apply_login_title)
                                        loginSheetSubtitle = context.getString(R.string.guest_apply_login_desc)
                                        showLoginBottomSheet = true
                                    },
                                    appliedJobsCount = applicationStats.appliedApplications,
                                    announcements = announcements,
                                    onDismissAnnouncement = { id -> announcementViewModel.dismissAnnouncement(id) },
                                    birthdayService = birthdayService,
                                    instantRequests = activeWorkerRequests,
                                    skippedInstantRequests = skippedWorkerRequests,
                                    updatingInstantRequestId = instantHelpState.updatingRequestId,
                                    isLoadingInstantRequests = instantHelpState.isLoadingRequests,
                                    instantHelpError = instantHelpState.error,
                                    showOnlineToggle = true,
                                    isOnline = workerOnline,
                                    isSavingOnline = savingOnline,
                                    onOnlineChange = { online ->
                                        if (isGuestUser) {
                                            loginSheetTitle = context.getString(R.string.guest_apply_login_title)
                                            loginSheetSubtitle = "Log in to turn ON Duty and start receiving instant job alerts."
                                            showLoginBottomSheet = true
                                            return@HomeSectionsContent
                                        }
                                        if (online) {
                                            val lat = currentLocation?.latitude
                                            val lng = currentLocation?.longitude
                                            if (lat != null && lng != null) {
                                                val dist = calculateHaversineDistanceKm(lat, lng, KHAMMAM_CENTER_LAT, KHAMMAM_CENTER_LNG)
                                                if (dist > KHAMMAM_SERVICE_RADIUS_KM) {
                                                    outOfAreaDistanceKm = dist
                                                    showOutOfAreaSheet = true
                                                    return@HomeSectionsContent
                                                }
                                            }
                                        }
                                        jobViewModel.setOnline(online) { message ->
                                            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onApplyInstantRequest = { request ->
                                        if (isGuestUser) {
                                            loginSheetTitle = context.getString(R.string.guest_apply_login_title)
                                            loginSheetSubtitle = context.getString(R.string.guest_apply_login_desc)
                                            showLoginBottomSheet = true
                                        } else if (request.requestId.startsWith("svc_")) {
                                            val bId = request.requestId.removePrefix("svc_")
                                            navController.navigate(com.example.dutype.navigation.Routes.partnerOfferRoute(bId))
                                        } else {
                                            // Same full-screen offer as the notification: Accept / Skip.
                                            navController.navigate(com.example.dutype.navigation.Routes.urgentOfferRoute(request.requestId))
                                        }
                                    },
                                    onCallInstantRequest = { request ->
                                        if (isGuestUser) {
                                            loginSheetTitle = context.getString(R.string.guest_call_login_title)
                                            loginSheetSubtitle = context.getString(R.string.guest_call_login_desc)
                                            showLoginBottomSheet = true
                                        } else if (request.requestId.startsWith("svc_")) {
                                            val bId = request.requestId.removePrefix("svc_")
                                            navController.navigate(com.example.dutype.navigation.Routes.partnerOfferRoute(bId))
                                        } else {
                                            openWorkerUrgentDialer(
                                                context,
                                                request.contactNumber.ifBlank { request.employerPhone }
                                            )
                                            instantHelpViewModel.respondToInstantRequest(request, "called") {
                                                // Contact UI is opened immediately; this callback only confirms the response record.
                                            }
                                        }
                                    },
                                    onIgnoreInstantRequest = { request ->
                                        com.example.dutype.urgent.UrgentSoundAlertManager.ignoreRequest(context, request.requestId)
                                        ignoredInstantRequestIds = ignoredInstantRequestIds + request.requestId
                                        android.widget.Toast.makeText(context, "Skipped — available below silently", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    todayEarningsAmount = todayEarningsAmount,
                                    todayJobsDone = todayJobsDone,
                                    thisWeekEarningsAmount = thisWeekEarningsAmount,
                                    weekJobsDone = thisWeekJobsDone,
                                    ratingValue = workerRating,
                                    reviewCount = workerReviewCount,
                                    promoBannerUrl = dynamicFeaturesConfig.promoBannerUrl,
                                    onRequestLocationPermission = {
                                        showLocationPermissionBottomSheet = true
                                    },
                                    // Map hidden for now (hero card and map tab are commented out).
                                    // onMapClick = { com.example.dutype.components.navigateToWorkerTab(navController, com.example.dutype.navigation.WorkerBottomRoutes.MAP) },
                                    onCategoryTap = { category ->
                                        val normalizedCategory = if (category.equals("All", ignoreCase = true)) {
                                            "All Jobs"
                                        } else {
                                            category
                                        }
                                        com.example.dutype.components.navigateToWorkerTab(
                                            navController,
                                            "${Routes.WORKER_ALL_JOBS}?filter=${Uri.encode(normalizedCategory)}",
                                            restoreState = false
                                        )
                                    },
                                    headerContent = {
                                        WorkerHomeGreetingHeader(
                                            areaName = homeAreaName,
                                            isLocationLoading = isLocationLoading && currentLocation == null,
                                            unreadNotificationCount = unreadNotificationCount,
                                            onNotificationClick = {
                                                currentUser?.uid?.let { userId ->
                                                    scope.launch {
                                                        try {
                                                            unreadNotificationCount = notificationRepository.unreadCount()
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
                                )
                            }
                        }
                    }

                    // P1-2: Removed commented-out Voice Search FAB block (DISABLED for >6 months).
                    // Voice search lives behind WORKER_ALL_JOBS?voiceQuery= now; reintroduce here only
                    // alongside the matching `voiceSearchLauncher` if voice-from-home is brought back.
                }
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
                        // P1: Show disclosure first; launcher fires inside the dialog's
                        // confirm callback so the system prompt only appears after consent.
                        shouldFetchCurrentLocationAfterPermission = true
                        showLocationPermissionBottomSheet = true
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

        // P1 PLAY STORE COMPLIANCE & SMOOTH PERMISSION UX — Location Permission Bottom Sheet
        // Google Play requires prominent disclosure before requesting location permission.
        // This bottom sheet explains the value clearly before launching the system prompt.
        com.example.dutype.components.LocationPermissionBottomSheet(
            isVisible = showLocationPermissionBottomSheet,
            onDismiss = {
                showLocationPermissionBottomSheet = false
                shouldFetchCurrentLocationAfterPermission = false
            },
            onAllowLocation = {
                showLocationPermissionBottomSheet = false
                requestOrOpenLocationSettings()
            }
        )

        // Guest Mode Login Bottom Sheet
        com.example.dutype.components.LoginBottomSheet(
            isVisible = showLoginBottomSheet,
            onDismiss = { showLoginBottomSheet = false },
            onLoginSuccess = {
                showLoginBottomSheet = false
            },
            role = com.example.dutype.models.UserRole.WORKER,
            title = if (loginSheetTitle.isNotBlank()) loginSheetTitle else stringResource(R.string.guest_apply_login_title),
            subtitle = if (loginSheetSubtitle.isNotBlank()) loginSheetSubtitle else stringResource(R.string.guest_apply_login_desc),
            navController = navController
        )

        if (!showNotificationBottomSheet && !showLocationPickerSheet) {
            AppUpdatePrompt(
                config = appUpdateConfig,
                currentVersionCode = appVersionInfo.code,
                userRole = "WORKER"
            )
        }

        if (showOutOfAreaSheet) {
            WorkerOutOfAreaBottomSheet(
                distanceKm = outOfAreaDistanceKm,
                onDismiss = { showOutOfAreaSheet = false }
            )
        }
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
        containerColor = WorkerColors.CardBackground
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
                    color = WorkerColors.TextPrimary
                )
            )
            Text(
                text = stringResource(R.string.choose_work_location_body),
                style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary)
            )

            Button(
                onClick = onUseCurrentLocation,
                enabled = !isFetchingCurrentLocation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary)
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
                    style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.Error)
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

private const val KHAMMAM_CENTER_LAT = 17.2473
private const val KHAMMAM_CENTER_LNG = 80.1514
private const val KHAMMAM_SERVICE_RADIUS_KM = 15.0

private fun calculateHaversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkerOutOfAreaBottomSheet(
    distanceKm: Double,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF2F2)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📍", fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Outside Serviceable Area",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF0F172A)
                ),
                textAlign = TextAlign.Center
            )

            if (distanceKm > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You are currently ${String.format("%.1f", distanceKm)} km away from Khammam City.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFFDC2626)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "DutyPe Instant Jobs & Home Services are currently active only within Khammam City (15 km radius). To turn ON duty and receive orders, you must be located within the Khammam service zone.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Understood",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
