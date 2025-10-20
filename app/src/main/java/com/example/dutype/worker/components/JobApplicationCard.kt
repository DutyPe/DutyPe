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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper functions for status display
private fun getStatusIcon(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "⏳"
        ApplicationStatus.UNDER_REVIEW -> "👀"
        ApplicationStatus.ACCEPTED -> "🎉"
        ApplicationStatus.REJECTED -> "❌"
    }
}

private fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending Review"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.REJECTED -> "Not Selected"
    }
}

private fun getStatusColor(status: ApplicationStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        ApplicationStatus.PENDING -> androidx.compose.ui.graphics.Color(0xFFF59E0B) // Amber
        ApplicationStatus.UNDER_REVIEW -> androidx.compose.ui.graphics.Color(0xFF3B82F6) // Blue
        ApplicationStatus.ACCEPTED -> androidx.compose.ui.graphics.Color(0xFF10B981) // Green
        ApplicationStatus.REJECTED -> androidx.compose.ui.graphics.Color(0xFFEF4444) // Red
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
    modifier: Modifier = Modifier
) {
    
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
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
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    
                    Text(
                        text = application.companyName,
                        style = MaterialTheme.typography.bodyMedium.copy(
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
                        style = MaterialTheme.typography.bodySmall.copy(
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
                        style = MaterialTheme.typography.bodySmall.copy(
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
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = application.payInfo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Medium
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
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9CA3AF)
                    )
                )
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onCardClick(application) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "View Details",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
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
            statusText = if (status == ApplicationStatus.PENDING) "In Progress" else "Completed",
            isCompleted = status != ApplicationStatus.PENDING,
            isCurrent = status == ApplicationStatus.PENDING
        ),
        TimelineStepData(
            stepNumber = 3,
            label = "Under Review",
            statusText = when {
                status == ApplicationStatus.UNDER_REVIEW -> "In Progress"
                status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED -> "Completed"
                else -> "Pending"
            },
            isCompleted = status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED,
            isCurrent = status == ApplicationStatus.UNDER_REVIEW
        ),
        TimelineStepData(
            stepNumber = 4,
            label = if (status == ApplicationStatus.ACCEPTED) "Selected" else if (status == ApplicationStatus.REJECTED) "Rejected" else "Decision",
            statusText = if (status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED) "Completed" else "Pending",
            isCompleted = status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED,
            isCurrent = false,
            isSuccess = status == ApplicationStatus.ACCEPTED,
            isFailure = status == ApplicationStatus.REJECTED
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
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF9CA3AF)
                            ),
                            textAlign = TextAlign.Center
                        )
                        
                        // Step title
                        Text(
                            text = stepData.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Status text
                        Text(
                            text = stepData.statusText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    stepData.isSuccess -> Color(0xFF10B981)
                                    stepData.isFailure -> Color(0xFFDC2626)
                                    stepData.isCurrent -> Color(0xFF3B82F6)
                                    stepData.isCompleted -> Color(0xFF10B981)
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
                        if (status != ApplicationStatus.PENDING) Color(0xFF10B981)
                        else Color(0xFFE5E7EB)
                    }
                    1 -> {
                        if (status == ApplicationStatus.UNDER_REVIEW || status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED) {
                            Color(0xFF10B981)
                        } else if (status == ApplicationStatus.PENDING) {
                            Color(0xFF3B82F6)
                        } else {
                            Color(0xFFE5E7EB)
                        }
                    }
                    2 -> {
                        if (status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED) {
                            Color(0xFF10B981)
                        } else if (status == ApplicationStatus.UNDER_REVIEW) {
                            Color(0xFF3B82F6)
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
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9CA3AF)
            ),
            textAlign = TextAlign.Center
        )
        
        // Step title
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Status text
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    isSuccess -> Color(0xFF10B981)
                    isFailure -> Color(0xFFDC2626)
                    isCurrent -> Color(0xFF3B82F6)
                    isCompleted -> Color(0xFF10B981)
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
                        .background(Color(0xFF10B981), CircleShape),
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
                        .background(Color(0xFF10B981), CircleShape),
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
                            color = Color(0xFF3B82F6),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFF3B82F6).copy(alpha = blinkAlpha), 
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
                        .background(Color(0xFF10B981), CircleShape),
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
                        .background(Color(0xFF10B981), CircleShape),
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
                            color = Color(0xFF3B82F6),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFF3B82F6).copy(alpha = blinkAlpha), 
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
                style = MaterialTheme.typography.bodySmall.copy(
                    color = getStatusColor(status),
                    fontWeight = FontWeight.Medium
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
