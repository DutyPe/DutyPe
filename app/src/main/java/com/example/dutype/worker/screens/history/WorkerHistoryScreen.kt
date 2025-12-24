package com.example.dutype.worker.screens.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
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
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.JobApplicationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerHistoryScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val jobApplicationViewModel: JobApplicationViewModel = hiltViewModel()
    val uiState by jobApplicationViewModel.uiState.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Accepted", "Rejected", "Withdrawn")
    
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        jobApplicationViewModel.loadMyApplications()
    }
    
    // Filter applications based on selected tab
    val filteredApplications = remember(uiState.applications, selectedTab) {
        when (selectedTab) {
            0 -> uiState.applications // All
            1 -> uiState.applications.filter { it.status == ApplicationStatus.ACCEPTED }
            2 -> uiState.applications.filter { it.status == ApplicationStatus.REJECTED }
            3 -> uiState.applications.filter { it.status == ApplicationStatus.WITHDRAWN }
            else -> uiState.applications
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Common Header
        CommonHeader(
            title = "Application History",
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
        } else if (filteredApplications.isEmpty()) {
            EmptyHistoryState(selectedTab = selectedTab)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredApplications) { application ->
                    HistoryApplicationCard(
                        application = application,
                        onClick = {
                            navController.navigate(Routes.jobDetailRoute(application.jobId))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(selectedTab: Int) {
    val (message, subMessage, icon) = when (selectedTab) {
        0 -> Triple("No applications yet", "Your job applications will appear here", Icons.Default.History)
        1 -> Triple("No accepted applications", "Accepted applications will appear here", Icons.Default.CheckCircle)
        2 -> Triple("No rejected applications", "Rejected applications will appear here", Icons.Default.Cancel)
        3 -> Triple("No withdrawn applications", "Withdrawn applications will appear here", Icons.Default.Close)
        else -> Triple("No applications", "Your applications will appear here", Icons.Default.History)
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFD1D5DB),
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
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
private fun HistoryApplicationCard(
    application: JobApplication,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        text = application.jobTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = application.companyName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
                
                StatusChip(status = application.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.LocationOn,
                    text = application.jobLocation.take(20)
                )
                InfoChip(
                    icon = Icons.Default.AttachMoney,
                    text = application.payInfo
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Applied ${formatDate(application.appliedAt)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF)
                )
            )
        }
    }
}

@Composable
private fun StatusChip(status: ApplicationStatus) {
    val (color, text, icon) = when (status) {
        ApplicationStatus.PENDING -> Triple(Color(0xFFF59E0B), "Pending", Icons.Default.Schedule)
        ApplicationStatus.UNDER_REVIEW -> Triple(Color(0xFF3B82F6), "Under Review", Icons.Default.Visibility)
        ApplicationStatus.ACCEPTED -> Triple(Color(0xFF10B981), "Accepted", Icons.Default.CheckCircle)
        ApplicationStatus.REJECTED -> Triple(Color(0xFFEF4444), "Rejected", Icons.Default.Cancel)
        ApplicationStatus.WITHDRAWN -> Triple(Color(0xFF6B7280), "Withdrawn", Icons.Default.Close)
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

private fun formatDate(timestamp: Long): String {
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
