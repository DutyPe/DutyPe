package com.example.dutype.worker.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants
import com.parttime.dutype.R
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
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
    hasApplied: Boolean = false,
    onViewTrack: (String) -> Unit = {}
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
            .width(350.dp)
            .clickable {
                onViewTrack(jobCard.jobId) // Also call the callback if provided
                onCardClick(jobCard.jobId) 
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(13.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Header: Icon + Job Title + Company Name + Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Job icon (left side) - Increased size for better visibility
                JobLottieAnimation(
                    jobTitle = jobCard.title,
                    modifier = Modifier.size(56.dp)
                )

                // Job title and company (center)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = jobCard.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (jobCard.isFilled) Color(0xFF6B7280) else Color(0xFF111827)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = jobCard.employerName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action icons (right side)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite button
                    IconButton(
                        onClick = handleSaveClick,
                        modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                            imageVector = if (localIsSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (localIsSaved) "Remove from favorites" else "Add to favorites",
                            tint = if (localIsSaved) Color(0xFF059669) else Color(0xFF6B7280),
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pay info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // INR symbol
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111827)
                    )
                )
                
                Text(
                    text = jobCard.payInfo.getDisplayText(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111827)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Location row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${jobCard.location.area} ⦿ ${jobCard.location.distance} km away",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tags row with vacancy info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vacancy chip
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFF3B82F6).copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${jobCard.vacancies} vacancies",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                // Job tags
                if (jobCard.tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(jobCard.tags.take(2)) { tag ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFFF59E0B).copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tag.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Apply button with view count in same row
            /*Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View count with eye icon (left side)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Views",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${jobCard.viewCount}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Apply button (right side) - matching test button style with more black text
                Button(
                    onClick = { onApplyClick(jobCard.jobId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text(
                        text = "Apply",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    )
                }
            }*/
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
            tint = Color.Black,
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
                    hasApplied -> Color.Black
                    else -> Color.Black // Fully black background
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
                    color = when {
                        hasApplied && applicationStatus != null -> Color.White
                        hasApplied -> Color.White
                        else -> Color.White // White text for darker background
                    }
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

// Lottie animation component for job icons
@Composable
private fun JobLottieAnimation(
    jobTitle: String,
    modifier: Modifier = Modifier
) {
    val lottieFile = getJobLottieFile(jobTitle)
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieFile))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
            .fillMaxSize()
    )
}

// Helper function to get job Lottie file based on job title
private fun getJobLottieFile(jobTitle: String): Int {
    return when {
        jobTitle.contains("cook", ignoreCase = true) || jobTitle.contains("chef", ignoreCase = true) -> R.raw.cook
        jobTitle.contains("driver", ignoreCase = true) -> R.raw.driver
        jobTitle.contains("clean", ignoreCase = true) -> R.raw.cleaner
        jobTitle.contains("delivery", ignoreCase = true) -> R.raw.delivery
        jobTitle.contains("waiter", ignoreCase = true) || jobTitle.contains("server", ignoreCase = true) -> R.raw.waiter
        jobTitle.contains("farming", ignoreCase = true) || jobTitle.contains("agriculture", ignoreCase = true) -> R.raw.farming
        jobTitle.contains("painter", ignoreCase = true) || jobTitle.contains("paint", ignoreCase = true) -> R.raw.painter
        else -> R.raw.driver // Default animation
    }
}
