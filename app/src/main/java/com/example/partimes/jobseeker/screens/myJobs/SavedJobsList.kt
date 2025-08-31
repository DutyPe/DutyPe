package com.example.partimes.jobseeker.screens.myJobs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.partimes.components.CompactJobCard
import com.example.partimes.data.dummy.jobListings
import com.example.partimes.models.toSummary

@Composable
fun SavedJobsList() {
    listOf(
        "Delivery Boy - Cafe Rio",
        "Waiter - Food Hub",
        "Receptionist - Cozy Hostel"
    )

    LazyColumn(modifier = Modifier.fillMaxSize()
    )
    {
        items(jobListings.size) { index ->
            val job = jobListings[index]
            CompactJobCard(
                job = job.toSummary(), // Convert JobListing to JobSummary
                onClick = { selectedJobSummary ->
                    // You can navigate using selectedJobSummary.jobId or title
                    // Example:
                    // navController.navigate("jobDetails/${selectedJobSummary.jobId}")
                }
            )

        }

    }
}
