package com.example.dutype.worker.screens

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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
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
import com.dutype.app.R
import com.example.dutype.ads.AdManager
import com.example.dutype.components.OfflineBanner
import com.example.dutype.components.ShareJobIconButton
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobListing
import com.example.dutype.models.parseTrustTier
import com.example.dutype.navigation.Routes
import com.example.dutype.services.JobAvailabilityFeedback
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.viewmodels.ConnectivityViewModel
import com.example.dutype.viewmodels.FirestoreJobViewModel
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
    onStatusBarColorChange: (Color) -> Unit = {},
    adManager: AdManager? = null // DISABLED: Ads temporarily disabled
) {
    val context = LocalContext.current
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
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

    // DISABLED: Ads temporarily disabled
    /*
    // Get AdManager from Hilt via ViewModel's injection (singleton instance)
    // This ensures we use the same AdManager that was initialized in Application.onCreate()
    val adManagerInstance = adManager ?: smartApplicationViewModel.adManager

    // Collect ad ready state
    val isAdReady by adManagerInstance.isInterstitialReady.collectAsState()

    // Preload interstitial ad when screen loads
    LaunchedEffect(Unit) {
        Timber.d("📺 JobDescriptionScreen: Loading interstitial ad... (currently ready: $isAdReady)")
        adManagerInstance.loadInterstitialAd(context)
    }
    */

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

    // ReportingService accessed via SmartJobApplicationViewModel (proper DI pattern)
    val reportingService = smartApplicationViewModel.reportingService
    val jobCallFeedbackService = smartApplicationViewModel.jobCallFeedbackService
    var pendingCallFeedbackJob by remember { mutableStateOf<JobListing?>(null) }
    var showCallFeedbackSheet by remember { mutableStateOf(false) }
    val callLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (pendingCallFeedbackJob != null) {
            showCallFeedbackSheet = true
        }
    }
    val launchEmployerDialer: (JobListing) -> Unit = { currentJob ->
        val phone = currentJob.contactNumber.trim()
        if (phone.isNotEmpty()) {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:$phone")
            }
            try {
                pendingCallFeedbackJob = currentJob
                callLauncher.launch(intent)
            } catch (e: Exception) {
                pendingCallFeedbackJob = null
                Timber.e(e, "Failed to start dialer for $phone")
                android.widget.Toast.makeText(context, context.getString(R.string.no_dialer_app_available), android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, context.getString(R.string.contact_number_not_available), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Guest mode - Login bottom sheet state
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) }

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
            snackbarMessage = "Application submitted successfully!"
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
                // Apr 2026 fast-path: collect the live Flow so the cached
                // JobListing renders INSTANTLY on screen entry, then the
                // fresh Firestore copy quietly replaces it once the network
                // fetch completes. The user no longer sees a blank loading
                // state on jobs they just scrolled past or recently viewed.
                jobViewModel.getJobByIdLive(jobId).collect { result ->
                    result.fold(
                        onSuccess = { fetchedJob ->
                            val jobWithDistance = if (fetchedJob != null &&
                                currentLocation != null &&
                                (currentLocation!!.latitude != 0.0 || currentLocation!!.longitude != 0.0) &&
                                (fetchedJob.lat != 0.0 || fetchedJob.lng != 0.0)) {
                                val distance = jobViewModel.locationService.calculateDistance(
                                    currentLocation!!.latitude, currentLocation!!.longitude,
                                    fetchedJob.lat, fetchedJob.lng
                                )
                                fetchedJob.copy(distance = distance)
                            } else fetchedJob
                            job = jobWithDistance
                            // Drop the loading state on the FIRST emission
                            // (cached or network) so the description and
                            // benefits surface immediately.
                            isLoading = false
                        },
                        onFailure = { exception ->
                            // If we already have a job from a cache emission,
                            // keep showing it; only set the error state on
                            // hard failure with no fallback.
                            if (job == null) {
                                error = exception.message ?: "Failed to load job details"
                            }
                            isLoading = false
                        }
                    )
                }
            } catch (e: Exception) {
                if (job == null) {
                    error = e.message ?: "Failed to load job details"
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(job?.id, currentLocation?.latitude, currentLocation?.longitude) {
        val currentJob = job ?: return@LaunchedEffect
        similarJobs = try {
            jobViewModel.getRecommendedJobsForJob(currentJob, limit = 5)
                .getOrElse { exception ->
                    Timber.w(exception, "JobDescriptionScreen: Failed to load recommended jobs for ${currentJob.id}")
                    emptyList()
                }
        } catch (e: Exception) {
            Timber.w(e, "JobDescriptionScreen: Non-critical recommended jobs load failure")
            emptyList()
        }
    }

    // Function to handle back navigation - DISABLED: Ads temporarily disabled
    val handleBackNavigation: () -> Unit = {
        // DISABLED: Ad code commented out
        /*
        Timber.d("📺 Back pressed - Ad ready state: $isAdReady")
        val activity = context as? Activity
        if (activity != null) {
            adManagerInstance.showInterstitialAd(
                activity = activity,
                onAdDismissed = {
                    Timber.d("📺 Ad dismissed, navigating back")
                    navController.popBackStack()
                },
                onAdNotReady = {
                    Timber.d("📺 Ad not ready (isAdReady=$isAdReady), navigating back directly")
                    navController.popBackStack()
                }
            )
        } else {
            Timber.d("📺 Activity is null, navigating back directly")
            navController.popBackStack()
        }
        */
        // Direct navigation without ads
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

            // Header - Using CommonHeader for consistency
            com.example.dutype.components.CommonHeader(
                title = job?.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.job_details),
                onBackClick = { handleBackNavigation() },
                showBackButton = true,
                backgroundColor = WorkerColors.CardBackground,
                titleColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                actions = {
                    // Save/Favorite Button
                    job?.let {
                        IconButton(
                            onClick = {
                                if (currentUser == null) {
                                    pendingAction = "save"
                                    showLoginBottomSheet = true
                                } else {
                                    if (isSaved) {
                                        savedJobsViewModel.unsaveJob(resolvedJobId)
                                        snackbarMessage = "Job removed from saved!"
                                    } else {
                                        savedJobsViewModel.saveJob(resolvedJobId)
                                        snackbarMessage = "Job saved!"
                                    }
                                    showSnackbar = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isSaved) "Remove from saved" else "Save job",
                                tint = if (isSaved) WorkerColors.Error else WorkerColors.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    job?.let { currentJob ->
                        ShareJobIconButton(
                            job = currentJob,
                            tint = WorkerColors.TextSecondary
                        )
                    }
                }
            )

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
                    error != null -> JobDescriptionErrorContent(error!!) { retryTrigger++ }
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
                reportingService.reportJob(jobId, reportType, description)
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
                                snackbarMessage = "Job saved!"
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

    if (showCallFeedbackSheet && pendingCallFeedbackJob != null) {
        JobCallFeedbackSheet(
            jobTitle = pendingCallFeedbackJob?.title.orEmpty(),
            companyName = pendingCallFeedbackJob?.companyName.orEmpty(),
            onDismiss = {
                showCallFeedbackSheet = false
                pendingCallFeedbackJob = null
            },
            onSubmit = { spokeWithEmployer, availability ->
                val feedbackJob = pendingCallFeedbackJob
                if (feedbackJob == null) {
                    Result.failure(Exception(context.getString(R.string.job_not_found)))
                } else {
                    jobCallFeedbackService.submitCallFeedback(
                        job = feedbackJob,
                        spokeWithEmployer = spokeWithEmployer,
                        availability = availability
                    )
                }
            },
            onSubmitted = {
                showCallFeedbackSheet = false
                pendingCallFeedbackJob = null
                snackbarMessage = context.getString(R.string.job_fresh_update_saved)
                showSnackbar = true
            }
        )
    }
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
    onCallClick: (JobListing) -> Unit = {},
    onApplyClick: () -> Unit = {},
    onLoginRequired: (String) -> Unit = {} // Callback for guest mode login
) {
    Column(modifier = Modifier.fillMaxWidth().background(WorkerColors.CardBackground)) {
        // Action Buttons - Call and Apply (half-half width)
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionButtonsContent(
                job = job,
                currentUser = currentUser,
                context = context,
                navController = navController,
                hasApplied = hasApplied,
                applicationStatus = applicationStatus,
                onCallClick = onCallClick,
                onApplyClick = onApplyClick,
                onLoginRequired = onLoginRequired,
            )
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
        modifier = Modifier.weight(1f).height(50.dp),
        shape = RoundedCornerShape(8.dp),
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
                navController.navigate(com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS)
            },
            modifier = Modifier.weight(1f).height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (applicationStatus) {
                    "ACCEPTED" -> WorkerColors.Success
                    "PENDING", "UNDER_REVIEW" -> WorkerColors.Warning
                    else -> WorkerColors.Primary
                }
            ),
            shape = RoundedCornerShape(8.dp)
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
    } else {
        Button(
            onClick = {
                if (currentUser == null) {
                    onLoginRequired("apply")
                } else {
                    onApplyClick()
                }
            },
            modifier = Modifier.weight(1f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(R.string.apply_now), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobCallFeedbackSheet(
    jobTitle: String,
    companyName: String,
    onDismiss: () -> Unit,
    onSubmit: suspend (Boolean, JobAvailabilityFeedback) -> Result<Unit>,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var spokeWithEmployer by remember { mutableStateOf<Boolean?>(null) }
    var availability by remember { mutableStateOf<JobAvailabilityFeedback?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WorkerColors.CardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(WorkerColors.Primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = WorkerColors.Primary, modifier = Modifier.size(21.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.quick_call_update),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = WorkerColors.TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.quick_call_update_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = WorkerColors.TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss, enabled = !isSubmitting) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = WorkerColors.TextSecondary)
                }
            }

            if (jobTitle.isNotBlank() || companyName.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WorkerColors.ChipBackground),
                    elevation = CardDefaults.cardElevation(0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (jobTitle.isNotBlank()) {
                            Text(jobTitle, color = WorkerColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (companyName.isNotBlank()) {
                            Text(companyName, color = WorkerColors.TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.did_speak_employer),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = WorkerColors.TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FeedbackChoiceButton(
                    text = stringResource(R.string.yes_spoke),
                    selected = spokeWithEmployer == true,
                    onClick = { spokeWithEmployer = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                )
                FeedbackChoiceButton(
                    text = stringResource(R.string.no_answer),
                    selected = spokeWithEmployer == false,
                    onClick = { spokeWithEmployer = false },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                )
            }

            Text(
                text = stringResource(R.string.job_still_available_question),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = WorkerColors.TextSecondary
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                JobAvailabilityFeedback.entries.forEach { option ->
                    FeedbackChoiceButton(
                        text = jobAvailabilityFeedbackLabel(option),
                        selected = availability == option,
                        onClick = { availability = option },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    )
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = WorkerColors.Error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    val spoke = spokeWithEmployer
                    val selectedAvailability = availability
                    if (spoke == null || selectedAvailability == null) return@Button
                    scope.launch {
                        isSubmitting = true
                        errorMessage = null
                        val result = onSubmit(spoke, selectedAvailability)
                        isSubmitting = false
                        result.fold(
                            onSuccess = { onSubmitted() },
                            onFailure = { error -> errorMessage = error.message ?: context.getString(R.string.save_feedback_failed) }
                        )
                    }
                },
                enabled = !isSubmitting && spokeWithEmployer != null && availability != null,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.submit_update), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun jobAvailabilityFeedbackLabel(option: JobAvailabilityFeedback): String = when (option) {
    JobAvailabilityFeedback.STILL_AVAILABLE -> stringResource(R.string.availability_still_available)
    JobAvailabilityFeedback.FILLED -> stringResource(R.string.availability_job_filled)
    JobAvailabilityFeedback.NOT_SURE -> stringResource(R.string.availability_not_sure)
}

@Composable
private fun FeedbackChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) WorkerColors.Primary else WorkerColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) WorkerColors.Primary.copy(alpha = 0.12f) else WorkerColors.CardBackground,
            contentColor = if (selected) Color(0xFF1D4ED8) else WorkerColors.TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        if (selected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    onJobClick: (String) -> Unit = {},
    inlineActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val heroImageUrl = remember(job.jobImageUrl) {
        job.jobImageUrl?.takeIf { it.isNotBlank() }
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
        
        // Combined Job Details Card - White background with light border (like worker job cards)
        item {
            // Get pay info from schema fields
            // Bug #6: salary is a free-form String now.
            val salaryStr = job.salary.ifBlank { "Negotiable" }
            val payTypeDisplay = when (job.salaryType.uppercase()) {
                "HOURLY" -> "per hour"
                "MONTHLY" -> "per month"
                else -> "per day"
            }
            val payAmount = salaryStr
            
            // Job type (Full-time / Part-time / etc.).
            val displayLocation = job.addressText.ifBlank { job.location }
            val jobTypeDisplay = job.jobType.ifBlank { "Not specified" }
            val shiftTimingDisplay = job.shiftTiming.ifBlank { "Not specified" }
            val experienceDisplay = job.experienceRequired.ifBlank { "Not specified" }
            // Employer joined time should be fetched from employer profile if needed
            
            Card(
                modifier = Modifier.fillMaxWidth().border(0.5.dp, WorkerColors.Border, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                shape = RoundedCornerShape(12.dp),
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
                    JobDetailRow(Icons.Default.Payments, WorkerColors.Success, "Salary:", if (payAmount != "Not specified") "₹$payAmount $payTypeDisplay" else payAmount)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Vacancies — not in schema, removed
                    
                    // Experience
                    JobDetailRow(Icons.Default.Star, Color(0xFFFBBF24), "Experience:", experienceDisplay)
                    Spacer(modifier = Modifier.height(10.dp))

                    JobDetailRow(
                        Icons.Outlined.WorkOutline,
                        WorkerColors.Primary,
                        "Education:",
                        job.educationRequired.ifBlank { "No qualification required" }
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
                        job.gender.ifBlank { "Any" }
                    )

                    // Posted time — shown last, after gender
                    if (job.createdAt > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        JobDetailRow(Icons.Default.AccessTime, WorkerColors.Primary, "Posted:", com.example.dutype.utils.DateTimeUtils.formatTimeAgoExactDays(job.createdAt))
                    }
                    
                    /* REMOVED: Employer Trust Section - employerTrustTier and employerCreatedAt no longer in JobListing model
                    // These fields should be fetched from employer profile if needed in the future
                            }
                        }
                    }
                    */
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // Job Description & Requirements Card - White background with light border
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(0.5.dp, WorkerColors.Border, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Job Description Section
                    Text(stringResource(R.string.job_description_label), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary))
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Description bullet points - Black bullets
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        parseDescriptionToBullets(job.description).forEach { point ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("-", style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(point, style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary, lineHeight = 22.sp))
                            }
                        }
                    }

                    // Requirements Section - REMOVED (requirements field no longer exists in JobListing)
                    // if (job.requirements.isNotEmpty()) {
                    //     Spacer(modifier = Modifier.height(16.dp))
                    //     Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                    //     Spacer(modifier = Modifier.height(16.dp))
                    //     
                    //     Text(stringResource(R.string.requirements), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                    //     
                    //     Spacer(modifier = Modifier.height(12.dp))
                    //     
                    //     // Requirements bullet points - Black bullets
                    //     Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    //         job.requirements.forEach { req ->
                    //             Row(modifier = Modifier.fillMaxWidth()) {
                    //                 Text("•", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    //                 Spacer(modifier = Modifier.width(10.dp))
                    //                 Text(req, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), lineHeight = 22.sp))
                    //             }
                    //         }
                    //     }
                    // }
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
                    Icon(Icons.Outlined.Shield, null, tint = WorkerColors.Primary, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dont_pay_fee_for_jobs), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = WorkerColors.Info))
                        Text(stringResource(R.string.report_suspicious_jobs), style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.Primary))
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
                            tint = WorkerColors.Primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Similar jobs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = WorkerColors.TextPrimary
                            )
                        )
                        Text(
                            text = "Roles related to this opening",
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
                    .background(Color.Black.copy(alpha = 0.96f))
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
                        .background(Color.Black.copy(alpha = 0.55f))
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

private fun parseDescriptionToBullets(description: String): List<String> {
    val lines = description.replace("•", "\n").replace("-", "\n").replace("*", "\n").split("\n").map { it.trim() }.filter { it.isNotEmpty() && it.length > 3 }
    if (lines.size > 1) return lines.take(6)
    val sentences = description.split(".").map { it.trim() }.filter { it.isNotEmpty() && it.length > 10 }
    return if (sentences.isNotEmpty()) sentences.take(6) else listOf(description)
}

@Composable
private fun JobDescriptionLoadingContent() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ShimmerBox(height = 100.dp) }
        item { ShimmerBox(height = 30.dp, width = 250.dp) }
        item { ShimmerBox(height = 50.dp) }
        item { ShimmerBox(height = 180.dp) }
        item { ShimmerBox(height = 60.dp) }
        item { ShimmerBox(height = 150.dp) }
    }
}

@Composable
private fun JobDescriptionErrorContent(error: String, onRetry: () -> Unit) {
    val errorAnimation by rememberInfiniteTransition(label = "error").animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "errorPulse"
    )

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, WorkerColors.Border)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(32.dp)) {
                Box(modifier = Modifier.size(80.dp).scale(errorAnimation).background(WorkerColors.Error.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Error, "Error", tint = WorkerColors.Error, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.ExtraLarge))
                }
                Text(stringResource(R.string.oops_something_wrong), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = com.example.dutype.ui.theme.WorkerColors.TextPrimary), textAlign = TextAlign.Center)
                Text(error, style = MaterialTheme.typography.bodyMedium.copy(color = WorkerColors.TextSecondary), textAlign = TextAlign.Center)
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Primary), shape = RoundedCornerShape(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                        Text(stringResource(R.string.try_again_button), color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
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
