package com.example.dutype.worker.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dutype.components.CommonHeader
import timber.log.Timber
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileDetailsScreen(
    navController: NavController,
    dataStore: ApplicationFormDataStore
) {
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    
    // Form state
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    
    // Edit mode state
    var isEditMode by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Timber.d("📸 WORKER PROFILE DETAILS: Image picker result - uri: $uri")
        uri?.let { selectedUri ->
            profileImageUri = selectedUri
            isUploadingImage = true
            
            scope.launch {
                try {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    Timber.d("📸 WORKER PROFILE DETAILS: Current user: ${currentUser?.uid}")
                    if (currentUser != null) {
                        Timber.d("📸 WORKER PROFILE DETAILS: Starting upload...")
                        val uploadResult = profileCompletionViewModel.uploadProfileImage(selectedUri, currentUser.uid, "worker")
                        uploadResult.fold(
                            onSuccess = { imageUrl ->
                                profileImageUrl = imageUrl
                                Timber.i("📸 WORKER PROFILE DETAILS: ✅ Profile image uploaded: $imageUrl")
                            },
                            onFailure = { exception ->
                                Timber.e(exception, "📸 WORKER PROFILE DETAILS: ❌ Failed to upload profile image")
                                profileImageUri = null
                            }
                        )
                    } else {
                        Timber.w("📸 WORKER PROFILE DETAILS: No current user - cannot upload")
                        profileImageUri = null
                    }
                } catch (e: Exception) {
                    Timber.e(e, "📸 WORKER PROFILE DETAILS: ❌ Error uploading profile image")
                    profileImageUri = null
                } finally {
                    isUploadingImage = false
                }
            }
        }
    }
    
    // Load existing profile data
    LaunchedEffect(Unit) {
        val personalInfo = dataStore.getPersonalInfo()
        val experienceList = dataStore.getExperience()
        val skillsList = dataStore.getSkills()
        
        fullName = personalInfo.fullName
        email = personalInfo.email
        phoneNumber = personalInfo.phone
        address = personalInfo.address
        dateOfBirth = personalInfo.dateOfBirth
        gender = personalInfo.gender
        skills = skillsList.joinToString(", ")
        experience = experienceList.joinToString(", ")
        
        // Load profile data from Firebase (including profile image)
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val workerProfileData = profileCompletionViewModel.getWorkerProfileData(currentUser.uid)
                workerProfileData.fold(
                    onSuccess = { data ->
                        fullName = data["fullName"] as? String ?: fullName
                        email = data["email"] as? String ?: email
                        phoneNumber = data["phone"] as? String ?: phoneNumber
                        address = data["address"] as? String ?: address
                        dateOfBirth = data["dateOfBirth"] as? String ?: dateOfBirth
                        gender = data["gender"] as? String ?: gender
                        profileImageUrl = data["profileImageUrl"] as? String
                        
                        val firebaseSkills = data["skills"]
                        if (firebaseSkills is List<*>) {
                            skills = firebaseSkills.filterIsInstance<String>().joinToString(", ")
                        }
                        
                        Timber.d("📸 WORKER PROFILE DETAILS: Loaded profile image URL: $profileImageUrl")
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Error loading worker profile data")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading worker profile data")
            }
        }
    }
    
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CommonHeader(title = "Profile Details", navController = navController)
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Profile Picture Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Profile Image Display - Centered, larger, professional
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3F4F6))
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isUploadingImage -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(40.dp),
                                            color = Color(0xFF3B82F6),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                    profileImageUri != null -> {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                .data(profileImageUri)
                                                .crossfade(true)
                                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                                .build(),
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    profileImageUrl != null && profileImageUrl!!.isNotBlank() -> {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                .data(profileImageUrl)
                                                .crossfade(true)
                                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                                .build(),
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(60.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Name display
                            if (fullName.isNotBlank()) {
                                Text(
                                    text = fullName,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Tap to change text
                            Text(
                                text = "Tap photo to change",
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }

                
                // Personal Information Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(1200, 300)) + slideInVertically(tween(1200, 300))
                    ) {
                        if (isEditMode) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("Personal Information", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    EditableProfileField("Full Name", fullName) { fullName = it }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    EditableProfileField("Email", email) { email = it }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    EditableProfileField("Phone Number", phoneNumber) { phoneNumber = it }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    EditableProfileField("Address", address) { address = it }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    EditableProfileField("Date of Birth", dateOfBirth) { dateOfBirth = it }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    EditableProfileField("Gender", gender) { gender = it }
                                }
                            }
                        } else {
                            ProfileSection(
                                title = "Personal Information",
                                items = listOf(
                                    "Full Name" to fullName,
                                    "Email" to email,
                                    "Phone Number" to phoneNumber,
                                    "Address" to address,
                                    "Date of Birth" to dateOfBirth,
                                    "Gender" to gender
                                ),
                                onEditClick = { isEditMode = true }
                            )
                        }
                    }
                }
                
                // Professional Information Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(1400, 400)) + slideInVertically(tween(1400, 400))
                    ) {
                        if (isEditMode) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("Professional Information", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    EditableProfileField("Skills", skills) { skills = it }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    EditableProfileField("Experience", experience) { experience = it }
                                }
                            }
                        } else {
                            ProfileSection(
                                title = "Professional Information",
                                items = listOf("Skills" to skills, "Experience" to experience),
                                onEditClick = { isEditMode = true }
                            )
                        }
                    }
                }
            }
            
            // Save/Cancel buttons when in edit mode
            if (isEditMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { isEditMode = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6))
                    ) {
                        Text("Cancel", color = Color.Black)
                    }
                    
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                try {
                                    val personalInfo = com.example.dutype.worker.models.PersonalInfo(
                                        fullName = fullName, email = email, phone = phoneNumber,
                                        address = address, dateOfBirth = dateOfBirth, gender = gender
                                    )
                                    dataStore.savePersonalInfo(personalInfo)
                                    dataStore.saveSkills(skills.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                                    
                                    val experienceList = experience.split("\n").filter { it.isNotEmpty() }.map { exp ->
                                        com.example.dutype.worker.models.WorkExperience(
                                            company = exp.trim(), position = exp.trim(), description = exp.trim()
                                        )
                                    }
                                    dataStore.saveExperience(experienceList)
                                    
                                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                    if (currentUser != null) {
                                        val workerProfileData = mapOf(
                                            "fullName" to fullName, "email" to email, "phone" to phoneNumber,
                                            "address" to address, "dateOfBirth" to dateOfBirth, "gender" to gender,
                                            "skills" to skills.split(",").map { it.trim() },
                                            "experience" to experienceList.map { mapOf("company" to it.company, "position" to it.position, "description" to it.description) },
                                            "updatedAt" to System.currentTimeMillis()
                                        )
                                        profileCompletionViewModel.saveWorkerProfileData(workerProfileData)
                                    }
                                    isEditMode = false
                                } catch (e: Exception) {
                                    Timber.e(e, "Error saving profile")
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Text(if (isSaving) "Saving..." else "Save", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(title: String, items: List<Pair<String, String>>, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF6366F1))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            items.forEach { (label, value) ->
                ProfileField(label = label, value = value.ifEmpty { "Not provided" })
                if (label != items.last().first) Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = if (value == "Not provided") Color(0xFF9CA3AF) else Color.Black)
    }
}

@Composable
fun EditableProfileField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = label != "Skills" && label != "Experience",
            minLines = if (label == "Skills" || label == "Experience") 3 else 1,
            keyboardOptions = when (label) {
                "Email" -> KeyboardOptions(keyboardType = KeyboardType.Email)
                "Phone Number" -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                else -> KeyboardOptions(keyboardType = KeyboardType.Text)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )
    }
}
