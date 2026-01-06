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
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.navigation.Routes
import com.example.dutype.components.ReusableSearchBar
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.components.JobCardShimmer
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.worker.components.JobCard
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

// Filter data class
data class JobFilters(
    val salaryMin: Int = 0,
    val salaryMax: Int = 100000,
    val maxDistance: Float = 15f, // Max 15km to reduce spam
    val experienceLevel: String = "Any",
    val gender: String = "Any",
    val sortBy: String = "Relevance"
)

/**
 * AllJobsScreen - Displays all available jobs with infinite scroll
 * 
 * PERFORMANCE: Uses server-side pagination (15 jobs at a time)
 * - Initial load: 15 jobs
 * - On scroll near end: Load 15 more from server
 * - Continues until all jobs are loaded
 */
@Composable
fun AllJobsScreen(
    navController: NavController,
    initialFilter: String = "All Jobs",
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    val jobUiState by jobViewModel.uiState.collectAsState()
    
    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf(initialFilter) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(JobFilters()) }
    var activeFilterCount by remember { mutableStateOf(0) }
    
    // Infinite scroll page size - increased for better UX
    val pageSize = 30L
    
    // Set status bar color and load initial batch of jobs
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        // Load first batch of jobs (increased from 15 to 50 for better initial experience)
        jobViewModel.loadJobs(50L)
    }
    
    // NOTE: Apply button removed from JobCard - users apply from JobDescriptionScreen
    // No need for duplicate API call here

    // Filter chips
    val filterChips = listOf(
        "All Jobs" to Icons.Default.Star,
        "Daily Jobs" to Icons.Default.CalendarToday,
        "Hourly Jobs" to Icons.Default.AccessTime,
        "Nearby" to Icons.Default.LocationOn,
        "Part Time" to Icons.Default.Work,
        "Full Time" to Icons.Default.CheckCircle
    )
    
    // Category mapping from UI names to Firestore category values
    val categoryMapping = mapOf(
        "Delivery" to "DELIVERY",
        "Shop Helper" to "HELPER",
        "Housekeeping" to "MAID",
        "Construction" to "HELPER",
        "Events" to "WAITER",
        "Kitchen" to "COOK",
        "Driver" to "DRIVER",
        "Security" to "SECURITY",
        "Electrician" to "ELECTRICIAN",
        "Plumber" to "PLUMBER",
        "Gardener" to "GARDENER",
        "Caretaker" to "CARETAKER",
        "Painter" to "PAINTER",
        "Carpenter" to "CARPENTER",
        "Receptionist" to "RECEPTIONIST",
        "Cashier" to "CASHIER",
        "Packer" to "PACKER"
    )
    
    // Check if initial filter is a category
    val isCategory = categoryMapping.containsKey(initialFilter)
    
    // Filter jobs based on selected chip and search query
    // PERFORMANCE: Using isFilled flag from job data instead of separate vacancy status API calls
    // NOTE: Applied jobs filtering removed - users can see all jobs, apply status shown in JobDescriptionScreen
    val filteredJobs = remember(selectedChip, jobUiState.jobs, searchQuery, initialFilter, filters) {
        // Filter out filled jobs using isFilled flag (no extra API call needed)
        val availableJobs = jobUiState.jobs.filter { job ->
            !job.isFilled
        }
        
        // Filter out expired jobs
        val activeJobs = availableJobs.filter { job ->
            !job.isExpired()
        }
        
        // First apply category filter if initial filter is a category
        val categoryFiltered = if (isCategory) {
            val firestoreCategory = categoryMapping[initialFilter] ?: initialFilter.uppercase()
            activeJobs.filter { job ->
                job.category.equals(firestoreCategory, ignoreCase = true) ||
                job.category.equals(initialFilter, ignoreCase = true) ||
                job.title.contains(initialFilter, ignoreCase = true)
            }
        } else {
            activeJobs
        }
        
        val chipFiltered = when (selectedChip) {
            "All Jobs" -> categoryFiltered
            "Daily Jobs" -> categoryFiltered.filter {
                it.payType.equals("DAILY", true) ||
                        it.payType.contains("day", true) ||
                        it.jobType.equals("Daily", true)
            }
            "Hourly Jobs" -> categoryFiltered.filter {
                it.payType.equals("HOURLY", true) ||
                        it.payType.contains("hour", true) ||
                        it.jobType.equals("Hourly", true)
            }
            "Nearby" -> categoryFiltered.filter { job ->
                job.distance != null && job.distance!! < 10.0
            }.sortedBy { it.distance }
            "Part Time" -> categoryFiltered.filter {
                it.jobType.equals("Part-time", true) ||
                        it.jobType.contains("part", true)
            }
            "Full Time" -> categoryFiltered.filter {
                it.jobType.equals("Full-time", true) ||
                        it.jobType.contains("full", true)
            }
            else -> categoryFiltered
        }
        
        // Apply advanced filters
        val advancedFiltered = chipFiltered.filter { job ->
            // Salary filter
            val jobSalary = job.payAmount.replace(",", "").replace("₹", "").toIntOrNull() 
                ?: job.payRate.toInt().takeIf { it > 0 } 
                ?: 0
            val salaryMatch = jobSalary == 0 || (jobSalary >= filters.salaryMin && jobSalary <= filters.salaryMax)
            
            // Distance filter
            val distanceMatch = job.distance == null || job.distance!! <= filters.maxDistance
            
            // Experience filter
            val experienceMatch = filters.experienceLevel == "Any" || 
                job.experienceLevel.contains(filters.experienceLevel, ignoreCase = true) ||
                job.experienceRequired.contains(filters.experienceLevel, ignoreCase = true)
            
            // Gender filter
            val genderMatch = filters.gender == "Any" || 
                job.gender.isEmpty() || 
                job.gender.equals(filters.gender, ignoreCase = true) ||
                job.gender.equals("Any", ignoreCase = true)
            
            salaryMatch && distanceMatch && experienceMatch && genderMatch
        }
        
        // Apply search filter
        val searchFiltered = if (searchQuery.isNotBlank()) {
            advancedFiltered.filter { job ->
                job.title.contains(searchQuery, ignoreCase = true) ||
                        job.companyName.contains(searchQuery, ignoreCase = true) ||
                        job.category.contains(searchQuery, ignoreCase = true) ||
                        job.location.contains(searchQuery, ignoreCase = true)
            }
        } else {
            advancedFiltered
        }
        
        // Apply sorting
        when (filters.sortBy) {
            "Salary: High to Low" -> searchFiltered.sortedByDescending { 
                it.payAmount.replace(",", "").replace("₹", "").toIntOrNull() ?: it.payRate.toInt() 
            }
            "Salary: Low to High" -> searchFiltered.sortedBy { 
                it.payAmount.replace(",", "").replace("₹", "").toIntOrNull() ?: it.payRate.toInt() 
            }
            "Distance" -> searchFiltered.sortedBy { it.distance ?: Float.MAX_VALUE.toDouble() }
            "Newest" -> searchFiltered.sortedByDescending { it.postedAt }
            else -> searchFiltered
        }
    }
    
    // Calculate active filter count
    LaunchedEffect(filters) {
        var count = 0
        if (filters.salaryMin > 0 || filters.salaryMax < 100000) count++
        if (filters.maxDistance < 50f) count++
        if (filters.experienceLevel != "Any") count++
        if (filters.gender != "Any") count++
        if (filters.sortBy != "Relevance") count++
        activeFilterCount = count
    }
    
    // NOTE: Apply button removed from JobCard - users apply from JobDescriptionScreen
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        // Common Header for consistency - show category name if filtering by category
        CommonHeader(
            title = if (isCategory) "$initialFilter Jobs" else "All Jobs",
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
            // Search bar with map icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ReusableSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
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
                    onClick = { selectedChip = chip },
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
        
        // Divider
        HorizontalDivider(
            color = Color(0xFFE5E7EB),
            thickness = 1.dp
        )

        // Job list
        when {
            jobUiState.isLoading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(6) {
                        JobCardShimmer()
                    }
                }
            }
            
            jobUiState.hasError -> {
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
                            style = AppTypography.emptyStateTitle.copy(
                                color = Color(0xFF374151)
                            )
                        )
                        Text(
                            text = jobUiState.error ?: "Unable to load jobs. Please try again.",
                            style = AppTypography.emptyStateSubtitle.copy(
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        )
                        Button(
                            onClick = { jobViewModel.loadJobs() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F2937)
                            ),
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
            
            filteredJobs.isEmpty() && !jobUiState.isLoading -> {
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
                            style = AppTypography.emptyStateTitle.copy(
                                color = Color(0xFF374151)
                            )
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
                            TextButton(
                                onClick = { selectedChip = "All Jobs" }
                            ) {
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
            
            else -> {
                // INFINITE SCROLL: Load 15 jobs at a time from server
                val listState = rememberLazyListState()
                
                // Server-side pagination: Detect when user scrolls near the end
                LaunchedEffect(listState, jobUiState.hasMore, jobUiState.isLoadingMore) {
                    snapshotFlow { 
                        val layoutInfo = listState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        // Trigger load when 5 items from end
                        lastVisibleItem >= totalItems - 5
                    }.collect { shouldLoadMore ->
                        if (shouldLoadMore && jobUiState.hasMore && !jobUiState.isLoadingMore && !jobUiState.isLoading) {
                            Timber.d("📦 INFINITE SCROLL: Loading more jobs from server...")
                            jobViewModel.loadMoreJobs(pageSize)
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
                        items = filteredJobs,
                        key = { it.jobId.ifEmpty { it.id } }
                    ) { job ->
                        val jobId = job.jobId.ifEmpty { job.id }
                        
                        // Use JobListing directly - no conversion needed
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
                            onCardClick = {
                                navController.navigate(Routes.jobDetailRoute(jobId))
                            }
                        )
                    }
                    
                    // Loading indicator at bottom when loading more from server
                    if (jobUiState.isLoadingMore || (jobUiState.hasMore && filteredJobs.isNotEmpty())) {
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
                    if (!jobUiState.hasMore && filteredJobs.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "You've seen all ${filteredJobs.size} jobs",
                                    style = AppTypography.caption,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Filter Bottom Sheet
    if (showFilterSheet) {
        JobFilterBottomSheet(
            filters = filters,
            onDismiss = { showFilterSheet = false },
            onApplyFilters = { newFilters ->
                filters = newFilters
                showFilterSheet = false
            },
            onResetFilters = {
                filters = JobFilters()
            }
        )
    }
}

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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
            
            // Distance - Geo-Fencing
            Text(
                text = "Maximum Distance",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quick distance chips for geo-fencing (max 15km to reduce spam)
            val distanceOptions = listOf(1f, 3f, 5f, 10f, 15f)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(distanceOptions) { distance ->
                    FilterChip(
                        onClick = { maxDistance = distance },
                        label = { 
                            Text(
                                text = "${distance.toInt()} km",
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
            
            // Fine-tune slider
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
