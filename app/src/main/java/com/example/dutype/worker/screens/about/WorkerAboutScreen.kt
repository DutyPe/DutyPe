package com.example.dutype.worker.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.ui.components.AboutUsData

@Composable
fun WorkerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Clean Header - matching the image style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = "About Us",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 20.sp
                    )
                )
            }
        }
        
        // Content - Using data from existing About Us screen but in clean format
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Get the actual data from the existing About Us screen
            val aboutData = remember {
                AboutUsData(
                    title = "About DutyPe",
                    description = "DutyPe is built to simplify local job hunting for everyone — especially those who want quick, short-term, or part-time work near them.",
                    mission = "Our mission is to connect workers with local opportunities like delivery, cooking, shop help, cleaning, and more — without resumes or complicated processes.",
                    vision = "We believe everyone deserves easy access to work that fits their lifestyle. That's why our platform is fast, simple, voice-enabled, and trusted by local employers.",
                    tagline = "Whether you're a student, homemaker, part-timer, or looking to earn extra income — DutyPe is made for you!",
                    keyFeatures = listOf(
                        "Quick job applications",
                        "Location-based matching", 
                        "Voice-enabled interface",
                        "Diverse job categories",
                        "Real-time notifications",
                        "Secure and trusted platform"
                    ),
                    values = listOf(
                        "Community-focused approach",
                        "Transparency in all dealings",
                        "Quality job opportunities",
                        "User-friendly experience",
                        "Results-driven solutions",
                        "Continuous innovation"
                    ),
                    footerText = "Made with ❤️ in India",
                    version = "Version 1.0.0"
                )
            }
            
            // Section 1: Overview
            Text(
                text = "1. COMPANY OVERVIEW ",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            
            Text(
                text = aboutData.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = aboutData.tagline,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Section 2: Mission
            Text(
                text = "2. MISSION",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = aboutData.mission,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Section 3: Vision & Values
            Text(
                text = "3. VISION",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = aboutData.vision,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Section 4: Key Features
            Text(
                text = "4. KEY FEATURES",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            aboutData.keyFeatures.forEach { feature ->
                Text(
                    text = "• $feature",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Black,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Footer
            Text(
                text = aboutData.footerText.replace("❤️", "").trim() + " in India",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = aboutData.version,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Black,
                    fontSize = 12.sp
                ),
                modifier = Modifier.padding(bottom = 40.dp)
            )
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
