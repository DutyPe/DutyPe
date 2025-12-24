package com.example.dutype.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.EmployerRatingTags
import com.example.dutype.models.JobRating
import com.example.dutype.models.RatingUserRole
import com.example.dutype.models.WorkerRatingTags
import com.example.dutype.services.RatingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Job Rating Bottom Sheet
 * Used by both workers (to rate employers) and employers (to rate workers)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobRatingBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    jobId: String,
    applicationId: String,
    jobTitle: String,
    companyName: String,
    ratedUserId: String,
    ratedUserName: String,
    ratedUserRole: RatingUserRole,
    raterUserId: String,
    raterUserRole: RatingUserRole,
    ratingService: RatingService,
    onRatingSubmitted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Rating states
    var overallRating by remember { mutableIntStateOf(0) }
    var punctualityRating by remember { mutableIntStateOf(0) }
    var qualityRating by remember { mutableIntStateOf(0) }
    var communicationRating by remember { mutableIntStateOf(0) }
    var professionalismRating by remember { mutableIntStateOf(0) }
    var paymentRating by remember { mutableIntStateOf(0) }
    var feedbackText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    
    // Determine which tags to show based on who is being rated
    val availableTags = if (ratedUserRole == RatingUserRole.WORKER) {
        WorkerRatingTags.POSITIVE + WorkerRatingTags.NEGATIVE
    } else {
        EmployerRatingTags.POSITIVE + EmployerRatingTags.NEGATIVE
    }
    
    // Colors
    val primaryColor = if (raterUserRole == RatingUserRole.WORKER) {
        Color(0xFF1F2937) // Black for worker
    } else {
        Color(0xFF3B82F6) // Blue for employer
    }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isSubmitting && !showSuccessAnimation) {
                    onDismiss()
                }
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            if (showSuccessAnimation) {
                // Success Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AnimatedVisibility(
                            visible = showSuccessAnimation,
                            enter = scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF10B981),
                                                Color(0xFF059669)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Thank You! 🙏",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Your rating helps build trust in DutyPe",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                }
            } else {
                // Rating Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = if (ratedUserRole == RatingUserRole.WORKER) 
                            "Rate Worker" else "Rate Employer",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = ratedUserName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    
                    Text(
                        text = jobTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Overall Rating
                    RatingSection(
                        title = "Overall Experience",
                        rating = overallRating,
                        onRatingChange = { overallRating = it }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Specific ratings based on who is being rated
                    if (ratedUserRole == RatingUserRole.WORKER) {
                        // Rating a worker
                        RatingSection(
                            title = "Punctuality",
                            rating = punctualityRating,
                            onRatingChange = { punctualityRating = it },
                            subtitle = "Did they arrive on time?"
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        RatingSection(
                            title = "Work Quality",
                            rating = qualityRating,
                            onRatingChange = { qualityRating = it },
                            subtitle = "Quality of work done"
                        )
                    } else {
                        // Rating an employer
                        RatingSection(
                            title = "Payment",
                            rating = paymentRating,
                            onRatingChange = { paymentRating = it },
                            subtitle = "Did they pay on time?"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    RatingSection(
                        title = "Communication",
                        rating = communicationRating,
                        onRatingChange = { communicationRating = it },
                        subtitle = "Clear and responsive"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    RatingSection(
                        title = "Professionalism",
                        rating = professionalismRating,
                        onRatingChange = { professionalismRating = it },
                        subtitle = "Professional behavior"
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Quick Tags
                    Text(
                        text = "Quick Tags (Optional)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableTags) { tag ->
                            val isSelected = selectedTags.contains(tag)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTags = if (isSelected) {
                                        selectedTags - tag
                                    } else {
                                        selectedTags + tag
                                    }
                                },
                                label = { Text(tag, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.1f),
                                    selectedLabelColor = primaryColor
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Feedback Text
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = { Text("Additional Feedback (Optional)") },
                        placeholder = { Text("Share your experience...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        ),
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Submit Button
                    Button(
                        onClick = {
                            if (overallRating == 0) {
                                Toast.makeText(context, "Please provide an overall rating", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isSubmitting = true
                            scope.launch {
                                try {
                                    val rating = JobRating(
                                        jobId = jobId,
                                        applicationId = applicationId,
                                        ratedUserId = ratedUserId,
                                        ratedUserRole = ratedUserRole,
                                        raterUserId = raterUserId,
                                        raterUserRole = raterUserRole,
                                        overallRating = overallRating,
                                        punctualityRating = punctualityRating,
                                        qualityRating = qualityRating,
                                        communicationRating = communicationRating,
                                        professionalismRating = professionalismRating,
                                        paymentRating = paymentRating,
                                        feedback = feedbackText,
                                        tags = selectedTags,
                                        jobTitle = jobTitle,
                                        companyName = companyName
                                    )
                                    
                                    val result = ratingService.submitRating(rating)
                                    
                                    if (result.isSuccess) {
                                        showSuccessAnimation = true
                                        delay(2000)
                                        onRatingSubmitted()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            result.exceptionOrNull()?.message ?: "Failed to submit rating",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Error submitting rating")
                                    Toast.makeText(context, "Failed to submit rating", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = overallRating > 0 && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Submit Rating",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Skip button
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting
                    ) {
                        Text(
                            text = "Maybe Later",
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingSection(
    title: String,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    subtitle: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151)
                    )
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF9CA3AF)
                        )
                    )
                }
            }
            
            // Star Rating
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { star ->
                    Icon(
                        imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Star $star",
                        tint = if (star <= rating) Color(0xFFFBBF24) else Color(0xFFD1D5DB),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onRatingChange(star) }
                    )
                }
            }
        }
    }
}
