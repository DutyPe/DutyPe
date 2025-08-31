package com.example.partimes.screens.employer.editjob

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.models.JobListing
import com.example.partimes.apis.postJobToBackend

@Composable
fun EditJobScreen(navController: NavController, jobId: String?) {
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var specificLocation by remember { mutableStateOf("") }
    var wage by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var preferences by remember { mutableStateOf("") }
    var vacancies by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var fromTime by remember { mutableStateOf("") }
    var toTime by remember { mutableStateOf("") }

    // Load job data
    LaunchedEffect(jobId) {
        jobId?.let {
            val job = getDummyJobById(it)
            job?.let {
                title = it.title
                company = it.company
                specificLocation = it.specificLocation
                wage = it.wage
                description = it.description
                preferences = it.preferences.joinToString(", ")
                vacancies = it.vacancies.toString()
                imageUrl = it.imageUrl.toString()
                phoneNumber = it.phoneNumber
                val timeParts = it.timing.split(" to ")
                fromTime = timeParts.getOrNull(0) ?: ""
                toTime = timeParts.getOrNull(1) ?: ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Edit Job",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Job Title") })
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") })
                OutlinedTextField(value = specificLocation, onValueChange = {}, label = { Text("Location") }, enabled = false)
                OutlinedTextField(value = wage, onValueChange = { wage = it }, label = { Text("Wage") })

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = fromTime, onValueChange = { fromTime = it }, label = { Text("From") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = toTime, onValueChange = { toTime = it }, label = { Text("To") }, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = preferences, onValueChange = { preferences = it }, label = { Text("Preferences") })
                OutlinedTextField(value = vacancies, onValueChange = { vacancies = it }, label = { Text("Vacancies") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") })
                OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("Phone Number") })
            }
        }

        Button(
            onClick = {
                val updatedJob = JobListing(
                    jobId = jobId ?: "unknown",
                    employerId = "employer1",
                    title = title,
                    company = company,
                    specificLocation = specificLocation,
                    locationNearby = "",
                    wage = wage,
                    timing = "$fromTime to $toTime",
                    description = description,
                    preferences = preferences.split(",").map { it.trim() },
                    vacancies = vacancies.toIntOrNull() ?: 1,
                    isActive = true,
                    isTrending = false,
                    postedAt = System.currentTimeMillis(),
                    imageUrl = imageUrl,
                    phoneNumber = phoneNumber
                )

                postJobToBackend(updatedJob) {
                    navController.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Update Job", fontSize = 16.sp, color = Color.White)
        }
    }
}

fun getDummyJobById(jobId: String): JobListing? {
    val dummyJobs = listOf(
        JobListing(
            jobId = "1",
            employerId = "employer1",
            title = "Delivery Partner",
            company = "Zomato",
            specificLocation = "MG Road",
            locationNearby = "",
            wage = "₹300/day",
            timing = "9:00 AM to 6:00 PM",
            description = "Deliver food orders",
            preferences = listOf("Bike Required", "Male"),
            vacancies = 5,
            isActive = true,
            isTrending = false,
            postedAt = System.currentTimeMillis(),
            imageUrl = "",
            phoneNumber = "9876543210"
        )
    )
    return dummyJobs.find { it.jobId == jobId }
}
