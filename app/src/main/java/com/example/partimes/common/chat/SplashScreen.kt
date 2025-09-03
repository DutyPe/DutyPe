package com.example.partimes.common.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import com.example.partimes.R

@Composable
fun SplashScreen(navController: NavController) {
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var pulseEffect by remember { mutableStateOf(false) }
    var exitAnimation by remember { mutableStateOf(false) }

    // Logo animations
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val logoRotation by animateFloatAsState(
        targetValue = if (logoVisible) 0f else -180f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "logo_rotation"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (exitAnimation) 0f else 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    // Text animations
    val titleAlpha by animateFloatAsState(
        targetValue = if (textVisible && !exitAnimation) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "title_alpha"
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (subtitleVisible && !exitAnimation) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "subtitle_alpha"
    )

    val titleOffset by animateFloatAsState(
        targetValue = if (textVisible) 0f else 50f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "title_offset"
    )

    val subtitleOffset by animateFloatAsState(
        targetValue = if (subtitleVisible) 0f else 30f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "subtitle_offset"
    )

    // Pulse effect for logo
    val pulseScale by animateFloatAsState(
        targetValue = if (pulseEffect) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Background gradient with more vibrant colors
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF6200EE),
            Color(0xFF3700B3),
            Color(0xFF1A0033)
        )
    )

    // Floating elements animation
    val floatingOffset1 by animateFloatAsState(
        targetValue = if (logoVisible) 0f else -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating1"
    )

    val floatingOffset2 by animateFloatAsState(
        targetValue = if (logoVisible) 0f else 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating2"
    )

    // Animation sequence
    LaunchedEffect(Unit) {
        delay(300)
        logoVisible = true
        delay(800)
        textVisible = true
        delay(400)
        subtitleVisible = true
        delay(200)
        pulseEffect = true
        delay(1500)
        exitAnimation = true
        delay(800)
        navController.navigate("login_bottom_sheet") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        // Floating background elements
        Box(
            modifier = Modifier
                .offset(x = (-150).dp, y = floatingOffset1.dp)
                .size(100.dp)
                .alpha(0.1f)
                .clip(CircleShape)
                .background(Color.White)
                .blur(radius = 2.dp)
        )

        Box(
            modifier = Modifier
                .offset(x = 200.dp, y = floatingOffset2.dp)
                .size(80.dp)
                .alpha(0.08f)
                .clip(CircleShape)
                .background(Color.White)
                .blur(radius = 3.dp)
        )

        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = (-floatingOffset1 * 0.5f).dp)
                .size(60.dp)
                .alpha(0.06f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .rotate(45f)
                .blur(radius = 1.dp)
        )

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Enhanced logo container with glow effect
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                // Glow effect background
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(pulseScale)
                        .alpha(0.3f)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.Transparent
                                ),
                                radius = 200f
                            )
                        )
                )

                // Logo with enhanced styling
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                        .shadow(20.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.parttimes),
                        contentDescription = "ParTimes Logo",
                        modifier = Modifier
                            .size(140.dp)
                            .scale(logoScale)
                            .rotate(logoRotation)
                            .alpha(logoAlpha)
                    )
                }
            }

            // App name with enhanced typography
            Text(
                text = "ParTimes",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = titleOffset.dp)
                    .alpha(titleAlpha),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline with elegant styling
            Text(
                text = "Find Your Perfect Part-Time Job",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = subtitleOffset.dp)
                    .alpha(subtitleAlpha),
                letterSpacing = 0.5.sp,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Additional subtitle
            Text(
                text = "Connect • Work • Earn",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(subtitleAlpha * 0.8f),
                letterSpacing = 1.sp
            )
        }

        // Loading indicator at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .alpha(if (pulseEffect) titleAlpha else 0f)
        ) {
            // Animated dots loading indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    val dotAlpha by animateFloatAsState(
                        targetValue = if (pulseEffect) 1f else 0.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = dotAlpha))
                    )
                }
            }
        }

        // Version text
        Text(
            text = "v1.0.0",
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .alpha(subtitleAlpha * 0.7f)
        )
    }
}