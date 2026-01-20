package com.example.dutype.employer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dutype.app.BuildConfig
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors

// Employer theme colors - Meesho style
private val EmployerPrimaryBlue = WorkerColors.Info
private val EmployerSecondaryBlue = Color(0xFF3B82F6)
private val EmployerLightBlue = WorkerColors.InfoLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(WorkerColors.CardBackground)

    // Clean Meesho-style background
    val backgroundColor = WorkerColors.ScreenBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Common Header component
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
            // Welcome Card - Employer focused
            AboutSectionCard(
                title = "Welcome to DutyPe",
                content = "Your all-in-one solution for finding the best local talent, right when you need it. We connect businesses with a pool of qualified, on-demand workers for gig-based and full-time roles."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Mission Card - Employer focused
            AboutSectionCard(
                title = "Our Mission",
                content = "To empower businesses by providing a seamless, efficient, and reliable platform to connect with a flexible workforce. We aim to simplify the hiring process, so you can focus on growing your business."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Vision Card - Employer focused
            AboutSectionCard(
                title = "Our Vision",
                content = "To become the leading platform for on-demand employment in India, creating a dynamic ecosystem where businesses can thrive with the right talent and workers can find meaningful opportunities."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Employer-Specific Features Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Key Features for Employers",
                        style = AppTypography.sectionHeader.copy(
                            color = EmployerSecondaryBlue
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FeatureList(
                        features = listOf(
                            "Post jobs in minutes",
                            "Access a large talent pool",
                            "GPS-based attendance tracking",
                            "Verified worker profiles",
                            "Flexible hiring options",
                            "Real-time application alerts",
                            "Manage multiple job postings",
                            "Track worker performance"
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Hiring Categories Card - Employer specific
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Hire For Any Role",
                        style = AppTypography.sectionHeader.copy(
                            color = EmployerSecondaryBlue
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FeatureList(
                        features = listOf(
                            "Delivery Personnel",
                            "Kitchen & Cooking Staff",
                            "Housekeeping & Cleaning",
                            "Shop Assistants & Retail",
                            "Childcare & Eldercare",
                            "Maintenance Workers",
                            "Event & Catering Staff",
                            "And many more..."
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Why Choose DutyPe Card - Employer focused
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
                            color = EmployerSecondaryBlue
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FeatureList(
                        features = listOf(
                            "Quick hiring process",
                            "Verified worker database",
                            "Cost-effective solutions",
                            "24/7 platform access",
                            "Dedicated support team"
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Values Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Our Core Values",
                        style = AppTypography.sectionHeader.copy(
                            color = EmployerSecondaryBlue
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FeatureList(
                        features = listOf(
                            "Efficiency in hiring",
                            "Reliability you can trust",
                            "Transparency in all dealings",
                            "Empowerment for businesses"
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Footer
            Text(
                text = "Made with 💙 in India",
                style = AppTypography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
            
            // Version info
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = AppTypography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = AppTypography.sectionHeader.copy(
                    color = EmployerSecondaryBlue
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
            tint = EmployerSecondaryBlue
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

@Preview(showBackground = true)
@Composable
fun EmployerAboutScreenPreview() {
    EmployerAboutScreen(
        navController = rememberNavController(),
        onStatusBarColorChange = {}
    )
}
