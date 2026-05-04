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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.dutype.app.R
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
                Toast.makeText(context, context.getString(R.string.failed_share_job), Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = stringResource(R.string.share_job_content_description),
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

