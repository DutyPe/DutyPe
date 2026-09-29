package com.example.dutype.worker.screens

import com.dutype.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.OfflineBanner
import com.example.dutype.viewmodels.ConnectivityViewModel
import com.example.dutype.navigation.Routes
import com.example.dutype.components.ReusableSearchBar
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.ComponentHeights
import com.example.dutype.components.JobCardShimmer
import com.example.dutype.viewmodels.AllJobsViewModel
import com.example.dutype.viewmodels.JobFilters
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.worker.components.WorkerHomeJobCard
import com.example.dutype.worker.components.JobCard
import com.example.dutype.utils.CategoryDetector
import com.example.dutype.components.CategoryIcon
import com.example.dutype.components.LocationAutocompleteField
import com.example.dutype.models.LocationData
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.dutype.models.JobListing
import com.example.dutype.repositories.LocationRepository
import com.example.dutype.viewmodels.AllJobsUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import com.example.dutype.components.EmptyLocationState
import com.example.dutype.components.EmptySearchState
import com.example.dutype.utils.findActivity
import com.example.dutype.components.openNotificationSettings

/**
 * AllJobsScreen - Displays all available jobs with infinite scroll
 * 
 * PAGINATION: 10 jobs per page
 * - Initial load: 10 jobs
 * - On scroll near end: Load 10 more from server
 * - Continues until all jobs are loaded
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */

// Industry standard pagination
private const val PAGE_SIZE = 10L

/** Start loading the next page when the user is this many items away from the end. */
private const val PREFETCH_DISTANCE = 5

/** When client-side filters hide every loaded job, keep auto-loading up to this many raw jobs. */
private const val AUTO_FILL_LIMIT = 100

private class JobsLocationPermission(
    val hasPermission: Boolean,
    val requestOrOpenSettings: () -> Unit
)

/**
 * Location permission state + launcher. A fresh fix is forwarded to the ViewModel, which
 * only re-sorts when the user moved more than ~500 m (and never clears the list).
 */
@Composable
private fun rememberJobsLocationPermission(
    viewModel: AllJobsViewModel,
    locationRepository: LocationRepository
): JobsLocationPermission {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasLocationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAttemptedLocationRequest by remember { mutableStateOf(false) }

    val refreshFix: () -> Unit = {
        coroutineScope.launch {
            locationRepository.refresh { freshLocation ->
                if (freshLocation != null) {
                    val data = viewModel.locationService.toLocationData(freshLocation)
                    viewModel.setUserLocation(data.latitude, data.longitude)
                }
            }
        }
    }
    val currentRefreshFix by rememberUpdatedState(refreshFix)

    // Automatically detect permission grant when returning to app
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (isGranted && !hasLocationPermission) {
                    hasLocationPermission = true
                    currentRefreshFix()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            currentRefreshFix()
        }
    }

    val requestOrOpenSettings: () -> Unit = {
        val activity = context.findActivity()
        val isPermanentlyDenied = activity != null && hasAttemptedLocationRequest &&
            !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) &&
            !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION)

        if (isPermanentlyDenied) {
            val isTelugu = com.example.dutype.utils.LocaleHelper.getLanguage(context) == com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU
            val message = if (isTelugu) "దయచేసి సెట్టింగ్స్‌లో లొకేషన్ అనుమతిని ఆన్ చేయండి" else "Please enable Location permission in App Settings"
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            openNotificationSettings(context)
        } else {
            hasAttemptedLocationRequest = true
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    return JobsLocationPermission(hasLocationPermission, requestOrOpenSettings)
}

/**
 * One-shot start-up work for the Jobs tab:
 *  1. apply the route filter (once per distinct value),
 *  2. use the last known location immediately,
 *  3. load page 1 (instant from cache when fresh),
 *  4. refine the location from GPS in the background (silent unless moved > 500 m).
 */
@Composable
private fun JobsScreenEffects(
    viewModel: AllJobsViewModel,
    locationRepository: LocationRepository,
    initialFilter: String,
    voiceQuery: String?,
    onStatusBarColorChange: (Color) -> Unit
) {
    LaunchedEffect(initialFilter) {
        onStatusBarColorChange(Color.White)
        viewModel.applyRouteFilter(initialFilter)

        val savedLocation = viewModel.locationPreferences.getSavedLocationIfFresh()
        if (savedLocation != null) {
            viewModel.setUserLocation(savedLocation.latitude, savedLocation.longitude)
        }
        viewModel.ensureLoaded()

        if (!voiceQuery.isNullOrBlank() && voiceQuery != viewModel.searchQuery.value) {
            viewModel.setSearchQuery(voiceQuery)
        }
    }

    LaunchedEffect(Unit) {
        val locationPreferences = viewModel.locationPreferences
        if (locationPreferences.isManualLocationLocked()) return@LaunchedEffect
        if (locationPreferences.isLocationFresh(5 * 60 * 1000L)) return@LaunchedEffect
        try {
            locationRepository.refresh { freshLocation ->
                if (freshLocation != null) {
                    val data = viewModel.locationService.toLocationData(freshLocation)
                    viewModel.setUserLocation(data.latitude, data.longitude)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to get fresh location")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllJobsScreen(
    navController: NavController,
    rootNavController: NavController? = null,
    initialFilter: String = "All Jobs",
    voiceQuery: String? = null,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val locationRepository = remember { com.example.dutype.di.locationRepositoryFromHilt(context) }
    val viewModel: AllJobsViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredJobs by viewModel.filteredJobs.collectAsStateWithLifecycle()
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val activeFilterCount by viewModel.activeFilterCount.collectAsStateWithLifecycle()
    val currentLocation by viewModel.locationPreferences.currentLocation.collectAsStateWithLifecycle()
    val correctedQuery by viewModel.correctedQuery.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }
    var showLocationPermissionBottomSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val locationPermission = rememberJobsLocationPermission(viewModel, locationRepository)
    JobsScreenEffects(viewModel, locationRepository, initialFilter, voiceQuery, onStatusBarColorChange)

    // Urgent-only now lives in JobFilters (filter sheet), so the VM output is final.
    val displayedJobs = filteredJobs

    // If client-side filters hide everything that is loaded, keep pulling pages
    // (nearest first) instead of showing a false empty state while more jobs exist.
    val canAutoFill = displayedJobs.isEmpty() && uiState.hasMore && !uiState.isLoading &&
        !uiState.isRefreshing && !uiState.isSyncing && !uiState.isLoadingMore &&
        !uiState.loadMoreFailed && uiState.jobs.size < AUTO_FILL_LIMIT
    LaunchedEffect(canAutoFill, uiState.jobs.size) {
        if (canAutoFill) viewModel.loadMoreJobs(PAGE_SIZE)
    }

    // A failed pull-to-refresh keeps the list; tell the user instead of blanking the screen.
    LaunchedEffect(uiState.error, uiState.hasError) {
        if (uiState.error != null && !uiState.hasError && uiState.jobs.isNotEmpty()) {
            android.widget.Toast.makeText(context, "Couldn't refresh jobs. Check your connection.", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
        val isOnline by connectivityViewModel.isOnline.collectAsState()
        OfflineBanner(isOffline = !isOnline)

        // Find Jobs is one of the bottom tabs, so no back chevron.
        CommonHeader(
            title = if (viewModel.isInitialFilterCategory()) "$initialFilter Jobs" else "Find Jobs",
            onBackClick = { navController.popBackStack() },
            showBackButton = false,
            backgroundColor = WorkerColors.CardBackground
        )

        JobLocationBar(
            locationText = currentLocation?.getShortAddress() ?: "Set your location",
            onClick = { showLocationSheet = true }
        )

        JobsSearchAndFilters(
            searchQuery = searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            activeFilterCount = activeFilterCount,
            correctedQuery = correctedQuery,
            onOpenFilters = { showFilterSheet = true }
        )

        // Always visible (also in empty/error states) so the user can switch category.
        JobCategoryRail(
            selectedChip = selectedChip,
            onCategoryClick = { viewModel.setCategoryAndReload(it) }
        )
        HorizontalDivider(color = WorkerColors.Border, thickness = 1.dp)

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshJobs() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            JobsContent(
                hasLocationAccess = locationPermission.hasPermission || currentLocation != null,
                uiState = uiState,
                jobs = displayedJobs,
                urgentOnly = filters.urgentOnly,
                searchQuery = searchQuery,
                selectedChip = selectedChip,
                locationName = currentLocation?.getShortAddress(),
                onRequestLocation = { showLocationPermissionBottomSheet = true },
                onRetry = { viewModel.retry() },
                onLoadMore = { viewModel.loadMoreJobs(PAGE_SIZE) },
                onShowAllJobs = { viewModel.showAllJobs() },
                onDisableUrgent = { viewModel.setFilters(filters.copy(urgentOnly = false)) },
                onClearSearch = { viewModel.setSearchQuery("") },
                onCitySuggestionClick = { chip ->
                    coroutineScope.launch {
                        val locData = com.example.dutype.location.TopCityChips.toLocationData(chip)
                        viewModel.locationPreferences.saveLocation(locData, forceManualOverride = true)
                        viewModel.onLocationChanged(chip.latitude, chip.longitude)
                    }
                },
                onHelpDesk = { navController.navigate(Routes.HELP) },
                onNavigateToJob = { jobId -> navController.navigate(Routes.jobDetailRoute(jobId)) },
                onSaveClick = { jobId, isSaved ->
                    if (isSaved) savedJobsViewModel.unsaveJob(jobId)
                    else savedJobsViewModel.saveJob(jobId)
                }
            )
        }
    }

    // Location picker sheet — search any area to re-sort jobs by nearest first.
    if (showLocationSheet) {
        JobLocationPickerSheet(
            locationService = viewModel.locationService,
            onDismiss = { showLocationSheet = false },
            onLocationSelected = { address, lat, lon ->
                val data = LocationData(
                    latitude = lat,
                    longitude = lon,
                    city = null,
                    address = address,
                    area = address.split(",").firstOrNull()?.trim()
                )
                viewModel.locationPreferences.saveLocation(data, forceManualOverride = true)
                viewModel.onLocationChanged(lat, lon)
                showLocationSheet = false
                android.widget.Toast.makeText(context, "Showing jobs near $address", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Location Permission Bottom Sheet
    com.example.dutype.components.LocationPermissionBottomSheet(
        isVisible = showLocationPermissionBottomSheet,
        onDismiss = { showLocationPermissionBottomSheet = false },
        onAllowLocation = {
            showLocationPermissionBottomSheet = false
            locationPermission.requestOrOpenSettings()
        }
    )

    // Filter Bottom Sheet
    if (showFilterSheet) {
        JobFilterBottomSheet(
            filters = filters,
            onDismiss = { showFilterSheet = false },
            onApplyFilters = { newFilters ->
                viewModel.setFilters(newFilters)
                showFilterSheet = false
            },
            onResetFilters = { viewModel.resetFilters() }
        )
    }
}

/**
 * Search bar + filter button. Sort, distance, salary and "urgent only" live in the
 * filter sheet (the filter button shows how many are active). No job count here.
 */
@Composable
private fun JobsSearchAndFilters(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    activeFilterCount: Int,
    correctedQuery: String?,
    onOpenFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WorkerColors.CardBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ReusableSearchBar(
                    query = searchQuery,
                    onQueryChange = onQueryChange,
                    placeholder = "Search job, company or area...",
                    height = 48,
                    backgroundColor = WorkerColors.CardBackground,
                    borderColor = WorkerColors.Border,
                    focusedBorderColor = WorkerColors.Primary,
                    searchIconColor = WorkerColors.IconSecondary,
                    textColor = WorkerColors.TextPrimary,
                    placeholderColor = WorkerColors.TextTertiary,
                    cornerRadius = 24,
                    fontSize = 14,
                    showShadow = false
                )
            }

            Box(
                modifier = Modifier
                    .size(ComponentHeights.MinimumTouchTarget)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeFilterCount > 0) WorkerColors.Primary else WorkerColors.ChipBackground)
                    .clickable(onClick = onOpenFilters),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "Filter",
                    tint = if (activeFilterCount > 0) Color.White else WorkerColors.IconPrimary,
                    modifier = Modifier.size(IconSizes.Standard)
                )
                if (activeFilterCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(18.dp)
                            .background(WorkerColors.Error, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$activeFilterCount",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Typo-tolerant search: tell the worker what was actually searched.
        if (!correctedQuery.isNullOrBlank() && searchQuery.trim().length >= 2) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Showing results for \u201C$correctedQuery\u201D",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = WorkerColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/** Category rail. Selecting a tile refetches from the server with that category. */
@Composable
private fun JobCategoryRail(
    selectedChip: String,
    onCategoryClick: (String) -> Unit
) {
    val categoryTabs = remember {
        val allCategories = listOf("All Jobs" to "")
        val jobCategories = com.example.dutype.employer.models.JobCategory.entries
            .filter { it != com.example.dutype.employer.models.JobCategory.OTHER }
            .map { it.displayName to it.icon }
        allCategories + jobCategories
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(WorkerColors.CardBackground)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp)
    ) {
        items(
            items = categoryTabs,
            key = { (label, _) -> "cat_tab_$label" },
            contentType = { "category_chip" }
        ) { (label, emoji) ->
            val isSelected = selectedChip == label ||
                (selectedChip == "Any" && label == "All Jobs") ||
                (selectedChip == "All" && label == "All Jobs")
            JobCategoryTile(
                label = label,
                emoji = emoji,
                isSelected = isSelected,
                onClick = { onCategoryClick(label) }
            )
        }
    }
}

private val JobCategoryPalette = listOf(
    Color(0xFFF5F3FF) to Color(0xFF7C3AED),
    Color(0xFFECFEFF) to Color(0xFF0891B2),
    Color(0xFFFFF7ED) to Color(0xFFEA580C),
    Color(0xFFFDF2F8) to Color(0xFFDB2777),
    Color(0xFFF1F5F9) to Color(0xFF0F172A)
)

private fun jobCategoryTint(label: String): Pair<Color, Color> = when (label) {
    "All Jobs" -> Color(0xFFF1F5F9) to Color(0xFF0F172A)
    "Electrician" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
    "Plumber" -> Color(0xFFEFF6FF) to Color(0xFF2563EB)
    "Driver" -> Color(0xFFF0FDF4) to Color(0xFF16A34A)
    "Cook" -> Color(0xFFFEF2F2) to Color(0xFFDC2626)
    else -> JobCategoryPalette[(label.hashCode() and 0x7fffffff) % JobCategoryPalette.size]
}

private fun jobCategoryIcon(label: String): ImageVector? = when (label) {
    "All Jobs" -> Icons.Default.Apps
    "Electrician" -> Icons.Default.Bolt
    "Plumber" -> Icons.Default.Build
    "Driver" -> Icons.Default.LocalShipping
    "Cook" -> Icons.Default.Restaurant
    else -> null
}

@Composable
private fun JobCategoryTile(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (container, tint) = jobCategoryTint(label)
    val tileShape = RoundedCornerShape(18.dp)
    val icon = jobCategoryIcon(label)
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(container, tileShape)
                .then(
                    if (isSelected) Modifier.border(2.dp, Color(0xFF0F0F0F), tileShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Text(text = emoji, fontSize = 26.sp)
            }
        }
        Text(
            text = if (label == "All Jobs") "All" else label,
            color = Color(0xFF0F172A),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

/**
 * Wraps a non-scrolling state (error / empty / permission) in a full-size lazy list so
 * pull-to-refresh still works there.
 */
@Composable
private fun PullableFill(content: @Composable () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "pullable_fill") {
            Box(modifier = Modifier.fillParentMaxSize()) {
                content()
            }
        }
    }
}

@Composable
private fun JobsShimmerList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) { JobCardShimmer() }
    }
}

/** Chooses between permission prompt, shimmer, error, empty and the job list. */
@Composable
private fun JobsContent(
    hasLocationAccess: Boolean,
    uiState: AllJobsUiState,
    jobs: List<JobListing>,
    urgentOnly: Boolean,
    searchQuery: String,
    selectedChip: String,
    locationName: String?,
    onRequestLocation: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onShowAllJobs: () -> Unit,
    onDisableUrgent: () -> Unit,
    onClearSearch: () -> Unit,
    onCitySuggestionClick: (com.example.dutype.location.TopCityChips.CityLocationChip) -> Unit,
    onHelpDesk: () -> Unit,
    onNavigateToJob: (String) -> Unit,
    onSaveClick: (String, Boolean) -> Unit
) {
    // Shimmer while the first page is in flight, or while filters hide everything loaded
    // and the auto-fill effect is fetching the next (nearest) page.
    val waitingForData = jobs.isEmpty() && (
        uiState.isLoading || uiState.isRefreshing || uiState.isSyncing ||
            (uiState.hasMore && !uiState.loadMoreFailed && uiState.jobs.size < AUTO_FILL_LIMIT)
        )

    when {
        !hasLocationAccess -> PullableFill {
            com.example.dutype.components.LocationPermissionRequiredState(
                onRequestPermissionClick = onRequestLocation
            )
        }

        uiState.hasError && uiState.jobs.isEmpty() -> PullableFill {
            ErrorState(error = uiState.error, onRetry = onRetry)
        }

        waitingForData -> JobsShimmerList()

        jobs.isEmpty() && uiState.loadMoreFailed -> PullableFill {
            ErrorState(error = null, onRetry = onLoadMore)
        }

        jobs.isEmpty() && urgentOnly -> PullableFill {
            UrgentEmptyState(onShowAll = onDisableUrgent)
        }

        jobs.isEmpty() -> PullableFill {
            EmptyState(
                searchQuery = searchQuery,
                selectedChip = selectedChip,
                locationName = locationName,
                onViewAllJobs = onShowAllJobs,
                onClearSearch = onClearSearch,
                onCitySuggestionClick = onCitySuggestionClick,
                onHelpDesk = onHelpDesk
            )
        }

        else -> JobsList(
            jobs = jobs,
            uiState = uiState,
            onLoadMore = onLoadMore,
            onNavigateToJob = onNavigateToJob,
            onSaveClick = onSaveClick
        )
    }
}

private data class LoadMoreInputs(
    val itemCount: Int,
    val hasMore: Boolean,
    val busy: Boolean,
    val failed: Boolean
)

private fun jobListKey(job: JobListing): String =
    job.id.ifBlank { job.jobId.ifBlank { "${job.employerId}:${job.title}:${job.createdAt}" } }

/**
 * Infinite list. The next page is requested while the user is still
 * [PREFETCH_DISTANCE] items away from the end, so scrolling never hits a wall. Item keys
 * are the job ids, so appended pages / silent refreshes keep the scroll position.
 */
@Composable
private fun JobsList(
    jobs: List<JobListing>,
    uiState: AllJobsUiState,
    onLoadMore: () -> Unit,
    onNavigateToJob: (String) -> Unit,
    onSaveClick: (String, Boolean) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val inputs by rememberUpdatedState(
        LoadMoreInputs(
            itemCount = jobs.size,
            hasMore = uiState.hasMore,
            busy = uiState.isLoadingMore || uiState.isLoading || uiState.isRefreshing || uiState.isSyncing,
            failed = uiState.loadMoreFailed
        )
    )

    LaunchedEffect(listState) {
        snapshotFlow {
            val current = inputs
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            current.hasMore && !current.busy && !current.failed &&
                current.itemCount > 0 &&
                lastVisible >= current.itemCount - 1 - PREFETCH_DISTANCE
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { currentOnLoadMore() }
    }

    val showJumpToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= 10 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = jobs,
                key = { job -> jobListKey(job) },
                contentType = { "job_card" }
            ) { job ->
                WorkerHomeJobCard(
                    job = job,
                    onCardClick = { onNavigateToJob(it) }
                )
            }

            if (uiState.hasMore || uiState.loadMoreFailed) {
                item(key = "alljobs_load_more_footer", contentType = "footer") {
                    LoadMoreFooter(failed = uiState.loadMoreFailed, onRetry = onLoadMore)
                }
            } else {
                item(key = "alljobs_end_of_list", contentType = "end") {
                    EndOfListRow()
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showJumpToTop,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp)
        ) {
            FloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                containerColor = WorkerColors.Primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Jump to Top",
                    modifier = Modifier.size(IconSizes.Standard)
                )
            }
        }
    }
}

@Composable
private fun LoadMoreFooter(failed: Boolean, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (failed) {
            TextButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizes.Small)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Couldn't load more jobs. Tap to retry")
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSizes.Standard),
                color = WorkerColors.TextPrimary,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun EndOfListRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "You're all caught up. That's every job near you.",
            style = AppTypography.bodySmall.copy(
                color = WorkerColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun UrgentEmptyState(onShowAll: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "No urgent jobs right now",
                style = AppTypography.emptyStateTitle.copy(color = WorkerColors.TextPrimary)
            )
            Text(
                text = "Employers haven't marked any nearby job as urgent. Show all jobs instead.",
                style = AppTypography.emptyStateSubtitle.copy(
                    color = WorkerColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            )
            Button(
                onClick = onShowAll,
                colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Show all jobs")
            }
        }
    }
}

/**
 * Location bar shown under the header. Surfaces the area jobs are sorted around
 * (previously invisible) and lets the worker change it.
 */
@Composable
private fun JobLocationBar(
    locationText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WorkerColors.CardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (com.example.dutype.ui.theme.isAppInDarkTheme()) Color.White.copy(alpha = 0.15f) else WorkerColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (com.example.dutype.ui.theme.isAppInDarkTheme()) Color.White else WorkerColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.auto_showing_jobs_near),
                style = AppTypography.labelSmall.copy(color = WorkerColors.TextTertiary)
            )
            Text(
                text = locationText,
                style = AppTypography.bodyMedium.copy(
                    color = WorkerColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(WorkerColors.Primary.copy(alpha = 0.10f))
                .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_change),
                style = AppTypography.labelMedium.copy(
                    color = WorkerColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = WorkerColors.Primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Bottom sheet to pick a job-search location. Reuses the proven
 * [LocationAutocompleteField]; on selection the caller re-sorts jobs nearest
 * first via the ViewModel and persists the manual location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobLocationPickerSheet(
    locationService: com.example.dutype.utils.LocationService,
    onDismiss: () -> Unit,
    onLocationSelected: (address: String, latitude: Double, longitude: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WorkerColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_choose_job_location),
                style = AppTypography.pageTitle.copy(
                    color = WorkerColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = stringResource(R.string.auto_search_any_area_city_or_locality_to_see_th),
                style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary)
            )
            LocationAutocompleteField(
                value = query,
                onValueChange = { query = it },
                onLocationSelected = onLocationSelected,
                locationService = locationService,
                label = "Search location",
                placeholder = "e.g. Hitech City, Hyderabad",
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}


/**
 * P1 PERFORMANCE FIX: Extracted ErrorState composable
 */
@Composable
private fun ErrorState(
    error: String?,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(WorkerColors.ErrorLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = WorkerColors.Error,
                    modifier = Modifier.size(IconSizes.Large) // Material Design 3: 36dp
                )
            }
            Text(
                text = stringResource(R.string.jobs_something_went_wrong),
                style = AppTypography.emptyStateTitle.copy(color = WorkerColors.TextPrimary)
            )
            Text(
                text = error ?: stringResource(R.string.jobs_unable_to_load),
                style = AppTypography.emptyStateSubtitle.copy(
                    color = WorkerColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizes.Small) // Material Design 3: 20dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.try_again))
            }
        }
    }
}

/**
 * P1 PERFORMANCE FIX: Extracted EmptyState composable
 */
@Composable
private fun EmptyState(
    searchQuery: String,
    selectedChip: String,
    locationName: String? = null,
    onViewAllJobs: () -> Unit,
    onClearSearch: () -> Unit = {},
    onCitySuggestionClick: (com.example.dutype.location.TopCityChips.CityLocationChip) -> Unit = {},
    onHelpDesk: (() -> Unit)? = null
) {
    if (searchQuery.isNotBlank()) {
        // Show search empty state for search queries
        EmptySearchState(
            searchQuery = searchQuery,
            onClearSearch = onClearSearch
        )
    } else {
        // Show location empty state for location misses
        EmptyLocationState(
            categoryFilter = selectedChip,
            locationName = locationName,
            onCitySuggestionClick = onCitySuggestionClick,
            onHelpDesk = onHelpDesk
        )
    }
}


/**
 * Filter Bottom Sheet - Advanced job filtering options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobFilterBottomSheet(
    filters: JobFilters,
    onDismiss: () -> Unit,
    onApplyFilters: (JobFilters) -> Unit,
    onResetFilters: () -> Unit
) {
    var salaryMin by remember { mutableStateOf(filters.salaryMin) }
    var salaryMax by remember { mutableStateOf(filters.salaryMax) }
    var maxDistance by remember { mutableStateOf(filters.maxDistance) }
    var experienceLevel by remember { mutableStateOf(filters.experienceLevel) }
    var sortBy by remember { mutableStateOf(filters.sortBy) }
    var payType by remember { mutableStateOf(filters.payType) }
    var workType by remember { mutableStateOf(filters.workType) }
    var category by remember { mutableStateOf(filters.category) }
    var shiftTiming by remember { mutableStateOf(filters.shiftTiming) }
    var urgentOnly by remember { mutableStateOf(filters.urgentOnly) }
    
    val experienceOptions = listOf("Any", "Fresher", "1-3 years", "3-5 years", "5+ years")
    val sortOptions = listOf("Relevance", "Distance", "Newest", "Salary: High to Low", "Salary: Low to High")
    val payTypeOptions = listOf("Any", "DAILY", "HOURLY", "MONTHLY")
    val workTypeOptions = listOf("Any", "Part-time", "Full-time", "Contract", "Temporary")
    val shiftTimingOptions = listOf("Any", "Morning", "Afternoon", "Evening", "Night", "Flexible")
    val categoryOptions = remember { listOf("Any") + com.example.dutype.utils.CategoryDetector.getAllCategories() }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.jobs_filter_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                    )
                )
                TextButton(
                    onClick = {
                        salaryMin = 0
                        salaryMax = 100000
                        maxDistance = null
                        experienceLevel = "Any"
                        sortBy = "Relevance"
                        payType = "Any"
                        workType = "Any"
                        category = "Any"
                        shiftTiming = "Any"
                        urgentOnly = false
                        onResetFilters()
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(stringResource(R.string.reset), color = WorkerColors.Error, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Urgent jobs only (moved here from the pills under the search bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(WorkerColors.ChipBackground)
                    .clickable { urgentOnly = !urgentOnly }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Urgent jobs only",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)
                    )
                    Text(
                        text = "Employers who need someone today",
                        style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary)
                    )
                }
                Switch(
                    checked = urgentOnly,
                    onCheckedChange = { urgentOnly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WorkerColors.Primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sort By
            Text(
                text = stringResource(R.string.jobs_sort_by),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sortOptions) { option ->
                    FilterChip(
                        onClick = { sortBy = option },
                        label = { Text(option, fontSize = 13.sp) },
                        selected = sortBy == option,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WorkerColors.Primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Job Type (Daily/Hourly)
            Text(
                text = stringResource(R.string.jobs_pay_type),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(payTypeOptions) { option ->
                    val displayOption = if (option == "Any") "All Types" else option.lowercase().capitalize()
                    FilterChip(
                        onClick = { payType = option },
                        label = { Text(displayOption, fontSize = 13.sp) },
                        selected = payType == option,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WorkerColors.Primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auto_location_radius),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)
                )
                Text(
                    text = if (maxDistance == null) "Everywhere" else "Within ${maxDistance!!.toInt()} km",
                    style = MaterialTheme.typography.labelMedium.copy(color = WorkerColors.Primary, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            val distanceOptions = listOf(null, 5f, 10f, 25f, 50f)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(distanceOptions) { distance ->
                    FilterChip(
                        onClick = { maxDistance = distance },
                        label = { 
                            Text(if (distance == null) "Anywhere" else "${distance.toInt()} km", fontSize = 13.sp) 
                        },
                        selected = maxDistance == distance,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WorkerColors.Primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
            if (maxDistance != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = maxDistance!!,
                    onValueChange = { maxDistance = it },
                    valueRange = 1f..50f,
                    steps = 49,
                    colors = SliderDefaults.colors(
                        thumbColor = WorkerColors.Primary,
                        activeTrackColor = WorkerColors.Primary,
                        inactiveTrackColor = WorkerColors.Border
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Work Type & Shift
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.post_job_work_type),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(workTypeOptions) { option ->
                            FilterChip(
                                onClick = { workType = option },
                                label = { Text(option, fontSize = 13.sp) },
                                selected = workType == option,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WorkerColors.Primary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Apply Button
            Button(
                onClick = {
                    onApplyFilters(
                        JobFilters(
                            salaryMin = salaryMin,
                            salaryMax = salaryMax,
                            maxDistance = maxDistance,
                            experienceLevel = experienceLevel,
                            sortBy = sortBy,
                            payType = payType,
                            workType = workType,
                            category = category, // Kept for data integrity, though hidden in UI
                            shiftTiming = shiftTiming,
                            urgentOnly = urgentOnly
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.auto_apply_filters),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
