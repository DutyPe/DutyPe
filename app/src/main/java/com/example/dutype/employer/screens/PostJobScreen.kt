package com.example.dutype.employer.screens

import com.dutype.app.R
import android.Manifest
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
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.employer.models.PayType
import com.example.dutype.employer.models.ShiftTiming
import com.example.dutype.location.LocationSuggestion
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.utils.JobValidationUtils
import com.example.dutype.utils.PayRateValidationResult
import com.example.dutype.utils.ValidationResult
import com.example.dutype.utils.findActivity
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
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

private val PostJobWorkTypes = listOf("Part-time", "Full-time", "Contract", "Temporary", "Weekend Only", "Student-friendly")
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
private val PostJobShiftChoices = listOf(
    ShiftTiming.MORNING to "Day Shift",
    ShiftTiming.NIGHT to "Night Shift",
    ShiftTiming.BOTH to "Both Shifts",
    ShiftTiming.FLEXIBLE to "Flexible"
)
private val PostJobPerkOptions = listOf("Food Provided", "Transport", "Overtime Bonus", "Accommodation")
private val PostJobExtraPayTypes = listOf(PayType.WEEKLY, PayType.HOURLY, PayType.TASK)

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
    val jobViewModel: com.example.dutype.viewmodels.FirestoreJobViewModel,
    val employerJobViewModel: FirestoreEmployerJobViewModel,
    val scope: CoroutineScope,
    private val stepState: MutableState<Int>
) {
    var rootNavController: NavController? = null
    var employerId: String? = null
    var onJobPosted: ((String?) -> Unit)? = null
    var onReviewTrigger: (() -> Unit)? = null
    var requestLocationPermission: () -> Unit = {}
    var requestNotificationPermission: () -> Unit = {}

    val locationService get() = jobViewModel.locationService
    private val profileCompletionService get() = jobViewModel.profileCompletionService
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
    var payType by mutableStateOf(PayType.DAILY)
    var location by mutableStateOf("")
    var description by mutableStateOf("")
    var contactNumber by mutableStateOf("")
    var category by mutableStateOf(JobCategory.OTHER)
    var customCategory by mutableStateOf("")
    var shiftTiming by mutableStateOf(ShiftTiming.FLEXIBLE)
    var customShiftStart by mutableStateOf("")
    var customShiftEnd by mutableStateOf("")
    var vacancies by mutableStateOf("")
    var employerName by mutableStateOf("")
    var companyName by mutableStateOf("")
    var employerType by mutableStateOf("COMPANY")
    var workType by mutableStateOf("Full-time")
    var experienceLevel by mutableStateOf("No Experience Required")
    var educationRequired by mutableStateOf("No qualification required")
    var gender by mutableStateOf("Both")
    var startDate by mutableStateOf("Immediately")
    var selectedPerks by mutableStateOf(setOf<String>())
    var repostOfJobId by mutableStateOf("")
    var employerTrustTier by mutableStateOf("VERIFIED")
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
            shiftTiming != ShiftTiming.FLEXIBLE ||
            workType != "Full-time" ||
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
            shiftTiming = shiftTiming,
            workType = workType,
            experienceLevel = experienceLevel,
            educationRequired = educationRequired,
            gender = gender,
            requirements = "",
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
                workType = savedDraft.workType
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
                if (cachedProfile.trustTier.isNotBlank()) {
                    employerTrustTier = cachedProfile.trustTier
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
        val db = com.example.dutype.di.firestoreFromHilt(context)
        val employerDoc = db.collection(com.example.dutype.firestore.FirestoreCollections.EMPLOYER_PROFILES).document(currentUser.uid).get().await()
        val phoneRoleDoc = currentUser.phoneNumber
            ?.let(com.example.dutype.utils.PhoneNumberUtils::normalize)
            ?.takeIf { it.isNotBlank() }
            ?.let { db.collection(com.example.dutype.firestore.FirestoreCollections.PHONE_ROLES).document(it).get().await() }

        if (employerDoc.exists() || phoneRoleDoc?.exists() == true) {
            val savedFullName = employerDoc.getString("fullName") ?: phoneRoleDoc?.getString("name")
            if (!savedFullName.isNullOrBlank()) {
                employerName = savedFullName
            }
            val savedContactPhone = employerDoc.getString("phone") ?: phoneRoleDoc?.getString("phoneNumber")
            if (!savedContactPhone.isNullOrBlank()) {
                contactNumber = savedContactPhone
            }
            val savedEmployerType = employerDoc.getString("employerType")
                ?: phoneRoleDoc?.getString("employerType")
                ?: "COMPANY"
            employerType = savedEmployerType
        }
        if (employerDoc.exists()) {
            val savedCompanyName = employerDoc.getString("companyName")
                ?: employerDoc.getString("fullName")
            if (!savedCompanyName.isNullOrBlank()) {
                companyName = savedCompanyName
            }
        }
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
        if (cachedLocation != null) {
            location = cachedLocation.getFullAddress()
            locationLatitude = cachedLocation.latitude
            locationLongitude = cachedLocation.longitude
            Timber.d("LOCATION DEBUG: Using recent cached location immediately")
        }

        val refinedLocation = locationRepository.getHighAccuracy(
            timeoutMs = if (cachedLocation != null) 4000L else 6000L,
            minAccuracyMeters = 35f
        )

        if (refinedLocation != null) {
            location = refinedLocation.getFullAddress()
            locationLatitude = refinedLocation.latitude
            locationLongitude = refinedLocation.longitude
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
                if (title.isBlank()) return false
                val scamCheck = JobValidationUtils.validateAgainstScamKeywords(title, description)
                scamValidationResult = scamCheck
                scamCheck.isValid
            }
            2 -> {
                if (payAmount.isBlank() || location.isBlank()) return false
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
    private fun createJobPosting(): JobPostingModel {
        return JobPostingModel(
            title = title,
            payAmount = payAmount,
            payType = payType,
            location = location,
            description = description,
            contactNumber = contactNumber,
            category = category,
            shiftTiming = shiftTiming,
            vacancies = vacancies.toIntOrNull() ?: 0,
            employerId = employerId ?: "",
            postedTime = System.currentTimeMillis()
        )
    }

    private fun shiftTimingText(): String {
        return if (shiftTiming == ShiftTiming.CUSTOM &&
            (customShiftStart.isNotBlank() || customShiftEnd.isNotBlank())
        ) {
            listOf(customShiftStart.trim(), customShiftEnd.trim())
                .filter { it.isNotBlank() }
                .joinToString(separator = " - ")
        } else {
            shiftTiming.displayName
        }
    }

    private fun buildDescriptionText(jobPosting: JobPostingModel): String {
        val baseDescriptionText = jobPosting.description.ifBlank {
            buildString {
                append("Hiring for ")
                append(jobPosting.title.trim())
                append(".")
                if (shiftTiming.displayName.isNotBlank()) {
                    append(" Shift: ")
                    append(shiftTiming.displayName)
                    append(".")
                }
            }
        }
        return buildString {
            append(baseDescriptionText)
            if (startDate != "Immediately") {
                append("\n\nStart: ")
                append(startDate)
            }
            if (selectedPerks.isNotEmpty()) {
                append("\n\nPerks: ")
                append(selectedPerks.joinToString(", "))
            }
        }
    }

    private fun buildJobData(
        jobPosting: JobPostingModel,
        finalLatitude: Double,
        finalLongitude: Double,
        vacancyCount: Int
    ): MutableMap<String, Any> {
        // salary is a String: the employer's exact text is sent verbatim.
        val payParsed = com.example.dutype.utils.PayAmountParser.parse(jobPosting.payAmount)
        val descriptionWithPayText = buildDescriptionText(jobPosting)
        val inferredCategoryName = com.example.dutype.utils.JobCategoryResolver.inferCategoryName(
            title = jobPosting.title,
            description = descriptionWithPayText
        )
        val locationMap: Map<String, Double> = mapOf("lat" to finalLatitude, "lng" to finalLongitude)
        val jobData = mutableMapOf<String, Any>()
        jobData["title"] = jobPosting.title
        jobData["jobType"] = workType
        jobData["category"] = inferredCategoryName
        jobData["location"] = locationMap
        jobData["addressText"] = jobPosting.location
        jobData["salary"] = payParsed.text
        jobData["salaryType"] = jobPosting.payType.name
        jobData["description"] = descriptionWithPayText
        jobData["gender"] = gender
        jobData["experienceRequired"] = experienceLevel
        jobData["educationRequired"] = educationRequired
        jobData["shiftTiming"] = shiftTimingText()
        jobData["vacancies"] = vacancyCount
        jobData["contactNumber"] = jobPosting.contactNumber
        jobData["employerId"] = employerId ?: ""
        jobData["jobImageUrl"] = jobImageUrl
        if (repostOfJobId.isNotBlank()) {
            jobData["repostOfJobId"] = repostOfJobId
            jobData["repostedAt"] = System.currentTimeMillis()
        }
        return jobData
    }

    private fun submitJobWithCoordinates(finalLatitude: Double, finalLongitude: Double) {
        if (isCreatingJob) {
            Timber.w("JOB POSTING DEBUG: Already creating job in submitJobWithCoordinates, ignoring")
            return
        }

        Timber.d("JOB POSTING DEBUG: Creating job posting...")
        val jobPosting = createJobPosting()
        val vacancyCount = vacancies.toIntOrNull()
        if (vacancyCount == null || vacancyCount !in 1..50) {
            Toast.makeText(context, context.getString(R.string.enter_number_positions), Toast.LENGTH_SHORT).show()
            isSubmittingJob = false
            return
        }

        val jobData = buildJobData(jobPosting, finalLatitude, finalLongitude, vacancyCount)
        employerJobViewModel.createJob(jobData) { success, newJobId, message ->
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
        val profileResult = profileCompletionService.getEmployerProfileData(uid)
        profileResult.onSuccess { profileData ->
            val savedName = (profileData["companyName"] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (profileData["fullName"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            if (!savedName.isNullOrBlank()) {
                companyName = savedName
            }
        }
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
        val scamCheck = JobValidationUtils.validateAgainstScamKeywords(title, description)
        scamValidationResult = scamCheck
        if (!scamCheck.isValid) {
            showScamWarningDialog = true
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
        if (payAmount.isBlank()) {
            Toast.makeText(context, "Enter the wage", Toast.LENGTH_SHORT).show()
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
    onJobPosted: ((String?) -> Unit)? = null,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    val context = LocalContext.current

    val statusBarColorToken = EmployerColors.StatusBarColor
    LaunchedEffect(statusBarColorToken) {
        onStatusBarColorChange?.invoke(statusBarColorToken)
    }
    val scope = rememberCoroutineScope()
    val jobViewModel: com.example.dutype.viewmodels.FirestoreJobViewModel = hiltViewModel()
    val employerJobViewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val instantHelpViewModel: InstantHelpViewModel = hiltViewModel()
    val subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
    val employerSubscription by subscriptionViewModel.activeSubscription.collectAsState()
    val stepState = rememberSaveable { mutableStateOf(1) }

    val c = remember {
        PostJobController(context, navController, jobViewModel, employerJobViewModel, scope, stepState)
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
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        c.onLocationPermissionResult(isGranted)
    }
    c.requestLocationPermission = {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        c.navigateAfterJobPosted(c.pendingNavJobId)
    }
    c.requestNotificationPermission = {
        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    PostJobSideEffects(c)
    PostJobDialogs(c)
    PostJobWizard(c)
    PostJobSheets(c)
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
        c.shiftTiming,
        c.workType,
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
        containerColor = Color.White
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
                    tint = EmployerColors.Primary,
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
            c.submitJob()
        },
        onProfileSetupRequired = {
            c.showLoginBottomSheet = false
            c.openProfileSetup()
        },
        requiresProfileCheck = true,
        role = com.example.dutype.models.UserRole.EMPLOYER,
        title = stringResource(R.string.login_to_post_job),
        subtitle = stringResource(R.string.login_publish_job_subtitle)
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
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
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
            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 140.dp)
    ) {
        PjProgressRow(currentStep = currentStep, totalSteps = PostJobTotalSteps)
        Text(
            text = postJobStepTitle(currentStep),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PjInk
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
            .background(Color.White)
            .navigationBarsPadding()
            .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 28.dp)
    ) {
        Button(
            onClick = { c.goNext() },
            enabled = ctaEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PjInk,
                contentColor = Color.White,
                disabledContainerColor = PjInk.copy(alpha = 0.5f),
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
                    fontWeight = FontWeight.Bold,
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

    PjSectionLabel("Description (optional)")
    PjField(
        value = c.description,
        onValueChange = { c.description = it },
        label = "Job Description",
        placeholder = "Describe the work, timings, what you expect",
        singleLine = false,
        minHeight = 96.dp
    )

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
    val savedWorkLocations by c.jobViewModel.savedWorkLocationsStore.locations.collectAsState()
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
        PostJobShiftChoices.forEach { choice ->
            PjChip(
                label = choice.second,
                selected = c.shiftTiming == choice.first,
                onClick = { c.shiftTiming = choice.first }
            )
        }
    }

    PjSectionLabel("Job type")
    PjFlow {
        PostJobWorkTypes.forEach { type ->
            PjChip(
                label = type,
                selected = c.workType == type,
                onClick = { c.workType = type }
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
    if (c.payAmount.isNotEmpty() && c.payAmount.length < 4) {
        Text(
            text = "Minimum 4 characters required",
            fontSize = 11.sp,
            color = EmployerColors.Error,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    PostJobExtraPayTypeChips(c)

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
}

@Composable
private fun PostJobExtraPayTypeChips(c: PostJobController) {
    PjFlow {
        PostJobExtraPayTypes.forEach { type ->
            PjChip(
                label = type.displayName,
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
    PjFlow {
        PostJobPerkOptions.forEach { perk ->
            val on = perk in c.selectedPerks
            PjChip(
                label = perk,
                selected = on,
                onClick = {
                    c.selectedPerks = if (on) c.selectedPerks - perk else c.selectedPerks + perk
                },
                selectedFill = Color(0xFF0F0F0F),
                showCheck = true
            )
        }
    }
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
                    tint = EmployerColors.TextPrimary
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_post_a_job),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = EmployerColors.TextPrimary
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        title = stringResource(R.string.pay_label),
                        value = if (payAmount.isBlank()) "Set salary" else "Rs. $payAmount ${payType.displayName}"
                    )
                    HeroSignalPill(
                        title = stringResource(R.string.type_label),
                        value = workType.ifBlank { "Choose work type" }
                    )
                    HeroSignalPill(
                        title = stringResource(R.string.area_label),
                        value = location.ifBlank { stringResource(R.string.add_work_location) }
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
            tint = EmployerColors.Primary
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
            tint = EmployerColors.Success
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
                        color = EmployerColors.TextPrimary
                    )
                    Text(
                        text = if (missingStudioItems.isEmpty()) {
                            "All critical details are in place. You can publish when you are ready."
                        } else {
                            "Finish the remaining essentials before launch."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = EmployerColors.TextSecondary,
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
                            color = EmployerColors.TextPrimary
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
                color = EmployerColors.TextPrimary,
                lineHeight = 30.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = EmployerColors.TextSecondary,
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
        color = EmployerColors.CardBackground,
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
                    containerColor = EmployerColors.Primary,
                    disabledContainerColor = EmployerColors.Border
                )
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.publishing), color = Color.White, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(
                        text = stringResource(R.string.publish_job),
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
private fun PostJobLaunchBar(
    publishEnabled: Boolean,
    isPublishing: Boolean,
    onPublish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(com.example.dutype.ui.theme.EmployerColors.CardBackground)
    ) {
        Divider(color = EmployerColors.Border, thickness = 1.dp)
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
                    containerColor = EmployerColors.Primary,
                    contentColor = Color.White,
                    disabledContainerColor = EmployerColors.Border,
                    disabledContentColor = EmployerColors.TextTertiary
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
                color = EmployerColors.TextPrimary,
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
                color = EmployerColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = EmployerColors.TextSecondary,
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

@Composable
private fun PostingTypeTabs(
    selectedType: String,
    isIndividual: Boolean,
    onVacancyClick: () -> Unit,
    onUrgentNeedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Both Company and Personal employers have a consistent tab layout:
    // Tab 0: Job Vacancy (vacancy)
    // Tab 1: Instant Task (urgent)
    val selectedIndex = if (selectedType == "urgent") 1 else 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = EmployerColors.CardBackground,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            TabRow(
                selectedTabIndex = selectedIndex,
                containerColor = EmployerColors.CardBackground,
                contentColor = EmployerColors.Primary
            ) {
                Tab(
                    selected = selectedIndex == 0,
                    onClick = onVacancyClick,
                    icon = { Icon(Icons.Default.Work, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text(if (isIndividual) "📋 Regular Vacancy" else stringResource(R.string.normal_job), fontWeight = if (selectedIndex == 0) FontWeight.Bold else FontWeight.Medium) }
                )
                Tab(
                    selected = selectedIndex == 1,
                    onClick = onUrgentNeedClick,
                    icon = { Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text(if (isIndividual) "⚡ Instant Task" else stringResource(R.string.urgent_need), fontWeight = if (selectedIndex == 1) FontWeight.Bold else FontWeight.Medium) }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = EmployerColors.Primary.copy(alpha = 0.08f)
        ) {
            Text(
                text = if (selectedType == "urgent") {
                    if (isIndividual) {
                        "⚡ Instant Task: Find immediate helpers nearby for household chores, cooking, cleaning, or urgent tasks today."
                    } else {
                        "⚡ Instant Urgent Need: Request quick workers nearby for immediate business shifts or peak hours today."
                    }
                } else {
                    if (isIndividual) {
                        "📋 Regular Vacancy: Post a standard vacancy for ongoing or part-time personal/household assistance."
                    } else {
                        "📋 Regular Vacancy: Post commercial vacancies for full-time, part-time, or shift roles at your company."
                    }
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EmployerColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

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
        // ── Section header (flat, no card wrapper) ────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$stepNumber. $title",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = com.example.dutype.ui.theme.EmployerColors.TextSecondary
            )
        }
        Divider(
            color = com.example.dutype.ui.theme.EmployerColors.Border,
            thickness = 1.dp
        )
        // ── Content ────────────────────────────────────────────────────────
        androidx.compose.runtime.CompositionLocalProvider(
            com.example.dutype.employer.components.LocalSectionInGroup provides true
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}


/**
 * ANTI-FRAUD FEATURE: Structured Job Titles
 * 
 * Employers CANNOT type a job title freely. They must select from a pre-set list.
 * This eliminates "Earn ₹50,000/day working from home" scams instantly.
 * 
 * Implemented: December 27, 2025
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EnhancedJobTitleSection(
    title: String,
    onTitleChange: (String) -> Unit
) {
    val primaryBlue = EmployerColors.Primary
    var titleError by remember { mutableStateOf<String?>(null) }

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
                        .background(EmployerColors.InfoLight, RoundedCornerShape(10.dp)),
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
                            color = EmployerColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "*",
                            color = EmployerColors.Error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = stringResource(R.string.post_job_select_position),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmployerColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Free-text title field. Whatever is typed shows on the card.
            OutlinedTextField(
                value = title,
                onValueChange = { newValue ->
                    onTitleChange(newValue)
                    titleError = null
                },
                label = { Text(stringResource(R.string.enter_job_title)) },
                placeholder = { Text(stringResource(R.string.job_title_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = titleError != null,
                supportingText = if (titleError != null) {
                    { Text(titleError!!, color = EmployerColors.Error) }
                } else {
                    { Text(stringResource(R.string.job_title_hint), color = EmployerColors.TextSecondary) }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (titleError != null) EmployerColors.Error else primaryBlue,
                    focusedLabelColor = if (titleError != null) EmployerColors.Error else primaryBlue,
                    unfocusedBorderColor = if (titleError != null) EmployerColors.Error else EmployerColors.Border,
                    cursorColor = primaryBlue,
                    unfocusedContainerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
                    focusedContainerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground
                )
            )

            // Apr 2026: title-suggestion chips removed � employer types the
            // title directly. Keeps the section compact and frees vertical
            // space for the description below.
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
    onPayTypeChange: (PayType) -> Unit
) {
    val primaryBlue = EmployerColors.Primary
    var payAmountError by remember { mutableStateOf<String?>(null) }

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
                        .background(EmployerColors.SuccessLight, RoundedCornerShape(10.dp)),
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
                        color = EmployerColors.TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.post_job_work_type_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmployerColors.TextSecondary
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
                            containerColor = EmployerColors.ChipBackground,
                            labelColor = EmployerColors.TextSecondary
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
                            color = EmployerColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "*",
                            color = EmployerColors.Error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = stringResource(R.string.post_job_how_much_pay),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmployerColors.TextSecondary
                    )
                }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            OutlinedTextField(
                value = payAmount,
                onValueChange = { newValue ->
                    onPayAmountChange(newValue)
                    payAmountError = if (newValue.length < 4) {
                        "Minimum 4 characters required"
                    } else {
                        null
                    }
                },
                label = { Text(stringResource(R.string.amount_rupees)) },
                placeholder = { Text(stringResource(R.string.amount_example_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = payAmountError != null,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (payAmountError != null) EmployerColors.Error else primaryBlue,
                    focusedLabelColor = if (payAmountError != null) EmployerColors.Error else primaryBlue,
                    unfocusedBorderColor = if (payAmountError != null) EmployerColors.Error else EmployerColors.Border,
                    cursorColor = primaryBlue
                )
            )
            if (payAmountError != null) {
                Text(
                    text = payAmountError!!,
                    color = EmployerColors.Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.auto_pay_type),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = EmployerColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PayType.values().toList()) { type ->
                    val selected = payType == type
                    FilterChip(
                        selected = selected,
                        onClick = { onPayTypeChange(type) },
                        modifier = Modifier.height(44.dp),
                        label = {
                            Text(
                                text = type.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
                            labelColor = EmployerColors.TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = EmployerColors.Border,
                            selectedBorderColor = primaryBlue
                        )
                    )
                }
            }
            
        }
    }
}

@Composable
private fun AutoPickedJobCategory(
    category: JobCategory,
    hasTitle: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.InfoLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_auto_picked_category),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = EmployerColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = if (hasTitle) category.displayName else "Type the title to detect category",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EmployerColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

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
                        color = PjInk
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PjSlate500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            trailing = {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = PjBlue
                    )
                } else {
                    Text(
                        text = "Detect",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PjBlue,
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
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, PjLine, RoundedCornerShape(12.dp))
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
                            tint = PjSlate500,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = suggestion.area.ifBlank { suggestion.city },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PjInk,
                                maxLines = 1
                            )
                            Text(
                                text = suggestion.displayName,
                                fontSize = 12.sp,
                                color = PjSlate500,
                                maxLines = 2
                            )
                        }
                    }
                    if (suggestion != searchSuggestions.take(5).last()) {
                        Divider(color = PjLine, thickness = 1.dp)
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
                selectedColor = Color(0xFF7C3AED),
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
                            selectedContainerColor = Color(0xFF6D28D9),
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
                    .background(Color.White, CircleShape)
                    .border(2.dp, PjInk, CircleShape)
                active -> Modifier
                    .size(28.dp)
                    .background(PjInk, CircleShape)
                else -> Modifier
                    .size(28.dp)
                    .background(PjLine, CircleShape)
            }
            Box(modifier = circleModifier, contentAlignment = Alignment.Center) {
                Text(
                    text = if (done) "✓" else i.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        done -> PjInk
                        active -> Color.White
                        else -> PjSlate400
                    }
                )
            }
            if (i < totalSteps) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp, end = 6.dp)
                        .height(2.dp)
                        .background(if (currentStep > i) PjInk else PjLine)
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
        color = PjSlate500,
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
    val textColor = if (selected) Color.White else PjInk
    Row(
        modifier = Modifier
            .height(height)
            .clip(shape)
            .background(if (selected) selectedFill else Color.White)
            .border(1.dp, if (selected) selectedFill else PjLine, shape)
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
                color = PjGreen
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
        focused -> PjInk
        else -> PjLine
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(Color.White, shape)
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
                color = PjSlate400
            )
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        color = PjSlate400,
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
                        color = PjInk
                    ),
                    cursorBrush = SolidColor(PjInk),
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
                .border(1.dp, PjLine, shape)
                .clickable { if (count > min) onCountChange(count - 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("−", fontSize = 18.sp, color = PjInk)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.widthIn(min = 24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PjInk,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(shape)
                .border(1.dp, PjLine, shape)
                .clickable { if (count < max) onCountChange(count + 1) },
            contentAlignment = Alignment.Center
        ) {
            Text("+", fontSize = 18.sp, color = PjInk)
        }
    }
}

@Composable
private fun PjSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PjInk else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else PjSlate500,
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
            .background(Color.White, shape)
            .border(1.dp, PjLine, shape)
            .padding(start = 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("₹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PjInk)
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (amount.isEmpty()) {
                Text("Enter amount", fontSize = 14.sp, color = PjSlate400, maxLines = 1)
            }
            BasicTextField(
                value = amount,
                onValueChange = onAmountChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PjInk
                ),
                cursorBrush = SolidColor(PjInk),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PjSegmentBg)
                .padding(3.dp)
        ) {
            PjSegment(label = "/Day", selected = payType == PayType.DAILY, onClick = { onPayTypeChange(PayType.DAILY) })
            PjSegment(label = "/Month", selected = payType == PayType.MONTHLY, onClick = { onPayTypeChange(PayType.MONTHLY) })
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
                    color = PjBlue,
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
