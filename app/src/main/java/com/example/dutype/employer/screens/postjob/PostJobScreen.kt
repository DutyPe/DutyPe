package com.example.dutype.employer.screens.postjob

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.employer.components.CategorySelectionGrid
import com.example.dutype.employer.components.ContactSection
import com.example.dutype.employer.components.JobDescriptionSection
import com.example.dutype.employer.components.JobPreviewDialog
import com.example.dutype.employer.components.JobSummaryCard
import com.example.dutype.employer.components.PayTypeDropdown
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
import com.example.dutype.models.JobListing
import com.example.dutype.utils.LocationService
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.services.FirestoreService
import com.example.dutype.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    var payType by remember { mutableStateOf(PayType.HOURLY) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(JobCategory.COOK) }
    var shiftTiming by remember { mutableStateOf(ShiftTiming.FLEXIBLE) }
    var urgency by remember { mutableStateOf(JobUrgency.FLEXIBLE) }
    var vacancies by remember { mutableStateOf("1") }
    var employerName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    
    // Enhanced fields for hyper-local jobs
    var selectedPerks by remember { mutableStateOf(setOf<JobPerk>()) }
    var workType by remember { mutableStateOf("Part-time") }
    var experienceLevel by remember { mutableStateOf("No Experience Required") }
    var ageRange by remember { mutableStateOf("18-35") }
    var gender by remember { mutableStateOf("Any") }
    var applicationDeadline by remember { mutableStateOf("") }
    var companySize by remember { mutableStateOf("Small (1-10 employees)") }
    var industry by remember { mutableStateOf("Food & Beverage") }
    var requirements by remember { mutableStateOf("") }
    var benefits by remember { mutableStateOf("") }
    
    // Quick selection options for hyper-local jobs
    val workTypes = listOf("Part-time", "Full-time", "Contract", "Temporary", "Weekend Only", "Student-friendly")
    val experienceLevels = listOf("No Experience Required", "1-2 years", "2-5 years", "5+ years")
    val ageRanges = listOf("18-25", "18-35", "25-45", "35+", "Any Age")
    val genders = listOf("Any", "Male", "Female")
    val companySizes = listOf("Small (1-10 employees)", "Medium (11-50 employees)", "Large (50+ employees)")
    val industries = listOf("Food & Beverage", "Retail", "Hospitality", "Delivery", "Cleaning", "Security", "Other")

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

    // Load employer profile data to get company name (MANDATORY)
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            scope.launch {
                try {
                    val db = FirebaseFirestore.getInstance()
                    // Fetch from 'users' collection (source of truth for employer data)
                    val userDoc = db.collection("users").document(currentUser.uid).get().await()
                    
                    if (userDoc.exists()) {
                        // Get company name from users collection (MANDATORY FIELD)
                        val savedCompanyName = userDoc.getString("companyName")
                        if (!savedCompanyName.isNullOrBlank()) {
                            companyName = savedCompanyName
                            println("✅ Company name loaded from users collection: $companyName")
                        } else {
                            println("⚠️ Company name is blank in users collection!")
                        }
                        
                        // Get full name for employer name (for reference only)
                        val savedFullName = userDoc.getString("fullName")
                        if (!savedFullName.isNullOrBlank()) {
                            employerName = savedFullName
                        }
                    } else {
                        println("❌ User document not found in users collection!")
                    }
                } catch (e: Exception) {
                    println("❌ Error loading employer profile: ${e.message}")
                    e.printStackTrace()
                }
            }
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
        
        // Validate that company name is available (MANDATORY)
        if (companyName.isBlank()) {
            Toast.makeText(context, "Please complete your company profile first to post jobs.", Toast.LENGTH_LONG).show()
            // Navigate to profile screen to complete company information
            navController.navigate(Routes.EMPLOYER_PROFILE)
            return
        }

        val jobPosting = createJobPosting()
        
        // Convert JobPostingModel to JobListing (optimized for job posting)
        val jobListing = JobListing(
            id = "",
            jobId = "",
            employerId = employerId ?: "emp_${System.currentTimeMillis()}",
            title = jobPosting.title,
            companyName = companyName, // Company name is mandatory and loaded from profile
            company = companyName,     // Use only company name, not employer name
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
        
        // Convert JobListing to Map for Firestore (removed duplicates)
        val jobData = mapOf(
            "title" to jobListing.title,
            "companyName" to jobListing.companyName,
            "company" to jobListing.company,
            "employerName" to employerName, // Add employer name for reference
            "location" to jobListing.location,
            "specificLocation" to jobListing.specificLocation,
            "locationNearby" to jobListing.locationNearby,
            "payAmount" to jobListing.payAmount,
            "payType" to jobListing.payType,
            "timing" to jobListing.timing,
            "shiftTiming" to jobListing.shiftTiming,
            "description" to jobListing.description,
            "benefits" to jobListing.benefits,
            "requirements" to jobListing.requirements,
            "vacancies" to jobListing.vacancies,
            "isActive" to jobListing.isActive,
            "isTrending" to jobListing.isTrending,
            "isRemote" to jobListing.isRemote,
            "isVerified" to jobListing.isVerified,
            "postedAt" to jobListing.postedAt,
            "postedTime" to jobListing.postedTime,
            "postedDate" to jobListing.postedDate,
            "contactNumber" to jobListing.contactNumber,
            "contactInfo" to jobListing.contactInfo,
            "category" to jobListing.category,
            "jobType" to jobListing.jobType,
            "experienceLevel" to jobListing.experienceLevel,
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
                            // Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Previous")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Cancel")
                        }
                    }

                    if (currentStep < totalSteps) {
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier.weight(1f),
                            enabled = validateStep(currentStep)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    } else {
                        // On last step, show Preview and Post buttons
                        OutlinedButton(
                            onClick = { showPreview = true },
                            modifier = Modifier.weight(1f),
                            enabled = validateStep(1) && validateStep(2) && validateStep(3)
                        )
                         {
                            // Icon(Icons.Default.Preview, contentDescription = null)
                            Spacer(modifier = Modifier.width(1.dp))
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
            // Segmented Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalSteps) { index ->
                    val isCompleted = index < currentStep - 1
                    val isCurrent = index == currentStep - 1
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(
                                when {
                                    isCompleted -> Color(0xFF10B981) // Green for completed
                                    isCurrent -> Color(0xFF3B82F6)   // Blue for current
                                    else -> Color(0xFFE5E7EB)        // Gray for upcoming
                                },
                                RoundedCornerShape(999.dp)
                            )
                    )
                }
            }

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
                // Company name is loaded automatically from profile (no UI shown)
                
                when (currentStep) {
                    1 -> {
                        item {
                            StepHeader("Job Details", "What type of local job are you posting?")
                        }

                        item {
                            EnhancedJobTitleSection(
                                title = title,
                                onTitleChange = { title = it },
                                category = category,
                                onCategoryChange = { category = it }
                            )
                        }

                        item {
                            WorkTypeSelection(
                                workType = workType,
                                onWorkTypeChange = { workType = it },
                                workTypes = workTypes
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
                            EnhancedPaymentSection(
                                payAmount = payAmount,
                                onPayAmountChange = { payAmount = it },
                                payType = payType,
                                onPayTypeChange = { payType = it }
                            )
                        }

                        item {
                            EnhancedLocationSection(
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
                            StepHeader("Requirements & Contact", "Set job requirements and contact details")
                        }

                        item {
                            RequirementsSection(
                                experienceLevel = experienceLevel,
                                onExperienceLevelChange = { experienceLevel = it },
                                experienceLevels = experienceLevels,
                                ageRange = ageRange,
                                onAgeRangeChange = { ageRange = it },
                                ageRanges = ageRanges,
                                gender = gender,
                                onGenderChange = { gender = it },
                                genders = genders,
                                industry = industry,
                                onIndustryChange = { industry = it },
                                industries = industries,
                                companySize = companySize,
                                onCompanySizeChange = { companySize = it },
                                companySizes = companySizes
                            )
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
                            StepHeader("Schedule & Perks", "Set work schedule and attractive benefits")
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp)
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

                        item {
                            PerksSelectionSection(
                                selectedPerks = selectedPerks,
                                onPerksChanged = { selectedPerks = it }
                            )
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

// Enhanced UI Components for Hyper-Local Jobs

@Composable
fun EnhancedJobTitleSection(
    title: String,
    onTitleChange: (String) -> Unit,
    category: JobCategory,
    onCategoryChange: (JobCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Job Title & Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Job Title (e.g., Waiter, Driver, Cook)") },
                placeholder = { Text("Enter job title...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    focusedLabelColor = Color(0xFF6366F1)
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            CategorySelectionGrid(
                selectedCategory = category,
                onCategorySelected = onCategoryChange
            )
        }
    }
}

@Composable
fun WorkTypeSelection(
    workType: String,
    onWorkTypeChange: (String) -> Unit,
    workTypes: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Work Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workTypes) { type ->
                    FilterChip(
                        onClick = { onWorkTypeChange(type) },
                        label = { Text(type) },
                        selected = workType == type,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6366F1),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedPaymentSection(
    payAmount: String,
    onPayAmountChange: (String) -> Unit,
    payType: PayType,
    onPayTypeChange: (PayType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Payment Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = payAmount,
                    onValueChange = onPayAmountChange,
                    label = { Text("Amount") },
                    placeholder = { Text("500") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        focusedLabelColor = Color(0xFF6366F1)
                    )
                )
                
                PayTypeDropdown(
                    selectedType = payType,
                    onTypeSelected = onPayTypeChange,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "💡 Tip: Competitive rates attract more applicants",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5D5757)
            )
        }
    }
}

@Composable
fun EnhancedLocationSection(
    location: String,
    onLocationChange: (String) -> Unit,
    isLoadingLocation: Boolean,
    locationError: String?,
    onLocationButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Work Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                label = { Text("Enter work location...") },
                placeholder = { Text("Type Here") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = onLocationButtonClick,
                        enabled = !isLoadingLocation
                    ) {
                        if (isLoadingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Use Current Location",
                                tint = Color(0xFF6366F1)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    focusedLabelColor = Color(0xFF6366F1)
                )
            )
            
            // Show detected address in highlighted box
            if (location.isNotBlank() && !isLoadingLocation && locationError == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF3B82F6).copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
//                    Icon(
//                        Icons.Default.Check,
//                        contentDescription = null,
//                        tint = Color(0xFF10B981),
//                        modifier = Modifier.size(20.dp)
//                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "📍Detected Address:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2937),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            
            if (locationError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = locationError,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "\uD83D\uDCCEExample work location \n Building name, street, city, state",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
fun RequirementsSection(
    experienceLevel: String,
    onExperienceLevelChange: (String) -> Unit,
    experienceLevels: List<String>,
    ageRange: String,
    onAgeRangeChange: (String) -> Unit,
    ageRanges: List<String>,
    gender: String,
    onGenderChange: (String) -> Unit,
    genders: List<String>,
    industry: String,
    onIndustryChange: (String) -> Unit,
    industries: List<String>,
    companySize: String,
    onCompanySizeChange: (String) -> Unit,
    companySizes: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Job Requirements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Experience Level
            Text(
                text = "Experience Required",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(experienceLevels) { level ->
                    FilterChip(
                        onClick = { onExperienceLevelChange(level) },
                        label = { Text(level) },
                        selected = experienceLevel == level,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Age Range
            Text(
                text = "Preferred Age Range",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ageRanges) { range ->
                    FilterChip(
                        onClick = { onAgeRangeChange(range) },
                        label = { Text(range) },
                        selected = ageRange == range,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B82F6),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Gender Preference
            Text(
                text = "Gender Preference",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genders.forEach { genderOption ->
                    FilterChip(
                        onClick = { onGenderChange(genderOption) },
                        label = { Text(genderOption) },
                        selected = gender == genderOption,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Industry
            Text(
                text = "Industry",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(industries) { industryOption ->
                    FilterChip(
                        onClick = { onIndustryChange(industryOption) },
                        label = { Text(industryOption) },
                        selected = industry == industryOption,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PerksSelectionSection(
    selectedPerks: Set<JobPerk>,
    onPerksChanged: (Set<JobPerk>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Perks & Benefits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Attract more candidates with attractive benefits",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            PerksSelectionGrid(
                selectedPerks = selectedPerks,
                onPerksChanged = onPerksChanged
            )
        }
    }
}

