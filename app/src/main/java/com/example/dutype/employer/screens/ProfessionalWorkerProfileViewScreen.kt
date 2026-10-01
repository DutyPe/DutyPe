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
    val applicationViewModel: com.example.dutype.viewmodels.EmployerApplicationViewModel = hiltViewModel()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val profileCompletionService = profileCompletionViewModel.profileCompletionService
    val ratingService = remember { com.example.dutype.di.ratingServiceFromHilt(context) }

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

            // The application (status, job) and the profile the callable allows this employer to see.
            application = applicationId?.let { applicationViewModel.getApplication(it) }
            application?.let { app ->
                workerProfile = WorkerProfileData(
                    workerId = app.workerId,
                    fullName = app.workerName.ifBlank { "Worker" },
                    phone = "",
                    email = "",
                    location = "",
                    gender = "",
                    profileImageUrl = app.workerPhoto.ifBlank { null },
                    experience = emptyList(),
                    skills = listOf(app.workerSkill).filter { it.isNotBlank() },
                    languages = emptyList(),
                    experienceLevel = "",
                    dateOfBirth = "",
                    educationQualification = "",
                    bio = ""
                )
            }

            profileCompletionService.getWorkerProfileForEmployer(
                workerId = workerId,
                jobId = application?.jobId?.takeIf { it.isNotBlank() }
            ).fold(
                onSuccess = { data ->
                    val P = com.example.dutype.firestore.FirestoreSchema.WorkerProfiles
                    val C = com.example.dutype.firestore.FirestoreSchema.WorkerCards
                    val years = (data[P.EXPERIENCE_YEARS] as? Number)?.toInt() ?: 0
                    workerProfile = WorkerProfileData(
                        workerId = workerId,
                        fullName = (data[P.NAME] as? String).orEmpty().ifBlank { workerProfile?.fullName ?: "Worker" },
                        phone = (data[P.PHONE] as? String).orEmpty(),
                        email = "",
                        location = (data[P.AREA] as? String).orEmpty(),
                        gender = (data[P.GENDER] as? String).orEmpty(),
                        profileImageUrl = (data[P.PHOTO_URL] as? String)?.ifBlank { null } ?: workerProfile?.profileImageUrl,
                        experience = emptyList(),
                        skills = (data[P.SKILLS] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                            .map { com.example.dutype.employer.models.JobCategory.fromKey(it).displayName },
                        languages = emptyList(),
                        experienceLevel = if (years > 0) "$years years" else context.getString(R.string.fresher_label),
                        dateOfBirth = (data[P.DATE_OF_BIRTH] as? String).orEmpty(),
                        educationQualification = (data[P.EDUCATION] as? String).orEmpty(),
                        bio = (data[P.BIO] as? String).orEmpty(),
                        rating = (data[C.RATING] as? Number)?.toDouble() ?: 0.0,
                        totalJobs = (data[C.JOBS_COMPLETED] as? Number)?.toInt() ?: 0,
                        completedJobs = (data[C.JOBS_COMPLETED] as? Number)?.toInt() ?: 0
                    )
                },
                onFailure = { e -> if (workerProfile == null) error = e.message ?: "Failed to load worker profile" }
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
            targetName = workerProfile?.fullName?.ifBlank { application?.workerName.orEmpty() } ?: stringResource(R.string.this_worker),
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
                                Toast.makeText(context, error.message ?: context.getString(R.string.failed_to_submit_rating), Toast.LENGTH_LONG).show()
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
                                    applicationViewModel.updateApplicationStatusForResult(app.id, ApplicationStatus.HIRED).getOrThrow()
                                    application = app.copy(status = ApplicationStatus.HIRED)
                                }
                            }
            ApplicationAction.REJECT -> {
                                application?.let { app ->
                                    applicationViewModel.updateApplicationStatusForResult(app.id, ApplicationStatus.REJECTED).getOrThrow()
                                    application = app.copy(status = ApplicationStatus.REJECTED)
                                }
                            }
                            ApplicationAction.MARK_COMPLETED -> {
                                application?.let { app ->
                                    applicationViewModel.updateApplicationStatusForResult(app.id, ApplicationStatus.COMPLETED).getOrThrow()
                                    application = app.copy(status = ApplicationStatus.COMPLETED)
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
                    ApplicationAction.SHORTLIST -> stringResource(R.string.accept_candidate)
                    ApplicationAction.REJECT -> stringResource(R.string.reject_application)
                    ApplicationAction.MARK_COMPLETED -> stringResource(R.string.mark_work_done)
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Text(
                text = when (action) {
                    ApplicationAction.SHORTLIST -> stringResource(R.string.accept_candidate_confirm_msg, workerName)
                    ApplicationAction.REJECT -> stringResource(R.string.reject_application_confirm_msg, workerName)
                    ApplicationAction.MARK_COMPLETED -> stringResource(R.string.mark_completed_confirm_msg, workerName)
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
private val WpTabs = listOf(R.string.wp_tab_skills_exp, R.string.wp_tab_reviews, R.string.wp_tab_contact)

private data class WpTimelineItem(
    val title: String,
    val subtitle: String,
    val dates: String
)

private fun wpBuildHistory(profile: WorkerProfileData, expLabel: String): List<WpTimelineItem> {
    if (profile.experience.isNotEmpty()) {
        return profile.experience.map {
            WpTimelineItem(it.position, it.company, it.duration)
        }
    }
    if (profile.experienceLevel.isNotBlank()) {
        return listOf(
            WpTimelineItem(
                title = "$expLabel: " + profile.experienceLevel,
                subtitle = profile.bio,
                dates = ""
            )
        )
    }
    return emptyList()
}

private fun wpAboutRows(context: android.content.Context, profile: WorkerProfileData): List<Pair<Int, String>> {
    val rows = mutableListOf<Pair<Int, String>>()
    if (profile.gender.isNotBlank()) rows.add(R.string.gender to profile.gender)
    if (profile.educationQualification.isNotBlank()) {
        rows.add(R.string.education to profile.educationQualification)
    }
    if (profile.dateOfBirth.isNotBlank()) rows.add(R.string.age_label to formatWorkerAge(context, profile.dateOfBirth))
    if (profile.languages.isNotEmpty()) rows.add(R.string.languages_label to profile.languages.joinToString(", "))
    if (profile.experienceLevel.isNotBlank() && profile.experience.isNotEmpty()) {
        rows.add(R.string.experience to profile.experienceLevel)
    }
    if (profile.bio.isNotBlank() && profile.experience.isNotEmpty()) rows.add(R.string.bio_label to profile.bio)
    return rows
}

private fun wpShare(context: android.content.Context, profile: WorkerProfileData) {
    val title = listOfNotNull(
        profile.skills.firstOrNull(),
        profile.location.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    val text = buildString {
        append(profile.fullName.ifBlank { context.getString(R.string.worker) })
        if (title.isNotBlank()) append("\n").append(title)
        if (profile.skills.isNotEmpty()) append("\n").append(context.getString(R.string.skills)).append(": ").append(profile.skills.joinToString(", "))
        append("\n").append(context.getString(R.string.shared_via_dutype))
    }
    runCatching {
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        context.startActivity(android.content.Intent.createChooser(send, context.getString(R.string.share_worker_profile)))
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
                contentDescription = stringResource(R.string.back),
                tint = WpNavy,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.share),
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
                text = stringResource(R.string.emp_profile_member_since, profile.memberSince),
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
                    contentDescription = null,
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
            text = profile.fullName.ifBlank { stringResource(R.string.worker) },
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
                    stringResource(R.string.rating_reviews_format, ratingText, reviewCount)
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
                text = stringResource(R.string.jobs_completed_format, profile.completedJobs),
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
            WpTabs.forEachIndexed { index, labelRes ->
                WpTabItem(
                    label = stringResource(labelRes),
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
    val context = LocalContext.current
    val history = wpBuildHistory(profile, stringResource(R.string.experience))
    val about = wpAboutRows(context, profile)
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
            Text(text = stringResource(R.string.no_skills_listed_yet), fontSize = 14.sp, color = WpMuted)
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
            text = stringResource(R.string.work_history),
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
private fun WpAboutCard(rows: List<Pair<Int, String>>) {
    Column(
        modifier = Modifier
            .wpCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.about),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = WpInk
        )
        rows.forEach { row ->
            Column {
                Text(text = stringResource(row.first), fontSize = 11.sp, color = WpFaint)
                Text(text = row.second, fontSize = 14.sp, color = WpNavy)
            }
        }
    }
}

@Composable
private fun WpReviewsTab(reviews: List<Rating>) {
    if (reviews.isEmpty()) {
        Text(
            text = stringResource(R.string.no_reviews_yet),
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
    val name = review.raterCompanyName.ifBlank { review.raterName }.ifBlank { stringResource(R.string.employer) }
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
            text = stringResource(R.string.no_contact_details_available),
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
                label = stringResource(R.string.phone_label),
                value = profile.phone,
                valueColor = WpCobalt,
                onClick = { wpDial(context, profile.phone) }
            )
        }
        if (profile.email.isNotBlank()) {
            WpContactRow(
                label = stringResource(R.string.email),
                value = profile.email,
                valueColor = WpNavy,
                onClick = null
            )
        }
        if (profile.location.isNotBlank()) {
            WpContactRow(
                label = stringResource(R.string.location),
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
            text = stringResource(R.string.reject_application),
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
        ApplicationStatus.APPLIED -> stringResource(R.string.hire_for_this_job)
        ApplicationStatus.HIRED -> stringResource(R.string.mark_work_done)
        ApplicationStatus.COMPLETED -> if (hasRatedWorker) stringResource(R.string.rated_worker) else stringResource(R.string.rate_worker)
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
                label = stringResource(R.string.call_worker),
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

private fun formatWorkerAge(context: android.content.Context, dateOfBirth: String): String {
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
    return if (age > 0) context.getString(R.string.years_old_format, age) else dateOfBirth
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

