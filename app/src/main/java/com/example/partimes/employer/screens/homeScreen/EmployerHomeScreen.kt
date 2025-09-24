package com.example.partimes.employer.screens.homeScreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.partimes.components.ScrollAwareLazyColumn
import com.example.partimes.employer.components.EmployerJobCard
import com.example.partimes.employer.models.JobPostingModel
import com.example.partimes.employer.models.enums.JobCategory
import com.example.partimes.employer.models.enums.PayType
import com.example.partimes.employer.models.enums.ShiftTiming
import com.example.partimes.employer.models.enums.JobUrgency
import com.example.partimes.employer.models.enums.JobPerk
import com.example.partimes.employer.viewmodels.EmployerViewModel
import com.example.partimes.employer.viewmodels.JobStats
import com.example.partimes.viewmodels.EmployerJobViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.partimes.auth.AuthManager
import com.example.partimes.network.ApiClient
import com.example.partimes.models.JobListing
import com.example.partimes.utils.ScrollStateManager
import com.example.partimes.utils.JobCardShimmer
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerHomeScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null,
    viewModel: EmployerJobViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val employerJobUiState by viewModel.uiState.collectAsState()
    
    // Initialize ApiClient and EmployerJobViewModel
    LaunchedEffect(Unit) {
        val authManager = AuthManager(context)
        ApiClient.initialize(authManager)
        viewModel.initialize(authManager)
        viewModel.loadMyJobs()
    }
    
    // Dashboard gradient
    val dashboardGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF2193b0), // Clean sky blue
            Color(0xFF6dd5ed), // Soft light blue
            Color(0xFFF3F7FD)  // Pure white
        ),
        startY = 0f, endY = 900f
    )

    val statusBarColor = Color(0xFF2193b0)

    // Update status bar color
    LaunchedEffect(statusBarColor) {
        onStatusBarColorChange(statusBarColor)
    }

    val recentJobs: List<JobListing> = employerJobUiState.myJobs
    val jobStats = JobStats(
        activeJobs = recentJobs.count { it.isActive },
        totalApplications = recentJobs.sumOf { it.applicationCount.toInt() },
        todayJobs = recentJobs.count { isToday(it.postedAt) },
        totalJobs = recentJobs.size
    )
    val isLoading = employerJobUiState.isLoading
    val isRefreshing = employerJobUiState.isRefreshing
    val error = employerJobUiState.error
    
    // Company name state - starts empty, will be populated from profile
    var companyName by remember { mutableStateOf("") }
    
    // Load company name from profile data
    LaunchedEffect(Unit) {
        // Simulate loading company name from profile
        // In a real app, this would come from SharedPreferences, database, or ViewModel
        companyName = "" // Start with empty, will be updated when profile is complete
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(dashboardGradient)
            .padding(top = 16.dp)
    ) {
        WelcomeHeader(
            companyName = companyName.ifEmpty { "Complete your profile" }
        )

        // Show dashboard content directly
        DashboardContent(
                recentJobs = recentJobs,
                jobStats = jobStats,
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                navController = navController,
                viewModel = viewModel,
                scrollStateManager = scrollStateManager
            )

        // Show error if any
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
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

@Composable
fun DashboardContent(
    recentJobs: List<JobListing>,
    jobStats: JobStats,
    isLoading: Boolean,
    isRefreshing: Boolean,
    navController: NavController,
    viewModel: EmployerJobViewModel,
    scrollStateManager: ScrollStateManager? = null
) {
    if (isLoading && recentJobs.isEmpty()) {
        // Show loading when first coming to the page
        LoadingScreen()
    } else {
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
            item {
                EnhancedStatsGrid(jobStats)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                RecentJobsSection(
                    jobs = recentJobs,
                    navController = navController,
                    onRefresh = { viewModel.refreshMyJobs() },
                    isRefreshing = isRefreshing,
                    onViewAllClick = { 
                        // Navigate to posted jobs screen
                        navController.navigate("employer_my_jobs")
                    },
                    onTabSwitch = { /* No longer needed */ }
                )
            }
            
        }
    }
}

//@Composable
//fun HomeTabContent(
//    recentJobs: List<JobPostingModel>,
//    jobStats: JobStats,
//    isLoading: Boolean,
//    isRefreshing: Boolean,
//    navController: NavController,
//    viewModel: EmployerViewModel,
//    onTabSwitch: (Int) -> Unit,
//    scrollStateManager: ScrollStateManager? = null
//) {
//    if (isLoading && recentJobs.isEmpty()) {
//        // Show loading when first coming to the page
//        LoadingScreen()
//    } else {
//        ScrollAwareLazyColumn(
//            modifier = Modifier.fillMaxSize(),
//            contentPadding = PaddingValues(
//                top = 16.dp,
//                start = 16.dp,
//                end = 16.dp,
//                bottom = 0.dp
//            ),
//            scrollStateManager = scrollStateManager
//        ) {
//
//
//            item {
//                EnhancedStatsGrid(jobStats)
//            }
//
//            item {
//                RecentJobsSection(
//                    jobs = recentJobs,
//                    navController = navController,
//                    onRefresh = { viewModel.refreshMyJobs() },
//                    isRefreshing = isRefreshing,
//                    onViewAllClick = { onTabSwitch(2) }, // Switch to My Jobs tab
//                    onTabSwitch = onTabSwitch
//                )
//            }
//        }
//    }
//}

@Composable
fun LoadingScreen() {
    // Shimmer setup - moved to top level so it can be used throughout the function
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Welcome header shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(16.dp)
        ) {
            Column {
                Spacer(
                    modifier = Modifier
                        .height(24.dp)
                        .fillMaxWidth(0.6f)
                        .background(brush, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Spacer(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.4f)
                        .background(brush, RoundedCornerShape(4.dp))
                )
            }
        }
        
        // Stats grid shimmer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(
                            modifier = Modifier
                                .size(40.dp)
                                .background(brush, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Spacer(
                            modifier = Modifier
                                .height(20.dp)
                                .fillMaxWidth(0.8f)
                                .background(brush, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Spacer(
                            modifier = Modifier
                                .height(14.dp)
                                .fillMaxWidth(0.6f)
                                .background(brush, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Job cards shimmer
        repeat(3) {
            JobCardShimmer()
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
    
    val isPlaceholder = companyName == "Complete your profile"

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            text = "$greeting,",
            style = MaterialTheme.typography.titleLarge.copy(color = Color.White.copy(alpha = 0.9f))
        )
        Text(
            text = companyName,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isPlaceholder) Color.White.copy(alpha = 0.7f) else Color.White
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
        elevation = CardDefaults.cardElevation(1.dp),
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
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(title: String,
             value: String,
             icon: ImageVector,
             color: Color,
             modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
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
    jobs: List<JobListing>,
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
            Column {
            Text(
                text = "Recent Job Postings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 8.dp)
            )
            }
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
                // View All button removed
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
                    .take(5) // Show more recent jobs
                    .forEach { job ->
                    // Convert JobListing to JobPostingModel for display
                    val jobPosting = JobPostingModel(
                        jobId = job.jobId,
                        title = job.title,
                        description = job.description,
                        location = job.location,
                        payAmount = job.wage?.split("/")?.get(0) ?: "0",
                        payType = when {
                            job.wage?.contains("hour", ignoreCase = true) == true -> PayType.HOURLY
                            job.wage?.contains("day", ignoreCase = true) == true -> PayType.DAILY
                            job.wage?.contains("month", ignoreCase = true) == true -> PayType.MONTHLY
                            else -> PayType.DAILY
                        },
                        category = try { JobCategory.valueOf(job.category.uppercase()) } catch (e: Exception) { JobCategory.HELPER },
                        shiftTiming = try { ShiftTiming.valueOf(job.shiftTiming.uppercase()) } catch (e: Exception) { ShiftTiming.FLEXIBLE },
                        urgency = if (job.isUrgent()) JobUrgency.URGENT else JobUrgency.FLEXIBLE,
                        vacancies = job.vacancies,
                        employerId = job.employerId,
                        employerName = job.company ?: job.employerId,
                        postedTime = job.postedAt,
                        contactNumber = job.contactNumber,
                        isActive = job.isActive,
                        applicationsReceived = job.applicationCount.toInt()
                    )
                    
                    EmployerJobCard(
                        jobPosting = jobPosting,
                        onEditClick = { jobId ->
                            navController.navigate("edit_job/$jobId")
                        },
                        onViewApplicationsClick = { jobId ->
                            navController.navigate("view_applicants/$jobId")
                        },
                        showActions = true // Show actions for better interaction
                    )
                }
                
                // View All button removed - all jobs now shown below
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
//            Button(
//                onClick = onPostJob,
//                shape = RoundedCornerShape(12.dp),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.primary
//                )
//            )
////            {
////                Icon(Icons.Default.Add, contentDescription = "Post Job")
////                Spacer(modifier = Modifier.width(8.dp))
////                Text("Post Your First Job", fontWeight = FontWeight.Bold)
////            }
        }
    }
}

// Helper function to check if a timestamp is from today
private fun isToday(timestamp: Long): Boolean {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance()
    date.timeInMillis = timestamp
    
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}