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
    // Local UI state
    var showFilterSheet by remember { mutableStateOf(false) }
    
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
    
    // Filter chips
    val filterChips = listOf(
        "All Jobs" to Icons.Default.Star,
        "Daily Jobs" to Icons.Default.CalendarToday,
        "Hourly Jobs" to Icons.Default.AccessTime,
        "Nearby" to Icons.Default.LocationOn,
        "Part Time" to Icons.Default.Work,
        "Full Time" to Icons.Default.CheckCircle
    )
    
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
                        backgroundColor = Color(0xFFF1F5F9),
                        borderColor = Color.Transparent,
                        focusedBorderColor = Color(0xFF1F2937),
                        searchIconColor = Color(0xFF6B7280),
                        textColor = Color(0xFF1F2937),
                        placeholderColor = Color(0xFF9CA3AF),
                        cornerRadius = 12,
                        fontSize = 14
                    )
                }
                
                // Filter button with badge
                Box(
                    modifier = Modifier
                        .size(ComponentHeights.MinimumTouchTarget) // Material Design 3: 48dp touch target
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activeFilterCount > 0) Color(0xFF1F2937) else Color(0xFFF1F5F9))
                        .clickable { showFilterSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = "Filter",
                        tint = if (activeFilterCount > 0) Color.White else Color(0xFF374151),
                        modifier = Modifier.size(IconSizes.Standard) // Material Design 3: 24dp
                    )
                    if (activeFilterCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(18.dp)
                                .background(Color(0xFFEF4444), CircleShape),
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
        
        // Filter chips section
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(WorkerColors.CardBackground)
                .padding(bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterChips) { (chip, icon) ->
                FilterChip(
                    onClick = { viewModel.setSelectedChip(chip) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(IconSizes.Small), // Material Design 3: 20dp
                                tint = if (selectedChip == chip) Color.White else Color(0xFF374151)
                            )
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedChip == chip) Color.White else Color(0xFF374151)
                                )
                            )
                        }
                    },
                    selected = selectedChip == chip,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1F2937),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color(0xFF374151)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedChip == chip,
                        borderColor = Color(0xFFE5E7EB),
                        selectedBorderColor = Color(0xFF1F2937),
                        borderWidth = 1.dp
                    )
                )
            }
        }
        
        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)

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
                                color = Color(0xFF1F2937),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Loading more jobs...",
                                style = AppTypography.bodySmall.copy(color = Color(0xFF6B7280))
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
                containerColor = Color(0xFF1F2937),
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
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(IconSizes.Large) // Material Design 3: 36dp
                )
            }
            Text(
                text = "Something went wrong",
                style = AppTypography.emptyStateTitle.copy(color = Color(0xFF374151))
            )
            Text(
                text = error ?: "Unable to load jobs. Please try again.",
                style = AppTypography.emptyStateSubtitle.copy(
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
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
    
    val experienceOptions = listOf("Any", "Fresher", "1-2 years", "2-5 years", "5+ years")
    val sortOptions = listOf("Relevance", "Newest", "Salary: High to Low", "Salary: Low to High", "Distance")
    val payTypeOptions = listOf("Any", "DAILY", "HOURLY", "MONTHLY")
    val workTypeOptions = listOf("Any", "Part-time", "Full-time", "Contract", "Temporary")
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Jobs",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                TextButton(onClick = {
                    salaryMin = 0
                    salaryMax = 100000
                    maxDistance = null
                    experienceLevel = "Any"
                    sortBy = "Relevance"
                    payType = "Any"
                    workType = "Any"
                    onResetFilters()
                }) {
                    Text(stringResource(R.string.reset), color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sort, salary, distance, experience and work type",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Sort By
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pay Type",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(payTypeOptions) { option ->
                        FilterChip(
                            onClick = { payType = option },
                            label = { Text(option, fontSize = 13.sp) },
                            selected = payType == option,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1F2937),
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Work Type",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(workTypeOptions) { option ->
                        FilterChip(
                            onClick = { workType = option },
                            label = { Text(option, fontSize = 13.sp) },
                            selected = workType == option,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1F2937),
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sortOptions) { option ->
                    FilterChip(
                        onClick = { sortBy = option },
                        label = { Text(option, fontSize = 13.sp) },
                        selected = sortBy == option,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1F2937),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Salary Range
            Text(
                text = "Salary Range",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹${salaryMin} - ₹${if (salaryMax >= 100000) "1L+" else salaryMax}",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
            )
            Spacer(modifier = Modifier.height(8.dp))
            RangeSlider(
                value = salaryMin.toFloat()..salaryMax.toFloat(),
                onValueChange = { range ->
                    salaryMin = range.start.toInt()
                    salaryMax = range.endInclusive.toInt()
                },
                valueRange = 0f..100000f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF1F2937),
                    activeTrackColor = Color(0xFF1F2937),
                    inactiveTrackColor = Color(0xFFE5E7EB)
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Distance (Optional)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Maximum Distance",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151)
                    )
                )
                Text(
                    text = if (maxDistance == null) "All" else "${maxDistance!!.toInt()} km",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            val distanceOptions = listOf(null, 1f, 3f, 5f, 10f, 15f, 25f, 50f)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(distanceOptions) { distance ->
                    FilterChip(
                        onClick = { maxDistance = distance },
                        label = { 
                            Text(
                                if (distance == null) "All" else "${distance.toInt()} km", 
                                fontSize = 13.sp
                            ) 
                        },
                        selected = maxDistance == distance,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1F2937),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            if (maxDistance != null) {
                Text(
                    text = "Fine-tune: ${maxDistance!!.toInt()} km",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                )
                Slider(
                    value = maxDistance!!,
                    onValueChange = { maxDistance = it },
                    valueRange = 1f..50f,
                    steps = 49,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF1F2937),
                        activeTrackColor = Color(0xFF1F2937),
                        inactiveTrackColor = Color(0xFFE5E7EB)
                    )
                )
            } else {
                Text(
                    text = "Showing all jobs regardless of distance",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Experience Level
            Text(
                text = "Experience Level",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(experienceOptions) { option ->
                    FilterChip(
                        onClick = { experienceLevel = option },
                        label = { Text(option, fontSize = 13.sp) },
                        selected = experienceLevel == option,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1F2937),
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
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
                            workType = workType
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Apply Filters",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
