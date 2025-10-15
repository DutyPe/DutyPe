package com.example.dutype.worker.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import java.text.SimpleDateFormat
import java.util.*

// Helper functions for status display
private fun getStatusIcon(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "⏳"
        ApplicationStatus.UNDER_REVIEW -> "👀"
        ApplicationStatus.REVIEWED -> "👀"
        ApplicationStatus.SHORTLISTED -> "⭐"
        ApplicationStatus.INTERVIEW_SCHEDULED -> "📅"
        ApplicationStatus.INTERVIEWED -> "💼"
        ApplicationStatus.SELECTED -> "🎉"
        ApplicationStatus.REJECTED -> "❌"
        ApplicationStatus.WITHDRAWN -> "↩️"
        ApplicationStatus.EXPIRED -> "⏰"
        ApplicationStatus.HIRED -> "🏆"
    }
}

private fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending Review"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.REVIEWED -> "Reviewed"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.INTERVIEW_SCHEDULED -> "Interview Scheduled"
        ApplicationStatus.INTERVIEWED -> "Interviewed"
        ApplicationStatus.SELECTED -> "Selected"
        ApplicationStatus.REJECTED -> "Not Selected"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
        ApplicationStatus.EXPIRED -> "Expired"
        ApplicationStatus.HIRED -> "Hired"
    }
}

private fun getStatusColor(status: ApplicationStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        ApplicationStatus.PENDING -> androidx.compose.ui.graphics.Color(0xFFF59E0B) // Amber
        ApplicationStatus.UNDER_REVIEW -> androidx.compose.ui.graphics.Color(0xFF3B82F6) // Blue
        ApplicationStatus.REVIEWED -> androidx.compose.ui.graphics.Color(0xFF3B82F6) // Blue
        ApplicationStatus.SHORTLISTED -> androidx.compose.ui.graphics.Color(0xFF10B981) // Green
        ApplicationStatus.INTERVIEW_SCHEDULED -> androidx.compose.ui.graphics.Color(0xFF8B5CF6) // Purple
        ApplicationStatus.INTERVIEWED -> androidx.compose.ui.graphics.Color(0xFF06B6D4) // Cyan
        ApplicationStatus.SELECTED -> androidx.compose.ui.graphics.Color(0xFF059669) // Emerald
        ApplicationStatus.REJECTED -> androidx.compose.ui.graphics.Color(0xFFEF4444) // Red
        ApplicationStatus.WITHDRAWN -> androidx.compose.ui.graphics.Color(0xFF6B7280) // Gray
        ApplicationStatus.EXPIRED -> androidx.compose.ui.graphics.Color(0xFF9CA3AF) // Light Gray
        ApplicationStatus.HIRED -> androidx.compose.ui.graphics.Color(0xFF10B981) // Green
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
    onWithdrawClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showWithdrawDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick(application) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
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
                            color = Color(0xFF111827)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
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
                    if (application.status == ApplicationStatus.PENDING || 
                        application.status == ApplicationStatus.REVIEWED) {
                        OutlinedButton(
                            onClick = { showWithdrawDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Withdraw",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    
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
    
    // Withdraw confirmation dialog
    if (showWithdrawDialog) {
        WithdrawApplicationDialog(
            onConfirm = {
                onWithdrawClick(application.applicationId)
                showWithdrawDialog = false
            },
            onDismiss = { showWithdrawDialog = false }
        )
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

@Composable
private fun WithdrawApplicationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Withdraw Application",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Text(
                text = "Are you sure you want to withdraw this application? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                )
            ) {
                Text("Withdraw")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
