package com.example.dutype.worker.screens

import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.WorkOff
import com.dutype.app.R
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dutype.components.OfflineBanner
import com.example.dutype.components.ShareJobIconButton
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobListing
import com.example.dutype.models.parseTrustTier
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.utils.findActivity
import com.example.dutype.viewmodels.ConnectivityViewModel
import com.example.dutype.viewmodels.JobDetailViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.worker.components.JobCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDescriptionScreen(
    jobId: String,
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val jobViewModel: JobDetailViewModel = hiltViewModel()
    val smartApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    val profileCompletionViewModel: com.example.dutype.viewmodels.ProfileCompletionViewModel = hiltViewModel()
    val savedJobsViewModel: com.example.dutype.viewmodels.SavedJobsViewModel = hiltViewModel()
    val profileCompletionService = profileCompletionViewModel.profileCompletionService

    val locationPreferences = remember { com.example.dutype.location.LocationPreferences(context) }
    val currentLocation by locationPreferences.currentLocation.collectAsStateWithLifecycle()
    val savedJobIds by savedJobsViewModel.savedJobIds.collectAsStateWithLifecycle()
    val appliedJobIds by smartApplicationViewModel.appliedJobIds.collectAsStateWithLifecycle()
    val applicationStatuses by smartApplicationViewModel.applicationStatuses.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) { onStatusBarColorChange(Color.White) }

    var job by remember { mutableStateOf<JobListing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var retryTrigger by remember { mutableStateOf(0) }

    // Batch-m #3: the sticky Call + Apply bar now appears immediately on
    // screen entry (no more reveal-on-scroll). Users were missing the
    // primary action because they didn't realise they had to scroll. The
    // bar is still hidden when the inline action row inside the page
    // body is on screen, so the user is never shown two duplicate bars
    // at once.
    val detailsListState = rememberLazyListState()

    // Batch-i #1: when the inline Call + Apply row (inserted right after
    // the "don't pay fee" safety banner) is on screen, the sticky bottom
    // bar disappears so the user is never shown two duplicate action
    // bars at once. We key off the LazyColumn item key `"inline_actions"`
    // so this stays correct regardless of list content around it.
    val inlineActionsVisible by remember {
        androidx.compose.runtime.derivedStateOf {
            detailsListState.layoutInfo.visibleItemsInfo.any { it.key == "inline_actions" }
        }
    }
    var similarJobs by remember { mutableStateOf<List<JobListing>>(emptyList()) }

    // Report state
    var showReportSheet by remember { mutableStateOf(false) }
    var showNonPaymentSheet by remember { mutableStateOf(false) }

    // ReportingService accessed via SmartJobApplicationViewModel (proper DI pattern)
    val reportingService = smartApplicationViewModel.reportingService
    val reviewTriggerService = com.example.dutype.di.rememberInAppReviewTriggerService()
    val callLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        context.findActivity()?.let { reviewTriggerService.onWorkerDirectContact(it) }
    }
    /** Calling counts as applying: the employer sees the caller among the applicants. */
    val launchEmployerDialer: (JobListing) -> Unit = { currentJob ->
        scope.launch {
            val phone = smartApplicationViewModel.callEmployer(currentJob.id)
            if (phone == null) {
                android.widget.Toast.makeText(context, context.getString(R.string.contact_number_not_available), android.widget.Toast.LENGTH_SHORT).show()
            } else {
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:$phone")
                }
                try {
                    callLauncher.launch(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to start dialer")
                    android.widget.Toast.makeText(context, context.getString(R.string.no_dialer_app_available), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Guest mode - Login bottom sheet state
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }

    // Notification permission prompt on saving jobs
    var showSaveJobNotificationPrompt by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {
        showSaveJobNotificationPrompt = false
    }

    val applicationUiState by smartApplicationViewModel.uiState.collectAsStateWithLifecycle()
    // REMOVED: jobApplicationUiState - not needed, we use smartApplicationViewModel.hasUserApplied() instead

    // Reactive auth state so the UI updates immediately after a login via
    // the bottom sheet — the old `val currentUser = ...getInstance().currentUser`
    // was a one-shot snapshot captured at composition time and stayed null
    // after the LoginBottomSheet signed in the user.
    var currentUser by remember { mutableStateOf(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser) }
    DisposableEffect(Unit) {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }
    val resolvedJobId = remember(jobId, job?.id, job?.jobId) {
        when {
            !job?.id.isNullOrBlank() -> job?.id.orEmpty()
            !job?.jobId.isNullOrBlank() -> job?.jobId.orEmpty()
            else -> jobId
        }
    }
    val isSaved = remember(savedJobIds, resolvedJobId, job?.isSaved) {
        (job?.isSaved == true) || resolvedJobId in savedJobIds
    }
    val currentApplicationStatus = remember(applicationStatuses, appliedJobIds, resolvedJobId) {
        applicationStatuses[resolvedJobId]
            ?: if (resolvedJobId in appliedJobIds) ApplicationStatus.APPLIED else null
    }
    val hasApplied = currentApplicationStatus != null
    val applicationStatus = currentApplicationStatus?.name

    // Prime shared saved/apply state for direct-entry detail screens
    LaunchedEffect(jobId, currentUser?.uid) {
        if (jobId.isNotEmpty()) {
            savedJobsViewModel.isJobSaved(jobId) { }
        }
        if (currentUser != null && jobId.isNotEmpty()) {
            smartApplicationViewModel.hasUserApplied(jobId) { }
        }
    }

    // Consolidated: Handle application success and error
    LaunchedEffect(applicationUiState.applicationSuccess, applicationUiState.error) {
        if (applicationUiState.applicationSuccess) {
            snackbarMessage = context.getString(R.string.job_desc_application_submitted)
            showSnackbar = true
            smartApplicationViewModel.clearSuccessStates()
        }
        applicationUiState.error?.let { errorMsg ->
            snackbarMessage = errorMsg
            showSnackbar = true
            smartApplicationViewModel.clearError()
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.3f else 1f,
        // Batch-p #2 perf: shortened the fade so users see the description
        // and benefits as soon as Firestore returns; the previous 600ms
        // tween made the screen feel sluggish even when data was already in.
        animationSpec = tween(150, easing = EaseInOutQuart),
        label = "contentAlpha"
    )

    LaunchedEffect(jobId, retryTrigger) {
        if (jobId.isNotEmpty()) {
            isLoading = true
            error = null
            try {
                // Same origin as the feed: a place the worker picked (e.g. Delhi) when they chose one,
                // else where the phone is now (last known position), else the saved location.
                val device = if (locationPreferences.isManualLocationLocked()) null
                    else com.example.dutype.worker.components.lastDeviceLatLng(context)
                jobViewModel.getJob(jobId).fold(
                    onSuccess = { fetchedJob ->
                        val here = device ?: currentLocation
                            ?.takeIf { it.latitude != 0.0 || it.longitude != 0.0 }
                            ?.let { it.latitude to it.longitude }
                        job = if (fetchedJob != null && here != null &&
                            (fetchedJob.lat != 0.0 || fetchedJob.lng != 0.0)
                        ) {
                            fetchedJob.copy(
                                distance = com.example.dutype.jobs.Geohash.distanceKm(
                                    here.first, here.second, fetchedJob.lat, fetchedJob.lng
                                )
                            )
                        } else fetchedJob
                        if (fetchedJob == null) error = context.getString(R.string.job_desc_job_no_longer_available)
                        isLoading = false
                    },
                    onFailure = { exception ->
                        error = exception.message ?: context.getString(R.string.job_desc_failed_load)
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                if (job == null) {
                    error = e.message ?: context.getString(R.string.job_desc_failed_load)
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(job?.id, currentLocation?.latitude, currentLocation?.longitude) {
        val currentJob = job ?: return@LaunchedEffect
        similarJobs = try {
            jobViewModel.similarJobs(currentJob, limit = 5)
                .getOrElse { exception ->
                    Timber.w(exception, "JobDescriptionScreen: Failed to load recommended jobs for ${currentJob.id}")
                    emptyList()
                }
        } catch (e: Exception) {
            Timber.w(e, "JobDescriptionScreen: Non-critical recommended jobs load failure")
            emptyList()
        }
    }

    // Function to handle back navigation
    val handleBackNavigation: () -> Unit = {
        navController.popBackStack()
    }

    BackHandler {
        handleBackNavigation()
    }

    Box(modifier = Modifier.fillMaxSize().background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)) {
        // Main content - ad is shown before navigation (in WorkerHomeScreen)
        Column(modifier = Modifier.fillMaxSize()) {
            // Offline banner at the very top
            val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
            val isOnline by connectivityViewModel.isOnline.collectAsState()
            OfflineBanner(isOffline = !isOnline)

            // Stitch design spec top bar — two small outlined square icon
            // buttons (back / share) only; the title now lives in the job
            // header card below, and Save moved to the bottom bookmark
            // button next to "Apply Now (1-Tap)".
            val handleSaveClick: () -> Unit = {
                if (currentUser == null) {
                    pendingAction = "save"
                    showLoginBottomSheet = true
                } else {
                    if (isSaved) {
                        savedJobsViewModel.unsaveJob(resolvedJobId)
                        snackbarMessage = context.getString(R.string.job_desc_job_removed)
                    } else {
                        savedJobsViewModel.saveJob(resolvedJobId)
                        snackbarMessage = context.getString(R.string.job_desc_job_saved)
                        val hasNotifPerm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                        if (!hasNotifPerm && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            showSaveJobNotificationPrompt = true
                        }
                    }
                    showSnackbar = true
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WorkerColors.ScreenBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WorkerColors.CardBackground)
                        .border(1.dp, WorkerColors.Border, RoundedCornerShape(12.dp))
                        .clickable { handleBackNavigation() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = WorkerColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                job?.let { currentJob ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WorkerColors.CardBackground)
                                .border(1.dp, WorkerColors.Border, RoundedCornerShape(12.dp))
                                .clickable { handleSaveClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isSaved) stringResource(R.string.job_desc_remove_from_saved) else stringResource(R.string.save_job),
                                tint = if (isSaved) WorkerColors.Error else WorkerColors.TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WorkerColors.CardBackground)
                                .border(1.dp, WorkerColors.Border, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            ShareJobIconButton(
                                job = currentJob,
                                tint = WorkerColors.TextPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = WorkerColors.Success,
                    trackColor = WorkerColors.Border
                )
            }

            // Shared apply-click behaviour used by BOTH the inline action
            // row (in content) and the sticky bottom bar. Declared at
            // screen scope so the two button sites can never drift.
            val handleApplyClick: () -> Unit = {
                scope.launch {
                    val currentUserId = currentUser?.uid
                    if (currentUserId != null) {
                        val canApply = profileCompletionService.canApplyDirectly(currentUserId)
                        canApply.fold(
                            onSuccess = { allowed ->
                                if (allowed) {
                                    navController.navigate(Routes.jobApplicationRoute(jobId))
                                } else {
                                    navController.navigate(
                                        Routes.profileSetupWithReturnRoute(Routes.jobApplicationRoute(jobId))
                                    )
                                }
                            },
                            onFailure = { err ->
                                android.widget.Toast.makeText(
                                    context,
                                    "Error checking profile: ${err.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
            val handleLoginRequired: (String) -> Unit = { action ->
                pendingAction = action
                showLoginBottomSheet = true
            }
            val handleCallClick: (JobListing) -> Unit = { currentJob ->
                if (currentUser == null) {
                    handleLoginRequired("call")
                } else {
                    launchEmployerDialer(currentJob)
                }
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f).graphicsLayer(alpha = contentAlpha)) {
                when {
                    isLoading -> JobDescriptionLoadingContent()
                    error != null -> JobDescriptionErrorContent(
                        jobGone = error == stringResource(R.string.job_desc_job_no_longer_available),
                        onRetry = { retryTrigger++ },
                        onFindJobs = {
                            runCatching {
                                com.example.dutype.components.navigateToWorkerTab(navController, com.example.dutype.navigation.WorkerBottomRoutes.JOBS)
                            }.onFailure { navController.popBackStack() }
                        },
                        onBack = { navController.popBackStack() }
                    )
                    job != null -> JobDetailsContent(
                        job = job!!,
                        listState = detailsListState,
                        similarJobs = similarJobs,
                        savedJobIds = savedJobIds,
                        onSimilarJobSaveToggle = { jobIdToToggle, shouldSave ->
                            if (shouldSave) {
                                savedJobsViewModel.saveJob(jobIdToToggle)
                            } else {
                                savedJobsViewModel.unsaveJob(jobIdToToggle)
                            }
                        },
                        onReportClick = { showReportSheet = true },
                        onReportNonPaymentClick = { showNonPaymentSheet = true },
                        hasApplied = hasApplied,
                        onJobClick = { clickedJobId ->
                            navController.navigate(Routes.jobDetailRoute(clickedJobId))
                        },
                        inlineActions = {
                            ActionButtonsContent(
                                job = job!!,
                                currentUser = currentUser,
                                context = context,
                                navController = navController,
                                hasApplied = hasApplied,
                                applicationStatus = applicationStatus,
                                onCallClick = handleCallClick,
                                onApplyClick = handleApplyClick,
                                onLoginRequired = handleLoginRequired,
                            )
                        }
                    )
                }
            }

            if (job != null && !isLoading && error == null) {
                // Batch-m #3: sticky bar shown immediately (was reveal-on-
                // scroll). Still hidden when inline Call+Apply row is on
                // screen so the user never sees two duplicate bars.
                AnimatedVisibility(
                    visible = !inlineActionsVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    BottomActionBar(
                        job = job!!,
                        currentUser = currentUser,
                        context = context,
                        navController = navController,
                        jobId = jobId,
                        hasApplied = hasApplied,
                        applicationStatus = applicationStatus,
                        isSaved = isSaved,
                        onSaveClick = handleSaveClick,
                        onCallClick = handleCallClick,
                        onApplyClick = handleApplyClick,
                        onLoginRequired = handleLoginRequired,
                    )
                }
            }
        }

        // Snackbar
        AnimatedVisibility(
            visible = showSnackbar,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 110.dp)
        ) {
            LaunchedEffect(showSnackbar) { kotlinx.coroutines.delay(2500); showSnackbar = false }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WorkerColors.Primary), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (snackbarMessage.contains("saved")) Icons.Default.CheckCircle else Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Text(snackbarMessage, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // Report Job Bottom Sheet
    if (showReportSheet && job != null) {
        com.example.dutype.components.ReportJobSheet(
            jobTitle = job!!.title,
            companyName = job!!.companyName,
            onDismiss = { showReportSheet = false },
            onReport = { reportType, description ->
                reportingService.reportJob(jobId, job!!.title, job!!.companyName, reportType, description)
            }
        )
    }

    // Non-payment claim needs an amount and work date, so it has its own sheet.
    if (showNonPaymentSheet && job != null) {
        com.example.dutype.components.ReportNonPaymentSheet(
            jobTitle = job!!.title,
            companyName = job!!.companyName,
            onDismiss = { showNonPaymentSheet = false },
            onReport = { amount, workedOn, description ->
                reportingService.reportNonPayment(
                    jobId = jobId,
                    jobTitle = job!!.title,
                    companyName = job!!.companyName,
                    amountOwed = amount,
                    workedOn = workedOn,
                    description = description
                )
            }
        )
    }

    // Guest Mode - Login Bottom Sheet
    com.example.dutype.components.LoginBottomSheet(
        isVisible = showLoginBottomSheet,
        onDismiss = {
            showLoginBottomSheet = false
            pendingAction = null
        },
        onLoginSuccess = {
            showLoginBottomSheet = false
            // Execute the pending action after successful login
            when (pendingAction) {
                "apply" -> {
                    // Navigate to JobApplicationScreen for review before submitting
                    navController.navigate(Routes.jobApplicationRoute(jobId))
                }
                "save" -> {
                    // Save the job after login
                    job?.let { currentJob ->
                        scope.launch {
                            try {
                                savedJobsViewModel.saveJob(currentJob.id.ifBlank { currentJob.jobId })
                                snackbarMessage = context.getString(R.string.job_desc_job_saved)
                                showSnackbar = true
                            } catch (e: Exception) {
                                Timber.e(e, "Error saving job after login")
                            }
                        }
                    }
                }
                "call" -> {
                    job?.let(launchEmployerDialer)
                }
            }
            pendingAction = null
        },
        onProfileSetupRequired = {
            // For job application - navigate to profile setup with return route to job application
            showLoginBottomSheet = false
            android.widget.Toast.makeText(context, context.getString(R.string.complete_profile_to_apply), android.widget.Toast.LENGTH_SHORT).show()
            // Navigate to profile setup with return route to job application screen
            navController.navigate(Routes.profileSetupWithReturnRoute(Routes.jobApplicationRoute(jobId)))
            pendingAction = null
        },
        requiresProfileCheck = pendingAction == "apply", // Only check profile for job applications
        role = com.example.dutype.models.UserRole.WORKER,
        title = stringResource(R.string.login_to_continue),
        subtitle = when (pendingAction) {
            "apply" -> stringResource(R.string.login_apply_job)
            "save" -> stringResource(R.string.login_save_job)
            "call" -> stringResource(R.string.login_call_employer)
            else -> stringResource(R.string.please_login_continue)
        }
    )

    // Notification permission prompt when worker saves/bookmarks a job
    com.example.dutype.components.NotificationPermissionBottomSheet(
        isVisible = showSaveJobNotificationPrompt,
        onDismiss = { showSaveJobNotificationPrompt = false },
        onEnableNotifications = {
            showSaveJobNotificationPrompt = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        userRole = "worker"
    )

}

@Composable
private fun BottomActionBar(
    job: JobListing,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    context: android.content.Context,
    navController: NavController,
    jobId: String,
    hasApplied: Boolean = false,
    applicationStatus: String? = null,
    isSaved: Boolean = false,
    onSaveClick: () -> Unit = {},
    onCallClick: (JobListing) -> Unit = {},
    onApplyClick: () -> Unit = {},
    onLoginRequired: (String) -> Unit = {} // Callback for guest mode login
) {
    // Stitch design spec sticky bar: bookmark square button + full-width
    // "Apply Now (1-Tap) →" black pill. The Call action stays reachable via
    // the inline Call+Apply row shown further up the page (ActionButtonsContent),
    // so the real "call the employer" feature is not lost — just not
    // duplicated in this literal 2-element bottom bar.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WorkerColors.CardBackground)
            .border(BorderStroke(1.dp, WorkerColors.Border))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Call the employer + Apply now (save moved to the top bar, next to share).
            ActionButtonsContent(
                job = job,
                currentUser = currentUser,
                context = context,
                navController = navController,
                hasApplied = hasApplied,
                applicationStatus = applicationStatus,
                onCallClick = onCallClick,
                onApplyClick = onApplyClick,
                onLoginRequired = onLoginRequired
            )
        }
    }
}

/**
 * Full-width black pill Apply button — same hasApplied/closed/open states
 * as the shared [ActionButtonsContent] Apply button, just laid out to fill
 * its row instead of sharing it 50/50 with a Call button.
 */
@Composable
private fun ApplyNowFullWidthButton(
    job: JobListing,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    hasApplied: Boolean,
    applicationStatus: String?,
    navController: NavController,
    onApplyClick: () -> Unit,
    onLoginRequired: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        hasApplied -> {
            Button(
                onClick = { com.example.dutype.components.navigateToWorkerTab(navController, com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS) },
                modifier = modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (applicationStatus) {
                        "ACCEPTED" -> WorkerColors.Success
                        "PENDING", "UNDER_REVIEW" -> WorkerColors.Warning
                        else -> WorkerColors.Primary
                    }
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = when (applicationStatus) {
                            "ACCEPTED" -> Icons.Default.CheckCircle
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = when (applicationStatus) {
                            "ACCEPTED" -> stringResource(R.string.hired_status)
                            "PENDING" -> stringResource(R.string.applied)
                            "UNDER_REVIEW" -> stringResource(R.string.under_review)
                            else -> stringResource(R.string.applied)
                        },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        job.status.lowercase() in setOf("closed", "filled", "deleted", "expired") -> {
            Button(
                onClick = { },
                enabled = false,
                modifier = modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = Color(0xFFE2E8F0).bg(),
                    disabledContentColor = Color(0xFF64748B).fg()
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = when (job.status.lowercase()) {
                        "filled" -> stringResource(R.string.job_desc_position_filled)
                        "deleted" -> stringResource(R.string.job_desc_job_removed_status)
                        "expired" -> stringResource(R.string.job_desc_job_expired)
                        else -> stringResource(R.string.job_desc_job_closed)
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
        else -> {
            Button(
                onClick = {
                    if (currentUser == null) onLoginRequired("apply") else onApplyClick()
                },
                modifier = modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = stringResource(R.string.job_desc_apply_now_1tap),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Reusable Call + Apply row. Shared by the sticky [BottomActionBar] and the
 * inline (in-content) action row that appears below the "Don't pay fee"
 * safety banner. Keeping this in one place guarantees both entry points
 * behave identically (same apply preconditions, same guest login flow).
 */
@Composable
private fun RowScope.ActionButtonsContent(
    job: JobListing,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    context: android.content.Context,
    navController: NavController,
    hasApplied: Boolean,
    applicationStatus: String?,
    onCallClick: (JobListing) -> Unit,
    onApplyClick: () -> Unit,
    onLoginRequired: (String) -> Unit,
) {
    // Call Button - Half width with icon and text
    OutlinedButton(
        onClick = {
            if (currentUser == null) {
                onLoginRequired("call")
            } else {
                onCallClick(job)
            }
        },
        modifier = Modifier.weight(1f).height(56.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, WorkerColors.Border),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(Icons.Default.Phone, null, tint = com.example.dutype.ui.theme.WorkerColors.TextPrimary, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.call), color = com.example.dutype.ui.theme.WorkerColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }

    // Apply Now Button - Shows different states based on application status
    if (hasApplied) {
        Button(
            onClick = {
                com.example.dutype.components.navigateToWorkerTab(navController, com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS)
            },
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (applicationStatus) {
                    "ACCEPTED" -> WorkerColors.Success
                    "PENDING", "UNDER_REVIEW" -> WorkerColors.Warning
                    else -> WorkerColors.Primary
                }
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = when (applicationStatus) {
                        "ACCEPTED" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Schedule
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = when (applicationStatus) {
                        "ACCEPTED" -> stringResource(R.string.hired_status)
                        "PENDING" -> stringResource(R.string.applied)
                        "UNDER_REVIEW" -> stringResource(R.string.under_review)
                        else -> stringResource(R.string.applied)
                    },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    } else if (job != null && job.status.lowercase() in setOf("closed", "filled", "deleted", "expired")) {
        Button(
            onClick = { },
            enabled = false,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = Color(0xFFE2E8F0).bg(),
                disabledContentColor = Color(0xFF64748B).fg()
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = when (job.status.lowercase()) {
                    "filled" -> stringResource(R.string.job_desc_position_filled)
                    "deleted" -> stringResource(R.string.job_desc_job_removed_status)
                    "expired" -> stringResource(R.string.job_desc_job_expired)
                    else -> stringResource(R.string.job_desc_job_closed)
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    } else {
        Button(
            onClick = {
                if (currentUser == null) {
                    onLoginRequired("apply")
                } else {
                    onApplyClick()
                }
            },
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(stringResource(R.string.apply_now), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}


@Composable
private fun JobDetailsContent(
    job: JobListing,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    similarJobs: List<JobListing> = emptyList(),
    savedJobIds: Set<String> = emptySet(),
    onSimilarJobSaveToggle: (String, Boolean) -> Unit = { _, _ -> },
    onReportClick: () -> Unit = {},
    onReportNonPaymentClick: () -> Unit = {},
    hasApplied: Boolean = false,
    onJobClick: (String) -> Unit = {},
    inlineActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val heroImageUrl = remember(job.photoUrl) {
        job.photoUrl?.takeIf { it.isNotBlank() }
    }

    // Fullscreen image preview state. Tapping the hero image opens the
    // viewer; tapping the viewer or pressing back closes it.
    var showFullscreenImage by remember(heroImageUrl) { mutableStateOf(false) }
    BackHandler(enabled = showFullscreenImage) { showFullscreenImage = false }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        // Bug #10 fix: leave room at the bottom so the report banner and
        // similar-jobs cards stay fully visible above the sticky
        // BottomActionBar (Call + Apply, ~74dp tall) instead of being
        // hidden behind it when the user scrolls to the end.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (!heroImageUrl.isNullOrBlank()) {
            item {
                var isHeroImageLoading by remember(heroImageUrl) { mutableStateOf(true) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        // Tap to open fullscreen viewer.
                        .clickable { showFullscreenImage = true },
                    // Neutral background so portrait/landscape uploads sit on
                    // a clean surface when ContentScale.Fit leaves bars
                    // around the edges.
                    colors = CardDefaults.cardColors(containerColor = WorkerColors.ChipBackground),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(0.5.dp, WorkerColors.Border)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(heroImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Job image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp)),
                            // Show the full uploaded image — every corner —
                            // instead of cropping the edges. Matches what the
                            // employer sees in the preview screen.
                            contentScale = ContentScale.Fit,
                            onLoading = { isHeroImageLoading = true },
                            onSuccess = { isHeroImageLoading = false },
                            onError = { isHeroImageLoading = false }
                        )
                        if (isHeroImageLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(WorkerColors.ChipBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // Location Section removed from top - now in Job Details Card

        // ACCESSIBILITY: Landmark Navigation - removed in optimization
        // if (job.landmark.isNotBlank()) {
        //     item {
        //         Card(
        //             modifier = Modifier.fillMaxWidth(),
        //             colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        //             shape = RoundedCornerShape(8.dp),
        //             elevation = CardDefaults.cardElevation(0.dp)
        //         ) {
        //             Row(
        //                 modifier = Modifier.fillMaxWidth().padding(12.dp),
        //                 verticalAlignment = Alignment.CenterVertically
        //             ) {
        //                 Text("🏛️", style = MaterialTheme.typography.bodyMedium)
        //                 Spacer(modifier = Modifier.width(8.dp))
        //                 Text(stringResource(R.string.near_label), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF166534)))
        //                 Text(job.landmark, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF166534)))
        //             }
        //         }
        //     }
        // }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Stitch design spec: job header card — title, employer + Verified
        // Employer, and a relative "posted X ago" timestamp. All real job
        // fields (job.title, job.companyName, job.isVerified, job.createdAt).
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = job.title.ifBlank { stringResource(R.string.job_details) },
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = WorkerColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    if (job.companyName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = job.companyName,
                                style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary, fontSize = 14.sp)
                            )
                        }
                    }
                    if (job.createdAt > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = com.example.dutype.utils.DateTimeUtils.formatTimeAgoExactDays(job.createdAt),
                            style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextTertiary, fontSize = 12.sp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Stitch design spec: 2x2 stat grid — SALARY / TYPE / DISTANCE / START.
        // Salary, type and distance are real job fields; "Join Today" is
        // fixed design copy since the data model has no per-job "start
        // availability" field yet.
        item {
            val statSalaryValue = job.payText
            val statTypeValue = com.example.dutype.employer.models.EmploymentType.fromKey(job.employmentType).displayName
            val statDistanceValue = when {
                job.distance == null || job.distance!! <= 0 -> stringResource(R.string.nearby)
                job.distance!! < 1.0 -> stringResource(R.string.job_desc_distance_m_away, (job.distance!! * 1000).toInt())
                else -> stringResource(R.string.job_desc_distance_km_away, "%.1f".format(job.distance))
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    JobStatCard(label = stringResource(R.string.job_desc_stat_salary), value = statSalaryValue, modifier = Modifier.weight(1f))
                    JobStatCard(label = stringResource(R.string.job_desc_stat_type), value = statTypeValue, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    JobStatCard(label = stringResource(R.string.job_desc_stat_distance), value = statDistanceValue, modifier = Modifier.weight(1f))
                    JobStatCard(label = stringResource(R.string.job_desc_stat_start), value = stringResource(R.string.job_desc_stat_join_today), modifier = Modifier.weight(1f))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Combined Job Details Card - White background with light border (like worker job cards)
        item {
            val payAmount = job.payText

            // Job type (Full-time / Part-time / etc.).
            val displayLocation = job.addressText.ifBlank { job.area }
            val jobTypeDisplay = com.example.dutype.employer.models.EmploymentType.fromKey(job.employmentType).displayName
            val shiftTimingDisplay = com.example.dutype.employer.models.JobShift.fromKey(job.shift).displayName
            val experienceDisplay = job.experienceRequired.ifBlank { "Not specified" }
            // Employer joined time should be fetched from employer profile if needed

            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Company Name - show at top of job details
                    if (job.companyName.isNotEmpty()) {
                        JobDetailRow(Icons.Filled.Business, Color(0xFF7C3AED), "Company:", job.companyName)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Location - Column format: location on one line, distance below
                    val distanceText = when {
                        job.distance == null || job.distance!! <= 0 -> stringResource(R.string.distance_unavailable)
                        job.distance!! < 0.05 -> stringResource(R.string.less_than_50m)
                        job.distance!! < 1.0 -> "${(job.distance!! * 1000).toInt()}m ${stringResource(R.string.away)}"
                        job.distance!! < 2.0 -> String.format("%.1f km ${stringResource(R.string.walkable)}", job.distance)
                        else -> String.format("%.1f km ${stringResource(R.string.away)}", job.distance)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = WorkerColors.Error, // Bright red for location
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            // Location on one line
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    "Location:",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = WorkerColors.TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    displayLocation.ifBlank { "Not specified" },
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                                    ),
                                    softWrap = true
                                )
                            }
                            // Distance on next line
                            if (distanceText != stringResource(R.string.distance_unavailable)) {
                                Text(
                                    distanceText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = WorkerColors.TextSecondary,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Salary/Pay
                    JobDetailRow(Icons.Default.Payments, WorkerColors.Success, "Salary:", payAmount)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Vacancies — not in schema, removed

                    // Experience
                    JobDetailRow(Icons.Default.Star, Color(0xFFFBBF24), "Experience:", experienceDisplay)
                    Spacer(modifier = Modifier.height(10.dp))

                    JobDetailRow(
                        Icons.Outlined.WorkOutline,
                        WorkerColors.Primary,
                        "Education:",
                        job.educationRequired.ifBlank { stringResource(R.string.job_desc_no_qualification) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Job Type (Full-time / Part-time)
                    JobDetailRow(Icons.Default.Schedule, Color(0xFF06B6D4), "Job Type:", jobTypeDisplay)
                    Spacer(modifier = Modifier.height(10.dp))

                    JobDetailRow(Icons.Default.AccessTime, Color(0xFF6366F1), "Shift:", shiftTimingDisplay)

                    // Category row removed — redundant with the Job Type row above.

                    if (job.vacancies > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        JobDetailRow(Icons.Default.People, Color(0xFF8B5CF6), "Vacancies:", job.vacancies.toString())
                    }

                    // Always show gender so workers can see preference even when it's "Any".
                    Spacer(modifier = Modifier.height(10.dp))
                    JobDetailRow(
                        Icons.Default.Person,
                        Color(0xFFEC4899),
                        "Gender:",
                        job.gender.ifBlank { stringResource(R.string.all) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // "About this Job" card — Stitch design spec title + small filled
        // circle bullets. Bullet content is still the job's real description
        // (parseDescriptionToBullets(job.description)); the timestamp moved
        // up into the new job header card so it isn't shown twice.
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.job_desc_about_job),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description bullet points — small green/teal filled circle bullets
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        parseDescriptionToBullets(job.description).forEach { point ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp)
                                        .size(6.dp)
                                        .background(WorkerColors.Success, CircleShape)
                                        )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(point, style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary, lineHeight = 22.sp))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // "Job Location" card — Stitch design spec: title, a compact real map
        // preview (reuses the app's GoogleMapView, centered on job.lat/lng —
        // not a fake placeholder box) and a "Get Directions →" link that opens
        // Google Maps navigation to the job's real coordinates.
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.auto_job_location),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = WorkerColors.TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (job.lat != 0.0 || job.lng != 0.0) {
                        com.example.dutype.worker.components.JobLocationPreview(
                            jobId = job.id,
                            lat = job.lat,
                            lng = job.lng,
                            placeLabel = job.area.ifBlank { job.district },
                            distanceKm = null
                        )
                    } else {
                        Text(
                            text = job.addressText.ifBlank { job.area }.ifBlank { stringResource(R.string.job_desc_location_not_available) },
                            style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // DutyPe Safety Banner - Light sky blue background
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WorkerColors.InfoLight),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Shield, null, tint = WorkerColors.Primary.fg(), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dont_pay_fee_for_jobs), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = WorkerColors.Info))
                        Text(stringResource(R.string.report_suspicious_jobs), style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.Primary.fg()))
                    }
                    TextButton(
                        onClick = onReportClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.report), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = WorkerColors.Error))
                    }
                }
            }
        }

        // Only shown to workers who applied.
        if (hasApplied) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                TextButton(
                    onClick = onReportNonPaymentClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.MoneyOff,
                        contentDescription = null,
                        tint = WorkerColors.Error,
                        modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.report_non_payment_action),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WorkerColors.Error
                        )
                    )
                }
            }
        }

        // Inline Call + Apply action row — appears directly after the
        // "don't pay fee" safety banner so the user has a natural,
        // non-floating touchpoint while they're reading the details.
        // Paired with a divider above it for visual separation. The
        // sticky BottomActionBar is hidden while this row is on screen
        // (see `inlineActionsVisible` in the outer screen) so the user
        // never sees two duplicated action bars at once.
        if (inlineActions != null) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                HorizontalDivider(color = WorkerColors.Border, thickness = 1.dp)
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item(key = "inline_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    inlineActions()
                }
            }
        }

        // Similar Jobs Section
        if (similarJobs.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            // Solid blue tile (no gradient).
                            .background(
                                color = WorkerColors.InfoLight,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.WorkOutline,
                            contentDescription = null,
                            tint = WorkerColors.Primary.fg(),
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.auto_similar_jobs),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = WorkerColors.TextPrimary
                            )
                        )
                        Text(
                            text = stringResource(R.string.auto_roles_related_to_this_opening),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = WorkerColors.TextSecondary
                            )
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }

            items(similarJobs.size) { index ->
                val similarJob = similarJobs[index]
                JobCard(
                    job = similarJob,
                    isSaved = similarJob.id in savedJobIds,
                    onSaveClick = { toggledJobId ->
                        onSimilarJobSaveToggle(toggledJobId, toggledJobId !in savedJobIds)
                    },
                    onCardClick = onJobClick,
                    modifier = Modifier.fillMaxWidth()
                )

                if (index < similarJobs.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Fullscreen image preview overlay. Sits above the LazyColumn so it
        // covers the entire screen (including any sticky bottom bar). Tap
        // anywhere or press back to dismiss.
        if (showFullscreenImage && !heroImageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.bg().copy(alpha = 0.96f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { showFullscreenImage = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(heroImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Job image fullscreen",
                    modifier = Modifier.fillMaxSize(),
                    // Fit so every corner of the image is visible \u2014 no edge
                    // cropping in the fullscreen view either.
                    contentScale = ContentScale.Fit
                )

                // Small close affordance in the top-right so users discover
                // the dismiss gesture even without the back button.
                IconButton(
                    onClick = { showFullscreenImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.bg().copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close image",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun JobDetailRow(icon: ImageVector, iconColor: Color, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            null,
            tint = iconColor,
            modifier = Modifier.size(22.dp) // Increased from IconSizes.Standard (20dp) to 22dp for better visibility
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = WorkerColors.TextSecondary,
                fontWeight = FontWeight.Medium // Added Medium weight for better readability
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
            ),
            softWrap = true
        )
    }
}

/**
 * Small white rounded stat card for the SALARY/TYPE/DISTANCE/START 2x2 grid
 * on the job detail screen — tiny gray uppercase label + bold dark value.
 */
@Composable
private fun JobStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(WorkerColors.CardBackground)
            .border(1.dp, WorkerColors.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = WorkerColors.TextTertiary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = WorkerColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun parseDescriptionToBullets(description: String): List<String> {
    val lines = description.replace("•", "\n").replace("-", "\n").replace("*", "\n").split("\n").map { it.trim() }.filter { it.isNotEmpty() && it.length > 3 }
    if (lines.size > 1) return lines.take(6)
    val sentences = description.split(".").map { it.trim() }.filter { it.isNotEmpty() && it.length > 10 }
    return if (sentences.isNotEmpty()) sentences.take(6) else listOf(description)
}

@Composable
private fun JobDescriptionLoadingContent() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ShimmerBox(height = 22.dp, width = 230.dp) }
        item { ShimmerBox(height = 14.dp, width = 150.dp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) { ShimmerBox(modifier = Modifier.weight(1f), height = 64.dp) }
            }
        }
        item { com.example.dutype.components.JobCardShimmer() }
        item { ShimmerBox(height = 170.dp) }
        item { ShimmerBox(height = 56.dp) }
    }
}

@Composable
private fun JobDescriptionErrorContent(
    jobGone: Boolean,
    onRetry: () -> Unit,
    onFindJobs: () -> Unit,
    onBack: () -> Unit
) {
    if (jobGone) {
        // Filled, closed or removed: point the worker at the other jobs instead of a dead end.
        com.example.dutype.components.DutyPeEmptyScreen(
            icon = Icons.Default.WorkOff,
            tone = com.example.dutype.components.EmptyTone.PURPLE,
            art = com.example.dutype.components.EmptyArt.SEARCH,
            title = stringResource(R.string.job_gone_title),
            message = stringResource(R.string.job_gone_body),
            primary = com.example.dutype.components.EmptyStateAction(
                label = stringResource(R.string.job_gone_find_more),
                icon = Icons.Default.Search,
                onClick = onFindJobs
            ),
            secondary = com.example.dutype.components.EmptyStateAction(label = stringResource(R.string.go_back), onClick = onBack)
        )
    } else {
        com.example.dutype.components.DutyPeEmptyScreen(
            icon = Icons.Default.CloudOff,
            tone = com.example.dutype.components.EmptyTone.ORANGE,
            art = com.example.dutype.components.EmptyArt.OFFLINE,
            title = stringResource(R.string.job_load_failed_title),
            message = stringResource(R.string.job_load_failed_body),
            primary = com.example.dutype.components.EmptyStateAction(
                label = stringResource(R.string.try_again_button),
                icon = Icons.Default.Refresh,
                onClick = onRetry
            ),
            secondary = com.example.dutype.components.EmptyStateAction(label = stringResource(R.string.go_back), onClick = onBack)
        )
    }
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier, width: androidx.compose.ui.unit.Dp? = null, height: androidx.compose.ui.unit.Dp = 16.dp) {
    val shimmerColors = listOf(WorkerColors.ChipBackground, WorkerColors.CardBackground, WorkerColors.ChipBackground)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "shimmer")
    val brush = Brush.linearGradient(colors = shimmerColors, start = Offset(translateAnim.value - 300f, translateAnim.value - 300f), end = Offset(translateAnim.value, translateAnim.value))
    Box(modifier = modifier.background(brush, RoundedCornerShape(8.dp)).let { if (width != null) it.width(width) else it.fillMaxWidth() }.height(height))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JobDescriptionScreenPreview() {
    JobDescriptionScreen(navController = rememberNavController(), jobId = "sample_job_id")
}
