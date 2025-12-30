package com.example.dutype.worker.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.JobApplication
import com.example.dutype.models.JobListing
import com.example.dutype.models.StatusUpdate
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.JobApplicationViewModel
import com.example.dutype.viewmodels.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
import java.util.UUID

/**
 * Smart Job Application Screen that fetches real job data
 * and handles the complete application submission flow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartJobApplicationScreen(
    jobId: String,
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val applicationViewModel: JobApplicationViewModel = hiltViewModel()
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val applicationUiState by applicationViewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    // Job data state
    var job by remember { mutableStateOf<JobListing?>(null) }
    var isLoadingJob by remember { mutableStateOf(true) }
    var jobError by remember { mutableStateOf<String?>(null) }
    
    // Check if already applied
    var hasAlreadyApplied by remember { mutableStateOf(false) }
    var existingApplicationStatus by remember { mutableStateOf<String?>(null) }

    // Form state
    var coverLetter by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    // Ensure status bar color is white for this screen
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }
    
    // Check if user has already applied to this job
    LaunchedEffect(jobId, currentUser, applicationUiState.applications) {
        if (currentUser != null && jobId.isNotEmpty()) {
            val existingApplication = applicationUiState.applications.find { 
                it.jobId == jobId && 
                it.status.name != "WITHDRAWN" && 
                it.status.name != "REJECTED" 
            }
            if (existingApplication != null) {
                hasAlreadyApplied = true
                existingApplicationStatus = existingApplication.status.name
            }
        }
    }
    
    // Load applications to check for existing
    LaunchedEffect(Unit) {
        applicationViewModel.loadMyApplications()
    }

    // Fetch job details
    LaunchedEffect(jobId) {
        if (jobId.isNotEmpty()) {
            try {
                val result = jobViewModel.getJobById(jobId)
                result.fold(
                    onSuccess = { fetchedJob ->
                        job = fetchedJob
                        isLoadingJob = false
                    },
                    onFailure = { exception ->
                        jobError = exception.message ?: "Failed to load job details"
                        isLoadingJob = false
                    }
                )
            } catch (e: Exception) {
                jobError = e.message ?: "Failed to load job details"
                isLoadingJob = false
            }
        }
    }

    // Load user profile
    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    // Pre-fill form with user's default cover letter
    LaunchedEffect(profileUiState.user) {
        val user = profileUiState.user
        if (user != null && coverLetter.isEmpty()) {
            coverLetter = user.coverLetter ?: ""
        }
    }

    // Handle application submission success
    LaunchedEffect(applicationUiState.submissionSuccess) {
        if (applicationUiState.submissionSuccess) {
            Toast.makeText(context, "Application submitted successfully!", Toast.LENGTH_SHORT).show()
            // Navigate back to worker home screen
            navController.navigate(Routes.WORKER_HOME) {
                popUpTo(Routes.WORKER_HOME) { inclusive = true }
            }
        }
    }
    
    // If already applied, show message and redirect
    if (hasAlreadyApplied) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "You have already applied to this job", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Apply for Job",
                        style = AppTypography.screenTitle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF111827)
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoadingJob -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            jobError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error loading job details",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Red
                        )
                        jobError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Go Back")
                        }
                    }
                }
            }

            job != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFFF8FAFC))
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    job?.let { currentJob ->
                        // 1. Profile preview FIRST - show worker's profile info
                        ProfilePreviewCard(
                            workerName = profileUiState.user?.fullName ?: currentUser?.displayName ?: "Your Name",
                            workerEmail = profileUiState.user?.email ?: currentUser?.email ?: "",
                            workerPhone = profileUiState.user?.getPhoneDisplay() ?: "",
                            workerLocation = profileUiState.user?.getAddressDisplay() ?: "",
                            workerGender = profileUiState.user?.gender,
                            workerDateOfBirth = profileUiState.user?.dateOfBirth,
                            workerSkills = profileUiState.user?.skills,
                            workerExperience = profileUiState.user?.experience,
                            profileImageUrl = profileUiState.user?.profileImageUrl ?: currentUser?.photoUrl?.toString(),
                            resumeUrl = profileUiState.user?.resumeUrl
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Application form SECOND
                        ApplicationForm(
                            coverLetter = coverLetter,
                            onCoverLetterChange = { coverLetter = it },
                            additionalNotes = additionalNotes,
                            onAdditionalNotesChange = { additionalNotes = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Job information card THIRD
                        JobInfoCard(
                            jobTitle = currentJob.title,
                            companyName = currentJob.companyName,
                            jobLocation = currentJob.location,
                            jobType = currentJob.jobType,
                            payInfo = getPayInfo(currentJob)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Submit button
                        SubmitApplicationButton(
                            isSubmitting = applicationUiState.isSubmitting,
                            onSubmit = {
                                if (currentUser != null) {
                                    val user = profileUiState.user
                                    val application = JobApplication(
                                        applicationId = UUID.randomUUID().toString(),
                                        jobId = jobId,
                                        workerId = currentUser.uid,
                                        employerId = currentJob.employerId,
                                        status = ApplicationStatus.PENDING,
                                        statusHistory = listOf(
                                            StatusUpdate(
                                                status = ApplicationStatus.PENDING,
                                                updatedAt = System.currentTimeMillis(),
                                                updatedBy = currentUser.uid,
                                                notes = "Application submitted",
                                                systemUpdate = true
                                            )
                                        ),
                                        // Worker information
                                        workerName = user?.fullName ?: currentUser.displayName ?: "",
                                        workerEmail = user?.email ?: currentUser.email ?: "",
                                        workerPhone = user?.getPhoneDisplay(),
                                        workerProfileImageUrl = user?.profileImageUrl ?: currentUser.photoUrl?.toString(),
                                        workerLocation = user?.getAddressDisplay(),
                                        workerDateOfBirth = user?.dateOfBirth,
                                        workerGender = user?.gender,

                                        // Professional information
                                        workExperience = emptyList(),
                                        workExperienceText = user?.experience,
                                        skills = user?.getSkillsList() ?: emptyList(),
                                        skillsText = user?.skills,
                                        education = emptyList(),
                                        certifications = emptyList(),
                                        languages = emptyList(),
                                        availability = null,
                                        expectedSalary = null,

                                        // Application content
                                        coverLetter = coverLetter,
                                        resumeUrl = user?.resumeUrl,
                                        additionalDocuments = emptyList(),

                                        // Job information snapshot
                                        jobTitle = currentJob.title,
                                        companyName = currentJob.companyName,
                                        jobLocation = currentJob.location,
                                        jobType = currentJob.jobType,
                                        payInfo = getPayInfo(currentJob),

                                        appliedAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis(),
                                        workerNotes = additionalNotes
                                    )
                                    applicationViewModel.submitApplication(application)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Error handling
    if (applicationUiState.hasError) {
        LaunchedEffect(applicationUiState.error) {
            // Show error message
            applicationUiState.error?.let { Timber.e("Application error: $it") }
        }
    }
}

private fun getPayInfo(job: JobListing): String {
    return if (job.payAmount.isNotEmpty() && job.payType.isNotEmpty()) {
        if (job.payAmount.contains("/")) {
            "₹${job.payAmount}"
        } else {
            "₹${job.payAmount}/${job.payType}"
        }
    } else if (job.salary.isNotEmpty()) {
        job.salary
    } else {
        ""
    }
}

@Composable
private fun ApplicationForm(
    coverLetter: String,
    onCoverLetterChange: (String) -> Unit,
    additionalNotes: String,
    onAdditionalNotesChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Application Details",
                style = AppTypography.sectionHeader.copy(
                    color = Color(0xFF111827)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cover letter
            OutlinedTextField(
                value = coverLetter,
                onValueChange = onCoverLetterChange,
                label = { Text("Cover Letter (Optional)") },
                placeholder = { Text("Tell the employer why you're a great fit for this role...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1F2937),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Additional notes
            OutlinedTextField(
                value = additionalNotes,
                onValueChange = onAdditionalNotesChange,
                label = { Text("Additional Notes (Optional)") },
                placeholder = { Text("Any additional information you'd like to share...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1F2937),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
        }
    }
}

@Composable
private fun SubmitApplicationButton(
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    Button(
        onClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1F2937)
        ),
        shape = RoundedCornerShape(16.dp),
        enabled = !isSubmitting
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isSubmitting) "Submitting..." else "Submit Application",
            style = AppTypography.buttonLarge.copy(
                color = Color.White
            )
        )
    }
}

@Composable
private fun JobInfoCard(
    jobTitle: String,
    companyName: String,
    jobLocation: String,
    jobType: String,
    payInfo: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Job Details",
                style = AppTypography.sectionHeader.copy(
                    color = Color(0xFF111827)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = jobTitle,
                style = AppTypography.cardTitle.copy(
                    color = Color(0xFF1E40AF)
                )
            )

            Text(
                text = companyName,
                style = AppTypography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = jobLocation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = jobType,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }

            if (payInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = payInfo,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF1F2937),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilePreviewCard(
    workerName: String,
    workerEmail: String,
    workerPhone: String = "",
    workerLocation: String = "",
    workerGender: String? = null,
    workerDateOfBirth: String? = null,
    workerSkills: String? = null, // Changed to String to match Firestore storage
    workerExperience: String? = null,
    profileImageUrl: String? = null,
    resumeUrl: String? = null
) {
    // Parse skills string to list
    val skillsList = workerSkills?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your Profile",
                    style = AppTypography.sectionHeader.copy(
                        color = Color(0xFF111827)
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                // Profile image
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF1F2937).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profileImageUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(profileImageUrl),
                            contentDescription = "Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initials = workerName.split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                            .ifEmpty { workerName.take(1).uppercase() }
                        Text(
                            text = initials,
                            style = AppTypography.sectionHeader.copy(
                                color = Color(0xFF1F2937)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Divider
            HorizontalDivider(color = Color(0xFFE5E7EB))
            
            Spacer(modifier = Modifier.height(16.dp))

            // Name
            ProfileInfoRow(
                icon = Icons.Default.Person,
                label = "Name",
                value = workerName.ifBlank { "Not provided" }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            ProfileInfoRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = workerEmail.ifBlank { "Not provided" }
            )

            // Phone
            if (workerPhone.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ProfileInfoRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = workerPhone
                )
            }

            // Location
            if (workerLocation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ProfileInfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    value = workerLocation
                )
            }
            
            // Gender
            if (!workerGender.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                ProfileInfoRow(
                    icon = Icons.Default.Person,
                    label = "Gender",
                    value = workerGender
                )
            }
            
            // Skills
            if (skillsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Skills",
                    style = AppTypography.labelMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    skillsList.take(3).forEach { skill ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1F2937).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = skill,
                                style = AppTypography.labelMedium.copy(
                                    color = Color(0xFF1F2937)
                                )
                            )
                        }
                    }
                    if (skillsList.size > 3) {
                        Text(
                            text = "+${skillsList.size - 3} more",
                            style = AppTypography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            ),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
            
            // Experience
            if (!workerExperience.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                ProfileInfoRow(
                    icon = Icons.Default.Work,
                    label = "Experience",
                    value = workerExperience
                )
            }

            // Resume
            if (!resumeUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF1F2937),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Resume attached",
                        style = AppTypography.labelLarge.copy(
                            color = Color(0xFF1F2937)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = AppTypography.labelSmall.copy(
                    color = Color(0xFF9CA3AF)
                )
            )
            Text(
                text = value,
                style = AppTypography.bodyMedium.copy(
                    color = Color(0xFF1F2937)
                )
            )
        }
    }
}
