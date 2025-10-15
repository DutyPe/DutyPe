package com.example.dutype.employer.screens.applications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enterprise-level Application Management Screen for Employers
 * Professional design with comprehensive application tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerApplicationManagementScreen(
    jobId: String? = null,
    onApplicationClick: (JobApplication) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val viewModel: EmployerApplicationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showStatusFilter by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Load applications based on whether it's for a specific job or all jobs
    LaunchedEffect(jobId) {
        if (jobId != null) {
            viewModel.loadJobApplications(jobId)
        } else {
            viewModel.loadEmployerApplications()
        }
    }
    
    // Handle search
    LaunchedEffect(searchQuery) {
        viewModel.searchApplications(searchQuery)
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (jobId != null) "Job Applications" else "All Applications",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (showSearchBar) "Close Search" else "Search",
                            tint = Color(0xFF1F2937)
                        )
                    }
                    IconButton(onClick = { showStatusFilter = !showStatusFilter }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color(0xFF1F2937)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1F2937)
                ),
                modifier = Modifier // Remove any top padding
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
        // Gradient header summary (especially useful if content previously overlapped)
        ApplicationsHeaderSummary(
            jobId = jobId,
            total = stats.totalApplications,
            pending = stats.pendingApplications,
            shortlisted = stats.shortlistedApplications,
            hired = stats.hiredApplications
        )
        
        // Search Bar
        AnimatedVisibility(
            visible = showSearchBar,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search applications...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        
        // Status Filter Chips
        AnimatedVisibility(
            visible = showStatusFilter,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { viewModel.filterApplicationsByStatus(null) },
                            label = { Text("All") },
                            selected = uiState.selectedStatusFilter == null,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                    
                    items(ApplicationStatus.values()) { status ->
                        FilterChip(
                            onClick = { viewModel.filterApplicationsByStatus(status) },
                            label = { Text(getStatusDisplayName(status)) },
                            selected = uiState.selectedStatusFilter == status,
                            leadingIcon = {
                                Icon(
                                    getStatusIcon(status),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
        
        // Statistics Cards (hide if job-specific and gradient summary already conveys info)
        if (jobId == null) {
            ApplicationStatsSection(stats = stats)
        }
        
        // Applications List
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            uiState.applications.isEmpty() && !uiState.isLoading -> {
                EmptyApplicationsState()
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.applications) { application ->
                        ApplicationCard(
                            application = application,
                            onClick = { onApplicationClick(application) },
                            onStatusUpdate = { newStatus, notes ->
                                viewModel.updateApplicationStatus(
                                    applicationId = application.applicationId,
                                    newStatus = newStatus,
                                    notes = notes
                                )
                            }
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ApplicationStatsSection(stats: com.example.dutype.models.ApplicationStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Application Statistics",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StatCard(
                        title = "Total",
                        value = stats.totalApplications.toString(),
                        icon = Icons.Default.Apps,
                        color = Color(0xFF3B82F6)
                    )
                }
                item {
                    StatCard(
                        title = "Pending",
                        value = stats.pendingApplications.toString(),
                        icon = Icons.Default.Schedule,
                        color = Color(0xFFF59E0B)
                    )
                }
                item {
                    StatCard(
                        title = "Reviewed",
                        value = stats.reviewedApplications.toString(),
                        icon = Icons.Default.Visibility,
                        color = Color(0xFF8B5CF6)
                    )
                }
                item {
                    StatCard(
                        title = "Shortlisted",
                        value = stats.shortlistedApplications.toString(),
                        icon = Icons.Default.Star,
                        color = Color(0xFF10B981)
                    )
                }
                item {
                    StatCard(
                        title = "Hired",
                        value = stats.hiredApplications.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF059669)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.size(100.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
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
private fun ApplicationCard(
    application: JobApplication,
    onClick: () -> Unit,
    onStatusUpdate: (ApplicationStatus, String?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Worker Info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Picture Placeholder
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = application.workerName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        )
                        Text(
                            text = application.workerEmail,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        application.workerPhone?.let { phone ->
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                        }
                        application.workerLocation?.let { location ->
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                        }
                        application.expectedSalary?.let { salary ->
                            Text(
                                text = "Expected Salary: $salary",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                        }
                        application.availability?.let { availability ->
                            Text(
                                text = "Availability: $availability",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                        }
                    }
                }
                
                // Status Badge
                StatusBadge(status = application.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Job Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = application.jobTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151)
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "•",
                    color = Color(0xFF6B7280)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = application.companyName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Applied Date
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Applied ${getTimeAgo(application.appliedAt)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
            
            // Cover Letter Preview
            if (application.coverLetter.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Cover Letter:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151)
                    )
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = application.coverLetter.take(100) + if (application.coverLetter.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp
                    ),
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ApplicationStatus) {
    val (backgroundColor, textColor, icon) = when (status) {
        ApplicationStatus.PENDING -> Triple(
            Color(0xFFFEF3C7),
            Color(0xFFD97706),
            Icons.Default.Schedule
        )
        ApplicationStatus.UNDER_REVIEW -> Triple(
            Color(0xFFE0E7FF),
            Color(0xFF3730A3),
            Icons.Default.Visibility
        )
        ApplicationStatus.SHORTLISTED -> Triple(
            Color(0xFFD1FAE5),
            Color(0xFF059669),
            Icons.Default.Star
        )
        ApplicationStatus.REJECTED -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFFDC2626),
            Icons.Default.Close
        )
        ApplicationStatus.HIRED -> Triple(
            Color(0xFFD1FAE5),
            Color(0xFF047857),
            Icons.Default.CheckCircle
        )
        else -> Triple(
            Color(0xFFF3F4F6),
            Color(0xFF6B7280),
            Icons.Default.Help
        )
    }
    
    Row(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        
        Text(
            text = getStatusDisplayName(status),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        )
    }
}

@Composable
private fun EmptyApplicationsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Assignment,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Applications Yet",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Applications will appear here once workers start applying to your jobs.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun ApplicationsHeaderSummary(
    jobId: String?,
    total: Int,
    pending: Int,
    shortlisted: Int,
    hired: Int
) {
    val title = if (jobId == null) "All Applications" else "Job Applications"
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)),
        start = Offset.Zero,
        end = Offset.Infinite
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    HeaderStat("Total", total, Color(0xFF93C5FD), modifier = Modifier.weight(1f))
                    HeaderStat("Pending", pending, Color(0xFFFDE68A), modifier = Modifier.weight(1f))
                    HeaderStat("Shortlisted", shortlisted, Color(0xFF6EE7B7), modifier = Modifier.weight(1f))
                    HeaderStat("Hired", hired, Color(0xFFA7F3D0), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeaderStat(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .fillMaxWidth(0.6f)
                .background(accent)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.85f))
        )
    }
}

// Helper functions
private fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.REJECTED -> "Rejected"
        ApplicationStatus.HIRED -> "Hired"
        else -> "Unknown"
    }
}

private fun getStatusIcon(status: ApplicationStatus): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        ApplicationStatus.PENDING -> Icons.Default.Schedule
        ApplicationStatus.UNDER_REVIEW -> Icons.Default.Visibility
        ApplicationStatus.SHORTLISTED -> Icons.Default.Star
        ApplicationStatus.REJECTED -> Icons.Default.Close
        ApplicationStatus.HIRED -> Icons.Default.CheckCircle
        else -> Icons.Default.Help
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
