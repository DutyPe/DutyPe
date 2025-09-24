package com.example.partimes.jobseeker.screens.profile

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.partimes.R
import com.example.partimes.data.ApplicationFormDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.partimes.jobseeker.components.EnhancedNavigationRow
import com.example.partimes.utils.ScrollStateManager
import com.example.partimes.components.ScrollAwareLazyColumn
import com.example.partimes.navigation.Routes
import com.example.partimes.viewmodels.ProfileViewModel
import com.example.partimes.auth.AuthManager
import com.example.partimes.network.ApiClient
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobseekerProfileScreen(
    rootNavController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null,
    dataStore: ApplicationFormDataStore
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profileUiState by profileViewModel.uiState.collectAsState()
    
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    
    // Get profile data from dataStore
    val personalInfo = remember { dataStore.getPersonalInfo() }
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
    val profileCompletion = if (isFormCompleted) 100 else dataStore.getFormCompletionPercentage()
    
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
                        onLogoutClick = { showLogoutDialog = true }
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
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail ->
                userName = newName
                userEmail = newEmail
                showEditDialog = false
            }
        )
    }

    if (showLogoutDialog) {
        ModernLogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                rootNavController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
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
                tint = Color(0xFF3B82F6),
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
    onLogoutClick: () -> Unit
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
                onClick = { rootNavController.navigate(Routes.MANUAL_LOCATION_ROUTE) }
            )

            FlatMenuItem(
                icon = Icons.Outlined.Person,
                title = "Complete Profile",
                subtitle = "Add professional details",
                onClick = { rootNavController.navigate(Routes.ADVANCED_PROFILE) }
            )

            FlatMenuItem(
                icon = Icons.Outlined.Settings,
                title = "Work Preferences",
                subtitle = "Set job preferences & filters",
                onClick = { rootNavController.navigate(Routes.WORK_PREFERENCES) }
            )

            FlatMenuItem(
                icon = Icons.Outlined.Psychology,
                title = "Skills & Experience",
                subtitle = "Showcase your expertise",
                onClick = { rootNavController.navigate(Routes.SKILLS_MANAGEMENT) }
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
                onClick = { rootNavController.navigate(Routes.NOTIFICATION_CENTER) }
            )

            FlatMenuItem(
                icon = Icons.AutoMirrored.Outlined.Help,
                title = "Help & Support",
                subtitle = "Get assistance when needed",
                onClick = { rootNavController.navigate(Routes.HELP) }
            )

            FlatMenuItem(
                icon = Icons.Outlined.Info,
                title = "About ParTimes",
                subtitle = "Learn more about us",
                onClick = { rootNavController.navigate(Routes.ABOUT_US) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
    isDestructive: Boolean = false
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
            tint = if (isDestructive) Color(0xFFDC2626) else Color.Black,
            modifier = Modifier.size(27.dp)
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
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(23.dp)
        )
    }
}


@Composable
private fun ModernEditDialog(
    userName: String,
    userEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var newName by remember { mutableStateOf(userName) }
    var newEmail by remember { mutableStateOf(userEmail) }

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
                verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    onValueChange = { newEmail = it },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(newName, newEmail) },
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
