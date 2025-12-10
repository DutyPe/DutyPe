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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Custom Header with search and filter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = if (jobId != null) "Job Applications" else "All Applications",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = { showSearchBar = !showSearchBar }) {
                    Icon(
                        imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (showSearchBar) "Close Search" else "Search",
                        tint = Color(0xFF3B82F6)
                    )
                }
                IconButton(onClick = { showStatusFilter = !showStatusFilter }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = Color(0xFF3B82F6)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
        }
        
        // Stats Summary Card
        ApplicationStatsSummary(stats = stats)
        
        // Search Bar
        AnimatedVisibility(
            visible = showSearchBar,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or job title...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF6B7280))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF6B7280))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
        }
        
        // Status Filter Chips
        AnimatedVisibility(
            visible = showStatusFilter,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { viewModel.filterApplicationsByStatus(null) },
                        label = { Text("All") },
                        selected = uiState.selectedStatusFilter == null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B82F6),
                            selectedLabelColor = Color.White
                        )
                    )
                }
                
                items(ApplicationStatus.values()) { status ->
                    FilterChip(
                        onClick = { viewModel.filterApplicationsByStatus(status) },
                        label = { Text(getStatusDisplayName(status)) },
                        selected = uiState.selectedStatusFilter == status,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = getStatusColor(status),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
        
        // Applications List
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            uiState.applications.isEmpty() && !uiState.isLoading -> {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyApplicationsState()
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.applications) { application ->
                        ApplicationCard(
                            application = application,
                            onClick = { 
                                // Update status to Under Review when employer clicks on application
                                if (application.status == ApplicationStatus.PENDING) {
                                    viewModel.updateApplicationStatus(
                                        applicationId = application.applicationId,
                                        newStatus = ApplicationStatus.UNDER_REVIEW,
                                        notes = "Application viewed by employer"
                                    )
                                }
                                onApplicationClick(application) 
                            },
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

// Stats Summary Card
@Composable
private fun ApplicationStatsSummary(stats: com.example.dutype.models.ApplicationStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatsSummaryItem(
                value = stats.totalApplications.toString(),
                label = "Total",
                color = Color(0xFF3B82F6)
            )
            StatsSummaryItem(
                value = stats.pendingApplications.toString(),
                label = "Pending",
                color = Color(0xFFF59E0B)
            )
            StatsSummaryItem(
                value = stats.reviewedApplications.toString(),
                label = "Reviewed",
                color = Color(0xFF8B5CF6)
            )
            StatsSummaryItem(
                value = stats.hiredApplications.toString(),
                label = "Hired",
                color = Color(0xFF10B981)
            )
        }
    }
}

@Composable
private fun StatsSummaryItem(
    value: String,
    label: String,
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

// Helper function to get status color
private fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFF59E0B)
        ApplicationStatus.UNDER_REVIEW -> Color(0xFF8B5CF6)
        ApplicationStatus.ACCEPTED -> Color(0xFF10B981)
        ApplicationStatus.REJECTED -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }
}

@Composable
private fun ApplicationCard(
    application: JobApplication,
    onClick: () -> Unit,
    onStatusUpdate: (ApplicationStatus, String?) -> Unit
) {
    // Determine display name - fallback to email or "Unknown Worker" if name is empty
    val displayName = when {
        application.workerName.isNotBlank() -> application.workerName
        application.workerEmail.isNotBlank() -> application.workerEmail.substringBefore("@")
        else -> "Unknown Worker"
    }
    
    // Get initials for avatar
    val initials = when {
        displayName.isNotBlank() && displayName != "Unknown Worker" -> {
            displayName.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .ifEmpty { displayName.take(1).uppercase() }
        }
        else -> "?"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row - Worker info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Profile Avatar with image support
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!application.workerProfileImageUrl.isNullOrBlank()) {
                            // Show profile image if available
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = application.workerProfileImageUrl
                                ),
                                contentDescription = "Worker Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Show initials or icon
                            if (initials.isNotBlank() && initials != "?") {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B82F6)
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (application.workerEmail.isNotBlank()) {
                            Text(
                                text = application.workerEmail,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Status Badge
                StatusBadge(status = application.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Job Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(18.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = application.jobTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1F2937)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = application.companyName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer - Applied time and contact info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Applied ${getTimeAgo(application.appliedAt)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9CA3AF)
                        )
                    )
                }
                
                // Phone if available
                application.workerPhone?.let { phone ->
                    if (phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }
                    }
                }
            }
            
            // Cover Letter Preview (if available)
            if (application.coverLetter.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = application.coverLetter.take(120) + if (application.coverLetter.length > 120) "..." else "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
        ApplicationStatus.ACCEPTED -> Triple(
            Color(0xFFD1FAE5),
            Color(0xFF059669),
            Icons.Default.Check
        )
        ApplicationStatus.REJECTED -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFFDC2626),
            Icons.Default.Close
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
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFFF3F4F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "No Applications Yet",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
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



// Helper functions
private fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.REJECTED -> "Rejected"
        else -> "Unknown"
    }
}

private fun getStatusIcon(status: ApplicationStatus): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        ApplicationStatus.PENDING -> Icons.Default.Schedule
        ApplicationStatus.UNDER_REVIEW -> Icons.Default.Visibility
        ApplicationStatus.ACCEPTED -> Icons.Default.Check
        ApplicationStatus.REJECTED -> Icons.Default.Close
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
