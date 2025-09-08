package com.example.partimes.common.chat.info

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.utils.BackNavigationTopBar
import kotlinx.coroutines.delay

data class PolicySection(
    val title: String,
    val content: String,
    val icon: ImageVector
)

@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    var isVisible by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "Privacy Policy", navController = navController)
        }
    ) { innerPadding ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Header Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Privacy Policy",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "Your privacy is important to us. This policy explains how we collect, use, and protect your personal information when you use Quick PartTimes.",
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Last updated: September 1, 2025",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Policy Sections
                policySections.forEach { section ->
                    PolicySectionCard(
                        section = section,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Questions about privacy?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Contact us at privacy@quickparttimes.com",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySectionCard(
    section: PolicySection,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = section.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = section.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
