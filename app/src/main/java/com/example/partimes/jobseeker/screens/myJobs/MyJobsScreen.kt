package com.example.partimes.jobseeker.screens.myJobs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.unit.dp
import com.example.partimes.R
import com.example.partimes.models.ApplicationStatus
import com.example.partimes.models.AppliedJob
import com.example.partimes.models.JobListing

@Composable
fun MyJobsScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Applied Jobs","Saved Jobs", )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 3.dp, end = 3.dp, top = 3.dp)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {

            0 -> {
                // Dummy JobListing
                val dummyJobListing = JobListing(
                    jobId = "dummyJobId1", // Added
                    employerId = "dummyEmployerId1", // Added
                    title = "Retail Associate (Part-time)",
                    company = "Reliance Trends",
                    locationNearby = "Kukatpally",
                    specificLocation = "Forum Mall",
                    wage = "₹2500/weekend",
                    timing = "Weekends, 10 AM - 6 PM",
                    isTrending = true,
                    vacancies = 5,
                    imageUrl = R.drawable.delivery.toString(), // <-- make sure you have a dummy drawable!
                    description = "Assist customers, manage inventory, and ensure smooth billing operations.",
                    preferences = listOf("Good communication", "Punctuality"),
                    phoneNumber = "9390693988",
                    isActive = true, // Added
                    postedAt = System.currentTimeMillis() // Added
                )

                // Dummy AppliedJob
                val dummyAppliedJob = AppliedJob(
                    jobListing = dummyJobListing,
                    status = ApplicationStatus.SELECTED,
                    lastUpdated = System.currentTimeMillis() - 2 * 60 * 60 * 1000L, // 2 hours ago
                    employerMessage = "We will get back to you soon!"
                )

                // Show the card
                AppliedJobCard(
                    appliedJob = dummyAppliedJob,
                    onClick = {
                        // Handle click here (maybe navigate to AppliedJobDetailScreen)
                    }
                )
            }
            1-> SavedJobsList()
        }
    }
}
