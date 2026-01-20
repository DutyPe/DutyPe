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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.ui.theme.MeeshoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Clean & Minimal DutyPe Splash Screen
 * Pure white background with black text - elegant and professional
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
    val bottomAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Infinite animations for loading dots
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    // Launch animations
    LaunchedEffect(Unit) {
        // Logo scale with bounce
        scope.launch {
            delay(100)
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(700, easing = EaseOutBack)
            )
        }
        
        // Logo fade in
        scope.launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }
        
        // Tagline fade in
        scope.launch {
            delay(400)
            taglineAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }
        
        // Bottom text fade in
        scope.launch {
            delay(700)
            bottomAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }

        // Wait for splash duration then navigate
        delay(duration)
        onSplashComplete()
    }

    // Colors - Clean white theme
    val backgroundColor = Color.White
    val primaryBlack = Color(0xFF1F2937)
    val secondaryGray = Color(0xFF6B7280)
    val lightGray = Color(0xFFE5E7EB)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(logoScale.value)
                .alpha(logoAlpha.value)
        ) {
            // DutyPe Text - Clean black
            Text(
                text = "DutyPe",
                style = TextStyle(
                    fontFamily = MeeshoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                    color = primaryBlack
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline - Subtle gray
            Text(
                text = "Find Work. Find Workers.",
                style = TextStyle(
                    fontFamily = MeeshoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = secondaryGray,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.alpha(taglineAlpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator - three black dots with staggered animation
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(taglineAlpha.value)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(dot1Alpha)
                        .background(
                            color = primaryBlack,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(dot2Alpha)
                        .background(
                            color = primaryBlack,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(dot3Alpha)
                        .background(
                            color = primaryBlack,
                            shape = CircleShape
                        )
                )
            }
        }

        // Bottom branding - minimal
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(bottomAlpha.value)
        ) {
            Text(
                text = "Made with ❤️ in India",
                style = TextStyle(
                    fontFamily = MeeshoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = secondaryGray.copy(alpha = 0.7f)
                )
            )
        }
    }
}
