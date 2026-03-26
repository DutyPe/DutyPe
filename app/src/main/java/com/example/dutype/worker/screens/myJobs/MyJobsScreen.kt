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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.dutype.app.R
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
import com.example.dutype.models.getStatusColor
import com.example.dutype.models.getDisplayName
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.components.ReusableSearchBar
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.models.JobApplication
import timber.log.Timber
import com.example.dutype.worker.components.JobApplicationCard
import com.example.dutype.components.JobCardShimmer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.google.firebase.auth.FirebaseAuth

// NOTE: getStatusDisplayName and getStatusColor removed
// Use extension functions from com.example.dutype.models:
// - ApplicationStatus.getDisplayName()
// - ApplicationStatus.getStatusColor()

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
    val jobApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var selectedStatusFilter by remember { mutableStateOf<ApplicationStatus?>(null) }
    
    // Withdraw dialog state
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var applicationToWithdraw by remember { mutableStateOf<JobApplication?>(null) }
    
    // Rating state
    var showRatingSheet by remember { mutableStateOf(false) }
    var applicationToRate by remember { mutableStateOf<JobApplication?>(null) }
    var ratedApplicationIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val ratingService = remember { com.example.dutype.services.RatingService(com.google.firebase.firestore.FirebaseFirestore.getInstance(), FirebaseAuth.getInstance()) }
    val ratingScope = rememberCoroutineScope()
    
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    // Get real data for both applied and saved jobs
    val jobApplicationUiState by jobApplicationViewModel.legacyUiState.collectAsStateWithLifecycle()
    val applications = jobApplicationUiState.applications
    val savedJobUiState by savedJobViewModel.uiState.collectAsStateWithLifecycle()
    val savedJobs = savedJobUiState.savedJobs
    
    
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

    val tabTitles = listOf(stringResource(R.string.applied_jobs), stringResource(R.string.saved_jobs))
    val tabIcons = listOf(Icons.Default.Work, Icons.Default.Bookmark)

    // Status bar color management based on current tab
    val statusBarColor = when (selectedTabIndex) {
        0 -> Color.White // Applied Jobs - White
        1 -> Color.White // Saved Jobs - White
        else -> Color.White
    }

    // Update status bar color - White for non-home screens
    LaunchedEffect(selectedTabIndex) {
        onStatusBarColorChange(Color.White)
        
        // Refresh applications when switching to Applied Jobs tab
        if (selectedTabIndex == 0) {
            jobApplicationViewModel.loadMyApplications()
        }
    }
    
    // Load saved jobs and applications once when component mounts
    LaunchedEffect(Unit) {
        savedJobViewModel.loadSavedJobs()
        // Applied jobs loaded via tab switch LaunchedEffect above (selectedTabIndex starts at 0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
            .statusBarsPadding() // Add top padding for status bar
    ) {
            // Offline banner at the very top
            val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
            val isOnline by connectivityViewModel.isOnline.collectAsState()
            com.example.dutype.components.OfflineBanner(isOffline = !isOnline)
            
        // Enhanced Header without search
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Title only - search removed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.my_jobs),
                        style = com.example.dutype.ui.theme.AppTypography.screenTitle.copy(
                            color = Color(0xFF111827)
                        )
                    )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WorkerColors.ScreenBackground)
                ) {
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
                                                Text("${status.getDisplayName()} ($count)")
                                            },
                                            selected = selectedStatusFilter == status,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = status.getStatusColor().copy(alpha = 0.1f),
                                                selectedLabelColor = status.getStatusColor()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    when {
                        jobApplicationUiState.isLoading -> {
                            ScrollAwareLazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(WorkerColors.ScreenBackground),
                                contentPadding = PaddingValues(
                                    top = 16.dp,
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 0.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                scrollStateManager = scrollStateManager
                            ) {
                                items(4) {
                                    JobCardShimmer()
                                }
                            }
                        }
                        filteredApplications.isEmpty() && searchQuery.isEmpty() && selectedStatusFilter == null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(WorkerColors.ScreenBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyAppliedJobsState(navController = navController)
                            }
                        }
                        else -> {
                            ScrollAwareLazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(WorkerColors.ScreenBackground),
                                contentPadding = PaddingValues(
                                    top = 16.dp,
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 0.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                scrollStateManager = scrollStateManager
                            ) {
                                if (filteredApplications.isEmpty() && searchQuery.isNotEmpty()) {
                                    item {
                                        EmptySearchResults(searchQuery = searchQuery)
                                    }
                                } else {
                                    items(
                                        items = filteredApplications,
                                        key = { application -> "myjobs_${application.id}" }
                                    ) { application ->
                                        JobApplicationCard(
                                            application = application,
                                            onCardClick = { app ->
                                                navController.navigate(Routes.jobDetailRoute(app.jobId)) {
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
                                            onStartWorkClick = { app ->
                                                navController.navigate(Routes.workerWorkStartQRRoute(app.jobId))
                                            },
                                            hasAlreadyRated = application.id in ratedApplicationIds
                                        )
                                    }
                                }
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
                    text = stringResource(R.string.withdraw_application_title),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.withdraw_application_message),
                    color = Color(0xFF6B7280)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        applicationToWithdraw?.let { app ->
                            jobApplicationViewModel.withdrawApplication(app.id) { success, error ->
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
                        text = stringResource(R.string.withdraw),
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
                        text = stringResource(R.string.cancel),
                        color = Color(0xFF6B7280)
                    )
                }
            }
        )
    }

    // Rating Bottom Sheet
    com.example.dutype.components.RatingBottomSheet(
        isVisible = showRatingSheet && applicationToRate != null,
        targetName = applicationToRate?.companyName ?: "",
        targetRole = "EMPLOYER",
        onDismiss = {
            showRatingSheet = false
            applicationToRate = null
        },
        onSubmit = { rating, review, tags ->
            applicationToRate?.let { app ->
                ratingScope.launch {
                    val result = ratingService.submitRating(
                        applicationId = app.id,
                        jobId = app.jobId,
                        targetUserId = app.employerId,
                        targetUserName = app.companyName,
                        targetRole = "EMPLOYER",
                        raterRole = "WORKER",
                        rating = rating,
                        review = review,
                        tags = tags
                    )
                    result.onSuccess { ratingResult ->
                        if (ratingResult.success) {
                            ratedApplicationIds = ratedApplicationIds + app.id
                        }
                    }
                    showRatingSheet = false
                    applicationToRate = null
                }
            }
        }
    )

    // Check which applications have already been rated
    LaunchedEffect(applications) {
        currentUser?.uid?.let {
            val completedApps = applications.filter { it.status == ApplicationStatus.COMPLETED }
            val rated = mutableSetOf<String>()
            completedApps.forEach { app ->
                if (ratingService.hasRated(app.jobId, app.employerId)) {
                    rated.add(app.id)
                }
            }
            ratedApplicationIds = rated
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
                text = stringResource(R.string.no_results_found),
                style = com.example.dutype.ui.theme.AppTypography.emptyStateTitle.copy(
                    color = Color(0xFF374151)
                )
            )
            Text(
                text = stringResource(R.string.no_jobs_match, searchQuery),
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
                text = stringResource(R.string.no_applications_yet),
                style = com.example.dutype.ui.theme.AppTypography.emptyStateTitle.copy(
                    color = Color(0xFF374151)
                )
            )
            Text(
                text = stringResource(R.string.apply_to_jobs_to_track),
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
                    text = stringResource(R.string.find_jobs),
                    style = com.example.dutype.ui.theme.AppTypography.buttonMedium
                )
            }
        }
    }
}

