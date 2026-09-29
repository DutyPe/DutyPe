package com.example.dutype.employer.screens

import com.dutype.app.R
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.sp
import com.example.dutype.services.Rating
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
import com.example.dutype.ui.theme.EmployerColors
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
    var showReportSheet by remember { mutableStateOf(false) }
    var hasRatedWorker by remember { mutableStateOf(false) }
    // Batch-k fix: let the Retry button actually trigger a re-fetch by
    // bumping this counter into the LaunchedEffect key set.
    var reloadTick by remember { mutableStateOf(0) }
    var reviews by remember { mutableStateOf<List<Rating>>(emptyList()) }

    // Reviews received by this worker (empty on failure; service swallows errors)
    LaunchedEffect(workerId, reloadTick) {
        reviews = ratingService.getUserRatings(workerId)
    }

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
                        
                        // Auto-mark application as viewed (Seen by Employer) when employer opens details
                        if (app.status == ApplicationStatus.APPLIED) {
                            scope.launch {
                                val markResult = jobApplicationService.markApplicationAsUnderReview(app.id, app.employerId)
                                markResult.onSuccess { updatedApp ->
                                    application = updatedApp
                                }
                            }
                        }
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
                        completedJobs = (data["completedJobs"] as? Number)?.toInt() ?: 0,
                        isVerified = (data["aadhaarVerified"] as? Boolean)
                            ?: (data["isAadhaarVerified"] as? Boolean) ?: false,
                        memberSince = wpMemberSince(data["createdAt"])
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

    // Rating Sheet
    if (showRatingSheet && workerProfile != null) {
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
                            tags = tags,
                            targetRole = "WORKER"
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
    
    WorkerProfileScreenBody(
        navController = navController,
        workerProfile = workerProfile,
        application = application,
        isLoading = isLoading,
        error = error,
        reviews = reviews,
        hasRatedWorker = hasRatedWorker,
        onRetry = { reloadTick++ },
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
                                        Toast.makeText(context, context.getString(R.string.already_rated_worker), Toast.LENGTH_SHORT).show()
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
                    .background(EmployerColors.Primary.copy(alpha = 0.10f)),
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
                                color = EmployerColors.Primary
                            )
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = EmployerColors.Primary,
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
                    color = EmployerColors.TextPrimary
                )
            )

            // Applied for
            val appliedFor = application?.jobTitle.orEmpty()
            if (appliedFor.isNotBlank()) {
                Text(
                    text = "Applied for $appliedFor",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EmployerColors.TextSecondary
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
                        tint = EmployerColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EmployerColors.TextSecondary
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
                    text = stringResource(R.string.auto_application_status),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
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
                    color = EmployerColors.TextSecondary
                )
            )
            
            Text(
                text = "Applied on ${dateFormat.format(Date(application.createdAt))}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EmployerColors.TextTertiary
                )
            )
            
            // Cover letter preview
            if (application.coverLetter.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = EmployerColors.ChipBackground
                    ),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auto_cover_letter),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EmployerColors.TextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = application.coverLetter,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EmployerColors.TextSecondary
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
                        .background(EmployerColors.Primary.copy(alpha = 0.10f)),
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
                                    color = EmployerColors.Primary
                                )
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = EmployerColors.Primary,
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
                            color = EmployerColors.TextPrimary
                        )
                    )
                }

                if (appliedAt != null) {
                    Text(
                        text = "Applied $appliedAt",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EmployerColors.TextSecondary
                        ),
                        textAlign = TextAlign.End
                    )
                }
            }

            androidx.compose.material3.HorizontalDivider(
                color = EmployerColors.Border,
                thickness = 0.5.dp
            )

            Text(
                text = stringResource(R.string.auto_personal_information),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
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
                color = EmployerColors.TextSecondary
            )
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = if (isPhone && value.isNotBlank()) EmployerColors.Primary else EmployerColors.TextPrimary
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
                text = stringResource(R.string.auto_work_experience),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
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
            containerColor = EmployerColors.ChipBackground
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
                        color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                    )
                )
                Text(
                    text = experience.duration,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EmployerColors.TextSecondary
                    )
                )
            }
            
            Text(
                text = experience.company,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EmployerColors.Primary
                )
            )
            
            Text(
                text = experience.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EmployerColors.TextSecondary
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
                text = stringResource(R.string.auto_skills),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                )
            )
            
            // Skills chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(skills) { skill ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = EmployerColors.Primary.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text(
                            text = skill,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = EmployerColors.Primary
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
                text = stringResource(R.string.auto_additional_information),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                )
            )
            
            if (workerProfile.languages.isNotEmpty()) {
                Text(
                    text = "Languages: ${workerProfile.languages.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EmployerColors.TextSecondary
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
        if (application?.status == ApplicationStatus.APPLIED || application?.status == ApplicationStatus.APPLIED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onActionClick(ApplicationAction.REJECT) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EmployerColors.Error
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
                        containerColor = EmployerColors.Success
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.accept), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Shown once the candidate is hired so the employer can close the contract early.
        // If they never do, the job auto-completes server-side a few hours after hiring.
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
                Text(stringResource(R.string.mark_work_done), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (application?.status == ApplicationStatus.COMPLETED) {
            OutlinedButton(
                onClick = onRateWorkerClick,
                enabled = !hasRatedWorker,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = EmployerColors.Warning,
                    disabledContentColor = EmployerColors.Success
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
                        ApplicationAction.SHORTLIST -> EmployerColors.Success
                        ApplicationAction.REJECT -> EmployerColors.Error
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
                color = EmployerColors.Primary
            )
            Text(
                text = stringResource(R.string.auto_loading_worker_profile),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = EmployerColors.TextSecondary
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
                    tint = EmployerColors.Error
                )
                Text(
                    text = stringResource(R.string.auto_failed_to_load_worker_profile),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                    )
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EmployerColors.TextSecondary
                    ),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmployerColors.Primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Worker Profile (Employer View) - flat design. All composables are kept small
// on purpose (large composables can trigger D8 VerifyError at runtime).
// ---------------------------------------------------------------------------

private val WpBg = Color(0xFFF8FAFC)
private val WpNavy = Color(0xFF0F172A)
private val WpCobalt = Color(0xFF2563EB)
private val WpBorder = Color(0xFFE2E8F0)
private val WpInk = Color(0xFF0F0F0F)
private val WpMuted = Color(0xFF64748B)
private val WpFaint = Color(0xFF94A3B8)
private val WpBody = Color(0xFF475569)
private val WpStar = Color(0xFFF59E0B)
private val WpGreen = Color(0xFF16A34A)
private val WpGreenBg = Color(0xFFF0FDF4)
private val WpAvatarBg = Color(0xFFDBEAFE)
private val WpDanger = Color(0xFFDC2626)
private val WpTabs = listOf("Skills & Exp", "Reviews", "Contact")

private data class WpTimelineItem(
    val title: String,
    val subtitle: String,
    val dates: String
)

private fun wpMemberSince(raw: Any?): String {
    var millis = 0L
    if (raw is Number) {
        millis = raw.toLong()
    } else if (raw is Map<*, *>) {
        val secs = (raw["_seconds"] as? Number) ?: (raw["seconds"] as? Number)
        millis = (secs?.toLong() ?: 0L) * 1000L
    }
    if (millis in 1..99_999_999_999L) millis *= 1000L
    if (millis <= 0L) return ""
    return SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(millis))
}

private fun wpBuildHistory(profile: WorkerProfileData): List<WpTimelineItem> {
    if (profile.experience.isNotEmpty()) {
        return profile.experience.map {
            WpTimelineItem(it.position, it.company, it.duration)
        }
    }
    if (profile.experienceLevel.isNotBlank()) {
        return listOf(
            WpTimelineItem(
                title = "Experience: " + profile.experienceLevel,
                subtitle = profile.bio,
                dates = ""
            )
        )
    }
    return emptyList()
}

private fun wpAboutRows(profile: WorkerProfileData): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    if (profile.gender.isNotBlank()) rows.add("Gender" to profile.gender)
    if (profile.educationQualification.isNotBlank()) {
        rows.add("Education" to profile.educationQualification)
    }
    if (profile.dateOfBirth.isNotBlank()) rows.add("Age" to formatWorkerAge(profile.dateOfBirth))
    if (profile.languages.isNotEmpty()) rows.add("Languages" to profile.languages.joinToString(", "))
    if (profile.experienceLevel.isNotBlank() && profile.experience.isNotEmpty()) {
        rows.add("Experience" to profile.experienceLevel)
    }
    if (profile.bio.isNotBlank() && profile.experience.isNotEmpty()) rows.add("Bio" to profile.bio)
    return rows
}

private fun wpShare(context: android.content.Context, profile: WorkerProfileData) {
    val title = listOfNotNull(
        profile.skills.firstOrNull(),
        profile.location.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    val text = buildString {
        append(profile.fullName.ifBlank { "Worker" })
        if (title.isNotBlank()) append("\n").append(title)
        if (profile.skills.isNotEmpty()) append("\nSkills: ").append(profile.skills.joinToString(", "))
        append("\nShared via DutyPe")
    }
    runCatching {
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        context.startActivity(android.content.Intent.createChooser(send, "Share worker profile"))
    }
}

private fun wpDial(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    runCatching {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_DIAL,
                android.net.Uri.parse("tel:$phone")
            )
        )
    }
}

private fun Modifier.wpCard(): Modifier {
    val shape = RoundedCornerShape(16.dp)
    return this
        .fillMaxWidth()
        .background(Color.White, shape)
        .border(1.dp, WpBorder, shape)
}

@Composable
private fun WorkerProfileScreenBody(
    navController: NavController,
    workerProfile: WorkerProfileData?,
    application: JobApplication?,
    isLoading: Boolean,
    error: String?,
    reviews: List<Rating>,
    hasRatedWorker: Boolean,
    onRetry: () -> Unit,
    onRateWorkerClick: () -> Unit,
    onActionClick: (ApplicationAction) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WpBg)
    ) {
        WpTopBar(
            onBack = { navController.popBackStack() },
            onShare = { if (workerProfile != null) wpShare(context, workerProfile) }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                LoadingWorkerProfileState()
            } else if (error != null) {
                ErrorWorkerProfileState(error = error, onRetry = onRetry)
            } else if (workerProfile != null) {
                WpScrollContent(
                    profile = workerProfile,
                    application = application,
                    reviews = reviews,
                    onActionClick = onActionClick
                )
            }
        }
        if (!isLoading && error == null && workerProfile != null) {
            WpBottomBar(
                phone = workerProfile.phone,
                status = application?.status,
                hasRatedWorker = hasRatedWorker,
                onCall = { wpDial(context, workerProfile.phone) },
                onRateWorkerClick = onRateWorkerClick,
                onActionClick = onActionClick
            )
        }
    }
}

@Composable
private fun WpTopBar(onBack: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 4.dp)
            .height(52.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = WpNavy,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Share",
                tint = WpNavy,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun WpScrollContent(
    profile: WorkerProfileData,
    application: JobApplication?,
    reviews: List<Rating>,
    onActionClick: (ApplicationAction) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            WpHeroCard(profile = profile, reviewCount = reviews.size)
        }
        Spacer(modifier = Modifier.height(16.dp))
        WpTabStrip(selected = tab, onSelect = { tab = it })
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            if (tab == 0) {
                WpSkillsTab(profile)
            } else if (tab == 1) {
                WpReviewsTab(reviews)
            } else {
                WpContactTab(profile)
            }
            if (application?.status == ApplicationStatus.APPLIED) {
                Spacer(modifier = Modifier.height(16.dp))
                WpRejectButton(onClick = { onActionClick(ApplicationAction.REJECT) })
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WpHeroCard(profile: WorkerProfileData, reviewCount: Int) {
    Column(
        modifier = Modifier
            .wpCard()
            .padding(20.dp)
    ) {
        WpAvatar(profile)
        Spacer(modifier = Modifier.height(14.dp))
        WpNameRow(profile)
        val subtitle = listOfNotNull(
            profile.skills.firstOrNull(),
            profile.location.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 14.sp, color = WpMuted)
        }
        WpStatsRow(profile, reviewCount)
        if (profile.memberSince.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Member since " + profile.memberSince,
                fontSize = 11.sp,
                color = WpFaint
            )
        }
    }
}

@Composable
private fun WpAvatar(profile: WorkerProfileData) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(WpAvatarBg),
        contentAlignment = Alignment.Center
    ) {
        val imageUrl = profile.profileImageUrl
        if (!imageUrl.isNullOrBlank()) {
            com.example.dutype.components.OptimizedProfileImage(
                imageUrl = imageUrl,
                contentDescription = "Worker Profile",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val initials = profile.fullName
                .split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .ifEmpty { profile.fullName.take(1).uppercase() }
            if (initials.isNotBlank()) {
                Text(
                    text = initials,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = WpCobalt
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = WpCobalt,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun WpNameRow(profile: WorkerProfileData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = profile.fullName.ifBlank { "Worker" },
            modifier = Modifier.weight(1f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = WpInk
        )
        if (profile.isVerified) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Aadhaar ✓",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(WpGreenBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = WpGreen
            )
        }
    }
}

@Composable
private fun WpStatsRow(profile: WorkerProfileData, reviewCount: Int) {
    val hasRating = profile.rating > 0.0
    val hasJobs = profile.completedJobs > 0
    if (!hasRating && !hasJobs) return
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasRating) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = WpStar,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val ratingText = String.format(Locale.US, "%.1f", profile.rating)
                val label = if (reviewCount > 0) {
                    ratingText + " · " + reviewCount + " reviews"
                } else {
                    ratingText
                }
                Text(text = label, fontSize = 13.sp, color = WpMuted)
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
        if (hasJobs) {
            Text(
                text = profile.completedJobs.toString() + " Jobs Completed",
                fontSize = 13.sp,
                color = WpMuted
            )
        }
    }
}

@Composable
private fun WpTabStrip(selected: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WpTabs.forEachIndexed { index, label ->
                WpTabItem(
                    label = label,
                    active = index == selected,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(index) }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(WpBorder)
        )
    }
}

@Composable
private fun WpTabItem(
    label: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 12.dp),
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) WpNavy else WpMuted,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (active) WpNavy else Color.Transparent)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WpSkillsTab(profile: WorkerProfileData) {
    val history = wpBuildHistory(profile)
    val about = wpAboutRows(profile)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (profile.skills.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profile.skills.forEach { skill -> WpSkillPill(skill) }
            }
        }
        if (history.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            WpHistoryCard(history)
        }
        if (about.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            WpAboutCard(about)
        }
        if (profile.skills.isEmpty() && history.isEmpty() && about.isEmpty()) {
            Text(text = "No skills listed yet", fontSize = 14.sp, color = WpMuted)
        }
    }
}

@Composable
private fun WpSkillPill(skill: String) {
    val shape = RoundedCornerShape(17.dp)
    Box(
        modifier = Modifier
            .height(34.dp)
            .background(Color.White, shape)
            .border(1.dp, WpBorder, shape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = skill, fontSize = 13.sp, color = WpNavy)
    }
}

@Composable
private fun WpHistoryCard(items: List<WpTimelineItem>) {
    Column(
        modifier = Modifier
            .wpCard()
            .padding(16.dp)
    ) {
        Text(
            text = "Work History",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = WpInk
        )
        Spacer(modifier = Modifier.height(14.dp))
        items.forEachIndexed { index, item ->
            WpTimelineRow(item = item, isLast = index == items.lastIndex)
        }
    }
}

@Composable
private fun WpTimelineRow(item: WpTimelineItem, isLast: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier
                .width(10.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(WpNavy)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(WpBorder)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = WpInk
            )
            if (item.subtitle.isNotBlank()) {
                Text(text = item.subtitle, fontSize = 12.sp, color = WpMuted)
            }
            if (item.dates.isNotBlank()) {
                Text(text = item.dates, fontSize = 11.sp, color = WpFaint)
            }
        }
    }
}

@Composable
private fun WpAboutCard(rows: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .wpCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "About",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = WpInk
        )
        rows.forEach { row ->
            Column {
                Text(text = row.first, fontSize = 11.sp, color = WpFaint)
                Text(text = row.second, fontSize = 14.sp, color = WpNavy)
            }
        }
    }
}

@Composable
private fun WpReviewsTab(reviews: List<Rating>) {
    if (reviews.isEmpty()) {
        Text(
            text = "No reviews yet",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp,
            color = WpMuted,
            textAlign = TextAlign.Center
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        reviews.forEach { review -> WpReviewCard(review) }
    }
}

@Composable
private fun WpReviewCard(review: Rating) {
    val name = review.raterCompanyName.ifBlank { review.raterName }.ifBlank { "Employer" }
    Column(
        modifier = Modifier
            .wpCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = WpInk
        )
        Row {
            for (i in 1..5) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (i <= review.rating) WpStar else WpBorder,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (review.review.isNotBlank()) {
            Text(text = review.review, fontSize = 14.sp, color = WpBody)
        }
        if (review.createdAt > 0L) {
            Text(
                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(Date(review.createdAt)),
                fontSize = 11.sp,
                color = WpFaint
            )
        }
    }
}

@Composable
private fun WpContactTab(profile: WorkerProfileData) {
    val context = LocalContext.current
    val hasAny = profile.phone.isNotBlank() || profile.email.isNotBlank() ||
        profile.location.isNotBlank()
    if (!hasAny) {
        Text(
            text = "No contact details available",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp,
            color = WpMuted,
            textAlign = TextAlign.Center
        )
        return
    }
    Column(
        modifier = Modifier
            .wpCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (profile.phone.isNotBlank()) {
            WpContactRow(
                label = "Phone",
                value = profile.phone,
                valueColor = WpCobalt,
                onClick = { wpDial(context, profile.phone) }
            )
        }
        if (profile.email.isNotBlank()) {
            WpContactRow(
                label = "Email",
                value = profile.email,
                valueColor = WpNavy,
                onClick = null
            )
        }
        if (profile.location.isNotBlank()) {
            WpContactRow(
                label = "Location",
                value = profile.location,
                valueColor = WpNavy,
                onClick = null
            )
        }
    }
}

@Composable
private fun WpContactRow(
    label: String,
    value: String,
    valueColor: Color,
    onClick: (() -> Unit)?
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Column(modifier = rowModifier) {
        Text(text = label, fontSize = 11.sp, color = WpFaint)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun WpRejectButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .border(1.dp, WpBorder, shape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Reject application",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = WpDanger
        )
    }
}

@Composable
private fun WpBottomBar(
    phone: String,
    status: ApplicationStatus?,
    hasRatedWorker: Boolean,
    onCall: () -> Unit,
    onRateWorkerClick: () -> Unit,
    onActionClick: (ApplicationAction) -> Unit
) {
    val rightLabel: String? = when (status) {
        ApplicationStatus.APPLIED -> "✓ Hire for This Job"
        ApplicationStatus.HIRED -> "✓ Mark Work Done"
        ApplicationStatus.COMPLETED -> if (hasRatedWorker) "Rated Worker" else "Rate Worker"
        else -> null
    }
    if (phone.isBlank() && rightLabel == null) return
    val rightEnabled = !(status == ApplicationStatus.COMPLETED && hasRatedWorker)
    val onRight: () -> Unit = {
        when (status) {
            ApplicationStatus.APPLIED -> onActionClick(ApplicationAction.SHORTLIST)
            ApplicationStatus.HIRED -> onActionClick(ApplicationAction.MARK_COMPLETED)
            ApplicationStatus.COMPLETED -> onRateWorkerClick()
            else -> {}
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(WpBorder)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WpBarButton(
                label = "Call Worker",
                modifier = Modifier.weight(1f),
                filled = false,
                showPhone = true,
                enabled = phone.isNotBlank(),
                onClick = onCall
            )
            if (rightLabel != null) {
                WpBarButton(
                    label = rightLabel,
                    modifier = Modifier.weight(1.2f),
                    filled = true,
                    showPhone = false,
                    enabled = rightEnabled,
                    onClick = onRight
                )
            }
        }
    }
}

@Composable
private fun WpBarButton(
    label: String,
    modifier: Modifier,
    filled: Boolean,
    showPhone: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    val base = modifier
        .height(56.dp)
        .alpha(if (enabled) 1f else 0.45f)
        .clip(shape)
    val styled = if (filled) {
        base.background(WpNavy)
    } else {
        base
            .background(Color.White)
            .border(1.5.dp, WpNavy, shape)
    }
    Box(
        modifier = styled.clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showPhone) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = WpNavy,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (filled) FontWeight.Bold else FontWeight.SemiBold,
                color = if (filled) Color.White else WpNavy
            )
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
    val completedJobs: Int = 0,
    val isVerified: Boolean = false,
    val memberSince: String = ""
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

