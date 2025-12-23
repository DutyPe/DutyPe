package com.example.dutype.employer.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
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
    val tabs = listOf("All Jobs", "Active", "Expired", "Paused")
    
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        employerJobViewModel.loadMyJobs()
    }
    
    val currentTime = System.currentTimeMillis()
    
    // Filter jobs based on selected tab
    val filteredJobs = remember(uiState.myJobs, selectedTab, currentTime) {
        when (selectedTab) {
            0 -> uiState.myJobs // All
            1 -> uiState.myJobs.filter { 
                it.isActive && (it.expiresAt == 0L || it.expiresAt > currentTime)
            }
            2 -> uiState.myJobs.filter { 
                it.expiresAt > 0L && it.expiresAt <= currentTime
            }
            3 -> uiState.myJobs.filter { !it.isActive }
            else -> uiState.myJobs
        }
    }
    
    // Calculate stats
    val totalJobs = uiState.myJobs.size
    val activeJobs = uiState.myJobs.count { it.isActive && (it.expiresAt == 0L || it.expiresAt > currentTime) }
    val expiredJobs = uiState.myJobs.count { it.expiresAt > 0L && it.expiresAt <= currentTime }
    val totalApplications = uiState.myJobs.sumOf { it.applicationCount.toInt() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                    
                    Text(
                        text = "Job Posting History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                // Stats Summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        count = totalJobs,
                        label = "Total Jobs",
                        color = Color(0xFF3B82F6)
                    )
                    StatItem(
                        count = activeJobs,
                        label = "Active",
                        color = Color(0xFF10B981)
                    )
                    StatItem(
                        count = expiredJobs,
                        label = "Expired",
                        color = Color(0xFFEF4444)
                    )
                    StatItem(
                        count = totalApplications,
                        label = "Applications",
                        color = Color(0xFFF59E0B)
                    )
                }
                
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

@Composable
private fun StatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
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
        shape = RoundedCornerShape(12.dp),
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
                
                JobStatusChip(
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
                    text = job.location.take(20)
                )
                InfoChip(
                    icon = Icons.Default.AttachMoney,
                    text = "₹${job.payAmount}"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatChip(
                        icon = Icons.Default.People,
                        count = job.applicationCount.toInt(),
                        label = "Applications"
                    )
                    StatChip(
                        icon = Icons.Default.Visibility,
                        count = job.viewCount.toInt(),
                        label = "Views"
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
private fun JobStatusChip(
    isActive: Boolean,
    isExpired: Boolean
) {
    val (color, text, icon) = when {
        isExpired -> Triple(Color(0xFFEF4444), "Expired", Icons.Default.EventBusy)
        !isActive -> Triple(Color(0xFF6B7280), "Paused", Icons.Default.Pause)
        else -> Triple(Color(0xFF10B981), "Active", Icons.Default.CheckCircle)
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
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
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF6B7280)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    count: Int,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3B82F6)
            )
        )
    }
}

@Composable
private fun EmptyHistoryState(selectedTab: Int) {
    val message = when (selectedTab) {
        0 -> "No jobs posted yet"
        1 -> "No active jobs"
        2 -> "No expired jobs"
        3 -> "No paused jobs"
        else -> "No jobs"
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WorkHistory,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF6B7280)
                )
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
