package com.example.dutype.worker.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.OptimizedProfileImage
import com.example.dutype.models.JobListing
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.findActivity
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.example.dutype.viewmodels.ProfileUiState
import com.example.dutype.viewmodels.ProfileViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

/**
 * Job Application Screen - Review and Submit Application
 * 
 * Shows:
 * 1. Job summary at top
 * 2. Worker's profile info (autofilled)
 * 3. Optional cover letter field
 * 4. Submit Application button
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
    
    // Cover letter state
    var coverLetter by remember { mutableStateOf("") }
    var coverLetterFileUri by remember { mutableStateOf<Uri?>(null) }
    var coverLetterFileName by remember { mutableStateOf<String?>(null) }
    var coverLetterUploadUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingCoverLetter by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // File picker for cover letter upload
    val coverLetterPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coverLetterFileUri = uri
            // Get file name
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) coverLetterFileName = it.getString(nameIndex)
                }
            }
            if (coverLetterFileName == null) coverLetterFileName = "cover_letter"
            
            // Upload to Firebase Storage
            val userId = currentUser?.uid ?: return@rememberLauncherForActivityResult
            isUploadingCoverLetter = true
            scope.launch {
                try {
                    val storageRef = FirebaseStorage.getInstance().reference
                        .child("cover_letters/$userId/${System.currentTimeMillis()}_${coverLetterFileName}")
                    storageRef.putFile(uri).await()
                    val downloadUrl = storageRef.downloadUrl.await().toString()
                    coverLetterUploadUrl = downloadUrl
                    Timber.d("Cover letter uploaded: $downloadUrl")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to upload cover letter")
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    coverLetterFileUri = null
                    coverLetterFileName = null
                } finally {
                    isUploadingCoverLetter = false
                }
            }
        }
    }
    
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

                        // Batch-l: replaced the bulky "Your Profile" card +
                        // duplicate "What employer will see" list with a
                        // single compact share notice.
                        ShareProfileNotice()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cover Letter Section
                        CoverLetterSection(
                            coverLetter = coverLetter,
                            onCoverLetterChange = { coverLetter = it },
                            coverLetterFileName = coverLetterFileName,
                            isUploading = isUploadingCoverLetter,
                            onUploadClick = { coverLetterPickerLauncher.launch("application/*") },
                            onRemoveFile = {
                                coverLetterFileUri = null
                                coverLetterFileName = null
                                coverLetterUploadUrl = null
                            }
                        )

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
                            isSubmitting = applicationUiState.isApplying || isUploadingCoverLetter,
                            onSubmit = {
                                val finalCoverLetter = when {
                                    coverLetterUploadUrl != null -> "[FILE] $coverLetterUploadUrl"
                                    coverLetter.isNotBlank() -> coverLetter
                                    else -> null
                                }
                                applicationViewModel.applyForJob(
                                    jobId = jobId,
                                    coverLetter = finalCoverLetter
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
private fun ShareProfileNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Your profile (name, phone, skills) will be shared with the employer.",
            style = AppTypography.bodyMedium.copy(
                color = Color(0xFF065F46),
                fontSize = 13.sp
            ),
            modifier = Modifier.weight(1f)
        )
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

@Composable
private fun ApplicationSentSuccess(
    jobTitle: String,
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
                .background(Color(0xFF10B981).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981)),
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
                color = Color(0xFF111827)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Your application for \"$jobTitle\" has been successfully sent.",
            style = AppTypography.bodyMedium.copy(
                color = Color(0xFF6B7280),
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onReturnHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1F2937)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Return to Home",
                style = AppTypography.buttonMedium.copy(
                    color = Color.White,
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
                    color = Color(0xFF6B7280),
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
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
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
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = job.addressText,
                            style = AppTypography.caption.copy(color = Color(0xFF6B7280)),
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
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "/$period",
                        style = AppTypography.caption.copy(color = Color(0xFF6B7280))
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    // Batch-l: kept as a private utility (no longer referenced) to avoid
    // touching unrelated callers; safe to delete in a future cleanup.
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                style = AppTypography.caption.copy(
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                )
            )
            Text(
                text = value,
                style = AppTypography.bodyMedium.copy(
                    color = Color(0xFF374151),
                    fontSize = 13.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverLetterSection(
    coverLetter: String,
    onCoverLetterChange: (String) -> Unit,
    coverLetterFileName: String?,
    isUploading: Boolean,
    onUploadClick: () -> Unit,
    onRemoveFile: () -> Unit
) {
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
            modifier = Modifier.padding(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF1F2937),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Cover Letter",
                    style = AppTypography.sectionHeader.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "(Optional)",
                    style = AppTypography.caption.copy(
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Upload file option
            if (coverLetterFileName != null) {
                // Show uploaded file
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = coverLetterFileName,
                            style = AppTypography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isUploading) {
                            Text(
                                text = "Uploading...",
                                style = AppTypography.caption.copy(
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.sp
                                )
                            )
                        } else {
                            Text(
                                text = "Uploaded successfully",
                                style = AppTypography.caption.copy(
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                    if (!isUploading) {
                        IconButton(onClick = onRemoveFile, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove file",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            } else {
                // Upload button
                OutlinedButton(
                    onClick = onUploadClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1F2937)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload Cover Letter (PDF, DOC)",
                        style = AppTypography.bodyMedium.copy(fontSize = 13.sp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Divider with "or"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
                Text(
                    text = "  or write below  ",
                    style = AppTypography.caption.copy(
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Cover letter text input
            OutlinedTextField(
                value = coverLetter,
                onValueChange = onCoverLetterChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = "Tell the employer why you're a great fit for this job...",
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1F2937),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color(0xFFFAFAFA),
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = AppTypography.bodyMedium.copy(fontSize = 14.sp)
            )
            
            Spacer(modifier = Modifier.height(3.dp))
            
            // Character count
            Text(
                text = "${coverLetter.length}/500",
                style = AppTypography.caption.copy(
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                ),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun WhatEmployerWillSeeSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(0.5.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "What employer will see",
                    style = AppTypography.sectionHeader.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF065F46),
                        fontSize = 14.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // List of visible items
            val visibleItems = listOf(
                "Your name and profile photo",
                "Phone number (for contact)",
                "Skills and experience",
                "Cover letter (if provided)",
                "Your location"
            )
            
            visibleItems.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item,
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF065F46),
                            fontSize = 13.sp
                        )
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
                containerColor = Color(0xFF374151),
                disabledContainerColor = Color(0xFF9CA3AF)
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
                color = Color(0xFF9CA3AF),
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
