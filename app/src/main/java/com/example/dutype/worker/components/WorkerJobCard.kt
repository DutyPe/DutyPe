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
    onViewTrack: (String) -> Unit = {}
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
    onViewTrack: (String) -> Unit = {}
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
    jobImageUrl: String? = null,
    onSaveClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, WorkerColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(com.example.dutype.ui.theme.WorkerColors.CardBackground)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 34.dp, y = (-28).dp)
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-24).dp, y = 26.dp)
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
            // Row 1: Job Icon + Title + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        // Solid surface for the job icon — no gradient.
                        .background(
                            color = com.example.dutype.ui.theme.WorkerColors.CardBackground,
                            shape = CircleShape
                        )
                        .border(1.dp, com.example.dutype.ui.theme.WorkerColors.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    JobImageOrAnimation(
                        jobImageUrl = jobImageUrl,
                        jobTitle = title,
                        companyName = companyName,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = ValidationUtils.capitalizeWords(title),
                        style = AppTypography.cardTitle.copy(
                            color = if (isClosed) WorkerColors.TextSecondary else WorkerColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (companyName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = companyName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = WorkerColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        onSaveClick()
                        Toast.makeText(context, if (!isSaved) "Job saved!" else "Job removed!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(com.example.dutype.ui.theme.WorkerColors.CardBackground)
                        .border(1.dp, WorkerColors.Border, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isSaved) WorkerColors.Error else WorkerColors.TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Pay
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (payDisplay == "Negotiable") {
                    Text(
                        text = payDisplay,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = WorkerColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                } else {
                    Text(
                        text = "₹${payDisplay.substringBefore("/")}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = WorkerColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    if (payDisplay.contains("/")) {
                        Text(
                            text = "/${payDisplay.substringAfter("/")}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = WorkerColors.TextSecondary,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Location
            if (locationDisplay.isNotEmpty()) {
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
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Row 4: Tags
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!statusLabel.isNullOrBlank()) {
                    CompactChip(
                        text = statusLabel,
                        chipType = when {
                            isFilled -> ChipType.FILLED
                            isExpired -> ChipType.EXPIRED
                            else -> ChipType.DEFAULT
                        }
                    )
                }

                CompactChip(
                    text = if (vacancies == 1) "1 vacancy" else "$vacancies vacancies",
                    chipType = ChipType.VACANCY
                )

                if (!workTypeLabel.isNullOrBlank()) {
                    CompactChip(text = workTypeLabel, chipType = ChipType.JOB_TYPE)
                }
            }
        }
        }
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
            .border(0.5.dp, bdrColor, RoundedCornerShape(16.dp))
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
