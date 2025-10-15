package com.example.dutype.common.chat.help

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.utils.BackNavigationTopBar
import kotlinx.coroutines.delay

data class SecurityTip(
    val title: String,
    val description: String,
    val tips: List<String>,
    val icon: ImageVector
)

@Composable
fun SecurityScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    // Set the status bar color to black
    onStatusBarColorChange(Color.Black)

    var isVisible by remember { mutableStateOf(false) }

    val securitySections = remember {
        listOf(
            SecurityTip(
                title = "Safe Job Searching",
                description = "Tips to stay safe while looking for work",
                icon = Icons.Default.Search,
                tips = listOf(
                    "🏢 Verify employer information before applying",
                    "📍 Meet employers in public places for interviews",
                    "🚫 Never pay fees to apply for jobs",
                    "📱 Use in-app communication when possible",
                    "🔍 Research companies online before meeting"
                )
            ),
            SecurityTip(
                title = "Personal Information",
                description = "Protect your sensitive data",
                icon = Icons.Default.PrivacyTip,
                tips = listOf(
                    "🔒 Never share banking details in job applications",
                    "📄 Don't provide ID copies unless job is confirmed",
                    "🏠 Be cautious about sharing home address",
                    "📞 Use app messaging before giving personal phone",
                    "💳 Never give credit card or payment info upfront"
                )
            ),
            SecurityTip(
                title = "Meeting Employers",
                description = "Safe practices for job interviews and meetings",
                icon = Icons.Default.People,
                tips = listOf(
                    "🌅 Schedule meetings during daytime hours",
                    "👥 Inform someone about your meeting location",
                    "🚌 Use public transportation when possible",
                    "📱 Keep your phone charged and accessible",
                    "🏢 Prefer office locations over private residences"
                )
            ),
            SecurityTip(
                title = "Red Flags to Watch",
                description = "Warning signs of potentially unsafe opportunities",
                icon = Icons.Default.Warning,
                tips = listOf(
                    "💰 Jobs promising unusually high pay for simple work",
                    "💳 Requests for upfront payments or fees",
                    "🏠 Insistence on meeting at private residences",
                    "⚡ Pressure to make immediate decisions",
                    "❓ Vague job descriptions or requirements",
                    "🚫 Unwillingness to provide company information"
                )
            ),
            SecurityTip(
                title = "Account Security",
                description = "Keep your Quick PartTimes account secure",
                icon = Icons.Default.Security,
                tips = listOf(
                    "🔐 Use a strong, unique password",
                    "📱 Enable two-factor authentication",
                    "🚪 Log out from shared devices",
                    "🔄 Update the app regularly",
                    "👀 Monitor your account activity",
                    "📧 Verify all email communications from us"
                )
            )
        )
    }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "Security & Safety", navController = navController)
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
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                        // Removed bottom padding to prevent white space
                    )
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
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your Safety Matters",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Learn how to stay safe while job hunting",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Security Sections
                securitySections.forEach { section ->
                    SecurityTipCard(
                        securityTip = section,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Emergency Contact Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Emergency Situations",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "If you feel unsafe or encounter suspicious activity, contact local authorities immediately and report to us at safety@quickparttimes.com",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityTipCard(
    securityTip: SecurityTip,
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
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = securityTip.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = securityTip.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = securityTip.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            securityTip.tips.forEach { tip ->
                Text(
                    text = tip,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}
