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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
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
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

// Helper functions for status display
private fun getStatusIcon(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.APPLIED -> "⏳"
        ApplicationStatus.SHORTLISTED -> "👀"
        ApplicationStatus.HIRED -> "🎉"
        ApplicationStatus.COMPLETED -> "✅"
        ApplicationStatus.REJECTED -> "❌"
        ApplicationStatus.WITHDRAWN -> ""
    }
}

private fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.APPLIED -> "Applied"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.HIRED -> "Hired"
        ApplicationStatus.COMPLETED -> "Completed"
        ApplicationStatus.REJECTED -> "Not Selected"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
    }
}

private fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.APPLIED -> Color(0xFFF59E0B) // Amber
        ApplicationStatus.SHORTLISTED -> Color(0xFF3B82F6) // Blue
        ApplicationStatus.HIRED -> Color(0xFF10B981) // Green
        ApplicationStatus.COMPLETED -> Color(0xFF1F8B4C) // Dark green
        ApplicationStatus.REJECTED -> Color(0xFFEF4444) // Red
        ApplicationStatus.WITHDRAWN -> Color(0xFF6B7280)
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
    hasAlreadyRated: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Can withdraw only if status is PENDING or UNDER_REVIEW
    val canWithdraw = application.status == ApplicationStatus.APPLIED || 
                      application.status == ApplicationStatus.SHORTLISTED
    
    // Quick-call: workers in early funnel stages (applied / shortlisted / hired)
    // can dial the employer directly to push their candidacy. Hidden once the
    // application is rejected or already completed (terminal states).
    // Batch-n #4: show the Call button even if `employerPhone` snapshot is
    // missing on legacy applications — when tapped without a number we
    // open the job detail screen so the worker can still find the contact
    // there. Previously the button was hidden whenever the phone was
    // blank, which silently dropped the primary CTA on most older rows.
    val isActiveStage = application.status == ApplicationStatus.APPLIED ||
        application.status == ApplicationStatus.SHORTLISTED ||
        application.status == ApplicationStatus.HIRED
    val canCall = isActiveStage
    val hasEmployerPhone = !application.employerPhone.isNullOrBlank()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isFilledForThisWorker = application.jobStatus.equals("closed", ignoreCase = true) &&
        application.status != ApplicationStatus.HIRED &&
        application.status != ApplicationStatus.COMPLETED
    
    // Can rate only if status is COMPLETED and hasn't rated yet
    val canRate = application.status == ApplicationStatus.HIRED && !hasAlreadyRated && onRateClick != null
    val isCompleted = application.status == ApplicationStatus.HIRED
    
    // State for rating section expansion
    var isRatingSectionExpanded by remember { mutableStateOf(canRate) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isFilledForThisWorker) { onCardClick(application) }
            .border(0.5.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WorkerColors.CardBackground
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
                                color = Color(0xFF111827)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (application.companyName.isNotBlank()) {
                            Text(
                                text = application.companyName,
                                style = AppTypography.bodyMedium.copy(
                                    color = Color(0xFF6B7280)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Status badge
                    StatusBadge(
                        status = application.status,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Applied time only. The worker's applied-job card intentionally
                // hides the job address to keep this list compact and private.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Applied ${formatDate(application.createdAt)}",
                        style = AppTypography.caption.copy(
                            color = Color(0xFF9CA3AF)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Quick-call hint banner — nudges the worker that calling is
                // the fastest way to land the job. Only shown when the
                // application is in an active funnel stage and the employer
                // shared a phone number on the job post.
                if (isFilledForThisWorker) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFFFF7ED),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "This job was filled. Your application is still saved here.",
                            style = AppTypography.caption.copy(
                                color = Color(0xFF9A3412),
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (canCall) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tap Call for super-fast response — employers reply 5× faster on calls",
                            style = AppTypography.caption.copy(
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Action buttons row - separate row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        
                        // Quick Call button — fires ACTION_DIAL with the
                        // denormalized employer phone (set at apply time).
                        // Falls back to opening the job detail screen when
                        // the phone snapshot is missing (legacy rows).
                        if (canCall && !isFilledForThisWorker) {
                            Button(
                                onClick = {
                                    val phone = application.employerPhone.orEmpty()
                                    if (phone.isNotBlank()) {
                                        runCatching {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_DIAL,
                                                android.net.Uri.parse("tel:$phone")
                                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                    } else {
                                        // No snapshot phone — bring the
                                        // worker to the full job detail
                                        // screen where the contact section
                                        // can resolve from job_details.
                                        onCardClick(application)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Call",
                                    style = AppTypography.buttonSmall.copy(
                                        color = Color.White
                                    )
                                )
                            }
                        }
                        
                        if (!isFilledForThisWorker) {
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
                    Text(
                        text = if (canRate) "Rate this employer" else "Rating submitted",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (canRate) Color(0xFFB45309) else Color(0xFF059669)
                        )
                    )
                    
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
                            
                            Spacer(modifier = Modifier.height(10.dp))
                                                        
                            // Rate button
                            Button(
                                onClick = { onRateClick?.invoke(application) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
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
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        TimelineStepData(
            stepNumber = 1,
            label = stringResource(R.string.applied),
            statusText = "Completed",
            isCompleted = true,
            isCurrent = false
        ),
        TimelineStepData(
            stepNumber = 2,
            label = stringResource(R.string.shortlisted),
            statusText = when (status) {
                ApplicationStatus.APPLIED -> "Pending"
                ApplicationStatus.SHORTLISTED -> "In Progress"
                ApplicationStatus.HIRED -> "Completed"
                ApplicationStatus.COMPLETED -> "Completed"
                ApplicationStatus.REJECTED -> "Cancelled"
                ApplicationStatus.WITHDRAWN -> "Withdrawn"
            },
            isCompleted = status == ApplicationStatus.SHORTLISTED || status == ApplicationStatus.HIRED,
            isCurrent = status == ApplicationStatus.SHORTLISTED,
            isFailure = status == ApplicationStatus.REJECTED
        ),
        TimelineStepData(
            stepNumber = 3,
            label = when (status) {
                ApplicationStatus.HIRED -> "Hired"
                ApplicationStatus.REJECTED -> "Rejected"
                ApplicationStatus.WITHDRAWN -> "Withdrawn"
                else -> "Decision"
            },
            statusText = when (status) {
                ApplicationStatus.HIRED -> "Completed"
                ApplicationStatus.REJECTED -> "Completed"
                ApplicationStatus.WITHDRAWN -> "Completed"
                else -> "Pending"
            },
            isCompleted = status == ApplicationStatus.HIRED || status == ApplicationStatus.REJECTED || status == ApplicationStatus.WITHDRAWN,
            isCurrent = false,
            isSuccess = status == ApplicationStatus.HIRED,
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
                        
                        // Step title only (no status text)
                        Text(
                            text = stepData.label,
                            style = AppTypography.labelMedium.copy(
                                color = Color(0xFF1F2937),
                                fontWeight = FontWeight.SemiBold
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
                .height(18.dp)
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
        modifier = Modifier.size(18.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isCompleted && isSuccess -> {
                // Success - Green checkmark circle
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
            isCompleted && isFailure -> {
                // Failure - Red X circle
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color(0xFFDC2626), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
            isCompleted -> {
                // Regular completed - Green checkmark circle
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(Color(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
            isSelected -> {
                // Current - Black circle with outline and center dot
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
                        .size(18.dp)
                        .background(Color(0xFFEBF4FF), CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFF1F2937),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
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
                        .size(18.dp)
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

// NOTE: formatDate uses DateTimeUtils.formatTimeAgoExactDays() for exact days display (no weeks)
private fun formatDate(timestamp: Long): String {
    return DateTimeUtils.formatTimeAgoExactDays(timestamp)
}
