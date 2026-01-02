package com.example.dutype.employer.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.auth.AuthManager
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun EmployerMoreSettingsScreen(
    navController: NavController,
    rootNavController: NavController,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    LaunchedEffect(Unit) {
        onStatusBarColorChange?.invoke(Color.White)
    }
    
    val context = LocalContext.current
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    // AuthManager accessed via ProfileCompletionViewModel (proper DI pattern)
    val authManager = profileCompletionViewModel.authManager
    val scope = rememberCoroutineScope()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Common Header
        CommonHeader(
            title = "More Settings",
            navController = navController
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ABOUT SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SectionHeader(title = "About")
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        SettingsMenuItem(
                            icon = Icons.Default.Info,
                            title = "About DutyPe",
                            onClick = { navController.navigate(Routes.EMPLOYER_ABOUT) }
                        )
                        
                        SettingsMenuItem(
                            icon = Icons.Default.Feedback,
                            title = "Send Feedback",
                            onClick = { showFeedbackSheet = true }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // SECURITY & LEGAL SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SectionHeader(title = "Security & Legal")
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        SettingsMenuItem(
                            icon = Icons.Default.Lock,
                            title = "Privacy Policy",
                            onClick = { navController.navigate(Routes.PRIVACY) }
                        )
                        
                        SettingsMenuItem(
                            icon = Icons.Default.Description,
                            title = "Terms & Conditions",
                            onClick = { navController.navigate(Routes.TERMS) }
                        )
                        
                        SettingsMenuItem(
                            icon = Icons.Default.Security,
                            title = "Security",
                            onClick = { navController.navigate(Routes.SECURITY) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // LOG OUT
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SettingsMenuItem(
                            icon = Icons.Default.ExitToApp,
                            title = "Log Out",
                            onClick = { showLogoutDialog = true },
                            isDestructive = true
                        )
                    }
                }
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
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }
    
    // Feedback Bottom Sheet
    com.example.dutype.components.FeedbackBottomSheet(
        isVisible = showFeedbackSheet,
        onDismiss = { showFeedbackSheet = false },
        userRole = "employer"
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTypography.sectionHeader.copy(color = Color.Black)
    )
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
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) Color(0xFFDC2626) else Color(0xFF374151),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) Color(0xFFDC2626) else Color(0xFF1F2937)
            ),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(20.dp)
        )
    }
}
