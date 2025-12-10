package com.example.dutype.employer.screens.applications

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dutype.models.ApplicationStatus
import com.example.dutype.models.DocumentAttachment
import com.example.dutype.models.Education
import com.example.dutype.models.JobApplication
import com.example.dutype.models.StatusUpdate
import com.example.dutype.models.WorkExperience
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.state.ApplicationStateManager
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.services.JobApplicationService
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enterprise-level Application Detail Screen for Employers
 * Comprehensive view of worker application with all profile data
 * Enhanced with CommonHeader and professional UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    applicationId: String,
    onBackClick: () -> Unit,
    onUpdateStatus: (ApplicationStatus, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val viewModel: EmployerApplicationViewModel = hiltViewModel()
    val jobApplicationService: JobApplicationService = remember { 
        JobApplicationService(
            notificationService = com.example.dutype.services.NotificationService(
                context = context,
                firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            ),
            profileCompletionService = ProfileCompletionService(),
            applicationStateManager = ApplicationStateManager()
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<ApplicationStatus?>(null) }

    // Find the specific application (could be null while loading)
    val application = uiState.applications.find { it.applicationId == applicationId }
    
    // Determine display name for header
    val displayName = when {
        application?.workerName?.isNotBlank() == true -> application.workerName
        application?.workerEmail?.isNotBlank() == true -> application.workerEmail.substringBefore("@")
        else -> "Application Details"
    }
    
    // Mark application as under review when employer opens it
    LaunchedEffect(applicationId, currentUser?.uid) {
        if (application != null && currentUser?.uid != null) {
            jobApplicationService.markApplicationAsUnderReview(applicationId, currentUser.uid)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Custom Header matching app style
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 20.sp
                        ),
                        maxLines = 1
                    )
                    if (application != null) {
                        Text(
                            text = application.jobTitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            ),
                            maxLines = 1
                        )
                    }
                }
                
                if (application != null) {
                    IconButton(onClick = { selectedStatus = application.status; showStatusDialog = true }) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Update Status", 
                            tint = Color(0xFF3B82F6)
                        )
                    }
                }
            }
            androidx.compose.material3.HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
        }
        
        // Main Content
        when {
            uiState.isLoading && application == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6), modifier = Modifier.size(48.dp))
                }
            }
            application == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Application not found",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color(0xFFF8FAFC)),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Enhanced Worker Profile Header Card
                    item {
                        EnhancedWorkerProfileCard(application)
                    }

                    // Worker Contact Info Card
                    item {
                        WorkerContactCard(
                            application = application,
                            onCall = { phone ->
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:$phone") }
                                runCatching { context.startActivity(intent) }
                            },
                            onEmail = { email ->
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply { data = android.net.Uri.parse("mailto:$email") }
                                runCatching { context.startActivity(intent) }
                            }
                        )
                    }

                    if (application.workExperience.isNotEmpty()) item { WorkExperienceCard(workExperience = application.workExperience) }
                    if (application.education.isNotEmpty()) item { EducationCard(education = application.education) }
                    
                    // Skills section - only show if there's data
                    if (application.skills.isNotEmpty() || application.certifications.isNotEmpty() || application.languages.isNotEmpty()) {
                        item {
                            SkillsAndCertificationsCard(
                                skills = application.skills,
                                certifications = application.certifications,
                                languages = application.languages
                            )
                        }
                    }
                    
                    if (application.coverLetter.isNotEmpty()) item { CoverLetterCard(coverLetter = application.coverLetter) }
                    if (application.additionalDocuments.isNotEmpty()) item { DocumentsCard(documents = application.additionalDocuments) }
                    item { JobInformationCard(application = application) }
                    item { ApplicationTimelineCard(statusHistory = application.statusHistory) }
                }
                
                // Bottom Action Bar
                ApplicationActionBar(
                    status = application.status,
                    onChangeStatus = { status ->
                        selectedStatus = status
                        showStatusDialog = true
                    },
                    onQuickAction = { quickStatus ->
                        onUpdateStatus(quickStatus, null)
                    }
                )
            }
        }
    }

    if (showStatusDialog && application != null) {
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

/**
 * Enhanced Worker Profile Card with image support
 */
@Composable
private fun EnhancedWorkerProfileCard(application: JobApplication) {
    val displayName = when {
        application.workerName.isNotBlank() -> application.workerName
        application.workerEmail.isNotBlank() -> application.workerEmail.substringBefore("@")
        else -> "Unknown Worker"
    }
    
    val initials = displayName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { displayName.take(1).uppercase() }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Image/Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!application.workerProfileImageUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = application.workerProfileImageUrl
                            ),
                            contentDescription = "Worker Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Status Badge
                    StatusBadge(status = application.status)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
                            text = "Applied ${getTimeAgo(application.appliedAt)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF9CA3AF)
                            )
                        )
                    }
                }
            }
            
            // Additional worker info if available
            if (application.workerLocation != null || application.workerGender != null || application.workerDateOfBirth != null) {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.HorizontalDivider(color = Color(0xFFE5E7EB))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    application.workerLocation?.let { location ->
                        if (location.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = location,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF6B7280)
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    
                    application.workerGender?.let { gender ->
                        if (gender.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = gender,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF6B7280)
                                    )
                                )
                            }
                        }
                    }
                    
                    application.expectedSalary?.let { salary ->
                        if (salary.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = salary,
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
    }
}

/**
 * Worker Contact Card with action buttons
 */
@Composable
private fun WorkerContactCard(
    application: JobApplication,
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Contact Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email
            if (application.workerEmail.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Email",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        Text(
                            text = application.workerEmail,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF1F2937)
                            )
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { onEmail(application.workerEmail) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Email", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            // Phone
            application.workerPhone?.let { phone ->
                if (phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Phone",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF6B7280)
                                )
                            )
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF1F2937)
                                )
                            )
                        }
                        
                        ElevatedButton(
                            onClick = { onCall(phone) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Call", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun ApplicationActionBar(
    status: ApplicationStatus,
    onChangeStatus: (ApplicationStatus) -> Unit,
    onQuickAction: (ApplicationStatus) -> Unit
) {
    Surface(
        shadowElevation = 8.dp, 
        tonalElevation = 2.dp, 
        color = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Primary suggested actions depending on current status
            when (status) {
                ApplicationStatus.PENDING, ApplicationStatus.UNDER_REVIEW -> {
                    // Enhanced Reject Button (Left side)
                    OutlinedButton(
                        onClick = { onQuickAction(ApplicationStatus.REJECTED) }, 
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = null, 
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reject", 
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                    }
                    
                    // Enhanced Accept Button (Right side)
                    ElevatedButton(
                        onClick = { onQuickAction(ApplicationStatus.ACCEPTED) }, 
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle, 
                            contentDescription = null, 
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Accept", 
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = { onChangeStatus(status) }, 
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Update Status", 
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationDetailLoadingShimmer(modifier: Modifier = Modifier) {
    // Simple placeholder shimmer implementation
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
    val transition = rememberInfiniteTransition(label = "app_detail_shimmer")
    val offsetX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Restart),
        label = "offset"
    )
    val brush = Brush.linearGradient(shimmerColors, start = Offset.Zero, end = Offset(offsetX, offsetX))
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(8) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (index == 0) 160.dp else 120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
            )
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
        ApplicationStatus.ACCEPTED -> "Accepted"
        ApplicationStatus.REJECTED -> "Rejected"
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
        ApplicationStatus.ACCEPTED -> Triple(
            Color(0xFFD1FAE5),
            Color(0xFF059669),
            Icons.Default.CheckCircle
        )
        ApplicationStatus.REJECTED -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFFDC2626),
            Icons.Default.Close
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
