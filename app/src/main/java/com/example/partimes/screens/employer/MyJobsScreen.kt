package com.example.partimes.screens.employer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.partimes.models.JobListing

@Composable
fun MyJobsScreen(navController: NavController) {
    val postedJobs = listOf(
        JobListing(
            jobId = "1",
            employerId = "employer1",
            title = "Delivery Partner",
            company = "Zomato",
            specificLocation = "MG Road",
            locationNearby = "",
            wage = "₹300/day",
            timing = "9:00 AM - 6:00 PM",
            description = "Deliver food orders to customers promptly and courteously.",
            preferences = listOf("Bike Required", "Male"),
            vacancies = 5,
            isActive = true,
            isTrending = false,
            postedAt = System.currentTimeMillis(),
            imageUrl = "",
            phoneNumber = "9876543210"
        ),
        JobListing(
            jobId = "2",
            employerId = "employer1",
            title = "Retail Staff",
            company = "Reliance Trends",
            specificLocation = "Brigade Road",
            locationNearby = "",
            wage = "₹250/day",
            timing = "11:00 AM - 8:00 PM",
            description = "Assist customers in-store and manage inventory.",
            preferences = listOf("Female", "Age 18-25"),
            vacancies = 3,
            isActive = true,
            isTrending = true,
            postedAt = System.currentTimeMillis(),
            imageUrl = "",
            phoneNumber = "9876543210"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Posted Jobs",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(postedJobs) { job ->
                JobCard(job = job, onEditClick = {
                     navController.navigate("edit_job/${job.jobId}")
                }, onViewApplicantsClick = {
                    navController.navigate("view_applicants/${job.jobId}")
                })
            }
        }
    }
}

@Composable
fun JobCard(
    job: JobListing,
    onEditClick: () -> Unit,
    onViewApplicantsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = job.title, style = MaterialTheme.typography.titleLarge)
            Text(text = job.company, style = MaterialTheme.typography.titleMedium.copy(color = Color.Gray))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Wage: ${job.wage}")
            Text(text = "Time: ${job.timing}")
            Text(text = "Vacancies: ${job.vacancies}")
            Text(text = "Location: ${job.specificLocation}")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                TextButton(onClick = onViewApplicantsClick) {
                    Icon(Icons.Default.People, contentDescription = "Applicants")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Applicants")
                }
            }
        }
    }
}
