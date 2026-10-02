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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.DutyPeEmptyState
import com.example.dutype.components.DutyPeErrorState
import com.example.dutype.components.DutyPeIssueState
import com.example.dutype.components.DutyPeLoadingList
import com.example.dutype.components.EmptyArt
import com.example.dutype.components.EmptyStateAction
import com.example.dutype.components.EmptyTone
import com.example.dutype.components.LoadIssue
import com.example.dutype.components.OfflineCachedNote
import com.example.dutype.components.ShimmerBox
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
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
internal val SvcInk = Color(0xFF0F172A)
internal val SvcLine = Color(0xFFE2E8F0)
internal val SvcPage = Color(0xFFF8FAFC)
private val IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

internal fun categoryIcon(category: String): ImageVector = when (category) {
    "AC" -> Icons.Filled.AcUnit
    "CLEANING" -> Icons.Filled.CleaningServices
    "ELECTRICIAN" -> Icons.Filled.ElectricalServices
    "PLUMBER" -> Icons.Filled.Plumbing
    "APPLIANCE" -> Icons.Filled.Kitchen
    else -> Icons.Filled.HomeRepairService
}

/** Each category's own colour, so the grid reads at a glance. */
internal fun categoryTint(category: String): Color = when (category) {
    "AC" -> Color(0xFF0EA5E9)
    "CLEANING" -> Color(0xFF14B8A6)
    "ELECTRICIAN" -> Color(0xFFF59E0B)
    "PLUMBER" -> Color(0xFF6366F1)
    "APPLIANCE" -> Color(0xFFEC4899)
    else -> SvcBlue
}

internal fun formatSlot(ms: Long): String =
    SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault()).apply { timeZone = IST }.format(Date(ms))

private fun formatTime(ms: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).apply { timeZone = IST }.format(Date(ms))

private fun formatDay(ms: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).apply { timeZone = IST }.format(Date(ms))

internal fun dial(context: Context, phone: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
}

private fun copyCode(context: Context, code: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("coupon", code))
    Toast.makeText(context, context.getString(R.string.svc_copy_code, code), Toast.LENGTH_SHORT).show()
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
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = SvcPage.bg(),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = actions
            )
        },
        content = content
    )
}

@Composable
internal fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

/** White rounded section used across the services screens. */
@Composable
internal fun SvcSection(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.bg())
            .border(1.dp, SvcLine.bg(), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}

@Composable
private fun Pill(text: String, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Compact card on the employer home that opens DutyPe Services (fallback for the showcase). */
@Composable
fun HomeServicesEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF).bg())
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
                    color = SvcInk.fg(),
                    fontSize = 16.sp
                )
                Text(stringResource(R.string.svc_entry_subtitle), color = SvcMuted.fg(), fontSize = 13.sp)
            }
            Text(stringResource(R.string.svc_book), color = SvcBlue, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────── Shared pieces ───────────────────────────

/** Blue hero: what DutyPe Services is, in one glance, with the first-booking offer. */
@Composable
internal fun ServicesHero(city: String, firstBookingFree: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF0EA5E9))))
            .padding(18.dp)
    ) {
        Pill(stringResource(R.string.svc_now_in, city), Color.White, icon = Icons.Filled.LocationOn)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.svc_hero_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 27.sp)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.svc_hero_sub), color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            HeroPoint(Icons.Filled.Verified, stringResource(R.string.svc_point_verified))
            HeroPoint(Icons.Filled.Payments, stringResource(R.string.svc_point_pay_after))
            HeroPoint(Icons.Filled.EventAvailable, stringResource(R.string.svc_point_rework))
        }
        if (firstBookingFree) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.svc_first_free), color = Color(0xFF92400E), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun HeroPoint(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Five category tiles in one row. */
@Composable
internal fun CategoryTiles(
    categories: List<ServiceCategory>,
    lang: String,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { cat ->
            val tint = categoryTint(cat.id)
            val on = cat.id == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (on) tint.copy(alpha = 0.12f) else Color.White.bg())
                    .border(if (on) 1.5.dp else 1.dp, if (on) tint else SvcLine.bg(), RoundedCornerShape(14.dp))
                    .clickable { onSelect(cat.id) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(categoryIcon(cat.id), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    cat.label(lang),
                    fontSize = 11.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = SvcInk.fg(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Coupon tickets: headline, title, code (tap copies, or [onApply] applies it). */
@Composable
internal fun OfferTickets(offers: List<PromoOffer>, onApply: ((PromoOffer) -> Unit)? = null, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(offers, key = { it.code }) { o ->
            Row(
                Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.bg())
                    .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(14.dp))
                    .clickable { if (onApply != null) onApply(o) else copyCode(context, o.code) }
            ) {
                Box(
                    Modifier.background(SvcGreen).padding(horizontal = 10.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (o.headline.isNotBlank()) Text(o.headline, color = SvcGreen, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(o.title, color = SvcInk.fg(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(o.code, color = SvcBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.width(4.dp))
                        if (onApply != null) {
                            Text("· " + stringResource(R.string.svc_apply), color = SvcBlue, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = SvcBlue, modifier = Modifier.size(12.dp))
                        }
                    }
                    if (o.minOrder > 0) Text(stringResource(R.string.svc_min_order, o.minOrder), color = SvcMuted.fg(), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SvcInk.fg(), modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Live booking strip ("Partner is on the way") shown above everything else. */
@Composable
internal fun ActiveBookingStrip(b: ServiceBooking, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = statusColor(b.status)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(categoryIcon(b.category), contentDescription = null, tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.svc_active_booking), color = SvcMuted.fg(), fontSize = 12.sp)
            Text(b.serviceName, fontWeight = FontWeight.Bold, color = SvcInk.fg(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(statusLabel(b.status), color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = color)
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
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (error != null) DutyPeErrorState(error, onRetry = { viewModel.loadCatalog(force = true) })
                else DutyPeLoadingList(rows = 5)
            }
            return@SvcScaffold
        }
        val category = selected ?: c.categories.firstOrNull()?.id
        val active = bookings.firstOrNull { it.isOpen }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "offline") { OfflineCachedNote() }
            if (active != null) {
                item(key = "active") {
                    ActiveBookingStrip(active, onClick = { navController.navigate(Routes.servicesBookingRoute(active.id)) })
                }
            }
            item(key = "hero") { ServicesHero(c.city, c.firstBookingFeeFree) }
            if (c.offers.isNotEmpty()) {
                item(key = "offers_title") { SectionTitle(stringResource(R.string.svc_offers)) }
                item(key = "offers") { OfferTickets(c.offers) }
            }
            item(key = "cat_title") { SectionTitle(stringResource(R.string.svc_choose_category)) }
            item(key = "categories") { CategoryTiles(c.categories, lang, category, onSelect = { selected = it }) }
            items(c.services.filter { it.category == category }, key = { it.id }) { service ->
                ServiceRow(service, lang) { navController.navigate(Routes.servicesBookRoute(service.id)) }
            }
            item(key = "how") { HowItWorks() }
        }
    }
}

@Composable
private fun ServiceRow(service: ServiceItem, lang: String, onBook: () -> Unit) {
    val tint = categoryTint(service.category)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.bg())
            .border(1.dp, SvcLine.bg(), RoundedCornerShape(16.dp))
            .clickable(onClick = onBook)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(categoryIcon(service.category), contentDescription = null, tint = tint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(service.label(lang), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SvcInk.fg())
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.svc_price, service.price), fontWeight = FontWeight.ExtraBold, color = SvcInk.fg())
                Text("  ·  " + stringResource(R.string.svc_minutes, service.durationMin), color = SvcMuted.fg(), fontSize = 12.sp)
            }
            if (service.inspection) {
                Spacer(Modifier.height(4.dp))
                Pill(stringResource(R.string.svc_inspection_badge), SvcOrange)
            }
            Spacer(Modifier.height(4.dp))
            Text(service.includes, color = SvcMuted.fg(), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onBook, shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 14.dp)) {
            Text(stringResource(R.string.svc_book), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HowItWorks() {
    SvcSection {
        Text(stringResource(R.string.svc_how_title), fontWeight = FontWeight.Bold, color = SvcInk.fg())
        listOf(R.string.svc_how_1, R.string.svc_how_2, R.string.svc_how_3).forEachIndexed { i, res ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(SvcBlue.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = SvcBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(stringResource(res), color = SvcInk.fg(), fontSize = 13.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Verified, contentDescription = null, tint = SvcGreen, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.svc_warranty), color = SvcMuted.fg(), fontSize = 12.sp)
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
    val catalogError by viewModel.catalogError.collectAsState()
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

    // Offers: the server prices the booking (first-booking offer, coupon), the screen only shows it.
    var couponInput by remember { mutableStateOf("") }
    var appliedCode by remember { mutableStateOf("") }
    var quote by remember { mutableStateOf<ServiceQuote?>(null) }
    var quoting by remember { mutableStateOf(false) }
    LaunchedEffect(serviceId, appliedCode) {
        quoting = true
        viewModel.quote(serviceId, appliedCode)
            .onSuccess { q ->
                quote = q
                // An invalid code is shown and then dropped, so it is never sent with the booking.
                if (appliedCode.isNotBlank() && q.couponError.isNotBlank()) {
                    Toast.makeText(context, q.couponError, Toast.LENGTH_LONG).show()
                    appliedCode = ""
                }
            }
        quoting = false
    }

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
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    catalogError != null -> DutyPeErrorState(catalogError, onRetry = { viewModel.loadCatalog(force = true) })
                    c != null -> DutyPeIssueState(LoadIssue.NOT_FOUND, onRetry = null)
                    else -> DutyPeLoadingList(rows = 3)
                }
            }
            return@SvcScaffold
        }
        val fee = c.feeFor(service)
        val q = quote
        val discount = q?.discount ?: 0
        val total = q?.total ?: (service.price + fee)
        val tint = categoryTint(service.category)
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OfflineCachedNote()
            SvcSection {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(categoryIcon(service.category), contentDescription = null, tint = tint)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(service.label(lang), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SvcInk.fg())
                        Text(
                            stringResource(R.string.svc_price, service.price) + "  ·  " + stringResource(R.string.svc_minutes, service.durationMin),
                            color = SvcMuted.fg(), fontSize = 13.sp
                        )
                    }
                }
                Text(service.includes, color = SvcMuted.fg(), fontSize = 13.sp)
                if (service.inspection) Text(stringResource(R.string.svc_inspection_info), color = SvcOrange, fontSize = 13.sp)
            }

            SvcSection {
                Text(stringResource(R.string.svc_when), fontWeight = FontWeight.Bold, color = SvcInk.fg())
                Row(Modifier.fillMaxWidth().clickable { scheduled = false }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !scheduled, onClick = { scheduled = false })
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = SvcOrange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.svc_now))
                }
                Row(Modifier.fillMaxWidth().clickable { scheduled = true }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = scheduled, onClick = { scheduled = true })
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = SvcBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
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
            }

            SvcSection {
                Text(stringResource(R.string.svc_address), fontWeight = FontWeight.Bold, color = SvcInk.fg())
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
            }

            // Offers & coupon
            SvcSection {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = SvcGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.svc_coupon), fontWeight = FontWeight.Bold, color = SvcInk.fg())
                }
                if (q != null && q.couponCode.isNotBlank()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0FDF4).bg()).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SvcGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.svc_coupon_applied, q.couponCode, q.discount),
                            color = SvcGreen, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { appliedCode = ""; couponInput = "" }) { Text(stringResource(R.string.svc_remove)) }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = couponInput,
                            onValueChange = { couponInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(20) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(stringResource(R.string.svc_coupon_hint)) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { appliedCode = couponInput.trim() },
                            enabled = couponInput.trim().length >= 3 && !quoting,
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(stringResource(R.string.svc_apply)) }
                    }
                }
                if (q != null && q.couponNote.isNotBlank()) Text(q.couponNote, color = SvcOrange, fontSize = 12.sp)
                val offers = q?.offers.orEmpty().filter { it.code != q?.couponCode }
                if (offers.isNotEmpty()) {
                    OfferTickets(offers, onApply = { couponInput = it.code; appliedCode = it.code })
                }
            }

            // Bill
            SvcSection {
                Text(stringResource(R.string.svc_bill), fontWeight = FontWeight.Bold, color = SvcInk.fg())
                PriceLine(stringResource(R.string.svc_price_service), service.price)
                PriceLine(stringResource(R.string.svc_price_fee), fee, struck = discount > 0 && discount >= fee && q?.couponCode.isNullOrBlank())
                if (discount > 0) DiscountLine(q?.discountLabel.orEmpty(), discount)
                HorizontalDivider(color = SvcLine.bg())
                PriceLine(stringResource(R.string.svc_price_total), total, bold = true)
                if (discount > 0) Pill(stringResource(R.string.svc_you_save, discount), SvcGreen, icon = Icons.Filled.LocalOffer)
                Text(stringResource(R.string.svc_pay_after), color = SvcMuted.fg(), fontSize = 12.sp)
            }

            val canBook = !submitting && lat != null && address.trim().length >= 5 && (!scheduled || slot != null)
            Button(
                onClick = {
                    submitting = true
                    scope.launch {
                        viewModel.book(
                            service, address.trim(), area.trim(), lat ?: 0.0, lng ?: 0.0, note.trim(),
                            if (scheduled) slot else null, q?.couponCode.orEmpty()
                        )
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
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(stringResource(R.string.svc_confirm) + " · ₹" + total, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            if (lat == null) Text(stringResource(R.string.svc_location_needed), color = SvcRed, fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun PriceLine(label: String, amount: Int, bold: Boolean = false, struck: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = SvcInk.fg())
        Text(
            "₹$amount",
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal,
            fontSize = if (bold) 17.sp else 14.sp,
            color = if (struck) SvcMuted.fg() else SvcInk.fg(),
            textDecoration = if (struck) TextDecoration.LineThrough else null
        )
    }
}

@Composable
internal fun DiscountLine(label: String, amount: Int) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label.ifBlank { stringResource(R.string.svc_price_discount) },
            modifier = Modifier.weight(1f), color = SvcGreen, fontWeight = FontWeight.SemiBold
        )
        Text("− ₹$amount", color = SvcGreen, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────── My bookings ───────────────────────────

@Composable
fun MyBookingsScreen(navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val load by viewModel.bookingsLoad.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    SvcScaffold(title = stringResource(R.string.svc_my_bookings), onBack = { navController.popBackStack() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val active = load.list.filter { it.isOpen }
            val past = load.list.filterNot { it.isOpen }
            LaunchedEffect(load.loaded) { if (load.loaded && active.isEmpty() && past.isNotEmpty()) tab = 1 }
            TabRow(selectedTabIndex = tab, containerColor = Color.White.bg()) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.svc_tab_active) + if (active.isNotEmpty()) " (${active.size})" else "") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.svc_tab_past) + if (past.isNotEmpty()) " (${past.size})" else "") })
            }
            OfflineCachedNote()
            val list = if (tab == 0) active else past
            when {
                !load.loaded -> DutyPeLoadingList(rows = 4, row = { BookingRowShimmer() })
                load.error != null && load.list.isEmpty() -> DutyPeErrorState(load.error, onRetry = null)
                list.isEmpty() && tab == 0 -> DutyPeEmptyState(
                    icon = Icons.Filled.HomeRepairService,
                    tone = EmptyTone.BLUE,
                    art = EmptyArt.WAITING,
                    title = stringResource(R.string.svc_no_active_title),
                    message = stringResource(R.string.svc_no_active_body),
                    primary = EmptyStateAction(stringResource(R.string.svc_book_service), Icons.Filled.HomeRepairService) {
                        navController.navigate(Routes.SERVICES)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                list.isEmpty() -> DutyPeEmptyState(
                    icon = Icons.Filled.History,
                    tone = EmptyTone.PURPLE,
                    art = EmptyArt.QUIET,
                    title = stringResource(R.string.svc_no_past_title),
                    message = stringResource(R.string.svc_no_past_body),
                    modifier = Modifier.fillMaxSize()
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(list, key = { it.id }) { b -> BookingRow(b) { navController.navigate(Routes.servicesBookingRoute(b.id)) } }
                }
            }
        }
    }
}

@Composable
private fun BookingRowShimmer() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.bg()).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(width = 44.dp, height = 44.dp, shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBox(width = 160.dp, height = 14.dp)
            ShimmerBox(width = 100.dp, height = 12.dp)
        }
        ShimmerBox(width = 48.dp, height = 18.dp)
    }
}

@Composable
internal fun BookingRow(b: ServiceBooking, onClick: () -> Unit) {
    val tint = categoryTint(b.category)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.bg())
            .border(1.dp, SvcLine.bg(), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(categoryIcon(b.category), contentDescription = null, tint = tint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(b.serviceName, fontWeight = FontWeight.Bold, color = SvcInk.fg(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatSlot(if (b.scheduledAt > 0) b.scheduledAt else b.createdAt), color = SvcMuted.fg(), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(statusLabel(b.status), statusColor(b.status))
                if (b.discount > 0) Pill(stringResource(R.string.svc_saved, b.discount), SvcGreen)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("₹${b.total}", fontWeight = FontWeight.ExtraBold, color = SvcInk.fg())
            if (b.status == BookingStatus.COMPLETED && b.rating > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    Text("${b.rating}", fontSize = 12.sp, color = SvcMuted.fg())
                }
            }
        }
    }
}

// ─────────────────────────── Booking detail ───────────────────────────

/** Booked → assigned → on the way → started → completed, with times where known. */
@Composable
private fun StatusTimeline(b: ServiceBooking) {
    val failed = b.status == BookingStatus.CANCELLED || b.status == BookingStatus.NO_PARTNER
    val order = listOf(BookingStatus.SEARCHING, BookingStatus.ASSIGNED, BookingStatus.ON_THE_WAY, BookingStatus.STARTED, BookingStatus.COMPLETED)
    val reached = order.indexOf(b.status).let { if (it < 0) 0 else it }
    val steps = listOf(
        stringResource(R.string.svc_step_booked) to b.createdAt,
        stringResource(R.string.svc_step_assigned) to b.assignedAt,
        stringResource(R.string.svc_step_on_way) to 0L,
        stringResource(R.string.svc_step_started) to b.startedAt,
        stringResource(R.string.svc_step_completed) to b.completedAt
    )
    SvcSection {
        steps.forEachIndexed { i, (label, at) ->
            val done = !failed && i <= reached
            val current = !failed && i == reached && b.status != BookingStatus.COMPLETED
            val color = when {
                done && current -> statusColor(b.status)
                done -> SvcGreen
                else -> SvcLine
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(20.dp).clip(CircleShape).background(if (done) color else Color.White.bg())
                            .border(2.dp, color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done && !current) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    if (i < steps.lastIndex) Box(Modifier.width(2.dp).height(22.dp).background(if (!failed && i < reached) SvcGreen else SvcLine))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        label,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                        color = if (done) SvcInk.fg() else SvcMuted.fg(),
                        fontSize = 14.sp
                    )
                    if (done && at > 0) Text(formatDay(at) + ", " + formatTime(at), color = SvcMuted.fg(), fontSize = 11.sp)
                }
            }
        }
        if (failed) Pill(statusLabel(b.status), SvcRed)
    }
}

@Composable
fun BookingDetailScreen(bookingId: String, navController: NavController, viewModel: HomeServicesViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val load by remember(bookingId) { viewModel.bookingLoad(bookingId) }.collectAsState(initial = BookingLoad(false, null, null))
    var code by remember { mutableStateOf("") }
    var confirmCancel by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var stars by remember { mutableIntStateOf(0) }
    var review by remember { mutableStateOf("") }

    LaunchedEffect(bookingId) { code = viewModel.startCode(bookingId) }

    SvcScaffold(title = stringResource(R.string.svc_title), onBack = { navController.popBackStack() }) { padding ->
        val b = load.booking
        if (b == null) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    !load.loaded -> DutyPeLoadingList(rows = 3)
                    load.error != null -> DutyPeErrorState(load.error, onRetry = null)
                    else -> DutyPeIssueState(LoadIssue.NOT_FOUND, onRetry = null)
                }
            }
            return@SvcScaffold
        }
        val tint = categoryTint(b.category)
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OfflineCachedNote()
            SvcSection {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(categoryIcon(b.category), contentDescription = null, tint = tint)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(b.serviceName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SvcInk.fg())
                        Text(stringResource(R.string.svc_booking_id, b.id.take(8).uppercase()), color = SvcMuted.fg(), fontSize = 12.sp)
                    }
                }
                Text(statusLabel(b.status), color = statusColor(b.status), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (b.status == BookingStatus.SEARCHING) LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                if (b.scheduledAt > 0) Text(stringResource(R.string.svc_scheduled_for, formatSlot(b.scheduledAt)), color = SvcMuted.fg(), fontSize = 13.sp)
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = SvcMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(b.addressText, color = SvcMuted.fg(), fontSize = 13.sp)
                }
            }

            if (code.isNotBlank() && b.status in setOf(BookingStatus.SEARCHING, BookingStatus.ASSIGNED, BookingStatus.ON_THE_WAY)) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0FDF4).bg())
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(16.dp)).padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.svc_start_code), color = SvcMuted.fg())
                    Text(code, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp, color = SvcGreen)
                    Text(stringResource(R.string.svc_start_code_help), fontSize = 12.sp, color = SvcMuted.fg(), textAlign = TextAlign.Center)
                }
            }

            if (b.partnerName.isNotBlank() && b.status != BookingStatus.CANCELLED) {
                SvcSection {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(SvcBlue), contentAlignment = Alignment.Center) {
                            Text(b.partnerName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.partnerName, fontWeight = FontWeight.Bold, color = SvcInk.fg())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = SvcGreen, modifier = Modifier.size(14.dp))
                                Text(" " + stringResource(R.string.svc_point_verified), fontSize = 12.sp, color = SvcMuted.fg())
                                if (b.partnerRating > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                    Text(" %.1f".format(b.partnerRating), fontSize = 12.sp, color = SvcMuted.fg())
                                }
                            }
                        }
                        if (b.partnerPhone.isNotBlank() && b.isOpen) {
                            Button(onClick = { dial(context, b.partnerPhone) }, shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.svc_call_partner))
                            }
                        }
                    }
                }
            }

            StatusTimeline(b)

            SvcSection {
                Text(stringResource(R.string.svc_bill), fontWeight = FontWeight.Bold, color = SvcInk.fg())
                PriceLine(stringResource(R.string.svc_price_service), b.price)
                PriceLine(stringResource(R.string.svc_price_fee), b.bookingFee)
                if (b.discount > 0) DiscountLine(b.discountLabel, b.discount)
                if (b.extras > 0) PriceLine(stringResource(R.string.svc_extras, b.extras, b.extrasNote), b.extras)
                HorizontalDivider(color = SvcLine.bg())
                PriceLine(stringResource(R.string.svc_price_total), b.total, bold = true)
                if (b.discount > 0) Pill(stringResource(R.string.svc_you_save, b.discount), SvcGreen, icon = Icons.Filled.LocalOffer)
                Text(
                    if (b.status == BookingStatus.COMPLETED) stringResource(R.string.svc_amount_to_pay, b.total)
                    else stringResource(R.string.svc_pay_after),
                    color = if (b.status == BookingStatus.COMPLETED) SvcGreen else SvcMuted.fg(),
                    fontWeight = if (b.status == BookingStatus.COMPLETED) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }

            if (b.status == BookingStatus.COMPLETED) {
                SvcSection {
                    if (b.rating > 0) {
                        Text(stringResource(R.string.svc_you_rated, b.rating), color = SvcInk.fg())
                    } else {
                        Text(stringResource(R.string.svc_rate), fontWeight = FontWeight.Bold, color = SvcInk.fg())
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
            }

            if (b.status == BookingStatus.NO_PARTNER || b.status == BookingStatus.CANCELLED || b.status == BookingStatus.COMPLETED) {
                Button(
                    onClick = { navController.navigate(Routes.SERVICES) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.svc_book_again), fontWeight = FontWeight.Bold) }
            }

            if (b.status in setOf(BookingStatus.SEARCHING, BookingStatus.ASSIGNED, BookingStatus.ON_THE_WAY)) {
                OutlinedButton(onClick = { confirmCancel = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.svc_cancel_booking), color = SvcRed)
                }
            }
            Spacer(Modifier.height(24.dp))
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

// ─────────────────────────── Employer home ───────────────────────────

/**
 * "What do you need today?" — the three ways DutyPe helps an employer, side by side:
 * hire staff (job post), someone right now (urgent), or fix something at home (services).
 */
@Composable
fun EmployerNeedChooser(
    onPostJob: () -> Unit,
    onPostUrgent: () -> Unit,
    onHomeServices: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.svc_need_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SvcInk.fg())
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeedTile(Icons.Filled.Work, stringResource(R.string.svc_need_hire), stringResource(R.string.svc_need_hire_sub), SvcBlue, onPostJob)
            NeedTile(Icons.Filled.Bolt, stringResource(R.string.svc_need_urgent), stringResource(R.string.svc_need_urgent_sub), SvcOrange, onPostUrgent)
            NeedTile(Icons.Filled.HomeRepairService, stringResource(R.string.svc_need_home), stringResource(R.string.svc_need_home_sub), Color(0xFF0D9488), onHomeServices)
        }
    }
}

@Composable
private fun RowScope.NeedTile(icon: ImageVector, title: String, sub: String, color: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.bg())
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SvcInk.fg(), lineHeight = 16.sp, minLines = 2)
        Spacer(Modifier.height(2.dp))
        Text(sub, fontSize = 11.sp, color = SvcMuted.fg(), lineHeight = 14.sp, maxLines = 2)
    }
}

/**
 * Home services on the employer home: offer banner, the 5 categories and popular services with
 * prices, each one tap from booking. Falls back to the compact card when the catalog can't load.
 */
@Composable
fun HomeServicesShowcase(
    onOpenServices: () -> Unit,
    onBookService: (String) -> Unit,
    onOpenBooking: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeServicesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lang = LocaleHelper.getLanguage(context)
    val catalog by viewModel.catalog.collectAsState()
    val error by viewModel.catalogError.collectAsState()
    val bookings by viewModel.myBookings.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadCatalog() }

    val c = catalog
    if (c == null) {
        if (error != null) HomeServicesEntryCard(onClick = onOpenServices, modifier = modifier)
        else ShimmerBox(modifier = modifier.fillMaxWidth(), height = 180.dp, shape = RoundedCornerShape(20.dp))
        return
    }
    val active = bookings.firstOrNull { it.isOpen }
    // One popular pick per category (the first fixed-price one), so the row covers every need.
    val popular = remember(c) {
        c.categories.mapNotNull { cat -> c.services.firstOrNull { it.category == cat.id && !it.inspection } ?: c.services.firstOrNull { it.category == cat.id } }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.svc_showcase_title, c.city)) {
            TextButton(onClick = onOpenServices) {
                Text(stringResource(R.string.svc_see_all), fontWeight = FontWeight.Bold)
            }
        }
        if (active != null) ActiveBookingStrip(active, onClick = { onOpenBooking(active.id) })
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))))
                .clickable(onClick = onOpenServices)
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.svc_hero_title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.svc_hero_sub), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            val headline = when {
                c.offers.isNotEmpty() -> c.offers.first().let { o -> (o.headline.ifBlank { o.title }) + " · " + o.code }
                c.firstBookingFeeFree -> stringResource(R.string.svc_first_free)
                else -> ""
            }
            if (headline.isNotBlank()) {
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFFEF3C7)).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(headline, color = Color(0xFF92400E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        CategoryTiles(c.categories, lang, selected = null, onSelect = { onOpenServices() })
        Text(stringResource(R.string.svc_popular), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SvcInk.fg())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(popular, key = { it.id }) { s ->
                val tint = categoryTint(s.category)
                Column(
                    Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.bg())
                        .border(1.dp, SvcLine.bg(), RoundedCornerShape(16.dp))
                        .clickable { onBookService(s.id) }
                        .padding(12.dp)
                ) {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(categoryIcon(s.category), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(s.label(lang), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SvcInk.fg(), maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.svc_price, s.price), fontWeight = FontWeight.ExtraBold, color = SvcInk.fg())
                    Text(stringResource(R.string.svc_minutes, s.durationMin), fontSize = 11.sp, color = SvcMuted.fg())
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SvcBlue).padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.svc_book), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

internal val NumberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
