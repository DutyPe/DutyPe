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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

import com.example.dutype.navigation.Routes
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.ui.components.ReusableSearchBar
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.ui.theme.WorkerGradientBackground
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.viewmodels.JobApplicationViewModel
import com.example.dutype.models.JobApplication
import timber.log.Timber
import com.example.dutype.worker.components.JobApplicationCard
import com.example.dutype.utils.JobCardShimmer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.dutype.components.JobRatingBottomSheet
import com.example.dutype.services.RatingService
import com.google.firebase.auth.FirebaseAuth

// Helper functions for status display and colors 
fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending Review"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.COMPLETED -> "Completed"
        ApplicationStatus.REJECTED -> "Not Selected"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
    }
}

fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFF59E0B) // Amber
        ApplicationStatus.UNDER_REVIEW -> Color(0xFF3B82F6) // Blue
        ApplicationStatus.ACCEPTED -> Color(0xFF10B981) // Green
        ApplicationStatus.COMPLETED -> Color(0xFF7C3AED) // Purple
        ApplicationStatus.REJECTED -> Color(0xFFEF4444) // Red
        ApplicationStatus.WITHDRAWN -> Color(0xFF6B7280) // Gray
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
    
    // Withdraw dialog state
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var applicationToWithdraw by remember { mutableStateOf<JobApplication?>(null) }
    
    // Rating state
    var showRatingSheet by remember { mutableStateOf(false) }
    var applicationToRate by remember { mutableStateOf<JobApplication?>(null) }
    val ratingService = remember { RatingService() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    var ratedJobIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Get real data for both applied and saved jobs
    val jobApplicationUiState by jobApplicationViewModel.uiState.collectAsStateWithLifecycle()
    val applications = jobApplicationUiState.applications
    val savedJobUiState by savedJobViewModel.uiState.collectAsStateWithLifecycle()
    val savedJobs = savedJobUiState.savedJobs
    
    // Load which jobs the worker has already rated
    LaunchedEffect(currentUser?.uid, applications) {
        currentUser?.uid?.let { userId ->
            // Get all completed applications and check which ones have been rated
            val completedApps = applications.filter { it.status == ApplicationStatus.COMPLETED }
            val ratedIds = mutableSetOf<String>()
            completedApps.forEach { app ->
                ratingService.hasUserRatedForJob(userId, app.jobId).onSuccess { hasRated ->
                    if (hasRated) ratedIds.add(app.jobId)
                }
            }
            ratedJobIds = ratedIds
        }
    }
    
    
    // Debug logging for MyJobsScreen
    LaunchedEffect(savedJobUiState) {
        Timber.d("MyJobsScreen: SavedJobs UI State - isLoading: ${savedJobUiState.isLoading}, savedJobs: ${savedJobUiState.savedJobs.size}, hasError: ${savedJobUiState.hasError}")
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
        
        // Refresh applications when switching to Applied Jobs tab
        if (selectedTabIndex == 0) {
            jobApplicationViewModel.loadMyApplications()
        }
    }
    
    // Load saved jobs and applications when component mounts
    // Also refresh applications to ensure we have the latest status updates from employers
    LaunchedEffect(Unit) {
        savedJobViewModel.loadSavedJobs()
        jobApplicationViewModel.loadMyApplications()
    }
    
    // Additional refresh when screen becomes visible (when user navigates back)
    // This ensures we always have the latest status updates
    LaunchedEffect(Unit) {
        // Small delay to ensure the screen is fully visible before refreshing
        kotlinx.coroutines.delay(100)
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Title and search toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Jobs",
                        style = com.example.dutype.ui.theme.AppTypography.screenTitle.copy(
                            color = Color(0xFF111827)
                        )
                    )

                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible }
                    ) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Black
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
                            focusedBorderColor = Color(0xFF1F2937)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Enhanced Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF1F2937),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color.Black,
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
                                        tint = if (selectedTabIndex == index) Color.Black else Color(0xFF6B7280)
                                    )
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabIndex == index) Color.Black else Color(0xFF6B7280)
                                    )
                                }
                            },
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
        

        // Content based on selected tab
        when (selectedTabIndex) {
            0 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Status filter chips for Applied Jobs - Only show when there are applications
                    if (applications.isNotEmpty() && !jobApplicationUiState.isLoading) {
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
                                        selectedContainerColor = Color(0xFF1F2937).copy(alpha = 0.1f),
                                        selectedLabelColor = Color(0xFF1F2937)
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        scrollStateManager = scrollStateManager
                    ) {
                        // Applied jobs list - Show shimmer while loading
                    if (jobApplicationUiState.isLoading) {
                        items(4) {
                            JobCardShimmer()
                        }
                    } else if (filteredApplications.isEmpty() && searchQuery.isNotEmpty()) {
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
                                },
                                onWithdrawClick = { app ->
                                    applicationToWithdraw = app
                                    showWithdrawDialog = true
                                },
                                onRateClick = { app ->
                                    applicationToRate = app
                                    showRatingSheet = true
                                },
                                hasAlreadyRated = ratedJobIds.contains(application.jobId)
                            )
                        }
                    }

                    if (filteredApplications.isEmpty() && searchQuery.isEmpty() && selectedStatusFilter == null && !jobApplicationUiState.isLoading) {
                            item {
                                EmptyAppliedJobsState(navController = navController)
                            }
                        }
                    }
                }
            }
            1 -> {
                SavedJobsList(
                    searchQuery = searchQuery,
                    onNavigateToJobDetails = { jobId ->
                        navController.navigate(Routes.jobDetailRoute(jobId))
                    },
                    scrollStateManager = scrollStateManager,
                    navController = navController
                )
            }
        }
    }
    
    // Withdraw Confirmation Dialog
    if (showWithdrawDialog && applicationToWithdraw != null) {
        AlertDialog(
            onDismissRequest = { 
                showWithdrawDialog = false
                applicationToWithdraw = null
            },
            title = {
                Text(
                    text = "Withdraw Application?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to withdraw your application for \"${applicationToWithdraw?.jobTitle}\"? This action cannot be undone.",
                    color = Color(0xFF6B7280)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        applicationToWithdraw?.let { app ->
                            jobApplicationViewModel.withdrawApplication(app.applicationId) { success, error ->
                                if (success) {
                                    Timber.d("Application withdrawn successfully")
                                } else {
                                    Timber.e("Failed to withdraw application: $error")
                                }
                            }
                        }
                        showWithdrawDialog = false
                        applicationToWithdraw = null
                    }
                ) {
                    Text(
                        text = "Withdraw",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showWithdrawDialog = false
                        applicationToWithdraw = null
                    }
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF6B7280)
                    )
                }
            }
        )
    }
    
    // Rating Bottom Sheet for worker to rate employer
    if (showRatingSheet && applicationToRate != null && currentUser != null) {
        JobRatingBottomSheet(
            isVisible = true,
            onDismiss = { 
                showRatingSheet = false
                applicationToRate = null
            },
            jobId = applicationToRate!!.jobId,
            applicationId = applicationToRate!!.applicationId,
            jobTitle = applicationToRate!!.jobTitle,
            companyName = applicationToRate!!.companyName,
            ratedUserId = applicationToRate!!.employerId,
            ratedUserName = applicationToRate!!.companyName,
            ratedUserRole = com.example.dutype.models.RatingUserRole.EMPLOYER,
            raterUserId = currentUser.uid,
            raterUserRole = com.example.dutype.models.RatingUserRole.WORKER,
            ratingService = ratingService,
            onRatingSubmitted = {
                // Add to rated jobs set
                applicationToRate?.let { app ->
                    ratedJobIds = ratedJobIds + app.jobId
                }
                showRatingSheet = false
                applicationToRate = null
            }
        )
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
                style = com.example.dutype.ui.theme.AppTypography.emptyStateTitle.copy(
                    color = Color(0xFF374151)
                )
            )
            Text(
                text = "No jobs match \"$searchQuery\"",
                style = com.example.dutype.ui.theme.AppTypography.emptyStateSubtitle.copy(
                    color = Color(0xFF6B7280)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyAppliedJobsState(navController: NavHostController? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Work,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Color(0xFF9CA3AF)
            )
            Text(
                text = "No Applications Yet",
                style = com.example.dutype.ui.theme.AppTypography.emptyStateTitle.copy(
                    color = Color(0xFF374151)
                )
            )
            Text(
                text = "Apply to jobs to track them here",
                style = com.example.dutype.ui.theme.AppTypography.emptyStateSubtitle.copy(
                    color = Color(0xFF6B7280)
                ),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {
                    // Use WORKER_HOME_TAB ("home") for navigation within worker bottom nav
                    navController?.navigate(Routes.WORKER_HOME_TAB) {
                        popUpTo(Routes.WORKER_HOME_TAB) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Find Jobs",
                    style = com.example.dutype.ui.theme.AppTypography.buttonMedium
                )
            }
        }
    }
}

