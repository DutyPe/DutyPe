package com.example.dutype.employer.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.fg
import com.example.dutype.ui.theme.bg
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.Assignment as AssignmentOutlinedIcon
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dutype.app.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.JobListing

/**
 * Data holder for candidate display on the Employer Home Screen
 */
data class CandidateDisplayItem(
    val id: String,
    val name: String,
    val initials: String,
    val trade: String,
    val distanceText: String,
    val rating: String,
    val jobsDone: Int,
    val jobId: String,
    val workerId: String
)

/**
 * Top Header Strip matching Stitch Version 1
 * [SS] Sri Sai Traders | 📍 Hubli, Karnataka ⌵ | [ 📞 Support / मदद ] [ 👤 ]
 */
@Composable
fun EmployerTopHeader(
    companyName: String,
    locationText: String,
    onLocationClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayCompanyName = companyName.ifBlank { stringResource(R.string.employer_my_business) }
    val displayLocation = locationText.ifBlank { stringResource(R.string.employer_select_location) }
    val initials = displayCompanyName
        .trim()
        .split("\\s+".toRegex())
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "MB" }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.bg())
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Company Name + Location (initials square box removed)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Column {
                Text(
                    text = displayCompanyName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A).fg(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLocationClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFEF4444).fg(),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = displayLocation,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B).fg(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8).fg(),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Support Pill (profile circle removed)
        Surface(
            onClick = onSupportClick,
            shape = RoundedCornerShape(16.dp),
            color = Color.White.bg(),
            border = BorderStroke(1.dp, Color(0xFF10B981).bd()),
            modifier = Modifier.height(34.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = Color(0xFF10B981).fg(),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.employer_support),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF047857).fg()
                )
            }
        }
    }
}

/**
 * Top Section: "Need workers for your shop or home?"
 * Two direct action cards: Urgent vs Regular Job (equal size, no chips, with category pills)
 */
@Composable
fun NeedWorkersSplitCard(
    onUrgentClick: (String?) -> Unit,
    onRegularJobClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.employer_home_need_workers_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A).fg()
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.employer_home_need_workers_subtitle),
            fontSize = 13.sp,
            color = Color(0xFF64748B).fg()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Urgent (FAST tag removed, height matches Regular Job)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onUrgentClick(null) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2).bg()),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5).bd()),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.bg()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color(0xFFDC2626).fg(),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.employer_home_urgent_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626).fg()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.employer_home_urgent_subtitle),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B).fg(),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Card 2: Regular Job
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onRegularJobClick() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5).bg()),
                border = BorderStroke(1.dp, Color(0xFF6EE7B7).bd()),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.bg()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = Color(0xFF059669).fg(),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.employer_home_regular_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857).fg()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.employer_home_regular_subtitle),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B).fg(),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Active Postings Section
 * Shows current live job summary, applicant badge, and call CTA (single clean card, no nested outer card)
 */
@Composable
fun ActivePostingsCard(
    activeJob: JobListing?,
    applicantCount: Int,
    totalActiveJobsCount: Int = 1,
    onViewAllClick: () -> Unit,
    onViewApplicantsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewAllLabel = if (totalActiveJobsCount > 1) {
        stringResource(R.string.employer_home_view_all_count, totalActiveJobsCount)
    } else {
        stringResource(R.string.employer_home_view_all)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.employer_home_active_postings),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A).fg()
            )

            if (activeJob != null) {
                Text(
                    text = viewAllLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981).fg(),
                    modifier = Modifier.clickable { onViewAllClick() }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeJob != null) {
            val jobTitle = activeJob.title.ifBlank { "Job Vacancy" }
            val openings = if (activeJob.vacancies > 0) activeJob.vacancies else 1
            val openingsSuffix = if (openings == 1) stringResource(R.string.employer_home_opening_single) else stringResource(R.string.employer_home_opening_plural)
            val openingsText = "$jobTitle · $openings $openingsSuffix"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = openingsText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A).fg(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeJob.payText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981).fg()
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (applicantCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFECFDF5).bg(),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0).bd())
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).bg())
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = stringResource(R.string.employer_home_applied_count, applicantCount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857).fg()
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF1F5F9).bg(),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1).bd())
                            ) {
                                Text(
                                    text = stringResource(R.string.employer_home_live_now),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569).fg(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onViewApplicantsClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A).bg()),
                        shape = RoundedCornerShape(23.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        val ctaLabel = if (applicantCount > 0) {
                            stringResource(R.string.employer_home_view_applicants_cta, applicantCount)
                        } else {
                            stringResource(R.string.employer_home_view_vacancy_cta)
                        }
                        Text(
                            text = ctaLabel,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Actual working zero state when no jobs are posted yet
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.employer_home_no_vacancies_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A).fg()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.employer_home_no_vacancies_desc),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B).fg(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onViewApplicantsClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A).bg()),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.employer_home_post_first_vacancy),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick Role Templates Section: "HIRE IN 1 MINUTE (POPULAR ROLES)"
 * Driver, Helper, Cook / Maid, Shop Assistant (clean English descriptions)
 */
@Composable
fun QuickRoleTemplatesSection(
    onRoleSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val roles = listOf(
        Triple("🚚", "Driver", "Delivery & drive"),
        Triple("📦", "Helper", "Loading & packing"),
        Triple("🍳", "Cook / Maid", "Cooking & home"),
        Triple("🏪", "Shop Assistant", "Retail & counter"),
        Triple("🛡️", "Security Guard", "Guard & watchman"),
        Triple("⚡", "Electrician", "Wiring & repairs"),
        Triple("🧹", "Cleaner", "Cleaning & sweep"),
        Triple("🛵", "Delivery Boy", "Parcel & delivery"),
        Triple("🏗️", "Labour", "Construction work"),
        Triple("➕", "Other", "Any other role")
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.employer_home_hire_popular_roles),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8).fg(),
            letterSpacing = 0.8.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(roles) { (emoji, title, vernacular) ->
                Card(
                    modifier = Modifier
                        .width(105.dp)
                        .height(96.dp)
                        .clickable { onRoleSelected(title) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A).fg(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = vernacular,
                            fontSize = 10.sp,
                            color = Color(0xFF64748B).fg(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recent Call Requests
 * Horizontal cards of verified candidates with direct Call and WhatsApp buttons
 * If candidates list is empty, displays clean real empty state
 */
@Composable
fun RecentCallRequestsSection(
    candidates: List<CandidateDisplayItem>,
    onCallClick: (CandidateDisplayItem) -> Unit,
    onWhatsAppClick: (CandidateDisplayItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.employer_home_recent_call_requests),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A).fg()
            )

            if (candidates.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFECFDF5).bg(),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0).bd())
                ) {
                    Text(
                        text = stringResource(R.string.employer_home_live_tag),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669).fg(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (candidates.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(candidates) { candidate ->
                    Card(
                        modifier = Modifier.width(285.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Box with Verified Badge
                            Box(modifier = Modifier.size(44.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A).bg()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = candidate.initials,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // Green check badge
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).bg())
                                        .align(Alignment.BottomEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Candidate Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = candidate.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A).fg(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = "${candidate.trade} · ${candidate.distanceText}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B).fg(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "★ ${candidate.rating} (${candidate.jobsDone} jobs)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706).fg()
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Action Buttons: Call & WhatsApp
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    onClick = { onCallClick(candidate) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981).bg(),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Call",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { onWhatsAppClick(candidate) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF22C55E).bg(),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = "WhatsApp",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Actual Empty State when no candidates have called yet
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF).bg()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📞", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.employer_home_no_requests_title),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A).fg()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.employer_home_no_requests_desc),
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B).fg()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helpline Trust Strip: "Trouble creating a job? Call our support desk | Call Now"
 */
@Composable
fun HelplineTrustStrip(
    onCallNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.bg()),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9).bg()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headset,
                    contentDescription = null,
                    tint = Color(0xFF475569).fg(),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = stringResource(R.string.employer_home_helpline_text),
                fontSize = 12.sp,
                color = Color(0xFF475569).fg(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = stringResource(R.string.employer_home_call_now),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A).fg(),
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onCallNow() }
            )
        }
    }
}

/**
 * Helper to launch WhatsApp for a given phone number
 */
fun openWhatsAppChat(context: Context, rawPhone: String, defaultMessage: String = "Hello from DutyPe") {
    val cleanPhone = rawPhone.replace("[^0-9+]".toRegex(), "")
    val phoneWithCountry = if (cleanPhone.startsWith("+")) cleanPhone.removePrefix("+") else if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
    try {
        val uri = Uri.parse("https://wa.me/$phoneWithCountry?text=${Uri.encode(defaultMessage)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$rawPhone"))
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}


// ---------------------------------------------------------------------------
// Mockup "21 - Employer Home Dashboard" components (flat, no shadows)
// ---------------------------------------------------------------------------

private val EhBorder = Color(0xFFE2E8F0)
private val EhNavy = Color(0xFF0F172A)
private val EhInk = Color(0xFF0F0F0F)
private val EhCobalt = Color(0xFF2563EB)
private val EhRed = Color(0xFFDC2626)
private val EhSlate = Color(0xFF64748B)
private val EhMuted = Color(0xFF94A3B8)

/** Data for one applicant in a job card facepile. */
data class EmployerFacepileItem(val name: String, val photoUrl: String?)

@Composable
fun EmployerHomeHeader(
    companyName: String,
    unreadCount: Int,
    onNotificationClick: () -> Unit,
    onPostJobClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayName = companyName.ifBlank { "My Business" }
    val initial = displayName.trim().firstOrNull()?.uppercase() ?: "M"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.bg())
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF).bg()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EhCobalt.fg()
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EhInk.fg(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = EhInk.fg(),
                        modifier = Modifier.size(24.dp)
                    )
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EhRed.bg())
                                .border(1.5.dp, Color.White.bd(), CircleShape)
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EhBorder.bg())
        )
    }
}

@Composable
fun EmployerHeroActionCards(
    onPostRegularClick: () -> Unit,
    onPostUrgentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.bg())
                .border(1.dp, EhBorder.bd(), RoundedCornerShape(12.dp))
                .clickable { onPostRegularClick() }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.AssignmentOutlinedIcon,
                contentDescription = null,
                tint = EhNavy.fg(),
                modifier = Modifier.size(24.dp)
            )
            Text(stringResource(R.string.employer_home_post_regular_job), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EhInk.fg())
            Text(stringResource(R.string.employer_home_fill_vacancy), fontSize = 12.sp, color = EhSlate.fg())
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFEF2F2).bg())
                .border(1.dp, Color(0xFFFECACA).bd(), RoundedCornerShape(12.dp))
                .clickable { onPostUrgentClick() }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.FlashOn,
                contentDescription = null,
                tint = EhRed.fg(),
                modifier = Modifier.size(24.dp)
            )
            Text(stringResource(R.string.employer_home_post_urgent_need), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EhRed.fg())
            Text(stringResource(R.string.employer_home_find_worker_now), fontSize = 12.sp, color = EhMuted.fg())
        }
    }
}

@Composable
fun EmployerStatsRow(
    activeJobs: Int,
    applicants: Int,
    creditsLeft: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EmployerStatCell(stringResource(R.string.employer_home_active_jobs), activeJobs, Modifier.weight(1f))
        EmployerStatCell(stringResource(R.string.employer_home_applicants), applicants, Modifier.weight(1f))
        EmployerStatCell(stringResource(R.string.employer_home_credits_left), creditsLeft, Modifier.weight(1f))
    }
}

@Composable
private fun EmployerStatCell(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.bg())
            .border(1.dp, EhBorder.bd(), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
            color = EhMuted.fg(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EhInk.fg()
        )
    }
}

@Composable
fun EmployerSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = EhSlate.fg(),
        modifier = modifier
    )
}

@Composable
fun EmployerJobManagementCard(
    title: String,
    applicantCount: Int,
    isUrgent: Boolean,
    facepile: List<EmployerFacepileItem>,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.bg())
            .border(1.dp, EhBorder.bd(), shape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EhInk.fg(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$applicantCount ${stringResource(R.string.applicants_lower)}",
                    fontSize = 13.sp,
                    color = EhSlate.fg()
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isUrgent) Color(0xFFFEF2F2).bg() else Color(0xFFF0FDF4).bg())
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isUrgent) stringResource(R.string.urgent_caps) else stringResource(R.string.open_caps),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUrgent) EhRed.fg() else Color(0xFF16A34A).fg()
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmployerFacepile(facepile)
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(EhNavy.bg())
                    .clickable { onReviewClick() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.employer_home_review),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun EmployerFacepile(items: List<EmployerFacepileItem>) {
    val shown = items.take(3)
    if (shown.isEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        return
    }
    val pastel = listOf(Color(0xFFDBEAFE), Color(0xFFBBF7D0), Color(0xFFFDE68A), Color(0xFFFBCFE8))
    Box(modifier = Modifier.width((24 + 16 * (shown.size - 1)).dp).height(24.dp)) {
        shown.forEachIndexed { index, item ->
            Box(
                modifier = Modifier
                    .offset(x = (16 * index).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(pastel[index % pastel.size])
                    .border(2.dp, Color.White.bd(), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!item.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = item.name.trim().firstOrNull()?.uppercase() ?: "W",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EhNavy.fg()
                    )
                }
            }
        }
    }
}

@Composable
fun EmployerNoActiveJobsCard(onPostJobClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.bg())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(stringResource(R.string.employer_home_no_active_jobs), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = EhInk.fg())
        Text(stringResource(R.string.employer_home_no_active_jobs_sub), fontSize = 13.sp, color = EhSlate.fg())
    }
}

/**
 * First-time employer home: a short headline, one strong "Post a job" action, two quick ways to
 * hire (urgent / by voice) and a three-step "how it works". Calm, neutral and dark-mode ready.
 */
@Composable
fun EmployerWelcomeSection(
    ownerName: String,
    onPostJob: () -> Unit,
    onPostUrgent: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = Color(0xFF0F172A)
    val muted = Color(0xFF64748B)
    val line = Color(0xFFE5E7EB)
    Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = stringResource(R.string.employer_welcome_overline).uppercase(),
            color = muted.fg(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (ownerName.isNotBlank()) stringResource(R.string.employer_welcome_title_name, ownerName.trim().substringBefore(' '))
            else stringResource(R.string.employer_welcome_title),
            color = ink.fg(), fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.employer_welcome_body),
            color = muted.fg(), fontSize = 14.sp, lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Primary: post a job.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ink.bg())
                .clickable(onClick = onPostJob)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.employer_welcome_post), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(stringResource(R.string.employer_welcome_post_sub), color = Color(0xFFCBD5E1).fg(), fontSize = 13.sp)
            }
            Box(
                modifier = Modifier.size(40.dp).background(Color.White.bg(), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF0F172A).fg(), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Two quick ways to hire.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            WelcomeTile(
                icon = Icons.Filled.Bolt, tint = Color(0xFFEA580C).fg(), tile = Color(0xFFFFF7ED),
                title = stringResource(R.string.employer_welcome_urgent), subtitle = stringResource(R.string.employer_welcome_urgent_sub),
                onClick = onPostUrgent, modifier = Modifier.weight(1f)
            )
            WelcomeTile(
                icon = Icons.Filled.Mic, tint = Color(0xFF7C3AED).fg(), tile = Color(0xFFF5F3FF),
                title = stringResource(R.string.employer_welcome_voice), subtitle = stringResource(R.string.employer_welcome_voice_sub),
                onClick = onVoice, modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(26.dp))

        // How it works.
        Text(stringResource(R.string.employer_welcome_how), color = ink.fg(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))
        val steps = listOf(
            stringResource(R.string.employer_step1_title) to stringResource(R.string.employer_step1_body),
            stringResource(R.string.employer_step2_title) to stringResource(R.string.employer_step2_body),
            stringResource(R.string.employer_step3_title) to stringResource(R.string.employer_step3_body)
        )
        steps.forEachIndexed { index, (title, body) ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
                    Box(
                        modifier = Modifier.size(28.dp).background(Color.White.bg(), CircleShape).border(1.dp, line.bd(), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text((index + 1).toString(), color = ink.fg(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (index < steps.lastIndex) {
                        Box(modifier = Modifier.width(1.dp).weight(1f).background(line.bd()))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.padding(top = 3.dp, bottom = if (index < steps.lastIndex) 18.dp else 0.dp)) {
                    Text(title, color = ink.fg(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(body, color = muted.fg(), fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC).bg(), RoundedCornerShape(12.dp))
                .border(1.dp, line.bd(), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color(0xFF16A34A).fg(), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(stringResource(R.string.employer_welcome_trust), color = muted.fg(), fontSize = 12.sp, lineHeight = 17.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WelcomeTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    tile: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.bg())
            .border(1.dp, Color(0xFFE5E7EB).bd(), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).background(tile.bg(), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, color = Color(0xFF0F172A).fg(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = Color(0xFF64748B).fg(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
