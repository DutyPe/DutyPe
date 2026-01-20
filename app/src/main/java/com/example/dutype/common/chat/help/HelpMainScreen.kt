package com.example.dutype.common.chat.help

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.AppConstants

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpMainScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)
    val context = LocalContext.current
    
    var showFeedbackSheet by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        // Common Header - consistent across all screens
        CommonHeader(
            title = "Help & Support",
            navController = navController,
            backgroundColor = WorkerColors.CardBackground
        )
        
        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Help Section Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        // FAQs
                        HelpMenuItem(
                            icon = Icons.Outlined.Help,
                            title = "FAQs",
                            subtitle = "Common questions answered",
                            onClick = { navController.navigate(Routes.FAQ) }
                        )
                        
                        MenuDivider()
                        

            
                        
                        // WhatsApp Support
                        HelpMenuItem(
                            icon = Icons.Outlined.Phone,
                            title = "WhatsApp Support",
                            subtitle = "Talk to a support agent",
                            onClick = { 
                                try {
                                    val phoneNumber = "919121706236"
                                    val message = "Hi, I need help with DutyPe app"
                                    val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919121706236"))
                                    try { context.startActivity(intent) } catch (e: Exception) { }
                                }
                            }
                        )
                        
                        MenuDivider()
                        
                        // Report a Problem
                        HelpMenuItem(
                            icon = Icons.Outlined.Report,
                            title = "Report a Problem",
                            subtitle = "Tell us what's wrong",
                            onClick = { navController.navigate(Routes.REPORT) }
                        )
                        
                        MenuDivider()
                        
                        // How to Use the App
                        HelpMenuItem(
                            icon = Icons.Outlined.School,
                            title = "How to Use the App",
                            subtitle = "Voice + graphic tutorials",
                            onClick = { navController.navigate(Routes.TUTORIAL) }
                        )
                        
                        MenuDivider()
                        
                        // Send Feedback
                        HelpMenuItem(
                            icon = Icons.Outlined.Feedback,
                            title = "Send Feedback",
                            subtitle = "Share your thoughts with us",
                            onClick = { showFeedbackSheet = true }
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
    
    // Feedback Bottom Sheet
    com.example.dutype.components.FeedbackBottomSheet(
        isVisible = showFeedbackSheet,
        onDismiss = { showFeedbackSheet = false },
        userRole = "user"
    )
}

@Composable
private fun HelpMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
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
            tint = Color(0xFF1F2937),
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 44.dp),
        thickness = 0.5.dp,
        color = Color(0xFFE5E7EB)
    )
}
