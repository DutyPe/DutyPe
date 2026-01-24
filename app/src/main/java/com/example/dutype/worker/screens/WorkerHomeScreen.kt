package com.example.dutype.worker.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
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
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.WorkerGradientBackground
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.ComponentHeights
import com.example.dutype.components.JobCardShimmer
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.utils.DeepLinkHandler
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.JobApplicationViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.worker.components.JobCard
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.components.BirthdayBanner
import com.example.dutype.components.ConnectivityAwareScreen
import com.example.dutype.components.OfflineBanner
import com.example.dutype.components.AnnouncementList
import com.example.dutype.viewmodels.ConnectivityViewModel
import com.example.dutype.services.BirthdayInfo
import com.example.dutype.services.BirthdayService
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState  
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import timber.log.Timber

// Helper function to open DutyPe app settings
fun openLocationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

// NOTE: hasAppliedToJob function removed - Apply button removed from JobCard
// Users now apply from JobDescriptionScreen only

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
    // LAZY LOADING: Only instantiate ViewModels needed for HomeScreen
    // Other ViewModels are instantiated on their respective screens
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    // LocationPreferences accessed via FirestoreJobViewModel (proper DI pattern)
    val locationPreferences = jobViewModel.locationPreferences
    val currentLocation by locationPreferences.currentLocation.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val jobApplicationViewModel: JobApplicationViewModel = hiltViewModel()
    // SavedJobsViewModel needed for save/unsave functionality on job cards
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    // Announcement ViewModel for in-app announcements
    val announcementViewModel: com.example.dutype.viewmodels.AnnouncementViewModel = hiltViewModel()
    val announcements by announcementViewModel.announcements.collectAsStateWithLifecycle()
    val dataStore: ApplicationFormDataStore = remember { ApplicationFormDataStore(context) }
    // NOTE: ProfileViewModel, NotificationViewModel removed from HomeScreen (lazy loading)
    // - Profile loads on ProfileScreen
    // - Notifications load on NotificationScreen
    val scope = rememberCoroutineScope()
    // Services accessed via ViewModels (proper DI pattern - no ServiceProviders)
    val jobApplicationService = jobApplicationViewModel.jobApplicationService
    val locationService = jobViewModel.locationService
    val jobUiState by jobViewModel.uiState.collectAsState()
    
    // Unread notification count for badge (lightweight - only count, not full notifications)
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    
    // Birthday wish state 🎂
    val birthdayService: BirthdayService = hiltViewModel<FirestoreJobViewModel>().let {
        // Access via Hilt - we'll inject it properly
        remember { 
            com.example.dutype.services.BirthdayService(
                com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                FirebaseAuth.getInstance(),
                com.example.dutype.services.NotificationService(context, com.google.firebase.firestore.FirebaseFirestore.getInstance())
            )
        }
    }
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

    // Track if location fetch is in progress to prevent duplicate calls
    var locationFetchInProgress by remember { mutableStateOf(false) }

    // Fetch location when permission is granted and loading is true
    LaunchedEffect(isLocationLoading) {
        if (isLocationLoading && hasLocationPermission && !locationFetchInProgress) {
            locationFetchInProgress = true
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
        Timber.d("🏠 WorkerHomeScreen - INIT: Starting minimal initialization (lazy loading enabled)")
        
        // Load announcements for worker role
        announcementViewModel.loadAnnouncements("worker")
        
        // CRITICAL: Load jobs in PARALLEL, not blocking
        launch {
            jobViewModel.loadJobsSummaryForHome()
        }
        
        // Location handling in separate coroutine (non-blocking)
        if (hasLocationPermission) {
            launch {
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
                        Timber.d("📍 LOCATION: No valid cached location, fetching fresh in background...")
                        isLocationLoading = true
                    }
                }
            }
        }
        
        // Handle permission bottom sheets (only once per session)
        if (!bottomSheetsShownInSession) {
            bottomSheetsShownInSession = true
            if (!hasNotificationPermission) {
                Timber.d("🏠 WorkerHomeScreen - Showing notification bottom sheet")
                showNotificationBottomSheet = true
            }
        }
        
        // Fetch unread notification count for badge (lightweight - only count)
        currentUser?.uid?.let { userId ->
            launch {
                try {
                    unreadNotificationCount = jobApplicationService.getUnreadNotificationCount(userId)
                    
                    // Birthday check
                    if (!birthdayService.hasWishedToday(context, userId)) {
                        val bday = birthdayService.checkIfBirthday(userId)
                        if (bday != null) {
                            birthdayInfo = bday
                            showBirthdayBanner = true
                            birthdayService.sendBirthdayNotification(userId, bday.userName)
                            birthdayService.markWishedToday(context, userId)
                            Timber.i("🎂 Happy Birthday ${bday.userName}! Banner and notification sent.")
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to fetch unread notification count or check birthday")
                }
            }
        }
        
        Timber.d("🏠 WorkerHomeScreen - INIT: Complete (lazy loading)")
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
                            loc.streetName?.takeIf { it.isNotBlank() },
                            loc.area?.takeIf { it.isNotBlank() },
                            loc.landmark?.takeIf { it.isNotBlank() },
                            loc.city?.takeIf { it.isNotBlank() },
                            loc.state?.takeIf { it.isNotBlank() },
                            loc.postalCode?.takeIf { it.isNotBlank() }
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

    WorkerGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .background(WorkerColors.ScreenBackground)
        ) {
            // Offline banner at the very top
            val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
            val isOnline by connectivityViewModel.isOnline.collectAsStateWithLifecycle()
            OfflineBanner(isOffline = !isOnline)
            

            // Header section - White background with curved bottom edge
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = WorkerColors.CardBackground,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(bottom = 6.dp)
            ) {
                // Top row with DutyPe and icons - Minimal vertical padding for tight spacing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp), // Minimal vertical padding for tight spacing
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side - DutyPe text
                    Text(
                        text = "DutyPe",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            color = Color(0xFF1F2937),
                            letterSpacing = (-0.5).sp
                        )
                    )

                    // Right side - Map and Notification buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Map View chip button - Jobs on Map (Accessibility Feature)
                        androidx.compose.material3.Surface(
                            onClick = { navController.navigate(Routes.WORKER_JOB_MAP) },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF8FAFC),
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
                                    tint = Color(0xFF374151),
                                    modifier = Modifier.size(IconSizes.Small) // Material Design 3: 20dp
                                )
                                Text(
                                    text = stringResource(R.string.map_view),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF374151)
                                    )
                                )
                            }
                        }
                        
                        // Notification icon with badge for unread messages
                        Box {
                            IconButton(
                                onClick = { navController.navigate(Routes.WORKER_NOTIFICATIONS) },
                                modifier = Modifier.size(ComponentHeights.MinimumTouchTarget) // Material Design 3: 48dp touch target
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(IconSizes.Standard) // Material Design 3: 24dp
                                )
                            }
                            
                            // Red dot badge when there are unread notifications
                            if (unreadNotificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 6.dp)
                                        .background(
                                            WorkerColors.Error,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
                
                // Flipkart-style Location Bar - Compact with white background and subtle corners
                androidx.compose.material3.Surface(
                    onClick = { rootNavController.navigate(Routes.MANUAL_LOCATION_ROUTE) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 0.dp), // Zero vertical padding for tightest spacing
                    shape = RoundedCornerShape(4.dp), // Very light rounded corners (reduced from 8dp to 4dp)
                    color = Color.White // Pure white background like professional apps
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 3.dp), // Reduced from 4dp to 3dp for more compact height
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Near me / Location icon (like Flipkart)
                        Icon(
                            painter = painterResource(id = R.drawable.near_me_24),
                            contentDescription = null,
                            tint = Color(0xFF1F2937),
                            modifier = Modifier.size(IconSizes.Small) // Material Design 3: 20dp
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Location text - FULL address display like Swiggy/Zomato/Flipkart
                        // Shows complete address with all components (street, area, city, state, PIN)
                        Text(
                            text = locationText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF374151),
                                fontSize = 13.sp,
                                lineHeight = 16.sp // Better line spacing for multi-line addresses
                            ),
                            maxLines = 3, // Allow up to 3 lines for full address
                            overflow = TextOverflow.Ellipsis, // Show ellipsis only if exceeds 3 lines
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(6.dp)) // Reduced spacing
                        
                        // Loading indicator or dropdown arrow
                        if (isLocationLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(IconSizes.Small), // Material Design 3: 20dp
                                strokeWidth = 2.dp,
                                color = Color(0xFF1F2937)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF374151),
                                modifier = Modifier.size(IconSizes.Standard) // Material Design 3: 24dp
                            )
                        }
                    }
                }
            }

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

            // Content section - Sections based home screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color(0xFFF9FAFB))
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
                        // This prevents shimmer from showing after jobs are already loaded
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
                            // PERFORMANCE FIX P0: Use ViewModel's filteredJobs instead of computing here
                            // This prevents excessive recomposition when applications list changes
                            // Filtering is now done in ViewModel with combine() operator
                            
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
                                        // LAZY LOADING: Use FirebaseAuth data instead of ProfileViewModel
                                        // Full profile data loads on ProfileScreen
                                        userName = currentUser?.displayName ?: "",
                                        userEmail = currentUser?.email ?: "",
                                        userSkills = emptyList() // Skills load on profile screen
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
                                modifier = Modifier.size(IconSizes.Standard) // Material Design 3: 24dp
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
private fun HomeSectionsContent(
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
    userSkills: List<String> = emptyList()
) {
    // Memoize filtered jobs to avoid recomputation on every recomposition
    val availableJobs = remember(jobListings, jobVacancyStatuses) {
        jobListings.filter { job ->
            jobVacancyStatuses[job.jobId] != JobVacancyStatus.FILLED
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
    
    ScrollAwareLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        scrollStateManager = scrollStateManager
    ) {
        // Section 1: Browse Categories (at the top)
        item {
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
        
        // Section 2: Jobs For You (skill-matched)
        item {
            RecommendedJobsSection(
                jobs = skillMatchedJobs,
                onViewAllClick = { navController.navigate(Routes.allJobsRoute("All Jobs")) },
                savedJobsViewModel = savedJobsViewModel,
                onNavigateToJob = onNavigateToJob,
                sectionTitle = if (userSkills.isNotEmpty()) stringResource(R.string.jobs_for_you) else null
            )
        }
        
        // Section 3: DutyPe Promise Carousel (at the bottom after jobs)
        item {
            DutyPePromiseCarousel()
        }
    }
}

@Composable
private fun RecommendedJobsSection(
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
                    color = Color(0xFF1F2937),
                    fontSize = 17.sp
                )
            )
            
            // Arrow button - clean minimal style
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View All",
                tint = Color(0xFF6B7280),
                modifier = Modifier
                    .size(IconSizes.Standard) // Material Design 3: 24dp
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
                val jobId = job.jobId.ifEmpty { job.id }
                JobCard(
                    job = job,
                    isSaved = job.isSaved,
                    onSaveClick = {
                        if (job.isSaved) {
                            savedJobsViewModel.unsaveJob(jobId)
                        } else {
                            savedJobsViewModel.saveJob(jobId)
                        }
                    },
                    onCardClick = { onNavigateToJob(it) }
                )
            }
        }
    }
}

@Composable
private fun BrowseCategoriesSection(
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
                    color = Color(0xFF1F2937),
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
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View All Categories",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(IconSizes.Standard) // Material Design 3: 24dp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Categories Grid - 5 per row
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

private data class CategoryItem(
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
                color = WorkerColors.TextPrimary,
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
    // Clean white card on light gray background
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
private fun PromiseItemWithIcon(
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


//@Preview(showBackground = true)
//@Composable
//fun WorkerHomeScreenPreview() {
//    WorkerHomeScreen(
//        navController = NavController(LocalContext.current),
//        onStatusBarColorChange = {}
//    )
//}
