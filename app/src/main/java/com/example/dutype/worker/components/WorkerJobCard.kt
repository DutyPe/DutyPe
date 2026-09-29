package com.example.dutype.worker.components

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
import com.example.dutype.models.JobListingSummary
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.ValidationUtils

/**
 * JobCard that accepts JobListing directly - PREFERRED
 * Uses only card fields: title, companyName, salary, salaryType, urgency, status, distance, isSaved
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
    applyButtonLabel: String = "Quick Apply"
) {
    var localIsSaved by remember { mutableStateOf(isSaved) }
    LaunchedEffect(isSaved) { localIsSaved = isSaved }

    val payDisplay = remember(job.salary, job.salaryType) {
        formatPayDisplay(job.salary, job.salaryType)
    }
    val locationDisplay = remember(job.addressText, job.location, job.distance) {
        formatLocationWithDistance(job.addressText.ifBlank { job.location }, job.distance)
    }
    val normalizedStatus = job.status.trim().lowercase()
    val isFilled = normalizedStatus == "closed"
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
        locationDisplay = locationDisplay,
        vacancies = job.vacancies,
        workTypeLabel = extractWorkTypeLabel(
            job.jobType,
            job.title,
            job.description
        ),
        isClosed = isFilled || isExpired,
        statusLabel = statusLabel,
        isFilled = isFilled,
        isExpired = isExpired,
        isUrgent = job.urgency.equals("HIGH", ignoreCase = true),
        isVerified = job.isVerified,
        applyButtonLabel = applyButtonLabel,
        isSaved = localIsSaved,
        jobImageUrl = job.jobImageUrl,
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
 * Uses only card fields: title, companyName, salary, salaryType, urgency, status, distance, isSaved
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
    applyButtonLabel: String = "Quick Apply"
) {
    var localIsSaved by remember { mutableStateOf(isSaved) }
    LaunchedEffect(isSaved) { localIsSaved = isSaved }

    val payDisplay = remember(job.salary, job.salaryType) {
        formatPayDisplay(job.salary, job.salaryType)
    }
    val locationDisplay = remember(job.locationText, job.companyCity, job.distance) {
        formatLocationWithDistance(
            job.locationText.ifBlank { job.companyCity },
            job.distance
        )
    }
    val normalizedStatus = job.status.trim().lowercase()
    val isFilled = normalizedStatus == "closed"
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
        locationDisplay = locationDisplay,
        vacancies = job.vacancies,
        workTypeLabel = extractWorkTypeLabel(
            job.jobType,
            job.title,
            ""
        ),
        isClosed = isFilled || isExpired,
        statusLabel = statusLabel,
        isFilled = isFilled,
        isExpired = isExpired,
        // JobListingSummary has no isVerified field (card-list projection only
        // carries jobmetadata fields) — verified badge is not shown here.
        isUrgent = job.urgency.equals("HIGH", ignoreCase = true),
        isVerified = false,
        applyButtonLabel = applyButtonLabel,
        isSaved = localIsSaved,
        jobImageUrl = job.jobImageUrl,
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
    locationDisplay: String,
    vacancies: Int,
    workTypeLabel: String?,
    isClosed: Boolean,
    statusLabel: String?,
    isFilled: Boolean,
    isExpired: Boolean,
    isSaved: Boolean,
    isUrgent: Boolean = false,
    isVerified: Boolean = false,
    applyButtonLabel: String = "Quick Apply",
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

                    if (isVerified) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = WorkerColors.Success,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Verified",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = WorkerColors.Success,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
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

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom row: bold green salary + black pill apply button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (payDisplay == "Negotiable") {
                        Text(
                            text = payDisplay,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = WorkerColors.Success,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    } else {
                        Text(
                            text = "₹${payDisplay.substringBefore("/")}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = WorkerColors.Success,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                        if (payDisplay.contains("/")) {
                            Text(
                                text = " /${payDisplay.substringAfter("/")}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = WorkerColors.TextSecondary,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            )
                        }
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
            text = "URGENT",
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
 * Bug #6: salary is now a free-form String. Delegate to SalaryFormatter
 * which handles "Negotiable", ranges ("1000-2000"), "2000+" and plain
 * numbers, appending the period suffix when appropriate.
 */
private fun formatPayDisplay(salary: String, salaryType: String): String =
    com.example.dutype.utils.SalaryFormatter.display(salary, salaryType)

/**
 * Format location with distance.
 * addressText comes from job_details (runtime only), distance is computed client-side.
 */
private fun formatLocationWithDistance(addressText: String, distance: Double?): String {
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
        distance < 1.0 -> "${(distance * 1000).toInt()}m away"
        distance < 2.0 -> "${"%.1f".format(distance)} km walkable"
        else -> "${"%.1f".format(distance)} km away"
    }
    return if (shortLocation.isNotEmpty()) "$distStr - $shortLocation" else distStr
}

private fun extractWorkTypeLabel(vararg candidates: String?): String? {
    val text = candidates
        .filterNotNull()
        .joinToString(" ")
        .lowercase()

    if (text.isBlank()) return null

    return when {
        listOf("part-time", "part time", "parttime", "weekend", "student").any(text::contains) -> "Part-time"
        listOf("full-time", "full time", "fulltime").any(text::contains) -> "Full-time"
        text.contains("contract") -> "Contract"
        text.contains("temporary") || text.contains("temp") -> "Temporary"
        else -> null
    }
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
    // Priority 1: Show employer uploaded image if available — fits inside the
    // caller's modifier (44dp circular avatar) instead of forcing 180dp height.
    if (!jobImageUrl.isNullOrBlank()) {
        OptimizedJobImage(
            imageUrl = jobImageUrl,
            contentDescription = "Job image",
            modifier = modifier.clip(CircleShape)
        )
    } else {
        // Priority 2: pick a category-specific emoji from the job title so
        // the card never feels generic when the employer skipped the image
        // upload. Falls back to a briefcase for unmatched titles.
        val emoji = getJobEmoji(jobTitle)
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(WorkerColors.WarningLight)
                .border(1.dp, WorkerColors.Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 22.sp
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
    applyButtonLabel: String = "Quick Apply",
    isApplied: Boolean = false
) {
    val payDisplay = remember(job.salary, job.salaryType) {
        formatPayDisplay(job.salary, job.salaryType)
    }
    val subtitle = remember(job.companyName, job.distance) {
        listOf(job.companyName.trim(), formatShortDistance(job.distance))
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }
    val normalizedStatus = job.status.trim().lowercase()
    val isFilled = normalizedStatus == "closed"
    val isExpired = normalizedStatus == "expired"
    val isClosed = isFilled || isExpired
    val statusLabel = when {
        isFilled -> stringResource(R.string.filled)
        isExpired -> stringResource(R.string.tab_expired)
        else -> null
    }
    val isUrgent = job.urgency.equals("HIGH", ignoreCase = true) && !isClosed
    val openJob = {
        onViewTrack(job.id)
        onCardClick(job.id)
    }
    val cardShape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color.White)
            .border(1.dp, HomeCardBorder, cardShape)
            .clickable { openJob() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeJobTile(
            imageUrl = job.jobImageUrl,
            initialSource = job.companyName.ifBlank { job.title }
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
                    color = HomeCardSlate,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HomeJobPayRow(
                payDisplay = payDisplay,
                showApply = !isClosed,
                applyButtonLabel = applyButtonLabel,
                isApplied = isApplied,
                onApplyClick = openJob
            )
        }
    }
}

@Composable
private fun HomeJobTile(imageUrl: String?, initialSource: String) {
    val tileShape = RoundedCornerShape(14.dp)
    val initial = remember(initialSource) {
        initialSource.trim().take(1).uppercase().ifBlank { "J" }
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(tileShape)
            .background(HomeCardTile),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            OptimizedJobImage(
                imageUrl = imageUrl,
                contentDescription = "Job image",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                color = HomeCardNavy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
            color = if (isClosed) HomeCardSlate else HomeCardInk,
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
                    .background(HomeCardRedBg, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "URGENT",
                    color = HomeCardRed,
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
            if (payDisplay == "Negotiable") {
                Text(
                    text = payDisplay,
                    color = HomeCardInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            } else {
                Text(
                    text = "₹${payDisplay.substringBefore("/")}",
                    color = HomeCardInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (payDisplay.contains("/")) {
                    Text(
                        text = " /${payDisplay.substringAfter("/")}",
                        color = HomeCardSlate,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
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
            .background(HomeCardInk)
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
            .border(1.dp, HomeCardBorder, shape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Applied",
            color = HomeCardSlate,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun formatShortDistance(distance: Double?): String {
    if (distance == null) return ""
    return if (distance < 1.0) "${(distance * 1000).toInt()} m" else "${"%.1f".format(distance)} km"
}
