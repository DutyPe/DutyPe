package com.example.dutype.employer.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import androidx.navigation.compose.rememberNavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.RatingBottomSheet
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.getDisplayName
import com.example.dutype.models.getStatusColor
import com.example.dutype.services.JobApplicationService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Professional Worker Profile View Screen
 * Enterprise-level worker profile viewing for employers with 30+ years of Android development experience
 * Provides comprehensive worker information, application details, and action management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalWorkerProfileViewScreen(
    navController: NavController,
    workerId: String,
    applicationId: String? = null,
    scrollStateManager: ScrollStateManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Services accessed via ViewModels (proper DI pattern)
    val jobApplicationViewModel: com.example.dutype.viewmodels.SmartJobApplicationViewModel = hiltViewModel()
    val jobApplicationService = jobApplicationViewModel.jobApplicationService
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val profileCompletionService = profileCompletionViewModel.profileCompletionService
    val ratingService = remember {
        com.example.dutype.services.RatingService(
            com.example.dutype.di.firestoreFromHilt(context),
            com.example.dutype.di.authFromHilt(context)
        )
    }
    
    // State management
    var workerProfile by remember { mutableStateOf<WorkerProfileData?>(null) }
    var application by remember { mutableStateOf<JobApplication?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<ApplicationAction?>(null) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var hasRatedWorker by remember { mutableStateOf(false) }
    // Batch-k fix: let the Retry button actually trigger a re-fetch by
    // bumping this counter into the LaunchedEffect key set.
    var reloadTick by remember { mutableStateOf(0) }

    // Load worker profile and application data
    LaunchedEffect(workerId, applicationId, reloadTick) {
        try {
            isLoading = true
            error = null

            // If we have an applicationId, seed from the denormalized
            // snapshot on the application doc so the screen has data even
            // if the callable below is slow/unavailable.
            applicationId?.let { appId ->
                val applicationsResult = jobApplicationService.getApplicationById(appId)
                applicationsResult.onSuccess { appResult: com.example.dutype.models.JobApplication? ->
                    val app = appResult
                    if (app != null) {
                        application = app
                        workerProfile = WorkerProfileData(
                            workerId = app.workerId,
                            fullName = app.workerName.ifBlank { "Worker" },
                            phone = app.workerPhone.orEmpty(),
                            email = app.workerEmail.orEmpty(),
                            location = "",
                            gender = app.workerGender,
                            profileImageUrl = app.workerProfileImageUrl,
                            experience = emptyList(),
                            skills = app.workerSkills,
                            languages = emptyList(),
                            experienceLevel = app.workerExperience,
                            dateOfBirth = app.workerDateOfBirth,
                            educationQualification = app.workerEducationQualification,
                            bio = app.workerBio
                        )
                    }
                }
            }

            // Always attempt the callable to get full profile (callable is
            // region-pinned to asia-south1 by ProfileCompletionService).
            val profileResult = profileCompletionService.getWorkerProfileForEmployer(
                workerId = workerId,
                jobId = application?.jobId?.takeIf { it.isNotBlank() }
            )
            profileResult.fold(
                onSuccess = { data ->
                    val locationMap = data["location"] as? Map<*, *>
                    val lat = (locationMap?.get("lat") as? Number)?.toDouble()
                    val lng = (locationMap?.get("lng") as? Number)?.toDouble()
                    val locationText = when {
                        !((data["city"] as? String).isNullOrBlank()) -> data["city"] as String
                        lat != null && lng != null && (lat != 0.0 || lng != 0.0) ->
                            String.format(Locale.US, "%.4f, %.4f", lat, lng)
                        else -> ""
                    }

                    val primarySkills = (data["skills"] as? List<*>)
                        ?.mapNotNull { it?.toString()?.trim()?.takeIf { value -> value.isNotBlank() } }
                        .orEmpty()
                    val jobTypeSkills = (data["jobTypes"] as? List<*>)
                        ?.mapNotNull { it?.toString()?.trim()?.takeIf { value -> value.isNotBlank() } }
                        .orEmpty()

                    workerProfile = WorkerProfileData(
                        workerId = workerId,
                        fullName = (data["fullName"] as? String).orEmpty()
                            .ifBlank { workerProfile?.fullName.orEmpty() }
                            .ifBlank { "Worker" },
                        phone = (data["phone"] as? String).orEmpty()
                            .ifBlank { workerProfile?.phone.orEmpty() },
                        email = (data["email"] as? String).orEmpty()
                            .ifBlank { workerProfile?.email.orEmpty() },
                        location = locationText,
                        gender = (data["gender"] as? String).orEmpty()
                            .ifBlank { workerProfile?.gender.orEmpty() },
                        profileImageUrl = (data["profileImageUrl"] as? String)
                            ?: workerProfile?.profileImageUrl,
                        experience = emptyList(),
                        skills = (primarySkills + jobTypeSkills + (workerProfile?.skills ?: emptyList())).distinct(),
                        languages = (data["languages"] as? List<*>)
                            ?.mapNotNull { it?.toString()?.trim()?.takeIf { value -> value.isNotBlank() } }
                            .orEmpty(),
                        experienceLevel = (data["experience"] as? String).orEmpty()
                            .ifBlank { workerProfile?.experienceLevel.orEmpty() },
                        dateOfBirth = (data["dateOfBirth"] as? String).orEmpty()
                            .ifBlank { workerProfile?.dateOfBirth.orEmpty() },
                        educationQualification = (data["educationQualification"] as? String).orEmpty()
                            .ifBlank { workerProfile?.educationQualification.orEmpty() },
                        bio = (data["bio"] as? String).orEmpty()
                            .ifBlank { workerProfile?.bio.orEmpty() },
                        rating = (data["rating"] as? Number)?.toDouble()
                            ?: (data["ratingAvg"] as? Number)?.toDouble() ?: 0.0,
                        totalJobs = (data["totalJobs"] as? Number)?.toInt() ?: 0,
                        completedJobs = (data["completedJobs"] as? Number)?.toInt() ?: 0
                    )
                },
                onFailure = { e ->
                    // Batch-k fix: the callable may fail (region mismatch,
                    // network, cold start). Fall back to the employer-owned
                    // applications row, which carries a denormalized worker
                    // snapshot (workerName/phone/email/skills/profileImageUrl).
                    // Employer rules allow reads where employerId == uid.
                    if (workerProfile == null) {
                        val fallback = runCatching {
                            val uid = com.google.firebase.auth.FirebaseAuth
                                .getInstance().currentUser?.uid
                            if (uid.isNullOrBlank()) return@runCatching null
                            val snap = com.google.firebase.firestore.FirebaseFirestore
                                .getInstance()
                                .collection("applications")
                                .whereEqualTo("employerId", uid)
                                .whereEqualTo("workerId", workerId)
                                .limit(1)
                                .get()
                                .await()
                            val d = snap.documents.firstOrNull()?.data ?: return@runCatching null
                            WorkerProfileData(
                                workerId = workerId,
                                fullName = (d["workerName"] as? String).orEmpty().ifBlank { "Worker" },
                                phone = (d["workerPhone"] as? String).orEmpty(),
                                email = (d["workerEmail"] as? String).orEmpty(),
                                location = "",
                                gender = (d["workerGender"] as? String).orEmpty(),
                                profileImageUrl = d["workerProfileImageUrl"] as? String,
                                experience = emptyList(),
                                skills = (d["workerSkills"] as? List<*>)
                                    ?.mapNotNull { it?.toString()?.trim()?.takeIf { v -> v.isNotBlank() } }
                                    .orEmpty(),
                                languages = emptyList(),
                                experienceLevel = (d["workerExperience"] as? String).orEmpty(),
                                dateOfBirth = (d["workerDateOfBirth"] as? String).orEmpty(),
                                educationQualification = (d["workerEducationQualification"] as? String).orEmpty(),
                                bio = (d["workerBio"] as? String).orEmpty()
                            )
                        }.getOrNull()

                        if (fallback != null) {
                            workerProfile = fallback
                        } else {
                            error = e.message ?: "Failed to load worker profile"
                        }
                    }
                    // If workerProfile was already seeded from applicationId
                    // snapshot, keep it and show no error.
                }
            )

            isLoading = false
        } catch (e: Exception) {
            if (workerProfile == null) error = e.message ?: "Failed to load worker profile"
            isLoading = false
        }
    }

    LaunchedEffect(application?.id, application?.jobId, application?.workerId, application?.status) {
        val app = application
        hasRatedWorker = if (app?.status == ApplicationStatus.COMPLETED && app.jobId.isNotBlank() && app.workerId.isNotBlank()) {
            ratingService.hasRated(app.jobId, app.workerId)
        } else {
            false
        }
    }

    if (showRatingSheet && application != null) {
        RatingBottomSheet(
            isVisible = showRatingSheet,
            targetName = workerProfile?.fullName?.ifBlank { application?.workerName.orEmpty() } ?: "this worker",
            targetRole = "WORKER",
            onDismiss = { showRatingSheet = false },
            onSubmit = { rating, review, tags ->
                application?.let { app ->
                    scope.launch {
                        ratingService.submitRating(
                            jobId = app.jobId,
                            targetUserId = app.workerId,
                            rating = rating,
                            review = review,
                            tags = tags
                        ).fold(
                            onSuccess = { result ->
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                if (result.success) {
                                    hasRatedWorker = true
                                    showRatingSheet = false
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
    
    // Solid role background; no gradient.
    val backgroundGradient = com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CommonHeader(
                title = stringResource(R.string.worker_profile_label),
                onBackClick = { navController.popBackStack() },
                backgroundColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground
            )

            // Batch-k fix: the previous `ProfessionalWorkerProfileHeader`
            // card duplicated the name/avatar block and rendered a bare
            // "Worker" fallback on load errors. It's been removed; the
            // list below now leads with a compact identity section inside
            // `PersonalInformationCard` so the screen shows only worker-
            // specific data in a single clean card stack.

            // Content
            if (isLoading) {
                LoadingWorkerProfileState()
            } else if (error != null) {
                ErrorWorkerProfileState(
                    error = error!!,
                    onRetry = { reloadTick++ }
                )
            } else if (workerProfile != null) {
                ScrollAwareLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 0.dp
                    ),
                    scrollStateManager = scrollStateManager
                ) {
                    // Personal Information (compact identity header merged in)
                    item {
                        PersonalInformationCard(
                            workerProfile = workerProfile!!,
                            application = application
                        )
                    }
                    
                    // Work Experience
                    if (workerProfile!!.experience.isNotEmpty()) {
                        item {
                            WorkExperienceCard(experience = workerProfile!!.experience)
                        }
                    }
                    
                    // Skills
                    if (workerProfile!!.skills.isNotEmpty()) {
                        item {
                            SkillsCard(skills = workerProfile!!.skills)
                        }
                    }

                    // Apr 2026: removed the redundant "Additional Information"
                    // card — languages now live inside Personal Information.

                    // Apr 2026: action buttons moved out of the scrolling
                    // list and pinned to the bottom of the screen instead
                    // (rendered below as a sibling to the Column). Keep a
                    // tail spacer so the last card isn't hidden behind it.
                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }
        }

        // Apr 2026: Shortlist / Reject pinned to the bottom of the screen
        // with breathing room from the system bar. Only shown once the
        // profile actually loaded.
        val showActionBar = application?.status in setOf(
            ApplicationStatus.APPLIED,
            ApplicationStatus.SHORTLISTED,
            ApplicationStatus.HIRED,
            ApplicationStatus.COMPLETED
        )
        if (!isLoading && error == null && workerProfile != null && showActionBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 24.dp + androidx.compose.foundation.layout.WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                    )
            ) {
                ActionButtonsCard(
                    application = application,
                    hasRatedWorker = hasRatedWorker,
                    onRateWorkerClick = {
                        application?.let { app ->
                            scope.launch {
                                val alreadyRated = ratingService.hasRated(app.jobId, app.workerId)
                                if (alreadyRated) {
                                    hasRatedWorker = true
                                } else {
                                    showRatingSheet = true
                                }
                            }
                        }
                    },
                    onActionClick = { action ->
                        selectedAction = action
                        showActionDialog = true
                    }
                )
            }
        }
    }
    
    // Action Dialog
    if (showActionDialog && selectedAction != null) {
        ApplicationActionDialog(
            action = selectedAction!!,
            workerName = workerProfile?.fullName ?: "Worker",
            onDismiss = { 
                showActionDialog = false
                selectedAction = null
            },
            onConfirm = { action ->
                scope.launch {
                    try {
                        when (action) {
                            ApplicationAction.SHORTLIST -> {
                                application?.let { app ->
                                    val result = jobApplicationService.acceptApplication(app.id, app.employerId)
                                    result.getOrThrow()
                                    application = app.copy(status = ApplicationStatus.HIRED)
                                }
                            }
            ApplicationAction.REJECT -> {
                                application?.let { app ->
                                    val result = jobApplicationService.rejectApplication(app.id, app.employerId)
                                    result.getOrThrow()
                                    application = app.copy(status = ApplicationStatus.REJECTED)
                                }
                            }
                            ApplicationAction.MARK_COMPLETED -> {
                                application?.let { app ->
                                    val updatedApplication = jobApplicationService.updateApplicationStatus(
                                        app.id,
                                        ApplicationStatus.COMPLETED,
                                        "employer"
                                    ).getOrThrow()
                                    application = updatedApplication.copy(status = ApplicationStatus.COMPLETED)
                                    val alreadyRated = ratingService.hasRated(app.jobId, app.workerId)
                                    if (alreadyRated) {
                                        hasRatedWorker = true
                                        Toast.makeText(context, "You already rated this worker", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showRatingSheet = true
                                    }
                                }
                            }
                            else -> { /* No action */ }
                        }
                        showActionDialog = false
                        selectedAction = null
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            }
        )
    }
}

@Composable
private fun ProfessionalWorkerProfileHeader(
    workerProfile: WorkerProfileData?,
    application: JobApplication?
) {
    // Bug fix: redesigned the header.
    // - Removed the noisy "Contact" message-style button (DutyPe has no
    //   in-app messaging; the dial fallback was confusing). Employers can
    //   tap the phone row in Personal Information to dial.
    // - The header now shows ONE clean identity block: large avatar,
    //   name, applied-job title, and a chip with location + status, so
    //   we no longer render the worker's name twice.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = workerProfile?.profileImageUrl
                if (!imageUrl.isNullOrBlank()) {
                    com.example.dutype.components.OptimizedProfileImage(
                        imageUrl = imageUrl,
                        contentDescription = "Worker Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    val initials = workerProfile?.fullName.orEmpty()
                        .split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                        .ifEmpty { workerProfile?.fullName?.take(1)?.uppercase().orEmpty() }
                    if (initials.isNotBlank()) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Name
            Text(
                text = workerProfile?.fullName.orEmpty().ifBlank { "Worker" },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            )

            // Applied for
            val appliedFor = application?.jobTitle.orEmpty()
            if (appliedFor.isNotBlank()) {
                Text(
                    text = "Applied for $appliedFor",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }

            // Location row
            val location = workerProfile?.location.orEmpty()
            if (location.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationStatusCard(
    application: JobApplication,
    onUpdateStatus: (ApplicationStatus) -> Unit
) {
    val statusColor = application.status.getStatusColor()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Application Status",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = application.status.getDisplayName(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    )
                }
            }
            
            Text(
                text = "Applied for ${application.jobTitle} at ${application.companyName}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                )
            )
            
            Text(
                text = "Applied on ${dateFormat.format(Date(application.createdAt))}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF)
                )
            )
            
            // Cover letter preview
            if (application.coverLetter.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Cover Letter",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = application.coverLetter,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalInformationCard(
    workerProfile: WorkerProfileData,
    application: JobApplication? = null
) {
    val appliedAt = application?.createdAt?.takeIf { it > 0L }?.let { createdAt ->
        SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Batch-k fix: inline identity block (avatar + name + applied-for)
            // inside the Personal Information card so the screen no longer
            // needs a separate big "Worker" header card.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = workerProfile.profileImageUrl
                    if (!imageUrl.isNullOrBlank()) {
                        com.example.dutype.components.OptimizedProfileImage(
                            imageUrl = imageUrl,
                            contentDescription = "Worker Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        val initials = workerProfile.fullName
                            .split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                            .ifEmpty { workerProfile.fullName.take(1).uppercase() }
                        if (initials.isNotBlank()) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B82F6)
                                )
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = workerProfile.fullName.ifBlank { "Worker" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827)
                        )
                    )
                }

                if (appliedAt != null) {
                    Text(
                        text = "Applied $appliedAt",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        ),
                        textAlign = TextAlign.End
                    )
                }
            }

            androidx.compose.material3.HorizontalDivider(
                color = Color(0xFFE5E7EB),
                thickness = 0.5.dp
            )

            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )

            if (workerProfile.phone.isNotBlank()) {
                PersonalInfoRow("Phone", workerProfile.phone, isPhone = true)
            }
            if (workerProfile.email.isNotBlank()) {
                PersonalInfoRow("Email", workerProfile.email)
            }
            if (workerProfile.gender.isNotBlank()) {
                PersonalInfoRow("Gender", workerProfile.gender)
            }
            if (workerProfile.experienceLevel.isNotBlank()) {
                PersonalInfoRow("Experience", workerProfile.experienceLevel)
            }
            if (workerProfile.educationQualification.isNotBlank()) {
                PersonalInfoRow("Education", workerProfile.educationQualification)
            }
            if (workerProfile.dateOfBirth.isNotBlank()) {
                PersonalInfoRow("Age", formatWorkerAge(workerProfile.dateOfBirth))
            }
            if (workerProfile.bio.isNotBlank()) {
                PersonalInfoRow("Bio", workerProfile.bio)
            }
            if (workerProfile.rating > 0.0) {
                PersonalInfoRow(
                    "Rating",
                    String.format(java.util.Locale.US, "%.1f / 5", workerProfile.rating)
                )
            }
            if (workerProfile.languages.isNotEmpty()) {
                PersonalInfoRow("Languages", workerProfile.languages.joinToString(", "))
            }
        }
    }
}

@Composable
private fun PersonalInfoRow(label: String, value: String, isPhone: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPhone && value.isNotBlank()) {
                    Modifier.clickable {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_DIAL,
                                    android.net.Uri.parse("tel:$value")
                                )
                            )
                        }
                    }
                } else Modifier
            ),
            verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label : ",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280)
            )
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = if (isPhone && value.isNotBlank()) Color(0xFF2563EB) else Color(0xFF1F2937)
            )
        )
    }
}

@Composable
private fun WorkExperienceCard(experience: List<WorkExperienceDisplay>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Work Experience",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            experience.forEach { exp ->
                ExperienceItem(experience = exp)
            }
        }
    }
}

@Composable
private fun ExperienceItem(experience: WorkExperienceDisplay) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = experience.position,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = experience.duration,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
            
            Text(
                text = experience.company,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF3B82F6)
                )
            )
            
            Text(
                text = experience.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}

@Composable
private fun SkillsCard(skills: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Skills",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            // Skills chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(skills) { skill ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text(
                            text = skill,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF3B82F6)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdditionalInfoCard(workerProfile: WorkerProfileData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Additional Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            if (workerProfile.languages.isNotEmpty()) {
                Text(
                    text = "Languages: ${workerProfile.languages.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsCard(
    application: JobApplication?,
    hasRatedWorker: Boolean,
    onRateWorkerClick: () -> Unit,
    onActionClick: (ApplicationAction) -> Unit
) {
    // Batch-p #6: dropped the elevated Card wrapper and the multiple
    // single-button rows. Shortlist + Reject now share ONE row with
    // weight(1f) on each button so they fill evenly and read clearly
    // as a paired primary/secondary action.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (application?.status == ApplicationStatus.APPLIED || application?.status == ApplicationStatus.SHORTLISTED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onActionClick(ApplicationAction.REJECT) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDC2626)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.reject), style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    onClick = { onActionClick(ApplicationAction.SHORTLIST) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accept", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Bug #15 fix: show "Mark Work Done" only after the candidate is
        // hired so the employer can complete the contract and the
        // worker's earnings move from pending â†’ paid on the Earnings tab.
        if (application?.status == ApplicationStatus.HIRED) {
            Button(
                onClick = { onActionClick(ApplicationAction.MARK_COMPLETED) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F8B4C)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mark Work Done", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (application?.status == ApplicationStatus.COMPLETED) {
            OutlinedButton(
                onClick = onRateWorkerClick,
                enabled = !hasRatedWorker,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFF59E0B),
                    disabledContentColor = Color(0xFF059669)
                )
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (hasRatedWorker) "Rated Worker" else "Rate Worker",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ApplicationActionDialog(
    action: ApplicationAction,
    workerName: String,
    onDismiss: () -> Unit,
    onConfirm: (ApplicationAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (action) {
                    ApplicationAction.SHORTLIST -> "Accept Candidate"
                    ApplicationAction.REJECT -> "Reject Application"
                    ApplicationAction.SEND_MESSAGE -> "Send Message"
                    ApplicationAction.MARK_COMPLETED -> "Mark Work Done"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Text(
                text = when (action) {
                    ApplicationAction.SHORTLIST -> "Are you sure you want to accept $workerName for this position?"
                    ApplicationAction.REJECT -> "Are you sure you want to reject $workerName's application?"
                    ApplicationAction.SEND_MESSAGE -> "Do you want to send a message to $workerName?"
                    ApplicationAction.MARK_COMPLETED -> "Mark this work as completed for $workerName? Their earnings will be unlocked."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(action) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (action) {
                        ApplicationAction.SHORTLIST -> Color(0xFF10B981)
                        ApplicationAction.REJECT -> Color(0xFFDC2626)
                        ApplicationAction.SEND_MESSAGE -> Color(0xFF3B82F6)
                        ApplicationAction.MARK_COMPLETED -> Color(0xFF1F8B4C)
                    }
                )
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun LoadingWorkerProfileState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF3B82F6)
            )
            Text(
                text = "Loading worker profile...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}

@Composable
private fun ErrorWorkerProfileState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFDC2626)
                )
                Text(
                    text = "Failed to load worker profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    ),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

// Screen-specific data classes for UI display
// NOTE: These are LOCAL to this screen and different from models/JobApplicationModels.kt
// They have simplified fields for display purposes only

data class WorkerProfileData(
    val workerId: String,
    val fullName: String,
    val phone: String,
    // Bug #7 fix: surface the worker's contact email on the employer
    // detail screen. Empty when the worker hasn't shared one.
    val email: String = "",
    val location: String,
    val gender: String,
    val profileImageUrl: String?,
    val experience: List<WorkExperienceDisplay>,
    val skills: List<String>,
    val languages: List<String>,
    // Apr 2026: free-form experience bucket the worker picked during setup
    // ("Less than a year", "1-2", "3-5", "More than 5"). Surfaces directly
    // on the employer's view of the candidate.
    val experienceLevel: String = "",
    val dateOfBirth: String = "",
    val educationQualification: String = "",
    val bio: String = "",
    val rating: Double = 0.0,
    val totalJobs: Int = 0,
    val completedJobs: Int = 0
)

private fun formatWorkerAge(dateOfBirth: String): String {
    val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")
    val birthDate = formats.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(dateOfBirth)
        }.getOrNull()
    } ?: return dateOfBirth
    val birth = java.util.Calendar.getInstance().apply { time = birthDate }
    val today = java.util.Calendar.getInstance()
    var age = today.get(java.util.Calendar.YEAR) - birth.get(java.util.Calendar.YEAR)
    if (today.get(java.util.Calendar.DAY_OF_YEAR) < birth.get(java.util.Calendar.DAY_OF_YEAR)) age--
    return if (age > 0) "$age years old" else dateOfBirth
}

/**
 * Simplified work experience for display only
 * Different from models/JobApplicationModels.WorkExperience which has more fields
 */
data class WorkExperienceDisplay(
    val company: String,
    val position: String,
    val duration: String,
    val description: String
)

enum class ApplicationAction {
    SHORTLIST,
    REJECT,
    SEND_MESSAGE,
    // Bug #15 fix: employer marks a HIRED application as COMPLETED, which
    // unlocks the worker's earnings entry on the Earnings dashboard.
    MARK_COMPLETED
}

// NOTE: getStatusColor removed - use ApplicationStatus.getStatusColor() extension from models instead

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfessionalWorkerProfileViewScreenPreview() {
    ProfessionalWorkerProfileViewScreen(navController = rememberNavController(), workerId = "sample_worker_id")
}

