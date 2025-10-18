package com.example.dutype.employer.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.components.ProfilePictureUpload
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerCompanyDetailsScreen(
    navController: NavController
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val profileCompletionService: ProfileCompletionService = remember { ProfileCompletionService() }
    
    // Form state
    var companyName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var companySize by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    
    // Profile completion state
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    var isProfileCompleted by remember { mutableStateOf(false) }
    
    // Editing state
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    
    // Load existing profile data from Firebase
    LaunchedEffect(Unit) {
        try {
            val status = profileCompletionViewModel.getProfileSetupStatus(com.example.dutype.models.UserRole.EMPLOYER)
            if (status != null) {
                // Load saved profile data
                val savedEmail = profileCompletionViewModel.getUserEmail()
                val savedName = profileCompletionViewModel.getUserName()
                
                if (savedEmail != null) {
                    contactEmail = savedEmail
                }
                if (savedName != null) {
                    companyName = savedName
                }
                
                // Load additional profile data from Firestore
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    try {
                        val employerProfileData = profileCompletionViewModel.getEmployerProfileData(currentUser.uid)
                        employerProfileData.fold(
                            onSuccess = { data ->
                                // Update all profile fields with Firebase data
                                companyName = data["companyName"] as? String ?: companyName
                                contactEmail = data["contactEmail"] as? String ?: contactEmail
                                contactPhone = data["contactPhone"] as? String ?: ""
                                businessAddress = data["businessAddress"] as? String ?: ""
                                
                                // Additional fields that might be available
                                industry = data["industry"] as? String ?: ""
                                companySize = data["companySize"] as? String ?: ""
                                website = data["website"] as? String ?: ""
                                description = data["description"] as? String ?: ""
                                profileImageUrl = data["profileImageUrl"] as? String
                                
                                println("✅ Company Details Screen - Profile data loaded from Firebase:")
                                println("  Company Name: $companyName")
                                println("  Email: $contactEmail")
                                println("  Phone: $contactPhone")
                                println("  Address: $businessAddress")
                                println("  Industry: $industry")
                                println("  Company Size: $companySize")
                                println("  Website: $website")
                                println("  Description: $description")
                                println("  Profile Image: $profileImageUrl")
                            },
                            onFailure = { exception ->
                                println("❌ Error loading employer profile data in Company Details: ${exception.message}")
                            }
                        )
                    } catch (e: Exception) {
                        // Handle error loading additional profile data
                        println("❌ Error loading employer profile data in Company Details: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error - keep default values
            println("❌ Error in Company Details LaunchedEffect: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // Update completion when data changes
    LaunchedEffect(companyName, contactEmail, contactPhone, businessAddress, industry, companySize, website, description, profileImageUrl) {
        val completion = profileCompletionService.calculateEmployerProfileCompletion(
            companyName = companyName,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            businessAddress = businessAddress,
            industry = industry,
            companySize = companySize,
            website = website,
            description = description,
            profileImageUrl = profileImageUrl
        )
        profileCompletionPercentage = completion
        isProfileCompleted = completion >= 100
    }
    
    var isVisible by remember { mutableStateOf(false) }
    
    // Function to save profile data to Firebase
    fun saveProfileToFirebase() {
        scope.launch {
            isLoading = true
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val profileData = mapOf<String, Any>(
                        "companyName" to companyName,
                        "contactEmail" to contactEmail,
                        "contactPhone" to contactPhone,
                        "businessAddress" to businessAddress,
                        "industry" to industry,
                        "companySize" to companySize,
                        "website" to website,
                        "description" to description,
                        "profileImageUrl" to (profileImageUrl ?: ""),
                        "updatedAt" to System.currentTimeMillis()
                    )
                    
                    // Use the same method as the profile screen
                    profileCompletionViewModel.saveEmployerProfileData(profileData)
                    
                    println("✅ Company Details Screen - Profile updated successfully in Firebase")
                    showSuccessMessage = true
                    isEditing = false
                }
            } catch (e: Exception) {
                println("❌ Error updating company profile: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
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
                    TopAppBar(
                        title = {
                            Text(
                                text = "Company Details",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (isEditing) {
                                IconButton(
                                    onClick = { saveProfileToFirebase() },
                                    enabled = !isLoading
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Save,
                                            contentDescription = "Save",
                                            tint = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            } else {
                                IconButton(onClick = { isEditing = true }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color(0xFF6366F1)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = Color.Black
                        )
                    )
                }
            }
            
            // Company Logo Section
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
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Company Logo",
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
            
            // Basic Information Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1200, 300)) + slideInVertically(tween(1200, 300))
                ) {
                    EditableProfileSection(
                        title = "Basic Information",
                        isEditing = isEditing,
                        items = listOf(
                            Triple("Company Name", companyName) { companyName = it },
                            Triple("Contact Email", contactEmail) { contactEmail = it },
                            Triple("Contact Phone", contactPhone) { contactPhone = it },
                            Triple("Business Address", businessAddress) { businessAddress = it }
                        )
                    )
                }
            }
            
            // Company Details Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1400, 400)) + slideInVertically(tween(1400, 400))
                ) {
                    EditableProfileSection(
                        title = "Company Details",
                        isEditing = isEditing,
                        items = listOf(
                            Triple("Industry", industry) { industry = it },
                            Triple("Company Size", companySize) { companySize = it },
                            Triple("Website", website) { website = it },
                            Triple("Description", description) { description = it }
                        )
                    )
                }
            }
        }
    }
    
    // Success message
    if (showSuccessMessage) {
        LaunchedEffect(showSuccessMessage) {
            kotlinx.coroutines.delay(2000)
            showSuccessMessage = false
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Profile Updated Successfully!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
fun EditableProfileSection(
    title: String,
    isEditing: Boolean,
    items: List<Triple<String, String, (String) -> Unit>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            items.forEach { (label, value, onValueChange) ->
                EditableProfileField(
                    label = label,
                    value = value,
                    isEditing = isEditing,
                    onValueChange = onValueChange
                )
                
                if (label != items.last().first) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun EditableProfileField(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = when (label) {
                    "Contact Email" -> KeyboardOptions(keyboardType = KeyboardType.Email)
                    "Contact Phone" -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                    "Website" -> KeyboardOptions(keyboardType = KeyboardType.Uri)
                    else -> KeyboardOptions(keyboardType = KeyboardType.Text)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
        } else {
            Text(
                text = value.ifEmpty { "Not provided" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = if (value.isEmpty()) Color(0xFF9CA3AF) else Color.Black
            )
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
