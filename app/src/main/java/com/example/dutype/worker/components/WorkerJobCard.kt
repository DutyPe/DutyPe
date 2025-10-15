package com.example.dutype.worker.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.dutype.worker.models.JobCardModel
import com.example.dutype.worker.models.JobTag
import com.example.dutype.worker.models.LocationInfo
import com.example.dutype.worker.models.PayInfo
import com.example.dutype.worker.models.TagType
import com.example.dutype.worker.models.TimeInfo
import com.example.dutype.worker.models.UrgencyLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCard(
    jobCard: JobCardModel,
    onApplyClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = false,
    hasApplied: Boolean = false
) {
    val context = LocalContext.current

    // Use the actual database state as the source of truth
    // This ensures state persists across navigation and app refreshes
    var localIsSaved by remember { mutableStateOf(isSaved) }

    // Debug logging
    LaunchedEffect(isSaved) {
        println("🔍 DEBUG JobCard: Job ${jobCard.jobId} (${jobCard.title}) isSaved: $isSaved")
        localIsSaved = isSaved
    }
    
    // Handle save/unsave with immediate UI feedback and proper state management
    val handleSaveClick = {
        // Toggle local state for immediate UI feedback
        localIsSaved = !localIsSaved
        
        // Call the parent's save handler (this will update the database)
        onSaveClick(jobCard.jobId)
        
        // Show toast message based on the new state
        val message = if (localIsSaved) {
            "Job saved to favorites!"
        } else {
            "Job removed from favorites!"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    Card(
        modifier = modifier
            .width(320.dp)
            .height(240.dp)
            .clickable { onCardClick(jobCard.jobId) },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Job Title + Employer Name + Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${jobCard.title} ",
//                        ${jobCard.employerName}
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF111827)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Direct Share Button - Opens system share sheet immediately
                    IconButton(
                        onClick = { shareJobDirectly(context, jobCard) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color(0xFF1E40AF).copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Job",
                                tint = Color(0xFF1E40AF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Time Info with urgency indicator
                    TimeInfoBadge(
                        timeInfo = jobCard.timeInfo
                    )
                }
            }

            // Pay & Type (highlighted)
            PayInfoCard(payInfo = jobCard.payInfo)

            // Location with distance
            LocationRow(locationInfo = jobCard.location)

            // Tags (Quick Indicators)
            if (jobCard.tags.isNotEmpty()) {
                TagsRow(
                    tags = jobCard.tags,
                    isVerifiedEmployer = jobCard.isVerifiedEmployer
                )
            }

            // Action Buttons (save/apply live here)
            ActionButtonsRow(
                jobId = jobCard.jobId,
                isSaved = localIsSaved,
                hasApplied = hasApplied,
                applicationStatus = null, // TODO: Pass actual application status
                onApplyClick = onApplyClick,
                onSaveClick = handleSaveClick
            )
        }
    }
}

// Direct sharing function that immediately opens system share sheet
private fun shareJobDirectly(context: Context, jobCard: JobCardModel) {
    val shareText = getEnhancedShareText(jobCard)
    val subject = "Job Opportunity: ${jobCard.title} at ${jobCard.employerName}"

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Share Job via")

    try {
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share job", Toast.LENGTH_SHORT).show()
    }
}

// Enhanced share text formatting function
private fun getEnhancedShareText(jobCard: JobCardModel): String {
    return buildString {
        appendLine("🎯 *JOB OPPORTUNITY*")
        appendLine()
        appendLine("💼 *${jobCard.title}*")
        appendLine("🏢 at *${jobCard.employerName}*")
        if (jobCard.isVerifiedEmployer) appendLine("✅ Verified Employer")
        appendLine()

        appendLine("💰 *Salary:* ${jobCard.payInfo.getTypeEmoji()} ${jobCard.payInfo.getDisplayText()}")
        appendLine("📍 *Location:* ${jobCard.location.getDisplayText()}")
        if (jobCard.location.getDistanceText().isNotEmpty()) {
            appendLine("🚗 *Distance:* ${jobCard.location.getDistanceText()}")
        }
        appendLine()

        if (jobCard.tags.isNotEmpty()) {
            appendLine("✨ *Highlights:*")
            jobCard.tags.take(4).forEach { tag ->
                appendLine("${tag.emoji} ${tag.text}")
            }
            appendLine()
        }

        if (jobCard.description.isNotEmpty()) {
            appendLine("📝 *Job Description:*")
            val desc = if (jobCard.description.length > 150) {
                "${jobCard.description.take(150)}..."
            } else {
                jobCard.description
            }
            appendLine(desc)
            appendLine()
        }

        if (jobCard.requirements.isNotEmpty()) {
            appendLine("📋 *Requirements:*")
            jobCard.requirements.take(3).forEach { req ->
                appendLine("• $req")
            }
            if (jobCard.requirements.size > 3) {
                appendLine("• And ${jobCard.requirements.size - 3} more...")
            }
            appendLine()
        }

        if (jobCard.phoneNumber.isNotEmpty()) {
            appendLine("📞 *Contact:* ${jobCard.phoneNumber}")
            appendLine()
        }

        appendLine("🚀 *Apply now through DutyPe App!*")
        appendLine("Download: bit.ly/Dutype-app")
        appendLine()
        appendLine("⏰ Posted: ${jobCard.timeInfo.getRelativeTime()}")
        if (jobCard.applicationDeadline != null) {
            appendLine("📅 Deadline: ${jobCard.applicationDeadline}")
        }
    }
}

@Composable
private fun TimeInfoBadge(
    timeInfo: TimeInfo,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (timeInfo.urgency) {
        UrgencyLevel.IMMEDIATE -> Color(0xFFDC2626) to Color.White
        UrgencyLevel.URGENT -> Color(0xFFEA580C) to Color.White
        UrgencyLevel.NORMAL -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = timeInfo.getRelativeTime(),
            style = MaterialTheme.typography.bodySmall.copy(
                color = textColor,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun PayInfoCard(
    payInfo: PayInfo
) {
    Card(
//        colors = CardDefaults.cardColors(
//            containerColor = Color(0xFF90D5FF).copy(alpha = 0.08f)
//        ),
//        shape = RoundedCornerShape(10.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = payInfo.getTypeEmoji(),
                fontSize = 20.sp
            )

            Text(
                text = payInfo.getDisplayText(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E40AF),
                    fontSize = 16.sp
                )
            )
        }
    }
}

@Composable
private fun LocationRow(
    locationInfo: LocationInfo
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location",
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = locationInfo.getDisplayText(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF374151),
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = locationInfo.getDistanceText(),
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF6B7280)
            )
        )
    }
}

@Composable
private fun TagsRow(
    tags: List<JobTag>,
    isVerifiedEmployer: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        tags.take(3).forEach { tag ->
            TagChip(tag = tag)
        }

        if (tags.size > 3) {
            TagChip(
                tag = JobTag(
                    "+${tags.size - 3} more",
                    "➕",
                    TagType.BENEFIT
                )
            )
        }
    }
}

@Composable
private fun TagChip(
    tag: JobTag
) {
    val backgroundColor = when (tag.type) {
        TagType.VERIFICATION -> Color(0xFFDCFCE7)
        TagType.URGENCY -> Color(0xFFFEE2E2)
        TagType.BENEFIT -> Color(0xFFE0F2FE)
        TagType.SCHEDULE -> Color(0xFFFEF3C7)
    }

    val textColor = when (tag.type) {
        TagType.VERIFICATION -> Color(0xFF059669)
        TagType.URGENCY -> Color(0xFFDC2626)
        TagType.BENEFIT -> Color(0xFF0369A1)
        TagType.SCHEDULE -> Color(0xFFCA8A04)
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = tag.emoji,
                fontSize = 10.sp
            )
            Text(
                text = tag.text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    jobId: String,
    isSaved: Boolean,
    hasApplied: Boolean = false,
    applicationStatus: String? = null,
    onApplyClick: (String) -> Unit,
    onSaveClick: () -> Unit,
    onQuickApply: ((String) -> Unit)? = null // Add quick apply callback
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Apply Button (Primary CTA) - Enhanced with quick apply
        Button(
            onClick = {
                if (hasApplied) {
                    // Already applied - just show toast
                    Toast.makeText(context, "You have already applied to this job", Toast.LENGTH_SHORT).show()
                } else {
                    // Check if quick apply is available and user wants to use it
                    if (onQuickApply != null) {
                        // Show quick apply dialog or directly apply
                        onQuickApply(jobId)
                    } else {
                        // Navigate to full application screen
                        onApplyClick(jobId)
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    hasApplied && applicationStatus != null -> {
                        when (applicationStatus) {
                            "PENDING" -> Color(0xFFF59E0B)
                            "REVIEWED", "UNDER_REVIEW" -> Color(0xFF3B82F6)
                            "ACCEPTED" -> Color(0xFF10B981)
                            "REJECTED" -> Color(0xFFEF4444)
                            else -> Color(0xFF6B7280)
                        }
                    }
                    hasApplied -> Color(0xFF6B7280)
                    else -> Color(0xFF3B82F6) // Using the same color as location and notification icons
                }
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = when {
                    hasApplied && applicationStatus != null -> {
                        when (applicationStatus) {
                            "PENDING" -> "Pending"
                            "REVIEWED", "UNDER_REVIEW" -> "Under Review"
                            "ACCEPTED" -> "Accepted"
                            "REJECTED" -> "Rejected"
                            else -> "Applied"
                        }
                    }
                    hasApplied -> "Applied"
                    else -> "Apply"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            )
        }

        // Save/Bookmark Button (Secondary CTA)
        IconButton(
            onClick = onSaveClick,
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (isSaved) Color(0xFFDCFCE7) else Color(0xFFF3F4F6),
                    RoundedCornerShape(8.dp)
                )
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isSaved) "Remove from favorites" else "Add to favorites",
                tint = if (isSaved) Color(0xFF059669) else Color(0xFF6B7280),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
