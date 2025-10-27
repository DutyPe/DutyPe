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
    onStatusBarColorChange(Color.White)

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
                description = "Keep your DutyPe account secure",
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
                text = "Security & Safety",
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
                text = "Your Safety Matters",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Learn how to stay safe while job hunting. Follow these guidelines to protect yourself and your personal information.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Security Sections - Clean format
            securitySections.forEachIndexed { index, section ->
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
                    text = section.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Black,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                section.tips.forEach { tip ->
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.Black,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Emergency Information
            Text(
                text = "Emergency Situations",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "If you feel unsafe or encounter suspicious activity, contact local authorities immediately and report to us at dutypein@gmail.com",
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

