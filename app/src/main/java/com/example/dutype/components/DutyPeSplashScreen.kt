package com.example.dutype.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import com.example.dutype.ui.theme.MeeshoFontFamily

/**
 * DutyPe Splash Screen - Logo appears then fades away
 * Logo shows centered, then fades out and disappears
 * 
 * STATUS BAR FIX: Uses statusBarsPadding() to ensure system UI is visible
 */
@Composable
fun DutyPeSplashScreen(
    onSplashComplete: () -> Unit
) {
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Hold logo in center briefly
        kotlinx.coroutines.delay(200)
        
        // Navigate immediately when fade starts (no blank screen)
        onSplashComplete()
        
        // Fade out animation happens after navigation starts
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 100,
                easing = FastOutSlowInEasing
            )
        )
    }

    // STATUS BAR: White background with dark icons for visibility
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // White status bar overlay at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Color.White)
                .align(Alignment.TopCenter)
        )
        
        // Main content centered
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Main logo - fades out in place
            Image(
                painter = painterResource(id = R.drawable.dutypenewlogo),
                contentDescription = "DutyPe Logo",
                modifier = Modifier
                    .size(280.dp)
                    .alpha(alpha.value)
            )
        }

        // Bottom branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Made with love in India 💙",
                style = TextStyle(
                    fontFamily = MeeshoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }
}
