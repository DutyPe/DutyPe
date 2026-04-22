package com.example.dutype.employer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerHistoryScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val employerJobViewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val uiState by employerJobViewModel.uiState.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_timeline),
        stringResource(R.string.tab_active),
        stringResource(R.string.tab_expired),
        stringResource(R.string.tab_all_jobs)
    )
    
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        try {
            employerJobViewModel.loadMyJobs()
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error loading jobs in EmployerHistoryScreen")
        }
    }

    val currentTime = System.currentTimeMillis()
    
    // Filter jobs based on selected tab - with null safety
    val filteredJobs = remember(uiState.myJobs, selectedTab) {
        try {
            when (selectedTab) {
                0 -> uiState.myJobs.sortedByDescending { it.createdAt } // Timeline - all sorted by date
                1 -> uiState.myJobs.filter { 
                    // Active jobs that haven't expired (using calculated expiry)
                    it.status == "open" && !it.isExpired()
                }
                2 -> uiState.myJobs.filter { 
                    // Expired jobs (using calculated expiry)
                    it.isExpired()
                }
                3 -> uiState.myJobs // All
                else -> uiState.myJobs
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error filtering jobs")
            emptyList()
        }
    }
    
    // Group jobs by month for timeline view - with null safety
    val groupedJobs = remember(filteredJobs) {
        try {
            filteredJobs.groupBy { job ->
                val calendar = Calendar.getInstance().apply { timeInMillis = job.createdAt }
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(calendar.time).uppercase()
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error grouping jobs")
            emptyMap()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        // Common Header
        CommonHeader(
            title = stringResource(R.string.job_posting_history),
            navController = navController
        )
        
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF3B82F6),
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF3B82F6),
                    height = 3.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
        
        // Content
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            }
            uiState.hasError -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = uiState.error ?: stringResource(R.string.history_failed_load_jobs),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { employerJobViewModel.loadMyJobs() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6)
                            )
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            filteredJobs.isEmpty() -> {
                EmptyHistoryState(selectedTab = selectedTab)
            }
            else -> {
            when (selectedTab) {
                0 -> {
                    // Timeline View - LinkedIn style
                    TimelineView(
                        groupedJobs = groupedJobs,
                        currentTime = currentTime,
                        onJobClick = { job ->
                            navController.navigate(Routes.employerApplicationsJobRoute(job.id))
                        }
                    )
                }
                else -> {
                    // List View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredJobs,
                            key = { job -> "emphistory_${job.id}" } // CRITICAL FIX: Unique key to prevent LazyColumn crashes
                        ) { job ->
                            HistoryJobCard(
                                job = job,
                                currentTime = currentTime,
                                onClick = {
                                    navController.navigate(Routes.employerApplicationsJobRoute(job.id))
                                }
                            )
                        }
                    }
                }
            }
            }  // Close else block
        }  // Close outer when
    }
}

@Composable
private fun TimelineView(
    groupedJobs: Map<String, List<JobListing>>,
    currentTime: Long,
    onJobClick: (JobListing) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
    ) {
        groupedJobs.forEach { (monthYear, jobs) ->
            // Month Header
            item(key = "header_$monthYear") {
                MonthHeader(monthYear = monthYear)
            }
            
            // Timeline items for this month
            items(
                items = jobs,
                key = { it.id }
            ) { job ->
                val isLastInMonth = jobs.last() == job
                TimelineJobCard(
                    job = job,
                    currentTime = currentTime,
                    isLastInMonth = isLastInMonth,
                    onClick = { onJobClick(job) }
                )
            }
            
            // Spacer between months
            item(key = "spacer_$monthYear") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MonthHeader(monthYear: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF3B82F6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = monthYear,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
private fun TimelineJobCard(
    job: JobListing,
    currentTime: Long,
    isLastInMonth: Boolean,
    onClick: () -> Unit
) {
    val lineColor = Color(0xFFE5E7EB)
    val isExpired = job.isExpired() // Use calculated expiry
    val isPaused = job.status != "open"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Draw vertical timeline line
                if (!isLastInMonth) {
                    drawLine(
                        color = lineColor,
                        start = Offset(20.dp.toPx(), 40.dp.toPx()),
                        end = Offset(20.dp.toPx(), size.height),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }
            }
    ) {
        // Timeline dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isExpired -> Color(0xFFEF4444)
                            isPaused -> Color(0xFF6B7280)
                            else -> Color(0xFF10B981)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isExpired -> Icons.Default.EventBusy
                        isPaused -> Icons.Default.Pause
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Job Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isExpired || isPaused) Color(0xFFF9FAFB) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Status badge and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JobStatusBadge(
                        isActive = job.status == "open",
                        isExpired = isExpired
                    )
                    
                    Text(
                        text = formatTimelineDate(job.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9CA3AF)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Job title
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isExpired || isPaused) Color(0xFF6B7280) else Color(0xFF111827)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Category - auto-detected
                Text(
                    text = job.getCategory(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Job details row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(
                        icon = Icons.Default.CurrencyRupee,
                        text = "₹${job.salary.ifBlank { "-" }}",
                        backgroundColor = Color(0xFFECFDF5),
                        iconColor = Color(0xFF10B981)
                    )
                    
                    InfoChip(
                        icon = Icons.Default.LocationOn,
                        text = job.addressText.ifBlank { job.location }.take(15),
                        backgroundColor = Color(0xFFF3F4F6),
                        iconColor = Color(0xFF6B7280)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                    // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Applications count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.history_view_applications),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3B82F6)
                            )
                        )
                    }
                    
                    // Expiry info - using calculated expiry (30 days from postedAt)
                    val expiresAt = job.expiresAt
                    val daysLeft = ((expiresAt - currentTime) / (24 * 60 * 60 * 1000)).toInt()
                    Text(
                        text = when {
                            isExpired -> stringResource(R.string.history_expired)
                            daysLeft == 0 -> stringResource(R.string.history_expires_today)
                            daysLeft == 1 -> stringResource(R.string.history_1_day_left)
                            else -> stringResource(R.string.history_days_left, daysLeft)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isExpired) Color(0xFFEF4444) 
                                   else if (daysLeft <= 2) Color(0xFFF59E0B)
                                   else Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(selectedTab: Int) {
    val (message, subMessage, icon) = when (selectedTab) {
        0 -> Triple(stringResource(R.string.history_no_job_posting), stringResource(R.string.history_start_posting_timeline), Icons.Default.Timeline)
        1 -> Triple(stringResource(R.string.history_no_active_jobs), stringResource(R.string.history_active_postings_here), Icons.Default.CheckCircle)
        2 -> Triple(stringResource(R.string.history_no_expired_jobs), stringResource(R.string.history_expired_postings_here), Icons.Default.EventBusy)
        3 -> Triple(stringResource(R.string.history_no_jobs_posted), stringResource(R.string.history_start_posting_here), Icons.Default.WorkHistory)
        else -> Triple(stringResource(R.string.history_no_jobs), stringResource(R.string.history_postings_appear_here), Icons.Default.WorkHistory)
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = subMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF9CA3AF)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HistoryJobCard(
    job: JobListing,
    currentTime: Long,
    onClick: () -> Unit
) {
    val isExpired = job.isExpired() // Use calculated expiry
    val isPaused = job.status != "open"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired || isPaused) Color(0xFFF9FAFB) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired || isPaused) Color(0xFF6B7280) else Color(0xFF111827)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = job.getCategory(), // Use auto-detected category
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
                
                JobStatusBadge(
                    isActive = job.status == "open",
                    isExpired = isExpired
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.LocationOn,
                    text = job.addressText.ifBlank { job.location }.take(20),
                    backgroundColor = Color(0xFFF3F4F6),
                    iconColor = Color(0xFF6B7280)
                )
                InfoChip(
                    icon = Icons.Default.CurrencyRupee,
                    text = "₹${job.salary.ifBlank { "-" }}",
                    backgroundColor = Color(0xFFECFDF5),
                    iconColor = Color(0xFF10B981)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.history_view_applications),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    )
                }
                
                // Expiry info - using calculated expiry (30 days from postedAt)
                val expiresAt = job.expiresAt
                val daysLeft = ((expiresAt - currentTime) / (24 * 60 * 60 * 1000)).toInt()
                Text(
                    text = when {
                        isExpired -> stringResource(R.string.history_expired)
                        daysLeft == 0 -> stringResource(R.string.history_expires_today)
                        daysLeft == 1 -> stringResource(R.string.history_1_day_left)
                        else -> stringResource(R.string.history_days_left, daysLeft)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isExpired) Color(0xFFEF4444) 
                               else if (daysLeft <= 2) Color(0xFFF59E0B)
                               else Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.history_posted_date, formatDate(job.createdAt)),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF)
                )
            )
        }
    }
}

@Composable
private fun JobStatusBadge(
    isActive: Boolean,
    isExpired: Boolean
) {
    val (color, text, icon) = when {
        isExpired -> Triple(Color(0xFFEF4444), stringResource(R.string.history_expired), Icons.Default.EventBusy)
        !isActive -> Triple(Color(0xFF6B7280), stringResource(R.string.history_status_paused), Icons.Default.Pause)
        else -> Triple(Color(0xFF10B981), stringResource(R.string.history_status_active), Icons.Default.CheckCircle)
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    backgroundColor: Color = Color(0xFFF3F4F6),
    iconColor: Color = Color(0xFF6B7280)
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF374151),
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// NOTE: formatDate() removed - use DateTimeUtils.formatRelativeTime() instead
private fun formatDate(timestamp: Long): String {
    return DateTimeUtils.formatRelativeTime(timestamp)
}

private fun formatTimelineDate(timestamp: Long): String {
    return DateTimeUtils.formatDate(timestamp)
}
