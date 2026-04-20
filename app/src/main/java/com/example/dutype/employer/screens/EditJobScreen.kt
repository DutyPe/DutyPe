package com.example.dutype.employer.screens

import android.Manifest
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
import com.example.dutype.employer.models.*
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.ui.theme.AppTypography
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
    var category by remember { mutableStateOf(JobCategory.COOK) }
    var shiftTiming by remember { mutableStateOf(ShiftTiming.FLEXIBLE) }
    var urgency by remember { mutableStateOf(JobUrgency.FLEXIBLE) }
    var selectedPerks by remember { mutableStateOf<Set<JobPerk>>(emptySet()) }
    var vacancies by remember { mutableStateOf("1") }
    var employerName by remember { mutableStateOf("") }

    // UI state
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoadingJob by remember { mutableStateOf(true) }
    
    // Work Location Management (Industry standard pattern)
    var showSaveLocationDialog by remember { mutableStateOf(false) }
    var showSavedLocationsSheet by remember { mutableStateOf(false) }
    var locationLabel by remember { mutableStateOf("") }
    
    // Location coordinates for distance calculation
    var locationLatitude by remember { mutableStateOf(0.0) }
    var locationLongitude by remember { mutableStateOf(0.0) }

    // Load jobs first, then find the specific job
    LaunchedEffect(jobId) {
        Timber.d("🔍 EditJobScreen - Loading job with ID: $jobId")
        
        // First load all jobs to ensure we have the latest data
        viewModel.loadMyJobs()
    }
    
    // Saved work locations now flow from SavedWorkLocationsStore via collectAsState above.
    
    // Add timeout for job loading (10 seconds)
    LaunchedEffect(jobId) {
        kotlinx.coroutines.delay(10000) // 10 seconds timeout
        if (isLoadingJob && currentJob == null) {
            Timber.w("🔍 EditJobScreen - Job loading timeout, navigating back")
            isLoadingJob = false
            navController.popBackStack()
        }
    }
    
    // Load the specific job once jobs are loaded
    LaunchedEffect(uiState.myJobs, jobId) {
        Timber.d("🔍 EditJobScreen - Jobs loaded: ${uiState.myJobs.size}, looking for jobId: $jobId")
        uiState.myJobs.forEach { job ->
            Timber.d("🔍 EditJobScreen - Available job: ${job.id} - ${job.title}")
        }
        
        if (uiState.myJobs.isNotEmpty()) {
            val existingJob = uiState.myJobs.find { it.id == jobId } // Fixed: use it.id instead of it.jobId
            if (existingJob != null) {
                Timber.d("🔍 EditJobScreen - Job found in existing jobs list: ${existingJob.title}")
                currentJob = existingJob
                isLoadingJob = false
            } else {
                // If not found in the list, try to load it directly from repository
                Timber.d("🔍 EditJobScreen - Job not found in existing list, loading from repository")
                viewModel.getJobById(jobId) { job ->
                    if (job != null) {
                        Timber.d("🔍 EditJobScreen - Job loaded from repository: ${job.title}")
                        currentJob = job
                    } else {
                        Timber.w("🔍 EditJobScreen - Job not found in repository")
                    }
                    isLoadingJob = false
                }
            }
        }
    }
    
    // Observe current job and check timing restriction
    LaunchedEffect(currentJob) {
        val job = currentJob
        if (job != null) {
            try {
                Timber.d("🔍 EditJobScreen - Job loaded: ${job.title}")
                Timber.d("🔍 EditJobScreen - Job posted at: ${job.createdAt}")
                
                // Industry standard: Allow editing within 7 days
                val currentTime = System.currentTimeMillis()
                val jobPostedTime = job.createdAt
                val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L // 7 days
                
                Timber.d("🔍 EditJobScreen - Current time: $currentTime")
                Timber.d("🔍 EditJobScreen - Job posted time: $jobPostedTime")
                Timber.d("🔍 EditJobScreen - Time difference: ${currentTime - jobPostedTime}")
                Timber.d("🔍 EditJobScreen - Seven days in millis: $sevenDaysInMillis")
                
                if (currentTime - jobPostedTime > sevenDaysInMillis) {
                    canEditJob = false
                    val daysSincePosted = (currentTime - jobPostedTime) / (24 * 60 * 60 * 1000)
                    timeRestrictionMessage = "Jobs can only be edited within 7 days of posting. This job was posted $daysSincePosted days ago."
                    Timber.w("🔍 EditJobScreen - Job cannot be edited, posted $daysSincePosted days ago")
                } else {
                    canEditJob = true
                    timeRestrictionMessage = ""
                    Timber.d("🔍 EditJobScreen - Job can be edited")
                }
            } catch (e: Exception) {
                Timber.e(e, "🔍 EditJobScreen - Error processing job: ${e.message}")
                e.printStackTrace()
                canEditJob = false
                timeRestrictionMessage = "Error processing job: ${e.message}"
            }
        } else {
            Timber.d("🔍 EditJobScreen - No job loaded yet")
        }
    }

    // Initialize form with current job data
    LaunchedEffect(currentJob) {
        currentJob?.let { job ->
            title = job.title
            payAmount = job.salary.toInt().toString()
            location = job.addressText.ifBlank { job.location }
            description = job.description
            contactNumber = job.contactNumber
            // Initialize location coordinates from existing job
            locationLatitude = job.lat
            locationLongitude = job.lng
            Timber.d("📍 EditJob: Loaded existing coordinates - lat: $locationLatitude, lon: $locationLongitude")
            // Convert string to enum for category - using auto-detected category
            category = JobCategory.values().firstOrNull {
                it.displayName.equals(job.jobType, ignoreCase = true) ||
                    it.name.equals(job.jobType, ignoreCase = true)
            } ?: JobCategory.values().firstOrNull {
                it.displayName.equals(job.getCategory(), ignoreCase = true) ||
                    it.name.equals(job.getCategory(), ignoreCase = true)
            } ?: JobCategory.OTHER
            // shiftTiming removed from schema — default to FLEXIBLE
            shiftTiming = ShiftTiming.values().firstOrNull {
                it.displayName.equals(job.shiftTiming, ignoreCase = true) ||
                    it.name.equals(job.shiftTiming, ignoreCase = true)
            } ?: ShiftTiming.FLEXIBLE
            // urgency removed from optimized schema
            // selectedPerks removed as per user request
            vacancies = job.vacancies.toString()
            employerName = job.companyName
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
                        Timber.d("📍 EditJob: Location set - lat: $locationLatitude, lon: $locationLongitude, accuracy: ${locationInfo.accuracy}m")
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
        return title.isNotBlank() &&
                payAmount.isNotBlank() &&
                location.isNotBlank() &&
                description.isNotBlank() &&
                contactNumber.isNotBlank()
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
                        Timber.d("📝 EDIT JOB: Geocoding manual location: $location")
                        val geocodedLocation = locationService.getCoordinatesFromAddress(location)
                        if (geocodedLocation != null) {
                            finalLatitude = geocodedLocation.latitude
                            finalLongitude = geocodedLocation.longitude
                            Timber.d("📝 EDIT JOB: Geocoded - lat: $finalLatitude, lon: $finalLongitude")
                        } else if (!locationChanged) {
                            Timber.w("📝 EDIT JOB: Geocoding failed, using original coordinates")
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

                    val normalizedUrgency = when (urgency) {
                        JobUrgency.IMMEDIATE, JobUrgency.URGENT -> "HIGH"
                        JobUrgency.NORMAL -> "MEDIUM"
                        JobUrgency.FLEXIBLE -> "LOW"
                    }

                    val updates = mapOf(
                        "title" to title,
                        "salary" to (payAmount.toDoubleOrNull() ?: 0.0),
                        "salaryType" to payType.name,
                        "location" to mapOf("lat" to finalLatitude, "lng" to finalLongitude),
                        "addressText" to location,
                        "description" to description,
                        "contactNumber" to contactNumber,
                        "jobType" to category.displayName,
                        "urgency" to normalizedUrgency,
                        "shiftTiming" to shiftTiming.displayName,
                        "vacancies" to (vacancies.toIntOrNull() ?: 1),
                        "benefits" to selectedPerks.map { it.displayName }
                    )
                    
                    Timber.d("📝 EDIT JOB: Updating job with coordinates - lat: $finalLatitude, lon: $finalLongitude")
                    
                    viewModel.updateJob(originalJob.id, updates) { success, error ->
                        if (success) {
                            navController.popBackStack()
                        } else {
                            // Handle error - could show a toast or error message
                            Timber.e("❌ Failed to update job: $error")
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
                    Timber.e("❌ Failed to delete job: $error")
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
                            color = Color(0xFF1F2937)
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
        containerColor = Color.White
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
                                    color = Color(0xFF1F2937),
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

            // Category Selection
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
                                    .background(Color(0xFFF3E8FF), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = Color(0xFF9333EA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Job Category",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937),
                                    fontSize = 15.sp
                                )
                            )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(JobCategory.values()) { jobCategory ->
                                FilterChip(
                                    onClick = { category = jobCategory },
                                    label = {
                                        Text(
                                            text = "${jobCategory.icon} ${jobCategory.displayName}",
                                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                                        )
                                    },
                                    selected = category == jobCategory,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                        selectedLabelColor = Color(0xFF3B82F6)
                                    )
                                )
                            }
                        }
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
                                    color = Color(0xFF1F2937),
                                    fontSize = 15.sp
                                )
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = payAmount,
                                onValueChange = { payAmount = it },
                                label = { Text(stringResource(R.string.amount)) },
                                placeholder = { Text("e.g., 500", color = Color(0xFF9CA3AF)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(2f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                )
                            )

                            // Pay Type Dropdown
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = payType.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier.menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF3B82F6),
                                        unfocusedBorderColor = Color(0xFFE5E7EB)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    PayType.values().forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type.displayName) },
                                            onClick = {
                                                payType = type
                                                expanded = false
                                            }
                                        )
                                    }
                                }
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
                                    color = Color(0xFF1F2937),
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
                                Timber.d("📍 EditJob: Location selected - $address at ($lat, $lng)")
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
                                                Timber.d("📍 EditJob: GPS location - lat: $locationLatitude, lon: $locationLongitude")
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
                        
                        // Save Location Button (Industry standard pattern - Uber, Swiggy, Zomato)
                        if (location.isNotBlank() && locationLatitude != 0.0 && locationLongitude != 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showSaveLocationDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF3B82F6)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.save_location), fontSize = 13.sp)
                                }
                                
                                if (savedWorkLocations.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { showSavedLocationsSheet = true },
                                        modifier = Modifier.weight(1f),
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
                                    color = Color(0xFF1F2937),
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
                                    color = Color(0xFF1F2937),
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
                                    color = Color(0xFF1F2937),
                                    fontSize = 15.sp
                                )
                            )
                        }

                        // Shift Timing
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Shift Timing",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF374151)
                                )
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(ShiftTiming.values()) { shift ->
                                    FilterChip(
                                        onClick = { shiftTiming = shift },
                                        label = { Text(shift.displayName, fontSize = MaterialTheme.typography.bodySmall.fontSize) },
                                        selected = shiftTiming == shift,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                            selectedLabelColor = Color(0xFF3B82F6)
                                        )
                                    )
                                }
                            }
                        }

                        // Urgency
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Urgency",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF374151)
                                )
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(JobUrgency.values()) { jobUrgency ->
                                    FilterChip(
                                        onClick = { urgency = jobUrgency },
                                        label = { Text(jobUrgency.displayName, fontSize = MaterialTheme.typography.bodySmall.fontSize) },
                                        selected = urgency == jobUrgency,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                            selectedLabelColor = Color(0xFF3B82F6)
                                        )
                                    )
                                }
                            }
                        }

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

            // Perks Section
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
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Perks & Benefits",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937),
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(Optional)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }

                        val perksList = JobPerk.values().toList()
                        val chunkedPerks = perksList.chunked(2)
                        
                        chunkedPerks.forEach { perkRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                perkRow.forEach { perk ->
                                    FilterChip(
                                        onClick = {
                                            selectedPerks = if (selectedPerks.contains(perk)) {
                                                selectedPerks - perk
                                            } else {
                                                selectedPerks + perk
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = "${perk.icon} ${perk.displayName}",
                                                fontSize = MaterialTheme.typography.bodySmall.fontSize
                                            )
                                        },
                                        selected = selectedPerks.contains(perk),
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.1f),
                                            selectedLabelColor = Color(0xFF10B981)
                                        )
                                    )
                                }
                                // Fill remaining space if odd number of perks in row
                                if (perkRow.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
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
    
    // Save Location Dialog (Industry standard pattern)
    if (showSaveLocationDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSaveLocationDialog = false
                locationLabel = ""
            },
            title = { 
                Text(
                    "Save Work Location",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Give this location a label for quick access later",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = locationLabel,
                        onValueChange = { locationLabel = it },
                        label = { Text(stringResource(R.string.address_label_hint)) },
                        placeholder = { Text(stringResource(R.string.enter_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        "Address: $location",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (locationLabel.isNotBlank()) {
                            runCatching {
                                savedWorkLocationsStore.add(
                                    label = locationLabel,
                                    address = location,
                                    latitude = locationLatitude,
                                    longitude = locationLongitude
                                )
                            }.onSuccess {
                                android.widget.Toast.makeText(
                                    context,
                                    "Location saved for this session",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { error ->
                                android.widget.Toast.makeText(
                                    context,
                                    "Failed to save location: ${error.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            showSaveLocationDialog = false
                            locationLabel = ""
                        }
                    },
                    enabled = locationLabel.isNotBlank(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSaveLocationDialog = false
                    locationLabel = ""
                }) {
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

                                // Increment usage count
                                runCatching {
                                    savedWorkLocationsStore.add(
                                        label = workLocation.label,
                                        address = workLocation.address,
                                        latitude = workLocation.latitude,
                                        longitude = workLocation.longitude
                                    )
                                }

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
