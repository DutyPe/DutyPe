package com.example.dutype.employer.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dutype.app.R
import androidx.navigation.NavController
import com.example.dutype.employer.components.JobImageUploadSection
import com.example.dutype.employer.models.*
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.utils.JobEditPolicy
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditJobScreen(
    navController: NavController,
    jobId: String,
    viewModel: FirestoreEmployerJobViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // LocationService accessed via FirestoreJobViewModel (proper DI pattern)
    val jobViewModel: com.example.dutype.viewmodels.FirestoreJobViewModel = hiltViewModel()
    val locationService = jobViewModel.locationService
    
    // Saved work locations quick-pick (process-scoped, in-memory)
    val savedWorkLocationsStore: com.example.dutype.services.SavedWorkLocationsStore =
        hiltViewModel<com.example.dutype.viewmodels.WorkerHomeViewModel>().savedWorkLocationsStore
    val savedWorkLocations by savedWorkLocationsStore.locations.collectAsState()

    // Get the current job from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState.isUpdatingJob || uiState.isDeletingJob
    
    // Current job state
    var currentJob by remember { mutableStateOf<com.example.dutype.models.JobListing?>(null) }
    
    // Timing restriction - can't edit after 48 hours
    var canEditJob by remember { mutableStateOf(true) }
    var timeRestrictionMessage by remember { mutableStateOf("") }

    // Initialize form state with current job data
    var title by remember { mutableStateOf("") }
    var payAmount by remember { mutableStateOf("") }
    var payType by remember { mutableStateOf(PayType.DAILY) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var shiftTiming by remember { mutableStateOf(ShiftTiming.FLEXIBLE) }
    var vacancies by remember { mutableStateOf("") }
    var employerName by remember { mutableStateOf("") }
    var workType by remember { mutableStateOf("Part-time") }
    var experienceLevel by remember { mutableStateOf("No Experience Required") }
    var educationRequired by remember { mutableStateOf("No qualification required") }
    var gender by remember { mutableStateOf("Both") }
    var jobImageUri by remember { mutableStateOf<Uri?>(null) }
    var jobImageUrl by remember { mutableStateOf("") }
    var isUploadingJobImage by remember { mutableStateOf(false) }

    val workTypes = listOf("Part-time", "Full-time", "Contract", "Temporary", "Weekend Only", "Student-friendly")
    val baseExperienceLevels = listOf(
        "No Experience Required",
        "Fresher (Educated)",
        "1-3 years",
        "3-5 years",
        "5+ years"
    )
    val experienceLevels = remember(experienceLevel) {
        if (experienceLevel.isNotBlank() && experienceLevel !in baseExperienceLevels) {
            baseExperienceLevels + experienceLevel
        } else {
            baseExperienceLevels
        }
    }
    val baseEducationRequirements = listOf(
        "No qualification required",
        "10th pass",
        "12th pass",
        "ITI",
        "Diploma",
        "Graduate",
        "Any qualification"
    )
    val educationRequirements = remember(educationRequired) {
        if (educationRequired.isNotBlank() && educationRequired !in baseEducationRequirements) {
            baseEducationRequirements + educationRequired
        } else {
            baseEducationRequirements
        }
    }
    val genders = listOf("Male", "Female", "Both")

    // UI state
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoadingJob by remember { mutableStateOf(true) }
    
    // Saved work locations are read-only here; new addresses are saved only from Manage Addresses.
    var showSavedLocationsSheet by remember { mutableStateOf(false) }
    
    // Location coordinates for distance calculation
    var locationLatitude by remember { mutableStateOf(0.0) }
    var locationLongitude by remember { mutableStateOf(0.0) }

    // Load jobs first, then find the specific job
    LaunchedEffect(jobId) {
        Timber.d(" EditJobScreen - Loading job with ID: $jobId")
        
        // First load all jobs to ensure we have the latest data
        viewModel.loadMyJobs()
    }
    
    // Saved work locations now flow from SavedWorkLocationsStore via collectAsState above.
    
    // Add timeout for job loading (10 seconds)
    LaunchedEffect(jobId) {
        kotlinx.coroutines.delay(10000) // 10 seconds timeout
        if (isLoadingJob && currentJob == null) {
            Timber.w(" EditJobScreen - Job loading timeout, navigating back")
            isLoadingJob = false
            navController.popBackStack()
        }
    }
    
    // Load the specific job once jobs are loaded
    LaunchedEffect(uiState.myJobs, jobId) {
        Timber.d(" EditJobScreen - Jobs loaded: ${uiState.myJobs.size}, looking for jobId: $jobId")
        uiState.myJobs.forEach { job ->
            Timber.d(" EditJobScreen - Available job: ${job.id} - ${job.title}")
        }
        
        if (uiState.myJobs.isNotEmpty()) {
            // Bug #1 fix: Use the cached card ONLY as a skeleton placeholder so the
            // form renders instantly, but ALWAYS fetch the full merged document via
            // getJobById(). uiState.myJobs is sourced from getJobsByEmployer() which
            // reads only the public `jobmetadata` collection (no description,
            // contactNumber, vacancies, benefits). The merged read pulls `job_details`
            // too, so the edit form is correctly pre-filled.
            val existingJob = uiState.myJobs.find { it.id == jobId }
            if (existingJob != null) {
                Timber.d(" EditJobScreen - placeholder from cache: ${existingJob.title}")
                currentJob = existingJob
            }
            Timber.d(" EditJobScreen - fetching full merged job from repository")
            viewModel.getJobById(jobId) { job ->
                if (job != null) {
                    Timber.d(" EditJobScreen - merged job loaded: ${job.title}")
                    currentJob = job
                } else if (existingJob == null) {
                    Timber.w(" EditJobScreen - Job not found in repository")
                }
                isLoadingJob = false
            }
        }
    }
    
    // Observe current job and check timing restriction
    LaunchedEffect(currentJob) {
        val job = currentJob
        if (job != null) {
            try {
                Timber.d(" EditJobScreen - Job loaded: ${job.title}")
                Timber.d(" EditJobScreen - Job posted at: ${job.createdAt}")
                
                val currentTime = System.currentTimeMillis()
                val jobPostedTime = job.createdAt
                
                Timber.d(" EditJobScreen - Current time: $currentTime")
                Timber.d(" EditJobScreen - Job posted time: $jobPostedTime")
                Timber.d(" EditJobScreen - Time difference: ${currentTime - jobPostedTime}")
                Timber.d(" EditJobScreen - Edit window millis: ${JobEditPolicy.EDIT_WINDOW_MILLIS}")
                
                if (!JobEditPolicy.canEdit(jobPostedTime, currentTime)) {
                    canEditJob = false
                    timeRestrictionMessage = JobEditPolicy.blockedMessage(jobPostedTime, currentTime)
                    Timber.w(" EditJobScreen - Job cannot be edited after ${JobEditPolicy.EDIT_WINDOW_HOURS} hours")
                } else {
                    canEditJob = true
                    timeRestrictionMessage = ""
                    Timber.d(" EditJobScreen - Job can be edited")
                }
            } catch (e: Exception) {
                Timber.e(e, " EditJobScreen - Error processing job: ${e.message}")
                e.printStackTrace()
                canEditJob = false
                timeRestrictionMessage = "Error processing job: ${e.message}"
            }
        } else {
            Timber.d(" EditJobScreen - No job loaded yet")
        }
    }

    // Initialize form with current job data
    LaunchedEffect(currentJob) {
        currentJob?.let { job ->
            title = job.title
            payAmount = job.salary
            location = job.addressText.ifBlank { job.location }
            description = job.description
            contactNumber = job.contactNumber
            // Initialize location coordinates from existing job
            locationLatitude = job.lat
            locationLongitude = job.lng
            Timber.d(" EditJob: Loaded existing coordinates - lat: $locationLatitude, lon: $locationLongitude")
            // Pay type from stored salaryType ("HOURLY"|"DAILY"|"MONTHLY")
            payType = PayType.values().firstOrNull {
                it.name.equals(job.salaryType, ignoreCase = true) ||
                    it.displayName.equals(job.salaryType, ignoreCase = true)
            } ?: PayType.DAILY
            vacancies = job.vacancies.toString()
            employerName = job.companyName
            // Job type stored on `jobType` (Full-time / Part-time / …).
            workType = job.jobType.ifBlank { "Part-time" }
            experienceLevel = job.experienceRequired.ifBlank { "No Experience Required" }
            educationRequired = job.educationRequired.ifBlank { "No qualification required" }
            gender = when {
                job.gender.equals("Any", ignoreCase = true) -> "Both"
                job.gender.isBlank() -> "Both"
                else -> job.gender
            }
            jobImageUrl = job.jobImageUrl.orEmpty()
            jobImageUri = null
            val storedShiftTiming = job.shiftTiming.ifBlank { ShiftTiming.FLEXIBLE.displayName }
            val matchedShift = listOf(
                ShiftTiming.MORNING,
                ShiftTiming.NIGHT,
                ShiftTiming.BOTH,
                ShiftTiming.FLEXIBLE
            ).firstOrNull { shift ->
                shift.name.equals(storedShiftTiming, ignoreCase = true) ||
                    shift.displayName.equals(storedShiftTiming, ignoreCase = true)
            }
            shiftTiming = matchedShift ?: ShiftTiming.FLEXIBLE
            Timber.d(" EditJob: prefilled payType=$payType shift=$storedShiftTiming")
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isLoadingLocation = true
            locationError = null
            scope.launch {
                try {
                    // Use getHighAccuracyLocation with GPS-level precision (5-10m)
                    val locationInfo = locationService.getHighAccuracyLocation(
                        timeoutMs = 15000L,  // Wait up to 15 seconds for GPS fix
                        minAccuracyMeters = 10f  // Target 10m GPS precision
                    )
                    if (locationInfo != null) {
                        // Use detailed full address
                        location = locationInfo.getFullAddress()
                        // Store coordinates for distance calculation
                        locationLatitude = locationInfo.latitude
                        locationLongitude = locationInfo.longitude
                        Timber.d(" EditJob: Location set - lat: $locationLatitude, lon: $locationLongitude, accuracy: ${locationInfo.accuracy}m")
                    } else {
                        locationError = "Unable to get current location"
                    }
                } catch (e: Exception) {
                    locationError = "Error getting location: ${e.message}"
                } finally {
                    isLoadingLocation = false
                }
            }
        } else {
            locationError = "Location permission denied"
        }
    }

    // Validation function
    fun validateForm(): Boolean {
        val vacancyCount = vacancies.toIntOrNull()
        return title.isNotBlank() &&
                payAmount.isNotBlank() &&
                location.isNotBlank() &&
                description.isNotBlank() &&
                contactNumber.isNotBlank() &&
                vacancyCount != null &&
                vacancyCount in 1..50 &&
                !isUploadingJobImage
    }

    // Update function
    fun updateJob() {
        currentJob?.let { originalJob ->
            if (validateForm()) {
                scope.launch {
                    // If coordinates are 0,0 (user typed location manually), try to geocode
                    var finalLatitude = locationLatitude
                    var finalLongitude = locationLongitude
                    val locationChanged = location != originalJob.addressText.ifBlank { originalJob.location }
                    
                    if (locationLatitude == 0.0 && locationLongitude == 0.0 && location.isNotBlank()) {
                        Timber.d(" EDIT JOB: Geocoding manual location: $location")
                        val geocodedLocation = locationService.getCoordinatesFromAddress(location)
                        if (geocodedLocation != null) {
                            finalLatitude = geocodedLocation.latitude
                            finalLongitude = geocodedLocation.longitude
                            Timber.d(" EDIT JOB: Geocoded - lat: $finalLatitude, lon: $finalLongitude")
                        } else if (!locationChanged) {
                            Timber.w(" EDIT JOB: Geocoding failed, using original coordinates")
                            finalLatitude = originalJob.lat
                            finalLongitude = originalJob.lng
                        } else {
                            locationError = context.getString(R.string.valid_job_location_required)
                            return@launch
                        }
                    }
                    
                    if (!com.example.dutype.utils.GeoUtils.hasValidCoordinates(finalLatitude, finalLongitude)) {
                        locationError = context.getString(R.string.valid_job_location_required)
                        return@launch
                    }

                    val finalShiftTiming = shiftTiming.displayName

                    val updates = mapOf(
                        "title" to title,
                        "salary" to payAmount.trim(),
                        "salaryType" to payType.name,
                        "location" to mapOf("lat" to finalLatitude, "lng" to finalLongitude),
                        "addressText" to location,
                        "description" to description,
                        "contactNumber" to contactNumber,
                        "vacancies" to (vacancies.toIntOrNull() ?: return@launch),
                        // Save the chosen work mode (Part-time / Full-time / …)
                        // as `jobType`.
                        "jobType" to workType,
                        "category" to com.example.dutype.utils.JobCategoryResolver.inferCategoryName(title, description),
                        "shiftTiming" to finalShiftTiming,
                        "experienceRequired" to experienceLevel,
                        "educationRequired" to educationRequired,
                        "gender" to gender,
                        "jobImageUrl" to jobImageUrl
                    )
                    
                    Timber.d(" EDIT JOB: Updating job with coordinates - lat: $finalLatitude, lon: $finalLongitude")
                    
                    viewModel.updateJob(originalJob.id, updates) { success, error ->
                        if (success) {
                            navController.popBackStack()
                        } else {
                            // Handle error - could show a toast or error message
                            Timber.e("âŒ Failed to update job: $error")
                        }
                    }
                }
            }
        }
    }

    // Delete function
    fun deleteJob() {
        currentJob?.let { job ->
            viewModel.deleteJob(job.id) { success, error ->
                if (success) {
                    navController.popBackStack()
                } else {
                    // Handle error - could show a toast or error message
                    Timber.e("âŒ Failed to delete job: $error")
                }
            }
        }
    }

    // Show loading indicator while job is being loaded
    if (isLoadingJob || currentJob == null) {
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
                    text = "Loading job details...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = "Please wait while we fetch your job information",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars), // Add status bar padding
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF3F4F6)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1F2937),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "Edit Job",
                        style = AppTypography.screenTitle.copy(
                            color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (canEditJob) {
                        Surface(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Job",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Column {
                    // Show timing restriction message if applicable
                    if (!canEditJob && timeRestrictionMessage.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 8.dp, 16.dp, 0.dp),
                            color = Color(0xFFFFF3CD),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFF856404),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = timeRestrictionMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF856404),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Text(
                                "Cancel",
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = { updateJob() },
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp),
                            enabled = !isLoading && validateForm() && canEditJob,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6),
                                disabledContainerColor = Color(0xFFE5E7EB)
                            )
                        ) {
                            if (isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Text(stringResource(R.string.updating), color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                Text(stringResource(R.string.update_job), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    
                    // Navigation bar spacer
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                    )
                }
            }
        },
        containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Job Title
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Job Title",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                        }
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("e.g., Cook, Driver, Cleaner", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                    }
                }
            }

            // Pay Information
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.payment_details),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                        }

                        OutlinedTextField(
                            value = payAmount,
                            onValueChange = { payAmount = it },
                            label = { Text(stringResource(R.string.amount)) },
                            placeholder = { Text("e.g., 12000 or 10000-12000", color = Color(0xFF9CA3AF)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )

                        Text(
                            text = "Pay type",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PayType.values().toList()) { type ->
                                val selected = payType == type
                                FilterChip(
                                    selected = selected,
                                    onClick = { payType = type },
                                    modifier = Modifier.height(34.dp),
                                    label = {
                                        Text(
                                            text = type.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF3B82F6),
                                        selectedLabelColor = Color.White,
                                        containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
                                        labelColor = Color(0xFF374151)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = Color(0xFFE5E7EB),
                                        selectedBorderColor = Color(0xFF3B82F6)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Location Section
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Job Location",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                        }

                        com.example.dutype.components.LocationAutocompleteField(
                            value = location,
                            onValueChange = { location = it },
                            onLocationSelected = { address, lat, lng ->
                                location = address
                                locationLatitude = lat
                                locationLongitude = lng
                                Timber.d(" EditJob: Location selected - $address at ($lat, $lng)")
                            },
                            locationService = locationService,
                            label = stringResource(R.string.work_location),
                            placeholder = stringResource(R.string.search_location_or_gps),
                            showCurrentLocationButton = true,
                            onCurrentLocationClick = {
                                if (locationService.hasLocationPermission()) {
                                    isLoadingLocation = true
                                    locationError = null
                                    scope.launch {
                                        try {
                                            val locationInfo = locationService.getHighAccuracyLocation(
                                                timeoutMs = 15000L,
                                                minAccuracyMeters = 10f
                                            )
                                            if (locationInfo != null) {
                                                location = locationInfo.getFullAddress()
                                                locationLatitude = locationInfo.latitude
                                                locationLongitude = locationInfo.longitude
                                                Timber.d(" EditJob: GPS location - lat: $locationLatitude, lon: $locationLongitude")
                                            } else {
                                                locationError = "Unable to get current location"
                                            }
                                        } catch (e: Exception) {
                                            locationError = "Error getting location: ${e.message}"
                                        } finally {
                                            isLoadingLocation = false
                                        }
                                    }
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )

                        locationError?.let { error ->
                            Text(
                                text = error,
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        if (savedWorkLocations.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showSavedLocationsSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF10B981)
                                )
                            ) {
                                Icon(
                                    Icons.Default.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.saved_count, savedWorkLocations.size), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Job Description
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Job Description",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                        }
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text(stringResource(R.string.describe_job_placeholder), color = Color(0xFF9CA3AF)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                    }
                }
            }

            // Job Image
            item {
                JobImageUploadSection(
                    selectedImageUri = jobImageUri ?: jobImageUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) },
                    isUploading = isUploadingJobImage,
                    onImageSelected = { uri ->
                        jobImageUri = uri
                        scope.launch {
                            isUploadingJobImage = true
                            try {
                                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                if (currentUser == null) {
                                    Toast.makeText(context, "Please login to upload image", Toast.LENGTH_SHORT).show()
                                    jobImageUri = null
                                    return@launch
                                }

                                val fileName = "job_image_${System.currentTimeMillis()}.jpg"
                                val storagePath = "job_images/${currentUser.uid}/$fileName"
                                val uploadResult = com.example.dutype.utils.ImageUploadUtils.uploadWithRetry(
                                    context = context,
                                    uri = uri,
                                    storagePath = storagePath
                                )

                                when (uploadResult) {
                                    is com.example.dutype.utils.ImageUploadUtils.UploadResult.Success -> {
                                        jobImageUrl = uploadResult.downloadUrl
                                        Toast.makeText(context, context.getString(R.string.post_job_image_uploaded), Toast.LENGTH_SHORT).show()
                                    }
                                    is com.example.dutype.utils.ImageUploadUtils.UploadResult.Failure -> {
                                        Timber.e(uploadResult.exception, "Edit job image upload failed: ${uploadResult.error}")
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.post_job_image_upload_failed, uploadResult.error),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        jobImageUri = null
                                    }
                                    else -> Unit
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Edit job image upload failed")
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.post_job_image_upload_failed, e.message ?: ""),
                                    Toast.LENGTH_SHORT
                                ).show()
                                jobImageUri = null
                            } finally {
                                isUploadingJobImage = false
                            }
                        }
                    },
                    onImageRemoved = {
                        jobImageUri = null
                        jobImageUrl = ""
                    }
                )
            }

            // Contact Information
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFDBEAFE), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Contact Information",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                        }
                        OutlinedTextField(
                            value = contactNumber,
                            onValueChange = { contactNumber = it },
                            label = { Text(stringResource(R.string.contact_number_label)) },
                            placeholder = { Text("e.g., +91 9876543210", color = Color(0xFF9CA3AF)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                        OutlinedTextField(
                            value = employerName,
                            onValueChange = { employerName = it },
                            label = { Text(stringResource(R.string.your_name_optional)) },
                            placeholder = { Text(stringResource(R.string.enter_your_name), color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                    }
                }
            }

            // Additional Details
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFFCE7F3), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFFDB2777),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Additional Details",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                        }

                        // Work type
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Work type",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF374151)
                                )
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(workTypes) { type ->
                                    val selected = workType == type
                                    FilterChip(
                                        onClick = { workType = type },
                                        label = {
                                            Text(
                                                type,
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                            )
                                        },
                                        selected = selected,
                                        modifier = Modifier.height(34.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF3B82F6),
                                            selectedLabelColor = Color.White,
                                            containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
                                            labelColor = Color(0xFF374151)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = Color(0xFFE5E7EB),
                                            selectedBorderColor = Color(0xFF3B82F6)
                                        )
                                    )
                                }
                            }
                        }

                        // Shift
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Shift",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF374151)
                                )
                            )
                            val shiftOptions = listOf(
                                ShiftTiming.MORNING,
                                ShiftTiming.NIGHT,
                                ShiftTiming.BOTH,
                                ShiftTiming.FLEXIBLE
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(shiftOptions) { option ->
                                    val selected = shiftTiming == option
                                    FilterChip(
                                        onClick = { shiftTiming = option },
                                        label = {
                                            Text(
                                                option.displayName,
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                            )
                                        },
                                        selected = selected,
                                        modifier = Modifier
                                            .height(34.dp)
                                            .defaultMinSize(minWidth = 0.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF3B82F6),
                                            selectedLabelColor = Color.White,
                                            containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
                                            labelColor = Color(0xFF374151)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = Color(0xFFE5E7EB),
                                            selectedBorderColor = Color(0xFF3B82F6)
                                        )
                                    )
                                }
                            }
                        }

                        // Urgency removed.

                        // Vacancies
                        OutlinedTextField(
                            value = vacancies,
                            onValueChange = { vacancies = it },
                            label = { Text(stringResource(R.string.number_of_vacancies)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                    }
                }
            }

            // Requirements
            item {
                RequirementsSection(
                    experienceLevel = experienceLevel,
                    onExperienceLevelChange = { experienceLevel = it },
                    experienceLevels = experienceLevels,
                    educationRequired = educationRequired,
                    onEducationRequiredChange = { educationRequired = it },
                    educationRequirements = educationRequirements,
                    gender = gender,
                    onGenderChange = { gender = it },
                    genders = genders
                )
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_job)) },
            text = { Text(stringResource(R.string.delete_job_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteJob()
                        showDeleteDialog = false
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(stringResource(R.string.deleting), color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Saved Locations Bottom Sheet (Industry standard pattern - Uber, Swiggy, Zomato)
    if (showSavedLocationsSheet) {
        AlertDialog(
            onDismissRequest = { showSavedLocationsSheet = false },
            title = { 
                Text(
                    "Saved Work Locations",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                ) 
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(savedWorkLocations) { workLocation ->
                        Surface(
                            onClick = {
                                // Use saved location
                                location = workLocation.address
                                locationLatitude = workLocation.latitude
                                locationLongitude = workLocation.longitude

                                showSavedLocationsSheet = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF9FAFB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        workLocation.label,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        workLocation.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 2
                                    )
                                    if (workLocation.usageCount > 0) {
                                        Text(
                                            "Used ${workLocation.usageCount} times",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF10B981),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSavedLocationsSheet = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

