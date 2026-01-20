package com.example.dutype.worker.components.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.worker.components.JobCard

/**
 * P2 PERFORMANCE FIX: Extracted HomeJobsSection composable
 * 
 * Reduces recomposition scope - only this component recomposes when
 * job data changes.
 * 
 * Uses regular JobCard - ad shows on back from JobDescriptionScreen.
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */
@Composable
fun HomeJobsSection(
    jobs: List<JobListing>,
    sectionTitle: String?,
    onViewAllClick: () -> Unit,
    navController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    onNavigateToJob: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header with View All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle ?: stringResource(R.string.jobs_fits_for_you),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    fontSize = 17.sp
                )
            )
            
            // View all button - clean minimal style
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clickable { onViewAllClick() }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.view_all),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Job Cards - Show only 3, using regular JobCard (ad shows on back from JobDescription)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            jobs.forEach { job ->
                val jobId = job.jobId.ifEmpty { job.id }
                JobCard(
                    job = job,
                    isSaved = job.isSaved,
                    onSaveClick = {
                        if (job.isSaved) {
                            savedJobsViewModel.unsaveJob(jobId)
                        } else {
                            savedJobsViewModel.saveJob(jobId)
                        }
                    },
                    onCardClick = { onNavigateToJob(it) }
                )
            }
        }
    }
}
