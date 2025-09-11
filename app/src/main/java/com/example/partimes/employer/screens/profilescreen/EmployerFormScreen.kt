package com.example.partimes.screens.employer.profilescreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.partimes.employer.viewmodels.EmployerViewModel

@OptIn(ExperimentalMaterial3Api::class)
//@file:OptIn(ExperimentalMaterial3Api::class)
//package com.example.partimes.screens.employer.profilescreen
@Composable
fun EmployerFormScreen(
    navController: NavController,
    employerViewModel: EmployerViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var yearsOfExperience by remember { mutableStateOf("") }
    var companySize by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var linkedInProfile by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var newSkill by remember { mutableStateOf("") }
    var skillsList by remember { mutableStateOf(listOf<String>()) }

    val companySizeOptions = listOf("1-10", "11-50", "51-200", "201-500", "500+")
    val industryOptions = listOf("Technology", "Healthcare", "Finance", "Education", "Retail", "Manufacturing", "Other")
    
    var expandedCompanySize by remember { mutableStateOf(false) }
    var expandedIndustry by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 0.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Create Professional Profile", 
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Personal Information Section
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Personal Information",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Professional Information Section
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Professional Information",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = position,
                        onValueChange = { position = it },
                        label = { Text("Position/Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = yearsOfExperience,
                        onValueChange = { yearsOfExperience = it },
                        label = { Text("Years of Experience") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        suffix = { Text("years") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Company Size Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedCompanySize,
                        onExpandedChange = { expandedCompanySize = !expandedCompanySize },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = companySize,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Company Size") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCompanySize) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCompanySize,
                            onDismissRequest = { expandedCompanySize = false }
                        ) {
                            companySizeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        companySize = option
                                        expandedCompanySize = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Industry Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedIndustry,
                        onExpandedChange = { expandedIndustry = !expandedIndustry },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = industry,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Industry") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIndustry) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedIndustry,
                            onDismissRequest = { expandedIndustry = false }
                        ) {
                            industryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        industry = option
                                        expandedIndustry = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = linkedInProfile,
                        onValueChange = { linkedInProfile = it },
                        label = { Text("LinkedIn Profile (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Professional Skills Section
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Professional Skills",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newSkill,
                            onValueChange = { newSkill = it },
                            label = { Text("Add a skill") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newSkill.isNotBlank()) {
                                    skillsList = skillsList + newSkill
                                    newSkill = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Skill")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(skillsList) { skill ->
                            FilterChip(
                                selected = true,
                                onClick = { skillsList = skillsList - skill },
                                label = { Text(skill) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Remove $skill",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bio Section
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "About You",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Professional Bio") },
                        placeholder = { Text("Tell us about yourself and your professional experience...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    employerViewModel.updateEmployer(
                        name = name,
                        company = company,
                        email = email,
                        professionalSkills = skillsList,
                        yearsOfExperience = yearsOfExperience.toIntOrNull() ?: 0,
                        position = position,
                        companySize = companySize,
                        industry = industry,
                        bio = bio,
                        linkedInProfile = linkedInProfile,
                        phoneNumber = phoneNumber
                    )
                    navController.navigate("employer_profile") {
                        popUpTo("create_profile") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Create Professional Profile",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
