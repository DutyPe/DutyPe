package com.example.partimes.jobseeker.screens.myJobs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.partimes.jobseeker.models.ApplicationStatus
import com.example.partimes.utils.ScrollStateManager
import com.example.partimes.components.ScrollAwareLazyColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobsScreen(
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<ApplicationStatus?>(null) }
    var isSearchVisible by remember { mutableStateOf(false) }

    val tabTitles = listOf("Applied Jobs", "Saved Jobs")
    val tabIcons = listOf(Icons.Default.Work, Icons.Default.Bookmark)

    // Status bar color management based on current tab
    val statusBarColor = when (selectedTabIndex) {
        0 -> Color.Black // Applied Jobs - Black
        1 -> Color.Black // Saved Jobs - Black
        else -> Color.Black
    }

    // Update status bar color when tab changes
    LaunchedEffect(statusBarColor) {
        onStatusBarColorChange(statusBarColor)
    }

    // Filter applied jobs based on search and status
    val filteredAppliedJobs = remember(searchQuery, selectedStatusFilter) {
        dummyAppliedJobs.filter { job ->
            val matchesSearch = searchQuery.isEmpty() ||
                job.jobListing.title.contains(searchQuery, ignoreCase = true) ||
                job.jobListing.company.contains(searchQuery, ignoreCase = true) ||
                job.jobListing.locationNearby.contains(searchQuery, ignoreCase = true)

            val matchesStatus = selectedStatusFilter == null || job.status == selectedStatusFilter

            matchesSearch && matchesStatus
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
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
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search jobs, companies, locations...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF6B7280)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF3F4F6),
                                unfocusedContainerColor = Color(0xFFF3F4F6),
                                focusedIndicatorColor = Color(0xFF6366F1),
                                unfocusedIndicatorColor = Color.Transparent
                            )
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

                        ApplicationStatus.values().forEach { status ->
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
                        if (filteredAppliedJobs.isEmpty() && searchQuery.isNotEmpty()) {
                            item {
                                EmptySearchResults(searchQuery = searchQuery)
                            }
                        } else {
                            items(filteredAppliedJobs) { appliedJob ->
                                AppliedJobCard(
                                    appliedJob = appliedJob,
                                    onClick = {
                                        // TODO: Navigate to job details
                                    }
                                )
                            }
                        }

                        if (filteredAppliedJobs.isEmpty() && searchQuery.isEmpty() && selectedStatusFilter == null) {
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

@Composable
private fun EmptySearchResults(searchQuery: String) {
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
private fun EmptyAppliedJobsState() {
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

private fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending"
        ApplicationStatus.SELECTED -> "Selected"
        ApplicationStatus.REJECTED -> "Rejected"
        ApplicationStatus.INTERVIEWING -> "Interview"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.VACANCY_FILLED -> "Vacancy Filled"
        ApplicationStatus.INTERVIEW_SCHEDULED -> "Interview Scheduled"
        ApplicationStatus.DOCUMENTS_PENDING -> "Documents Pending"
    }
}

private fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFFF9800)
        ApplicationStatus.SELECTED -> Color(0xFF4CAF50)
        ApplicationStatus.REJECTED -> Color(0xFFF44336)
        ApplicationStatus.INTERVIEWING -> Color(0xFF2196F3)
        ApplicationStatus.SHORTLISTED -> Color(0xFF9C27B0)
        ApplicationStatus.VACANCY_FILLED -> Color(0xFF607D8B)
        ApplicationStatus.INTERVIEW_SCHEDULED -> Color(0xFF00BCD4)
        ApplicationStatus.DOCUMENTS_PENDING -> Color(0xFFFF5722)
    }
}
