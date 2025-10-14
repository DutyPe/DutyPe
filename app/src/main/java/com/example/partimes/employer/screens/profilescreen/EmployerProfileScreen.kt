package com.example.partimes.common.employer

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
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
import coil.compose.rememberAsyncImagePainter
import com.example.partimes.R
import com.example.partimes.auth.AuthManager
import com.example.partimes.auth.GoogleSignInManager
import com.example.partimes.viewmodels.ProfileCompletionViewModel
import com.example.partimes.components.ProfessionalLogoutDialog
import com.example.partimes.components.RoleSwitchSection
import com.example.partimes.models.UserRole
import com.example.partimes.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerProfileScreen(
    rootNavController: NavController
) {
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val context = LocalContext.current
    val authManager: AuthManager = remember { AuthManager(context) }
    val googleSignInManager: GoogleSignInManager = remember { GoogleSignInManager(context) }
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
    var profileSetupStatus by remember { mutableStateOf<com.example.partimes.state.ProfileSetupStatus?>(null) }
    val scope = rememberCoroutineScope()

    // Load profile data from our mandatory profile setup
    LaunchedEffect(Unit) {
        try {
            val status = profileCompletionViewModel.getProfileSetupStatus(com.example.partimes.models.UserRole.EMPLOYER)
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
                    employerProfileData?.let { data ->
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
                    }
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

    // Simplified profile completion calculation
    fun calculateProfileCompletion(): Int {
        var completion = 0
        
        // Basic Information (60%)
        if (companyName.isNotEmpty()) completion += 25
        if (companyEmail.isNotEmpty()) completion += 25
        if (companyPhone.isNotEmpty()) completion += 10
        
        // Company Details (30%)
        if (companyAddress.isNotEmpty()) completion += 20
        if (profileImageUri != null) completion += 10
        
        // Additional Verification (10%)
        // This could include document verification, business license, etc.
        // For now, we'll add this when other fields are complete
        if (completion >= 90) completion = 100
        
        return completion
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
                            profileCompletion = profileCompletion,
                            onLogoClick = { imagePickerLauncher.launch("image/*") },
                            onEditClick = { showEditDialog = true }
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
                            
                            profileCompletionViewModel.saveEmployerProfileData(currentUser.uid, employerProfileData)
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
                text = if (companyEmail.isNotEmpty()) companyEmail else "company@example.com",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (companyEmail.isNotEmpty()) Color(0xFF666666) else Color(0xFF999999)
                )
            )

//            Text(
//                text = "Software Development • San Francisco, CA",
//                style = MaterialTheme.typography.bodySmall.copy(
//                    color = Color(0xFF888888)
//                )
//            )

            Spacer(modifier = Modifier.height(12.dp))

            // Company Profile Completion
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (profileCompletion > 0) Color(0xFFE8F5E8) else Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = if (profileCompletion > 0) Color(0xFF4CAF50) else Color(0xFF999999),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (profileCompletion > 0) 
                            "Profile ${profileCompletion}% Complete"
                        else 
                            "Complete Profile",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = if (profileCompletion > 0) Color(0xFF2193b0) else Color(0xFF999999)
                        )
                    )

                    // Show what's missing when profile is incomplete
                   if (profileCompletion < 100) {
                       Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getMissingFieldsText(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF666666),
                               fontSize = 10.sp
                           ),
                           textAlign = TextAlign.Center
                       )
                   }
                }
            }
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
            // Company Management Section
            MenuSectionHeader("Company Management")

            EmployerNavigationRow(
                icon = Icons.Outlined.Business,
                title = "Company Details",
                subtitle = "Update company information",
                onClick = { rootNavController.navigate("company_details") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Work,
                title = "Job Postings",
                subtitle = "Manage active job listings",
                badgeText = "24",
                onClick = { rootNavController.navigate("job_postings") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.People,
                title = "Applications Received",
                subtitle = "Review candidate applications",
                badgeText = "156",
                onClick = { rootNavController.navigate("applications") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Schedule,
                title = "Interview Schedule",
                subtitle = "Manage interviews & meetings",
                badgeText = "8",
                onClick = { rootNavController.navigate("interviews") }
            )

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF0F0F0)
            )

            // Recruitment Tools Section
            MenuSectionHeader("Recruitment Tools")


            EmployerNavigationRow(
                icon = Icons.Outlined.Assessment,
                title = "Skill Assessments",
                subtitle = "Create technical tests",
                onClick = { rootNavController.navigate("assessments") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Analytics,
                title = "Hiring Analytics",
                subtitle = "Track recruitment metrics",
                onClick = { rootNavController.navigate("analytics") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.StarRate,
                title = "Reviews",
                subtitle = "View employee feedback",
                onClick = { rootNavController.navigate("employer_reviews") }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = DividerDefaults.Thickness, color = Color(0xFFF0F0F0)
            )
//             Employer Referral Program
//             Employer Premium Membership
            EmployerNavigationRow(
                icon = Icons.Outlined.CardGiftcard,
                title = "Refer & Earn",
                subtitle = "Invite others and earn rewards",
                onClick = { rootNavController.navigate("employer_refer_earn") }
            )
            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = DividerDefaults.Thickness, color = Color(0xFFF0F0F0)
            )

//             Business Settings Section
            MenuSectionHeader("Settings")

            EmployerNavigationRow(
                icon = Icons.Outlined.Payment,
                title = "Billing & Subscription",
                subtitle = "Manage payment plans",
                onClick = { rootNavController.navigate("billing") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.LocationOn,
                title = "Manage addresses",
                subtitle = "Add or remove office locations",
                onClick = { rootNavController.navigate("employer_manage_addresses") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Notifications,
                title = "Notification Settings",
                subtitle = "Configure alerts & updates",
                onClick = { rootNavController.navigate("employer_notifications") }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = DividerDefaults.Thickness, color = Color(0xFFF0F0F0)
            )

            // Support Section
            MenuSectionHeader("Support & Legal")

            EmployerNavigationRow(
                icon = Icons.Outlined.Assessment,
                title = "Employer Agreement",
                subtitle = "View terms & conditions",
                onClick = { rootNavController.navigate("employer_terms") }
            )

            EmployerNavigationRow(
                icon = Icons.AutoMirrored.Outlined.Help,
                title = "Support",
                subtitle = "Get help & FAQs",
                onClick = { rootNavController.navigate("employer_help") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Info,
                title = "About",
                subtitle = "Learn about our platform",
                onClick = { rootNavController.navigate("employer_about") }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF2193b0),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Company Profile",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Company Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                )
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
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                    label = { Text("Contact Phone") },
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
                    label = { Text("Business Address") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                OutlinedTextField(
                    value = newIndustry,
                    onValueChange = { newIndustry = it },
                    label = { Text("Industry") },
                    leadingIcon = {
                        Icon(Icons.Default.Work, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newCompanySize,
                    onValueChange = { newCompanySize = it },
                    label = { Text("Company Size") },
                    leadingIcon = {
                        Icon(Icons.Default.People, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
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
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newDescription,
                    onValueChange = { newDescription = it },
                    label = { Text("Company Description") },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onSave(newName, newEmail, newPhone, newAddress, newIndustry, newCompanySize, newWebsite, newDescription)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2193b0)
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
        }
    )
}