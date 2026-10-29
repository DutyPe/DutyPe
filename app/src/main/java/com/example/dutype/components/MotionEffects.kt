package com.example.dutype.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Standard Airbnb / Swiggy / Zomato Motion Curves
 */
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
val SwiggySpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

/**
 * Staggered cascade entrance modifier:
 * Elements slide up smoothly with a subtle scale and opacity ramp,
 * creating the signature Swiggy / Airbnb / Zomato card cascade effect.
 */
fun Modifier.cascadeEnter(
    index: Int = 0,
    isVisible: Boolean = true,
    baseDelayMs: Int = 35,
    durationMs: Int = 280,
    slideDistanceY: Float = 36f
): Modifier = composed {
    val alphaAnim = remember { Animatable(if (isVisible) 0f else 1f) }
    val transYAnim = remember { Animatable(if (isVisible) slideDistanceY else 0f) }
    val scaleAnim = remember { Animatable(if (isVisible) 0.95f else 1f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            val stagger = (index.coerceAtMost(10) * baseDelayMs)
            delay(stagger.toLong())
            launch {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
                )
            }
            launch {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = EmphasizedDecelerate)
                )
            }
            transYAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = durationMs, easing = EmphasizedDecelerate)
            )
        }
    }

    this.graphicsLayer {
        this.alpha = alphaAnim.value
        this.translationY = transYAnim.value
        this.scaleX = scaleAnim.value
        this.scaleY = scaleAnim.value
    }
}

/**
 * Tactile spring-press feedback modifier:
 * Gently scales down to 0.97f upon touch and springs back with a subtle bounce on release,
 * matching modern Airbnb & Zomato interactive cards.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressScale: Float = 0.965f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncyScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Composable container for applying the cascade effect to any item block.
 */
@Composable
fun CascadeContainer(
    index: Int,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    baseDelayMs: Int = 35,
    slideDistanceY: Float = 32f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.cascadeEnter(
            index = index,
            isVisible = isVisible,
            baseDelayMs = baseDelayMs,
            slideDistanceY = slideDistanceY
        )
    ) {
        content()
    }
}
