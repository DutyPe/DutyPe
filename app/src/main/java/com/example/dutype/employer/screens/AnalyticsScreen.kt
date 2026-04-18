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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

import com.example.dutype.models.JobListing
import com.example.dutype.models.ApplicationStats
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.components.CommonHeader
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.employer.models.JobStats
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController) {
    val viewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Get application statistics
    val applicationViewModel: EmployerApplicationViewModel = hiltViewModel()
    val appStats by applicationViewModel.stats.collectAsStateWithLifecycle()
    val appUiState by applicationViewModel.uiState.collectAsStateWithLifecycle()
    
    // Calculate stats directly from JobListing
    val activeJobs = uiState.myJobs.count { it.status == "open" }
    val pausedJobs = uiState.myJobs.count { it.status != "open" }
    val totalJobs = uiState.myJobs.size
    val todayJobs = uiState.myJobs.count { DateTimeUtils.isToday(it.createdAt) }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        // Common Header
        CommonHeader(
            title = "Analytics Dashboard",
            navController = navController
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Stats Grid
            item {
                OverviewStatsSection(
                    jobStats = jobStats,
                    activeJobs = activeJobs,
                    pausedJobs = pausedJobs
                )
            }
            
            // Application Stats Section
            item {
                ApplicationStatsCard(appStats = appStats)
            }
            
            // Recent Applications Section
            item {
                RecentApplicationsSection(
                    applications = appUiState.applications,
                    navController = navController
                )
            }
            
            // Recent Jobs Activity
            item {
                RecentJobsActivitySection(jobs = uiState.myJobs)
            }
        }
    }
}

// Overview Stats Section - Clean grid layout
@Composable
fun OverviewStatsSection(
    jobStats: JobStats,
    activeJobs: Int,
    pausedJobs: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Job Overview",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )
        
        // First row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Active Jobs",
                value = activeJobs.toString(),
                icon = Icons.Default.Work,
                color = Color(0xFF10B981),
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
        
        // Second row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "Today's Posts",
                value = jobStats.todayJobs.toString(),
                icon = Icons.Default.CalendarToday,
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Jobs",
                value = jobStats.totalJobs.toString(),
                icon = Icons.Default.Analytics,
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Application Stats Card
@Composable
fun ApplicationStatsCard(appStats: ApplicationStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Application Summary",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ApplicationStatItem(
                    label = "Total",
                    value = appStats.totalApplications.toString(),
                    color = Color(0xFF3B82F6)
                )
                ApplicationStatItem(
                    label = "Applied",
                    value = appStats.appliedApplications.toString(),
                    color = Color(0xFFF59E0B)
                )
                ApplicationStatItem(
                    label = "Shortlisted",
                    value = appStats.shortlistedApplications.toString(),
                    color = Color(0xFF8B5CF6)
                )
                ApplicationStatItem(
                    label = "Hired",
                    value = appStats.hiredApplications.toString(),
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
private fun ApplicationStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
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

// Recent Applications Section
@Composable
fun RecentApplicationsSection(
    applications: List<JobApplication>,
    navController: NavController
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Applications",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                TextButton(
                    onClick = { navController.navigate("employer_applications") }
                ) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            if (applications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No applications yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            } else {
                applications.take(5).forEach { application ->
                    RecentApplicationItem(
                        application = application,
                        onClick = {
                            navController.navigate("worker_profile_view/${application.workerId}")
                        }
                    )
                }
            }
        }
    }
}

// Recent Jobs Activity Section
@Composable
fun RecentJobsActivitySection(jobs: List<JobListing>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recent Job Activity",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            if (jobs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No jobs posted yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            } else {
                jobs.take(5).forEach { job ->
                    JobActivityItem(job = job)
                }
            }
        }
    }
}

@Composable
private fun JobActivityItem(job: JobListing) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (job.status == "open") Color(0xFF10B981).copy(alpha = 0.1f) 
                    else Color(0xFFF59E0B).copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (job.status == "open") Icons.Default.Work else Icons.Default.Pause,
                contentDescription = null,
                tint = if (job.status == "open") Color(0xFF10B981) else Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Posted ${DateTimeUtils.formatRelativeTime(job.createdAt)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
        
        // Status badge
        Box(
            modifier = Modifier
                .background(
                    if (job.status == "open") Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (job.status == "open") "Active" else "Paused",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (job.status == "open") Color(0xFF059669) else Color(0xFFD97706)
                )
            )
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
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
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
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
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

// NOTE: getTimeAgo() removed - use DateTimeUtils.formatRelativeTime() instead
// Import: import com.example.dutype.utils.DateTimeUtils



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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Worker ${application.workerId.takeLast(6)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "Job ${application.jobId.takeLast(6)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                )
            }

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (application.status) {
                        ApplicationStatus.APPLIED -> Color(0xFFFEF3C7)
                        ApplicationStatus.SHORTLISTED -> Color(0xFFDBEAFE)
                        ApplicationStatus.HIRED -> Color(0xFFD1FAE5)
                        ApplicationStatus.REJECTED -> Color(0xFFFEE2E2)
                    }
                )
            ) {
                Text(
                    text = application.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = when (application.status) {
                            ApplicationStatus.APPLIED -> Color(0xFF92400E)
                            ApplicationStatus.SHORTLISTED -> Color(0xFF1E40AF)
                            ApplicationStatus.HIRED -> Color(0xFF065F46)
                            ApplicationStatus.REJECTED -> Color(0xFF991B1B)
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// NOTE: isToday() removed - use DateTimeUtils.isToday() instead
// Import: import com.example.dutype.utils.DateTimeUtils

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnalyticsScreenPreview() {
    AnalyticsScreen(navController = rememberNavController())
}
