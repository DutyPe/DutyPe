package com.example.dutype.employer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import androidx.navigation.compose.rememberNavController
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.getDisplayName
import com.example.dutype.models.getStatusColor
import com.example.dutype.services.JobApplicationService
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    // Services accessed via ViewModels (proper DI pattern)
    val jobApplicationViewModel: com.example.dutype.viewmodels.JobApplicationViewModel = hiltViewModel()
    val jobApplicationService = jobApplicationViewModel.jobApplicationService
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    
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
                // Load application details from service
                val applicationsResult = jobApplicationService.getApplicationById(appId)
                applicationsResult.onSuccess { appResult: com.example.dutype.models.JobApplication? ->
                    val app = appResult
                    if (app != null) {
                        application = app
                        
                        // Build worker profile from application data
                        val experienceList: List<WorkExperienceDisplay> = if (app.workExperience.isNotEmpty()) {
                            app.workExperience.map { exp: com.example.dutype.models.WorkExperience ->
                                WorkExperienceDisplay(
                                    company = exp.company,
                                    position = exp.position,
                                    duration = "${exp.startDate} - ${exp.endDate ?: "Present"}",
                                    description = exp.description
                                )
                            }
                        } else if (!app.workExperienceText.isNullOrBlank()) {
                            app.workExperienceText!!.split(",").map { it.trim() }.filter { it.isNotBlank() }.map { expStr: String ->
                                WorkExperienceDisplay(
                                    company = "",
                                    position = expStr,
                                    duration = "",
                                    description = ""
                                )
                            }
                        } else {
                            emptyList()
                        }
                        
                        val skillsList: List<String> = if (app.skills.isNotEmpty()) {
                            app.skills
                        } else if (!app.skillsText.isNullOrBlank()) {
                            app.skillsText!!.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        } else {
                            emptyList()
                        }
                        
                        val educationList: List<EducationDisplay> = app.education.map { edu: com.example.dutype.models.Education ->
                            EducationDisplay(
                                institution = edu.institution,
                                degree = edu.degree,
                                year = edu.endDate ?: edu.startDate
                            )
                        }
                        
                        workerProfile = WorkerProfileData(
                            workerId = app.workerId,
                            fullName = app.workerName.ifBlank { "Unknown Worker" },
                            email = app.workerEmail,
                            phone = app.workerPhone ?: "",
                            location = app.workerLocation ?: "",
                            dateOfBirth = app.workerDateOfBirth ?: "",
                            gender = app.workerGender ?: "",
                            profileImageUrl = app.workerProfileImageUrl,
                            experience = experienceList,
                            skills = skillsList,
                            education = educationList,
                            certifications = app.certifications,
                            languages = app.languages,
                            availability = app.availability ?: "",
                            expectedSalary = app.expectedSalary ?: "",
                            resumeUrl = app.resumeUrl ?: "",
                            portfolioUrl = "",
                            linkedinUrl = "",
                            githubUrl = ""
                        )
                    } else {
                        error = "Application not found"
                    }
                }.onFailure { e: Throwable ->
                    error = e.message
                }
            }
            
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
                                                app.id,
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
                                        app.id,
                                        ApplicationStatus.ACCEPTED,
                                        "employer" // updatedBy parameter
                                    )
                                    application = app.copy(status = ApplicationStatus.ACCEPTED)
                                }
                            }
                            ApplicationAction.REJECT -> {
                                application?.let { app ->
                                    jobApplicationService.updateApplicationStatus(
                                        app.id,
                                        ApplicationStatus.REJECTED,
                                        "employer" // updatedBy parameter
                                    )
                                    application = app.copy(status = ApplicationStatus.REJECTED)
                                }
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
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
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
                    // Profile image with actual image support
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Color(0xFF3B82F6).copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile.profileImageUrl.isNullOrBlank()) {
                            com.example.dutype.components.OptimizedProfileImage(
                                imageUrl = profile.profileImageUrl,
                                contentDescription = "Worker Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            // Show initials or icon
                            val initials = profile.fullName.split(" ")
                                .take(2)
                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                .joinToString("")
                                .ifEmpty { profile.fullName.take(1).uppercase() }
                            
                            if (initials.isNotBlank()) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B82F6)
                                    )
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
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
    val statusColor = application.status.getStatusColor()
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
private fun WorkExperienceCard(experience: List<WorkExperienceDisplay>) {
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
private fun ExperienceItem(experience: WorkExperienceDisplay) {
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
private fun EducationCard(education: List<EducationDisplay>) {
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shortlist", style = MaterialTheme.typography.bodySmall)
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
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
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
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
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
                        ApplicationAction.SEND_MESSAGE -> Color(0xFF3B82F6)
                    }
                )
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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

// Screen-specific data classes for UI display
// NOTE: These are LOCAL to this screen and different from models/JobApplicationModels.kt
// They have simplified fields for display purposes only

data class WorkerProfileData(
    val workerId: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val location: String,
    val dateOfBirth: String,
    val gender: String,
    val profileImageUrl: String?,
    val experience: List<WorkExperienceDisplay>,
    val skills: List<String>,
    val education: List<EducationDisplay>,
    val certifications: List<String>,
    val languages: List<String>,
    val availability: String,
    val expectedSalary: String,
    val resumeUrl: String,
    val portfolioUrl: String,
    val linkedinUrl: String,
    val githubUrl: String
)

/**
 * Simplified work experience for display only
 * Different from models/JobApplicationModels.WorkExperience which has more fields
 */
data class WorkExperienceDisplay(
    val company: String,
    val position: String,
    val duration: String,
    val description: String
)

/**
 * Simplified education for display only
 * Different from models/JobApplicationModels.Education which has more fields
 */
data class EducationDisplay(
    val institution: String,
    val degree: String,
    val year: String
)

enum class ApplicationAction {
    SHORTLIST,
    REJECT,
    SEND_MESSAGE
}

// NOTE: getStatusColor removed - use ApplicationStatus.getStatusColor() extension from models instead

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfessionalWorkerProfileViewScreenPreview() {
    ProfessionalWorkerProfileViewScreen(navController = rememberNavController(), workerId = "sample_worker_id")
}
