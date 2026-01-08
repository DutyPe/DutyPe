package com.example.dutype.worker.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.ValidationUtils
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.components.TrustBadge
import com.example.dutype.components.TrustBadgeWithInfo
import com.example.dutype.components.TrustBadgeSize
import com.example.dutype.components.ShareJobIconButton
import com.example.dutype.components.JobSafetyCard
import com.example.dutype.components.analyzeJobRisk
import com.example.dutype.services.JobShareImageGenerator
import com.example.dutype.models.parseTrustTier
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.ui.theme.WorkerColors
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDescriptionScreen(
    jobId: String,
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {},
    jobShareImageGenerator: JobShareImageGenerator? = null
) {
    val context = LocalContext.current
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    // REMOVED: savedJobsViewModel - not used in this screen, was causing unnecessary loading
    val smartApplicationViewModel: SmartJobApplicationViewModel = hiltViewModel()
    // REMOVED: jobApplicationViewModel - not needed, we use smartApplicationViewModel.hasUserApplied() instead
    val chatViewModel: com.example.dutype.viewmodels.ChatViewModel = hiltViewModel()
    val locationPreferences = remember { com.example.dutype.location.LocationPreferences(context) }
    val currentLocation by locationPreferences.currentLocation.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Use injected share image generator or fallback to provided one
    val shareGenerator = jobShareImageGenerator ?: jobViewModel.jobShareImageGenerator
    
    LaunchedEffect(Unit) { onStatusBarColorChange(Color.White) }

    var job by remember { mutableStateOf<JobListing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaved by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var retryTrigger by remember { mutableStateOf(0) }
    
    // Report state
    var showReportSheet by remember { mutableStateOf(false) }
    // ReportingService accessed via SmartJobApplicationViewModel (proper DI pattern)
    val reportingService = smartApplicationViewModel.reportingService
    
    // Guest mode - Login bottom sheet state
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<String?>(null) } // "apply", "call", "message", "whatsapp"
    
    // Application state
    var hasApplied by remember { mutableStateOf(false) }
    var applicationStatus by remember { mutableStateOf<String?>(null) }
    val applicationUiState by smartApplicationViewModel.uiState.collectAsStateWithLifecycle()
    // REMOVED: jobApplicationUiState - not needed, we use smartApplicationViewModel.hasUserApplied() instead
    
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    
    // Check if user has already applied to this job - LIGHTWEIGHT check
    LaunchedEffect(jobId, currentUser) {
        if (currentUser != null && jobId.isNotEmpty()) {
            // PERFORMANCE FIX: Only check if user applied to THIS job, don't load all applications
            smartApplicationViewModel.hasUserApplied(jobId) { applied ->
                hasApplied = applied
                if (applied) {
                    applicationStatus = "APPLIED" // Generic status, details load if needed
                }
            }
        }
    }
    
    // REMOVED: Don't load all applications on mount - too heavy
    // LaunchedEffect(Unit) { jobApplicationViewModel.loadMyApplications() }
    
    // Handle application success
    LaunchedEffect(applicationUiState.applicationSuccess) {
        if (applicationUiState.applicationSuccess) {
            hasApplied = true
            applicationStatus = "PENDING"
            snackbarMessage = "Application submitted successfully!"
            showSnackbar = true
            smartApplicationViewModel.clearSuccessStates()
            // REMOVED: Don't reload all applications here - MyJobsScreen will load them
        }
    }
    
    // Handle application error
    LaunchedEffect(applicationUiState.error) {
        applicationUiState.error?.let { errorMsg ->
            snackbarMessage = errorMsg
            showSnackbar = true
            smartApplicationViewModel.clearError()
        }
    }
    
    LaunchedEffect(job) { job?.let { isSaved = it.isSaved } }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.3f else 1f,
        animationSpec = tween(600, easing = EaseInOutQuart),
        label = "contentAlpha"
    )

    LaunchedEffect(jobId, retryTrigger) {
        if (jobId.isNotEmpty()) {
            isLoading = true
            error = null
            try {
                val result = jobViewModel.getJobById(jobId)
                result.fold(
                    onSuccess = { fetchedJob ->
                        val jobWithDistance = if (fetchedJob != null && 
                            currentLocation != null && 
                            (currentLocation!!.latitude != 0.0 || currentLocation!!.longitude != 0.0) &&
                            (fetchedJob.latitude != 0.0 || fetchedJob.longitude != 0.0)) {
                            val distance = jobViewModel.locationService.calculateDistance(
                                currentLocation!!.latitude, currentLocation!!.longitude,
                                fetchedJob.latitude, fetchedJob.longitude
                            )
                            fetchedJob.copy(distance = distance)
                        } else fetchedJob
                        job = jobWithDistance
                        isLoading = false
                    },
                    onFailure = { exception ->
                        error = exception.message ?: "Failed to load job details"
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                error = e.message ?: "Failed to load job details"
                isLoading = false
            }
        }
    }

    BackHandler {
        navController.popBackStack()
    }

    Box(modifier = Modifier.fillMaxSize().background(WorkerColors.ScreenBackground)) {
        // Main content - ad is shown before navigation (in WorkerHomeScreen)
        Column(modifier = Modifier.fillMaxSize()) {
            // Header - Using CommonHeader for consistency
            com.example.dutype.components.CommonHeader(
                title = "Job Details",
                onBackClick = { navController.popBackStack() },
                showBackButton = true,
                backgroundColor = WorkerColors.CardBackground,
                titleColor = Color.Black,
                actions = {
                    // Report Button (Flag icon)
                    job?.let { currentJob ->
                        com.example.dutype.components.ReportJobIconButton(
                            onClick = { showReportSheet = true },
                            tint = Color(0xFF6B7280)
                        )
                    }
                    
                    // Share Button
                    job?.let { currentJob ->
                        ShareJobIconButton(
                            job = currentJob,
                            jobShareImageGenerator = shareGenerator,
                            tint = Color(0xFF6B7280)
                        )
                    }
                }
            )
            
            // Loading indicator below header
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF10B981), trackColor = Color(0xFFE5E7EB))
            }

            // Content
            Box(modifier = Modifier.fillMaxSize().weight(1f).graphicsLayer(alpha = contentAlpha)) {
                when {
                    isLoading -> LoadingContent()
                    error != null -> ErrorContent(error!!) { retryTrigger++ }
                    job != null -> JobDetailsContent(job!!, shareGenerator = shareGenerator)
                }
            }

            // Bottom Action Bar
            if (job != null && !isLoading && error == null) {
                BottomActionBar(
                    job = job!!,
                    currentUser = currentUser,
                    context = context,
                    navController = navController,
                    jobId = jobId,
                    hasApplied = hasApplied,
                    applicationStatus = applicationStatus,
                    onApplyClick = {
                        // Navigate to JobApplicationScreen for review before submitting
                        navController.navigate(Routes.jobApplicationRoute(jobId))
                    },
                    onMessageEmployer = {
                        // Start or open conversation with employer
                        val employerId = job!!.employerId
                        if (employerId.isNotEmpty()) {
                            android.widget.Toast.makeText(context, "Opening chat...", android.widget.Toast.LENGTH_SHORT).show()
                            scope.launch {
                                val result = chatViewModel.chatService.getOrCreateConversation(
                                    otherUserId = employerId,
                                    jobId = jobId
                                )
                                result.fold(
                                    onSuccess = { conversationId ->
                                        navController.navigate(Routes.chatConversationDetailRoute(conversationId))
                                    },
                                    onFailure = { e ->
                                        android.widget.Toast.makeText(context, "Failed to open chat: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Employer info not available", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLoginRequired = { action ->
                        pendingAction = action
                        showLoginBottomSheet = true
                    }
                )
            }
        }

        // Snackbar
        AnimatedVisibility(
            visible = showSnackbar,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 110.dp)
        ) {
            LaunchedEffect(showSnackbar) { kotlinx.coroutines.delay(2500); showSnackbar = false }
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (snackbarMessage.contains("saved")) Icons.Default.CheckCircle else Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(snackbarMessage, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
    
    // Report Job Bottom Sheet
    if (showReportSheet && job != null) {
        com.example.dutype.components.ReportJobSheet(
            jobTitle = job!!.title,
            companyName = job!!.companyName,
            onDismiss = { showReportSheet = false },
            onReport = { reportType, description ->
                reportingService.reportJob(jobId, reportType, description)
            }
        )
    }
    
    // Guest Mode - Login Bottom Sheet
    com.example.dutype.components.LoginBottomSheet(
        isVisible = showLoginBottomSheet,
        onDismiss = { 
            showLoginBottomSheet = false
            pendingAction = null
        },
        onLoginSuccess = {
            showLoginBottomSheet = false
            // Execute the pending action after successful login
            when (pendingAction) {
                "apply" -> {
                    // Navigate to JobApplicationScreen for review before submitting
                    navController.navigate(Routes.jobApplicationRoute(jobId))
                }
                "call" -> {
                    val phone = job?.contactNumber?.ifEmpty { job?.phoneNumber ?: "" } ?: ""
                    if (phone.isNotEmpty()) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply { 
                            data = android.net.Uri.parse("tel:$phone") 
                        }
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    }
                }
                "message" -> {
                    val employerId = job?.employerId ?: ""
                    if (employerId.isNotEmpty()) {
                        scope.launch {
                            val result = chatViewModel.chatService.getOrCreateConversation(
                                otherUserId = employerId,
                                jobId = jobId
                            )
                            result.fold(
                                onSuccess = { conversationId ->
                                    navController.navigate(Routes.chatConversationDetailRoute(conversationId))
                                },
                                onFailure = { e ->
                                    android.widget.Toast.makeText(context, "Failed to open chat: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
                "whatsapp" -> {
                    val phone = job?.contactNumber?.ifEmpty { job?.phoneNumber ?: "" } ?: ""
                    if (phone.isNotEmpty()) {
                        com.example.dutype.components.openWhatsAppApply(
                            context = context,
                            phoneNumber = phone,
                            jobTitle = job?.title ?: "",
                            companyName = job?.companyName ?: "",
                            salary = job?.payAmount?.ifEmpty { job?.salary } ?: "",
                            location = job?.area ?: job?.location ?: ""
                        )
                    }
                }
            }
            pendingAction = null
        },
        onProfileSetupRequired = {
            // For job application - navigate to profile setup with return route to job application
            showLoginBottomSheet = false
            android.widget.Toast.makeText(context, "Please complete your profile to apply", android.widget.Toast.LENGTH_SHORT).show()
            // Navigate to profile setup with return route to job application screen
            navController.navigate(Routes.profileSetupWithReturnRoute(Routes.jobApplicationRoute(jobId)))
            pendingAction = null
        },
        requiresProfileCheck = pendingAction == "apply", // Only check profile for job applications
        role = com.example.dutype.models.UserRole.WORKER,
        title = "Login to Continue",
        subtitle = when (pendingAction) {
            "apply" -> "Login to apply for this job"
            "call" -> "Login to call the employer"
            "message" -> "Login to message the employer"
            "whatsapp" -> "Login to contact via WhatsApp"
            else -> "Please login to continue"
        }
    )
}

@Composable
private fun BottomActionBar(
    job: JobListing,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    context: android.content.Context,
    navController: NavController,
    jobId: String,
    hasApplied: Boolean = false,
    applicationStatus: String? = null,
    onApplyClick: () -> Unit = {},
    onMessageEmployer: () -> Unit = {},
    onLoginRequired: (String) -> Unit = {} // Callback for guest mode login
) {
    Column(modifier = Modifier.fillMaxWidth().background(WorkerColors.CardBackground)) {
        // Action Buttons - Call and Apply (half-half width)
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Call Button - Half width with icon and text
            OutlinedButton(
                onClick = {
                    if (currentUser == null) {
                        onLoginRequired("call")
                    } else {
                        val phone = job.contactNumber.ifEmpty { job.phoneNumber ?: "" }
                        if (phone.isNotEmpty()) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:$phone") }
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Phone, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Call", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            
            /* COMMENTED OUT: Message Employer Button - Blue
            Button(
                onClick = {
                    if (currentUser == null) {
                        onLoginRequired("message")
                    } else {
                        onMessageEmployer()
                    }
                },
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            */
            
            /* COMMENTED OUT: WhatsApp Button - Green
            Button(
                onClick = {
                    if (currentUser == null) {
                        onLoginRequired("whatsapp")
                    } else {
                        val phone = job.contactNumber.ifEmpty { job.phoneNumber ?: "" }
                        if (phone.isNotEmpty()) {
                            com.example.dutype.components.openWhatsAppApply(
                                context = context,
                                phoneNumber = phone,
                                jobTitle = job.title,
                                companyName = job.companyName,
                                salary = job.payAmount.ifEmpty { job.salary },
                                location = job.area ?: job.location
                            )
                        } else {
                            android.widget.Toast.makeText(context, "Phone number not available", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Chat, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            */

            // Apply Now Button - Shows different states based on application status
            if (hasApplied) {
                // Already Applied - Show status button
                Button(
                    onClick = {
                        // Navigate to My Jobs to see application status
                        navController.navigate(Routes.WORKER_MY_JOBS)
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (applicationStatus) {
                            "ACCEPTED" -> Color(0xFF10B981) // Green
                            "PENDING", "UNDER_REVIEW" -> Color(0xFFF59E0B) // Amber
                            else -> Color(0xFF6B7280) // Gray
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = when (applicationStatus) {
                                "ACCEPTED" -> Icons.Default.CheckCircle
                                else -> Icons.Default.Schedule
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = when (applicationStatus) {
                                "ACCEPTED" -> "Hired!"
                                "PENDING" -> "Applied"
                                "UNDER_REVIEW" -> "Under Review"
                                else -> "Applied"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // Not Applied - Show Apply Now button
                Button(
                    onClick = {
                        if (currentUser == null) {
                            onLoginRequired("apply")
                        } else {
                            // Navigate to application screen for review before submitting
                            onApplyClick()
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply Now", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}


@Composable
private fun JobDetailsContent(job: JobListing, modifier: Modifier = Modifier, shareGenerator: JobShareImageGenerator? = null) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Job Image Section - Show if employer uploaded an image
        if (job.jobImageUrl.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(job.jobImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Job image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
        
        // Location Section - with consistent icon styling
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = job.area?.ifEmpty { job.location } ?: job.location,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = Color.Black)
                )
                Text(" • ", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9CA3AF)))
                val distanceText = when {
                    job.distance == null || job.distance!! <= 0 -> context.getString(R.string.distance_unavailable)
                    job.distance!! < 0.05 -> context.getString(R.string.less_than_50m)
                    job.distance!! < 1.0 -> "${(job.distance!! * 1000).toInt()}m ${context.getString(R.string.away)}"
                    job.distance!! < 2.0 -> String.format("%.1f km ${context.getString(R.string.walkable)}", job.distance)
                    else -> String.format("%.1f km ${context.getString(R.string.away)}", job.distance)
                }
                Text(distanceText, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
            }
        }
        
        // AI Safety Analysis Card - COMMENTED OUT
        /*
        item {
            // Calculate employer account age
            val employerAccountAgeDays = if (job.employerCreatedAt != null && job.employerCreatedAt > 0) {
                ((System.currentTimeMillis() - job.employerCreatedAt) / (24 * 60 * 60 * 1000)).toInt()
            } else 30
            
            // Check if employer has verified badge
            val hasVerifiedBadge = job.employerTrustTier.contains("VERIFIED", ignoreCase = true) ||
                job.employerTrustTier.contains("TRUSTED", ignoreCase = true)
            
            // Analyze job for scam risk
            val safetyAnalysis = remember(job) {
                analyzeJobRisk(
                    title = job.title,
                    description = job.description,
                    category = job.category,
                    payAmount = job.payAmount.ifEmpty { job.salary },
                    payType = job.payType,
                    location = job.area ?: job.location,
                    employerAccountAgeDays = employerAccountAgeDays,
                    hasVerifiedBadge = hasVerifiedBadge
                )
            }
            
            JobSafetyCard(
                analysisResult = safetyAnalysis,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        */
        
        // ACCESSIBILITY: Landmark Navigation - helps workers find location by landmarks
        if (job.landmark.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏛️", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Near: ", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF166534)))
                        Text(job.landmark, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF166534)))
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // Combined Job Details Card - White background with light border (like worker job cards)
        item {
            val trustTier = parseTrustTier(job.employerTrustTier)
            
            // Get pay info
            val payAmount = job.payAmount.ifEmpty { job.salary }.ifEmpty { if (job.payRate > 0) job.payRate.toInt().toString() else "Not specified" }
            val payTypeDisplay = when {
                job.payType.contains("hour", true) -> "per hour"
                job.payType.contains("month", true) -> "per month"
                job.payType.contains("delivery", true) || job.payType.contains("task", true) -> "per delivery"
                job.payType.contains("daily", true) || job.payType.contains("day", true) -> "per day"
                else -> "per day"
            }
            
            // Determine payment cycle from payType
            val paymentCycle = when {
                job.payType.contains("hour", true) -> "Hourly"
                job.payType.contains("daily", true) || job.payType.contains("day", true) -> "Daily"
                job.payType.contains("week", true) -> "Weekly"
                job.payType.contains("month", true) -> "Monthly"
                job.payType.contains("task", true) || job.payType.contains("delivery", true) -> "Per Task"
                job.payPeriod.isNotEmpty() -> job.payPeriod
                else -> "Not specified"
            }
            
            // Get working hours - check multiple fields
            val workingHoursDisplay = when {
                job.workingHours.isNotEmpty() -> job.workingHours
                job.shiftTiming.isNotEmpty() -> job.shiftTiming
                job.timing.isNotEmpty() -> job.timing
                else -> "Not specified"
            }
            
            // Get experience level
            val experienceDisplay = when {
                job.experienceLevel.isNotEmpty() -> job.experienceLevel
                job.experienceRequired.isNotEmpty() -> job.experienceRequired
                else -> "Not specified"
            }
            
            // Calculate employer joined time
            val employerJoinedText = if (job.employerCreatedAt != null && job.employerCreatedAt > 0) {
                val daysSinceJoined = ((System.currentTimeMillis() - job.employerCreatedAt) / (24 * 60 * 60 * 1000)).toInt()
                when {
                    daysSinceJoined < 1 -> "Today"
                    daysSinceJoined == 1 -> "1 day ago"
                    daysSinceJoined < 30 -> "$daysSinceJoined days ago"
                    daysSinceJoined < 60 -> "1 month ago"
                    daysSinceJoined < 365 -> "${daysSinceJoined / 30} months ago"
                    daysSinceJoined < 730 -> "1 year ago"
                    else -> "${daysSinceJoined / 365} years ago"
                }
            } else {
                "N/A"
            }
            
            Card(
                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Job Title - Prominent display at top
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 20.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Job Details Section
                    Text("Job Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Company Name - show at top of job details
                    if (job.companyName.isNotEmpty()) {
                        JobDetailRow(Icons.Default.Work, Color(0xFF6B7280), "Company:", job.companyName)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    
                    // Posted time - moved from header
                    if (job.postedAt > 0) {
                        JobDetailRow(Icons.Default.AccessTime, Color(0xFF6B7280), "Posted:", job.getTimeAgoDisplayText())
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    
                    // Salary/Pay
                    JobDetailRow(Icons.Default.Payments, Color(0xFF6B7280), "Salary:", if (payAmount != "Not specified") "₹$payAmount $payTypeDisplay" else payAmount)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Job Type
                    JobDetailRow(Icons.Outlined.WorkOutline, Color(0xFF6B7280), "Job Type:", job.jobType.ifEmpty { "Not specified" })
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Vacancies
                    JobDetailRow(Icons.Filled.People, Color(0xFF6B7280), "Vacancies:", "${job.vacancies}")
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Experience
                    JobDetailRow(Icons.Default.Star, Color(0xFF6B7280), "Experience:", experienceDisplay)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Gender Preference
                    val genderDisplay = job.gender.ifEmpty { "Any" }
                    JobDetailRow(Icons.Default.Person, Color(0xFF6B7280), "Gender:", genderDisplay)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Working Hours
                    JobDetailRow(Icons.Default.Schedule, Color(0xFF6B7280), "Working Hours:", workingHoursDisplay)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Payment Cycle
                    JobDetailRow(Icons.Outlined.CalendarToday, Color(0xFF6B7280), "Payment Cycle:", paymentCycle)
                    
                    // Show category if available
                    if (job.category.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        JobDetailRow(Icons.Outlined.Category, Color(0xFF6B7280), "Category:", job.category)
                    }
                    
                    /* COMMENTED OUT: Employer Trust Section
                    // Divider
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Employer Trust Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Employer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                        TrustBadge(
                            tier = trustTier,
                            size = TrustBadgeSize.MEDIUM,
                            showLabel = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Employer Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Rating - N/A for now
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("N/A", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                                Text("Rating", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280), fontSize = 11.sp))
                            }
                        }
                        
                        // Paid on time - N/A for now
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AccessTime, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("N/A", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                                Text("Paid on time", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280), fontSize = 11.sp))
                            }
                        }
                        
                        // Employer Joined
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PersonAdd, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(employerJoinedText, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black), maxLines = 1)
                                Text("Joined", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280), fontSize = 11.sp))
                            }
                        }
                    }
                    */
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // Job Description & Requirements Card - White background with light border
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Job Description Section
                    Text("Job Description", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Description bullet points - Black bullets
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        parseDescriptionToBullets(job.description).forEach { point ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("•", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), fontWeight = FontWeight.Bold, fontSize = 16.sp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(point, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), lineHeight = 22.sp))
                            }
                        }
                    }
                    
                    // Requirements Section (if available)
                    if (job.requirements.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Requirements", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Requirements bullet points - Black bullets
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            job.requirements.forEach { req ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("•", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), fontWeight = FontWeight.Bold, fontSize = 16.sp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(req, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), lineHeight = 22.sp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // DutyPe Safety Banner - Light sky blue background
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF8FF)),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Shield, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Don't pay any fee for jobs", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF1E40AF)))
                        Text("Report suspicious jobs to DutyPe", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF3B82F6)))
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun JobDetailRow(icon: ImageVector, iconColor: Color, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF6B7280), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Color.Black))
    }
}

private fun parseDescriptionToBullets(description: String): List<String> {
    val lines = description.replace("•", "\n").replace("-", "\n").replace("*", "\n").split("\n").map { it.trim() }.filter { it.isNotEmpty() && it.length > 3 }
    if (lines.size > 1) return lines.take(6)
    val sentences = description.split(".").map { it.trim() }.filter { it.isNotEmpty() && it.length > 10 }
    return if (sentences.isNotEmpty()) sentences.take(6) else listOf(description)
}

@Composable
private fun LoadingContent() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ShimmerBox(height = 100.dp) }
        item { ShimmerBox(height = 30.dp, width = 250.dp) }
        item { ShimmerBox(height = 50.dp) }
        item { ShimmerBox(height = 180.dp) }
        item { ShimmerBox(height = 60.dp) }
        item { ShimmerBox(height = 150.dp) }
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
    val errorAnimation by rememberInfiniteTransition(label = "error").animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "errorPulse"
    )

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(32.dp)) {
                Box(modifier = Modifier.size(80.dp).scale(errorAnimation).background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Error, "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp))
                }
                Text("Oops! Something went wrong", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1F2937)), textAlign = TextAlign.Center)
                Text(error, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)), textAlign = TextAlign.Center)
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)), shape = RoundedCornerShape(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                        Text("Try Again", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier, width: androidx.compose.ui.unit.Dp? = null, height: androidx.compose.ui.unit.Dp = 16.dp) {
    val shimmerColors = listOf(Color(0xFFE5E7EB), Color(0xFFF3F4F6), Color(0xFFE5E7EB))
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "shimmer")
    val brush = Brush.linearGradient(colors = shimmerColors, start = Offset(translateAnim.value - 300f, translateAnim.value - 300f), end = Offset(translateAnim.value, translateAnim.value))
    Box(modifier = modifier.background(brush, RoundedCornerShape(8.dp)).let { if (width != null) it.width(width) else it.fillMaxWidth() }.height(height))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JobDescriptionScreenPreview() {
    JobDescriptionScreen(navController = rememberNavController(), jobId = "sample_job_id")
}
