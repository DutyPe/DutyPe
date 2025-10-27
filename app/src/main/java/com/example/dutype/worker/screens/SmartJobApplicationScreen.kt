package com.example.dutype.worker.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.models.JobApplication
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.StatusUpdate
import com.example.dutype.viewmodels.JobApplicationViewModel
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
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
    var job by remember { mutableStateOf<com.example.dutype.models.JobListing?>(null) }
    var isLoadingJob by remember { mutableStateOf(true) }
    var jobError by remember { mutableStateOf<String?>(null) }

    // Form state
    var coverLetter by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }
    var hasAlreadyApplied by remember { mutableStateOf(false) }

    // Ensure status bar color is white for this screen
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
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

    // Check if user has already applied
    LaunchedEffect(jobId) {
        applicationViewModel.hasAppliedToJob(jobId) { applied ->
            hasAlreadyApplied = applied
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
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Apply for Job",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                        Text(
                            text = jobError!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Go Back")
                        }
                    }
                }
            }

            hasAlreadyApplied -> {
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
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Already Applied",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "You have already applied for this job. Check your applications to track the status.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
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
                        .verticalScroll(rememberScrollState())
                ) {
                    // Job information card
                    JobInfoCard(
                        jobTitle = job!!.title,
                        companyName = job!!.companyName,
                        jobLocation = job!!.location,
                        jobType = job!!.jobType,
                        payInfo = if (job!!.payAmount.isNotEmpty() && job!!.payType.isNotEmpty()) {
                            if (job!!.payAmount.contains("/")) {
                                "₹${job!!.payAmount}"
                            } else {
                                "₹${job!!.payAmount}/${job!!.payType}"
                            }
                        } else if (job!!.salary.isNotEmpty()) {
                            job!!.salary
                        } else {
                            ""
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Application form
                    ApplicationForm(
                        coverLetter = coverLetter,
                        onCoverLetterChange = { coverLetter = it },
                        additionalNotes = additionalNotes,
                        onAdditionalNotesChange = { additionalNotes = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Profile preview
                    ProfilePreviewCard(
                        workerName = profileUiState.user?.fullName ?: currentUser?.displayName ?: "Your Name",
                        workerEmail = profileUiState.user?.email ?: currentUser?.email ?: "dutypein@gmail.com",
                        workerPhone = profileUiState.user?.phoneNumber ?: "",
                        workerLocation = profileUiState.user?.location ?: "",
                        resumeUrl = profileUiState.user?.resumeUrl
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit button
                    SubmitApplicationButton(
                        isSubmitting = applicationUiState.isSubmitting,
                        onSubmit = {
                            if (currentUser != null && job != null) {
                                val user = profileUiState.user
                                val application = JobApplication(
                                    applicationId = UUID.randomUUID().toString(),
                                    jobId = jobId,
                                    workerId = currentUser.uid,
                                    employerId = job!!.employerId,
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
                                    workerPhone = user?.phoneNumber,
                                    workerProfileImageUrl = user?.profileImageUrl ?: currentUser.photoUrl?.toString(),
                                    workerLocation = user?.location,
                                    workerDateOfBirth = user?.dateOfBirth,
                                    workerGender = user?.gender,

                                    // Professional information - simplified approach
                                    workExperience = emptyList(), // Will be populated from user profile if available
                                    skills = user?.skills ?: emptyList(),
                                    education = emptyList(), // Will be populated from user profile if available
                                    certifications = emptyList(), // Basic profile - can be enhanced later
                                    languages = emptyList(), // Basic profile - can be enhanced later
                                    availability = null, // Basic profile - can be enhanced later
                                    expectedSalary = null, // Basic profile - can be enhanced later

                                    // Application content
                                    coverLetter = coverLetter,
                                    resumeUrl = user?.resumeUrl,
                                    additionalDocuments = emptyList(), // Basic profile - can be enhanced later

                                    // Job information snapshot
                                    jobTitle = job!!.title,
                                    companyName = job!!.companyName,
                                    jobLocation = job!!.location,
                                    jobType = job!!.jobType,
                                    payInfo = if (job!!.payAmount.isNotEmpty() && job!!.payType.isNotEmpty()) {
                                        if (job!!.payAmount.contains("/")) {
                                            "₹${job!!.payAmount}"
                                        } else {
                                            "₹${job!!.payAmount}/${job!!.payType}"
                                        }
                                    } else {
                                        job!!.salary
                                    },

                                    appliedAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis(),
                                    workerNotes = additionalNotes
                                )
                                applicationViewModel.submitApplication(application)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Error handling
    if (applicationUiState.hasError) {
        LaunchedEffect(applicationUiState.error) {
            // Show error message - you can implement a snackbar or toast here
            println("Application error: ${applicationUiState.error}")
        }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Application Details",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
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
                    focusedBorderColor = Color(0xFF6366F1),
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
                    focusedBorderColor = Color(0xFF6366F1),
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
            containerColor = Color(0xFF6366F1)
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
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Job Details",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = jobTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E40AF)
                )
            )

            Text(
                text = companyName,
                style = MaterialTheme.typography.titleMedium.copy(
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
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = payInfo,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF10B981),
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
    resumeUrl: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Your Profile Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = workerName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = workerEmail,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    )
                )
            }

            // Phone (if available)
            if (workerPhone.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = workerPhone,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }

            // Location (if available)
            if (workerLocation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = workerLocation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
            }

            // Resume (if available)
            if (!resumeUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Resume attached",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
