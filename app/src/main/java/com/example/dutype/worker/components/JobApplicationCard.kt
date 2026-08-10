package com.example.dutype.worker.components

import com.dutype.app.R
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

// Helper functions for status display
private fun getStatusIcon(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.APPLIED -> "⏳"
        ApplicationStatus.HIRED -> "🎉"
        ApplicationStatus.COMPLETED -> "✅"
        ApplicationStatus.REJECTED -> "❌"
        ApplicationStatus.WITHDRAWN -> ""
        else -> ""
    }
}

private fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.APPLIED -> "Applied"
        ApplicationStatus.HIRED -> "Hired"
        ApplicationStatus.WORK_SUBMITTED -> "Awaiting confirmation"
        ApplicationStatus.COMPLETED -> "Completed"
        ApplicationStatus.REJECTED -> "Not Selected"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
        ApplicationStatus.DELETED -> "Removed"
        ApplicationStatus.FILLED -> "Closed"
    }
}

private fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.APPLIED -> Color(0xFFF59E0B) // Amber
        ApplicationStatus.HIRED -> Color(0xFF10B981) // Green
        ApplicationStatus.COMPLETED -> Color(0xFF1F8B4C) // Dark green
        ApplicationStatus.REJECTED -> Color(0xFFEF4444) // Red
        ApplicationStatus.WITHDRAWN -> Color(0xFF6B7280)
        else -> Color(0xFF6B7280) // Gray
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
    onMarkWorkDoneClick: ((JobApplication) -> Unit)? = null,
    hasAlreadyRated: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Can withdraw only if status is APPLIED and within 24 hours
    val canWithdraw = application.status == ApplicationStatus.APPLIED && 
                      (System.currentTimeMillis() - application.createdAt) < 24L * 60L * 60L * 1000L || 
                      application.status == ApplicationStatus.APPLIED
    
    // Quick-call: workers in early funnel stages (applied / shortlisted / hired)
    // can dial the employer directly to push their candidacy. Hidden once the
    // application is rejected or already completed (terminal states).
    // Batch-n #4: show the Call button even if `employerPhone` snapshot is
    // missing on legacy applications — when tapped without a number we
    // open the job detail screen so the worker can still find the contact
    // there. Previously the button was hidden whenever the phone was
    // blank, which silently dropped the primary CTA on most older rows.
    val isActiveStage = application.status == ApplicationStatus.APPLIED ||
        application.status == ApplicationStatus.APPLIED ||
        application.status == ApplicationStatus.HIRED
    val canCall = isActiveStage
    val hasEmployerPhone = !application.employerPhone.isNullOrBlank()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isFilledForThisWorker = application.jobStatus.equals("closed", ignoreCase = true) &&
        application.status != ApplicationStatus.HIRED &&
        application.status != ApplicationStatus.COMPLETED
    val showNoResponseHelp = canCall &&
        !isFilledForThisWorker &&
        application.status in setOf(ApplicationStatus.APPLIED, ApplicationStatus.APPLIED) &&
        System.currentTimeMillis() - application.createdAt >= 24L * 60L * 60L * 1000L
    
    // Can rate only if status is COMPLETED and hasn't rated yet
    val canRate = application.status == ApplicationStatus.COMPLETED && !hasAlreadyRated && onRateClick != null
    val isCompleted = application.status == ApplicationStatus.COMPLETED
    
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
                                color = WorkerColors.TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (application.companyName.isNotBlank()) {
                            Text(
                                text = application.companyName,
                                style = AppTypography.bodyMedium.copy(
                                    color = WorkerColors.TextSecondary
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
                            color = WorkerColors.TextTertiary
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
                                color = WorkerColors.WarningLight,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = WorkerColors.Warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.auto_this_job_was_filled_your_application_is_st),
                            style = AppTypography.caption.copy(
                                color = WorkerColors.Warning,
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
                                color = WorkerColors.SuccessLight,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = WorkerColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.auto_call_employer_to_confirm_availability_and),
                            style = AppTypography.caption.copy(
                                color = WorkerColors.Success,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (showNoResponseHelp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = WorkerColors.WarningLight,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = WorkerColors.Warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.auto_employer_not_responding_try_one_call_then),
                            style = AppTypography.caption.copy(
                                color = WorkerColors.Warning,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                    onCardClick(application)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WorkerColors.Success
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.auto_call_employer),
                                style = AppTypography.buttonSmall.copy(
                                    color = Color.White
                                )
                            )
                        }
                    }

                    if (canWithdraw && onWithdrawClick != null) {
                        Button(
                            onClick = { onWithdrawClick(application) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WorkerColors.Error.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.auto_withdraw),
                                style = AppTypography.buttonSmall.copy(
                                    color = WorkerColors.Error
                                )
                            )
                        }
                    }

                    // Lets the worker put their side of the record down first; the
                    // employer still has to confirm before the job counts as completed.
                    if (application.status == ApplicationStatus.HIRED && onMarkWorkDoneClick != null) {
                        Button(
                            onClick = { onMarkWorkDoneClick(application) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WorkerColors.Primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.mark_work_done),
                                style = AppTypography.buttonSmall.copy(color = Color.White)
                            )
                        }
                    }
                        
                }
            }
            
            // Rating Section for Completed Jobs - Clean separate section
            if (isCompleted) {
                HorizontalDivider(
                    color = WorkerColors.ChipBackground,
                    thickness = 1.dp
                )
                
                // Rating section header (clickable to expand/collapse)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRatingSectionExpanded = !isRatingSectionExpanded }
                        .background(
                            if (canRate) WorkerColors.WarningLight.copy(alpha = 0.5f) else WorkerColors.ChipBackground
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (canRate) "Rate this employer" else "Rating submitted",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (canRate) WorkerColors.Warning else WorkerColors.Success
                        )
                    )
                    
                        Icon(
                            imageVector = if (isRatingSectionExpanded) 
                                Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                        tint = WorkerColors.IconSecondary,
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
                                if (canRate) WorkerColors.WarningLight.copy(alpha = 0.3f) else WorkerColors.SuccessLight
                            )
                            .padding(20.dp)
                    ) {
                        if (canRate) {
                            // Show rating prompt
                            Text(
                                text = "How was your experience working with ${application.companyName}?",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = WorkerColors.Warning
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                                                        
                            // Rate button
                            Button(
                                onClick = { onRateClick?.invoke(application) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WorkerColors.Warning
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.auto_rate_employer),
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
                                    tint = WorkerColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Thank you for rating ${application.companyName}!",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = WorkerColors.Success
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
            label = stringResource(R.string.under_review),
            statusText = when (status) {
                ApplicationStatus.APPLIED -> "Pending"
                ApplicationStatus.HIRED -> "Completed"
                ApplicationStatus.COMPLETED -> "Completed"
                ApplicationStatus.REJECTED -> "Cancelled"
                ApplicationStatus.WITHDRAWN -> "Withdrawn"
                else -> "Cancelled"
            },
            isCompleted = status == ApplicationStatus.APPLIED ||
                status == ApplicationStatus.HIRED ||
                status == ApplicationStatus.COMPLETED,
            isCurrent = status == ApplicationStatus.APPLIED,
            isFailure = status == ApplicationStatus.REJECTED
        ),
        TimelineStepData(
            stepNumber = 3,
            label = "Decision",
            statusText = when (status) {
                ApplicationStatus.HIRED -> "Completed"
                ApplicationStatus.COMPLETED -> "Completed"
                ApplicationStatus.REJECTED -> "Completed"
                ApplicationStatus.WITHDRAWN -> "Completed"
                else -> "Pending"
            },
            isCompleted = status == ApplicationStatus.HIRED ||
                status == ApplicationStatus.COMPLETED ||
                status == ApplicationStatus.REJECTED ||
                status == ApplicationStatus.WITHDRAWN,
            isCurrent = false,
            isSuccess = status == ApplicationStatus.HIRED || status == ApplicationStatus.COMPLETED,
            isFailure = status == ApplicationStatus.REJECTED || status == ApplicationStatus.WITHDRAWN
        ),
        TimelineStepData(
            stepNumber = 4,
            label = "Complete",
            statusText = when (status) {
                ApplicationStatus.COMPLETED -> "Completed"
                ApplicationStatus.REJECTED -> "Cancelled"
                ApplicationStatus.WITHDRAWN -> "Withdrawn"
                else -> "Pending"
            },
            isCompleted = status == ApplicationStatus.COMPLETED,
            isCurrent = status == ApplicationStatus.HIRED,
            isSuccess = status == ApplicationStatus.COMPLETED,
            isFailure = status == ApplicationStatus.REJECTED || status == ApplicationStatus.WITHDRAWN
        )
    )
    
    Box(
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        val connectorActiveColor = WorkerColors.TextPrimary
        val connectorInactiveColor = WorkerColors.Border

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
                                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
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
                        if (steps[1].isCompleted || steps[1].isCurrent) connectorActiveColor
                        else connectorInactiveColor
                    }
                    1 -> {
                        if (steps[2].isCompleted || steps[2].isCurrent) connectorActiveColor
                        else connectorInactiveColor
                    }
                    2 -> {
                        if (steps[3].isCompleted || steps[3].isCurrent) connectorActiveColor
                        else connectorInactiveColor
                    }
                    else -> connectorInactiveColor
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
                        .background(WorkerColors.Success, CircleShape),
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
                        .background(WorkerColors.Error, CircleShape),
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
                        .background(WorkerColors.Success, CircleShape),
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
                        .background(WorkerColors.InfoLight, CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                WorkerColors.TextPrimary.copy(alpha = blinkAlpha), 
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
                        .background(WorkerColors.Border, CircleShape)
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
