package com.example.partimes.screens.jobseekers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileScreen(onContinue: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var selectedLocation by remember { mutableStateOf("") }
    val locationOptions = listOf("Mumbai", "Delhi", "Bangalore", "Chennai", "Kolkata")

    var selectedLanguage by remember { mutableStateOf("") }
    val languageOptions = listOf("Hindi", "English", "Marathi", "Tamil", "Telugu")

    var selectedSkill by remember { mutableStateOf("") }
    val skillOptions = listOf("Cooking", "Cleaning", "Driving", "Gardening", "Babysitting")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create Your Profile", fontSize = 22.sp, color = Color.Black)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = false,
            onExpandedChange = { },
        ) {
            OutlinedTextField(
                value = selectedLocation,
                onValueChange = {},
                readOnly = true,
                label = { Text("Location") },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            DropdownMenu(expanded = false, onDismissRequest = {}) {
                locationOptions.forEach { location ->
                    DropdownMenuItem(
                        text = { Text(location) },
                        onClick = { selectedLocation = location }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = false,
            onExpandedChange = { },
        ) {
            OutlinedTextField(
                value = selectedLanguage,
                onValueChange = {},
                readOnly = true,
                label = { Text("Preferred Language") },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            DropdownMenu(expanded = false, onDismissRequest = {}) {
                languageOptions.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang) },
                        onClick = { selectedLanguage = lang }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = false,
            onExpandedChange = { },
        ) {
            OutlinedTextField(
                value = selectedSkill,
                onValueChange = {},
                readOnly = true,
                label = { Text("Main Skill / Service") },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            DropdownMenu(expanded = false, onDismissRequest = {}) {
                skillOptions.forEach { skill ->
                    DropdownMenuItem(
                        text = { Text(skill) },
                        onClick = { selectedSkill = skill }
                    )
                }
            }
        }

        Button(
            onClick = {
                if (name.isNotEmpty() && phone.isNotEmpty() &&
                    selectedLocation.isNotEmpty() && selectedLanguage.isNotEmpty() && selectedSkill.isNotEmpty()
                ) {
                    onContinue()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}
