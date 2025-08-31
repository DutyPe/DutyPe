package com.example.partimes.screens.jobseekers

import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch


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
    
    val tabTitles = listOf("All", "Hourly", "Daily", "Part-time / Full-time")
    val tabIcons = listOf(
        Icons.Default.Star,
        Icons.Default.AccessTime,
        Icons.Default.CalendarToday,
        Icons.Default.Work
    )
    var jobListings by remember { mutableStateOf<List<JobListing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0)

    // Simulate fetching jobs
    LaunchedEffect(Unit) {
        isLoading = true
        jobListings = fetchAllJobs()
        isLoading = false
    }

    // Determine what location text to show
    val locationText = when {
        currentLocation != null -> {
            val city = currentLocation!!.city
            val state = currentLocation!!.state
            val postalCode = currentLocation!!.postalCode

            when {
                !city.isNullOrEmpty() && !state.isNullOrEmpty() && !postalCode.isNullOrEmpty() -> {
                    "📍$city, $state, $postalCode"
                }
                !city.isNullOrEmpty() && !state.isNullOrEmpty() -> {
                    "📍$city, $state"
                }
                !city.isNullOrEmpty() -> {
                    "📍$city"
                }
                currentLocation!!.address.isNotEmpty() -> {
                    "📍${currentLocation!!.address}"
                }
                else -> "📍Select Your Location"
            }
        }
        else -> "📍Select Your Location"
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 3.dp, vertical = 3.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedSearchBar()
                    }
                    IconButton(onClick = { /* Handle notifications */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (currentLocation == null) Color.Red else Color.Gray,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clickable {
                            // Navigate to location selection screen
                            // You can replace this with your actual location selection route
                            // navController.navigate("location_selection")
                        }
                )
            }
        },
        // Remove the bottom bar from here if you're adding it in your main app scaffold
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)  // This applies the scaffold's padding
                    .fillMaxSize()
            ) {
                // Sticky Tab Row
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier
                                .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                .height(3.dp),
                            color = Color.Black
                        )
                    },
                    modifier = Modifier.background(Color.White)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = tabIcons[index],
                                        contentDescription = title,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (pagerState.currentPage == index) Color.Black else Color.Black.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = title,
                                        color = if (pagerState.currentPage == index) Color.Black else Color.Black.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            modifier = Modifier.background(Color.White)
                        )
                    }
                }

                // Pager for swipe between tabs
                HorizontalPager(
                    count = tabTitles.size,
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    // Adding SwipeRefresh inside each page content
                    SwipeRefresh(
                        state = rememberSwipeRefreshState(isRefreshing),
                        onRefresh = {
                            coroutineScope.launch {
                                isRefreshing = true
                                jobListings = fetchAllJobs()
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 60.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                CircularProgressIndicator(color = Color.Black)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    top = 8.dp,
                                    // Add extra padding at the bottom to prevent content from being hidden by bottom bar
                                    bottom = 80.dp  // Adjust this value based on your bottom bar height
                                ),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    when (page) {
                                        0 -> {
                                            Column {
                                                if (jobListings.isEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(top = 60.dp),
                                                        contentAlignment = Alignment.TopCenter
                                                    ) {
                                                        Text(
                                                            text = "No jobs available",
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                } else {
                                                    Column {
                                                        jobListings.forEach { jobListing ->
                                                            CompactJobCard(
                                                                job = jobListing.toSummary(),
                                                                onClick = { jobId -> navController.navigate(Routes.jobDetailRoute(jobId)) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        1 -> HourlyJobsList()
                                        2 -> DailyJobsList()
                                        3 -> FullTimePartTimersList()
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 30.dp, horizontal = 13.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Jobs Made with\n \uD83D\uDC99 in Bharat\n",
                                            fontSize = 33.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 40.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}