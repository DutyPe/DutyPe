package com.example.dutype.common.employer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.dutype.app.R
import com.example.dutype.auth.AuthManager
import com.example.dutype.auth.GoogleSignInManager
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.components.ProfileCompletionProgress
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.components.RoleSwitchSection
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerProfileScreen(
    rootNavController: NavController,
    localNavController: NavController? = null
) {
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val context = LocalContext.current
    val authManager: AuthManager = remember { AuthManager(context) }
    val googleSignInManager: GoogleSignInManager = remember { GoogleSignInManager(context) }
    val profileCompletionService: ProfileCompletionService = remember { ProfileCompletionService() }
    
    // Profile completion state
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    var isProfileCompleted by remember { mutableStateOf(false) }
    var companyName by remember { mutableStateOf("") }
    var companyEmail by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var companySize by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var profileCompletion by remember { mutableStateOf(0) }
    var isEmployerMode by remember { mutableStateOf(true) }
    var isVerified by remember { mutableStateOf(false) }
    var profileSetupStatus by remember { mutableStateOf<com.example.dutype.state.ProfileSetupStatus?>(null) }
    val scope = rememberCoroutineScope()

    // Calculate profile completion percentage
    LaunchedEffect(companyName, companyEmail, companyPhone, companyAddress, industry, companySize, website, description) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val completion = profileCompletionService.calculateEmployerProfileCompletion(
                    companyName = companyName,
                    contactEmail = companyEmail,
                    contactPhone = companyPhone,
                    businessAddress = companyAddress,
                    industry = industry,
                    companySize = companySize,
                    website = website,
                    description = description,
                    profileImageUrl = null
                )
                profileCompletionPercentage = completion
                isProfileCompleted = completion >= 100
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Load profile data from our mandatory profile setup
    LaunchedEffect(Unit) {
        try {
            val status = profileCompletionViewModel.getProfileSetupStatus(com.example.dutype.models.UserRole.EMPLOYER)
            profileSetupStatus = status
            profileCompletion = status.completionPercentage
            
            // Load saved profile data
            val savedEmail = profileCompletionViewModel.getUserEmail()
            val savedName = profileCompletionViewModel.getUserName()
            
            if (savedEmail != null) {
                companyEmail = savedEmail
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
                        companyEmail = data["contactEmail"] as? String ?: companyEmail
                        companyPhone = data["contactPhone"] as? String ?: ""
                        companyAddress = data["businessAddress"] as? String ?: ""
                        
                        // Additional fields that might be available
                        industry = data["industry"] as? String ?: ""
                        companySize = data["companySize"] as? String ?: ""
                        website = data["website"] as? String ?: ""
                        description = data["description"] as? String ?: ""
                        
                        println("✅ Employer profile data loaded from Firebase:")
                        println("  Company Name: $companyName")
                        println("  Email: $companyEmail")
                        println("  Phone: $companyPhone")
                        println("  Address: $companyAddress")
                        println("  Industry: $industry")
                        println("  Company Size: $companySize")
                        },
                        onFailure = { exception ->
                            println("❌ Error loading employer profile data: ${exception.message}")
                    }
                    )
                } catch (e: Exception) {
                    // Handle error loading additional profile data
                    println("❌ Error loading employer profile data: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            // Handle error - keep default values
            println("❌ Error in employer profile LaunchedEffect: ${e.message}")
            e.printStackTrace()
        }
    }

    // Animation states
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Enhanced profile completion calculation based on setup data
    fun calculateProfileCompletion(): Int {
        var completion = 0
        val totalFields = 10
        
        // Basic Information (40% - 4 fields)
        if (companyName.isNotEmpty()) completion += 1
        if (companyEmail.isNotEmpty()) completion += 1
        if (companyPhone.isNotEmpty()) completion += 1
        if (companyAddress.isNotEmpty()) completion += 1
        
        // Company Details (30% - 3 fields)
        if (profileImageUri != null) completion += 1
        if (industry.isNotEmpty()) completion += 1
        if (companySize.isNotEmpty()) completion += 1
        
        // Professional Details (20% - 2 fields)
        if (website.isNotEmpty()) completion += 1
        if (description.isNotEmpty()) completion += 1
        
        // Verification Status (10% - 1 field)
        if (isVerified) completion += 1
        
        return (completion * 100) / totalFields
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            profileImageUri = uri
            
            // Update profile completion when image is selected
            profileCompletion = calculateProfileCompletion()
        }

    // Gradient background - Corporate theme
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E3A8A), // Deep professional blue
            Color(0xFF3B82F6), // Bright blue
            Color(0xFFE0F2FE), // Light blue
            Color.White
        ),
        startY = 0f,
        endY = 800f
    )


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 0.dp)
        ) {

            item {
                // Enhanced Company Header Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp)
                    ) {
                        CompanyHeaderSection(
                            profileImageUri = profileImageUri,
                            companyName = companyName,
                            companyEmail = companyEmail,
                            companyPhone = companyPhone,
                            companyAddress = companyAddress,
                            industry = industry,
                            companySize = companySize,
                            profileCompletion = profileCompletion,
                            onLogoClick = { imagePickerLauncher.launch("image/*") },
                            onEditClick = { showEditDialog = true }
                        )
                    }
                }
            }

            // Profile Completion Progress (Clickable)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 300)) + slideInVertically(tween(1000, 300))
                ) {
                    Box(
                        modifier = Modifier.clickable {
                            // Navigate to company details screen
                            rootNavController.navigate(Routes.EMPLOYER_COMPANY_DETAILS)
                        }
                    ) {
                        ProfileCompletionProgress(
                            completionPercentage = profileCompletionPercentage,
                            isCompleted = isProfileCompleted,
                            showDetails = !isProfileCompleted,
                            isWorker = false
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Employer Menu Options Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1200, 400)) + slideInVertically(tween(1200, 400))
                ) {
                    EmployerMenuOptionsSection(
                        rootNavController = rootNavController,
                        localNavController = localNavController,
                        onLogoutClick = {
                            showLogoutDialog = true
                        },
                        profileCompletionViewModel = profileCompletionViewModel,
                        scope = scope
                    )                }
            }
        }
    }

    // Enhanced Company Edit Dialog
    if (showEditDialog) {
        EditCompanyDialog(
            companyName = companyName,
            companyEmail = companyEmail,
            companyPhone = companyPhone,
            companyAddress = companyAddress,
            industry = industry,
            companySize = companySize,
            website = website,
            description = description,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail, newPhone, newAddress, newIndustry, newCompanySize, newWebsite, newDescription ->
                scope.launch {
                    try {
                        // Update local variables
                companyName = newName
                companyEmail = newEmail
                companyPhone = newPhone
                companyAddress = newAddress
                        industry = newIndustry
                        companySize = newCompanySize
                        website = newWebsite
                        description = newDescription
                
                // Calculate profile completion based on filled fields
                profileCompletion = calculateProfileCompletion()
                
                        // Save to Firebase
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val employerProfileData = mapOf(
                                "companyName" to newName,
                                "contactEmail" to newEmail,
                                "contactPhone" to newPhone,
                                "businessAddress" to newAddress,
                                "industry" to newIndustry,
                                "companySize" to newCompanySize,
                                "website" to newWebsite,
                                "description" to newDescription,
                                "updatedAt" to System.currentTimeMillis()
                            )
                            
                            profileCompletionViewModel.saveEmployerProfileData(employerProfileData)
                            println("✅ Employer profile updated successfully in Firebase")
                        }
                        
                        showEditDialog = false
                    } catch (e: Exception) {
                        println("❌ Error updating employer profile: ${e.message}")
                        // Still close dialog even if Firebase save fails
                showEditDialog = false
                    }
                }
            }
        )
    }

    // Enhanced Logout Dialog
    if (showLogoutDialog) {
        ProfessionalLogoutDialog(
            isVisible = showLogoutDialog,
            onDismiss = { showLogoutDialog = false },
            navController = rootNavController,
            userRole = "Employer",
            authManager = authManager,
            googleSignInManager = googleSignInManager,
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }
}


@Composable
private fun CompanyHeaderSection(
    profileImageUri: Uri?,
    companyName: String,
    companyEmail: String,
    companyPhone: String,
    companyAddress: String,
    industry: String,
    companySize: String,
    profileCompletion: Int,
    onLogoClick: () -> Unit,
    onEditClick: () -> Unit
) {
    // Get text showing what fields are missing
    fun getMissingFieldsText(): String {
        val missingFields = mutableListOf<String>()
        
        if (companyName.isEmpty()) missingFields.add("Company Name")
        if (companyEmail.isEmpty()) missingFields.add("Email")
        if (companyPhone.isEmpty()) missingFields.add("Phone")
        if (companyAddress.isEmpty()) missingFields.add("Address")
        if (profileImageUri == null) missingFields.add("Logo")
        
        return when {
            missingFields.isEmpty() -> "Profile Complete!"
            missingFields.size <= 3 -> "Missing: ${missingFields.joinToString(", ")}"
            else -> "Missing ${missingFields.size} Tap to complete"
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Company Logo with Status Ring
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Outer ring for profile completion
                CircularProgressIndicator(
                progress = { profileCompletion / 100f },
                    modifier = Modifier.size(100.dp),
                    color = Color(0xFF4CAF50),
                    strokeWidth = 4.dp,
                trackColor = Color(0xFFE3F2FD),
                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                )

                // Company Logo
            Card(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { onLogoClick() },
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Image(
                        painter = if (profileImageUri != null)
                            rememberAsyncImagePainter(profileImageUri)
                        else
                            painterResource(id = R.drawable.company_default),
                        contentDescription = "Company Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Camera icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-8).dp, y = (-8).dp)
                        .size(28.dp)
                        .background(Color(0xFF2193b0), CircleShape)
                        .clickable { onLogoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Change Logo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Company Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (companyName.isNotEmpty()) companyName else "Your Company Name",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (companyName.isNotEmpty()) Color(0xFF1A1A1A) else Color(0xFF999999)
                    )
                )
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Company Info",
                        tint = Color(0xFF2193b0),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = if (companyEmail.isNotEmpty()) companyEmail else "dutypein@gmail.com",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (companyEmail.isNotEmpty()) Color(0xFF666666) else Color(0xFF999999)
                )
            )

//            Text(
//                text = "Food Service • San Francisco, CA",
//                style = MaterialTheme.typography.bodySmall.copy(
//                    color = Color(0xFF888888)
//                )
//            )

        }
    }
}


@Composable
private fun CompanyStatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF666666)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmployerMenuOptionsSection(
    rootNavController: NavController,
    localNavController: NavController? = null,
    onLogoutClick: () -> Unit,
    profileCompletionViewModel: ProfileCompletionViewModel,
    scope: CoroutineScope
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            // Essential Menu Items Only
            MenuSectionHeader("Account Management")

            EmployerNavigationRow(
                icon = Icons.Outlined.Business,
                title = "Company Details",
                subtitle = "Update company information",
                onClick = { localNavController?.navigate(Routes.EMPLOYER_COMPANY_DETAILS) ?: rootNavController.navigate(Routes.EMPLOYER_COMPANY_DETAILS) }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                subtitle = "Manage notification preferences",
                onClick = { localNavController?.navigate(Routes.EMPLOYER_NOTIFICATIONS) ?: rootNavController.navigate(Routes.EMPLOYER_NOTIFICATIONS) }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Settings,
                title = "Settings",
                subtitle = "App preferences and privacy",
                onClick = { localNavController?.navigate(Routes.EMPLOYER_ABOUT) ?: rootNavController.navigate(Routes.EMPLOYER_ABOUT) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = DividerDefaults.Thickness, color = Color(0xFFF0F0F0)
            )

            // Legal & Security Section
            MenuSectionHeader("Legal & Security")

            EmployerNavigationRow(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "How we protect your privacy",
                onClick = { localNavController?.navigate(Routes.PRIVACY) ?: rootNavController.navigate(Routes.PRIVACY) }
            )

            EmployerNavigationRow(
                icon = Icons.Default.Gavel,
                title = "Terms & Conditions",
                subtitle = "Terms of service agreement",
                onClick = { localNavController?.navigate(Routes.TERMS) ?: rootNavController.navigate(Routes.TERMS) }
            )

            EmployerNavigationRow(
                icon = Icons.Default.Security,
                title = "Security",
                subtitle = "Account security & tips",
                onClick = { localNavController?.navigate(Routes.SECURITY) ?: rootNavController.navigate(Routes.SECURITY) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = DividerDefaults.Thickness, color = Color(0xFFF0F0F0)
            )

            // Support Section
            MenuSectionHeader("Support")

            EmployerNavigationRow(
                icon = Icons.AutoMirrored.Outlined.Help,
                title = "Support",
                subtitle = "Get help & FAQs",
                onClick = { localNavController?.navigate(Routes.EMPLOYER_HELP) ?: rootNavController.navigate(Routes.EMPLOYER_HELP) }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Info,
                title = "About",
                subtitle = "Learn about our platform",
                onClick = { localNavController?.navigate(Routes.EMPLOYER_ABOUT) ?: rootNavController.navigate(Routes.EMPLOYER_ABOUT) }
            )

            // Role Switch Section - Above logout button
                RoleSwitchSection(
                    currentRole = UserRole.EMPLOYER,
                    onRoleSwitch = { newRole ->
                        println("🔄 Employer Profile - Role switch triggered: $newRole")
                        when (newRole) {
                            UserRole.WORKER -> {
                                println("🔄 Employer Profile - Switching to WORKER")
                                // Update user role in local storage first
                                scope.launch {
                                    try {
                                        println("🔄 Employer Profile - Updating user role to WORKER")
                                        profileCompletionViewModel.updateUserRole(UserRole.WORKER)
                                        // Small delay to ensure role is saved
                                        delay(500)
                                        println("🔄 Employer Profile - Navigating to WORKER_HOME")
                                        // Switch to worker mode
                                        rootNavController.navigate(Routes.WORKER_HOME) {
                                            popUpTo(Routes.EMPLOYER_HOME) { inclusive = true }
                                        }
                                        println("🔄 Employer Profile - Navigation completed")
                                    } catch (e: Exception) {
                                        // Handle error gracefully
                                        println("❌ Error switching to worker role: ${e.message}")
                                    }
                                }
                            }
                            else -> {
                                println("🔄 Employer Profile - Invalid role switch: $newRole")
                            }
                        }
                    }
                )

            Spacer(modifier = Modifier.height(16.dp))

            // Logout
            EmployerNavigationRow(
                icon = Icons.AutoMirrored.Outlined.ExitToApp,
                title = "Log Out",
                subtitle = "Sign out of employer account",
                onClick = onLogoutClick,
                isDestructive = true
            )
            Spacer(modifier = Modifier.height(8.dp))

        }
    }
}

@Composable
private fun MenuSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF666666)
        ),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun EmployerNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    badgeText: String? = null,
    jobId: String? = null, // Add jobId for job-related rows
    profileCompletionViewModel: ProfileCompletionViewModel? = null // Pass viewModel for application count
) {
    var isPressed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        animationSpec = tween(150),
        label = "alpha"
    )

    // Application count state for job postings
    var applicationCount by remember { mutableStateOf(badgeText) }
    val scope = rememberCoroutineScope()

    // If jobId and viewModel are provided, fetch the correct application count
    LaunchedEffect(jobId) {
        if (jobId != null && profileCompletionViewModel != null) {
            scope.launch {
                val count = getApplicationCountForJob(jobId, profileCompletionViewModel)
                applicationCount = count.toString()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable {
                isPressed = true
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isDestructive)
                        Color(0xFFFFEBEE)
                    else
                        Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color(0xFFE53E3E) else Color(0xFF1565C0),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (isDestructive) Color(0xFFE53E3E) else Color(0xFF1A1A1A)
                    )
                )
                
                // Badge for notifications/counts
                (applicationCount ?: badgeText)?.let { badge ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = Color(0xFFFF4444)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF666666)
                )
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(20.dp)
        )
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }
}

// Add a function to get application count for a specific job
private suspend fun getApplicationCountForJob(jobId: String, profileCompletionViewModel: ProfileCompletionViewModel): Int {
    // Fetch applications for this job only
    val applications = profileCompletionViewModel.getApplicationsForJob(jobId)
    return applications
}
@Composable
private fun EditCompanyDialog(
    companyName: String,
    companyEmail: String,
    companyPhone: String,
    companyAddress: String,
    industry: String,
    companySize: String,
    website: String,
    description: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String) -> Unit
) {
    var newName by remember { mutableStateOf(companyName) }
    var newEmail by remember { mutableStateOf(companyEmail) }
    var newPhone by remember { mutableStateOf(companyPhone) }
    var newAddress by remember { mutableStateOf(companyAddress) }
    var newIndustry by remember { mutableStateOf(industry) }
    var newCompanySize by remember { mutableStateOf(companySize) }
    var newWebsite by remember { mutableStateOf(website) }
    var newDescription by remember { mutableStateOf(description) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF2193b0),
                            modifier = Modifier.size(28.dp)
                )
                        Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Edit Company Profile",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF6B7280)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Scrollable content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Company Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2193b0),
                                focusedLabelColor = Color(0xFF2193b0)
                )
                        )
                    }
                    
                    item {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { /* Email cannot be changed */ },
                    label = { Text("Contact Email") },
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
                    }
                    
                    item {
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { /* Phone cannot be changed */ },
                    label = { Text("Contact Phone") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF666666),
                                disabledBorderColor = Color(0xFFE0E0E0),
                                disabledLabelColor = Color(0xFF999999)
                    ),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Phone number locked",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                    
                    item {
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                    label = { Text("Business Address") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2193b0),
                                focusedLabelColor = Color(0xFF2193b0)
                    )
                        )
                    }
                    
                    item {
                OutlinedTextField(
                    value = newIndustry,
                    onValueChange = { newIndustry = it },
                    label = { Text("Industry") },
                    leadingIcon = {
                        Icon(Icons.Default.Work, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2193b0),
                                focusedLabelColor = Color(0xFF2193b0)
                )
                        )
                    }
                    
                    item {
                OutlinedTextField(
                    value = newCompanySize,
                    onValueChange = { newCompanySize = it },
                    label = { Text("Company Size") },
                    leadingIcon = {
                        Icon(Icons.Default.People, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2193b0),
                                focusedLabelColor = Color(0xFF2193b0)
                )
                        )
                    }
                    
                    item {
                OutlinedTextField(
                    value = newWebsite,
                    onValueChange = { newWebsite = it },
                    label = { Text("Website") },
                    placeholder = { Text("https://example.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Language, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2193b0),
                                focusedLabelColor = Color(0xFF2193b0)
                )
                        )
                    }
                    
                    item {
                OutlinedTextField(
                    value = newDescription,
                    onValueChange = { newDescription = it },
                    label = { Text("Company Description") },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2193b0),
                                focusedLabelColor = Color(0xFF2193b0)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF6B7280))
                    }
                    
            Button(
                onClick = { 
                    onSave(newName, newEmail, newPhone, newAddress, newIndustry, newCompanySize, newWebsite, newDescription)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2193b0)
                ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
            ) {
                Text("Save Changes")
            }
                }
            }
        }
    }
}


@Composable
fun InfoChip(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
