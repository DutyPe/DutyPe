package com.example.partimes.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ReusableAboutUs(
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    headerTitle: String = "About Us",
    backgroundColor: Color = Color.White,
    gradientColors: List<Color> = listOf(
        Color.White,
        Color.White,
        Color.White
    ),
    cardBackgroundColor: Color = Color.White,
    primaryTextColor: Color = Color(0xFF1E293B),
    secondaryTextColor: Color = Color(0xFF64748B),
    accentColor: Color = Color(0xFF3B82F6),
    showAnimation: Boolean = true
) {
    var isVisible by remember { mutableStateOf(!showAnimation) }

    LaunchedEffect(showAnimation) {
        if (showAnimation) {
            delay(100)
            isVisible = true
        }
    }

    val aboutData = remember {
        AboutUsData(
            title = "About Quick PartTimes",
            description = "Quick PartTimes is built to simplify local job hunting for everyone — especially those who want quick, short-term, or part-time work near them.",
            mission = "Our mission is to connect workers with local opportunities like delivery, cooking, shop help, cleaning, and more — without resumes or complicated processes.",
            vision = "We believe everyone deserves easy access to work that fits their lifestyle. That's why our platform is fast, simple, voice-enabled, and trusted by local employers.",
            tagline = "Whether you're a student, homemaker, part-timer, or looking to earn extra income — Quick PartTimes is made for you!",
            keyFeatures = listOf(
                "🚀 Quick job applications",
                "📍 Location-based matching", 
                "🎤 Voice-enabled interface",
                "💼 Diverse job categories",
                "⚡ Real-time notifications",
                "🔒 Secure and trusted platform"
            ),
            values = listOf(
                "🤝 Community-focused approach",
                "💯 Transparency in all dealings",
                "🌟 Quality job opportunities",
                "📱 User-friendly experience",
                "🎯 Results-driven solutions",
                "🔄 Continuous innovation"
            ),
            footerText = "Made with ❤️ in India",
            version = "Version 1.0.0"
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Main About Section - Flat Design
            AboutSection(
                data = aboutData,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Features Section - Flat Design
            FeaturesSection(
                title = "Key Features",
                icon = Icons.Default.Star,
                features = aboutData.keyFeatures,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Values Section - Flat Design
            ValuesSection(
                title = "Our Values",
                icon = Icons.Default.Favorite,
                values = aboutData.values,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Footer Section - Flat Design
            FooterSection(
                footerText = aboutData.footerText,
                version = aboutData.version,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AboutSection(
    data: AboutUsData,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    accentColor: Color
) {
    Column {
        // Title with Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = data.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )
        }
        
        // Description
        Text(
            text = data.description,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = secondaryTextColor,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Mission
        Text(
            text = data.mission,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = secondaryTextColor,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Vision
        Text(
            text = data.vision,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = secondaryTextColor,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Tagline
        Text(
            text = data.tagline,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp,
            color = accentColor
        )
    }
}

@Composable
private fun FeaturesSection(
    title: String,
    icon: ImageVector,
    features: List<String>,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Column {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryTextColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )
        }
        
        // Features List
        features.forEach { feature ->
            Text(
                text = feature,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = secondaryTextColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun ValuesSection(
    title: String,
    icon: ImageVector,
    values: List<String>,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Column {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryTextColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )
        }
        
        // Values List
        values.forEach { value ->
            Text(
                text = value,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = secondaryTextColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun FooterSection(
    footerText: String,
    version: String,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = footerText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = secondaryTextColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = version,
            fontSize = 14.sp,
            color = primaryTextColor,
            textAlign = TextAlign.Center
        )
    }
}

// Data class for About Us content
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
