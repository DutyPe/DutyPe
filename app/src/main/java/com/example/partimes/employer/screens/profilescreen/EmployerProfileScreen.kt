package com.example.partimes.screens.employer.profilescreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.partimes.viewmodels.EmployerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerProfileScreen(
    navController: NavController,
    employerViewModel: EmployerViewModel = viewModel()
) {
    val context = LocalContext.current
    val employerState = employerViewModel.employer.collectAsState()

    var profileImageUri: Uri? by remember { mutableStateOf<Uri?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    // Sample fields
    var name by remember { mutableStateOf(employerState.value.name) }
    var company by remember { mutableStateOf(employerState.value.company) }
    var email by remember { mutableStateOf(employerState.value.email) }

    // Image picker launcher
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        profileImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Employer Profile") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(profileImageUri)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "Profile Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Icon",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Upload + Remove buttons
            Row {
                TextButton(onClick = { imageLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Upload, contentDescription = "Upload")
                    Spacer(Modifier.width(4.dp))
                    Text("Upload Image")
                }
                if (profileImageUri != null) {
                    TextButton(onClick = { profileImageUri = null }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(Modifier.width(4.dp))
                        Text("Remove Image")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Profile card
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditing) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { isEditing = false }) { Text("Cancel") }
                            Button(onClick = {
                                employerViewModel.updateEmployer(name, company, email)
                                isEditing = false
                            }) {
                                Text("Save")
                            }
                        }
                    } else {
                        ProfileField("Name", name)
                        Divider(Modifier.padding(vertical = 8.dp))
                        ProfileField("Company", company)
                        Divider(Modifier.padding(vertical = 8.dp))
                        ProfileField("Email", email)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { isEditing = true }, modifier = Modifier.align(Alignment.End)) {
                            Text("Edit Profile")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Gray))
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
