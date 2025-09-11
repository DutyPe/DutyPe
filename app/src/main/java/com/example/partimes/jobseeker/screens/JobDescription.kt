package com.example.partimes.screens.jobseekers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.partimes.jobseeker.api.getJobById
import com.example.partimes.models.JobListing
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDescriptionScreen(
    jobId: String?,
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    // Ensure status bar color is black for this screen
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.Black)
    }

    var job by remember { mutableStateOf<JobListing?>(null) }

    LaunchedEffect(jobId) {
        if (jobId != null) {
            getJobById(jobId) { fetchedJob ->
                job = fetchedJob
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(job?.title ?: "", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (job != null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                JobDetailRow(icon = Icons.Default.Business, label = "Company", value = job!!.company)
                JobDetailRow(icon = Icons.Default.LocationOn, label = "Location", value = "${job!!.specificLocation} (${job!!.locationNearby})")
                JobDetailRow(icon = Icons.Default.Work, label = "Timings", value = job!!.timing)
                JobDetailRow(icon = Icons.Default.Star, label = "Vacancies", value = "${job!!.vacancies}")
                JobDetailRow(icon = Icons.Default.Schedule, label = "Posted", value = DateFormat.getDateInstance().format(Date(job!!.postedAt)))

                Spacer(modifier = Modifier.height(24.dp))

                Text("Description", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(job!!.description)

                Spacer(modifier = Modifier.height(24.dp))

                Text("Preferences", style = MaterialTheme.typography.titleMedium)
                job!!.preferences.forEach {
                    BulletPoint(it)
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    BackHandler { navController.popBackStack() }
}


@Composable
fun JobDetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", modifier = Modifier.padding(end = 8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
