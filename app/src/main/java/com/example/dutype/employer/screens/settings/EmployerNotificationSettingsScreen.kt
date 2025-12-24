package com.example.dutype.employer.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerNotificationSettingsScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    onStatusBarColorChange(Color.White)
    
    // Notification settings state
    var allNotifications by remember { mutableStateOf(true) }
    var newApplications by remember { mutableStateOf(true) }
    var applicationUpdates by remember { mutableStateOf(true) }
    var jobPostingAlerts by remember { mutableStateOf(true) }
    var messageNotifications by remember { mutableStateOf(true) }
    var promotionalNotifications by remember { mutableStateOf(false) }
    
    val backgroundColor = Color(0xFFF8FAFC)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        CommonHeader(
            title = "Notification Settings",
            navController = navController
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Master toggle card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "All Notifications",
                                style = AppTypography.sectionHeader.copy(
                                    color = Color(0xFF1F2937)
                                )
                            )
                            Text(
                                text = if (allNotifications) "Enabled" else "Disabled",
                                style = AppTypography.bodySmall.copy(
                                    color = if (allNotifications) Color(0xFF10B981) else Color(0xFF6B7280)
                                )
                            )
                        }
                    }
                    Switch(
                        checked = allNotifications,
                        onCheckedChange = { allNotifications = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE5E7EB)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Notification categories
            Text(
                text = "Notification Categories",
                style = AppTypography.sectionHeader.copy(
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    NotificationToggleItem(
                        icon = Icons.Default.PersonAdd,
                        title = "New Applications",
                        description = "Get notified when someone applies to your job",
                        isEnabled = newApplications && allNotifications,
                        onToggle = { newApplications = it },
                        enabled = allNotifications
                    )
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    NotificationToggleItem(
                        icon = Icons.Default.Update,
                        title = "Application Updates",
                        description = "Updates on application status changes",
                        isEnabled = applicationUpdates && allNotifications,
                        onToggle = { applicationUpdates = it },
                        enabled = allNotifications
                    )
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    NotificationToggleItem(
                        icon = Icons.Default.Work,
                        title = "Job Posting Alerts",
                        description = "Alerts about your job postings",
                        isEnabled = jobPostingAlerts && allNotifications,
                        onToggle = { jobPostingAlerts = it },
                        enabled = allNotifications
                    )
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    NotificationToggleItem(
                        icon = Icons.Default.Message,
                        title = "Messages",
                        description = "New message notifications",
                        isEnabled = messageNotifications && allNotifications,
                        onToggle = { messageNotifications = it },
                        enabled = allNotifications
                    )
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    NotificationToggleItem(
                        icon = Icons.Default.Campaign,
                        title = "Promotional",
                        description = "Tips, offers and updates from DutyPe",
                        isEnabled = promotionalNotifications && allNotifications,
                        onToggle = { promotionalNotifications = it },
                        enabled = allNotifications
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // System settings button
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF3B82F6)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open System Notification Settings",
                    style = AppTypography.buttonMedium
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NotificationToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) Color(0xFF3B82F6) else Color(0xFFD1D5DB),
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = AppTypography.listItemTitle.copy(
                        color = if (enabled) Color(0xFF1F2937) else Color(0xFF9CA3AF)
                    )
                )
                Text(
                    text = description,
                    style = AppTypography.bodySmall.copy(
                        color = if (enabled) Color(0xFF6B7280) else Color(0xFFD1D5DB)
                    )
                )
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3B82F6),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E7EB),
                disabledCheckedThumbColor = Color.White.copy(alpha = 0.5f),
                disabledCheckedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.3f),
                disabledUncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                disabledUncheckedTrackColor = Color(0xFFE5E7EB).copy(alpha = 0.5f)
            )
        )
    }
}
