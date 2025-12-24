package com.example.dutype.employer.screens.history

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
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerHistoryScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val employerJobViewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val uiState by employerJobViewModel.uiState.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Timeline", "Active", "Expired", "All Jobs")
    
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        employerJobViewModel.loadMyJobs()
    }
    
    val currentTime = System.currentTimeMillis()
    
    // Filter jobs based on selected tab
    val filteredJobs = remember(uiState.myJobs, selectedTab, currentTime) {
        when (selectedTab) {
            0 -> uiState.myJobs.sortedByDescending { it.postedAt } // Timeline - all sorted by date
            1 -> uiState.myJobs.filter { 
                it.isActive && (it.expiresAt == 0L || it.expiresAt > currentTime)
            }
            2 -> uiState.myJobs.filter { 
                it.expiresAt > 0L && it.expiresAt <= currentTime
            }
            3 -> uiState.myJobs // All
            else -> uiState.myJobs
        }
    }
    
    // Group jobs by month for timeline view
    val groupedJobs = remember(filteredJobs) {
        filteredJobs.groupBy { job ->
            val calendar = Calendar.getInstance().apply { timeInMillis = job.postedAt }
            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(calendar.time).uppercase()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Common Header
        CommonHeader(
            title = "Job Posting History",
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        } else if (filteredJobs.isEmpty()) {
            EmptyHistoryState(selectedTab = selectedTab)
        } else {
            when (selectedTab) {
                0 -> {
                    // Timeline View - LinkedIn style
                    TimelineView(
                        groupedJobs = groupedJobs,
                        currentTime = currentTime,
                        onJobClick = { job ->
                            navController.navigate(Routes.viewApplicantsRoute(job.id))
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
                        items(filteredJobs) { job ->
                            HistoryJobCard(
                                job = job,
                                currentTime = currentTime,
                                onClick = {
                                    navController.navigate(Routes.viewApplicantsRoute(job.id))
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
    val isExpired = job.expiresAt > 0L && job.expiresAt <= currentTime
    val isPaused = !job.isActive
    
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
                        isActive = job.isActive,
                        isExpired = isExpired
                    )
                    
                    Text(
                        text = formatTimelineDate(job.postedAt),
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
                
                // Category
                Text(
                    text = job.category,
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
                        text = "₹${job.payAmount}",
                        backgroundColor = Color(0xFFECFDF5),
                        iconColor = Color(0xFF10B981)
                    )
                    
                    InfoChip(
                        icon = Icons.Default.LocationOn,
                        text = job.location.take(15),
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
                            text = "${job.applicationCount.toInt()} applications",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF3B82F6)
                            )
                        )
                    }
                    
                    // Expiry info
                    if (job.expiresAt > 0L) {
                        val daysLeft = job.getDaysUntilExpiry()
                        Text(
                            text = when {
                                isExpired -> "Expired"
                                daysLeft == 0 -> "Expires today"
                                daysLeft == 1 -> "1 day left"
                                else -> "$daysLeft days left"
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
}

@Composable
private fun EmptyHistoryState(selectedTab: Int) {
    val (message, subMessage, icon) = when (selectedTab) {
        0 -> Triple("No job posting history", "Start posting jobs to build your timeline", Icons.Default.Timeline)
        1 -> Triple("No active jobs", "Your active job postings will appear here", Icons.Default.CheckCircle)
        2 -> Triple("No expired jobs", "Expired job postings will appear here", Icons.Default.EventBusy)
        3 -> Triple("No jobs posted yet", "Start posting jobs to see them here", Icons.Default.WorkHistory)
        else -> Triple("No jobs", "Your job postings will appear here", Icons.Default.WorkHistory)
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
    val isExpired = job.expiresAt > 0L && job.expiresAt <= currentTime
    val isPaused = !job.isActive
    
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
                        text = job.category,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
                
                JobStatusBadge(
                    isActive = job.isActive,
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
                    text = job.location.take(20),
                    backgroundColor = Color(0xFFF3F4F6),
                    iconColor = Color(0xFF6B7280)
                )
                InfoChip(
                    icon = Icons.Default.CurrencyRupee,
                    text = "₹${job.payAmount}",
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
                        text = "${job.applicationCount.toInt()} applications",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    )
                }
                
                // Expiry info
                if (job.expiresAt > 0L) {
                    val daysLeft = job.getDaysUntilExpiry()
                    Text(
                        text = when {
                            isExpired -> "Expired"
                            daysLeft == 0 -> "Expires today"
                            daysLeft == 1 -> "1 day left"
                            else -> "$daysLeft days left"
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Posted ${formatDate(job.postedAt)}",
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
        isExpired -> Triple(Color(0xFFEF4444), "Expired", Icons.Default.EventBusy)
        !isActive -> Triple(Color(0xFF6B7280), "Paused", Icons.Default.Pause)
        else -> Triple(Color(0xFF10B981), "Active", Icons.Default.CheckCircle)
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

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60 * 1000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} min ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }
}

private fun formatTimelineDate(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val date = Date(timestamp)
    return SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
}
