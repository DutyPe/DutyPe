package com.example.dutype.employer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dutype.models.JobListing
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController) {
    val viewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    // Get application statistics
    val applicationViewModel: EmployerApplicationViewModel = hiltViewModel()
    val appStats by applicationViewModel.stats.collectAsStateWithLifecycle()
    
    // Calculate stats directly from JobListing
    val activeJobs = uiState.myJobs.count { it.isActive }
    val pausedJobs = uiState.myJobs.count { !it.isActive }
    val totalJobs = uiState.myJobs.size
    val todayJobs = uiState.myJobs.count { isToday(it.postedAt) }
    val totalApplications = appStats.totalApplications
    
    val jobStats = JobStats(
        activeJobs = activeJobs,
        totalApplications = totalApplications,
        todayJobs = todayJobs,
        totalJobs = totalJobs
    )

    // Load applications data when screen loads
    LaunchedEffect(Unit) {
        applicationViewModel.loadEmployerApplications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Combined Analytics Dashboard with 5 key metrics
            item {
                CombinedAnalyticsCard(
                    jobStats = jobStats,
                    activeJobs = activeJobs,
                    pausedJobs = pausedJobs,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Applications Management Section
            item {
                ApplicationsManagementSection(
                    navController = navController,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

// Data classes
data class JobStats(
    val activeJobs: Int = 0,
    val totalApplications: Int = 0,
    val todayJobs: Int = 0,
    val totalJobs: Int = 0
)

// Combined Analytics Card with 5 key metrics
@Composable
fun CombinedAnalyticsCard(
    jobStats: JobStats,
    activeJobs: Int,
    pausedJobs: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First row: Today's Posts, Applications
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Today's Posts",
                    value = jobStats.todayJobs.toString(),
                    icon = Icons.Default.CalendarToday,
                    color = Color(0xFFF57C00),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Applications",
                    value = jobStats.totalApplications.toString(),
                    icon = Icons.Default.PersonAdd,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.weight(1f)
                )
            }

            // Second row: Total Jobs, Paused Jobs
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Total Jobs",
                    value = jobStats.totalJobs.toString(),
                    icon = Icons.Default.Analytics,
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Paused Jobs",
                    value = pausedJobs.toString(),
                    icon = Icons.Default.Pause,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }

            // Third row: Active Jobs
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Active Jobs",
                    value = activeJobs.toString(),
                    icon = Icons.Default.Work,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                // Empty space to maintain layout
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
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
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
            )
        }
    }
}


@Composable
fun AnalyticsItem(
    label: String,
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
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
            )
        }
    }
}

@Composable
fun ActivityItem(
    title: String,
    time: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
                Text(
                text = time,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
            )
        }
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        else -> "${diff / (24 * 60 * 60 * 1000)}d ago"
    }
}

// Applications Management Section
@Composable
fun ApplicationsManagementSection(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val applicationViewModel: EmployerApplicationViewModel = hiltViewModel()
    val appStats by applicationViewModel.stats.collectAsStateWithLifecycle()
    val uiState by applicationViewModel.uiState.collectAsStateWithLifecycle()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAllApplications by remember { mutableStateOf(false) }
    
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
                    text = "Applications Management",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                TextButton(
                    onClick = { showAllApplications = true }
                ) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF3B82F6)
                        )
                    )
                }
            }
            
            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ApplicationStatItem(
                    label = "Total",
                    value = appStats.totalApplications.toString(),
                    icon = Icons.Default.PersonAdd,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                ApplicationStatItem(
                    label = "Pending",
                    value = appStats.pendingApplications.toString(),
                    icon = Icons.Default.Schedule,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                ApplicationStatItem(
                    label = "Shortlisted",
                    value = appStats.shortlistedApplications.toString(),
                    icon = Icons.Default.Star,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                ApplicationStatItem(
                    label = "Hired",
                    value = appStats.hiredApplications.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF059669),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Recent Applications Preview
            if (uiState.applications.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recent Applications",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )
                    )
                    
                    // Show recent applications
                    uiState.applications.take(3).forEach { application ->
                        RecentApplicationItem(
                            application = application,
                            onClick = {
                                navController.navigate("employer_application_detail/${application.applicationId}")
                            }
                        )
                    }
                    
                    if (uiState.applications.size > 3) {
                        TextButton(
                            onClick = { showAllApplications = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View ${uiState.applications.size - 3} more applications")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "No applications",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No applications yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Applications will appear here when workers apply to your jobs",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
    
    // Show full applications management screen
    if (showAllApplications) {
        EmployerApplicationManagementScreen(
            jobId = null, // Show all applications
            onApplicationClick = { application ->
                navController.navigate("employer_application_detail/${application.applicationId}")
            },
            onBackClick = { showAllApplications = false }
        )
    }
}

@Composable
fun ApplicationStatItem(
    label: String,
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
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
            )
        }
    }
}

@Composable
fun RecentApplicationItem(
    application: JobApplication,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = application.workerName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3B82F6)
                    )
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = application.workerName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "Applied for ${application.jobTitle}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                )
            }
            
            // Status Badge
    Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (application.status) {
                        ApplicationStatus.PENDING -> Color(0xFFFEF3C7)
                        ApplicationStatus.UNDER_REVIEW -> Color(0xFFDBEAFE)
                        ApplicationStatus.ACCEPTED -> Color(0xFFD1FAE5)
                        ApplicationStatus.REJECTED -> Color(0xFFFEE2E2)
                        else -> Color(0xFFF3F4F6)
                    }
                )
            ) {
                Text(
                    text = application.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = when (application.status) {
                            ApplicationStatus.PENDING -> Color(0xFF92400E)
                            ApplicationStatus.UNDER_REVIEW -> Color(0xFF1E40AF)
                            ApplicationStatus.ACCEPTED -> Color(0xFF065F46)
                            ApplicationStatus.REJECTED -> Color(0xFF991B1B)
                            else -> Color(0xFF6B7280)
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Helper function to check if a timestamp is from today
 */
private fun isToday(timestamp: Long): Boolean {
    val today = Calendar.getInstance()
    val jobDate = Calendar.getInstance()
    jobDate.timeInMillis = timestamp
    
    return today.get(Calendar.YEAR) == jobDate.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == jobDate.get(Calendar.DAY_OF_YEAR)
}
