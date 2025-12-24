package com.example.dutype.worker.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.ui.theme.AppTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper functions for status display
private fun getStatusIcon(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "⏳"
        ApplicationStatus.UNDER_REVIEW -> "👀"
        ApplicationStatus.ACCEPTED -> "🎉"
        ApplicationStatus.COMPLETED -> "✅"
        ApplicationStatus.REJECTED -> "❌"
        ApplicationStatus.WITHDRAWN -> "↩️"
    }
}

private fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending Review"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.COMPLETED -> "Completed"
        ApplicationStatus.REJECTED -> "Not Selected"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
    }
}

private fun getStatusColor(status: ApplicationStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        ApplicationStatus.PENDING -> androidx.compose.ui.graphics.Color(0xFFF59E0B) // Amber
        ApplicationStatus.UNDER_REVIEW -> androidx.compose.ui.graphics.Color(0xFF1F2937) // Blue
        ApplicationStatus.ACCEPTED -> androidx.compose.ui.graphics.Color(0xFF1F2937) // Green
        ApplicationStatus.COMPLETED -> androidx.compose.ui.graphics.Color(0xFF7C3AED) // Purple
        ApplicationStatus.REJECTED -> androidx.compose.ui.graphics.Color(0xFFEF4444) // Red
        ApplicationStatus.WITHDRAWN -> androidx.compose.ui.graphics.Color(0xFF6B7280) // Gray
    }
}

/**
 * Professional Job Application Card
 * Displays job application with status and actions
 */
@Composable
fun JobApplicationCard(
    application: JobApplication,
    onCardClick: (JobApplication) -> Unit,
    onWithdrawClick: ((JobApplication) -> Unit)? = null,
    onRateClick: ((JobApplication) -> Unit)? = null,
    hasAlreadyRated: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Can withdraw only if status is PENDING or UNDER_REVIEW
    val canWithdraw = application.status == ApplicationStatus.PENDING || 
                      application.status == ApplicationStatus.UNDER_REVIEW
    
    // Can rate only if status is COMPLETED and hasn't rated yet
    val canRate = application.status == ApplicationStatus.COMPLETED && !hasAlreadyRated && onRateClick != null
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick(application) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (application.isFilled) Color.White.copy(alpha = 0.6f) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Application Timeline at the top
            ApplicationTimeline(
                status = application.status,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Job title and company
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = application.jobTitle,
                        style = AppTypography.cardTitle.copy(
                            color = if (application.isFilled) Color(0xFF6B7280) else Color(0xFF111827)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Filled status indicator
                    if (application.isFilled) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFF6B7280).copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Position Filled",
                                style = AppTypography.status.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                        }
                    }
                    
                    Text(
                        text = application.companyName,
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Status badge
                StatusBadge(
                    status = application.status,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Job details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = application.jobLocation,
                        style = AppTypography.caption.copy(
                            color = Color(0xFF6B7280)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Job type
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = application.jobType,
                        style = AppTypography.caption.copy(
                            color = Color(0xFF6B7280)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pay info
            if (application.payInfo.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = application.payInfo,
                        style = AppTypography.labelMedium.copy(
                            color = Color(0xFF1F2937)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Applied date and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Applied ${formatDate(application.appliedAt)}",
                    style = AppTypography.caption.copy(
                        color = Color(0xFF9CA3AF)
                    )
                )
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Withdraw button - only show if can withdraw
                    if (canWithdraw && onWithdrawClick != null) {
                        Button(
                            onClick = { onWithdrawClick(application) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Withdraw",
                                style = AppTypography.buttonSmall.copy(
                                    color = Color(0xFFEF4444)
                                )
                            )
                        }
                    }
                    
                    // Rate Employer button - only show if job is completed and hasn't rated
                    if (canRate) {
                        Button(
                            onClick = { onRateClick?.invoke(application) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Rate Employer",
                                style = AppTypography.buttonSmall.copy(
                                    color = Color.White
                                )
                            )
                        }
                    }
                    
                    Button(
                        onClick = { onCardClick(application) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F2937)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "View Details",
                            style = AppTypography.buttonSmall.copy(
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
    
}

@Composable
private fun ApplicationTimeline(
    status: ApplicationStatus,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        TimelineStepData(
            stepNumber = 1,
            label = "Applied",
            statusText = "Completed",
            isCompleted = true,
            isCurrent = false
        ),
        TimelineStepData(
            stepNumber = 2,
            label = "Pending",
            statusText = if (status == ApplicationStatus.PENDING) "In Progress" else if (status == ApplicationStatus.WITHDRAWN) "Withdrawn" else "Completed",
            isCompleted = status != ApplicationStatus.PENDING && status != ApplicationStatus.WITHDRAWN,
            isCurrent = status == ApplicationStatus.PENDING,
            isFailure = status == ApplicationStatus.WITHDRAWN
        ),
        TimelineStepData(
            stepNumber = 3,
            label = "Under Review",
            statusText = when {
                status == ApplicationStatus.UNDER_REVIEW -> "In Progress"
                status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED || status == ApplicationStatus.COMPLETED -> "Completed"
                status == ApplicationStatus.WITHDRAWN -> "Cancelled"
                else -> "Pending"
            },
            isCompleted = status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED || status == ApplicationStatus.COMPLETED,
            isCurrent = status == ApplicationStatus.UNDER_REVIEW,
            isFailure = status == ApplicationStatus.WITHDRAWN
        ),
        TimelineStepData(
            stepNumber = 4,
            label = when (status) {
                ApplicationStatus.ACCEPTED -> "Hired"
                ApplicationStatus.COMPLETED -> "Completed"
                ApplicationStatus.REJECTED -> "Rejected"
                ApplicationStatus.WITHDRAWN -> "Withdrawn"
                else -> "Decision"
            },
            statusText = when (status) {
                ApplicationStatus.ACCEPTED -> "In Progress"
                ApplicationStatus.COMPLETED -> "Completed"
                ApplicationStatus.REJECTED -> "Completed"
                ApplicationStatus.WITHDRAWN -> "Cancelled"
                else -> "Pending"
            },
            isCompleted = status == ApplicationStatus.COMPLETED || status == ApplicationStatus.REJECTED,
            isCurrent = status == ApplicationStatus.ACCEPTED,
            isSuccess = status == ApplicationStatus.COMPLETED,
            isFailure = status == ApplicationStatus.REJECTED || status == ApplicationStatus.WITHDRAWN
        )
    )
    
    Box(
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        // Stepper content
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            steps.forEachIndexed { index, stepData ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Step indicator
                        StepIndicatorDot(
                            isSelected = stepData.isCurrent,
                            isCompleted = stepData.isCompleted,
                            isSuccess = stepData.isSuccess,
                            isFailure = stepData.isFailure
                        )
                        
                        // Step number
                        Text(
                            text = "STEP ${stepData.stepNumber}",
                            style = AppTypography.labelSmall.copy(
                                color = Color(0xFF9CA3AF)
                            ),
                            textAlign = TextAlign.Center
                        )
                        
                        // Step title
                        Text(
                            text = stepData.label,
                            style = AppTypography.labelMedium.copy(
                                color = Color(0xFF1F2937)
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Status text
                        Text(
                            text = stepData.statusText,
                            style = AppTypography.labelSmall.copy(
                                color = when {
                                    stepData.isSuccess -> Color(0xFF1F2937)
                                    stepData.isFailure -> Color(0xFFDC2626)
                                    stepData.isCurrent -> Color(0xFF1F2937)
                                    stepData.isCompleted -> Color(0xFF1F2937)
                                    else -> Color(0xFF9CA3AF)
                                }
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        
        // Horizontal connectors positioned at indicator center level
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .offset(y = 0.dp)
                .zIndex(-1f)
        ) {
            val width = size.width
            val itemWidth = width / steps.size
            val lineWidth = 2.dp.toPx()
            val yOffset = size.height / 2

            // Draw connecting lines between indicators
            repeat(steps.size - 1) { index ->
                val startX = (index + 1) * itemWidth - itemWidth / 2
                val endX = (index + 1) * itemWidth + itemWidth / 2
                
                val lineColor = when (index) {
                    0 -> {
                        if (status != ApplicationStatus.PENDING) Color(0xFF1F2937)
                        else Color(0xFFE5E7EB)
                    }
                    1 -> {
                        if (status == ApplicationStatus.UNDER_REVIEW || status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED) {
                            Color(0xFF1F2937)
                        } else if (status == ApplicationStatus.PENDING) {
                            Color(0xFF1F2937)
                        } else {
                            Color(0xFFE5E7EB)
                        }
                    }
                    2 -> {
                        if (status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED) {
                            Color(0xFF1F2937)
                        } else if (status == ApplicationStatus.UNDER_REVIEW) {
                            Color(0xFF1F2937)
                        } else {
                            Color(0xFFE5E7EB)
                        }
                    }
                    else -> Color(0xFFE5E7EB)
                }
                
                drawLine(
                    color = lineColor,
                    start = Offset(startX, yOffset),
                    end = Offset(endX, yOffset),
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private data class TimelineStepData(
    val stepNumber: Int,
    val label: String,
    val statusText: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
    val isSuccess: Boolean = false,
    val isFailure: Boolean = false
)

@Composable
private fun TimelineStep(
    stepNumber: Int,
    label: String,
    statusText: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    isSuccess: Boolean = false,
    isFailure: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Step indicator
        TimelineIndicator(
            isCompleted = isCompleted,
            isCurrent = isCurrent,
            isSuccess = isSuccess,
            isFailure = isFailure
        )
        
        // Step number
        Text(
            text = "STEP $stepNumber",
            style = AppTypography.labelSmall.copy(
                color = Color(0xFF9CA3AF)
            ),
            textAlign = TextAlign.Center
        )
        
        // Step title
        Text(
            text = label,
            style = AppTypography.labelMedium.copy(
                color = Color(0xFF1F2937)
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Status text
        Text(
            text = statusText,
            style = AppTypography.labelSmall.copy(
                color = when {
                    isSuccess -> Color(0xFF1F2937)
                    isFailure -> Color(0xFFDC2626)
                    isCurrent -> Color(0xFF1F2937)
                    isCompleted -> Color(0xFF1F2937)
                    else -> Color(0xFF9CA3AF)
                }
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StepIndicatorDot(
    isSelected: Boolean,
    isCompleted: Boolean,
    isSuccess: Boolean = false,
    isFailure: Boolean = false
) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isCompleted && isSuccess -> {
                // Success - Green checkmark circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF1F2937), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            isCompleted && isFailure -> {
                // Failure - Red X circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFDC2626), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            isCompleted -> {
                // Regular completed - Green checkmark circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF1F2937), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            isSelected -> {
                // Current - Blue circle with outline and center dot
                val infiniteTransition = rememberInfiniteTransition(label = "blink")
                val blinkAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "blink"
                )
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFEBF4FF), CircleShape)
                        .border(
                            width = 2.dp,
                            color = Color(0xFF1F2937),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFF1F2937).copy(alpha = blinkAlpha), 
                                CircleShape
                            )
                    )
                }
            }
            else -> {
                // Pending - Light blue circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFEBF4FF), CircleShape)
                )
            }
        }
    }
}


@Composable
private fun TimelineIndicator(
    isCompleted: Boolean,
    isCurrent: Boolean,
    isSuccess: Boolean = false,
    isFailure: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )
    
    Box(
        modifier = Modifier
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isCompleted && isSuccess -> {
                // Success - Green checkmark circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF1F2937), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            isCompleted && isFailure -> {
                // Failure - Red X circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFDC2626), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            isCompleted -> {
                // Regular completed - Green checkmark circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF1F2937), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            isCurrent -> {
                // Current - Blue circle with outline and center dot
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFEBF4FF), CircleShape)
                        .border(
                            width = 2.dp,
                            color = Color(0xFF1F2937),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFF1F2937).copy(alpha = blinkAlpha), 
                                CircleShape
                            )
                    )
                }
            }
            else -> {
                // Pending - Light blue circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFEBF4FF), CircleShape)
                )
            }
        }
    }
}


@Composable
private fun StatusBadge(
    status: ApplicationStatus,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = getStatusColor(status).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = getStatusIcon(status),
                fontSize = 12.sp
            )
            Text(
                text = getStatusDisplayName(status),
                style = AppTypography.status.copy(
                    color = getStatusColor(status)
                )
            )
        }
    }
}


private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60 * 1000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(date)
        }
    }
}
