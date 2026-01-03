package com.example.dutype.worker.screens.myJobs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import timber.log.Timber
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.worker.components.JobCard
import com.example.dutype.models.JobListing
import com.example.dutype.components.JobCardShimmer
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.viewmodels.SavedJobsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

@Composable
fun SavedJobsList(
    searchQuery: String = "",
    onNavigateToJobDetails: (String) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null,
    navController: androidx.navigation.NavHostController? = null
) {
    val context = LocalContext.current
    val savedJobViewModel: SavedJobsViewModel = hiltViewModel()
    val uiState by savedJobViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Debug logging for UI state
    LaunchedEffect(uiState) {
        Timber.d("SavedJobsList: UI State - isLoading=${uiState.isLoading}, savedJobs=${uiState.savedJobs.size}, hasError=${uiState.hasError}")
        if (uiState.savedJobs.isNotEmpty()) {
            uiState.savedJobs.forEach { job ->
                Timber.d("SavedJobsList: Saved job - ${job.id} (${job.title})")
            }
        }
    }

    // Load saved jobs when component mounts
    LaunchedEffect(Unit) {
        Timber.d("SavedJobsList: Component mounted, loading saved jobs...")
        savedJobViewModel.loadSavedJobs()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Timber.d("SavedJobsList: Showing loading state")
                    LoadingSavedJobs()
                }
                uiState.savedJobs.isEmpty() && searchQuery.isEmpty() -> {
                    Timber.d("SavedJobsList: Showing empty state")
                    EmptySavedJobsState(navController = navController)
                }
                uiState.savedJobs.isEmpty() && searchQuery.isNotEmpty() -> {
                    Timber.d("SavedJobsList: Empty search results for: $searchQuery")
                    EmptySearchResultsForSavedJobs(searchQuery = searchQuery)
                }
                else -> {
                    Timber.d("SavedJobsList: Showing ${uiState.savedJobs.size} jobs")
                    SavedJobsContent(
                        savedJobs = uiState.savedJobs,
                        onNavigateToJobDetails = onNavigateToJobDetails,
                        onUnsaveJob = { jobId ->
                            savedJobViewModel.unsaveJob(jobId)
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
    savedJobs: List<JobListing>,
    onNavigateToJobDetails: (String) -> Unit,
    onUnsaveJob: (String) -> Unit,
    scrollStateManager: ScrollStateManager? = null
) {
    // Debug logging for SavedJobsContent
    LaunchedEffect(savedJobs) {
        Timber.d("SavedJobsContent: Received ${savedJobs.size} jobs")
        savedJobs.forEach { job ->
            Timber.d("SavedJobsContent: Job ${job.id} - ${job.title}")
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with count and actions
        SavedJobsHeader(
            savedJobsCount = savedJobs.size,
            onClearAll = { 
                savedJobs.forEach { job ->
                    val jobId = job.jobId.ifEmpty { job.id }
                    onUnsaveJob(jobId)
                }
            }
        )
        
        // Jobs list - using JobListing directly with JobCard
        ScrollAwareLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            scrollStateManager = scrollStateManager
        ) {
            items(
                items = savedJobs,
                key = { it.jobId.ifEmpty { it.id } }
            ) { job ->
                val jobId = job.jobId.ifEmpty { job.id }
                // Use JobCard with JobListing directly
                JobCard(
                    job = job.copy(isSaved = true), // Mark as saved
                    isSaved = true,
                    hasApplied = false,
                    onApplyClick = { onNavigateToJobDetails(jobId) },
                    onSaveClick = { onUnsaveJob(jobId) },
                    onCardClick = { onNavigateToJobDetails(jobId) }
                )
            }
        }
    }
}

@Composable
private fun SavedJobsHeader(
    savedJobsCount: Int,
    onClearAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1F2937),
                                    Color(0xFF374151)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = stringResource(R.string.saved_jobs),
                        style = AppTypography.sectionHeader.copy(
                            color = Color(0xFF1E293B)
                        )
                    )
                    Text(
                        text = "$savedJobsCount ${if (savedJobsCount == 1) stringResource(R.string.job_singular) else stringResource(R.string.jobs_plural)} ${stringResource(R.string.saved_lowercase)}",
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }
            
            AnimatedVisibility(
                visible = savedJobsCount > 0,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFDC2626)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear all",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.clear_all),
                        style = AppTypography.buttonMedium
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
                    text = stringResource(R.string.no_results_found),
                    style = AppTypography.emptyStateTitle.copy(
                        color = Color(0xFF111827)
                    )
                )
                Text(
                    text = stringResource(R.string.no_saved_jobs_match, searchQuery),
                    style = AppTypography.emptyStateSubtitle.copy(
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptySavedJobsState(navController: androidx.navigation.NavHostController? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = Color(0xFF9CA3AF), // Same gray color as applied jobs icon
                modifier = Modifier.size(56.dp)
            )
            
            Text(
                text = stringResource(R.string.no_saved_jobs),
                style = AppTypography.emptyStateTitle.copy(
                    color = Color(0xFF1E293B)
                )
            )
            Text(
                text = stringResource(R.string.tap_bookmark_to_save),
                style = AppTypography.emptyStateSubtitle.copy(
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
            )
            
            Button(
                onClick = {
                    // Use WORKER_HOME_TAB ("home") for navigation within worker bottom nav
                    navController?.navigate(com.example.dutype.navigation.Routes.WORKER_HOME_TAB) {
                        popUpTo(com.example.dutype.navigation.Routes.WORKER_HOME_TAB) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text(
                    text = stringResource(R.string.browse_jobs),
                    style = AppTypography.buttonMedium.copy(
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlight(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF1F2937),
                modifier = Modifier.size(16.dp)
            )
        }
        
        Column {
            Text(
                text = title,
                style = AppTypography.labelLarge.copy(
                    color = Color(0xFF1E293B)
                )
            )
            Text(
                text = description,
                style = AppTypography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}
