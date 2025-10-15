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
    onStatusBarColorChange(Color.Black)
    var isVisible by remember { mutableStateOf(false) }

    val termsSections = remember {
        listOf(
            TermsSection(
                title = "Acceptance of Terms",
                content = "By using Quick PartTimes, you agree to these terms and conditions. If you don't agree with any part of these terms, please don't use our service.",
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
                content = "All content and features of Quick PartTimes are our intellectual property. You may not copy, modify, or distribute our app or its content without permission.",
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

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "Terms & Conditions", navController = navController)
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
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Terms & Conditions",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "Please read these terms and conditions carefully before using Quick PartTimes. These terms govern your use of our platform and services.",
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Effective date: September 1, 2025",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Terms Sections
                termsSections.forEach { section ->
                    TermsSectionCard(
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
                            text = "Questions about these terms?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Contact us at legal@quickparttimes.com",
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
private fun TermsSectionCard(
    section: TermsSection,
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
