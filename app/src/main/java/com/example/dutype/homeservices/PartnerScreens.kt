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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.dutype.utils.AadhaarVerifier
import kotlinx.coroutines.tasks.await
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
import kotlinx.coroutines.isActive
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
                    .background(Color(0xFF2563EB))
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Application Under Review", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 15.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.svc_partner_pending), color = Color(0xFF64748B), fontSize = 12.5.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                }
                PartnerStatus.SUSPENDED -> Text(stringResource(R.string.svc_partner_suspended), color = SvcRed)
                else -> {
                    // Partner Onboarding Phase Status Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFDBEAFE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🚀", fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Partner Onboarding Active ⚡",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1E3A8A)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "You are an approved DutyPe Service Partner! We are currently onboarding local technicians across Khammam. Once customer doorstep bookings go live, orders will be dispatched directly to your phone.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF),
                                    lineHeight = 16.5.sp
                                )
                            }
                        }
                    }

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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFDCFCE7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = SvcGreen, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("My Earnings", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("DutyPe Partner Income", color = Color(0xFF64748B), fontSize = 11.5.sp)
                                }
                                Text("₹$totalEarnings", color = Color(0xFF0F172A), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            }
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Today", color = Color(0xFF64748B), fontSize = 11.5.sp)
                                    Text("₹$todayEarnings", color = SvcGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Column {
                                    Text("Last 7 Days", color = Color(0xFF64748B), fontSize = 11.5.sp)
                                    Text("₹$weekEarnings", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Column {
                                    Text("Jobs Done", color = Color(0xFF64748B), fontSize = 11.5.sp)
                                    Text("${p.jobsCompleted}", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

// =============================================================================
// STITCH DESIGN SPEC TOKENS FOR PARTNER ONBOARDING
// =============================================================================
private val StitchNavy = Color(0xFF0F172A)
private val StitchGreen = Color(0xFF00B159)
private val StitchBorder = Color(0xFFE2E8F0)
private val StitchLabel = Color(0xFF64748B)

@Composable
private fun PartnerApplyForm(catalog: ServicesCatalog?, viewModel: HomeServicesViewModel) {
    val context = LocalContext.current
    val lang = LocaleHelper.getLanguage(context)
    val scope = rememberCoroutineScope()
    val chosen = remember { mutableStateListOf<String>() }
    var customServiceInput by remember { mutableStateOf("") }
    val customServicesList = remember { mutableStateListOf<String>() }
    var years by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var proof by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var hasVehicleAndTools by remember { mutableStateOf(true) }
    var acceptedConductRules by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showConductRulesSheet by remember { mutableStateOf(false) }
    var showVisualGuideSheet by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    // Aadhaar State (Mandatory verified e-KYC for Service Partners)
    var aadhaarNumber by remember { mutableStateOf("") }
    val cleanAadhaar = AadhaarVerifier.clean(aadhaarNumber)
    val aadhaarValidation = remember(cleanAadhaar) { AadhaarVerifier.validate(cleanAadhaar) }
    var isAadhaarVerified by remember { mutableStateOf(false) }
    var verifiedMaskedAadhaar by remember { mutableStateOf("") }
    var showAadhaarVerificationSheet by remember { mutableStateOf(false) }

    if (catalog == null) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = StitchGreen)
        }
        return
    }

    val needsSkill = chosen.any { id -> catalog.categories.firstOrNull { it.id == id }?.isSkilled != false }

    // Hero Header Banner
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, StitchBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFECFDF5))
                    .border(1.dp, Color(0xFF00B159).copy(alpha = 0.3f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF00B159), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.partner_apply_badge),
                    color = Color(0xFF065F46),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.partner_apply_hero_title),
                color = StitchNavy,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.partner_apply_hero_subtitle),
                color = StitchLabel,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(14.dp))
            // 3 trust perks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "⚡ Daily Direct UPI",
                    "🛡️ Verified Jobs",
                    "📍 Nearby (0-5 km)"
                ).forEach { perk ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = perk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    // Interactive Visual Partner Guide CTA Banner
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showVisualGuideSheet = true },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = BorderStroke(1.dp, Color(0xFF86EFAC))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00B159)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "👀 Visual Partner Guide & Video",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = Color(0xFF065F46)
                )
                Text(
                    "See what service partners do, how to work, and get paid",
                    fontSize = 11.5.sp,
                    color = Color(0xFF047857)
                )
            }
            Text(
                "View →",
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = Color(0xFF00B159)
            )
        }
    }

    // Section 1: Choose 3 to 5 Services (Strict 3-5 skill limitation)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, StitchBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFECFDF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StitchGreen)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "SELECT 3 TO 5 SERVICES *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = StitchLabel,
                        letterSpacing = 0.5.sp
                    )
                    val countColor = when {
                        chosen.size < 3 -> Color(0xFFD97706) // Amber
                        chosen.size in 3..5 -> Color(0xFF00B159) // Emerald
                        else -> Color(0xFFEF4444)
                    }
                    val countStatus = when {
                        chosen.size == 0 -> "Choose 3 to 5 services (0/5 selected)"
                        chosen.size < 3 -> "${chosen.size}/5 selected (Select ${3 - chosen.size} more)"
                        chosen.size in 3..5 -> "✓ ${chosen.size} of 5 services selected (Ready)"
                        else -> "Maximum 5 services allowed"
                    }
                    Text(
                        text = countStatus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = countColor
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            for (skilled in listOf(true, false)) {
                val cats = catalog.categories.filter { it.isSkilled == skilled }
                if (cats.isNotEmpty()) {
                    Text(
                        text = stringResource(if (skilled) R.string.partner_skilled_trades else R.string.partner_general_trades),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (skilled) Color(0xFFD97706) else StitchNavy,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                    for (row in cats.chunked(3)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (cat in row) {
                                val isSelected = cat.id in chosen
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clickable {
                                            if (isSelected) {
                                                chosen.remove(cat.id)
                                            } else {
                                                if (chosen.size >= 5) {
                                                    Toast.makeText(
                                                        context,
                                                        "You can select up to 5 services only (3-5 skills).",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    chosen.add(cat.id)
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(0xFF00B159) else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = cat.label(lang),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF065F46) else Color(0xFF334155),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            for (i in 0 until (3 - row.size)) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // Custom Service Entry Field
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(12.dp))

            Text(
                text = "OTHER / CUSTOM SERVICE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StitchLabel,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Can't find your trade in the list? Type it below to add it to your profile:",
                fontSize = 11.5.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ENTER CUSTOM SERVICE OR SKILL",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchLabel,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customServiceInput,
                        onValueChange = { customServiceInput = it.take(40) },
                        placeholder = {
                            Text(
                                "E.g. Mehendi Artist, Boutique, Patient Care...",
                                fontSize = 13.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Handyman, contentDescription = null, tint = Color(0xFF00B159), modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B159),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                    Button(
                        onClick = {
                            val trimmed = customServiceInput.trim()
                            if (trimmed.isNotBlank()) {
                                if (trimmed !in chosen) {
                                    if (chosen.size >= 5) {
                                        Toast.makeText(
                                            context,
                                            "Maximum 5 services allowed. Please remove a service first.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        chosen.add(trimmed)
                                        if (trimmed !in customServicesList) customServicesList.add(trimmed)
                                        customServiceInput = ""
                                    }
                                }
                            }
                        },
                        enabled = customServiceInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00B159),
                            disabledContainerColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.height(56.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text("+ Add", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                    }
                }
            }

            // Display added custom services chips with delete button
            if (customServicesList.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                for (row in customServicesList.chunked(3)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (custom in row) {
                            val isSelected = custom in chosen
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clickable {
                                        if (isSelected) {
                                            chosen.remove(custom)
                                        } else {
                                            if (chosen.size >= 5) {
                                                Toast.makeText(
                                                    context,
                                                    "Maximum 5 services allowed.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                chosen.add(custom)
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Color(0xFF00B159) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = custom,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF065F46) else Color(0xFF334155),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                customServicesList.remove(custom)
                                                chosen.remove(custom)
                                            }
                                    )
                                }
                            }
                        }
                        for (i in 0 until (3 - row.size)) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // Section 2: Experience & Local Coverage
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, StitchBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFECFDF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("2", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StitchGreen)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "EXPERIENCE & PREFERRED WORK LOCALITY *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = StitchLabel,
                        letterSpacing = 0.5.sp
                    )
                    Text(stringResource(R.string.partner_section_exp_sub), fontSize = 12.sp, color = StitchLabel)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "YEARS OF EXPERIENCE *",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchLabel,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = years,
                    onValueChange = { v -> years = v.filter { it.isDigit() }.take(2) },
                    placeholder = { Text(stringResource(R.string.partner_exp_years_hint), fontSize = 13.5.sp) },
                    keyboardOptions = NumberKeyboard,
                    leadingIcon = {
                        Icon(Icons.Filled.Handyman, contentDescription = null, tint = Color(0xFF00B159), modifier = Modifier.size(20.dp))
                    },
                    isError = needsSkill && (years.toIntOrNull() ?: 0) < 1,
                    supportingText = if (needsSkill) {
                        { Text(stringResource(R.string.svc_skill_years_needed), fontSize = 11.5.sp) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00B159),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        errorBorderColor = Color(0xFFEF4444)
                    )
                )
            }

            if (needsSkill) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SKILL SUMMARY & PAST WORK *",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchLabel,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = proof,
                        onValueChange = { proof = it.take(300) },
                        placeholder = { Text(stringResource(R.string.partner_skill_details_hint), fontSize = 13.5.sp) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B159),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "PREFERRED SERVICE AREA / NEIGHBORHOOD *",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchLabel,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it.take(80) },
                    placeholder = { Text(stringResource(R.string.partner_area_hint), fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF00B159), modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00B159),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )
            }
        }
    }

    // Section 3: Identity (12-Digit Aadhaar with Mandatory Verification) & Bank Payouts
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, StitchBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFECFDF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("3", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StitchGreen)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "IDENTITY (MANDATORY AADHAAR VERIFICATION) *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = StitchLabel,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "Service Partners must verify Aadhaar before receiving customer orders",
                        fontSize = 12.sp,
                        color = StitchLabel
                    )
                }
            }

            // Aadhaar Verification Block
            if (isAadhaarVerified) {
                // Verified State Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.5.dp, Color(0xFF00B159))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00B159)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "✓ Aadhaar Verified (Govt ID Confirmed)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                "ID: $verifiedMaskedAadhaar • 256-bit Encrypted",
                                fontSize = 11.5.sp,
                                color = Color(0xFF047857)
                            )
                        }
                        TextButton(
                            onClick = {
                                isAadhaarVerified = false
                                verifiedMaskedAadhaar = ""
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Change", fontSize = 11.5.sp, color = StitchLabel)
                        }
                    }
                }
            } else {
                // Unverified Input State
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "12-DIGIT AADHAAR NUMBER *",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchLabel,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = aadhaarNumber,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(12)
                            aadhaarNumber = digits
                        },
                        placeholder = { Text("Enter 12-digit Aadhaar number", fontSize = 13.5.sp) },
                        keyboardOptions = NumberKeyboard,
                        visualTransformation = remember { com.example.dutype.utils.AadhaarVisualTransformation() },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = if (aadhaarValidation.isValid) Color(0xFF00B159) else Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (aadhaarValidation.isValid) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Format Valid",
                                    tint = Color(0xFF00B159),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        supportingText = {
                            if (aadhaarValidation.isValid) {
                                Text(
                                    "✓ Valid Aadhaar number. Tap 'Verify Aadhaar via OTP' below.",
                                    color = Color(0xFF00B159),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp
                                )
                            } else if (cleanAadhaar.isNotEmpty()) {
                                Text(
                                    aadhaarValidation.message,
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.5.sp
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (aadhaarValidation.isValid) Color(0xFF00B159) else Color(0xFF00B159),
                            unfocusedBorderColor = if (aadhaarValidation.isValid) Color(0xFF00B159) else Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    // Mandatory Verify Aadhaar Button
                    Button(
                        onClick = { showAadhaarVerificationSheet = true },
                        enabled = cleanAadhaar.length == 12 && aadhaarValidation.isValid,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00B159),
                            disabledContainerColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Verify Aadhaar via OTP (Required) 🛡️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (cleanAadhaar.length == 12 && aadhaarValidation.isValid) Color.White else Color(0xFF94A3B8)
                        )
                    }

                    // Security guarantee banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECFDF5))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Bank-grade 256-bit encrypted • Required by DutyPe for customer safety",
                            fontSize = 10.5.sp,
                            color = Color(0xFF047857),
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Payout UPI ID
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "PAYOUT UPI ID (FOR DAILY EARNINGS) *",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchLabel,
                    letterSpacing = 0.5.sp
                )
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it.trim().take(50) },
                    placeholder = { Text(stringResource(R.string.partner_upi_hint), fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF00B159), modifier = Modifier.size(20.dp))
                    },
                    supportingText = { Text(stringResource(R.string.partner_upi_sub), fontSize = 11.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00B159),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )
            }

            // Vehicle and tools confirmation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .clickable { hasVehicleAndTools = !hasVehicleAndTools }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                androidx.compose.material3.Checkbox(
                    checked = hasVehicleAndTools,
                    onCheckedChange = { hasVehicleAndTools = it }
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.partner_tools_vehicle),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = StitchNavy,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }

    // Section 4: Mandatory Partner Conduct & Zero-Tolerance Rules
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, StitchBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Gavel, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "PARTNER CODE OF CONDUCT & STRICT RULES *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFB91C1C),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "Zero tolerance for fraud, offline haggling, or misbehavior",
                        fontSize = 11.5.sp,
                        color = StitchLabel
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showConductRulesSheet = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (acceptedConductRules) Color(0xFFECFDF5) else Color(0xFFF8FAFC)
                ),
                border = BorderStroke(
                    1.dp,
                    if (acceptedConductRules) Color(0xFF00B159) else StitchBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (acceptedConductRules) Color(0xFF00B159) else Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (acceptedConductRules) Icons.Filled.CheckCircle else Icons.Filled.Gavel,
                            contentDescription = null,
                            tint = if (acceptedConductRules) Color.White else Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (acceptedConductRules) "✓ Partner Conduct & Strict Terms Accepted" else "Review & Accept Partner Conduct Terms *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (acceptedConductRules) Color(0xFF065F46) else StitchNavy
                        )
                        Text(
                            text = if (acceptedConductRules) "Zero offline demands, family privacy, and police action terms agreed" else "Tap here to review what to do, conduct, and legal action terms",
                            fontSize = 11.5.sp,
                            color = if (acceptedConductRules) Color(0xFF047857) else StitchLabel,
                            lineHeight = 15.sp
                        )
                    }
                    Text(
                        text = if (acceptedConductRules) "Review" else "Read →",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (acceptedConductRules) Color(0xFF00B159) else Color(0xFF2563EB)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { acceptedConductRules = !acceptedConductRules }
                    .padding(vertical = 4.dp)
            ) {
                androidx.compose.material3.Checkbox(
                    checked = acceptedConductRules,
                    onCheckedChange = { acceptedConductRules = it }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "I have read and agree to all DutyPe Partner Conduct Rules, Pricing Standards, and Strict Disciplinary Actions.",
                    fontSize = 12.sp,
                    color = StitchNavy,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            val isSkillsValid = chosen.size in 3..5
            val canSubmit = isSkillsValid &&
                isAadhaarVerified &&
                area.isNotBlank() &&
                upiId.isNotBlank() &&
                acceptedConductRules &&
                !submitting &&
                (!needsSkill || (years.toIntOrNull() ?: 0) >= 1)

            Button(
                onClick = {
                    if (!isSkillsValid) {
                        Toast.makeText(context, "Please select between 3 and 5 services (currently ${chosen.size}).", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!isAadhaarVerified) {
                        Toast.makeText(context, "Please verify your 12-digit Aadhaar number before submitting.", Toast.LENGTH_SHORT).show()
                        showAadhaarVerificationSheet = true
                        return@Button
                    }
                    if (!acceptedConductRules) {
                        Toast.makeText(context, "Please review and accept the Partner Code of Conduct.", Toast.LENGTH_SHORT).show()
                        showConductRulesSheet = true
                        return@Button
                    }

                    submitting = true
                    val formattedNote = buildString {
                        append("AadhaarMasked: $verifiedMaskedAadhaar | ")
                        append("AadhaarLast4: ${cleanAadhaar.takeLast(4)} | ")
                        append("AadhaarVerified: Yes (e-KYC) | ")
                        if (upiId.isNotBlank()) append("UPI: $upiId | ")
                        if (customServicesList.isNotEmpty()) append("CustomServices: ${customServicesList.joinToString(", ")} | ")
                        append("Vehicle & Tools: ${if (hasVehicleAndTools) "Yes" else "No"} | ")
                        append("ConductRulesAccepted: Yes")
                    }
                    scope.launch {
                        viewModel.apply(chosen.toList(), years.toIntOrNull() ?: 0, area.trim(), formattedNote, proof.trim(), true)
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                        submitting = false
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00B159),
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    val submitBtnText = when {
                        !isSkillsValid -> "Select 3 to 5 Services (${chosen.size}/5)"
                        !isAadhaarVerified -> "Verify Aadhaar to Submit"
                        !acceptedConductRules -> "Accept Partner Conduct Rules"
                        !canSubmit -> "Complete Required Fields Above"
                        else -> "Submit Service Partner Application →"
                    }
                    Text(
                        text = submitBtnText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (canSubmit) Color.White else Color(0xFF94A3B8)
                    )
                }
            }

            Text(
                text = "⚡ Review takes 12–24 hrs. Once approved, you can toggle Online to receive customer requests.",
                fontSize = 11.5.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showAadhaarVerificationSheet) {
        com.example.dutype.components.AadhaarKycVerificationSheet(
            aadhaarDigits = cleanAadhaar,
            onDismiss = { showAadhaarVerificationSheet = false },
            onVerified = { masked ->
                isAadhaarVerified = true
                verifiedMaskedAadhaar = masked
                showAadhaarVerificationSheet = false
                Toast.makeText(context, "Aadhaar Identity Confirmed! e-KYC Verified.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showConductRulesSheet) {
        PartnerConductAndRulesBottomSheet(
            onDismiss = { showConductRulesSheet = false },
            onAccept = {
                acceptedConductRules = true
                showConductRulesSheet = false
                Toast.makeText(context, "Partner Conduct Rules Accepted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showVisualGuideSheet) {
        PartnerVisualGuideBottomSheet(
            onDismiss = { showVisualGuideSheet = false }
        )
    }

    if (showRulesDialog) {
        com.example.dutype.guidelines.GuidelinesDialog(com.example.dutype.guidelines.GuidelineRole.PARTNER, onDismiss = {
            showRulesDialog = false
        })
    }
}



// =============================================================================
// PARTNER CONDUCT AND ZERO-TOLERANCE RULES BOTTOM SHEET
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartnerConductAndRulesBottomSheet(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    var hasAgreed by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Gavel, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Partner Code of Conduct & Rules",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = StitchNavy
                    )
                    Text(
                        text = "Mandatory zero-tolerance policies & transparent pricing",
                        fontSize = 12.sp,
                        color = StitchLabel
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchLabel)
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rule 1: Work Standards
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, StitchBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("1. Service & Work Standards", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StitchNavy)
                        }
                        Text(
                            "• Arrive at the customer's location on time within your chosen booking slot.\n" +
                            "• Carry all necessary professional tools, equipment, and clean safety gear.\n" +
                            "• MANDATORY START OTP: Always request and enter customer's 4-digit OTP before starting work.\n" +
                            "• Clean up work debris completely before leaving customer premises.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Rule 2: Professional Conduct & Privacy
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, StitchBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👔", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("2. Polite Behavior & Family Privacy", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StitchNavy)
                        }
                        Text(
                            "• Maintain polite, respectful language with customers, elders, and women.\n" +
                            "• ZERO TOLERANCE: Strictly NO smoking, alcohol, paan, gutkha, or spitting at customer premises.\n" +
                            "• Respect home privacy: Never enter unauthorized rooms or inspect personal belongings.\n" +
                            "• Never take unauthorized photos or videos of the customer's home or family.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Rule 3: Transparent Pricing & No Haggling
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, StitchBorder)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💰", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("3. Transparent Charges & Zero Haggling", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StitchNavy)
                        }
                        Text(
                            "• Stick strictly to DutyPe fixed service rates shown in the app.\n" +
                            "• NO OFFLINE CASH HAGGLING: Never demand extra money, cash tips, or hidden charges.\n" +
                            "• Customer pays digital fee through app or standard rate.\n" +
                            "• Attempting to make offline deals outside the platform leads to immediate ban.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Rule 4: Strict Disciplinary Action & Police FIR
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("4. Zero-Tolerance Violations & Legal Action", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB91C1C))
                        }
                        Text(
                            "If any partner engages in fraud, theft, property damage, extortion, verbal abuse, or harassment:\n" +
                            "1. Immediate permanent termination and blacklisting across all partner platforms.\n" +
                            "2. Forfeiture of all pending wallet balances and incentives.\n" +
                            "3. Formal Police Complaint (FIR) filed by DutyPe Trust & Safety with your verified Aadhaar, photo, and GPS audit logs.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF991B1B),
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Acknowledgment Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { hasAgreed = !hasAgreed }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Checkbox(
                    checked = hasAgreed,
                    onCheckedChange = { hasAgreed = it }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "I have read, understood, and accept all DutyPe Partner Conduct Rules, Pricing Standards, and Strict Disciplinary Actions.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchNavy,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    onAccept()
                    onDismiss()
                },
                enabled = hasAgreed,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00B159),
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    text = "I Agree & Accept Partner Terms →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = if (hasAgreed) Color.White else Color(0xFF94A3B8)
                )
            }
        }
    }
}

// =============================================================================
// PARTNER VISUAL GUIDE & VIDEO WALKTHROUGH BOTTOM SHEET
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartnerVisualGuideBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedLang by remember {
        mutableStateOf(LocaleHelper.getLanguage(context).takeIf { it in listOf("en", "te", "hi") } ?: "te")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFECFDF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📖", fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when (selectedLang) {
                            "te" -> "సర్వీస్ పార్ట్‌నర్ విజువల్ గైడ్"
                            "hi" -> "सर्विस पार्टनर विज़ुअल गाइड"
                            else -> "Service Partner Visual Guide"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = StitchNavy
                    )
                    Text(
                        text = when (selectedLang) {
                            "te" -> "ఆర్డర్‌లను ఎలా స్వీకరించాలి, పనిచేసి డబ్బు ఎలా పొందాలి"
                            "hi" -> "बुकिंग कैसे स्वीकारें, काम करें और भुगतान पाएं"
                            else -> "How to accept bookings, work professionally, and get paid"
                        },
                        fontSize = 12.sp,
                        color = StitchLabel
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchLabel)
                }
            }

            // Language Selector Row (Item 3: Localization)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "te" to "తెలుగు",
                    "en" to "English",
                    "hi" to "हिंदी"
                ).forEach { (code, label) ->
                    val isSelected = selectedLang == code
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable { selectedLang = code },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF00B159) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00B159) else Color(0xFFE2E8F0))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 5 Visual Flow Steps (Localized)
                Text(
                    text = when (selectedLang) {
                        "te" -> "డ్యూటీపే పార్ట్‌నర్ పని విధానం (5 దశలు)"
                        "hi" -> "ड्यूटीपे पार्टनर काम कैसे करता है (5 चरण)"
                        else -> "HOW DUTYPE PARTNER WORK WORKS (5 STEPS)"
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchLabel,
                    letterSpacing = 0.5.sp
                )

                val steps = when (selectedLang) {
                    "te" -> listOf(
                        Triple(
                            "1. సర్వీస్ అలర్ట్ పొందండి",
                            "మీకు 5 కిమీ పరిధిలో కస్టమర్ ఆర్డర్ వచ్చినప్పుడు లౌడ్ రింగ్ వస్తుంది. సంపాదన వివరాలు చూసి 60 సెకన్లలో యాక్సెప్ట్ చేయండి.",
                            "📱"
                        ),
                        Triple(
                            "2. నేరుగా కస్టమర్ ఇంటికి వెళ్లండి",
                            "యాప్‌లో 'Navigate' నొక్కి Google Maps ద్వారా నేరుగా కస్టమర్ డోర్‌స్టెప్‌కు చేరుకోండి. ల్యాండ్‌మార్క్ కోసం యాప్‌లోనే కాల్ చేయవచ్చు.",
                            "🗺️"
                        ),
                        Triple(
                            "3. కస్టమర్ Start OTP నమోదు చేయండి",
                            "ముఖ్యమైనది: పని ప్రారంభించే ముందు కస్టమర్‌ను 4 అంకెల DutyPe Start OTP అడగండి. ఇది ఇన్సూరెన్స్ యాక్టివేట్ చేస్తుంది.",
                            "🔑"
                        ),
                        Triple(
                            "4. నాణ్యమైన పని & శుభ్రత",
                            "అవసరమైన టూల్స్ తెచ్చుకోండి, పని పూర్తి చేసి కస్టమర్‌కు టెస్ట్ చేసి చూపించండి, వర్క్‌స్పేస్‌ను శుభ్రం చేయండి.",
                            "🛠️"
                        ),
                        Triple(
                            "5. తక్షణ UPI చెల్లింపు",
                            "పని ముగిశాక 'Complete' నొక్కండి. మొత్తం మీ నమోదిత UPI ఐడీకి ఎలాంటి కమీషన్ కటింగ్ లేకుండా నేరుగా జమ అవుతుంది!",
                            "⚡"
                        )
                    )
                    "hi" -> listOf(
                        Triple(
                            "1. सर्विस अलर्ट प्राप्त करें",
                            "5 किमी के दायरे में ग्राहक की बुकिंग का तेज अलर्ट बजेगा। कमाई देखकर 60 सेकंड में स्वीकार करें।",
                            "📱"
                        ),
                        Triple(
                            "2. ग्राहक के दरवाजे तक पहुंचें",
                            "'Navigate' दबाएं और Google Maps के जरिए सीधे ग्राहक के घर तक पहुंचें। जरूरत पड़ने पर कॉल करें।",
                            "🗺️"
                        ),
                        Triple(
                            "3. कस्टमर Start OTP दर्ज करें",
                            "अति महत्वपूर्ण: काम शुरू करने से पहले ग्राहक से 4 अंकों का DutyPe Start OTP लें। इससे बीमा सक्रिय होता है।",
                            "🔑"
                        ),
                        Triple(
                            "4. बेहतरीन काम और सफाई",
                            "सभी आवश्यक टूल्स साथ लाएं, काम अच्छे से पूरा करें और काम खत्म होने के बाद जगह को साफ रखें।",
                            "🛠️"
                        ),
                        Triple(
                            "5. तुरंत बैंक UPI भुगतान",
                            "'Complete' दबाते ही पूरी राशि बिना किसी कमीशन कटौती के सीधे आपके रजिस्टर्ड UPI में ट्रांसफर होगी!",
                            "⚡"
                        )
                    )
                    else -> listOf(
                        Triple(
                            "1. Receive Service Alert",
                            "Loud alert rings when a nearby customer needs help within 5 km. You have 60 seconds to review earnings and tap Accept.",
                            "📱"
                        ),
                        Triple(
                            "2. One-Tap Doorstep Navigation",
                            "Tap 'Navigate' for live Google Maps directions straight to customer doorstep. Call customer in-app if you need landmark guidance.",
                            "🗺️"
                        ),
                        Triple(
                            "3. Enter Customer Start OTP",
                            "CRITICAL: Before touching tools, ask customer for the 4-digit DutyPe Start OTP. Entering this officially activates job insurance.",
                            "🔑"
                        ),
                        Triple(
                            "4. Deliver Quality Work & Clean Up",
                            "Carry all required tools, perform the repair/service properly, test with customer, and leave the workspace tidy.",
                            "🛠️"
                        ),
                        Triple(
                            "5. Instant Direct UPI Payout",
                            "Tap 'Complete'. Customer verifies work and full payment is credited directly to your registered UPI ID without delay!",
                            "⚡"
                        )
                    )
                }

                steps.forEach { (title, desc, emoji) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, StitchBorder)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFECFDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = StitchNavy)
                                Spacer(Modifier.height(3.dp))
                                Text(desc, fontSize = 12.sp, color = Color(0xFF475569), lineHeight = 16.sp)
                            }
                        }
                    }
                }

                // Visual Do's & Don'ts Grid (Localized)
                Text(
                    text = when (selectedLang) {
                        "te" -> "పార్ట్‌నర్ నియమాలు (చేయవలసినవి & చేయకూడనివి)"
                        "hi" -> "पार्टनर नियम (क्या करें और क्या न करें)"
                        else -> "PARTNER DO'S & DON'TS"
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchLabel,
                    letterSpacing = 0.5.sp
                )

                val dosList = when (selectedLang) {
                    "te" -> listOf(
                        "5 నిమిషాల ముందుగా వెళ్లండి",
                        "Start OTP తప్పక అడగండి",
                        "స్వంత టూల్స్ తెచ్చుకోండి",
                        "మర్యాదగా మాట్లాడండి",
                        "ఏరియాను క్లీన్ చేయండి"
                    )
                    "hi" -> listOf(
                        "5 मिनट पहले पहुंचें",
                        "Start OTP अवश्य लें",
                        "अपने टूल्स साथ लाएं",
                        "विनम्रता से बात करें",
                        "काम के बाद सफाई करें"
                    )
                    else -> listOf(
                        "Reach 5 mins early",
                        "Ask for Start OTP",
                        "Bring own tools",
                        "Polite & respectful",
                        "Clean work area"
                    )
                }

                val dontsList = when (selectedLang) {
                    "te" -> listOf(
                        "ఆఫ్‌లైన్ బేరసారాలు వద్దు",
                        "ధూమపానం / గుట్కా వద్దు",
                        "ఇతరులను వెంట తీసుకురావద్దు",
                        "ఇంటి ఫోటోలు తీయవద్దు",
                        "OTP లేకుండా పని చేయవద్దు"
                    )
                    "hi" -> listOf(
                        "ऑफलाइन मोलभाव न करें",
                        "धूम्रपान / गुटखा न लें",
                        "अनजान लोगों को न लाएं",
                        "घर की तस्वीरें न लें",
                        "बिना OTP काम शुरू न करें"
                    )
                    else -> listOf(
                        "No offline haggling",
                        "No smoking / gutkha",
                        "Never bring strangers",
                        "No photos of home",
                        "Never skip Start OTP"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Do's Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = when (selectedLang) {
                                    "te" -> "✓ ఎల్లప్పుడూ చేయండి"
                                    "hi" -> "✓ हमेशा करें"
                                    else -> "✓ ALWAYS DO"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF166534)
                            )
                            dosList.forEach { item ->
                                Text("• $item", fontSize = 11.sp, color = Color(0xFF14532D))
                            }
                        }
                    }

                    // Don'ts Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = when (selectedLang) {
                                    "te" -> "✗ ఎప్పుడూ చేయకండి"
                                    "hi" -> "✗ कभी न करें"
                                    else -> "✗ NEVER DO"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B)
                            )
                            dontsList.forEach { item ->
                                Text("• $item", fontSize = 11.sp, color = Color(0xFF7F1D1D))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    text = when (selectedLang) {
                        "te" -> "అర్థమైంది & సేవ చేయడానికి సిద్ధం →"
                        "hi" -> "मैं समझ गया, सेवा के लिए तैयार हूँ →"
                        else -> "I Understand & Ready to Serve →"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = Color.White
                )
            }
        }
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
                placeholder = { Text(stringResource(R.string.svc_topup_amount, c.minTopup)) },
                keyboardOptions = NumberKeyboard,
                shape = RoundedCornerShape(10.dp),
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
                placeholder = { Text(stringResource(R.string.svc_topup_utr)) },
                keyboardOptions = NumberKeyboard,
                shape = RoundedCornerShape(10.dp),
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
                if (error != null) {
                    com.example.dutype.components.DutyPeErrorState(
                        message = error,
                        onRetry = {
                            error = null
                            scope.launch {
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
                        }
                    )
                } else com.example.dutype.components.DutyPeLoadingList(rows = 2)
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

            if (!o.available && !o.mine && o.status != "ASSIGNED") {
                Text(stringResource(R.string.svc_offer_gone), color = SvcRed, textAlign = TextAlign.Center)
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
                                    else -> context.getString(R.string.svc_offer_gone)
                                }
                                if (msg == null) {
                                    context.findActivity()?.let { act ->
                                        reviewTriggerService.onWorkerJobApplication(act)
                                    }
                                    Toast.makeText(context, "Order accepted successfully!", Toast.LENGTH_SHORT).show()
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
                else Text(if (o.mine || o.status == "ASSIGNED") "Accept & Confirm Order" else stringResource(R.string.svc_offer_accept), fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

    LaunchedEffect(booking?.status) {
        val b = booking
        if (b != null && b.status == BookingStatus.ON_THE_WAY) {
            val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                while (isActive && booking?.status == BookingStatus.ON_THE_WAY) {
                    runCatching {
                        fused.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) {
                                scope.launch {
                                    viewModel.updatePartnerLiveLocation(bookingId, loc.latitude, loc.longitude, loc.bearing)
                                }
                            }
                        }
                    }
                    kotlinx.coroutines.delay(8000L)
                }
            }
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

            // ── Live OpenFreeMap Navigation Route to Customer ──
            if (b.status in setOf(BookingStatus.ASSIGNED, BookingStatus.ON_THE_WAY)) {
                com.example.dutype.components.OpenFreeMapTrackerView(
                    customerLat = b.lat,
                    customerLng = b.lng,
                    partnerLat = b.partnerLat,
                    partnerLng = b.partnerLng,
                    customerLabel = b.addressText.ifBlank { "Customer Doorstep" },
                    partnerName = "My Location",
                    partnerPhone = b.customerPhone,
                    partnerBearing = b.partnerBearing,
                    isPartnerMode = true,
                    onNavigateClick = { openMaps(context, b.lat, b.lng) }
                )
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
                        Button(
                            onClick = { act("on_the_way") },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = SvcGreen),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Directions, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Accept & Start Journey", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { v -> code = v.filter { it.isDigit() }.take(4) },
                        placeholder = { Text(stringResource(R.string.svc_job_enter_code)) },
                        keyboardOptions = NumberKeyboard,
                        shape = RoundedCornerShape(10.dp),
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
                        placeholder = { Text(stringResource(R.string.svc_job_extras)) },
                        keyboardOptions = NumberKeyboard,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val extraValue = extras.toIntOrNull() ?: 0
                    if (extraValue > 0) {
                        OutlinedTextField(
                            value = extrasNote,
                            onValueChange = { extrasNote = it.take(200) },
                            placeholder = { Text(stringResource(R.string.svc_job_extras_note)) },
                            shape = RoundedCornerShape(10.dp),
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
