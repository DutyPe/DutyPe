package com.example.dutype.worker.screens

import android.app.Activity
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    // Guest mode - Login bottom sheet state
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // "apply", "call", "message", "whatsapp"

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
        animationSpec = tween(600, easing = EaseInOutQuart),
        label = "contentAlpha"
    )

    LaunchedEffect(jobId, retryTrigger) {
        if (jobId.isNotEmpty()) {
            isLoading = true
            error = null
            try {
                val result = jobViewModel.getJobById(jobId)
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
                        isLoading = false
                    },
                    onFailure = { exception ->
                        error = exception.message ?: "Failed to load job details"
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                error = e.message ?: "Failed to load job details"
                isLoading = false
            }
        }
    }

    LaunchedEffect(job?.id, job?.jobType, currentLocation?.latitude, currentLocation?.longitude) {
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
                title = stringResource(R.string.job_details),
                onBackClick = { handleBackNavigation() },
                showBackButton = true,
                backgroundColor = WorkerColors.CardBackground,
                titleColor = Color.Black,
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
                                tint = if (isSaved) Color(0xFFEF4444) else Color(0xFF6B7280),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    job?.let { currentJob ->
                        ShareJobIconButton(
                            job = currentJob,
                            tint = Color(0xFF6B7280)
                        )
                    }
                }
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFFE5E7EB)
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
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)), shape = RoundedCornerShape(12.dp)) {
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
                    val phone = job?.contactNumber ?: ""
                    if (phone.isNotEmpty()) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply { 
                            data = android.net.Uri.parse("tel:$phone") 
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to start dialer for $phone")
                            android.widget.Toast.makeText(context, "No dialer app available", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                "message" -> {
                    // Chat feature removed - default to call
                    val phone = job?.contactNumber ?: ""
                    if (phone.isNotEmpty()) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply { 
                            data = android.net.Uri.parse("tel:$phone") 
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to start dialer for $phone")
                            android.widget.Toast.makeText(context, "No dialer app available", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                "whatsapp" -> {
                    val phone = job?.contactNumber ?: ""
                    if (phone.isNotEmpty()) {
                        com.example.dutype.components.openWhatsAppApply(
                            context = context,
                            phoneNumber = phone,
                            jobTitle = job?.title ?: "",
                            companyName = job?.companyName ?: "",
                            salary = job?.let { j ->
                                "₹${com.example.dutype.utils.SalaryFormatter.display(j.salary, j.salaryType)}"
                            } ?: "",
                            location = job?.addressText ?: ""
                        )
                    }
                }
            }
            pendingAction = null
        },
        onProfileSetupRequired = {
            // For job application - navigate to profile setup with return route to job application
            showLoginBottomSheet = false
            android.widget.Toast.makeText(context, "Please complete your profile to apply", android.widget.Toast.LENGTH_SHORT).show()
            // Navigate to profile setup with return route to job application screen
            navController.navigate(Routes.profileSetupWithReturnRoute(Routes.jobApplicationRoute(jobId)))
            pendingAction = null
        },
        requiresProfileCheck = pendingAction == "apply", // Only check profile for job applications
        role = com.example.dutype.models.UserRole.WORKER,
        title = stringResource(R.string.login_to_continue),
        subtitle = when (pendingAction) {
            "apply" -> "Login to apply for this job"
            "save" -> "Login to save this job"
            "call" -> "Login to call the employer"
            "message" -> "Login to message the employer"
            "whatsapp" -> "Login to contact via WhatsApp"
            else -> "Please login to continue"
        }
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
    onApplyClick: () -> Unit,
    onLoginRequired: (String) -> Unit,
) {
    // Call Button - Half width with icon and text
    OutlinedButton(
        onClick = {
            if (currentUser == null) {
                onLoginRequired("call")
            } else {
                val phone = job.contactNumber
                if (phone.isNotEmpty()) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:$phone") }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to start dialer for $phone")
                        android.widget.Toast.makeText(context, "No dialer app available", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, "Contact number not available", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = Modifier.weight(1f).height(50.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(Icons.Default.Phone, null, tint = Color.Black, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.call), color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                    "ACCEPTED" -> Color(0xFF10B981)
                    "PENDING", "UNDER_REVIEW" -> Color(0xFFF59E0B)
                    else -> Color(0xFF6B7280)
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
                        "ACCEPTED" -> "Hired!"
                        "PENDING" -> "Applied"
                        "UNDER_REVIEW" -> "Under Review"
                        else -> "Applied"
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
            shape = RoundedCornerShape(8.dp)
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
    onJobClick: (String) -> Unit = {},
    inlineActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        // Bug #10 fix: leave room at the bottom so the report banner and
        // similar-jobs cards stay fully visible above the sticky
        // BottomActionBar (Call + Apply, ~74dp tall) instead of being
        // hidden behind it when the user scrolls to the end.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Job Image Section - Removed in optimization (jobImageUrl field no longer exists)
        // if (job.jobImageUrl.isNotBlank()) {
        //     item {
        //         Card(
        //             modifier = Modifier.fillMaxWidth(),
        //             colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
        //             shape = RoundedCornerShape(12.dp),
        //             elevation = CardDefaults.cardElevation(2.dp)
        //         ) {
        //             AsyncImage(
        //                 model = ImageRequest.Builder(context)
        //                     .data(job.jobImageUrl)
        //                     .crossfade(true)
        //                     .build(),
        //                 contentDescription = "Job image",
        //                 modifier = Modifier
        //                     .fillMaxWidth()
        //                     .height(180.dp)
        //                     .clip(RoundedCornerShape(12.dp)),
        //                 contentScale = ContentScale.Crop
        //             )
        //         }
        //     }
        //     
        //     item { Spacer(modifier = Modifier.height(12.dp)) }
        // }
        
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
            val paymentCycle = when (job.salaryType.uppercase()) {
                "HOURLY" -> "Hourly"
                "MONTHLY" -> "Monthly"
                else -> "Daily"
            }
            val payAmount = salaryStr
            
            // Working hours — not in schema, show N/A
            val displayLocation = job.addressText.ifBlank { job.location }
            val workingHoursDisplay = job.workingHours.ifBlank { "Not specified" }
            val experienceDisplay = job.experienceRequired.ifBlank { "Not specified" }
            val shiftTimingDisplay = job.shiftTiming.ifBlank { "Not specified" }
            // Employer joined time should be fetched from employer profile if needed
            
            Card(
                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Job Title - Prominent display at top
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 20.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
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
                            tint = Color(0xFFEF4444), // Bright red for location
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            // Location on one line
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Location:", 
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF6B7280),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    displayLocation.ifBlank { "Not specified" },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold, 
                                        color = Color.Black
                                    )
                                )
                            }
                            // Distance on next line
                            if (distanceText != stringResource(R.string.distance_unavailable)) {
                                Text(
                                    distanceText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF6B7280),
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Posted time
                    if (job.createdAt > 0) {
                        JobDetailRow(Icons.Default.AccessTime, Color(0xFF3B82F6), "Posted:", com.example.dutype.utils.DateTimeUtils.formatTimeAgoExactDays(job.createdAt))
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    
                    // Salary/Pay
                    JobDetailRow(Icons.Default.Payments, Color(0xFF10B981), "Salary:", if (payAmount != "Not specified") "₹$payAmount $payTypeDisplay" else payAmount)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Job Type
                    JobDetailRow(Icons.Filled.Work, Color(0xFFF59E0B), "Job Type:", job.jobType.ifEmpty { "Not specified" })
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Vacancies — not in schema, removed
                    
                    // Experience
                    JobDetailRow(Icons.Default.Star, Color(0xFFFBBF24), "Experience:", experienceDisplay)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Shift timing
                    JobDetailRow(Icons.Default.Schedule, Color(0xFF0EA5E9), "Shift:", shiftTimingDisplay)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Working Hours
                    JobDetailRow(Icons.Default.Schedule, Color(0xFF06B6D4), "Working Hours:", workingHoursDisplay)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Payment Cycle
                    JobDetailRow(Icons.Filled.CalendarToday, Color(0xFFF472B6), "Payment Cycle:", paymentCycle)

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
                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Job Description Section
                    Text(stringResource(R.string.job_description_label), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Description bullet points - Black bullets
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        parseDescriptionToBullets(job.description).forEach { point ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("-", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), fontWeight = FontWeight.Bold, fontSize = 16.sp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(point, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), lineHeight = 22.sp))
                            }
                        }
                    }

                    if (job.benefits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Benefits",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            job.benefits.forEach { benefit ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("-", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), fontWeight = FontWeight.Bold, fontSize = 16.sp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(benefit, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), lineHeight = 22.sp))
                                }
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF8FF)),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Shield, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dont_pay_fee_for_jobs), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF1E40AF)))
                        Text(stringResource(R.string.report_suspicious_jobs), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF3B82F6)))
                    }
                    TextButton(
                        onClick = onReportClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.report), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626)))
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
                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
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
                                color = Color(0xFFDBEAFE),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.WorkOutline,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Similar jobs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        )
                        Text(
                            text = "Roles related to this opening",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B)
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
}

@Composable
private fun JobDetailRow(icon: ImageVector, iconColor: Color, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Medium // Added Medium weight for better readability
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            value, 
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold, 
                color = Color.Black
            )
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
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(32.dp)) {
                Box(modifier = Modifier.size(80.dp).scale(errorAnimation).background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Error, "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.ExtraLarge))
                }
                Text(stringResource(R.string.oops_something_wrong), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)), textAlign = TextAlign.Center)
                Text(error, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)), textAlign = TextAlign.Center)
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)), shape = RoundedCornerShape(12.dp)) {
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
    val shimmerColors = listOf(Color(0xFFF9FAFB), Color.White, Color(0xFFF9FAFB))
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
