package com.example.dutype.worker.components

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobListingSummary
import com.example.dutype.viewmodels.AdViewModel
import timber.log.Timber

/**
 * AdAwareJobCard - Wrapper around JobCard that handles rewarded ad logic
 * 
 * Shows a rewarded ad before navigating to job details (if not viewed today).
 * This centralizes ad logic in one place instead of duplicating in every screen.
 * 
 * Usage: Replace JobCard with AdAwareJobCard in all worker screens
 * 
 * @param job The job listing to display
 * @param onNavigateToJob Called after ad completes (or skips) with jobId - navigate to job details
 * @param onSaveClick Called when save button is clicked
 * @param modifier Modifier for the card
 * @param isSaved Whether the job is saved
 */
@Composable
fun AdAwareJobCard(
    job: JobListing,
    onNavigateToJob: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = job.isSaved
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adViewModel: AdViewModel = hiltViewModel()
    
    var isShowingAd by remember { mutableStateOf(false) }
    
    JobCard(
        job = job,
        onSaveClick = onSaveClick,
        onCardClick = { jobId ->
            handleJobCardClick(
                context = context,
                activity = activity,
                adViewModel = adViewModel,
                jobId = jobId,
                isShowingAd = isShowingAd,
                setIsShowingAd = { isShowingAd = it },
                onNavigateToJob = onNavigateToJob
            )
        },
        modifier = modifier,
        isSaved = isSaved,
        onViewTrack = { /* View tracking handled internally */ }
    )
}

/**
 * AdAwareJobCard for JobListingSummary (lightweight version)
 */
@Composable
fun AdAwareJobCard(
    job: JobListingSummary,
    onNavigateToJob: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = job.isSaved
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adViewModel: AdViewModel = hiltViewModel()
    
    var isShowingAd by remember { mutableStateOf(false) }
    
    JobCard(
        job = job,
        onSaveClick = onSaveClick,
        onCardClick = { jobId ->
            handleJobCardClick(
                context = context,
                activity = activity,
                adViewModel = adViewModel,
                jobId = jobId,
                isShowingAd = isShowingAd,
                setIsShowingAd = { isShowingAd = it },
                onNavigateToJob = onNavigateToJob
            )
        },
        modifier = modifier,
        isSaved = isSaved,
        onViewTrack = { /* View tracking handled internally */ }
    )
}

/**
 * Shared logic for handling job card click with ad
 */
private fun handleJobCardClick(
    context: android.content.Context,
    activity: Activity?,
    adViewModel: AdViewModel,
    jobId: String,
    isShowingAd: Boolean,
    setIsShowingAd: (Boolean) -> Unit,
    onNavigateToJob: (String) -> Unit
) {
    if (activity == null || isShowingAd) {
        // No activity or already showing ad - navigate directly
        onNavigateToJob(jobId)
        return
    }
    
    // Check if already viewed today (no ad needed)
    if (adViewModel.hasViewedJobToday(context, jobId)) {
        Timber.d("📺 Job $jobId already viewed today, skipping ad")
        onNavigateToJob(jobId)
        return
    }
    
    // Show rewarded ad
    setIsShowingAd(true)
    Timber.d("📺 Showing rewarded ad for job $jobId")
    
    adViewModel.showWorkerRewardedAdForJob(
        context = context,
        jobId = jobId,
        activity = activity,
        onCanView = {
            setIsShowingAd(false)
            Timber.d("📺 Ad completed, navigating to job $jobId")
            onNavigateToJob(jobId)
        },
        onAdNotReady = {
            setIsShowingAd(false)
            Timber.d("📺 Ad not ready, navigating to job $jobId anyway")
            onNavigateToJob(jobId)
        }
    )
}
