package com.example.dutype.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Professional animations and micro-interactions
 * Provides smooth transitions and engaging user feedback
 */

/**
 * Fade in animation for content
 */
@Composable
fun FadeInAnimation(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    duration: Int = 300,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(duration)),
        exit = fadeOut(animationSpec = tween(duration))
    ) {
        content()
    }
}

/**
 * Slide in animation from bottom
 */
@Composable
fun SlideInFromBottomAnimation(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    duration: Int = 300,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(duration)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(duration))
    ) {
        content()
    }
}

/**
 * Slide in animation from right
 */
@Composable
fun SlideInFromRightAnimation(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    duration: Int = 300,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(duration)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(duration))
    ) {
        content()
    }
}

/**
 * Scale animation for buttons and cards
 */
@Composable
fun ScaleAnimation(
    isPressed: Boolean,
    modifier: Modifier = Modifier,
    scale: Float = 0.95f,
    content: @Composable () -> Unit
) {
    val scaleValue by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = tween(100),
        label = "scale_animation"
    )

    Box(
        modifier = modifier
            .scale(scaleValue)
            .graphicsLayer {
                alpha = if (isPressed) 0.8f else 1f
            }
    ) {
        content()
    }
}

/**
 * Bounce animation for success states
 */
@Composable
fun BounceAnimation(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_scale"
    )

    Box(
        modifier = modifier.scale(scale)
    ) {
        content()
    }
}

/**
 * Shake animation for error states
 */
@Composable
fun ShakeAnimation(
    isShaking: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val offsetX by animateFloatAsState(
        targetValue = if (isShaking) 0f else 0f,
        animationSpec = if (isShaking) {
            keyframes {
                durationMillis = 500
                0f at 0 with LinearEasing
                -10f at 50 with LinearEasing
                10f at 100 with LinearEasing
                -10f at 150 with LinearEasing
                10f at 200 with LinearEasing
                -10f at 250 with LinearEasing
                10f at 300 with LinearEasing
                -10f at 350 with LinearEasing
                10f at 400 with LinearEasing
                0f at 500 with LinearEasing
            }
        } else {
            tween(0)
        },
        label = "shake_animation"
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationX = offsetX
        }
    ) {
        content()
    }
}

/**
 * Animated button with ripple effect
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    isPressed = true
                    onClick()
                }
            ),
        enabled = enabled,
        colors = colors
    ) {
        ScaleAnimation(isPressed = isPressed) {
            content()
        }
    }
}

/**
 * Animated card with hover effect
 */
@Composable
fun AnimatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: CardElevation = CardDefaults.cardElevation(),
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    isPressed = true
                    onClick()
                }
            ),
        elevation = elevation
    ) {
        ScaleAnimation(isPressed = isPressed) {
            content()
        }
    }
}

/**
 * Animated floating action button
 */
@Composable
fun AnimatedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        ScaleAnimation(isPressed = isPressed) {
            content()
        }
    }
}

/**
 * Animated progress indicator
 */
@Composable
fun AnimatedProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )

    LinearProgressIndicator(
        progress = animatedProgress,
        modifier = modifier,
        color = color,
        trackColor = backgroundColor
    )
}

/**
 * Animated counter for numbers
 */
@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "counter_animation"
    )

    Text(
        text = animatedCount.toString(),
        modifier = modifier,
        style = style,
        color = color
    )
}

/**
 * Animated visibility with staggered children
 */
@Composable
fun StaggeredAnimation(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    staggerDelay: Int = 100,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(300, delayMillis = staggerDelay)
                ),
        exit = fadeOut(animationSpec = tween(300)) +
                slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(300)
                )
    ) {
        content()
    }
}

/**
 * Animated list item with slide in effect
 */
@Composable
fun AnimatedListItem(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(400, delayMillis = delay, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(400, delayMillis = delay)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        content()
    }
}

/**
 * Animated tab indicator
 */
@Composable
fun AnimatedTabIndicator(
    selectedTabIndex: Int,
    tabCount: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val indicatorOffset by animateFloatAsState(
        targetValue = selectedTabIndex.toFloat(),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "tab_indicator_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(color.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(1f / tabCount)
                .offset(x = (indicatorOffset * (1f / tabCount)).dp)
                .background(color)
        )
    }
}


/**
 * Animated notification badge
 */
@Composable
fun AnimatedNotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.error
) {
    val scale by animateFloatAsState(
        targetValue = if (count > 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badge_scale"
    )

    if (count > 0) {
        Box(
            modifier = modifier
                .size(20.dp)
                .scale(scale)
                .background(
                    color = color,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = MaterialTheme.colorScheme.onError,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Animated loading dots
 */
@Composable
fun AnimatedLoadingDots(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_dots")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1_alpha"
    )
    
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2_alpha"
    )
    
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3_alpha"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = color.copy(alpha = dot1Alpha),
                    shape = RoundedCornerShape(4.dp)
                )
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = color.copy(alpha = dot2Alpha),
                    shape = RoundedCornerShape(4.dp)
                )
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = color.copy(alpha = dot3Alpha),
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}

/**
 * Animated success checkmark
 */
@Composable
fun AnimatedSuccessCheckmark(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkmark_scale"
    )

    if (isVisible) {
        Box(
            modifier = modifier
                .size(24.dp)
                .scale(scale)
                .background(
                    color = color,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
