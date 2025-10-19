package com.example.dutype.worker.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.worker.models.TimeInfo
import com.example.dutype.worker.models.UrgencyLevel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDescriptionScreen(
    jobId: String,
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val jobViewModel: FirestoreJobViewModel = hiltViewModel()
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    
    // Ensure status bar color is white for this screen
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }

    var job by remember { mutableStateOf<JobListing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaved by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var retryTrigger by remember { mutableStateOf(0) }
    
    // Update isSaved when job changes
    LaunchedEffect(job) {
        job?.let { 
            isSaved = it.isSaved 
        }
    }

    // Animation states for smoother transitions
    val contentAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.3f else 1f,
        animationSpec = tween(600, easing = EaseInOutQuart),
        label = "contentAlpha"
    )

    val saveButtonScale by animateFloatAsState(
        targetValue = if (isSaved) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "saveButtonScale"
    )

    // Fetch job details when jobId changes or retry is triggered
    LaunchedEffect(jobId, retryTrigger) {
        if (jobId.isNotEmpty()) {
            isLoading = true
            error = null
            
            try {
                // Use the JobRepository to fetch job details
                val result = jobViewModel.getJobById(jobId)
                result.fold(
                    onSuccess = { fetchedJob ->
                        job = fetchedJob
                        isLoading = false
                        
                        // Note: View tracking is handled in WorkerHomeScreen when job card is clicked
                        // to avoid double counting
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

    // Handle back button - always go to worker home
    BackHandler {
        navController.navigate(Routes.WORKER_HOME) {
            popUpTo(Routes.WORKER_HOME) { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FA),
                        Color(0xFFEEF2F6)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Enhanced Custom Top Bar with job title and better styling
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RectangleShape,
                        clip = false
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RectangleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left side: Back button + Job info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Enhanced back button
                            Card(
                                onClick = {
                                    navController.navigate(Routes.WORKER_HOME) {
                                        popUpTo(Routes.WORKER_HOME) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier.size(44.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF8F9FA)
                                ),
                                shape = CircleShape,
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFF1F2937),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Job title and subtitle with better layout
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = job?.title ?: "Job Details",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = Color(0xFF1F2937),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Show company name if job is loaded
                                job?.let { currentJob ->
                                    if (!isLoading) {
                                        Text(
                                            text = currentJob.companyName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Color(0xFF6B7280),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Right side: Action buttons row (Share and Save)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Share button
                            Card(
                                onClick = {
                                    // Share job details
                                    val shareText = "🎯 JOB OPPORTUNITY\n\n💼 ${job?.title ?: "Job"}\n🏢 ${job?.companyName ?: "Company"}\n${if (job?.vacancies != null && job?.vacancies!! > 0) "👥 ${job?.vacancies} vacancies\n" else ""}💰 ${job?.payAmount ?: ""}\n📍 ${job?.location ?: ""}\n\n📝 ${job?.description ?: ""}"
                                    
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Job Opportunity: ${job?.title}")
                                    }
                                    
                                    try {
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Job via"))
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                },
                                modifier = Modifier.size(44.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF8F9FA)
                                ),
                                shape = CircleShape,
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color(0xFF6B7280),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Enhanced save button with animation
                            Card(
                                onClick = {
                                    isSaved = !isSaved
                                    if (isSaved) {
                                        savedJobsViewModel.saveJob(jobId)
                                        snackbarMessage = "Job saved to favorites!"
                                        // Update the job object as well
                                        job = job?.copy(isSaved = true)
                                    } else {
                                        savedJobsViewModel.unsaveJob(jobId)
                                        snackbarMessage = "Job removed from favorites!"
                                        // Update the job object as well
                                        job = job?.copy(isSaved = false)
                                    }
                                    showSnackbar = true
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(saveButtonScale),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSaved) Color(0xFFFFEBEE) else Color(0xFFF8F9FA)
                                ),
                                shape = CircleShape,
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (isSaved) 4.dp else 2.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (isSaved) "Unsave" else "Save",
                                        tint = if (isSaved) Color(0xFFE53E3E) else Color(0xFF6B7280),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Optional loading progress indicator
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF1E40AF),
                            trackColor = Color(0xFFE5E7EB)
                        )
                    }
                }
            }

            // Content with smooth transition - taking full remaining space
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .graphicsLayer(alpha = contentAlpha)
            ) {
                when {
                    isLoading -> {
                        LoadingContent()
                    }
                    error != null -> {
                        ErrorContent(
                            error = error!!,
                            onRetry = {
                                // Trigger retry by incrementing retryTrigger
                                retryTrigger++
                            }
                        )
                    }
                    job != null -> {
                        JobDetailsContent(
                            job = job!!,
                            isSaved = isSaved,
                            onApplyClick = {
                                // Navigate to proper job application screen instead of profile setup
                                navController.navigate("job_application/$jobId")
                            },
                            onSaveClick = {
                                isSaved = !isSaved
                                snackbarMessage = if (isSaved) "Job saved!" else "Job unsaved!"
                                showSnackbar = true
                            }
                        )
                    }
                }
            }

            // Fixed bottom action bar at the very bottom - like a navigation bar
            if (job != null) {
                AnimatedVisibility(
                    visible = !isLoading && error == null,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it }
                    ) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Call button
                        Button(
                            onClick = {
                                val phone = job?.contactNumber?.ifEmpty { job?.phoneNumber ?: "" } ?: ""
                                if (phone.isNotEmpty()) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                        data = android.net.Uri.parse("tel:$phone")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Show toast or handle error
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Call",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Apply button
                        Button(
                            onClick = {
                                navController.navigate("job_application/$jobId")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Apply",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Enhanced Snackbar with smooth animation
        AnimatedVisibility(
            visible = showSnackbar,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it / 2 }
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 110.dp)
        ) {
            LaunchedEffect(showSnackbar) {
                kotlinx.coroutines.delay(2500)
                showSnackbar = false
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1F2937)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (snackbarMessage.contains("saved")) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (snackbarMessage.contains("saved")) Color(0xFF10B981) else Color(0xFF60A5FA),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = snackbarMessage,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Job Header Shimmer
        item {
            JobHeaderShimmer()
        }
        
        // Company Info Shimmer
        item {
            CompanyInfoShimmer()
        }
        
        // Job Description Shimmer
        item {
            JobDescriptionShimmer()
        }
        
        // Requirements Shimmer
        item {
            RequirementsShimmer()
        }
        
        // Benefits Shimmer
        item {
            BenefitsShimmer()
        }
        
        // Job Details Shimmer
        item {
            JobDetailsShimmer()
        }
        
        // Contact Info Shimmer
        item {
            ContactInfoShimmer()
        }
        
        // Action Buttons Shimmer
        item {
            ActionButtonsShimmer()
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    val errorAnimation by rememberInfiniteTransition(label = "error").animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "errorPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                // Animated error icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(errorAnimation)
                        .background(
                            Color(0xFFEF4444).copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Error title
                Text(
                    text = "Oops! Something went wrong",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )

                // Error message
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                // Enhanced retry button
                Card(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E40AF)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Try Again",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp
                                )
                            )
                        }
                    }
                }

                // Additional help text
                Text(
                    text = "Please check your internet connection and try again",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun JobDetailsContent(
    job: JobListing,
    isSaved: Boolean,
    onApplyClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Job Header Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3B82F6))
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = "Company",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = job.companyName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                if (job.payAmount.isNotEmpty() || job.salary.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = "Salary",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        if (job.payAmount.isNotEmpty() && job.payType.isNotEmpty()) {
                            Text(
                                text = if (job.payAmount.contains("/")) {
                                    "₹${job.payAmount}"
                                } else {
                                    "₹${job.payAmount}/${job.payType.lowercase()}"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (job.payAmount.isNotEmpty()) {
                            Text(
                                text = "₹${job.payAmount}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = job.location,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (job.jobType.isNotEmpty()) {
                        item {
                            Text(
                                text = job.jobType,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (job.experienceLevel.isNotEmpty()) {
                        item {
                            Text(
                                text = job.experienceLevel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Job Description
        item {
            Column {
                Text(
                    text = "Job Description",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF374151)
                )
            }
        }

        // Requirements
        if (job.requirements.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Requirements",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        job.requirements.forEach { requirement ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = requirement,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF374151),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Benefits
        if (job.benefits.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Benefits & Perks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        job.benefits.forEach { benefit ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = benefit,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF374151),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Job Details
        item {
            Column {
                Text(
                    text = "Job Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (job.jobType.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Work,
                                contentDescription = null,
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Job Type: ${job.jobType}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                    if (job.experienceLevel.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Experience: ${job.experienceLevel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                    if (job.workingHours.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Working Hours: ${job.workingHours}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                }
            }
        }

        // Contact Information
        if (job.contactNumber.isNotEmpty() || job.phoneNumber?.isNotEmpty() == true) {
            item {
                Column {
                    Text(
                        text = "Contact Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val phoneNumber = job.contactNumber.ifEmpty { job.phoneNumber ?: "" }
                    if (phoneNumber.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Phone",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = phoneNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF374151)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            iconColor.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color(0xFF1F2937)
                )
            }

            content()
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor
        )
    }
}

@Composable
private fun EnhancedTagChip(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun DetailInfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(110.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 18.sp
            )
        }
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
        else -> {
            val date = java.util.Date(timestamp)
            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(date)
        }
    }
}

// Shimmer Components
@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp? = null,
    height: androidx.compose.ui.unit.Dp = 16.dp
) {
    val shimmerColors = listOf(
        Color(0xFFE5E7EB),
        Color(0xFFF3F4F6),
        Color(0xFFE5E7EB)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 300f, translateAnim.value - 300f),
        end = Offset(translateAnim.value, translateAnim.value)
    )

    Box(
        modifier = modifier
            .background(brush, RoundedCornerShape(8.dp))
            .let {
                if (width != null) {
                    it.width(width)
                } else {
                    it.fillMaxWidth()
                }
            }
            .height(height)
    )
}

@Composable
private fun JobHeaderShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShimmerBox(width = 280.dp, height = 32.dp)
            ShimmerBox(width = 200.dp, height = 24.dp)
            ShimmerBox(width = 140.dp, height = 20.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(width = 120.dp, height = 16.dp)
                ShimmerBox(width = 80.dp, height = 16.dp)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBox(width = 80.dp, height = 24.dp)
                ShimmerBox(width = 100.dp, height = 24.dp)
                ShimmerBox(width = 90.dp, height = 24.dp)
            }
        }
    }
}

@Composable
private fun CompanyInfoShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(width = 36.dp, height = 36.dp)
                ShimmerBox(width = 150.dp, height = 24.dp)
            }
            ShimmerBox(width = null, height = 16.dp)
            ShimmerBox(width = null, height = 16.dp)
            ShimmerBox(width = 200.dp, height = 16.dp)
        }
    }
}

@Composable
private fun JobDescriptionShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(width = 36.dp, height = 36.dp)
                ShimmerBox(width = 140.dp, height = 24.dp)
            }
            ShimmerBox(width = null, height = 16.dp)
            ShimmerBox(width = null, height = 16.dp)
            ShimmerBox(width = null, height = 16.dp)
            ShimmerBox(width = 250.dp, height = 16.dp)
        }
    }
}

@Composable
private fun RequirementsShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(width = 36.dp, height = 36.dp)
                ShimmerBox(width = 120.dp, height = 24.dp)
            }
            repeat(3) {
                ShimmerBox(width = null, height = 16.dp)
            }
        }
    }
}

@Composable
private fun BenefitsShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(width = 36.dp, height = 36.dp)
                ShimmerBox(width = 140.dp, height = 24.dp)
            }
            repeat(3) {
                ShimmerBox(width = null, height = 16.dp)
            }
        }
    }
}

@Composable
private fun JobDetailsShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(width = 36.dp, height = 36.dp)
                ShimmerBox(width = 100.dp, height = 24.dp)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    ShimmerBox(width = 120.dp, height = 100.dp)
                }
            }
        }
    }
}

@Composable
private fun ContactInfoShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(width = 36.dp, height = 36.dp)
                ShimmerBox(width = 160.dp, height = 24.dp)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(width = 20.dp, height = 20.dp)
                ShimmerBox(width = 120.dp, height = 16.dp)
            }
        }
    }
}

@Composable
private fun ActionButtonsShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(
                modifier = Modifier.weight(1f),
                width = null,
                height = 56.dp
            )
            ShimmerBox(
                modifier = Modifier.weight(1f),
                width = null,
                height = 56.dp
            )
        }
    }
}
