package com.example.dutype.employer.screens

import android.Manifest
import android.widget.Toast
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
import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.LocationService
import com.example.dutype.viewmodels.InstantHelpViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId


private data class UrgentCategoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val defaultPay: Int = 600,
    val label: String = title
)

private val URGENT_TRADE_GRID = listOf(
    UrgentCategoryItem("cook", "Cook", "Hotel & home cook", "", 600),
    UrgentCategoryItem("electrician", "Electrician", "Repair & wiring", "", 800),
    UrgentCategoryItem("plumber", "Plumber", "Pipes & fittings", "", 800),
    UrgentCategoryItem("loading_helper", "Loading Helper", "Godown & loading", "", 600, "Helper"),
    UrgentCategoryItem("driver", "Driver", "Auto, tempo, car", "", 700),
    UrgentCategoryItem("security", "Security", "Guard & watchman", "", 600),
    UrgentCategoryItem("carpenter", "Carpenter", "Wood work", "", 800),
    UrgentCategoryItem("delivery", "Delivery", "Delivery partner", "", 600),
    UrgentCategoryItem("painter", "Painter", "Painting work", "", 700)
)

private val URGENT_CATEGORIES = URGENT_TRADE_GRID + listOf(
    UrgentCategoryItem("cleaner", "Cleaner / Maid", "Shop & house clean", "", 500),
    UrgentCategoryItem("other", "Other Work", "Specify manually", "", 600, "Other work")
)

private val URGENT_DURATION_OPTIONS = listOf(
    "Half Day (4 hrs)",
    "Full Day (8 hrs)",
    "2-3 Days",
    "1 Week",
    "Monthly"
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
        onBackClick = { navController.popBackStack() },
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
    onBackClick: () -> Unit = {}
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
    var contactNumber by rememberSaveable { mutableStateOf("") }

    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
    var tempManualAddress by rememberSaveable { mutableStateOf("") }

    suspend fun refreshEmployerLocationState(): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrBlank()) {
            hasEmployerLocation = false
            return false
        }
        val snapshot = runCatching {
            FirebaseFirestore.getInstance()
                .collection(FirestoreCollections.EMPLOYER_PROFILES)
                .document(userId)
                .get()
                .await()
        }.getOrNull()
        val location = snapshot?.get("businessLocation") as? Map<*, *>
        val latitude = (location?.get("lat") as? Number)?.toDouble() ?: 0.0
        val longitude = (location?.get("lng") as? Number)?.toDouble() ?: 0.0
        val valid = GeoUtils.hasValidCoordinates(latitude, longitude)
        hasEmployerLocation = valid

        if (addressText.isBlank()) {
            val addr = snapshot?.getString("businessAddress")
                ?: snapshot?.getString("address")
                ?: ""
            if (addr.isNotBlank()) {
                addressText = addr
            }
        }
        return valid
    }

    suspend fun autoPickEmployerLocation(): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrBlank()) return false
        if (!locationService.hasLocationPermission()) {
            urgentLocationError = context.getString(R.string.location_permission_required_current)
            return false
        }

        isAutoPickingLocation = true
        return try {
            val locationInfo = locationService.getHighAccuracyLocation(
                timeoutMs = 15000L,
                minAccuracyMeters = 10f
            )
            if (locationInfo == null || !GeoUtils.hasValidCoordinates(locationInfo.latitude, locationInfo.longitude)) {
                urgentLocationError = context.getString(R.string.worker_location_fetch_failed)
                false
            } else {
                val addr = locationInfo.getFullAddress()
                val updateData = mutableMapOf<String, Any>(
                    "businessLocation" to mapOf(
                        "lat" to locationInfo.latitude,
                        "lng" to locationInfo.longitude
                    ),
                    "geohash" to GeoUtils.encodeGeohash(locationInfo.latitude, locationInfo.longitude),
                    "updatedAt" to Timestamp.now()
                )
                if (addr.isNotBlank()) {
                    updateData["businessAddress"] = addr
                    addressText = addr
                }

                FirebaseFirestore.getInstance()
                    .collection(FirestoreCollections.EMPLOYER_PROFILES)
                    .document(userId)
                    .set(updateData, SetOptions.merge())
                    .await()
                hasEmployerLocation = true
                urgentLocationError = null
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
        val authPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber.orEmpty()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val profilePhone = if (userId.isNullOrBlank()) {
            authPhone
        } else {
            runCatching {
                FirebaseFirestore.getInstance()
                    .collection(FirestoreCollections.EMPLOYER_PROFILES)
                    .document(userId)
                    .get()
                    .await()
            }.getOrNull()?.let { snapshot ->
                snapshot.getString("phone")
                    ?: snapshot.getString("phoneNumber")
                    ?: snapshot.getString("contactNumber")
            }.orEmpty().ifBlank { authPhone }
        }
        if (contactNumber.isBlank()) contactNumber = profilePhone

        val hasSavedLocation = refreshEmployerLocationState()
        if (!hasSavedLocation && locationService.hasLocationPermission()) {
            autoPickEmployerLocation()
        }
    }

    val currentCategoryItem = URGENT_CATEGORIES.firstOrNull { it.id == selectedCategoryItem }
        ?: URGENT_CATEGORIES.first()
    val effectiveCategory = if (selectedCategoryItem == "other") {
        otherCategory.trim().ifBlank { "General Helper" }
    } else {
        currentCategoryItem.title
    }

    val canPost = (selectedCategoryItem != "other" || otherCategory.trim().length >= 2) &&
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

        viewModel.createUrgentNeed(
            QuickUrgentNeedInput(
                title = effectiveCategory,
                description = notes.ifBlank { "$effectiveCategory needed - $selectedDuration" },
                category = effectiveCategory,
                workersNeeded = workersNeeded,
                needType = if (urgencyType == "tomorrow") "scheduled" else "urgent_now",
                urgencyType = urgencyType,
                contactNumber = contactNumber,
                budgetText = "\u20B9${pp.toInt()} per worker",
                perPersonPayment = pp,
                totalPayment = total,
                durationText = selectedDuration,
                addressText = addressText.ifBlank { "Worksite Location" },
                radiusKm = 10.0,
                scheduledAtMillis = scheduledMillis,
                scheduledAtLabel = when (urgencyType) {
                    "tomorrow" -> "Tomorrow"
                    "today" -> "Today"
                    else -> "Right Now"
                }
            )
        ) { requestId ->
            Toast.makeText(context, "Urgent need broadcasted!", Toast.LENGTH_SHORT).show()
            onPosted(requestId)
        }
    }

    val errorMessage = state.error
    val activeWorkersCount = 48

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
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
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "What do you need right now?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = UrgentNavy
                )
                Spacer(modifier = Modifier.height(18.dp))

                UrgentTradeGrid(
                    selectedId = selectedCategoryItem,
                    onSelect = { item ->
                        selectedCategoryItem = item.id
                        perPersonPaymentText = item.defaultPay.toString()
                    }
                )
                if (selectedCategoryItem == "other") {
                    UrgentOtherInput(value = otherCategory, onValueChange = { otherCategory = it })
                }

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
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
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
}

@Composable
private fun UrgentAlertStrip(showBack: Boolean, onBackClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF2F2))) {
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
                        contentDescription = "Back",
                        tint = UrgentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = "\u26A1 Find a worker within 15 minutes",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = UrgentRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFFECACA))
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
            .background(if (selected) UrgentNavy else Color.White)
            .border(1.dp, if (selected) UrgentNavy else UrgentBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (selected) "\u2713 $label" else label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else UrgentNavy,
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
                        label = item.label,
                        selected = selectedId == item.id,
                        onClick = { onSelect(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        val other = URGENT_CATEGORIES.last()
        UrgentTradeCell(
            label = other.label,
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
                "Specify work (e.g. Mason, Welder, Tailor)",
                fontSize = 12.5.sp,
                color = Color(0xFF94A3B8)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UrgentNavy,
            unfocusedBorderColor = UrgentBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
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
        if (isDetecting) "Detecting\u2026" else "Tap GPS Detect"
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White)
                .border(1.dp, UrgentBorder, shape)
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = UrgentRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEditAddress)
            ) {
                Text(text = "Job location", fontSize = 11.sp, color = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = shownAddress,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UrgentNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "GPS Detect",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = UrgentCobalt,
                modifier = Modifier.clickable(onClick = onGpsDetect)
            )
        }
        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                fontSize = 11.sp,
                color = UrgentRed,
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
            .background(if (selected) Color(0xFF0F0F0F) else Color.White)
            .border(1.dp, if (selected) Color(0xFF0F0F0F) else UrgentBorder, shape)
            .clickable(onClick = onClick)
            .padding(start = horizontalPad, end = horizontalPad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else UrgentNavy,
            maxLines = 1
        )
    }
}

@Composable
private fun UrgentFieldLabel(text: String) {
    Text(text = text, fontSize = 13.sp, color = Color(0xFF64748B))
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
            .border(1.dp, UrgentBorder, shape)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix.isNotEmpty()) {
            Text(text = prefix, fontSize = 13.sp, color = UrgentNavy)
            Spacer(modifier = Modifier.width(4.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 13.sp, color = UrgentNavy),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UrgentWageSection(wageText: String, onWageChange: (String) -> Unit) {
    Column {
        UrgentFieldLabel("Daily wage")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            URGENT_WAGE_OPTIONS.forEach { amount ->
                UrgentChip(
                    label = "\u20B9$amount/day",
                    selected = wageText == amount.toString(),
                    onClick = { onWageChange(amount.toString()) },
                    modifier = Modifier.weight(1f),
                    horizontalPad = 4.dp
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Custom", fontSize = 12.sp, color = Color(0xFF94A3B8))
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
        UrgentFieldLabel("When do you need them?")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UrgentChip("Right now", urgencyType == "right_now", { onUrgencyChange("right_now") }, Modifier.weight(1f), 4.dp)
            UrgentChip("Today", urgencyType == "today", { onUrgencyChange("today") }, Modifier.weight(1f), 4.dp)
            UrgentChip("Tomorrow", urgencyType == "tomorrow", { onUrgencyChange("tomorrow") }, Modifier.weight(1f), 4.dp)
        }

        Spacer(modifier = Modifier.height(18.dp))
        UrgentFieldLabel("Workers needed")
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            UrgentChip("\u2212", false, { if (workersNeeded > 1) onWorkersChange(workersNeeded - 1) }, Modifier.width(56.dp), 0.dp)
            Text(
                text = if (workersNeeded == 1) "1 worker" else "$workersNeeded workers",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = UrgentNavy,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(96.dp)
            )
            UrgentChip("+", false, { if (workersNeeded < 20) onWorkersChange(workersNeeded + 1) }, Modifier.width(56.dp), 0.dp)
        }

        Spacer(modifier = Modifier.height(18.dp))
        UrgentFieldLabel("Duration")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            URGENT_DURATION_OPTIONS.forEach { dur ->
                UrgentChip(dur, selectedDuration == dur, { onDurationChange(dur) })
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        UrgentFieldLabel("Workers will call you on")
        Spacer(modifier = Modifier.height(10.dp))
        UrgentInputBox(
            value = contactNumber,
            onValueChange = onContactChange,
            prefix = "",
            keyboardType = KeyboardType.Phone,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UrgentLiveCounter(count: Int) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFFEFF6FF))
            .border(1.dp, Color(0xFFBFDBFE), shape)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Text(
            text = "\u26A1 $count workers active within 3 km",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D4ED8)
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
            containerColor = UrgentRed,
            disabledContainerColor = Color(0xFFFCA5A5)
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
            text = "\u26A1 Broadcast Urgent Need Now \u2192",
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
        containerColor = Color.White,
        title = {
            Text(
                text = "Worksite Location",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = UrgentNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDetectGps,
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentNavy),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Detect Current GPS Location")
                }
                Text("Or enter manual address:", fontSize = 12.sp, color = Color(0xFF64748B))
                OutlinedTextField(
                    value = manualAddress,
                    onValueChange = onManualAddressChange,
                    placeholder = { Text("e.g. APMC Market Yard, Gate 2, Hubli") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save Address", fontWeight = FontWeight.Bold, color = UrgentCobalt)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        }
    )
}
