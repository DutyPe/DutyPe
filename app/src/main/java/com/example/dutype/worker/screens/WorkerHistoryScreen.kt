package com.example.dutype.worker.screens

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
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.getStatusColor
import com.example.dutype.models.getDisplayName
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.components.ApplicationStatusBadge
import com.example.dutype.ui.theme.WorkerColors
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerHistoryScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val jobApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    val uiState by jobApplicationViewModel.legacyUiState.collectAsStateWithLifecycle()
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_timeline),
        stringResource(R.string.tab_completed),
        stringResource(R.string.tab_all_history)
    )
    
    val screenBg = com.example.dutype.ui.theme.WorkerColors.ScreenBackground
    LaunchedEffect(screenBg) {
        onStatusBarColorChange(screenBg)
    }
    LaunchedEffect(Unit) {
        jobApplicationViewModel.loadMyApplications()
    }
    
    // Filter applications based on selected tab.
    // Bug #13 fix: Tabs were broken — tab 0 (Timeline) and tab 1 (Completed)
    // both filtered by HIRED, so they showed identical lists. Timeline should
    // show ALL non-rejected applications grouped by month; Completed shows
    // only HIRED; All History shows everything.
    val filteredApplications = remember(uiState.applications, selectedTab) {
        when (selectedTab) {
            0 -> uiState.applications
                .filter { it.status != ApplicationStatus.REJECTED }
                .sortedByDescending { it.createdAt }
            // Bug #15 fix: Completed tab now includes both HIRED (work in
            // progress) and COMPLETED (employer marked done) so workers see
            // every job they were chosen for, not just paid ones.
            1 -> uiState.applications
                .filter { it.status == ApplicationStatus.HIRED || it.status == ApplicationStatus.COMPLETED }
                .sortedByDescending { it.createdAt }
            2 -> uiState.applications.sortedByDescending { it.createdAt }
            else -> uiState.applications
        }
    }
    
    // Group applications by month for timeline view
    val groupedApplications = remember(filteredApplications) {
        filteredApplications.groupBy { app ->
            val calendar = Calendar.getInstance().apply { timeInMillis = app.createdAt }
            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(calendar.time).uppercase()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
                .background(WorkerColors.ScreenBackground)
    ) {
        // Common Header
        CommonHeader(
            title = stringResource(R.string.work_history),
            navController = navController
        )
        
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
                containerColor = WorkerColors.ScreenBackground,
            contentColor = Color(0xFF1F2937),
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
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
                CircularProgressIndicator(color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
            }
        } else if (filteredApplications.isEmpty()) {
            EmptyHistoryState(selectedTab = selectedTab)
        } else {
            when (selectedTab) {
                0 -> {
                    // Timeline View - LinkedIn style
                    TimelineView(
                        groupedApplications = groupedApplications,
                        onJobClick = { application ->
                            navController.navigate(Routes.jobDetailRoute(application.jobId))
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
    }
}

@Composable
private fun TimelineView(
    groupedApplications: Map<String, List<JobApplication>>,
    onJobClick: (JobApplication) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
    ) {
        groupedApplications.forEach { (monthYear, applications) ->
            // Month Header
            item(key = "header_$monthYear") {
                MonthHeader(monthYear = monthYear)
            }
            
            // Timeline items for this month
            items(
                items = applications,
                key = { it.id }
            ) { application ->
                val isLastInMonth = applications.last() == application
                TimelineJobCard(
                    application = application,
                    isLastInMonth = isLastInMonth,
                    onClick = { onJobClick(application) }
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
                .background(Color(0xFF1F2937)),
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
                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
private fun TimelineJobCard(
    application: JobApplication,
    isLastInMonth: Boolean,
    onClick: () -> Unit
) {
    val lineColor = Color(0xFFE5E7EB)
    
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
                        when (application.status) {
                            ApplicationStatus.HIRED -> Color(0xFF10B981)
                            ApplicationStatus.SHORTLISTED -> Color(0xFF3B82F6)
                            else -> Color(0xFF6B7280)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (application.status) {
                        ApplicationStatus.HIRED -> Icons.Default.CheckCircle
                        ApplicationStatus.SHORTLISTED -> Icons.Default.ThumbUp
                        else -> Icons.Default.Work
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
            colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Status badge - use centralized component
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ApplicationStatusBadge(status = application.status)
                    
                    Text(
                        text = formatTimelineDate(application.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9CA3AF)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Job title
                Text(
                    text = application.jobTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Company name
                Text(
                    text = application.companyName,
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
                    // expectedSalary not stored on application
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(selectedTab: Int) {
    val (message, subMessage, icon) = when (selectedTab) {
        0 -> Triple(stringResource(R.string.history_no_work_history), stringResource(R.string.history_complete_jobs_timeline), Icons.Default.Timeline)
        1 -> Triple(stringResource(R.string.history_no_completed_jobs), stringResource(R.string.history_completed_appear_here), Icons.Default.CheckCircle)
        2 -> Triple(stringResource(R.string.history_no_applications_yet), stringResource(R.string.history_applications_appear_here), Icons.Default.History)
        else -> Triple(stringResource(R.string.history_no_applications), stringResource(R.string.history_your_applications_here), Icons.Default.History)
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
private fun HistoryApplicationCard(
    application: JobApplication,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
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
                
                ApplicationStatusBadge(status = application.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (application.jobLocation.isNotBlank()) {
                    InfoChip(
                        icon = Icons.Default.LocationOn,
                        text = application.jobLocation.take(20),
                        backgroundColor = Color(0xFFF3F4F6),
                        iconColor = Color(0xFF6B7280)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.history_applied_time, DateTimeUtils.formatRelativeTime(application.createdAt)),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF)
                )
            )
        }
    }
}

// NOTE: StatusBadge removed - use ApplicationStatusBadge from com.example.dutype.components

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

// NOTE: formatDate removed - use DateTimeUtils.formatRelativeTime() instead

private fun formatTimelineDate(timestamp: Long): String {
    return DateTimeUtils.formatDate(timestamp)
}
