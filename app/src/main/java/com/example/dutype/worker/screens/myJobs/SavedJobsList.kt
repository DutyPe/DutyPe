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
import com.example.dutype.worker.components.JobCard
import com.example.dutype.worker.models.JobCardModel
import com.example.dutype.worker.models.PayInfo
import com.example.dutype.worker.models.PayType
import com.example.dutype.worker.models.LocationInfo
import com.example.dutype.worker.models.JobTag
import com.example.dutype.worker.models.TagType
import com.example.dutype.worker.models.TimeInfo
import com.example.dutype.worker.models.UrgencyLevel
import com.example.dutype.models.JobListing
import com.example.dutype.utils.JobCardShimmer
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.viewmodels.SavedJobsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
                        savedJobs = uiState.savedJobs.map { jobListing ->
                            convertJobListingToJobCardModel(jobListing)
                        },
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
    savedJobs: List<JobCardModel>,
    onNavigateToJobDetails: (String) -> Unit,
    onUnsaveJob: (String) -> Unit,
    scrollStateManager: ScrollStateManager? = null
) {
    // Debug logging for SavedJobsContent
    LaunchedEffect(savedJobs) {
        Timber.d("SavedJobsContent: Received ${savedJobs.size} jobs")
        savedJobs.forEach { job ->
            Timber.d("SavedJobsContent: Job ${job.jobId} - ${job.title}")
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
                    onUnsaveJob(job.jobId)
                }
            }
        )
        
        // Jobs list with enhanced animations
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
                key = { it.jobId }
            ) { job ->
                EnhancedSavedJobCard(
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
                                    Color(0xFF6366F1),
                                    Color(0xFF8B5CF6)
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
                        text = "Saved Jobs",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    )
                    Text(
                        text = "$savedJobsCount ${if (savedJobsCount == 1) "job" else "jobs"} saved",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF64748B)
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
                        text = "Clear All",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedSavedJobCard(
    jobCard: JobCardModel,
    onApplyClick: (String) -> Unit,
    onUnsaveClick: (String) -> Unit,
    onCardClick: (String) -> Unit
) {
    var isRemoving by remember { mutableStateOf(false) }
    
    AnimatedVisibility(
        visible = !isRemoving,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCardClick(jobCard.jobId) },
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header with favorite indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saved Job",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            isRemoving = true
                            onUnsaveClick(jobCard.jobId)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove from saved",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Job content
                JobCard(
                    jobCard = jobCard,
                    onApplyClick = onApplyClick,
                    onSaveClick = { /* Already saved, no action needed */ },
                    onCardClick = onCardClick,
                    isSaved = true // All jobs in saved jobs list are saved
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onApplyClick(jobCard.jobId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF6366F1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Apply Now",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    
                    Button(
                        onClick = { onCardClick(jobCard.jobId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "View Details",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )
                    }
                }
            }
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
                onCardClick = onCardClick,
                isSaved = true // All jobs in saved jobs list are saved
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
private fun EmptySavedJobsState(navController: androidx.navigation.NavHostController? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8FAFC)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(40.dp)
            ) {
                // Animated bookmark icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFEF3C7),
                                    Color(0xFFFDE68A)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No Saved Jobs Yet",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    )
                    Text(
                        text = "Start building your job collection by tapping the bookmark icon on job cards you're interested in.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    )
                }
                
                // Feature highlights
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureHighlight(
                        icon = Icons.Default.Star,
                        title = "Easy Access",
                        description = "Quickly find jobs you're interested in"
                    )
                    FeatureHighlight(
                        icon = Icons.Default.Favorite,
                        title = "Personal Collection",
                        description = "Build your own curated job list"
                    )
                }
                
                Button(
                    onClick = {
                        // Navigate to home screen to browse jobs
                        Timber.d("🏠 EmptySavedJobsState - Browse Jobs button clicked")
                        try {
                            if (navController != null) {
                                Timber.d("🏠 EmptySavedJobsState - Navigating to WORKER_HOME")
                                navController.navigate(com.example.dutype.navigation.Routes.WORKER_HOME) {
                                    // Pop back to the main worker screen to avoid back stack issues
                                    popUpTo(com.example.dutype.navigation.Routes.WORKER_MY_JOBS) {
                                        inclusive = true
                                    }
                                }
                            } else {
                                Timber.w("🏠 EmptySavedJobsState - NavController is null!")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "🏠 EmptySavedJobsState - Navigation error")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Browse Available Jobs",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
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
                .background(Color(0xFFEEF2FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(16.dp)
            )
        }
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B)
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF64748B)
                )
            )
        }
    }
}

// Conversion function to convert JobListing to JobCardModel
private fun convertJobListingToJobCardModel(job: JobListing): JobCardModel {
    Timber.d("Converting job ${job.id} to JobCardModel")
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
            distance = job.distance?.let { com.example.dutype.location.formatDistance(it) } ?: "N/A"
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
        isSaved = true, // All jobs in saved jobs list are saved
        isApplied = false // TODO: Get from WorkerJobInteraction
    )
}
