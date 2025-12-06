package com.example.dutype.employer.screens.editjob

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.employer.models.enums.*
import com.example.dutype.utils.LocationService
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditJobScreen(
    navController: NavController,
    jobId: String,
    viewModel: FirestoreEmployerJobViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }

    // Get the current job from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState.isUpdatingJob || uiState.isDeletingJob
    
    // Current job state
    var currentJob by remember { mutableStateOf<com.example.dutype.models.JobListing?>(null) }
    
    // Timing restriction - can't edit after 23 hours
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

    // Load jobs first, then find the specific job
    LaunchedEffect(jobId) {
        Timber.d("🔍 EditJobScreen - Loading job with ID: $jobId")
        
        // First load all jobs to ensure we have the latest data
        viewModel.loadMyJobs()
    }
    
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
            Timber.d("🔍 EditJobScreen - Available job: ${job.jobId} - ${job.title}")
        }
        
        if (uiState.myJobs.isNotEmpty()) {
            val existingJob = uiState.myJobs.find { it.jobId == jobId }
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
                Timber.d("🔍 EditJobScreen - Job posted time: ${job.postedTime}")
                
                // Check if job can be edited (within 23 hours)
                val currentTime = System.currentTimeMillis()
                val jobPostedTime = job.postedAt
                val twentyThreeHoursInMillis = 23 * 60 * 60 * 1000L // 23 hours in milliseconds
                
                Timber.d("🔍 EditJobScreen - Current time: $currentTime")
                Timber.d("🔍 EditJobScreen - Job posted time: $jobPostedTime")
                Timber.d("🔍 EditJobScreen - Time difference: ${currentTime - jobPostedTime}")
                Timber.d("🔍 EditJobScreen - Twenty three hours in millis: $twentyThreeHoursInMillis")
                
                if (currentTime - jobPostedTime > twentyThreeHoursInMillis) {
                    canEditJob = false
                    val hoursSincePosted = (currentTime - jobPostedTime) / (60 * 60 * 1000)
                    timeRestrictionMessage = "Job cannot be edited after 23 hours. Posted $hoursSincePosted hours ago."
                    Timber.w("🔍 EditJobScreen - Job cannot be edited, posted $hoursSincePosted hours ago")
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
            payAmount = job.payAmount
            location = job.location
            description = job.description
            contactNumber = job.contactNumber
            // Convert string to enum for category
            category = try {
                JobCategory.valueOf(job.category.uppercase())
            } catch (e: Exception) {
                JobCategory.COOK // Default fallback
            }
            // Convert string to enum for shiftTiming
            shiftTiming = try {
                ShiftTiming.valueOf(job.shiftTiming.uppercase())
            } catch (e: Exception) {
                ShiftTiming.FLEXIBLE // Default fallback
            }
            // Convert string to enum for urgency
            urgency = try {
                JobUrgency.valueOf(job.urgency.uppercase())
            } catch (e: Exception) {
                JobUrgency.FLEXIBLE // Default fallback
            }
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
                    val locationInfo = locationService.getCurrentLocation()
                    if (locationInfo != null) {
                        location = locationInfo.address
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
                val updates = mapOf(
                    "title" to title,
                    "payAmount" to payAmount,
                    "payType" to payType.name,
                    "location" to location,
                    "description" to description,
                    "contactNumber" to contactNumber,
                    "category" to category,
                    "shiftTiming" to shiftTiming,
                    "urgency" to urgency,
                    "vacancies" to (vacancies.toIntOrNull() ?: 1),
                    "companyName" to employerName,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                viewModel.updateJob(originalJob.jobId, updates) { success, error ->
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

    // Delete function
    fun deleteJob() {
        currentJob?.let { job ->
            viewModel.deleteJob(job.jobId) { success, error ->
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
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Job",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (canEditJob) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Job", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2193b0),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column {
                    // Show timing restriction message if applicable
                    if (!canEditJob && timeRestrictionMessage.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 8.dp, 16.dp, 0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFF856404),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = timeRestrictionMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF856404)
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = { updateJob() },
                            modifier = Modifier.weight(2f),
                            enabled = !isLoading && validateForm() && canEditJob
                        ) {
                            if (isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White
                                    )
                                    Text("Updating...", color = Color.White)
                                }
                            } else {
                                Text("Update Job")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Job Title
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Job Title",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("e.g., Cook, Driver, Cleaner") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Category Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Job Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
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
                                    selected = category == jobCategory
                                )
                            }
                        }
                    }
                }
            }

            // Pay Information
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Payment Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = payAmount,
                                onValueChange = { payAmount = it },
                                label = { Text("Amount") },
                                placeholder = { Text("e.g., 500") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(2f),
                                singleLine = true
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
                                    modifier = Modifier.menuAnchor()
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Job Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            placeholder = { Text("Enter location or use GPS") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (locationService.hasLocationPermission()) {
                                            isLoadingLocation = true
                                            locationError = null
                                            scope.launch {
                                                try {
                                                    val locationInfo = locationService.getCurrentLocation()
                                                    if (locationInfo != null) {
                                                        location = locationInfo.address
                                                    } else {
                                                        locationError = "Unable to get current location"
                                                    }
                                                } catch (e: Exception) {
                                                    locationError = "Error getting location"
                                                } finally {
                                                    isLoadingLocation = false
                                                }
                                            }
                                        } else {
                                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        }
                                    }
                                ) {
                                    if (isLoadingLocation) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(Icons.Default.LocationOn, contentDescription = "Use GPS")
                                    }
                                }
                            }
                        )

                        locationError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Job Description
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Job Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Describe the job responsibilities and requirements...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )
                    }
                }
            }

            // Contact Information
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Contact Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = contactNumber,
                            onValueChange = { contactNumber = it },
                            label = { Text("Contact Number") },
                            placeholder = { Text("e.g., +91 9876543210") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = employerName,
                            onValueChange = { employerName = it },
                            label = { Text("Your Name (Optional)") },
                            placeholder = { Text("Enter your name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Additional Details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Additional Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Shift Timing
                        Text(
                            text = "Shift Timing",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ShiftTiming.values()) { shift ->
                                FilterChip(
                                    onClick = { shiftTiming = shift },
                                    label = { Text(shift.displayName, fontSize = MaterialTheme.typography.bodySmall.fontSize) },
                                    selected = shiftTiming == shift
                                )
                            }
                        }

                        // Urgency
                        Text(
                            text = "Urgency",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(JobUrgency.values()) { jobUrgency ->
                                FilterChip(
                                    onClick = { urgency = jobUrgency },
                                    label = { Text(jobUrgency.displayName, fontSize = MaterialTheme.typography.bodySmall.fontSize) },
                                    selected = urgency == jobUrgency
                                )
                            }
                        }

                        // Vacancies
                        OutlinedTextField(
                            value = vacancies,
                            onValueChange = { vacancies = it },
                            label = { Text("Number of Vacancies") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Perks Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Perks & Benefits (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

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
                                        modifier = Modifier.weight(1f)
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
            title = { Text("Delete Job") },
            text = { Text("Are you sure you want to delete this job posting? This action cannot be undone.") },
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
                            Text("Deleting...", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
