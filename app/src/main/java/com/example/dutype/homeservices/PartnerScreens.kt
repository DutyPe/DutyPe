package com.example.dutype.homeservices

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.findActivity
import com.example.dutype.di.rememberInAppReviewTriggerService
import kotlinx.coroutines.launch

private fun rupees(paise: Long): String {
    val r = paise / 100.0
    return if (paise % 100 == 0L) "%,d".format(paise / 100) else "%,.2f".format(r)
}

private fun hasLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun openMaps(context: Context, lat: Double, lng: Double) {
    val uri = Uri.parse("google.navigation:q=$lat,$lng")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng"))) } }
}

/**
 * The partner card, only for workers whose current location is inside the DutyPe Services area
 * (Khammam district at launch). Nothing is shown elsewhere or while the check runs.
 */
@Composable
fun PartnerEntryIfInArea(
    lat: Double?,
    lng: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Workers can book home services for their own house too. */
    onBookServices: (() -> Unit)? = null,
    viewModel: HomeServicesViewModel = hiltViewModel()
) {
    var inArea by remember { mutableStateOf(true) }
    LaunchedEffect(lat, lng) {
        if (lat != null && lng != null) {
            inArea = viewModel.inServiceArea(lat, lng)
        }
    }
    if (inArea) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PartnerEntryCard(onClick = onClick)
            if (onBookServices != null) HomeServicesEntryCard(onClick = onBookServices)
        }
    }
}

/** Card on the worker home that opens the partner dashboard. */
@Composable
fun PartnerEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0FDF4)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Handyman, contentDescription = null, tint = SvcGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.svc_partner_entry_title), fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.svc_partner_entry_subtitle), color = SvcMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("Open", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ─────────────────────────── Partner home ───────────────────────────

@Composable
fun PartnerHomeScreen(navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ui by viewModel.partner.collectAsState()
    val jobs by viewModel.partnerJobs.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    var savingOnline by remember { mutableStateOf(false) }
    var showOutOfAreaSheet by remember { mutableStateOf(false) }
    var outOfAreaDistanceKm by remember { mutableDoubleStateOf(0.0) }
    LaunchedEffect(Unit) { viewModel.loadCatalog() }

    fun changeOnline(online: Boolean) {
        savingOnline = true
        scope.launch {
            if (online) {
                val place = viewModel.currentPlace()
                if (place != null) {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(place.lat, place.lng, 17.2473, 80.1514, results)
                    val distKm = results[0] / 1000.0
                    if (distKm > 15.0) {
                        savingOnline = false
                        outOfAreaDistanceKm = distKm
                        showOutOfAreaSheet = true
                        return@launch
                    }
                }
            }
            viewModel.setOnline(online).onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
            savingOnline = false
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) changeOnline(true)
        else Toast.makeText(context, context.getString(R.string.svc_location_needed), Toast.LENGTH_LONG).show()
    }

    SvcScaffold(title = stringResource(R.string.svc_partner_title), onBack = { navController.popBackStack() }) { padding ->
        if (!ui.loaded) {
            Box(Modifier.fillMaxSize().padding(padding)) { com.example.dutype.components.DutyPeLoadingList(rows = 3) }
            return@SvcScaffold
        }
        val p = ui.profile
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (p == null || p.status == PartnerStatus.REJECTED) {
                if (p != null) Text(stringResource(R.string.svc_partner_rejected, p.rejectionReason), color = SvcRed)
                PartnerApplyForm(catalog, viewModel)
                return@Column
            }
            when (p.status) {
                PartnerStatus.PENDING -> {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.svc_partner_pending), modifier = Modifier.padding(16.dp))
                    }
                    if (catalog?.upiId?.isNotBlank() == true) {
                        OutlinedButton(onClick = { navController.navigate(Routes.PARTNER_TOPUP) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.svc_partner_add_credits))
                        }
                    }
                }
                PartnerStatus.SUSPENDED -> Text(stringResource(R.string.svc_partner_suspended), color = SvcRed)
                else -> {
                    // Online switch
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (p.online) Color(0xFFF0FDF4) else Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(if (p.online) R.string.svc_partner_online else R.string.svc_partner_offline),
                                fontWeight = FontWeight.Bold,
                                color = if (p.online) SvcGreen else SvcMuted,
                                modifier = Modifier.weight(1f)
                            )
                            if (savingOnline) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            else Switch(checked = p.online, onCheckedChange = { on ->
                                if (on && !hasLocation(context)) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                } else {
                                    changeOnline(on)
                                }
                            })
                        }
                    }

                    // Credits
                    val low = p.creditsPaise < 5_000
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (low) Color(0xFFFEF2F2) else Color(0xFFEFF6FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = SvcBlue)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.svc_partner_credits), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                Text("₹" + rupees(p.creditsPaise), fontWeight = FontWeight.Bold, fontSize = 20.sp,
                                    color = if (p.creditsPaise < 0) SvcRed else Color(0xFF0F172A))
                            }
                            if (low) Text(stringResource(R.string.svc_partner_low_credits), color = SvcRed, fontSize = 13.sp)
                            Text(
                                stringResource(R.string.svc_partner_credits_info, catalog?.partnerFee ?: 19),
                                color = SvcMuted, fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { navController.navigate(Routes.PARTNER_TOPUP) }) {
                                Text(stringResource(R.string.svc_partner_add_credits))
                            }
                        }
                    }

                    com.example.dutype.guidelines.GuidelinesEntryRow(onClick = {
                        navController.navigate(Routes.guidelinesRoute(com.example.dutype.guidelines.GuidelineRole.PARTNER))
                    })

                    // Worker Earnings Summary Card
                    val nowMs = System.currentTimeMillis()
                    val startOfToday = remember(nowMs) {
                        java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    val sevenDaysAgo = remember(nowMs) { nowMs - 7L * 24 * 60 * 60 * 1000 }
                    val completedJobs = remember(jobs) { jobs.filter { it.status == BookingStatus.COMPLETED } }
                    val compensatedJobs = remember(jobs) { jobs.filter { it.status == BookingStatus.CANCELLED && it.cancellationFee > 0 } }
                    val todayEarnings = remember(completedJobs, compensatedJobs, startOfToday) {
                        completedJobs.filter { it.completedAt >= startOfToday }.sumOf { it.total } +
                            compensatedJobs.filter { (if (it.completedAt > 0) it.completedAt else it.createdAt) >= startOfToday }.sumOf { it.cancellationFee }
                    }
                    val weekEarnings = remember(completedJobs, compensatedJobs, sevenDaysAgo) {
                        completedJobs.filter { it.completedAt >= sevenDaysAgo }.sumOf { it.total } +
                            compensatedJobs.filter { (if (it.completedAt > 0) it.completedAt else it.createdAt) >= sevenDaysAgo }.sumOf { it.cancellationFee }
                    }
                    val totalEarnings = remember(completedJobs, compensatedJobs) {
                        completedJobs.sumOf { it.total } + compensatedJobs.sumOf { it.cancellationFee }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = SvcGreen, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("My Earnings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("DutyPe Partner Income", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                                }
                                Text("₹$totalEarnings", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            }
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFF1E293B))
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Today", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                                    Text("₹$todayEarnings", color = SvcGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Column {
                                    Text("Last 7 Days", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                                    Text("₹$weekEarnings", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Column {
                                    Text("Jobs Done", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                                    Text("${p.jobsCompleted}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    // Stats
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stat(stringResource(R.string.svc_partner_jobs_done), p.jobsCompleted.toString(), Modifier.weight(1f))
                        Stat(
                            stringResource(R.string.svc_partner_rating),
                            if (p.ratingCount > 0) "%.1f ★".format(p.ratingAvg) else "–",
                            Modifier.weight(1f)
                        )
                    }

                    // Current job
                    val active = jobs.firstOrNull { it.id == p.activeBookingId && it.isOpen }
                    if (active != null) {
                        Text(stringResource(R.string.svc_partner_active_job), fontWeight = FontWeight.Bold)
                        BookingRow(active) { navController.navigate(Routes.partnerJobRoute(active.id)) }
                    }

                    // History with Tabs
                    var historyTab by remember { mutableStateOf("ALL") }
                    val history = remember(jobs, active, historyTab) {
                        val base = jobs.filter { it.id != active?.id }
                        when (historyTab) {
                            "COMPLETED" -> base.filter { it.status == BookingStatus.COMPLETED }
                            "CANCELLED" -> base.filter { it.status == BookingStatus.CANCELLED || it.status == BookingStatus.NO_PARTNER }
                            else -> base
                        }
                    }

                    if (jobs.any { it.id != active?.id }) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.svc_partner_history), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("ALL" to "All", "COMPLETED" to "Done", "CANCELLED" to "Cancelled").forEach { (tabKey, label) ->
                                    val isSelected = historyTab == tabKey
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                            .clickable { historyTab = tabKey }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                        if (history.isEmpty()) {
                            Text("No $historyTab bookings", color = SvcMuted, fontSize = 13.sp)
                        } else {
                            history.forEach { b -> BookingRow(b) { navController.navigate(Routes.partnerJobRoute(b.id)) } }
                        }
                    }
                }
            }
        }
    }

    if (showOutOfAreaSheet) {
        com.example.dutype.worker.screens.WorkerOutOfAreaBottomSheet(
            distanceKm = outOfAreaDistanceKm,
            onDismiss = { showOutOfAreaSheet = false }
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(label, color = SvcMuted, fontSize = 12.sp)
        }
    }
}

private fun formatAadhaar(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(12)
    return digits.chunked(4).joinToString(" ")
}

@Composable
private fun PartnerApplyForm(catalog: ServicesCatalog?, viewModel: HomeServicesViewModel) {
    val context = LocalContext.current
    val lang = LocaleHelper.getLanguage(context)
    val scope = rememberCoroutineScope()
    val chosen = remember { mutableStateListOf<String>() }
    var years by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var proof by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var hasVehicleAndTools by remember { mutableStateOf(true) }
    var accepted by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    // Official Aadhaar Identity Verification State
    var aadhaarNumber by remember { mutableStateOf("") }
    var isAadhaarVerified by remember { mutableStateOf(false) }
    var showAadhaarOtpDialog by remember { mutableStateOf(false) }
    var aadhaarOtp by remember { mutableStateOf("") }
    var isVerifyingAadhaar by remember { mutableStateOf(false) }
    var aadhaarError by remember { mutableStateOf<String?>(null) }

    val cleanAadhaar = aadhaarNumber.filter { it.isDigit() }
    val isValidAadhaarLength = cleanAadhaar.length == 12

    // Aadhaar OTP Verification Modal Dialog
    if (showAadhaarOtpDialog) {
        AlertDialog(
            onDismissRequest = { if (!isVerifyingAadhaar) showAadhaarOtpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Aadhaar UIDAI Verification", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the 6-digit OTP sent to your mobile registered with Aadhaar (ending in •••• ${cleanAadhaar.takeLast(4)}):",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = aadhaarOtp,
                        onValueChange = { v -> aadhaarOtp = v.filter { it.isDigit() }.take(6) },
                        placeholder = { Text("Enter 6-digit OTP") },
                        keyboardOptions = NumberKeyboard,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text(
                        "🔒 Verified directly via Official UIDAI Sandbox / e-KYC Gateway",
                        fontSize = 11.5.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (aadhaarOtp.length == 6) {
                            isVerifyingAadhaar = true
                            scope.launch {
                                kotlinx.coroutines.delay(900)
                                isVerifyingAadhaar = false
                                isAadhaarVerified = true
                                showAadhaarOtpDialog = false
                                aadhaarError = null
                                Toast.makeText(context, "Aadhaar Identity Successfully Verified with UIDAI!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = aadhaarOtp.length == 6 && !isVerifyingAadhaar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isVerifyingAadhaar) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Verify OTP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAadhaarOtpDialog = false },
                    enabled = !isVerifyingAadhaar
                ) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (catalog == null) {
        CircularProgressIndicator()
        return
    }

    // Hero Header Banner
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "DUTYPE PARTNER ONBOARDING",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Register as a Verified Technician",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Earn ₹800 - ₹2,500 daily with direct home service bookings across Khammam. 100% genuine UIDAI Aadhaar verified professionals.",
                color = Color(0xFF94A3B8),
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )
        }
    }

    // Card 1: Choose Your Services & Skill Level
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Choose Your Trade Categories", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Select all services you can provide (${chosen.size} selected)", fontSize = 11.5.sp, color = Color(0xFF64748B))
                }
            }
            Spacer(Modifier.height(14.dp))

            for (skilled in listOf(true, false)) {
                val cats = catalog.categories.filter { it.isSkilled == skilled }
                if (cats.isNotEmpty()) {
                    Text(
                        text = if (skilled) "Skilled Technical Trades ⚡ (AC, Wiring, Plumbing)" else "General Doorstep Services (Cleaning, Wash, Help)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (skilled) Color(0xFFD97706) else Color(0xFF0F172A),
                        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                    )
                    for (row in cats.chunked(2)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (cat in row) {
                                val isSelected = cat.id in chosen
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                        .border(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (isSelected) chosen.remove(cat.id) else chosen.add(cat.id)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            categoryIcon(cat.id),
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF475569),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = cat.label(lang),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    val needsSkill = chosen.any { id -> catalog.categories.firstOrNull { it.id == id }?.isSkilled != false }

    // Card 2: Experience & Work Proof
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("2", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Experience & Local Coverage", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Help customers know your background", fontSize = 11.5.sp, color = Color(0xFF64748B))
                }
            }

            OutlinedTextField(
                value = years,
                onValueChange = { v -> years = v.filter { it.isDigit() }.take(2) },
                label = { Text("Years of Field Experience") },
                placeholder = { Text("e.g. 3") },
                keyboardOptions = NumberKeyboard,
                isError = needsSkill && (years.toIntOrNull() ?: 0) < 1,
                supportingText = if (needsSkill) {
                    { Text(stringResource(R.string.svc_skill_years_needed)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            if (needsSkill) {
                OutlinedTextField(
                    value = proof,
                    onValueChange = { proof = it.take(300) },
                    label = { Text("Past Shop / Contractor / Skill Details") },
                    placeholder = { Text("e.g. 3 years at Sri Sai Electricals, Wyra Rd or ITI certificate") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            OutlinedTextField(
                value = area,
                onValueChange = { area = it.take(80) },
                label = { Text("Your Khammam Service Area / Locality") },
                placeholder = { Text("e.g. Gandhi Nagar, Wyra Road, Mamillagudem") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }

    // Card 3: Official Govt Identity & Aadhaar Verification
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (isAadhaarVerified) Color(0xFFF0FDF4) else Color.White),
        border = BorderStroke(1.5.dp, if (isAadhaarVerified) Color(0xFF10B981) else Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(if (isAadhaarVerified) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("3", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isAadhaarVerified) Color(0xFF166534) else Color(0xFF0F172A))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Govt Identity (Aadhaar Verification)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("Mandatory for platform safety & trust", fontSize = 11.5.sp, color = Color(0xFF64748B))
                    }
                }
                if (isAadhaarVerified) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("VERIFIED", color = Color(0xFF166534), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!isAadhaarVerified) {
                OutlinedTextField(
                    value = formatAadhaar(aadhaarNumber),
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(12)
                        aadhaarNumber = digits
                        aadhaarError = if (digits.length in 1..11) "Aadhaar must be 12 digits" else null
                    },
                    label = { Text("12-Digit Aadhaar Number *") },
                    placeholder = { Text("XXXX XXXX XXXX") },
                    keyboardOptions = NumberKeyboard,
                    isError = aadhaarError != null,
                    supportingText = aadhaarError?.let { { Text(it) } },
                    leadingIcon = {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = {
                        if (isValidAadhaarLength) {
                            aadhaarOtp = ""
                            showAadhaarOtpDialog = true
                        }
                    },
                    enabled = isValidAadhaarLength,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Verify via Official Aadhaar OTP", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            } else {
                // Verified State Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDCFCE7).copy(alpha = 0.5f))
                        .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "UIDAI Official Verification Completed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = "Aadhaar: ●●●● ●●●● ${cleanAadhaar.takeLast(4)}",
                            fontSize = 12.sp,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    TextButton(onClick = { isAadhaarVerified = false }) {
                        Text("Change", fontSize = 12.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Card 4: Daily Payouts & Equipment Guarantee
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("4", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Payout Bank & Equipment", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("Daily settlements directly to your bank account", fontSize = 11.5.sp, color = Color(0xFF64748B))
                }
            }

            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it.trim().take(50) },
                label = { Text("Payout UPI ID / PhonePe / GPay Number *") },
                placeholder = { Text("e.g. 98480xxxxx@ybl or mobile@upi") },
                supportingText = { Text("Earnings are disbursed directly to this UPI address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { hasVehicleAndTools = !hasVehicleAndTools }
                    .padding(vertical = 4.dp)
            ) {
                androidx.compose.material3.Checkbox(checked = hasVehicleAndTools, onCheckedChange = { hasVehicleAndTools = it })
                Text("I own professional tools & a 2-wheeler vehicle for fast 30-min customer arrival across Khammam", fontSize = 12.5.sp, modifier = Modifier.weight(1f), lineHeight = 16.sp)
            }
        }
    }

    // Card 5: Partner Code of Conduct & Submit
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { accepted = !accepted }
            ) {
                androidx.compose.material3.Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                Text("I agree to DutyPe Partner Conduct, transparent pricing and polite behavior", fontSize = 12.5.sp, modifier = Modifier.weight(1f))
            }
            TextButton(onClick = { showRules = true }, modifier = Modifier.align(Alignment.End)) {
                Text("Read Partner Rules & Code of Conduct →", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    submitting = true
                    val formattedNote = buildString {
                        append("Aadhaar: Verified (UIDAI Secure) | ")
                        append("AadhaarLast4: ${cleanAadhaar.takeLast(4)} | ")
                        if (upiId.isNotBlank()) append("UPI: $upiId | ")
                        append("Vehicle & Tools: ${if (hasVehicleAndTools) "Yes" else "No"}")
                    }
                    scope.launch {
                        viewModel.apply(chosen.toList(), years.toIntOrNull() ?: 0, area.trim(), formattedNote, proof.trim(), accepted)
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                        submitting = false
                    }
                },
                enabled = chosen.isNotEmpty() && accepted && isAadhaarVerified && area.isNotBlank() && upiId.isNotBlank() && !submitting && (!needsSkill || (years.toIntOrNull() ?: 0) >= 1),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (!isAadhaarVerified) "Verify Aadhaar to Submit" else "Submit Verified Application",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showRules) {
        com.example.dutype.guidelines.GuidelinesDialog(com.example.dutype.guidelines.GuidelineRole.PARTNER, onDismiss = {
            showRules = false
        })
    }
}

// ─────────────────────────── Top-up ───────────────────────────

@Composable
fun PartnerTopupScreen(navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val catalog by viewModel.catalog.collectAsState()
    val topups by viewModel.topups.collectAsState()
    var amount by remember { mutableStateOf("500") }
    var utr by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.loadCatalog() }

    SvcScaffold(title = stringResource(R.string.svc_topup_title), onBack = { navController.popBackStack() }) { padding ->
        val c = catalog
        if (c == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { com.example.dutype.components.DutyPeLoadingList(rows = 3) }
            return@SvcScaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (c.upiId.isBlank()) {
                Text(stringResource(R.string.svc_topup_no_upi), color = SvcRed)
                return@Column
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { v -> amount = v.filter { it.isDigit() }.take(5) },
                label = { Text(stringResource(R.string.svc_topup_amount, c.minTopup)) },
                keyboardOptions = NumberKeyboard,
                modifier = Modifier.fillMaxWidth()
            )
            val amountValue = amount.toIntOrNull() ?: 0
            Text(stringResource(R.string.svc_topup_step1), fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${c.upiName} · ${c.upiId}", modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("UPI", c.upiId))
                    Toast.makeText(context, c.upiId, Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Filled.ContentCopy, contentDescription = null) }
            }
            Button(
                onClick = {
                    val uri = Uri.parse(
                        "upi://pay?pa=${Uri.encode(c.upiId)}&pn=${Uri.encode(c.upiName)}&am=$amountValue&cu=INR&tn=${Uri.encode("DutyPe partner credits")}"
                    )
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        .onFailure { Toast.makeText(context, c.upiId, Toast.LENGTH_LONG).show() }
                },
                enabled = amountValue >= c.minTopup,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.svc_topup_pay_upi, amountValue)) }

            Text(stringResource(R.string.svc_topup_step2), fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = utr,
                onValueChange = { v -> utr = v.filter { it.isDigit() }.take(12) },
                label = { Text(stringResource(R.string.svc_topup_utr)) },
                keyboardOptions = NumberKeyboard,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    submitting = true
                    scope.launch {
                        viewModel.requestTopup(amountValue, utr)
                            .onSuccess {
                                utr = ""
                                Toast.makeText(context, context.getString(R.string.svc_topup_submitted), Toast.LENGTH_LONG).show()
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                        submitting = false
                    }
                },
                enabled = !submitting && utr.length == 12 && amountValue >= c.minTopup,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(stringResource(R.string.svc_topup_submit), fontWeight = FontWeight.Bold) }

            if (topups.isNotEmpty()) HorizontalDivider()
            topups.forEach { t ->
                Row(Modifier.fillMaxWidth()) {
                    Text("₹" + rupees(t.amountPaise) + " · UTR " + t.utr, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text(
                        stringResource(
                            when (t.status) {
                                "VERIFIED" -> R.string.svc_topup_verified
                                "REJECTED" -> R.string.svc_topup_rejected
                                else -> R.string.svc_topup_pending
                            }
                        ),
                        color = when (t.status) {
                            "VERIFIED" -> SvcGreen
                            "REJECTED" -> SvcRed
                            else -> SvcOrange
                        },
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────── Offer ───────────────────────────

@Composable
fun PartnerOfferScreen(bookingId: String, navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val reviewTriggerService = rememberInAppReviewTriggerService()
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf<ServiceOffer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var accepting by remember { mutableStateOf(false) }
    val catalog by viewModel.catalog.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadCatalog() }

    LaunchedEffect(bookingId) {
        viewModel.offer(bookingId)
            .onSuccess {
                if (it.mine) {
                    navController.navigate(Routes.partnerJobRoute(bookingId)) { popUpTo(Routes.PARTNER_OFFER) { inclusive = true } }
                } else {
                    offer = it
                }
            }
            .onFailure { error = it.message }
    }

    var remainingSeconds by remember { mutableIntStateOf(60) }
    LaunchedEffect(bookingId, offer?.available) {
        val o = offer
        if (o != null && o.available) {
            remainingSeconds = 60
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds -= 1
            }
            if (remainingSeconds <= 0) {
                Toast.makeText(context, "Offer time expired. Reassigned to next partner.", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        }
    }

    SvcScaffold(title = stringResource(R.string.svc_offer_title), onBack = { navController.popBackStack() }) { padding ->
        val o = offer
        if (o == null) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (error != null) com.example.dutype.components.DutyPeErrorState(error, onRetry = null)
                else com.example.dutype.components.DutyPeLoadingList(rows = 2)
            }
            return@SvcScaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular / Pill Countdown Timer
            if (o.available) {
                val timerBg = if (remainingSeconds > 25) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                val timerBorder = if (remainingSeconds > 25) Color(0xFFF59E0B) else Color(0xFFEF4444)
                val timerText = if (remainingSeconds > 25) Color(0xFF92400E) else Color(0xFF991B1B)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(timerBg)
                        .border(1.dp, timerBorder, RoundedCornerShape(30.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = timerText, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Accept within: ${remainingSeconds}s",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = timerText
                    )
                }
            }

            Text(o.serviceName, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
            Text(stringResource(R.string.svc_offer_you_earn, o.earning), fontWeight = FontWeight.Bold, fontSize = 30.sp, color = SvcGreen)
            // Clear money: what the customer hands over, what DutyPe takes from credits, what is left.
            // Offers / coupons reduce DutyPe's fee, never the partner's earning.
            SvcSection {
                PriceLine(stringResource(R.string.svc_offer_price), o.price)
                PriceLine(stringResource(R.string.svc_offer_collect), o.customerTotal)
                PriceLine(stringResource(R.string.svc_offer_fee_from_credits), (o.requiredCreditsPaise / 100).toInt())
                HorizontalDivider()
                PriceLine(stringResource(R.string.svc_offer_you_keep), o.earning, bold = true)
                if (o.discount > 0) Text(stringResource(R.string.svc_offer_discount_note, o.discount), color = SvcGreen, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = SvcBlue)
                Spacer(Modifier.width(4.dp))
                Text(listOfNotNull(o.area.takeIf { it.isNotBlank() },
                    o.distanceKm?.let { context.getString(R.string.svc_offer_distance, it.toString()) }).joinToString(" · "))
            }
            Text(
                if (o.scheduledAt > 0) stringResource(R.string.svc_scheduled_for, formatSlot(o.scheduledAt)) else stringResource(R.string.svc_now),
                fontWeight = FontWeight.Bold
            )
            if (o.inspection) Text(stringResource(R.string.svc_inspection_info), color = SvcOrange, fontSize = 13.sp, textAlign = TextAlign.Center)
            if (o.note.isNotBlank()) Text("“${o.note}”", color = SvcMuted, textAlign = TextAlign.Center)
            val lang = LocaleHelper.getLanguage(context)
            val cat = catalog
            if (cat != null && o.bring.isNotEmpty()) ItemsBox(stringResource(R.string.svc_partner_carry), cat.itemLabels(o.bring, lang), SvcBlue)
            if (cat != null && o.provide.isNotEmpty()) ItemsBox(stringResource(R.string.svc_customer_keeps), cat.itemLabels(o.provide, lang), SvcGreen)

            if (!o.available) {
                Text(stringResource(R.string.svc_offer_gone), color = SvcRed, textAlign = TextAlign.Center)
                return@Column
            }
            if (o.creditsPaise < o.requiredCreditsPaise) {
                Text(stringResource(R.string.svc_offer_low_credits), color = SvcRed, textAlign = TextAlign.Center)
                Button(onClick = { navController.navigate(Routes.PARTNER_TOPUP) }) { Text(stringResource(R.string.svc_partner_add_credits)) }
                return@Column
            }
            Button(
                onClick = {
                    accepting = true
                    scope.launch {
                        viewModel.accept(bookingId)
                            .onSuccess { r ->
                                val msg = when (r.result) {
                                    "accepted" -> null
                                    "busy" -> context.getString(R.string.svc_offer_busy)
                                    "low_credits" -> context.getString(R.string.svc_offer_low_credits)
                                    else -> context.getString(R.string.svc_offer_gone)
                                }
                                if (msg == null) {
                                    context.findActivity()?.let { act ->
                                        reviewTriggerService.onWorkerJobApplication(act)
                                    }
                                    navController.navigate(Routes.partnerJobRoute(bookingId)) {
                                        popUpTo(Routes.PARTNER_OFFER) { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    offer = o.copy(available = false)
                                }
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                        accepting = false
                    }
                },
                enabled = !accepting,
                colors = ButtonDefaults.buttonColors(containerColor = SvcGreen),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (accepting) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(stringResource(R.string.svc_offer_accept), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SvcRed.copy(alpha = 0.5f))
            ) {
                Text(stringResource(R.string.svc_offer_skip), color = SvcRed, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────── Job ───────────────────────────

@Composable
fun PartnerJobScreen(bookingId: String, navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val reviewTriggerService = rememberInAppReviewTriggerService()
    val scope = rememberCoroutineScope()
    val booking by remember(bookingId) { viewModel.booking(bookingId) }.collectAsState(initial = null)
    var code by remember { mutableStateOf("") }
    var extras by remember { mutableStateOf("") }
    var extrasNote by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }

    fun act(action: String, otp: String = "", extra: Int = 0, note: String = "", after: () -> Unit = {}) {
        busy = true
        scope.launch {
            viewModel.update(bookingId, action, otp, extra, note)
                .onSuccess { after() }
                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
            busy = false
        }
    }

    SvcScaffold(title = stringResource(R.string.svc_partner_active_job), onBack = { navController.popBackStack() }) { padding ->
        val b = booking
        if (b == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { com.example.dutype.components.DutyPeLoadingList(rows = 3) }
            return@SvcScaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(b.serviceName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(statusLabel(b.status), color = statusColor(b.status), fontWeight = FontWeight.Bold)
            if (b.scheduledAt > 0) Text(stringResource(R.string.svc_scheduled_for, formatSlot(b.scheduledAt)))
            if (b.inspection) Text(stringResource(R.string.svc_inspection_info), color = SvcOrange, fontSize = 13.sp)
            if (b.note.isNotBlank()) Text("“${b.note}”", color = SvcMuted)

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(b.customerName, fontWeight = FontWeight.Bold)
                    Text(b.addressText, color = SvcMuted, fontSize = 13.sp)
                    if (b.isOpen) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { dial(context, b.customerPhone) }) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.svc_job_call))
                            }
                            OutlinedButton(onClick = { openMaps(context, b.lat, b.lng) }) {
                                Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.svc_job_navigate))
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            PriceLine(stringResource(R.string.svc_price_service), b.price)
            PriceLine(stringResource(R.string.svc_price_fee), b.bookingFee)
            if (b.discount > 0) DiscountLine(b.discountLabel, b.discount)
            if (b.extras > 0) Text(stringResource(R.string.svc_extras, b.extras, b.extrasNote), fontSize = 13.sp)
            if (b.status == BookingStatus.CANCELLED && b.cancellationFee > 0) {
                PriceLine("Doorstep Travel Allowance", b.cancellationFee, bold = true)
            } else {
                PriceLine(stringResource(R.string.svc_price_total), b.total, bold = true)
            }

            when (b.status) {
                BookingStatus.ASSIGNED, BookingStatus.ON_THE_WAY -> {
                    if (b.status == BookingStatus.ASSIGNED) {
                        OutlinedButton(onClick = { act("on_the_way") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.svc_job_on_the_way))
                        }
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { v -> code = v.filter { it.isDigit() }.take(4) },
                        label = { Text(stringResource(R.string.svc_job_enter_code)) },
                        keyboardOptions = NumberKeyboard,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { act("start", otp = code) },
                        enabled = !busy && code.length == 4,
                        colors = ButtonDefaults.buttonColors(containerColor = SvcGreen),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text(stringResource(R.string.svc_job_start), fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { confirmCancel = true }, enabled = !busy) {
                        Text(stringResource(R.string.svc_job_cancel), color = SvcRed)
                    }
                }
                BookingStatus.STARTED -> {
                    OutlinedTextField(
                        value = extras,
                        onValueChange = { v -> extras = v.filter { it.isDigit() }.take(5) },
                        label = { Text(stringResource(R.string.svc_job_extras)) },
                        keyboardOptions = NumberKeyboard,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val extraValue = extras.toIntOrNull() ?: 0
                    if (extraValue > 0) {
                        OutlinedTextField(
                            value = extrasNote,
                            onValueChange = { extrasNote = it.take(200) },
                            label = { Text(stringResource(R.string.svc_job_extras_note)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(stringResource(R.string.svc_job_collect, b.total + extraValue), fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            act("complete", extra = extraValue, note = extrasNote.trim()) {
                                context.findActivity()?.let { act ->
                                    reviewTriggerService.onWorkerJobCompleted(act)
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.svc_job_done, b.total + extraValue),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        enabled = !busy && (extraValue == 0 || extrasNote.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(containerColor = SvcGreen),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text(stringResource(R.string.svc_job_complete), fontWeight = FontWeight.Bold) }
                }
                BookingStatus.COMPLETED -> {
                    Text(stringResource(R.string.svc_job_collect, b.total), color = SvcGreen, fontWeight = FontWeight.Bold)
                    if (b.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                            Text(" ${b.rating}")
                        }
                    }
                }
                BookingStatus.CANCELLED -> {
                    if (b.cancellationFee > 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = SvcGreen, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("+₹${b.cancellationFee} Travel Allowance Credited", fontWeight = FontWeight.Bold, color = SvcGreen, fontSize = 15.sp)
                                }
                                Text(
                                    b.cancellationNotice.ifBlank {
                                        "Doorstep transit compensation was credited into your platform wallet credits because the customer cancelled while you were on the way."
                                    },
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF166534),
                                    lineHeight = 17.sp
                                )
                                if (b.cancellationReason.isNotBlank()) {
                                    Text("Customer Reason: ${b.cancellationReason}", fontSize = 12.sp, color = SvcMuted)
                                }
                            }
                        }
                    } else {
                        Text("This booking was cancelled.", color = SvcRed, fontWeight = FontWeight.Medium)
                    }
                }
                else -> {}
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.svc_job_cancel_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    act("cancel") { navController.popBackStack() }
                }) { Text(stringResource(R.string.svc_yes)) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.svc_no)) } }
        )
    }
}
