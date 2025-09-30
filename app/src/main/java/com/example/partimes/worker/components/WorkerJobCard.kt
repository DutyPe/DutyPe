package com.example.partimes.worker.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
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
import com.example.partimes.worker.models.JobCardModel
import com.example.partimes.worker.models.JobTag
import com.example.partimes.worker.models.LocationInfo
import com.example.partimes.worker.models.PayInfo
import com.example.partimes.worker.models.TagType
import com.example.partimes.worker.models.TimeInfo
import com.example.partimes.worker.models.UrgencyLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCard(
    jobCard: JobCardModel,
    onApplyClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSaved by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                    // Share Button
                    IconButton(
                        onClick = { shareJobToWhatsApp(context, jobCard) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Job",
                            tint = Color(0xFF1E40AF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // (Removed) header favorite icon per request

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
                isSaved = isSaved,
                onApplyClick = onApplyClick,
                onSaveClick = {
                    isSaved = !isSaved
                    onSaveClick(jobCard.jobId)
                }
            )
        }
    }
}

private fun shareJobToWhatsApp(context: Context, jobCard: JobCardModel) {
    val shareText = jobCard.getShareableText()

    // Create WhatsApp sharing intent
    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        setPackage("com.whatsapp")
    }

    // Check if WhatsApp is installed
    if (whatsappIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(whatsappIntent)
    } else {
        // Fallback to general sharing if WhatsApp is not installed
        val generalIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Job Opportunity: ${jobCard.title}")
        }
        context.startActivity(Intent.createChooser(generalIntent, "Share Job via"))
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
            text = timeInfo.postedTime,
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

            Text(
                text = "• ${payInfo.type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF1E40AF),
                    fontWeight = FontWeight.Medium
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
    onApplyClick: (String) -> Unit,
    onSaveClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Apply Button (Primary CTA)
        Button(
            onClick = { onApplyClick(jobId) },
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E40AF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Apply",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
        }

        // Save Button (Secondary)
        OutlinedButton(
            onClick = { onSaveClick(jobId) },
            modifier = Modifier
                .width(90.dp)
                .height(44.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSaved) Color(0xFF1E40AF).copy(alpha = 0.1f) else Color.Transparent,
                contentColor = if (isSaved) Color(0xFF1E40AF) else Color(0xFF6B7280)
            ),
            border = BorderStroke(
                1.dp,
                if (isSaved) Color(0xFF1E40AF) else Color(0xFFD1D5DB)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Save",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}