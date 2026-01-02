package com.example.dutype.worker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dutype.app.BuildConfig
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography

// Worker theme colors
private val WorkerPrimaryGreen = Color(0xFF059669)
private val WorkerSecondaryGreen = Color(0xFF10B981)
private val WorkerLightGreen = Color(0xFFD1FAE5)

/**
 * Data class for About Us screen content
 */
data class AboutUsData(
    val title: String,
    val description: String,
    val mission: String,
    val vision: String,
    val tagline: String,
    val keyFeatures: List<String>,
    val values: List<String>,
    val footerText: String,
    val version: String
)

@Composable
fun WorkerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)
    
    // Clean white background for professional look
    val backgroundColor = Color(0xFFF8FAFC)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Common header - used across all info screens
        CommonHeader(
            title = "About Us",
            navController = navController
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Welcome Card - Worker focused
            WorkerAboutSectionCard(
                title = "Welcome to DutyPe",
                content = "Your gateway to local job opportunities! DutyPe connects you with employers looking for reliable workers like you. Whether you're seeking part-time work, gig jobs, or full-time employment, we've got you covered."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Mission Card - Worker focused
            WorkerAboutSectionCard(
                title = "Our Mission",
                content = "To empower workers by providing easy access to local job opportunities. We believe everyone deserves a chance to earn, grow, and succeed — without complicated applications or lengthy processes."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Vision Card - Worker focused
            WorkerAboutSectionCard(
                title = "Our Vision",
                content = "To become India's most trusted platform for local employment, where every worker can find meaningful work that fits their skills, schedule, and location preferences."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Worker-Specific Features Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Key Features for Workers",
                        style = AppTypography.sectionHeader.copy(
                            color = WorkerSecondaryGreen
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    WorkerFeatureList(
                        features = listOf(
                            "Quick one-tap job applications",
                            "Jobs near your location",
                            "Voice-enabled job search",
                            "Real-time job notifications",
                            "Save jobs for later",
                            "Track your applications",
                            "Build your work profile",
                            "Earn trust badges"
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Job Categories Card - Worker specific
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Job Categories",
                        style = AppTypography.sectionHeader.copy(
                            color = WorkerSecondaryGreen
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    WorkerFeatureList(
                        features = listOf(
                            "Delivery & Logistics",
                            "Food Service & Cooking",
                            "Housekeeping & Cleaning",
                            "Shop & Retail Help",
                            "Childcare & Eldercare",
                            "Maintenance & Repairs",
                            "Event & Catering Staff",
                            "And many more..."
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Why Choose DutyPe Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Why Choose DutyPe?",
                        style = AppTypography.sectionHeader.copy(
                            color = WorkerSecondaryGreen
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    WorkerFeatureList(
                        features = listOf(
                            "No resume required",
                            "Verified employers",
                            "Transparent pay information",
                            "Flexible work options",
                            "Safe & secure platform"
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Footer
            Text(
                text = "Made with ❤️ in India",
                style = AppTypography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = AppTypography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun WorkerAboutSectionCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = AppTypography.sectionHeader.copy(
                    color = WorkerSecondaryGreen
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = content,
                style = AppTypography.bodyMedium.copy(
                    color = Color(0xFF4B5563)
                )
            )
        }
    }
}

@Composable
private fun WorkerFeatureList(features: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        features.forEach {
            WorkerFeatureItem(text = it)
        }
    }
}

@Composable
private fun WorkerFeatureItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = WorkerSecondaryGreen
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = AppTypography.bodyMedium.copy(
                color = Color(0xFF4B5563)
            )
        )
    }
}
