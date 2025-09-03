package com.example.partimes.jobseeker.screens.myJobs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.partimes.components.CompactJobCard
import com.example.partimes.data.dummy.jobListings
import com.example.partimes.models.JobListing
import com.example.partimes.models.toSummary

@Composable
fun SavedJobsList(
    searchQuery: String = "",
    onNavigateToJobDetails: (String) -> Unit = {}
) {
    // Mock saved job IDs - in a real app, this would come from a database or preferences
    val savedJobIds = remember {
        setOf("job1", "job2", "job4") // Some job IDs from the dummy data
    }

    // Filter saved jobs from the main job listings
    val savedJobs = remember(savedJobIds) {
        jobListings.filter { job ->
            savedJobIds.contains(job.jobId)
        }
    }

    // Apply search filter
    val filteredSavedJobs = remember(savedJobs, searchQuery) {
        if (searchQuery.isEmpty()) {
            savedJobs
        } else {
            savedJobs.filter { job ->
                job.title.contains(searchQuery, ignoreCase = true) ||
                job.company.contains(searchQuery, ignoreCase = true) ||
                job.locationNearby.contains(searchQuery, ignoreCase = true) ||
                job.specificLocation.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            savedJobs.isEmpty() -> {
                EmptySavedJobsState()
            }
            filteredSavedJobs.isEmpty() && searchQuery.isNotEmpty() -> {
                EmptySearchResults(searchQuery = searchQuery)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSavedJobs) { job ->
                        SavedJobCard(
                            job = job,
                            onClick = { job.jobId?.let { onNavigateToJobDetails(it) } },
                            onUnsave = {
                                // TODO: Implement unsave functionality
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedJobCard(
    job: JobListing,
    onClick: () -> Unit,
    onUnsave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with save button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Saved badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            Color(0xFF10B981).copy(alpha = 0.1f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Saved",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Unsave button
                IconButton(
                    onClick = onUnsave,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Remove from saved",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Use the existing CompactJobCard for consistency
            CompactJobCard(
                job = job.toSummary(),
                onClick = { onClick() },
//                modifier = Modifier.padding(0.dp) // Remove extra padding since it's already in the outer card
            )

            // Additional saved job info
            if (job.isTrending) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "This is a trending job in your area",
                        fontSize = 12.sp,
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySavedJobsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF9CA3AF)
            )
            Text(
                text = "No Saved Jobs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
            Text(
                text = "Save jobs you're interested in to see them here",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { /* TODO: Navigate to job search */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Jobs")
            }
        }
    }
}

@Composable
private fun EmptySearchResults(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF9CA3AF)
            )
            Text(
                text = "No saved jobs found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
            Text(
                text = "No saved jobs match \"$searchQuery\"",
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
        }
    }
}
