package com.example.partimes.common.employer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.partimes.R
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerProfileScreen(rootNavController: NavController) {
    var companyLogoUri by remember { mutableStateOf<Uri?>(null) }
    var companyName by remember { mutableStateOf("TechCorp Solutions") }
    var companyEmail by remember { mutableStateOf("hr@techcorp.com") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var profileCompletion by remember { mutableStateOf(85) }

    // Animation states
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            companyLogoUri = uri
        }

    // Gradient background - Corporate theme
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF000000), // Darker blue for corporate feel
            Color(0xFF000000),
            Color(0xFFF8F9FF)
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
            contentPadding = PaddingValues(bottom = 100.dp)
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
                            companyLogoUri = companyLogoUri,
                            companyName = companyName,
                            companyEmail = companyEmail,
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
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail ->
                companyName = newName
                companyEmail = newEmail
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
    companyLogoUri: Uri?,
    companyName: String,
    companyEmail: String,
    profileCompletion: Int,
    onLogoClick: () -> Unit,
    onEditClick: () -> Unit
) {
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
                    progress = profileCompletion / 100f,
                    modifier = Modifier.size(100.dp),
                    color = Color(0xFF4CAF50),
                    strokeWidth = 4.dp,
                    trackColor = Color(0xFFE3F2FD)
                )

                // Company Logo

            Card(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { onLogoClick() },
                    shape = RoundedCornerShape(16.dp), // Square logo for companies
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Image(
                        painter = if (companyLogoUri != null)
                            rememberAsyncImagePainter(companyLogoUri)
                        else
                            painterResource(id = R.drawable.company_default), // Use company default logo
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
                        .background(Color(0xFF1565C0), CircleShape)
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
                    text = companyName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Company Info",
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = companyEmail,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF666666)
                )
            )

            Text(
                text = "Software Development • San Francisco, CA",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF888888)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Company Profile Completion
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E8)
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
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Company Profile ${profileCompletion}% Complete",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1565C0)
                        )
                    )
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
                icon = Icons.Outlined.Search,
                title = "Talent Search",
                subtitle = "Find and contact candidates",
                onClick = { rootNavController.navigate("talent_search") }
            )

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
                title = "Company Reviews",
                subtitle = "Manage employer rating",
                onClick = { rootNavController.navigate("reviews") }
            )

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF0F0F0)
            )

            // Business Settings Section
            MenuSectionHeader("Business Settings")

            EmployerNavigationRow(
                icon = Icons.Outlined.Payment,
                title = "Billing & Subscription",
                subtitle = "Manage payment plans",
                onClick = { rootNavController.navigate("billing") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Security,
                title = "Team Management",
                subtitle = "Manage HR team access",
                onClick = { rootNavController.navigate("team_management") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Notifications,
                title = "Notification Settings",
                subtitle = "Configure alerts & updates",
                onClick = { rootNavController.navigate("notifications") }
            )

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF0F0F0)
            )

            // Support Section
            MenuSectionHeader("Support & Legal")

            EmployerNavigationRow(
                icon = Icons.Outlined.Assignment,
                title = "Employer Agreement",
                subtitle = "View terms & conditions",
                onClick = { rootNavController.navigate("employer_terms") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Help,
                title = "Employer Support",
                subtitle = "Get help with recruiting",
                onClick = { rootNavController.navigate("employer_help") }
            )

            EmployerNavigationRow(
                icon = Icons.Outlined.Info,
                title = "About ParTimes Business",
                subtitle = "Learn about our platform",
                onClick = { rootNavController.navigate("about_business") }
            )

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF0F0F0)
            )

            // Logout
            EmployerNavigationRow(
                icon = Icons.Outlined.ExitToApp,
                title = "Log Out",
                subtitle = "Sign out of employer account",
                onClick = onLogoutClick,
                isDestructive = true
            )
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
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var newName by remember { mutableStateOf(companyName) }
    var newEmail by remember { mutableStateOf(companyEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF1565C0),
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Company Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("HR Email Address") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(newName, newEmail) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
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