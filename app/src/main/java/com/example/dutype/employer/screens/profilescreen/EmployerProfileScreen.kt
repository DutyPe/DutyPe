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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.example.dutype.models.UserRole
import com.example.dutype.navigation.Routes
import kotlinx.coroutines.launch
import timber.log.Timber

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
    
    var companyName by remember { mutableStateOf("") }
    var companyEmail by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
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
            profileImageUri = uri
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
        
        // Company Info Section (Clickable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { showEditDialog = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Company Logo
            Box(
                modifier = Modifier.size(60.dp)
            ) {
                Image(
                    painter = if (profileImageUri != null)
                        rememberAsyncImagePainter(profileImageUri)
                    else
                        painterResource(id = R.drawable.company_default),
                    contentDescription = "Company Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White)
                )
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
                    icon = Icons.Default.Business,
                    title = "Company Details",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_COMPANY_DETAILS) ?: rootNavController.navigate(Routes.EMPLOYER_COMPANY_DETAILS) }
                )
            }
            
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
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_NOTIFICATIONS) ?: rootNavController.navigate(Routes.EMPLOYER_NOTIFICATIONS) }
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
            
            item {
                SettingsMenuItem(
                    icon = Icons.Default.CardGiftcard,
                    title = "Refer & Earn",
                    onClick = { localNavController?.navigate(Routes.EMPLOYER_REFER_EARN) ?: rootNavController.navigate(Routes.EMPLOYER_REFER_EARN) }
                )
            }
            
            item {
                RoleSwitchSettingsMenuItem(
                    isEmployerMode = true,
                    onRoleSwitch = { newValue ->
                        scope.launch {
                            try {
                                if (!newValue) {
                                    profileCompletionViewModel.updateUserRole(UserRole.WORKER)
                                    rootNavController.navigate(Routes.WORKER_HOME) {
                                        popUpTo(Routes.EMPLOYER_HOME) { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Error switching to worker role")
                            }
                        }
                    }
                )
            }
            
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

    // Edit Dialog
    if (showEditDialog) {
        EditCompanyDialog(
            companyName = companyName,
            companyEmail = companyEmail,
            companyPhone = companyPhone,
            companyAddress = companyAddress,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail, newPhone, newAddress ->
                scope.launch {
                    try {
                        companyName = newName
                        companyEmail = newEmail
                        companyPhone = newPhone
                        companyAddress = newAddress
                
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val employerProfileData = mapOf(
                                "companyName" to newName,
                                "contactEmail" to newEmail,
                                "contactPhone" to newPhone,
                                "businessAddress" to newAddress,
                                "updatedAt" to System.currentTimeMillis()
                            )
                            profileCompletionViewModel.saveEmployerProfileData(employerProfileData)
                        }
                        showEditDialog = false
                    } catch (e: Exception) {
                        Timber.e("Error updating profile: ${e.message}")
                        showEditDialog = false
                    }
                }
            }
        )
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

@Composable
private fun RoleSwitchSettingsMenuItem(
    isEmployerMode: Boolean,
    onRoleSwitch: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Switch to Worker",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = Color.Black
            ),
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isEmployerMode,
            onCheckedChange = onRoleSwitch,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF3B82F6),
                checkedTrackColor = Color.White.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
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
                            tint = Color(0xFF3B82F6),
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
                
                // Form fields
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Company Name") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                focusedLabelColor = Color(0xFF3B82F6)
                            )
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { },
                            label = { Text("Contact Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
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
                            onValueChange = { newPhone = it },
                            label = { Text("Contact Phone") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                focusedLabelColor = Color(0xFF3B82F6)
                            )
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = newAddress,
                            onValueChange = { newAddress = it },
                            label = { Text("Business Address") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3B82F6),
                                focusedLabelColor = Color(0xFF3B82F6)
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
                        onClick = { onSave(newName, newEmail, newPhone, newAddress) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
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
