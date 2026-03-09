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
import androidx.compose.ui.text.font.FontWeight
import com.example.dutype.models.JobListing
import com.example.dutype.services.JobShareService

/**
 * Share Job Button Component - Industry Standard
 * 
 * Simple text-based sharing following LinkedIn/Indeed/Swiggy best practices:
 * - Instant sharing (no image generation)
 * - Works perfectly on WhatsApp, SMS, Email
 * - Android App Links (opens app if installed, else Play Store)
 * - Clean, professional format
 */

@Composable
fun ShareJobIconButton(
    job: JobListing,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF3B82F6)
) {
    val context = LocalContext.current
    val jobShareService = remember { JobShareService() }
    
    IconButton(
        onClick = {
            try {
                jobShareService.shareJob(context, job)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to share job", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share Job",
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

// REMOVED: ShareJobButton(), ShareJobOutlinedButton(), ShareJobFAB() - Dead code, never called
// Only ShareJobIconButton() and ShareJobCard() are used in the codebase
// If button variants are needed in future, they can be re-added

/**
 * Share Job Card - Full card with quick share button
 */
@Composable
fun ShareJobCard(
    job: JobListing,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val jobShareService = remember { JobShareService() }
    
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
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF166534)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Share instantly via WhatsApp, SMS, Email",
                    fontSize = 12.sp,
                    color = Color(0xFF4ADE80)
                )
            }
            
            Button(
                onClick = {
                    try {
                        jobShareService.shareJob(context, job)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981) // Green
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Share", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

