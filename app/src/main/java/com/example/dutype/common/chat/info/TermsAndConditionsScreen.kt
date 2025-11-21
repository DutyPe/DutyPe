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
import com.example.dutype.components.CommonHeader
import kotlinx.coroutines.delay

data class TermsSection(
    val title: String,
    val content: String,
    val icon: ImageVector
)

@Composable
fun TermsAndConditionsScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)

    val termsSections = remember {
        listOf(
            TermsSection(
                title = "Acceptance of Terms",
                content = "By using DutyPe, you agree to these terms and conditions. If you don't agree with any part of these terms, please don't use our service.",
                icon = Icons.Default.Gavel
            ),
            TermsSection(
                title = "User Responsibilities",
                content = "You must provide accurate information, maintain the security of your account, and use the app in compliance with all applicable laws. You're responsible for all activities under your account.",
                icon = Icons.Default.AccountCircle
            ),
            TermsSection(
                title = "Job Applications",
                content = "When applying for jobs, you agree to provide truthful information. Employers may contact you directly. We're not responsible for employment decisions or workplace issues.",
                icon = Icons.Default.Work
            ),
            TermsSection(
                title = "Platform Usage",
                content = "You may not use our platform for illegal activities, spam, harassment, or to post false information. We reserve the right to suspend accounts that violate these rules.",
                icon = Icons.Default.Rule
            ),
            TermsSection(
                title = "Intellectual Property",
                content = "All content and features of DutyPe are our intellectual property. You may not copy, modify, or distribute our app or its content without permission.",
                icon = Icons.Default.Copyright
            ),
            TermsSection(
                title = "Limitation of Liability",
                content = "We provide the platform 'as is' and aren't liable for job outcomes, payment disputes, or workplace issues. Our liability is limited to the extent permitted by law.",
                icon = Icons.Default.Warning
            ),
            TermsSection(
                title = "Data and Privacy",
                content = "Your use of our service is also governed by our Privacy Policy. We collect and use data as described in our privacy policy to provide and improve our services.",
                icon = Icons.Default.PrivacyTip
            ),
            TermsSection(
                title = "Service Availability",
                content = "We strive to keep our service available 24/7, but we don't guarantee uninterrupted access. We may temporarily suspend service for maintenance or updates.",
                icon = Icons.Default.Schedule
            ),
            TermsSection(
                title = "Changes to Terms",
                content = "We may update these terms from time to time. We'll notify you of significant changes through the app or email. Continued use means you accept the updated terms.",
                icon = Icons.Default.Update
            ),
            TermsSection(
                title = "Termination",
                content = "You can delete your account anytime. We may suspend or terminate accounts that violate our terms. Upon termination, these terms remain in effect for any outstanding obligations.",
                icon = Icons.Default.Close
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
            title = "Terms & Conditions",
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
                text = "Terms & Conditions Overview",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Please read these terms and conditions carefully before using DutyPe. These terms govern your use of our platform and services.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Effective date: September 1, 2025",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Terms Sections - Clean format
            termsSections.forEachIndexed { index, section ->
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
                text = "Questions about these terms?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
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

