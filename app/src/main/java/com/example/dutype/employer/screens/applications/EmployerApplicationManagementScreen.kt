package com.example.dutype.employer.screens.applications

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import com.dutype.app.R
import timber.log.Timber
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.viewmodels.EmployerJobsViewModel
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.utils.findActivity
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
    onBackClick: () -> Unit = {},
    onSubscribeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: EmployerApplicationViewModel = hiltViewModel()
    val jobViewModel: EmployerJobsViewModel = hiltViewModel()
    val reviewTriggerService = rememberInAppReviewTriggerService()
    val scope = rememberCoroutineScope()
    val ratingService = remember { com.example.dutype.di.ratingServiceFromHilt(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val matchedWorkersState by viewModel.matchedWorkersState.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showStatusFilter by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // FINTECH: Contact Unlock Dialog State
    var showUnlockDialog by remember { mutableStateOf(false) }
    var pendingUnlockApplication by remember { mutableStateOf<JobApplication?>(null) }
    var pendingRatingApplication by remember { mutableStateOf<JobApplication?>(null) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var ratedApplicationIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var reportSummary by remember(jobId) { mutableStateOf<JobReportSummary?>(null) }
    var isReportSummaryLoading by remember(jobId) { mutableStateOf(false) }
    var currentJob by remember(jobId) { mutableStateOf<JobListing?>(null) }
    var isJobClosedOverride by remember(jobId) { mutableStateOf(false) }
    var showCloseJobDialog by remember { mutableStateOf(false) }
    var pendingCloseJobId by remember { mutableStateOf<String?>(null) }
    var isClosingJob by remember { mutableStateOf(false) }

    val toggleJobCalls: (String, Boolean) -> Unit = { targetJobId, pauseCalls ->
        val done: (Boolean, String?) -> Unit = { ok, error ->
            if (ok) {
                isJobClosedOverride = pauseCalls
                currentJob = currentJob?.copy(status = if (pauseCalls) "filled" else "open")
                Toast.makeText(
                    context,
                    if (pauseCalls) context.getString(R.string.calls_stopped_job_filled) else context.getString(R.string.job_reopened_accepting_calls),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(context, error ?: context.getString(R.string.failed_update_call_status), Toast.LENGTH_SHORT).show()
            }
        }
        if (pauseCalls) jobViewModel.markFilled(targetJobId, done) else jobViewModel.reopenJob(targetJobId, done)
    }

    val isJobFilled = remember(currentJob, isJobClosedOverride) {
        isJobClosedOverride ||
        currentJob?.status?.equals("filled", ignoreCase = true) == true ||
        currentJob?.status?.equals("closed", ignoreCase = true) == true
    }

    val hiredApplications = remember(uiState.applications) {
        uiState.applications.filter {
            it.status == ApplicationStatus.HIRED ||
            it.status == ApplicationStatus.COMPLETED
        }
    }

    LaunchedEffect(jobId) {
        if (jobId == null) {
            currentJob = null
            isJobClosedOverride = false
            viewModel.loadEmployerApplications()
        } else {
            viewModel.loadJobApplications(jobId)
            jobViewModel.getJob(jobId) { job ->
                currentJob = job
                isJobClosedOverride = job != null && !job.isOpen()
            }
        }
    }

    LaunchedEffect(jobId, isJobFilled, currentJob) {
        if (jobId != null && currentJob != null && !isJobFilled) {
            viewModel.loadMatchedWorkers(jobId)
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
        NeedSubscriptionDialog(
            onDismiss = {
                showUnlockDialog = false
                pendingUnlockApplication = null
            },
            onSubscribeClick = {
                showUnlockDialog = false
                pendingUnlockApplication = null
                onSubscribeClick()
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
                                    context.findActivity()?.let { act -> reviewTriggerService.onEmployerVerifiedWork(act) }
                                    ratedApplicationIds = ratedApplicationIds + application.id
                                    showRatingSheet = false
                                    pendingRatingApplication = null
                                }
                            },
                            onFailure = { error ->
                                Toast.makeText(context, error.message ?: context.getString(R.string.failed_to_submit_rating), Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        )
    }

    val effectiveCloseJobId = pendingCloseJobId ?: jobId
    if (showCloseJobDialog && effectiveCloseJobId != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isClosingJob) {
                    showCloseJobDialog = false
                    pendingCloseJobId = null
                }
            },
            title = { Text(stringResource(R.string.vacancies_filled_title)) },
            text = {
                Text(stringResource(R.string.vacancies_filled_message))
            },
            confirmButton = {
                TextButton(
                    enabled = !isClosingJob,
                    onClick = {
                        isClosingJob = true
                        jobViewModel.markFilled(effectiveCloseJobId) { success, error ->
                            isClosingJob = false
                            if (success) {
                                showCloseJobDialog = false
                                pendingCloseJobId = null
                                isJobClosedOverride = true
                                currentJob = currentJob?.copy(status = "filled")
                                Toast.makeText(context, context.getString(R.string.job_closed_as_filled), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, error ?: context.getString(R.string.unable_to_close_job), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text(if (isClosingJob) stringResource(R.string.closing_ellipsis) else stringResource(R.string.mark_as_filled))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isClosingJob,
                    onClick = {
                        showCloseJobDialog = false
                        pendingCloseJobId = null
                    }
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
        uiState.applications.count { it.status == ApplicationStatus.APPLIED }
    }
    val hiredCount = remember(uiState.applications) {
        uiState.applications.count { it.status in filledApplicationStatuses }
    }

    val applicantText = if (applicantCountForSummary == 1) stringResource(R.string.applicant) else stringResource(R.string.applicants)
    val headerStrip = if (jobId != null) {
        "$jobTitleForActions · $applicantCountForSummary $applicantText"
    } else {
        "$applicantCountForSummary $applicantText"
    }
    val extras: @Composable () -> Unit = {
        HiringRoomExtras(
            reportSummary = reportSummary,
            isReportSummaryLoading = isReportSummaryLoading,
            jobTitle = jobTitleForActions,
            isJobLive = isJobLive,
            applicantsCount = applicantCountForSummary,
            matchedWorkersCount = matchedWorkersState.workers.size,
            callReadyCandidates = callReadyCandidates,
            isClosingJob = isClosingJob,
            isJobFilled = isJobFilled,
            onCloseJob = { showCloseJobDialog = true },
            onToggleCalls = { pause -> if (jobId != null) toggleJobCalls(jobId, pause) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HrBackground.bg())
    ) {
        HiringRoomHeader(
            title = if (jobId != null) stringResource(R.string.hiring_room) else stringResource(R.string.all_applications),
            subtitle = headerStrip,
            onBackClick = onBackClick
        )

        if (jobId != null) {
            extras()

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground,
                contentColor = EmployerColors.TextPrimary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        viewModel.loadMatchedWorkers(jobId, force = true)
                    },
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
                isContactUnlocked = { workerId -> viewModel.isContactUnlocked(workerId) },
                onUnlockContact = { worker ->
                    viewModel.fetchPhoneNumberForWorker(
                        jobId = jobId,
                        workerId = worker.workerId,
                        onSuccess = { },
                        onFailure = {
                            Toast.makeText(context, context.getString(R.string.phone_number_not_available), Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onCallWorker = { worker ->
                    val phone = worker.phone
                    if (phone.isNotBlank()) {
                        dialWorkerPhone(context, phone)
                    } else {
                        Toast.makeText(context, context.getString(R.string.fetching_phone_number), Toast.LENGTH_SHORT).show()
                        viewModel.fetchPhoneNumberForWorker(
                            jobId = jobId,
                            workerId = worker.workerId,
                            onSuccess = { fetchedPhone ->
                                if (fetchedPhone.isNotBlank()) {
                                    dialWorkerPhone(context, fetchedPhone)
                                } else {
                                    Toast.makeText(context, context.getString(R.string.phone_number_not_available), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onFailure = {
                                Toast.makeText(context, context.getString(R.string.failed_fetch_phone_number), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onChatWorker = { worker ->
                    val phone = worker.phone
                    val jobTitleMsg = jobTitleForActions.ifBlank { "DutyPe" }
                    if (phone.isNotBlank()) {
                        openWorkerWhatsApp(context, phone, "Hello ${worker.fullName}, I have a job opening on DutyPe: $jobTitleMsg.")
                    } else {
                        viewModel.fetchPhoneNumberForWorker(
                            jobId = jobId,
                            workerId = worker.workerId,
                            onSuccess = { fetchedPhone ->
                                if (fetchedPhone.isNotBlank()) {
                                    openWorkerWhatsApp(context, fetchedPhone, "Hello ${worker.fullName}, I have a job opening on DutyPe: $jobTitleMsg.")
                                } else {
                                    Toast.makeText(context, context.getString(R.string.phone_number_not_available), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onFailure = {
                                Toast.makeText(context, context.getString(R.string.failed_fetch_phone_number), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                leadingContent = null,
                modifier = Modifier.weight(1f)
            )
        } else {
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

            HiringApplicantsSection(
                modifier = Modifier.weight(1f),
                isLoading = uiState.isLoading,
                loadError = if (uiState.hasError && uiState.allApplications.isEmpty()) uiState.error ?: "" else null,
                onRetry = { viewModel.retry() },
                allApplications = uiState.applications,
                visibleApplications = visibleApplications,
                isJobSpecific = jobId != null,
                callReadyCandidates = callReadyCandidates,
                isJobLive = isJobLive,
                ratedApplicationIds = ratedApplicationIds,
                employerShopAddress = currentJob?.addressText.orEmpty(),
                leadingContent = if (jobId != null) {
                    {
                        val openApplicants = uiState.allApplications.filter { it.status == ApplicationStatus.APPLIED }
                        if (isJobLive && openApplicants.size >= 2) {
                            com.example.dutype.employer.ai.AiTopPicksCard(
                                jobId = jobId,
                                applicantIds = openApplicants.map { it.id },
                                onCall = { appId ->
                                    uiState.allApplications.firstOrNull { it.id == appId }?.let { app ->
                                        viewModel.unlockContact(
                                            application = app,
                                            onSuccess = { phone ->
                                                if (phone.isNotBlank()) context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                            },
                                            onFailure = {
                                                Toast.makeText(context, context.getString(R.string.phone_number_not_available), Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                onHire = { appId ->
                                    uiState.allApplications.firstOrNull { it.id == appId }?.let { app ->
                                        viewModel.updateApplicationStatus(app.id, ApplicationStatus.HIRED, "Hired from AI top picks")
                                        context.findActivity()?.let { act -> reviewTriggerService.onEmployerHiredWorker(act) }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                } else null,
                onCloseJob = { showCloseJobDialog = true },
                onOpen = { application ->
                    onApplicationClick(application)
                },
                onUnlock = { application ->
                    viewModel.unlockContact(
                        application = application,
                        onSuccess = { _ ->
                            Toast.makeText(context, context.getString(R.string.contact_unlocked), Toast.LENGTH_SHORT).show()
                            val activity = context as? Activity
                            if (activity != null) {
                                reviewTriggerService.onEmployerContactUnlocked(activity)
                            }
                        },
                        onFailure = {
                            Toast.makeText(context, context.getString(R.string.phone_number_not_available), Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onStatusUpdate = { application, newStatus, notes ->
                    viewModel.updateApplicationStatus(
                        applicationId = application.id,
                        newStatus = newStatus,
                        notes = notes
                    )
                    if (newStatus == ApplicationStatus.HIRED) {
                        context.findActivity()?.let { act -> reviewTriggerService.onEmployerHiredWorker(act) }
                        val targetJobId = jobId ?: application.jobId
                        if (targetJobId.isNotBlank()) {
                            viewModel.canHireMoreApplicants(currentJob?.vacancies ?: 1) { canAccept, remaining ->
                                if (!canAccept || remaining <= 0) {
                                    pendingCloseJobId = targetJobId
                                    showCloseJobDialog = true
                                }
                            }
                        }
                    }
                },
                onHirePrompt = { app ->
                    viewModel.updateApplicationStatus(app.id, ApplicationStatus.HIRED, "Hired directly")
                    context.findActivity()?.let { act -> reviewTriggerService.onEmployerHiredWorker(act) }
                },
                onRate = { application ->
                    scope.launch {
                        if (application.status == ApplicationStatus.HIRED) {
                            val completed = viewModel.updateApplicationStatusForResult(
                                applicationId = application.id,
                                newStatus = ApplicationStatus.COMPLETED
                            )
                            if (completed.isFailure) {
                                Toast.makeText(
                                    context,
                                    completed.exceptionOrNull()?.message ?: context.getString(R.string.failed_mark_work_done),
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }
                            context.findActivity()?.let { act -> reviewTriggerService.onEmployerVerifiedWork(act) }
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

@Composable
private fun MatchedWorkersContent(
    state: com.example.dutype.viewmodels.MatchedWorkersUiState,
    isJobLive: Boolean,
    onRefresh: () -> Unit,
    isContactUnlocked: (String) -> Boolean,
    onUnlockContact: (MatchedWorker) -> Unit,
    onCallWorker: (MatchedWorker) -> Unit,
    onChatWorker: ((MatchedWorker) -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            Column(modifier = modifier.fillMaxSize()) {
                if (leadingContent != null) leadingContent()
                com.example.dutype.components.DutyPeLoadingList(modifier = Modifier.weight(1f))
            }
        }
        state.hasError -> {
            Column(modifier = modifier.fillMaxSize()) {
                if (leadingContent != null) leadingContent()
                com.example.dutype.components.DutyPeErrorState(message = state.error, onRetry = onRefresh, modifier = Modifier.weight(1f))
            }
        }
        state.workers.isEmpty() -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (leadingContent != null) {
                    item(key = "matched_leading_empty") { leadingContent() }
                }
                item(key = "matched_empty_view") {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isJobLive) Icons.Default.Search else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmployerColors.TextSecondary,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = if (isJobLive) stringResource(R.string.no_strong_worker_matches) else stringResource(R.string.job_is_filled),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isJobLive) {
                                    stringResource(R.string.matches_improve_hint)
                                } else {
                                    stringResource(R.string.accepted_workers_hint)
                                },
                                style = AppTypography.bodySmall.copy(color = EmployerColors.TextSecondary),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        else -> {
            val rankedWorkers = remember(state.workers) {
                state.workers.sortedMatchedWorkersForConnectNow()
            }
            // A filled job shows no new matches.
            val visibleWorkers = if (isJobLive) rankedWorkers else emptyList()

            if (visibleWorkers.isEmpty()) {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (leadingContent != null) {
                        item(key = "matched_leading_filled") { leadingContent() }
                    }
                    item(key = "matched_filled_view") {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmployerColors.TextSecondary,
                                    modifier = Modifier.size(42.dp)
                                )
                                Text(
                                    text = stringResource(R.string.job_is_filled),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = stringResource(R.string.auto_new_matches_are_hidden_accepted_workers_wi),
                                    style = AppTypography.bodySmall.copy(color = EmployerColors.TextSecondary),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (leadingContent != null) {
                        item(key = "matched_leading_content") { leadingContent() }
                    }
                    item(key = "matched_header_text") {
                        Text(
                            text = if (isJobLive) stringResource(R.string.best_workers_for_job) else stringResource(R.string.filled_job_matches),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmployerColors.TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isJobLive) {
                                stringResource(R.string.nearest_workers_sort_hint)
                            } else {
                                stringResource(R.string.selected_workers_stay_active_hint)
                            },
                            style = AppTypography.bodySmall.copy(color = EmployerColors.TextSecondary)
                        )
                        if (!state.actionError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.actionError,
                                style = AppTypography.bodySmall.copy(color = Color(0xFFB91C1C).fg())
                            )
                        }
                    }

                    itemsIndexed(visibleWorkers, key = { index, it -> "${it.workerId.ifBlank { "worker" }}_$index" }) { _, worker ->
                        MatchedWorkerCard(
                            worker = worker,
                            isJobLive = isJobLive,
                            isDisabledForFilledJob = !isJobLive,
                            isContactUnlocked = isContactUnlocked(worker.workerId),
                            onUnlockContact = { onUnlockContact(worker) },
                            onCallWorker = { onCallWorker(worker) },
                            onChatWorker = onChatWorker?.let { cb -> { cb(worker) } }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, EmployerColors.Border)
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
                    color = if (isLive) EmployerColors.SuccessLight else EmployerColors.Border
                ) {
                    Text(
                        text = if (isLive) stringResource(R.string.live) else stringResource(R.string.filled),
                        style = AppTypography.caption.copy(
                            color = if (isLive) Color(0xFF047857).fg() else EmployerColors.TextSecondary,
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
                        color = EmployerColors.Primary.fg(),
                        modifier = Modifier.weight(1f)
                    )
                    HiringRoomMetricItem(
                        value = matchedWorkersCount.toString(),
                        label = stringResource(R.string.nearby_matches),
                        icon = Icons.Default.Work,
                        color = Color(0xFF7C3AED).fg(),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    HiringRoomMetricItem(
                        value = callReadyCandidates.toString(),
                        label = stringResource(R.string.call_ready),
                        icon = Icons.Default.Call,
                        color = EmployerColors.Success,
                        modifier = Modifier.weight(1f)
                    )
                    HiringRoomMetricItem(
                        value = if (isLive) stringResource(R.string.open_status) else stringResource(R.string.done_status),
                        label = stringResource(R.string.hiring_status),
                        icon = Icons.Default.CheckCircle,
                        color = if (isLive) EmployerColors.Warning else EmployerColors.TextSecondary,
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
                    colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
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
                style = AppTypography.caption.copy(color = EmployerColors.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
            .background(EmployerColors.WarningLight, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.best_first_for_applicants, totalApplications),
            style = AppTypography.labelLarge.copy(color = EmployerColors.Warning, fontWeight = FontWeight.Bold)
        )
        Text(
            text = stringResource(R.string.call_ready_lifted_first, callReadyCandidates),
            style = AppTypography.bodySmall.copy(color = EmployerColors.Warning)
        )
        if (isJobLive) {
            OutlinedButton(
                onClick = onCloseJob,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmployerColors.Warning)
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
    isContactUnlocked: Boolean,
    onUnlockContact: () -> Unit,
    onCallWorker: (MatchedWorker) -> Unit,
    onChatWorker: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val canCall = (worker.phone.isNotBlank() || isContactUnlocked) && !isDisabledForFilledJob
    val workerStatusText = when {
        isDisabledForFilledJob -> stringResource(R.string.job_is_filled)
        worker.isAvailable -> stringResource(R.string.available_now)
        else -> formatWorkerDistance(worker.distanceKm)
    }
    val contentAlpha = if (isDisabledForFilledJob) 0.58f else 1f
    val cardContainerColor = if (isDisabledForFilledJob) {
        EmployerColors.ChipBackground
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
                        .background(if (isDisabledForFilledJob) EmployerColors.Border else EmployerColors.InfoLight, CircleShape),
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
                            tint = EmployerColors.Info,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = worker.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmployerColors.TextPrimary.copy(alpha = contentAlpha)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = workerStatusText,
                        style = AppTypography.caption.copy(color = EmployerColors.TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

            }

            MatchedWorkerMetricsGrid(worker = worker)

            if (isDisabledForFilledJob) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = EmployerColors.Border
                ) {
                    Text(
                        text = stringResource(R.string.disabled_job_filled),
                        style = AppTypography.bodySmall.copy(color = EmployerColors.TextSecondary),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (worker.skills.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(worker.skills.take(5)) { skill ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = EmployerColors.ChipBackground
                        ) {
                            Text(
                                text = skill.replaceFirstChar { it.titlecase(Locale.ROOT) },
                                style = AppTypography.caption.copy(color = EmployerColors.TextSecondary),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            if (worker.matchReasons.isNotEmpty()) {
                Text(
                    text = worker.matchReasons.joinToString(" • "),
                    style = AppTypography.bodySmall.copy(color = EmployerColors.TextSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isContactUnlocked) {
                    Button(
                        onClick = onUnlockContact,
                        enabled = !isDisabledForFilledJob,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.unlock))
                    }
                } else {
                    Button(
                        onClick = { onCallWorker(worker) },
                        enabled = canCall && !isDisabledForFilledJob,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Success)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.call))
                    }
                    if (onChatWorker != null) {
                        OutlinedButton(
                            onClick = onChatWorker,
                            enabled = canCall && !isDisabledForFilledJob,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF25D366))
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF25D366))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.chat), color = Color(0xFF25D366), fontWeight = FontWeight.SemiBold)
                        }
                    }
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
            label = stringResource(R.string.away_label),
            value = formatWorkerDistanceShort(worker.distanceKm),
            color = EmployerColors.Primary.fg(),
            modifier = Modifier.weight(1f)
        )
        WorkerMetricBlock(
            icon = Icons.Default.CheckCircle,
            label = stringResource(R.string.jobs_done_label),
            value = worker.completedJobs.toString(),
            color = EmployerColors.Success,
            modifier = Modifier.weight(1f)
        )
        WorkerMetricBlock(
            icon = Icons.Default.Star,
            label = formatWorkerRatingCount(worker.ratingCount),
            value = formatWorkerRatingValue(worker.rating),
            color = EmployerColors.Warning,
            modifier = Modifier.weight(1f)
        )
        WorkerMetricBlock(
            icon = Icons.Default.FlashOn,
            label = stringResource(R.string.status_label),
            value = formatMatchedWorkerStatus(worker),
            color = Color(0xFF7C3AED).fg(),
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
            style = AppTypography.labelLarge.copy(color = EmployerColors.TextPrimary, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = AppTypography.caption.copy(color = EmployerColors.TextSecondary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2).bg()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_community_reports),
                style = AppTypography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmployerColors.Error
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
                        color = Color(0xFFB91C1C).fg()
                    )
                    Text(
                        text = stringResource(R.string.auto_loading_report_details),
                        style = AppTypography.caption.copy(color = Color(0xFF7F1D1D).fg())
                    )
                }
            } else if (summary != null) {
                Text(
                    text = stringResource(R.string.reported_by_workers_format, summary.reportCount),
                    style = AppTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7F1D1D).fg()
                    )
                )

                if (summary.typeCounts.isNotEmpty()) {
                    summary.typeCounts.entries
                        .sortedByDescending { it.value }
                        .forEach { entry ->
                            Text(
                                text = "${entry.key}: ${entry.value}",
                                style = AppTypography.caption.copy(color = Color(0xFF7F1D1D).fg())
                            )
                        }
                }

                if (summary.recentReports.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFFFECACA).bd(), thickness = 1.dp)
                    summary.recentReports.take(3).forEach { report ->
                        Text(
                            text = "${report.reportType}: ${report.description}",
                            style = AppTypography.bodySmall.copy(color = Color(0xFF7F1D1D).fg())
                        )
                    }
                }
            }
        }
    }
}

// NOTE: getStatusColor removed - use ApplicationStatus.getStatusColor() extension function
// Import: import com.example.dutype.models.getStatusColor

// ==================== HIRING ROOM (flat design) ====================

private val HrBackground = Color(0xFFF8FAFC)
private val HrInk = Color(0xFF0F172A)
private val HrName = Color(0xFF0F0F0F)
private val HrMuted = Color(0xFF64748B)
private val HrFaint = Color(0xFF94A3B8)
private val HrBorder = Color(0xFFE2E8F0)
private val HrDivider = Color(0xFFF1F5F9)
private val HrGreen = Color(0xFF10B981)
private val HrRed = Color(0xFFDC2626)
private val HrAvatarColors = listOf(Color(0xFFDBEAFE), Color(0xFFBBF7D0), Color(0xFFFDE68A))
private val HrShortlistedStatuses = setOf(
    ApplicationStatus.HIRED,
    ApplicationStatus.COMPLETED
)

private enum class HiringFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    NEW(R.string.filter_new),
    ACCEPTED(R.string.status_hired),
    CONTACTED(R.string.filter_contacted),
    REJECTED(R.string.filter_rejected)
}

private fun JobApplication.matchesHiringFilter(filter: HiringFilter, contacted: Set<String>): Boolean {
    return when (filter) {
        HiringFilter.ALL -> true
        HiringFilter.NEW -> status == ApplicationStatus.APPLIED
        HiringFilter.ACCEPTED -> status in HrShortlistedStatuses
        HiringFilter.CONTACTED -> id in contacted
        HiringFilter.REJECTED -> status == ApplicationStatus.REJECTED
    }
}

@Composable
private fun hiringSubtitle(application: JobApplication): String {
    val years = application.worker?.experienceYears ?: 0
    val expText = if (years > 0) stringResource(R.string.years_exp_format, years) else ""
    return listOf(application.jobTitle.trim(), expText)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
}

private fun hiringInitials(name: String): String {
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
    return initials.ifEmpty { "?" }
}

private fun dialWorkerPhone(context: Context, phone: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.unable_to_open_dialer), Toast.LENGTH_SHORT).show()
    }
}

private fun openWorkerWhatsApp(context: Context, phone: String, message: String) {
    val rawDigits = phone.filter { it.isDigit() }
    val formattedPhone = if (rawDigits.length == 10) "91$rawDigits" else rawDigits
    val waUri = Uri.parse("https://wa.me/$formattedPhone?text=${Uri.encode(message)}")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, waUri))
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun HiringRoomHeader(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = HrInk.fg(),
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBackClick)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = HrInk.fg()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = HrMuted.fg(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HiringRoomExtras(
    reportSummary: JobReportSummary?,
    isReportSummaryLoading: Boolean,
    jobTitle: String,
    isJobLive: Boolean,
    applicantsCount: Int,
    matchedWorkersCount: Int,
    callReadyCandidates: Int,
    isClosingJob: Boolean,
    isJobFilled: Boolean,
    onCloseJob: () -> Unit,
    onToggleCalls: (Boolean) -> Unit
) {
    Column {
        if (isReportSummaryLoading || reportSummary != null) {
            JobReportSummaryCard(
                summary = reportSummary,
                isLoading = isReportSummaryLoading
            )
        }
        HiringRoomSummaryCard(
            jobTitle = jobTitle,
            isLive = isJobLive,
            applicantsCount = applicantsCount,
            matchedWorkersCount = matchedWorkersCount,
            callReadyCandidates = callReadyCandidates,
            isClosingJob = isClosingJob,
            onCloseJob = onCloseJob
        )
        IncomingCallsStatusBanner(
            isCallsPaused = isJobFilled,
            onToggleCalls = onToggleCalls
        )
    }
}

@Composable
private fun HiringFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) HrInk.bg() else Color.White.bg())
            .border(1.dp, if (selected) HrInk.bd() else HrBorder.bd(), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else HrInk.fg(),
            maxLines = 1
        )
    }
}

@Composable
private fun HiringFilterRow(
    active: HiringFilter,
    counts: Map<HiringFilter, Int>,
    onSelect: (HiringFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HiringFilter.values().toList()) { filter ->
            val count = counts[filter] ?: 0
            val filterName = stringResource(filter.labelRes)
            val label = if (filter == HiringFilter.REJECTED && count == 0) {
                filterName
            } else {
                "$filterName ($count)"
            }
            HiringFilterChip(
                label = label,
                selected = filter == active,
                onClick = { onSelect(filter) }
            )
        }
    }
}

@Composable
private fun HiringApplicantsSection(
    modifier: Modifier,
    isLoading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    allApplications: List<JobApplication>,
    visibleApplications: List<JobApplication>,
    isJobSpecific: Boolean,
    callReadyCandidates: Int,
    isJobLive: Boolean,
    ratedApplicationIds: Set<String>,
    employerShopAddress: String,
    leadingContent: (@Composable () -> Unit)?,
    onCloseJob: () -> Unit,
    onOpen: (JobApplication) -> Unit,
    onUnlock: (JobApplication) -> Unit,
    onStatusUpdate: (JobApplication, ApplicationStatus, String?) -> Unit,
    onHirePrompt: (JobApplication) -> Unit,
    onRate: (JobApplication) -> Unit
) {
    var activeFilter by remember { mutableStateOf(HiringFilter.ALL) }
    var contactedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val contacted = contactedIds
    val filter = activeFilter
    val counts = remember(visibleApplications, contacted) {
        HiringFilter.values().associateWith { f ->
            visibleApplications.count { it.matchesHiringFilter(f, contacted) }
        }
    }
    val displayed = remember(visibleApplications, filter, contacted) {
        visibleApplications.filter { it.matchesHiringFilter(filter, contacted) }
    }
    val showList = !isLoading && allApplications.isNotEmpty() && displayed.isNotEmpty()
    val online by com.example.dutype.components.rememberOnline()

    Column(modifier = modifier) {
        // Applicants already on screen stay visible offline, with a note that they are saved data.
        if (showList) com.example.dutype.components.OfflineCachedNote()
        HiringFilterRow(
            active = filter,
            counts = counts,
            onSelect = { activeFilter = it }
        )
        when {
            loadError != null -> {
                Box(modifier = Modifier.weight(1f)) {
                    com.example.dutype.components.DutyPeErrorState(message = loadError, onRetry = onRetry)
                }
            }
            isLoading -> {
                com.example.dutype.components.DutyPeLoadingList(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp)
                )
            }
            // An empty list while offline is "no internet", not "no applicants yet".
            allApplications.isEmpty() && !online -> {
                Box(modifier = Modifier.weight(1f)) {
                    com.example.dutype.components.DutyPeIssueState(com.example.dutype.components.LoadIssue.OFFLINE, onRetry = onRetry)
                }
            }
            allApplications.isEmpty() -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (leadingContent != null) {
                        item(key = "hiring_applicants_leading_empty") { leadingContent() }
                    }
                    item(key = "hiring_applicants_empty_state") {
                        EmptyApplicationsState(isJobSpecific = isJobSpecific)
                    }
                }
            }
            displayed.isEmpty() -> {
                FilterEmptyState(
                    filter = filter,
                    onClearFilter = { activeFilter = HiringFilter.ALL },
                    modifier = Modifier.weight(1f)
                )
            }
            else -> {
                HiringApplicantsList(
                    modifier = Modifier.weight(1f),
                    applications = displayed,
                    totalCount = allApplications.size,
                    showManyBanner = isJobSpecific && filter == HiringFilter.ALL,
                    callReadyCandidates = callReadyCandidates,
                    isJobLive = isJobLive,
                    ratedApplicationIds = ratedApplicationIds,
                    employerShopAddress = employerShopAddress,
                    leadingContent = if (filter == HiringFilter.ALL) leadingContent else null,
                    onCloseJob = onCloseJob,
                    onOpen = onOpen,
                    onUnlock = onUnlock,
                    onContacted = { id -> contactedIds = contactedIds + id },
                    onStatusUpdate = onStatusUpdate,
                    onHirePrompt = onHirePrompt,
                    onRate = onRate
                )
            }
        }
    }
}

@Composable
private fun FilterEmptyState(
    filter: HiringFilter,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filterName = stringResource(filter.labelRes)
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = EmployerColors.TextSecondary,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = "No $filterName candidates",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = when (filter) {
                    HiringFilter.NEW -> "All applicants for this job have been reviewed."
                    HiringFilter.ACCEPTED -> "You haven't accepted any candidates yet. Tap 'Accept' on candidate cards in 'All' to hire them."
                    HiringFilter.CONTACTED -> "You haven't contacted any candidates yet. Call or chat with applicants to keep track."
                    HiringFilter.REJECTED -> "No candidates have been marked as rejected."
                    else -> "No candidates match this filter."
                },
                style = AppTypography.bodySmall.copy(color = EmployerColors.TextSecondary),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onClearFilter,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.filter_all))
            }
        }
    }
}

@Composable
private fun HiringApplicantsList(
    modifier: Modifier,
    applications: List<JobApplication>,
    totalCount: Int,
    showManyBanner: Boolean,
    callReadyCandidates: Int,
    isJobLive: Boolean,
    ratedApplicationIds: Set<String>,
    employerShopAddress: String,
    leadingContent: (@Composable () -> Unit)?,
    onCloseJob: () -> Unit,
    onOpen: (JobApplication) -> Unit,
    onUnlock: (JobApplication) -> Unit,
    onContacted: (String) -> Unit,
    onStatusUpdate: (JobApplication, ApplicationStatus, String?) -> Unit,
    onHirePrompt: (JobApplication) -> Unit,
    onRate: (JobApplication) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingContent != null) {
            item { leadingContent() }
        }
        if (showManyBanner && totalCount >= MANY_APPLICANTS_THRESHOLD) {
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    TooManyApplicationsBanner(
                        totalApplications = totalCount,
                        callReadyCandidates = callReadyCandidates,
                        isJobLive = isJobLive,
                        onCloseJob = onCloseJob
                    )
                }
            }
        }
        items(applications) { application ->
            HiringApplicantCard(
                application = application,
                hasAlreadyRated = application.id in ratedApplicationIds,
                employerShopAddress = employerShopAddress,
                modifier = Modifier.padding(horizontal = 20.dp),
                onClick = { onOpen(application) },
                onUnlockContact = { onUnlock(application) },
                onContacted = { onContacted(application.id) },
                onStatusUpdate = { status, notes -> onStatusUpdate(application, status, notes) },
                onHirePrompt = onHirePrompt,
                onRate = { onRate(application) }
            )
        }
    }
}

@Composable
private fun HiringAvatar(application: JobApplication) {
    val name = application.workerName
    val bg = HrAvatarColors[(name.hashCode() and 0x7fffffff) % HrAvatarColors.size]
    val imageUrl = application.workerPhoto
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            com.example.dutype.components.OptimizedProfileImage(
                imageUrl = imageUrl,
                contentDescription = "Worker Profile",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = hiringInitials(name),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = HrInk.fg()
            )
        }
    }
}

@Composable
private fun HiringCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(1.dp, HrBorder.bd(), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = HrInk.fg(),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HiringCardTopRow(
    application: JobApplication,
    displayName: String,
    onCall: () -> Unit,
    onChat: () -> Unit
) {
    val subtitle = hiringSubtitle(application)
    val distance = application.worker?.distanceKm
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        HiringAvatar(application)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = HrName.fg(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = HrMuted.fg(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (distance != null && distance > 0.0) {
                Text(
                    text = stringResource(R.string.km_away_format, distance),
                    fontSize = 11.sp,
                    color = HrFaint.fg(),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HiringCircleButton(Icons.Outlined.Call, stringResource(R.string.call), onCall)
                HiringCircleButton(Icons.Outlined.ChatBubbleOutline, stringResource(R.string.chat), onChat)
            }
        }
    }
}

@Composable
private fun HiringSkillChip(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(shape)
            .border(1.dp, HrBorder.bd(), shape)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = HrInk.fg(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HiringActionChip(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .background(if (active) activeColor else Color.White.bg())
            .border(1.dp, if (active) activeColor else HrBorder.bd(), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) Color.White else HrInk.fg(),
            maxLines = 1
        )
    }
}

@Composable
private fun HiringRateSection(
    status: ApplicationStatus,
    hasAlreadyRated: Boolean,
    onRate: () -> Unit
) {
    val canMarkWorkDone = status == ApplicationStatus.HIRED
    OutlinedButton(
        onClick = onRate,
        enabled = canMarkWorkDone || !hasAlreadyRated,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, HrBorder.bd()),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = HrInk.fg(),
            disabledContentColor = HrGreen.fg()
        ),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        Text(
            text = if (canMarkWorkDone) stringResource(R.string.mark_work_done) else if (hasAlreadyRated) stringResource(R.string.rated_worker) else stringResource(R.string.rate_worker),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HiringApplicantCard(
    application: JobApplication,
    hasAlreadyRated: Boolean,
    employerShopAddress: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onUnlockContact: () -> Unit,
    onContacted: () -> Unit,
    onStatusUpdate: (ApplicationStatus, String?) -> Unit,
    onHirePrompt: (JobApplication) -> Unit,
    onRate: () -> Unit
) {
    val context = LocalContext.current
    val phone = application.workerPhone.trim()
    val displayName = application.workerName.ifBlank { stringResource(R.string.unknown_worker) }
    val defaultWaMsg = stringResource(R.string.whatsapp_msg_application, displayName)
    val interviewMsg = if (employerShopAddress.isNotBlank()) {
        stringResource(R.string.whatsapp_msg_interview_address, displayName, employerShopAddress)
    } else {
        stringResource(R.string.whatsapp_msg_interview_simple, displayName)
    }
    val notSuitableReason = stringResource(R.string.reason_not_suitable)
    val status = application.status
    val isShortlisted = status in HrShortlistedStatuses
    val isRejected = status == ApplicationStatus.REJECTED
    val skills = listOf(application.workerSkill).filter { it.isNotBlank() }
        .map { com.example.dutype.employer.models.JobCategory.fromKey(it).displayName }
    val cardShape = RoundedCornerShape(16.dp)

    val onCall: () -> Unit = {
        if (phone.isNotBlank()) {
            dialWorkerPhone(context, phone)
            onContacted()
        } else {
            onUnlockContact()
        }
    }
    val onChat: () -> Unit = {
        if (phone.isNotBlank()) {
            openWorkerWhatsApp(context, phone, defaultWaMsg)
            onContacted()
        } else {
            onUnlockContact()
        }
    }
    val onInterview: () -> Unit = {
        if (phone.isNotBlank()) {
            openWorkerWhatsApp(context, phone, interviewMsg)
            onContacted()
        } else {
            onUnlockContact()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White.bg())
            .border(1.dp, HrBorder.bd(), cardShape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        HiringCardTopRow(
            application = application,
            displayName = displayName,
            onCall = onCall,
            onChat = onChat
        )

        if (skills.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                skills.forEach { skill ->
                    HiringSkillChip(text = skill, modifier = Modifier.weight(1f, fill = false))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp, color = HrDivider.bd())
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HiringActionChip(
                label = if (isShortlisted) stringResource(R.string.status_hired) else stringResource(R.string.accept),
                active = isShortlisted,
                activeColor = HrGreen.fg(),
                onClick = {
                    if (status == ApplicationStatus.APPLIED) {
                        onStatusUpdate(ApplicationStatus.HIRED, "Hired directly")
                    }
                },
                modifier = Modifier.weight(1f)
            )
            HiringActionChip(
                label = stringResource(R.string.action_reject),
                active = isRejected,
                activeColor = HrRed.fg(),
                onClick = {
                    if (status == ApplicationStatus.APPLIED) {
                        onStatusUpdate(ApplicationStatus.REJECTED, notSuitableReason)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (status == ApplicationStatus.HIRED || status == ApplicationStatus.COMPLETED) {
            Spacer(modifier = Modifier.height(12.dp))
            HiringRateSection(
                status = status,
                hasAlreadyRated = hasAlreadyRated,
                onRate = onRate
            )
        }
    }
}

// NOTE: StatusBadge removed - use centralized ApplicationStatusBadge from components instead

@Composable
private fun EmptyApplicationsState(
    isJobSpecific: Boolean
) {
    com.example.dutype.components.DutyPeEmptyScreen(
        icon = Icons.Filled.Groups,
        badge = Icons.Filled.HourglassTop,
        tone = com.example.dutype.components.EmptyTone.GREEN,
        art = com.example.dutype.components.EmptyArt.WAITING,
        title = if (isJobSpecific) stringResource(R.string.waiting_applied_workers) else stringResource(R.string.waiting_worker_responses),
        message = if (isJobSpecific) stringResource(R.string.applied_workers_empty_body) else stringResource(R.string.applications_empty_body),
        tips = listOf(
            stringResource(R.string.empty_applicants_tip_pay),
            stringResource(R.string.empty_applicants_tip_photo),
            stringResource(R.string.empty_applicants_tip_share)
        )
    )
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

@Composable
private fun formatWorkerDistance(distanceKm: Double?): String {
    return when {
        distanceKm == null -> stringResource(R.string.distance_nearby)
        distanceKm < 0.1 -> stringResource(R.string.distance_under_100m)
        distanceKm < 1.0 -> stringResource(R.string.distance_meters_away, (distanceKm * 1000).toInt())
        else -> stringResource(R.string.km_away_format, distanceKm)
    }
}

@Composable
private fun formatWorkerDistanceShort(distanceKm: Double?): String {
    return when {
        distanceKm == null -> stringResource(R.string.distance_nearby)
        distanceKm < 0.1 -> "<100m"
        distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()}m"
        else -> String.format(Locale.ROOT, "%.1fkm", distanceKm)
    }
}

@Composable
private fun formatWorkerRatingValue(rating: Double): String {
    return if (rating > 0.0) String.format(Locale.ROOT, "%.1f", rating) else stringResource(R.string.rating_new)
}

@Composable
private fun formatWorkerRatingCount(ratingCount: Int): String {
    return when (ratingCount) {
        0 -> stringResource(R.string.rating_label_none)
        1 -> stringResource(R.string.rating_label_single)
        else -> stringResource(R.string.rating_label_multiple, ratingCount)
    }
}

@Composable
private fun formatMatchedWorkerStatus(worker: MatchedWorker): String {
    return if (worker.isAvailable) stringResource(R.string.available) else stringResource(R.string.distance_nearby)
}

private fun JobApplication.connectNowScore(): Int {
    var score = 0
    if (status in connectableApplicationStatuses) score += 1_000
    if (workerPhone.isNotBlank()) score += 500
    if (status == ApplicationStatus.APPLIED) score += 120
    if (status == ApplicationStatus.APPLIED) score += 90
    if (status == ApplicationStatus.HIRED) score += 60
    score += profileCompletenessScore()
    return score
}

private fun JobApplication.profileCompletenessScore(): Int {
    var score = 0
    if (workerName.isNotBlank()) score += 40
    if (workerPhoto.isNotBlank()) score += 30
    if (workerSkill.isNotBlank()) score += 40
    worker?.let { card ->
        if (card.experienceYears > 0) score += 25
        if (card.ratingCount > 0) score += (card.rating * 10).toInt()
        score += card.jobsCompleted.coerceAtMost(20) * 2
        card.distanceKm?.let { km -> score += (40 - km * 2).toInt().coerceAtLeast(0) }
    }
    return score
}

private fun JobApplication.isConnectNowCandidate(): Boolean {
    return status in connectableApplicationStatuses && workerPhone.isNotBlank()
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
    ApplicationStatus.APPLIED,
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
        colors = CardDefaults.cardColors(containerColor = EmployerColors.SuccessLight)
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
                tint = EmployerColors.Success,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.free_contacts_remaining, freeRemaining),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EmployerColors.Success,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Dialog prompting the employer to purchase a subscription to view contact details
 */
@Composable
private fun NeedSubscriptionDialog(
    onDismiss: () -> Unit,
    onSubscribeClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(EmployerColors.PrimaryLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = EmployerColors.Primary.fg(),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.auto_subscription_required),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                )
            )
        },
        text = {
            Text(
                text = stringResource(R.string.auto_you_need_an_active_subscription_to_view_wo),
                style = MaterialTheme.typography.bodyLarge,
                color = com.example.dutype.ui.theme.EmployerColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onSubscribeClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmployerColors.Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.auto_view_plans),
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.auto_cancel),
                    color = com.example.dutype.ui.theme.EmployerColors.TextSecondary
                )
            }
        },
        containerColor = Color.White.bg(),
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun JobFilledCandidatesContent(
    hiredApplications: List<JobApplication>,
    onApplicationClick: (JobApplication) -> Unit,
    onUnlockContact: (JobApplication) -> Unit,
    isContactUnlocked: (String, Int) -> Boolean,
    ratedApplicationIds: Set<String>,
    onStatusUpdate: (JobApplication, ApplicationStatus, String?) -> Unit,
    onShowRatingSheet: (JobApplication) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayList = hiredApplications

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5).bg()),
            border = BorderStroke(1.5.dp, Color(0xFFA7F3D0).bd()),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFD1FAE5).bg(), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669).fg(),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.auto_job_position_filled),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46).fg()
                        )
                    )
                    Text(
                        text = if (hiredApplications.isNotEmpty())
                            stringResource(R.string.showing_hired_candidates)
                        else
                            stringResource(R.string.hiring_completed_position),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF047857).fg()
                        )
                    )
                }
            }
        }

        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.auto_no_hired_workers_selected_yet_for_this_fil),
                    style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(displayList) { index, application ->
                    val unlocked = isContactUnlocked(application.workerId, index)
                    SelectedWorkerCard(
                        application = application,
                        applicationIndex = index,
                        isContactUnlocked = unlocked,
                        hasAlreadyRated = application.id in ratedApplicationIds,
                        onClick = { onApplicationClick(application) },
                        onUnlockContact = { onUnlockContact(application) },
                        onStatusUpdate = { status, notes -> onStatusUpdate(application, status, notes) },
                        onRateWorker = { onShowRatingSheet(application) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedWorkerCard(
    application: JobApplication,
    applicationIndex: Int,
    isContactUnlocked: Boolean,
    hasAlreadyRated: Boolean,
    onClick: () -> Unit,
    onUnlockContact: () -> Unit,
    onStatusUpdate: (ApplicationStatus, String?) -> Unit,
    onRateWorker: () -> Unit
) {
    val workerPhone = application.workerPhone.trim()
    val context = LocalContext.current
    val canMarkWorkDone = application.status == ApplicationStatus.HIRED
    val canRateCompletedWork = application.status == ApplicationStatus.COMPLETED
    val canCallWorker = workerPhone.isNotBlank() &&
            application.status != ApplicationStatus.REJECTED &&
            application.status != ApplicationStatus.WITHDRAWN

    val unknownWorkerText = stringResource(R.string.unknown_worker)
    val displayName = application.workerName.ifBlank { unknownWorkerText }
    val initials = if (displayName != unknownWorkerText) {
        displayName.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { displayName.take(1).uppercase() }
    } else "?"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        border = BorderStroke(1.5.dp, EmployerColors.Success),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmployerColors.SuccessLight,
                    border = BorderStroke(1.dp, EmployerColors.Success)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmployerColors.Success,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.auto_selected_worker),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmployerColors.Success,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                ApplicationStatusBadge(status = application.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(EmployerColors.SuccessLight, CircleShape)
                        .border(1.5.dp, EmployerColors.Success, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!application.workerPhoto.isBlank()) {
                        com.example.dutype.components.OptimizedProfileImage(
                            imageUrl = application.workerPhoto,
                            contentDescription = "Worker Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmployerColors.Success
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = AppTypography.cardTitle.copy(
                            color = EmployerColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )


                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = EmployerColors.TextTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.hired_on_date, DateTimeUtils.formatRelativeTime(application.createdAt)),
                            style = AppTypography.caption.copy(color = EmployerColors.TextTertiary, fontSize = 11.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isContactUnlocked) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmployerColors.WarningLight),
                    border = BorderStroke(1.dp, EmployerColors.Warning)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = EmployerColors.Warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.auto_contact_details_are_locked),
                                style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.Warning),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        TextButton(
                            onClick = onUnlockContact,
                            colors = ButtonDefaults.textButtonColors(contentColor = EmployerColors.Warning)
                        ) {
                            Text(stringResource(R.string.unlock), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else if (canCallWorker) {
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
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Success)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.call_hired_worker), style = AppTypography.labelLarge, color = Color.White)
                }
            }

            if (canMarkWorkDone || canRateCompletedWork) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = if (hasAlreadyRated) EmployerColors.SuccessLight else EmployerColors.WarningLight,
                    border = BorderStroke(1.dp, if (hasAlreadyRated) EmployerColors.Success else EmployerColors.Warning)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (canMarkWorkDone) {
                                stringResource(R.string.did_worker_finish_job)
                            } else if (hasAlreadyRated) {
                                stringResource(R.string.rating_submitted_success)
                            } else {
                                stringResource(R.string.rate_worker_help_others)
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (hasAlreadyRated) EmployerColors.Success else EmployerColors.Warning
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onRateWorker,
                            enabled = canMarkWorkDone || !hasAlreadyRated,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canMarkWorkDone) EmployerColors.Success else EmployerColors.Warning,
                                disabledContainerColor = EmployerColors.SuccessLight
                            )
                        ) {
                            Icon(
                                imageVector = if (canMarkWorkDone) Icons.Default.CheckCircle else Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (canMarkWorkDone) stringResource(R.string.mark_work_done) else if (hasAlreadyRated) stringResource(R.string.feedback_submitted) else stringResource(R.string.rate_and_review),
                                style = AppTypography.labelLarge,
                                color = if (hasAlreadyRated) EmployerColors.Success else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingCallsStatusBanner(
    isCallsPaused: Boolean,
    onToggleCalls: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCallsPaused) Color(0xFFFEF2F2).bg() else Color(0xFFF0FDF4).bg()
        ),
        border = BorderStroke(1.dp, if (isCallsPaused) Color(0xFFFECACA).bd() else Color(0xFFBBF7D0).bd())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (isCallsPaused) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCallsPaused) Icons.Default.CallEnd else Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = if (isCallsPaused) Color(0xFFDC2626).fg() else Color(0xFF16A34A).fg(),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isCallsPaused) stringResource(R.string.incoming_calls_stopped) else stringResource(R.string.accepting_worker_calls),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCallsPaused) Color(0xFF991B1B).fg() else Color(0xFF166534).fg()
                    )
                    Text(
                        text = if (isCallsPaused) stringResource(R.string.job_filled_cannot_call) else stringResource(R.string.workers_can_call_desc),
                        fontSize = 11.sp,
                        color = if (isCallsPaused) Color(0xFFB91C1C).fg() else Color(0xFF15803D).fg()
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = !isCallsPaused,
                onCheckedChange = { acceptingCalls ->
                    onToggleCalls(!acceptingCalls)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF16A34A).bd(),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFDC2626).bd()
                )
            )
        }
    }
}

