package com.example.dutype.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.JobListing
import com.example.dutype.services.JobShareImageGenerator
import kotlinx.coroutines.launch

/**
 * Share Job Button Component
 * 
 * Generates a branded image with job details and shares via system share sheet.
 * Can be used as:
 * - Icon button (compact)
 * - Full button with text
 * - FAB style
 */

@Composable
fun ShareJobIconButton(
    job: JobListing,
    jobShareImageGenerator: JobShareImageGenerator,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF3B82F6)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    IconButton(
        onClick = {
            if (!isLoading) {
                isLoading = true
                scope.launch {
                    val result = jobShareImageGenerator.shareJob(context, job)
                    isLoading = false
                    if (result.isFailure) {
                        Toast.makeText(context, "Failed to share job", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        modifier = modifier,
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = tint
            )
        } else {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share Job",
                tint = tint
            )
        }
    }
}

@Composable
fun ShareJobButton(
    job: JobListing,
    jobShareImageGenerator: JobShareImageGenerator,
    modifier: Modifier = Modifier,
    text: String = "Share Job",
    containerColor: Color = Color(0xFF3B82F6),
    contentColor: Color = Color.White
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    Button(
        onClick = {
            if (!isLoading) {
                isLoading = true
                scope.launch {
                    val result = jobShareImageGenerator.shareJob(context, job)
                    isLoading = false
                    if (result.isFailure) {
                        Toast.makeText(context, "Failed to share job", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        modifier = modifier,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isLoading) "Generating..." else text,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ShareJobOutlinedButton(
    job: JobListing,
    jobShareImageGenerator: JobShareImageGenerator,
    modifier: Modifier = Modifier,
    text: String = "Share",
    borderColor: Color = Color(0xFF3B82F6)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    OutlinedButton(
        onClick = {
            if (!isLoading) {
                isLoading = true
                scope.launch {
                    val result = jobShareImageGenerator.shareJob(context, job)
                    isLoading = false
                    if (result.isFailure) {
                        Toast.makeText(context, "Failed to share job", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        modifier = modifier,
        enabled = !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = borderColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = borderColor
            )
        } else {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, fontSize = 14.sp)
        }
    }
}

/**
 * Share Job FAB - Floating Action Button style
 */
@Composable
fun ShareJobFAB(
    job: JobListing,
    jobShareImageGenerator: JobShareImageGenerator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    FloatingActionButton(
        onClick = {
            if (!isLoading) {
                isLoading = true
                scope.launch {
                    val result = jobShareImageGenerator.shareJob(context, job)
                    isLoading = false
                    if (result.isFailure) {
                        Toast.makeText(context, "Failed to share job", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        modifier = modifier,
        containerColor = Color(0xFF25D366), // WhatsApp green
        contentColor = Color.White
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share Job"
            )
        }
    }
}

/**
 * Share Job Card - Full card with preview
 */
@Composable
fun ShareJobCard(
    job: JobListing,
    jobShareImageGenerator: JobShareImageGenerator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0FDF4) // Light green
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📤 Share this job",
                    fontSize = 16.sp,
                    color = Color(0xFF166534)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Share on WhatsApp, Instagram & more",
                    fontSize = 12.sp,
                    color = Color(0xFF4ADE80)
                )
            }
            
            Button(
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        scope.launch {
                            val result = jobShareImageGenerator.shareJob(context, job)
                            isLoading = false
                            if (result.isFailure) {
                                Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF25D366)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Share", fontSize = 14.sp)
                }
            }
        }
    }
}
