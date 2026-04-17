package com.example.dutype.employer.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.data.JobDraftDataStore
import com.example.dutype.components.CommonHeader
import com.example.dutype.employer.components.ContactSection
import com.example.dutype.employer.components.JobDescriptionSection
import com.example.dutype.employer.components.JobImageUploadSection
import com.example.dutype.employer.components.JobSummaryCard
import com.example.dutype.employer.components.PayTypeDropdown
import com.example.dutype.employer.components.PerksSelectionGrid
import com.example.dutype.employer.components.VacanciesSection
import com.example.dutype.employer.components.WorkScheduleSection
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.JobPerk
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.employer.models.JobUrgency
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
import com.example.dutype.employer.viewmodels.AIJobPostingViewModel
import com.example.dutype.location.LocationSuggestion
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.JobValidationUtils
import com.example.dutype.utils.PayRateValidationResult
import com.example.dutype.utils.ValidationResult
import com.example.dutype.utils.findActivity
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.ByteArrayOutputStream

/**
 * PERFORMANCE FIX: Compress image before upload to reduce bandwidth and storage costs
 * Reduces image size by ~60-80% while maintaining acceptable quality
 * 
 * @param context Android context for content resolver
 * @param uri Image URI to compress
 * @param maxWidth Maximum width in pixels (default 1200px for job images)
 * @param quality JPEG quality 0-100 (default 85 for good balance)
 * @return Compressed image as ByteArray, or null if compression fails
 */
private fun compressImage(
    context: Context,
    uri: Uri,
    maxWidth: Int = 1200,
    quality: Int = 85
): ByteArray? {
    return try {
        // Load bitmap from URI
        val inputStream = context.contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()
        
        // Calculate sample size for efficient memory usage
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        var sampleSize = 1
        
        if (originalWidth > maxWidth) {
            sampleSize = (originalWidth.toFloat() / maxWidth).toInt()
        }
        
        // Decode with sample size
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val newInputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(newInputStream, null, decodeOptions)
        newInputStream?.close()
        
        if (bitmap == null) {
            Timber.w("📸 COMPRESS: Failed to decode bitmap")
            return null
        }
        
        // Scale if still too large
        val scaledBitmap = if (bitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / bitmap.width
            val newHeight = (bitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true).also {
                if (it != bitmap) bitmap.recycle()
            }
        } else {
            bitmap
        }
        
        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val compressedBytes = outputStream.toByteArray()
        
        // Cleanup
        scaledBitmap.recycle()
        outputStream.close()
        
        Timber.d("📸 COMPRESS: Original size estimate: ${originalWidth}x${originalHeight}, Compressed: ${compressedBytes.size / 1024}KB")
        compressedBytes
    } catch (e: Exception) {
        Timber.e(e, "📸 COMPRESS: Failed to compress image")
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    navController: NavController,
    rootNavController: NavController? = null,
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
    // LocationService accessed via FirestoreJobViewModel (proper DI pattern)
    val jobViewModel: com.example.dutype.viewmodels.FirestoreJobViewModel = hiltViewModel()
    val locationService = jobViewModel.locationService
    val employerJobViewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    
    // WorkLocationManager for saved locations quick-pick (from shared ViewModel)
    val workLocationManager = jobViewModel.workLocationManager
    var savedWorkLocations by remember { mutableStateOf<List<com.example.dutype.models.WorkLocation>>(emptyList()) }
    LaunchedEffect(Unit) {
        workLocationManager.getWorkLocations().onSuccess { savedWorkLocations = it }
    }
    
    // Get InAppReviewTriggerService from Hilt
    val reviewTriggerServiceHolder: com.example.dutype.viewmodels.InAppReviewTriggerServiceHolder = hiltViewModel()
    val reviewTriggerService = reviewTriggerServiceHolder.service
    
    val employerJobUiState by employerJobViewModel.uiState.collectAsState()
    
    // Profile Completion Service for pre-check
    val profileCompletionService = jobViewModel.profileCompletionService

    // AI Backend Repository for fraud detection
    val aiJobPostingViewModel: AIJobPostingViewModel = hiltViewModel()
    val aiUiState by aiJobPostingViewModel.uiState.collectAsState()
    
    // PROFILE COMPLETION CHECK STATE - Check only when submitting, not on screen load
    var isCheckingProfile by remember { mutableStateOf(false) }
    var profileCheckResult by remember { mutableStateOf<com.example.dutype.services.ProfileCompletionService.PreJobPostCheckResult?>(null) }
    var showProfileIncompleteDialog by remember { mutableStateOf(false) }
    var pendingJobSubmitAfterProfileCheck by remember { mutableStateOf(false) }

    // Critical publish checks tracked for the launch bar
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

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    
    // ANTI-FRAUD: Validation state for No-Data-Entry Firewall and Pay Rate Guardrails
    var scamValidationResult by remember { mutableStateOf<ValidationResult?>(null) }
    var payRateValidationResult by remember { mutableStateOf<PayRateValidationResult?>(null) }
    var showScamWarningDialog by remember { mutableStateOf(false) }
    var showPayRateWarningDialog by remember { mutableStateOf(false) }
    var isSubmittingJob by remember { mutableStateOf(false) } // Local guard against duplicate submissions
    
    // AI FRAUD DETECTION: State for AI-powered job screening
    var isAIAnalyzing by remember { mutableStateOf(false) }
    var aiBlockReason by remember { mutableStateOf<String?>(null) }
    var showAIBlockDialog by remember { mutableStateOf(false) }
    var aiRiskScore by remember { mutableStateOf(0) }

    // AI REAL-TIME REVIEW: Show user feedback while typing
    var aiReviewStatus by remember { mutableStateOf("pending") } // pending, analyzing, good, warning, bad
    var aiReviewMessage by remember { mutableStateOf("") }
    var aiDetectedIssues by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAIReviewing by remember { mutableStateOf(false) }

    // Location coordinates for distance calculation
    var locationLatitude by remember { mutableDoubleStateOf(0.0) }
    var locationLongitude by remember { mutableStateOf(0.0) }
    
    // ANTI-FRAUD: Employer's current GPS location for consistency check
    var employerCurrentLatitude by remember { mutableStateOf(0.0) }
    var employerCurrentLongitude by remember { mutableStateOf(0.0) }
    var showLocationWarningDialog by remember { mutableStateOf(false) }
    var locationDistanceKm by remember { mutableStateOf(0.0) }
    var pendingJobSubmission by remember { mutableStateOf(false) } // Flag to proceed after warning
    
    // Guest mode - Login bottom sheet state for job posting
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    
    // LazyList state for the single-canvas studio layout
    val listState = rememberLazyListState()

    // P2 FIX: Auto-save draft on field changes (debounced 2 seconds)
    val draftTrigger = remember { MutableStateFlow(0L) }

    @OptIn(FlowPreview::class)
    LaunchedEffect(Unit) {
        draftTrigger
            .debounce(2000L) // 2 second debounce
            .collect {
                if (title.isNotBlank() || description.isNotBlank() || payAmount.isNotBlank() || location.isNotBlank()) {
                    val draft = JobDraftDataStore.JobDraft(
                        title = title,
                        description = description,
                        payAmount = payAmount,
                        payType = payType,
                        location = location,
                        category = category,
                        customCategory = customCategory,
                        vacancies = vacancies,
                        contactNumber = contactNumber,
                        shiftTiming = shiftTiming,
                        urgency = urgency,
                        perks = selectedPerks,
                        workType = workType,
                        experienceLevel = experienceLevel,
                        ageRange = ageRange,
                        gender = gender,
                        landmark = landmark,
                        requirements = requirements,
                        benefits = benefits
                    )
                    employerJobViewModel.saveDraft(draft)
                    Timber.d("📝 AUTO-SAVE: Draft saved")
                }
            }
    }

    // Trigger auto-save when form fields change
    LaunchedEffect(title, description, payAmount, location, category, vacancies, contactNumber) {
        draftTrigger.value = System.currentTimeMillis()
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
                        timeoutMs = 5000L,  // Fall back to manual selection quickly
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

    // P1 FIX: Load employer profile data from CACHE (5-minute TTL)
    // P2 FIX: Restore draft if available
    // NOTE: Profile check moved to job submission time - allows non-logged-in users to fill form
    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            scope.launch {
                try {
                    // P1 FIX: Use cached profile instead of fresh Firestore fetch
                    val cachedProfile = employerJobViewModel.getCachedProfile()

                    if (cachedProfile != null) {
                        // Load from cache (fast path)
                        if (cachedProfile.companyName.isNotBlank()) {
                            companyName = cachedProfile.companyName
                            Timber.d("✅ Company name loaded from CACHE: $companyName")
                        }
                        if (cachedProfile.employerName.isNotBlank()) {
                            employerName = cachedProfile.employerName
                        }
                        if (cachedProfile.contactPhone.isNotBlank()) {
                            contactNumber = cachedProfile.contactPhone
                            Timber.d("✅ Contact number loaded from CACHE: $contactNumber")
                        }
                        if (cachedProfile.trustTier.isNotBlank()) {
                            employerTrustTier = cachedProfile.trustTier
                            Timber.d("✅ Trust tier loaded from CACHE: $employerTrustTier")
                        }
                    } else {
                        // Fallback to direct Firestore fetch (cache miss)
                        Timber.d("📦 Cache miss, fetching from Firestore...")
                        val db = FirebaseFirestore.getInstance()
                        val userDoc = db.collection(com.example.dutype.firestore.FirestoreCollections.USERS).document(currentUser.uid).get().await()
                        val employerDoc = db.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES).document(currentUser.uid).get().await()

                        if (userDoc.exists()) {
                            val savedFullName = userDoc.getString("fullName")
                            if (!savedFullName.isNullOrBlank()) {
                                employerName = savedFullName
                            }
                            val savedContactPhone = userDoc.getString("phone")
                            if (!savedContactPhone.isNullOrBlank()) {
                                contactNumber = savedContactPhone
                            }
                        }
                        if (employerDoc.exists()) {
                            val savedCompanyName = employerDoc.getString("companyName")
                            if (!savedCompanyName.isNullOrBlank()) {
                                companyName = savedCompanyName
                            }
                        }
                    }

                    // P2 FIX: Restore draft if available
                    val savedDraft = employerJobViewModel.getSavedDraft()
                    if (savedDraft != null && savedDraft.hasContent()) {
                        Timber.d("📝 Restoring job draft...")
                        title = savedDraft.title
                        description = savedDraft.description
                        payAmount = savedDraft.payAmount
                        payType = savedDraft.payType
                        location = savedDraft.location
                        category = savedDraft.category
                        customCategory = savedDraft.customCategory
                        vacancies = savedDraft.vacancies
                        // Don't override contact number from profile
                        if (contactNumber.isBlank()) {
                            contactNumber = savedDraft.contactNumber
                        }
                        shiftTiming = savedDraft.shiftTiming
                        urgency = savedDraft.urgency
                        selectedPerks = savedDraft.perks
                        workType = savedDraft.workType
                        experienceLevel = savedDraft.experienceLevel
                        ageRange = savedDraft.ageRange
                        gender = savedDraft.gender
                        landmark = savedDraft.landmark
                        requirements = savedDraft.requirements
                        benefits = savedDraft.benefits
                        Timber.d("✅ Draft restored successfully")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Error loading employer profile or draft")
                }
            }
        }
    }
    
    // Clear legacy redirect flag from the old multi-step flow
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("dutype_prefs", android.content.Context.MODE_PRIVATE)
        val shouldJumpToLastStep = prefs.getBoolean("jump_to_post_job_last_step", false)
        if (shouldJumpToLastStep) {
            Timber.d("📍 Clearing legacy post-job step redirect flag")
            prefs.edit().remove("jump_to_post_job_last_step").apply()
        }
    }

    // Validation functions for each step
    fun validateStep(step: Int): Boolean {
        return when (step) {
            1 -> {
                // Basic validation
                if (title.isBlank() || description.isBlank()) return false
                
                // ANTI-FRAUD: No-Data-Entry Firewall - Check for scam keywords
                val scamCheck = JobValidationUtils.validateAgainstScamKeywords(title, description)
                scamValidationResult = scamCheck
                scamCheck.isValid
            }
            2 -> {
                // Basic validation
                if (payAmount.isBlank() || location.isBlank()) return false
                
                // ANTI-FRAUD: Pay Rate Guardrails - Validate pay rate
                val payCheck = JobValidationUtils.validatePayRate(category, payType, payAmount)
                payRateValidationResult = payCheck
                // Allow proceeding but show warning (don't block)
                true
            }
            3 -> contactNumber.isNotBlank()
            4 -> true
            else -> false
        }
    }
    
    // Get suggested pay range for current category
    fun getSuggestedPayRange(): String {
        return JobValidationUtils.formatSuggestedRange(category, payType)
    }

    val hasValidJobCoordinates = com.example.dutype.utils.GeoUtils.hasValidCoordinates(
        locationLatitude,
        locationLongitude
    )

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
        val normalizedJobType = if (category == JobCategory.OTHER && customCategory.isNotBlank()) {
            customCategory.trim()
        } else {
            category.displayName
        }
        val normalizedUrgency = when (urgency) {
            JobUrgency.IMMEDIATE, JobUrgency.URGENT -> "HIGH"
            JobUrgency.NORMAL -> "MEDIUM"
            JobUrgency.FLEXIBLE -> "LOW"
        }
        val normalizedBenefits = (
            selectedPerks.map { it.displayName } +
                benefits.split(",").map { it.trim() }.filter { it.isNotBlank() }
            ).distinct()
        
        // Build job data map directly from jobPosting (no intermediate JobListing needed)
        val jobData = mapOf(
            // Core job information
            "title" to jobPosting.title,
            "jobType" to normalizedJobType,
            
            // Location information
            "location" to mapOf("lat" to finalLatitude, "lng" to finalLongitude),
            "addressText" to jobPosting.location,
            
            // Pay information
            "salary" to (jobPosting.payAmount.toDoubleOrNull() ?: 0.0),
            "salaryType" to jobPosting.payType.name,
            
            // Job details
            "description" to jobPosting.description,
            "gender" to gender,
            "experienceRequired" to experienceLevel,
            "shiftTiming" to shiftTiming.displayName,
            "vacancies" to (vacancies.toIntOrNull() ?: 1),
            "benefits" to normalizedBenefits,
            
            // Contact information
            "contactNumber" to jobPosting.contactNumber,
            "workingHours" to workType,
            "educationRequired" to requirements.trim(),
            
            // Job metadata — createdAt/expiresAt set by JobFirestoreService.createJob()
            "urgency" to normalizedUrgency,
            
            // System fields
            "employerId" to (employerId ?: "")
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
                
                // Trigger in-app review after successful job posting
                context.findActivity()?.let { activity ->
                    reviewTriggerService.onEmployerJobPosted(activity)
                }
                
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

    // Submit job function - handles login check, profile check, and geocoding
    fun submitJob(finalLatitude1: Double, finalLongitude1: Double) {
        Timber.d("📝 JOB POSTING DEBUG: submitJob() called")
        
        // STEP 1: Check if user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Timber.d("📝 JOB POSTING DEBUG: User not logged in, showing login sheet")
            showLoginBottomSheet = true
            return
        }
        
        // SENIOR FIX: Use ONLY ViewModel state as single source of truth
        // Prevents race condition from dual guards desynchronizing
        // Check and set atomically (in practice, Compose state updates are on main thread)
        if (employerJobUiState.isCreatingJob) {
            Timber.w("📝 JOB POSTING DEBUG: Already creating job (ViewModel guard), ignoring duplicate call")
            return
        }
        
        if (!validateStep(4)) {
            Timber.w("📝 JOB POSTING DEBUG: Step 4 validation failed")
            return
        }
        
        // Atomically set ViewModel state to guard against all competing threads/calls
        // Remove local isSubmittingJob flag - causes dual guard desync
        // isSubmittingJob = true  // REMOVED: Local flag causes race condition
        isCheckingProfile = true
        
        // STEP 2: Check profile completion (use cached result if available)
        scope.launch {
            try {
                // Use cached result if already checked in this session
                val checkResult = if (profileCheckResult != null && profileCheckResult!!.canPost) {
                    Timber.d("✅ PROFILE CHECK: Using cached result - ${profileCheckResult!!.completionPercentage}% complete")
                    profileCheckResult!!
                } else {
                    val result = profileCompletionService.preJobPostCheck(currentUser.uid)
                    profileCheckResult = result
                    result
                }
                
                isCheckingProfile = false
                
                if (!checkResult.canPost) {
                    Timber.w("⚠️ PROFILE CHECK: Employer cannot post jobs - ${checkResult.completionPercentage}% complete")
                    // SENIOR FIX: ViewModel state resets automatically; no need for local flag
                    showProfileIncompleteDialog = true
                    return@launch
                }
                
                Timber.d("✅ PROFILE CHECK: Employer can post jobs - ${checkResult.completionPercentage}% complete")
                
                // Validate that company name is available (MANDATORY)
                if (companyName.isBlank()) {
                    // Try to load company name from profile
                    val profileResult = profileCompletionService.getEmployerProfileData(currentUser.uid)
                    profileResult.onSuccess { profileData ->
                        val savedCompanyName = profileData["companyName"] as? String
                        if (!savedCompanyName.isNullOrBlank()) {
                            companyName = savedCompanyName
                        }
                    }
                    
                    if (companyName.isBlank()) {
                        Timber.w("📝 JOB POSTING DEBUG: Company name is blank - redirecting to profile")
                        // SENIOR FIX: ViewModel state resets automatically; no need for local flag
                        val navToUse = rootNavController ?: navController
                        navToUse.navigate(
                            Routes.employerProfileSetupWithReturnRoute(Routes.EMPLOYER_POST_JOB)
                        ) {
                            launchSingleTop = true
                        }
                        return@launch
                    }
                }
                
                // STEP 3: Proceed with job submission
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
                        Timber.w("📝 JOB POSTING DEBUG: Geocoding failed")
                    }
                }

                if (!com.example.dutype.utils.GeoUtils.hasValidCoordinates(finalLatitude, finalLongitude)) {
                    locationError = context.getString(R.string.valid_job_location_required)
                    // SENIOR FIX: ViewModel state resets automatically; no need for local flag
                    Toast.makeText(context, R.string.valid_job_location_required, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // ANTI-FRAUD: Location Consistency Check (NON-BLOCKING)
                // Run in background - don't block job posting
                if (finalLatitude != 0.0 && finalLongitude != 0.0) {
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        try {
                            val employerLocation = locationService.getHighAccuracyLocation(
                                timeoutMs = 5000L,  // Reduced timeout
                                minAccuracyMeters = 100f  // Less strict accuracy
                            )
                            
                            if (employerLocation != null) {
                                val distance = locationService.calculateDistance(
                                    employerLocation.latitude, employerLocation.longitude,
                                    finalLatitude, finalLongitude
                                )
                                
                                Timber.d("🛡️ ANTI-FRAUD: Location consistency check - Distance: ${String.format("%.2f", distance)} km")
                                
                                // Log suspicious activity but don't block posting
                                if (distance > 50.0) {
                                    Timber.w("🛡️ ANTI-FRAUD: ⚠️ Suspicious location detected (${String.format("%.2f", distance)} km away)")
                                    // TODO: Send to fraud detection system
                                }
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "🛡️ ANTI-FRAUD: Background location check failed")
                        }
                    }
                }
                
                // Post job immediately without waiting for location check
                pendingJobSubmission = false
                submitJobWithCoordinates(finalLatitude, finalLongitude)
            } catch (e: Exception) {
                Timber.e(e, "📝 JOB POSTING DEBUG: Error in submitJob")
                // SENIOR FIX: ViewModel state resets automatically on error; only reset UI flags
                isCheckingProfile = false
                Toast.makeText(context, "Error posting job: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Hiring Studio palette
    val primaryBlue = Color(0xFF2563EB)
    val successGreen = Color(0xFF059669)
    val accentOrange = Color(0xFFFF8A3D)
    val darkText = Color(0xFF0F172A)
    val pageBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFBF5),
            Color(0xFFF2F6FF),
            Color(0xFFF8FAFC)
        )
    )
    val basicsReady = title.isNotBlank() && description.isNotBlank()
    val compensationReady = payAmount.isNotBlank() && location.isNotBlank()
    val requirementsReady = contactNumber.isNotBlank()
    val locationPinned = hasValidJobCoordinates
    val hasHeroImage = jobImageUrl.isNotBlank() || jobImageUri != null
    val readinessCount = listOf(
        basicsReady,
        compensationReady,
        requirementsReady,
        locationPinned
    ).count { it }
    val missingStudioItems = buildList {
        if (!basicsReady) add("Add a clear title and description")
        if (!compensationReady) add("Set pay and work location")
        if (!requirementsReady) add("Add contact details")
        if (!locationPinned) add("Pin the exact job location")
    }
    val publishEnabled = !employerJobUiState.isCreatingJob &&
        !isSubmittingJob &&
        !isLoadingLocation &&
        basicsReady &&
        compensationReady &&
        requirementsReady &&
        locationPinned

    fun attemptPublishJob(allowOutOfRangePay: Boolean = false) {
        val scamCheck = JobValidationUtils.validateAgainstScamKeywords(title, description)
        scamValidationResult = scamCheck
        if (!scamCheck.isValid) {
            showScamWarningDialog = true
            return
        }

        val payCheck = JobValidationUtils.validatePayRate(category, payType, payAmount)
        payRateValidationResult = payCheck
        if (!payCheck.isValid && !allowOutOfRangePay) {
            showPayRateWarningDialog = true
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showLoginBottomSheet = true
        } else {
            submitJob(0.0, 0.0)
        }
    }
    
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
                    Text(stringResource(R.string.post_anyway))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showLocationWarningDialog = false
                        pendingJobSubmission = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // ANTI-FRAUD: Scam Keywords Warning Dialog (No-Data-Entry Firewall)
    if (showScamWarningDialog && scamValidationResult != null && !scamValidationResult!!.isValid) {
        AlertDialog(
            onDismissRequest = { showScamWarningDialog = false },
            icon = {
                Text("\uD83D\uDEAB", fontSize = 48.sp) // 🚫
            },
            title = {
                Text(
                    text = "Job Posting Blocked",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Column {
                    Text(
                        text = scamValidationResult!!.errorMessage ?: "This job posting contains suspicious content.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "\uD83D\uDEE1\uFE0F DutyPe is for local, in-person jobs only.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Work-from-home, online jobs, and data entry jobs are not allowed to protect workers from scams.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showScamWarningDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryBlue
                    )
                ) {
                    Text(stringResource(R.string.edit_job_details))
                }
            }
        )
    }
    
    // ANTI-FRAUD: Pay Rate Warning Dialog (Pay Rate Guardrails)
    if (showPayRateWarningDialog && payRateValidationResult != null && !payRateValidationResult!!.isValid) {
        AlertDialog(
            onDismissRequest = { showPayRateWarningDialog = false },
            icon = {
                Text(if (payRateValidationResult!!.isTooLow) "\uD83D\uDCB8" else "\uD83D\uDCB0", fontSize = 48.sp) // 💸 or 💰
            },
            title = {
                Text(
                    text = if (payRateValidationResult!!.isTooLow) "Pay Rate Too Low" else "Pay Rate Too High",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
            },
            text = {
                Column {
                    Text(
                        text = payRateValidationResult!!.errorMessage ?: "Pay rate is outside the expected range.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "\uD83D\uDCCA Market Rate for ${category.displayName}:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = getSuggestedPayRange(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You can still post this job, but workers may be skeptical of ${if (payRateValidationResult!!.isTooLow) "low" else "unusually high"} pay rates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showPayRateWarningDialog = false
                        attemptPublishJob(allowOutOfRangePay = true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B)
                    )
                ) {
                    Text(stringResource(R.string.continue_anyway))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPayRateWarningDialog = false }
                ) {
                    Text(stringResource(R.string.edit_pay_rate))
                }
            }
        )
    }
    
    // PROFILE INCOMPLETE DIALOG - Navigate to profile setup with prefilled data
    if (showProfileIncompleteDialog && profileCheckResult != null) {
        AlertDialog(
            onDismissRequest = { 
                showProfileIncompleteDialog = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Complete Your Profile",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "You need to complete your profile before posting jobs.",
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Profile: ${profileCheckResult!!.completionPercentage}% complete (need 80%)",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF9800),
                        textAlign = TextAlign.Center
                    )
                    if (profileCheckResult!!.missingFields.isNotEmpty()) {
                        Text(
                            "Missing: ${profileCheckResult!!.missingFields.take(3).joinToString(", ")}${if (profileCheckResult!!.missingFields.size > 3) "..." else ""}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProfileIncompleteDialog = false
                        val navToUse = rootNavController ?: navController
                        navToUse.navigate(
                            Routes.employerProfileSetupWithReturnRoute(Routes.EMPLOYER_POST_JOB)
                        ) {
                            launchSingleTop = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    )
                ) {
                    Text(stringResource(R.string.complete_profile_button))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showProfileIncompleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            PostJobLaunchBar(
                publishEnabled = publishEnabled,
                isPublishing = employerJobUiState.isCreatingJob || isSubmittingJob,
                onPublish = { attemptPublishJob() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
                .padding(paddingValues)
        ) {
            PostJobBackdropDecor(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
                val isOnline by connectivityViewModel.isOnline.collectAsState()
                com.example.dutype.components.OfflineBanner(isOffline = !isOnline)

                CommonHeader(
                    title = "Post a job",
                    navController = navController,
                    backgroundColor = Color.Transparent,
                    titleColor = Color(0xFF0F172A)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
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

                    item {
                        JobImageUploadSection(
                            selectedImageUri = jobImageUri,
                            isUploading = isUploadingJobImage,
                            onImageSelected = { uri ->
                                jobImageUri = uri
                                scope.launch {
                                    isUploadingJobImage = true
                                    try {
                                        val currentUser = FirebaseAuth.getInstance().currentUser
                                        if (currentUser != null) {
                                            val fileName = "job_image_${System.currentTimeMillis()}.jpg"
                                            val storagePath = "job_images/${currentUser.uid}/$fileName"

                                            Timber.d("📸 JOB IMAGE: Starting upload with compression...")

                                            val uploadResult = com.example.dutype.utils.ImageUploadUtils.uploadWithRetry(
                                                context = context,
                                                uri = uri,
                                                storagePath = storagePath
                                            )

                                            when (uploadResult) {
                                                is com.example.dutype.utils.ImageUploadUtils.UploadResult.Success -> {
                                                    jobImageUrl = uploadResult.downloadUrl
                                                    Timber.d("📸 JOB IMAGE: ✅ Upload successful!")
                                                    Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                                is com.example.dutype.utils.ImageUploadUtils.UploadResult.Failure -> {
                                                    Timber.e(uploadResult.exception, "📸 JOB IMAGE: ❌ Upload failed: ${uploadResult.error}")
                                                    Toast.makeText(context, "Failed to upload image: ${uploadResult.error}", Toast.LENGTH_SHORT).show()
                                                    jobImageUri = null
                                                    jobImageUrl = ""
                                                }
                                                else -> {
                                                }
                                            }
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

                    item {
                        EnhancedPaymentSection(
                            payAmount = payAmount,
                            onPayAmountChange = { payAmount = it },
                            payType = payType,
                            onPayTypeChange = { payType = it },
                            category = category,
                            suggestedRange = getSuggestedPayRange()
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
                                            val locationInfo = locationService.getHighAccuracyLocation(
                                                timeoutMs = 5000L,
                                                minAccuracyMeters = 10f
                                            )
                                            if (locationInfo != null) {
                                                location = locationInfo.getFullAddress()
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
                                locationLatitude = lat
                                locationLongitude = lon
                                Timber.d("📍 LOCATION SEARCH: Selected location - lat: $lat, lon: $lon")
                            },
                            savedLocations = savedWorkLocations
                        )
                    }

                    item {
                        VacanciesSection(
                            vacancies = vacancies,
                            onVacanciesChange = { vacancies = it }
                        )
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
                            genders = genders
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

                    item {
                        AdditionalHiringNotesSection(
                            requirements = requirements,
                            onRequirementsChange = { requirements = it },
                            benefits = benefits,
                            onBenefitsChange = { benefits = it }
                        )
                    }

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
                                            .background(Color(0xFFECFCCB), RoundedCornerShape(10.dp)),
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

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                }
            }
        }
    }
    
    // Guest Mode - Login Bottom Sheet for job posting with profile check
    com.example.dutype.components.LoginBottomSheet(
        isVisible = showLoginBottomSheet,
        onDismiss = { showLoginBottomSheet = false },
        onLoginSuccess = {
            showLoginBottomSheet = false
            // After successful login, submit the job (which will check profile completion)
            submitJob(0.0, 0.0)
        },
        onProfileSetupRequired = {
            showLoginBottomSheet = false
            val navToUse = rootNavController ?: navController
            navToUse.navigate(
                Routes.employerProfileSetupWithReturnRoute(Routes.EMPLOYER_POST_JOB)
            ) {
                launchSingleTop = true
            }
        },
        requiresProfileCheck = true, // Check profile completion for job posting
        role = com.example.dutype.models.UserRole.EMPLOYER,
        title = "Login to Post Job",
        subtitle = "Please login to publish your job posting and reach thousands of workers"
    )
}

@Composable
private fun PostJobBackdropDecor(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 12.dp)
                .size(220.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFC48A).copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(180.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF93C5FD).copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 24.dp)
                .size(200.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF86EFAC).copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun PostJobCommandHeader(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            onClick = { navController.popBackStack() },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 6.dp
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFF0F172A)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Post a Job",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
        }
    }
}

@Composable
private fun PostJobHeroCard(
    title: String,
    payAmount: String,
    payType: PayType,
    location: String,
    companyName: String,
    employerTrustTier: String,
    readinessCount: Int,
    workType: String,
    hasHeroImage: Boolean,
    locationPinned: Boolean
) {
    val readinessPercent = readinessCount * 25

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1D4ED8),
                            Color(0xFFFF8A3D)
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "Worker-facing preview",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$readinessPercent% ready",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = title.ifBlank { "Your role headline will appear here" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 36.sp
                )

                Text(
                    text = if (companyName.isBlank()) {
                        "Add company and role details to make the listing feel legitimate immediately."
                    } else {
                        "$companyName • ${employerTrustTier.replace('_', ' ')} employer"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    lineHeight = 22.sp
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroSignalPill(
                        title = "Pay",
                        value = if (payAmount.isBlank()) "Set salary" else "₹$payAmount ${payType.displayName}"
                    )
                    HeroSignalPill(
                        title = "Type",
                        value = workType.ifBlank { "Choose work type" }
                    )
                    HeroSignalPill(
                        title = "Area",
                        value = location.ifBlank { "Add work location" }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (hasHeroImage) "🖼️" else "📄", fontSize = 24.sp)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (locationPinned) "Listing confidence is strong" else "Listing still needs a verified pin",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (hasHeroImage) {
                                    "Image added. Your post will feel more real when workers browse the feed."
                                } else {
                                    "Add a photo or poster if you want the listing to stand out faster in the feed."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f),
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostJobSignalStrip(
    readinessCount: Int,
    employerTrustTier: String,
    hasValidJobCoordinates: Boolean,
    hasImage: Boolean
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StudioSignalCard(
            title = "Essentials",
            value = "$readinessCount / 4",
            caption = "Critical publish checks complete",
            tint = Color(0xFF2563EB)
        )
        StudioSignalCard(
            title = "Trust Tier",
            value = employerTrustTier.replace('_', ' '),
            caption = "Employer reputation visible to workers",
            tint = Color(0xFFFF8A3D)
        )
        StudioSignalCard(
            title = "Map Pin",
            value = if (hasValidJobCoordinates) "Verified" else "Missing",
            caption = if (hasValidJobCoordinates) "Exact work area captured" else "Set a precise local pin",
            tint = Color(0xFF059669)
        )
        StudioSignalCard(
            title = "Visual",
            value = if (hasImage) "Live" else "Optional",
            caption = "Job poster / image state",
            tint = Color(0xFF7C3AED)
        )
    }
}

@Composable
private fun PostJobChecklistPanel(
    missingStudioItems: List<String>,
    locationPinned: Boolean,
    hasHeroImage: Boolean,
    primaryColor: Color,
    accentColor: Color,
    successColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Launch Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (missingStudioItems.isEmpty()) {
                            "All critical details are in place. You can publish when you are ready."
                        } else {
                            "Finish the remaining essentials before launch."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        lineHeight = 19.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (missingStudioItems.isEmpty()) "✓" else "!", color = primaryColor, fontWeight = FontWeight.Black)
                }
            }

            if (missingStudioItems.isEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReadinessTag(
                        label = if (locationPinned) "Location verified" else "Location pending",
                        backgroundColor = successColor.copy(alpha = 0.12f),
                        contentColor = successColor
                    )
                    ReadinessTag(
                        label = if (hasHeroImage) "Image added" else "Image optional",
                        backgroundColor = accentColor.copy(alpha = 0.12f),
                        contentColor = accentColor
                    )
                }
            } else {
                missingStudioItems.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "•",
                            color = accentColor,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioSectionBanner(
    eyebrow: String,
    title: String,
    description: String,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.White.copy(alpha = 0.55f),
                    RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                lineHeight = 30.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun PostJobLaunchBar(
    publishEnabled: Boolean,
    isPublishing: Boolean,
    onPublish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onPublish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = publishEnabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1F2937),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                )
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.post_job),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadinessTag(
    label: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = backgroundColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StudioMetaChip(
    label: String,
    value: String,
    backgroundColor: Color,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HeroSignalPill(title: String, value: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.66f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun StudioSignalCard(
    title: String,
    value: String,
    caption: String,
    tint: Color
) {
    Surface(
        modifier = Modifier.width(168.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun AdditionalHiringNotesSection(
    requirements: String,
    onRequirementsChange: (String) -> Unit,
    benefits: String,
    onBenefitsChange: (String) -> Unit
) {
    PolishedCard(accentColor = Color(0xFF7C3AED)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
                    Text("✍", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Extra Requirements & Benefits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            OutlinedTextField(
                value = requirements,
                onValueChange = onRequirementsChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Extra Requirements") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C3AED),
                    focusedLabelColor = Color(0xFF7C3AED),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = Color(0xFF7C3AED),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            OutlinedTextField(
                value = benefits,
                onValueChange = onBenefitsChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Additional Benefits") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2563EB),
                    focusedLabelColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = Color(0xFF2563EB),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
        }
    }
}

@Composable
fun PolishedCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFF8A3D),
    showAccent: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.93f),
        shadowElevation = 10.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFF8FAFC)
                    )
                )
            )
        ) {
            if (showAccent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor,
                                    Color(0xFF2563EB),
                                    Color(0xFF10B981)
                                )
                            )
                        )
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
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
    var isOtherSelected by remember { mutableStateOf(false) }
    var customTitleError by remember { mutableStateOf<String?>(null) }
    
    // Predefined job titles - Clean names without slashes
    val predefinedJobTitles = listOf(
        "Cook" to "\uD83D\uDC68\u200D\uD83C\uDF73",
        "Chef" to "\uD83D\uDC69\u200D\uD83C\uDF73",
        "Maid" to "\uD83E\uDDF9",
        "House Cleaner" to "\uD83C\uDFE0",
        "Driver" to "\uD83D\uDE97",
        "Security Guard" to "\uD83D\uDEE1\uFE0F",
        "Delivery Executive" to "\uD83D\uDCE6",
        "Waiter" to "\uD83C\uDF7D\uFE0F",
        "Server" to "\uD83E\uDD35",
        "Helper" to "\uD83E\uDD1D",
        "Assistant" to "\uD83D\uDCBC",
        "Electrician" to "\u26A1",
        "Plumber" to "\uD83D\uDD27",
        "Painter" to "\uD83C\uDFA8",
        "Carpenter" to "\uD83E\uDE9A",
        "Gardener" to "\uD83C\uDF31",
        "Caretaker" to "\uD83D\uDC76",
        "Nanny" to "\uD83D\uDC69\u200D\uD83C\uDF7C",
        "Receptionist" to "\uD83D\uDCBC",
        "Cashier" to "\uD83D\uDCB5",
        "Packer" to "\uD83D\uDCE6",
        "Loader" to "\uD83D\uDCE6",
        "Office Boy" to "\uD83C\uDFE2",
        "Factory Worker" to "\uD83C\uDFED",
        "Construction Worker" to "\uD83D\uDC77",
        "Shop Assistant" to "\uD83D\uDED2",
        "Housekeeping Staff" to "\uD83C\uDFE0",
        "Kitchen Helper" to "\uD83C\uDF73",
        "Watchman" to "\uD83D\uDC41\uFE0F",
        "AC Technician" to "\u2744\uFE0F",
        "Tailor" to "\uD83E\uDDF5",
        "Other" to "\u2795"
    )
    
    // Only show the custom text field when the user explicitly selects "Other"
    LaunchedEffect(title) {
        isOtherSelected = title == "Other"
    }
    
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
                    Text("\uD83D\uDCDD", fontSize = 18.sp)
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
                    Text("\uD83D\uDEE1\uFE0F", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Only local, in-person jobs allowed. No online or WFH jobs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Dropdown for job title selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = if (isOtherSelected && customCategory.isNotBlank()) customCategory else title,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.select_job_title)) },
                    placeholder = { Text(stringResource(R.string.tap_to_select_job_title)) },
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
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    predefinedJobTitles.forEach { (jobTitle, icon) ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        jobTitle,
                                        fontWeight = if (title == jobTitle) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (jobTitle == "Other") primaryBlue else Color(0xFF1E293B)
                                    )
                                }
                            },
                            onClick = {
                                onTitleChange(jobTitle)
                                isOtherSelected = jobTitle == "Other"
                                if (jobTitle != "Other") {
                                    onCustomCategoryChange("")
                                    customTitleError = null
                                }
                                // Auto-select matching category
                                val matchingCategory = when (jobTitle) {
                                    "Cook", "Chef", "Kitchen Helper" -> JobCategory.COOK
                                    "Maid", "House Cleaner", "Housekeeping Staff" -> JobCategory.MAID
                                    "Driver" -> JobCategory.DRIVER
                                    "Security Guard", "Watchman" -> JobCategory.SECURITY
                                    "Delivery Executive" -> JobCategory.DELIVERY
                                    "Waiter", "Server" -> JobCategory.WAITER
                                    "Electrician" -> JobCategory.ELECTRICIAN
                                    "Plumber" -> JobCategory.PLUMBER
                                    "Painter" -> JobCategory.PAINTER
                                    "Carpenter" -> JobCategory.CARPENTER
                                    "Gardener" -> JobCategory.GARDENER
                                    "Caretaker", "Nanny" -> JobCategory.CARETAKER
                                    "Receptionist" -> JobCategory.RECEPTIONIST
                                    "Cashier" -> JobCategory.CASHIER
                                    "Packer", "Loader" -> JobCategory.PACKER
                                    "Other" -> JobCategory.OTHER
                                    else -> JobCategory.HELPER
                                }
                                onCategoryChange(matchingCategory)
                                expanded = false
                            },
                            modifier = Modifier.background(Color.White)
                        )
                    }
                }
            }
            
            // Custom job title input when "Other" is selected
            if (isOtherSelected || title == "Other") {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = customCategory,
                    onValueChange = { newValue ->
                        onCustomCategoryChange(newValue)
                        // Validate against scam keywords
                        val validation = JobValidationUtils.validateAgainstScamKeywords(newValue, "")
                        customTitleError = if (!validation.isValid) {
                            "This job title is not allowed. Only local, in-person jobs."
                        } else {
                            null
                        }
                    },
                    label = { Text(stringResource(R.string.enter_job_title)) },
                    placeholder = { Text(stringResource(R.string.job_title_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = customTitleError != null,
                    supportingText = if (customTitleError != null) {
                        { Text(customTitleError!!, color = Color(0xFFDC2626)) }
                    } else {
                        { Text(stringResource(R.string.job_title_hint), color = Color(0xFF6B7280)) }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (customTitleError != null) Color(0xFFDC2626) else primaryBlue,
                        focusedLabelColor = if (customTitleError != null) Color(0xFFDC2626) else primaryBlue,
                        unfocusedBorderColor = if (customTitleError != null) Color(0xFFDC2626) else Color(0xFFE2E8F0),
                        cursorColor = primaryBlue,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
            }
            
            // Show selected category badge
            if (title.isNotBlank() && title != "Other") {
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
            } else if (isOtherSelected && customCategory.isNotBlank() && customTitleError == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\u2705", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Custom: $customCategory",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF10B981)
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
                    Text("\u23F0", fontSize = 18.sp)
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
    onPayTypeChange: (PayType) -> Unit,
    category: JobCategory = JobCategory.OTHER,
    suggestedRange: String = ""
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
                    Text("\uD83D\uDCB0", fontSize = 18.sp)
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
            
            // ANTI-FRAUD: Show suggested market rate for the category
            if (suggestedRange.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFECFDF5),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\uD83D\uDCCA",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Market rate for ${category.displayName}:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = suggestedRange,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
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
                            // Allow flexible input: numbers, ranges (10000-15000), or text
                            onPayAmountChange(newValue)
                        },
                        label = { Text(stringResource(R.string.amount_rupees)) },
                        placeholder = { Text("e.g. 12000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = false, // Remove numeric validation error
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            focusedLabelColor = primaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            cursorColor = primaryBlue
                        )
                    )
                }
                
                PayTypeDropdown(
                    selectedType = payType,
                    onTypeSelected = onPayTypeChange,
                    modifier = Modifier.weight(1f)
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
    onLocationSelected: ((Double, Double) -> Unit)? = null,
    savedLocations: List<com.example.dutype.models.WorkLocation> = emptyList()
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
        if (location.length >= 3 && !isLoadingLocation && showSuggestions) {
            kotlinx.coroutines.delay(500) // Debounce
            isSearching = true
            
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
                }
            }

            // Saved work locations quick-pick
            if (savedLocations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    savedLocations.take(5).forEach { loc ->
                        FilterChip(
                            selected = location == loc.address,
                            onClick = {
                                showSuggestions = false
                                searchSuggestions = emptyList()
                                onLocationChange(loc.address)
                                onLocationSelected?.invoke(loc.latitude, loc.longitude)
                            },
                            label = { Text(loc.label.ifBlank { loc.address.take(25) }, maxLines = 1) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            OutlinedTextField(
                value = location,
                onValueChange = { 
                    onLocationChange(it)
                    showSuggestions = true
                },
                label = { Text(stringResource(R.string.search_work_location)) },
                placeholder = { Text(stringResource(R.string.location_search_placeholder)) },
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
                        onClick = {
                            showSuggestions = false
                            searchSuggestions = emptyList()
                            onLocationButtonClick()
                        },
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
                label = { Text(stringResource(R.string.nearby_landmark_optional)) },
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
    genders: List<String>
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

