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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dutype.app.BuildConfig
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors

@Composable
fun WorkerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        CommonHeader(
            title = "About Us",
            navController = navController,
            backgroundColor = WorkerColors.CardBackground
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Welcome Card
            AboutSectionCard(
                title = "Welcome to DutyPe",
                content = "Your gateway to local job opportunities! DutyPe connects you with employers looking for reliable workers like you. Whether you're seeking part-time work, gig jobs, or full-time employment, we've got you covered."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Mission Card
            AboutSectionCard(
                title = "Our Mission",
                content = "To empower workers by providing easy access to local job opportunities. We believe everyone deserves a chance to earn, grow, and succeed — without complicated applications or lengthy processes."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Vision Card
            AboutSectionCard(
                title = "Our Vision",
                content = "To become India's most trusted platform for local employment, where every worker can find meaningful work that fits their skills, schedule, and location preferences."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Key Features Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Key Features",
                        style = AppTypography.sectionHeader.copy(
                            color = Color(0xFF1F2937)
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FeatureList(
                        features = listOf(
                            "Quick one-tap job applications",
                            "Jobs near your location",
                            "Real-time job notifications",
                            "Save jobs for later",
                            "Track your applications",
                            "Build your work profile"
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Job Categories Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Job Categories",
                        style = AppTypography.sectionHeader.copy(
                            color = Color(0xFF1F2937)
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FeatureList(
                        features = listOf(
                            "Delivery & Logistics",
                            "Food Service & Cooking",
                            "Housekeeping & Cleaning",
                            "Shop & Retail Help",
                            "Childcare & Eldercare",
                            "Maintenance & Repairs"
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Why Choose DutyPe Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Why Choose DutyPe?",
                        style = AppTypography.sectionHeader.copy(
                            color = Color(0xFF1F2937)
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FeatureList(
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
private fun AboutSectionCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = AppTypography.sectionHeader.copy(
                    color = Color(0xFF1F2937)
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
private fun FeatureList(features: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        features.forEach {
            FeatureItem(text = it)
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF1F2937) // Black color instead of green
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
