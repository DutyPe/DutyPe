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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
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

/** Card on the worker home that opens the partner dashboard. */
@Composable
fun PartnerEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(SvcGreen), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Handyman, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.svc_partner_entry_title), fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 16.sp)
                Text(stringResource(R.string.svc_partner_entry_subtitle), color = SvcMuted, fontSize = 13.sp)
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
    LaunchedEffect(Unit) { viewModel.loadCatalog() }

    fun changeOnline(online: Boolean) {
        savingOnline = true
        scope.launch {
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
            Centered { CircularProgressIndicator() }
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
                                stringResource(R.string.svc_partner_credits_info, catalog?.commissionPct ?: 10),
                                color = SvcMuted, fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { navController.navigate(Routes.PARTNER_TOPUP) }) {
                                Text(stringResource(R.string.svc_partner_add_credits))
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

                    // History
                    val history = jobs.filter { it.id != active?.id }
                    if (history.isNotEmpty()) {
                        Text(stringResource(R.string.svc_partner_history), fontWeight = FontWeight.Bold)
                        history.forEach { b -> BookingRow(b) { navController.navigate(Routes.partnerJobRoute(b.id)) } }
                    }
                }
            }
        }
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

@Composable
private fun PartnerApplyForm(catalog: ServicesCatalog?, viewModel: HomeServicesViewModel) {
    val context = LocalContext.current
    val lang = LocaleHelper.getLanguage(context)
    val scope = rememberCoroutineScope()
    val chosen = remember { mutableStateListOf<String>() }
    var years by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    Text(stringResource(R.string.svc_partner_apply_title), fontWeight = FontWeight.Bold, fontSize = 20.sp)
    Text(stringResource(R.string.svc_partner_apply_info), color = SvcMuted)
    if (catalog == null) {
        CircularProgressIndicator()
        return
    }
    catalog.categories.forEach { cat ->
        FilterChip(
            selected = cat.id in chosen,
            onClick = { if (cat.id in chosen) chosen.remove(cat.id) else chosen.add(cat.id) },
            label = { Text(cat.label(lang)) },
            leadingIcon = { Icon(categoryIcon(cat.id), contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
    OutlinedTextField(
        value = years,
        onValueChange = { v -> years = v.filter { it.isDigit() }.take(2) },
        label = { Text(stringResource(R.string.svc_partner_experience)) },
        keyboardOptions = NumberKeyboard,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = area,
        onValueChange = { area = it.take(80) },
        label = { Text(stringResource(R.string.svc_partner_area)) },
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = {
            submitting = true
            scope.launch {
                viewModel.apply(chosen.toList(), years.toIntOrNull() ?: 0, area.trim(), "")
                    .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                submitting = false
            }
        },
        enabled = chosen.isNotEmpty() && !submitting,
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) { Text(stringResource(R.string.svc_partner_apply_btn), fontWeight = FontWeight.Bold) }
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
            Centered { CircularProgressIndicator() }
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
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf<ServiceOffer?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var accepting by remember { mutableStateOf(false) }

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

    SvcScaffold(title = stringResource(R.string.svc_offer_title), onBack = { navController.popBackStack() }) { padding ->
        val o = offer
        if (o == null) {
            Centered {
                if (error != null) Text(error.orEmpty(), textAlign = TextAlign.Center) else CircularProgressIndicator()
            }
            return@SvcScaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(o.serviceName, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
            Text(stringResource(R.string.svc_offer_you_earn, o.earning), fontWeight = FontWeight.Bold, fontSize = 30.sp, color = SvcGreen)
            Text(stringResource(R.string.svc_offer_customer_pays, o.price + o.bookingFee, o.bookingFee), color = SvcMuted, textAlign = TextAlign.Center)
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
            TextButton(onClick = { navController.popBackStack() }) { Text(stringResource(R.string.svc_offer_skip)) }
        }
    }
}

// ─────────────────────────── Job ───────────────────────────

@Composable
fun PartnerJobScreen(bookingId: String, navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val context = LocalContext.current
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
            Centered { CircularProgressIndicator() }
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
            if (b.extras > 0) Text(stringResource(R.string.svc_extras, b.extras, b.extrasNote), fontSize = 13.sp)
            PriceLine(stringResource(R.string.svc_price_total), b.total, bold = true)

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
                    Text(stringResource(R.string.svc_job_collect, b.price + b.bookingFee + extraValue), fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            act("complete", extra = extraValue, note = extrasNote.trim()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.svc_job_done, b.price + b.bookingFee + extraValue),
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
