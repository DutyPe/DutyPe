package com.example.dutype.worker.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
private fun getStatusIcon(status: ApplicationStatus, isFilled: Boolean = false): String {
    // If job is filled and worker wasn't accepted, show filled icon
    if (isFilled && status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.COMPLETED) {
        return "🚫"
    }
    return when (status) {
        ApplicationStatus.PENDING -> "⏳"
        ApplicationStatus.UNDER_REVIEW -> "👀"
        ApplicationStatus.ACCEPTED -> "🎉"
        ApplicationStatus.COMPLETED -> "✅"
        ApplicationStatus.REJECTED -> "❌"
        ApplicationStatus.WITHDRAWN -> "↩️"
    }
}

private fun getStatusDisplayName(status: ApplicationStatus, isFilled: Boolean = false): String {
    // If job is filled and worker wasn't accepted, show vacancy filled
    if (isFilled && status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.COMPLETED) {
        return "Vacancy Filled"
    }
    return when (status) {
        ApplicationStatus.PENDING -> "Pending Review"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.COMPLETED -> "Completed"
        ApplicationStatus.REJECTED -> "Not Selected"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
    }
}

private fun getStatusColor(status: ApplicationStatus, isFilled: Boolean = false): Color {
    // If job is filled and worker wasn't accepted, show gray
    if (isFilled && status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.COMPLETED) {
        return Color(0xFF6B7280) // Gray for vacancy filled
    }
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFF59E0B) // Amber
        ApplicationStatus.UNDER_REVIEW -> Color(0xFF3B82F6) // Blue
        ApplicationStatus.ACCEPTED -> Color(0xFF10B981) // Green
        ApplicationStatus.COMPLETED -> Color(0xFF7C3AED) // Purple
        ApplicationStatus.REJECTED -> Color(0xFFEF4444) // Red
        ApplicationStatus.WITHDRAWN -> Color(0xFF6B7280) // Gray
    }
}

/**
 * Professional Job Application Card
 * Displays job application with status and actions
 * For completed jobs, shows a clean rating section
 */
@Composable
fun JobApplicationCard(
    application: JobApplication,
    onCardClick: (JobApplication) -> Unit,
    onWithdrawClick: ((JobApplication) -> Unit)? = null,
    onRateClick: ((JobApplication) -> Unit)? = null,
    onStartWorkClick: ((JobApplication) -> Unit)? = null,
    hasAlreadyRated: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Can withdraw only if status is PENDING or UNDER_REVIEW
    val canWithdraw = application.status == ApplicationStatus.PENDING || 
                      application.status == ApplicationStatus.UNDER_REVIEW
    
    // Can start work only if status is ACCEPTED
    val canStartWork = application.status == ApplicationStatus.ACCEPTED && onStartWorkClick != null
    
    // Can rate only if status is COMPLETED and hasn't rated yet
    val canRate = application.status == ApplicationStatus.COMPLETED && !hasAlreadyRated && onRateClick != null
    val isCompleted = application.status == ApplicationStatus.COMPLETED
    
    // State for rating section expansion
    var isRatingSectionExpanded by remember { mutableStateOf(canRate) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick(application) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column {
            // Main card content
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Application Timeline at the top
                ApplicationTimeline(
                    status = application.status,
                    modifier = Modifier.fillMaxWidth(),
                    isFilled = application.isFilled
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
                                color = Color(0xFF111827)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Filled status indicator
                        if (application.isFilled) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFFF59E0B).copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Position Filled",
                                    style = AppTypography.status.copy(
                                        color = Color(0xFFF59E0B)
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
                        modifier = Modifier.padding(start = 8.dp),
                        isFilled = application.isFilled
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
                
                // Pay info and Applied time in same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                style = AppTypography.labelMedium.copy(
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    
                    // Applied time
                    Text(
                        text = "Applied ${formatDate(application.appliedAt)}",
                        style = AppTypography.caption.copy(
                            color = Color(0xFF9CA3AF)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Action buttons row - separate row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Work button - only show if accepted
                        if (canStartWork) {
                            Button(
                                onClick = { onStartWorkClick?.invoke(application) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🔐 Start Work",
                                    style = AppTypography.buttonSmall.copy(
                                        color = Color.White
                                    )
                                )
                            }
                        }
                        
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
            
            // Rating Section for Completed Jobs - Clean separate section
            if (isCompleted) {
                HorizontalDivider(
                    color = Color(0xFFF3F4F6),
                    thickness = 1.dp
                )
                
                // Rating section header (clickable to expand/collapse)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRatingSectionExpanded = !isRatingSectionExpanded }
                        .background(
                            if (canRate) Color(0xFFFEF3C7).copy(alpha = 0.5f) else Color(0xFFF9FAFB)
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (canRate) Color(0xFFF59E0B) else Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (canRate) "Rate this employer" else "Rating submitted",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (canRate) Color(0xFFB45309) else Color(0xFF059669)
                            )
                        )
                    }
                    
                    Icon(
                        imageVector = if (isRatingSectionExpanded) 
                            Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Expandable rating content
                AnimatedVisibility(
                    visible = isRatingSectionExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (canRate) Color(0xFFFEF3C7).copy(alpha = 0.3f) else Color(0xFFF0FDF4)
                            )
                            .padding(20.dp)
                    ) {
                        if (canRate) {
                            // Show rating prompt
                            Text(
                                text = "How was your experience working with ${application.companyName}?",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF78350F)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Star preview (non-interactive)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(5) {
                                    Icon(
                                        imageVector = Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Rate button
                            Button(
                                onClick = { onRateClick?.invoke(application) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Rate Employer",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        } else {
                            // Show "already rated" message
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Thank you for rating ${application.companyName}!",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF059669)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationTimeline(
    status: ApplicationStatus,
    modifier: Modifier = Modifier,
    isFilled: Boolean = false
) {
    // If job is filled and worker wasn't accepted, show special timeline
    val effectiveStatus = if (isFilled && status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.COMPLETED) {
        ApplicationStatus.REJECTED // Treat as rejected for timeline purposes
    } else {
        status
    }
    
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
            statusText = if (effectiveStatus == ApplicationStatus.PENDING) "In Progress" else if (effectiveStatus == ApplicationStatus.WITHDRAWN) "Withdrawn" else "Completed",
            isCompleted = effectiveStatus != ApplicationStatus.PENDING && effectiveStatus != ApplicationStatus.WITHDRAWN,
            isCurrent = effectiveStatus == ApplicationStatus.PENDING && !isFilled,
            isFailure = effectiveStatus == ApplicationStatus.WITHDRAWN
        ),
        TimelineStepData(
            stepNumber = 3,
            label = "Under Review",
            statusText = when {
                isFilled && status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.COMPLETED -> "Vacancy Filled"
                effectiveStatus == ApplicationStatus.UNDER_REVIEW -> "In Progress"
                effectiveStatus == ApplicationStatus.ACCEPTED || effectiveStatus == ApplicationStatus.REJECTED || effectiveStatus == ApplicationStatus.COMPLETED -> "Completed"
                effectiveStatus == ApplicationStatus.WITHDRAWN -> "Cancelled"
                else -> "Pending"
            },
            isCompleted = effectiveStatus == ApplicationStatus.ACCEPTED || effectiveStatus == ApplicationStatus.REJECTED || effectiveStatus == ApplicationStatus.COMPLETED || isFilled,
            isCurrent = effectiveStatus == ApplicationStatus.UNDER_REVIEW && !isFilled,
            isFailure = effectiveStatus == ApplicationStatus.WITHDRAWN || (isFilled && status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.COMPLETED)
        ),
        TimelineStepData(
            stepNumber = 4,
            label = when {
                isFilled && status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.COMPLETED -> "Vacancy Filled"
                effectiveStatus == ApplicationStatus.ACCEPTED -> "Hired"
                effectiveStatus == ApplicationStatus.COMPLETED -> "Completed"
                effectiveStatus == ApplicationStatus.REJECTED -> "Rejected"
                effectiveStatus == ApplicationStatus.WITHDRAWN -> "Withdrawn"
                else -> "Decision"
            },
            statusText = when {
                isFilled && status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.COMPLETED -> "Position Filled"
                effectiveStatus == ApplicationStatus.ACCEPTED -> "In Progress"
                effectiveStatus == ApplicationStatus.COMPLETED -> "Completed"
                effectiveStatus == ApplicationStatus.REJECTED -> "Completed"
                effectiveStatus == ApplicationStatus.WITHDRAWN -> "Cancelled"
                else -> "Pending"
            },
            isCompleted = effectiveStatus == ApplicationStatus.COMPLETED || effectiveStatus == ApplicationStatus.REJECTED || (isFilled && status != ApplicationStatus.ACCEPTED),
            isCurrent = effectiveStatus == ApplicationStatus.ACCEPTED,
            isSuccess = effectiveStatus == ApplicationStatus.COMPLETED,
            isFailure = effectiveStatus == ApplicationStatus.REJECTED || effectiveStatus == ApplicationStatus.WITHDRAWN || (isFilled && status != ApplicationStatus.ACCEPTED && status != ApplicationStatus.COMPLETED)
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
                        if (steps[1].isCompleted || steps[1].isCurrent) Color(0xFF1F2937)
                        else Color(0xFFE5E7EB)
                    }
                    1 -> {
                        if (steps[2].isCompleted || steps[2].isCurrent) Color(0xFF1F2937)
                        else Color(0xFFE5E7EB)
                    }
                    2 -> {
                        if (steps[3].isCompleted || steps[3].isCurrent) Color(0xFF1F2937)
                        else Color(0xFFE5E7EB)
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
                // Success - Worker black checkmark circle
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
                // Regular completed - Worker black checkmark circle
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
                // Current - Worker black circle with outline and center dot
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
                // Pending - Light gray circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFE5E7EB), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: ApplicationStatus,
    modifier: Modifier = Modifier,
    isFilled: Boolean = false
) {
    Box(
        modifier = modifier
            .background(
                color = getStatusColor(status, isFilled).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = getStatusIcon(status, isFilled),
                fontSize = 12.sp
            )
            Text(
                text = getStatusDisplayName(status, isFilled),
                style = AppTypography.status.copy(
                    color = getStatusColor(status, isFilled)
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
