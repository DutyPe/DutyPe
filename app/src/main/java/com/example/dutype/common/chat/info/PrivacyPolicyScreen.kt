package com.example.dutype.common.chat.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader

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
        // Common header - used across all info screens
        CommonHeader(
            title = "Privacy Policy",
            navController = navController
        )
        
        // Content - Clean format like About Us screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .padding(start = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Privacy Policy Overview",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
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
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Last updated: December 28, 2025",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Policy Sections - Clean format
            policySections.forEachIndexed { index, section ->
                Text(
                    text = "${index + 1}. ${section.title.uppercase()}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
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
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Contact Information
            Text(
                text = "Questions about privacy?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "If you believe that any information we are holding on you is incorrect or incomplete, please write to:\n\n" +
                       "2-80-6, Surya Thanda Village\nEnkoor (Mandal), Khammam\nTelangana - 507168, India\n\n" +
                       "Or contact us at:\nPhone: +91-9390693988\nEmail: dutypein@gmail.com",
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

