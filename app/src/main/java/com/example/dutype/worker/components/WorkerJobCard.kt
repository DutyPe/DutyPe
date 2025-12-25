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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import timber.log.Timber
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.worker.models.JobCardModel
import com.example.dutype.worker.models.LocationInfo
import com.example.dutype.worker.models.PayInfo
import com.example.dutype.worker.models.PayType
import com.example.dutype.worker.models.UrgencyLevel
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.utils.ValidationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCard(
    jobCard: JobCardModel,
    onApplyClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = false,
    hasApplied: Boolean = false,
    onViewTrack: (String) -> Unit = {},
    employerCreatedAt: Long = 0L,
    employerPaidOnTimePercentage: Int = 96
) {
    val context = LocalContext.current
    var localIsSaved by remember { mutableStateOf(isSaved) }
    var showAd by remember { mutableStateOf(false) }
    var pendingJobId by remember { mutableStateOf("") }

    // Calculate employer status
    val isNewEmployer = remember(employerCreatedAt) {
        if (employerCreatedAt == 0L) false
        else (System.currentTimeMillis() - employerCreatedAt) / (24 * 60 * 60 * 1000) <= 7
    }
    
    val isVerifiedEmployer = remember(employerCreatedAt) {
        if (employerCreatedAt == 0L) true
        else (System.currentTimeMillis() - employerCreatedAt) / (24 * 60 * 60 * 1000) > 7
    }

    // Check if urgent hiring
    val isUrgentHiring = remember(jobCard.hiringUrgency, jobCard.timeInfo) {
        jobCard.hiringUrgency.equals("TODAY", ignoreCase = true) ||
        jobCard.hiringUrgency.equals("IMMEDIATE", ignoreCase = true) ||
        jobCard.hiringUrgency.equals("URGENT", ignoreCase = true) ||
        jobCard.timeInfo.urgency == UrgencyLevel.IMMEDIATE || 
        jobCard.timeInfo.urgency == UrgencyLevel.URGENT
    }

    LaunchedEffect(isSaved) {
        localIsSaved = isSaved
    }
    
    val handleSaveClick = {
        localIsSaved = !localIsSaved
        onSaveClick(jobCard.jobId)
        Toast.makeText(context, if (localIsSaved) "Job saved!" else "Job removed!", Toast.LENGTH_SHORT).show()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onViewTrack(jobCard.jobId)
                pendingJobId = jobCard.jobId
                showAd = true
            }
            .border(0.5.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp) // Added more elevation
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
                // Job Lottie Animation Icon - Circular - Animation fills the circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    JobLottieAnimation(
                        jobTitle = jobCard.title,
                        modifier = Modifier.size(52.dp) // Animation fills the entire circle
                    )
                }

                // Title and Company - Same row alignment
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = ValidationUtils.capitalizeWords(jobCard.title),
                        style = AppTypography.cardTitle.copy(
                            color = if (jobCard.isFilled) Color(0xFF6B7280) else Color(0xFF111827),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = ValidationUtils.capitalizeWords(jobCard.employerName),
                        style = AppTypography.caption.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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

            // Row 2: Pay Info - Black text with smaller suffix
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "₹${jobCard.payInfo.getFormattedPay()}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF111827), // Black color
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = jobCard.payInfo.getPaymentSuffix(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF6B7280), // Gray color
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Employer Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isNewEmployer) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "New Employer",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                    Text("•", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    Text(
                        text = "Payment protected by ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "DutyPe",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                } else if (isVerifiedEmployer) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Verified Employer",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                    Text("•", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    Text(
                        text = "Paid on time $employerPaidOnTimePercentage%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 4: Location with distance
            Text(
                text = jobCard.location.getLocationWithDistance(),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 5: Tags and Apply Button - Compact layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tags row - compact with reduced spacing
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Vacancy chip
                    CompactChip(text = "${jobCard.vacancies} ${if (jobCard.vacancies == 1) "Vacancy" else "Vacancies"}")
                    
                    // Job type chip
                    if (jobCard.jobType.isNotEmpty()) {
                        CompactChip(text = jobCard.jobType)
                    }
                    
                    // Urgent Hiring chip
                    if (isUrgentHiring) {
                        CompactChip(
                            text = "Urgent Hiring",
                            backgroundColor = Color(0xFFFEF3C7),
                            borderColor = Color(0xFFFCD34D),
                            textColor = Color(0xFFD97706)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Apply Now button
                Button(
                    onClick = { if (!hasApplied) onApplyClick(jobCard.jobId) },
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

@Composable
private fun CompactChip(
    text: String,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = Color(0xFFE5E7EB),
    textColor: Color = Color(0xFF374151)
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp) // Increased padding for taller chips
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontSize = 11.sp
            ),
            maxLines = 1
        )
    }
}

// Extension function for PayInfo - Updated with black color and smaller "paid after" text
fun PayInfo.getFormattedPay(): String {
    val trimmedAmount = amount.trim()
    return when {
        type == PayType.PER_TASK || period.contains("delivery", true) || period.contains("task", true) -> 
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
        type == PayType.PER_TASK || period.contains("delivery", true) || period.contains("task", true) -> 
            "paid after delivery"
        type == PayType.DAILY -> "paid after shift"
        type == PayType.HOURLY -> "paid hourly"
        type == PayType.MONTHLY -> "paid monthly"
        else -> "paid after shift"
    }
}

// Extension function for LocationInfo - Updated format
fun LocationInfo.getLocationWithDistance(): String {
    val distanceValue = distance.replace("km", "").replace(" ", "").toDoubleOrNull()
    
    return when {
        distanceValue == null || distance == "N/A" -> getDisplayText()
        distanceValue < 1.0 -> {
            val formatted = String.format("%.1f", distanceValue)
            "${getDisplayText()} • ${formatted}km walkable"
        }
        else -> {
            val formatted = String.format("%.1f", distanceValue)
            "${getDisplayText()} • ${formatted} km away nearby"
        }
    }
}

@Composable
private fun JobLottieAnimation(jobTitle: String, modifier: Modifier = Modifier) {
    val lottieFile = getJobLottieFile(jobTitle)
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieFile))
    val progress by animateLottieCompositionAsState(composition = composition, iterations = LottieConstants.IterateForever)
    LottieAnimation(composition = composition, progress = { progress }, modifier = modifier.fillMaxSize())
}

private fun getJobLottieFile(jobTitle: String): Int {
    return when {
        jobTitle.contains("cook", true) || jobTitle.contains("chef", true) -> R.raw.cook
        jobTitle.contains("driver", true) -> R.raw.driver
        jobTitle.contains("clean", true) || jobTitle.contains("housekeep", true) -> R.raw.cleaner
        jobTitle.contains("delivery", true) -> R.raw.delivery
        jobTitle.contains("waiter", true) || jobTitle.contains("server", true) -> R.raw.waiter
        jobTitle.contains("painter", true) || jobTitle.contains("paint", true) -> R.raw.painter
        jobTitle.contains("electric", true) -> R.raw.driver
        else -> R.raw.driver
    }
}
