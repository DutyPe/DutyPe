package com.example.dutype.employer.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

// Employer theme colors
private val EmployerPrimaryBlue = Color(0xFF1E3A8A)
private val EmployerSecondaryBlue = Color(0xFF3B82F6)
private val EmployerLightBlue = Color(0xFFE0F2FE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(EmployerPrimaryBlue)

    // Employer gradient background
    val employerGradient = Brush.verticalGradient(
        colors = listOf(
            EmployerPrimaryBlue,
            EmployerSecondaryBlue,
            EmployerLightBlue,
            Color.White
        ),
        startY = 0f,
        endY = 1200f
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = employerGradient)
    ) {
        // Custom Top Bar with employer theme
        TopAppBar(
            title = {
                Text(
                    text = "About Us",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
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
                content = "Your all-in-one solution for finding the best local talent, right when you need it. We connect businesses with a pool of qualified, on-demand workers for gig-based and full-time roles."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Mission Card
            AboutSectionCard(
                title = "Our Mission",
                content = "To empower businesses by providing a seamless, efficient, and reliable platform to connect with a flexible workforce. We aim to simplify the hiring process, so you can focus on growing your business."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Vision Card
            AboutSectionCard(
                title = "Our Vision",
                content = "To become the leading platform for on-demand employment in India, creating a dynamic ecosystem where businesses can thrive with the right talent and workers can find meaningful opportunities."
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Features Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Key Features for Employers",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmployerSecondaryBlue
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FeatureList(
                        features = listOf(
                            "Post jobs in minutes",
                            "Access a large talent pool",
                            "GPS-based attendance",
                            "Verified worker profiles",
                            "Flexible hiring options"
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
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmployerSecondaryBlue
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FeatureList(
                        features = listOf(
                            "Efficiency",
                            "Reliability",
                            "Transparency",
                            "Empowerment"
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Version info
            Text(
                text = "Version 1.0.5",
                style = MaterialTheme.typography.bodySmall.copy(
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
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmployerSecondaryBlue
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF4B5563),
                    lineHeight = 22.sp
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
            style = MaterialTheme.typography.bodyMedium.copy(
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
