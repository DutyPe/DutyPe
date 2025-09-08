package com.example.partimes.jobseeker.screens.myJobs

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.partimes.jobseeker.components.JobCard
import com.example.partimes.jobseeker.models.JobCardModel
import com.example.partimes.utils.JobCardShimmer
import com.example.partimes.viewmodels.SavedJobsViewModel

@Composable
fun SavedJobsList(
    searchQuery: String = "",
    onNavigateToJobDetails: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: SavedJobsViewModel = viewModel()
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
                uiState.isEmpty && searchQuery.isEmpty() -> {
                    EmptySavedJobsState()
                }
                uiState.isEmpty && searchQuery.isNotEmpty() -> {
                    EmptySearchResults(searchQuery = searchQuery)
                }
                else -> {
                    SavedJobsContent(
                        savedJobs = uiState.savedJobs,
                        onNavigateToJobDetails = onNavigateToJobDetails,
                        onUnsaveJob = { jobId ->
                            viewModel.unsaveJob(jobId)
                        }
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
    onUnsaveJob: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

@Composable
private fun EmptySearchResults(searchQuery: String) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = "No results",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF6B7280)
                )
                Text(
                    text = "No saved jobs found for \"$searchQuery\"",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Try adjusting your search terms or clear the search to see all saved jobs.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}
