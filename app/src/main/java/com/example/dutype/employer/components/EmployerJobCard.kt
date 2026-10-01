package com.example.dutype.employer.components

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dutype.models.JobListing
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.firestore.FirestoreSchema
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.EmployerColors

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerJobCard(
    modifier: Modifier = Modifier,
    jobPosting: JobListing,
    applications: List<JobApplication> = emptyList(),
    onCallWorker: (com.example.dutype.models.JobApplication) -> Unit = {},
    onHireWorker: (JobApplication) -> Unit = {},
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
                onViewTrack(jobPosting.id)
                // Apr 2026: tap on card now opens the OLX-style preview
                // when the host screen wires it; legacy callers that
                // don't pass onCardClick fall back to applications.
                if (onCardClick != null) {
                    onCardClick(jobPosting.id)
                } else {
                    onViewApplicationsClick(jobPosting.id)
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
                    isFilled = (jobPosting.status == FirestoreSchema.Values.JobStatus.FILLED),
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
                        text = com.example.dutype.utils.DateTimeUtils.formatRelativeTime(jobPosting.createdAt),
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
                        val heroUrl = jobPosting.photoUrl
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
                                text = JobCategory.fromKey(jobPosting.category).icon,
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
                    if ((jobPosting.status == FirestoreSchema.Values.JobStatus.FILLED)) {
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
                                    color = Color(0xFFF59E0B).fg()
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

            if (applications.isNotEmpty()) {
                // High-visibility green banner for Tier 2/3 employers
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7).bg(),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC).bd()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A).fg(),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${applications.size} ${if (applications.size == 1) "Worker" else "Workers"} Applied! Call & Hire Directly",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF15803D).fg()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Render latest 1 or 2 applicants directly on the card
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    applications.take(2).forEach { applicant ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC).bg(),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(EmployerColors.Primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = applicant.workerName.trim().take(1).uppercase().ifBlank { "W" },
                                        fontWeight = FontWeight.Bold,
                                        color = EmployerColors.Primary.fg(),
                                        fontSize = 14.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = applicant.workerName.ifBlank { "Verified Worker" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = EmployerColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val detailParts = mutableListOf<String>()
                                    applicant.worker?.distanceKm?.takeIf { it > 0 }?.let {
                                        detailParts.add(String.format(Locale.getDefault(), "%.1f km away", it))
                                    }
                                    applicant.worker?.experienceYears?.takeIf { it > 0 }?.let {
                                        detailParts.add("$it yrs exp")
                                    }
                                    Text(
                                        text = if (detailParts.isNotEmpty()) detailParts.joinToString(" • ") else "Available to work",
                                        fontSize = 11.sp,
                                        color = EmployerColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // 1-Tap Direct Call Button
                                Button(
                                    onClick = { onCallWorker(applicant) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A).bg()),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // 1-Tap Hire Button
                                if (applicant.status != ApplicationStatus.HIRED) {
                                    OutlinedButton(
                                        onClick = { onHireWorker(applicant) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Hire", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB).fg())
                                    }
                                } else {
                                    Text("✅ Hired", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A).fg())
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onViewApplicationsClick(jobPosting.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB).bg())
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Hiring Room (${applications.size} ${if (applications.size == 1) "Applicant" else "Applicants"})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9).bg(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = EmployerColors.TextSecondary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "0 Applied yet • Tap below to find & call nearby matching workers",
                            fontSize = 12.sp,
                            color = EmployerColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onViewApplicationsClick(jobPosting.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB).bg())
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Hiring Room (View Matches)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
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
                onEditClick(jobPosting.id)
            },
            onViewApplicationsClick = {
                showJobManagementDialog = false
                onViewApplicationsClick(jobPosting.id)
            },
            onShareClick = {
                showJobManagementDialog = false
                onShareClick(jobPosting.id)
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
    val now = System.currentTimeMillis()
    val isClosed = isFilled || normalizedStatus == "closed" || normalizedStatus == "filled"
    val isExpired = normalizedStatus == "expired" ||
        (!isClosed && expiresAtMillis > 0L && expiresAtMillis <= now)
    val isExpiringSoon = !isClosed && !isExpired && expiresAtMillis > now && (expiresAtMillis - now <= 3L * 24 * 60 * 60 * 1000L)

    val (color, statusText, icon) = when {
        isClosed -> Triple(Color(0xFF16A34A), "Filled", Icons.Default.CheckCircle)
        isExpired -> Triple(Color(0xFFEF4444), "Expired", Icons.Default.EventBusy)
        isExpiringSoon -> {
            val daysLeft = (((expiresAtMillis - now) / (24 * 60 * 60 * 1000L)) + 1).toInt()
            Triple(Color(0xFFF59E0B), "Expires in ${daysLeft}d", Icons.Default.Warning)
        }
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
private fun JobDetailsRow(jobPosting: JobListing) {
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
                text = jobPosting.payText,
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
                text = jobPosting.area,
                style = AppTypography.bodyMedium,
                color = EmployerColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}

@Composable
private fun JobActionsRow(
    jobPosting: JobListing,
    onEditClick: (String) -> Unit,
    onShareClick: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        // Edit button
        IconButton(
            onClick = { onEditClick(jobPosting.id) },
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
            onClick = { onShareClick(jobPosting.id) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share job",
                tint = Color(0xFF8B5CF6).fg(),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobManagementDialog(
    jobPosting: JobListing,
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
                            text = "${jobPosting.applicationCount} applications received",
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6).bg())
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
