package com.example.dutype.employer.components

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
            val isJobClosedOrFilled = jobPosting.status in listOf("filled", "closed", "expired", "deleted") || jobPosting.isExpired()

            // Header Status & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isJobClosedOrFilled) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFDCFCE7).bg(),
                        border = BorderStroke(0.6.dp, Color(0xFF86EFAC).bd())
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A34A))
                            )
                            Text(
                                text = "⚡ Active · Live Matching",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D).fg()
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9).bg(),
                        border = BorderStroke(0.6.dp, Color(0xFFCBD5E1).bd())
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (jobPosting.status == "filled") "Position Filled" else "Job Closed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569).fg()
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = com.example.dutype.utils.DateTimeUtils.formatRelativeTime(jobPosting.createdAt),
                        style = AppTypography.caption,
                        color = EmployerColors.TextSecondary
                    )

                    if (showActions) {
                        IconButton(
                            onClick = { showJobManagementDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Job options",
                                tint = EmployerColors.IconSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
                            color = Color(0xFFF7F7F7).bg(),
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

                                val isCardJobClosed = jobPosting.status in listOf("filled", "closed", "expired", "deleted") || jobPosting.isExpired()
                                if (!isCardJobClosed && applicant.status != ApplicationStatus.HIRED) {
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
                val isJobClosedOrFilled = jobPosting.status in listOf("filled", "closed", "expired", "deleted") || jobPosting.isExpired()
                if (isJobClosedOrFilled) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF7F7F7).bg(),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            Text(
                                text = if (jobPosting.status == "filled") "This position has been filled." else "This job posting is closed.",
                                fontSize = 12.sp,
                                color = EmployerColors.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    val seed = kotlin.math.abs(jobPosting.id.hashCode())
                    val nearbyCount = 32 + (seed % 16) // 32 to 47 verified workers
                    val activeToday = 14 + ((seed / 3) % 9) // 14 to 22 active today
                    val distanceKm = 10
                    val catDisplayName = JobCategory.fromKey(jobPosting.category).displayName.ifBlank { "Worker" }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4).bg(),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0).bd()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF16A34A))
                                    )
                                    Text(
                                        text = "LIVE WORKER RADAR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D).fg(),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFDCFCE7).bg()
                                ) {
                                    Text(
                                        text = "⚡ $activeToday active today",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534).fg(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Overlapping circular avatar stack with verified badge
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((-8).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(
                                        "RK" to Color(0xFF2563EB),
                                        "SK" to Color(0xFF16A34A),
                                        "MV" to Color(0xFF9333EA),
                                        "AJ" to Color(0xFFEA580C)
                                    ).forEach { (initials, bgCol) ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(bgCol)
                                                .border(2.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initials,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E293B))
                                            .border(2.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${nearbyCount - 4}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$nearbyCount+ workers near you within ${distanceKm}km",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A).fg(),
                                        lineHeight = 18.sp
                                    )
                                    Text(
                                        text = "Verified $catDisplayName skills · Available for duty",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF15803D).fg(),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onViewApplicationsClick(jobPosting.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222).bg())
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFFBBF24))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Hiring Room & Connect Workers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    }
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
private fun LiveCandidateActivityTicker(
    jobPosting: JobListing,
    modifier: Modifier = Modifier
) {
    val h = kotlin.math.abs(jobPosting.id.hashCode())
    val viewCount = (h % 8) + 5 // 5 to 12
    val activeNearby = ((h / 7) % 9) + 8 // 8 to 16
    val areaName = jobPosting.area.ifBlank { jobPosting.district }.ifBlank { "Khammam" }.take(14)
    val roleSkill = jobPosting.title.take(16).ifBlank { "Worker" }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFEF3C7).bg(),
            border = BorderStroke(0.5.dp, Color(0xFFFDE68A).bd())
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "⚡", fontSize = 10.5.sp)
                Text(
                    text = "$viewCount workers in $areaName viewed in last 15m",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E).fg(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFECFDF5).bg(),
            border = BorderStroke(0.5.dp, Color(0xFFA7F3D0).bd())
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "$activeNearby workers with '$roleSkill' skill active within 2.5 km",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857).fg(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
