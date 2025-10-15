package com.example.dutype.worker.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.navigation.Routes
import kotlinx.coroutines.launch

/**
 * Enhanced onboarding screen with awesome animations and auto-scrolling
 * Guides users through initial setup with swipe gestures and smooth transitions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerOnboardingScreen(
    navController: NavController
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll functionality removed - users can manually swipe through pages
    
    // Background gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667EEA),
                        Color(0xFF764BA2),
                        Color(0xFFF093FB)
                    )
                )
            )
    ) {
        // Floating background elements
        FloatingBackgroundElements()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        val isActive = pagerState.currentPage == index
                        val scale by animateFloatAsState(
                            targetValue = if (isActive) 1.2f else 1f,
                            animationSpec = spring(dampingRatio = 0.6f),
                            label = "dot_scale"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(if (isActive) 12.dp else 8.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
                
                // Skip button removed - profile setup is mandatory
                // TextButton(
                //     onClick = {
                //         navController.navigate(Routes.WORKER_HOME) {
                //             popUpTo(Routes.WORKER_ONBOARDING) { inclusive = true }
                //         }
                //     }
                // ) {
                //     Text(
                //         text = "Skip",
                //         color = Color.White,
                //         fontWeight = FontWeight.Medium
                //     )
                // }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Horizontal pager with swipe gestures
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 0.dp),
                pageSpacing = 0.dp
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    when (page) {
                        0 -> WelcomeStep()
                        1 -> ProfileSetupStep()
                        2 -> ApplicationFormStep(navController = navController)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Bottom navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color.White.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                // Next/Get Started button
                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            navController.navigate(Routes.PROFILE_SETUP) {
                                popUpTo(Routes.WORKER_ONBOARDING) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF667EEA)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < 2) "Next" else "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingBackgroundElements() {
    // Floating circles with animations
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    
    val float1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )
    
    val float2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )
    
    val float3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float3"
    )
    
    // Floating circle 1
    Box(
        modifier = Modifier
            .offset(
                x = (50 + float1 * 20).dp,
                y = (100 + float1 * 30).dp
            )
            .size(80.dp)
            .background(
                Color.White.copy(alpha = 0.1f),
                CircleShape
            )
    )
    
    // Floating circle 2
    Box(
        modifier = Modifier
            .offset(
                x = (300 + float2 * -40).dp,
                y = (200 + float2 * 20).dp
            )
            .size(120.dp)
            .background(
                Color.White.copy(alpha = 0.08f),
                CircleShape
            )
    )
    
    // Floating circle 3
    Box(
        modifier = Modifier
            .offset(
                x = (150 + float3 * 30).dp,
                y = (400 + float3 * -25).dp
            )
            .size(60.dp)
            .background(
                Color.White.copy(alpha = 0.12f),
                CircleShape
            )
    )
}

@Composable
private fun WelcomeStep() {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Animated welcome illustration
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = 0.6f),
                initialScale = 0.5f
            ) + fadeIn(animationSpec = tween(1000))
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEmotions,
                    contentDescription = "Welcome",
                    modifier = Modifier.size(80.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
        ) {
            Text(
                text = "Welcome to Dutype!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 400))
        ) {
            Text(
                text = "Let's set up your profile to help you find the perfect job opportunities.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileSetupStep() {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Animated profile setup illustration
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = 0.6f),
                initialScale = 0.5f
            ) + fadeIn(animationSpec = tween(1000))
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Setup",
                    modifier = Modifier.size(80.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
        ) {
            Text(
                text = "Complete Your Profile",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 400))
        ) {
            Text(
                text = "We'll help you create a professional profile that stands out to employers.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Feature highlights
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 20 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 600))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureHighlight(
                    icon = Icons.Default.Star,
                    text = "Professional Profile"
                )
                FeatureHighlight(
                    icon = Icons.Default.Work,
                    text = "Work Experience"
                )
                FeatureHighlight(
                    icon = Icons.Default.School,
                    text = "Skills & Education"
                )
            }
        }
    }
}

@Composable
private fun ApplicationFormStep(
    navController: NavController
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Animated application form illustration
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = 0.6f),
                initialScale = 0.5f
            ) + fadeIn(animationSpec = tween(1000))
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = "Application Form",
                    modifier = Modifier.size(80.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
        ) {
            Text(
                text = "Fill Application Form",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 400))
        ) {
            Text(
                text = "Complete your application form with your skills, experience, and preferences. This will help us match you with the right opportunities.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Form features
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 20 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 600))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureHighlight(
                    icon = Icons.Default.Person,
                    text = "Personal Information"
                )
                FeatureHighlight(
                    icon = Icons.Default.Work,
                    text = "Work Experience"
                )
                FeatureHighlight(
                    icon = Icons.Default.Star,
                    text = "Skills & Preferences"
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlight(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp
            )
        )
    }
}
