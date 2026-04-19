package com.example.dutype.employer.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dutype.app.R
import com.example.dutype.employer.helpers.JobPostingHelpers
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.employer.models.JobUrgency
import com.example.dutype.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerJobCard(
    modifier: Modifier = Modifier,
    jobPosting: JobPostingModel,
    onEditClick: (String) -> Unit = {},
    onViewApplicationsClick: (String) -> Unit = {},
    onToggleActiveClick: (String) -> Unit = {},
    onShareClick: (String) -> Unit = {},
    showActions: Boolean = true,
    onViewTrack: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showJobManagementDialog by remember { mutableStateOf(false) }
    
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                onViewTrack(jobPosting.jobId) // Also call the callback if provided
                onViewApplicationsClick(jobPosting.jobId) 
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Job title with category icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = jobPosting.category.icon,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = jobPosting.title,
                            style = AppTypography.cardTitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Category name
                    Text(
                        text = jobPosting.category.displayName,
                        style = AppTypography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 40.dp)
                    )
                    
                    // Filled status indicator
                    if (jobPosting.isFilled) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFFF59E0B).copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Vacancies Filled",
                                style = AppTypography.status.copy(
                                    color = Color(0xFFF59E0B)
                                )
                            )
                        }
                    }
                }

                // Posted time and status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Posted time
                    Text(
                        text = JobPostingHelpers.getTimeAgo(jobPosting.postedTime),
                        style = AppTypography.caption,
                        color = Color.Gray
                    )
                    
                    // Status indicator
                    JobStatusBadge(
                        isActive = jobPosting.isActive,
                        urgency = jobPosting.urgency
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Job details
            JobDetailsRow(jobPosting = jobPosting)

            Spacer(modifier = Modifier.height(12.dp))

            // Description preview
            if (jobPosting.description.isNotBlank()) {
                Text(
                    text = jobPosting.description,
                    style = AppTypography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Perks display removed as per user request

            // Footer with stats and actions
            JobCardFooter(
                jobPosting = jobPosting,
                showActions = showActions,
                onEditClick = onEditClick,
                onViewApplicationsClick = onViewApplicationsClick,
                onToggleActiveClick = onToggleActiveClick,
                onShareClick = onShareClick,
                onShowManagementDialog = { showJobManagementDialog = true }
            )
        }
    }
    
    // Job Management Dialog
    if (showJobManagementDialog) {
        JobManagementDialog(
            jobPosting = jobPosting,
            onDismiss = { showJobManagementDialog = false },
            onEditClick = { 
                showJobManagementDialog = false
                onEditClick(jobPosting.jobId)
            },
            onViewApplicationsClick = { 
                showJobManagementDialog = false
                onViewApplicationsClick(jobPosting.jobId)
            },
            onToggleActiveClick = { 
                showJobManagementDialog = false
                onToggleActiveClick(jobPosting.jobId)
            },
            onShareClick = { 
                showJobManagementDialog = false
                onShareClick(jobPosting.jobId)
            }
        )
    }
}

@Composable
private fun JobStatusBadge(
    isActive: Boolean,
    urgency: JobUrgency
) {
    val (backgroundColor, textColor, statusText, icon) = when {
        !isActive -> Quadruple(Color(0xFFEF4444), Color.White, "Paused", Icons.Default.Pause)
        urgency == JobUrgency.IMMEDIATE -> Quadruple(Color(0xFFEF4444), Color.White, "Urgent", Icons.Default.Warning)
        urgency == JobUrgency.URGENT -> Quadruple(Color(0xFFF59E0B), Color.White, "Priority", Icons.Default.PriorityHigh)
        else -> Quadruple(Color(0xFF10B981), Color.White, "Active", Icons.Default.CheckCircle)
    }

    Row(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = statusText,
            style = AppTypography.status,
            color = textColor
        )
    }
}

// Helper data class for quadruple values
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun JobDetailsRow(jobPosting: JobPostingModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left column - Pay and Location
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyRupee,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "₹${jobPosting.payAmount} ${jobPosting.payType.displayName}",
                    style = AppTypography.price,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = jobPosting.location,
                    style = AppTypography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right column - Vacancies and Timing
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${jobPosting.vacancies} position${if (jobPosting.vacancies != 1) "s" else ""}",
                style = AppTypography.labelLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = jobPosting.shiftTiming.displayName,
                style = AppTypography.caption,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun PerksDisplay(perks: List<com.example.dutype.employer.models.JobPerk>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        perks.take(3).forEach { perk ->
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${perk.icon} ${perk.displayName}",
                    style = AppTypography.caption,
                    color = Color.Gray
                )
            }
        }

        if (perks.size > 3) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFF3F4F6),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "+${perks.size - 3} more",
                    style = AppTypography.caption,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun JobCardFooter(
    jobPosting: JobPostingModel,
    showActions: Boolean,
    onEditClick: (String) -> Unit,
    onViewApplicationsClick: (String) -> Unit,
    onToggleActiveClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onShowManagementDialog: () -> Unit = {}
) {
    Column {
        HorizontalDivider(color = Color(0xFFE5E7EB))

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Stats
            JobStatsRow(jobPosting = jobPosting)

            // Right side - Actions
            if (showActions) {
                JobActionsRow(
                    jobPosting = jobPosting,
                    onEditClick = onEditClick,
                    onToggleActiveClick = onToggleActiveClick,
                    onShareClick = onShareClick
                )
            }
        }
    }
}

@Composable
private fun JobStatsRow(jobPosting: JobPostingModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Applications count
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${jobPosting.applicationsReceived}",
                style = AppTypography.labelLarge
            )
            Text(
                text = "applications",
                style = AppTypography.caption,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun JobActionsRow(
    jobPosting: JobPostingModel,
    onEditClick: (String) -> Unit,
    onToggleActiveClick: (String) -> Unit,
    onShareClick: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {


        // Edit button
        IconButton(
            onClick = { onEditClick(jobPosting.jobId) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit job",
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(18.dp)
            )
        }

        // Toggle active/inactive
        IconButton(
            onClick = { onToggleActiveClick(jobPosting.jobId) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (jobPosting.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (jobPosting.isActive) "Pause job" else "Activate job",
                tint = if (jobPosting.isActive) Color(0xFFF59E0B) else Color(0xFF10B981),
                modifier = Modifier.size(18.dp)
            )
        }

        // Share button
        IconButton(
            onClick = { onShareClick(jobPosting.jobId) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share job",
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobManagementDialog(
    jobPosting: JobPostingModel,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onViewApplicationsClick: () -> Unit,
    onToggleActiveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Manage Job",
                style = AppTypography.sectionHeader
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Job info
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = jobPosting.title,
                            style = AppTypography.cardTitle
                        )
                        Text(
                            text = "${jobPosting.applicationsReceived} applications received",
                            style = AppTypography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Status: ${if (jobPosting.isActive) "Active" else "Paused"}",
                            style = AppTypography.caption,
                            color = if (jobPosting.isActive) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                }
                
                // Management options
                Text(
                    text = "What would you like to do?",
                    style = AppTypography.labelLarge
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Applications
                Button(
                    onClick = onViewApplicationsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.applications))
                }
                
                // Edit Job
                OutlinedButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.edit_button))
                }
            }
        }
    )
}
