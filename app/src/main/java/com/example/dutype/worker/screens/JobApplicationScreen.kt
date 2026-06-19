package com.example.dutype.worker.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.JobListing
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.findActivity
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.viewmodels.ProfileViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

/**
 * Job Application Screen - Review and Submit Application
 * 
 * Shows:
 * 1. Job summary at top
 * 2. Worker behavior tips before going to work
 * 3. Submit Application button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicationScreen(
    jobId: String,
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val applicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    
    // Get InAppReviewTriggerService from Hilt
    val reviewTriggerService = rememberInAppReviewTriggerService()
    
    val currentUser = FirebaseAuth.getInstance().currentUser
    val jobUiState by jobViewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val applicationUiState by applicationViewModel.uiState.collectAsStateWithLifecycle()
    
    // Find the job from loaded jobs or load it
    val job = remember(jobUiState.jobs, jobId) {
        jobUiState.jobs.find { it.id == jobId }
    }
    
    // Load job if not found
    LaunchedEffect(jobId) {
        onStatusBarColorChange(Color.White)
        // Load profile first
        profileViewModel.loadProfile()
        Timber.d("JobApplicationScreen: Loading profile for user ${currentUser?.uid}")
    }
    
    // Debug: Log profile state
    LaunchedEffect(profileUiState.user) {
        Timber.d("JobApplicationScreen: Profile loaded - fullName=${profileUiState.user?.fullName}, phone=${profileUiState.user?.phone}")
    }
    
    // Load job details
    var loadedJob by remember { mutableStateOf<JobListing?>(null) }
    LaunchedEffect(jobId, job) {
        if (job == null && loadedJob == null) {
            val result = jobViewModel.getJobById(jobId)
            result.onSuccess { fetchedJob ->
                loadedJob = fetchedJob
            }
        }
    }
    
    // Use either found job or loaded job
    val displayJob = job ?: loadedJob
    
    // Batch-l: drive an in-screen success view (animated check + CTA)
    // instead of toast-then-pop. We hold the success flag locally so that
    // clearing the VM state for the next apply doesn't immediately tear
    // down the success UI.
    var showSuccess by remember { mutableStateOf(false) }
    LaunchedEffect(applicationUiState.applicationSuccess) {
        if (applicationUiState.applicationSuccess) {
            showSuccess = true
            applicationViewModel.clearSuccessStates()
            context.findActivity()?.let { activity ->
                reviewTriggerService.onWorkerJobApplication(activity)
            }
        }
    }
    
    // Handle application error
    LaunchedEffect(applicationUiState.error) {
        applicationUiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            applicationViewModel.clearError()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        // Header
        CommonHeader(
            title = stringResource(R.string.apply_for_job),
            onBackClick = {
                if (showSuccess) {
                    // Treat back the same as the CTA so we don't strand the
                    // user on a stale apply form.
                    navigateToWorkerHome(navController)
                } else {
                    navController.popBackStack()
                }
            },
            backgroundColor = WorkerColors.CardBackground
        )

        when {
            showSuccess && displayJob != null -> {
                // Batch-l: in-screen success state replaces the prior
                // toast-then-popBackStack flow. Renders an animated check
                // tick and a single "Return to Home" CTA that pops the
                // whole apply / details stack back to the worker home.
                ApplicationSentSuccess(
                    jobTitle = displayJob.title,
                    canCallEmployer = displayJob.contactNumber.trim().isNotBlank(),
                    onCallEmployer = {
                        val phone = displayJob.contactNumber.trim()
                        if (phone.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.contact_number_not_available), Toast.LENGTH_SHORT).show()
                        } else {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }.onFailure {
                                Toast.makeText(context, context.getString(R.string.unable_to_open_dialer), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onViewMyJobs = { navigateToWorkerMyJobs(navController) },
                    onReturnHome = { navigateToWorkerHome(navController) }
                )
            }
            displayJob == null || profileUiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 132.dp)
                    ) {
                        // Job Summary Card
                        JobSummaryCard(job = displayJob)

                        Spacer(modifier = Modifier.height(12.dp))

                        WorkerWorkTipsSection()

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground,
                        shadowElevation = 8.dp
                    ) {
                        SubmitApplicationButton(
                            modifier = Modifier.padding(
                                top = 12.dp,
                                bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            ),
                            isSubmitting = applicationUiState.isApplying,
                            onSubmit = {
                                applicationViewModel.applyForJob(
                                    jobId = jobId,
                                    coverLetter = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerWorkTipsSection() {
    val tips = listOf(
        stringResource(R.string.apply_tip_confirm_details),
        stringResource(R.string.apply_tip_reach_on_time),
        stringResource(R.string.apply_tip_polite_work),
        stringResource(R.string.apply_tip_no_fee)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(0.5.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = WorkerColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.apply_work_tips_title),
                    style = AppTypography.sectionHeader.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = WorkerColors.TextPrimary,
                        fontSize = 15.sp
                    )
                )
            }

            tips.forEach { tip ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = WorkerColors.Success,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = tip,
                        style = AppTypography.bodyMedium.copy(
                            color = WorkerColors.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Batch-m fix: route the worker home navigation through the
 * WorkerNavGraph's actual start destination ("home" — see
 * `WorkerBottomRoutes.HOME`). The earlier code popped to
 * `Routes.WORKER_HOME` ("worker_home") which is a top-level role-graph
 * route NOT registered inside WorkerNavGraph, so the popBackStack call
 * silently no-op'd and the Return-to-Home button appeared dead.
 */
private fun navigateToWorkerHome(navController: NavController) {
    navController.navigate(com.example.dutype.navigation.WorkerBottomRoutes.HOME) {
        popUpTo(com.example.dutype.navigation.WorkerBottomRoutes.HOME) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
    }
}

private fun navigateToWorkerMyJobs(navController: NavController) {
    navController.navigate(com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS) {
        popUpTo(com.example.dutype.navigation.WorkerBottomRoutes.HOME) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
    }
}

@Composable
private fun ApplicationSentSuccess(
    jobTitle: String,
    canCallEmployer: Boolean,
    onCallEmployer: () -> Unit,
    onViewMyJobs: () -> Unit,
    onReturnHome: () -> Unit
) {
    // Animated scale-in for the check circle.
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }
    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )
    val tickAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 250),
        label = "tickAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(WorkerColors.Success.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(WorkerColors.Success),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(44.dp)
                        .scale(tickAlpha)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Application Sent!",
            style = AppTypography.cardTitle.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = WorkerColors.TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Your application for \"$jobTitle\" has been sent. Calling now gives you the fastest chance to confirm the work.",
            style = AppTypography.bodyMedium.copy(
                color = WorkerColors.TextSecondary,
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = if (canCallEmployer) onCallEmployer else onViewMyJobs,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canCallEmployer) WorkerColors.Success else WorkerColors.Primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (canCallEmployer) Icons.Default.Call else Icons.Default.Work,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (canCallEmployer) "Call employer now" else "View My Jobs",
                style = AppTypography.buttonMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = if (canCallEmployer) onViewMyJobs else onReturnHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (canCallEmployer) "View My Jobs" else "Return to Home",
                style = AppTypography.buttonMedium.copy(
                    color = WorkerColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun JobSummaryCard(job: JobListing) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(0.5.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section header
            Text(
                text = "Applying for",
                style = AppTypography.labelMedium.copy(
                    color = WorkerColors.TextSecondary,
                    fontSize = 12.sp
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Job image or icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WorkerColors.ChipBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = WorkerColors.TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = AppTypography.cardTitle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Job details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Location (from job_details.addressText — runtime only)
                if (job.addressText.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = WorkerColors.Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = job.addressText,
                            style = AppTypography.caption.copy(color = WorkerColors.TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Pay — Bug #6: salary is a String now.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val salaryStr = job.salary.ifBlank { "Negotiable" }
                    val period = when (job.salaryType.uppercase()) {
                        "HOURLY" -> "hour"
                        "MONTHLY" -> "month"
                        else -> "day"
                    }
                    Text(
                        text = "₹$salaryStr",
                        style = AppTypography.labelMedium.copy(
                            color = WorkerColors.Success,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "/$period",
                        style = AppTypography.caption.copy(color = WorkerColors.TextSecondary)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmitApplicationButton(
    modifier: Modifier = Modifier,
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = WorkerColors.Primary,
                disabledContainerColor = WorkerColors.TextDisabled
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSubmitting) {
                val pulse = rememberInfiniteTransition(label = "submit_check_pulse")
                val scale by pulse.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "submit_check_scale"
                )
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .scale(scale)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submitting...",
                    style = AppTypography.buttonMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit Application",
                    style = AppTypography.buttonMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Disclaimer
        Text(
            text = "By submitting, you agree to share your profile information with the employer.",
            style = AppTypography.caption.copy(
                color = WorkerColors.TextTertiary,
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
