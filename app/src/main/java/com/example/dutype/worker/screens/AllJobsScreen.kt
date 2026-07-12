package com.example.dutype.worker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.dutype.worker.components.JobCard
import com.example.dutype.utils.CategoryDetector
import com.example.dutype.components.CategoryIcon
import com.example.dutype.components.LocationAutocompleteField
import com.example.dutype.models.LocationData
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import timber.log.Timber
import com.dutype.app.R
import com.example.dutype.components.EmptyLocationState
import com.example.dutype.components.EmptySearchState

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
    
    // P0 FIX: Use dedicated AllJobsViewModel with filtering in ViewModel
    val viewModel: AllJobsViewModel = hiltViewModel()
    
    // Collect state from ViewModel using lifecycle-aware collection
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredJobs by viewModel.filteredJobs.collectAsStateWithLifecycle()
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val activeFilterCount by viewModel.activeFilterCount.collectAsStateWithLifecycle()
    val visibleCategory = remember(filters.category, uiState.initialCategory) {
        when {
            filters.category != "Any" -> filters.category
            !uiState.initialCategory.isNullOrBlank() -> uiState.initialCategory ?: "All"
            else -> "All"
        }
    }
    // Local UI state
    var showFilterSheet by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }
    val currentLocation by viewModel.locationPreferences.currentLocation.collectAsStateWithLifecycle()
    
    // Pagination: 10 jobs per page
    val pageSize = PAGE_SIZE
    
    // Set status bar color and initialize ViewModel
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        
        // CRITICAL: Set initial category FIRST
        val categoryForQuery = initialFilter.takeIf { it != "All Jobs" }
        viewModel.setInitialCategory(categoryForQuery)
        
        // 🚀 UBER/SWIGGY STRATEGY: Get location fast and load jobs in parallel
        val locationPreferences = viewModel.locationPreferences
        val savedLocation = locationPreferences.getSavedLocationIfFresh()
        
        if (savedLocation != null) {
            Timber.d("📍 AllJobsScreen: Using cached location - lat=${savedLocation.latitude}, lon=${savedLocation.longitude}")
            viewModel.setUserLocation(savedLocation.latitude, savedLocation.longitude)
        }
        
        // Load jobs immediately (don't wait for location)
        Timber.d("📍 AllJobsScreen: Loading jobs with category: $categoryForQuery")
        viewModel.loadJobs(limit = PAGE_SIZE, category = categoryForQuery)
        
        // Get fresh location in background to update distances
        if (!locationPreferences.isManualLocationLocked()) {
            launch {
                try {
                    // Fetch only when cache is stale to avoid repeated GPS calls.
                    if (!locationPreferences.isLocationFresh(5 * 60 * 1000L)) {
                        val locationService = viewModel.locationService
                        locationRepository.refresh { freshLocation ->
                            if (freshLocation != null) {
                                Timber.d("📍 AllJobsScreen: Fresh location received - updating distances")
                                // Location already saved by getLocationFast()
                                val data = locationService.toLocationData(freshLocation)
                                viewModel.setUserLocation(data.latitude, data.longitude)
                            }
                        }
                    } else {
                        Timber.d("📍 AllJobsScreen: Skipping GPS fetch - using fresh cached location")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get fresh location")
                }
            }
        } else {
            Timber.d("📍 AllJobsScreen: Manual location lock active - skipping background GPS refresh")
        }
        
        // Auto-search with voice query if provided
        if (!voiceQuery.isNullOrBlank()) {
            timber.log.Timber.d("🎤 Voice query received: $voiceQuery")
            timber.log.Timber.d("🎤 Setting search query in ViewModel...")
            viewModel.setSearchQuery(voiceQuery)
            timber.log.Timber.d("🎤 Search query set successfully")
        }
    }
    
    // Debug: Log search query changes
    LaunchedEffect(searchQuery) {
        timber.log.Timber.d("🔍 Search query in UI: '$searchQuery'")
        timber.log.Timber.d("🔍 Filtered jobs count: ${filteredJobs.size}")
    }
    
    val categoryTabs = remember {
        val allCategories = listOf("All Jobs" to "📋")
        val jobCategories = com.example.dutype.employer.models.JobCategory.entries
            .filter { it != com.example.dutype.employer.models.JobCategory.OTHER }
            .map { it.displayName to it.icon }
        allCategories + jobCategories
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        // Offline banner at the very top
        val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
        val isOnline by connectivityViewModel.isOnline.collectAsState()
        OfflineBanner(isOffline = !isOnline)
        
        // Common Header
        CommonHeader(
            title = if (viewModel.isInitialFilterCategory()) "$initialFilter Jobs" else "All Jobs",
            onBackClick = { navController.popBackStack() },
            backgroundColor = WorkerColors.CardBackground
        )

        // Location bar — shows the area jobs are sorted around; tap to change.
        JobLocationBar(
            locationText = currentLocation?.getShortAddress() ?: "Set your location",
            onClick = { showLocationSheet = true }
        )

        // Search and Filter Section
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
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        placeholder = stringResource(R.string.search_jobs_companies),
                        height = 48,
                        backgroundColor = WorkerColors.ChipBackground,
                        borderColor = Color.Transparent,
                        focusedBorderColor = WorkerColors.Primary,
                        searchIconColor = WorkerColors.IconSecondary,
                        textColor = WorkerColors.TextPrimary,
                        placeholderColor = WorkerColors.TextTertiary,
                        cornerRadius = 12,
                        fontSize = 14
                    )
                }
                
                // Filter button with badge
                Box(
                    modifier = Modifier
                        .size(ComponentHeights.MinimumTouchTarget) // Material Design 3: 48dp touch target
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activeFilterCount > 0) WorkerColors.Primary else WorkerColors.ChipBackground)
                        .clickable { showFilterSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = "Filter",
                        tint = if (activeFilterCount > 0) Color.White else WorkerColors.IconPrimary,
                        modifier = Modifier.size(IconSizes.Standard) // Material Design 3: 24dp
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
        }
        // Category rail section
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(WorkerColors.CardBackground)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(categoryTabs) { (label, emoji) ->
                val isSelected = selectedChip == label || 
                                 (selectedChip == "Any" && label == "All Jobs") ||
                                 (selectedChip == "All" && label == "All Jobs")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(64.dp)
                        .clickable { viewModel.setCategoryAndReload(label) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = if (isSelected) {
                                    if (com.example.dutype.ui.theme.isAppInDarkTheme()) Color.White.copy(alpha = 0.15f) else WorkerColors.Primary.copy(alpha = 0.15f)
                                } else {
                                    if (com.example.dutype.ui.theme.isAppInDarkTheme()) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (label == "All Jobs") "All" else label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) WorkerColors.Primary else WorkerColors.TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }


        
        HorizontalDivider(color = WorkerColors.Border, thickness = 1.dp)

        // Job list content
        when {
            uiState.isLoading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(6) { JobCardShimmer() }
                }
            }
            
            uiState.hasError -> {
                ErrorState(
                    error = uiState.error,
                    onRetry = { 
                        val categoryForQuery = initialFilter.takeIf { it != "All Jobs" }
                        viewModel.loadJobs(category = categoryForQuery) 
                    }
                )
            }
            
            filteredJobs.isEmpty() && !uiState.isLoading -> {
                EmptyState(
                    searchQuery = searchQuery,
                    selectedChip = selectedChip,
                    onViewAllJobs = { viewModel.setSelectedChip("All Jobs") },
                    onClearSearch = { viewModel.setSearchQuery("") }
                )
            }
            
            else -> {
                JobsList(
                    jobs = filteredJobs,
                    uiState = uiState,
                    pageSize = pageSize,
                    onLoadMore = { 
                        val categoryForQuery = initialFilter.takeIf { it != "All Jobs" }
                        viewModel.loadMoreJobs(pageSize, categoryForQuery) 
                    },
                    onNavigateToJob = { jobId -> navController.navigate(Routes.jobDetailRoute(jobId)) },
                    onSaveClick = { jobId, isSaved ->
                        if (isSaved) savedJobsViewModel.unsaveJob(jobId)
                        else savedJobsViewModel.saveJob(jobId)
                    }
                )
            }
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
                viewModel.setUserLocation(lat, lon)
                showLocationSheet = false
                android.widget.Toast.makeText(context, "Showing jobs near $address", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

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

@Composable
private fun CategoryQuickFilterSection(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = remember { listOf("All") + CategoryDetector.getAllCategories() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WorkerColors.CardBackground)
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.filter_by_category),
            style = AppTypography.sectionHeader.copy(
                color = WorkerColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory.equals(category, ignoreCase = true)
                CategoryPill(
                    label = category,
                    icon = CategoryIcon.forDisplayName(category),
                    selected = isSelected,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

/**
 * Icon + label category chip used in the category browser. Selected state uses
 * the role primary colour with white content (readable in light and dark).
 */
@Composable
private fun CategoryPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) WorkerColors.Primary else WorkerColors.ChipBackground
    val foreground = if (selected) Color.White else WorkerColors.TextSecondary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = AppTypography.labelLarge.copy(
                color = foreground,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        )
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
                text = "Showing jobs near",
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
                text = "Change",
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
                text = "Choose job location",
                style = AppTypography.pageTitle.copy(
                    color = WorkerColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Search any area, city or locality to see the nearest jobs there.",
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
 * P1 PERFORMANCE FIX: Extracted JobsList composable
 * Reduces recomposition scope - only this component recomposes when jobs change
 * Uses regular JobCard - ad shows on back from JobDescriptionScreen
 * 
 * SMOOTH INFINITE SCROLL: Loads 10 jobs at a time when user scrolls near end
 */
@Composable
private fun JobsList(
    jobs: List<com.example.dutype.models.JobListing>,
    uiState: com.example.dutype.viewmodels.AllJobsUiState,
    pageSize: Long,
    onLoadMore: () -> Unit,
    onNavigateToJob: (String) -> Unit,
    onSaveClick: (String, Boolean) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var lastLoadTriggerToken by remember { mutableStateOf<String?>(null) }
    val shouldLoadMore by remember(jobs.size, uiState.hasMore, uiState.isLoading, uiState.isLoadingMore) {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            jobs.isNotEmpty() &&
                listState.isScrollInProgress &&
                uiState.hasMore &&
                !uiState.isLoading &&
                !uiState.isLoadingMore &&
                lastVisibleItemIndex >= jobs.lastIndex
        }
    }

    LaunchedEffect(shouldLoadMore, uiState.lastDocumentId, jobs.size) {
        if (shouldLoadMore) {
            val nextLoadToken = "${uiState.lastDocumentId ?: "null"}:${jobs.size}"
            if (nextLoadToken != lastLoadTriggerToken) {
                lastLoadTriggerToken = nextLoadToken
                Timber.d("📦 AllJobs: user reached last visible job, requesting next page (token=$nextLoadToken)")
                onLoadMore()
            }
        }
    }
    
    // Show "Jump to Top" button after loading 300+ jobs (LinkedIn approach)
    // LinkedIn shows it earlier for better UX
    val showJumpToTop by remember {
        derivedStateOf { jobs.size >= 300 }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = jobs,
                key = { job -> "alljobs_${job.id}" } // CRITICAL FIX: Add context prefix
            ) { job ->
                JobCard(
                    job = job,
                    isSaved = job.isSaved,
                    onSaveClick = { jobId ->
                        onSaveClick(jobId, job.isSaved)
                    },
                    onCardClick = { onNavigateToJob(it) }
                )
            }
            
            if (uiState.hasMore && jobs.isNotEmpty()) {
                item(key = "alljobs_load_more_sentinel") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(IconSizes.Standard), // Material Design 3: 24dp
                                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.jobs_loading_more),
                                style = AppTypography.bodySmall.copy(color = WorkerColors.TextSecondary)
                            )
                        }
                    }
                }
            }
        }
        
        // ENTERPRISE FEATURE: Jump to Top FAB (LinkedIn's exact approach)
        // Shows after loading 300+ jobs for easy navigation back to top
        // LinkedIn shows it earlier than competitors for better UX
        androidx.compose.animation.AnimatedVisibility(
            visible = showJumpToTop,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 80.dp) // Above bottom nav
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                        Timber.d("📦 Jumped to top - ${jobs.size} jobs loaded")
                    }
                },
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
    onViewAllJobs: () -> Unit,
    onClearSearch: () -> Unit = {}
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
            categoryFilter = selectedChip
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
    
    val experienceOptions = listOf("Any", "Fresher", "1-3 years", "3-5 years", "5+ years")
    val sortOptions = listOf("Relevance", "Newest", "Salary: High to Low", "Salary: Low to High", "Distance")
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
                        onResetFilters()
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(stringResource(R.string.reset), color = WorkerColors.Error, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
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
                    text = "Location Radius",
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
                            shiftTiming = shiftTiming
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
                    text = "Apply Filters",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
