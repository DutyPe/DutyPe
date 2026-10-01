package com.example.dutype.employer.screens

import com.dutype.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dutype.viewmodels.EmployerJobsViewModel
import com.example.dutype.viewmodels.InstantHelpViewModel
import com.example.dutype.models.InstantRequest
import com.example.dutype.models.JobListing
import com.example.dutype.models.ApplicationStats
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.employer.screens.applications.EmployerApplicationManagementScreen
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.components.CommonHeader
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.employer.models.JobStats
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import androidx.compose.ui.res.stringResource

private fun InstantRequest.toJobListing(): JobListing {
    return JobListing(
        id = requestId,
        employerId = employerId,
        title = "[Urgent] $title",
        payAmount = budgetText.filter { it.isDigit() }.toLongOrNull() ?: 0L,
        payType = com.example.dutype.firestore.FirestoreSchema.Values.PayType.DAILY,
        employmentType = com.example.dutype.firestore.FirestoreSchema.Values.EmploymentType.DAILY,
        geohash = geohash,
        urgency = com.example.dutype.firestore.FirestoreSchema.Values.Urgency.HIGH,
        status = status,
        createdAt = createdAt,
        expiresAt = expiresAt,
        lat = lat,
        lng = lng,
        companyName = employerName,
        description = description,
        addressText = addressText,
        contactNumber = contactNumber,
        vacancies = workersNeeded
    )
}

private val HiBg = Color(0xFFF8FAFC)
private val HiInk = Color(0xFF0F172A)
private val HiBlack = Color(0xFF0F0F0F)
private val HiBorder = Color(0xFFE2E8F0)
private val HiMuted = Color(0xFF94A3B8)
private val HiGreen = Color(0xFF10B981)
private val HiRanges = listOf(R.string.this_week, R.string.this_month, R.string.period_3_months)
private val HiRangeDays = listOf(7, 30, 90)
private val HiDays = listOf(R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu, R.string.day_fri, R.string.day_sat, R.string.day_sun)

// PLACEHOLDER scale used when there is no application data yet (mock design values)
private val HiMockBars = listOf(70f, 96f, 58f, 140f, 110f, 44f, 32f)

@Composable
fun AnalyticsScreen(navController: NavController) {
    val viewModel: EmployerJobsViewModel = hiltViewModel()

    val applicationViewModel: EmployerApplicationViewModel = hiltViewModel()
    val appUiState by applicationViewModel.uiState.collectAsStateWithLifecycle()

    val instantHelpViewModel: InstantHelpViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.loadMyJobs()
        applicationViewModel.loadEmployerApplications()
        instantHelpViewModel.loadEmployerUrgentNeeds()
    }

    var rangeIndex by rememberSaveable { mutableStateOf(0) }
    val selected = rangeIndex
    val cutoff = System.currentTimeMillis() - HiRangeDays[selected] * 86_400_000L
    val inRange = appUiState.applications.filter { it.createdAt >= cutoff }
    val applicationsCount = inRange.size
    val hiredCount = inRange.count { it.status == ApplicationStatus.HIRED }
    val weekdayCounts = hiWeekdayCounts(inRange)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HiBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
    ) {
        HiTitleRow(navController)
        Spacer(modifier = Modifier.height(16.dp))
        HiFilterChips(selected = selected, onSelect = { rangeIndex = it })
        Spacer(modifier = Modifier.height(20.dp))
        HiKpiGrid(applications = applicationsCount, hired = hiredCount)
        Spacer(modifier = Modifier.height(16.dp))
        HiBarChartCard(counts = weekdayCounts)
        Spacer(modifier = Modifier.height(16.dp))
        HiInsightCard()
    }
}

private fun hiWeekdayCounts(apps: List<JobApplication>): List<Int> {
    val counts = IntArray(7)
    val cal = Calendar.getInstance()
    apps.forEach {
        cal.timeInMillis = it.createdAt
        val idx = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        counts[idx] = counts[idx] + 1
    }
    return counts.toList()
}

@Composable
private fun HiTitleRow(navController: NavController) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = HiInk,
            modifier = Modifier
                .size(24.dp)
                .clickable { navController.popBackStack() }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.hiring_insights),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = HiInk
        )
    }
}

@Composable
private fun HiFilterChips(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HiRanges.forEachIndexed { index, labelRes ->
            HiChip(label = stringResource(labelRes), active = index == selected, onClick = { onSelect(index) })
        }
    }
}

@Composable
private fun HiChip(label: String, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val base = Modifier
        .height(36.dp)
        .clip(shape)
        .background(if (active) HiBlack else Color.White, shape)
    val bordered = if (active) base else base.border(1.dp, HiBorder, shape)
    Box(
        modifier = bordered
            .clickable { onClick() }
            .padding(start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) Color.White else HiInk
        )
    }
}

@Composable
private fun HiKpiGrid(applications: Int, hired: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // PLACEHOLDER: job views are not tracked yet
            HiKpiCard(stringResource(R.string.job_views), "1,240", "↑ 12%", Modifier.weight(1f))
            // Real count; delta is a placeholder
            HiKpiCard(stringResource(R.string.applications), applications.toString(), "↑ 8%", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Real count; delta is a placeholder
            HiKpiCard(stringResource(R.string.workers_hired), hired.toString(), "↑ 5%", Modifier.weight(1f))
            // PLACEHOLDER: response time is not tracked yet
            HiKpiCard(stringResource(R.string.avg_response), stringResource(R.string.eighteen_min), stringResource(R.string.delta_better), Modifier.weight(1f))
        }
    }
}

@Composable
private fun HiKpiCard(label: String, value: String, delta: String, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .background(Color.White, shape)
            .border(1.dp, HiBorder, shape)
            .padding(16.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = HiMuted)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = HiBlack)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = delta, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HiGreen)
    }
}

@Composable
private fun HiBarChartCard(counts: List<Int>) {
    val shape = RoundedCornerShape(16.dp)
    val maxCount = counts.maxOrNull() ?: 0
    val heights: List<Float> = if (maxCount <= 0) {
        HiMockBars
    } else {
        counts.map { maxOf(4f, it.toFloat() / maxCount * 140f) }
    }
    val maxIndex = heights.indexOf(heights.maxOrNull() ?: 0f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape)
            .border(1.dp, HiBorder, shape)
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.weekly_applications),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = HiBlack
        )
        Spacer(modifier = Modifier.height(16.dp))
        HiBars(heights = heights, highlight = maxIndex)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HiBorder)
        )
        Spacer(modifier = Modifier.height(8.dp))
        HiBarLabels()
    }
}

@Composable
private fun HiBars(heights: List<Float>, highlight: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEachIndexed { index, h ->
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(h.dp)
                    .background(
                        if (index == highlight) HiInk else HiBorder,
                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                    )
            )
        }
    }
}

@Composable
private fun HiBarLabels() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HiDays.forEach { dayRes ->
            Text(
                text = stringResource(dayRes),
                modifier = Modifier.width(24.dp),
                fontSize = 11.sp,
                color = HiMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun HiInsightCard() {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF6FF), shape)
            .border(1.dp, Color(0xFFBFDBFE), shape)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Text(
            text = stringResource(R.string.hiring_insight_tip),
            fontSize = 13.sp,
            color = Color(0xFF1D4ED8)
        )
    }
}

// Overview Stats Section - Clean grid layout
@Composable
fun OverviewStatsSection(
    jobStats: JobStats,
    activeJobs: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.auto_job_overview),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
            )
        )

        // First row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = stringResource(R.string.active_jobs),
                value = activeJobs.toString(),
                icon = Icons.Default.Work,
                color = EmployerColors.Success,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.total_jobs),
                value = jobStats.totalJobs.toString(),
                icon = Icons.Default.Analytics,
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )
        }

        // Second row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = stringResource(R.string.todays_posts),
                value = jobStats.todayJobs.toString(),
                icon = Icons.Default.CalendarToday,
                color = EmployerColors.Primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Application Stats Card
@Composable
fun ApplicationStatsCard(appStats: ApplicationStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.ChipBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_application_summary),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ApplicationStatItem(
                    label = stringResource(R.string.total_label),
                    value = appStats.totalApplications.toString(),
                    color = EmployerColors.Primary
                )
                ApplicationStatItem(
                    label = stringResource(R.string.applied),
                    value = appStats.appliedApplications.toString(),
                    color = EmployerColors.Warning
                )
                ApplicationStatItem(
                    label = stringResource(R.string.shortlisted),
                    value = appStats.totalApplications.toString(),
                    color = Color(0xFF8B5CF6)
                )
                ApplicationStatItem(
                    label = stringResource(R.string.hired),
                    value = appStats.hiredApplications.toString(),
                    color = EmployerColors.Success
                )
            }
        }
    }
}

@Composable
private fun ApplicationStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = EmployerColors.TextSecondary
            )
        )
    }
}

// Recent Applications Section
@Composable
fun RecentApplicationsSection(
    applications: List<JobApplication>,
    navController: NavController
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auto_recent_applications),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                    )
                )
                TextButton(
                    onClick = { navController.navigate(com.example.dutype.navigation.Routes.EMPLOYER_APPLICATIONS) }
                ) {
                    Text(
                        text = stringResource(R.string.auto_view_all),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = EmployerColors.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            if (applications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = EmployerColors.TextTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.auto_no_applications_yet),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = EmployerColors.TextSecondary
                            )
                        )
                    }
                }
            } else {
                applications.take(5).forEach { application ->
                    RecentApplicationItem(
                        application = application,
                        onClick = {
                            navController.navigate(com.example.dutype.navigation.Routes.workerProfileViewRoute(application.workerId))
                        }
                    )
                }
            }
        }
    }
}

// Recent Jobs Activity Section
@Composable
fun RecentJobsActivitySection(jobs: List<JobListing>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_recent_job_activity),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                )
            )

            if (jobs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = EmployerColors.TextTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.auto_no_jobs_posted_yet),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = EmployerColors.TextSecondary
                            )
                        )
                    }
                }
            } else {
                jobs.take(5).forEach { job ->
                    JobActivityItem(job = job)
                }
            }
        }
    }
}

@Composable
private fun JobActivityItem(job: JobListing) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmployerColors.ChipBackground, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    EmployerColors.Success.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Work,
                contentDescription = null,
                tint = EmployerColors.Success,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.posted_time_ago_format, DateTimeUtils.formatRelativeTime(job.createdAt)),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EmployerColors.TextSecondary
                )
            )
        }

        // Status badge
        Box(
            modifier = Modifier
                .background(
                    EmployerColors.SuccessLight,
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_open),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = EmployerColors.Success
                )
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EmployerColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}


@Composable
fun AnalyticsItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
            )
        }
    }
}

@Composable
fun ActivityItem(
    title: String,
    time: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EmployerColors.TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
                Text(
                text = time,
                style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
            )
        }
    }
}

// NOTE: getTimeAgo() removed - use DateTimeUtils.formatRelativeTime() instead
// Import: import com.example.dutype.utils.DateTimeUtils



@Composable
fun RecentApplicationItem(
    application: JobApplication,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.ChipBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.worker_id_format, application.workerId.takeLast(6)),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = stringResource(R.string.job_id_format, application.jobId.takeLast(6)),
                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                )
            }

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (application.status) {
                        ApplicationStatus.APPLIED -> EmployerColors.WarningLight
                        ApplicationStatus.HIRED -> EmployerColors.SuccessLight
                        ApplicationStatus.COMPLETED -> EmployerColors.SuccessLight
                        ApplicationStatus.REJECTED -> EmployerColors.ErrorLight
                        ApplicationStatus.WITHDRAWN -> EmployerColors.ChipBackground
                        else -> EmployerColors.InfoLight
                    }
                )
            ) {
                Text(
                    text = application.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = when (application.status) {
                            ApplicationStatus.APPLIED -> EmployerColors.Warning
                            ApplicationStatus.HIRED -> EmployerColors.Success
                            ApplicationStatus.COMPLETED -> EmployerColors.Success
                            ApplicationStatus.REJECTED -> EmployerColors.Error
                            ApplicationStatus.WITHDRAWN -> Color(0xFF4B5563)
                            else -> EmployerColors.Info
                        }
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// NOTE: isToday() removed - use DateTimeUtils.isToday() instead
// Import: import com.example.dutype.utils.DateTimeUtils

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AnalyticsScreenPreview() {
    AnalyticsScreen(navController = rememberNavController())
}
