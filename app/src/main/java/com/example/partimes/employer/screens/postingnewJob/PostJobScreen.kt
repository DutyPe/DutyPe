package com.example.partimes.screens.employer.postingnewJob

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Work
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
import com.example.partimes.apis.postJobToBackend
import com.example.partimes.models.JobListing
import java.util.UUID

@Composable
fun PostJobScreen(navController: NavController, jobId: String?) {
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var specificLocation by remember { mutableStateOf("Fetching current location...") }
    var wage by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var preferences by remember { mutableStateOf("") }
    var vacancies by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var requiredSkills by remember { mutableStateOf("") }
    var fromTime by remember { mutableStateOf("") }
    var toTime by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Post a New Job",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Job Title") })
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company Name") })
                OutlinedTextField(value = specificLocation, onValueChange = {}, label = { Text("Location") }, enabled = false)
                OutlinedTextField(value = wage, onValueChange = { wage = it }, label = { Text("Wage") }, placeholder = { Text("e.g., ₹200/day") })

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = fromTime,
                        onValueChange = { fromTime = it },
                        label = { Text("From") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = toTime,
                        onValueChange = { toTime = it },
                        label = { Text("To") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") })
                OutlinedTextField(value = jobType, onValueChange = { jobType = it }, label = { Text("Job Type") })
                OutlinedTextField(value = requiredSkills, onValueChange = { requiredSkills = it }, label = { Text("Required Skills") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Job Description") }, maxLines = 4)
                OutlinedTextField(value = preferences, onValueChange = { preferences = it }, label = { Text("Preferences") })
                OutlinedTextField(
                    value = vacancies,
                    onValueChange = { vacancies = it },
                    label = { Text("Vacancies") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL (Optional)") })
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Contact Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        }

        Button(
            onClick = {
                val newJob = JobListing(
                    jobId = UUID.randomUUID().toString(),
                    employerId = "dummy-employer",
                    title = title,
                    company = company,
                    specificLocation = specificLocation,
                    locationNearby = "",
                    wage = wage,
                    timing = "$fromTime to $toTime",
                    description = description,
                    preferences = preferences.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    vacancies = vacancies.toIntOrNull() ?: 1,
                    isActive = true,
                    isTrending = false,
                    postedAt = System.currentTimeMillis(),
                    imageUrl = imageUrl,
                    phoneNumber = phoneNumber
                )
                postJobToBackend(newJob) {
                    navController.popBackStack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Submit Job", fontSize = 16.sp, color = Color.White)
        }
    }
}
