package com.example.dutype.common.employer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dutype.viewmodels.FirestoreEmployerJobViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dutype.models.JobListing
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController) {
    val jobViewModel: FirestoreEmployerJobViewModel = hiltViewModel()
    val uiState by jobViewModel.uiState.collectAsStateWithLifecycle()
    val jobs = uiState.myJobs
    val activeJobs = jobs.count { it.isActive }
    val pausedJobs = jobs.count { !it.isActive }
    val totalApplications = jobs.sumOf { it.applicationCount.toInt() }
    val todayJobs = jobs.count { isToday(it.postedAt) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    AnalyticsStatCard("Active", activeJobs.toString(), Icons.Default.Work, Color(0xFF1976D2), Modifier.weight(1f))
                    AnalyticsStatCard("Paused", pausedJobs.toString(), Icons.Default.Pause, Color(0xFFF59E0B), Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    AnalyticsStatCard("Applications", totalApplications.toString(), Icons.Default.PersonAdd, Color(0xFF388E3C), Modifier.weight(1f))
                    AnalyticsStatCard("Today's Posts", todayJobs.toString(), Icons.Default.CalendarToday, Color(0xFFF57C00), Modifier.weight(1f))
                }
            }
            if (pausedJobs > 0) {
                item {
                    Text(
                        text = "Paused Jobs (${pausedJobs})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                items(jobs.filter { !it.isActive }) { job ->
                    PausedJobRow(job)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = color))
            Text(title, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)))
        }
    }
}

@Composable
private fun PausedJobRow(job: JobListing) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(job.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
            Text(job.location, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)))
        }
    }
}

private fun isToday(timestamp: Long): Boolean {
    val today = java.util.Calendar.getInstance()
    val date = java.util.Calendar.getInstance()
    date.timeInMillis = timestamp
    return today.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR) &&
            today.get(java.util.Calendar.DAY_OF_YEAR) == date.get(java.util.Calendar.DAY_OF_YEAR)
}
