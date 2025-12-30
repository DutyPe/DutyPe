package com.example.dutype.employer.screens.postjob

import android.Manifest
import android.net.Uri
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
import androidx.compose.material.icons.filled.Search

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import timber.log.Timber

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
import com.example.dutype.employer.components.JobImageUploadSection
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
import com.example.dutype.location.LocationSuggestion
import com.example.dutype.models.JobListing
import com.example.dutype.utils.LocationService
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    var customCategory by remember { mutableStateOf("") }
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
    
    // Employer Trust Tier (loaded from profile)
    var employerTrustTier by remember { mutableStateOf("VERIFIED") }
    
    // ACCESSIBILITY: Landmark Navigation - helps workers find location by landmarks
    var landmark by remember { mutableStateOf("") }
    
    // JOB IMAGE: Optional image upload for job posting
    var jobImageUri by remember { mutableStateOf<Uri?>(null) }
    var jobImageUrl by remember { mutableStateOf("") }
    var isUploadingJobImage by remember { mutableStateOf(false) }
    val storage = remember { FirebaseStorage.getInstance() }
    
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
    var isSubmittingJob by remember { mutableStateOf(false) } // Local guard against duplicate submissions
    
    // Location coordinates for distance calculation
    var locationLatitude by remember { mutableStateOf(0.0) }
    var locationLongitude by remember { mutableStateOf(0.0) }
    
    // ANTI-FRAUD: Employer's current GPS location for consistency check
    var employerCurrentLatitude by remember { mutableStateOf(0.0) }
    var employerCurrentLongitude by remember { mutableStateOf(0.0) }
    var showLocationWarningDialog by remember { mutableStateOf(false) }
    var locationDistanceKm by remember { mutableStateOf(0.0) }
    var pendingJobSubmission by remember { mutableStateOf(false) } // Flag to proceed after warning
    
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
                    Timber.d("📍 LOCATION DEBUG: Fetching high accuracy location (GPS-level precision)...")
                    // Use getHighAccuracyLocation with GPS-level precision (5-10m)
                    val locationInfo = locationService.getHighAccuracyLocation(
                        timeoutMs = 15000L,  // Wait up to 15 seconds for GPS fix
                        minAccuracyMeters = 10f  // Target 10m GPS precision
                    )
                    if (locationInfo != null) {
                        // Use detailed address for job posting
                        location = locationInfo.getFullAddress()
                        // Store coordinates for distance calculation
                        locationLatitude = locationInfo.latitude
                        locationLongitude = locationInfo.longitude
                        Timber.d("📍 LOCATION DEBUG: ✅ High accuracy location fetched!")
                        Timber.d("📍   - Full Address: ${locationInfo.getFullAddress()}")
                        Timber.d("📍   - Short: ${locationInfo.getShortAddress()}")
                        Timber.d("📍   - Latitude: ${locationInfo.latitude}")
                        Timber.d("📍   - Longitude: ${locationInfo.longitude}")
                        Timber.d("📍   - Accuracy: ${locationInfo.accuracy}m")
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
                        
                        // Get contact phone from profile
                        val savedContactPhone = userDoc.getString("contactPhone") ?: userDoc.getString("phoneNumber")
                        if (!savedContactPhone.isNullOrBlank()) {
                            contactNumber = savedContactPhone
                            Timber.d("✅ Contact number loaded from profile: $contactNumber")
                        }
                        
                        // Get employer trust tier from profile
                        val savedTrustTier = userDoc.getString("trustTier")
                        if (!savedTrustTier.isNullOrBlank()) {
                            employerTrustTier = savedTrustTier
                            Timber.d("✅ Trust tier loaded from profile: $employerTrustTier")
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

    // Actual job submission with coordinates - MUST be defined before submitJob
    fun submitJobWithCoordinates(finalLatitude: Double, finalLongitude: Double) {
        // Double-check to prevent duplicate submissions
        if (employerJobUiState.isCreatingJob) {
            Timber.w("📝 JOB POSTING DEBUG: Already creating job in submitJobWithCoordinates, ignoring")
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
            category = if (category == JobCategory.OTHER && customCategory.isNotBlank()) customCategory else jobPosting.category.name,
            jobType = "Part-time",
            experienceLevel = "Entry Level",
            workingHours = jobPosting.shiftTiming.name,
            ageRange = ageRange,
            gender = gender,
            applicationDeadline = applicationDeadline,
            companySize = companySize,
            industry = industry,
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
        Timber.d("📝   - Latitude: $finalLatitude")
        Timber.d("📝   - Longitude: $finalLongitude")
        Timber.d("📝   - Has valid coordinates: ${finalLatitude != 0.0 || finalLongitude != 0.0}")
        
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
            "latitude" to finalLatitude,
            "longitude" to finalLongitude,
            "payAmount" to jobListing.payAmount,
            "payType" to jobListing.payType,
            "timing" to jobListing.timing,
            "shiftTiming" to jobListing.shiftTiming,
            "description" to jobListing.description,
            "benefits" to jobListing.benefits,
            "requirements" to jobListing.requirements,
            "perks" to selectedPerks.map { it.displayName },
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
            "category" to (if (category == JobCategory.OTHER && customCategory.isNotBlank()) customCategory else category.name),
            "jobType" to jobListing.jobType,
            "experienceLevel" to jobListing.experienceLevel,
            "workingHours" to jobListing.workingHours,
            "applicationDeadline" to jobListing.applicationDeadline,
            "ageRange" to jobListing.ageRange,
            "gender" to jobListing.gender,
            "companySize" to jobListing.companySize,
            "industry" to jobListing.industry,
            "urgency" to jobListing.urgency,
            "applicationCount" to jobListing.applicationCount,
            "landmark" to landmark, // ACCESSIBILITY: Landmark Navigation for workers
            "employerTrustTier" to employerTrustTier, // Employer trust tier for badge display
            "jobImageUrl" to jobImageUrl // Optional job image uploaded by employer
        )
        
        // DEBUG: Log all job data being sent to Firestore
        Timber.d("📝 JOB POSTING DEBUG: Job Data to be saved:")
        jobData.forEach { (key, value) ->
            Timber.d("📝   - $key: $value")
        }
        
        employerJobViewModel.createJob(jobData as Map<String, Any>) { success, message ->
            // Reset local guard
            isSubmittingJob = false
            
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

    // Submit job function - handles geocoding if needed
    fun submitJob(finalLatitude1: Double, finalLongitude1: Double) {
        Timber.d("📝 JOB POSTING DEBUG: submitJob() called")
        
        // Prevent multiple submissions with local guard
        if (isSubmittingJob) {
            Timber.w("📝 JOB POSTING DEBUG: Already submitting (local guard), ignoring duplicate call")
            return
        }
        
        // Prevent multiple submissions with ViewModel state
        if (employerJobUiState.isCreatingJob) {
            Timber.w("📝 JOB POSTING DEBUG: Already creating job (ViewModel), ignoring duplicate call")
            return
        }
        
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
        
        // Set local guard immediately
        isSubmittingJob = true
        
        // If coordinates are 0,0 (user typed location manually), try to geocode
        scope.launch {
            try {
                var finalLatitude = locationLatitude
                var finalLongitude = locationLongitude
                
                if (locationLatitude == 0.0 && locationLongitude == 0.0 && location.isNotBlank()) {
                    Timber.d("📝 JOB POSTING DEBUG: Geocoding manual location: $location")
                    val geocodedLocation = locationService.getCoordinatesFromAddress(location)
                    if (geocodedLocation != null) {
                        finalLatitude = geocodedLocation.latitude
                        finalLongitude = geocodedLocation.longitude
                        Timber.d("📝 JOB POSTING DEBUG: Geocoded - lat: $finalLatitude, lon: $finalLongitude")
                    } else {
                        Timber.w("📝 JOB POSTING DEBUG: Geocoding failed, using 0,0 coordinates")
                    }
                }
                
                // ANTI-FRAUD: Location Consistency Check
                // Get employer's current GPS location and compare with job location
                if (finalLatitude != 0.0 && finalLongitude != 0.0) {
                    try {
                        val employerLocation = locationService.getHighAccuracyLocation(
                            timeoutMs = 10000L,
                            minAccuracyMeters = 50f
                        )
                        
                        if (employerLocation != null) {
                            employerCurrentLatitude = employerLocation.latitude
                            employerCurrentLongitude = employerLocation.longitude
                            
                            // Calculate distance between employer's current location and job location
                            val distance = locationService.calculateDistance(
                                employerCurrentLatitude, employerCurrentLongitude,
                                finalLatitude, finalLongitude
                            )
                            locationDistanceKm = distance
                            
                            Timber.d("🛡️ ANTI-FRAUD: Location consistency check")
                            Timber.d("🛡️   - Employer location: ($employerCurrentLatitude, $employerCurrentLongitude)")
                            Timber.d("🛡️   - Job location: ($finalLatitude, $finalLongitude)")
                            Timber.d("🛡️   - Distance: ${String.format("%.2f", distance)} km")
                            
                            // If distance > 30km, show warning (potential scam center)
                            if (distance > 30.0 && !pendingJobSubmission) {
                                Timber.w("🛡️ ANTI-FRAUD: ⚠️ Location mismatch detected! Distance: ${String.format("%.2f", distance)} km")
                                isSubmittingJob = false
                                showLocationWarningDialog = true
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "🛡️ ANTI-FRAUD: Could not verify employer location, proceeding anyway")
                    }
                }
                
                // Reset pending flag
                pendingJobSubmission = false
                
                // Call the actual submission function (NOT recursive!)
                submitJobWithCoordinates(finalLatitude, finalLongitude)
            } catch (e: Exception) {
                Timber.e(e, "📝 JOB POSTING DEBUG: Error in submitJob")
                isSubmittingJob = false
                Toast.makeText(context, "Error posting job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Professional color palette
    val primaryBlue = Color(0xFF2563EB)
    val successGreen = Color(0xFF10B981)
    val lightGray = Color(0xFFF8FAFC)
    val darkText = Color(0xFF1E293B)
    
    // ANTI-FRAUD: Location Consistency Warning Dialog
    if (showLocationWarningDialog) {
        AlertDialog(
            onDismissRequest = { 
                showLocationWarningDialog = false 
                pendingJobSubmission = false
            },
            icon = {
                Text("⚠️", fontSize = 48.sp)
            },
            title = {
                Text(
                    text = "Location Mismatch Detected",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your current location is ${String.format("%.1f", locationDistanceKm)} km away from the job location.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2)
                    ) {
                        Text(
                            text = "🛡️ This check helps prevent remote scam centers from posting fake local jobs.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF991B1B)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Are you sure you want to post this job?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationWarningDialog = false
                        pendingJobSubmission = true
                        // Retry submission with flag set
                        scope.launch {
                            submitJob(0.0, 0.0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    Text("Post Anyway")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showLocationWarningDialog = false
                        pendingJobSubmission = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

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
                            // Last step - show Post Job button (Back button is already shown above)
                            Button(
                                onClick = {
                                    val finalLatitude = 0.0
                                    val finalLongitude = 0.0
                                    submitJob(finalLatitude, finalLongitude)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                enabled = !employerJobUiState.isCreatingJob && !isSubmittingJob && validateStep(1) && validateStep(2) && validateStep(3),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = successGreen,
                                    disabledContainerColor = Color(0xFFCBD5E1)
                                )
                            ) {
                                if (employerJobUiState.isCreatingJob || isSubmittingJob) {
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
                                onCategoryChange = { category = it },
                                customCategory = customCategory,
                                onCustomCategoryChange = { customCategory = it }
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
                        
                        // Job Image Upload Section (Optional)
                        item {
                            JobImageUploadSection(
                                selectedImageUri = jobImageUri,
                                isUploading = isUploadingJobImage,
                                onImageSelected = { uri ->
                                    jobImageUri = uri
                                    // Upload image to Firebase Storage
                                    scope.launch {
                                        isUploadingJobImage = true
                                        try {
                                            val currentUser = FirebaseAuth.getInstance().currentUser
                                            if (currentUser != null) {
                                                val fileName = "job_image_${System.currentTimeMillis()}.jpg"
                                                val storagePath = "job_images/${currentUser.uid}/$fileName"
                                                val storageRef = storage.reference.child(storagePath)
                                                
                                                Timber.d("📸 JOB IMAGE: Uploading to path: $storagePath")
                                                storageRef.putFile(uri).await()
                                                val downloadUrl = storageRef.downloadUrl.await()
                                                jobImageUrl = downloadUrl.toString()
                                                Timber.d("📸 JOB IMAGE: ✅ Upload successful! URL: $jobImageUrl")
                                                Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Timber.e(e, "📸 JOB IMAGE: ❌ Upload failed")
                                            Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                                            jobImageUri = null
                                            jobImageUrl = ""
                                        } finally {
                                            isUploadingJobImage = false
                                        }
                                    }
                                },
                                onImageRemoved = {
                                    jobImageUri = null
                                    jobImageUrl = ""
                                    Timber.d("📸 JOB IMAGE: Image removed")
                                }
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
                                        Timber.d("📍 LOCATION BUTTON: Permission granted, fetching high accuracy location (Swiggy/Zomato precision)...")
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
                                                    // Use detailed full address for job posting
                                                    location = locationInfo.getFullAddress()
                                                    // Store coordinates for distance calculation
                                                    locationLatitude = locationInfo.latitude
                                                    locationLongitude = locationInfo.longitude
                                                    Timber.d("📍 LOCATION BUTTON: ✅ High accuracy location set - lat: $locationLatitude, lon: $locationLongitude, accuracy: ${locationInfo.accuracy}m")
                                                    Timber.d("📍 LOCATION BUTTON: Full Address: $location")
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
                                },
                                landmark = landmark,
                                onLandmarkChange = { landmark = it },
                                onLocationSelected = { lat, lon ->
                                    // Update coordinates when user selects from search suggestions
                                    locationLatitude = lat
                                    locationLongitude = lon
                                    Timber.d("📍 LOCATION SEARCH: Selected location - lat: $lat, lon: $lon")
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
                                                color = Color(0xFF6B7280)
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
                                description = description,
                                selectedPerks = selectedPerks
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
                .padding(top = 20.dp, bottom = 20.dp)
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stepDescriptions.getOrElse(currentStep - 1) { "" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B7280)
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar only (no step labels below)
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

/**
 * ANTI-FRAUD FEATURE: Structured Job Titles
 * 
 * Employers CANNOT type a job title freely. They must select from a pre-set list.
 * This eliminates "Earn ₹50,000/day working from home" scams instantly.
 * 
 * Implemented: December 27, 2025
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedJobTitleSection(
    title: String,
    onTitleChange: (String) -> Unit,
    category: JobCategory,
    onCategoryChange: (JobCategory) -> Unit,
    customCategory: String = "",
    onCustomCategoryChange: (String) -> Unit = {}
) {
    val primaryBlue = Color(0xFF2563EB)
    var expanded by remember { mutableStateOf(false) }
    
    // Predefined job titles - NO FREE TEXT ALLOWED (Anti-fraud measure)
    val predefinedJobTitles = listOf(
        "Cook / Chef" to "👨‍🍳",
        "Maid / Cleaner" to "🧹",
        "Driver" to "🚗",
        "Security Guard" to "🛡️",
        "Delivery Executive" to "📦",
        "Waiter / Server" to "🍽️",
        "Helper / Assistant" to "🤝",
        "Electrician" to "⚡",
        "Plumber" to "🔧",
        "Painter" to "🎨",
        "Carpenter" to "🪚",
        "Gardener" to "🌱",
        "Caretaker / Nanny" to "👶",
        "Receptionist" to "💼",
        "Cashier" to "💵",
        "Packer / Loader" to "📦",
        "Office Boy" to "🏢",
        "Factory Worker" to "🏭",
        "Construction Worker" to "👷",
        "Shop Assistant" to "🛒",
        "Housekeeping Staff" to "🏠",
        "Kitchen Helper" to "🍳",
        "Watchman" to "👁️",
        "AC Technician" to "❄️",
        "Tailor" to "🧵"
    )
    
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
                            text = "Job Title",
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
                        text = "Select the position you're hiring for",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Anti-fraud info banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEF3C7)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🛡️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "For your safety, job titles are pre-defined to prevent scams",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Dropdown for job title selection (NO FREE TEXT)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { }, // Read-only - selection only
                    readOnly = true,
                    label = { Text("Select Job Title") },
                    placeholder = { Text("Tap to select a job title") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
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
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    predefinedJobTitles.forEach { (jobTitle, icon) ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        jobTitle,
                                        fontWeight = if (title == jobTitle) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                onTitleChange(jobTitle)
                                // Auto-select matching category
                                val matchingCategory = when {
                                    jobTitle.contains("Cook") || jobTitle.contains("Chef") -> JobCategory.COOK
                                    jobTitle.contains("Maid") || jobTitle.contains("Cleaner") -> JobCategory.MAID
                                    jobTitle.contains("Driver") -> JobCategory.DRIVER
                                    jobTitle.contains("Security") || jobTitle.contains("Watchman") -> JobCategory.SECURITY
                                    jobTitle.contains("Delivery") -> JobCategory.DELIVERY
                                    jobTitle.contains("Waiter") || jobTitle.contains("Server") -> JobCategory.WAITER
                                    jobTitle.contains("Electrician") -> JobCategory.ELECTRICIAN
                                    jobTitle.contains("Plumber") -> JobCategory.PLUMBER
                                    jobTitle.contains("Painter") -> JobCategory.PAINTER
                                    jobTitle.contains("Carpenter") -> JobCategory.CARPENTER
                                    jobTitle.contains("Gardener") -> JobCategory.GARDENER
                                    jobTitle.contains("Caretaker") || jobTitle.contains("Nanny") -> JobCategory.CARETAKER
                                    jobTitle.contains("Receptionist") -> JobCategory.RECEPTIONIST
                                    jobTitle.contains("Cashier") -> JobCategory.CASHIER
                                    jobTitle.contains("Packer") || jobTitle.contains("Loader") -> JobCategory.PACKER
                                    else -> JobCategory.HELPER
                                }
                                onCategoryChange(matchingCategory)
                                expanded = false
                            },
                            leadingIcon = null
                        )
                    }
                }
            }
            
            // Show selected category badge
            if (title.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryBlue.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category.icon, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Category: ${category.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = primaryBlue
                        )
                    }
                }
            }
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
                        color = Color(0xFF6B7280)
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
    val payAmountNum = payAmount.toIntOrNull() ?: 0
    val isError = payAmount.isNotEmpty() && payAmountNum > 50000
    
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
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { newValue ->
                            // Only allow numeric input and max ₹50,000
                            if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 5)) {
                                onPayAmountChange(newValue)
                            }
                        },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("500") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isError,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isError) Color(0xFFDC2626) else primaryBlue,
                            focusedLabelColor = if (isError) Color(0xFFDC2626) else primaryBlue,
                            unfocusedBorderColor = if (isError) Color(0xFFDC2626) else Color(0xFFE2E8F0),
                            cursorColor = primaryBlue,
                            errorBorderColor = Color(0xFFDC2626)
                        )
                    )
                    if (isError) {
                        Text(
                            text = "Max ₹50,000 for hyper-local jobs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }
                
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
    onLocationButtonClick: () -> Unit,
    landmark: String = "",
    onLandmarkChange: (String) -> Unit = {},
    onLocationSelected: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val primaryBlue = Color(0xFF2563EB)
    val successGreen = Color(0xFF10B981)
    val scope = rememberCoroutineScope()
    
    // Location search state
    var isSearching by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    
    // Search for locations when user types
    LaunchedEffect(location) {
        if (location.length >= 3 && !isLoadingLocation) {
            kotlinx.coroutines.delay(500) // Debounce
            isSearching = true
            showSuggestions = true
            
            scope.launch {
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocationName(location, 5) { addresses ->
                            searchSuggestions = addresses.mapIndexed { index, address ->
                                LocationSuggestion(
                                    placeId = "geocoder_$index",
                                    displayName = address.getAddressLine(0) ?: location,
                                    city = address.locality ?: address.subAdminArea ?: "",
                                    state = address.adminArea ?: "",
                                    country = address.countryName ?: "India",
                                    postalCode = address.postalCode ?: "",
                                    area = address.subLocality ?: "",
                                    latitude = address.latitude,
                                    longitude = address.longitude
                                )
                            }
                            isSearching = false
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(location, 5)
                        searchSuggestions = addresses?.mapIndexed { index, address ->
                            LocationSuggestion(
                                placeId = "geocoder_$index",
                                displayName = address.getAddressLine(0) ?: location,
                                city = address.locality ?: address.subAdminArea ?: "",
                                state = address.adminArea ?: "",
                                country = address.countryName ?: "India",
                                postalCode = address.postalCode ?: "",
                                area = address.subLocality ?: "",
                                latitude = address.latitude,
                                longitude = address.longitude
                            )
                        } ?: emptyList()
                        isSearching = false
                    }
                } catch (e: Exception) {
                    searchSuggestions = emptyList()
                    isSearching = false
                }
            }
        } else {
            searchSuggestions = emptyList()
            showSuggestions = false
        }
    }
    
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
                        text = "Search or use GPS to set location",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            OutlinedTextField(
                value = location,
                onValueChange = { 
                    onLocationChange(it)
                    showSuggestions = true
                },
                label = { Text("Search work location") },
                placeholder = { Text("Type to search (e.g., Koramangala, Bangalore)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(start = 8.dp),
                            strokeWidth = 2.dp,
                            color = primaryBlue
                        )
                    } else {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                },
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
            
            // Location search suggestions dropdown
            if (showSuggestions && searchSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "📍 Select a location",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        searchSuggestions.take(5).forEach { suggestion ->
                            Surface(
                                onClick = {
                                    onLocationChange(suggestion.displayName)
                                    onLocationSelected?.invoke(suggestion.latitude, suggestion.longitude)
                                    showSuggestions = false
                                    searchSuggestions = emptyList()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = primaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion.area.ifBlank { suggestion.city },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = suggestion.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF6B7280),
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                            if (suggestion != searchSuggestions.last()) {
                                Divider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
            
            // ACCESSIBILITY FEATURE: Landmark Navigation
            // Workers recognize landmarks better than street names
            Spacer(modifier = Modifier.height(16.dp))
            
            // Landmark info banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF0FDF4)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏛️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add a nearby landmark to help workers find the location easily",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF166534)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = landmark,
                onValueChange = onLandmarkChange,
                label = { Text("Nearby Landmark (Optional)") },
                placeholder = { Text("e.g., Near Big Temple, Opposite Metro Station") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    Text("🏛️", fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    focusedLabelColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = Color(0xFF10B981),
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
                        color = Color(0xFF6B7280)
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
                            color = Color(0xFF6B7280)
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

