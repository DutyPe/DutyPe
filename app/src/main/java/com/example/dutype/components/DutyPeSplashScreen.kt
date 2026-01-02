package com.example.dutype.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Premium DutyPe Splash Screen with Text Logo
 * Uses "DutyPe" text style consistent with WorkerHomeScreen
 */
@Composable
fun DutyPeSplashScreen(
    navController: NavController,
    onSplashComplete: () -> Unit = {},
    duration: Long = 2500L
) {
    // Animation states
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val ringScale = remember { Animatable(0.8f) }
    val ringAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Infinite animations for glow effects
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    // Particle positions for floating effect
    val particles = remember {
        List(15) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 6f + 2f,
                speed = Random.nextFloat() * 0.3f + 0.1f,
                alpha = Random.nextFloat() * 0.5f + 0.2f
            )
        }
    }

    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_offset"
    )

    // Launch animations
    LaunchedEffect(Unit) {
        // Logo scale animation
        scope.launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = EaseOutBack)
            )
        }
        
        // Logo alpha animation
        scope.launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }
        
        // Ring animations
        scope.launch {
            ringAlpha.animateTo(
                targetValue = 0.6f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
        }
        scope.launch {
            ringScale.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(1000, easing = EaseInOutCubic)
            )
        }
        
        // Tagline animation (delayed)
        scope.launch {
            delay(400)
            taglineAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }

        // Wait for splash duration then navigate
        delay(duration)
        onSplashComplete()
    }

    // Premium gradient background
    val gradientColors = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF16213E),
        Color(0xFF0F3460),
        Color(0xFF1A1A2E)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(gradientColors)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating particles background
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            particles.forEach { particle ->
                val yOffset = ((particle.y + particleOffset * particle.speed) % 1f) * size.height
                drawCircle(
                    color = Color.White.copy(alpha = particle.alpha * glowPulse),
                    radius = particle.size,
                    center = Offset(particle.x * size.width, yOffset)
                )
            }
        }

        // Rotating glow rings
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(ringScale.value)
                .alpha(ringAlpha.value)
                .rotate(ringRotation),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Outer ring
                drawCircle(
                    color = Color(0xFF4ECDC4).copy(alpha = 0.3f * glowPulse),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 3f)
                )
                // Inner ring
                drawCircle(
                    color = Color(0xFFFF6B6B).copy(alpha = 0.2f * glowPulse),
                    radius = size.minDimension / 2.5f,
                    style = Stroke(width = 2f)
                )
            }
        }

        // Second rotating ring (opposite direction)
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(ringScale.value * 0.9f)
                .alpha(ringAlpha.value * 0.5f)
                .rotate(-ringRotation * 0.7f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFFFFE66D).copy(alpha = 0.15f * glowPulse),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 2f)
                )
            }
        }

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(logoScale.value)
                .alpha(logoAlpha.value)
        ) {
            // DutyPe Text Logo (styled like WorkerHomeScreen)
            Text(
                text = "DutyPe",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color(0xFF4ECDC4).copy(alpha = 0.6f),
                        offset = Offset(0f, 4f),
                        blurRadius = 12f
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline
            Text(
                text = "Find Work. Find Workers.",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }

        // Bottom branding
        Text(
            text = "Made with ❤️ in India",
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(taglineAlpha.value)
        )
    }
}

// REMOVED: DutyPeQuickSplash() - Dead code, never called anywhere in codebase
// Only DutyPeSplashScreen() is used in MainNavGraph
// If quick splash is needed in future, it can be re-added

/**
 * Data class for floating particles
 */
private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)
