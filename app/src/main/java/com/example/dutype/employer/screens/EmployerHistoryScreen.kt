package com.example.dutype.employer.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.RatingBottomSheet
import com.example.dutype.data.JobDraftDataStore
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
import com.example.dutype.models.InstantResponse
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.viewmodels.InstantHelpViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.dutype.app.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerHistoryScreen(
    navController: NavController,
    initialTab: String? = null,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val employerJobViewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val instantHelpViewModel: InstantHelpViewModel = hiltViewModel()
    val uiState by employerJobViewModel.uiState.collectAsStateWithLifecycle()
    val instantHelpState by instantHelpViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ratingService = remember {
        com.example.dutype.services.RatingService(
            com.example.dutype.di.firestoreFromHilt(context),
            com.example.dutype.di.authFromHilt(context)
        )
    }
    
    val urgentTabIndex = 3
    val tabs = listOf(
        stringResource(R.string.tab_all_jobs),
        stringResource(R.string.tab_active),
        stringResource(R.string.tab_expired),
        stringResource(R.string.urgent)
    )
    var selectedTab by remember(initialTab) {
        mutableIntStateOf(
            when (initialTab?.lowercase(Locale.ROOT)) {
                "urgent" -> urgentTabIndex
                "active" -> 1
                "expired" -> 2
                else -> 0
            }
        )
    }
    var pendingRatingResponse by remember { mutableStateOf<InstantResponse?>(null) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var ratedResponseIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var repostingJobId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        try {
            employerJobViewModel.loadMyJobs()
            instantHelpViewModel.loadEmployerUrgentNeeds()
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error loading jobs in EmployerHistoryScreen")
        }
    }

    LaunchedEffect(instantHelpState.employerInstantResponses) {
        val completedResponses = instantHelpState.employerInstantResponses.values
            .flatten()
            .filter { it.status.equals("completed", ignoreCase = true) }
        ratedResponseIds = completedResponses.mapNotNull { response ->
            if (ratingService.hasRated(response.requestId, response.workerId)) response.responseId else null
        }.toSet()
    }

    if (showRatingSheet && pendingRatingResponse != null) {
        RatingBottomSheet(
            isVisible = showRatingSheet,
            targetName = pendingRatingResponse!!.workerName.ifBlank { "this worker" },
            targetRole = "WORKER",
            onDismiss = {
                showRatingSheet = false
                pendingRatingResponse = null
            },
            onSubmit = { rating, review, tags ->
                pendingRatingResponse?.let { response ->
                    scope.launch {
                        ratingService.submitRating(
                            jobId = response.requestId,
                            targetUserId = response.workerId,
                            rating = rating,
                            review = review,
                            tags = tags,
                            targetRole = "WORKER"
                        ).fold(
                            onSuccess = { result ->
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                if (result.success) {
                                    ratedResponseIds = ratedResponseIds + response.responseId
                                    showRatingSheet = false
                                    pendingRatingResponse = null
                                }
                            },
                            onFailure = { error ->
                                Toast.makeText(context, error.message ?: "Failed to submit rating", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        )
    }

    val currentTime = System.currentTimeMillis()

    fun repostExpiredJob(job: JobListing) {
        if (repostingJobId != null) return

        val missingFields = job.missingRepostFields()
        if (missingFields.isNotEmpty()) {
            Toast.makeText(
                context,
                "Open normal post to recreate this job; missing ${missingFields.joinToString()}",
                Toast.LENGTH_LONG
            ).show()
            navController.navigate(Routes.EMPLOYER_POST_JOB)
            return
        }

        repostingJobId = job.id
        scope.launch {
            runCatching {
                val employerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "GUEST"
                employerJobViewModel.jobDraftDataStore.saveDraft(job.toRepostDraft().copy(employerId = employerId))
            }.onSuccess {
                Toast.makeText(context, context.getString(R.string.review_publish_repost), Toast.LENGTH_SHORT).show()
                navController.navigate(Routes.EMPLOYER_POST_JOB)
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: context.getString(R.string.unable_prepare_repost), Toast.LENGTH_LONG).show()
            }
            repostingJobId = null
        }
    }
    
    // Filter jobs based on selected tab - with null safety
    val filteredJobs = remember(uiState.myJobs, selectedTab) {
        try {
            when (selectedTab) {
                0 -> uiState.myJobs.sortedByDescending { it.createdAt } // Timeline - all sorted by date
                1 -> uiState.myJobs.filter { 
                    // Active jobs that haven't expired (using calculated expiry)
                    it.status == "open" && !it.isExpired()
                }
                2 -> uiState.myJobs.filter { 
                    // Expired jobs (using calculated expiry)
                    it.isExpired()
                }
                else -> uiState.myJobs
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error filtering jobs")
            emptyList()
        }
    }
    
    // Group jobs by month for timeline view - with null safety
    val groupedJobs = remember(filteredJobs) {
        try {
            filteredJobs.groupBy { job ->
                val calendar = Calendar.getInstance().apply { timeInMillis = job.createdAt }
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(calendar.time).uppercase()
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Error grouping jobs")
            emptyMap()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        // Common Header
        CommonHeader(
            title = stringResource(R.string.job_posting_history),
            navController = navController
        )
        
        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
            contentColor = EmployerColors.Primary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = EmployerColors.Primary,
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
        if (selectedTab == urgentTabIndex) {
            EmployerUrgentNeedHistoryContent(
                requests = instantHelpState.employerInstantRequests,
                responsesByRequestId = instantHelpState.employerInstantResponses,
                isLoading = instantHelpState.isLoadingEmployerUrgentNeeds,
                updatingRequestId = instantHelpState.updatingEmployerRequestId,
                updatingResponseId = instantHelpState.updatingEmployerResponseId,
                ratedResponseIds = ratedResponseIds,
                onOpenRequest = { request -> navController.navigate(Routes.employerUrgentNeedDetailRoute(request.requestId)) },
                onOpenWorkerProfile = { response -> navController.navigate(Routes.workerProfileViewRoute(response.workerId)) },
                onCallWorker = { phone ->
                    if (phone.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    }
                },
                onSelectResponse = { response -> instantHelpViewModel.acceptEmployerInstantResponse(response) },
                onCompleteResponse = { response -> instantHelpViewModel.completeEmployerInstantResponse(response, "Completed from employer history") },
                onNoShowResponse = { response -> instantHelpViewModel.markEmployerInstantResponseNoShow(response, "Worker did not come") },
                onRateResponse = { response ->
                    pendingRatingResponse = response
                    showRatingSheet = true
                },
                onMarkRequestFilled = { request -> instantHelpViewModel.markEmployerInstantRequestFilled(request) },
                onCancelRequest = { request -> instantHelpViewModel.cancelEmployerInstantRequest(request, "Cancelled from employer history") },
                onPostUrgentNeed = { navController.navigate(Routes.EMPLOYER_POST_URGENT_NEED) }
            )
        } else when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmployerColors.Primary)
                }
            }
            uiState.hasError -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = EmployerColors.Error,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = uiState.error ?: stringResource(R.string.history_failed_load_jobs),
                            style = MaterialTheme.typography.bodyLarge,
                            color = EmployerColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { employerJobViewModel.loadMyJobs() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmployerColors.Primary
                            )
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            filteredJobs.isEmpty() -> {
                EmptyHistoryState(selectedTab = selectedTab)
            }
            else -> {
                when (selectedTab) {
                    0 -> {
                        TimelineView(
                            groupedJobs = groupedJobs,
                            currentTime = currentTime,
                            repostingJobId = repostingJobId,
                            onJobClick = { job ->
                                navController.navigate(Routes.employerJobPreviewRoute(job.id))
                            },
                            onRepostExpiredJob = { job -> repostExpiredJob(job) }
                        )
                }
                else -> {
                    // List View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredJobs,
                            key = { job -> "emphistory_${job.id}" },
                            contentType = { "employer_history_card" }
                        ) { job ->
                            HistoryJobCard(
                                job = job,
                                currentTime = currentTime,
                                isReposting = repostingJobId == job.id,
                                onClick = {
                                    navController.navigate(Routes.employerJobPreviewRoute(job.id))
                                },
                                onRepostExpiredJob = { repostExpiredJob(job) }
                            )
                        }
                    }
                }
            }
            }  // Close else block
        }  // Close outer when
    }
}

@Composable
private fun TimelineView(
    groupedJobs: Map<String, List<JobListing>>,
    currentTime: Long,
    repostingJobId: String?,
    onJobClick: (JobListing) -> Unit,
    onRepostExpiredJob: (JobListing) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
    ) {
        groupedJobs.forEach { (monthYear, jobs) ->
            // Month Header
            item(key = "header_$monthYear") {
                MonthHeader(monthYear = monthYear)
            }
            
            // Timeline items for this month
            items(
                items = jobs,
                key = { job -> "emptimeline_${job.id}" },
                contentType = { "employer_timeline_card" }
            ) { job ->
                val isLastInMonth = jobs.last() == job
                TimelineJobCard(
                    job = job,
                    currentTime = currentTime,
                    isLastInMonth = isLastInMonth,
                    isReposting = repostingJobId == job.id,
                    onClick = { onJobClick(job) },
                    onRepostExpiredJob = { onRepostExpiredJob(job) }
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
                .background(EmployerColors.Primary),
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
                color = com.example.dutype.ui.theme.EmployerColors.TextPrimary,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
private fun TimelineJobCard(
    job: JobListing,
    currentTime: Long,
    isLastInMonth: Boolean,
    isReposting: Boolean,
    onClick: () -> Unit,
    onRepostExpiredJob: () -> Unit
) {
    val lineColor = EmployerColors.Border
    val isExpired = job.isExpired() // Use calculated expiry
    val isClosed = job.status != "open"
    
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
                        when {
                            isExpired -> EmployerColors.Error
                            isClosed -> EmployerColors.TextSecondary
                            else -> EmployerColors.Success
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isExpired -> Icons.Default.EventBusy
                        isClosed -> Icons.Default.Cancel
                        else -> Icons.Default.CheckCircle
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
            colors = CardDefaults.cardColors(
                containerColor = if (isExpired || isClosed) EmployerColors.ChipBackground else EmployerColors.CardBackground
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Status badge and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JobStatusBadge(
                        isClosed = isClosed,
                        isExpired = isExpired
                    )
                    
                    Text(
                        text = formatTimelineDate(job.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EmployerColors.TextTertiary
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Job title
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isExpired || isClosed) EmployerColors.TextSecondary else EmployerColors.TextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Category - auto-detected
                Text(
                    text = job.getCategory(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EmployerColors.TextSecondary
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Job details row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(
                        icon = Icons.Default.CurrencyRupee,
                        text = "₹${job.salary.ifBlank { "-" }}",
                        backgroundColor = EmployerColors.SuccessLight,
                        iconColor = EmployerColors.Success
                    )
                    
                    InfoChip(
                        icon = Icons.Default.LocationOn,
                        text = job.addressText.ifBlank { job.location }.take(15),
                        backgroundColor = EmployerColors.ChipBackground,
                        iconColor = EmployerColors.TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                    // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Applications count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = EmployerColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.history_view_applications),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EmployerColors.Primary
                            )
                        )
                    }
                    
                    // Expiry info - using calculated expiry (30 days from postedAt)
                    val expiresAt = job.expiresAt
                    val daysLeft = ((expiresAt - currentTime) / (24 * 60 * 60 * 1000)).toInt()
                    Text(
                        text = when {
                            isExpired -> stringResource(R.string.history_expired)
                            daysLeft == 0 -> stringResource(R.string.history_expires_today)
                            daysLeft == 1 -> stringResource(R.string.history_1_day_left)
                            else -> stringResource(R.string.history_days_left, daysLeft)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isExpired) EmployerColors.Error 
                                   else if (daysLeft <= 2) EmployerColors.Warning
                                   else EmployerColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                if (isExpired) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onRepostExpiredJob,
                        enabled = !isReposting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isReposting) "Reposting..." else "Repost same job")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(selectedTab: Int) {
    val (message, subMessage, icon) = when (selectedTab) {
        0 -> Triple(stringResource(R.string.history_no_job_posting), stringResource(R.string.history_start_posting_timeline), Icons.Default.Timeline)
        1 -> Triple(stringResource(R.string.history_no_active_jobs), stringResource(R.string.history_active_postings_here), Icons.Default.CheckCircle)
        2 -> Triple(stringResource(R.string.history_no_expired_jobs), stringResource(R.string.history_expired_postings_here), Icons.Default.EventBusy)
        3 -> Triple(stringResource(R.string.history_no_jobs_posted), stringResource(R.string.history_start_posting_here), Icons.Default.WorkHistory)
        else -> Triple(stringResource(R.string.history_no_jobs), stringResource(R.string.history_postings_appear_here), Icons.Default.WorkHistory)
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
                    .background(EmployerColors.ChipBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmployerColors.TextTertiary,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmployerColors.TextSecondary
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = subMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EmployerColors.TextTertiary
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HistoryJobCard(
    job: JobListing,
    currentTime: Long,
    isReposting: Boolean,
    onClick: () -> Unit,
    onRepostExpiredJob: () -> Unit
) {
    val isExpired = job.isExpired() // Use calculated expiry
    val isClosed = job.status != "open"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired || isClosed) EmployerColors.ChipBackground else EmployerColors.CardBackground
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
                            color = if (isExpired || isClosed) EmployerColors.TextSecondary else EmployerColors.TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = job.getCategory(), // Use auto-detected category
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EmployerColors.TextSecondary
                        )
                    )
                }
                
                JobStatusBadge(
                    isClosed = isClosed,
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
                    text = job.addressText.ifBlank { job.location }.take(20),
                    backgroundColor = EmployerColors.ChipBackground,
                    iconColor = EmployerColors.TextSecondary
                )
                InfoChip(
                    icon = Icons.Default.CurrencyRupee,
                    text = "₹${job.salary.ifBlank { "-" }}",
                    backgroundColor = EmployerColors.SuccessLight,
                    iconColor = EmployerColors.Success
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = EmployerColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.history_view_applications),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmployerColors.Primary
                        )
                    )
                }
                
                // Expiry info - using calculated expiry (30 days from postedAt)
                val expiresAt = job.expiresAt
                val daysLeft = ((expiresAt - currentTime) / (24 * 60 * 60 * 1000)).toInt()
                Text(
                    text = when {
                        isExpired -> stringResource(R.string.history_expired)
                        daysLeft == 0 -> stringResource(R.string.history_expires_today)
                        daysLeft == 1 -> stringResource(R.string.history_1_day_left)
                        else -> stringResource(R.string.history_days_left, daysLeft)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isExpired) EmployerColors.Error 
                               else if (daysLeft <= 2) EmployerColors.Warning
                               else EmployerColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.history_posted_date, formatDate(job.createdAt)),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EmployerColors.TextTertiary
                )
            )

            if (isExpired) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRepostExpiredJob,
                    enabled = !isReposting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isReposting) "Reposting..." else "Repost same job")
                }
            }
        }
    }
}

private fun JobListing.missingRepostFields(): List<String> {
    return buildList {
        if (title.isBlank()) add("title")
        if (salary.isBlank()) add("pay")
        if (addressText.ifBlank { location }.isBlank() || !com.example.dutype.utils.GeoUtils.hasValidCoordinates(lat, lng)) add("location")
        if (contactNumber.isBlank()) add("contact")
    }
}

private fun JobListing.toRepostDraft(): JobDraftDataStore.JobDraft {
    val descriptionText = description.ifBlank {
        buildString {
            append(title)
            if (salary.isNotBlank()) append(". Pay: ").append(salary)
            append('.')
        }
    }
    val inferredCategory = com.example.dutype.utils.JobCategoryResolver.inferCategory(title, descriptionText) ?: JobCategory.OTHER
    val customCategoryValue = when {
        inferredCategory != JobCategory.OTHER -> ""
        title.isNotBlank() -> title
        else -> com.example.dutype.utils.CategoryDetector.detectCategory(title, descriptionText)
            .takeUnless { it.equals("Other", ignoreCase = true) }
            .orEmpty()
    }

    return JobDraftDataStore.JobDraft(
        title = title,
        description = descriptionText,
        payAmount = salary,
        payType = salaryType.toDraftPayType(),
        location = addressText.ifBlank { location },
        locationLatitude = lat,
        locationLongitude = lng,
        category = inferredCategory,
        customCategory = customCategoryValue,
        vacancies = vacancies.coerceAtLeast(1).toString(),
        contactNumber = contactNumber,
        shiftTiming = shiftTiming.toDraftShiftTiming(),
        workType = jobType.ifBlank { "Part-time" },
        experienceLevel = experienceRequired.ifBlank { "No Experience Required" },
        educationRequired = educationRequired.ifBlank { "No qualification required" },
        gender = gender.ifBlank { "Any" },
        repostOfJobId = id,
    )
}

private fun String.toDraftPayType(): PayType {
    return PayType.entries.firstOrNull {
        it.name.equals(this, ignoreCase = true) || it.displayName.equals(this, ignoreCase = true)
    } ?: PayType.HOURLY
}

private fun String.toDraftShiftTiming(): ShiftTiming {
    return ShiftTiming.entries.firstOrNull {
        it.name.equals(this, ignoreCase = true) || it.displayName.equals(this, ignoreCase = true)
    } ?: ShiftTiming.FLEXIBLE
}

@Composable
private fun JobStatusBadge(
    isClosed: Boolean,
    isExpired: Boolean
) {
    val (color, text, icon) = when {
        isExpired -> Triple(EmployerColors.Error, stringResource(R.string.history_expired), Icons.Default.EventBusy)
        isClosed -> Triple(EmployerColors.TextSecondary, "Closed", Icons.Default.Cancel)
        else -> Triple(EmployerColors.Success, "Open", Icons.Default.CheckCircle)
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
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
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    backgroundColor: Color = EmployerColors.ChipBackground,
    iconColor: Color = EmployerColors.TextSecondary
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
                    color = EmployerColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// NOTE: formatDate() removed - use DateTimeUtils.formatRelativeTime() instead
private fun formatDate(timestamp: Long): String {
    return DateTimeUtils.formatRelativeTime(timestamp)
}

private fun formatTimelineDate(timestamp: Long): String {
    return DateTimeUtils.formatDate(timestamp)
}
