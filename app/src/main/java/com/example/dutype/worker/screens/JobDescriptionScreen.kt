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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Shield
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
import com.example.dutype.viewmodels.SavedJobsViewModel

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
    val locationPreferences = remember { com.example.dutype.location.LocationPreferences(context) }
    val currentLocation by locationPreferences.currentLocation.collectAsState()
    
    LaunchedEffect(Unit) { onStatusBarColorChange(Color.White) }

    var job by remember { mutableStateOf<JobListing?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaved by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var retryTrigger by remember { mutableStateOf(0) }
    
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    
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
                            val distance = com.example.dutype.location.calculateDistance(
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
        navController.navigate(Routes.WORKER_HOME) {
            popUpTo(Routes.WORKER_HOME) { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header - Simple with back arrow, title and company
            Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        navController.navigate(Routes.WORKER_HOME) {
                            popUpTo(Routes.WORKER_HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.Black)
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ValidationUtils.capitalizeWords(job?.title ?: "Driver"),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.Black),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        job?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(it.companyName, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF10B981), trackColor = Color(0xFFE5E7EB))
            }

            // Content
            Box(modifier = Modifier.fillMaxSize().weight(1f).graphicsLayer(alpha = contentAlpha)) {
                when {
                    isLoading -> LoadingContent()
                    error != null -> ErrorContent(error!!) { retryTrigger++ }
                    job != null -> JobDetailsContent(job!!)
                }
            }

            // Bottom Action Bar
            if (job != null && !isLoading && error == null) {
                BottomActionBar(job!!, currentUser, context, navController, jobId)
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
}

@Composable
private fun BottomActionBar(
    job: JobListing,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    context: android.content.Context,
    navController: NavController,
    jobId: String
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Call Button - Outlined
            OutlinedButton(
                onClick = {
                    if (currentUser == null) {
                        android.widget.Toast.makeText(context, "Please login to call", android.widget.Toast.LENGTH_SHORT).show()
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
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Phone, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Text("Call Employer", color = Color.Black, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }

            // Apply Now Button - Dark color (same as before)
            Button(
                onClick = {
                    if (currentUser == null) {
                        android.widget.Toast.makeText(context, "Please login to apply", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        navController.navigate("job_application/$jobId")
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


@Composable
private fun JobDetailsContent(job: JobListing, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Pay Section - Card with border
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val payAmount = job.payAmount.ifEmpty { job.salary }.ifEmpty { if (job.payRate > 0) job.payRate.toInt().toString() else "1000" }
                    val payType = when {
                        job.payType.contains("hour", true) -> "hour"
                        job.payType.contains("month", true) -> "month"
                        job.payType.contains("delivery", true) || job.payType.contains("task", true) -> "delivery"
                        else -> "day"
                    }
                    
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("₹$payAmount", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 32.sp))
                        Text(" / $payType", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF6B7280), fontSize = 16.sp))
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Payment protected by ", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)))
                        Text("DutyPe", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(12.dp)) }
        
        // Location Section
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
                val distanceText = if (job.distance != null && job.distance!! > 0) String.format("%.1f km away", job.distance) else "0.0 km away"
                Text(distanceText, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)))
            }
        }
        
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        // DutyPe Safety Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Shield, null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Don't pay any fee for the jobs", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E)))
                    Text(" - Please report to ", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF92400E)))
                    Text("DutyPe", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF92400E)))
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // Job Details Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Job Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                    
                    JobDetailRow(Icons.Default.Schedule, Color(0xFF6B7280), "Job Type:", job.jobType.ifEmpty { "Part-time" })
                    JobDetailRow(Icons.Default.Work, Color(0xFF6B7280), "Experience:", job.experienceLevel.ifEmpty { "Entry Level" })
                    JobDetailRow(Icons.Default.AccessTime, Color(0xFF6B7280), "Working Hours:", job.workingHours.ifEmpty { "Flexible" })
                    JobDetailRow(Icons.Default.Payments, Color(0xFF6B7280), "Payment Cycle:", "Weekly / Monthly")
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(12.dp)) }
        
        // Employer Trust Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Employer Trust: ", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black))
                        Text("4.8", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paid on time", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)))
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(20.dp))
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        // Job Description Section
        item {
            Text("Job Description", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Description bullet points
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                parseDescriptionToBullets(job.description).forEach { point ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("•", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 16.sp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(point, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), lineHeight = 22.sp))
                    }
                }
            }
        }
        
        // Requirements
        if (job.requirements.isNotEmpty()) {
            item { 
                Spacer(modifier = Modifier.height(20.dp))
                Text("Requirements", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black))
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    job.requirements.forEach { req ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("•", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 16.sp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(req, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF374151), lineHeight = 22.sp))
                        }
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
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
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
