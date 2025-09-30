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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.navigation.NavController
import com.example.partimes.components.ScrollAwareLazyColumn
import com.example.partimes.worker.components.JobCard
import com.example.partimes.worker.models.PayType
import com.example.partimes.worker.models.JobCardModel
import com.example.partimes.worker.models.PayInfo
import com.example.partimes.worker.models.LocationInfo
import com.example.partimes.worker.models.JobTag
import com.example.partimes.worker.models.TagType
import com.example.partimes.worker.models.TimeInfo
import com.example.partimes.worker.models.UrgencyLevel
import com.example.partimes.location.LocationPreferences
import com.example.partimes.models.JobListing
import com.example.partimes.navigation.Routes
import com.example.partimes.ui.theme.WorkerGradientBackground
import com.example.partimes.ui.components.ReusableSearchBar
import com.example.partimes.utils.JobCardShimmer
import com.example.partimes.utils.ScrollStateManager
import com.example.partimes.viewmodels.SavedJobsViewModel
import com.example.partimes.viewmodels.JobViewModel
import com.example.partimes.viewmodels.ProfileViewModel
import com.example.partimes.auth.AuthManager
import com.example.partimes.network.ApiClient
import com.example.partimes.notifications.viewmodels.NotificationCenterViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.partimes.R
import com.example.partimes.data.ApplicationFormDataStore
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState

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
    val jobViewModel: JobViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val dataStore: ApplicationFormDataStore = remember { ApplicationFormDataStore(context) }
    val notificationViewModel: NotificationCenterViewModel = hiltViewModel()
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    val jobUiState by jobViewModel.uiState.collectAsState()
    val profileUiState by profileViewModel.uiState.collectAsState()
    
    // Initialize ApiClient and load data
    LaunchedEffect(Unit) {
        val authManager = AuthManager(context)
        ApiClient.initialize(authManager)
        jobViewModel.loadJobs()
        profileViewModel.loadProfile()
    }
    
    // WhatsApp sharing function
    val shareToWhatsApp = {
        val packageManager = context.packageManager
        val appPackageName = context.packageName
        
        try {
            // Try to open WhatsApp directly
            val whatsappIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
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
                    !city.isNullOrEmpty() && !state.isNullOrEmpty() -> {
                        "$city, $state"
                    }
                    !city.isNullOrEmpty() -> {
                        city
                    }
                    currentLocation!!.address.isNotEmpty() -> {
                        // Extract city from address if available
                        val addressParts = currentLocation!!.address.split(",")
                        if (addressParts.isNotEmpty()) {
                            addressParts[0].trim()
                        } else {
                        currentLocation!!.address
                }
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
                // Welcome message with user's name
                val backendUser = profileUiState.user
                val personalInfo = remember { dataStore.getPersonalInfo() }
                val userName = when {
                    backendUser?.fullName?.isNotBlank() == true -> backendUser.fullName
                    personalInfo.fullName.isNotBlank() -> personalInfo.fullName
                    else -> "User"
                }
                
                Text(
                    text = "Welcome back, $userName! 👋",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                                text = "Hyderabad",
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
                                    savedJobsViewModel = savedJobsViewModel
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
private fun JobsContent(
    page: Int,
    jobListings: List<JobListing>,
    navController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    scrollStateManager: ScrollStateManager? = null
) {
    // Convert JobListing to JobCardModel for display
    val jobCards = remember(jobListings) {
        jobListings.map { jobListing ->
            JobCardModel(
                jobId = jobListing.id,
                title = jobListing.title,
                employerName = jobListing.companyName,
                payInfo = PayInfo(
                    amount = (jobListing.payAmount.ifEmpty { jobListing.salary }).ifEmpty {
                        if (jobListing.payRate > 0.0) jobListing.payRate.toInt().toString() else ""
                    },
                    type = when {
                        jobListing.payType.equals("HOURLY", true) || jobListing.payType.contains("hour", true) -> PayType.HOURLY
                        jobListing.payType.equals("DAILY", true) || jobListing.payType.contains("day", true) -> PayType.DAILY
                        jobListing.payType.equals("MONTHLY", true) || jobListing.payType.contains("month", true) -> PayType.MONTHLY
                        else -> PayType.DAILY
                    },
                    period = "" // computed in PayInfo.getDisplayText
                ),
                location = LocationInfo(
                    area = jobListing.area ?: jobListing.location,
                    city = jobListing.city ?: jobListing.location,
                    distance = "2.5"
                ),
                tags = listOf(
                    JobTag(
                        text = jobListing.jobType,
                        emoji = "💼",
                        type = TagType.BENEFIT
                    ),
                    JobTag(
                        text = jobListing.category,
                        emoji = "🏷️",
                        type = TagType.BENEFIT
                    )
                ),
                timeInfo = TimeInfo(
                    postedTime = jobListing.postedDate,
                    urgency = if (jobListing.isUrgent()) UrgencyLevel.URGENT else UrgencyLevel.NORMAL
                ),
                isVerifiedEmployer = jobListing.isVerified,
                phoneNumber = jobListing.contactNumber,
                description = jobListing.description,
                requirements = jobListing.requirements,
                benefits = jobListing.benefits,
                workingHours = jobListing.workingHours,
                experienceRequired = jobListing.experienceLevel,
                ageRange = jobListing.ageRange,
                gender = jobListing.gender,
                vacancies = jobListing.vacancies,
                jobType = jobListing.jobType,
                applicationDeadline = jobListing.applicationDeadline,
                companySize = jobListing.companySize,
                industry = jobListing.industry,
                viewCount = jobListing.viewCount.toInt(),
                applicationCount = jobListing.applicationCount.toInt(),
                isBookmarked = false, // TODO: Get from WorkerJobInteraction
                isApplied = false // TODO: Get from WorkerJobInteraction
            )
        }
    }
    
    ScrollAwareLazyColumn(
        contentPadding = PaddingValues(
            top = 13.dp,
            start = 13.dp,
            end = 13.dp,
            bottom = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        scrollStateManager = scrollStateManager,
        modifier = Modifier.fillMaxSize()
    ) {
        when (page) {
            0 -> {
                // Show all jobs
                if (jobCards.isEmpty()) {
                    item {
                        EmptyTabContent(
                            title = "No Jobs Available",
                            message = "Check back later for new opportunities.",
                            icon = "🔍"
                        )
                    }
                } else {
                    items(jobCards.size) { index ->
                    JobCard(
                            jobCard = jobCards[index],
                        onApplyClick = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        },
                        onSaveClick = { jobId ->
                            savedJobsViewModel.saveJob(jobId)
                        },
                        onCardClick = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        }
                    )
                    }
                }
            }
            1 -> {
                // Filter hourly jobs
                val hourlyJobs = jobCards.filter { 
                    it.payInfo.type == PayType.HOURLY
                }
                if (hourlyJobs.isEmpty()) {
                    item {
                        EmptyTabContent(
                            title = "No Hourly Jobs",
                            message = "No hourly jobs available at the moment.",
                            icon = "⏰"
                        )
                    }
                } else {
                items(hourlyJobs.size) { index ->
                    JobCard(
                        jobCard = hourlyJobs[index],
                        onApplyClick = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        },
                        onSaveClick = { jobId ->
                            savedJobsViewModel.saveJob(jobId)
                        },
                        onCardClick = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        }
                    )
                    }
                }
            }
            2 -> {
                // Filter daily jobs
                val dailyJobs = jobCards.filter { 
                    it.payInfo.type == PayType.DAILY
                }
                if (dailyJobs.isEmpty()) {
                    item {
                        EmptyTabContent(
                            title = "No Daily Jobs",
                            message = "No daily jobs available at the moment.",
                            icon = "📅"
                        )
                    }
                } else {
                items(dailyJobs.size) { index ->
                    JobCard(
                        jobCard = dailyJobs[index],
                        onApplyClick = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        },
                        onSaveClick = { jobId ->
                            savedJobsViewModel.saveJob(jobId)
                        },
                        onCardClick = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        }
                    )
                    }
                }
            }
            3 -> {
                // Filter monthly/per-task jobs
                val fullTimePartTimeJobs = jobCards.filter { 
                    it.payInfo.type == PayType.MONTHLY ||
                    it.payInfo.type == PayType.PER_TASK
                }
                if (fullTimePartTimeJobs.isEmpty()) {
                    item {
                        EmptyTabContent(
                            title = "No Full-time Jobs",
                            message = "No full-time or monthly jobs available at the moment.",
                            icon = "💼"
                        )
                    }
                } else {
                items(fullTimePartTimeJobs.size) { index ->
                    JobCard(
                        jobCard = fullTimePartTimeJobs[index],
                        onApplyClick = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        },
                        onSaveClick = { jobId ->
                            savedJobsViewModel.saveJob(jobId)
                        },
                        onCardClick = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        }
                    )
                    }
                }
            }
        }

        item {
            FooterContent()
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
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
    savedJobsViewModel: SavedJobsViewModel
) {
    // Convert JobListing to JobCardModel
    val jobCards = remember(jobListings) {
        jobListings.map { job ->
            JobCardModel(
                jobId = job.id,
                title = job.title,
                employerName = job.companyName,
                payInfo = PayInfo(
                    amount = (job.payAmount.ifEmpty { job.salary }).ifEmpty {
                        if (job.payRate > 0.0) job.payRate.toInt().toString() else ""
                    },
                    type = when {
                        job.payType.equals("HOURLY", true) || job.payType.contains("hour", true) -> PayType.HOURLY
                        job.payType.equals("DAILY", true) || job.payType.contains("day", true) -> PayType.DAILY
                        job.payType.equals("MONTHLY", true) || job.payType.contains("month", true) -> PayType.MONTHLY
                        else -> PayType.DAILY
                    },
                    period = "" // computed in PayInfo.getDisplayText
                ),
                location = LocationInfo(
                    area = job.area ?: job.location,
                    city = job.city ?: job.location,
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
                savedJobsViewModel = savedJobsViewModel
            )
        }
        
        // Hourly Jobs Section
        item {
            JobSection(
                title = "Hourly Jobs",
                jobs = jobCards.filter { it.payInfo.type == PayType.HOURLY },
                navController = navController,
                savedJobsViewModel = savedJobsViewModel
            )
        }
        
        // Daily Jobs Section
        item {
            JobSection(
                title = "Daily Jobs",
                jobs = jobCards.filter { it.payInfo.type == PayType.DAILY },
                navController = navController,
                savedJobsViewModel = savedJobsViewModel
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
                savedJobsViewModel = savedJobsViewModel
            )
        }
    }
}

@Composable
private fun JobSection(
    title: String,
    jobs: List<JobCardModel>,
    navController: NavController,
    savedJobsViewModel: SavedJobsViewModel
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
                    onApplyClick = { jobId ->
                        navController.navigate(Routes.jobDetailRoute(jobId))
                    },
                    onSaveClick = { jobId ->
                        savedJobsViewModel.saveJob(jobId)
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

//@Preview(showBackground = true)
//@Composable
//fun WorkerHomeScreenPreview() {
//    WorkerHomeScreen(
//        navController = NavController(LocalContext.current),
//        onStatusBarColorChange = {}
//    )
//}