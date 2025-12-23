package com.example.dutype.employer.screens.postjob

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.LocationOn

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import timber.log.Timber
import androidx.compose.material3.TextButton

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.employer.components.CategorySelectionGrid
import com.example.dutype.employer.components.ContactSection
import com.example.dutype.employer.components.JobDescriptionSection
import com.example.dutype.employer.components.JobSummaryCard
import com.example.dutype.employer.components.PayTypeDropdown
import com.example.dutype.employer.components.PerksSelectionGrid

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
    onJobPosted: (() -> Unit)? = null,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Set status bar to white for this screen
    LaunchedEffect(Unit) {
        onStatusBarColorChange?.invoke(Color.White)
    }
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
    var vacancies by remember { mutableStateOf("") }
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
    
    // Location coordinates for distance calculation
    var locationLatitude by remember { mutableStateOf(0.0) }
    var locationLongitude by remember { mutableStateOf(0.0) }
    
    // LazyList state for scrolling
    val listState = rememberLazyListState()
    
    // Scroll to top when step changes
    LaunchedEffect(currentStep) {
        listState.animateScrollToItem(0)
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.d("📍 LOCATION DEBUG: Permission result - isGranted: $isGranted")
        if (isGranted) {
            isLoadingLocation = true
            locationError = null
            scope.launch {
                try {
                    Timber.d("📍 LOCATION DEBUG: Fetching current location...")
                    val locationInfo = locationService.getCurrentLocation()
                    if (locationInfo != null) {
                        location = locationInfo.address
                        // Store coordinates for distance calculation
                        locationLatitude = locationInfo.latitude
                        locationLongitude = locationInfo.longitude
                        Timber.d("📍 LOCATION DEBUG: Location fetched successfully!")
                        Timber.d("📍   - Address: ${locationInfo.address}")
                        Timber.d("📍   - Latitude: ${locationInfo.latitude}")
                        Timber.d("📍   - Longitude: ${locationInfo.longitude}")
                    } else {
                        Timber.w("📍 LOCATION DEBUG: locationInfo is null")
                        locationError = "Unable to get current location"
                    }
                } catch (e: Exception) {
                    Timber.e(e, "📍 LOCATION DEBUG: Error getting location")
                    locationError = "Error getting location: ${e.message}"
                } finally {
                    isLoadingLocation = false
                }
            }
        } else {
            Timber.w("📍 LOCATION DEBUG: Permission denied")
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
                            Timber.d("✅ Company name loaded from users collection: $companyName")
                        } else {
                            Timber.w("⚠️ Company name is blank in users collection!")
                        }
                        
                        // Get full name for employer name (for reference only)
                        val savedFullName = userDoc.getString("fullName")
                        if (!savedFullName.isNullOrBlank()) {
                            employerName = savedFullName
                        }
                    } else {
                        Timber.e("❌ User document not found in users collection!")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Error loading employer profile")
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
        Timber.d("📝 JOB POSTING DEBUG: submitJob() called")
        
        if (!validateStep(4)) {
            Timber.w("📝 JOB POSTING DEBUG: Step 4 validation failed")
            return
        }
        
        // Validate that company name is available (MANDATORY)
        if (companyName.isBlank()) {
            Timber.w("📝 JOB POSTING DEBUG: Company name is blank - redirecting to profile")
            Toast.makeText(context, "Please complete your company profile first to post jobs.", Toast.LENGTH_LONG).show()
            // Navigate to profile screen to complete company information
            navController.navigate(Routes.EMPLOYER_PROFILE)
            return
        }

        Timber.d("📝 JOB POSTING DEBUG: Creating job posting...")
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
        
        // Extract area and city from location string for display
        val locationParts = location.split(",").map { it.trim() }
        val area = locationParts.getOrNull(0) ?: location
        val city = locationParts.getOrNull(1) ?: locationParts.getOrNull(0) ?: location
        
        // DEBUG: Log location coordinates
        Timber.d("📝 JOB POSTING DEBUG: Location Details:")
        Timber.d("📝   - Raw location: $location")
        Timber.d("📝   - Area: $area")
        Timber.d("📝   - City: $city")
        Timber.d("📝   - Latitude: $locationLatitude")
        Timber.d("📝   - Longitude: $locationLongitude")
        Timber.d("📝   - Has valid coordinates: ${locationLatitude != 0.0 || locationLongitude != 0.0}")
        
        // Convert JobListing to Map for Firestore (removed duplicates)
        val jobData = mapOf(
            "title" to jobListing.title,
            "companyName" to jobListing.companyName,
            "company" to jobListing.company,
            "employerName" to employerName, // Add employer name for reference
            "location" to jobListing.location,
            "specificLocation" to jobListing.specificLocation,
            "locationNearby" to jobListing.locationNearby,
            "area" to area,
            "city" to city,
            "latitude" to locationLatitude,
            "longitude" to locationLongitude,
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
        
        // DEBUG: Log all job data being sent to Firestore
        Timber.d("📝 JOB POSTING DEBUG: Job Data to be saved:")
        jobData.forEach { (key, value) ->
            Timber.d("📝   - $key: $value")
        }
        
        employerJobViewModel.createJob(jobData as Map<String, Any>) { success, message ->
            if (success) {
                Timber.i("📝 JOB POSTING DEBUG: ✅ Job posted successfully!")
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
                Timber.e("📝 JOB POSTING DEBUG: ❌ Job posting failed: $message")
                Toast.makeText(context, "Error posting job: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Professional color palette
    val primaryBlue = Color(0xFF2563EB)
    val successGreen = Color(0xFF10B981)
    val lightGray = Color(0xFFF8FAFC)
    val darkText = Color(0xFF1E293B)

    Scaffold(
        containerColor = lightGray,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = Color.White
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)))
                                )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = darkText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back", color = darkText, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Cancel", color = darkText, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (currentStep < totalSteps) {
                            Button(
                                onClick = { currentStep++ },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                enabled = validateStep(currentStep),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryBlue,
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                )
                            ) {
                                Text("Continue", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { submitJob() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                enabled = !employerJobUiState.isCreatingJob && validateStep(1) && validateStep(2) && validateStep(3),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = successGreen,
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                )
                            ) {
                                if (employerJobUiState.isCreatingJob) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Post Job", fontWeight = FontWeight.SemiBold)
                                }
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(lightGray)
                .padding(paddingValues)
        ) {
            // Professional Step Indicator
            StepProgressIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps,
                primaryColor = primaryBlue,
                successColor = successGreen
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    top = 8.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentStep) {
                    1 -> {
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
                                    Timber.d("📍 LOCATION BUTTON: Clicked - checking permission...")
                                    if (locationService.hasLocationPermission()) {
                                        Timber.d("📍 LOCATION BUTTON: Permission granted, fetching location...")
                                        isLoadingLocation = true
                                        locationError = null
                                        scope.launch {
                                            try {
                                                val locationInfo = locationService.getCurrentLocation()
                                                if (locationInfo != null) {
                                                    location = locationInfo.address
                                                    // Store coordinates for distance calculation
                                                    locationLatitude = locationInfo.latitude
                                                    locationLongitude = locationInfo.longitude
                                                    Timber.d("📍 LOCATION BUTTON: ✅ Location set - lat: $locationLatitude, lon: $locationLongitude")
                                                    Timber.d("📍 LOCATION BUTTON: Address: $location")
                                                } else {
                                                    Timber.w("📍 LOCATION BUTTON: locationInfo is null")
                                                    locationError = "Unable to get current location"
                                                }
                                            } catch (e: Exception) {
                                                Timber.e(e, "📍 LOCATION BUTTON: Error getting location")
                                                locationError = "Error getting location"
                                            } finally {
                                                isLoadingLocation = false
                                            }
                                        }
                                    } else {
                                        Timber.d("📍 LOCATION BUTTON: Requesting permission...")
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
                            PolishedCard {
                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🕐", fontSize = 18.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Work Schedule & Urgency",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "When do you need someone?",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))

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

                        // Final Review Header
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✨", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Almost Done!",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E40AF)
                                        )
                                        Text(
                                            text = "Review your job posting below",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF3B82F6)
                                        )
                                    }
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

// Clean Minimal Step Progress Indicator - Enhanced Version
@Composable
fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    primaryColor: Color,
    successColor: Color
) {
    val stepLabels = listOf("Job Details", "Pay & Location", "Requirements", "Review & Post")
    val stepIcons = listOf("📝", "💰", "📋", "✨")
    val stepDescriptions = listOf(
        "Title, category & description",
        "Salary, location & vacancies", 
        "Experience & preferences",
        "Final review before posting"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 24.dp)
        ) {
            // Header with step count and icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stepIcons.getOrElse(currentStep - 1) { "📝" },
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stepLabels.getOrElse(currentStep - 1) { "" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stepDescriptions.getOrElse(currentStep - 1) { "" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
                
                // Step counter badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = primaryColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "$currentStep/$totalSteps",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Enhanced Progress bar with step indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    val stepNumber = index + 1
                    val isCompleted = stepNumber < currentStep
                    val isCurrent = stepNumber == currentStep
                    
                    val barColor by animateColorAsState(
                        targetValue = when {
                            isCompleted -> successColor
                            isCurrent -> primaryColor
                            else -> Color(0xFFE2E8F0)
                        },
                        animationSpec = tween(300),
                        label = "barColor"
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Step labels below progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Details", "Pay", "Require", "Post").forEachIndexed { index, label ->
                    val stepNumber = index + 1
                    val isCompleted = stepNumber < currentStep
                    val isCurrent = stepNumber == currentStep
                    
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isCompleted -> successColor
                            isCurrent -> primaryColor
                            else -> Color(0xFFCBD5E1)
                        },
                        modifier = Modifier.width(50.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Enhanced Polished Card wrapper with gradient accent
@Composable
fun PolishedCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF2563EB),
    showAccent: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        tonalElevation = 1.dp
    ) {
        if (showAccent) {
            Row {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.5f)
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                        )
                )
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        } else {
            content()
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
    val primaryBlue = Color(0xFF2563EB)
    
    PolishedCard {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Section header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📝", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Job Title & Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "*",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "What position are you hiring for?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Job Title") },
                placeholder = { Text("e.g., Waiter, Driver, Cook") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    focusedLabelColor = primaryBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = primaryBlue,
                    unfocusedContainerColor = Color(0xFFFAFAFA),
                    focusedContainerColor = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFFF3E8FF), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏷️", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
    val primaryBlue = Color(0xFF2563EB)
    
    PolishedCard {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏰", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Work Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Full-time, part-time or flexible?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(workTypes) { type ->
                    FilterChip(
                        onClick = { onWorkTypeChange(type) },
                        label = { 
                            Text(
                                type,
                                fontWeight = if (workType == type) FontWeight.SemiBold else FontWeight.Normal
                            ) 
                        },
                        selected = workType == type,
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF475569)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = workType == type
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
    val primaryBlue = Color(0xFF2563EB)
    
    PolishedCard {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💰", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Payment Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "*",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "How much will you pay?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = payAmount,
                    onValueChange = onPayAmountChange,
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("500") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryBlue,
                        focusedLabelColor = primaryBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        cursorColor = primaryBlue
                    )
                )
                
                PayTypeDropdown(
                    selectedType = payType,
                    onTypeSelected = onPayTypeChange,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFFEF3C7),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Competitive rates attract more applicants",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF92400E),
                    fontWeight = FontWeight.Medium
                )
            }
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
    val primaryBlue = Color(0xFF2563EB)
    val successGreen = Color(0xFF10B981)
    
    PolishedCard {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFDBEAFE), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📍", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Work Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "*",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = "Where will the work be done?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                label = { Text("Enter work location") },
                placeholder = { Text("Building, Street, City") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    Surface(
                        onClick = onLocationButtonClick,
                        enabled = !isLoadingLocation,
                        shape = RoundedCornerShape(10.dp),
                        color = primaryBlue.copy(alpha = 0.1f),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = primaryBlue
                                )
                            } else {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Use Current Location",
                                    tint = primaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    focusedLabelColor = primaryBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = primaryBlue,
                    unfocusedContainerColor = Color(0xFFFAFAFA),
                    focusedContainerColor = Color.White
                )
            )
            
            // Show detected address in highlighted box
            if (location.isNotBlank() && !isLoadingLocation && locationError == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            successGreen.copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = successGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = successGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Location Set",
                            style = MaterialTheme.typography.labelMedium,
                            color = successGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1E293B),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            
            if (locationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFFEE2E2),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ $locationError",
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "📍 Tap the location icon to auto-detect your address",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
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
    PolishedCard {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF3E8FF), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📋", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Job Requirements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Who are you looking for?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Experience Level
            RequirementChipSection(
                title = "Experience Required",
                icon = "💼",
                options = experienceLevels,
                selectedOption = experienceLevel,
                onOptionSelected = onExperienceLevelChange,
                selectedColor = Color(0xFF10B981)
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Age Range
            RequirementChipSection(
                title = "Preferred Age Range",
                icon = "👤",
                options = ageRanges,
                selectedOption = ageRange,
                onOptionSelected = onAgeRangeChange,
                selectedColor = Color(0xFF2563EB)
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Gender Preference
            Text(
                text = "Gender Preference",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                genders.forEach { genderOption ->
                    FilterChip(
                        onClick = { onGenderChange(genderOption) },
                        label = { 
                            Text(
                                genderOption,
                                fontWeight = if (gender == genderOption) FontWeight.Medium else FontWeight.Normal
                            ) 
                        },
                        selected = gender == genderOption,
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF475569)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = gender == genderOption
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Industry
            RequirementChipSection(
                title = "Industry",
                icon = "🏢",
                options = industries,
                selectedOption = industry,
                onOptionSelected = onIndustryChange,
                selectedColor = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
private fun RequirementChipSection(
    title: String,
    icon: String = "",
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    selectedColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon.isNotEmpty()) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569)
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(options) { option ->
            FilterChip(
                onClick = { onOptionSelected(option) },
                label = { 
                    Text(
                        option,
                        fontWeight = if (selectedOption == option) FontWeight.Medium else FontWeight.Normal
                    ) 
                },
                selected = selectedOption == option,
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedColor,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFFF1F5F9),
                    labelColor = Color(0xFF475569)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent,
                    enabled = true,
                    selected = selectedOption == option
                )
            )
        }
    }
}

@Composable
fun PerksSelectionSection(
    selectedPerks: Set<JobPerk>,
    onPerksChanged: (Set<JobPerk>) -> Unit
) {
    PolishedCard {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎁", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Perks & Benefits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Attract more candidates",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                
                // Selected count badge
                if (selectedPerks.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Text(
                            text = "${selectedPerks.size} selected",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            PerksSelectionGrid(
                selectedPerks = selectedPerks,
                onPerksChanged = onPerksChanged
            )
        }
    }
}

