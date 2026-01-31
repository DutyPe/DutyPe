package com.example.dutype.employer.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.data.JobDraftDataStore
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
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.JobPerk
import com.example.dutype.employer.models.JobUrgency
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
import com.example.dutype.employer.viewmodels.AIJobPostingViewModel
import com.example.dutype.location.LocationSuggestion
import com.example.dutype.models.JobListing
import com.example.dutype.utils.ImageUploadUtils
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import com.example.dutype.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.dutype.utils.JobValidationUtils
import com.example.dutype.utils.ValidationResult
import com.example.dutype.utils.PayRateValidationResult
import com.example.dutype.repositories.AIBackendRepository
import com.example.dutype.services.ai.JobPostingScreeningService
import com.example.dutype.services.ai.JobAnalysisRequest
import com.example.dutype.services.ai.EmployerHistory
import com.example.dutype.viewmodels.FirestoreJobViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID

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
    
    // PERFORMANCE FIX: Idempotency key to prevent duplicate job submissions
    // Generated once per form session, included in job data to detect duplicates
    val idempotencyKey = remember { UUID.randomUUID().toString() }
    
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
    
    // LazyList state for scrolling
    val listState = rememberLazyListState()
    
    // Scroll to top when step changes
    LaunchedEffect(currentStep) {
        listState.animateScrollToItem(0)
    }

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
                        val userDoc = db.collection("users").document(currentUser.uid).get().await()

                        if (userDoc.exists()) {
                            val savedCompanyName = userDoc.getString("companyName")
                            if (!savedCompanyName.isNullOrBlank()) {
                                companyName = savedCompanyName
                            }
                            val savedFullName = userDoc.getString("fullName")
                            if (!savedFullName.isNullOrBlank()) {
                                employerName = savedFullName
                            }
                            val savedContactPhone = userDoc.getString("contactPhone") ?: userDoc.getString("phoneNumber")
                            if (!savedContactPhone.isNullOrBlank()) {
                                contactNumber = savedContactPhone
                            }
                            val savedTrustTier = userDoc.getString("trustTier")
                            if (!savedTrustTier.isNullOrBlank()) {
                                employerTrustTier = savedTrustTier
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
            location = jobPosting.location,
            payAmount = "${jobPosting.payAmount}/${jobPosting.payType.name.lowercase()}",
            payType = jobPosting.payType.name.lowercase(),
            shiftTiming = jobPosting.shiftTiming.name,
            description = jobPosting.description,
            requirements = if (requirements.isNotBlank()) requirements.split(",").map { it.trim() } else emptyList(),
            benefits = if (benefits.isNotBlank()) benefits.split(",").map { it.trim() } else emptyList(),
            vacancies = jobPosting.vacancies,
            isActive = true,
            urgency = if (jobPosting.urgency == JobUrgency.URGENT) "URGENT" else "NORMAL",
            postedAt = System.currentTimeMillis(),
            contactNumber = jobPosting.contactNumber,
            jobType = "Part-time",
            ageRange = ageRange,
            gender = gender,
            applicationCount = 0L
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
        
        // Convert JobListing to Map for Firestore (only essential fields)
        val jobData = mapOf(
            // Core job information
            "title" to jobListing.title,
            "companyName" to jobListing.companyName,
            "employerName" to employerName,
            "category" to (if (category == JobCategory.OTHER && customCategory.isNotBlank()) customCategory else category.name),
            
            // Location information
            "location" to jobListing.location,
            "area" to area,
            "city" to city,
            "latitude" to finalLatitude,
            "longitude" to finalLongitude,
            "landmark" to landmark,
            
            // Pay information
            "payAmount" to jobListing.payAmount,
            "payType" to jobListing.payType,
            
            // Schedule and timing
            "shiftTiming" to jobListing.shiftTiming,
            
            // Job details
            "description" to jobListing.description,
            "benefits" to jobListing.benefits,
            "requirements" to jobListing.requirements,
            "perks" to selectedPerks.map { it.displayName },
            "vacancies" to jobListing.vacancies,
            "urgency" to jobListing.urgency,
            
            // Contact information
            "contactNumber" to jobListing.contactNumber,
            
            // Job metadata
            "isActive" to jobListing.isActive,
            "postedAt" to jobListing.postedAt,
            "createdAt" to jobListing.postedAt,
            "expiresAt" to (jobListing.postedAt + (jobListing.expiryDays * 24 * 60 * 60 * 1000L)),
            "expiryDays" to jobListing.expiryDays,
            
            // Job type and requirements
            "jobType" to jobListing.jobType,
            "ageRange" to jobListing.ageRange,
            "gender" to jobListing.gender,
            
            // Counters
            "applicationCount" to jobListing.applicationCount,
            
            // Images
            // REMOVED: employerTrustTier - no longer stored in job, should be fetched from employer profile
            "jobImageUrl" to jobImageUrl,
            
            // System fields
            "idempotencyKey" to idempotencyKey
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
        
        // Set local guard immediately
        isSubmittingJob = true
        isCheckingProfile = true
        
        // STEP 2: Check profile completion before allowing job posting
        scope.launch {
            try {
                val checkResult = profileCompletionService.preJobPostCheck(currentUser.uid)
                profileCheckResult = checkResult
                isCheckingProfile = false
                
                if (!checkResult.canPost) {
                    Timber.w("⚠️ PROFILE CHECK: Employer cannot post jobs - ${checkResult.completionPercentage}% complete")
                    isSubmittingJob = false
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
                        isSubmittingJob = false
                        // Navigate to employer profile setup instead of showing toast
                        navController.navigate(
                            Routes.employerProfileSetupWithReturnRoute(Routes.EMPLOYER_POST_JOB)
                        )
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
                        Timber.w("📝 JOB POSTING DEBUG: Geocoding failed, using 0,0 coordinates")
                    }
                }
                
                // ANTI-FRAUD: Location Consistency Check
                if (finalLatitude != 0.0 && finalLongitude != 0.0) {
                    try {
                        val employerLocation = locationService.getHighAccuracyLocation(
                            timeoutMs = 10000L,
                            minAccuracyMeters = 50f
                        )
                        
                        if (employerLocation != null) {
                            employerCurrentLatitude = employerLocation.latitude
                            employerCurrentLongitude = employerLocation.longitude
                            
                            val distance = locationService.calculateDistance(
                                employerCurrentLatitude, employerCurrentLongitude,
                                finalLatitude, finalLongitude
                            )
                            locationDistanceKm = distance
                            
                            Timber.d("🛡️ ANTI-FRAUD: Location consistency check - Distance: ${String.format("%.2f", distance)} km")
                            
                            if (distance > 30.0 && !pendingJobSubmission) {
                                Timber.w("🛡️ ANTI-FRAUD: ⚠️ Location mismatch detected!")
                                isSubmittingJob = false
                                showLocationWarningDialog = true
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "🛡️ ANTI-FRAUD: Could not verify employer location, proceeding anyway")
                    }
                }
                
                pendingJobSubmission = false
                submitJobWithCoordinates(finalLatitude, finalLongitude)
            } catch (e: Exception) {
                Timber.e(e, "📝 JOB POSTING DEBUG: Error in submitJob")
                isSubmittingJob = false
                isCheckingProfile = false
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
                    Text("Edit Job Details")
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
                        // Allow proceeding to next step
                        if (currentStep < 4) currentStep++
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B)
                    )
                ) {
                    Text("Continue Anyway")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPayRateWarningDialog = false }
                ) {
                    Text("Edit Pay Rate")
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
                        // Navigate to employer profile setup with return route
                        // Use proper helper function to encode the return route
                        navController.navigate(Routes.employerProfileSetupWithReturnRoute(Routes.EMPLOYER_POST_JOB))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    )
                ) {
                    Text("Complete Profile")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { 
                        showProfileIncompleteDialog = false
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
                                onClick = { 
                                    // ANTI-FRAUD: Check validations before proceeding
                                    when (currentStep) {
                                        1 -> {
                                            // Check for scam keywords
                                            val scamCheck = JobValidationUtils.validateAgainstScamKeywords(title, description)
                                            scamValidationResult = scamCheck
                                            if (!scamCheck.isValid) {
                                                showScamWarningDialog = true
                                            } else {
                                                currentStep++
                                            }
                                        }
                                        2 -> {
                                            // Check pay rate guardrails
                                            val payCheck = JobValidationUtils.validatePayRate(category, payType, payAmount)
                                            payRateValidationResult = payCheck
                                            if (!payCheck.isValid) {
                                                showPayRateWarningDialog = true
                                            } else {
                                                currentStep++
                                            }
                                        }
                                        else -> currentStep++
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                enabled = when (currentStep) {
                                    1 -> title.isNotBlank() && description.isNotBlank()
                                    2 -> payAmount.isNotBlank() && location.isNotBlank()
                                    3 -> contactNumber.isNotBlank()
                                    else -> true
                                },
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
                                    // Guest mode check - require login at final submit
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    if (currentUser == null) {
                                        showLoginBottomSheet = true
                                    } else {
                                        val finalLatitude = 0.0
                                        val finalLongitude = 0.0
                                        submitJob(finalLatitude, finalLongitude)
                                    }
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
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(paddingValues)
        ) {
            // Offline banner at the very top
            val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
            val isOnline by connectivityViewModel.isOnline.collectAsState()
            com.example.dutype.components.OfflineBanner(isOffline = !isOnline)
            
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
                                    // Upload compressed image to Firebase Storage
                                    scope.launch {
                                        isUploadingJobImage = true
                                        try {
                                            val currentUser = FirebaseAuth.getInstance().currentUser
                                            if (currentUser != null) {
                                                // PERFORMANCE FIX: Compress image before upload (~60-80% size reduction)
                                                val compressedBytes = compressImage(context, uri)
                                                
                                                if (compressedBytes != null) {
                                                    val fileName = "job_image_${System.currentTimeMillis()}.jpg"
                                                    val storagePath = "job_images/${currentUser.uid}/$fileName"
                                                    val storageRef = storage.reference.child(storagePath)
                                                    
                                                    Timber.d("📸 JOB IMAGE: Uploading compressed image (${compressedBytes.size / 1024}KB) to path: $storagePath")
                                                    
                                                    // Upload compressed bytes instead of original file
                                                    storageRef.putBytes(compressedBytes).await()
                                                    val downloadUrl = storageRef.downloadUrl.await()
                                                    jobImageUrl = downloadUrl.toString()
                                                    Timber.d("📸 JOB IMAGE: ✅ Upload successful! URL: $jobImageUrl")
                                                    Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    // Fallback to original upload if compression fails
                                                    Timber.w("📸 JOB IMAGE: Compression failed, uploading original")
                                                    val fileName = "job_image_${System.currentTimeMillis()}.jpg"
                                                    val storagePath = "job_images/${currentUser.uid}/$fileName"
                                                    val storageRef = storage.reference.child(storagePath)
                                                    
                                                    storageRef.putFile(uri).await()
                                                    val downloadUrl = storageRef.downloadUrl.await()
                                                    jobImageUrl = downloadUrl.toString()
                                                    Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
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
                    }

                    2 -> {
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
            // Profile incomplete - navigate to profile setup with return route
            showLoginBottomSheet = false
            navController.navigate(Routes.employerProfileSetupWithReturnRoute(Routes.EMPLOYER_POST_JOB))
        },
        requiresProfileCheck = true, // Check profile completion for job posting
        role = com.example.dutype.models.UserRole.EMPLOYER,
        title = "Login to Post Job",
        subtitle = "Please login to publish your job posting and reach thousands of workers"
    )
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
    
    // Update isOtherSelected when title changes
    LaunchedEffect(title) {
        isOtherSelected = title == "Other" || !predefinedJobTitles.any { it.first == title }
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
                    label = { Text("Enter Job Title") },
                    placeholder = { Text("e.g., Barista, Salon Assistant") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = customTitleError != null,
                    supportingText = if (customTitleError != null) {
                        { Text(customTitleError!!, color = Color(0xFFDC2626)) }
                    } else {
                        { Text("Enter a specific job title for your local business", color = Color(0xFF6B7280)) }
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
                    text = "\uD83D\uDCA1",
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

