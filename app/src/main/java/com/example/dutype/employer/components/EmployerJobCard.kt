package com.example.dutype.employer.components

import com.dutype.app.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.example.dutype.employer.helpers.JobPostingHelpers
import com.example.dutype.employer.models.JobPostingModel
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.EmployerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerJobCard(
    modifier: Modifier = Modifier,
    jobPosting: JobPostingModel,
    onEditClick: (String) -> Unit = {},
    onViewApplicationsClick: (String) -> Unit = {},
    onShareClick: (String) -> Unit = {},
    showActions: Boolean = true,
    onViewTrack: (String) -> Unit = {},
    onCardClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showJobManagementDialog by remember { mutableStateOf(false) }
    
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onViewTrack(jobPosting.jobId)
                // Apr 2026: tap on card now opens the OLX-style preview
                // when the host screen wires it; legacy callers that
                // don't pass onCardClick fall back to applications.
                if (onCardClick != null) {
                    onCardClick(jobPosting.jobId)
                } else {
                    onViewApplicationsClick(jobPosting.jobId)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, EmployerColors.Border),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                JobStatusBadge(
                    status = jobPosting.status,
                    isFilled = jobPosting.isFilled,
                    expiresAt = jobPosting.expiresAt
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showActions) {
                        JobActionsRow(
                            jobPosting = jobPosting,
                            onEditClick = onEditClick,
                            onShareClick = onShareClick
                        )
                    }

                    Text(
                        text = JobPostingHelpers.getTimeAgo(jobPosting.postedTime),
                        style = AppTypography.caption,
                        color = EmployerColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Header with title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Job title with category icon (or hero image when present)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val heroUrl = jobPosting.imageUrl
                        if (!heroUrl.isNullOrBlank()) {
                            // Bug #5: replace emoji with the uploaded job image.
                            coil.compose.AsyncImage(
                                model = heroUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = jobPosting.category.icon,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        Text(
                            text = jobPosting.title,
                            style = AppTypography.cardTitle,
                            color = EmployerColors.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Category name hidden per request (employers asked us
                    // to drop the auto-detected category subtitle on the job
                    // card so the title stands alone).

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
                                text = stringResource(R.string.auto_vacancies_filled),
                                style = AppTypography.status.copy(
                                    color = Color(0xFFF59E0B)
                                )
                            )
                        }
                    }
                }

            }

            Spacer(modifier = Modifier.height(12.dp))

            // Job details
            JobDetailsRow(jobPosting = jobPosting)

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onViewApplicationsClick(jobPosting.jobId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.view_applications))
            }

        // Extension button for expired jobs removed

            // Perks display removed as per user request
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
            onShareClick = { 
                showJobManagementDialog = false
                onShareClick(jobPosting.jobId)
            }
        )
    }
}

@Composable
private fun JobStatusBadge(
    status: String,
    isFilled: Boolean,
    expiresAt: Long?
) {
    val normalizedStatus = status.lowercase()
    val expiresAtMillis = expiresAt ?: 0L
    val isClosed = isFilled || normalizedStatus == "closed"
    val isExpired = normalizedStatus == "expired" ||
        (!isClosed && expiresAtMillis > 0L && expiresAtMillis <= System.currentTimeMillis())
    val (color, statusText, icon) = when {
        isClosed -> Triple(Color(0xFF16A34A), "Filled", Icons.Default.CheckCircle)
        isExpired -> Triple(Color(0xFFEF4444), "Expired", Icons.Default.EventBusy)
        else -> Triple(Color(0xFF10B981), "Active", Icons.Default.CheckCircle)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = statusText,
                style = AppTypography.status,
                color = color
            )
        }
    }
}

@Composable
private fun JobDetailsRow(jobPosting: JobPostingModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CurrencyRupee,
                contentDescription = null,
                tint = EmployerColors.IconPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "${jobPosting.payAmount} ${jobPosting.payType.displayName}",
                style = AppTypography.price,
                color = EmployerColors.TextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = EmployerColors.IconSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = jobPosting.location,
                style = AppTypography.bodyMedium,
                color = EmployerColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}

private fun JobPostingModel.shiftDisplayText(): String {
    val text = shiftTimingText?.trim().orEmpty()
    return when {
        text.equals("Any", ignoreCase = true) -> shiftTiming.displayName
        text.isNotBlank() -> text
        else -> shiftTiming.displayName
    }
}

@Composable
private fun JobCardFooter(
    jobPosting: JobPostingModel,
    showActions: Boolean,
    onEditClick: (String) -> Unit,
    onViewApplicationsClick: (String) -> Unit,
    onShareClick: (String) -> Unit,
    onShowManagementDialog: () -> Unit = {}
) {
    Column {
        HorizontalDivider(color = EmployerColors.Divider)

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Per-user request (Apr 2026): the "ðŸ‘¥ N applications" stat
            // chip is removed from this row. The full applications count
            // is already shown when the employer taps the card.

            // Right side - Actions
            if (showActions) {
                JobActionsRow(
                    jobPosting = jobPosting,
                    onEditClick = onEditClick,
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
                text = stringResource(R.string.auto_applications),
                style = AppTypography.caption,
                color = EmployerColors.TextSecondary
            )
        }
    }
}

@Composable
private fun JobActionsRow(
    jobPosting: JobPostingModel,
    onEditClick: (String) -> Unit,
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
                tint = EmployerColors.IconSecondary,
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
    onShareClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.auto_manage_job),
                style = AppTypography.sectionHeader
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Job info
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = jobPosting.title,
                            style = AppTypography.cardTitle,
                            color = EmployerColors.TextPrimary
                        )
                        Text(
                            text = "${jobPosting.applicationsReceived} applications received",
                            style = AppTypography.bodyMedium,
                            color = EmployerColors.TextSecondary
                        )
                    }
                }
                
                // Management options
                Text(
                    text = stringResource(R.string.auto_what_would_you_like_to_do),
                    style = AppTypography.labelLarge,
                    color = EmployerColors.TextPrimary
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
