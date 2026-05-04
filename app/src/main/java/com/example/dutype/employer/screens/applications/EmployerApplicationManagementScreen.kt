package com.example.dutype.employer.screens.applications

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dutype.components.ApplicationListItemShimmer
import com.example.dutype.components.ApplicationStatusBadge
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.RatingBottomSheet
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobListing
import com.example.dutype.models.MatchedWorker
import com.example.dutype.models.getDisplayName
import com.example.dutype.models.getStatusColor
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Enterprise-level Application Management Screen for Employers
 * Professional design with comprehensive application tracking
 */
private const val MANY_APPLICANTS_THRESHOLD = 12

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerApplicationManagementScreen(
    jobId: String? = null,
    onApplicationClick: (JobApplication) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: EmployerApplicationViewModel = hiltViewModel()
    val jobViewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val reviewTriggerService = rememberInAppReviewTriggerService()
    val scope = rememberCoroutineScope()
    val ratingService = remember {
        com.example.dutype.services.RatingService(
            com.example.dutype.di.firestoreFromHilt(context),
            com.example.dutype.di.authFromHilt(context)
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val matchedWorkersState by viewModel.matchedWorkersState.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showStatusFilter by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // Batch-p #7: filter chips at top — Total / Applied / Shortlisted /
    // Hired. Selecting one filters the application list locally so the
    // counts in the header stay accurate while the user drills in.
    var statusFilter by remember { mutableStateOf<ApplicationStatus?>(null) }
    
    // FINTECH: Contact Unlock Dialog State
    var showUnlockDialog by remember { mutableStateOf(false) }
    var pendingUnlockApplication by remember { mutableStateOf<JobApplication?>(null) }
    var pendingRatingApplication by remember { mutableStateOf<JobApplication?>(null) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var ratedApplicationIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var reportSummary by remember(jobId) { mutableStateOf<JobReportSummary?>(null) }
    var isReportSummaryLoading by remember(jobId) { mutableStateOf(false) }
    var currentJob by remember(jobId) { mutableStateOf<JobListing?>(null) }
    var isJobClosedOverride by remember(jobId) { mutableStateOf(false) }
    var showCloseJobDialog by remember { mutableStateOf(false) }
    var isClosingJob by remember { mutableStateOf(false) }
    
    LaunchedEffect(jobId) {
        if (jobId == null) {
            currentJob = null
            isJobClosedOverride = false
            viewModel.loadEmployerApplications()
        } else {
            viewModel.loadMatchedWorkers(jobId)
            viewModel.loadJobApplications(jobId)
            jobViewModel.getJobById(jobId) { job ->
                currentJob = job
                isJobClosedOverride = job?.status?.equals("closed", ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(jobId) {
        if (jobId.isNullOrBlank()) {
            reportSummary = null
            isReportSummaryLoading = false
            return@LaunchedEffect
        }

        isReportSummaryLoading = true
        reportSummary = fetchJobReportSummary(jobId)
        isReportSummaryLoading = false
    }
    
    // Handle search
    LaunchedEffect(searchQuery, selectedTabIndex, jobId) {
        if (jobId == null || selectedTabIndex == 1) {
            viewModel.searchApplications(searchQuery)
        }
    }

    LaunchedEffect(uiState.applications) {
        val completedApplications = uiState.applications.filter { it.status == ApplicationStatus.COMPLETED }
        ratedApplicationIds = completedApplications.mapNotNull { application ->
            if (ratingService.hasRated(application.jobId, application.workerId)) application.id else null
        }.toSet()
    }
    
    // FINTECH: Contact Unlock Payment Dialog
    if (showUnlockDialog && pendingUnlockApplication != null) {
        ContactUnlockDialog(
            application = pendingUnlockApplication!!,
            unlockPrice = viewModel.getContactUnlockPrice(),
            isProcessing = isProcessingPayment,
            onDismiss = { 
                showUnlockDialog = false
                pendingUnlockApplication = null
            },
            onConfirmPayment = {
                isProcessingPayment = true
                viewModel.processContactUnlockPayment(
                    applicationId = pendingUnlockApplication!!.id,
                    onSuccess = {
                        isProcessingPayment = false
                        showUnlockDialog = false
                        pendingUnlockApplication = null
                        Toast.makeText(context, context.getString(R.string.contact_unlocked), Toast.LENGTH_SHORT).show()
                        val activity = context as? Activity
                        if (activity != null) {
                            reviewTriggerService.onEmployerContactUnlocked(activity)
                        }
                    },
                    onFailure = { error ->
                        isProcessingPayment = false
                        Toast.makeText(context, context.getString(R.string.payment_failed_error, error), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    if (showRatingSheet && pendingRatingApplication != null) {
        RatingBottomSheet(
            isVisible = showRatingSheet,
            targetName = pendingRatingApplication!!.workerName.ifBlank { "this worker" },
            targetRole = "WORKER",
            onDismiss = {
                showRatingSheet = false
                pendingRatingApplication = null
            },
            onSubmit = { rating, review, tags ->
                pendingRatingApplication?.let { application ->
                    scope.launch {
                        ratingService.submitRating(
                            jobId = application.jobId,
                            targetUserId = application.workerId,
                            rating = rating,
                            review = review,
                            tags = tags,
                            targetRole = "WORKER"
                        ).fold(
                            onSuccess = { result ->
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                if (result.success) {
                                    ratedApplicationIds = ratedApplicationIds + application.id
                                    showRatingSheet = false
                                    pendingRatingApplication = null
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

    if (showCloseJobDialog && jobId != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isClosingJob) showCloseJobDialog = false
            },
            title = { Text(stringResource(R.string.close_job_when_filled)) },
            text = {
                Text(stringResource(R.string.close_job_when_filled_body))
            },
            confirmButton = {
                TextButton(
                    enabled = !isClosingJob,
                    onClick = {
                        isClosingJob = true
                        jobViewModel.updateJob(jobId, mapOf("status" to "closed")) { success, error ->
                            isClosingJob = false
                            if (success) {
                                showCloseJobDialog = false
                                isJobClosedOverride = true
                                currentJob = currentJob?.copy(status = "closed")
                                Toast.makeText(context, context.getString(R.string.job_closed_as_filled), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, error ?: context.getString(R.string.unable_to_close_job), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text(if (isClosingJob) stringResource(R.string.closing_ellipsis) else stringResource(R.string.close_job))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isClosingJob,
                    onClick = { showCloseJobDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val jobTitleForActions = currentJob?.title
        ?.takeIf { it.isNotBlank() }
        ?: uiState.applications.firstOrNull()?.jobTitle?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.dutype_job)
    val applicantCountForSummary = when {
        uiState.applications.isNotEmpty() -> uiState.applications.size
        currentJob != null -> currentJob?.applicationCount ?: 0
        else -> stats.totalApplications
    }
    val isJobLive = !isJobClosedOverride && (currentJob?.status?.equals("open", ignoreCase = true) ?: true)
    val callReadyCandidates = remember(uiState.applications, matchedWorkersState.workers, isJobLive, jobId) {
        val visibleApplicationPool = if (jobId != null && !isJobLive) {
            uiState.applications.filter { it.status in filledApplicationStatuses }
        } else {
            uiState.applications
        }
        visibleApplicationPool.count { it.isConnectNowCandidate() } +
            matchedWorkersState.workers.count { it.isCallReadyMatch() }
    }
    val shortlistedCount = remember(uiState.applications) {
        uiState.applications.count { it.status == ApplicationStatus.SHORTLISTED }
    }
    val hiredCount = remember(uiState.applications) {
        uiState.applications.count { it.status in filledApplicationStatuses }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        // Common Header - consistent across all screens
        CommonHeader(
            title = if (jobId != null) "Hiring Room" else "All Applications",
            onBackClick = onBackClick
        )

        if (jobId != null && (isReportSummaryLoading || reportSummary != null)) {
            JobReportSummaryCard(
                summary = reportSummary,
                isLoading = isReportSummaryLoading
            )
        }

        if (jobId != null) {
            HiringRoomSummaryCard(
                jobTitle = jobTitleForActions,
                isLive = isJobLive,
                applicantsCount = applicantCountForSummary,
                matchedWorkersCount = matchedWorkersState.workers.size,
                callReadyCandidates = callReadyCandidates,
                isClosingJob = isClosingJob,
                onCloseJob = { showCloseJobDialog = true }
            )

        }

        if (jobId != null) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground,
                contentColor = Color(0xFF1F2937)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.hiring_room_best_matches)) },
                    icon = { Icon(Icons.Default.Verified, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(R.string.hiring_room_applied_workers)) },
                    icon = { Icon(Icons.Default.Work, contentDescription = null) }
                )
            }
        }

        if (jobId != null && selectedTabIndex == 0) {
            MatchedWorkersContent(
                state = matchedWorkersState,
                isJobLive = isJobLive,
                onRefresh = { viewModel.loadMatchedWorkers(jobId, force = true) },
                onRequestWorker = { worker -> viewModel.requestMatchedWorker(jobId, worker.workerId) },
                onCallWorker = { phone ->
                    if (phone.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    }
                },
                modifier = Modifier.weight(1f)
            )
        } else {
        
        // FINTECH: Free contacts remaining banner
        if (uiState.freeContactsRemaining > 0 && uiState.applications.size > 3) {
            FreeContactsBanner(freeRemaining = uiState.freeContactsRemaining)
        }
        
        // Stats Summary Card — click any item to filter the list
        ApplicationStatsSummary(
            stats = stats,
            selectedFilter = statusFilter,
            onFilterSelected = { statusFilter = it }
        )

        val rankedApplications = remember(uiState.applications) {
            uiState.applications.sortedApplicationsForConnectNow()
        }

        val visibleApplications = remember(rankedApplications, isJobLive, jobId) {
            if (jobId != null && !isJobLive) {
                rankedApplications.filter { it.status in filledApplicationStatuses }
            } else {
                rankedApplications
            }
        }

        val displayedApplications = remember(visibleApplications, statusFilter) {
            if (statusFilter == null) visibleApplications
            else visibleApplications.filter { it.status == statusFilter }
        }
        
        // Applications List
        when {
            uiState.isLoading -> {
                // Show shimmer loading for application list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) {
                        ApplicationListItemShimmer()
                    }
                }
            }
            uiState.applications.isEmpty() && !uiState.isLoading -> {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyApplicationsState(
                        isJobSpecific = jobId != null
                    )
                }
            }
            displayedApplications.isEmpty() -> {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyApplicationsState(
                        isJobSpecific = false
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (jobId != null && uiState.applications.size >= MANY_APPLICANTS_THRESHOLD && statusFilter == null) {
                        item {
                            TooManyApplicationsBanner(
                                totalApplications = uiState.applications.size,
                                callReadyCandidates = callReadyCandidates,
                                isJobLive = isJobLive,
                                onCloseJob = { showCloseJobDialog = true }
                            )
                        }
                    } else if (jobId != null && statusFilter == null) {
                        item {
                            RankingHintBanner()
                        }
                    }

                    itemsIndexed(displayedApplications) { index, application ->
                        val isContactUnlocked = viewModel.isContactUnlocked(application.id, index)
                        
                        ApplicationCard(
                            application = application,
                            applicationIndex = index,
                            isContactUnlocked = isContactUnlocked,
                            hasAlreadyRated = application.id in ratedApplicationIds,
                            onClick = { 
                                onApplicationClick(application) 
                            },
                            onUnlockContact = {
                                viewModel.unlockContact(
                                    applicationId = application.id,
                                    onSuccess = {
                                        Toast.makeText(context, context.getString(R.string.contact_unlocked), Toast.LENGTH_SHORT).show()
                                        val activity = context as? Activity
                                        if (activity != null) {
                                            reviewTriggerService.onEmployerContactUnlocked(activity)
                                        }
                                    },
                                    onPaymentRequired = {
                                        pendingUnlockApplication = application
                                        showUnlockDialog = true
                                    }
                                )
                            },
                            onStatusUpdate = { newStatus, notes ->
                                viewModel.updateApplicationStatus(
                                    applicationId = application.id,
                                    newStatus = newStatus,
                                    notes = notes
                                )
                            },
                            onRateWorker = {
                                scope.launch {
                                    if (application.status == ApplicationStatus.HIRED) {
                                        val completed = viewModel.updateApplicationStatusForResult(
                                            applicationId = application.id,
                                            newStatus = ApplicationStatus.COMPLETED,
                                            notes = "Marked work done from applications list"
                                        )
                                        if (completed.isFailure) {
                                            Toast.makeText(
                                                context,
                                                completed.exceptionOrNull()?.message ?: "Failed to mark work done",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }
                                        Toast.makeText(context, context.getString(R.string.work_marked_done_rate_now), Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val alreadyRated = ratingService.hasRated(application.jobId, application.workerId)
                                    if (alreadyRated) {
                                        ratedApplicationIds = ratedApplicationIds + application.id
                                        Toast.makeText(context, context.getString(R.string.already_rated_worker), Toast.LENGTH_SHORT).show()
                                    } else {
                                        pendingRatingApplication = application
                                        showRatingSheet = true
                                    }
                                }
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
private fun MatchedWorkersContent(
    state: com.example.dutype.viewmodels.MatchedWorkersUiState,
    isJobLive: Boolean,
    onRefresh: () -> Unit,
    onRequestWorker: (MatchedWorker) -> Unit,
    onCallWorker: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) { ApplicationListItemShimmer() }
            }
        }
        state.hasError -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = state.error ?: "Failed to load matched workers",
                        style = AppTypography.bodyMedium.copy(color = Color(0xFF991B1B)),
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onRefresh, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
        state.workers.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = if (isJobLive) Icons.Default.Search else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = if (isJobLive) stringResource(R.string.no_strong_worker_matches) else stringResource(R.string.job_is_filled),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isJobLive) {
                            "Matches improve when workers add skills and location."
                        } else {
                            "Accepted workers will stay visible here when available."
                        },
                        style = AppTypography.bodySmall.copy(color = Color(0xFF6B7280)),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        else -> {
            val rankedWorkers = remember(state.workers) {
                state.workers.sortedMatchedWorkersForConnectNow()
            }
            val visibleWorkers = remember(rankedWorkers, isJobLive) {
                if (isJobLive) {
                    rankedWorkers
                } else {
                    rankedWorkers.sortedWith(
                        compareByDescending<MatchedWorker> { it.requestStatus.equals("accepted", ignoreCase = true) }
                            .thenByDescending { it.connectNowScore() }
                    )
                }
            }

            if (visibleWorkers.isEmpty()) {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = stringResource(R.string.job_is_filled),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "New matches are hidden. Accepted workers will stay visible here.",
                            style = AppTypography.bodySmall.copy(color = Color(0xFF6B7280)),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = if (isJobLive) stringResource(R.string.best_workers_for_job) else stringResource(R.string.filled_job_matches),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isJobLive) {
                                "Nearest workers are shown first, then availability and profile strength."
                            } else {
                                "Selected workers stay active. Remaining matches are shown as disabled cards."
                            },
                            style = AppTypography.bodySmall.copy(color = Color(0xFF6B7280))
                        )
                        if (!state.actionError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.actionError,
                                style = AppTypography.bodySmall.copy(color = Color(0xFFB91C1C))
                            )
                        }
                    }

                    items(visibleWorkers, key = { it.workerId }) { worker ->
                        MatchedWorkerCard(
                            worker = worker,
                            isJobLive = isJobLive,
                            isDisabledForFilledJob = !isJobLive && !worker.requestStatus.equals("accepted", ignoreCase = true),
                            isRequesting = state.requestingWorkerId == worker.workerId,
                            onRequestWorker = { onRequestWorker(worker) },
                            onCallWorker = { onCallWorker(worker.phone) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HiringRoomSummaryCard(
    jobTitle: String,
    isLive: Boolean,
    applicantsCount: Int,
    matchedWorkersCount: Int,
    callReadyCandidates: Int,
    isClosingJob: Boolean,
    onCloseJob: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = jobTitle.ifBlank { if (isLive) stringResource(R.string.open_job) else stringResource(R.string.closed_job) },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isLive) Color(0xFFDCFCE7) else Color(0xFFE5E7EB)
                ) {
                    Text(
                        text = if (isLive) stringResource(R.string.live) else stringResource(R.string.filled),
                        style = AppTypography.caption.copy(
                            color = if (isLive) Color(0xFF047857) else Color(0xFF374151),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    HiringRoomMetricItem(
                        value = applicantsCount.toString(),
                        label = stringResource(R.string.applicants),
                        icon = Icons.Default.Person,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    HiringRoomMetricItem(
                        value = matchedWorkersCount.toString(),
                        label = stringResource(R.string.nearby_matches),
                        icon = Icons.Default.Work,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    HiringRoomMetricItem(
                        value = callReadyCandidates.toString(),
                        label = stringResource(R.string.call_ready),
                        icon = Icons.Default.Call,
                        color = Color(0xFF059669),
                        modifier = Modifier.weight(1f)
                    )
                    HiringRoomMetricItem(
                        value = if (isLive) stringResource(R.string.open_status) else stringResource(R.string.done_status),
                        label = stringResource(R.string.hiring_status),
                        icon = Icons.Default.CheckCircle,
                        color = if (isLive) Color(0xFFEA580C) else Color(0xFF6B7280),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCloseJob,
                    enabled = isLive && !isClosingJob,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isClosingJob) {
                            stringResource(R.string.closing)
                        } else if (isLive) {
                            stringResource(R.string.close_job)
                        } else {
                            stringResource(R.string.closed)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HiringRoomMetricItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.09f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(
                text = value,
                style = AppTypography.labelLarge.copy(color = color, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = AppTypography.caption.copy(color = Color(0xFF6B7280)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RankingHintBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(R.string.ranking_hint_best_first),
            style = AppTypography.bodySmall.copy(color = Color(0xFF1E40AF)),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TooManyApplicationsBanner(
    totalApplications: Int,
    callReadyCandidates: Int,
    isJobLive: Boolean,
    onCloseJob: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.best_first_for_applicants, totalApplications),
            style = AppTypography.labelLarge.copy(color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
        )
        Text(
            text = stringResource(R.string.call_ready_lifted_first, callReadyCandidates),
            style = AppTypography.bodySmall.copy(color = Color(0xFF92400E))
        )
        if (isJobLive) {
            OutlinedButton(
                onClick = onCloseJob,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF92400E))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.close_job_when_filled))
            }
        }
    }
}

@Composable
private fun MatchedWorkerCard(
    worker: MatchedWorker,
    isJobLive: Boolean,
    isDisabledForFilledJob: Boolean,
    isRequesting: Boolean,
    onRequestWorker: () -> Unit,
    onCallWorker: () -> Unit
) {
    val status = worker.requestStatus.lowercase(Locale.ROOT)
    val requestSent = status in setOf("pending", "accepted")
    val canCall = worker.phone.isNotBlank() && !isDisabledForFilledJob
    val workerStatusText = when (status) {
        "accepted" -> stringResource(R.string.selected_worker)
        "pending" -> stringResource(R.string.request_sent)
        "rejected" -> stringResource(R.string.not_available_for_job)
        else -> if (isDisabledForFilledJob) stringResource(R.string.job_is_filled) else if (worker.isAvailable) stringResource(R.string.available_now) else stringResource(R.string.invite_to_confirm)
    }
    val contentAlpha = if (isDisabledForFilledJob) 0.58f else 1f
    val cardContainerColor = if (isDisabledForFilledJob) {
        Color(0xFFF3F4F6)
    } else {
        com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDisabledForFilledJob) 0.dp else 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(if (isDisabledForFilledJob) Color(0xFFE5E7EB) else Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (worker.profileImageUrl.isNotBlank()) {
                        com.example.dutype.components.OptimizedProfileImage(
                            imageUrl = worker.profileImageUrl,
                            contentDescription = "Worker profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = worker.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827).copy(alpha = contentAlpha)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = workerStatusText,
                        style = AppTypography.caption.copy(color = Color(0xFF6B7280)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (status.isNotBlank()) {
                    RequestStatusPill(status)
                }
            }

            MatchedWorkerMetricsGrid(worker = worker)

            if (status == "accepted") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5)
                ) {
                    Text(
                        text = if (isJobLive) {
                            stringResource(R.string.selected_keep_job_open)
                        } else {
                            stringResource(R.string.selected_filled_job)
                        },
                        style = AppTypography.bodySmall.copy(color = Color(0xFF047857)),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (isDisabledForFilledJob) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE5E7EB)
                ) {
                    Text(
                        text = stringResource(R.string.disabled_job_filled),
                        style = AppTypography.bodySmall.copy(color = Color(0xFF4B5563)),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (worker.skills.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(worker.skills.take(5)) { skill ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF3F4F6)
                        ) {
                            Text(
                                text = skill.replaceFirstChar { it.titlecase(Locale.ROOT) },
                                style = AppTypography.caption.copy(color = Color(0xFF374151)),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            if (worker.matchReasons.isNotEmpty()) {
                Text(
                    text = worker.matchReasons.joinToString(" • "),
                    style = AppTypography.bodySmall.copy(color = Color(0xFF4B5563)),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRequestWorker,
                    enabled = isJobLive && !requestSent && !isRequesting && !isDisabledForFilledJob,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isRequesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when {
                            !isJobLive -> stringResource(R.string.job_is_filled)
                            requestSent -> stringResource(R.string.requested)
                            else -> stringResource(R.string.request)
                        }
                    )
                }

                Button(
                    onClick = onCallWorker,
                    enabled = canCall && !isDisabledForFilledJob,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.call))
                }
            }
        }
    }
}

@Composable
private fun MatchedWorkerMetricsGrid(worker: MatchedWorker) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        WorkerMetricBlock(
            icon = Icons.Default.LocationOn,
            label = "Away",
            value = formatWorkerDistanceShort(worker.distanceKm),
            color = Color(0xFF2563EB),
            modifier = Modifier.weight(1f)
        )
        WorkerMetricBlock(
            icon = Icons.Default.CheckCircle,
            label = "Jobs done",
            value = worker.completedJobs.toString(),
            color = Color(0xFF059669),
            modifier = Modifier.weight(1f)
        )
        WorkerMetricBlock(
            icon = Icons.Default.Star,
            label = formatWorkerRatingCount(worker.ratingCount),
            value = formatWorkerRatingValue(worker.rating),
            color = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
        WorkerMetricBlock(
            icon = Icons.Default.FlashOn,
            label = "Status",
            value = formatMatchedWorkerStatus(worker),
            color = Color(0xFF7C3AED),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WorkerMetricBlock(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 82.dp)
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            text = value,
            style = AppTypography.labelLarge.copy(color = Color(0xFF111827), fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = AppTypography.caption.copy(color = Color(0xFF6B7280)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RequestStatusPill(status: String) {
    val normalized = status.lowercase(Locale.ROOT)
    val color = when (normalized) {
        "accepted" -> Color(0xFF16A34A)
        "rejected" -> Color(0xFFDC2626)
        "expired" -> Color(0xFF6B7280)
        else -> Color(0xFFF59E0B)
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = normalized.replaceFirstChar { it.titlecase(Locale.ROOT) },
            style = AppTypography.caption.copy(color = color, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private data class ReportPreviewItem(
    val reportType: String,
    val description: String
)

private data class JobReportSummary(
    val reportCount: Int,
    val typeCounts: Map<String, Int>,
    val recentReports: List<ReportPreviewItem>
)

private suspend fun fetchJobReportSummary(jobId: String): JobReportSummary? {
    return try {
        val doc = FirebaseFirestore.getInstance()
            .collection("jobmetadata")
            .document(jobId)
            .get()
            .await()

        val data = doc.data ?: return null
        val reportCount = (data["reportCount"] as? Number)?.toInt() ?: 0
        if (reportCount <= 0) return null

        val typeCounts = mutableMapOf<String, Int>()
        val rawTypeCounts = data["reportTypeCounts"] as? Map<*, *>
        rawTypeCounts?.forEach { (rawType, rawCount) ->
            val type = rawType?.toString()?.trim().orEmpty().ifBlank { "OTHER" }
            val count = (rawCount as? Number)?.toInt() ?: 0
            if (count > 0) {
                typeCounts[type] = count
            }
        }

        val recentReports = (data["reportSamples"] as? List<*>)
            ?.mapNotNull { rawItem ->
                val itemMap = rawItem as? Map<*, *> ?: return@mapNotNull null
                val type = itemMap["reportType"]?.toString()?.trim().orEmpty().ifBlank { "OTHER" }
                val description = itemMap["description"]?.toString()?.trim().orEmpty()
                if (description.isBlank()) return@mapNotNull null
                ReportPreviewItem(reportType = type, description = description)
            }
            .orEmpty()

        JobReportSummary(
            reportCount = reportCount,
            typeCounts = typeCounts,
            recentReports = recentReports
        )
    } catch (_: FirebaseFirestoreException) {
        null
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun JobReportSummaryCard(
    summary: JobReportSummary?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Community Reports",
                style = AppTypography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )
            )

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFB91C1C)
                    )
                    Text(
                        text = "Loading report details...",
                        style = AppTypography.caption.copy(color = Color(0xFF7F1D1D))
                    )
                }
            } else if (summary != null) {
                Text(
                    text = "Reported by ${summary.reportCount} worker(s)",
                    style = AppTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7F1D1D)
                    )
                )

                if (summary.typeCounts.isNotEmpty()) {
                    summary.typeCounts.entries
                        .sortedByDescending { it.value }
                        .forEach { entry ->
                            Text(
                                text = "${entry.key}: ${entry.value}",
                                style = AppTypography.caption.copy(color = Color(0xFF7F1D1D))
                            )
                        }
                }

                if (summary.recentReports.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFFFECACA), thickness = 1.dp)
                    summary.recentReports.take(3).forEach { report ->
                        Text(
                            text = "${report.reportType}: ${report.description}",
                            style = AppTypography.bodySmall.copy(color = Color(0xFF7F1D1D))
                        )
                    }
                }
            }
        }
    }
}

// Stats Summary Card
@Composable
private fun ApplicationStatsSummary(
    stats: com.example.dutype.models.ApplicationStats,
    selectedFilter: ApplicationStatus?,
    onFilterSelected: (ApplicationStatus?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatsSummaryItem(
                value = stats.totalApplications.toString(),
                label = stringResource(R.string.total_label),
                color = Color(0xFF3B82F6),
                isSelected = selectedFilter == null,
                onClick = { onFilterSelected(null) }
            )
            StatsSummaryItem(
                value = stats.appliedApplications.toString(),
                label = stringResource(R.string.applied),
                color = Color(0xFFF59E0B),
                isSelected = selectedFilter == ApplicationStatus.APPLIED,
                onClick = { onFilterSelected(ApplicationStatus.APPLIED) }
            )
            StatsSummaryItem(
                value = stats.shortlistedApplications.toString(),
                label = stringResource(R.string.shortlisted),
                color = Color(0xFF8B5CF6),
                isSelected = selectedFilter == ApplicationStatus.SHORTLISTED,
                onClick = { onFilterSelected(ApplicationStatus.SHORTLISTED) }
            )
            StatsSummaryItem(
                value = stats.hiredApplications.toString(),
                label = stringResource(R.string.hired),
                color = Color(0xFF10B981),
                isSelected = selectedFilter == ApplicationStatus.HIRED,
                onClick = { onFilterSelected(ApplicationStatus.HIRED) }
            )
        }
    }
}

@Composable
private fun StatsSummaryItem(
    value: String,
    label: String,
    color: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = value,
            style = AppTypography.statNumber.copy(color = color)
        )
        Text(
            text = label,
            style = AppTypography.caption.copy(
                color = if (isSelected) color else Color(0xFF6B7280)
            )
        )
    }
}

// NOTE: getStatusColor removed - use ApplicationStatus.getStatusColor() extension function
// Import: import com.example.dutype.models.getStatusColor

@Composable
private fun ApplicationCard(
    application: JobApplication,
    applicationIndex: Int = 0,
    isContactUnlocked: Boolean = true,
    hasAlreadyRated: Boolean = false,
    onClick: () -> Unit,
    onUnlockContact: () -> Unit = {},
    onStatusUpdate: (ApplicationStatus, String?) -> Unit,
    onRateWorker: () -> Unit = {}
) {
    val workerEmail = application.workerEmail.orEmpty()
    val workerPhone = application.workerPhone.orEmpty().trim()
    val context = LocalContext.current
    val canMarkWorkDone = application.status == ApplicationStatus.HIRED
    val canRateCompletedWork = application.status == ApplicationStatus.COMPLETED
    val canCallWorker = workerPhone.isNotBlank() &&
        application.status != ApplicationStatus.REJECTED &&
        application.status != ApplicationStatus.WITHDRAWN
    // Determine display name - fallback to "Unknown Worker" if name is empty
    val displayName = when {
        application.workerName.isNotBlank() -> application.workerName
        else -> "Unknown Worker"
    }
    
    // Get initials for avatar
    val initials = when {
        displayName.isNotBlank() && displayName != "Unknown Worker" -> {
            displayName.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .ifEmpty { displayName.take(1).uppercase() }
        }
        else -> "?"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row - Worker info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Profile Avatar with image support
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!application.workerProfileImageUrl.isNullOrBlank()) {
                            // Show profile image if available
                            com.example.dutype.components.OptimizedProfileImage(
                                imageUrl = application.workerProfileImageUrl,
                                contentDescription = "Worker Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            // Show initials or icon
                            if (initials.isNotBlank() && initials != "?") {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B82F6)
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = displayName,
                            style = AppTypography.cardTitle.copy(color = com.example.dutype.ui.theme.EmployerColors.TextPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (workerEmail.isNotBlank()) {
                            Text(
                                text = workerEmail,
                                style = AppTypography.caption.copy(color = Color(0xFF6B7280)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Worker location removed - not stored on application
                    }
                }
                
                // Status Badge - using centralized component
                ApplicationStatusBadge(status = application.status)
            }
            
            // Apr 2026: skill chips removed from the applicant card. Skills
            // now live only on the worker profile detail screen so the list
            // card stays scannable.

            Spacer(modifier = Modifier.height(12.dp))

            // Batch-p #7: dropped the redundant Job Info card
            // (jobTitle + companyName) from the applicant card. The
            // employer is already inside the job context (filtered list
            // or job-scoped applications screen) so repeating the title
            // on every applicant card just adds noise.

            // Skills preview removed - skills not stored on application
            val skillsToShow = emptyList<String>()
            
            if (skillsToShow.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    skillsToShow.forEach { skill ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = skill,
                                style = AppTypography.labelSmall.copy(
                                    color = Color(0xFF3B82F6)
                                ),
                                maxLines = 1
                            )
                        }
                    }
                    // +more indicator removed along with skills
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer - Applied time and contact info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Applied ${DateTimeUtils.formatRelativeTime(application.createdAt)}",
                        style = AppTypography.caption.copy(color = Color(0xFF9CA3AF))
                    )
                }
                
                // Phone stays hidden as text, but the primary call action is
                // available here so employers can connect without opening the
                // worker profile first.
            }

            if (canCallWorker) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$workerPhone")))
                        }.onFailure {
                            Toast.makeText(context, context.getString(R.string.unable_to_open_dialer), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.call_worker), style = AppTypography.labelLarge, color = Color.White)
                }
            }

            // Quick actions on list card (replaces hidden menu flow)
            if (application.status == ApplicationStatus.APPLIED || application.status == ApplicationStatus.SHORTLISTED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onStatusUpdate(ApplicationStatus.REJECTED, "Rejected from applications list") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.reject), style = AppTypography.labelLarge)
                    }

                    Button(
                        onClick = { onStatusUpdate(ApplicationStatus.HIRED, "Hired from applications list") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.accept), style = AppTypography.labelLarge, color = Color.White)
                    }
                }
            }

            if (canMarkWorkDone || canRateCompletedWork) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (hasAlreadyRated) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (canMarkWorkDone) {
                            "Work completed?"
                        } else if (hasAlreadyRated) {
                            "Review given"
                        } else {
                            "Rate this worker"
                        },
                        style = AppTypography.labelLarge,
                        color = if (hasAlreadyRated) Color(0xFF059669) else Color(0xFFB45309)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRateWorker,
                        enabled = canMarkWorkDone || !hasAlreadyRated,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF59E0B),
                            disabledContentColor = Color(0xFF059669)
                        )
                    ) {
                        Icon(
                            imageVector = if (canMarkWorkDone) Icons.Default.CheckCircle else Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (canMarkWorkDone) "Mark Work Done" else if (hasAlreadyRated) "Rated Worker" else "Rate Worker",
                            style = AppTypography.labelLarge
                        )
                    }
                }
            }
            
            // Cover Letter Preview (if available)
            if (application.coverLetter.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = application.coverLetter.take(120) + if (application.coverLetter.length > 120) "..." else "",
                    style = AppTypography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// NOTE: StatusBadge removed - use centralized ApplicationStatusBadge from components instead

@Composable
private fun EmptyApplicationsState(
    isJobSpecific: Boolean
) {
    val subtitle = if (isJobSpecific) {
        stringResource(R.string.applied_workers_empty_body)
    } else {
        stringResource(R.string.applications_empty_body)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFFF3F4F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = if (isJobSpecific) stringResource(R.string.waiting_applied_workers) else stringResource(R.string.waiting_worker_responses),
            style = AppTypography.emptyStateTitle.copy(color = com.example.dutype.ui.theme.EmployerColors.TextPrimary)
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        )

    }
}




private fun List<JobApplication>.sortedApplicationsForConnectNow(): List<JobApplication> {
    return sortedWith(
        compareByDescending<JobApplication> { it.connectNowScore() }
            .thenByDescending { it.createdAt }
    )
}

private fun List<MatchedWorker>.sortedMatchedWorkersForConnectNow(): List<MatchedWorker> {
    return sortedWith(
        compareBy<MatchedWorker> { it.distanceKm ?: Double.MAX_VALUE }
            .thenByDescending { it.connectNowScore() }
            .thenByDescending { it.matchScore }
    )
}

private fun formatWorkerDistance(distanceKm: Double?): String {
    return when {
        distanceKm == null -> "Nearby"
        distanceKm < 0.1 -> "Under 100 m"
        distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m away"
        else -> String.format(Locale.ROOT, "%.1f km away", distanceKm)
    }
}

private fun formatWorkerDistanceShort(distanceKm: Double?): String {
    return when {
        distanceKm == null -> "Nearby"
        distanceKm < 0.1 -> "<100m"
        distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()}m"
        else -> String.format(Locale.ROOT, "%.1fkm", distanceKm)
    }
}

private fun formatCompletedWork(completedJobs: Int): String {
    return when (completedJobs) {
        0 -> "No jobs yet"
        1 -> "1 job"
        else -> "$completedJobs jobs"
    }
}

private fun formatWorkerRating(rating: Double, ratingCount: Int): String {
    return when {
        rating <= 0.0 -> "New"
        ratingCount > 0 -> String.format(Locale.ROOT, "%.1f (%d)", rating, ratingCount)
        else -> String.format(Locale.ROOT, "%.1f", rating)
    }
}

private fun formatWorkerRatingValue(rating: Double): String {
    return if (rating > 0.0) String.format(Locale.ROOT, "%.1f", rating) else "New"
}

private fun formatWorkerRatingCount(ratingCount: Int): String {
    return when (ratingCount) {
        0 -> "Rating"
        1 -> "1 rating"
        else -> "$ratingCount ratings"
    }
}

private fun formatMatchedWorkerStatus(worker: MatchedWorker): String {
    return when (worker.requestStatus.lowercase(Locale.ROOT)) {
        "accepted" -> "Selected"
        "pending" -> "Waiting"
        "rejected" -> "Declined"
        else -> if (worker.isAvailable) "Available" else "Invite"
    }
}

private fun JobApplication.connectNowScore(): Int {
    var score = 0
    if (status in connectableApplicationStatuses) score += 1_000
    if (!workerPhone.isNullOrBlank()) score += 500
    if (status == ApplicationStatus.SHORTLISTED) score += 120
    if (status == ApplicationStatus.APPLIED) score += 90
    if (status == ApplicationStatus.HIRED) score += 60
    score += profileCompletenessScore()
    return score
}

private fun JobApplication.profileCompletenessScore(): Int {
    var score = 0
    if (workerName.isNotBlank()) score += 40
    if (!workerProfileImageUrl.isNullOrBlank()) score += 30
    if (!workerPhone.isNullOrBlank()) score += 40
    if (!workerEmail.isNullOrBlank()) score += 20
    if (workerSkills.isNotEmpty()) score += 40
    if (workerExperience.isNotBlank()) score += 25
    if (workerEducationQualification.isNotBlank()) score += 20
    if (workerBio.isNotBlank()) score += 20
    return score
}

private fun JobApplication.isConnectNowCandidate(): Boolean {
    return status in connectableApplicationStatuses && !workerPhone.isNullOrBlank()
}

private fun MatchedWorker.connectNowScore(): Int {
    var score = matchScore
    if (isCallReadyMatch()) score += 1_000
    if (isAvailable) score += 180
    if (phone.isNotBlank()) score += 220
    if (distanceKm != null) {
        score += when {
            distanceKm <= 3.0 -> 120
            distanceKm <= 7.0 -> 80
            distanceKm <= 12.0 -> 40
            else -> 0
        }
    }
    if (skills.isNotEmpty()) score += 60
    if (experience.isNotBlank()) score += 30
    if (completedJobs > 0) score += (completedJobs * 12).coerceAtMost(120)
    if (rating > 0.0) score += (rating * 20).toInt().coerceAtMost(100)
    if (ratingCount > 0) score += ratingCount.coerceAtMost(30)
    return score
}

private fun MatchedWorker.isCallReadyMatch(): Boolean {
    return phone.isNotBlank()
}

private val connectableApplicationStatuses = setOf(
    ApplicationStatus.APPLIED,
    ApplicationStatus.SHORTLISTED,
    ApplicationStatus.HIRED
)

private val filledApplicationStatuses = setOf(
    ApplicationStatus.HIRED,
    ApplicationStatus.COMPLETED
)

// Helper functions
// NOTE: getStatusDisplayName removed - use ApplicationStatus.getDisplayName() extension function
// Import: import com.example.dutype.models.getDisplayName

// NOTE: getStatusIcon removed - use ApplicationStatus.getStatusIcon() extension function  
// Import: import com.example.dutype.models.getStatusIcon

// NOTE: getTimeAgo removed - use DateTimeUtils.formatRelativeTime() instead
// Import: import com.example.dutype.utils.DateTimeUtils

// ==================== FINTECH: CONTACT UNLOCK COMPONENTS ====================

/**
 * Banner showing free contacts remaining
 */
@Composable
private fun FreeContactsBanner(freeRemaining: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "🎁 $freeRemaining free contact unlocks remaining",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF065F46),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Dialog for unlocking contact with payment
 */
@Composable
private fun ContactUnlockDialog(
    application: JobApplication,
    unlockPrice: Int,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFFEF3C7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.unlock_contact),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.unlock_worker_contact_desc, application.workerName),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                )
                
                // Price card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.unlock_price),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                        )
                        Text(
                            text = "₹$unlockPrice",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        )
                    }
                }
                
                // Benefits
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UnlockBenefitItem(stringResource(R.string.benefit_phone_number))
                    UnlockBenefitItem(stringResource(R.string.benefit_direct_communication))
                    UnlockBenefitItem(stringResource(R.string.benefit_faster_hiring))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPayment,
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.processing))
                } else {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pay_and_unlock, unlockPrice))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text(stringResource(R.string.cancel), color = Color(0xFF6B7280))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground
    )
}

@Composable
private fun UnlockBenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF374151))
        )
    }
}
