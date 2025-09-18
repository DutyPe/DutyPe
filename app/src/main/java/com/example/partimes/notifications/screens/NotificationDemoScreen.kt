package com.example.partimes.notifications.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.partimes.notifications.manager.InAppNotificationManager
import com.example.partimes.notifications.manager.InAppNotificationProvider
import com.example.partimes.notifications.models.*
import com.example.partimes.notifications.services.NotificationService
import com.example.partimes.notifications.services.NotificationTriggerService
import com.example.partimes.ui.theme.JobseekerGradientBackground
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Demo screen showing how to use in-app notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDemoScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationDemoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    InAppNotificationProvider(userId = "demo_user") {
        JobseekerGradientBackground {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Notification Demo",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Test In-App Notifications",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    item {
                        Text(
                            text = "Tap the buttons below to trigger different types of notifications",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                     // Notification trigger buttons
                     items(getNotificationDemoItems(
                         onNewJobAlert = { viewModel.triggerNewJobAlert() },
                         onApplicationUpdate = { viewModel.triggerApplicationUpdate() },
                         onInterviewScheduled = { viewModel.triggerInterviewScheduled() },
                         onEmployerMessage = { viewModel.triggerEmployerMessage() },
                         onJobRecommendation = { viewModel.triggerJobRecommendation() },
                         onSystemUpdate = { viewModel.triggerSystemUpdate() }
                     )) { demoItem ->
                         NotificationDemoCard(
                             title = demoItem.title,
                             description = demoItem.description,
                             icon = demoItem.icon,
                             onClick = { demoItem.onTrigger() }
                         )
                     }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Clear all notifications button
                        Button(
                            onClick = { viewModel.clearAllNotifications() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Notifications")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Demo item for notification triggers
 */
data class NotificationDemoItem(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit,
    val onTrigger: () -> Unit
)

/**
 * Get list of demo notification items
 */
fun getNotificationDemoItems(
    onNewJobAlert: () -> Unit,
    onApplicationUpdate: () -> Unit,
    onInterviewScheduled: () -> Unit,
    onEmployerMessage: () -> Unit,
    onJobRecommendation: () -> Unit,
    onSystemUpdate: () -> Unit
): List<NotificationDemoItem> {
    return listOf(
        NotificationDemoItem(
            title = "New Job Alert",
            description = "Trigger a new job alert notification",
            icon = { 
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = null,
                    tint = Color(0xFF2196F3)
                )
            },
            onTrigger = onNewJobAlert
        ),
        NotificationDemoItem(
            title = "Application Update",
            description = "Trigger an application status update",
            icon = { 
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
            },
            onTrigger = onApplicationUpdate
        ),
        NotificationDemoItem(
            title = "Interview Scheduled",
            description = "Trigger an interview scheduled notification",
            icon = { 
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0)
                )
            },
            onTrigger = onInterviewScheduled
        ),
        NotificationDemoItem(
            title = "Employer Message",
            description = "Trigger an employer message notification",
            icon = { 
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    tint = Color(0xFF00BCD4)
                )
            },
            onTrigger = onEmployerMessage
        ),
        NotificationDemoItem(
            title = "Job Recommendation",
            description = "Trigger a job recommendation notification",
            icon = { 
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFF3F51B5)
                )
            },
            onTrigger = onJobRecommendation
        ),
        NotificationDemoItem(
            title = "System Update",
            description = "Trigger a system update notification",
            icon = { 
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF607D8B)
                )
            },
            onTrigger = onSystemUpdate
        )
    )
}

/**
 * Demo card for notification triggers
 */
@Composable
fun NotificationDemoCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }
            
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Trigger",
                    tint = Color(0xFF6366F1)
                )
            }
        }
    }
}

/**
 * ViewModel for notification demo
 */
@HiltViewModel
class NotificationDemoViewModel @Inject constructor(
    private val notificationService: NotificationService,
    private val triggerService: NotificationTriggerService,
    private val notificationManager: InAppNotificationManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationDemoUiState())
    val uiState: StateFlow<NotificationDemoUiState> = _uiState.asStateFlow()
    
    fun triggerNewJobAlert() {
        // Trigger new job alert notification
        // This would be called from a coroutine scope in real usage
    }
    
    fun triggerApplicationUpdate() {
        // Trigger application update notification
    }
    
    fun triggerInterviewScheduled() {
        // Trigger interview scheduled notification
    }
    
    fun triggerEmployerMessage() {
        // Trigger employer message notification
    }
    
    fun triggerJobRecommendation() {
        // Trigger job recommendation notification
    }
    
    fun triggerSystemUpdate() {
        // Trigger system update notification
    }
    
    fun clearAllNotifications() {
        // Implementation for clearing notifications
    }
}

data class NotificationDemoUiState(
    val isLoading: Boolean = false
)
