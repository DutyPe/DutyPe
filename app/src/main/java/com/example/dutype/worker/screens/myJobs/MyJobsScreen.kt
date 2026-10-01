package com.example.dutype.worker.screens.myJobs

import com.dutype.app.R
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

import com.example.dutype.navigation.Routes
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.components.ApplicationStatusBadge
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.models.JobApplication
import com.example.dutype.components.JobCardShimmer
import timber.log.Timber
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth

// ─── Design tokens (pixel-exact "My Jobs" mockup) ──────────────────────────
private val MyJobsInk = Color(0xFF0F0F0F)
private val MyJobsMuted = Color(0xFF64748B)
private val MyJobsFaint = Color(0xFF94A3B8)
private val MyJobsBorder = Color(0xFFE2E8F0)
private val MyJobsEmerald = Color(0xFF10B981)
private val MyJobsEmeraldTint = Color(0xFFF0FDF4)
private val MyJobsScreenBg = Color(0xFFF8FAFC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobsScreen(
    navController: NavHostController,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null
) {
    // Local state management
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val savedJobViewModel: SavedJobsViewModel = hiltViewModel()
    val jobApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()

    // Withdraw dialog state
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var applicationToWithdraw by remember { mutableStateOf<JobApplication?>(null) }

    // Rating state
    var showRatingSheet by remember { mutableStateOf(false) }
    var applicationToRate by remember { mutableStateOf<JobApplication?>(null) }
    var ratedApplicationIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val cardContext = androidx.compose.ui.platform.LocalContext.current
    val ratingService = remember { com.example.dutype.di.ratingServiceFromHilt(cardContext) }
    val ratingScope = rememberCoroutineScope()

    val currentUser = FirebaseAuth.getInstance().currentUser

    // Get real data for both applied and saved jobs
    val jobApplicationUiState by jobApplicationViewModel.legacyUiState.collectAsStateWithLifecycle()
    val applications = jobApplicationUiState.applications
    val savedJobUiState by savedJobViewModel.uiState.collectAsStateWithLifecycle()
    val savedJobs = savedJobUiState.savedJobs

    // Debug logging for MyJobsScreen
    LaunchedEffect(savedJobUiState) {
        Timber.d("MyJobsScreen: SavedJobs UI State - isLoading: ${savedJobUiState.isLoading}, savedJobs: ${savedJobUiState.savedJobs.size}, hasError: ${savedJobUiState.hasError}")
    }

    // Real status buckets driving the Applied / Active / History pill tabs.
    // ApplicationStatus's forward pipeline is APPLIED -> HIRED -> COMPLETED;
    // REJECTED / WITHDRAWN / DELETED / FILLED are terminal, non-progressing
    // states and are grouped into History alongside COMPLETED.
    val appliedApplications = remember(applications) {
        applications.filter { it.status == ApplicationStatus.APPLIED }
            .sortedByDescending { it.createdAt }
    }
    val activeApplications = remember(applications) {
        applications.filter { it.status == ApplicationStatus.HIRED }
            .sortedByDescending { it.createdAt }
    }
    val historyApplications = remember(applications) {
        applications.filter {
            it.status == ApplicationStatus.COMPLETED ||
                it.status == ApplicationStatus.REJECTED ||
                it.status == ApplicationStatus.WITHDRAWN
        }.sortedByDescending { it.createdAt }
    }

    // Status bar matches the exact-spec screen background.
    val statusBarColor = MyJobsScreenBg
    LaunchedEffect(selectedTabIndex, statusBarColor) {
        onStatusBarColorChange(statusBarColor)

        // Refresh applications when switching to any application-backed tab
        if (selectedTabIndex in 0..2) {
            jobApplicationViewModel.loadMyApplications()
        } else if (selectedTabIndex == 3) {
            savedJobViewModel.loadSavedJobs()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MyJobsScreenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Offline banner at the very top
            val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
            val isOnline by connectivityViewModel.isOnline.collectAsState()
            com.example.dutype.components.OfflineBanner(isOffline = !isOnline)

            // Header: bold 22sp "My Jobs" title + pill tab row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.my_jobs),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MyJobsInk
                )

                Spacer(modifier = Modifier.height(16.dp))

                val tabScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tabScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MyJobsPillTab(
                        label = stringResource(R.string.my_jobs_applied_count, appliedApplications.size),
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 }
                    )
                    MyJobsPillTab(
                        label = stringResource(R.string.my_jobs_active_count, activeApplications.size),
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 }
                    )
                    MyJobsPillTab(
                        label = stringResource(R.string.my_jobs_history_count, historyApplications.size),
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 }
                    )
                    MyJobsPillTab(
                        label = stringResource(R.string.my_jobs_saved_count, savedJobs.size),
                        selected = selectedTabIndex == 3,
                        onClick = { selectedTabIndex = 3 }
                    )
                }
            }

            // Content based on selected tab
            when (selectedTabIndex) {
                0 -> {
                    MyJobsStatusListContent(
                        isLoading = jobApplicationUiState.isLoading,
                        items = appliedApplications,
                        scrollStateManager = scrollStateManager,
                        emptyIcon = Icons.Outlined.Inbox,
                        emptyMessage = stringResource(R.string.my_jobs_no_applied_jobs),
                        navController = navController
                    ) { application ->
                        AppliedJobCard(
                            application = application,
                            onClick = {
                                navController.navigate(Routes.jobDetailRoute(application.jobId)) {
                                    popUpTo(com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS) {
                                        inclusive = false
                                    }
                                }
                            },
                            onWithdrawClick = {
                                applicationToWithdraw = application
                                showWithdrawDialog = true
                            }
                        )
                    }
                }
                1 -> {
                    MyJobsStatusListContent(
                        isLoading = jobApplicationUiState.isLoading,
                        items = activeApplications,
                        scrollStateManager = scrollStateManager,
                        emptyIcon = Icons.Outlined.WorkOutline,
                        emptyMessage = stringResource(R.string.my_jobs_no_active_jobs),
                        navController = navController
                    ) { application ->
                        ActiveJobCard(
                            application = application,
                            onClick = {
                                navController.navigate(Routes.jobDetailRoute(application.jobId)) {
                                    popUpTo(com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS) {
                                        inclusive = false
                                    }
                                }
                            }
                        )
                    }
                }
                2 -> {
                    MyJobsStatusListContent(
                        isLoading = jobApplicationUiState.isLoading,
                        items = historyApplications,
                        scrollStateManager = scrollStateManager,
                        emptyIcon = Icons.Outlined.History,
                        emptyMessage = stringResource(R.string.my_jobs_no_history_jobs),
                        navController = navController
                    ) { application ->
                        HistoryJobCard(
                            application = application,
                            onClick = {
                                navController.navigate(Routes.jobDetailRoute(application.jobId)) {
                                    popUpTo(com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS) {
                                        inclusive = false
                                    }
                                }
                            },
                            onWithdrawClick = { app ->
                                applicationToWithdraw = app
                                showWithdrawDialog = true
                            },
                            onRateClick = { app ->
                                applicationToRate = app
                                showRatingSheet = true
                            },
                            hasAlreadyRated = application.id in ratedApplicationIds
                        )
                    }
                }
                3 -> {
                    SavedJobsList(
                        searchQuery = "",
                        onNavigateToJobDetails = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        },
                        scrollStateManager = scrollStateManager,
                        navController = navController
                    )
                }
            }
        }
    }

    // Withdraw Confirmation Dialog
    if (showWithdrawDialog && applicationToWithdraw != null) {
        AlertDialog(
            onDismissRequest = {
                showWithdrawDialog = false
                applicationToWithdraw = null
            },
            title = {
                Text(
                    text = stringResource(R.string.withdraw_application_title),
                    fontWeight = FontWeight.Bold,
                    color = MyJobsInk
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.withdraw_application_message),
                    color = MyJobsMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        applicationToWithdraw?.let { app ->
                            jobApplicationViewModel.withdrawApplication(app) { success, error ->
                                if (success) {
                                    Timber.d("Application withdrawn successfully")
                                    android.widget.Toast.makeText(cardContext, cardContext.getString(R.string.my_jobs_application_withdrawn), android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    Timber.e("Failed to withdraw application: $error")
                                    android.widget.Toast.makeText(
                                        cardContext,
                                        error ?: cardContext.getString(R.string.my_jobs_withdraw_failed),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        showWithdrawDialog = false
                        applicationToWithdraw = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.withdraw),
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showWithdrawDialog = false
                        applicationToWithdraw = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MyJobsMuted
                    )
                }
            }
        )
    }

    // Rating Bottom Sheet
    com.example.dutype.components.RatingBottomSheet(
        isVisible = showRatingSheet && applicationToRate != null,
        targetName = applicationToRate?.companyName ?: "",
        targetRole = "EMPLOYER",
        onDismiss = {
            showRatingSheet = false
            applicationToRate = null
        },
        onSubmit = { rating, review, tags ->
            applicationToRate?.let { app ->
                ratingScope.launch {
                    val result = ratingService.submitRating(
                        jobId = app.jobId,
                        targetUserId = app.employerId,
                        rating = rating,
                        review = review,
                        tags = tags,
                        targetRole = "EMPLOYER"
                    )
                    result.onSuccess { ratingResult ->
                        if (ratingResult.success) {
                            ratedApplicationIds = ratedApplicationIds + app.id
                            showRatingSheet = false
                            applicationToRate = null
                        }
                        android.widget.Toast.makeText(
                            cardContext,
                            ratingResult.message,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure { error ->
                        android.widget.Toast.makeText(
                            cardContext,
                            error.message ?: cardContext.getString(R.string.my_jobs_rating_submit_failed),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    )

    // Check which applications have already been rated
    LaunchedEffect(applications) {
        currentUser?.uid?.let {
            val completedApps = applications.filter { it.status == ApplicationStatus.COMPLETED }
            val rated = mutableSetOf<String>()
            completedApps.forEach { app ->
                if (ratingService.hasRated(app.jobId, app.employerId)) {
                    rated.add(app.id)
                }
            }
            ratedApplicationIds = rated
        }
    }
}

/**
 * Shared list chrome (loading shimmer / empty state / real list) for the
 * Applied, Active and History tabs — each backed by a real status-filtered
 * slice of [SmartJobApplicationViewModel]'s applications.
 */
@Composable
private fun MyJobsStatusListContent(
    isLoading: Boolean,
    items: List<JobApplication>,
    scrollStateManager: ScrollStateManager?,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    emptyMessage: String,
    navController: NavHostController,
    itemContent: @Composable (JobApplication) -> Unit
) {
    when {
        isLoading -> {
            ScrollAwareLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                scrollStateManager = scrollStateManager
            ) {
                items(4) {
                    JobCardShimmer()
                }
            }
        }
        items.isEmpty() -> {
            MyJobsEmptyState(
                icon = emptyIcon,
                message = emptyMessage,
                navController = navController
            )
        }
        else -> {
            ScrollAwareLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentPadding = PaddingValues(top = 4.dp, start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                scrollStateManager = scrollStateManager
            ) {
                itemsIndexed(
                    items = items,
                    key = { index, application -> "myjobs_${application.id.ifBlank { "app" }}_$index" }
                ) { _, application ->
                    itemContent(application)
                }
            }
        }
    }
}

// ─── Pill tab ───────────────────────────────────────────────────────────────
@Composable
private fun MyJobsPillTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (selected) MyJobsInk else Color.White)
            .then(
                if (!selected) Modifier.border(1.dp, MyJobsBorder, RoundedCornerShape(28.dp)) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else MyJobsInk
        )
    }
}

// ─── Applied tab card ───────────────────────────────────────────────────────
@Composable
private fun AppliedJobCard(application: JobApplication, onClick: () -> Unit, onWithdrawClick: () -> Unit) {
    val dialEmployer = rememberEmployerDialer()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, MyJobsBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = application.jobTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MyJobsInk
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = application.companyName,
            fontSize = 13.sp,
            color = MyJobsMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = appliedAgoLabel(application.createdAt),
            fontSize = 11.sp,
            color = MyJobsFaint
        )
        Spacer(modifier = Modifier.height(18.dp))
        JobProgressTracker(
            steps = listOf(stringResource(R.string.applied), stringResource(R.string.hired), stringResource(R.string.completed)),
            currentIndex = applicationStepIndex(application.status)
        )
        Spacer(modifier = Modifier.height(14.dp))
        val context = androidx.compose.ui.platform.LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Same dial behaviour as the Active card; falls back to the job page
            // (which shows the contact) when the number isn't loaded yet.
            MyJobsActionButton(
                label = stringResource(R.string.my_jobs_call_employer),
                filled = true,
                modifier = Modifier.weight(1f),
                onClick = { dialEmployer(application, onClick) }
            )
            // Worker can take back an application until the employer hires.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(22.dp))
                    .clickable(onClick = onWithdrawClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.withdraw),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFDC2626)
                )
            }
        }
    }
}

// ─── Active tab card ────────────────────────────────────────────────────────
@Composable
private fun ActiveJobCard(application: JobApplication, onClick: () -> Unit) {
    val dialEmployer = rememberEmployerDialer()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, MyJobsBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MyJobsEmeraldTint)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.my_jobs_active_now),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MyJobsEmerald
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = application.jobTitle,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MyJobsInk
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Call Employer — reuses the app's existing ACTION_DIAL pattern
            // (see JobApplicationCard.kt) against the denormalized
            // application.employerPhone field.
            MyJobsActionButton(
                label = stringResource(R.string.my_jobs_call_employer),
                filled = false,
                modifier = Modifier.weight(1f),
                onClick = { dialEmployer(application, onClick) }
            )

            // Get Directions — mirrors JobDescriptionScreen's Google Maps
            // navigation intent with a browser fallback; JobApplication has
            // no lat/lng snapshot, so the real job address text is used as
            // the map query instead of coordinates.
            MyJobsActionButton(
                label = stringResource(R.string.my_jobs_get_directions),
                filled = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    val job = application.job
                    val query = android.net.Uri.encode(application.jobArea.ifBlank { application.companyName })
                    runCatching {
                        val uri = if (job != null && (job.lat != 0.0 || job.lng != 0.0)) {
                            android.net.Uri.parse("geo:${job.lat},${job.lng}?q=${job.lat},${job.lng}($query)")
                        } else {
                            android.net.Uri.parse("geo:0,0?q=$query")
                        }
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        context.startActivity(intent)
                    }.onFailure {
                        runCatching {
                            val webUri = android.net.Uri.parse(
                                "https://www.google.com/maps/search/?api=1&query=$query"
                            )
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, webUri))
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MyJobsActionButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (filled) MyJobsEmerald else Color.White)
            .then(
                if (!filled) Modifier.border(1.dp, MyJobsBorder, RoundedCornerShape(12.dp)) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (filled) Color.White else MyJobsInk,
            textAlign = TextAlign.Center
        )
    }
}

// ─── History tab card ───────────────────────────────────────────────────────
@Composable
private fun HistoryJobCard(
    application: JobApplication,
    onClick: () -> Unit,
    onWithdrawClick: (JobApplication) -> Unit,
    onRateClick: (JobApplication) -> Unit,
    hasAlreadyRated: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, MyJobsBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = application.jobTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MyJobsInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = application.companyName,
                    fontSize = 13.sp,
                    color = MyJobsMuted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            ApplicationStatusBadge(status = application.status)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = appliedAgoLabel(application.createdAt),
            fontSize = 11.sp,
            color = MyJobsFaint
        )

        if (application.status == ApplicationStatus.COMPLETED && !hasAlreadyRated) {
            Spacer(modifier = Modifier.height(12.dp))
            MyJobsActionButton(
                label = stringResource(R.string.rate_employer),
                filled = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onRateClick(application) }
            )
        }
    }
}

// ─── 3-step progress tracker (Applied → Hired → Completed) ────────────────
private fun applicationStepIndex(status: ApplicationStatus): Int = when (status) {
    ApplicationStatus.APPLIED -> 0
    ApplicationStatus.HIRED -> 1
    ApplicationStatus.COMPLETED -> 2
    else -> 0
}

@Composable
private fun JobProgressTracker(steps: List<String>, currentIndex: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { index, label ->
            val state = when {
                index < currentIndex -> TrackerStepState.COMPLETED
                index == currentIndex -> TrackerStepState.CURRENT
                else -> TrackerStepState.PENDING
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                TrackerStepCircle(state = state)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = if (state == TrackerStepState.CURRENT) FontWeight.SemiBold else FontWeight.Normal,
                    color = when (state) {
                        TrackerStepState.COMPLETED -> MyJobsMuted
                        TrackerStepState.CURRENT -> MyJobsInk
                        TrackerStepState.PENDING -> MyJobsFaint
                    }
                )
            }

            if (index != steps.lastIndex) {
                val connectorColor = if (index < currentIndex) MyJobsInk else MyJobsBorder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 15.dp)
                        .height(2.dp)
                        .background(connectorColor)
                )
            }
        }
    }
}

private enum class TrackerStepState { COMPLETED, CURRENT, PENDING }

@Composable
private fun TrackerStepCircle(state: TrackerStepState) {
    when (state) {
        TrackerStepState.COMPLETED -> {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MyJobsInk),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        TrackerStepState.CURRENT -> {
            val infiniteTransition = rememberInfiniteTransition(label = "trackerPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "trackerPulseScale"
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, MyJobsEmerald, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MyJobsEmerald)
                )
            }
        }
        TrackerStepState.PENDING -> {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, MyJobsBorder, CircleShape)
            )
        }
    }
}

// ─── Empty state ────────────────────────────────────────────────────────────
@Composable
private fun MyJobsEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    navController: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MyJobsBorder.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MyJobsFaint,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            fontSize = 16.sp,
            color = MyJobsMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MyJobsInk)
                .clickable {
                    runCatching {
                        com.example.dutype.components.navigateToWorkerTab(navController, com.example.dutype.navigation.WorkerBottomRoutes.JOBS)
                    }.onFailure { error ->
                        Timber.e(error, "Failed to navigate to all jobs from my jobs empty state")
                    }
                }
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.my_jobs_browse_jobs),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────
/** `application.createdAt` is when the worker applied, so the label says "Applied". */
@Composable
private fun appliedAgoLabel(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diffMs = System.currentTimeMillis() - timestamp
    val diffMinutes = diffMs / 60_000
    val diffHours = diffMinutes / 60
    val diffDays = diffHours / 24
    return when {
        diffMinutes < 60 -> stringResource(R.string.my_jobs_applied_just_now)
        diffHours < 24 -> stringResource(R.string.my_jobs_applied_hours_ago, diffHours)
        else -> stringResource(R.string.my_jobs_applied_days_ago, diffDays)
    }
}

/**
 * Dials the employer of an application. The number comes from applyToJob(viaCall), which also
 * counts the call;
 * [fallback] (open the job page) runs when it is not available.
 */
@Composable
private fun rememberEmployerDialer(): (com.example.dutype.models.JobApplication, () -> Unit) -> Unit {
    val viewModel: com.example.dutype.viewmodels.SmartJobApplicationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(viewModel) {
        { application, fallback ->
            scope.launch {
                val phone = viewModel.callEmployer(application.jobId)
                if (phone == null) {
                    fallback()
                } else {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone"))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        }
    }
}
