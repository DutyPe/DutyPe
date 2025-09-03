package com.example.partimes.screens.jobseekers

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.apis.fetchAllJobs
import com.example.partimes.components.AnimatedSearchBar
import com.example.partimes.components.CompactJobCard
import com.example.partimes.jobseeker.differentPartTimes.DailyJobsList
import com.example.partimes.jobseeker.differentPartTimes.FullTimePartTimersList
import com.example.partimes.jobseeker.differentPartTimes.HourlyJobsList
import com.example.partimes.models.JobListing
import com.example.partimes.models.toSummary
import com.example.partimes.navigation.Routes
import com.example.partimes.location.LocationPreferences
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

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
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val locationPreferences = remember { LocationPreferences(context) }
    val currentLocation by locationPreferences.currentLocation.collectAsState()

    val tabTitles = listOf("All Jobs", "Hourly", "Daily", "Part-time / Full-time")
    val tabIcons = listOf(
        Icons.Default.Star,
        Icons.Default.AccessTime,
        Icons.Default.CalendarToday,
        Icons.Default.Work
    )

    var uiState by remember { mutableStateOf(HomeUiState()) }
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0)
    val pullToRefreshState = rememberPullToRefreshState()

    // Enhanced job fetching with error handling
    suspend fun fetchJobsWithErrorHandling() {
        try {
            val jobs = fetchAllJobs()
            uiState = uiState.copy(
                jobListings = jobs,
                isLoading = false,
                hasError = false,
                error = null
            )
        } catch (e: Exception) {
            uiState = uiState.copy(
                isLoading = false,
                hasError = true,
                error = e.message ?: "Failed to load jobs. Please try again."
            )
        }
    }

    // Initial data loading
    LaunchedEffect(Unit) {
        fetchJobsWithErrorHandling()
    }

    // Enhanced location text with better formatting
    val locationText = remember(currentLocation) {
        when {
            currentLocation != null -> {
                val city = currentLocation!!.city
                val state = currentLocation!!.state
                val postalCode = currentLocation!!.postalCode

                when {
                    !city.isNullOrEmpty() && !state.isNullOrEmpty() && !postalCode.isNullOrEmpty() -> {
                        "$city, $state $postalCode"
                    }
                    !city.isNullOrEmpty() && !state.isNullOrEmpty() -> {
                        "$city, $state"
                    }
                    !city.isNullOrEmpty() -> {
                        city
                    }
                    currentLocation!!.address.isNotEmpty() -> {
                        currentLocation!!.address
                    }
                    else -> "Select Your Location"
                }
            }
            else -> "Select Your Location"
        }
    }

    Scaffold(
        topBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header with search and notifications
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedSearchBar()
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                // TODO: Handle notifications
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF374151)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Enhanced location selector
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // TODO: Navigate to location selection
                                // navController.navigate("location_selection")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentLocation == null)
                                Color(0xFFFEE2E2) else Color(0xFFF0FDF4)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = if (currentLocation == null) Color(0xFFDC2626) else Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = locationText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = if (currentLocation == null) Color(0xFFDC2626) else Color(0xFF059669),
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            if (currentLocation == null) {
                                Text(
                                    text = "Required",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                // Enhanced Tab Row with Material 3 components
                PrimaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        val animatedPadding by animateDpAsState(
                            targetValue = if (isSelected) 16.dp else 12.dp,
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        )

                        Tab(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            modifier = Modifier.padding(vertical = animatedPadding),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = tabIcons[index],
                                        contentDescription = title,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) Color(0xFF6366F1) else Color(0xFF6B7280)
                                    )
                                    Text(
                                        text = title,
                                        color = if (isSelected) Color(0xFF6366F1) else Color(0xFF6B7280),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        )
                    }
                }

                // Enhanced Pager with pull-to-refresh
                HorizontalPager(
                    count = tabTitles.size,
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = {
                            coroutineScope.launch {
                                uiState = uiState.copy(isRefreshing = true)
                                fetchJobsWithErrorHandling()
                                uiState = uiState.copy(isRefreshing = false)
                            }
                        },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when {
                            uiState.isLoading -> {
                                LoadingContent()
                            }
                            uiState.hasError -> {
                                ErrorContent(
                                    error = uiState.error ?: "Unknown error occurred",
                                    onRetry = {
                                        coroutineScope.launch {
                                            uiState = uiState.copy(isLoading = true, hasError = false)
                                            fetchJobsWithErrorHandling()
                                        }
                                    }
                                )
                            }
                            else -> {
                                JobsContent(
                                    page = page,
                                    jobListings = uiState.jobListings,
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF6366F1),
                strokeWidth = 3.dp
            )
            Text(
                text = "Loading amazing opportunities...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                )
            )
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
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
    navController: NavController
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = 16.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        when (page) {
            0 -> {
                if (jobListings.isEmpty()) {
                    item {
                        EmptyJobsContent()
                    }
                } else {
                    items(jobListings.size) { index ->
                        CompactJobCard(
                            job = jobListings[index].toSummary(),
                            onClick = { jobId ->
                                navController.navigate(Routes.jobDetailRoute(jobId))
                            }
                        )
                    }
                }
            }
            1 -> {
                item { HourlyJobsList() }
            }
            2 -> {
                item { DailyJobsList() }
            }
            3 -> {
                item { FullTimePartTimersList() }
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
                        fontSize = 24.sp
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
