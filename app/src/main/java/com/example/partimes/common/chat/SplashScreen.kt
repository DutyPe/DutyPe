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
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(navController: NavController) {
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    var pulseEffect by remember { mutableStateOf(false) }
    var exitAnimation by remember { mutableStateOf(false) }
    var backgroundParticles by remember { mutableStateOf(false) }

    // Enhanced logo animations with more sophisticated physics
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.2f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "logo_scale"
    )

    val logoRotation by animateFloatAsState(
        targetValue = if (logoVisible) 0f else 360f,
        animationSpec = tween(1800, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)),
        label = "logo_rotation"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (exitAnimation) 0f else 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    // Enhanced text animations with staggered effects
    val titleAlpha by animateFloatAsState(
        targetValue = if (textVisible && !exitAnimation) 1f else 0f,
        animationSpec = tween(1000, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "title_alpha"
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (subtitleVisible && !exitAnimation) 1f else 0f,
        animationSpec = tween(800, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "subtitle_alpha"
    )

    val titleOffset by animateFloatAsState(
        targetValue = if (textVisible) 0f else 80f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "title_offset"
    )

    val subtitleOffset by animateFloatAsState(
        targetValue = if (subtitleVisible) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "subtitle_offset"
    )

    // Enhanced pulse effect with more dynamic scaling
    val pulseScale by animateFloatAsState(
        targetValue = if (pulseEffect) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Professional premium gradient with enhanced depth - AWESOME COLORS
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A0A23), // Deep midnight blue
            Color(0xFF1A1A3E), // Rich dark purple
            Color(0xFF2D1B69), // Royal purple
            Color(0xFF4C1D95), // Vibrant purple
            Color(0xFF7C2D92), // Magenta purple
            Color(0xFF9333EA)  // Electric purple
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )

    // Enhanced floating particles with varied sizes and speeds
    val particleOffsets = remember {
        List(6) { index ->
            Animatable((-100f + index * 50f))
        }
    }

    // Particle animations
    LaunchedEffect(backgroundParticles) {
        if (backgroundParticles) {
            particleOffsets.forEachIndexed { index, animatable ->
                launch {
                    animatable.animateTo(
                        targetValue = 100f - index * 30f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 4000 + index * 1000,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                }
            }
        }
    }

    // Sophisticated animation sequence
    LaunchedEffect(Unit) {
        delay(200)
        logoVisible = true
        delay(600)
        backgroundParticles = true
        delay(400)
        textVisible = true
        delay(500)
        subtitleVisible = true
        delay(300)
        pulseEffect = true
        delay(2500) // Extended viewing time
        exitAnimation = true
        delay(800)
        navController.navigate(com.example.partimes.navigation.Routes.SELECT_ROLE) {
            popUpTo(com.example.partimes.navigation.Routes.SPLASH) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        // Enhanced floating background particles
        particleOffsets.forEachIndexed { index, offset ->
            val size = (60 + index * 15).dp
            val alpha = 0.03f + (index * 0.01f)
            val xOffset = (-150 + index * 60).dp

            Box(
                modifier = Modifier
                    .offset(x = xOffset, y = offset.value.dp)
                    .size(size)
                    .alpha(alpha)
                    .clip(if (index % 2 == 0) CircleShape else RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3B82F6).copy(alpha = 0.4f),
                                Color(0xFF6366F1).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                    .rotate(if (index % 2 == 0) 0f else 45f)
                    .blur(radius = (2 + index).dp)
            )
        }

        // Premium brand elements
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Enhanced logo section with premium effects
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                // Sophisticated multi-layer glow system
                repeat(4) { layer ->
                    Box(
                        modifier = Modifier
                            .size((320 - layer * 20).dp)
                            .scale(pulseScale * (1f - layer * 0.05f))
                            .alpha(0.1f - layer * 0.02f)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6).copy(alpha = 0.6f - layer * 0.1f),
                                        Color(0xFF6366F1).copy(alpha = 0.4f - layer * 0.08f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.2f - layer * 0.04f),
                                        Color.Transparent
                                    ),
                                    radius = (400f - layer * 50f)
                                )
                            )
                    )
                }

                // Premium logo container with enhanced styling
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent
                                )
                            )
                        )
                        .shadow(
                            elevation = 32.dp,
                            shape = CircleShape,
                            spotColor = Color(0xFF3B82F6).copy(alpha = 0.4f),
                            ambientColor = Color.Black.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.parttimes),
                        contentDescription = "DutyPe Logo",
                        modifier = Modifier
                            .size(180.dp)
                            .scale(logoScale)
                            .rotate(logoRotation)
                            .alpha(logoAlpha)
                    )
                }
            }

            // Premium brand name with sophisticated typography
            Text(
                text = "DutyPe",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = titleOffset.dp)
                    .alpha(titleAlpha)
                    .shadow(
                        elevation = 12.dp,
                        spotColor = Color(0xFF3B82F6).copy(alpha = 0.6f),
                        ambientColor = Color.Black.copy(alpha = 0.3f)
                    ),
                letterSpacing = 3.2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced tagline with premium styling
            Text(
                text = "Empowering Part-Time Excellence",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = subtitleOffset.dp)
                    .alpha(subtitleAlpha),
                letterSpacing = 1.2.sp,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Professional mission statement
            Text(
                text = "Connect • Excel • Thrive",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(subtitleAlpha * 0.9f)
                    .shadow(
                        elevation = 4.dp,
                        spotColor = Color(0xFF1E293B).copy(alpha = 0.3f)
                    ),
                letterSpacing = 1.8.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

        }

        // Enhanced loading indicator with premium design
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .alpha(if (pulseEffect) titleAlpha else 0f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    val dotAlpha by animateFloatAsState(
                        targetValue = if (pulseEffect) 1f else 0.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    val dotScale by animateFloatAsState(
                        targetValue = if (pulseEffect) 1.2f else 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_scale_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6).copy(alpha = dotAlpha),
                                        Color(0xFF6366F1).copy(alpha = dotAlpha * 0.7f),
                                        Color(0xFF8B5CF6).copy(alpha = dotAlpha * 0.4f)
                                    )
                                )
                            )
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                spotColor = Color(0xFF3B82F6).copy(alpha = dotAlpha * 0.6f)
                            )
                    )
                }
            }
        }

        // Professional status information
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(subtitleAlpha * 0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1E293B).copy(alpha = 0.6f),
                            Color(0xFF334155).copy(alpha = 0.4f)
                        )
                    )
                )
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Version 2.0.0 • Enterprise Edition",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE2E8F0),
                letterSpacing = 0.5.sp
            )
        }
    }
}