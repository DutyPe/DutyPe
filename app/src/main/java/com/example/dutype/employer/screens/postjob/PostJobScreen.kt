package com.example.dutype.employer.screens.postjob

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DatePicker
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dutype.auth.AuthManager
import com.example.dutype.network.ApiClient
import com.example.dutype.models.JobListing
import com.example.dutype.employer.components.ContactSection
import com.example.dutype.employer.components.JobDescriptionSection
import com.example.dutype.employer.components.JobPreviewDialog
import com.example.dutype.employer.components.JobSummaryCard
import com.example.dutype.employer.components.JobTitleSection
import com.example.dutype.employer.components.LocationSection
import com.example.dutype.employer.components.PaymentSection
import com.example.dutype.employer.components.PerksSelectionGrid
import com.example.dutype.employer.components.StepHeader
import com.example.dutype.employer.components.VacanciesSection
import com.example.dutype.employer.components.WorkScheduleSection
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.employer.models.enums.JobCategory
import com.example.dutype.employer.models.enums.JobPerk
import com.example.dutype.employer.models.enums.JobUrgency
import com.example.dutype.employer.models.enums.PayType
import com.example.dutype.employer.models.enums.ShiftTiming
import com.example.dutype.utils.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    navController: NavController,
    employerId: String? = null,
    onJobPosted: (() -> Unit)? = null // Add callback for when job is posted
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    val employerJobViewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val employerJobUiState by employerJobViewModel.uiState.collectAsState()

    // Step management
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 4

    // Form state matching JobPostingModel
    var title by remember { mutableStateOf("") }
    var payAmount by remember { mutableStateOf("") }
    var payType by remember { mutableStateOf(PayType.DAILY) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(JobCategory.COOK) }
    var shiftTiming by remember { mutableStateOf(ShiftTiming.FLEXIBLE) }
    var urgency by remember { mutableStateOf(JobUrgency.FLEXIBLE) }
    var vacancies by remember { mutableStateOf("1") }
    var employerName by remember { mutableStateOf("") }
    
    // Additional fields for complete job posting
    var ageRange by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var applicationDeadline by remember { mutableStateOf("") }
    var companySize by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var requirements by remember { mutableStateOf("") }
    var benefits by remember { mutableStateOf("") }

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showPreview by remember { mutableStateOf(false) }

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

    // Validation functions for each step
    fun validateStep(step: Int): Boolean {
        return when (step) {
            1 -> title.isNotBlank() && description.isNotBlank()
            2 -> payAmount.isNotBlank() && location.isNotBlank()
            3 -> contactNumber.isNotBlank()
            4 -> true
            else -> false
        }
    }

    // Create job posting function
    fun createJobPosting(): JobPostingModel {
        return JobPostingModel(
            title = title,
            payAmount = payAmount,
            payType = payType,
            location = location,
            description = description,
            contactNumber = contactNumber,
            category = category,
            shiftTiming = shiftTiming,
            urgency = urgency,
            vacancies = vacancies.toIntOrNull() ?: 1,
            employerId = employerId ?: "",
            employerName = employerName,
            postedTime = System.currentTimeMillis()
        )
    }

    // Submit job function
    fun submitJob() {
        if (!validateStep(4)) return

        val jobPosting = createJobPosting()
        
        // Convert JobPostingModel to JobListing (optimized for job posting)
        val jobListing = JobListing(
            id = "",
            jobId = "",
            employerId = employerId ?: "emp_${System.currentTimeMillis()}",
            title = jobPosting.title,
            companyName = jobPosting.employerName,
            company = jobPosting.employerName,
            location = jobPosting.location,
            specificLocation = jobPosting.location,
            locationNearby = jobPosting.location,
            payAmount = "${jobPosting.payAmount}/${jobPosting.payType.name.lowercase()}",
            payType = jobPosting.payType.name.lowercase(),
            // Removed payPeriod - redundant with payType
            timing = jobPosting.shiftTiming.name,
            shiftTiming = jobPosting.shiftTiming.name,
            description = jobPosting.description,
            preferences = emptyList(),
            requirements = if (requirements.isNotBlank()) requirements.split(",").map { it.trim() } else emptyList(),
            benefits = if (benefits.isNotBlank()) benefits.split(",").map { it.trim() } else emptyList(),
            vacancies = jobPosting.vacancies,
            isActive = true,
            isTrending = false,
            isRemote = false,
            isVerified = false,
            urgency = if (jobPosting.urgency == JobUrgency.URGENT) "URGENT" else "NORMAL",
            postedAt = System.currentTimeMillis(),
            postedTime = System.currentTimeMillis().toString(),
            postedDate = System.currentTimeMillis().toString(),
            phoneNumber = jobPosting.contactNumber,
            contactNumber = jobPosting.contactNumber,
            contactInfo = jobPosting.contactNumber,
            category = jobPosting.category.name,
            jobType = "Part-time",
            experienceLevel = "Entry Level",
            workingHours = jobPosting.shiftTiming.name,
            ageRange = ageRange,
            gender = gender,
            applicationDeadline = applicationDeadline,
            companySize = companySize,
            industry = industry,
            viewCount = 0L,
            applicationCount = 0L
            // Removed isBookmarked and isApplied - these are worker-specific
            // Removed imageUrl as requested
        )
        
        // Convert JobListing to Map for Firestore
        val jobData = mapOf(
            "title" to jobListing.title,
            "companyName" to jobListing.companyName,
            "company" to jobListing.company,
            "location" to jobListing.location,
            "specificLocation" to jobListing.specificLocation,
            "locationNearby" to jobListing.locationNearby,
            "area" to jobListing.area,
            "city" to jobListing.city,
            "payRate" to jobListing.payRate,
            "payAmount" to jobListing.payAmount,
            "payType" to jobListing.payType,
            "payPeriod" to jobListing.payPeriod,
            "timing" to jobListing.timing,
            "shiftTiming" to jobListing.shiftTiming,
            "description" to jobListing.description,
            "preferences" to jobListing.preferences,
            "benefits" to jobListing.benefits,
            "requirements" to jobListing.requirements,
            "skills" to jobListing.skills,
            "vacancies" to jobListing.vacancies,
            "isActive" to jobListing.isActive,
            "isTrending" to jobListing.isTrending,
            "isRemote" to jobListing.isRemote,
            "isVerified" to jobListing.isVerified,
            "postedAt" to jobListing.postedAt,
            "postedTime" to jobListing.postedTime,
            "postedDate" to jobListing.postedDate,
            "imageUrl" to jobListing.imageUrl,
            "phoneNumber" to jobListing.phoneNumber,
            "contactNumber" to jobListing.contactNumber,
            "contactInfo" to jobListing.contactInfo,
            "category" to jobListing.category,
            "jobType" to jobListing.jobType,
            "experienceLevel" to jobListing.experienceLevel,
            "experienceRequired" to jobListing.experienceRequired,
            "workingHours" to jobListing.workingHours,
            "applicationDeadline" to jobListing.applicationDeadline,
            "ageRange" to jobListing.ageRange,
            "gender" to jobListing.gender,
            "companySize" to jobListing.companySize,
            "industry" to jobListing.industry,
            "urgency" to jobListing.urgency,
            "viewCount" to jobListing.viewCount,
            "applicationCount" to jobListing.applicationCount
        )
        
        employerJobViewModel.createJob(jobData as Map<String, Any>) { success, message ->
            if (success) {
                Toast.makeText(context, "Job posted successfully!", Toast.LENGTH_SHORT).show()
                // Call the callback if provided (for tabbed interface)
                onJobPosted?.invoke()
                // Navigate to employer home screen to show the posted job
                if (onJobPosted == null) {
                    navController.navigate("employer_home") {
                        // Clear the back stack so user can't go back to the posting form
                        popUpTo("employer_home") { inclusive = false }
                    }
                }
            } else {
                Toast.makeText(context, "Error posting job: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Job Preview Dialog
    if (showPreview) {
        JobPreviewDialog(
            jobPosting = createJobPosting(),
            onDismiss = { showPreview = false },
            onConfirmPost = {
                showPreview = false
                submitJob()
            }
        )
    }

    Scaffold(

        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Previous")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }

                    if (currentStep < totalSteps) {
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier.weight(2f),
                            enabled = validateStep(currentStep)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    } else {
                        // On last step, show Preview and Post buttons
                        OutlinedButton(
                            onClick = { showPreview = true },
                            modifier = Modifier.weight(1f),
                            enabled = validateStep(1) && validateStep(2) && validateStep(3)
                        ) {
                            Icon(Icons.Default.Preview, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview")
                        }

                        Button(
                            onClick = { submitJob() },
                            modifier = Modifier.weight(1f),
                            enabled = !employerJobUiState.isCreatingJob && validateStep(1) && validateStep(2) && validateStep(3)
                        ) {
                            if (employerJobUiState.isCreatingJob) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Post")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A), // Deep professional blue
                            Color(0xFF3B82F6), // Bright blue
                            Color(0xFFE0F2FE), // Light blue
                            Color.White
                        ),
                        startY = 0f,
                        endY = 900f
                    )
                )
                .padding(paddingValues)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    1 -> {
                        item {
                            StepHeader("Basic Information", "Tell us about the job position")
                        }

                        item {
                            JobTitleSection(
                                title = title,
                                onTitleChange = { title = it },
                                category = category,
                                onCategoryChange = { category = it }
                            )
                        }

                        item {
                            JobDescriptionSection(
                                description = description,
                                onDescriptionChange = { description = it }
                            )
                        }
                    }

                    2 -> {
                        item {
                            StepHeader("Payment & Location", "Set compensation and work location")
                        }

                        item {
                            PaymentSection(
                                payAmount = payAmount,
                                onPayAmountChange = { payAmount = it },
                                payType = payType,
                                onPayTypeChange = { payType = it }
                            )
                        }

                        item {
                            LocationSection(
                                location = location,
                                onLocationChange = { location = it },
                                isLoadingLocation = isLoadingLocation,
                                locationError = locationError,
                                onLocationButtonClick = {
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
                                            } catch (_: Exception) {
                                                locationError = "Error getting location"
                                            } finally {
                                                isLoadingLocation = false
                                            }
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                }
                            )
                        }
                        item {
                            VacanciesSection(
                                vacancies = vacancies,
                                onVacanciesChange = { vacancies = it }
                            )
                        }
                    }

                    3 -> {
                        item {
                            StepHeader("Contact Information", "How candidates can reach you")
                        }

                        item {
                            ContactSection(
                                contactNumber = contactNumber,
                                onContactNumberChange = { contactNumber = it },
                                employerName = employerName,
                                onEmployerNameChange = { employerName = it }
                            )
                        }
                    }

                    4 -> {
                        item {
                            StepHeader("Additional Details", "Optional settings and preferences")
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Work Schedule & Urgency",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    WorkScheduleSection(
                                        selectedShift = shiftTiming,
                                        onShiftSelected = { shiftTiming = it },
                                        selectedUrgency = urgency,
                                        onUrgencySelected = { urgency = it }
                                    )
                                }
                            }
                        }

                        // Additional Job Details
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Job Requirements & Preferences",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Age Range
                                    OutlinedTextField(
                                        value = ageRange,
                                        onValueChange = { ageRange = it },
                                        label = { Text("Age Range (e.g., 18-25, 25-35)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Gender Preference - Radio Buttons
                                    Text(
                                        text = "Gender Preference",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = gender == "Any",
                                                onClick = { gender = "Any" }
                                            )
                                            Text("Any", modifier = Modifier.padding(start = 4.dp))
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = gender == "Male",
                                                onClick = { gender = "Male" }
                                            )
                                            Text("Male", modifier = Modifier.padding(start = 4.dp))
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = gender == "Female",
                                                onClick = { gender = "Female" }
                                            )
                                            Text("Female", modifier = Modifier.padding(start = 4.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Application Deadline - Date Picker
                                    var showDatePicker by remember { mutableStateOf(false) }
                                    val dateFormatter = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
                                    
                                    OutlinedTextField(
                                        value = applicationDeadline,
                                        onValueChange = { applicationDeadline = it },
                                        label = { Text("Application Deadline") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { showDatePicker = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "Select Date"
                                                )
                                            }
                                        }
                                    )
                                    
                                    if (showDatePicker) {
                                        val datePickerState = rememberDatePickerState(
                                            initialSelectedDateMillis = if (applicationDeadline.isNotEmpty()) {
                                                try {
                                                    dateFormatter.parse(applicationDeadline)?.time
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            } else null
                                        )
                                        
                                        Dialog(
                                            onDismissRequest = { showDatePicker = false },
                                            properties = DialogProperties(usePlatformDefaultWidth = false)
                                        ) {
                                            Card(
                                                modifier = Modifier.padding(16.dp),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp)
                                                ) {
                                                    Text(
                                                        text = "Select Application Deadline",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                    )
                                                    
                                                    DatePicker(state = datePickerState)
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        TextButton(onClick = { showDatePicker = false }) {
                                                            Text("Cancel")
                                                        }
                                                        TextButton(
                                                            onClick = {
                                                                datePickerState.selectedDateMillis?.let { millis ->
                                                                    applicationDeadline = dateFormatter.format(java.util.Date(millis))
                                                                }
                                                                showDatePicker = false
                                                            }
                                                        ) {
                                                            Text("OK")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Company Size - Numeric Input
                                    OutlinedTextField(
                                        value = companySize,
                                        onValueChange = { newValue ->
                                            // Only allow numbers
                                            if (newValue.all { it.isDigit() }) {
                                                companySize = newValue
                                            }
                                        },
                                        label = { Text("Company Size (Number of Employees)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        placeholder = { Text("e.g., 10") }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Industry
                                    OutlinedTextField(
                                        value = industry,
                                        onValueChange = { industry = it },
                                        label = { Text("Industry (e.g., Food & Beverage, Retail)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Requirements
                                    OutlinedTextField(
                                        value = requirements,
                                        onValueChange = { requirements = it },
                                        label = { Text("Job Requirements (comma-separated)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        maxLines = 4,
                                        placeholder = { Text("e.g., Experience in cooking, Valid driving license, Good communication skills") }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Benefits
                                    OutlinedTextField(
                                        value = benefits,
                                        onValueChange = { benefits = it },
                                        label = { Text("Job Benefits (comma-separated)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        maxLines = 4,
                                        placeholder = { Text("e.g., Flexible hours, Free meals, Transportation allowance") }
                                    )
                                }
                            }
                        }

                        item {
                            JobSummaryCard(
                                title = title,
                                category = category,
                                payAmount = payAmount,
                                payType = payType,
                                location = location,
                                vacancies = vacancies,
                                urgency = urgency,
                                shiftTiming = shiftTiming,
                                description = description
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}
