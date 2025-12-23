package com.example.dutype.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Professional Profile Completion Progress Component
 * Enhanced with 30+ years of Android development experience
 * Shows completion percentage with smooth animations and professional styling
 */
@Composable
fun ProfileCompletionProgress(
    completionPercentage: Int,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
    isCompleted: Boolean = false,
    isWorker: Boolean = true
) {
    // Animated progress value
    val animatedProgress by animateFloatAsState(
        targetValue = completionPercentage / 100f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = EaseInOutCubic
        ),
        label = "progress_animation"
    )
    
    // Animated percentage text
    val animatedPercentage by animateIntAsState(
        targetValue = completionPercentage,
        animationSpec = tween(
            durationMillis = 1200,
            easing = EaseOutCubic
        ),
        label = "percentage_animation"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) 
                Color(0xFF10B981).copy(alpha = 0.1f) 
            else 
                Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isCompleted) 
                                    Color(0xFF10B981) 
                                else 
                                    Color(0xFF3B82F6),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = if (isCompleted) "Profile Complete!" else "Profile Completion",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isCompleted) 
                                Color(0xFF10B981) 
                            else 
                                Color(0xFF1F2937)
                        )
                        Text(
                            text = if (isCompleted) "All information provided" else "Complete your profile to get started",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                
                // Percentage display
                Text(
                    text = "${animatedPercentage}%",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isCompleted) 
                        Color(0xFF10B981) 
                    else 
                        Color(0xFF3B82F6)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(4.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            brush = if (isCompleted) {
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF10B981),
                                        Color(0xFF059669)
                                    )
                                )
                            } else {
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6),
                                        Color(0xFF1D4ED8)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
            
            // Progress details (if enabled)
            if (showDetails && !isCompleted) {
                // Details content can be added here if needed
            }
        }
    }
}


/**
 * Profile Picture Upload Component
 * Displays the actual profile image and handles upload
 */
@Composable
fun ProfilePictureUpload(
    currentImageUri: String?,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isUploading: Boolean = false,
    onPickImage: (() -> Unit)? = null,
    localImageUri: android.net.Uri? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile picture display - shows actual image
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(Color(0xFFF3F4F6))
                .then(
                    if (onPickImage != null && !isUploading) {
                        Modifier.clickable { onPickImage() }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isUploading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = Color(0xFF3B82F6),
                        strokeWidth = 3.dp
                    )
                }
                localImageUri != null -> {
                    // Show locally selected image (before upload completes)
                    coil.compose.AsyncImage(
                        model = localImageUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                currentImageUri != null && currentImageUri.isNotBlank() -> {
                    // Show image from URL (Firebase Storage)
                    coil.compose.AsyncImage(
                        model = currentImageUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                else -> {
                    // Show placeholder
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
            
            // Camera overlay icon
            if (!isUploading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .background(Color(0xFF3B82F6), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Photo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(
            onClick = { onPickImage?.invoke() },
            enabled = !isUploading
        ) {
            Text(
                text = if (currentImageUri != null) "Change Photo" else "Add Photo",
                color = Color(0xFF3B82F6),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
