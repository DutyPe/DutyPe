package com.example.dutype.common.employer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.dutype.app.R
import com.example.dutype.auth.AuthManager
import com.example.dutype.auth.GoogleSignInManager
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.services.ProfileCompletionService
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.navigation.Routes
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun EmployerProfileScreen(
    rootNavController: NavController,
    localNavController: NavController? = null,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    // Set status bar to white for this screen
    LaunchedEffect(Unit) {
        onStatusBarColorChange?.invoke(Color.White)
    }
    
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val context = LocalContext.current
    val authManager: AuthManager = remember { AuthManager(context) }
    val googleSignInManager: GoogleSignInManager = remember { GoogleSignInManager(context) }
    val profileCompletionService: ProfileCompletionService = remember { ProfileCompletionService() }
    
    var companyName by remember { mutableStateOf("") }
    var companyEmail by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") } 
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load profile data
    LaunchedEffect(Unit) {
        val savedEmail = profileCompletionViewModel.getUserEmail()
        val savedName = profileCompletionViewModel.getUserName()
        
        if (savedEmail != null) companyEmail = savedEmail
        if (savedName != null) companyName = savedName
        
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val employerProfileData = profileCompletionViewModel.getEmployerProfileData(currentUser.uid)
                employerProfileData.fold(
                    onSuccess = { data ->
                        companyName = data["companyName"] as? String ?: companyName
                        companyEmail = data["contactEmail"] as? String ?: companyEmail
                        companyPhone = data["contactPhone"] as? String ?: ""
                        companyAddress = data["businessAddress"] as? String ?: ""
                        profileImageUrl = data["profileImageUrl"] as? String
                    },
                    onFailure = {
                        Timber.e("Error loading employer profile data")
                    }
                )
            } catch (e: Exception) {
                Timber.e("Error loading profile: ${e.message}")
            }
        }
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                profileImageUri = selectedUri
                isUploadingImage = true
                
                // Upload image to Firebase Storage and update profile
                scope.launch {
                    try {
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            // Upload to Firebase Storage
                            val uploadResult = profileCompletionViewModel.uploadProfileImage(selectedUri, currentUser.uid, "employer")
                            uploadResult.fold(
                                onSuccess = { imageUrl ->
                                    profileImageUrl = imageUrl
                                    Timber.i("Profile image uploaded: $imageUrl")
                                    
                                    // Update employer profile data with image URL
                                    val updatedProfileData = mapOf(
                                        "profileImageUrl" to imageUrl,
                                        "updatedAt" to System.currentTimeMillis()
                                    )
                                    profileCompletionViewModel.saveEmployerProfileData(updatedProfileData)
                                },
                                onFailure = { exception ->
                                    Timber.e(exception, "Failed to upload profile image")
                                }
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error uploading profile image")
                    } finally {
                        isUploadingImage = false
                    }
                }
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(13.dp))
        
        // Profile Title
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Company Info Section (Clickable) - Navigate to Company Details screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { 
                    localNavController?.navigate(Routes.EMPLOYER_COMPANY_DETAILS) 
                        ?: rootNavController.navigate(Routes.EMPLOYER_COMPANY_DETAILS) 
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Company Logo - Clickable to upload image
            Box(
                modifier = Modifier.size(60.dp)
            ) {
                Image(
                    painter = when {
                        isUploadingImage -> painterResource(id = R.drawable.company_default)
                        profileImageUri != null -> rememberAsyncImagePainter(profileImageUri)
                        profileImageUrl != null -> rememberAsyncImagePainter(profileImageUrl)
                        else -> painterResource(id = R.drawable.company_default)
                    },
                    contentDescription = "Company Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
                
                // Show loading indicator when uploading
                if (isUploadingImage) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Company Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = companyName.ifEmpty { "Your Company" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = companyEmail.ifEmpty { "company@email.com" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray
                    ),
                    maxLines = 1
                )
                if (companyPhone.isNotEmpty()) {
                    Text(
                        text = companyPhone,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        )
                    )
                }
            }
            
            // Right side - Arrow only
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // App Settings Section
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Settings Menu Items
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item {
                SettingsMenuItem(
                    icon = Icons.Default.LocationOn,
                    title = "Manage Addresses",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) ?: rootNavController.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_NOTIFICATION_SETTINGS) ?: rootNavController.navigate(Routes.EMPLOYER_NOTIFICATION_SETTINGS) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Help,
                    title = "Help & Support",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_HELP) ?: rootNavController.navigate(Routes.EMPLOYER_HELP) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Info,
                    title = "About Us",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_ABOUT) ?: rootNavController.navigate(Routes.EMPLOYER_ABOUT) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    onClick = { localNavController?.navigate(Routes.PRIVACY) ?: rootNavController.navigate(Routes.PRIVACY) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Gavel,
                    title = "Terms & Conditions",
                    onClick = { localNavController?.navigate(Routes.TERMS) ?: rootNavController.navigate(Routes.TERMS) }
                )
            }
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.Security,
                    title = "Security",
                    onClick = { localNavController?.navigate(Routes.SECURITY) ?: rootNavController.navigate(Routes.SECURITY) }
                )
            }
            
            // Refer & Earn - Commented out for v2 release
            // item {
            //     SettingsMenuItem(
            //         icon = Icons.Default.CardGiftcard,
            //         title = "Refer & Earn",
            //         onClick = { localNavController?.navigate(Routes.EMPLOYER_REFER_EARN) ?: rootNavController.navigate(Routes.EMPLOYER_REFER_EARN) }
            //     )
            // }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsMenuItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Log Out",
                    onClick = { showLogoutDialog = true },
                    isDestructive = true
                )
            }
        }
    }

    // Logout Dialog
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
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
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
            tint = if (isDestructive) Color(0xFFDC2626) else Color(0xFF3B82F6),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) Color(0xFFDC2626) else Color.Black
            ),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}




