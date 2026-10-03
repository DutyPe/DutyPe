package com.example.dutype.employer.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
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
import androidx.navigation.NavController
import com.example.dutype.employer.components.JobImageUploadSection
import com.example.dutype.employer.models.EmploymentType
import com.example.dutype.employer.models.*
import com.example.dutype.viewmodels.EmployerJobsViewModel
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.EmployerColors
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
    viewModel: EmployerJobsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = viewModel.locationService
    val savedWorkLocationsStore = viewModel.savedWorkLocationsStore
    val savedWorkLocations by savedWorkLocationsStore.locations.collectAsState()

    // Get the current job from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState.isUpdatingJob

    // Current job state
    var currentJob by remember { mutableStateOf<com.example.dutype.models.JobListing?>(null) }

    // Timing restriction - can't edit after 48 hours
    var canEditJob by remember { mutableStateOf(true) }
    var timeRestrictionMessage by remember { mutableStateOf("") }

    // Initialize form state with current job data
    var title by remember { mutableStateOf("") }
    var payAmount by remember { mutableStateOf("") }
    var payType by remember { mutableStateOf(PayType.MONTHLY) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf(JobShift.ANY) }
    var vacancies by remember { mutableStateOf("") }
    var employerName by remember { mutableStateOf("") }
    var employmentType by remember { mutableStateOf(EmploymentType.FULL_TIME) }
    var experienceLevel by remember { mutableStateOf("No Experience Required") }
    var educationRequired by remember { mutableStateOf("No qualification required") }
    var gender by remember { mutableStateOf("Both") }
    var jobImageUri by remember { mutableStateOf<Uri?>(null) }
    var jobImageUrl by remember { mutableStateOf("") }
    var isUploadingJobImage by remember { mutableStateOf(false) }

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

    // Card + details in one read of each doc; closes the screen if the job is gone.
    LaunchedEffect(jobId) {
        viewModel.getJob(jobId) { job ->
            currentJob = job
            isLoadingJob = false
            if (job == null) navController.popBackStack()
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
            payAmount = if (job.payAmount > 0) job.payAmount.toString() else ""
            location = job.addressText.ifBlank { job.area }
            description = job.description
            contactNumber = job.contactNumber
            locationLatitude = job.lat
            locationLongitude = job.lng
            payType = com.example.dutype.utils.PayRules.vacancyPayType(PayType.fromKey(job.payType))
            vacancies = job.vacancies.toString()
            employerName = job.companyName
            employmentType = com.example.dutype.utils.PayRules.vacancyEmploymentType(EmploymentType.fromKey(job.employmentType))
            experienceLevel = job.experienceRequired.ifBlank { "No Experience Required" }
            educationRequired = job.educationRequired.ifBlank { "No qualification required" }
            gender = when (job.gender) {
                "MALE" -> "Male"
                "FEMALE" -> "Female"
                else -> "Both"
            }
            jobImageUrl = job.photoUrl.orEmpty()
            jobImageUri = null
            shift = JobShift.fromKey(job.shift)
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
                (payAmount.filter { it.isDigit() }.toLongOrNull() ?: 0L) in com.example.dutype.utils.PayRules.range(payType, employmentType) &&
                location.isNotBlank() &&
                description.trim().length >= 10 &&
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
                    val locationChanged = location != originalJob.addressText

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

                    val form = com.example.dutype.jobs.JobForm(
                        title = title.trim(),
                        category = com.example.dutype.utils.JobCategoryResolver.inferCategoryName(title, description, originalJob.category),
                        employmentType = employmentType.key,
                        payAmount = if (payType == PayType.NEGOTIABLE) 0L else payAmount.filter { it.isDigit() }.toLongOrNull() ?: 0L,
                        payType = payType.key,
                        vacancies = vacancies.toIntOrNull() ?: return@launch,
                        urgency = originalJob.urgency,
                        shift = shift.key,
                        area = com.example.dutype.utils.AreaText.from(location),
                        lat = finalLatitude,
                        lng = finalLongitude,
                        photoUrl = jobImageUrl.ifBlank { null },
                        description = description.trim(),
                        addressText = location.trim(),
                        contactNumber = contactNumber,
                        gender = when (gender) { "Male" -> "MALE"; "Female" -> "FEMALE"; else -> "ANY" },
                        experienceRequired = experienceLevel,
                        educationRequired = educationRequired,
                        benefits = originalJob.benefits
                    )

                    viewModel.updateJob(originalJob.id, form) { success, error ->
                        if (success) {
                            navController.popBackStack()
                        } else {
                            android.widget.Toast.makeText(context, error ?: context.getString(R.string.failed_update_job), android.widget.Toast.LENGTH_LONG).show()
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
                    android.widget.Toast.makeText(context, error ?: context.getString(R.string.failed_delete_job), android.widget.Toast.LENGTH_LONG).show()
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
                    color = EmployerColors.Primary.fg()
                )
                Text(
                    text = stringResource(R.string.auto_loading_job_details),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = stringResource(R.string.auto_please_wait_while_we_fetch_your_job_inform),
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
                color = Color.White.bg(),
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
                        color = EmployerColors.ChipBackground
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = EmployerColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(R.string.auto_edit_job),
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
                            color = EmployerColors.ErrorLight
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Job",
                                    tint = EmployerColors.Error,
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
                color = Color.White.bg()
            ) {
                Column {
                    // Show timing restriction message if applicable
                    if (!canEditJob && timeRestrictionMessage.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 8.dp, 16.dp, 0.dp),
                            color = EmployerColors.WarningLight,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = EmployerColors.Warning,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = timeRestrictionMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = EmployerColors.Warning,
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmployerColors.Border)
                        ) {
                            Text(
                                "Cancel",
                                color = EmployerColors.TextSecondary,
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
                                containerColor = EmployerColors.Primary,
                                disabledContainerColor = EmployerColors.Border
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
                    color = Color.White.bg(),
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
                                    .background(EmployerColors.Primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = EmployerColors.Primary.fg(),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auto_job_title),
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
                            placeholder = { Text(stringResource(R.string.edit_job_title_hint), color = EmployerColors.TextTertiary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmployerColors.Primary.bd(),
                                unfocusedBorderColor = EmployerColors.Border
                            )
                        )
                    }
                }
            }

            // Pay Information
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.bg(),
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
                                    .background(EmployerColors.SuccessLight, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = EmployerColors.Success,
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

                        val isPayAmountInvalid = (payAmount.toLongOrNull() ?: 0L) > com.example.dutype.utils.SalaryFormatter.MAX_PAY_RUPEES
                        OutlinedTextField(
                            value = payAmount,
                            onValueChange = { typed -> payAmount = typed.filter { it.isDigit() }.take(6) },
                            label = { Text(stringResource(R.string.amount)) },
                            placeholder = { Text(stringResource(R.string.edit_job_salary_hint), color = EmployerColors.TextTertiary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmployerColors.Primary.bd(),
                                unfocusedBorderColor = EmployerColors.Border
                            ),
                            isError = isPayAmountInvalid,
                            supportingText = {
                                if (isPayAmountInvalid) {
                                    Text(
                                        stringResource(R.string.pay_max_limit),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )

                        Text(
                            text = stringResource(R.string.auto_pay_type),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EmployerColors.TextSecondary
                            )
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(com.example.dutype.utils.PayRules.VACANCY_PAY_TYPES) { type ->
                                val selected = payType == type
                                FilterChip(
                                    selected = selected,
                                    onClick = { payType = type },
                                    modifier = Modifier.height(34.dp),
                                    label = {
                                        Text(
                                            text = stringResource(type.labelRes),
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmployerColors.Primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
                                        labelColor = EmployerColors.TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = EmployerColors.Border,
                                        selectedBorderColor = EmployerColors.Primary.bd()
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
                    color = Color.White.bg(),
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
                                    .background(EmployerColors.ErrorLight, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = EmployerColors.Error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auto_job_location),
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
                                focusedBorderColor = EmployerColors.Primary.bd(),
                                unfocusedBorderColor = EmployerColors.Border
                            )
                        )

                        locationError?.let { error ->
                            Text(
                                text = error,
                                color = EmployerColors.Error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (savedWorkLocations.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showSavedLocationsSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = EmployerColors.Success
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
                    color = Color.White.bg(),
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
                                    .background(EmployerColors.WarningLight, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = EmployerColors.Warning,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auto_job_description),
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
                            placeholder = { Text(stringResource(R.string.describe_job_placeholder), color = EmployerColors.TextTertiary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmployerColors.Primary.bd(),
                                unfocusedBorderColor = EmployerColors.Border
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
                                    Toast.makeText(context, context.getString(R.string.please_login_upload_image), Toast.LENGTH_SHORT).show()
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
                    color = Color.White.bg(),
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
                                    .background(EmployerColors.InfoLight, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = EmployerColors.Primary.fg(),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auto_contact_information),
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
                            placeholder = { Text(stringResource(R.string.phone_number_example_hint), color = EmployerColors.TextTertiary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmployerColors.Primary.bd(),
                                unfocusedBorderColor = EmployerColors.Border
                            )
                        )
                        OutlinedTextField(
                            value = employerName,
                            onValueChange = { employerName = it },
                            label = { Text(stringResource(R.string.your_name_optional)) },
                            placeholder = { Text(stringResource(R.string.enter_your_name), color = EmployerColors.TextTertiary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmployerColors.Primary.bd(),
                                unfocusedBorderColor = EmployerColors.Border
                            )
                        )
                    }
                }
            }

            // Additional Details
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.bg(),
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
                                    .background(Color(0xFFDB2777).bg().copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFFDB2777).fg(),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auto_additional_details),
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
                                text = stringResource(R.string.auto_work_type),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = EmployerColors.TextSecondary
                                )
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(com.example.dutype.utils.PayRules.VACANCY_EMPLOYMENT_TYPES) { type ->
                                    val selected = employmentType == type
                                    FilterChip(
                                        onClick = { employmentType = type },
                                        label = {
                                            Text(
                                                type.displayName,
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                            )
                                        },
                                        selected = selected,
                                        modifier = Modifier.height(34.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmployerColors.Primary,
                                            selectedLabelColor = Color.White,
                                            containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
                                            labelColor = EmployerColors.TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = EmployerColors.Border,
                                            selectedBorderColor = EmployerColors.Primary.bd()
                                        )
                                    )
                                }
                            }
                        }

                        // Shift
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.auto_shift),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = EmployerColors.TextSecondary
                                )
                            )
                            val shiftOptions = JobShift.entries
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(shiftOptions) { option ->
                                    val selected = shift == option
                                    FilterChip(
                                        onClick = { shift = option },
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
                                            selectedContainerColor = EmployerColors.Primary,
                                            selectedLabelColor = Color.White,
                                            containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
                                            labelColor = EmployerColors.TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = EmployerColors.Border,
                                            selectedBorderColor = EmployerColors.Primary.bd()
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
                                focusedBorderColor = EmployerColors.Primary.bd(),
                                unfocusedBorderColor = EmployerColors.Border
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
                    stringResource(R.string.saved_work_locations),
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
                            color = EmployerColors.ChipBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmployerColors.Border)
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
                                        .background(EmployerColors.SuccessLight, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = EmployerColors.Success,
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
                                            stringResource(R.string.used_times_format, workLocation.usageCount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EmployerColors.Success,
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

