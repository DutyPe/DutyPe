package com.example.partimes.common.chat.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color(0xFF2193b0)) // Sky blue theme color
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    Scaffold(
        topBar = {
            // Custom transparent top bar that blends with gradient
            TopAppBar(
                title = {
                    Text(
                        text = "About Us",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2193b0), // Clean sky blue
                                Color(0xFF6dd5ed), // Soft light blue
                                Color(0xFFFFFFFF)  // Pure white
                            )
                        )
                    )
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                        // Removed bottom padding to prevent white space
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Main About Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFFFFF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "About Quick PartTimes",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF374151)
                            )
                        }

                        Text(
                            text = "Quick PartTimes is built to simplify local job hunting for everyone — especially those who want quick, short-term, or part-time work near them.",
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "Our mission is to connect job seekers with local opportunities like delivery, cooking, shop help, cleaning, and more — without resumes or complicated processes.",
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "We believe everyone deserves easy access to work that fits their lifestyle. That's why our platform is fast, simple, voice-enabled, and trusted by local employers.",
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "Whether you're a student, homemaker, part-timer, or looking to earn extra income — Quick PartTimes is made for you!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Features Card
                FeatureCard(
                    title = "Key Features",
                    icon = Icons.Default.Star,
                    features = listOf(
                        "🚀 Quick job applications",
                        "📍 Location-based matching",
                        "🎤 Voice-enabled interface",
                        "💼 Diverse job categories",
                        "⚡ Real-time notifications",
                        "🔒 Secure and trusted platform"
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Values Card
                FeatureCard(
                    title = "Our Values",
                    icon = Icons.Default.Favorite,
                    features = listOf(
                        "🤝 Community-focused approach",
                        "💯 Transparency in all dealings",
                        "🌟 Quality job opportunities",
                        "📱 User-friendly experience",
                        "🎯 Results-driven solutions",
                        "🔄 Continuous innovation"
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Footer
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Made with ❤️ in India",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Version 1.0.0",
                            fontSize = 14.sp,
                            color = Color(0xFF374151)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BackNavigationTopBar(title: String, navController: NavController) {
    TODO("Not yet implemented")
}

@Composable
private fun FeatureCard(
    title: String,
    icon: ImageVector,
    features: List<String>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
            }

            features.forEach { feature ->
                Text(
                    text = feature,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

annotation class AboutUsScreen
