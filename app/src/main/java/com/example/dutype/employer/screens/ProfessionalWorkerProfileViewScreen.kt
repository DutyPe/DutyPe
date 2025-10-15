package com.example.dutype.employer.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.getDisplayName
import com.example.dutype.services.JobApplicationService
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.utils.ScrollStateManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Professional Worker Profile View Screen
 * Enterprise-level worker profile viewing for employers with 30+ years of Android development experience
 * Provides comprehensive worker information, application details, and action management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalWorkerProfileViewScreen(
    navController: NavController,
    workerId: String,
    applicationId: String? = null,
    scrollStateManager: ScrollStateManager? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jobApplicationService: JobApplicationService = hiltViewModel()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val applicationStateManager: ApplicationStateManager = hiltViewModel()
    
    // State management
    var workerProfile by remember { mutableStateOf<WorkerProfileData?>(null) }
    var application by remember { mutableStateOf<JobApplication?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<ApplicationAction?>(null) }
    
    // Load worker profile and application data
    LaunchedEffect(workerId, applicationId) {
        try {
            isLoading = true
            error = null
            
            // Load application if provided
            applicationId?.let { appId ->
                // Load application details
                // This would typically come from a service call
                // For now, we'll simulate the data
                application = JobApplication(
                    applicationId = appId,
                    jobId = "sample_job",
                    workerId = workerId,
                    employerId = "current_employer",
                    workerName = "John Doe",
                    workerEmail = "john.doe@example.com",
                    workerPhone = "+1234567890",
                    jobTitle = "Software Developer",
                    companyName = "Tech Corp",
                    jobLocation = "San Francisco, CA",
                    jobType = "Full-time",
                    payInfo = "$80,000 - $100,000",
                    coverLetter = "I am excited to apply for this position...",
                    status = ApplicationStatus.PENDING,
                    appliedAt = System.currentTimeMillis() - 86400000 // 1 day ago
                )
            }
            
            // Load worker profile data
            // This would typically come from a service call
            workerProfile = WorkerProfileData(
                workerId = workerId,
                fullName = "John Doe",
                email = "john.doe@example.com",
                phone = "+1234567890",
                location = "San Francisco, CA",
                dateOfBirth = "1990-05-15",
                gender = "Male",
                profileImageUrl = null,
                experience = listOf(
                    WorkExperience(
                        company = "Tech Solutions Inc",
                        position = "Senior Developer",
                        duration = "2020 - Present",
                        description = "Led development of multiple web applications using React and Node.js"
                    ),
                    WorkExperience(
                        company = "StartupXYZ",
                        position = "Full Stack Developer",
                        duration = "2018 - 2020",
                        description = "Developed and maintained company's main product using modern web technologies"
                    )
                ),
                skills = listOf("React", "Node.js", "TypeScript", "Python", "AWS", "Docker"),
                education = listOf(
                    Education(
                        institution = "University of California",
                        degree = "Bachelor of Computer Science",
                        year = "2012"
                    )
                ),
                certifications = listOf(
                    "AWS Certified Developer",
                    "Google Cloud Professional",
                    "React Developer Certification"
                ),
                languages = listOf("English (Native)", "Spanish (Fluent)", "French (Basic)"),
                availability = "Available immediately",
                expectedSalary = "$80,000 - $100,000",
                resumeUrl = "https://example.com/resume.pdf",
                portfolioUrl = "https://johndoe.dev",
                linkedinUrl = "https://linkedin.com/in/johndoe",
                githubUrl = "https://github.com/johndoe"
            )
            
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }
    
    // Professional gradient background
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E3A8A), // Deep professional blue
            Color(0xFF3B82F6), // Bright blue
            Color(0xFFE0F2FE), // Light blue
            Color.White
        ),
        startY = 0f,
        endY = 1200f
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Professional Header
            ProfessionalWorkerProfileHeader(
                workerProfile = workerProfile,
                application = application,
                onBackClick = { navController.popBackStack() },
                onContactClick = { 
                    // Navigate to messaging
                    navController.navigate("message/$workerId")
                }
            )
            
            // Content
            if (isLoading) {
                LoadingWorkerProfileState()
            } else if (error != null) {
                ErrorWorkerProfileState(
                    error = error!!,
                    onRetry = {
                        scope.launch {
                            // Retry loading logic
                        }
                    }
                )
            } else if (workerProfile != null) {
                ScrollAwareLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 0.dp
                    ),
                    scrollStateManager = scrollStateManager
                ) {
                    // Application Status Card
                    application?.let { app ->
                        item {
                            ApplicationStatusCard(
                                application = app,
                                onUpdateStatus = { newStatus ->
                                    scope.launch {
                                        try {
                                            jobApplicationService.updateApplicationStatus(
                                                app.applicationId,
                                                newStatus,
                                                "employer" // updatedBy parameter
                                            )
                                            application = app.copy(status = newStatus)
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                    }
                                }
                            )
                        }
                    }
                    
                    // Personal Information
                    item {
                        PersonalInformationCard(workerProfile = workerProfile!!)
                    }
                    
                    // Work Experience
                    if (workerProfile!!.experience.isNotEmpty()) {
                        item {
                            WorkExperienceCard(experience = workerProfile!!.experience)
                        }
                    }
                    
                    // Skills
                    if (workerProfile!!.skills.isNotEmpty()) {
                        item {
                            SkillsCard(skills = workerProfile!!.skills)
                        }
                    }
                    
                    // Education
                    if (workerProfile!!.education.isNotEmpty()) {
                        item {
                            EducationCard(education = workerProfile!!.education)
                        }
                    }
                    
                    // Certifications
                    if (workerProfile!!.certifications.isNotEmpty()) {
                        item {
                            CertificationsCard(certifications = workerProfile!!.certifications)
                        }
                    }
                    
                    // Additional Information
                    item {
                        AdditionalInfoCard(workerProfile = workerProfile!!)
                    }
                    
                    // Action Buttons
                    item {
                        ActionButtonsCard(
                            application = application,
                            onActionClick = { action ->
                                selectedAction = action
                                showActionDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Action Dialog
    if (showActionDialog && selectedAction != null) {
        ApplicationActionDialog(
            action = selectedAction!!,
            workerName = workerProfile?.fullName ?: "Worker",
            onDismiss = { 
                showActionDialog = false
                selectedAction = null
            },
            onConfirm = { action ->
                scope.launch {
                    try {
                        when (action) {
                            ApplicationAction.SHORTLIST -> {
                                application?.let { app ->
                                    jobApplicationService.updateApplicationStatus(
                                        app.applicationId,
                                        ApplicationStatus.SHORTLISTED,
                                        "employer" // updatedBy parameter
                                    )
                                    application = app.copy(status = ApplicationStatus.SHORTLISTED)
                                }
                            }
                            ApplicationAction.REJECT -> {
                                application?.let { app ->
                                    jobApplicationService.updateApplicationStatus(
                                        app.applicationId,
                                        ApplicationStatus.REJECTED,
                                        "employer" // updatedBy parameter
                                    )
                                    application = app.copy(status = ApplicationStatus.REJECTED)
                                }
                            }
                            ApplicationAction.SCHEDULE_INTERVIEW -> {
                                // Navigate to interview scheduling
                                navController.navigate("schedule_interview/${application?.applicationId}")
                            }
                            ApplicationAction.SEND_MESSAGE -> {
                                // Navigate to messaging
                                navController.navigate("message/$workerId")
                            }
                        }
                        showActionDialog = false
                        selectedAction = null
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            }
        )
    }
}

@Composable
private fun ProfessionalWorkerProfileHeader(
    workerProfile: WorkerProfileData?,
    application: JobApplication?,
    onBackClick: () -> Unit,
    onContactClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color(0xFF3B82F6).copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF3B82F6)
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Worker Profile",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        )
                        Text(
                            text = "Review candidate information",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
                
                // Contact button
                Button(
                    onClick = onContactClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Contact", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            // Worker basic info
            workerProfile?.let { profile ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile image placeholder
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Color(0xFF3B82F6).copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = profile.fullName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        )
                        Text(
                            text = profile.email,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        Text(
                            text = profile.location,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF9CA3AF)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationStatusCard(
    application: JobApplication,
    onUpdateStatus: (ApplicationStatus) -> Unit
) {
    val statusColor = getStatusColor(application.status)
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Application Status",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = application.status.getDisplayName(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = statusColor
                        )
                    )
                }
            }
            
            Text(
                text = "Applied for ${application.jobTitle} at ${application.companyName}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                )
            )
            
            Text(
                text = "Applied on ${dateFormat.format(Date(application.appliedAt))}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF)
                )
            )
            
            // Cover letter preview
            if (application.coverLetter.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Cover Letter",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF374151)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = application.coverLetter,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalInformationCard(workerProfile: WorkerProfileData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            PersonalInfoRow("Email", workerProfile.email)
            PersonalInfoRow("Phone", workerProfile.phone)
            PersonalInfoRow("Location", workerProfile.location)
            PersonalInfoRow("Date of Birth", workerProfile.dateOfBirth)
            PersonalInfoRow("Gender", workerProfile.gender)
            PersonalInfoRow("Availability", workerProfile.availability)
            PersonalInfoRow("Expected Salary", workerProfile.expectedSalary)
        }
    }
}

@Composable
private fun PersonalInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F2937)
            )
        )
    }
}

@Composable
private fun WorkExperienceCard(experience: List<WorkExperience>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Work Experience",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            experience.forEach { exp ->
                ExperienceItem(experience = exp)
            }
        }
    }
}

@Composable
private fun ExperienceItem(experience: WorkExperience) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = experience.position,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = experience.duration,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
            
            Text(
                text = experience.company,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF3B82F6)
                )
            )
            
            Text(
                text = experience.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}

@Composable
private fun SkillsCard(skills: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Skills",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            // Skills chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(skills) { skill ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = skill,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF3B82F6)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EducationCard(education: List<Education>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Education",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            education.forEach { edu ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = edu.degree,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937)
                            )
                        )
                        Text(
                            text = edu.institution,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF3B82F6)
                            )
                        )
                        Text(
                            text = edu.year,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CertificationsCard(certifications: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Certifications",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            certifications.forEach { cert ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Certified",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF10B981)
                    )
                    Text(
                        text = cert,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF1F2937)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalInfoCard(workerProfile: WorkerProfileData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Additional Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            if (workerProfile.languages.isNotEmpty()) {
                Text(
                    text = "Languages: ${workerProfile.languages.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }
            
            if (workerProfile.resumeUrl.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = "Resume",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF3B82F6)
                    )
                    Text(
                        text = "Resume Available",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF3B82F6)
                        )
                    )
                }
            }
            
            if (workerProfile.portfolioUrl.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = "Portfolio",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF3B82F6)
                    )
                    Text(
                        text = "Portfolio Available",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF3B82F6)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsCard(
    application: JobApplication?,
    onActionClick: (ApplicationAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onActionClick(ApplicationAction.SHORTLIST) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shortlist", style = MaterialTheme.typography.bodySmall)
                }
                
                Button(
                    onClick = { onActionClick(ApplicationAction.SCHEDULE_INTERVIEW) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6)
                    )
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Interview", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onActionClick(ApplicationAction.SEND_MESSAGE) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3B82F6)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Message", style = MaterialTheme.typography.bodySmall)
                }
                
                OutlinedButton(
                    onClick = { onActionClick(ApplicationAction.REJECT) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDC2626)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ApplicationActionDialog(
    action: ApplicationAction,
    workerName: String,
    onDismiss: () -> Unit,
    onConfirm: (ApplicationAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (action) {
                    ApplicationAction.SHORTLIST -> "Shortlist Candidate"
                    ApplicationAction.REJECT -> "Reject Application"
                    ApplicationAction.SCHEDULE_INTERVIEW -> "Schedule Interview"
                    ApplicationAction.SEND_MESSAGE -> "Send Message"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Text(
                text = when (action) {
                    ApplicationAction.SHORTLIST -> "Are you sure you want to shortlist $workerName for this position?"
                    ApplicationAction.REJECT -> "Are you sure you want to reject $workerName's application?"
                    ApplicationAction.SCHEDULE_INTERVIEW -> "Do you want to schedule an interview with $workerName?"
                    ApplicationAction.SEND_MESSAGE -> "Do you want to send a message to $workerName?"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(action) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (action) {
                        ApplicationAction.SHORTLIST -> Color(0xFF10B981)
                        ApplicationAction.REJECT -> Color(0xFFDC2626)
                        ApplicationAction.SCHEDULE_INTERVIEW -> Color(0xFF8B5CF6)
                        ApplicationAction.SEND_MESSAGE -> Color(0xFF3B82F6)
                    }
                )
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LoadingWorkerProfileState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFF3B82F6)
            )
            Text(
                text = "Loading worker profile...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}

@Composable
private fun ErrorWorkerProfileState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFDC2626)
                )
                Text(
                    text = "Failed to load worker profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    ),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

// Data classes
data class WorkerProfileData(
    val workerId: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val location: String,
    val dateOfBirth: String,
    val gender: String,
    val profileImageUrl: String?,
    val experience: List<WorkExperience>,
    val skills: List<String>,
    val education: List<Education>,
    val certifications: List<String>,
    val languages: List<String>,
    val availability: String,
    val expectedSalary: String,
    val resumeUrl: String,
    val portfolioUrl: String,
    val linkedinUrl: String,
    val githubUrl: String
)

data class WorkExperience(
    val company: String,
    val position: String,
    val duration: String,
    val description: String
)

data class Education(
    val institution: String,
    val degree: String,
    val year: String
)

enum class ApplicationAction {
    SHORTLIST,
    REJECT,
    SCHEDULE_INTERVIEW,
    SEND_MESSAGE
}

// Helper function to get status color
private fun getStatusColor(status: ApplicationStatus): Color {
    return when (status) {
        ApplicationStatus.PENDING -> Color(0xFFF59E0B)
        ApplicationStatus.UNDER_REVIEW -> Color(0xFF3B82F6)
        ApplicationStatus.REVIEWED -> Color(0xFF3B82F6)
        ApplicationStatus.SHORTLISTED -> Color(0xFF10B981)
        ApplicationStatus.INTERVIEW_SCHEDULED -> Color(0xFF8B5CF6)
        ApplicationStatus.INTERVIEWED -> Color(0xFF6366F1)
        ApplicationStatus.SELECTED -> Color(0xFF059669)
        ApplicationStatus.REJECTED -> Color(0xFFDC2626)
        ApplicationStatus.WITHDRAWN -> Color(0xFF6B7280)
        ApplicationStatus.EXPIRED -> Color(0xFF9CA3AF)
        ApplicationStatus.HIRED -> Color(0xFF047857)
    }
}
