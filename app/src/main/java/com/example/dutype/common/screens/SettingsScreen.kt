package com.example.dutype.common.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.AccountDeletionDialog
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.appVersionName
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU
    val currentUser = FirebaseAuth.getInstance().currentUser
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val authManager = profileCompletionViewModel.authManager
    val appVersion = remember(context) { context.appVersionName() }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAccountDeletionDialog by remember { mutableStateOf(false) }

    var userRoleStr by remember { mutableStateOf("WORKER") }
    LaunchedEffect(Unit) {
        val role = profileCompletionViewModel.getUserRole()
        if (role == com.example.dutype.models.UserRole.EMPLOYER) {
            userRoleStr = "EMPLOYER"
        }
    }

    val titleText = if (isTelugu) "సెట్టింగ్‌లు" else "Settings"
    val legalSectionTitle = if (isTelugu) "చట్టపరమైన మరియు నిబంధనలు" else "Legal & Policies"
    val accountSectionTitle = if (isTelugu) "ఖాతా నిర్వహణ" else "Account"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color(0xFF0F172A)
                    )
                )
            }
        }

        // Body Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Legal & Policies
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = legalSectionTitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(0.6.dp, Color(0xFFE2E8F0))
                ) {
                    Column {
                        SettingsMenuItem(
                            icon = Icons.Outlined.Security,
                            title = if (isTelugu) "గోప్యతా విధానం" else "Privacy Policy",
                            subtitle = if (isTelugu) "మీ డేటా భద్రతా వివరాలు" else "How your data is protected",
                            onClick = { navController.navigate(Routes.PRIVACY_POLICY) }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                        SettingsMenuItem(
                            icon = Icons.Outlined.Description,
                            title = if (isTelugu) "సేవా నిబంధనలు" else "Terms of Service",
                            subtitle = if (isTelugu) "నియమ నిబంధనలు" else "Usage rules and guidelines",
                            onClick = { navController.navigate(Routes.TERMS_OF_SERVICE) }
                        )
                    }
                }
            }

            // Section 2: Account Actions (if logged in)
            if (currentUser != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = accountSectionTitle,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(0.6.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            SettingsMenuItem(
                                icon = Icons.AutoMirrored.Outlined.ExitToApp,
                                title = stringResource(R.string.log_out),
                                subtitle = if (isTelugu) "ఖాతా నుండి నిష్క్రమించండి" else "Sign out of your account",
                                iconColor = Color(0xFFD97706),
                                onClick = { showLogoutDialog = true }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            SettingsMenuItem(
                                icon = Icons.Default.DeleteForever,
                                title = if (isTelugu) "ఖాతా శాశ్వతంగా తొలగించు" else "Delete Account & Data",
                                subtitle = if (isTelugu) "ఖాతా మరియు మొత్తం డేటా తొలగింపు" else "Permanent deletion of account and data",
                                isDestructive = true,
                                iconColor = Color(0xFFDC2626),
                                onClick = { showAccountDeletionDialog = true }
                            )
                        }
                    }
                }
            }

            // Version & Made in Bharat footer
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "DutyPe v$appVersion",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "Made with ❤️ in Bharat",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }

    if (showLogoutDialog) {
        ProfessionalLogoutDialog(
            isVisible = showLogoutDialog,
            onDismiss = { showLogoutDialog = false },
            navController = navController,
            userRole = userRoleStr,
            authManager = authManager,
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }

    if (showAccountDeletionDialog) {
        AccountDeletionDialog(
            isVisible = showAccountDeletionDialog,
            onDismiss = { showAccountDeletionDialog = false },
            navController = navController,
            userRole = userRoleStr,
            authManager = authManager,
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    iconColor: Color = Color(0xFF2563EB),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isDestructive) Color(0xFFFEE2E2) else iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) Color(0xFFDC2626) else iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) Color(0xFFDC2626) else Color(0xFF0F172A),
                    fontSize = 15.sp
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
    }
}
