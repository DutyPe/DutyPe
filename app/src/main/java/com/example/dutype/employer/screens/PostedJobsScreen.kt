package com.example.dutype.employer.screens

// ImageVector import removed - no longer needed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.employer.components.EmployerJobCard
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.employer.models.JobStats
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.components.EmptyActionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostedJobsScreen(
    navController: NavController,
    viewModel: FirestoreEmployerJobViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Convert JobListing to JobPostingModel for EmployerJobCard compatibility
    val postedJobs = uiState.myJobs.map { job ->
        JobPostingModel(
            jobId = job.id,
            title = job.title,
            payAmount = job.salary.toInt().toString(),
            payType = com.example.dutype.employer.models.PayType.DAILY,
            location = job.addressText.ifBlank { job.location },
            description = job.description,
            contactNumber = job.contactNumber,
            category = com.example.dutype.employer.models.JobCategory.HELPER,
            postedTime = job.createdAt,
            isActive = job.status == "open",
            applicationsReceived = 0,
            employerId = job.employerId,
            isFilled = job.status == "closed"
        )
    }
    
    // Calculate job stats from the jobs list
    val jobStats = JobStats(
        activeJobs = postedJobs.count { it.isActive },
        pausedJobs = postedJobs.count { !it.isActive },
        totalApplications = postedJobs.sumOf { it.applicationsReceived },
        todayJobs = postedJobs.count { DateTimeUtils.isToday(it.postedTime) },
        totalJobs = postedJobs.size
    )
    val isLoading = uiState.isLoading
    val isRefreshing = uiState.isRefreshing
    val error = uiState.error

    Column(
        modifier = Modifier.fillMaxSize() // No outer padding
    ) {

        if (isLoading && postedJobs.isEmpty()) {
            PostedJobsLoadingContent()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 13.dp,
                    start = 13.dp,
                    end = 13.dp,
                    bottom = 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                // Header section with job count and refresh
                item {
                    JobsHeaderSection(
                        totalJobs = postedJobs.size,
                        onRefresh = { viewModel.refreshMyJobs() },
                        isRefreshing = isRefreshing
                    )
                }

                // Error handling
                error?.let { errorMessage ->
                    item {
                        // Using local ErrorCard with retry/dismiss functionality
                        // Different from components/ErrorCard which is simpler
                        PostedJobsErrorCard(
                            errorMessage = errorMessage,
                            onDismiss = { viewModel.clearError() },
                            onRetry = { viewModel.refreshMyJobs() }
                        )
                    }
                }

                // Job list or empty state
                if (postedJobs.isEmpty() && !isLoading) {
                    item {
                        PostedJobsEmptyState(
                            onPostJob = { navController.navigate("employer_post_job") }
                        )
                    }
                } else {
                    items(
                        items = postedJobs,
                        key = { job -> "posted_${job.jobId}" }
                    ) { job ->
                        EmployerJobCard(
                            jobPosting = job,
                            onEditClick = { jobId ->
                                navController.navigate("edit_job/$jobId")
                            },
                            onViewApplicationsClick = { jobId ->
                                navController.navigate("employer_applications_job/$jobId")
                            },
                            onToggleActiveClick = { jobId ->
                                viewModel.toggleJobStatus(jobId)
                            },
                            onShareClick = { jobId ->
                                // Share job functionality with deep link
                                val jobDeepLink = com.example.dutype.utils.DeepLinkHandler.generateJobWebLink(jobId)
                                val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Job Opening: ${job.title}")
                                    putExtra(android.content.Intent.EXTRA_TEXT, """
🎯 *Job Opening: ${job.title}*
📍 ${job.location}
💰 Pay: ₹${job.payAmount}

👉 View & Apply Now:
$jobDeepLink

📱 Download DutyPe App:
📲 $playStoreUrl
                                    """.trimIndent())
                                }
                                navController.context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Job"))
                            },
                            showActions = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JobsHeaderSection(
    totalJobs: Int,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Job Postings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$totalJobs total jobs posted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PostedJobsLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading your jobs...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
    }
}

/**
 * Local ErrorCard with retry/dismiss functionality
 * Different from components/ErrorCard.kt which is a simpler animated error display
 * This version includes action buttons for retry and dismiss
 */
@Composable
private fun PostedJobsErrorCard(
    errorMessage: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Error Loading Jobs",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.retry))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        }
    }
}

@Composable
private fun PostedJobsEmptyState(onPostJob: () -> Unit) {
    EmptyActionState(
        icon = Icons.Default.Work,
        title = "No Jobs Posted Yet",
        subtitle = "Start building your team by posting your first job opportunity",
        actionLabel = stringResource(R.string.post_your_first_job),
        onAction = onPostJob
    )
}
