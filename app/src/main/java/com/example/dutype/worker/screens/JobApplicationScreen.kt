package com.example.dutype.worker.screens

import com.dutype.app.R
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.dutype.utils.AudioRecordingHelper
import com.example.dutype.utils.AudioPlaybackHelper
import java.io.File
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import com.example.dutype.ui.theme.MeeshoFontFamily
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
import com.example.dutype.viewmodels.ProfileViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
import androidx.compose.ui.res.stringResource

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
    val reviewTriggerService = com.example.dutype.di.rememberInAppReviewTriggerService()
    LaunchedEffect(applicationUiState.applicationSuccess) {
        if (applicationUiState.applicationSuccess) {
            showSuccess = true
            applicationViewModel.clearSuccessStates()
            context.findActivity()?.let { reviewTriggerService.onWorkerJobApplication(it) }
        }
    }
    
    // Handle application error
    LaunchedEffect(applicationUiState.error) {
        applicationUiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            applicationViewModel.clearError()
        }
    }

    // Quick Apply sheet: selected quick-select note chips, folded into the
    // existing `coverLetter` field on submit (no new backend field).
    var selectedQuickNotes by remember { mutableStateOf(setOf<String>()) }

    val recordingHelper = remember { AudioRecordingHelper(context) }
    val playbackHelper = remember { AudioPlaybackHelper() }

    var recordedAudioFile by remember { mutableStateOf<File?>(null) }
    var recordedDurationSec by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingProgressSeconds by remember { mutableIntStateOf(0) }

    val isPlayingPreview by playbackHelper.isPlaying.collectAsStateWithLifecycle()
    val playbackProgress by playbackHelper.progress.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            recordingHelper.cleanup()
            playbackHelper.release()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingProgressSeconds = 0
            for (i in 1..15) {
                delay(1000L)
                if (!isRecording) break
                recordingProgressSeconds = i
                if (i >= 15) {
                    val stopResult = recordingHelper.stopRecording()
                    stopResult.onSuccess { (file, duration) ->
                        recordedAudioFile = file
                        recordedDurationSec = duration
                    }
                    isRecording = false
                    break
                }
            }
        } else {
            recordingProgressSeconds = 0
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            playbackHelper.stop()
            val result = recordingHelper.startRecording()
            result.onSuccess {
                isRecording = true
                recordedAudioFile = null
                recordedDurationSec = 0
            }.onFailure {
                Toast.makeText(context, "Failed to start recording: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice intro", Toast.LENGTH_LONG).show()
        }
    }

    val handleStartRecording: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            playbackHelper.stop()
            val result = recordingHelper.startRecording()
            result.onSuccess {
                isRecording = true
                recordedAudioFile = null
                recordedDurationSec = 0
            }.onFailure {
                Toast.makeText(context, "Failed to start recording: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val handleStopRecording: () -> Unit = {
        val stopResult = recordingHelper.stopRecording()
        stopResult.onSuccess { (file, duration) ->
            recordedAudioFile = file
            recordedDurationSec = duration
        }.onFailure {
            Toast.makeText(context, "Recording was too short, please try again", Toast.LENGTH_SHORT).show()
        }
        isRecording = false
    }
    
    when {
        showSuccess && displayJob != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
            ) {
                CommonHeader(
                    title = stringResource(R.string.apply_for_job),
                    onBackClick = {
                        // Treat back the same as the CTA so we don't strand the
                        // user on a stale apply form.
                        navigateToWorkerHome(navController)
                    },
                    backgroundColor = WorkerColors.CardBackground
                )
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
        }
        displayJob == null || profileUiState.isLoading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
            ) {
                CommonHeader(
                    title = stringResource(R.string.apply_for_job),
                    onBackClick = { navController.popBackStack() },
                    backgroundColor = WorkerColors.CardBackground
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = com.example.dutype.ui.theme.WorkerColors.TextPrimary)
                }
            }
        }
        else -> {
            // Quick Apply Confirmation Sheet — pixel-exact modal bottom sheet
            // design. Dims whatever was behind this destination and slides a
            // white, rounded-top sheet up from the bottom.
            QuickApplyConfirmationSheet(
                job = displayJob,
                workerName = profileUiState.user?.fullName,
                workerPhotoUrl = profileUiState.user?.profileImageUrl,
                isSubmitting = applicationUiState.isApplying,
                selectedQuickNotes = selectedQuickNotes,
                onToggleQuickNote = { note ->
                    selectedQuickNotes = if (selectedQuickNotes.contains(note)) {
                        selectedQuickNotes - note
                    } else {
                        selectedQuickNotes + note
                    }
                },
                onDismiss = { navController.popBackStack() },
                onSubmit = {
                    // Selected quick-select chips are folded into the existing
                    // `coverLetter` field the submit call already accepts —
                    // no new backend field is introduced.
                    val note = selectedQuickNotes.takeIf { it.isNotEmpty() }?.joinToString(", ")
                    applicationViewModel.applyForJob(
                        jobId = jobId,
                        coverLetter = note,
                        audioFile = recordedAudioFile,
                        audioDurationSec = if (recordedAudioFile != null) recordedDurationSec else null
                    )
                }
            )
        }
    }
}

/**
 * Quick Apply Confirmation Sheet — pixel-exact restyle of the worker
 * "Apply Now (1-Tap) →" flow into a modal-bottom-sheet-styled layout.
 *
 * NOTE: this is rendered as the sole content of the `job_application` nav
 * destination (a regular screen route, not a `dialog`/bottom-sheet nav
 * destination), so it fakes the sheet chrome — dimmed scrim + white,
 * rounded-top card pinned to the bottom — rather than using a real
 * ModalBottomSheet. Converting the nav destination itself to a true
 * bottom-sheet route would touch WorkerNavGraph/WorkerMainScreen routing
 * and was out of scope for a visual-only restyle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickApplyConfirmationSheet(
    job: JobListing,
    workerName: String?,
    workerPhotoUrl: String?,
    isSubmitting: Boolean,
    selectedQuickNotes: Set<String>,
    onToggleQuickNote: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val quickNoteOptions = listOf("Can join today", "Have own tools", "Available overtime")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(
                    // Swallow taps so they don't fall through to the scrim's
                    // dismiss handler above.
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                ),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE2E8F0))
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "APPLYING FOR:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val employerName = job.companyName.trim()
                    Text(
                        text = if (employerName.isNotEmpty()) "${job.title} — $employerName" else job.title,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F0F0F)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Worker profile preview
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0FDF4))
                                .border(1.dp, Color(0xFFA7F3D0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!workerPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = workerPhotoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = workerName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "W",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = workerName?.trim()?.takeIf { it.isNotEmpty() } ?: "You",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F0F0F)
                            )
                            // Worker-level trade/experience isn't loaded by this
                            // screen's view models (ProfileViewModel's `User`
                            // only carries name + photo). Using the job's real
                            // detected category here instead of fabricating an
                            // "X yrs exp" figure that has no backing data.
                            Text(
                                text = job.getCategory(),
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Add a quick note (optional)",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickNoteOptions.forEach { option ->
                            val isSelected = selectedQuickNotes.contains(option)
                            Box(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(if (isSelected) Color(0xFF0F0F0F) else Color.White)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF0F0F0F) else Color(0xFFE2E8F0),
                                        RoundedCornerShape(17.dp)
                                    )
                                    .clickable { onToggleQuickNote(option) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF0F0F0F)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onSubmit,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            text = if (isSubmitting) "Sending…" else "Confirm & Send Application →",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Employer will be notified instantly",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(
                            12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerWorkTipsSection() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Critical Alert Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.auto_safety_warning),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF991B1B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.apply_tip_no_fee),
                        style = AppTypography.bodySmall.copy(
                            color = Color(0xFF7F1D1D),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }

        // Guidelines Checklist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.apply_work_tips_title),
                        style = AppTypography.sectionHeader.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            fontSize = 14.sp
                        ),
                        fontFamily = MeeshoFontFamily
                    )
                }

                // Tip 1: Confirm Details
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_verify_phone_details),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = stringResource(R.string.apply_tip_confirm_details),
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Tip 2: Punctuality
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFECFDF5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_reach_on_time),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = stringResource(R.string.apply_tip_reach_on_time),
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 16.sp
                        )
                    }
                }

                // Tip 3: Polite Work
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFFDF2F8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = Color(0xFFEC4899),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_polite_behaviour),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = stringResource(R.string.apply_tip_polite_work),
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 16.sp
                        )
                    }
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
    com.example.dutype.components.navigateToWorkerTab(navController, com.example.dutype.navigation.WorkerBottomRoutes.HOME)
}

private fun navigateToWorkerMyJobs(navController: NavController) {
    // Fresh My Jobs so the just-submitted application is shown, not a stale saved tab.
    com.example.dutype.components.navigateToWorkerTab(
        navController,
        com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS,
        restoreState = false
    )
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

    val context = LocalContext.current
    val isTelugu = com.example.dutype.utils.LocaleHelper.getLanguage(context) == com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU

    val hasNotificationPermission = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    var showNotificationCard by remember { mutableStateOf(!hasNotificationPermission) }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showNotificationCard = false
        if (isGranted) {
            Toast.makeText(context, if (isTelugu) "నోటిఫికేషన్లు ఆన్ చేయబడ్డాయి" else "Application alerts enabled", Toast.LENGTH_SHORT).show()
        }
    }

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
            text = stringResource(R.string.auto_application_sent),
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

        if (showNotificationCard) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isTelugu) "ఎంప్లాయర్ కాల్‌ను మిస్ కాకండి!" else "Don't miss the employer's call!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = if (isTelugu) "మిమ్మల్ని షార్ట్‌లిస్ట్ చేసినా లేదా సంప్రదించినా వెంటనే తెలుసుకోవడానికి నోటిఫికేషన్‌లను ఆన్ చేయండి." else "Turn on notifications to know instantly when this employer shortlists or contacts you.",
                            fontSize = 12.sp,
                            color = Color(0xFF3B82F6),
                            lineHeight = 16.sp
                        )
                    }
                    TextButton(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    ) {
                        Text(
                            text = if (isTelugu) "ఆన్ చేయండి" else "Turn On",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = if (canCallEmployer) onCallEmployer else onViewMyJobs,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canCallEmployer) WorkerColors.Success else WorkerColors.Primary
            ),
            shape = RoundedCornerShape(28.dp)
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
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
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
                text = stringResource(R.string.auto_applying_for),
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
                .height(56.dp),
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = WorkerColors.Accent,
                disabledContainerColor = WorkerColors.TextDisabled
            ),
            shape = RoundedCornerShape(28.dp)
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
                    text = stringResource(R.string.auto_submitting),
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
                    text = stringResource(R.string.auto_submit_application),
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
            text = stringResource(R.string.auto_by_submitting_you_agree_to_share_your_prof),
            style = AppTypography.caption.copy(
                color = WorkerColors.TextTertiary,
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun VoiceIntroRecordingCard(
    recordedAudioFile: File?,
    recordedDurationSec: Int,
    isRecording: Boolean,
    recordingProgressSeconds: Int,
    isPlayingPreview: Boolean,
    playbackProgress: Float,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onTogglePlayPreview: () -> Unit,
    onReRecord: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            1.dp,
            if (isRecording) Color(0xFFEF4444) else if (recordedAudioFile != null) Color(0xFF10B981) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (isRecording) Color(0xFFFEE2E2) else if (recordedAudioFile != null) Color(0xFFD1FAE5) else Color(0xFFEDE9FE),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Mic else if (recordedAudioFile != null) Icons.Default.CheckCircle else Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isRecording) Color(0xFFDC2626) else if (recordedAudioFile != null) Color(0xFF059669) else Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "15-Sec Voice Intro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "HIRE 3X FASTER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Bolkar batayein: Naam, kaam ka anubhav, bike/licence",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            if (isRecording) {
                // Recording active state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFDC2626), CircleShape)
                        )
                        Text(
                            text = "Recording Voice Intro: 0:${recordingProgressSeconds.toString().padStart(2, '0')} / 0:15",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (recordingProgressSeconds.toFloat() / 15f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFDC2626),
                        trackColor = Color(0xFFFECACA)
                    )

                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Done Recording (15s Max)", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (recordedAudioFile != null) {
                // Recorded state with preview player
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            Text(
                                text = "Voice Intro Recorded (${recordedDurationSec}s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                        TextButton(
                            onClick = onReRecord,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-record", fontSize = 11.sp, color = Color(0xFF059669))
                        }
                    }

                    LinearProgressIndicator(
                        progress = { playbackProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF059669),
                        trackColor = Color(0xFFA7F3D0)
                    )

                    Button(
                        onClick = onTogglePlayPreview,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlayingPreview) "Pause Preview" else "Listen Preview", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Idle state - tap to record
                Button(
                    onClick = onStartRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record 15-Sec Voice Intro", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
