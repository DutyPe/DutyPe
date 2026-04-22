package com.example.dutype.worker.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import com.dutype.app.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
 * Uses only schema fields: title, jobType, salary, salaryType, urgency, status, distance, isSaved
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
    val isUrgent = job.urgency.equals("HIGH", ignoreCase = true)
    val isClosed = job.status.equals("closed", ignoreCase = true) || job.status.equals("expired", ignoreCase = true)

    JobCardInternal(
        jobId = job.id,
        title = job.title,
        companyName = job.companyName,
        payDisplay = payDisplay,
        locationDisplay = locationDisplay,
        jobType = job.jobType,
        workTypeLabel = extractWorkTypeLabel(
            job.workingHours,
            job.shiftTiming,
            job.title,
            job.description
        ),
        isUrgent = isUrgent,
        isClosed = isClosed,
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
 * PERFORMANCE OPTIMIZED: JobCard that accepts JobListingSummary
 * Uses only schema fields: title, jobType, salary, salaryType, urgency, status, distance, isSaved
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
    val isUrgent = job.urgency.equals("HIGH", ignoreCase = true)
    val isClosed = job.status.equals("closed", ignoreCase = true) || job.status.equals("expired", ignoreCase = true)

    JobCardInternal(
        jobId = job.id,
        title = job.title,
        companyName = job.companyName,
        payDisplay = payDisplay,
        locationDisplay = locationDisplay,
        jobType = job.jobType,
        workTypeLabel = extractWorkTypeLabel(job.jobType, job.title),
        isUrgent = isUrgent,
        isClosed = isClosed,
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobCardInternal(
    jobId: String,
    title: String,
    companyName: String,
    payDisplay: String,
    locationDisplay: String,
    jobType: String,
    workTypeLabel: String?,
    isUrgent: Boolean,
    isClosed: Boolean,
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.86f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Solid card surface (no gradient) per the role-theme rule.
                .background(com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 34.dp, y = (-28).dp)
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF93C5FD).copy(alpha = 0.2f))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-24).dp, y = 26.dp)
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF67E8F9).copy(alpha = 0.18f))
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
                            color = com.example.dutype.ui.theme.LocalRoleColors.current.secondaryBackground,
                            shape = CircleShape
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    JobImageOrAnimation(
                        jobImageUrl = jobImageUrl,
                        jobTitle = title,
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
                            color = if (isClosed) Color(0xFF6B7280) else Color(0xFF111827),
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
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = companyName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF374151),
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
                        .background(Color.White.copy(alpha = 0.72f))
                        .border(1.dp, Color.White.copy(alpha = 0.82f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isSaved) Color(0xFFEF4444) else Color(0xFF9CA3AF),
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
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                } else {
                    Text(
                        text = "₹${payDisplay.substringBefore("/")}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    if (payDisplay.contains("/")) {
                        Text(
                            text = "/${payDisplay.substringAfter("/")}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF6B7280),
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
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = locationDisplay,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Row 4: Tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (jobType.isNotEmpty()) {
                    CompactChip(text = jobType, chipType = ChipType.JOB_TYPE)
                }

                if (!workTypeLabel.isNullOrBlank() && !jobType.equals(workTypeLabel, ignoreCase = true)) {
                    CompactChip(text = workTypeLabel, chipType = ChipType.JOB_TYPE)
                }

                if (isUrgent) {
                    CompactChip(text = "Urgent Hiring", chipType = ChipType.URGENT)
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
    borderColor: Color = Color(0xFFE5E7EB),
    textColor: Color = Color(0xFF374151),
    chipType: ChipType = ChipType.DEFAULT
) {
    // Determine colors based on chip type - matching the provided style
    val (bgColor, txtColor, bdrColor) = when {
        backgroundColor != Color.Transparent -> Triple(backgroundColor, textColor, borderColor)
        else -> when (chipType) {
            ChipType.VACANCY -> Triple(
                Color(0xFF374151).copy(alpha = 0.1f), // Light black/gray background
                Color(0xFF374151), // Dark gray text (not full black)
                Color(0xFF374151)  // Dark gray border
            )
            ChipType.JOB_TYPE -> Triple(
                Color(0xFFF59E0B).copy(alpha = 0.1f), // Light amber background
                Color(0xFF92400E), // Dark amber text
                Color(0xFFF59E0B)  // Amber border
            )
            ChipType.CATEGORY -> Triple(
                Color(0xFFF59E0B).copy(alpha = 0.1f), // Light amber background
                Color(0xFF92400E), // Dark amber text
                Color(0xFFF59E0B)  // Amber border
            )
            ChipType.URGENT -> Triple(
                Color(0xFFF59E0B).copy(alpha = 0.1f), // Light amber background
                Color(0xFF92400E), // Dark amber text
                Color(0xFFF59E0B)  // Amber border
            )
            ChipType.DEFAULT -> Triple(
                Color(0xFFF59E0B).copy(alpha = 0.1f), // Light amber background
                Color(0xFF92400E), // Dark amber text
                Color(0xFFF59E0B)  // Amber border
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
    val normalizedLocation = addressText
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .joinToString(", ")

    if (distance == null) return normalizedLocation
    val distStr = when {
        distance < 1.0 -> "${(distance * 1000).toInt()}m away"
        distance < 2.0 -> "${"%.1f".format(distance)} km walkable"
        else -> "${"%.1f".format(distance)} km away"
    }
    return if (normalizedLocation.isNotEmpty()) "$normalizedLocation • $distStr" else distStr
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
 * Get emoji for job category - lightweight alternative to Lottie animations
 */
private fun getJobEmoji(jobTitle: String): String {
    return when {
        jobTitle.contains("delivery", true) || jobTitle.contains("courier", true) -> "🚚"
        jobTitle.contains("driver", true) || jobTitle.contains("driving", true) -> "🚗"
        jobTitle.contains("cook", true) || jobTitle.contains("chef", true) || jobTitle.contains("kitchen", true) -> "👨‍🍳"
        jobTitle.contains("clean", true) || jobTitle.contains("housekeep", true) || jobTitle.contains("maid", true) -> "🧹"
        jobTitle.contains("waiter", true) || jobTitle.contains("server", true) || jobTitle.contains("restaurant", true) -> "🍽️"
        jobTitle.contains("security", true) || jobTitle.contains("guard", true) -> "🛡️"
        jobTitle.contains("painter", true) || jobTitle.contains("paint", true) -> "🎨"
        jobTitle.contains("electric", true) || jobTitle.contains("electrician", true) -> "⚡"
        jobTitle.contains("plumb", true) || jobTitle.contains("plumber", true) -> "🔧"
        jobTitle.contains("carpenter", true) || jobTitle.contains("wood", true) -> "🪚"
        jobTitle.contains("helper", true) || jobTitle.contains("labour", true) || jobTitle.contains("labor", true) -> "💪"
        jobTitle.contains("office", true) || jobTitle.contains("admin", true) || jobTitle.contains("data entry", true) -> "💼"
        jobTitle.contains("sales", true) || jobTitle.contains("marketing", true) -> "📊"
        jobTitle.contains("retail", true) || jobTitle.contains("shop", true) || jobTitle.contains("store", true) -> "🏪"
        jobTitle.contains("warehouse", true) || jobTitle.contains("packing", true) || jobTitle.contains("loading", true) -> "📦"
        jobTitle.contains("construction", true) || jobTitle.contains("mason", true) || jobTitle.contains("building", true) -> "🏗️"
        jobTitle.contains("garden", true) || jobTitle.contains("landscap", true) -> "🌱"
        jobTitle.contains("tailor", true) || jobTitle.contains("sewing", true) || jobTitle.contains("stitch", true) -> "🧵"
        jobTitle.contains("beauty", true) || jobTitle.contains("salon", true) || jobTitle.contains("parlour", true) -> "💇"
        jobTitle.contains("teach", true) || jobTitle.contains("tutor", true) || jobTitle.contains("education", true) -> "📚"
        jobTitle.contains("nurse", true) || jobTitle.contains("medical", true) || jobTitle.contains("health", true) -> "🏥"
        jobTitle.contains("ac", true) || jobTitle.contains("technician", true) || jobTitle.contains("repair", true) -> "🔨"
        jobTitle.contains("event", true) || jobTitle.contains("catering", true) -> "🎉"
        else -> "💼" // Default briefcase for general jobs
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Priority 1: Show employer uploaded image if available — fits inside the
    // caller's modifier (44dp circular avatar) instead of forcing 180dp height.
    if (!jobImageUrl.isNullOrBlank()) {
        OptimizedJobImage(
            imageUrl = jobImageUrl,
            contentDescription = "Job image",
            modifier = modifier.clip(CircleShape)
        )
    } else {
        // Priority 2: Show category icon (Lottie removed for performance)
        JobLottieAnimation(jobTitle = jobTitle, modifier = modifier)
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
