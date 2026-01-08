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
import androidx.compose.runtime.snapshotFlow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.navigation.Routes
import com.example.dutype.components.ReusableSearchBar
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.components.JobCardShimmer
import com.example.dutype.viewmodels.AllJobsViewModel
import com.example.dutype.viewmodels.JobFilters
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.worker.components.JobCard
import com.example.dutype.worker.components.AdAwareJobCard
import timber.log.Timber

/**
 * AllJobsScreen - Displays all available jobs with infinite scroll
 * 
 * P0 PERFORMANCE FIX: Filtering logic moved to AllJobsViewModel
 * - Before: filteredJobs computed in remember{} block - triggers recomposition
 * - After: filteredJobs as StateFlow from ViewModel - computed outside composition
 * 
 * PERFORMANCE: Uses server-side pagination (30 jobs at a time)
 * - Initial load: 50 jobs
 * - On scroll near end: Load 30 more from server
 * - Continues until all jobs are loaded
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */
@Composable
fun AllJobsScreen(
    navController: NavController,
    initialFilter: String = "All Jobs",
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    
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
    
    // Infinite scroll page size
    val pageSize = 30L
    
    // Set status bar color and initialize ViewModel
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        viewModel.setInitialCategory(initialFilter.takeIf { it != "All Jobs" })
        viewModel.loadJobs(50L)
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
            .background(WorkerColors.ScreenBackground)
    ) {
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
                        placeholder = "Search jobs, companies...",
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activeFilterCount > 0) Color(0xFF1F2937) else Color(0xFFF1F5F9))
                        .clickable { showFilterSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = "Filter",
                        tint = if (activeFilterCount > 0) Color.White else Color(0xFF374151),
                        modifier = Modifier.size(22.dp)
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
                                modifier = Modifier.size(16.dp),
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
                    onRetry = { viewModel.loadJobs() }
                )
            }
            
            filteredJobs.isEmpty() && !uiState.isLoading -> {
                EmptyState(
                    searchQuery = searchQuery,
                    selectedChip = selectedChip,
                    onViewAllJobs = { viewModel.setSelectedChip("All Jobs") }
                )
            }
            
            else -> {
                JobsList(
                    jobs = filteredJobs,
                    uiState = uiState,
                    pageSize = pageSize,
                    onLoadMore = { viewModel.loadMoreJobs(pageSize) },
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
 * Uses AdAwareJobCard for centralized ad handling
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
    
    // Server-side pagination: Detect when user scrolls near the end
    LaunchedEffect(listState, uiState.hasMore, uiState.isLoadingMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 5
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && uiState.hasMore && !uiState.isLoadingMore && !uiState.isLoading) {
                Timber.d("📦 INFINITE SCROLL: Loading more jobs from server...")
                onLoadMore()
            }
        }
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = jobs,
            key = { it.jobId.ifEmpty { it.id } }
        ) { job ->
            val jobId = job.jobId.ifEmpty { job.id }
            
            AdAwareJobCard(
                job = job,
                isSaved = job.isSaved,
                onSaveClick = { onSaveClick(jobId, job.isSaved) },
                onNavigateToJob = onNavigateToJob
            )
        }
        
        // Loading indicator at bottom
        if (uiState.isLoadingMore || (uiState.hasMore && jobs.isNotEmpty())) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF1F2937),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
        
        // End of list indicator
        if (!uiState.hasMore && jobs.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You've seen all ${jobs.size} jobs",
                        style = AppTypography.caption,
                        color = Color(0xFF9CA3AF)
                    )
                }
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
                    modifier = Modifier.size(36.dp)
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
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
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
    onViewAllJobs: () -> Unit
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
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkOff,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = "No Jobs Found",
                style = AppTypography.emptyStateTitle.copy(color = Color(0xFF374151))
            )
            Text(
                text = if (searchQuery.isNotBlank())
                    "No jobs match \"$searchQuery\". Try different keywords."
                else
                    "No $selectedChip available right now.\nTry a different filter or check back later.",
                style = AppTypography.emptyStateSubtitle.copy(
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
            )
            if (selectedChip != "All Jobs") {
                TextButton(onClick = onViewAllJobs) {
                    Text(
                        text = "View All Jobs",
                        color = Color(0xFF1F2937),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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
    var gender by remember { mutableStateOf(filters.gender) }
    var sortBy by remember { mutableStateOf(filters.sortBy) }
    
    val experienceOptions = listOf("Any", "Fresher", "1-2 years", "2-5 years", "5+ years")
    val genderOptions = listOf("Any", "Male", "Female")
    val sortOptions = listOf("Relevance", "Newest", "Salary: High to Low", "Salary: Low to High", "Distance")
    
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
                    maxDistance = 15f
                    experienceLevel = "Any"
                    gender = "Any"
                    sortBy = "Relevance"
                    onResetFilters()
                }) {
                    Text("Reset", color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                }
            }
            
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
            
            // Distance
            Text(
                text = "Maximum Distance",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val distanceOptions = listOf(1f, 3f, 5f, 10f, 15f)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(distanceOptions) { distance ->
                    FilterChip(
                        onClick = { maxDistance = distance },
                        label = { Text("${distance.toInt()} km", fontSize = 13.sp) },
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
            Text(
                text = "Fine-tune: ${maxDistance.toInt()} km",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
            )
            Slider(
                value = maxDistance,
                onValueChange = { maxDistance = it },
                valueRange = 1f..15f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF1F2937),
                    activeTrackColor = Color(0xFF1F2937),
                    inactiveTrackColor = Color(0xFFE5E7EB)
                )
            )
            
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
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Gender Preference
            Text(
                text = "Gender Preference",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                genderOptions.forEach { option ->
                    FilterChip(
                        onClick = { gender = option },
                        label = { Text(option, fontSize = 13.sp) },
                        selected = gender == option,
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
                            gender = gender,
                            sortBy = sortBy
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
