package com.example.dutype.worker.screens.myJobs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.dutype.data.dummy.dummyAppliedJobs
import com.example.dutype.navigation.Routes
import com.example.dutype.models.ApplicationStats
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.ui.components.ReusableSearchBar
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.ui.theme.WorkerGradientBackground
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.viewmodels.JobApplicationViewModel
import com.example.dutype.models.JobApplication
import com.example.dutype.worker.components.JobApplicationCard
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Helper functions for status display and colors 
fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending Review"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.REJECTED -> "Not Selected"
    }
}

fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFF59E0B) // Amber
        ApplicationStatus.UNDER_REVIEW -> Color(0xFF3B82F6) // Blue
        ApplicationStatus.ACCEPTED -> Color(0xFF10B981) // Green
        ApplicationStatus.REJECTED -> Color(0xFFEF4444) // Red
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobsScreen(
    navController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null
) {
    // Local state management
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val savedJobViewModel: SavedJobsViewModel = hiltViewModel()
    val jobApplicationViewModel: JobApplicationViewModel = hiltViewModel()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var selectedStatusFilter by remember { mutableStateOf<ApplicationStatus?>(null) }
    
    // Get real data for both applied and saved jobs
    val jobApplicationUiState by jobApplicationViewModel.uiState.collectAsStateWithLifecycle()
    val applications = jobApplicationUiState.applications
    val savedJobUiState by savedJobViewModel.uiState.collectAsStateWithLifecycle()
    val savedJobs = savedJobUiState.savedJobs
    
    // Application statistics
    val applicationStats = remember(applications) {
        val total = applications.size
        val pending = applications.count { it.status == ApplicationStatus.PENDING }
        val underReview = applications.count { it.status == ApplicationStatus.UNDER_REVIEW }
        val accepted = applications.count { it.status == ApplicationStatus.ACCEPTED }
        val rejected = applications.count { it.status == ApplicationStatus.REJECTED }
        
        ApplicationStats(
            totalApplications = total,
            pendingApplications = pending,
            shortlistedApplications = accepted,
            interviewedApplications = underReview,
            selectedApplications = accepted,
            rejectedApplications = rejected,
            thisMonthApplications = applications.count {
                val currentTime = System.currentTimeMillis()
                val monthAgo = currentTime - (30 * 24 * 60 * 60 * 1000L)
                it.appliedAt >= monthAgo
            },
            responseRate = if (total > 0) {
                val respondedApplications = applications.count { 
                    it.status != ApplicationStatus.PENDING
                }
                (respondedApplications.toFloat() / total) * 100f
            } else 0f
        )
    }
    
    // Debug logging for MyJobsScreen
    LaunchedEffect(savedJobUiState) {
        println("🔍 DEBUG MyJobsScreen: SavedJobs UI State - isLoading: ${savedJobUiState.isLoading}, savedJobs: ${savedJobUiState.savedJobs.size}, hasError: ${savedJobUiState.hasError}")
    }
    
    val filteredApplications = remember(applications, searchQuery, selectedStatusFilter) {
        applications.filter { application ->
            val matchesSearch = searchQuery.isEmpty() || 
                application.jobTitle.contains(searchQuery, ignoreCase = true) ||
                application.companyName.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || application.status == selectedStatusFilter
            matchesSearch && matchesStatus
        }
    }
    
    val filteredSavedJobs = remember(savedJobs, searchQuery) {
        savedJobs.filter { job ->
            searchQuery.isEmpty() || 
            job.title.contains(searchQuery, ignoreCase = true) ||
            job.companyName.contains(searchQuery, ignoreCase = true)
        }
    }

    val tabTitles = listOf("Applied Jobs", "Saved Jobs")
    val tabIcons = listOf(Icons.Default.Work, Icons.Default.Bookmark)

    // Status bar color management based on current tab
    val statusBarColor = when (selectedTabIndex) {
        0 -> Color.White // Applied Jobs - White
        1 -> Color.White // Saved Jobs - White
        else -> Color.White
    }

    // Update status bar color when tab changes
    LaunchedEffect(selectedTabIndex) {
        onStatusBarColorChange(statusBarColor)
    }
    
    // Load saved jobs and applications when component mounts
    LaunchedEffect(Unit) {
        savedJobViewModel.loadSavedJobs()
        jobApplicationViewModel.loadMyApplications()
    }

    WorkerGradientBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Enhanced Header with search
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title and search toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Jobs",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible }
                    ) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF6366F1)
                        )
                    }
                }

                // Search bar with animation
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        ReusableSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Search jobs, companies, locations...",
                            height = 48,
                            backgroundColor = Color(0xFFF3F4F6),
                            borderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF6366F1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enhanced Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF6366F1),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF6366F1),
                            height = 3.dp
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = tabIcons[index],
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selectedTabIndex == index) Color(0xFF6366F1) else Color(0xFF6B7280)
                                    )
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabIndex == index) Color(0xFF6366F1) else Color(0xFF6B7280)
                                    )
                                }
                            },
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
        
        // Application Statistics Card (only show for Applied Jobs tab)
        if (selectedTabIndex == 0 && applications.isNotEmpty()) {
            ApplicationStatisticsCard(
                stats = applicationStats,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Content based on selected tab
        when (selectedTabIndex) {
            0 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Status filter chips for Applied Jobs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        item {
                            FilterChip(
                                onClick = { selectedStatusFilter = null },
                                label = { Text("All") },
                                selected = selectedStatusFilter == null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6366F1).copy(alpha = 0.1f),
                                    selectedLabelColor = Color(0xFF6366F1)
                                )
                            )
                        }

                        ApplicationStatus.entries.forEach { status ->
                            item {
                                val count = applications.count { it.status == status }
                                if (count > 0) {
                                    FilterChip(
                                        onClick = {
                                            selectedStatusFilter = if (selectedStatusFilter == status) null else status
                                        },
                                        label = {
                                            Text("${getStatusDisplayName(status)} ($count)")
                                        },
                                        selected = selectedStatusFilter == status,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = getStatusColor(status).copy(alpha = 0.1f),
                                            selectedLabelColor = getStatusColor(status)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Applied jobs list
                    ScrollAwareLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 0.dp
                        ),
                        scrollStateManager = scrollStateManager
                    ) {
                        if (filteredApplications.isEmpty() && searchQuery.isNotEmpty()) {
                            item {
                                EmptySearchResults(searchQuery = searchQuery)
                            }
                        } else {
                            items(filteredApplications) { application ->
                                JobApplicationCard(
                                    application = application,
                                    onCardClick = { app ->
                                        navController.navigate(Routes.jobDetailRoute(app.jobId)) {
                                            // This ensures proper back navigation to the applied jobs tab
                                            popUpTo(Routes.WORKER_MY_JOBS) {
                                                inclusive = false
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        if (filteredApplications.isEmpty() && searchQuery.isEmpty() && selectedStatusFilter == null) {
                            item {
                                EmptyAppliedJobsState()
                            }
                        }
                    }
                }
            }
            1 -> {
                SavedJobsList(
                    searchQuery = searchQuery,
                    onNavigateToJobDetails = { jobId ->
                        // TODO: Navigate to job details
                        // navController.navigate(Routes.jobDetailRoute(jobId))
                    },
                    scrollStateManager = scrollStateManager
                )
            }
        }
    }
    }
}

@Composable
fun EmptySearchResults(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF9CA3AF)
            )
            Text(
                text = "No results found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
            Text(
                text = "No jobs match \"$searchQuery\"",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyAppliedJobsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Work,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF9CA3AF)
            )
            Text(
                text = "No Applications Yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
            Text(
                text = "Start applying to jobs to see them here",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { /* TODO: Navigate to job search */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Find Jobs")
            }
        }
    }
}

@Composable
fun ApplicationStatisticsCard(
    stats: ApplicationStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Application Statistics",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = "${stats.totalApplications} Total",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
            
            // Statistics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pending Applications
                StatisticItem(
                    label = "Pending",
                    count = stats.pendingApplications,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                
                // Shortlisted Applications
                StatisticItem(
                    label = "Shortlisted",
                    count = stats.shortlistedApplications,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                
                // Interviewed Applications
                StatisticItem(
                    label = "Interviewed",
                    count = stats.interviewedApplications,
                    color = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
                
                // Rejected Applications
                StatisticItem(
                    label = "Rejected",
                    count = stats.rejectedApplications,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Response Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Response Rate",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.1f%%", stats.responseRate),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3B82F6)
                    )
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}
