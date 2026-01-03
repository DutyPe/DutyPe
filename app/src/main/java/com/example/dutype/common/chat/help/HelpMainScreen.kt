package com.example.dutype.common.chat.help

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.CommonHeader
import com.example.dutype.navigation.Routes
import com.example.dutype.worker.components.EnhancedNavigationRow

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
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Common Header - consistent across all screens
        CommonHeader(
            title = "Help & Support",
            navController = navController
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
                .padding(start = 13.dp)
        ) {
            EnhancedNavigationRow(
                imageResId = R.drawable.helpsupport,
                title = "FAQs",
                subtitle = "Common questions answered",
                onClick = { navController.navigate(Routes.FAQ) }
            )

            EnhancedNavigationRow(
                imageResId = R.drawable.chat,
                title = "Chat Support",
                subtitle = "Live or automated replies",
                onClick = { 
                    // Open WhatsApp chat with support number
                    try {
                        val phoneNumber = "919876543210" // Replace with actual support number
                        val message = "Hi, I need help with DutyPe app"
                        val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback - open email
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@dutype.com")
                            putExtra(Intent.EXTRA_SUBJECT, "DutyPe Support Request")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignore if no email app
                        }
                    }
                }
            )

            EnhancedNavigationRow(
                imageResId = R.drawable.whatsapp,
                title = "WhatsApp Support",
                subtitle = "Talk to a support agent",
                onClick = { 
                    // Open WhatsApp with support number
                    try {
                        val phoneNumber = "919876543210" // Replace with actual support number
                        val message = "Hi, I need help with DutyPe app"
                        val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback - open dialer
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignore if no dialer
                        }
                    }
                }
            )

            EnhancedNavigationRow(
                imageResId = R.drawable.report,
                title = "Report a Problem",
                subtitle = "Tell us what's wrong",
                onClick = { navController.navigate(Routes.REPORT) }
            )

            EnhancedNavigationRow(
                imageResId = R.drawable.tutorial,
                title = "How to Use the App",
                subtitle = "Voice + graphic tutorials",
                onClick = { navController.navigate(Routes.TUTORIAL) }
            )
            
            EnhancedNavigationRow(
                imageResId = R.drawable.share,
                title = "Send Feedback",
                subtitle = "Share your thoughts with us",
                onClick = { showFeedbackSheet = true }
            )
        }
    }
    
    // Feedback Bottom Sheet
    com.example.dutype.components.FeedbackBottomSheet(
        isVisible = showFeedbackSheet,
        onDismiss = { showFeedbackSheet = false },
        userRole = "user"
    )
}
