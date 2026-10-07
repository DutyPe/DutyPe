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
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
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
import com.example.dutype.viewmodels.EmployerJobsViewModel
import com.example.dutype.viewmodels.InstantHelpViewModel
import androidx.navigation.NavController
import com.example.dutype.employer.screens.EmployerExecutiveHeroCard
import com.example.dutype.employer.screens.EmployerExecutiveInstantHelpCard
import com.example.dutype.employer.screens.VoicePulsingFab
import com.example.dutype.utils.findActivity
import com.example.dutype.di.rememberInAppReviewTriggerService
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
import com.example.dutype.models.LocationData
import com.example.dutype.ui.theme.bd
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
    "CARPENTER" -> Icons.Filled.Carpenter
    "PAINTER" -> Icons.Filled.FormatPaint
    "HOME_HELP" -> Icons.Filled.Handyman
    "VEHICLE" -> Icons.Filled.LocalCarWash
    else -> Icons.Filled.HomeRepairService
}

/** Each category's own colour, so the grid reads at a glance. */
internal fun categoryTint(category: String): Color = when (category) {
    "AC" -> Color(0xFF0EA5E9)
    "CLEANING" -> Color(0xFF14B8A6)
    "ELECTRICIAN" -> Color(0xFFF59E0B)
    "PLUMBER" -> Color(0xFF6366F1)
    "APPLIANCE" -> Color(0xFFEC4899)
    "CARPENTER" -> Color(0xFFB45309)
    "PAINTER" -> Color(0xFF8B5CF6)
    "HOME_HELP" -> Color(0xFF16A34A)
    "VEHICLE" -> Color(0xFF0284C7)
    else -> SvcBlue
}

/**
 * Renders 3D isometric clay render for a category via Coil (Memory + Disk cache).
 * Falls back to crisp vector [categoryIcon] while loading, offline, or when image is not yet cached.
 */
@Composable
internal fun CategoryClayImage(
    category: ServiceCategory,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier.size(24.dp)
) {
    val fallbackTint = if (isSelected) Color.White else Color(0xFF0F172A)
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(category.resolvedImageUrl)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = category.name,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    categoryIcon(category.id),
                    contentDescription = null,
                    tint = fallbackTint,
                    modifier = iconModifier
                )
            }
        },
        error = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    categoryIcon(category.id),
                    contentDescription = null,
                    tint = fallbackTint,
                    modifier = iconModifier
                )
            }
        }
    )
}

/**
 * Renders 3D isometric clay render for a specific service via Coil (Memory + Disk cache).
 * Falls back to crisp category vector [categoryIcon] while loading, offline, or when image is not yet cached.
 */
@Composable
internal fun ServiceClayImage(
    service: ServiceItem,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier.size(28.dp),
    tint: Color = Color(0xFF0F172A),
    contentScale: ContentScale = ContentScale.Crop
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(service.resolvedImageUrl)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = service.name,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    categoryIcon(service.category),
                    contentDescription = null,
                    tint = tint,
                    modifier = iconModifier
                )
            }
        },
        error = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    categoryIcon(service.category),
                    contentDescription = null,
                    tint = tint,
                    modifier = iconModifier
                )
            }
        }
    )
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
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = SvcPage.bg(),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SvcInk.fg()) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = SvcInk.fg())
                        }
                    }
                },
                actions = actions
            )
        },
        bottomBar = bottomBar,
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

/** Compact card on the employer and worker home that opens DutyPe Services (fallback for the showcase). */
@Composable
fun HomeServicesEntryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
        border = BorderStroke(1.dp, SvcLine.bg())
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.HomeRepairService, contentDescription = null, tint = SvcInk.fg(), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.svc_entry_title, "Khammam"),
                    fontWeight = FontWeight.Bold,
                    color = SvcInk.fg(),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.svc_entry_subtitle), color = SvcMuted.fg(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(stringResource(R.string.svc_book), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

// ─────────────────────────── Shared pieces ───────────────────────────

/** Hero banner: Urban Company / Pronto style deep obsidian card with clean white & slate typography. */
@Composable
internal fun ServicesHero(city: String, firstBookingFree: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .padding(18.dp)
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.svc_now_in, city), color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.svc_hero_title),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 26.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.svc_hero_sub),
            color = Color(0xFF94A3B8),
            fontSize = 12.5.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeroPoint(Icons.Filled.Verified, stringResource(R.string.svc_point_verified))
            HeroPoint(Icons.Filled.Payments, stringResource(R.string.svc_point_pay_after))
            HeroPoint(Icons.Filled.EventAvailable, stringResource(R.string.svc_point_rework))
        }
        if (firstBookingFree) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEF3C7))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.svc_first_free),
                    color = Color(0xFF92400E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun HeroPoint(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Category tiles, 4 per row in a clean square grid matching Urban Company (Reference Image 1). */
@Composable
internal fun CategoryTiles(
    categories: List<ServiceCategory>,
    lang: String,
    selected: String? = null,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        categories.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { cat ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable { onSelect(cat.id) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.bg())
                                .border(1.dp, Color(0xFFE2E8F0).bd(), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryClayImage(
                                category = cat,
                                isSelected = false,
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                iconModifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = cat.label(lang),
                            fontSize = 11.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SvcInk.fg(),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** "Keep ready" / "Partner brings" list with an icon header and clean check icons. */
@Composable
internal fun ItemsBox(title: String, items: List<String>, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (color == SvcGreen) Icons.Filled.Inventory2 else Icons.Filled.Build, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
        }
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(item, fontSize = 12.5.sp, color = SvcInk.fg())
            }
        }
    }
}

/**
 * Ultra-clean scope/inclusions card matching Urban Company / InstaHelp Gold specification.
 * Uses minimalist stroke checkmark (green) and stroke cross (red) with isolated right-side visual.
 */
@Composable
internal fun ServiceScopeCard(
    title: String,
    included: List<String>,
    excluded: List<String> = emptyList(),
    imageRes: Int? = null,
    imageUrl: String? = null,
    fallbackVector: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(Modifier.height(2.dp))
                included.forEach { inc ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 1.5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color(0xFF0F9D58),
                            modifier = Modifier
                                .size(15.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = inc,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            lineHeight = 17.5.sp
                        )
                    }
                }
                excluded.forEach { exc ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 1.5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = exc,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 17.5.sp
                        )
                    }
                }
            }

            // Right-side isolated visual asset
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (!imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        error = {
                            if (fallbackVector != null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(fallbackVector, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    )
                } else if (fallbackVector != null) {
                    Icon(
                        imageVector = fallbackVector,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(36.dp)
                    )
                }
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
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.bg())
            .border(1.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(b.category), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (b.status == BookingStatus.SEARCHING) Color(0xFFF59E0B) else SvcGreen))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (b.status == BookingStatus.SEARCHING) "SEARCHING IN KHAMMAM" else "LIVE TRACKING",
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(b.serviceName, fontWeight = FontWeight.Bold, color = SvcInk.fg(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.5.sp)
                if (b.partnerName.isNotBlank()) {
                    Text("Partner: ${b.partnerName}", fontSize = 12.sp, color = SvcMuted.fg())
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Text(
                    text = statusLabel(b.status),
                    color = color,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        if (b.status == BookingStatus.SEARCHING) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = color
            )
        }
    }
}

/** Automatic Post-Booking Rating Modal to build platform trust & reviews */
@Composable
fun PostBookingRatingModal(
    booking: ServiceBooking,
    onDismiss: () -> Unit,
    onSubmitRating: (stars: Int, review: String) -> Unit
) {
    var stars by remember { mutableIntStateOf(5) }
    var review by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val quickTags = listOf("On Time ⏰", "Polite & Professional 👔", "Great Work 🛠️", "Fair Price 💰", "Clean Work 🧹")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "How was your service?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${booking.serviceName} with ${booking.partnerName.ifBlank { "Partner" }}",
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive 5 Star Row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { i ->
                        IconButton(
                            onClick = { stars = i },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "$i stars",
                                tint = if (i <= stars) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                // Dynamic label for stars
                val ratingLabel = when (stars) {
                    5 -> "🌟 Excellent Service!"
                    4 -> "👍 Very Good"
                    3 -> "👌 Good / Satisfactory"
                    2 -> "👎 Needs Improvement"
                    else -> "⚠️ Poor Experience"
                }
                Text(
                    text = ratingLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (stars >= 4) Color(0xFF166534) else if (stars == 3) Color(0xFFB45309) else Color(0xFFDC2626)
                )

                // Quick Feedback Tags
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTags) { tag ->
                        val isSelected = selectedTag == tag
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable {
                                selectedTag = if (isSelected) null else tag
                                if (!isSelected && review.isBlank()) {
                                    review = tag
                                }
                            }
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Optional Review comment
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it.take(300) },
                    placeholder = { Text("Write a quick review for ${booking.partnerName.ifBlank { "partner" }}...", fontSize = 12.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (stars > 0 && !isSubmitting) {
                        isSubmitting = true
                        val finalReview = if (review.isNotBlank()) review else selectedTag.orEmpty()
                        onSubmitRating(stars, finalReview.trim())
                    }
                },
                enabled = stars > 0 && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Rating", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Ask Me Later", color = Color(0xFF64748B), fontSize = 13.sp)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

/** "How was the AC service? Rate Kiran" — for the latest completed booking not rated yet. */
@Composable
internal fun RateLastServiceCard(bookings: List<ServiceBooking>, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val b = bookings.firstOrNull { it.status == BookingStatus.COMPLETED && it.rating == 0 } ?: return
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFFBEB).bg())
            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(16.dp))
            .clickable { onOpen(b.id) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.svc_rate_nudge_title, b.serviceName), fontWeight = FontWeight.Bold, color = SvcInk.fg(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.svc_rate_nudge_body, b.partnerName.ifBlank { "the partner" }), fontSize = 12.sp, color = SvcMuted.fg())
        }
        Text(stringResource(R.string.svc_rate), color = Color(0xFFB45309), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ─────────────────────────── Urban Company / Pronto Style Promotional & Highlight Components ───────────────────────────

@Composable
private fun ServicesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    containerColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val serviceSuggestions = remember {
        listOf(
            "AC service",
            "House cleaning",
            "Plumber",
            "Electrician",
            "Painter",
            "Carpenter",
            "Water purifier",
            "Bathroom cleaning",
            "Appliance repair"
        )
    }
    var suggestionIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2600)
            suggestionIndex = (suggestionIndex + 1) % serviceSuggestions.size
        }
    }
    val currentSuggestion = serviceSuggestions[suggestionIndex]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                AnimatedContent(
                    targetState = currentSuggestion,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith
                        (slideOutVertically { -it } + fadeOut())
                    },
                    label = "searchPlaceholderAnim"
                ) { suggestion ->
                    Text(
                        text = "Search for '$suggestion'...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = hex.trim()
        val formatted = if (clean.startsWith("#")) clean else "#$clean"
        Color(android.graphics.Color.parseColor(formatted))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
private fun ServicesLocationTopRow(
    city: String,
    userLocation: LocationData?,
    onLocationClick: () -> Unit,
    onBookingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distKm = remember(userLocation) {
        if (userLocation != null && userLocation.latitude != 0.0 && userLocation.longitude != 0.0) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                17.2473,
                80.1514,
                results
            )
            results[0] / 1000.0
        } else {
            0.0
        }
    }
    val isOutOfArea = remember(userLocation, distKm) {
        userLocation != null && userLocation.latitude != 0.0 && userLocation.longitude != 0.0 && distKm > 15.0
    }
    val etaMinutes = remember(distKm) {
        (24 + (distKm * 2.2)).toInt().coerceIn(19, 59)
    }

    val displayLocation = remember(userLocation, city) {
        val area = userLocation?.area?.trim()
        val locCity = userLocation?.city?.trim()
        when {
            !area.isNullOrBlank() && !locCity.isNullOrBlank() -> "$area, $locCity"
            !area.isNullOrBlank() -> area
            !locCity.isNullOrBlank() -> locCity
            city.isNotBlank() && !city.contains("Hyderabad", ignoreCase = true) -> city
            else -> "Khammam"
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location row on left with pin icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable(onClick = onLocationClick)
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                if (isOutOfArea) {
                    Text(
                        text = "Coming Soon",
                        color = Color(0xFFFDE68A),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                } else {
                    Text(
                        text = "In $etaMinutes minutes",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                }
                Spacer(Modifier.height(1.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isOutOfArea) "$displayLocation • OUTSIDE ZONE".uppercase() else displayLocation.uppercase(),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Action buttons on the right: TWO white rounded squares (Bookings & Profile) with white icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bookings / Orders button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBookingsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = "My Bookings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Profile / Account button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RunningBorderPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "running_border")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .drawBehind {
                rotate(angle) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFFFFD700), // Gold
                                Color(0xFF38BDF8), // Cyan
                                Color(0xFFFFFFFF), // White spark
                                Color(0xFF818CF8), // Indigo
                                Color(0xFFFFD700)  // Gold
                            )
                        ),
                        radius = size.maxDimension
                    )
                }
            }
            .padding(1.8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = Color(0xFF0F172A),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun ServicesIntegratedHeroBanner(
    pagerState: androidx.compose.foundation.pager.PagerState,
    customBanners: List<PromoBanner> = emptyList(),
    onBookPromo: (targetServiceId: String?, targetCategoryId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (customBanners.isEmpty()) return

    val count = customBanners.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(146.dp)
            .clip(RoundedCornerShape(0.dp))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val banner = customBanners.getOrNull(page) ?: return@HorizontalPager
            val bgStart = parseHexColor(banner.bgStartColor, Color(0xFF0F172A))
            val bgEnd = parseHexColor(banner.bgEndColor, Color(0xFF1E3A8A))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                bgStart,
                                bgEnd.copy(alpha = 0.5f),
                                bgStart
                            )
                        )
                    )
                    .clickable { onBookPromo(banner.targetServiceId, banner.targetCategoryId) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
                    ) {
                        Text(
                            text = banner.headline.ifBlank { "DutyPe Assured Services" },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 21.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        RunningBorderPill(
                            text = banner.cta.ifBlank { "Book Now" },
                            onClick = { onBookPromo(banner.targetServiceId, banner.targetCategoryId) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        val scale = banner.imageScale.coerceIn(0.6f, 1.4f)
                        if (!banner.imageUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(banner.imageUrl)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = banner.headline,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxHeight(0.92f * scale)
                                    .width((130 * scale).dp)
                            )
                        } else {
                            HeroApplianceVisual()
                        }
                    }
                }
            }
        }

        // Pagination indicator pills in the bottom right corner
        if (count > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(count) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(if (isSelected) 14.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.35f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroApplianceVisual() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(130.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient blue glow behind device
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF3B82F6).copy(alpha = 0.6f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Sleek Black RO Purifier Dispenser Box
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(102.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF020617)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "HELLO",
                            color = Color(0xFF38BDF8),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF0F172A))
                    )
                }
            }

            // Companion smartphone mockup
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF090D16))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(3.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFF1E293B))
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF0369A1).copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("99% PURE", color = Color.White, fontSize = 5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HeroServiceAssuranceVisual() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF10B981).copy(alpha = 0.5f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF064E3B).copy(alpha = 0.6f))
                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Verified,
                contentDescription = null,
                tint = Color(0xFF34D399),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun ServicesPromoCarousel(
    onBookPromo: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .width(310.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onBookPromo),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF59E0B))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("⚡ SPECIAL OFFER", color = Color(0xFF1E1B4B), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("LIMITED PERIOD", color = Color.White.copy(alpha = 0.7f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Get 25% OFF on your first booking",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 23.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "AC repair, deep cleaning, plumbing & electricians",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text("Book Now", color = Color(0xFF1E1B4B), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF1E1B4B), modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .width(310.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onBookPromo),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF064E3B), Color(0xFF065F46))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.25f))
                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("🛡️ DUTYPE ASSURED", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Fixed Upfront Pricing & 7-Day Warranty",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 23.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "Background verified technicians • Pay post completion",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF10B981))
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text("Explore Services", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewAndNoteworthySection(
    services: List<ServiceItem>,
    lang: String,
    onBookService: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "New and noteworthy",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SvcInk.fg(),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(services.take(6), key = { "new_${it.id}" }) { service ->
                Card(
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onBookService(service.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0).bd(), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            ServiceClayImage(
                                service = service,
                                modifier = Modifier.fillMaxSize(),
                                iconModifier = Modifier.size(34.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFBE185D))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "New",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = service.label(lang),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SvcInk.fg(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Starts at ₹${service.price}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanerHomeStoryBanner(
    onExploreCleaning: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onExploreCleaning),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFDE68A))
                    )
                )
                .border(1.dp, Color(0xFFFCD34D), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD97706))
                            .padding(horizontal = 8.dp, vertical = 2.5.dp)
                    ) {
                        Text("✨ POPULAR IN KHAMMAM", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A cleaner home, without any hassle",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF451A03),
                    lineHeight = 23.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Full home deep cleaning starting from ₹199 • Hospital-grade sanitization",
                    fontSize = 12.5.sp,
                    color = Color(0xFF78350F),
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Book Cleaning",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MostBookedServicesSection(
    services: List<ServiceItem>,
    lang: String,
    onBookService: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Most booked services",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SvcInk.fg(),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(services.take(8), key = { "most_${it.id}" }) { service ->
                Card(
                    modifier = Modifier
                        .width(175.dp)
                        .height(262.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Image covers top section of outer card fully
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(118.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .background(Color(0xFFF8FAFC)),
                            contentAlignment = Alignment.Center
                        ) {
                            ServiceClayImage(
                                service = service,
                                modifier = Modifier.fillMaxSize(),
                                iconModifier = Modifier.size(46.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.TopStart) {
                                    Text(
                                        text = service.label(lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SvcInk.fg(),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 17.sp
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("4.85 (12.4k)", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SvcMuted.fg())
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Starts at", fontSize = 10.sp, color = SvcMuted.fg())
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("₹${service.price}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SvcInk.fg())
                                        if (service.mrp > service.price) {
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "₹${service.mrp}",
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF94A3B8),
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .clickable { onBookService(service.id) }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ADD",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────── Dedicated Category Services Screen ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryServicesScreen(
    categoryId: String,
    navController: NavController,
    viewModel: HomeServicesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lang = LocaleHelper.getLanguage(context)
    val catalog by viewModel.catalog.collectAsState()
    val error by viewModel.catalogError.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var variantSheetService by remember { mutableStateOf<ServiceItem?>(null) }
    var detailSheetService by remember { mutableStateOf<ServiceItem?>(null) }

    LaunchedEffect(Unit) { viewModel.loadCatalog() }

    val c = catalog
    val cat = c?.categories?.firstOrNull { it.id == categoryId }
    val categoryServices = c?.services?.filter { it.category == categoryId }.orEmpty()
    val filteredServices = if (searchQuery.isBlank()) categoryServices
    else categoryServices.filter {
        it.label(lang).contains(searchQuery, ignoreCase = true) ||
        it.includes.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = cat?.label(lang) ?: "Category Services",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SvcInk.fg()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SvcInk.fg())
                    }
                },
                actions = {
                    TextButton(onClick = { navController.navigate(Routes.SERVICES_BOOKINGS) }) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.svc_my_bookings), fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.bg())
            )
        },
        containerColor = Color(0xFFF8FAFC).bg()
    ) { padding ->
        if (c == null) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (error != null) DutyPeErrorState(error, onRetry = { viewModel.loadCatalog(force = true) })
                else DutyPeLoadingList(rows = 5)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (cat != null) {
                item(key = "cat_hero") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFDCFCE7))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("4.85 ★ • ${categoryServices.size} Services", color = Color(0xFF15803D), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(cat.label(lang), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SvcInk.fg())
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (cat.isSkilled) "Experienced certified professionals with specialized tools & guaranteed quality"
                                    else "Quick, reliable doorstep help at standardized upfront prices",
                                    fontSize = 12.sp,
                                    color = SvcMuted.fg(),
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0).bd(), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryClayImage(
                                    category = cat,
                                    isSelected = false,
                                    modifier = Modifier.size(64.dp),
                                    iconModifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                item(key = "trust_guarantee") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Verified Pros", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E40AF))
                        }
                        Text("•", color = Color(0xFF93C5FD))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("30-Day Guarantee", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E40AF))
                        }
                        Text("•", color = Color(0xFF93C5FD))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Payments, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Pay Post-Work", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E40AF))
                        }
                    }
                }

                item(key = "cat_search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search ${cat.label(lang)} services...", fontSize = 13.5.sp, color = SvcMuted.fg()) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SvcMuted.fg(), modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.bg(),
                            unfocusedContainerColor = Color.White.bg(),
                            focusedBorderColor = SvcBlue.bd(),
                            unfocusedBorderColor = Color(0xFFE2E8F0).bd()
                        ),
                        singleLine = true
                    )
                }
            }

            item(key = "services_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Services (${filteredServices.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SvcInk.fg()
                    )
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "Filtered",
                            fontSize = 12.sp,
                            color = SvcBlue.fg(),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (filteredServices.isEmpty()) {
                item(key = "no_services") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (searchQuery.isNotBlank()) "No services matching \"$searchQuery\""
                            else "No services available in this category yet.",
                            color = SvcMuted.fg(),
                            fontSize = 14.sp
                        )
                        if (searchQuery.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { searchQuery = "" }) {
                                Text("Clear search", color = SvcBlue.fg())
                            }
                        }
                    }
                }
            } else {
                items(filteredServices, key = { it.id }) { service ->
                    ServiceRow(
                        service = service,
                        lang = lang,
                        keepReady = c.itemLabels(service.provide, lang),
                        onBook = {
                            navController.navigate(Routes.servicesBookRoute(service.id))
                        },
                        onOptionsClick = {
                            variantSheetService = service
                        },
                        onCardClick = {
                            detailSheetService = service
                        }
                    )
                }
            }

            item(key = "cat_how") { HowItWorks() }
        }
    }

    // Modal Bottom Sheet: Options / Variants Selector
    variantSheetService?.let { service ->
        VariantSelectorBottomSheet(
            service = service,
            onDismiss = { variantSheetService = null },
            onSelectOption = { option ->
                variantSheetService = null
                navController.navigate(Routes.servicesBookRoute(service.id))
            }
        )
    }

    // Modal Bottom Sheet: Service Full Details & Inclusions
    detailSheetService?.let { service ->
        ServiceDetailsBottomSheet(
            service = service,
            lang = lang,
            onDismiss = { detailSheetService = null },
            onBookNow = {
                detailSheetService = null
                navController.navigate(Routes.servicesBookRoute(service.id))
            },
            onOptionsClick = if (service.hasMultipleOptions) {
                {
                    detailSheetService = null
                    variantSheetService = service
                }
            } else null
        )
    }
}

@Composable
fun ServicesHomeShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "services_home_shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val lightShimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE2E8F0),
            Color(0xFFF8FAFC),
            Color(0xFFE2E8F0)
        ),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    val darkShimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E293B),
            Color(0xFF334155),
            Color(0xFF1E293B)
        ),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC).bg())
            .verticalScroll(rememberScrollState())
    ) {
        // Dark Navy Header Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                // Top location row & action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(darkShimmerBrush)
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(darkShimmerBrush)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(darkShimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(darkShimmerBrush)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Search bar placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(lightShimmerBrush)
                    )
                }
            }
        }

        // Hero Banner Shimmer Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F172A),
                            Color(0xFF0F172A).copy(alpha = 0.85f),
                            Color(0xFFF8FAFC).bg()
                        )
                    )
                )
                .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(146.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(darkShimmerBrush)
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .width(170.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(darkShimmerBrush)
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(darkShimmerBrush)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(darkShimmerBrush)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(darkShimmerBrush)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // "What needs fixing?" Section Heading Shimmer
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(140.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(lightShimmerBrush)
        )

        Spacer(Modifier.height(12.dp))

        // 4x2 Category Grid Shimmer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(4) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(94.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(lightShimmerBrush)
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(lightShimmerBrush)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Featured Service Cards Shimmer
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .width(160.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(lightShimmerBrush)
        )

        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(lightShimmerBrush)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(lightShimmerBrush)
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(lightShimmerBrush)
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(lightShimmerBrush)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(lightShimmerBrush)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────── Services home (Urban Company / Pronto Experience) ───────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServicesHomeScreen(
    navController: NavController,
    rootNavController: NavController? = null,
    viewModel: HomeServicesViewModel = hiltViewModel()
) {
    val nav = rootNavController ?: navController
    val context = LocalContext.current
    val lang = LocaleHelper.getLanguage(context)
    val catalog by viewModel.catalog.collectAsState()
    val error by viewModel.catalogError.collectAsState()
    val bookings by viewModel.myBookings.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var ratingBookingToPrompt by remember { mutableStateOf<ServiceBooking?>(null) }
    var ratingPromptDismissed by remember { mutableStateOf(false) }
    var showLocalitySheet by remember { mutableStateOf(false) }

    val localView = androidx.compose.ui.platform.LocalView.current
    if (!localView.isInEditMode) {
        SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            androidx.core.view.WindowCompat.getInsetsController(window, localView).isAppearanceLightStatusBars = false
        }
    }
    DisposableEffect(localView) {
        val window = (context as? android.app.Activity)?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
        val previousLight = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (controller != null && previousLight != null) {
                controller.isAppearanceLightStatusBars = previousLight
            }
        }
    }

    LaunchedEffect(bookings) {
        if (!ratingPromptDismissed) {
            val unrated = bookings.firstOrNull { it.status == BookingStatus.COMPLETED && it.rating == 0 }
            if (unrated != null && ratingBookingToPrompt == null) {
                ratingBookingToPrompt = unrated
            }
        }
    }

    val scope = rememberCoroutineScope()
    val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            val loc = viewModel.userLocation.value
            if (loc == null || !loc.hasValidCoordinates()) {
                scope.launch { viewModel.currentPlace() }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadCatalog()
        val loc = viewModel.userLocation.value
        val needsLocation = loc == null || !loc.hasValidCoordinates()
        if (needsLocation) {
            if (hasLocationPermission) {
                viewModel.currentPlace()
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }

    val c = catalog
    if (c == null) {
        Box(Modifier.fillMaxSize()) {
            if (error != null) DutyPeErrorState(error, onRetry = { viewModel.loadCatalog(force = true) })
            else ServicesHomeShimmer()
        }
        return
    }

    val unratedBooking = ratingBookingToPrompt
    if (unratedBooking != null) {
        PostBookingRatingModal(
            booking = unratedBooking,
            onDismiss = {
                ratingBookingToPrompt = null
                ratingPromptDismissed = true
            },
            onSubmitRating = { stars, review ->
                scope.launch {
                    viewModel.rate(unratedBooking.id, stars, review)
                        .onSuccess { Toast.makeText(context, "Thank you for rating!", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                    ratingBookingToPrompt = null
                    ratingPromptDismissed = true
                }
            }
        )
    }

    val active = bookings.firstOrNull { it.isOpen }

    val matchingServices = if (searchQuery.isNotBlank()) {
        val q = searchQuery.trim().lowercase()
        c.services.filter {
            it.label(lang).lowercase().contains(q) ||
            it.name.lowercase().contains(q) ||
            it.category.lowercase().contains(q)
        }
    } else null

    val listState = rememberLazyListState()
    val banners = c.promoBanners
    val totalBanners = banners.size
    val pagerState = rememberPagerState(pageCount = { if (totalBanners > 0) totalBanners else 1 })

    LaunchedEffect(pagerState, totalBanners) {
        if (totalBanners > 1) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                val nextPage = (pagerState.currentPage + 1) % totalBanners
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    // Dynamic header background color matching current active promo banner slide
    val (activeStartColor, activeEndColor) = remember(pagerState.currentPage, banners) {
        if (banners.isNotEmpty() && pagerState.currentPage in banners.indices) {
            val b = banners[pagerState.currentPage]
            parseHexColor(b.bgStartColor, Color(0xFF0F172A)) to parseHexColor(b.bgEndColor, Color(0xFF1E3A8A))
        } else {
            Color(0xFF0F172A) to Color(0xFF1E3A8A)
        }
    }

    val animatedStartColor by animateColorAsState(
        targetValue = activeStartColor,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "headerStartColor"
    )

    // Detect whether user has scrolled down past the top location bar
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
        }
    }

    if (!localView.isInEditMode) {
        SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            androidx.core.view.WindowCompat.getInsetsController(window, localView).isAppearanceLightStatusBars = isScrolled
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC).bg())
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Item 0: Top location row + History & Profile buttons
            // Scrolls away when user scrolls down
            item(key = "top_location_row") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(animatedStartColor)
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp)
                ) {
                    ServicesLocationTopRow(
                        city = c.city,
                        userLocation = userLocation,
                        onLocationClick = { showLocalitySheet = true },
                        onBookingsClick = { nav.navigate(Routes.SERVICES_BOOKINGS) },
                        onProfileClick = { runCatching { nav.navigate(Routes.EMPLOYER_PROFILE) } }
                    )
                }
            }

            // Sticky Header: Search Bar docks at top under status bar when scrolling! Pure white background with border
            stickyHeader(key = "sticky_search_bar") {
                val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val animatedTopPad by animateDpAsState(
                    targetValue = if (isScrolled) statusBarInset else 0.dp,
                    animationSpec = tween(150, easing = FastOutSlowInEasing),
                    label = "stickySearchTopPad"
                )
                val stickyBgColor by animateColorAsState(
                    targetValue = if (isScrolled) Color.White else animatedStartColor,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "stickySearchBg"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(stickyBgColor)
                        .then(
                            if (isScrolled) {
                                Modifier.drawBehind {
                                    drawLine(
                                        color = Color(0xFFE2E8F0),
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            } else Modifier
                        )
                        .padding(
                            top = animatedTopPad + (if (isScrolled) 8.dp else 0.dp),
                            start = 16.dp,
                            end = 16.dp,
                            bottom = if (isScrolled) 8.dp else 0.dp
                        )
                ) {
                    ServicesSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        containerColor = if (isScrolled) Color(0xFFF8FAFC) else Color.White
                    )
                }
            }

            // Promotional banner: only shown when not actively searching AND banners configured
            if (searchQuery.isBlank() && banners.isNotEmpty()) {
                item(key = "hero_promo_banner") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        animatedStartColor,
                                        animatedStartColor.copy(alpha = 0.85f),
                                        Color(0xFFF8FAFC).bg()
                                    )
                                )
                            )
                            .padding(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 10.dp)
                    ) {
                        ServicesIntegratedHeroBanner(
                            pagerState = pagerState,
                            customBanners = banners,
                            onBookPromo = { targetSvc, targetCat ->
                                when {
                                    !targetSvc.isNullOrBlank() -> nav.navigate(Routes.servicesBookRoute(targetSvc))
                                    !targetCat.isNullOrBlank() -> nav.navigate(Routes.servicesCategoryRoute(targetCat))
                                    c.services.isNotEmpty() -> nav.navigate(Routes.servicesBookRoute(c.services.first().id))
                                }
                            }
                        )
                    }
                }
            }

            item(key = "offline") {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    OfflineCachedNote()
                }
            }

            if (matchingServices != null) {
                // Search Results State
                item(key = "search_header") {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "Search results (${matchingServices.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SvcInk.fg()
                        )
                    }
                }
                if (matchingServices.isEmpty()) {
                    item(key = "no_search_results") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No services found for \"$searchQuery\"", color = SvcMuted.fg(), fontSize = 14.sp)
                        }
                    }
                } else {
                    items(matchingServices, key = { it.id }) { service ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            ServiceRow(service, lang, c.itemLabels(service.provide, lang)) {
                                nav.navigate(Routes.servicesBookRoute(service.id))
                            }
                        }
                    }
                }
            } else {
                if (active != null) {
                    item(key = "active") {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            ActiveBookingStrip(active, onClick = { nav.navigate(Routes.servicesBookingRoute(active.id)) })
                        }
                    }
                }
                if (bookings.any { it.status == BookingStatus.COMPLETED && it.rating == 0 }) {
                    item(key = "rate") {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            RateLastServiceCard(bookings, onOpen = { nav.navigate(Routes.servicesBookingRoute(it)) })
                        }
                    }
                }

                // 2. Category Grid ("What needs fixing?") - Navigates directly to dedicated CategoryServicesScreen!
                item(key = "cat_title") {
                    SectionTitle(
                        text = stringResource(R.string.svc_choose_category),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
                item(key = "categories") {
                    CategoryTiles(
                        categories = c.categories,
                        lang = lang,
                        selected = null,
                        onSelect = { catId ->
                            nav.navigate(Routes.servicesCategoryRoute(catId))
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Showcase Card: DutyPe Verified Staff & Workers Hiring
                item(key = "hiring_showcase_hero") {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        EmployerExecutiveHeroCard(
                            companyName = "",
                            onPostJob = { nav.navigate(Routes.EMPLOYER_POST_JOB) },
                            onVoiceJob = { nav.navigate(Routes.dutypeAiRoute(listen = true)) }
                        )
                    }
                }

                // 3. New and Noteworthy Section (Configured by admin via noteworthyServiceIds)
                if (c.services.isNotEmpty()) {
                    item(key = "new_and_noteworthy") {
                        NewAndNoteworthySection(
                            services = c.noteworthyServices,
                            lang = lang,
                            onBookService = { nav.navigate(Routes.servicesBookRoute(it)) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                // 4. Promotional Story Banner (Image 3 bottom: "A cleaner home, without any hassle")
                item(key = "cleaner_home_banner") {
                    CleanerHomeStoryBanner(
                        onExploreCleaning = {
                            val cleanCat = c.categories.firstOrNull { it.id.contains("clean", ignoreCase = true) }
                            if (cleanCat != null) nav.navigate(Routes.servicesCategoryRoute(cleanCat.id))
                            else if (c.categories.isNotEmpty()) nav.navigate(Routes.servicesCategoryRoute(c.categories.first().id))
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                // 5. Most Booked Services (Image 4!)
                if (c.services.isNotEmpty()) {
                    item(key = "most_booked") {
                        MostBookedServicesSection(
                            services = c.mostBookedServices,
                            lang = lang,
                            onBookService = { nav.navigate(Routes.servicesBookRoute(it)) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                // 6. Spotlight Category Sections (Image 4 bottom: with "See all" button!)
                val spotlightCategories = c.categories.take(2)
                spotlightCategories.forEach { spotCat ->
                    val spotServices = c.services.filter { it.category == spotCat.id }.take(2)
                    if (spotServices.isNotEmpty()) {
                        item(key = "spotlight_header_${spotCat.id}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = spotCat.label(lang),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = SvcInk.fg()
                                    )
                                    Text(
                                        text = if (spotCat.isSkilled) "Verified certified pros" else "Doorstep service in your area",
                                        fontSize = 12.sp,
                                        color = SvcMuted.fg()
                                    )
                                }
                                TextButton(onClick = { nav.navigate(Routes.servicesCategoryRoute(spotCat.id)) }) {
                                    Text(
                                        text = "See all",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF6366F1)
                                    )
                                    Icon(
                                        Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        items(spotServices, key = { "spot_${it.id}" }) { service ->
                            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                ServiceRow(service, lang, c.itemLabels(service.provide, lang)) {
                                    nav.navigate(Routes.servicesBookRoute(service.id))
                                }
                            }
                        }
                    }
                }

                // 7. Offers if any
                if (c.offers.isNotEmpty()) {
                    item(key = "offers_title") {
                        SectionTitle(
                            text = stringResource(R.string.svc_offers),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        )
                    }
                    item(key = "offers") {
                        OfferTickets(
                            offers = c.offers,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // 8. How It Works & Guarantees
                item(key = "how") {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        HowItWorks()
                    }
                }

                // Extra bottom spacer
                item(key = "bottom_spacer") {
                    Spacer(Modifier.height(36.dp).navigationBarsPadding())
                }
            }

        }

        // Floating Pulsing Voice AI FAB (DutyPe AI Assistant for voice booking/hiring)
        VoicePulsingFab(
            onClick = {
                nav.navigate(Routes.dutypeAiRoute(listen = true))
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 105.dp)
        )

        if (showLocalitySheet) {
            LocalityPickerBottomSheet(
                currentLocation = userLocation,
                onDismiss = { showLocalitySheet = false },
                onUseGps = {
                    showLocalitySheet = false
                    viewModel.resetToGps()
                    if (!hasLocationPermission) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onSelectLocality = { name, lat, lng ->
                    showLocalitySheet = false
                    viewModel.setManualLocation(
                        area = name,
                        city = if (lat == 17.3850) "Hyderabad" else "Khammam",
                        lat = lat,
                        lng = lng
                    )
                },
                onSearchPlaces = { query -> viewModel.searchPlaces(query) }
            )
        }
    }
}

private data class LocalityOption(val name: String, val desc: String, val lat: Double, val lng: Double, val isOutside: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalityPickerBottomSheet(
    currentLocation: LocationData?,
    onDismiss: () -> Unit,
    onUseGps: () -> Unit,
    onSelectLocality: (name: String, lat: Double, lng: Double) -> Unit,
    onSearchPlaces: suspend (String) -> List<com.example.dutype.models.PlaceSuggestion>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var isSearchingPlaces by remember { mutableStateOf(false) }
    var dynamicPlaces by remember { mutableStateOf<List<LocalityOption>>(emptyList()) }

    val allLocalities = remember {
        listOf(
            LocalityOption("VDO's Colony", "Central Khammam • Residential Hub", 17.2580, 80.1550),
            LocalityOption("Wyra Road", "City Center • Hub", 17.2473, 80.1514),
            LocalityOption("Gandhi Chowk", "Main Market & Bus Stand", 17.2485, 80.1528),
            LocalityOption("Mamata Hospital Road", "Medical & Residential", 17.2650, 80.1620),
            LocalityOption("Rotary Nagar / Collectorate", "Administrative Zone", 17.2550, 80.1480),
            LocalityOption("Ballepalli", "South-West Khammam", 17.2350, 80.1380),
            LocalityOption("Khanapuram Haveli", "North Khammam", 17.2610, 80.1380),
            LocalityOption("NSP Colony", "Residential Area", 17.2390, 80.1450),
            LocalityOption("Trunk Road / Kaman Bazaar", "Commercial Shopping", 17.2430, 80.1550),
            LocalityOption("Pakabanda", "South Khammam", 17.2350, 80.1600),
            LocalityOption("Yellandu Cross Road", "Bypass & Outer Ring", 17.2720, 80.1580),
            LocalityOption("Mustafa Nagar", "Residential Hub", 17.2510, 80.1420),
            LocalityOption("Kaviraj Nagar", "Residential Area", 17.2560, 80.1600),
            LocalityOption("Burhanpuram", "Central Khammam", 17.2460, 80.1490),
            LocalityOption("Mayuri Centre", "Commercial Junction", 17.2490, 80.1560),
            LocalityOption("Srinivasa Nagar / Bank Colony", "Residential Area", 17.2540, 80.1620),
            LocalityOption("RTC Bus Stand Area", "Transit Hub", 17.2478, 80.1535),
            LocalityOption("Outside Zone Test (Hyderabad)", "185 km Away • Out of Service Zone", 17.3850, 78.4867, isOutside = true)
        )
    }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            dynamicPlaces = emptyList()
            isSearchingPlaces = false
            return@LaunchedEffect
        }
        isSearchingPlaces = true
        kotlinx.coroutines.delay(350)
        try {
            val suggestions = onSearchPlaces(trimmed)
            val mapped = suggestions.mapNotNull { suggestion ->
                if (suggestion.latitude != 0.0 && suggestion.longitude != 0.0) {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        suggestion.latitude,
                        suggestion.longitude,
                        17.2473,
                        80.1514,
                        results
                    )
                    val distKm = results[0] / 1000.0
                    val isOut = distKm > 15.0
                    LocalityOption(
                        name = suggestion.description,
                        desc = if (isOut) "${String.format("%.1f", distKm)} km Away • Outside Zone" else "${String.format("%.1f", distKm)} km • In Khammam Zone",
                        lat = suggestion.latitude,
                        lng = suggestion.longitude,
                        isOutside = isOut
                    )
                } else null
            }
            dynamicPlaces = mapped
        } catch (_: Exception) {
            dynamicPlaces = emptyList()
        } finally {
            isSearchingPlaces = false
        }
    }

    val filtered = remember(query, dynamicPlaces) {
        if (query.isBlank()) {
            allLocalities
        } else {
            val localMatches = allLocalities.filter {
                it.name.contains(query, ignoreCase = true) || it.desc.contains(query, ignoreCase = true)
            }
            val existingNames = localMatches.map { it.name.lowercase().trim() }.toSet()
            val extraDynamic = dynamicPlaces.filter { it.name.lowercase().trim() !in existingNames }
            localMatches + extraDynamic
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White.bg(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Choose Service Location",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SvcInk.fg()
                    )
                    Text(
                        text = "DutyPe is active in Khammam City (15km radius)",
                        fontSize = 12.sp,
                        color = SvcMuted.fg()
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = SvcMuted.fg())
                }
            }

            Spacer(Modifier.height(14.dp))

            // Use GPS Location Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onUseGps),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEFF6FF).bg(),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Use Current Location (GPS)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E40AF)
                        )
                        Text(
                            text = "Auto-detect phone GPS (e.g. VDO's Colony / Khammam)",
                            fontSize = 11.5.sp,
                            color = Color(0xFF3B82F6)
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Search Bar with dynamic indicator
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search any Khammam area or landmark...", fontSize = 13.sp, color = SvcMuted.fg()) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SvcMuted.fg(), modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (isSearchingPlaces) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = SvcBlue.fg(), strokeWidth = 2.dp)
                    } else if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = SvcMuted.fg(), modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.bg(),
                    unfocusedContainerColor = Color.White.bg(),
                    focusedBorderColor = SvcBlue.bd(),
                    unfocusedBorderColor = Color(0xFFE2E8F0).bd()
                )
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (query.isBlank()) "POPULAR KHAMMAM AREAS" else "SEARCH RESULTS (${filtered.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty() && !isSearchingPlaces) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No localities found for \"$query\"\nTry searching a landmark or area name",
                        fontSize = 13.sp,
                        color = SvcMuted.fg(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { "${it.name}_${it.lat}_${it.lng}" }) { loc ->
                        val isCurrent = currentLocation?.area?.equals(loc.name, ignoreCase = true) == true
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLocality(loc.name, loc.lat, loc.lng) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) Color(0xFFF1F5F9).bg() else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (loc.isOutside) Icons.Filled.Warning else Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = if (loc.isOutside) Color(0xFFDC2626) else if (isCurrent) SvcBlue.fg() else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = loc.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (loc.isOutside) Color(0xFFDC2626) else SvcInk.fg(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = loc.desc,
                                        fontSize = 11.sp,
                                        color = SvcMuted.fg(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isCurrent) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = SvcBlue.fg(),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ServiceRow(
    service: ServiceItem,
    lang: String,
    keepReady: List<String>,
    onOptionsClick: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
    onBook: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCardClick?.invoke() ?: onBook() }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
        border = BorderStroke(1.dp, SvcLine.bg())
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = service.label(lang),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SvcInk.fg(),
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(4.dp))
                // Micro-Rating Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${service.rating} (${service.ratingCount})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = SvcMuted.fg()
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Price & Duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${service.price}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SvcInk.fg()
                    )
                    if (service.mrp > service.price) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "₹${service.mrp}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textDecoration = TextDecoration.LineThrough
                        )
                        if (service.discountPercent > 0) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "${service.discountPercent}% OFF",
                                    color = Color(0xFF15803D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⏱ ${service.durationMin} min",
                            color = Color(0xFF475569),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (service.inspection) {
                    Spacer(Modifier.height(4.dp))
                    Pill(stringResource(R.string.svc_inspection_badge), SvcOrange)
                }
                Spacer(Modifier.height(6.dp))

                // Top Inclusions Bullets (Urban Company format)
                val incList = if (service.inclusions.isNotEmpty()) service.inclusions.take(2)
                              else service.includes.split("·", "\n").map { it.trim() }.filter { it.isNotBlank() }.take(2)
                if (incList.isNotEmpty()) {
                    incList.forEach { inc ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(inc, fontSize = 11.5.sp, color = SvcMuted.fg(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    Text(
                        text = service.includes,
                        color = SvcMuted.fg(),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (keepReady.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.svc_keep_ready_short, keepReady.take(2).joinToString(", ")),
                        color = SvcGreen,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            // Right Side: Visual Squircle + Signature BOOK or OPTIONS Pill Button at bottom right
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier
                        .size(82.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    ServiceClayImage(
                        service = service,
                        modifier = Modifier.fillMaxSize(),
                        iconModifier = Modifier.size(42.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // If service has multiple options/variants, show OPTIONS ▾ button
                if (service.hasMultipleOptions) {
                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEEF2FF))
                                .border(1.5.dp, Color(0xFF4F46E5), RoundedCornerShape(8.dp))
                                .clickable(onClick = { onOptionsClick?.invoke() ?: onBook() })
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "OPTIONS",
                                    color = Color(0xFF4F46E5),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            text = "customizable",
                            fontSize = 9.5.sp,
                            color = Color(0xFF6366F1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    // Urban Company / Pronto signature BOOK pill button
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .clickable(onClick = onBook)
                            .padding(horizontal = 22.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.svc_book).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────── Urban Company Sticky Mini-Cart Bar ───────────────────────────

@Composable
fun StickyMiniCartBar(
    service: ServiceItem,
    option: ServiceOption?,
    onViewCartClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val effectivePrice = option?.price ?: service.price
                val effectiveMrp = option?.mrp ?: service.mrp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "1 item · ₹$effectivePrice",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (effectiveMrp > effectivePrice) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "₹$effectiveMrp",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = option?.title ?: service.name,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onViewCartClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Book Now", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                }
            }
        }
    }
}

// ─────────────────────────── Variant / Options Selector Sheet ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantSelectorBottomSheet(
    service: ServiceItem,
    onDismiss: () -> Unit,
    onSelectOption: (ServiceOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedOption by remember { mutableStateOf(service.options.firstOrNull()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White.bg(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SvcInk.fg()
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Select option / package based on your requirement",
                        fontSize = 12.sp,
                        color = SvcMuted.fg()
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = SvcMuted.fg())
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Options List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                service.options.forEach { option ->
                    val isSelected = selectedOption?.id == option.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFF0FDF4) else Color.White.bg()
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF10B981) else Color(0xFFE2E8F0)
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedOption = option },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF10B981)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SvcInk.fg()
                                )
                                if (option.description.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = option.description,
                                        fontSize = 11.5.sp,
                                        color = SvcMuted.fg()
                                    )
                                }
                                if (option.durationMin > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "⏱ ${option.durationMin} mins",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${option.price}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SvcInk.fg()
                                )
                                if (option.mrp > option.price) {
                                    Text(
                                        text = "₹${option.mrp}",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8),
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                    if (option.discountPercent > 0) {
                                        Text(
                                            text = "${option.discountPercent}% OFF",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // CTA Button
            Button(
                onClick = {
                    selectedOption?.let { onSelectOption(it) }
                },
                enabled = selectedOption != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (selectedOption != null) "Select & Book · ₹${selectedOption?.price}" else "Select an option",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─────────────────────────── Service Details & Inclusions Bottom Sheet ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsBottomSheet(
    service: ServiceItem,
    lang: String,
    onDismiss: () -> Unit,
    onBookNow: () -> Unit,
    onOptionsClick: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White.bg(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Close & Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("★ ${service.rating} (${service.ratingCount})", color = Color(0xFF15803D), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("⏱ ${service.durationMin} mins", fontSize = 12.sp, color = SvcMuted.fg())
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = SvcMuted.fg())
                }
            }

            // Image & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    ServiceClayImage(
                        service = service,
                        modifier = Modifier.size(72.dp),
                        iconModifier = Modifier.size(38.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.label(lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SvcInk.fg(),
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹${service.price}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SvcInk.fg())
                        if (service.mrp > service.price) {
                            Spacer(Modifier.width(8.dp))
                            Text("₹${service.mrp}", fontSize = 14.sp, color = Color(0xFF94A3B8), textDecoration = TextDecoration.LineThrough)
                            if (service.discountPercent > 0) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFDCFCE7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("${service.discountPercent}% OFF", color = Color(0xFF15803D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Trust highlights
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Verified Pro", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                }
                Text("•", color = Color(0xFFCBD5E1))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("30-Day Guarantee", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                }
                Text("•", color = Color(0xFFCBD5E1))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Payments, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pay After Service", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                }
            }

            // What's Included (Clean Modern Scope Cards)
            val isCleaningOrHelper = service.category == "CLEANING" || service.category == "HOME_HELP"
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isCleaningOrHelper) "One helper can do it all" else "What's included",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SvcInk.fg()
                )

                if (isCleaningOrHelper) {
                    ServiceScopeCard(
                        title = "Kitchen cleaning",
                        included = listOf("Crockery & lunch boxes", "Wiping cabinet exterior"),
                        excluded = listOf("Hard food stains", "Chimney"),
                        imageRes = R.drawable.svc_scope_kitchen
                    )
                    ServiceScopeCard(
                        title = "Meal prep & serving",
                        included = listOf("Veggies chopping & salad prep", "Meat marination", "Serving food"),
                        excluded = listOf("Cooking full meals"),
                        imageRes = R.drawable.svc_scope_mealprep
                    )
                    ServiceScopeCard(
                        title = "Mopping, dusting & wiping",
                        included = listOf("Dusting & Mopping floor", "Wet wiping furniture"),
                        excluded = listOf("Wiping walls", "Hard to reach areas"),
                        imageRes = R.drawable.svc_scope_mopping
                    )
                    ServiceScopeCard(
                        title = "Bathroom cleaning",
                        included = listOf("Toilet seat & wash basin", "Tiles scrub & mirror wipe"),
                        excluded = listOf("Acid wash & severe scale", "Ceiling / exhaust motor"),
                        imageRes = R.drawable.svc_scope_bathroom
                    )
                } else {
                    val inclusionsList = if (service.inclusions.isNotEmpty()) service.inclusions
                    else service.includes.split("·", ",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                    val exclusionsList = if (service.exclusions.isNotEmpty()) service.exclusions
                    else listOf(
                        "Spare parts & materials (billed transparently at actual MRP)",
                        "Major civil, masonry or structural modifications"
                    )

                    ServiceScopeCard(
                        title = "Service scope & inclusions",
                        included = if (inclusionsList.isNotEmpty()) inclusionsList else listOf(service.includes),
                        excluded = exclusionsList,
                        imageUrl = service.resolvedImageUrl,
                        fallbackVector = categoryIcon(service.category)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Action CTA
            if (service.hasMultipleOptions && onOptionsClick != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onOptionsClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("View Options (${service.options.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onBookNow()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Book Now · ₹${service.price}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))
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

private fun dayLabel(offset: Int): String {
    return when (offset) {
        0 -> "Today"
        1 -> "Tomorrow"
        else -> {
            val cal = Calendar.getInstance(IST).apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            SimpleDateFormat("EEE, d MMM", Locale.getDefault()).apply { timeZone = IST }.format(cal.time)
        }
    }
}

/** Hourly slots 8 AM – 8 PM IST on [dayOffset] (0 today, 1 tomorrow, up to 4 days ahead), at least 60 minutes ahead. */
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
    val reviewTriggerService = rememberInAppReviewTriggerService()
    val lang = LocaleHelper.getLanguage(context)
    val scope = rememberCoroutineScope()
    val catalog by viewModel.catalog.collectAsState()
    val catalogError by viewModel.catalogError.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadCatalog() }

    val prefs = remember { context.getSharedPreferences("dutype_saved_addresses", Context.MODE_PRIVATE) }
    val savedWorkLocations by viewModel.savedWorkLocationsStore.locations.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.savedWorkLocationsStore.start()
    }

    val baseAddressTags = listOf("🏠 Home", "🏢 Work", "🏬 Shop", "📍 Other")
    val addressTags = remember(savedWorkLocations) {
        val custom = savedWorkLocations.mapNotNull { loc ->
            val labelLower = loc.label.lowercase(Locale.ROOT)
            val icon = when {
                labelLower.contains("home") -> "🏠"
                labelLower.contains("work") || labelLower.contains("office") -> "🏢"
                labelLower.contains("shop") -> "🏬"
                else -> "📍"
            }
            val formatted = "$icon ${loc.label}"
            if (baseAddressTags.none { it.substringAfter(" ").equals(loc.label, ignoreCase = true) }) formatted else null
        }
        baseAddressTags + custom
    }
    var selectedTag by remember { mutableStateOf<String?>("🏠 Home") }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var newAddressTag by remember { mutableStateOf("🏠 Home") }
    var newAddressText by remember { mutableStateOf("") }
    var newAddressArea by remember { mutableStateOf("") }

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

    fun getSavedAddressForTag(tag: String): Pair<String, String>? {
        val cleanTag = tag.substringAfter(" ").trim().lowercase(Locale.ROOT)
        val prefAddr = prefs.getString("addr_$cleanTag", null)?.trim()
        val prefArea = prefs.getString("area_$cleanTag", null)?.trim().orEmpty()
        if (!prefAddr.isNullOrBlank()) return Pair(prefAddr, prefArea)
        val workLoc = savedWorkLocations.firstOrNull {
            it.label.contains(cleanTag, ignoreCase = true) || it.id.contains(cleanTag, ignoreCase = true)
        }
        if (workLoc != null && workLoc.address.isNotBlank()) {
            return Pair(workLoc.address, workLoc.label)
        }
        return null
    }

    fun saveCurrentAddress(tag: String, addrText: String, areaText: String) {
        val cleanTag = tag.substringAfter(" ").trim().lowercase(Locale.ROOT).replace(" ", "_")
        val tagLabel = tag.substringAfter(" ").trim()
        val curLat = lat ?: 17.2473
        val curLng = lng ?: 80.1514
        prefs.edit()
            .putString("addr_$cleanTag", addrText.trim())
            .putString("area_$cleanTag", areaText.trim().ifBlank { tagLabel })
            .putString("lat_$cleanTag", curLat.toString())
            .putString("lng_$cleanTag", curLng.toString())
            .apply()
        viewModel.savedWorkLocationsStore.add(
            label = tagLabel,
            address = addrText.trim(),
            latitude = curLat,
            longitude = curLng
        )
        Toast.makeText(context, context.getString(R.string.address_saved_session), Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        // Silently inherit current location selected on Services Home Screen
        val initialLoc = viewModel.userLocation.value
        if (initialLoc != null && initialLoc.hasValidCoordinates()) {
            lat = initialLoc.latitude
            lng = initialLoc.longitude
            if (area.isBlank()) area = initialLoc.area.orEmpty()
            if (address.isBlank()) address = initialLoc.address
        }
        if (address.isBlank()) {
            val defaultSaved = getSavedAddressForTag("🏠 Home")
            if (defaultSaved != null && defaultSaved.first.isNotBlank()) {
                address = defaultSaved.first
                if (area.isBlank()) area = defaultSaved.second
            }
        }
    }

    // Offers: the server prices the booking (first-booking offer, coupon), the screen only shows it.
    var couponInput by remember { mutableStateOf("") }
    var appliedCode by remember { mutableStateOf("") }
    var couponErrorText by remember { mutableStateOf<String?>(null) }
    var quote by remember { mutableStateOf<ServiceQuote?>(null) }
    var quoting by remember { mutableStateOf(false) }
    LaunchedEffect(serviceId, appliedCode) {
        quoting = true
        viewModel.quote(serviceId, appliedCode)
            .onSuccess { q ->
                quote = q
                // An invalid code is shown and then dropped, so it is never sent with the booking.
                if (appliedCode.isNotBlank() && q.couponError.isNotBlank()) {
                    couponErrorText = q.couponError
                    Toast.makeText(context, q.couponError, Toast.LENGTH_SHORT).show()
                    appliedCode = ""
                } else if (appliedCode.isNotBlank() && q.couponCode.isNotBlank()) {
                    couponErrorText = null
                    Toast.makeText(context, "Coupon ${q.couponCode} applied!", Toast.LENGTH_SHORT).show()
                }
            }
            .onFailure { err ->
                // Do not show coupon errors or toasts on screen load if user has not entered a coupon!
                if (appliedCode.isNotBlank()) {
                    couponErrorText = err.message ?: "Failed to verify coupon"
                    Toast.makeText(context, couponErrorText, Toast.LENGTH_SHORT).show()
                    appliedCode = ""
                }
            }
        quoting = false
    }

    fun locate(silent: Boolean = false) {
        locating = true
        scope.launch {
            if (!silent) {
                viewModel.resetToGps()
            }
            val place = viewModel.currentPlace()
            locating = false
            if (place != null) {
                lat = place.lat
                lng = place.lng
                if (area.isBlank() || !silent) area = place.area
                if (address.isBlank() || !silent) address = place.address
            } else if (!silent) {
                Toast.makeText(context, context.getString(R.string.svc_location_needed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) locate(silent = false)
    }
    val hasPermission = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val requestLocation = {
        if (hasPermission()) locate(silent = false)
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val c = catalog
    val service = c?.services?.firstOrNull { it.id == serviceId }
    val fee = if (c != null && service != null) c.feeFor(service) else 0
    val q = quote
    val discount = q?.discount ?: 0
    val total = if (service != null) q?.total ?: (service.price + fee) else 0
    val distanceToKhammamKm = remember(lat, lng) {
        if (lat != null && lng != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(lat!!, lng!!, 17.2473, 80.1514, results)
            results[0] / 1000.0
        } else {
            0.0
        }
    }
    val isOutOfArea = remember(lat, lng, distanceToKhammamKm) {
        lat != null && lng != null && distanceToKhammamKm > 15.0
    }
    val canBook = !submitting && !quoting && !isOutOfArea && address.trim().length >= 5 && (!scheduled || slot != null)

    fun doBook() {
        if (!canBook || service == null) return
        submitting = true
        scope.launch {
            val cleanTag = selectedTag?.substringAfter(" ")?.trim()?.lowercase(Locale.ROOT) ?: "home"
            val tagName = selectedTag?.substringAfter(" ")?.trim() ?: "Home"
            prefs.edit()
                .putString("addr_$cleanTag", address.trim())
                .putString("area_$cleanTag", area.trim().ifBlank { tagName })
                .apply()

            val finalLat = lat ?: 17.2473
            val finalLng = lng ?: 80.1514
            runCatching {
                viewModel.savedWorkLocationsStore.add(
                    label = tagName,
                    address = address.trim(),
                    latitude = finalLat,
                    longitude = finalLng
                )
            }
            viewModel.book(
                service, address.trim(), area.trim(), finalLat, finalLng, note.trim(),
                if (scheduled) slot else null, q?.couponCode.orEmpty()
            )
                .onSuccess { id ->
                    context.findActivity()?.let { act ->
                        reviewTriggerService.onServiceBooked(act)
                    }
                    navController.navigate(Routes.servicesBookingRoute(id)) {
                        popUpTo(Routes.SERVICES_BOOK) { inclusive = true }
                    }
                }
                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
            submitting = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White.bg(),
        bottomBar = {
            if (c != null && service != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.bg(),
                    border = BorderStroke(1.dp, SvcLine.bg()),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("₹$total", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SvcInk.fg())
                                if (discount > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("₹${service.price + fee}", color = Color(0xFF94A3B8), fontSize = 13.sp, textDecoration = TextDecoration.LineThrough)
                                }
                            }
                            Text(
                                text = stringResource(R.string.svc_pay_after),
                                color = SvcMuted.fg(),
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                        Button(
                            onClick = { doBook() },
                            enabled = canBook,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F172A),
                                disabledContainerColor = if (isOutOfArea) Color(0xFFFEE2E2) else Color(0xFFE2E8F0),
                                disabledContentColor = if (isOutOfArea) Color(0xFFDC2626) else Color(0xFF94A3B8)
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (isOutOfArea) "Outside Service Area" else "Confirm Booking",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (canBook) Color.White else if (isOutOfArea) Color(0xFFDC2626) else Color(0xFF94A3B8),
                                    letterSpacing = 0.3.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (c == null || service == null) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                when {
                    catalogError != null -> DutyPeErrorState(catalogError, onRetry = { viewModel.loadCatalog(force = true) })
                    c != null -> DutyPeIssueState(LoadIssue.NOT_FOUND, onRetry = null)
                    else -> DutyPeLoadingList(rows = 3)
                }
            }
            return@Scaffold
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Edge-to-edge Cover Image (Fills top section completely)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!service.resolvedImageUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(service.resolvedImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = service.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            error = {
                                Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC)), contentAlignment = Alignment.Center) {
                                    ServiceClayImage(
                                        service = service,
                                        modifier = Modifier.size(130.dp),
                                        iconModifier = Modifier.size(64.dp),
                                        tint = Color(0xFF0F172A)
                                    )
                                }
                            }
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC)), contentAlignment = Alignment.Center) {
                            ServiceClayImage(
                                service = service,
                                modifier = Modifier.size(130.dp),
                                iconModifier = Modifier.size(64.dp),
                                tint = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                // 2. Body Details (Padded 16.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OfflineCachedNote()

                    // Title & Price Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = service.label(lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = SvcInk.fg(),
                            lineHeight = 28.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("4.85 (14.2k ratings)  ·  ", fontSize = 12.5.sp, color = SvcMuted.fg(), fontWeight = FontWeight.Medium)
                            Text("⏱ ${service.durationMin} mins", fontSize = 12.5.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${service.price}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SvcInk.fg())
                            if (service.mrp > service.price) {
                                Spacer(Modifier.width(8.dp))
                                Text("₹${service.mrp}", fontSize = 13.sp, color = Color(0xFF94A3B8), textDecoration = TextDecoration.LineThrough)
                                if (service.discountPercent > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFDCFCE7))
                                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    ) {
                                        Text("${service.discountPercent}% OFF", color = Color(0xFF15803D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Service Scope & What's Included
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val sId = service.id.lowercase()
                        val sName = (service.name + " " + service.label(lang)).lowercase()

                        val isBathroom = sId.contains("bath") || sName.contains("bathroom") || sName.contains("toilet") || sName.contains("washroom")
                        val isKitchen = sId.contains("kitchen") || sName.contains("kitchen")
                        val isMopping = sId.contains("mop") || sName.contains("mop") || sName.contains("sweep") || sName.contains("floor")
                        val isCooking = sId.contains("cook") || sName.contains("cook") || sName.contains("meal")
                        val isGeneralHelper = (sId.contains("helper") || sId.contains("all_round") || sName.contains("helper")) && !isBathroom && !isKitchen && !isMopping && !isCooking

                        Column {
                            Text(
                                text = "What's Included",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = SvcInk.fg()
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Verified specialists with 100% transparent pricing and satisfaction guarantee",
                                fontSize = 12.5.sp,
                                color = SvcMuted.fg(),
                                lineHeight = 16.sp
                            )
                        }

                        when {
                            isBathroom -> {
                                ServiceScopeCard(
                                    title = service.label(lang),
                                    included = if (service.inclusions.isNotEmpty()) service.inclusions else listOf("Toilet seat, tiles scrub & mirror wipe", "Wash basin & tap descaling", "Floor disinfection & stain cleanup"),
                                    excluded = if (service.exclusions.isNotEmpty()) service.exclusions else listOf("Acid wash & severe scale", "Ceiling / exhaust motor repair"),
                                    imageRes = R.drawable.svc_scope_bathroom
                                )
                            }
                            isKitchen -> {
                                ServiceScopeCard(
                                    title = service.label(lang),
                                    included = if (service.inclusions.isNotEmpty()) service.inclusions else listOf("Crockery, sink & countertop cleaning", "Wiping appliance & cabinet exteriors", "Floor degreasing & trash clearing"),
                                    excluded = if (service.exclusions.isNotEmpty()) service.exclusions else listOf("Deep chimney internal motor degrease", "Hard burnt-on stove encrustations"),
                                    imageRes = R.drawable.svc_scope_kitchen
                                )
                            }
                            isMopping -> {
                                ServiceScopeCard(
                                    title = service.label(lang),
                                    included = if (service.inclusions.isNotEmpty()) service.inclusions else listOf("Floor sweeping & wet mopping", "Under-furniture surface dusting", "Disinfectant floor wipe"),
                                    excluded = if (service.exclusions.isNotEmpty()) service.exclusions else listOf("Wall washing & ceiling dusting", "Severe construction debris cleanup"),
                                    imageRes = R.drawable.svc_scope_mopping
                                )
                            }
                            isCooking -> {
                                ServiceScopeCard(
                                    title = service.label(lang),
                                    included = if (service.inclusions.isNotEmpty()) service.inclusions else listOf("Vegetable chopping & salad preparation", "Meat/paneer marination", "Serving food & dining table setup"),
                                    excluded = if (service.exclusions.isNotEmpty()) service.exclusions else listOf("Full multi-course gourmet chef cooking", "Post-dinner heavy vessel washing"),
                                    imageRes = R.drawable.svc_scope_mealprep
                                )
                            }
                            isGeneralHelper -> {
                                ServiceScopeCard(
                                    title = "Mopping, dusting & wiping",
                                    included = listOf("Dusting & Mopping floor", "Wet wiping furniture"),
                                    excluded = listOf("Wiping walls", "Hard to reach areas"),
                                    imageRes = R.drawable.svc_scope_mopping
                                )
                                ServiceScopeCard(
                                    title = "Kitchen & meal assistance",
                                    included = listOf("Veggies chopping & prep", "Crockery & sink washing"),
                                    excluded = listOf("Cooking full meals"),
                                    imageRes = R.drawable.svc_scope_kitchen
                                )
                            }
                            else -> {
                                val inclusionsList = if (service.inclusions.isNotEmpty()) service.inclusions
                                else if (service.includes.isNotBlank()) service.includes.split("·", "\n").map { it.trim() }.filter { it.isNotBlank() }
                                else listOf(
                                    "Pre-service safety check & thorough problem diagnosis",
                                    "Standard service execution by trained professional",
                                    "Post-service functionality verification & cleanup",
                                    "30-Day DutyPe service guarantee"
                                )
                                val exclusionsList = if (service.exclusions.isNotEmpty()) service.exclusions
                                else listOf(
                                    "Spare parts & replacement materials (billed at actual MRP)",
                                    "Major civil, masonry or structural modifications"
                                )

                                ServiceScopeCard(
                                    title = service.label(lang),
                                    included = inclusionsList,
                                    excluded = exclusionsList,
                                    imageUrl = service.resolvedImageUrl,
                                    fallbackVector = categoryIcon(service.category)
                                )
                            }
                        }

                        if (service.inspection) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.svc_inspection_info),
                                    color = SvcOrange,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        if (service.provide.isNotEmpty()) {
                            ItemsBox(stringResource(R.string.svc_customer_keeps_you), c.itemLabels(service.provide, lang), SvcGreen)
                        }
                        if (service.bring.isNotEmpty()) {
                            ItemsBox(stringResource(R.string.svc_partner_brings), c.itemLabels(service.bring, lang), SvcBlue)
                        }
                    }

            // When section (Side-by-side Pronto/UC style option cards)
            SvcSection {
                Text(stringResource(R.string.svc_when), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SvcInk.fg())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val instantSelected = !scheduled
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (instantSelected) Color(0xFFF8FAFC).bg() else Color.White.bg())
                            .border(
                                width = if (instantSelected) 1.5.dp else 1.dp,
                                color = if (instantSelected) Color(0xFF0F172A).bg() else SvcLine.bg(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { scheduled = false }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = SvcOrange, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Now", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SvcInk.fg())
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Partner in ~60 min", fontSize = 11.5.sp, color = SvcMuted.fg())
                    }
                    val schedSelected = scheduled
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (schedSelected) Color(0xFFF8FAFC).bg() else Color.White.bg())
                            .border(
                                width = if (schedSelected) 1.5.dp else 1.dp,
                                color = if (schedSelected) Color(0xFF0F172A).bg() else SvcLine.bg(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { scheduled = true }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = SvcBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Schedule", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SvcInk.fg())
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Pick date & slot", fontSize = 11.5.sp, color = SvcMuted.fg())
                    }
                }
                if (scheduled) {
                    Spacer(Modifier.height(8.dp))
                    Text("Select Date", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = SvcInk.fg())
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items((0..4).toList()) { offset ->
                            FilterChip(
                                selected = dayOffset == offset,
                                onClick = { dayOffset = offset; slot = null },
                                label = { Text(dayLabel(offset), fontWeight = if (dayOffset == offset) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                    val options = slots(dayOffset)
                    Spacer(Modifier.height(6.dp))
                    Text("Select Time Slot", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = SvcInk.fg())
                    if (options.isEmpty()) {
                        Text("No more slots available for this date. Please pick another day.", fontSize = 12.sp, color = SvcMuted.fg())
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(options) { t ->
                                FilterChip(
                                    selected = slot == t,
                                    onClick = { slot = t },
                                    label = { Text(SimpleDateFormat("h:mm a", Locale.getDefault()).apply { timeZone = IST }.format(Date(t))) }
                                )
                            }
                        }
                    }
                }
            }

            // Address section with detect location button on top right
            SvcSection {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.svc_address), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SvcInk.fg())
                    OutlinedButton(
                        onClick = { requestLocation() },
                        enabled = !locating,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, if (lat != null) SvcGreen else SvcLine.bg())
                    ) {
                        if (locating) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 1.5.dp)
                        } else {
                            Icon(
                                if (lat != null) Icons.Filled.CheckCircle else Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = if (lat != null) SvcGreen else SvcInk.fg(),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(if (lat != null) R.string.svc_location_set else R.string.svc_use_location),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (lat != null) SvcGreen else SvcInk.fg()
                            )
                        }
                    }
                }

                // Quick Saved Address Tags Row (Home, Work, Shop, Other + Custom Saved Locations)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(addressTags) { tag ->
                        val isSelected = selectedTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTag = tag
                                val saved = getSavedAddressForTag(tag)
                                if (saved != null) {
                                    address = saved.first
                                    if (saved.second.isNotBlank()) area = saved.second
                                }
                            },
                            label = { Text(tag, fontSize = 11.5.sp) }
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = { showAddAddressDialog = true },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("New Address", fontSize = 11.sp)
                        }
                    }
                }

                if (isOutOfArea) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📍", fontSize = 16.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Coming Soon to Your Area",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "DutyPe Home Services is currently operational only within Khammam City (15 km radius). Your selected location is ${String.format("%.1f", distanceToKhammamKm)} km away.",
                                fontSize = 12.sp,
                                color = Color(0xFF7F1D1D),
                                lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { requestLocation() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Detect Phone GPS",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        lat = 17.2473
                                        lng = 80.1514
                                        area = "Wyra Road"
                                        address = "Wyra Road, Khammam, Telangana 507001"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "📍 Test Wyra Road",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFDC2626),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it.take(300) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.svc_address_hint)) },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (address.trim().length >= 5) {
                        TextButton(
                            onClick = {
                                selectedTag?.let { saveCurrentAddress(it, address, area) }
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = SvcGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("Save as ${selectedTag ?: "Home"}", fontSize = 11.5.sp, color = SvcGreen, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    TextButton(
                        onClick = { showAddAddressDialog = true },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("+ Add another address", fontSize = 11.5.sp, color = SvcInk.fg(), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Instructions for partner section
            SvcSection {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_profile_message),
                        contentDescription = null,
                        tint = SvcInk.fg(),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.svc_partner_instructions_title), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SvcInk.fg())
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(300) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.svc_note_hint)) },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
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
                        TextButton(onClick = { appliedCode = ""; couponInput = ""; couponErrorText = null }) {
                            Text(stringResource(R.string.svc_remove))
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = couponInput,
                            onValueChange = {
                                couponInput = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(20)
                                couponErrorText = null
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(stringResource(R.string.svc_coupon_hint)) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                couponErrorText = null
                                appliedCode = couponInput.trim()
                            },
                            enabled = couponInput.trim().length >= 3 && !quoting,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (quoting) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Text(stringResource(R.string.svc_apply))
                            }
                        }
                    }
                    if (!couponErrorText.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(couponErrorText!!, color = SvcRed, fontSize = 12.sp)
                    }
                }
                if (q != null && q.couponNote.isNotBlank()) Text(q.couponNote, color = SvcOrange, fontSize = 12.sp)
                val offers = q?.offers.orEmpty().filter { it.code != q?.couponCode }
                if (offers.isNotEmpty()) {
                    OfferTickets(offers, onApply = {
                        couponInput = it.code
                        couponErrorText = null
                        appliedCode = it.code
                    })
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

            if (lat == null) {
                Text(stringResource(R.string.svc_location_needed), color = SvcRed, fontSize = 12.sp)
            }
            Spacer(Modifier.height(30.dp))
        } // inner Column
    } // outer scrollable Column

    // Floating Header Overlays (Back & Share buttons over cover image)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { navController.popBackStack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out ${service.label(lang)} on DutyPe: ₹${service.price} only!\nhttps://dutype.in/services")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share service"))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = "Share",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
} // outer Box
} // Scaffold

    if (showAddAddressDialog) {
        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            title = { Text(stringResource(R.string.add_address), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("🏠 Home", "🏢 Work", "🏬 Shop", "📍 Other").forEach { tag ->
                            FilterChip(
                                selected = newAddressTag == tag,
                                onClick = { newAddressTag = tag },
                                label = { Text(tag, fontSize = 11.5.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newAddressArea,
                        onValueChange = { newAddressArea = it },
                        label = { Text("Locality / Landmark") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAddressText,
                        onValueChange = { newAddressText = it },
                        label = { Text("Complete Address") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAddressText.trim().length >= 5) {
                            selectedTag = newAddressTag
                            address = newAddressText.trim()
                            if (newAddressArea.isNotBlank()) area = newAddressArea.trim()
                            saveCurrentAddress(newAddressTag, newAddressText.trim(), newAddressArea.trim())
                            showAddAddressDialog = false
                            newAddressText = ""
                            newAddressArea = ""
                        }
                    },
                    enabled = newAddressText.trim().length >= 5
                ) {
                    Text("Save & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAddressDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
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
                if (b.status == BookingStatus.CANCELLED && b.cancellationFee > 0) {
                    Pill("₹${b.cancellationFee} Transit Fare", Color(0xFFEA580C))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (b.status == BookingStatus.CANCELLED && b.cancellationFee > 0) {
                Text("₹${b.cancellationFee}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFEA580C))
                Text("Transit Fare", fontSize = 10.sp, color = SvcMuted.fg())
            } else {
                Text("₹${b.total}", fontWeight = FontWeight.ExtraBold, color = SvcInk.fg())
            }
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
    val reviewTriggerService = rememberInAppReviewTriggerService()
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
            // Live Booking Progress Tracker Hero
            val trackerStatusText = when (b.status) {
                BookingStatus.SEARCHING -> "Searching for Technician..."
                BookingStatus.ASSIGNED -> "Technician Assigned"
                BookingStatus.ON_THE_WAY -> "Technician On The Way"
                BookingStatus.STARTED -> "Service In Progress"
                BookingStatus.COMPLETED -> "Service Completed"
                BookingStatus.CANCELLED -> "Booking Cancelled"
                BookingStatus.NO_PARTNER -> "No Partner Available"
                else -> "Booking Placed"
            }
            val trackerSubtext = when (b.status) {
                BookingStatus.SEARCHING -> "Matching you with the nearest background-verified professional in Khammam..."
                BookingStatus.ASSIGNED -> "Your partner has accepted and is preparing their tools."
                BookingStatus.ON_THE_WAY -> "Partner is en route to your doorstep. Keep your start OTP ready."
                BookingStatus.STARTED -> "Work is underway. OTP successfully verified."
                BookingStatus.COMPLETED -> "Service successfully completed. Transparent pricing guaranteed."
                BookingStatus.CANCELLED -> "This request was cancelled."
                BookingStatus.NO_PARTNER -> "All partners are currently busy. We apologize for the inconvenience."
                else -> "Your booking details have been confirmed."
            }

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

                // Live Tracker Status Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor(b.status).copy(alpha = 0.08f))
                        .border(1.dp, statusColor(b.status).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor(b.status)))
                        Spacer(Modifier.width(8.dp))
                        Text(trackerStatusText, color = statusColor(b.status), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(trackerSubtext, fontSize = 12.5.sp, color = SvcInk.fg(), lineHeight = 17.sp)
                    if (b.status == BookingStatus.SEARCHING) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color = statusColor(b.status))
                    }
                }

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

            if (b.status == BookingStatus.CANCELLED && b.cancellationFee > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = SvcRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Doorstep Travel Allowance Paid: ₹${b.cancellationFee}", fontWeight = FontWeight.Bold, color = SvcRed, fontSize = 14.sp)
                        }
                        Text(
                            b.cancellationNotice.ifBlank {
                                "₹${b.cancellationFee} was deducted and transferred to the technician as travel allowance for transit expenses since the partner was already on the way."
                            },
                            fontSize = 12.5.sp,
                            color = Color(0xFF7F1D1D),
                            lineHeight = 17.sp
                        )
                        if (b.cancellationReason.isNotBlank()) {
                            Text("Cancellation Reason: ${b.cancellationReason}", fontSize = 12.sp, color = SvcMuted.fg())
                        }
                    }
                }
            }

            SvcSection {
                Text(stringResource(R.string.svc_bill), fontWeight = FontWeight.Bold, color = SvcInk.fg())
                PriceLine(stringResource(R.string.svc_price_service), b.price)
                PriceLine(stringResource(R.string.svc_price_fee), b.bookingFee)
                if (b.discount > 0) DiscountLine(b.discountLabel, b.discount)
                if (b.extras > 0) PriceLine(stringResource(R.string.svc_extras, b.extras, b.extrasNote), b.extras)
                if (b.status == BookingStatus.CANCELLED && b.cancellationFee > 0) {
                    PriceLine("Doorstep Travel Allowance", b.cancellationFee)
                }
                HorizontalDivider(color = SvcLine.bg())
                if (b.status == BookingStatus.CANCELLED && b.cancellationFee > 0) {
                    PriceLine("Total Deducted", b.cancellationFee, bold = true)
                } else {
                    PriceLine(stringResource(R.string.svc_price_total), b.total, bold = true)
                }
                if (b.discount > 0 && b.status != BookingStatus.CANCELLED) Pill(stringResource(R.string.svc_you_save, b.discount), SvcGreen, icon = Icons.Filled.LocalOffer)
                Text(
                    when {
                        b.status == BookingStatus.COMPLETED -> stringResource(R.string.svc_amount_to_pay, b.total)
                        b.status == BookingStatus.CANCELLED && b.cancellationFee > 0 -> "Doorstep travel fare credited directly to the service partner."
                        b.status == BookingStatus.CANCELLED -> "No charges incurred for this cancellation."
                        else -> stringResource(R.string.svc_pay_after)
                    },
                    color = when {
                        b.status == BookingStatus.COMPLETED -> SvcGreen
                        b.status == BookingStatus.CANCELLED && b.cancellationFee > 0 -> SvcRed
                        else -> SvcMuted.fg()
                    },
                    fontWeight = if (b.status == BookingStatus.COMPLETED || (b.status == BookingStatus.CANCELLED && b.cancellationFee > 0)) FontWeight.Bold else FontWeight.Normal,
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
                                        .onSuccess {
                                            context.findActivity()?.let { act ->
                                                reviewTriggerService.onServiceCompleted(act, stars)
                                            }
                                            Toast.makeText(context, context.getString(R.string.svc_thanks_rating), Toast.LENGTH_SHORT).show()
                                        }
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
        val b = load.booking
        if (b != null && b.status == BookingStatus.ON_THE_WAY) {
            CancellationCompensationBottomSheet(
                booking = b,
                busy = busy,
                onDismiss = { confirmCancel = false },
                onConfirmCancel = { reason ->
                    busy = true
                    scope.launch {
                        viewModel.cancel(bookingId, reason)
                            .onSuccess {
                                confirmCancel = false
                                Toast.makeText(context, "Booking cancelled. Travel allowance processed.", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                        busy = false
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { confirmCancel = false },
                title = { Text(stringResource(R.string.svc_cancel_confirm)) },
                text = { Text("No cancellation fee will be charged as the technician has not started transit yet.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmCancel = false
                        busy = true
                        scope.launch {
                            viewModel.cancel(bookingId, "Customer cancelled before transit")
                                .onSuccess { Toast.makeText(context, "Booking cancelled successfully.", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                            busy = false
                        }
                    }) { Text(stringResource(R.string.svc_yes), color = SvcRed) }
                },
                dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.svc_no)) } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancellationCompensationBottomSheet(
    booking: ServiceBooking,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirmCancel: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cancellationReasons = listOf(
        "Technician is taking longer than expected",
        "Booked by mistake / wrong address",
        "No longer need the service today",
        "Emergency / had to step out",
        "Other reason"
    )
    var selectedReason by remember { mutableStateOf(cancellationReasons[0]) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White.bg(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF2F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = SvcRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Partner En Route to Doorstep",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SvcInk.fg()
                    )
                    Text(
                        text = if (booking.partnerName.isNotBlank()) "${booking.partnerName} is currently traveling" else "Technician is on the way to your address",
                        fontSize = 12.sp,
                        color = SvcMuted.fg()
                    )
                }
                IconButton(onClick = onDismiss, enabled = !busy) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = SvcMuted.fg())
                }
            }

            // Doorstep Transit Fare Alert Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Doorstep Travel Fare",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF9A3412)
                        )
                        Text(
                            text = "₹10 – ₹30",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFFC2410C)
                        )
                    }
                    Text(
                        text = "To protect service professionals who spend fuel traveling to your doorstep, cancelling while the technician is on the way incurs a distance-based travel fare (min ₹10 to max ₹30). This amount is credited directly to the worker.",
                        fontSize = 12.sp,
                        color = Color(0xFF7C2D12),
                        lineHeight = 16.5.sp
                    )
                }
            }

            // Cancellation reason selection
            Text(
                text = "Reason for cancellation",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = SvcInk.fg()
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cancellationReasons.forEach { reason ->
                    val isSelected = selectedReason == reason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFFF1F5F9) else Color.Transparent)
                            .clickable(enabled = !busy) { selectedReason = reason }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedReason = reason },
                            enabled = !busy
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = reason,
                            fontSize = 13.sp,
                            color = if (isSelected) SvcInk.fg() else SvcMuted.fg(),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Keep Booking CTA (primary recommendation)
            Button(
                onClick = onDismiss,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    text = "Keep Booking (Wait for Partner)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            // Destructive Cancel CTA
            OutlinedButton(
                onClick = { onConfirmCancel(selectedReason) },
                enabled = !busy,
                border = BorderStroke(1.dp, SvcRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = SvcRed)
                } else {
                    Text(
                        text = "Cancel & Pay Travel Fare",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = SvcRed
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
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
    onHomeServices: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SvcInk.fg())
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeedTile(
                icon = Icons.Filled.Work,
                title = stringResource(R.string.svc_need_hire),
                sub = stringResource(R.string.svc_need_hire_sub),
                color = SvcBlue,
                onClick = onPostJob
            )
            NeedTile(
                icon = Icons.Filled.Bolt,
                title = stringResource(R.string.svc_need_urgent),
                sub = stringResource(R.string.svc_need_urgent_sub),
                color = SvcOrange,
                onClick = onPostUrgent
            )
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
            .border(1.dp, SvcLine.bg(), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SvcInk.fg(), lineHeight = 16.sp, minLines = 2)
        Spacer(Modifier.height(2.dp))
        Text(sub, fontSize = 11.sp, color = SvcMuted.fg(), lineHeight = 14.sp, maxLines = 2)
    }
}

/**
 * Clean, lightweight teaser card for the Employer Home Screen.
 * Showcases that home services are available in the user's city/area without loading
 * the full 50-service catalog.
 * Tapping navigates to the dedicated Services bottom navigation bar tab.
 */
@Composable
fun ServicesAreaTeaserCard(
    onOpenServices: () -> Unit,
    onOpenBooking: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeServicesViewModel = hiltViewModel()
) {
    val bookings by viewModel.myBookings.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadCatalog()
    }
    val active = bookings.firstOrNull { it.isOpen }
    val cityName = catalog?.city?.ifBlank { "Khammam" } ?: "Khammam"

    val displayCategories = listOf(
        Triple("Cleaning", Icons.Filled.CleaningServices, "From ₹199"),
        Triple("Electrician", Icons.Filled.ElectricalServices, "From ₹149"),
        Triple("AC Service", Icons.Filled.AcUnit, "From ₹399"),
        Triple("Plumbing", Icons.Filled.Plumbing, "From ₹149")
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (active != null) {
            ActiveBookingStrip(active, onClick = { onOpenBooking(active.id) })
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenServices),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header row: Brand pill + "View All" CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "DUTYPE SERVICES",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFF1F5F9))
                            .clickable(onClick = onOpenServices)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "View All",
                            color = Color(0xFF0F172A),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Doorstep Services in $cityName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Background-verified technicians arrive in ~60 mins. Transparent pricing with post-service payment.",
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 17.5.sp
                )

                Spacer(Modifier.height(16.dp))

                // Service category cards with icons & starting prices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayCategories.forEach { (label, icon, price) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC).bg())
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenServices)
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = price,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(Modifier.height(10.dp))

                // Trust Guarantees Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Verified Pros", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("60-Min Arrival", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pay After", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                    }
                }
            }
        }
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
    // Diverse popular picks across categories (up to 16+ top services)
    val popular = remember(c) {
        val list = mutableListOf<ServiceItem>()
        c.categories.forEach { cat ->
            val catServices = c.services.filter { it.category == cat.id }
            val preferred = catServices.filter { !it.inspection }.take(2)
            if (preferred.isNotEmpty()) {
                list.addAll(preferred)
            } else {
                list.addAll(catServices.take(2))
            }
        }
        if (list.size < 12) {
            val remaining = c.services.filter { it !in list }
            list.addAll(remaining.take(16 - list.size))
        }
        list
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.svc_showcase_title, c.city)) {
            TextButton(onClick = onOpenServices) {
                Text(stringResource(R.string.svc_see_all), fontWeight = FontWeight.Bold)
            }
        }
        if (active != null) ActiveBookingStrip(active, onClick = { onOpenBooking(active.id) })
        RateLastServiceCard(bookings, onOpen = onOpenBooking)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .clickable(onClick = onOpenServices)
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.svc_hero_title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.svc_hero_sub), color = Color(0xFF94A3B8), fontSize = 12.sp)
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
                Column(
                    Modifier
                        .width(155.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.bg())
                        .border(1.dp, SvcLine.bg(), RoundedCornerShape(16.dp))
                        .clickable { onBookService(s.id) }
                        .padding(12.dp)
                ) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                        ServiceClayImage(service = s, modifier = Modifier.size(38.dp), iconModifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(s.label(lang), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SvcInk.fg(), maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.svc_price, s.price), fontWeight = FontWeight.Bold, color = SvcInk.fg(), fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.svc_minutes, s.durationMin), fontSize = 10.5.sp, color = SvcMuted.fg())
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                            .clickable { onBookService(s.id) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.svc_book).uppercase(), color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

internal val NumberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
