package com.example.partimes.employer.screens.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerNotificationsScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color(0xFF2193b0)) // Sky blue theme color
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { 
        context.getSharedPreferences("employer_notifications", Context.MODE_PRIVATE)
    }
    
    // Load notification preferences from SharedPreferences (real-time data)
    var jobApplicationsEnabled by remember { 
        mutableStateOf(prefs.getBoolean("job_applications", true))
    }
    var newMessagesEnabled by remember { 
        mutableStateOf(prefs.getBoolean("new_messages", true))
    }
    var jobExpiryEnabled by remember { 
        mutableStateOf(prefs.getBoolean("job_expiry", true))
    }
    var weeklyReportsEnabled by remember { 
        mutableStateOf(prefs.getBoolean("weekly_reports", false))
    }
    var marketingEnabled by remember { 
        mutableStateOf(prefs.getBoolean("marketing", false))
    }
    var pushNotificationsEnabled by remember { 
        mutableStateOf(prefs.getBoolean("push_notifications", true))
    }
    var emailNotificationsEnabled by remember { 
        mutableStateOf(prefs.getBoolean("email_notifications", true))
    }
    var smsNotificationsEnabled by remember { 
        mutableStateOf(prefs.getBoolean("sms_notifications", false))
    }
    
    // Save button state
    var isSaving by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Custom transparent top bar that blends with gradient
            TopAppBar(
                title = {
                    Text(
                        text = "Notification Settings",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2193b0), // Clean sky blue
                            Color(0xFF6dd5ed), // Soft light blue
                            Color(0xFFFFFFFF)  // Pure white
                        )
                    )
                )
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                //elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF4ABE51),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Notification Preferences",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Text(
                        text = "Customize how and when you receive notifications about your job postings and applications.",
                        fontSize = 14.sp,
                        color = Color(0xFF374151).copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }
            }

            // Notification Types Section
            NotificationSection(
                title = "Job & Application Alerts",
                icon = Icons.Default.Work,

                items = listOf(
                    NotificationItem(
                        title = "New Job Applications",
                        description = "Get notified when someone applies to your jobs",
                        icon = Icons.Default.PersonAdd,
                        enabled = jobApplicationsEnabled,
                        onToggle = { jobApplicationsEnabled = it }
                    ),
                    NotificationItem(
                        title = "New Messages",
                        description = "Alerts for messages from workers",
                        icon = Icons.AutoMirrored.Filled.Message,
                        enabled = newMessagesEnabled,
                        onToggle = { newMessagesEnabled = it }
                    ),
                    NotificationItem(
                        title = "Job Expiry Reminders",
                        description = "Reminders before your job postings expire",
                        icon = Icons.Default.Schedule,
                        enabled = jobExpiryEnabled,
                        onToggle = { jobExpiryEnabled = it }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reports Section
            NotificationSection(
                title = "Reports & Analytics",
                icon = Icons.Default.Analytics,
                items = listOf(
                    NotificationItem(
                        title = "Weekly Reports",
                        description = "Summary of your hiring activity",
                        icon = Icons.Default.Assessment,
                        enabled = weeklyReportsEnabled,
                        onToggle = { weeklyReportsEnabled = it }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Marketing Section
            NotificationSection(
                title = "Marketing & Updates",
                icon = Icons.Default.Campaign,
                items = listOf(
                    NotificationItem(
                        title = "Product Updates",
                        description = "New features and platform improvements",
                        icon = Icons.Default.Update,
                        enabled = marketingEnabled,
                        onToggle = { marketingEnabled = it }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Delivery Methods Section
            NotificationSection(
                title = "Delivery Methods",
                icon = Icons.Default.Settings,
                items = listOf(
                    NotificationItem(
                        title = "Push Notifications",
                        description = "In-app notifications",
                        icon = Icons.Default.NotificationsActive,
                        enabled = pushNotificationsEnabled,
                        onToggle = { pushNotificationsEnabled = it }
                    ),
                    NotificationItem(
                        title = "Email Notifications",
                        description = "Receive notifications via email",
                        icon = Icons.Default.Email,
                        enabled = emailNotificationsEnabled,
                        onToggle = { emailNotificationsEnabled = it }
                    ),
                    NotificationItem(
                        title = "SMS Notifications",
                        description = "Receive notifications via SMS",
                        icon = Icons.Default.Sms,
                        enabled = smsNotificationsEnabled,
                        onToggle = { smsNotificationsEnabled = it }
                    )
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button with enhanced functionality
            Button(
                onClick = {
                    isSaving = true
                    // Save notification preferences to SharedPreferences (real-time)
                    saveNotificationPreferences(
                        prefs,
                        jobApplicationsEnabled,
                        newMessagesEnabled,
                        jobExpiryEnabled,
                        weeklyReportsEnabled,
                        marketingEnabled,
                        pushNotificationsEnabled,
                        emailNotificationsEnabled,
                        smsNotificationsEnabled
                    )
                    // Show success feedback
                    showSaveSuccess = true
                    isSaving = false
                    
                    // Hide success message after 2 seconds
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        kotlinx.coroutines.delay(2000)
                        showSaveSuccess = false
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showSaveSuccess) Color(0xFF03707E) else Color(0xFF2196F3),
                    disabledContainerColor = Color(0xFF2193b0).copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSaving) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saving...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showSaveSuccess) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (showSaveSuccess) "Saved!" else "Save Preferences",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NotificationSection(
    title: String,
    icon: ImageVector,
    items: List<NotificationItem>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
            }

            items.forEach { item ->
                NotificationItemRow(item = item)
                if (item != items.last()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(item: NotificationItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.description,
                fontSize = 14.sp,
                color = Color(0xFF6B7280),
                lineHeight = 18.sp
            )
        }
        
        Switch(
            checked = item.enabled,
            onCheckedChange = item.onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2193b0),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E7EB)
            )
        )
    }
}

private data class NotificationItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val onToggle: (Boolean) -> Unit
)

// Real-time save function for notification preferences
private fun saveNotificationPreferences(
    prefs: SharedPreferences,
    jobApplications: Boolean,
    newMessages: Boolean,
    jobExpiry: Boolean,
    weeklyReports: Boolean,
    marketing: Boolean,
    pushNotifications: Boolean,
    emailNotifications: Boolean,
    smsNotifications: Boolean
) {
    prefs.edit().apply {
        putBoolean("job_applications", jobApplications)
        putBoolean("new_messages", newMessages)
        putBoolean("job_expiry", jobExpiry)
        putBoolean("weekly_reports", weeklyReports)
        putBoolean("marketing", marketing)
        putBoolean("push_notifications", pushNotifications)
        putBoolean("email_notifications", emailNotifications)
        putBoolean("sms_notifications", smsNotifications)
        apply()
    }
}
