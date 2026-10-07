package com.example.dutype.worker.components

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import timber.log.Timber
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.components.OptimizedJobImage
import com.example.dutype.models.JobListing
import com.example.dutype.employer.models.EmploymentType
import com.example.dutype.models.JobListingSummary
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.ValidationUtils

/**
 * JobCard that accepts JobListing directly - PREFERRED
 * Uses only card fields: title, companyName, payText, area, urgency, status, distance, isSaved
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCard(
    job: JobListing,
    isSaved: Boolean = false,
    onSaveClick: (String) -> Unit,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onViewTrack: (String) -> Unit = {},
    applyButtonLabel: String? = null
) {
    val context = LocalContext.current
    var localIsSaved by remember { mutableStateOf(isSaved) }
    LaunchedEffect(isSaved) { localIsSaved = isSaved }

    val effectiveApplyLabel = applyButtonLabel ?: stringResource(R.string.card_apply_now)
    val payDisplay = payAmountLabel(job.payAmount, job.payType)
    val payUnit = payUnitLabel(job.payAmount, job.payType)
    val locationDisplay = remember(job.area, job.distance, job.feedSection) {
        formatLocationWithDistance(context, job.area, job.distance, job.distanceApprox, job.district, job.feedSection)
    }
    val normalizedStatus = job.status.trim().lowercase()
    val isFilled = normalizedStatus == "filled"
    val isExpired = normalizedStatus == "expired"
    val statusLabel = when {
        isFilled -> stringResource(R.string.filled)
        isExpired -> stringResource(R.string.tab_expired)
        else -> null
    }

    JobCardInternal(
        jobId = job.id,
        title = job.title,
        companyName = job.companyName,
        payDisplay = payDisplay,
        payUnit = payUnit,
        shift = job.shift,
        postedAt = job.createdAt,
        applicants = job.applicationCount,
        locationDisplay = locationDisplay,
        vacancies = job.vacancies,
        workTypeLabel = stringResource(EmploymentType.fromKey(job.employmentType).titleRes),
        isClosed = isFilled || isExpired,
        statusLabel = statusLabel,
        isFilled = isFilled,
        isExpired = isExpired,
        isUrgent = job.isUrgent,
        applyButtonLabel = effectiveApplyLabel,
        isSaved = localIsSaved,
        jobImageUrl = job.photoUrl,
        onSaveClick = {
            localIsSaved = !localIsSaved
            onSaveClick(job.id)
        },
        onCardClick = {
            onViewTrack(job.id)
            onCardClick(job.id)
        },
        modifier = modifier
    )
}

/**
 * PERFORMANCE OPTIMIZED: JobCard that accepts JobListingSummary.....
 * Uses only card fields: title, companyName, payText, area, urgency, status, distance, isSaved
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCard(
    job: JobListingSummary,
    isSaved: Boolean = false,
    onSaveClick: (String) -> Unit,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onViewTrack: (String) -> Unit = {},
    applyButtonLabel: String? = null
) {
    val context = LocalContext.current
    var localIsSaved by remember { mutableStateOf(isSaved) }
    LaunchedEffect(isSaved) { localIsSaved = isSaved }

    val effectiveApplyLabel = applyButtonLabel ?: stringResource(R.string.card_apply_now)
    val payDisplay = payAmountLabel(job.payAmount, job.payType)
    val payUnit = payUnitLabel(job.payAmount, job.payType)
    val locationDisplay = remember(job.area, job.distance, job.feedSection) {
        formatLocationWithDistance(context, job.area, job.distance, job.distanceApprox, job.district, job.feedSection)
    }
    val normalizedStatus = job.status.trim().lowercase()
    val isFilled = normalizedStatus == "filled"
    val isExpired = normalizedStatus == "expired"
    val statusLabel = when {
        isFilled -> stringResource(R.string.filled)
        isExpired -> stringResource(R.string.tab_expired)
        else -> null
    }

    JobCardInternal(
        jobId = job.id,
        title = job.title,
        companyName = job.companyName,
        payDisplay = payDisplay,
        payUnit = payUnit,
        shift = job.shift,
        postedAt = job.createdAt,
        applicants = job.applicationCount,
        locationDisplay = locationDisplay,
        vacancies = job.vacancies,
        workTypeLabel = stringResource(EmploymentType.fromKey(job.employmentType).titleRes),
        isClosed = isFilled || isExpired,
        statusLabel = statusLabel,
        isFilled = isFilled,
        isExpired = isExpired,
        isUrgent = job.isUrgent,
        applyButtonLabel = effectiveApplyLabel,
        isSaved = localIsSaved,
        jobImageUrl = job.photoUrl,
        onSaveClick = {
            localIsSaved = !localIsSaved
            onSaveClick(job.id)
        },
        onCardClick = {
            onViewTrack(job.id)
            onCardClick(job.id)
        },
        modifier = modifier
    )
}

/**
 * Internal JobCard implementation — uses only schema fields.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun JobCardInternal(
    jobId: String,
    title: String,
    companyName: String,
    payDisplay: String,
    payUnit: String?,
    shift: String,
    postedAt: Long,
    applicants: Int,
    locationDisplay: String,
    vacancies: Int,
    workTypeLabel: String?,
    isClosed: Boolean,
    statusLabel: String?,
    isFilled: Boolean,
    isExpired: Boolean,
    isSaved: Boolean,
    isUrgent: Boolean = false,
    applyButtonLabel: String = "Apply now",
    jobImageUrl: String? = null,
    onSaveClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stitch design spec: flat white card, 16dp corners, hairline border,
    // no shadow, no logo/heart — title + URGENT badge, employer (+ Verified),
    // distance, then salary + a black pill Quick-Apply/Apply button.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WorkerColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Title + URGENT badge (or closed/expired status badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = ValidationUtils.capitalizeWords(title),
                    style = AppTypography.cardTitle.copy(
                        color = if (isClosed) WorkerColors.TextSecondary else WorkerColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                when {
                    isUrgent && !isClosed -> UrgentBadge()
                    !statusLabel.isNullOrBlank() -> CompactChip(
                        text = statusLabel,
                        chipType = if (isExpired) ChipType.EXPIRED else ChipType.FILLED
                    )
                }
            }

            if (companyName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = companyName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WorkerColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                }
            }

            // Distance / location row
            if (locationDisplay.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = WorkerColors.IconSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = locationDisplay,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WorkerColors.TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            JobCardFacts(
                workTypeLabel = workTypeLabel,
                shift = shift,
                vacancies = vacancies,
                postedAt = postedAt,
                applicants = applicants,
                isClosed = isClosed
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: bold green salary + black pill apply button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = payDisplay,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = WorkerColors.Success,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (payUnit == null) 16.sp else 17.sp
                        )
                    )
                    if (payUnit != null) {
                        Text(
                            text = " $payUnit",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = WorkerColors.TextSecondary,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                if (!isClosed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(WorkerColors.Primary)
                            .clickable { onCardClick() }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "$applyButtonLabel →",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Small red "URGENT" pill badge — top-right of an urgent job card.
 */
@Composable
private fun UrgentBadge() {
    Box(
        modifier = Modifier
            .background(WorkerColors.ErrorLight, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.urgent_caps),
            style = MaterialTheme.typography.labelSmall.copy(
                color = WorkerColors.Error,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}

/**
 * Compact chip with colored backgrounds based on chip type
 * - Vacancy chips: Light black/gray background with dark gray text
 * - Job type/Category chips: Amber/Orange with light amber background
 */
@Composable
private fun CompactChip(
    text: String,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = WorkerColors.Border,
    textColor: Color = WorkerColors.TextSecondary,
    chipType: ChipType = ChipType.DEFAULT
) {
    // Determine colors based on chip type - matching the provided style
    val (bgColor, txtColor, bdrColor) = when {
        backgroundColor != Color.Transparent -> Triple(backgroundColor, textColor, borderColor)
        else -> when (chipType) {
            ChipType.VACANCY -> Triple(
                WorkerColors.ChipBackground,
                WorkerColors.ChipText,
                WorkerColors.Border
            )
            ChipType.JOB_TYPE -> Triple(
                WorkerColors.Warning.copy(alpha = 0.1f), // Light amber background
                WorkerColors.Warning, // Dark amber text
                WorkerColors.Warning  // Amber border
            )
            ChipType.CATEGORY -> Triple(
                WorkerColors.Warning.copy(alpha = 0.1f), // Light amber background
                WorkerColors.Warning, // Dark amber text
                WorkerColors.Warning  // Amber border
            )
            ChipType.URGENT -> Triple(
                WorkerColors.Warning.copy(alpha = 0.1f), // Light amber background
                WorkerColors.Warning, // Dark amber text
                WorkerColors.Warning  // Amber border
            )
            ChipType.FILLED -> Triple(
                WorkerColors.SuccessLight,
                WorkerColors.Success,
                WorkerColors.Success
            )
            ChipType.EXPIRED -> Triple(
                WorkerColors.ErrorLight,
                WorkerColors.Error,
                WorkerColors.Error
            )
            ChipType.DEFAULT -> Triple(
                WorkerColors.Warning.copy(alpha = 0.1f), // Light amber background
                WorkerColors.Warning, // Dark amber text
                WorkerColors.Warning  // Amber border
            )
        }
    }
    
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(0.5.dp, bdrColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = txtColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

/**
 * Chip type enum for colored chips
 */
private enum class ChipType {
    VACANCY,    // Blue - for vacancy count
    JOB_TYPE,   // Green - for Full-time, Part-time
    CATEGORY,   // Purple - for job category like Driver, Cook
    URGENT,     // Amber - for urgent hiring
    FILLED,
    EXPIRED,
    DEFAULT     // Gray - default style
}

// =============================================================================
// HELPER FUNCTIONS — schema-only, no legacy fields
// =============================================================================


/**
 * Format location with distance.
 * addressText comes from job_details (runtime only), distance is computed client-side.
 */
/** "Kothagudem, Bhadradri Kothagudem" for jobs shown in the district / state sections (outside 20 km). */
private fun farPlace(area: String, district: String, section: com.example.dutype.models.FeedSection?): String {
    if (section == null || section.isNearby || section == com.example.dutype.models.FeedSection.ANYWHERE) return ""
    val town = area.split(",").firstOrNull()?.trim().orEmpty()
    return listOf(town, district).filter { it.isNotBlank() }.distinctBy { it.lowercase() }.joinToString(", ")
}

private fun formatLocationWithDistance(
    context: android.content.Context,
    addressText: String,
    distance: Double?,
    approximate: Boolean = false,
    district: String = "",
    section: com.example.dutype.models.FeedSection? = null
): String {
    val far = farPlace(addressText, district, section)
    if (far.isNotEmpty()) {
        return listOf(formatShortDistance(distance, approximate), far).filter { it.isNotBlank() }.joinToString(" - ")
    }
    val parts = addressText
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
    val shortLocation = when {
        parts.isEmpty() -> ""
        parts.size >= 2 && parts.last().equals("India", ignoreCase = true) -> parts.first()
        parts.size >= 3 -> parts[parts.size - 3]
        else -> parts.first()
    }

    if (distance == null) return shortLocation
    val distStr = when {
        approximate -> context.getString(R.string.distance_away, "~${formatShortDistance(distance, false).removePrefix("~")}")
        distance < 1.0 -> context.getString(R.string.distance_away, "${(distance * 1000).toInt()}m")
        distance < 2.0 -> context.getString(R.string.distance_walkable, "${"%.1f".format(distance)} km")
        else -> context.getString(R.string.distance_away, "${"%.1f".format(distance)} km")
    }
    return if (shortLocation.isNotEmpty()) "$distStr - $shortLocation" else distStr
}


/**
 * Get emoji for job category - lightweight alternative to Lottie animations.
 *
 * Ordering matters: more specific matches must come before generic ones
 * (e.g. "sales executive" before "executive", "care taker" before "care").
 * Covers the full Android JobCategory list plus the long-tail of common
 * full-time / part-time roles (teacher, nurse, technician, sales exec, etc.).
 */
private fun getJobEmoji(jobTitle: String): String {
    val t = jobTitle.lowercase()
    return when {
        // Food & hospitality
        t.contains("chef") || t.contains("cook") || t.contains("kitchen") || t.contains("tandoor") || t.contains("biryani") -> "👨‍🍳"
        t.contains("baker") || t.contains("bakery") || t.contains("pastry") -> "🧁"
        t.contains("barista") || t.contains("coffee") || t.contains("cafe") -> "☕"
        t.contains("bartender") || t.contains("bar tender") -> "🍸"
        t.contains("waiter") || t.contains("waitress") || t.contains("server") || t.contains("steward") || t.contains("restaurant") -> "🍽️"
        t.contains("catering") || t.contains("event") -> "🎉"

        // Driving & delivery
        t.contains("delivery") || t.contains("courier") || t.contains("rider") || t.contains("swiggy") || t.contains("zomato") || t.contains("dunzo") || t.contains("parcel") -> "📦"
        t.contains("truck") || t.contains("lorry") -> "🚛"
        t.contains("auto driver") || t.contains("auto-rickshaw") || t.contains("rickshaw") -> "🛵"
        t.contains("bike") || t.contains("two wheeler") || t.contains("two-wheeler") || t.contains("scooter") -> "🏍️"
        t.contains("driver") || t.contains("chauffeur") || t.contains("uber") || t.contains("ola") || t.contains("cab") || t.contains("taxi") || t.contains("driving") -> "🚗"

        // Home help / personal services
        t.contains("nanny") || t.contains("babysit") || t.contains("ayah") -> "👶"
        t.contains("care taker") || t.contains("caretaker") || t.contains("caregiver") || t.contains("elder care") || t.contains("old age") -> "🧑‍🦽"
        t.contains("maid") || t.contains("house help") || t.contains("housekeep") || t.contains("cleaner") || t.contains("cleaning") || t.contains("janitor") || t.contains("sweeper") -> "🧹"
        t.contains("laundry") || t.contains("dhobi") || t.contains("ironing") -> "🧺"
        t.contains("beauty") || t.contains("salon") || t.contains("parlour") || t.contains("parlor") || t.contains("makeup") -> "💇"
        t.contains("barber") || t.contains("hair") -> "💇‍♂️"
        t.contains("tailor") || t.contains("sewing") || t.contains("stitch") -> "🧵"

        // Trades / construction
        t.contains("electrician") || t.contains("electric") || t.contains("wiring") -> "⚡"
        t.contains("plumber") || t.contains("plumbing") || t.contains("pipe") -> "🔧"
        t.contains("carpenter") || t.contains("woodwork") || t.contains("furniture") -> "🪚"
        t.contains("painter") || t.contains("painting") -> "🎨"
        t.contains("mason") || t.contains("construction") || t.contains("building") -> "🏗️"
        t.contains("welder") || t.contains("welding") -> "🔥"
        t.contains("mechanic") || t.contains("garage") || t.contains("workshop") -> "🔩"
        t.contains("ac ") || t.contains("a.c.") || t.contains("hvac") -> "❄️"
        t.contains("technician") || t.contains("techincian") || t.contains("techinitcina") || t.contains("repair") -> "🛠️"

        // Outdoor / agri
        t.contains("gardener") || t.contains("garden") || t.contains("landscap") || t.contains("horticult") -> "🌱"
        t.contains("farm") || t.contains("agri") || t.contains("dairy") -> "🌾"

        // Security / logistics
        t.contains("security") || t.contains("guard") || t.contains("watchman") || t.contains("bouncer") -> "🛡️"
        t.contains("warehouse") || t.contains("godown") || t.contains("inventory") || t.contains("loader") || t.contains("packer") || t.contains("packing") -> "📦"

        // Office / front-of-house
        t.contains("receptionist") || t.contains("front desk") || t.contains("front-desk") -> "💼"
        t.contains("office boy") || t.contains("office assistant") || t.contains("peon") -> "🗂️"
        t.contains("data entry") || t.contains("typing") || t.contains("computer operator") -> "⌨️"
        t.contains("telecaller") || t.contains("tele caller") || t.contains("call center") || t.contains("callcenter") || t.contains("bpo") || t.contains("customer support") || t.contains("customer service") -> "☎️"
        t.contains("cashier") || t.contains("billing") -> "💵"
        t.contains("accountant") || t.contains("accounts") || t.contains("bookkeep") || t.contains("tally") -> "🧮"
        t.contains("hr ") || t.contains("recruit") || t.contains("talent") -> "🤝"

        // Sales / retail
        t.contains("sales executive") || t.contains("sales exec") || t.contains("sales exacurtin") -> "💼"
        t.contains("field sales") || t.contains("sales") || t.contains("salesman") || t.contains("marketing") -> "📊"
        t.contains("retail") || t.contains("shop") || t.contains("store") || t.contains("showroom") -> "🏪"

        // Education / care
        t.contains("teacher") || t.contains("teach") || t.contains("tutor") || t.contains("trainer") || t.contains("faculty") || t.contains("educator") || t.contains("education") -> "👩‍🏫"
        t.contains("nurse") || t.contains("medical") || t.contains("hospital") || t.contains("clinic") || t.contains("healthcare") || t.contains("health care") -> "👩‍⚕️"
        t.contains("pharmacist") || t.contains("pharmacy") -> "💊"
        t.contains("doctor") -> "🩺"

        // Tech
        t.contains("developer") || t.contains("software") || t.contains("engineer") || t.contains("programmer") || t.contains("coder") -> "💻"
        t.contains("designer") || t.contains("graphic") -> "🎨"

        // Misc labour / generic
        t.contains("helper") || t.contains("assistant") || t.contains("labour") || t.contains("labor") || t.contains("worker") -> "💪"

        else -> "💼"
    }
}

@Composable
private fun JobLottieAnimation(jobTitle: String, modifier: Modifier = Modifier) {
    // Use emoji instead of Lottie for better performance
    val emoji = getJobEmoji(jobTitle)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 28.sp
        )
    }
}

/**
 * Job Image or Animation Component
 * Priority: 1. Employer uploaded image, 2. Category icon
 */
@Composable
private fun JobImageOrAnimation(
    jobImageUrl: String?,
    jobTitle: String,
    companyName: String,
    modifier: Modifier = Modifier
) {
    if (!jobImageUrl.isNullOrBlank()) {
        OptimizedJobImage(
            imageUrl = jobImageUrl,
            contentDescription = stringResource(R.string.job_image_desc),
            modifier = modifier.clip(CircleShape),
            placeholderIcon = Icons.Default.Business
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(WorkerColors.ChipBackground)
                .border(1.dp, WorkerColors.Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = companyName,
                tint = WorkerColors.TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Get icon resource for job category
 */
private fun getJobIconResource(jobTitle: String): Int {
    return when {
        jobTitle.contains("cook", true) || jobTitle.contains("chef", true) -> R.drawable.company_default
        jobTitle.contains("driver", true) -> R.drawable.company_default
        jobTitle.contains("clean", true) || jobTitle.contains("housekeep", true) -> R.drawable.company_default
        jobTitle.contains("delivery", true) -> R.drawable.company_default
        jobTitle.contains("waiter", true) || jobTitle.contains("server", true) -> R.drawable.company_default
        jobTitle.contains("painter", true) || jobTitle.contains("paint", true) -> R.drawable.company_default
        jobTitle.contains("electric", true) -> R.drawable.company_default
        else -> R.drawable.company_default
    }
}

// NOTE: getTimeAgo() removed - use DateTimeUtils.formatRelativeTime() instead

// =============================================================================
// WORKER HOME CARD (Option A design) — flat compact card used only on the
// worker home screen. JobCard above is untouched for AllJobs and other callers.
// =============================================================================

private val HomeCardInk = Color(0xFF0F0F0F)
private val HomeCardNavy = Color(0xFF0F172A)
private val HomeCardSlate = Color(0xFF64748B)
private val HomeCardBorder = Color(0xFFE2E8F0)
private val HomeCardTile = Color(0xFFF1F5F9)
private val HomeCardRed = Color(0xFFDC2626)
private val HomeCardRedBg = Color(0xFFFEF2F2)

/**
 * Compact home-screen job card: company tile, title (+ URGENT tag only when the
 * job's urgency is HIGH, i.e. the same field the classic JobCard uses), company
 * and distance, pay, and the existing Quick Apply button (opens job details,
 * exactly like the classic card's button).
 */
@Composable
fun WorkerHomeJobCard(
    job: JobListing,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onViewTrack: (String) -> Unit = {},
    applyButtonLabel: String? = null,
    isApplied: Boolean = false
) {
    val effectiveApplyLabel = applyButtonLabel ?: stringResource(R.string.card_apply_now)
    val payDisplay = payAmountLabel(job.payAmount, job.payType)
    val payUnit = payUnitLabel(job.payAmount, job.payType)
    val facts = homeCardFacts(job)
    val subtitle = remember(job.companyName, job.distance, job.feedSection) {
        listOf(job.companyName.trim(), farPlace(job.area, job.district, job.feedSection),
            formatShortDistance(job.distance, job.distanceApprox))
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }
    val normalizedStatus = job.status.trim().lowercase()
    val isFilled = normalizedStatus == "filled"
    val isExpired = normalizedStatus == "expired"
    val isClosed = isFilled || isExpired
    val statusLabel = when {
        isFilled -> stringResource(R.string.filled)
        isExpired -> stringResource(R.string.tab_expired)
        else -> null
    }
    val isUrgent = job.isUrgent && !isClosed
    val openJob = {
        onViewTrack(job.id)
        onCardClick(job.id)
    }
    val cardShape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White.bg())
            .border(1.dp, HomeCardBorder.bd(), cardShape)
            .clickable { openJob() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeJobTile(
            imageUrl = job.photoUrl,
            category = job.category,
            title = job.title,
            description = job.description
        )
        Column(modifier = Modifier.weight(1f)) {
            HomeJobTitleRow(
                title = job.title,
                isUrgent = isUrgent,
                statusLabel = statusLabel,
                isExpired = isExpired,
                isClosed = isClosed
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = HomeCardSlate.fg(),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (facts.isNotEmpty() && !isClosed) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = facts,
                    color = HomeCardSlate.fg(),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HomeJobPayRow(
                payDisplay = payDisplay,
                payUnit = payUnit,
                showApply = !isClosed,
                applyButtonLabel = effectiveApplyLabel,
                isApplied = isApplied,
                onApplyClick = openJob
            )
        }
    }
}

/** Job photo when the employer uploaded one, else the same category icon as the Find Jobs rail. */
@Composable
private fun HomeJobTile(imageUrl: String?, category: String, title: String, description: String) {
    val tileShape = RoundedCornerShape(14.dp)
    if (!imageUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(tileShape)
                .background(HomeCardTile.bg()),
            contentAlignment = Alignment.Center
        ) {
            OptimizedJobImage(
                imageUrl = imageUrl,
                contentDescription = "Job image",
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        JobCategoryIconTile(category = category, title = title, description = description, size = 48.dp)
    }
}

@Composable
private fun HomeJobTitleRow(
    title: String,
    isUrgent: Boolean,
    statusLabel: String?,
    isExpired: Boolean,
    isClosed: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = ValidationUtils.capitalizeWords(title),
            color = if (isClosed) HomeCardSlate.fg() else HomeCardInk.fg(),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isUrgent) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(HomeCardRedBg.bg(), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(R.string.urgent_caps),
                    color = HomeCardRed.fg(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (!statusLabel.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            CompactChip(
                text = statusLabel,
                chipType = if (isExpired) ChipType.EXPIRED else ChipType.FILLED
            )
        }
    }
}

@Composable
private fun HomeJobPayRow(
    payDisplay: String,
    payUnit: String?,
    showApply: Boolean,
    applyButtonLabel: String,
    isApplied: Boolean,
    onApplyClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = payDisplay,
                color = HomeCardInk.fg(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (payUnit != null) {
                Text(
                    text = " $payUnit",
                    color = HomeCardSlate.fg(),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
        if (showApply) {
            Spacer(modifier = Modifier.width(8.dp))
            if (isApplied) {
                HomeAppliedButton()
            } else {
                HomeApplyButton(label = applyButtonLabel, onClick = onApplyClick)
            }
        }
    }
}

@Composable
private fun HomeApplyButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(HomeCardInk.bg())
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeAppliedButton() {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .border(1.dp, HomeCardBorder.bd(), shape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.applied),
            color = HomeCardSlate.fg(),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** "3.4 km" — or "~3 km" when the worker allowed only approximate location (±1–3 km). */
private fun formatShortDistance(distance: Double?, approximate: Boolean = false): String {
    if (distance == null) return ""
    if (approximate) return if (distance < 2.0) "~1 km" else "~${distance.toInt()} km"
    return if (distance < 1.0) "${(distance * 1000).toInt()} m" else "${"%.1f".format(distance)} km"
}

// =============================================================================
// PAY + FACTS
// =============================================================================

/** "₹15,000" or "Negotiable" (one rupee sign; the unit is separate). */
@Composable
internal fun payAmountLabel(payAmount: Long, payType: String): String {
    val type = com.example.dutype.employer.models.PayType.fromKey(payType)
    return if (payAmount <= 0L || type == com.example.dutype.employer.models.PayType.NEGOTIABLE) {
        stringResource(R.string.negotiable)
    } else {
        "₹" + com.example.dutype.utils.SalaryFormatter.amount(payAmount)
    }
}

/** "/day", "/week", "/month", "/hour" in the app language; null when negotiable. */
@Composable
internal fun payUnitLabel(payAmount: Long, payType: String): String? {
    val type = com.example.dutype.employer.models.PayType.fromKey(payType)
    if (payAmount <= 0L || type.unitRes == 0) return null
    return stringResource(type.unitRes)
}

/** "2h ago", "3d ago", "Just posted"; null after two weeks (not worth showing). */
@Composable
private fun postedAgoLabel(createdAt: Long): String? {
    if (createdAt <= 0L) return null
    val minutes = (System.currentTimeMillis() - createdAt) / 60_000L
    return when {
        minutes < 60 -> stringResource(R.string.card_posted_just_now)
        minutes < 24 * 60 -> stringResource(R.string.card_posted_hours, (minutes / 60).toInt())
        minutes < 14 * 24 * 60 -> stringResource(R.string.card_posted_days, (minutes / (24 * 60)).toInt())
        else -> null
    }
}

@Composable
private fun shiftLabel(shift: String): String? = when (shift.uppercase()) {
    com.example.dutype.firestore.FirestoreSchema.Values.Shift.DAY -> stringResource(R.string.card_day_shift)
    com.example.dutype.firestore.FirestoreSchema.Values.Shift.NIGHT -> stringResource(R.string.card_night_shift)
    else -> null
}

/**
 * Job type · shift · openings as small chips, then "Posted 2h ago · 12 applied" (or "Be the first
 * to apply" for a fresh job) — what a worker checks before tapping.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobCardFacts(
    workTypeLabel: String?,
    shift: String,
    vacancies: Int,
    postedAt: Long,
    applicants: Int,
    isClosed: Boolean
) {
    val shiftText = shiftLabel(shift)
    val chips = listOfNotNull(
        workTypeLabel?.takeIf { it.isNotBlank() }?.let { it to ChipType.VACANCY },
        shiftText?.let { it to ChipType.VACANCY },
        if (vacancies > 1) stringResource(R.string.card_openings, vacancies) to ChipType.VACANCY else null
    )
    if (chips.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chips.forEach { (text, type) -> CompactChip(text = text, chipType = type) }
        }
    }
    if (isClosed) return
    val posted = postedAgoLabel(postedAt)
    val social = if (applicants > 0) stringResource(R.string.card_applied_count, applicants) else null
    val line = listOfNotNull(posted, social).joinToString(" · ")
    val firstToApply = applicants == 0
    if (line.isBlank() && !firstToApply) return
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (line.isNotBlank()) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextTertiary, fontSize = 11.sp),
                maxLines = 1
            )
        }
        if (firstToApply) {
            if (line.isNotBlank()) Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.card_be_first),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = WorkerColors.Success,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
        }
    }
}

/** One compact line for the home card: "Full-time · Day shift · 3 openings · 2h ago". */
@Composable
private fun homeCardFacts(job: JobListing): String = listOfNotNull(
    stringResource(EmploymentType.fromKey(job.employmentType).titleRes),
    shiftLabel(job.shift),
    if (job.vacancies > 1) stringResource(R.string.card_openings, job.vacancies) else null,
    postedAgoLabel(job.createdAt)
).joinToString(" · ")
