package com.example.partimes.screens.employer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.partimes.apis.postJobToBackend
import com.example.partimes.models.JobListing
import java.util.UUID


@Composable
fun PostJobScreen(navController: NavController) {
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

    // TODO: In real implementation, use LocationUtils to fetch actual location and update this value
    // TODO: Later, replace phoneNumber with value from ViewModel once user profile is integrated

    /*
    // Example for future ViewModel use:
    val userViewModel: UserViewModel = viewModel()
    val phoneNumber by userViewModel.phoneNumber.collectAsState()
    */

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Post a New Job", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Job Title") }, placeholder = { Text("e.g., Delivery Executive") })
        OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") }, placeholder = { Text("e.g., Swiggy") })
        OutlinedTextField(value = specificLocation, onValueChange = {}, label = { Text("Location") }, enabled = false)

        OutlinedTextField(value = wage, onValueChange = { wage = it }, label = { Text("Wage") }, placeholder = { Text("e.g., ₹200/day") })

        OutlinedTextField(value = fromTime, onValueChange = { fromTime = it }, label = { Text("From Time") }, placeholder = { Text("e.g., 10:00 AM") })
        OutlinedTextField(value = toTime, onValueChange = { toTime = it }, label = { Text("To Time") }, placeholder = { Text("e.g., 6:00 PM") })

        OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") }, placeholder = { Text("e.g., 2 weeks") })
        OutlinedTextField(value = jobType, onValueChange = { jobType = it }, label = { Text("Job Type") }, placeholder = { Text("e.g., Part-time") })
        OutlinedTextField(value = requiredSkills, onValueChange = { requiredSkills = it }, label = { Text("Required Skills") }, placeholder = { Text("e.g., Communication, Punctuality") })

        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, placeholder = { Text("Detailed job role, requirements, etc.") })
        OutlinedTextField(value = preferences, onValueChange = { preferences = it }, label = { Text("Preferences") }, placeholder = { Text("e.g., Male, Age 18-25") })
        OutlinedTextField(
            value = vacancies,
            onValueChange = { vacancies = it },
            label = { Text("Vacancies") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("e.g., 5") }
        )
        OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") }, placeholder = { Text("Optional image link") })

        // ✅ Editable contact number (will be replaced later with dynamic value)
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Contact Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            placeholder = { Text("e.g., 9876543210") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = {
                val newJob = JobListing(
                    jobId = UUID.randomUUID().toString(),
                    employerId = "dummy-employer",
                    title = title,
                    company = company,
                    specificLocation = specificLocation,
                    locationNearby = "", // Removed
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
            }) {
                Text("Submit Job")
            }
        }
    }
}
