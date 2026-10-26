package com.example.dutype.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Modern Airbnb-style 3 animating bouncing dots loader.
 * Can be used as a standalone full-screen loader or inline loader.
 */
@Composable
fun ThreeDotsBouncingLoader(
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp,
    dotColor: Color = Color(0xFF222222),
    bounceHeight: Dp = 8.dp,
    spacing: Dp = 8.dp
) {
    val dots = listOf(
        remember { Animatable(0f) },
        remember { Animatable(0f) },
        remember { Animatable(0f) }
    )

    dots.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(index * 150L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { animatable ->
            val yOffset = (-bounceHeight.value * animatable.value).dp
            val alpha = 0.4f + (0.6f * animatable.value)
            Box(
                modifier = Modifier
                    .offset(y = yOffset)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = alpha))
            )
        }
    }
}

/**
 * Fullscreen clean 3-dots loader matching Airbnb loading screen.
 */
@Composable
fun FullScreenThreeDotsLoader(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    dotColor: Color = Color(0xFF222222)
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        ThreeDotsBouncingLoader(
            dotSize = 9.dp,
            dotColor = dotColor,
            bounceHeight = 7.dp,
            spacing = 7.dp
        )
    }
}

/**
 * Compact animated 3-dots bouncing loader designed specifically for buttons across the app.
 * Replaces circular spinners with modern Airbnb-style pulsing/bouncing dots.
 */
@Composable
fun ButtonThreeDotsLoader(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White,
    dotSize: Dp = 6.dp,
    bounceHeight: Dp = 4.dp,
    spacing: Dp = 4.dp
) {
    ThreeDotsBouncingLoader(
        modifier = modifier,
        dotSize = dotSize,
        dotColor = dotColor,
        bounceHeight = bounceHeight,
        spacing = spacing
    )
}
