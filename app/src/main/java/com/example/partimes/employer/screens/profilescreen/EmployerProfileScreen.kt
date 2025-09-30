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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
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
import coil.compose.rememberAsyncImagePainter
import com.example.partimes.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerProfileScreen(rootNavController: NavController) {
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var companyName by remember { mutableStateOf("") }
    var companyEmail by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var profileCompletion by remember { mutableStateOf(0) }
    var isEmployerMode by remember { mutableStateOf(true) }

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
            // Role Switch Section - Add this as the first item
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600))
                ) {
                    /*
                    RoleSwitchSection(
                        isEmployerMode = isEmployerMode,
                        onRoleSwitch = { newMode ->
                            isEmployerMode = newMode
                            if (!newMode) {
                                // Switch to worker mode - using correct route
                                rootNavController.navigate("profile") {
                                    popUpTo("employer_profile") { inclusive = true }
                                }
                            }
                        }
                    )
                    */
                }
            }

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
                Spacer(modifier = Modifier.height(20.dp))

                // Company Stats Cards Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    CompanyStatsCardsSection()
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
                        }
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
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail, newPhone, newAddress ->
                companyName = newName
                companyEmail = newEmail
                companyPhone = newPhone
                companyAddress = newAddress
                
                // Calculate profile completion based on filled fields
                profileCompletion = calculateProfileCompletion()
                
                showEditDialog = false
            }
        )
    }

    // Enhanced Logout Dialog
    if (showLogoutDialog) {
        EmployerLogoutConfirmDialog(
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
private fun CompanyStatsCardsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CompanyStatsCard(
            title = "Active Jobs",
            value = "24",
            icon = Icons.Default.Work,
            color = Color(0xFF1976D2),
            modifier = Modifier.weight(1f)
        )
        CompanyStatsCard(
            title = "Applications",
            value = "156",
            icon = Icons.Default.Assignment,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        CompanyStatsCard(
            title = "Employees",
            value = "500+",
            icon = Icons.Default.People,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
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
    onLogoutClick: () -> Unit
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
    badgeText: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        animationSpec = tween(150),
        label = "alpha"
    )

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
                badgeText?.let { badge ->
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

@Composable
private fun EditCompanyDialog(
    companyName: String,
    companyEmail: String,
    companyPhone: String,
    companyAddress: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var newName by remember { mutableStateOf(companyName) }
    var newEmail by remember { mutableStateOf(companyEmail) }
    var newPhone by remember { mutableStateOf(companyPhone) }
    var newAddress by remember { mutableStateOf(companyAddress) }

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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(400.dp)
            ) {
                item {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Company Name") },
                        placeholder = { Text("Enter your company name") },
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null)
                    },
                    singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                )
                }
                item {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("HR Email Address") },
                        placeholder = { Text("hr@yourcompany.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 98765 43210") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Company Address") },
                        placeholder = { Text("Enter your office address") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
//                item {
//                    OutlinedTextField(
//                        value = newIndustry,
//                        onValueChange = { newIndustry = it },
//                        label = { Text("Industry") },
//                        placeholder = { Text("e.g., Technology, Healthcare, Finance") },
//                        leadingIcon = {
//                            Icon(Icons.Default.Work, contentDescription = null)
//                        },
//                        singleLine = true,
//                        shape = RoundedCornerShape(12.dp),
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                }
//                item {
//                    OutlinedTextField(
//                        value = newSize,
//                        onValueChange = { newSize = it },
//                        label = { Text("Company Size") },
//                        placeholder = { Text("e.g., 1-10, 11-50, 51-200, 500+") },
//                        leadingIcon = {
//                            Icon(Icons.Default.People, contentDescription = null)
//                        },
//                        singleLine = true,
//                        shape = RoundedCornerShape(12.dp),
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                }
//                item {
//                    OutlinedTextField(
//                        value = newBio,
//                        onValueChange = { newBio = it },
//                        label = { Text("Company Bio") },
//                        placeholder = { Text("Tell us about your company...") },
//                        leadingIcon = {
//                            Icon(Icons.Default.Info, contentDescription = null)
//                        },
//                        maxLines = 3,
//                        shape = RoundedCornerShape(12.dp),
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(newName, newEmail, newPhone, newAddress) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2193b0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF666666)
                )
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun EmployerLogoutConfirmDialog(
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
                    tint = Color(0xFFE53E3E),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out from Employer Account",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to log out from your employer account? All unsaved job postings will be lost.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53E3E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Log Out")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF666666)
                )
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
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
                            color = Color(0xFF2193b0).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = null,
                        tint = Color(0xFF2193b0),
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
                    tint = if (!isEmployerMode) Color(0xFF2193b0) else Color(0xFFCCCCCC),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isEmployerMode,
                    onCheckedChange = onRoleSwitch,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF2193b0),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCCCCCC)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = "Employer",
                    tint = if (isEmployerMode) Color(0xFF2193b0) else Color(0xFFCCCCCC),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
