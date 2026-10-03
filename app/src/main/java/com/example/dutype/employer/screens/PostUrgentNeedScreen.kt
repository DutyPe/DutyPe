package com.example.dutype.employer.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import android.Manifest
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.dutype.employer.components.VoiceJobPostingBottomSheet
import com.example.dutype.employer.components.VoiceJobTriggerCard
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.firestore.FirestoreSchema.EmployerProfiles
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.LocationService
import com.example.dutype.viewmodels.InstantHelpViewModel
import com.example.dutype.components.LoginBottomSheet
import com.example.dutype.models.UserRole
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId


private data class UrgentCategoryItem(
    val id: String,
    val title: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val emoji: String,
    val defaultPay: Int = 600,
    @StringRes val labelRes: Int = titleRes
)

private val URGENT_TRADE_GRID = listOf(
    UrgentCategoryItem("cook", "Cook", R.string.category_cook, R.string.urgent_cat_cook_sub, "", 600),
    UrgentCategoryItem("electrician", "Electrician", R.string.category_electrician, R.string.urgent_cat_electrician_sub, "", 800),
    UrgentCategoryItem("plumber", "Plumber", R.string.category_plumber, R.string.urgent_cat_plumber_sub, "", 800),
    UrgentCategoryItem("loading_helper", "Loading Helper", R.string.urgent_cat_loading_helper, R.string.urgent_cat_loading_helper_sub, "", 600, R.string.category_helper),
    UrgentCategoryItem("driver", "Driver", R.string.category_driver, R.string.urgent_cat_driver_sub, "", 700),
    UrgentCategoryItem("security", "Security", R.string.category_security, R.string.urgent_cat_security_sub, "", 600),
    UrgentCategoryItem("carpenter", "Carpenter", R.string.category_carpenter, R.string.urgent_cat_carpenter_sub, "", 800),
    UrgentCategoryItem("delivery", "Delivery", R.string.category_delivery, R.string.urgent_cat_delivery_sub, "", 600),
    UrgentCategoryItem("painter", "Painter", R.string.category_painter, R.string.urgent_cat_painter_sub, "", 700)
)

private val URGENT_CATEGORIES = URGENT_TRADE_GRID + listOf(
    UrgentCategoryItem("cleaner", "Cleaner / Maid", R.string.urgent_cat_cleaner, R.string.urgent_cat_cleaner_sub, "", 500),
    UrgentCategoryItem("other", "Other Work", R.string.urgent_cat_other, R.string.urgent_cat_other_sub, "", 600, R.string.urgent_cat_other_label)
)

private data class UrgentDurationOption(val key: String, @StringRes val labelRes: Int)

private val URGENT_DURATION_OPTIONS = listOf(
    UrgentDurationOption("Half Day (4 hrs)", R.string.duration_half_day),
    UrgentDurationOption("Full Day (8 hrs)", R.string.duration_full_day),
    UrgentDurationOption("2-3 Days", R.string.duration_2_3_days),
    UrgentDurationOption("1 Week", R.string.duration_1_week),
    UrgentDurationOption("Monthly", R.string.duration_monthly)
)

private val URGENT_WAGE_OPTIONS = listOf(500, 750, 1000)

private val UrgentNavy = Color(0xFF0F172A)
private val UrgentCobalt = Color(0xFF2563EB)
private val UrgentBorder = Color(0xFFE2E8F0)
private val UrgentRed = Color(0xFFDC2626)

@Composable
fun PostUrgentNeedScreen(
    navController: NavController,
    initialCategory: String? = null,
    viewModel: InstantHelpViewModel = hiltViewModel()
) {
    PostUrgentNeedContent(
        viewModel = viewModel,
        initialCategory = initialCategory,
        onPosted = { requestId ->
            navController.navigate(Routes.employerUrgentNeedDetailRoute(requestId)) {
                popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = false }
                launchSingleTop = true
            }
        },
        showTopBar = true,
        onBackClick = {
            val popped = navController.popBackStack()
            if (!popped) {
                navController.navigate(Routes.EMPLOYER_HOME) {
                    popUpTo(Routes.EMPLOYER_HOME) { inclusive = false }
                    launchSingleTop = true
                }
            }
        },
        navController = navController,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun PostUrgentNeedContent(
    viewModel: InstantHelpViewModel,
    onPosted: (String) -> Unit,
    initialCategory: String? = null,
    onInsufficientCredits: () -> Boolean = { false },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    showTopBar: Boolean = false,
    onBackClick: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 28.dp,
    navController: NavController? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember(context) { LocationService(context) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Form fields with prefilled initialCategory support
    val initialMatch = remember(initialCategory) {
        if (initialCategory.isNullOrBlank()) null
        else URGENT_CATEGORIES.firstOrNull {
            it.title.equals(initialCategory, ignoreCase = true) ||
            it.id.equals(initialCategory, ignoreCase = true) ||
            it.title.contains(initialCategory, ignoreCase = true) ||
            initialCategory.contains(it.title, ignoreCase = true)
        }
    }
    var selectedCategoryItem by rememberSaveable(initialCategory) {
        mutableStateOf(
            initialMatch?.id ?: if (!initialCategory.isNullOrBlank()) "other" else "loading_helper"
        )
    }
    var otherCategory by rememberSaveable(initialCategory) {
        mutableStateOf(
            if (initialMatch == null && !initialCategory.isNullOrBlank()) initialCategory else ""
        )
    }

    var urgencyType by rememberSaveable { mutableStateOf("right_now") }
    var workersNeeded by rememberSaveable { mutableStateOf(2) }

    var selectedDuration by rememberSaveable { mutableStateOf("Full Day (8 hrs)") }

    var perPersonPaymentText by rememberSaveable(initialCategory) {
        mutableStateOf(initialMatch?.defaultPay?.toString() ?: "600")
    }

    var hasEmployerLocation by rememberSaveable { mutableStateOf(false) }
    var isAutoPickingLocation by rememberSaveable { mutableStateOf(false) }
    var urgentLocationError by rememberSaveable { mutableStateOf<String?>(null) }

    var addressText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    // The job title the employer types; tapping a work-type chip fills it (and stays editable).
    var jobTitle by rememberSaveable(initialCategory) { mutableStateOf(initialCategory?.takeIf { initialMatch == null }.orEmpty()) }
    var lastChipTitle by rememberSaveable { mutableStateOf("") }
    var contactNumber by rememberSaveable { mutableStateOf("") }

    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var tempManualAddress by rememberSaveable { mutableStateOf("") }
    var showVoiceSheet by rememberSaveable { mutableStateOf(false) }
    var showLoginBottomSheet by rememberSaveable { mutableStateOf(false) }

    val profileCompletionService = hiltViewModel<com.example.dutype.viewmodels.ProfileCompletionViewModel>().profileCompletionService

    suspend fun refreshEmployerLocationState(): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val employer = userId?.let { profileCompletionService.getEmployer(it).getOrNull() }
        val valid = employer != null && GeoUtils.hasValidCoordinates(employer.lat, employer.lng)
        hasEmployerLocation = valid
        if (addressText.isBlank() && employer != null && employer.address.isNotBlank()) {
            addressText = employer.address
        }
        if (contactNumber.isBlank()) {
            contactNumber = employer?.phone.orEmpty().ifBlank { FirebaseAuth.getInstance().currentUser?.phoneNumber.orEmpty() }
        }
        return valid
    }

    suspend fun autoPickEmployerLocation(): Boolean {
        if (!locationService.hasLocationPermission()) {
            urgentLocationError = context.getString(R.string.location_permission_required_current)
            return false
        }

        isAutoPickingLocation = true
        return try {
            val locationInfo = locationService.getHighAccuracyLocation(
                timeoutMs = 10000L,
                minAccuracyMeters = 50f
            ) ?: locationService.getCurrentLocation()
              ?: locationService.getCachedLocation()

            if (locationInfo == null || !GeoUtils.hasValidCoordinates(locationInfo.latitude, locationInfo.longitude)) {
                urgentLocationError = context.getString(R.string.worker_location_fetch_failed)
                false
            } else {
                val addr = locationInfo.getFullAddress()
                if (addr.isNotBlank()) {
                    addressText = addr
                } else if (addressText.isBlank()) {
                    addressText = "${String.format("%.4f", locationInfo.latitude)}, ${String.format("%.4f", locationInfo.longitude)}"
                }
                hasEmployerLocation = true
                urgentLocationError = null

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (!userId.isNullOrBlank()) {
                    runCatching {
                        profileCompletionService.saveEmployer(
                            buildMap {
                                put(EmployerProfiles.LAT, locationInfo.latitude)
                                put(EmployerProfiles.LNG, locationInfo.longitude)
                                if (addr.isNotBlank()) put(EmployerProfiles.ADDRESS, addr)
                            }
                        )
                    }
                }
                Toast.makeText(context, context.getString(R.string.location_updated), Toast.LENGTH_SHORT).show()
                true
            }
        } catch (_: Exception) {
            urgentLocationError = context.getString(R.string.worker_location_fetch_failed)
            false
        } finally {
            isAutoPickingLocation = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch { autoPickEmployerLocation() }
        } else {
            urgentLocationError = context.getString(R.string.location_permission_required_current)
            Toast.makeText(context, urgentLocationError, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasSavedLocation = refreshEmployerLocationState()
        if (!hasSavedLocation && locationService.hasLocationPermission()) {
            autoPickEmployerLocation()
        }
    }

    val currentCategoryItem = URGENT_CATEGORIES.firstOrNull { it.id == selectedCategoryItem }
        ?: URGENT_CATEGORIES.first()
    val defaultGeneralHelper = stringResource(R.string.general_helper)
    val effectiveCategory = if (selectedCategoryItem == "other") {
        otherCategory.trim().ifBlank { jobTitle.trim() }.ifBlank { defaultGeneralHelper }
    } else {
        currentCategoryItem.title
    }

    val canPost = (selectedCategoryItem != "other" || otherCategory.trim().length >= 2 || jobTitle.trim().length >= 2) &&
            contactNumber.isNotBlank() &&
            !isAutoPickingLocation

    fun startGpsDetect() {
        if (locationService.hasLocationPermission()) {
            scope.launch { autoPickEmployerLocation() }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun broadcastNeed() {
        val pp = perPersonPaymentText.toDoubleOrNull() ?: 600.0
        val total = pp * workersNeeded
        // Urgent work is short and paid the same day: a regular salary belongs in a job post.
        val rules = com.example.dutype.utils.PayRules
        if (pp < rules.URGENT_MIN || pp > rules.URGENT_MAX) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.pay_rule_urgent_range, rules.URGENT_MIN.toInt(), rules.URGENT_MAX.toInt()),
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showLoginBottomSheet = true
            return
        }

        if (!hasEmployerLocation) {
            if (locationService.hasLocationPermission()) {
                scope.launch {
                    autoPickEmployerLocation()
                }
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                return
            }
        }

        val zone = ZoneId.systemDefault()
        val scheduledMillis = when (urgencyType) {
            "tomorrow" -> LocalDate.now(zone).plusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
            "today" -> System.currentTimeMillis() + 24L * 60 * 60 * 1000L
            else -> 0L
        }

        val defaultWorksite = context.getString(R.string.urgent_need_worksite_location)
        val scheduledAtLabelText = when (urgencyType) {
            "tomorrow" -> context.getString(R.string.urgent_need_tomorrow)
            "today" -> context.getString(R.string.urgent_need_today)
            else -> context.getString(R.string.urgent_need_right_now)
        }

        viewModel.createUrgentNeed(
            QuickUrgentNeedInput(
                title = jobTitle.trim().ifBlank { if (notes.isBlank()) effectiveCategory else "$effectiveCategory: ${notes.trim()}" }.take(80),
                description = notes.ifBlank { "$effectiveCategory needed - $selectedDuration" },
                category = effectiveCategory,
                workersNeeded = workersNeeded,
                needType = if (urgencyType == "tomorrow") "scheduled" else "urgent_now",
                urgencyType = urgencyType,
                contactNumber = contactNumber,
                budgetText = context.getString(R.string.budget_per_worker_format, pp.toInt()),
                perPersonPayment = pp,
                totalPayment = total,
                durationText = selectedDuration,
                addressText = addressText.ifBlank { defaultWorksite },
                radiusKm = 10.0,
                scheduledAtMillis = scheduledMillis,
                scheduledAtLabel = scheduledAtLabelText
            )
        ) { requestId ->
            Toast.makeText(context, context.getString(R.string.urgent_need_broadcasted_success), Toast.LENGTH_SHORT).show()
            onPosted(requestId)
        }
    }

    val errorMessage = state.error
    val activeWorkersCount = 48

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White.bg())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            UrgentAlertStrip(showBack = showTopBar, onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                VoiceJobTriggerCard(
                    onClick = { showVoiceSheet = true }
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.urgent_need_what_do_you_need),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = UrgentNavy.fg()
                )
                Spacer(modifier = Modifier.height(14.dp))

                UrgentTitleField(value = jobTitle, onValueChange = { jobTitle = it.take(80) })
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.urgent_pick_work_type),
                    fontSize = 12.sp,
                    color = Color(0xFF64748B).fg()
                )
                Spacer(modifier = Modifier.height(10.dp))
                UrgentTradeGrid(
                    selectedId = selectedCategoryItem,
                    onSelect = { item ->
                        selectedCategoryItem = item.id
                        perPersonPaymentText = item.defaultPay.toString()
                        // Fill the title from the chip unless the employer wrote their own.
                        val chipTitle = context.getString(item.labelRes)
                        if (item.id != "other" && (jobTitle.isBlank() || jobTitle == lastChipTitle)) {
                            jobTitle = chipTitle
                            lastChipTitle = chipTitle
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))
                UrgentLocationCard(
                    addressText = addressText,
                    isDetecting = isAutoPickingLocation,
                    errorText = urgentLocationError,
                    onGpsDetect = { startGpsDetect() },
                    onEditAddress = {
                        tempManualAddress = addressText
                        showLocationDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))
                UrgentWageSection(
                    wageText = perPersonPaymentText,
                    onWageChange = { perPersonPaymentText = it }
                )

                Spacer(modifier = Modifier.height(18.dp))
                UrgentDetailsSection(
                    urgencyType = urgencyType,
                    onUrgencyChange = { urgencyType = it },
                    workersNeeded = workersNeeded,
                    onWorkersChange = { workersNeeded = it },
                    selectedDuration = selectedDuration,
                    onDurationChange = { selectedDuration = it },
                    contactNumber = contactNumber,
                    onContactChange = { contactNumber = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) }
                )

                Spacer(modifier = Modifier.height(22.dp))
                UrgentLiveCounter(count = activeWorkersCount)

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.Error)
                    )
                }
                Spacer(modifier = Modifier.height(130.dp))
            }
        }

        UrgentBroadcastButton(
            enabled = !state.isPostingUrgentNeed && canPost,
            loading = state.isPostingUrgentNeed || isAutoPickingLocation,
            onClick = { broadcastNeed() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = bottomPadding)
        )
    }

    if (showLocationDialog) {
        UrgentLocationDialog(
            manualAddress = tempManualAddress,
            onManualAddressChange = { tempManualAddress = it },
            onDetectGps = {
                if (locationService.hasLocationPermission()) {
                    scope.launch {
                        autoPickEmployerLocation()
                        showLocationDialog = false
                    }
                } else {
                    startGpsDetect()
                }
            },
            onSave = {
                if (tempManualAddress.isNotBlank()) {
                    addressText = tempManualAddress.trim()
                    hasEmployerLocation = true
                }
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false }
        )
    }

    if (showVoiceSheet) {
        VoiceJobPostingBottomSheet(
            onDismiss = { showVoiceSheet = false },
            onPostUrgentJob = { input ->
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    showVoiceSheet = false
                    showLoginBottomSheet = true
                } else {
                    scope.launch {
                        if (!hasEmployerLocation) {
                            autoPickEmployerLocation()
                        }
                        viewModel.createUrgentNeed(input) { requestId ->
                            Toast.makeText(context, context.getString(R.string.urgent_need_posted_success), Toast.LENGTH_SHORT).show()
                            onPosted(requestId)
                        }
                    }
                }
            },
            onEditManually = { input ->
                val matched = URGENT_CATEGORIES.firstOrNull {
                    it.title.equals(input.category, ignoreCase = true) ||
                    context.getString(it.labelRes).equals(input.category, ignoreCase = true)
                }
                if (matched != null) {
                    selectedCategoryItem = matched.id
                } else {
                    selectedCategoryItem = "other"
                    otherCategory = input.category
                }
                workersNeeded = input.workersNeeded
                if (input.title.isNotBlank()) jobTitle = input.title.take(80)
                if (input.perPersonPayment > 0) {
                    perPersonPaymentText = input.perPersonPayment.toInt().toString()
                }
                if (input.addressText.isNotBlank()) {
                    addressText = input.addressText
                }
                if (input.description.isNotBlank()) {
                    notes = input.description
                }
            },
            employerPhone = contactNumber,
            defaultAddress = addressText
        )
    }

    LoginBottomSheet(
        isVisible = showLoginBottomSheet,
        onDismiss = { showLoginBottomSheet = false },
        onLoginSuccess = {
            showLoginBottomSheet = false
            broadcastNeed()
        },
        onProfileSetupRequired = {
            showLoginBottomSheet = false
            navController?.navigate(
                Routes.employerProfileSetupWithReturnRoute(Routes.EMPLOYER_POST_JOB)
            ) {
                launchSingleTop = true
            }
        },
        requiresProfileCheck = false,
        role = UserRole.EMPLOYER,
        title = stringResource(R.string.login_to_post_job),
        subtitle = stringResource(R.string.login_publish_job_subtitle),
        navController = navController
    )
}

@Composable
private fun UrgentAlertStrip(showBack: Boolean, onBackClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF2F2).bg())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (showBack) Modifier.statusBarsPadding() else Modifier)
                .height(48.dp)
        ) {
            if (showBack) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = UrgentRed.fg(),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.urgent_need_find_worker_15m),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = UrgentRed.fg(),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFFECACA).bg())
        )
    }
}

@Composable
private fun UrgentTradeCell(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cellHeight: androidx.compose.ui.unit.Dp = 56.dp
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(cellHeight)
            .clip(shape)
            .background(if (selected) UrgentNavy.bg() else Color.White.bg())
            .border(1.dp, if (selected) UrgentNavy.bd() else UrgentBorder.bd(), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (selected) "\u2713 $label" else label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else UrgentNavy.fg(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UrgentTradeGrid(
    selectedId: String,
    onSelect: (UrgentCategoryItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        URGENT_TRADE_GRID.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    UrgentTradeCell(
                        label = stringResource(item.labelRes),
                        selected = selectedId == item.id,
                        onClick = { onSelect(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        val other = URGENT_CATEGORIES.last()
        UrgentTradeCell(
            label = stringResource(other.labelRes),
            selected = selectedId == other.id,
            onClick = { onSelect(other) },
            modifier = Modifier.fillMaxWidth(),
            cellHeight = 44.dp
        )
    }
}

@Composable
private fun UrgentOtherInput(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                stringResource(R.string.urgent_need_specify_work_hint),
                fontSize = 12.5.sp,
                color = Color(0xFF94A3B8).fg()
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UrgentNavy.bd(),
            unfocusedBorderColor = UrgentBorder.bd(),
            focusedContainerColor = Color.White.bg(),
            unfocusedContainerColor = Color.White.bg()
        )
    )
}

@Composable
private fun UrgentLocationCard(
    addressText: String,
    isDetecting: Boolean,
    errorText: String?,
    onGpsDetect: () -> Unit,
    onEditAddress: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val shownAddress = addressText.ifBlank {
        if (isDetecting) stringResource(R.string.urgent_need_detecting) else stringResource(R.string.urgent_need_tap_gps)
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White.bg())
                .border(1.dp, UrgentBorder.bd(), shape)
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = UrgentRed.fg(),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEditAddress)
            ) {
                Text(text = stringResource(R.string.urgent_need_job_location), fontSize = 11.sp, color = Color(0xFF94A3B8).fg())
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = shownAddress,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UrgentNavy.fg(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.urgent_need_gps_detect),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = UrgentCobalt.fg(),
                modifier = Modifier.clickable(onClick = onGpsDetect)
            )
        }
        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                fontSize = 11.sp,
                color = UrgentRed.fg(),
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }
    }
}

@Composable
private fun UrgentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPad: androidx.compose.ui.unit.Dp = 16.dp
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) Color(0xFF0F0F0F).bg() else Color.White.bg())
            .border(1.dp, if (selected) Color(0xFF0F0F0F).bd() else UrgentBorder.bd(), shape)
            .clickable(onClick = onClick)
            .padding(start = horizontalPad, end = horizontalPad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else UrgentNavy.fg(),
            maxLines = 1
        )
    }
}

@Composable
private fun UrgentFieldLabel(text: String) {
    Text(text = text, fontSize = 13.sp, color = Color(0xFF64748B).fg())
}

@Composable
private fun UrgentInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .border(1.dp, UrgentBorder.bd(), shape)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix.isNotEmpty()) {
            Text(text = prefix, fontSize = 13.sp, color = UrgentNavy.fg())
            Spacer(modifier = Modifier.width(4.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 13.sp, color = UrgentNavy.fg()),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UrgentWageSection(wageText: String, onWageChange: (String) -> Unit) {
    Column {
        UrgentFieldLabel(stringResource(R.string.urgent_need_daily_wage))
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            URGENT_WAGE_OPTIONS.forEach { amount ->
                UrgentChip(
                    label = stringResource(R.string.wage_per_day_format, amount),
                    selected = wageText == amount.toString(),
                    onClick = { onWageChange(amount.toString()) },
                    modifier = Modifier.weight(1f),
                    horizontalPad = 4.dp
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.urgent_need_custom_wage), fontSize = 12.sp, color = Color(0xFF94A3B8).fg())
            Spacer(modifier = Modifier.width(10.dp))
            UrgentInputBox(
                value = wageText,
                onValueChange = { input -> onWageChange(input.filter { it.isDigit() }.take(5)) },
                prefix = "\u20B9",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.width(120.dp)
            )
        }
    }
}

@Composable
private fun UrgentDetailsSection(
    urgencyType: String,
    onUrgencyChange: (String) -> Unit,
    workersNeeded: Int,
    onWorkersChange: (Int) -> Unit,
    selectedDuration: String,
    onDurationChange: (String) -> Unit,
    contactNumber: String,
    onContactChange: (String) -> Unit
) {
    Column {
        UrgentFieldLabel(stringResource(R.string.urgent_need_when_needed))
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UrgentChip(stringResource(R.string.urgent_need_right_now), urgencyType == "right_now", { onUrgencyChange("right_now") }, Modifier.weight(1f), 4.dp)
            UrgentChip(stringResource(R.string.urgent_need_today), urgencyType == "today", { onUrgencyChange("today") }, Modifier.weight(1f), 4.dp)
            UrgentChip(stringResource(R.string.urgent_need_tomorrow), urgencyType == "tomorrow", { onUrgencyChange("tomorrow") }, Modifier.weight(1f), 4.dp)
        }

        Spacer(modifier = Modifier.height(18.dp))
        UrgentFieldLabel(stringResource(R.string.urgent_need_workers_needed))
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            UrgentChip("\u2212", false, { if (workersNeeded > 1) onWorkersChange(workersNeeded - 1) }, Modifier.width(56.dp), 0.dp)
            Text(
                text = if (workersNeeded == 1) stringResource(R.string.one_worker) else stringResource(R.string.workers_count_format, workersNeeded),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = UrgentNavy.fg(),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(96.dp)
            )
            UrgentChip("+", false, { if (workersNeeded < 20) onWorkersChange(workersNeeded + 1) }, Modifier.width(56.dp), 0.dp)
        }

        Spacer(modifier = Modifier.height(18.dp))
        UrgentFieldLabel(stringResource(R.string.urgent_need_duration))
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            URGENT_DURATION_OPTIONS.forEach { dur ->
                UrgentChip(stringResource(dur.labelRes), selectedDuration == dur.key, { onDurationChange(dur.key) })
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        UrgentFieldLabel(stringResource(R.string.urgent_need_workers_will_call_on))
        Spacer(modifier = Modifier.height(10.dp))
        UrgentPhoneField(value = contactNumber, onValueChange = onContactChange)
    }
}

@Composable
private fun UrgentLiveCounter(count: Int) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFFEFF6FF).bg())
            .border(1.dp, Color(0xFFBFDBFE).bd(), shape)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Text(
            text = stringResource(R.string.urgent_need_active_counter, count),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D4ED8).fg()
        )
    }
}

@Composable
private fun UrgentBroadcastButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = UrgentRed.bg(),
            disabledContainerColor = Color(0xFFFCA5A5).bg()
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = stringResource(R.string.urgent_need_post_now_btn),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun UrgentLocationDialog(
    manualAddress: String,
    onManualAddressChange: (String) -> Unit,
    onDetectGps: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White.bg(),
        title = {
            Text(
                text = stringResource(R.string.urgent_need_worksite_location),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = UrgentNavy.fg()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDetectGps,
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentNavy.bg()),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.urgent_need_detect_gps_btn))
                }
                Text(stringResource(R.string.urgent_need_or_manual_address), fontSize = 12.sp, color = Color(0xFF64748B).fg())
                OutlinedTextField(
                    value = manualAddress,
                    onValueChange = onManualAddressChange,
                    placeholder = { Text(stringResource(R.string.urgent_manual_address_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.urgent_need_save_address), fontWeight = FontWeight.Bold, color = UrgentCobalt.fg())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color(0xFF64748B).fg())
            }
        }
    )
}

/** "Job title" — typed freely, or filled by tapping a work type below. */
@Composable
private fun UrgentTitleField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.urgent_job_title_label)) },
        placeholder = { Text(stringResource(R.string.urgent_job_title_hint), color = Color(0xFF94A3B8).fg()) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UrgentNavy.fg(),
            unfocusedBorderColor = UrgentBorder.bd(),
            focusedLabelColor = UrgentNavy.fg(),
            focusedContainerColor = Color.White.bg(),
            unfocusedContainerColor = Color.White.bg(),
            focusedTextColor = UrgentNavy.fg(),
            unfocusedTextColor = UrgentNavy.fg()
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/** "Workers will call you on": a comfortable phone field with an icon, not a thin stretched pill. */
@Composable
private fun UrgentPhoneField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(stringResource(R.string.urgent_phone_hint), color = Color(0xFF94A3B8).fg()) },
        leadingIcon = {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Phone,
                contentDescription = null,
                tint = Color(0xFF64748B).fg(),
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UrgentNavy.fg(),
            unfocusedBorderColor = UrgentBorder.bd(),
            focusedContainerColor = Color.White.bg(),
            unfocusedContainerColor = Color.White.bg(),
            focusedTextColor = UrgentNavy.fg(),
            unfocusedTextColor = UrgentNavy.fg()
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
