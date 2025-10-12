package com.example.partimes.worker.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.partimes.R
import com.example.partimes.components.ScrollAwareLazyColumn
import com.example.partimes.data.ApplicationFormDataStore
import com.example.partimes.location.LocationPreferences
import com.example.partimes.models.JobListing
import com.example.partimes.navigation.Routes
import com.example.partimes.notifications.viewmodels.NotificationCenterViewModel
import com.example.partimes.state.SavedJobsStateManager
import com.example.partimes.ui.components.ReusableSearchBar
import com.example.partimes.ui.theme.WorkerGradientBackground
import com.example.partimes.utils.JobCardShimmer
import com.example.partimes.utils.ScrollStateManager
import com.example.partimes.viewmodels.FirestoreJobViewModel
import com.example.partimes.viewmodels.ProfileViewModel
import com.example.partimes.viewmodels.JobApplicationViewModel
import com.example.partimes.viewmodels.ProfileCompletionViewModel
import com.example.partimes.viewmodels.SavedJobsViewModel
import com.example.partimes.viewmodels.SmartJobApplicationViewModel
import com.example.partimes.worker.components.ProfileCompletionPrompt
import com.example.partimes.worker.components.CompactProfileCompletionBanner
import com.example.partimes.worker.components.JobCard
import com.example.partimes.worker.models.JobCardModel
import com.example.partimes.worker.models.JobTag
import com.example.partimes.worker.models.LocationInfo
import com.example.partimes.worker.models.PayInfo
import com.example.partimes.worker.models.PayType
import com.example.partimes.worker.models.TagType
import com.example.partimes.worker.models.TimeInfo
import com.example.partimes.worker.models.UrgencyLevel
import com.example.partimes.models.ApplicationStatus
import com.example.partimes.models.JobApplication
import com.example.partimes.services.ProfileCompletionService
import com.example.partimes.state.ProfileSetupStateManager
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState

// Helper function to check if a job has been applied to
fun hasAppliedToJob(jobId: String, applications: List<JobApplication>): Boolean {
    return applications.any { application -> 
        application.jobId == jobId && 
        application.status != ApplicationStatus.WITHDRAWN && 
        application.status != ApplicationStatus.REJECTED 
    }
}

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
    scrollStateManager: ScrollStateManager? = null
) {
    val context = LocalContext.current
    val locationPreferences = remember { LocationPreferences(context) }
    val currentLocation by locationPreferences.currentLocation.collectAsState()
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val jobApplicationViewModel: JobApplicationViewModel = hiltViewModel()
    val smartApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    val dataStore: ApplicationFormDataStore = remember { ApplicationFormDataStore(context) }
    val notificationViewModel: NotificationCenterViewModel = hiltViewModel()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    val jobUiState by jobViewModel.uiState.collectAsState()
    val profileUiState by profileViewModel.uiState.collectAsState()
    val jobApplicationUiState by jobApplicationViewModel.uiState.collectAsStateWithLifecycle()
    val applications = jobApplicationUiState.applications
    
    // Profile completion variables
    var profileSetupStatus by remember { mutableStateOf<com.example.partimes.state.ProfileSetupStatus?>(null) }
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    var missingFields by remember { mutableStateOf<List<String>>(emptyList()) }
    var canApplyDirectly by remember { mutableStateOf(false) }
    
    // Load profile completion status
    LaunchedEffect(Unit) {
        try {
            val status = profileCompletionViewModel.getProfileSetupStatus(com.example.partimes.models.UserRole.WORKER)
            profileSetupStatus = status
            profileCompletionPercentage = status.completionPercentage
            missingFields = status.missingFields
            canApplyDirectly = status.isComplete
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Function to apply for job directly
    val applyForJobDirectly: (String) -> Unit = { jobId ->
        if (canApplyDirectly) {
            // Show applying message
            android.widget.Toast.makeText(context, "Applying for job...", android.widget.Toast.LENGTH_SHORT).show()

            // Use SmartJobApplicationViewModel for direct application
            smartApplicationViewModel.applyForJob(jobId)

            // Navigate to applied jobs to show the new application
            navController.navigate(Routes.WORKER_MY_JOBS)
        } else {
            // Profile not complete, show completion prompt
            android.widget.Toast.makeText(context, "Please complete your profile first", android.widget.Toast.LENGTH_LONG).show()
            navController.navigate(Routes.PROFILE_SETUP)
        }
    }
    
    // Function to save/unsave job
    fun saveJob(jobId: String) {
        savedJobsViewModel.saveJob(jobId)
    }
    
    fun unsaveJob(jobId: String) {
        savedJobsViewModel.unsaveJob(jobId)
    }
    
    // Smart application features
    
    // Function to check if user has applied for a job
    fun hasAppliedToJob(jobId: String, applications: List<com.example.partimes.models.JobApplication>): Boolean {
        return applications.any { it.jobId == jobId }
    }
    
    
    // Load data
    LaunchedEffect(Unit) {
        jobViewModel.loadJobs()
        profileViewModel.loadProfile()
        jobApplicationViewModel.loadMyApplications()
    }
    
    
    // Refresh jobs when screen becomes visible (for proper saved state)
    DisposableEffect(Unit) {
        // Refresh jobs when component is created
        jobViewModel.loadJobs()
        
        onDispose {
            // Cleanup if needed
        }
    }
    
    // WhatsApp sharing function
    val shareToWhatsApp = {
        val packageManager = context.packageManager
        val appPackageName = context.packageName
        
        try {
            // Try to open WhatsApp directly
            val whatsappIntent = packageManager.getLaunchIntentForPackage ("com.whatsapp")
            if (whatsappIntent != null) {
                // Create sharing intent for WhatsApp
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, 
                        "Check out this amazing job app! Download DutyPe and find your dream job.\n\n" +
                        "Download link: https://play.google.com/store/apps/details?id=$appPackageName"
                    )
                    setPackage("com.whatsapp")
                }
                context.startActivity(shareIntent)
            } else {
                // WhatsApp not installed, open in browser
                val browserIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20https://play.google.com/store/apps/details?id=$appPackageName")
                )
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Fallback to browser
            val browserIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20https://play.google.com/store/apps/details?id=$appPackageName")
            )
            context.startActivity(browserIntent)
        }
    }

    val tabTitles = listOf("All Jobs", "Hourly", "Daily", "Part-time / Full-time")
    val tabIcons = listOf(
        Icons.Default.Star,
        Icons.Default.AccessTime,
        Icons.Default.CalendarToday,
        Icons.Default.Work
    )

    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0)
    val pullToRefreshState = rememberPullToRefreshState()

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

    // Handle search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            jobViewModel.searchJobs(searchQuery)
        } else {
            jobViewModel.loadJobs()
        }
    }

    // Enhanced location text - showing only city name for cleaner display
    val locationText = remember(currentLocation) {
        when {
            currentLocation != null -> {
                val city = currentLocation!!.city
                val state = currentLocation!!.state

                when {
                    !city.isNullOrEmpty() -> {
                        // Show only city name, truncate if too long
                        truncateLocationHeaderText(city)
                    }
                    currentLocation!!.address.isNotEmpty() -> {
                        // Extract city from address if available
                        val addressParts = currentLocation!!.address.split(",")
                        val extractedCity = if (addressParts.isNotEmpty()) {
                            addressParts[0].trim()
                        } else {
                            currentLocation!!.address
                        }
                        truncateLocationHeaderText(extractedCity)
                    }
                    else -> "Select Your Location"
                }
            }
            else -> "Please select location"
        }
    }

    WorkerGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Enhanced header section with modern design and subtle animation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {

                // Single row header - Location on left, Icons on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Location section
                    Row(
                        modifier = Modifier
                            .weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Location icon
                        Icon(
                            painter = painterResource(id = R.drawable.location_icon),
                            contentDescription = "Location",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(25.dp)
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        // Location text (clickable) with dropdown arrow (not clickable)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Text(
                                text = locationText,
                                modifier = Modifier
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        // Navigate based on whether location is selected or not
                                        if (currentLocation == null) {
                                            rootNavController.navigate(Routes.LOCATION_SERVICE)
                                        } else {
                                            rootNavController.navigate(Routes.MANUAL_LOCATION_ROUTE)
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    // Right side - Action icons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Refer button with WhatsApp icon
                        Button(
                            onClick = { shareToWhatsApp() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE8F5E8),
                                contentColor = Color(0xFF25D366)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                    painter = painterResource(id = R.drawable.whatsapp),
                                    contentDescription = "WhatsApp",
                                    modifier = Modifier.size(21.dp)
                                    )
                                    Text(
                                    text = "Refer",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        // Heart icon
//                        IconButton(
//                            onClick = { /* Handle favorites */ },
//                            modifier = Modifier.size(38.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Outlined.FavoriteBorder,
//                                contentDescription = "Favorites",
//                                tint = Color(0xFF6B7280),
//                                modifier = Modifier.size(25.dp)
//                            )
//                        }

                        // Notification icon with badge
                        Box {
                            IconButton(
                                onClick = {
                                    navController.navigate(Routes.NOTIFICATION_CENTER)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            
                            // Notification badge
                            if (notificationUiState.unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            Color.Red,
                                            shape = CircleShape
                                        )
                                        .align(Alignment.TopEnd)
                                        .offset(x = 2.dp, y = (-2).dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search bar below location section
                ReusableSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search For Location..",
                    height = 48,
                    showClearButton = true,
                    backgroundColor = Color.White
                )
            }

            // Content section with pager
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Simple job cards list
                    PullToRefreshBox(
                        isRefreshing = jobUiState.isRefreshing,
                        onRefresh = {
                            jobViewModel.refreshJobs()
                        },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            jobUiState.isLoading -> {
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
                            jobUiState.jobs.isEmpty() -> {
                                EmptyJobsContent()
                            }
                            else -> {
                                HorizontalJobsContent(
                                    jobListings = jobUiState.jobs,
                                    navController = navController,
                                    savedJobsViewModel = savedJobsViewModel,
                                    applications = applications,
                                    onApplyClick = applyForJobDirectly
                                )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun LoadingContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(6) { // Show 6 shimmer cards
            JobCardShimmer()
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
                    modifier = Modifier.size(48.dp)
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
                        containerColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
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
}

@Composable
private fun EmptyJobsContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
//            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "🔍",
                    fontSize = 48.sp
                )
                Text(
                    text = "No Jobs Available",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = "Check back later for new opportunities or try adjusting your search criteria.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
private fun FooterContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Jobs Made with",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "💙",
                        fontSize = 23.sp
                    )
                    Text(
                        text = "in Bharat",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalJobsContent(
    jobListings: List<JobListing>,
    navController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    applications: List<JobApplication>,
    onApplyClick: (String) -> Unit
) {
    // Convert JobListing to JobCardModel
    val jobCards = remember(jobListings) {
        jobListings.map { job ->
            JobCardModel(
                jobId = job.id,
                title = job.title,
                employerName = job.companyName,
                payInfo = PayInfo(
                    amount = cleanPaymentAmount(
                        (job.payAmount.ifEmpty { job.salary }).ifEmpty {
                            if (job.payRate > 0.0) job.payRate.toInt().toString() else ""
                        }
                    ),
                    type = when {
                        job.payType.equals("HOURLY", true) || job.payType.contains("hour", true) -> PayType.HOURLY
                        job.payType.equals("DAILY", true) || job.payType.contains("day", true) -> PayType.DAILY
                        job.payType.equals("MONTHLY", true) || job.payType.contains("month", true) -> PayType.MONTHLY
                        else -> PayType.DAILY
                    },
                    period = "" // computed in PayInfo.getDisplayText
                ),
                location = LocationInfo(
                    area = truncateLocationText(job.area ?: job.location),
                    city = truncateLocationText(job.city ?: job.location),
                    distance = "2.5"
                ),
                tags = listOf(
                    JobTag(
                        text = job.jobType,
                        emoji = "💼",
                        type = TagType.BENEFIT
                    ),
                    JobTag(
                        text = job.category,
                        emoji = "🏷️",
                        type = TagType.BENEFIT
                    )
                ),
                timeInfo = TimeInfo(
                    postedTime = job.postedDate,
                    urgency = if (job.isUrgent()) UrgencyLevel.URGENT else UrgencyLevel.NORMAL
                ),
                phoneNumber = job.contactNumber,
                description = job.description,
                jobType = job.jobType,
                isBookmarked = false, // TODO: Get from WorkerJobInteraction
                isSaved = job.isSaved, // Get from JobListing
                isApplied = false // TODO: Get from WorkerJobInteraction
            )
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // All Jobs Section
        item {
            JobSection(
                title = "All Jobs",
                jobs = jobCards,
                navController = navController,
                savedJobsViewModel = savedJobsViewModel,
                applications = applications,
                onApplyClick = onApplyClick
            )
        }
        
        // Hourly Jobs Section
        item {
            JobSection(
                title = "Hourly Jobs",
                jobs = jobCards.filter { it.payInfo.type == PayType.HOURLY },
                navController = navController,
                savedJobsViewModel = savedJobsViewModel,
                applications = applications,
                onApplyClick = onApplyClick
            )
        }
        
        // Daily Jobs Section
        item {
            JobSection(
                title = "Daily Jobs",
                jobs = jobCards.filter { it.payInfo.type == PayType.DAILY },
                navController = navController,
                savedJobsViewModel = savedJobsViewModel,
                applications = applications,
                onApplyClick = onApplyClick
            )
        }
        
        // Part-time/Full-time Jobs Section
        item {
            JobSection(
                title = "Part-time & Full-time",
                jobs = jobCards.filter { 
                    it.jobType == "Part-time" || it.jobType == "Full-time" 
                },
                navController = navController,
                savedJobsViewModel = savedJobsViewModel,
                applications = applications,
                onApplyClick = onApplyClick
            )
        }
    }
}

@Composable
private fun JobSection(
    title: String,
    jobs: List<JobCardModel>,
    navController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    applications: List<JobApplication>,
    onApplyClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            )
            Text(
                text = "See all",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.clickable { /* Handle see all */ }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Horizontal scrolling job cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(jobs) { job ->
                JobCard(
                    jobCard = job,
                    isSaved = job.isSaved,
                    hasApplied = hasAppliedToJob(job.jobId, applications),
                            onApplyClick = { jobId ->
                                onApplyClick(jobId)
                            },
                    onSaveClick = { jobId ->
                        if (job.isSaved) {
                            savedJobsViewModel.unsaveJob(jobId)
                        } else {
                            savedJobsViewModel.saveJob(jobId)
                        }
                    },
                    onCardClick = { jobId ->
                        navController.navigate(Routes.jobDetailRoute(jobId))
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyTabContent(
    title: String,
    message: String,
    icon: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 48.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

/**
 * Helper function to truncate location text for better display in job cards
 * Only used in WorkerHomeScreen to keep location names concise
 */
private fun truncateLocationText(locationText: String?): String {
    if (locationText.isNullOrEmpty()) {
        return "Location"
    }
    
    // If the text is already short enough, return as is
    if (locationText.length <= 20) {
        return locationText
    }
    
    // Truncate long location names and add ellipsis
    return "${locationText.take(17)}..."
}

/**
 * Helper function to clean payment amount from duplicates
 * Removes any existing payment type text (hourly, daily, monthly) from the amount
 */
private fun cleanPaymentAmount(amount: String): String {
    return amount
        .replace("/hourly", "", ignoreCase = true)
        .replace("/daily", "", ignoreCase = true)
        .replace("/monthly", "", ignoreCase = true)
        .replace("/per task", "", ignoreCase = true)
        .replace("hourly", "", ignoreCase = true)
        .replace("daily", "", ignoreCase = true)
        .replace("monthly", "", ignoreCase = true)
        .replace("per task", "", ignoreCase = true)
        .replace("per hour", "", ignoreCase = true)
        .replace("per day", "", ignoreCase = true)
        .replace("per month", "", ignoreCase = true)
        .replace("Rs.", "", ignoreCase = true)
        .replace("rs", "", ignoreCase = true)
        .replace("/", "")
        .trim()
}

/**
 * Helper function to truncate location text for header display
 * Removes pincode and long addresses from location display
 */
private fun truncateLocationHeaderText(locationText: String): String {
    // Remove pincode (6 digits) from the location text
    val withoutPincode = locationText.replace(Regex("\\b\\d{6}\\b"), "").trim()
    
    // Remove any trailing comma or hyphen
    val cleaned = withoutPincode.replace(Regex("[,-]\\s*$"), "").trim()
    
    // If still too long, truncate and add ellipsis
    return if (cleaned.length > 20) {
        "${cleaned.take(17)}..."
    } else {
        cleaned
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