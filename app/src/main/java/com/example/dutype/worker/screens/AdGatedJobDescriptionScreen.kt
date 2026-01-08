package com.example.dutype.worker.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.viewmodels.AdViewModel
import timber.log.Timber

/**
 * Ad-Gated Job Description Screen
 * 
 * Shows a rewarded ad before allowing worker to view job description.
 * If worker has already viewed this job today, skips the ad.
 */
@Composable
fun AdGatedJobDescriptionScreen(
    jobId: String,
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adViewModel: AdViewModel = hiltViewModel()
    
    var showAdPrompt by remember { mutableStateOf(false) }
    var isLoadingAd by remember { mutableStateOf(false) }
    var canViewJob by remember { mutableStateOf(false) }
    var adCheckComplete by remember { mutableStateOf(false) }
    
    // Check if worker has already viewed this job today
    LaunchedEffect(jobId) {
        adViewModel.loadWorkerRewardedAd(context)
        
        if (adViewModel.hasViewedJobToday(context, jobId)) {
            // Already viewed today - skip ad
            Timber.d("📺 Job $jobId already viewed today, skipping ad")
            canViewJob = true
        } else {
            // Need to show ad
            showAdPrompt = true
        }
        adCheckComplete = true
    }
    
    // If can view job, show the actual job description
    if (canViewJob) {
        JobDescriptionScreen(
            jobId = jobId,
            navController = navController,
            onStatusBarColorChange = onStatusBarColorChange
        )
        return
    }
    
    // Show ad prompt or loading state
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (!adCheckComplete) {
            // Loading state
            CircularProgressIndicator(
                color = Color(0xFF3B82F6),
                modifier = Modifier.size(48.dp)
            )
        } else if (showAdPrompt) {
            // Ad prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "📺",
                    fontSize = 64.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Watch a Short Ad",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Watch a quick 15-30 second ad to view this job's full details. It's FREE!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280)
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        if (activity != null) {
                            isLoadingAd = true
                            adViewModel.showWorkerRewardedAdForJob(
                                context = context,
                                jobId = jobId,
                                activity = activity,
                                onCanView = {
                                    isLoadingAd = false
                                    canViewJob = true
                                },
                                onAdNotReady = {
                                    isLoadingAd = false
                                    // Ad not ready - allow viewing anyway (better UX)
                                    canViewJob = true
                                }
                            )
                        }
                    },
                    enabled = !isLoadingAd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    if (isLoadingAd) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Loading Ad...")
                    } else {
                        Text(
                            text = "📺 Watch Ad & View Job",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Text(
                        text = "Go Back",
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}
