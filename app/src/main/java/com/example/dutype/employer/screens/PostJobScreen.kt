package com.example.dutype.employer.screens

import android.Manifest
import android.R.attr.category
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Maps free-form job titles (typed by the employer) to a likely [JobCategory]
 * using simple keyword matching. Returns null when nothing recognisable is in
 * the title so we don't silently overwrite the employer's pick.
 *
 * Order matters â€” more specific keywords come first.
 */
private fun inferCategoryFromTitle(title: String): JobCategory? {
    val t = title.lowercase().trim()
    if (t.length < 3) return null
    val rules: List<Pair<List<String>, JobCategory>> = listOf(
        listOf("delivery", "courier", "rider", "swiggy", "zomato", "dunzo", "parcel") to JobCategory.DELIVERY,
        listOf("driver", "chauffeur", "uber", "ola", "cab", "taxi", "truck") to JobCategory.DRIVER,
        listOf("cook", "chef", "kitchen", "tandoor", "biryani") to JobCategory.COOK,
        listOf("waiter", "server", "steward") to JobCategory.WAITER,
        listOf("maid", "house help", "housekeep", "babysit", "nanny", "ayah") to JobCategory.MAID,
        listOf("security", "guard", "watchman", "bouncer") to JobCategory.SECURITY,
        listOf("electrician", "wiring", "electrical") to JobCategory.ELECTRICIAN,
        listOf("plumber", "plumbing", "pipe") to JobCategory.PLUMBER,
        listOf("painter", "painting") to JobCategory.PAINTER,
        listOf("carpenter", "woodwork") to JobCategory.CARPENTER,
        listOf("gardener", "garden", "landscap", "horticult") to JobCategory.GARDENER,
        listOf("caretaker", "care taker", "caregiver") to JobCategory.CARETAKER,
        listOf("receptionist", "front desk") to JobCategory.RECEPTIONIST,
        listOf("cashier", "billing") to JobCategory.CASHIER,
        listOf("packer", "packing", "loader") to JobCategory.PACKER,
        listOf("cleaner", "cleaning", "janitor", "sweeper") to JobCategory.MAID,
        listOf("helper", "assistant", "labour", "labor") to JobCategory.HELPER
    )
    for ((words, category) in rules) {
        if (words.any { t.contains(it) }) return category
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    navController: NavController,
    rootNavController: NavController? = null,
    employerId: String? = null,
    onJobPosted: ((String?) -> Unit)? = null,
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
    val locationRepository = remember { com.example.dutype.di.locationRepositoryFromHilt(context) }
    val employerJobViewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    
    // Saved work locations quick-pick (process-scoped, in-memory)
    val savedWorkLocationsStore = jobViewModel.savedWorkLocationsStore
    val savedWorkLocations by savedWorkLocationsStore.locations.collectAsState()
    
    // Get InAppReviewTriggerService from Hilt
    val reviewTriggerService = com.example.dutype.di.rememberInAppReviewTriggerService()
    
    val employerJobUiState by employerJobViewModel.uiState.collectAsState()
    
    // Profile Completion Service for pre-check
    val profileCompletionService = jobViewModel.profileCompletionService
    
    // PROFILE COMPLETION CHECK STATE - Check only when submitting, not on screen load
    var isCheckingProfile by remember { mutableStateOf(false) }
    var profileCheckResult by remember { mutableStateOf<com.example.dutype.services.ProfileCompletionService.PreJobPostCheckResult?>(null) }
    var showProfileIncompleteDialog by remember { mutableStateOf(false) }
    var pendingJobSubmitAfterProfileCheck by remember { mutableStateOf(false) }

    // Form state matching JobPostingModel
    var title by remember { mutableStateOf("") }
    var payAmount by remember { mutableStateOf("") }
    // Batch-p #11.1: default pay type is daily (matches Indian gig market
    // mental model better than HOURLY for the typical job categories on
    // DutyPe — cook, maid, helper, driver, delivery).
    var payType by remember { mutableStateOf(PayType.DAILY) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(JobCategory.COOK) }
    var customCategory by remember { mutableStateOf("") }
    // Track whether the employer hand-picked a category. Auto-detection from
    // the title only runs while this stays false so we never clobber an
    // explicit choice.
    var categoryManuallySet by remember { mutableStateOf(false) }
    var shiftTiming by remember { mutableStateOf(ShiftTiming.FLEXIBLE) }
    // Batch-p #3: when shiftTiming == CUSTOM the employer types their own
    // start/end timing into these two fields; the merged display string is
    // persisted as the job's shiftTiming value.
    var customShiftStart by remember { mutableStateOf("") }
    var customShiftEnd by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf(JobUrgency.NORMAL) }
    var vacancies by remember { mutableStateOf("") }
    var employerName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    
    // Enhanced fields for hyper-local jobs
    var selectedPerks by remember { mutableStateOf(setOf<JobPerk>()) }
    // Custom perks the employer has typed manually (e.g. "Gym membership").
    // These are merged with selectedPerks.displayName when persisting.
    var customPerks by remember { mutableStateOf(listOf<String>()) }
    var workType by remember { mutableStateOf("Part-time") }
    var experienceLevel by remember { mutableStateOf("No Experience Required") }
    var ageRange by remember { mutableStateOf("18-30") }
    var gender by remember { mutableStateOf("Both") }
    
    // Employer Trust Tier (loaded from profile)
    var employerTrustTier by remember { mutableStateOf("VERIFIED") }
    
    // JOB IMAGE: Optional image upload for job posting
    var jobImageUri by remember { mutableStateOf<Uri?>(null) }
    var jobImageUrl by remember { mutableStateOf("") }
    var isUploadingJobImage by remember { mutableStateOf(false) }
    val storage = remember { FirebaseStorage.getInstance() }
    
    // Quick selection options for hyper-local jobs
    val workTypes = listOf("Part-time", "Full-time", "Contract", "Temporary", "Weekend Only", "Student-friendly")
    val baseExperienceLevels = listOf(
        "No Experience Required",
        "Fresher (Educated)",
        "1-2 years",
        "2-5 years",
        "5+ years"
    )
    val experienceLevels = remember(experienceLevel) {
        if (experienceLevel.isNotBlank() && experienceLevel !in baseExperienceLevels) {
            baseExperienceLevels + experienceLevel
        } else {
            baseExperienceLevels
        }
    }
    val ageRanges = listOf("18-30", "30-45", "Any age")
    val genders = listOf("Male", "Female", "Both")

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

    // Auto-detect the job category from a free-form title (e.g. "Need a
    // delivery boy in Madhapur" -> DELIVERY). Skipped once the employer has
    // explicitly chosen a category from the dropdown.
    LaunchedEffect(title, categoryManuallySet) {
        if (categoryManuallySet) return@LaunchedEffect
        val inferred = inferCategoryFromTitle(title)
        if (inferred != null && inferred != category) {
            category = inferred
        }
    }

    // P2 FIX: Auto-save draft on field changes (debounced 2 seconds)
    val draftTrigger = remember { MutableStateFlow(0L) }

    @OptIn(FlowPreview::class)
    LaunchedEffect(Unit) {
        draftTrigger
            .debounce(2000L) // 2 second debounce
            .collect {
                val hasDraftContent =
                    title.isNotBlank() ||
                    description.isNotBlank() ||
                    payAmount.isNotBlank() ||
                    location.isNotBlank() ||
                    vacancies.isNotBlank() ||
                    contactNumber.isNotBlank() ||
                    customCategory.isNotBlank() ||
                    selectedPerks.isNotEmpty() ||
                    customPerks.isNotEmpty() ||
                    experienceLevel != "No Experience Required" ||
                    ageRange != "18-30" ||
                    gender != "Both" ||
                    shiftTiming != ShiftTiming.FLEXIBLE ||
                    urgency != JobUrgency.NORMAL ||
                    workType != "Part-time"

                if (hasDraftContent) {
                    val draft = JobDraftDataStore.JobDraft(
                        title = title,
                        description = description,
                        payAmount = payAmount,
                        payType = payType,
                        location = location,
                        locationLatitude = locationLatitude,
                        locationLongitude = locationLongitude,
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
                        requirements = "",
                        benefits = ""
                    )
                    employerJobViewModel.saveDraft(draft)
                    Timber.d(" AUTO-SAVE: Draft saved")
                }
            }
    }

    // Trigger auto-save when form fields change
    LaunchedEffect(
        title,
        description,
        payAmount,
        payType,
        location,
        category,
        customCategory,
        vacancies,
        contactNumber,
        shiftTiming,
        urgency,
        selectedPerks,
        customPerks,
        workType,
        experienceLevel,
        ageRange,
        gender
    ) {
        draftTrigger.value = System.currentTimeMillis()
    }

    suspend fun fetchWorkLocationFast() {
        val cachedLocation = locationRepository.lastKnownLocationIfFresh(10 * 60 * 1000L)
        if (cachedLocation != null) {
            location = cachedLocation.getFullAddress()
            locationLatitude = cachedLocation.latitude
            locationLongitude = cachedLocation.longitude
            Timber.d(" LOCATION DEBUG: Using recent cached location immediately")
        }

        val refinedLocation = locationRepository.getHighAccuracy(
            timeoutMs = if (cachedLocation != null) 4000L else 6000L,
            minAccuracyMeters = 35f
        )

        if (refinedLocation != null) {
            location = refinedLocation.getFullAddress()
            locationLatitude = refinedLocation.latitude
            locationLongitude = refinedLocation.longitude
            Timber.d(" LOCATION DEBUG: âœ… Refined location fetched (${refinedLocation.accuracy}m)")
        } else if (cachedLocation == null) {
            Timber.w(" LOCATION DEBUG: No cached or refined location available")
            locationError = "Unable to get current location"
        } else {
            Timber.w(" LOCATION DEBUG: Refinement timed out, keeping cached location")
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.d(" LOCATION DEBUG: Permission result - isGranted: $isGranted")
        if (isGranted) {
            isLoadingLocation = true
            locationError = null
            scope.launch {
                try {
                    Timber.d(" LOCATION DEBUG: Fetching fast-first location...")
                    fetchWorkLocationFast()
                } catch (e: Exception) {
                    Timber.e(e, " LOCATION DEBUG: Error getting location")
                    locationError = "Error getting location: ${e.message}"
                } finally {
                    isLoadingLocation = false
                }
            }
        } else {
            Timber.w(" LOCATION DEBUG: Permission denied")
            locationError = "Location permission denied"
        }
    }

    // P1 FIX: Load employer profile data from CACHE (5-minute TTL)
    // P2 FIX: Restore draft if available
    // NOTE: Profile check moved to job submission time - allows non-logged-in users to fill form
    // Bug #4 fix: The draft restore branch used to live inside the
    // `if (currentUser != null)` block, so a guest who saved a draft and then
    // signed in would lose it on the FIRST authenticated composition. Draft
    // restore now runs unconditionally on entry and re-runs whenever the auth
    // state flips so the GUEST bucket is migrated transparently.
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val savedDraft = employerJobViewModel.getSavedDraft()
                if (savedDraft != null && savedDraft.hasContent()) {
                    Timber.d(" Restoring job draft (device-scoped)...")
                    title = savedDraft.title
                    description = savedDraft.description
                    payAmount = savedDraft.payAmount
                    payType = savedDraft.payType
                    location = savedDraft.location
                    if (savedDraft.locationLatitude != 0.0 || savedDraft.locationLongitude != 0.0) {
                        locationLatitude = savedDraft.locationLatitude
                        locationLongitude = savedDraft.locationLongitude
                    }
                    category = savedDraft.category
                    customCategory = savedDraft.customCategory
                    vacancies = savedDraft.vacancies
                    if (contactNumber.isBlank()) {
                        contactNumber = savedDraft.contactNumber
                    }
                    shiftTiming = savedDraft.shiftTiming
                    urgency = savedDraft.urgency
                    selectedPerks = savedDraft.perks
                    workType = savedDraft.workType
                    experienceLevel = savedDraft.experienceLevel
                    ageRange = savedDraft.ageRange
                    gender = if (savedDraft.gender.equals("Any", ignoreCase = true)) {
                        "Both"
                    } else {
                        savedDraft.gender
                    }
                    Timber.d("âœ… Draft restored successfully")
                }
            } catch (e: Exception) {
                Timber.e(e, "âŒ Error restoring job draft")
            }
        }
    }

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
                            Timber.d("âœ… Company name loaded from CACHE: $companyName")
                        }
                        if (cachedProfile.employerName.isNotBlank()) {
                            employerName = cachedProfile.employerName
                        }
                        if (cachedProfile.contactPhone.isNotBlank()) {
                            contactNumber = cachedProfile.contactPhone
                            Timber.d("âœ… Contact number loaded from CACHE: $contactNumber")
                        }
                        if (cachedProfile.trustTier.isNotBlank()) {
                            employerTrustTier = cachedProfile.trustTier
                            Timber.d("âœ… Trust tier loaded from CACHE: $employerTrustTier")
                        }
                    } else {
                        // Fallback to direct Firestore fetch (cache miss)
                        Timber.d(" Cache miss, fetching from Firestore...")
                        val db = com.example.dutype.di.firestoreFromHilt(context)
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
                } catch (e: Exception) {
                    Timber.e(e, "âŒ Error loading employer profile")
                }
            }
        }
    }
    
    // Clear legacy redirect flag from the old multi-step flow
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("dutype_prefs", android.content.Context.MODE_PRIVATE)
        val shouldJumpToLastStep = prefs.getBoolean("jump_to_post_job_last_step", false)
        if (shouldJumpToLastStep) {
            Timber.d(" Clearing legacy post-job step redirect flag")
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
            Timber.w(" JOB POSTING DEBUG: Already creating job in submitJobWithCoordinates, ignoring")
            return
        }
        
        Timber.d(" JOB POSTING DEBUG: Creating job posting...")
        val jobPosting = createJobPosting()
        val normalizedJobType = if (category == JobCategory.OTHER && customCategory.isNotBlank()) {
            customCategory.trim()
        } else {
            category.displayName
        }
        val normalizedUrgency = when (urgency) {
            JobUrgency.IMMEDIATE, JobUrgency.URGENT -> "HIGH"
            JobUrgency.NORMAL -> "MEDIUM"
            JobUrgency.WITHIN_MONTH -> "WITHIN_MONTH"
        }
        val normalizedBenefits = (selectedPerks.map { it.displayName } + customPerks).distinct()

        // BUG #6 FIX: payAmount is a free-form field (the form/help text says
        // "Enter amount, range (10000-15000), or text (Based on experience)").
        // The previous `toDoubleOrNull() ?: 0.0` collapsed every non-numeric
        // input to 0.0, which the strict Firestore schema (`salary > 0`) then
        // silently rejected so the post never went through. The parser keeps
        // a positive numeric for filtering and surfaces the original text in
        // the description so workers still see "Pay: 15000-20000" or
        // "Pay: Negotiable".
        // Bug #6 fix: salary is a String â€” the employer's exact text
        // ("Negotiable" / "1000-2000" / "2000+" / "5000") is sent
        // verbatim to Firestore. The card layer formats for display via
        // SalaryFormatter; numeric filters parse the lower bound.
        val payParsed = com.example.dutype.utils.PayAmountParser.parse(jobPosting.payAmount)
        val descriptionWithPayText = jobPosting.description

        // Build job data map directly from jobPosting (no intermediate JobListing needed)
        val jobData = mapOf(
            // Core job information
            "title" to jobPosting.title,
            "jobType" to normalizedJobType,
            
            // Location information
            "location" to mapOf("lat" to finalLatitude, "lng" to finalLongitude),
            "addressText" to jobPosting.location,
            
            // Pay information
            "salary" to payParsed.text,
            "salaryType" to jobPosting.payType.name,
            
            // Job details
            "description" to descriptionWithPayText,
            "gender" to gender,
            "experienceRequired" to experienceLevel,
            "shiftTiming" to (
                // Batch-p #3: persist the typed-in start/end timing for CUSTOM shifts.
                if (shiftTiming == ShiftTiming.CUSTOM &&
                    (customShiftStart.isNotBlank() || customShiftEnd.isNotBlank())
                ) {
                    listOf(customShiftStart.trim(), customShiftEnd.trim())
                        .filter { it.isNotBlank() }
                        .joinToString(separator = " - ")
                } else {
                    shiftTiming.displayName
                }
            ),
            "vacancies" to (vacancies.toIntOrNull() ?: 1),
            "benefits" to normalizedBenefits,
            
            // Contact information
            "contactNumber" to jobPosting.contactNumber,
            "workingHours" to workType,
            
            // Job metadata â€” createdAt/expiresAt set by JobFirestoreService.createJob()
            "urgency" to normalizedUrgency,
            
            // System fields
            "employerId" to (employerId ?: ""),

            // #5 fix: optional hero image URL persisted on jobmetadata so
            // it shows on worker / employer job cards instead of an emoji.
            "jobImageUrl" to jobImageUrl
        )
        
        // DEBUG: Log all job data being sent to Firestore
        Timber.d(" JOB POSTING DEBUG: Job Data to be saved:")
        jobData.forEach { (key, value) ->
            Timber.d("   - $key: $value")
        }
        
        employerJobViewModel.createJob(jobData as Map<String, Any>) { success, newJobId, message ->
            // Reset local guard
            isSubmittingJob = false
            
            if (success) {
                Timber.i(" JOB POSTING DEBUG: âœ… Job posted successfully!")
                Toast.makeText(context, context.getString(R.string.post_job_success), Toast.LENGTH_SHORT).show()
                
                // Trigger in-app review after successful job posting
                context.findActivity()?.let { activity ->
                    reviewTriggerService.onEmployerJobPosted(activity)
                }
                
                // Call the callback if provided (for tabbed interface)
                onJobPosted?.invoke(newJobId)
                // Navigate to employer home screen to show the posted job
                if (onJobPosted == null) {
                    if (!newJobId.isNullOrBlank()) {
                        navController.navigate(Routes.employerJobPreviewRoute(newJobId)) {
                            popUpTo(Routes.EMPLOYER_HOME) { inclusive = false }
                        }
                    } else {
                        navController.navigate(Routes.EMPLOYER_HOME) {
                            popUpTo(Routes.EMPLOYER_HOME) { inclusive = false }
                        }
                    }
                }
            } else {
                Timber.e(" JOB POSTING DEBUG: âŒ Job posting failed: $message")
                Toast.makeText(context, context.getString(R.string.post_job_error, message), Toast.LENGTH_LONG).show()
            }
        }
    }

    // Submit job function - handles login check, profile check, and geocoding
    fun submitJob(finalLatitude1: Double, finalLongitude1: Double) {
        Timber.d(" JOB POSTING DEBUG: submitJob() called")
        
        // STEP 1: Check if user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Timber.d(" JOB POSTING DEBUG: User not logged in, showing login sheet")
            showLoginBottomSheet = true
            return
        }
        
        // SENIOR FIX: Use ONLY ViewModel state as single source of truth
        // Prevents race condition from dual guards desynchronizing
        // Check and set atomically (in practice, Compose state updates are on main thread)
        if (employerJobUiState.isCreatingJob) {
            Timber.w(" JOB POSTING DEBUG: Already creating job (ViewModel guard), ignoring duplicate call")
            return
        }
        
        if (!validateStep(4)) {
            Timber.w(" JOB POSTING DEBUG: Step 4 validation failed")
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
                    Timber.d("âœ… PROFILE CHECK: Using cached result - ${profileCheckResult!!.completionPercentage}% complete")
                    profileCheckResult!!
                } else {
                    val result = profileCompletionService.preJobPostCheck(currentUser.uid)
                    profileCheckResult = result
                    result
                }
                
                isCheckingProfile = false
                
                if (!checkResult.canPost) {
                    Timber.w("âš ï¸ PROFILE CHECK: Employer cannot post jobs - ${checkResult.completionPercentage}% complete")
                    // SENIOR FIX: ViewModel state resets automatically; no need for local flag
                    showProfileIncompleteDialog = true
                    return@launch
                }
                
                Timber.d("âœ… PROFILE CHECK: Employer can post jobs - ${checkResult.completionPercentage}% complete")
                
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
                        Timber.w(" JOB POSTING DEBUG: Company name is blank - redirecting to profile")
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
                    Timber.d(" JOB POSTING DEBUG: Geocoding manual location: $location")
                    val geocodedLocation = locationService.getCoordinatesFromAddress(location)
                    if (geocodedLocation != null) {
                        finalLatitude = geocodedLocation.latitude
                        finalLongitude = geocodedLocation.longitude
                        Timber.d(" JOB POSTING DEBUG: Geocoded - lat: $finalLatitude, lon: $finalLongitude")
                    } else {
                        Timber.w(" JOB POSTING DEBUG: Geocoding failed")
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
                            val employerLocation = locationRepository.getHighAccuracy(
                                timeoutMs = 5000L,  // Reduced timeout
                                minAccuracyMeters = 100f  // Less strict accuracy
                            )
                            
                            if (employerLocation != null) {
                                val distance = locationService.calculateDistance(
                                    employerLocation.latitude, employerLocation.longitude,
                                    finalLatitude, finalLongitude
                                )
                                
                                Timber.d("¸ ANTI-FRAUD: Location consistency check - Distance: ${String.format("%.2f", distance)} km")
                                
                                // Log suspicious activity but don't block posting
                                if (distance > 50.0) {
                                    Timber.w("¸ ANTI-FRAUD: âš ï¸ Suspicious location detected (${String.format("%.2f", distance)} km away)")
                                    // TODO: Send to fraud detection system
                                }
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "¸ ANTI-FRAUD: Background location check failed")
                        }
                    }
                }
                
                // Post job immediately without waiting for location check
                pendingJobSubmission = false
                submitJobWithCoordinates(finalLatitude, finalLongitude)
            } catch (e: Exception) {
                Timber.e(e, " JOB POSTING DEBUG: Error in submitJob")
                // SENIOR FIX: ViewModel state resets automatically on error; only reset UI flags
                isCheckingProfile = false
                Toast.makeText(context, context.getString(R.string.post_job_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Hiring Studio palette
    val primaryBlue = Color(0xFF2563EB)
    val successGreen = Color(0xFF059669)
    val accentOrange = Color(0xFFFF8A3D)
    val darkText = Color(0xFF0F172A)
    // Full white canvas for all post-job steps.
    val pageBackground = Color.White
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
        if (!basicsReady) add(stringResource(R.string.post_job_checklist_title_desc))
        if (!compensationReady) add(stringResource(R.string.post_job_checklist_pay_location))
        if (!requirementsReady) add(stringResource(R.string.post_job_checklist_contact))
        if (!locationPinned) add(stringResource(R.string.post_job_checklist_pin_location))
    }
    val publishEnabled = !employerJobUiState.isCreatingJob &&
        !isSubmittingJob &&
        !isLoadingLocation &&
        basicsReady &&
        compensationReady &&
        requirementsReady &&
        locationPinned

    // Apr 2026: Apna-style 3-step wizard. Steps:
    //   0 → Job details   (title, work type, description, image)
    //   1 → Pay & where   (compensation + location)
    //   2 → Requirements & contact (people, schedule, perks, contact, review)
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 3
    // Per-step Next gate. Mirrors the publish checklist but scoped to
    // the fields that live on each step so the user isn't stuck on a
    // later step because of an earlier issue.
    val canAdvanceFromStep: (Int) -> Boolean = { step ->
        when (step) {
            0 -> basicsReady
            1 -> compensationReady && locationPinned
            else -> true
        }
    }

    fun attemptPublishJob() {
        val scamCheck = JobValidationUtils.validateAgainstScamKeywords(title, description)
        scamValidationResult = scamCheck
        if (!scamCheck.isValid) {
            showScamWarningDialog = true
            return
        }

        // Batch-p #11: market-rate / pay-out-of-range warning dialog has been
        // removed. Employers can publish at any pay rate without being told it
        // looks unusual.

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
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.post_job_location_mismatch_title),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.post_job_location_mismatch_desc, String.format("%.1f", locationDistanceKm)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2)
                    ) {
                        Text(
                            text = stringResource(R.string.post_job_location_mismatch_info),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF991B1B)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.post_job_location_confirm),
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
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.post_job_blocked_title),
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
                                text = stringResource(R.string.post_job_blocked_info),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.post_job_blocked_desc),
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
    
    // Batch-p #11: market-rate / pay-rate guardrail dialog has been removed.

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
                    stringResource(R.string.post_job_complete_profile_title),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.post_job_complete_profile_desc),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.post_job_profile_progress, profileCheckResult!!.completionPercentage),
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF9800),
                        textAlign = TextAlign.Center
                    )
                    if (profileCheckResult!!.missingFields.isNotEmpty()) {
                        Text(
                            stringResource(R.string.post_job_missing_fields, profileCheckResult!!.missingFields.take(3).joinToString(", ") + if (profileCheckResult!!.missingFields.size > 3) "..." else ""),
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
            // Apr 2026: stepper restored. Bottom bar morphs based on step:
            // step 0   → [Next]
            // step 1   → [Back] [Next]
            // step 2   → [Back] [Post job]
            PostJobStepNavBar(
                currentStep = currentStep,
                totalSteps = totalSteps,
                canAdvance = canAdvanceFromStep(currentStep),
                publishEnabled = publishEnabled,
                isPublishing = employerJobUiState.isCreatingJob || isSubmittingJob,
                onBack = {
                    if (currentStep > 0) {
                        currentStep -= 1
                        scope.launch { listState.scrollToItem(0) }
                    }
                },
                onNext = {
                    if (currentStep < totalSteps - 1) {
                        currentStep += 1
                        scope.launch { listState.scrollToItem(0) }
                    }
                },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
                val isOnline by connectivityViewModel.isOnline.collectAsState()
                com.example.dutype.components.OfflineBanner(isOffline = !isOnline)

                CommonHeader(
                    title = stringResource(R.string.post_a_job),
                    navController = navController,
                    backgroundColor = Color.Transparent,
                    titleColor = Color(0xFF0F172A)
                )

                // Apr 2026: top stepper indicator (3 numbered steps).
                PostJobTopStepper(
                    currentStep = currentStep,
                    stepLabels = listOf(
                        "Job\ndetails",
                        "Pay &\nshift",
                        "Requirements\n& contact"
                    )
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
                        // Batch-p #11: stepper header removed; everything
                        // is on a single scrollable canvas now.
                        Spacer(modifier = Modifier.height(0.dp))
                    }

                    // Group 1: Job Details (title, work type, description, image) — STEP 0
                    if (currentStep == 0) item {
                        StudioGroupCard(
                            stepNumber = 1,
                            title = stringResource(R.string.tell_us_about_role),
                            subtitle = stringResource(R.string.job_title_work_type_desc),
                            icon = "\uD83D\uDCDD",
                            accentColor = Color(0xFF2563EB)
                        ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    EnhancedJobTitleSection(
                                        title = title,
                                        onTitleChange = { title = it },
                                        category = category,
                                        onCategoryChange = {
                                            category = it
                                            categoryManuallySet = true
                                        },
                                        customCategory = customCategory,
                                        onCustomCategoryChange = { customCategory = it }
                                    )
                                    Divider(color = Color(0xFFEDF2F7), thickness = 1.dp)
                                    WorkTypeSelection(
                                        workType = workType,
                                        onWorkTypeChange = { workType = it },
                                        workTypes = workTypes,
                                        payAmount = payAmount,
                                        onPayAmountChange = { payAmount = it },
                                        payType = payType,
                                        onPayTypeChange = { payType = it },
                                        category = category
                                    )
                                    Divider(color = Color(0xFFEDF2F7), thickness = 1.dp)
                                    JobDescriptionSection(
                                        description = description,
                                        onDescriptionChange = { description = it }
                                    )
                                    Divider(color = Color(0xFFEDF2F7), thickness = 1.dp)
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

                                                        Timber.d(" JOB IMAGE: Starting upload with compression...")

                                                        val uploadResult = com.example.dutype.utils.ImageUploadUtils.uploadWithRetry(
                                                            context = context,
                                                            uri = uri,
                                                            storagePath = storagePath
                                                        )

                                                        when (uploadResult) {
                                                            is com.example.dutype.utils.ImageUploadUtils.UploadResult.Success -> {
                                                                jobImageUrl = uploadResult.downloadUrl
                                                                Timber.d(" JOB IMAGE: âœ… Upload successful!")
                                                                Toast.makeText(context, context.getString(R.string.post_job_image_uploaded), Toast.LENGTH_SHORT).show()
                                                            }
                                                            is com.example.dutype.utils.ImageUploadUtils.UploadResult.Failure -> {
                                                                Timber.e(uploadResult.exception, " JOB IMAGE: âŒ Upload failed: ${uploadResult.error}")
                                                                Toast.makeText(context, context.getString(R.string.post_job_image_upload_failed, uploadResult.error ?: ""), Toast.LENGTH_SHORT).show()
                                                                jobImageUri = null
                                                                jobImageUrl = ""
                                                            }
                                                            else -> {
                                                            }
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Timber.e(e, " JOB IMAGE: âŒ Upload failed")
                                                    Toast.makeText(context, context.getString(R.string.post_job_image_upload_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
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
                                            Timber.d(" JOB IMAGE: Image removed")
                                        }
                                    )
                                    // Group 1 close
                                }
                        }
                    }

                    // Group 2: Pay & Location — STEP 1
                    if (currentStep == 1) item {
                        StudioGroupCard(
                            stepNumber = 2,
                            title = stringResource(R.string.pay_where_work),
                            subtitle = stringResource(R.string.set_pay_pin_location),
                            icon = "\uD83D\uDCB0",
                            accentColor = Color(0xFF059669)
                        ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    EnhancedLocationSection(
                                        location = location,
                                        onLocationChange = { location = it },
                                        isLoadingLocation = isLoadingLocation,
                                        locationError = locationError,
                                        onLocationButtonClick = {
                                            Timber.d(" LOCATION BUTTON: Clicked - checking permission...")
                                            if (locationService.hasLocationPermission()) {
                                                Timber.d(" LOCATION BUTTON: Permission granted, fetching fast-first location...")
                                                isLoadingLocation = true
                                                locationError = null
                                                scope.launch {
                                                    try {
                                                        fetchWorkLocationFast()
                                                        Timber.d(" LOCATION BUTTON: Location set - lat: $locationLatitude, lon: $locationLongitude")
                                                    } catch (e: Exception) {
                                                        Timber.e(e, " LOCATION BUTTON: Error getting location")
                                                        locationError = "Error getting location"
                                                    } finally {
                                                        isLoadingLocation = false
                                                    }
                                                }
                                            } else {
                                                Timber.d(" LOCATION BUTTON: Requesting permission...")
                                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                            }
                                        },
                                        onLocationSelected = { lat, lon ->
                                            locationLatitude = lat
                                            locationLongitude = lon
                                            Timber.d(" LOCATION SEARCH: Selected location - lat: $lat, lon: $lon")
                                        },
                                        savedLocations = savedWorkLocations
                                    )
                                    Divider(color = Color(0xFFEDF2F7), thickness = 1.dp)
                                    // Apr 2026: shift timing moved here from
                                    // Step 3 so the employer sets pay AND
                                    // shift in one place; Step 3 now focuses
                                    // purely on candidate requirements + contact.
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
                                                Icon(
                                                    imageVector = Icons.Default.AccessTime,
                                                    contentDescription = null,
                                                    tint = Color(0xFF65A30D),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = stringResource(R.string.post_job_schedule_urgency),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(18.dp))
                                        WorkScheduleSection(
                                            selectedShift = shiftTiming,
                                            onShiftSelected = { shiftTiming = it },
                                            selectedUrgency = urgency,
                                            onUrgencySelected = { urgency = it },
                                            customStart = customShiftStart,
                                            onCustomStartChange = { customShiftStart = it },
                                            customEnd = customShiftEnd,
                                            onCustomEndChange = { customShiftEnd = it }
                                        )
                                    }
                                    // Group 2 close
                                }
                        }
                    }

                    // Group 3: People, Schedule & Perks — STEP 2
                    if (currentStep == 2) item {
                        StudioGroupCard(
                            stepNumber = 3,
                            title = stringResource(R.string.who_you_want_extras),
                            subtitle = stringResource(R.string.vacancies_requirements_schedule),
                            icon = "\uD83D\uDC65",
                            accentColor = Color(0xFFD946EF)
                        ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    VacanciesSection(
                                        vacancies = vacancies,
                                        onVacanciesChange = { vacancies = it }
                                    )
                                    Divider(color = Color(0xFFEDF2F7), thickness = 1.dp)
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
                                    Divider(color = Color(0xFFEDF2F7), thickness = 1.dp)
                                    PerksSelectionSection(
                                        selectedPerks = selectedPerks,
                                        onPerksChanged = { selectedPerks = it },
                                        customPerks = customPerks,
                                        onCustomPerksChanged = { customPerks = it }
                                    )
                                    // Group 3 close
                                }
                        }
                    }

                    // Standalone: Contact details — STEP 2
                    if (currentStep == 2) item {
                        ContactSection(
                            contactNumber = contactNumber,
                            onContactNumberChange = { contactNumber = it },
                            employerName = employerName,
                            onEmployerNameChange = { employerName = it }
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
        title = stringResource(R.string.login_to_post_job),
        subtitle = stringResource(R.string.login_publish_job_subtitle)
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
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(180.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 24.dp)
                .size(200.dp)
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
                // Solid premium navy hero surface (no gradient).
                .background(
                    color = Color(0xFF0F172A),
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
                            text = stringResource(R.string.post_job_worker_preview),
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
                            text = stringResource(R.string.post_job_ready_percent, readinessPercent),
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
                        "$companyName â€¢ ${employerTrustTier.replace('_', ' ')} employer"
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
                        title = stringResource(R.string.pay_label),
                        value = if (payAmount.isBlank()) "Set salary" else "Rs. $payAmount ${payType.displayName}"
                    )
                    HeroSignalPill(
                        title = stringResource(R.string.type_label),
                        value = workType.ifBlank { "Choose work type" }
                    )
                    HeroSignalPill(
                        title = stringResource(R.string.area_label),
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
                            Icon(
                                imageVector = if (hasHeroImage) Icons.Default.Preview else Icons.Default.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
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
            title = stringResource(R.string.essentials),
            value = "$readinessCount / 4",
            caption = "Critical publish checks complete",
            tint = Color(0xFF2563EB)
        )
        StudioSignalCard(
            title = stringResource(R.string.trust_tier),
            value = employerTrustTier.replace('_', ' '),
            caption = "Employer reputation visible to workers",
            tint = Color(0xFFFF8A3D)
        )
        StudioSignalCard(
            title = stringResource(R.string.map_pin),
            value = if (hasValidJobCoordinates) "Verified" else "Missing",
            caption = if (hasValidJobCoordinates) "Exact work area captured" else "Set a precise local pin",
            tint = Color(0xFF059669)
        )
        StudioSignalCard(
            title = stringResource(R.string.visual),
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
                        text = stringResource(R.string.post_job_launch_checklist),
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
                    Icon(
                        imageVector = if (missingStudioItems.isEmpty()) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
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
                            text = "-",
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
private fun PostJobPublishBar(
    publishEnabled: Boolean,
    isPublishing: Boolean,
    onPublish: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            Button(
                onClick = onPublish,
                enabled = publishEnabled && !isPublishing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    disabledContainerColor = Color(0xFFCBD5E1)
                )
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Publishing…", color = Color.White, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(
                        text = "Publish Job",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PostJobStepperBar(
    currentStep: Int,
    totalSteps: Int,
    canAdvance: Boolean,
    publishEnabled: Boolean,
    isPublishing: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPublish: () -> Unit
) {
    val isLastStep = currentStep == totalSteps
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A))
                ) {
                    Text(text = stringResource(R.string.back), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            if (isLastStep) {
                Button(
                    onClick = onPublish,
                    modifier = Modifier
                        .weight(1f)
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
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = canAdvance,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = Color(0xFF94A3B8)
                    )
                ) {
                    Text(
                        text = if (currentStep == totalSteps - 1) "Review" else "Next",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
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
fun PolishedCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFF8A3D),
    showAccent: Boolean = true,
    content: @Composable () -> Unit
) {
    // When already inside a grouped parent card, render flat so we don't
    // produce nested chrome.
    if (com.example.dutype.employer.components.LocalSectionInGroup.current) {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
        return
    }
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
            // Solid card surface (no gradient).
            modifier = Modifier.background(com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground)
        ) {
            if (showAccent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        // Solid accent strip (no gradient).
                        .background(accentColor)
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
 * Studio-style group card used by the redesigned Post Job flow.
 * Renders a numbered step badge, gradient-tinted header (icon + title +
 * subtitle), then the section's stacked sub-sections inside one cohesive
 * card so the screen reads as a guided studio rather than a long form.
 */
@Composable
fun StudioGroupCard(
    stepNumber: Int,
    title: String,
    subtitle: String,
    icon: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.runtime.CompositionLocalProvider(
            com.example.dutype.employer.components.LocalSectionInGroup provides true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                content()
            }
        }
    }
}

/**
 * ANTI-FRAUD FEATURE: Structured Job Titles
 * 
 * Employers CANNOT type a job title freely. They must select from a pre-set list.
 * This eliminates "Earn â‚¹50,000/day working from home" scams instantly.
 * 
 * Implemented: December 27, 2025
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    var titleError by remember { mutableStateOf<String?>(null) }

    // Apr 2026 redesign: title is a free-text field. Predefined titles
    // appear as wrapping chips below the field — tap a chip to fill,
    // or type your own. Whatever the employer types is what workers see
    // on the job card; no more "Other" placeholder hiding the real title.
    val suggestedTitles = remember {
        listOf(
            "Cook" to "\uD83D\uDC68\u200D\uD83C\uDF73",
            "Chef" to "\uD83D\uDC69\u200D\uD83C\uDF73",
            "Maid" to "\uD83E\uDDF9",
            "House Cleaner" to "\uD83C\uDFE0",
            "Driver" to "\uD83D\uDE97",
            "Security Guard" to "\uD83D\uDEE1\uFE0F",
            "Delivery Executive" to "\uD83D\uDCE6",
            "Waiter" to "\uD83C\uDF7D\uFE0F",
            "Helper" to "\uD83E\uDD1D",
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
            "Office Boy" to "\uD83C\uDFE2",
            "Factory Worker" to "\uD83C\uDFED",
            "Construction Worker" to "\uD83D\uDC77",
            "Shop Assistant" to "\uD83D\uDED2",
            "Kitchen Helper" to "\uD83C\uDF73",
            "AC Technician" to "\u2744\uFE0F",
            "Tailor" to "\uD83E\uDDF5"
        )
    }

    fun categoryFor(t: String): JobCategory = when (t.trim()) {
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
        "" -> JobCategory.OTHER
        else -> JobCategory.OTHER
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
                            text = stringResource(R.string.post_job_title_label),
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
                        text = stringResource(R.string.post_job_select_position),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Free-text title field. Whatever is typed shows on the card.
            OutlinedTextField(
                value = title,
                onValueChange = { newValue ->
                    onTitleChange(newValue)
                    // Keep customCategory in sync for legacy draft saves;
                    // submitJobWithCoordinates uses it when category=OTHER.
                    onCustomCategoryChange(newValue.trim())
                    val matched = categoryFor(newValue)
                    onCategoryChange(matched)
                    titleError = null
                },
                label = { Text(stringResource(R.string.enter_job_title)) },
                placeholder = { Text(stringResource(R.string.job_title_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = titleError != null,
                supportingText = if (titleError != null) {
                    { Text(titleError!!, color = Color(0xFFDC2626)) }
                } else {
                    { Text(stringResource(R.string.job_title_hint), color = Color(0xFF6B7280)) }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (titleError != null) Color(0xFFDC2626) else primaryBlue,
                    focusedLabelColor = if (titleError != null) Color(0xFFDC2626) else primaryBlue,
                    unfocusedBorderColor = if (titleError != null) Color(0xFFDC2626) else Color(0xFFE2E8F0),
                    cursorColor = primaryBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Suggestion chips — wrap into multiple rows. Tap to fill.
            Text(
                text = stringResource(R.string.post_job_pick_quick_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Apr 2026: 2-row horizontally-scrolling chip grid (left → right).
            // Replaces the previous wrapping FlowRow so the form stays compact.
            androidx.compose.foundation.lazy.grid.LazyHorizontalGrid(
                rows = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
            ) {
                items(suggestedTitles.size) { index ->
                    val (suggestion, icon) = suggestedTitles[index]
                    val selected = title.trim().equals(suggestion, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(0.dp),
                        color = if (selected) primaryBlue.copy(alpha = 0.10f) else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 0.5.dp,
                            color = if (selected) primaryBlue else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.clickable {
                            onTitleChange(suggestion)
                            onCustomCategoryChange(suggestion)
                            onCategoryChange(categoryFor(suggestion))
                            titleError = null
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(icon, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) primaryBlue else Color(0xFF1E293B)
                            )
                        }
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
    workTypes: List<String>,
    payAmount: String,
    onPayAmountChange: (String) -> Unit,
    payType: PayType,
    onPayTypeChange: (PayType) -> Unit,
    category: JobCategory
) {
    val primaryBlue = Color(0xFF2563EB)

    // Apr 2026: market-rate hint and pay-rate guardrail dialog removed.

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
                        text = stringResource(R.string.post_job_work_type),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = stringResource(R.string.post_job_work_type_desc),
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

            Spacer(modifier = Modifier.height(24.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.payment_details),
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
                        text = stringResource(R.string.post_job_how_much_pay),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
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
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.post_job_work_location),
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
                        text = "Warning: $locationError",
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
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.post_job_requirements),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = stringResource(R.string.post_job_looking_for),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Experience Level
            RequirementChipSection(
                title = stringResource(R.string.experience_required),
                icon = Icons.Default.Work,
                options = experienceLevels,
                selectedOption = experienceLevel,
                onOptionSelected = onExperienceLevelChange,
                selectedColor = Color(0xFF10B981),
                allowCustomOption = true,
                customOptionHint = "Add your own experience"
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Age Range
            // Batch-p #11.2: chips are 18-30 / 30-45 / Any age plus a
            // free-form "Add your own" entry for cases like "21-28".
            RequirementChipSection(
                title = stringResource(R.string.preferred_age_range),
                icon = Icons.Default.Person,
                options = ageRanges,
                selectedOption = ageRange,
                onOptionSelected = onAgeRangeChange,
                selectedColor = Color(0xFF2563EB),
                allowCustomOption = true,
                customOptionHint = "Add your own age range"
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Gender Preference
            Text(
                text = stringResource(R.string.post_job_gender_preference),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(genders) { genderOption ->
                    FilterChip(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .height(34.dp),
                        onClick = { onGenderChange(genderOption) },
                        label = {
                            Text(
                                genderOption,
                                fontWeight = if (gender == genderOption) FontWeight.Medium else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        selected = gender == genderOption,
                        shape = RoundedCornerShape(8.dp),
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
    icon: ImageVector? = null,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    selectedColor: Color,
    allowCustomOption: Boolean = false,
    customOptionHint: String = "Add your own"
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customOptionText by remember { mutableStateOf("") }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF475569),
                modifier = Modifier.size(14.dp)
            )
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
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(options) { option ->
            FilterChip(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .height(34.dp),
                onClick = { onOptionSelected(option) },
                label = { 
                    Text(
                        option,
                        fontWeight = if (selectedOption == option) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 11.sp
                    ) 
                },
                selected = selectedOption == option,
                shape = RoundedCornerShape(8.dp),
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

        if (allowCustomOption) {
            item {
                FilterChip(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .height(34.dp),
                    onClick = { showCustomInput = !showCustomInput },
                    label = {
                        Text(
                            if (showCustomInput) "Cancel" else "Add your own +",
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    },
                    selected = false,
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF1F5F9),
                        selectedLabelColor = Color(0xFF334155),
                        containerColor = Color(0xFFF1F5F9),
                        labelColor = Color(0xFF334155)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent,
                        enabled = true,
                        selected = false
                    )
                )
            }
        }
    }

    if (allowCustomOption && showCustomInput) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customOptionText,
                onValueChange = { customOptionText = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(customOptionHint) },
                placeholder = { Text(stringResource(R.string.education_example_hint)) },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = selectedColor,
                    focusedLabelColor = selectedColor,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    cursorColor = selectedColor
                )
            )

            Button(
                onClick = {
                    val trimmed = customOptionText.trim()
                    if (trimmed.isNotBlank()) {
                        val existingOption = options.firstOrNull { it.equals(trimmed, ignoreCase = true) }
                        onOptionSelected(existingOption ?: trimmed)
                        customOptionText = ""
                        showCustomInput = false
                    }
                },
                enabled = customOptionText.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.add_button))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PerksSelectionSection(
    selectedPerks: Set<JobPerk>,
    onPerksChanged: (Set<JobPerk>) -> Unit,
    customPerks: List<String> = emptyList(),
    onCustomPerksChanged: (List<String>) -> Unit = {}
) {
    var newPerkText by remember { mutableStateOf("") }
    var showCustomPerkInput by remember { mutableStateOf(false) }
    val totalCount = selectedPerks.size + customPerks.size

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
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
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
                            text = stringResource(R.string.post_job_attract_candidates),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                
                // Selected count badge
                if (totalCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Text(
                            text = stringResource(R.string.post_job_selected_count, totalCount),
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
                onPerksChanged = onPerksChanged,
                onAddOwnPerkClick = { showCustomPerkInput = true }
            )

            if (showCustomPerkInput) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPerkText,
                        onValueChange = { if (it.length <= 40) newPerkText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. Free meals", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = newPerkText.trim()
                            if (trimmed.isNotEmpty() &&
                                customPerks.none { it.equals(trimmed, ignoreCase = true) } &&
                                selectedPerks.none { it.displayName.equals(trimmed, ignoreCase = true) }
                            ) {
                                onCustomPerksChanged(customPerks + trimmed)
                                newPerkText = ""
                                showCustomPerkInput = false
                            }
                        },
                        enabled = newPerkText.trim().isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(stringResource(R.string.add_button), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (customPerks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customPerks.forEach { perk ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = perk,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onCustomPerksChanged(customPerks - perk) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove $perk",
                                        tint = Color(0xFF065F46),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



// ===========================================================================
// Apr 2026: Apna-style 3-step wizard helpers for the Post Job screen.
// Top stepper indicator + dual-action bottom bar (Back / Next / Post job).
// Kept minimalist on purpose: no shadows, single accent color, tight type.
// ===========================================================================

@Composable
private fun PostJobTopStepper(
    currentStep: Int,
    stepLabels: List<String>
) {
    val accent = Color(0xFF2563EB)
    val mutedCircle = Color(0xFFE2E8F0)
    val mutedText = Color(0xFF94A3B8)
    val connector = Color(0xFFE2E8F0)

    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            stepLabels.forEachIndexed { index, label ->
                val isActive = index == currentStep
                val isDone = index < currentStep
                val circleColor = when {
                    isActive -> accent.copy(alpha = 0.12f)
                    isDone -> accent
                    else -> mutedCircle
                }
                val borderColor = if (isActive) accent else Color.Transparent
                val numberColor = when {
                    isActive -> accent
                    isDone -> Color.White
                    else -> mutedText
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(circleColor, RoundedCornerShape(18.dp))
                            .border(
                                width = if (isActive) 2.dp else 0.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = numberColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = label,
                        color = if (isActive) Color(0xFF0F172A) else mutedText,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }

                if (index < stepLabels.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(top = 17.dp)
                            .height(1.dp)
                            .weight(0.5f)
                            .background(if (index < currentStep) accent else connector)
                    )
                }
            }
        }
    }
}

@Composable
private fun PostJobStepNavBar(
    currentStep: Int,
    totalSteps: Int,
    canAdvance: Boolean,
    publishEnabled: Boolean,
    isPublishing: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPublish: () -> Unit
) {
    val accent = Color(0xFF2563EB)
    val isLast = currentStep == totalSteps - 1
    val showBack = currentStep > 0

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showBack) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Text("Back", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            if (isLast) {
                Button(
                    onClick = onPublish,
                    enabled = publishEnabled && !isPublishing,
                    modifier = Modifier
                        .weight(if (showBack) 1.6f else 1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = accent,
                        disabledContainerColor = accent.copy(alpha = 0.4f),
                        contentColor = Color.White,
                        disabledContentColor = Color.White
                    )
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Post job", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            } else {
                Button(
                    onClick = onNext,
                    enabled = canAdvance,
                    modifier = Modifier
                        .weight(if (showBack) 1.6f else 1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = accent,
                        disabledContainerColor = accent.copy(alpha = 0.4f),
                        contentColor = Color.White,
                        disabledContentColor = Color.White
                    )
                ) {
                    Text("Next", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}
