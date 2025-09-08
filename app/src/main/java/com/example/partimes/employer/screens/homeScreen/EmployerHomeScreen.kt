package com.example.partimes.employer.screens.homeScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.partimes.employer.components.EmployerJobCard
import com.example.partimes.employer.models.JobPostingModel
import com.example.partimes.employer.screens.postedJobs.PostedJobsScreen
import com.example.partimes.employer.screens.postjob.PostJobScreen
import com.example.partimes.employer.viewmodels.EmployerViewModel
import com.example.partimes.employer.viewmodels.JobStats
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerHomeScreen(
    navController: NavController,
    viewModel: EmployerViewModel = viewModel()
) {
    // Define different background gradients for each tab
    val homeGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A237E),  // Deep indigo
            Color(0xFF283593),  // Indigo
            Color(0xFFE3F2FD)   // Light blue background
        ),
        startY = 0f,
        endY = 900f
    )

    val postJobGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1B5E20),  // Deep green
            Color(0xFF388E3C),  // Green
            Color(0xFFE8F5E9)   // Light green background
        ),
        startY = 0f,
        endY = 900f
    )

    val myJobsGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE65100),  // Deep orange
            Color(0xFFF57C00),  // Orange
            Color(0xFFFFF3E0)   // Light orange background
        ),
        startY = 0f,
        endY = 900f
    )

    // State for tabs - Use rememberSaveable for stability
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = remember { listOf("Home", "Post Job", "My Jobs") }

    // Get current tab background gradient
    val currentGradient = when (selectedTabIndex) {
        0 -> homeGradient
        1 -> postJobGradient
        2 -> myJobsGradient
        else -> homeGradient
    }

    val recentJobs by viewModel.recentJobs.collectAsStateWithLifecycle()
    val jobStats by viewModel.jobStats.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // Show error snackbar
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Handle error display
            viewModel.clearError()
        }
    }

    // Use Box layout similar to JobseekerProfileScreen for edge-to-edge support
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // WelcomeHeader - moved inside column without extra padding
            WelcomeHeader("TechCorp Solutions")

            // Fixed Tab Row with different indicator colors for each tab
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty() && selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = when (selectedTabIndex) {
                                    0 -> Color(0xFF1976D2) // Blue for Home
                                    1 -> Color(0xFF388E3C) // Green for Post Job
                                    2 -> Color(0xFFF57C00) // Orange for My Jobs
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                height = 3.dp
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Home
                                        1 -> Icons.Default.Add
                                        else -> Icons.Default.Work
                                    },
                                    contentDescription = title,
                                    tint = if (selectedTabIndex == index) {
                                        when (index) {
                                            0 -> Color(0xFF1976D2) // Blue for Home
                                            1 -> Color(0xFF388E3C) // Green for Post Job
                                            2 -> Color(0xFFF57C00) // Orange for My Jobs
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) {
                                        when (index) {
                                            0 -> Color(0xFF1976D2) // Blue for Home
                                            1 -> Color(0xFF388E3C) // Green for Post Job
                                            2 -> Color(0xFFF57C00) // Orange for My Jobs
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                if (index == 2 && recentJobs.isNotEmpty()) {
                                    Badge(
                                        containerColor = Color(0xFFF57C00) // Orange badge for My Jobs
                                    ) {
                                        Text(
                                            text = recentJobs.size.toString(),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab Content - No extra Box wrapper
            when (selectedTabIndex) {
                0 -> {
                    // Home Tab - Original dashboard content
                    HomeTabContent(
                        recentJobs = recentJobs,
                        jobStats = jobStats,
                        isLoading = isLoading,
                        isRefreshing = isRefreshing,
                        error = error,
                        navController = navController,
                        viewModel = viewModel,
                        onTabSwitch = { tabIndex -> selectedTabIndex = tabIndex }
                    )
                }
                1 -> {
                    // Post Job Tab
                    PostJobScreen(
                        navController = navController,
                        onJobPosted = {
                            selectedTabIndex = 2 // Switch to My Jobs after posting
                            viewModel.refreshJobs()
                        }
                    )
                }
                2 -> {
                    // My Jobs Tab
                    PostedJobsScreen(
                        navController = navController,
                        viewModel = viewModel
                    )
                }
            }
        }

        // Error snackbar - only show on Home tab
        if (selectedTabIndex == 0) {
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTabContent(
    recentJobs: List<JobPostingModel>,
    jobStats: JobStats,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String?,
    navController: NavController,
    viewModel: EmployerViewModel,
    onTabSwitch: (Int) -> Unit
) {
    if (isLoading && recentJobs.isEmpty()) {
        // Show loading when first coming to the page
        LoadingScreen()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {


            item {
                EnhancedStatsGrid(jobStats)
            }

            item {
                RecentJobsSection(
                    jobs = recentJobs,
                    navController = navController,
                    onRefresh = { viewModel.refreshJobs() },
                    isRefreshing = isRefreshing,
                    onViewAllClick = { onTabSwitch(2) }, // Switch to My Jobs tab
                    onTabSwitch = onTabSwitch
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading your dashboard...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}

@Composable
fun WelcomeHeader(companyName: String) {
    val currentTime = Calendar.getInstance()
    val greeting = when (currentTime.get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            text = "$greeting,",
            style = MaterialTheme.typography.titleLarge.copy(color = Color.White.copy(alpha = 0.9f))
        )
        Text(
            text = companyName,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date()),
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.8f))
        )
    }
}

@Composable
fun EnhancedStatsGrid(stats: JobStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your Dashboard",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Active Jobs",
                    value = stats.activeJobs.toString(),
                    icon = Icons.Default.Work,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Applications",
                    value = stats.totalApplications.toString(),
                    icon = Icons.Default.PersonAdd,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Today's Posts",
                    value = stats.todayJobs.toString(),
                    icon = Icons.Default.CalendarToday,
                    color = Color(0xFFF57C00),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Jobs",
                    value = stats.totalJobs.toString(),
                    icon = Icons.Default.Analytics,
                    color = Color(0xFF7B1FA2),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RecentJobsSection(
    jobs: List<JobPostingModel>,
    navController: NavController,
    onRefresh: () -> Unit,
    isRefreshing: Boolean = false,
    onViewAllClick: () -> Unit,
    onTabSwitch: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Job Postings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
                TextButton(onClick = onViewAllClick) {
                    Text("View All", fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ArrowForward, contentDescription = "View All")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Show refresh indicator at the fetching place
        if (isRefreshing && jobs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Refreshing recent jobs...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (jobs.isEmpty()) {
            EmptyJobsState(onPostJob = { onTabSwitch(1) })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                jobs
                    .take(3)
                    .forEach { job ->
                    EmployerJobCard(
                        jobPosting = job,
                        onEditClick = { jobId ->
                            navController.navigate("edit_job/$jobId")
                        },
                        onViewApplicationsClick = { jobId ->
                            navController.navigate("view_applicants/$jobId")
                        },
                        showActions = false
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyJobsState(onPostJob: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Work,
                contentDescription = "No jobs",
                modifier = Modifier.size(64.dp),
                tint = Color.Gray.copy(alpha = 0.6f)
            )
            Text(
                text = "No Recent Jobs",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start by posting your first job to find great candidates",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onPostJob,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post Job")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Post Your First Job", fontWeight = FontWeight.Bold)
            }
        }
    }
}