package com.example.dutype.employer.screens

import com.dutype.app.R
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
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
import java.time.format.DateTimeFormatter

@Composable
fun PostUrgentNeedScreen(
    navController: NavController,
    viewModel: InstantHelpViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmployerColors.ScreenBackground)
    ) {
        CommonHeader(
            title = stringResource(R.string.post_urgent_need_title),
            navController = navController,
            subtitle = stringResource(R.string.post_urgent_need_subtitle),
            backgroundColor = EmployerColors.ScreenBackground,
            titleColor = EmployerColors.TextPrimary,
            subtitleColor = EmployerColors.TextSecondary
        )

        val context = LocalContext.current
        val subscriptionViewModel: com.example.dutype.viewmodels.SubscriptionViewModel = hiltViewModel()
        val employerSubscription by subscriptionViewModel.activeSubscription.collectAsStateWithLifecycle()

        PostUrgentNeedContent(
            viewModel = viewModel,
            onPosted = { requestId ->
                navController.navigate(Routes.employerUrgentNeedDetailRoute(requestId)) {
                    popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onInsufficientCredits = {
                if (employerSubscription.status != "LOADING" && employerSubscription.normalCredits <= 0) {
                    Toast.makeText(context, "Please purchase a subscription to post jobs", Toast.LENGTH_LONG).show()
                    navController.navigate(Routes.EMPLOYER_SUBSCRIPTION)
                    true
                } else {
                    false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostUrgentNeedContent(
    viewModel: InstantHelpViewModel,
    onPosted: (String) -> Unit,
    onInsufficientCredits: () -> Boolean = { false },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    showIntroCard: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember(context) { LocationService(context) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Form fields
    val categories = stringArrayResource(R.array.urgent_work_categories).toList()
    var selectedCategory by rememberSaveable { mutableStateOf(categories.first()) }
    var otherCategory by rememberSaveable { mutableStateOf("") }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }

    val urgencyOptions = listOf(
        "right_now" to "Right Now",
        "within_1_hour" to "Within 1 Hour",
        "today" to "Today",
        "tomorrow" to "Tomorrow",
        "custom" to "Select Date"
    )
    var urgencyType by rememberSaveable { mutableStateOf("right_now") }
    var scheduledAtMillis by rememberSaveable { mutableStateOf(0L) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    
    var workersNeeded by rememberSaveable { mutableStateOf(1) }
    
    val durationOptions = stringArrayResource(R.array.urgent_work_durations).toList()
    var selectedDuration by rememberSaveable { mutableStateOf(durationOptions.first()) }
    var durationExpanded by rememberSaveable { mutableStateOf(false) }

    var perPersonPaymentText by rememberSaveable { mutableStateOf("") }
    
    var hasEmployerLocation by rememberSaveable { mutableStateOf(false) }
    var isAutoPickingLocation by rememberSaveable { mutableStateOf(false) }
    var urgentLocationError by rememberSaveable { mutableStateOf<String?>(null) }
    
    var addressText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var contactNumber by rememberSaveable { mutableStateOf("") }

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
            addressText = snapshot?.getString("businessAddress").orEmpty()
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
                val updateData = mutableMapOf<String, Any>(
                    "businessLocation" to mapOf(
                        "lat" to locationInfo.latitude,
                        "lng" to locationInfo.longitude
                    ),
                    "geohash" to GeoUtils.encodeGeohash(locationInfo.latitude, locationInfo.longitude),
                    "updatedAt" to Timestamp.now()
                )
                if (addressText.isBlank()) {
                    val addr = locationInfo.getFullAddress()
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

    val effectiveCategory = if (selectedCategory == "Other") otherCategory.trim() else selectedCategory
    val canPost = effectiveCategory.length >= 3 &&
        contactNumber.isNotBlank() &&
        hasEmployerLocation &&
        !isAutoPickingLocation

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 1: Category
        item {
            UrgentNeedSectionCard(title = "What work do you need?") {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                
                if (selectedCategory == "Other") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otherCategory,
                        onValueChange = { otherCategory = it },
                        label = { Text("What work do you need?") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        // Section 2: Timing
        item {
            UrgentNeedSectionCard(title = "When do you need them?") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    urgencyOptions.forEach { option ->
                        FilterChip(
                            selected = urgencyType == option.first,
                            onClick = { 
                                urgencyType = option.first
                                if (option.first == "custom") {
                                    showDatePicker = true
                                }
                            },
                            label = { Text(option.second) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmployerColors.WarningLight,
                                selectedLabelColor = EmployerColors.Warning
                            )
                        )
                    }
                }
                
                if (urgencyType == "custom" && scheduledAtMillis > 0) {
                    val dateStr = LocalDate.ofEpochDay(scheduledAtMillis / (24 * 60 * 60 * 1000L)).format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
                    Text("Selected Date: $dateStr", style = MaterialTheme.typography.bodyMedium, color = EmployerColors.Primary)
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = if (scheduledAtMillis > 0) scheduledAtMillis else System.currentTimeMillis()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                scheduledAtMillis = datePickerState.selectedDateMillis ?: 0L
                                showDatePicker = false
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDatePicker = false
                                if (scheduledAtMillis == 0L) urgencyType = "right_now"
                            }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }
        }

        // Section 3 & 4: Workers and Duration
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UrgentNeedSectionCard(title = "Workers", modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (workersNeeded > 1) workersNeeded-- }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = workersNeeded.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { if (workersNeeded < 20) workersNeeded++ }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
                
                UrgentNeedSectionCard(title = "Duration", modifier = Modifier.weight(1.5f)) {
                    ExposedDropdownMenuBox(
                        expanded = durationExpanded,
                        onExpandedChange = { durationExpanded = !durationExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedDuration,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = durationExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = durationExpanded,
                            onDismissRequest = { durationExpanded = false }
                        ) {
                            durationOptions.forEach { dur ->
                                DropdownMenuItem(
                                    text = { Text(dur) },
                                    onClick = {
                                        selectedDuration = dur
                                        durationExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Payment
        item {
            UrgentNeedSectionCard(title = "Payment") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = perPersonPaymentText,
                        onValueChange = { value -> perPersonPaymentText = value.filter { it.isDigit() } },
                        label = { Text("₹ per person") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    
                    val pp = perPersonPaymentText.toDoubleOrNull() ?: 0.0
                    val total = pp * workersNeeded
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(EmployerColors.PrimaryLight, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Amount", style = MaterialTheme.typography.bodySmall, color = EmployerColors.TextSecondary)
                        Text(
                            text = "₹${total.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmployerColors.TextPrimary
                        )
                    }
                }
            }
        }

        // Section 6 & 7: Location and Address
        item {
            UrgentNeedSectionCard(title = "Exact Address") {
                if (!hasEmployerLocation) {
                    Button(
                        onClick = {
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
                        },
                        enabled = !isAutoPickingLocation,
                        colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAutoPickingLocation) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        }
                        Text(text = stringResource(R.string.auto_use_current_location))
                    }
                    if (!urgentLocationError.isNullOrBlank()) {
                        Text(
                            text = urgentLocationError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.Error)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        placeholder = { Text("e.g. Near Bus Stand, House No 12-3") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        // Section 8: Description
        item {
            UrgentNeedSectionCard(title = "Description (Optional)") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 150) notes = it },
                    placeholder = { Text("e.g. Need two helpers to shift furniture.") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${notes.length}/150") },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        // Section 9: Contact Preference (Read only)
        item {
            UrgentNeedSectionCard(title = "Contact Preference") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = EmployerColors.Success)
                    Spacer(Modifier.width(8.dp))
                    Text("Call (Workers will call you directly)")
                }
            }
        }

        if (!state.error.isNullOrBlank()) {
            item {
                Text(
                    text = state.error ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.Error),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        item {
            Button(
                onClick = {
                    // Free Strategy: No credit check for Urgent Work
                    val pp = perPersonPaymentText.toDoubleOrNull() ?: 0.0
                    val total = pp * workersNeeded
                    
                    viewModel.createUrgentNeed(
                        QuickUrgentNeedInput(
                            title = effectiveCategory, // Use category as title for quick display
                            description = notes,
                            category = effectiveCategory,
                            workersNeeded = workersNeeded,
                            needType = if (urgencyType == "custom" || urgencyType == "tomorrow") "scheduled" else "urgent_now",
                            urgencyType = urgencyType,
                            contactNumber = contactNumber,
                            budgetText = "₹${pp.toInt()} per person",
                            perPersonPayment = pp,
                            totalPayment = total,
                            durationText = selectedDuration,
                            addressText = addressText,
                            radiusKm = 10.0,
                            scheduledAtMillis = if (urgencyType == "tomorrow") {
                                val zone = ZoneId.systemDefault()
                                LocalDate.now(zone).plusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
                            } else if (urgencyType == "custom") {
                                scheduledAtMillis // Ideally picked from a DatePicker if we implement it
                            } else 0L,
                            scheduledAtLabel = if (urgencyType == "tomorrow") "Tomorrow" else ""
                        )
                    ) { requestId ->
                        Toast.makeText(context, "Urgent need posted", Toast.LENGTH_SHORT).show()
                        onPosted(requestId)
                    }
                },
                enabled = !state.isPostingUrgentNeed && canPost,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
            ) {
                if (state.isPostingUrgentNeed) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp).size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                Text("Post Urgent Need")
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun UrgentNeedSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = EmployerColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
