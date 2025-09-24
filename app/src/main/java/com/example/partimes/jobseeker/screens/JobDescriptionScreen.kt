package com.example.partimes.jobseeker.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.example.partimes.jobseeker.models.JobCardModel
import com.example.partimes.models.JobListing
import com.example.partimes.ui.theme.JobseekerGradientBackground
import com.example.partimes.navigation.Routes
import com.example.partimes.viewmodels.JobViewModel
import com.example.partimes.viewmodels.SavedJobsViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDescriptionScreen(
    jobId: String,
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val jobViewModel: JobViewModel = hiltViewModel()
    val savedJobsViewModel: SavedJobsViewModel = hiltViewModel()
    
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

    // Handle back button
    BackHandler {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        job?.title ?: "Job Details", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSaved = !isSaved
                            snackbarMessage = if (isSaved) "Job saved!" else "Job unsaved!"
                            showSnackbar = true
                        }
                    ) {
                        Icon(
                            if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isSaved) "Unsave" else "Save",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E40AF)
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            // Navigate to application form
                            navController.navigate("${Routes.PROFILE_SETUP}?jobId=$jobId")
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
    }

    // Snackbar for user feedback
    if (showSnackbar) {
        LaunchedEffect(showSnackbar) {
            kotlinx.coroutines.delay(2000)
            showSnackbar = false
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E40AF)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = snackbarMessage,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Failed to load job details",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF374151),
                textAlign = TextAlign.Center
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E40AF)
                )
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun JobDetailsContent(
    job: JobListing,
    isSaved: Boolean,
    onApplyClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Job Header
        item {
            JobHeaderCard(job = job)
        }
        
        // Company Info
        item {
            CompanyInfoCard(job = job)
        }
        
        // Job Description
        item {
            JobDescriptionCard(job = job)
        }
        
        // Requirements
        if (job.requirements.isNotEmpty()) {
            item {
                RequirementsCard(requirements = job.requirements)
            }
        }
        
        // Benefits
        if (job.benefits.isNotEmpty()) {
            item {
                BenefitsCard(benefits = job.benefits)
            }
        }
        
        // Job Details
        item {
            JobDetailsCard(job = job)
        }
        
        // Contact Info
        item {
            ContactInfoCard(job = job)
        }
        
        // Action Buttons
        item {
            ActionButtonsCard(
                onApplyClick = onApplyClick,
                onSaveClick = onSaveClick,
                isSaved = isSaved
            )
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun JobHeaderCard(job: JobListing) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Job Title
            Text(
                text = job.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1F2937)
            )
            
            // Company Name
            Text(
                text = job.companyName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF6B7280)
            )
            
            // Pay Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AttachMoney,
                    contentDescription = "Pay",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "₹${job.wage} ${job.payType}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF10B981)
                )
            }
            
            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = job.location,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF374151)
                )
            }
            
            // Posted Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Posted",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Posted ${getTimeAgo(job.postedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
private fun CompanyInfoCard(job: JobListing) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "About Company",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1F2937)
            )
            
            Text(
                text = job.companyName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF374151)
            )
            
            if (job.industry.isNotEmpty()) {
                Text(
                    text = "Industry: ${job.industry}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
            
            if (job.companySize.isNotEmpty()) {
                Text(
                    text = "Company Size: ${job.companySize} employees",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}

@Composable
private fun JobDescriptionCard(job: JobListing) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Job Description",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1F2937)
            )
            
            Text(
                text = job.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF374151),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun RequirementsCard(requirements: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Requirements",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1F2937)
            )
            
            requirements.forEach { requirement ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1E40AF),
                        fontWeight = FontWeight.Bold
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

@Composable
private fun BenefitsCard(benefits: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Benefits",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1F2937)
            )
            
            benefits.forEach { benefit ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
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

@Composable
private fun JobDetailsCard(job: JobListing) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Job Details",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1F2937)
            )
            
            // Job Type
            DetailRow(
                icon = Icons.Default.Work,
                label = "Job Type",
                value = job.jobType
            )
            
            // Working Hours
            if (job.workingHours.isNotEmpty()) {
                DetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Working Hours",
                    value = job.workingHours
                )
            }
            
            // Experience Required
            if (job.experienceRequired.isNotEmpty()) {
                DetailRow(
                    icon = Icons.Default.School,
                    label = "Experience Required",
                    value = job.experienceRequired
                )
            }
            
            // Vacancies
            DetailRow(
                icon = Icons.Default.People,
                label = "Vacancies",
                value = job.vacancies.toString()
            )
            
            // Gender Preference
            if (job.gender.isNotEmpty() && job.gender != "Any") {
                DetailRow(
                    icon = Icons.Default.Person,
                    label = "Gender Preference",
                    value = job.gender
                )
            }
            
            // Age Range
            if (job.ageRange.isNotEmpty()) {
                DetailRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Age Range",
                    value = job.ageRange
                )
            }
            
            // Application Deadline
            if (job.applicationDeadline.isNotEmpty()) {
                DetailRow(
                    icon = Icons.Default.Event,
                    label = "Application Deadline",
                    value = job.applicationDeadline
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF374151),
            modifier = Modifier.width(120.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ContactInfoCard(job: JobListing) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Contact Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF1F2937)
            )
            
            if (job.contactNumber.isNotEmpty()) {
                DetailRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = job.contactNumber
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsCard(
    onApplyClick: () -> Unit,
    onSaveClick: () -> Unit,
    isSaved: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Save Button
            OutlinedButton(
                onClick = onSaveClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1E40AF)
                ),
                border = BorderStroke(1.dp, Color(0xFF1E40AF))
            ) {
                Icon(
                    if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isSaved) "Unsave" else "Save",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaved) "Saved" else "Save Job",
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Apply Button
            Button(
                onClick = onApplyClick,
                modifier = Modifier.weight(2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E40AF)
                )
            ) {
                Text(
                    text = "Apply Now",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
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
            animation = tween(1200, easing = LinearEasing),
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
            .background(brush)
            .let { 
                if (width != null) {
                    it.width(width)
                } else {
                    it.width(100.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(width = 200.dp, height = 24.dp)
            ShimmerBox(width = 150.dp, height = 20.dp)
            ShimmerBox(width = 120.dp, height = 20.dp)
            ShimmerBox(width = 180.dp, height = 16.dp)
            ShimmerBox(width = 100.dp, height = 16.dp)
        }
    }
}

@Composable
private fun CompanyInfoShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(width = 120.dp, height = 20.dp)
            ShimmerBox(width = 180.dp, height = 18.dp)
            ShimmerBox(width = 100.dp, height = 16.dp)
            ShimmerBox(width = 140.dp, height = 16.dp)
        }
    }
}

@Composable
private fun JobDescriptionShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(width = 120.dp, height = 20.dp)
            repeat(4) {
                ShimmerBox(width = null, height = 16.dp)
            }
        }
    }
}

@Composable
private fun RequirementsShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(width = 100.dp, height = 20.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(width = 80.dp, height = 20.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(width = 100.dp, height = 20.dp)
            repeat(5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShimmerBox(width = 20.dp, height = 20.dp)
                    ShimmerBox(width = 120.dp, height = 16.dp)
                    ShimmerBox(width = 100.dp, height = 16.dp)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(width = 140.dp, height = 20.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(width = 20.dp, height = 20.dp)
                ShimmerBox(width = 120.dp, height = 16.dp)
                ShimmerBox(width = 100.dp, height = 16.dp)
            }
        }
    }
}

@Composable
private fun ActionButtonsShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
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
                height = 48.dp
            )
            ShimmerBox(
                modifier = Modifier.weight(2f),
                width = null,
                height = 48.dp
            )
        }
    }
}
