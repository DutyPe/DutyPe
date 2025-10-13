package com.example.partimes.worker.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.partimes.R
import com.example.partimes.data.ApplicationFormDataStore
import com.example.partimes.worker.models.PersonalInfo
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.example.partimes.utils.ScrollStateManager
import com.example.partimes.navigation.Routes
import com.example.partimes.viewmodels.ProfileViewModel
import com.example.partimes.auth.AuthManager
import com.example.partimes.network.ApiClient
import com.example.partimes.viewmodels.ProfileCompletionViewModel
import com.example.partimes.components.ProfessionalLogoutDialog
import com.example.partimes.components.RoleSwitchSection
import com.example.partimes.auth.GoogleSignInManager
import com.example.partimes.models.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileScreen(
    rootNavController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null,
    dataStore: ApplicationFormDataStore
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val authManager = remember { AuthManager(context) }
    val googleSignInManager = remember { GoogleSignInManager(context) }
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profileUiState by profileViewModel.uiState.collectAsState()
    
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var isEmployerMode by remember { mutableStateOf(false) } // Added for switch functionality
    val scope = rememberCoroutineScope()

    // Get profile data from dataStore
    var personalInfo by remember { mutableStateOf(dataStore.getPersonalInfo()) }
    val experience = remember { dataStore.getExperience() }
    val skills = remember { dataStore.getSkills() }
    val coverLetter = remember { dataStore.getCoverLetter() }
    val isFormCompleted = remember { dataStore.isFormCompleted() }
    
    // Initialize ProfileViewModel and load profile data from backend
    LaunchedEffect(Unit) {
        val authManager = AuthManager(context)
        ApiClient.initialize(authManager)
        profileViewModel.loadProfile()
    }
    
    // Use backend profile data if available, otherwise fallback to dataStore
    val backendUser = profileUiState.user
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var profileSetupStatus by remember { mutableStateOf<com.example.partimes.state.ProfileSetupStatus?>(null) }
    var profileCompletion by remember { mutableStateOf(0) }
    
    // Load profile completion status from our new system
    LaunchedEffect(Unit) {
        try {
            val status = profileCompletionViewModel.getProfileSetupStatus(com.example.partimes.models.UserRole.WORKER)
            profileSetupStatus = status
            profileCompletion = status.completionPercentage
            
            // Load user info from ProfileSetupStateManager
            val savedEmail = profileCompletionViewModel.getUserEmail()
            val savedName = profileCompletionViewModel.getUserName()
            
            if (savedEmail != null) {
                userEmail = savedEmail
            }
            if (savedName != null) {
                userName = savedName
            }
            
            // Load additional profile data from Firestore
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                try {
                    val workerProfileData = profileCompletionViewModel.getWorkerProfileData(currentUser.uid)
                    workerProfileData?.let { data ->
                        // Update personal info with Firestore data
                        val updatedPersonalInfo = personalInfo.copy(
                            fullName = data["fullName"] as? String ?: personalInfo.fullName,
                            email = data["email"] as? String ?: personalInfo.email,
                            phone = data["phone"] as? String ?: personalInfo.phone,
                            address = data["address"] as? String ?: personalInfo.address,
                            dateOfBirth = data["dateOfBirth"] as? String ?: personalInfo.dateOfBirth,
                            gender = data["gender"] as? String ?: personalInfo.gender
                        )
                        
                        // Save the updated personal info back to DataStore
                        dataStore.savePersonalInfo(updatedPersonalInfo)
                        
                        // Also update the display variables
                        userName = updatedPersonalInfo.fullName
                        userEmail = updatedPersonalInfo.email
                        
                        // Load additional fields that might not be in PersonalInfo
                        val skills = data["skills"] as? String ?: ""
                        val experience = data["experience"] as? String ?: ""
                        
                        println("✅ Worker profile data loaded from Firebase:")
                        println("  Full Name: ${updatedPersonalInfo.fullName}")
                        println("  Email: ${updatedPersonalInfo.email}")
                        println("  Phone: ${updatedPersonalInfo.phone}")
                        println("  Address: ${updatedPersonalInfo.address}")
                        println("  Date of Birth: ${updatedPersonalInfo.dateOfBirth}")
                        println("  Gender: ${updatedPersonalInfo.gender}")
                        println("  Skills: $skills")
                        println("  Experience: $experience")
                    }
                } catch (e: Exception) {
                    // Handle error loading additional profile data
                    println("❌ Error loading worker profile data: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            // Fallback to dataStore
            profileCompletion = if (isFormCompleted) 100 else dataStore.getFormCompletionPercentage()
        }
    }
    
    // Update userName and userEmail when data changes
    LaunchedEffect(backendUser, personalInfo) {
        userName = when {
            backendUser?.fullName?.isNotBlank() == true -> backendUser.fullName
            personalInfo.fullName.isNotBlank() -> personalInfo.fullName
            else -> "User"
        }
        userEmail = when {
            backendUser?.email?.isNotBlank() == true -> backendUser.email
            personalInfo.email.isNotBlank() -> personalInfo.email
            else -> "user@example.com"
        }
    }

    // Status bar color
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        delay(200)
        isVisible = true
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            profileImageUri = uri
        }

    // Clean white background like Instagram
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // Instagram-style Profile Header
            item {
                InstagramStyleProfileHeader(
                    profileImageUri = profileImageUri,
                    userName = userName,
                    userEmail = userEmail,
                    profileCompletion = profileCompletion,
                    onImageClick = { imagePickerLauncher.launch("image/*") },
                    onEditClick = { showEditDialog = true },
                    isVisible = isVisible
                )
            }

            // Profile Completion Progress Bar
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(800, 300)) + slideInVertically(tween(800, 300))
                ) {
                    ProfileCompletionProgress(profileCompletion, dataStore)
                }
            }

            // Application Form Data Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
                ) {
                    ApplicationFormDataSection(dataStore, backendUser)
                }
            }

            // Settings Menu (Flat Design)
            item {
                Spacer(modifier = Modifier.height(24.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
                ) {
                    FlatSettingsMenu(
                        rootNavController = rootNavController,
                        onLogoutClick = { showLogoutDialog = true },
                        profileCompletionViewModel = profileCompletionViewModel,
                        scope = scope,
                        isVisible = isVisible
                    )
                }
            }
        }
    }

    // Dialogs
    if (showEditDialog) {
        ModernEditDialog(
            userName = userName,
            userEmail = userEmail,
            personalInfo = personalInfo,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail, updatedPersonalInfo ->
                scope.launch {
                    try {
                        // Update local variables
                        userName = newName
                        userEmail = newEmail
                        personalInfo = updatedPersonalInfo
                        
                        // Save to DataStore
                        dataStore.savePersonalInfo(updatedPersonalInfo)
                        
                        // Save to Firebase
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val workerProfileData = mapOf(
                                "fullName" to newName,
                                "email" to newEmail,
                                "phone" to updatedPersonalInfo.phone,
                                "address" to updatedPersonalInfo.address,
                                "dateOfBirth" to updatedPersonalInfo.dateOfBirth,
                                "gender" to updatedPersonalInfo.gender,
                                "skills" to skills,
                                "experience" to experience,
                                "updatedAt" to System.currentTimeMillis()
                            )
                            
                            profileCompletionViewModel.saveWorkerProfileData(currentUser.uid, workerProfileData)
                            println("✅ Worker profile updated successfully in Firebase")
                            
                            // Refresh the profile data from Firebase to show updated values
                            try {
                                val refreshedData = profileCompletionViewModel.getWorkerProfileData(currentUser.uid)
                                refreshedData?.let { data ->
                                    val refreshedPersonalInfo = personalInfo.copy(
                                        fullName = data["fullName"] as? String ?: personalInfo.fullName,
                                        email = data["email"] as? String ?: personalInfo.email,
                                        phone = data["phone"] as? String ?: personalInfo.phone,
                                        address = data["address"] as? String ?: personalInfo.address,
                                        dateOfBirth = data["dateOfBirth"] as? String ?: personalInfo.dateOfBirth,
                                        gender = data["gender"] as? String ?: personalInfo.gender
                                    )
                                    
                                    // Update local state with refreshed data
                                    personalInfo = refreshedPersonalInfo
                                    userName = refreshedPersonalInfo.fullName
                                    userEmail = refreshedPersonalInfo.email
                                    
                                    println("✅ Worker profile refreshed with updated data")
                                }
                            } catch (refreshError: Exception) {
                                println("⚠️ Could not refresh profile data: ${refreshError.message}")
                            }
                        }
                        
                        showEditDialog = false
                    } catch (e: Exception) {
                        println("❌ Error updating worker profile: ${e.message}")
                        // Still close dialog even if Firebase save fails
                        showEditDialog = false
                    }
                }
            }
        )
    }

    if (showLogoutDialog) {
        ProfessionalLogoutDialog(
            isVisible = showLogoutDialog,
            onDismiss = { showLogoutDialog = false },
            navController = rootNavController,
            userRole = "Worker",
            authManager = authManager,
            googleSignInManager = googleSignInManager,
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }
}

@Composable
private fun RoleSwitchSection(
    isEmployerMode: Boolean,
    onRoleSwitch: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(0xFFDC2626).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Switch Mode",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                    )
                    Text(
                        text = if (isEmployerMode) "Currently: Employer" else "Currently: Worker",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF666666)
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Worker",
                    tint = if (!isEmployerMode) Color(0xFF3B82F6) else Color(0xFFCCCCCC),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isEmployerMode,
                    onCheckedChange = onRoleSwitch,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF3B82F6),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCCCCCC)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = "Employer",
                    tint = if (isEmployerMode) Color(0xFF3B82F6) else Color(0xFFCCCCCC),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun InstagramStyleProfileHeader(
    profileImageUri: Uri?,
    userName: String,
    userEmail: String,
    profileCompletion: Int,
    onImageClick: () -> Unit,
    onEditClick: () -> Unit,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Top row: Profile picture + Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture (Instagram style)
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Profile image
                    Image(
                        painter = if (profileImageUri != null)
                            rememberAsyncImagePainter(profileImageUri)
                        else
                            painterResource(id = R.drawable.user),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .clickable { onImageClick() }
                    )

                    // Camera icon overlay (small)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(27.dp)
                            .background(Color(0xFF3B82F6), CircleShape)
                            .clickable { onImageClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Stats Row (Instagram style)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        number = "24",
                        label = "Applications"
                    )
                    StatItem(
                        number = "8",
                        label = "Interviews"
                    )
                    StatItem(
                        number = "3",
                        label = "Offers"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Info Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Username
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Email
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        )
                    )
                }
                
                // Edit Icon
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationFormDataSection(
    dataStore: ApplicationFormDataStore,
    backendUser: com.example.partimes.models.User? = null
) {
    val personalInfo = remember { dataStore.getPersonalInfo() }
    val experience = remember { dataStore.getExperience() }
    val skills = remember { dataStore.getSkills() }
    val coverLetter = remember { dataStore.getCoverLetter() }
    val isFormCompleted = remember { dataStore.isFormCompleted() }
    
    if (isFormCompleted || personalInfo.fullName.isNotBlank() || backendUser != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Section Header
            Text(
                text = "Profile Information",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Personal Information Card - Use backend data if available, otherwise dataStore
            if (backendUser?.fullName?.isNotBlank() == true || personalInfo.fullName.isNotBlank()) {
                ApplicationDataCard(
                    title = "Personal Information",
                    icon = Icons.Default.Person,
                    items = listOf(
                        "Name" to (backendUser?.fullName ?: personalInfo.fullName),
                        "Email" to (backendUser?.email ?: personalInfo.email),
                        "Phone" to (backendUser?.phoneNumber ?: personalInfo.phone),
                        "Address" to (backendUser?.location ?: personalInfo.address),
                        "Date of Birth" to (backendUser?.dateOfBirth ?: personalInfo.dateOfBirth),
                        "Gender" to (backendUser?.gender ?: personalInfo.gender)
                    ).filter { it.second.isNotBlank() }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Experience Card
            if (experience.isNotEmpty()) {
                ApplicationDataCard(
                    title = "Work Experience",
                    icon = Icons.Default.Work,
                    items = experience.mapIndexed { index, exp ->
                        "Experience ${index + 1}" to "${exp.position} at ${exp.company}"
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Skills Card - Use backend data if available, otherwise dataStore
            val displaySkills = backendUser?.skills ?: skills
            if (displaySkills.isNotEmpty()) {
                ApplicationDataCard(
                    title = "Skills",
                    icon = Icons.Default.Star,
                    items = listOf("Skills" to displaySkills.joinToString(", "))
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Cover Letter Card - Use backend data if available, otherwise dataStore
            val displayCoverLetter = backendUser?.coverLetter ?: coverLetter
            if (displayCoverLetter.isNotBlank()) {
                ApplicationDataCard(
                    title = "Cover Letter",
                    icon = Icons.Default.Description,
                    items = listOf("Cover Letter" to displayCoverLetter.take(100) + if (displayCoverLetter.length > 100) "..." else "")
                )
            }
        }
    }
}

@Composable
private fun ApplicationDataCard(
    title: String,
    icon: ImageVector,
    items: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Card Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Card Items
        items.forEach { (label, value) ->
            if (value.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$label:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280)
                        ),
                        modifier = Modifier.width(80.dp)
                    )
                    
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF374151)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (items.indexOf(label to value) < items.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    number: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF6B7280)
            )
        )
    }
}

@Composable
private fun ProfileCompletionProgress(
    profileCompletion: Int,
    dataStore: ApplicationFormDataStore
) {
    val applicationFormCompletion = remember { dataStore.getFormCompletionPercentage() }
    val isApplicationFormCompleted = remember { dataStore.isFormCompleted() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Progress bar header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile Completion",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
            )
            Text(
                text = "${profileCompletion}%",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6)
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    Color(0xFFE5E7EB),
                    RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(profileCompletion / 100f)
                    .background(
                        Color(0xFF3B82F6),
                        RoundedCornerShape(4.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress description
        Text(
            text = when {
                profileCompletion < 30 -> "Complete your basic information to get started"
                profileCompletion < 60 -> "Add more details to improve your profile visibility"
                profileCompletion < 90 -> "Almost there! Complete a few more sections"
                else -> "Excellent! Your profile is well-completed"
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF6B7280)
            )
        )
    }
}


@Composable
private fun FlatSettingsMenu(
    rootNavController: NavController,
    onLogoutClick: () -> Unit,
    profileCompletionViewModel: ProfileCompletionViewModel,
    scope: CoroutineScope,
    isVisible: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Profile Management Section
        Text(
            text = "Profile Management",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            FlatMenuItem(
                icon = Icons.Outlined.LocationOn,
                title = "Location & Availability",
                subtitle = "Update work location preferences",
                onClick = { rootNavController.navigate(Routes.MANUAL_LOCATION_ROUTE) },
                iconColor = Color(0xFF3B82F6) // Blue for location
            )

            FlatMenuItem(
                icon = Icons.Outlined.Person,
                title = "Complete Profile",
                subtitle = "Add professional details",
                onClick = { rootNavController.navigate(Routes.ADVANCED_PROFILE) },
                iconColor = Color(0xFF8B5CF6) // Purple for profile
            )

            FlatMenuItem(
                icon = Icons.Outlined.Settings,
                title = "Work Preferences",
                subtitle = "Set job preferences & filters",
                onClick = { rootNavController.navigate(Routes.WORK_PREFERENCES) },
                iconColor = Color(0xFF059669) // Green for settings
            )

            FlatMenuItem(
                icon = Icons.Outlined.Psychology,
                title = "Skills & Experience",
                subtitle = "Showcase your expertise",
                onClick = { rootNavController.navigate(Routes.SKILLS_MANAGEMENT) },
                iconColor = Color(0xFFF59E0B) // Orange for skills
            )

        }

        Spacer(modifier = Modifier.height(24.dp))

        // Support & Information Section
        Text(
            text = "Support & Information",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            FlatMenuItem(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                subtitle = "Manage your alerts",
                onClick = { rootNavController.navigate(Routes.NOTIFICATION_CENTER) },
                iconColor = Color(0xFFEC4899) // Pink for notifications
            )

            FlatMenuItem(
                icon = Icons.AutoMirrored.Outlined.Help,
                title = "Help & Support",
                subtitle = "Get assistance when needed",
                onClick = { rootNavController.navigate(Routes.HELP) },
                iconColor = Color(0xFF10B981) // Teal for help
            )

            FlatMenuItem(
                icon = Icons.Outlined.Info,
                title = "About ParTimes",
                subtitle = "Learn more about us",
                onClick = { rootNavController.navigate(Routes.ABOUT_US) },
                iconColor = Color(0xFF6B7280) // Gray for info
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Role Switch Section - Above logout button
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600))
        ) {
            RoleSwitchSection(
                currentRole = UserRole.WORKER,
                onRoleSwitch = { newRole ->
                    println("🔄 Worker Profile - Role switch triggered: $newRole")
                    when (newRole) {
                        UserRole.EMPLOYER -> {
                            println("🔄 Worker Profile - Switching to EMPLOYER")
                            // Update user role in local storage first
                            scope.launch {
                                try {
                                    println("🔄 Worker Profile - Updating user role to EMPLOYER")
                                    profileCompletionViewModel.updateUserRole(UserRole.EMPLOYER)
                                    // Small delay to ensure role is saved
                                    delay(500)
                                    println("🔄 Worker Profile - Navigating to EMPLOYER_HOME")
                                    // Switch to employer mode
                                    rootNavController.navigate(Routes.EMPLOYER_HOME) {
                                        popUpTo(Routes.WORKER_HOME) { inclusive = true }
                                    }
                                    println("🔄 Worker Profile - Navigation completed")
                                } catch (e: Exception) {
                                    // Handle error gracefully
                                    println("❌ Error switching to employer role: ${e.message}")
                                }
                            }
                        }
                        else -> {
                            println("🔄 Worker Profile - Invalid role switch: $newRole")
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Section
        FlatMenuItem(
            icon = Icons.AutoMirrored.Outlined.ExitToApp,
            title = "Log Out",
            subtitle = "Sign out of your account",
            onClick = onLogoutClick,
            isDestructive = true
        )
    }
}

@Composable
private fun FlatMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    iconColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor ?: if (isDestructive) Color(0xFFDC2626) else Color(0xFF059669),
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) Color(0xFFDC2626) else Color(0xFF1F2937)
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF059669),
            modifier = Modifier.size(28.dp)
        )
    }
}


@Composable
private fun ModernEditDialog(
    userName: String,
    userEmail: String,
    personalInfo: PersonalInfo,
    onDismiss: () -> Unit,
    onSave: (String, String, PersonalInfo) -> Unit
) {
    var newName by remember { mutableStateOf(userName) }
    var newEmail by remember { mutableStateOf(userEmail) }
    var newPhone by remember { mutableStateOf(personalInfo.phone) }
    var newAddress by remember { mutableStateOf(personalInfo.address) }
    var newDateOfBirth by remember { mutableStateOf(personalInfo.dateOfBirth) }
    var newGender by remember { mutableStateOf(personalInfo.gender) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { /* Email cannot be changed */ },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color(0xFF666666),
                        disabledBorderColor = Color(0xFFE0E0E0),
                        disabledLabelColor = Color(0xFF999999)
                    )
                )
                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newAddress,
                    onValueChange = { newAddress = it },
                    label = { Text("Address") },
                    leadingIcon = {
                        Icon(Icons.Default.Home, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newDateOfBirth,
                    onValueChange = { newDateOfBirth = it },
                    label = { Text("Date of Birth") },
                    placeholder = { Text("DD/MM/YYYY") },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newGender,
                    onValueChange = { newGender = it },
                    label = { Text("Gender") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val updatedPersonalInfo = personalInfo.copy(
                        fullName = newName,
                        email = newEmail,
                        phone = newPhone,
                        address = newAddress,
                        dateOfBirth = newDateOfBirth,
                        gender = newGender
                    )
                    onSave(newName, newEmail, updatedPersonalInfo)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ModernLogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to log out? You'll need to sign in again to access your account.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280),
                    lineHeight = 22.sp
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Log Out")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel", color = Color(0xFF6B7280))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}