package com.example.dutype.worker.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.ProfileViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

/**
 * Job Application Screen - Review and Submit Application
 * 
 * Shows:
 * 1. Job summary at top
 * 2. Worker's profile info (auto-filled)
 * 3. Optional cover letter field
 * 4. Submit Application button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicationScreen(
    jobId: String,
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val applicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    
    val currentUser = FirebaseAuth.getInstance().currentUser
    val jobUiState by jobViewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val applicationUiState by applicationViewModel.uiState.collectAsStateWithLifecycle()
    
    // Cover letter state
    var coverLetter by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }
    
    // Find the job from loaded jobs or load it
    val job = remember(jobUiState.jobs, jobId) {
        jobUiState.jobs.find { it.jobId == jobId || it.id == jobId }
    }
    
    // Load job if not found
    LaunchedEffect(jobId) {
        onStatusBarColorChange(Color.White)
        // Load profile
        profileViewModel.loadProfile()
    }
    
    // Load job details
    var loadedJob by remember { mutableStateOf<JobListing?>(null) }
    LaunchedEffect(jobId, job) {
        if (job == null && loadedJob == null) {
            val result = jobViewModel.getJobById(jobId)
            result.onSuccess { fetchedJob ->
                loadedJob = fetchedJob
            }
        }
    }
    
    // Use either found job or loaded job
    val displayJob = job ?: loadedJob
    
    // Handle application success
    LaunchedEffect(applicationUiState.applicationSuccess) {
        if (applicationUiState.applicationSuccess) {
            Toast.makeText(context, "Application submitted successfully!", Toast.LENGTH_SHORT).show()
            applicationViewModel.clearSuccessStates()
            // Navigate back to previous screen (job details) and then to my jobs
            navController.popBackStack()
        }
    }
    
    // Handle application error
    LaunchedEffect(applicationUiState.error) {
        applicationUiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            applicationViewModel.clearError()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        // Header
        CommonHeader(
            title = "Apply for Job",
            onBackClick = { navController.popBackStack() },
            backgroundColor = WorkerColors.CardBackground
        )
        
        if (displayJob == null || profileUiState.isLoading) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1F2937))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Job Summary Card
                JobSummaryCard(job = displayJob)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Your Profile Section
                YourProfileSection(
                    profileUiState = profileUiState,
                    currentUser = currentUser
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cover Letter Section
                CoverLetterSection(
                    coverLetter = coverLetter,
                    onCoverLetterChange = { coverLetter = it },
                    additionalNotes = additionalNotes,
                    onAdditionalNotesChange = { additionalNotes = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // What Employer Will See
                WhatEmployerWillSeeSection()
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Submit Button
                SubmitApplicationButton(
                    isSubmitting = applicationUiState.isApplying,
                    onSubmit = {
                        applicationViewModel.applyForJob(
                            jobId = jobId,
                            coverLetter = coverLetter.takeIf { it.isNotBlank() },
                            additionalNotes = additionalNotes.takeIf { it.isNotBlank() }
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun JobSummaryCard(job: JobListing) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(0.5.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section header
            Text(
                text = "Applying for",
                style = AppTypography.labelMedium.copy(
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Job image or icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!job.jobImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = job.jobImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Work,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = AppTypography.cardTitle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = job.companyName.ifEmpty { job.company },
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Job details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = job.area ?: job.location,
                        style = AppTypography.caption.copy(color = Color(0xFF6B7280)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Pay
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${job.payAmount.ifEmpty { job.salary }}",
                        style = AppTypography.labelMedium.copy(
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "/${job.payType.lowercase()}",
                        style = AppTypography.caption.copy(color = Color(0xFF6B7280))
                    )
                }
            }
        }
    }
}

@Composable
private fun YourProfileSection(
    profileUiState: com.example.dutype.viewmodels.ProfileUiState,
    currentUser: com.google.firebase.auth.FirebaseUser?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(0.5.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Profile",
                    style = AppTypography.sectionHeader.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                
                // Verified badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Auto-filled",
                        style = AppTypography.caption.copy(
                            color = Color(0xFF10B981),
                            fontSize = 12.sp
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile image
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    val profileImageUrl = profileUiState.user?.profileImageUrl
                    if (!profileImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profileUiState.user?.fullName 
                            ?: currentUser?.displayName 
                            ?: "Your Name",
                        style = AppTypography.cardTitle.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    )
                    
                    Text(
                        text = profileUiState.user?.phone 
                            ?: currentUser?.phoneNumber 
                            ?: "Phone number",
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider(color = Color(0xFFF3F4F6))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Skills
            val skillsList = profileUiState.user?.getSkillsList() ?: emptyList()
            if (skillsList.isNotEmpty()) {
                ProfileInfoRow(
                    icon = Icons.Default.Star,
                    label = "Skills",
                    value = skillsList.take(3).joinToString(", ") + if (skillsList.size > 3) " +${skillsList.size - 3} more" else ""
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Location
            val location = profileUiState.user?.address ?: profileUiState.user?.location
            if (!location.isNullOrBlank()) {
                ProfileInfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    value = location
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Experience
            val experience = profileUiState.user?.experience
            if (!experience.isNullOrBlank()) {
                ProfileInfoRow(
                    icon = Icons.Default.Work,
                    label = "Experience",
                    value = experience
                )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                style = AppTypography.caption.copy(
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                )
            )
            Text(
                text = value,
                style = AppTypography.bodyMedium.copy(
                    color = Color(0xFF374151),
                    fontSize = 13.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoverLetterSection(
    coverLetter: String,
    onCoverLetterChange: (String) -> Unit,
    additionalNotes: String,
    onAdditionalNotesChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(0.5.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF1F2937),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Cover Letter",
                    style = AppTypography.sectionHeader.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "(Optional)",
                    style = AppTypography.caption.copy(
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Cover letter input
            OutlinedTextField(
                value = coverLetter,
                onValueChange = onCoverLetterChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = "Tell the employer why you're a great fit for this job...",
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1F2937),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color(0xFFFAFAFA),
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = AppTypography.bodyMedium.copy(fontSize = 14.sp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Character count
            Text(
                text = "${coverLetter.length}/500",
                style = AppTypography.caption.copy(
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                ),
                modifier = Modifier.align(Alignment.End)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Additional notes
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = null,
                    tint = Color(0xFF1F2937),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Additional Notes",
                    style = AppTypography.sectionHeader.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "(Optional)",
                    style = AppTypography.caption.copy(
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = additionalNotes,
                onValueChange = onAdditionalNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                placeholder = {
                    Text(
                        text = "Any specific availability, questions, or notes for the employer...",
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1F2937),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color(0xFFFAFAFA),
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = AppTypography.bodyMedium.copy(fontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun WhatEmployerWillSeeSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(0.5.dp, WorkerColors.Border, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "What employer will see",
                    style = AppTypography.sectionHeader.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF065F46),
                        fontSize = 14.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // List of visible items
            val visibleItems = listOf(
                "Your name and profile photo",
                "Phone number (for contact)",
                "Skills and experience",
                "Cover letter (if provided)",
                "Your location"
            )
            
            visibleItems.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item,
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF065F46),
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmitApplicationButton(
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1F2937),
                disabledContainerColor = Color(0xFF9CA3AF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submitting...",
                    style = AppTypography.buttonMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit Application",
                    style = AppTypography.buttonMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Disclaimer
        Text(
            text = "By submitting, you agree to share your profile information with the employer.",
            style = AppTypography.caption.copy(
                color = Color(0xFF9CA3AF),
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
