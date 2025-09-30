package com.example.partimes.worker.screens.myJobs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.partimes.data.dummy.dummyAppliedJobs
import com.example.partimes.data.dummy.dummySavedJobs
import com.example.partimes.worker.models.ApplicationStatus
import com.example.partimes.utils.ScrollStateManager
import com.example.partimes.ui.components.ReusableSearchBar
import com.example.partimes.components.ScrollAwareLazyColumn
import com.example.partimes.ui.theme.WorkerGradientBackground

// Helper functions for status display and colors
fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.DRAFT -> "Draft"
        ApplicationStatus.SUBMITTED -> "Submitted"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.INTERVIEW_SCHEDULED -> "Interview Scheduled"
        ApplicationStatus.INTERVIEWED -> "Interviewed"
        ApplicationStatus.SELECTED -> "Selected"
        ApplicationStatus.REJECTED -> "Rejected"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
        ApplicationStatus.EXPIRED -> "Expired"
    }
}

fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.DRAFT -> Color(0xFF9E9E9E)
        ApplicationStatus.SUBMITTED -> Color(0xFF2196F3)
        ApplicationStatus.UNDER_REVIEW -> Color(0xFFFF9800)
        ApplicationStatus.SHORTLISTED -> Color(0xFF9C27B0)
        ApplicationStatus.INTERVIEW_SCHEDULED -> Color(0xFF00BCD4)
        ApplicationStatus.INTERVIEWED -> Color(0xFF3F51B5)
        ApplicationStatus.SELECTED -> Color(0xFF4CAF50)
        ApplicationStatus.REJECTED -> Color(0xFFF44336)
        ApplicationStatus.WITHDRAWN -> Color(0xFF607D8B)
        ApplicationStatus.EXPIRED -> Color(0xFF795548)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobsScreen(
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null
) {
    // Local state management
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var selectedStatusFilter by remember { mutableStateOf<ApplicationStatus?>(null) }
    
    // Use dummy data for now
    val applications = dummyAppliedJobs
    val savedJobs = dummySavedJobs
    
    val filteredApplications = remember(applications, searchQuery, selectedStatusFilter) {
        applications.filter { application ->
            val matchesSearch = searchQuery.isEmpty() || 
                application.jobListing.title.contains(searchQuery, ignoreCase = true) ||
                application.jobListing.company.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || application.status == selectedStatusFilter
            matchesSearch && matchesStatus
        }
    }
    
    val filteredSavedJobs = remember(savedJobs, searchQuery) {
        savedJobs.filter { job ->
            searchQuery.isEmpty() || 
            job.title.contains(searchQuery, ignoreCase = true) ||
            job.employerName.contains(searchQuery, ignoreCase = true)
        }
    }

    val tabTitles = listOf("Applied Jobs", "Saved Jobs")
    val tabIcons = listOf(Icons.Default.Work, Icons.Default.Bookmark)

    // Status bar color management based on current tab
    val statusBarColor = when (selectedTabIndex) {
        0 -> Color.White // Applied Jobs - White
        1 -> Color.White // Saved Jobs - White
        else -> Color.White
    }

    // Update status bar color when tab changes
    LaunchedEffect(selectedTabIndex) {
        onStatusBarColorChange(statusBarColor)
    }

    WorkerGradientBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Enhanced Header with search
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title and search toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Jobs",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible }
                    ) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF6366F1)
                        )
                    }
                }

                // Search bar with animation
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        ReusableSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Search jobs, companies, locations...",
                            height = 48,
                            backgroundColor = Color(0xFFF3F4F6),
                            borderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF6366F1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enhanced Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF6366F1),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF6366F1),
                            height = 3.dp
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = tabIcons[index],
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selectedTabIndex == index) Color(0xFF6366F1) else Color(0xFF6B7280)
                                    )
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTabIndex == index) Color(0xFF6366F1) else Color(0xFF6B7280)
                                    )
                                }
                            },
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }

        // Content based on selected tab
        when (selectedTabIndex) {
            0 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Status filter chips for Applied Jobs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        item {
                            FilterChip(
                                onClick = { selectedStatusFilter = null },
                                label = { Text("All") },
                                selected = selectedStatusFilter == null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6366F1).copy(alpha = 0.1f),
                                    selectedLabelColor = Color(0xFF6366F1)
                                )
                            )
                        }

                        ApplicationStatus.entries.forEach { status ->
                            item {
                                val count = dummyAppliedJobs.count { it.status == status }
                                if (count > 0) {
                                    FilterChip(
                                        onClick = {
                                            selectedStatusFilter = if (selectedStatusFilter == status) null else status
                                        },
                                        label = {
                                            Text("${getStatusDisplayName(status)} ($count)")
                                        },
                                        selected = selectedStatusFilter == status,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = getStatusColor(status).copy(alpha = 0.1f),
                                            selectedLabelColor = getStatusColor(status)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Applied jobs list
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
                        if (filteredApplications.isEmpty() && searchQuery.isNotEmpty()) {
                            item {
                                EmptySearchResults(searchQuery = searchQuery)
                            }
                        } else {
                            items(filteredApplications) { appliedJob ->
                                AppliedJobCard(
                                    appliedJob = appliedJob,
                                    onClick = { jobListing ->
                                        // TODO: Navigate to job details
                                    }
                                )
                            }
                        }

                        if (filteredApplications.isEmpty() && searchQuery.isEmpty() && selectedStatusFilter == null) {
                            item {
                                EmptyAppliedJobsState()
                            }
                        }
                    }
                }
            }
            1 -> {
                SavedJobsList(
                    searchQuery = searchQuery,
                    onNavigateToJobDetails = { jobId ->
                        // TODO: Navigate to job details
                        // navController.navigate(Routes.jobDetailRoute(jobId))
                    },
                    scrollStateManager = scrollStateManager
                )
            }
        }
    }
    }
}

@Composable
fun EmptySearchResults(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF9CA3AF)
            )
            Text(
                text = "No results found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
            Text(
                text = "No jobs match \"$searchQuery\"",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyAppliedJobsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Work,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF9CA3AF)
            )
            Text(
                text = "No Applications Yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
            Text(
                text = "Start applying to jobs to see them here",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { /* TODO: Navigate to job search */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Find Jobs")
            }
        }
    }
}


