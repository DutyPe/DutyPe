package com.example.dutype.common.chat.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
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
        // Common header - used across all help screens
        CommonHeader(
            title = "Security & Safety",
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
                text = "Your Safety Matters",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
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
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Security Sections - Clean format
            securitySections.forEachIndexed { index, section ->
                Text(
                    text = "${index + 1}. ${section.title.uppercase()}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 18.sp,
                        lineHeight = 24.sp
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
                    modifier = Modifier.padding(bottom = 12.dp)
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
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Emergency Information
            Text(
                text = "Emergency Situations",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
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

