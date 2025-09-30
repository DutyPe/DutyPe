package com.example.partimes.worker.screens.myJobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.partimes.worker.components.JobCard
import com.example.partimes.worker.models.JobCardModel
import com.example.partimes.worker.models.PayInfo
import com.example.partimes.worker.models.PayType
import com.example.partimes.worker.models.LocationInfo
import com.example.partimes.worker.models.JobTag
import com.example.partimes.worker.models.TagType
import com.example.partimes.worker.models.TimeInfo
import com.example.partimes.worker.models.UrgencyLevel
import com.example.partimes.models.JobListing
import com.example.partimes.utils.JobCardShimmer
import com.example.partimes.utils.ScrollStateManager
import com.example.partimes.components.ScrollAwareLazyColumn
import com.example.partimes.viewmodels.SavedJobsViewModel

@Composable
fun SavedJobsList(
    searchQuery: String = "",
    onNavigateToJobDetails: (String) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null
) {
    val context = LocalContext.current
    val viewModel: SavedJobsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Update search query when it changes
    LaunchedEffect(searchQuery) {
        viewModel.updateSearchQuery(searchQuery)
    }

    // Show snackbar for messages
    LaunchedEffect(uiState.showMessage) {
        uiState.showMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    LoadingSavedJobs()
                }
                uiState.savedJobs.isEmpty() && searchQuery.isEmpty() -> {
                    EmptySavedJobsState()
                }
                uiState.savedJobs.isEmpty() && searchQuery.isNotEmpty() -> {
                    EmptySearchResultsForSavedJobs(searchQuery = searchQuery)
                }
                else -> {
                    SavedJobsContent(
                        savedJobs = uiState.savedJobs.map { jobListing ->
                            convertJobListingToJobCardModel(jobListing)
                        },
                        onNavigateToJobDetails = onNavigateToJobDetails,
                        onUnsaveJob = { jobId ->
                            viewModel.unsaveJob(jobId)
                        },
                        scrollStateManager = scrollStateManager
                    )
                }
            }
        }

        // Snackbar for showing messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun LoadingSavedJobs() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(3) { // Show 3 shimmer cards for saved jobs
            JobCardShimmer()
        }
    }
}

@Composable
private fun SavedJobsContent(
    savedJobs: List<JobCardModel>,
    onNavigateToJobDetails: (String) -> Unit,
    onUnsaveJob: (String) -> Unit,
    scrollStateManager: ScrollStateManager? = null
) {
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
        items(savedJobs, key = { it.jobId }) { job ->
            SavedJobCard(
                jobCard = job,
                onApplyClick = { jobId ->
                    onNavigateToJobDetails(jobId)
                },
                onUnsaveClick = { jobId ->
                    onUnsaveJob(jobId)
                },
                onCardClick = { jobId ->
                    onNavigateToJobDetails(jobId)
                }
            )
        }
    }
}

@Composable
private fun SavedJobCard(
    jobCard: JobCardModel,
    onApplyClick: (String) -> Unit,
    onUnsaveClick: (String) -> Unit,
    onCardClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            JobCard(
                jobCard = jobCard,
                onApplyClick = onApplyClick,
                onSaveClick = { /* Already saved, no action needed */ },
                onCardClick = onCardClick
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { onUnsaveClick(jobCard.jobId) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from saved",
                        tint = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySearchResultsForSavedJobs(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF9CA3AF)
                )
                Text(
                    text = "No Results Found",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = "No saved jobs match \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptySavedJobsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "🔖",
                    fontSize = 48.sp
                )
                Text(
                    text = "No Saved Jobs",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = "Save interesting jobs by tapping the bookmark icon on job cards. Your saved jobs will appear here for easy access.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                )
                Button(
                    onClick = { /* TODO: Navigate to home screen */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Browse Jobs")
                }
            }
        }
    }
}

// Conversion function to convert JobListing to JobCardModel
private fun convertJobListingToJobCardModel(job: JobListing): JobCardModel {
    return JobCardModel(
        jobId = job.id,
        title = job.title,
        employerName = job.companyName,
        payInfo = PayInfo(
            amount = job.payAmount,
            type = when {
                job.payType.equals("HOURLY", true) || job.payType.contains("hour", true) -> PayType.HOURLY
                job.payType.equals("DAILY", true) || job.payType.contains("day", true) -> PayType.DAILY
                job.payType.equals("MONTHLY", true) || job.payType.contains("month", true) -> PayType.MONTHLY
                else -> PayType.DAILY
            },
            period = ""
        ),
        location = LocationInfo(
            area = job.area ?: job.location,
            city = job.city ?: job.location,
            distance = "2.5"
        ),
        tags = listOf(
            JobTag(
                text = job.jobType,
                emoji = "💼",
                type = TagType.BENEFIT
            ),
            JobTag(
                text = job.category,
                emoji = "🏷️",
                type = TagType.BENEFIT
            )
        ),
        timeInfo = TimeInfo(
            postedTime = job.postedDate,
            urgency = if (job.isUrgent()) UrgencyLevel.URGENT else UrgencyLevel.NORMAL
        ),
        phoneNumber = job.contactNumber,
        description = job.description,
        jobType = job.jobType,
        isBookmarked = false, // TODO: Get from WorkerJobInteraction
        isApplied = false // TODO: Get from WorkerJobInteraction
    )
}