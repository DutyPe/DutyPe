package com.example.dutype.homeservices

import android.Manifest
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal val SvcBlue = Color(0xFF2563EB)
internal val SvcGreen = Color(0xFF16A34A)
internal val SvcOrange = Color(0xFFEA580C)
internal val SvcRed = Color(0xFFDC2626)
internal val SvcMuted = Color(0xFF64748B)
private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

internal fun categoryIcon(category: String): ImageVector = when (category) {
    "AC" -> Icons.Filled.AcUnit
    "CLEANING" -> Icons.Filled.CleaningServices
    "ELECTRICIAN" -> Icons.Filled.ElectricalServices
    "PLUMBER" -> Icons.Filled.Plumbing
    "APPLIANCE" -> Icons.Filled.Kitchen
    else -> Icons.Filled.HomeRepairService
}

internal fun formatSlot(ms: Long): String =
    SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault()).apply { timeZone = IST }.format(Date(ms))

internal fun dial(context: android.content.Context, phone: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
}

@Composable
internal fun statusLabel(status: String): String = stringResource(
    when (status) {
        BookingStatus.SEARCHING -> R.string.svc_status_searching
        BookingStatus.ASSIGNED -> R.string.svc_status_assigned
        BookingStatus.ON_THE_WAY -> R.string.svc_status_on_the_way
        BookingStatus.STARTED -> R.string.svc_status_started
        BookingStatus.COMPLETED -> R.string.svc_status_completed
        BookingStatus.NO_PARTNER -> R.string.svc_status_no_partner
        else -> R.string.svc_status_cancelled
    }
)

internal fun statusColor(status: String): Color = when (status) {
    BookingStatus.COMPLETED -> SvcGreen
    BookingStatus.CANCELLED, BookingStatus.NO_PARTNER -> SvcRed
    BookingStatus.SEARCHING -> SvcOrange
    else -> SvcBlue
}

@Composable
internal fun SvcScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = { actions() }
            )
        },
        content = content
    )
}

@Composable
internal fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

/** Card on the employer home that opens DutyPe Services. */
@Composable
fun HomeServicesEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(SvcBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.HomeRepairService, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.svc_entry_title, "Khammam"),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp
                )
                Text(stringResource(R.string.svc_entry_subtitle), color = SvcMuted, fontSize = 13.sp)
            }
            Text(stringResource(R.string.svc_book), color = SvcBlue, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────── Services home ───────────────────────────

@Composable
fun ServicesHomeScreen(navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lang = LocaleHelper.getLanguage(context)
    val catalog by viewModel.catalog.collectAsState()
    val error by viewModel.catalogError.collectAsState()
    val bookings by viewModel.myBookings.collectAsState()
    var selected by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadCatalog() }

    SvcScaffold(
        title = stringResource(R.string.svc_title),
        onBack = { navController.popBackStack() },
        actions = {
            TextButton(onClick = { navController.navigate(Routes.SERVICES_BOOKINGS) }) {
                Icon(Icons.Filled.History, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.svc_my_bookings))
            }
        }
    ) { padding ->
        val c = catalog
        if (c == null) {
            Centered {
                if (error != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error.orEmpty(), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadCatalog(force = true) }) { Text(stringResource(R.string.svc_retry)) }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
            return@SvcScaffold
        }
        val category = selected ?: c.categories.firstOrNull()?.id
        val active = bookings.firstOrNull { it.isOpen }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (active != null) {
                item(key = "active") {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            navController.navigate(Routes.servicesBookingRoute(active.id))
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED))
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(active.serviceName, fontWeight = FontWeight.Bold)
                            Text(statusLabel(active.status), color = statusColor(active.status))
                        }
                    }
                }
            }
            item(key = "categories") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(c.categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = cat.id == category,
                            onClick = { selected = cat.id },
                            label = { Text(cat.label(lang)) },
                            leadingIcon = { Icon(categoryIcon(cat.id), contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
            item(key = "trust") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = SvcGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.svc_warranty), color = SvcMuted, fontSize = 13.sp)
                }
            }
            items(c.services.filter { it.category == category }, key = { it.id }) { service ->
                ServiceRow(service, lang, c.feeFor(service)) {
                    navController.navigate(Routes.servicesBookRoute(service.id))
                }
            }
        }
    }
}

@Composable
private fun ServiceRow(service: ServiceItem, lang: String, fee: Int, onBook: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(service.label(lang), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.svc_price, service.price), fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("  ·  " + stringResource(R.string.svc_minutes, service.durationMin), color = SvcMuted, fontSize = 13.sp)
                    if (service.inspection) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.svc_inspection_badge),
                            color = SvcOrange,
                            fontSize = 12.sp,
                            modifier = Modifier.background(Color(0xFFFFF7ED), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(service.includes, color = SvcMuted, fontSize = 12.sp, maxLines = 2)
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onBook, shape = RoundedCornerShape(10.dp)) { Text(stringResource(R.string.svc_book)) }
        }
    }
}

// ─────────────────────────── Book ───────────────────────────

/** Hourly slots 8 AM – 8 PM IST on [dayOffset] (0 today, 1 tomorrow), at least 60 minutes ahead. */
private fun slots(dayOffset: Int, now: Long = System.currentTimeMillis()): List<Long> {
    val cal = Calendar.getInstance(IST).apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return (8..20).map { hour ->
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.timeInMillis
    }.filter { it >= now + 60 * 60 * 1000L }
}

@Composable
fun BookServiceScreen(serviceId: String, navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lang = LocaleHelper.getLanguage(context)
    val scope = rememberCoroutineScope()
    val catalog by viewModel.catalog.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadCatalog() }

    var address by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    var locating by remember { mutableStateOf(false) }
    var scheduled by remember { mutableStateOf(false) }
    var dayOffset by remember { mutableIntStateOf(0) }
    var slot by remember { mutableStateOf<Long?>(null) }
    var submitting by remember { mutableStateOf(false) }

    fun locate() {
        locating = true
        scope.launch {
            val place = viewModel.currentPlace()
            locating = false
            if (place == null) {
                Toast.makeText(context, context.getString(R.string.svc_location_needed), Toast.LENGTH_LONG).show()
            } else {
                lat = place.lat
                lng = place.lng
                if (area.isBlank()) area = place.area
                if (address.isBlank()) address = place.address
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) locate()
        else Toast.makeText(context, context.getString(R.string.svc_location_needed), Toast.LENGTH_LONG).show()
    }
    val hasPermission = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val requestLocation = {
        if (hasPermission()) locate()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(Unit) { if (hasPermission()) locate() }

    SvcScaffold(title = stringResource(R.string.svc_book_title), onBack = { navController.popBackStack() }) { padding ->
        val c = catalog
        val service = c?.services?.firstOrNull { it.id == serviceId }
        if (c == null || service == null) {
            Centered { CircularProgressIndicator() }
            return@SvcScaffold
        }
        val fee = c.feeFor(service)
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(categoryIcon(service.category), contentDescription = null, tint = SvcBlue)
                Spacer(Modifier.width(8.dp))
                Text(service.label(lang), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(service.includes, color = SvcMuted, fontSize = 13.sp)
            if (service.inspection) Text(stringResource(R.string.svc_inspection_info), color = SvcOrange, fontSize = 13.sp)

            HorizontalDivider()
            Text(stringResource(R.string.svc_when), fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().clickable { scheduled = false }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !scheduled, onClick = { scheduled = false })
                Text(stringResource(R.string.svc_now))
            }
            Row(Modifier.fillMaxWidth().clickable { scheduled = true }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = scheduled, onClick = { scheduled = true })
                Text(stringResource(R.string.svc_schedule))
            }
            if (scheduled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = dayOffset == 0, onClick = { dayOffset = 0; slot = null }, label = { Text(stringResource(R.string.svc_today)) })
                    FilterChip(selected = dayOffset == 1, onClick = { dayOffset = 1; slot = null }, label = { Text(stringResource(R.string.svc_tomorrow)) })
                }
                val options = slots(dayOffset)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options) { t ->
                        FilterChip(
                            selected = slot == t,
                            onClick = { slot = t },
                            label = { Text(SimpleDateFormat("h a", Locale.getDefault()).apply { timeZone = IST }.format(Date(t))) }
                        )
                    }
                }
            }

            HorizontalDivider()
            Text(stringResource(R.string.svc_address), fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { requestLocation() }, enabled = !locating, modifier = Modifier.fillMaxWidth()) {
                if (locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(if (lat != null) Icons.Filled.CheckCircle else Icons.Filled.LocationOn, contentDescription = null,
                    tint = if (lat != null) SvcGreen else SvcBlue)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (lat != null) R.string.svc_location_set else R.string.svc_use_location))
            }
            OutlinedTextField(
                value = address,
                onValueChange = { address = it.take(300) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.svc_address_hint)) },
                minLines = 2
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(300) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.svc_note_hint)) }
            )

            HorizontalDivider()
            PriceLine(stringResource(R.string.svc_price_service), service.price)
            PriceLine(stringResource(R.string.svc_price_fee), fee)
            PriceLine(stringResource(R.string.svc_price_total), service.price + fee, bold = true)
            Text(stringResource(R.string.svc_pay_after), color = SvcMuted, fontSize = 13.sp)

            val canBook = !submitting && lat != null && address.trim().length >= 5 && (!scheduled || slot != null)
            Button(
                onClick = {
                    submitting = true
                    scope.launch {
                        viewModel.book(service, address.trim(), area.trim(), lat ?: 0.0, lng ?: 0.0, note.trim(), if (scheduled) slot else null)
                            .onSuccess { id ->
                                navController.navigate(Routes.servicesBookingRoute(id)) {
                                    popUpTo(Routes.SERVICES_BOOK) { inclusive = true }
                                }
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                        submitting = false
                    }
                },
                enabled = canBook,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(stringResource(R.string.svc_confirm) + " · ₹" + (service.price + fee), fontWeight = FontWeight.Bold)
            }
            if (lat == null) Text(stringResource(R.string.svc_location_needed), color = SvcRed, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun PriceLine(label: String, amount: Int, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text("₹$amount", fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

// ─────────────────────────── My bookings ───────────────────────────

@Composable
fun MyBookingsScreen(navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val bookings by viewModel.myBookings.collectAsState()
    SvcScaffold(title = stringResource(R.string.svc_my_bookings), onBack = { navController.popBackStack() }) { padding ->
        if (bookings.isEmpty()) {
            Centered {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.svc_no_bookings), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { navController.navigate(Routes.SERVICES) }) { Text(stringResource(R.string.svc_book)) }
                }
            }
            return@SvcScaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(bookings, key = { it.id }) { b -> BookingRow(b) { navController.navigate(Routes.servicesBookingRoute(b.id)) } }
        }
    }
}

@Composable
internal fun BookingRow(b: ServiceBooking, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(categoryIcon(b.category), contentDescription = null, tint = SvcBlue)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(b.serviceName, fontWeight = FontWeight.Bold)
                Text(statusLabel(b.status), color = statusColor(b.status), fontSize = 13.sp)
                Text(formatSlot(if (b.scheduledAt > 0) b.scheduledAt else b.createdAt), color = SvcMuted, fontSize = 12.sp)
            }
            Text("₹${b.total}", fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────── Booking detail ───────────────────────────

@Composable
fun BookingDetailScreen(bookingId: String, navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val booking by remember(bookingId) { viewModel.booking(bookingId) }.collectAsState(initial = null)
    var code by remember { mutableStateOf("") }
    var confirmCancel by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var stars by remember { mutableIntStateOf(0) }
    var review by remember { mutableStateOf("") }

    LaunchedEffect(bookingId) { code = viewModel.startCode(bookingId) }

    SvcScaffold(title = stringResource(R.string.svc_title), onBack = { navController.popBackStack() }) { padding ->
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
            Text(statusLabel(b.status), color = statusColor(b.status), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (b.status == BookingStatus.SEARCHING) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (b.scheduledAt > 0) Text(stringResource(R.string.svc_scheduled_for, formatSlot(b.scheduledAt)), color = SvcMuted)
            Text(b.addressText, color = SvcMuted, fontSize = 13.sp)

            if (b.partnerName.isNotBlank() && b.status != BookingStatus.CANCELLED) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(b.partnerName, fontWeight = FontWeight.Bold)
                            if (b.partnerRating > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                    Text(" %.1f".format(b.partnerRating), fontSize = 13.sp)
                                }
                            }
                        }
                        if (b.partnerPhone.isNotBlank() && b.isOpen) {
                            Button(onClick = { dial(context, b.partnerPhone) }) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.svc_call_partner))
                            }
                        }
                    }
                }
            }

            if (code.isNotBlank() && b.status in setOf(BookingStatus.SEARCHING, BookingStatus.ASSIGNED, BookingStatus.ON_THE_WAY)) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.svc_start_code), color = SvcMuted)
                        Text(code, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp, color = SvcGreen)
                        Text(stringResource(R.string.svc_start_code_help), fontSize = 12.sp, color = SvcMuted, textAlign = TextAlign.Center)
                    }
                }
            }

            HorizontalDivider()
            PriceLine(stringResource(R.string.svc_price_service), b.price)
            PriceLine(stringResource(R.string.svc_price_fee), b.bookingFee)
            if (b.extras > 0) Text(stringResource(R.string.svc_extras, b.extras, b.extrasNote), fontSize = 13.sp)
            PriceLine(stringResource(R.string.svc_price_total), b.total, bold = true)
            Text(
                if (b.status == BookingStatus.COMPLETED) stringResource(R.string.svc_amount_to_pay, b.total)
                else stringResource(R.string.svc_pay_after),
                color = if (b.status == BookingStatus.COMPLETED) SvcGreen else SvcMuted,
                fontWeight = if (b.status == BookingStatus.COMPLETED) FontWeight.Bold else FontWeight.Normal
            )

            if (b.status == BookingStatus.COMPLETED) {
                HorizontalDivider()
                if (b.rating > 0) {
                    Text(stringResource(R.string.svc_you_rated, b.rating))
                } else {
                    Text(stringResource(R.string.svc_rate), fontWeight = FontWeight.Bold)
                    Row {
                        (1..5).forEach { i ->
                            IconButton(onClick = { stars = i }) {
                                Icon(Icons.Filled.Star, contentDescription = "$i", tint = if (i <= stars) Color(0xFFF59E0B) else Color(0xFFCBD5E1))
                            }
                        }
                    }
                    OutlinedTextField(
                        value = review,
                        onValueChange = { review = it.take(500) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.svc_review_hint)) }
                    )
                    Button(
                        onClick = {
                            busy = true
                            scope.launch {
                                viewModel.rate(b.id, stars, review.trim())
                                    .onSuccess { Toast.makeText(context, context.getString(R.string.svc_thanks_rating), Toast.LENGTH_SHORT).show() }
                                    .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                                busy = false
                            }
                        },
                        enabled = stars > 0 && !busy
                    ) { Text(stringResource(R.string.svc_submit)) }
                }
            }

            if (b.status == BookingStatus.NO_PARTNER || b.status == BookingStatus.CANCELLED) {
                Button(onClick = { navController.navigate(Routes.SERVICES) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.svc_book_again))
                }
            }

            if (b.status in setOf(BookingStatus.SEARCHING, BookingStatus.ASSIGNED, BookingStatus.ON_THE_WAY)) {
                OutlinedButton(onClick = { confirmCancel = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.svc_cancel_booking), color = SvcRed)
                }
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.svc_cancel_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    busy = true
                    scope.launch {
                        viewModel.cancel(bookingId).onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                        busy = false
                    }
                }) { Text(stringResource(R.string.svc_yes)) }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.svc_no)) } }
        )
    }
}

internal val NumberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
