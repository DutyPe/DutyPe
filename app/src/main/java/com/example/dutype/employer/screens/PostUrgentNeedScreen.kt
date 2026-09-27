package com.example.dutype.employer.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val defaultPay: Int = 600
)

private val URGENT_CATEGORIES = listOf(
    UrgentCategoryItem("loading_helper", "Loading Helper", "Godown & loading", "📦", 600),
    UrgentCategoryItem("driver", "Driver", "Auto, tempo, car", "🚚", 700),
    UrgentCategoryItem("cleaner", "Cleaner / Maid", "Shop & house clean", "🧹", 500),
    UrgentCategoryItem("cook", "Cook", "Hotel & home cook", "🍳", 600),
    UrgentCategoryItem("electrician", "Electrician", "Repair & wiring", "⚡", 800),
    UrgentCategoryItem("other", "Other Work", "Specify manually", "➕", 600)
)

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

    val durationOptions = listOf(
        "Half Day (4 hrs)",
        "Full Day (8 hrs)",
        "2-3 Days",
        "1 Week",
        "Monthly"
    )
    var selectedDuration by rememberSaveable { mutableStateOf("Full Day (8 hrs)") }
    var durationExpanded by rememberSaveable { mutableStateOf(false) }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top App Bar
        if (showTopBar) {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }

                    Text(
                        text = "Post Urgent Need",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // ⚡ 2-Hour Match Pill Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "2-Hour Match",
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Scrollable Body
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Urgency Hero Banner
            item(key = "urgent_banner") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFDC2626), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Need Workers Immediately?",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Your requirement is broadcasted to verified workers within 5 km. Interested workers will call you directly.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF7F1D1D),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 2. Select Work Category
            item(key = "work_category_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Work Category",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tap to choose",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    // 2-Column Grid (3 rows)
                    for (i in URGENT_CATEGORIES.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val item1 = URGENT_CATEGORIES[i]
                            UrgentCategoryCard(
                                item = item1,
                                isSelected = selectedCategoryItem == item1.id,
                                onClick = {
                                    selectedCategoryItem = item1.id
                                    perPersonPaymentText = item1.defaultPay.toString()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            if (i + 1 < URGENT_CATEGORIES.size) {
                                val item2 = URGENT_CATEGORIES[i + 1]
                                UrgentCategoryCard(
                                    item = item2,
                                    isSelected = selectedCategoryItem == item2.id,
                                    onClick = {
                                        selectedCategoryItem = item2.id
                                        perPersonPaymentText = item2.defaultPay.toString()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Manual input for "Other Work"
                    if (selectedCategoryItem == "other") {
                        OutlinedTextField(
                            value = otherCategory,
                            onValueChange = { otherCategory = it },
                            placeholder = {
                                Text(
                                    "Specify work (e.g. Plumber, Carpenter, Security)",
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFDC2626),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                }
            }

            // 3. When do you need workers?
            item(key = "timing_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "When do you need workers?",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card 1: Right Now (with FASTEST badge)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 6.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { urgencyType = "right_now" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (urgencyType == "right_now") Color(0xFFFEF2F2) else Color.White,
                                border = BorderStroke(
                                    if (urgencyType == "right_now") 1.5.dp else 1.dp,
                                    if (urgencyType == "right_now") Color(0xFFEF4444) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Right Now",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (urgencyType == "right_now") Color(0xFFDC2626) else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Within 2 hrs",
                                        fontSize = 10.5.sp,
                                        color = if (urgencyType == "right_now") Color(0xFF7F1D1D) else Color(0xFF64748B)
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-6).dp),
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = "FASTEST",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Card 2: Today
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 6.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { urgencyType = "today" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (urgencyType == "today") Color(0xFFFEF2F2) else Color.White,
                                border = BorderStroke(
                                    if (urgencyType == "today") 1.5.dp else 1.dp,
                                    if (urgencyType == "today") Color(0xFFEF4444) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Today",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (urgencyType == "today") Color(0xFFDC2626) else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Later today",
                                        fontSize = 10.5.sp,
                                        color = if (urgencyType == "today") Color(0xFF7F1D1D) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        // Card 3: Tomorrow
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 6.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { urgencyType = "tomorrow" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (urgencyType == "tomorrow") Color(0xFFFEF2F2) else Color.White,
                                border = BorderStroke(
                                    if (urgencyType == "tomorrow") 1.5.dp else 1.dp,
                                    if (urgencyType == "tomorrow") Color(0xFFEF4444) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Tomorrow",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (urgencyType == "tomorrow") Color(0xFFDC2626) else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Morning shift",
                                        fontSize = 10.5.sp,
                                        color = if (urgencyType == "tomorrow") Color(0xFF7F1D1D) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Workers Needed & Work Duration
            item(key = "workers_duration_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Column: Workers Needed Stepper
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Workers Needed",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .clickable { if (workersNeeded > 1) workersNeeded-- },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                }

                                Text(
                                    text = if (workersNeeded == 1) "1 Worker" else "$workersNeeded Workers",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .clickable { if (workersNeeded < 20) workersNeeded++ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                }
                            }
                        }
                    }

                    // Right Column: Work Duration Dropdown
                    Column(
                        modifier = Modifier.weight(1.15f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Work Duration",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable { durationExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = selectedDuration,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = durationExpanded,
                                onDismissRequest = { durationExpanded = false }
                            ) {
                                durationOptions.forEach { dur ->
                                    DropdownMenuItem(
                                        text = { Text(dur, fontSize = 13.sp) },
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

            // 5. Pay per worker (Cash or UPI)
            item(key = "payment_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pay per worker (Cash or UPI)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Standard APMC Rate",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF059669)
                        )
                    }

                    // Main Rate Display Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹ ",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                BasicTextField(
                                    value = perPersonPaymentText,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }.take(5)
                                        perPersonPaymentText = digits
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(110.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "per day / worker",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Quick Suggestion Chips
                    val paySuggestions = listOf(500, 600, 800, 1000)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paySuggestions.forEach { amount ->
                            val isSelected = perPersonPaymentText == amount.toString()
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { perPersonPaymentText = amount.toString() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFEF2F2) else Color.White,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFFEF4444) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (amount == 1000) {
                                            if (isSelected) "₹1,000 ✓" else "₹1,000"
                                        } else {
                                            if (isSelected) "₹$amount ✓" else "₹$amount"
                                        },
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFFDC2626) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Where should workers reach?
            item(key = "reach_section") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Where should workers reach?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Location Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFFEF2F2), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val displayLine1 = addressText.substringBefore(",").trim().ifBlank {
                                        if (hasEmployerLocation) "Current Worksite Location" else "Worksite Location"
                                    }
                                    val displayLine2 = addressText.substringAfter(",", "").trim().ifBlank {
                                        if (hasEmployerLocation) "GPS Verified · Nearby Broadcast" else "Tap change to detect GPS or set address"
                                    }
                                    Text(
                                        text = displayLine1,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = displayLine2,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Change",
                                    color = Color(0xFF2563EB),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        tempManualAddress = addressText
                                        showLocationDialog = true
                                    }
                                )
                            }

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            // Contact Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFDCFCE7), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Workers will call directly on:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = contactNumber.ifBlank { "+91 98765 43210" },
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC))
                                ) {
                                    Text(
                                        text = "Verified Owner",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error display if any
            if (!state.error.isNullOrBlank()) {
                item(key = "error_msg") {
                    Text(
                        text = state.error ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.Error),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // Docked Bottom Action Bar (Trust Strip + Big Red CTA)
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trust line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zero advance commission · Pay worker directly on completion",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF047857),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Red CTA Button
                Button(
                    onClick = {
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
                                return@Button
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
                                budgetText = "₹${pp.toInt()} per worker",
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
                    },
                    enabled = !state.isPostingUrgentNeed && canPost,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        disabledContainerColor = Color(0xFFFCA5A5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (state.isPostingUrgentNeed || isAutoPickingLocation) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Broadcast Urgent Need Now ⚡",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Change Address Dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Text(
                    text = "Worksite Location",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (locationService.hasLocationPermission()) {
                                scope.launch {
                                    autoPickEmployerLocation()
                                    showLocationDialog = false
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detect Current GPS Location")
                    }

                    Text("Or enter manual address:", fontSize = 12.sp, color = Color(0xFF64748B))

                    OutlinedTextField(
                        value = tempManualAddress,
                        onValueChange = { tempManualAddress = it },
                        placeholder = { Text("e.g. APMC Market Yard, Gate 2, Hubli") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempManualAddress.isNotBlank()) {
                            addressText = tempManualAddress.trim()
                            hasEmployerLocation = true
                        }
                        showLocationDialog = false
                    }
                ) {
                    Text("Save Address", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
private fun UrgentCategoryCard(
    item: UrgentCategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFFEF2F2) else Color.White,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) Color(0xFFEF4444) else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.emoji,
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF991B1B) else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 10.5.sp,
                    color = if (isSelected) Color(0xFF7F1D1D) else Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color(0xFFDC2626), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
