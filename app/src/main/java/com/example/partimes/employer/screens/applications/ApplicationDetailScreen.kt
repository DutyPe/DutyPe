package com.example.partimes.employer.screens.applications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.partimes.models.*
import com.example.partimes.viewmodels.EmployerApplicationViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enterprise-level Application Detail Screen for Employers
 * Comprehensive view of worker application with all profile data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    applicationId: String,
    onBackClick: () -> Unit,
    onUpdateStatus: (ApplicationStatus, String?) -> Unit = { _, _ -> }
) {
    val viewModel: EmployerApplicationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<ApplicationStatus?>(null) }
    var statusNotes by remember { mutableStateOf("") }
    
    // Find the specific application
    val application = uiState.applications.find { it.applicationId == applicationId }
    
    if (application == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Application not found")
        }
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Application Details",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1F2937)
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { 
                        selectedStatus = application.status
                        showStatusDialog = true 
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Update Status",
                        tint = Color(0xFF3B82F6)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color(0xFF1F2937)
            )
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Application Status Card
            item {
                ApplicationStatusCard(application = application)
            }
            
            // Worker Basic Information
            item {
                WorkerBasicInfoCard(application = application)
            }
            
            // Work Experience
            if (application.workExperience.isNotEmpty()) {
                item {
                    WorkExperienceCard(workExperience = application.workExperience)
                }
            }
            
            // Education
            if (application.education.isNotEmpty()) {
                item {
                    EducationCard(education = application.education)
                }
            }
            
            // Skills & Certifications
            item {
                SkillsAndCertificationsCard(
                    skills = application.skills,
                    certifications = application.certifications,
                    languages = application.languages
                )
            }
            
            // Cover Letter
            if (application.coverLetter.isNotEmpty()) {
                item {
                    CoverLetterCard(coverLetter = application.coverLetter)
                }
            }
            
            // Documents
            if (application.additionalDocuments.isNotEmpty()) {
                item {
                    DocumentsCard(documents = application.additionalDocuments)
                }
            }
            
            // Job Information
            item {
                JobInformationCard(application = application)
            }
            
            // Application Timeline
            item {
                ApplicationTimelineCard(statusHistory = application.statusHistory)
            }
        }
    }
    
    // Status Update Dialog
    if (showStatusDialog) {
        StatusUpdateDialog(
            currentStatus = application.status,
            onDismiss = { showStatusDialog = false },
            onStatusUpdate = { newStatus, notes ->
                onUpdateStatus(newStatus, notes)
                showStatusDialog = false
            }
        )
    }
}

@Composable
private fun ApplicationStatusCard(application: JobApplication) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                Text(
                    text = "Application Status",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                
                StatusBadge(status = application.status)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Applied",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                    Text(
                        text = getTimeAgo(application.appliedAt),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1F2937)
                        )
                    )
                }
                
                application.lastViewedByEmployer?.let { viewedAt ->
                    Column {
                        Text(
                            text = "Last Viewed",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        Text(
                            text = getTimeAgo(viewedAt),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1F2937)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerBasicInfoCard(application: JobApplication) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Picture and Basic Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = application.workerName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    Text(
                        text = application.workerEmail,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                    application.workerPhone?.let { phone ->
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Additional Personal Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                application.workerLocation?.let { location ->
                    InfoItem(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        value = location
                    )
                }
                
                application.expectedSalary?.let { salary ->
                    InfoItem(
                        icon = Icons.Default.AttachMoney,
                        label = "Expected Salary",
                        value = salary
                    )
                }
            }
            
            application.availability?.let { availability ->
                Spacer(modifier = Modifier.height(12.dp))
                InfoItem(
                    icon = Icons.Default.Schedule,
                    label = "Availability",
                    value = availability
                )
            }
        }
    }
}

@Composable
private fun WorkExperienceCard(workExperience: List<WorkExperience>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Work Experience",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            workExperience.forEach { exp ->
                ExperienceItem(experience = exp)
                if (exp != workExperience.last()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ExperienceItem(experience: WorkExperience) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = experience.position,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = experience.company,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF3B82F6)
                    )
                )
                experience.location?.let { location ->
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${experience.startDate} - ${experience.endDate ?: "Present"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
                if (experience.isCurrent) {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = experience.description,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF374151),
                lineHeight = 20.sp
            )
        )
        
        if (experience.achievements.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Key Achievements:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
            )
            experience.achievements.forEach { achievement ->
                Text(
                    text = "• $achievement",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }
    }
}

@Composable
private fun EducationCard(education: List<Education>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Education",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            education.forEach { edu ->
                EducationItem(education = edu)
                if (edu != education.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun EducationItem(education: Education) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = education.degree,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            Text(
                text = education.institution,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF3B82F6)
                )
            )
            education.fieldOfStudy?.let { field ->
                Text(
                    text = field,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${education.startDate} - ${education.endDate ?: "Present"}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
            education.gpa?.let { gpa ->
                Text(
                    text = "GPA: $gpa",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun SkillsAndCertificationsCard(
    skills: List<String>,
    certifications: List<String>,
    languages: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Skills & Qualifications",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (skills.isNotEmpty()) {
                SkillsSection(title = "Skills", items = skills)
            }
            
            if (certifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                SkillsSection(title = "Certifications", items = certifications)
            }
            
            if (languages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                SkillsSection(title = "Languages", items = languages)
            }
        }
    }
}

@Composable
private fun SkillsSection(title: String, items: List<String>) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { skill ->
                SkillChip(skill = skill)
            }
        }
    }
}

@Composable
private fun SkillChip(skill: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = skill,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF3B82F6),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun CoverLetterCard(coverLetter: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Cover Letter",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = coverLetter,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF374151),
                    lineHeight = 22.sp
                )
            )
        }
    }
}

@Composable
private fun DocumentsCard(documents: List<DocumentAttachment>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Documents",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            documents.forEach { doc ->
                DocumentItem(document = doc)
                if (doc != documents.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DocumentItem(document: DocumentAttachment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Open document */ },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Attachment,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.fileName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF1F2937)
                )
            )
            Text(
                text = "${document.fileType.name} • ${formatFileSize(document.fileSize)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
        
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Download",
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun JobInformationCard(application: JobApplication) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Job Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoItem(
                icon = Icons.Default.Work,
                label = "Position",
                value = application.jobTitle
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoItem(
                icon = Icons.Default.Business,
                label = "Company",
                value = application.companyName
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoItem(
                icon = Icons.Default.LocationOn,
                label = "Location",
                value = application.jobLocation
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoItem(
                icon = Icons.Default.Schedule,
                label = "Job Type",
                value = application.jobType
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoItem(
                icon = Icons.Default.AttachMoney,
                label = "Pay Range",
                value = application.payInfo
            )
        }
    }
}

@Composable
private fun ApplicationTimelineCard(statusHistory: List<StatusUpdate>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Application Timeline",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            statusHistory.forEach { update ->
                TimelineItem(update = update)
                if (update != statusHistory.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(update: StatusUpdate) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (update.systemUpdate) Color(0xFF6B7280) else Color(0xFF3B82F6),
                    shape = CircleShape
                )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = getStatusDisplayName(update.status),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
            )
            Text(
                text = getTimeAgo(update.updatedAt),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
            update.notes?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF1F2937)
                )
            )
        }
    }
}

@Composable
private fun StatusUpdateDialog(
    currentStatus: ApplicationStatus,
    onDismiss: () -> Unit,
    onStatusUpdate: (ApplicationStatus, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(currentStatus) }
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Update Application Status")
        },
        text = {
            Column {
                Text("Select new status:")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ApplicationStatus.values().forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = status },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(getStatusDisplayName(status))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onStatusUpdate(selectedStatus, notes.takeIf { it.isNotEmpty() })
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun getStatusDisplayName(status: ApplicationStatus): String {
    return when (status) {
        ApplicationStatus.PENDING -> "Pending"
        ApplicationStatus.UNDER_REVIEW -> "Under Review"
        ApplicationStatus.REVIEWED -> "Reviewed"
        ApplicationStatus.SHORTLISTED -> "Shortlisted"
        ApplicationStatus.INTERVIEW_SCHEDULED -> "Interview Scheduled"
        ApplicationStatus.INTERVIEWED -> "Interviewed"
        ApplicationStatus.SELECTED -> "Selected"
        ApplicationStatus.REJECTED -> "Rejected"
        ApplicationStatus.WITHDRAWN -> "Withdrawn"
        ApplicationStatus.EXPIRED -> "Expired"
        ApplicationStatus.HIRED -> "Hired"
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

@Composable
private fun StatusBadge(status: ApplicationStatus) {
    val (backgroundColor, textColor, icon) = when (status) {
        ApplicationStatus.PENDING -> Triple(
            Color(0xFFFEF3C7),
            Color(0xFFD97706),
            Icons.Default.Schedule
        )
        ApplicationStatus.UNDER_REVIEW -> Triple(
            Color(0xFFE0E7FF),
            Color(0xFF3730A3),
            Icons.Default.Visibility
        )
        ApplicationStatus.SHORTLISTED -> Triple(
            Color(0xFFD1FAE5),
            Color(0xFF059669),
            Icons.Default.Star
        )
        ApplicationStatus.REJECTED -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFFDC2626),
            Icons.Default.Close
        )
        ApplicationStatus.HIRED -> Triple(
            Color(0xFFD1FAE5),
            Color(0xFF047857),
            Icons.Default.CheckCircle
        )
        else -> Triple(
            Color(0xFFF3F4F6),
            Color(0xFF6B7280),
            Icons.Default.Help
        )
    }
    
    Row(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
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
            text = getStatusDisplayName(status),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        )
    }
}
