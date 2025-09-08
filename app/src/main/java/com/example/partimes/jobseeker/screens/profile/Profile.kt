package com.example.partimes.jobseeker.screens.profile

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
import com.example.partimes.jobseeker.components.EnhancedNavigationRow
import com.example.partimes.utils.SystemUIConfigs
import com.example.partimes.utils.SystemUIController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobseekerProfileScreen(rootNavController: NavController) {
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var userName by remember { mutableStateOf("VAMSI BANOTH") }
    var userEmail by remember { mutableStateOf("vamsib298@gmail.com") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var profileCompletion by remember { mutableStateOf(75) }


    // Animation states
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            profileImageUri = uri
        }

    // Gradient background with edge-to-edge support
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0066FF),
            Color(0xFF004BD9),
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
            contentPadding = PaddingValues(top = 48.dp, bottom = 100.dp) // Top padding for status bar area
        ) {
            item {
                // Enhanced Header Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp) // Add top padding to move header down from phone edge
                    ) {
                        ProfileHeaderSection(
                            profileImageUri = profileImageUri,
                            userName = userName,
                            userEmail = userEmail,
                            profileCompletion = profileCompletion,
                            onImageClick = { imagePickerLauncher.launch("image/*") },
                            onEditClick = { showEditDialog = true }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))

                // Stats Cards Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    StatsCardsSection()
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Menu Options Section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(1200, 400)) + slideInVertically(tween(1200, 400))
                ) {
                    MenuOptionsSection(
                        rootNavController = rootNavController,
                        onLogoutClick = { showLogoutDialog = true }
                    )
                }
            }
        }
    }

    // Enhanced Edit Dialog
    if (showEditDialog) {
        EditProfileDialog(
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

    // Enhanced Logout Dialog
    if (showLogoutDialog) {
        LogoutConfirmDialog(
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
private fun ProfileHeaderSection(
    profileImageUri: Uri?,
    userName: String,
    userEmail: String,
    profileCompletion: Int,
    onImageClick: () -> Unit,
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
            // Profile Image with Status Ring
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

                // Profile Image
                Card(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { onImageClick() },
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Image(
                        painter = if (profileImageUri != null)
                            rememberAsyncImagePainter(profileImageUri)
                        else
                            painterResource(id = R.drawable.user),
                        contentDescription = "Profile Picture",
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
                        .background(Color(0xFF0066FF), CircleShape)
                        .clickable { onImageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Change Photo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = userName,
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
                        contentDescription = "Edit Name",
                        tint = Color(0xFF0066FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF666666)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Profile Completion
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0F7FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Profile ${profileCompletion}% Complete",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0066FF)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCardsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatsCard(
            title = "Applications",
            value = "12",
            icon = Icons.Default.Assignment,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        StatsCard(
            title = "Interviews",
            value = "3",
            icon = Icons.Default.Schedule,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
        StatsCard(
            title = "Offers",
            value = "1",
            icon = Icons.Default.WorkspacePremium,
            color = Color(0xFF9C27B0),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatsCard(
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
private fun MenuOptionsSection(
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
            // Account Section
            MenuSectionHeader("Account & Profile")

            EnhancedNavigationRow(
                icon = Icons.Outlined.LocationOn,
                title = "Location & Availability",
                subtitle = "Manage your work location",
                onClick = { rootNavController.navigate("location") }
            )

            EnhancedNavigationRow(
                icon = Icons.Outlined.Settings,
                title = "Work Preferences",
                subtitle = "Set your work preferences",
                onClick = { rootNavController.navigate("preferences") }
            )

            EnhancedNavigationRow(
                icon = Icons.Outlined.Psychology,
                title = "Experience & Skills",
                subtitle = "Update your expertise",
                onClick = { rootNavController.navigate("skills") }
            )

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF0F0F0)
            )

            // App Section
            MenuSectionHeader("App & Support")

            EnhancedNavigationRow(
                icon = Icons.Outlined.Assignment,
                title = "Terms & Conditions",
                subtitle = "Read our terms",
                onClick = { rootNavController.navigate("terms") }
            )

            EnhancedNavigationRow(
                icon = Icons.Outlined.Help,
                title = "Help & Support",
                subtitle = "Get help when needed",
                onClick = { rootNavController.navigate("help") }
            )

            EnhancedNavigationRow(
                icon = Icons.Outlined.Info,
                title = "About ParTimes",
                subtitle = "Learn about our app",
                onClick = { rootNavController.navigate("about") }
            )

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color(0xFFF0F0F0)
            )

            // Logout
            EnhancedNavigationRow(
                icon = Icons.Outlined.ExitToApp,
                title = "Log Out",
                subtitle = "Sign out of your account",
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
private fun EditProfileDialog(
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
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Email Address") },
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
                    containerColor = Color(0xFF0066FF)
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
private fun LogoutConfirmDialog(
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
                    text = "Log Out",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to log out? You'll need to sign in again to access your account.",
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
