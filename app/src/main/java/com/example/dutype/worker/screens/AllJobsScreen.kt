package com.example.dutype.worker.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.navigation.Routes
import com.example.dutype.services.JobApplicationService
import com.example.dutype.services.NotificationService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.ui.components.ReusableSearchBar
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.utils.JobCardShimmer
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.JobApplicationViewModel
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.worker.components.JobCard
import com.example.dutype.worker.models.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun AllJobsScreen(
    navController: NavController,
    initialFilter: String = "All Jobs",
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    val jobApplicationViewModel: JobApplicationViewModel = hiltViewModel()
    val smartApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    val jobApplicationService: JobApplicationService = remember {
        JobApplicationService(
            notificationService = NotificationService(
                context = context,
                firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            ),
            profileCompletionService = ProfileCompletionService(),
            applicationStateManager = ApplicationStateManager()
        )
    }
    
    val jobUiState by jobViewModel.uiState.collectAsState()
    val jobApplicationUiState by jobApplicationViewModel.uiState.collectAsStateWithLifecycle()
    val applications = jobApplicationUiState.applications
    
    // View tracking state
    var jobVacancyStatuses by remember { mutableStateOf<Map<String, JobVacancyStatus>>(emptyMap()) }
    
    // Profile completion
    var canApplyDirectly by remember { mutableStateOf(false) }
    
    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf(initialFilter) }
    
    // Set status bar color
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        jobViewModel.loadJobs()
        jobApplicationViewModel.loadMyApplications()
    }
    
    // Load profile completion status
    LaunchedEffect(Unit) {
        try {
            val status = profileCompletionViewModel.getProfileSetupStatus(com.example.dutype.models.UserRole.WORKER)
            canApplyDirectly = status.isComplete
        } catch (e: Exception) {
            Timber.e(e, "Error loading profile status")
        }
    }
    
    // Load vacancy statuses
    LaunchedEffect(jobUiState.jobs) {
        jobUiState.jobs.forEach { job ->
            jobApplicationService.getJobVacancyStatus(job.jobId).onSuccess { status ->
                jobVacancyStatuses = jobVacancyStatuses + (job.jobId to status)
            }
        }
    }

    // Filter chips
    val filterChips = listOf(
        "All Jobs" to Icons.Default.Star,
        "Daily Jobs" to Icons.Default.CalendarToday,
        "Hourly Jobs" to Icons.Default.AccessTime,
        "Nearby" to Icons.Default.LocationOn,
        "Part Time" to Icons.Default.Work,
        "Full Time" to Icons.Default.CheckCircle
    )
    
    // Filter jobs based on selected chip and search query
    val filteredJobs = remember(selectedChip, jobUiState.jobs, jobVacancyStatuses, searchQuery, applications) {
        // First filter out jobs that worker has already applied to
        val nonAppliedJobs = jobUiState.jobs.filter { job ->
            !applications.any { app -> app.jobId == job.jobId }
        }
        
        // Filter out filled jobs (by vacancy status or isFilled flag)
        val availableJobs = nonAppliedJobs.filter { job ->
            jobVacancyStatuses[job.jobId] != JobVacancyStatus.FILLED && !job.isFilled
        }
        
        // Filter out expired jobs
        val activeJobs = availableJobs.filter { job ->
            !job.isExpired()
        }
        
        val chipFiltered = when (selectedChip) {
            "All Jobs" -> activeJobs
            "Daily Jobs" -> activeJobs.filter {
                it.payType.equals("DAILY", true) ||
                        it.payType.contains("day", true) ||
                        it.jobType.equals("Daily", true)
            }
            "Hourly Jobs" -> activeJobs.filter {
                it.payType.equals("HOURLY", true) ||
                        it.payType.contains("hour", true) ||
                        it.jobType.equals("Hourly", true)
            }
            "Nearby" -> activeJobs.filter { job ->
                job.distance != null && job.distance!! < 10.0
            }.sortedBy { it.distance }
            "Part Time" -> activeJobs.filter {
                it.jobType.equals("Part-time", true) ||
                        it.jobType.contains("part", true)
            }
            "Full Time" -> activeJobs.filter {
                it.jobType.equals("Full-time", true) ||
                        it.jobType.contains("full", true)
            }
            else -> activeJobs
        }
        
        // Apply search filter
        if (searchQuery.isNotBlank()) {
            chipFiltered.filter { job ->
                job.title.contains(searchQuery, ignoreCase = true) ||
                        job.companyName.contains(searchQuery, ignoreCase = true) ||
                        job.category.contains(searchQuery, ignoreCase = true) ||
                        job.location.contains(searchQuery, ignoreCase = true)
            }
        } else {
            chipFiltered
        }
    }
    
    // Apply for job function
    val applyForJob: (String) -> Unit = { jobId ->
        if (canApplyDirectly) {
            Toast.makeText(context, "Applying for job...", Toast.LENGTH_SHORT).show()
            smartApplicationViewModel.applyForJob(jobId)
            navController.navigate(Routes.WORKER_MY_JOBS)
        } else {
            Toast.makeText(context, "Please complete your profile first", Toast.LENGTH_LONG).show()
            navController.navigate(Routes.PROFILE_SETUP)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Common Header for consistency
        CommonHeader(
            title = "All Jobs",
            subtitle = if (!jobUiState.isLoading) "${filteredJobs.size} jobs available" else null,
            onBackClick = { navController.popBackStack() },
            backgroundColor = Color.White
        )
        
        // Search and Filter Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Search bar with map icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ReusableSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search jobs, companies...",
                        height = 48,
                        backgroundColor = Color(0xFFF1F5F9),
                        borderColor = Color.Transparent,
                        focusedBorderColor = Color(0xFF1F2937),
                        searchIconColor = Color(0xFF6B7280),
                        textColor = Color(0xFF1F2937),
                        placeholderColor = Color(0xFF9CA3AF),
                        cornerRadius = 12,
                        fontSize = 14
                    )
                }
                
                // Map view button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { navController.navigate(Routes.WORKER_JOB_MAP) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = "Map View",
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        
        // Filter chips section
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterChips) { (chip, icon) ->
                FilterChip(
                    onClick = { selectedChip = chip },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedChip == chip) Color.White else Color(0xFF374151)
                            )
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (selectedChip == chip) Color.White else Color(0xFF374151)
                                )
                            )
                        }
                    },
                    selected = selectedChip == chip,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1F2937),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color(0xFF374151)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedChip == chip,
                        borderColor = Color(0xFFE5E7EB),
                        selectedBorderColor = Color(0xFF1F2937),
                        borderWidth = 1.dp
                    )
                )
            }
        }
        
        // Divider
        HorizontalDivider(
            color = Color(0xFFE5E7EB),
            thickness = 1.dp
        )

        // Job list
        when {
            jobUiState.isLoading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(6) {
                        JobCardShimmer()
                    }
                }
            }
            
            jobUiState.hasError -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Something went wrong",
                            style = AppTypography.emptyStateTitle.copy(
                                color = Color(0xFF374151)
                            )
                        )
                        Text(
                            text = jobUiState.error ?: "Unable to load jobs. Please try again.",
                            style = AppTypography.emptyStateSubtitle.copy(
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        )
                        Button(
                            onClick = { jobViewModel.loadJobs() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1F2937)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Try Again")
                        }
                    }
                }
            }
            
            filteredJobs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkOff,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Text(
                            text = "No Jobs Found",
                            style = AppTypography.emptyStateTitle.copy(
                                color = Color(0xFF374151)
                            )
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) 
                                "No jobs match \"$searchQuery\". Try different keywords."
                            else 
                                "No $selectedChip available right now.\nTry a different filter or check back later.",
                            style = AppTypography.emptyStateSubtitle.copy(
                                color = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                        )
                        if (selectedChip != "All Jobs") {
                            TextButton(
                                onClick = { selectedChip = "All Jobs" }
                            ) {
                                Text(
                                    text = "View All Jobs",
                                    color = Color(0xFF1F2937),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredJobs) { job ->
                        val vacancyStatus = jobVacancyStatuses[job.jobId] ?: JobVacancyStatus.OPEN
                        val isFilled = vacancyStatus == JobVacancyStatus.FILLED
                        
                        val jobCard = JobCardModel(
                            jobId = job.id,
                            title = job.title,
                            employerName = job.companyName,
                            payInfo = PayInfo(
                                amount = cleanPaymentAmount(
                                    (job.payAmount.ifEmpty { job.salary }).ifEmpty {
                                        if (job.payRate > 0.0) job.payRate.toInt().toString() else ""
                                    }
                                ),
                                type = when {
                                    job.payType.equals("HOURLY", true) || job.payType.contains("hour", true) -> PayType.HOURLY
                                    job.payType.equals("DAILY", true) || job.payType.contains("day", true) -> PayType.DAILY
                                    job.payType.equals("MONTHLY", true) || job.payType.contains("month", true) -> PayType.MONTHLY
                                    job.payType.contains("delivery", true) || job.payType.contains("task", true) -> PayType.PER_TASK
                                    else -> PayType.DAILY
                                },
                                period = job.payType
                            ),
                            location = LocationInfo(
                                area = job.area ?: job.location,
                                city = job.city ?: job.location,
                                distance = job.distance?.let { com.example.dutype.location.formatDistance(it) } ?: "N/A"
                            ),
                            tags = listOf(
                                JobTag(text = job.jobType, emoji = "\uD83D\uDCBC", type = TagType.BENEFIT),
                                JobTag(text = job.category, emoji = "\uD83C\uDFF7\uFE0F", type = TagType.BENEFIT)
                            ),
                            timeInfo = TimeInfo(
                                postedTime = job.postedDate,
                                urgency = if (job.isUrgent()) UrgencyLevel.URGENT else UrgencyLevel.NORMAL
                            ),
                            phoneNumber = job.contactNumber,
                            description = job.description,
                            jobType = job.jobType,
                            vacancies = job.vacancies,
                            isSaved = job.isSaved,
                            isFilled = isFilled,
                            employerId = job.employerId,
                            hiringUrgency = job.urgency,
                            employerTrustTier = job.employerTrustTier,
                            jobImageUrl = job.jobImageUrl
                        )
                        
                        JobCard(
                            jobCard = jobCard,
                            isSaved = job.isSaved,
                            hasApplied = applications.any { it.jobId == job.jobId },
                            onApplyClick = { applyForJob(job.jobId) },
                            onSaveClick = {
                                if (job.isSaved) {
                                    savedJobsViewModel.unsaveJob(job.jobId)
                                } else {
                                    savedJobsViewModel.saveJob(job.jobId)
                                }
                            },
                            onCardClick = {
                                navController.navigate(Routes.jobDetailRoute(job.jobId))
                            },
                            employerTrustTier = job.employerTrustTier
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper function to clean payment amount
 */
private fun cleanPaymentAmount(amount: String): String {
    return amount
        .replace("/hourly", "", ignoreCase = true)
        .replace("/daily", "", ignoreCase = true)
        .replace("/monthly", "", ignoreCase = true)
        .replace("per task", "", ignoreCase = true)
        .replace("hourly", "", ignoreCase = true)
        .replace("daily", "", ignoreCase = true)
        .replace("monthly", "", ignoreCase = true)
        .replace("per hour", "", ignoreCase = true)
        .replace("per day", "", ignoreCase = true)
        .replace("per month", "", ignoreCase = true)
        .replace("Rs.", "", ignoreCase = true)
        .replace("rs", "", ignoreCase = true)
        .replace("/", "")
        .trim()
}
