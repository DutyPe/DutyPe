package com.example.dutype.onboarding

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
 * General onboarding screen for first-time users
 * Shows app introduction and navigates to role selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    navController: NavController
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    
    // Background gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A), // Deep blue
                        Color(0xFF3B82F6), // Blue
                        Color(0xFF06B6D4), // Cyan
                        Color(0xFF10B981)  // Green
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
            // Progress dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 40.dp)
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
            
            Spacer(modifier = Modifier.height(60.dp))
            
            // Horizontal pager with onboarding content
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
                        0 -> WelcomePage()
                        1 -> FeaturesPage()
                        2 -> GetStartedPage()
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
                            navController.navigate(Routes.SELECT_ROLE) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1E3A8A)
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
    
    // Floating circles
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
}

@Composable
private fun WelcomePage() {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Welcome illustration
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = 0.6f),
                initialScale = 0.5f
            ) + fadeIn(animationSpec = tween(1000))
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Welcome",
                    modifier = Modifier.size(100.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(60.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
        ) {
            Text(
                text = "Welcome to\nDutyPe",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 36.sp
                ),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(30.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 400))
        ) {
            Text(
                text = "Your gateway to finding the perfect job opportunities and connecting with local businesses.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeaturesPage() {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = 0.6f),
                initialScale = 0.5f
            ) + fadeIn(animationSpec = tween(1000))
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = "Features",
                    modifier = Modifier.size(100.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(60.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
        ) {
            Text(
                text = "Features",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 36.sp
                ),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Feature list
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            listOf(
                "🎯 Find local job opportunities",
                "👥 Connect with employers",
                "📱 Easy application process",
                "📍 Location-based search"
            ).forEachIndexed { index, feature ->
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInHorizontally(
                        initialOffsetX = { -50 },
                        animationSpec = spring(dampingRatio = 0.8f)
                    ) + fadeIn(animationSpec = tween(800, delayMillis = 600 + index * 200))
                ) {
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun GetStartedPage() {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = 0.6f),
                initialScale = 0.5f
            ) + fadeIn(animationSpec = tween(1000))
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = "Get Started",
                    modifier = Modifier.size(100.dp),
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(60.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
        ) {
            Text(
                text = "Ready to Start?",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 36.sp
                ),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(30.dp))
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 30 },
                animationSpec = spring(dampingRatio = 0.8f)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 400))
        ) {
            Text(
                text = "Choose your role and let's get you started on your journey to finding the perfect job or worker.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
