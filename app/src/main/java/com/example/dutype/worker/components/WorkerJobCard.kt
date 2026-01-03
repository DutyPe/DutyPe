package com.example.dutype.worker.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants
import com.dutype.app.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import timber.log.Timber
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.worker.models.LocationInfo
import com.example.dutype.worker.models.PayInfo
import com.example.dutype.employer.models.PayType
import com.example.dutype.worker.models.TimeInfo
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.utils.toPayInfo
import com.example.dutype.utils.toLocationInfo
import com.example.dutype.utils.toTimeInfo
import com.example.dutype.utils.hasUrgentHiring
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.utils.AIScamDetector
import com.example.dutype.components.TrustBadge
import com.example.dutype.components.TrustBadgeSize
import com.example.dutype.components.JobSafetyBadge
import com.example.dutype.models.parseTrustTier

/**
 * JobCard that accepts JobListing directly - PREFERRED
 * This eliminates the need for JobCardModel conversion
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCard(
    job: JobListing,
    onApplyClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = job.isSaved,
    hasApplied: Boolean = false,
    onViewTrack: (String) -> Unit = {}
) {
    // Convert JobListing to UI models using extension functions
    val payInfo = remember(job) { job.toPayInfo() }
    val locationInfo = remember(job) { job.toLocationInfo() }
    val timeInfo = remember(job) { job.toTimeInfo() }
    val isUrgentHiring = remember(job) { job.hasUrgentHiring() }
    
    // AI Scam Detection - Quick analysis for card display
    val safetyAnalysis = remember(job.title, job.description, job.payAmount) {
        AIScamDetector.analyzeJob(
            title = job.title,
            description = job.description,
            category = job.category,
            payAmount = job.payAmount.ifEmpty { job.salary },
            payType = job.payType,
            location = job.area ?: job.location,
            hasVerifiedBadge = job.employerTrustTier.contains("VERIFIED", ignoreCase = true)
        )
    }
    
    JobCardInternal(
        jobId = job.jobId.ifEmpty { job.id },
        title = job.title,
        employerName = job.companyName.ifEmpty { job.company },
        payInfo = payInfo,
        location = locationInfo,
        timeInfo = timeInfo,
        vacancies = job.vacancies,
        jobType = job.jobType,
        jobImageUrl = job.jobImageUrl,
        isFilled = job.isFilled,
        employerTrustTier = job.employerTrustTier,
        isUrgentHiring = isUrgentHiring,
        postedAt = job.postedAt,
        onApplyClick = onApplyClick,
        onSaveClick = onSaveClick,
        onCardClick = onCardClick,
        modifier = modifier,
        isSaved = isSaved,
        hasApplied = hasApplied,
        onViewTrack = onViewTrack,
        riskLevel = safetyAnalysis.riskLevel
    )
}

/**
 * PERFORMANCE OPTIMIZED: JobCard that accepts JobListingSummary
 * 
 * This card renders using only the lightweight summary data (~15 fields)
 * instead of the full JobListing (~50+ fields). This reduces:
 * - Memory footprint for large lists
 * - Parse time for each job
 * - Network bandwidth (when using summary fetch)
 * 
 * Full job details are fetched only when user clicks on the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCard(
    job: JobListingSummary,
    onApplyClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = job.isSaved,
    hasApplied: Boolean = false,
    onViewTrack: (String) -> Unit = {}
) {
    // Format pay display
    val payDisplay = remember(job.payAmount, job.payType) {
        formatSummaryPayDisplay(job.payAmount, job.payType)
    }
    
    // Format location with distance
    val locationDisplay = remember(job.location, job.distance) {
        formatSummaryLocationWithDistance(job.location, job.distance)
    }
    
    // Calculate posted time ago
    val postedTimeAgo = remember(job.postedAt) {
        if (job.postedAt == 0L) "" else DateTimeUtils.formatTimeAgo(job.postedAt)
    }
    
    // Parse trust tier
    val trustTier = remember(job.employerTrustTier) {
        parseTrustTier(job.employerTrustTier)
    }
    
    val context = LocalContext.current
    var localIsSaved by remember { mutableStateOf(isSaved) }
    var showAd by remember { mutableStateOf(false) }
    var pendingJobId by remember { mutableStateOf("") }
    
    LaunchedEffect(isSaved) {
        localIsSaved = isSaved
    }
    
    val handleSaveClick = {
        localIsSaved = !localIsSaved
        onSaveClick(job.jobId.ifEmpty { job.id })
        Toast.makeText(context, if (localIsSaved) "Job saved!" else "Job removed!", Toast.LENGTH_SHORT).show()
    }
    
    val jobId = job.jobId.ifEmpty { job.id }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onViewTrack(jobId)
                pendingJobId = jobId
                showAd = true
            }
            .border(0.5.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(14.dp)
        ) {
            // Row 1: Job Icon + Title/Company + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Job Image/Animation Icon - Circular
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    JobImageOrAnimation(
                        jobImageUrl = job.jobImageUrl,
                        jobTitle = job.title,
                        modifier = Modifier.size(52.dp)
                    )
                }

                // Title and Company
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = ValidationUtils.capitalizeWords(job.title),
                        style = AppTypography.cardTitle.copy(
                            color = if (job.isFilled) Color(0xFF6B7280) else Color(0xFF111827),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Company name with employer status inline
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = ValidationUtils.capitalizeWords(job.companyName),
                            style = AppTypography.caption.copy(
                                color = Color(0xFF6B7280),
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        // Posted time ago
                        if (postedTimeAgo.isNotEmpty()) {
                            Text(
                                text = "• $postedTimeAgo",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.sp
                                )
                            )
                        }
                        
                        // Trust Badge
                        TrustBadge(
                            tier = trustTier,
                            size = TrustBadgeSize.SMALL,
                            showLabel = true
                        )
                    }
                }

                // Favorite button
                IconButton(
                    onClick = handleSaveClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (localIsSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (localIsSaved) Color(0xFFEF4444) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Pay Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = payDisplay.first,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF111827),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = payDisplay.second,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 4: Location with distance
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
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

            // Row 5: Tags and Apply Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tags row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Vacancy chip
                    CompactChip(
                        text = "${job.vacancies} ${if (job.vacancies == 1) "Vacancy" else "Vacancies"}",
                        chipType = ChipType.VACANCY
                    )
                    
                    // Job type chip
                    if (job.jobType.isNotEmpty()) {
                        CompactChip(
                            text = job.jobType,
                            chipType = ChipType.JOB_TYPE
                        )
                    }
                    
                    // Urgent Hiring chip
                    if (job.isUrgent()) {
                        CompactChip(
                            text = "Urgent Hiring",
                            chipType = ChipType.URGENT
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Apply Now button
                Button(
                    onClick = { if (!hasApplied) onApplyClick(jobId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2937),
                        disabledContainerColor = Color(0xFF9CA3AF)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp),
                    enabled = !hasApplied
                ) {
                    Text(
                        text = if (hasApplied) "Applied" else "Apply Now",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
    
    if (showAd) {
        showAd = false
        if (pendingJobId.isNotEmpty()) {
            onCardClick(pendingJobId)
            pendingJobId = ""
        }
    }
}

/**
 * Internal JobCard implementation - shared by both overloads
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobCardInternal(
    jobId: String,
    title: String,
    employerName: String,
    payInfo: PayInfo,
    location: LocationInfo,
    timeInfo: com.example.dutype.worker.models.TimeInfo,
    vacancies: Int,
    jobType: String,
    jobImageUrl: String?,
    isFilled: Boolean,
    employerTrustTier: String,
    isUrgentHiring: Boolean,
    postedAt: Long,
    onApplyClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = false,
    hasApplied: Boolean = false,
    onViewTrack: (String) -> Unit = {},
    riskLevel: AIScamDetector.RiskLevel = AIScamDetector.RiskLevel.SAFE
) {
    val context = LocalContext.current
    var localIsSaved by remember { mutableStateOf(isSaved) }
    var showAd by remember { mutableStateOf(false) }
    var pendingJobId by remember { mutableStateOf("") }

    // Parse trust tier
    val trustTier = remember(employerTrustTier) {
        parseTrustTier(employerTrustTier)
    }
    
    // Calculate posted time ago
    val postedTimeAgo = remember(postedAt) {
        if (postedAt == 0L) "" else DateTimeUtils.formatRelativeTime(postedAt)
    }

    LaunchedEffect(isSaved) {
        localIsSaved = isSaved
    }
    
    val handleSaveClick = {
        localIsSaved = !localIsSaved
        onSaveClick(jobId)
        Toast.makeText(context, if (localIsSaved) "Job saved!" else "Job removed!", Toast.LENGTH_SHORT).show()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onViewTrack(jobId)
                pendingJobId = jobId
                showAd = true
            }
            .border(0.5.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(14.dp)
        ) {
            // Row 1: Job Icon + Title/Company + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Job Image/Animation Icon - Circular
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    JobImageOrAnimation(
                        jobImageUrl = jobImageUrl,
                        jobTitle = title,
                        modifier = Modifier.size(52.dp)
                    )
                }

                // Title and Company
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = ValidationUtils.capitalizeWords(title),
                        style = AppTypography.cardTitle.copy(
                            color = if (isFilled) Color(0xFF6B7280) else Color(0xFF111827),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Company name with employer status inline
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = ValidationUtils.capitalizeWords(employerName),
                            style = AppTypography.caption.copy(
                                color = Color(0xFF6B7280),
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        // Posted time ago
                        if (postedTimeAgo.isNotEmpty()) {
                            Text(
                                text = "• $postedTimeAgo",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.sp
                                )
                            )
                        }
                        
                        // Trust Badge
                        TrustBadge(
                            tier = trustTier,
                            size = TrustBadgeSize.SMALL,
                            showLabel = true
                        )
                    }
                }

                // Favorite button
                IconButton(
                    onClick = handleSaveClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (localIsSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (localIsSaved) Color(0xFFEF4444) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Pay Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₹${payInfo.getFormattedPay()}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF111827),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = payInfo.getPaymentSuffix(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 4: Location with distance
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = location.getLocationWithDistance(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 5: Tags and Apply Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tags row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Safety Badge - Show for risky jobs (MEDIUM and above)
                    JobSafetyBadge(
                        riskLevel = riskLevel,
                        showLabel = true
                    )
                    
                    // Vacancy chip
                    CompactChip(
                        text = "$vacancies ${if (vacancies == 1) "Vacancy" else "Vacancies"}",
                        chipType = ChipType.VACANCY
                    )
                    
                    // Job type chip
                    if (jobType.isNotEmpty()) {
                        CompactChip(
                            text = jobType,
                            chipType = ChipType.JOB_TYPE
                        )
                    }
                    
                    // Urgent Hiring chip
                    if (isUrgentHiring) {
                        CompactChip(
                            text = "Urgent Hiring",
                            chipType = ChipType.URGENT
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Apply Now button
                Button(
                    onClick = { if (!hasApplied) onApplyClick(jobId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2937),
                        disabledContainerColor = Color(0xFF9CA3AF)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp),
                    enabled = !hasApplied
                ) {
                    Text(
                        text = if (hasApplied) "Applied" else "Apply Now",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
    
    if (showAd) {
        showAd = false
        if (pendingJobId.isNotEmpty()) {
            onCardClick(pendingJobId)
            pendingJobId = ""
        }
    }
}

/**
 * Compact chip with colored backgrounds based on chip type
 * - Vacancy chips: Blue background
 * - Job type chips (Full-time, Part-time): Green background
 * - Category chips (Driver, Cook, etc.): Purple background
 * - Urgent Hiring: Amber/Yellow background
 */
@Composable
private fun CompactChip(
    text: String,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = Color(0xFFE5E7EB),
    textColor: Color = Color(0xFF374151),
    chipType: ChipType = ChipType.DEFAULT
) {
    // Determine colors based on chip type
    val (bgColor, txtColor, bdrColor) = when {
        backgroundColor != Color.Transparent -> Triple(backgroundColor, textColor, borderColor)
        else -> when (chipType) {
            ChipType.VACANCY -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), Color(0xFF93C5FD)) // Blue
            ChipType.JOB_TYPE -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), Color(0xFF6EE7B7)) // Green
            ChipType.CATEGORY -> Triple(Color(0xFFEDE9FE), Color(0xFF5B21B6), Color(0xFFC4B5FD)) // Purple
            ChipType.URGENT -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Color(0xFFFCD34D)) // Amber
            ChipType.DEFAULT -> Triple(Color(0xFFF3F4F6), Color(0xFF374151), Color(0xFFE5E7EB)) // Gray
        }
    }
    
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(14.dp))
            .border(1.dp, bdrColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = txtColor,
                fontSize = 11.sp
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

// Extension function for PayInfo - Updated with black color and smaller "paid after" text
fun PayInfo.getFormattedPay(): String {
    val trimmedAmount = amount.trim()
    return when {
        type == PayType.TASK || period.contains("delivery", true) || period.contains("task", true) -> 
            "$trimmedAmount/delivery"
        type == PayType.DAILY -> "$trimmedAmount/day"
        type == PayType.HOURLY -> "$trimmedAmount/hour"
        type == PayType.MONTHLY -> "$trimmedAmount/month"
        else -> "$trimmedAmount/day"
    }
}

// Get the payment suffix text
fun PayInfo.getPaymentSuffix(): String {
    return when {
        type == PayType.TASK || period.contains("delivery", true) || period.contains("task", true) -> 
            "paid after delivery"
        type == PayType.DAILY -> "paid after shift"
        type == PayType.HOURLY -> "paid hourly"
        type == PayType.MONTHLY -> "paid monthly"
        else -> "paid after shift"
    }
}

// Extension function for LocationInfo - Updated format with precise distance
fun LocationInfo.getLocationWithDistance(): String {
    val distanceValue = distance.replace("km", "").replace("m", "").replace(" ", "").toDoubleOrNull()
    val isMeters = distance.contains("m") && !distance.contains("km")
    
    return when {
        distanceValue == null || distance == "N/A" -> getDisplayText()
        isMeters -> {
            // Distance is in meters
            "${getDisplayText()} • ${distance} away"
        }
        distanceValue < 1.0 -> {
            // Less than 1km - show in meters
            val meters = (distanceValue * 1000).toInt()
            "${getDisplayText()} • ${meters}m away"
        }
        distanceValue < 2.0 -> {
            // 1-2km - walkable distance
            "${getDisplayText()} • ${String.format("%.1f", distanceValue)} km walkable"
        }
        else -> {
            // More than 2km
            "${getDisplayText()} • ${String.format("%.1f", distanceValue)} km away"
        }
    }
}

@Composable
private fun JobLottieAnimation(jobTitle: String, modifier: Modifier = Modifier) {
    val lottieFile = getJobLottieFile(jobTitle)
    
    // If no matching animation, show company icon instead
    if (lottieFile == null) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.company_default),
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = modifier.fillMaxSize().padding(12.dp)
        )
    } else {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieFile))
        val progress by animateLottieCompositionAsState(composition = composition, iterations = LottieConstants.IterateForever)
        LottieAnimation(composition = composition, progress = { progress }, modifier = modifier.fillMaxSize())
    }
}

/**
 * Job Image or Animation Component
 * Priority: 1. Employer uploaded image, 2. Lottie animation, 3. Company icon
 */
@Composable
private fun JobImageOrAnimation(
    jobImageUrl: String?,
    jobTitle: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Priority 1: Show employer uploaded image if available
    if (!jobImageUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(jobImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Job image",
            modifier = modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Priority 2 & 3: Show Lottie animation or company icon
        JobLottieAnimation(jobTitle = jobTitle, modifier = modifier)
    }
}

private fun getJobLottieFile(jobTitle: String): Int? {
    return when {
        jobTitle.contains("cook", true) || jobTitle.contains("chef", true) -> R.raw.cook
        jobTitle.contains("driver", true) -> R.raw.driver
        jobTitle.contains("clean", true) || jobTitle.contains("housekeep", true) -> R.raw.cleaner
        jobTitle.contains("delivery", true) -> R.raw.delivery
        jobTitle.contains("waiter", true) || jobTitle.contains("server", true) -> R.raw.waiter
        jobTitle.contains("painter", true) || jobTitle.contains("paint", true) -> R.raw.painter
        jobTitle.contains("electric", true) -> R.raw.driver
        else -> null // Return null for unmatched jobs - will show company icon
    }
}

// NOTE: getTimeAgo() removed - use DateTimeUtils.formatRelativeTime() instead
// Import: import com.example.dutype.utils.DateTimeUtils

// =============================================================================
// HELPER FUNCTIONS FOR JobListingSummary OVERLOAD
// =============================================================================

/**
 * Format pay display for JobListingSummary
 */
private fun formatSummaryPayDisplay(payAmount: String, payType: String): Pair<String, String> {
    val trimmedAmount = payAmount.trim()
    val formattedPay = when {
        payType.contains("task", true) || payType.contains("delivery", true) -> "₹$trimmedAmount/delivery"
        payType.contains("daily", true) || payType.contains("day", true) -> "₹$trimmedAmount/day"
        payType.contains("hour", true) -> "₹$trimmedAmount/hour"
        payType.contains("month", true) -> "₹$trimmedAmount/month"
        else -> "₹$trimmedAmount/day"
    }
    
    val suffix = when {
        payType.contains("task", true) || payType.contains("delivery", true) -> "paid after delivery"
        payType.contains("daily", true) || payType.contains("day", true) -> "paid after shift"
        payType.contains("hour", true) -> "paid hourly"
        payType.contains("month", true) -> "paid monthly"
        else -> "paid after shift"
    }
    
    return Pair(formattedPay, suffix)
}

/**
 * Format location with distance for JobListingSummary
 * Shows short location (area, city) + distance in km
 */
private fun formatSummaryLocationWithDistance(location: String, distance: Double?): String {
    // Extract short location - first 2 parts only (area, city)
    val shortLocation = location.split(",").take(2).joinToString(", ") { it.trim() }
    
    if (distance == null) return shortLocation
    
    return when {
        distance < 1.0 -> {
            val meters = (distance * 1000).toInt()
            "$shortLocation • ${meters}m away"
        }
        distance < 2.0 -> "$shortLocation • ${String.format("%.1f", distance)} km walkable"
        else -> "$shortLocation • ${String.format("%.1f", distance)} km away"
    }
}
