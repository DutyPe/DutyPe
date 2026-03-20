package com.example.dutype.employer.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.EmployerColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerCompanyDetailsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    
    // Current user ID
    var currentUserId by remember { mutableStateOf("") }
    
    // Form state
    var companyName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var companySize by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    
    // Image picker launcher with toast notification
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Timber.d("📸 EMPLOYER COMPANY DETAILS: Image picker result - uri: $uri")
        uri?.let { selectedUri ->
            profileImageUri = selectedUri
            isUploadingImage = true
            
            scope.launch {
                try {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    Timber.d("📸 EMPLOYER COMPANY DETAILS: Current user: ${currentUser?.uid}")
                    if (currentUser != null) {
                        Timber.d("📸 EMPLOYER COMPANY DETAILS: Starting upload...")
                        val uploadResult = profileCompletionViewModel.uploadProfileImage(selectedUri, currentUser.uid, "employer")
                        uploadResult.fold(
                            onSuccess = { imageUrl ->
                                profileImageUrl = imageUrl
                                Timber.i("📸 EMPLOYER COMPANY DETAILS: ✅ Profile image uploaded: $imageUrl")
                                
                                // Update profile data with new image URL
                                val updatedProfileData = mapOf(
                                    "profileImageUrl" to imageUrl
                                )
                                profileCompletionViewModel.saveEmployerProfileData(updatedProfileData)
                                
                                // Show success toast
                                Toast.makeText(context, "Company logo updated successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { exception ->
                                Timber.e(exception, "📸 EMPLOYER COMPANY DETAILS: ❌ Failed to upload profile image")
                                profileImageUri = null
                                Toast.makeText(context, "Failed to upload logo. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        Timber.w("📸 EMPLOYER COMPANY DETAILS: No current user - cannot upload")
                        profileImageUri = null
                        Toast.makeText(context, "Please login to upload logo", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "📸 EMPLOYER COMPANY DETAILS: ❌ Error uploading profile image")
                    profileImageUri = null
                    Toast.makeText(context, "Error uploading logo", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploadingImage = false
                }
            }
        }
    }
    
    // Editing state
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    // Rating state
    var employerRating by remember { mutableStateOf(0f) }
    var employerTotalRatings by remember { mutableStateOf(0) }
    var employerReviews by remember { mutableStateOf<List<com.example.dutype.services.Rating>>(emptyList()) }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var isReviewsLoading by remember { mutableStateOf(false) }
    val ratingService = remember {
        com.example.dutype.services.RatingService(
            com.google.firebase.firestore.FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        )
    }
    
    // Load existing profile data from Firebase
    LaunchedEffect(Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            try {
                val employerProfileData = profileCompletionViewModel.getEmployerProfileData(currentUser.uid)
                employerProfileData.fold(
                    onSuccess = { data ->
                        companyName = data["companyName"] as? String ?: ""
                        contactEmail = data["contactEmail"] as? String ?: ""
                        contactPhone = data["contactPhone"] as? String ?: ""
                        businessAddress = data["businessAddress"] as? String ?: ""
                        industry = data["industry"] as? String ?: ""
                        companySize = data["companySize"] as? String ?: ""
                        profileImageUrl = data["profileImageUrl"] as? String
                        
                        Timber.d("✅ Company Details - Loaded profile image URL: $profileImageUrl")
                    },
                    onFailure = { exception ->
                        Timber.e("❌ Error loading employer profile data: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Timber.e("❌ Error loading employer profile data: ${e.message}")
            }
        }
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
                        "phone" to contactPhone,
                        "profileImageUrl" to (profileImageUrl ?: "")
                    )
                    
                    // Use the same method as the profile screen
                    profileCompletionViewModel.saveEmployerProfileData(profileData)
                    
                    Timber.d("✅ Company Details Screen - Profile updated successfully in Firebase")
                    Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                    showSuccessMessage = true
                    isEditing = false
                }
            } catch (e: Exception) {
                Timber.e("❌ Error updating company profile: ${e.message}")
                Toast.makeText(context, "Failed to save profile", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            try {
                val employerDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("employer_profiles")
                    .document(currentUserId)
                    .get()
                    .await()
                employerRating = (employerDoc.getDouble("rating") ?: 0.0).toFloat()
                employerTotalRatings = (employerDoc.getLong("totalRatings") ?: 0L).toInt()
            } catch (e: Exception) {
                Timber.e(e, "Error loading employer rating summary")
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmployerColors.ScreenBackground)
    ) {
        // Common Header with edit/save action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CommonHeader(
                    title = "Company Details",
                    navController = navController
                )
            }
            
            // Edit/Save button
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
                        tint = Color(0xFF3B82F6)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Company Logo/Profile Image Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Company Logo",
                        style = AppTypography.sectionHeader.copy(
                            color = Color.Black
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Company Logo Image - Display actual image
                    Box(
                        modifier = Modifier
                            .size(120.dp)
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
                                com.example.dutype.components.OptimizedProfileImage(
                                    imageUrl = profileImageUri.toString(),
                                    contentDescription = "Company Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            profileImageUrl != null && profileImageUrl!!.isNotBlank() -> {
                                com.example.dutype.components.OptimizedProfileImage(
                                    imageUrl = profileImageUrl,
                                    contentDescription = "Company Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Add Logo",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                        
                        // Camera overlay
                        if (!isUploadingImage) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(36.dp)
                                    .background(Color(0xFF3B82F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isUploadingImage
                    ) {
                        Text(
                            text = if (profileImageUrl != null) "Change Logo" else "Upload Logo",
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Basic Information Section
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Company Details Section
            EditableProfileSection(
                title = "Company Details",
                isEditing = isEditing,
                items = listOf(
                    Triple("Industry", industry) { industry = it },
                    Triple("Company Size", companySize) { companySize = it }
                )
            )
            
            // Ratings & Reviews Section - Only show if user is logged in
            if (currentUserId.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            scope.launch {
                                isReviewsLoading = true
                                employerReviews = ratingService.getUserRatings(currentUserId, "EMPLOYER")
                                isReviewsLoading = false
                                showReviewsSheet = true
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ratings & Reviews",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = WorkerColors.TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (employerTotalRatings > 0) {
                                    "★ ${"%.1f".format(employerRating)}  •  $employerTotalRatings review${if (employerTotalRatings != 1) "s" else ""}"
                                } else {
                                    "No ratings yet"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary)
                            )
                        }

                        Text(
                            text = "View",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF3B82F6),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
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

    com.example.dutype.components.UserReviewsBottomSheet(
        isVisible = showReviewsSheet,
        title = "Employer Ratings & Reviews",
        averageRating = employerRating,
        totalRatings = employerTotalRatings,
        reviews = employerReviews,
        isLoading = isReviewsLoading,
        onDismiss = { showReviewsSheet = false }
    )
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
                    style = AppTypography.sectionHeader.copy(
                        color = Color.Black
                    )
                )
                
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF3B82F6)
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
                style = AppTypography.sectionHeader.copy(
                    color = Color.Black
                )
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isFetchingLocation by remember { mutableStateOf(false) }
    
    // Phone number should be read-only (from login)
    val isPhoneField = label == "Contact Phone"
    val isAddressField = label == "Business Address"
    
    Column {
        Text(
            text = label,
            style = AppTypography.labelMedium.copy(
                color = Color(0xFF6B7280)
            )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isPhoneField, // Phone number is read-only
                keyboardOptions = when (label) {
                    "Contact Email" -> KeyboardOptions(keyboardType = KeyboardType.Email)
                    "Contact Phone" -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                    else -> KeyboardOptions(keyboardType = KeyboardType.Text)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    disabledBorderColor = Color(0xFFE5E7EB),
                    disabledTextColor = Color(0xFF6B7280)
                ),
                trailingIcon = if (isAddressField && !isFetchingLocation) {
                    {
                        IconButton(
                            onClick = {
                                isFetchingLocation = true
                                scope.launch {
                                    try {
                                        val locationService = com.example.dutype.utils.LocationService(context)
                                        val locationInfo = locationService.getCurrentLocation()
                                        if (locationInfo != null) {
                                            onValueChange(locationInfo.getFullAddress())
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Unable to fetch location. Please check permissions.",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Error fetching location: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        isFetchingLocation = false
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Fetch current location",
                                tint = Color(0xFF3B82F6)
                            )
                        }
                    }
                } else if (isAddressField && isFetchingLocation) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF3B82F6)
                        )
                    }
                } else null
            )
            
            // Helper text for phone field
            if (isPhoneField) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Phone number cannot be changed (from login)",
                    style = AppTypography.labelSmall.copy(
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                )
            }
        } else {
            Text(
                text = value.ifEmpty { "Not provided" },
                style = AppTypography.bodyLarge.copy(
                    color = if (value.isEmpty()) Color(0xFF9CA3AF) else Color.Black
                )
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
            style = AppTypography.labelMedium.copy(
                color = Color(0xFF6B7280)
            )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = AppTypography.bodyLarge.copy(
                color = if (value == "Not provided") Color(0xFF9CA3AF) else Color.Black
            )
        )
    }
}
