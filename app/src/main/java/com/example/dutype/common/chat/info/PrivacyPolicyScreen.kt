package com.example.dutype.common.chat.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.utils.BackNavigationTopBar
import kotlinx.coroutines.delay

data class PolicySection(
    val title: String,
    val content: String,
    val icon: ImageVector
)

@Composable
fun PrivacyPolicyScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)
    
    val policySections = remember {
        listOf(
            PolicySection(
                title = "Information We Collect",
                content = "We collect information you provide directly to us, such as when you create an account, apply for jobs, or contact us for support. This includes your name, email, phone number, location, and job preferences. We also collect usage data to improve our services.",
                icon = Icons.Default.Info
            ),
            PolicySection(
                title = "How We Use Your Information",
                content = "We use your information to match you with relevant job opportunities, send notifications about applications, improve our services, and provide customer support. We never sell your personal information to third parties.",
                icon = Icons.Default.Settings
            ),
            PolicySection(
                title = "Location Data",
                content = "We use your location to show you nearby job opportunities. Location data is only collected when you give permission and can be disabled at any time in your device settings.",
                icon = Icons.Default.LocationOn
            ),
            PolicySection(
                title = "Data Security",
                content = "We implement industry-standard security measures to protect your personal information. All data is encrypted in transit and at rest. We regularly update our security practices to ensure your information remains safe.",
                icon = Icons.Default.Security
            ),
            PolicySection(
                title = "Sharing Information",
                content = "We only share your information with employers when you apply for their jobs. We may also share anonymized, aggregated data for business purposes. We never share personal information without your explicit consent.",
                icon = Icons.Default.Share
            ),
            PolicySection(
                title = "Your Rights",
                content = "You have the right to access, update, or delete your personal information at any time. You can also opt out of certain communications and request a copy of all data we have about you.",
                icon = Icons.Default.AccountCircle
            ),
            PolicySection(
                title = "Data Retention",
                content = "We retain your personal information for as long as your account is active or as needed to provide services. When you delete your account, we securely delete your personal information within 30 days.",
                icon = Icons.Default.Schedule
            ),
            PolicySection(
                title = "Cookies and Tracking",
                content = "We use cookies and similar technologies to improve your experience, remember your preferences, and analyze app usage. You can control cookie settings through your device preferences.",
                icon = Icons.Default.Cookie
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Clean Header - matching the About Us screen style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 20.sp
                )
            )
        }
        
        // Content - Clean format like About Us screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Privacy Policy Overview",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Your privacy is important to us. This policy explains how we collect, use, and protect your personal information when you use DutyPe.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = "Last updated: September 1, 2025",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Policy Sections - Clean format
            policySections.forEachIndexed { index, section ->
                Text(
                    text = "${index + 1}. ${section.title.uppercase()}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 22.sp,
                        lineHeight = 28.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = section.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Black,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
            
            // Contact Information
            Text(
                text = "Questions about privacy?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Contact us at dutypein@gmail.com",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 40.dp)
            )
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

