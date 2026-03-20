package com.example.dutype.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

// Helper function to get app version dynamically
private fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "Unknown"
    } catch (e: Exception) {
        Timber.e(e, "Error getting app version")
        "Unknown"
    }
}

// Helper function to get app version code
private fun getAppVersionCode(context: android.content.Context): Long {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    } catch (e: Exception) {
        Timber.e(e, "Error getting app version code")
        0L
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    userRole: String = "worker"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedRating by remember { mutableIntStateOf(0) }
    var feedbackText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("General") }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    
    // Get dynamic app version
    val appVersion = remember { getAppVersion(context) }
    val appVersionCode = remember { getAppVersionCode(context) }
    
    val categories = listOf("General", "Bug Report", "Feature Request", "UI/UX", "Performance", "Other")
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isSubmitting && !showSuccessAnimation) {
                    selectedRating = 0
                    feedbackText = ""
                    selectedCategory = "General"
                    onDismiss()
                }
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            // Success Animation Overlay
            if (showSuccessAnimation) {
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
                        // Animated checkmark
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
                                    modifier = Modifier.size(33.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(23.dp))
                        
                        Text(
                            text = "Thank You! 🙏",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Your feedback helps us improve DutyPe",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            } else {
                // Original Simple Feedback Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = "Share Your Feedback",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Help us improve DutyPe by sharing your experience",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Rating Section
                    Text(
                        text = "How would you rate your experience?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Star Rating
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { rating ->
                            Icon(
                                imageVector = if (rating <= selectedRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Rating $rating",
                                tint = if (rating <= selectedRating) Color(0xFFFBBF24) else Color(0xFFD1D5DB),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { selectedRating = rating }
                            )
                        }
                    }
                    
                    // Rating text
                    if (selectedRating > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (selectedRating) {
                                1 -> "Poor 😞"
                                2 -> "Fair 😐"
                                3 -> "Good 🙂"
                                4 -> "Very Good 😊"
                                5 -> "Excellent! 🤩"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Category Selection
                    Text(
                        text = "Feedback Category",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Category Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.take(3).forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                    selectedLabelColor = Color(0xFF3B82F6)
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.drop(3).forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                    selectedLabelColor = Color(0xFF3B82F6)
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Feedback Text Field
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = { Text("Your feedback (optional)") },
                        placeholder = { Text("Tell us what you think...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        ),
                        maxLines = 4
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Skip button
                        OutlinedButton(
                            onClick = {
                                selectedRating = 0
                                feedbackText = ""
                                selectedCategory = "General"
                                onDismiss()
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF6B7280)
                            )
                        ) {
                            Text(
                                text = "Later",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        
                        // Submit Button
                        Button(
                            onClick = {
                                if (selectedRating == 0) {
                                    Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                
                                isSubmitting = true
                                scope.launch {
                                    try {
                                        val currentUser = FirebaseAuth.getInstance().currentUser
                                        val feedbackData = mapOf(
                                            "userId" to (currentUser?.uid ?: "anonymous"),
                                            "userEmail" to (currentUser?.email ?: "anonymous"),
                                            "userRole" to userRole,
                                            "rating" to selectedRating,
                                            "category" to selectedCategory,
                                            "feedback" to feedbackText,
                                            "timestamp" to System.currentTimeMillis(),
                                            "appVersion" to appVersion,
                                            "appVersionCode" to appVersionCode,
                                            "platform" to "Android",
                                            "deviceModel" to android.os.Build.MODEL,
                                            "androidVersion" to android.os.Build.VERSION.RELEASE
                                        )
                                        
                                        Timber.d("Feedback strict mode: skipping Firestore write for app_feedback")
                                        
                                        Timber.i("Feedback submitted: rating=$selectedRating, category=$selectedCategory, version=$appVersion")
                                        
                                        // Show success animation
                                        showSuccessAnimation = true
                                        
                                        // Auto close after animation
                                        delay(2000)
                                        
                                        // Reset and close
                                        selectedRating = 0
                                        feedbackText = ""
                                        selectedCategory = "General"
                                        showSuccessAnimation = false
                                        onDismiss()
                                        
                                    } catch (e: Exception) {
                                        Timber.e(e, "Error submitting feedback")
                                        Toast.makeText(context, "Failed to submit feedback. Please try again.", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            enabled = selectedRating > 0 && !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Submit",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
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
