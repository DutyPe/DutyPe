package com.example.dutype.employer.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.core.content.ContextCompat
import com.dutype.app.R
import com.example.dutype.components.CommonHeader
import com.example.dutype.firestore.FirestoreCollections
import com.example.dutype.models.QuickUrgentNeedInput
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.JobCategoryResolver
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

        val context = androidx.compose.ui.platform.LocalContext.current
        val subscriptionViewModel: com.example.dutype.viewmodels.SubscriptionViewModel = hiltViewModel()
        val employerSubscription by subscriptionViewModel.activeSubscription.collectAsStateWithLifecycle()

        PostUrgentNeedContent(
            viewModel = viewModel,
            onPosted = { requestId ->
                navController.navigate(Routes.employerUrgentNeedDetailRoute(requestId)) {
                    // Keep dashboard in back stack so system/app-bar back returns home.
                    popUpTo(Routes.EMPLOYER_DASHBOARD) { inclusive = false }
                    launchSingleTop = true
                }
            },
            onInsufficientCredits = {
                if (employerSubscription.status != "LOADING" && employerSubscription.normalCredits <= 0) {
                    android.widget.Toast.makeText(context, "Please purchase a subscription to post jobs", android.widget.Toast.LENGTH_LONG).show()
                    navController.navigate(Routes.EMPLOYER_SUBSCRIPTION)
                    true
                } else {
                    false
                }
            }
        )
    }
}

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
    val urgentNeedPostedText = stringResource(R.string.urgent_need_posted_toast)
    val locationPermissionRequiredText = stringResource(R.string.location_permission_required_current)
    val locationUpdatedText = stringResource(R.string.location_updated)

    var title by rememberSaveable { mutableStateOf("") }
    var needType by rememberSaveable { mutableStateOf("urgent_now") }
    var workersNeededText by rememberSaveable { mutableStateOf("1") }
    var budgetText by rememberSaveable { mutableStateOf("") }
    val radiusKm = 10.0
    var notes by rememberSaveable { mutableStateOf("") }
    var contactNumber by rememberSaveable { mutableStateOf("") }
    var hasEmployerLocation by rememberSaveable { mutableStateOf(false) }
    var isAutoPickingLocation by rememberSaveable { mutableStateOf(false) }
    var urgentLocationError by rememberSaveable { mutableStateOf<String?>(null) }

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
        return valid
    }

    suspend fun autoPickEmployerLocation(): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrBlank()) return false
        if (!locationService.hasLocationPermission()) {
            urgentLocationError = locationPermissionRequiredText
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
                    "businessAddress" to locationInfo.getFullAddress(),
                    "businessLocation" to mapOf(
                        "lat" to locationInfo.latitude,
                        "lng" to locationInfo.longitude
                    ),
                    "geohash" to GeoUtils.encodeGeohash(locationInfo.latitude, locationInfo.longitude),
                    "updatedAt" to Timestamp.now()
                )
                FirebaseFirestore.getInstance()
                    .collection(FirestoreCollections.EMPLOYER_PROFILES)
                    .document(userId)
                    .set(updateData, SetOptions.merge())
                    .await()
                hasEmployerLocation = true
                urgentLocationError = null
                Toast.makeText(context, locationUpdatedText, Toast.LENGTH_SHORT).show()
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
            urgentLocationError = locationPermissionRequiredText
            Toast.makeText(context, locationPermissionRequiredText, Toast.LENGTH_SHORT).show()
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

    val inferredCategory = JobCategoryResolver.inferCategory(title, notes)
    val effectiveCategory = inferredCategory?.displayName ?: "Helper"
    val workersNeeded = workersNeededText.toIntOrNull()?.takeIf { it in 1..20 }
    val scheduleLabel = if (needType == "scheduled") {
        buildTomorrowScheduleLabel()
    } else {
        ""
    }
    val canPost = title.trim().length >= 3 &&
        contactNumber.trim().isNotBlank() &&
        workersNeeded != null &&
        hasEmployerLocation &&
        !isAutoPickingLocation

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            UrgentNeedSectionCard(title = stringResource(R.string.urgent_work_details)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.urgent_work_needed)) },
                    placeholder = { Text(stringResource(R.string.urgent_work_needed_hint)) },
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                AutoPickedUrgentCategory(
                    category = effectiveCategory,
                    hasTitle = title.trim().length >= 3
                )

                OutlinedTextField(
                    value = workersNeededText,
                    onValueChange = { value -> workersNeededText = value.filter { it.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.urgent_workers_needed)) },
                    placeholder = { Text("2") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text(stringResource(R.string.urgent_workers_needed_help)) },
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.urgent_work_details)) },
                    placeholder = { Text(stringResource(R.string.urgent_work_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        item {
            UrgentNeedSectionCard(title = stringResource(R.string.urgent_timing)) {
                SectionLabel(stringResource(R.string.urgent_when))
                val timingOptions = listOf(
                    "urgent_now" to stringResource(R.string.urgent_now),
                    "today" to stringResource(R.string.today),
                    "scheduled" to stringResource(R.string.urgent_tomorrow)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(timingOptions) { option ->
                        FilterChip(
                            selected = needType == option.first,
                            onClick = { needType = option.first },
                            label = { Text(option.second) },
                            leadingIcon = if (needType == option.first) {
                                { Icon(Icons.Default.Schedule, contentDescription = null) }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmployerColors.WarningLight,
                                selectedLabelColor = EmployerColors.Warning
                            )
                        )
                    }
                }

                Text(
                    text = if (needType == "scheduled") {
                        stringResource(R.string.urgent_scheduled_expiry, scheduleLabel)
                    } else {
                        stringResource(R.string.urgent_today_expiry)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                )

                if (needType == "scheduled") {
                    Text(
                        text = scheduleLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EmployerColors.Success,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        item {
            UrgentNeedSectionCard(title = stringResource(R.string.urgent_pay_reach)) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text(stringResource(R.string.urgent_budget)) },
                    placeholder = { Text(stringResource(R.string.urgent_budget_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

            }
        }

        item {
            UrgentNeedSectionCard(title = stringResource(R.string.work_location)) {
                if (hasEmployerLocation) {
                    Text(
                        text = stringResource(R.string.location_updated),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EmployerColors.Success,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = stringResource(R.string.urgent_workers_within_10km_notified),
                        style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.location_permission_required_current),
                        style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                    )

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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAutoPickingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        }
                        Text(text = stringResource(R.string.enable_location))
                    }

                    if (!urgentLocationError.isNullOrBlank()) {
                        Text(
                            text = urgentLocationError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.Error)
                        )
                    }
                }
            }
        }

        item {
            UrgentNeedSectionCard(title = stringResource(R.string.contact_number)) {
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { value -> contactNumber = value },
                    label = { Text(stringResource(R.string.contact_number)) },
                    placeholder = { Text(stringResource(R.string.urgent_phone_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
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

        if (showIntroCard) {
            item {
                UrgentNeedInfoCard(
                    needType = needType,
                    scheduleLabel = scheduleLabel
                )
            }
        }

        item {
            Button(
                onClick = {
                    if (onInsufficientCredits()) return@Button
                    viewModel.createUrgentNeed(
                        QuickUrgentNeedInput(
                            title = title,
                            description = notes,
                            category = effectiveCategory,
                            workersNeeded = workersNeeded ?: 1,
                            needType = needType,
                            contactNumber = contactNumber,
                            budgetText = budgetText,
                            radiusKm = radiusKm,
                            scheduledAtMillis = if (needType == "scheduled") {
                                buildTomorrowScheduleMillis()
                            } else {
                                0L
                            },
                            scheduleLabel = scheduleLabel
                        )
                    ) { requestId ->
                        Toast.makeText(context, urgentNeedPostedText, Toast.LENGTH_SHORT).show()
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
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
                Text(stringResource(R.string.post_urgent_need_title))
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun AutoPickedUrgentCategory(category: String, hasTitle: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = stringResource(R.string.urgent_auto_category),
            style = MaterialTheme.typography.labelMedium.copy(
                color = EmployerColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            text = if (hasTitle) category else stringResource(R.string.urgent_auto_category_waiting),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = EmployerColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun UrgentNeedInfoCard(
    needType: String,
    scheduleLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.urgent_jobs_expire_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = EmployerColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = if (needType == "scheduled") {
                    stringResource(R.string.urgent_scheduled_expiry, scheduleLabel)
                } else {
                    stringResource(R.string.urgent_today_expiry)
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary)
            )
            Text(
                text = stringResource(R.string.urgent_workers_within_10km_notified),
                style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary)
            )
        }
    }
}

@Composable
private fun UrgentNeedSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            color = EmployerColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    )
}

private fun buildTomorrowScheduleMillis(): Long {
    val zone = ZoneId.systemDefault()
    return LocalDate.now(zone)
        .plusDays(1)
        .atTime(9, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}

private fun buildTomorrowScheduleLabel(): String {
    val date = LocalDate.now(ZoneId.systemDefault()).plusDays(1)
    val dateText = date.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))
    return "Tomorrow, $dateText"
}
