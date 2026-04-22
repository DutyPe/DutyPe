package com.example.dutype.employer.screens.applications

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dutype.components.ApplicationListItemShimmer
import com.example.dutype.components.ApplicationStatusBadge
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.getDisplayName
import com.example.dutype.models.getStatusColor
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.utils.DateTimeUtils
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.di.rememberInAppReviewTriggerService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Enterprise-level Application Management Screen for Employers
 * Professional design with comprehensive application tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerApplicationManagementScreen(
    jobId: String? = null,
    onApplicationClick: (JobApplication) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: EmployerApplicationViewModel = hiltViewModel()
    val reviewTriggerService = rememberInAppReviewTriggerService()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showStatusFilter by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // FINTECH: Contact Unlock Dialog State
    var showUnlockDialog by remember { mutableStateOf(false) }
    var pendingUnlockApplication by remember { mutableStateOf<JobApplication?>(null) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var reportSummary by remember(jobId) { mutableStateOf<JobReportSummary?>(null) }
    var isReportSummaryLoading by remember(jobId) { mutableStateOf(false) }
    
    // Load applications based on whether it's for a specific job or all jobs
    LaunchedEffect(jobId) {
        if (jobId != null) {
            viewModel.loadJobApplications(jobId)
        } else {
            viewModel.loadEmployerApplications()
        }
    }

    LaunchedEffect(jobId) {
        if (jobId.isNullOrBlank()) {
            reportSummary = null
            isReportSummaryLoading = false
            return@LaunchedEffect
        }

        isReportSummaryLoading = true
        reportSummary = fetchJobReportSummary(jobId)
        isReportSummaryLoading = false
    }
    
    // Handle search
    LaunchedEffect(searchQuery) {
        viewModel.searchApplications(searchQuery)
    }
    
    // FINTECH: Contact Unlock Payment Dialog
    if (showUnlockDialog && pendingUnlockApplication != null) {
        ContactUnlockDialog(
            application = pendingUnlockApplication!!,
            unlockPrice = viewModel.getContactUnlockPrice(),
            isProcessing = isProcessingPayment,
            onDismiss = { 
                showUnlockDialog = false
                pendingUnlockApplication = null
            },
            onConfirmPayment = {
                isProcessingPayment = true
                viewModel.processContactUnlockPayment(
                    applicationId = pendingUnlockApplication!!.id,
                    onSuccess = {
                        isProcessingPayment = false
                        showUnlockDialog = false
                        pendingUnlockApplication = null
                        Toast.makeText(context, "Contact unlocked! ✅", Toast.LENGTH_SHORT).show()
                        val activity = context as? Activity
                        if (activity != null) {
                            reviewTriggerService.onEmployerContactUnlocked(activity)
                        }
                    },
                    onFailure = { error ->
                        isProcessingPayment = false
                        Toast.makeText(context, context.getString(R.string.payment_failed_error, error), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        // Common Header - consistent across all screens
        CommonHeader(
            title = if (jobId != null) "Job Applications" else "All Applications",
            onBackClick = onBackClick
        )

        if (jobId != null && (isReportSummaryLoading || reportSummary != null)) {
            JobReportSummaryCard(
                summary = reportSummary,
                isLoading = isReportSummaryLoading
            )
        }
        
        // FINTECH: Free contacts remaining banner
        if (uiState.freeContactsRemaining > 0 && uiState.applications.size > 3) {
            FreeContactsBanner(freeRemaining = uiState.freeContactsRemaining)
        }
        
        // Stats Summary Card
        ApplicationStatsSummary(stats = stats)
        
        // Applications List
        when {
            uiState.isLoading -> {
                // Show shimmer loading for application list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) {
                        ApplicationListItemShimmer()
                    }
                }
            }
            uiState.applications.isEmpty() && !uiState.isLoading -> {
                Box(modifier = Modifier.weight(1f)) {
                    EmptyApplicationsState()
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(uiState.applications) { index, application ->
                        val isContactUnlocked = viewModel.isContactUnlocked(application.id, index)
                        
                        ApplicationCard(
                            application = application,
                            applicationIndex = index,
                            isContactUnlocked = isContactUnlocked,
                            onClick = { 
                                // Update status to Shortlisted when employer clicks on application
                                if (application.status == ApplicationStatus.APPLIED) {
                                    viewModel.updateApplicationStatus(
                                        applicationId = application.id,
                                        newStatus = ApplicationStatus.SHORTLISTED,
                                        notes = "Application viewed by employer"
                                    )
                                }
                                onApplicationClick(application) 
                            },
                            onUnlockContact = {
                                viewModel.unlockContact(
                                    applicationId = application.id,
                                    onSuccess = {
                                        Toast.makeText(context, "Contact unlocked! ✅", Toast.LENGTH_SHORT).show()
                                        val activity = context as? Activity
                                        if (activity != null) {
                                            reviewTriggerService.onEmployerContactUnlocked(activity)
                                        }
                                    },
                                    onPaymentRequired = {
                                        pendingUnlockApplication = application
                                        showUnlockDialog = true
                                    }
                                )
                            },
                            onStatusUpdate = { newStatus, notes ->
                                viewModel.updateApplicationStatus(
                                    applicationId = application.id,
                                    newStatus = newStatus,
                                    notes = notes
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private data class ReportPreviewItem(
    val reportType: String,
    val description: String
)

private data class JobReportSummary(
    val reportCount: Int,
    val typeCounts: Map<String, Int>,
    val recentReports: List<ReportPreviewItem>
)

private suspend fun fetchJobReportSummary(jobId: String): JobReportSummary? {
    return try {
        val doc = FirebaseFirestore.getInstance()
            .collection("jobmetadata")
            .document(jobId)
            .get()
            .await()

        val data = doc.data ?: return null
        val reportCount = (data["reportCount"] as? Number)?.toInt() ?: 0
        if (reportCount <= 0) return null

        val typeCounts = mutableMapOf<String, Int>()
        val rawTypeCounts = data["reportTypeCounts"] as? Map<*, *>
        rawTypeCounts?.forEach { (rawType, rawCount) ->
            val type = rawType?.toString()?.trim().orEmpty().ifBlank { "OTHER" }
            val count = (rawCount as? Number)?.toInt() ?: 0
            if (count > 0) {
                typeCounts[type] = count
            }
        }

        val recentReports = (data["reportSamples"] as? List<*>)
            ?.mapNotNull { rawItem ->
                val itemMap = rawItem as? Map<*, *> ?: return@mapNotNull null
                val type = itemMap["reportType"]?.toString()?.trim().orEmpty().ifBlank { "OTHER" }
                val description = itemMap["description"]?.toString()?.trim().orEmpty()
                if (description.isBlank()) return@mapNotNull null
                ReportPreviewItem(reportType = type, description = description)
            }
            .orEmpty()

        JobReportSummary(
            reportCount = reportCount,
            typeCounts = typeCounts,
            recentReports = recentReports
        )
    } catch (_: FirebaseFirestoreException) {
        null
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun JobReportSummaryCard(
    summary: JobReportSummary?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Community Reports",
                style = AppTypography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )
            )

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFB91C1C)
                    )
                    Text(
                        text = "Loading report details...",
                        style = AppTypography.caption.copy(color = Color(0xFF7F1D1D))
                    )
                }
            } else if (summary != null) {
                Text(
                    text = "Reported by ${summary.reportCount} worker(s)",
                    style = AppTypography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7F1D1D)
                    )
                )

                if (summary.typeCounts.isNotEmpty()) {
                    summary.typeCounts.entries
                        .sortedByDescending { it.value }
                        .forEach { entry ->
                            Text(
                                text = "${entry.key}: ${entry.value}",
                                style = AppTypography.caption.copy(color = Color(0xFF7F1D1D))
                            )
                        }
                }

                if (summary.recentReports.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFFFECACA), thickness = 1.dp)
                    summary.recentReports.take(3).forEach { report ->
                        Text(
                            text = "${report.reportType}: ${report.description}",
                            style = AppTypography.bodySmall.copy(color = Color(0xFF7F1D1D))
                        )
                    }
                }
            }
        }
    }
}

// Stats Summary Card
@Composable
private fun ApplicationStatsSummary(stats: com.example.dutype.models.ApplicationStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatsSummaryItem(
                value = stats.totalApplications.toString(),
                label = stringResource(R.string.total_label),
                color = Color(0xFF3B82F6)
            )
            StatsSummaryItem(
                value = stats.appliedApplications.toString(),
                label = stringResource(R.string.applied),
                color = Color(0xFFF59E0B)
            )
            StatsSummaryItem(
                value = stats.shortlistedApplications.toString(),
                label = stringResource(R.string.shortlisted),
                color = Color(0xFF8B5CF6)
            )
            StatsSummaryItem(
                value = stats.hiredApplications.toString(),
                label = stringResource(R.string.hired),
                color = Color(0xFF10B981)
            )
        }
    }
}

@Composable
private fun StatsSummaryItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = AppTypography.statNumber.copy(color = color)
        )
        Text(
            text = label,
            style = AppTypography.caption.copy(color = Color(0xFF6B7280))
        )
    }
}

// NOTE: getStatusColor removed - use ApplicationStatus.getStatusColor() extension function
// Import: import com.example.dutype.models.getStatusColor

@Composable
private fun ApplicationCard(
    application: JobApplication,
    applicationIndex: Int = 0,
    isContactUnlocked: Boolean = true,
    onClick: () -> Unit,
    onUnlockContact: () -> Unit = {},
    onStatusUpdate: (ApplicationStatus, String?) -> Unit
) {
    val workerEmail = ""
    // Determine display name - fallback to "Unknown Worker" if name is empty
    val displayName = when {
        application.workerName.isNotBlank() -> application.workerName
        else -> "Unknown Worker"
    }
    
    // Get initials for avatar
    val initials = when {
        displayName.isNotBlank() && displayName != "Unknown Worker" -> {
            displayName.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .ifEmpty { displayName.take(1).uppercase() }
        }
        else -> "?"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row - Worker info and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Profile Avatar with image support
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!application.workerProfileImageUrl.isNullOrBlank()) {
                            // Show profile image if available
                            com.example.dutype.components.OptimizedProfileImage(
                                imageUrl = application.workerProfileImageUrl,
                                contentDescription = "Worker Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            // Show initials or icon
                            if (initials.isNotBlank() && initials != "?") {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B82F6)
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = displayName,
                            style = AppTypography.cardTitle.copy(color = Color(0xFF1F2937)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (workerEmail.isNotBlank()) {
                            Text(
                                text = workerEmail,
                                style = AppTypography.caption.copy(color = Color(0xFF6B7280)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Worker location removed - not stored on application
                    }
                }
                
                // Status Badge - using centralized component
                ApplicationStatusBadge(status = application.status)
            }
            
            // Bug #18 fix: surface the worker's skill snapshot directly on the
            // applicant card so employers can quickly judge fit without
            // opening the detail screen (worker_profiles is owner-only by
            // rules, so we depend on the denormalized snapshot from apply).
            if (application.workerSkills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    application.workerSkills.take(6).forEach { skill ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFFEEF2FF),
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = skill,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF3730A3),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                    if (application.workerSkills.size > 6) {
                        Text(
                            text = "+${application.workerSkills.size - 6}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF6B7280)
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Job Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(18.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = application.jobTitle,
                            style = AppTypography.labelLarge.copy(color = Color(0xFF1F2937)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = application.companyName,
                            style = AppTypography.caption.copy(color = Color(0xFF6B7280))
                        )
                    }
                }
            }
            
            // Skills preview removed - skills not stored on application
            val skillsToShow = emptyList<String>()
            
            if (skillsToShow.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    skillsToShow.forEach { skill ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = skill,
                                style = AppTypography.labelSmall.copy(
                                    color = Color(0xFF3B82F6)
                                ),
                                maxLines = 1
                            )
                        }
                    }
                    // +more indicator removed along with skills
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer - Applied time and contact info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Applied ${DateTimeUtils.formatRelativeTime(application.createdAt)}",
                        style = AppTypography.caption.copy(color = Color(0xFF9CA3AF))
                    )
                }
                
                // FINTECH: Contact Unlock - Phone display based on unlock status
                application.workerPhone?.let { phone ->
                    if (phone.isNotBlank()) {
                        if (isContactUnlocked) {
                            // Show phone number
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = phone,
                                    style = AppTypography.caption.copy(color = Color(0xFF10B981))
                                )
                            }
                        } else {
                            // Show locked contact with unlock button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                    .clickable { onUnlockContact() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.unlock_contact),
                                    style = AppTypography.caption.copy(
                                        color = Color(0xFFD97706),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Quick actions on list card (replaces hidden menu flow)
            if (application.status == ApplicationStatus.APPLIED || application.status == ApplicationStatus.SHORTLISTED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onStatusUpdate(ApplicationStatus.REJECTED, "Rejected from applications list") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.reject), style = AppTypography.labelLarge)
                    }

                    Button(
                        onClick = { onStatusUpdate(ApplicationStatus.HIRED, "Hired from applications list") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.accept), style = AppTypography.labelLarge, color = Color.White)
                    }
                }
            }
            
            // Cover Letter Preview (if available)
            if (application.coverLetter.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = application.coverLetter.take(120) + if (application.coverLetter.length > 120) "..." else "",
                    style = AppTypography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        lineHeight = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// NOTE: StatusBadge removed - use centralized ApplicationStatusBadge from components instead

@Composable
private fun EmptyApplicationsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFFF3F4F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "No Applications Yet",
            style = AppTypography.emptyStateTitle.copy(color = Color(0xFF1F2937))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Applications will appear here once workers start applying to your jobs.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        )
    }
}



// Helper functions
// NOTE: getStatusDisplayName removed - use ApplicationStatus.getDisplayName() extension function
// Import: import com.example.dutype.models.getDisplayName

// NOTE: getStatusIcon removed - use ApplicationStatus.getStatusIcon() extension function  
// Import: import com.example.dutype.models.getStatusIcon

// NOTE: getTimeAgo removed - use DateTimeUtils.formatRelativeTime() instead
// Import: import com.example.dutype.utils.DateTimeUtils

// ==================== FINTECH: CONTACT UNLOCK COMPONENTS ====================

/**
 * Banner showing free contacts remaining
 */
@Composable
private fun FreeContactsBanner(freeRemaining: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "🎁 $freeRemaining free contact unlocks remaining",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF065F46),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Dialog for unlocking contact with payment
 */
@Composable
private fun ContactUnlockDialog(
    application: JobApplication,
    unlockPrice: Int,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFFEF3C7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.unlock_contact),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.unlock_worker_contact_desc, application.workerName),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                )
                
                // Price card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.unlock_price),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280))
                        )
                        Text(
                            text = "₹$unlockPrice",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        )
                    }
                }
                
                // Benefits
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UnlockBenefitItem(stringResource(R.string.benefit_phone_number))
                    UnlockBenefitItem(stringResource(R.string.benefit_direct_communication))
                    UnlockBenefitItem(stringResource(R.string.benefit_faster_hiring))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPayment,
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.processing))
                } else {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pay_and_unlock, unlockPrice))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text(stringResource(R.string.cancel), color = Color(0xFF6B7280))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
private fun UnlockBenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF374151))
        )
    }
}
