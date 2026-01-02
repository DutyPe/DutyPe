package com.example.dutype.employer.screens.applications

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.example.dutype.models.getDisplayName
import com.example.dutype.viewmodels.EmployerApplicationViewModel
import com.example.dutype.components.ApplicationDetailShimmer
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.example.dutype.components.JobRatingBottomSheet
import com.example.dutype.components.ApplicationStatusBadge
import com.example.dutype.services.RatingService
import com.example.dutype.utils.DateTimeUtils
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Chat
import com.example.dutype.services.ChatService

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
    onUpdateStatus: (ApplicationStatus, String?) -> Unit = { _, _ -> },
    onVerifyWork: ((String, String) -> Unit)? = null, // jobId, applicationId
    onMessageWorker: ((String) -> Unit)? = null // conversationId callback
) {
    val context = LocalContext.current
    val viewModel: EmployerApplicationViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    
    // Rating service for submitting ratings (injected via Hilt)
    val profileCompletionViewModel: com.example.dutype.viewmodels.ProfileCompletionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val ratingService = profileCompletionViewModel.ratingService
    
    // Chat service for messaging (injected via Hilt)
    val chatViewModel: com.example.dutype.viewmodels.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val chatService = chatViewModel.chatService
    var isStartingChat by remember { mutableStateOf(false) }

    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<ApplicationStatus?>(null) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var hasAlreadyRated by remember { mutableStateOf(false) }
    
    // FINTECH: Contact Unlock State
    var showUnlockDialog by remember { mutableStateOf(false) }
    var isProcessingPayment by remember { mutableStateOf(false) }

    // Find the specific application (could be null while loading)
    val application = uiState.applications.find { it.applicationId == applicationId }
    
    // Get application index for contact unlock check
    val applicationIndex = uiState.applications.indexOfFirst { it.applicationId == applicationId }
    val isContactUnlocked = if (applicationIndex >= 0) {
        viewModel.isContactUnlocked(applicationId, applicationIndex)
    } else true // Default to unlocked if not found
    
    // Check if employer has already rated this worker for this job
    LaunchedEffect(application?.workerId, application?.jobId, currentUser?.uid) {
        if (application != null && currentUser != null) {
            ratingService.hasUserRatedForJob(
                raterId = currentUser.uid,
                jobId = application.jobId
            ).onSuccess { hasRated ->
                hasAlreadyRated = hasRated
            }
        }
    }
    
    // Determine display name for header
    val displayName = when {
        application?.workerName?.isNotBlank() == true -> application.workerName
        application?.workerEmail?.isNotBlank() == true -> application.workerEmail.substringBefore("@")
        else -> "Application Details"
    }
    
    // Mark application as under review when employer opens it
    LaunchedEffect(applicationId, currentUser?.uid) {
        if (application != null && currentUser?.uid != null) {
            viewModel.markApplicationAsUnderReview(applicationId)
        }
    }
    
    // Load application if not found in current state
    LaunchedEffect(applicationId) {
        if (uiState.applications.isEmpty() || uiState.applications.none { it.applicationId == applicationId }) {
            viewModel.loadApplicationById(applicationId)
        }
    }
    
    // FINTECH: Contact Unlock Payment Dialog
    if (showUnlockDialog && application != null) {
        ContactUnlockDetailDialog(
            application = application,
            unlockPrice = viewModel.getContactUnlockPrice(),
            isProcessing = isProcessingPayment,
            onDismiss = { showUnlockDialog = false },
            onConfirmPayment = {
                isProcessingPayment = true
                viewModel.processContactUnlockPayment(
                    applicationId = applicationId,
                    onSuccess = {
                        isProcessingPayment = false
                        showUnlockDialog = false
                        Toast.makeText(context, "Contact unlocked! ✅", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        isProcessingPayment = false
                        Toast.makeText(context, "Payment failed: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Common Header with subtitle and action button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CommonHeader(
                    title = displayName,
                    subtitle = application?.jobTitle,
                    onBackClick = onBackClick
                )
            }
            
            if (application != null) {
                IconButton(
                    onClick = { selectedStatus = application.status; showStatusDialog = true }
                ) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = "Update Status", 
                        tint = Color(0xFF3B82F6)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        
        // Main Content
        when {
            uiState.isLoading && application == null -> {
                ApplicationDetailShimmer(
                    modifier = Modifier.weight(1f)
                )
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

                    // Worker Contact Info Card with unlock feature
                    item {
                        WorkerContactCard(
                            application = application,
                            isContactUnlocked = isContactUnlocked,
                            onUnlockContact = {
                                viewModel.unlockContact(
                                    applicationId = applicationId,
                                    onSuccess = {
                                        Toast.makeText(context, "Contact unlocked! ✅", Toast.LENGTH_SHORT).show()
                                    },
                                    onPaymentRequired = {
                                        showUnlockDialog = true
                                    }
                                )
                            },
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

                    // Work Experience - show structured or text-based
                    if (application.workExperience.isNotEmpty()) {
                        item { WorkExperienceCard(workExperience = application.workExperience) }
                    } else if (!application.workExperienceText.isNullOrBlank()) {
                        item { WorkExperienceTextCard(experienceText = application.workExperienceText) }
                    }
                    
                    if (application.education.isNotEmpty()) item { EducationCard(education = application.education) }
                    
                    // Skills section - show structured or text-based
                    if (application.skills.isNotEmpty() || application.certifications.isNotEmpty() || application.languages.isNotEmpty()) {
                        item {
                            SkillsAndCertificationsCard(
                                skills = application.skills,
                                certifications = application.certifications,
                                languages = application.languages
                            )
                        }
                    } else if (!application.skillsText.isNullOrBlank()) {
                        item { SkillsTextCard(skillsText = application.skillsText) }
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
                    },
                    onRateWorker = if (application.status == ApplicationStatus.COMPLETED && !hasAlreadyRated) {
                        { showRatingSheet = true }
                    } else null,
                    onVerifyWork = if (application.status == ApplicationStatus.ACCEPTED && onVerifyWork != null) {
                        { onVerifyWork(application.jobId, application.applicationId) }
                    } else null,
                    onMessageWorker = if (onMessageWorker != null && !isStartingChat) {
                        {
                            isStartingChat = true
                            scope.launch {
                                chatService.getOrCreateConversation(
                                    otherUserId = application.workerId,
                                    jobId = application.jobId
                                ).onSuccess { conversationId ->
                                    isStartingChat = false
                                    onMessageWorker(conversationId)
                                }.onFailure { error ->
                                    isStartingChat = false
                                    Toast.makeText(context, "Failed to start chat: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else null,
                    isStartingChat = isStartingChat
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
    
    // Rating Bottom Sheet for employer to rate worker
    if (showRatingSheet && application != null && currentUser != null) {
        JobRatingBottomSheet(
            isVisible = true,
            onDismiss = { showRatingSheet = false },
            jobId = application.jobId,
            applicationId = application.applicationId,
            jobTitle = application.jobTitle,
            companyName = application.companyName,
            ratedUserId = application.workerId,
            ratedUserName = application.workerName,
            ratedUserRole = com.example.dutype.models.RatingUserRole.WORKER,
            raterUserId = currentUser.uid,
            raterUserRole = com.example.dutype.models.RatingUserRole.EMPLOYER,
            ratingService = ratingService,
            onRatingSubmitted = {
                showRatingSheet = false
                hasAlreadyRated = true
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
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Image/Avatar with border
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
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
                            style = MaterialTheme.typography.headlineSmall.copy(
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
                        style = AppTypography.sectionHeader.copy(color = Color(0xFF1F2937))
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Status Badge - using centralized component
                    ApplicationStatusBadge(status = application.status)
                    
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
                            text = "Applied ${DateTimeUtils.formatRelativeTime(application.appliedAt)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
            
            // Additional worker info if available
            if (application.workerLocation != null || application.workerGender != null || application.expectedSalary != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    application.workerLocation?.let { location ->
                        if (location.isNotBlank()) {
                            InfoPill(
                                icon = Icons.Default.LocationOn,
                                text = location,
                                iconColor = Color(0xFF3B82F6)
                            )
                        }
                    }
                    
                    application.workerGender?.let { gender ->
                        if (gender.isNotBlank()) {
                            InfoPill(
                                icon = Icons.Default.Person,
                                text = gender,
                                iconColor = Color(0xFF8B5CF6)
                            )
                        }
                    }
                    
                    application.expectedSalary?.let { salary ->
                        if (salary.isNotBlank()) {
                            InfoPill(
                                icon = Icons.Default.AttachMoney,
                                text = salary,
                                iconColor = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = iconColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF374151),
                fontSize = 11.sp
            ),
            maxLines = 1
        )
    }
}

/**
 * Worker Contact Card with action buttons and contact unlock
 */
@Composable
private fun WorkerContactCard(
    application: JobApplication,
    isContactUnlocked: Boolean = true,
    onUnlockContact: () -> Unit = {},
    onCall: (String) -> Unit,
    onEmail: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Contact Information",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        fontSize = 15.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email
            if (application.workerEmail.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF9FAFB),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Email",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = application.workerEmail,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF1F2937),
                                    fontSize = 14.sp
                                )
                            )
                        }
                        
                        Surface(
                            onClick = { onEmail(application.workerEmail) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Email",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
            
            // Phone - with contact unlock feature
            application.workerPhone?.let { phone ->
                if (phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isContactUnlocked) Color(0xFFF9FAFB) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isContactUnlocked) Color(0xFF10B981).copy(alpha = 0.1f) 
                                        else Color(0xFFD97706).copy(alpha = 0.1f), 
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isContactUnlocked) Icons.Default.Phone else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isContactUnlocked) Color(0xFF10B981) else Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Phone",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 11.sp
                                    )
                                )
                                if (isContactUnlocked) {
                                    Text(
                                        text = phone,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFF1F2937),
                                            fontSize = 14.sp
                                        )
                                    )
                                } else {
                                    Text(
                                        text = "••••••••••",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 14.sp
                                        )
                                    )
                                }
                            }
                            
                            if (isContactUnlocked) {
                                Surface(
                                    onClick = { onCall(phone) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981)
                                ) {
                                    Text(
                                        text = "Call",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            } else {
                                // Unlock button
                                Surface(
                                    onClick = onUnlockContact,
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFD97706)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Unlock",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp
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
    }
}

/**
 * FINTECH: Contact Unlock Dialog for Application Detail Screen
 */
@Composable
private fun ContactUnlockDetailDialog(
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
                text = "Unlock Contact",
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
                    text = "Unlock ${application.workerName}'s phone number to contact them directly.",
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
                            text = "Unlock Price",
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
                    Text("Processing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pay ₹$unlockPrice & Unlock")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}



@Composable
private fun ApplicationActionBar(
    status: ApplicationStatus,
    onChangeStatus: (ApplicationStatus) -> Unit,
    onQuickAction: (ApplicationStatus) -> Unit,
    onRateWorker: (() -> Unit)? = null,
    onVerifyWork: (() -> Unit)? = null,
    onMessageWorker: (() -> Unit)? = null,
    isStartingChat: Boolean = false
) {
    Surface(
        shadowElevation = 8.dp, 
        tonalElevation = 2.dp, 
        color = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column {
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
                    ApplicationStatus.ACCEPTED -> {
                        // Verify Work Button (for QR/Code verification)
                        if (onVerifyWork != null) {
                            ElevatedButton(
                                onClick = onVerifyWork, 
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "🔐", 
                                    fontSize = 18.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Verify Work", 
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }
                        
                        // Mark as Complete Button
                        ElevatedButton(
                            onClick = { onQuickAction(ApplicationStatus.COMPLETED) }, 
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFF7C3AED),
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
                                "Mark Complete", 
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                    ApplicationStatus.COMPLETED -> {
                        // Rate Worker Button
                        if (onRateWorker != null) {
                            ElevatedButton(
                                onClick = onRateWorker, 
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = Color(0xFFF59E0B),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Rate Worker", 
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        } else {
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
                
                // Message Worker Button (always visible)
                if (onMessageWorker != null) {
                    OutlinedButton(
                        onClick = onMessageWorker,
                        enabled = !isStartingChat,
                        modifier = Modifier.height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF3B82F6)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isStartingChat) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF3B82F6),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Navigation bar spacer
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            )
        }
    }
}


@Composable
private fun WorkExperienceCard(workExperience: List<WorkExperience>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFEF3C7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Work Experience",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        fontSize = 15.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            workExperience.forEachIndexed { index, exp ->
                ExperienceItem(experience = exp)
                if (index < workExperience.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * Text-based Work Experience Card for simple string experience data
 */
@Composable
private fun WorkExperienceTextCard(experienceText: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFEF3C7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Work Experience",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        fontSize = 15.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = experienceText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF374151),
                    lineHeight = 22.sp,
                    fontSize = 14.sp
                )
            )
        }
    }
}

/**
 * Text-based Skills Card for simple string skills data
 */
@Composable
private fun SkillsTextCard(skillsText: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFDBEAFE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Skills",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        fontSize = 15.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Display skills as chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val skillsList = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                items(skillsList) { skill ->
                    SkillChip(skill = skill)
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFDCFCE7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Education",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        fontSize = 15.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            education.forEachIndexed { index, edu ->
                EducationItem(education = edu)
                if (index < education.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFDBEAFE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Skills & Qualifications",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        fontSize = 15.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (skills.isNotEmpty()) {
                SkillsSection(title = "Skills", items = skills)
            }
            
            if (certifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                SkillsSection(title = "Certifications", items = certifications)
            }
            
            if (languages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFCE7F3), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFFDB2777),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Cover Letter",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        fontSize = 15.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = coverLetter,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF374151),
                        lineHeight = 22.sp,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun DocumentsCard(documents: List<DocumentAttachment>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFEF3C7), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Attachment,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Documents",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        fontSize = 15.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            documents.forEachIndexed { index, doc ->
                DocumentItem(document = doc)
                if (index < documents.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DocumentItem(document: DocumentAttachment) {
    val context = LocalContext.current
    
    // Function to open document URL in browser or download manager
    fun openDocument() {
        if (document.fileUrl.isNotBlank()) {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(document.fileUrl)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Unable to open document",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            android.widget.Toast.makeText(
                context,
                "Document URL not available",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openDocument() },
        color = Color(0xFFF9FAFB),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Attachment,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF1F2937),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "${document.fileType.name} • ${formatFileSize(document.fileSize)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                )
            }
            
            Surface(
                onClick = { openDocument() },
                shape = CircleShape,
                color = Color(0xFFEFF6FF)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun JobInformationCard(application: JobApplication) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Job Information",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        fontSize = 15.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoItem(
                        icon = Icons.Default.Work,
                        label = "Position",
                        value = application.jobTitle
                    )
                    
                    InfoItem(
                        icon = Icons.Default.Business,
                        label = "Company",
                        value = application.companyName
                    )
                    
                    InfoItem(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        value = application.jobLocation
                    )
                    
                    InfoItem(
                        icon = Icons.Default.Schedule,
                        label = "Job Type",
                        value = application.jobType
                    )
                    
                    InfoItem(
                        icon = Icons.Default.AttachMoney,
                        label = "Pay Range",
                        value = application.payInfo
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationTimelineCard(statusHistory: List<StatusUpdate>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFF3E8FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = Color(0xFF9333EA),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Application Timeline",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        fontSize = 15.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            statusHistory.forEachIndexed { index, update ->
                TimelineItem(update = update, isLast = index == statusHistory.size - 1)
                if (index < statusHistory.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(update: StatusUpdate, isLast: Boolean = false) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (update.systemUpdate) Color(0xFF9CA3AF) else Color(0xFF3B82F6),
                        shape = CircleShape
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(Color(0xFFE5E7EB))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = update.status.getDisplayName(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    fontSize = 14.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = DateTimeUtils.formatRelativeTime(update.updatedAt),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            )
            update.notes?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp
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
                        Text(status.getDisplayName())
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

// NOTE: getStatusDisplayName removed - use ApplicationStatus.getDisplayName() extension instead
// NOTE: StatusBadge removed - use centralized ApplicationStatusBadge from components instead

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
