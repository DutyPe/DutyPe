package com.example.dutype.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import timber.log.Timber
import java.io.File

/**
 * Selfie Capture Step Component for Profile Setup
 * Supports both Worker (black/gray theme) and Employer (blue/purple theme) roles
 */
@Composable
fun SelfieCaptureStep(
    selfieUri: Uri?,
    isUploading: Boolean,
    selfieError: String?,
    isEmployer: Boolean = false,
    onSelfieCapture: (Uri) -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Theme colors based on role
    val primaryColor = if (isEmployer) Color(0xFF8B5CF6) else Color(0xFF3B82F6)
    val secondaryColor = if (isEmployer) Color(0xFFD8B4FE) else Color(0xFF60A5FA)
    val accentColor = if (isEmployer) Color(0xFF8B5CF6) else Color(0xFF111111)

    // Create temp file for camera capture
    fun createImageFile(): Uri {
        val imageFile = File.createTempFile(
            "selfie_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempImageUri != null) {
            Timber.d("📸 Selfie captured successfully: $tempImageUri")
            onSelfieCapture(tempImageUri!!)
        } else {
            Timber.w("📸 Selfie capture failed or cancelled")
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            tempImageUri = createImageFile()
            tempImageUri?.let { cameraLauncher.launch(it) }
        } else {
            Timber.w("📸 Camera permission denied")
        }
    }
    
    // Check initial permission state
    LaunchedEffect(Unit) {
        hasCameraPermission = context.checkSelfPermission(Manifest.permission.CAMERA) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    Column(
        modifier = Modifier.padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.15f),
                                secondaryColor.copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Text(
                    text = "Profile Photo",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "Take a clear selfie for verification",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        
        // Selfie capture area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF9FAFB)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Selfie preview or placeholder
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(
                            if (selfieUri != null) Color.Transparent
                            else Color(0xFFE5E7EB)
                        )
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                colors = if (selfieUri != null) 
                                    listOf(Color(0xFF10B981), Color(0xFF34D399))
                                else 
                                    listOf(primaryColor, secondaryColor)
                            ),
                            shape = CircleShape
                        )
                        .clickable(enabled = !isUploading) {
                            if (hasCameraPermission) {
                                tempImageUri = createImageFile()
                                tempImageUri?.let { cameraLauncher.launch(it) }
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = primaryColor,
                            strokeWidth = 3.dp
                        )
                    } else if (selfieUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(selfieUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Selfie preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        // Success checkmark overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-8).dp, y = (-8).dp)
                                .size(40.dp)
                                .background(Color(0xFF10B981), CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Verified",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to capture",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }
                    }
                }
                
                // Action buttons
                if (selfieUri != null && !isUploading) {
                    OutlinedButton(
                        onClick = {
                            onRetake()
                            if (hasCameraPermission) {
                                tempImageUri = createImageFile()
                                tempImageUri?.let { cameraLauncher.launch(it) }
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accentColor
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retake Photo")
                    }
                } else if (!isUploading) {
                    Button(
                        onClick = {
                            if (hasCameraPermission) {
                                tempImageUri = createImageFile()
                                tempImageUri?.let { cameraLauncher.launch(it) }
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        )
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Selfie")
                    }
                }
                
                // Guidelines
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = primaryColor.copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Photo Guidelines",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937)
                            )
                        )
                        GuidelineItem(
                            icon = Icons.Default.Face,
                            text = "Face the camera directly",
                            color = primaryColor
                        )
                        GuidelineItem(
                            icon = Icons.Default.WbSunny,
                            text = "Ensure good lighting",
                            color = primaryColor
                        )
                        GuidelineItem(
                            icon = Icons.Default.RemoveRedEye,
                            text = "Keep eyes open and visible",
                            color = primaryColor
                        )
                        GuidelineItem(
                            icon = Icons.Default.Block,
                            text = "No sunglasses or masks",
                            color = primaryColor
                        )
                    }
                }
            }
        }
        
        // Error message
        if (selfieError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selfieError,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFEF4444)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidelineItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF6B7280)
            )
        )
    }
}
