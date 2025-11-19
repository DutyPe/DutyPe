package com.example.dutype.worker.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.auth.AuthManager
import com.example.dutype.components.ProfilePictureUpload
import com.example.dutype.models.UserRole
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileDetailsScreen(
    navController: NavController,
    dataStore: ApplicationFormDataStore
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val profileCompletionService: ProfileCompletionService = remember { ProfileCompletionService() }
    
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
    var isUploadingImage by remember { mutableStateOf(false) }
    
    // Profile completion state
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    var isProfileCompleted by remember { mutableStateOf(false) }
    
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
        
        // Calculate completion percentage
        val completion = profileCompletionService.calculateWorkerProfileCompletion(
            fullName = fullName,
            email = email,
            phoneNumber = phoneNumber,
            address = address,
            dateOfBirth = dateOfBirth,
            gender = gender,
            skills = skills,
            experience = experience,
            profileImageUrl = profileImageUrl
        )
        profileCompletionPercentage = completion
        isProfileCompleted = completion >= 100
    }
    
    // Update completion when data changes
    LaunchedEffect(fullName, email, phoneNumber, address, dateOfBirth, gender, skills, experience, profileImageUrl) {
        val completion = profileCompletionService.calculateWorkerProfileCompletion(
            fullName = fullName,
            email = email,
            phoneNumber = phoneNumber,
            address = address,
            dateOfBirth = dateOfBirth,
            gender = gender,
            skills = skills,
            experience = experience,
            profileImageUrl = profileImageUrl
        )
        profileCompletionPercentage = completion
        isProfileCompleted = completion >= 100
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .statusBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", 
                                 tint = Color.Black, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Profile Details", style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 20.sp))
                    }
                }
            }
            
            // Profile Picture Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Profile Picture",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            ProfilePictureUpload(
                                currentImageUri = profileImageUrl,
                                onImageSelected = { imageUrl ->
                                    profileImageUrl = imageUrl
                                },
                                isUploading = isUploadingImage
                            )
                        }
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
                        onEditClick = {
                            // Navigate to edit screen or show edit dialog
                        }
                    )
                }
            }
            
            // Professional Information Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1400, 400)) + slideInVertically(tween(1400, 400))
                ) {
                    ProfileSection(
                        title = "Professional Information",
                        items = listOf(
                            "Skills" to skills,
                            "Experience" to experience
                        ),
                        onEditClick = {
                            // Navigate to edit screen or show edit dialog
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSection(
    title: String,
    items: List<Pair<String, String>>,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF6366F1)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            items.forEach { (label, value) ->
                ProfileField(
                    label = label,
                    value = value.ifEmpty { "Not provided" }
                )
                
                if (label != items.last().first) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = if (value == "Not provided") Color(0xFF9CA3AF) else Color.Black
        )
    }
}
