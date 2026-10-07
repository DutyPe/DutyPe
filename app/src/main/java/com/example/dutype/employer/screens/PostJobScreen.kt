package com.example.dutype.employer.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.data.JobDraftDataStore
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.SelectableLocationMap
import com.example.dutype.employer.components.ContactSection
import com.example.dutype.employer.components.JobDescriptionSection
import com.example.dutype.employer.components.JobImageUploadSection
import com.example.dutype.employer.components.VacanciesSection
import com.example.dutype.employer.components.WorkScheduleSection
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.employer.models.EmploymentType
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.JobShift
import com.example.dutype.location.LocationSuggestion
import com.example.dutype.models.JobListing
import com.example.dutype.models.InstantRequest
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.utils.JobValidationUtils
import com.example.dutype.utils.PayRateValidationResult
import com.example.dutype.utils.ValidationResult
import com.example.dutype.utils.findActivity
import com.example.dutype.viewmodels.EmployerJobsViewModel
import com.example.dutype.viewmodels.InstantHelpViewModel
import com.example.dutype.viewmodels.SubscriptionViewModel
import com.google.android.gms.maps.model.LatLng
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

// ---------------------------------------------------------------------------
// Post-job wizard: constants, controller (state + logic) and small composables.
// PostJobScreen() is intentionally tiny: a giant single @Composable body made
// D8/ART fail verification (VerifyError), so all state/logic/UI live in
// separate small functions below.
// ---------------------------------------------------------------------------

private const val PostJobTotalSteps = 3

private val PostJobBaseExperienceLevels = listOf(
    "No Experience Required",
    "Fresher (Educated)",
    "1-3 years",
    "3-5 years",
    "5+ years"
)
private val PostJobBaseEducationRequirements = listOf(
    "No qualification required",
    "10th pass",
    "12th pass",
    "ITI",
    "Diploma",
    "Graduate",
    "Any qualification"
)
private val PostJobGenders = listOf("Male", "Female", "Both")
private val PostJobQuickTitles = listOf("Cook", "Electrician", "Driver", "Helper")
private val PostJobBaseCategoryChips = listOf(
    JobCategory.COOK,
    JobCategory.MAID,
    JobCategory.DRIVER,
    JobCategory.HELPER,
    JobCategory.SECURITY,
    JobCategory.DELIVERY,
    JobCategory.OTHER
)
private val PostJobStartDateOptions = listOf("Immediately", "Tomorrow", "Next Week")
private val PostJobPerkOptions = listOf(
    "Food Provided",
    "Accommodation / Stay",
    "Transport / Fuel",
    "Overtime Bonus",
    "Flexible Timings",
    "Weekly Payout",
    "Tips Included",
    "Performance Bonus",
    "Training Provided",
    "Uniform Provided",
    "ESI / PF",
    "Mobile Allowance"
)
private val PostJobExtraPayTypes = listOf(PayType.HOURLY, PayType.NEGOTIABLE)
private const val MIN_DESCRIPTION_CHARS = 10

private fun postJobWithCustomOption(base: List<String>, current: String): List<String> {
    return if (current.isNotBlank() && current !in base) base + current else base
}

private fun postJobCategoryChipsFor(category: JobCategory): List<JobCategory> {
    return if (category in PostJobBaseCategoryChips) PostJobBaseCategoryChips else PostJobBaseCategoryChips + category
}

private fun postJobStepTitle(step: Int): String = when (step) {
    1 -> "What job do you need?"
    2 -> "Where and when?"
    else -> "Pay & perks"
}

private fun postJobVacanciesValid(vacancies: String): Boolean {
    val n = vacancies.toIntOrNull()
    return n != null && n in 1..50
}

/**
 * Plain (non-composable) holder for all post-job form state, dependencies and
 * business logic. Keeps the composable functions small.
 */
private class PostJobController(
    var context: Context,
    var navController: NavController,
    val employerJobViewModel: EmployerJobsViewModel,
    val scope: CoroutineScope,
    private val stepState: MutableState<Int>
) {
    var rootNavController: NavController? = null
    var employerId: String? = null
    var onJobPosted: ((String?) -> Unit)? = null
    var onReviewTrigger: (() -> Unit)? = null
    var requestLocationPermission: () -> Unit = {}
    var requestNotificationPermission: () -> Unit = {}

    val locationService get() = employerJobViewModel.locationService
    private val profileCompletionService get() = employerJobViewModel.profileCompletionService
    private val locationRepository by lazy { com.example.dutype.di.locationRepositoryFromHilt(context) }

    var currentStep: Int
        get() = stepState.value
        set(value) {
            stepState.value = value
        }

    // Dialog / sheet state
    var showNoCreditsDialog by mutableStateOf(false)
    var isCheckingProfile by mutableStateOf(false)
    var profileCheckResult by mutableStateOf<com.example.dutype.services.ProfileCompletionService.PreJobPostCheckResult?>(null)
    var showProfileIncompleteDialog by mutableStateOf(false)
    var scamValidationResult by mutableStateOf<ValidationResult?>(null)
    var payRateValidationResult by mutableStateOf<PayRateValidationResult?>(null)
    var showScamWarningDialog by mutableStateOf(false)
    var isSubmittingJob by mutableStateOf(false)
    var showLocationWarningDialog by mutableStateOf(false)
    var locationDistanceKm by mutableStateOf(0.0)
    var pendingJobSubmission by mutableStateOf(false)
    var showLoginBottomSheet by mutableStateOf(false)
    var showPostJobNotificationBottomSheet by mutableStateOf(false)
    var pendingNavJobId by mutableStateOf<String?>(null)

    // Form state
    var title by mutableStateOf("")
    var payAmount by mutableStateOf("")
    var payType by mutableStateOf(PayType.MONTHLY)
    var location by mutableStateOf("")
    var description by mutableStateOf("")
    var contactNumber by mutableStateOf("")
    var category by mutableStateOf(JobCategory.OTHER)
    var customCategory by mutableStateOf("")
    var shift by mutableStateOf(JobShift.ANY)
    var vacancies by mutableStateOf("")
    var employerName by mutableStateOf("")
    var companyName by mutableStateOf("")
    var employerType by mutableStateOf("COMPANY")
    var employmentType by mutableStateOf(EmploymentType.FULL_TIME)
    var experienceLevel by mutableStateOf("No Experience Required")
    var educationRequired by mutableStateOf("No qualification required")
    var gender by mutableStateOf("Both")
    var startDate by mutableStateOf("Immediately")
    var selectedPerks by mutableStateOf(setOf<String>())
    var repostOfJobId by mutableStateOf("")
    var jobImageUri by mutableStateOf<Uri?>(null)
    var jobImageUrl by mutableStateOf("")
    var isUploadingJobImage by mutableStateOf(false)
    var isLoadingLocation by mutableStateOf(false)
    var locationError by mutableStateOf<String?>(null)
    var locationLatitude by mutableStateOf(0.0)
    var locationLongitude by mutableStateOf(0.0)

    val draftTrigger = MutableStateFlow(0L)

    private val isCreatingJob: Boolean
        get() = employerJobViewModel.uiState.value.isCreatingJob

    fun navigateAfterJobPosted(jobId: String?) {
        onJobPosted?.invoke(jobId)
        if (onJobPosted == null) {
            if (!jobId.isNullOrBlank()) {
                navController.navigate(Routes.employerApplicationsJobRoute(jobId)) {
                    popUpTo(Routes.EMPLOYER_HOME) { inclusive = false }
                }
            } else {
                navController.navigate(Routes.EMPLOYER_HOME) {
                    popUpTo(Routes.EMPLOYER_HOME) { inclusive = false }
                }
            }
        }
    }

    /** Talk-to-post: the AI draft fills the form; the employer checks it and posts as usual. */
    fun applyAiDraft(d: com.example.dutype.employer.ai.AiJobDraft) {
        if (d.title.isNotBlank()) title = d.title
        JobCategory.entries.firstOrNull { it.name == d.category }?.let { category = it }
        payType = com.example.dutype.utils.PayRules.vacancyPayType(PayType.fromKey(d.payType))
        payAmount = if (d.payAmount > 0) d.payAmount.toString() else ""
        vacancies = d.vacancies.coerceIn(1, 50).toString()
        shift = JobShift.fromKey(d.shift)
        employmentType = com.example.dutype.utils.PayRules.vacancyEmploymentType(EmploymentType.fromKey(d.employmentType))
        experienceLevel = d.experience
        educationRequired = d.education
        gender = d.gender
        selectedPerks = d.perks.toSet()
        if (d.description.isNotBlank()) description = d.description
        currentStep = 1
        draftTrigger.value = System.currentTimeMillis()
    }

    fun syncInferredCategory() {
        val inferred = com.example.dutype.utils.JobCategoryResolver.inferCategory(title, description) ?: JobCategory.OTHER
        if (inferred != category) category = inferred
    }

    // ---------------------------------------------------------------- drafts
    private fun hasDraftContent(): Boolean {
        return title.isNotBlank() ||
            description.isNotBlank() ||
            payAmount.isNotBlank() ||
            location.isNotBlank() ||
            vacancies.isNotBlank() ||
            contactNumber.isNotBlank() ||
            customCategory.isNotBlank() ||
            experienceLevel != "No Experience Required" ||
            educationRequired != "No qualification required" ||
            gender != "Both" ||
            shift != JobShift.ANY ||
            employmentType != EmploymentType.FULL_TIME ||
            selectedPerks.isNotEmpty() ||
            repostOfJobId.isNotBlank()
    }

    fun autoSaveDraft() {
        if (!hasDraftContent()) return
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
            shift = shift,
            employmentType = employmentType,
            experienceLevel = experienceLevel,
            educationRequired = educationRequired,
            gender = gender,
            benefits = selectedPerks.map { it.take(60).trim() }.filter { it.isNotBlank() }.toList(),
            repostOfJobId = repostOfJobId
        )
        employerJobViewModel.saveDraft(draft)
        Timber.d("AUTO-SAVE: Draft saved")
    }

    suspend fun restoreDraft() {
        try {
            val savedDraft = employerJobViewModel.getSavedDraft()
            if (savedDraft != null && savedDraft.hasContent()) {
                Timber.d("Restoring job draft (device-scoped)...")
                title = savedDraft.title
                description = savedDraft.description
                payAmount = savedDraft.payAmount
                payType = com.example.dutype.utils.PayRules.vacancyPayType(savedDraft.payType)
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
                shift = savedDraft.shift
                employmentType = com.example.dutype.utils.PayRules.vacancyEmploymentType(savedDraft.employmentType)
                selectedPerks = savedDraft.benefits.toSet()
                experienceLevel = savedDraft.experienceLevel
                educationRequired = savedDraft.educationRequired
                gender = if (savedDraft.gender.equals("Any", ignoreCase = true)) {
                    "Both"
                } else {
                    savedDraft.gender
                }
                repostOfJobId = savedDraft.repostOfJobId
                Timber.d("Draft restored successfully")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error restoring job draft")
        }
    }

    // --------------------------------------------------------------- profile
    suspend fun loadEmployerProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        try {
            val cachedProfile = employerJobViewModel.getCachedProfile()
            if (cachedProfile != null) {
                if (cachedProfile.companyName.isNotBlank()) {
                    companyName = cachedProfile.companyName
                }
                if (cachedProfile.employerName.isNotBlank()) {
                    employerName = cachedProfile.employerName
                }
                if (cachedProfile.contactPhone.isNotBlank()) {
                    contactNumber = cachedProfile.contactPhone
                }
                if (cachedProfile.employerType.isNotBlank()) {
                    employerType = cachedProfile.employerType
                }
                if (companyName.isBlank()) {
                    val fallbackName = cachedProfile.companyName.ifBlank { cachedProfile.employerName }
                    if (fallbackName.isNotBlank()) {
                        companyName = fallbackName
                    }
                }
            } else {
                Timber.d("Cache miss, fetching from Firestore...")
                loadProfileFromFirestore(currentUser)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading employer profile")
        }
    }

    private suspend fun loadProfileFromFirestore(currentUser: com.google.firebase.auth.FirebaseUser) {
        val P = com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
        val doc = com.example.dutype.di.firestoreFromHilt(context)
            .collection(P.COLLECTION).document(currentUser.uid).get().await()
        if (!doc.exists()) return
        doc.getString(P.OWNER_NAME)?.takeIf { it.isNotBlank() }?.let { employerName = it }
        doc.getString(P.PHONE)?.takeIf { it.isNotBlank() }?.let { contactNumber = it }
        employerType = doc.getString(P.EMPLOYER_TYPE) ?: employerType
        companyName = doc.getString(P.BUSINESS_NAME)?.takeIf { it.isNotBlank() } ?: employerName
    }

    fun clearLegacyStepRedirectFlag() {
        val prefs = context.getSharedPreferences("dutype_prefs", Context.MODE_PRIVATE)
        val shouldJumpToLastStep = prefs.getBoolean("jump_to_post_job_last_step", false)
        if (shouldJumpToLastStep) {
            Timber.d("Clearing legacy post-job step redirect flag")
            prefs.edit().remove("jump_to_post_job_last_step").apply()
        }
    }

    // -------------------------------------------------------------- location
    suspend fun fetchWorkLocationFast() {
        val cachedLocation = locationRepository.lastKnownLocationIfFresh(10 * 60 * 1000L)
            ?: locationRepository.lastKnownLocation()
        if (cachedLocation != null) {
            val addr = cachedLocation.getFullAddress().ifBlank { cachedLocation.address }
            if (addr.isNotBlank()) location = addr
            locationLatitude = cachedLocation.latitude
            locationLongitude = cachedLocation.longitude
            Timber.d("LOCATION DEBUG: Using recent cached location immediately")
        }

        val refinedLocation = locationRepository.getHighAccuracy(
            timeoutMs = if (cachedLocation != null) 5000L else 10000L,
            minAccuracyMeters = 50f
        ) ?: locationService.getCurrentLocation()
          ?: locationService.getCachedLocation()

        if (refinedLocation != null && com.example.dutype.utils.GeoUtils.hasValidCoordinates(refinedLocation.latitude, refinedLocation.longitude)) {
            val addr = refinedLocation.getFullAddress()
            if (addr.isNotBlank()) {
                location = addr
            } else if (location.isBlank()) {
                location = "${String.format("%.4f", refinedLocation.latitude)}, ${String.format("%.4f", refinedLocation.longitude)}"
            }
            locationLatitude = refinedLocation.latitude
            locationLongitude = refinedLocation.longitude
            locationError = null
            Toast.makeText(context, context.getString(R.string.location_updated), Toast.LENGTH_SHORT).show()
            Timber.d("LOCATION DEBUG: Refined location fetched (${refinedLocation.accuracy}m)")
        } else if (cachedLocation == null) {
            Timber.w("LOCATION DEBUG: No cached or refined location available")
            locationError = "Unable to get current location"
        } else {
            Timber.w("LOCATION DEBUG: Refinement timed out, keeping cached location")
        }
    }

    fun startLocationFetch(errorMessage: (Exception) -> String) {
        isLoadingLocation = true
        locationError = null
        scope.launch {
            try {
                fetchWorkLocationFast()
            } catch (e: Exception) {
                Timber.e(e, "LOCATION DEBUG: Error getting location")
                locationError = errorMessage(e)
            } finally {
                isLoadingLocation = false
            }
        }
    }

    fun onLocationPermissionResult(isGranted: Boolean) {
        Timber.d("LOCATION DEBUG: Permission result - isGranted: $isGranted")
        if (isGranted) {
            startLocationFetch { e -> "Error getting location: ${e.message}" }
        } else {
            Timber.w("LOCATION DEBUG: Permission denied")
            locationError = "Location permission denied"
        }
    }

    fun onLocationButtonClick() {
        if (locationService.hasLocationPermission()) {
            startLocationFetch { _ -> context.getString(R.string.error_getting_location) }
        } else {
            requestLocationPermission()
        }
    }

    fun onLocationSelected(lat: Double, lon: Double) {
        locationLatitude = lat
        locationLongitude = lon
        Timber.d("LOCATION SEARCH: Selected location - lat: $lat, lon: $lon")
    }

    // ----------------------------------------------------------- job image
    fun onJobImageSelected(uri: Uri) {
        jobImageUri = uri
        scope.launch { uploadJobImage(uri) }
    }

    fun onJobImageRemoved() {
        jobImageUri = null
        jobImageUrl = ""
        Timber.d("JOB IMAGE: Image removed")
    }

    private suspend fun uploadJobImage(uri: Uri) {
        isUploadingJobImage = true
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val fileName = "job_image_${System.currentTimeMillis()}.jpg"
                val storagePath = "job_images/${currentUser.uid}/$fileName"

                Timber.d("JOB IMAGE: Starting upload with compression...")

                val uploadResult = com.example.dutype.utils.ImageUploadUtils.uploadWithRetry(
                    context = context,
                    uri = uri,
                    storagePath = storagePath
                )

                when (uploadResult) {
                    is com.example.dutype.utils.ImageUploadUtils.UploadResult.Success -> {
                        jobImageUrl = uploadResult.downloadUrl
                        Timber.d("JOB IMAGE: Upload successful!")
                        Toast.makeText(context, context.getString(R.string.post_job_image_uploaded), Toast.LENGTH_SHORT).show()
                    }
                    is com.example.dutype.utils.ImageUploadUtils.UploadResult.Failure -> {
                        Timber.e(uploadResult.exception, "JOB IMAGE: Upload failed: ${uploadResult.error}")
                        Toast.makeText(context, context.getString(R.string.post_job_image_upload_failed, uploadResult.error ?: ""), Toast.LENGTH_SHORT).show()
                        jobImageUri = null
                        jobImageUrl = ""
                    }
                    else -> {
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "JOB IMAGE: Upload failed")
            Toast.makeText(context, context.getString(R.string.post_job_image_upload_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
            jobImageUri = null
            jobImageUrl = ""
        } finally {
            isUploadingJobImage = false
        }
    }

    // ------------------------------------------------------------ validation
    fun validateStep(step: Int): Boolean {
        return when (step) {
            1 -> {
                if (title.isBlank() || description.trim().length < MIN_DESCRIPTION_CHARS) return false
                val scamCheck = JobValidationUtils.validateAgainstScamKeywords(title, description)
                scamValidationResult = scamCheck
                scamCheck.isValid
            }
            2 -> {
                if ((payType != PayType.NEGOTIABLE && payAmount.isBlank()) || location.isBlank()) return false
                if (payType != PayType.NEGOTIABLE &&
                    (payAmount.filter { it.isDigit() }.toLongOrNull() ?: 0L) > com.example.dutype.utils.SalaryFormatter.MAX_PAY_RUPEES
                ) return false
                if (com.example.dutype.utils.PayRules.vacancyError(context, payType, employmentType, payAmount.filter { it.isDigit() }.toLongOrNull() ?: 0L) != null) return false
                val payCheck = JobValidationUtils.validatePayRate(category, payType, payAmount)
                payRateValidationResult = payCheck
                true
            }
            3 -> contactNumber.isNotBlank() && postJobVacanciesValid(vacancies)
            4 -> true
            else -> false
        }
    }

    // --------------------------------------------------------------- publish
    private fun buildDescriptionText(): String {
        val base = description.trim().ifBlank { "Hiring for ${title.trim()}. ${shift.displayName}." }
        return if (startDate == "Immediately") base else "$base\n\nStart: $startDate"
    }

    private fun buildJobForm(lat: Double, lng: Double, vacancyCount: Int): com.example.dutype.jobs.JobForm {
        val fullDescription = buildDescriptionText()
        return com.example.dutype.jobs.JobForm(
            title = title.trim(),
            category = com.example.dutype.utils.JobCategoryResolver.inferCategoryName(title, fullDescription, category.name),
            employmentType = employmentType.key,
            payAmount = if (payType == PayType.NEGOTIABLE) 0L else payAmount.filter { it.isDigit() }.toLongOrNull() ?: 0L,
            payType = payType.key,
            vacancies = vacancyCount,
            urgency = com.example.dutype.firestore.FirestoreSchema.Values.Urgency.NORMAL,
            shift = shift.key,
            area = com.example.dutype.utils.AreaText.from(location),
            lat = lat,
            lng = lng,
            photoUrl = jobImageUrl.ifBlank { null },
            description = fullDescription,
            addressText = location.trim(),
            contactNumber = contactNumber,
            gender = when (gender) { "Male" -> "MALE"; "Female" -> "FEMALE"; else -> "ANY" },
            experienceRequired = experienceLevel,
            educationRequired = educationRequired,
            benefits = selectedPerks.map { it.take(60).trim() }.filter { it.isNotBlank() }.toList(),
            businessName = companyName.trim().takeIf { it.isNotBlank() && !it.equals(employerName.trim(), ignoreCase = true) }.orEmpty()
        )
    }

    private fun submitJobWithCoordinates(finalLatitude: Double, finalLongitude: Double) {
        if (isCreatingJob) return
        val vacancyCount = vacancies.toIntOrNull()
        if (vacancyCount == null || vacancyCount !in 1..50) {
            Toast.makeText(context, context.getString(R.string.enter_number_positions), Toast.LENGTH_SHORT).show()
            isSubmittingJob = false
            return
        }
        employerJobViewModel.postJob(buildJobForm(finalLatitude, finalLongitude, vacancyCount)) { success, newJobId, message ->
            onJobCreated(success, newJobId, message)
        }
    }

    private fun onJobCreated(success: Boolean, newJobId: String?, message: String?) {
        isSubmittingJob = false
        if (success) {
            Timber.i("JOB POSTING DEBUG: Job posted successfully!")
            Toast.makeText(context, context.getString(R.string.post_job_success), Toast.LENGTH_SHORT).show()
            try {
                onReviewTrigger?.invoke()
            } catch (e: Exception) {
                Timber.e(e, "Review trigger failed after job post")
            }

            val isTiramisuOrAbove = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
            val hasNotificationPermission = if (isTiramisuOrAbove) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (!hasNotificationPermission && isTiramisuOrAbove) {
                pendingNavJobId = newJobId
                showPostJobNotificationBottomSheet = true
            } else {
                navigateAfterJobPosted(newJobId)
            }
        } else {
            Timber.e("JOB POSTING DEBUG: Job posting failed: $message")
            Toast.makeText(context, context.getString(R.string.post_job_error, message), Toast.LENGTH_LONG).show()
        }
    }

    /** Handles login check, profile check, and geocoding, then posts the job. */
    fun submitJob() {
        Timber.d("JOB POSTING DEBUG: submitJob() called")

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Timber.d("JOB POSTING DEBUG: User not logged in, showing login sheet")
            showLoginBottomSheet = true
            return
        }

        if (isCreatingJob) {
            Timber.w("JOB POSTING DEBUG: Already creating job (ViewModel guard), ignoring duplicate call")
            return
        }

        if (!validateStep(4)) {
            Timber.w("JOB POSTING DEBUG: Step 4 validation failed")
            return
        }

        isCheckingProfile = true
        val uid = currentUser.uid
        val displayName = currentUser.displayName
        scope.launch {
            try {
                runSubmitFlow(uid, displayName)
            } catch (e: Exception) {
                Timber.e(e, "JOB POSTING DEBUG: Error in submitJob")
                isCheckingProfile = false
                Toast.makeText(context, context.getString(R.string.post_job_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun runSubmitFlow(uid: String, displayName: String?) {
        val cached = profileCheckResult
        val checkResult = if (cached != null && cached.canPost) {
            cached
        } else {
            val result = profileCompletionService.preJobPostCheck(uid)
            profileCheckResult = result
            result
        }

        isCheckingProfile = false

        if (!checkResult.canPost) {
            Timber.w("PROFILE CHECK: Employer cannot post jobs - ${checkResult.completionPercentage}% complete")
            showProfileIncompleteDialog = true
            return
        }

        ensureCompanyName(uid, displayName)

        var finalLatitude = locationLatitude
        var finalLongitude = locationLongitude

        if (locationLatitude == 0.0 && locationLongitude == 0.0 && location.isNotBlank()) {
            Timber.d("JOB POSTING DEBUG: Geocoding manual location: $location")
            val geocodedLocation = locationService.getCoordinatesFromAddress(location)
            if (geocodedLocation != null) {
                finalLatitude = geocodedLocation.latitude
                finalLongitude = geocodedLocation.longitude
            } else {
                Timber.w("JOB POSTING DEBUG: Geocoding failed")
            }
        }

        if (!com.example.dutype.utils.GeoUtils.hasValidCoordinates(finalLatitude, finalLongitude)) {
            locationError = context.getString(R.string.valid_job_location_required)
            Toast.makeText(context, R.string.valid_job_location_required, Toast.LENGTH_SHORT).show()
            return
        }

        if (finalLatitude != 0.0 && finalLongitude != 0.0) {
            launchLocationConsistencyCheck(finalLatitude, finalLongitude)
        }

        pendingJobSubmission = false
        submitJobWithCoordinates(finalLatitude, finalLongitude)
    }

    private suspend fun ensureCompanyName(uid: String, displayName: String?) {
        if (!companyName.isBlank()) return
        profileCompletionService.getEmployer(uid).getOrNull()?.displayName
            ?.takeIf { it.isNotBlank() }
            ?.let { companyName = it }
        if (companyName.isBlank()) {
            companyName = employerName.ifBlank {
                displayName?.trim().orEmpty().ifBlank { "DutyPe Employer" }
            }
        }
    }

    /** ANTI-FRAUD: non-blocking location consistency check running in background. */
    private fun launchLocationConsistencyCheck(finalLatitude: Double, finalLongitude: Double) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val employerLocation = locationRepository.getHighAccuracy(
                    timeoutMs = 5000L,
                    minAccuracyMeters = 100f
                )

                if (employerLocation != null) {
                    val distance = locationService.calculateDistance(
                        employerLocation.latitude, employerLocation.longitude,
                        finalLatitude, finalLongitude
                    )

                    Timber.d("ANTI-FRAUD: Location consistency check - Distance: ${String.format("%.2f", distance)} km")

                    if (distance > 50.0) {
                        Timber.w("ANTI-FRAUD: Suspicious location detected (${String.format("%.2f", distance)} km away)")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "ANTI-FRAUD: Background location check failed")
            }
        }
    }

    fun attemptPublishJob() {
        val scamCheck = JobValidationUtils.validateAgainstScamKeywords(title, description)
        scamValidationResult = scamCheck
        if (!scamCheck.isValid) {
            showScamWarningDialog = true
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showLoginBottomSheet = true
            return
        }

        submitJob()
    }

    fun goNext() {
        when (currentStep) {
            1 -> goNextFromStep1()
            2 -> goNextFromStep2()
            else -> goNextFromStep3()
        }
    }

    private fun goNextFromStep1() {
        if (title.isBlank()) {
            Toast.makeText(context, "Enter a job title", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.trim().length < MIN_DESCRIPTION_CHARS) {
            Toast.makeText(context, R.string.post_job_description_required, Toast.LENGTH_LONG).show()
            return
        }
        val scamCheck = JobValidationUtils.validateAgainstScamKeywords(title, description)
        scamValidationResult = scamCheck
        if (!scamCheck.isValid) {
            showScamWarningDialog = true
            return
        }
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showLoginBottomSheet = true
            return
        }
        if (!postJobVacanciesValid(vacancies)) {
            vacancies = "1"
        }
        currentStep = 2
    }

    private fun goNextFromStep2() {
        if (location.isBlank()) {
            locationError = context.getString(R.string.valid_job_location_required)
            Toast.makeText(context, R.string.valid_job_location_required, Toast.LENGTH_SHORT).show()
            return
        }
        currentStep = 3
    }

    private fun goNextFromStep3() {
        if (payType != PayType.NEGOTIABLE && (payAmount.filter { it.isDigit() }.toLongOrNull() ?: 0L) <= 0L) {
            Toast.makeText(context, "Enter the wage", Toast.LENGTH_SHORT).show()
            return
        }
        if (payType != PayType.NEGOTIABLE &&
            (payAmount.filter { it.isDigit() }.toLongOrNull() ?: 0L) > com.example.dutype.utils.SalaryFormatter.MAX_PAY_RUPEES
        ) {
            Toast.makeText(context, R.string.pay_max_limit, Toast.LENGTH_SHORT).show()
            return
        }
        com.example.dutype.utils.PayRules.vacancyError(context, payType, employmentType, payAmount.filter { it.isDigit() }.toLongOrNull() ?: 0L)?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            return
        }
        if (contactNumber.isBlank()) {
            Toast.makeText(context, "Enter a contact number", Toast.LENGTH_SHORT).show()
            return
        }
        if (!postJobVacanciesValid(vacancies)) {
            Toast.makeText(context, context.getString(R.string.enter_number_positions), Toast.LENGTH_SHORT).show()
            return
        }
        attemptPublishJob()
    }

    fun openProfileSetup() {
        val navToUse = rootNavController ?: navController
        navToUse.navigate(
            Routes.employerProfileSetupWithReturnRoute(Routes.EMPLOYER_POST_JOB)
        ) {
            launchSingleTop = true
        }
    }

    fun openSubscription() {
        val navToUse = rootNavController ?: navController
        navToUse.navigate(Routes.EMPLOYER_SUBSCRIPTION)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    navController: NavController,
    rootNavController: NavController? = null,
    employerId: String? = null,
    initialTab: String? = null,
    onJobPosted: ((String?) -> Unit)? = null,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    val context = LocalContext.current

    val statusBarColorToken = EmployerColors.StatusBarColor
    LaunchedEffect(statusBarColorToken) {
        onStatusBarColorChange?.invoke(statusBarColorToken)
    }
    val scope = rememberCoroutineScope()
    val employerJobViewModel: EmployerJobsViewModel = hiltViewModel()
    val instantHelpViewModel: InstantHelpViewModel = hiltViewModel()
    val subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
    val employerSubscription by subscriptionViewModel.activeSubscription.collectAsState()
    val stepState = rememberSaveable { mutableStateOf(1) }
    var selectedPostJobTab by rememberSaveable { mutableIntStateOf(0) } // 0: Instant Job, 1: Regular Job

    val c = remember {
        PostJobController(context, navController, employerJobViewModel, scope, stepState)
    }
    c.context = context
    c.navController = navController
    c.rootNavController = rootNavController
    c.employerId = employerId
    c.onJobPosted = onJobPosted
    val reviewTriggerService = com.example.dutype.di.rememberInAppReviewTriggerService()
    c.onReviewTrigger = {
        context.findActivity()?.let { reviewTriggerService.onEmployerJobPosted(it) }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        c.onLocationPermissionResult(granted)
    }
    c.requestLocationPermission = {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        c.navigateAfterJobPosted(c.pendingNavJobId)
    }
    c.requestNotificationPermission = {
        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(Color.White.bg())
    ) {
        // Top Bar: Back button to Home/Hiring Room + 2-Tab Segmented Switch (Instant Need vs Regular Job) + Voice AI
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.bg()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedPostJobTab == 1 && c.currentStep > 1) {
                            c.currentStep = c.currentStep - 1
                        } else {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                (rootNavController ?: navController).navigate(Routes.EMPLOYER_DASHBOARD) {
                                    popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = PjInk.fg(),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFF1F5F9).bg())
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 0: Instant Need
                    val isInstant = selectedPostJobTab == 0
                    Surface(
                        onClick = { selectedPostJobTab = 0 },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isInstant) Color.White.bg() else Color.Transparent,
                        shadowElevation = if (isInstant) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.tab_instant_need),
                                fontSize = 13.sp,
                                fontWeight = if (isInstant) FontWeight.Bold else FontWeight.Medium,
                                color = if (isInstant) Color(0xFFDC2626).fg() else Color(0xFF64748B).fg()
                            )
                        }
                    }

                    // Tab 1: Regular Job
                    val isRegular = selectedPostJobTab == 1
                    Surface(
                        onClick = { selectedPostJobTab = 1 },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isRegular) Color.White.bg() else Color.Transparent,
                        shadowElevation = if (isRegular) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.tab_regular_job),
                                fontSize = 13.sp,
                                fontWeight = if (isRegular) FontWeight.Bold else FontWeight.Medium,
                                color = if (isRegular) Color(0xFF0F172A).fg() else Color(0xFF64748B).fg()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // DutyPe AI Voice Assistant Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A).bg())
                        .clickable {
                            (rootNavController ?: navController).navigate(Routes.dutypeAiRoute(listen = true))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Job Post",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (selectedPostJobTab == 0) {
                PostUrgentNeedContent(
                    viewModel = instantHelpViewModel,
                    onPosted = { requestId ->
                        navController.navigate(Routes.employerUrgentNeedDetailRoute(requestId)) {
                            popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    showTopBar = false,
                    bottomPadding = 12.dp,
                    navController = rootNavController ?: navController,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                var showAiAssistant by rememberSaveable { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxSize()) {
                    PostJobSideEffects(c)
                    PostJobDialogs(c)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Talk-to-post + "workers available nearby" (first step only).
                        if (c.currentStep == 1) {
                            com.example.dutype.employer.ai.AiPostBanner(
                                lat = c.locationLatitude,
                                lng = c.locationLongitude,
                                category = c.category.name,
                                onTalk = { showAiAssistant = true },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) { PostJobWizard(c) }
                    }
                    PostJobSheets(c)
                    if (showAiAssistant) {
                        com.example.dutype.employer.ai.AiPostAssistantSheet(
                            onDismiss = { showAiAssistant = false },
                            onApply = { draft ->
                                c.applyAiDraft(draft)
                                showAiAssistant = false
                                Toast.makeText(context, context.getString(R.string.ai_post_filled_toast), Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun PostJobSideEffects(c: PostJobController) {
    // Category is internal only; derived from the typed role/description.
    LaunchedEffect(c.title, c.description) {
        c.syncInferredCategory()
    }

    // Auto-save draft on field changes (debounced 2 seconds)
    LaunchedEffect(Unit) {
        c.draftTrigger
            .debounce(2000L)
            .collect { c.autoSaveDraft() }
    }
    PostJobDraftTriggerEffect(c)

    LaunchedEffect(Unit) {
        c.scope.launch { c.restoreDraft() }
    }
    LaunchedEffect(Unit) {
        c.scope.launch { c.loadEmployerProfile() }
    }
    LaunchedEffect(Unit) {
        c.clearLegacyStepRedirectFlag()
    }
}

@Composable
private fun PostJobDraftTriggerEffect(c: PostJobController) {
    LaunchedEffect(
        c.title,
        c.description,
        c.payAmount,
        c.payType,
        c.location,
        c.category,
        c.customCategory,
        c.vacancies,
        c.contactNumber,
        c.shift,
        c.employmentType,
        c.selectedPerks,
        c.experienceLevel,
        c.educationRequired,
        c.gender,
        c.repostOfJobId
    ) {
        c.draftTrigger.value = System.currentTimeMillis()
    }
}

// ------------------------------------------------------------------ dialogs
@Composable
private fun PostJobDialogs(c: PostJobController) {
    if (c.showLocationWarningDialog) {
        PostJobLocationWarningDialog(c)
    }
    val scam = c.scamValidationResult
    if (c.showScamWarningDialog && scam != null && !scam.isValid) {
        PostJobScamWarningDialog(c, scam.errorMessage)
    }
    val profileResult = c.profileCheckResult
    if (c.showProfileIncompleteDialog && profileResult != null) {
        PostJobProfileIncompleteDialog(c, profileResult.completionPercentage, profileResult.missingFields)
    }
    if (c.showNoCreditsDialog) {
        PostJobNoCreditsSheet(c)
    }
}

@Composable
private fun PostJobLocationWarningDialog(c: PostJobController) {
    AlertDialog(
        onDismissRequest = {
            c.showLocationWarningDialog = false
            c.pendingJobSubmission = false
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = EmployerColors.Error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.post_job_location_mismatch_title),
                fontWeight = FontWeight.Bold,
                color = EmployerColors.Error
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.post_job_location_mismatch_desc, String.format("%.1f", c.locationDistanceKm)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmployerColors.ErrorLight
                ) {
                    Text(
                        text = stringResource(R.string.post_job_location_mismatch_info),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmployerColors.Error
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
                    c.showLocationWarningDialog = false
                    c.pendingJobSubmission = true
                    c.submitJob()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmployerColors.Error
                )
            ) {
                Text(stringResource(R.string.post_anyway))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    c.showLocationWarningDialog = false
                    c.pendingJobSubmission = false
                }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun PostJobScamWarningDialog(c: PostJobController, errorMessage: String?) {
    AlertDialog(
        onDismissRequest = { c.showScamWarningDialog = false },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = EmployerColors.Error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.post_job_blocked_title),
                fontWeight = FontWeight.Bold,
                color = EmployerColors.Error
            )
        },
        text = {
            Column {
                Text(
                    text = errorMessage ?: "This job posting contains suspicious content.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmployerColors.ErrorLight
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.post_job_blocked_info),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = EmployerColors.Error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.post_job_blocked_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = EmployerColors.Error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { c.showScamWarningDialog = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmployerColors.Primary
                )
            ) {
                Text(stringResource(R.string.edit_job_details))
            }
        }
    )
}

@Composable
private fun PostJobProfileIncompleteDialog(
    c: PostJobController,
    completionPercentage: Any,
    missingFields: List<Any?>
) {
    AlertDialog(
        onDismissRequest = { c.showProfileIncompleteDialog = false },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = EmployerColors.Warning,
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
                    stringResource(R.string.post_job_profile_progress, completionPercentage),
                    fontWeight = FontWeight.Medium,
                    color = EmployerColors.Warning,
                    textAlign = TextAlign.Center
                )
                if (missingFields.isNotEmpty()) {
                    Text(
                        stringResource(R.string.post_job_missing_fields, missingFields.take(3).joinToString(", ") + if (missingFields.size > 3) "..." else ""),
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
                    c.showProfileIncompleteDialog = false
                    c.openProfileSetup()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmployerColors.Primary
                )
            ) {
                Text(stringResource(R.string.complete_profile_button))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { c.showProfileIncompleteDialog = false }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostJobNoCreditsSheet(c: PostJobController) {
    ModalBottomSheet(
        onDismissRequest = {
            c.showNoCreditsDialog = false
            c.navController.popBackStack()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White.bg()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(EmployerColors.Primary.copy(alpha = 0.1f), CircleShape)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = EmployerColors.Primary.fg(),
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = stringResource(R.string.auto_unlock_more_hires),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = EmployerColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.auto_you_ve_used_all_your_job_posts_get_a_subsc),
                style = MaterialTheme.typography.bodyMedium,
                color = EmployerColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    c.showNoCreditsDialog = false
                    c.openSubscription()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text("View Plans", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            TextButton(
                onClick = {
                    c.showNoCreditsDialog = false
                    c.navController.popBackStack()
                }
            ) {
                Text("Maybe Later", color = EmployerColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun PostJobSheets(c: PostJobController) {
    com.example.dutype.components.LoginBottomSheet(
        isVisible = c.showLoginBottomSheet,
        onDismiss = { c.showLoginBottomSheet = false },
        onLoginSuccess = {
            c.showLoginBottomSheet = false
            if (c.currentStep == 3) {
                c.submitJob()
            } else if (c.currentStep == 1) {
                c.currentStep = 2
            }
        },
        onProfileSetupRequired = {
            c.showLoginBottomSheet = false
            c.openProfileSetup()
        },
        requiresProfileCheck = true,
        role = com.example.dutype.models.UserRole.EMPLOYER,
        title = stringResource(R.string.login_to_post_job),
        subtitle = stringResource(R.string.login_publish_job_subtitle),
        navController = c.rootNavController ?: c.navController
    )

    com.example.dutype.components.NotificationPermissionBottomSheet(
        isVisible = c.showPostJobNotificationBottomSheet,
        onDismiss = {
            c.showPostJobNotificationBottomSheet = false
            c.navigateAfterJobPosted(c.pendingNavJobId)
        },
        onEnableNotifications = {
            c.showPostJobNotificationBottomSheet = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                c.requestNotificationPermission()
            } else {
                c.navigateAfterJobPosted(c.pendingNavJobId)
            }
        },
        userRole = "employer"
    )
}

// ------------------------------------------------------------------- wizard
@Composable
private fun PostJobWizard(c: PostJobController) {
    val currentStep = c.currentStep
    val wizardScrollState = rememberScrollState()
    LaunchedEffect(currentStep) {
        wizardScrollState.scrollTo(0)
    }
    androidx.activity.compose.BackHandler(enabled = currentStep > 1) {
        c.currentStep = c.currentStep - 1
    }

    val employerJobUiState by c.employerJobViewModel.uiState.collectAsState()
    val ctaBusy = employerJobUiState.isCreatingJob || c.isSubmittingJob

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.bg())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            PostJobOfflineBanner()
            PostJobWizardBody(c, currentStep, wizardScrollState)
        }

        PostJobBottomCta(c, currentStep, ctaBusy)
    }
}

@Composable
private fun PostJobOfflineBanner() {
    val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
    val isOnline by connectivityViewModel.isOnline.collectAsState()
    com.example.dutype.components.OfflineBanner(isOffline = !isOnline)
}

@Composable
private fun PostJobWizardBody(
    c: PostJobController,
    currentStep: Int,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            // Room for the fixed Next / Post button bar (80dp + the phone's navigation bar) and a
            // little breathing space, so the last field is never hidden behind it.
            .navigationBarsPadding()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 112.dp)
    ) {
        PjProgressRow(currentStep = currentStep, totalSteps = PostJobTotalSteps)
        Text(
            text = postJobStepTitle(currentStep),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = PjInk.fg()
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (currentStep == 1) {
            PostJobStep1(c)
        } else if (currentStep == 2) {
            PostJobStep2(c)
        } else {
            PostJobStep3(c)
        }
    }
}

@Composable
private fun BoxScope.PostJobBottomCta(c: PostJobController, currentStep: Int, ctaBusy: Boolean) {
    val ctaEnabled = if (currentStep == 3) (!ctaBusy && !c.isLoadingLocation) else true
    val ctaLabel = if (currentStep == 3) stringResource(R.string.post_job) + " →" else "Next →"
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Color.White.bg())
            .navigationBarsPadding()
            .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp)
    ) {
        Button(
            onClick = { c.goNext() },
            enabled = ctaEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PjInk.bg(),
                contentColor = Color.White,
                disabledContainerColor = PjInk.bg().copy(alpha = 0.5f),
                disabledContentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
        ) {
            if (currentStep == 3 && ctaBusy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    text = ctaLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

// ---------------------------------------------------------------- step 1
@Composable
private fun PostJobStep1(c: PostJobController) {
    PjField(
        value = c.title,
        onValueChange = {
            c.title = it
            c.customCategory = it.trim()
        },
        label = stringResource(R.string.post_job_title_label),
        placeholder = stringResource(R.string.job_title_placeholder)
    )
    Spacer(modifier = Modifier.height(10.dp))
    PostJobQuickTitleChips(c)

    PjSectionLabel("Workers needed")
    PjCountStepper(
        count = c.vacancies.toIntOrNull()?.coerceIn(1, 50) ?: 1,
        onCountChange = { c.vacancies = it.toString() },
        min = 1,
        max = 50
    )

    PjSectionLabel("Category")
    PostJobCategoryChips(c)

    PjSectionLabel("Description")
    PjField(
        value = c.description,
        onValueChange = { c.description = it },
        label = "Job Description",
        placeholder = "Describe the work, timings, what you expect",
        singleLine = false,
        minHeight = 96.dp
    )
    if (c.description.isNotEmpty() && c.description.trim().length < MIN_DESCRIPTION_CHARS) {
        Text(
            text = stringResource(R.string.post_job_description_required),
            fontSize = 11.sp,
            color = EmployerColors.Error,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    PjSectionLabel("Photo (optional)")
    JobImageUploadSection(
        selectedImageUri = c.jobImageUri,
        isUploading = c.isUploadingJobImage,
        onImageSelected = { uri -> c.onJobImageSelected(uri) },
        onImageRemoved = { c.onJobImageRemoved() }
    )

    val experienceLevels = remember(c.experienceLevel) {
        postJobWithCustomOption(PostJobBaseExperienceLevels, c.experienceLevel)
    }
    PjOptionGroup(
        label = stringResource(R.string.experience_required),
        options = experienceLevels,
        selected = c.experienceLevel,
        onSelect = { c.experienceLevel = it },
        allowCustom = true,
        customHint = stringResource(R.string.add_own_experience)
    )
}

@Composable
private fun PostJobQuickTitleChips(c: PostJobController) {
    PjFlow {
        PostJobQuickTitles.forEach { quick ->
            PjChip(
                label = quick,
                selected = c.title.trim().equals(quick, ignoreCase = true),
                onClick = {
                    c.title = quick
                    c.customCategory = quick
                },
                height = 32.dp,
                fontSize = 12.sp,
                cornerRadius = 16.dp
            )
        }
    }
}

@Composable
private fun PostJobCategoryChips(c: PostJobController) {
    val categoryChips = postJobCategoryChipsFor(c.category)
    PjFlow {
        categoryChips.forEach { cat ->
            PjChip(
                label = cat.displayName,
                selected = c.category == cat,
                onClick = { c.category = cat }
            )
        }
    }
}

// ---------------------------------------------------------------- step 2
@Composable
private fun PostJobStep2(c: PostJobController) {
    val savedWorkLocations by c.employerJobViewModel.savedWorkLocationsStore.locations.collectAsState()
    EnhancedLocationSection(
        location = c.location,
        onLocationChange = { c.location = it },
        locationService = c.locationService,
        isLoadingLocation = c.isLoadingLocation,
        locationError = c.locationError,
        onLocationButtonClick = { c.onLocationButtonClick() },
        onLocationSelected = { lat, lon -> c.onLocationSelected(lat, lon) },
        locationLatitude = c.locationLatitude,
        locationLongitude = c.locationLongitude,
        savedLocations = savedWorkLocations
    )

    PjSectionLabel("Start date")
    PjFlow {
        PostJobStartDateOptions.forEach { opt ->
            PjChip(
                label = opt,
                selected = c.startDate == opt,
                onClick = { c.startDate = opt },
                leadingIcon = Icons.Default.CalendarToday
            )
        }
    }

    PjSectionLabel("Shift")
    PjFlow {
        JobShift.entries.forEach { option ->
            PjChip(
                label = option.displayName,
                selected = c.shift == option,
                onClick = { c.shift = option }
            )
        }
    }

    PjSectionLabel("Job type")
    PjFlow {
        com.example.dutype.utils.PayRules.VACANCY_EMPLOYMENT_TYPES.forEach { type ->
            PjChip(
                label = type.displayName,
                selected = c.employmentType == type,
                onClick = { c.employmentType = type }
            )
        }
    }
}

// ---------------------------------------------------------------- step 3
@Composable
private fun PostJobStep3(c: PostJobController) {
    PjSectionLabel("Wage", firstOnPage = true)
    PjWageField(
        amount = c.payAmount,
        onAmountChange = { c.payAmount = it },
        payType = c.payType,
        onPayTypeChange = { c.payType = it }
    )
    if ((c.payAmount.toLongOrNull() ?: 0L) > com.example.dutype.utils.SalaryFormatter.MAX_PAY_RUPEES) {
        Text(
            text = stringResource(R.string.pay_max_limit),
            fontSize = 11.sp,
            color = EmployerColors.Error,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val amountValue = c.payAmount.toLongOrNull() ?: 0L
    val payError = com.example.dutype.utils.PayRules.vacancyError(context, c.payType, c.employmentType, amountValue)
    if (payError != null) {
        Text(text = payError, fontSize = 12.sp, color = EmployerColors.Error, modifier = Modifier.padding(top = 4.dp))
    } else if (com.example.dutype.utils.PayRules.minimumWageHint(c.payType, c.employmentType, amountValue)) {
        Text(text = stringResource(R.string.pay_rule_min_wage_hint), fontSize = 12.sp, color = Color(0xFFB45309), modifier = Modifier.padding(top = 4.dp))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = stringResource(R.string.pay_rule_vacancy_info), fontSize = 12.sp, color = PjSlate400.fg(), lineHeight = 16.sp)

    PjSectionLabel("Perks")
    PostJobPerkChips(c)

    val educationRequirements = remember(c.educationRequired) {
        postJobWithCustomOption(PostJobBaseEducationRequirements, c.educationRequired)
    }
    PjOptionGroup(
        label = stringResource(R.string.education_required),
        options = educationRequirements,
        selected = c.educationRequired,
        onSelect = { c.educationRequired = it },
        allowCustom = true,
        customHint = "Add education requirement"
    )

    PjSectionLabel(stringResource(R.string.post_job_gender_preference))
    PjFlow {
        PostJobGenders.forEach { g ->
            PjChip(
                label = g,
                selected = c.gender == g,
                onClick = { c.gender = g }
            )
        }
    }

    PjSectionLabel("Contact")
    PjField(
        value = c.contactNumber,
        onValueChange = { c.contactNumber = it },
        label = stringResource(R.string.contact_number_label),
        placeholder = "+91 9876543210",
        keyboardType = KeyboardType.Phone
    )
    Spacer(modifier = Modifier.height(10.dp))
    PjField(
        value = c.employerName,
        onValueChange = { c.employerName = it },
        label = stringResource(R.string.your_name),
        placeholder = stringResource(R.string.enter_your_name)
    )
    // Asked only here, when hiring: saved to the employer profile so it is never asked again.
    Spacer(modifier = Modifier.height(10.dp))
    PjField(
        value = c.companyName,
        onValueChange = { c.companyName = it.take(80) },
        label = stringResource(R.string.post_job_business_name),
        placeholder = stringResource(R.string.post_job_business_name_hint)
    )
}

@Composable
private fun PostJobExtraPayTypeChips(c: PostJobController) {
    PjFlow {
        PostJobExtraPayTypes.forEach { type ->
            PjChip(
                label = stringResource(type.labelRes),
                selected = c.payType == type,
                onClick = { c.payType = type },
                height = 32.dp,
                fontSize = 12.sp,
                cornerRadius = 16.dp
            )
        }
    }
}

@Composable
private fun PostJobPerkChips(c: PostJobController) {
    var customPerkInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        val allPerkOptions = remember(c.selectedPerks) {
            val combined = PostJobPerkOptions.toMutableList()
            c.selectedPerks.forEach { perk ->
                if (perk !in combined) combined.add(perk)
            }
            combined
        }

        PjFlow {
            allPerkOptions.forEach { perk ->
                val on = perk in c.selectedPerks
                val isCustom = perk !in PostJobPerkOptions
                PjChip(
                    label = if (isCustom) "$perk ✕" else perk,
                    selected = on,
                    onClick = {
                        c.selectedPerks = if (on) c.selectedPerks - perk else c.selectedPerks + perk
                    },
                    selectedFill = Color(0xFF0F0F0F).bg(),
                    showCheck = !isCustom
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Custom perk input: employers can type custom benefits matching their mindset
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customPerkInput,
                onValueChange = { if (it.length <= 50) customPerkInput = it },
                placeholder = {
                    Text(
                        "Add custom perk (e.g. Tea & snacks, Daily incentive)",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A)),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.bg(),
                    unfocusedContainerColor = Color.White.bg(),
                    focusedBorderColor = Color(0xFF2563EB).bd(),
                    unfocusedBorderColor = Color(0xFFCBD5E1).bd()
                )
            )

            Spacer(Modifier.width(8.dp))

            Surface(
                modifier = Modifier
                    .height(48.dp)
                    .clickable(
                        enabled = customPerkInput.trim().isNotBlank()
                    ) {
                        val trimmed = customPerkInput.trim()
                        if (trimmed.isNotBlank()) {
                            c.selectedPerks = c.selectedPerks + trimmed
                            customPerkInput = ""
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (customPerkInput.trim().isNotBlank()) Color(0xFF0F172A) else Color(0xFFE2E8F0)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Add",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (customPerkInput.trim().isNotBlank()) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
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

@Composable
fun EnhancedLocationSection(
    location: String,
    onLocationChange: (String) -> Unit,
    locationService: com.example.dutype.utils.LocationService,
    isLoadingLocation: Boolean,
    locationError: String?,
    onLocationButtonClick: () -> Unit,
    onLocationSelected: ((Double, Double) -> Unit)? = null,
    locationLatitude: Double = 0.0,
    locationLongitude: Double = 0.0,
    savedLocations: List<com.example.dutype.models.WorkLocation> = emptyList()
) {
    val primaryBlue = EmployerColors.Primary
    val successGreen = EmployerColors.Success

    // Location search state
    var isSearching by remember { mutableStateOf(false) }
    var isResolvingPinnedAddress by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var selectedLocationText by remember { mutableStateOf("") }
    var pendingPinnedLocation by remember { mutableStateOf<LatLng?>(null) }

    // Search for locations when user types
    LaunchedEffect(location, showSuggestions, selectedLocationText) {
        if (location.length >= 3 && !isLoadingLocation && showSuggestions && location != selectedLocationText) {
            kotlinx.coroutines.delay(500) // Debounce
            isSearching = true
            try {
                searchSuggestions = locationService.searchPlaces(location, maxResults = 5)
                    .map { suggestion ->
                        LocationSuggestion(
                            placeId = suggestion.placeId,
                            displayName = suggestion.description,
                            city = "",
                            state = "",
                            country = "India",
                            area = suggestion.description.substringBefore(",").trim(),
                            latitude = suggestion.latitude,
                            longitude = suggestion.longitude
                        )
                    }
            } catch (e: Exception) {
                searchSuggestions = emptyList()
            } finally {
                isSearching = false
            }
        } else {
            searchSuggestions = emptyList()
            showSuggestions = false
        }
    }

    LaunchedEffect(pendingPinnedLocation) {
        val pinnedLocation = pendingPinnedLocation ?: return@LaunchedEffect
        kotlinx.coroutines.delay(350)
        isResolvingPinnedAddress = true
        try {
            val resolvedLocation = locationService.getLocationFromCoordinates(
                pinnedLocation.latitude,
                pinnedLocation.longitude
            )
            val resolvedAddress = resolvedLocation
                ?.getFullAddress()
                ?.takeIf { it.isNotBlank() }
                ?: String.format("%.6f, %.6f", pinnedLocation.latitude, pinnedLocation.longitude)
            selectedLocationText = resolvedAddress
            onLocationChange(resolvedAddress)
        } finally {
            isResolvingPinnedAddress = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Saved work locations quick-pick
        if (savedLocations.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                savedLocations.take(5).forEach { loc ->
                    PjChip(
                        label = loc.label.ifBlank { loc.address.take(25) },
                        selected = location == loc.address,
                        onClick = {
                            showSuggestions = false
                            searchSuggestions = emptyList()
                            onLocationChange(loc.address)
                            onLocationSelected?.invoke(loc.latitude, loc.longitude)
                        },
                        height = 32.dp,
                        fontSize = 12.sp,
                        cornerRadius = 16.dp,
                        leadingIcon = Icons.Default.LocationOn
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        PjField(
            value = location,
            onValueChange = {
                selectedLocationText = ""
                onLocationChange(it)
                showSuggestions = true
            },
            label = "Job Location",
            placeholder = stringResource(R.string.location_search_placeholder),
            singleLine = false,
            leading = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = PjInk.fg()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PjSlate500.fg(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            trailing = {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = PjBlue.fg()
                    )
                } else {
                    Text(
                        text = "Detect",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PjBlue.fg(),
                        modifier = Modifier.clickable {
                            showSuggestions = false
                            searchSuggestions = emptyList()
                            onLocationButtonClick()
                        }
                    )
                }
            }
        )

        // Location search suggestions dropdown (flat)
        if (showSuggestions && searchSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.bg(), RoundedCornerShape(12.dp))
                    .border(1.dp, PjLine.bd(), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                searchSuggestions.take(5).forEach { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLocationText = suggestion.displayName
                                showSuggestions = false
                                searchSuggestions = emptyList()
                                onLocationChange(suggestion.displayName)
                                onLocationSelected?.invoke(suggestion.latitude, suggestion.longitude)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PjSlate500.fg(),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = suggestion.area.ifBlank { suggestion.city },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PjInk.fg(),
                                maxLines = 1
                            )
                            Text(
                                text = suggestion.displayName,
                                fontSize = 12.sp,
                                color = PjSlate500.fg(),
                                maxLines = 2
                            )
                        }
                    }
                    if (suggestion != searchSuggestions.take(5).last()) {
                        Divider(color = PjLine.bd(), thickness = 1.dp)
                    }
                }
            }
        }

        if (locationError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Warning: $locationError",
                color = EmployerColors.Error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RequirementsSection(
    experienceLevel: String,
    onExperienceLevelChange: (String) -> Unit,
    experienceLevels: List<String>,
    educationRequired: String,
    onEducationRequiredChange: (String) -> Unit,
    educationRequirements: List<String>,
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
                        .background(EmployerColors.InfoLight, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED).fg(),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.post_job_requirements),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = EmployerColors.TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.post_job_looking_for),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmployerColors.TextSecondary
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
                selectedColor = EmployerColors.Success,
                allowCustomOption = true,
                customOptionHint = stringResource(R.string.add_own_experience)
            )

            Spacer(modifier = Modifier.height(18.dp))

            RequirementChipSection(
                title = stringResource(R.string.education_required),
                icon = Icons.Default.School,
                options = educationRequirements,
                selectedOption = educationRequired,
                onOptionSelected = onEducationRequiredChange,
                selectedColor = Color(0xFF7C3AED).fg(),
                allowCustomOption = true,
                customOptionHint = "Add education requirement"
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Gender Preference
            Text(
                text = stringResource(R.string.post_job_gender_preference),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = EmployerColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(genders) { genderOption ->
                    FilterChip(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .height(44.dp),
                        onClick = { onGenderChange(genderOption) },
                        label = {
                            Text(
                                genderOption,
                                fontWeight = if (gender == genderOption) FontWeight.Medium else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        selected = gender == genderOption,
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6D28D9).bg(),
                            selectedLabelColor = Color.White,
                            containerColor = EmployerColors.ChipBackground,
                            labelColor = EmployerColors.TextSecondary
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
                tint = EmployerColors.TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = EmployerColors.TextSecondary
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
                    .height(44.dp),
                onClick = { onOptionSelected(option) },
                label = {
                    Text(
                        option,
                        fontWeight = if (selectedOption == option) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                selected = selectedOption == option,
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedColor,
                    selectedLabelColor = Color.White,
                    containerColor = EmployerColors.ChipBackground,
                    labelColor = EmployerColors.TextSecondary
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
                        .height(44.dp),
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
                        selectedContainerColor = EmployerColors.ChipBackground,
                        selectedLabelColor = EmployerColors.TextSecondary,
                        containerColor = EmployerColors.ChipBackground,
                        labelColor = EmployerColors.TextSecondary
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
                    unfocusedBorderColor = EmployerColors.Border,
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

// ===========================================================================
// Apr 2026: Apna-style 3-step wizard helpers for the Post Job screen.
// Top stepper indicator + dual-action bottom bar (Back / Next / Post job).
// Kept minimalist on purpose: no shadows, single accent color, tight type.
// ===========================================================================

// ===========================================================================
// Flat 3-step wizard primitives for the Post Job screen (no shadows / gradients).
// ===========================================================================
private val PjInk = Color(0xFF0F172A)
private val PjLine = Color(0xFFE2E8F0)
private val PjSlate400 = Color(0xFF94A3B8)
private val PjSlate500 = Color(0xFF64748B)
private val PjSegmentBg = Color(0xFFF1F5F9)
private val PjGreen = Color(0xFF10B981)
private val PjBlue = Color(0xFF2563EB)

@Composable
private fun PjProgressRow(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            val done = currentStep > i
            val active = currentStep == i
            val circleModifier = when {
                done -> Modifier
                    .size(28.dp)
                    .background(Color.White.bg(), CircleShape)
                    .border(2.dp, PjInk.fg(), CircleShape)
                active -> Modifier
                    .size(28.dp)
                    .background(PjInk.bg(), CircleShape)
                else -> Modifier
                    .size(28.dp)
                    .background(PjLine.bg(), CircleShape)
            }
            Box(modifier = circleModifier, contentAlignment = Alignment.Center) {
                Text(
                    text = if (done) "✓" else i.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        done -> PjInk.fg()
                        active -> Color.White
                        else -> PjSlate400.fg()
                    }
                )
            }
            if (i < totalSteps) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp, end = 6.dp)
                        .height(2.dp)
                        .background(if (currentStep > i) PjInk.fg() else PjLine.bd())
                )
            }
        }
    }
}

@Composable
private fun PjSectionLabel(text: String, firstOnPage: Boolean = false) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = PjSlate500.fg(),
        modifier = Modifier.padding(top = if (firstOnPage) 0.dp else 26.dp, bottom = 10.dp)
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PjFlow(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun PjChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    height: Dp = 36.dp,
    fontSize: TextUnit = 13.sp,
    cornerRadius: Dp = 18.dp,
    selectedFill: Color = PjInk,
    showCheck: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(cornerRadius)
    val textColor = if (selected) Color.White else PjInk.fg()
    Row(
        modifier = Modifier
            .height(height)
            .clip(shape)
            .background(if (selected) selectedFill.bg() else Color.White.bg())
            .border(1.dp, if (selected) selectedFill.bg() else PjLine.bd(), shape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected && showCheck) {
            Text(
                text = "✓",
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = PjGreen.fg()
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun PjField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 56.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> EmployerColors.Error
        focused -> PjInk.fg()
        else -> PjLine.bd()
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(Color.White.bg(), shape)
            .border(1.dp, borderColor, shape)
            .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = PjSlate400.fg()
            )
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        color = PjSlate400.fg(),
                        maxLines = if (singleLine) 1 else 2
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PjInk.fg()
                    ),
                    cursorBrush = SolidColor(PjInk.fg()),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
private fun PjCountStepper(
    count: Int,
    onCountChange: (Int) -> Unit,
    min: Int,
    max: Int
) {
    val shape = RoundedCornerShape(12.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(shape)
                .border(1.dp, PjLine.bd(), shape)
                .clickable { if (count > min) onCountChange(count - 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("−", fontSize = 18.sp, color = PjInk.fg())
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.widthIn(min = 24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PjInk.fg(),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(shape)
                .border(1.dp, PjLine.bd(), shape)
                .clickable { if (count < max) onCountChange(count + 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 18.sp, color = PjInk.fg())
        }
    }
}

@Composable
private fun PjSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PjInk.bg() else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else PjSlate500.fg(),
            maxLines = 1
        )
    }
}

@Composable
private fun PjWageField(
    amount: String,
    onAmountChange: (String) -> Unit,
    payType: PayType,
    onPayTypeChange: (PayType) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White.bg(), shape)
            .border(1.dp, PjLine.bd(), shape)
            .padding(start = 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("₹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PjInk.fg())
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (amount.isEmpty()) {
                Text("Enter amount", fontSize = 14.sp, color = PjSlate400.fg(), maxLines = 1)
            }
            BasicTextField(
                value = amount,
                // Whole rupees only; one digit past the limit is let through so the warning shows.
                onValueChange = { typed -> onAmountChange(typed.filter { it.isDigit() }.take(6)) },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PjInk.fg()
                ),
                cursorBrush = SolidColor(PjInk.fg()),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PjSegmentBg.bg())
                .padding(3.dp)
        ) {
            PjSegment(label = stringResource(R.string.per_week), selected = payType == PayType.WEEKLY, onClick = { onPayTypeChange(PayType.WEEKLY) })
            PjSegment(label = stringResource(R.string.per_month), selected = payType == PayType.MONTHLY, onClick = { onPayTypeChange(PayType.MONTHLY) })
        }
    }
}

@Composable
private fun PjOptionGroup(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    allowCustom: Boolean = false,
    customHint: String = "Add your own"
) {
    var showCustom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    PjSectionLabel(label)
    PjFlow {
        options.forEach { option ->
            PjChip(
                label = option,
                selected = selected == option,
                onClick = { onSelect(option) }
            )
        }
        if (allowCustom) {
            PjChip(
                label = if (showCustom) "Cancel" else "Add your own +",
                selected = false,
                onClick = { showCustom = !showCustom }
            )
        }
    }
    if (allowCustom && showCustom) {
        Spacer(modifier = Modifier.height(10.dp))
        PjField(
            value = customText,
            onValueChange = { customText = it },
            label = customHint,
            trailing = {
                Text(
                    text = "Add",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PjBlue.fg(),
                    modifier = Modifier.clickable {
                        val trimmed = customText.trim()
                        if (trimmed.isNotBlank()) {
                            val existing = options.firstOrNull { it.equals(trimmed, ignoreCase = true) }
                            onSelect(existing ?: trimmed)
                            customText = ""
                            showCustom = false
                        }
                    }
                )
            }
        )
    }
}
